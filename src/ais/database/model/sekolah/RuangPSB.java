package ais.database.model.sekolah;

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

import ais.database.model.Gedung;
import ais.database.model.GeneralValueObject;

/**
 * Entity master <b>ruang ujian seleksi PSB</b> (Penerimaan Siswa Baru): satu baris mewakili satu
 * ruangan fisik tempat calon siswa mengerjakan tes seleksi, lengkap dengan kapasitas kursinya.
 * Dipetakan ke tabel {@code sekolah.ruang_psb} dan diaudit penuh oleh Hibernate Envers
 * ({@link Audited}), sehingga setiap perubahan kapasitas/status tersimpan permanen di tabel
 * revisi dan dapat ditelusuri lewat tombol "Revisi" pada grid.
 *
 * <h2>Peran dalam alur PSB (terverifikasi dari kode pemanggil)</h2>
 * <p>Ruang ujian adalah <b>unit kuota</b> pada alur pendaftaran calon siswa, bukan sekadar
 * katalog ruangan. Rantai penggunaannya:</p>
 * <ol>
 *   <li>Admin membuat gelombang pendaftaran ({@link GelombangPendaftaranPsb}) lalu jadwal ujian
 *       ({@link UjianPSB}) di bawahnya.</li>
 *   <li>Untuk tiap jadwal ujian dibuat sejumlah {@code RuangPSB} lewat layar master
 *       {@code /pages/psb/ruang_psb.zul} ({@code ais.action.master.sekolah.RuangPSBAction}).
 *       Layar yang sama juga dibuka sebagai sub-tab dari layar gelombang
 *       ({@code GelombangPendaftaranPsbAction}) dengan parameter
 *       {@code ?gelombangPendaftaranPsb=<id>} yang mengunci filter gelombang.</li>
 *   <li>Saat calon siswa mendaftar dan nomor ujiannya dibangkitkan, mesin alokasi memilih
 *       <b>ruang dengan id terkecil yang masih {@code penuh = 0}</b> pada gelombang tersebut,
 *       lalu menuliskan pasangan (ruang, calon siswa) ke entity relasi
 *       {@link RuangGelombangPendaftaranPsbPSB} (tabel {@code sekolah.ruang_gelombang_psb}).
 *       Tiga tempat menjalankan pemilihan yang sama:
 *       {@code ais.common.CommonPSB.dapatkanRuangUjian(...)} (jalur pendaftaran online),
 *       {@code ais.action.master.sekolah.psb.noujian.DefaultNoUjianGeneratorPsb.generateNoUjian(...)}
 *       (jalur pembangkit nomor ujian), dan pemeriksaan kuota di muka pada
 *       {@code ais.action.master.sekolah.CalonSiswaAction}.</li>
 *   <li>Setelah alokasi terisi, ruang menjadi satuan cetak: absensi ujian, berita acara,
 *       lembar verifikasi, dan album foto peserta — semuanya lewat
 *       {@code ais.action.master.sekolah.psb.CommonReportPsb} dengan parameter {@code ruang}
 *       berisi {@link #getId()}.</li>
 * </ol>
 *
 * <h2>Ruang "Online" yang dibuat otomatis</h2>
 * <p>Non-obvious: instalasi dengan pendaftaran online tidak wajib punya ruangan fisik.
 * {@code GelombangPendaftaranPsb.chekKuotaPendaftar()} menyemai otomatis satu {@link UjianPSB}
 * bernama {@code "Online"} dan satu {@code RuangPSB} bernama/berkode {@code "Online"} dengan
 * {@link #setKapasitasRuangan(Integer) kapasitas 10000} bila gelombang tersebut belum punya
 * ruang sama sekali. Artinya baris {@code RuangPSB} bisa muncul di layar master tanpa pernah
 * diketik admin, dan pada jalur online praktis tidak pernah terjadi "kuota habis".</p>
 *
 * <h2>Semantik kapasitas dan flag {@code penuh} — tiga ambang berbeda</h2>
 * <p>{@link #getPenuh()} adalah {@link Integer} 0/1 (bukan {@code Boolean}) dan berfungsi sebagai
 * <b>penanda cepat</b> agar mesin alokasi tidak perlu menghitung ulang isi ruang. Yang perlu
 * diwaspadai: tiga pemanggil memakai ambang yang <b>tidak konsisten</b> untuk menentukan penuh —</p>
 * <ul>
 *   <li>{@code CommonPSB.dapatkanRuangUjian()}: {@code kapasitas < isi + 2} (sengaja menyisakan
 *       margin untuk menghindari <i>race</i> alokasi bersamaan);</li>
 *   <li>{@code RuangPSBAction.RuangPSBRenderer.render()}: {@code isi == kapasitas} (persis sama,
 *       sehingga isi yang melompati kapasitas tidak pernah menandai penuh);</li>
 *   <li>{@code DefaultNoUjianGeneratorPsb}/{@code CalonSiswaAction}: {@code isi < kapasitas}
 *       sebagai syarat boleh menambah.</li>
 * </ul>
 * <p>"Isi" pun dihitung dengan definisi yang berbeda: jalur alokasi menghitung calon siswa yang
 * sudah punya {@code noUjian}, sedangkan layar master menghitung yang sudah punya
 * {@code siswa.nomorInduk}. Angka "kapasitas/isi" pada grid karena itu tidak selalu sama dengan
 * angka yang dipakai mesin alokasi.</p>
 *
 * <h2>Redundansi {@code gelombangPendaftaranPsb} vs {@code ujianPSB}</h2>
 * <p>Entity ini menyimpan gelombang <b>dua kali</b>: langsung lewat
 * {@link #getGelombangPendaftaranPsb()} dan tidak langsung lewat
 * {@code getUjianPSB().getGelombangPendaftaranPsb()}. Query alokasi mensyaratkan
 * <b>keduanya</b> cocok dengan gelombang calon siswa. Konsekuensi praktis: bila admin mengubah
 * salah satu saja (mis. memindahkan ruang ke jadwal ujian gelombang lain tanpa mengubah kolom
 * gelombang), ruang tersebut <b>hilang senyap</b> dari kandidat alokasi — tetap tampil di layar
 * master, tetapi tidak pernah terpilih dan tanpa pesan kesalahan apa pun.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Atribut ruangan:</b> {@link #getKodeRuangan()}, {@link #getNama()},
 *       {@link #getKapasitasRuangan()}, {@link #getPenuh()}.</li>
 *   <li><b>Relasi:</b> {@link #getGedung()}, {@link #getGelombangPendaftaranPsb()},
 *       {@link #getUjianPSB()} — ketiganya {@code LAZY} dan diresolusi lewat
 *       {@code check()}.</li>
 *   <li><b>Nilai turunan (bukan input pengguna):</b> {@link #getTahun()},
 *       {@link #getTahunAkademik()} — disalin dari {@link UjianPSB} setiap kali dibaca.</li>
 *   <li><b>Utilitas:</b> {@link #toString()}, konstruktor {@link #RuangPSB()}.</li>
 * </ul>
 * <p>Tidak ada method query statis, tidak ada {@code compareTo}/{@code equals}/{@code hashCode}
 * (identitas memakai bawaan {@link Object}), dan tidak ada koleksi anak — seluruh relasi
 * balik dipegang {@link RuangGelombangPendaftaranPsbPSB}. Entity ini juga terdaftar di
 * {@code ais.common.InitData.initClasses(...)} sehingga barisnya dipramuat ke cache in-memory
 * yang dipakai {@code check()}.</p>
 *
 * <h2>Pola berulang yang diverifikasi pada berkas ini</h2>
 * <ul>
 *   <li><b>Getter write-back/destruktif — ADA, lima buah.</b> Tiga getter relasi menimpa
 *       field-nya dengan hasil {@code check()}; {@link #getKapasitasRuangan()} dan
 *       {@link #getPenuh()} menulis nilai default ke field; {@link #getTahun()} dan
 *       {@link #getTahunAkademik()} menimpa field dari {@link UjianPSB}. Karena Hibernate
 *       memakai akses properti (lihat di bawah), <b>nilai hasil timpaan itulah yang ikut
 *       ter-flush ke database</b>, bukan nilai yang tersimpan sebelumnya.</li>
 *   <li><b>{@code getKeterangan()} membalik kontrak — TIDAK ADA</b> (entity ini tidak punya
 *       properti keterangan).</li>
 *   <li><b>{@code compareTo()} dipangkas — TIDAK ADA</b> (tidak ada {@code compareTo} sama
 *       sekali).</li>
 *   <li><b>Penciutan {@code TreeSet} — TIDAK ADA</b>: seluruh pemanggil memuat entity ini ke
 *       {@code List}, tidak pernah ke {@code Set} berurut, sehingga risiko baris "hilang"
 *       akibat pembanding yang tidak unik tidak berlaku di sini.</li>
 *   <li><b>Cakupan tenant sekolah/yayasan — TIDAK ADA FILTER SAMA SEKALI.</b> Entity ini tidak
 *       punya kolom {@code sekolah}/{@code yayasan}; batas tenant hanya bisa ditarik lewat
 *       {@code gelombangPendaftaranPsb.sekolah}. {@code RuangPSBAction.initCriteria()} tidak
 *       memasang syarat tenant apa pun, dan combobox gelombang diisi
 *       {@code Common.insertCombo(...)} yang memang memuat seluruh baris tanpa filter. Ini
 *       varian yang lebih telanjang dari pola "dasbor sekolah tanpa scoping tenant": bukan
 *       ternary yang gagal-terbuka, melainkan memang tidak pernah ditulis.</li>
 * </ul>
 *
 * <h2>Catatan pemetaan Hibernate</h2>
 * <p>{@link Id} berada pada {@link #getId()}, sehingga mode akses seluruh entity adalah
 * <b>PROPERTY</b> — Hibernate membaca anotasi pada <i>getter</i> dan mengabaikan anotasi pada
 * setter maupun field. Karena itu {@code @Column(name = "tahun")} yang terpasang pada
 * {@link #setTahun(Integer)} sebetulnya <b>tidak berpengaruh</b>; kolomnya tetap bernama
 * {@code tahun} hanya karena kebetulan sama dengan nama default yang diturunkan dari nama
 * properti. Jangan jadikan berkas ini contoh peletakan anotasi.</p>
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, resolusi proxy lazy, dan kontrak audit dimiliki
 * {@link ais.database.model.GeneralValueObject}. Kelas ini hanya memuat state dan aturan yang
 * khas ruang ujian PSB; persistence, transaksi, dan otorisasi tetap tanggung jawab
 * Action/service pemanggil — jangan menaruh query di model.</p>
 *
 * <p><b>Catatan pengulangan field induk:</b> {@link #id}, {@link #oleh}, {@link #olehId}, dan
 * {@link #tanggal_dirubah} sengaja <b>dideklarasikan ulang</b> di sini. Ini bukan duplikasi yang
 * keliru: {@link ais.database.model.GeneralValueObject} bukan {@code @Entity} maupun
 * {@code @MappedSuperclass}, melainkan POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti apa pun miliknya. Tanpa deklarasi ulang ini, kolom {@code id}, {@code oleh},
 * {@code oleh_id}, dan {@code tanggal_dirubah} tidak akan ada pemetaannya.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see UjianPSB
 * @see GelombangPendaftaranPsb
 * @see RuangGelombangPendaftaranPsbPSB
 * @see Gedung
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "ruang_psb")
public class RuangPSB extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibekukan agar instance yang tersimpan di sesi ZK atau
	 * cache tetap dapat dibaca setelah kelas dikompilasi ulang; jangan diubah tanpa alasan kuat.
	 */
	private static final long serialVersionUID = -7550466125892447098L;

	/**
	 * Kunci primer {@code sekolah.ruang_psb.id}, dibangkitkan {@code IDENTITY} (sekuens
	 * database). Dideklarasikan ulang dari {@link ais.database.model.GeneralValueObject} karena
	 * induknya tidak dipetakan Hibernate — lihat catatan pada Javadoc kelas.
	 */
	private Long id;

	/**
	 * Nama pengguna yang terakhir mengubah baris ini. Diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan oleh formulir.
	 */
	private String oleh;

	/**
	 * Id pengguna yang terakhir mengubah baris ini, pendamping {@link #oleh}. Diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>penolakan diam-diam</b>: nilai
	 * {@code null} atau string kosong/spasi diabaikan sehingga nilai lama dipertahankan. Pola ini
	 * mencegah jejak audit terhapus oleh proses yang tidak membawa konteks pengguna (mis. tugas
	 * terjadwal atau impor), tetapi juga berarti pemanggil <b>tidak bisa mengosongkan</b> kolom
	 * ini lewat setter.
	 *
	 * @param olehId id pengguna pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penolakan diam-diam yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null} atau kosong diabaikan begitu saja.
	 *
	 * @param oleh nama pengguna pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@link javax.persistence.PreUpdate}: dijalankan Hibernate tepat sebelum
	 * pernyataan {@code UPDATE} baris ini dikirim ke database. Isinya mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang memperbarui
	 * {@link #tanggal_dirubah} serta {@link #oleh}/{@link #olehId} dari konteks pengguna yang
	 * sedang aktif. Merupakan implementasi dari method {@code abstract} milik
	 * {@link ais.database.model.GeneralValueObject}, sehingga setiap entity turunan wajib
	 * menyediakannya.
	 *
	 * <p><b>Efek samping:</b> mengubah state instance sesaat sebelum flush. Tidak pernah dipanggil
	 * langsung oleh kode aplikasi.</p>
	 *
	 * <p><b>Catatan gaya:</b> deklarasi ini dan field {@link #tanggal_dirubah} berada pada satu
	 * baris sumber yang sama (sisa pembangkitan otomatis lintas berkas model); dibiarkan apa
	 * adanya agar diff terhadap berkas sejenis tetap dapat dibandingkan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi maupun penolakan {@code null}
	 * (berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}). Praktisnya hanya
	 * dipanggil oleh {@code AuditTimestampInterceptor}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini. Field-nya diinisialisasi ke waktu
	 * <b>pembuatan object</b> memakai {@code ais.ui.util.WaktuUtil.getDate()} (zona waktu
	 * aplikasi, bukan {@code new Date()} polos), lalu diperbarui {@link #onUpdate()} pada setiap
	 * {@code UPDATE}.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada instance baru karena field
	 *         diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ruangan, dipakai sebagai label pada combobox dan disisipkan langsung ke
	 * pesan pengguna — mis. {@code "Kuota / Ruangan " + ruangSelected + " telah penuh"} pada
	 * {@code DefaultNoUjianGeneratorPsb}.
	 *
	 * <p><b>Kuirk:</b> method ini membaca <b>field</b> {@link #nama} secara langsung, bukan
	 * {@link #getNama()}. Dua akibatnya: (1) spasi di ujung nama tidak dipangkas seperti pada
	 * getter, dan (2) nilainya bisa {@code null} sehingga penggabungan string menghasilkan
	 * teks {@code "null"} di dialog pengguna. Membaca field juga berarti method ini aman
	 * dipanggil pada proxy yang belum terinisialisasi tanpa memicu pemuatan relasi.</p>
	 *
	 * @return nama ruangan apa adanya, dapat {@code null}
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Kode singkat ruangan (mis. {@code "R-01"}, atau {@code "Online"} untuk ruang virtual hasil
	 * semai otomatis). Wajib diisi pada formulir master.
	 */
	private String kodeRuangan;

	/** Nama ruangan yang ditampilkan pada grid, combobox, dan lembar cetak absensi/berita acara. */
	private String nama;

	/**
	 * Gedung tempat ruangan berada ({@code public.gedung}). Opsional — kolomnya
	 * {@code nullable = true} — meskipun formulir master mewajibkannya lewat validasi UI.
	 */
	private Gedung gedung;

	/** Jumlah kursi ujian yang tersedia; menjadi batas alokasi calon siswa ke ruang ini. */
	private Integer kapasitasRuangan;

	/**
	 * Gelombang pendaftaran pemilik ruang ini. Menjadi salah satu dari dua syarat pencocokan pada
	 * mesin alokasi (lihat Javadoc kelas, bagian redundansi gelombang).
	 */
	private GelombangPendaftaranPsb gelombangPendaftaranPsb;

	/**
	 * Penanda ruang sudah penuh: {@code 0} = masih menerima, {@code 1} = tidak dipilih lagi oleh
	 * mesin alokasi. Bertipe {@link Integer}, bukan {@code Boolean}.
	 */
	private Integer penuh;

	/** Tahun penerimaan. Nilai turunan dari {@link UjianPSB}; lihat {@link #getTahun()}. */
	private Integer tahun;

	/**
	 * Tahun akademik penerimaan (mis. {@code "2026/2027"}). Nilai turunan dari {@link UjianPSB};
	 * lihat {@link #getTahunAkademik()}.
	 */
	private String tahunAkademik;

	/**
	 * Jadwal ujian ({@code sekolah.ujian_psb}) yang memakai ruangan ini. Selain menjadi induk
	 * logis, relasi ini juga menjadi <b>sumber nilai</b> {@link #tahun} dan
	 * {@link #tahunAkademik}.
	 */
	private UjianPSB ujianPSB;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate untuk membuat instance saat memuat
	 * baris, sekaligus dipakai kode aplikasi ({@code RuangPSBAction.onAdd()} dan
	 * {@code GelombangPendaftaranPsb.chekKuotaPendaftar()}) untuk membuat ruang baru. Tidak
	 * mengisi nilai apa pun; nilai bawaan {@code kapasitasRuangan = 30} dan {@code penuh = 0}
	 * baru muncul saat getter masing-masing dipanggil.
	 */
	public RuangPSB() {
	}

	/**
	 * Mengembalikan kunci primer baris ini. Dipakai luas sebagai parameter laporan
	 * ({@code parameters.put("ruang", ruang.getId())}) dan sebagai kunci urutan pemilihan ruang —
	 * mesin alokasi selalu memilih <b>id terkecil</b> yang belum penuh, sehingga urutan pengisian
	 * ruang mengikuti urutan pembuatan datanya, bukan nama atau kodenya.
	 *
	 * <p>Kolomnya {@code insertable = false} karena nilainya dibangkitkan database
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
	 * Menyetel kunci primer. Hanya dipakai Hibernate saat memuat baris; kode aplikasi tidak boleh
	 * memanggilnya untuk entity yang sudah tersimpan.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode ruangan apa adanya (tanpa pemangkasan spasi, berbeda dari
	 * {@link #getNama()}). Kolom {@code kode_ruangan} bersifat {@code NOT NULL} sepanjang 50
	 * karakter dan divalidasi wajib isi pada formulir master.
	 *
	 * @return kode ruangan, {@code null} hanya pada entity yang belum diisi
	 */
	@Column(name = "kode_ruangan", nullable = false, length = 50)
	public String getKodeRuangan() {
		return this.kodeRuangan;
	}

	/**
	 * Menyetel kode ruangan. Tanpa validasi maupun normalisasi; keharusan isi ditegakkan di
	 * {@code RuangPSBAction.onSave()}, bukan di sini.
	 *
	 * @param kodeRuangan kode ruangan baru
	 */
	public void setKodeRuangan(String kodeRuangan) {
		this.kodeRuangan = kodeRuangan;
	}

	/**
	 * Mengembalikan nama ruangan dengan spasi ujung <b>dipangkas</b>.
	 *
	 * <p><b>Asimetri yang perlu diingat:</b> pemangkasan hanya terjadi saat membaca —
	 * {@link #setNama(String)} menyimpan nilai apa adanya, sehingga kolom di database dapat
	 * berisi spasi ujung sementara UI tampak bersih. Pencarian pada layar master memakai
	 * {@code ilike ... ANYWHERE} sehingga tidak terganggu, tetapi perbandingan string persis
	 * terhadap nilai dari database bisa meleset. Bandingkan dengan {@link #toString()} yang justru
	 * membaca field mentah.</p>
	 *
	 * @return nama ruangan yang sudah dipangkas, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama ruangan. Tanpa validasi maupun pemangkasan spasi.
	 *
	 * @param nama nama ruangan baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan gedung tempat ruangan berada, setelah proxy lazy diresolusi.
	 *
	 * <p><b>Efek samping (pola getter write-back):</b> hasil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} <b>ditulis balik</b> ke field
	 * {@link #gedung} sebelum dikembalikan. Karena entity ini memakai akses properti, object
	 * hasil resolusi itulah yang dilihat Hibernate saat flush. {@code check()} sengaja
	 * mengembalikan argumen apa adanya bila keempat sumber resolusinya gagal, sehingga method ini
	 * tetap dapat mengembalikan proxy yang belum terinisialisasi pada konteks tanpa session.</p>
	 *
	 * @return gedung terkait, atau {@code null} bila ruangan tidak dikaitkan ke gedung mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gedung", nullable = true)
	public Gedung getGedung() {
		gedung = check(gedung);
		return this.gedung;
	}

	/**
	 * Menyetel gedung tempat ruangan berada. Diisi dari combobox "Gedung" pada formulir master
	 * (daftar gedung yang {@code aktif}), dan boleh {@code null}.
	 *
	 * @param gedung gedung baru, boleh {@code null}
	 */
	public void setGedung(Gedung gedung) {
		this.gedung = gedung;
	}

	/**
	 * Menyetel jumlah kursi ujian. Nilai dari formulir master diambil dari {@code Decimalbox}
	 * lalu dikonversi ke {@link Integer}; {@code chekKuotaPendaftar()} memakai nilai
	 * {@code 10000} untuk ruang virtual "Online".
	 *
	 * @param kapasitasRuangan kapasitas baru; {@code null} akan diganti nilai bawaan saat dibaca
	 */
	public void setKapasitasRuangan(Integer kapasitasRuangan) {
		this.kapasitasRuangan = kapasitasRuangan;
	}

	/**
	 * Mengembalikan jumlah kursi ujian, dengan <b>nilai bawaan 30</b> bila belum diisi.
	 *
	 * <p><b>Efek samping (getter write-back):</b> nilai bawaan tidak sekadar dikembalikan,
	 * melainkan <b>ditulis ke field</b>. Pada entity terkelola, sekadar membaca kapasitas sebuah
	 * ruang yang kolomnya masih {@code NULL} akan membuat angka 30 ikut ter-flush ke database
	 * pada akhir transaksi — perubahan data sebagai akibat operasi baca.</p>
	 *
	 * <p>Catatan pemetaan: {@code length = 10} pada kolom numerik tidak berpengaruh; atribut itu
	 * hanya bermakna untuk tipe berbasis teks.</p>
	 *
	 * @return kapasitas ruangan; tidak pernah {@code null}
	 */
	@Column(name = "kapasitas_ruangan", length = 10, nullable = false)
	public Integer getKapasitasRuangan() {
		if (kapasitasRuangan == null) {
			kapasitasRuangan = 30;
		}
		return kapasitasRuangan;
	}

	/**
	 * Menyetel gelombang pendaftaran pemilik ruang ini.
	 *
	 * <p>Pada formulir master, combobox "Gelombang" dinonaktifkan bila ruangan sudah terisi calon
	 * siswa ({@code cekRuanganIsi(...) > 0}) agar alokasi yang sudah berjalan tidak berpindah
	 * gelombang. Perlindungan itu hanya ada di lapisan UI — setter ini sendiri tidak memeriksa
	 * apa pun.</p>
	 *
	 * @param gelombangPendaftaranPsb gelombang baru, boleh {@code null}
	 */
	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

	/**
	 * Mengembalikan gelombang pendaftaran pemilik ruang ini, setelah proxy lazy diresolusi
	 * (pola getter write-back yang sama dengan {@link #getGedung()}).
	 *
	 * <p>Selain menjadi label kolom "Gelombang" pada grid, relasi ini adalah satu-satunya jalur
	 * menuju konteks sekolah: {@code RuangPSBAction.onCetakAbsensi(...)} membaca
	 * {@code getGelombangPendaftaranPsb().getSekolah().getId()} sebagai parameter laporan —
	 * rantai yang akan melempar {@code NullPointerException} bila kolom gelombang kosong,
	 * padahal kolomnya memang boleh {@code null}.</p>
	 *
	 * @return gelombang pendaftaran terkait, atau {@code null} bila belum dikaitkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_psb")
	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
		return gelombangPendaftaranPsb;
	}

	/**
	 * Menyetel penanda ruang penuh. Nilai yang dipakai kode hanyalah {@code 0} (masih menerima)
	 * dan {@code 1} (penuh); tidak ada validasi yang menegakkannya.
	 *
	 * <p>Dipanggil dari tiga tempat: centang "Penuh" pada grid master (manual, langsung
	 * disimpan), {@code CommonPSB.dapatkanRuangUjian()} saat kapasitas hampir terlampaui, dan
	 * renderer grid saat isi ruang sama persis dengan kapasitas.</p>
	 *
	 * @param penuh {@code 0} atau {@code 1}; {@code null} akan diganti {@code 0} saat dibaca
	 */
	public void setPenuh(Integer penuh) {
		this.penuh = penuh;
	}

	/**
	 * Mengembalikan penanda ruang penuh, dengan <b>nilai bawaan {@code 0}</b> bila kolomnya masih
	 * {@code NULL}.
	 *
	 * <p><b>Efek samping (getter write-back):</b> sama seperti {@link #getKapasitasRuangan()},
	 * nilai bawaan ditulis ke field sehingga membaca properti ini pada entity terkelola dapat
	 * menghasilkan {@code UPDATE} tak terduga. Perilaku "menganggap belum penuh" ini juga berarti
	 * baris lama yang kolomnya {@code NULL} otomatis kembali menjadi kandidat alokasi.</p>
	 *
	 * @return {@code 0} bila ruang masih menerima peserta, {@code 1} bila sudah penuh; tidak
	 *         pernah {@code null}
	 */
	@Column(name = "penuh")
	public Integer getPenuh() {
		if (penuh == null) {
			penuh = 0;
		}
		return penuh;
	}

	/**
	 * Mengembalikan tahun penerimaan ruang ini, ditampilkan pada kolom "Tahun" grid master
	 * (kolom ini dapat diurutkan lewat {@code sort="auto(tahun)"} sehingga pengurutannya memakai
	 * nilai <i>kolom database</i>, bukan nilai turunan di bawah ini).
	 *
	 * <p><b>Nilai turunan, bukan input pengguna.</b> Formulir master tidak punya isian tahun;
	 * nilainya selalu disalin dari {@code ujianPSB.getTahun()} setiap kali getter dipanggil, lalu
	 * <b>ditulis balik</b> ke field {@link #tahun}. Karena Hibernate memakai getter ini saat
	 * flush, kolom {@code tahun} di database praktis selalu tersinkron dengan jadwal ujian —
	 * termasuk menimpa nilai yang pernah disetel manual lewat {@link #setTahun(Integer)}.</p>
	 *
	 * <p><b>Asimetri terhadap {@link #getTahunAkademik()}:</b> method ini membaca <b>field</b>
	 * {@code ujianPSB} secara langsung tanpa memanggil {@link #getUjianPSB()}, sehingga proxy
	 * lazy tidak diresolusi lebih dulu lewat {@code check()}. Akibatnya pada konteks tanpa session
	 * aktif, {@code ujianPSB.getTahun()} dapat melempar
	 * {@code org.hibernate.LazyInitializationException}, sementara {@link #getTahunAkademik()}
	 * yang memanggil {@code getUjianPSB()} lebih dulu justru selamat. Perbedaan dua baris ini
	 * murni ketidakkonsistenan penulisan, bukan keputusan desain.</p>
	 *
	 * @return tahun penerimaan; {@code null} bila ruang belum dikaitkan ke jadwal ujian dan
	 *         kolomnya memang kosong
	 */
	public Integer getTahun() {
		if (ujianPSB != null) {
			tahun = ujianPSB.getTahun();
		}
		return tahun;
	}

	/**
	 * Menyetel tahun penerimaan secara manual.
	 *
	 * <p><b>Praktis tidak berguna:</b> nilai apa pun yang disetel di sini akan ditimpa
	 * {@link #getTahun()} pada pembacaan berikutnya selama {@link #ujianPSB} tidak {@code null}.
	 * Tidak ada pemanggil di luar Hibernate.</p>
	 *
	 * <p><b>Catatan pemetaan:</b> anotasi {@code @Column(name = "tahun")} terpasang pada setter
	 * ini, padahal entity memakai akses properti (getter). Anotasi tersebut <b>diabaikan</b>
	 * Hibernate; kolomnya tetap bernama {@code tahun} semata karena sama dengan nama default
	 * turunan properti.</p>
	 *
	 * @param tahun tahun penerimaan
	 */
	@Column(name = "tahun")
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan tahun akademik penerimaan (mis. {@code "2026/2027"}), ditampilkan pada kolom
	 * "Tahun Akademik" grid master dan ikut dicetak pada laporan album PSB.
	 *
	 * <p><b>Nilai turunan, bukan input pengguna</b> — sama seperti {@link #getTahun()}: disalin
	 * dari {@code ujianPSB.getTahunAkademik()} lalu ditulis balik ke field, sehingga kolom
	 * database ikut tersinkron saat flush.</p>
	 *
	 * <p>Berbeda dari {@link #getTahun()}, method ini memanggil {@link #getUjianPSB()} lebih dulu
	 * sehingga proxy lazy diresolusi {@code check()} dan pembacaan di luar session tetap aman.
	 * Pemanggilan itu sendiri membawa efek samping getter relasi (penulisan balik field
	 * {@link #ujianPSB}), dan nilai kembaliannya sengaja diabaikan — field instance-lah yang
	 * dibaca ulang di baris berikutnya.</p>
	 *
	 * @return tahun akademik penerimaan, atau {@code null} bila ruang belum dikaitkan ke jadwal
	 *         ujian dan kolomnya kosong
	 */
	public String getTahunAkademik() {
		getUjianPSB();
		if (ujianPSB != null) {
			tahunAkademik = ujianPSB.getTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik penerimaan secara manual. Sama seperti {@link #setTahun(Integer)},
	 * nilainya akan ditimpa {@link #getTahunAkademik()} pada pembacaan berikutnya selama
	 * {@link #ujianPSB} tidak {@code null}.
	 *
	 * @param tahunAkademik tahun akademik penerimaan
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan jadwal ujian yang memakai ruangan ini, setelah proxy lazy diresolusi (pola
	 * getter write-back yang sama dengan {@link #getGedung()}).
	 *
	 * <p>Relasi ini adalah induk logis ruang: mesin alokasi menyaring kandidat dengan
	 * {@code createAlias("ujianPSB", "ujianPSB")} lalu mensyaratkan
	 * {@code ujianPSB.gelombangPendaftaranPsb} sama dengan gelombang calon siswa. Karena
	 * {@code createAlias} menghasilkan <i>inner join</i>, ruang yang kolom {@code ujian_psb}-nya
	 * {@code NULL} <b>tidak pernah terpilih</b> oleh mesin alokasi meskipun kolomnya dipetakan
	 * {@code nullable = true} — sebuah ruang tanpa jadwal ujian efektif mati fungsi tanpa pesan
	 * apa pun.</p>
	 *
	 * @return jadwal ujian terkait, atau {@code null} bila ruangan belum dikaitkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ujian_psb", nullable = true)
	public UjianPSB getUjianPSB() {
		ujianPSB = check(ujianPSB);
		return ujianPSB;
	}

	/**
	 * Menyetel jadwal ujian pemakai ruangan ini. Diisi dari combobox "Ruang untuk ujian" pada
	 * formulir master, yang divalidasi wajib isi oleh {@code RuangPSBAction.onSave()}.
	 *
	 * <p><b>Perhatian:</b> mengubah jadwal ujian tanpa turut menyelaraskan
	 * {@link #setGelombangPendaftaranPsb(GelombangPendaftaranPsb)} akan membuat ruang ini gagal
	 * memenuhi syarat ganda mesin alokasi — lihat Javadoc kelas, bagian redundansi gelombang.</p>
	 *
	 * @param ujianPSB jadwal ujian baru, boleh {@code null}
	 */
	public void setUjianPSB(UjianPSB ujianPSB) {
		this.ujianPSB = ujianPSB;
	}

}
