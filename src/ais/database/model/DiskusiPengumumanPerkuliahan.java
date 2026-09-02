package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.StringReader;
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

import ais.common.Html2Text;

//import org.zkforge.fckez.MyCkEditor;

/**
 * Entity <b>satu komentar/tanggapan pada sebuah pengumuman akademik perkuliahan</b> (tabel
 * {@code public.diskusi_pengumuman_perkuliahan}). Satu baris = satu catatan teks
 * ({@link #getCatatan()}) yang ditempelkan pada satu {@link PengumumanPerkuliahan}
 * ({@link #getPengumumanPerkuliahan()}) oleh seorang penulis yang direkam sebagai
 * <i>salah satu</i> dari dua relasi eksklusif: {@link #getMahasiswa()} (bila yang berkomentar
 * mahasiswa) atau {@link #getTbmuser()} (bila yang berkomentar dosen/tendik/role lain).
 * Kumpulan baris dengan {@code pengumuman_perkuliahan} yang sama membentuk satu utas diskusi
 * datar (tanpa balasan berjenjang) di bawah pengumuman tersebut.
 *
 * <h3>Status pemakaian: AKTIF, dan ini entity diskusi akademik yang SEBENARNYA</h3>
 * <p>Berdasarkan verifikasi menyeluruh atas <i>source tree</i>, hanya dua berkas non-model yang
 * menyebut class ini — dan keduanya berada di jalur produksi modul pengumuman perkuliahan:</p>
 * <ul>
 * <li><b>{@code ais.action.master.TampilanPengumumanPerkuliahanAction}</b> — layar
 * <i>menghadap-mahasiswa</i> ("Beranda"/tampilan pengumuman). Membangun tombol "Komentar"
 * (hanya tampil bila {@code PengumumanPerkuliahan.getBolehDiberiKomentar()} bernilai
 * {@code true}), form modal penulisan komentar, grid utas ({@code loadData(...)} +
 * {@code DetailPengumumanRenderer}), penyimpanan ({@code onSave(...)}), pengaitan lampiran
 * ({@code LampiranLain} dengan {@code ref} = {@link #getId()} dan jenis
 * {@code "Lampiran Komentar Pengumuman Perkuliahan"}), serta pengiriman email notifikasi
 * ({@code kirimEmail(...)}).</li>
 * <li><b>{@code ais.action.master.helper.DetailPengumumanPerkuliahanHelper}</b> — panel
 * "Komentar Pengumuman Akademik" pada layar <i>pengelolaan</i> pengumuman
 * ({@code PengumumanPerkuliahanAction}, dipasang sebagai salah satu tab detail pengumuman).
 * Menyediakan grid + tambah/ubah/hapus komentar untuk petugas.</li>
 * </ul>
 * <p>Entity ini terdaftar di {@code hibernate.cfg.xml} (baris {@code <mapping class=...>}),
 * sehingga — seperti seluruh entity terpetakan lain — ia juga terjangkau oleh endpoint reflektif
 * generik ({@code /Data}, {@code /Api}) di luar kedua layar di atas.</p>
 *
 * <h3>JEBAKAN PENAMAAN — class ini BUKAN {@link Diskusi}</h3>
 * <p>Ada dua nama yang sangat mudah tertukar, dan keduanya <b>tidak berhubungan sama sekali</b>:</p>
 * <ul>
 * <li>{@link Diskusi} (+ {@link DiskusiKomentar}) — <b>modul editorial jurnal ilmiah</b>
 * (peer-review): utas pembicaraan tertutup yang menempel pada sebuah naskah dalam sebuah jurnal
 * penelitian, dengan {@code stageKey}, {@code visibility} dan {@code anonymity_mode}. Penulis
 * utamanya {@code ais.action.master.jurnal.JurnalDiscussionService}. Tidak dipakai sama sekali
 * oleh modul pengumuman/perkuliahan/e-learning.</li>
 * <li><b>{@code DiskusiPengumumanPerkuliahan} (class ini)</b> — komentar publik/akademik pada
 * pengumuman sebuah <i>perkuliahan</i> (kelas matakuliah). Inilah yang benar-benar dipakai untuk
 * diskusi pengumuman perkuliahan.</li>
 * </ul>
 * <p>Sekeluarga dengan class ini (struktur nyaris identik, beda induk pengumumannya saja):
 * {@link DiskusiPengumumanAkademis} (pengumuman akademik umum),
 * {@link DiskusiPengumumanPenelitian} (pengumuman penelitian) dan
 * {@link DiskusiPenelitianDanPengabdian} (pengajuan penelitian/pengabdian) — masing-masing punya
 * helper detail sendiri dengan nama method yang sama ({@code displayDetailPengumuman}). Terpisah
 * lagi: {@link PertemuanPunyaDiskusi}, forum tanya-jawab per pertemuan kuliah (e-learning) — itulah
 * yang muncul sebagai tombol "Ikut Diskusi" di layar absensi/e-learning.</p>
 *
 * <h3>Relasi dengan {@link GeneralValueObject}</h3>
 * <p>Class ini turunan langsung {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa — Hibernate
 * <b>tidak</b> memetakan properti induknya. Karena itu deklarasi ULANG {@link #id}, {@link #oleh},
 * {@link #olehId} dan {@link #tanggal_dirubah} di sini <b>bukan bug atau duplikasi ceroboh</b>,
 * melainkan keharusan teknis supaya keempat kolom itu ikut terpetakan. Konsekuensinya field-field
 * tersebut <b>membayangi (shadow)</b> field senama milik induk. Sebaliknya, properti induk yang
 * <b>tidak</b> dideklarasikan ulang di sini (mis. {@code nama}, {@code keterangan}, {@code kode})
 * <b>tidak</b> terpetakan sama sekali: memanggilnya lewat instance class ini akan mengembalikan
 * nilai bawaan induk, bukan nilai dari database. Fasilitas induk yang tetap dipakai:
 * {@code check(...)} (resolusi proxy lazy) pada {@link #getTbmuser()} dan {@link #getMahasiswa()}.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ol>
 * <li><b>Identitas &amp; penyajian:</b> konstruktor {@link #DiskusiPengumumanPerkuliahan()},
 * {@link #getId()} (PK {@code IDENTITY}), {@link #toString()}.</li>
 * <li><b>Isi komentar:</b> {@link #getCatatan()} (badan teks — <b>getter berefek samping</b>),
 * {@link #getJudul()} (kolom mati, lihat di bawah), {@link #getTanggal()} (waktu komentar
 * dibuat/disunting).</li>
 * <li><b>Penunjuk konteks:</b> {@link #getPengumumanPerkuliahan()} (induk utas, {@code NOT NULL})
 * — satu-satunya kunci penyaring yang dipakai semua query di kedua layar pemakai.</li>
 * <li><b>Penunjuk penulis (eksklusif satu sama lain):</b> {@link #getMahasiswa()} dan
 * {@link #getTbmuser()} — <b>keduanya getter berefek samping</b>.</li>
 * <li><b>Jejak audit (deklarasi ulang milik induk):</b> {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, kait {@link #onUpdate()}.</li>
 * <li><b>Field dorman:</b> {@link #getPengguna()} — tidak beranotasi, tidak pernah dibaca/ditulis
 * siapa pun.</li>
 * </ol>
 * <p>Tidak ada method bisnis, tidak ada method query statis, tidak ada validasi dan tidak ada
 * konstanta di class ini: seluruh logika (pengisian label penulis, validasi, notifikasi email,
 * pengaitan lampiran) berada di kedua kelas pemakai. Class ini murni kantong data + tiga getter
 * berefek samping.</p>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui pemelihara</h3>
 * <ul>
 * <li><b>{@link #getJudul()} adalah getter DESTRUKTIF yang mematikan kolomnya sendiri.</b> Getter
 * ini <b>selalu</b> menimpa field dengan konstanta {@code "-"} sebelum mengembalikannya, sehingga
 * apa pun yang diberikan lewat {@link #setJudul(String)} hilang tanpa jejak. Karena pemetaan entity
 * ini memakai <i>property access</i> (anotasi menempel pada getter), Hibernate membaca nilai lewat
 * getter tersebut saat {@code INSERT}/{@code UPDATE} — jadi kolom {@code judul} di database
 * <b>selalu</b> berisi {@code "-"} pada setiap baris. Efeknya kolom ini {@code NOT NULL} terpenuhi
 * secara otomatis, tapi tidak menyimpan informasi apa pun. Tidak ada satu pun pemanggil
 * {@code setJudul(...)} di luar class ini.</li>
 * <li><b>{@link #getCatatan()} adalah getter DESTRUKTIF yang meratakan HTML menjadi teks polos.</b>
 * Setiap pemanggilan mem-parsing isi field dengan {@link Html2Text} lalu <b>menulis balik</b> hasil
 * teks-polosnya ke field. Sekali lagi karena <i>property access</i>, hasil perataan itu ikut
 * tersimpan permanen pada {@code flush} berikutnya — bahkan bila komentar hanya <i>dibaca</i> untuk
 * dirender ke grid. Konsekuensi baik: praktis tidak ada jalur XSS tersimpan lewat kolom ini (markup
 * dihapus di sumber, bukan sekadar di-escape saat render), termasuk pada badan email notifikasi
 * yang menyisipkan {@code getCatatan()} apa adanya ke HTML. Konsekuensi buruk: <b>kehilangan data
 * senyap</b> — teks yang kebetulan mengandung karakter {@code <} (mis. "nilai a &lt; b") akan
 * ditafsirkan sebagai awal tag dan bagian setelahnya ikut terbuang, permanen. Field
 * di-<i>default</i> ke string kosong bila {@code null}, sehingga getter ini tidak pernah
 * mengembalikan {@code null}.</li>
 * <li><b>{@link #getTbmuser()} adalah getter DESTRUKTIF — pola kembar {@code Komentar.getTbmuser()}
 * dan {@code OrganisasiDosenPunyaDosen}.</b> Bila {@link #mahasiswa} tidak {@code null}, getter ini
 * menulis {@code null} ke field {@code tbmuser} sebelum mengembalikannya, sehingga kolom FK
 * {@code tbmuser} ikut dikosongkan permanen. Di sini efeknya <b>disengaja dan konsisten</b>: kedua
 * kelas pemakai sudah lebih dulu melakukan hal yang sama di {@code onSave(...)}
 * ({@code if (mahasiswa != null) tbmuser = null;}), jadi getter ini hanya menegakkan ulang invarian
 * "tepat satu dari {@code mahasiswa}/{@code tbmuser} terisi". Tetap perlu diwaspadai bila kelak ada
 * jalur tulis baru yang mengisi keduanya sekaligus.</li>
 * <li><b>{@link #setOleh(String)} dan {@link #setOlehId(String)} MENOLAK nilai kosong.</b> Keduanya
 * langsung {@code return} bila argumennya {@code null}/spasi saja — nilai lama dipertahankan. Jadi
 * label penulis tidak bisa dikosongkan lewat setter, hanya bisa ditimpa dengan nilai non-kosong.
 * Ini penting karena {@link #getOleh()} adalah <b>satu-satunya</b> nama penulis yang benar-benar
 * dirender di grid komentar kedua layar.</li>
 * <li><b>Label penulis {@link #getOleh()} bisa berpindah identitas.</b> Kedua {@code onSave(...)}
 * mengisi ulang {@code oleh} dengan identitas <i>pengguna yang sedang menyimpan</i>, bukan pengguna
 * yang pertama menulis. Karena tombol "Ubah" pada layar pengelolaan tidak bergerbang kepemilikan
 * (lihat catatan keamanan di bawah), menyunting komentar orang lain akan sekaligus
 * <b>mengalihkan atribusi penulisnya</b> ke penyunting. Relasi {@link #getMahasiswa()}/
 * {@link #getTbmuser()} juga ditimpa pada saat yang sama.</li>
 * <li><b>Bug label "(Dosen)" yang tidak pernah tercapai.</b> Pada kedua {@code onSave(...)}
 * cabang penentuan label penulis ditulis
 * {@code if (tbmuser.getMahasiswa() != null) {...} else if (tbmuser.getMahasiswa() != null) {...}}
 * — kondisi cabang kedua identik dengan yang pertama, sehingga cabang {@code "(Dosen)"} adalah
 * <b>kode mati</b>. Komentar dari dosen selalu berlabel {@code "<userId> (<NamaRole>)"}, bukan
 * {@code "<Nama Dosen> (Dosen)"}. Dicatat, tidak diperbaiki.</li>
 * <li><b>Email notifikasi menyiarkan SELURUH utas.</b> {@code kirimEmail(...)} mengumpulkan alamat
 * dari {@code korespondensi} pengumuman <i>plus</i> email seluruh mahasiswa dan seluruh
 * {@code Tbmuser} yang pernah berkomentar pada pengumuman yang sama, lalu mengirim badan email yang
 * berisi <b>daftar lengkap semua komentar</b> pada pengumuman itu beserta NIM/nama/userId
 * penulisnya. Artinya seorang mahasiswa yang menulis satu komentar akan menerima transkrip penuh
 * percakapan (termasuk komentar dosen/petugas) lewat email, dan setiap komentar baru mengirim ulang
 * transkrip yang makin panjang ke seluruh peserta.</li>
 * <li><b>Query pembaca tidak seragam.</b> Layar mahasiswa menyaring
 * {@code catatan != "" AND catatan IS NOT NULL} dan mengurutkan {@code id} menurun (terbaru di
 * atas); layar pengelolaan tidak menyaring apa pun dan mengurutkan {@code id} menaik. Baris
 * berkomentar kosong karena itu terlihat di layar petugas tapi tersembunyi dari mahasiswa, dan
 * urutan utas terbalik antara kedua layar.</li>
 * </ul>
 *
 * <h3>Catatan keamanan — dua jalur, hanya satu yang bergerbang</h3>
 * <p>Kedua kelas pemakai punya <i>renderer</i> baris komentar bernama sama
 * ({@code DetailPengumumanRenderer}) dengan tombol "Ubah Data"/"Hapus Data" yang sama, tetapi
 * perlakuan hak aksesnya berbeda:</p>
 * <ul>
 * <li><b>{@code TampilanPengumumanPerkuliahanAction} (layar mahasiswa) — BERGERBANG.</b> Grup
 * tombol aksi hanya ditampilkan bila {@code bolehUbahKomentar}, yaitu bila
 * {@link #getTbmuser()} sama dengan pengguna login <i>atau</i> {@link #getMahasiswa()} sama dengan
 * mahasiswa pengguna login. Ini <b>contoh positif</b>: mahasiswa hanya bisa menyunting/menghapus
 * komentarnya sendiri.</li>
 * <li><b>{@code DetailPengumumanPerkuliahanHelper} (layar pengelolaan) — TANPA GERBANG APA PUN.</b>
 * Tombol "Ubah Data"/"Hapus Data" pada renderer helper ini tidak diperiksa terhadap kepemilikan,
 * tidak diperiksa terhadap {@code CommonPrivilages.UPDATE}/{@code DELETE}, dan bahkan tidak
 * mengikuti flag {@code readonly} milik helper itu sendiri — flag tersebut hanya menyembunyikan
 * toolbar <i>lampiran</i> ({@code displayAttachment}), bukan toolbar komentar. Siapa pun yang bisa
 * membuka tab detail pengumuman dapat menyunting maupun menghapus permanen komentar milik siapa
 * pun, termasuk komentar dosen dan mahasiswa. Penghapusan memakai {@code Common.refreshDelete(...)}
 * (hard delete). Ini <b>instance baru dari pola berulang "dua jalur ke data yang sama, satu benar
 * satu salah"</b>, dan kembaran struktural temuan pada {@code Komentar}/{@code KomentarRenderer}.</li>
 * </ul>
 * <p>Peringan: baris tetap disalin ke tabel audit Envers ({@link Audited}), sehingga isi komentar
 * yang dihapus masih dapat ditelusuri lewat riwayat revisi.</p>
 *
 * @see PengumumanPerkuliahan
 * @see ais.database.model.GeneralValueObject
 * @see Diskusi
 * @see DiskusiPengumumanAkademis
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "diskusi_pengumuman_perkuliahan")

public class DiskusiPengumumanPerkuliahan extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai tetap dari generator {@code hbm2java}; jangan diubah
	 * agar objek yang pernah diserialisasi (mis. ke sesi ZK) tetap dapat dibaca kembali.
	 */
	private static final long serialVersionUID = 2463822577541439808L;

	/**
	 * Kunci primer baris komentar (kolom {@code id}, {@code IDENTITY}). Dideklarasikan ulang di sini
	 * karena {@link GeneralValueObject} bukan {@code @MappedSuperclass}.
	 */
	private Long id;

	/**
	 * Kait JPA {@code @PreUpdate} sekaligus deklarasi field {@link #tanggal_dirubah} (keduanya
	 * ditulis pada satu baris fisik oleh generator; format ini dipertahankan apa adanya).
	 *
	 * <p>{@code onUpdate()} dipanggil <b>otomatis oleh Hibernate/JPA</b> tepat sebelum baris ini
	 * di-{@code UPDATE}, lalu mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang menyegarkan
	 * {@link #tanggal_dirubah} serta {@link #oleh}/{@link #olehId} dari pengguna sesi berjalan.
	 * Tidak ada padanan {@code @PrePersist}: untuk baris <b>baru</b>, ketiga kolom audit itu diisi
	 * lewat jalur lain, yaitu {@code AuditTimestampInterceptor.onSave(...)} pada level interceptor
	 * Hibernate.</p>
	 *
	 * <p>Field {@link #tanggal_dirubah} sendiri diinisialisasi seketika saat objek dibuat dengan
	 * {@code ais.ui.util.WaktuUtil.getDate()} (waktu server, menghormati penyetelan zona/offset
	 * aplikasi), bukan {@code new Date()} langsung. Jangan dikacaukan dengan {@link #tanggal} yang
	 * merupakan waktu <i>bisnis</i> komentar dan diisi eksplisit oleh kelas pemakai.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; normalnya diisi otomatis oleh
	 *                        {@code AuditTimestampInterceptor}, bukan oleh kode aplikasi
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, presisi
	 * {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         konstruktor karena field diinisialisasi di deklarasinya
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini: mengembalikan field {@link #catatan} <b>mentah</b> (bukan lewat
	 * {@link #getCatatan()}), sehingga tidak memicu perataan HTML dan bisa mengembalikan
	 * {@code null} untuk baris yang catatannya belum diisi.
	 *
	 * @return isi komentar apa adanya, atau {@code null}
	 */
	public String toString() {
		return catatan;
	}

	/**
	 * Kolom {@code judul} ({@code NOT NULL}, panjang 500). <b>Mati secara efektif</b>: selalu ditimpa
	 * konstanta {@code "-"} oleh {@link #getJudul()}. Lihat catatan pada getter tersebut.
	 */
	private String judul;

	/**
	 * Badan teks komentar (kolom {@code catatan}, {@code NOT NULL}, panjang 3000). Disimpan sebagai
	 * teks polos karena {@link #getCatatan()} meratakan HTML apa pun yang masuk.
	 */
	private String catatan;

	/**
	 * Label identitas penulis/penyunting terakhir dalam bentuk teks siap tampil (kolom {@code oleh}),
	 * mis. {@code "12345 - Budi (Mahasiswa)"} atau {@code "admin (Administrator)"}. Inilah nilai yang
	 * benar-benar dirender di kolom "Oleh" pada grid kedua layar. Deklarasi ulang properti audit
	 * milik {@link GeneralValueObject}.
	 */
	private String oleh;

	/**
	 * Identifier pengguna yang melakukan perubahan terakhir (kolom {@code oleh_id}), diisi otomatis
	 * oleh {@code AuditTimestampInterceptor}. Deklarasi ulang properti audit milik
	 * {@link GeneralValueObject}.
	 */
	private String olehId;

	/**
	 * Identifier pengguna terakhir yang menyentuh baris ini.
	 *
	 * @return identifier pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identifier pengguna yang melakukan perubahan terakhir.
	 *
	 * <p><b>Efek samping/kuirk:</b> argumen {@code null} atau yang hanya berisi spasi
	 * <b>diabaikan diam-diam</b> (method langsung {@code return}), sehingga nilai lama tetap
	 * dipertahankan. Field ini karena itu tidak dapat dikosongkan kembali lewat setter.</p>
	 *
	 * @param olehId identifier pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Field <b>dorman</b> (tidak beranotasi kolom, tidak pernah dibaca maupun ditulis oleh kode mana
	 * pun di luar pasangan {@link #getPengguna()}/{@link #setPengguna(String)}). Kemungkinan sisa
	 * rancangan awal sebelum identitas penulis dipecah menjadi relasi {@link #mahasiswa}/
	 * {@link #tbmuser} + label {@link #oleh}.
	 */
	private String pengguna;

	/**
	 * Waktu <i>bisnis</i> komentar (kolom {@code tanggal}): diisi ulang dengan waktu server setiap
	 * kali komentar disimpan, baik saat dibuat maupun saat disunting.
	 */
	private Date tanggal;

	/** Pengumuman perkuliahan induk utas ini (kolom FK {@code pengumuman_perkuliahan}, {@code NOT NULL}). */
	private PengumumanPerkuliahan pengumumanPerkuliahan;

	/**
	 * Akun pengguna penulis komentar untuk penulis <b>non-mahasiswa</b> (kolom FK {@code tbmuser},
	 * boleh {@code null}). Saling eksklusif dengan {@link #mahasiswa} — lihat catatan destruktif pada
	 * {@link #getTbmuser()}.
	 */
	private Tbmuser tbmuser;

	/**
	 * Mahasiswa penulis komentar (kolom FK {@code mahasiswa}, boleh {@code null}). Saling eksklusif
	 * dengan {@link #tbmuser}.
	 */
	private Mahasiswa mahasiswa;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate. Juga dipakai langsung oleh kedua kelas
	 * pemakai untuk membuat komentar baru ({@code init(new DiskusiPengumumanPerkuliahan(), ...)}).
	 * Tidak menginisialisasi apa pun selain {@link #tanggal_dirubah} lewat inisialisasi field.
	 *
	 * <p>Komentar {@code // MyCkEditor} di badan konstruktor adalah sisa peninggalan dari masa ketika
	 * isian komentar direncanakan memakai editor kaya CKEditor (lihat pula {@code import} yang
	 * dikomentari di kepala berkas); kini isian komentar memakai {@code Textbox} biasa multibaris.</p>
	 */
	public DiskusiPengumumanPerkuliahan() {
		// MyCkEditor
	}

	/**
	 * Kunci primer baris komentar (kolom {@code id}, strategi {@code IDENTITY} — nomor berurutan
	 * yang dibangkitkan database). Juga dipakai sebagai nilai {@code ref} untuk mengaitkan berkas
	 * {@code LampiranLain} berjenis {@code "Lampiran Komentar Pengumuman Perkuliahan"} ke komentar
	 * ini.
	 *
	 * <p>Anotasi {@code @Id} yang menempel pada getter inilah yang menetapkan seluruh pemetaan entity
	 * ini sebagai <i>property access</i> — konsekuensinya semua getter berefek samping di class ini
	 * ikut dijalankan Hibernate saat {@code INSERT}/{@code UPDATE}/pemeriksaan <i>dirty</i>.</p>
	 *
	 * @return id baris, atau {@code null} untuk objek yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris komentar. Normalnya tidak dipanggil kode aplikasi — nilainya
	 * dibangkitkan database.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Badan teks komentar (kolom {@code catatan}, {@code NOT NULL}, panjang 3000).
	 *
	 * <p><b>GETTER DESTRUKTIF — verifikasi langsung atas isi file ini.</b> Method ini tidak sekadar
	 * membaca:</p>
	 * <ol>
	 * <li>bila field {@code null}, field <b>diisi</b> string kosong (sehingga nilai balik tidak pernah
	 * {@code null} dan batasan {@code NOT NULL} kolom selalu terpenuhi);</li>
	 * <li>isi field diparsing dengan {@link Html2Text} (parser HTML bawaan Swing) dan hasil teks
	 * polosnya <b>ditulis balik ke field</b>.</li>
	 * </ol>
	 * <p>Karena entity ini memakai <i>property access</i>, Hibernate memanggil getter ini saat
	 * membaca nilai untuk {@code INSERT}/{@code UPDATE} dan saat pemeriksaan <i>dirty</i> ketika
	 * {@code flush} — jadi perataan HTML itu <b>tersimpan permanen</b>, bahkan bila komentar hanya
	 * dibaca untuk dirender ke grid atau untuk menyusun badan email notifikasi.</p>
	 * <p><b>Sisi baik:</b> markup dibuang di sumber, bukan sekadar di-escape saat render, sehingga
	 * praktis tidak ada jalur XSS tersimpan lewat kolom ini — termasuk pada badan email
	 * {@code kirimEmail(...)} yang menyisipkan hasil method ini langsung ke HTML.
	 * <b>Sisi buruk:</b> kehilangan data senyap untuk teks sah yang mengandung {@code <} (parser
	 * menganggapnya awal tag dan membuang sisanya).</p>
	 * <p>Kegagalan parsing ditangkap dan hanya dicatat lewat
	 * {@code ais.common.ErrorAuditUtil.record(...)}; dalam kasus itu isi field dikembalikan apa
	 * adanya tanpa diratakan.</p>
	 *
	 * @return isi komentar sebagai teks polos; tidak pernah {@code null} (minimal string kosong)
	 */
	@Column(name = "catatan", nullable = false, length = 3000)
	public String getCatatan() {
		if (catatan == null) {
			catatan = "";
		}
		try {
			Html2Text parser = new Html2Text();
			parser.parse(new StringReader(catatan));
			catatan = parser.getText();
			parser = null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/DiskusiPengumumanPerkuliahan.java:108");
			// TODO: handle exception
		}
		return this.catatan;
	}

	/**
	 * Menyetel badan teks komentar. Dipanggil oleh {@code onSave(...)} kedua kelas pemakai dengan
	 * nilai mentah dari {@code Textbox} isian; perataan HTML baru terjadi pada pembacaan berikutnya
	 * lewat {@link #getCatatan()}.
	 *
	 * @param catatan isi komentar; boleh {@code null} (akan dinormalkan menjadi string kosong saat
	 *                dibaca)
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Menyetel label identitas penulis/penyunting terakhir.
	 *
	 * <p><b>Efek samping/kuirk:</b> argumen {@code null} atau yang hanya berisi spasi
	 * <b>diabaikan diam-diam</b>, sehingga label lama dipertahankan. Ini berarti bila
	 * {@code onSave(...)} gagal menentukan label (mis. tidak ada pengguna login sehingga
	 * {@code myoleh} tetap string kosong), komentar akan tetap menampilkan nama penulis
	 * <i>sebelumnya</i> — bukan kosong.</p>
	 *
	 * @param oleh label identitas siap tampil; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Label identitas penulis/penyunting terakhir (kolom {@code oleh}) — satu-satunya nama penulis
	 * yang benar-benar ditampilkan pada kolom "Oleh" di grid komentar kedua layar pemakai.
	 *
	 * <p>Perlu diingat nilainya <b>ditulis ulang setiap penyimpanan</b> dengan identitas pengguna
	 * yang sedang menyimpan, sehingga menyunting komentar orang lain akan mengalihkan atribusinya.</p>
	 *
	 * @return label identitas penulis, atau {@code null} bila belum pernah diisi nilai non-kosong
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel waktu bisnis komentar. Kedua {@code onSave(...)} selalu memanggilnya dengan
	 * {@code ais.ui.util.WaktuUtil.getDate()} — termasuk saat menyunting komentar lama, sehingga
	 * tanggal asli pembuatan komentar tidak dipertahankan.
	 *
	 * @param tanggal waktu komentar dibuat/disunting
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Waktu bisnis komentar (kolom {@code tanggal}, presisi {@code TIMESTAMP}) — dirender di kolom
	 * "Tanggal" grid kedua layar dan disisipkan ke transkrip pada email notifikasi.
	 *
	 * @return waktu komentar dibuat/disunting terakhir, atau {@code null} untuk baris lama yang belum
	 *         pernah diisi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menyetel pengumuman induk utas ini. Wajib diisi sebelum menyimpan karena kolomnya
	 * {@code NOT NULL}; kedua {@code onSave(...)} mengisinya dari pengumuman yang sedang dibuka.
	 *
	 * @param pengumumanPerkuliahan pengumuman perkuliahan induk
	 */
	public void setPengumumanPerkuliahan(PengumumanPerkuliahan pengumumanPerkuliahan) {
		this.pengumumanPerkuliahan = pengumumanPerkuliahan;
	}

	/**
	 * Pengumuman perkuliahan tempat komentar ini menempel (kolom FK {@code pengumuman_perkuliahan},
	 * {@code NOT NULL}). Inilah <b>satu-satunya</b> kunci penyaring yang dipakai seluruh query
	 * pembaca: pemuatan grid pada kedua layar, penghitungan jumlah komentar untuk memutuskan apakah
	 * panel diskusi perlu digambar, dan keempat query pengumpulan alamat email di
	 * {@code kirimEmail(...)}.
	 *
	 * <p>Relasi ini {@code @ManyToOne} dengan {@code cascade = {PERSIST, MERGE}} dan
	 * {@code @Fetch(FetchMode.SELECT)} — dimuat lewat query terpisah, bukan {@code JOIN}. Berbeda
	 * dari {@link #getTbmuser()}/{@link #getMahasiswa()}, getter ini <b>tidak</b> memanggil
	 * {@code check(...)}, sehingga bila objek sudah <i>detached</i> pemanggil bisa menerima proxy
	 * lazy yang belum terinisialisasi.</p>
	 *
	 * @return pengumuman induk; secara praktis tidak pernah {@code null} untuk baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengumuman_perkuliahan", nullable = false)
	public PengumumanPerkuliahan getPengumumanPerkuliahan() {
		return pengumumanPerkuliahan;
	}

	/**
	 * Menyetel judul komentar. <b>Tidak berguna:</b> nilai apa pun yang disetel di sini akan ditimpa
	 * konstanta {@code "-"} pada pembacaan berikutnya oleh {@link #getJudul()}. Tidak ada pemanggil
	 * di luar class ini.
	 *
	 * @param judul judul komentar; diabaikan secara efektif
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Kolom {@code judul} ({@code NOT NULL}, panjang 500).
	 *
	 * <p><b>GETTER DESTRUKTIF — verifikasi langsung atas isi file ini.</b> Method ini
	 * <b>selalu</b> menimpa field dengan konstanta {@code "-"} sebelum mengembalikannya. Karena
	 * entity ini memakai <i>property access</i>, nilai itulah yang dibaca Hibernate saat
	 * {@code INSERT}/{@code UPDATE} — sehingga <b>setiap baris di tabel selalu berisi
	 * {@code judul = '-'}</b> dan kolom ini tidak pernah menyimpan informasi apa pun. Efek
	 * sampingannya batasan {@code NOT NULL} kolom selalu terpenuhi tanpa perlu validasi di UI (form
	 * komentar memang hanya menyediakan isian catatan, tidak ada isian judul).</p>
	 *
	 * @return selalu {@code "-"}
	 */
	@Column(name = "judul", nullable = false, length = 500)
	public String getJudul() {
		judul = "-";
		return judul;
	}

	/**
	 * Field dorman {@link #pengguna}. Tidak beranotasi kolom sehingga <b>tidak terpetakan
	 * Hibernate</b>, dan tidak ada satu pun pemanggil di seluruh <i>source tree</i>.
	 *
	 * @return isi field {@code pengguna}; praktis selalu {@code null}
	 */
	public String getPengguna() {
		return pengguna;
	}

	/**
	 * Menyetel field dorman {@link #pengguna}. Tidak berpengaruh pada apa pun — nilainya tidak
	 * tersimpan ke database maupun dibaca kode lain.
	 *
	 * @param pengguna nilai yang disimpan di memori saja
	 */
	public void setPengguna(String pengguna) {
		this.pengguna = pengguna;
	}

	/**
	 * Akun pengguna penulis komentar untuk penulis <b>non-mahasiswa</b> (dosen, tendik, admin) —
	 * kolom FK {@code tbmuser}, boleh {@code null}, dimuat {@code LAZY}.
	 *
	 * <p><b>GETTER DESTRUKTIF — verifikasi langsung atas isi file ini.</b> Bila {@link #mahasiswa}
	 * tidak {@code null}, method ini <b>menulis {@code null} ke field {@code tbmuser}</b> sebelum
	 * mengembalikannya. Karena entity ini memakai <i>property access</i>, Hibernate membaca nilai
	 * lewat getter ini saat {@code INSERT}/{@code UPDATE} dan saat pemeriksaan <i>dirty</i> ketika
	 * {@code flush} — sehingga kolom FK {@code tbmuser} di database ikut dikosongkan permanen.</p>
	 * <p>Berbeda dari kasus {@code Komentar.getTbmuser()} (yang menyebabkan identitas penulis hilang
	 * tanpa disengaja), di sini perilakunya <b>konsisten dengan niat pemakainya</b>: kedua
	 * {@code onSave(...)} sudah lebih dulu menjalankan {@code if (mahasiswa != null) tbmuser = null;}
	 * sebelum menyimpan. Getter ini hanya menegakkan ulang invarian "tepat satu dari
	 * {@link #mahasiswa}/{@code tbmuser} yang terisi", yang juga menjadi dasar penentuan nama
	 * pengirim pada badan email notifikasi. Tetap perlu diwaspadai bila kelak ada jalur tulis baru
	 * yang mengisi keduanya sekaligus — data akan hilang senyap.</p>
	 * <p>Selain itu getter ini memanggil {@code check(...)} milik {@link GeneralValueObject} dan
	 * <b>menugaskan hasilnya kembali ke field</b>, untuk meresolusi proxy lazy pada objek yang sudah
	 * <i>detached</i> (dibawa lintas request ZK / diambil dari cache). Method itu tidak pernah
	 * melempar exception dan tidak mengembalikan {@code null} untuk argumen non-null; kegagalan
	 * resolusi bersifat senyap. Getter ini <b>tidak</b> membuka maupun menutup sesi Hibernate sendiri
	 * — pengelolaan sesi sepenuhnya urusan {@code check(...)}.</p>
	 *
	 * @return akun penulis non-mahasiswa, atau {@code null} bila penulisnya mahasiswa (atau bila
	 *         memang belum pernah diisi)
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		if (mahasiswa != null) {
			tbmuser = null;
		}
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menyetel akun pengguna penulis komentar. Kedua {@code onSave(...)} memanggilnya dengan
	 * pengguna login saat ini, namun sudah menormalkan nilainya menjadi {@code null} lebih dulu bila
	 * pengguna itu ternyata seorang mahasiswa.
	 *
	 * @param tbmuser akun penulis non-mahasiswa, atau {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mahasiswa penulis komentar (kolom FK {@code mahasiswa}, boleh {@code null}, dimuat
	 * {@code LAZY}). Terisi bila komentar ditulis dari akun mahasiswa; saling eksklusif dengan
	 * {@link #getTbmuser()}.
	 *
	 * <p>Getter ini memanggil {@code check(...)} milik {@link GeneralValueObject} dan
	 * <b>menugaskan hasilnya kembali ke field</b> — pola standar seluruh entity AIS untuk meresolusi
	 * proxy lazy pada objek yang sudah <i>detached</i>. Penugasan balik itu penting karena
	 * {@code check(...)} bisa mengembalikan instance yang <i>berbeda</i> (kanonik dari
	 * {@code EntityIdentityMap}, dari cache, atau hasil reload). Berbeda dari
	 * {@link #getTbmuser()}, getter ini <b>tidak</b> mengosongkan field apa pun, dan ia
	 * <b>tidak</b> membuka/menutup sesi Hibernate sendiri.</p>
	 * <p>Dipakai layar mahasiswa untuk menentukan {@code bolehUbahKomentar} (membandingkan
	 * {@code getMahasiswa().getId()} dengan mahasiswa pengguna login) — satu-satunya gerbang
	 * kepemilikan yang ada pada entity ini — serta oleh {@code kirimEmail(...)} untuk mengumpulkan
	 * alamat email/NIM penerima notifikasi.</p>
	 *
	 * @return mahasiswa penulis, atau {@code null} bila penulisnya bukan mahasiswa
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa penulis komentar. Kedua {@code onSave(...)} memanggilnya dengan
	 * {@code Common.getCurrentUser().getMahasiswa()} — bernilai {@code null} untuk pengguna
	 * non-mahasiswa.
	 *
	 * @param mahasiswa mahasiswa penulis, atau {@code null}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}
}
