package ais.database.model;

// Generated Apr 5, 2010 4:05:36 PM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.envers.Audited;

/**
 * Entity <b>hak akses granular</b> (tabel {@code public.role_privilage}): satu baris menautkan
 * <b>satu</b> {@link Menu} ke <b>satu</b> {@link Tbmrole} dan menyimpan enam flag operasi
 * — {@link #getRead() read}, {@link #getCreate() create}, {@link #getUpdate() update},
 * {@link #getDelete() delete}, {@link #getApprove() approve}, {@link #getReject() reject}
 * — masing-masing bernilai {@code 1} (diberikan) atau {@code 0}/{@code null} (tidak diberikan).
 *
 * <p>Kelas ini adalah <b>jantung sistem hak akses AIS</b>. Nyaris seluruh pemeriksaan "boleh
 * tidak pengguna ini melakukan X di layar Y" berakhir sebagai pembacaan satu baris tabel ini.
 * Karena itu perlakukan setiap perubahan di sini sebagai perubahan keamanan, bukan perubahan
 * model data biasa.</p>
 *
 * <h2>Posisi dalam model keamanan</h2>
 * <p>AIS memakai <b>dua tabel yang saling melengkapi tetapi tidak saling menjamin</b>:</p>
 * <ol>
 *   <li>{@code job_has_menu} (relasi {@code Tbmrole.menus} &harr; {@code Menu}) — menentukan
 *       menu apa yang <b>muncul</b> di pohon navigasi sebuah role. Lihat Javadoc
 *       {@link Menu} untuk struktur pohonnya ({@code root}/{@code child}, tanpa FK formal).</li>
 *   <li><b>Tabel ini</b> ({@code role_privilage}) — menentukan <b>operasi apa</b> yang boleh
 *       dijalankan pada menu tersebut.</li>
 * </ol>
 * <p><b>Keduanya tidak dijaga konsisten oleh basis data maupun oleh kode.</b> Baris
 * {@code RolePrivilage} bisa ada untuk menu yang tidak terdaftar di {@code job_has_menu}
 * (menu "tidak terlihat" tetapi haknya tetap terbaca oleh pemeriksa yang hanya melihat tabel
 * ini), dan sebaliknya menu bisa terdaftar di {@code job_has_menu} tanpa baris privilege
 * pasangannya. Beberapa kode bahkan memanfaatkan ketidaksinkronan itu secara sengaja:
 * {@code NewUiHybridMenuAccessService.loadAssigned} <i>memulihkan</i> menu administrator yang
 * hilang dari {@code job_has_menu} dengan cara membaca baris {@code RolePrivilage}-nya.</p>
 *
 * <h2>Siapa saja yang membaca baris ini (dan hasilnya berbeda-beda)</h2>
 * <p>Ini bagian paling penting untuk dipahami sebelum menyentuh kelas ini: <b>tidak ada satu
 * penegak hak akses tunggal</b>. Minimal lima jalur membaca tabel yang sama dan
 * <b>menafsirkannya secara berbeda</b>, terutama saat data tidak ideal (baris tidak ada, atau
 * baris ganda untuk pasangan role+menu yang sama):</p>
 * <table border="1" summary="Perbandingan penafsiran RolePrivilage antar-pemakai">
 *   <tr><th>Pemakai</th><th>Tidak ada baris</th><th>Baris ganda</th><th>Menghormati
 *       {@code Menu.aktif}?</th></tr>
 *   <tr><td>{@code ais.common.CommonPrivilages.checkPrevilages} (UI ZK lama)</td>
 *       <td><b>DITOLAK</b> ({@code false})</td>
 *       <td><b>Gabungan OR</b> — cukup satu baris memberi hak</td>
 *       <td>Tidak</td></tr>
 *   <tr><td>{@code ais.common.newui.NewUiPermission.from} (New UI)</td>
 *       <td><b>DITOLAK</b> ({@code none()}, fail-closed)</td>
 *       <td>Yang terakhir menang (ditimpa di {@code Map})</td>
 *       <td>Ya, di {@code NewUiHybridMenuAccessService}</td></tr>
 *   <tr><td>{@code GenericCrudRoutePrivilegeResolver.resolve} (scaffold CRUD v2)</td>
 *       <td>{@code null} — pemanggil memakai nilai dasar dari {@code CommonPrivilages}</td>
 *       <td>Yang <b>pertama</b> ditemui menang (urutan baris DB, tidak dijamin)</td>
 *       <td><b>Tidak</b></td></tr>
 *   <tr><td>{@code ais.action.servlet.api.HakAksesApi.privilegeJson} (REST/mobile)</td>
 *       <td><b>DIIZINKAN BACA</b> — mengembalikan {@code r=1} sebagai default</td>
 *       <td>Yang terakhir menang</td>
 *       <td>Ya</td></tr>
 *   <tr><td>{@code ais.action.servlet.api.GrupPenggunaAksesApi} (pengurus grup pengguna)</td>
 *       <td>Membuat baris baru</td>
 *       <td>Baris kedua dst. <b>dihapus</b> saat menyimpan</td>
 *       <td>Ya (hanya menu aktif yang bisa dipilih)</td></tr>
 * </table>
 * <p>Konsekuensi praktis: <b>jawaban "boleh atau tidak" bergantung pada pintu masuk yang
 * dipakai</b>, bukan semata pada isi tabel. Jangan menambah pemakai keenam dengan aturan
 * default sendiri; pakai {@code CommonPrivilages} (ZK) atau {@code NewUiPermission} (New UI).</p>
 *
 * <h2>Catatan keamanan yang wajib diketahui</h2>
 * <ul>
 *   <li><b>Pencocokan SUBSTRING pada {@code GenericCrudRoutePrivilegeResolver}.</b> Bila
 *       request scaffold tidak menyertakan {@code menuId} eksplisit, resolver mengambil
 *       <b>seluruh</b> baris {@code RolePrivilage} milik role, menormalkan {@code Menu.getUrl()}
 *       (buang semua karakter non-alfanumerik, huruf kecilkan), lalu memilih baris pertama yang
 *       URL-nya <b>mengandung</b> token halaman ({@code url.indexOf(pageToken) >= 0}). Token
 *       halaman hanya wajib &ge;4 karakter. Akibatnya menu yang sama sekali tak berkaitan bisa
 *       terpilih hanya karena namanya bersinggungan (mis. token {@code "user"} cocok dengan
 *       {@code /pages/master/log_user_actifity.zul}), dan flag baris tersebut dipakai
 *       apa adanya. Di {@code GenericCrudRequestContext.from} hasil resolver <b>menimpa</b>
 *       (bukan mengiris/meng-AND) hasil {@code CommonPrivilages}, sehingga pencocokan yang
 *       keliru dapat <b>menaikkan</b> hak, bukan sekadar menurunkannya.</li>
 *   <li><b>{@code Menu.aktif=false} tidak mencabut hak di resolver itu.</b> Menonaktifkan menu
 *       menyembunyikannya dari pohon New UI ({@code NewUiHybridMenuAccessService}) dan dari
 *       {@code HakAksesApi}, tetapi {@code GenericCrudRoutePrivilegeResolver} dan
 *       {@code CommonPrivilages.checkPrevilages} <b>tidak memeriksa flag itu sama sekali</b>.
 *       Baris {@code RolePrivilage} yang tertinggal tetap memberi hak CRUD pada rute scaffold.
 *       Lihat juga catatan "kontrol keamanan semu" pada Javadoc {@link Menu}.</li>
 *   <li><b>Tidak ada kunci unik (role, menu).</b> Baris ganda mungkin ada — dibuktikan oleh
 *       {@code GrupPenggunaAksesApi} yang secara eksplisit mengumpulkan dan menghapus
 *       {@code duplikat}. Pada baris ganda, UI ZK memberi hak paling permisif (OR), sedangkan
 *       resolver scaffold memakai baris pertama yang kebetulan dikembalikan basis data.</li>
 *   <li><b>Cache privilege UI lama tidak thread-safe.</b> {@code CommonPrivilages.rolePrivilagesUtama}
 *       adalah {@code public static HashMap} biasa yang dibaca dan ditulis langsung dari thread
 *       request tanpa sinkronisasi; hanya sisi pembersihnya
 *       ({@code NewUiCacheInvalidator.invalidateRole}) yang memakai {@code synchronized (map)},
 *       sehingga penguncian itu tidak memberi eksklusi apa pun.</li>
 *   <li><b>Pemberian hak penuh otomatis saat startup.</b> {@code MenuHelper.ensurePrivilege}
 *       dan variannya ({@code ensureSosialFullPrivilege}, {@code ensureReimbursementPrivilege})
 *       serta {@code UIUtil.ensurePrivilege} membuat baris dengan CRUD penuh bagi role
 *       tertentu (umumnya {@code Tbmrole.ADMINISTRATOR}) tanpa interaksi operator. Menambahkan
 *       menu baru lewat helper itu berarti menambah hak, bukan sekadar menambah navigasi.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)} — PK {@code IDENTITY}.</li>
 *   <li><b>Relasi penaut:</b> {@link #getRole()}/{@link #setRole(Tbmrole)} dan
 *       {@link #getMenu()}/{@link #setMenu(Menu)} — dua sisi tautan, keduanya
 *       {@code nullable = false}.</li>
 *   <li><b>Flag hak akses:</b> {@code read}, {@code create}, {@code update}, {@code delete}
 *       (wajib, {@code nullable = false}) serta {@code approve}, {@code reject} (opsional,
 *       kolom boleh {@code null} pada instalasi lama).</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()} beserta setter-nya, ditambah callback
 *       {@link #onUpdate()}.</li>
 *   <li><b>Invalidasi cache:</b> {@link #invalidateNewUiPrivilegeCache()} — callback JPA yang
 *       menyambungkan perubahan baris ini ke cache hak akses di memori.</li>
 *   <li><b>Diagnostik:</b> {@link #toString()} (perhatian: <b>memutasi</b> objek, lihat di sana).</li>
 * </ul>
 *
 * <h2>Catatan pemetaan</h2>
 * <ul>
 *   <li>Nama kolom flag diberi awalan garis bawah ({@code _read}, {@code _update}, {@code _create},
 *       {@code _delete}, {@code _approve}, {@code _reject}) karena {@code read}/{@code create}/
 *       {@code update}/{@code delete} bertabrakan dengan kata kunci SQL.</li>
 *   <li>{@code dynamicInsert}/{@code dynamicUpdate} aktif: hanya kolom yang benar-benar berubah
 *       yang ikut dalam {@code INSERT}/{@code UPDATE}.</li>
 *   <li>{@link Audited} (Envers) aktif: setiap perubahan baris menghasilkan revisi riwayat.
 *       Karena itu mutasi tak sengaja (lihat {@link #getApprove()}) berbiaya nyata, bukan
 *       sekadar kotor di memori.</li>
 *   <li>Relasi {@code role} dan {@code menu} memakai {@code cascade = {PERSIST, MERGE}}.
 *       Menyimpan sebuah {@code RolePrivilage} ikut mendorong operasi ke {@link Menu} dan
 *       {@link Tbmrole} yang ditautkan — berbahaya bila menu yang dipasang adalah instance
 *       transient hasil {@code new Menu(id)} (pola yang dipakai
 *       {@code UIUtil.ensurePrivilege}), karena {@code PERSIST} akan mencoba menulis baris
 *       menu berisi kolom kosong.</li>
 * </ul>
 *
 * <h2>Verifikasi pola berulang (diverifikasi dari kode kelas ini sendiri)</h2>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field: ADA — 4 buah.</b> {@link #getRole()} dan
 *       {@link #getMenu()} menugaskan ulang field dari hasil
 *       {@link GeneralValueObject#check(Object)}; {@link #getApprove()} dan
 *       {@link #getReject()} mengganti {@code null} menjadi {@code 0} pada field.</li>
 *   <li><b>Getter yang menutup sesi Hibernate: TIDAK ADA</b> di kelas ini. Namun
 *       {@link GeneralValueObject#check(Object)} yang dipanggil dua getter relasi <i>dapat</i>
 *       membuka dan menutup sesi sendiri untuk memuat ulang proxy yang sudah detached.</li>
 *   <li><b>Getter destruktif (membuang data yang sudah terisi): TIDAK ADA.</b>
 *       {@link #getApprove()}/{@link #getReject()} hanya mengisi nilai default saat kosong,
 *       tidak pernah menimpa nilai yang sudah ada — berbeda dari pola destruktif pada
 *       {@code CicilanPembayaranGagal.getTanggal()} atau {@code JamPerkuliahan.getFakultas()}.</li>
 *   <li><b>Method yang memutasi objek di luar setter: ADA — {@link #toString()}</b>, yang
 *       menugaskan ulang field {@code menu} dan {@code role}.</li>
 * </ul>
 *
 * @see Menu
 * @see Tbmrole
 * @see ais.common.CommonPrivilages
 * @see ais.common.newui.NewUiPermission
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "role_privilage")
public class RolePrivilage extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Baris {@code RolePrivilage} ikut tersimpan di
	 * {@code CommonPrivilages.rolePrivilagesUtama} dan dapat ikut terserialisasi bersama
	 * session ZK, jadi nilai ini sebaiknya tidak diubah tanpa alasan.
	 */
	private static final long serialVersionUID = -2114693380000282945L;
	/**
	 * Primary key baris hak akses, dibangkitkan basis data ({@code IDENTITY}).
	 * Dideklarasikan ulang di kelas ini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}, sehingga Hibernate tidak memetakan field induknya.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}. Dideklarasikan ulang di
	 * sini karena alasan teknis yang sama dengan {@link #id}.
	 */
	private String oleh;
	/**
	 * Identitas (username/NIP) pengguna terakhir yang mengubah baris ini. Pendamping
	 * {@link #oleh}; lihat catatan deklarasi ulang di sana.
	 */
	private String olehId;

	/**
	 * @return identitas pengguna terakhir yang mengubah baris hak akses ini, atau {@code null}
	 *         bila baris belum pernah diubah lewat jalur yang beraudit.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah, dengan <b>penjagaan anti-penghapusan</b>:
	 * nilai {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b> sehingga jejak
	 * audit lama tidak terhapus oleh pemanggil yang lalai.
	 *
	 * <p>Konsekuensinya identitas pengubah <b>tidak dapat dikosongkan lagi</b> lewat setter
	 * ini; satu-satunya cara adalah UPDATE langsung ke basis data.</p>
	 *
	 * @param olehId identitas pengguna; {@code null}/kosong tidak berefek apa pun
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan diam-diam agar jejak audit tidak hilang.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong tidak berefek apa pun
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna terakhir yang mengubah baris hak akses ini, atau {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi metadata audit ({@link #oleh},
	 * {@link #olehId}, {@link #tanggal_dirubah}) dari pengguna yang sedang aktif tepat
	 * sebelum Hibernate mengeksekusi {@code UPDATE}.
	 *
	 * <p>Dipanggil oleh penyedia persistence, bukan oleh kode aplikasi.</p>
	 *
	 * <p><b>Perhatian:</b> tidak ada pasangan {@code @PrePersist}, jadi baris yang
	 * <b>baru dibuat</b> (mis. oleh {@code MenuHelper.ensurePrivilege} saat startup)
	 * tidak mendapat atribusi {@code oleh}/{@code olehId} sama sekali. Sebaliknya, mutasi
	 * tak sengaja pada baris lama — lihat {@link #getApprove()} — akan menimpa atribusi
	 * lama dengan identitas pengguna yang kebetulan sekadar <i>membaca</i> hak akses.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Callback JPA yang membuang cache hak akses di memori setiap kali baris ini
	 * disimpan, diubah, atau dihapus, supaya perubahan hak langsung terasa tanpa
	 * menunggu restart JVM.
	 *
	 * <p>Alurnya: mengambil {@code roleId} dari field {@link #role} lalu memanggil
	 * {@code NewUiCacheInvalidator.invalidateRole(roleId)}, yang (a) menghapus seluruh entri
	 * berawalan {@code roleId + "_"} dari {@code CommonPrivilages.rolePrivilagesUtama}
	 * (cache UI ZK lama, kunci {@code roleId + "_" + menuId}) dan (b) menaikkan versi global
	 * pohon menu New UI sehingga setiap session memuat ulang pohonnya pada request berikutnya.</p>
	 *
	 * <p><b>Non-obvious 1:</b> method ini membaca <b>field</b> {@code role} secara langsung,
	 * bukan {@link #getRole()}, jadi tidak ada resolusi proxy lewat
	 * {@link GeneralValueObject#check(Object)}. Bila field {@code role} kebetulan
	 * {@code null} (mis. baris yang dibangun sebagian sebelum dihapus), {@code roleId}
	 * menjadi {@code null} dan pembersihan cache per-role <b>dilewati</b> — hanya versi
	 * global yang naik.</p>
	 *
	 * <p><b>Non-obvious 2:</b> seluruh badan dibungkus {@code try/catch} yang hanya mencatat
	 * ke {@code ErrorAuditUtil}. Kegagalan invalidasi <b>tidak</b> membatalkan transaksi
	 * penyimpanan hak akses — perubahan hak tetap tersimpan meski cache lama masih dipakai.</p>
	 *
	 * <p>Jalur invalidasi kedua yang independen ada di
	 * {@code ais.database.hibernate.AuditListener}, yang membangun ulang
	 * {@code rolePrivilagesUtama} tanpa entri milik role terkait.</p>
	 */
	@javax.persistence.PostPersist
	@javax.persistence.PostUpdate
	@javax.persistence.PostRemove
	protected void invalidateNewUiPrivilegeCache() {
		try {
			String roleId = role == null ? null : role.getRoleId();
			ais.common.newui.NewUiCacheInvalidator.invalidateRole(roleId);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "RolePrivilage.invalidateNewUiPrivilegeCache");
		}
	}

	/**
	 * Waktu perubahan terakhir baris hak akses ini. <b>Diinisialisasi saat objek Java
	 * dibuat</b> (bukan saat ditulis ke basis data) memakai {@code WaktuUtil.getDate()};
	 * untuk baris yang dimuat dari basis data nilainya ditimpa Hibernate dari kolom.
	 * Diperbarui otomatis oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Berbeda dari {@link #setOleh(String)}, setter ini
	 * <b>tidak</b> menyaring {@code null}.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris hak akses ini (presisi {@code TIMESTAMP}).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Ringkasan baris untuk keperluan log/diagnostik: {@code id-menu - read R - update U -
	 * create C - delete D - approve A - REJECT}.
	 *
	 * <p><b>PERINGATAN — method ini MEMUTASI objek dan dapat menyentuh basis data.</b>
	 * Dua baris pertama menugaskan ulang field {@code menu} dan {@code role} dengan hasil
	 * {@link #getMenu()}/{@link #getRole()}, yang di dalamnya memanggil
	 * {@link GeneralValueObject#check(Object)}. Bila kedua relasi masih berupa proxy lazy
	 * yang sudah detached, {@code check()} dapat membuka session Hibernate baru untuk memuat
	 * ulang entity. Jadi memanggil {@code toString()} pada baris hak akses — termasuk secara
	 * tak sengaja lewat perangkaian string di logging atau lewat
	 * {@code CommonPrivilages.buildKeterangan} — bukan operasi bebas efek samping.</p>
	 *
	 * <p><b>Kuirk penulisan:</b> label {@code " - "} terakhir tidak diberi teks
	 * {@code "reject"} seperti lima flag sebelumnya, sehingga nilai reject muncul tanpa nama
	 * kolom. Juga hanya {@code menu} yang dicetak; {@code role} dimuat tetapi tidak pernah
	 * ditampilkan.</p>
	 *
	 * @return representasi teks baris hak akses ini
	 */
	public String toString() {
		menu = getMenu();
		role = getRole();
		return id + "-" + menu + " - read " + read + " - update " + update + " - create " + create + " - delete "
				+ delete + " - approve " + approve + " - " + reject;
	}

	/**
	 * Role (grup pengguna) pemilik hak akses ini. Sisi "siapa" dari tautan.
	 * Kolom {@code role}, {@code nullable = false}.
	 */
	private Tbmrole role;
	/**
	 * Menu yang haknya diatur baris ini. Sisi "di layar mana" dari tautan.
	 * Kolom {@code menu}, {@code nullable = false}.
	 */
	private Menu menu;
	/**
	 * Flag hak <b>membaca</b>/membuka layar. {@code 1} = boleh. Kolom {@code _read},
	 * {@code nullable = false}. Kode konstanta pasangannya adalah
	 * {@code CommonPrivilages.READ} ({@code 0}).
	 */
	private Integer read;
	/**
	 * Flag hak <b>mengubah</b> data. {@code 1} = boleh. Kolom {@code _update},
	 * {@code nullable = false}. Konstanta pasangannya {@code CommonPrivilages.UPDATE}
	 * ({@code 2}).
	 */
	private Integer update;
	/**
	 * Flag hak <b>menambah</b> data. {@code 1} = boleh. Kolom {@code _create},
	 * {@code nullable = false}. Konstanta pasangannya {@code CommonPrivilages.CREATE}
	 * ({@code 1}).
	 */
	private Integer create;
	/**
	 * Flag hak <b>menghapus</b> data. {@code 1} = boleh. Kolom {@code _delete},
	 * {@code nullable = false}. Konstanta pasangannya {@code CommonPrivilages.DELETE}
	 * ({@code 3}).
	 */
	private Integer delete;
	/**
	 * Flag hak <b>menyetujui</b> (approve) pada layar berjenjang. {@code 1} = boleh.
	 * Kolom {@code _approve}, <b>boleh {@code null}</b> — ditambahkan setelah empat flag
	 * dasar, sehingga baris instalasi lama umumnya berisi {@code null}. Konstanta
	 * pasangannya {@code CommonPrivilages.APPROVE} ({@code 4}).
	 */
	private Integer approve;
	/**
	 * Flag hak <b>menolak</b> (reject) pada layar berjenjang. {@code 1} = boleh.
	 * Kolom {@code _reject}, <b>boleh {@code null}</b> dengan alasan historis yang sama
	 * dengan {@link #approve}. Konstanta pasangannya {@code CommonPrivilages.REJECT}
	 * ({@code 5}).
	 */
	private Integer reject;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Objek hasil konstruktor ini belum
	 * punya role, menu, maupun flag apa pun; keenam flag masih {@code null} sampai diisi
	 * setter, dan {@code null} <b>tidak</b> berarti "boleh" di semua pemakai — lihat tabel
	 * penafsiran pada Javadoc kelas.
	 */
	public RolePrivilage() {
	}

	/**
	 * @return primary key baris hak akses, atau {@code null} bila baris belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Karena kolom {@code id} dipetakan {@code insertable = false} dan
	 * dibangkitkan basis data, setter ini praktis hanya dipakai Hibernate saat memuat baris.
	 *
	 * @param id primary key baris hak akses
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Role pemilik hak akses ini — sisi "siapa" dari tautan Menu&harr;Role.
	 *
	 * <p><b>Getter yang menulis balik.</b> Nilai dilewatkan
	 * {@link GeneralValueObject#check(Object)} lalu <b>ditugaskan kembali</b> ke field
	 * {@code role}. Tujuannya menghindari {@code LazyInitializationException}: bila relasi
	 * masih proxy lazy yang sudah detached, {@code check()} akan mencoba cache in-memory,
	 * session yang tersedia, lalu (bila perlu) <b>membuka session baru</b> untuk memuat ulang
	 * entity dan menutupnya kembali. Jadi getter ini bisa memicu query basis data.</p>
	 *
	 * <p>Dipanggil antara lain oleh {@code AuditListener} saat membersihkan cache privilege
	 * dan oleh {@link #toString()}. Perhatikan bahwa
	 * {@link #invalidateNewUiPrivilegeCache()} sengaja <b>tidak</b> memakai getter ini.</p>
	 *
	 * @return role pemilik hak akses; secara skema tidak boleh {@code null}, tetapi bisa
	 *         {@code null} pada objek yang belum lengkap diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "role", nullable = false)
	public Tbmrole getRole() {
		role = check(role);
		return this.role;
	}

	/**
	 * Menyetel role pemilik hak akses.
	 *
	 * <p>Karena relasi memakai {@code cascade = {PERSIST, MERGE}}, role yang dipasang di sini
	 * ikut terbawa saat baris ini disimpan.</p>
	 *
	 * @param role role (grup pengguna) pemilik hak; wajib terisi sebelum disimpan
	 */
	public void setRole(Tbmrole role) {
		this.role = role;
	}

	/**
	 * Menu yang haknya diatur baris ini — sisi "di layar mana" dari tautan Menu&harr;Role.
	 *
	 * <p><b>Getter yang menulis balik</b>, dengan mekanisme dan konsekuensi yang sama persis
	 * dengan {@link #getRole()}: hasil {@link GeneralValueObject#check(Object)} ditugaskan
	 * kembali ke field, dan resolusi proxy dapat membuka session Hibernate sendiri.</p>
	 *
	 * <p>Inilah getter yang dipakai hampir semua pemeriksa hak akses untuk memetakan baris ke
	 * menu: {@code HakAksesApi.privilegePerMenu}, {@code GrupPenggunaAksesApi.idMenu},
	 * {@code NewUiHybridMenuAccessService.loadAssigned}, dan — yang paling perlu diwaspadai —
	 * {@code GenericCrudRoutePrivilegeResolver}, yang membaca {@code getMenu().getUrl()} lalu
	 * mencocokkannya sebagai <b>substring</b> (lihat catatan keamanan pada Javadoc kelas).</p>
	 *
	 * @return menu yang haknya diatur; secara skema tidak boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "menu", nullable = false)
	public Menu getMenu() {
		menu = check(menu);
		return this.menu;
	}

	/**
	 * Menyetel menu yang haknya diatur baris ini.
	 *
	 * <p><b>Hati-hati dengan cascade.</b> Relasi ini {@code cascade = {PERSIST, MERGE}} dan
	 * {@link Menu} memakai primary key yang <b>diisi manual</b>. Memasang instance transient
	 * hasil {@code new Menu(idMenu)} (pola yang dipakai {@code UIUtil.ensurePrivilege})
	 * membuat Hibernate mencoba menulis baris {@code menu} berisi kolom kosong saat
	 * {@code RolePrivilage} ini disimpan. Ambil {@link Menu} dari session
	 * ({@code session.get(Menu.class, id)}) alih-alih membangunnya sendiri.</p>
	 *
	 * @param menu menu tujuan hak akses; wajib terisi sebelum disimpan
	 */
	public void setMenu(Menu menu) {
		this.menu = menu;
	}

	/**
	 * Flag hak <b>membaca</b>/membuka layar menu terkait.
	 *
	 * <p>Tidak ada penjagaan {@code null} di sini (berbeda dari {@link #getApprove()}),
	 * karena kolom {@code _read} dipetakan {@code nullable = false}. Namun beberapa pemakai
	 * memanggil {@code getRead().equals(1)} secara langsung — di antaranya
	 * {@code CommonPrivilages.checkPrevilages} — sehingga baris warisan yang berisi
	 * {@code NULL} di basis data (mis. hasil {@code INSERT} native yang melewati Hibernate)
	 * akan memicu {@code NullPointerException} alih-alih penolakan yang rapi.</p>
	 *
	 * @return {@code 1} bila hak baca diberikan, {@code 0} bila tidak
	 */
	@Column(name = "_read", nullable = false)
	public Integer getRead() {
		return this.read;
	}

	/**
	 * Menyetel flag hak baca.
	 *
	 * @param read {@code 1} untuk memberikan hak baca, {@code 0} untuk mencabutnya
	 */
	public void setRead(Integer read) {
		this.read = read;
	}

	/**
	 * Flag hak <b>mengubah</b> data pada menu terkait. Sama seperti {@link #getRead()},
	 * tanpa penjagaan {@code null}.
	 *
	 * @return {@code 1} bila hak ubah diberikan, {@code 0} bila tidak
	 */
	@Column(name = "_update", nullable = false)
	public Integer getUpdate() {
		return this.update;
	}

	/**
	 * Menyetel flag hak ubah.
	 *
	 * @param update {@code 1} untuk memberikan hak ubah, {@code 0} untuk mencabutnya
	 */
	public void setUpdate(Integer update) {
		this.update = update;
	}

	/**
	 * Flag hak <b>menambah</b> data pada menu terkait. Sama seperti {@link #getRead()},
	 * tanpa penjagaan {@code null}.
	 *
	 * @return {@code 1} bila hak tambah diberikan, {@code 0} bila tidak
	 */
	@Column(name = "_create", nullable = false)
	public Integer getCreate() {
		return this.create;
	}

	/**
	 * Menyetel flag hak tambah.
	 *
	 * @param create {@code 1} untuk memberikan hak tambah, {@code 0} untuk mencabutnya
	 */
	public void setCreate(Integer create) {
		this.create = create;
	}

	/**
	 * Flag hak <b>menghapus</b> data pada menu terkait. Sama seperti {@link #getRead()},
	 * tanpa penjagaan {@code null}.
	 *
	 * @return {@code 1} bila hak hapus diberikan, {@code 0} bila tidak
	 */
	@Column(name = "_delete", nullable = false)
	public Integer getDelete() {
		return this.delete;
	}

	/**
	 * Menyetel flag hak hapus.
	 *
	 * @param delete {@code 1} untuk memberikan hak hapus, {@code 0} untuk mencabutnya
	 */
	public void setDelete(Integer delete) {
		this.delete = delete;
	}

	/**
	 * Flag hak <b>menyetujui</b> (approve) pada layar berjenjang.
	 *
	 * <p><b>Getter yang menulis balik ke field.</b> Kolom {@code _approve} boleh {@code null}
	 * (ditambahkan setelah empat flag dasar), jadi getter ini menormalkan {@code null}
	 * menjadi {@code 0} <b>dan menyimpan hasilnya kembali ke field</b> sebelum
	 * mengembalikannya. Secara semantik ini aman — {@code null} dan {@code 0} sama-sama
	 * berarti "tidak diberikan" bagi seluruh pemakai — tetapi efek sampingnya nyata:</p>
	 * <ul>
	 *   <li>Pada baris yang sedang <b>managed</b> di session Hibernate terbuka (kasus normal
	 *       di {@code CommonPrivilages.checkPrevilages} dan
	 *       {@code GenericCrudRoutePrivilegeResolver}), perubahan field membuat baris menjadi
	 *       <i>dirty</i> sehingga flush berikutnya menerbitkan {@code UPDATE}.</li>
	 *   <li>{@code UPDATE} itu memicu {@link #onUpdate()}, yang <b>menimpa</b>
	 *       {@code oleh}/{@code olehId}/{@code tanggal_dirubah} dengan identitas pengguna yang
	 *       sekadar <i>membaca</i> hak akses — jejak audit "siapa terakhir mengubah hak ini"
	 *       jadi menyesatkan.</li>
	 *   <li>{@code UPDATE} itu juga menghasilkan revisi {@link Audited} (Envers) dan memicu
	 *       {@link #invalidateNewUiPrivilegeCache()}, yakni pembuangan cache hak akses seluruh
	 *       role hanya karena satu pembacaan.</li>
	 * </ul>
	 * <p>Karena baris hak akses lama umumnya berisi {@code NULL} pada kolom ini, gejalanya
	 * bersifat sekali per baris lalu berhenti.</p>
	 *
	 * @return {@code 1} bila hak setujui diberikan, {@code 0} bila tidak; tidak pernah
	 *         mengembalikan {@code null}
	 */
	@Column(name = "_approve")
	public Integer getApprove() {
		if (approve == null) {
			approve = 0;
		}
		return approve;
	}

	/**
	 * Menyetel flag hak setujui.
	 *
	 * @param approve {@code 1} untuk memberikan hak setujui, {@code 0}/{@code null} untuk
	 *                mencabutnya
	 */
	public void setApprove(Integer approve) {
		this.approve = approve;
	}

	/**
	 * Flag hak <b>menolak</b> (reject) pada layar berjenjang.
	 *
	 * <p><b>Getter yang menulis balik ke field</b>, dengan mekanisme dan seluruh efek samping
	 * yang sama persis dengan {@link #getApprove()} — termasuk kemungkinan menerbitkan
	 * {@code UPDATE}, menimpa jejak audit, membuat revisi Envers, dan membuang cache hak
	 * akses hanya karena baris tersebut dibaca. Lihat penjelasan lengkap di sana.</p>
	 *
	 * @return {@code 1} bila hak tolak diberikan, {@code 0} bila tidak; tidak pernah
	 *         mengembalikan {@code null}
	 */
	@Column(name = "_reject")
	public Integer getReject() {
		if (reject == null) {
			reject = 0;
		}
		return reject;
	}

	/**
	 * Menyetel flag hak tolak.
	 *
	 * @param reject {@code 1} untuk memberikan hak tolak, {@code 0}/{@code null} untuk
	 *               mencabutnya
	 */
	public void setReject(Integer reject) {
		this.reject = reject;
	}

}
