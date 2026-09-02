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
 * Entity <b>dispensasi (pengecualian) syarat PKL</b>: satu baris tabel
 * {@code public.pengecualian_pkl_mahasiswa} menyatakan bahwa seorang
 * {@link Mahasiswa} <b>dibebaskan dari seluruh pemeriksaan kelayakan akademis</b> untuk satu
 * program {@link Pkl} tertentu, disertai {@link #getKeterangan() keterangan} alasan bebas-teks.
 *
 * <p>Secara struktur kelas ini adalah entity penghubung (junction) {@code Mahasiswa} &times;
 * {@code Pkl} dengan satu muatan tambahan ({@code keterangan}). Secara <i>makna</i> ia jauh lebih
 * dari sekadar tabel relasi: <b>keberadaan barisnya saja sudah merupakan keputusan bisnis</b> yang
 * mematikan seluruh gerbang kelayakan pendaftaran PKL. Karena itu tabel ini sebaiknya
 * diperlakukan sebagai "surat sakti" &mdash; bukan data master biasa.</p>
 *
 * <h2>Efek nyata satu baris di tabel ini</h2>
 * <p>Satu-satunya pembaca fungsional entity ini adalah {@code ais.common.Common.checkSyaratPkl
 * (Mahasiswa, Pkl)}. Method tersebut membuka dirinya dengan:</p>
 * <pre>
 * int kecuali = rowCount(PengecualianPklMahasiswa WHERE pkl = :pkl AND mahasiswa = :mahasiswa);
 * if (kecuali &gt; 0) {
 *     return true;   // <b>keluar lebih awal</b>
 * }
 * </pre>
 * <p>Karena pemeriksaan ini adalah <b>baris pertama</b> method, adanya baris di sini melewati
 * <b>semua</b> pemeriksaan berikutnya sekaligus, bukan hanya SKS/IPK:</p>
 * <ol>
 * <li><b>Kecocokan jurusan</b> &mdash; mahasiswa dari prodi lain tetap bisa didaftarkan ke program
 * PKL milik prodi tertentu.</li>
 * <li><b>Kecocokan fakultas</b> &mdash; idem untuk lintas fakultas.</li>
 * <li><b>Sinkronisasi &amp; perhitungan KRS</b> ({@code Common.singkronkanKrsMahasiswa}) &mdash;
 * bahkan tidak dijalankan, sehingga SKS dan IPK mahasiswa tidak pernah dihitung.</li>
 * <li><b>Ambang syarat utama</b> {@link Pkl#getMinimalSksBolehIkutPkl()} dan
 * {@link Pkl#getMinimalIpkBolehIkutPkl()}.</li>
 * <li><b>Ambang syarat alternatif</b> {@link Pkl#getAktifkanSyaratLain()} /
 * {@link Pkl#getMinimalSksBolehIkutPkl2()} / {@link Pkl#getMinimalIpkBolehIkutPkl2()}.</li>
 * </ol>
 * <p><b>Yang TIDAK dilewati:</b> pemeriksaan tagihan/pembayaran. Di
 * {@code ais.action.master.pkl.PklUntukMahasiswaAction} blok {@code pkl.getHarusBayar()} &rarr;
 * {@code Common.checkStatusPembayaranMahasiswa(...)} dijalankan <b>sebelum</b>
 * {@code checkSyaratPkl}, jadi pengecualian di sini tidak membebaskan mahasiswa dari kewajiban
 * bayar. Pembebasan finansial ditangani mekanisme lain &mdash; daftar putih
 * {@link Pkl#getNimMhsTanpaBiaya()} dan entity {@link BaypassPembayaranMahasiswa}.</p>
 *
 * <h2>Hubungan dengan bug default syarat SKS/IPK di {@link Pkl}</h2>
 * <p>{@link Pkl} menyimpan jebakan yang sudah didokumentasikan di kelas tersebut: untuk baris PKL
 * lama yang kolom {@code minimalSksBolehIkutPkl2} / {@code minimalIpkBolehIkutPkl2}-nya masih
 * {@code NULL}, getter men-default-kannya menjadi {@code 0} dan {@code 0.0}. Bila pada baris
 * seperti itu {@link Pkl#getAktifkanSyaratLain()} dinyalakan <b>tanpa mengisi angkanya</b>, syarat
 * alternatif menjadi {@code sks >= 0 && ipk >= 0.0} yang <b>selalu benar</b> &mdash; artinya
 * <b>seluruh pendaftar lolos</b>. Bug identik ada di {@link Kkn}.</p>
 * <p>Kaitannya dengan kelas ini ada dua sisi, dan keduanya perlu disadari bersama:</p>
 * <ul>
 * <li><b>Kelas ini adalah jalur sah yang seharusnya dipakai.</b> Bila seorang mahasiswa perlu
 * dibebaskan dari ambang SKS/IPK, cara yang benar adalah menambahkan satu baris <i>di sini</i>
 * (terarah pada satu mahasiswa, dengan keterangan alasan), <b>bukan</b> menurunkan atau
 * mengaktifkan ambang alternatif di {@link Pkl} &mdash; karena yang terakhir itu berdampak ke
 * <b>semua</b> pendaftar program tersebut, bukan ke satu orang.</li>
 * <li><b>Kelas ini membuat bug itu sulit terdeteksi.</b> Karena pemeriksaan pengecualian keluar
 * lebih awal, mahasiswa yang punya baris di sini tetap lolos apa pun kondisi ambangnya. Jadi
 * kalau audit "siapa saja yang lolos padahal tidak memenuhi syarat" dilakukan, hasilnya bercampur
 * antara dispensasi sah (baris di tabel ini) dan kebocoran akibat bug ambang di {@link Pkl}.
 * Untuk memisahkannya, bandingkan daftar pendaftar diterima dengan isi tabel ini.</li>
 * </ul>
 * <p><b>Efek samping penting yang kemungkinan tidak disengaja:</b> karena keluar lebih awal terjadi
 * sebelum pemeriksaan jurusan/fakultas, entity yang <i>namanya</i> hanya menjanjikan pengecualian
 * syarat akademis ternyata juga membobol batasan sasaran peserta program. Ini
 * <b>tidak diperbaiki</b> di sini, hanya dicatat.</p>
 *
 * <h2>Perbandingan dengan {@link PengecualianKknMahasiswa}</h2>
 * <p>Kelas ini adalah <b>kembaran salin-tempel</b> dari {@link PengecualianKknMahasiswa}: struktur
 * field, anotasi, dan perannya identik &mdash; hanya relasi induknya yang berganti dari
 * {@link Kkn} ke {@link Pkl}, dan tabelnya dari {@code pengecualian_kkn_mahasiswa} ke
 * {@code pengecualian_pkl_mahasiswa}. Kesejajaran itu berlanjut sampai ke lapisan atas:
 * {@code Common.checkSyaratKkn} / {@code Common.checkSyaratPkl} berpasangan,
 * {@code PengecualianKknMahasiswaHelper} / {@code PengecualianPklMahasiswaHelper} berpasangan, dan
 * kunci konfigurasi {@code tampilkan_pengecualian_kkn_mahasiswa_di_seleksi} /
 * {@code tampilkan_pengecualian_pkl_mahasiswa_di_seleksi} berpasangan. <b>Konsekuensi
 * pemeliharaan:</b> perbaikan pada salah satu sisi hampir selalu perlu diterapkan juga ke sisi
 * lainnya.</p>
 * <p>Jejak salin-tempel itu masih terlihat pada lapisan UI PKL, dan <b>tidak dibetulkan</b> di
 * pekerjaan dokumentasi ini: judul jendela {@code PengecualianPklMahasiswaHelper.display()}
 * berbunyi "Daftar Pengecualian <b>KKN</b> mahasiswa", dan pesan penolakan jurusan/fakultas di
 * {@code Common.checkSyaratPkl} berbunyi "tidak bisa mendaftar di <b>KKN</b> di ..." padahal
 * konteksnya PKL.</p>
 *
 * <h2>Siapa yang membuat, membaca, dan menghapus baris di sini</h2>
 * <ol>
 * <li><b>Pembuatan (hanya satu jalur).</b>
 * {@code ais.action.master.helper.PengecualianPklMahasiswaHelper}, sebuah jendela modal yang
 * dibuka dari tombol "Pengecualian" pada layar seleksi penerima PKL
 * ({@code ais.action.master.pkl.SeleksiPenerimaPklAction} &rarr;
 * {@code ais.action.master.helper.PendaftarPklHelper}). Tombol "Ambil Data Mahasiswa" di jendela
 * itu membuka pemilih massal {@code AmbilDataMahasiswaBanyak}; setiap mahasiswa yang dicentang
 * langsung menjadi satu baris entity ini dengan {@code keterangan} diisi string kosong. Tidak ada
 * jalur pembuatan lain di seluruh repo &mdash; tidak lewat API, servlet, importir Excel, maupun
 * layar mahasiswa.</li>
 * <li><b>Pembacaan fungsional (hanya satu jalur).</b> {@code Common.checkSyaratPkl}, yang dipanggil
 * dari dua tempat: {@code ais.action.master.pkl.PklUntukMahasiswaAction} (pendaftaran, termasuk
 * jalur mahasiswa sendiri) dan {@code ais.action.master.helper.AmbilDataMahasiswaSeleksiPklHelper}
 * (pendaftaran massal oleh petugas).</li>
 * <li><b>Pembacaan tampilan.</b> {@code PengecualianPklMahasiswaHelper.loadData(Object)} mengisi
 * grid jendela modal dengan baris milik satu {@code Pkl}, disaring nama/fakultas/prodi dan
 * dibatasi {@code Common.MAX_RESULT_50}.</li>
 * <li><b>Perubahan.</b> Hanya kolom {@code keterangan}, lewat textbox inline pada grid
 * ({@code onChange} &rarr; {@code Common.refreshSaveOrUpdate}). Mahasiswa dan PKL sebuah baris
 * tidak pernah bisa diubah setelah dibuat.</li>
 * <li><b>Penghapusan.</b> Tombol "Hapus" per baris (dengan dialog konfirmasi) &rarr;
 * {@code Common.refreshDelete}. Penghapusan baris adalah <b>satu-satunya cara mencabut</b>
 * dispensasi &mdash; tidak ada flag "batal"/"kedaluwarsa" di entity ini.</li>
 * <li><b>Tidak ada pembaca lain.</b> Entity ini tidak diekspor ke Feeder/PDDikti, tidak muncul di
 * dasbor mana pun, tidak masuk cache JSON mahasiswa, dan tidak dipakai laporan cetak. Puluhan
 * berkas {@code ais/common/Common*Helper.java} memang meng-{@code import} kelas ini, tetapi itu
 * hanya blok import warisan hasil pemecahan {@code Common.java}; hanya {@code Common.java} sendiri
 * yang benar-benar memakainya.</li>
 * </ol>
 *
 * <h2>Tata kelola: apa yang TIDAK ada di alur ini</h2>
 * <p>Bagian ini sengaja ditulis panjang karena entity ini memberikan pembebasan syarat, sehingga
 * pertanyaan "siapa yang boleh, dan bagaimana dibuktikan" lebih penting daripada strukturnya.
 * Semua poin di bawah diverifikasi dari kode, bukan diasumsikan:</p>
 * <ul>
 * <li><b>Tidak ada alur persetujuan.</b> Entity ini tidak punya kolom status, pengaju, penyetuju,
 * tanggal disetujui, maupun masa berlaku. Baris langsung berlaku pada detik ia disimpan
 * ({@code session.save}) &mdash; tidak ada tahap "diajukan" yang menunggu verifikasi.</li>
 * <li><b>Tidak ada batasan lingkup operator.</b> Dialog {@code AmbilDataMahasiswaBanyak} menyusun
 * Criteria-nya hanya dengan filter status aktif dan teks bebas (angkatan, nama, prodi); tidak ada
 * satu pun predikat yang mengunci hasil ke fakultas/prodi milik operator yang sedang login.
 * Artinya operator prodi mana pun dapat memberi dispensasi kepada mahasiswa mana pun di
 * <b>seluruh institusi</b>.</li>
 * <li><b>Tombol hanya dijaga di sisi UI.</b> Tombol "Pengecualian" di {@code PendaftarPklHelper}
 * dikendalikan {@code pengecualian.setVisible(tbmuser.getMahasiswa() == null &&
 * Common.bolehKonfigurasi("tampilkan_pengecualian_pkl_mahasiswa_di_seleksi"))}. Keduanya bukan
 * pemeriksaan hak akses: yang pertama sekadar "akun ini bukan akun mahasiswa", yang kedua sebuah
 * saklar konfigurasi <b>global</b> (didaftarkan di {@code KonfigurasiNewAction} dengan default
 * AKTIF) yang berlaku sama untuk semua pengguna. Di dalam
 * {@code PengecualianPklMahasiswaHelper} sendiri &mdash; konstruktor, {@code display()},
 * {@code loadData()}, listener simpan, dan listener hapus &mdash; <b>tidak ada satu pun</b>
 * pemanggilan {@code CommonPrivilages.checkPrevilages(...)}.</li>
 * <li><b>Pembuatan massal tanpa gerbang otorisasi server-side.</b> Satu kali klik pada
 * {@code AmbilDataMahasiswaBanyak} dapat mencentang hingga {@code Common.MAX_RESULT} mahasiswa
 * dan menyimpan sebanyak itu baris pengecualian dalam satu loop, tanpa pemeriksaan hak akses,
 * tanpa batas jumlah, dan tanpa konfirmasi jumlah.</li>
 * <li><b>Inversi hak akses yang mencolok.</b> Pada layar yang sama
 * ({@code PendaftarPklHelper}), tindakan yang <i>lebih ringan</i> justru dijaga lebih ketat:
 * checkbox "terima/tolak" pendaftar dinonaktifkan bila pengguna tidak punya
 * {@code CommonPrivilages.APPROVE} ({@code labelTelahTerpenuhi.setDisabled(!approve)}, nilainya
 * berasal dari {@code SeleksiPenerimaPklAction}). Tombol "Pengecualian" &mdash; yang membebaskan
 * mahasiswa dari <i>seluruh</i> syarat &mdash; <b>tidak melihat {@code approve} sama sekali</b>.
 * Pengguna tanpa hak APPROVE karena itu tidak dapat meluluskan satu pendaftar, tetapi dapat
 * membuat pendaftar mana pun otomatis lolos.</li>
 * <li><b>Jejak audit pembuat baris kosong.</b> Kolom {@code oleh}/{@code olehId} hanya diisi oleh
 * hook {@code @PreUpdate} {@link #onUpdate()}; tidak ada hook {@code @PrePersist}. Karena baris
 * dibuat lewat {@code session.save} dan tidak pernah di-{@code UPDATE} kecuali keterangannya
 * disunting belakangan, <b>baris pengecualian yang dibuat lalu dibiarkan apa adanya berakhir
 * dengan {@code oleh}/{@code olehId} bernilai {@code NULL}</b> &mdash; tidak tercatat siapa yang
 * memberikan dispensasi. Envers ({@code @Audited}) memang merekam versi ke
 * {@code new_audit.pengecualian_pkl_mahasiswa__audit}, tetapi repo ini memakai
 * {@code DefaultRevisionEntity} bawaan (tidak ada {@code @RevisionEntity} kustom), sehingga
 * revisinya hanya menyimpan nomor dan waktu &mdash; <b>bukan identitas pengguna</b>.</li>
 * </ul>
 * <p><b>Perbandingan dengan {@link BaypassPembayaranMahasiswa}.</b> Pola di atas &mdash; tanpa
 * persetujuan, tanpa lingkup operator, pembuatan massal tanpa gerbang server-side &mdash; adalah
 * pola yang sama dengan mekanisme bypass pembayaran, dan di sini <b>diperberat</b> oleh jejak
 * audit pembuat yang kosong serta inversi hak akses terhadap APPROVE. Perbedaannya: dampak entity
 * ini bersifat akademis (kelayakan mengikuti PKL, dan sebagai efek samping juga batasan
 * jurusan/fakultas), bukan finansial langsung. Skala kerusakan karena itu lebih kecil, tetapi
 * bentuk kelemahannya identik.</p>
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 * <p>Kelas induk <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}, melainkan POJO
 * abstrak biasa; Hibernate <b>tidak memetakan properti milik induk</b>. Karena itu deklarasi ulang
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini
 * <b>bukan duplikasi yang keliru, melainkan keharusan teknis</b> &mdash; tanpa deklarasi ulang,
 * kolom-kolom tersebut tidak akan pernah dipetakan. Pola yang sama muncul di hampir semua entity
 * repo ini. Yang benar-benar diwarisi dari induk adalah kumpulan utilitas statis (mis.
 * {@link GeneralValueObject#check(Object)}) &mdash; namun perhatikan bahwa <b>kelas ini tidak
 * memakai satu pun di antaranya</b>: kedua getter relasinya mengembalikan field mentah tanpa
 * resolusi proxy.</p>
 *
 * <h2>Pemetaan Hibernate</h2>
 * <p>{@code @Entity} + {@code @Table(schema = "public", name = "pengecualian_pkl_mahasiswa")},
 * {@code dynamicInsert}/{@code dynamicUpdate} aktif (hanya kolom yang benar-benar berubah ikut
 * dalam {@code INSERT}/{@code UPDATE}), dan {@code @Audited} sehingga setiap perubahan direkam
 * Hibernate Envers ke tabel bayangan {@code new_audit.pengecualian_pkl_mahasiswa__audit}
 * (properti {@code envers.default_schema}/{@code envers.audit_table_suffix} di
 * {@code hibernate.cfg.xml}); kelas didaftarkan sebagai {@code <mapping class=...>} di berkas
 * konfigurasi yang sama.</p>
 * <p>Pemetaan memakai <b>property access</b> (anotasi {@code @Id} menempel pada {@link #getId()}),
 * sehingga <b>setiap pasangan getter/setter yang tidak dianotasi {@code @Transient} tetap
 * dipetakan</b> &mdash; dan di kelas ini <b>tidak ada satu pun {@code @Transient}</b>. Karena
 * {@code ais.database.hibernate.MyNamingStrategy} adalah turunan {@code DefaultNamingStrategy}
 * (nama kolom = nama properti apa adanya), properti tanpa {@code @Column} jatuh ke kolom bernama
 * persis seperti propertinya: {@code oleh}, {@code olehId}, {@code tanggal_dirubah}.</p>
 * <p>Kedua relasi memakai {@code @ManyToOne} dengan {@code cascade = {PERSIST, MERGE}} dan
 * {@code @Fetch(FetchMode.SELECT)}. Konsekuensi cascade yang perlu disadari: menyimpan atau
 * me-{@code merge} sebuah baris pengecualian ikut mem-{@code persist}/{@code merge} object
 * {@link Mahasiswa} dan {@link Pkl} yang menempel padanya &mdash; termasuk perubahan yang tidak
 * disengaja pada kedua object tersebut. {@code CascadeType.REMOVE} sengaja tidak ada, sehingga
 * menghapus baris pengecualian <b>tidak</b> menghapus mahasiswa atau programnya.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 * <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan hook {@code @PreUpdate}
 * {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> &mdash; {@link #getId()}/{@link #setId(Long)}, konstruktor tanpa argumen
 * {@link #PengecualianPklMahasiswa()}, dan {@link #toString()}.</li>
 * <li><b>Muatan bisnis</b> &mdash; {@link #getKeterangan()}/{@link #setKeterangan(String)}: alasan
 * dispensasi, satu-satunya kolom data (bukan relasi, bukan audit) di kelas ini.</li>
 * <li><b>Relasi</b> &mdash; {@link #getMahasiswa()}/{@link #setMahasiswa(Mahasiswa)} dan
 * {@link #getPkl()}/{@link #setPkl(Pkl)}: pasangan yang membentuk makna baris.</li>
 * </ol>
 * <p><b>Tidak ada</b> method utilitas statis, query bawaan, konstanta status, maupun method
 * bisnis di kelas ini &mdash; seluruh logikanya ada di {@code Common.checkSyaratPkl} dan di
 * helper UI.</p>
 *
 * <h2>Verifikasi pola berulang repo (hasil: kelas ini BERSIH)</h2>
 * <p>Beberapa entity repo ini punya getter yang tidak polos. Ketiga pola tersebut diperiksa satu
 * per satu pada kode kelas ini dan <b>tidak satu pun ditemukan</b>:</p>
 * <ul>
 * <li><b>Getter yang menulis balik ke field / ke database</b> &mdash; tidak ada. Keenam getter
 * ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, {@code keterangan},
 * {@code mahasiswa}, {@code pkl}) hanya melakukan {@code return field;} tanpa cabang default,
 * normalisasi, maupun penugasan.</li>
 * <li><b>Getter yang membuka atau menutup {@code Session} Hibernate</b> &mdash; tidak ada. Kelas
 * ini bahkan tidak meng-{@code import} {@code HibernateUtil}, dan tidak memanggil
 * {@link GeneralValueObject#check(Object)} yang di entity lain menjadi jalur akses DB implisit.
 * Satu-satunya rujukan ke lapisan persistensi adalah pemanggilan interceptor di
 * {@link #onUpdate()}.</li>
 * <li><b>Getter destruktif</b> (membuang atau meng-{@code null}-kan nilai saat dibaca) &mdash;
 * tidak ada.</li>
 * </ul>
 * <p>Yang <i>tidak</i> polos justru dua <b>setter</b>-nya: {@link #setOleh(String)} dan
 * {@link #setOlehId(String)} mengabaikan nilai {@code null}/kosong secara diam-diam. Lihat
 * masing-masing method.</p>
 *
 * <h2>Kuirk yang perlu diketahui sebelum menyunting</h2>
 * <ul>
 * <li><b>Komentar generator salah.</b> Javadoc kelas hasil {@code hbm2java} semula berbunyi
 * "Bank generated by hbm2java" &mdash; sisa salin-tempel dari entity lain; kelas ini tidak ada
 * hubungannya dengan bank.</li>
 * <li><b>{@link #toString()} memakai field mentah {@code keterangan}.</b> Karena jalur pembuatan
 * satu-satunya mengisi {@code keterangan} dengan string kosong, {@code toString()} baris baru
 * mengembalikan {@code ""}; untuk baris yang keterangannya belum pernah diisi lewat jalur lain ia
 * bisa mengembalikan {@code null}. Beberapa komponen ZK memanggil {@code toString()} secara
 * implisit.</li>
 * <li><b>Tidak ada kunci unik alami.</b> Pasangan ({@code mahasiswa}, {@code pkl}) tidak
 * dideklarasikan unik, dan jalur pembuatan di {@code PengecualianPklMahasiswaHelper}
 * <b>tidak memeriksa apakah barisnya sudah ada</b> (berbeda dari
 * {@code AmbilDataMahasiswaSeleksiPklHelper} yang memeriksa {@code MahasiswaDaftarPkl} lebih
 * dulu). Menambahkan mahasiswa yang sama dua kali menghasilkan baris duplikat. Secara fungsional
 * tidak berbahaya &mdash; {@code checkSyaratPkl} memakai {@code rowCount() > 0} &mdash; tetapi
 * duplikatnya muncul berulang di grid dan harus dihapus satu per satu.</li>
 * <li><b>Kedua relasi {@code nullable = true}.</b> Baris dengan {@code mahasiswa} atau {@code pkl}
 * bernilai {@code NULL} dapat tersimpan di tabel. Baris seperti itu tidak pernah cocok dengan
 * kriteria {@code Restrictions.eq(...)} di {@code checkSyaratPkl} sehingga tidak berefek apa pun,
 * tetapi akan membuat renderer grid melempar {@code NullPointerException} pada
 * {@code mahasiswa.getNim()}. Jalur pembuatan yang ada selalu mengisi keduanya, jadi kondisi ini
 * hanya muncul dari data yang disunting langsung di database.</li>
 * <li><b>{@link #getId()} dianotasi {@code insertable = false}.</b> Nilainya sepenuhnya berasal
 * dari {@code IDENTITY} milik PostgreSQL; menyetel {@link #setId(Long)} sebelum {@code save}
 * tidak akan mempengaruhi baris yang dibuat.</li>
 * <li><b>{@code serialVersionUID} tidak unik.</b> Nilai {@code 2463821577548439808L} dipakai
 * bersama oleh ratusan entity repo ini (semuanya hasil generator yang sama). Jangan
 * mengandalkannya untuk membedakan tipe.</li>
 * </ul>
 *
 * @see Pkl
 * @see Mahasiswa
 * @see PengecualianKknMahasiswa
 * @see BaypassPembayaranMahasiswa
 * @see ais.database.model.pkl.MahasiswaDaftarPkl
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pengecualian_pkl_mahasiswa")

public class PengecualianPklMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini dibagi bersama oleh ratusan entity lain di repo ini
	 * (warisan generator {@code hbm2java}) sehingga tidak bisa dipakai untuk membedakan tipe.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama, diisi PostgreSQL lewat {@code IDENTITY}. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang <b>meng-{@code UPDATE}</b> baris ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna terakhir yang <b>meng-{@code UPDATE}</b> baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini (bukan yang membuatnya).
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()} lewat
	 * {@code AuditTimestampInterceptor.olehId()}. Karena hook-nya hanya {@code @PreUpdate},
	 * nilai ini <b>tetap {@code null}</b> untuk baris pengecualian yang dibuat lalu tidak pernah
	 * disunting &mdash; lihat catatan jejak audit pada Javadoc kelas.</p>
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah di-{@code UPDATE}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> setter ini <b>mengabaikan secara diam-diam</b> nilai {@code null}
	 * maupun string yang hanya berisi spasi &mdash; ia langsung {@code return} tanpa mengubah
	 * apa pun dan tanpa melempar exception. Akibatnya nilai audit lama <b>tidak pernah bisa
	 * dikosongkan kembali</b> lewat setter ini. Perilaku ini seragam di seluruh entity repo dan
	 * disengaja: {@code AuditTimestampInterceptor} kadang tidak dapat menentukan pengguna
	 * (mis. proses batch), dan dalam kondisi itu identitas lama lebih baik dipertahankan
	 * daripada terhapus.</p>
	 *
	 * @param olehId id pengguna; {@code null} atau string kosong/spasi diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan diam-diam</b> sehingga nama lama dipertahankan.</p>
	 *
	 * @param oleh nama pengguna; {@code null} atau string kosong/spasi diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini (bukan yang membuatnya).
	 *
	 * <p>Diisi otomatis oleh {@link #onUpdate()}. Sama seperti {@link #getOlehId()}, nilainya
	 * {@code null} untuk baris yang belum pernah di-{@code UPDATE} &mdash; termasuk mayoritas
	 * baris pengecualian, karena jalur pembuatannya hanya melakukan {@code INSERT}.</p>
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila baris belum pernah di-{@code UPDATE}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang menyegarkan metadata audit tepat sebelum baris ini
	 * di-{@code UPDATE}, plus deklarasi field {@code tanggal_dirubah} yang menempel di baris kode
	 * yang sama (bentuk padat warisan penyuntingan massal; dibiarkan apa adanya).
	 *
	 * <p><b>Method {@code onUpdate()}.</b> Mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang &mdash; bila
	 * {@code AuditTrailHelper} menilai memang ada perubahan bisnis &mdash; menyetel
	 * {@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)}, dan
	 * {@link #setOlehId(String)}. Dipanggil oleh Hibernate, <b>tidak pernah</b> dipanggil manual.
	 * Pada entity ini praktis hanya terpicu saat keterangan dispensasi disunting lewat textbox
	 * inline di {@code PengecualianPklMahasiswaHelper}.</p>
	 *
	 * <p><b>Tidak ada pasangan {@code @PrePersist}</b>, sehingga {@code INSERT} tidak mencatat
	 * pembuat baris &mdash; konsekuensinya dibahas pada Javadoc kelas.</p>
	 *
	 * <p><b>Field {@code tanggal_dirubah}.</b> Diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat object dibuat di memori, sehingga baris baru
	 * tetap punya stempel waktu meski hook di atas belum pernah berjalan. Lihat
	 * {@link #getTanggal_dirubah()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini
	 * <b>tidak menyaring {@code null}</b> &mdash; memanggilnya dengan {@code null} benar-benar
	 * mengosongkan kolom. Umumnya hanya dipanggil oleh {@code AuditTimestampInterceptor}.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; {@code null} diterima dan mengosongkan nilai
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP} ke kolom {@code tanggal_dirubah}. Karena field-nya
	 * diinisialisasi saat object dibuat, nilainya untuk baris baru adalah waktu pembuatan; untuk
	 * baris yang pernah disunting, waktu {@code UPDATE} terakhir yang dianggap membawa perubahan
	 * bisnis oleh {@code AuditTrailHelper}.</p>
	 *
	 * @return stempel waktu perubahan terakhir; jarang {@code null} (hanya bila dikosongkan
	 *         eksplisit lewat {@link #setTanggal_dirubah(Date)} atau berasal dari data lama)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengembalikan {@link #getKeterangan() keterangan} dispensasi apa adanya sebagai representasi
	 * teks object ini.
	 *
	 * <p>Membaca <b>field mentah</b>, bukan getter (di kelas ini keduanya setara karena getternya
	 * polos). Karena jalur pembuatan satu-satunya mengisi keterangan dengan string kosong,
	 * {@code toString()} baris baru mengembalikan {@code ""}; untuk data lama nilainya dapat
	 * {@code null}. Perlu diperhatikan karena beberapa komponen ZK memanggil {@code toString()}
	 * secara implisit saat merender object.</p>
	 *
	 * @return keterangan alasan dispensasi; dapat berupa string kosong atau {@code null}
	 */
	public String toString() {
		return keterangan;
	}

	/** Mahasiswa yang mendapat dispensasi. Lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;

	/** Program PKL tempat dispensasi berlaku. Lihat {@link #getPkl()}. */
	private Pkl pkl;

	/** Alasan dispensasi (bebas-teks). Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA, sekaligus satu-satunya konstruktor.
	 *
	 * <p>Object hasil konstruktor ini belum bermakna: {@link #getMahasiswa()} dan
	 * {@link #getPkl()} masih {@code null} sehingga barisnya tidak akan pernah cocok dengan
	 * kriteria di {@code Common.checkSyaratPkl}. Pemanggil wajib mengisi keduanya (dan biasanya
	 * {@link #setKeterangan(String)}) sebelum {@code session.save} &mdash; lihat listener tombol
	 * "Ambil Data Mahasiswa" di {@code PengecualianPklMahasiswaHelper}.</p>
	 */
	public PengecualianPklMahasiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dipetakan sebagai {@code IDENTITY} dengan {@code insertable = false}, sehingga nilainya
	 * sepenuhnya ditentukan PostgreSQL saat {@code INSERT} dan baru terisi setelah flush.
	 * Dipakai {@code PengecualianPklMahasiswaHelper.loadData(Object)} sebagai kunci pengurutan
	 * ({@code Order.desc("id")}, sehingga baris terbaru tampil paling atas).</p>
	 *
	 * @return id baris; {@code null} untuk object yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Hanya dipakai Hibernate; menyetelnya manual sebelum {@code save}
	 * tidak berpengaruh karena kolomnya {@code insertable = false}.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan alasan dispensasi dalam bentuk teks bebas.
	 *
	 * <p>Ini adalah satu-satunya kolom data (bukan relasi, bukan audit) di entity ini, dan
	 * <b>satu-satunya bukti tertulis</b> mengapa seorang mahasiswa dibebaskan dari syarat PKL.
	 * Isinya tidak divalidasi, tidak wajib diisi, dan tidak pernah dibaca oleh logika bisnis
	 * mana pun &mdash; {@code Common.checkSyaratPkl} hanya menghitung jumlah baris, bukan
	 * membaca kolom ini. Perannya murni dokumentasi bagi operator.</p>
	 *
	 * <p><b>Ditampilkan &amp; disunting di:</b> kolom "Keterangan" pada grid
	 * {@code PengecualianPklMahasiswaHelper}, lewat textbox inline yang menyimpan perubahan
	 * langsung pada event {@code onChange} ({@code Common.refreshSaveOrUpdate}) tanpa tombol
	 * simpan terpisah.</p>
	 *
	 * @return alasan dispensasi; string kosong untuk baris yang baru dibuat, dapat {@code null}
	 *         pada data lama
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel alasan dispensasi.
	 *
	 * <p>Tidak ada penyaringan {@code null}/kosong di sini (berbeda dari
	 * {@link #setOleh(String)}), tidak ada pembatasan panjang, dan tidak ada validasi isi.
	 * Dipanggil dari dua tempat: saat pembuatan baris (diisi string kosong) dan dari listener
	 * {@code onChange} textbox keterangan pada grid.</p>
	 *
	 * @param keterangan alasan dispensasi; {@code null} diterima apa adanya
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan mahasiswa yang mendapat dispensasi.
	 *
	 * <p>Getter polos: mengembalikan field apa adanya, tanpa resolusi proxy lewat
	 * {@link GeneralValueObject#check(Object)} dan tanpa penulisan balik. Relasi dipetakan
	 * {@code @ManyToOne} (fetch bawaan JPA = {@code EAGER}) dengan
	 * {@code @Fetch(FetchMode.SELECT)}, jadi Hibernate mengambil mahasiswanya lewat
	 * {@code SELECT} terpisah &mdash; artinya memuat N baris pengecualian memicu N query
	 * tambahan (efek "N+1" pada grid, dibatasi {@code Common.MAX_RESULT_50}).</p>
	 *
	 * <p><b>Dipakai di.</b> {@code Common.checkSyaratPkl} mencocokkannya lewat
	 * {@code Restrictions.eq("mahasiswa", mahasiswa)}, dan renderer grid
	 * {@code PengecualianPklMahasiswaHelper} membaca NIM/nama/jurusan/fakultas dari object yang
	 * dikembalikan &mdash; <b>tanpa penjagaan {@code null}</b>, sehingga baris cacat
	 * (mahasiswa {@code NULL}) membuat grid gagal dirender.</p>
	 *
	 * @return mahasiswa penerima dispensasi, atau {@code null} bila kolomnya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa penerima dispensasi.
	 *
	 * <p>Dipanggil hanya sekali dalam hidup sebuah baris, yaitu saat pembuatan di
	 * {@code PengecualianPklMahasiswaHelper} (loop atas hasil pilihan
	 * {@code AmbilDataMahasiswaBanyak}); tidak ada layar yang memindahkan dispensasi ke mahasiswa
	 * lain. Karena relasi ber-{@code cascade} {@code PERSIST}/{@code MERGE}, object yang
	 * dipasang di sini ikut ter-{@code persist}/{@code merge} saat baris ini disimpan.</p>
	 *
	 * @param mahasiswa mahasiswa penerima dispensasi
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan program PKL tempat dispensasi ini berlaku.
	 *
	 * <p>Getter polos, sifat fetch-nya sama dengan {@link #getMahasiswa()}. Dispensasi bersifat
	 * <b>per program</b>, bukan berlaku umum: mahasiswa yang dikecualikan pada satu {@link Pkl}
	 * tetap harus memenuhi syarat pada program PKL lainnya. Untuk membebaskannya dari program
	 * lain, operator harus membuat baris baru dari layar program tersebut.</p>
	 *
	 * <p><b>Dipakai di.</b> {@code Common.checkSyaratPkl}
	 * ({@code Restrictions.eq("pkl", pkl)}) dan {@code PengecualianPklMahasiswaHelper.loadData}
	 * sebagai penyaring utama grid.</p>
	 *
	 * @return program PKL terkait, atau {@code null} bila kolomnya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pkl", nullable = true)
	public Pkl getPkl() {
		return pkl;
	}

	/**
	 * Menyetel program PKL tempat dispensasi berlaku.
	 *
	 * <p>Diisi sekali saat pembuatan baris dengan {@link Pkl} yang sedang dibuka di layar seleksi
	 * penerima PKL. Sama seperti {@link #setMahasiswa(Mahasiswa)}, object yang dipasang ikut
	 * ter-{@code persist}/{@code merge} karena {@code cascade}.</p>
	 *
	 * @param pkl program PKL terkait
	 */
	public void setPkl(Pkl pkl) {
		this.pkl = pkl;
	}

}
