package ais.database.model.employ;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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
import ais.database.model.Pegawai;

/**
 * Entity JPA/Hibernate yang memetakan tabel {@code employ.riwayat_kerja_pegawai}, mencatat
 * satu baris riwayat pekerjaan sebelumnya (di luar riwayat karir struktural AIS) milik seorang
 * {@link Pegawai} — misalnya pengalaman kerja di instansi/perusahaan lain sebelum atau di sela
 * masa kerja saat ini, sebagaimana lazim diminta pada formulir biodata kepegawaian.
 *
 * <p>Kelas ini adalah anggota pertama dari <b>klaster "riwayat pegawai"</b> di paket
 * {@code ais.database.model.employ} — sekelompok entity yang seluruhnya mencatat SATU jenis
 * riwayat kepegawaian per baris tabel dan berbagi kerangka struktural yang nyaris identik:
 * {@link RiwayatPendidikanPegawai} (pendidikan formal), {@link RiwayatPelatihanPegawai}
 * (pelatihan/diklat), {@link RiwayatOrganisasiKampusPegawai},
 * {@link RiwayatOrganisasiSekolahPegawai}, {@link RiwayatOrganisasiLainPegawai} (keanggotaan
 * organisasi), {@link RiwayatTandaJasaPegawai} (tanda jasa/penghargaan),
 * {@link RiwayatKeluarNegeriPegawai} (perjalanan luar negeri dinas),
 * {@link RiwayatKeteranganLainPegawai} (surat keterangan lain-lain), dan
 * {@link RiwayatKartuIdentitasPegawai} (kepemilikan kartu identitas). Javadoc kelas ini berlaku
 * sebagai rujukan pola bagi seluruh anggota klaster tersebut; setiap kelas lain tetap
 * didokumentasikan penuh secara independen karena masing-masing adalah entity JPA yang berdiri
 * sendiri (tabel sendiri, FK sendiri), namun field/method yang mengikuti pola yang sama akan
 * menunjuk balik ke penjelasan di sini agar tidak diulang secara harfiah.
 *
 * <h2>Pola arsitektur bersama klaster riwayat</h2>
 * <ul>
 * <li><b>Field audit shadow {@code oleh}/{@code olehId}</b> — pasangan field non-relasional
 * (tanpa {@code @Column} eksplisit, tanpa anotasi relasi) yang menyimpan jejak "siapa yang
 * terakhir mengubah baris ini" secara tekstual (nama dan id pengguna), terpisah dari mekanisme
 * audit Envers ({@code @Audited}) yang sudah aktif di kelas ini. Ini BUKAN bug — field semacam
 * ini adalah kebutuhan teknis berulang di banyak entity AIS untuk audit trail ringan yang bisa
 * ditampilkan langsung di UI tanpa query tabel audit Envers yang terpisah. Setter-nya
 * ({@link #setOleh(String)}, {@link #setOlehId(String)}) sengaja mengabaikan nilai null/kosong
 * secara diam-diam (tanpa exception) — lihat javadoc masing-masing untuk implikasinya.</li>
 * <li><b>Timestamp otomatis {@code tanggal_dirubah}</b> — diinisialisasi eager saat objek
 * dibuat lewat {@link ais.ui.util.WaktuUtil#getDate()}, lalu diperbarui otomatis setiap kali
 * Hibernate melakukan UPDATE lewat hook {@code @PreUpdate} {@link #onUpdate()}. Lihat javadoc
 * {@link #onUpdate()} dan {@link #getTanggal_dirubah()} untuk detail siklus hidupnya.</li>
 * <li><b>Fallback {@code pegawai} ke pengguna saat ini</b> — getter relasi
 * {@link #getPegawai()} tidak sekadar mengembalikan field, tetapi memuat ulang proxy lazy lewat
 * {@code check()} (lihat {@link GeneralValueObject}) dan, bila hasilnya tetap null, mencoba
 * mengisi dari {@link Common#getCurrentUser()}. Pola ini konsisten di hampir semua anggota
 * klaster — KECUALI {@link RiwayatOrganisasiKampusPegawai#getPegawai()}, yang getter-nya
 * TIDAK memiliki fallback maupun try/catch ini meski namanya sangat mirip. Selalu verifikasi
 * kode aktual sebelum berasumsi perilaku identik antar-kelas klaster ini.</li>
 * <li><b>Flag {@code status}</b> — Boolean yang di-null-guard di getter agar tidak pernah
 * mengembalikan null ke pemanggil. Di kelas ini dan mayoritas anggota klaster, default saat
 * null adalah {@code false}. PENGECUALIAN PENTING: {@link RiwayatKartuIdentitasPegawai#getStatus()}
 * men-default ke {@code true} — kebalikan dari klaster lainnya meski nama method dan tipenya
 * identik. Jangan berasumsi makna/polaritas {@code status} sama lintas kelas tanpa membaca
 * kode masing-masing.</li>
 * <li><b>{@code serialVersionUID}</b> bernilai sama persis ({@code 2463821577548439808L}) di
 * seluruh sepuluh kelas klaster ini — kemungkinan besar hasil copy-paste dari template
 * hbm2java yang sama saat generate awal, bukan dikelola per-kelas. Ini praktik yang secara
 * teknis keliru untuk kelas {@link java.io.Serializable} yang berbeda struktur, tetapi karena
 * entity-entity ini tidak diserialisasi lintas-versi/lintas-JVM secara terpisah (Hibernate saja
 * yang memakainya), risikonya rendah — dicatat di sini sebagai observasi arsitektur, bukan
 * temuan yang perlu ditindaklanjuti.</li>
 * </ul>
 *
 * <p>Relasi many-to-one wajib ke {@link Pegawai} lewat kolom {@code pegawai} (fetch LAZY,
 * cascade PERSIST+MERGE, {@code nullable = false}) adalah satu-satunya foreign key di kelas
 * ini; tidak ada field lain yang merujuk ke lookup/master table lain (berbeda dari, misalnya,
 * {@link RiwayatPendidikanPegawai} yang juga merujuk ke {@code Pendidikan}, atau
 * {@link RiwayatPelatihanPegawai} yang merujuk ke {@code JenisPelatihan}).
 *
 * <p><b>Cakupan penggunaan nyata:</b> kelas ini bukan entity yatim — dirujuk aktif oleh
 * {@code ais.action.master.employ.RiwayatKerjaPegawaiAction} dan
 * {@code ais.action.master.employ.helper.RiwayatKerjaPegawaiHelper} di lapisan action/helper
 * untuk CRUD riwayat kerja pegawai dari UI kepegawaian.
 *
 * Bank generated by hbm2java
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "riwayat_kerja_pegawai")
public class RiwayatKerjaPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kontrak {@link java.io.Serializable} yang diwarisi dari
	 * {@link GeneralValueObject}. Nilainya identik di seluruh sepuluh kelas klaster riwayat
	 * (lihat catatan di javadoc kelas) — bukan angka yang dihitung khusus untuk struktur field
	 * kelas ini, melainkan sisa template hbm2java yang di-copy-paste. Jangan mengubahnya kecuali
	 * ada kebutuhan kompatibilitas serialisasi yang jelas, karena mengubahnya sendirian (tanpa
	 * menyelaraskan kelas lain) tidak memiliki efek fungsional untuk penggunaan Hibernate biasa.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris riwayat kerja, dibangkitkan otomatis oleh database (IDENTITY). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini — field audit shadow, lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini — field audit shadow, lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna (bukan id pegawai) yang tercatat terakhir kali mengubah baris
	 * riwayat kerja ini, sebagai bagian dari field audit shadow {@code oleh}/{@code olehId}
	 * (lihat javadoc kelas). Nilai ini murni tekstual, tidak dihubungkan lewat foreign key ke
	 * tabel pengguna, sehingga tidak pernah divalidasi terhadap keberadaan pengguna tersebut di
	 * database — bisa saja merujuk pengguna yang sudah dihapus/dinonaktifkan.
	 *
	 * @return id pengguna terakhir yang mengubah, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir, dipakai oleh lapisan audit/interceptor saat baris
	 * ini disimpan atau diperbarui. Setter ini sengaja <b>diam-diam mengabaikan</b> pemanggilan
	 * dengan nilai {@code null} atau string kosong/whitespace-only ({@code trim().isEmpty()}) —
	 * dalam kasus itu method langsung {@code return} tanpa menyentuh field {@link #olehId} sama
	 * sekali, tanpa exception dan tanpa log. Konsekuensinya: sekali {@code olehId} terisi nilai
	 * valid, tidak ada cara untuk "mengosongkannya kembali" lewat setter ini — pemanggil yang
	 * bermaksud membersihkan audit trail (misalnya lewat form yang mengirim string kosong) akan
	 * menemukan nilai lama tetap bertahan tanpa indikasi kegagalan apa pun. Ini konsisten dengan
	 * perilaku {@link #setOleh(String)} di kelas ini dan seluruh anggota klaster riwayat lain,
	 * jadi kemungkinan besar merupakan keputusan desain yang disengaja (audit trail "hanya maju,
	 * tidak pernah mundur") ketimbang bug — namun tetap perlu diwaspadai oleh siapa pun yang
	 * mengandalkan setter ini untuk mereset nilai.
	 *
	 * @param olehId id pengguna pengubah; nilai null/kosong diabaikan tanpa efek.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir. Berlaku persis seperti
	 * {@link #setOlehId(String)}: nilai null atau kosong/whitespace-only diabaikan secara diam
	 * -diam tanpa mengubah field {@link #oleh}, sehingga setter ini tidak bisa dipakai untuk
	 * mengosongkan nilai yang sudah ada. Lihat javadoc {@link #setOlehId(String)} untuk
	 * penjelasan lengkap mengenai implikasi pola ini.
	 *
	 * @param oleh nama pengguna pengubah; nilai null/kosong diabaikan tanpa efek.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna (bukan id) yang tercatat terakhir kali mengubah baris riwayat
	 * kerja ini. Lihat javadoc {@link #getOlehId()} dan {@link #setOleh(String)} untuk konteks
	 * lengkap field audit shadow ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus hidup JPA {@code @PreUpdate} yang dipanggil otomatis oleh Hibernate tepat
	 * sebelum statement UPDATE dieksekusi untuk baris entity ini. Implementasinya mendelegasikan
	 * seluruh pekerjaan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)},
	 * yang bertanggung jawab menuliskan stempel waktu perubahan (dan berpotensi field audit
	 * lain) ke field {@link #tanggal_dirubah} milik entity yang sedang di-update.
	 *
	 * <p>Perlu dicatat bahwa {@link #tanggal_dirubah} SUDAH diinisialisasi eager pada saat objek
	 * Java dibuat (lihat deklarasi field di bawah, yang memanggil
	 * {@link ais.ui.util.WaktuUtil#getDate()} langsung sebagai nilai awal) — bukan hanya saat
	 * update. Artinya field ini punya dua jalur penulisan yang berbeda maknanya: (1) saat objek
	 * baru dibuat di memori (mis. lewat constructor default), nilainya adalah waktu pembuatan
	 * objek Java, yang BUKAN berarti waktu INSERT ke database (bisa berbeda jika objek dibuat
	 * lalu baru di-persist belakangan); (2) saat entity yang sudah persisten diubah dan
	 * di-flush, hook {@code onUpdate()} ini berjalan dan (lewat {@code AuditTimestampInterceptor})
	 * menuliskan ulang nilai ke waktu saat itu. Baris INSERT pertama TIDAK memicu hook ini
	 * (hanya {@code @PreUpdate}, bukan {@code @PrePersist}), sehingga nilai awal saat insert
	 * murni bergantung pada kapan objek Java dikonstruksi, bukan kapan baris benar-benar masuk
	 * ke database — celah waktu ini biasanya kecil dalam alur request web normal, tetapi bisa
	 * signifikan bila objek dibuat lalu ditahan lama sebelum disimpan (mis. proses batch atau
	 * form multi-step). Method ini dideklarasikan {@code protected} sehingga hanya dapat dipicu
	 * oleh mekanisme lifecycle JPA/Hibernate itu sendiri, bukan dipanggil langsung dari kode
	 * aplikasi lain.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengatur nilai stempel waktu perubahan terakhir secara manual. Dalam alur normal, field
	 * ini diisi otomatis lewat inisialisasi eager di deklarasi field dan lewat hook
	 * {@link #onUpdate()} — setter ini tersedia untuk kasus di mana kode pemanggil (mis. proses
	 * migrasi data atau import riwayat lama) perlu memaksa nilai tertentu, memotong jalur
	 * otomatis tersebut.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris riwayat kerja ini. Lihat javadoc
	 * {@link #onUpdate()} untuk penjelasan lengkap kapan dan bagaimana nilai ini diperbarui.
	 *
	 * @return tanggal-waktu perubahan terakhir (tipe {@code TIMESTAMP} di database).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string default entity ini, dipakai misalnya oleh komponen UI (combo box,
	 * label ringkas) yang menampilkan daftar riwayat kerja tanpa perlu memformat field lain
	 * secara eksplisit. Implementasinya hanya mengembalikan field {@link #keterangan} apa
	 * adanya — CATATAN: bila {@link #keterangan} belum diisi (null), method ini mengembalikan
	 * {@code null}, bukan string kosong atau representasi lain seperti nama tempat kerja
	 * ({@link #nama}). Pemanggil yang mengonkatenasi hasil {@code toString()} langsung ke string
	 * lain (mis. {@code "Riwayat: " + obj}) berisiko menghasilkan literal {@code "null"} pada
	 * output, dan pemanggil yang membandingkan atau menyimpan hasilnya berisiko NPE bila
	 * memanggil method String lain di atasnya tanpa null-check.
	 *
	 * @return isi field {@link #keterangan}, dapat berupa {@code null}.
	 */
	public String toString() {
		return keterangan;
	}

	/** Catatan/keterangan bebas mengenai baris riwayat kerja ini; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Pegawai pemilik baris riwayat kerja ini; lihat {@link #getPegawai()}. */
	private Pegawai pegawai;

	/** Nama tempat kerja/instansi/perusahaan pada riwayat kerja ini; lihat {@link #getNama()}. */
	private String nama;
	/** Kedudukan/jabatan pegawai di tempat kerja tersebut; lihat {@link #getKedudukan()}. */
	private String kedudukan;
	/** Tahun mulai bekerja di tempat tersebut, default tahun berjalan; lihat {@link #getTahunMulai()}. */
	private Integer tahunMulai = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
	/** Tahun selesai/berakhir bekerja di tempat tersebut, default tahun berjalan; lihat {@link #getTahunSelesai()}. */
	private Integer tahunSelesai = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
	/** Alamat tempat kerja tersebut; lihat {@link #getAlamat()}. */
	private String alamat;
	/** Nama pimpinan/atasan pegawai di tempat kerja tersebut; lihat {@link #getPimpinan()}. */
	private String pimpinan;
	/** Flag status baris riwayat kerja ini (mis. aktif/terverifikasi); default {@code false}, lihat {@link #getStatus()}. */
	private Boolean status = false;

	/**
	 * Mengembalikan {@link Pegawai} pemilik baris riwayat kerja ini, dengan dua lapis resolusi
	 * di luar sekadar membaca field: pertama, field {@link #pegawai} dijalankan lewat
	 * {@code check(pegawai)} (diwarisi dari {@link GeneralValueObject}) yang menyegarkan/
	 * meresolusi proxy lazy Hibernate bila diperlukan (lihat javadoc {@code check()} di
	 * {@link GeneralValueObject} untuk mekanismenya secara rinci — melibatkan cache, deteksi
	 * proxy detached, dan percobaan reload lewat session baru). Kedua, bila hasil {@code check()}
	 * masih {@code null}, method ini mencoba fallback dengan mengambil pegawai dari pengguna
	 * yang sedang login lewat {@link Common#getCurrentUser()}{@code .getPegawai()}.
	 *
	 * <p>Fallback tersebut dibungkus {@code try/catch} yang MENELAN seluruh exception (termasuk
	 * kemungkinan {@code NullPointerException} bila tidak ada pengguna yang login atau pengguna
	 * tersebut tidak memiliki data pegawai terkait) — exception yang tertangkap direkam lewat
	 * {@code ais.common.ErrorAuditUtil.record(e, ...)} untuk keperluan audit, TANPA dilempar
	 * ulang. Artinya bila fallback gagal, method ini tetap mengembalikan {@code null} secara
	 * senyap alih-alih memberi sinyal error yang jelas ke pemanggil — pemanggil yang tidak
	 * melakukan null-check pada hasil {@code getPegawai()} berisiko NPE di titik pemakaian
	 * berikutnya, bukan di titik kegagalan aslinya, yang mempersulit debugging.
	 *
	 * <p><b>Kapan fallback ini relevan:</b> pola ini masuk akal untuk baris riwayat kerja yang
	 * sedang dibuat lewat form self-service (pegawai mengisi biodatanya sendiri) di mana field
	 * {@code pegawai} pada objek baru belum tentu diset eksplisit oleh kode UI — getter ini
	 * menjadi jaring pengaman agar baris tetap terhubung ke pegawai yang login. Namun untuk
	 * kasus admin/HR yang mengedit riwayat kerja PEGAWAI LAIN, fallback ke pengguna saat ini
	 * justru berpotensi salah kaprah bila field {@code pegawai} tidak sengaja kosong (misalnya
	 * karena bug di lapisan pemanggil) — baris bisa "diam-diam" ditautkan ke pegawai admin yang
	 * sedang login, bukan ke pegawai yang seharusnya menjadi subjek riwayat. Ini adalah pola yang
	 * berulang di seluruh klaster riwayat pegawai KECUALI di
	 * {@link RiwayatOrganisasiKampusPegawai#getPegawai()}, yang getter-nya tidak memiliki
	 * fallback maupun try/catch ini — perbedaan yang mudah terlewat karena kedua kelas terlihat
	 * sangat mirip secara struktural.
	 *
	 * <p>Relasi dipetakan {@code @ManyToOne} dengan {@code cascade = {PERSIST, MERGE}} (baris
	 * pegawai terkait ikut di-insert/update bila belum tersimpan, tetapi TIDAK ikut terhapus
	 * bila baris riwayat ini dihapus — tidak ada {@code CascadeType.REMOVE}) dan
	 * {@code fetch = FetchType.LAZY} (pegawai baru dimuat dari database saat getter ini benar
	 * -benar dipanggil, bukan saat baris riwayat kerja dimuat), kolom join {@code pegawai}
	 * bersifat {@code nullable = false} di level skema — sehingga pada baris yang sudah
	 * tersimpan valid di database, fallback ke pengguna saat ini seharusnya jarang terpakai;
	 * fallback ini lebih relevan untuk objek yang belum di-persist.
	 *
	 * @return pegawai pemilik riwayat kerja ini, hasil dari proxy yang di-resolusi atau fallback
	 *         pengguna saat ini; dapat berupa {@code null} bila keduanya gagal.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);

		try {
			if (pegawai == null) {
				pegawai = Common.getCurrentUser().getPegawai();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/RiwayatKerjaPegawai.java:100");

		}

		return pegawai;
	}

	/**
	 * Mengisi field {@link Pegawai} pemilik baris riwayat kerja ini secara langsung, tanpa
	 * validasi apa pun (tidak seperti {@link #setOleh(String)}/{@link #setOlehId(String)} yang
	 * menolak nilai kosong). Nilai {@code null} diterima apa adanya di sini.
	 *
	 * @param pegawai pegawai pemilik baris riwayat kerja ini.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan alamat tempat kerja pada riwayat ini, dipetakan ke kolom {@code alamat_}
	 * (dengan underscore akhir untuk menghindari bentrok dengan kata kunci reserved di beberapa
	 * dialek SQL) dan bersifat {@code nullable = false} di level skema — meski demikian, tidak
	 * ada validasi non-null pada level Java/setter, sehingga kekosongan hanya akan terdeteksi
	 * saat flush ke database (constraint violation), bukan lebih awal di lapisan aplikasi.
	 *
	 * @return alamat tempat kerja.
	 */
	@Column(name = "alamat_", nullable = false)
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Mengisi alamat tempat kerja pada riwayat ini.
	 *
	 * @param alamat alamat tempat kerja baru.
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Constructor default tanpa argumen, dibutuhkan oleh spesifikasi JPA/Hibernate agar
	 * provider persistence dapat menginstansiasi entity ini secara reflektif (mis. saat memuat
	 * hasil query). Tidak melakukan inisialisasi tambahan di luar nilai default field
	 * (termasuk inisialisasi eager {@link #tahunMulai}, {@link #tahunSelesai}, dan
	 * {@link #tanggal_dirubah} yang terjadi di deklarasi field masing-masing, bukan di sini).
	 */
	public RiwayatKerjaPegawai() {
	}

	/**
	 * Mengembalikan primary key baris riwayat kerja ini. Dibangkitkan otomatis oleh database
	 * lewat strategi {@link javax.persistence.GenerationType#IDENTITY} — kolom {@code id}
	 * dipetakan {@code insertable = false} karena nilainya diserahkan sepenuhnya ke database
	 * (auto-increment), bukan diisi oleh aplikasi sebelum INSERT.
	 *
	 * @return id baris, atau {@code null} untuk entity yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id baris riwayat kerja secara manual. Karena kolom {@code id} dipetakan
	 * {@code insertable = false}, pengisian manual di sini tidak akan terbawa ke statement
	 * INSERT — setter ini berguna terutama untuk kebutuhan seperti membangun objek referensi
	 * ringan (mis. untuk operasi delete-by-id) tanpa memuat seluruh baris dari database.
	 *
	 * @param id id baris yang ingin diset.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan/keterangan bebas mengenai baris riwayat kerja ini. Kolom
	 * {@code keterangan} bersifat {@code nullable = true} sehingga field ini boleh kosong;
	 * lihat juga {@link #toString()} yang mengekspos nilai ini langsung sebagai representasi
	 * string default entity.
	 *
	 * @return isi keterangan, dapat berupa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan/keterangan bebas mengenai baris riwayat kerja ini.
	 *
	 * @param keterangan teks keterangan baru, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama tempat kerja/instansi/perusahaan pada riwayat kerja ini. Tidak ada
	 * anotasi {@code @Column} eksplisit pada getter ini, sehingga nama kolom database mengikuti
	 * strategi penamaan default Hibernate dari nama property {@code nama}.
	 *
	 * @return nama tempat kerja.
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Mengisi nama tempat kerja/instansi/perusahaan pada riwayat kerja ini.
	 *
	 * @param nama nama tempat kerja baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan kedudukan/jabatan pegawai di tempat kerja pada riwayat ini (mis. "Staf",
	 * "Kepala Bagian").
	 *
	 * @return kedudukan/jabatan di tempat kerja tersebut.
	 */
	public String getKedudukan() {
		return kedudukan;
	}

	/**
	 * Mengisi kedudukan/jabatan pegawai di tempat kerja pada riwayat ini.
	 *
	 * @param kedudukan kedudukan/jabatan baru.
	 */
	public void setKedudukan(String kedudukan) {
		this.kedudukan = kedudukan;
	}

	/**
	 * Mengembalikan tahun mulai bekerja di tempat kerja pada riwayat ini. Field ini
	 * diinisialisasi eager ke tahun kalender berjalan saat objek dikonstruksi (lihat deklarasi
	 * field {@link #tahunMulai}), sehingga objek baru yang belum diisi eksplisit oleh form akan
	 * tetap punya nilai tahun berjalan, bukan {@code null}.
	 *
	 * @return tahun mulai bekerja.
	 */
	public Integer getTahunMulai() {
		return tahunMulai;
	}

	/**
	 * Mengisi tahun mulai bekerja di tempat kerja pada riwayat ini.
	 *
	 * @param tahunMulai tahun mulai bekerja baru.
	 */
	public void setTahunMulai(Integer tahunMulai) {
		this.tahunMulai = tahunMulai;
	}

	/**
	 * Mengembalikan tahun selesai/berakhir bekerja di tempat kerja pada riwayat ini. Seperti
	 * {@link #getTahunMulai()}, field ini diinisialisasi eager ke tahun berjalan saat objek
	 * dikonstruksi.
	 *
	 * @return tahun selesai bekerja.
	 */
	public Integer getTahunSelesai() {
		return tahunSelesai;
	}

	/**
	 * Mengisi tahun selesai/berakhir bekerja di tempat kerja pada riwayat ini.
	 *
	 * @param tahunSelesai tahun selesai bekerja baru.
	 */
	public void setTahunSelesai(Integer tahunSelesai) {
		this.tahunSelesai = tahunSelesai;
	}

	/**
	 * Mengembalikan nama pimpinan/atasan pegawai di tempat kerja pada riwayat ini.
	 *
	 * @return nama pimpinan di tempat kerja tersebut.
	 */
	public String getPimpinan() {
		return pimpinan;
	}

	/**
	 * Mengisi nama pimpinan/atasan pegawai di tempat kerja pada riwayat ini.
	 *
	 * @param pimpinan nama pimpinan baru.
	 */
	public void setPimpinan(String pimpinan) {
		this.pimpinan = pimpinan;
	}

	/**
	 * Mengembalikan flag status baris riwayat kerja ini, dengan null-guard: bila field internal
	 * {@link #status} bernilai {@code null} (mis. pada baris lama yang dimuat dari database
	 * sebelum kolom ini ada, atau objek yang dikonstruksi lewat jalur yang melewati inisialisasi
	 * default field), getter ini MENULISKAN ULANG field menjadi {@code false} sebagai efek
	 * samping sebelum mengembalikannya — bukan sekadar mengembalikan nilai default tanpa
	 * mengubah state objek. Konsekuensinya, memanggil getter ini pada entity yang sedang
	 * dikelola Hibernate (managed, dalam transaksi) berpotensi menandai entity tersebut sebagai
	 * "dirty" (untuk kombinasi dirty-checking tertentu) hanya karena getter dipanggil, meski
	 * secara logis tidak ada perubahan data yang dimaksud oleh pemanggil.
	 *
	 * <p>Default {@code false} di kelas ini SAMA dengan mayoritas anggota klaster riwayat
	 * pegawai lain, tetapi BERBEDA dengan {@link RiwayatKartuIdentitasPegawai#getStatus()} yang
	 * men-default ke {@code true}. Karena nama method, tipe, dan pola null-guard-nya identik,
	 * kekeliruan berasumsi "status selalu default false di seluruh klaster riwayat" sangat
	 * mudah terjadi — selalu verifikasi kelas spesifik yang sedang dipakai.
	 *
	 * @return status baris riwayat kerja ini, tidak pernah {@code null}.
	 */
	public Boolean getStatus() {
		if (status == null) {
			status = false;
		}
		return status;
	}

	/**
	 * Mengisi flag status baris riwayat kerja ini secara langsung, tanpa null-guard (berbeda
	 * dari {@link #getStatus()}) — memanggil setter ini dengan {@code null} akan menyimpan
	 * {@code null} apa adanya ke field, dan null-guard baru berlaku lagi saat {@link #getStatus()}
	 * dipanggil berikutnya.
	 *
	 * @param status status baru.
	 */
	public void setStatus(Boolean status) {
		this.status = status;
	}

}
