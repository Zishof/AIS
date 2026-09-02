package ais.database.model;

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

/**
 * <b>Penempatan kelas pada jalur Penerimaan Mahasiswa Baru (PMB)</b>: satu baris menyatakan
 * "kelas {@link Kelas} bernama {@code X} dibuka untuk menampung {@code N} calon mahasiswa pada
 * cakupan Fakultas/Prodi tertentu, berlaku untuk gelombang pendaftaran {@code G} <i>atau</i>
 * untuk kombinasi tahun akademik + jenis semester tertentu". Memetakan tabel
 * {@code public.kelas_pmb}.
 *
 * <p><b>Ini bukan master kelas.</b> Master kelas paralel akademik tetap {@link Kelas}; entity ini
 * hanyalah <i>pembungkus PMB</i> atas kelas tersebut — ia menambahkan kuota
 * ({@link #getKapasitasRuangan()}), penanda penuh ({@link #getPenuh()}), cakupan
 * ({@link #getFakultas()}/{@link #getJurusan()}) dan cakupan waktu
 * ({@link #getGelombangPendaftaran()} atau {@link #getTahunAkademik()} +
 * {@link #getJenisSemester()}). Namanya sendiri tidak pernah diketik pengguna: lihat
 * {@link #getNama()}, yang selalu menyalin ulang nama dari {@link #getKelas()}. Entity ini adalah
 * salah satu dari lima pemegang <i>foreign key</i> sungguhan ke {@link Kelas} (lihat daftar di
 * Javadoc {@link Kelas}).</p>
 *
 * <h3>Peran dalam alur PMB</h3>
 *
 * <ol>
 *   <li>Calon mahasiswa mendaftar dan datanya tersimpan sebagai
 *   {@link BiodataCalonMahasiswa}.</li>
 *   <li>Setelah dinyatakan diterima ({@code BiodataCalonMahasiswa.prodiLulus} terisi), calon itu
 *   ditempatkan ke sebuah baris {@code KelasPmb} lewat kolom
 *   {@code biodata_calon_mahasiswa.kelas_pmb} — arah relasinya <b>dari</b> calon mahasiswa,
 *   entity ini sendiri <b>tidak</b> menyimpan koleksi anggota. Karena itu "jumlah terisi" selalu
 *   dihitung ulang dengan {@code count} atas {@link BiodataCalonMahasiswa} (lihat
 *   {@code KelasPmbAction.cekRuanganIsi(KelasPmb)}), bukan disimpan di baris ini.</li>
 *   <li>Penempatan dapat dilakukan manual (panel detail
 *   {@code KelasPmbPunyaBiodataCalonMahasiswaHelper}: tombol "Ambil Calon Mahasiswa",
 *   "Bersihkan", dan tombol lepas per baris) atau massal-otomatis lewat
 *   {@code KelasPmbAction.executeSyncMassal()}.</li>
 *   <li>Saat calon mahasiswa berubah menjadi {@link Mahasiswa} (daftar ulang), penempatan ini ikut
 *   terbawa: {@link Mahasiswa#getKelasPmb()} membaca kolomnya sendiri <i>lalu menimpanya</i> dari
 *   {@code BiodataCalonMahasiswa} bila calon asalnya masih terhubung, dan
 *   {@link Mahasiswa#getKelas()} (String) mengambil {@code getKelasPmb().getKelas().getNama()}.
 *   Jadi baris {@code KelasPmb} adalah jembatan yang mengubah hasil penempatan PMB menjadi nama
 *   kelas akademik mahasiswa.</li>
 * </ol>
 *
 * <h3>Cakupan waktu: dua mode yang saling eksklusif (di UI)</h3>
 *
 * <p>Sebuah baris seharusnya dipakai dalam salah satu dari dua mode:</p>
 * <ul>
 *   <li><b>Mode gelombang</b> — {@link #getGelombangPendaftaran()} diisi; tahun akademik dan
 *   jenis semester diambil dari gelombang itu.</li>
 *   <li><b>Mode tahun akademik</b> — gelombang dikosongkan;
 *   {@link #getTahunAkademik()} + {@link #getJenisSemester()} diisi sendiri, sehingga kelas
 *   berlaku lintas gelombang dalam satu semester.</li>
 * </ul>
 * <p>{@code KelasPmbAction.init(KelasPmb)} menegakkan keeksklusifan itu di layar (memilih
 * gelombang otomatis menonaktifkan dua dropdown TA/semester dan sebaliknya), dan
 * {@code onSave(Event)} bahkan meng-{@code null}-kan sisi yang tidak dipakai. <b>Namun lihat butir
 * 4 di bawah</b>: getter di kelas ini membatalkan pengosongan tersebut pada saat <i>flush</i>.</p>
 *
 * <h3>Pengelompokan anggota kelas ini</h3>
 *
 * <ul>
 *   <li><b>Identitas &amp; audit</b>: {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Identitas kelas</b>: {@link #getKelas()} (satu-satunya sumber identitas nyata) dan
 *   {@link #getNama()} (cache turunan darinya).</li>
 *   <li><b>Kuota</b>: {@link #getKapasitasRuangan()} dan penanda {@link #getPenuh()}.</li>
 *   <li><b>Cakupan organisasi</b>: {@link #getFakultas()}, {@link #getJurusan()} — kosong berarti
 *   "semua", persis seperti pada {@link Kelas}.</li>
 *   <li><b>Cakupan waktu</b>: {@link #getGelombangPendaftaran()}, {@link #getTahunAkademik()},
 *   {@link #getJenisSemester()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>Pemetaan memakai <i>property access</i>.</b> Seluruh anotasi JPA menempel pada
 *   <i>getter</i>, sehingga Hibernate membaca nilai lewat getter — termasuk ketika menyusun
 *   snapshot dan ketika menulis INSERT/UPDATE. Setiap getter di kelas ini yang menormalkan atau
 *   menulis balik ke field karenanya ikut menentukan <b>isi database</b>, bukan sekadar isi layar.
 *   Konsekuensi ini berlaku untuk {@link #getNama()}, {@link #getKapasitasRuangan()},
 *   {@link #getPenuh()}, {@link #getTahunAkademik()} dan {@link #getJenisSemester()}.</li>
 *   <li><b>Empat getter relasi menulis balik hasil {@code check()}.</b> {@link #getKelas()},
 *   {@link #getFakultas()}, {@link #getJurusan()} dan {@link #getGelombangPendaftaran()} semuanya
 *   berbentuk {@code x = check(x); return x;}. {@link GeneralValueObject#check(Object)} dapat
 *   <b>membuka sesi Hibernate sendiri</b> (tahap 3, reload object detached) dan menutupnya lagi di
 *   {@code finally}, sehingga memanggil getter relasi pada instance detached bisa memicu akses
 *   basis data yang tidak terlihat di pemanggil. Tidak ada getter destruktif di kelas ini (tidak
 *   ada yang menghapus baris atau me-{@code null}-kan relasi lain), tetapi ada empat getter yang
 *   <b>menulis balik ke field</b> — lihat butir 3-5.</li>
 *   <li><b>{@link #getNama()} adalah cache turunan, bukan data yang bisa diisi.</b> Getter selalu
 *   menimpa field {@code nama} dengan nama {@link #getKelas()} ({@code ""} bila kelas kosong), jadi
 *   {@link #setNama(String)} praktis tidak berpengaruh dan layar tambah/ubah pun tidak menyediakan
 *   isian nama. Konsekuensi penting: kolom {@code nama} tetap dipetakan dan diandalkan oleh
 *   pencarian ({@code Restrictions.ilike("nama", ...)}) serta pengurutan antrean overflow
 *   ({@code Order.asc("nama")}) di {@code KelasPmbAction} — keduanya bekerja hanya karena getter
 *   ini menyalin ulang nama kelas ke kolom pada setiap flush. Efek sampingnya: mengganti nama di
 *   master {@link Kelas} otomatis merambat ke seluruh baris {@code KelasPmb} yang memakainya
 *   (pada penyimpanan berikutnya), dan bila relasi {@link #getKelas()} hilang, nama tersimpan
 *   berubah menjadi string kosong secara permanen.</li>
 *   <li><b>{@link #getTahunAkademik()} dan {@link #getJenisSemester()} membatalkan pengosongan
 *   yang dilakukan {@code onSave}.</b> Keduanya menimpa field dari
 *   {@link #getGelombangPendaftaran()} bila gelombang terisi. Karena pemetaan memakai property
 *   access, nilai {@code null} yang sengaja ditulis {@code KelasPmbAction.onSave(Event)} dalam
 *   mode gelombang <b>tidak pernah benar-benar tersimpan</b>: saat flush, kolom
 *   {@code tahunakademik}/{@code jenissemester} terisi ulang dari gelombang. Hasilnya konsisten
 *   (bukan data salah), tetapi asumsi "kalau mode gelombang maka dua kolom itu kosong" tidak
 *   berlaku di database — kode pembaca harus tetap memeriksa gelombang lebih dulu, seperti yang
 *   dilakukan {@code executeSyncMassal()} dan perender grid.</li>
 *   <li><b>Nilai bawaan yang diam-diam tersimpan.</b> {@link #getKapasitasRuangan()} mengubah
 *   {@code null} menjadi <b>3000</b> dan {@link #getPenuh()} mengubah {@code null} menjadi
 *   {@code 0}; {@link #getJenisSemester()} mengubah {@code null} menjadi
 *   {@link Perkuliahan#GANJIL}. Sekali getter dipanggil (termasuk oleh Hibernate saat flush),
 *   nilai bawaan itu ikut tertulis ke baris. Angka 3000 patut diwaspadai: baris yang kapasitasnya
 *   kosong di basis data akan diperlakukan sebagai kelas berkapasitas 3000 oleh sinkronisasi
 *   massal.</li>
 *   <li><b>Tidak ada properti {@code aktif} maupun {@code keterangan}.</b> Berbeda dari kebanyakan
 *   master lain, baris {@code KelasPmb} tidak bisa dinonaktifkan — satu-satunya cara menghentikan
 *   pengisian adalah mencentang {@link #getPenuh()} atau menghapus barisnya. Properti
 *   {@code keterangan} dan {@code aktif} milik {@link GeneralValueObject} <b>tidak</b>
 *   dideklarasikan ulang di sini sehingga tidak terpetakan Hibernate; jangan mengandalkan
 *   nilainya bertahan (lihat butir 8).</li>
 *   <li><b>Keunikan tidak dijamin.</b> Tidak ada {@code unique = true} maupun pemeriksaan di UI
 *   yang mencegah dua baris {@code KelasPmb} menunjuk {@link Kelas} yang sama pada gelombang atau
 *   TA/semester yang sama. Dua baris kembar seperti itu akan diperlakukan sebagai dua kelas
 *   terpisah dalam antrean overflow sinkronisasi massal.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *   duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa — bukan
 *   {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate sama sekali tidak
 *   memetakan properti kelas induk. Setiap entity turunan wajib mendeklarasikan sendiri kolom
 *   yang ingin dipetakan; itulah sebabnya empat properti itu muncul lagi di sini, sementara
 *   {@code nama}/{@code keterangan}/{@code aktif} milik induk tidak otomatis ikut.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi tidak
 *   ada {@code @PrePersist}, jadi {@code oleh}/{@code olehId} baru terisi pada perubahan pertama,
 *   bukan saat baris dibuat. Riwayat lengkapnya bergantung pada {@code @Audited} (Envers), yang
 *   ditampilkan di grid lewat {@code RevisiHelper.createNewRevisi(KelasPmb.class, ...)}.</li>
 *   <li><b>Entity ini ikut dipanaskan saat aplikasi start.</b> {@code ais.common.InitData}
 *   mendaftarkan {@code KelasPmb.class} ke {@code initClasses(...)}, sehingga barisnya masuk cache
 *   {@code ConstantValues} dan dapat diambil tanpa menyentuh basis data.</li>
 *   <li><b>Komentar generator di berkas ini salah.</b> Baris "Ruang generated by hbm2java" adalah
 *   sisa salin-tempel — komentar yang sama muncul di {@code RuangPMB}, {@code RuangPSB},
 *   {@code InterviewCalonMahasiswa}, {@code KelasSiswaPSB} dan
 *   {@code recruitment.RuangPegawai}, sementara {@code Ruang} sendiri justru tidak memuatnya.
 *   Jangan memakainya untuk menyimpulkan asal-usul tabel.</li>
 *   <li><b>Catatan kontrol akses.</b> {@code /pages/master/kelas_pmb.zul} <i>tidak</i> termasuk
 *   daftar {@code MUST_CHECKED} di {@code CommonPrivilages}, tetapi
 *   {@code KelasPmbAction.doAfterCompose} memeriksa sesi login dan
 *   {@code CommonPrivilages.READ} secara eksplisit, serta menyembunyikan tombol
 *   Tambah/Ubah/Hapus/Sinkronisasi sesuai hak — sejauh itu <b>contoh positif</b>. Yang
 *   <b>tidak</b> berpagar: checkbox "Penuh" di grid (langsung {@code refreshSaveOrUpdate} tanpa
 *   pemeriksaan {@code UPDATE}) dan dua tombol pada panel detail keanggotaan
 *   ("Ambil Calon Mahasiswa" dan "Bersihkan" di
 *   {@code KelasPmbPunyaBiodataCalonMahasiswaHelper}, yang masing-masing menautkan calon
 *   mahasiswa secara batch dan melepas <i>seluruh</i> anggota kelas lewat satu SQL update) —
 *   pengguna berhak baca saja tetap dapat menjalankan ketiganya.</li>
 *   <li><b>Perender grid menulis ke basis data.</b> {@code KelasPmbAction.KelasPmbRenderer}
 *   memanggil {@code Common.refreshUpdate(kelasPmb)} ketika jumlah terisi mencapai kapasitas —
 *   sekadar menampilkan daftar dapat memicu UPDATE pada baris-baris yang tampil.</li>
 * </ol>
 *
 * @see GeneralValueObject
 * @see Kelas
 * @see BiodataCalonMahasiswa#getKelasPmb()
 * @see Mahasiswa#getKelasPmb()
 * @see GelombangPendaftaran
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kelas_pmb")
public class KelasPmb extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibekukan agar instance yang tersimpan di
	 * {@code HttpSession} atau dikirim antar-node tetap kompatibel walau field kelas ini berubah.
	 */
	private static final long serialVersionUID = -7550466125892447098L;

	/** Kunci primer {@code kelas_pmb.id}; lihat {@link #getId()}. */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}. Dideklarasikan ulang di sini
	 * (bukan diwarisi) karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private String oleh;

	/**
	 * Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh} dan diisi
	 * pada saat yang sama oleh {@link #onUpdate()}.
	 */
	private String olehId;

	/**
	 * Id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna audit; {@code null} bila baris belum pernah diubah setelah dibuat
	 *         (tidak ada {@code @PrePersist} di kelas ini).
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna audit. Nilai {@code null} atau yang hanya berisi spasi
	 * <b>diabaikan</b> (field lama dipertahankan), sehingga jejak audit tidak bisa dikosongkan
	 * secara tidak sengaja oleh pemanggil yang mengirim nilai kosong.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna audit. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan</b> agar jejak audit tidak terhapus.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna audit; {@code null} bila baris belum pernah diubah.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait siklus hidup JPA yang dijalankan tepat sebelum setiap UPDATE: mendelegasikan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} yang mengisi {@link #oleh}, {@link #olehId} dan
	 * {@link #tanggal_dirubah} dari pengguna yang sedang login. Tidak ada {@code @PrePersist}
	 * padanannya, jadi baris yang baru dibuat belum memiliki jejak pembuat.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server saat instance dibuat
	 * ({@code WaktuUtil.getDate()}) sehingga baris baru pun sudah bernilai, lalu diperbarui pada
	 * setiap UPDATE lewat {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; umumnya diisi otomatis oleh {@link #onUpdate()}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang dibuat lewat
	 *         konstruktor karena field sudah diinisialisasi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity, dipakai ZK sebagai label bawaan pada {@code Combobox}/{@code Listbox}.
	 *
	 * <p><b>Perhatian:</b> mengembalikan <i>field</i> {@link #nama} secara langsung, <b>bukan</b>
	 * {@link #getNama()}. Untuk instance yang belum pernah melewati {@link #getNama()} maupun
	 * pemuatan Hibernate (mis. {@code new KelasPmb()}), hasilnya {@code null} — bukan string
	 * kosong. Gunakan {@link #getNama()} bila membutuhkan nilai yang selalu tersinkron dengan
	 * {@link #getKelas()}.</p>
	 *
	 * @return nama kelas apa adanya dari field; dapat {@code null}.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Cache nama kelas, disalin ulang dari {@link #getKelas()} setiap kali {@link #getNama()}
	 * dipanggil. Kolom {@code nama} tetap dipetakan karena dipakai untuk pencarian dan pengurutan
	 * di {@code KelasPmbAction}.
	 */
	private String nama;

	/** Master kelas yang dibuka untuk PMB — satu-satunya sumber identitas baris ini. */
	private Kelas kelas;

	/** Daya tampung kelas (jumlah calon mahasiswa maksimum); lihat {@link #getKapasitasRuangan()}. */
	private Integer kapasitasRuangan;

	/** Pembatas cakupan fakultas; {@code null} berarti berlaku untuk semua fakultas. */
	private Fakultas fakultas;

	/** Pembatas cakupan program studi; {@code null} berarti berlaku untuk semua prodi. */
	private Jurusan jurusan;

	/** Gelombang pendaftaran tempat kelas ini berlaku; {@code null} bila memakai mode TA/semester. */
	private GelombangPendaftaran gelombangPendaftaran;

	/** Penanda kelas sudah penuh: {@code 1} penuh, {@code 0} masih menerima. Bukan {@code Boolean}. */
	private Integer penuh;

	/** Tahun akademik pada mode non-gelombang; ikut ditimpa dari gelombang, lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;

	/** Jenis semester (Ganjil/Genap) pada mode non-gelombang; lihat {@link #getJenisSemester()}. */
	private String jenisSemester;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA; juga dipakai
	 * {@code KelasPmbAction.onAdd(Event)} untuk membuka form tambah dengan baris kosong.
	 */
	public KelasPmb() {
	}

	/**
	 * Kunci primer baris ini ({@code kelas_pmb.id}), dibangkitkan basis data
	 * ({@code GenerationType.IDENTITY}) sehingga kolomnya tidak ikut dalam INSERT
	 * ({@code insertable = false}).
	 *
	 * @return id baris; {@code null} bila entity belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci primer. Umumnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci primer.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama kelas PMB, <b>selalu diturunkan ulang</b> dari {@link #getKelas()}.
	 *
	 * <p><b>Getter yang menulis ke field.</b> Sebelum mengembalikan nilai, method ini menimpa field
	 * {@link #nama} dengan nama master kelas — atau dengan string kosong bila {@link #getKelas()}
	 * bernilai {@code null}. Karena pemetaan memakai <i>property access</i>, nilai hasil timpaan
	 * itulah yang ditulis Hibernate ke kolom {@code nama} pada setiap flush. Akibatnya:</p>
	 * <ul>
	 *   <li>{@link #setNama(String)} praktis tidak berpengaruh, dan layar tambah/ubah memang tidak
	 *   menyediakan isian nama;</li>
	 *   <li>perubahan nama di master {@link Kelas} merambat ke baris ini pada penyimpanan
	 *   berikutnya;</li>
	 *   <li>bila relasi kelas hilang, kolom {@code nama} menjadi string kosong secara permanen;</li>
	 *   <li>pencarian {@code ilike("nama", ...)} dan pengurutan antrean overflow
	 *   {@code Order.asc("nama")} di {@code KelasPmbAction} bekerja justru karena penyalinan
	 *   ini.</li>
	 * </ul>
	 * <p>Perhatikan pemanggilan {@link #getKelas()} di dalamnya dapat memicu resolusi proxy lazy
	 * ({@link GeneralValueObject#check(Object)}), termasuk membuka sesi Hibernate sementara bila
	 * instance sudah detached.</p>
	 *
	 * @return nama kelas yang sudah dipangkas spasi; string kosong bila kelas belum dipilih. Cabang
	 *         pengembalian {@code null} secara praktis tidak pernah tercapai karena field selalu
	 *         diisi lebih dulu di baris sebelumnya.
	 */
	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		nama = getKelas() == null ? "" : getKelas().getNama();
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama kelas. Praktis tidak berguna: {@link #getNama()} akan menimpanya kembali dari
	 * {@link #getKelas()} pada pemanggilan berikutnya, termasuk saat Hibernate melakukan flush.
	 *
	 * @param nama nama kelas.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menetapkan daya tampung kelas.
	 *
	 * @param kapasitasRuangan jumlah calon mahasiswa maksimum yang boleh ditempatkan.
	 */
	public void setKapasitasRuangan(Integer kapasitasRuangan) {
		this.kapasitasRuangan = kapasitasRuangan;
	}

	/**
	 * Daya tampung kelas: jumlah maksimum {@link BiodataCalonMahasiswa} yang boleh menunjuk baris
	 * ini.
	 *
	 * <p><b>Getter yang menulis ke field.</b> Bila field masih {@code null}, nilainya diisi
	 * <b>3000</b> lebih dulu. Karena pemetaan memakai <i>property access</i>, nilai bawaan itu ikut
	 * tersimpan ke kolom {@code kapasitas_ruangan} pada flush berikutnya. Angka ini bukan sekadar
	 * kosmetik: {@code KelasPmbAction.executeSyncMassal()} menghitung sisa kursi sebagai
	 * {@code getKapasitasRuangan() - jumlahTerisi}, sehingga baris yang kapasitasnya kosong di basis
	 * data akan diperlakukan sebagai kelas berkapasitas 3000.</p>
	 *
	 * @return daya tampung; tidak pernah {@code null}.
	 */
	@Column(name = "kapasitas_ruangan", length = 10, nullable = false)
	public Integer getKapasitasRuangan() {
		if (kapasitasRuangan == null) {
			kapasitasRuangan = 3000;
		}
		return kapasitasRuangan;
	}

	/**
	 * Menetapkan gelombang pendaftaran. Mengisi nilai non-{@code null} berarti memilih "mode
	 * gelombang": {@link #getTahunAkademik()} dan {@link #getJenisSemester()} akan ikut mengambil
	 * nilai dari gelombang ini.
	 *
	 * @param gelombangPendaftaran gelombang pendaftaran; {@code null} untuk mode TA/semester.
	 */
	public void setGelombangPendaftaran(GelombangPendaftaran gelombangPendaftaran) {
		this.gelombangPendaftaran = gelombangPendaftaran;
	}

	/**
	 * Gelombang pendaftaran tempat kelas ini berlaku (kolom {@code gelombang_pendaftaran}).
	 *
	 * <p><b>Getter yang menulis balik hasil resolusi lazy.</b> Nilai dilewatkan
	 * {@link GeneralValueObject#check(Object)} lalu disimpan kembali ke field; pada instance
	 * detached, {@code check} dapat membuka dan menutup sesi Hibernate sendiri untuk memuat ulang
	 * object. Getter ini dipanggil dari {@link #getTahunAkademik()} dan
	 * {@link #getJenisSemester()}, sehingga dua getter itu pun berpotensi menyentuh basis data.</p>
	 *
	 * <p>Dipakai {@code KelasPmbAction.executeSyncMassal()} sebagai bagian pertama kunci
	 * pengelompokan ("GEL|&lt;id&gt;"), dan oleh perender grid untuk memilih label kolom periode.</p>
	 *
	 * @return gelombang pendaftaran; {@code null} bila baris memakai mode tahun akademik.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran")
	public GelombangPendaftaran getGelombangPendaftaran() {
		gelombangPendaftaran = check(gelombangPendaftaran);
		return gelombangPendaftaran;
	}

	/**
	 * Menetapkan penanda penuh.
	 *
	 * @param penuh {@code 1} bila kelas dinyatakan penuh, {@code 0} bila masih menerima.
	 */
	public void setPenuh(Integer penuh) {
		this.penuh = penuh;
	}

	/**
	 * Penanda kelas sudah penuh, disimpan sebagai angka ({@code 1} = penuh, {@code 0} = masih
	 * menerima) alih-alih boolean.
	 *
	 * <p><b>Getter yang menulis ke field:</b> {@code null} diubah menjadi {@code 0} lebih dulu,
	 * sehingga baris lama yang kolomnya kosong ikut terisi {@code 0} pada flush berikutnya.</p>
	 *
	 * <p>Nilainya dikelola dari tiga arah: (a) checkbox "Penuh" di grid — tanpa pemeriksaan hak
	 * {@code UPDATE}; (b) perender grid, yang otomatis menyetel {@code 1} dan menyimpan baris saat
	 * jumlah terisi mencapai {@link #getKapasitasRuangan()}; dan (c)
	 * {@code KelasPmbAction.executeSyncMassal()}, yang menyinkronkan ulang penanda ini di awal dan
	 * menyetelnya menjadi {@code 1} begitu kursi terakhir sebuah kelas terisi. Perhatikan penanda
	 * ini hanya <b>informasi turunan</b>: penempatan tetap ditentukan perhitungan sisa kursi yang
	 * dihitung ulang, bukan oleh nilai kolom ini.</p>
	 *
	 * @return {@code 1} atau {@code 0}; tidak pernah {@code null}.
	 */
	@Column(name = "penuh")
	public Integer getPenuh() {
		if (penuh == null) {
			penuh = 0;
		}
		return penuh;
	}

	/**
	 * Master kelas akademik yang dibuka untuk PMB (kolom {@code kelas}) — inti identitas baris ini.
	 *
	 * <p><b>Getter yang menulis balik hasil resolusi lazy</b> ({@code kelas = check(kelas)}), dengan
	 * kemungkinan membuka sesi Hibernate sementara pada instance detached. Nilai inilah yang
	 * disalin {@link #getNama()} ke kolom {@code nama}, dan yang akhirnya menjadi
	 * {@link Mahasiswa#getKelas()} (String) setelah calon mahasiswa didaftar-ulangkan.</p>
	 *
	 * @return master kelas; {@code null} bila belum dipilih (kondisi yang dicegah validasi
	 *         {@code KelasPmbAction.onSave(Event)}, tetapi tidak dicegah basis data).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas")
	public Kelas getKelas() {
		kelas = check(kelas);
		return kelas;
	}

	/**
	 * Menetapkan master kelas. Karena {@link #getNama()} menurunkan namanya dari sini, mengganti
	 * nilai ini otomatis mengganti nama baris pada penyimpanan berikutnya.
	 *
	 * @param kelas master kelas akademik.
	 */
	public void setKelas(Kelas kelas) {
		this.kelas = kelas;
	}

	/**
	 * Pembatas cakupan fakultas (kolom {@code fakultas}); {@code null} berarti kelas berlaku untuk
	 * <b>semua</b> fakultas — grid menampilkannya sebagai "Semua".
	 *
	 * <p><b>Getter yang menulis balik hasil resolusi lazy</b> ({@code fakultas = check(fakultas)}).
	 * Berbeda dari {@link Kelas#getFakultas()} dan {@link ItemBiayaPunyaAkun#getFakultas()}, getter
	 * ini <b>tidak</b> memaksa nilai mengikuti fakultas milik {@link #getJurusan()} — dua kolom
	 * dapat saling bertentangan tanpa ada yang menjaganya.</p>
	 *
	 * <p>Dipakai {@code executeSyncMassal()} sebagai bagian kedua kunci pengelompokan
	 * ("FAK|&lt;id&gt;", dipakai hanya bila prodi kosong) dan untuk menyaring calon mahasiswa lewat
	 * {@code prodiLulus.fakultas}.</p>
	 *
	 * @return fakultas cakupan; {@code null} berarti semua fakultas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas")
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menetapkan pembatas cakupan fakultas.
	 *
	 * @param fakultas fakultas cakupan; {@code null} agar kelas berlaku untuk semua fakultas.
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Pembatas cakupan program studi (kolom {@code jurusan}); {@code null} berarti kelas berlaku
	 * untuk <b>semua</b> prodi — grid menampilkannya sebagai "Semua". Teks bantuan di layar master
	 * menyatakannya eksplisit ("Kosongkan Jurusan jika kelas ini berlaku untuk semua Jurusan").
	 *
	 * <p><b>Getter yang menulis balik hasil resolusi lazy</b> ({@code jurusan = check(jurusan)}).
	 * Dipakai {@code executeSyncMassal()} sebagai bagian kedua kunci pengelompokan
	 * ("PRODI|&lt;id&gt;", diprioritaskan di atas fakultas) dan disaring terhadap
	 * {@code BiodataCalonMahasiswa.prodiLulus} — jadi penempatan otomatis hanya mengambil calon
	 * yang <b>lulus di prodi ini</b>, bukan yang sekadar memilihnya.</p>
	 *
	 * @return prodi cakupan; {@code null} berarti semua prodi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan")
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan pembatas cakupan program studi.
	 *
	 * @param jurusan prodi cakupan; {@code null} agar kelas berlaku untuk semua prodi.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Tahun akademik berlakunya kelas ini pada "mode tahun akademik" (gelombang dikosongkan).
	 *
	 * <p><b>Getter yang menulis ke field.</b> Bila {@link #getGelombangPendaftaran()} terisi, field
	 * ditimpa dengan tahun akademik milik gelombang tersebut — nilai yang diisi lewat
	 * {@link #setTahunAkademik(String)} (termasuk {@code null} yang sengaja ditulis
	 * {@code KelasPmbAction.onSave(Event)} dalam mode gelombang) karenanya tidak bertahan. Karena
	 * pemetaan memakai <i>property access</i>, hasil timpaan itulah yang tersimpan ke basis data,
	 * sehingga asumsi "mode gelombang berarti kolom ini kosong" <b>tidak berlaku</b>. Pembaca harus
	 * memeriksa {@link #getGelombangPendaftaran()} lebih dulu, seperti yang dilakukan
	 * {@code executeSyncMassal()} dan perender grid.</p>
	 *
	 * <p>Properti ini tidak punya {@code @Column} maupun {@code @Transient}: ia tetap dipetakan,
	 * dengan nama kolom mengikuti {@code MyNamingStrategy} (mewarisi {@code DefaultNamingStrategy}
	 * tanpa mengubah pemetaan properti), yakni nama properti apa adanya yang oleh PostgreSQL
	 * diperlakukan huruf kecil: {@code tahunakademik}.</p>
	 *
	 * @return tahun akademik; dapat {@code null} bila kelas belum pernah diberi gelombang maupun
	 *         tahun akademik.
	 */
	public String getTahunAkademik() {
		if(getGelombangPendaftaran() != null) {
			tahunAkademik = getGelombangPendaftaran().getTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik. Nilai ini hanya bertahan selama {@link #getGelombangPendaftaran()}
	 * kosong; bila gelombang terisi, {@link #getTahunAkademik()} akan menimpanya kembali.
	 *
	 * @param tahunAkademik tahun akademik, mis. {@code "2026/2027"}.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Jenis semester berlakunya kelas ini ({@link Perkuliahan#GANJIL} atau
	 * {@link Perkuliahan#GENAP}) pada "mode tahun akademik".
	 *
	 * <p><b>Getter yang menulis ke field, dengan dua lapis timpaan:</b></p>
	 * <ol>
	 *   <li>bila field masih {@code null}, diisi nilai bawaan {@link Perkuliahan#GANJIL} — nilai
	 *   bawaan ini ikut tersimpan ke basis data pada flush berikutnya, sehingga baris yang tidak
	 *   pernah diisi semesternya diam-diam tercatat sebagai Ganjil;</li>
	 *   <li>bila {@link #getGelombangPendaftaran()} terisi, field ditimpa lagi dengan jenis semester
	 *   milik gelombang tersebut (yang di {@link GelombangPendaftaran#getJenisSemester()} sendiri
	 *   juga berdefault Ganjil), membatalkan nilai apa pun yang diisi lewat
	 *   {@link #setJenisSemester(String)}.</li>
	 * </ol>
	 *
	 * <p>Sama seperti {@link #getTahunAkademik()}, properti ini tidak beranotasi {@code @Column}
	 * tetapi tetap dipetakan ke kolom {@code jenissemester}.</p>
	 *
	 * @return {@code "Ganjil"} atau {@code "Genap"}; tidak pernah {@code null} setelah getter ini
	 *         dipanggil.
	 */
	public String getJenisSemester() {
		if (jenisSemester == null) {
			jenisSemester = Perkuliahan.GANJIL;
		}
		if(getGelombangPendaftaran() != null) {
			jenisSemester = getGelombangPendaftaran().getJenisSemester();
		}
		return jenisSemester;
	}

	/**
	 * Menetapkan jenis semester. Nilai ini hanya bertahan selama
	 * {@link #getGelombangPendaftaran()} kosong; bila gelombang terisi,
	 * {@link #getJenisSemester()} akan menimpanya kembali.
	 *
	 * @param jenisSemester {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}.
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

}
