package ais.database.model.rab;

// Generated Dec 19, 2009 10:58:09 PM by Hibernate Tools 3.2.4.CR1

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.library.Perpustakaan;

/**
 * Entitas <b>Proyek</b> pada modul RAB/perencanaan anggaran, dipetakan ke tabel
 * {@code rab.proyek}. Sebuah {@code Proyek} adalah wadah penjadwalan pekerjaan: ia menaungi
 * sekumpulan {@link Tugas} yang membentuk model bergaya <i>Gantt chart</i> (tanggal mulai/selesai,
 * bobot, predecessor). Berbeda dari kebanyakan entitas sepaket, {@code Proyek} sendiri hampir tidak
 * membawa data substantif — hanya {@code nama} dan {@code keterangan} — karena seluruh isi
 * perencanaan berada pada anak-anaknya.
 *
 * <h2>Posisi dalam struktur modul RAB</h2>
 * <p>Verifikasi atas basis kode menempatkan {@code Proyek} pada dua sumbu sekaligus:</p>
 * <ul>
 *   <li><b>Ke bawah, sebagai induk tugas.</b> {@link Tugas#getProyek()} memegang referensi ke
 *   entitas ini lewat kolom {@code proyek}. {@code TugasAction}, {@code TugasRevisiAction},
 *   {@code TugasTreeModel}, dan {@code AmbilDataTugasBanyak} semuanya menyaring daftar tugas
 *   berdasarkan proyek terpilih. Satu proyek berisi banyak tugas.</li>
 *   <li><b>Ke samping, sebagai lampiran pada pohon anggaran.</b> {@link #getWorkspace()} menunjuk
 *   satu simpul {@link Workspace} — pohon program/kegiatan/sub-kegiatan yang menyimpan nilai
 *   anggaran. {@code ProyekAction} menampilkan {@code getWorkspace().getHargaTotal()} sebagai kolom
 *   pada daftar proyek, sehingga hubungan ini berperan menautkan jadwal pelaksanaan dengan pagu
 *   anggaran simpul terkait. Relasi bersifat opsional ({@code nullable = true}): proyek boleh
 *   berdiri tanpa simpul anggaran.</li>
 * </ul>
 * <p>Perlu ditegaskan bahwa {@code Proyek} <b>tidak</b> berelasi langsung dengan
 * {@link OutputKegiatan}, {@link RencanaDanRealisasiOutputKegiatan}, {@link Tor}, maupun
 * {@link RenstraProgram}. Jadi meski sama-sama berada di modul RAB, jalur "penjadwalan pekerjaan"
 * (Proyek → Tugas) dan jalur "capaian output kegiatan" (OutputKegiatan →
 * RencanaDanRealisasiOutputKegiatan) adalah dua cabang terpisah yang hanya bertemu secara tidak
 * langsung melalui {@link Workspace} dan {@link SatuanKerja}.</p>
 *
 * <h2>Mekanisme revisi proyek</h2>
 * <p>{@code Proyek} adalah salah satu dari dua sumbu operasi "buat revisi baru" pada
 * {@code ais.action.master.rab.util.RabUtil}. Method
 * {@code RabUtil#createNewRevisi(Integer, Integer, Proyek, Proyek, EventListener)} menyalin seluruh
 * pohon {@link Tugas} dari satu proyek/revisi ke proyek/revisi tujuan. Yang penting dipahami:
 * <b>nomor revisi disimpan pada {@code Tugas}, bukan pada {@code Proyek}</b> — entitas ini tidak
 * punya field {@code revisi} sama sekali. Karena itu satu baris {@code Proyek} dapat menaungi
 * beberapa generasi revisi tugas sekaligus, dan pemanggil yang ingin melihat "proyek revisi ke-N"
 * harus selalu menyaring {@code Tugas} berdasarkan pasangan ({@code proyek}, {@code revisi}).
 * {@code RabUtil} memperingatkan pengguna dan <b>menghapus</b> data lama bila revisi tujuan sudah
 * terisi, sehingga operasi ini destruktif terhadap tugas — bukan terhadap baris {@code Proyek}
 * itu sendiri.</p>
 *
 * <h2>PERINGATAN — pembatasan tenant bersifat opsional pada layar pencarian</h2>
 * <p>Entitas ini memang membawa relasi {@link SatuanKerja}, tetapi penyaringannya di
 * {@code ProyekAction} <b>tidak wajib</b>: kriteria pencarian menambahkan
 * {@code Restrictions.eq("satuanKerja", ...)} hanya bila kotak pencarian satuan kerja terisi, dan
 * jatuh ke {@code Restrictions.sqlRestriction("1=1")} bila dibiarkan kosong. Artinya membuka layar
 * daftar proyek tanpa mengisi filter akan menampilkan proyek <b>seluruh satuan kerja</b>, termasuk
 * milik satker lain, lengkap dengan nama dan nilai pagu anggarannya. Ini instansi dari pola
 * berulang "filter tenant lemah/hilang" yang sudah tercatat pada inisiatif dokumentasi ini —
 * penyaringan ada tetapi berupa kenyamanan pencarian, bukan penjaga otorisasi.</p>
 *
 * <h2>Pemetaan ORM</h2>
 * <p>Entitas memakai {@code dynamicInsert}/{@code dynamicUpdate} sehingga Hibernate hanya menulis
 * kolom yang benar-benar berubah, dan diberi {@link Audited} sehingga Hibernate Envers merekam
 * setiap revisi ke tabel bayangan pada skema {@code rab}. Kunci utama memakai strategi
 * {@link javax.persistence.GenerationType#IDENTITY}. Kedua relasi ({@code satuanKerja} dan
 * {@code workspace}) memakai {@link FetchType#LAZY} dengan {@code cascade} terbatas pada
 * {@link CascadeType#PERSIST} dan {@link CascadeType#MERGE} — tidak ada {@code REMOVE}, sehingga
 * menghapus proyek tidak pernah ikut menghapus satuan kerja atau simpul anggarannya.</p>
 *
 * @see Tugas
 * @see Workspace
 * @see SatuanKerja
 * @see ais.database.dao.rab.ProyekDao
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "proyek")

public class Proyek extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja disamakan dengan hampir seluruh entitas lain
	 * di paket {@code ais.database.model.rab} (hasil salin-tempel templat hbm2java), sehingga
	 * <b>tidak</b> bisa dipakai untuk membedakan tipe saat deserialisasi.
	 */
	private static final long serialVersionUID = -8738027816264807168L;

	/**
	 * Kunci utama basis data, dibangkitkan oleh kolom identity pada {@code rab.proyek.id}. Bernilai
	 * {@code null} selama objek belum pernah disimpan — kondisi ini juga dipakai
	 * {@link #getSatuanKerja()} sebagai penanda "objek masih baru".
	 */
	private Long id;

	/**
	 * Field audit bayangan: nama pengguna terakhir yang mengubah baris ini. Diisi lewat
	 * {@link #setOleh(String)} oleh lapisan interceptor/penyimpanan, bukan oleh pengguna.
	 */
	private String oleh;

	/**
	 * Field audit bayangan: identitas (id pengguna) terakhir yang mengubah baris ini. Pasangan dari
	 * {@link #oleh}, diisi oleh lapisan interceptor/penyimpanan.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Setter ini <b>menolak diam-diam</b> nilai
	 * {@code null} maupun string kosong/spasi sehingga jejak audit yang sudah terisi tidak terhapus
	 * oleh proses penyalinan objek (mis. saat revisi proyek) atau pengikatan form yang mengirim
	 * nilai kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau kosong diabaikan sehingga jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum operasi {@code UPDATE}, mendelegasikan
	 * pemutakhiran stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak boleh dipanggil
	 * langsung dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}

	/**
	 * Field audit bayangan: stempel waktu perubahan terakhir. Diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil#getDate()} (bukan {@code new Date()}, agar mengikuti zona
	 * waktu/penyesuaian waktu aplikasi) dan diperbarui otomatis oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya diisi otomatis lewat {@link #onUpdate()};
	 * pemanggilan manual hanya relevan pada skenario impor/migrasi data.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai kolom {@code TIMESTAMP}.
	 *
	 * @return waktu penyimpanan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nama proyek. Dipetakan ke kolom {@code varchar(255)} yang dinyatakan {@code NOT NULL} pada
	 * tingkat skema. Dipakai luas sebagai label pada pesan konfirmasi revisi di {@code RabUtil}.
	 */
	private String nama;

	/**
	 * Keterangan bebas atas proyek. Tidak diberi anotasi {@code @Column} eksplisit sehingga memakai
	 * pemetaan bawaan Hibernate (kolom {@code keterangan}, panjang bawaan 255).
	 */
	private String keterangan;

	/**
	 * Satuan kerja pemilik proyek. Berfungsi sebagai penanda tenant, diisi otomatis untuk objek baru
	 * oleh {@link #getSatuanKerja()}. Boleh {@code null}.
	 */
	private SatuanKerja satuanKerja;

	/**
	 * Simpul pohon anggaran ({@link Workspace}) yang ditautkan ke proyek ini, sumber nilai pagu yang
	 * ditampilkan pada daftar proyek. Boleh {@code null}.
	 */
	private Workspace workspace;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Juga dipakai {@code ProyekAction} saat menekan tombol tambah data — perhatikan bahwa objek
	 * hasil konstruktor ini punya {@code id} bernilai {@code null}, yang mengaktifkan pengisian
	 * otomatis satuan kerja pada {@link #getSatuanKerja()}.
	 */
	public Proyek() {
	}

	/**
	 * Konstruktor pintas yang langsung mengisi {@link #nama}, disediakan oleh generator hbm2java
	 * untuk kolom {@code NOT NULL}.
	 *
	 * @param nama nama proyek.
	 */
	public Proyek(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Normalnya diisi Hibernate setelah {@code INSERT}. Perlu diingat bahwa
	 * mengubah nilai ini dari {@code null} menjadi bukan-{@code null} juga mematikan pengisian
	 * otomatis satuan kerja pada {@link #getSatuanKerja()}.
	 *
	 * @param id kunci utama baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama proyek dengan spasi di ujung sudah dipangkas. Pemangkasan dilakukan pada
	 * getter (bukan setter), sehingga nilai di memori bisa saja masih mengandung spasi sementara
	 * nilai yang ditulis Hibernate ke basis data sudah terpangkas.
	 *
	 * @return nama proyek yang sudah dipangkas, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama proyek apa adanya, tanpa pemangkasan maupun validasi panjang. Kolom target
	 * dinyatakan {@code NOT NULL} dan dibatasi 255 karakter, sehingga nilai {@code null} atau lebih
	 * panjang dari 255 karakter baru gagal saat penyimpanan, bukan saat pemanggilan setter.
	 *
	 * @param nama nama proyek.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas atas proyek.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas atas proyek.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan satuan kerja pemilik proyek ini. Method ini <b>bukan getter murni</b>: ia
	 * melakukan dua hal berefek samping sebelum mengembalikan nilai, dan keduanya perlu dipahami
	 * sebelum menyunting kode di sekitarnya.
	 *
	 * <h3>Efek samping pertama — resolusi proxy lazy</h3>
	 * <p>Baris {@code satuanKerja = check(satuanKerja)} memanggil
	 * {@link GeneralValueObject#check(Object)}, utilitas bersama yang memaksa proxy Hibernate yang
	 * masih malas (belum ter-<i>initialize</i>) menjadi objek nyata, lalu menyimpan hasilnya kembali
	 * ke field. Tujuannya menghindari {@code LazyInitializationException} ketika objek dipakai di
	 * luar sesi Hibernate — misalnya saat baris di-<i>render</i> ke layar ZK atau diserialisasi.
	 * Karena hasilnya ditulis balik ke field, pemanggilan getter ini <b>mengubah state objek</b>,
	 * bukan sekadar membacanya. Relasi ini dideklarasikan {@link FetchType#LAZY}, jadi tanpa
	 * {@code check(...)} nilai yang dikembalikan bisa berupa proxy yang tidak dapat dipakai.</p>
	 *
	 * <h3>Efek samping kedua — pengisian otomatis tenant untuk objek baru</h3>
	 * <p>Bila setelah resolusi proxy nilainya masih {@code null} <b>dan</b> {@link #id} juga
	 * {@code null} (artinya objek belum pernah disimpan), method mengisi sendiri satuan kerja dari
	 * konteks pengguna yang sedang login, dengan urutan:</p>
	 * <ol>
	 *   <li>{@code Common.getCurrentUser().ambilSatuanKerja()} — satuan kerja yang melekat pada
	 *   pengguna aktif;</li>
	 *   <li>bila langkah pertama menghasilkan {@code null}, dicoba
	 *   {@code Common.getCurrentPerpustakaan().getSatuanKerja()} — satuan kerja milik konteks
	 *   perpustakaan aktif, jalur cadangan untuk sesi yang berjalan dalam konteks modul
	 *   perpustakaan.</li>
	 * </ol>
	 * <p>Penjaga {@code this.id == null} penting: ia memastikan pengisian otomatis <b>hanya</b>
	 * berlaku untuk objek yang belum tersimpan. Baris yang sudah ada di basis data dengan kolom
	 * {@code satuan_kerja} bernilai NULL tidak akan diam-diam dipindahkan ke satuan kerja pembaca —
	 * perilaku yang justru <i>tidak</i> dijaga pada {@link RenstraProgram#getSatuanKerja()}
	 * (bandingkan catatan pada berkas tersebut).</p>
	 *
	 * <h3>Penanganan galat</h3>
	 * <p>Seluruh blok pengisian otomatis dibungkus {@code try/catch(Exception)} yang tidak
	 * melemparkan ulang, melainkan hanya mencatat kejadian lewat
	 * {@code ais.common.ErrorAuditUtil.record(...)}. Ini disengaja: getter dipanggil dari banyak
	 * konteks tanpa sesi pengguna (batch, penjadwal, uji, deserialisasi Envers) di mana
	 * {@code Common.getCurrentUser()} wajar gagal, dan getter entitas tidak boleh menggagalkan
	 * proses hanya karena konteks tersebut tidak tersedia. Konsekuensinya bersifat
	 * <b>fail-open</b>: pada konteks tanpa pengguna, proyek baru tersimpan dengan
	 * {@code satuan_kerja} bernilai NULL. Baris ber-{@code satuanKerja} NULL semacam itu lolos dari
	 * penyaringan {@code Restrictions.eq("satuanKerja", ...)} di {@code ProyekAction}, sehingga
	 * praktis tidak terlihat oleh satker mana pun sampai filter dikosongkan.</p>
	 *
	 * <h3>Implikasi bagi pemanggil</h3>
	 * <p>Karena getter ini adalah <i>property accessor</i> yang dibaca Hibernate saat
	 * <i>dirty checking</i>, pengisian otomatis di atas juga menjadi mekanisme yang menetapkan
	 * pemilik saat {@code INSERT} pertama — tanpa perlu {@code ProyekAction} mengisinya secara
	 * eksplisit. Sebaliknya, jangan memanggil getter ini dari kode yang bertujuan sekadar
	 * "memeriksa apakah pemilik sudah diisi": pemanggilannya sendiri berpotensi mengisi field
	 * tersebut. Untuk pemeriksaan murni, bacalah hasil {@link #setSatuanKerja(SatuanKerja)}
	 * terakhir yang diketahui atau lakukan kueri langsung ke basis data.</p>
	 *
	 * @return satuan kerja pemilik proyek, atau {@code null} bila tidak dapat ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		if (this.satuanKerja == null && this.id == null) {
			try {
				SatuanKerja satuanKerja = Common.getCurrentUser().ambilSatuanKerja();
				Perpustakaan currentPerpustakaan = Common.getCurrentPerpustakaan();
				if (satuanKerja == null && currentPerpustakaan != null) {
					satuanKerja = currentPerpustakaan.getSatuanKerja();
				}
				this.satuanKerja = satuanKerja;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/Proyek.java:128");
			}
		}
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik proyek secara eksplisit. Pengisian manual ini mengalahkan
	 * pengisian otomatis pada {@link #getSatuanKerja()}, karena getter hanya bertindak ketika field
	 * masih {@code null}. Dipanggil {@code ProyekAction} saat pengguna memilih satuan kerja pada
	 * form. Tidak ada validasi bahwa satuan kerja yang diberikan berada dalam cakupan wewenang
	 * pengguna.
	 *
	 * @param satuanKerja satuan kerja pemilik; boleh {@code null}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan simpul pohon anggaran ({@link Workspace}) yang ditautkan ke proyek ini. Seperti
	 * {@link #getSatuanKerja()}, method ini memanggil {@link GeneralValueObject#check(Object)} untuk
	 * memaksa resolusi proxy lazy dan menuliskan hasilnya kembali ke field — jadi ia mengubah state
	 * objek, bukan sekadar membacanya. Berbeda dari {@code getSatuanKerja()}, di sini
	 * <b>tidak ada</b> pengisian otomatis: bila relasi belum diisi, nilai {@code null} dikembalikan
	 * apa adanya. {@code ProyekAction} sudah menjaga hal ini dengan memeriksa {@code null} sebelum
	 * memanggil {@code toString()} maupun {@code getHargaTotal()} pada hasilnya.
	 *
	 * @return simpul anggaran terkait, atau {@code null} bila proyek tidak ditautkan ke simpul mana
	 *         pun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace", nullable = true)
	public Workspace getWorkspace() {
		workspace = check(workspace);
		return workspace;
	}

	/**
	 * Menyetel simpul pohon anggaran yang ditautkan ke proyek ini. Tidak ada validasi bahwa simpul
	 * yang diberikan berada pada satuan kerja yang sama dengan {@link #getSatuanKerja()}, sehingga
	 * penautan lintas satker secara teknis dimungkinkan.
	 *
	 * @param workspace simpul anggaran; boleh {@code null}.
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

}
