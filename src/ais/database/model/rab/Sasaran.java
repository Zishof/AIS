package ais.database.model.rab;

// Generated Dec 19, 2009 10:58:09 PM by Hibernate Tools 3.2.4.CR1

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.library.Perpustakaan;



/**
 * Entitas master <b>Sasaran</b> pada modul RAB/perencanaan anggaran, dipetakan ke tabel
 * {@code rab.sasaran}. Dalam kerangka perencanaan strategis (RENSTRA/RKA-KL), <i>sasaran</i> adalah
 * kondisi yang ingin dicapai oleh sebuah program atau kegiatan — misalnya "Meningkatnya kualitas
 * lulusan" atau "Terselenggaranya tata kelola yang akuntabel". Entitas ini hanya menyimpan
 * <b>nomenklatur</b> sasaran ({@code kode}, {@code nama}, {@code keterangan}); angka target maupun
 * capaiannya sepenuhnya berada pada entitas penghubung yang merujuk ke sini.
 *
 * <h2>Hubungan kembar dengan {@link Indikator}</h2>
 * <p>{@code Sasaran} dan {@link Indikator} adalah sepasang entitas yang <b>identik strukturnya</b>
 * — field, anotasi, dan bahkan bentuk {@link #getSatuanKerja()}-nya sama persis, hanya berbeda nama
 * kelas dan nama tabel. Keduanya berperan sebagai dua sisi rumusan kinerja: {@code Sasaran}
 * menyatakan <i>apa yang ingin dicapai</i>, {@code Indikator} menyatakan <i>bagaimana pencapaian
 * itu diukur</i>. Keduanya juga dipasangkan pada tempat yang sama, yaitu simpul {@link Workspace}
 * lewat {@link WorkspacePunyaSasaran} dan {@link WorkspacePunyaIndikator}. Setiap perubahan pada
 * salah satu berkas hampir selalu perlu dicerminkan pada berkas kembarannya.</p>
 *
 * <h2>PERINGATAN — jangan tertukar dengan {@code RenstraProgram.sasaran}</h2>
 * <p>{@link RenstraProgram} memiliki properti bernama {@code sasaran} juga
 * ({@link RenstraProgram#getSasaran()}), tetapi itu adalah <b>kolom teks bebas bertipe
 * {@code String}</b>, <b>bukan</b> foreign key ke entitas ini. Verifikasi atas basis kode
 * menegaskan {@code RenstraProgramAction} tidak pernah mengimpor
 * {@code ais.database.model.rab.Sasaran}. Artinya sasaran yang ditulis pada dokumen RENSTRA program
 * tidak terhubung ke katalog ini dan tidak dapat direkonsiliasi dengannya tanpa pencocokan teks.
 * Kesamaan nama ini jebakan yang mudah terlewat karena keduanya berada di paket yang sama.</p>
 *
 * <h2>Siapa yang merujuk entitas ini</h2>
 * <p>Verifikasi atas basis kode menemukan dua pemegang foreign key, dan salah satunya berada di
 * luar modul RAB:</p>
 * <ul>
 *   <li>{@link WorkspacePunyaSasaran} — tabel penghubung banyak-ke-banyak antara simpul
 *   {@link Workspace} (pohon program/kegiatan beranggaran) dan sasaran. Ini pemakaian utamanya.</li>
 *   <li>{@code ais.database.model.lkp.KegiatanTugasJabatanPunyaSasaran} — <b>lintas modul</b>:
 *   modul LKP (kinerja/tugas jabatan pegawai) memakai ulang katalog sasaran RAB yang sama, bukan
 *   membuat katalog sendiri, persis seperti yang dilakukannya terhadap {@link Indikator}.
 *   Konsekuensinya, menghapus atau mengubah entri di sini juga berdampak pada penilaian kinerja
 *   pegawai — jangan asumsikan katalog ini hanya melayani modul RAB.</li>
 * </ul>
 * <p>Perhatikan bahwa {@link Tor} merujuk {@link Indikator} tetapi <b>tidak</b> merujuk
 * {@code Sasaran} — asimetri yang membedakan pasangan kembar ini dalam praktik.
 * {@code ais.action.master.rab.SasaranAction} beserta pembantu {@code AmbilDataSasaranBanbox},
 * {@code AmbilDataSasaranBanyak}, dan {@code WorkspacePunyaSasaranHelper} menyediakan layar CRUD
 * dan komponen pemilihan.</p>
 *
 * <h2>Pembatasan tenant — ada, tetapi jatuh terbuka bila cakupan kosong</h2>
 * <p>Entitas ini membawa relasi {@link SatuanKerja} dan {@code SasaranAction} menyaringnya dengan
 * bentuk yang relatif kuat: himpunan satuan kerja dalam cakupan pengguna dihitung lebih dulu, lalu
 * kriteria menjadi {@code Restrictions.in("satuanKerja", satuanKerjas)}, digabung {@code OR} dengan
 * {@code isNull("satuanKerja")} ketika tidak ada satker induk yang dipilih (agar entri "global"
 * tanpa pemilik ikut terlihat). Namun bila himpunan cakupan tersebut <b>kosong</b>, seluruh kriteria
 * diganti {@code Restrictions.sqlRestriction("1=1")} — artinya pengguna melihat seluruh sasaran
 * lintas satuan kerja. Perilaku <i>fail-open</i> ini identik dengan yang ada pada
 * {@code IndikatorAction} dan merupakan pola berulang yang sudah tercatat pada inisiatif
 * dokumentasi ini.</p>
 *
 * <h2>Pemetaan ORM</h2>
 * <p>Entitas memakai {@code dynamicInsert}/{@code dynamicUpdate} sehingga Hibernate hanya menulis
 * kolom yang benar-benar berubah, dan diberi {@link Audited} sehingga Hibernate Envers merekam
 * setiap revisi ke tabel bayangan pada skema {@code rab}. Kunci utama memakai strategi
 * {@link javax.persistence.GenerationType#IDENTITY}. Kolom {@code nama} dan {@code keterangan}
 * dipetakan sebagai {@code text}, sementara {@code kode} memakai pemetaan bawaan. Relasi
 * {@code satuanKerja} tidak diberi {@code fetch = LAZY} sehingga mengikuti bawaan
 * {@link ManyToOne} (yaitu <i>eager</i>), dengan {@link FetchMode#SELECT} agar Hibernate memakai
 * kueri terpisah alih-alih {@code JOIN}.</p>
 *
 * <h2>Catatan gaya kode</h2>
 * <p>Berkas asli memampatkan deklarasi field {@code oleh}/{@code olehId} beserta accessor-nya ke
 * dalam satu baris, dan menempelkan deklarasi field {@code tanggal_dirubah} di belakang method
 * {@link #onUpdate()}. Pada berkas ini deklarasi tersebut dipisah baris agar setiap anggota dapat
 * diberi dokumentasi, tanpa mengubah semantik apa pun.</p>
 *
 * @see Indikator
 * @see WorkspacePunyaSasaran
 * @see RenstraProgram#getSasaran()
 * @see ais.database.dao.rab.SasaranDao
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "sasaran")



public class Sasaran extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja disamakan dengan hampir seluruh entitas lain
	 * di paket {@code ais.database.model.rab} (hasil salin-tempel templat hbm2java), sehingga
	 * <b>tidak</b> bisa dipakai untuk membedakan tipe saat deserialisasi.
	 */
	private static final long serialVersionUID = -8738027816264807168L;

	/**
	 * Kunci utama basis data, dibangkitkan oleh kolom identity pada {@code rab.sasaran.id}.
	 * Bernilai {@code null} selama objek belum pernah disimpan — kondisi ini juga dipakai
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
	 * oleh proses penyalinan objek atau pengikatan form yang mengirim nilai kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau kosong diabaikan sehingga jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	 * Kode/nomenklatur sasaran. Diisi bebas oleh operator; tidak dibangkitkan otomatis dan tidak
	 * dijamin unik.
	 */
	private String kode;

	/**
	 * Rumusan sasaran, misalnya "Meningkatnya kualitas lulusan". Dipetakan sebagai kolom
	 * {@code text} sehingga rumusan panjang tertampung utuh.
	 */
	private String nama;

	/**
	 * Keterangan tambahan atas sasaran, mis. penjelasan konteks atau acuan dokumen perencanaan.
	 * Dipetakan sebagai kolom {@code text}.
	 */
	private String keterangan;

	/**
	 * Satuan kerja pemilik entri sasaran. Berfungsi sebagai penanda tenant dan diisi otomatis untuk
	 * objek baru oleh {@link #getSatuanKerja()}. Boleh {@code null} — baris dengan nilai
	 * {@code null} diperlakukan {@code SasaranAction} sebagai entri global yang terlihat oleh semua
	 * satuan kerja.
	 */
	private SatuanKerja satuanKerja;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Juga dipakai {@code SasaranAction} saat menekan tombol tambah data — objek hasil konstruktor
	 * ini punya {@code id} bernilai {@code null}, yang mengaktifkan pengisian otomatis satuan kerja
	 * pada {@link #getSatuanKerja()}.
	 */
	public Sasaran() {
	}

	/**
	 * Konstruktor pintas yang langsung mengisi {@link #nama}, disediakan oleh generator hbm2java.
	 *
	 * @param nama rumusan sasaran.
	 */
	public Sasaran(String nama) {
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
	 * Mengembalikan rumusan sasaran dengan spasi di ujung sudah dipangkas. Pemangkasan dilakukan
	 * pada getter (bukan setter), sehingga nilai di memori bisa saja masih mengandung spasi
	 * sementara nilai yang ditulis Hibernate ke basis data sudah terpangkas.
	 *
	 * @return rumusan sasaran yang sudah dipangkas, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel rumusan sasaran apa adanya, tanpa pemangkasan maupun pemeriksaan duplikasi.
	 *
	 * @param nama rumusan sasaran.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan tambahan atas sasaran. Berbeda dari {@link #getNama()}, nilai
	 * dikembalikan apa adanya tanpa pemangkasan spasi.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan tambahan atas sasaran.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kode/nomenklatur sasaran apa adanya, tanpa pemangkasan spasi dan tanpa nilai
	 * bawaan bila kosong.
	 *
	 * @return kode sasaran, atau {@code null} bila belum diisi.
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode/nomenklatur sasaran. Tidak ada validasi format maupun pemeriksaan keunikan.
	 *
	 * @param kode kode sasaran.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan satuan kerja pemilik entri sasaran ini. Method ini <b>bukan getter murni</b>:
	 * untuk objek yang belum tersimpan, ia mengisi sendiri relasi tenant dari konteks pengguna yang
	 * sedang login. Isinya sama persis dengan {@link Indikator#getSatuanKerja()}, sesuai sifat
	 * kembar kedua entitas.
	 *
	 * <h3>Kapan pengisian otomatis berjalan</h3>
	 * <p>Blok pengisian hanya dijalankan bila <b>kedua</b> syarat terpenuhi: field
	 * {@link #satuanKerja} masih {@code null} <b>dan</b> {@link #id} juga {@code null}. Syarat kedua
	 * itulah penjaganya — ia memastikan baris yang sudah ada di basis data dengan kolom
	 * {@code satuan_kerja} bernilai NULL tidak diam-diam dipindahkan menjadi milik satuan kerja
	 * pengguna yang kebetulan membacanya. Tanpa penjaga tersebut, membuka layar daftar saja sudah
	 * cukup untuk mengubah kepemilikan data lama, karena Hibernate membaca getter ini saat
	 * <i>dirty checking</i> dan akan menuliskan nilai barunya pada {@code flush} berikutnya.
	 * Bandingkan dengan {@link RenstraProgram#getSatuanKerja()} yang justru tidak memasang penjaga
	 * ini.</p>
	 *
	 * <h3>Urutan sumber nilai</h3>
	 * <ol>
	 *   <li>{@code Common.getCurrentUser().ambilSatuanKerja()} — satuan kerja yang melekat pada
	 *   pengguna aktif; ini jalur utama.</li>
	 *   <li>Bila jalur pertama menghasilkan {@code null}, dicoba
	 *   {@code Common.getCurrentPerpustakaan().getSatuanKerja()} — satuan kerja milik konteks
	 *   perpustakaan aktif. Jalur cadangan ini melayani sesi yang berjalan dalam konteks modul
	 *   perpustakaan, di mana identitas pengguna tidak selalu terikat langsung ke satuan kerja.
	 *   Perhatikan bahwa {@code currentPerpustakaan} diambil <i>sebelum</i> pemeriksaan {@code null}
	 *   dilakukan, sehingga kueri konteks perpustakaan selalu berjalan meski jalur pertama sudah
	 *   berhasil — biaya kecil yang sifatnya artefak penulisan, bukan kebutuhan.</li>
	 * </ol>
	 *
	 * <h3>Penanganan galat bersifat fail-open</h3>
	 * <p>Seluruh blok dibungkus {@code try/catch(Exception)} yang tidak melemparkan ulang, melainkan
	 * hanya mencatat kejadian lewat {@code ais.common.ErrorAuditUtil.record(...)}. Ini disengaja:
	 * getter entitas dipanggil dari banyak konteks tanpa sesi pengguna (proses batch, penjadwal,
	 * uji, rekonstruksi revisi oleh Envers) di mana {@code Common.getCurrentUser()} wajar gagal, dan
	 * getter tidak boleh menggagalkan proses hanya karena itu. Konsekuensinya, pada konteks tanpa
	 * pengguna sasaran baru tersimpan dengan {@code satuan_kerja} bernilai NULL — yang oleh
	 * {@code SasaranAction} justru diperlakukan sebagai entri global dan <b>terlihat oleh semua
	 * satuan kerja</b> (kriteria pencariannya menggabungkan {@code isNull("satuanKerja")} dengan
	 * {@code OR}). Jadi kegagalan penentuan pemilik berujung pada data yang lebih terbuka, bukan
	 * lebih tertutup.</p>
	 *
	 * <h3>Tidak ada resolusi proxy</h3>
	 * <p>Berbeda dari {@link Proyek#getSatuanKerja()} dan {@link Tor#getSatuanKerja()}, method ini
	 * tidak memanggil {@link GeneralValueObject#check(Object)}. Itu konsisten dengan pemetaannya:
	 * relasi di sini tidak dinyatakan {@code LAZY}, sehingga mengikuti bawaan {@link ManyToOne}
	 * (eager) dan nilainya sudah berupa objek nyata, bukan proxy yang perlu dipaksa terinisialisasi.
	 * Jangan menambahkan {@code check(...)} tanpa sekaligus mengubah strategi fetch-nya.</p>
	 *
	 * @return satuan kerja pemilik sasaran, atau {@code null} bila tidak dapat ditentukan (entri
	 *         diperlakukan sebagai global).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (this.satuanKerja == null && this.id == null) {
			try {
				SatuanKerja satuanKerja = Common.getCurrentUser()
						.ambilSatuanKerja();
				Perpustakaan currentPerpustakaan = Common
						.getCurrentPerpustakaan();
				if (satuanKerja == null && currentPerpustakaan != null) {
					satuanKerja = currentPerpustakaan.getSatuanKerja();
				}
				this.satuanKerja = satuanKerja;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/Sasaran.java:138");
			}
		}
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik entri sasaran secara eksplisit. Pengisian manual ini mengalahkan
	 * pengisian otomatis pada {@link #getSatuanKerja()}, karena getter hanya bertindak ketika field
	 * masih {@code null}. Tidak ada validasi bahwa satuan kerja yang diberikan berada dalam cakupan
	 * wewenang pengguna.
	 *
	 * @param satuanKerja satuan kerja pemilik; {@code null} menjadikan entri berlaku global.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
