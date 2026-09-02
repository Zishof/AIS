package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

/**
 * Entity <b>MASTER organisasi kemahasiswaan intra kampus</b> &mdash; tabel
 * {@code public.organisasi_intra_kampus}.
 *
 * <p>Satu baris mewakili satu organisasi kemahasiswaan di dalam kampus: BEM, DPM/MPM, UKM,
 * himpunan mahasiswa jurusan (HMJ), komunitas/klub, dan sejenisnya. Baris ini hanya menyimpan
 * <i>identitas organisasi</i> (kode, nama Indonesia, nama Inggris, cakupan fakultas/jurusan,
 * keterangan) ditambah <b>syarat akademik minimal untuk menjadi anggotanya</b> (IPK, SKS, angka
 * kredit kegiatan kemahasiswaan). <b>Siapa</b> mahasiswa yang menjadi anggota, dengan jabatan apa,
 * pada tahun berapa, dan apakah pengajuannya sudah disetujui &mdash; semuanya disimpan pada entity
 * penghubung {@link OrganisasiIntraKampusPunyaMahasiswa}, bukan di sini.</p>
 *
 * <h2>Perbandingan dengan {@link ais.database.model.OrganisasiDosen}</h2>
 * <p>Kelas ini adalah <b>padanan mahasiswa</b> dari {@link OrganisasiDosen} (padanan dosen).
 * Keduanya lahir dari cetakan generator yang sama sehingga sebagian besar strukturnya identik,
 * tetapi ada tiga perbedaan yang penting dipahami:</p>
 * <ul>
 *   <li><b>SAMA persis</b>: blok jejak audit ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *       beserta {@link #onUpdate()}), {@link #getId()}, {@link #getNama()} (wajib isi + unik),
 *       {@link #getKeterangan()}, {@link #getKode()} (termasuk seluruh perilaku pembangkitan
 *       otomatis dan kuirknya), {@link #getJurusan()}/{@link #getFakultas()} (opsional,
 *       {@code null} = "Semua", memakai {@code check(...)}), {@link #getNamaEn()}, dan
 *       {@link #toString()}. Bahkan {@code serialVersionUID}-nya bernilai sama.</li>
 *   <li><b>TIDAK ADA di sini &mdash; tingkat/level organisasi.</b> {@link OrganisasiDosen} punya
 *       relasi {@code levelOrganisasiDosen} ke master {@link LevelOrganisasiDosen}
 *       (Internasional/Nasional/Lokal). Kelas ini <b>tidak punya padanan apa pun</b>: tidak ada
 *       field level, dan tidak ada kelas {@code LevelOrganisasiIntraKampus} di paket ini
 *       (satu-satunya kelas "Level*" pada model adalah {@link LevelOrganisasiDosen}). Hal ini
 *       masuk akal secara domain &mdash; organisasi <i>intra kampus</i> menurut definisinya selalu
 *       bertingkat lokal. Konsekuensi menguntungkan: <b>bug pelaporan A-4.5.5 pada keluarga
 *       {@link OrganisasiDosen} tidak punya padanan di sini</b>, karena tidak ada kolom level yang
 *       bisa tertukar dengan kolom jabatan. Sebagai gantinya, tingkat organisasi hanya tersirat
 *       dari kombinasi {@link #getFakultas()}/{@link #getJurusan()} &mdash; lihat konvensi tak
 *       tertulis yang dijelaskan pada {@link #getFakultas()}.</li>
 *   <li><b>HANYA ADA di sini &mdash; syarat akademik keanggotaan.</b> Tiga properti
 *       {@link #getMinimalIpk()}, {@link #getMinimalSks()}, dan {@link #getMinimalSkkm()} tidak
 *       punya padanan di {@link OrganisasiDosen}. Berbeda dari {@code levelOrganisasiDosen} yang
 *       praktis write-only, ketiga properti ini <b>benar-benar ditegakkan</b> pada alur pendaftaran
 *       mandiri mahasiswa &mdash; lihat {@link #getMinimalIpk()}.</li>
 * </ul>
 * <p>Perbedaan kecil lain: {@link OrganisasiDosen} <b>dan</b> {@link JabatanOrganisasiDosen}
 * terdaftar sebagai entity yang di-<i>preload</i> ke cache in-memory oleh
 * {@code ais.common.InitData}, sedangkan kelas ini maupun {@link JabatanOrganisasiIntraKampus}
 * <b>tidak</b> (nol kemunculan di berkas itu). Karena master serumpun lain &mdash; termasuk
 * {@code JabatanKegiatanKemahasiswaan} &mdash; ada di daftar tersebut, absennya pasangan
 * intra-kampus tampak sebagai <b>kelalaian, bukan keputusan desain</b>. Konsekuensinya:
 * {@code check(...)} pada {@link #getJurusan()}/{@link #getFakultas()} lebih sering benar-benar
 * menyentuh session/database, dan pengisian combobox jabatan pada helper detail melakukan query
 * per baris grid (pola N+1).</p>
 *
 * <h2>Posisi dalam keluarga entity</h2>
 * <ul>
 *   <li>{@link OrganisasiIntraKampusPunyaMahasiswa} &mdash; keanggotaan seorang {@link Mahasiswa}
 *       pada satu organisasi; satu-satunya entity yang menunjuk balik ke sini lewat properti
 *       {@code organisasiIntraKampus}. Menyimpan {@code jabatanOrganisasiIntraKampus},
 *       {@code tahun}, {@code mulai}/{@code sampai}, {@code persetujuan}, {@code keterangan}, dan
 *       {@code tbmuser} (identitas pengaju). Entity itu <b>belum didokumentasikan</b> pada saat
 *       tulisan ini dibuat; catatan ringkasnya lihat bagian "Catatan keamanan" di bawah.</li>
 *   <li>{@link JabatanOrganisasiIntraKampus} &mdash; master <b>jabatan/peran mahasiswa di dalam
 *       organisasi</b> (Ketua/Pengurus/Anggota). Dirujuk oleh
 *       {@link OrganisasiIntraKampusPunyaMahasiswa}, <b>bukan</b> oleh entity ini. Master ini
 *       hanya berisi {@code nama} + {@code keterangan}; ia <b>tidak</b> menyimpan tingkat
 *       organisasi (bandingkan catatan bug pada {@code OrganisasiDosen}).</li>
 *   <li>{@link Fakultas} / {@link Jurusan} &mdash; cakupan organisasi. Keduanya boleh {@code null},
 *       yang berarti "berlaku untuk semua"; lihat {@link #getFakultas()}.</li>
 * </ul>
 *
 * <h2>Dari mana baris ini dibuat/diubah</h2>
 * <ol>
 *   <li><b>Layar master</b> &mdash; {@code ais.action.master.OrganisasiIntraKampusAction}
 *       ({@code /pages/master/organisasi_intra_kampus.zul}). Menyediakan CRUD (Tambah/Ubah/Hapus),
 *       pencarian per nama/kode/fakultas/jurusan serta per nama+NIM mahasiswa anggotanya dan per
 *       Dosen PA anggotanya, panel detail keanggotaan
 *       ({@code OrganisasiIntraKampusPunyaMahasiswaHelper}), dan tab bawaan untuk master
 *       {@link JabatanOrganisasiIntraKampus}.</li>
 *   <li><b>Impor Excel per-organisasi</b> &mdash;
 *       {@code OrganisasiIntraKampusAction#onUploadData}. Setiap <i>sheet</i> pada berkas
 *       {@code .xlsx} dicocokkan ke satu organisasi <b>berdasarkan {@link #getKode() kode}</b>
 *       (nama sheet = kode). Bila tidak ketemu, organisasi baru dibuat otomatis dengan
 *       {@code nama} dan {@code keterangan} = nama sheet (lihat kuirk pada {@link #getKode()}).
 *       Isi sheet (kolom NIM) kemudian dipakai membuat baris
 *       {@link OrganisasiIntraKampusPunyaMahasiswa}.</li>
 *   <li><b>Impor/ekspor generik</b> &mdash; {@code Common.uploadData}/{@code Common.cetakData}
 *       dengan daftar kolom {@code id, nama, namaEn, fakultas, jurusan, minimalIpk, minimalSks,
 *       minimalSkkm, keterangan}. Perhatikan {@code kode} <b>tidak</b> termasuk kolom yang
 *       diekspor/diimpor &mdash; sama seperti pada {@link OrganisasiDosen} &mdash; tetapi ketiga
 *       properti syarat akademik <b>ikut</b>, berbeda dari {@code levelOrganisasiDosen} yang
 *       tertinggal di daftar kolom {@link OrganisasiDosen}.</li>
 * </ol>
 *
 * <h2>Siapa yang membaca baris ini</h2>
 * <ul>
 *   <li>{@code ais.action.master.helper.AmbilDataOrganisasiForOrganisasiIntraKampusHelper}
 *       &mdash; <b>pendaftaran mandiri mahasiswa</b>: grid berisi organisasi yang dapat dipilih,
 *       lengkap dengan kolom "syarat" yang dirender dari ketiga properti {@code minimal*}. Method
 *       {@code save()}-nya memanggil
 *       {@code Common.checkApakahMemenuhiSyaratOrganisasiKemahasiswaan(...)} sebelum membuat baris
 *       keanggotaan; lihat {@link #getMinimalIpk()}.</li>
 *   <li>{@code ais.action.master.helper.OrganisasiIntraKampusPunyaMahasiswaHelper} &mdash; panel
 *       detail daftar anggota pada layar master (arah organisasi &rarr; mahasiswa).</li>
 *   <li>{@code ais.action.master.helper.MahasiswaPunyaOrganisasiIntraKampusHelper} &mdash; arah
 *       sebaliknya (mahasiswa &rarr; organisasi), dipakai halaman profil dan dasbor.</li>
 *   <li>{@code ais.action.master.helper.profile.ProfileMahasiswa} dan
 *       {@code ProfileUiHelper} &mdash; blok "Organisasi" pada halaman profil mahasiswa.</li>
 *   <li>{@code ais.action.master.dashboard.admin.DashboardOrganisasiIntraKampusUmum} &mdash;
 *       agregasi jumlah mahasiswa per organisasi per jabatan per tahun (kembaran
 *       {@code DashboardOrganisasiDosenUmum}). Juga dirujuk
 *       {@code DasborPerguruanTinggiTerpadu}, {@code DasboardAktivitasMahasiswa},
 *       {@code DashboardKegiatanKemahasiswaan}, dan {@code DashboardMahasiswa}.</li>
 *   <li>{@code ais.action.report.format1.akademik.LaporanPrestasiMahasiswa} &mdash; cetakan
 *       prestasi mahasiswa; seluruh properti baris ini disalin ke parameter JasperReports lewat
 *       {@code Common.insertProperty(OrganisasiIntraKampus.class, ...)} dengan awalan
 *       {@code organisasiIntraKampus}.</li>
 *   <li>{@code ais.action.master.BiodataMahasiswaAction} &mdash; tautan ke keanggotaan organisasi
 *       dari layar biodata.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 * <ul>
 *   <li><b>Jejak audit</b> (dideklarasikan ulang dari base class, lihat catatan di bawah):
 *       {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()} beserta
 *       setter-nya, dan callback {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *       {@link #getNamaEn()}, {@link #toString()}.</li>
 *   <li><b>Cakupan</b>: {@link #getFakultas()}, {@link #getJurusan()}.</li>
 *   <li><b>Syarat akademik keanggotaan</b> (khas kelas ini): {@link #getMinimalIpk()},
 *       {@link #getMinimalSks()}, {@link #getMinimalSkkm()}.</li>
 *   <li><b>Deskriptif</b>: {@link #getKeterangan()}.</li>
 * </ul>
 * <p>Tidak ada method utilitas, query statis, maupun logika bisnis lain di kelas ini. Logika
 * non-trivial hanya ada pada {@link #getKode()} (pembangkitan kode + tulis balik) dan pada tiga
 * getter {@code minimal*} yang meng-<i>coalesce</i> {@code null} menjadi {@code 0.0}. Penegakan
 * syarat akademiknya sendiri berada di luar kelas ini
 * ({@code CommonAcademicSyncHelper#checkApakahMemenuhiSyaratOrganisasiKemahasiswaan}).</p>
 *
 * <h2>Verifikasi pola berulang keluarga entity ini</h2>
 * <p>Diperiksa langsung dari kode kelas ini, bukan diasumsikan dari entity lain:</p>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field/DB</b>: <b>ADA satu</b> &mdash; {@link #getKode()}
 *       membangkitkan kode dari {@link #getId() id} lalu <b>menyimpannya ke field {@code kode}</b>.
 *       Karena {@code kode} properti terpetakan Hibernate, sekadar me-render daftar organisasi
 *       (renderer memanggil {@code getKode()} per baris) atau mengekspor Excel sudah cukup untuk
 *       memicu {@code UPDATE} saat flush. Ketiga getter {@code minimal*} <b>tidak</b> menulis balik
 *       &mdash; nilai {@code 0.0} hanya dikembalikan, field tetap {@code null}.</li>
 *   <li><b>Getter yang menutup session Hibernate</b>: <b>TIDAK ADA</b>. Tidak ada satu pun
 *       pemanggilan {@code HibernateUtil.closeSession()} di kelas ini.</li>
 *   <li><b>Getter destruktif</b> (getter yang mengosongkan data seperti
 *       {@code Komentar#getTbmuser()}): <b>TIDAK ADA</b> di kelas ini &mdash; semua relasi murni
 *       baca. (Pola itu justru muncul pada entity penghubungnya; lihat "Catatan keamanan".)</li>
 *   <li><b>{@code getNama()} yang membangkitkan ulang label</b> (pola {@code Kota}/
 *       {@code Penghasilan}): <b>TIDAK ADA</b> &mdash; {@link #getNama()} di sini hanya
 *       me-{@code trim()} tanpa menulis balik.</li>
 *   <li><b>Konsistensi {@code check()}</b>: berbeda dari {@link OrganisasiDosen} yang asimetris
 *       (dua relasi memakai {@code check(...)}, satu tidak), di kelas ini <b>kedua</b> relasi yang
 *       ada ({@link #getJurusan()}, {@link #getFakultas()}) sama-sama memakai {@code check(...)}
 *       dengan {@code FetchType.LAZY}.</li>
 * </ul>
 *
 * <h2>Catatan {@code GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}
 * &mdash; ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan properti apa pun yang
 * dideklarasikan di sana. Karena itu field {@link #id}, {@link #oleh}, {@link #olehId}, dan
 * {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di kelas ini agar ikut tersimpan;
 * pengulangan tersebut <b>keharusan teknis, bukan bug</b>. Konsekuensi lain: properti warisan yang
 * <i>tidak</i> dideklarasikan ulang (misalnya {@code diubahDari}) tidak tersimpan ke database dan
 * akan kembali {@code null} setelah baris dimuat ulang &mdash; perhatikan bahwa entity penghubung
 * {@link OrganisasiIntraKampusPunyaMahasiswa} justru <i>mendeklarasikan</i> {@code diubahDari},
 * jadi perilakunya berbeda dari kelas ini.</p>
 *
 * <h2>Catatan Envers</h2>
 * <p>Kelas ditandai {@link Audited}, sehingga setiap perubahan baris tersalin ke tabel revisi dan
 * dapat ditelusuri lewat {@code RevisiHelper} pada layar masternya. Kombinasi
 * {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya menuliskan kolom yang
 * benar-benar berubah.</p>
 *
 * <h2>Catatan keamanan (hasil audit, tidak diperbaiki di sini)</h2>
 * <ul>
 *   <li><b>Inversi hak akses (kembaran persis {@code OrganisasiDosenAction}).</b> Pada
 *       {@code OrganisasiIntraKampusAction}, deklarasi bendera {@code edit}/{@code delete}
 *       <b>dikomentari</b> ({@code // private boolean edit = false;}), begitu pula
 *       {@code // button.setVisible(edit);} pada tombol Ubah dan
 *       {@code // button.setVisible(delete);} pada tombol Hapus di renderer grid. Tidak ada satu
 *       pun pemanggilan {@code CommonPrivilages.checkPrevilages(...)} di seluruh kelas Action ini
 *       &mdash; bahkan versi terkomentarinya pun tidak ada (di {@code OrganisasiDosenAction} masih
 *       tersisa sebagai komentar). Satu-satunya gerbang adalah {@code Common.doCheckSecurity()}
 *       yang hanya memeriksa hak <b>READ tingkat menu</b>. Efeknya: siapa pun yang bisa membuka
 *       layar master dapat mengubah dan menghapus baris organisasi. Karena tombol Tambah
 *       ({@code add}) juga tidak pernah di-{@code setVisible(...)}, ia default terlihat, dan kedua
 *       tombol impor massal yang bergantung padanya
 *       ({@code upload.setVisible(add != null &amp;&amp; add.isVisible())}) ikut selalu aktif.
 *       <b>Bukti bahwa ini anomali, bukan gaya arsitektur:</b> kelas saudara
 *       {@code JabatanOrganisasiIntraKampusAction} &mdash; yang bahkan di-<i>include</i> sebagai
 *       tab di layar yang sama &mdash; memasang lengkap
 *       {@code add.setVisible(checkPrevilages(CREATE))}, {@code edit = ...UPDATE},
 *       {@code delete = ...DELETE}.</li>
 *   <li><b>Impor Excel melewati seluruh syarat akademik dan alur persetujuan.</b>
 *       {@code OrganisasiIntraKampusAction#onUploadData} membuat baris
 *       {@link OrganisasiIntraKampusPunyaMahasiswa} massal dari kolom NIM tanpa pernah memanggil
 *       {@code checkApakahMemenuhiSyaratOrganisasiKemahasiswaan(...)}, dan mengisi kolom
 *       {@code persetujuan} langsung dari sel berkas &mdash; sehingga keanggotaan dapat disetujui
 *       borongan tanpa melewati layar persetujuan. Sheet yang tak dikenal juga membuat baris master
 *       baru (jalur CREATE tanpa gerbang; lihat {@link #getKode()}).</li>
 *   <li><b>Aksi massal "Bersihkan" tanpa gerbang.</b> Pada
 *       {@code OrganisasiIntraKampusPunyaMahasiswaHelper}, tombol "Bersihkan" menjalankan
 *       {@code DELETE} SQL native atas seluruh anggota yang belum disetujui pada organisasi
 *       tersebut, tanpa memeriksa {@code CommonPrivilages.DELETE} (padahal bendera {@code delete}
 *       sudah tersedia dan dipakai di renderer berkas yang sama). Karena SQL native, penghapusan
 *       itu melewati Envers dan {@code AuditListener}, sehingga indeks JSON per-mahasiswa pada
 *       {@link Mahasiswa} menjadi basi. Tombol "Ambil Mahasiswa" di panel yang sama juga tanpa
 *       gerbang.</li>
 *   <li><b>SQL injection pada {@code OrganisasiIntraKampusAction#initCriteria}.</b> Nilai kotak
 *       pencarian "Nama Mahasiswa" ({@code searchnamamhs}) dan "NIM" ({@code searchnim})
 *       disisipkan <b>mentah</b> ke dalam string {@code Restrictions.sqlRestriction(...)} yang
 *       membangun subquery {@code organisasi_intra_kampus_punya_mahasiswa}. Ini instance baru dari
 *       pola yang sama persis dengan yang ditemukan pada {@code OrganisasiDosenAction}.</li>
 *   <li><b>Identitas pengaju keanggotaan terhapus.</b> Pada entity penghubung
 *       {@link OrganisasiIntraKampusPunyaMahasiswa}, getter {@code getTbmuser()} mengembalikan
 *       {@code null} bila akun pengaju terkait seorang mahasiswa
 *       ({@code tbmuser.getMahasiswa() != null}). Karena pemetaan Hibernate di keluarga entity ini
 *       berbasis <i>property access</i>, nilai {@code null} itulah yang dibandingkan saat
 *       pemeriksaan <i>dirty</i> dan yang dituliskan kembali ke kolom &mdash; kembaran pola
 *       {@code OrganisasiDosenPunyaDosen#getTbmuser()}. Di sini dampaknya lebih luas karena
 *       pengaju keanggotaan organisasi kemahasiswaan <b>memang normalnya mahasiswa</b>, sehingga
 *       kolom pengaju praktis selalu berakhir kosong.</li>
 *   <li><b>Fail-open pada pemeriksaan syarat.</b> Lihat {@link #getMinimalIpk()} &mdash;
 *       {@code checkApakahMemenuhiSyaratOrganisasiKemahasiswaan} membungkus seluruh
 *       perhitungannya dalam {@code try}/{@code catch (Exception)} dan mengembalikan nilai awal
 *       {@code true} bila terjadi kesalahan.</li>
 *   <li><b>Layar master tidak menyaring per fakultas/prodi pengguna.</b>
 *       {@code initCriteria} hanya memakai nilai kotak pencarian; tidak ada pembatasan berdasarkan
 *       satuan kerja pengguna yang login. Tombol "Download Data Mahasiswa" karenanya dapat
 *       mengeluarkan NIM + nama seluruh anggota semua organisasi se-universitas.</li>
 * </ul>
 *
 * <h2>Ketidakkonsistenan pelaporan {@code persetujuan}</h2>
 * <p>Bukan celah keamanan, tetapi patut dicatat karena berdampak ke angka yang dilaporkan:
 * {@code LaporanPrestasiMahasiswa} (SKPI) menyaring
 * {@code Restrictions.eq("persetujuan", true)}, sedangkan
 * {@code DashboardOrganisasiIntraKampusUmum}, {@code DasborPerguruanTinggiTerpadu}, dan
 * {@code DasboardAktivitasMahasiswa} <b>tidak menyaring {@code persetujuan} sama sekali</b>.
 * Akibatnya pengajuan yang belum (atau tidak akan) disetujui tetap terhitung sebagai capaian
 * institusi pada dasbor, sehingga angka dasbor selalu &ge; angka pada dokumen resmi mahasiswa.</p>
 *
 * <h2>Kuirk komentar generator</h2>
 * <p>Komentar aslinya berbunyi <i>"Bank generated by hbm2java"</i> &mdash; artefak salin-tempel
 * dari {@link Bank}, sumber yang sama yang dibajak puluhan entity lain di paket ini. Komentar itu
 * digantikan Javadoc di atas; tidak ada hubungan apa pun antara entity ini dan {@link Bank}.</p>
 *
 * @see GeneralValueObject
 * @see OrganisasiDosen
 * @see OrganisasiIntraKampusPunyaMahasiswa
 * @see JabatanOrganisasiIntraKampus
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "organisasi_intra_kampus")
public class OrganisasiIntraKampus extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja dibiarkan sama dengan sejumlah entity master lain
	 * hasil generator {@code hbm2java} (mis. {@link OrganisasiDosen},
	 * {@link JabatanOrganisasiIntraKampus}, {@link OrganisasiIntraKampusPunyaMahasiswa});
	 * duplikasi ini tidak menimbulkan masalah karena {@code serialVersionUID} hanya dibandingkan
	 * antar versi kelas yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris, kolom {@code id}. Dideklarasikan ulang dari base class (lihat Javadoc kelas). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang dari base class. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang dari base class. */
	private String olehId;

	/**
	 * ID pengguna terakhir yang mengubah baris ini. Diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 *
	 * <p>Properti ini dideklarasikan ulang di sini (tidak sekadar diwarisi) karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass}; tanpa deklarasi ulang, kolomnya
	 * tidak akan terpetakan Hibernate.</p>
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> (method
	 * langsung {@code return}), sehingga jejak audit lama tidak pernah bisa dihapus lewat setter
	 * ini.</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir. Umumnya dipanggil
	 * {@code AuditTimestampInterceptor}, bukan kode aplikasi.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Dideklarasikan ulang di kelas ini karena
	 * alasan yang sama dengan {@link #getOlehId()}.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE}
	 * baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati
	 * callback ini, sehingga organisasi yang dibuat otomatis oleh impor Excel (lihat
	 * {@link #getKode()}) tidak punya jejak audit sampai ada penyuntingan berikutnya.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja diletakkan pada baris fisik yang sama
	 * dalam kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir. Berbeda dari {@link #setOleh(String)}, setter ini
	 * <b>tidak</b> menyaring {@code null}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, presisi timestamp).
	 * Terisi otomatis saat objek dibuat dan diperbarui pada setiap {@code UPDATE} lewat
	 * {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir; praktis tidak pernah {@code null} untuk objek baru
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p><b>Perhatian:</b> method ini membaca <b>field</b> {@code nama} langsung, bukan lewat
	 * {@link #getNama()}, sehingga hasilnya <b>tidak</b> di-{@code trim()} dan bisa berisi spasi
	 * awal/akhir. Dipakai antara lain untuk label combobox, pesan progres impor/ekspor, dan
	 * keperluan debug.</p>
	 *
	 * @return string {@code "<id>-<nama>"}; bagian id berbunyi {@code "null"} untuk baris yang
	 *         belum disimpan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama organisasi dalam bahasa Indonesia (kolom {@code nama}, wajib isi, unik). */
	private String nama;
	/** Keterangan bebas tentang organisasi (kolom {@code keterangan}, opsional). */
	private String keterangan;
	/** Kode organisasi (kolom {@code kode}); dibangkitkan otomatis dari id bila kosong &mdash; lihat {@link #getKode()}. */
	private String kode;
	/** Cakupan program studi organisasi; {@code null} berarti berlaku untuk semua prodi. */
	private Jurusan jurusan;
	/** Cakupan fakultas organisasi; {@code null} berarti berlaku untuk semua fakultas. */
	private Fakultas fakultas;

	/** Syarat IPK minimal calon anggota; {@code null}/{@code 0} berarti tanpa syarat &mdash; lihat {@link #getMinimalIpk()}. */
	private Double minimalIpk;
	/** Syarat SKS kumulatif minimal calon anggota; {@code null}/{@code 0} berarti tanpa syarat &mdash; lihat {@link #getMinimalSks()}. */
	private Double minimalSks;
	/** Syarat angka kredit kegiatan kemahasiswaan (SKKM) minimal calon anggota &mdash; lihat {@link #getMinimalSkkm()}. */
	private Double minimalSkkm;
	/** Nama organisasi dalam bahasa Inggris (kolom {@code namaen}, opsional). */
	private String namaEn;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Semua properti dibiarkan
	 * {@code null} kecuali {@code tanggal_dirubah} yang langsung terisi jam aplikasi. Dipakai juga
	 * oleh layar master ({@code onAdd}) dan alur impor Excel untuk membuat baris baru.
	 *
	 * <p>Perhatikan konsekuensi kombinasi ini: baris yang baru dibuat memiliki ketiga properti
	 * {@code minimal*} bernilai {@code null}, yang oleh getter-nya diterjemahkan menjadi
	 * {@code 0.0} = "tanpa syarat". Jadi <b>organisasi baru selalu terbuka untuk semua
	 * mahasiswa</b> sampai ada yang mengisi syaratnya.</p>
	 */
	public OrganisasiIntraKampus() {
	}

	/**
	 * Primary key baris (kolom {@code id}, {@code IDENTITY}/serial PostgreSQL).
	 *
	 * <p>{@code insertable = false} berarti nilai kolom sepenuhnya ditentukan sequence database;
	 * objek baru punya {@code id} {@code null} sampai di-{@code flush}. Nilai ini juga menjadi
	 * bahan pembentuk {@link #getKode() kode} otomatis.</p>
	 *
	 * @return id baris, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Praktis hanya dipakai Hibernate saat memuat/menyimpan baris; kode
	 * aplikasi tidak boleh mengubah id baris yang sudah tersimpan.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama organisasi dalam bahasa Indonesia (kolom {@code nama}, wajib isi, <b>unik</b> di
	 * tingkat database).
	 *
	 * <p>Ini adalah label utama organisasi di seluruh aplikasi: kolom grid layar master, grid
	 * pendaftaran mandiri mahasiswa, judul panel detail keanggotaan, label pada profil mahasiswa,
	 * kunci pengelompokan pada {@code DashboardOrganisasiIntraKampusUmum}, dan kolom pada
	 * {@code LaporanPrestasiMahasiswa}.</p>
	 *
	 * <p>Getter hanya melakukan {@code trim()} pada nilai yang dikembalikan dan <b>tidak</b>
	 * menulis balik ke field &mdash; berbeda dari pola {@code Kota#getNama()}/
	 * {@code Penghasilan#getNama()} yang memicu {@code UPDATE} saat sekadar dibaca.</p>
	 *
	 * <p>Keunikan ditegakkan dua kali: oleh constraint kolom, dan oleh
	 * {@code OrganisasiIntraKampusAction#checkNamaOrganisasiIntraKampus()} yang menolak simpan
	 * bila sudah ada baris lain (id berbeda) dengan nama persis sama.</p>
	 *
	 * <p><b>Kuirk validasi:</b> pemeriksaan aplikasi itu memakai
	 * {@code Restrictions.eq("nama", ...)} yang <i>case-sensitive</i>, sehingga "BEM" dan "Bem"
	 * lolos di lapisan aplikasi; yang benar-benar menahannya hanyalah constraint database (yang
	 * juga case-sensitive di PostgreSQL, jadi keduanya sama-sama tersimpan). Selain itu validasi
	 * ini <b>tidak dijalankan</b> pada jalur impor mana pun ({@code onUploadData} maupun
	 * {@code Common.uploadData}).</p>
	 *
	 * @return nama organisasi tanpa spasi awal/akhir, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255, unique = true)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama organisasi. Nilai disimpan apa adanya (tanpa {@code trim()}); pemangkasan
	 * baru terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * <p>Selain dari form Tambah/Ubah layar master, setter ini juga dipanggil
	 * {@code OrganisasiIntraKampusAction#onUploadData} dengan <i>nama sheet</i> Excel sebagai
	 * nilainya saat membuat organisasi "hantu"; lihat kuirk pada {@link #getKode()}.</p>
	 *
	 * @param nama nama organisasi; wajib non-kosong agar lolos validasi layar master dan constraint
	 *             {@code NOT NULL} kolomnya
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas tentang organisasi (kolom {@code keterangan}, opsional).
	 *
	 * <p>Ditampilkan sebagai kolom tersendiri pada grid layar master dan pada grid pendaftaran
	 * mandiri mahasiswa. Pada organisasi yang dibuat otomatis oleh impor Excel, isinya disamakan
	 * dengan nama sheet.</p>
	 *
	 * <p>Sama seperti {@code OrganisasiDosen#getKeterangan()}, getter ini mengembalikan field apa
	 * adanya sehingga <b>boleh {@code null}</b> &mdash; berbeda dari klaim umum
	 * {@link GeneralValueObject} yang menjanjikan keterangan non-null. Pemanggil (mis.
	 * {@code new Label(...)} pada renderer) harus siap menerimanya.</p>
	 *
	 * @return keterangan organisasi, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan organisasi.
	 *
	 * @param keterangan keterangan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kode organisasi (kolom {@code kode}) &mdash; <b>satu-satunya method dengan logika tulis di
	 * kelas ini</b>. Perilakunya identik baris-per-baris dengan
	 * {@code OrganisasiDosen#getKode()}.
	 *
	 * <p><b>Tujuan:</b> menyediakan identitas pendek dan stabil untuk organisasi, dipakai sebagai
	 * <i>nama sheet</i> pada ekspor Excel keanggotaan
	 * ({@code workbook.createSheet(organisasiIntraKampus.getKode())}) dan sebagai kunci pencocokan
	 * kembali saat berkas itu diimpor ulang lewat {@code onUploadData}.</p>
	 *
	 * <p><b>Perilaku:</b> bila baris sudah punya {@link #getId() id} tetapi {@code kode} masih
	 * {@code null}/kosong, kode dibangkitkan sebagai id yang <b>dipadkan nol menjadi 5 digit</b>
	 * ({@code "0000000000" + id}, lalu diambil 5 karakter terakhir), kemudian <b>disimpan ke field
	 * {@code kode}</b>. Bila kode sudah terisi, nilainya dikembalikan apa adanya.</p>
	 *
	 * <p><b>Efek samping (pola "getter yang menulis"):</b> karena {@code kode} adalah properti
	 * terpetakan Hibernate (tidak beranotasi {@code @Column}, jadi memakai nama kolom bawaan
	 * {@code kode}), penugasan di dalam getter membuat objek menjadi <i>dirty</i>. Sekadar
	 * me-render daftar organisasi sudah memicu {@code UPDATE} &mdash; renderer layar master
	 * memanggil {@code getKode()} untuk setiap baris, begitu pula proses ekspor Excel. Jadi kolom
	 * {@code kode} terisi sendiri seiring waktu tanpa ada yang pernah mengetiknya.</p>
	 *
	 * <p><b>Kuirk 1 &mdash; tidak ada jalur input manual:</b> layar master
	 * {@code OrganisasiIntraKampusAction} menyediakan kotak <i>pencarian</i> kode
	 * ({@code searchkode}) tetapi <b>tidak</b> menyediakan kolom isian kode pada form Tambah/Ubah,
	 * dan {@code kode} juga tidak termasuk daftar kolom impor/ekspor generik. Praktis nilai kode
	 * selalu hasil pembangkitan otomatis di atas.</p>
	 *
	 * <p><b>Kuirk 2 &mdash; kode meluap tanpa peringatan:</b> pemadan hanya 5 digit. Begitu id
	 * melewati 99.999, kode yang dihasilkan adalah 5 digit <i>terakhir</i> dari id sehingga dua
	 * organisasi berbeda bisa memperoleh kode identik; tidak ada constraint unik pada kolom ini
	 * yang mencegahnya, dan pencocokan sheet impor akan menjadi ambigu.</p>
	 *
	 * <p><b>Kuirk 3 &mdash; impor sheet asing membuat organisasi "hantu":</b> pada
	 * {@code onUploadData}, sheet yang namanya tidak cocok dengan kode mana pun akan membuat
	 * {@code OrganisasiIntraKampus} baru dengan {@code nama} dan {@code keterangan} = nama sheet
	 * (biasanya berupa angka seperti {@code "00012"}), tanpa cakupan fakultas/jurusan dan tanpa
	 * syarat akademik apa pun. Karena baris baru itu langsung memperoleh kode dari <b>id barunya
	 * sendiri</b> (bukan dari nama sheet), pengunggahan berkas yang sama untuk kedua kali tetap
	 * tidak menemukan kecocokan dan mencoba membuat baris kembar &mdash; yang lalu ditolak
	 * constraint unik pada {@code nama} dan gagal diam-diam.</p>
	 *
	 * @return kode organisasi (5 digit berpadding nol), atau {@code null} bila baris belum punya id
	 *         dan kode belum pernah diisi
	 */
	public String getKode() {
		if (id != null && (kode == null || kode.trim().isEmpty())) {
			String k = "0000000000" + id;
			kode = k.substring(k.length() - 5);
		}
		return kode;
	}

	/**
	 * Menetapkan kode organisasi secara eksplisit. Praktis hanya dipakai Hibernate saat memuat
	 * baris; tidak ada layar yang memanggilnya (lihat kuirk pada {@link #getKode()}).
	 *
	 * <p>Mengisi nilai non-kosong di sini mematikan pembangkitan otomatis pada
	 * {@link #getKode()}.</p>
	 *
	 * @param kode kode organisasi; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Program studi yang menjadi cakupan organisasi ini (kolom FK {@code jurusan}, opsional).
	 *
	 * <p>{@code null} berarti <b>"Semua"</b> prodi &mdash; itulah teks yang dirender layar master
	 * dan grid pendaftaran mandiri untuk nilai kosong. Cakupan ini bersifat <i>penyaring daftar
	 * pilihan</i>, bukan aturan keanggotaan: ia menentukan organisasi mana yang muncul di grid
	 * pemilih, sementara syarat yang benar-benar diverifikasi saat menyimpan keanggotaan hanyalah
	 * ketiga properti {@code minimal*} (lihat {@link #getMinimalIpk()}).</p>
	 *
	 * <p><b>Efek samping:</b> getter memanggil {@code check(...)} milik {@link GeneralValueObject}
	 * untuk meresolusi proxy lazy (cache in-memory &rarr; session aktif &rarr; session baru), lalu
	 * <b>menugaskan kembali hasilnya ke field</b>. Penugasan ini menukar proxy dengan instance yang
	 * sudah terinisialisasi &mdash; tidak mengubah identitas baris, jadi tidak membuat objek dirty
	 * terhadap kolom FK-nya. Berbeda dari {@link OrganisasiDosen}, kelas ini <b>tidak</b> terdaftar
	 * di {@code ais.common.InitData}, sehingga langkah cache in-memory umumnya meleset dan
	 * resolusinya benar-benar menyentuh session/database.</p>
	 *
	 * @return prodi cakupan organisasi, atau {@code null} bila berlaku untuk semua prodi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan prodi cakupan organisasi. Dipanggil {@code OrganisasiIntraKampusAction#onSave}
	 * dari pilihan combobox Prodi; nilai {@code null} berarti organisasi berlaku lintas prodi.
	 *
	 * @param jurusan prodi cakupan; boleh {@code null}
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Fakultas yang menjadi cakupan organisasi ini (kolom FK {@code fakultas}, opsional).
	 *
	 * <p>Semantik dan efek samping {@code check(...)}-nya identik dengan {@link #getJurusan()}:
	 * {@code null} dirender sebagai <b>"Semua"</b>.</p>
	 *
	 * <p><b>Konvensi tak tertulis &mdash; ini pengganti kolom "tingkat organisasi":</b> karena
	 * kelas ini tidak punya padanan {@code levelOrganisasiDosen}, tingkat organisasi hanya
	 * tersirat dari kombinasi kedua FK:</p>
	 * <ul>
	 *   <li>{@code jurusan != null} &rarr; tingkat program studi (HMJ);</li>
	 *   <li>{@code jurusan == null} dan {@code fakultas != null} &rarr; tingkat fakultas;</li>
	 *   <li>keduanya {@code null} &rarr; tingkat universitas (BEM/DPM/UKM).</li>
	 * </ul>
	 * <p>Konvensi ini <b>tidak divalidasi di mana pun</b>: tidak ada yang mencegah baris dengan
	 * {@code jurusan} terisi tetapi {@code fakultas} kosong, dan bentuk data seperti itu membuat
	 * kedua filter di layar master saling bertentangan.</p>
	 *
	 * <p><b>Kuirk penyaringan yang tidak konsisten antar layar:</b> grid pendaftaran mandiri
	 * ({@code AmbilDataOrganisasiForOrganisasiIntraKampusHelper}) menyusun filternya sebagai
	 * {@code Restrictions.or(Restrictions.isNull("fakultas"), eq(...))} sehingga organisasi
	 * bercakupan "Semua" selalu ikut tampil &mdash; sama seperti pada
	 * {@link OrganisasiDosen}. Sebaliknya {@code OrganisasiIntraKampusAction#initCriteria} pada
	 * <b>layar master</b> memakai {@code CommonSearchFilterHelper.eqSelectedWithId(...)} polos
	 * tanpa cabang {@code isNull}, sehingga begitu pengguna memilih sebuah fakultas (atau prodi),
	 * seluruh organisasi tingkat universitas <b>hilang dari daftar</b>. Dua layar, dua semantik,
	 * satu kolom.</p>
	 *
	 * <p><b>Kuirk:</b> berbeda dari {@code ItemBiayaPunyaAkun#getFakultas()}, getter ini
	 * <b>tidak</b> menurunkan fakultas dari {@link #getJurusan()} secara otomatis. Akibatnya kedua
	 * kolom bisa saling bertentangan &mdash; misalnya {@code jurusan} diisi prodi milik Fakultas A
	 * sementara {@code fakultas} diisi Fakultas B &mdash; dan tidak ada validasi konsistensi di
	 * {@code onSave}.</p>
	 *
	 * @return fakultas cakupan organisasi, atau {@code null} bila berlaku untuk semua fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menetapkan fakultas cakupan organisasi. Dipanggil {@code OrganisasiIntraKampusAction#onSave}
	 * dari pilihan combobox Fakultas.
	 *
	 * @param fakultas fakultas cakupan; boleh {@code null}
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * <b>Syarat IPK minimal</b> yang harus dipenuhi mahasiswa untuk dapat mendaftar sebagai anggota
	 * organisasi ini. Tidak ada padanan properti ini di {@link OrganisasiDosen}.
	 *
	 * <p><b>Kontrak nilai:</b> getter meng-<i>coalesce</i> {@code null} menjadi {@code 0.0},
	 * sehingga pemanggil tidak pernah menerima {@code null} dan aman melakukan aritmetika/pembanding
	 * primitif. Field-nya sendiri <b>tidak</b> ditulis balik &mdash; nilai {@code 0.0} hanya
	 * dikembalikan, jadi getter ini tidak membuat objek dirty (berbeda dari {@link #getKode()}).</p>
	 *
	 * <p><b>Ambang "tanpa syarat":</b> seluruh kode pemanggil memakai perbandingan
	 * {@code > 0.1} (bukan {@code > 0}) untuk memutuskan apakah syarat ini aktif. Jadi nilai
	 * {@code null}, {@code 0}, maupun angka kecil di bawah {@code 0.1} sama-sama berarti "tanpa
	 * syarat IPK". Karena {@link #OrganisasiIntraKampus() konstruktor} meninggalkan field ini
	 * {@code null}, organisasi yang baru dibuat selalu terbuka untuk semua mahasiswa.</p>
	 *
	 * <p><b>Siapa yang membacanya (berbeda dari {@code levelOrganisasiDosen} yang praktis
	 * write-only, properti ini benar-benar ditegakkan):</b></p>
	 * <ol>
	 *   <li>{@code AmbilDataOrganisasiForOrganisasiIntraKampusHelper.MahasiswaRenderer#render} pada
	 *       layar pendaftaran mandiri &mdash; menampilkan label {@code "IPK >= <nilai>"}; bila
	 *       ketiga properti {@code minimal*} di bawah ambang, kolomnya berbunyi "Tidak ada
	 *       syarat".</li>
	 *   <li>{@code CommonAcademicSyncHelper#checkApakahMemenuhiSyaratOrganisasiKemahasiswaan(
	 *       Mahasiswa, OrganisasiIntraKampus)} (dipanggil lewat fasad {@code Common}) &mdash;
	 *       <b>penegakan sesungguhnya, dan satu-satunya titik logika yang menegakkannya</b>.
	 *       Dipanggil dari dua tempat: {@code save()} helper di atas (arah mahasiswa &rarr;
	 *       organisasi) dan {@code AmbilDataMahasiswaForOrganisasiIntraKampusHelper#save()} (arah
	 *       organisasi &rarr; mahasiswa). Untuk setiap baris yang dicentang, organisasi/mahasiswa
	 *       yang syaratnya tidak terpenuhi dilewati dan alasannya dikumpulkan menjadi satu pesan
	 *       peringatan di akhir proses. IPK yang dibandingkan berasal dari
	 *       {@code Common.singkronkanKrsMahasiswa(mahasiswa).getIpk()}. Strukturnya sejajar dengan
	 *       {@code checkApakahMemenuhiSyaratBeasiswa} yang memakai
	 *       {@code Beasiswa.batasanIP}/{@code batasanSkkp}/{@code batasanSks}.</li>
	 *   <li>{@code OrganisasiIntraKampusAction} &mdash; kolom ringkas
	 *       {@code "<minimalSks> / <minimalIpk> / <minimalSkkm>"} pada grid daftar, isian
	 *       {@code MyDoublebox} berlabel "Minimal IPK"/"Minimal SKS"/"Minimal SKKM" pada form
	 *       Tambah/Ubah, dan salah satu kolom impor/ekspor generik.</li>
	 * </ol>
	 *
	 * <p><b>Jalur yang MELEWATI syarat ini sepenuhnya:</b> impor Excel keanggotaan
	 * ({@code OrganisasiIntraKampusAction#onUploadData}) membuat baris
	 * {@link OrganisasiIntraKampusPunyaMahasiswa} langsung dari kolom NIM tanpa pernah memanggil
	 * pemeriksaan di atas &mdash; dan bahkan mengisi kolom {@code persetujuan} dari sel berkas.
	 * Jadi syarat akademik ini hanya mengikat alur pendaftaran lewat kedua grid pemilih; alur
	 * impor massal bebas sepenuhnya.</p>
	 *
	 * <p><b>Catatan keamanan &mdash; pemeriksaan syarat bersifat <i>fail-open</i>:</b>
	 * {@code checkApakahMemenuhiSyaratOrganisasiKemahasiswaan} menginisialisasi hasilnya
	 * {@code true} lalu membungkus seluruh perhitungan dalam {@code try}/
	 * {@code catch (Exception e)}. Bila terjadi kesalahan apa pun (mis. sinkronisasi KRS gagal),
	 * pengecualian hanya diteruskan ke {@code Common.tampilErrorJikaAdmin(e)} dan method
	 * mengembalikan {@code true} &mdash; mahasiswa dianggap <b>memenuhi</b> syarat. Ini pola yang
	 * sama dengan {@code checkApakahMemenuhiSyaratBeasiswa}.</p>
	 *
	 * @return syarat IPK minimal; {@code 0.0} bila belum diisi (artinya tanpa syarat), tidak pernah
	 *         {@code null}
	 */
	public Double getMinimalIpk() {
		return minimalIpk == null ? 0.0 : minimalIpk;
	}

	/**
	 * Menetapkan syarat IPK minimal. Dipanggil {@code OrganisasiIntraKampusAction#onSave} dari
	 * isian {@code MyDoublebox} "Minimal IPK", dan oleh alur impor generik
	 * {@code Common.uploadData}.
	 *
	 * <p>Tidak ada validasi rentang: nilai negatif maupun di atas 4,00 diterima apa adanya. Nilai
	 * {@code null} (kotak isian dikosongkan) juga diterima dan berarti "tanpa syarat".</p>
	 *
	 * @param minimalIpk syarat IPK minimal; boleh {@code null}
	 */
	public void setMinimalIpk(Double minimalIpk) {
		this.minimalIpk = minimalIpk;
	}

	/**
	 * <b>Syarat SKS kumulatif minimal</b> yang harus sudah ditempuh mahasiswa untuk mendaftar
	 * sebagai anggota organisasi ini.
	 *
	 * <p>Kontrak nilai, ambang {@code > 0.1}, dan daftar pemanggilnya sama persis dengan
	 * {@link #getMinimalIpk()} &mdash; lihat penjelasan lengkap di sana, termasuk catatan
	 * <i>fail-open</i>.</p>
	 *
	 * <p><b>Kuirk khas properti ini &mdash; pemotongan ke bilangan bulat:</b> pada
	 * {@code checkApakahMemenuhiSyaratOrganisasiKemahasiswaan}, nilai yang dibandingkan adalah
	 * {@code organisasiIntraKampus.getMinimalSks().intValue()} terhadap
	 * {@code krsMahasiswa.getSksk()} (bertipe {@code Integer}). Bagian desimal syarat
	 * <b>dibuang</b>, bukan dibulatkan: syarat {@code 120,9} efektif menjadi {@code 120}, sehingga
	 * mahasiswa dengan 120 SKS tetap lolos. Sebaliknya label yang ditampilkan di grid pendaftaran
	 * ("SKS Total &ge; ...") merender nilai {@code Double} aslinya, sehingga teks yang dibaca
	 * mahasiswa bisa berbeda dari ambang yang benar-benar ditegakkan.</p>
	 *
	 * <p>Perhatikan juga bahwa tipe {@code Double} di sini sekadar keseragaman dengan dua properti
	 * saudaranya; SKS pada dasarnya bilangan bulat.</p>
	 *
	 * @return syarat SKS minimal; {@code 0.0} bila belum diisi (artinya tanpa syarat), tidak pernah
	 *         {@code null}
	 */
	public Double getMinimalSks() {
		return minimalSks == null ? 0.0 : minimalSks;
	}

	/**
	 * Menetapkan syarat SKS kumulatif minimal. Dipanggil
	 * {@code OrganisasiIntraKampusAction#onSave} dari isian {@code MyDoublebox} "Minimal SKS" dan
	 * oleh alur impor generik.
	 *
	 * <p>Tidak ada validasi rentang maupun pembulatan; lihat kuirk pemotongan pada
	 * {@link #getMinimalSks()}.</p>
	 *
	 * @param minimalSks syarat SKS minimal; boleh {@code null}
	 */
	public void setMinimalSks(Double minimalSks) {
		this.minimalSks = minimalSks;
	}

	/**
	 * <b>Syarat angka kredit kegiatan kemahasiswaan (SKKM) minimal</b> yang harus dikumpulkan
	 * mahasiswa untuk mendaftar sebagai anggota organisasi ini.
	 *
	 * <p>Kontrak nilai, ambang {@code > 0.1}, dan daftar pemanggilnya sama persis dengan
	 * {@link #getMinimalIpk()} &mdash; lihat penjelasan lengkap di sana, termasuk catatan
	 * <i>fail-open</i>.</p>
	 *
	 * <p><b>Sumber angka pembandingnya:</b> berbeda dari dua properti saudaranya yang membaca
	 * ringkasan KRS, nilai yang dibandingkan di sini dihitung ulang oleh
	 * {@code Common.hitungAngkaKredit(mahasiswa)} &mdash; akumulasi rubrik
	 * {@link NilaiKegiatanKemahasiswaan} atas kegiatan yang pernah diikuti mahasiswa. Perhitungan
	 * itu relatif mahal dan dipanggil sekali per organisasi yang dicentang, jadi mencentang banyak
	 * organisasi sekaligus mengulang perhitungan yang sama berkali-kali.</p>
	 *
	 * <p><b>Kepanjangan SKKM</b> menurut catatan bantuan pada form Tambah/Ubah layar master
	 * ({@code Common.initKeterangan(rows, "SKKM = Surat Keterangan Kredit Mahasiswa")}) adalah
	 * <i>Surat Keterangan Kredit Mahasiswa</i> &mdash; bukan "satuan kredit" seperti yang biasa
	 * diduga. Label yang dirender di grid pendaftaran sendiri berbunyi
	 * <b>"Angka Kredit &ge; ..."</b>; jadi ada tiga istilah berbeda (nama properti, label form,
	 * label grid) untuk satu besaran yang sama.</p>
	 *
	 * @return syarat angka kredit minimal; {@code 0.0} bila belum diisi (artinya tanpa syarat),
	 *         tidak pernah {@code null}
	 */
	public Double getMinimalSkkm() {
		return minimalSkkm == null ? 0.0 : minimalSkkm;
	}

	/**
	 * Menetapkan syarat angka kredit kegiatan kemahasiswaan minimal. Dipanggil
	 * {@code OrganisasiIntraKampusAction#onSave} dari isian {@code MyDoublebox} "Minimal SKKM" dan
	 * oleh alur impor generik.
	 *
	 * @param minimalSkkm syarat angka kredit minimal; boleh {@code null}
	 */
	public void setMinimalSkkm(Double minimalSkkm) {
		this.minimalSkkm = minimalSkkm;
	}

	/**
	 * Nama organisasi dalam bahasa Inggris (kolom {@code namaen}, opsional).
	 *
	 * <p>Perhatikan nama kolom fisiknya seluruhnya huruf kecil tanpa pemisah ({@code namaen}),
	 * berbeda dari nama properti Java {@code namaEn}; anotasi {@code @Column} eksplisit inilah yang
	 * menjembatani keduanya.</p>
	 *
	 * <p>Dipakai untuk dokumen berbahasa Inggris (transkrip/borang internasional) dan ikut dalam
	 * daftar kolom ekspor/impor generik. Getter mengembalikan field apa adanya tanpa {@code trim()}
	 * &mdash; berbeda dari {@link #getNama()}.</p>
	 *
	 * @return nama organisasi dalam bahasa Inggris, atau {@code null} bila belum diisi
	 */
	@Column(name = "namaen")
	public String getNamaEn() {
		return namaEn;
	}

	/**
	 * Menetapkan nama organisasi dalam bahasa Inggris. Dipanggil
	 * {@code OrganisasiIntraKampusAction#onSave} dari isian "Nama Organisasi (dalam bhs inggris)".
	 *
	 * <p>Tidak ada validasi wajib-isi maupun keunikan untuk kolom ini.</p>
	 *
	 * @param namaEn nama organisasi dalam bahasa Inggris; boleh {@code null}/kosong
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}
}
