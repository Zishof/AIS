package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
import ais.common.ConstantValues;

/**
 * Entity <b>gelaran (periode) KKN</b> — Kuliah Kerja Nyata, kegiatan pengabdian masyarakat
 * mahasiswa — pada tabel {@code public.kkn}. Satu baris mewakili SATU penyelenggaraan KKN:
 * namanya, rentang tanggalnya, lingkup peserta yang boleh mendaftar
 * ({@link Fakultas}/{@link Jurusan}/program), ambang SKS &amp; IPK yang harus dipenuhi pendaftar,
 * aturan pembayaran, dan dua sakelar perizinan pengubahan agenda.
 *
 * <h3>Kedudukan dalam alur KKN</h3>
 * <p>Kelas ini adalah <b>akar</b> seluruh modul KKN. Semua entity KKN lain menunjuk balik ke
 * sini lewat properti bernama {@code kkn}:</p>
 * <pre>
 * Kkn  (kelas ini — satu gelaran/periode KKN)
 *  ├─ KknPunyaPersyaratan ─── PersyaratanKkn        (berkas/isian yang wajib diunggah pendaftar)
 *  │        └─ MahasiswaKknPersyaratan              (jawaban &amp; skor tiap mahasiswa)
 *  ├─ KknPunyaKomponenPenilaianKkn ─ KomponenPenilaianKkn  (komponen &amp; bobot nilai akhir)
 *  ├─ MahasiswaDaftarKkn        (PENDAFTAR: tanggalDaftar, terima, memenuhiSyarat, totalSkor)
 *  ├─ MahasiswaDapatKkn         (PESERTA yang akhirnya diterima)
 *  ├─ PengecualianKknMahasiswa  (daftar mahasiswa yang dibebaskan dari SEMUA syarat)
 *  └─ KelompokKkn               (kelompok/posko: lokasi, kuota, dosen_pembimbing1..10)
 *           └─ MahasiswaDapatKelompokKkn  (anggota kelompok + nilai + kelulusan)
 * </pre>
 * <p>Urutan kerjanya: operator membuka gelaran lewat {@code ais.action.master.KknAction} →
 * mahasiswa mendaftar lewat {@code ais.action.master.kkn.KknUntukMahasiswaAction} (menghasilkan
 * {@code MahasiswaDaftarKkn}) → panitia menyaring lewat
 * {@code ais.action.master.kkn.SeleksiPenerimaKknAction} → yang lolos dipecah ke kelompok lewat
 * {@code ais.action.master.kkn.KelompokKknAction} → agenda/pertemuan lapangan dikelola
 * {@code ais.action.master.helper.AktifitasKknHelper} dan
 * {@code ais.action.master.helper.RevisiPertemuanKknHelper} → nilai akhir dihitung
 * {@code ais.action.master.helper.PenilaianKknHelper} dan
 * {@code ais.database.model.MahasiswaDapatKelompokKkn}.</p>
 *
 * <h3>Tidak ada satu pun koleksi di sisi ini</h3>
 * <p>Perhatikan bahwa kelas ini <b>tidak memetakan {@code Set}/{@code List} apa pun</b>: relasi
 * ke kelompok, pendaftar, persyaratan, dan komponen penilaian seluruhnya berarah satu, dari anak
 * ke induk. Kode pemakainya selalu memuat sendiri dengan
 * {@code session.createCriteria(X.class).add(Restrictions.eq("kkn", kkn))}. Jadi menghapus baris
 * {@code kkn} tidak akan meng-<i>cascade</i> apa pun — data anaknya menjadi yatim, bukan ikut
 * terhapus.
 *
 * <h3>Aturan kelayakan pendaftar (di mana syarat SKS/IPK benar-benar dievaluasi)</h3>
 * <p>Field-field syarat di kelas ini <b>hanya</b> dibaca oleh dua tempat: layar penginputan
 * {@code KknAction} dan penjaga gerbang
 * {@code ais.common.Common.checkSyaratKkn(Mahasiswa, Kkn)}. Urutan pemeriksaan di sana:</p>
 * <ol>
 *   <li>Bila ada baris {@code PengecualianKknMahasiswa} untuk pasangan (kkn, mahasiswa) →
 *   <b>langsung lolos</b>, seluruh syarat berikutnya dilewati.</li>
 *   <li>{@link #getJurusan()} bila terisi harus <b>sama persis</b> dengan jurusan mahasiswa;
 *   {@link #getFakultas()} bila terisi harus sama dengan fakultas jurusan mahasiswa. Kosong
 *   berarti "semua".</li>
 *   <li>SKS kumulatif dan IPK diambil dari {@code Common.singkronkanKrsMahasiswa(...)}, lalu
 *   dibandingkan: lolos bila {@code sks >= }{@link #getMinimalSksBolehIkutKkn()}
 *   <b>DAN</b> {@code ipk >= }{@link #getMinimalIpkBolehIkutKkn()}.</li>
 *   <li>Bila gagal <b>DAN</b> {@link #getAktifkanSyaratLain()} bernilai {@code true}, pasangan
 *   kedua dicoba: {@code sks >= }{@link #getMinimalSksBolehIkutKkn2()} <b>DAN</b>
 *   {@code ipk >= }{@link #getMinimalIpkBolehIkutKkn2()}. Jadi hubungan antar-pasangan adalah
 *   <b>ATAU</b> (label di layar memang berbunyi "Atau juga minimal SKS dan IPK"), sedangkan di
 *   dalam satu pasangan adalah <b>DAN</b>.</li>
 * </ol>
 * <p>Gerbang yang sama dipakai tiga layar: tombol Simpan pada {@code KknUntukMahasiswaAction},
 * dialog penambahan peserta massal
 * {@code ais.action.master.helper.AmbilDataMahasiswaKknHelper}, dan dialog seleksi
 * {@code ais.action.master.helper.AmbilDataMahasiswaSeleksiKknHelper}.</p>
 *
 * <p>Penyaringan daftar KKN yang <i>terlihat</i> oleh mahasiswa dilakukan terpisah di
 * {@code KknUntukMahasiswaAction.initCriteria(boolean)} dengan pola
 * {@code Restrictions.or(isNull(kolom), eq(kolom, nilaiMahasiswa))} untuk {@code jurusan},
 * {@code fakultas}, dan {@code program} — sehingga kolom yang {@code null} berarti "berlaku
 * untuk semua". Perhatikan <b>asimetrinya</b>: {@code jurusan} dan {@code fakultas} diperiksa
 * dua kali (saat menyaring daftar <i>dan</i> saat menyimpan pendaftaran), sedangkan
 * {@link #getProgram()} <b>hanya</b> menyaring daftar — {@code checkSyaratKkn(...)} tidak
 * pernah membandingkannya. Jalur pendaftaran yang tidak lewat layar mahasiswa (mis. penambahan
 * peserta massal oleh operator) karena itu bisa menembus batas program.</p>
 *
 * <h3>Aturan pembayaran</h3>
 * <p>Tiga field bekerja bersama saat tombol Simpan pendaftaran ditekan:</p>
 * <ul>
 *   <li>{@link #getNimMhsTanpaBiaya()} — daftar NIM yang <b>dibebaskan dari kedua</b>
 *   pemeriksaan di bawah. Diperiksa paling dulu; bila NIM pendaftar ada di daftar ini,
 *   seluruh pemeriksaan biaya dilewati.</li>
 *   <li>{@link #getKodeItemBiaya()} — daftar kode {@code ItemBiaya} (dipisah koma) yang
 *   <b>seluruhnya</b> harus sudah punya minimal satu baris {@code CicilanPembayaran} atas nama
 *   mahasiswa (hubungan antar-kode adalah DAN).</li>
 *   <li>{@link #getHarusBayar()} — bila {@code true}, tagihan perkuliahan semester berjalan
 *   harus lunas menurut {@code Common.checkStatusPembayaranMahasiswa(...)}.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Bayangan field audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #getId()}. Lihat catatan
 *   arsitektur di bawah.</li>
 *   <li><b>Identitas &amp; jadwal</b> — {@link #getNama_kelompok()}, {@link #getNama()},
 *   {@link #toString()}, {@link #getKeterangan()}, {@link #getTanggal_mulai()},
 *   {@link #getTanggal_selesai()}.</li>
 *   <li><b>Lingkup peserta</b> — {@link #getFakultas()}, {@link #getJurusan()},
 *   {@link #getProgram()}.</li>
 *   <li><b>Syarat akademik</b> — {@link #getMinimalSksBolehIkutKkn()},
 *   {@link #getMinimalIpkBolehIkutKkn()}, {@link #getAktifkanSyaratLain()},
 *   {@link #getMinimalSksBolehIkutKkn2()}, {@link #getMinimalIpkBolehIkutKkn2()}.</li>
 *   <li><b>Syarat pembayaran</b> — {@link #getHarusBayar()}, {@link #getKodeItemBiaya()},
 *   {@link #getNimMhsTanpaBiaya()}.</li>
 *   <li><b>Konteks akademik turunan</b> — {@link #getSemester()},
 *   {@link #getTahunAkademik()}, keduanya diturunkan dari {@link #getTanggal_mulai()}.</li>
 *   <li><b>Pelaporan &amp; perizinan agenda</b> — {@link #getJenisAktfitasMahasiswa()}
 *   (kategori Feeder/PDDikti), {@link #getMahasiswaBolehMerubahAgenda()},
 *   {@link #getDosenBolehMerubahAgenda()}.</li>
 * </ol>
 * <p>Tidak ada method bisnis, query statis, maupun utilitas berkas di kelas ini; seluruh
 * anggotanya konstruktor, {@code toString()}, atau pasangan getter/setter properti. Logika
 * bisnis KKN hidup di {@code Common}, di {@code ais.action.master.kkn.*}, dan di
 * {@code ais.action.master.helper.*Kkn*}.</p>
 *
 * <h3>Catatan arsitektur: field audit dideklarasikan ulang di sini</h3>
 * <p>Kelas ini {@code extends} {@link GeneralValueObject}, tetapi induk tersebut <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * sama sekali tidak memetakan propertinya. Karena itu {@code id}, {@code oleh}, {@code olehId},
 * dan {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di setiap entity, termasuk di
 * sini. Ini keharusan teknis arsitektur, bukan kelalaian atau duplikasi yang perlu "dirapikan".
 * Kontrak umum method warisan ({@code check}, {@code udah}, {@code ambilData}, dan
 * kawan-kawan) didokumentasikan lengkap di {@link GeneralValueObject}.</p>
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 *   <li><b>Nama field {@code nama_kelompok} menyesatkan.</b> Meski namanya "kelompok", kolom
 *   ini menyimpan <b>nama gelaran KKN</b> (mis. "KKN Reguler Genap 2025/2026"), bukan nama
 *   kelompok/posko — nama kelompok yang sebenarnya ada di
 *   {@code ais.database.model.kkn.KelompokKkn}. {@link #toString()} dan {@link #getNama()}
 *   keduanya memakai kolom ini.</li>
 *   <li><b>Pemetaan berbasis properti, dan tidak ada satu pun {@code @Transient}.</b> Karena
 *   {@code @Id} dipasang pada getter {@link #getId()}, Hibernate membaca SELURUH getter sebagai
 *   kolom — termasuk yang sama sekali tanpa anotasi seperti {@link #getSemester()} atau
 *   {@link #getNimMhsTanpaBiaya()}. Konsekuensinya besar: <b>apa pun yang dikembalikan getter
 *   akan ikut tersimpan ke basis data</b> pada flush berikutnya lewat dirty checking, walau
 *   pemanggil hanya bermaksud "membaca". Ini berlaku baik untuk getter yang menugaskan ulang ke
 *   field-nya sendiri maupun untuk getter yang sekadar mengembalikan nilai bawaan tanpa
 *   penugasan.</li>
 *   <li><b>Getter yang menulis balik ke field — sudah diverifikasi ada 14</b>:
 *   {@link #getNama()}, {@link #getJurusan()}, {@link #getFakultas()},
 *   {@link #getJenisAktfitasMahasiswa()}, {@link #getMinimalSksBolehIkutKkn()},
 *   {@link #getMinimalIpkBolehIkutKkn()}, {@link #getMinimalSksBolehIkutKkn2()},
 *   {@link #getMinimalIpkBolehIkutKkn2()}, {@link #getAktifkanSyaratLain()},
 *   {@link #getHarusBayar()}, {@link #getKodeItemBiaya()}, {@link #getSemester()},
 *   {@link #getTahunAkademik()}, dan {@link #getNimMhsTanpaBiaya()}. Tiga di antaranya
 *   ({@code getJurusan}, {@code getFakultas}, {@code getJenisAktfitasMahasiswa}) "netral" —
 *   hanya menyeragamkan instance hasil {@link GeneralValueObject#check(Object)}; <b>sebelas
 *   sisanya benar-benar mengubah nilai data</b> dari {@code null} menjadi nilai bawaan atau
 *   nilai turunan.</li>
 *   <li><b>Getter yang tidak menulis balik namun tetap mengubah nilai tersimpan</b>:
 *   {@link #getProgram()} (mengubah string kosong menjadi {@code null}),
 *   {@link #getMahasiswaBolehMerubahAgenda()} dan {@link #getDosenBolehMerubahAgenda()}
 *   (mengubah {@code null} menjadi {@code true}), serta cabang fallback
 *   {@link #getJenisAktfitasMahasiswa()} (mengembalikan {@code ConstantValues.KKN} tanpa
 *   menugaskannya ke field). Field-nya memang tidak disentuh, tetapi karena akses properti,
 *   nilai hasil getter-lah yang dibandingkan dan disimpan Hibernate.</li>
 *   <li><b>Tidak ada field {@code aktif} sama sekali.</b> Sudah diperiksa menyeluruh: gelaran
 *   KKN tidak punya sakelar aktif/non-aktif satu-arah maupun dua-arah seperti banyak master
 *   lain di paket ini. Yang menentukan sebuah gelaran masih "terlihat" oleh mahasiswa adalah
 *   kecocokan {@code jurusan}/{@code fakultas}/{@code program} pada
 *   {@code KknUntukMahasiswaAction.initCriteria(boolean)} — bukan flag, dan bahkan bukan
 *   {@link #getTanggal_selesai()} (tanggal tidak ikut menyaring daftar; gelaran lama tetap
 *   muncul, hanya diurutkan paling bawah karena {@code Order.desc("tanggal_mulai")}).</li>
 *   <li><b>Tidak ada getter yang membuka atau menutup sesi Hibernate.</b> Sudah diperiksa
 *   menyeluruh: kelas ini tidak mengimpor {@code Session}/{@code HibernateUtil} dan tidak
 *   menjalankan query apa pun. Satu-satunya sentuhan ke lapisan persistensi adalah
 *   {@code check()} pada tiga getter relasi, yang pengelolaan sesinya ditangani sepenuhnya di
 *   dalam {@link GeneralValueObject}.</li>
 *   <li><b>Jebakan konfigurasi: "Aktifkan Syarat Lain" bisa mematikan syarat, bukan
 *   menambahnya.</b> {@link #getMinimalSksBolehIkutKkn2()} dan
 *   {@link #getMinimalIpkBolehIkutKkn2()} mengganti {@code null} dengan <b>0</b> dan
 *   <b>0.0</b>, sedangkan nilai awal field-nya 110 dan 2.0. Pada baris lama yang kolom
 *   syarat-2-nya masih {@code null}, mencentang "Aktifkan Syarat Lain" tanpa mengisi angkanya
 *   membuat cabang kedua berbunyi {@code sks >= 0 && ipk >= 0.0} — yaitu <b>semua mahasiswa
 *   lolos</b>, termasuk yang gagal di syarat pertama. Lihat catatan pada masing-masing
 *   method.</li>
 *   <li><b>Asimetri nilai bawaan.</b> Syarat pertama jatuh ke 100 SKS / IPK 3.0 bila
 *   {@code null}; syarat kedua diinisialisasi 110 SKS / IPK 2.0 pada instance baru tetapi
 *   jatuh ke 0 / 0.0 bila {@code null}. Tidak ada satu pun angka ini yang berasal dari
 *   konfigurasi — semuanya hardcoded di getter.</li>
 *   <li><b>{@code ConstantValues.KKN} adalah entity bersama yang mutable dan bisa
 *   {@code null}.</b> Ia bukan konstanta melainkan field statis non-final yang baru terisi
 *   ketika {@code ais.common.InitDataHelper} selesai menyemai tabel
 *   {@code jenis_aktivitas_mahasiswa}. Baris masternya dicari berdasarkan {@code feeder = 5},
 *   tetapi penugasan ke {@code ConstantValues.KKN} bergantung pada <b>nama yang persis</b>
 *   {@code "Kuliah kerja nyata"} — bila operator pernah mengubah nama baris itu, konstanta
 *   tersebut tetap {@code null} selamanya. Sebelum {@code InitData} rampung (atau pada kasus
 *   nama berubah tadi), {@link #getJenisAktfitasMahasiswa()} mengembalikan {@code null}, dan
 *   {@code KknAction} yang langsung merangkai
 *   {@code kkn.getJenisAktfitasMahasiswa().getKampusMerderka()} akan melempar
 *   {@code NullPointerException}. Instance yang dikembalikan juga dipakai bersama seluruh
 *   aplikasi dan berasal dari sesi Hibernate lain.</li>
 *   <li><b>Ketidakcocokan ambang bulan antara {@link #getSemester()} dan
 *   {@link #getTahunAkademik()}.</b> Bila kalender akademik ({@code RencanaTahunAkademik})
 *   tidak ditemukan, keduanya jatuh ke perhitungan kalender di
 *   {@code ais.common.CommonCurrentSessionHelper} yang memakai ambang <i>berbeda</i>:
 *   semester ganjil dimulai pada {@code Calendar.MONTH >= 5} (Juni), sedangkan tahun akademik
 *   maju ke {@code YYYY/YYYY+1} baru pada {@code Calendar.MONTH > 5} (Juli). Gelaran KKN yang
 *   {@link #getTanggal_mulai()}-nya jatuh di bulan <b>Juni</b> dan kedua kolomnya masih
 *   {@code null} akan terisi otomatis dengan pasangan yang mustahil: semester Ganjil tetapi
 *   tahun akademik periode sebelumnya. Kedua nilai itu langsung ikut tersimpan (lihat butir
 *   getter penulis-balik) dan dipakai lagi oleh {@code Common.checkSyaratKkn(...)} untuk
 *   menghitung semester mahasiswa.</li>
 *   <li><b>Nilai turunan bergantung pada pengguna yang sedang login.</b>
 *   {@code Common.isNowSemensterGanjil(Date)} memanggil {@code Common.getCurrentUser()} untuk
 *   memilih {@code RencanaTahunAkademik} yang berlaku. Getter {@link #getSemester()} dan
 *   {@link #getTahunAkademik()} karena itu bisa menghasilkan nilai berbeda bila dipanggil dari
 *   proses latar/penjadwal yang tidak punya sesi pengguna.</li>
 *   <li><b>Kolom {@code kkn} pada {@code KelompokKkn} boleh {@code null}.</b> Kelompok yatim
 *   ({@code kelompokKkn.getKkn() == null}) sah secara pemetaan, tetapi
 *   {@code MahasiswaDapatKelompokKkn} memuat komponen penilaiannya dengan
 *   {@code Restrictions.eq("kkn", kelompokKkn.getKkn())} dan {@code AktifitasKknHelper}
 *   membaca sakelar agenda lewat rantai yang sama — jadi kelompok tanpa induk akan kehilangan
 *   seluruh komponen nilai dan izin agendanya.</li>
 *   <li><b>Kembaran hampir persis di modul lain.</b> {@code ais.database.model.Pkl} (kerja
 *   praktik/PKL) adalah salinan struktur kelas ini, lengkap dengan pasangan syarat ganda,
 *   {@code aktifkanSyaratLain}, dan {@code Common.checkSyaratPkl(...)} yang sengaja disebut
 *   dalam dokumentasinya sebagai "copy semantis dari {@code checkSyaratKkn}". Perbaikan di sini
 *   hampir selalu perlu ditiru ke sana, dan sebaliknya.</li>
 * </ul>
 *
 * <p>Entity ini {@code @Audited} (Envers, tabel riwayat {@code kkn_AUD}) dan memakai
 * {@code dynamicInsert}/{@code dynamicUpdate} sehingga hanya kolom yang benar-benar berubah
 * yang ikut dalam pernyataan SQL.</p>
 *
 * @see GeneralValueObject
 * @see ais.database.model.kkn.KelompokKkn
 * @see ais.database.model.kkn.MahasiswaDaftarKkn
 * @see MahasiswaDapatKkn
 * @see MahasiswaDapatKelompokKkn
 * @see PengecualianKknMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kkn")

public class Kkn extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja dibiarkan sama dengan sejumlah entity lain hasil
	 * generate {@code hbm2java} pada 2010; jangan diubah agar objek yang pernah diserialisasi
	 * (mis. ke dalam sesi ZK yang dipersistensi) tetap bisa dibaca.
	 */
	private static final long serialVersionUID = 2413821571548439808L;

	/** Kunci utama tabel {@code public.kkn}; dibangkitkan basis data ({@code IDENTITY}). */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String oleh;

	/** ID pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna yang terakhir mengubah baris ini (bayangan field audit).
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna yang mengubah baris ini.
	 *
	 * <p><b>Satu arah:</b> masukan {@code null} atau kosong/spasi diabaikan diam-diam
	 * ({@code return} lebih awal), sehingga nilai audit yang sudah ada <b>tidak bisa dihapus</b>
	 * lewat setter ini.</p>
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh apa pun
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan kosong/{@code null} diabaikan
	 * diam-diam sehingga nilai audit lama tidak bisa terhapus.</p>
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh apa pun
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini (bayangan field audit).
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} dijalankan,
	 * meneruskan ke {@code AuditTimestampInterceptor.ubah(this)} agar {@code oleh},
	 * {@code olehId}, dan {@code tanggal_dirubah} terisi dari konteks pengguna yang sedang aktif.
	 *
	 * <p>Tidak untuk dipanggil langsung dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat dan diperbarui
	 * {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity ini, dipakai ZK untuk melabeli combobox/listbox gelaran KKN dan
	 * ikut menyusun nama pendaftaran ({@code MahasiswaDaftarKkn.nama} berbentuk
	 * {@code "<mahasiswa>--><nama kkn>"}).
	 *
	 * <p><b>Membaca field {@code nama_kelompok} secara langsung</b>, bukan lewat
	 * {@link #getNama_kelompok()} — perbedaannya tidak berdampak karena getter tersebut juga
	 * hanya mengembalikan field apa adanya. Dapat mengembalikan {@code null} bila gelaran belum
	 * diberi nama; pemanggil yang merangkainya ke dalam string akan menampilkan
	 * {@code "null"}.</p>
	 *
	 * @return nama gelaran KKN, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return nama_kelompok;
	}

	/**
	 * Salinan denormalisasi {@link #getNama_kelompok()}; kolom {@code nama} pada tabel. Selalu
	 * ditimpa ulang oleh {@link #getNama()}, jadi jangan pernah diisi terpisah.
	 */
	private String nama;

	/**
	 * Mengembalikan nama gelaran KKN, sekaligus <b>menyalin ulang</b> nilai
	 * {@link #getNama_kelompok()} ke field {@code nama}.
	 *
	 * <p><b>Efek samping:</b> ini salah satu getter yang menulis ke field-nya sendiri. Karena
	 * pemetaan berbasis properti (lihat Javadoc kelas), sekadar memanggil getter ini sudah
	 * membuat kolom {@code nama} pada basis data disamakan dengan kolom {@code nama_kelompok}
	 * pada flush berikutnya. Apa pun yang pernah ditulis {@link #setNama(String)} akan hilang
	 * pada pemanggilan pertama getter ini.</p>
	 *
	 * <p>Dipakai antara lain oleh {@code KknUntukMahasiswaAction.daftar(Kkn)} saat menyusun
	 * label pendaftaran.</p>
	 *
	 * @return nama gelaran KKN (isi {@code nama_kelompok}), bisa {@code null}
	 */
	public String getNama() {
		nama = getNama_kelompok();
		return nama;
	}

	/**
	 * Menetapkan kolom {@code nama}.
	 *
	 * <p><b>Praktis tidak berguna:</b> nilai apa pun yang diberikan di sini akan ditimpa pada
	 * pemanggilan {@link #getNama()} berikutnya. Untuk mengubah nama gelaran, pakai
	 * {@link #setNama_kelompok(String)}.</p>
	 *
	 * @param nama nama gelaran; akan segera ditimpa oleh {@link #getNama()}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * <b>Nama gelaran KKN</b> — bukan nama kelompok, meski namanya berbunyi demikian (lihat
	 * catatan pada Javadoc kelas). Sumber tunggal bagi {@link #toString()} dan
	 * {@link #getNama()}.
	 */
	private String nama_kelompok;

	/**
	 * Tanggal mulai gelaran KKN. Selain sebagai informasi jadwal, tanggal inilah yang menjadi
	 * acuan penurunan {@link #getSemester()} dan {@link #getTahunAkademik()}, dan menjadi kunci
	 * pengurutan daftar KKN ({@code Order.desc("tanggal_mulai")}).
	 */
	private Date tanggal_mulai = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Tanggal selesai gelaran KKN. Bersifat informatif: <b>tidak</b> dipakai untuk menutup
	 * pendaftaran maupun menyaring daftar KKN yang terlihat mahasiswa.
	 */
	private Date tanggal_selesai = ais.ui.util.WaktuUtil.getDate();

	/** Catatan/keterangan bebas tentang gelaran KKN ini. */
	private String keterangan;

	/**
	 * Pembatas lingkup peserta pada tingkat fakultas; {@code null} berarti "semua fakultas".
	 * Dievaluasi di {@code Common.checkSyaratKkn(...)} dan
	 * {@code KknUntukMahasiswaAction.initCriteria(boolean)}.
	 */
	private Fakultas fakultas;

	/**
	 * Pembatas lingkup peserta pada tingkat jurusan/program studi; {@code null} berarti "semua
	 * jurusan". Bila terisi bersamaan dengan {@link #fakultas}, keduanya harus cocok.
	 */
	private Jurusan jurusan;

	/**
	 * Pembatas lingkup peserta berdasarkan <b>program/jenjang pendidikan</b> — disimpan sebagai
	 * String yang sama dengan kolom {@code Mahasiswa.program}, bukan sebagai relasi ke entity
	 * {@code Program}. {@code null}/kosong berarti "semua program" dan ditampilkan sebagai
	 * "Semua" di layar. Berbeda dari {@link #jurusan}/{@link #fakultas}, field ini hanya
	 * menyaring daftar KKN yang terlihat mahasiswa dan <b>tidak</b> diperiksa ulang saat
	 * pendaftaran disimpan.
	 */
	private String program;

	/** Ambang SKS kumulatif pasangan syarat PERTAMA; {@code null} berarti pakai bawaan 100. */
	private Integer minimalSksBolehIkutKkn;

	/** Ambang IPK pasangan syarat PERTAMA; {@code null} berarti pakai bawaan 3.0. */
	private Double minimalIpkBolehIkutKkn;

	/**
	 * Sakelar pengaktif pasangan syarat KEDUA (label layar: "Aktifkan Syarat Lain"). Bila
	 * {@code false}, {@link #minimalSksBolehIkutKkn2}/{@link #minimalIpkBolehIkutKkn2} tidak
	 * pernah dievaluasi dan kotak isiannya dinonaktifkan di layar.
	 */
	private Boolean aktifkanSyaratLain;

	/**
	 * Ambang SKS kumulatif pasangan syarat KEDUA. Nilai awal instance baru 110, tetapi
	 * {@link #getMinimalSksBolehIkutKkn2()} menggantinya dengan <b>0</b> bila kolomnya
	 * {@code null} di basis data.
	 */
	private Integer minimalSksBolehIkutKkn2 = 110;

	/**
	 * Ambang IPK pasangan syarat KEDUA. Nilai awal instance baru 2.0, tetapi
	 * {@link #getMinimalIpkBolehIkutKkn2()} menggantinya dengan <b>0.0</b> bila kolomnya
	 * {@code null} di basis data.
	 */
	private Double minimalIpkBolehIkutKkn2 = 2.0;

	/**
	 * Daftar kode {@code ItemBiaya} (dipisah koma) yang harus sudah pernah dibayar pendaftar.
	 * Kosong berarti pemeriksaan ini dilewati.
	 */
	private String kodeItemBiaya;

	/**
	 * Daftar NIM (dipisah koma) yang dibebaskan dari SELURUH pemeriksaan biaya —
	 * {@link #kodeItemBiaya} maupun {@link #harusBayar}. Dinormalkan menjadi bentuk
	 * {@code ",nim1,nim2,"} oleh {@link #getNimMhsTanpaBiaya()}.
	 */
	private String nimMhsTanpaBiaya;

	/**
	 * Bila {@code true}, pendaftar harus lunas tagihan perkuliahan semester berjalan menurut
	 * {@code Common.checkStatusPembayaranMahasiswa(...)}.
	 */
	private Boolean harusBayar;

	/**
	 * Semester penyelenggaraan ({@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}). Diisi
	 * otomatis dari {@link #tanggal_mulai} oleh {@link #getSemester()} bila masih {@code null}.
	 */
	private String semester;

	/**
	 * Tahun akademik penyelenggaraan dalam format {@code "2025/2026"}. Diisi otomatis dari
	 * {@link #tanggal_mulai} oleh {@link #getTahunAkademik()} bila masih {@code null}.
	 *
	 * <p><b>Format wajib:</b> {@code Common.checkSyaratKkn(...)} memanggil
	 * {@code Integer.parseInt(StringUtils.split(ta, "/")[0])} atas nilai ini — isian yang tidak
	 * mengandung tahun angka di depan tanda garis miring akan melempar
	 * {@code NumberFormatException} dan menggagalkan seluruh pendaftaran.</p>
	 */
	private String tahunAkademik;

	/**
	 * Izin bagi mahasiswa untuk mengubah agenda/aktivitas KKN kelompoknya.
	 * {@code null} diperlakukan sebagai {@code true} (diizinkan).
	 */
	private Boolean mahasiswaBolehMerubahAgenda;

	/**
	 * Izin bagi dosen pembimbing untuk mengubah agenda/aktivitas KKN kelompok bimbingannya.
	 * {@code null} diperlakukan sebagai {@code true} (diizinkan).
	 */
	private Boolean dosenBolehMerubahAgenda;

	/**
	 * Kategori aktivitas mahasiswa untuk pelaporan Feeder/PDDikti. Bila {@code null},
	 * {@link #getJenisAktfitasMahasiswa()} mengembalikan {@code ConstantValues.KKN}
	 * ("Kuliah kerja nyata", Feeder id 5).
	 */
	private JenisAktfitasMahasiswa jenisAktfitasMahasiswa;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA sekaligus dipakai layar
	 * {@code KknAction} saat membuat gelaran baru.
	 *
	 * <p>Perhatikan bahwa beberapa field sudah punya nilai awal lewat inisialisasi deklarasi:
	 * {@link #tanggal_mulai} dan {@link #tanggal_selesai} diisi waktu server saat ini,
	 * {@link #minimalSksBolehIkutKkn2} diisi 110, dan {@link #minimalIpkBolehIkutKkn2} diisi
	 * 2.0.</p>
	 */
	public Kkn() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Anotasi {@code @Id} yang menempel di getter inilah yang membuat seluruh kelas memakai
	 * <b>akses properti</b>; itulah sebabnya setiap getter di kelas ini menjadi kolom, dengan
	 * segala konsekuensi yang dijabarkan pada Javadoc kelas.</p>
	 *
	 * @return ID basis data, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Umumnya hanya dipanggil Hibernate; kode aplikasi tidak perlu
	 * mengisinya sendiri karena kolomnya {@code IDENTITY} dan {@code insertable = false}.
	 *
	 * @param id ID basis data
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan nama gelaran KKN (kolom {@code nama_kelompok}).
	 *
	 * @param nama_kelompok nama gelaran, mis. "KKN Reguler Genap 2025/2026"
	 */
	public void setNama_kelompok(String nama_kelompok) {
		this.nama_kelompok = nama_kelompok;
	}

	/**
	 * Mengembalikan nama gelaran KKN apa adanya, tanpa efek samping.
	 *
	 * @return nama gelaran, atau {@code null} bila belum diisi
	 * @see #getNama()
	 */
	public String getNama_kelompok() {
		return nama_kelompok;
	}

	/**
	 * Menetapkan tanggal mulai gelaran.
	 *
	 * <p>Perubahan tanggal ini <b>tidak</b> otomatis memperbarui {@link #getSemester()} maupun
	 * {@link #getTahunAkademik()} yang sudah pernah terisi — keduanya hanya diturunkan saat
	 * kolomnya masih {@code null}.</p>
	 *
	 * @param tanggal_mulai tanggal mulai; boleh {@code null}
	 */
	public void setTanggal_mulai(Date tanggal_mulai) {
		this.tanggal_mulai = tanggal_mulai;
	}

	/**
	 * Mengembalikan tanggal mulai gelaran KKN.
	 *
	 * @return tanggal mulai (bawaan: waktu pembuatan objek)
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal_mulai() {
		return tanggal_mulai;
	}

	/**
	 * Menetapkan tanggal selesai gelaran.
	 *
	 * @param tanggal_selesai tanggal selesai; boleh {@code null}
	 */
	public void setTanggal_selesai(Date tanggal_selesai) {
		this.tanggal_selesai = tanggal_selesai;
	}

	/**
	 * Mengembalikan tanggal selesai gelaran KKN.
	 *
	 * <p>Nilai ini murni informatif — tidak ada satu pun pemeriksaan kelayakan yang
	 * membandingkan tanggal hari ini dengannya, sehingga pendaftaran <b>tidak</b> tertutup
	 * dengan sendirinya setelah tanggal ini lewat.</p>
	 *
	 * @return tanggal selesai (bawaan: waktu pembuatan objek)
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal_selesai() {
		return tanggal_selesai;
	}

	/**
	 * Menetapkan keterangan bebas gelaran KKN.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan keterangan bebas gelaran KKN apa adanya.
	 *
	 * @return teks keterangan, atau {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan pembatas jurusan/program studi peserta.
	 *
	 * @param jurusan jurusan yang boleh ikut; {@code null} berarti semua jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan pembatas jurusan peserta, setelah proxy lazy-nya diresolusi.
	 *
	 * <p><b>Efek samping:</b> hasil {@link GeneralValueObject#check(Object)} ditugaskan kembali
	 * ke field {@code jurusan}. Penulisan ini "netral" — yang berubah hanyalah instance mana
	 * yang dipegang, bukan baris mana yang dirujuk — tetapi tetap perlu diketahui bahwa getter
	 * ini bisa menyentuh cache atau membuka sesi baca sendiri lewat {@code check()}.</p>
	 *
	 * <p>Dipakai {@code Common.checkSyaratKkn(...)} sebagai gerbang pertama: bila terisi dan
	 * berbeda dari jurusan mahasiswa, pendaftaran ditolak dengan pesan eksplisit.</p>
	 *
	 * @return jurusan pembatas, atau {@code null} bila gelaran terbuka untuk semua jurusan
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan pembatas fakultas peserta.
	 *
	 * @param fakultas fakultas yang boleh ikut; {@code null} berarti semua fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan pembatas fakultas peserta, setelah proxy lazy-nya diresolusi.
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getJurusan()} — hasil {@code check()}
	 * ditugaskan kembali ke field.</p>
	 *
	 * <p>Pada {@code Common.checkSyaratKkn(...)} nilai ini dibandingkan dengan
	 * {@code mahasiswa.getJurusan().getFakultas()}, bukan dengan fakultas mahasiswa secara
	 * langsung.</p>
	 *
	 * @return fakultas pembatas, atau {@code null} bila gelaran terbuka untuk semua fakultas
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Mengembalikan ambang SKS kumulatif pasangan syarat PERTAMA.
	 *
	 * <p><b>Efek samping:</b> bila kolomnya masih {@code null}, field diisi <b>100</b> dan
	 * nilai itu ikut tersimpan ke basis data pada flush berikutnya. Angka 100 hardcoded di
	 * sini, bukan dari konfigurasi.</p>
	 *
	 * <p>Dibaca {@code Common.checkSyaratKkn(...)} — mahasiswa lolos syarat pertama bila SKS
	 * kumulatifnya (dari {@code Common.singkronkanKrsMahasiswa(...)}) lebih besar atau sama
	 * dengan nilai ini <b>dan</b> IPK-nya memenuhi {@link #getMinimalIpkBolehIkutKkn()}.</p>
	 *
	 * @return ambang SKS syarat pertama; tidak pernah {@code null}
	 */
	public Integer getMinimalSksBolehIkutKkn() {
		if (minimalSksBolehIkutKkn == null) {
			minimalSksBolehIkutKkn = 100;
		}
		return minimalSksBolehIkutKkn;
	}

	/**
	 * Menetapkan ambang SKS kumulatif pasangan syarat PERTAMA.
	 *
	 * @param minimalSksBolehIkutKkn ambang SKS; {@code null} akan diganti 100 saat dibaca
	 */
	public void setMinimalSksBolehIkutKkn(Integer minimalSksBolehIkutKkn) {
		this.minimalSksBolehIkutKkn = minimalSksBolehIkutKkn;
	}

	/**
	 * Mengembalikan ambang IPK pasangan syarat PERTAMA.
	 *
	 * <p><b>Efek samping:</b> bila kolomnya masih {@code null}, field diisi <b>3.0</b> dan ikut
	 * tersimpan pada flush berikutnya. Perhatikan bahwa 3.0 adalah ambang yang cukup tinggi —
	 * gelaran lama yang kolomnya masih kosong akan tiba-tiba menolak sebagian besar pendaftar
	 * begitu getter ini pertama kali dipanggil.</p>
	 *
	 * @return ambang IPK syarat pertama; tidak pernah {@code null}
	 */
	public Double getMinimalIpkBolehIkutKkn() {
		if (minimalIpkBolehIkutKkn == null) {
			minimalIpkBolehIkutKkn = 3.0;
		}
		return minimalIpkBolehIkutKkn;
	}

	/**
	 * Menetapkan ambang IPK pasangan syarat PERTAMA.
	 *
	 * @param minimalIpkBolehIkutKkn ambang IPK; {@code null} akan diganti 3.0 saat dibaca
	 */
	public void setMinimalIpkBolehIkutKkn(Double minimalIpkBolehIkutKkn) {
		this.minimalIpkBolehIkutKkn = minimalIpkBolehIkutKkn;
	}

	/**
	 * Mengembalikan ambang SKS kumulatif pasangan syarat KEDUA (alternatif).
	 *
	 * <p><b>Efek samping &amp; jebakan:</b> bila kolomnya masih {@code null}, field diisi
	 * <b>0</b> — bukan 110 seperti nilai awal deklarasi field. Dipadukan dengan
	 * {@link #getMinimalIpkBolehIkutKkn2()} yang jatuh ke 0.0, mencentang
	 * {@link #getAktifkanSyaratLain()} pada gelaran lama tanpa mengisi angkanya membuat cabang
	 * alternatif berbunyi {@code sks >= 0 && ipk >= 0.0}, yaitu <b>meloloskan semua
	 * pendaftar</b>. Nilai 110 hanya berlaku untuk instance yang baru dibuat di memori dan
	 * belum pernah disimpan.</p>
	 *
	 * @return ambang SKS syarat kedua; tidak pernah {@code null}
	 */
	public Integer getMinimalSksBolehIkutKkn2() {
		if (minimalSksBolehIkutKkn2 == null) {
			minimalSksBolehIkutKkn2 = 0;
		}
		return minimalSksBolehIkutKkn2;
	}

	/**
	 * Menetapkan ambang SKS kumulatif pasangan syarat KEDUA.
	 *
	 * @param minimalSksBolehIkutKkn2 ambang SKS alternatif; {@code null} akan diganti 0 saat
	 *                                dibaca
	 */
	public void setMinimalSksBolehIkutKkn2(Integer minimalSksBolehIkutKkn2) {
		this.minimalSksBolehIkutKkn2 = minimalSksBolehIkutKkn2;
	}

	/**
	 * Mengembalikan ambang IPK pasangan syarat KEDUA (alternatif).
	 *
	 * <p><b>Efek samping &amp; jebakan:</b> identik dengan
	 * {@link #getMinimalSksBolehIkutKkn2()} — {@code null} menjadi <b>0.0</b>, bukan 2.0
	 * seperti nilai awal deklarasi field.</p>
	 *
	 * @return ambang IPK syarat kedua; tidak pernah {@code null}
	 */
	public Double getMinimalIpkBolehIkutKkn2() {
		if (minimalIpkBolehIkutKkn2 == null) {
			minimalIpkBolehIkutKkn2 = 0.0;
		}
		return minimalIpkBolehIkutKkn2;
	}

	/**
	 * Menetapkan ambang IPK pasangan syarat KEDUA.
	 *
	 * @param minimalIpkBolehIkutKkn2 ambang IPK alternatif; {@code null} akan diganti 0.0 saat
	 *                                dibaca
	 */
	public void setMinimalIpkBolehIkutKkn2(Double minimalIpkBolehIkutKkn2) {
		this.minimalIpkBolehIkutKkn2 = minimalIpkBolehIkutKkn2;
	}

	/**
	 * Mengembalikan sakelar pengaktif pasangan syarat KEDUA.
	 *
	 * <p><b>Efek samping:</b> {@code null} diganti {@code false} dan ditulis balik ke field.</p>
	 *
	 * <p>Di layar {@code KknAction}, nilai ini juga mengendalikan {@code setDisabled(...)} pada
	 * kotak isian SKS/IPK kedua lewat sebuah {@code EventListener onClick}.</p>
	 *
	 * @return {@code true} bila pasangan syarat kedua ikut dievaluasi
	 */
	public Boolean getAktifkanSyaratLain() {
		if (aktifkanSyaratLain == null) {
			aktifkanSyaratLain = false;
		}
		return aktifkanSyaratLain;
	}

	/**
	 * Menetapkan sakelar pengaktif pasangan syarat KEDUA.
	 *
	 * @param aktifkanSyaratLain {@code true} untuk mengaktifkan syarat alternatif
	 */
	public void setAktifkanSyaratLain(Boolean aktifkanSyaratLain) {
		this.aktifkanSyaratLain = aktifkanSyaratLain;
	}

	/**
	 * Mengembalikan sakelar "Harus telah membayar".
	 *
	 * <p><b>Efek samping:</b> {@code null} diganti {@code false} dan ditulis balik ke field —
	 * artinya gelaran lama yang kolomnya kosong secara permanen menjadi "tidak wajib
	 * lunas".</p>
	 *
	 * <p>Bila {@code true}, tombol Simpan pendaftaran memanggil
	 * {@code Common.checkStatusPembayaranMahasiswa(...)} untuk semester berjalan dan menolak
	 * pendaftar yang belum lunas. Pemeriksaan ini <b>dilewati sepenuhnya</b> bila NIM pendaftar
	 * tercantum di {@link #getNimMhsTanpaBiaya()}.</p>
	 *
	 * @return {@code true} bila tagihan perkuliahan harus lunas dulu
	 */
	public Boolean getHarusBayar() {
		if (harusBayar == null) {
			harusBayar = false;
		}
		return harusBayar;
	}

	/**
	 * Menetapkan sakelar "Harus telah membayar".
	 *
	 * @param harusBayar {@code true} bila tagihan perkuliahan harus lunas dulu
	 */
	public void setHarusBayar(Boolean harusBayar) {
		this.harusBayar = harusBayar;
	}

	/**
	 * Mengembalikan daftar kode {@code ItemBiaya} yang wajib sudah dibayar pendaftar.
	 *
	 * <p><b>Efek samping:</b> {@code null} diganti string kosong dan ditulis balik ke field.</p>
	 *
	 * <p>Formatnya daftar kode dipisah koma. Tombol Simpan pendaftaran mem-{@code split(",")}
	 * nilai ini, mencari {@code ItemBiaya} dengan {@code kode} yang sama, lalu menghitung baris
	 * {@code CicilanPembayaran} milik mahasiswa untuk item tersebut; nol baris berarti
	 * pendaftaran ditolak. Kode yang tidak ditemukan di tabel {@code ItemBiaya} <b>diabaikan
	 * diam-diam</b> — salah ketik kode berarti syarat itu hilang tanpa peringatan apa pun.</p>
	 *
	 * @return daftar kode item biaya dipisah koma; tidak pernah {@code null}
	 */
	public String getKodeItemBiaya() {
		if (kodeItemBiaya == null) {
			kodeItemBiaya = "";
		}
		return kodeItemBiaya;
	}

	/**
	 * Menetapkan daftar kode {@code ItemBiaya} yang wajib sudah dibayar pendaftar.
	 *
	 * @param kodeItemBiaya daftar kode dipisah koma; {@code null} akan diganti string kosong
	 *                      saat dibaca
	 */
	public void setKodeItemBiaya(String kodeItemBiaya) {
		this.kodeItemBiaya = kodeItemBiaya;
	}

	/**
	 * Mengembalikan semester penyelenggaraan gelaran KKN.
	 *
	 * <p><b>Efek samping:</b> bila kolomnya masih {@code null}, nilainya diturunkan dari
	 * {@link #getTanggal_mulai()} lewat {@code Common.isNowSemensterGanjil(Date)} menjadi
	 * {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}, lalu ditulis balik ke field
	 * sehingga ikut tersimpan pada flush berikutnya. Penurunan ini hanya terjadi <b>sekali</b>:
	 * setelah kolom terisi, mengubah tanggal mulai tidak akan memperbaruinya.</p>
	 *
	 * <p><b>Cara penurunannya.</b> {@code Common.isNowSemensterGanjil(Date)} lebih dulu mencari
	 * baris {@code RencanaTahunAkademik} (kalender akademik yang dikonfigurasi kampus) yang
	 * berlaku bagi pengguna yang sedang login pada tanggal tersebut; bila ketemu, semesternya
	 * dipakai apa adanya. Bila tidak, ia jatuh ke perhitungan kalender polos:
	 * {@code Calendar.MONTH >= 5}, yaitu <b>Juni sampai Desember</b> dianggap Ganjil. Karena
	 * cabang pertama bergantung pada pengguna aktif, hasilnya bisa berbeda bila getter ini
	 * dipanggil dari proses latar tanpa sesi pengguna.</p>
	 *
	 * <p><b>Tidak selaras dengan {@link #getTahunAkademik()} untuk bulan Juni</b> — lihat
	 * catatan ambang bulan pada Javadoc kelas.</p>
	 *
	 * <p>Dipakai {@code Common.checkSyaratKkn(...)} dan
	 * {@code KknUntukMahasiswaAction} untuk menghitung semester ke berapa mahasiswa berada saat
	 * gelaran ini berlangsung.</p>
	 *
	 * @return {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}
	 */
	public String getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil(getTanggal_mulai()) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return semester;
	}

	/**
	 * Menetapkan semester penyelenggaraan.
	 *
	 * @param semester {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tahun akademik penyelenggaraan gelaran KKN.
	 *
	 * <p><b>Efek samping:</b> bila kolomnya masih {@code null}, nilainya diturunkan dari
	 * {@link #getTanggal_mulai()} lewat {@code Common.getCurrentTahunAkademik(Date)} dan
	 * ditulis balik ke field. Sama seperti {@link #getSemester()}, penurunan hanya terjadi
	 * sekali.</p>
	 *
	 * <p><b>Cara penurunannya.</b> Bila ada {@code RencanaTahunAkademik} yang berlaku,
	 * {@code getNama()}-nya dipakai langsung. Bila tidak, dipakai perhitungan kalender
	 * {@code Calendar.MONTH > 5} → {@code YYYY/YYYY+1}, selain itu {@code YYYY-1/YYYY} —
	 * artinya batas majunya jatuh di <b>Juli</b>, satu bulan lebih lambat daripada batas
	 * semester ganjil di {@link #getSemester()}. Lihat catatan ambang bulan pada Javadoc
	 * kelas.</p>
	 *
	 * <p><b>Format penting:</b> {@code Common.checkSyaratKkn(...)} memecah nilai ini dengan
	 * pemisah {@code "/"} dan mem-{@code parseInt} potongan pertamanya. Nilai yang tidak
	 * berbentuk {@code "<tahun>/<tahun>"} akan melempar {@code NumberFormatException} yang
	 * menggagalkan seluruh pemeriksaan kelayakan, bukan sekadar bagian tahun akademiknya.</p>
	 *
	 * @return tahun akademik, mis. {@code "2025/2026"}
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik(getTanggal_mulai());
		}
		return tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik penyelenggaraan.
	 *
	 * @param tahunAkademik tahun akademik dalam format {@code "2025/2026"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan pembatas program/jenjang pendidikan peserta, sudah dipangkas spasinya.
	 *
	 * <p>Berbeda dari getter bernilai bawaan lain di kelas ini, getter ini <b>tidak</b> menulis
	 * balik ke field: string kosong hanya <i>dilaporkan</i> sebagai {@code null}. Karena
	 * pemetaan berbasis properti, nilai {@code null} itulah yang tetap ikut tersimpan pada
	 * flush berikutnya, sehingga kolom yang semula berisi spasi akan "membersihkan diri"
	 * menjadi {@code NULL}. Itu justru yang diinginkan, sebab
	 * {@code KknUntukMahasiswaAction.initCriteria(boolean)} menyaring dengan
	 * {@code Restrictions.or(isNull("program"), eq("program", mahasiswa.getProgram()))} —
	 * kolom berisi spasi tidak akan pernah cocok dengan cabang {@code isNull}.</p>
	 *
	 * <p>Di layar, nilai {@code null}/kosong ditampilkan sebagai <b>"Semua"</b>. Pilihan
	 * isiannya berasal dari {@code Common.initPrograms(...)} yang membaca cache
	 * {@code Common.programs} (label diambil dari {@code Program.getNamaBaru()}, nilai yang
	 * disimpan adalah kunci string-nya), dan comboboxnya dipasang {@code setReadonly(true)}
	 * sehingga operator hanya bisa memilih, tidak mengetik bebas.</p>
	 *
	 * <p><b>Tidak ikut divalidasi saat menyimpan pendaftaran</b> —
	 * {@code Common.checkSyaratKkn(...)} hanya memeriksa jurusan dan fakultas.</p>
	 *
	 * @return nama program tanpa spasi pinggir, atau {@code null} bila berlaku untuk semua
	 *         program
	 */
	public String getProgram() {
		return program == null || program.trim().isEmpty() ? null : program.trim();
	}

	/**
	 * Menetapkan pembatas program/kelas perkuliahan peserta.
	 *
	 * @param program nama program (mis. "Reguler"); {@code null}/kosong berarti semua program
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan daftar NIM yang dibebaskan dari seluruh pemeriksaan biaya, dalam bentuk
	 * <b>ternormalisasi</b> {@code ",nim1,nim2,"} (selalu diapit koma, tanpa koma ganda).
	 *
	 * <p><b>Efek samping:</b> getter ini <b>menulis ulang field-nya sendiri</b> dengan bentuk
	 * ternormalisasi itu, sehingga membaca saja sudah mengubah isi kolom di basis data pada
	 * flush berikutnya. Nilai {@code null} berubah menjadi string kosong; nilai yang hanya
	 * berisi koma juga menjadi string kosong.</p>
	 *
	 * <p><b>Cara kerja.</b> Nilai dibungkus koma di kedua ujungnya, lalu koma ganda dirapatkan
	 * dengan {@code replaceAll(",,", ",")} yang dijalankan <b>tiga kali berturut-turut</b> —
	 * pengulangan manual ini perlu karena {@code replaceAll} tidak menangani pencocokan yang
	 * tumpang tindih. Tiga lintasan cukup untuk maksimal delapan koma beruntun; masukan yang
	 * lebih ekstrem masih bisa menyisakan koma ganda. Sesudahnya, tiga pemeriksaan literal
	 * ({@code ","}, {@code ",,"}, {@code ",,,"}) mengosongkan hasil yang hanya berisi
	 * pemisah.</p>
	 *
	 * <p><b>Idempoten:</b> memanggilnya berulang kali menghasilkan nilai yang sama, karena
	 * bentuk {@code ",a,b,"} dibungkus lagi menjadi {@code ",,a,b,,"} lalu dirapatkan kembali
	 * ke {@code ",a,b,"}.</p>
	 *
	 * <p><b>Pemakaian.</b> Tombol Simpan pada {@code KknUntukMahasiswaAction} mem-{@code split}
	 * nilai ini dengan koma lalu membandingkan tiap potongan (setelah {@code trim}) dengan NIM
	 * pendaftar. Bila cocok, <b>kedua</b> pemeriksaan biaya — {@link #getKodeItemBiaya()} dan
	 * {@link #getHarusBayar()} — dilewati seluruhnya. Karena bentuk ternormalisasi diawali
	 * koma, potongan pertama hasil {@code split} selalu string kosong; itu tidak berbahaya
	 * selama NIM mahasiswa tidak pernah kosong.</p>
	 *
	 * @return daftar NIM bebas biaya berbentuk {@code ",nim1,nim2,"}, atau string kosong;
	 *         tidak pernah {@code null}
	 */
	public String getNimMhsTanpaBiaya() {
		nimMhsTanpaBiaya = (nimMhsTanpaBiaya == null || nimMhsTanpaBiaya.trim().equalsIgnoreCase(",") ? ""
				: "," + nimMhsTanpaBiaya.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (nimMhsTanpaBiaya.equals(",")) {
			nimMhsTanpaBiaya = "";
		} else if (nimMhsTanpaBiaya.equals(",,")) {
			nimMhsTanpaBiaya = "";
		} else if (nimMhsTanpaBiaya.equals(",,,")) {
			nimMhsTanpaBiaya = "";
		}
		return nimMhsTanpaBiaya == null ? "" : nimMhsTanpaBiaya.trim();
	}

	/**
	 * Menetapkan daftar NIM yang dibebaskan dari pemeriksaan biaya.
	 *
	 * <p>Isian bebas: koma pemisah di ujung maupun ganda akan dirapikan sendiri oleh
	 * {@link #getNimMhsTanpaBiaya()} pada pembacaan berikutnya.</p>
	 *
	 * @param nimMhsTanpaBiaya daftar NIM dipisah koma; boleh {@code null}
	 */
	public void setNimMhsTanpaBiaya(String nimMhsTanpaBiaya) {
		this.nimMhsTanpaBiaya = nimMhsTanpaBiaya;
	}

	/**
	 * Mengembalikan kategori aktivitas mahasiswa untuk pelaporan Feeder/PDDikti.
	 *
	 * <p><b>Efek samping:</b> hasil {@link GeneralValueObject#check(Object)} ditugaskan kembali
	 * ke field (penulisan "netral", hanya menyeragamkan instance).</p>
	 *
	 * <p><b>Fallback yang tidak ditulis balik:</b> bila field tetap {@code null}, yang
	 * dikembalikan adalah {@code ConstantValues.KKN} — <b>tanpa</b> menugaskannya ke field.
	 * Karena pemetaan berbasis properti, nilai fallback ini tetap ikut tersimpan sebagai kunci
	 * asing pada flush berikutnya, sehingga baris yang semula kosong akan "mengisi dirinya
	 * sendiri" dengan kategori KKN begitu getter ini dipanggil.</p>
	 *
	 * <p><b>Hati-hati dengan {@code ConstantValues.KKN}.</b> Itu bukan konstanta melainkan
	 * field statis non-final yang baru diisi {@code ais.common.InitDataHelper} saat penyemaian
	 * awal tabel {@code jenis_aktivitas_mahasiswa}. Baris masternya dicari lewat
	 * {@code feeder = 5}, tetapi penugasannya ke {@code ConstantValues.KKN} baru terjadi bila
	 * {@code nama}-nya persis {@code "Kuliah kerja nyata"} — nama yang pernah diubah operator
	 * membuat konstanta itu tetap {@code null}. Sebelum {@code InitData} selesai (atau pada
	 * kasus nama berubah tadi), getter ini mengembalikan {@code null}, dan pemanggil seperti
	 * {@code KknAction} yang merangkai
	 * {@code kkn.getJenisAktfitasMahasiswa().getKampusMerderka()} akan melempar
	 * {@code NullPointerException}. Instance-nya dibagi ke seluruh aplikasi dan berasal dari
	 * sesi Hibernate lain; digabung dengan {@code CascadeType.MERGE} pada relasi ini, itu
	 * berarti objek yang sama bisa ikut terbawa ke banyak sesi sekaligus.</p>
	 *
	 * <p>Cast {@code (JenisAktfitasMahasiswa)} pada pernyataan {@code return} sebenarnya
	 * berlebihan — kedua cabang ternary sudah bertipe {@code JenisAktfitasMahasiswa}.</p>
	 *
	 * @return kategori aktivitas; {@code null} bila {@code InitData} belum selesai berjalan
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_aktfitas_mahasiswa", nullable = true)
	public JenisAktfitasMahasiswa getJenisAktfitasMahasiswa() {
		jenisAktfitasMahasiswa = check(jenisAktfitasMahasiswa);
		return (JenisAktfitasMahasiswa) (jenisAktfitasMahasiswa == null ? ConstantValues.KKN : jenisAktfitasMahasiswa);
	}

	/**
	 * Menetapkan kategori aktivitas mahasiswa untuk pelaporan Feeder/PDDikti.
	 *
	 * @param jenisAktfitasMahasiswa kategori aktivitas; {@code null} berarti pakai fallback
	 *                               {@code ConstantValues.KKN}
	 */
	public void setJenisAktfitasMahasiswa(JenisAktfitasMahasiswa jenisAktfitasMahasiswa) {
		this.jenisAktfitasMahasiswa = jenisAktfitasMahasiswa;
	}

	/**
	 * Mengembalikan izin bagi mahasiswa untuk mengubah agenda/aktivitas KKN kelompoknya.
	 *
	 * <p><b>Bawaan {@code true}:</b> {@code null} dilaporkan sebagai {@code true}, yaitu
	 * <i>diizinkan</i>. Berbeda dari kebanyakan getter lain di kelas ini, nilai pengganti
	 * <b>tidak</b> ditulis balik ke field — namun karena pemetaan berbasis properti, nilai
	 * {@code true} itulah yang tetap ikut tersimpan pada flush berikutnya.</p>
	 *
	 * <p>Dibaca {@code ais.action.master.helper.AktifitasKknHelper} lewat rantai
	 * {@code kelompokKkn.getKkn().getMahasiswaBolehMerubahAgenda()} untuk menentukan apakah
	 * tombol ubah agenda ditampilkan bagi pengguna bertipe mahasiswa.</p>
	 *
	 * @return {@code true} bila mahasiswa boleh mengubah agenda
	 */
	public Boolean getMahasiswaBolehMerubahAgenda() {
		return mahasiswaBolehMerubahAgenda == null ? true : mahasiswaBolehMerubahAgenda;
	}

	/**
	 * Menetapkan izin bagi mahasiswa untuk mengubah agenda/aktivitas KKN.
	 *
	 * @param mahasiswaBolehMerubahAgenda {@code true} untuk mengizinkan; {@code null} dibaca
	 *                                    sebagai {@code true}
	 */
	public void setMahasiswaBolehMerubahAgenda(Boolean mahasiswaBolehMerubahAgenda) {
		this.mahasiswaBolehMerubahAgenda = mahasiswaBolehMerubahAgenda;
	}

	/**
	 * Mengembalikan izin bagi dosen pembimbing untuk mengubah agenda/aktivitas KKN kelompok
	 * bimbingannya.
	 *
	 * <p>Perilakunya identik dengan {@link #getMahasiswaBolehMerubahAgenda()}: {@code null}
	 * berarti <i>diizinkan</i>, tanpa penulisan balik ke field. Dibaca dari titik yang sama di
	 * {@code AktifitasKknHelper}, pada cabang untuk pengguna bertipe dosen.</p>
	 *
	 * @return {@code true} bila dosen boleh mengubah agenda
	 */
	public Boolean getDosenBolehMerubahAgenda() {
		return dosenBolehMerubahAgenda == null ? true : dosenBolehMerubahAgenda;
	}

	/**
	 * Menetapkan izin bagi dosen pembimbing untuk mengubah agenda/aktivitas KKN.
	 *
	 * @param dosenBolehMerubahAgenda {@code true} untuk mengizinkan; {@code null} dibaca
	 *                                sebagai {@code true}
	 */
	public void setDosenBolehMerubahAgenda(Boolean dosenBolehMerubahAgenda) {
		this.dosenBolehMerubahAgenda = dosenBolehMerubahAgenda;
	}
}
