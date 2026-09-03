package ais.database.model.sekolah;

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

import ais.database.model.GeneralValueObject;

/**
 * Entity MASTER <b>status awal siswa</b> — katalog "jalur/asal masuk" yang dipakai untuk menandai
 * bagaimana seorang siswa (atau calon siswa) pertama kali tercatat di sekolah. Satu baris tabel
 * {@code sekolah.status_awal_siswa} mewakili satu jalur masuk.
 *
 * <p><b>Isi domain (terverifikasi dari kode, bukan dugaan).</b> Nilai yang benar-benar dikenal
 * kode adalah tiga baris yang di-seed dan diikat ke konstanta global oleh
 * {@code ais.common.InitDataHelper}:</p>
 * <table border="1">
 *   <tr><th>Konstanta</th><th>Pola pencarian</th><th>Dibuat otomatis bila tidak ada?</th></tr>
 *   <tr><td>{@code ConstantValues.BARU_SISWA}</td>
 *       <td>{@code nama} ilike "Baru" ATAU "Peserta didik baru"</td>
 *       <td>Ya — {@code kode=""}, {@code nama="Peserta didik baru"}</td></tr>
 *   <tr><td>{@code ConstantValues.BARU_SISWA_BEASISWA}</td>
 *       <td>{@code nama} ilike "Baru-Beasiswa" ATAU "Beasiswa"</td>
 *       <td>Ya — {@code kode=""}, {@code nama="Beasiswa"}</td></tr>
 *   <tr><td>{@code ConstantValues.PINDAHAN_SISWA}</td>
 *       <td>{@code nama} ilike "Pindahan" (persis)</td>
 *       <td>Ya — {@code kode=""}, {@code nama="Pindahan"}</td></tr>
 * </table>
 * <p>Selain ketiganya, isi tabel bebas ditambah lewat layar master, dan pada instalasi baru
 * seluruh baris {@link ais.database.model.StatusAwalMahasiswa} ikut <b>disalin</b> ke sini oleh
 * {@code InitDataHelper} bila tabel siswa masih kosong ({@code kode} diisi
 * {@code statusAwal.getFeeder() + ""}). Karena itu instalasi hasil kloning bisa memuat baris
 * bernuansa perguruan tinggi seperti "Transfer", "PKU", atau "Alih Prodi" yang tidak relevan untuk
 * jenjang sekolah. Dugaan umum "Tinggal Kelas" <b>tidak</b> ditemukan di kode mana pun — status
 * mengulang/naik kelas ditangani mekanisme lain, bukan entity ini.</p>
 *
 * <h3>Hubungan dengan {@link ais.database.model.StatusAwalMahasiswa}</h3>
 *
 * <p>Kelas ini adalah <b>klon jenjang sekolah</b> dari entity versi perguruan tinggi:
 * {@code serialVersionUID} keduanya identik ({@code 2461821577548439808L}), susunan field dan
 * anotasinya sama persis, hanya saja versi sekolah tidak punya {@code feeder} maupun
 * {@code alihProdi}. Perbedaan perilaku yang penting dicatat: versi mahasiswa <i>tidak</i> menulis
 * balik apa pun di {@code getNama()}, sedangkan {@link #getNama()} di kelas ini menulis balik
 * (lihat "Hal non-obvious" di bawah).</p>
 *
 * <h3>Peran dalam model data</h3>
 *
 * <p>Entity ini adalah sisi "satu" dari empat relasi {@code @ManyToOne}, seluruhnya memetakan
 * kolom bernama {@code status_awal_siswa}:</p>
 * <ul>
 *   <li>{@link Siswa#getStatusAwalSiswa()} — status awal siswa aktif. Getter-nya
 *       ber-<i>fallback</i>: bila kolom kosong ia mengembalikan
 *       {@code ConstantValues.BARU_SISWA}.</li>
 *   <li>{@link CalonSiswa#getStatusAwalSiswa()} — status awal calon siswa PSB/PPDB. Getter-nya
 *       paling agresif: bila calon punya gelombang pendaftaran, nilai gelombang MENIMPA nilai
 *       baris; bila {@code getMerupakanPindahan()} bernilai benar, nilai dipaksa menjadi
 *       {@code ConstantValues.PINDAHAN_SISWA}.</li>
 *   <li>{@link GelombangPendaftaranPsb#getStatusAwalSiswa()} — status awal default satu gelombang
 *       pendaftaran, dengan fallback {@code BARU_SISWA}.</li>
 *   <li>{@link PengaturanBiaya#getStatusAwalSiswa()} — <b>dimensi penentu tarif</b>. Di sini
 *       fallback ke {@code BARU_SISWA} sengaja DIKOMENTARI, sehingga {@code null} tetap
 *       {@code null} dan bermakna "berlaku untuk semua status awal".</li>
 * </ul>
 *
 * <p><b>Konsekuensi keuangan.</b> Karena {@link PengaturanBiaya} memakai kolom ini sebagai salah
 * satu dimensi pemilihan tarif, isi master ini ikut menentukan <i>berapa</i> tagihan seorang siswa:
 * {@code PengaturanBiaya.buatCriteriaStatusAwalSiswa(...)} menyaring dengan
 * {@code (statusAwalSiswa IS NULL OR statusAwalSiswa = <status siswa>)}, dan
 * {@code Tagihan} membandingkan langsung {@link #getId()} pengaturan biaya dengan
 * {@link #getId()} status siswa. Jalur sejenis dipakai {@code TagihanUtil},
 * {@code TagihanUtilCalonSiswa}, {@code DetailTagihanSiswaHelper},
 * {@code DetailTagihanCalonSiswaHelper}, dan {@code AnalisisTagihanSekolahHelper}. Menghapus atau
 * menonaktifkan satu baris di sini dapat mengubah tarif yang terpilih untuk sekelompok siswa.</p>
 *
 * <p>Nilai {@link #getNama()} juga ikut diekspor keluar aplikasi: ke berkas Excel rekap ujian
 * ({@code PertemuanPunyaUjianSiswaHelper}) dan ke payload JSON host-to-host bank
 * ({@code DownloadTagihanSiswaBankOnline}, sebagai {@code attribute4}).</p>
 *
 * <h3>Layar dan penjagaan hak akses</h3>
 *
 * <p>Layar CRUD-nya {@code /pages/master/sekolah/status_awal_siswa.zul}, di-apply oleh
 * {@code ais.action.master.sekolah.StatusAwalSiswaAction}. Action tersebut memanggil
 * {@code Common.doCheckSecurity()} di {@code doBeforeCompose} dan — berbeda dengan banyak layar
 * master lain yang sudah ditinjau — <b>menjaga ketiga aksi mutasi dengan benar</b>: tombol Tambah
 * dibatasi {@code CREATE}, tombol Ubah dan KEDUA checkbox grid ("Aktif" dan "Pindahan") dibatasi
 * {@code UPDATE} lewat {@code setDisabled(!edit)}, tombol Hapus dibatasi {@code DELETE}. Tidak ada
 * kontrol grid yang lolos gerbang.</p>
 *
 * <p><b>Namun berlaku pola "pewarisan hak lewat menu induk".</b> Layar yang sama disisipkan sebagai
 * tab {@code MyInclude} bernama "Status Awal" di {@code KonfigurasiTampilanSiswaAction}. Karena
 * {@code CommonPrivilages.checkPrevilages(...)} menguji hak terhadap {@code Common.getCurrentMenu()}
 * — yaitu menu halaman yang sedang dibuka, bukan halaman yang di-{@code include} — maka saat dibuka
 * dari tab tersebut yang diperiksa adalah hak atas menu Konfigurasi Tampilan Siswa. Pemegang hak
 * ubah pada layar konfigurasi itu dengan sendirinya memperoleh CREATE/UPDATE/DELETE atas katalog
 * status awal (dan enam katalog master siswa lain yang menjadi tab bersamanya). Menu tersendiri
 * untuk layar ini tetap ada ({@code MenuInitializer} id {@code 765899}), jadi ini benar-benar dua
 * pintu masuk dengan dua matriks hak berbeda.</p>
 *
 * <p><b>Tidak ada kolom tenant.</b> Entity ini tidak punya FK ke sekolah maupun yayasan — katalog
 * bersifat GLOBAL per instalasi. Seluruh sekolah dalam satu instalasi berbagi baris yang sama,
 * sehingga penyuntingan oleh operator satu sekolah langsung terasa di sekolah lain. Ini bukan
 * "fail-open cakupan tenant" (tidak ada cakupan yang bocor), melainkan memang desain katalog
 * global — tetapi tetap perlu diingat saat menilai dampak perubahan.</p>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ul>
 *   <li><b>Identitas &amp; konstruktor</b> — {@link #getId()}, {@link #setId(Long)},
 *       {@link #StatusAwalSiswa()}, {@link #StatusAwalSiswa(Long)}.</li>
 *   <li><b>Atribut deskriptif</b> — {@link #getKode()}/{@link #setKode(String)},
 *       {@link #getNama()}/{@link #setNama(String)}.</li>
 *   <li><b>Penyaringan &amp; penanda perilaku</b> — {@link #getAktif()}/{@link #setAktif(Boolean)},
 *       {@link #getPindahan()}/{@link #setPindahan(Boolean)}.</li>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *       {@code onUpdate()}.</li>
 *   <li><b>Representasi</b> — {@link #toString()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious</h3>
 * <ul>
 *   <li><b>DUA getter menulis balik ke field — dan tulisan itu sampai ke basis data.</b>
 *       {@link #getNama()} mengganti nama "baru" menjadi "Peserta didik baru", dan
 *       {@link #getPindahan()} memaksa {@code pindahan = true} untuk baris yang namanya mengandung
 *       "pindahan". Karena pemetaan memakai akses properti (anotasi berada di getter) dan
 *       {@code dynamicUpdate = true}, hasil tulis-balik itu terbawa ke UPDATE pada flush pertama
 *       setelah entity dibaca — tanpa aksi pengguna, bahkan dari halaman laporan yang hanya
 *       menampilkan nama, dan tetap tercatat sebagai revisi Envers. Ini instance pola
 *       "getter write-back/destruktif" yang sudah dikenal proyek ini. Perhatikan pula bahwa
 *       {@code GeneralValueObject.compareTo(...)} memanggil {@code getNama()} sebagai kunci urut
 *       ketiga, sehingga <i>mengurutkan</i> sekumpulan objek entity ini pun sudah cukup untuk
 *       memicu penggantian nama.</li>
 *   <li><b>{@code aktif} tidak pernah ditulis saat baris dibuat.</b>
 *       {@code StatusAwalSiswaAction.onSave()} hanya menulis {@code kode}, {@code nama}, dan
 *       {@code keterangan} — tidak pernah {@code setAktif(...)} — sehingga baris baru masuk basis
 *       data dengan {@code aktif = NULL}. {@link #getAktif()} memperlakukan {@code NULL} sebagai
 *       {@code true}, jadi di grid master baris itu tampak <i>tercentang</i>, dan filter grid
 *       ({@code isNull("aktif") OR eq("aktif", true)}) juga menampilkannya. Tetapi empat pembaca
 *       menyaring KETAT dengan {@code Restrictions.eq("aktif", true)} — combo status awal di
 *       {@code SiswaAction} (dua tempat: filter pencarian dan formulir entri siswa),
 *       {@code PengaturanBiayaAction} (formulir tarif), dan {@code GelombangPendaftaranPsbAction} —
 *       dan SQL {@code = true} tidak pernah cocok dengan {@code NULL}. Akibatnya status awal yang
 *       baru dibuat admin <b>tidak pernah muncul</b> di formulir siswa, tarif, maupun gelombang
 *       PSB sampai seseorang men-toggle checkbox "Aktif" dua kali (mematikan lalu menyalakan, yang
 *       menulis nilai eksplisit). Tiga pembaca lain ({@code CalonSiswaAction}, filter pencarian
 *       {@code PengaturanBiayaAction}, dan grid master sendiri) memakai bentuk toleran-NULL,
 *       sehingga baris yang sama terlihat di sebagian layar dan hilang di sebagian lain. Ini
 *       instance berulang pola "kolom aktif tak pernah ditulis" yang sudah dikenal proyek ini.</li>
 *   <li><b>Kolom "Keterangan" pada layar tidak pernah tersimpan.</b> Grid ZUL punya kolom
 *       Keterangan, formulir tambah/ubah punya {@code Textbox} keterangan, dan {@code onSave()}
 *       memanggil {@code setKeterangan(...)}. Namun properti {@code keterangan} hanya ada di
 *       {@link ais.database.model.GeneralValueObject} dan TIDAK dideklarasikan ulang di kelas ini;
 *       karena induk itu bukan {@code @Entity}/{@code @MappedSuperclass}, Hibernate tidak
 *       memetakannya. Isian Keterangan hanya hidup di memori satu request dan kolom di grid selalu
 *       kosong ({@code GeneralValueObject.getKeterangan()} mengembalikan {@code ""}, bukan
 *       {@code null} — pola "getKeterangan() membalik kontrak"). Ketiadaan pemetaan ini bahkan
 *       pernah menimbulkan NPE reflektif dan sudah diberi catatan perbaikan eksplisit di
 *       {@code SiswaAction} dan {@code CalonSiswaAction}, yang kini melewatkan deskripsi
 *       {@code ""} saat mengisi combobox.</li>
 *   <li><b>Pola {@code getNomorUrut()} non-null TIDAK ada di sini.</b> Kelas ini tidak meng-override
 *       {@code getNomorUrut()}, sehingga cabang pertama {@code GeneralValueObject.compareTo(...)}
 *       tidak pernah aktif dan bug penciutan {@code TreeSet} yang ditemukan pada batch sebelumnya
 *       tidak berlaku. Pengurutan jatuh ke {@code nama}, dan tidak ada koleksi
 *       {@code SortedSet}/{@code TreeSet} berisi entity ini di seluruh source tree. Verifikasi
 *       negatif.</li>
 *   <li><b>Keunikan {@code nama} hanya divalidasi di aplikasi, dan validasinya bisa dilangkahi.</b>
 *       {@code StatusAwalSiswaAction.checkNamaStatusAwalSiswa()} menjalankan SELECT COUNT sebelum
 *       simpan; tidak ada unique constraint di basis data. Jalur tulis lain (auto-seed
 *       {@code InitDataHelper}, penyalinan dari {@code StatusAwalMahasiswa}) bisa menambah
 *       duplikat. Ada pula jalur halus: menyimpan baris bernama persis "Baru" lolos pemeriksaan,
 *       lalu {@link #getNama()} mengganti namanya sendiri menjadi "Peserta didik baru" pada
 *       pembacaan berikutnya — bila baris "Peserta didik baru" sudah ada, master berakhir dengan
 *       dua baris bernama sama tanpa pernah ditolak. Karena {@code ConstantValues.BARU_SISWA}
 *       dicari dengan {@code setMaxResults(1)}, mana dari keduanya yang menjadi konstanta global
 *       menjadi tidak deterministik.</li>
 *   <li><b>Pengikatan konstanta berbasis NAMA, bukan id.</b> Mengganti nama baris "Pindahan"
 *       menjadi, misalnya, "Pindahan Sekolah" membuat {@code ConstantValues.PINDAHAN_SISWA} gagal
 *       ditemukan pada start berikutnya dan {@code InitDataHelper} akan MEMBUAT baris "Pindahan"
 *       baru — master jadi punya dua baris bermakna sama, sementara
 *       {@link CalonSiswa#getStatusAwalSiswa()} memakai baris baru itu.</li>
 *   <li><b>Penamaan kolom tidak seragam.</b> Hanya {@code id}, {@code nama}, dan {@code kode} yang
 *       punya {@code @Column}. {@code aktif} dan {@code pindahan} mengikuti
 *       {@code MyNamingStrategy} (turunan {@code DefaultNamingStrategy} yang mengembalikan nama
 *       properti apa adanya), jadi nama kolomnya sama dengan nama propertinya.</li>
 *   <li><b>Field {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah} sengaja
 *       dideklarasikan ULANG</b> walaupun sudah ada di {@link ais.database.model.GeneralValueObject}.
 *       Ini BUKAN duplikasi keliru melainkan keharusan teknis: induknya POJO abstrak biasa (bukan
 *       {@code @Entity}/{@code @MappedSuperclass}), sehingga properti audit harus dinyatakan lagi
 *       di sini agar Hibernate dan Envers memetakannya.</li>
 *   <li><b>Komentar header kelas menyesatkan.</b> Baris "Bank generated by hbm2java" adalah sisa
 *       salin-tempel dari entity {@code Bank} — bukan penanda bahwa berkas ini dibangkitkan sebagai
 *       "Bank". Komentar dipertahankan apa adanya agar diff tetap minimal; jangan dijadikan acuan
 *       saat menelusuri asal berkas.</li>
 * </ul>
 *
 * <p>Anotasi kelas: {@code @Entity} + {@code @org.hibernate.annotations.Entity(dynamicInsert,
 * dynamicUpdate)} sehingga hanya kolom yang berubah yang dikirim ke basis data, dan
 * {@code @Audited} sehingga setiap perubahan lewat session Hibernate direkam Envers ke tabel
 * bayangan (perubahan lewat SQL native TIDAK terekam).</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.StatusAwalMahasiswa
 * @see Siswa
 * @see CalonSiswa
 * @see GelombangPendaftaranPsb
 * @see PengaturanBiaya
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "status_awal_siswa")

public class StatusAwalSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dikunci agar objek yang sudah ter-serialisasi (mis. di
	 * session ZK atau cache) tetap kompatibel walau kelas ini ditambahi anggota baru. Nilai ini
	 * identik dengan milik {@link ais.database.model.StatusAwalMahasiswa} — jejak bahwa kelas ini
	 * lahir sebagai salinan entity tersebut.
	 */
	private static final long serialVersionUID = 2461821577548439808L;

	/**
	 * Kunci primer baris, dipetakan ke kolom {@code id} dengan strategi {@code IDENTITY}
	 * (serial PostgreSQL). Dideklarasikan ulang dari
	 * {@link ais.database.model.GeneralValueObject} karena induknya tidak dipetakan Hibernate.
	 * Lihat {@link #getId()}.
	 */
	private Long id;

	/**
	 * Nama pengguna yang terakhir mengubah baris ini. Diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}. Dideklarasikan ulang dari
	 * {@link ais.database.model.GeneralValueObject} karena alasan pemetaan yang sama seperti
	 * {@link #id}. Lihat {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * Identitas (id pengguna) yang terakhir mengubah baris ini, pendamping {@link #oleh}. Lihat
	 * {@link #getOlehId()}.
	 */
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
	 * Menetapkan id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Perhatikan penjagaan anti-timpa:</b> nilai {@code null} maupun string kosong/hanya
	 * spasi DIABAIKAN diam-diam (method langsung {@code return} tanpa menulis apa pun), sehingga
	 * jejak audit yang sudah terisi tidak bisa dikosongkan lewat setter ini. Umumnya dipanggil dari
	 * {@code AuditTimestampInterceptor}, bukan dari kode layar.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong/hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau kosong/hanya spasi
	 * diabaikan diam-diam sehingga jejak audit lama tidak tertimpa nilai hampa.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong/hanya spasi
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
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum UPDATE baris ini dikirim
	 * ke basis data, lalu mendelegasikan pembaruan stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p><b>Efek samping:</b> mengubah {@code tanggal_dirubah} (dan, tergantung implementasi
	 * interceptor, {@link #oleh}/{@link #olehId}) pada objek yang sedang di-flush. Tidak pernah
	 * dipanggil manual dari kode aplikasi; tidak berjalan untuk INSERT maupun untuk perubahan lewat
	 * SQL native.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berbagi baris dengan method ini (gaya asli
	 * berkas, dipertahankan agar diff tetap minimal): stempel waktu perubahan terakhir,
	 * diinisialisasi ke waktu server saat objek dibuat lewat {@code WaktuUtil.getDate()} dan
	 * diperbarui oleh callback ini setiap kali baris di-UPDATE lewat session Hibernate. Lihat
	 * {@link #getTanggal_dirubah()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak perlu dipanggil manual: {@code onUpdate()} sudah mengurusnya, dan nilai yang
	 * disetel di sini akan tertimpa kembali oleh callback tersebut pada flush berikutnya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP} (tanggal + jam). Tidak pernah {@code null} untuk objek
	 * yang baru dibuat di JVM ini karena field-nya sudah diinisialisasi saat konstruksi; baris lama
	 * hasil migrasi bisa saja bernilai {@code null} di basis data.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris, berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca field {@link #nama} <b>langsung</b>, bukan lewat {@link #getNama()}, sehingga
	 * method ini TIDAK memicu tulis-balik "baru" &rarr; "Peserta didik baru" dan juga tidak
	 * memangkas spasi. Nilai ini dipakai sebagai label cadangan pada pengisian combobox
	 * ({@code CommonComboInsertHelper}) saat deskripsi yang diminta kosong.</p>
	 *
	 * @return gabungan id dan nama, mis. {@code "3-Pindahan"}; bagian yang belum terisi tampil
	 *         sebagai {@code "null"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode singkat status awal, dipetakan ke kolom {@code kode} (maksimal 50 karakter, boleh
	 * {@code NULL}). Tidak dipakai untuk keputusan bisnis apa pun; pada baris hasil auto-seed berisi
	 * string kosong, dan pada baris hasil penyalinan dari {@link ais.database.model.StatusAwalMahasiswa}
	 * berisi kode Feeder/PDDikti sumbernya. Lihat {@link #getKode()}.
	 */
	private String kode;

	/**
	 * Nama status awal — kunci alami entity ini. Dipakai untuk pengikatan konstanta global di
	 * {@code InitDataHelper}, untuk deduksi {@link #getPindahan()}, sebagai label di seluruh
	 * combobox/laporan, dan sebagai kunci urut di
	 * {@code GeneralValueObject.compareTo(GeneralValueObject)}. Lihat {@link #getNama()} —
	 * getter-nya menulis balik ke field ini.
	 */
	private String nama;

	/**
	 * Penanda baris masih dipakai. Kolom {@code aktif} (tanpa {@code @Column}, nama kolom sama
	 * dengan nama properti). Bernilai {@code null} untuk seluruh baris yang dibuat lewat layar
	 * master karena {@code onSave()} tidak pernah mengisinya — lihat pembahasan lengkap pada
	 * Javadoc kelas dan {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Penanda bahwa status awal ini bermakna "siswa pindahan". Kolom {@code pindahan} (tanpa
	 * {@code @Column}). Nilainya dapat dipaksa menjadi {@code true} oleh {@link #getPindahan()}
	 * berdasarkan isi {@link #nama}.
	 */
	private Boolean pindahan;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA dan dipakai layar master saat menekan tombol
	 * "Tambah" ({@code StatusAwalSiswaAction.onAdd(...)}). Seluruh field dibiarkan {@code null}
	 * kecuali {@code tanggal_dirubah} yang langsung terisi waktu server.
	 */
	public StatusAwalSiswa() {
	}

	/**
	 * Konstruktor pintas yang hanya mengisi kunci primer, berguna untuk membuat referensi ringan ke
	 * sebuah baris tanpa memuatnya dari basis data.
	 *
	 * <p><b>Perhatian:</b> objek hasil konstruktor ini bukan entity terkelola dan seluruh atribut
	 * lain bernilai {@code null}; jangan disimpan lewat {@code saveOrUpdate} karena akan menimpa
	 * baris asli dengan nilai kosong. Tidak ada pemanggil aktif di source tree saat ini.</p>
	 *
	 * @param id kunci primer baris yang diacu
	 */
	public StatusAwalSiswa(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kunci primer baris.
	 *
	 * <p>Dipetakan sebagai {@code @Id} dengan {@code GenerationType.IDENTITY} dan
	 * {@code insertable = false}, artinya nilainya diserahkan sepenuhnya ke serial basis data dan
	 * tidak pernah ikut dikirim pada INSERT. Nilai inilah yang dibandingkan oleh mesin tagihan
	 * ({@code Tagihan}, {@code JenisDiskon}-sejenis) saat mencocokkan status awal siswa dengan
	 * status awal pada pengaturan biaya.</p>
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
	 * Menetapkan kunci primer baris.
	 *
	 * <p>Hampir tidak pernah dipanggil kode aplikasi — pengisiannya diserahkan ke Hibernate.
	 * Menyetel id pada objek yang sudah terkelola akan membingungkan session dan sebaiknya
	 * dihindari.</p>
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama status awal, dengan DUA olahan sekaligus.
	 *
	 * <p><b>1. Penggantian istilah yang menulis balik ke field (destruktif).</b> Bila nilai
	 * tersimpan persis sama dengan "baru" (tanpa peduli besar-kecil huruf), field {@link #nama}
	 * <b>ditimpa</b> menjadi {@code "Peserta didik baru"} — istilah Dapodik untuk jenjang sekolah.
	 * Ini bukan sekadar penyesuaian tampilan: karena pemetaan Hibernate memakai akses properti
	 * (anotasi {@code @Column} berada di getter ini) dan entity beranotasi {@code dynamicUpdate},
	 * nilai baru itu terbaca sebagai perubahan pada dirty-check dan dikirim sebagai UPDATE pada
	 * flush pertama setelah entity dibaca — <i>tanpa</i> aksi pengguna, termasuk dari halaman
	 * laporan atau ekspor yang hanya menampilkan nama, dan tetap dicatat sebagai revisi Envers.
	 * Efek lanjutannya: baris "Baru" hasil penyalinan dari
	 * {@link ais.database.model.StatusAwalMahasiswa} berganti nama sendiri di kemudian hari, dan
	 * bila baris "Peserta didik baru" sudah ada, master berakhir dengan dua baris bernama sama
	 * meski validasi keunikan di layar master tidak pernah dilanggar secara langsung.
	 * {@code InitDataHelper} sudah mengantisipasi hal ini dengan mencari
	 * {@code ConstantValues.BARU_SISWA} memakai "Baru" ATAU "Peserta didik baru".</p>
	 *
	 * <p><b>2. Pemangkasan spasi pada nilai kembali.</b> Hasil akhirnya di-{@code trim()}, tetapi
	 * field-nya tidak — sehingga baris yang tersimpan dengan spasi berlebih juga akan ter-UPDATE
	 * menjadi versi terpangkas pada flush berikutnya, lewat mekanisme dirty-check yang sama.</p>
	 *
	 * <p>Dipanggil dari mana saja: renderer grid master, seluruh combobox status awal, ekspor Excel
	 * rekap ujian, payload JSON host-to-host bank, dan — yang mudah terlewat —
	 * {@code GeneralValueObject.compareTo(GeneralValueObject)} sebagai kunci urut ketiga, sehingga
	 * sekadar mengurutkan daftar entity ini sudah memicu seluruh efek di atas.</p>
	 *
	 * @return nama status awal yang sudah dinormalkan dan dipangkas, atau {@code null} bila belum
	 *         diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (nama != null && nama.equalsIgnoreCase("baru")) {
			nama = "Peserta didik baru";
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama status awal apa adanya, tanpa validasi maupun pemangkasan (layar master sudah
	 * memangkasnya sendiri sebelum memanggil method ini).
	 *
	 * <p><b>Berdampak luas.</b> Nama adalah kunci alami entity ini: mengubahnya dapat memutus
	 * pengikatan {@code ConstantValues.BARU_SISWA}/{@code BARU_SISWA_BEASISWA}/{@code PINDAHAN_SISWA}
	 * pada start berikutnya (sehingga {@code InitDataHelper} membuat baris duplikat), mengubah
	 * label pada laporan dan ekspor bank, dan — bila nama barunya mengandung "pindahan" — memaksa
	 * {@link #getPindahan()} mengembalikan {@code true} secara permanen.</p>
	 *
	 * <p>Dipanggil dari {@code StatusAwalSiswaAction.onSave(...)} (setelah validasi wajib-isi dan
	 * pemeriksaan keunikan) dan dari jalur seed {@code InitDataHelper}.</p>
	 *
	 * @param nama nama status awal yang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menetapkan kode singkat status awal.
	 *
	 * <p>Dipanggil dari {@code StatusAwalSiswaAction.onSave(...)} — yang mewajibkan isian tidak
	 * kosong di layar, walau basis data sendiri membolehkan {@code NULL} — serta dari jalur seed
	 * {@code InitDataHelper} (diisi {@code ""} untuk baris tetap, atau kode Feeder sumber untuk
	 * baris hasil penyalinan dari {@link ais.database.model.StatusAwalMahasiswa}).</p>
	 *
	 * @param kode kode baru; tidak divalidasi di sini
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kode singkat status awal apa adanya (tanpa normalisasi maupun pemangkasan).
	 *
	 * <p>Hanya dipakai untuk tampilan: kolom pertama grid master, dan sebagai deskripsi item pada
	 * combobox status awal di {@code SiswaAction} dan {@code GelombangPendaftaranPsbAction}. Tidak
	 * ada logika bisnis yang bercabang berdasarkan nilai ini.</p>
	 *
	 * @return kode status awal, atau {@code null} bila belum diisi
	 */
	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode;
	}

	/**
	 * Mengembalikan penanda aktif, dengan {@code null} diperlakukan sebagai {@code true}.
	 *
	 * <p>Berbeda dengan {@link #getNama()} dan {@link #getPindahan()}, method ini <b>tidak</b>
	 * menulis balik ke field — nilai {@code NULL} di basis data tetap {@code NULL}.</p>
	 *
	 * <p><b>Inilah sumber ketidakcocokan yang penting.</b> Layar master memakai getter ini untuk
	 * mencentang checkbox "Aktif", dan filter grid-nya juga toleran-NULL
	 * ({@code isNull("aktif") OR eq("aktif", true)}), sehingga baris ber-{@code aktif} {@code NULL}
	 * terlihat aktif. Sebaliknya empat pembaca lain menyaring ketat dengan
	 * {@code Restrictions.eq("aktif", true)} — combo pencarian dan combo formulir di
	 * {@code SiswaAction}, combo formulir tarif di {@code PengaturanBiayaAction}, dan combo di
	 * {@code GelombangPendaftaranPsbAction} — dan SQL {@code = true} tidak pernah cocok dengan
	 * {@code NULL}. Karena {@code StatusAwalSiswaAction.onSave(...)} tidak pernah memanggil
	 * {@link #setAktif(Boolean)}, setiap baris yang baru dibuat admin bernilai {@code NULL} dan
	 * karenanya tidak pernah muncul di empat layar tersebut sampai checkbox "Aktif" di-toggle dua
	 * kali.</p>
	 *
	 * @return {@code true} bila baris dianggap aktif (termasuk saat kolomnya masih {@code NULL}),
	 *         {@code false} hanya bila pernah dinonaktifkan secara eksplisit; tidak pernah
	 *         {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan penanda aktif.
	 *
	 * <p>Satu-satunya pemanggil adalah listener {@code onCheck} checkbox "Aktif" di grid master,
	 * yang langsung menyusulinya dengan {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan
	 * tersimpan seketika tanpa dialog konfirmasi. Checkbox tersebut dinonaktifkan bila pengguna
	 * tidak punya hak {@code CommonPrivilages.UPDATE}. Formulir tambah/ubah TIDAK memanggil method
	 * ini — lihat catatan pada {@link #getAktif()}.</p>
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk menonaktifkan
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penanda "siswa pindahan", dengan deduksi dari nama yang <b>menulis balik ke
	 * field</b>.
	 *
	 * <p>Bila {@link #getNama()} mengandung potongan kata "pindahan" (tanpa peduli besar-kecil
	 * huruf), field {@link #pindahan} ditimpa menjadi {@code true}. Sama seperti
	 * {@link #getNama()}, tulis-balik ini ikut ter-UPDATE ke basis data pada flush pertama setelah
	 * entity dibaca karena pemetaan memakai akses properti dan {@code dynamicUpdate = true}.
	 * Konsekuensi praktisnya: untuk baris yang namanya mengandung "pindahan", checkbox "Pindahan"
	 * di grid master <b>tidak dapat dimatikan secara permanen</b> — pengguna boleh membukanya dan
	 * nilainya tersimpan {@code false}, tetapi render berikutnya memanggil getter ini, memaksanya
	 * kembali {@code true}, dan menuliskannya lagi ke basis data. Perhatikan juga bahwa method ini
	 * memanggil {@link #getNama()} dua kali, sehingga ikut memicu seluruh efek samping getter
	 * tersebut.</p>
	 *
	 * <p><b>Status pemakaian: yatim fungsional di sisi sekolah.</b> Satu-satunya pembaca
	 * {@code getPindahan()} pada tipe ini adalah layar master sendiri (untuk mencentang checkbox).
	 * Tidak ada satu pun keputusan bisnis jenjang sekolah — tagihan, PSB, rapor — yang bercabang
	 * berdasarkan flag ini; penentuan "calon siswa pindahan" ditangani lewat
	 * {@code CalonSiswa.getMerupakanPindahan()} yang memaksa relasi ke
	 * {@code ConstantValues.PINDAHAN_SISWA}, bukan lewat kolom ini. Bandingkan dengan versi
	 * perguruan tinggi ({@link ais.database.model.StatusAwalMahasiswa#getPindahan()}) yang benar-benar
	 * dibaca oleh {@code MahasiswaAction}, {@code NewDetailBiayaExcelAction}, dan
	 * {@code BiodataCalonMahasiswa} — porting ke jenjang sekolah berhenti di layar master.</p>
	 *
	 * @return {@code true} bila baris menandakan siswa pindahan (eksplisit maupun hasil deduksi
	 *         nama), {@code false} bila tidak; tidak pernah {@code null}
	 */
	public Boolean getPindahan() {
		if (getNama() != null && getNama().trim().toLowerCase().contains("pindahan")) {
			pindahan = true;
		}
		return pindahan == null ? false : pindahan;
	}

	/**
	 * Menetapkan penanda "siswa pindahan".
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox "Pindahan" di grid master, diikuti
	 * {@code Common.refreshSaveOrUpdate(...)} sehingga tersimpan seketika. Checkbox tersebut
	 * dinonaktifkan bila pengguna tidak punya hak {@code CommonPrivilages.UPDATE}.</p>
	 *
	 * <p><b>Ingat batasan pada {@link #getPindahan()}:</b> untuk baris yang namanya mengandung
	 * "pindahan", menyetel {@code false} di sini tidak akan bertahan — getter akan memaksanya
	 * kembali {@code true} pada pembacaan berikutnya sekaligus menuliskannya ke basis data.</p>
	 *
	 * @param pindahan {@code true} bila status awal ini bermakna pindahan
	 */
	public void setPindahan(Boolean pindahan) {
		this.pindahan = pindahan;
	}

}
