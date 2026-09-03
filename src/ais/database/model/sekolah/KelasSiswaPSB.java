package ais.database.model.sekolah;

// Generated Apr 5, 2010 1:13:29 AM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.GeneralValueObject;

/**
 * <b>Ruang PSB</b> — alokasi <i>kuota tempat</i> untuk sebuah kelas yang ditawarkan pada satu
 * gelombang Penerimaan Siswa Baru (PSB/PPDB). Tabel {@code sekolah.kelas_siswa_psb}.
 *
 * <h2>Domain terverifikasi (bukan tebakan dari nama kelas)</h2>
 * <p>Nama kelas {@code KelasSiswaPSB} menyesatkan: ini <b>bukan</b> tabel penghubung
 * "siswa ↔ kelas" dan <b>bukan</b> penempatan kelas bagi calon siswa yang lolos seleksi.
 * Entity ini adalah <b>master ruang/kuota</b>. Bukti dari kode pemanggil, bukan dari penamaan:</p>
 * <ul>
 *   <li>Judul jendela tambah/ubah pada
 *       {@code ais.action.master.sekolah.KelasSiswaPSBAction#init(KelasSiswaPSB)} berbunyi
 *       persis <b>"Tambah Ruang PSB"</b> / <b>"Ubah Ruang PSB"</b>, dan jendela popup di
 *       {@code WEB-INF/z/x/y/pages/psb/kelas_psb.zul} diberi judul <b>"Tambah Ruang"</b>.</li>
 *   <li>Seluruh method laporan di Action menamai parameternya {@code ruang}
 *       ({@code onCetakAbsensi}/{@code onCetakBau}/{@code onCetakAlbum} menerima argumen bernama
 *       {@code KelasSiswaPSB ruang} dan mengisi parameter Jasper {@code "ruang"} serta
 *       {@code "ket_ruang"}).</li>
 *   <li>Kolom kapasitas bernama {@code kapasitas_ruangan} dengan label formulir
 *       <b>"Kapasitas Ruangan"</b>.</li>
 * </ul>
 * <p>Jadi satu baris = "kelas X dibuka pada gelombang Y dengan daya tampung Z tempat".
 * Anggota kelas yang sesungguhnya (siapa calon siswa di dalamnya) TIDAK disimpan di sini,
 * melainkan di {@link KelasSiswaPunyaSiswa} / {@link KelasLesSiswaPunyaSiswa}.</p>
 *
 * <h2>Dua rasa kelas dalam satu tabel (relasi eksklusif, tidak ditegakkan DB)</h2>
 * <p>Sebuah baris menunjuk <b>salah satu</b> dari dua jenis kelas:</p>
 * <ul>
 *   <li>{@link #getKelasSiswa()} — kelas reguler/rombongan belajar ({@link KelasSiswa}),
 *       dipakai untuk ruang tes/penempatan jalur reguler;</li>
 *   <li>{@link #getKelasLesSiswa()} — kelas les/kursus ({@link KelasLesSiswa}), dipakai untuk
 *       daftar "Pilih Kelas" yang bisa dicentang calon siswa saat mengisi formulir PPDB.</li>
 * </ul>
 * <p>Keeksklusifan itu hanya ditegakkan di lapisan UI — {@code KelasSiswaPSBAction#init(...)}
 * memasang {@code EventListener} yang menyembunyikan salah satu banbox begitu yang lain terisi,
 * dan {@code onSave(...)} menolak simpan bila <b>keduanya</b> kosong. Tidak ada
 * <i>check constraint</i>, trigger, maupun validasi di entity ini yang mencegah baris berisi
 * kedua FK sekaligus, atau baris yang FK-nya diubah menjadi keduanya lewat jalur lain
 * (REST/SQL/impor). Konsumen wajib tetap memeriksa {@code null} pada kedua getter.</p>
 *
 * <h2>Siapa yang membaca entity ini</h2>
 * <ol>
 *   <li><b>Layar master "Ruang PSB"</b> —
 *       {@code ais.action.master.sekolah.KelasSiswaPSBAction}, dipasang oleh DUA halaman ZUL
 *       sekaligus: {@code pages/psb/kelas_psb.zul} (modul sekolah) <i>dan</i>
 *       {@code pages/pmb/kelas_pmb.zul} (modul PMB/perguruan tinggi). Layar PMB memakai
 *       ulang entity sekolah ini apa adanya; ia BUKAN {@code ais.database.model.KelasPmb}
 *       (tabel {@code public.kelas_pmb}) yang merupakan entity berbeda.</li>
 *   <li><b>Formulir pendaftaran PPDB</b> —
 *       {@code ais.action.master.sekolah.CalonSiswaAction#initKelasLes(...)} memakai entity ini
 *       sebagai <i>daftar putih</i> kelas les yang boleh dipilih calon siswa: query mengambil
 *       {@code kelasLesSiswa.id} dari baris {@code KelasSiswaPSB} milik gelombang yang aktif,
 *       dengan syarat {@code penuh IS NULL OR penuh = 0} dan kelas lesnya masih aktif. Inilah
 *       satu-satunya tempat nilai {@link #getPenuh()} benar-benar berpengaruh pada perilaku
 *       aplikasi bagi pengguna akhir.</li>
 * </ol>
 * <p>Salinan kembar {@code ais.action.master.KelasSiswaPSBAction} (paket {@code master}, tanpa
 * {@code .sekolah}) TIDAK dipasang oleh ZUL mana pun — hasil <i>copy-paste</i> yang yatim.
 * Perubahan perilaku layar harus dilakukan di varian {@code .sekolah}, bukan di sana.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; audit</b>: {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Label</b>: {@link #getNama()}, {@link #setNama(String)}, {@link #toString()}
 *       — <b>lihat peringatan getter destruktif di bawah</b>.</li>
 *   <li><b>Relasi</b>: {@link #getKelasSiswa()}, {@link #getKelasLesSiswa()},
 *       {@link #getGelombangPendaftaranPsb()}.</li>
 *   <li><b>Kuota</b>: {@link #getKapasitasRuangan()}, {@link #getPenuh()}.</li>
 * </ul>
 *
 * <h2>Pola arsitektur repo yang TERVERIFIKASI ADA di berkas ini</h2>
 * <ul>
 *   <li><b>Getter destruktif / write-back</b>: ADA, dan yang paling parah adalah
 *       {@link #getNama()} yang <b>merusak data</b> (uraian lengkap di Javadoc method
 *       tersebut). {@link #getKapasitasRuangan()} dan {@link #getPenuh()} juga menulis balik
 *       nilai default ke field, sehingga sekadar merender grid dapat menghasilkan
 *       {@code UPDATE} pada baris yang sebetulnya tidak disunting siapa pun — dan karena
 *       kelas ini {@link Audited}, setiap {@code UPDATE} semu itu juga menambah baris revisi
 *       Envers.</li>
 *   <li><b>Setter menolak diam-diam</b>: ADA pada {@link #setOleh(String)} dan
 *       {@link #setOlehId(String)} (nilai {@code null}/kosong diabaikan tanpa peringatan).</li>
 *   <li><b>Resolusi proxy lazy lewat {@code check(...)}</b>: ADA pada ketiga getter relasi,
 *       sesuai konvensi {@link ais.database.model.GeneralValueObject}.</li>
 *   <li><b>Deklarasi ULANG {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}</b>:
 *       ADA — dan ini <b>bukan bug</b>. {@link ais.database.model.GeneralValueObject} adalah
 *       POJO abstrak biasa (bukan {@code @Entity}/{@code @MappedSuperclass}), sehingga
 *       Hibernate tidak memetakan properti induknya; setiap entity turunan WAJIB
 *       mendeklarasikan ulang kolom-kolom itu.</li>
 *   <li><b>{@code getKeterangan()} membalik kontrak</b>: TIDAK ADA (tidak ada
 *       {@code keterangan} di entity ini).</li>
 *   <li><b>{@code compareTo()} dipangkas</b>: TIDAK ADA — kelas ini tidak menimpa
 *       {@code compareTo}, jadi memakai implementasi {@link GeneralValueObject}.</li>
 *   <li><b>Penciutan {@code TreeSet}</b>: TIDAK ADA — entity ini tidak memiliki koleksi
 *       apa pun.</li>
 *   <li><b>Cakupan tenant (sekolah/yayasan)</b>: entity ini <b>tidak punya</b> FK
 *       {@code sekolah} maupun {@code yayasan}; satu-satunya jalur tenant adalah
 *       {@link #getGelombangPendaftaranPsb()} → {@code GelombangPendaftaranPsb.getSekolah()}.
 *       Lihat bagian keamanan di bawah.</li>
 * </ul>
 *
 * <h2>Catatan keamanan (temuan pada pemanggil, bukan pada entity ini)</h2>
 * <p>Didokumentasikan di sini karena entity inilah yang menjadi objek datanya, dan pembaca
 * berkas ini perlu tahu bahwa <b>tidak ada</b> penyaringan tenant yang bisa diandalkan:</p>
 * <ul>
 *   <li>{@code KelasSiswaPSBAction#initCriteria(boolean)} membangun {@code Criteria} hanya
 *       dengan filter <i>nama</i> dan <i>gelombang</i>. <b>Nol</b> pembatasan
 *       sekolah/yayasan — bukan "fail-open", melainkan memang tidak ada filternya. Grid
 *       "Ruang PSB" karena itu menampilkan ruang milik SELURUH sekolah/yayasan dalam satu
 *       instalasi kepada siapa pun yang punya hak READ menu tersebut.</li>
 *   <li>Combobox "Gelombang" diisi lewat {@code Common.insertCombo(..., GelombangPendaftaranPsb
 *       .class, aktif)}; helper tersebut ({@code CommonComboInsertHelper}) tidak menambahkan
 *       kriteria tenant apa pun, sehingga daftar gelombang pun lintas sekolah.</li>
 *   <li>Baris grid dapat dibuka (komponen {@code MyDetail}) untuk menampilkan daftar
 *       <i>calon siswa</i> di dalam kelas terkait melalui {@code DetailKelasSiswaHelper} /
 *       {@code DetailKelasLesSiswaHelper} — sehingga ketiadaan filter di atas berlanjut
 *       menjadi paparan data pribadi calon siswa lintas tenant.</li>
 *   <li>Hak {@code UPDATE}/{@code DELETE} diperiksa dengan {@code CommonPrivilages
 *       .checkPrevilages(...)} yang bersifat global per-peran, bukan per-tenant: pengguna
 *       dengan hak ubah/hapus di satu sekolah dapat mengubah kuota atau menghapus ruang PSB
 *       milik sekolah lain.</li>
 *   <li>Banbox pemilih kelas ({@code AmbilDataKelasSiswaBanbox}) mengunci combo "Sekolah"
 *       hanya bila {@code SekolahUtil.getSekolah() != null}; bila konteks sekolah tidak
 *       terdeteksi, combo tetap "Semua" — pola fail-open cakupan tenant yang sudah dikenal
 *       di repo ini.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see GelombangPendaftaranPsb
 * @see KelasSiswa
 * @see KelasLesSiswa
 * @see KelasSiswaPunyaSiswa
 * @see KelasLesSiswaPunyaSiswa
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sekolah", name = "kelas_siswa_psb")
public class KelasSiswaPSB extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar instance yang tersimpan di session ZK/HTTP tetap
	 * dapat dibaca setelah kelas dikompilasi ulang. JANGAN diubah.
	 */
	private static final long serialVersionUID = -7550466125892447098L;

	/**
	 * Kunci primer, kolom {@code id}, dibangkitkan basis data ({@code IDENTITY} → sekuens
	 * berurutan). Lihat {@link #getId()}.
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang menyimpan baris ini (kolom {@code oleh} pada tabel Envers/audit).
	 * Diisi otomatis oleh {@code AuditTimestampInterceptor}, bukan oleh formulir. Lihat
	 * {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * Id pengguna terakhir yang menyimpan baris ini (pendamping {@link #oleh}). Diisi otomatis
	 * oleh {@code AuditTimestampInterceptor}. Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p>Getter murni tanpa efek samping. Nilai diisi oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan oleh layar mana pun.</p>
	 *
	 * @return id pengguna penyimpan terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna penyimpan terakhir.
	 *
	 * <p><b>Perilaku non-obvious:</b> argumen {@code null} atau string kosong/hanya spasi
	 * <b>diabaikan diam-diam</b> — nilai lama dipertahankan dan tidak ada pengecualian yang
	 * dilempar. Tujuannya menjaga jejak audit agar tidak terhapus oleh proses yang kebetulan
	 * memanggil setter tanpa konteks pengguna (mis. job latar atau impor), tetapi konsekuensinya
	 * jejak audit <b>tidak dapat dikosongkan kembali</b> lewat API ini.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong setelah {@code trim()}
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna penyimpan terakhir.
	 *
	 * <p>Menerapkan aturan "tolak diam-diam" yang sama dengan {@link #setOlehId(String)}:
	 * {@code null} atau string kosong tidak menimpa nilai lama.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah {@code trim()}
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini. Getter murni.
	 *
	 * @return nama pengguna penyimpan terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dijalankan Hibernate tepat sebelum pernyataan
	 * {@code UPDATE} dikirim ke basis data.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #oleh}, {@link #olehId}, dan {@link #tanggal_dirubah} dari konteks
	 * pengguna yang sedang aktif.</p>
	 *
	 * <p><b>Efek samping penting:</b> karena beberapa getter di kelas ini menulis balik ke
	 * field-nya (lihat {@link #getNama()}, {@link #getKapasitasRuangan()}, {@link #getPenuh()}),
	 * baris ini bisa menjadi "kotor" dan memicu {@code UPDATE} — dan dengan demikian memicu
	 * callback ini — meskipun pengguna tidak menyunting apa pun.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), lalu diperbarui oleh {@link #onUpdate()}.
	 * Lihat {@link #getTanggal_dirubah()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Setter polos tanpa validasi — menerima {@code null}. Normalnya tidak dipanggil kode
	 * aplikasi; pengisian dilakukan Hibernate saat memuat baris dan oleh {@link #onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru (boleh {@code null})
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir. Getter murni.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek baru karena field
	 *         diinisialisasi ke waktu server saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity, dipakai antara lain oleh komponen ZK saat menampilkan objek ini
	 * sebagai item combobox/banbox.
	 *
	 * <p><b>Non-obvious — sengaja dicatat:</b> method ini membaca <b>field</b> {@link #nama}
	 * secara langsung, BUKAN {@link #getNama()}. Akibatnya:</p>
	 * <ul>
	 *   <li>pada objek yang baru dibuat lewat {@link #KelasSiswaPSB()} dan belum di-{@code set},
	 *       method ini mengembalikan {@code null} — pemanggil yang merangkainya dengan
	 *       {@code +} akan mendapat teks {@code "null"}, dan yang memanggil method {@link String}
	 *       di atasnya akan mendapat {@link NullPointerException};</li>
	 *   <li>nilainya bisa berbeda dari {@link #getNama()} untuk objek yang sama, karena
	 *       {@code getNama()} menimpa field tersebut setiap kali dipanggil.</li>
	 * </ul>
	 *
	 * @return isi mentah field {@code nama}, apa adanya (dapat {@code null})
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Label ruang, kolom {@code nama} (maks. 150 karakter, {@code NOT NULL}).
	 *
	 * <p>Secara konseptual merupakan salinan denormalisasi dari nama kelas yang ditunjuk, agar
	 * pencarian pada layar master dapat memakai {@code ilike} langsung ke kolom ini. Lihat
	 * peringatan pada {@link #getNama()} mengenai bagaimana salinan tersebut dirusak.</p>
	 */
	private String nama;

	/**
	 * Kelas reguler/rombongan belajar yang diwakili ruang ini (kolom FK {@code kelas_siswa}).
	 * Saling eksklusif dengan {@link #kelasLesSiswa}. Lihat {@link #getKelasSiswa()}.
	 */
	private KelasSiswa kelasSiswa;

	/**
	 * Kelas les/kursus yang diwakili ruang ini (kolom FK {@code kelas_les_siswa}).
	 * Saling eksklusif dengan {@link #kelasSiswa}. Lihat {@link #getKelasLesSiswa()}.
	 */
	private KelasLesSiswa kelasLesSiswa;

	/**
	 * Daya tampung ruang dalam satuan orang (kolom {@code kapasitas_ruangan}, {@code NOT NULL}).
	 * Lihat {@link #getKapasitasRuangan()} untuk perilaku nilai default.
	 */
	private Integer kapasitasRuangan;

	/**
	 * Gelombang PSB tempat ruang ini ditawarkan (kolom FK {@code gelombang_pendaftaran_psb}).
	 * Merupakan satu-satunya jalur menuju konteks sekolah/yayasan bagi entity ini. Lihat
	 * {@link #getGelombangPendaftaranPsb()}.
	 */
	private GelombangPendaftaranPsb gelombangPendaftaranPsb;

	/**
	 * Penanda "ruang sudah penuh" (kolom {@code penuh}): {@code 0} = masih menerima,
	 * {@code 1} = penuh. Disimpan sebagai {@link Integer}, bukan {@code boolean}. Lihat
	 * {@link #getPenuh()}.
	 */
	private Integer penuh;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi entity, dan
	 * dipakai layar master saat menekan tombol "Tambah"
	 * ({@code KelasSiswaPSBAction#onAdd(Event)} memanggil {@code init(new KelasSiswaPSB())}).
	 *
	 * <p>Objek hasil konstruktor ini hanya memiliki {@link #tanggal_dirubah} yang terisi; seluruh
	 * field lain {@code null}. Perhatikan bahwa {@code KelasSiswaPSBAction#init(...)} langsung
	 * memanggil {@code new BigDecimal(kelasSiswaPSB.getKapasitasRuangan())} pada objek baru —
	 * itu aman semata-mata karena {@link #getKapasitasRuangan()} menyuntikkan default
	 * {@code 3000}, bukan karena field-nya benar-benar terisi.</p>
	 */
	public KelasSiswaPSB() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Dipetakan {@code IDENTITY} dengan {@code insertable = false} — nilai sepenuhnya
	 * ditentukan basis data pada saat {@code INSERT}. Karena berurutan, id dapat ditebak/dienumerasi;
	 * jangan jadikan id sebagai satu-satunya pengaman akses.</p>
	 *
	 * <p>Dipakai layar master untuk membedakan mode tambah dan ubah
	 * ({@code getId() == null ? "Tambah Ruang PSB" : "Ubah Ruang PSB"}) dan sebagai parameter
	 * {@code "ruang"} pada ketiga laporan Jasper.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci primer. Setter polos tanpa validasi; normalnya hanya dipanggil Hibernate
	 * saat memuat baris.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan label ruang — <b>getter destruktif dengan bug nyata</b>.
	 *
	 * <p><b>Maksud yang tampaknya diinginkan penulis:</b> menyegarkan salinan denormalisasi
	 * {@link #nama} dari kelas yang ditunjuk, sehingga label ruang selalu mengikuti nama kelas
	 * terkini.</p>
	 *
	 * <p><b>Yang sebenarnya terjadi:</b> kedua baris penugasan berdiri sendiri-sendiri, dan
	 * baris kedua <b>menimpa hasil baris pertama tanpa syarat</b>:</p>
	 * <pre>
	 * nama = getKelasSiswa()    == null ? "" : getKelasSiswa().getNama();
	 * nama = getKelasLesSiswa() == null ? "" : getKelasLesSiswa().getNama();  // menimpa
	 * </pre>
	 * <p>Karena kedua relasi bersifat saling eksklusif (lihat Javadoc kelas), untuk ruang yang
	 * dibangun dari {@link KelasSiswa} — yaitu ruang jalur reguler — cabang kedua selalu
	 * mengevaluasi {@code kelasLesSiswa == null} dan menetapkan {@code nama = ""}. Hasilnya:</p>
	 * <ul>
	 *   <li>method ini <b>selalu mengembalikan string kosong</b> untuk ruang berbasis kelas
	 *       reguler; nama kelas yang baru saja diambil pada baris pertama dibuang;</li>
	 *   <li>karena penugasan itu mengenai <b>field</b>, nilai {@code ""} ikut tersimpan: pada
	 *       flush berikutnya Hibernate mendeteksi properti {@code nama} berubah dan menerbitkan
	 *       {@code UPDATE ... SET nama = ''}. Sekadar membuka grid sudah cukup memicunya, karena
	 *       renderer memanggil {@code kelasSiswaPSB.getNama()} dan
	 *       {@code Common.refreshUpdate(...)};</li>
	 *   <li>kolom {@code nama} dipetakan {@code nullable = false}, sehingga {@code ""} lolos
	 *       tanpa error basis data — kerusakan berlangsung senyap;</li>
	 *   <li>kelas ini {@link Audited}, jadi setiap penimpaan tersebut juga menambah revisi
	 *       Envers yang menyesatkan.</li>
	 * </ul>
	 *
	 * <p><b>Dampak fungsional yang dapat diamati:</b></p>
	 * <ul>
	 *   <li>kolom grid "Nama Kelas" pada layar Ruang PSB tampil kosong untuk ruang reguler
	 *       ({@code RevisiHelper.createNewRevisi(..., kelasSiswaPSB.getNama())});</li>
	 *   <li>filter pencarian {@code Restrictions.ilike("nama", ...)} di
	 *       {@code KelasSiswaPSBAction#initCriteria(boolean)} membandingkan terhadap kolom yang
	 *       sudah dikosongkan, sehingga pencarian berdasarkan nama tidak pernah menemukan ruang
	 *       reguler;</li>
	 *   <li>parameter laporan {@code "ket_ruang"} pada {@code onCetakAlbum(...)} ikut kosong.</li>
	 * </ul>
	 *
	 * <p><b>Catatan konsistensi:</b> nilai kembalian juga di-{@code trim()}, sedangkan
	 * {@link #toString()} membaca field mentah tanpa {@code trim()} — dua jalur baca yang bisa
	 * memberi hasil berbeda untuk objek yang sama.</p>
	 *
	 * <p><b>Peringatan pemeliharaan:</b> memperbaiki bug ini (mis. dengan menjadikan baris kedua
	 * bersyarat) akan mengubah isi kolom {@code nama} di basis data pada pembacaan berikutnya
	 * dan otomatis "menghidupkan" kembali filter pencarian — periksa dampaknya pada data lama
	 * yang sudah terlanjur dikosongkan sebelum melakukannya.</p>
	 *
	 * @return nama kelas les bila ruang ini berbasis kelas les; string kosong untuk kasus
	 *         lainnya (termasuk seluruh ruang berbasis kelas reguler). Praktis tidak pernah
	 *         {@code null}, karena kedua penugasan di atas selalu menghasilkan nilai non-null
	 */
	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		nama = getKelasSiswa() == null ? "" : getKelasSiswa().getNama();
		nama = getKelasLesSiswa() == null ? "" : getKelasLesSiswa().getNama();
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan label ruang.
	 *
	 * <p>Setter polos tanpa validasi. Praktis tidak berguna dari sisi aplikasi: nilai apa pun
	 * yang ditetapkan di sini akan ditimpa pada pemanggilan {@link #getNama()} berikutnya.
	 * Jalur pemanggil sesungguhnya adalah Hibernate saat memuat baris dari basis data.</p>
	 *
	 * @param nama label ruang (boleh {@code null})
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menetapkan daya tampung ruang.
	 *
	 * <p>Setter polos tanpa validasi — tidak ada pemeriksaan nilai negatif maupun nol. Dipanggil
	 * {@code KelasSiswaPSBAction#onSave(Event)} dari isi {@code Decimalbox} "Kapasitas Ruangan"
	 * (Action hanya memvalidasi bahwa kotaknya tidak kosong, bukan bahwa nilainya masuk akal).</p>
	 *
	 * @param kapasitasRuangan daya tampung dalam satuan orang (boleh {@code null})
	 */
	public void setKapasitasRuangan(Integer kapasitasRuangan) {
		this.kapasitasRuangan = kapasitasRuangan;
	}

	/**
	 * Mengembalikan daya tampung ruang, dengan <b>default 3000 yang ditulis balik</b>.
	 *
	 * <p>Bila field masih {@code null}, method ini menetapkannya ke {@code 3000} sebelum
	 * mengembalikannya. Angka tersebut adalah <i>magic number</i> tanpa konstanta bernama;
	 * praktis berarti "tak terbatas" — ia jauh melampaui daya tampung ruang ujian mana pun dan
	 * berfungsi sebagai penjaga agar kolom {@code NOT NULL} tidak pernah gagal disimpan.</p>
	 *
	 * <p><b>Efek samping:</b> penugasan mengenai field, sehingga entity terkelola yang kolom
	 * {@code kapasitas_ruangan}-nya masih {@code NULL} (mis. baris hasil impor lama) akan
	 * ter-{@code UPDATE} menjadi {@code 3000} begitu ada yang membacanya — termasuk saat
	 * Hibernate sendiri membaca properti ini untuk pemeriksaan dirty. Konstruktor default juga
	 * mengandalkan perilaku ini: {@code new BigDecimal(kelasSiswaPSB.getKapasitasRuangan())} di
	 * {@code KelasSiswaPSBAction#init(...)} hanya aman berkat default tersebut.</p>
	 *
	 * <p><b>Kaitan dengan {@link #getPenuh()}:</b> nilai ini dibandingkan dengan hasil hitung
	 * {@code KelasSiswaPSBAction#cekRuanganIsi(KelasSiswaPSB)} memakai {@code equals}
	 * (kesamaan persis), bukan {@code >=} — lihat catatan pada {@link #getPenuh()}.</p>
	 *
	 * @return daya tampung ruang; tidak pernah {@code null}
	 */
	@Column(name = "kapasitas_ruangan", length = 10, nullable = false)
	public Integer getKapasitasRuangan() {
		if (kapasitasRuangan == null) {
			kapasitasRuangan = 3000;
		}
		return kapasitasRuangan;
	}

	/**
	 * Menetapkan gelombang PSB pemilik ruang ini.
	 *
	 * <p>Setter polos tanpa validasi. Dipanggil {@code KelasSiswaPSBAction#onSave(Event)} dari
	 * item combobox "Gelombang". Perhatikan bahwa layar menonaktifkan combo tersebut bila ruang
	 * sudah terisi ({@code cekRuanganIsi(...) > 0}) — pengaman itu murni di UI dan tidak
	 * tercermin di entity, sehingga jalur non-UI tetap dapat memindahkan ruang berisi ke
	 * gelombang lain.</p>
	 *
	 * @param gelombangPendaftaranPsb gelombang pemilik (boleh {@code null})
	 */
	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

	/**
	 * Mengembalikan gelombang PSB pemilik ruang ini.
	 *
	 * <p>Relasi {@code @ManyToOne} malas ({@code LAZY}) dengan cascade {@code PERSIST}/
	 * {@code MERGE}. Sesuai konvensi repo, getter memanggil
	 * {@link ais.database.model.GeneralValueObject#check(Object)} untuk meresolusi proxy Hibernate
	 * yang mungkin sudah lepas dari session, lalu <b>menulis balik</b> hasilnya ke field —
	 * bentuk write-back yang jinak (menukar proxy dengan instance yang sama secara identitas
	 * entity), berbeda dari kerusakan pada {@link #getNama()}.</p>
	 *
	 * <p><b>Signifikansi:</b> inilah satu-satunya jalur dari ruang menuju konteks tenant. Ketiga
	 * method laporan pada Action mengambil {@code getGelombangPendaftaranPsb().getSekolah()
	 * .getId()} sebagai parameter {@code sekolah_id} — tanpa pemeriksaan {@code null}, sehingga
	 * ruang yang gelombangnya kosong (mungkin terjadi karena kolom FK tidak {@code NOT NULL})
	 * akan melempar {@link NullPointerException} saat mencetak.</p>
	 *
	 * @return gelombang pemilik, atau {@code null} bila kolom FK kosong / referensi tak dapat
	 *         diresolusi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_psb")
	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
		return gelombangPendaftaranPsb;
	}

	/**
	 * Menetapkan penanda penuh.
	 *
	 * <p>Setter polos tanpa validasi; tidak ada pembatasan bahwa nilainya harus {@code 0} atau
	 * {@code 1}. Dua pemanggil nyata, keduanya di layar master:</p>
	 * <ul>
	 *   <li>otomatis, di {@code KelasSiswaPSBRenderer#render(...)} saat jumlah penghuni dinilai
	 *       sudah menyamai kapasitas;</li>
	 *   <li>manual, lewat checkbox "Penuh" pada kolom grid, yang menyimpan langsung dengan
	 *       {@code Common.refreshSaveOrUpdate(...)} tanpa konfirmasi.</li>
	 * </ul>
	 *
	 * @param penuh {@code 0} = masih menerima, {@code 1} = penuh (boleh {@code null})
	 */
	public void setPenuh(Integer penuh) {
		this.penuh = penuh;
	}

	/**
	 * Mengembalikan penanda penuh, dengan <b>default {@code 0} yang ditulis balik</b>.
	 *
	 * <p>Bila field masih {@code null}, method ini menetapkannya ke {@code 0} ("belum penuh")
	 * sebelum mengembalikannya — write-back yang, seperti {@link #getKapasitasRuangan()}, dapat
	 * memicu {@code UPDATE} dan revisi Envers pada baris yang tidak disunting siapa pun.</p>
	 *
	 * <p><b>Peran bisnis nyata.</b> Nilai ini adalah satu-satunya properti entity yang benar-benar
	 * memengaruhi pengalaman calon siswa:
	 * {@code CalonSiswaAction#initKelasLes(...)} hanya menawarkan kelas les yang barisnya
	 * memenuhi {@code penuh IS NULL OR penuh = 0}. Ruang yang ditandai {@code 1} langsung
	 * menghilang dari daftar "Pilih Kelas" pada formulir PPDB.</p>
	 *
	 * <p><b>Dua kelemahan penegakan kuota yang perlu diketahui pemelihara</b> (keduanya berada di
	 * {@code KelasSiswaPSBAction}, bukan di entity ini):</p>
	 * <ol>
	 *   <li><b>Penandaan otomatis memakai kesamaan persis.</b> Renderer grid mengevaluasi
	 *       {@code isi.equals(kelasSiswaPSB.getKapasitasRuangan())}. Bila jumlah penghuni pernah
	 *       <i>melewati</i> kapasitas (mis. dua pendaftaran hampir bersamaan, atau kapasitas
	 *       diturunkan admin setelah kelas terisi), kondisi itu tidak pernah lagi terpenuhi dan
	 *       ruang <b>tidak pernah</b> otomatis ditandai penuh.</li>
	 *   <li><b>Penandaan hanya terjadi saat layar admin dibuka.</b> Pemeriksaan kuota berada di
	 *       dalam {@code render(...)} grid Ruang PSB; jalur pendaftaran calon siswa sendiri
	 *       hanya <i>membaca</i> {@code penuh} dan tidak pernah menghitung ulang isi ruang.
	 *       Selama tidak ada admin yang membuka layar tersebut, kuota tidak ditegakkan sama
	 *       sekali dan kelas dapat terisi melebihi {@link #getKapasitasRuangan()}.</li>
	 * </ol>
	 *
	 * @return {@code 0} bila ruang masih menerima, {@code 1} bila sudah penuh; tidak pernah
	 *         {@code null}
	 */
	@Column(name = "penuh")
	public Integer getPenuh() {
		if (penuh == null) {
			penuh = 0;
		}
		return penuh;
	}

	/**
	 * Mengembalikan kelas reguler/rombongan belajar yang diwakili ruang ini.
	 *
	 * <p>Relasi {@code @ManyToOne} malas dengan resolusi proxy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan write-back jinak ke field
	 * (pola standar repo).</p>
	 *
	 * <p>Bernilai {@code null} untuk ruang yang dibangun dari kelas les — pemanggil di layar
	 * master memakai kondisi itu untuk memutuskan tab detail mana yang ditampilkan dan query
	 * hitung penghuni mana ({@link KelasSiswaPunyaSiswa} vs {@link KelasLesSiswaPunyaSiswa})
	 * yang dijalankan.</p>
	 *
	 * @return kelas reguler terkait, atau {@code null} bila ruang ini berbasis kelas les / FK
	 *         kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_siswa")
	public KelasSiswa getKelasSiswa() {
		kelasSiswa = check(kelasSiswa);
		return kelasSiswa;
	}

	/**
	 * Menetapkan kelas reguler yang diwakili ruang ini.
	 *
	 * <p>Setter polos tanpa validasi: <b>tidak</b> mengosongkan {@link #kelasLesSiswa}, sehingga
	 * entity ini sendiri tidak menegakkan keeksklusifan kedua relasi. Aturan tersebut hanya ada
	 * di {@code KelasSiswaPSBAction#init(...)}/{@code onSave(Event)}.</p>
	 *
	 * @param kelasSiswa kelas reguler (boleh {@code null})
	 */
	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}

	/**
	 * Mengembalikan kelas les/kursus yang diwakili ruang ini.
	 *
	 * <p>Relasi {@code @ManyToOne} malas dengan resolusi proxy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan write-back jinak ke field.</p>
	 *
	 * <p>Nilai inilah yang menentukan hasil {@link #getNama()} (lihat bug yang diuraikan di
	 * sana), dan yang diproyeksikan sebagai {@code kelasLesSiswa.id} oleh
	 * {@code CalonSiswaAction#initKelasLes(...)} untuk menyusun daftar kelas les yang boleh
	 * dipilih calon siswa.</p>
	 *
	 * @return kelas les terkait, atau {@code null} bila ruang ini berbasis kelas reguler / FK
	 *         kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_les_siswa")
	public KelasLesSiswa getKelasLesSiswa() {
		kelasLesSiswa = check(kelasLesSiswa);
		return kelasLesSiswa;
	}

	/**
	 * Menetapkan kelas les/kursus yang diwakili ruang ini.
	 *
	 * <p>Setter polos tanpa validasi: <b>tidak</b> mengosongkan {@link #kelasSiswa}; sama seperti
	 * {@link #setKelasSiswa(KelasSiswa)}, keeksklusifan relasi tidak ditegakkan di lapisan
	 * model.</p>
	 *
	 * @param kelasLesSiswa kelas les (boleh {@code null})
	 */
	public void setKelasLesSiswa(KelasLesSiswa kelasLesSiswa) {
		this.kelasLesSiswa = kelasLesSiswa;
	}

}
