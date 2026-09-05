package ais.database.model.sop;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jabatan;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * Satu <b>langkah/tahap yang benar-benar terjadi</b> pada sebuah pengajuan SOP — entity yang
 * MEREKAM keputusan disposisi (diteruskan / disetujui / dikembalikan) beserta siapa pelakunya dan
 * kapan. Ia adalah pasangan "runtime" dari {@link AlurSop}: kalau {@code AlurSop} adalah
 * <i>definisi</i> jenjang (template alur: aktor siapa, boleh ke mana, apakah di sini letak
 * persetujuan), maka {@code DisposisiAlurSop} adalah <i>instansi</i> jenjang itu untuk satu
 * pengajuan tertentu.
 *
 * <h2>Kedudukan dalam model data SOP</h2>
 * <p>Ada tiga lapis yang mudah tertukar:</p>
 * <ol>
 * <li>{@link Sop} — definisi prosedur (dokumen SOP-nya).</li>
 * <li>{@link AlurSop} — definisi tiap simpul/jenjang di dalam prosedur itu (siapa aktornya, apa
 * rute setelahnya, apakah "persetujuan ada di sini", berapa jangka waktunya).</li>
 * <li>{@link DisposisiSop} (header, satu per pengajuan) dan <b>kelas ini</b>
 * {@code DisposisiAlurSop} (detail, satu baris per jenjang yang dilewati pengajuan tersebut).</li>
 * </ol>
 * <p>Jadi hubungan {@code DisposisiSop} : {@code DisposisiAlurSop} adalah <b>header : detail</b>
 * (satu-ke-banyak lewat kolom {@code disposisi_sop}), sekaligus <b>hubungan pointer balik</b>:
 * header menyimpan tiga penunjuk ke baris detail tertentu —
 * {@link DisposisiSop#getDisposisiStart()} (langkah pertama/pengaju),
 * {@link DisposisiSop#getDisposisiEnd()} (langkah terakhir yang sudah tercatat), dan
 * {@link DisposisiSop#getDisposisiSetuju()} (langkah yang dianggap "titik disetujui").</p>
 *
 * <h2>Rantai jenjang: {@code sebelumnya} / {@code setelahnya}</h2>
 * <p>Urutan tahap dibentuk sebagai <b>linked list dua arah</b> antar baris detail:
 * {@link #getSebelumnya()} menunjuk langkah yang mendisposisikan ke sini, dan
 * {@link #getSetelahnya()} menunjuk langkah lanjutannya. Percabangan (satu langkah mendisposisi ke
 * beberapa aktor sekaligus) diwujudkan sebagai <b>beberapa baris anak</b> yang semuanya memiliki
 * {@code sebelumnya = langkah ini} — karena itu kode pemanggil yang ingin tahu "apakah langkah ini
 * masih ujung" lebih andal menghitung anak lewat query {@code where sebelumnya = id} daripada
 * membaca {@code setelahnya} (lihat {@code ProsesDisposisiSopService}).</p>
 *
 * <h2>Bagaimana "sudah diproses" direpresentasikan</h2>
 * <p>Tidak ada kolom status/enum. Sebuah baris jenjang dianggap <b>masih menunggu</b> selama
 * ketiga kolom pelaku ({@code diajukan_oleh}, {@code mahasiswa}, {@code siswa}) masih kosong, dan
 * dianggap <b>sudah diproses</b> begitu salah satunya terisi. Karena itu tindakan "menyetujui"
 * secara fisik hanyalah: mengisi {@code diajukanOleh} dengan user yang mengklik, mengisi
 * {@code waktu}/{@code keterangan}, lalu membuat baris anak untuk jenjang berikutnya. Status
 * "disetujui" sendiri tidak pernah disimpan sebagai nilai tersendiri melainkan <b>diturunkan</b>
 * oleh {@link #setujui()} dari konfigurasi {@link AlurSop}.</p>
 *
 * <h2>PERINGATAN KEAMANAN — entity ini TIDAK memvalidasi wewenang</h2>
 * <p>Ini penting dicatat karena kelas ini adalah tempat keputusan persetujuan mendarat: kelas ini
 * <b>tidak memiliki satu pun pemeriksaan wewenang</b>. Secara konkret:</p>
 * <ul>
 * <li>Satu-satunya callback JPA di sini adalah {@link #onUpdate()} yang beranotasi
 * {@code @PreUpdate}, dan isinya <b>hanya</b> mencatat stempel waktu/pelaku audit
 * ({@code AuditTimestampInterceptor.ubah}). Tidak ada {@code @PrePersist}, tidak ada
 * {@code @EntityListeners}, tidak ada listener Hibernate khusus untuk tabel ini.</li>
 * <li>{@link #setDiajukanOleh(Tbmuser)} adalah setter polos: siapa pun yang dikirim pemanggil akan
 * tersimpan sebagai "yang memproses" jenjang ini, tanpa dibandingkan dengan aktor/jabatan/username
 * yang didefinisikan pada {@link AlurSop} jenjang tersebut.</li>
 * <li>Tidak ada pemeriksaan <i>self-approval</i>: tidak ada apa pun di entity ini yang melarang
 * {@code diajukanOleh} pada jenjang persetujuan sama dengan pengaju di
 * {@link DisposisiSop#getDiajukanOleh()}. Bahkan sebaliknya — lihat {@link #getDiajukanOleh()},
 * yang untuk jenjang {@code start} dan jenjang {@code kembaliKePengaju} <b>menyalin</b> pengaju
 * header ke slot pelaku jenjang ini.</li>
 * <li>Tidak ada penegakan urutan jenjang: {@link #setSebelumnya(DisposisiAlurSop)} dan
 * {@link #setSetelahnya(DisposisiAlurSop)} menerima baris mana pun, dan {@link #setAlurSop(AlurSop)}
 * menerima definisi jenjang mana pun, tanpa memverifikasi bahwa jenjang itu memang rute sah dari
 * langkah sebelumnya. Urutan hanya "benar" sejauh pemanggil membentuknya dengan benar.</li>
 * <li>Di sisi basis data pun tidak ada penjaga: seluruh indeks yang dibuat {@code InitIndex} untuk
 * {@code disposisi_alur_sop} adalah indeks <b>kinerja</b> (b-tree/GIN untuk pencarian dasbor), tidak
 * ada {@code UNIQUE} maupun {@code CHECK} yang mencegah jenjang ganda, lompat jenjang, atau
 * penyetuju = pengaju.</li>
 * </ul>
 * <p>Konsekuensinya: <b>seluruh beban otorisasi ada di pemanggil</b>. Jalur API
 * ({@code ais.action.servlet.api.SopService#proses}) memang memanggil
 * {@code SopUtil.resolveAktor(...)} dan menolak bila pengguna tidak berhak; sebaliknya jalur
 * {@code ProsesDisposisiSopService.prosesLangkah(...)} — yang dipakai halaman JSP
 * {@code pengajuan_sop_service.jsp} aksi {@code simpanDisposisi} — hanya memvalidasi kelengkapan
 * (catatan wajib, rute wajib) dan <b>tidak</b> memanggil resolusi aktor sama sekali, sementara
 * {@code disposisiAlurSopId} yang diproses diambil apa adanya dari parameter request. Karena entity
 * tidak menjadi lapis pertahanan kedua, gerbang "tombol tidak tampil" pada UI adalah satu-satunya
 * penghalang pada jalur tersebut. Setiap perbaikan hendaknya diletakkan di lapis service/entity,
 * bukan sekadar menyembunyikan tombol.</p>
 *
 * <h2>Getter "destruktif" dan bahayanya pada entity ini</h2>
 * <p>Kelas ini memakai akses properti (anotasi {@code @Id} pada getter), sehingga <b>semua</b>
 * getter tanpa {@code @Transient} adalah properti persisten. Sementara itu banyak getter di sini
 * bukan getter polos melainkan <i>menulis balik ke field</i> saat dipanggil:
 * {@link #getKode()}, {@link #getKeterangan()}, {@link #getWaktu()}, {@link #getDiajukanOleh()},
 * {@link #getMahasiswa()}, {@link #getSiswa()}, {@link #getKeyword()},
 * {@link #getParameterTambahan()}, {@link #getParameterTambahanInds()}, {@link #getSelesai()},
 * {@link #getAktif()}, dan {@link #getUsernamePengguna()}. Karena Hibernate memanggil getter yang
 * sama saat <i>dirty checking</i>, nilai hasil turunan itu ikut <b>tersimpan permanen</b> pada
 * flush berikutnya — termasuk kolom {@code diajukan_oleh} yang merupakan bukti "siapa memproses".
 * Ini juga alasan beberapa query di paket ini sengaja dijalankan dengan
 * {@code FlushMode.MANUAL} (lihat {@link DisposisiSop#ambil(Session, ais.ui.util.FormSop)}).</p>
 *
 * <h2>Riwayat/audit</h2>
 * <p>Kelas beranotasi {@link Audited} (Hibernate Envers), jadi setiap perubahan lewat sesi
 * Hibernate terekam di tabel riwayatnya. Perlu diingat: penghapusan lewat SQL native — mis.
 * {@link DisposisiSop#hapus()} yang menjalankan {@code delete from disposisi_alur_sop ...} —
 * <b>tidak</b> melewati Envers sehingga tidak meninggalkan jejak revisi.</p>
 *
 * <p>Kelas dibangkitkan awal oleh hbm2java ("Bank generated by hbm2java") lalu berkembang manual;
 * bentuk kode (tanpa lambda, tanpa try-with-resources) mengikuti target kompilasi Java 1.7.</p>
 *
 * @see AlurSop
 * @see DisposisiSop
 * @see ais.action.master.sop.helper.ProsesDisposisiSopService
 * @see ais.action.master.sop.DisposisiAlurSopAction
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "disposisi_alur_sop")
public class DisposisiAlurSop extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilainya sengaja sama dengan milik {@link DisposisiSop} karena keduanya
	 * lahir dari template generator yang sama; jangan diubah karena instance entity SOP ikut
	 * diserialkan (cache in-memory/MapDB dan penyimpanan sesi ZK).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code disposisi_alur_sop}, di-generate database (identity/sequence). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi mesin audit, lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi mesin audit, lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini (kolom bayangan audit).
	 *
	 * <p>Field ini bukan bagian dari logika disposisi: ia diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} sebagai jejak "siapa terakhir menyentuh baris".
	 * Jangan menyamakannya dengan {@link #getDiajukanOleh()} — yang terakhir itulah pelaku
	 * disposisi yang bermakna secara proses.</p>
	 *
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Setter ini <b>menolak nilai kosong</b>: bila {@code olehId} {@code null} atau hanya berisi
	 * spasi, nilai lama dipertahankan dan tidak ditimpa. Perilaku ini disengaja agar jejak audit
	 * yang sudah ada tidak terhapus ketika sebuah entity di-<i>rebind</i> dari form UI yang tidak
	 * mengirim field audit.</p>
	 *
	 * @param olehId id pengguna; nilai {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan agar jejak
	 * audit lama tidak tertimpa nilai hampa.</p>
	 *
	 * @param oleh nama pengguna; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini (kolom bayangan audit).
	 *
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA sebelum baris ini di-{@code UPDATE}: mencatat stempel waktu dan pelaku perubahan.
	 *
	 * <p><b>Ini satu-satunya callback siklus hidup pada entity ini</b>, dan isinya murni audit
	 * teknis — mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #getTanggal_dirubah()}, {@link #getOleh()} dan {@link #getOlehId()}. Perlu ditegaskan
	 * karena mudah disalahpahami: <b>tidak ada validasi wewenang di sini</b>. Callback ini tidak
	 * memeriksa apakah pengguna yang menyimpan memang aktor sah jenjang ini, tidak memeriksa
	 * penyetuju bukan pengaju, dan tidak memeriksa urutan jenjang. Juga tidak ada
	 * {@code @PrePersist}, sehingga baris <b>baru</b> (kasus paling umum saat aktor memproses
	 * disposisi) tidak melewati callback apa pun.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini (kolom bayangan audit), diinisialisasi ke waktu
	 * server saat object dibuat lewat {@code WaktuUtil.getDate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil mesin audit, bukan kode modul.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Jangan dipakai sebagai "waktu disposisi": waktu tindakan aktor ada pada
	 * {@link #getWaktu()}. Nilai di sini ikut berubah setiap kali baris tersentuh mesin audit,
	 * termasuk oleh penyimpanan yang tidak berkaitan dengan keputusan disposisi.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berbentuk {@code "<id>-<disposisiSop>"}, dipakai untuk penelusuran
	 * log dan tampilan sederhana.
	 *
	 * <p>Perhatikan bahwa method ini <b>menyentuh relasi lazy</b>: ia memanggil
	 * {@link #getDisposisiSop()} (yang me-resolve proxy lewat {@code check()}) lalu menyimpan
	 * hasilnya ke field. Artinya memanggil {@code toString()} — mis. tanpa sengaja lewat
	 * penggabungan string di logging — dapat memicu pemuatan header pengajuan. Kalau header sudah
	 * tidak dapat dimuat (sesi tertutup), {@code check()} akan mengembalikan nilai yang aman
	 * sehingga hasilnya tetap berupa teks, bukan pengecualian.</p>
	 *
	 * @return teks {@code "<id>-<header pengajuan>"}
	 */
	public String toString() {
		disposisiSop = getDisposisiSop();
		return id + "-" + disposisiSop;
	}

	/** Kode langkah; pada praktiknya diturunkan dari kode header, lihat {@link #getKode()}. */
	private String kode;
	/** Teks gabungan untuk pencarian dasbor; selalu dihitung ulang, lihat {@link #getKeyword()}. */
	private String keyword;
	/** Header pengajuan (satu pengajuan punya banyak baris jenjang seperti ini). */
	private DisposisiSop disposisiSop;
	/** Definisi jenjang yang sedang dijalankan baris ini (template dari {@link AlurSop}). */
	private AlurSop alurSop;
	/** Waktu tindakan aktor pada jenjang ini; kosong berarti jenjang belum diproses. */
	private Date waktu;
	/** Catatan/disposisi yang ditulis aktor pada jenjang ini. */
	private String keterangan;

	/** Batas waktu jenjang ini, dihitung dari waktu langkah sebelumnya + jangka waktu definisi. */
	private Date waktuMaksimal;
	/** Pelaku jenjang ini bila ia seorang siswa (jalur sekolah). */
	private Siswa siswa;
	/** Pelaku jenjang ini bila ia seorang mahasiswa (jalur perguruan tinggi). */
	private Mahasiswa mahasiswa;
	/** Pelaku jenjang ini bila ia pengguna internal (pegawai/staf); inti bukti "siapa memproses". */
	private Tbmuser diajukanOleh;
	/** Isian parameter tambahan versi "terbaca manusia" (berlabel), format baris {@code <=>}. */
	private String parameterTambahan;
	/** Isian parameter tambahan versi "berbasis id" (stabil terhadap perubahan label). */
	private String parameterTambahanInds;

	/** Langkah yang mendisposisikan ke jenjang ini (mata rantai ke belakang). */
	private DisposisiAlurSop sebelumnya;
	/** Langkah lanjutan dari jenjang ini (mata rantai ke depan). */
	private DisposisiAlurSop setelahnya;
	/** Kantong properti bebas berformat JSON untuk kebutuhan modul pemanggil. */
	private String properti;
	/** Daftar username aktor terpilih, disimpan sebagai teks dipisah titik koma. */
	private String usernamePengguna;
	/** Penanda "jenjang ini selesai/disetujui"; sebagian besar diturunkan, lihat {@link #getSelesai()}. */
	private Boolean selesai;
	/** Penanda bahwa jenjang ini merupakan pengembalian (revisi) ke langkah sebelumnya. */
	private Boolean kembali;
	/** Penanda aktif; dihitung ulang tiap kali dibaca, lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk instansiasi entity.
	 */
	public DisposisiAlurSop() {
	}

	/**
	 * Membuat baris jenjang baru sekaligus menetapkan username aktor yang memprosesnya.
	 *
	 * <p>Dipakai jalur pemrosesan native ({@code ProsesDisposisiSopService.prosesLangkah}) saat
	 * membuat langkah baru. Perlu dicatat bahwa parameter ini hanya <b>mencatat</b> username; ia
	 * tidak diverifikasi terhadap daftar aktor yang berhak pada {@link AlurSop} jenjang ini.</p>
	 *
	 * @param usernamePengguna username aktor pemroses; boleh kosong
	 */
	public DisposisiAlurSop(String usernamePengguna) {
		this.usernamePengguna = usernamePengguna;
	}

	/**
	 * Varian {@link #DisposisiAlurSop(String)} yang menerima {@code Object} apa pun.
	 *
	 * <p>Berguna bagi pemanggil yang memegang nilai bertipe longgar (mis. hasil pembacaan atribut
	 * komponen ZK). Nilai {@code null} diterjemahkan menjadi string kosong, bukan {@code null},
	 * supaya {@link #getUsernamePengguna()} tidak perlu menangani kasus null tambahan.</p>
	 *
	 * @param usernamePengguna sumber username; {@code null} diperlakukan sebagai string kosong
	 */
	public DisposisiAlurSop(Object usernamePengguna) {
		this.usernamePengguna = usernamePengguna == null ? "" : usernamePengguna.toString();
	}

	/**
	 * Mengembalikan kunci utama baris jenjang ini.
	 *
	 * <p>Nilai {@code null} berarti baris belum pernah disimpan. Beberapa logika di paket ini
	 * memakai perbandingan id sebagai proksi urutan kronologis (mis. {@link #getAktif()} yang
	 * membandingkan id langkah ini dengan id {@code disposisiEnd} header), karena id di-generate
	 * berurutan oleh database.</p>
	 *
	 * @return id baris jenjang, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Normalnya hanya dipanggil Hibernate setelah {@code INSERT}.
	 *
	 * @param id kunci utama baris jenjang
	 */
	public void setId(Long id) {
		this.id = id;
	}

	public String getKode() {
		alurSop = getAlurSop();
		if (alurSop != null && disposisiSop != null) {
			kode = disposisiSop.getKode();
		}
		return kode == null ? "" : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		alurSop = getAlurSop();
		if (alurSop != null && alurSop.getStart() && disposisiSop != null) {
			keterangan = disposisiSop.getKeterangan();
		}

		return this.keterangan == null ? "" : keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public boolean setujui() {
		try {
			DisposisiAlurSop disposisiAlurSop = this;
			boolean hasil = ((disposisiAlurSop.getAlurSop().getPersetujuanAdaDiSini()
					&& disposisiAlurSop.getSetelahnya() == null)
					|| (disposisiAlurSop.getSetelahnya() != null && disposisiAlurSop.getAlurSop()
							.ambilAlurSetujui(disposisiAlurSop.getSetelahnya().getAlurSop())));

			return hasil;
		} catch (Exception e) {
			return false;
		}
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alur_sop", nullable = false)
	public AlurSop getAlurSop() {
		alurSop = check(alurSop);
		return alurSop;
	}

	public void setAlurSop(AlurSop alurSop) {
		this.alurSop = alurSop;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = false)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {

		alurSop = getAlurSop();
		if (alurSop != null && alurSop.getStart() && disposisiSop != null
				&& Hibernate.isInitialized(disposisiSop)) {
			// disposisiSop diakses langsung dari field (bukan lewat getDisposisiSop()/
			// check()), jadi proxy lazy yang belum termuat & sesi sudah tertutup (mis.
			// render dashboard setelah OpenSessionInViewListener menutup sesi) akan
			// melempar LazyInitializationException jika tetap di-dereference. Kalau
			// belum initialized, lewati saja - fallback di bawah (waktu==null -> null
			// atau new Date()) sudah menangani nilai kosong dengan aman.
			waktu = disposisiSop.getWaktu();
		}

		if (getDiajukanOleh() == null && getMahasiswa() == null && getSiswa() == null) {
			waktu = null;
		} else if (waktu == null) {
			waktu = new Date();
		}

		return waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuMaksimal() {

		try {
			alurSop = getAlurSop();
			// alurSop bisa null di sini (mis. saat flush/dirty-check Hibernate atas
			// entity yang belum lengkap terisi - lihat DefaultFlushEntityEventListener
			// yang memanggil getter ini di luar alur render UI biasa). Tanpa cek ini,
			// alurSop.getJangkaWaktu() melempar NullPointerException langsung, tidak
			// peduli getSebelumnya()/getWaktu() sudah dicek non-null.
			if (getSebelumnya() != null && getSebelumnya().getWaktu() != null && alurSop != null
					&& alurSop.getJangkaWaktu() != null) {
				Calendar calendar = WaktuUtil.getCalendar();
				calendar.setTime(getSebelumnya().getWaktu());
				calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + alurSop.getJangkaWaktu());
				waktuMaksimal = calendar.getTime();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sop/DisposisiAlurSop.java:253");
			// TODO: handle exception
		}

		return waktuMaksimal;
	}

	public void setWaktuMaksimal(Date waktuMaksimal) {
		this.waktuMaksimal = waktuMaksimal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan_oleh", nullable = true)
	public Tbmuser getDiajukanOleh() {
		diajukanOleh = check(diajukanOleh);

		alurSop = getAlurSop();
		disposisiSop = getDisposisiSop();
		if (alurSop != null && alurSop.getKembaliKePengaju() && disposisiSop != null
				&& disposisiSop.getDiajukanOleh() != null) {
			diajukanOleh = disposisiSop.getDiajukanOleh();
		} else if (getMahasiswa() != null || getSiswa() != null) {
			diajukanOleh = null;
		} else if (alurSop != null && disposisiSop != null && alurSop.getStart() && getSebelumnya() == null) {
			diajukanOleh = disposisiSop.getDiajukanOleh();
		}
		return diajukanOleh;
	}

	public void setDiajukanOleh(Tbmuser diajukanOleh) {
		this.diajukanOleh = diajukanOleh;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);

		alurSop = getAlurSop();
		disposisiSop = getDisposisiSop();
		if (alurSop != null && alurSop.getKembaliKePengaju() && disposisiSop != null
				&& disposisiSop.getMahasiswa() != null) {
			mahasiswa = disposisiSop.getMahasiswa();
		} else if (alurSop != null && disposisiSop != null && alurSop.getStart()) {
			mahasiswa = disposisiSop.getMahasiswa();
		}

		return mahasiswa;
	}

	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);

		alurSop = getAlurSop();
		disposisiSop = getDisposisiSop();
		if (alurSop != null && alurSop.getKembaliKePengaju() && disposisiSop != null
				&& disposisiSop.getSiswa() != null) {
			siswa = disposisiSop.getSiswa();
		} else if (alurSop != null && disposisiSop != null && alurSop.getStart()) {
			siswa = disposisiSop.getSiswa();
		}

		return siswa;
	}

	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		try {
			if (parameterTambahanInds == null) {
				parameterTambahanInds = "";
			}

			if (parameterTambahanInds.isEmpty() && getSebelumnya() != null) {
				parameterTambahanInds = getSebelumnya().getParameterTambahanInds();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sop/DisposisiAlurSop.java:338");
			// TODO: handle exception
		}

		return parameterTambahanInds;
	}

	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	public List<CommonVO> ambilDataParameterTambahan() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getParameterTambahan().split("\n");
		for (int j = 0; j < splNama.length; j++) {
			CommonVO commonVO = new CommonVO();
			String namaCol = splNama.length > j ? splNama[j] : "";

			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = 1;
			try {
				nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sop/DisposisiAlurSop.java:363");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sop/DisposisiAlurSop.java:369");

			}

			// System.out.println("namaCol=> " + namaCol + ", lbl=> " + lbl + ", val=> " +
			// val + ", url=>" + url);

			String[] param = lbl.split("->");

			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setName5(param[0]);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	public void populateParameterTambahan(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				KelompokParameterTambahanAlurSop kelompokParameterTambahanAlurSop = (KelompokParameterTambahanAlurSop) row
						.getAttribute("kelompokParameterTambahanAlurSop");
				if (parameterTambahan != null && kelompokParameterTambahanAlurSop != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(DisposisiAlurSop.class, getId(),
							kelompokParameterTambahanAlurSop.getId() + "->" + parameterTambahan.getId());

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);
					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);
					String url = "";
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(getId(), jenis);
						if (lam != null) {
							try {
								url = lam.createLinkUri();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

					String s = kelompokParameterTambahanAlurSop.getNama() + "->" + parameterTambahan.getLabelInputan()
							+ "<=>" + val + "<=>" + url + "<=>" + parameterTambahan.getNomorUrut() + "<=>"
							+ parameterTambahan.getId() + "<=>" + kelompokParameterTambahanAlurSop.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanAlurSop.getId() + "->" + parameterTambahan.getId() + "<=>"
							+ val + "<=>" + url + "<=>" + (keterangan == null ? "" : keterangan.getValue().trim());
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		// System.out.println("parameterTambahanStr => " + parameterTambahanStr);
		// System.out.println("parameterTambahanInds => " + parameterTambahanInds);
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		if (parameterTambahan.isEmpty() && getSebelumnya() != null) {
			parameterTambahan = getSebelumnya().getParameterTambahan();
		}

		return parameterTambahan;
	}

	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sebelumnya", nullable = true)
	public DisposisiAlurSop getSebelumnya() {
		sebelumnya = check(sebelumnya);
		return sebelumnya;
	}

	public void setSebelumnya(DisposisiAlurSop sebelumnya) {
		this.sebelumnya = sebelumnya;
	}

	@Column(name = "keyword", nullable = true, columnDefinition = "text")
	public String getKeyword() {
		alurSop = getAlurSop();
		disposisiSop = getDisposisiSop();
		keyword = "";

		try {
			// FIX LazyInitializationException: alurSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (alurSop != null) {
				keyword += alurSop.getKode() + "_" + alurSop.getNama() + "_" + alurSop.getAktor() + "_"
						+ alurSop.getKhususUsername();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sop/DisposisiAlurSop.java:getKeyword-alurSop-lazy");
		}

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (disposisiSop != null && disposisiSop.getDiajukanOleh() != null) {
				keyword += "_" + disposisiSop.getDiajukanOleh().getUserNama();
			} else if (disposisiSop != null && disposisiSop.getMahasiswa() != null) {
				keyword += "_" + disposisiSop.getMahasiswa().getNim() + "_" + disposisiSop.getMahasiswa().getNama();
			} else if (disposisiSop != null && disposisiSop.getSiswa() != null) {
				keyword += "_" + disposisiSop.getSiswa().getNomorInduk() + "_"
						+ disposisiSop.getSiswa().getNomorIndukNasional() + "_" + disposisiSop.getSiswa().getNama();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sop/DisposisiAlurSop.java:getKeyword-disposisiSop-lazy");
		}

		try {
			// FIX LazyInitializationException: diajukanOleh/mahasiswa/siswa bisa berupa instance
			// canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain
			// yang sudah closed -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai
			// fallback dipertahankan).
			if (getDiajukanOleh() != null) {
				keyword += "_" + getDiajukanOleh().getUserNama();
			} else if (getMahasiswa() != null) {
				keyword += "_" + getMahasiswa().getNim() + "_" + getMahasiswa().getNama();
			} else if (getSiswa() != null) {
				keyword += "_" + getSiswa().getNomorInduk() + "_" + getSiswa().getNomorIndukNasional() + "_"
						+ getSiswa().getNama();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/sop/DisposisiAlurSop.java:getKeyword-diajukanOleh-lazy");
		}

		keyword += "_" + getKeterangan();
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "setelahnya", nullable = true)
	public DisposisiAlurSop getSetelahnya() {
		try {
			setelahnya = check(setelahnya);
			if (setelahnya != null && getId() != null) {
				setelahnya.setSebelumnya(this);
			}
		} catch (Exception e) {
			setelahnya = check(setelahnya);
		}
		return setelahnya;
	}

	public void setSetelahnya(DisposisiAlurSop setelahnya) {
		this.setelahnya = setelahnya;
	}

	public static String JSON = new JSONObject().toString();

	@Column(name = "properti", nullable = true, columnDefinition = "text")
	public String getProperti() {
		return properti == null || properti.isEmpty() ? JSON : properti;
	}

	public void setProperti(String properti) {
		this.properti = properti;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void parameterMap(DisposisiSop disposisiSop, Map parameters) {
		Date sekarang = WaktuUtil.getDate();
		if (disposisiSop != null) {
			Session session = HibernateUtil.currentSession();
			List<DisposisiAlurSop> disposisiAlurSops = session.createCriteria(DisposisiAlurSop.class)
					.add(Restrictions.isNotNull("alurSop")).addOrder(Order.asc("id"))
					.add(Restrictions.eq("disposisiSop", disposisiSop)).list();

			if (disposisiSop.getDiajukanOleh() != null && disposisiSop.getDiajukanOleh().getPegawai() != null) {

				Pegawai pegawai = disposisiSop.getDiajukanOleh().getPegawai();
				List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkatData(sekarang);
				JabatanFungsional jabatanFungsional = pegawai.ambilJabatanFungsional(kenaikanPangkats);
				JabatanStruktural jabatanStruktural = pegawai.ambilJabatanStruktural(kenaikanPangkats);
				Jabatan jabatan = pegawai.ambilJabatan(kenaikanPangkats);

				if (pegawai.getGuru() != null) {
					LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getGuru().getId(), LampiranLain.TTD_GURU);
					if (lampiranLain != null) {
						parameters.put("ttd_pengaju", lampiranLain.ambilFile().getAbsolutePath());
					}
				} else if (pegawai.getDosen() != null) {
					LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getDosen().getId(), LampiranLain.TTD_DOSEN);
					if (lampiranLain != null) {
						parameters.put("ttd_pengaju", lampiranLain.ambilFile().getAbsolutePath());
					}
				} else {
					LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getId(), LampiranLain.TTD_PEGAWAI);
					if (lampiranLain != null) {
						parameters.put("ttd_pengaju", lampiranLain.ambilFile().getAbsolutePath());
					}
				}

				parameters.put("jabatanFungsional_sop_pengaju",
						jabatanFungsional == null ? "" : jabatanFungsional.getNama());
				parameters.put("jabatanStruktural_sop_pengaju",
						jabatanStruktural == null ? "" : jabatanStruktural.getNama());
				parameters.put("jabatan_sop_pengaju", jabatan == null ? "" : jabatan.getNama());

			}

			int ke = 0;
			for (DisposisiAlurSop disposisiAlurSop : disposisiAlurSops) {
				ke++;

				parameters.put("aktor_sop_ke_" + ke, disposisiAlurSop.getAlurSop().getAktor());
				parameters.put("oleh_sop_ke_" + ke, disposisiAlurSop.getDiajukanOleh() == null ? ""
						: disposisiAlurSop.getDiajukanOleh().getUserNama());

				parameters.put("satuan_kerja_sop_ke_" + ke,
						disposisiAlurSop.getDiajukanOleh() == null
								|| disposisiAlurSop.getDiajukanOleh().ambilSatuanKerja() == null ? ""
										: disposisiAlurSop.getDiajukanOleh().ambilSatuanKerja().getNama());

				parameters.put("catatan_sop_ke_" + ke, disposisiAlurSop.getKeterangan());
				parameters.put("tanggal_sop_ke_" + ke, disposisiAlurSop.getWaktu() == null ? ""
						: Common.dateFormat6.get().format(disposisiAlurSop.getWaktu()));

				parameters.put("tgl_sop_ke_" + ke, disposisiAlurSop.getWaktu() == null ? ""
						: Common.dateFormat2.get().format(disposisiAlurSop.getWaktu()));

				parameters.put("waktu_sop_ke_" + ke, disposisiAlurSop.getWaktu() == null ? ""
						: Common.dateFormat51.get().format(disposisiAlurSop.getWaktu()));

				if (disposisiAlurSop.getDiajukanOleh() != null
						&& disposisiAlurSop.getDiajukanOleh().getPegawai() != null) {

					parameters.put("nama_pegawai_ke_" + ke, disposisiAlurSop.getDiajukanOleh().getUserNama());
					parameters.put("code_pegawai_ke_" + ke, disposisiAlurSop.getDiajukanOleh().getPegawai().getCode());
					parameters.put("mycode_pegawai_ke_" + ke,
							disposisiAlurSop.getDiajukanOleh().getPegawai().getMycode());
					parameters.put("aktor_pegawai_ke_" + ke, disposisiAlurSop.getAlurSop().getAktorSop() == null ? ""
							: disposisiAlurSop.getAlurSop().getAktorSop().getNama());

					Pegawai pegawai = disposisiAlurSop.getDiajukanOleh().getPegawai();
					List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkatData(sekarang);
					JabatanFungsional jabatanFungsional = pegawai.ambilJabatanFungsional(kenaikanPangkats);
					JabatanStruktural jabatanStruktural = pegawai.ambilJabatanStruktural(kenaikanPangkats);
					Jabatan jabatan = pegawai.ambilJabatan(kenaikanPangkats);

					if (pegawai.getGuru() != null) {
						LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getGuru().getId(),
								LampiranLain.TTD_GURU);
						if (lampiranLain != null) {
							parameters.put("ttd_ke_" + ke, lampiranLain.ambilFile().getAbsolutePath());

						}
					} else if (pegawai.getDosen() != null) {
						LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getDosen().getId(),
								LampiranLain.TTD_DOSEN);
						if (lampiranLain != null) {
							parameters.put("ttd_ke_" + ke, lampiranLain.ambilFile().getAbsolutePath());

						}
					} else {
						LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getId(), LampiranLain.TTD_PEGAWAI);
						if (lampiranLain != null) {
							parameters.put("ttd_ke_" + ke, lampiranLain.ambilFile().getAbsolutePath());

						}
					}

					parameters.put("jabatanFungsional_sop_ke_" + ke,
							jabatanFungsional == null ? "" : jabatanFungsional.getNama());
					parameters.put("jabatanStruktural_sop_ke_" + ke,
							jabatanStruktural == null ? "" : jabatanStruktural.getNama());
					parameters.put("jabatan_sop_ke_" + ke, jabatan == null ? "" : jabatan.getNama());

					parameters.put("nama_pegawai_" + disposisiAlurSop.getAlurSop().getKode(),
							disposisiAlurSop.getDiajukanOleh().getUserNama());
					parameters.put("code_pegawai_" + disposisiAlurSop.getAlurSop().getKode(),
							disposisiAlurSop.getDiajukanOleh().getPegawai().getCode());
					parameters.put("mycode_pegawai_" + disposisiAlurSop.getAlurSop().getKode(),
							disposisiAlurSop.getDiajukanOleh().getPegawai().getMycode());
					parameters.put("aktor_pegawai_" + disposisiAlurSop.getAlurSop().getKode(),
							disposisiAlurSop.getAlurSop().getAktorSop() == null ? ""
									: disposisiAlurSop.getAlurSop().getAktorSop().getNama());

					if (pegawai.getGuru() != null) {
						LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getGuru().getId(),
								LampiranLain.TTD_GURU);
						if (lampiranLain != null) {
							parameters.put("ttd_" + disposisiAlurSop.getAlurSop().getId(),
									lampiranLain.ambilFile().getAbsolutePath());
							parameters.put("ttd_pegawai_" + pegawai.getId(),
									lampiranLain.ambilFile().getAbsolutePath());

							parameters.put("ttd_pegawai_" + disposisiAlurSop.getAlurSop().getKode(),
									lampiranLain.ambilFile().getAbsolutePath());

						}
					} else if (pegawai.getDosen() != null) {
						LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getDosen().getId(),
								LampiranLain.TTD_DOSEN);
						if (lampiranLain != null) {
							parameters.put("ttd_" + disposisiAlurSop.getAlurSop().getId(),
									lampiranLain.ambilFile().getAbsolutePath());
							parameters.put("ttd_pegawai_" + pegawai.getId(),
									lampiranLain.ambilFile().getAbsolutePath());
							parameters.put("ttd_pegawai_" + disposisiAlurSop.getAlurSop().getKode(),
									lampiranLain.ambilFile().getAbsolutePath());
						}
					} else {
						LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getId(), LampiranLain.TTD_PEGAWAI);
						if (lampiranLain != null) {
							parameters.put("ttd_" + disposisiAlurSop.getAlurSop().getId(),
									lampiranLain.ambilFile().getAbsolutePath());
							parameters.put("ttd_pegawai_" + pegawai.getId(),
									lampiranLain.ambilFile().getAbsolutePath());
							parameters.put("ttd_pegawai_" + disposisiAlurSop.getAlurSop().getKode(),
									lampiranLain.ambilFile().getAbsolutePath());
						}
					}

					parameters.put("jabatanFungsional_sop_" + disposisiAlurSop.getAlurSop().getId(),
							jabatanFungsional == null ? "" : jabatanFungsional.getNama());
					parameters.put("jabatanStruktural_sop_" + disposisiAlurSop.getAlurSop().getId(),
							jabatanStruktural == null ? "" : jabatanStruktural.getNama());
					parameters.put("jabatan_sop_" + disposisiAlurSop.getAlurSop().getId(),
							jabatan == null ? "" : jabatan.getNama());
				}

				parameters.put("aktor_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getAlurSop().getAktor());
				parameters.put("oleh_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getDiajukanOleh() == null ? ""
								: disposisiAlurSop.getDiajukanOleh().getUserNama());

				parameters.put("satuan_kerja_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getDiajukanOleh() == null
								|| disposisiAlurSop.getDiajukanOleh().ambilSatuanKerja() == null ? ""
										: disposisiAlurSop.getDiajukanOleh().ambilSatuanKerja().getNama());

				parameters.put("catatan_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getKeterangan());
				parameters.put("tanggal_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getWaktu() == null ? ""
								: Common.dateFormat6.get().format(disposisiAlurSop.getWaktu()));

				parameters.put("tgl_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getWaktu() == null ? ""
								: Common.dateFormat2.get().format(disposisiAlurSop.getWaktu()));

				parameters.put("waktu_sop_" + disposisiAlurSop.getAlurSop().getId(),
						disposisiAlurSop.getWaktu() == null ? ""
								: Common.dateFormat51.get().format(disposisiAlurSop.getWaktu()));

				parameters.put("aktor_sop_" + disposisiAlurSop.getAlurSop().getKode(),
						disposisiAlurSop.getAlurSop().getAktor());
				parameters.put("oleh_sop_" + disposisiAlurSop.getAlurSop().getKode(),
						disposisiAlurSop.getDiajukanOleh() == null ? ""
								: disposisiAlurSop.getDiajukanOleh().getUserNama());

				parameters.put("satuan_kerja_sop_" + disposisiAlurSop.getAlurSop().getKode(),
						disposisiAlurSop.getDiajukanOleh() == null
								|| disposisiAlurSop.getDiajukanOleh().ambilSatuanKerja() == null ? ""
										: disposisiAlurSop.getDiajukanOleh().ambilSatuanKerja().getNama());

				parameters.put("catatan_sop_" + disposisiAlurSop.getAlurSop().getKode(),
						disposisiAlurSop.getKeterangan());
				parameters.put("tanggal_sop_" + disposisiAlurSop.getAlurSop().getKode(),
						disposisiAlurSop.getWaktu() == null ? ""
								: Common.dateFormat6.get().format(disposisiAlurSop.getWaktu()));

				parameters.put("tgl_sop_" + disposisiAlurSop.getAlurSop().getKode(),
						disposisiAlurSop.getWaktu() == null ? ""
								: Common.dateFormat2.get().format(disposisiAlurSop.getWaktu()));

				parameters.put("waktu_sop_" + disposisiAlurSop.getAlurSop().getKode(),
						disposisiAlurSop.getWaktu() == null ? ""
								: Common.dateFormat51.get().format(disposisiAlurSop.getWaktu()));

			}
		}
	}

	public Boolean getSelesai() {
		if (getAlurSop() != null && setujui()) {
			selesai = getDiajukanOleh() != null;
		} else if (getAlurSop() != null && getAlurSop().getJikaProsesDisetujuiMakaSelesai()
				&& getDiajukanOleh() != null) {
			selesai = getSetelahnya() == null;
		}

		if (getDiajukanOleh() == null) {
			selesai = false;
		}

		return selesai == null ? false : selesai;
	}

	public void setSelesai(Boolean selesai) {
		this.selesai = selesai;
	}

	public Boolean getKembali() {
		return kembali == null ? false : kembali;
	}

	public void setKembali(Boolean kembali) {
		this.kembali = kembali;
	}

	public Boolean getAktif() {

		if (getId() != null && getDisposisiSop() != null && getDisposisiSop().getDisposisiEnd() != null
				&& getAlurSop() != null && getAlurSop().getSebelumnya() != null
				&& getAlurSop().getSebelumnya().getAlurSetelahnyaBerupaPilihan()
				&& getDisposisiSop().getDisposisiEnd().getId() != null
				&& getDisposisiSop().getDisposisiEnd().getId() > getId() && getDiajukanOleh() == null
				&& getMahasiswa() == null && getSiswa() == null) {
			aktif = false;
		} else {
			aktif = true;
		}

		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "username_pengguna", nullable = true, columnDefinition = "text")
	public String getUsernamePengguna() {

		usernamePengguna = (usernamePengguna == null || usernamePengguna.trim().equalsIgnoreCase(";") ? ""
				: ";" + usernamePengguna.trim() + ";").replaceAll(";;", ";").replaceAll(";;", ";")
				.replaceAll(";;", ";");

		if (usernamePengguna.equals(";")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(";;")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(";;;")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(";;;;")) {
			usernamePengguna = "";
		}

		return usernamePengguna == null ? "" : usernamePengguna.trim();
	}

	public void setUsernamePengguna(String usernamePengguna) {
		this.usernamePengguna = usernamePengguna;
	}

}
