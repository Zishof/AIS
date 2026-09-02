package ais.database.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Entity direktori akun tabel <b>{@code public._user_access}</b> &mdash; salinan kredensial dan
 * status akun yang <i>dimaksudkan</i> menjadi basis data pengguna Spring Security internal AIS,
 * berpasangan dengan {@link UserRole} (tabel penghubung {@code user_role}) dan {@link RoleAccess}
 * (tabel peran {@code role_access}). Bentuk fieldnya menyalin persis kontrak
 * {@code org.springframework.security.core.userdetails.UserDetails}: {@code username},
 * {@code password}, {@code enabled}, {@code accountExpired}, {@code accountLocked},
 * {@code passwordExpired}, ditambah data profil ringan ({@code email}, {@code firstName},
 * {@code lastName}, {@code nama}, {@code keterangan}).
 *
 * <h2>PERINGATAN UTAMA &mdash; tabel ini HANYA DITULIS, TIDAK PERNAH DIBACA</h2>
 * <p>Nama kelas ini menyesatkan. Meskipun namanya menyiratkan pengendali hak akses, <b>tidak ada
 * satu baris kode pun di pohon sumber ini yang membaca {@code _user_access} untuk autentikasi
 * maupun otorisasi</b>. Verifikasi (2 Sep 2026):</p>
 * <ul>
 *   <li>{@code WEB-INF/applicationContext-security.xml} mendaftarkan
 *   {@code ais.common.UserDetailsServiceImpl} sebagai {@code user-service-ref} &mdash; bukan
 *   {@code jdbc-user-service} yang membaca tabel ini.</li>
 *   <li>{@code UserDetailsServiceImpl.getUserDetails(String)} <b>tidak menyentuh sama sekali</b>
 *   {@code UserAccess}/{@code UserRole}/{@code RoleAccess}. Ia membaca peta statis
 *   {@code SecurityFilter.dataLogin} dan membangun {@code UserDetails} dari lima entity lain:
 *   {@link Tbmuser}, {@code Mahasiswa}, {@code sekolah.Siswa}, {@code sisdes.Penduduk}, dan
 *   {@code BiodataCalonMahasiswa}. Password diambil dengan <i>mendekripsi</i> kolom password
 *   entity-entity tersebut, bukan dari {@link #getPassword()} di sini.</li>
 *   <li>Peran yang benar-benar ditegakkan hanya {@code ROLE_SUPERVISOR} (aturan
 *   {@code /pages/maintenance/job/**}) dan {@code ROLE_USER}, keduanya diterbitkan
 *   {@code UserDetailsServiceImpl} dari {@code Tbmuser.getRoot()}/{@code Tbmuser.hakAkses()}.
 *   Nilai {@code ROLE_MAHASISWA}/{@code ROLE_DOSEN} yang ditulis ke {@link RoleAccess} lewat
 *   {@code Common.saveOrUpdateUserAccess(...)} <b>tidak dirujuk di berkas konfigurasi keamanan
 *   mana pun</b> dan tidak pernah dipetakan menjadi {@code GrantedAuthority}.</li>
 *   <li>Otorisasi layar/menu sehari-hari memakai jalur yang sama sekali lain:
 *   {@link Tbmrole}/{@code CommonPrivilages}/{@link Menu}.</li>
 * </ul>
 * <p>Konsekuensi yang harus dipahami sebelum menyentuh kelas ini: <b>seluruh flag status akun di
 * sini adalah kontrol keamanan semu</b>. Menyetel {@link #setEnabled(Boolean) enabled}{@code =false},
 * {@link #setAccountLocked(Boolean) accountLocked}{@code =true}, atau
 * {@link #setPasswordExpired(Boolean) passwordExpired}{@code =true} <b>tidak mengunci siapa pun</b>
 * &mdash; pengguna tetap dapat login lewat jalur normal. Sebaliknya, menghapus baris di sini juga
 * tidak mencabut akses. Jangan pernah memakai tabel ini sebagai dasar keputusan keamanan tanpa
 * lebih dulu membangun jalur pembacaannya.</p>
 *
 * <h2>Siapa yang menulis ke tabel ini</h2>
 * <p>Praktis hanya satu penulis: {@code ais.common.Common#saveOrUpdateUserAccess(Tbmuser,
 * Mahasiswa, String, String, String)}. Method itu membuka {@code Session} + transaksi
 * <i>dedikasi</i> sendiri (bukan sesi ZK), mencari baris berdasarkan {@code username}, membuatnya
 * bila belum ada, lalu menyetel {@code email} (hanya alamat pertama sebelum koma),
 * {@code enabled=true}, {@code firstName=username}, dan {@code password=MD5.crypt(password)}.
 * Pemanggilnya tersebar di seluruh titik pembuatan akun/ganti kata sandi:</p>
 * <ul>
 *   <li>{@code ais.action.maintenance.TbmuserAction} (master pengguna sistem),</li>
 *   <li>{@code ais.action.master.DosenAction}, {@code ais.action.master.PerkuliahanAction},
 *   {@code ais.action.master.sekolah.GuruAction},
 *   {@code ais.action.master.recruitment.CalonPegawaiAction},</li>
 *   <li>{@code ais.action.master.MahasiswaAction} (akun mahasiswa <i>dan</i> akun turunan orang
 *   tua lewat {@code userOrtu}),</li>
 *   <li>{@code ais.action.master.helper.ChangePasswordWindow} dan
 *   {@code ais.action.servlet.api.PasswordSayaApi} (ubah kata sandi mandiri),</li>
 *   <li>{@code ais.common.helper.DownloadPasswordPegawaiHelper} (cetak/unduh kata sandi pegawai).</li>
 * </ul>
 * <p>Satu-satunya pemanggil yang mengubah baris ini <i>secara langsung</i> (tanpa lewat helper di
 * atas) adalah {@code MahasiswaAction} pada prosedur ganti NIM: baris dengan {@code username} =
 * NIM lama dicari, lalu {@code username} dan {@code firstName}-nya ditulis ulang menjadi NIM baru
 * &mdash; itu pun hanya bila belum ada baris ber-{@code username} NIM baru.</p>
 * <p><b>Tidak ada kode yang menghapus baris di sini.</b> Akun yang dinonaktifkan atau dihapus di
 * {@link Tbmuser}/{@code Mahasiswa} meninggalkan barisnya di {@code _user_access} selamanya,
 * lengkap dengan hash kata sandi terakhirnya.</p>
 *
 * <h2>Jebakan penamaan: {@code Common.setUserAccess(...)} BUKAN tentang kelas ini</h2>
 * <p>{@code Common.setUserAccess(HttpServletRequest)}, {@code Common.setUserAccess(Session,
 * HttpServletRequest)}, dan {@code Common.removeUserAccess()} &mdash; yang dipanggil dari
 * {@code SecurityFilter} serta belasan Action ({@code LoginAction}, {@code AlumniAction},
 * {@code PMBAction}, {@code PSBAction}, dan lain-lain) &mdash; mendelegasi ke
 * {@code CommonMenuAccessHelper} dan mengurus objek {@code AccessedUsers} (sesi pengguna aktif dan
 * hak menu). Semuanya <b>tidak ada hubungannya</b> dengan entity ini. Kesamaan nama murni
 * kebetulan; jangan tertukar saat menelusuri pemanggil.</p>
 *
 * <h2>Pemetaan Hibernate</h2>
 * <ul>
 *   <li>Akses properti (annotasi {@link Id @Id} berada di {@link #getId()}), maka <b>getter-lah
 *   yang dipanggil Hibernate</b> saat memuat, menyimpan, dan melakukan <i>dirty check</i> &mdash;
 *   lihat bagian berikutnya.</li>
 *   <li>{@code dynamicInsert}/{@code dynamicUpdate} aktif: hanya kolom yang benar-benar berubah
 *   yang ikut di {@code INSERT}/{@code UPDATE}.</li>
 *   <li>{@link Audited @Audited} (Hibernate Envers): setiap perubahan menghasilkan baris riwayat
 *   di tabel {@code _user_access_AUD}. <b>Termasuk kolom {@code password}</b>, sehingga seluruh
 *   hash kata sandi lama tersimpan permanen di tabel audit.</li>
 *   <li>Kolom tanpa {@link Column @Column} eksplisit ({@code username}, {@code password},
 *   {@code enabled}, {@code firstName}, {@code lastName}, {@code oleh}, {@code olehId},
 *   {@code tanggal_dirubah}) mengikuti {@code ais.database.hibernate.MyNamingStrategy}, yang
 *   memperluas {@code DefaultNamingStrategy} tanpa mengubah aturan kolom &mdash; artinya <b>nama
 *   kolom sama persis dengan nama properti, termasuk huruf besar</b> ({@code firstName},
 *   {@code lastName}, {@code olehId}), bukan {@code first_name} gaya {@code ImprovedNamingStrategy}.</li>
 *   <li>{@code username} <b>tidak</b> diberi {@code unique = true}; keunikan hanya "dijaga" secara
 *   longgar oleh pencarian {@code setMaxResults(1)} di helper penulisnya.</li>
 * </ul>
 *
 * <h2>Getter yang MEMUTASI state (auto-normalisasi)</h2>
 * <p>Diverifikasi langsung dari kode kelas ini &mdash; empat getter menulis balik ke fieldnya
 * sendiri saat dibaca:</p>
 * <ul>
 *   <li>{@link #getEmail()} &mdash; merapikan koma ganda dan mengubah {@code null}/{@code ","}
 *   menjadi {@code ""};</li>
 *   <li>{@link #getAccountExpired()}, {@link #getAccountLocked()}, {@link #getPasswordExpired()}
 *   &mdash; mengubah {@code null} menjadi {@code false}.</li>
 * </ul>
 * <p>Karena pemetaan memakai akses properti, keempatnya juga dipanggil Hibernate saat
 * <i>dirty check</i>. Pada baris lama yang kolomnya masih {@code NULL}, sekadar memuat entity di
 * dalam sesi yang kemudian di-{@code flush} sudah cukup untuk memicu {@code UPDATE} nyata,
 * menjalankan kait {@link #onUpdate()}, dan mencatat revisi Envers atas nama pengguna yang
 * kebetulan sedang aktif. Tidak ada getter di kelas ini yang membuka atau menutup
 * {@code Session} Hibernate, dan tidak ada getter yang menghapus data relasional &mdash; efeknya
 * murni normalisasi nilai kolomnya sendiri.</p>
 *
 * <h2>Deklarasi ulang field dari kelas induk</h2>
 * <p>{@code id}, {@code nama}, {@code keterangan}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} dideklarasikan ulang di sini padahal {@link GeneralValueObject} juga
 * memilikinya. Ini <b>bukan bug</b>: {@link GeneralValueObject} adalah POJO abstrak biasa, bukan
 * {@code @Entity} maupun {@code @MappedSuperclass}, sehingga Hibernate tidak memetakan properti
 * induk. Setiap entity yang butuh kolom-kolom tersebut wajib mendeklarasikannya kembali.</p>
 * <p><b>Catatan halus:</b> karena {@link #getKeterangan()} di sini meng-<i>override</i> versi induk
 * <b>tanpa</b> normalisasi {@code null}&rarr;{@code ""}, jaminan yang didokumentasikan pada
 * {@code GeneralValueObject.compareTo(GeneralValueObject)} ("cabang {@code keterangan} selalu
 * memenuhi syarat non-null") <b>tidak berlaku</b> untuk {@code UserAccess}. Ditambah
 * {@code nomorUrut}/{@code nim} yang tidak pernah diisi untuk tipe ini, pengurutan alami dua
 * {@code UserAccess} praktis bergantung pada {@link #getNama()} saja, dan mengembalikan {@code 0}
 * ("setara") bila keduanya bernama {@code null}.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Identitas &amp; jejak audit</b>: {@link #getId()}, {@link #getOleh()},
 *   {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Kredensial</b>: {@link #getUsername()}, {@link #getPassword()}.</li>
 *   <li><b>Status akun (semu &mdash; lihat peringatan di atas)</b>: {@link #getEnabled()},
 *   {@link #getAccountExpired()}, {@link #getAccountLocked()}, {@link #getPasswordExpired()}.</li>
 *   <li><b>Profil</b>: {@link #getNama()}, {@link #getKeterangan()}, {@link #getFirstName()},
 *   {@link #getLastName()}, {@link #getEmail()}, {@link #appendEmail(String)}.</li>
 *   <li><b>Representasi</b>: {@link #toString()}.</li>
 * </ol>
 * <p>Tidak ada method bisnis, tidak ada query statis, dan tidak ada relasi yang dipetakan dari
 * sisi kelas ini. Arah relasi ke peran dimiliki {@link UserRole} ({@code @ManyToOne} ke
 * {@code UserAccess} lewat kolom {@code user_id}); tidak ada koleksi balik {@code @OneToMany} di
 * sini, jadi penelusuran peran suatu {@code UserAccess} harus lewat query ke {@link UserRole}.</p>
 *
 * <h2>TEMUAN KEAMANAN yang perlu ditindaklanjuti terpisah</h2>
 * <ol>
 *   <li><b>Gudang kredensial bayangan tanpa fungsi.</b> Setiap kali kata sandi staf, dosen, guru,
 *   mahasiswa, atau orang tua dibuat/diubah, salinan {@code MD5} <b>tanpa salt</b> dari kata sandi
 *   tersebut ikut ditulis ke {@code _user_access.password} (dan ke {@code _user_access_AUD}).
 *   Karena tidak ada yang membacanya, ini adalah permukaan serangan murni: risiko tanpa manfaat.
 *   MD5 tanpa salt sudah tidak layak untuk kata sandi, dan di AIS kata sandi awal umumnya sama
 *   dengan NIM/ID pengguna sehingga pemulihan lewat tabel pelangi menjadi sepele.</li>
 *   <li><b>Kontrol keamanan semu</b> (pola berulang yang sudah dicatat pada {@code Ruang.ip} dan
 *   {@code Menu.aktif}): {@code enabled}/{@code accountExpired}/{@code accountLocked}/
 *   {@code passwordExpired} tampak persis seperti sakelar penguncian akun standar Spring Security,
 *   tetapi tidak ditegakkan di mana pun. Operator yang mengira sudah "mengunci" akun lewat kolom
 *   ini keliru. Kolom {@code enabled} bahkan selalu dipaksa {@code true} oleh helper penulisnya
 *   setiap kali kata sandi diubah.</li>
 *   <li><b>Peran yatim.</b> Baris {@link RoleAccess} {@code ROLE_MAHASISWA}/{@code ROLE_DOSEN} dan
 *   {@link UserRole} yang menghubungkannya dibuat rutin, tetapi tidak pernah menjadi
 *   {@code GrantedAuthority} maupun dirujuk aturan {@code intercept-url}. Bila suatu saat jalur
 *   pembacaan tabel ini "dihidupkan" (mis. beralih ke {@code jdbc-user-service}), data yang sudah
 *   telanjur ada &mdash; termasuk baris akun lama yang tidak pernah dihapus &mdash; akan langsung
 *   menjadi kredensial aktif. Perlu pembersihan lebih dulu.</li>
 * </ol>
 * <p>Ketiganya masuk cakupan task audit hak akses yang sudah ada
 * ({@code task_c27d18e4}/{@code task_4180ddb8}); tidak ada perubahan logika yang dilakukan di
 * berkas ini.</p>
 *
 * <p><b>Catatan pemeliharaan:</b> komentar generator asli berbunyi "Bank generated by hbm2java"
 * &mdash; salah salin-tempel dari entity {@code Bank}, sama seperti yang sudah dikonfirmasi pada
 * {@code JamPerkuliahan}, {@code MasaPerkuliahan}, dan
 * {@code GelombangPendaftaranSidangTugasAkhir}. Jangan dijadikan petunjuk asal-usul tabel.</p>
 *
 * @see UserRole
 * @see RoleAccess
 * @see GeneralValueObject
 * @see Tbmuser
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "_user_access")

public class UserAccess extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi Java. Nilainya <b>identik</b> dengan yang dipakai {@link UserRole}
	 * dan beberapa entity lain hasil salin-tempel berkas ini; kesamaan tersebut tidak
	 * berkonsekuensi apa pun karena {@code serialVersionUID} hanya dibandingkan antar-versi kelas
	 * yang sama, bukan antar-kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci utama baris {@code _user_access}. Deklarasi ulang dari {@link GeneralValueObject};
	 * lihat Javadoc kelas.
	 */
	private Long id;

	/**
	 * Nama pengguna yang terakhir mengubah baris ini (jejak audit). Diisi
	 * {@code AuditTimestampInterceptor} lewat kait {@link #onUpdate()}. Deklarasi ulang dari
	 * {@link GeneralValueObject}.
	 */
	private String oleh;

	/**
	 * Id/NIP pengguna yang terakhir mengubah baris ini (jejak audit). Dipetakan ke kolom
	 * {@code olehId} apa adanya (lihat catatan penamaan kolom pada Javadoc kelas). Deklarasi ulang
	 * dari {@link GeneralValueObject}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id/NIP pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengubah terakhir, atau {@code null} bila baris belum pernah diubah lewat kait
	 *         audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id/NIP pengguna pengubah &mdash; <b>menolak nilai kosong</b>.
	 *
	 * <p>Bila argumen {@code null} atau hanya berisi spasi, method langsung {@code return} tanpa
	 * mengubah apa pun, sehingga jejak audit lama tidak bisa dihapus dengan menyetel string kosong.
	 * Umumnya dipanggil {@code AuditTimestampInterceptor}, bukan kode layar.</p>
	 *
	 * @param olehId id/NIP pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah &mdash; <b>menolak nilai kosong</b>, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengubah, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari konteks pengguna aktif serta
	 * memperbarui {@link #setTanggal_dirubah(Date)}. Tidak pernah dipanggil manual dari kode
	 * aplikasi, dan <b>tidak</b> berjalan pada {@code INSERT} (hanya {@code UPDATE}).</p>
	 *
	 * <p>Perhatikan interaksinya dengan getter auto-normalisasi yang didaftar pada Javadoc kelas:
	 * pembacaan biasa pun bisa membuat entity kotor, memicu {@code UPDATE}, memanggil kait ini,
	 * dan menghasilkan revisi Envers atas nama pengguna yang kebetulan sedang membuka layar.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke jam server saat object dibuat
	 * ({@code WaktuUtil.getDate()}), lalu diperbarui kait {@link #onUpdate()} pada setiap
	 * {@code UPDATE}. Deklarasi ulang dari {@link GeneralValueObject}; lihat Javadoc kelas.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Umumnya dipanggil
	 * {@code AuditTimestampInterceptor}, bukan kode layar.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris akun ini.
	 *
	 * <p>Tanpa {@link Column @Column}, sehingga jatuh ke penamaan default {@code MyNamingStrategy}
	 * &mdash; kolom {@code tanggal_dirubah} apa adanya, bertipe {@code TIMESTAMP}.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object yang baru dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas baris akun untuk keperluan log/debug, berformat {@code "id-nama"}.
	 *
	 * <p>Meng-<i>override</i> {@link GeneralValueObject#toString()} yang berformat
	 * {@code "kode - nama"}. Membaca field {@code nama} <b>langsung</b>, bukan lewat
	 * {@link #getNama()}, sehingga nilainya tidak di-{@code trim}. Bila {@code nama} belum diisi
	 * hasilnya berupa string berakhiran {@code "-null"}, dan pada object baru yang belum disimpan
	 * berupa {@code "null-null"}.</p>
	 *
	 * <p>Perhatikan: yang ditampilkan adalah {@code nama}, <b>bukan</b> {@link #getUsername()}
	 * &mdash; padahal {@code nama} praktis tidak pernah diisi oleh
	 * {@code Common.saveOrUpdateUserAccess(...)} (helper itu mengisi {@code firstName}, bukan
	 * {@code nama}). Jangan mengandalkan keluaran method ini untuk mengenali akun.</p>
	 *
	 * @return string {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama tampilan akun. Deklarasi ulang dari {@link GeneralValueObject}. Dalam praktiknya
	 * hampir selalu {@code NULL} di database karena tidak ada penulis yang mengisinya &mdash;
	 * lihat {@link #toString()}.
	 */
	private String nama;

	/**
	 * Keterangan bebas atas baris akun. Deklarasi ulang dari {@link GeneralValueObject}. Sama
	 * seperti {@link #nama}, tidak ada kode yang mengisinya.
	 */
	private String keterangan;

	/**
	 * Nama pengguna untuk login. Diisi dari {@code Tbmuser.getUserId()} untuk staf/dosen/guru,
	 * dari NIM untuk mahasiswa, atau dari {@code Mahasiswa.getUserOrtu()} untuk akun turunan orang
	 * tua. Menjadi kunci pencarian tunggal di seluruh penulis, namun <b>tanpa batasan unik di
	 * tingkat kolom</b>.
	 */
	private String username;

	/**
	 * Hash {@code MD5} <b>tanpa salt</b> dari kata sandi pengguna, dihasilkan
	 * {@code ais.common.MD5#crypt(String)}. Tidak pernah dipakai untuk memverifikasi login mana
	 * pun (lihat peringatan keamanan pada Javadoc kelas) dan seluruh nilai lamanya tersimpan
	 * permanen di tabel audit Envers {@code _user_access_AUD}.
	 */
	private String password;

	/**
	 * Flag "akun aktif" gaya Spring Security. Default {@code true} pada object baru, dan dipaksa
	 * {@code true} lagi oleh {@code Common.saveOrUpdateUserAccess(...)} setiap kali kata sandi
	 * pengguna diubah. <b>Tidak ditegakkan di mana pun</b> &mdash; menyetelnya {@code false} tidak
	 * memblokir login.
	 */
	private Boolean enabled = true;

	/**
	 * Flag "akun kedaluwarsa" gaya Spring Security. Default {@code false}; tidak ada kode yang
	 * pernah menyetelnya {@code true} maupun membacanya untuk keputusan akses.
	 */
	private Boolean accountExpired = false;

	/**
	 * Flag "akun terkunci" gaya Spring Security. Default {@code false}; tidak ditegakkan di mana
	 * pun &mdash; menyetelnya {@code true} <b>tidak</b> mengunci akun.
	 */
	private Boolean accountLocked = false;

	/**
	 * Flag "kata sandi kedaluwarsa" gaya Spring Security. Default {@code false}; tidak ditegakkan
	 * di mana pun &mdash; tidak ada mekanisme pemaksaan ganti kata sandi yang membacanya.
	 */
	private Boolean passwordExpired = false;

	/**
	 * Alamat surel pemilik akun. Disimpan sebagai satu string; secara desain dapat memuat beberapa
	 * alamat dipisah koma (lihat {@link #appendEmail(String)}), tetapi
	 * {@code Common.saveOrUpdateUserAccess(...)} selalu menyimpan <b>alamat pertama saja</b>.
	 * Dinormalisasi saat dibaca oleh {@link #getEmail()}.
	 */
	private String email;

	/**
	 * Nama depan gaya Spring Security. Oleh {@code Common.saveOrUpdateUserAccess(...)} diisi
	 * dengan <b>nilai {@code username}</b>, bukan nama depan sesungguhnya &mdash; jangan
	 * ditampilkan ke pengguna sebagai nama orang.
	 */
	private String firstName;

	/**
	 * Nama belakang gaya Spring Security. Tidak ada kode yang mengisinya; praktis selalu
	 * {@code NULL} di database.
	 */
	private String lastName;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk membuat instance saat memuat
	 * baris. Tidak menyetel apa pun selain nilai awal field ({@link #enabled} {@code true},
	 * ketiga flag lain {@code false}, {@link #tanggal_dirubah} = jam server).
	 */
	public UserAccess() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Nilai dibangkitkan {@link GenerationType#AUTO}, sehingga pada PostgreSQL diambil dari
	 * sequence Hibernate, bukan dari kolom {@code serial} milik tabel. Method ini juga menjadi
	 * dasar {@link GeneralValueObject#equals(Object)} (perbandingan berbasis id lewat pemanggilan
	 * polimorfik), sehingga dua object {@code UserAccess} yang belum disimpan
	 * ({@code id == null}) selalu dianggap berbeda.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Umumnya hanya dipanggil Hibernate; jangan diubah pada entity
	 * yang sedang dikelola sesi.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama tampilan akun, sudah di-{@code trim}.
	 *
	 * <p>Meng-<i>override</i> {@link GeneralValueObject#getNama()}. Hasil {@code trim} <b>tidak</b>
	 * ditulis balik ke field, jadi method ini tidak memutasi state (berbeda dari
	 * {@link #getEmail()}).</p>
	 *
	 * @return nama akun tanpa spasi di tepi, atau {@code null} bila belum diisi &mdash; yang dalam
	 *         praktik hampir selalu terjadi
	 */
	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama tampilan akun. Tanpa validasi.
	 *
	 * @param nama nama baru; boleh {@code null}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan baris apa adanya.
	 *
	 * <p><b>Berbeda dari induknya:</b> {@link GeneralValueObject#getKeterangan()} menormalkan
	 * {@code null} menjadi {@code ""}, sedangkan override ini tidak. Akibatnya jaminan yang
	 * didokumentasikan pada {@code GeneralValueObject.compareTo(GeneralValueObject)} &mdash; bahwa
	 * cabang {@code keterangan} selalu tersedia &mdash; tidak berlaku untuk tipe ini. Pemanggil
	 * wajib memeriksa {@code null} sendiri.</p>
	 *
	 * @return keterangan baris, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan baris. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama pengguna login pada baris ini.
	 *
	 * <p>Inilah satu-satunya kunci pencarian yang dipakai
	 * {@code Common.saveOrUpdateUserAccess(...)} dan prosedur ganti NIM di
	 * {@code MahasiswaAction}. Tanpa {@link Column @Column}, sehingga dipetakan ke kolom
	 * {@code username} apa adanya dan <b>tanpa batasan unik</b>.</p>
	 *
	 * @return username akun, atau {@code null} bila belum diisi
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Menyetel nama pengguna login. Tanpa validasi dan tanpa pemeriksaan duplikasi.
	 *
	 * <p>Dipanggil saat baris baru dibuat ({@code Common.saveOrUpdateUserAccess(...)}) dan saat NIM
	 * mahasiswa diganti ({@code MahasiswaAction} menulis ulang {@code username} dan
	 * {@link #setFirstName(String) firstName} menjadi NIM baru, hanya bila belum ada baris lain
	 * dengan NIM baru tersebut &mdash; bila sudah ada, baris lama dibiarkan memakai NIM lama dan
	 * menjadi yatim).</p>
	 *
	 * @param username username baru
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Mengembalikan hash kata sandi yang tersimpan.
	 *
	 * <p><b>Tidak pernah dipanggil untuk memverifikasi login.</b> Autentikasi AIS berjalan lewat
	 * {@code UserDetailsServiceImpl} yang mendekripsi kolom password pada {@link Tbmuser},
	 * {@code Mahasiswa}, {@code Siswa}, {@code Penduduk}, atau memakai nomor registrasi pada
	 * {@code BiodataCalonMahasiswa}. Nilai di sini murni salinan mati &mdash; lihat peringatan
	 * keamanan pada Javadoc kelas.</p>
	 *
	 * @return hash MD5 heksadesimal 32 karakter, atau {@code null} bila belum pernah diisi
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Menyetel hash kata sandi. <b>Tidak melakukan hashing sendiri</b> &mdash; pemanggil wajib
	 * menyerahkan nilai yang sudah di-hash.
	 *
	 * <p>Satu-satunya pemanggil, {@code Common.saveOrUpdateUserAccess(...)}, menyerahkan
	 * {@code MD5.crypt(password.trim())}. Karena entity ini {@link Audited @Audited}, setiap
	 * perubahan nilai di sini juga mengendap permanen di tabel revisi
	 * {@code _user_access_AUD}.</p>
	 *
	 * @param password hash kata sandi yang sudah dihitung pemanggil
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Mengembalikan flag "akun aktif".
	 *
	 * <p>Tidak menormalkan {@code null} (berbeda dari ketiga flag status lainnya), sehingga dapat
	 * mengembalikan {@code null} untuk baris lama yang kolomnya {@code NULL} &mdash; hati-hati
	 * dengan auto-unboxing di sisi pemanggil. Nilai ini <b>tidak menentukan apa pun</b> secara
	 * fungsional; lihat peringatan keamanan pada Javadoc kelas.</p>
	 *
	 * @return {@code true}/{@code false}, atau {@code null} bila kolom belum pernah diisi
	 */
	public Boolean getEnabled() {
		return enabled;
	}

	/**
	 * Menyetel flag "akun aktif". Dipaksa {@code true} oleh
	 * {@code Common.saveOrUpdateUserAccess(...)} pada setiap pembuatan akun maupun perubahan kata
	 * sandi, jadi nilai {@code false} yang disetel manual akan tertimpa pada perubahan kata sandi
	 * berikutnya.
	 *
	 * @param enabled status aktif baru
	 */
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Mengembalikan alamat surel akun <b>sambil merapikannya di tempat</b>.
	 *
	 * <p>Getter ini <b>memutasi field</b> {@link #email} sebelum mengembalikannya:</p>
	 * <ol>
	 *   <li>bila mengandung {@code ",,"}, {@code replaceAll(",,", ",")} dijalankan <b>tepat lima
	 *   kali</b> &mdash; cukup untuk merapikan deretan hingga sekitar 32 koma beruntun, tetapi
	 *   deretan yang lebih panjang masih menyisakan koma ganda;</li>
	 *   <li>{@code null} diubah menjadi string kosong;</li>
	 *   <li>nilai yang setelah di-{@code trim} tepat sama dengan {@code ","} diubah menjadi string
	 *   kosong. Perhatikan pemeriksaan ini hanya menangkap kasus persis satu koma &mdash; sisa
	 *   koma di awal/akhir seperti {@code "a@b.com,"} atau {@code ", ,"} tetap lolos.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> karena pemetaan memakai akses properti, Hibernate memanggil method
	 * ini saat <i>dirty check</i>. Membuka baris lama yang kolom {@code email}-nya {@code NULL} di
	 * dalam sesi yang kemudian di-{@code flush} sudah cukup untuk menghasilkan {@code UPDATE}
	 * (dari {@code NULL} menjadi {@code ''}), memanggil {@link #onUpdate()}, dan mencatat revisi
	 * Envers baru atas nama pengguna yang kebetulan sedang aktif.</p>
	 *
	 * @return alamat surel yang sudah dinormalisasi; tidak pernah {@code null} setelah method ini
	 *         dijalankan
	 */
	@Column(name = "email", length = 255)
	public String getEmail() {
		if (email != null && email.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				email = email.replaceAll(",,", ",");
			}
		}
		if (email == null) {
			email = "";
		}
		if (email.trim().equals(",")) {
			email = "";
		}
		return this.email;
	}

	/**
	 * Menyetel alamat surel akun, menimpa seluruh nilai lama. Tanpa validasi format.
	 *
	 * <p>{@code Common.saveOrUpdateUserAccess(...)} memanggilnya dengan <b>alamat pertama saja</b>
	 * ({@code email.trim().split(",")[0].trim()}), atau string kosong bila argumennya kosong
	 * &mdash; sehingga daftar surel majemuk yang dimiliki entity sumber tidak pernah tersalin utuh
	 * ke sini.</p>
	 *
	 * @param email alamat surel baru; boleh {@code null}
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Menambahkan satu alamat surel ke daftar (dipisahkan koma) <b>bila lolos semua penyaring</b>.
	 *
	 * <p>Urutan pemeriksaan:</p>
	 * <ol>
	 *   <li>Bila field {@link #email} sudah terisi, argumen tidak kosong, dan
	 *   {@code StringUtils.contains(this.email, email)} bernilai {@code true}, method
	 *   {@code return} tanpa berbuat apa-apa. <b>Kuirk:</b> ini pencocokan <i>substring</i>, bukan
	 *   pencocokan elemen &mdash; alamat {@code "ani@x.ac.id"} akan dianggap sudah ada bila daftar
	 *   memuat {@code "budi.ani@x.ac.id"}, sehingga penambahan yang sah bisa terlewat diam-diam.</li>
	 *   <li>Alamat ditambahkan hanya bila tidak kosong, lolos
	 *   {@code Common.isValidEmailAddress(...)}, dan tidak diawali {@code "@"}.</li>
	 *   <li>Bila field masih kosong/{@code null}, alamat menjadi nilai pertama; bila sudah terisi,
	 *   alamat disambung dengan pemisah koma.</li>
	 * </ol>
	 *
	 * <p>Argumen yang gagal validasi dibuang <b>diam-diam</b> (tidak ada exception, tidak ada
	 * nilai balik) &mdash; pemanggil tidak dapat membedakan "berhasil ditambahkan" dari
	 * "ditolak".</p>
	 *
	 * <p><b>Catatan:</b> pada tipe ini method tersebut <b>tidak pernah dipanggil</b>. Ia merupakan
	 * salinan dari method senama pada {@code Mahasiswa}/{@code Siswa}/{@code Penduduk}/
	 * {@link Tbmuser} (yang memang dipakai dari {@code ApiUtil} dan {@code Common}), sehingga di
	 * sini efektif berupa kode mati.</p>
	 *
	 * @param email alamat surel yang akan ditambahkan; {@code null}/kosong/tidak valid diabaikan
	 */
	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	/**
	 * Mengembalikan isi kolom {@code firstName}.
	 *
	 * <p><b>Bukan nama depan sesungguhnya.</b> {@code Common.saveOrUpdateUserAccess(...)} mengisi
	 * kolom ini dengan nilai {@code username} (user id staf atau NIM mahasiswa), demikian pula
	 * prosedur ganti NIM di {@code MahasiswaAction}.</p>
	 *
	 * @return isi kolom {@code firstName}, atau {@code null} bila belum diisi
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * Menyetel isi kolom {@code firstName}. Tanpa validasi; lihat catatan pada
	 * {@link #getFirstName()} soal isi sebenarnya.
	 *
	 * @param firstName nilai baru
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	/**
	 * Mengembalikan isi kolom {@code lastName}. Tidak ada kode yang pernah mengisinya, sehingga
	 * praktis selalu {@code null}.
	 *
	 * @return isi kolom {@code lastName}, atau {@code null}
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * Menyetel isi kolom {@code lastName}. Tanpa validasi; tidak ada pemanggil di pohon sumber ini.
	 *
	 * @param lastName nilai baru
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	/**
	 * Mengembalikan flag "akun kedaluwarsa", <b>dengan penormalan yang menulis balik ke field</b>:
	 * nilai {@code null} diubah menjadi {@code false} sebelum dikembalikan.
	 *
	 * <p>Karena pemetaan memakai akses properti, penormalan ini ikut berjalan pada <i>dirty
	 * check</i> Hibernate &mdash; baris lama yang kolomnya {@code NULL} akan ter-{@code UPDATE}
	 * hanya karena dibaca. Nilai flag itu sendiri tidak menentukan apa pun secara fungsional;
	 * lihat peringatan keamanan pada Javadoc kelas.</p>
	 *
	 * @return {@code true} atau {@code false}; tidak pernah {@code null}
	 */
	@Column(name = "account_expired", nullable = true)
	public Boolean getAccountExpired() {
		if (accountExpired == null) {
			accountExpired = false;
		}
		return accountExpired;
	}

	/**
	 * Menyetel flag "akun kedaluwarsa". Tidak ada pemanggil di pohon sumber ini, dan nilainya tidak
	 * ditegakkan di jalur autentikasi mana pun.
	 *
	 * @param accountExpired nilai baru; {@code null} akan dinormalkan menjadi {@code false} pada
	 *                       pembacaan berikutnya oleh {@link #getAccountExpired()}
	 */
	public void setAccountExpired(Boolean accountExpired) {
		this.accountExpired = accountExpired;
	}

	/**
	 * Mengembalikan flag "akun terkunci", <b>dengan penormalan yang menulis balik ke field</b>:
	 * nilai {@code null} diubah menjadi {@code false} sebelum dikembalikan. Perilaku dan efek
	 * sampingnya identik dengan {@link #getAccountExpired()}.
	 *
	 * <p><b>Penting:</b> flag ini <b>tidak mengunci akun</b>. Tidak ada kode di AIS yang
	 * membacanya saat login; lihat peringatan keamanan pada Javadoc kelas.</p>
	 *
	 * @return {@code true} atau {@code false}; tidak pernah {@code null}
	 */
	@Column(name = "account_locked", nullable = true)
	public Boolean getAccountLocked() {
		if (accountLocked == null) {
			accountLocked = false;
		}
		return accountLocked;
	}

	/**
	 * Menyetel flag "akun terkunci". Tidak ada pemanggil di pohon sumber ini, dan menyetelnya
	 * {@code true} <b>tidak</b> memblokir login pengguna.
	 *
	 * @param accountLocked nilai baru; {@code null} akan dinormalkan menjadi {@code false} pada
	 *                      pembacaan berikutnya oleh {@link #getAccountLocked()}
	 */
	public void setAccountLocked(Boolean accountLocked) {
		this.accountLocked = accountLocked;
	}

	/**
	 * Mengembalikan flag "kata sandi kedaluwarsa", <b>dengan penormalan yang menulis balik ke
	 * field</b>: nilai {@code null} diubah menjadi {@code false} sebelum dikembalikan. Perilaku dan
	 * efek sampingnya identik dengan {@link #getAccountExpired()}.
	 *
	 * <p>Tidak ada mekanisme pemaksaan ganti kata sandi di AIS yang membaca flag ini.</p>
	 *
	 * @return {@code true} atau {@code false}; tidak pernah {@code null}
	 */
	@Column(name = "password_expired", nullable = true)
	public Boolean getPasswordExpired() {
		if (passwordExpired == null) {
			passwordExpired = false;
		}
		return passwordExpired;
	}

	/**
	 * Menyetel flag "kata sandi kedaluwarsa". Tidak ada pemanggil di pohon sumber ini, dan nilainya
	 * tidak ditegakkan di jalur autentikasi mana pun.
	 *
	 * @param passwordExpired nilai baru; {@code null} akan dinormalkan menjadi {@code false} pada
	 *                        pembacaan berikutnya oleh {@link #getPasswordExpired()}
	 */
	public void setPasswordExpired(Boolean passwordExpired) {
		this.passwordExpired = passwordExpired;
	}

}
