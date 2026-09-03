package ais.database.model.sekolah;

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

import ais.database.model.GeneralValueObject;

/**
 * Master <b>jenjang pendidikan orang tua/wali siswa</b> &mdash; tabel
 * {@code sekolah.pendidikan_orang_tua_siswa}.
 *
 * <h3>Peran sebenarnya: katalog pilihan, BUKAN data pendidikan keluarga</h3>
 *
 * <p>Baris di tabel ini <b>tidak</b> memuat riwayat pendidikan siapa pun. Ia hanya mendefinisikan
 * <i>daftar pilihan</i> jenjang yang nanti dipilih di formulir biodata siswa &mdash; satu baris =
 * satu jenjang (mis. &laquo;SD&raquo;, &laquo;SMP&raquo;, &laquo;S1&raquo;). Seluruh kolomnya
 * adalah {@link #getKode() kode}, {@link #getNama() nama}, {@link #getKeterangan() keterangan},
 * dan penanda {@link #getAktif() aktif}; tidak ada satu pun kolom yang menunjuk siswa, orang tua,
 * kelas, sekolah, atau yayasan.</p>
 *
 * <p><b>Isi tabel tidak ditentukan oleh kode.</b> Penelusuran seluruh repo tidak menemukan satu
 * pun daftar nilai bawaan (&laquo;SD&raquo;/&laquo;SMP&raquo;/&laquo;S1&raquo; dan seterusnya)
 * yang di-<i>hardcode</i> untuk kelas ini. Satu-satunya pengisi otomatis adalah blok penyalinan di
 * {@code InitDataHelper.initMaster()} yang mengkloning seluruh baris kembaran perguruan tinggi
 * {@link ais.database.model.PendidikanOrangTua} ({@code public.pendidikan_orang_tua}) &mdash;
 * dan tabel PT itu sendiri <b>juga tidak disemai kode manapun</b> (di {@code InitData} ia hanya
 * ikut daftar {@code initClasses(...)}, yang cuma memuat baris yang <i>sudah</i> ada ke cache
 * memori). Jadi pada instalasi yang benar-benar baru, tabel sumber kosong &rarr; loop penyalinan
 * nol iterasi &rarr; tabel ini <b>tetap kosong</b> sampai seorang admin mengetikkan jenjangnya
 * lewat layar master. Isi konkret di instalasi berjalan adalah data operasional, bukan konstanta
 * program.</p>
 *
 * <h3>Data pribadi yang sesungguhnya ada di hilir</h3>
 *
 * <p>Jenjang pendidikan <i>milik keluarga tertentu</i> tersimpan sebagai foreign key di
 * {@link Siswa}, bukan di sini:</p>
 * <ul>
 *   <li>{@link Siswa#getPendidikanAyah()} &rarr; kolom {@code pendidikan_ortu_ayah_id};</li>
 *   <li>{@link Siswa#getPendidikanIbu()} &rarr; kolom {@code pendidikan_ortu_ibu_id};</li>
 *   <li>{@link Siswa#getPendidikanWali()} &rarr; kolom {@code pendidikan_ortu_wali_id_data}
 *   (perhatikan akhiran {@code _data} yang tidak konsisten dengan dua kolom di atas &mdash;
 *   penamaan warisan, bukan salah ketik yang bisa diperbaiki tanpa migrasi).</li>
 * </ul>
 * <p>Konsekuensi untuk audit privasi: mengunci kelas ini <b>tidak</b> melindungi profil keluarga
 * siswa, dan membocorkan kelas ini <b>tidak</b> membocorkannya. Jangkauan kebocoran ditentukan
 * oleh gerbang akses di {@code Siswa}/{@code SiswaAction}.</p>
 *
 * <h3>Siapa yang memakai kelas ini</h3>
 *
 * <ol>
 *   <li><b>Layar master</b> {@code ais.action.master.sekolah.PendidikanOrangTuaSiswaAction}
 *   (ZUL {@code pages/master/sekolah/pendidikan_orang_tua_siswa.zul}) &mdash; satu-satunya penulis
 *   interaktif: tambah/ubah/hapus jenjang, plus centang {@code Aktif} per baris di grid.</li>
 *   <li><b>Combobox biodata siswa</b>: {@code SiswaAction} memanggil
 *   {@code Common.insertCombo(..., "nama", PendidikanOrangTuaSiswa.class,
 *   Restrictions.eq("aktif", true))} tiga kali (Pendidikan Ayah/Ibu/Wali). Perhatikan filternya
 *   <b>ketat</b> &mdash; lihat butir 3 di bawah.</li>
 *   <li><b>Penyemaian awal</b> di {@code InitDataHelper.initMaster()}: bila tabel ini masih nol
 *   baris, seluruh baris {@link ais.database.model.PendidikanOrangTua} disalin
 *   ({@code nama} apa adanya, {@code kode} dari {@code getKode() + ""}). Hanya dua properti itu
 *   yang diisi &mdash; {@code keterangan} dan {@code aktif} tidak disentuh.</li>
 *   <li><b>Preload cache</b>: {@code InitData} mendaftarkan kelas ini ke
 *   {@code InitDataHelper.initData(Class)} sehingga barisnya dimuat ke {@code MemoryCacheUtil}
 *   saat startup. Ini pemuat, bukan penyemai.</li>
 *   <li><b>Layar CRUD reflektif</b> {@code DynamicJspCrudGenerator.generate(
 *   PendidikanOrangTuaSiswa.class)} yang dirender oleh
 *   {@code WEB-INF/baru/modul/pagesmastersekolahpendidikanorangtuasiswazul/index.jsp}.</li>
 *   <li><b>Scaffold UI baru</b> {@code WEB-INF/new/sekolah/uiux/pendidikan_orang_tua_siswa.jsp}
 *   dan {@code .../services/pendidikan_orang_tua_siswa_service.jsp} &mdash; keduanya hanya
 *   menaruh atribut deskriptif lalu meneruskan ke {@code _shared/services/dispatcher.jsp};
 *   tidak ada akses data langsung di JSP.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>{@link CalonSiswa} TIDAK memakai kelas ini.</b> Field pendidikan orang tua pada calon
 *   siswa bertipe {@link ais.database.model.Pendidikan} (skema {@code public}) &mdash; katalog
 *   yang sama sekali berbeda, diisi lewat {@code CalonSiswaAction} dan formulir pendaftaran
 *   online {@code psb.form.PPDB1}/{@code PPDB2}. Jadi satu konsep bisnis yang sama dilayani
 *   <b>dua katalog terpisah</b> di dua sisi daur hidup pendaftaran, dan penelusuran repo tidak
 *   menemukan satu pun kode yang memindahkan nilai itu dari {@code CalonSiswa} ke {@code Siswa}
 *   saat calon siswa diterima. Praktis: jenjang pendidikan orang tua harus diketik ulang setelah
 *   penerimaan, dan laporan yang menggabungkan kedua sisi tidak bisa mengandalkan id yang sama.
 *   Jangan &laquo;merapikan&raquo; dengan mengganti tipe di salah satu sisi tanpa migrasi data
 *   &mdash; kedua kolom FK menunjuk tabel fisik yang berbeda.</li>
 *   <li><b>Satu-satunya pintu menu adalah tab di layar konfigurasi siswa</b> (pola
 *   <i>pewarisan hak lewat menu induk</i>, varian paling murni). ZUL kelas ini <b>tidak punya
 *   entri menu sendiri</b> di seluruh repo; ia hanya disisipkan sebagai tab ke-6 dari 7 sub-tab
 *   di {@code ais.action.master.KonfigurasiTampilanSiswaAction} (label tab: &laquo;Pendidikan&raquo;),
 *   berdampingan dengan Jenis Tinggal, Alat Transportasi, Penghasilan, Pekerjaan, Status Awal, dan
 *   Status Keluar. Karena {@code CommonPrivilages.checkPrevilages(...)} menyelesaikan haknya lewat
 *   {@code Common.getCurrentMenu()} &mdash; yaitu menu <i>induk</i> yang sedang aktif &mdash;
 *   pengguna yang punya {@code CREATE}/{@code UPDATE}/{@code DELETE} pada menu
 *   &laquo;Konfigurasi Tampilan Siswa&raquo; otomatis memperoleh hak yang sama atas <b>ketujuh</b>
 *   katalog itu sekaligus, tanpa satu pun izin terpisah yang bisa dicabut. Ini <b>pintu yang sama
 *   persis</b> dengan yang dipakai tiga kembarannya ({@code JenisTinggalSiswa},
 *   {@code AlatTransportasiSiswa}, {@code StatusAwalSiswa}), bukan pintu baru.</li>
 *   <li><b>Kolom {@code aktif} tidak pernah ditulis lewat jalur simpan biasa &mdash; catatan
 *   pengamatan, bukan kesimpulan.</b> Fakta-fakta yang bisa dibaca langsung dari kode:
 *     <ul>
 *       <li>field {@code aktif} tidak diinisialisasi (nilai awal {@code null}) dan konstruktor
 *       tidak mengisinya;</li>
 *       <li>{@code PendidikanOrangTuaSiswaAction.onSave()} hanya memanggil {@code setKode()},
 *       {@code setNama()}, {@code setKeterangan()} &mdash; <b>tidak pernah</b> {@code setAktif()};</li>
 *       <li>blok penyalinan di {@code InitDataHelper.initMaster()} juga hanya mengisi
 *       {@code nama} dan {@code kode};</li>
 *       <li>satu-satunya penulis {@code aktif} adalah checkbox per baris di
 *       {@code PendidikanOrangTuaSiswaRenderer}, yang harus ditekan pengguna;</li>
 *       <li>{@link #getAktif()} mengembalikan {@code true} bila field {@code null}, <b>tanpa</b>
 *       menulis balik ke field (berbeda dari getter destruktif di sebagian entity lain);</li>
 *       <li>pemetaan kelas ini memakai <i>property access</i> (anotasi {@code @Id}/{@code @Column}
 *       menempel di getter), dan kelas dianotasi {@code dynamicInsert = true}.</li>
 *     </ul>
 *     Apakah baris hasil penyimpanan/penyalinan berakhir sebagai {@code NULL} atau {@code true} di
 *     kolom database <b>belum diverifikasi secara empiris</b> dan sengaja tidak disimpulkan di
 *     sini: dokumentasi internal proyek memuat dua penjelasan yang saling bertentangan untuk pola
 *     getter serupa. Yang <i>pasti</i> dari kode adalah dampaknya bila nilainya ternyata
 *     {@code NULL}, lihat butir berikut.</li>
 *   <li><b>Dua pembaca memperlakukan {@code aktif} secara berbeda &mdash; ini yang membuat butir 3
 *   penting.</b> Layar master {@code initCriteria()} memakai penyaring <i>toleran</i>
 *   {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}, sedangkan combobox biodata di
 *   {@code SiswaAction} memakai penyaring <i>ketat</i> {@code Restrictions.eq("aktif", true)}.
 *   Bila kolomnya benar-benar {@code NULL}, gejalanya sangat khas: baris tampil normal di layar
 *   master (bahkan dengan centang &laquo;Aktif&raquo; menyala, karena checkbox dicentang dari
 *   {@link #getAktif()} yang meng-<i>coalesce</i> {@code null} menjadi {@code true}) tetapi
 *   <b>tidak pernah muncul</b> di combobox Pendidikan Ayah/Ibu/Wali. Penyembuhannya pun khas:
 *   menekan checkbox dua kali (mati lalu hidup) memaksa {@code setAktif(true)} tersimpan.</li>
 *   <li><b>{@link #getKeterangan()} di sini DIPETAKAN dengan benar.</b> Ia punya
 *   {@code @Column(name = "keterangan")} sendiri, jadi isian keterangan bertahan lintas request.
 *   Ini berbeda dari saudara satu layarnya {@code StatusAwalSiswa}, tempat {@code keterangan}
 *   sama sekali tidak dipetakan karena {@link GeneralValueObject} bukan {@code @MappedSuperclass}.
 *   Jangan menyamaratakan perilaku kedua kelas hanya karena strukturnya kembar.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *   duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa &mdash;
 *   bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate sama sekali
 *   tidak memetakan properti kelas induk. Setiap entity turunan <b>wajib</b> mendeklarasikan
 *   sendiri kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Properti induk yang lain ikut tidak terpetakan.</b> {@code nomorUrut}, {@code nim},
 *   {@code sekolah}, dan kawan-kawan tetap ada sebagai field Java tetapi tidak punya kolom.
 *   Akibatnya {@link GeneralValueObject#getNomorUrut()} di sini selalu {@code null}, sehingga
 *   {@link GeneralValueObject#compareTo(GeneralValueObject)} langsung jatuh ke kunci berikutnya
 *   ({@code nim} yang juga selalu {@code null}) lalu ke {@code nama}. Pengurutan kelas ini karena
 *   itu <i>alfabetis berdasarkan nama</i> dan <b>tidak</b> mengalami masalah penciutan
 *   {@code TreeSet} yang muncul pada entity lain yang meng-override {@code getNomorUrut()} dengan
 *   nilai default non-null.</li>
 *   <li><b>{@link #toString()} membaca field mentah dan bisa mengembalikan {@code null}.</b> Ia
 *   meng-override versi induk (yang mencetak {@code "<id>-<nama>"}) dan mengembalikan field
 *   {@code nama} apa adanya &mdash; tanpa {@code trim()} seperti {@link #getNama()}, dan tanpa
 *   penjagaan {@code null} untuk baris yang namanya belum diisi. Pemanggil yang merantai
 *   {@code obj.toString().xxx()} akan ber-{@code NullPointerException}; perangkaian string biasa
 *   aman karena menghasilkan {@code "null"}.</li>
 *   <li><b>{@link #getNama()} men-{@code trim()} saat baca, {@link #setNama(String)} tidak saat
 *   tulis.</b> Spasi ujung tersimpan di database dan hanya disembunyikan ketika dibaca lewat
 *   getter. Validasi keunikan di layar master
 *   ({@code checkNamaPendidikanOrangTuaSiswa()}) memakai {@code Restrictions.eq("nama",
 *   nilai.trim())} sehingga bersifat <i>case-sensitive</i>: &laquo;SMA&raquo; dan
 *   &laquo;Sma&raquo; dianggap dua jenjang berbeda dan keduanya lolos.</li>
 *   <li><b>{@link #getKode()} tidak punya {@code @Column} sendiri</b> &mdash; ia terpetakan lewat
 *   penamaan default ke kolom {@code kode} dengan panjang default 255, tanpa keunikan yang
 *   ditegakkan. Layar master menyediakan isian dan penyaring pencarian untuk kode, tetapi tidak
 *   ada satu pun validasi duplikat untuknya (validasi hanya menyentuh {@code nama}). Hal yang sama
 *   berlaku untuk {@link #getAktif()}.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi tidak
 *   ada {@code @PrePersist}, jadi {@code oleh}/{@code olehId} hanya terisi saat baris di-UPDATE,
 *   bukan saat dibuat. Baris hasil penyalinan awal karena itu tidak punya pemilik tercatat sama
 *   sekali.</li>
 *   <li><b>{@code serialVersionUID} identik dengan milik {@link ais.database.model.PendidikanOrangTua}</b>
 *   (dan juga dengan {@code PenghasilanOrangTuaSiswa}/{@code PekerjaanOrtuSiswa}) &mdash; sisa
 *   salin-tempel saat keluarga kelas sekolah dibuat dari kelas PT. Tidak berpengaruh apa pun
 *   karena serialisasi Java selalu dilakukan per kelas, dan <b>bukan</b> tanda berbagi tabel:
 *   kelas ini memetakan {@code sekolah.pendidikan_orang_tua_siswa} sedangkan kembaran PT
 *   memetakan {@code public.pendidikan_orang_tua}.</li>
 *   <li><b>Komentar &laquo;Bank generated by hbm2java&raquo; pada Javadoc lama adalah sisa
 *   generator</b> yang tertinggal dari templat entity {@code Bank}; ia tidak pernah menggambarkan
 *   isi kelas ini.</li>
 * </ol>
 *
 * <h3>Catatan akses (hasil penelusuran, bukan klaim umum)</h3>
 *
 * <p><b>Layar master memakai gerbang standar, dengan satu perbedaan dari saudaranya.</b>
 * {@code PendidikanOrangTuaSiswaAction.doBeforeCompose()} memanggil {@code Common.doCheckSecurity()},
 * dan tombol Tambah/Ubah/Hapus masing-masing bergantung pada
 * {@code CommonPrivilages.CREATE}/{@code UPDATE}/{@code DELETE}. Berbeda dari
 * {@code PenghasilanOrangTuaSiswaAction}, {@code doAfterCompose()} di sini <b>tidak</b> mengulang
 * pemeriksaan {@code usersTemp} + {@code READ}; dalam praktiknya layar ini selalu dirender sebagai
 * tab di dalam {@code KonfigurasiTampilanSiswaAction} yang sudah melakukan pemeriksaan itu, jadi
 * ini perbedaan konsistensi, bukan lubang yang terbukti. Yang perlu diingat tetap butir 2 di atas:
 * hak yang dipakai adalah hak menu induk.</p>
 *
 * <p><b>Tidak ada penyaringan sekolah/yayasan di {@code initCriteria()}</b> &mdash; dan di sini itu
 * <b>bukan</b> cacat <i>fail-open</i>: tabel ini kamus global tanpa kolom kepemilikan apa pun, dan
 * isinya bukan data pribadi. {@code PendidikanOrangTuaSiswaAction} juga <b>tidak memanggil</b>
 * {@code OrangTua.ambilAnakSiswa()} sama sekali, jadi pola <i>fail-open</i> cakupan orang tua yang
 * berulang di modul sekolah tidak berlaku untuk kelas ini.</p>
 *
 * <p><b>Verifikasi negatif untuk jalur pra-otentikasi:</b> kelas ini tidak muncul di
 * {@code psb.form.PPDB*} maupun jalur {@code /ppdb} lainnya (formulir pendaftaran online memakai
 * {@link ais.database.model.Pendidikan}), sehingga daftar jenjang versi sekolah tidak terekspos
 * sebelum login.</p>
 *
 * <p><b>Ekspor tanpa gerbang (dampak rendah):</b> tombol cetak/ekspor yang dipasang
 * {@code Common.cetakData(PendidikanOrangTuaSiswa.class, ...)} di {@code doAfterCompose()}
 * ditambahkan tanpa pemeriksaan hak apa pun, sementara tombol unggah massal
 * ({@code Common.uploadData}) dijaga oleh kombinasi {@code CREATE && UPDATE && DELETE}. Yang bisa
 * diekspor hanyalah kamus jenjang, bukan data siswa.</p>
 *
 * @see GeneralValueObject
 * @see ais.database.model.PendidikanOrangTua
 * @see PenghasilanOrangTuaSiswa
 * @see PekerjaanOrtuSiswa
 * @see Siswa#getPendidikanAyah()
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "pendidikan_orang_tua_siswa")
public class PendidikanOrangTuaSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sama persis dengan milik
	 * {@link ais.database.model.PendidikanOrangTua} (dan beberapa saudara sekeluarga) &mdash; sisa
	 * salin-tempel saat kelas sekolah dibuat dari kelas perguruan tinggi. Tidak berdampak apa pun
	 * karena serialisasi selalu dilakukan per kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci utama {@code sekolah.pendidikan_orang_tua_siswa.id}, strategi {@code IDENTITY}
	 * (di-generate database). Dideklarasikan ulang di sini karena {@link GeneralValueObject}
	 * tidak dipetakan Hibernate &mdash; lihat javadoc kelas.
	 */
	private Long id;

	/** Nama pengguna terakhir yang meng-UPDATE baris ini; diisi {@link #onUpdate()}, kosong untuk baris yang belum pernah diubah. */
	private String oleh;

	/** Id pengguna terakhir yang meng-UPDATE baris ini; diisi {@link #onUpdate()}, kosong untuk baris yang belum pernah diubah. */
	private String olehId;

	/** @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah. <b>Menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/berisi spasi saja: dalam kasus itu method langsung {@code return} dan nilai lama
	 * dipertahankan. Perilaku ini disengaja agar jejak audit yang sudah ada tidak terhapus oleh
	 * pemanggil yang tidak punya konteks pengguna.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan diam-diam</b> agar jejak audit lama tidak tertimpa.
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
	 * kolom-kolom ini &mdash; termasuk baris hasil penyalinan awal dari
	 * {@link ais.database.model.PendidikanOrangTua} (lihat javadoc kelas).
	 *
	 * <p>Pada baris deklarasi yang sama juga dideklarasikan field {@code tanggal_dirubah}, yang
	 * diinisialisasi ke waktu server saat objek dibuat ({@code ais.ui.util.WaktuUtil.getDate()})
	 * sehingga baris baru tetap punya stempel waktu meski belum pernah di-update. Penggabungan dua
	 * deklarasi dalam satu baris adalah gaya warisan repo; jangan dipisah tanpa alasan karena
	 * banyak berkas entity lain memakai bentuk identik.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Biasanya tidak dipanggil kode aplikasi &mdash;
	 * pengisian normalnya dilakukan {@link #onUpdate()}. Berbeda dari {@link #setOleh(String)},
	 * di sini {@code null} <b>diterima</b> dan akan mengosongkan kolom.
	 *
	 * @param tanggal_dirubah stempel waktu baru, boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir, dipetakan ke kolom {@code tanggal_dirubah}
	 *         bertipe {@code TIMESTAMP}; untuk baris baru berisi waktu pembuatan objek
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity: <b>field {@code nama} mentah</b>, meng-override versi
	 * {@link GeneralValueObject#toString()} yang mencetak {@code "<id>-<nama>"}.
	 *
	 * <p>Dua kuirk yang perlu diingat: (a) nilai dibaca langsung dari field sehingga
	 * <b>tidak</b> ikut {@code trim()} seperti {@link #getNama()} &mdash; untuk nama berspasi ujung
	 * keduanya memberi jawaban berbeda; (b) hasilnya bisa {@code null} untuk baris yang namanya
	 * belum diisi, sehingga pemanggil yang merantai method pada hasil {@code toString()} akan
	 * ber-{@code NullPointerException}. Dipakai antara lain oleh komponen ZK yang menampilkan
	 * objek entity secara langsung.</p>
	 *
	 * @return nama jenjang apa adanya, atau {@code null}
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Kode singkat jenjang (mis. kode padanan dari katalog perguruan tinggi). Tidak wajib, tidak
	 * unik, dan tidak divalidasi di layar manapun; terpetakan ke kolom {@code kode} lewat penamaan
	 * default.
	 */
	private String kode;

	/**
	 * Nama jenjang yang tampil di combobox biodata siswa &mdash; kolom {@code nama},
	 * {@code NOT NULL}. Ini sekaligus kunci keunikan logis yang divalidasi layar master dan kunci
	 * pengurutan efektif entity ini.
	 */
	private String nama;

	/** Keterangan bebas; terpetakan ke kolom {@code keterangan} yang boleh {@code null}. Ditampilkan sebagai kolom tersendiri di grid layar master. */
	private String keterangan;

	/**
	 * Penanda baris masih dipakai. Tidak diinisialisasi (awalnya {@code null}) dan tidak pernah
	 * ditulis oleh {@code onSave()} maupun blok penyalinan awal &mdash; satu-satunya penulis
	 * adalah checkbox per baris di layar master. Lihat butir 3 dan 4 pada javadoc kelas untuk
	 * catatan pengamatan lengkapnya.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen. Diperlukan Hibernate untuk instansiasi reflektif, dan dipakai
	 * langsung oleh {@code PendidikanOrangTuaSiswaAction.onAdd()} serta blok penyalinan di
	 * {@code InitDataHelper.initMaster()}. Tidak mengisi field apa pun &mdash; termasuk
	 * {@link #aktif} yang karenanya tetap {@code null}.
	 */
	public PendidikanOrangTuaSiswa() {
	}

	/**
	 * @return kunci utama baris ini, atau {@code null} untuk objek yang belum disimpan.
	 *         Kolom ditandai {@code insertable = false} karena nilainya dihasilkan database
	 *         ({@code IDENTITY}), bukan dikirim aplikasi.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Hanya dipakai Hibernate saat memuat/menyimpan baris; kode aplikasi
	 * tidak boleh memanggilnya untuk memindahkan identitas antar baris.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama jenjang yang sudah di-{@code trim()}, atau {@code null} bila field kosong.
	 *         {@code trim()} hanya dilakukan pada nilai kembalian &mdash; field dan isi database
	 *         <b>tidak</b> ikut dirapikan, jadi getter ini tidak membuat baris menjadi <i>dirty</i>.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenjang apa adanya, <b>tanpa</b> {@code trim()}. Pemanggil bertanggung jawab
	 * merapikan input sendiri; layar master meneruskan {@code Textbox.getValue()} langsung
	 * sehingga spasi ujung yang diketik pengguna ikut tersimpan.
	 *
	 * @param nama nama jenjang; kolomnya {@code NOT NULL} sehingga {@code null} akan ditolak
	 *             database saat flush
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas baris ini, atau {@code null}. Dipetakan penuh ke kolom {@code keterangan}, jadi isiannya bertahan lintas request. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Diisi dari {@code Textbox} keterangan di layar master saat
	 * Simpan; boleh {@code null}/kosong.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return kode singkat jenjang, atau {@code null}. Tanpa {@code @Column} eksplisit sehingga terpetakan ke kolom {@code kode} dengan panjang default. */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode singkat jenjang. Diisi dari {@code Textbox} kode di layar master, dan dari
	 * {@code pendidikanOrangTua.getKode() + ""} pada blok penyalinan awal &mdash; perhatikan
	 * perangkaian string itu menghasilkan literal {@code "null"} (bukan {@code null}) bila kode di
	 * katalog perguruan tinggi kosong.
	 *
	 * @param kode kode baru; tidak divalidasi keunikan maupun panjangnya
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan status aktif baris, dengan {@code null} di-<i>coalesce</i> menjadi
	 * {@code true} sehingga baris lama yang belum pernah dicentang tetap dianggap aktif.
	 *
	 * <p><b>Penting:</b> nilai coalesced <b>tidak</b> dituliskan balik ke field &mdash; getter ini
	 * tidak destruktif dan tidak membuat baris menjadi <i>dirty</i>. Namun karena kelas ini
	 * memakai <i>property access</i>, Hibernate membaca nilai lewat getter ini saat INSERT dan
	 * dirty-check, sehingga nilai yang dilihat Hibernate berbeda dari isi field di memori.
	 * Konsekuensi persisnya di database belum diverifikasi empiris &mdash; lihat butir 3 dan 4 pada
	 * javadoc kelas sebelum mengandalkan perilaku ini.</p>
	 *
	 * <p>Dipanggil antara lain oleh {@code PendidikanOrangTuaSiswaRenderer} untuk menentukan status
	 * awal checkbox &laquo;Aktif&raquo; tiap baris grid.</p>
	 *
	 * @return {@code true} bila baris aktif atau belum pernah disetel; {@code false} hanya bila
	 *         eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif baris. <b>Satu-satunya jalur penulisan {@code aktif} di seluruh
	 * repo</b> adalah pemanggilan dari listener {@code onCheck} checkbox di
	 * {@code PendidikanOrangTuaSiswaRenderer}, yang langsung disusul
	 * {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan tersimpan seketika tanpa menekan
	 * tombol Simpan. {@code onSave()} pada form tambah/ubah tidak menyentuh properti ini sama
	 * sekali.
	 *
	 * <p>Menonaktifkan baris menyembunyikannya dari combobox Pendidikan Ayah/Ibu/Wali di biodata
	 * siswa (penyaring {@code Restrictions.eq("aktif", true)}), tetapi <b>tidak</b> mengubah baris
	 * {@link Siswa} yang sudah terlanjur menunjuk jenjang itu &mdash; nilai lama tetap tersimpan
	 * dan tetap tampil di laporan.</p>
	 *
	 * @param aktif status baru; {@code null} akan diperlakukan sebagai aktif oleh
	 *              {@link #getAktif()} tetapi <b>tidak</b> oleh penyaring ketat di
	 *              {@code SiswaAction}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
