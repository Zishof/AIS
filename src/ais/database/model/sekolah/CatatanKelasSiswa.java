package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.CommonVO;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.WaktuUtil;

/**
 * Entity baris <b>Catatan Kelas</b> — satu catatan naratif + kumpulan parameter penilaian yang
 * melekat pada SATU <b>rombongan belajar</b> (kelas), untuk satu tahun ajaran dan satu semester.
 *
 * <p>Dipetakan ke tabel <code>sekolah.catatan_kelas_siswa</code>. Meski namanya mengandung kata
 * "Siswa", entity ini <b>BUKAN</b> catatan per peserta didik: kolom subjeknya adalah
 * {@link #getKelasSiswa()} yang menunjuk {@link ais.database.model.sekolah.KelasSiswa} — dan
 * {@code KelasSiswa} sendiri dipetakan ke tabel <code>sekolah.kelas</code>, yaitu master
 * <i>rombongan belajar</i> (nama kelas, tingkat, ruang, kurikulum, guru pembina/wali kelas, guru
 * BK). Jadi satu baris di sini berarti: "catatan X untuk kelas 7-A tahun 2025/2026 semester 1",
 * bukan "catatan X untuk siswa Budi". Tidak ada relasi ke {@code Siswa} maupun ke {@code Guru}
 * pada entity ini sama sekali.</p>
 *
 * <h3>Hubungan dengan {@code CatatanSiswa} — entity kembar, bukan turunan</h3>
 * <p>{@link ais.database.model.sekolah.CatatanSiswa} (tabel <code>sekolah.catatan_siswa</code>)
 * adalah <b>saudara kembar struktural</b> entity ini, bukan induk maupun turunannya: tidak ada
 * relasi database di antara keduanya, tidak ada kelas basis bersama selain
 * {@link ais.database.model.GeneralValueObject}. Yang membuat keduanya kembar adalah bentuk
 * kolom dan mekanismenya yang nyaris identik — {@code kode}/{@code nama}/{@code keterangan}/
 * {@code waktu}/{@code jenis*}/{@code sekolah}/{@code yayasan}/{@code tahunAjaran}/
 * {@code semester} plus PASANGAN blob {@code parameterTambahan} + {@code parameterTambahanInds}
 * dengan format pemisah <code>&lt;=&gt;</code> yang sama persis. Perbedaan yang menentukan:</p>
 * <ul>
 *   <li>{@code CatatanSiswa} punya TIGA relasi subjek — {@code siswa} (utama), {@code kelasSiswa}
 *   (konteks kelas saat catatan dibuat) dan {@code guru} (penulis catatan). Entity ini hanya
 *   punya {@code kelasSiswa}.</li>
 *   <li>Karena itu {@code CatatanSiswa} berisi data pribadi SATU peserta didik (dan karenanya
 *   menjadi temuan privasi berat pada audit terdahulu, lihat bagian keamanan di bawah),
 *   sedangkan baris di sini berisi penilaian/catatan atas satu rombel secara kolektif.</li>
 *   <li>Rantai konfigurasi parameternya terpisah penuh:
 *   {@link ais.database.model.sekolah.JenisCatatanKelasSiswa} &rarr;
 *   {@link ais.database.model.sekolah.KelompokParameterTambahanCatatanKelasSiswa} &rarr;
 *   {@link ais.database.model.sekolah.ParameterTambahanCatatanKelasSiswa}, sejajar tapi tidak
 *   berbagi baris dengan rantai {@code JenisCatatanSiswa}/{@code KelompokParameterTambahanCatatanSiswa}
 *   milik {@code CatatanSiswa}.</li>
 * </ul>
 *
 * <h3>Jalur pemakaian yang terverifikasi</h3>
 * <ul>
 *   <li><b>Layar master</b> — <code>/pages/master/sekolah/catatan_kelas_siswa.zul</code>,
 *   dikendalikan {@code ais.action.master.sekolah.CatatanKelasSiswaAction}. Layar ini menyisipkan
 *   dua halaman master lain lewat {@code MyInclude}: <code>jenis_catatan_kelas_siswa.zul</code>
 *   dan <code>parameter_tambahan_catatan_kelas_siswa.zul</code> (lihat catatan hak akses di
 *   bawah). Menu terdaftar dua kali di {@code ais.common.MenuSnapshotData}: id 18107 (di bawah
 *   modul kesiswaan guru/BK, root 431) dan id 22131 (di bawah "Pendataan" Sistem Sekolah, root
 *   5701).</li>
 *   <li><b>Dasbor</b> — {@code ais.action.master.catatan.DasbordCatatan} dengan
 *   {@code Lingkup.KELAS_SISWA}; pemuatnya {@code muatCatatanKelasSiswa()} menolak pengguna
 *   berperan siswa dan mempersempit ke kelas yang guru pembina-nya adalah pengguna yang login.</li>
 *   <li><b>Cetak per baris</b> — {@code ais.action.report.format1.sekolah.LaporanCatatanKelasSiswa#cetak}.
 *   Perlu diketahui: cetak ini juga dipicu OTOMATIS setiap kali baris disimpan dari layar master
 *   (dipanggil dari {@code onSave()} lewat timer), bukan hanya lewat tombol Cetak.</li>
 *   <li><b>Rapor siswa</b> — {@code LaporanRaporSiswa} menyertakan seluruh {@code CatatanKelasSiswa}
 *   milik kelas peserta didik ke dalam rapornya bila jenis rapor yang dipakai menyalakan
 *   {@code getAmbilCatatanSiswa()}. Konsekuensi praktis: isi entity ini <b>sampai ke tangan siswa
 *   dan orang tua</b> lewat lembar rapor, meskipun layar dan dasbornya tertutup bagi mereka.</li>
 *   <li><b>Indeks basis data</b> — {@code ais.common.InitIndex} membuat
 *   <code>idx_catatan_kelas_siswa_rapor</code> atas <code>(kelas_siswa, nama)</code>, sesuai pola
 *   akses rapor di atas.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota kelas ini</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}. Entity ber-{@code @Audited}
 *   (Hibernate Envers), sehingga setiap perubahan juga mengendap di tabel revisi.</li>
 *   <li><b>Subjek &amp; klasifikasi</b> — {@link #getKelasSiswa()} (rombel yang dicatat),
 *   {@link #getJenisCatatanKelasSiswa()} (jenis/kategori catatan yang sekaligus menentukan
 *   parameter apa saja yang harus diisi).</li>
 *   <li><b>Cermin identitas kelas</b> — {@link #getKode()} dan {@link #getNama()}; keduanya
 *   BUKAN nilai independen melainkan salinan dari kelas, lihat "getter yang menulis balik".</li>
 *   <li><b>Isi catatan</b> — {@link #getKeterangan()} (teks bebas) dan {@link #getWaktu()}.</li>
 *   <li><b>Periode akademik</b> — {@link #getTahunAjaran()} dan {@link #getSemester()}, keduanya
 *   ber-default ke periode berjalan bila kosong.</li>
 *   <li><b>Cakupan tenant</b> — {@link #getSekolah()} dan {@link #getYayasan()}, keduanya
 *   diturunkan dari kelas dan saling berantai.</li>
 *   <li><b>Parameter tambahan (blob teks)</b> — {@link #getParameterTambahan()},
 *   {@link #getParameterTambahanInds()}, {@link #ambilDataParameterTambahan()} dan
 *   {@link #populateParameterTambahan(List)}. Ini satu-satunya bagian entity yang memuat logika
 *   nyata.</li>
 * </ul>
 *
 * <h3>Dua blob paralel: {@code parameterTambahan} vs {@code parameterTambahanInds}</h3>
 * <p>Nilai isian dinamis TIDAK disimpan sebagai baris tabel anak, melainkan diserialisasi ke DUA
 * kolom {@code text} sekaligus oleh {@link #populateParameterTambahan(List)}. Keduanya berisi satu
 * baris per parameter (dipisah <code>\n</code>) dengan field dipisah <code>&lt;=&gt;</code>, namun
 * berbeda tujuan:</p>
 * <ul>
 *   <li><b>{@code parameterTambahan}</b> — versi <i>berlabel manusia</i>, dipakai untuk tampil dan
 *   cetak. Field: <code>namaKelompok-&gt;labelInputan</code>, nilai, url lampiran, nomor urut, id
 *   parameter, id kelompok, keterangan.</li>
 *   <li><b>{@code parameterTambahanInds}</b> — versi <i>berkunci id</i> ("Inds" = <i>ids</i>),
 *   dipakai untuk mencocokkan nilai kembali ke parameternya saat render. Field:
 *   <code>idKelompok-&gt;idParameter</code>, nilai, url lampiran, keterangan.</li>
 * </ul>
 * <p>Karena label ikut tersimpan di blob pertama, mengubah nama kelompok atau label parameter di
 * master TIDAK memperbarui baris catatan lama — baris lama tetap membawa label versi lamanya.
 * Sebaliknya blob kedua tahan terhadap perubahan label karena hanya memuat id.</p>
 *
 * <h3>Getter yang MENULIS BALIK ke state (non-obvious)</h3>
 * <p>Beberapa getter di kelas ini bukan pembaca murni; mereka mengubah field saat dibaca. Karena
 * Hibernate memakai <i>property access</i> (anotasi dipasang pada getter), efeknya nyata: nilai
 * hasil tulis-balik itulah yang ikut ter-<code>INSERT</code>/<code>UPDATE</code> saat flush.</p>
 * <ul>
 *   <li>{@link #getKode()} dan {@link #getNama()} <b>menimpa</b> field lokal dengan kode/nama
 *   kelas terkini setiap kali dipanggil. Kolom {@code nama} karena itu adalah <i>denormalisasi</i>
 *   nama rombel, bukan judul catatan — dan ia otomatis menyusul bila kelas diganti namanya, asalkan
 *   baris catatan tersentuh operasi tulis. Ini juga menjelaskan mengapa {@code toString()}
 *   menampilkan nama kelas.</li>
 *   <li>{@link #getSemester()} dan {@link #getTahunAjaran()} <b>mengisi</b> dirinya dengan periode
 *   akademik berjalan saat masih {@code null}. Membaca baris lama yang periodenya belum terisi
 *   akan MEMBERI CAP periode SAAT INI pada baris itu, bukan periode saat catatan dibuat.</li>
 *   <li>{@link #getSekolah()} dan {@link #getYayasan()} menurunkan nilainya dari kelas (dan
 *   yayasan dari sekolah), lalu menyimpannya. Efek baiknya: cakupan tenant baris ini praktis tidak
 *   bisa menyimpang dari kelasnya.</li>
 *   <li>{@link #getParameterTambahan()} dan {@link #getParameterTambahanInds()} mengubah
 *   {@code null} menjadi string kosong — cukup jinak, tapi tetap berarti membaca objek "kosong"
 *   akan menandainya kotor bagi Hibernate.</li>
 * </ul>
 * <p>Konsekuensi umum: <b>membaca entity ini dapat memicu penulisan</b> (mencetak laporan,
 * merender dasbor, atau sekadar mengiterasi hasil query bisa menghasilkan <code>UPDATE</code>).</p>
 *
 * <h3>Catatan keamanan / privasi</h3>
 * <p><b>Verifikasi NEGATIF terhadap temuan berat {@code CatatanSiswa}:</b> pada audit terdahulu
 * ditemukan bahwa penyemai bawaan {@code ais.common.MenuInitializer#ensureSiswaRoleAndPrivileges()}
 * memberi role SISWA hak <i>create/read/update/delete</i> PENUH atas menu 48916
 * (<code>catatan_siswa.zul</code>) — artinya peserta didik dapat mengubah dan menghapus catatan
 * pribadi tentang dirinya sendiri. <b>Pola itu TIDAK berulang untuk entity ini.</b> Daftar menu
 * yang disemai ke role siswa hanya memuat 431898, 127616 dan 48916; menu Catatan Kelas (18107 dan
 * 22131) tidak ada di daftar mana pun, dan tidak terdaftar di modul portal siswa (root 73) seperti
 * halnya {@code catatan_siswa.zul}. Tidak ada pula penyemaian hak untuk role orang tua/wali di
 * seluruh repo. Jadi akses tulis atas baris-baris ini hanya muncul bila diberikan manual lewat
 * layar Grup Pengguna.</p>
 * <p>Yang <b>tetap perlu diwaspadai</b> pada jalur entity ini:</p>
 * <ul>
 *   <li><b>Cakupan tenant fail-open pada pencarian layar master.</b>
 *   {@code CatatanKelasSiswaAction#initCriteria} memasang {@code Restrictions.sqlRestriction("1=1")}
 *   untuk SETIAP filter yang tidak dipilih, termasuk filter Sekolah dan Yayasan (combo-nya diisi
 *   dengan opsi "Semua"). Tanpa pilihan eksplisit, daftar yang tampil mencakup catatan kelas
 *   SELURUH sekolah dan yayasan pada instalasi, bukan hanya milik tenant pengguna. Entity ini
 *   sendiri tidak dapat disalahkan — {@link #getSekolah()}/{@link #getYayasan()} justru rajin
 *   mengisi kolom tenant — kelemahannya murni di sisi kueri pemanggil.</li>
 *   <li><b>Pewarisan hak lewat halaman yang disisipkan.</b> Pemeriksaan hak
 *   ({@code CommonPrivilages.checkPrevilages}) dievaluasi terhadap <i>menu yang sedang aktif</i>.
 *   Karena layar Catatan Kelas menyisipkan master Jenis Catatan dan master Parameter Tambahan di
 *   dalam tab-nya sendiri, hak CRUD atas menu "Catatan Kelas" otomatis berlaku atas kedua master
 *   itu tanpa pemeriksaan hak menu masing-masing. Layar hanya menyembunyikan kedua tab tersebut
 *   secara kosmetik bagi pengguna berperan guru atau siswa.</li>
 *   <li><b>Pendaftaran menu ganda.</b> URL yang sama terdaftar sebagai dua id menu (18107 dan
 *   22131) dengan baris hak akses terpisah, sehingga tingkat hak yang berlaku bergantung pada
 *   dari mana halaman dibuka.</li>
 *   <li><b>Kunci lampiran tidak ber-namespace entity (potensi bocor silang).</b>
 *   {@link #populateParameterTambahan(List)} mengambil lampiran dengan
 *   {@code LampiranLain.ambil(getId(), "idKelompok->idParameter")}. Pasangan (ref, jenis) itu tidak
 *   memuat penanda kelas pemilik, sedangkan {@code CatatanSiswa} memakai format kunci yang PERSIS
 *   sama dengan id-nya sendiri. Dua tabel berbeda dengan urutan {@code IDENTITY} masing-masing akan
 *   dengan mudah menghasilkan id yang sama, sehingga lampiran catatan pribadi seorang siswa dan
 *   lampiran catatan kelas dapat saling tertukar bila id catatan dan id kelompok parameter
 *   kebetulan berimpit. Banyak entity lain di repo ini memakai {@code Kelas.class.getName()}
 *   sebagai {@code jenis} justru untuk menghindari hal ini.</li>
 *   <li><b>Rapor sebagai kanal keluar.</b> Seperti disebut di atas, isi entity ini ikut tercetak
 *   pada rapor siswa. Catatan yang ditulis dengan asumsi "hanya dibaca guru" akan terbaca wali
 *   murid bila jenis rapor menyalakan {@code ambilCatatanSiswa}.</li>
 * </ul>
 *
 * <h3>Bug yang ditemukan pada pemanggil (bukan pada entity ini)</h3>
 * <p>{@code CatatanKelasSiswaAction#initCriteria} menyaring kotak pencarian dengan
 * {@code Restrictions.ilike("kelasSiswa.nomorIndukNasional", ...)} dan
 * {@code Restrictions.ilike("kelasSiswa.nomorInduk", ...)}. Kedua properti itu TIDAK ADA pada
 * {@link ais.database.model.sekolah.KelasSiswa} maupun pada seluruh rantai induknya — keduanya
 * milik {@code Siswa}, dan barisnya jelas hasil salin-tempel dari {@code CatatanSiswaAction} yang
 * di sana memang beralias {@code siswa}. Akibatnya Hibernate melempar kegagalan resolusi properti
 * begitu kotak pencarian diisi: <b>pencarian pada layar Catatan Kelas tidak pernah bisa dipakai</b>.
 * Dicatat di sini agar pembaca entity tidak mengira {@code KelasSiswa} punya nomor induk.</p>
 *
 * <h3>Mengapa field audit dideklarasikan ulang</h3>
 * <p>{@link ais.database.model.GeneralValueObject} adalah POJO abstrak biasa — <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti
 * apa pun miliknya. Deklarasi ulang {@code oleh}, {@code olehId} dan {@code tanggal_dirubah} di
 * kelas ini karena itu bukan duplikasi yang keliru, melainkan keharusan teknis agar ketiga kolom
 * audit benar-benar terpetakan.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.CatatanSiswa
 * @see ais.database.model.sekolah.KelasSiswa
 * @see ais.database.model.sekolah.JenisCatatanKelasSiswa
 * @see ais.database.model.sekolah.KelompokParameterTambahanCatatanKelasSiswa
 * @see ais.database.model.sekolah.ParameterTambahanCatatanKelasSiswa
 * @see ais.database.model.ParameterTambahan
 * @see ais.database.model.file.LampiranLain
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "catatan_kelas_siswa")
public class CatatanKelasSiswa extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id} (IDENTITY). */
	private Long id;

	/**
	 * Nama pengguna yang terakhir mengubah baris ini. Diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * <p>Dideklarasikan ulang di sini karena {@link ais.database.model.GeneralValueObject} tidak
	 * dipetakan Hibernate (bukan {@code @MappedSuperclass}).</p>
	 */
	private String oleh;

	/**
	 * Id pengguna yang terakhir mengubah baris ini. Pasangan teknis dari {@link #oleh}, diisi
	 * otomatis oleh {@link #onUpdate()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/berisi spasi saja — nilai lama dipertahankan, tanpa pesan kesalahan. Jadi jejak audit
	 * tidak bisa dihapus dengan menyetel ulang ke kosong, dan pemanggil yang bermaksud
	 * mengosongkan kolom ini akan gagal tanpa tanda apa pun.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p>Berperilaku sama dengan {@link #setOlehId(String)}: nilai {@code null} atau kosong
	 * diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum baris ini di-{@code UPDATE},
	 * meneruskan ke {@code AuditTimestampInterceptor.ubah(this)} untuk mengisi
	 * {@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()} dari pengguna sesi
	 * aktif.
	 *
	 * <p><b>Efek samping:</b> mengubah state entity. Tidak untuk dipanggil manual dari kode
	 * aplikasi. Perhatikan bahwa kait ini hanya berjalan pada {@code UPDATE}; pengisian awal saat
	 * {@code INSERT} bergantung pada nilai bawaan field dan pada pemanggil.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan objek sehingga baris
	 * baru tidak pernah bernilai {@code null}, lalu diperbarui {@link #onUpdate()} tiap
	 * {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis oleh {@link #onUpdate()}; pemanggilan manual biasanya hanya pada
	 * jalur impor/migrasi data.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini dalam bentuk <code>{id}-{nama}</code>.
	 *
	 * <p><b>Non-obvious:</b> yang tampil setelah tanda hubung adalah <b>nama KELAS</b>, bukan judul
	 * catatan — lihat {@link #getNama()} yang selalu menyalin nama dari
	 * {@link #getKelasSiswa()}. Method ini membaca field {@code nama} secara langsung (bukan lewat
	 * getter), sehingga pada objek yang belum pernah dibaca lewat {@link #getNama()} nilainya bisa
	 * masih {@code null} dan tercetak sebagai <code>{id}-null</code>.</p>
	 *
	 * @return string <code>{id}-{nama}</code>
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode kelas hasil salinan; lihat {@link #getKode()} — bukan nilai independen.
	 *
	 * <p>Tidak beranotasi {@code @Column} pada getter-nya, sehingga Hibernate memetakannya dengan
	 * nama kolom bawaan {@code kode}.</p>
	 */
	private String kode;

	/**
	 * Rombongan belajar (kelas) yang dicatat — subjek tunggal baris ini. Kolom
	 * {@code kelas_siswa}.
	 */
	private KelasSiswa kelasSiswa;

	/** Nama kelas hasil salinan; lihat {@link #getNama()} — bukan judul catatan. */
	private String nama;

	/** Isi catatan naratif bebas, kolom {@code keterangan} bertipe {@code text}. */
	private String keterangan;

	/** Waktu kejadian/pencatatan, kolom {@code waktu} bertipe {@code timestamp}. */
	private Date waktu;

	/**
	 * Jenis/kategori catatan. Selain berperan sebagai label, jenis inilah yang menentukan kelompok
	 * parameter mana saja yang harus diisi pada baris ini.
	 */
	private JenisCatatanKelasSiswa jenisCatatanKelasSiswa;

	/** Sekolah pemilik baris; diturunkan dari kelas oleh {@link #getSekolah()}. */
	private Sekolah sekolah;

	/** Yayasan pemilik baris; diturunkan dari sekolah oleh {@link #getYayasan()}. */
	private Yayasan yayasan;

	/**
	 * Blob teks nilai parameter versi <i>berlabel manusia</i> (untuk tampil/cetak). Format
	 * dijelaskan pada Javadoc kelas dan pada {@link #populateParameterTambahan(List)}.
	 */
	private String parameterTambahan;

	/**
	 * Blob teks nilai parameter versi <i>berkunci id</i> (untuk pencocokan balik saat render).
	 * Format dijelaskan pada Javadoc kelas dan pada {@link #populateParameterTambahan(List)}.
	 */
	private String parameterTambahanInds;

	/**
	 * Tahun ajaran, format <code>2025/2026</code> (maks. 9 karakter). Ber-default ke tahun
	 * akademik berjalan saat dibaca dalam keadaan kosong — lihat {@link #getTahunAjaran()}.
	 */
	private String tahunAjaran;

	/**
	 * Semester: {@code 1} untuk ganjil, {@code 2} untuk genap. Ber-default ke semester berjalan
	 * saat dibaca dalam keadaan kosong — lihat {@link #getSemester()}.
	 */
	private Integer semester;

	/**
	 * Konstruktor kosong wajib bagi Hibernate.
	 *
	 * <p>Objek yang dibuat lewat konstruktor ini sudah membawa {@link #getTanggal_dirubah()}
	 * terisi waktu pembuatan; seluruh field lain masih {@code null} sampai diisi pemanggil atau
	 * diisi sendiri oleh getter yang menulis balik.</p>
	 */
	public CatatanKelasSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
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
	 * Menetapkan kunci utama baris ini.
	 *
	 * <p>Biasanya hanya dipanggil Hibernate; pengisian manual dipakai pada jalur impor/migrasi.</p>
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode kelas yang dicatat.
	 *
	 * <p><b>Getter dengan efek samping.</b> Bila {@link #getKelasSiswa()} tidak {@code null},
	 * field {@code kode} <b>ditimpa</b> dengan kode kelas terkini sebelum dikembalikan — nilai apa
	 * pun yang pernah disetel lewat {@link #setKode(String)} akan hilang. Karena Hibernate memakai
	 * property access, nilai hasil timpaan inilah yang ikut tersimpan pada flush berikutnya,
	 * sehingga kolom ini berperilaku sebagai denormalisasi kode kelas yang menyusul otomatis.</p>
	 *
	 * <p>Hasilnya selalu di-{@code trim} dan tidak pernah {@code null}: bila kelas belum diisi dan
	 * kode masih kosong, yang dikembalikan adalah string kosong.</p>
	 *
	 * @return kode kelas (sudah di-trim), atau string kosong bila tidak tersedia
	 */
	public String getKode() {
		if (getKelasSiswa() != null) {
			kode = getKelasSiswa().getKode();
		}
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menetapkan kode.
	 *
	 * <p><b>Perhatian:</b> nilai yang disetel di sini hanya bertahan selama
	 * {@link #getKelasSiswa()} bernilai {@code null}; begitu kelas terisi, pembacaan berikutnya
	 * lewat {@link #getKode()} akan menimpanya.</p>
	 *
	 * @param kode kode yang hendak disimpan
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama kelas yang dicatat.
	 *
	 * <p><b>Getter dengan efek samping — dan kolom yang mudah disalahpahami.</b> Meski kolomnya
	 * bernama {@code nama} dan berstatus {@code nullable = false}, isinya BUKAN judul catatan
	 * melainkan salinan nama rombongan belajar: bila {@link #getKelasSiswa()} tidak {@code null},
	 * field {@code nama} ditimpa dengan nama kelas terkini setiap kali getter ini dipanggil.
	 * Konsekuensinya kolom ini menyusul otomatis saat kelas diganti nama — tapi hanya untuk baris
	 * yang tersentuh operasi tulis; baris yang tidak pernah tersentuh tetap membawa nama lama,
	 * sehingga dua baris untuk kelas yang sama bisa menampilkan nama berbeda.</p>
	 *
	 * <p>Kolom ini juga menjadi kunci pengurutan laporan rapor (indeks
	 * <code>idx_catatan_kelas_siswa_rapor</code> atas <code>(kelas_siswa, nama)</code>) dan menjadi
	 * bagian dari {@link #toString()}.</p>
	 *
	 * @return nama kelas (sudah di-trim), atau {@code null} bila belum tersedia
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (getKelasSiswa() != null) {
			nama = getKelasSiswa().getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setKode(String)}, nilai ini akan ditimpa nama kelas
	 * pada pembacaan berikutnya begitu {@link #getKelasSiswa()} terisi.</p>
	 *
	 * @param nama nama yang hendak disimpan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan isi catatan naratif.
	 *
	 * <p>Kolom bertipe {@code text} tanpa batas panjang. Isinya ditampilkan apa adanya pada daftar
	 * layar master, pada dasbor catatan, pada cetakan {@code LaporanCatatanKelasSiswa}, dan — bila
	 * jenis rapor menyalakan pengambilan catatan — pada rapor siswa.</p>
	 *
	 * @return isi catatan, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan isi catatan naratif.
	 *
	 * @param keterangan teks catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan semester periode catatan ({@code 1} = ganjil, {@code 2} = genap).
	 *
	 * <p><b>Getter dengan efek samping.</b> Bila field masih {@code null}, getter ini MENGISINYA
	 * dengan semester yang sedang berjalan menurut {@code Common.isNowSemensterGanjil()} lalu
	 * mengembalikan nilai itu. Karena property access, isian tersebut ikut tersimpan pada flush
	 * berikutnya.</p>
	 *
	 * <p><b>Risiko yang perlu disadari:</b> baris lama yang semesternya belum pernah terisi akan
	 * mendapat cap semester <i>saat pembacaan</i>, bukan semester saat catatan dibuat. Membaca
	 * arsip lama di tengah semester genap dapat menandai catatan lama sebagai catatan semester
	 * genap secara permanen.</p>
	 *
	 * @return {@code 1} untuk semester ganjil atau {@code 2} untuk genap; tidak pernah {@code null}
	 */
	@Column(name = "semester", nullable = true)
	public Integer getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil() ? 1 : 2;
		}
		return this.semester;
	}

	/**
	 * Menetapkan semester periode catatan.
	 *
	 * <p>Diisi dari kombo Semester pada layar master. Menyetel {@code null} tidak benar-benar
	 * mengosongkan nilai secara permanen: pembacaan berikutnya lewat {@link #getSemester()} akan
	 * mengisinya kembali dengan semester berjalan.</p>
	 *
	 * @param semester {@code 1} untuk ganjil, {@code 2} untuk genap
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tahun ajaran periode catatan, format <code>2025/2026</code>.
	 *
	 * <p><b>Getter dengan efek samping</b>, sejalan dengan {@link #getSemester()}: bila masih
	 * {@code null}, field diisi dengan tahun akademik berjalan
	 * ({@code Common.getCurrentTahunAkademik()}) dan nilai itu ikut tersimpan pada flush
	 * berikutnya. Berlaku peringatan yang sama soal baris arsip yang mendapat cap periode saat
	 * dibaca.</p>
	 *
	 * @return tahun ajaran berformat <code>YYYY/YYYY</code>; tidak pernah {@code null}
	 */
	@Column(name = "tahun_ajaran", nullable = true, length = 9)
	public String getTahunAjaran() {

		if (tahunAjaran == null) {
			tahunAjaran = Common.getCurrentTahunAkademik();
		}
		return this.tahunAjaran;
	}

	/**
	 * Menetapkan tahun ajaran periode catatan.
	 *
	 * <p>Diisi dari kombo Tahun Ajaran pada layar master. Panjang kolom dibatasi 9 karakter,
	 * pas untuk format <code>YYYY/YYYY</code>; nilai yang lebih panjang akan ditolak basis data.</p>
	 *
	 * @param tahunAjaran tahun ajaran berformat <code>YYYY/YYYY</code>
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * Mengembalikan waktu kejadian/pencatatan.
	 *
	 * <p><b>Non-obvious — nilai bawaan TIDAK tersimpan.</b> Berbeda dari
	 * {@link #getSemester()}/{@link #getTahunAjaran()}, getter ini hanya mengembalikan waktu saat
	 * ini sebagai pengganti tampilan ketika field masih {@code null}; ia TIDAK menulis balik ke
	 * field. Akibatnya sebuah baris yang kolom {@code waktu}-nya {@code NULL} di basis data akan
	 * tampak "baru saja" setiap kali dimuat, dan nilainya berubah-ubah tiap pembacaan — tanggal
	 * yang tampil pada dasbor maupun cetakan untuk baris semacam itu tidak dapat dipercaya.</p>
	 *
	 * @return waktu catatan; bila field kosong, waktu server saat pemanggilan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menetapkan waktu kejadian/pencatatan.
	 *
	 * <p>Diisi dari komponen tanggal pada layar master saat menyimpan.</p>
	 *
	 * @param waktu waktu catatan
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan jenis/kategori catatan.
	 *
	 * <p>Jenis bukan sekadar label: {@link ais.database.model.sekolah.JenisCatatanKelasSiswa}
	 * memuat himpunan {@link ais.database.model.sekolah.KelompokParameterTambahanCatatanKelasSiswa}
	 * yang menentukan parameter apa saja yang wajib diisi pada baris ini, baik saat form dirender
	 * maupun saat baris ditampilkan kembali.</p>
	 *
	 * <p><b>Efek samping:</b> nilai dilewatkan lebih dulu ke {@code check()} milik
	 * {@link ais.database.model.GeneralValueObject}, yang menormalkan proxy Hibernate yang tidak
	 * dapat di-<i>initialize</i> (mis. baris terkait sudah terhapus) menjadi {@code null} dan
	 * menulis hasilnya kembali ke field. Ini mencegah {@code ObjectNotFoundException} merembet ke
	 * lapisan tampilan, tetapi juga berarti relasi yang menggantung akan hilang diam-diam.</p>
	 *
	 * @return jenis catatan, atau {@code null} bila belum diisi atau relasinya tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_catatan_kelasSiswa")
	public JenisCatatanKelasSiswa getJenisCatatanKelasSiswa() {
		jenisCatatanKelasSiswa = check(jenisCatatanKelasSiswa);
		return jenisCatatanKelasSiswa;
	}

	/**
	 * Menetapkan jenis/kategori catatan.
	 *
	 * <p>Diisi dari kombo Jenis Catatan pada layar master, yang sudah dipersempit ke jenis
	 * ber-{@code aktif = true} milik sekolah terpilih.</p>
	 *
	 * @param jenisCatatanKelasSiswa jenis catatan yang dipilih
	 */
	public void setJenisCatatanKelasSiswa(JenisCatatanKelasSiswa jenisCatatanKelasSiswa) {
		this.jenisCatatanKelasSiswa = jenisCatatanKelasSiswa;
	}

	/**
	 * Mengembalikan rombongan belajar (kelas) yang dicatat — subjek tunggal baris ini.
	 *
	 * <p>{@link ais.database.model.sekolah.KelasSiswa} dipetakan ke tabel
	 * <code>sekolah.kelas</code>: ia adalah master ROMBEL (nama, tingkat, ruang, kurikulum, guru
	 * pembina/wali kelas, guru BK), <b>bukan</b> peserta didik dan bukan pula baris pendaftaran
	 * siswa ke kelas (yang itu adalah {@code KelasSiswaPunyaSiswa}). Relasi inilah yang membedakan
	 * entity ini dari {@link ais.database.model.sekolah.CatatanSiswa}, yang subjek utamanya
	 * {@code Siswa}.</p>
	 *
	 * <p>Relasi ini juga menjadi sumber turunan bagi {@link #getKode()}, {@link #getNama()} dan
	 * {@link #getSekolah()}, sehingga mengganti kelas pada sebuah baris ikut memindahkan
	 * kepemilikan tenant baris tersebut.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check()} seperti pada
	 * {@link #getJenisCatatanKelasSiswa()} — proxy yang tidak dapat dimuat dinormalkan menjadi
	 * {@code null} dan ditulis balik ke field.</p>
	 *
	 * @return kelas yang dicatat, atau {@code null} bila belum diisi atau relasinya tidak dapat
	 *         dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_siswa")
	public KelasSiswa getKelasSiswa() {
		kelasSiswa = check(kelasSiswa);
		return kelasSiswa;
	}

	/**
	 * Menetapkan rombongan belajar yang dicatat.
	 *
	 * <p>Wajib diisi dari sisi aplikasi: {@code onSave()} pada layar master menolak penyimpanan
	 * bila kelas belum dipilih. Kolom {@code kelas_siswa} sendiri tidak dipaksa
	 * {@code NOT NULL} di tingkat pemetaan, sehingga baris tanpa kelas masih mungkin muncul lewat
	 * jalur impor — dan baris semacam itu akan gagal dirender pada layar master, yang membaca
	 * {@code kelasSiswa.getKurikulumSekolah()} tanpa pemeriksaan {@code null} lebih dulu.</p>
	 *
	 * @param kelasSiswa kelas/rombel yang dicatat
	 */
	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}

	/**
	 * Mengembalikan sekolah pemilik baris ini.
	 *
	 * <p><b>Getter dengan efek samping — sekaligus penjaga konsistensi tenant.</b> Bila
	 * {@link #getKelasSiswa()} terisi, sekolah SELALU diambil ulang dari kelas dan menimpa nilai
	 * yang tersimpan; kolom {@code sekolah_id} karena itu tidak dapat menyimpang dari sekolah
	 * pemilik kelas, meski pengguna memilih sekolah lain pada kombo di layar master. Hanya bila
	 * kelas kosong, nilai tersimpan dipakai apa adanya (setelah dinormalkan {@code check()}).</p>
	 *
	 * @return sekolah pemilik baris, atau {@code null} bila tidak dapat ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		if (getKelasSiswa() != null) {
			sekolah = getKelasSiswa().getSekolah();
		} else {
			sekolah = check(sekolah);
		}
		return this.sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik baris ini.
	 *
	 * <p><b>Non-obvious:</b> objek {@code Sekolah} yang belum punya id (mis. instance kosong hasil
	 * kombo yang belum dipilih) dinormalkan menjadi {@code null}, bukan disimpan apa adanya — ini
	 * mencegah Hibernate mencoba meng-{@code cascade} penyimpanan sekolah baru yang tidak
	 * dikehendaki lewat {@code CascadeType.PERSIST}.</p>
	 *
	 * <p>Nilai yang disetel di sini hanya menentukan hasil selama kelas belum terisi; lihat
	 * {@link #getSekolah()}.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id akan disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris ini.
	 *
	 * <p><b>Getter dengan efek samping berantai.</b> Method ini memanggil {@link #getSekolah()}
	 * lebih dulu — sehingga ikut memicu seluruh efek samping getter tersebut, termasuk
	 * pengambilan ulang sekolah dari kelas — lalu menurunkan yayasan dari sekolah itu, dan terakhir
	 * menormalkan hasilnya lewat {@code check()}. Rantai ini membuat kolom {@code yayasan_id}
	 * praktis selalu konsisten dengan kelas: kelas &rarr; sekolah &rarr; yayasan.</p>
	 *
	 * <p>Bila sekolah tidak dapat ditentukan, nilai tersimpan dipakai apa adanya.</p>
	 *
	 * @return yayasan pemilik baris, atau {@code null} bila tidak dapat ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menetapkan yayasan pemilik baris ini.
	 *
	 * <p>Berperilaku sama dengan {@link #setSekolah(Sekolah)}: objek tanpa id dinormalkan menjadi
	 * {@code null} agar tidak ter-{@code cascade} sebagai yayasan baru.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id akan disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan blob nilai parameter versi <i>berkunci id</i>.
	 *
	 * <p>Satu baris per parameter (dipisah <code>\n</code>), tiap baris terdiri atas empat bagian
	 * yang dipisah <code>&lt;=&gt;</code>:</p>
	 * <ol>
	 *   <li><code>{idKelompok}-&gt;{idParameter}</code> — kunci pencocokan,</li>
	 *   <li>nilai isian,</li>
	 *   <li>URL lampiran (kosong bila parameter tidak menuntut lampiran),</li>
	 *   <li>keterangan tambahan per parameter.</li>
	 * </ol>
	 *
	 * <p>Inilah blob yang dibaca saat render: renderer layar master dan
	 * {@code LaporanRaporSiswa} membangun kunci <code>{idKelompok}-&gt;{idParameter}</code> dari
	 * konfigurasi jenis catatan, lalu mencari baris yang cocok di sini. Karena hanya memuat id,
	 * blob ini tetap benar meski label parameter diubah di master.</p>
	 *
	 * <p><b>Efek samping ringan:</b> nilai {@code null} diubah menjadi string kosong dan ditulis
	 * balik ke field.</p>
	 *
	 * @return blob berkunci id; tidak pernah {@code null}, bisa berupa string kosong
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}

		return parameterTambahanInds;
	}

	/**
	 * Menetapkan blob nilai parameter versi berkunci id.
	 *
	 * <p>Dalam alur normal hanya dipanggil dari {@link #populateParameterTambahan(List)}; menyetel
	 * langsung berarti memikul sendiri tanggung jawab menjaga format dan menjaga agar tetap sejalan
	 * dengan {@link #getParameterTambahan()}.</p>
	 *
	 * @param parameterTambahanInds blob berkunci id
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Memecah blob {@link #getParameterTambahan()} (versi berlabel manusia) menjadi daftar
	 * {@link ais.database.model.CommonVO} siap tampil, terurut menurut nomor urut parameter.
	 *
	 * <p>Setiap baris blob dipecah dengan <code>&lt;=&gt;</code> lalu dipetakan ke properti
	 * {@code CommonVO} sebagai berikut:</p>
	 * <ul>
	 *   <li>bagian ke-1 &rarr; {@code name} — label gabungan
	 *   <code>{namaKelompok}-&gt;{labelInputan}</code>; bagian sebelum <code>-&gt;</code>
	 *   dipisahkan lagi ke {@code name5} sehingga pemanggil dapat mengelompokkan per kelompok
	 *   parameter,</li>
	 *   <li>bagian ke-2 &rarr; {@code name1} — nilai isian,</li>
	 *   <li>bagian ke-3 &rarr; {@code name2} — URL lampiran,</li>
	 *   <li>bagian ke-4 &rarr; {@code nomorUrut} — urutan tampil,</li>
	 *   <li>bagian ke-5 &rarr; {@code id} — id parameter (disimpan sebagai string).</li>
	 * </ul>
	 *
	 * <p><b>Ketahanan terhadap data cacat:</b> setiap bagian diambil dengan pemeriksaan panjang
	 * array, sehingga baris yang tidak lengkap menghasilkan string kosong alih-alih melempar
	 * kesalahan. Nomor urut dan id yang tidak dapat diurai jatuh ke nilai bawaan {@code 1} dan
	 * {@code 1L}, dengan kesalahannya dicatat ke {@code ErrorAuditUtil} — perhatikan bahwa nilai
	 * bawaan {@code id = 1L} ini dipakai untuk SEMUA baris cacat, sehingga beberapa entri bisa
	 * berbagi id palsu yang sama.</p>
	 *
	 * <p><b>Kuirk:</b> bila blob kosong, {@code "".split("\n")} tetap menghasilkan satu elemen
	 * kosong, sehingga hasil method ini adalah daftar berisi SATU {@code CommonVO} kosong, bukan
	 * daftar kosong. Pemanggil perlu menyaringnya sendiri.</p>
	 *
	 * <p>Method ini murni membaca — tidak mengubah blob dan tidak menyentuh basis data (kecuali
	 * efek samping ringan {@link #getParameterTambahan()} yang mengganti {@code null} menjadi
	 * string kosong).</p>
	 *
	 * @return daftar {@code CommonVO} hasil penguraian, sudah diurutkan lewat
	 *         {@code Collections.sort}; tidak pernah {@code null}
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CatatanKelasSiswa.java:258");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CatatanKelasSiswa.java:264");

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
	 * Menyerap isian form parameter tambahan dari baris-baris komponen ZK dan menyerialisasinya ke
	 * KEDUA blob teks entity ini ({@link #getParameterTambahan()} dan
	 * {@link #getParameterTambahanInds()}).
	 *
	 * <p><b>Kapan dipanggil:</b> dari {@code ParameterTambahanCatatanKelasSiswaListener} — baik
	 * pada {@code onSave(CatatanKelasSiswa)} sebelum baris disimpan lewat
	 * {@code Common.refreshSaveOrUpdate}, maupun saat isian form berubah. Tidak dipanggil dari
	 * jalur lain.</p>
	 *
	 * <p><b>Bentuk masukan:</b> setiap {@link org.zkoss.zul.Row} diharapkan membawa tiga atribut
	 * yang dipasang saat form dibangun — {@code "parameterTambahan"}
	 * ({@link ais.database.model.ParameterTambahan}), {@code "kelompokParameterTambahanCatatanKelasSiswa"}
	 * ({@link ais.database.model.sekolah.KelompokParameterTambahanCatatanKelasSiswa}) dan
	 * opsional {@code "keterangan"} (sebuah {@link org.zkoss.zul.Textbox}). Baris yang salah satu
	 * dari dua atribut pertamanya {@code null} dilewati diam-diam. Nilai isian itu sendiri dibaca
	 * lewat {@code ParameterTambahan.ambilVal(row, parameterTambahan)}, yang tahu cara mengambil
	 * nilai dari beragam tipe komponen input.</p>
	 *
	 * <p><b>Bentuk keluaran.</b> Blob berlabel ({@code parameterTambahan}) — tujuh bagian
	 * dipisah <code>&lt;=&gt;</code>: <code>{namaKelompok}-&gt;{labelInputan}</code>, nilai, url
	 * lampiran, nomor urut, id parameter, id kelompok, keterangan. Blob berkunci id
	 * ({@code parameterTambahanInds}) — empat bagian:
	 * <code>{idKelompok}-&gt;{idParameter}</code>, nilai, url lampiran, keterangan. Baris dipisah
	 * <code>\n</code> pada keduanya.</p>
	 *
	 * <p><b>Lampiran.</b> Untuk parameter yang menuntut lampiran
	 * ({@code getHarusMenyertakanLampiran()}), method ini mencari berkasnya dengan
	 * {@code LampiranLain.ambil(getId(), "{idKelompok}->{idParameter}")} lalu menyisipkan URL
	 * unduhnya ke kedua blob. Dua hal yang perlu diketahui:</p>
	 * <ul>
	 *   <li>Pada baris BARU {@link #getId()} masih {@code null} saat method ini berjalan (ia
	 *   dipanggil sebelum penyimpanan), sehingga URL lampiran pada penyimpanan pertama akan
	 *   kosong dan baru terisi pada penyimpanan berikutnya.</li>
	 *   <li>Pasangan kunci (ref, jenis) itu <b>tidak memuat penanda kelas pemilik</b>, sementara
	 *   {@link ais.database.model.sekolah.CatatanSiswa} memakai format kunci yang identik dengan
	 *   id-nya sendiri. Karena kedua tabel punya urutan {@code IDENTITY} terpisah, id yang sama
	 *   mudah muncul di keduanya — lampiran catatan pribadi seorang siswa dan lampiran catatan
	 *   kelas berpotensi saling tertukar. Bandingkan dengan entity lain di repo ini yang memakai
	 *   {@code Kelas.class.getName()} sebagai {@code jenis} justru untuk mencegah tabrakan
	 *   semacam itu.</li>
	 * </ul>
	 *
	 * <p><b>Penanganan kesalahan:</b> setiap baris diproses dalam blok {@code try} sendiri; galat
	 * pada satu baris hanya dilaporkan lewat {@code Common.tampilErrorJikaAdmin} (terlihat oleh
	 * admin saja) dan baris itu HILANG dari hasil — penyimpanan tetap dilanjutkan dengan blob yang
	 * tidak lengkap, tanpa peringatan bagi pengguna biasa. Galat saat menyusun URL lampiran
	 * ditangani serupa, dengan URL dibiarkan kosong.</p>
	 *
	 * <p><b>Efek samping:</b> menimpa PENUH kedua blob lewat
	 * {@link #setParameterTambahanInds(String)} dan {@link #setParameterTambahan(String)} — isi
	 * lama tidak digabung, melainkan diganti. Bila {@code parameterRows} {@code null} atau kosong,
	 * method langsung keluar tanpa menyentuh apa pun, sehingga blob lama dipertahankan. Method ini
	 * juga membaca basis data (pencarian {@code LampiranLain}) namun tidak membuka atau menutup
	 * transaksi sendiri.</p>
	 *
	 * @param parameterRows baris-baris komponen ZK berisi isian parameter; {@code null} atau
	 *                      kosong berarti tidak ada perubahan
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
				KelompokParameterTambahanCatatanKelasSiswa kelompokParameterTambahanCatatanKelasSiswa = (KelompokParameterTambahanCatatanKelasSiswa) row
						.getAttribute("kelompokParameterTambahanCatatanKelasSiswa");
				if (parameterTambahan != null && kelompokParameterTambahanCatatanKelasSiswa != null) {
					String jenis = kelompokParameterTambahanCatatanKelasSiswa.getId() + "->"
							+ parameterTambahan.getId();

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);
					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null && row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan") : null);
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

					String s = kelompokParameterTambahanCatatanKelasSiswa.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanCatatanKelasSiswa.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanCatatanKelasSiswa.getId() + "->" + parameterTambahan.getId()
							+ "<=>" + val + "<=>" + url + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());
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
	 * Mengembalikan blob nilai parameter versi <i>berlabel manusia</i>.
	 *
	 * <p>Satu baris per parameter (dipisah <code>\n</code>), tiap baris terdiri atas tujuh bagian
	 * yang dipisah <code>&lt;=&gt;</code>: <code>{namaKelompok}-&gt;{labelInputan}</code>, nilai,
	 * url lampiran, nomor urut, id parameter, id kelompok, keterangan. Dipakai untuk tampil dan
	 * cetak, serta menjadi masukan {@link #ambilDataParameterTambahan()}.</p>
	 *
	 * <p><b>Catatan penting:</b> karena label ikut disalin ke dalam blob, mengubah nama kelompok
	 * atau label parameter di layar master TIDAK memperbarui baris catatan yang sudah tersimpan —
	 * cetakan lama akan tetap memakai label versi lama. Gunakan
	 * {@link #getParameterTambahanInds()} bila yang dibutuhkan adalah pencocokan yang tahan
	 * perubahan label.</p>
	 *
	 * <p><b>Efek samping ringan:</b> nilai {@code null} diubah menjadi string kosong dan ditulis
	 * balik ke field.</p>
	 *
	 * @return blob berlabel; tidak pernah {@code null}, bisa berupa string kosong
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		return parameterTambahan;
	}

	/**
	 * Menetapkan blob nilai parameter versi berlabel manusia.
	 *
	 * <p>Dalam alur normal hanya dipanggil dari {@link #populateParameterTambahan(List)}; menyetel
	 * langsung berarti memikul sendiri tanggung jawab menjaga format tujuh bagian dan menjaga agar
	 * tetap sejalan dengan {@link #getParameterTambahanInds()}.</p>
	 *
	 * @param parameterTambahan blob berlabel
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

}
