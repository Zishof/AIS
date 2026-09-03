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
 * Entity MASTER <b>jenis tempat tinggal siswa</b> — katalog data pribadi yang menjawab pertanyaan
 * "siswa ini tinggal di mana / bersama siapa". Satu baris mewakili satu kategori tempat tinggal.
 * Bersama {@link AlatTransportasiSiswa}, {@link PenghasilanOrangTuaSiswa},
 * {@link PekerjaanOrtuSiswa}, {@link PendidikanOrangTuaSiswa}, {@link StatusAwalSiswa}, dan
 * {@link StatusKeluarSiswa}, entity ini membentuk kelompok "katalog data pribadi siswa" yang
 * mengisi combobox pada formulir biodata siswa.
 *
 * <h3>Isi domain (TERVERIFIKASI dari kode, bukan dugaan)</h3>
 *
 * <p>Tabel ini <b>tidak</b> di-seed dengan daftar literalnya sendiri. Pada instalasi baru
 * {@code ais.common.InitDataHelper} menyalin seluruh baris
 * {@link ais.database.model.JenisTinggalMahasiswa} ke sini, tetapi HANYA bila tabel ini masih
 * benar-benar kosong ({@code COUNT(*) == 0}):</p>
 *
 * <pre>
 * jenisTinggalSiswa.setNama(jenisTinggalMahasiswa.getNama());
 * jenisTinggalSiswa.setKode(jenisTinggalMahasiswa.getFeeder() + "");
 * </pre>
 *
 * <p>Daftar sumbernya adalah seed PDDikti Feeder di {@code InitDataHelper} yang di-hardcode
 * sebagai {@code "1;Bersama orang tua|2;Wali|3;Kost|4;Asrama|5;Panti asuhan|99;Lainnya|"}. Jadi
 * isi baku katalog ini setelah instalasi adalah enam baris berikut:</p>
 *
 * <table border="1">
 *   <tr><th>{@code kode}</th><th>{@code nama}</th></tr>
 *   <tr><td>{@code "1"}</td><td>Bersama orang tua</td></tr>
 *   <tr><td>{@code "2"}</td><td>Wali</td></tr>
 *   <tr><td>{@code "3"}</td><td>Kost</td></tr>
 *   <tr><td>{@code "4"}</td><td>Asrama</td></tr>
 *   <tr><td>{@code "5"}</td><td>Panti asuhan</td></tr>
 *   <tr><td>{@code "99"}</td><td>Lainnya</td></tr>
 * </table>
 *
 * <p>Perhatikan bahwa {@link #getKode()} di sini menyimpan <b>kode Feeder PDDikti sebagai
 * String</b>, bukan kode buatan sekolah — tetapi kelas ini sendiri TIDAK punya properti
 * {@code feeder} bertipe {@code Long} seperti versi mahasiswanya, sehingga hubungan ke
 * nomenklatur Feeder hanya bertahan sebagai teks dan tidak pernah dibaca ulang oleh eksportir
 * Feeder mana pun. Bila ada baris {@code JenisTinggalMahasiswa} yang dibuat manual lewat layar
 * master PT (tanpa {@code feeder}), penyalinan di atas menghasilkan {@code kode} berisi string
 * literal {@code "null"} — bukan {@code NULL} basis data.</p>
 *
 * <h3>Klon jenjang sekolah dari {@link ais.database.model.JenisTinggalMahasiswa}</h3>
 *
 * <p>Kelas ini adalah salinan jenjang sekolah dari entity versi perguruan tinggi:
 * {@code serialVersionUID} keduanya <b>identik</b> ({@code 2463821577548439808L}) dan susunan
 * anggotanya sama persis, kecuali versi sekolah membuang {@code feeder}. Nama tabel fisiknya pun
 * ikut tersalin apa adanya: <b>{@code @Table(schema = "sekolah", name = "jenis_tinggal_mahasiswa")}</b>
 * — sebuah tabel bernama "mahasiswa" di dalam skema {@code sekolah}. Ini nyaris pasti sisa
 * salin-tempel, tetapi <b>tidak berbahaya</b>: karena skemanya berbeda
 * ({@code sekolah.jenis_tinggal_mahasiswa} vs {@code public.jenis_tinggal_mahasiswa}), kedua
 * {@code @Entity} memetakan dua tabel FISIK yang berbeda. Ini <i>bukan</i> kasus "dua entity satu
 * tabel" seperti pasangan {@code KompetensiDasarMatapelajaran}/{@code JenisJadwalPelajaran}.
 * Konsekuensi praktisnya hanya membingungkan saat menulis SQL manual atau membaca dump basis
 * data.</p>
 *
 * <h3>Peran dalam model data — dan empat representasi paralel yang tidak nyambung</h3>
 *
 * <p>Entity ini hanya menjadi sisi "satu" dari <b>satu</b> relasi:
 * {@link Siswa#getJenisTinggal()} ({@code @ManyToOne}, kolom {@code sekolah.siswa.jenis_tinggal},
 * nullable). Tidak ada koleksi balik. Yang penting untuk diketahui, konsep "tempat tinggal"
 * ternyata diwakili <b>empat cara berbeda</b> di aplikasi yang sama, dan tidak satu pun saling
 * memetakan:</p>
 * <ol>
 *   <li><b>Siswa aktif</b> — FK ke kelas ini ({@code Siswa.jenisTinggal}). Satu-satunya penulis di
 *       seluruh source tree adalah {@code SiswaAction.onSave()}.</li>
 *   <li><b>Calon siswa, layar petugas</b> — {@link CalonSiswa#getJenisTinggalMahasiswa()}, FK ke
 *       {@link ais.database.model.JenisTinggalMahasiswa} di skema {@code public}, bukan ke kelas
 *       ini. Diisi {@code CalonSiswaAction}.</li>
 *   <li><b>Calon siswa, formulir PPDB publik</b> — {@code CalonSiswa.setJenisTinggal(String)},
 *       kolom teks bebas. {@code PPDB1}/{@code PPDB2} mengisi combonya dari konfigurasi
 *       {@code jenis_tinggal_calon_siswa} yang defaultnya
 *       {@code "Bersama orang tua;Bersama wali;Pondok pesantren;Panti asuhan"} — kosakata yang
 *       BERBEDA dari katalog ini ("Bersama wali" vs "Wali", "Pondok pesantren" yang tidak ada di
 *       sini, sementara "Kost"/"Asrama"/"Lainnya" tidak ada di sana).</li>
 *   <li><b>Mahasiswa</b> — {@code BiodataMahasiswa.jenisTinggalMahasiswa}, satu-satunya yang
 *       benar-benar diekspor ke Feeder PDDikti ({@code id_jns_tinggal}).</li>
 * </ol>
 *
 * <p><b>Akibatnya jawaban PPDB tidak pernah terbawa.</b> Karena satu-satunya penulis
 * {@code Siswa.jenisTinggal} adalah formulir entri siswa, apa pun yang diisi calon siswa saat
 * mendaftar (baik teks bebas PPDB maupun FK versi mahasiswa) tidak pernah dipindahkan ke siswa
 * saat calon diterima. Operator harus memilih ulang secara manual, dan bila tidak, kolom
 * {@code jenis_tinggal} tetap {@code NULL} selamanya.</p>
 *
 * <p><b>Tidak ada pembaca hilir.</b> Berbeda dengan {@link StatusAwalSiswa} (dimensi tarif
 * {@code PengaturanBiaya}) atau {@link ais.database.model.JenisTinggalMahasiswa} (diekspor ke
 * Feeder dan dipakai {@code LaporanRekapJumlahMahasiswaPerTempatTinggalSaatKuliah} serta
 * {@code DashboardRekapMahasiswa}), sisi SISWA tidak punya satu pun laporan, dasbor, ekspor
 * Feeder, atau API yang membaca {@code Siswa.getJenisTinggal()}. Nilainya murni dokumentasi
 * biodata: diisi, disimpan, ditampilkan kembali di formulir yang sama, dan tidak memengaruhi
 * keputusan bisnis apa pun. Ini <i>bukan</i> relasi yatim (kolomnya benar-benar ditulis dan
 * dibaca ulang oleh formulir), hanya relasi yang tidak punya konsumen hilir.</p>
 *
 * <h3>Layar, penjagaan hak akses, dan pewarisan hak TIGA TINGKAT</h3>
 *
 * <p>Layar CRUD-nya {@code /pages/master/sekolah/jenis_tinggal_siswa.zul}, di-apply oleh
 * {@code ais.action.master.sekolah.JenisTinggalSiswaAction}. Action-nya memanggil
 * {@code Common.doCheckSecurity()} di {@code doBeforeCompose} dan menjaga kontrolnya dengan benar
 * di tingkat layar: tombol Tambah dibatasi {@code CREATE}, checkbox "Aktif" di grid
 * di-{@code setDisabled(!edit)} dengan {@code edit = checkPrevilages(UPDATE)}, tombol Ubah/Hapus
 * lewat {@code Common.copyEditDeleteButtons(edit, delete, ...)}, dan tombol "Upload Data" (impor
 * Excel massal) bahkan menuntut ketiganya sekaligus. <b>Tidak ada kontrol grid yang lolos
 * gerbang</b> — berbeda dengan {@code Intbox} nomor urut tanpa gerbang yang ditemukan pada batch
 * sebelumnya (entity ini memang tidak punya kolom nomor urut).</p>
 *
 * <p><b>Tetapi hak yang diuji sama sekali bukan hak atas layar ini.</b> Penelusuran menyeluruh
 * source tree menunjukkan {@code jenis_tinggal_siswa.zul} <b>TIDAK punya entri menu sendiri</b>:
 * tidak ada di {@code MenuInitializer}, tidak ada di {@code MenuSnapshotData}, dan tidak
 * dirujuk {@code .zul} lain selain satu tempat. Satu-satunya jalan masuk adalah rantai
 * {@code MyInclude} bertingkat tiga:</p>
 * <ol>
 *   <li>Menu <b>"Siswa"</b> ({@code MenuInitializer} id {@code 887727},
 *       {@code /pages/master/sekolah/siswa.zul}) — menu operasional harian tata usaha.</li>
 *   <li>Tab <b>"Form Siswa"</b> di {@code siswa.zul} (tanpa atribut {@code visible} apa pun, jadi
 *       selalu tampil) &rarr; {@code SiswaAction.onFormTambahan()} meng-{@code include}
 *       {@code /pages/master/konfigurasi_siswa.zul} — yang juga tidak punya entri menu sendiri.</li>
 *   <li>{@code KonfigurasiTampilanSiswaAction} membangun tujuh tab lazy, tab pertama
 *       <b>"Jenis Tinggal"</b> meng-{@code include} {@code jenis_tinggal_siswa.zul}.</li>
 * </ol>
 *
 * <p>Karena {@code CommonPrivilages.checkPrevilages(...)} menguji hak terhadap
 * {@code Common.getCurrentMenu()} — menu halaman yang dibuka pengguna, bukan halaman yang
 * di-{@code include} — maka yang dievaluasi sepanjang rantai itu adalah hak atas menu <b>Siswa</b>.
 * Siapa pun yang memegang CREATE/UPDATE/DELETE untuk pendataan siswa (peran tata usaha yang sangat
 * lazim) dengan sendirinya memperoleh CRUD penuh <i>plus impor Excel massal</i> atas katalog
 * GLOBAL ini dan enam katalog saudaranya. Ini instance pola "pewarisan hak lewat menu induk" yang
 * sudah dikenal proyek ini, dengan <b>varian baru: rantai TIGA TINGKAT dan layar yang benar-benar
 * tanpa menu sendiri</b> — lebih murni daripada {@link StatusAwalSiswa} yang setidaknya masih punya
 * menu sendiri sebagai pintu kedua. Sisi baiknya, mekanismenya tetap <i>fail-closed</i>: bila
 * {@code getCurrentMenu()} bernilai {@code null} atau pengguna hanya punya READ, seluruh tombol
 * mutasi memang tersembunyi/nonaktif.</p>
 *
 * <p><b>Tidak ada kolom tenant.</b> Entity ini tidak punya FK ke sekolah maupun yayasan — katalog
 * bersifat GLOBAL per instalasi, dan {@code JenisTinggalSiswaAction.initCriteria()} memang tidak
 * menambahkan penyaring cakupan apa pun. Seluruh sekolah dalam satu instalasi berbagi baris yang
 * sama, sehingga penyuntingan oleh operator satu sekolah langsung terasa di sekolah lain. Ini
 * BUKAN "fail-open cakupan tenant" (tidak ada cakupan yang bocor karena memang tidak pernah ada),
 * melainkan desain katalog global — tetapi tetap perlu diingat saat menilai dampak perubahan,
 * terutama digabung dengan pewarisan hak di atas.</p>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ul>
 *   <li><b>Identitas &amp; konstruktor</b> — {@link #JenisTinggalSiswa()}, {@link #getId()},
 *       {@link #setId(Long)}.</li>
 *   <li><b>Atribut deskriptif</b> — {@link #getKode()}/{@link #setKode(String)},
 *       {@link #getNama()}/{@link #setNama(String)},
 *       {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 *   <li><b>Penyaringan</b> — {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *       {@code onUpdate()}.</li>
 *   <li><b>Representasi</b> — {@link #toString()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious</h3>
 * <ul>
 *   <li><b>Pola "kolom aktif tak pernah ditulis" — TERKONFIRMASI di sini.</b>
 *       {@code JenisTinggalSiswaAction.onSave()} hanya menulis {@code kode}, {@code nama}, dan
 *       {@code keterangan}; ia tidak pernah memanggil {@code setAktif(...)}. Auto-seed
 *       {@code InitDataHelper} juga tidak. Jadi setiap baris baru masuk basis data dengan
 *       {@code aktif = NULL}. Di layar master hal itu tak terlihat: {@link #getAktif()}
 *       memperlakukan {@code NULL} sebagai {@code true} sehingga checkbox tampak tercentang, dan
 *       filter grid memakai bentuk toleran-NULL
 *       ({@code isNull("aktif") OR eq("aktif", true)}). Namun satu-satunya pembaca hilir yang
 *       benar-benar penting, yaitu combo "Jenis Tinggal" pada formulir biodata siswa
 *       ({@code SiswaAction}, lewat
 *       {@code Common.insertComboDanSemua(..., Restrictions.eq("aktif", true))}), menyaring
 *       <b>KETAT</b> — dan SQL {@code aktif = true} tidak pernah cocok dengan {@code NULL}.
 *       Akibatnya jenis tempat tinggal yang baru dibuat admin, <i>termasuk keenam baris hasil
 *       auto-seed pada instalasi baru</i>, <b>tidak pernah muncul</b> di formulir siswa sampai
 *       seseorang men-toggle checkbox "Aktif" dua kali (mematikan lalu menyalakan, yang barulah
 *       menulis nilai eksplisit lewat {@code Common.refreshSaveOrUpdate}). Pada instalasi yang
 *       belum pernah disentuh, combo tersebut praktis kosong sama sekali. Ini instance berulang
 *       pola yang sama dengan {@code JenisLaporanJadwalSekolah}, {@code JenisMateriHarianDefault},
 *       {@code JenisSKGuru}, {@code JenisNilaiHuruf}, {@code KelompokGelombang}, dan
 *       {@link StatusAwalSiswa}.</li>
 *   <li><b>{@link #getKeterangan()} membalik kontrak kelas induk.</b>
 *       {@code GeneralValueObject.getKeterangan()} menjamin TIDAK PERNAH mengembalikan
 *       {@code null} (mengembalikan {@code ""} bila kosong); override di kelas ini mengembalikan
 *       field mentah, jadi bisa {@code null}. Ini varian yang BERBEDA dari pola batch sebelumnya:
 *       di sini properti {@code keterangan} justru <b>dideklarasikan ulang dan DIPETAKAN</b>
 *       ({@code @Column(name = "keterangan", nullable = true)}), sehingga isian Keterangan
 *       benar-benar tersimpan ke basis data — tidak hilang seperti pada {@link StatusAwalSiswa}.
 *       Yang perlu diwaspadai hanya asumsi non-null: pemanggil yang menganggap kontrak induk masih
 *       berlaku bisa NPE. Efek samping lain yang halus: cabang keempat
 *       {@code GeneralValueObject.compareTo(...)} yang di induk SELALU memenuhi syarat, di sini
 *       bisa gagal — namun tidak berdampak karena cabang {@code nama} sudah lebih dulu menang.</li>
 *   <li><b>Pola {@code getNomorUrut()} non-null TIDAK ada di sini — verifikasi negatif.</b> Kelas
 *       ini tidak meng-override {@code getNomorUrut()} maupun {@code getNim()}, sehingga dua cabang
 *       pertama {@code GeneralValueObject.compareTo(...)} tidak pernah aktif dan pengurutan jatuh
 *       ke {@link #getNama()} yang normalnya unik. Bug penciutan {@code TreeSet} yang ditemukan
 *       pada batch sebelumnya tidak berlaku; tidak ada pula koleksi {@code SortedSet}/{@code TreeSet}
 *       berisi entity ini di source tree (entity ini memang tidak punya koleksi balik).</li>
 *   <li><b>Tidak ada getter write-back/destruktif — verifikasi negatif.</b> {@link #getNama()}
 *       memang men-{@code trim()}, tetapi hanya pada nilai yang DIKEMBALIKAN; ia tidak menugaskan
 *       ulang ke {@code this.nama}. Karena pemetaan memakai akses properti, Hibernate tetap
 *       menyimpan nilai ter-{@code trim} saat flush, namun tidak ada mutasi diam-diam bergaya
 *       {@code StatusAwalSiswa.getNama()} yang mengganti isi baris. Efek samping yang tersisa
 *       hanya kosmetik: {@link #toString()} membaca field mentah sehingga bisa menampilkan spasi
 *       tepi yang tidak terlihat di layar.</li>
 *   <li><b>{@link #toString()} menyimpang dari format induk.</b> Induk memakai
 *       {@code "kode - nama"}; override di sini memakai {@code id + "-" + nama} tanpa spasi.
 *       Karena seluruh titik tampil (grid, combo, ekspor Excel) memakai properti eksplisit, format
 *       ini hanya terlihat di log dan pesan debug.</li>
 *   <li><b>Kolom "feeder" hantu pada ekspor/impor.</b>
 *       {@code JenisTinggalSiswaAction.doAfterCompose()} mendaftarkan kolom
 *       {@code {"id","kode","nama","keterangan","aktif","feeder"}} untuk tombol Download dan Upload
 *       — padahal kelas ini tidak punya properti {@code feeder} (hanya versi mahasiswanya yang
 *       punya). {@code CommonDownloadUpload.readByGetterSafely()} menelan
 *       {@code NoSuchMethodException} dan mengembalikan {@code null}, jadi tidak ada crash: berkas
 *       Excel sekadar memuat satu kolom yang selalu kosong. Sisa salin-tempel dari klon
 *       mahasiswa.</li>
 *   <li><b>Keunikan {@code nama} hanya divalidasi di aplikasi.</b>
 *       {@code JenisTinggalSiswaAction.checkNamaJenisTinggalSiswa()} menjalankan SELECT COUNT
 *       sebelum simpan (variabel lokalnya masih bernama {@code kotaCount} — sisa salin-tempel dari
 *       master Kota); tidak ada unique constraint di basis data, dan {@code kode} tidak divalidasi
 *       sama sekali. Jalur tulis lain (auto-seed dan impor Excel massal) dapat menambah duplikat
 *       tanpa pemeriksaan.</li>
 *   <li><b>Penamaan kolom tidak seragam.</b> Hanya {@code id}, {@code nama}, dan {@code keterangan}
 *       yang punya {@code @Column} eksplisit. {@code kode}, {@code aktif}, {@code oleh}, dan
 *       {@code olehId} mengikuti naming strategy default proyek (mengembalikan nama properti apa
 *       adanya), sehingga nama kolomnya persis sama dengan nama propertinya — termasuk
 *       {@code olehId} yang tetap camelCase.</li>
 *   <li><b>Field {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, {@code kode},
 *       {@code nama}, {@code keterangan} sengaja dideklarasikan ULANG</b> walaupun namanya sama
 *       dengan field di {@link ais.database.model.GeneralValueObject}. Ini BUKAN duplikasi keliru
 *       melainkan KEHARUSAN TEKNIS: induknya adalah POJO abstrak biasa — bukan {@code @Entity}
 *       maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti induk sama
 *       sekali. Setiap subclass yang butuh kolom-kolom itu wajib menyatakannya kembali beserta
 *       anotasinya. Konsekuensi lanjutannya: field induk yang senama menjadi ter-<i>shadow</i> dan
 *       selamanya {@code null}, sehingga method induk yang membaca field secara langsung (bukan
 *       lewat getter virtual) tidak akan pernah melihat nilai yang tersimpan di sini.</li>
 *   <li><b>Judul jendela pop-up di ZUL salah tempel</b> ({@code title="Tambah Alat Transportasi
 *       Siswa"}) — tidak berdampak karena {@code JenisTinggalSiswaAction.init(...)} selalu
 *       menimpanya dengan "Tambah/Ubah Jenis Tinggal Siswa" sebelum jendela ditampilkan, tetapi
 *       menegaskan bahwa layar ini disalin dari saudaranya {@link AlatTransportasiSiswa}.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.JenisTinggalMahasiswa
 * @see Siswa#getJenisTinggal()
 * @see AlatTransportasiSiswa
 * @see StatusAwalSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jenis_tinggal_mahasiswa")
public class JenisTinggalSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya <b>identik</b> dengan
	 * {@link ais.database.model.JenisTinggalMahasiswa} — bukti bahwa kelas ini lahir sebagai
	 * salinan entity versi perguruan tinggi. Entity ini di-serialisasi karena disimpan sebagai
	 * atribut komponen ZK (desktop/session) dan sebagai {@code value} pada {@code Comboitem}.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer, di-<i>generate</i> basis data. Dipetakan lewat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris; diisi otomatis, lihat {@link #setOleh(String)}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris; diisi otomatis, lihat {@link #setOlehId(String)}. */
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
	 * Menyetel id pengguna yang mengubah baris ini, dengan penjagaan: nilai {@code null} atau yang
	 * hanya berisi spasi <b>diabaikan diam-diam</b> sehingga nilai lama dipertahankan.
	 *
	 * <p>Perilaku "tolak nilai kosong" ini disengaja: pengisi jejak audit
	 * ({@code AuditTimestampInterceptor} lewat {@code onUpdate()}) dipanggil
	 * juga dari konteks tanpa pengguna login (job terjadwal, impor, auto-seed), dan tanpa
	 * penjagaan ini jejak audit yang sudah benar akan terhapus menjadi kosong. Efek sampingnya:
	 * kolom ini <b>tidak bisa dikosongkan kembali</b> lewat setter.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong/hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang mengubah baris ini, dengan penjagaan yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong/hanya spasi diabaikan diam-diam dan
	 * nilai lama dipertahankan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong/hanya spasi
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
	 * Callback JPA {@code @PreUpdate} — implementasi dari satu-satunya method {@code abstract} di
	 * {@link ais.database.model.GeneralValueObject}. Dipanggil Hibernate tepat sebelum setiap
	 * UPDATE dan mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan {@link #setTanggal_dirubah(Date)}
	 * dari konteks pengguna aktif.
	 *
	 * <p><b>Perhatikan:</b> tidak ada pasangan {@code @PrePersist}, sehingga pada INSERT jejak
	 * audit hanya berisi nilai awal field {@code tanggal_dirubah} (waktu objek dibuat di memori)
	 * dan {@code oleh}/{@code olehId} tetap {@code null} sampai baris pertama kali diubah.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris yang sama dengan
	 * callback ini (pola seragam di seluruh entity AIS). Nilai awalnya
	 * {@code ais.ui.util.WaktuUtil.getDate()} — waktu server saat objek dibuat, bukan waktu
	 * INSERT.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi — nilai {@code null} diterima apa adanya.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini. Dipetakan sebagai kolom
	 * {@code tanggal_dirubah} bertipe {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibuat
	 *         (diinisialisasi dengan waktu server), namun bisa {@code null} bila kolomnya kosong di
	 *         basis data
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks berformat {@code "<id>-<nama>"} — <b>menyimpang</b> dari format induk
	 * {@code "kode - nama"} ({@link ais.database.model.GeneralValueObject#toString()}).
	 *
	 * <p>Membaca field {@code nama} secara langsung, bukan lewat {@link #getNama()}, sehingga spasi
	 * tepi tidak ter-{@code trim}. Karena seluruh titik tampil UI (grid master, combo formulir
	 * siswa, ekspor Excel) memakai properti eksplisit, method ini praktis hanya muncul di log dan
	 * pesan debug.</p>
	 *
	 * @return {@code "<id>-<nama>"}; bagian {@code id} berisi {@code "null"} untuk objek yang belum
	 *         tersimpan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode kategori. Pada baris hasil auto-seed berisi kode Feeder PDDikti sebagai teks
	 * ({@code "1"}..{@code "5"}, {@code "99"}); pada baris buatan pengguna berisi teks bebas.
	 */
	private String kode;

	/** Nama kategori yang ditampilkan di grid dan combo, mis. "Bersama orang tua", "Kost". */
	private String nama;

	/** Keterangan bebas; benar-benar dipetakan dan tersimpan, lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Penanda baris aktif. <b>Tidak pernah ditulis saat baris dibuat</b> (lihat catatan pola
	 * "kolom aktif tak pernah ditulis" pada Javadoc kelas) sehingga nilainya {@code NULL} sampai
	 * checkbox di layar master ditekan.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Juga dipakai
	 * {@code JenisTinggalSiswaAction.onAdd()} untuk menyiapkan formulir baris baru dan
	 * {@code InitDataHelper} saat menyalin katalog dari versi mahasiswa. Seluruh field dibiarkan
	 * {@code null} kecuali {@code tanggal_dirubah} yang diisi waktu server.
	 */
	public JenisTinggalSiswa() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Nilai dibangkitkan basis data ({@code IDENTITY}) dan kolomnya ditandai
	 * {@code insertable = false}, jadi jangan pernah menyetel {@link #setId(Long)} sebelum
	 * menyimpan baris baru. Getter inilah yang menentukan bahwa seluruh pemetaan kelas ini memakai
	 * <b>akses properti</b> (anotasi dibaca dari getter, bukan field) — itulah sebabnya
	 * {@link #getNama()} yang men-{@code trim} ikut memengaruhi nilai yang ditulis ke basis data.
	 * {@code GeneralValueObject.equals(...)} juga berbasis nilai ini.</p>
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
	 * Menyetel kunci primer. Hanya untuk Hibernate dan kebutuhan uji; jangan dipanggil dari kode
	 * aplikasi karena kolomnya {@code insertable = false}.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode kategori.
	 *
	 * <p>Tanpa {@code @Column} eksplisit, jadi dipetakan ke kolom bernama {@code kode} lewat naming
	 * strategy default. Ditampilkan sebagai kolom pertama grid master dan dapat dicari lewat kotak
	 * "Kode" ({@code ilike} ANYWHERE). Tidak divalidasi keunikannya dan tidak wajib diisi.</p>
	 *
	 * @return kode kategori, atau {@code null} bila kosong
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode kategori. Tanpa validasi.
	 *
	 * <p>Dipanggil dari {@code JenisTinggalSiswaAction.onSave()} (isi {@code Textbox} "Kode Jenis
	 * Tinggal Siswa") dan dari auto-seed {@code InitDataHelper} yang mengisinya dengan
	 * {@code jenisTinggalMahasiswa.getFeeder() + ""} — perhatikan bahwa konkatenasi ini menghasilkan
	 * string {@code "null"} bila {@code feeder} kosong.</p>
	 *
	 * @param kode kode kategori
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama kategori, <b>sudah ter-{@code trim}</b>.
	 *
	 * <p>Pemangkasan dilakukan hanya pada nilai yang dikembalikan — field tidak ditugaskan ulang,
	 * jadi ini BUKAN getter write-back/destruktif. Meski begitu, karena pemetaan memakai akses
	 * properti, Hibernate membaca nilai lewat getter ini saat flush sehingga yang tersimpan ke
	 * basis data pun sudah ter-{@code trim}.</p>
	 *
	 * <p>Ini juga kunci urut ketiga {@code GeneralValueObject.compareTo(...)} dan — karena dua
	 * kunci sebelumnya ({@code nomorUrut}, {@code nim}) tidak pernah terisi pada kelas ini —
	 * praktis menjadi satu-satunya kunci pengurutan yang berlaku.</p>
	 *
	 * <p>Nilai inilah yang menjadi label combo "Jenis Tinggal" pada formulir biodata siswa dan
	 * kolom "Nama" di grid master (lewat {@code RevisiHelper.createNewRevisi} yang membungkusnya
	 * sebagai tautan riwayat revisi Envers).</p>
	 *
	 * @return nama kategori tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kategori. Tanpa validasi dan tanpa {@code trim} — pemangkasan baru terjadi saat
	 * dibaca kembali lewat {@link #getNama()}.
	 *
	 * <p>Kolomnya {@code nullable = false}, sehingga menyimpan objek dengan nama {@code null} akan
	 * gagal di tingkat basis data. Penjagaan di aplikasi ada di
	 * {@code JenisTinggalSiswaAction.onSave()}, yang menolak nama kosong dan nama yang sudah ada
	 * (lewat {@code checkNamaJenisTinggalSiswa()}) sebelum menyimpan — tetapi jalur impor Excel
	 * massal dan auto-seed tidak melewati penjagaan itu.</p>
	 *
	 * @param nama nama kategori
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini.
	 *
	 * <p><b>Membalik kontrak kelas induk:</b>
	 * {@code GeneralValueObject.getKeterangan()} menjamin tidak pernah mengembalikan {@code null}
	 * (mengembalikan {@code ""}), sedangkan override ini mengembalikan field mentah sehingga
	 * <b>bisa {@code null}</b>. Pemanggil yang mengandalkan kontrak induk berisiko NPE.</p>
	 *
	 * <p>Berbeda dengan beberapa entity katalog lain di modul ini, properti {@code keterangan} di
	 * sini <b>benar-benar dipetakan</b> ({@code @Column(name = "keterangan")}) karena dideklarasikan
	 * ulang di kelas ini — jadi isian Keterangan pada formulir tersimpan permanen, tidak hilang
	 * setiap request.</p>
	 *
	 * <p>Ditampilkan sebagai kolom ketiga grid master dan sebagai teks deskripsi pada combo "Jenis
	 * Tinggal" di formulir biodata siswa ({@code Common.insertComboDanSemua(..., "keterangan", ...)}).</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi.
	 *
	 * <p>Dipanggil dari {@code JenisTinggalSiswaAction.onSave()} dengan isi {@code Textbox}
	 * "Keterangan" (3 baris). Berbeda dengan entity katalog yang tidak memetakan properti ini,
	 * nilai yang disetel di sini benar-benar dipersistensikan.</p>
	 *
	 * @param keterangan keterangan bebas; {@code null} diperbolehkan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif baris, dengan <b>normalisasi toleran-NULL</b>: {@code NULL}
	 * dianggap {@code true}.
	 *
	 * <p>Normalisasi ini yang menyembunyikan bug "kolom aktif tak pernah ditulis" di layar master:
	 * baris baru (yang selalu masuk dengan {@code aktif = NULL}) tampak tercentang dan tetap lolos
	 * filter grid yang memakai {@code isNull("aktif") OR eq("aktif", true)}. Normalisasi ini
	 * <b>tidak</b> berlaku pada query: combo "Jenis Tinggal" di formulir biodata siswa menyaring
	 * dengan {@code Restrictions.eq("aktif", true)} di tingkat SQL, yang tidak pernah cocok dengan
	 * {@code NULL} — sehingga baris tersebut tak pernah bisa dipilih sampai checkbox "Aktif"
	 * di-toggle dua kali. Lihat Javadoc kelas untuk rinciannya.</p>
	 *
	 * <p>Tanpa {@code @Column} eksplisit; dipetakan ke kolom {@code aktif} lewat naming strategy
	 * default.</p>
	 *
	 * @return {@code true} bila baris aktif atau nilainya belum pernah ditulis; {@code false} hanya
	 *         bila secara eksplisit dinonaktifkan. Tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif baris. Tanpa validasi; {@code null} diterima dan akan dibaca kembali
	 * sebagai {@code true} oleh {@link #getAktif()}.
	 *
	 * <p><b>Satu-satunya pemanggil di seluruh source tree</b> adalah listener {@code onCheck}
	 * checkbox "Aktif" pada renderer grid {@code JenisTinggalSiswaAction}, yang langsung menyusul
	 * dengan {@code Common.refreshSaveOrUpdate(...)} sehingga perubahan tersimpan seketika tanpa
	 * tombol Simpan. Checkbox itu dinonaktifkan bila pengguna tidak punya hak {@code UPDATE} —
	 * meski hak yang diuji adalah hak atas menu induk, bukan atas layar ini (lihat Javadoc kelas).
	 * Karena {@code onSave()} tidak pernah memanggil method ini, inilah satu-satunya cara kolom
	 * {@code aktif} pernah memperoleh nilai eksplisit.</p>
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
