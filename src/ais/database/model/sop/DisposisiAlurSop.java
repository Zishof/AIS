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

	/**
	 * Mengembalikan kode langkah — pada praktiknya <b>kode pengajuan induk</b>, bukan kode
	 * tersendiri milik jenjang ini.
	 *
	 * <p>Selama definisi jenjang ({@link #getAlurSop()}) sudah termuat dan header pengajuan sudah
	 * ada di field, nilai {@code kode} baris ini <b>ditimpa</b> dengan kode header
	 * ({@code disposisiSop.getKode()}). Tujuannya agar setiap baris riwayat disposisi membawa nomor
	 * dokumen yang sama sehingga mudah dicari di dasbor dan pada cetakan.</p>
	 *
	 * <p><b>Dua hal yang perlu diwaspadai.</b> Pertama, ini adalah <i>getter destruktif</i> pada
	 * properti yang persisten: karena Hibernate memanggil getter yang sama ketika melakukan
	 * <i>dirty checking</i>, kode hasil salinan tersebut ikut tersimpan ke kolom {@code kode} pada
	 * flush berikutnya. Kedua, kondisi pengecekan membaca <b>field</b> {@code disposisiSop} secara
	 * langsung, bukan lewat {@link #getDisposisiSop()}. Artinya penyalinan hanya terjadi bila header
	 * kebetulan sudah pernah dimuat (mis. oleh getter lain yang dipanggil sebelumnya); bila belum,
	 * method ini diam-diam mengembalikan nilai kode lama. Pilihan ini sekaligus melindungi dari
	 * {@code LazyInitializationException} saat proxy header tidak lagi terhubung ke sesi.</p>
	 *
	 * @return kode dokumen (sudah di-{@code trim}); string kosong bila belum ada kode
	 */
	public String getKode() {
		alurSop = getAlurSop();
		if (alurSop != null && disposisiSop != null) {
			kode = disposisiSop.getKode();
		}
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode langkah secara manual.
	 *
	 * <p>Perlu diingat nilai yang disetel di sini bersifat sementara pada kondisi umum: begitu
	 * {@link #getKode()} dipanggil dengan header pengajuan sudah termuat, nilainya ditimpa kembali
	 * oleh kode header.</p>
	 *
	 * @param kode kode dokumen
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan catatan/disposisi yang ditulis aktor pada jenjang ini.
	 *
	 * <p>Ada satu perlakuan khusus: bila jenjang ini adalah <b>jenjang awal</b>
	 * ({@code alurSop.getStart()}), catatan diambil dari keterangan header pengajuan
	 * ({@link DisposisiSop#getKeterangan()}). Alasannya, pada langkah pertama yang "menulis catatan"
	 * sesungguhnya adalah pengaju lewat form pengajuan, bukan lewat kotak disposisi; dengan
	 * penyalinan ini tampilan riwayat menjadi konsisten — baris pertama memperlihatkan maksud
	 * pengajuan, baris berikutnya memperlihatkan disposisi tiap aktor.</p>
	 *
	 * <p>Seperti {@link #getKode()}, ini getter destruktif atas properti persisten: hasil salinan
	 * dapat ikut tertulis ke kolom {@code keterangan} saat flush. Kondisi juga memakai field
	 * {@code disposisiSop} langsung (bukan getter), sehingga penyalinan hanya berlaku ketika header
	 * kebetulan sudah termuat.</p>
	 *
	 * @return catatan jenjang ini; string kosong bila belum ada catatan (tidak pernah {@code null})
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		alurSop = getAlurSop();
		if (alurSop != null && alurSop.getStart() && disposisiSop != null) {
			keterangan = disposisiSop.getKeterangan();
		}

		return this.keterangan == null ? "" : keterangan;
	}

	/**
	 * Menyetel catatan/disposisi jenjang ini.
	 *
	 * <p>Pada jenjang awal nilai ini akan ditimpa kembali oleh keterangan header ketika
	 * {@link #getKeterangan()} dipanggil; untuk jenjang selain awal, nilai bertahan sebagaimana
	 * disetel. Kewajiban pengisian catatan (bila {@code AlurSop.getCatatanWajibDiisi()} bernilai
	 * benar) <b>tidak</b> ditegakkan di sini melainkan di lapis service/UI.</p>
	 *
	 * @param keterangan catatan disposisi
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menjawab pertanyaan: <b>apakah langkah ini merupakan titik persetujuan yang sudah tercapai?</b>
	 *
	 * <p>Inilah satu-satunya tempat di entity ini yang "menghitung" makna persetujuan, dan
	 * penting untuk dipahami bahwa ia adalah <b>fungsi turunan murni</b> — ia membaca konfigurasi
	 * jenjang pada {@link AlurSop} lalu menyimpulkan; ia tidak memeriksa identitas siapa pun, tidak
	 * menulis apa pun, dan tidak menolak apa pun.</p>
	 *
	 * <p>Aturannya dua cabang:</p>
	 * <ol>
	 * <li><b>Langkah ini ujung rantai.</b> Bila {@code alurSop.getPersetujuanAdaDiSini()} bernilai
	 * benar <i>dan</i> {@link #getSetelahnya()} masih {@code null}, langkah ini dianggap titik
	 * persetujuan. Dengan kata lain: definisi jenjang menyatakan "persetujuan ada di sini" dan
	 * proses memang berhenti di sini.</li>
	 * <li><b>Langkah ini sudah diteruskan.</b> Bila sudah ada {@code setelahnya}, keputusan
	 * diserahkan ke {@code alurSop.ambilAlurSetujui(setelahnya.getAlurSop())}. Method tersebut
	 * mencocokkan jenjang tujuan dengan salah satu slot rute {@code setelahnya1..setelahnya20} pada
	 * definisi, lalu mengembalikan penanda {@code persetujuanAdaDiSini1..20} yang bersesuaian.
	 * Jadi maknanya: "apakah <i>rute yang benar-benar dipilih</i> merupakan rute persetujuan?" —
	 * sebuah jenjang bisa punya satu cabang yang berarti menyetujui dan cabang lain yang berarti
	 * menolak/meneruskan, dan yang menentukan adalah cabang mana yang diambil.</li>
	 * </ol>
	 *
	 * <p><b>Perilaku saat gagal.</b> Seluruh badan method dibungkus {@code try/catch} yang
	 * mengembalikan {@code false} untuk pengecualian apa pun. Ini menutup dua kasus nyata:
	 * {@code getAlurSop()} bisa {@code null} pada baris yang belum lengkap (menimbulkan
	 * {@code NullPointerException}), dan proxy lazy bisa terlepas dari sesinya. Arah kegagalannya
	 * <i>fail-closed</i> untuk pembacaan (kalau ragu, anggap belum disetujui) — tetapi perlu dicatat
	 * bahwa fail-closed di sini hanya menyangkut <b>tampilan/penurunan status</b>, bukan penolakan
	 * penyimpanan; entity tetap menerima data yang dikirim pemanggil.</p>
	 *
	 * <p>Pemakai penting method ini: {@link DisposisiSop#getDisposisiSetuju()} (menentukan baris
	 * mana yang menjadi "titik disetujui" pengajuan) dan {@link #getSelesai()}.</p>
	 *
	 * @return {@code true} bila jenjang ini merupakan titik persetujuan yang tercapai; {@code false}
	 *         bila bukan, atau bila data tidak dapat dievaluasi
	 */
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

	/**
	 * Mengembalikan definisi jenjang ({@link AlurSop}) yang sedang dijalankan baris ini.
	 *
	 * <p>Relasi inilah yang membawa seluruh <b>aturan</b> jenjang: siapa aktor yang berhak
	 * ({@code aktorSop}/{@code khususUsername}), apakah "persetujuan ada di sini", rute lanjutan
	 * apa saja yang tersedia, apakah catatan wajib diisi, berapa jangka waktunya, dan seterusnya.
	 * Baris {@code DisposisiAlurSop} sendiri hanya mencatat apa yang terjadi; yang menyatakan apa
	 * yang <i>seharusnya</i> terjadi adalah object yang dikembalikan method ini.</p>
	 *
	 * <p>Pemuatan dilakukan lewat {@code check()} milik {@code GeneralValueObject} sehingga proxy
	 * lazy yang sudah lepas dari sesinya tetap dapat diselesaikan (atau dikembalikan secara aman)
	 * tanpa melempar {@code LazyInitializationException}. Meski kolom {@code alur_sop} dipetakan
	 * {@code nullable = false}, banyak kode di paket ini tetap memeriksa hasilnya terhadap
	 * {@code null} karena baris yang masih dibentuk di memori atau proxy yang gagal dimuat bisa
	 * memberi {@code null}.</p>
	 *
	 * @return definisi jenjang, atau {@code null} bila belum diisi/tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alur_sop", nullable = false)
	public AlurSop getAlurSop() {
		alurSop = check(alurSop);
		return alurSop;
	}

	/**
	 * Menetapkan definisi jenjang untuk baris ini.
	 *
	 * <p><b>Tanpa validasi rute.</b> Setter ini menerima {@link AlurSop} mana pun: ia tidak
	 * memeriksa bahwa jenjang tersebut memang salah satu rute sah dari
	 * {@link #getSebelumnya()}, tidak memeriksa bahwa jenjang itu milik {@link Sop} yang sama
	 * dengan pengajuan pada header, dan tidak memeriksa bahwa jenjang itu belum pernah dilewati.
	 * Konsistensi rantai sepenuhnya menjadi tanggung jawab kode pemanggil (jalur ZK
	 * {@code DisposisiAlurSopAction}, jalur native {@code ProsesDisposisiSopService}, dan jalur API
	 * {@code SopService}).</p>
	 *
	 * @param alurSop definisi jenjang yang dijalankan baris ini
	 */
	public void setAlurSop(AlurSop alurSop) {
		this.alurSop = alurSop;
	}

	/**
	 * Mengembalikan header pengajuan ({@link DisposisiSop}) yang memiliki baris jenjang ini.
	 *
	 * <p>Header menyimpan identitas pengajuan: nomor/kode, pengaju, waktu pengajuan, serta penunjuk
	 * ke baris awal/akhir/titik-setuju. Semua baris jenjang dari satu pengajuan berbagi header yang
	 * sama, dan dari header itulah beberapa getter di kelas ini menyalin nilai (lihat
	 * {@link #getKode()}, {@link #getKeterangan()}, {@link #getDiajukanOleh()}).</p>
	 *
	 * <p>Sama seperti relasi lain, pemuatan melalui {@code check()} agar aman terhadap proxy lazy
	 * yang sesinya sudah ditutup.</p>
	 *
	 * @return header pengajuan, atau {@code null} bila belum diisi/tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = false)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan header pengajuan pemilik baris jenjang ini, dengan penjaga "jangan turun ke nilai
	 * kosong".
	 *
	 * <p>Baris pertama method melakukan <i>early return</i> bila argumen {@code null} atau belum
	 * punya id — jadi header yang sudah terpasang tidak akan pernah dihapus/ditimpa oleh object
	 * hampa. Ini penting karena banyak jalur menyimpan ulang entity hasil rebinding form ZK yang
	 * kadang membawa header kosong; tanpa penjaga ini kolom {@code disposisi_sop} (yang dipetakan
	 * {@code nullable = false}) bisa gagal saat {@code INSERT}/{@code UPDATE} atau, lebih buruk,
	 * memutus baris jenjang dari pengajuannya sehingga riwayat disposisi tampak hilang.</p>
	 *
	 * <p><b>Catatan kode:</b> ekspresi ternary setelah early return sebenarnya <b>selalu</b>
	 * memilih argumen baru, karena kondisi {@code (disposisiSop == null || disposisiSop.getId() ==
	 * null)} di dalamnya sudah pasti bernilai salah — kasus itu sudah disaring oleh early return di
	 * atas. Jadi efektifnya method ini setara dengan "abaikan bila kosong, selain itu timpa".
	 * Cabang tersebut dibiarkan apa adanya karena tidak berdampak pada perilaku.</p>
	 *
	 * @param disposisiSop header pengajuan; {@code null} atau header tanpa id diabaikan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Mengembalikan waktu tindakan aktor pada jenjang ini — sekaligus <b>penanda de facto</b>
	 * apakah jenjang sudah diproses.
	 *
	 * <p>Logikanya tiga lapis:</p>
	 * <ol>
	 * <li><b>Jenjang awal mewarisi waktu pengajuan.</b> Bila {@code alurSop.getStart()} bernilai
	 * benar dan header sudah termuat, waktu diambil dari {@link DisposisiSop#getWaktu()}. Sekali
	 * lagi karena baris pertama bukan "tindakan disposisi" melainkan momen pengajuan itu sendiri.</li>
	 * <li><b>Belum ada pelaku berarti belum ada waktu.</b> Bila ketiga slot pelaku
	 * ({@link #getDiajukanOleh()}, {@link #getMahasiswa()}, {@link #getSiswa()}) kosong, waktu
	 * dipaksa {@code null}. Inilah cara sistem menandai "jenjang ini masih menunggu": baris
	 * placeholder untuk tahap berikutnya memang dibuat lebih dulu tanpa pelaku, dan tidak boleh
	 * terlihat seolah sudah diproses hanya karena punya timestamp.</li>
	 * <li><b>Sudah ada pelaku tetapi waktu kosong</b> diisi waktu sekarang sebagai jaring pengaman
	 * agar riwayat tidak menampilkan baris tanpa tanggal.</li>
	 * </ol>
	 *
	 * <p><b>Catatan teknis penting</b> (sudah ada komentar di badan method): pemeriksaan header
	 * memakai field {@code disposisiSop} langsung disertai {@code Hibernate.isInitialized(...)},
	 * bukan {@link #getDisposisiSop()}. Ini disengaja agar proxy lazy yang belum termuat dan
	 * sesinya sudah ditutup — tipikal saat render dasbor setelah {@code OpenSessionInViewListener}
	 * menutup sesi — tidak di-dereference dan tidak melempar
	 * {@code LazyInitializationException}; nilai fallback sudah menangani kasus kosong.</p>
	 *
	 * <p>Getter ini juga destruktif atas properti persisten: nilai hasil turunan (termasuk
	 * pengosongan menjadi {@code null}) dapat ikut tersimpan ke kolom {@code waktu} saat flush
	 * berikutnya.</p>
	 *
	 * @return waktu tindakan pada jenjang ini, atau {@code null} bila jenjang belum diproses
	 */
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

	/**
	 * Menyetel waktu tindakan pada jenjang ini.
	 *
	 * <p>Umumnya diisi waktu server oleh lapis service. Pada jenjang yang dikonfigurasi
	 * {@code AlurSop.getTanggalDisposisiBolehDiubah()}, aktor boleh memasukkan tanggal sendiri —
	 * kelonggaran itu diputuskan di lapis pemanggil (lihat {@code SopService.proses}), bukan di
	 * sini; setter ini menerima tanggal apa pun, termasuk tanggal mundur atau maju.</p>
	 *
	 * @param waktu waktu tindakan pada jenjang ini
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Menghitung batas waktu (tenggat) jenjang ini berdasarkan waktu langkah sebelumnya ditambah
	 * jangka waktu yang ditetapkan definisi jenjang.
	 *
	 * <p>Rumusnya sederhana: {@code waktuMaksimal = sebelumnya.getWaktu() + alurSop.getJangkaWaktu()
	 * hari}. Nilai inilah yang dipakai dasbor untuk menyorot disposisi yang terlambat/mendekati
	 * tenggat (ada indeks khusus pada kolom {@code waktumaksimal} untuk kebutuhan tersebut).</p>
	 *
	 * <p>Perhitungan hanya berjalan bila keempat prasyarat terpenuhi: ada langkah sebelumnya,
	 * langkah itu punya waktu, definisi jenjang termuat, dan jangka waktu terisi. Bila salah satu
	 * tidak terpenuhi, nilai lama dipertahankan (dan bisa saja {@code null}, yang berarti "tanpa
	 * tenggat"). Konsekuensinya: jenjang <b>awal</b> tidak pernah punya tenggat karena tidak punya
	 * langkah sebelumnya.</p>
	 *
	 * <p><b>Kenapa ada cek {@code alurSop != null}</b> (lihat komentar di badan method): getter ini
	 * ikut dipanggil Hibernate di luar alur render UI biasa — mis. saat {@code dirty check}/flush
	 * atas entity yang belum lengkap terisi — dan pada saat itu relasi jenjang bisa masih kosong,
	 * sehingga {@code alurSop.getJangkaWaktu()} akan melempar {@code NullPointerException} meskipun
	 * {@code getSebelumnya()} dan {@code getWaktu()} sudah dipastikan tidak null. Blok
	 * {@code try/catch} di sekelilingnya menjadi lapis terakhir: kegagalan dicatat ke audit galat
	 * dan nilai lama dikembalikan, sehingga perhitungan tenggat tidak pernah menggagalkan
	 * penyimpanan atau tampilan.</p>
	 *
	 * @return tenggat jenjang ini, atau {@code null} bila tidak dapat/tidak perlu dihitung
	 */
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

	/**
	 * Menyetel tenggat jenjang ini secara manual.
	 *
	 * <p>Nilai yang disetel bersifat sementara bila prasyarat perhitungan pada
	 * {@link #getWaktuMaksimal()} terpenuhi, karena getter tersebut akan menghitung ulang dan
	 * menimpanya. Setter ini berguna untuk baris yang tenggatnya ditentukan pemanggil (mis. saat
	 * membuat langkah berikutnya dengan tenggat khusus).</p>
	 *
	 * @param waktuMaksimal tenggat jenjang ini
	 */
	public void setWaktuMaksimal(Date waktuMaksimal) {
		this.waktuMaksimal = waktuMaksimal;
	}

	/**
	 * Mengembalikan pengguna internal (pegawai/staf) yang memproses jenjang ini — <b>inilah bukti
	 * "siapa menyetujui/mendisposisi"</b> yang tersimpan di kolom {@code diajukan_oleh}.
	 *
	 * <p>Karena kolom ini yang menjadi jejak pertanggungjawaban persetujuan, perlu dipahami betul
	 * bahwa nilainya <b>tidak selalu berarti "orang ini menekan tombol setujui"</b>. Getter ini
	 * memodifikasi nilainya sendiri lewat tiga cabang berikut, dan karena ia adalah properti
	 * persisten dengan akses properti, hasil modifikasi itu <b>ikut tersimpan ke database</b> pada
	 * flush berikutnya:</p>
	 * <ol>
	 * <li><b>Jenjang "kembali ke pengaju"</b> ({@code alurSop.getKembaliKePengaju()}): pelaku
	 * ditimpa dengan pengaju header ({@link DisposisiSop#getDiajukanOleh()}). Masuk akal secara
	 * proses — langkah revisi memang dikembalikan kepada pengaju — tetapi artinya baris tersebut
	 * akan tercatat atas nama pengaju walaupun bukan pengaju yang menyentuhnya.</li>
	 * <li><b>Pelaku bukan pengguna internal</b>: bila {@link #getMahasiswa()} atau
	 * {@link #getSiswa()} terisi, {@code diajukanOleh} dikosongkan agar satu baris hanya punya satu
	 * jenis pelaku (tiga slot pelaku bersifat saling meniadakan).</li>
	 * <li><b>Jenjang awal</b> ({@code alurSop.getStart()} dan belum punya langkah sebelumnya):
	 * pelaku diisi dengan pengaju header, karena baris pertama memang merepresentasikan pengaju.</li>
	 * </ol>
	 *
	 * <p><b>Implikasi keamanan yang perlu dicatat.</b> Cabang (1) dan (3) berarti entity ini secara
	 * sengaja <i>menyalin pengaju ke slot pelaku</i>. Tidak ada satu pun pemeriksaan di kelas ini
	 * yang membandingkan pelaku jenjang persetujuan dengan pengaju pengajuan — jadi tidak ada
	 * penjaga <i>self-approval</i> di level entity, dan tidak ada pula pemeriksaan bahwa pelaku
	 * memang termasuk aktor yang berhak menurut {@code alurSop.getAktorSop()} /
	 * {@code alurSop.getKhususUsername()}. Verifikasi semacam itu hanya ada di sebagian lapis
	 * pemanggil (mis. {@code SopUtil.resolveAktor} pada jalur API). Bila kelak ditambahkan penjaga,
	 * penjaga tersebut harus membedakan penyalinan sah pada cabang (1)/(3) dari pengisian pelaku
	 * pada jenjang persetujuan biasa.</p>
	 *
	 * @return pengguna internal pemroses jenjang ini, atau {@code null} bila jenjang belum diproses
	 *         atau pelakunya berupa mahasiswa/siswa
	 */
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

	/**
	 * Menetapkan pengguna internal yang memproses jenjang ini.
	 *
	 * <p><b>Setter polos, tanpa pemeriksaan apa pun.</b> Siapa pun yang dikirim pemanggil akan
	 * tercatat sebagai pemroses jenjang: tidak dibandingkan dengan aktor/jabatan/username yang
	 * ditetapkan {@link AlurSop}, tidak dibandingkan dengan pengaju pengajuan, dan tidak diperiksa
	 * apakah jenjang ini memang giliran yang bersangkutan. Otorisasi sepenuhnya berada di lapis
	 * pemanggil; entity ini hanya merekam.</p>
	 *
	 * <p>Ingat pula bahwa nilai yang disetel di sini dapat ditimpa kembali oleh
	 * {@link #getDiajukanOleh()} pada jenjang {@code start} maupun {@code kembaliKePengaju}.</p>
	 *
	 * @param diajukanOleh pengguna internal pemroses jenjang ini
	 */
	public void setDiajukanOleh(Tbmuser diajukanOleh) {
		this.diajukanOleh = diajukanOleh;
	}

	/**
	 * Mengembalikan mahasiswa yang menjadi pelaku jenjang ini (jalur perguruan tinggi).
	 *
	 * <p>Mesin SOP dipakai lintas modul, termasuk pengajuan yang pelakunya bukan pegawai melainkan
	 * mahasiswa (mis. pengajuan mahasiswa, surat keterangan). Slot ini adalah pasangan
	 * {@link #getDiajukanOleh()} untuk kasus tersebut, dan ketiga slot pelaku bersifat saling
	 * meniadakan.</p>
	 *
	 * <p>Sama seperti pelaku pegawai, getter ini destruktif: pada jenjang {@code kembaliKePengaju}
	 * maupun jenjang {@code start}, nilai disalin dari {@link DisposisiSop#getMahasiswa()} sehingga
	 * baris tersebut tercatat atas nama mahasiswa pengaju. Nilai hasil salinan itu dapat ikut
	 * tersimpan ke kolom {@code mahasiswa} saat flush.</p>
	 *
	 * @return mahasiswa pelaku jenjang ini, atau {@code null} bila bukan jalur mahasiswa
	 */
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

	/**
	 * Menetapkan mahasiswa pelaku jenjang ini. Setter polos tanpa validasi wewenang, seperti
	 * {@link #setDiajukanOleh(Tbmuser)}.
	 *
	 * @param mahasiswa mahasiswa pelaku jenjang ini
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan siswa yang menjadi pelaku jenjang ini (jalur sekolah).
	 *
	 * <p>Kembar dari {@link #getMahasiswa()} untuk instalasi sekolah (mis. alur PSB, pengajuan
	 * siswa). Perilakunya identik: pada jenjang {@code kembaliKePengaju} dan jenjang {@code start},
	 * nilai disalin dari {@link DisposisiSop#getSiswa()}, dan hasil salinan itu dapat ikut
	 * tersimpan ke kolom {@code siswa} saat flush.</p>
	 *
	 * @return siswa pelaku jenjang ini, atau {@code null} bila bukan jalur siswa
	 */
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

	/**
	 * Menetapkan siswa pelaku jenjang ini. Setter polos tanpa validasi wewenang, seperti
	 * {@link #setDiajukanOleh(Tbmuser)}.
	 *
	 * @param siswa siswa pelaku jenjang ini
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan isian parameter tambahan dalam bentuk <b>berbasis id</b>, dengan pewarisan dari
	 * langkah sebelumnya bila baris ini belum punya isian sendiri.
	 *
	 * <p>Format tiap barisnya (dipisah baris baru) adalah:
	 * {@code <idKelompok>-><idParameter><=><nilai><=><url lampiran><=><catatan>}. Berbeda dari
	 * {@link #getParameterTambahan()} yang menyimpan label agar mudah dibaca manusia, bentuk ini
	 * memakai id sehingga tetap sahih walau label/urutan parameter kelak diubah admin — inilah
	 * bentuk yang dipakai untuk mengisi ulang form saat jenjang berikutnya dibuka.</p>
	 *
	 * <p><b>Pewarisan.</b> Bila isian baris ini kosong, nilainya diambil dari
	 * {@link #getSebelumnya()} — dan karena getter pada langkah sebelumnya melakukan hal yang sama,
	 * pewarisan berantai ke belakang sampai menemukan langkah yang benar-benar mengisi. Efeknya,
	 * data yang diisi pengaju di awal ikut terbawa sepanjang rantai disposisi tanpa perlu disalin
	 * eksplisit tiap tahap. Konsekuensinya juga: nilai hasil warisan itu <b>ikut tersimpan</b> ke
	 * kolom baris ini pada flush berikutnya (getter destruktif atas properti persisten), sehingga
	 * lama-kelamaan tiap baris menyimpan salinannya sendiri.</p>
	 *
	 * <p>Blok {@code try/catch} melindungi dari kegagalan pemuatan rantai langkah sebelumnya
	 * (proxy lazy lepas sesi); bila gagal, nilai yang sudah ada dikembalikan apa adanya.</p>
	 *
	 * @return isian parameter tambahan berbasis id; string kosong bila tidak ada (tidak {@code null})
	 */
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

	/**
	 * Menyetel isian parameter tambahan berbasis id (format lihat
	 * {@link #getParameterTambahanInds()}).
	 *
	 * <p>Biasanya dipanggil dari {@link #populateParameterTambahan(List)} (jalur ZK) atau dari
	 * penyandi pada jalur API. Tidak ada validasi format di sini: string apa pun diterima, dan
	 * kesalahan format baru terasa saat pembacaan (yang memang dirancang toleran).</p>
	 *
	 * @param parameterTambahanInds isian parameter tambahan berbasis id
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mengurai isian parameter tambahan versi berlabel ({@link #getParameterTambahan()}) menjadi
	 * daftar {@link CommonVO} yang siap ditampilkan/dicetak, terurut menurut nomor urut.
	 *
	 * <p>Sumbernya adalah satu string besar: tiap baris (dipisah {@code \n}) mewakili satu
	 * parameter, dan tiap baris terdiri atas ruas-ruas yang dipisah penanda {@code <=>} dengan
	 * urutan: <i>label</i>, <i>nilai</i>, <i>url lampiran</i>, <i>nomor urut</i>, <i>id
	 * parameter</i> (ruas ke-6 dan ke-7 — id kelompok dan catatan — ditulis oleh
	 * {@link #populateParameterTambahan(List)} tetapi tidak dibaca di sini). Label sendiri
	 * berbentuk {@code "<nama kelompok>-><label inputan>"}, dan bagian sebelum {@code ->} disimpan
	 * terpisah pada {@code name5} agar pemanggil bisa mengelompokkan tampilan per kelompok
	 * parameter.</p>
	 *
	 * <p>Pemetaan ke {@link CommonVO}: {@code id} = id parameter, {@code name} = label lengkap,
	 * {@code name1} = nilai, {@code name2} = url lampiran, {@code name5} = nama kelompok,
	 * {@code nomorUrut} = nomor urut. Pengurutan terakhir memakai
	 * {@code Collections.sort(...)} yang mengikuti {@code compareTo} milik {@code CommonVO}
	 * (berbasis nomor urut), sehingga urutan tampil mengikuti urutan yang ditetapkan admin pada
	 * definisi parameter, bukan urutan penyimpanan.</p>
	 *
	 * <p><b>Sifat toleran terhadap data cacat.</b> Setiap ruas dibaca dengan pemeriksaan panjang
	 * array lebih dahulu, dan konversi angka ({@code nomorUrut}, {@code id}) dibungkus
	 * {@code try/catch} yang jatuh ke nilai bawaan 1 sambil mencatat galat ke audit. Ini
	 * disengaja: string parameter berumur panjang dan bisa berasal dari versi format yang lebih
	 * lama, sehingga satu baris rusak tidak boleh menggagalkan seluruh cetakan disposisi. Perlu
	 * dicatat pula bahwa string kosong tetap menghasilkan satu elemen (hasil {@code split} atas
	 * string kosong berisi satu elemen kosong), jadi pemanggil sebaiknya menyaring elemen berlabel
	 * kosong bila tidak ingin menampilkan baris hampa.</p>
	 *
	 * @return daftar parameter tambahan terurut; tidak pernah {@code null}
	 */
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

	/**
	 * Membaca isian parameter tambahan dari baris-baris form ZK lalu menyandikannya menjadi dua
	 * string yang disimpan pada baris jenjang ini: versi berlabel
	 * ({@link #setParameterTambahan(String)}) dan versi berbasis id
	 * ({@link #setParameterTambahanInds(String)}).
	 *
	 * <p>Setiap {@link Row} yang dikirim diharapkan membawa tiga atribut yang ditanam saat form
	 * dibangun: {@code "parameterTambahan"} (definisi parameter), {@code "kelompokParameter
	 * TambahanAlurSop"} (kelompok tempat parameter itu berada pada jenjang ini), dan opsional
	 * {@code "keterangan"} berupa {@link Textbox} catatan per parameter. Baris yang tidak membawa
	 * kedua atribut wajib itu dilewati diam-diam — bentuk form SOP bisa memuat baris hiasan/pemisah
	 * yang bukan isian.</p>
	 *
	 * <p><b>Format yang dihasilkan.</b> Versi berlabel: {@code <namaKelompok>-><labelInputan><=>
	 * <nilai><=><url><=><nomorUrut><=><idParameter><=><idKelompok><=><catatan>}. Versi berbasis id:
	 * {@code <idKelompok>-><idParameter><=><nilai><=><url><=><catatan>}. Keduanya menggabungkan
	 * banyak parameter dengan pemisah baris baru. Dua bentuk disimpan sekaligus karena masing-masing
	 * punya kegunaan: yang berlabel dipakai untuk menampilkan/mencetak riwayat apa adanya (tahan
	 * walau definisi parameter kelak dihapus), sedangkan yang berbasis id dipakai untuk mengisi
	 * ulang form pada jenjang berikutnya (tahan walau label diubah).</p>
	 *
	 * <p><b>Lampiran.</b> Untuk parameter yang menandai {@code harusMenyertakanLampiran}, method
	 * mencari berkas terkait lewat {@code LampiranLain.ambil(getId(), jenis)} dan menyimpan
	 * tautannya pada ruas {@code url}. Kunci {@code jenis} diperoleh dari
	 * {@code LampiranLain.resolveJenisParameterTambahan(DisposisiAlurSop.class, getId(),
	 * "<idKelompok>-><idParameter>")} — pemakaian resolver ini penting karena kunci jenis lampiran
	 * pernah bertabrakan antar modul; resolver-lah yang memutuskan bentuk kunci yang benar
	 * (termasuk bentuk lama) tanpa perlu migrasi data. Perhatikan bahwa kelengkapan lampiran
	 * <b>tidak</b> ditegakkan di sini: bila berkas belum ada, ruas url dibiarkan kosong dan isian
	 * tetap tersimpan; penolakan "lampiran wajib" dilakukan lapis service/UI.</p>
	 *
	 * <p><b>Ketahanan.</b> Seluruh badan perulangan dibungkus {@code try/catch} per baris yang
	 * menampilkan galat hanya kepada admin ({@code Common.tampilErrorJikaAdmin}); satu baris
	 * bermasalah tidak membatalkan penyandian baris lain. Method langsung {@code return} tanpa
	 * menyentuh apa pun bila daftar baris {@code null}/kosong — jadi ia tidak pernah "mengosongkan"
	 * isian yang sudah ada hanya karena form tidak menyertakan baris parameter.</p>
	 *
	 * <p>Method ini hanya mengisi field di memori; penyimpanan ke basis data tetap dilakukan
	 * pemanggil lewat {@code saveOrUpdate}.</p>
	 *
	 * @param parameterRows baris-baris form ZK yang memuat isian parameter tambahan; {@code null}
	 *                      atau kosong berarti tidak ada yang dikerjakan
	 */
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

	/**
	 * Mengembalikan isian parameter tambahan dalam bentuk <b>berlabel</b> (siap dibaca manusia),
	 * dengan pewarisan dari langkah sebelumnya bila baris ini belum punya isian sendiri.
	 *
	 * <p>Format tiap barisnya dijelaskan pada {@link #populateParameterTambahan(List)}, dan
	 * penguraiannya dilakukan {@link #ambilDataParameterTambahan()}. Bentuk berlabel ini sengaja
	 * menyimpan nama kelompok dan label inputan apa adanya sehingga riwayat lama tetap terbaca
	 * meskipun definisi parameternya kelak diubah atau dihapus admin.</p>
	 *
	 * <p>Sama seperti {@link #getParameterTambahanInds()}, isian kosong akan diwarisi dari
	 * {@link #getSebelumnya()} secara berantai; nilai warisan itu ikut tersimpan ke kolom baris ini
	 * pada flush berikutnya karena ini getter destruktif atas properti persisten. Bedanya, di sini
	 * pewarisan <b>tidak</b> dibungkus {@code try/catch}, sehingga kegagalan memuat rantai langkah
	 * sebelumnya akan merambat ke pemanggil.</p>
	 *
	 * @return isian parameter tambahan berlabel; string kosong bila tidak ada (tidak {@code null})
	 */
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

	/**
	 * Menyetel isian parameter tambahan berlabel.
	 *
	 * <p>Tidak ada validasi format; pasangannya yang berbasis id sebaiknya disetel bersamaan agar
	 * kedua bentuk tetap sinkron (itulah yang dilakukan
	 * {@link #populateParameterTambahan(List)}).</p>
	 *
	 * @param parameterTambahan isian parameter tambahan berlabel
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan langkah yang mendisposisikan ke jenjang ini (mata rantai ke belakang).
	 *
	 * <p>Relasi ini yang membentuk urutan riwayat: dari baris mana pun, rantai
	 * {@code sebelumnya} dapat ditelusuri mundur sampai jenjang awal. Selain untuk tampilan, ia
	 * dipakai untuk menghitung tenggat ({@link #getWaktuMaksimal()}), untuk mewarisi isian
	 * parameter tambahan, dan untuk fitur "kembali ke aktor sebelumnya" (revisi).</p>
	 *
	 * <p>Perhatikan bahwa <b>percabangan</b> membuat relasi ini tidak simetris dengan
	 * {@link #getSetelahnya()}: satu langkah bisa memiliki banyak anak yang semuanya menunjuk ke
	 * langkah ini sebagai {@code sebelumnya}, sementara kolom {@code setelahnya} hanya menampung
	 * satu penunjuk. Karena itu kode yang ingin tahu "apakah langkah ini sudah diteruskan" lebih
	 * andal menghitung baris dengan {@code sebelumnya = id} ini.</p>
	 *
	 * @return langkah sebelumnya, atau {@code null} bila ini jenjang awal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sebelumnya", nullable = true)
	public DisposisiAlurSop getSebelumnya() {
		sebelumnya = check(sebelumnya);
		return sebelumnya;
	}

	/**
	 * Menetapkan langkah sebelumnya bagi jenjang ini.
	 *
	 * <p>Setter polos: tidak diperiksa apakah langkah yang ditunjuk berasal dari pengajuan yang
	 * sama, apakah jenjangnya memang rute sah menuju jenjang ini, ataupun apakah rantai yang
	 * terbentuk bebas siklus. Selain dipanggil lapis service saat merangkai langkah baru, setter
	 * ini juga dipanggil dari dalam {@link #getSetelahnya()} untuk memperbaiki penunjuk balik.</p>
	 *
	 * @param sebelumnya langkah yang mendisposisikan ke jenjang ini
	 */
	public void setSebelumnya(DisposisiAlurSop sebelumnya) {
		this.sebelumnya = sebelumnya;
	}

	/**
	 * Menyusun ulang <b>teks pencarian gabungan</b> untuk baris jenjang ini — kolom yang menjadi
	 * sasaran pencarian bebas pada dasbor SOP.
	 *
	 * <p>Isinya adalah rangkaian nilai yang mungkin diketik pengguna saat mencari sebuah
	 * pengajuan, digabung dengan pemisah garis bawah:</p>
	 * <ol>
	 * <li>Dari definisi jenjang: kode, nama, aktor, dan username khusus.</li>
	 * <li>Dari header pengajuan: nama pengaju (pengguna internal), atau NIM + nama mahasiswa, atau
	 * nomor induk + nomor induk nasional + nama siswa.</li>
	 * <li>Dari pelaku jenjang ini sendiri: dengan tiga kemungkinan yang sama seperti di atas.</li>
	 * <li>Catatan disposisi ({@link #getKeterangan()}).</li>
	 * </ol>
	 *
	 * <p>Kolom {@code keyword} diindeks GIN {@code gin_trgm_ops} oleh {@code InitIndex}, sehingga
	 * pencarian "mengandung" dapat berjalan cepat pada tabel yang besar. Karena itu nilainya
	 * <b>selalu dibangun ulang</b> setiap kali getter dipanggil (baris pertama mengosongkan
	 * {@code keyword}) — dan karena ini properti persisten, hasil bangun ulang tersebut ikut
	 * tersimpan saat flush, sehingga indeks tetap segar mengikuti perubahan nama/aktor.</p>
	 *
	 * <p><b>Tiga blok {@code try/catch} terpisah</b> bukan kebetulan (lihat komentar di badan
	 * method): relasi seperti {@code alurSop} dan {@code disposisiSop} bisa berupa instance
	 * kanonik/berbagi milik {@code AuditTimestampInterceptor} yang proxy-nya terikat pada
	 * {@link Session} lain yang sudah ditutup. Dengan memisahkan blok, kegagalan pada satu sumber
	 * data tidak menghapus kontribusi sumber lain: bagian yang gagal dilewati (dicatat ke audit
	 * galat) dan bagian selebihnya tetap masuk. Konsekuensi yang perlu disadari: bila pemuatan
	 * gagal saat entity kebetulan sedang di-flush, kolom {@code keyword} bisa tersimpan dalam
	 * keadaan tidak lengkap sehingga baris tersebut lebih sulit ditemukan lewat pencarian sampai
	 * ada penyimpanan berikutnya yang berhasil membangunnya penuh.</p>
	 *
	 * <p><b>Catatan privasi:</b> kolom ini memuat nama, NIM, dan nomor induk. Setiap fitur yang
	 * menampilkan atau mengekspor {@code keyword} mentah perlu tunduk pada pembatasan akses yang
	 * sama dengan data aslinya.</p>
	 *
	 * @return teks pencarian gabungan untuk baris jenjang ini
	 */
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

	/**
	 * Menyetel teks pencarian gabungan.
	 *
	 * <p>Praktis tidak berguna dipanggil dari luar: {@link #getKeyword()} membangun ulang nilainya
	 * dari nol setiap kali dipanggil, sehingga apa pun yang disetel di sini akan tertimpa pada
	 * pembacaan berikutnya. Setter tetap ada karena dibutuhkan Hibernate untuk memetakan properti.</p>
	 *
	 * @param keyword teks pencarian gabungan
	 */
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	/**
	 * Mengembalikan langkah lanjutan dari jenjang ini (mata rantai ke depan), sekaligus
	 * <b>memperbaiki penunjuk balik</b> pada langkah lanjutan tersebut.
	 *
	 * <p>Selain me-resolve proxy lazy lewat {@code check()}, getter ini memanggil
	 * {@code setelahnya.setSebelumnya(this)} bila baris ini sudah punya id. Tujuannya menjaga
	 * rantai tetap konsisten dua arah walau salah satu sisi belum tersimpan/terisi: object langkah
	 * lanjutan yang dibaca dari database mungkin membawa {@code sebelumnya} berupa instance lain
	 * (atau proxy yang tidak dapat dimuat), sedangkan pemanggil di sini sudah memegang instance
	 * yang benar. <b>Efek sampingnya perlu disadari</b>: membaca {@code setelahnya} berarti
	 * <i>menulis</i> ke object lain, dan karena {@code sebelumnya} adalah properti persisten,
	 * perbaikan itu dapat ikut tersimpan sebagai {@code UPDATE} pada baris langkah lanjutan saat
	 * flush.</p>
	 *
	 * <p>Blok {@code try/catch} di sekelilingnya memastikan kegagalan perbaikan penunjuk balik
	 * (mis. proxy langkah lanjutan lepas dari sesinya) tidak menggagalkan pembacaan: pada jalur
	 * catch, {@code setelahnya} tetap di-resolve dan dikembalikan.</p>
	 *
	 * <p>Ingat keterbatasannya pada alur bercabang: kolom {@code setelahnya} hanya menampung satu
	 * penunjuk, sedangkan satu langkah dapat mendisposisi ke beberapa aktor. Untuk kasus itu,
	 * sumber kebenarannya adalah kumpulan baris anak dengan {@code sebelumnya = langkah ini}, bukan
	 * nilai yang dikembalikan method ini. {@link #setujui()} dan {@link #getSelesai()} bergantung
	 * pada nilai ini, sehingga urutan penulisan rantai oleh lapis service (mengisi
	 * {@code setelahnya} sebelum memperbarui status header) berpengaruh pada hasil yang terbaca.</p>
	 *
	 * @return langkah lanjutan, atau {@code null} bila jenjang ini masih ujung rantai
	 */
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

	/**
	 * Menetapkan langkah lanjutan dari jenjang ini.
	 *
	 * <p>Setter polos tanpa validasi: tidak diperiksa apakah jenjang tujuan termasuk rute yang
	 * diizinkan definisi ({@code AlurSop.ambilAlurSetelahnya()}), apakah berasal dari pengajuan
	 * yang sama, atau apakah menciptakan siklus. Karena {@link #setujui()} menyimpulkan status
	 * persetujuan dari pasangan (jenjang ini, jenjang setelahnya), nilai yang disetel di sini ikut
	 * menentukan apakah sebuah langkah terbaca sebagai "menyetujui" — satu alasan lagi mengapa
	 * pembentukan rantai harus dijaga ketat di lapis service.</p>
	 *
	 * @param setelahnya langkah lanjutan dari jenjang ini
	 */
	public void setSetelahnya(DisposisiAlurSop setelahnya) {
		this.setelahnya = setelahnya;
	}

	/**
	 * Nilai bawaan kolom {@code properti}: representasi teks sebuah {@link JSONObject} kosong
	 * ({@code "{}"}).
	 *
	 * <p>Bersifat {@code public static} dan <b>tidak final</b> mengikuti pola yang sama di banyak
	 * entity AIS, sehingga secara teknis bisa diubah dari luar; perlakukan sebagai tetapan dan
	 * jangan menulisinya.</p>
	 */
	public static String JSON = new JSONObject().toString();

	/**
	 * Mengembalikan kantong properti bebas berformat JSON milik baris jenjang ini.
	 *
	 * <p>Kolom ini dipakai modul pemanggil untuk menitipkan data tambahan yang tidak layak
	 * mendapat kolom tersendiri. Bila kosong, yang dikembalikan adalah JSON kosong
	 * {@code "{}"} ({@link #JSON}) — bukan {@code null} — supaya pemanggil dapat langsung mengurai
	 * hasilnya tanpa pemeriksaan tambahan.</p>
	 *
	 * <p>Nilai bawaan itu hanya diberikan pada pembacaan; ia tidak ditulis balik ke field, jadi
	 * getter ini termasuk yang <b>tidak</b> destruktif. Perlu dicatat bahwa kolom {@code properti}
	 * pada tabel ini pernah menjadi perhatian dari sisi kinerja (ukurannya besar dan diindeks GIN
	 * trigram), sehingga sebaiknya tidak dijadikan tempat menyimpan data bervolume besar.</p>
	 *
	 * @return teks JSON properti; {@code "{}"} bila belum diisi
	 */
	@Column(name = "properti", nullable = true, columnDefinition = "text")
	public String getProperti() {
		return properti == null || properti.isEmpty() ? JSON : properti;
	}

	/**
	 * Menyetel kantong properti bebas berformat JSON.
	 *
	 * <p>Tidak ada validasi bahwa isinya benar-benar JSON yang sahih; pemanggil bertanggung jawab
	 * atas bentuk isinya.</p>
	 *
	 * @param properti teks JSON properti
	 */
	public void setProperti(String properti) {
		this.properti = properti;
	}

	/**
	 * Mengisi {@code Map} parameter laporan dengan seluruh jejak disposisi sebuah pengajuan, agar
	 * template cetak (JasperReports) dapat menampilkan siapa memproses apa, kapan, dengan jabatan
	 * apa, dan dengan tanda tangan siapa.
	 *
	 * <p>Ini adalah jembatan antara model SOP dan lembar cetak. Karena template dibuat oleh
	 * pengguna/implementator dan tidak dapat menjalankan kueri sendiri, method ini
	 * "meratakan" (flatten) seluruh riwayat disposisi menjadi kunci-kunci teks sederhana yang
	 * tinggal ditempelkan di template.</p>
	 *
	 * <h2>Data yang dibaca</h2>
	 * <p>Seluruh {@code DisposisiAlurSop} milik pengajuan diambil lewat sesi Hibernate berjalan
	 * ({@code HibernateUtil.currentSession()}), disaring {@code alurSop is not null}, dan
	 * <b>diurutkan menaik menurut id</b> — jadi urutan pada cetakan mengikuti urutan pembuatan
	 * baris (kronologis), bukan urutan rantai {@code sebelumnya}/{@code setelahnya}. Pada alur
	 * bercabang keduanya bisa berbeda, sehingga penomoran "ke-<i>n</i>" pada cetakan sebaiknya
	 * dibaca sebagai "baris ke-<i>n</i>", bukan "jenjang ke-<i>n</i> menurut alur".</p>
	 *
	 * <h2>Kunci untuk pengaju</h2>
	 * <p>Bila pengaju header adalah pengguna internal yang tertaut ke pegawai, ditambahkan
	 * {@code jabatanFungsional_sop_pengaju}, {@code jabatanStruktural_sop_pengaju},
	 * {@code jabatan_sop_pengaju}, serta {@code ttd_pengaju} (path berkas tanda tangan). Jabatan
	 * dihitung menurut <b>keadaan saat ini</b> ({@code WaktuUtil.getDate()}) lewat riwayat kenaikan
	 * pangkat pegawai — perlu disadari bahwa mencetak ulang dokumen lama akan menampilkan jabatan
	 * terbaru pejabat, bukan jabatan pada saat dokumen dulu disetujui.</p>
	 *
	 * <h2>Kunci untuk tiap langkah</h2>
	 * <p>Untuk setiap baris disposisi, kunci yang sama ditulis dalam <b>tiga penamaan sekaligus</b>
	 * supaya template lama maupun baru tetap jalan:</p>
	 * <ul>
	 * <li>berdasarkan nomor urut baris: {@code ..._sop_ke_<n>} (mis. {@code aktor_sop_ke_1},
	 * {@code oleh_sop_ke_1}, {@code catatan_sop_ke_1}, {@code tanggal_sop_ke_1});</li>
	 * <li>berdasarkan id definisi jenjang: {@code ..._sop_<idAlurSop>};</li>
	 * <li>berdasarkan kode definisi jenjang: {@code ..._sop_<kodeAlurSop>} — bentuk yang paling
	 * stabil dan paling disarankan untuk template baru, karena tidak berubah walau urutan berubah
	 * atau data dipindah antar basis data.</li>
	 * </ul>
	 * <p>Isi kuncinya: aktor jenjang, nama pemroses, satuan kerja pemroses, catatan disposisi, dan
	 * waktu dalam tiga format ({@code tanggal_} panjang, {@code tgl_} pendek, {@code waktu_}
	 * lengkap dengan jam). Untuk pemroses yang tertaut pegawai ditambahkan pula nama/kode/mycode
	 * pegawai, nama aktor SOP, ketiga jabatan, dan path tanda tangan ({@code ttd_ke_<n>},
	 * {@code ttd_<idAlurSop>}, {@code ttd_pegawai_<idPegawai>}, {@code ttd_pegawai_<kodeAlurSop>}).
	 * Tanda tangan dipilih menurut peran: guru memakai {@code LampiranLain.TTD_GURU}, dosen memakai
	 * {@code TTD_DOSEN}, selain itu {@code TTD_PEGAWAI}.</p>
	 *
	 * <h2>Hal yang perlu diwaspadai</h2>
	 * <ul>
	 * <li><b>Tanda tangan ikut tercetak semata-mata karena baris disposisi ada.</b> Method ini
	 * hanya membaca; ia tidak memeriksa apakah baris tersebut sah, apakah pemrosesnya memang aktor
	 * berwenang, atau apakah pengajuan sudah benar-benar disetujui. Dokumen bertanda tangan yang
	 * dihasilkan karenanya hanya sekuat proses yang menghasilkan baris disposisinya.</li>
	 * <li><b>Banyak pemanggilan berulang.</b> Getter {@code getDiajukanOleh()},
	 * {@code getAlurSop()}, dan {@code getWaktu()} dipanggil berkali-kali per baris, dan sebagian
	 * di antaranya destruktif (menulis balik ke field). Karena method ini memakai sesi berjalan —
	 * bukan sesi terpisah — pencetakan berpotensi menandai entity sebagai kotor dan memicu
	 * penulisan saat flush. Kalau perlu mencetak dalam konteks yang sensitif, pertimbangkan sesi
	 * dengan {@code FlushMode.MANUAL} seperti yang dilakukan
	 * {@link DisposisiSop#ambil(Session, ais.ui.util.FormSop)}.</li>
	 * <li><b>Berkas tanda tangan diakses lewat path absolut</b> ({@code ambilFile()
	 * .getAbsolutePath()}), jadi berkas harus benar-benar ada di penyimpanan server saat mencetak.</li>
	 * <li>Bila pengajuan {@code null}, method tidak melakukan apa-apa; {@code parameters} yang
	 * sudah ada tidak pernah dihapus, hanya ditambah/ditimpa per kunci.</li>
	 * </ul>
	 *
	 * @param disposisiSop header pengajuan yang riwayat disposisinya akan diratakan; {@code null}
	 *                     berarti tidak ada yang dikerjakan
	 * @param parameters   map parameter laporan yang akan diisi (kunci bertipe {@code String})
	 */
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
