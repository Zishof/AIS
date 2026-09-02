package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kkn.KknPunyaKomponenPenilaianKkn;
import ais.database.model.kkn.KomponenPenilaianKkn;

/**
 * Keanggotaan seorang mahasiswa pada satu kelompok KKN — sekaligus <b>kartu nilai</b> mahasiswa
 * tersebut untuk gelaran KKN yang bersangkutan.
 *
 * <p>Entity Hibernate atas tabel {@code public.mahasiswa_dapat_kelompok_kelompok_kkn} (perhatikan
 * kata "kelompok" yang memang tertulis dua kali pada nama tabel — jangan "dirapikan", nama itu
 * dipakai apa adanya oleh basis data produksi). Turunan langsung
 * {@link ais.database.model.GeneralValueObject} dan pengimplementasi
 * {@link ais.database.model.VOPesertaPembelajaran}, ber-{@code @Audited} (Envers menyimpan riwayat
 * setiap perubahan) serta {@code dynamicInsert}/{@code dynamicUpdate} sehingga hanya kolom yang
 * benar-benar berubah yang ikut di-{@code INSERT}/{@code UPDATE}.</p>
 *
 * <h2>Posisi dalam alur modul KKN</h2>
 *
 * <p>Modul KKN berjalan berlapis dan entity ini duduk di lapis ketiga:</p>
 *
 * <ol>
 *   <li>{@link ais.database.model.Kkn} — gelaran/periode KKN (akar modul: tahun akademik,
 *   semester, jadwal, syarat SKS/IPK).</li>
 *   <li>{@code MahasiswaDaftarKkn} — pendaftaran + seleksi. Selama {@code terima} belum bernilai
 *   {@code MahasiswaDaftarKkn.DITERIMA}, mahasiswa tidak boleh masuk kelompok mana pun.</li>
 *   <li>{@link ais.database.model.kkn.KelompokKkn} — kelompok/rombongan beserta lokasi, kuota, dan
 *   sampai sepuluh dosen pembimbing. <b>Entity ini</b> menyambungkan {@code KelompokKkn} dengan
 *   {@link ais.database.model.Mahasiswa}: satu baris = satu mahasiswa di satu kelompok.</li>
 *   <li>Pelaksanaan: {@code Pertemuan}/aktivitas KKN, penilaian per komponen, lalu konversi nilai
 *   ke KRS lewat {@link ais.database.model.Detailperkuliahan}, dan terakhir pencetakan
 *   {@code Sertifikat}.</li>
 * </ol>
 *
 * <p><b>Tidak ada FK langsung ke {@code kkn}.</b> Gelaran KKN hanya bisa dicapai dua hop lewat
 * {@code kelompokKkn.kkn}; itulah sebabnya semua query di modul ini memakai
 * {@code createAlias("kelompokKkn", "kelompokKkn").add(Restrictions.eq("kelompokKkn.kkn", kkn))}.
 * Konsekuensinya, memindah sebuah kelompok ke gelaran KKN lain otomatis memindah seluruh
 * anggotanya, dan tidak ada mekanisme yang menjaga agar mahasiswa hanya punya satu kelompok per
 * gelaran — keunikan itu dijaga oleh kode pemanggil, bukan oleh basis data.</p>
 *
 * <p>Entity ini <b>tidak memiliki satu pun koleksi</b>. Sama seperti {@link ais.database.model.Kkn},
 * semua relasi ditarik dari sisi anak dengan {@code Restrictions.eq(...)}, sehingga menghapus satu
 * baris di sini tidak meng-cascade apa pun.</p>
 *
 * <h2>Dua jalur pembuatan baris</h2>
 *
 * <ul>
 *   <li><b>Mahasiswa memilih sendiri</b> ({@code KknUntukMahasiswaAction}) — hanya ditawarkan bila
 *   pendaftarannya sudah {@code DITERIMA} dan belum ada baris dengan {@link #getDiterima()}
 *   {@code = true}. Baris yang terbentuk berstatus <i>belum disetujui</i>.</li>
 *   <li><b>Admin/operator menempatkan</b> ({@code AmbilDataMahasiswaKelompokKknHelper},
 *   {@code KelompokKknHelper}) — penempatan massal maupun satuan; di sini pula centang
 *   "Diterima" diaktifkan.</li>
 * </ul>
 *
 * <p>{@link #getDiterima()} berperan sebagai gerbang: selama {@code false} mahasiswa masih boleh
 * berpindah kelompok dan barisnya masih boleh dihapus; begitu {@code true}, pilihan terkunci dan
 * tombol cetak sertifikat baru muncul. Flag ini <b>dua arah</b> — centang di
 * {@code KelompokKknHelper} bisa dinyalakan maupun dimatikan kembali oleh operator.</p>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ul>
 *   <li><b>Audit/jejak perubahan</b>: {@link #getOleh()}, {@link #setOleh(String)},
 *   {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *   {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}. Field-field ini
 *   <b>sengaja dideklarasikan ulang</b> di sini; lihat catatan di bawah.</li>
 *   <li><b>Identitas &amp; relasi</b>: {@link #getId()}, {@link #getKelompokKkn()},
 *   {@link #getMahasiswa()}, {@link #getDetailperkuliahan()}, {@link #ambilVOPembelajaran()},
 *   {@link #toString()}.</li>
 *   <li><b>Status keanggotaan &amp; catatan</b>: {@link #getDiterima()}, {@link #getKeterangan()},
 *   {@link #getHasil()}, {@link #getNamaDosen()}.</li>
 *   <li><b>Nilai — mesin sesungguhnya kelas ini</b>: {@link #getDetailNilai()},
 *   {@link #populateDetailNilai(KomponenPenilaianKkn, Double, Boolean)},
 *   {@link #retreiveDetailNilai(KomponenPenilaianKkn)},
 *   {@link #retreiveDetailVerifikasiNilai(KknPunyaKomponenPenilaianKkn)},
 *   {@link #hitungTotalNilai(Boolean)}, {@link #hitungTotalNilai(Boolean, List)},
 *   {@link #bersihkanNilaiKeDefault()}, {@link #bersihkanNilaiKeDefault(List)},
 *   {@link #refreshNilaiKeDefault()}, {@link #reloadKknPunyaKomponenPenilaianKkn(Session)},
 *   ditambah nilai ringkas {@link #getTotalNilai()}, {@link #getNilaiHuruf()},
 *   {@link #getTotalIP()}, {@link #getLulus()}.</li>
 *   <li><b>Cache berkas</b>: {@link #write()}.</li>
 * </ul>
 *
 * <h2>Format kolom {@code detailNilai}</h2>
 *
 * <p>Nilai per komponen <b>tidak</b> disimpan sebagai baris tabel tersendiri, melainkan
 * dikemas jadi satu string di kolom {@code detailNilai} bertipe {@code text}. Bentuknya:</p>
 *
 * <pre>
 *   &lt;idKomponen&gt;,&lt;nilai&gt;,0,&lt;bobot&gt;,&lt;terverifikasi&gt;;&lt;idKomponen&gt;,...
 * </pre>
 *
 * <p>Entri dipisah {@code ";"}, kolom di dalam entri dipisah {@code ","}, dengan arti:</p>
 *
 * <ol start="0">
 *   <li>id {@link ais.database.model.kkn.KomponenPenilaianKkn} — <b>bukan</b> id
 *   {@link ais.database.model.kkn.KknPunyaKomponenPenilaianKkn}, meskipun banyak variabel lokal
 *   di kelas ini terlanjur dinamai {@code idKknPunyaKomponenPenilaianKkn};</li>
 *   <li>nilai angka komponen tersebut;</li>
 *   <li>selalu literal {@code "0"} — slot warisan format lama, tidak pernah dibaca;</li>
 *   <li>bobot komponen ({@code KomponenPenilaianKkn.getBobot()}), dipakai sebagai
 *   <i>persen</i> saat perata-rataan;</li>
 *   <li>penanda "nilai sudah diverifikasi" ({@code true}/{@code false}).</li>
 * </ol>
 *
 * <p>Karena disimpan sebagai teks bebas, baris lama bisa korup (segmen kosong, token bukan angka,
 * literal teks {@code "null"}). Seluruh parser di kelas ini karena itu memakai penjaga
 * {@code Common.isNumber(...)} dan memperlakukan token rusak sebagai "belum dinilai", bukan
 * sebagai kesalahan — riwayat perbaikannya tercatat pada komentar di masing-masing method.</p>
 *
 * <h2>Hal-hal non-obvious yang wajib diketahui sebelum menyunting</h2>
 *
 * <ul>
 *   <li><b>Field audit dideklarasikan ulang bukan karena kelalaian.</b>
 *   {@link ais.database.model.GeneralValueObject} adalah POJO abstrak biasa — <i>bukan</i>
 *   {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti
 *   induknya sama sekali. {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah}
 *   harus dideklarasikan lagi di setiap entity agar punya kolom. Menghapusnya "supaya DRY" akan
 *   menghilangkan kolom-kolom itu dari pemetaan.</li>
 *   <li><b>Beberapa getter menulis balik ke field</b> sehingga, pada object yang masih melekat pada
 *   session Hibernate, sekadar <i>membaca</i> dapat memicu {@code UPDATE} lewat dirty-checking:
 *   {@link #getKelompokKkn()} dan {@link #getMahasiswa()} (penugasan ulang hasil {@code check()}),
 *   {@link #getLulus()} (mengoreksi {@code lulus} agar selaras master Nilai Huruf, sekaligus
 *   menormalkan {@code nilaiHuruf}), {@link #getNamaDosen()} (menghitung ulang nama pembimbing dari
 *   kelompok), serta {@link #toString()} (menugaskan ulang {@code kelompokKkn} dan
 *   {@code mahasiswa}). {@link #refreshNilaiKeDefault()} yang dipanggil dari hampir semua method
 *   nilai juga menulis ulang {@code detailNilai}.</li>
 *   <li><b>Tidak ada satu pun method di kelas ini yang membuka atau menutup session Hibernate
 *   sendiri.</b> {@link #bersihkanNilaiKeDefault()} dan {@link #refreshNilaiKeDefault()} memakai
 *   {@code HibernateUtil.currentSession()} — session milik thread yang sudah berjalan — dan
 *   membiarkannya terbuka. Jangan menambahkan {@code session.close()} di sini: yang tertutup adalah
 *   session milik request pemanggil.</li>
 *   <li><b>Tidak ada {@code @Transient}.</b> Pemetaan memakai akses properti (anotasi ada di
 *   getter), jadi setiap getter yang bentuknya {@code getXxx()} menjadi kolom — termasuk
 *   {@link #getNamaDosen()} yang isinya sebenarnya data turunan dari kelompok. Menambah getter baru
 *   berarti menambah kolom, kecuali diberi {@code @Transient}.</li>
 *   <li>{@link #reloadKknPunyaKomponenPenilaianKkn(Session)} <b>rusak dan tidak dipakai</b> —
 *   lihat catatan pada method tersebut.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.Kkn
 * @see ais.database.model.kkn.KelompokKkn
 * @see ais.database.model.kkn.KomponenPenilaianKkn
 * @see ais.database.model.kkn.KknPunyaKomponenPenilaianKkn
 * @see ais.database.model.Detailperkuliahan
 * @see ais.database.model.VOPesertaPembelajaran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "mahasiswa_dapat_kelompok_kelompok_kkn")
public class MahasiswaDapatKelompokKkn extends GeneralValueObject implements VOPesertaPembelajaran {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel; lihat {@link #getId()}. */
	private Long id;
	/** Bayangan nama pengguna pengubah terakhir; lihat {@link #getOleh()}. */
	private String oleh;
	/** Bayangan id pengguna pengubah terakhir; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna yang mengubah baris ini.
	 *
	 * <p>Masukan {@code null} atau kosong <b>diabaikan diam-diam</b> sehingga jejak audit lama tidak
	 * bisa terhapus oleh pemanggil yang lalai mengisi konteks pengguna.</p>
	 *
	 * @param olehId id pengguna; nilai kosong/{@code null} tidak berpengaruh apa pun
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan kosong/{@code null} diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh apa pun
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} dijalankan dan
	 * meneruskan ke {@code AuditTimestampInterceptor.ubah(this)} agar {@code oleh}, {@code olehId},
	 * dan {@code tanggal_dirubah} terisi dari konteks pengguna yang sedang aktif.
	 *
	 * <p>Tidak untuk dipanggil langsung dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat dan diperbarui
	 * {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks keanggotaan ini, berbentuk {@code "<kelompok>-<mahasiswa>"}.
	 *
	 * <p><b>Bukan method murni.</b> Sebelum merangkai teks, kedua relasi diambil lewat
	 * {@link #getKelompokKkn()} dan {@link #getMahasiswa()} lalu <b>ditugaskan kembali ke field</b>.
	 * Artinya sekadar mencetak object ini bisa memicu resolusi proxy lazy (berpotensi query
	 * tambahan, bahkan pembacaan ulang lewat session baru) dan menandai object sebagai kotor bagi
	 * dirty-checking Hibernate. Hindari memanggilnya di dalam log yang dieksekusi berulang.</p>
	 *
	 * @return gabungan {@code KelompokKkn.toString()} dan {@code Mahasiswa.toString()} dipisah
	 *         tanda hubung
	 */
	public String toString() {
		kelompokKkn = getKelompokKkn();
		mahasiswa = getMahasiswa();
		return kelompokKkn + "-" + mahasiswa;
	}

	/** Nama dosen pembimbing hasil turunan dari kelompok; lihat {@link #getNamaDosen()}. */
	private String namaDosen;
	/** Kelompok KKN yang diikuti; lihat {@link #getKelompokKkn()}. */
	private KelompokKkn kelompokKkn;
	/** Mahasiswa anggota kelompok; lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Baris KRS tempat nilai KKN dikonversi; lihat {@link #getDetailperkuliahan()}. */
	private Detailperkuliahan detailperkuliahan;
	/** Catatan bebas operator; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Ringkasan hasil/luaran KKN mahasiswa; lihat {@link #getHasil()}. */
	private String hasil;
	/** Nilai akhir angka hasil perata-rataan komponen; lihat {@link #getTotalNilai()}. */
	private Double totalNilai;
	/** Nilai huruf padanan {@link #totalNilai}; lihat {@link #getNilaiHuruf()}. */
	private String nilaiHuruf;
	/** Angka mutu untuk perhitungan IPK; lihat {@link #getTotalIP()}. */
	private Double totalIP = 0.0;
	/** Status lulus/tidak; lihat {@link #getLulus()}. */
	private Boolean lulus;

	/**
	 * Rincian nilai per komponen dalam satu string berformat khusus. Format lengkapnya dijelaskan
	 * pada Javadoc kelas; jangan diubah manual dari luar — pakai
	 * {@link #populateDetailNilai(KomponenPenilaianKkn, Double, Boolean)}.
	 */
	private String detailNilai = "";
	/** Gerbang persetujuan keanggotaan; lihat {@link #getDiterima()}. */
	private Boolean diterima;

	/**
	 * Menulis snapshot JSON baris ini ke berkas cache sementara.
	 *
	 * <p>Meneruskan ke {@code GeneralValueObject.write(String...)} dengan daftar nama kelas yang
	 * <b>dikecualikan</b> dari penelusuran relasi saat serialisasi ({@link Jurusan}, {@link Dosen},
	 * kelas ini sendiri, {@link Pegawai}, {@link Fakultas}, {@link PerguruanTinggi},
	 * {@link LembagaPengangkat}, {@link TingkatKesulitanMatakuliah}, {@link Kurikulum},
	 * {@link MasaPerkuliahan}, {@link JamPerkuliahan}, {@link JenisEvaluasi}, {@link Ruang}).
	 * Pengecualian ini yang menahan serialisasi agar tidak merambat ke separuh basis data lewat
	 * relasi {@link #getMahasiswa()}.</p>
	 *
	 * <p>Dipanggil dari {@code KelompokKkn.populateMahasiswaDapatKelompokKkn(...)} saat kelompok
	 * membangun indeks berkas anggotanya. Sesuai kontrak induk, nilai balik yang tidak {@code null}
	 * <b>bukan jaminan</b> berkas benar-benar ditulis — bila penulisan dilewati, yang dikembalikan
	 * hanyalah penunjuk lokasi.</p>
	 *
	 * @return berkas cache hasil penulisan, atau berkas penunjuk lokasi bila penulisan dilewati
	 * @see ais.database.model.GeneralValueObject#write(String...)
	 */
	public File write() {
		File f = write(Jurusan.class.getName(), Dosen.class.getName(), MahasiswaDapatKelompokKkn.class.getName(),
				Pegawai.class.getName(), Fakultas.class.getName(), PerguruanTinggi.class.getName(),
				LembagaPengangkat.class.getName(), TingkatKesulitanMatakuliah.class.getName(),
				Kurikulum.class.getName(), MasaPerkuliahan.class.getName(), JamPerkuliahan.class.getName(),
				JenisEvaluasi.class.getName(), Ruang.class.getName());
		return f;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/ZK.
	 *
	 * <p>Dipakai juga oleh kode aplikasi saat mahasiswa memilih kelompok untuk pertama kali
	 * ({@code KknUntukMahasiswaAction}) maupun saat operator menempatkan mahasiswa
	 * ({@code AmbilDataMahasiswaKelompokKknHelper}). Setelah dibuat, minimal
	 * {@link #setMahasiswa(Mahasiswa)} dan {@link #setKelompokKkn(KelompokKkn)} harus diisi karena
	 * kedua kolom FK-nya {@code nullable = false}.</p>
	 */
	public MahasiswaDapatKelompokKkn() {
	}

	/**
	 * Kunci utama baris keanggotaan ini ({@code IDENTITY}, dibangkitkan basis data).
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
	 * Menetapkan kunci utama. Umumnya hanya dipakai Hibernate.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Catatan bebas operator atas keanggotaan ini (mis. alasan pemindahan kelompok).
	 *
	 * <p>Dapat disunting dari daftar anggota di {@code KelompokKknHelper} dan ikut diekspor pada
	 * cetak data {@code KelompokKknAction}.</p>
	 *
	 * @return isi keterangan; boleh {@code null}
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas operator.
	 *
	 * @param keterangan isi keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan kelompok KKN yang diikuti mahasiswa ini.
	 *
	 * @param kelompokKkn kelompok tujuan; wajib terisi sebelum penyimpanan karena kolomnya
	 *                    {@code nullable = false}
	 */
	public void setKelompokKkn(KelompokKkn kelompokKkn) {
		this.kelompokKkn = kelompokKkn;
	}

	/**
	 * Kelompok KKN yang diikuti mahasiswa ini — sekaligus satu-satunya jalan menuju gelaran
	 * {@link ais.database.model.Kkn} ({@code getKelompokKkn().getKkn()}).
	 *
	 * <p>Relasi {@code @ManyToOne} lazy; getter menjalankan pola standar repo ini, yaitu
	 * meresolusi proxy lewat {@code check(...)} lalu <b>menugaskannya kembali ke field</b>.
	 * Penugasan ulang itu wajib (lihat penjelasan lengkap pola tersebut di
	 * {@link ais.database.model.GeneralValueObject#check(Object)}) dan berarti pemanggilan getter
	 * bisa memicu query tambahan.</p>
	 *
	 * @return kelompok KKN terkait
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kkn", nullable = false)
	public KelompokKkn getKelompokKkn() {
		kelompokKkn = check(kelompokKkn);
		return kelompokKkn;
	}

	/**
	 * Menetapkan mahasiswa anggota kelompok.
	 *
	 * @param mahasiswa mahasiswa yang bersangkutan; wajib terisi sebelum penyimpanan karena
	 *                  kolomnya {@code nullable = false}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mahasiswa pemilik keanggotaan ini.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy dengan pola {@code check(...)} + penugasan ulang yang sama
	 * seperti {@link #getKelompokKkn()}.</p>
	 *
	 * @return mahasiswa terkait
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Nilai akhir angka KKN mahasiswa ini.
	 *
	 * <p>Diisi pemanggil dari hasil {@link #hitungTotalNilai(Boolean)}; getter hanya menormalkan
	 * {@code null} menjadi {@code 0.0} dan <b>tidak</b> menulis balik hasil normalisasi ke field,
	 * sehingga kolom di basis data tetap {@code NULL} selama belum pernah dinilai.</p>
	 *
	 * @return nilai akhir, atau {@code 0.0} bila belum dinilai
	 */
	public Double getTotalNilai() {
		return totalNilai == null ? 0.0 : totalNilai;
	}

	/**
	 * Menetapkan nilai akhir angka.
	 *
	 * @param totalNilai nilai akhir; boleh {@code null}
	 */
	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	/**
	 * Nilai huruf padanan {@link #getTotalNilai()} (mis. {@code "A"}, {@code "B+"}).
	 *
	 * <p>Ditetapkan {@code PenilaianKknHelper} dari master {@code NilaiHuruf} yang cocok dengan
	 * angkatan/jurusan/fakultas/tahun akademik mahasiswa. Getter memangkas spasi tepi, namun
	 * mengembalikan {@code null} apa adanya bila memang belum pernah diisi.</p>
	 *
	 * @return nilai huruf yang sudah di-{@code trim()}, atau {@code null}
	 */
	public String getNilaiHuruf() {
		return this.nilaiHuruf == null ? null : this.nilaiHuruf.trim();
	}

	/**
	 * Menetapkan nilai huruf.
	 *
	 * @param nilaiHuruf nilai huruf; boleh {@code null}
	 */
	public void setNilaiHuruf(String nilaiHuruf) {
		this.nilaiHuruf = nilaiHuruf;
	}

	/**
	 * Menyusun ulang {@link #getDetailNilai()} dari komponen penilaian yang berlaku pada gelaran
	 * KKN kelompok ini, dengan nilai yang sudah ada dipertahankan.
	 *
	 * <p><b>PERINGATAN — method ini rusak dan saat ini tidak dipanggil dari mana pun.</b> Kriteria
	 * yang dibangunnya menyaring properti {@code parent}, {@code persen}, dan {@code statusPertemuan}
	 * pada {@link ais.database.model.kkn.KknPunyaKomponenPenilaianKkn}, padahal entity tersebut
	 * hanya punya {@code id}, {@code nama}, {@code keterangan}, {@code kkn}, dan
	 * {@code komponenPenilaianKkn} (ditambah field audit). Hibernate akan melempar
	 * {@code QueryException: could not resolve property: parent} begitu kriteria dieksekusi, dan
	 * karena pembangunan kriteria berada <b>di luar</b> blok {@code try}, exception itu merambat ke
	 * pemanggil, bukan tertangkap {@code Common.tampilErrorJikaAdmin}. Bentuknya identik dengan
	 * penyaringan yang sah pada modul perkuliahan ({@code PertemuanPunyaFormatNilai} memang punya
	 * {@code parent}/{@code persen}/{@code statusPertemuan}), jadi hampir pasti sisa salin-tempel.
	 * Jangan dipanggil sebelum kriterianya diperbaiki; padanan yang benar-benar dipakai adalah
	 * {@link #bersihkanNilaiKeDefault()} dan {@link #refreshNilaiKeDefault()}.</p>
	 *
	 * <p>Rancangan aslinya: memuat komponen penilaian aktif milik gelaran KKN, membaca nilai lama
	 * tiap komponen lewat {@link #retreiveDetailNilai(KomponenPenilaianKkn)} beserta status
	 * verifikasinya, lalu menimpa {@code detailNilai} dengan susunan baru. Komponen yang gagal
	 * diproses dilewati dan kesalahannya hanya ditampilkan kepada admin.</p>
	 *
	 * @param session session Hibernate aktif yang dipakai memuat daftar komponen penilaian
	 * @see #refreshNilaiKeDefault()
	 * @see #bersihkanNilaiKeDefault(List)
	 */
	@SuppressWarnings("unchecked")
	public void reloadKknPunyaKomponenPenilaianKkn(Session session) {

		refreshNilaiKeDefault();

		String formatbaru = "";
		List<KknPunyaKomponenPenilaianKkn> kknPunyaKomponenPenilaianKkns = session
				.createCriteria(KknPunyaKomponenPenilaianKkn.class).add(Restrictions.eq("kkn", kelompokKkn.getKkn()))
				.add(Restrictions.isNull("parent")).add(Restrictions.gt("persen", 0.01))
				.createCriteria("statusPertemuan").add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).addOrder(Order.asc("id")).list();
		for (KknPunyaKomponenPenilaianKkn kknPunyaKomponenPenilaianKkn : kknPunyaKomponenPenilaianKkns) {
			try {
				Double jumlah = retreiveDetailNilai(kknPunyaKomponenPenilaianKkn.getKomponenPenilaianKkn());
				Boolean verivy = retreiveDetailVerifikasiNilai(kknPunyaKomponenPenilaianKkn);
				String aformatBaru = kknPunyaKomponenPenilaianKkn.getKomponenPenilaianKkn().getId() + "," + jumlah
						+ ",0," + kknPunyaKomponenPenilaianKkn.getKomponenPenilaianKkn().getBobot() + "," + verivy;
				formatbaru += formatbaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		detailNilai = formatbaru;
		// System.out.println("detailNilai baru => " + detailNilai);
	}

	/**
	 * Pintasan {@link #hitungTotalNilai(Boolean, List)} tanpa daftar komponen yang disiapkan
	 * pemanggil.
	 *
	 * <p>Bila {@code gunakanKknPunyaKomponenPenilaianKknDariDatabase} bernilai {@code true}, daftar
	 * komponen aktif akan dimuat sendiri dari basis data lewat {@link #bersihkanNilaiKeDefault()}.
	 * Ini bentuk yang dipakai {@code PenilaianKknHelper} setiap kali dosen mengubah satu nilai.</p>
	 *
	 * @param gunakanKknPunyaKomponenPenilaianKknDariDatabase {@code true} untuk membuang dulu entri
	 *                                                        nilai milik komponen yang sudah tidak
	 *                                                        aktif/tidak terpasang pada gelaran ini
	 * @return nilai akhir hasil rata-rata terbobot
	 * @see #hitungTotalNilai(Boolean, List)
	 */
	public Double hitungTotalNilai(Boolean gunakanKknPunyaKomponenPenilaianKknDariDatabase) {
		return hitungTotalNilai(gunakanKknPunyaKomponenPenilaianKknDariDatabase, null);
	}

	/**
	 * Menghitung nilai akhir KKN sebagai <b>rata-rata terbobot ternormalisasi</b> atas seluruh entri
	 * pada {@link #getDetailNilai()}.
	 *
	 * <p>Urutan kerjanya:</p>
	 *
	 * <ol>
	 *   <li>{@link #refreshNilaiKeDefault()} — memulihkan {@code detailNilai} bila kosong padahal
	 *   nilai akhir lama sudah terisi.</li>
	 *   <li>Bila {@code gunakanKknPunyaKomponenPenilaianKknDariDatabase} {@code true}: membuang
	 *   entri milik komponen yang tidak lagi berlaku, memakai
	 *   {@link #bersihkanNilaiKeDefault(List)} bila {@code kknPunyaKomponenPenilaianKkns} diberikan,
	 *   atau {@link #bersihkanNilaiKeDefault()} (yang memuat sendiri daftarnya dari basis data)
	 *   bila {@code null}.</li>
	 *   <li>Mengurai tiap entri menjadi pasangan (nilai, bobot). Entri yang id-nya bukan angka
	 *   dilewati; entri yang <b>bobotnya</b> tidak terbaca sebagai angka juga dilewati seluruhnya —
	 *   nilainya tidak ikut dihitung sama sekali. Bila kolom nilainya yang rusak/kosong, nilai
	 *   dianggap {@code 0.0} tetapi bobotnya <b>tetap</b> menambah pembagi, sehingga komponen yang
	 *   belum dinilai menarik turun nilai akhir.</li>
	 *   <li>Menjumlahkan {@code nilai * (bobot / totalBobot)}. Karena bobot dibagi dengan total
	 *   bobot yang benar-benar hadir, jumlah bobot tidak harus 100.</li>
	 * </ol>
	 *
	 * <p>Id yang muncul lebih dari sekali pada {@code detailNilai} <b>tidak</b> menggandakan bobot:
	 * pasangan disimpan ke {@link Map} sehingga entri terakhir menimpa yang sebelumnya, tetapi
	 * variabel pembagi {@code totalPersen} tetap menjumlahkan setiap kemunculan — duplikat karena
	 * itu membuat nilai akhir lebih kecil dari seharusnya. Pembersihan pada langkah 2 memang
	 * membuang duplikat, jadi jalur {@code true} tidak terkena.</p>
	 *
	 * <p>Method ini <b>tidak</b> menyimpan hasilnya; pemanggil sendiri yang meneruskan hasil ke
	 * {@link #setTotalNilai(Double)}, mencari padanan nilai hurufnya, lalu menyalin semuanya ke
	 * {@link #getDetailperkuliahan()} agar muncul di KRS/transkrip.</p>
	 *
	 * @param gunakanKknPunyaKomponenPenilaianKknDariDatabase {@code true} untuk membersihkan dulu
	 *                                                        entri komponen yang tak berlaku
	 * @param kknPunyaKomponenPenilaianKkns                   daftar komponen yang berlaku; boleh
	 *                                                        {@code null} agar dimuat dari basis
	 *                                                        data
	 * @return nilai akhir; {@code 0.0} bila belum ada entri nilai atau total bobotnya nol
	 */
	public Double hitungTotalNilai(Boolean gunakanKknPunyaKomponenPenilaianKknDariDatabase,
			List<KknPunyaKomponenPenilaianKkn> kknPunyaKomponenPenilaianKkns) {

		refreshNilaiKeDefault();
		if (gunakanKknPunyaKomponenPenilaianKknDariDatabase) {
			if (kknPunyaKomponenPenilaianKkns == null) {
				bersihkanNilaiKeDefault();
			} else {
				bersihkanNilaiKeDefault(kknPunyaKomponenPenilaianKkns);
			}
		}

		Double total = 0.0;
		String str = getDetailNilai();
		Double totalPersen = 0.0;

		if (str != null && !str.trim().isEmpty()) {
			String[] s = StringUtils.split(str, ";");
			Map<Long, Object[]> nilais = new HashMap<Long, Object[]>();
			for (String ss : s) {
				try {
					String[] sss = StringUtils.split(ss, ",");
					// Guard token kosong/bukan angka (lihat retreiveDetailNilai): baris rusak
					// dilewati diam-diam, tidak lagi memunculkan NumberFormatException yang
					// ditampilkan ke admin lewat tampilErrorJikaAdmin.
					if (sss == null || sss.length == 0 || sss[0] == null || !Common.isNumber(sss[0].trim())) {
						continue;
					}
					Long idKknPunyaKomponenPenilaianKkn = Long.parseLong(sss[0].trim());
					Double persen = sss.length > 3 && sss[3] != null && Common.isNumber(sss[3].trim())
							? Double.parseDouble(sss[3].trim()) : null;
					if (persen != null) {
						Double n = sss.length > 1 && sss[1] != null && Common.isNumber(sss[1].trim())
								? Double.parseDouble(sss[1].trim()) : 0.0;
						nilais.put(idKknPunyaKomponenPenilaianKkn, new Object[] { n, persen });

						totalPersen += persen;

					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (totalPersen > 0.001) {
				for (Long kknPunyaKomponenPenilaianKkn : nilais.keySet()) {
					try {
						Double n = (Double) nilais.get(kknPunyaKomponenPenilaianKkn)[0];
						Double persen = (Double) nilais.get(kknPunyaKomponenPenilaianKkn)[1];
						total += (n * (persen / totalPersen));
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		}

		return total;
	}

	/**
	 * Menyimpan (menambah atau memperbarui) nilai satu komponen penilaian ke dalam
	 * {@link #getDetailNilai()}.
	 *
	 * <p>Inilah satu-satunya jalur resmi untuk menulis nilai. Dipanggil {@code PenilaianKknHelper}
	 * pada event {@code onChange} setiap kotak nilai, biasanya langsung disusul
	 * {@code setTotalNilai(hitungTotalNilai(true))} dan {@code Common.refreshUpdate(...)}.</p>
	 *
	 * <p>Perilaku rinci:</p>
	 *
	 * <ul>
	 *   <li>{@code jumlah} di bawah {@code 0.01} memaksa {@code verify} menjadi {@code false} —
	 *   nilai nol/kosong tidak boleh berstatus terverifikasi.</li>
	 *   <li>{@code jumlah} {@code null} ditulis sebagai {@code 0.0}. Ini perbaikan atas bug lama:
	 *   dulu {@code null} ikut dirangkai apa adanya sehingga literal teks {@code "null"} tersimpan
	 *   di basis data dan menggagalkan {@code parseDouble} saat dibaca lagi.</li>
	 *   <li>Entri lama dengan id yang sama ditimpa; bila belum ada, entri baru ditambahkan di
	 *   akhir.</li>
	 *   <li><b>Efek samping penting:</b> entri lama yang id-nya bukan angka <b>dibuang</b> dari
	 *   string hasil — pembersihan ini disengaja agar baris korup tidak menumpuk.</li>
	 *   <li>{@code komponenPenilaianKkn} bernilai {@code null} membuat method tidak melakukan apa
	 *   pun.</li>
	 * </ul>
	 *
	 * <p>Perubahan hanya mengenai field {@code detailNilai} di memori; penyimpanan ke basis data
	 * tetap tanggung jawab pemanggil.</p>
	 *
	 * @param komponenPenilaianKkn komponen yang dinilai; {@code null} membuat pemanggilan diabaikan
	 * @param jumlah               nilai angka komponen; {@code null} diperlakukan sebagai {@code 0.0}
	 * @param verify               penanda nilai sudah diverifikasi; dipaksa {@code false} bila
	 *                             nilainya di bawah {@code 0.01}
	 */
	public void populateDetailNilai(KomponenPenilaianKkn komponenPenilaianKkn, Double jumlah, Boolean verify) {
		if (jumlah != null && jumlah < 0.01) {
			verify = false;
		}
		// KE-FIX (NumberFormatException "For input string: \"null\"" saat hitungTotalNilai()):
		// jumlah null (komponen belum dinilai) sebelumnya ikut ditulis mentah via string
		// concat ("," + jumlah + ",") sehingga literal teks "null" tersimpan di kolom
		// detailNilai, lalu gagal di-parseDouble saat dibaca kembali. Default-kan ke 0 di sini
		// supaya teks "null" tidak pernah tertulis ke DB.
		Double jumlahAman = jumlah == null ? Double.valueOf(0.0) : jumlah;
		if (komponenPenilaianKkn != null) {
			String formatBaru = "";
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = nn.split(",");
					// Guard diperluas: bukan hanya kosong, tapi juga token yang bukan angka
					// (lihat retreiveDetailNilai) supaya tidak jadi NumberFormatException.
					if (s.length > 0 && s[0] != null && Common.isNumber(s[0].trim())) {
						Long formatId = Long.parseLong(s[0].trim());
						if (komponenPenilaianKkn.getId().equals(formatId)) {
							aformatBaru = komponenPenilaianKkn.getId() + "," + jumlahAman + ",0,"
									+ komponenPenilaianKkn.getBobot() + "," + verify;
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!ada) {
				String aformatBaru = komponenPenilaianKkn.getId() + "," + jumlahAman + ",0,"
						+ komponenPenilaianKkn.getBobot() + "," + verify;
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			detailNilai = formatBaru;
		}

	}

	/**
	 * Membuang entri {@link #getDetailNilai()} yang komponennya sudah tidak berlaku, dengan daftar
	 * komponen yang berlaku dimuat sendiri dari basis data.
	 *
	 * <p>Daftar diambil dari {@link ais.database.model.kkn.KknPunyaKomponenPenilaianKkn} milik
	 * gelaran {@code kelompokKkn.getKkn()}, disaring pada komponen yang {@code aktif} bernilai
	 * {@code true} <b>atau</b> {@code null} (komponen lama yang belum pernah diisi flag-nya tetap
	 * dianggap aktif). Hasilnya diteruskan ke {@link #bersihkanNilaiKeDefault(List)}.</p>
	 *
	 * <p>Memakai {@code HibernateUtil.currentSession()} — session milik thread pemanggil — dan
	 * <b>tidak menutupnya</b>. Field {@code kelompokKkn} dibaca langsung tanpa melewati
	 * {@link #getKelompokKkn()}, jadi method ini mengandalkan relasi kelompok sudah terpasang;
	 * pada object yang belum lengkap hasilnya {@code NullPointerException}.</p>
	 *
	 * <p>Perhatikan bahwa penyaringan di sini memakai flag {@code aktif} milik
	 * {@code KomponenPenilaianKkn}, berbeda dengan penyaringan (yang rusak) di
	 * {@link #reloadKknPunyaKomponenPenilaianKkn(Session)}.</p>
	 *
	 * @see #bersihkanNilaiKeDefault(List)
	 */
	@SuppressWarnings("unchecked")
	public void bersihkanNilaiKeDefault() {
		Session session = HibernateUtil.currentSession();
		List<KknPunyaKomponenPenilaianKkn> kknPunyaKomponenPenilaianKkns = session
				.createCriteria(KknPunyaKomponenPenilaianKkn.class)
				.createAlias("komponenPenilaianKkn", "komponenPenilaianKkn")
				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianKkn.aktif"),
						Restrictions.eq("komponenPenilaianKkn.aktif", true)))
				.add(Restrictions.eq("kkn", kelompokKkn.getKkn())).list();
		bersihkanNilaiKeDefault(kknPunyaKomponenPenilaianKkns);
	}

	/**
	 * Menyaring {@link #getDetailNilai()} sehingga hanya menyisakan entri milik komponen yang ada
	 * pada daftar {@code kknPunyaKomponenPenilaianKkns}.
	 *
	 * <p>Selain menyaring, method ini juga <b>membuang duplikat</b> (id yang sudah pernah muncul
	 * dilewati) dan <b>membuang entri korup</b> (id bukan angka) — keduanya memang tugas
	 * pembersihan, bukan kesalahan. Urutan entri yang lolos tetap dipertahankan, dan isi tiap entri
	 * disalin apa adanya sehingga nilai serta status verifikasi tidak berubah.</p>
	 *
	 * <p>Bila {@code detailNilai} kosong/{@code null}, method tidak melakukan apa pun — termasuk
	 * tidak menimpanya dengan string kosong. Kegagalan pada satu entri hanya dilaporkan ke admin dan
	 * tidak menggagalkan pembersihan entri lain.</p>
	 *
	 * <p>Perubahan hanya di memori; penyimpanan tetap urusan pemanggil.</p>
	 *
	 * @param kknPunyaKomponenPenilaianKkns daftar pemasangan komponen penilaian yang dianggap
	 *                                      berlaku; yang dibandingkan adalah id
	 *                                      {@code getKomponenPenilaianKkn().getId()} masing-masing
	 */
	public void bersihkanNilaiKeDefault(List<KknPunyaKomponenPenilaianKkn> kknPunyaKomponenPenilaianKkns) {
		String formatbaru = "";

		if (detailNilai != null && !detailNilai.trim().isEmpty()) {
			String[] s = StringUtils.split(detailNilai, ";");

			List<Long> ids = new ArrayList<Long>();
			for (KknPunyaKomponenPenilaianKkn kknPunyaKomponenPenilaianKkn : kknPunyaKomponenPenilaianKkns) {
				ids.add(kknPunyaKomponenPenilaianKkn.getKomponenPenilaianKkn().getId());
			}

			List<Long> idKknPunyaKomponenPenilaianKkns = new ArrayList<Long>();
			for (String ss : s) {
				try {
					String[] sss = StringUtils.split(ss, ",");
					// Guard token kosong/bukan angka (lihat retreiveDetailNilai): baris rusak
					// memang harus DIBUANG oleh proses pembersihan ini, bukan jadi exception.
					if (sss == null || sss.length == 0 || sss[0] == null || !Common.isNumber(sss[0].trim())) {
						continue;
					}
					Long idKknPunyaKomponenPenilaianKkn = Long.parseLong(sss[0].trim());
					if (!idKknPunyaKomponenPenilaianKkns.contains(idKknPunyaKomponenPenilaianKkn)) {
						idKknPunyaKomponenPenilaianKkns.add(idKknPunyaKomponenPenilaianKkn);
						if (ids.contains(idKknPunyaKomponenPenilaianKkn)) {
							formatbaru += formatbaru.isEmpty() ? ss : ";" + ss;
						}
					}
				} catch (Exception e) {
					// KE-FIX: satu entri detailNilai yang korup/tak terparse tidak boleh
					// menggagalkan pembersihan entri-entri lain yang valid.
					Common.tampilErrorJikaAdmin(e);
				}
			}

			detailNilai = formatbaru;
		}
	}

	/**
	 * Memulihkan {@link #getDetailNilai()} dari nilai akhir lama untuk data warisan yang belum
	 * pernah punya rincian per komponen.
	 *
	 * <p>Hanya bekerja bila <b>kedua</b> syarat terpenuhi: {@code detailNilai} kosong/{@code null}
	 * <b>dan</b> {@code totalNilai} lebih besar dari {@code 1.0}. Dalam kondisi itu seluruh komponen
	 * aktif gelaran ini dimuat dari basis data, lalu tiap komponen diisi dengan
	 * <b>nilai akhir yang sama persis</b> dan status verifikasi {@code false}. Dengan begitu, nilai
	 * akhir hasil {@link #hitungTotalNilai(Boolean)} tetap sama dengan nilai lama (rata-rata
	 * terbobot dari angka yang seragam) sementara layar penilaian per komponen tidak lagi kosong.
	 * Ini rekonstruksi, bukan data asli: rincian per komponen yang tampil bukan nilai yang pernah
	 * diberikan dosen.</p>
	 *
	 * <p><b>Efek samping:</b> method ini dipanggil di awal hampir semua method nilai — termasuk
	 * {@link #retreiveDetailNilai(KomponenPenilaianKkn)} dan
	 * {@link #retreiveDetailVerifikasiNilai(KknPunyaKomponenPenilaianKkn)} yang bersifat
	 * "baca saja" — sehingga membaca nilai satu komponen saja bisa memicu query dan menulis ulang
	 * {@code detailNilai}. Pada object yang masih melekat pada session, penulisan itu ikut ter-flush
	 * ke basis data oleh dirty-checking tanpa ada pemanggilan {@code save}/{@code update} eksplisit.</p>
	 *
	 * <p>Memakai {@code HibernateUtil.currentSession()} dan tidak menutupnya. Sama seperti
	 * {@link #bersihkanNilaiKeDefault()}, field {@code kelompokKkn} dibaca langsung tanpa
	 * {@code check(...)}.</p>
	 */
	@SuppressWarnings("unchecked")
	public void refreshNilaiKeDefault() {
		if ((detailNilai == null || detailNilai.trim().isEmpty()) && totalNilai != null && totalNilai > 1.0) {
			String formatbaru = "";
			Session session = HibernateUtil.currentSession();
			List<KknPunyaKomponenPenilaianKkn> kknPunyaKomponenPenilaianKkns = session
					.createCriteria(KknPunyaKomponenPenilaianKkn.class)
					.createAlias("komponenPenilaianKkn", "komponenPenilaianKkn")
					.add(Restrictions.or(Restrictions.isNull("komponenPenilaianKkn.aktif"),
							Restrictions.eq("komponenPenilaianKkn.aktif", true)))
					.add(Restrictions.eq("kkn", kelompokKkn.getKkn())).list();

			for (KknPunyaKomponenPenilaianKkn kknPunyaKomponenPenilaianKkn : kknPunyaKomponenPenilaianKkns) {

				String aformatBaru = kknPunyaKomponenPenilaianKkn.getKomponenPenilaianKkn().getId() + "," + totalNilai
						+ ",0," + kknPunyaKomponenPenilaianKkn.getKomponenPenilaianKkn().getBobot() + ",false";
				formatbaru += formatbaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			detailNilai = formatbaru;
		}
	}

	/**
	 * Membaca nilai satu komponen penilaian dari {@link #getDetailNilai()}.
	 *
	 * <p>Dipakai layar penilaian ({@code PenilaianKknHelper}) untuk mengisi kotak nilai tiap
	 * komponen, dan {@code SertifikatAction} untuk mencetak rincian nilai pada sertifikat.</p>
	 *
	 * <p>Diawali {@link #refreshNilaiKeDefault()}, sehingga <b>tidak bebas efek samping</b>
	 * meskipun namanya "retreive". Pencarian dilakukan linear atas entri {@code detailNilai};
	 * entri dengan id bukan angka dilewati, dan entri yang cocok tetapi kolom nilainya
	 * kosong/rusak/{@code "null"} menghasilkan {@code 0.0} alih-alih exception — perbaikan atas
	 * {@code NumberFormatException} yang dulu muncul untuk baris warisan.</p>
	 *
	 * @param formatIdSource komponen yang dicari nilainya; {@code null} atau tanpa id menghasilkan
	 *                       {@code 0.0}
	 * @return nilai komponen; {@code 0.0} bila komponen belum dinilai atau tidak ditemukan
	 */
	public Double retreiveDetailNilai(KomponenPenilaianKkn formatIdSource) {

		refreshNilaiKeDefault();

		if (formatIdSource != null && formatIdSource.getId() != null) {
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					// FIX NumberFormatException: For input string: "" -- satu segmen detailNilai
					// bisa kosong/rusak (mis. ada ";" ganda atau di ujung string, atau kolom id
					// kosong). Token kosong/bukan angka BUKAN error: berarti "tidak ada nilai",
					// jadi cukup dilewati. Idiom sama dengan parser sejenis yang sudah diperbaiki
					// di Detailperkuliahan.retreiveDetailNilai (guard Common.isNumber).
					if (s.length == 0 || s[0] == null || !Common.isNumber(s[0].trim())) {
						continue;
					}
					Long formatId = Long.parseLong(s[0].trim());
					if (formatIdSource.getId().equals(formatId)) {
						// Kolom nilai bisa hilang/kosong/"null" pada baris lama yang rusak --
						// anggap belum dinilai (0.0), jangan lempar exception.
						if (s.length <= 1 || s[1] == null || !Common.isNumber(s[1].trim())) {
							return 0.0;
						}
						return Double.parseDouble(s[1].trim());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaDapatKelompokKkn.java:375");

				}
			}
		}

		return 0.0;
	}

	/**
	 * Membaca status verifikasi nilai satu komponen penilaian dari {@link #getDetailNilai()}.
	 *
	 * <p>Bentuknya sejajar {@link #retreiveDetailNilai(KomponenPenilaianKkn)} — termasuk pemanggilan
	 * {@link #refreshNilaiKeDefault()} di awal — dengan dua perbedaan: parameternya adalah
	 * <b>pemasangan</b> komponen pada gelaran ({@link ais.database.model.kkn.KknPunyaKomponenPenilaianKkn})
	 * sementara yang dicocokkan tetap id {@code getKomponenPenilaianKkn().getId()} di dalamnya, dan
	 * hasilnya kolom ke-4 entri.</p>
	 *
	 * <p>Aturan pengembalian {@code false} (bukan exception) berlaku untuk: komponen tidak ditemukan,
	 * kolom nilai kosong/bukan angka, nilai kurang dari {@code 0.01} (nilai nol tidak mungkin
	 * terverifikasi), dan entri format lama yang belum punya kolom verifikasi.</p>
	 *
	 * <p><b>Catatan:</b> penjaga di awal memeriksa {@code formatIdSource.getId()}, sedangkan yang
	 * dipakai membandingkan adalah id komponen di dalamnya — object dengan {@code id} terisi tetapi
	 * {@code komponenPenilaianKkn} {@code null} akan menabrak {@code NullPointerException} yang lalu
	 * ditelan blok {@code catch} dan berujung {@code false}.</p>
	 *
	 * @param formatIdSource pemasangan komponen penilaian yang ditanyakan; {@code null} atau tanpa
	 *                       id menghasilkan {@code false}
	 * @return {@code true} hanya bila entri komponen ada, nilainya &ge; {@code 0.01}, dan kolom
	 *         verifikasinya berbunyi {@code "true"}
	 */
	public Boolean retreiveDetailVerifikasiNilai(KknPunyaKomponenPenilaianKkn formatIdSource) {

		refreshNilaiKeDefault();

		if (formatIdSource != null && formatIdSource.getId() != null) {
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					// Guard sama seperti retreiveDetailNilai di atas: segmen kosong/rusak
					// dilewati, bukan dijadikan NumberFormatException.
					if (s.length == 0 || s[0] == null || !Common.isNumber(s[0].trim())) {
						continue;
					}
					Long formatId = Long.parseLong(s[0].trim());
					if (formatIdSource.getKomponenPenilaianKkn().getId().equals(formatId)) {
						// Nilai kosong/bukan angka = belum dinilai -> otomatis belum terverifikasi.
						if (s.length <= 1 || s[1] == null || !Common.isNumber(s[1].trim())) {
							return false;
						}
						if (Double.parseDouble(s[1].trim()) < 0.01) {
							return false;
						}
						// Kolom verifikasi bisa tidak ada pada baris format lama -> anggap false
						// (perilaku sama dengan Boolean.parseBoolean untuk teks non-"true").
						if (s.length <= 4 || s[4] == null) {
							return false;
						}
						return Boolean.parseBoolean(s[4].trim());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaDapatKelompokKkn.java:400");

				}
			}
		}

		return false;
	}

	/**
	 * Angka mutu (bobot IPK) padanan {@link #getNilaiHuruf()}, mis. {@code 4.0} untuk {@code "A"}.
	 *
	 * <p>Diisi {@code PenilaianKknHelper} dari {@code NilaiHuruf.getNilaiDiIPK()} dan disalin ke
	 * {@link #getDetailperkuliahan()} agar KKN ikut menyumbang IPK. Getter menormalkan {@code null}
	 * menjadi {@code 0.0} tanpa menulis balik ke field.</p>
	 *
	 * @return angka mutu; {@code 0.0} bila belum dinilai
	 */
	public Double getTotalIP() {
		return totalIP == null ? 0.0 : totalIP;
	}

	/**
	 * Menetapkan angka mutu untuk perhitungan IPK.
	 *
	 * @param totalIP angka mutu; boleh {@code null}
	 */
	public void setTotalIP(Double totalIP) {
		this.totalIP = totalIP;
	}

	/**
	 * Status kelulusan KKN mahasiswa ini — <b>getter yang menghitung ulang dan menulis balik</b>.
	 *
	 * <p>Bukan pembaca field biasa. Urutan penentuannya:</p>
	 *
	 * <ol>
	 *   <li>{@code nilaiHuruf} dinormalkan lebih dulu ({@code nilaiHuruf = getNilaiHuruf()}),
	 *   sehingga spasi tepi pada field ikut terpangkas permanen.</li>
	 *   <li><b>Master Nilai Huruf diutamakan.</b> Bila ada konfigurasi yang cocok
	 *   ({@code ConstantValues.lulusDariNilaiHuruf(nilaiHuruf, getMahasiswa())}, prioritas
	 *   Jurusan &rarr; Fakultas &rarr; global), hasilnya <b>menimpa</b> field {@code lulus} bila
	 *   berbeda, lalu dikembalikan. Ini disengaja: status lulus yang tersimpan dan sudah basi
	 *   dikoreksi mengikuti konfigurasi terbaru. Perhatikan bahwa langkah ini memanggil
	 *   {@link #getMahasiswa()} sehingga dapat memicu resolusi proxy.</li>
	 *   <li>Bila tidak ada konfigurasi yang cocok dan {@code lulus} masih {@code null}, dipakai
	 *   aturan cadangan berbasis huruf: huruf kosong atau mengandung {@code D}, {@code E}, atau
	 *   {@code T} (huruf besar) berarti tidak lulus; selain itu lulus. Perhatikan aturan ini
	 *   berbasis "mengandung", jadi huruf gabungan seperti {@code "BD"} pun dianggap tidak
	 *   lulus.</li>
	 *   <li>Tanpa nilai huruf sama sekali, {@code lulus} dipaksa {@code false} — bahkan bila
	 *   sebelumnya sudah pernah diset {@code true}.</li>
	 * </ol>
	 *
	 * <p>Karena hasilnya ditulis ke field, memanggil getter ini pada object yang masih melekat pada
	 * session Hibernate dapat memicu {@code UPDATE} lewat dirty-checking. Kegagalan pembacaan
	 * konfigurasi ditelan (dicatat ke audit error) dan alur jatuh ke aturan cadangan.</p>
	 *
	 * @return {@code true} bila mahasiswa dinyatakan lulus KKN, {@code false} bila tidak
	 */
	public Boolean getLulus() {
		nilaiHuruf = getNilaiHuruf();
		// Utamakan KONFIGURASI Nilai Huruf yang DIPEROLEH (permintaan): status lulus mengikuti master
		// Nilai Huruf (ConstantValues.lulusDariNilaiHuruf) & mengoreksi nilai tersimpan yang basi.
		try {
			if (nilaiHuruf != null && !nilaiHuruf.trim().isEmpty()) {
				Boolean cfgLulus = ais.common.ConstantValues.lulusDariNilaiHuruf(nilaiHuruf, getMahasiswa());
				if (cfgLulus != null) {
					if (lulus == null || !lulus.equals(cfgLulus)) {
						lulus = cfgLulus;
					}
					return lulus;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/MahasiswaDapatKelompokKkn.java:431");
		}
		if (lulus == null && nilaiHuruf != null) {
			if (nilaiHuruf.isEmpty() || nilaiHuruf.toUpperCase().contains("D") || nilaiHuruf.toUpperCase().contains("E")
					|| nilaiHuruf.toUpperCase().contains("T")) {
				lulus = false;
			} else {
				lulus = true;
			}
		} else if (lulus == null) {
			lulus = true;
		}

		if (nilaiHuruf == null) {
			lulus = false;
		}

		return lulus;
	}

	/**
	 * Menetapkan status kelulusan secara eksplisit.
	 *
	 * <p>Ingat bahwa {@link #getLulus()} dapat menimpa nilai ini bila master Nilai Huruf menyatakan
	 * lain.</p>
	 *
	 * @param lulus status kelulusan; boleh {@code null} agar ditentukan otomatis
	 */
	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	/**
	 * Baris KRS ({@link ais.database.model.Detailperkuliahan}) tempat nilai KKN ini dikonversi.
	 *
	 * <p>Inilah jembatan agar KKN muncul di KRS/transkrip: setelah nilai dihitung,
	 * {@code PenilaianKknHelper} menyalin {@code totalNilai}, {@code totalIP}, {@code nilaiHuruf},
	 * dan {@code lulus} ke baris {@code Detailperkuliahan} ini. Bila masih {@code null}, helper
	 * mencoba menebaknya sendiri: mencari {@code Detailperkuliahan} milik mahasiswa yang sudah
	 * {@code DISETUJUI} dan <b>nama mata kuliahnya (atau mata kuliah konversinya) mengandung kata
	 * "kkn"</b> — pencocokan berbasis teks, bukan kode mata kuliah — lalu menyimpannya ke sini.
	 * Operator juga dapat memilih baris KRS secara manual.</p>
	 *
	 * <p>Berbeda dengan relasi lain di kelas ini, getter ini <b>tidak</b> memanggil {@code check(...)}
	 * dan relasinya tidak ditandai {@code LAZY} — dengan {@code @Fetch(FetchMode.SELECT)} baris KRS
	 * diambil lewat query terpisah saat entity dimuat.</p>
	 *
	 * @return baris KRS terkait, atau {@code null} bila nilai KKN belum dikonversi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detailperkuliahan_id", nullable = true)
	public Detailperkuliahan getDetailperkuliahan() {
		return detailperkuliahan;
	}

	/**
	 * Menetapkan baris KRS tujuan konversi nilai KKN.
	 *
	 * @param detailperkuliahan baris KRS; boleh {@code null}
	 */
	public void setDetailperkuliahan(Detailperkuliahan detailperkuliahan) {
		this.detailperkuliahan = detailperkuliahan;
	}

	/**
	 * Rincian nilai per komponen dalam satu string {@code text}; format lengkapnya dijelaskan pada
	 * Javadoc kelas.
	 *
	 * <p>Getter menormalkan {@code null} menjadi string kosong dan memangkas spasi tepi, tanpa
	 * menulis balik ke field. Jangan mengurai string ini di kode pemanggil — pakai
	 * {@link #retreiveDetailNilai(KomponenPenilaianKkn)} dan
	 * {@link #retreiveDetailVerifikasiNilai(KknPunyaKomponenPenilaianKkn)} yang sudah memuat semua
	 * penjaga untuk data warisan yang korup.</p>
	 *
	 * @return string rincian nilai; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getDetailNilai() {
		return detailNilai == null ? "" : detailNilai.trim();
	}

	/**
	 * Menimpa seluruh rincian nilai sekaligus.
	 *
	 * <p>Setter mentah tanpa validasi format; disediakan untuk Hibernate dan pemulihan data. Untuk
	 * mengubah nilai satu komponen pakai
	 * {@link #populateDetailNilai(KomponenPenilaianKkn, Double, Boolean)}.</p>
	 *
	 * @param detailNilai string rincian nilai berformat khusus; boleh {@code null}
	 */
	public void setDetailNilai(String detailNilai) {
		this.detailNilai = detailNilai;
	}

	/**
	 * Apakah keanggotaan ini sudah disetujui operator.
	 *
	 * <p>Gerbang utama entity ini. Selama {@code false}: mahasiswa masih boleh mengganti pilihan
	 * kelompoknya lewat portal, tombol hapus anggota masih tampil, dan tombol cetak sertifikat
	 * disembunyikan. Begitu {@code true}: portal mahasiswa hanya menampilkan status "DISETUJUI"
	 * beserta nama kelompok, dan baris ini ikut terhitung sebagai anggota resmi pada pengecekan
	 * kuota kelompok, rekap BKD dosen pembimbing, serta berbagai dasbor statistik.</p>
	 *
	 * <p>Flag ini <b>dua arah</b> — operator dapat mencentang maupun membatalkannya kembali dari
	 * daftar anggota di {@code KelompokKknHelper}. Getter menormalkan {@code null} menjadi
	 * {@code false} tanpa menulis balik, sehingga baris lama yang kolomnya {@code NULL} tetap
	 * dianggap belum disetujui. Perhatikan bahwa query pencarian menyaring langsung ke kolom
	 * ({@code Restrictions.eq("diterima", false)}) sehingga baris ber-{@code NULL} tidak akan
	 * terjaring oleh filter "belum diterima" — ketaksesuaian kecil antara perilaku getter dan
	 * perilaku query.</p>
	 *
	 * <p>Blok yang dikomentari di dalam method adalah rancangan lama: dulu kelompok yang tidak
	 * membolehkan mahasiswa memilih sendiri ({@code KelompokKkn.getMahasiswaBisaMemilih()}) otomatis
	 * dianggap diterima. Persetujuan kini selalu eksplisit.</p>
	 *
	 * @return {@code true} bila keanggotaan sudah disetujui; {@code false} bila belum atau kolomnya
	 *         masih {@code NULL}
	 */
	public Boolean getDiterima() {
		// if (kelompokKkn != null && !kelompokKkn.getMahasiswaBisaMemilih()) {
		// diterima = true;
		// }
		return diterima == null ? false : diterima;
	}

	/**
	 * Menyetujui atau membatalkan persetujuan keanggotaan ini.
	 *
	 * @param diterima {@code true} untuk menyetujui, {@code false} untuk mengembalikan ke status
	 *                 belum disetujui
	 */
	public void setDiterima(Boolean diterima) {
		this.diterima = diterima;
	}

	/**
	 * Nama para dosen pembimbing kelompok — <b>kolom turunan yang dihitung ulang setiap dibaca</b>.
	 *
	 * <p>Bila relasi {@code kelompokKkn} terisi, isinya dibangun ulang dari
	 * {@code KelompokKkn.populateDosenBuNama()} (mengumpulkan dosen pembimbing 1..10 yang tidak
	 * {@code null}), diambil bentuk {@code toString()} daftarnya, lalu tanda kurung siku
	 * {@code "["} dan {@code "]"} dibuang sehingga tersisa daftar nama dipisah koma. Hasilnya
	 * <b>ditulis balik ke field</b>, jadi getter ini ikut mengotori object bagi dirty-checking
	 * Hibernate — dan karena tidak ada {@code @Transient}, nilainya benar-benar tersimpan sebagai
	 * kolom.</p>
	 *
	 * <p>Kolom tersimpan itu memang dimanfaatkan: {@code KelompokKknAction} memakai
	 * {@code "namaDosen"} sebagai salah satu kolom ekspor daftar anggota. Konsekuensinya, nilai di
	 * basis data adalah <b>salinan</b> yang baru ikut berubah kalau baris ini kebetulan dibaca lagi
	 * setelah susunan pembimbing kelompok diubah.</p>
	 *
	 * <p>Perhatikan field {@code kelompokKkn} dibaca langsung (bukan lewat {@link #getKelompokKkn()}),
	 * jadi tidak ada resolusi {@code check(...)} di sini; bila relasinya {@code null}, nilai lama
	 * dikembalikan apa adanya.</p>
	 *
	 * @return daftar nama dosen pembimbing dipisah koma, atau nilai tersimpan terakhir bila kelompok
	 *         belum terpasang
	 */
	public String getNamaDosen() {
		if (kelompokKkn != null) {
			namaDosen = org.apache.commons.lang3.StringUtils.replace(
					org.apache.commons.lang3.StringUtils.replace(kelompokKkn.populateDosenBuNama().toString(), "]", ""),
					"[", "");
		}
		return namaDosen;
	}

	/**
	 * Menetapkan nama dosen pembimbing tersimpan.
	 *
	 * <p>Nyaris tak berguna dipanggil dari luar karena {@link #getNamaDosen()} menimpanya kembali
	 * begitu relasi kelompok terisi; disediakan untuk Hibernate dan jalur impor data.</p>
	 *
	 * @param namaDosen daftar nama dosen pembimbing
	 */
	public void setNamaDosen(String namaDosen) {
		this.namaDosen = namaDosen;
	}

	/**
	 * Implementasi {@link ais.database.model.VOPesertaPembelajaran}: objek pembelajaran yang diikuti
	 * peserta ini, yaitu kelompok KKN-nya.
	 *
	 * <p>Lewat antarmuka ini kode generik pembelajaran (pertemuan, absensi, diskusi, e-learning)
	 * dapat memperlakukan anggota KKN sama seperti peserta perkuliahan biasa.</p>
	 *
	 * <p><b>Catatan:</b> yang dikembalikan adalah field {@code kelompokKkn} apa adanya — tanpa
	 * {@code check(...)} seperti pada {@link #getKelompokKkn()} — sehingga pemanggil bisa menerima
	 * proxy lazy yang belum terinisialisasi, atau {@code null} pada object yang belum lengkap.</p>
	 *
	 * @return kelompok KKN yang diikuti, sebagai {@link ais.database.model.VOPembelajaran}
	 */
	@Override
	public VOPembelajaran ambilVOPembelajaran() {
		// TODO Auto-generated method stub
		return kelompokKkn;
	}

	/**
	 * Ringkasan hasil/luaran KKN mahasiswa ini (kolom {@code text}).
	 *
	 * <p>Diisi bebas oleh dosen/operator dari daftar anggota di {@code KelompokKknHelper} dan ikut
	 * dicetak pada ekspor {@code KelompokKknAction}. Getter menormalkan {@code null} menjadi string
	 * kosong dan memangkas spasi tepi — penting karena nilainya langsung dipakai membuat
	 * {@code Label}/{@code Textbox} ZK yang tidak menerima {@code null}.</p>
	 *
	 * @return isi hasil; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getHasil() {
		return hasil == null ? "" : hasil.trim();
	}

	/**
	 * Menetapkan ringkasan hasil/luaran KKN.
	 *
	 * @param hasil isi hasil; boleh {@code null}
	 */
	public void setHasil(String hasil) {
		this.hasil = hasil;
	}
}
