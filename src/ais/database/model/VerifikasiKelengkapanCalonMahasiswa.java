package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Master data <b>item/jenis berkas kelengkapan calon mahasiswa</b> (PMB) — tabel
 * {@code public.verifikasi_kelengkapan_calon_mahasiswa}.
 *
 * <p>Satu baris entity ini = satu <i>syarat dokumen</i> yang harus dipenuhi pendaftar, mis.
 * "Fotocopy Ijazah yang telah dilegalisir", "Satu (1) lembar fotocopy Kartu Keluarga", "Dua (2)
 * lembar pas photo warna ukuran 3 x 4 terbaru". Isinya murni <b>definisi syarat</b>: nama syarat,
 * keterangan, plus enam flag Boolean yang menentukan kapan syarat itu menggigit (wajib diunggah?
 * wajib diverifikasi petugas? menghalangi ujian? menghalangi interview?).</p>
 *
 * <p><b>Entity ini TIDAK menyimpan file dan TIDAK menyimpan status per pendaftar.</b> Ia hanya
 * katalog syarat. Status "sudah diunggah / sudah diverifikasi" milik seorang calon disimpan di
 * {@link BiodataCalonMahasiswaPunyaVerifikasiBerkas} (satu baris per pasangan calon &times;
 * syarat), sedangkan isi file fisiknya disimpan sebagai {@link
 * ais.database.model.file.LampiranLain} yang menunjuk ke id baris
 * {@code BiodataCalonMahasiswaPunyaVerifikasiBerkas} tersebut. Lihat "Jalur file &amp; catatan
 * keamanan" di bawah.</p>
 *
 * <h3>Peta relasi (semua sisi pemilik ada di kelas LAIN)</h3>
 * <p>Kelas ini sengaja tidak mendeklarasikan satu pun relasi. Empat tabel menunjuk ke sini:</p>
 * <ul>
 * <li>{@link BiodataCalonMahasiswaPunyaVerifikasiBerkas} — {@code @ManyToOne} lewat kolom
 * {@code verifikasi_kelengkapan_calon_mahasiswa}. Ini penghubung ke {@link BiodataCalonMahasiswa};
 * <b>tidak ada</b> relasi langsung calon mahasiswa &rarr; syarat.</li>
 * <li>{@link GelombangPendaftaran#getVerifikasiKelengkapanCalonMahasiswas()} —
 * {@code @ManyToMany} lewat tabel gabung {@code gelombang_punya_verifikasi}
 * ({@code gelombang}, {@code verifikasi}). Menentukan syarat mana yang berlaku untuk suatu
 * gelombang pendaftaran.</li>
 * <li>{@link JenisSeleksi#getVerifikasiKelengkapanCalonMahasiswas()} — {@code @ManyToMany} lewat
 * tabel gabung {@code jenis_seleksi_punya_verifikasi} ({@code jenis_seleksi}, {@code verifikasi}).
 * Penyaring tambahan berdasarkan jalur seleksi calon.</li>
 * <li>{@link ais.database.model.file.LampiranLain} — lampiran yang menempel pada baris master ini
 * sendiri (lihat di bawah), disimpan dengan {@code jenis} =
 * {@code "ais.database.model.VerifikasiKelengkapanCalonMahasiswa"}.</li>
 * </ul>
 *
 * <h3>Bagaimana "daftar syarat efektif" seorang calon dihitung</h3>
 * <p>Resolusinya ada di {@code ais.action.master.pmb.VerifikasiPMBHelper.ambilVerifikasiEfektif()}
 * dan diulang (salin-tempel) di {@code DaftarMahasiswaLulusAction}, {@code CetakRegistrasiAction},
 * serta {@code CommonReportHelper}. Alurnya:</p>
 * <ol>
 * <li>Ambil himpunan syarat milik {@link GelombangPendaftaran} calon. <b>Bila himpunan ini
 * kosong, hasil akhir langsung kosong</b> — tidak ada fallback ke "semua syarat aktif".</li>
 * <li>Buang yang {@link #getAktif()} bernilai {@code false}.</li>
 * <li>Bila calon punya {@link JenisSeleksi} dan jenis seleksi itu punya daftar syarat sendiri
 * yang tidak kosong, daftar dari langkah 1 <b>disaring/diganti</b> oleh daftar jenis seleksi.</li>
 * <li>Urutkan dengan {@code Collections.sort()} &rarr; {@link GeneralValueObject#compareTo}.</li>
 * </ol>
 * <p><b>Konsekuensi fail-open yang perlu diketahui:</b> gelombang pendaftaran yang lupa dicentang
 * daftar berkasnya membuat SELURUH gerbang kelengkapan berkas (sebelum ujian, sebelum interview,
 * sebelum cetak registrasi) diam-diam nonaktif untuk semua pendaftar di gelombang itu — tidak ada
 * peringatan, tidak ada log. Pola ini identik dengan fail-open filter-kosong yang tercatat pada
 * entity lain di inisiatif dokumentasi ini.</p>
 *
 * <p><b>Urutan tampil tidak dapat dikonfigurasi.</b> Pengurutan memakai
 * {@link GeneralValueObject#compareTo(GeneralValueObject)} yang mencoba berturut-turut
 * {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr; {@code keterangan}. Karena
 * {@link GeneralValueObject} <b>bukan</b> {@code @MappedSuperclass}, properti {@code nomorUrut}
 * dan {@code nim} tidak dipetakan Hibernate dan selalu {@code null} pada instance hasil query;
 * pengurutan otomatis jatuh ke {@link #getNama()}. Artinya daftar syarat selalu tampil
 * <b>alfabetis menurut teks panjang nama syarat</b>, dan layar CRUD tidak menyediakan cara
 * mengubahnya.</p>
 *
 * <h3>Hubungan dengan {@link Berkas} — klarifikasi</h3>
 * <p>Javadoc {@link Berkas} menduga entity ini "kemungkinan menggantikan" fitur {@code Berkas}
 * yang yatim. Pemeriksaan kode mengoreksi dugaan itu menjadi lebih tepat sebagai berikut:</p>
 * <ul>
 * <li><b>Secara fungsional: YA.</b> Kebutuhan "daftar jenis dokumen yang harus diserahkan" di
 * AIS dilayani oleh entity ini (beserta kembarannya untuk jenjang/objek lain), bukan oleh
 * {@code Berkas}. {@code Berkas} tidak dirujuk satu pun alur PMB, sedangkan entity ini dirujuk
 * belasan Action, helper, report, layanan UI baru, dan JSP.</li>
 * <li><b>Secara historis: TIDAK.</b> Ini bukan hasil migrasi/refactor dari {@code Berkas}. Kedua
 * file membawa header generator yang <b>persis sama</b> — {@code "Generated Apr 16, 2010
 * 2:27:16 PM by Hibernate Tools 3.2.4.CR1"} — jadi kedua tabel lahir dari satu kali jalan
 * {@code hbm2java} yang sama, berdampingan sejak awal. Tidak ada kolom bersama, tidak ada FK
 * antar keduanya, dan tidak ada kode migrasi data di codebase.</li>
 * <li><b>Bentuk datanya pun berbeda.</b> {@code Berkas} adalah <i>pohon</i> taksonomi
 * (self-reference {@code parent}, penghitung {@code jmlDipakai}) tanpa flag kewajiban; entity ini
 * adalah <i>daftar datar</i> tanpa hierarki tetapi dengan enam flag gerbang. Keduanya tidak
 * saling menggantikan secara struktur — {@code Berkas} adalah cabang yang mati, entity ini
 * cabang yang tumbuh.</li>
 * </ul>
 *
 * <h3>Kembaran di jenjang/objek lain (duplikasi yang tidak simetris)</h3>
 * <p>Pola master-syarat ini disalin-tempel ke beberapa modul:
 * {@code ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa} (PSB) dan
 * {@code VerifikasiKelengkapanCalonPegawai} (rekrutmen). Salinan versi sekolah <b>lebih maju</b>:
 * ia memuat method query statis pembantu (pengecekan kelengkapan sebelum ujian/interview) di
 * dalam entity-nya sendiri, sementara di sini logika yang setara berada di luar, di
 * {@code VerifikasiPMBHelper}. Perbedaan letak logika ini berarti perbaikan bug pada salah satu
 * jalur tidak otomatis mengalir ke jalur lain.</p>
 *
 * <h3>Data awal otomatis (auto-seed)</h3>
 * <p>Dua mekanisme mengisi tabel ini tanpa campur tangan operator:</p>
 * <ul>
 * <li>{@code ais.common.InitData} mendaftarkan kelas ini ke {@code InitDataHelper.initData()}
 * saat aplikasi start.</li>
 * <li>{@code VerifikasiKelengkapanCalonMahasiswaAction.doAfterCompose()} menghitung
 * {@code rowCount}; bila <b>nol</b>, ia langsung menyimpan 14 baris syarat bawaan (ijazah, raport,
 * sertifikat prestasi, KTP/kartu pelajar, kartu SNMPTN, kartu keluarga, surat keterangan tidak
 * mampu, rekening listrik, foto rumah, kartu BIDIKMISI, dsb.) — teks yang jelas berasal dari satu
 * institusi tertentu. Baris hasil seed itu <b>tidak mengisi</b> {@code aktif}, {@code wajib},
 * maupun {@code verifikasi}, sehingga semuanya {@code null}; karena getter di kelas ini
 * mengembalikan {@code true} untuk {@code null} (lihat {@link #getAktif()}, {@link #getWajib()},
 * {@link #getVerifikasi()}), <b>ke-14 baris itu seketika berstatus aktif + wajib upload + wajib
 * verifikasi</b> begitu layar dibuka pertama kali.</li>
 * </ul>
 *
 * <h3>Jalur file &amp; catatan keamanan</h3>
 * <p>Ada <b>dua</b> tempat file menempel pada alur ini, keduanya lewat {@link
 * ais.database.model.file.LampiranLain}:</p>
 * <ul>
 * <li><b>Lampiran pada baris master ini sendiri</b> — layar CRUD memanggil
 * {@code LampiranLain.createDownloadUploadFileLain(hbox, getId(),
 * VerifikasiKelengkapanCalonMahasiswa.class.getName(), "Lampiran", ...)}, biasanya untuk
 * menempelkan formulir/template yang bisa diunduh pendaftar. Ini file publik-panitia, bukan data
 * pribadi.</li>
 * <li><b>Berkas milik pendaftar</b> — disimpan dengan {@code jenis} =
 * {@code BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.getName()} dan {@code ref} = id baris
 * penghubung tersebut ({@code FileFotoLain.createFileFotoLain(...)} di
 * {@code NewUiCandidateDocumentVerificationService.upload()}).</li>
 * </ul>
 * <p><b>SANGAT RELEVAN terhadap {@code SECURITY_FINDING_AmbilLampiran_IDOR.md}</b> (dokumen ada di
 * {@code ais/action/servlet/}). Berkas pendaftar pada kelompok kedua adalah baris
 * {@code LampiranLain} dengan {@code ref} = id berurutan {@link
 * BiodataCalonMahasiswaPunyaVerifikasiBerkas} dan {@code jenis} = nama kelas Java yang mudah
 * ditebak — persis bentuk yang dilaporkan rentan pada servlet {@code /al}
 * ({@code ais.action.servlet.AmbilLampiran}), yang menerima {@code ref}/{@code clazz}/{@code jenis}
 * sebagai parameter polos tanpa pemeriksaan kepemilikan. Isi berkas di jalur ini termasuk data
 * pribadi paling sensitif dalam sistem: fotocopy KTP/kartu keluarga, ijazah, surat keterangan tidak
 * mampu, rekening listrik, bahkan foto rumah pendaftar (lihat daftar auto-seed di atas). URL unduh
 * dibentuk {@code CommonMedia.getFile(lampiranLain.getId(), LampiranLain.class.getName())} —
 * misalnya pada kolom "Verifikasi Kelengkapan Berkas" di {@code CommonReportHelper} — dan
 * {@code NewUiCandidateDocumentVerificationService.downloadZip()} bahkan mengekspor seluruh
 * lampiran hasil filter ke satu ZIP.</p>
 *
 * <p>Pemeriksaan atas jalur PMB ini menunjukkan kelompok berkas di sini <b>bukan sekadar korban
 * tambahan</b> dari temuan tersebut, melainkan memperluasnya: berkas yang sudah ter-cache ke disk
 * disajikan lewat URL statis di dalam webapp ({@code FileFotoLain.createLinkUri()} &rarr;
 * {@code /f<ctx>/LampiranLain/<id>/<nama file>}) sehingga melewati servlet sepenuhnya, dan beberapa
 * JSP PMB memuat data pendaftar dari {@code request.getParameter("id")} tanpa mencocokkannya dengan
 * sesi. Rinciannya di luar cakupan file ini — <b>lihat/lengkapi
 * {@code SECURITY_FINDING_AmbilLampiran_IDOR.md}</b>. Entity ini sendiri tidak menyimpan file dan
 * bukan penyebab kelemahan tersebut, tetapi ia <b>mendefinisikan dan memperbesar radius
 * dampaknya</b>.</p>
 *
 * <p><b>Penjagaan layar CRUD.</b> {@code VerifikasiKelengkapanCalonMahasiswaAction} memanggil
 * {@code Common.doCheckSecurity()} di {@code doBeforeCompose()} dan memakai
 * {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)} untuk menyembunyikan tombol.
 * Perlu dicatat: halaman {@code /pages/master/verifikasi_kelengkapan_calon_mahasiswa.zul}
 * <b>tidak</b> termasuk dalam daftar {@code CommonPrivilages.MUST_CHECKED} (12 halaman hardcoded),
 * sehingga {@code doCheckSecurity()} pada praktiknya tidak menegakkan apa pun di layar ini —
 * instance lain dari temuan sistemik yang sudah dicatat inisiatif ini. Tombol unggah massal
 * ({@code Common.uploadData(...)}) pun hanya disembunyikan lewat {@code setVisible()}, tanpa
 * gerbang otorisasi sisi server pada endpoint unggahnya.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Identitas &amp; jejak audit:</b> {@link #getId()}/{@link #setId(Long)},
 * {@link #getOleh()}/{@link #setOleh(String)}, {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@code onUpdate()}.</li>
 * <li><b>Deskripsi syarat:</b> {@link #getNama()}/{@link #setNama(String)},
 * {@link #getKeterangan()}/{@link #setKeterangan(String)}, {@link #toString()}.</li>
 * <li><b>Flag dasar:</b> {@link #getAktif()}, {@link #getWajib()}, {@link #getVerifikasi()}
 * beserta setter-nya.</li>
 * <li><b>Flag gerbang tahapan:</b> {@link #getWajibUploadSebelumUjian()},
 * {@link #getWajibUploadSebelumInterview()}, {@link #getWajibVerifikasiSebelumUjian()},
 * {@link #getWajibVerifikasiSebelumInterview()} beserta setter-nya.</li>
 * </ul>
 *
 * <h3>Kuirk &amp; jebakan yang harus diketahui sebelum menyentuh kelas ini</h3>
 * <ol>
 * <li><b>{@link #getVerifikasi()} adalah getter yang MENULIS.</b> Bila {@link #getWajib()}
 * bernilai {@code false}, getter ini menetapkan field {@code verifikasi = false} sebelum
 * mengembalikan nilai. Karena Hibernate memetakan kelas ini lewat akses <i>property</i> dan
 * entity ber-{@code dynamicUpdate} berada di bawah dirty-checking, sekadar <i>membaca</i>
 * properti ini pada instance terkelola dapat memicu {@code UPDATE} (dan satu baris revisi Envers)
 * saat session di-flush. Pembacaan semacam itu benar-benar terjadi di dalam session terbuka,
 * mis. pada konstruktor {@code NewUiCandidateDocumentVerificationService.Row}. Lihat method
 * tersebut untuk rinciannya.</li>
 * <li><b>Default {@code null} berarti "ya".</b> {@code aktif}, {@code wajib}, dan
 * {@code verifikasi} membaca {@code null} sebagai {@code true} (paling ketat), sedangkan keempat
 * flag {@code wajib...Sebelum...} membaca {@code null} sebagai {@code false} (paling longgar).
 * Dua konvensi berlawanan di satu kelas yang sama — mudah salah asumsi saat menambah flag baru
 * atau saat menulis migrasi SQL.</li>
 * <li><b>{@link #setOleh(String)}/{@link #setOlehId(String)} menolak nilai kosong secara diam-diam</b>
 * — jejak audit tidak dapat dikosongkan kembali, dan pemanggil tidak diberi tahu bahwa nilainya
 * diabaikan.</li>
 * <li><b>Tidak ada {@code @PrePersist}.</b> Hanya {@code @PreUpdate} yang terpasang, jadi
 * {@code oleh}/{@code olehId} baris baru (termasuk seluruh baris hasil auto-seed) tetap
 * {@code null} kecuali pemanggil mengisinya sendiri.</li>
 * <li><b>Trim tidak simetris.</b> {@link #getNama()} men-trim saat baca tetapi
 * {@link #setNama(String)} menyimpan apa adanya, sedangkan {@link #getKeterangan()} tidak men-trim
 * sama sekali. Nilai di kolom DB karenanya boleh berspasi tepi, dan pencarian {@code ilike} atas
 * kolom mentah (mis. filter dokumen di layanan UI baru) bisa berperilaku berbeda dari tampilan.</li>
 * <li><b>{@link #toString()} melewati {@link #getNama()}</b> — ia membaca field {@code nama}
 * langsung sehingga hasilnya tidak ter-trim.</li>
 * <li><b>Header generator basi:</b> Javadoc asli hasil {@code hbm2java} berbunyi
 * {@code "Bank generated by hbm2java"} — sisa salin-tempel dari entity {@code Bank}. Nilai
 * {@link #serialVersionUID} pun identik dengan {@link BiodataCalonMahasiswaPunyaVerifikasiBerkas},
 * jejak salin-tempel yang sama.</li>
 * <li><b>Penamaan kolom campur.</b> Hanya {@code id}, {@code nama}, dan {@code keterangan} yang
 * punya {@code @Column} eksplisit. Enam properti Boolean tidak beranotasi, dan karena aplikasi
 * tidak menyetel {@code naming_strategy}, Hibernate memakai nama properti apa adanya
 * (camelCase, mis. {@code wajibUploadSebelumUjian}) — bukan {@code snake_case} seperti kolom
 * bertulis tangan di sekitarnya.</li>
 * </ol>
 *
 * <p><b>Catatan tentang kelas induk.</b> {@link GeneralValueObject} adalah POJO abstrak biasa —
 * <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate tidak
 * memetakan satu pun properti induknya. Deklarasi ulang {@code id}, {@code oleh}, {@code olehId},
 * dan {@code tanggal_dirubah} di kelas ini karena itu <b>bukan duplikasi yang keliru</b>,
 * melainkan keharusan teknis agar kolom-kolom tersebut ikut terpetakan. Jangan "membersihkan"-nya.</p>
 *
 * @see BiodataCalonMahasiswaPunyaVerifikasiBerkas
 * @see BiodataCalonMahasiswa
 * @see GelombangPendaftaran
 * @see JenisSeleksi
 * @see Berkas
 * @see ais.database.model.file.LampiranLain
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "verifikasi_kelengkapan_calon_mahasiswa")
public class VerifikasiKelengkapanCalonMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya kebetulan sama persis dengan
	 * {@link BiodataCalonMahasiswaPunyaVerifikasiBerkas#serialVersionUID} — jejak salin-tempel
	 * antar entity, bukan penanda kompatibilitas yang disengaja.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci primer, kolom {@code id} ({@code IDENTITY}/sequence PostgreSQL). Dideklarasikan ulang di
	 * sini karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private Long id;

	/**
	 * Nama tampil pengguna terakhir yang mengubah baris ini (kolom {@code oleh}). Diisi manual oleh
	 * pemanggil — tidak ada {@code @PrePersist} yang mengisinya otomatis.
	 */
	private String oleh;

	/**
	 * Id/username pengguna terakhir yang mengubah baris ini (kolom {@code olehId}). Sama seperti
	 * {@link #oleh}, hanya terisi bila pemanggil menyetelnya.
	 */
	private String olehId;

	/**
	 * Mengembalikan id/username pengubah terakhir apa adanya (boleh {@code null} bila baris belum
	 * pernah distempel).
	 *
	 * @return isi kolom {@code olehId}, atau {@code null}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id/username pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return} tanpa mengubah apa pun dan tanpa melempar exception).
	 * Akibatnya jejak audit yang sudah terisi tidak dapat dikosongkan kembali lewat setter ini.
	 * Berbeda dengan {@link BiodataCalonMahasiswaPunyaVerifikasiBerkas#setOlehId(String)}, nilai
	 * yang lolos <b>tidak</b> di-trim di sini.</p>
	 *
	 * @param olehId id/username pengubah; {@code null}/kosong/whitespace diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampil pengubah terakhir.
	 *
	 * <p>Berperilaku sama dengan {@link #setOlehId(String)}: {@code null}/kosong/whitespace
	 * diabaikan diam-diam, dan nilai yang lolos disimpan tanpa di-trim.</p>
	 *
	 * @param oleh nama tampil pengubah; {@code null}/kosong/whitespace diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengubah terakhir apa adanya.
	 *
	 * @return isi kolom {@code oleh}, atau {@code null} bila belum pernah distempel
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} dan deklarasi field {@link #tanggal_dirubah} — keduanya berada
	 * pada satu baris fisik yang sama (bentuk asli hasil penyisipan otomatis; jangan dirapikan tanpa
	 * alasan, agar diff tetap bersih).
	 *
	 * <p><b>{@code onUpdate()}</b> dipanggil Hibernate <i>tepat sebelum</i> setiap {@code UPDATE}
	 * baris ini dan meneruskan instance ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang menyegarkan stempel
	 * waktu perubahan. Tidak ada padanan {@code @PrePersist}, jadi pada {@code INSERT} stempel yang
	 * dipakai adalah nilai inisialisasi field di bawah. Method sengaja {@code protected}: hanya
	 * provider persistensi yang boleh memanggilnya, bukan kode aplikasi.</p>
	 *
	 * <p><b>{@code tanggal_dirubah}</b> menyimpan waktu perubahan terakhir (kolom
	 * {@code tanggal_dirubah}). Diinisialisasi saat objek dibuat dengan
	 * {@code ais.ui.util.WaktuUtil.getDate()} — jam server aplikasi yang sudah disesuaikan zona
	 * waktu, bukan {@code new Date()} mentah — sehingga baris baru tetap punya stempel meski
	 * {@code @PrePersist} tidak ada.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir secara manual.
	 *
	 * <p>Umumnya tidak perlu dipanggil kode aplikasi: {@code onUpdate()} sudah menyegarkannya
	 * otomatis pada setiap {@code UPDATE}. Setter ini terutama dipakai oleh Hibernate saat memuat
	 * baris dan oleh utilitas impor/migrasi yang ingin mempertahankan stempel asal.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin disimpan; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (dipetakan sebagai {@code TIMESTAMP}).
	 *
	 * <p>Dipakai antara lain sebagai kunci pengurutan daftar verifikasi berkas pada layanan UI baru
	 * ({@code NewUiCandidateDocumentVerificationService}, {@code Order.desc("tanggal_dirubah")}) —
	 * meski di sana yang diurutkan adalah kolom milik {@link BiodataCalonMahasiswaPunyaVerifikasiBerkas}.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru dibuat di
	 *         memori, namun bisa {@code null} bila kolom di DB kosong
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai luas oleh komponen ZK sebagai label item combobox/listbox dan oleh
	 * {@code RevisiHelper} pada layar CRUD. Perhatikan dua hal: (1) method ini membaca field
	 * {@code nama} <b>langsung</b>, bukan lewat {@link #getNama()}, sehingga spasi tepi tidak
	 * ter-trim; (2) {@code id} yang {@code null} (objek belum tersimpan) akan tercetak sebagai
	 * {@code "null-..."} — tidak melempar exception, tetapi tampak janggal di layar.</p>
	 *
	 * @return gabungan id dan nama syarat, dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama/teks syarat dokumen, mis. {@code "Satu (1) lembar fotocopy Kartu Keluarga yang telah
	 * dilegalisir pejabat yang berwenang"}. Kolom {@code nama}, tipe {@code text},
	 * {@code NOT NULL}. Praktiknya berisi kalimat panjang, bukan label pendek — pertimbangkan ini
	 * saat merancang lebar kolom grid atau ekspor.
	 */
	private String nama;

	/**
	 * Apakah dokumen ini <b>wajib diunggah</b> pendaftar. {@code null} diperlakukan sebagai
	 * {@code true} oleh {@link #getWajib()}. Mematikan flag ini juga memaksa {@link #verifikasi}
	 * menjadi {@code false} (lihat {@link #getVerifikasi()}).
	 */
	private Boolean wajib;

	/**
	 * Apakah item syarat ini masih dipakai. {@code null} diperlakukan sebagai {@code true} oleh
	 * {@link #getAktif()}. Item non-aktif dibuang dari daftar syarat efektif oleh
	 * {@code VerifikasiPMBHelper.isAktif()} dan tidak ikut dihitung pada gerbang mana pun.
	 */
	private Boolean aktif;

	/**
	 * Apakah dokumen yang sudah diunggah masih <b>harus diverifikasi petugas</b> sebelum dianggap
	 * lengkap. {@code null} diperlakukan sebagai {@code true} oleh {@link #getVerifikasi()}. Field
	 * ini bergantung pada {@link #wajib}: bila dokumen tidak wajib, field ini dipaksa
	 * {@code false} <i>di dalam getter-nya</i> — satu-satunya efek samping tulis di kelas ini.
	 */
	private Boolean verifikasi;

	/**
	 * Keterangan bebas/petunjuk pengisian yang ditampilkan mendampingi {@link #nama}. Kolom
	 * {@code keterangan}, tipe {@code text}, boleh {@code null}. Pada baris hasil auto-seed
	 * nilainya sengaja disalin sama persis dengan {@link #nama}.
	 */
	private String keterangan;

	/**
	 * Gerbang tahapan: bila {@code true}, pendaftar <b>tidak boleh mengikuti ujian</b> selama file
	 * untuk syarat ini belum diunggah. Diperiksa
	 * {@code VerifikasiPMBHelper.cekBerkasWajibUpload(..., untukUjian = true)}. {@code null}
	 * diperlakukan sebagai {@code false} (longgar).
	 */
	private Boolean wajibUploadSebelumUjian;

	/**
	 * Gerbang tahapan: bila {@code true}, pendaftar <b>tidak boleh mengikuti wawancara/interview</b>
	 * selama file untuk syarat ini belum diunggah. Diperiksa
	 * {@code VerifikasiPMBHelper.cekBerkasWajibUpload(..., untukUjian = false)}. {@code null}
	 * diperlakukan sebagai {@code false}.
	 */
	private Boolean wajibUploadSebelumInterview;

	/**
	 * Gerbang tahapan yang lebih ketat dari {@link #wajibUploadSebelumUjian}: bila {@code true},
	 * unggahan saja tidak cukup — berkas harus sudah <b>diverifikasi petugas</b>
	 * ({@code BiodataCalonMahasiswaPunyaVerifikasiBerkas.verified == true}) sebelum pendaftar boleh
	 * ikut ujian. {@code null} diperlakukan sebagai {@code false}.
	 */
	private Boolean wajibVerifikasiSebelumUjian;

	/**
	 * Padanan {@link #wajibVerifikasiSebelumUjian} untuk tahap wawancara/interview: berkas harus
	 * sudah diverifikasi petugas sebelum pendaftar boleh diwawancara. {@code null} diperlakukan
	 * sebagai {@code false}.
	 */
	private Boolean wajibVerifikasiSebelumInterview;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Tidak menyetel satu pun flag, jadi {@code aktif}/{@code wajib}/{@code verifikasi} bernilai
	 * {@code null} — yang oleh getter-nya dibaca sebagai {@code true}. Objek baru karenanya
	 * bersikap "paling ketat" secara default, sementara keempat flag {@code wajib...Sebelum...}
	 * bersikap "paling longgar". Satu-satunya nilai yang benar-benar diinisialisasi adalah
	 * {@code tanggal_dirubah}.</p>
	 */
	public VerifikasiKelengkapanCalonMahasiswa() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Selain sebagai identitas Hibernate, id ini dipakai sebagai <i>kunci map</i> pada
	 * {@code VerifikasiPMBHelper.ambilMapBerkas()} (memetakan syarat &rarr; baris status pendaftar)
	 * dan sebagai {@code ref} lampiran template pada layar CRUD
	 * ({@code LampiranLain.createDownloadUploadFileLain(..., getId(),
	 * VerifikasiKelengkapanCalonMahasiswa.class.getName(), "Lampiran", ...)}).</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer. Normalnya hanya dipanggil Hibernate; kode aplikasi cukup menyimpan
	 * objek dan membiarkan sequence mengisi nilainya.
	 *
	 * @param id kunci primer baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama/teks syarat dokumen, <b>sudah di-trim</b>.
	 *
	 * <p>Ini adalah nilai yang tampil di grid layar CRUD, di daftar centang pada layar Gelombang
	 * Pendaftaran/Jenis Seleksi, di pesan peringatan ketika pendaftar dicegah mengikuti
	 * ujian/interview, dan di kolom laporan cetak biodata. Juga menjadi kunci pengurutan efektif
	 * daftar syarat (lihat catatan pengurutan pada Javadoc kelas).</p>
	 *
	 * <p>Perhatikan asimetri: trim dilakukan saat <i>baca</i>, sedangkan
	 * {@link #setNama(String)} menyimpan nilai apa adanya. Filter pencarian yang menembak kolom DB
	 * langsung (mis. {@code Restrictions.ilike("v.nama", ...)}) karena itu bekerja atas nilai yang
	 * belum ter-trim.</p>
	 *
	 * @return nama syarat tanpa spasi tepi, atau {@code null} bila kolom kosong
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama/teks syarat dokumen.
	 *
	 * <p>Tidak melakukan trim maupun validasi panjang/keunikan. Pengecekan duplikat nama dilakukan
	 * di lapisan UI ({@code VerifikasiKelengkapanCalonMahasiswaAction.checkNamaVerifikasi
	 * KelengkapanCalonMahasiswa()}), bukan di sini — penyimpanan lewat jalur lain (impor massal,
	 * endpoint generik) tidak melewati pengecekan tersebut.</p>
	 *
	 * @param nama teks syarat; kolom {@code NOT NULL}, jadi {@code null} akan gagal saat flush
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/petunjuk pengisian apa adanya.
	 *
	 * <p>Berbeda dengan {@link #getNama()}, method ini <b>tidak</b> men-trim dan tidak menormalkan
	 * {@code null} menjadi string kosong — pemanggil harus siap menerima {@code null}. (Bandingkan
	 * {@link BiodataCalonMahasiswaPunyaVerifikasiBerkas#getKeterangan()} yang justru
	 * mengembalikan {@code ""}.)</p>
	 *
	 * @return isi kolom {@code keterangan}, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan/petunjuk pengisian.
	 *
	 * @param keterangan teks bebas; {@code null} diperbolehkan (kolom {@code nullable})
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif item syarat ini, dengan {@code null} dibaca sebagai {@code true}.
	 *
	 * <p>Konvensi "{@code null} = aktif" ini penting: baris lama maupun baris hasil auto-seed yang
	 * tidak pernah menyentuh kolom {@code aktif} tetap ikut menggigit sebagai syarat. Untuk
	 * menonaktifkan sebuah syarat, nilainya harus <b>ditulis eksplisit</b> {@code false}.</p>
	 *
	 * <p>Dikonsumsi {@code VerifikasiPMBHelper.isAktif()}, {@code CommonReportHelper}, dan layar
	 * CRUD (checkbox "Aktif"). Item non-aktif dibuang dari daftar syarat efektif dan karenanya
	 * tidak lagi memblokir ujian/interview.</p>
	 *
	 * @return {@code true} bila item masih dipakai (termasuk saat kolom {@code null});
	 *         {@code false} hanya bila disetel eksplisit {@code false}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif item syarat.
	 *
	 * <p>Dipanggil dari checkbox "Aktif" pada renderer grid layar CRUD, yang langsung menyusulnya
	 * dengan {@code Common.refreshSaveOrUpdate(...)} — perubahan tersimpan seketika saat dicentang,
	 * tanpa tombol Simpan.</p>
	 *
	 * @param aktif {@code false} untuk menonaktifkan; {@code null} berefek sama dengan {@code true}
	 *              saat dibaca kembali
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan status "wajib diunggah", dengan {@code null} dibaca sebagai {@code true}.
	 *
	 * <p>Menjadi prasyarat bagi {@link #getVerifikasi()}: selama nilainya {@code false}, kewajiban
	 * verifikasi ikut dimatikan. Ditampilkan sebagai checkbox "Wajib Upload" di layar CRUD dan
	 * sebagai label "Wajib"/"Tidak" pada layar verifikasi berkas per pendaftar.</p>
	 *
	 * <p><b>Catatan:</b> flag ini <i>tidak</i> dipakai oleh gerbang ujian/interview — gerbang itu
	 * memakai {@link #getWajibUploadSebelumUjian()}/{@link #getWajibUploadSebelumInterview()}. Jadi
	 * sebuah syarat bisa saja bertanda "Wajib" namun tidak menghalangi tahapan apa pun.</p>
	 *
	 * @return {@code true} bila dokumen wajib diunggah (termasuk saat kolom {@code null})
	 */
	public Boolean getWajib() {
		return wajib == null ? true : wajib;
	}

	/**
	 * Menyetel status "wajib diunggah".
	 *
	 * <p>Pada layar CRUD, mematikan checkbox ini juga menonaktifkan dan mengosongkan checkbox
	 * "Wajib Verifikasi" di sisi UI; penegakan yang setara di sisi model dilakukan oleh
	 * {@link #getVerifikasi()}.</p>
	 *
	 * @param wajib {@code false} bila dokumen bersifat opsional
	 */
	public void setWajib(Boolean wajib) {
		this.wajib = wajib;
	}

	/**
	 * Mengembalikan status "wajib diverifikasi petugas", dengan {@code null} dibaca sebagai
	 * {@code true}.
	 *
	 * <p><b>PERHATIAN — getter ini memiliki efek samping tulis.</b> Bila {@link #getWajib()}
	 * bernilai {@code false}, method ini terlebih dahulu menetapkan field {@code verifikasi} menjadi
	 * {@code false} sebelum mengembalikan nilai. Ini menegakkan invarian "dokumen yang tidak wajib
	 * diunggah tidak mungkin wajib diverifikasi", tetapi caranya tidak lazim:</p>
	 * <ul>
	 * <li>Kelas ini dipetakan Hibernate lewat <b>akses property</b> (anotasi berada di getter), dan
	 * entity ditandai {@code dynamicUpdate} serta {@code @Audited}. Pada instance yang masih
	 * terkelola sebuah session, mengubah field lewat getter menjadikan entity <i>dirty</i>, sehingga
	 * flush berikutnya dapat mengeluarkan {@code UPDATE} <b>plus</b> satu baris revisi Envers —
	 * padahal pemanggil merasa hanya membaca.</li>
	 * <li>Pembacaan semacam itu benar-benar terjadi di dalam session terbuka, mis. pada konstruktor
	 * {@code NewUiCandidateDocumentVerificationService.Row} yang memanggil
	 * {@code x.getVerifikasiKelengkapanCalonMahasiswa().getVerifikasi()} sementara session masih
	 * hidup dan masih akan menjalankan query berikutnya (yang memicu auto-flush).</li>
	 * <li>Efeknya <b>satu arah</b>: nilai {@code true} tidak pernah dipulihkan saat
	 * {@link #getWajib()} dinyalakan kembali. Menonaktifkan lalu mengaktifkan ulang "Wajib Upload"
	 * dapat menghilangkan pengaturan "Wajib Verifikasi" secara permanen tanpa pemberitahuan.</li>
	 * </ul>
	 * <p>Jangan mengandalkan method ini sebagai getter murni, dan jangan memanggilnya di dalam
	 * perulangan atas entity terkelola bila {@code UPDATE} tak diinginkan.</p>
	 *
	 * @return {@code true} bila berkas harus diverifikasi petugas (termasuk saat kolom {@code null});
	 *         selalu {@code false} bila dokumen tidak wajib diunggah
	 */
	public Boolean getVerifikasi() {
		if (!getWajib()) {
			verifikasi = false;
		}
		return verifikasi == null ? true : verifikasi;
	}

	/**
	 * Menyetel status "wajib diverifikasi petugas".
	 *
	 * <p>Nilai yang disetel di sini dapat ditimpa kembali menjadi {@code false} oleh
	 * {@link #getVerifikasi()} bila {@link #getWajib()} bernilai {@code false} — setter ini tidak
	 * memvalidasi konsistensi itu sendiri.</p>
	 *
	 * @param verifikasi {@code true} bila unggahan masih harus disetujui petugas
	 */
	public void setVerifikasi(Boolean verifikasi) {
		this.verifikasi = verifikasi;
	}

	/**
	 * Mengembalikan flag "wajib diunggah sebelum ujian", dengan {@code null} dibaca sebagai
	 * {@code false}.
	 *
	 * <p>Dibaca {@code VerifikasiPMBHelper.cekBerkasWajibUpload(biodata, untukUjian = true)} yang
	 * dipanggil lewat {@code ambilPesanGagalSebelumUjian()}/{@code checkVerifikasiSebelumUjian()}
	 * sebelum pendaftar diizinkan menekan tombol "Ikut Ujian". Bila flag menyala dan lampiran
	 * belum ada, helper mengembalikan pesan panduan langkah demi langkah dan tahapan dibatalkan.</p>
	 *
	 * <p>Perhatikan konvensi {@code null}-nya <b>berlawanan</b> dengan {@link #getAktif()},
	 * {@link #getWajib()}, dan {@link #getVerifikasi()}: di sini {@code null} berarti "tidak
	 * memblokir".</p>
	 *
	 * @return {@code true} bila ketiadaan berkas ini memblokir ujian
	 */
	public Boolean getWajibUploadSebelumUjian() {
		return wajibUploadSebelumUjian == null ? false : wajibUploadSebelumUjian;
	}

	/**
	 * Menyetel flag "wajib diunggah sebelum ujian".
	 *
	 * <p>Dipanggil dari checkbox "Wajib Upload sebelum Ujian" pada renderer grid layar CRUD, yang
	 * langsung menyimpan perubahan lewat {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * @param wajibUploadSebelumUjian {@code true} untuk memblokir ujian bila berkas belum diunggah
	 */
	public void setWajibUploadSebelumUjian(Boolean wajibUploadSebelumUjian) {
		this.wajibUploadSebelumUjian = wajibUploadSebelumUjian;
	}

	/**
	 * Mengembalikan flag "wajib diunggah sebelum wawancara/interview", dengan {@code null} dibaca
	 * sebagai {@code false}.
	 *
	 * <p>Padanan {@link #getWajibUploadSebelumUjian()} untuk tahap interview; dibaca
	 * {@code VerifikasiPMBHelper.cekBerkasWajibUpload(biodata, untukUjian = false)} melalui
	 * {@code ambilPesanGagalSebelumInterview()}/{@code checkVerifikasiSebelumInterview()}.</p>
	 *
	 * @return {@code true} bila ketiadaan berkas ini memblokir interview
	 */
	public Boolean getWajibUploadSebelumInterview() {
		return wajibUploadSebelumInterview == null ? false : wajibUploadSebelumInterview;
	}

	/**
	 * Menyetel flag "wajib diunggah sebelum wawancara/interview".
	 *
	 * <p>Dipanggil dari checkbox "Wajib Upload sebelum Interview" pada renderer grid layar CRUD dan
	 * disimpan seketika.</p>
	 *
	 * @param wajibUploadSebelumInterview {@code true} untuk memblokir interview bila berkas belum
	 *                                    diunggah
	 */
	public void setWajibUploadSebelumInterview(Boolean wajibUploadSebelumInterview) {
		this.wajibUploadSebelumInterview = wajibUploadSebelumInterview;
	}

	/**
	 * Mengembalikan flag "wajib diverifikasi petugas sebelum ujian", dengan {@code null} dibaca
	 * sebagai {@code false}.
	 *
	 * <p>Lebih ketat daripada {@link #getWajibUploadSebelumUjian()}: berkas harus sudah ada
	 * <i>dan</i> {@code BiodataCalonMahasiswaPunyaVerifikasiBerkas.verified} bernilai {@code true}.
	 * Perlu dicatat, jalur pemeriksaan gabungan di {@code VerifikasiPMBHelper} menguji status
	 * verifikasi memakai {@link #getVerifikasi()} (flag umum), sedangkan flag khusus tahap ini
	 * dibaca terpisah — sehingga kombinasi keduanya harus disetel konsisten oleh operator agar
	 * gerbang berperilaku seperti yang diharapkan.</p>
	 *
	 * @return {@code true} bila berkas yang belum diverifikasi memblokir ujian
	 */
	public Boolean getWajibVerifikasiSebelumUjian() {
		return wajibVerifikasiSebelumUjian == null ? false : wajibVerifikasiSebelumUjian;
	}

	/**
	 * Menyetel flag "wajib diverifikasi petugas sebelum ujian".
	 *
	 * <p>Dipanggil dari checkbox "Wajib Verifikasi sebelum Ujian" pada renderer grid layar CRUD dan
	 * disimpan seketika.</p>
	 *
	 * @param wajibVerifikasiSebelumUjian {@code true} untuk mensyaratkan verifikasi petugas sebelum
	 *                                    ujian
	 */
	public void setWajibVerifikasiSebelumUjian(Boolean wajibVerifikasiSebelumUjian) {
		this.wajibVerifikasiSebelumUjian = wajibVerifikasiSebelumUjian;
	}

	/**
	 * Mengembalikan flag "wajib diverifikasi petugas sebelum wawancara/interview", dengan
	 * {@code null} dibaca sebagai {@code false}.
	 *
	 * <p>Padanan {@link #getWajibVerifikasiSebelumUjian()} untuk tahap interview.</p>
	 *
	 * @return {@code true} bila berkas yang belum diverifikasi memblokir interview
	 */
	public Boolean getWajibVerifikasiSebelumInterview() {
		return wajibVerifikasiSebelumInterview == null ? false : wajibVerifikasiSebelumInterview;
	}

	/**
	 * Menyetel flag "wajib diverifikasi petugas sebelum wawancara/interview".
	 *
	 * <p>Dipanggil dari checkbox "Wajib Verifikasi sebelum Interview" pada renderer grid layar CRUD
	 * dan disimpan seketika.</p>
	 *
	 * @param wajibVerifikasiSebelumInterview {@code true} untuk mensyaratkan verifikasi petugas
	 *                                        sebelum interview
	 */
	public void setWajibVerifikasiSebelumInterview(Boolean wajibVerifikasiSebelumInterview) {
		this.wajibVerifikasiSebelumInterview = wajibVerifikasiSebelumInterview;
	}

}
