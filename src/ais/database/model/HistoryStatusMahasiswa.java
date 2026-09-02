package ais.database.model;

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

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import ais.common.Common;
import ais.common.ConstantValues;

/**
 * Entity <b>riwayat status kemahasiswaan per semester</b> (tabel {@code public.history_status_mahasiswa},
 * {@code @Audited} Envers, {@code dynamicInsert}/{@code dynamicUpdate}). Satu baris menjawab pertanyaan
 * <i>"mahasiswa X berstatus apa pada semester Y?"</i> — Aktif, Cuti, Nonaktif (Tidak Aktif), Lulus,
 * Drop Out, atau Keluar — beserta konteks periodenya (tahun akademik, ganjil/genap/SP, tahap, SKS yang
 * diambil, status awal, program) dan catatan administratifnya (tanggal berlaku status, keterangan).
 *
 * <h2>Riwayat, bukan "status terkini"</h2>
 * <p>Nama kelas ini menyesatkan bila dibaca sepintas: ini bukan tabel arsip pendamping dari sebuah
 * kolom "status sekarang" di {@link Mahasiswa}. {@link Mahasiswa} <b>tidak punya</b> properti
 * {@code statusMahasiswa} sama sekali — yang ada di sana hanyalah:</p>
 * <ul>
 *   <li>{@link Mahasiswa#getStatusKeluar()} — status <i>terminal</i> (Lulus/Keluar/Drop Out), yaitu
 *       nasib akhir mahasiswa, bukan status per semester;</li>
 *   <li>{@link Mahasiswa#getKelompokStatusMahasiswa()} — aturan <i>override</i> berbasis rentang
 *       semester ({@code smtMulai}..{@code smtSampai} &rarr; satu {@link StatusMahasiswa});</li>
 *   <li>{@link Mahasiswa#getPaksaAktifSemester()} — daftar semester yang dipaksa Aktif oleh admin.</li>
 * </ul>
 * <p>Jadi kelas inilah SATU-SATUNYA tempat penyimpanan status per semester. "Status terkini" adalah
 * sekadar baris riwayat pada semester berjalan, yang diambil lewat
 * {@code HistoryStatusMahasiswaUtil.currentStatus(...)}.</p>
 *
 * <h2>Relasi dengan {@code HistoryStatusMahasiswaUtil}</h2>
 * <p>{@code ais.action.master.helper.HistoryStatusMahasiswaUtil} adalah lapisan
 * <b>pencarian, caching, dan penyimpanan</b> di atas entity ini: query Criteria ke tabel, cache dua
 * lapis (RAM lewat {@code GeneralValueObject.ambilDataLangsung} + JSON per-mahasiswa), pembuatan baris
 * default bila belum ada, sinkronisasi massal, serta mesin aturan lanjutan
 * ({@code kalkulasiStatusLogikaLanjutan}: lambat bayar, kegiatan bersyarat-aktif, status terminal,
 * paksa aktif). Pembagian tugasnya kira-kira begini:</p>
 * <ul>
 *   <li><b>Util</b> menentukan baris MANA yang dipakai dan menyimpannya ke DB.</li>
 *   <li><b>Entity ini</b> menghitung ulang isi baris tersebut saat dibaca, memakai data
 *       {@link Mahasiswa} yang ditautkan (lihat {@link #ambilStatusMahasiswa(Integer)}).</li>
 * </ul>
 * <p>Akibatnya kedua lapis punya potongan aturan status yang <b>tumpang tindih</b> (mis. Lulus/DO
 * retroaktif berdasarkan {@code statusKeluar} dievaluasi di kedua tempat). Bila mengubah aturan status,
 * PERIKSA KEDUANYA — mengubah salah satu saja menghasilkan status yang berbeda tergantung jalur
 * pemanggilan (dari cache Util vs dari getter entity).</p>
 *
 * <h2>PERINGATAN: getter di kelas ini bukan getter pasif</h2>
 * <p>Ini karakter paling penting dan paling berbahaya dari kelas ini. Karena {@code @Id} dipasang pada
 * <i>getter</i> ({@link #getId()}), Hibernate memakai <b>property access</b> — artinya
 * getter-getter di bawah ini adalah yang dibaca Hibernate saat dirty-checking / flush, dan
 * semuanya <b>menulis balik ke field</b> saat dipanggil:</p>
 * <table border="1" summary="Getter yang punya efek samping">
 *   <tr><th>Getter</th><th>Efek samping</th></tr>
 *   <tr><td>{@link #getMahasiswa()}</td><td>menugaskan ulang hasil {@code check()} (resolusi proxy lazy)</td></tr>
 *   <tr><td>{@link #getStatusMahasiswa()}</td><td>menghitung ULANG seluruh status lewat {@link #ambilStatusMahasiswa(Integer)} dan menimpa field</td></tr>
 *   <tr><td>{@link #getSemester()}</td><td>menghitung semester dari tahun akademik + angkatan bila masih {@code null}</td></tr>
 *   <tr><td>{@link #getGanjilGenap()}</td><td>menurunkan ganjil/genap/SP dari {@code semester}/{@code sp}</td></tr>
 *   <tr><td>{@link #getStatusAwalMahasiswa()}</td><td>menghitung ulang status awal lewat {@link #ambilStatusAwal}</td></tr>
 *   <tr><td>{@link #getProgram()}</td><td>menyalin program dari {@link Mahasiswa} / {@link ProgramMahasiswa}</td></tr>
 *   <tr><td>{@link #toString()}</td><td>memanggil dua getter di atas, jadi ikut memicu perhitungan penuh</td></tr>
 * </table>
 * <p>Konsekuensi yang harus disadari:</p>
 * <ol>
 *   <li><b>Membaca bisa menulis ke database.</b> Bila instance masih terikat session Hibernate,
 *       nilai baru hasil perhitungan getter akan terlihat oleh dirty-check dan ikut ter-{@code UPDATE}
 *       saat flush — beserta satu baris baru di tabel audit Envers. Jadi sekadar menampilkan daftar
 *       riwayat di layar dapat <i>mengubah</i> riwayat itu sendiri.</li>
 *   <li><b>{@code toString()} mahal.</b> Jangan dipakai di dalam loop panas atau logging debug —
 *       satu pemanggilan bisa memicu resolusi proxy, query cuti, dan seluruh mesin aturan status.</li>
 *   <li><b>Hasil tidak deterministik terhadap waktu.</b> Status yang dikembalikan bergantung pada
 *       kondisi {@link Mahasiswa} SAAT INI (status keluar, kelompok status, cuti, tunggakan), bukan
 *       pada apa yang tersimpan di kolom saat baris dibuat.</li>
 * </ol>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Audit shadow</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas &amp; relasi</b> — {@link #getId()}/{@link #setId(Long)},
 *       {@link #getMahasiswa()}/{@link #setMahasiswa(Mahasiswa)}.</li>
 *   <li><b>Mesin status</b> — {@link #ambilStatusMahasiswa(Integer)} (inti aturan),
 *       {@link #getStatusMahasiswa()}, {@code statusSama} (pembanding id yang null-safe).</li>
 *   <li><b>Penentu periode</b> — {@link #getSemester()}, {@link #getGanjilGenap()},
 *       {@link #getTahunAkademik()}, {@link #getTahap()}, {@link #getSp()}.</li>
 *   <li><b>Utilitas statis lintas-kelas</b> — {@link #ambilStatusAwal(Mahasiswa, Integer, StatusAwalMahasiswa)}
 *       dan {@link #ambilProgram(Mahasiswa, Integer, String)}; keduanya dipanggil dari luar
 *       (mis. {@link Kegiatan}, {@code SkripsiAction}, {@code MahasiswaAction}) sebagai fungsi murni
 *       tanpa perlu instance entity.</li>
 *   <li><b>Data administratif polos</b> — {@link #getKeterangan()}, {@link #getTanggalStatus()},
 *       {@link #getSks()}.</li>
 * </ul>
 *
 * <h2>Catatan lapangan</h2>
 * <p>Beberapa perbaikan {@code NullPointerException} sudah dipatri sebagai komentar inline di
 * {@link #ambilStatusMahasiswa(Integer)} dan {@link #ambilStatusAwal}. Semuanya berasal dari kasus
 * nyata: getter dipanggil dari thread pool tagihan, dari reflection {@code BeanUtils.copyProperties}
 * saat sinkronisasi cache JSON, dan dari objek yang field-nya belum terisi lengkap. Jangan
 * "membersihkan" pengecekan null yang terlihat berlebihan di sana tanpa membaca komentarnya dulu.</p>
 *
 * <p>Field {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} sengaja
 * DIDEKLARASIKAN ULANG di kelas ini meski juga ada di {@link GeneralValueObject}. Itu bukan duplikasi
 * yang keliru: {@link GeneralValueObject} adalah POJO abstrak biasa — <b>bukan</b> {@code @Entity}
 * maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti induknya sama sekali.
 * Deklarasi ulang adalah keharusan teknis agar kolom audit ikut tersimpan.</p>
 *
 * @see ais.action.master.helper.HistoryStatusMahasiswaUtil
 * @see Mahasiswa
 * @see StatusMahasiswa
 * @see StatusAwalMahasiswa
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "history_status_mahasiswa")
public class HistoryStatusMahasiswa extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci primer baris riwayat (kolom {@code id}, IDENTITY). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (audit shadow). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id/NIP pengguna terakhir yang mengubah baris ini (audit shadow). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir mengubah baris riwayat ini.
	 *
	 * @return id/NIP pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah. <b>Nilai {@code null} atau string kosong DIABAIKAN</b> — field
	 * lama dipertahankan. Pola ini disengaja: kolom audit tidak boleh terhapus oleh jalur
	 * penyimpanan yang kebetulan tidak tahu siapa penggunanya (mis. batch/import).
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan agar jejak audit tidak hilang.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir mengubah baris riwayat ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini,
	 * lalu mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} untuk memperbarui
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} dari konteks pengguna yang sedang aktif.
	 *
	 * <p>Tidak untuk dipanggil manual.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir (audit shadow). Diinisialisasi ke waktu server saat objek
	 * dibuat, lalu diperbarui {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi cap waktu perubahan terakhir. Berbeda dari {@link #setOleh(String)}, di sini
	 * {@code null} diterima apa adanya.
	 *
	 * @param tanggal_dirubah cap waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu perubahan terakhir baris ini (kolom {@code TIMESTAMP}).
	 *
	 * @return cap waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris riwayat: {@code "<mahasiswa>-<status>-<semester>-<tahunAkademik>"}.
	 *
	 * <p><b>PERINGATAN — method ini MAHAL dan punya efek samping.</b> Ia sengaja memanggil
	 * {@link #getMahasiswa()} dan {@link #getStatusMahasiswa()} (bukan membaca field mentah),
	 * sehingga satu pemanggilan dapat memicu resolusi proxy lazy, query cuti, pembacaan konfigurasi,
	 * dan seluruh mesin aturan {@link #ambilStatusMahasiswa(Integer)} — lalu menimpa field
	 * {@code mahasiswa} dan {@code statusMahasiswa} dengan hasilnya. Hindari memakainya di dalam
	 * loop, logging debug bervolume tinggi, atau breakpoint inspector saat sesi Hibernate masih
	 * terbuka.</p>
	 *
	 * @return gabungan mahasiswa, status, semester, dan tahun akademik dipisahkan tanda hubung
	 */
	public String toString() {
		mahasiswa = getMahasiswa();
		statusMahasiswa = getStatusMahasiswa();
		return mahasiswa + "-" + statusMahasiswa + "-" + semester + "-" + tahunAkademik;
	}

	/** Mahasiswa pemilik riwayat ini (kolom {@code mahasiswa}, wajib, lazy). */
	private Mahasiswa mahasiswa;
	/** Status hasil perhitungan pada semester ini (kolom {@code status_mahasiswa}). */
	private StatusMahasiswa statusMahasiswa;
	/** Status awal/kategori masuk yang berlaku pada semester ini (kolom {@code status_awal_mahasiswa}). */
	private StatusAwalMahasiswa statusAwalMahasiswa;
	/** Penanda Semester Pendek; bernilai {@link Perkuliahan#SEMESTER_PENDEK} untuk baris SP. */
	private Integer sp;
	/** Nomor semester mahasiswa (1, 2, 3, ...) yang diwakili baris ini. */
	private Integer semester;
	/** Nomor tahap, dipakai bila penomoran tahapan diaktifkan ({@code ConstantValues.aktifkanTahapan}). */
	private Integer tahap;
	/** Jumlah SKS yang diambil pada semester ini; dipakai sebagai bukti keaktifan. */
	private Integer sks;
	/** Tahun akademik periode ini, format {@code "2025/2026"}. */
	private String tahunAkademik;
	/** Jenis semester: {@link Perkuliahan#GANJIL}, {@link Perkuliahan#GENAP}, atau {@link Perkuliahan#SP}. */
	private String ganjilGenap;
	/** Catatan administratif bebas atas status ini (mis. alasan nonaktif), diisi dari layar Studi Mahasiswa. */
	private String keterangan;
	/** Tanggal berlakunya status ini; dipakai laporan EMIS/EPSBED sebagai tanggal perubahan status. */
	private Date tanggalStatus;
	/** Program/kelas (Reguler, Karyawan, dsb.) yang berlaku pada semester ini (kolom {@code program}). */
	private String program;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Semua field dibiarkan {@code null} kecuali
	 * {@link #tanggal_dirubah} yang terisi waktu server.
	 */
	public HistoryStatusMahasiswa() {
	}

	/**
	 * Konstruktor pintas untuk membuat baris riwayat yang langsung ditandai reguler atau Semester
	 * Pendek.
	 *
	 * @param sp {@link Perkuliahan#SEMESTER_PENDEK} untuk baris SP, {@code null} untuk baris reguler
	 */
	public HistoryStatusMahasiswa(Integer sp) {
		this.sp = sp;
	}

	/**
	 * Konstruktor pintas berisi kombinasi minimal yang dipakai hasil generate {@code hbm2java}:
	 * periode, beban SKS, dan penanda SP.
	 *
	 * @param tahunAkademik tahun akademik, format {@code "2025/2026"}
	 * @param sks           jumlah SKS yang diambil pada periode itu
	 * @param sp            {@link Perkuliahan#SEMESTER_PENDEK} untuk baris SP, {@code null} bila reguler
	 */
	public HistoryStatusMahasiswa(String tahunAkademik, Integer sks, Integer sp) {
		this.tahunAkademik = tahunAkademik;
		this.sks = sks;
		this.sp = sp;
	}

	/**
	 * Kunci primer baris riwayat. Kolom {@code id} bertipe IDENTITY dan {@code insertable = false},
	 * jadi nilainya diisi database saat {@code INSERT}, bukan oleh aplikasi.
	 *
	 * <p>Karena anotasi {@code @Id} dipasang di sini (pada getter), seluruh pemetaan Hibernate kelas
	 * ini memakai <b>property access</b> — semua getter di bawah ikut dibaca Hibernate saat
	 * dirty-check/flush. Lihat peringatan pada Javadoc kelas.</p>
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
	 * Mengisi kunci primer. Umumnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci primer baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mahasiswa pemilik baris riwayat ini (kolom {@code mahasiswa}, {@code NOT NULL}, {@code LAZY}).
	 *
	 * <p><b>Efek samping:</b> memanggil {@link GeneralValueObject#check(Object)} lalu
	 * <b>menugaskan hasilnya kembali</b> ke field, yakni pola resolusi proxy lazy standar repo ini.
	 * Karena itu getter ini bisa menyentuh cache in-memory atau membuka session Hibernate baru bila
	 * proxy-nya sudah mati.</p>
	 *
	 * <p><b>Bisa mengembalikan {@code null}</b> meski kolomnya {@code NOT NULL}: {@code check()}
	 * menyerah dan mengembalikan {@code null} bila barisnya sudah terhapus atau proxy-nya tidak bisa
	 * diinisialisasi ulang. Semua pemanggil di kelas ini sudah memperhitungkan hal itu — lihat
	 * komentar KE-FIX di {@link #ambilStatusMahasiswa(Integer)}.</p>
	 *
	 * @return mahasiswa pemilik riwayat, atau {@code null} bila proxy tidak bisa diresolusi
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menautkan baris riwayat ini ke seorang mahasiswa. Penugasan langsung tanpa validasi.
	 *
	 * @param mahasiswa mahasiswa pemilik riwayat
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * <b>Inti mesin aturan status</b> di sisi entity: menentukan status mahasiswa pada
	 * {@code semester} tertentu berdasarkan kondisi {@link Mahasiswa} saat ini, lalu
	 * <b>menyimpan hasilnya ke field {@code statusMahasiswa}</b> dan mengembalikannya.
	 *
	 * <p><b>Alur keputusan</b> (dievaluasi berurutan; yang pertama cocok menang):</p>
	 * <ol>
	 *   <li>Bila {@code mahasiswa} belum tertaut, tidak ada yang bisa dihitung — cukup resolusi proxy
	 *       {@code statusMahasiswa} apa adanya.</li>
	 *   <li>Menentukan <b>batas semester</b> {@code jumlah_semester}: {@link Mahasiswa#getSemesterLulus()}
	 *       bila ada, kalau tidak {@code Jenjang.getJumlahSemester()}; default 8 bila keduanya gagal.</li>
	 *   <li><b>Status terminal retroaktif</b>: bila {@code semester >= jumlah_semester} dan
	 *       {@link Mahasiswa#getStatusKeluar()} bernama "Lulus" &rarr; {@link ConstantValues#LULUS};
	 *       bila namanya mengandung "keluar" &rarr; {@link ConstantValues#DROP_OUT}.
	 *       (Perhatikan pemetaan yang membingungkan ini: status keluar bernama "…keluar…" dipetakan
	 *       ke DROP_OUT, sedangkan {@code ConstantValues.KELUAR} baru dipakai di cabang dalam.)</li>
	 *   <li><b>Override kelompok</b>: bila {@link Mahasiswa#getKelompokStatusMahasiswa()} punya rentang
	 *       {@code smtMulai}..{@code smtSampai} yang mencakup semester ini, statusnya dipakai langsung.</li>
	 *   <li><b>Cuti</b>: bila {@link Mahasiswa#ambilCuti(Integer, Integer, boolean)} menemukan pengajuan
	 *       cuti yang SUDAH disetujui untuk semester ini &rarr; {@link ConstantValues#CUTI}.</li>
	 *   <li><b>Bukti keaktifan</b>: bila ada SKS terambil ({@code getSks() > 0}) atau ini semester 1,
	 *       dan mahasiswa belum punya status keluar &rarr; {@link ConstantValues#AKTIF}.</li>
	 *   <li>Selain itu masuk ke blok <b>pembayaran &amp; batas studi</b>: mengulang cek terminal
	 *       (termasuk {@link ConstantValues#KELUAR} untuk status keluar bernama lain), lalu memeriksa
	 *       daftar {@link Mahasiswa#getBatasStudi()} (daftar semester dipisah koma yang menonaktifkan),
	 *       lalu flag pembayaran {@code "checkStatusPembayaranMahasiswa"} (lihat di bawah), lalu
	 *       "pemulihan": mahasiswa yang belum melewati batas semester dan tidak punya status keluar
	 *       tetapi statusnya terlanjur Lulus/Keluar/DO/Cuti dikembalikan menjadi Aktif.</li>
	 *   <li>Tiga koreksi penutup: (a) status Lulus tanpa {@code statusKeluar} diturunkan menjadi
	 *       Tidak Aktif — Lulus tanpa SK dianggap tidak sah; (b) status Tidak Aktif dibatalkan menjadi
	 *       Aktif bila flag pembayaran bernilai {@code "true"} dan mahasiswa tidak kena batas studi;
	 *       (c) status Cuti dibatalkan menjadi Aktif bila pengajuan cutinya hilang atau belum disetujui.</li>
	 * </ol>
	 *
	 * <p><b>Flag {@code "checkStatusPembayaranMahasiswa"}</b> dibaca lewat
	 * {@link GeneralValueObject#retreive(String)} — bukan kolom database, melainkan penyimpanan
	 * kunci-nilai per-instance milik {@link GeneralValueObject}. Yang menulisnya adalah
	 * {@code ais.action.master.BaypassPembayaranMahasiswaAction} ({@code put("true"/"false",
	 * "checkStatusPembayaranMahasiswa")}) saat menjalankan bypass/rekalkulasi pembayaran. Bila baris
	 * riwayat ini tidak berasal dari jalur itu, flag akan kosong dan kedua cabang yang memakainya
	 * dilewati.</p>
	 *
	 * <p><b>Ketahanan galat.</b> Seluruh badan method dibungkus {@code try/catch} yang mencatat ke
	 * {@code ErrorAuditUtil}; pengambilan {@code statusKeluar}, {@code kelompokStatus},
	 * {@code batasStudi}, dan data cuti masing-masing juga dibungkus {@code try/catch} sendiri supaya
	 * satu relasi yang mati tidak menggagalkan seluruh perhitungan. Bila terjadi galat, nilai
	 * {@code statusMahasiswa} yang sudah sempat terhitung tetap dikembalikan.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@link #getStatusMahasiswa()} (jadi ikut terpicu oleh Hibernate saat
	 * flush dan oleh {@link #toString()}), serta langsung dari {@code KrsHelper},
	 * {@code KrsPaketHelper}, {@code KrsNonPaketHelper}, {@code KrsKurikulumHelper},
	 * {@code StudiMahasiswaHelper}, {@code MahasiswaAction}, dan {@code ElearningApiUtil}.</p>
	 *
	 * @param semester nomor semester yang dinilai; {@code null} diperlakukan sebagai {@code 0}
	 *                 (efeknya sama seperti belum memulai perkuliahan)
	 * @return status pada semester tersebut; dapat {@code null} bila baris belum punya status dan
	 *         tidak ada aturan yang cocok
	 */
	public StatusMahasiswa ambilStatusMahasiswa(Integer semester) {
		try {
			if (mahasiswa == null) {
				statusMahasiswa = check(statusMahasiswa);
			} else {
				mahasiswa = getMahasiswa();
				/* KE-FIX NullPointerException di ambilStatusMahasiswa (dipanggil dari
				 * PembayaranUtilHelper.getDetailBiayaMahasiswadariDatabase pada thread pool
				 * TagihanUIBuilder): getMahasiswa() memanggil check(mahasiswa) yang BISA
				 * mengembalikan null saat proxy lazy-nya sudah mati / barisnya terhapus.
				 * Jadi meskipun pemeriksaan null di atas lolos, variabel mahasiswa dapat
				 * berubah menjadi null TEPAT DI SINI, lalu seluruh blok di bawah
				 * (mahasiswa.getStatusKeluar(), mahasiswa.ambilCuti(), ...) meledak dan
				 * perhitungan tagihan mahasiswa itu gagal total. Bila terjadi, jatuhkan ke
				 * penanganan yang SAMA seperti cabang "mahasiswa == null" di atas. */
				if (mahasiswa == null) {
					statusMahasiswa = check(statusMahasiswa);
					return statusMahasiswa;
				}
				// FIX NPE (auto-unboxing): parameter semester bisa null (mis. dipanggil dari jalur
				// kalkulasiStatusLogikaLanjutan/thread async dengan objek yang semester-nya belum
				// diisi) -> "semester >= jumlah_semester" di bawah meledak saat unboxing meski
				// jumlah_semester sudah dijaga. Default 0 (efeknya sama seperti belum mulai smt).
				if (semester == null) {
					semester = 0;
				}
				Integer jumlah_semester = 8;
				try {
					Jurusan jurusan = mahasiswa.getJurusan();
					Jenjang jenjang = jurusan.getJenjang();
					jumlah_semester = mahasiswa.getSemesterLulus() != null ? mahasiswa.getSemesterLulus()
							: jenjang.getJumlahSemester();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/HistoryStatusMahasiswa.java:148");
//				e.printStackTrace();
				}
				// FIX NPE (auto-unboxing): getJumlahSemester()/getSemesterLulus() bisa mengembalikan
				// null -> jumlah_semester null -> "semester >= jumlah_semester" di bawah meledak saat
				// unboxing. Jaga nilai default 8 tetap berlaku bila hasilnya null.
				if (jumlah_semester == null) {
					jumlah_semester = 8;
				}
				StatusKeluar statusKeluar = null;
				String namaStatusKeluar = null;
				KelompokStatusMahasiswa kelompokStatus = null;
				String batasStudi = null;
				try { statusKeluar = mahasiswa.getStatusKeluar(); } catch (Exception e) { statusKeluar = null; }
				try { namaStatusKeluar = statusKeluar == null ? null : statusKeluar.getNama(); } catch (Exception e) { namaStatusKeluar = null; }
				try { kelompokStatus = mahasiswa.getKelompokStatusMahasiswa(); } catch (Exception e) { kelompokStatus = null; }
				try { batasStudi = mahasiswa.getBatasStudi(); } catch (Exception e) { batasStudi = null; }

				if (semester >= jumlah_semester && namaStatusKeluar != null
						&& namaStatusKeluar.trim().equalsIgnoreCase("Lulus")) {
					statusMahasiswa = ConstantValues.LULUS;
				}

				else if (semester >= jumlah_semester && namaStatusKeluar != null
						&& namaStatusKeluar.trim().toLowerCase().contains("keluar")) {
					statusMahasiswa = ConstantValues.DROP_OUT;
				}

				else if (kelompokStatus != null && kelompokStatus.getSmtMulai() != null
						&& kelompokStatus.getSmtSampai() != null && kelompokStatus.getSmtMulai() <= semester
						&& kelompokStatus.getSmtSampai() >= semester) {
					statusMahasiswa = kelompokStatus.getStatusMahasiswa();
				} else {

					PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = null;
					try { pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahap,
							getSp() != null && getSp().equals(Perkuliahan.SEMESTER_PENDEK)); } catch (Exception e) { pendaftaranCutiMahasiswa = null; }

					if (pendaftaranCutiMahasiswa != null && Boolean.TRUE.equals(pendaftaranCutiMahasiswa.getPersetujuan())
							&& pendaftaranCutiMahasiswa.getSemester() != null
							&& pendaftaranCutiMahasiswa.getSemester().equals(semester)) {
						statusMahasiswa = ConstantValues.CUTI;
					} else {

						if (statusMahasiswa == null) {
							statusMahasiswa = ConstantValues.AKTIF;
						}

						if (((getSks() != null && getSks() > 0)
								|| (getSemester() != null && getSemester().equals(1)))
								&& statusKeluar == null) {
							statusMahasiswa = ConstantValues.AKTIF;
						} else {

							String checkStatusPembayaranMahasiswa = retreive("checkStatusPembayaranMahasiswa");

							boolean ada = false;

							if (semester >= jumlah_semester && namaStatusKeluar != null
									&& namaStatusKeluar.trim().equalsIgnoreCase("Lulus")) {
								statusMahasiswa = ConstantValues.LULUS;
							}

							else if (semester >= jumlah_semester && namaStatusKeluar != null
									&& namaStatusKeluar.trim().toLowerCase().contains("keluar")) {
								statusMahasiswa = ConstantValues.DROP_OUT;
							}

							else if (semester >= jumlah_semester && statusKeluar != null && namaStatusKeluar != null) {
								statusMahasiswa = ConstantValues.KELUAR;
							} else if (batasStudi != null && !batasStudi.isEmpty()) {

								for (String s : batasStudi.split(",")) {
									if (s.trim().equalsIgnoreCase(semester.toString())) {
										ada = true;
									}
								}

								if (ada) {
									statusMahasiswa = ConstantValues.TIDAK_AKTIF;
								}
							} else if (semester != null && checkStatusPembayaranMahasiswa != null
									&& !checkStatusPembayaranMahasiswa.trim().isEmpty()
									&& checkStatusPembayaranMahasiswa.equalsIgnoreCase("false")) {
								statusMahasiswa = ConstantValues.TIDAK_AKTIF;
							} else if (semester != null && semester < jumlah_semester
									&& statusKeluar == null
									&& statusMahasiswa != null && statusMahasiswa.getId() != null
									&& (statusSama(statusMahasiswa, ConstantValues.LULUS)
											|| statusSama(statusMahasiswa, ConstantValues.KELUAR)
											|| statusSama(statusMahasiswa, ConstantValues.DROP_OUT)
											|| statusSama(statusMahasiswa, ConstantValues.CUTI))) {
								statusMahasiswa = ConstantValues.AKTIF;
							}

							if (statusMahasiswa != null && ConstantValues.LULUS != null
									&& statusMahasiswa.getId().equals(ConstantValues.LULUS.getId())
									&& statusKeluar == null) {
								statusMahasiswa = ConstantValues.TIDAK_AKTIF;
							}

							if (!ada && statusMahasiswa != null && ConstantValues.TIDAK_AKTIF != null
									&& ConstantValues.TIDAK_AKTIF.getId().equals(statusMahasiswa.getId())
									&& checkStatusPembayaranMahasiswa != null
									&& !checkStatusPembayaranMahasiswa.trim().isEmpty()
									&& checkStatusPembayaranMahasiswa.equalsIgnoreCase("true")) {
								statusMahasiswa = ConstantValues.AKTIF;
							}

							if (statusMahasiswa != null && ConstantValues.CUTI != null
									&& statusMahasiswa.getId().equals(ConstantValues.CUTI.getId())
									&& (pendaftaranCutiMahasiswa == null
											|| !Boolean.TRUE.equals(pendaftaranCutiMahasiswa.getPersetujuan()))) {
								statusMahasiswa = ConstantValues.AKTIF;
							}

						}
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/HistoryStatusMahasiswa.java:258");
//			e.printStackTrace();
		}

		return statusMahasiswa;
	}

	/**
	 * Pembanding dua {@link StatusMahasiswa} berdasarkan kunci primernya, aman terhadap {@code null}
	 * di keempat titik (objek kiri, id kiri, objek kanan, id kanan).
	 *
	 * <p>Perlu ada karena konstanta {@link ConstantValues} bisa belum ter-seed ({@code null}) pada
	 * fase awal boot aplikasi, sementara {@code statusMahasiswa} bisa berupa proxy yang belum punya
	 * id. Perbandingan berdasarkan id — bukan {@code equals()} objek — juga menghindari kegagalan
	 * saat dua instance mewakili baris yang sama tetapi berasal dari session Hibernate berbeda.</p>
	 *
	 * @param kiri  status pertama
	 * @param kanan status kedua
	 * @return {@code true} hanya bila kedua status ada, keduanya punya id, dan id-nya sama
	 */
	private static boolean statusSama(StatusMahasiswa kiri, StatusMahasiswa kanan) {
		return kiri != null && kiri.getId() != null && kanan != null && kanan.getId() != null
				&& kiri.getId().equals(kanan.getId());
	}

	/**
	 * Status kemahasiswaan pada semester baris ini (kolom {@code status_mahasiswa}, lazy).
	 *
	 * <p><b>Bukan getter pasif.</b> Ini adalah properti yang dibaca Hibernate saat dirty-check/flush,
	 * namun setiap pemanggilan menjalankan urutan berikut dan <b>menimpa field</b>:</p>
	 * <ol>
	 *   <li>{@code check(statusMahasiswa)} — resolusi proxy lazy nilai yang tersimpan;</li>
	 *   <li>{@link #getMahasiswa()} — resolusi proxy mahasiswa;</li>
	 *   <li>{@link #getSemester()} — menghitung nomor semester bila masih kosong;</li>
	 *   <li>{@link #ambilStatusMahasiswa(Integer)} — menjalankan SELURUH mesin aturan status.</li>
	 * </ol>
	 * <p>Artinya nilai yang tersimpan di kolom bisa <b>ditimpa hasil perhitungan ulang</b> begitu
	 * baris ini dibaca di dalam session yang masih terbuka: {@code UPDATE} beserta satu revisi audit
	 * Envers akan ikut terkirim saat flush. Bila hanya ingin melihat nilai kolom apa adanya tanpa
	 * memicu perhitungan, jangan lewat getter ini.</p>
	 *
	 * @return status pada semester ini; dapat {@code null}
	 * @see #ambilStatusMahasiswa(Integer)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_mahasiswa", nullable = true)
	public StatusMahasiswa getStatusMahasiswa() {

		statusMahasiswa = check(statusMahasiswa);

		mahasiswa = getMahasiswa();
		semester = getSemester();

		statusMahasiswa = ambilStatusMahasiswa(semester);

		return statusMahasiswa;
	}

	/**
	 * Mengisi status secara eksplisit. Penugasan langsung tanpa validasi.
	 *
	 * <p>Perlu diingat, nilai yang diset di sini <b>tidak dijamin bertahan</b>: pemanggilan
	 * {@link #getStatusMahasiswa()} berikutnya akan menghitung ulang dan bisa menggantinya.
	 * Untuk memaksa suatu status bertahan, aturan yang bersangkutan harus ikut dipenuhi (mis. lewat
	 * {@link Mahasiswa#getKelompokStatusMahasiswa()} atau {@code paksaAktifSemester}).</p>
	 *
	 * @param statusMahasiswa status yang diinginkan
	 */
	public void setStatusMahasiswa(StatusMahasiswa statusMahasiswa) {
		this.statusMahasiswa = statusMahasiswa;
	}

	/**
	 * Nomor semester mahasiswa yang diwakili baris ini.
	 *
	 * <p><b>Efek samping — perhitungan malas (lazy) satu kali.</b> Bila field {@code semester} masih
	 * {@code null} <i>dan</i> {@code mahasiswa}, {@code ganjilGenap}, serta {@code tahunAkademik}
	 * sudah terisi, nilainya diturunkan dan <b>ditulis balik ke field</b> memakai
	 * {@code Common.getSemester(tahunAngkatan, ganjilGenap, pindahKeKampusIniMasukSemester,
	 * tahun, semesterMulai)} — dengan {@code tahun} = potongan pertama {@code tahunAkademik} sebelum
	 * tanda {@code "/"} (jadi {@code "2025/2026"} &rarr; {@code 2025}).</p>
	 *
	 * <p>Setiap kegagalan (format tahun akademik tidak valid, data angkatan kosong, dsb.) ditelan dan
	 * menghasilkan {@code semester = 0} — bukan {@code null}. Nilai 0 inilah yang membuat baris
	 * "rusak" berperilaku seperti mahasiswa yang belum memulai kuliah, bukan meledak.</p>
	 *
	 * @return nomor semester (mulai 1), {@code 0} bila perhitungan gagal, atau {@code null} bila
	 *         prasyarat perhitungan belum terpenuhi dan kolomnya memang kosong
	 */
	public Integer getSemester() {
		if (semester == null && mahasiswa != null && ganjilGenap != null && !ganjilGenap.trim().isEmpty()
				&& tahunAkademik != null) {
			try {
				ganjilGenap = getGanjilGenap();
				Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
				String ta = tahunAkademik;
				Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
				semester = Common.getSemester(tahunAngkatanMhs, ganjilGenap,
						mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
			} catch (Exception e) {
				semester = 0;
			}
		}
		return semester;
	}

	/**
	 * Mengisi nomor semester secara eksplisit. Mengisi nilai bukan-{@code null} di sini mematikan
	 * perhitungan otomatis di {@link #getSemester()}.
	 *
	 * @param semester nomor semester
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Tahun akademik periode ini, format {@code "2025/2026"}.
	 *
	 * @return tahun akademik, atau {@code null}
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Mengisi tahun akademik periode ini.
	 *
	 * @param tahunAkademik tahun akademik, format {@code "2025/2026"} (dipakai
	 *                      {@link #getSemester()} untuk mengambil tahun acuan)
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Catatan administratif bebas atas status semester ini (mis. alasan cuti/nonaktif). Diisi
	 * operator dari layar Studi Mahasiswa ({@code TampilStudiMahasiswaHelper}) dan
	 * {@code MahasiswaAction}; tidak dipakai mesin aturan status.
	 *
	 * @return keterangan, atau {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi catatan administratif atas status semester ini.
	 *
	 * @param keterangan teks keterangan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Tanggal berlakunya status pada semester ini (kolom {@code DATE}). Murni data administratif —
	 * dibaca laporan EMIS ({@code LaporanFormatEMIS}, {@code LaporanFormatEMISRiwayatMahasiswa})
	 * sebagai tanggal perubahan status, dan tidak ikut memengaruhi perhitungan status.
	 *
	 * @return tanggal berlaku status, atau {@code null} bila tidak diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalStatus() {
		return tanggalStatus;
	}

	/**
	 * Mengisi tanggal berlakunya status.
	 *
	 * @param tanggalStatus tanggal berlaku status
	 */
	public void setTanggalStatus(Date tanggalStatus) {
		this.tanggalStatus = tanggalStatus;
	}

	/**
	 * Nomor tahap periode ini. Relevan hanya bila penomoran tahapan diaktifkan
	 * ({@code ConstantValues.aktifkanTahapan}), yaitu untuk jenjang yang membagi satu semester
	 * menjadi beberapa tahap. Dipakai {@link #ambilStatusMahasiswa(Integer)} saat mencari pengajuan
	 * cuti yang cocok.
	 *
	 * @return nomor tahap, atau {@code null} bila tahapan tidak dipakai
	 */
	public Integer getTahap() {
		return tahap;
	}

	/**
	 * Mengisi nomor tahap periode ini.
	 *
	 * @param tahap nomor tahap
	 */
	public void setTahap(Integer tahap) {
		this.tahap = tahap;
	}

	/**
	 * Jenis semester periode ini: {@link Perkuliahan#GANJIL}, {@link Perkuliahan#GENAP}, atau
	 * {@link Perkuliahan#SP}.
	 *
	 * <p><b>Efek samping:</b> nilainya <b>diturunkan ulang setiap kali dipanggil</b> dan ditulis ke
	 * field, mengabaikan apa pun yang tersimpan di kolom: bila {@code sp} menandai Semester Pendek
	 * maka hasilnya selalu {@code SP}; kalau tidak, ganjil/genap dihitung dari paritas
	 * {@code semester} (genap &rarr; {@link Perkuliahan#GENAP}, ganjil &rarr;
	 * {@link Perkuliahan#GANJIL}). Nilai kolom hanya bertahan bila {@code sp} dan {@code semester}
	 * dua-duanya masih {@code null}.</p>
	 *
	 * <p><b>Kuirk:</b> baris pertama memanggil {@link #getMahasiswa()} (memicu resolusi proxy lazy,
	 * berpotensi membuka session baru) padahal hasilnya <b>tidak dipakai sama sekali</b> di method
	 * ini. Sisa peninggalan versi lama; biaya tersembunyi yang perlu diketahui bila getter ini
	 * dipanggil dalam loop.</p>
	 *
	 * @return jenis semester, atau {@code null} bila belum bisa ditentukan
	 */
	public String getGanjilGenap() {
		mahasiswa = getMahasiswa();
		if (sp != null && sp.equals(Perkuliahan.SEMESTER_PENDEK)) {
			ganjilGenap = Perkuliahan.SP;
		} else if (semester != null) {
			ganjilGenap = semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
		}
		return ganjilGenap;
	}

	/**
	 * Mengisi jenis semester secara eksplisit. Perlu diingat nilainya akan ditimpa lagi oleh
	 * {@link #getGanjilGenap()} bila {@code sp} atau {@code semester} sudah terisi.
	 *
	 * @param ganjilGenap jenis semester
	 */
	public void setGanjilGenap(String ganjilGenap) {
		this.ganjilGenap = ganjilGenap;
	}

	/**
	 * Menentukan <b>status awal</b> (kategori masuk: Baru, Pindahan, Transfer, Lanjutan, dsb.) yang
	 * berlaku bagi seorang mahasiswa pada semester tertentu.
	 *
	 * <p>Fungsi statis — tidak menyentuh state instance sama sekali — supaya bisa dipakai dari luar
	 * tanpa punya baris riwayat: {@link Kegiatan#getStatusAwalMahasiswa()}, {@code SkripsiAction},
	 * {@code MahasiswaRequestTugasAkhirAction}, dan {@code MahasiswaAction} (untuk memeriksa apakah
	 * status awal yang tersimpan di baris riwayat sudah sesuai aturan).</p>
	 *
	 * <p><b>Urutan penentuan</b> (yang belakangan dapat menimpa yang sebelumnya):</p>
	 * <ol>
	 *   <li><b>Override rentang semester</b> dari {@link Mahasiswa#getKelompokMahasiswa()}: hingga
	 *       TIGA pasang aturan ({@code statusAwalMahasiswa}/{@code smtMulai}..{@code smtSampai},
	 *       lalu varian {@code 2}, lalu varian {@code 3}) dicoba berurutan; yang pertama mencakup
	 *       {@code semester} dipakai dan menandai {@code ada = true}.</li>
	 *   <li>Bila tak ada override yang cocok: ambil status awal utama
	 *       {@link Mahasiswa#getStatusAwalMahasiswa()} — ini juga dipakai secara PAKSA (menimpa
	 *       argumen) bila {@code Mahasiswa.getStatusAwalSelaluIkutDataUtama()} bernilai {@code true}.</li>
	 *   <li>Sebaliknya, bila flag "selalu ikut data utama" mati: baca status awal per-semester dari
	 *       penyimpanan kunci-nilai {@link Mahasiswa} dengan kunci {@code "sts_<semester>"} (ditulis
	 *       oleh {@link #setStatusAwalMahasiswa(StatusAwalMahasiswa)}), lalu resolusi id-nya lewat
	 *       {@link ConstantValues#ambil(String, java.io.Serializable)}.</li>
	 *   <li>Dua aturan "berubah mulai semester tertentu" yang dipasang admin di data mahasiswa:
	 *       {@code smtStatusAwal} + {@code statusAwalMahasiswaSetelahSmtTertentu}, lalu
	 *       {@code smtStatusAwalLagi} + {@code statusAwalMahasiswaSetelahSmtTertentuLagi}. Keduanya
	 *       berlaku untuk SEMUA semester &ge; ambangnya, dan yang kedua menimpa yang pertama.</li>
	 * </ol>
	 *
	 * <p><b>Perilaku pass-through.</b> Argumen {@code statusAwalMahasiswa} berfungsi sebagai nilai
	 * awal sekaligus nilai kembalian default: bila mahasiswa {@code null} (lihat komentar FIX NPE)
	 * atau tak satu pun aturan cocok, argumen dikembalikan apa adanya. Pemanggil karena itu boleh
	 * mengirim {@code null} bila hanya ingin tahu "seharusnya apa".</p>
	 *
	 * @param mahasiswa           mahasiswa yang dinilai; {@code null} menyebabkan argumen ketiga
	 *                            dikembalikan langsung
	 * @param semester            semester yang dinilai; {@code null} melewatkan semua aturan
	 *                            berbasis rentang semester
	 * @param statusAwalMahasiswa nilai awal / nilai balik default
	 * @return status awal yang berlaku pada semester tersebut, bisa {@code null}
	 */
	public static StatusAwalMahasiswa ambilStatusAwal(Mahasiswa mahasiswa, Integer semester,
			StatusAwalMahasiswa statusAwalMahasiswa) {
		// FIX NPE: dipanggil dari getter getStatusAwalMahasiswa() yg bisa terpicu via reflection
		// (BeanUtils.copyProperties saat sinkronisasi cache JSON) SEBELUM field mahasiswa terisi.
		if (mahasiswa == null) {
			return statusAwalMahasiswa;
		}
		boolean ada = false;
		KelompokMahasiswa kelompokMahasiswa = mahasiswa.getKelompokMahasiswa();
		if (kelompokMahasiswa != null && semester != null) {
			if (kelompokMahasiswa.getStatusAwalMahasiswa() != null
					&& kelompokMahasiswa.getSmtMulai() != null && kelompokMahasiswa.getSmtSampai() != null
					&& kelompokMahasiswa.getSmtMulai() <= semester
					&& kelompokMahasiswa.getSmtSampai() >= semester) {
				statusAwalMahasiswa = kelompokMahasiswa.getStatusAwalMahasiswa();
				ada = true;
			} else if (kelompokMahasiswa.getStatusAwalMahasiswa2() != null
					&& kelompokMahasiswa.getSmtMulai2() != null && kelompokMahasiswa.getSmtSampai2() != null
					&& kelompokMahasiswa.getSmtMulai2() <= semester && kelompokMahasiswa.getSmtSampai2() >= semester) {
				statusAwalMahasiswa = kelompokMahasiswa.getStatusAwalMahasiswa2();
				ada = true;
			} else if (kelompokMahasiswa.getStatusAwalMahasiswa3() != null
					&& kelompokMahasiswa.getSmtMulai3() != null && kelompokMahasiswa.getSmtSampai3() != null
					&& kelompokMahasiswa.getSmtMulai3() <= semester && kelompokMahasiswa.getSmtSampai3() >= semester) {
				statusAwalMahasiswa = kelompokMahasiswa.getStatusAwalMahasiswa3();
				ada = true;
			}
		}

		if (!ada || statusAwalMahasiswa == null) {

			if ((statusAwalMahasiswa == null || mahasiswa.getStatusAwalSelaluIkutDataUtama()) && mahasiswa != null
					&& mahasiswa.getStatusAwalMahasiswa() != null) {
				statusAwalMahasiswa = mahasiswa.getStatusAwalMahasiswa();
			}

			else if (!mahasiswa.getStatusAwalSelaluIkutDataUtama() && mahasiswa != null && semester != null) {
				String s = mahasiswa.retreive("sts_" + semester);
				if (s != null && !s.isEmpty() && Common.isNumber(s)) {
					statusAwalMahasiswa = (StatusAwalMahasiswa) ConstantValues
							.ambil(StatusAwalMahasiswa.class.getName(), Long.parseLong(s.trim()));
				}
			}

			if (semester != null && mahasiswa != null && mahasiswa.getSmtStatusAwal() != null
					&& mahasiswa.getSmtStatusAwal() <= semester
					&& mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentu() != null) {
				statusAwalMahasiswa = mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentu();
			}

			if (semester != null && mahasiswa != null && mahasiswa.getSmtStatusAwalLagi() != null
					&& mahasiswa.getSmtStatusAwalLagi() <= semester
					&& mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentuLagi() != null) {
				statusAwalMahasiswa = mahasiswa.getStatusAwalMahasiswaSetelahSmtTertentuLagi();
			}
		}

		return statusAwalMahasiswa;
	}

	/**
	 * Menentukan <b>program/kelas</b> (Reguler, Karyawan, Kelas Malam, dsb.) yang berlaku bagi
	 * seorang mahasiswa pada semester tertentu, berdasarkan {@link Mahasiswa#getProgramMahasiswa()}.
	 *
	 * <p>Sama seperti {@link #ambilStatusAwal}, ini fungsi statis tanpa state supaya bisa dipanggil
	 * dari luar tanpa baris riwayat: {@link Kegiatan}, {@code SkripsiAction},
	 * {@code MahasiswaRequestTugasAkhirAction}.</p>
	 *
	 * <p>{@link ProgramMahasiswa} menyimpan hingga TIGA aturan berbasis rentang semester
	 * ({@code program}/{@code smtMulai}..{@code smtSampai}, lalu varian {@code 2} dan {@code 3});
	 * yang pertama mencakup {@code semester} dipakai dan sisanya tidak dievaluasi. Berbeda dari
	 * {@code ambilStatusAwal}, di sini <b>tidak ada</b> mekanisme cadangan ke data utama — bila tak
	 * ada rentang yang cocok, argumen {@code program} dikembalikan apa adanya. Pengambilan nilai
	 * cadangan itu dilakukan pemanggilnya, lihat {@link #getProgram()}.</p>
	 *
	 * @param mahasiswa mahasiswa yang dinilai; {@code null} menyebabkan {@code program} dikembalikan langsung
	 * @param semester  semester yang dinilai; {@code null} menyebabkan {@code program} dikembalikan langsung
	 * @param program   nilai awal / nilai balik default
	 * @return nama program yang berlaku pada semester tersebut
	 */
	public static String ambilProgram(Mahasiswa mahasiswa, Integer semester, String program) {

		if (mahasiswa == null || semester == null) {
			return program;
		}

		ProgramMahasiswa programMahasiswa = mahasiswa.getProgramMahasiswa();
		if (programMahasiswa != null) {
			if (programMahasiswa.getProgram() != null && programMahasiswa.getSmtMulai() != null
					&& programMahasiswa.getSmtSampai() != null && programMahasiswa.getSmtMulai() <= semester
					&& programMahasiswa.getSmtSampai() >= semester) {
				program = programMahasiswa.getProgram();
			} else if (programMahasiswa.getProgram2() != null && programMahasiswa.getSmtMulai2() != null
					&& programMahasiswa.getSmtSampai2() != null && programMahasiswa.getSmtMulai2() <= semester
					&& programMahasiswa.getSmtSampai2() >= semester) {
				program = programMahasiswa.getProgram2();
			} else if (programMahasiswa.getProgram3() != null && programMahasiswa.getSmtMulai3() != null
					&& programMahasiswa.getSmtSampai3() != null && programMahasiswa.getSmtMulai3() <= semester
					&& programMahasiswa.getSmtSampai3() >= semester) {
				program = programMahasiswa.getProgram3();

			}
		}

		return program;
	}

	/**
	 * Status awal/kategori masuk yang berlaku pada semester baris ini (kolom
	 * {@code status_awal_mahasiswa}, lazy).
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getMahasiswa()} (resolusi proxy), meresolusi proxy
	 * nilai tersimpan, lalu <b>menghitung ulang</b> lewat {@link #ambilStatusAwal} dan menimpa field.
	 * Sama seperti {@link #getStatusMahasiswa()}, nilai kolom bisa berubah hanya karena dibaca.</p>
	 *
	 * <p><b>Kuirk:</b> yang dikirim ke {@link #ambilStatusAwal} adalah <b>field</b> {@code semester},
	 * bukan {@link #getSemester()}. Jadi bila {@code semester} masih {@code null} (belum sempat
	 * dihitung lazy), seluruh aturan berbasis rentang semester dilewati dan hasilnya jatuh ke status
	 * awal utama mahasiswa. Ini berbeda perilaku dengan {@link #getStatusMahasiswa()} yang memaksa
	 * {@code getSemester()} lebih dulu.</p>
	 *
	 * @return status awal yang berlaku pada semester ini, bisa {@code null}
	 * @see #ambilStatusAwal(Mahasiswa, Integer, StatusAwalMahasiswa)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswa() {
		mahasiswa = getMahasiswa();
		statusAwalMahasiswa = check(statusAwalMahasiswa);
		statusAwalMahasiswa = HistoryStatusMahasiswa.ambilStatusAwal(mahasiswa, semester, statusAwalMahasiswa);
		return statusAwalMahasiswa;
	}

	/**
	 * Mengisi status awal untuk semester baris ini.
	 *
	 * <p><b>Efek samping penting — setter ini menulis ke luar dirinya sendiri.</b> Bila
	 * {@code mahasiswa}, status baru (beserta id-nya), dan {@code semester} semuanya terisi, id
	 * status awal ikut dicatat ke penyimpanan kunci-nilai milik {@link Mahasiswa} dengan kunci
	 * {@code "sts_<semester>"} lewat {@code Mahasiswa.put(nilai, kunci)}. Itulah sumber data yang
	 * dibaca kembali oleh {@link #ambilStatusAwal} pada cabang "tidak selalu ikut data utama",
	 * sehingga status awal per-semester tetap diketahui walau baris riwayatnya sendiri belum
	 * tersimpan. Perhatikan urutan argumen {@code put}: <b>nilai dulu, baru kunci</b>.</p>
	 *
	 * <p>Karena {@code GeneralValueObject.put(...)} dapat memicu operasi tulis berkas/cache, setter
	 * ini tidak sepenuhnya murah — hindari memanggilnya dalam loop besar.</p>
	 *
	 * @param statusAwalMahasiswa status awal yang berlaku pada semester ini
	 */
	public void setStatusAwalMahasiswa(StatusAwalMahasiswa statusAwalMahasiswa) {
		if (mahasiswa != null && statusAwalMahasiswa != null && statusAwalMahasiswa.getId() != null
				&& semester != null) {
			mahasiswa.put(statusAwalMahasiswa.getId().toString(), "sts_" + semester);
		}
		this.statusAwalMahasiswa = statusAwalMahasiswa;
	}

	/**
	 * Jumlah SKS yang diambil mahasiswa pada semester ini. Dipakai
	 * {@link #ambilStatusMahasiswa(Integer)} sebagai bukti keaktifan: SKS &gt; 0 memaksa status
	 * AKTIF selama mahasiswa belum punya status keluar.
	 *
	 * <p><b>Menormalkan {@code null} menjadi {@code 0}</b> — jadi getter ini tidak pernah
	 * mengembalikan {@code null}, dan pemanggil tidak bisa membedakan "belum diisi" dari
	 * "benar-benar nol SKS". Karena Hibernate memakai property access, normalisasi ini juga berarti
	 * baris yang kolomnya {@code NULL} akan tertulis ulang menjadi {@code 0} saat flush.</p>
	 *
	 * @return jumlah SKS, {@code 0} bila belum diisi; tidak pernah {@code null}
	 */
	public Integer getSks() {
		return sks == null ? 0 : sks;
	}

	/**
	 * Mengisi jumlah SKS yang diambil pada semester ini.
	 *
	 * @param sks jumlah SKS
	 */
	public void setSks(Integer sks) {
		this.sks = sks;
	}

	/**
	 * Mengisi program/kelas periode ini secara eksplisit. Nilainya dapat ditimpa lagi oleh
	 * {@link #getProgram()}.
	 *
	 * @param program nama program/kelas
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Program/kelas (Reguler, Karyawan, dsb.) yang berlaku pada semester baris ini (kolom
	 * {@code program}, panjang 50).
	 *
	 * <p><b>Efek samping &amp; urutan penentuan:</b></p>
	 * <ol>
	 *   <li>Bila {@code Mahasiswa.getProgramSelaluIkutDataUtama()} bernilai {@code true}, program
	 *       data utama mahasiswa dikembalikan LANGSUNG — field {@code program} milik baris ini
	 *       <b>tidak diperbarui</b>, sehingga nilai yang tampil bisa berbeda dari yang tersimpan
	 *       di kolom. (Getter ini juga memanggil {@code getMahasiswa()} tiga kali di jalur tersebut,
	 *       masing-masing memicu {@code check()}.)</li>
	 *   <li>Selain itu: bila {@code program} masih kosong, disalin dari
	 *       {@link Mahasiswa#getProgram()}.</li>
	 *   <li>Lalu {@link #ambilProgram(Mahasiswa, Integer, String)} diberi kesempatan menimpanya
	 *       dengan aturan rentang semester {@link ProgramMahasiswa}. Hasilnya <b>ditulis balik ke
	 *       field</b>, jadi bisa ikut ter-{@code UPDATE} saat flush.</li>
	 * </ol>
	 *
	 * <p><b>Kuirk:</b> langkah 2 dan 3 memakai <b>field</b> {@code mahasiswa} dan {@code semester}
	 * mentah, bukan getter-nya. Bila {@code mahasiswa} belum diresolusi pada langkah 1 (mis. jalur
	 * "selalu ikut data utama" tidak diambil karena {@code getMahasiswa()} mengembalikan
	 * {@code null}), kedua langkah itu tidak melakukan apa-apa.</p>
	 *
	 * @return nama program yang berlaku pada semester ini, bisa {@code null}
	 */
	@Column(name = "program", length = 50)
	public String getProgram() {

		if (getMahasiswa() != null && getMahasiswa().getProgramSelaluIkutDataUtama()) {
			return getMahasiswa().getProgram();
		}

		if ((program == null || program.trim().isEmpty()) && mahasiswa != null) {
			program = mahasiswa.getProgram();
		}

		program = HistoryStatusMahasiswa.ambilProgram(mahasiswa, semester, program);

		return program;
	}

	/**
	 * Penanda Semester Pendek. Bernilai {@link Perkuliahan#SEMESTER_PENDEK} bila baris ini mewakili
	 * status SP, {@code null} (atau nilai lain) untuk semester reguler.
	 *
	 * <p>Kolom inilah yang memisahkan riwayat SP dari riwayat reguler pada semester yang sama —
	 * mahasiswa boleh Nonaktif di semester regulernya namun tetap Aktif di SP-nya. Dipakai
	 * {@link #getGanjilGenap()} dan {@link #ambilStatusMahasiswa(Integer)} (saat mencocokkan
	 * pengajuan cuti SP).</p>
	 *
	 * @return penanda SP, atau {@code null} untuk baris reguler
	 */
	public Integer getSp() {
		return sp;
	}

	/**
	 * Mengisi penanda Semester Pendek.
	 *
	 * @param sp {@link Perkuliahan#SEMESTER_PENDEK} untuk baris SP, {@code null} untuk reguler
	 */
	public void setSp(Integer sp) {
		this.sp = sp;
	}
}
