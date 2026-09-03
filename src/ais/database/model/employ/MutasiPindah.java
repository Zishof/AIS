package ais.database.model.employ;

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

/**
 * Model data untuk mutasi pindah. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code Date tanggal_dirubah}, {@code String DISETUJUI}, {@code
 * String BELUM_DIPROSES}, {@code Pegawai pegawai}; pemetaan persistence: tabel {@code employ.mutasi_pindah};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()},
 * {@code getKeterangan()}, {@code getPegawai()}); mutasi data ({@code setOlehId()}, {@code onUpdate()}, {@code
 * setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code setKeterangan()}); operasi domain lain
 * ({@code toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * <h2>Isi berkas: perpindahan unit kerja</h2>
 * <p>Satu baris mencatat kepindahan seorang pegawai dari satu {@link UnitKerja} ke unit kerja lain,
 * lengkap dengan jabatan struktural sebelum dan sesudah perpindahan, nomor serta tanggal surat usul,
 * dan tanggal mulai berlakunya (TMT). Pengelolaannya ada pada layar mutasi
 * ({@code ais.action.master.employ.MutasiAction}) beserta halaman ZUL-nya.</p>
 *
 * <h2>Warisan struktur dari {@link Pensiun}</h2>
 * <p>Entity ini jelas dibentuk dengan menyalin {@link Pensiun}, dan sebagian sisa salinan itu masih
 * tertinggal: {@link #getJenisPensiun()}, {@link #getGolonganTerakhir()}, dan
 * {@link #getTanggalPensiun()} tidak ada kaitannya dengan perpindahan unit kerja. Ketiganya
 * dipetakan {@code nullable = true} di sini (di {@link Pensiun} dua yang pertama wajib), dan layar
 * mutasi tidak pernah mengisinya. Perlakukan ketiganya sebagai kolom mati: jangan dipakai untuk
 * logika baru, dan jangan diandalkan berisi apa pun pada data yang ada.</p>
 *
 * <h2>Tidak ada gerbang persetujuan</h2>
 * <p>Konstanta {@link #DISETUJUI} dan {@link #BELUM_DIPROSES} beserta field {@link #getStatus()}
 * mengesankan adanya alur persetujuan mutasi, tetapi <b>alur itu tidak pernah dibangun</b>. Layar
 * mutasi menyimpan pegawai, nomor dan tanggal surat usul, TMT, unit kerja awal/tujuan, serta jabatan
 * struktural awal/akhir — dan tidak pernah menyentuh {@code status}. Karena tidak ada titik
 * persetujuan, tidak ada pula pemeriksaan hak atau kepemilikan yang dapat ditinjau. Berkas mutasi
 * dengan demikian hanya berfungsi sebagai catatan administratif; ia tidak menggerakkan perubahan
 * apa pun pada data pegawai secara otomatis.</p>
 *
 * @see GeneralValueObject
 * @see UnitKerja
 * @see Pensiun
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "mutasi_pindah")

public class MutasiPindah extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai ini dipakai bersama oleh banyak entity paket
	 * {@code employ} karena berkas-berkasnya disalin dari template yang sama; angkanya tidak memiliki
	 * makna bisnis dan tidak boleh diubah tanpa alasan, sebab perubahan akan mematahkan deserialisasi
	 * state ZK/HTTP session dari versi aplikasi sebelumnya.
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	/** Kunci utama baris {@code employ.mutasi_pindah}; diisi database (IDENTITY). */
	private Long id;
	/** Nama/identitas petugas terakhir yang menyimpan berkas ini -- jejak audit tampilan. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan berkas ini -- jejak audit yang dapat ditelusuri balik. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir menyimpan berkas mutasi ini, terpisah dari {@link #getOleh()} yang
	 * menyimpan nama tampilan. Dapat {@code null} untuk baris warisan maupun baris yang disimpan
	 * proses batch tanpa konteks pengguna.
	 *
	 * @return id pengguna penyimpan terakhir, atau {@code null} bila tidak tercatat
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()}. Mengabaikan (tidak menimpa nilai lama) bila masukan
	 * {@code null}/kosong-setelah-trim -- pola pengaman umum di entity domain kepegawaian agar jejak
	 * audit "olehId" tidak pernah ditimpa kosong. Setter ini karena itu bersifat satu arah: nilai
	 * yang sudah terisi tidak dapat dikosongkan kembali lewat jalur ini.
	 *
	 * @param olehId id pengguna penyimpan; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Keterangan bebas untuk berkas mutasi ini; juga menjadi label {@link #toString()}. */
	private String keterangan;
	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum tiap {@code UPDATE}
	 * baris ini, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * yang menyetel {@link #tanggal_dirubah} ke waktu saat itu. Deklarasi one-liner mengikuti gaya
	 * berkas hbm2java asli (tidak dirapikan agar diff minimal terhadap riwayat SVN).
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Kunci utama baris {@code employ.mutasi_pindah}.
	 *
	 * <p>Dihasilkan database dengan strategi {@code IDENTITY} dan dipetakan
	 * {@code insertable = false}, sehingga nilai yang diisi manual pada objek baru <b>diabaikan</b>
	 * saat {@code INSERT}. Objek yang belum tersimpan mengembalikan {@code null}.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter {@link #getId()}. Umumnya hanya dipanggil Hibernate saat memuat/menyimpan baris, atau
	 * oleh kode yang sengaja membentuk referensi ringan ke baris yang sudah ada.
	 *
	 * @param id nilai kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Setter {@link #getOleh()} -- pola pengaman sama dengan {@link #setOlehId(String)}: masukan
	 * {@code null}/kosong-setelah-trim diabaikan sehingga nama penyimpan lama tidak tertimpa kosong.
	 *
	 * @param oleh nama penyimpan; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama/identitas petugas yang terakhir menyimpan berkas mutasi ini -- jejak audit untuk
	 * ditampilkan di layar. Untuk penelusuran teknis gunakan {@link #getOlehId()}.
	 *
	 * @return nama penyimpan terakhir, atau {@code null} bila tidak tercatat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Setter {@link #getTanggal_dirubah()}. Normalnya <b>tidak perlu dipanggil manual</b> karena
	 * {@link #onUpdate()} sudah menyetelnya otomatis sebelum tiap {@code UPDATE}. Pemanggilan manual
	 * hanya masuk akal pada migrasi/impor data yang ingin mempertahankan cap waktu asli.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu perubahan terakhir baris ini. Diinisialisasi ke waktu pembuatan objek melalui
	 * {@code WaktuUtil.getDate()} (bukan {@code new Date()}, agar mengikuti sumber waktu tunggal
	 * aplikasi) dan diperbarui otomatis oleh {@link #onUpdate()}.
	 *
	 * @return cap waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibentuk di JVM
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Label teks entity ini, yaitu isi {@link #getKeterangan() keterangan}. Karena kolom tersebut
	 * boleh {@code null}, komponen ZK yang menampilkan objek ini apa adanya dapat memperlihatkan
	 * baris kosong. Pola yang sama dipakai {@link Pensiun}.
	 *
	 * @return keterangan berkas ini, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Keterangan bebas mengenai berkas mutasi ini, misalnya alasan atau catatan perpindahan. Boleh
	 * {@code null}. Perhatikan bahwa nilai ini juga dipakai sebagai hasil {@link #toString()}.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Setter {@link #getKeterangan()}. Menerima {@code null} apa adanya sehingga keterangan dapat
	 * dikosongkan kembali.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nilai {@link #getStatus()} yang menandakan usulan mutasi sudah disetujui.
	 *
	 * <p><b>Konstanta ini tidak pernah dipakai.</b> Penelusuran seluruh kode sumber tidak menemukan
	 * satu pun pembacaan maupun penulisan {@code MutasiPindah.DISETUJUI}, dan tidak ada kode yang
	 * memanggil {@code setStatus} pada entity ini. Disalin apa adanya dari {@link Pensiun}.</p>
	 */
	public static final String DISETUJUI = "DISETUJUI";
	/**
	 * Nilai {@link #getStatus()} yang menandakan usulan mutasi belum diproses. Sama seperti
	 * {@link #DISETUJUI}, konstanta ini tidak pernah dirujuk kode mana pun.
	 */
	public static final String BELUM_DIPROSES = "BELUM DIPROSES";

	/** Pegawai yang dimutasikan; relasi wajib -- lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Nomor surat usul mutasi. */
	private String noSuratUsul;
	/** Tanggal surat usul mutasi. */
	private Date tanggalSuratUsul;
	/** Sisa salinan dari {@link Pensiun}; tidak dipakai alur mutasi. */
	private JenisPensiun jenisPensiun;
	/** Sisa salinan dari {@link Pensiun}; tidak dipakai alur mutasi. */
	private Golongan golonganTerakhir;
	/** Sisa salinan dari {@link Pensiun}; tidak dipakai alur mutasi. */
	private Date tanggalPensiun;
	/** Terhitung mulai tanggal berlakunya mutasi. */
	private Date tmt;
	/** Status berkas dalam bentuk teks bebas; tidak pernah ditulis kode mana pun. */
	private String status;
	/** Unit kerja asal sebelum perpindahan. */
	private UnitKerja unitKerjaAwal;
	/** Unit kerja tujuan setelah perpindahan. */
	private UnitKerja unitKerjaTujuan;
	/** Jabatan struktural yang dipangku sebelum perpindahan. */
	private JabatanStruktural jabatanStrukturalAwal;
	/** Jabatan struktural yang dipangku setelah perpindahan. */
	private JabatanStruktural jabatanStrukturalAkhir;

	/**
	 * Pegawai yang dimutasikan. <b>Getter ini bukan pembaca murni</b> — ia mengubah state objek, dan
	 * perilakunya perlu dipahami sebelum dipakai.
	 *
	 * <p>Dua hal terjadi sebelum nilai dikembalikan. Pertama, referensi dilewatkan
	 * {@code GeneralValueObject.check(..)} yang meresolusi proxy lazy: bila objek yang dipegang masih
	 * berupa proxy yang belum ter-inisialisasi dan session pembuatnya sudah tertutup, helper tersebut
	 * berusaha menggantinya dengan objek nyata — dari peta identitas entity, dari cache, atau dengan
	 * membuka session baru dan memuat ulang berdasarkan id. Hasil resolusi <b>ditulis balik</b> ke
	 * field, sehingga pemanggilan berikutnya menerima objek yang sama. Tujuannya menghindari
	 * kegagalan pemuatan lazy pada objek yang sudah lepas dari session; harganya adalah getter dengan
	 * efek samping, yang karenanya tidak aman dipanggil dari banyak thread sekaligus atas satu
	 * instance.</p>
	 *
	 * <p>Kedua — dan ini yang jauh lebih berbahaya — bila setelah resolusi nilainya <b>tetap
	 * {@code null}</b>, getter mengisi field dengan pegawai milik <b>pengguna yang sedang login</b>.
	 * Pengganti ini bukan sekadar nilai kembalian sementara: karena entity dipetakan lewat akses
	 * properti (anotasi menempel pada getter), Hibernate memanggil getter yang sama ketika memeriksa
	 * perubahan sebelum menulis. Berkas mutasi yang kehilangan referensi pegawainya karena itu dapat
	 * <b>tersimpan atas nama pembacanya</b>, bukan atas nama pegawai yang sesungguhnya dimutasikan.
	 * Kolomnya {@code nullable = false}, sehingga substitusi ini sekaligus menyamarkan data yang
	 * seharusnya ditolak database. Untuk memeriksa apakah sebuah berkas benar-benar punya pemilik,
	 * jangan bersandar pada getter ini.</p>
	 *
	 * <p>Kegagalan mengambil pengguna aktif — misalnya karena getter dipanggil dari utas latar tanpa
	 * konteks sesi — ditangkap dan dicatat ke audit error, lalu field dibiarkan {@code null}.
	 * Perilaku getter ini dengan demikian berbeda antara konteks web dan konteks batch.</p>
	 *
	 * <p>Relasinya {@code @ManyToOne} dengan pemuatan lazy dan cascade {@code PERSIST}/{@code MERGE},
	 * jadi menyimpan berkas ini ikut menyimpan perubahan pada objek pegawai yang tertaut.</p>
	 *
	 * @return pegawai yang dimutasikan, hasil substitusi pengguna aktif bila referensi aslinya
	 *         kosong, atau {@code null} bila substitusi pun tidak memungkinkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);

		try {
			if (pegawai == null) {
				pegawai = Common.getCurrentUser().getPegawai();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/MutasiPindah.java:123");

		}

		return pegawai;
	}

	/**
	 * Setter {@link #getPegawai()}. Menyimpan referensi apa adanya, termasuk {@code null} -- namun
	 * perlu diingat bahwa menyetel {@code null} tidak benar-benar mengosongkan relasi, karena
	 * getter-nya akan menggantinya dengan pegawai pengguna aktif pada pembacaan berikutnya.
	 *
	 * @param pegawai pegawai yang dimutasikan
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Nomor surat usul mutasi, yaitu nomor surat yang mengajukan perpindahan pegawai ini. Teks bebas
	 * tanpa format yang ditegakkan; boleh {@code null}.
	 *
	 * @return nomor surat usul, atau {@code null} bila belum diisi
	 */
	@Column(name = "no_surat_usul")
	public String getNoSuratUsul() {
		return noSuratUsul;
	}

	/**
	 * Setter {@link #getNoSuratUsul()}.
	 *
	 * @param noSuratUsul nomor surat usul; boleh {@code null}
	 */
	public void setNoSuratUsul(String noSuratUsul) {
		this.noSuratUsul = noSuratUsul;
	}

	/**
	 * Tanggal surat usul mutasi. Tanpa anotasi {@code @Temporal}, sehingga dipetakan mengikuti
	 * default penyedia persistence untuk {@link Date} (cap waktu lengkap, bukan tanggal saja).
	 *
	 * @return tanggal surat usul, atau {@code null} bila belum diisi
	 */
	@Column(name = "tanggal_surat_usul")
	public Date getTanggalSuratUsul() {
		return tanggalSuratUsul;
	}

	/**
	 * Setter {@link #getTanggalSuratUsul()}.
	 *
	 * @param tanggalSuratUsul tanggal surat usul; boleh {@code null}
	 */
	public void setTanggalSuratUsul(Date tanggalSuratUsul) {
		this.tanggalSuratUsul = tanggalSuratUsul;
	}

	/**
	 * <b>Kolom mati.</b> Sisa penyalinan struktur {@link Pensiun}: jenis pensiun tidak punya makna
	 * pada berkas perpindahan unit kerja. Berbeda dengan {@link Pensiun#getJenisPensiun()} yang
	 * wajib diisi, di sini kolomnya {@code nullable = true} dan layar mutasi tidak pernah
	 * mengisinya, sehingga nilainya {@code null} pada seluruh data yang wajar.
	 *
	 * <p>Jangan memakai field ini untuk logika baru. Bila kelak dibersihkan, penghapusannya perlu
	 * disertai penyesuaian tabel audit Envers karena entity ini {@code @Audited}.</p>
	 *
	 * @return selalu {@code null} dalam pemakaian normal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenisPensiun", nullable = true)
	public JenisPensiun getJenisPensiun() {
		return jenisPensiun;
	}

	/**
	 * Setter {@link #getJenisPensiun()} -- kolom mati, tidak dipakai alur mutasi.
	 *
	 * @param jenisPensiun jenis pensiun; tidak bermakna di sini
	 */
	public void setJenisPensiun(JenisPensiun jenisPensiun) {
		this.jenisPensiun = jenisPensiun;
	}

	/**
	 * <b>Kolom mati.</b> Sisa penyalinan struktur {@link Pensiun}. Dipetakan {@code nullable = true}
	 * dan tidak pernah diisi layar mutasi.
	 *
	 * <p>Perhatikan bahwa perpindahan unit kerja memang tidak mengubah golongan pegawai — perubahan
	 * golongan ditangani {@link KenaikanPangkat}, bukan berkas ini. Jangan memakai field ini untuk
	 * logika baru.</p>
	 *
	 * @return selalu {@code null} dalam pemakaian normal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "golongan_terakhir", nullable = true)
	public Golongan getGolonganTerakhir() {
		return golonganTerakhir;
	}

	/**
	 * Setter {@link #getGolonganTerakhir()} -- kolom mati, tidak dipakai alur mutasi.
	 *
	 * @param golonganTerakhir golongan terakhir; tidak bermakna di sini
	 */
	public void setGolonganTerakhir(Golongan golonganTerakhir) {
		this.golonganTerakhir = golonganTerakhir;
	}

	/**
	 * <b>Kolom mati.</b> Sisa penyalinan struktur {@link Pensiun}; tanggal pensiun tidak bermakna
	 * pada berkas perpindahan unit kerja dan tidak pernah diisi layar mutasi.
	 *
	 * <p>Nama kolomnya {@code tanggal_pesiun} — dengan huruf "n" yang hilang, salah ketik yang
	 * terbawa persis dari {@link Pensiun#getTanggalPensiun()}. Query SQL langsung yang menyentuh
	 * kolom ini harus memakai ejaan keliru tersebut.</p>
	 *
	 * @return selalu {@code null} dalam pemakaian normal
	 */
	@Column(name = "tanggal_pesiun")
	public Date getTanggalPensiun() {
		return tanggalPensiun;
	}

	/**
	 * Setter {@link #getTanggalPensiun()} -- kolom mati, tidak dipakai alur mutasi.
	 *
	 * @param tanggalPensiun tanggal pensiun; tidak bermakna di sini
	 */
	public void setTanggalPensiun(Date tanggalPensiun) {
		this.tanggalPensiun = tanggalPensiun;
	}

	/**
	 * Status pemrosesan berkas mutasi, dirancang untuk diisi salah satu dari {@link #DISETUJUI} atau
	 * {@link #BELUM_DIPROSES}. <b>Tidak ada kode yang menulis field ini</b>: layar mutasi menyimpan
	 * seluruh field lain tanpa pernah menyentuh {@code status}, sehingga nilainya {@code null} pada
	 * seluruh baris yang dibuat aplikasi.
	 *
	 * <p><b>Ketidakcocokan tipe pada penyaring pencarian.</b> Layar mutasi menyediakan combo
	 * penyaring status berisi dua pilihan, "Disetujui" dan "Belum Disetujui", yang nilainya berupa
	 * {@code Boolean} {@code true}/{@code false}. Nilai tersebut diteruskan ke kriteria pencarian
	 * sebagai pembanding untuk properti {@code status} yang bertipe {@link String}. Perbandingan
	 * {@code Boolean} terhadap kolom teks itu tidak akan pernah cocok dengan {@code "DISETUJUI"}
	 * maupun {@code "BELUM DIPROSES"}; bergantung pada dialek database, penyaring tersebut
	 * menghasilkan nol baris atau menggagalkan query. Cacat ini tidak terlihat selama kolomnya
	 * memang selalu {@code null}, tetapi akan langsung terasa bila alur persetujuan kelak
	 * dihidupkan — perbaiki penyaringnya bersamaan.</p>
	 *
	 * @return status berkas, atau {@code null} -- yang merupakan keadaan normal saat ini
	 */
	@Column(name = "status")
	public String getStatus() {
		return status;
	}

	/**
	 * Setter {@link #getStatus()}. Menerima teks apa adanya tanpa memeriksa bahwa nilainya termasuk
	 * salah satu konstanta yang dikenal, dan tanpa pemeriksaan hak akses apa pun -- gerbang
	 * persetujuan, bila kelak dibutuhkan, harus dibangun di lapisan Action/service.
	 *
	 * @param status status berkas; boleh {@code null}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Unit kerja asal, yaitu tempat pegawai bertugas <b>sebelum</b> perpindahan. Disimpan sebagai
	 * salinan referensi pada berkas sehingga riwayat perpindahan tetap terbaca meski penempatan
	 * pegawai berubah lagi kemudian.
	 *
	 * <p>Kolomnya boleh {@code null} (tidak ada {@code nullable = false}), jadi berkas dapat tersimpan
	 * tanpa unit kerja asal -- kelengkapannya bergantung pada validasi di layar mutasi. Relasi
	 * memakai {@code FetchMode.SELECT} sehingga diambil lewat query terpisah, dan getter ini pembaca
	 * murni tanpa resolusi lazy maupun substitusi.</p>
	 *
	 * @return unit kerja asal, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "unit_kerja_awal")
	public UnitKerja getUnitKerjaAwal() {
		return unitKerjaAwal;
	}

	/**
	 * Setter {@link #getUnitKerjaAwal()}.
	 *
	 * @param unitKerjaAwal unit kerja asal; boleh {@code null}
	 */
	public void setUnitKerjaAwal(UnitKerja unitKerjaAwal) {
		this.unitKerjaAwal = unitKerjaAwal;
	}

	/**
	 * Unit kerja tujuan, yaitu tempat pegawai bertugas <b>setelah</b> perpindahan. Bersama
	 * {@link #getUnitKerjaAwal()} membentuk pasangan asal-tujuan yang menjadi inti berkas ini.
	 *
	 * <p>Kolomnya boleh {@code null}, dan tidak ada pemeriksaan di tingkat model bahwa unit tujuan
	 * berbeda dari unit asal; validasi semacam itu merupakan tanggung jawab layar mutasi.</p>
	 *
	 * @return unit kerja tujuan, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "unit_kerja_tujuan")
	public UnitKerja getUnitKerjaTujuan() {
		return unitKerjaTujuan;
	}

	/**
	 * Setter {@link #getUnitKerjaTujuan()}.
	 *
	 * @param unitKerjaTujuan unit kerja tujuan; boleh {@code null}
	 */
	public void setUnitKerjaTujuan(UnitKerja unitKerjaTujuan) {
		this.unitKerjaTujuan = unitKerjaTujuan;
	}

	/**
	 * Terhitung mulai tanggal (TMT) berlakunya mutasi, yaitu saat pegawai resmi bertugas di unit
	 * kerja tujuan. Dibedakan dari {@link #getTanggalSuratUsul()} yang mencatat kapan perpindahan
	 * diusulkan; dalam praktik administrasi keduanya lazim berbeda.
	 *
	 * <p>Perlu ditegaskan bahwa tanggal ini <b>tidak menggerakkan apa pun secara otomatis</b>: tidak
	 * ada proses terjadwal yang membaca TMT lalu memindahkan penempatan pegawai. Perpindahan pada
	 * data pegawai harus dilakukan terpisah.</p>
	 *
	 * @return tanggal mulai berlaku mutasi, atau {@code null} bila belum diisi
	 */
	@Column(name = "tmt")
	public Date getTmt() {
		return tmt;
	}

	/**
	 * Setter {@link #getTmt()}.
	 *
	 * @param tmt tanggal mulai berlaku mutasi; boleh {@code null}
	 */
	public void setTmt(Date tmt) {
		this.tmt = tmt;
	}

	/**
	 * Jabatan struktural yang dipangku pegawai <b>sebelum</b> perpindahan, disimpan sebagai salinan
	 * referensi pada berkas. Boleh {@code null}, misalnya untuk pegawai yang memang tidak memangku
	 * jabatan struktural.
	 *
	 * @return jabatan struktural awal, atau {@code null} bila tidak ada/tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_struktural_awal")
	public JabatanStruktural getJabatanStrukturalAwal() {
		return jabatanStrukturalAwal;
	}

	/**
	 * Setter {@link #getJabatanStrukturalAwal()}.
	 *
	 * @param jabatanStrukturalAwal jabatan struktural sebelum perpindahan; boleh {@code null}
	 */
	public void setJabatanStrukturalAwal(JabatanStruktural jabatanStrukturalAwal) {
		this.jabatanStrukturalAwal = jabatanStrukturalAwal;
	}

	/**
	 * Jabatan struktural yang dipangku pegawai <b>setelah</b> perpindahan. Boleh {@code null}.
	 *
	 * <p>Perhatikan bahwa jabatan yang benar-benar berlaku bagi seorang pegawai <b>tidak</b> dibaca
	 * dari sini: {@code Pegawai} menurunkannya dari berkas {@link KenaikanPangkat} yang berlaku.
	 * Nilai di sini murni catatan administratif pada berkas mutasi dan tidak akan tercermin pada
	 * jabatan pegawai kecuali dibuatkan berkas kenaikan pangkat/jabatan tersendiri.</p>
	 *
	 * @return jabatan struktural akhir, atau {@code null} bila tidak ada/tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_struktural_akhir")
	public JabatanStruktural getJabatanStrukturalAkhir() {
		return jabatanStrukturalAkhir;
	}

	/**
	 * Setter {@link #getJabatanStrukturalAkhir()}.
	 *
	 * @param jabatanStrukturalAkhir jabatan struktural setelah perpindahan; boleh {@code null}
	 */
	public void setJabatanStrukturalAkhir(JabatanStruktural jabatanStrukturalAkhir) {
		this.jabatanStrukturalAkhir = jabatanStrukturalAkhir;
	}

}
