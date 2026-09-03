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




import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;



import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Entity master <b>kategori (seksi) field kustom untuk data Calon Siswa</b> pada modul PSB
 * (Penerimaan Siswa Baru), dipetakan ke tabel {@code sekolah.kelompok_parameter_tambahan_calon_siswa}.
 *
 * <p>Satu baris entity ini mewakili satu <i>judul seksi</i> pada formulir pendaftaran calon siswa —
 * misalnya baris bawaan {@code "VII. Form Tambahan"}. Entity ini <b>tidak</b> menyimpan definisi
 * field maupun nilai isian; keduanya hidup di lapis lain. Rantai lengkapnya:</p>
 *
 * <ol>
 *   <li><b>Kategori</b> — entity ini. Hanya nama, keterangan, penanda aktif, dan nomor urut tampil.</li>
 *   <li><b>Penghubung</b> — {@link ais.database.model.sekolah.ParameterTambahanGelombangPendaftaranPsb},
 *   yang memasangkan satu {@code ParameterTambahan} (definisi field: label, tipe isian, wajib
 *   lampiran, dsb.) dengan satu kategori entity ini <i>dan</i> dengan satu
 *   {@code GelombangPendaftaranPsb}. Lapis inilah pemilik saklar visibilitas
 *   {@code tampilDiFromSebelumLogin}/{@code tampilDiFromSetelahLogin} (perhatikan salah eja
 *   "From" pada nama properti aslinya).</li>
 *   <li><b>Nilai isian</b> — didenormalisasi ke kolom text milik pemilik data, yaitu
 *   {@code CalonSiswa.parameterTambahan}/{@code parameterTambahanInds} (dan salinannya di
 *   {@code Siswa} setelah calon dipromosikan menjadi siswa). Kunci gabungan yang dipakai
 *   berbentuk <code>idKelompok-&gt;idParameter</code>, kunci yang sama juga menjadi argumen
 *   {@code jenis} pada {@code LampiranLain.ambil(idPemilik, jenis)}. Konsekuensinya: <b>id baris
 *   entity ini ikut menjadi bagian kunci penyimpanan nilai</b>, sehingga memindahkan sebuah field
 *   ke kategori lain memutus keterkaitan dengan nilai yang sudah tersimpan.</li>
 * </ol>
 *
 * <h3>Padanan versi Perguruan Tinggi</h3>
 * <p>Kembaran entity ini untuk jalur PMB adalah
 * {@link ais.database.model.KelompokParameterTambahanCalonMahasiswa}. Keduanya jelas hasil
 * salin-tempel — bahkan {@code serialVersionUID}-nya <b>identik</b>
 * ({@code 2463821577548439808L}). Perbedaan yang benar-benar ada, terverifikasi dari kode:</p>
 * <ul>
 *   <li>Versi PT memiliki dua field saklar visibilitas milik kategori itu sendiri
 *   ({@code tampilDiFormPendaftaran} dan {@code tampilDiFormSetelahLogin}); <b>entity ini tidak
 *   memilikinya sama sekali</b>. Di jalur sekolah, keputusan tampil/tidak dipindahkan ke lapis
 *   penghubung per-field, bukan per-kategori.</li>
 *   <li>Akibat langsungnya, kuirk "aman secara bawaan" versi PT — kategori bawaan lahir dengan
 *   {@code tampilDiFormPendaftaran = false} sehingga tidak pernah muncul sebelum admin
 *   mengaktifkannya — <b>TIDAK berlaku di sini</b>. Lihat {@link #checkCreateDefault()}.</li>
 *   <li>Selain itu isi kelas praktis sama kata-per-kata: field, getter/setter, auto-seed, dan
 *   {@link #compareTo(GeneralValueObject)} yang sama-sama dipangkas menjadi satu baris.</li>
 * </ul>
 *
 * <h3>Cakupan data (multi-sekolah)</h3>
 * <p>Berbeda dari kerabat dekatnya di paket yang sama ({@code KelompokParameterTambahanCatatanSiswa},
 * {@code ...CatatanGuru}, {@code ...CatatanKelasSiswa}) yang punya kolom cakupan
 * {@code yayasan}/{@code sekolah}, entity ini <b>tidak punya kolom cakupan apa pun</b>. Seluruh
 * baris bersifat global: pada instalasi yayasan dengan banyak sekolah, setiap kategori yang dibuat
 * satu sekolah tampil pada formulir PSB semua sekolah.</p>
 *
 * <h3>Tempat pemakaian</h3>
 * <ul>
 *   <li><b>Layar master</b> — {@code ais.action.master.sekolah.KelompokParameterTambahanCalonSiswaAction}
 *   dengan tampilan {@code /pages/psb/kelompok_parameter_tambahan_calon_siswa.zul}. Layar ini
 *   tidak berdiri sendiri di menu, melainkan disisipkan sebagai tab "Manajemen Kelompok" di dalam
 *   {@code ParameterTambahanGelombangPendaftaranPsbAction}.</li>
 *   <li><b>Formulir pendaftaran</b> — {@code ais.action.master.sekolah.psb.ParameterTambahanPsbListener}
 *   membangun seksi formulir per kategori; nilai isiannya dipanen kembali oleh
 *   {@code CalonSiswa.populateParameterTambahan(List)} dan {@code Siswa.populateParameterTambahan(List)}.</li>
 *   <li><b>Tampilan pengumuman</b> — {@code TampilanPengumumanAkademisAction} menampilkan ulang
 *   nilai isian beserta lampirannya, dikelompokkan per kategori.</li>
 *   <li><b>Data awal</b> — {@code ais.common.InitData} mendaftarkan kelas ini pada
 *   {@code initClasses(...)}.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ul>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, callback {@code onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Atribut domain:</b> {@link #getNama()}/{@link #setNama(String)},
 *   {@link #getKeterangan()}/{@link #setKeterangan(String)},
 *   {@link #getAktif()}/{@link #setAktif(Boolean)},
 *   {@link #getNomorUrut()}/{@link #setNomorUrut(Integer)},
 *   {@link #getDefaultData()}/{@link #setDefaultData(Boolean)}.</li>
 *   <li><b>Utilitas statis:</b> {@link #checkCreateDefault()}.</li>
 *   <li><b>Pengurutan:</b> {@link #compareTo(GeneralValueObject)} (override dipangkas).</li>
 * </ul>
 *
 * <h3>Catatan tentang kelas induk</h3>
 * <p>{@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu pun
 * propertinya. Karena itu deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi yang keliru</b>, melainkan keharusan
 * teknis agar kolom-kolom tersebut benar-benar terpetakan.</p>
 *
 * <h3>Anotasi kelas</h3>
 * <p>{@code @Audited} (Envers) merekam versi setiap perubahan sehingga tombol revisi
 * ({@code RevisiHelper.createNewRevisi}) di grid master dapat menampilkan riwayat.
 * {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya menyertakan kolom yang
 * benar-benar terisi/berubah pada pernyataan SQL.</p>
 *
 * @see ais.database.model.KelompokParameterTambahanCalonMahasiswa
 * @see ais.database.model.sekolah.ParameterTambahanGelombangPendaftaranPsb
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "kelompok_parameter_tambahan_calon_siswa")



public class KelompokParameterTambahanCalonSiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya <b>persis sama</b> dengan milik
	 * {@link ais.database.model.KelompokParameterTambahanCalonMahasiswa} — jejak salin-tempel antar
	 * kedua kelas. Tidak berbahaya (identitas serialisasi ditentukan nama kelas, bukan hanya angka
	 * ini), tetapi menjadi petunjuk bahwa perubahan pada salah satu kelas biasanya perlu
	 * dipertimbangkan juga pada kembarannya.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris; nilainya dihasilkan database ({@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;

	/** Nama tampil pengguna yang terakhir mengubah baris ini; diisi otomatis lewat {@code onUpdate()}. */
	private String oleh;

	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis lewat {@code onUpdate()}. */
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
	 * Menyetel id pengguna pengubah terakhir, dengan <b>penolakan senyap</b>: nilai {@code null}
	 * atau string kosong/whitespace diabaikan tanpa pesan apa pun sehingga nilai lama tetap utuh.
	 *
	 * <p>Tujuannya menjaga jejak audit agar tidak terhapus oleh pemanggil yang menyalin properti
	 * secara borongan (mis. {@code BeanUtils.copyProperties}) dari object kosong.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penolakan senyap yang sama seperti
	 * {@link #setOlehId(String)}: {@code null} atau string kosong/whitespace diabaikan.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pengisian jejak audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate
	 * menerbitkan {@code UPDATE}.
	 *
	 * <p><b>Efek samping:</b> memutasi state instance ini. Tidak dipanggil pada {@code INSERT}
	 * (hanya {@code @PreUpdate}), sehingga baris bawaan hasil {@link #checkCreateDefault()} lahir
	 * <b>tanpa jejak</b> {@code oleh}/{@code olehId}.</p>
	 *
	 * <p><b>Perhatian format:</b> baris sumber ini juga memuat deklarasi field
	 * {@code tanggal_dirubah} yang diinisialisasi ke waktu server saat instance dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) — konsekuensinya, entity yang baru dibuat sudah
	 * memiliki stempel waktu meski belum pernah disimpan. Penggabungan method + field dalam satu
	 * baris adalah pola penyisipan otomatis yang dipakai di seluruh entity AIS; jangan dirapikan
	 * tanpa alasan kuat karena banyak perkakas repo mencocokkannya secara harfiah.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; {@code null} diterima apa adanya.
	 *
	 * <p>Umumnya tidak perlu dipanggil manual — {@code AuditTimestampInterceptor} mengisinya lewat
	 * callback {@code onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Tidak pernah {@code null} pada instance yang dibuat lewat konstruktor, karena field-nya
	 * sudah diinisialisasi ke waktu server saat deklarasi.</p>
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas baris ini dalam bentuk <code>id + "-" + nama</code>.
	 *
	 * <p><b>Menimpa</b> {@link ais.database.model.GeneralValueObject#toString()} yang formatnya
	 * <code>kode + " - " + nama</code>. Membaca field {@code nama} secara langsung (bukan lewat
	 * {@link #getNama()}), jadi hasilnya tidak di-{@code trim} dan dapat berbentuk
	 * {@code "null-null"} pada instance baru yang belum disimpan.</p>
	 *
	 * @return teks gabungan id dan nama kelompok
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kategori/seksi sebagaimana tampil sebagai judul pada formulir PSB. Wajib diisi. */
	private String nama;

	/** Keterangan bebas kategori; ditampilkan pada kolom kedua grid master. Boleh kosong. */
	private String keterangan;

	/**
	 * Penanda baris bawaan hasil {@link #checkCreateDefault()}. Baris ber-{@code true} tidak
	 * menampilkan tombol Hapus di grid master.
	 */
	private Boolean defaultData;

	/** Penanda aktif; hanya kategori aktif yang ikut disaring ke formulir PSB dan tampilan pengumuman. */
	private Boolean aktif;

	/**
	 * Nomor urut tampil seksi pada formulir PSB. Menjadi satu-satunya kunci
	 * {@link #compareTo(GeneralValueObject)}.
	 */
	private Integer nomorUrut;

	/**
	 * Memastikan tersedianya satu baris kategori <b>bawaan</b> ({@code defaultData = true}) sebagai
	 * penampung field kustom yang belum dikelompokkan, dan mengembalikannya.
	 *
	 * <p>Alurnya: mencari baris pertama dengan {@code defaultData = true} lewat session native. Bila
	 * tidak ditemukan, membuat baris baru bernama dan berketerangan {@code "VII. Form Tambahan"},
	 * lalu <b>membuka dan meng-commit transaksinya sendiri</b> di tempat. Terakhir memanggil
	 * {@code HibernateUtil.closeSession()} tanpa syarat — juga ketika baris sudah ada dan tidak ada
	 * yang ditulis.</p>
	 *
	 * <p><b>Efek samping yang perlu disadari:</b></p>
	 * <ul>
	 *   <li><b>Menulis ke database dari jalur baca.</b> Method ini dipanggil dari
	 *   {@code doAfterCompose()} — jadi sekadar membuka layar dapat menyisipkan baris master baru.</li>
	 *   <li><b>Menutup session bersama.</b> Karena {@code closeSession()} dipanggil di tengah
	 *   lifecycle ZK, object lain yang masih {@code lazy} pada session yang sama menjadi
	 *   {@code detached}. Pemanggil harus memuat ulang apa pun yang dibutuhkan setelahnya.</li>
	 *   <li><b>Tanpa jejak audit.</b> Karena {@code INSERT} tidak memicu {@code @PreUpdate}, baris
	 *   bawaan lahir dengan {@code oleh}/{@code olehId} kosong.</li>
	 *   <li><b>Langsung terlihat calon siswa.</b> Berbeda dari padanan versi PT
	 *   ({@link ais.database.model.KelompokParameterTambahanCalonMahasiswa}) yang menyetel
	 *   {@code tampilDiFormPendaftaran = false} sehingga aman secara bawaan, entity ini tidak punya
	 *   saklar semacam itu. Baris bawaan langsung {@code aktif} (lihat {@link #getAktif()}) dan
	 *   akan tampil begitu ada satu saja field yang menunjuk ke sana — termasuk pada formulir
	 *   sebelum login, karena filter {@code tampilDiFromSebelumLogin} di lapis penghubung
	 *   memperlakukan nilai {@code null} sebagai "boleh tampil".</li>
	 * </ul>
	 *
	 * <p><b>Pemanggil nyata (satu-satunya di codebase):</b>
	 * {@code ais.action.master.sekolah.ParameterTambahanGelombangPendaftaranPsbAction.doAfterCompose(Component)}.
	 * Di sana hasilnya langsung dipakai untuk sebuah migrasi data mentah: sebuah
	 * {@code createSQLQuery("update sekolah.parameter_tambahan_gelombang_pendaftaran_psb set
	 * kelompok_parameter_tambahan_calon_siswa=&lt;id&gt; where ... is null")} yang dijalankan
	 * <b>setiap kali layar dibuka, tanpa syarat</b> — sehingga seluruh baris penghubung yatim
	 * "diadopsi" oleh kategori bawaan. Karena berupa SQL native, perubahan itu melewati Envers dan
	 * tidak muncul pada riwayat revisi.</p>
	 *
	 * @return baris kategori bawaan yang sudah ada, atau baris baru yang barusan disimpan; tidak
	 *         pernah {@code null}
	 */
	public static KelompokParameterTambahanCalonSiswa checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanCalonSiswa kelompokParameterTambahanCalonSiswa = (KelompokParameterTambahanCalonSiswa) session
				.createCriteria(KelompokParameterTambahanCalonSiswa.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanCalonSiswa == null) {
			kelompokParameterTambahanCalonSiswa = new KelompokParameterTambahanCalonSiswa();
			kelompokParameterTambahanCalonSiswa.setDefaultData(true);
			kelompokParameterTambahanCalonSiswa.setNama("VII. Form Tambahan");
			kelompokParameterTambahanCalonSiswa.setKeterangan("VII. Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanCalonSiswa);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanCalonSiswa;
	}

	/**
	 * Konstruktor tanpa argumen. Diperlukan Hibernate untuk membuat instance saat memuat baris, dan
	 * dipakai langsung oleh {@link #checkCreateDefault()} serta handler tombol "Tambah" pada layar
	 * master ({@code KelompokParameterTambahanCalonSiswaAction.onAdd(Event)}).
	 *
	 * <p>Seluruh properti dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang sudah terisi
	 * waktu server dari inisialisasi field.</p>
	 */
	public KelompokParameterTambahanCalonSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Nilainya juga menjadi bagian kunci penyimpanan nilai isian di
	 * {@code CalonSiswa}/{@code Siswa} (format <code>idKelompok-&gt;idParameter</code>), sehingga id
	 * kategori bersifat semantik, bukan sekadar surrogate key internal.</p>
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code IDENTITY}).</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris. Tanpa validasi.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate; kode aplikasi sebaiknya tidak menyetelnya manual karena
	 * nilai dibangkitkan database.</p>
	 *
	 * @param id id baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kategori, dengan {@code trim()} otomatis pada spasi di tepi.
	 *
	 * <p><b>Menimpa</b> {@link ais.database.model.GeneralValueObject#getNama()} yang mengembalikan
	 * nilai apa adanya. Karena Hibernate memakai property access, hasil {@code trim()} inilah yang
	 * ikut tertulis kembali ke database pada penyimpanan berikutnya — spasi tepi tidak pernah
	 * bertahan lama.</p>
	 *
	 * @return nama kategori tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kategori. Tanpa validasi di lapis entity.
	 *
	 * <p>Validasi "wajib diisi" dan "nama tidak boleh duplikat" ditegakkan di layar master
	 * ({@code onSave(Event)} dan {@code checkNamaKelompokParameterTambahanCalonSiswa()}), bukan di
	 * sini — penulis lewat jalur lain (mis. {@link #checkCreateDefault()}) tidak melewati
	 * pemeriksaan itu.</p>
	 *
	 * @param nama nama kategori baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan kategori <b>apa adanya</b>.
	 *
	 * <p><b>Membalik kontrak kelas induk.</b> {@link ais.database.model.GeneralValueObject#getKeterangan()}
	 * menormalkan {@code null} menjadi {@code ""} dan menjanjikan "tidak pernah {@code null}";
	 * override ini menghapus jaminan tersebut dan dapat mengembalikan {@code null}. Pemanggil yang
	 * menulis kode berdasarkan kontrak induk (mis. langsung memanggil {@code .trim()} atau
	 * {@code .isEmpty()}) berisiko {@code NullPointerException}.</p>
	 *
	 * <p>Dampak praktisnya di sini terbatas: pemakaian utamanya adalah
	 * {@code new Label(...getKeterangan())} pada renderer grid master, yang menerima {@code null}
	 * dengan aman. Cabang {@code keterangan} pada {@code compareTo} kelas induk pun tidak pernah
	 * tercapai karena {@link #compareTo(GeneralValueObject)} di kelas ini sudah dipangkas.</p>
	 *
	 * @return keterangan kategori, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan kategori. Tanpa validasi; {@code null} diterima.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda "baris bawaan", dengan <b>normalisasi destruktif</b>: bila field masih
	 * {@code null}, field itu <i>diisi</i> {@code false} lebih dulu, bukan sekadar dikembalikan.
	 *
	 * <p><b>Efek samping:</b> karena Hibernate memakai property access, sekadar membaca baris lama
	 * yang berkolom {@code NULL} akan membuat nilai {@code false} tertulis ke database pada flush
	 * berikutnya.</p>
	 *
	 * <p><b>Pemakaian:</b> {@link #checkCreateDefault()} memakai propertinya sebagai kriteria
	 * pencarian, dan renderer grid master memakainya untuk menyembunyikan tombol Hapus pada baris
	 * bawaan ({@code delete && !getDefaultData()}).</p>
	 *
	 * @return {@code true} bila baris ini kategori bawaan; tidak pernah {@code null}
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menyetel penanda "baris bawaan". Tanpa validasi.
	 *
	 * <p>Di codebase hanya {@link #checkCreateDefault()} yang menyetelnya ke {@code true}; layar
	 * master tidak menyediakan kontrol untuk mengubahnya, sehingga status ini praktis permanen
	 * setelah baris terbentuk.</p>
	 *
	 * @param defaultData {@code true} untuk menandai baris sebagai kategori bawaan
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan penanda aktif, dengan normalisasi destruktif yang sama seperti
	 * {@link #getDefaultData()} — bedanya nilai bawaannya {@code true}, sehingga kategori yang
	 * kolomnya masih {@code NULL} dianggap <b>aktif</b>.
	 *
	 * <p><b>Efek samping:</b> membaca baris berkolom {@code NULL} menulis balik {@code true} pada
	 * flush berikutnya.</p>
	 *
	 * <p><b>Pemakaian:</b> menjadi filter keras di jalur tampil —
	 * {@code ParameterTambahanPsbListener} dan {@code TampilanPengumumanAkademisAction} sama-sama
	 * menambahkan {@code Restrictions.eq("kelompokParameterTambahanCalonSiswa.aktif", true)}.
	 * Konsekuensinya, menonaktifkan satu kategori <b>menyembunyikan seluruh seksi</b> beserta
	 * semua field di dalamnya dari formulir PSB, meski masing-masing field masih bertanda aktif.
	 * Perhatikan filter itu memakai perbandingan {@code eq true} yang tidak cocok dengan
	 * {@code NULL} di level SQL, jadi baris yang belum pernah dibaca/di-flush ulang tetap
	 * tersembunyi walaupun getter ini menganggapnya aktif.</p>
	 *
	 * @return {@code true} bila kategori aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel penanda aktif. Tanpa validasi.
	 *
	 * <p>Dipanggil dari event {@code onCheck} checkbox "Aktif" pada grid master, yang langsung
	 * menyimpan lewat {@code Common.refreshSaveOrUpdate(...)}. Checkbox tersebut sudah dijaga hak
	 * akses ({@code setDisabled(!edit)}).</p>
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil, dengan normalisasi destruktif ke {@code 1} bila masih
	 * {@code null}.
	 *
	 * <p><b>Efek samping:</b> membaca baris berkolom {@code NULL} menulis balik {@code 1} pada
	 * flush berikutnya. Perhatikan pula bahwa pemeriksaan {@code null} dilakukan <b>dua kali</b>
	 * (blok {@code if} lalu operator terner); cabang terner sudah tidak mungkin tercapai —
	 * redundansi yang tidak berbahaya, warisan pola salin-tempel keluarga kelas ini.</p>
	 *
	 * <p><b>Konsekuensi operasional:</b> formulir Tambah/Ubah pada layar master hanya menyediakan
	 * isian <i>Nama Kelompok</i> dan <i>Keterangan</i> — tidak ada isian nomor urut. Karena itu
	 * setiap kategori baru lahir dengan nomor urut {@code 1} dan semua kategori bernilai sama
	 * sampai admin mengubahnya lewat kolom "Nomor Urut" di grid. Selama semua nilai seri, urutan
	 * seksi pada formulir PSB praktis ditentukan urutan baris yang dikembalikan database
	 * (pengurutan yang dipakai konsumen bersifat stabil, jadi baris seri mempertahankan urutan
	 * asalnya) — bukan urutan yang sengaja dirancang.</p>
	 *
	 * @return nomor urut tampil; tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil. Tanpa validasi — nilai negatif, nol, maupun duplikat diterima.
	 *
	 * <p><b>Satu-satunya jalur UI</b> yang memanggilnya adalah {@code Intbox} pada kolom "Nomor
	 * Urut" grid master, yang pada event {@code onChange} langsung menyimpan lewat
	 * {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * <p><b>Perhatian hak akses:</b> berbeda dari checkbox "Aktif" ({@code setDisabled(!edit)}) dan
	 * tombol Ubah/Hapus ({@code setVisible(edit)}/{@code setVisible(delete && ...)}) di baris yang
	 * sama, {@code Intbox} tersebut <b>tidak diberi guard hak akses sama sekali</b> di
	 * {@code KelompokParameterTambahanCalonSiswaAction}. Pengguna yang hanya berhak {@code READ}
	 * tetap dapat mengubah nomor urut dan perubahannya langsung tersimpan permanen, sehingga urutan
	 * seksi formulir PSB berubah bagi semua orang. Ini pola yang berulang di seluruh keluarga
	 * {@code KelompokParameterTambahan*Action} dan sudah tercatat sebagai temuan audit tersendiri;
	 * jangan diperbaiki sepihak di satu berkas saja.</p>
	 *
	 * @param nomorUrut nomor urut tampil baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membandingkan dua kategori <b>hanya</b> berdasarkan {@link #getNomorUrut()}.
	 *
	 * <p><b>Menimpa</b> {@link ais.database.model.GeneralValueObject#compareTo(GeneralValueObject)}
	 * yang berjenjang empat kunci ({@code nomorUrut} → {@code nim} → {@code nama} →
	 * {@code keterangan}) dan dibungkus {@code try/catch}. Versi ini dipangkas menjadi satu baris:
	 * <b>tanpa kunci cadangan</b> dan <b>tanpa penanganan exception</b>. Akibatnya seluruh kategori
	 * yang bernomor urut sama dianggap setara, tanpa pembeda nama.</p>
	 *
	 * <p><b>Risiko:</b> argumen di-cast paksa ke {@code KelompokParameterTambahanCalonSiswa},
	 * sehingga membandingkan dengan {@link ais.database.model.GeneralValueObject} jenis lain
	 * melempar {@code ClassCastException} — tidak seperti versi induk yang menelan exception dan
	 * mengembalikan {@code 0}. {@code arg0} bernilai {@code null} melempar
	 * {@code NullPointerException}. Dalam praktiknya aman karena kedua pemanggil nyata menyortir
	 * daftar homogen.</p>
	 *
	 * <p><b>Pemanggil nyata:</b> {@code Collections.sort(...)} atas {@code List} hasil query
	 * {@code Projections.groupProperty(...)} di
	 * {@code ais.action.master.sekolah.psb.ParameterTambahanPsbListener} dan
	 * {@code ais.action.master.TampilanPengumumanAkademisAction} — keduanya menentukan urutan seksi
	 * pada formulir pendaftaran dan pada tampilan pengumuman. Karena keduanya memakai
	 * {@code List} (bukan {@code TreeSet}/{@code TreeMap}), kategori bernomor urut kembar
	 * <b>tidak</b> saling melenyapkan; keduanya tetap tampil, hanya urutannya yang tidak
	 * ditentukan. Konsistensi ini perlu dijaga: mengganti struktur data konsumen menjadi
	 * {@code TreeSet} akan langsung menghilangkan seksi kembar dari formulir, karena
	 * {@code compareTo} di sini tidak konsisten dengan {@link #equals(Object)}.</p>
	 *
	 * <p>Grid layar master sendiri <b>tidak</b> memakai method ini — daftarnya diurutkan di
	 * database berdasarkan {@code nama} ({@code Order.asc("nama")}), sehingga urutan yang dilihat
	 * admin di layar master tidak mencerminkan urutan yang dilihat calon siswa di formulir.</p>
	 *
	 * @param arg0 kategori pembanding; harus bertipe {@code KelompokParameterTambahanCalonSiswa}
	 *             dan tidak boleh {@code null}
	 * @return negatif/nol/positif sesuai perbandingan nomor urut
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanCalonSiswa) arg0).getNomorUrut());
	}
}
