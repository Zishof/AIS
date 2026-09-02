package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ais.common.Common;

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

import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;

/**
 * Entity master <b>jurusan / program studi</b> (tabel {@code public.jurusan}) — satuan akademik
 * terkecil yang menyelenggarakan kurikulum di AIS.
 *
 * <h2>Posisi dalam hierarki institusi</h2>
 *
 * <p>Rantai organisasi AIS berbentuk
 * {@link PerguruanTinggi} &rarr; {@link Fakultas} &rarr; <b>{@code Jurusan}</b>. Relasi ke
 * {@link Fakultas} adalah satu-satunya relasi yang dipetakan {@code nullable = false}, jadi setiap
 * baris jurusan wajib menempel pada sebuah fakultas; alamat surat-menyurat pun diturunkan dari
 * perguruan tinggi induk lewat fakultas (lihat {@link #getAlamat()}). Di samping itu ada
 * pengelompokan opsional lewat {@link GrupJurusan} (mis. "rumpun ilmu"), penjenjangan lewat
 * {@link Jenjang} (S1/S2/D3/...) dan {@link JenjangProgramStudi}, serta pemetaan ke unit anggaran
 * lewat {@link ais.database.model.rab.SatuanKerja}.</p>
 *
 * <h2>Peran sebagai rujukan lintas modul</h2>
 *
 * <p>{@code Jurusan} adalah salah satu entity paling banyak dirujuk di seluruh basis kode: lebih
 * dari <b>130 kelas entity lain</b> di paket {@code ais.database.model} punya field
 * {@code private Jurusan ...} — antara lain {@link Mahasiswa}, {@link Dosen}, {@link Perkuliahan},
 * kurikulum, KRS, tagihan/biaya, wisuda, PMB/PSB, perpustakaan, dan pelaporan PDDIKTI. Akibatnya:</p>
 *
 * <ul>
 *   <li>Hampir semua layar daftar dan laporan menyaring datanya "per prodi" memakai id jurusan.</li>
 *   <li>Hak akses operator kerap dibatasi per fakultas/jurusan, sehingga perubahan pada relasi
 *   {@link #getFakultas()} berdampak ke visibilitas data, bukan sekadar tampilan.</li>
 *   <li>Perubahan perilaku getter di kelas ini terasa di sangat banyak layar sekaligus. Perlakukan
 *   setiap suntingan sebagai perubahan berdampak luas.</li>
 * </ul>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ol>
 *   <li><b>Identitas &amp; kode.</b> {@link #getId()}, {@link #getKode()} (kode internal),
 *   {@link #getKodeEpsbed()} (kode PDPT/EPSBED lama), {@link #getFeeder()} (UUID prodi di Feeder
 *   PDDIKTI), {@link #getKodeLain()} (kode bebas untuk kebutuhan lokal).</li>
 *   <li><b>Penamaan &amp; gelar.</b> {@link #getNama()}/{@link #getNamaEn()},
 *   {@link #getGelar()}/{@link #getSingkatanGelar()} beserta pasangan berbahasa Inggris
 *   {@link #getGelarEn()}/{@link #getSingkatanGelarEn()} — dipakai pencetakan ijazah, transkrip,
 *   dan album wisuda dwibahasa.</li>
 *   <li><b>Struktur &amp; pejabat.</b> {@link #getFakultas()}, {@link #getJenjang()},
 *   {@link #getJenjangProgramStudi()}, {@link #getGrupJurusan()}, {@link #getKaprodi()}, tiga
 *   slot pejabat bebas {@link #getPegawai1()}..{@link #getPegawai3()} dengan label yang bisa
 *   diganti pengguna ({@link #getLabelPejabat1()}..{@link #getLabelPejabat3()}), dan
 *   {@link #getSatuanKerja()}.</li>
 *   <li><b>Akreditasi — dua set terpisah.</b> Set pertama untuk BAN-PT
 *   ({@link #getStatusAkreditasi()}, {@link #getPeringkatAkreditasi()}, {@link #getAkreditasi()},
 *   {@link #getNoSkAkreditasi()}, {@link #getTanggalAkreditasi()}); set kedua berakhiran
 *   {@code Kes} untuk LAM-PTKes, lembaga akreditasi mandiri rumpun kesehatan
 *   ({@link #getStatusAkreditasiKes()} dst.). Keduanya diisi berdampingan pada layar yang sama dan
 *   tidak saling menggantikan.</li>
 *   <li><b>Atribut operasional.</b> {@link #getAktif()}, {@link #getJenisJurusan()} (Eksakta /
 *   Sosial), {@link #getBahasaPengantar()}, {@link #getRasioDosenDanMahasiswa()},
 *   {@link #getWa()}, {@link #getAlamat()}/{@link #getAlamat2()},
 *   {@link #getDosenHarusPakaiSatuanKerja()}, {@link #getDeskripsi()}, {@link #getProfil()}.</li>
 *   <li><b>Audit.</b> {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()} dan
 *   kait {@link #onUpdate()}; seluruh perubahan baris juga direkam Hibernate Envers karena kelas
 *   ini beranotasi {@link org.hibernate.envers.Audited}.</li>
 *   <li><b>Lampiran cetak.</b> {@link #putFile(java.util.Map)} — satu-satunya method non-accessor
 *   di kelas ini.</li>
 * </ol>
 *
 * <h2>Hal-hal non-obvious yang wajib diketahui sebelum menyunting</h2>
 *
 * <p><b>1. Pemetaan memakai akses property.</b> Anotasi JPA ({@link javax.persistence.Id},
 * {@link javax.persistence.Column}, {@link javax.persistence.ManyToOne}) dipasang pada
 * <i>getter</i>, sehingga Hibernate membaca dan menulis kolom lewat getter, bukan lewat field.
 * Semua "normalisasi" di dalam getter di bawah karena itu bukan sekadar kosmetik tampilan — nilai
 * itulah yang dipakai saat {@code INSERT} dan saat entity <i>detached</i> disimpan ulang.</p>
 *
 * <p><b>2. Getter yang menulis balik ke field ("normalisasi diam-diam").</b>
 * {@link #getNama()}, {@link #getKode()}, {@link #getDeskripsi()}, {@link #getProfil()},
 * {@link #getAlamat()}, dan {@link #getAlamat2()} <b>mengubah state object</b> saat dipanggil.
 * Yang paling mencolok: {@link #getKode()} menyulih kode kosong menjadi {@code "--"},
 * {@link #getProfil()} menyulih profil kosong dengan paragraf boilerplate sepanjang ~1.000
 * karakter, dan {@link #getAlamat()} menyalin alamat {@link PerguruanTinggi} ke field jurusan.
 * Untuk baris yang <b>baru</b> disimpan ({@code dynamicInsert = true} hanya melewatkan nilai
 * {@code null}), nilai sulihan itu ikut tertulis ke database. Berbeda dengan pola yang ditemukan
 * di entity biodata, tidak ada getter di kelas ini yang menjalankan {@code save}/{@code insert}
 * sendiri ke tabel master lain.</p>
 *
 * <p><b>3. Getter yang bisa memicu query.</b> {@link #getAlamat()} dan {@link #getAlamat2()}
 * memanggil {@link #getFakultas()} &rarr; {@link GeneralValueObject#check(Object)}, yang boleh
 * membuka session Hibernate sendiri untuk meresolusi proxy lazy. Jadi getter "alamat" yang tampak
 * sepele dapat menyentuh database. {@code check()} tidak menutup session milik pemanggil.</p>
 *
 * <p><b>4. Field bayangan.</b> {@code id}, {@code nama}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>dideklarasikan ulang</b> di kelas ini padahal sudah ada di
 * {@link GeneralValueObject}. Hal ini hanya aman karena getter/setter pasangannya ikut
 * di-override, sehingga seluruh kode yang mengakses lewat method tetap melihat nilai milik
 * {@code Jurusan}; salinan milik kelas induk selamanya {@code null}. Jangan menambahkan kode di
 * {@link GeneralValueObject} yang membaca field-field itu <i>langsung</i> (tanpa getter), karena
 * untuk {@code Jurusan} kode semacam itu akan membaca nilai kosong.</p>
 *
 * <p><b>5. Nilai default yang tidak pernah {@code null}.</b> Sebagian besar getter di kelas ini
 * mengembalikan nilai pengganti saat field kosong ({@link #getAktif()} &rarr; {@code true},
 * {@link #getStatusAkreditasi()} &rarr; "Tidak Terakreditasi / Kadaluarsa",
 * {@link #getBahasaPengantar()} &rarr; "Bahasa Indonesia",
 * {@link #getRasioDosenDanMahasiswa()} &rarr; {@code 0.4}, label pejabat &rarr; "Pejabat I/II/III",
 * {@link #getWa()} &rarr; {@code ""}). Konsekuensinya pemanggil <b>tidak bisa membedakan</b>
 * "belum pernah diisi" dari "diisi persis sama dengan default"; kalau perbedaan itu penting,
 * bacalah kolomnya lewat query, bukan lewat getter.</p>
 *
 * <p><b>6. {@link #toString()} menyimpang dari induk.</b> Bentuknya {@code "id-nama"} (bukan
 * {@code "kode - nama"} seperti {@link GeneralValueObject#toString()}) dan membaca <i>field</i>
 * {@code nama} secara langsung, sehingga bisa mencetak {@code "12-null"} pada instance yang
 * {@link #getNama()}-nya belum pernah dipanggil.</p>
 *
 * <p><b>7. Identitas.</b> {@code equals()}/{@code compareTo()} diwarisi dari
 * {@link GeneralValueObject} (berbasis {@code id}, dengan {@code hashCode()} yang tidak
 * di-override di seluruh hierarki) — lihat peringatan lengkapnya di
 * {@link GeneralValueObject#equals(Object)}.</p>
 *
 * @see GeneralValueObject
 * @see Fakultas
 * @see PerguruanTinggi
 * @see ais.action.master.JurusanAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jurusan")

public class Jurusan extends GeneralValueObject {

	/**
	 * Nilai {@link #getJenisJurusan()} untuk prodi rumpun ilmu pasti/alam/teknik. Salah satu dari
	 * dua pilihan combobox "Jenis Jurusan" pada layar master jurusan; dipakai antara lain oleh
	 * laporan profil dosen untuk memisahkan statistik eksakta dan sosial.
	 */
	public static final String JENIS_EKSAKTA = "Eksakta";
	/**
	 * Nilai {@link #getJenisJurusan()} untuk prodi rumpun ilmu sosial/humaniora.
	 *
	 * @see #JENIS_EKSAKTA
	 */
	public static final String JENIS_SOSIAL = "Sosial";

	/** Status akreditasi BAN-PT tertinggi pada instrumen 9 kriteria (IAPS 4.0). */
	public static final String TERAKREDITASI_UNGGUL = "Terakreditasi Unggul";
	/** Status akreditasi BAN-PT peringkat A pada instrumen lama (7 standar). */
	public static final String TERAKREDITASI_A = "Terakreditasi A";
	/**
	 * Status akreditasi BAN-PT "Baik Sekali" (padanan A pada instrumen 9 kriteria).
	 *
	 * <p><b>Catatan:</b> nama konstantanya salah eja — {@code SEKLAI}, bukan {@code SEKALI}.
	 * Nilai String-nya sendiri benar dan tersimpan di database, jadi jangan mengganti nama
	 * konstanta ini tanpa menelusuri seluruh pemakaiannya.</p>
	 */
	public static final String TERAKREDITASI_BAIK_SEKLAI = "Terakreditasi Baik Sekali";
	/** Status akreditasi BAN-PT peringkat B pada instrumen lama. */
	public static final String TERAKREDITASI_B = "Terakreditasi B";
	/** Status akreditasi BAN-PT "Baik" (padanan B pada instrumen 9 kriteria). */
	public static final String TERAKREDITASI_BAIK = "Terakreditasi Baik";
	/** Status akreditasi BAN-PT peringkat C pada instrumen lama. */
	public static final String TERAKREDITASI_C = "Terakreditasi C";
	/** Status akreditasi BAN-PT "Baik" tingkat minimum (padanan C pada instrumen 9 kriteria). */
	public static final String TERAKREDITASI_MINIMUM = "Terakreditasi Minimum";
	/**
	 * Status "belum/tidak terakreditasi atau SK-nya sudah kedaluwarsa". Sekaligus menjadi
	 * <b>nilai default</b> yang dikembalikan {@link #getStatusAkreditasi()} bila kolomnya kosong,
	 * sehingga prodi yang datanya belum diisi ikut tampil sebagai tidak terakreditasi.
	 */
	public static final String TERAKREDITASI_KADALUWARSA = "Tidak Terakreditasi / Kadaluarsa";
	// public static final String BELUM_TERAKREDITASI = "Belum Terakreditasi";

	/**
	 * Daftar seluruh pilihan status akreditasi BAN-PT, berurutan dari peringkat tertinggi ke
	 * terendah, diisi oleh blok inisialisasi statis di bawah.
	 *
	 * <p>Dipakai sebagai sumber isi combobox "Status Akreditasi Terakhir BAN-PT" pada
	 * {@code JurusanAction} dan sebagai sumbu pengelompokan pada laporan borang akreditasi
	 * ({@code LaporanAkreditasi1B}).</p>
	 *
	 * <p><b>Perhatian:</b> ini {@link ArrayList} yang <b>bisa diubah (mutable)</b> dan berlingkup
	 * statis — memanggil {@code add}/{@code remove}/{@code clear} atasnya akan mengubah isi
	 * combobox dan laporan di seluruh aplikasi selama JVM hidup. Perlakukan sebagai konstanta
	 * hanya-baca.</p>
	 */
	public static final List<String> SEMUA_STATUS = new ArrayList<String>();

	/**
	 * Mengisi {@link #SEMUA_STATUS} sekali saja saat kelas dimuat, dengan urutan peringkat yang
	 * disengaja (Unggul, A, Baik Sekali, B, Baik, C, Minimum, Tidak Terakreditasi). Urutan inilah
	 * yang muncul di combobox dan di kolom laporan, jadi menambah status baru berarti memutuskan
	 * posisinya di urutan ini.
	 */
	static {
		SEMUA_STATUS.add(TERAKREDITASI_UNGGUL);
		SEMUA_STATUS.add(TERAKREDITASI_A);
		SEMUA_STATUS.add(TERAKREDITASI_BAIK_SEKLAI);
		SEMUA_STATUS.add(TERAKREDITASI_B);
		SEMUA_STATUS.add(TERAKREDITASI_BAIK);
		SEMUA_STATUS.add(TERAKREDITASI_C);
		SEMUA_STATUS.add(TERAKREDITASI_MINIMUM);
		SEMUA_STATUS.add(TERAKREDITASI_KADALUWARSA);
	}

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja dikunci agar object {@code Jurusan} yang
	 * pernah diserialisasi (cache berkas, sesi ZK, kirim antar-node) tetap bisa dibaca setelah
	 * kelas ini disunting.
	 */
	private static final long serialVersionUID = -1414018650710502115L;
	/**
	 * Primary key baris jurusan. <b>Field bayangan</b>: {@link GeneralValueObject} sudah punya
	 * field {@code id} sendiri, tetapi karena {@link #getId()}/{@link #setId(Long)} ikut
	 * di-override, salinan milik kelas induk tidak pernah terisi untuk {@code Jurusan}.
	 */
	private Long id;
	/**
	 * Nama pengguna yang terakhir mengubah baris ini. <b>Field bayangan</b> dari
	 * {@link GeneralValueObject}; lihat {@link #setOleh(String)}.
	 */
	private String oleh;
	/**
	 * Id pengguna yang terakhir mengubah baris ini. <b>Field bayangan</b> dari
	 * {@link GeneralValueObject}; lihat {@link #setOlehId(String)}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris jurusan ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, <b>mengabaikan nilai kosong</b>: bila argumen
	 * {@code null} atau hanya berisi spasi, method langsung keluar dan nilai lama dipertahankan.
	 *
	 * <p>Pola "abaikan kosong" ini sengaja dipakai agar jejak audit tidak terhapus oleh jalur
	 * simpan yang kebetulan tidak membawa informasi pengguna (proses batch, impor Excel,
	 * sinkronisasi Feeder). Efek sampingnya: field ini tidak bisa dikosongkan lewat setter.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong
	 * @see GeneralValueObject#setOlehId(String)
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan aturan "abaikan nilai kosong" yang sama
	 * seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 * @see GeneralValueObject#setOleh(String)
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris jurusan ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@link javax.persistence.PreUpdate}: dipanggil provider persistence tepat sebelum
	 * perintah {@code UPDATE} baris jurusan dikirim, dan meneruskan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} untuk memutakhirkan
	 * {@link #getTanggal_dirubah()} beserta penanda pengguna pengubah.
	 *
	 * <p>Ini adalah implementasi dari satu-satunya method {@code abstract} milik
	 * {@link GeneralValueObject}, jadi setiap entity punya versinya sendiri dengan isi identik.
	 * Method ini <b>tidak</b> berjalan pada operasi {@code INSERT}, pada update lewat HQL/SQL
	 * massal, maupun bila baris diubah di luar aplikasi.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris yang sama oleh
	 * perkakas audit otomatis — bentuknya tidak lazim, tetapi sah secara sintaks. Nilai awalnya
	 * diisi waktu server saat instance dibuat, sehingga entity baru sudah punya stempel waktu
	 * meski belum pernah disimpan.</p>
	 *
	 * @see GeneralValueObject#onUpdate()
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris jurusan. Tanpa validasi; biasanya dipanggil
	 * {@link #onUpdate()} lewat {@code AuditTimestampInterceptor}, bukan oleh kode layar.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris jurusan (kolom {@code tanggal_dirubah},
	 * presisi {@code TIMESTAMP}).
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang
	 *         dibuat lewat constructor karena field-nya berisi waktu pembuatan object
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks jurusan dalam bentuk {@code "id-nama"}.
	 *
	 * <p><b>Dua penyimpangan yang perlu diingat:</b></p>
	 * <ul>
	 *   <li>Formatnya <b>berbeda</b> dari {@link GeneralValueObject#toString()} yang memakai
	 *   {@code "kode - nama"}; komponen ZK yang menampilkan object apa adanya karena itu akan
	 *   memperlihatkan id numerik untuk jurusan.</li>
	 *   <li>Method ini membaca <i>field</i> {@code nama} <b>langsung</b>, bukan lewat
	 *   {@link #getNama()} yang menormalkan {@code null} menjadi {@code ""}. Pada instance yang
	 *   {@code getNama()}-nya belum pernah dipanggil dan namanya kosong, hasilnya berupa teks
	 *   {@code "12-null"}.</li>
	 * </ul>
	 *
	 * @return gabungan id dan nama jurusan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama jurusan/program studi. <b>Field bayangan</b> dari {@link GeneralValueObject}; lihat
	 * {@link #getNama()} untuk normalisasi yang diterapkan saat dibaca.
	 */
	private String nama;
	/** Nama jurusan dalam bahasa Inggris untuk dokumen dwibahasa. Lihat {@link #getNamaEn()}. */
	private String namaEn;
	/** Fakultas induk (wajib, kolom {@code fakultas} {@code NOT NULL}). Lihat {@link #getFakultas()}. */
	private Fakultas fakultas;
	/** Kode internal jurusan. Lihat {@link #getKode()} — kosong disulih menjadi {@code "--"}. */
	private String kode;
	/** Jenjang pendidikan (S1, S2, D3, ...). Lihat {@link #getJenjang()}. */
	private Jenjang jenjang;
	/** Kode prodi pada pelaporan PDPT/EPSBED (DIKTI generasi lama). Lihat {@link #getKodeEpsbed()}. */
	private String kodeEpsbed;
	/** Kode bebas tambahan untuk kebutuhan lokal institusi. Lihat {@link #getKodeLain()}. */
	private String kodeLain;

	/** Bahasa pengantar perkuliahan; kosong dianggap "Bahasa Indonesia". Lihat {@link #getBahasaPengantar()}. */
	private String bahasaPengantar;
	/** Ketua program studi. Lihat {@link #getKaprodi()}. */
	private Dosen kaprodi;
	/** Gelar lengkap lulusan, mis. "Sarjana Komputer". Lihat {@link #getGelar()}. */
	private String gelar;
	/** Singkatan gelar lulusan, mis. "S.Kom.". Lihat {@link #getSingkatanGelar()}. */
	private String singkatanGelar;

	/** Gelar lulusan versi Inggris; kosong akan jatuh ke {@link #getGelar()}. */
	private String gelarEn;
	/** Singkatan gelar versi Inggris; kosong akan jatuh ke {@link #getSingkatanGelar()}. */
	private String singkatanGelarEn;

	/** Peringkat akreditasi BAN-PT terakhir (teks bebas). Dicetak pada ijazah. */
	private String peringkatAkreditasi;
	/** Nilai/skor akreditasi BAN-PT terakhir (teks bebas). */
	private String akreditasi;
	/** Nomor SK akreditasi BAN-PT terakhir. */
	private String noSkAkreditasi;
	/** Tanggal SK akreditasi BAN-PT terakhir (presisi tanggal, tanpa jam). */
	private Date tanggalAkreditasi;
	// private Date tanggalAkhirAkreditasi;
	// private String pejabatAkreditasi;
	// private Date tanggalMulaiOperasional;

	/** Klasifikasi jenjang program studi menurut nomenklatur DIKTI. Lihat {@link #getJenjangProgramStudi()}. */
	private JenjangProgramStudi jenjangProgramStudi;
	/** Deskripsi prodi (HTML, kolom {@code text}); tampil di portal PMB. Lihat {@link #getDeskripsi()}. */
	private String deskripsi;
	/**
	 * Profil prodi (kolom {@code text}).
	 *
	 * <p>Tidak ada satu pun layar di aplikasi yang mengisi atau menampilkan field ini; nilainya
	 * hanya muncul lewat {@link #getProfil()} yang menyulih isi kosong dengan paragraf boilerplate
	 * panjang. Lihat catatan lengkapnya di {@link #getProfil()}.</p>
	 */
	private String profil;
	/** Grup/rumpun jurusan opsional. Lihat {@link #getGrupJurusan()}. */
	private GrupJurusan grupJurusan;
	/** Baris alamat pertama; kosong diisi dari alamat perguruan tinggi. Lihat {@link #getAlamat()}. */
	private String alamat;
	/** Baris alamat kedua; kosong diisi dari alamat perguruan tinggi. Lihat {@link #getAlamat2()}. */
	private String alamat2;
	/** Penanda prodi masih aktif; kosong dianggap aktif. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Rumpun keilmuan: {@link #JENIS_EKSAKTA} atau {@link #JENIS_SOSIAL}. */
	private String jenisJurusan;
	/** Status akreditasi BAN-PT terakhir; salah satu nilai {@link #SEMUA_STATUS}. */
	private String statusAkreditasi;

	/** UUID prodi pada PDDIKTI Feeder, kunci sinkronisasi data ke DIKTI. Lihat {@link #getFeeder()}. */
	private String feeder;

	/** Rasio dosen : mahasiswa yang dipakai perhitungan borang; kosong dianggap {@code 0.4}. */
	private Double rasioDosenDanMahasiswa;

	/** Label bebas untuk slot pejabat pertama; kosong dianggap "Pejabat I". */
	private String labelPejabat1;
	/** Label bebas untuk slot pejabat kedua; kosong dianggap "Pejabat II". */
	private String labelPejabat2;
	/** Label bebas untuk slot pejabat ketiga; kosong dianggap "Pejabat III". */
	private String labelPejabat3;

	/** Pegawai pengisi slot pejabat pertama (label lihat {@link #getLabelPejabat1()}). */
	private Pegawai pegawai1;
	/** Pegawai pengisi slot pejabat kedua (label lihat {@link #getLabelPejabat2()}). */
	private Pegawai pegawai2;
	/** Pegawai pengisi slot pejabat ketiga (label lihat {@link #getLabelPejabat3()}). */
	private Pegawai pegawai3;
	/** Nomor WhatsApp operator prodi. Lihat {@link #getWa()}. */
	private String wa;

	/** Nilai akreditasi LAM-PTKes terakhir (rumpun kesehatan), terpisah dari BAN-PT. */
	private String akreditasiKes;
	/** Status akreditasi LAM-PTKes terakhir. Berbeda dari BAN-PT, isian ini berupa teks bebas. */
	private String statusAkreditasiKes;
	/** Peringkat akreditasi LAM-PTKes terakhir. */
	private String peringkatAkreditasiKes;
	/** Nomor SK akreditasi LAM-PTKes terakhir. */
	private String noSkAkreditasiKes;
	/** Tanggal SK akreditasi LAM-PTKes terakhir. */
	private Date tanggalAkreditasiKes;
	/** Satuan kerja (unit anggaran) yang menaungi prodi. Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Penanda dosen prodi ini wajib punya satuan kerja. Lihat {@link #getDosenHarusPakaiSatuanKerja()}. */
	private Boolean dosenHarusPakaiSatuanKerja;

	/**
	 * Constructor default tanpa argumen. WAJIB ada karena Hibernate memakainya untuk membuat
	 * instance saat menghidrasi baris {@code jurusan} dari hasil query.
	 */
	public Jurusan() {
	}

	/**
	 * Constructor pintas yang langsung menyetel primary key, untuk membuat object "penunjuk"
	 * (hanya berisi id) sebagai kriteria pencarian atau referensi relasi tanpa memuat seluruh
	 * baris dari database.
	 *
	 * <p>Berbeda dari {@link GeneralValueObject#GeneralValueObject(Long)} yang memanggil
	 * {@code setId(...)}, versi ini menulis <b>langsung</b> ke field {@code id} milik
	 * {@code Jurusan}. Hasil akhirnya sama karena {@link #getId()} juga membaca field yang sama.</p>
	 *
	 * @param id primary key baris jurusan yang ditunjuk
	 */
	public Jurusan(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan primary key baris jurusan (kolom {@code id}, sequence/identity, tidak pernah
	 * ditulis aplikasi karena dipetakan {@code insertable = false}).
	 *
	 * <p>Nilai inilah yang dipakai {@link GeneralValueObject#equals(Object)} dan menjadi kunci
	 * seluruh relasi "jurusan" pada 130-an entity lain.</p>
	 *
	 * @return primary key jurusan, atau {@code null} bila baris belum pernah disimpan
	 * @see GeneralValueObject#getId()
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key jurusan. Tanpa validasi.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jurusan/program studi setelah dinormalkan: dipangkas spasi tepi dan
	 * setiap pasangan spasi ganda diringkas menjadi satu spasi.
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method ini <b>menulis</b> string
	 * kosong ke field terlebih dahulu. Karena pemetaan kelas ini memakai akses property, nilai
	 * yang sudah dinormalkan itulah yang dibaca Hibernate; artinya nama dengan spasi berlebih di
	 * database akan ikut terapikan saat baris jurusan disimpan ulang.</p>
	 *
	 * <p>Dua hal yang mudah salah dibaca: (1) pemeriksaan {@code this.nama == null} pada baris
	 * {@code return} tidak pernah bernilai benar karena field sudah dipaksa non-null di atasnya —
	 * jadi method ini <b>tidak pernah mengembalikan {@code null}</b>; (2) {@code replaceAll("  ", " ")}
	 * hanya satu lintasan, sehingga tiga spasi berturut-turut menyisakan dua spasi.</p>
	 *
	 * @return nama jurusan yang sudah dirapikan; {@code ""} bila belum diisi, tidak pernah
	 *         {@code null}
	 */
	@Column(name = "nama", length = 150)
	public String getNama() {
		if (nama == null) {
			nama = "";
		}
		return this.nama == null ? null : this.nama.trim().replaceAll("  ", " ");
	}

	/**
	 * Menyetel nama jurusan apa adanya, tanpa validasi maupun perapian (perapian dikerjakan
	 * {@link #getNama()} saat dibaca).
	 *
	 * @param nama nama jurusan baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menyetel fakultas induk. Tanpa validasi — kolomnya {@code NOT NULL}, jadi menyimpan jurusan
	 * dengan fakultas {@code null} akan gagal di tingkat database, bukan di sini.
	 *
	 * @param fakultas fakultas induk baru
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan {@link Fakultas} induk, setelah proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Relasi ini <b>wajib</b> ({@code nullable = false}) dan menjadi jalan menuju
	 * {@link PerguruanTinggi} — dipakai untuk kop surat, alamat (lihat {@link #getAlamat()}), dan
	 * pembatasan hak akses operator per fakultas. Karena {@code cascade} mencakup
	 * {@code PERSIST} dan {@code MERGE}, menyimpan jurusan dengan fakultas baru yang belum
	 * tersimpan akan ikut menyimpan fakultas tersebut.</p>
	 *
	 * <p><b>Efek samping:</b> hasil resolusi ditulis balik ke field, dan {@code check()} boleh
	 * membuka session Hibernate sendiri bila proxy sudah lepas dari session asalnya — jadi getter
	 * ini dapat menyentuh database.</p>
	 *
	 * @return fakultas induk; {@code null} hanya untuk baris yang belum lengkap
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = false)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menyetel kode internal jurusan apa adanya, tanpa validasi keunikan.
	 *
	 * @param kode kode jurusan baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kode internal jurusan yang sudah dipangkas spasi tepinya.
	 *
	 * <p><b>Efek samping penting:</b> bila kode masih kosong, method ini <b>menulis</b> nilai
	 * {@code "--"} ke field. Karena Hibernate membaca kolom lewat getter, nilai pengganti itu ikut
	 * tersimpan ke database pada operasi simpan berikutnya — kode {@code "--"} yang muncul di
	 * daftar prodi berasal dari sini, bukan dari isian pengguna. Pemeriksaan
	 * {@code kode == null} pada baris {@code return} karena itu tidak pernah terpenuhi, sehingga
	 * method ini tidak pernah mengembalikan {@code null}.</p>
	 *
	 * @return kode jurusan; {@code "--"} bila belum diisi, tidak pernah {@code null}
	 */
	@Column(name = "kode", length = 150)
	public String getKode() {
		if (kode == null || kode.trim().isEmpty()) {
			kode = "--";
		}
		return kode == null ? null : kode.trim();
	}

	/**
	 * Menyetel jenjang pendidikan prodi. Tanpa validasi.
	 *
	 * @param jenjang jenjang baru (S1, S2, D3, ...)
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	/**
	 * Mengembalikan {@link Jenjang} pendidikan prodi (S1/S2/S3/D3/profesi), setelah proxy lazy
	 * diresolusi lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Jenjang menentukan banyak aturan turunan: syarat SKS kelulusan, format nomor ijazah,
	 * dan pengelompokan pada pelaporan PDDIKTI. Pada layar master jurusan, combobox jenjang hanya
	 * menampilkan jenjang yang masih aktif.</p>
	 *
	 * @return jenjang pendidikan, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang")
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		return jenjang;
	}

	/**
	 * Menyetel kode prodi untuk pelaporan PDPT/EPSBED. Tanpa validasi format.
	 *
	 * @param kodeEpsbed kode PDPT baru
	 */
	public void setKodeEpsbed(String kodeEpsbed) {
		this.kodeEpsbed = kodeEpsbed;
	}

	/**
	 * Mengembalikan kode prodi versi PDPT/EPSBED (sistem pelaporan DIKTI generasi lama) dengan
	 * spasi tepi dipangkas. Pada layar master jurusan isian ini berlabel <i>"Kode PDPT"</i>.
	 *
	 * <p>Berbeda dari {@link #getFeeder()} yang berisi UUID pada PDDIKTI Feeder generasi baru;
	 * banyak institusi masih mengisi keduanya karena laporan lama tetap diminta.</p>
	 *
	 * @return kode PDPT/EPSBED yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "kode_epsbed", nullable = true)
	public String getKodeEpsbed() {
		return kodeEpsbed == null ? null : kodeEpsbed.trim();
	}

	/**
	 * Mengembalikan {@link Dosen} yang menjabat sebagai ketua program studi, setelah proxy lazy
	 * diresolusi lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Kaprodi dipakai sebagai penanda tangan default pada surat dan transkrip tingkat prodi,
	 * dan sebagai penerima notifikasi persetujuan (KRS, bimbingan) pada beberapa alur. Perhatikan
	 * bahwa jabatan struktural lain tidak disimpan di sini melainkan pada tiga slot bebas
	 * {@link #getPegawai1()}..{@link #getPegawai3()}.</p>
	 *
	 * @return dosen kaprodi, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kaprodi", nullable = true)
	public Dosen getKaprodi() {
		kaprodi = check(kaprodi);
		return kaprodi;
	}

	/**
	 * Menyetel dosen kaprodi. Tanpa validasi bahwa dosen tersebut memang berhomebase di jurusan
	 * ini.
	 *
	 * @param kaprodi dosen kaprodi baru
	 */
	public void setKaprodi(Dosen kaprodi) {
		this.kaprodi = kaprodi;
	}

	/**
	 * Mengembalikan gelar lengkap lulusan prodi (mis. "Sarjana Komputer"), apa adanya.
	 *
	 * <p>Dicetak pada ijazah dan album wisuda; pasangan berbahasa Inggrisnya
	 * {@link #getGelarEn()} jatuh kembali ke nilai ini bila belum diisi.</p>
	 *
	 * @return gelar lulusan, atau {@code null} bila belum diisi
	 */
	public String getGelar() {
		return gelar;
	}

	/**
	 * Menyetel gelar lengkap lulusan prodi. Tanpa validasi.
	 *
	 * @param gelar gelar baru
	 */
	public void setGelar(String gelar) {
		this.gelar = gelar;
	}

	/**
	 * Mengembalikan singkatan gelar lulusan (mis. "S.Kom."), apa adanya; inilah bentuk yang
	 * ditempelkan di belakang nama lulusan pada transkrip dan surat keterangan lulus.
	 *
	 * @return singkatan gelar, atau {@code null} bila belum diisi
	 */
	@Column(name = "singkatan_gelar", nullable = true)
	public String getSingkatanGelar() {
		return singkatanGelar;
	}

	/**
	 * Menyetel singkatan gelar lulusan. Tanpa validasi.
	 *
	 * @param singkatanGelar singkatan gelar baru
	 */
	public void setSingkatanGelar(String singkatanGelar) {
		this.singkatanGelar = singkatanGelar;
	}

	/**
	 * Mengembalikan <b>nilai/skor</b> akreditasi BAN-PT terakhir apa adanya (teks bebas, mis.
	 * "361").
	 *
	 * <p>Jangan tertukar dengan {@link #getStatusAkreditasi()} (pilihan terkontrol dari
	 * {@link #SEMUA_STATUS}) maupun {@link #getPeringkatAkreditasi()} (huruf/predikat). Ketiganya
	 * kolom terpisah yang diisi berdampingan pada layar master jurusan.</p>
	 *
	 * @return nilai akreditasi BAN-PT, atau {@code null} bila belum diisi
	 */
	@Column(name = "akreditasi", nullable = true)
	public String getAkreditasi() {
		return akreditasi;
	}

	/**
	 * Menyetel nilai/skor akreditasi BAN-PT terakhir. Tanpa validasi.
	 *
	 * @param akreditasi nilai akreditasi baru
	 */
	public void setAkreditasi(String akreditasi) {
		this.akreditasi = akreditasi;
	}

	/**
	 * Mengembalikan nomor SK akreditasi BAN-PT terakhir apa adanya. Dicetak pada ijazah dan
	 * lampiran borang.
	 *
	 * @return nomor SK akreditasi BAN-PT, atau {@code null} bila belum diisi
	 */
	@Column(name = "no_sk_akreditasi", nullable = true)
	public String getNoSkAkreditasi() {
		return noSkAkreditasi;
	}

	/**
	 * Menyetel nomor SK akreditasi BAN-PT terakhir. Tanpa validasi.
	 *
	 * @param noSkAkreditasi nomor SK baru
	 */
	public void setNoSkAkreditasi(String noSkAkreditasi) {
		this.noSkAkreditasi = noSkAkreditasi;
	}

	/**
	 * Mengembalikan tanggal SK akreditasi BAN-PT terakhir (dipetakan {@code DATE}, jadi bagian
	 * jam tidak disimpan).
	 *
	 * <p>Tanggal berakhirnya masa berlaku tidak disimpan — field {@code tanggalAkhirAkreditasi}
	 * sudah dinonaktifkan (lihat deklarasi yang dikomentari di atas), sehingga kedaluwarsa
	 * akreditasi harus ditandai manual lewat {@link #setStatusAkreditasi(String)}.</p>
	 *
	 * @return tanggal SK akreditasi BAN-PT, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_akreditasi", nullable = true)
	public Date getTanggalAkreditasi() {
		return tanggalAkreditasi;
	}

	/**
	 * Menyetel tanggal SK akreditasi BAN-PT terakhir. Tanpa validasi.
	 *
	 * @param tanggalAkreditasi tanggal SK baru
	 */
	public void setTanggalAkreditasi(Date tanggalAkreditasi) {
		this.tanggalAkreditasi = tanggalAkreditasi;
	}

	/**
	 * Mengembalikan {@link JenjangProgramStudi} — klasifikasi nomenklatur program studi menurut
	 * DIKTI — setelah proxy lazy diresolusi lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Berbeda dari {@link #getJenjang()} yang menyatakan strata (S1/S2/D3), entity ini
	 * memerinci jenis programnya (akademik/vokasi/profesi beserta kodenya) untuk keperluan
	 * pelaporan. Relasinya opsional dan hanya dipakai di sebagian layar.</p>
	 *
	 * @return jenjang program studi, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_program_studi", nullable = true)
	public JenjangProgramStudi getJenjangProgramStudi() {
		jenjangProgramStudi = check(jenjangProgramStudi);
		return jenjangProgramStudi;
	}

	/**
	 * Menyetel klasifikasi jenjang program studi. Tanpa validasi kecocokan dengan
	 * {@link #getJenjang()}.
	 *
	 * @param jenjangProgramStudi klasifikasi baru
	 */
	public void setJenjangProgramStudi(JenjangProgramStudi jenjangProgramStudi) {
		this.jenjangProgramStudi = jenjangProgramStudi;
	}

	/**
	 * Mengembalikan deskripsi prodi (kolom {@code text}, isinya berupa HTML karena ditampilkan
	 * dengan komponen {@code Html} pada portal PMB dan dikirim sebagai {@code introductoryText}
	 * saat data prodi disinkronkan ke situs).
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method ini menulis string kosong ke
	 * field sehingga hasilnya tidak pernah {@code null}. Isi HTML dikembalikan apa adanya —
	 * pemanggil bertanggung jawab atas sanitasi bila menampilkannya di konteks yang berbeda.</p>
	 *
	 * @return deskripsi prodi; {@code ""} bila belum diisi, tidak pernah {@code null}
	 */
	@Column(name = "deskripsi", columnDefinition = "text")
	public String getDeskripsi() {
		if (deskripsi == null) {
			deskripsi = "";
		}
		return deskripsi;
	}

	/**
	 * Menyetel deskripsi prodi (HTML) apa adanya, tanpa sanitasi.
	 *
	 * @param deskripsi deskripsi baru
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Mengembalikan profil prodi (kolom {@code text}), <b>menyulih isi kosong dengan paragraf
	 * boilerplate generik sepanjang ~1.000 karakter</b> yang ditulis langsung di dalam kode.
	 *
	 * <p>Beberapa hal yang perlu diketahui sebelum memakai atau mengubah method ini:</p>
	 * <ul>
	 *   <li><b>Efek samping:</b> teks boilerplate itu <b>ditulis balik ke field</b>. Karena
	 *   pemetaan kelas ini memakai akses property, teks tersebut ikut masuk ke perintah
	 *   {@code INSERT} saat baris jurusan baru disimpan — jadi paragraf generik ini bisa benar-benar
	 *   mengendap di database, bukan sekadar nilai tampilan.</li>
	 *   <li>Isinya sama persis untuk <b>semua</b> prodi, sehingga tidak boleh dipakai sebagai
	 *   penanda "profil sudah diisi". Untuk membedakan terisi/kosong, baca kolomnya lewat query.</li>
	 *   <li>Pada kondisi kode saat ini, tidak ada satu pun layar yang memanggil
	 *   {@code getProfil()}/{@code setProfil(...)} milik {@code Jurusan} — layar master jurusan
	 *   tidak menampilkan field ini sama sekali. Jalur yang tersisa adalah pemanggilan reflektif
	 *   (ekspor JSON, parameter laporan), yang akan ikut membawa paragraf panjang ini.</li>
	 * </ul>
	 *
	 * @return profil prodi; teks boilerplate default bila belum diisi, tidak pernah {@code null}
	 */
	@Column(name = "profil", columnDefinition = "text")
	public String getProfil() {
		if (profil == null || profil.trim().isEmpty()) {
			profil = "Program studi ini diselenggarakan sebagai bagian dari komitmen institusi dalam menyediakan pendidikan tinggi yang bermutu, relevan, berkarakter, dan berorientasi pada kebutuhan masyarakat serta perkembangan ilmu pengetahuan. Profil program studi disusun untuk memberikan gambaran resmi mengenai arah akademik, tata kelola pembelajaran, pengembangan kompetensi lulusan, budaya mutu, layanan mahasiswa, kegiatan akademik, penguatan soft skill, dan kontribusi program studi terhadap pembangunan. Setiap mahasiswa diarahkan untuk memperoleh pengalaman belajar yang tertib, terukur, dan bermakna melalui kurikulum yang dikembangkan secara berkelanjutan, proses pembelajaran yang menghargai etika akademik, serta pendampingan dosen yang mendorong kemandirian, daya kritis, kemampuan komunikasi, dan tanggung jawab profesional. Program studi juga menempatkan penelitian, pengabdian kepada masyarakat, kerja sama, dan pemanfaatan teknologi sebagai bagian penting dari proses pendidikan sehingga lulusan memiliki kemampuan adaptif, integritas pribadi, kepedulian sosial, serta kesiapan untuk memasuki dunia kerja, melanjutkan studi, atau mengembangkan karya yang bermanfaat bagi lingkungan sekitarnya.";
		}
		return profil;
	}

	/**
	 * Menyetel profil prodi apa adanya. Menyetel {@code null} atau string kosong berarti
	 * mengembalikan prodi ke teks boilerplate default {@link #getProfil()}.
	 *
	 * @param profil profil baru
	 */
	public void setProfil(String profil) {
		this.profil = profil;
	}

	/**
	 * Mengembalikan UUID program studi pada <b>PDDIKTI Feeder</b> — kunci pemetaan antara data
	 * lokal AIS dan data DIKTI saat sinkronisasi (mahasiswa, nilai, aktivitas kuliah semua
	 * dikirim dengan {@code id_prodi} ini).
	 *
	 * <p>Normalisasi: spasi tepi dipangkas, dan nilai kosong dikembalikan sebagai {@code null}
	 * (bukan {@code ""}) sehingga pemanggil cukup memeriksa {@code null} untuk tahu prodi ini
	 * belum dipetakan ke Feeder. Field tidak diubah — berbeda dari {@link #getKode()} dan
	 * {@link #getProfil()}, getter ini <b>tidak</b> punya efek samping.</p>
	 *
	 * <p>Isian ini hanya tampil pada layar master jurusan bila konfigurasi mengizinkan admin
	 * mengakses fitur Feeder.</p>
	 *
	 * @return UUID prodi di Feeder, atau {@code null} bila belum dipetakan
	 * @see #getKodeEpsbed()
	 */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Menyetel UUID prodi pada PDDIKTI Feeder. Tanpa validasi format UUID.
	 *
	 * @param feeder UUID prodi baru
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Mengembalikan {@link GrupJurusan} — pengelompokan opsional beberapa prodi (mis. rumpun ilmu
	 * atau "jurusan" yang membawahi beberapa program studi) — setelah proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p>Dipakai antara lain untuk mengambil nama ketua jurusan pada album wisuda. Barisnya
	 * opsional: pada layar master jurusan, baris pilihan grup disembunyikan bila tabel
	 * {@code grup_jurusan} masih kosong.</p>
	 *
	 * @return grup jurusan, atau {@code null} bila prodi tidak tergabung dalam grup mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_jurusan", nullable = true)
	public GrupJurusan getGrupJurusan() {
		grupJurusan = check(grupJurusan);
		return grupJurusan;
	}

	/**
	 * Menyetel grup jurusan. Tanpa validasi.
	 *
	 * @param grupJurusan grup jurusan baru
	 */
	public void setGrupJurusan(GrupJurusan grupJurusan) {
		this.grupJurusan = grupJurusan;
	}

	/**
	 * Mengembalikan nama prodi dalam bahasa Inggris, dengan <b>fallback</b> ke {@link #getNama()}
	 * bila belum diisi — sehingga dokumen dwibahasa (ijazah, transkrip, album wisuda) selalu
	 * mendapat teks, meski teksnya berbahasa Indonesia.
	 *
	 * <p>Konsekuensi fallback ini: pemanggil tidak bisa membedakan "nama Inggris belum diisi" dari
	 * "nama Inggris kebetulan sama dengan nama Indonesia". Nilai yang dikembalikan juga
	 * <b>tidak</b> dinormalkan bila field terisi (spasi tepi ikut terbawa), berbeda dari cabang
	 * fallback-nya yang melewati normalisasi {@link #getNama()}.</p>
	 *
	 * @return nama prodi versi Inggris, atau nama Indonesia sebagai pengganti; tidak pernah
	 *         {@code null}
	 */
	public String getNamaEn() {
		return namaEn == null || namaEn.trim().isEmpty() ? getNama() : namaEn;
	}

	/**
	 * Menyetel nama prodi versi Inggris. Tanpa validasi.
	 *
	 * @param namaEn nama Inggris baru
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * Mengembalikan peringkat/predikat akreditasi BAN-PT terakhir apa adanya (teks bebas, mis.
	 * "A" atau "Unggul"). Nilai inilah yang dicetak pada ijazah lulusan.
	 *
	 * @return peringkat akreditasi BAN-PT, atau {@code null} bila belum diisi
	 * @see #getStatusAkreditasi()
	 * @see #getAkreditasi()
	 */
	public String getPeringkatAkreditasi() {
		return peringkatAkreditasi;
	}

	/**
	 * Menyetel peringkat akreditasi BAN-PT terakhir. Tanpa validasi terhadap daftar peringkat
	 * resmi.
	 *
	 * @param peringkatAkreditasi peringkat baru
	 */
	public void setPeringkatAkreditasi(String peringkatAkreditasi) {
		this.peringkatAkreditasi = peringkatAkreditasi;
	}

	/**
	 * Mengembalikan baris alamat pertama prodi, dengan <b>pewarisan alamat dari perguruan
	 * tinggi</b> bila alamat prodi masih kosong.
	 *
	 * <p>Alur lengkapnya: memuat {@link #getFakultas()} (yang memicu
	 * {@link GeneralValueObject#check(Object)} dan bisa membuka session Hibernate sendiri),
	 * menelusuri {@code fakultas.getPerguruanTinggi().getAlamat1()}, lalu <b>menulis</b> hasilnya
	 * ke field {@code alamat} object ini.</p>
	 *
	 * <p><b>Dua efek samping yang mudah terlewat:</b> (1) getter "alamat" yang tampak sepele ini
	 * dapat menyentuh database; (2) alamat perguruan tinggi yang tersalin ikut tertulis ke kolom
	 * {@code alamat} tabel jurusan saat baris tersebut disimpan, sehingga sesudahnya prodi punya
	 * alamat "sendiri" yang tidak lagi ikut berubah bila alamat perguruan tinggi diperbarui.</p>
	 *
	 * @return alamat baris pertama; {@code ""} bila prodi maupun perguruan tinggi tidak punya
	 *         alamat, tidak pernah {@code null}
	 */
	public String getAlamat() {
		fakultas = getFakultas();
		if ((alamat == null || alamat.trim().isEmpty()) && fakultas != null && fakultas.getPerguruanTinggi() != null
				&& fakultas.getPerguruanTinggi().getAlamat1() != null) {
			alamat = fakultas.getPerguruanTinggi().getAlamat1();
		}
		return alamat == null ? "" : alamat;
	}

	/**
	 * Menyetel baris alamat pertama prodi. Menyetel {@code null}/kosong berarti membuka kembali
	 * pewarisan alamat dari perguruan tinggi pada pemanggilan {@link #getAlamat()} berikutnya.
	 *
	 * @param alamat alamat baris pertama baru
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan baris alamat kedua prodi, dengan pewarisan dari
	 * {@code perguruanTinggi.getAlamat2()} bila masih kosong.
	 *
	 * <p>Alur, efek samping (pemuatan relasi lewat {@link #getFakultas()} dan penulisan balik ke
	 * field) serta konsekuensinya identik dengan {@link #getAlamat()}; hanya kolom sumbernya yang
	 * berbeda.</p>
	 *
	 * @return alamat baris kedua; {@code ""} bila tidak ada sumber alamat, tidak pernah
	 *         {@code null}
	 * @see #getAlamat()
	 */
	public String getAlamat2() {
		fakultas = getFakultas();
		if ((alamat2 == null || alamat2.trim().isEmpty()) && fakultas != null && fakultas.getPerguruanTinggi() != null
				&& fakultas.getPerguruanTinggi().getAlamat2() != null) {
			alamat2 = fakultas.getPerguruanTinggi().getAlamat2();
		}
		return alamat2 == null ? "" : alamat2;
	}

	/**
	 * Menyetel baris alamat kedua prodi. Menyetel {@code null}/kosong membuka kembali pewarisan
	 * alamat dari perguruan tinggi.
	 *
	 * @param alamat2 alamat baris kedua baru
	 */
	public void setAlamat2(String alamat2) {
		this.alamat2 = alamat2;
	}

	/**
	 * Mengembalikan penanda prodi masih aktif menyelenggarakan pendidikan.
	 *
	 * <p><b>Default {@code true}</b>: kolom yang masih {@code null} — termasuk seluruh baris lama
	 * dari sebelum field ini ada — dianggap aktif. Karena itu penonaktifan prodi harus eksplisit.
	 * Nilai ini menyaring daftar prodi pada berbagai layar (mis. pemilihan prodi saat penerimaan
	 * dan pembuatan tagihan), jadi mengubahnya menyembunyikan prodi dari alur-alur tersebut tanpa
	 * menghapus datanya.</p>
	 *
	 * @return {@code true} bila prodi aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif prodi. Menyetel {@code null} sama artinya dengan "aktif" karena
	 * {@link #getAktif()} memperlakukan {@code null} sebagai {@code true}.
	 *
	 * @param aktif penanda aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan rumpun keilmuan prodi apa adanya — nilainya salah satu dari
	 * {@link #JENIS_EKSAKTA} atau {@link #JENIS_SOSIAL}, atau {@code null} bila belum dipilih.
	 *
	 * <p>Disimpan sebagai teks, bukan enum atau relasi, sehingga perbandingan harus memakai
	 * konstanta di kelas ini agar tidak salah eja. Dipakai antara lain untuk memisahkan statistik
	 * dosen eksakta dan sosial pada laporan borang.</p>
	 *
	 * @return "Eksakta", "Sosial", atau {@code null}
	 */
	public String getJenisJurusan() {
		return jenisJurusan;
	}

	/**
	 * Menyetel rumpun keilmuan prodi. Tanpa validasi bahwa nilainya termasuk
	 * {@link #JENIS_EKSAKTA}/{@link #JENIS_SOSIAL}.
	 *
	 * @param jenisJurusan rumpun keilmuan baru
	 */
	public void setJenisJurusan(String jenisJurusan) {
		this.jenisJurusan = jenisJurusan;
	}

	/**
	 * Mengembalikan status akreditasi BAN-PT terakhir, dengan <b>default
	 * {@link #TERAKREDITASI_KADALUWARSA}</b> ("Tidak Terakreditasi / Kadaluarsa") bila kolomnya
	 * kosong.
	 *
	 * <p>Akibat default ini, prodi yang datanya sekadar belum diisi tetap tampil sebagai tidak
	 * terakreditasi pada combobox dan laporan — pilihan yang aman, tetapi berarti nilai
	 * "kadaluwarsa" tidak bisa dibedakan dari "belum pernah diisi" lewat getter ini.</p>
	 *
	 * @return salah satu nilai {@link #SEMUA_STATUS}; tidak pernah {@code null}
	 * @see #getPeringkatAkreditasi()
	 * @see #getAkreditasi()
	 */
	public String getStatusAkreditasi() {
		return statusAkreditasi == null || statusAkreditasi.trim().isEmpty() ? TERAKREDITASI_KADALUWARSA
				: statusAkreditasi;
	}

	/**
	 * Menyetel status akreditasi BAN-PT. Tanpa validasi bahwa nilainya ada di
	 * {@link #SEMUA_STATUS}.
	 *
	 * @param statusAkreditasi status akreditasi baru
	 */
	public void setStatusAkreditasi(String statusAkreditasi) {
		this.statusAkreditasi = statusAkreditasi;
	}

	/**
	 * Mengembalikan gelar lulusan versi Inggris, dengan fallback ke {@link #getGelar()} bila
	 * belum diisi.
	 *
	 * <p><b>Beda halus dengan {@link #getNamaEn()}:</b> di sini fallback hanya terjadi pada nilai
	 * {@code null} — string kosong atau berisi spasi dianggap sudah diisi dan dikembalikan apa
	 * adanya, sehingga dokumen berbahasa Inggris bisa tercetak tanpa gelar bila pengguna pernah
	 * mengosongkan isian ini lewat layar (Textbox ZK menyimpan {@code ""}, bukan {@code null}).</p>
	 *
	 * @return gelar versi Inggris, atau gelar Indonesia sebagai pengganti; bisa {@code null} bila
	 *         keduanya belum diisi
	 */
	public String getGelarEn() {
		return gelarEn == null ? getGelar() : gelarEn;
	}

	/**
	 * Menyetel gelar lulusan versi Inggris. Tanpa validasi.
	 *
	 * @param gelarEn gelar versi Inggris baru
	 */
	public void setGelarEn(String gelarEn) {
		this.gelarEn = gelarEn;
	}

	/**
	 * Mengembalikan singkatan gelar versi Inggris, dengan fallback ke
	 * {@link #getSingkatanGelar()} bila bernilai {@code null}.
	 *
	 * <p>Berlaku catatan yang sama seperti {@link #getGelarEn()}: string kosong <b>tidak</b>
	 * memicu fallback.</p>
	 *
	 * @return singkatan gelar versi Inggris, atau singkatan Indonesia sebagai pengganti; bisa
	 *         {@code null}
	 */
	public String getSingkatanGelarEn() {
		return singkatanGelarEn == null ? getSingkatanGelar() : singkatanGelarEn;
	}

	/**
	 * Menyetel singkatan gelar versi Inggris. Tanpa validasi.
	 *
	 * @param singkatanGelarEn singkatan gelar versi Inggris baru
	 */
	public void setSingkatanGelarEn(String singkatanGelarEn) {
		this.singkatanGelarEn = singkatanGelarEn;
	}

	/**
	 * Mengembalikan rasio dosen terhadap mahasiswa yang dipakai perhitungan kecukupan dosen,
	 * dengan <b>default {@code 0.4}</b> bila belum diisi.
	 *
	 * <p>Angka ini dipakai sebagai parameter pada perhitungan borang/laporan mutu; nilai default
	 * dipilih sewenang-wenang di kode dan bukan berasal dari regulasi, jadi institusi yang
	 * memakainya untuk pelaporan resmi sebaiknya mengisinya eksplisit lewat layar master jurusan.</p>
	 *
	 * @return rasio dosen:mahasiswa; tidak pernah {@code null}
	 */
	public Double getRasioDosenDanMahasiswa() {
		return rasioDosenDanMahasiswa == null ? 0.4 : rasioDosenDanMahasiswa;
	}

	/**
	 * Menyetel rasio dosen terhadap mahasiswa. Tanpa validasi rentang.
	 *
	 * @param rasioDosenDanMahasiswa rasio baru
	 */
	public void setRasioDosenDanMahasiswa(Double rasioDosenDanMahasiswa) {
		this.rasioDosenDanMahasiswa = rasioDosenDanMahasiswa;
	}

	/**
	 * Mengembalikan bahasa pengantar perkuliahan prodi (spasi tepi dipangkas), dengan <b>default
	 * "Bahasa Indonesia"</b> bila belum diisi.
	 *
	 * <p>Dipakai sebagai parameter cetak pada album wisuda dan laporan profil lulusan. Getter ini
	 * hanya menormalkan nilai kembalian dan <b>tidak</b> menulis balik ke field.</p>
	 *
	 * @return bahasa pengantar; tidak pernah {@code null}
	 */
	public String getBahasaPengantar() {
		return bahasaPengantar == null || bahasaPengantar.trim().isEmpty() ? "Bahasa Indonesia"
				: bahasaPengantar.trim();
	}

	/**
	 * Menyetel bahasa pengantar perkuliahan. Tanpa validasi.
	 *
	 * @param bahasaPengantar bahasa pengantar baru
	 */
	public void setBahasaPengantar(String bahasaPengantar) {
		this.bahasaPengantar = bahasaPengantar;
	}

	/**
	 * Mengembalikan kode tambahan bebas apa adanya — slot untuk kode prodi versi lain yang dipakai
	 * institusi (kode yayasan, kode kerja sama, kode sistem lama) di luar
	 * {@link #getKode()}, {@link #getKodeEpsbed()}, dan {@link #getFeeder()}.
	 *
	 * <p>Tanpa normalisasi apa pun: nilai bisa {@code null}, kosong, atau mengandung spasi tepi.</p>
	 *
	 * @return kode lain, atau {@code null} bila belum diisi
	 */
	public String getKodeLain() {
		return kodeLain;
	}

	/**
	 * Menyetel kode tambahan bebas. Tanpa validasi.
	 *
	 * @param kodeLain kode lain baru
	 */
	public void setKodeLain(String kodeLain) {
		this.kodeLain = kodeLain;
	}

	/**
	 * Mengembalikan label jabatan untuk slot pejabat pertama ({@link #getPegawai1()}), dengan
	 * default "Pejabat I".
	 *
	 * <p>Tiga pasang label+pegawai ini adalah slot <b>bebas</b>: institusi mengisi sendiri nama
	 * jabatannya (mis. "Sekretaris Prodi", "Ketua Gugus Mutu") lewat layar master jurusan. Pada
	 * mode informasi, baris pejabat hanya ditampilkan bila labelnya sudah <b>diubah</b> dari
	 * default — jadi teks "Pejabat I" di sini berfungsi sekaligus sebagai penanda "slot belum
	 * dipakai". Karena itu jangan mengubah string default ini tanpa menyesuaikan
	 * {@code JurusanAction}, yang membandingkannya secara harfiah.</p>
	 *
	 * <p>Perhatikan fallback memakai perbandingan {@code null} saja: label yang dikosongkan lewat
	 * Textbox ZK tersimpan sebagai {@code ""} dan akan dikembalikan sebagai string kosong.</p>
	 *
	 * @return label jabatan slot pertama; tidak pernah {@code null}
	 */
	public String getLabelPejabat1() {
		return labelPejabat1 == null ? "Pejabat I" : labelPejabat1;
	}

	/**
	 * Menyetel label jabatan slot pejabat pertama. Tanpa validasi.
	 *
	 * @param labelPejabat1 label jabatan baru
	 */
	public void setLabelPejabat1(String labelPejabat1) {
		this.labelPejabat1 = labelPejabat1;
	}

	/**
	 * Mengembalikan label jabatan untuk slot pejabat kedua ({@link #getPegawai2()}), dengan
	 * default "Pejabat II". Berlaku seluruh catatan pada {@link #getLabelPejabat1()}.
	 *
	 * @return label jabatan slot kedua; tidak pernah {@code null}
	 */
	public String getLabelPejabat2() {
		return labelPejabat2 == null ? "Pejabat II" : labelPejabat2;
	}

	/**
	 * Menyetel label jabatan slot pejabat kedua. Tanpa validasi.
	 *
	 * @param labelPejabat2 label jabatan baru
	 */
	public void setLabelPejabat2(String labelPejabat2) {
		this.labelPejabat2 = labelPejabat2;
	}

	/**
	 * Mengembalikan label jabatan untuk slot pejabat ketiga ({@link #getPegawai3()}), dengan
	 * default "Pejabat III". Berlaku seluruh catatan pada {@link #getLabelPejabat1()}.
	 *
	 * @return label jabatan slot ketiga; tidak pernah {@code null}
	 */
	public String getLabelPejabat3() {
		return labelPejabat3 == null ? "Pejabat III" : labelPejabat3;
	}

	/**
	 * Menyetel label jabatan slot pejabat ketiga. Tanpa validasi.
	 *
	 * @param labelPejabat3 label jabatan baru
	 */
	public void setLabelPejabat3(String labelPejabat3) {
		this.labelPejabat3 = labelPejabat3;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai1", nullable = true)
	public Pegawai getPegawai1() {
		pegawai1 = check(pegawai1);
		return pegawai1;
	}

	public void setPegawai1(Pegawai pegawai1) {
		this.pegawai1 = pegawai1;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai2", nullable = true)
	public Pegawai getPegawai2() {
		pegawai2 = check(pegawai2);
		return pegawai2;
	}

	public void setPegawai2(Pegawai pegawai2) {
		this.pegawai2 = pegawai2;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai3", nullable = true)
	public Pegawai getPegawai3() {
		pegawai3 = check(pegawai3);
		return pegawai3;
	}

	public void setPegawai3(Pegawai pegawai3) {
		this.pegawai3 = pegawai3;
	}

	public String getWa() {
		return wa == null ? "" : wa.trim();
	}

	public void setWa(String wa) {
		this.wa = wa;
	}

	public String getAkreditasiKes() {
		return akreditasiKes;
	}

	public void setAkreditasiKes(String akreditasiKes) {
		this.akreditasiKes = akreditasiKes;
	}

	public String getStatusAkreditasiKes() {
		return statusAkreditasiKes;
	}

	public void setStatusAkreditasiKes(String statusAkreditasiKes) {
		this.statusAkreditasiKes = statusAkreditasiKes;
	}

	public String getPeringkatAkreditasiKes() {
		return peringkatAkreditasiKes;
	}

	public void setPeringkatAkreditasiKes(String peringkatAkreditasiKes) {
		this.peringkatAkreditasiKes = peringkatAkreditasiKes;
	}

	public String getNoSkAkreditasiKes() {
		return noSkAkreditasiKes;
	}

	public void setNoSkAkreditasiKes(String noSkAkreditasiKes) {
		this.noSkAkreditasiKes = noSkAkreditasiKes;
	}

	public Date getTanggalAkreditasiKes() {
		return tanggalAkreditasiKes;
	}

	public void setTanggalAkreditasiKes(Date tanggalAkreditasiKes) {
		this.tanggalAkreditasiKes = tanggalAkreditasiKes;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	public Boolean getDosenHarusPakaiSatuanKerja() {
		return dosenHarusPakaiSatuanKerja == null ? false : dosenHarusPakaiSatuanKerja;
	}

	public void setDosenHarusPakaiSatuanKerja(Boolean dosenHarusPakaiSatuanKerja) {
		this.dosenHarusPakaiSatuanKerja = dosenHarusPakaiSatuanKerja;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putFile(Map parameters) {
		Jurusan jurusan = this;
		File file = FileFoto.fileAdaDiFolder(LampiranLain.KOP_JURUSAN, jurusan.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("KOP_JURUSAN", file.getAbsolutePath());
			parameters.put("KOP_JURUSAN_" + jurusan.getId(), file.getAbsolutePath());
			parameters.put("KOP_JURUSAN_" + jurusan.getNama(), file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, jurusan.getId(), LampiranLain.KOP_JURUSAN);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("KOP_JURUSAN", fileKop.getAbsolutePath());
					parameters.put("KOP_JURUSAN_" + jurusan.getId(), fileKop.getAbsolutePath());
					parameters.put("KOP_JURUSAN_" + jurusan.getNama(), fileKop.getAbsolutePath());
				}
			}
		}

		file = FileFoto.fileAdaDiFolder(LampiranLain.STEMPEL_JURUSAN, jurusan.getId());
		if (Common.isGambarLaporanValid(file)) {
			parameters.put("STEMPEL_JURUSAN", file.getAbsolutePath());
			parameters.put("STEMPEL_JURUSAN_" + jurusan.getId(), file.getAbsolutePath());
			parameters.put("STEMPEL_JURUSAN_" + jurusan.getNama(), file.getAbsolutePath());
		} else {
			LampiranLain kop = LampiranLain.ambil(false, jurusan.getId(), LampiranLain.STEMPEL_JURUSAN);
			if (kop != null) {
				File fileKop = kop.ambilFile();
				if (Common.isGambarLaporanValid(fileKop)) {
					parameters.put("STEMPEL_JURUSAN", fileKop.getAbsolutePath());
					parameters.put("STEMPEL_JURUSAN_" + jurusan.getId(), fileKop.getAbsolutePath());
					parameters.put("STEMPEL_JURUSAN_" + jurusan.getNama(), fileKop.getAbsolutePath());
				}
			}
		}
	}
}
