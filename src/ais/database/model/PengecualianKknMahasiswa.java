package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

/**
 * Entity <b>daftar putih (whitelist) dispensasi syarat KKN</b> pada tabel
 * {@code public.pengecualian_kkn_mahasiswa}. Satu baris berarti: <i>mahasiswa X dibebaskan dari
 * SELURUH syarat kelayakan akademik gelaran KKN Y</i>. Isinya hanya tiga muatan bisnis —
 * {@link #getMahasiswa() mahasiswa}, {@link #getKkn() kkn}, dan {@link #getKeterangan() keterangan}
 * (alasan bebas-teks) — ditambah empat kolom audit bawaan.
 *
 * <h3>Satu-satunya pembaca, dan seberapa kuat efeknya</h3>
 * <p>Tabel ini <b>hanya dibaca di satu tempat di seluruh basis kode</b>:
 * {@code ais.common.Common.checkSyaratKkn(Mahasiswa, Kkn)}. Query-nya adalah baris pertama method
 * tersebut, sebuah {@code rowCount} sederhana:</p>
 * <pre>
 * createCriteria(PengecualianKknMahasiswa.class)
 *     .add(Restrictions.eq("kkn", kkn))
 *     .add(Restrictions.eq("mahasiswa", mahasiswa))
 *     .setProjection(Projections.rowCount())      // &gt; 0  →  return true
 * </pre>
 * <p>Karena pemeriksaan itu berada <b>paling depan</b> dan langsung {@code return true}, satu baris
 * di sini mem-<i>bypass</i> sekaligus <b>empat</b> gerbang berikutnya di {@code checkSyaratKkn}:</p>
 * <ol>
 *   <li>kecocokan {@link Kkn#getJurusan()} dengan jurusan mahasiswa;</li>
 *   <li>kecocokan {@link Kkn#getFakultas()} dengan fakultas jurusan mahasiswa;</li>
 *   <li>pasangan syarat pertama {@link Kkn#getMinimalSksBolehIkutKkn()} <b>DAN</b>
 *   {@link Kkn#getMinimalIpkBolehIkutKkn()};</li>
 *   <li>pasangan syarat kedua {@link Kkn#getMinimalSksBolehIkutKkn2()} <b>DAN</b>
 *   {@link Kkn#getMinimalIpkBolehIkutKkn2()} yang aktif bila
 *   {@link Kkn#getAktifkanSyaratLain()}.</li>
 * </ol>
 * <p>Efek sampingnya juga: seluruh perhitungan berat yang biasanya dijalankan
 * {@code checkSyaratKkn} — {@code Common.getSemester(...)} dan terutama
 * {@code Common.singkronkanKrsMahasiswa(...)} — <b>tidak pernah dieksekusi</b> untuk mahasiswa yang
 * dikecualikan. Jadi baris di sini bukan sekadar "melonggarkan ambang", melainkan memutus jalur
 * evaluasi sepenuhnya.</p>
 *
 * <h3>Apa yang TIDAK dibebaskan</h3>
 * <p>Dispensasi ini <b>tidak</b> menyentuh syarat pembayaran. Di
 * {@code ais.action.master.kkn.KknUntukMahasiswaAction} pemeriksaan
 * {@link Kkn#getNimMhsTanpaBiaya()}, {@link Kkn#getKodeItemBiaya()}, dan {@link Kkn#getHarusBayar()}
 * dijalankan <b>sebelum</b> {@code checkSyaratKkn(...)} dipanggil, sehingga tetap berlaku penuh.
 * Pembebasan biaya punya mekanismenya sendiri ({@code nimMhsTanpaBiaya} pada {@link Kkn} dan
 * {@link BaypassPembayaranMahasiswa}). Demikian pula kelengkapan berkas
 * {@code PersyaratanKkn}/{@code MahasiswaKknPersyaratan} dan konfigurasi
 * {@code jika_sudah_dapat_kkn_mahasiswa_tidak_boleh_mengajukan_kkn} — keduanya diperiksa
 * <i>sesudah</i> {@code checkSyaratKkn(...)} dan tidak ikut dilewati.</p>
 *
 * <h3>Tiga jalur yang terdampak</h3>
 * <p>Gerbang {@code checkSyaratKkn(...)} — dan karenanya daftar putih ini — dipakai tiga layar:</p>
 * <ul>
 *   <li>{@code KknUntukMahasiswaAction} — tombol Simpan pendaftaran mandiri oleh mahasiswa;</li>
 *   <li>{@code ais.action.master.helper.AmbilDataMahasiswaKknHelper} — penambahan
 *   <b>peserta</b> massal ({@code MahasiswaDapatKkn}) oleh operator;</li>
 *   <li>{@code ais.action.master.helper.AmbilDataMahasiswaSeleksiKknHelper} — penambahan
 *   <b>pendaftar</b> massal ({@code MahasiswaDaftarKkn}) oleh operator.</li>
 * </ul>
 *
 * <h3>Dari mana baris ini dibuat</h3>
 * <p>Hanya satu layar yang menulis tabel ini:
 * {@code ais.action.master.helper.PengecualianKknMahasiswaHelper}, dibuka lewat tombol
 * <i>"Pengecualian"</i> pada toolbar {@code ais.action.master.helper.PendaftarKknHelper} (layar
 * Seleksi Penerima KKN, {@code ais.action.master.kkn.SeleksiPenerimaKknAction}). Alurnya:
 * operator menekan "Ambil Data Mahasiswa" → dialog pemilihan massal
 * {@code ais.action.master.helper.generic.AmbilDataMahasiswaBanyak} → setiap mahasiswa tercentang
 * menghasilkan satu baris baru dengan {@code keterangan} berisi string kosong. Keterangan lalu
 * dapat diedit langsung di grid (tersimpan seketika pada event {@code onChange}), dan setiap baris
 * punya tombol Hapus.</p>
 * <p>Tidak ada jalur tulis lain: tidak ada API servlet, tidak ada importir Excel, tidak ada
 * penulisan dari {@code Common} maupun dari modul lain. Baris ini juga tidak pernah dibuat otomatis
 * oleh sistem.</p>
 *
 * <h3>Catatan otorisasi bagi pemakai kelas ini</h3>
 * <p>Didokumentasikan apa adanya, tanpa perubahan kode, karena berpengaruh pada cara baris di tabel
 * ini boleh dipercaya:</p>
 * <ul>
 *   <li><b>Tidak ada alur persetujuan.</b> Skema tabel sama sekali tidak punya kolom status,
 *   penyetuju, tanggal berlaku, maupun masa berlaku — hanya {@code mahasiswa}, {@code kkn}, dan
 *   {@code keterangan}. Baris langsung berlaku sejak {@code session.save(...)} dijalankan.</li>
 *   <li><b>Tombol pembukanya dijaga UI saja.</b> Di {@code PendaftarKknHelper} tombol
 *   "Pengecualian" hanya disembunyikan lewat
 *   {@code setVisible(tbmuser.getMahasiswa() == null &amp;&amp; Common.bolehKonfigurasi(
 *   "tampilkan_pengecualian_kkn_mahasiswa_di_seleksi"))}. Konfigurasi itu didaftarkan
 *   {@code KonfigurasiNewAction} dengan bawaan {@code Konfigurasi.AKTIF}, jadi menyala secara
 *   bawaan.</li>
 *   <li><b>Tidak ada pemeriksaan hak akses server-side.</b> {@code PengecualianKknMahasiswaHelper}
 *   tidak mengimpor {@code CommonPrivilages} sama sekali: {@code display()}, penambahan massal,
 *   penyuntingan {@code keterangan}, dan tombol Hapus semuanya berjalan tanpa
 *   {@code checkPrevilages(CREATE/UPDATE/DELETE)}. Satu-satunya hak akses yang dibaca layar
 *   induknya ({@code SeleksiPenerimaKknAction}) adalah {@code CommonPrivilages.APPROVE}, dan itu
 *   hanya mengatur bisa-tidaknya checkbox "Terima" pada daftar pendaftar diedit — sama sekali tidak
 *   dipakai untuk pengecualian.</li>
 *   <li><b>Tidak ada batasan lingkup operator.</b> Dialog {@code AmbilDataMahasiswaBanyak} menyusun
 *   kriteria atas seluruh {@code Mahasiswa} aktif dengan penyaring teks nama/NIM/angkatan/prodi
 *   yang diketik sendiri oleh operator; tidak ada predikat yang mengikat hasil ke fakultas,
 *   jurusan, atau unit pengguna yang sedang login. Siapa pun yang bisa membuka layar ini dapat
 *   mengecualikan mahasiswa mana pun di seluruh institusi, termasuk mahasiswa di luar
 *   fakultas/jurusan gelaran KKN yang bersangkutan.</li>
 *   <li><b>Penambahan massal tanpa gerbang.</b> Satu kali interaksi memilih N mahasiswa akan
 *   menyimpan N baris pengecualian dalam satu perulangan {@code session.save(...)}, tanpa batas
 *   jumlah, tanpa konfirmasi, dan tanpa pemeriksaan otorisasi per baris.</li>
 *   <li><b>Jejak audit ada, tetapi hanya pada tabel.</b> {@code oleh}/{@code olehId} dan
 *   {@code tanggal_dirubah} diisi {@code AuditTimestampInterceptor} pada INSERT maupun UPDATE
 *   (interceptor terpasang di level {@code SessionFactory}), dan entity ini {@code @Audited}
 *   sehingga riwayatnya tersimpan di {@code pengecualian_kkn_mahasiswa_AUD}. Namun tidak ada satu
 *   pun layar yang menampilkan kolom-kolom itu — grid pengecualian hanya menampilkan NIM, nama,
 *   jurusan, fakultas, keterangan, dan tombol hapus.</li>
 * </ul>
 * <p>Pola yang sama (pembebasan berdampak besar, tanpa persetujuan, tanpa lingkup, hanya dijaga
 * {@code setVisible}) sudah tercatat pada {@link BaypassPembayaranMahasiswa}. Bedanya: yang di sana
 * berdampak finansial, yang di sini berdampak akademik.</p>
 *
 * <h3>Hubungan dengan jebakan konfigurasi "Aktifkan Syarat Lain"</h3>
 * <p>{@link Kkn} mendokumentasikan sebuah jebakan: pada baris KKN lama yang kolom syarat keduanya
 * masih {@code null}, {@link Kkn#getMinimalSksBolehIkutKkn2()} dan
 * {@link Kkn#getMinimalIpkBolehIkutKkn2()} jatuh ke {@code 0}/{@code 0.0}, sehingga mencentang
 * "Aktifkan Syarat Lain" tanpa mengisi angkanya membuat cabang kedua berbunyi
 * {@code sks >= 0 && ipk >= 0.0} — semua pendaftar lolos.</p>
 * <p><b>Kelas ini bukan penyebab, bukan pula obatnya.</b> Sudah diverifikasi dari kode: keduanya
 * hidup di method yang sama ({@code checkSyaratKkn}) tetapi tidak saling menyentuh — daftar putih
 * ini dievaluasi lebih dulu dan berhenti di {@code return true}, sedangkan jebakan syarat-2 baru
 * relevan justru ketika daftar putih <i>tidak</i> memuat mahasiswa tersebut. Hubungan keduanya
 * bersifat <b>saling menggantikan</b>: kelas ini adalah mekanisme resmi dan per-mahasiswa untuk
 * memberi dispensasi (jejaknya terekam siapa, kapan, dan alasannya), sedangkan jebakan syarat-2
 * memberi dispensasi <b>tanpa sengaja dan untuk semua orang sekaligus</b>. Karena keduanya
 * menghasilkan gejala yang serupa di layar ("mahasiswa yang seharusnya gagal ternyata lolos"),
 * penelusuran insiden wajib memeriksa <i>keduanya</i>: bila di tabel ini tidak ada baris untuk
 * mahasiswa yang bersangkutan, curigai konfigurasi syarat-2 pada baris {@code kkn}-nya.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ol>
 *   <li><b>Bayangan field audit</b> — {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}. Lihat catatan arsitektur di bawah.</li>
 *   <li><b>Muatan bisnis</b> — {@link #getMahasiswa()}, {@link #getKkn()},
 *   {@link #getKeterangan()} beserta setter-nya.</li>
 *   <li><b>Lain-lain</b> — konstruktor tanpa argumen dan {@link #toString()}.</li>
 * </ol>
 * <p>Tidak ada method bisnis, query statis, method utilitas, maupun konstanta domain di kelas ini;
 * seluruh anggotanya adalah field, konstruktor, {@code toString()}, callback JPA, atau pasangan
 * getter/setter properti.</p>
 *
 * <h3>Verifikasi pola berulang (diperiksa langsung pada kode kelas ini)</h3>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field: TIDAK ADA.</b> Kesembilan getter di kelas ini murni
 *   {@code return}; tidak satu pun menugaskan ulang field, mengganti {@code null} dengan nilai
 *   bawaan, atau menurunkan nilai dari properti lain. Ini membedakannya secara mencolok dari
 *   {@link Kkn} (14 getter penulis-balik) dan {@link BaypassPembayaranMahasiswa}.</li>
 *   <li><b>Getter yang menutup/membuka sesi Hibernate: TIDAK ADA.</b> Kelas ini tidak mengimpor
 *   {@code Session}, {@code HibernateUtil}, maupun {@code Common}, dan tidak menjalankan query apa
 *   pun.</li>
 *   <li><b>Getter destruktif: TIDAK ADA.</b> Tidak ada nilai yang dibuang, dikosongkan, atau
 *   ditimpa saat dibaca.</li>
 *   <li><b>{@code check(...)} tidak dipakai.</b> Berbeda dari kebanyakan entity sepaket,
 *   {@link #getMahasiswa()} dan {@link #getKkn()} mengembalikan field mentah tanpa melewatkan
 *   {@link GeneralValueObject#check(Object)}. Konsekuensinya proxy lazy tidak pernah
 *   dinormalkan/di-resolve oleh entity ini sendiri — lihat catatan pada masing-masing getter.</li>
 * </ul>
 *
 * <h3>Catatan arsitektur: field audit dideklarasikan ulang di sini</h3>
 * <p>Kelas ini {@code extends} {@link GeneralValueObject}, tetapi induk tersebut <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa sehingga Hibernate sama
 * sekali tidak memetakan propertinya. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di setiap entity, termasuk di sini. Ini
 * keharusan teknis arsitektur, bukan kelalaian atau duplikasi yang perlu "dirapikan". Kontrak umum
 * method warisan ({@code check}, {@code udah}, {@code ambilData}, dan kawan-kawan) didokumentasikan
 * lengkap di {@link GeneralValueObject}.</p>
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 *   <li><b>Komentar generator salah salin-tempel.</b> Blok komentar asli hasil {@code hbm2java}
 *   berbunyi "Bank generated by hbm2java" — sisa dari entity {@code Bank}. Tidak ada hubungan apa
 *   pun antara kelas ini dan perbankan; komentar itu digantikan dokumentasi ini.</li>
 *   <li><b>Kembaran persis di modul PKL.</b> {@link PengecualianPklMahasiswa} adalah salinan
 *   struktur kelas ini baris-per-baris — panjang berkas sama (125 baris), susunan anggota sama,
 *   bahkan {@code serialVersionUID}-nya <b>identik</b> ({@code 2463821577548439808L}). Pembacanya
 *   pun kembaran: {@code Common.checkSyaratPkl(...)}, yang oleh dokumentasinya sendiri disebut
 *   "copy semantis dari {@code checkSyaratKkn}". Perbaikan di sini hampir selalu perlu ditiru ke
 *   sana, dan sebaliknya.</li>
 *   <li><b>Pemetaan berbasis properti, dan tidak ada satu pun {@code @Transient}.</b> Karena
 *   {@code @Id} dipasang pada getter {@link #getId()}, Hibernate membaca SELURUH getter sebagai
 *   kolom. Untuk kelas ini konsekuensinya kecil (tidak ada getter yang memodifikasi nilai), tetapi
 *   aturan itu tetap berlaku bila kelak ada getter turunan yang ditambahkan.</li>
 *   <li><b>Kedua kolom relasi {@code nullable = true}.</b> Baik {@code mahasiswa} maupun
 *   {@code kkn} boleh kosong di tingkat skema. Baris dengan salah satu kolom {@code null} tidak
 *   akan pernah cocok dengan {@code Restrictions.eq(...)} di {@code checkSyaratKkn(...)} sehingga
 *   tidak berbahaya, tetapi akan membuat {@code PengecualianKknMahasiswaHelper} melempar
 *   {@code NullPointerException} saat me-render grid — renderer-nya langsung memanggil
 *   {@code mahasiswa.getNim()} tanpa penjagaan null.</li>
 *   <li><b>Tidak ada kunci unik (kkn, mahasiswa).</b> Menekan "Ambil Data Mahasiswa" dua kali untuk
 *   mahasiswa yang sama menghasilkan dua baris duplikat; pemanggilnya tidak melakukan pola "query
 *   dulu baru insert". Duplikat tidak mengubah perilaku ({@code rowCount &gt; 0} tetap terpenuhi),
 *   tetapi menghapus satu baris saja tidak akan mencabut dispensasinya — dispensasi baru hilang
 *   setelah baris terakhir dihapus.</li>
 *   <li><b>{@link #getKeterangan()} meniadakan jaminan non-null milik induk.</b>
 *   {@link GeneralValueObject#getKeterangan()} mengembalikan {@code ""} ketika field-nya
 *   {@code null}; override di sini mengembalikan field mentah sehingga {@code null} bisa lolos
 *   keluar. Karena {@link #toString()} langsung mengembalikan {@code keterangan}, {@code toString()}
 *   kelas ini <b>bisa mengembalikan {@code null}</b> — melanggar kontrak umum {@code toString()}.
 *   Baris yang dibuat lewat layar selalu diberi {@code ""} sehingga kasus ini hanya muncul pada
 *   baris yang dibuat di luar layar (mis. skrip SQL langsung).</li>
 *   <li><b>{@code cascade = PERSIST, MERGE} pada kedua relasi.</b> Menyimpan baris pengecualian
 *   akan ikut mem-persist/merge object {@link Mahasiswa} dan {@link Kkn} yang menempel padanya.
 *   Karena kedua object itu selalu berasal dari hasil query pada alur nyata, efeknya normalnya
 *   tidak terlihat; tetapi jangan menyetel {@link #setMahasiswa(Mahasiswa)} dengan instance
 *   setengah jadi.</li>
 *   <li><b>Sesi campur pada layar pengelolanya.</b> {@code PengecualianKknMahasiswaHelper.loadData}
 *   memuat baris memakai {@code Common.getManualSession()}, sedangkan penyuntingan
 *   {@code keterangan} dan penghapusan memakai {@code HibernateUtil.currentSession()}. Object yang
 *   sama karena itu berpindah sesi; ini pola yang sudah ada dan bukan sesuatu yang boleh
 *   "dibetulkan" tanpa menguji ulang seluruh layar.</li>
 * </ul>
 *
 * <p>Entity ini {@code @Audited} (Envers, tabel riwayat {@code pengecualian_kkn_mahasiswa_AUD}) dan
 * memakai {@code dynamicInsert}/{@code dynamicUpdate} sehingga hanya kolom yang benar-benar berubah
 * yang ikut dalam pernyataan SQL.</p>
 *
 * @see GeneralValueObject
 * @see Kkn
 * @see Mahasiswa
 * @see PengecualianPklMahasiswa
 * @see BaypassPembayaranMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pengecualian_kkn_mahasiswa")

public class PengecualianKknMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya <b>identik</b> dengan {@link PengecualianPklMahasiswa} —
	 * sisa salin-tempel dari generate {@code hbm2java} pada 2010. Jangan diubah agar object yang
	 * pernah diserialisasi (mis. ke dalam sesi ZK yang dipersistensi) tetap bisa dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code public.pengecualian_kkn_mahasiswa}; dibangkitkan basis data ({@code IDENTITY}). */
	private Long id;

	/** Nama pengguna terakhir yang menyimpan baris ini (bayangan field audit). */
	private String oleh;

	/** ID pengguna terakhir yang menyimpan baris ini (bayangan field audit). */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna yang terakhir menyimpan baris ini (bayangan field audit).
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir (bayangan field audit).
	 *
	 * <p><b>Setter defensif.</b> Nilai {@code null} atau yang hanya berisi spasi <b>diabaikan
	 * diam-diam</b> — field lama dipertahankan. Akibatnya jejak audit dapat tetap menunjuk pelaku
	 * sebelumnya bila konteks pengguna tidak tersedia saat penyimpanan (mis. proses latar). Pada
	 * alur normal nilai ini tidak disetel manual, melainkan ditulis langsung ke state Hibernate
	 * oleh {@code ais.database.hibernate.AuditTimestampInterceptor} pada INSERT maupun UPDATE,
	 * sehingga penjagaan di sini terlewati.</p>
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir (bayangan field audit).
	 *
	 * <p><b>Setter defensif</b> dengan perilaku identik {@link #setOlehId(String)}: nilai
	 * {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris ini (bayangan field audit).
	 *
	 * <p>Tidak ada layar yang menampilkan nilai ini; satu-satunya cara membacanya adalah query
	 * langsung ke tabel atau ke tabel riwayat Envers.</p>
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA yang dijalankan tepat sebelum baris ini di-UPDATE.
	 *
	 * <p>Mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}
	 * yang menyegarkan {@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)}, dan
	 * {@link #setOlehId(String)} dengan identitas pengguna yang sedang aktif. Tidak dipanggil pada
	 * INSERT — pengisian metadata saat INSERT ditangani {@code onSave} pada interceptor level
	 * {@code SessionFactory}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}

	/**
	 * Waktu penyimpanan terakhir baris ini (bayangan field audit). Diinisialisasi ke waktu server
	 * saat instance dibuat lewat {@code ais.ui.util.WaktuUtil.getDate()}, lalu diperbarui otomatis
	 * pada setiap simpan.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu penyimpanan terakhir (bayangan field audit).
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini <b>tidak</b>
	 * defensif: nilai {@code null} diterima apa adanya.</p>
	 *
	 * @param tanggal_dirubah waktu penyimpanan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu penyimpanan terakhir baris ini (bayangan field audit), dipetakan sebagai
	 * {@code TIMESTAMP}.
	 *
	 * @return waktu penyimpanan terakhir; tidak pernah {@code null} pada instance baru
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini: <b>hanya {@link #getKeterangan() keterangan}</b>, bukan
	 * identitas mahasiswa maupun gelaran KKN-nya.
	 *
	 * <p>Meng-override {@link GeneralValueObject#toString()} yang berformat {@code "kode - nama"}.
	 * Dua catatan penting:</p>
	 * <ul>
	 *   <li>Method ini membaca <b>field mentah</b> {@code keterangan}, bukan lewat
	 *   {@link #getKeterangan()}, sehingga <b>bisa mengembalikan {@code null}</b> — melanggar
	 *   kontrak umum {@code toString()} dan berpotensi memunculkan teks {@code "null"} atau NPE
	 *   pada komponen ZK yang menampilkannya.</li>
	 *   <li>Karena baris yang dibuat lewat layar selalu berisi {@code ""}, hasilnya praktis selalu
	 *   string kosong — tidak berguna untuk penelusuran log. Pakai
	 *   {@code getMahasiswa().getNim()} bila butuh identitas.</li>
	 * </ul>
	 *
	 * @return keterangan mentah baris ini; dapat {@code null}
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mahasiswa yang dikecualikan. Kolom {@code mahasiswa}, boleh {@code null} di tingkat skema.
	 */
	private Mahasiswa mahasiswa;

	/**
	 * Gelaran KKN tempat pengecualian ini berlaku. Kolom {@code kkn}, boleh {@code null} di tingkat
	 * skema. Pengecualian bersifat <b>per gelaran</b> — mahasiswa yang dikecualikan pada satu
	 * gelaran tetap harus memenuhi syarat pada gelaran lain.
	 */
	private Kkn kkn;

	/**
	 * Alasan dispensasi dalam bentuk teks bebas. Kolom {@code keterangan}, boleh {@code null}.
	 * Satu-satunya tempat operator dapat mencatat mengapa mahasiswa ini dibebaskan; tidak divalidasi
	 * dan tidak wajib diisi.
	 */
	private String keterangan;

	/**
	 * Konstruktor default tanpa argumen. WAJIB ada karena Hibernate membutuhkannya untuk membuat
	 * instance saat hidrasi entity dari hasil query; juga dipakai
	 * {@code PengecualianKknMahasiswaHelper} saat membuat baris pengecualian baru.
	 */
	public PengecualianKknMahasiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Anotasi {@code @Id} berada pada getter ini, yang menetapkan <b>property access</b> untuk
	 * seluruh kelas: Hibernate membaca setiap getter sebagai kolom.</p>
	 *
	 * @return kunci utama; {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Praktis tidak pernah dipanggil kode aplikasi — nilainya
	 * dibangkitkan basis data ({@code IDENTITY}) dan diisi Hibernate.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan alasan dispensasi (teks bebas).
	 *
	 * <p><b>Perhatian:</b> method ini meng-override {@link GeneralValueObject#getKeterangan()} yang
	 * menjamin hasil non-null (mengembalikan {@code ""} untuk field {@code null}). Override di sini
	 * mengembalikan <b>field mentah</b>, jadi <b>bisa {@code null}</b>. Kode yang mengandalkan
	 * jaminan induk — termasuk asumsi pada {@link GeneralValueObject#compareTo(GeneralValueObject)}
	 * bahwa cabang {@code keterangan} selalu tersedia — tidak berlaku untuk entity ini.</p>
	 *
	 * <p>Ditampilkan dan dapat disunting langsung pada kolom "Keterangan" di grid
	 * {@code PengecualianKknMahasiswaHelper}; setiap perubahan disimpan seketika lewat
	 * {@code Common.refreshSaveOrUpdate(...)} pada event {@code onChange}, tanpa tombol Simpan dan
	 * tanpa konfirmasi.</p>
	 *
	 * @return alasan dispensasi, atau {@code null} bila kolomnya kosong
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel alasan dispensasi. Tanpa validasi apa pun: {@code null}, string kosong, dan teks
	 * sepanjang apa pun diterima.
	 *
	 * <p>Dipanggil dua kali pada alur nyata: saat baris dibuat (diisi {@code ""} oleh
	 * {@code PengecualianKknMahasiswaHelper}) dan saat operator menyunting kolom Keterangan di
	 * grid.</p>
	 *
	 * @param keterangan alasan dispensasi baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan mahasiswa yang dikecualikan.
	 *
	 * <p>Relasi {@code @ManyToOne} ke kolom {@code mahasiswa} dengan
	 * {@code cascade = PERSIST, MERGE} dan {@code FetchMode.SELECT} (dimuat lewat SELECT terpisah,
	 * bukan JOIN).</p>
	 *
	 * <p><b>Tidak melewatkan {@link GeneralValueObject#check(Object)}</b> — berbeda dari mayoritas
	 * entity sepaket, getter ini mengembalikan field mentah. Object yang dikembalikan karena itu
	 * dapat berupa proxy lazy milik sesi asal; membacanya setelah sesi tersebut ditutup akan
	 * melempar {@code LazyInitializationException}. Ini relevan karena
	 * {@code PengecualianKknMahasiswaHelper} memuat daftarnya lewat
	 * {@code Common.getManualSession()} lalu memanggil {@code getNim()}/{@code getNama()}/
	 * {@code getJurusan()} pada saat render.</p>
	 *
	 * @return mahasiswa yang dikecualikan; dapat {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa yang dikecualikan.
	 *
	 * <p>Dipanggil sekali per baris baru di {@code PengecualianKknMahasiswaHelper}, untuk setiap
	 * mahasiswa yang dicentang pada dialog {@code AmbilDataMahasiswaBanyak}. Tidak ada validasi:
	 * tidak diperiksa apakah mahasiswa masih aktif, apakah jurusan/fakultasnya cocok dengan gelaran
	 * KKN pada {@link #setKkn(Kkn)}, maupun apakah baris untuk pasangan yang sama sudah ada.</p>
	 *
	 * <p><b>Efek samping cascade:</b> karena relasinya {@code PERSIST}/{@code MERGE}, object yang
	 * disetel di sini ikut di-persist/merge saat baris pengecualian disimpan.</p>
	 *
	 * @param mahasiswa mahasiswa yang dibebaskan dari syarat KKN
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan gelaran KKN tempat pengecualian ini berlaku.
	 *
	 * <p>Relasi {@code @ManyToOne} ke kolom {@code kkn} dengan {@code cascade = PERSIST, MERGE} dan
	 * {@code FetchMode.SELECT}. Sama seperti {@link #getMahasiswa()}, getter ini mengembalikan
	 * <b>field mentah tanpa {@link GeneralValueObject#check(Object)}</b> sehingga dapat berupa proxy
	 * lazy.</p>
	 *
	 * <p>Nilai inilah yang membuat dispensasi bersifat per gelaran: {@code checkSyaratKkn(...)}
	 * mencocokkan {@code Restrictions.eq("kkn", kkn)} bersama {@code eq("mahasiswa", mahasiswa)},
	 * jadi baris dengan {@code kkn} berbeda tidak pernah ikut terhitung.</p>
	 *
	 * @return gelaran KKN yang bersangkutan; dapat {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kkn", nullable = true)
	public Kkn getKkn() {
		return kkn;
	}

	/**
	 * Menyetel gelaran KKN tempat pengecualian ini berlaku.
	 *
	 * <p>Selalu diisi dengan gelaran yang sedang dibuka pada layar Seleksi Penerima KKN — nilai
	 * yang diteruskan ke konstruktor {@code PengecualianKknMahasiswaHelper(Kkn)}. Tidak ada jalur
	 * yang memungkinkan operator memilih gelaran lain dari dalam layar pengecualian.</p>
	 *
	 * <p><b>Efek samping cascade:</b> object {@link Kkn} yang disetel di sini ikut
	 * di-persist/merge saat baris pengecualian disimpan.</p>
	 *
	 * @param kkn gelaran KKN yang dispensasinya diberikan
	 */
	public void setKkn(Kkn kkn) {
		this.kkn = kkn;
	}

}
