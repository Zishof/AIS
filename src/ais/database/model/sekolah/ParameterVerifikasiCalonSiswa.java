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
 * Entity Hibernate yang memetakan tabel {@code sekolah.parameter_verifikasi_calon_siswa} pada
 * modul <b>PSB/PPDB</b> (penerimaan siswa baru). Satu baris = <b>satu nilai gradasi/tingkat</b>
 * yang dapat dipilih saat seorang calon siswa mendaftarkan entri berkas persyaratan.
 *
 * <h2>Domain — TERVERIFIKASI dari kode, bukan dari nama kelas</h2>
 * <p>Nama kelas "ParameterVerifikasiCalonSiswa" mudah disalahartikan sebagai <i>katalog jenis
 * dokumen</i> yang diverifikasi ("Akte Kelahiran", "Kartu Keluarga", "Surat Sehat"). <b>Bukan
 * itu.</b> Pemeriksaan seluruh pembaca/penulis menunjukkan entity ini adalah <b>katalog nilai
 * tingkat</b>: pada layar petugas maupun formulir pendaftaran mandiri, isinya dirender sebagai
 * satu combobox berlabel persis <b>"Tingkat&nbsp;*"</b> dan sebagai kolom sub-grid berjudul
 * <b>"Tingkat"</b> (lihat {@code ais.action.master.sekolah.psb.VerifikasiParameterPSBHelper}).</p>
 *
 * <p>Bukti terkuatnya adalah data awal yang ditanam sendiri oleh layar masternya
 * ({@code ParameterVerifikasiCalonSiswaAction.doAfterCompose()}, hanya bila tabel masih kosong):</p>
 * <ol>
 * <li>{@code "Prestasi Tingkat Sekolah/Daerah/Kapubaten"} (salah eja bawaan; seharusnya
 * "Kabupaten" — tetap dipertahankan agar instalasi lama tidak berubah);</li>
 * <li>{@code "Prestasi Tingkat Nasional"};</li>
 * <li>{@code "Prestasi Tingkat Internasional"}.</li>
 * </ol>
 * <p>Jadi "parameter verifikasi" di sini berarti <i>parameter/atribut pelengkap</i> dari sebuah
 * entri berkas, bukan jenis berkasnya. Jenis berkasnya justru dipegang oleh
 * {@link GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa} (kolom {@code judul} dan
 * {@code nama}), sedangkan kelas ini melengkapinya dengan tingkatan.</p>
 *
 * <h2>Posisinya dalam rantai verifikasi PSB</h2>
 * <ol>
 * <li>{@link GelombangPendaftaranPsb} — gelombang pendaftaran milik satu {@code Sekolah}/{@code Yayasan}.</li>
 * <li>{@link GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa} — <b>kategori berkas</b>
 * yang berlaku pada gelombang itu ("Prestasi Akademik" / "Sertifikat"). Kategori inilah yang
 * memilih <b>subset</b> baris kelas ini yang boleh dipakai, lewat relasi {@code @ManyToMany}
 * melalui tabel penghubung {@code sekolah.gelombang_verifikasi_calon_siswa_punya_parameter}
 * (kolom {@code gelombang} dan {@code parameter}).</li>
 * <li><b>Kelas ini</b> — katalog <b>global</b> nilai tingkat, dipakai bersama oleh seluruh
 * gelombang dan seluruh sekolah pada satu instalasi.</li>
 * <li>{@code ais.database.model.sekolah.CalonSiswaPunyaVerifikasiParameter} — baris
 * <b>transaksi</b> milik seorang calon siswa; menunjuk balik ke kategori (butir 2) <i>dan</i> ke
 * satu baris kelas ini sebagai tingkat terpilih, plus status {@code verified} dan berkas bukti.</li>
 * </ol>
 *
 * <h2>Siapa yang membaca dan menulis</h2>
 * <ul>
 * <li><b>Penulis (satu-satunya):</b> {@code ais.action.master.sekolah.ParameterVerifikasiCalonSiswaAction}
 * — layar CRUD {@code z/x/y/pages/master/sekolah/parameter_verifikasi_calon_siswa.zul}. Layar itu
 * juga yang menanam tiga baris awal di atas dan menyediakan tombol Download serta Upload Data
 * massal untuk kolom {@code id, nama, aktif, nomorUrut, keterangan}.</li>
 * <li><b>Pembaca 1:</b> {@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswaAction}
 * — memuat seluruh baris <b>aktif</b> ({@code Restrictions.or(isNull("aktif"), eq("aktif", true))})
 * diurutkan {@code Order.asc("nomorUrut")}, lalu merendernya sebagai daftar checkbox untuk
 * menentukan subset milik satu kategori.</li>
 * <li><b>Pembaca 2:</b> {@code VerifikasiParameterPSBHelper} — membaca subset milik kategori,
 * mengurutkannya dengan {@code Collections.sort(...)} (yaitu
 * {@link GeneralValueObject#compareTo(GeneralValueObject)}), lalu mengisi combobox "Tingkat" dengan
 * properti {@code nama}. Helper ini dipanggil dari layar petugas {@code CalonSiswaAction}
 * <b>dan</b> dari keluarga formulir pendaftaran mandiri {@code PPDB1}…{@code PPDB_Simple8}; tombol
 * "Tambah" beserta combobox "Tingkat" dirender <b>tanpa syarat pengguna login</b>, sehingga nama
 * baris katalog ini terlihat oleh pendaftar publik (isinya label generik, bukan data pribadi).</li>
 * </ul>
 *
 * <h2>Non-obvious 1 — {@code getNomorUrut()} adalah AKAR bug penciutan {@code TreeSet}</h2>
 * <p>Ini temuan paling penting pada berkas ini. {@link GeneralValueObject#compareTo(GeneralValueObject)}
 * memakai <b>{@code getNomorUrut()} sebagai kunci urut PERTAMA</b>, dan hanya turun ke NIM/nama/
 * keterangan bila salah satu sisi {@code null}. Versi induknya memang mengembalikan {@code null}
 * saat belum diisi — tetapi {@link #getNomorUrut()} pada kelas ini <b>meng-override</b>-nya dengan
 * nilai baku {@code 1} sehingga <b>tidak pernah {@code null}</b>.</p>
 * <p>Akibatnya, selama admin belum pernah menyetel nomor urut yang berbeda-beda, setiap pasangan
 * baris katalog ini menghasilkan {@code Integer.valueOf(1).compareTo(1) == 0} — yaitu <b>dianggap
 * duplikat oleh setiap {@code TreeSet}/{@code TreeMap}</b>. Inilah mekanisme persis di balik bug
 * kehilangan data senyap yang didokumentasikan pada
 * {@link GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa#getParameterVerifikasiCalonSiswas()}:
 * pada penyimpanan <b>pertama</b> sebuah kategori baru (saat koleksinya masih {@code new TreeSet<>()}
 * dan belum digantikan {@code PersistentSet} oleh Hibernate), berapa pun jumlah tingkat yang
 * dicentang pengguna, hanya <b>satu</b> yang benar-benar tersimpan. Edit-simpan berikutnya terlihat
 * normal karena koleksinya sudah bukan {@code TreeSet} lagi.</p>
 * <p>Konsekuensi praktis yang perlu diketahui pemelihara: mengisi {@code nomorUrut} berbeda untuk
 * tiap baris di layar master <b>menyembuhkan</b> gejala tersebut, sedangkan menyeragamkannya
 * (atau membiarkannya {@code NULL}) menghidupkannya kembali. Jangan "merapikan"
 * {@link #getNomorUrut()} menjadi mengembalikan {@code null} tanpa memeriksa ulang seluruh jalur
 * pengurutan; dan jangan pula menganggapnya kosmetik.</p>
 *
 * <h2>Non-obvious 2 — {@code aktif} di sini TIDAK terkena bug "aktif tak pernah ditulis"</h2>
 * <p>Baris hasil penanaman otomatis disimpan tanpa pernah menyentuh {@code aktif}, sehingga
 * kolomnya {@code NULL} di database — pola yang pada beberapa katalog master lain membuat baris
 * baru tak pernah muncul di kombo mana pun sampai checkbox ditekan dua kali. Di sini pola itu
 * <b>tidak terjadi</b>, karena kedua sisi konsisten memperlakukan {@code NULL} sebagai "aktif":
 * {@link #getAktif()} mengembalikan {@code true} bila field {@code null}, dan satu-satunya query
 * pembaca memakai {@code or(isNull("aktif"), eq("aktif", true))}. Berkas ini karenanya layak
 * dipakai sebagai <b>contoh pembanding positif</b> untuk pola tersebut.</p>
 *
 * <h2>Non-obvious 3 — katalog GLOBAL, tanpa kolom tenant</h2>
 * <p>Entity ini <b>tidak memiliki kolom {@code sekolah} maupun {@code yayasan}</b>, dan seluruh
 * query pembacanya memang tidak menyaring tenant. Ini bukan kasus <i>fail-open</i> (filter yang
 * gagal), melainkan ketiadaan konsep tenant secara desain: satu instalasi berbagi satu daftar
 * tingkat. Efek sampingnya nyata — mengganti nama sebuah tingkat, menonaktifkannya, atau
 * menghapusnya berlaku serentak untuk <b>seluruh sekolah/yayasan</b> dalam instalasi tersebut,
 * termasuk gelombang yang sedang berjalan.</p>
 *
 * <h2>Non-obvious 4 — penghapusan baris memicu NPE laten di panel verifikasi</h2>
 * <p>FK {@code CalonSiswaPunyaVerifikasiParameter.parameter_verifikasi_calon_siswa} bersifat
 * {@code nullable = true} dan pembacanya di {@code VerifikasiParameterPSBHelper.reloadData(...)}
 * memanggil {@code getParameterVerifikasiCalonSiswa().getNama()} <b>tanpa penjaga {@code null}</b>.
 * Menghapus baris katalog di sini adalah cara paling langsung membuat kolom itu menjadi
 * {@code null} pada data lama, sehingga panel verifikasi calon siswa yang bersangkutan gagal
 * dirender. Nonaktifkan ({@code aktif = false}) jauh lebih aman daripada menghapus.</p>
 *
 * <h2>Pemeriksaan pola berulang repo pada berkas ini</h2>
 * <ul>
 * <li><b>Getter penulis-balik / destruktif</b> (pola {@code KelasSiswaPSB.getNama()}):
 * <b>TIDAK ADA</b>. {@link #getNama()} hanya mem-{@code trim()} nilai kembalian dan
 * <b>tidak</b> menugaskannya kembali ke field; {@link #getAktif()} dan {@link #getNomorUrut()}
 * menormalkan {@code null} hanya pada nilai kembalian, juga tanpa menulis field. Karena itu
 * tidak ada revisi Envers palsu / {@code UPDATE} hantu dari sekadar merender grid.</li>
 * <li><b>Fail-open cakupan tenant:</b> <b>TIDAK RELEVAN</b> — lihat Non-obvious 3.</li>
 * <li><b>Pewarisan hak lewat menu induk:</b> <b>ADA</b> — lihat bagian berikut.</li>
 * <li><b>Tombol mutasi massal tanpa gerbang:</b> <b>ADA satu</b> ({@code Intbox} nomor urut) —
 * lihat bagian berikut. Tombol Upload Data massal justru <b>digerbangi dengan benar</b>
 * ({@code add.isVisible() && edit && delete}).</li>
 * </ul>
 *
 * <h2>Hak akses — hasil verifikasi terhadap layar pengelola</h2>
 * <p><b>Kesimpulan: layar master ini BUKAN instance "nol {@code checkPrevilages}" berikutnya.</b>
 * Berbeda dari empat kerabat PSB-nya ({@code RuangPSB},
 * {@code CalonSiswaPunyaVerifikasiParameter},
 * {@link GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa}/{@code InterviewPunyaCalonSiswa},
 * {@code RuangGelombangPendaftaranPsbPSB}), {@code ParameterVerifikasiCalonSiswaAction} memasang
 * gerbang yang benar untuk ketiga operasi tulis:</p>
 * <pre>{@code
 * add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
 * edit   = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
 * delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
 * }</pre>
 * <p>Namun tiga celah tetap terverifikasi ada, dan semuanya bersifat "sekitar" gerbang tersebut:</p>
 * <ol>
 * <li><b>{@code Intbox} nomor urut tanpa gerbang.</b> Pada renderer grid, checkbox "Aktif"
 * benar-benar dikunci ({@code checkbox.setDisabled(!edit)}), tetapi {@code Intbox} untuk
 * {@code nomorUrut} di sebelahnya dibuat <b>tanpa {@code setDisabled} sama sekali</b> dan
 * listener {@code onChange}-nya langsung memanggil {@code Common.refreshUpdate(...)}. Pemegang
 * hak <b>BACA saja</b> karenanya dapat mengubah nomor urut baris katalog global, tersimpan
 * seketika. Ini kembaran persis pola pada {@code JenisAktiftasHarianDefault}, tetapi di sini
 * dampaknya <b>lebih dari kosmetik</b>: karena {@code nomorUrut} adalah kunci urut pertama
 * {@code compareTo} (Non-obvious 1), menyeragamkan nilainya menghidupkan kembali bug penciutan
 * {@code TreeSet} pada konfigurasi kategori verifikasi.</li>
 * <li><b>Pewarisan hak lewat menu induk.</b> Berkas {@code parameter_verifikasi_calon_siswa.zul}
 * <b>tidak pernah dibuka sebagai halaman menu tersendiri</b>: satu-satunya pemuatnya adalah
 * {@code GelombangPendaftaranPsbAction.onVerifikasiTambahan(...)}, yang menyisipkannya sebagai
 * {@code MyInclude} pada tab "Verifikasi Tambahan" di layar <b>Gelombang Pendaftaran PSB</b>.
 * Karena {@code CommonPrivilages.checkPrevilages(...)} menguji hak terhadap
 * {@code Common.getCurrentMenu()} — yaitu menu halaman terluar — hak CREATE/UPDATE/DELETE yang
 * diuji di atas sesungguhnya adalah hak atas menu <b>Gelombang Pendaftaran PSB</b>, bukan atas
 * katalog ini. Siapa pun yang boleh mengubah gelombang otomatis boleh mengubah katalog global
 * tingkat verifikasi.</li>
 * <li><b>Penanaman data awal mendahului gerbang apa pun.</b> Blok penanaman tiga baris di
 * {@code doAfterCompose()} berjalan sebelum {@code edit}/{@code delete} dihitung dan tidak
 * dibungkus pemeriksaan hak apa pun; membuka tab tersebut satu kali dengan hak BACA saja sudah
 * menulis tiga baris ke database. Tombol Download (ekspor katalog) juga tidak digerbangi, namun
 * isinya hanya label — bukan PII.</li>
 * </ol>
 * <p>{@code doBeforeCompose()} hanya memanggil {@code Common.doCheckSecurity()} (memastikan sudah
 * login); halaman ini <b>tidak</b> terdaftar di {@code CommonPrivilages.MUST_CHECKED}, sehingga
 * hak READ tidak pernah diperiksa secara eksplisit.</p>
 *
 * <h2>Catatan teknis pemetaan</h2>
 * <ul>
 * <li>{@code @org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)} —
 * Hibernate hanya menyertakan kolom yang benar-benar berubah pada {@code INSERT}/{@code UPDATE}.</li>
 * <li>{@code @Audited} (Envers) — setiap perubahan direkam ke tabel audit pasangannya, termasuk
 * perubahan {@code nomorUrut} yang dipicu {@code Intbox} tanpa gerbang di atas.</li>
 * <li>{@link GeneralValueObject} <b>bukan</b> {@code @Entity}/{@code @MappedSuperclass} melainkan
 * POJO abstrak biasa, sehingga Hibernate tidak memetakan properti induknya. Deklarasi ulang
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini <b>bukan
 * duplikasi yang salah</b>, melainkan keharusan teknis agar kolom-kolom itu tetap terpetakan.
 * Perhatikan bahwa {@code nama}, {@code keterangan}, {@code aktif}, dan {@code nomorUrut} juga
 * dideklarasikan ulang di sini dengan alasan yang sama — dan untuk {@code nomorUrut} deklarasi
 * ulang itulah yang menyembunyikan getter induk (lihat Non-obvious 1).</li>
 * <li>Strategi penamaan kolom adalah {@code ais.database.hibernate.MyNamingStrategy}, turunan
 * {@code DefaultNamingStrategy}: nama kolom = nama properti <b>apa adanya</b>. Karena
 * {@link #getAktif()} dan {@link #getNomorUrut()} tidak memakai {@code @Column}, kolom fisiknya
 * bernama {@code aktif} dan <b>{@code nomorUrut}</b> — camelCase, menyimpang dari konvensi
 * snake_case skema {@code sekolah} lainnya. Jangan menuliskannya sebagai {@code nomor_urut} pada
 * SQL native.</li>
 * <li>Keunikan {@code nama} <b>tidak</b> ditegakkan oleh constraint database; hanya oleh
 * pemeriksaan aplikasi {@code checkNamaParameterVerifikasiCalonSiswa()} yang bersifat
 * <i>case-sensitive</i> ({@code Restrictions.eq}). Unggah massal dan skrip di luar layar itu
 * dapat menyisipkan duplikat.</li>
 * <li>Komentar stub asli berkas ini berbunyi "Bank generated by hbm2java" — salah-salin dari
 * berkas lain (kelas ini tidak ada hubungannya dengan {@code Bank}) dan sudah digantikan oleh
 * dokumentasi ini.</li>
 * </ul>
 *
 * @see GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa
 * @see GelombangPendaftaranPsb
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "parameter_verifikasi_calon_siswa")



public class ParameterVerifikasiCalonSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap ini menjaga kompatibilitas objek yang sudah pernah
	 * diserialisasi (mis. ke dalam session ZK atau cache) ketika kelas berubah. Jangan diubah
	 * kecuali struktur field memang sengaja dibuat tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama baris, kolom {@code id}. Dideklarasikan ulang di sini (bukan diwarisi dari
	 * {@link GeneralValueObject}) karena kelas induk tidak dipetakan Hibernate.
	 */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi otomatis oleh interceptor audit. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; diisi otomatis oleh interceptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah, atau {@code null} bila baris belum pernah diubah
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>validasi non-trivial</b>: argumen
	 * {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b> sehingga nilai lama
	 * tetap bertahan. Jejak audit karenanya tidak dapat dikosongkan lewat setter ini.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan <b>validasi non-trivial</b> yang sama
	 * dengan {@link #setOlehId(String)}: argumen {@code null} atau kosong/spasi diabaikan
	 * diam-diam.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil kontainer persistence TEPAT SEBELUM pernyataan
	 * {@code UPDATE} dikirim, dan meneruskan objek ini ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari pengguna sesi aktif serta memperbarui
	 * {@code tanggal_dirubah}.
	 *
	 * <p><b>Hanya UPDATE.</b> Tidak ada {@code @PrePersist}, sehingga baris yang baru dibuat
	 * mengandalkan nilai awal field {@code tanggal_dirubah} (disetel
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat objek dibuat di JVM) dan tidak pernah mendapat
	 * {@code oleh}/{@code olehId} dari jalur ini. Pada entity ini efeknya kentara pada tiga baris
	 * hasil penanaman otomatis: kolom {@code oleh}/{@code olehid}-nya kosong sampai ada yang
	 * pertama kali mengubahnya.</p>
	 *
	 * <p><b>Catatan tata letak:</b> baris kode ini memuat DUA deklarasi sekaligus — method
	 * {@code onUpdate()} dan field {@code tanggal_dirubah} — hasil penyisipan otomatis oleh
	 * perkakas migrasi. Jangan dipisah tanpa alasan; Javadoc ini mendokumentasikan keduanya.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi; biasanya dipanggil interceptor audit,
	 * bukan kode layar.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini, dipetakan ke kolom {@code tanggal_dirubah}
	 * bertipe {@code TIMESTAMP} (nama kolom mengikuti nama properti apa adanya karena
	 * {@code MyNamingStrategy} adalah turunan {@code DefaultNamingStrategy}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat di JVM
	 *         karena field-nya diinisialisasi {@code ais.ui.util.WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris dalam format {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca <b>field</b> {@code nama} secara langsung, bukan lewat {@link #getNama()},
	 * sehingga nilainya <b>tidak di-{@code trim()}</b> dan spasi tepi (bila ada di database) ikut
	 * tampil. Untuk baris yang belum tersimpan, {@code id} masih {@code null} sehingga hasilnya
	 * berawalan {@code "null-"}.</p>
	 *
	 * <p>Tidak menimpa {@link GeneralValueObject#toString()} secara semantik berbeda; dipakai
	 * terutama saat objek muncul di pesan log/debug. Combobox "Tingkat" pada layar verifikasi
	 * <b>tidak</b> memakai method ini — ia mengambil properti {@code nama} secara eksplisit.</p>
	 *
	 * @return string {@code id + "-" + nama}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama tingkat sebagaimana tampil di combobox "Tingkat" (mis. "Prestasi Tingkat Nasional"). */
	private String nama;
	/** Keterangan bebas; layar master mengisinya, tidak ada pembaca yang menampilkannya di PSB. */
	private String keterangan;
	/** Saklar tampil/tidaknya baris di daftar pilihan; {@code null} diperlakukan sebagai aktif. */
	private Boolean aktif;
	/**
	 * Nomor urut tampil. Deklarasi ulang ini menyembunyikan field induk dan, bersama
	 * {@link #getNomorUrut()}, menjadi akar bug penciutan {@code TreeSet} yang diuraikan pada
	 * Javadoc kelas.
	 */
	private Integer nomorUrut;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Dipakai juga oleh layar master
	 * saat pengguna menekan "Tambah" dan oleh blok penanaman data awal. Semua field dibiarkan
	 * pada nilai bawaannya ({@code null}, kecuali {@code tanggal_dirubah}).
	 */
	public ParameterVerifikasiCalonSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Dipetakan {@code @Id} dengan {@code @GeneratedValue(strategy = IDENTITY)} — nilainya
	 * dibangkitkan database, karena itu kolomnya {@code insertable = false}. Bernilai {@code null}
	 * selama objek belum di-{@code save}; layar master memakai fakta ini untuk membedakan mode
	 * "Tambah" dari "Ubah".</p>
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
	 * Menyetel kunci utama baris. Tanpa validasi; dipanggil Hibernate, bukan kode layar.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama tingkat, sudah di-{@code trim()}.
	 *
	 * <p><b>Bukan getter destruktif:</b> hasil {@code trim()} hanya dikembalikan, <b>tidak</b>
	 * ditugaskan kembali ke field — sehingga sekadar merender grid tidak memicu {@code UPDATE}
	 * atau revisi Envers palsu. Nilai kembalian {@code null} bila field belum diisi.</p>
	 *
	 * <p>Kolom {@code nama} bersifat {@code nullable = false} sepanjang 255 karakter; layar master
	 * mewajibkan pengisiannya dan menolak nama yang sudah ada (pemeriksaan aplikasi, bukan
	 * constraint database, dan bersifat <i>case-sensitive</i>).</p>
	 *
	 * @return nama tingkat tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama tingkat. Tanpa validasi maupun {@code trim()} di sini — pemangkasan dilakukan
	 * pemanggil (layar master menyimpan {@code nama.getValue()} apa adanya pada jalur "Simpan",
	 * namun mem-{@code trim()} pada jalur penanaman data awal).
	 *
	 * @param nama nama tingkat baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini, apa adanya (dapat {@code null}).
	 *
	 * <p>Meng-override {@link GeneralValueObject#getKeterangan()}. Kolom {@code keterangan}
	 * bersifat {@code nullable = true} dan hanya ditampilkan di grid layar master; tidak ada
	 * pembaca di alur PSB yang menampilkannya kepada calon siswa.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif baris, dengan <b>nilai baku {@code true} bila field
	 * {@code null}</b>.
	 *
	 * <p>Normalisasi ini hanya terjadi pada nilai kembalian — field tidak ditulis — sehingga
	 * tidak ada {@code UPDATE} hantu. Yang penting: satu-satunya query pembaca
	 * ({@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswaAction}) memakai
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}, jadi <b>konsisten</b> dengan
	 * getter ini. Berbeda dari beberapa katalog master lain di repo, baris baru yang belum pernah
	 * disentuh checkbox tetap langsung terpilih — bukan instance bug "aktif tak pernah ditulis".</p>
	 *
	 * <p>Tanpa {@code @Column}, sehingga kolom fisiknya bernama {@code aktif} mengikuti
	 * {@code MyNamingStrategy}.</p>
	 *
	 * @return {@code true} bila baris aktif atau statusnya belum pernah diisi; {@code false} bila
	 *         sengaja dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif baris. Tanpa validasi.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Aktif" pada grid layar master, yang
	 * <b>digerbangi dengan benar</b> ({@code checkbox.setDisabled(!edit)}) dan langsung
	 * menyimpan lewat {@code Common.refreshSaveOrUpdate(...)}. Menonaktifkan baris jauh lebih
	 * aman daripada menghapusnya — lihat catatan NPE laten pada Javadoc kelas.</p>
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk menyembunyikan dari daftar
	 *              pilihan; {@code null} diperlakukan sama dengan {@code true} saat dibaca
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil, dengan <b>nilai baku {@code 1} bila field {@code null}</b>.
	 *
	 * <p><b>PALING PENTING pada kelas ini.</b> Method ini meng-override
	 * {@link GeneralValueObject#getNomorUrut()} — yang mengembalikan {@code null} apa adanya —
	 * sehingga <b>tidak pernah {@code null}</b>. Karena
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} memakai {@code getNomorUrut()}
	 * sebagai kunci urut PERTAMA dan hanya turun ke NIM/nama/keterangan bila salah satu sisi
	 * {@code null}, dua baris katalog yang sama-sama belum diberi nomor urut akan menghasilkan
	 * {@code compareTo == 0} — yaitu <b>dianggap duplikat</b> oleh {@code TreeSet}/{@code TreeMap}.</p>
	 *
	 * <p>Inilah akar bug kehilangan data senyap pada
	 * {@link GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa#getParameterVerifikasiCalonSiswas()}:
	 * saat sebuah kategori verifikasi <b>baru</b> disimpan pertama kali, koleksinya masih
	 * {@code new TreeSet<>()} sehingga hanya <b>satu</b> tingkat yang tersimpan berapa pun yang
	 * dicentang pengguna. Mengisi nomor urut yang berbeda-beda untuk tiap baris menyembuhkan
	 * gejalanya; menyeragamkannya menghidupkannya kembali.</p>
	 *
	 * <p>Nilai ini juga dipakai sebagai kunci {@code Order.asc("nomorUrut")} pada query daftar
	 * checkbox. Perhatikan bahwa pengurutan SQL memakai nilai kolom sesungguhnya — jadi baris
	 * ber-{@code NULL} diurutkan menurut aturan {@code NULL} database, bukan menurut nilai baku
	 * {@code 1} milik getter ini. Kedua jalur karenanya bisa memberi urutan berbeda.</p>
	 *
	 * <p>Tanpa {@code @Column}, sehingga kolom fisiknya bernama {@code nomorUrut} (camelCase,
	 * menyimpang dari konvensi snake_case skema {@code sekolah}).</p>
	 *
	 * @return nomor urut tampil; {@code 1} bila belum pernah diisi — tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil. Tanpa validasi (nilai negatif atau ganda diterima).
	 *
	 * <p><b>Titik tulis tanpa gerbang hak akses.</b> Satu-satunya pemanggil di UI adalah listener
	 * {@code onChange} dari {@code Intbox} nomor urut pada grid layar master — dan {@code Intbox}
	 * itu dibuat <b>tanpa {@code setDisabled}</b>, berbeda dari checkbox "Aktif" di sebelahnya
	 * yang dikunci {@code !edit}. Perubahannya langsung dipersistensikan lewat
	 * {@code Common.refreshUpdate(...)}, sehingga pemegang hak <b>BACA saja</b> dapat mengubah
	 * urutan katalog global — dan, lewat mekanisme pada {@link #getNomorUrut()}, ikut menentukan
	 * apakah bug penciutan {@code TreeSet} aktif atau tidak.</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} akan dibaca kembali sebagai {@code 1}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

}
