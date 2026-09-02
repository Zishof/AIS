package ais.database.model;

// Generated Dec 29, 2009 12:53:03 AM by Hibernate Tools 3.2.4.CR1

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

import ais.common.ConstantValues;

/**
 * Master <b>format penilaian sidang tugas akhir/skripsi</b> — satu baris tabel
 * {@code format_nilai_skripsi} adalah satu "jenis pengajuan sidang" yang dapat dipilih (atau
 * ditetapkan) untuk seorang mahasiswa, lengkap dengan susunan dosen penilainya, bobot nilai
 * masing-masing, syarat administratif untuk boleh mendaftar, daftar lampiran yang harus diunggah,
 * biaya yang harus lunas, serta cara nilai akhirnya dikonversi menjadi nilai huruf. Class ini
 * awalnya hasil {@code hbm2java} (lihat komentar generator di atas paket) lalu tumbuh manual
 * menjadi tabel konfigurasi yang sangat lebar: ±244 method, hampir seluruhnya getter/setter.
 *
 * <p>Entity ini <b>tidak</b> menyimpan nilai satu mahasiswa pun. Ia hanya "cetakan"; nilainya
 * ada di {@link Skripsi} (satu baris per mahasiswa per tugas akhir) yang menunjuk balik lewat
 * {@code Skripsi.formatNilaiSkripsi}. Perbandingannya seperti {@code Matakuliah} (master) versus
 * {@code Detailperkuliahan} (data per mahasiswa).</p>
 *
 * <h3>Posisi dalam alur sidang tugas akhir</h3>
 * <ol>
 * <li><b>Pemilihan format</b> — saat mahasiswa/operator membuka layar pengajuan sidang,
 * {@code SkripsiAction} mengambil semua format yang {@link #getAktif() aktif} dan cocok dengan
 * fakultas/jurusan/program/status awal mahasiswa, lalu <i>mengurutkannya berdasarkan skor
 * kecocokan</i> (jurusan 16 poin, fakultas 8, program 4, status awal 2, tahun angkatan 1) sehingga
 * format paling spesifik muncul paling atas. Format ber-{@link #getTidakBolehDipilihMahasiswa()}
 * {@code true} hanya boleh dipilihkan oleh petugas.</li>
 * <li><b>Gerbang syarat pendaftaran</b> — sebelum pengajuan disimpan, {@code SkripsiAction}
 * memeriksa berturut-turut {@link #getProsentaseLunas()}/{@link #getHarusLunas()},
 * {@link #getHarusMengembalikanBukuPerpustakaan()}, {@link #getMinimalSks()},
 * {@link #getMinimalAngkaKredit()}, {@link #getMinimalIpk()},
 * {@link #getMatkulPrasyaratLulus()}, serta keberadaan mata kuliah tugas akhir di KRS
 * ({@link #getKodeMatakuliah()} / {@link #getKodeMatakuliahDan()}). Gagal satu saja → pengajuan
 * ditolak dengan pesan yang menyebut {@link #getNama()}.</li>
 * <li><b>Kelengkapan berkas</b> — {@link #getUploadLampiran1()} .. {@link #getUploadLampiran20()}
 * memberi <i>judul</i> 20 slot lampiran; pasangannya {@code ...Wajib} menentukan apakah slot itu
 * memblokir pengajuan bila kosong, dan {@link #getTipeItem1()} .. {@link #getTipeItem20()}
 * menautkan slot ke jenis koleksi perpustakaan (dipakai {@code LibraryUtil} untuk otomatis
 * mendaftarkan naskah tugas akhir sebagai item pustaka).</li>
 * <li><b>Susunan dosen &amp; bobot</b> — delapan slot dosen (lihat tabel di bawah) menentukan siapa
 * saja yang menilai dan berapa persen kontribusi nilainya.</li>
 * <li><b>Komponen penilaian</b> — rincian butir yang dinilai tiap dosen tidak ada di sini,
 * melainkan di {@link SkripsiPunyaKomponenPenilaianSkripsi} (tabel penghubung
 * format &rarr; {@code KomponenPenilaianSkripsi}) yang di-query oleh
 * {@code PenilaianSkripsiHelper.populateKomponen(String)}.</li>
 * <li><b>Nilai akhir &amp; KHS</b> — {@link #getJenisNilaiHuruf()} (bila diisi) menggantikan skala
 * huruf milik mata kuliah saat {@code Skripsi.getNilaiHuruf()} menerjemahkan total nilai, dan
 * {@link #getBobot()} dipakai {@code GradingHelper} untuk menimbang total nilai format ini
 * terhadap format lain (mis. nilai seminar proposal + nilai sidang) ketika keduanya digabung
 * menjadi satu nilai mata kuliah tugas akhir di kartu hasil studi.</li>
 * <li><b>Pelaporan</b> — {@link #getJenisKegiatanMahasiswa()} menyediakan kode aktivitas untuk
 * ekspor PDDikti ({@code EksporAktifitasSkripsiFeeder} dan saudaranya).</li>
 * </ol>
 *
 * <h3>Delapan slot dosen — dan penamaan bobot yang tertukar</h3>
 * <p>Setiap slot punya empat atribut sejajar: <i>label</i> ({@code dosenN}), <i>kode</i>
 * ({@code kodeN}, dipakai sebagai kode peran pada berita acara/laporan), <i>bendera aktif</i>
 * ({@code dosenNAktif}), dan <i>bobot persentase nilai</i> — yang justru <b>tidak</b> mengikuti
 * penomoran slot:</p>
 * <table border="1" summary="Pemetaan slot dosen ke atribut dan ke kolom tabel skripsi">
 * <tr><th>Slot</th><th>Label default</th><th>Bobot di class ini</th><th>Kolom orang di
 * {@code skripsi}</th><th>Kolom nilai di {@code skripsi}</th></tr>
 * <tr><td>{@code dosen1}</td><td>Pembimbing I</td><td><b>{@link #getProsentasiNilaiKetuaSidang()}</b></td><td>{@code pembimbing}</td><td><b>{@code nilai_ketua_sidang}</b></td></tr>
 * <tr><td>{@code dosen2}</td><td>Pembimbing II</td><td><b>{@link #getProsentasiNilaiPembimbing()}</b></td><td>{@code ketua_sidang}</td><td><b>{@code nilai_pembimbing}</b></td></tr>
 * <tr><td>{@code dosen21}</td><td>Pembimbing III</td><td>{@link #getProsentasiNilaiPembimbing3()}</td><td>{@code pembimbing3}</td><td>{@code nilai_pembimbing3}</td></tr>
 * <tr><td>{@code dosen3}</td><td>Penguji I</td><td>{@link #getProsentasiNilaiPenguji1()}</td><td>{@code penguji1}</td><td>{@code nilai_penguji1}</td></tr>
 * <tr><td>{@code dosen4}</td><td>Penguji II</td><td>{@link #getProsentasiNilaiPenguji2()}</td><td>{@code penguji2}</td><td>{@code nilai_penguji2}</td></tr>
 * <tr><td>{@code dosen5}</td><td>Penguji III</td><td>{@link #getProsentasiNilaiPenguji3()}</td><td>{@code penguji3}</td><td>{@code nilai_penguji_3}</td></tr>
 * <tr><td>{@code dosen6}</td><td>Penguji IV</td><td>{@link #getProsentasiNilaiPenguji4()}</td><td>{@code penguji4}</td><td>{@code nilai_penguji_4}</td></tr>
 * <tr><td>{@code dosen7}</td><td>Penguji V</td><td>{@link #getProsentasiNilaiPenguji5()}</td><td>{@code penguji5}</td><td>{@code nilai_penguji_5}</td></tr>
 * </table>
 *
 * <p><b>KETERKAITAN DENGAN "BUG SLOT DOSEN 1/2" DI {@link Skripsi}:</b> class inilah <i>sisi
 * master</i> dari penamaan yang tertukar itu, dan bug tersebut <b>ada juga di sini</b>. Slot 1
 * berlabel default "Pembimbing I" namun bobotnya disimpan pada kolom
 * {@code prosentasi_nilai_ketua_sidang}, sedangkan slot 2 berlabel "Pembimbing II" dengan bobot di
 * {@code prosentasi_nilai_pembimbing}. Pergeseran nama ini <b>sejalan sempurna</b> dengan kolom
 * nilai di tabel {@code skripsi} ({@code nilai_ketua_sidang} milik slot 1, {@code nilai_pembimbing}
 * milik slot 2) — jadi keduanya "salah dengan cara yang sama", bukan dua kesalahan yang saling
 * meniadakan. Buktinya paling telanjang ada di {@link #getDosen1Aktif()} yang, saat bendera belum
 * pernah diisi, menyimpulkan keaktifan slot 1 dari {@link #getProsentasiNilaiKetuaSidang()}, dan
 * {@link #getDosen2Aktif()} dari {@link #getProsentasiNilaiPembimbing()}. Rantai
 * label &rarr; bobot &rarr; nilai dipakai konsisten oleh {@code PenilaianSkripsiHelper},
 * {@code Skripsi.dataDosen(boolean)} dan {@code FormatNilaiSkripsiAction}, sehingga <b>angka yang
 * tampil di layar dan laporan tetap benar</b>; yang salah semata-mata <i>namanya</i>. Risikonya
 * jatuh pada query SQL langsung, laporan ad-hoc, dan integrasi eksternal yang membaca
 * {@code format_nilai_skripsi}/{@code skripsi} apa adanya: kolom
 * {@code prosentasi_nilai_ketua_sidang} sama sekali bukan bobot "ketua sidang", melainkan bobot
 * dosen slot 1 yang secara default disebut "Pembimbing I". Jangan "dirapikan" tanpa migrasi data
 * serentak di kedua tabel. Lihat Javadoc class {@link Skripsi} untuk tabel pemetaan yang sama dari
 * sisi data per mahasiswa.</p>
 *
 * <p>Untuk perbandingan: entity sejenis {@link FormatNilaiProposalSkripsi} (seminar proposal, bukan
 * sidang akhir) memakai penamaan yang <b>bersih</b> — {@code prosentasiNilaiPembimbing1/2/3} —
 * dan tidak punya slot {@code dosen21}/{@code dosen7}. Jadi jangan menyalin asumsi penamaan dari
 * satu entity ke entity lain.</p>
 *
 * <h3>Jebakan lain seputar slot dosen</h3>
 * <ul>
 * <li><b>Pencocokan berbasis teks label.</b> Sepanjang aplikasi, "peran" seorang dosen diwakili
 * oleh <i>string</i> hasil {@code getDosenN()} (lihat {@code Skripsi.dataDosen(boolean)} yang
 * mengemasnya ke {@code CommonVO}, lalu {@code PenilaianSkripsiHelper.nilaiDosen},
 * {@code persenDosen}, {@code populateKomponen}, dan {@code Skripsi.simpanDosen}). Bila dua slot
 * pada satu format diberi label yang persis sama, cabang {@code if/else} pertama yang menang dan
 * slot kedua menjadi tidak terjangkau. Label karena itu praktis wajib unik per format.</li>
 * <li><b>Penomoran {@code dosen21} bukan "slot ke-21"</b> melainkan slot sisipan antara
 * {@code dosen2} dan {@code dosen3} (Pembimbing III), ditambahkan belakangan agar kolom lama tidak
 * perlu digeser.</li>
 * <li><b>{@code populateKomponen} tidak mengenali {@code dosen21}.</b> Pemetaan label &rarr; kolom
 * kebolehan komponen di {@code PenilaianSkripsiHelper.populateKomponen(String)} hanya punya cabang
 * untuk {@code dosen1}..{@code dosen7} tanpa {@code dosen21}, sehingga Pembimbing III jatuh ke
 * nilai awal {@code "dosen1"} dan melihat daftar komponen milik slot 1. Dicatat apa adanya, tidak
 * diperbaiki di sini.</li>
 * <li><b>{@code VOPembelajaran} memetakan slot 1/2 secara terbalik.</b> Pada cabang
 * {@code this instanceof Skripsi} untuk penentuan sebutan peran seorang dosen, kode di
 * {@link VOPembelajaran} mengembalikan {@code getDosen1()} ketika {@code skripsi.getKetuaSidang()}
 * tidak null dan {@code getDosen2()} ketika {@code getPembimbing()} tidak null — kebalikan dari
 * pemetaan kanonik pada {@code Skripsi.dataDosen(boolean)} maupun dari method lain di file yang
 * sama. Sekali lagi: dicatat, tidak diperbaiki.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 * <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #toString()}, {@link #getNama()},
 * {@link #getAktif()}, plus field audit yang di-<i>shadow</i> dari induk (lihat di bawah):
 * {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 * <li><b>Cakupan/penargetan format</b> — {@link #getFakultas()}, {@link #getJurusan()},
 * {@link #getProgram()}, {@link #getStatusAwalMahasiswa()}, {@link #getTahunAngkatan()},
 * {@link #getTidakBolehDipilihMahasiswa()}.</li>
 * <li><b>Slot dosen</b> — label {@code getDosen1()}..{@code getDosen7()} + {@code getDosen21()},
 * kode {@code getKode1()}..{@code getKode7()} + {@code getKode21()}, bendera
 * {@code getDosen1Aktif()}.., dan bobot {@code getProsentasiNilai*()}.</li>
 * <li><b>Syarat pendaftaran akademik</b> — {@link #getMinimalSks()}, {@link #getMinimalIpk()},
 * {@link #getMinimalAngkaKredit()}, {@link #getMatkulPrasyaratLulus()},
 * {@link #getKodeMatakuliah()}, {@link #getKodeMatakuliahDan()},
 * {@link #getTidakWajibMengambilMkTertentu()}.</li>
 * <li><b>Syarat keuangan &amp; perpustakaan</b> — {@link #getHarusLunas()},
 * {@link #getProsentaseLunas()}, {@link #getKodeItemBiaya()}, {@link #getSekaliBayar()},
 * {@link #getHarusMengembalikanBukuPerpustakaan()}.</li>
 * <li><b>Lampiran</b> — 20 × judul {@code getUploadLampiranN()}, 20 × bendera wajib
 * {@code getUploadLampiranNWajib()}, 20 × penautan pustaka {@code getTipeItemN()}.</li>
 * <li><b>Penilaian &amp; pelaporan</b> — {@link #getBobot()}, {@link #getJenisNilaiHuruf()},
 * {@link #getJenis()}, {@link #getJenisKegiatanMahasiswa()},
 * {@link #getMahasiswaBolehMengubahAgendaAtauJadwalBimbingan()}.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh file ini</h3>
 * <ul>
 * <li><b>Field audit di-shadow.</b> {@code id}, {@code oleh}, {@code olehId} dan
 * {@code tanggal_dirubah} dideklarasikan ulang di class ini padahal
 * {@link GeneralValueObject} sudah punya field bernama sama. Yang dibaca/ditulis Hibernate adalah
 * milik class ini (getter-nyalah yang dianotasi), sementara field induk tetap ada dan tidak pernah
 * terisi. Pola yang sama ditemukan pada semua entity {@code ais.database.model} yang sudah
 * didokumentasikan sejauh ini — bukan kekhasan file ini. Konsekuensinya: memanggil
 * {@code super.getOleh()} atau membaca field induk lewat refleksi akan mengembalikan
 * {@code null}.</li>
 * <li><b>Ada getter yang menulis balik ke field</b> sehingga tidak murni:
 * {@link #getKodeMatakuliah()} dan {@link #getKodeMatakuliahDan()} menormalkan koma lalu
 * <i>menyimpan</i> hasil normalisasinya ke field, dan bahkan mengosongkannya bila
 * {@link #getTidakWajibMengambilMkTertentu()} {@code true}; {@link #getJenisKegiatanMahasiswa()}
 * mengisi field relasi dari kode teks {@link #getJenis()}. Karena Hibernate membandingkan snapshot
 * field, sekadar menampilkan entity ini di grid dapat membuatnya dianggap kotor dan memicu
 * {@code UPDATE} saat flush. Ini varian lokal dari pola "getter berefek samping" yang universal di
 * codebase AIS.</li>
 * <li><b>Getter relasi memanggil {@code check(...)}</b> ({@link #getJurusan()},
 * {@link #getFakultas()}, {@link #getStatusAwalMahasiswa()}, {@link #getJenisNilaiHuruf()},
 * {@link #getJenisKegiatanMahasiswa()}), yang bila entity sudah <i>detached</i> akan
 * <b>membuka session Hibernate sendiri</b> untuk memuat ulang proxy — lihat
 * {@link GeneralValueObject#check(Object)}.</li>
 * <li><b>Hampir semua getter null-safe dengan nilai default</b> ({@code 0.0}, {@code false},
 * {@code ""}). Karena itu "belum pernah diisi" dan "sengaja diisi nol/false" tidak dapat dibedakan
 * dari luar; hanya {@code getDosenNAktif()} yang memanfaatkan {@code null} sebagai keadaan ketiga
 * ("belum pernah dikonfigurasi, simpulkan dari bobot"). Setter-nya <b>tidak</b> null-safe dan
 * menerima {@code null} apa adanya, kecuali {@link #setOleh(String)}/{@link #setOlehId(String)}
 * yang mengabaikan masukan kosong.</li>
 * <li><b>Sebagian besar property tanpa anotasi {@code @Column}</b> sehingga nama kolomnya
 * diturunkan Hibernate dari nama property (mis. {@code uploadLampiran12Wajib} &rarr;
 * {@code uploadlampiran12wajib} sesuai strategi penamaan yang berlaku). Hanya sejumlah kecil
 * property yang menyebut nama kolom eksplisit; justru property itulah yang namanya tertukar
 * (lihat di atas).</li>
 * <li><b>{@link #toString()} tidak mencerminkan seluruh bobot</b> — ia menyebut
 * {@code prosentasiNilaiPenguji1} dua kali dan tidak pernah menyebut
 * {@code prosentasiNilaiPembimbing3}, {@code prosentasiNilaiPenguji4} maupun
 * {@code prosentasiNilaiPenguji5}. Jangan dipakai sebagai sumber kebenaran.</li>
 * <li><b>{@code @Audited}</b> (Hibernate Envers): setiap perubahan format menghasilkan baris
 * revisi. Mengubah bobot pada format yang sudah dipakai <i>tidak</i> menghitung ulang nilai skripsi
 * yang sudah tersimpan; perhitungan ulang dipicu terpisah lewat
 * {@code PenilaianSkripsiHelper}.</li>
 * </ul>
 *
 * <p>Class ini semula dibangkitkan {@code hbm2java} (Hibernate Tools 3.2.4.CR1, 29 Des 2009).</p>
 *
 * @see Skripsi
 * @see FormatNilaiProposalSkripsi
 * @see SkripsiPunyaKomponenPenilaianSkripsi
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "format_nilai_skripsi")
public class FormatNilaiSkripsi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya dibangkitkan sekali dan sengaja tidak pernah diubah
	 * agar sesi ZK/cache yang sudah ter-serialize dari versi aplikasi sebelumnya tetap dapat
	 * dibaca meski daftar field entity bertambah.
	 */
	private static final long serialVersionUID = -8713937809725960837L;
	/**
	 * Kunci utama. Sengaja dideklarasikan ulang di sini walau {@link GeneralValueObject} sudah
	 * punya field {@code id} — inilah field yang benar-benar dipetakan Hibernate (lihat
	 * {@link #getId()}); field milik induk tidak pernah terisi.
	 */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan format ini; field audit yang di-shadow dari induk. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan format ini; field audit yang di-shadow dari induk. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini apa adanya (boleh {@code null}
	 * bila baris dibuat oleh proses batch atau berasal dari data lama).
	 *
	 * <p>Perhatikan bahwa nilai ini berasal dari field {@code olehId} milik <b>class ini</b>, yang
	 * menutupi ({@code shadow}) field bernama sama di {@link GeneralValueObject}. Memanggil
	 * {@code super.getOlehId()} akan selalu mengembalikan {@code null}.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 * @see GeneralValueObject
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Masukan {@code null} atau yang hanya berisi spasi
	 * <b>diabaikan diam-diam</b> sehingga nilai lama dipertahankan — perilaku sengaja, agar jalur
	 * simpan yang tidak membawa identitas pengguna (proses batch, impor, penjadwal) tidak
	 * menghapus jejak audit yang sudah ada.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, masukan
	 * kosong/{@code null} diabaikan agar nilai audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 * @see #setOlehId(String)
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini apa adanya, tanpa
	 * normalisasi.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dipanggil tepat sebelum setiap {@code UPDATE} baris ini, meneruskan
	 * ke {@code AuditTimestampInterceptor.ubah(this)} untuk memperbarui
	 * {@link #getTanggal_dirubah()} dan identitas pengubah secara terpusat.
	 *
	 * <p>Implementasi wajib dari satu-satunya method {@code abstract} pada
	 * {@link GeneralValueObject}. Jangan dipanggil manual dari kode aplikasi — Hibernate yang
	 * memanggilnya.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Field audit yang di-shadow dari induk; nilai awalnya diisi
	 * <b>saat object dibuat di JVM</b> (bukan saat baris disimpan) lewat
	 * {@code WaktuUtil.getDate()}, sehingga format yang dibaca dari database pun sempat memegang
	 * waktu "sekarang" sebelum Hibernate menimpanya dengan nilai kolom.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Berbeda dengan {@link #setOleh(String)}, method ini
	 * <b>tidak</b> menolak {@code null} — memanggilnya dengan {@code null} benar-benar mengosongkan
	 * kolom stempel waktu.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}. Diisi
	 * otomatis lewat {@link #onUpdate()} pada setiap {@code UPDATE}.
	 *
	 * @return waktu perubahan terakhir; praktis tidak pernah {@code null} karena field punya nilai
	 *         awal saat object dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas untuk keperluan log dan debug, berbentuk
	 * {@code id-nama-bobot1-bobot2bobot3-bobot3-bobot4-bobot5}.
	 *
	 * <p><b>Perhatian, isinya cacat dan tidak lengkap</b> (dicatat, tidak diperbaiki):</p>
	 * <ul>
	 * <li>{@code prosentasiNilaiPenguji1} disebut <b>dua kali</b> berturut-turut;</li>
	 * <li>pemisah {@code "-"} antara bobot pembimbing dan bobot penguji I <b>tertinggal</b>,
	 * sehingga kedua angka menempel tanpa pemisah;</li>
	 * <li>{@code prosentasiNilaiPembimbing3}, {@code prosentasiNilaiPenguji4} dan
	 * {@code prosentasiNilaiPenguji5} tidak pernah muncul;</li>
	 * <li>field dibaca <b>langsung</b>, bukan lewat getter null-safe, sehingga bobot yang belum
	 * pernah diisi tercetak sebagai {@code null} dan {@code nama} tercetak apa adanya (bukan hasil
	 * penggantian pada {@link #getNama()}).</li>
	 * </ul>
	 *
	 * <p>Untuk menampilkan format ini ke pengguna pakailah {@link #getNama()}; komponen ZK di
	 * {@code SkripsiAction}/{@code FormatNilaiSkripsiAction} memang memakai itu, bukan
	 * {@code toString()}.</p>
	 *
	 * @return string gabungan id, nama, dan sebagian bobot; tidak pernah {@code null}
	 */
	public String toString() {
		return id + "-" + nama + "-" + prosentasiNilaiKetuaSidang + "-" + prosentasiNilaiPembimbing
				+ prosentasiNilaiPenguji1 + "-" + prosentasiNilaiPenguji1 + "-" + prosentasiNilaiPenguji2 + "-"
				+ prosentasiNilaiPenguji3;
	}

	/** Nama format seperti dilihat pengguna; lihat {@link #getNama()} untuk penggantian namanya. */
	private String nama;

	// ---------------------------------------------------------------------------------------
	// BOBOT NILAI PER SLOT DOSEN (satuan persen). PENOMORANNYA TERGESER SATU LANGKAH terhadap
	// label slot: prosentasiNilaiKetuaSidang milik slot dosen1 ("Pembimbing I"),
	// prosentasiNilaiPembimbing milik slot dosen2 ("Pembimbing II"), dan seterusnya. Lihat tabel
	// pemetaan pada Javadoc class sebelum menyentuh salah satu dari field ini.
	// ---------------------------------------------------------------------------------------
	/** Bobot nilai slot {@code dosen1} — meski namanya "ketua sidang". */
	private Double prosentasiNilaiKetuaSidang = 0.0;
	/** Bobot nilai slot {@code dosen2} — meski namanya "pembimbing". */
	private Double prosentasiNilaiPembimbing = 0.0;
	/** Bobot nilai slot {@code dosen21} (Pembimbing III); penamaannya sudah lurus. */
	private Double prosentasiNilaiPembimbing3 = 0.0;
	/** Bobot nilai slot {@code dosen3} (Penguji I); penamaan slot penguji sudah lurus. */
	private Double prosentasiNilaiPenguji1 = 0.0;
	/** Bobot nilai slot {@code dosen4} (Penguji II). */
	private Double prosentasiNilaiPenguji2 = 0.0;
	/** Bobot nilai slot {@code dosen5} (Penguji III). */
	private Double prosentasiNilaiPenguji3 = 0.0;
	/** Bobot nilai slot {@code dosen6} (Penguji IV). */
	private Double prosentasiNilaiPenguji4 = 0.0;
	/** Bobot nilai slot {@code dosen7} (Penguji V). */
	private Double prosentasiNilaiPenguji5 = 0.0;

	// Label peran tiap slot dosen. Dipakai sebagai KUNCI PENCOCOKAN berbasis teks di seluruh
	// aplikasi, jadi praktis wajib unik dalam satu format.
	private String dosen1;
	private String dosen2;
	private String dosen21;
	private String dosen3;
	private String dosen4;
	private String dosen5;
	private String dosen6;
	private String dosen7;

	// Kode peran tiap slot dosen (mis. kode jabatan pada berita acara/SK penguji). Ikut dikemas ke
	// CommonVO oleh Skripsi.dataDosen(boolean) bersama labelnya.
	private String kode1;
	private String kode2;
	private String kode21;
	private String kode3;
	private String kode4;
	private String kode5;
	private String kode6;
	private String kode7;

	// Bendera "slot ini dipakai". Bertipe Boolean (bukan boolean) karena null punya arti khusus:
	// "belum pernah dikonfigurasi" -> getter menyimpulkannya dari bobot nilai slot bersangkutan.
	private Boolean dosen1Aktif;
	private Boolean dosen2Aktif;
	private Boolean dosen21Aktif;
	private Boolean dosen3Aktif;
	private Boolean dosen4Aktif;
	private Boolean dosen5Aktif;
	private Boolean dosen6Aktif;
	private Boolean dosen7Aktif;

	// Penargetan format: keempat field berikut (plus tahunAngkatan di bawah) menentukan format mana
	// yang muncul dan seberapa "cocok" ia untuk seorang mahasiswa. null = berlaku untuk semua.
	private Fakultas fakultas;
	private Jurusan jurusan;
	private String program;
	private StatusAwalMahasiswa statusAwalMahasiswa;
	private Boolean aktif;

	// Syarat administratif non-akademik untuk boleh mendaftar sidang.
	private Boolean tidakWajibMengambilMkTertentu;
	private Boolean harusLunas;
	private Double prosentaseLunas;
	private Boolean harusMengembalikanBukuPerpustakaan;
	private String kodeItemBiaya;
	private Boolean sekaliBayar;

	// Syarat akademik + bobot format terhadap nilai mata kuliah tugas akhir di KHS.
	private String kodeMatakuliah;
	private Double bobot;
	private Integer minimalSks;
	private Double minimalIpk;
	private Double minimalAngkaKredit;
	// Matakuliah PRASYARAT LULUS untuk boleh DAFTAR sidang (kode/nama dipisah koma). Bila mahasiswa
	// BELUM LULUS salah satu matkul ini -> tidak boleh mendaftar. Kosong = tanpa prasyarat matkul.
	private String matkulPrasyaratLulus;

	// 20 slot lampiran. Isi field = JUDUL slot yang ditampilkan ke mahasiswa; slot berjudul kosong
	// dianggap tidak dipakai dan tidak dirender sama sekali.
	private String uploadLampiran1;
	private String uploadLampiran2;
	private String uploadLampiran3;
	private String uploadLampiran4;
	private String uploadLampiran5;
	private String uploadLampiran6;
	private String uploadLampiran7;
	private String uploadLampiran8;
	private String uploadLampiran9;
	private String uploadLampiran10;
	private String uploadLampiran11;
	private String uploadLampiran12;
	private String uploadLampiran13;
	private String uploadLampiran14;
	private String uploadLampiran15;
	private String uploadLampiran16;
	private String uploadLampiran17;
	private String uploadLampiran18;
	private String uploadLampiran19;
	private String uploadLampiran20;

	// Bendera "lampiran ke-N wajib": bila true dan berkasnya belum diunggah, pengajuan sidang
	// diblokir dengan pesan "<judul lampiran> wajib diupload !".
	private Boolean uploadLampiran1Wajib;
	private Boolean uploadLampiran2Wajib;
	private Boolean uploadLampiran3Wajib;
	private Boolean uploadLampiran4Wajib;
	private Boolean uploadLampiran5Wajib;
	private Boolean uploadLampiran6Wajib;
	private Boolean uploadLampiran7Wajib;
	private Boolean uploadLampiran8Wajib;
	private Boolean uploadLampiran9Wajib;
	private Boolean uploadLampiran10Wajib;
	private Boolean uploadLampiran11Wajib;
	private Boolean uploadLampiran12Wajib;
	private Boolean uploadLampiran13Wajib;
	private Boolean uploadLampiran14Wajib;
	private Boolean uploadLampiran15Wajib;
	private Boolean uploadLampiran16Wajib;
	private Boolean uploadLampiran17Wajib;
	private Boolean uploadLampiran18Wajib;
	private Boolean uploadLampiran19Wajib;
	private Boolean uploadLampiran20Wajib;

	// Penautan tiap slot lampiran ke jenis koleksi perpustakaan: menyimpan ID TipeItem sebagai Long
	// mentah (bukan relasi @ManyToOne), jadi tidak ada foreign key maupun cascade. Dipakai
	// LibraryUtil untuk mendaftarkan berkas tugas akhir sebagai item pustaka.
	private Long tipeItem1;
	private Long tipeItem2;
	private Long tipeItem3;
	private Long tipeItem4;
	private Long tipeItem5;
	private Long tipeItem6;
	private Long tipeItem7;
	private Long tipeItem8;
	private Long tipeItem9;
	private Long tipeItem10;
	private Long tipeItem11;
	private Long tipeItem12;
	private Long tipeItem13;
	private Long tipeItem14;
	private Long tipeItem15;
	private Long tipeItem16;
	private Long tipeItem17;
	private Long tipeItem18;
	private Long tipeItem19;
	private Long tipeItem20;

	/** Daftar mata kuliah yang harus diambil <b>semuanya</b> (relasi DAN); lihat {@link #getKodeMatakuliahDan()}. */
	private String kodeMatakuliahDan;
	/** Angkatan yang boleh memakai format ini; dicocokkan dengan {@code contains}, bukan sama persis. */
	private String tahunAngkatan;
	/** Bila {@code true}, format hanya boleh ditetapkan petugas, tidak muncul di layar mahasiswa. */
	private Boolean tidakBolehDipilihMahasiswa;

	/** Kode jenis kegiatan versi lama (teks); dimigrasikan on-the-fly oleh {@link #getJenisKegiatanMahasiswa()}. */
	private String jenis;
	/** Relasi pengganti {@link #jenis}, dipakai ekspor PDDikti/Feeder. */
	private JenisKegiatanMahasiswa jenisKegiatanMahasiswa;
	/** Skala nilai huruf khusus format ini; bila diisi, menimpa skala milik mata kuliah. */
	private JenisNilaiHurufMatakuliah jenisNilaiHuruf;
	/** Izin mahasiswa mengubah sendiri agenda/jadwal bimbingan; lihat {@code PenjadwalanSkripsiHelper}. */
	private Boolean mahasiswaBolehMengubahAgendaAtauJadwalBimbingan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA sekaligus dipakai
	 * {@code FormatNilaiSkripsiAction} saat pengguna menekan "tambah data baru".
	 *
	 * <p>Tidak menyetel apa pun: semua nilai default berasal dari inisialisasi field (bobot
	 * {@code 0.0}, {@link #tanggal_dirubah} = waktu sekarang) atau dari getter null-safe. Karena
	 * seluruh bendera {@code dosenNAktif} masih {@code null}, format yang baru dibuat otomatis
	 * menganggap <b>tidak ada slot dosen yang aktif</b> sampai bobotnya diisi &gt; 0,1.</p>
	 */
	public FormatNilaiSkripsi() {
	}

	/**
	 * Mengembalikan kunci utama baris ini, dipakai di seluruh aplikasi sebagai pembanding
	 * kesamaan format (lihat kontrak {@code equals}/{@code compareTo} di
	 * {@link GeneralValueObject}).
	 *
	 * <p>Kolomnya {@code insertable = false} karena diisi oleh sekuens/identity database, jadi
	 * bernilai {@code null} sampai baris benar-benar tersimpan.</p>
	 *
	 * @return id baris, atau {@code null} untuk object yang belum pernah di-{@code persist}
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Dipanggil Hibernate; kode aplikasi praktis tidak pernah memakainya
	 * kecuali saat membangun object rujukan ringan.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama format seperti yang ditampilkan ke pengguna — dipakai luas sebagai teks
	 * {@code Comboitem} pada layar pengajuan sidang, judul kolom rekap nilai, dan bagian dari pesan
	 * peringatan saat syarat pendaftaran tidak terpenuhi.
	 *
	 * <p><b>Dua penggantian tersembunyi</b> (bukan sekadar null-safe): bila {@code nama} masih
	 * {@code null} <i>atau</i> berisi persis teks warisan {@code "Format Nilai Standard"}
	 * (perbandingan tanpa peduli besar-kecil huruf), yang dikembalikan adalah
	 * {@code "Pengajuan Sidang"}. Teks {@code "Format Nilai Standard"} adalah nama baris contoh
	 * yang dulu dibuat otomatis saat instalasi, dan penggantian ini membuatnya tampil dengan
	 * istilah yang lebih ramah tanpa perlu memutakhirkan data lama. Konsekuensinya: <b>nilai yang
	 * dikembalikan getter ini bisa berbeda dari isi kolom database</b>, jadi jangan memakainya
	 * untuk menyusun kriteria pencarian atau membandingkan dengan hasil query SQL langsung.</p>
	 *
	 * <p>Nama yang sah selalu di-{@code trim}. Entity sejenis {@link FormatNilaiProposalSkripsi}
	 * melakukan hal yang sama dengan teks pengganti {@code "Pengajuan Proposal"}.</p>
	 *
	 * @return nama format yang siap ditampilkan; tidak pernah {@code null} dan tidak pernah kosong
	 *         kecuali kolomnya memang berisi spasi
	 */
	public String getNama() {
		return nama == null || nama.trim().equalsIgnoreCase("Format Nilai Standard") ? "Pengajuan Sidang" : nama.trim();
	}

	/**
	 * Menyetel nama format apa adanya, tanpa {@code trim} dan tanpa penolakan nilai kosong.
	 * Menyimpan {@code "Format Nilai Standard"} di sini berarti pengguna akan melihat
	 * {@code "Pengajuan Sidang"} — lihat {@link #getNama()}.
	 *
	 * @param nama nama format; {@code null} diterima dan membuat {@link #getNama()} memakai teks
	 *             pengganti
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Menyetel bobot persentase nilai untuk slot dosen <b>{@code dosen1}</b> (label default
	 * "Pembimbing I") — bukan untuk ketua sidang, meski namanya berkata demikian. Lihat tabel
	 * pemetaan pada Javadoc class.
	 *
	 * <p>Menyetel nilai {@code > 0.1} di sini juga membuat {@link #getDosen1Aktif()} menganggap
	 * slot 1 aktif selama bendera {@code dosen1Aktif} belum pernah diisi eksplisit.</p>
	 *
	 * @param prosentasiNilaiKetuaSidang bobot dalam persen; {@code null} diterima dan diperlakukan
	 *                                   sebagai {@code 0.0} oleh getter-nya
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public void setProsentasiNilaiKetuaSidang(Double prosentasiNilaiKetuaSidang) {
		this.prosentasiNilaiKetuaSidang = prosentasiNilaiKetuaSidang;
	}

	/**
	 * Mengembalikan bobot persentase nilai slot dosen <b>{@code dosen1}</b> ({@link #getDosen1()},
	 * label default "Pembimbing I"), null-safe ke {@code 0.0}.
	 *
	 * <p><b>Nama kolomnya menyesatkan.</b> Kolom {@code prosentasi_nilai_ketua_sidang} adalah bobot
	 * slot pertama, yang berpasangan dengan kolom {@code nilai_ketua_sidang} pada tabel
	 * {@code skripsi} — dan kolom itu pun menyimpan nilai dari dosen yang tercatat di kolom
	 * {@code pembimbing}. Ketidakcocokan nama ini konsisten di seluruh aplikasi sehingga hasil di
	 * layar benar; yang keliru hanya penamaannya. Rincian lengkapnya ada pada Javadoc class dan
	 * pada {@link Skripsi}.</p>
	 *
	 * <p>Pemakai utamanya: {@code PenilaianSkripsiHelper} (mengalikan nilai dosen slot 1 dengan
	 * bobot ini saat menghitung total), {@code FormatNilaiSkripsiAction} (menampilkan
	 * "label/bobot"), dan {@link #getDosen1Aktif()}.</p>
	 *
	 * @return bobot dalam persen; {@code 0.0} bila belum pernah diisi
	 * @see Skripsi
	 */
	@Column(name = "prosentasi_nilai_ketua_sidang", nullable = true, precision = 15)
	public Double getProsentasiNilaiKetuaSidang() {
		return prosentasiNilaiKetuaSidang == null ? 0.0 : prosentasiNilaiKetuaSidang;
	}

	/**
	 * Menyetel bobot persentase nilai slot dosen <b>{@code dosen2}</b> (label default
	 * "Pembimbing II"). Perilakunya identik dengan
	 * {@link #setProsentasiNilaiKetuaSidang(Double)}, hanya slotnya yang berbeda.
	 *
	 * @param prosentasiNilaiPembimbing bobot dalam persen; {@code null} diterima
	 * @see #setProsentasiNilaiKetuaSidang(Double)
	 */
	public void setProsentasiNilaiPembimbing(Double prosentasiNilaiPembimbing) {
		this.prosentasiNilaiPembimbing = prosentasiNilaiPembimbing;
	}

	/**
	 * Mengembalikan bobot persentase nilai slot dosen <b>{@code dosen2}</b> ({@link #getDosen2()},
	 * label default "Pembimbing II"), null-safe ke {@code 0.0}. Berpasangan dengan kolom
	 * {@code nilai_pembimbing} pada tabel {@code skripsi} yang, sesuai pergeseran nama yang sama,
	 * menyimpan nilai dari dosen di kolom {@code ketua_sidang}.
	 *
	 * @return bobot dalam persen; {@code 0.0} bila belum pernah diisi
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	@Column(name = "prosentasi_nilai_pembimbing", nullable = true, precision = 15)
	public Double getProsentasiNilaiPembimbing() {
		return prosentasiNilaiPembimbing == null ? 0.0 : prosentasiNilaiPembimbing;
	}

	/**
	 * Menyetel bobot persentase nilai slot dosen {@code dosen3} (label default "Penguji I").
	 *
	 * @param prosentasiNilaiPenguji1 bobot dalam persen; {@code null} diterima
	 * @see #setProsentasiNilaiKetuaSidang(Double)
	 */
	public void setProsentasiNilaiPenguji1(Double prosentasiNilaiPenguji1) {
		this.prosentasiNilaiPenguji1 = prosentasiNilaiPenguji1;
	}

	/**
	 * Mengembalikan bobot persentase nilai slot dosen {@code dosen3} ({@link #getDosen3()}, label
	 * default "Penguji I"), null-safe ke {@code 0.0}. Berbeda dengan dua slot pertama, penamaan
	 * kolom slot penguji sudah lurus: {@code prosentasi_nilai_penguji_1} memang bobot Penguji I.
	 *
	 * @return bobot dalam persen; {@code 0.0} bila belum pernah diisi
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	@Column(name = "prosentasi_nilai_penguji_1", nullable = true, precision = 15)
	public Double getProsentasiNilaiPenguji1() {
		return prosentasiNilaiPenguji1 == null ? 0.0 : prosentasiNilaiPenguji1;
	}

	/**
	 * Menyetel bobot persentase nilai slot dosen {@code dosen4} (label default "Penguji II").
	 *
	 * @param prosentasiNilaiPenguji2 bobot dalam persen; {@code null} diterima
	 * @see #setProsentasiNilaiKetuaSidang(Double)
	 */
	public void setProsentasiNilaiPenguji2(Double prosentasiNilaiPenguji2) {
		this.prosentasiNilaiPenguji2 = prosentasiNilaiPenguji2;
	}

	/**
	 * Mengembalikan bobot persentase nilai slot dosen {@code dosen4} ({@link #getDosen4()}, label
	 * default "Penguji II"), null-safe ke {@code 0.0}.
	 *
	 * @return bobot dalam persen; {@code 0.0} bila belum pernah diisi
	 * @see #getProsentasiNilaiPenguji1()
	 */
	@Column(name = "prosentasi_nilai_penguji_2", nullable = true, precision = 15)
	public Double getProsentasiNilaiPenguji2() {
		return prosentasiNilaiPenguji2 == null ? 0.0 : prosentasiNilaiPenguji2;
	}

	/**
	 * Menyetel bobot persentase nilai slot dosen {@code dosen5} (label default "Penguji III").
	 *
	 * @param prosentasiNilaiPenguji3 bobot dalam persen; {@code null} diterima
	 * @see #setProsentasiNilaiKetuaSidang(Double)
	 */
	public void setProsentasiNilaiPenguji3(Double prosentasiNilaiPenguji3) {
		this.prosentasiNilaiPenguji3 = prosentasiNilaiPenguji3;
	}

	/**
	 * Mengembalikan bobot persentase nilai slot dosen {@code dosen5} ({@link #getDosen5()}, label
	 * default "Penguji III"), null-safe ke {@code 0.0}.
	 *
	 * @return bobot dalam persen; {@code 0.0} bila belum pernah diisi
	 * @see #getProsentasiNilaiPenguji1()
	 */
	@Column(name = "prosentasi_nilai_penguji_3", nullable = true, precision = 15)
	public Double getProsentasiNilaiPenguji3() {
		return prosentasiNilaiPenguji3 == null ? 0.0 : prosentasiNilaiPenguji3;
	}

	/**
	 * Menyetel bobot persentase nilai slot dosen {@code dosen6} (label default "Penguji IV").
	 *
	 * @param prosentasiNilaiPenguji4 bobot dalam persen; {@code null} diterima
	 * @see #setProsentasiNilaiKetuaSidang(Double)
	 */
	public void setProsentasiNilaiPenguji4(Double prosentasiNilaiPenguji4) {
		this.prosentasiNilaiPenguji4 = prosentasiNilaiPenguji4;
	}

	/**
	 * Mengembalikan bobot persentase nilai slot dosen {@code dosen6} ({@link #getDosen6()}, label
	 * default "Penguji IV"), null-safe ke {@code 0.0}.
	 *
	 * @return bobot dalam persen; {@code 0.0} bila belum pernah diisi
	 * @see #getProsentasiNilaiPenguji1()
	 */
	@Column(name = "prosentasi_nilai_penguji_4", nullable = true, precision = 15)
	public Double getProsentasiNilaiPenguji4() {
		return prosentasiNilaiPenguji4 == null ? 0.0 : prosentasiNilaiPenguji4;
	}

	/**
	 * Menyetel jurusan pemilik format ini. {@code null} berarti format berlaku lintas jurusan.
	 *
	 * @param jurusan jurusan penargetan, atau {@code null} untuk semua jurusan
	 * @see #getJurusan()
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan jurusan yang menjadi sasaran format ini; {@code null} berarti format tidak
	 * dibatasi per jurusan.
	 *
	 * <p>Ini kriteria penargetan dengan <b>bobot tertinggi</b>: saat beberapa format sama-sama
	 * memenuhi syarat untuk seorang mahasiswa, {@code SkripsiAction} memberi 16 poin bila
	 * jurusannya cocok (dibanding 8 untuk fakultas, 4 program, 2 status awal, 1 angkatan) dan
	 * menaruh format berskor tertinggi di urutan teratas daftar pilihan.</p>
	 *
	 * <p><b>Berefek samping:</b> memanggil {@link GeneralValueObject#check(Object)} lalu
	 * <i>menyimpan hasilnya kembali</i> ke field. Bila entity sudah detached, {@code check} dapat
	 * membuka session Hibernate baru untuk memuat ulang proxy.</p>
	 *
	 * @return jurusan penargetan, atau {@code null} bila berlaku untuk semua jurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel fakultas pemilik format ini. {@code null} berarti format berlaku lintas fakultas.
	 *
	 * @param fakultas fakultas penargetan, atau {@code null} untuk semua fakultas
	 * @see #getFakultas()
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan fakultas yang menjadi sasaran format ini; {@code null} berarti tidak dibatasi
	 * per fakultas. Bernilai 8 poin pada penyaringan format (lihat {@link #getJurusan()}).
	 *
	 * <p>Berefek samping lewat {@link GeneralValueObject#check(Object)}, sama seperti
	 * {@link #getJurusan()}.</p>
	 *
	 * @return fakultas penargetan, atau {@code null} bila berlaku untuk semua fakultas
	 * @see #getJurusan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menyetel program studi/jenjang sasaran (teks bebas, maksimal 50 karakter, mis. "S1", "S2",
	 * "Reguler"), apa adanya tanpa {@code trim}.
	 *
	 * @param program nama program; {@code null} atau kosong berarti berlaku untuk semua program
	 * @see #getProgram()
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan program studi/jenjang sasaran, sudah di-{@code trim}. Berbeda dengan
	 * kebanyakan getter teks di class ini yang mengembalikan string kosong, getter ini
	 * <b>menormalkan string kosong menjadi {@code null}</b> — perlu diperhatikan karena pemanggil
	 * memakai {@code null} sebagai penanda "berlaku untuk semua program" (bernilai 4 poin pada
	 * penyaringan format, lihat {@link #getJurusan()}).
	 *
	 * @return nama program yang sudah dirapikan, atau {@code null} bila tidak dibatasi
	 */
	@Column(name = "program", length = 50)
	public String getProgram() {
		return program == null || program.trim().isEmpty() ? null : program.trim();
	}

	/**
	 * Menyetel status awal mahasiswa (mis. reguler, pindahan, alih jenjang) yang disasar format
	 * ini.
	 *
	 * @param statusAwalMahasiswa status awal sasaran, atau {@code null} untuk semua status
	 * @see #getStatusAwalMahasiswa()
	 */
	public void setStatusAwalMahasiswa(StatusAwalMahasiswa statusAwalMahasiswa) {
		this.statusAwalMahasiswa = statusAwalMahasiswa;
	}

	/**
	 * Mengembalikan status awal mahasiswa yang disasar format ini; {@code null} berarti tidak
	 * dibatasi. Bernilai 2 poin pada penyaringan format (lihat {@link #getJurusan()}), dan pada
	 * layar pengajuan dibandingkan dengan status awal <i>pengajuan</i> (yang bisa berbeda dari
	 * status mahasiswa saat ini bila ada riwayat pengajuan sebelumnya).
	 *
	 * <p>Berefek samping lewat {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return status awal sasaran, atau {@code null} bila berlaku untuk semua status
	 * @see #getJurusan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswa() {
		statusAwalMahasiswa = check(statusAwalMahasiswa);
		return statusAwalMahasiswa;
	}

	/**
	 * Menyatakan apakah format ini masih boleh dipakai untuk pengajuan baru.
	 *
	 * <p><b>Default-nya {@code true}</b> — format lama yang kolomnya masih {@code null} tetap
	 * dianggap aktif. Query penyaringan di {@code SkripsiAction} mengikuti pola yang sama
	 * ({@code aktif IS NULL OR aktif = true}), jadi untuk menonaktifkan sebuah format kolomnya
	 * harus diisi {@code false} secara eksplisit; mengosongkannya tidak cukup.</p>
	 *
	 * <p>Menonaktifkan format tidak memengaruhi skripsi yang sudah terlanjur memakainya — nilai
	 * dan bobot yang sudah tersimpan tetap dihitung dengan format tersebut.</p>
	 *
	 * @return {@code true} bila format masih dapat dipilih
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif format. Isi {@code false} untuk memensiunkan format; {@code null}
	 * dibaca kembali sebagai {@code true} oleh {@link #getAktif()}.
	 *
	 * @param aktif status aktif; {@code null} diterima dan berarti "aktif"
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan <b>label peran</b> slot dosen pertama, dengan default {@code "Pembimbing I"}
	 * bila kolomnya belum diisi atau hanya berisi spasi.
	 *
	 * <p>Label ini jauh lebih penting daripada sekadar teks tampilan: ia menjadi <b>kunci
	 * pencocokan</b> peran dosen di seluruh alur penilaian. {@code Skripsi.dataDosen(boolean)}
	 * mengemas label ini ke dalam {@code CommonVO} bersama dosen yang menempati slot, lalu
	 * {@code PenilaianSkripsiHelper} membandingkannya kembali dengan {@code getDosen1()} .. untuk
	 * menentukan nilai mana, bobot mana, dan daftar komponen penilaian mana yang berlaku. Karena
	 * itu:</p>
	 * <ul>
	 * <li>dua slot dengan label identik pada satu format akan bertabrakan — cabang {@code if}
	 * pertama yang menang;</li>
	 * <li>mengubah label pada format yang sudah dipakai <b>tidak</b> mengubah data nilai yang sudah
	 * tersimpan (nilai disimpan per kolom, bukan per label), tetapi mengubah kecocokan pada
	 * layar/laporan yang mencocokkan berdasarkan label.</li>
	 * </ul>
	 *
	 * <p>Slot ini berpasangan dengan bobot {@link #getProsentasiNilaiKetuaSidang()}, bendera
	 * {@link #getDosen1Aktif()}, kode {@link #getKode1()}, kolom orang {@code skripsi.pembimbing}
	 * dan kolom nilai {@code skripsi.nilai_ketua_sidang} — perhatikan pergeseran nama yang
	 * dijelaskan pada Javadoc class.</p>
	 *
	 * <p><b>Catatan konsistensi:</b> {@link VOPembelajaran} memetakan label ini ke
	 * {@code Skripsi.getKetuaSidang()} (slot 2), berlawanan dengan pemetaan kanonik di
	 * {@code Skripsi.dataDosen(boolean)}. Dicatat apa adanya, tidak diperbaiki.</p>
	 *
	 * <p>Perhatikan pula bahwa nilai kembalian <b>tidak</b> di-{@code trim} bila kolomnya terisi;
	 * spasi di ujung label akan membuat pencocokan {@code equals} gagal.</p>
	 *
	 * @return label peran slot 1; tidak pernah {@code null}
	 */
	public String getDosen1() {
		return dosen1 == null || dosen1.trim().isEmpty() ? "Pembimbing I" : dosen1;
	}

	/**
	 * Menyetel label peran slot dosen pertama, apa adanya (tanpa {@code trim}). Mengosongkannya
	 * mengembalikan label ke default {@code "Pembimbing I"}.
	 *
	 * @param dosen1 label peran slot 1
	 * @see #getDosen1()
	 */
	public void setDosen1(String dosen1) {
		this.dosen1 = dosen1;
	}

	/**
	 * Mengembalikan label peran slot dosen kedua, default {@code "Pembimbing II"}. Perilaku dan
	 * jebakan pencocokan berbasis teksnya persis sama dengan {@link #getDosen1()}; slot ini
	 * berpasangan dengan bobot {@link #getProsentasiNilaiPembimbing()}, bendera
	 * {@link #getDosen2Aktif()}, kode {@link #getKode2()}, serta kolom {@code skripsi.ketua_sidang}
	 * / {@code skripsi.nilai_pembimbing}.
	 *
	 * @return label peran slot 2; tidak pernah {@code null}
	 * @see #getDosen1()
	 */
	public String getDosen2() {
		return dosen2 == null || dosen2.trim().isEmpty() ? "Pembimbing II" : dosen2;
	}

	/**
	 * Menyetel label peran slot dosen kedua.
	 *
	 * @param dosen2 label peran slot 2
	 * @see #getDosen1()
	 */
	public void setDosen2(String dosen2) {
		this.dosen2 = dosen2;
	}

	/**
	 * Mengembalikan label peran slot dosen {@code dosen3}, default {@code "Penguji I"}.
	 * Berpasangan dengan {@link #getProsentasiNilaiPenguji1()}, {@link #getDosen3Aktif()},
	 * {@link #getKode3()}, dan kolom {@code skripsi.penguji1}/{@code skripsi.nilai_penguji1}.
	 *
	 * @return label peran slot {@code dosen3}; tidak pernah {@code null}
	 * @see #getDosen1()
	 */
	public String getDosen3() {
		return dosen3 == null || dosen3.trim().isEmpty() ? "Penguji I" : dosen3;
	}

	/**
	 * Menyetel label peran slot dosen {@code dosen3}.
	 *
	 * @param dosen3 label peran slot {@code dosen3}
	 * @see #getDosen1()
	 */
	public void setDosen3(String dosen3) {
		this.dosen3 = dosen3;
	}

	/**
	 * Mengembalikan label peran slot dosen {@code dosen4}, default {@code "Penguji II"}.
	 * Berpasangan dengan {@link #getProsentasiNilaiPenguji2()}, {@link #getDosen4Aktif()} dan
	 * {@link #getKode4()}.
	 *
	 * @return label peran slot {@code dosen4}; tidak pernah {@code null}
	 * @see #getDosen1()
	 */
	public String getDosen4() {
		return dosen4 == null || dosen4.trim().isEmpty() ? "Penguji II" : dosen4;
	}

	/**
	 * Menyetel label peran slot dosen {@code dosen4}.
	 *
	 * @param dosen4 label peran slot {@code dosen4}
	 * @see #getDosen1()
	 */
	public void setDosen4(String dosen4) {
		this.dosen4 = dosen4;
	}

	/**
	 * Mengembalikan label peran slot dosen {@code dosen5}, default {@code "Penguji III"}.
	 * Berpasangan dengan {@link #getProsentasiNilaiPenguji3()}, {@link #getDosen5Aktif()} dan
	 * {@link #getKode5()}.
	 *
	 * @return label peran slot {@code dosen5}; tidak pernah {@code null}
	 * @see #getDosen1()
	 */
	public String getDosen5() {
		return dosen5 == null || dosen5.trim().isEmpty() ? "Penguji III" : dosen5;
	}

	/**
	 * Menyetel label peran slot dosen {@code dosen5}.
	 *
	 * @param dosen5 label peran slot {@code dosen5}
	 * @see #getDosen1()
	 */
	public void setDosen5(String dosen5) {
		this.dosen5 = dosen5;
	}

	/**
	 * Mengembalikan label peran slot dosen {@code dosen6}, default {@code "Penguji IV"}.
	 * Berpasangan dengan {@link #getProsentasiNilaiPenguji4()}, {@link #getDosen6Aktif()} dan
	 * {@link #getKode6()}.
	 *
	 * @return label peran slot {@code dosen6}; tidak pernah {@code null}
	 * @see #getDosen1()
	 */
	public String getDosen6() {
		return dosen6 == null || dosen6.trim().isEmpty() ? "Penguji IV" : dosen6;
	}

	/**
	 * Menyetel label peran slot dosen {@code dosen6}.
	 *
	 * @param dosen6 label peran slot {@code dosen6}
	 * @see #getDosen1()
	 */
	public void setDosen6(String dosen6) {
		this.dosen6 = dosen6;
	}

	/**
	 * Mengembalikan daftar kode/nama mata kuliah tugas akhir yang harus ada di KRS mahasiswa,
	 * dengan hubungan <b>ATAU</b> (cukup salah satu) — bandingkan dengan
	 * {@link #getKodeMatakuliahDan()} yang berhubungan DAN. Formatnya string dipisah koma;
	 * tiap token dicocokkan ke {@code Matakuliah} berdasarkan {@code kode} <i>atau</i>
	 * {@code nama} secara persis (tanpa peduli besar-kecil huruf).
	 *
	 * <p>Nilai ini dipakai untuk dua hal: (a) menampilkan daftar mata kuliah tugas akhir di
	 * samping nama format pada layar pemilihan, dan (b) mencari baris
	 * {@code Detailperkuliahan}/KRS yang akan menampung nilai tugas akhir
	 * ({@code cariDetailperkuliahanSkripsi}).</p>
	 *
	 * <p><b>Getter ini TIDAK murni — ia menulis balik ke field.</b> Alurnya: nilai dibungkus koma
	 * di kedua ujung ({@code ",A,B,"}), koma ganda dirapatkan dengan tiga kali
	 * {@code replaceAll(",,", ",")} berturut-turut, lalu hasil yang hanya berisi koma
	 * ({@code ","}, {@code ",,"}, {@code ",,,"}) dikosongkan. Hasil akhirnya <b>disimpan kembali
	 * ke field {@code kodeMatakuliah}</b>, sehingga sekadar membaca property ini dapat membuat
	 * Hibernate menganggap entity kotor dan menerbitkan {@code UPDATE} saat flush. Lebih jauh
	 * lagi, bila {@link #getTidakWajibMengambilMkTertentu()} bernilai {@code true} field ini
	 * <b>dikosongkan</b> — artinya mencentang opsi itu lalu menyimpan format akan benar-benar
	 * menghapus daftar mata kuliah dari database, bukan sekadar mengabaikannya.</p>
	 *
	 * <p>Efek samping yang lebih halus: karena hasilnya selalu diawali dan diakhiri koma, memecah
	 * hasil dengan {@code split(",")} menghasilkan token pertama berupa string kosong — semua
	 * pemanggil di aplikasi ini sudah melewatkan token kosong, tapi kode baru perlu berhati-hati.
	 * Pemeriksaan {@code if (kodeMatakuliah == null)} pada baris berikutnya tidak pernah bernilai
	 * benar karena ekspresi sebelumnya selalu menghasilkan string.</p>
	 *
	 * @return daftar kode/nama mata kuliah dipisah koma (biasanya diawali dan diakhiri koma), atau
	 *         string kosong bila tidak ada syarat mata kuliah; tidak pernah {@code null}
	 * @see #getKodeMatakuliahDan()
	 * @see #getTidakWajibMengambilMkTertentu()
	 */
	public String getKodeMatakuliah() {
		kodeMatakuliah = (kodeMatakuliah == null || kodeMatakuliah.trim().equalsIgnoreCase(",") ? ""
				: "," + kodeMatakuliah.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (kodeMatakuliah.equals(",")) {
			kodeMatakuliah = "";
		} else if (kodeMatakuliah.equals(",,")) {
			kodeMatakuliah = "";
		} else if (kodeMatakuliah.equals(",,,")) {
			kodeMatakuliah = "";
		}

		if (kodeMatakuliah == null) {
			kodeMatakuliah = "";
		}

		if (getTidakWajibMengambilMkTertentu()) {
			kodeMatakuliah = "";
		}

		return kodeMatakuliah;
	}

	/**
	 * Menyetel daftar kode/nama mata kuliah tugas akhir berelasi ATAU, apa adanya. Normalisasi
	 * koma baru terjadi saat dibaca lewat {@link #getKodeMatakuliah()}.
	 *
	 * @param kodeMatakuliah daftar kode/nama dipisah koma; {@code null} diterima
	 * @see #getKodeMatakuliah()
	 */
	public void setKodeMatakuliah(String kodeMatakuliah) {
		this.kodeMatakuliah = kodeMatakuliah;
	}

	/**
	 * Mengembalikan jumlah SKS minimal yang harus sudah diperoleh mahasiswa untuk boleh mendaftar
	 * sidang, null-safe ke {@code 0} (artinya tanpa syarat SKS).
	 *
	 * <p>Dibandingkan oleh {@code SkripsiAction} dengan {@code KrsMahasiswa.getSksk()} — SKS
	 * kumulatif hasil sinkronisasi KRS, bukan SKS semester berjalan. Bila kurang, pengajuan
	 * ditolak dengan pesan yang menyebutkan angka minimal dan angka yang sudah dicapai.</p>
	 *
	 * @return SKS minimal; {@code 0} bila tidak disyaratkan
	 */
	public Integer getMinimalSks() {
		return minimalSks == null ? 0 : minimalSks;
	}

	/**
	 * Menyetel jumlah SKS minimal untuk mendaftar sidang.
	 *
	 * @param minimalSks SKS minimal; {@code null} diterima dan dibaca sebagai {@code 0}
	 * @see #getMinimalSks()
	 */
	public void setMinimalSks(Integer minimalSks) {
		this.minimalSks = minimalSks;
	}

	/**
	 * Mengembalikan IPK minimal yang harus dicapai untuk boleh mendaftar sidang, null-safe ke
	 * {@code 0.0} (tanpa syarat). Dibandingkan dengan {@code KrsMahasiswa.getIpk()}.
	 *
	 * @return IPK minimal; {@code 0.0} bila tidak disyaratkan
	 * @see #getMinimalSks()
	 */
	public Double getMinimalIpk() {
		return minimalIpk == null ? 0.0 : minimalIpk;
	}

	/**
	 * Menyetel IPK minimal untuk mendaftar sidang.
	 *
	 * @param minimalIpk IPK minimal; {@code null} diterima dan dibaca sebagai {@code 0.0}
	 * @see #getMinimalIpk()
	 */
	public void setMinimalIpk(Double minimalIpk) {
		this.minimalIpk = minimalIpk;
	}

	/**
	 * Mengembalikan angka kredit kegiatan kemahasiswaan minimal (poin SKPI/keaktifan, dihitung
	 * {@code Common.hitungAngkaKredit(Mahasiswa)}) yang harus dikumpulkan untuk boleh mendaftar
	 * sidang, null-safe ke {@code 0.0}.
	 *
	 * @return angka kredit minimal; {@code 0.0} bila tidak disyaratkan
	 * @see #getMinimalSks()
	 */
	public Double getMinimalAngkaKredit() {
		return minimalAngkaKredit == null ? 0.0 : minimalAngkaKredit;
	}

	/**
	 * Menyetel angka kredit kegiatan kemahasiswaan minimal untuk mendaftar sidang.
	 *
	 * @param minimalAngkaKredit angka kredit minimal; {@code null} diterima
	 * @see #getMinimalAngkaKredit()
	 */
	public void setMinimalAngkaKredit(Double minimalAngkaKredit) {
		this.minimalAngkaKredit = minimalAngkaKredit;
	}

	/**
	 * Mengembalikan daftar mata kuliah yang harus <b>sudah LULUS</b> sebelum mahasiswa boleh
	 * mendaftar sidang — berbeda dari {@link #getKodeMatakuliah()} yang hanya menuntut mata kuliah
	 * tugas akhir <i>diambil</i> di KRS.
	 *
	 * <p>Formatnya daftar dipisah koma berisi kode <i>atau</i> nama mata kuliah. {@code SkripsiAction}
	 * mengumpulkan seluruh {@code Detailperkuliahan} mahasiswa yang berstatus lulus, memasukkan
	 * kode dan nama mata kuliahnya (huruf besar) ke sebuah himpunan, lalu menolak pengajuan sambil
	 * menyebutkan token mana saja yang belum terpenuhi. Mata kuliah konversi ikut dihitung.</p>
	 *
	 * <p>Berbeda dengan kebanyakan getter di class ini, yang satu ini <b>tidak null-safe</b>:
	 * ia mengembalikan {@code null} apa adanya (pemanggil memang sudah memeriksanya). Kolomnya
	 * dipetakan sebagai {@code text} sehingga daftarnya boleh sangat panjang. Kosong berarti tanpa
	 * prasyarat mata kuliah.</p>
	 *
	 * @return daftar kode/nama mata kuliah prasyarat lulus dipisah koma, atau {@code null}
	 */
	@javax.persistence.Column(columnDefinition = "text")
	public String getMatkulPrasyaratLulus() {
		return matkulPrasyaratLulus;
	}

	/**
	 * Menyetel daftar mata kuliah prasyarat lulus, apa adanya (tanpa {@code trim} dan tanpa
	 * validasi bahwa kode/nama-nya benar-benar ada).
	 *
	 * @param matkulPrasyaratLulus daftar kode/nama dipisah koma; {@code null}/kosong berarti tanpa
	 *                             prasyarat
	 * @see #getMatkulPrasyaratLulus()
	 */
	public void setMatkulPrasyaratLulus(String matkulPrasyaratLulus) {
		this.matkulPrasyaratLulus = matkulPrasyaratLulus;
	}

	/**
	 * Saklar utama syarat keuangan: bila {@code true}, mahasiswa hanya boleh mendaftar sidang
	 * setelah memenuhi ambang pelunasan {@link #getProsentaseLunas()}. Null-safe ke {@code false}.
	 *
	 * <p>Perhatikan pembagian tugas yang agak berbelit antara kedua field: {@code SkripsiAction}
	 * lebih dulu memeriksa {@code getProsentaseLunas() > 0.1} sebagai gerbang, baru memanggil
	 * {@code Common.checkStatusPembayaranMahasiswaPengajuanSidang(...)} yang <i>di dalamnya</i>
	 * memeriksa {@code getHarusLunas()}. Akibatnya syarat pembayaran baru benar-benar aktif bila
	 * <b>kedua</b> field terisi: {@code harusLunas = true} DAN {@code prosentaseLunas > 0.1}.
	 * Pemeriksaan itu sendiri masih dilewati untuk semester awal mahasiswa (semester 1 atau
	 * semester-semester pertama setelah pindah kampus).</p>
	 *
	 * @return {@code true} bila pelunasan biaya menjadi syarat pendaftaran
	 * @see #getProsentaseLunas()
	 */
	public Boolean getHarusLunas() {
		return harusLunas == null ? false : harusLunas;
	}

	/**
	 * Menyetel saklar syarat pelunasan biaya.
	 *
	 * @param harusLunas {@code true} bila pelunasan menjadi syarat; {@code null} dibaca sebagai
	 *                   {@code false}
	 * @see #getHarusLunas()
	 */
	public void setHarusLunas(Boolean harusLunas) {
		this.harusLunas = harusLunas;
	}

	/**
	 * Mengembalikan ambang persentase pelunasan biaya perkuliahan semester berjalan yang harus
	 * dicapai untuk boleh mendaftar sidang, null-safe ke {@code 0.0}.
	 *
	 * <p>Nilai {@code <= 0.1} berarti syarat ini dilewati sama sekali (lihat penjelasan pada
	 * {@link #getHarusLunas()}). Angka ini juga ikut disebut apa adanya di pesan penolakan yang
	 * ditampilkan ke pengguna.</p>
	 *
	 * @return ambang pelunasan dalam persen; {@code 0.0} bila tidak disyaratkan
	 * @see #getHarusLunas()
	 */
	public Double getProsentaseLunas() {
		return prosentaseLunas == null ? 0.0 : prosentaseLunas;
	}

	/**
	 * Menyetel ambang persentase pelunasan biaya.
	 *
	 * @param prosentaseLunas ambang dalam persen; {@code null} diterima
	 * @see #getProsentaseLunas()
	 */
	public void setProsentaseLunas(Double prosentaseLunas) {
		this.prosentaseLunas = prosentaseLunas;
	}

	/**
	 * Menyatakan apakah mahasiswa wajib sudah mengembalikan seluruh pinjaman perpustakaan sebelum
	 * boleh mendaftar sidang, null-safe ke {@code false}.
	 *
	 * <p>Bila {@code true}, {@code SkripsiAction} mencari semua
	 * {@code PeminjamanPengadaanItemDetail} milik mahasiswa yang belum ada baris pengembaliannya
	 * dan menolak pengajuan sambil merinci judul item beserta jumlah hari keterlambatannya. Semua
	 * pinjaman yang belum kembali menghalangi, tidak hanya yang terlambat.</p>
	 *
	 * @return {@code true} bila bebas pustaka menjadi syarat pendaftaran
	 */
	public Boolean getHarusMengembalikanBukuPerpustakaan() {
		return harusMengembalikanBukuPerpustakaan == null ? false : harusMengembalikanBukuPerpustakaan;
	}

	/**
	 * Menyetel syarat bebas pinjaman perpustakaan.
	 *
	 * @param harusMengembalikanBukuPerpustakaan {@code true} bila menjadi syarat; {@code null}
	 *                                           dibaca sebagai {@code false}
	 * @see #getHarusMengembalikanBukuPerpustakaan()
	 */
	public void setHarusMengembalikanBukuPerpustakaan(Boolean harusMengembalikanBukuPerpustakaan) {
		this.harusMengembalikanBukuPerpustakaan = harusMengembalikanBukuPerpustakaan;
	}

	/**
	 * Mengembalikan daftar kode {@code ItemBiaya} (dipisah koma) yang harus sudah dibayar untuk
	 * biaya sidang, sudah di-{@code trim} dan null-safe ke string kosong.
	 *
	 * <p>Untuk setiap kode, {@code SkripsiAction} mencari {@code ItemBiaya} yang cocok lalu
	 * memanggil {@code Mahasiswa.hitungTotalCicilanPembayaran(semester, }{@link #getSekaliBayar()}{@code ,
	 * null, kode)}; bila totalnya nol (belum ada pembayaran sama sekali) dan mahasiswa tidak punya
	 * dispensasi "baypass", pengajuan ditolak. Kode yang tidak ditemukan di master biaya diabaikan
	 * diam-diam.</p>
	 *
	 * <p>Berbeda dari {@link #getKodeMatakuliah()}, getter ini <b>tidak</b> menulis balik ke field
	 * dan tidak menambahkan koma pembungkus.</p>
	 *
	 * @return daftar kode item biaya dipisah koma, atau string kosong; tidak pernah {@code null}
	 * @see #getSekaliBayar()
	 */
	public String getKodeItemBiaya() {
		return kodeItemBiaya == null ? "" : kodeItemBiaya.trim();
	}

	/**
	 * Menyetel daftar kode item biaya sidang, apa adanya.
	 *
	 * @param kodeItemBiaya daftar kode dipisah koma; {@code null} diterima
	 * @see #getKodeItemBiaya()
	 */
	public void setKodeItemBiaya(String kodeItemBiaya) {
		this.kodeItemBiaya = kodeItemBiaya;
	}

	/**
	 * Mengembalikan bobot format ini terhadap nilai akhir mata kuliah tugas akhir, dengan
	 * <b>default {@code 100.0}</b> (bukan {@code 0.0} seperti bobot per dosen) — sehingga format
	 * yang belum dikonfigurasi otomatis menyumbang penuh.
	 *
	 * <p>Bobot ini dipakai ketika satu mata kuliah tugas akhir dinilai dari <b>beberapa format
	 * sekaligus</b>, misalnya nilai seminar proposal ({@link FormatNilaiProposalSkripsi}) digabung
	 * dengan nilai sidang akhir. {@code GradingHelper} menjumlahkan bobot semua format yang punya
	 * nilai untuk mahasiswa tersebut menjadi {@code totalPersen}, lalu tiap total nilai dikalikan
	 * bobotnya dan dibagi {@code totalPersen}. Karena penyebutnya dihitung dari format yang
	 * <i>benar-benar bernilai</i>, bobot tidak wajib berjumlah 100 dan format yang belum dinilai
	 * tidak menyeret rata-rata ke bawah.</p>
	 *
	 * <p>Jangan tertukar dengan {@code KomponenPenilaianSkripsi.getBobot()} (bobot butir penilaian
	 * di dalam satu format) maupun dengan bobot per slot dosen
	 * ({@code getProsentasiNilai*()}).</p>
	 *
	 * @return bobot format dalam persen; {@code 100.0} bila belum pernah diisi
	 */
	public Double getBobot() {
		return bobot == null ? 100.0 : bobot;
	}

	/**
	 * Menyetel bobot format terhadap nilai mata kuliah tugas akhir. Menyimpan {@code null}
	 * membuat getter mengembalikan {@code 100.0}, bukan {@code 0.0}.
	 *
	 * @param bobot bobot dalam persen; {@code null} diterima
	 * @see #getBobot()
	 */
	public void setBobot(Double bobot) {
		this.bobot = bobot;
	}

	/**
	 * Menyatakan bahwa format ini tidak menuntut mata kuliah tugas akhir tertentu ada di KRS,
	 * null-safe ke {@code false}.
	 *
	 * <p><b>Efeknya destruktif, bukan sekadar mengabaikan.</b> Ketika bernilai {@code true},
	 * {@link #getKodeMatakuliah()} dan {@link #getKodeMatakuliahDan()} <i>mengosongkan field
	 * masing-masing</i> saat dibaca, sehingga daftar mata kuliah yang pernah dikonfigurasi akan
	 * hilang dari database begitu entity ini ter-flush. Mencentang opsi ini lalu membatalkannya
	 * kembali tidak memulihkan daftar lama.</p>
	 *
	 * @return {@code true} bila syarat mata kuliah di KRS ditiadakan
	 * @see #getKodeMatakuliah()
	 */
	public Boolean getTidakWajibMengambilMkTertentu() {
		return tidakWajibMengambilMkTertentu == null ? false : tidakWajibMengambilMkTertentu;
	}

	/**
	 * Menyetel opsi "tidak wajib mengambil mata kuliah tertentu". Perhatikan efek pengosongan
	 * daftar mata kuliah yang dijelaskan pada {@link #getTidakWajibMengambilMkTertentu()}.
	 *
	 * @param tidakWajibMengambilMkTertentu {@code true} untuk meniadakan syarat mata kuliah
	 * @see #getTidakWajibMengambilMkTertentu()
	 */
	public void setTidakWajibMengambilMkTertentu(Boolean tidakWajibMengambilMkTertentu) {
		this.tidakWajibMengambilMkTertentu = tidakWajibMengambilMkTertentu;
	}

	/**
	 * Mengembalikan <b>judul</b> slot lampiran ke-1 — teks yang dilihat mahasiswa sebagai nama
	 * berkas yang harus diunggah (mis. "Naskah Skripsi", "Lembar Persetujuan"). Sudah di-{@code
	 * trim} dan null-safe ke string kosong.
	 *
	 * <p>Judul kosong berarti <b>slot tidak dipakai</b>: {@code SkripsiAction} dan
	 * {@code MahasiswaRequestTugasAkhirAction} melewati slot berjudul kosong sehingga barisnya
	 * tidak dirender sama sekali. Bila slot dipakai dan
	 * {@link #getUploadLampiran1Wajib()} bernilai {@code true}, pengajuan diblokir dengan pesan
	 * "&lt;judul&gt; wajib diupload !" selama berkasnya belum ada. Judul yang sama juga dipakai
	 * {@code LibraryUtil} sebagai nama item pustaka bila slot ditautkan lewat
	 * {@link #getTipeItem1()}.</p>
	 *
	 * <p>Sepuluh slot pertama, lalu 11-15 dan 16-20, dideklarasikan dalam tiga gelombang
	 * berbeda di file ini; perilaku ketiganya identik. Getter ini adalah acuan pola untuk
	 * {@code getUploadLampiran2()} sampai {@code getUploadLampiran20()}.</p>
	 *
	 * @return judul slot lampiran ke-1, atau string kosong bila slot tidak dipakai; tidak pernah
	 *         {@code null}
	 * @see #getUploadLampiran1Wajib()
	 * @see #getTipeItem1()
	 */
	public String getUploadLampiran1() {
		return uploadLampiran1 == null ? "" : uploadLampiran1.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-1, apa adanya (tanpa {@code trim}). Mengosongkannya membuat
	 * slot tidak lagi ditampilkan. Acuan pola bagi seluruh {@code setUploadLampiranN(String)}.
	 *
	 * @param uploadLampiran1 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran1(String uploadLampiran1) {
		this.uploadLampiran1 = uploadLampiran1;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-2. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-2, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran2() {
		return uploadLampiran2 == null ? "" : uploadLampiran2.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-2, apa adanya.
	 *
	 * @param uploadLampiran2 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran2(String uploadLampiran2) {
		this.uploadLampiran2 = uploadLampiran2;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-3. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-3, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran3() {
		return uploadLampiran3 == null ? "" : uploadLampiran3.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-3, apa adanya.
	 *
	 * @param uploadLampiran3 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran3(String uploadLampiran3) {
		this.uploadLampiran3 = uploadLampiran3;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-4. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-4, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran4() {
		return uploadLampiran4 == null ? "" : uploadLampiran4.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-4, apa adanya.
	 *
	 * @param uploadLampiran4 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran4(String uploadLampiran4) {
		this.uploadLampiran4 = uploadLampiran4;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-5. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-5, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran5() {
		return uploadLampiran5 == null ? "" : uploadLampiran5.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-5, apa adanya.
	 *
	 * @param uploadLampiran5 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran5(String uploadLampiran5) {
		this.uploadLampiran5 = uploadLampiran5;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-6. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-6, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran6() {
		return uploadLampiran6 == null ? "" : uploadLampiran6.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-6, apa adanya.
	 *
	 * @param uploadLampiran6 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran6(String uploadLampiran6) {
		this.uploadLampiran6 = uploadLampiran6;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-7. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-7, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran7() {
		return uploadLampiran7 == null ? "" : uploadLampiran7.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-7, apa adanya.
	 *
	 * @param uploadLampiran7 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran7(String uploadLampiran7) {
		this.uploadLampiran7 = uploadLampiran7;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-8. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-8, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran8() {
		return uploadLampiran8 == null ? "" : uploadLampiran8.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-8, apa adanya.
	 *
	 * @param uploadLampiran8 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran8(String uploadLampiran8) {
		this.uploadLampiran8 = uploadLampiran8;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-9. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-9, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran9() {
		return uploadLampiran9 == null ? "" : uploadLampiran9.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-9, apa adanya.
	 *
	 * @param uploadLampiran9 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran9(String uploadLampiran9) {
		this.uploadLampiran9 = uploadLampiran9;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-10. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-10, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran10() {
		return uploadLampiran10 == null ? "" : uploadLampiran10.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-10, apa adanya.
	 *
	 * @param uploadLampiran10 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran10(String uploadLampiran10) {
		this.uploadLampiran10 = uploadLampiran10;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-1 <b>wajib</b> diunggah sebelum pengajuan sidang boleh
	 * disimpan, null-safe ke {@code false}.
	 *
	 * <p>Bendera ini hanya bermakna bila slotnya memang dipakai, yaitu {@link #getUploadLampiran1()}
	 * tidak kosong. Saat wajib dan berkasnya belum ada, layar pengajuan
	 * ({@code SkripsiAction} untuk sidang, {@code MahasiswaRequestTugasAkhirAction} untuk pengajuan
	 * judul) menolak penyimpanan dengan pesan "&lt;judul lampiran&gt; wajib diupload !", dan pada
	 * form judul slotnya diberi tanda bintang. Yang diperiksa hanya <b>keberadaan</b> berkas — bukan
	 * jenis, ukuran, maupun isinya.</p>
	 *
	 * <p>Acuan pola bagi {@code getUploadLampiran2Wajib()} sampai
	 * {@code getUploadLampiran20Wajib()}.</p>
	 *
	 * @return {@code true} bila lampiran slot ke-1 wajib diunggah
	 * @see #getUploadLampiran1()
	 */
	public Boolean getUploadLampiran1Wajib() {
		return uploadLampiran1Wajib == null ? false : uploadLampiran1Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-1.
	 *
	 * @param uploadLampiran1Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran1Wajib(Boolean uploadLampiran1Wajib) {
		this.uploadLampiran1Wajib = uploadLampiran1Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-2 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-2 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran2Wajib() {
		return uploadLampiran2Wajib == null ? false : uploadLampiran2Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-2.
	 *
	 * @param uploadLampiran2Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran2Wajib(Boolean uploadLampiran2Wajib) {
		this.uploadLampiran2Wajib = uploadLampiran2Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-3 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-3 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran3Wajib() {
		return uploadLampiran3Wajib == null ? false : uploadLampiran3Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-3.
	 *
	 * @param uploadLampiran3Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran3Wajib(Boolean uploadLampiran3Wajib) {
		this.uploadLampiran3Wajib = uploadLampiran3Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-4 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-4 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran4Wajib() {
		return uploadLampiran4Wajib == null ? false : uploadLampiran4Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-4.
	 *
	 * @param uploadLampiran4Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran4Wajib(Boolean uploadLampiran4Wajib) {
		this.uploadLampiran4Wajib = uploadLampiran4Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-5 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-5 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran5Wajib() {
		return uploadLampiran5Wajib == null ? false : uploadLampiran5Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-5.
	 *
	 * @param uploadLampiran5Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran5Wajib(Boolean uploadLampiran5Wajib) {
		this.uploadLampiran5Wajib = uploadLampiran5Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-6 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-6 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran6Wajib() {
		return uploadLampiran6Wajib == null ? false : uploadLampiran6Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-6.
	 *
	 * @param uploadLampiran6Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran6Wajib(Boolean uploadLampiran6Wajib) {
		this.uploadLampiran6Wajib = uploadLampiran6Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-7 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-7 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran7Wajib() {
		return uploadLampiran7Wajib == null ? false : uploadLampiran7Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-7.
	 *
	 * @param uploadLampiran7Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran7Wajib(Boolean uploadLampiran7Wajib) {
		this.uploadLampiran7Wajib = uploadLampiran7Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-8 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-8 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran8Wajib() {
		return uploadLampiran8Wajib == null ? false : uploadLampiran8Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-8.
	 *
	 * @param uploadLampiran8Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran8Wajib(Boolean uploadLampiran8Wajib) {
		this.uploadLampiran8Wajib = uploadLampiran8Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-9 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-9 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran9Wajib() {
		return uploadLampiran9Wajib == null ? false : uploadLampiran9Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-9.
	 *
	 * @param uploadLampiran9Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran9Wajib(Boolean uploadLampiran9Wajib) {
		this.uploadLampiran9Wajib = uploadLampiran9Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-10 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-10 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran10Wajib() {
		return uploadLampiran10Wajib == null ? false : uploadLampiran10Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-10.
	 *
	 * @param uploadLampiran10Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran10Wajib(Boolean uploadLampiran10Wajib) {
		this.uploadLampiran10Wajib = uploadLampiran10Wajib;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-11. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-11, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran11() {
		return uploadLampiran11 == null ? "" : uploadLampiran11.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-11, apa adanya.
	 *
	 * @param uploadLampiran11 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran11(String uploadLampiran11) {
		this.uploadLampiran11 = uploadLampiran11;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-12. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-12, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran12() {
		return uploadLampiran12 == null ? "" : uploadLampiran12.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-12, apa adanya.
	 *
	 * @param uploadLampiran12 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran12(String uploadLampiran12) {
		this.uploadLampiran12 = uploadLampiran12;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-13. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-13, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran13() {
		return uploadLampiran13 == null ? "" : uploadLampiran13.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-13, apa adanya.
	 *
	 * @param uploadLampiran13 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran13(String uploadLampiran13) {
		this.uploadLampiran13 = uploadLampiran13;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-14. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-14, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran14() {
		return uploadLampiran14 == null ? "" : uploadLampiran14.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-14, apa adanya.
	 *
	 * @param uploadLampiran14 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran14(String uploadLampiran14) {
		this.uploadLampiran14 = uploadLampiran14;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-15. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-15, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran15() {
		return uploadLampiran15 == null ? "" : uploadLampiran15.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-15, apa adanya.
	 *
	 * @param uploadLampiran15 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran15(String uploadLampiran15) {
		this.uploadLampiran15 = uploadLampiran15;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-11 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-11 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran11Wajib() {
		return uploadLampiran11Wajib == null ? false : uploadLampiran11Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-11.
	 *
	 * @param uploadLampiran11Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran11Wajib(Boolean uploadLampiran11Wajib) {
		this.uploadLampiran11Wajib = uploadLampiran11Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-12 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-12 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran12Wajib() {
		return uploadLampiran12Wajib == null ? false : uploadLampiran12Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-12.
	 *
	 * @param uploadLampiran12Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran12Wajib(Boolean uploadLampiran12Wajib) {
		this.uploadLampiran12Wajib = uploadLampiran12Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-13 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-13 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran13Wajib() {
		return uploadLampiran13Wajib == null ? false : uploadLampiran13Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-13.
	 *
	 * @param uploadLampiran13Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran13Wajib(Boolean uploadLampiran13Wajib) {
		this.uploadLampiran13Wajib = uploadLampiran13Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-14 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-14 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran14Wajib() {
		return uploadLampiran14Wajib == null ? false : uploadLampiran14Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-14.
	 *
	 * @param uploadLampiran14Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran14Wajib(Boolean uploadLampiran14Wajib) {
		this.uploadLampiran14Wajib = uploadLampiran14Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-15 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-15 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran15Wajib() {
		return uploadLampiran15Wajib == null ? false : uploadLampiran15Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-15.
	 *
	 * @param uploadLampiran15Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran15Wajib(Boolean uploadLampiran15Wajib) {
		this.uploadLampiran15Wajib = uploadLampiran15Wajib;
	}

	/**
	 * Mengembalikan <b>id</b> {@code TipeItem} (jenis koleksi perpustakaan) yang ditautkan ke slot
	 * lampiran ke-1, atau {@code null} bila slot itu tidak ditautkan ke perpustakaan.
	 *
	 * <p>Nilainya disimpan sebagai {@code Long} mentah, <b>bukan</b> relasi {@code @ManyToOne}:
	 * tidak ada foreign key, tidak ada cascade, dan id yang menunjuk ke baris {@code TipeItem} yang
	 * sudah dihapus baru ketahuan salah ketika dipakai. {@code FormatNilaiSkripsiAction} hanya
	 * membungkusnya kembali menjadi {@code new TipeItem(id)} untuk memilih item combo.</p>
	 *
	 * <p>Pemakai sesungguhnya adalah {@code LibraryUtil}, yang untuk tiap slot memanggil
	 * {@code checkSkripsiForItem(skripsi, tipeItemN, uploadLampiranN, N)} sehingga berkas yang
	 * diunggah mahasiswa otomatis terdaftar sebagai item pustaka bertipe tersebut. Slot tanpa tipe
	 * item tetap boleh diunggah, hanya tidak masuk katalog perpustakaan.</p>
	 *
	 * <p>Berbeda dari mayoritas getter di class ini, getter ini <b>tidak null-safe</b>: {@code null}
	 * dikembalikan apa adanya karena {@code null} memang bermakna "tidak ditautkan".</p>
	 *
	 * <p>Acuan pola bagi {@code getTipeItem2()} sampai {@code getTipeItem20()}.</p>
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-1, atau {@code null}
	 * @see #getUploadLampiran1()
	 */
	public Long getTipeItem1() {
		return tipeItem1;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-1; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem1 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem1(Long tipeItem1) {
		this.tipeItem1 = tipeItem1;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-2; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-2, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem2() {
		return tipeItem2;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-2; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem2 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem2(Long tipeItem2) {
		this.tipeItem2 = tipeItem2;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-3; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-3, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem3() {
		return tipeItem3;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-3; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem3 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem3(Long tipeItem3) {
		this.tipeItem3 = tipeItem3;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-4; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-4, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem4() {
		return tipeItem4;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-4; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem4 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem4(Long tipeItem4) {
		this.tipeItem4 = tipeItem4;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-5; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-5, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem5() {
		return tipeItem5;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-5; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem5 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem5(Long tipeItem5) {
		this.tipeItem5 = tipeItem5;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-6; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-6, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem6() {
		return tipeItem6;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-6; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem6 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem6(Long tipeItem6) {
		this.tipeItem6 = tipeItem6;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-7; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-7, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem7() {
		return tipeItem7;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-7; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem7 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem7(Long tipeItem7) {
		this.tipeItem7 = tipeItem7;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-8; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-8, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem8() {
		return tipeItem8;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-8; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem8 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem8(Long tipeItem8) {
		this.tipeItem8 = tipeItem8;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-9; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-9, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem9() {
		return tipeItem9;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-9; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem9 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem9(Long tipeItem9) {
		this.tipeItem9 = tipeItem9;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-10; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-10, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem10() {
		return tipeItem10;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-10; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem10 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem10(Long tipeItem10) {
		this.tipeItem10 = tipeItem10;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-11; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-11, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem11() {
		return tipeItem11;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-11; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem11 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem11(Long tipeItem11) {
		this.tipeItem11 = tipeItem11;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-12; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-12, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem12() {
		return tipeItem12;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-12; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem12 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem12(Long tipeItem12) {
		this.tipeItem12 = tipeItem12;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-13; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-13, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem13() {
		return tipeItem13;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-13; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem13 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem13(Long tipeItem13) {
		this.tipeItem13 = tipeItem13;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-14; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-14, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem14() {
		return tipeItem14;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-14; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem14 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem14(Long tipeItem14) {
		this.tipeItem14 = tipeItem14;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-15; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-15, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem15() {
		return tipeItem15;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-15; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem15 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem15(Long tipeItem15) {
		this.tipeItem15 = tipeItem15;
	}

	/**
	 * Mengembalikan daftar kode/nama mata kuliah tugas akhir yang harus ada di KRS mahasiswa
	 * dengan hubungan <b>DAN</b> — seluruh token pada daftar ini harus terpenuhi, berbeda dari
	 * {@link #getKodeMatakuliah()} yang cukup salah satu. Pada layar pemilihan format, keduanya
	 * dirangkai menjadi satu kalimat: token {@code kodeMatakuliah} disambung dengan kata "atau",
	 * token {@code kodeMatakuliahDan} dengan kata "dan".
	 *
	 * <p>Normalisasi koma, penulisan balik ke field, dan pengosongan paksa saat
	 * {@link #getTidakWajibMengambilMkTertentu()} bernilai {@code true} <b>persis sama</b> dengan
	 * {@link #getKodeMatakuliah()} — termasuk sifatnya sebagai getter berefek samping yang dapat
	 * memicu {@code UPDATE} dan cabang {@code if (kodeMatakuliahDan == null)} yang tidak pernah
	 * tercapai. Baca Javadoc method tersebut untuk rinciannya.</p>
	 *
	 * @return daftar kode/nama mata kuliah dipisah koma, atau string kosong; tidak pernah
	 *         {@code null}
	 * @see #getKodeMatakuliah()
	 */
	public String getKodeMatakuliahDan() {
		kodeMatakuliahDan = (kodeMatakuliahDan == null || kodeMatakuliahDan.trim().equalsIgnoreCase(",") ? ""
				: "," + kodeMatakuliahDan.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (kodeMatakuliahDan.equals(",")) {
			kodeMatakuliahDan = "";
		} else if (kodeMatakuliahDan.equals(",,")) {
			kodeMatakuliahDan = "";
		} else if (kodeMatakuliahDan.equals(",,,")) {
			kodeMatakuliahDan = "";
		}

		if (kodeMatakuliahDan == null) {
			kodeMatakuliahDan = "";
		}

		if (getTidakWajibMengambilMkTertentu()) {
			kodeMatakuliahDan = "";
		}

		return kodeMatakuliahDan;
	}

	/**
	 * Menyetel daftar kode/nama mata kuliah berelasi DAN, apa adanya. Normalisasi baru terjadi
	 * saat dibaca.
	 *
	 * @param kodeMatakuliahDan daftar kode/nama dipisah koma; {@code null} diterima
	 * @see #getKodeMatakuliahDan()
	 */
	public void setKodeMatakuliahDan(String kodeMatakuliahDan) {
		this.kodeMatakuliahDan = kodeMatakuliahDan;
	}

	/**
	 * Mengembalikan angkatan yang disasar format ini (sudah di-{@code trim}, null-safe ke string
	 * kosong). Kosong berarti format berlaku untuk semua angkatan.
	 *
	 * <p><b>Pencocokannya longgar dan berbasis substring</b>, bukan kesamaan persis: format
	 * dianggap cocok bila teks pada kolom ini <i>mengandung</i> tahun angkatan mahasiswa
	 * ({@code cocokTahunAngkatan}). Karena itu satu format dapat menyasar banyak angkatan sekaligus
	 * dengan menuliskannya berurutan (mis. {@code "2019,2020,2021"}), tetapi konsekuensinya nilai
	 * seperti {@code "2019"} juga akan cocok untuk mahasiswa angkatan {@code "201"} bila data
	 * semacam itu ada. Bila tidak cocok, format tidak dimasukkan ke daftar pilihan sama sekali —
	 * berbeda dari kriteria penargetan lain yang hanya memengaruhi urutan.</p>
	 *
	 * <p>Sebagai komponen skor kecocokan, angkatan bernilai paling rendah (1 poin); lihat
	 * {@link #getJurusan()}.</p>
	 *
	 * @return teks angkatan sasaran, atau string kosong bila berlaku untuk semua angkatan
	 */
	@Column(name = "tahun_angkatan")
	public String getTahunAngkatan() {
		return tahunAngkatan == null ? "" : tahunAngkatan.trim();
	}

	/**
	 * Menyetel angkatan sasaran, apa adanya (tanpa {@code trim} maupun validasi format).
	 *
	 * @param tahunAngkatan teks angkatan; {@code null}/kosong berarti semua angkatan
	 * @see #getTahunAngkatan()
	 */
	public void setTahunAngkatan(String tahunAngkatan) {
		this.tahunAngkatan = tahunAngkatan;
	}

	/**
	 * Menyatakan bahwa biaya pada {@link #getKodeItemBiaya()} bersifat <b>sekali bayar</b> selama
	 * masa studi, bukan per semester. Null-safe ke {@code false}.
	 *
	 * <p>Diteruskan sebagai argumen ke
	 * {@code Mahasiswa.hitungTotalCicilanPembayaran(semester, sekaliBayar, null, kode)}: bila
	 * {@code true}, pembayaran dicari di <b>seluruh</b> semester sehingga mahasiswa yang sudah
	 * membayar biaya sidang pada percobaan sebelumnya tidak diminta membayar lagi; bila
	 * {@code false}, hanya pembayaran pada semester berjalan yang dihitung.</p>
	 *
	 * @return {@code true} bila biaya cukup dibayar sekali seumur masa studi
	 * @see #getKodeItemBiaya()
	 */
	public Boolean getSekaliBayar() {
		return sekaliBayar == null ? false : sekaliBayar;
	}

	/**
	 * Menyetel sifat sekali-bayar untuk biaya sidang.
	 *
	 * @param sekaliBayar {@code true} bila cukup dibayar sekali; {@code null} dibaca sebagai
	 *                    {@code false}
	 * @see #getSekaliBayar()
	 */
	public void setSekaliBayar(Boolean sekaliBayar) {
		this.sekaliBayar = sekaliBayar;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-16. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-16, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran16() {
		return uploadLampiran16 == null ? "" : uploadLampiran16.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-16, apa adanya.
	 *
	 * @param uploadLampiran16 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran16(String uploadLampiran16) {
		this.uploadLampiran16 = uploadLampiran16;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-17. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-17, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran17() {
		return uploadLampiran17 == null ? "" : uploadLampiran17.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-17, apa adanya.
	 *
	 * @param uploadLampiran17 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran17(String uploadLampiran17) {
		this.uploadLampiran17 = uploadLampiran17;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-18. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-18, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran18() {
		return uploadLampiran18 == null ? "" : uploadLampiran18.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-18, apa adanya.
	 *
	 * @param uploadLampiran18 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran18(String uploadLampiran18) {
		this.uploadLampiran18 = uploadLampiran18;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-19. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-19, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran19() {
		return uploadLampiran19 == null ? "" : uploadLampiran19.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-19, apa adanya.
	 *
	 * @param uploadLampiran19 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran19(String uploadLampiran19) {
		this.uploadLampiran19 = uploadLampiran19;
	}

	/**
	 * Mengembalikan judul slot lampiran ke-20. Arti "judul kosong berarti slot tidak dipakai",
	 * pemangkasan spasi, dan sifat null-safe-nya identik dengan {@link #getUploadLampiran1()}.
	 *
	 * @return judul slot lampiran ke-20, atau string kosong bila slot tidak dipakai
	 * @see #getUploadLampiran1()
	 */
	public String getUploadLampiran20() {
		return uploadLampiran20 == null ? "" : uploadLampiran20.trim();
	}

	/**
	 * Menyetel judul slot lampiran ke-20, apa adanya.
	 *
	 * @param uploadLampiran20 judul slot lampiran; {@code null}/kosong menonaktifkan slot
	 * @see #getUploadLampiran1()
	 */
	public void setUploadLampiran20(String uploadLampiran20) {
		this.uploadLampiran20 = uploadLampiran20;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-16 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-16 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran16Wajib() {
		return uploadLampiran16Wajib == null ? false : uploadLampiran16Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-16.
	 *
	 * @param uploadLampiran16Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran16Wajib(Boolean uploadLampiran16Wajib) {
		this.uploadLampiran16Wajib = uploadLampiran16Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-17 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-17 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran17Wajib() {
		return uploadLampiran17Wajib == null ? false : uploadLampiran17Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-17.
	 *
	 * @param uploadLampiran17Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran17Wajib(Boolean uploadLampiran17Wajib) {
		this.uploadLampiran17Wajib = uploadLampiran17Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-18 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-18 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran18Wajib() {
		return uploadLampiran18Wajib == null ? false : uploadLampiran18Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-18.
	 *
	 * @param uploadLampiran18Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran18Wajib(Boolean uploadLampiran18Wajib) {
		this.uploadLampiran18Wajib = uploadLampiran18Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-19 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-19 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran19Wajib() {
		return uploadLampiran19Wajib == null ? false : uploadLampiran19Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-19.
	 *
	 * @param uploadLampiran19Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran19Wajib(Boolean uploadLampiran19Wajib) {
		this.uploadLampiran19Wajib = uploadLampiran19Wajib;
	}

	/**
	 * Menyatakan apakah lampiran slot ke-20 wajib diunggah; perilakunya identik dengan
	 * {@link #getUploadLampiran1Wajib()}.
	 *
	 * @return {@code true} bila lampiran slot ke-20 wajib diunggah
	 * @see #getUploadLampiran1Wajib()
	 */
	public Boolean getUploadLampiran20Wajib() {
		return uploadLampiran20Wajib == null ? false : uploadLampiran20Wajib;
	}

	/**
	 * Menyetel bendera wajib untuk lampiran slot ke-20.
	 *
	 * @param uploadLampiran20Wajib {@code true} bila wajib diunggah; {@code null} dibaca sebagai
	 *                              {@code false}
	 * @see #getUploadLampiran1Wajib()
	 */
	public void setUploadLampiran20Wajib(Boolean uploadLampiran20Wajib) {
		this.uploadLampiran20Wajib = uploadLampiran20Wajib;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-16; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-16, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem16() {
		return tipeItem16;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-16; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem16 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem16(Long tipeItem16) {
		this.tipeItem16 = tipeItem16;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-17; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-17, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem17() {
		return tipeItem17;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-17; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem17 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem17(Long tipeItem17) {
		this.tipeItem17 = tipeItem17;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-18; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-18, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem18() {
		return tipeItem18;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-18; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem18 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem18(Long tipeItem18) {
		this.tipeItem18 = tipeItem18;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-19; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-19, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem19() {
		return tipeItem19;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-19; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem19 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem19(Long tipeItem19) {
		this.tipeItem19 = tipeItem19;
	}

	/**
	 * Mengembalikan id {@code TipeItem} yang ditautkan ke slot lampiran ke-20; perilakunya identik
	 * dengan {@link #getTipeItem1()}, termasuk sifatnya yang tidak null-safe.
	 *
	 * @return id {@code TipeItem} untuk slot lampiran ke-20, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public Long getTipeItem20() {
		return tipeItem20;
	}

	/**
	 * Menyetel id {@code TipeItem} untuk slot lampiran ke-20; {@code null} berarti slot tidak
	 * ditautkan ke koleksi perpustakaan.
	 *
	 * @param tipeItem20 id {@code TipeItem}, atau {@code null}
	 * @see #getTipeItem1()
	 */
	public void setTipeItem20(Long tipeItem20) {
		this.tipeItem20 = tipeItem20;
	}

	/**
	 * Mengembalikan bobot persentase nilai slot dosen {@code dosen21} ({@link #getDosen21()}, label
	 * default "Pembimbing III"), null-safe ke {@code 0.0}. Berpasangan dengan kolom
	 * {@code skripsi.nilai_pembimbing3}; penamaan slot ini sudah lurus, tidak ikut tergeser seperti
	 * slot 1 dan 2.
	 *
	 * @return bobot dalam persen; {@code 0.0} bila belum pernah diisi
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public Double getProsentasiNilaiPembimbing3() {
		return prosentasiNilaiPembimbing3 == null ? 0.0 : prosentasiNilaiPembimbing3;
	}

	/**
	 * Menyetel bobot persentase nilai slot dosen {@code dosen21} (Pembimbing III).
	 *
	 * @param prosentasiNilaiPembimbing3 bobot dalam persen; {@code null} diterima
	 * @see #setProsentasiNilaiKetuaSidang(Double)
	 */
	public void setProsentasiNilaiPembimbing3(Double prosentasiNilaiPembimbing3) {
		this.prosentasiNilaiPembimbing3 = prosentasiNilaiPembimbing3;
	}

	/**
	 * Mengembalikan label peran slot dosen {@code dosen21}, default {@code "Pembimbing III"}.
	 *
	 * <p><b>Penomorannya bukan "slot ke-21".</b> Slot ini ditambahkan belakangan sebagai sisipan
	 * antara {@code dosen2} dan {@code dosen3} agar penomoran kolom lama tidak perlu digeser,
	 * sehingga urutan logisnya adalah {@code dosen1}, {@code dosen2}, <b>{@code dosen21}</b>,
	 * {@code dosen3} .. {@code dosen7} — persis urutan yang dipakai
	 * {@code Skripsi.dataDosen(boolean)}.</p>
	 *
	 * <p><b>Jebakan:</b> {@code PenilaianSkripsiHelper.populateKomponen(String)} tidak punya cabang
	 * untuk label ini, sehingga Pembimbing III jatuh ke nilai awal {@code "dosen1"} dan melihat
	 * daftar komponen penilaian milik slot 1. Dicatat apa adanya, tidak diperbaiki. Method lain
	 * ({@code nilaiDosen}, {@code persenDosen}, {@code sinkronkanRequestTugasAkhir}) sudah
	 * mengenali slot ini dengan benar.</p>
	 *
	 * @return label peran slot {@code dosen21}; tidak pernah {@code null}
	 * @see #getDosen1()
	 */
	public String getDosen21() {
		return dosen21 == null || dosen21.trim().isEmpty() ? "Pembimbing III" : dosen21;
	}

	/**
	 * Menyetel label peran slot dosen {@code dosen21} (Pembimbing III).
	 *
	 * @param dosen21 label peran slot {@code dosen21}
	 * @see #getDosen21()
	 */
	public void setDosen21(String dosen21) {
		this.dosen21 = dosen21;
	}

	/**
	 * Menyatakan bahwa format ini <b>tidak boleh dipilih sendiri oleh mahasiswa</b> dan hanya dapat
	 * ditetapkan oleh petugas dari layar administrasi. Null-safe ke {@code false}, jadi format
	 * lama otomatis boleh dipilih mahasiswa.
	 *
	 * <p>Dipakai untuk format khusus — misalnya sidang ulang, jalur tanpa biaya, atau format
	 * transisi — yang tidak semestinya muncul di menu pengajuan mandiri.</p>
	 *
	 * @return {@code true} bila format hanya untuk penetapan oleh petugas
	 */
	public Boolean getTidakBolehDipilihMahasiswa() {
		return tidakBolehDipilihMahasiswa == null ? false : tidakBolehDipilihMahasiswa;
	}

	/**
	 * Menyetel pembatasan "hanya boleh ditetapkan petugas".
	 *
	 * @param tidakBolehDipilihMahasiswa {@code true} untuk menyembunyikan format dari mahasiswa
	 * @see #getTidakBolehDipilihMahasiswa()
	 */
	public void setTidakBolehDipilihMahasiswa(Boolean tidakBolehDipilihMahasiswa) {
		this.tidakBolehDipilihMahasiswa = tidakBolehDipilihMahasiswa;
	}

	/**
	 * Mengembalikan bobot persentase nilai slot dosen {@code dosen7} ({@link #getDosen7()}, label
	 * default "Penguji V"), null-safe ke {@code 0.0}. Slot terakhir; tidak dimiliki entity sejenis
	 * {@link FormatNilaiProposalSkripsi}.
	 *
	 * @return bobot dalam persen; {@code 0.0} bila belum pernah diisi
	 * @see #getProsentasiNilaiPenguji1()
	 */
	public Double getProsentasiNilaiPenguji5() {
		return prosentasiNilaiPenguji5 == null ? 0.0 : prosentasiNilaiPenguji5;
	}

	/**
	 * Menyetel bobot persentase nilai slot dosen {@code dosen7} (Penguji V).
	 *
	 * @param prosentasiNilaiPenguji5 bobot dalam persen; {@code null} diterima
	 * @see #setProsentasiNilaiKetuaSidang(Double)
	 */
	public void setProsentasiNilaiPenguji5(Double prosentasiNilaiPenguji5) {
		this.prosentasiNilaiPenguji5 = prosentasiNilaiPenguji5;
	}

	/**
	 * Mengembalikan label peran slot dosen {@code dosen7}, default {@code "Penguji V"}.
	 * Berpasangan dengan {@link #getProsentasiNilaiPenguji5()}, {@link #getDosen7Aktif()} dan
	 * {@link #getKode7()}.
	 *
	 * @return label peran slot {@code dosen7}; tidak pernah {@code null}
	 * @see #getDosen1()
	 */
	public String getDosen7() {
		return dosen7 == null || dosen7.trim().isEmpty() ? "Penguji V" : dosen7;
	}

	/**
	 * Menyetel label peran slot dosen {@code dosen7}.
	 *
	 * @param dosen7 label peran slot {@code dosen7}
	 * @see #getDosen1()
	 */
	public void setDosen7(String dosen7) {
		this.dosen7 = dosen7;
	}

	/**
	 * Menyatakan apakah slot dosen pertama dipakai pada format ini — yakni apakah ada baris
	 * "Pembimbing I" pada layar penetapan dosen, layar penilaian, berita acara, dan ekspor Feeder
	 * ({@code Skripsi.dataDosen(boolean)} melewati slot yang tidak aktif).
	 *
	 * <p><b>Nilai {@code null} punya arti tersendiri</b> — berbeda dari bendera {@code Boolean}
	 * lain di class ini yang sekadar null-safe ke {@code false}. Bila bendera belum pernah diisi,
	 * keaktifan <i>disimpulkan dari bobot nilainya</i>: slot dianggap aktif bila
	 * {@link #getProsentasiNilaiKetuaSidang()} melebihi {@code 0.1}. Dengan begitu format lama yang
	 * dibuat sebelum kolom bendera ada tetap berperilaku benar: slot yang diberi bobot berarti
	 * dipakai. Ambang {@code 0.1} (bukan {@code 0}) dipakai untuk menghindari galat pembulatan
	 * bilangan pecahan.</p>
	 *
	 * <p>Begitu bendera diisi eksplisit, bobot tidak lagi berpengaruh: slot dapat dinyatakan aktif
	 * walau berbobot nol (mis. penguji yang hadir tetapi tidak memberi nilai), atau dinonaktifkan
	 * walau bobotnya masih tersimpan.</p>
	 *
	 * <p>Pasangan bobot &harr; bendera inilah bukti paling jelas bahwa {@code prosentasi_nilai_
	 * ketua_sidang} memang milik slot {@code dosen1} yang berlabel default "Pembimbing I" — lihat
	 * pembahasan penamaan tertukar pada Javadoc class dan pada {@link Skripsi}.</p>
	 *
	 * <p>Acuan pola bagi seluruh {@code getDosenNAktif()} lainnya.</p>
	 *
	 * @return {@code true} bila slot {@code dosen1} dipakai
	 * @see #getDosen1()
	 * @see #getProsentasiNilaiKetuaSidang()
	 */
	public Boolean getDosen1Aktif() {
		return dosen1Aktif == null ? getProsentasiNilaiKetuaSidang() > 0.1 : dosen1Aktif;
	}

	/**
	 * Menyetel bendera keaktifan slot {@code dosen1} secara eksplisit. Menyimpan {@code null}
	 * mengembalikan perilaku "simpulkan dari bobot" yang dijelaskan di
	 * {@link #getDosen1Aktif()}.
	 *
	 * @param dosen1Aktif {@code true}/{@code false} eksplisit, atau {@code null} untuk kembali ke
	 *                    penyimpulan dari bobot
	 * @see #getDosen1Aktif()
	 */
	public void setDosen1Aktif(Boolean dosen1Aktif) {
		this.dosen1Aktif = dosen1Aktif;
	}

	/**
	 * Menyatakan apakah slot dosen {@code dosen2} dipakai. Perilakunya identik dengan
	 * {@link #getDosen1Aktif()}, hanya bobot acuannya {@link #getProsentasiNilaiPembimbing()}.
	 *
	 * @return {@code true} bila slot {@code dosen2} dipakai
	 * @see #getDosen1Aktif()
	 */
	public Boolean getDosen2Aktif() {
		return dosen2Aktif == null ? getProsentasiNilaiPembimbing() > 0.1 : dosen2Aktif;
	}

	/**
	 * Menyetel bendera keaktifan slot {@code dosen2}.
	 *
	 * @param dosen2Aktif bendera keaktifan, atau {@code null} untuk penyimpulan dari bobot
	 * @see #getDosen1Aktif()
	 */
	public void setDosen2Aktif(Boolean dosen2Aktif) {
		this.dosen2Aktif = dosen2Aktif;
	}

	/**
	 * Menyatakan apakah slot dosen {@code dosen21} (Pembimbing III) dipakai. Perilakunya identik
	 * dengan {@link #getDosen1Aktif()}, dengan bobot acuan
	 * {@link #getProsentasiNilaiPembimbing3()}.
	 *
	 * @return {@code true} bila slot {@code dosen21} dipakai
	 * @see #getDosen1Aktif()
	 */
	public Boolean getDosen21Aktif() {
		return dosen21Aktif == null ? getProsentasiNilaiPembimbing3() > 0.1 : dosen21Aktif;
	}

	/**
	 * Menyetel bendera keaktifan slot {@code dosen21}.
	 *
	 * @param dosen21Aktif bendera keaktifan, atau {@code null} untuk penyimpulan dari bobot
	 * @see #getDosen1Aktif()
	 */
	public void setDosen21Aktif(Boolean dosen21Aktif) {
		this.dosen21Aktif = dosen21Aktif;
	}

	/**
	 * Menyatakan apakah slot dosen {@code dosen3} (Penguji I) dipakai. Perilakunya identik dengan
	 * {@link #getDosen1Aktif()}, dengan bobot acuan {@link #getProsentasiNilaiPenguji1()}.
	 *
	 * @return {@code true} bila slot {@code dosen3} dipakai
	 * @see #getDosen1Aktif()
	 */
	public Boolean getDosen3Aktif() {
		return dosen3Aktif == null ? getProsentasiNilaiPenguji1() > 0.1 : dosen3Aktif;
	}

	/**
	 * Menyetel bendera keaktifan slot {@code dosen3}.
	 *
	 * @param dosen3Aktif bendera keaktifan, atau {@code null} untuk penyimpulan dari bobot
	 * @see #getDosen1Aktif()
	 */
	public void setDosen3Aktif(Boolean dosen3Aktif) {
		this.dosen3Aktif = dosen3Aktif;
	}

	/**
	 * Menyatakan apakah slot dosen {@code dosen4} (Penguji II) dipakai. Perilakunya identik dengan
	 * {@link #getDosen1Aktif()}, dengan bobot acuan {@link #getProsentasiNilaiPenguji2()}.
	 *
	 * @return {@code true} bila slot {@code dosen4} dipakai
	 * @see #getDosen1Aktif()
	 */
	public Boolean getDosen4Aktif() {
		return dosen4Aktif == null ? getProsentasiNilaiPenguji2() > 0.1 : dosen4Aktif;
	}

	/**
	 * Menyetel bendera keaktifan slot {@code dosen4}.
	 *
	 * @param dosen4Aktif bendera keaktifan, atau {@code null} untuk penyimpulan dari bobot
	 * @see #getDosen1Aktif()
	 */
	public void setDosen4Aktif(Boolean dosen4Aktif) {
		this.dosen4Aktif = dosen4Aktif;
	}

	/**
	 * Menyatakan apakah slot dosen {@code dosen5} (Penguji III) dipakai. Perilakunya identik dengan
	 * {@link #getDosen1Aktif()}, dengan bobot acuan {@link #getProsentasiNilaiPenguji3()}.
	 *
	 * @return {@code true} bila slot {@code dosen5} dipakai
	 * @see #getDosen1Aktif()
	 */
	public Boolean getDosen5Aktif() {
		return dosen5Aktif == null ? getProsentasiNilaiPenguji3() > 0.1 : dosen5Aktif;
	}

	/**
	 * Menyetel bendera keaktifan slot {@code dosen5}.
	 *
	 * @param dosen5Aktif bendera keaktifan, atau {@code null} untuk penyimpulan dari bobot
	 * @see #getDosen1Aktif()
	 */
	public void setDosen5Aktif(Boolean dosen5Aktif) {
		this.dosen5Aktif = dosen5Aktif;
	}

	/**
	 * Menyatakan apakah slot dosen {@code dosen6} (Penguji IV) dipakai. Perilakunya identik dengan
	 * {@link #getDosen1Aktif()}, dengan bobot acuan {@link #getProsentasiNilaiPenguji4()}.
	 *
	 * @return {@code true} bila slot {@code dosen6} dipakai
	 * @see #getDosen1Aktif()
	 */
	public Boolean getDosen6Aktif() {
		return dosen6Aktif == null ? getProsentasiNilaiPenguji4() > 0.1 : dosen6Aktif;
	}

	/**
	 * Menyetel bendera keaktifan slot {@code dosen6}.
	 *
	 * @param dosen6Aktif bendera keaktifan, atau {@code null} untuk penyimpulan dari bobot
	 * @see #getDosen1Aktif()
	 */
	public void setDosen6Aktif(Boolean dosen6Aktif) {
		this.dosen6Aktif = dosen6Aktif;
	}

	/**
	 * Menyatakan apakah slot dosen {@code dosen7} (Penguji V) dipakai. Perilakunya identik dengan
	 * {@link #getDosen1Aktif()}, dengan bobot acuan {@link #getProsentasiNilaiPenguji5()}.
	 *
	 * @return {@code true} bila slot {@code dosen7} dipakai
	 * @see #getDosen1Aktif()
	 */
	public Boolean getDosen7Aktif() {
		return dosen7Aktif == null ? getProsentasiNilaiPenguji5() > 0.1 : dosen7Aktif;
	}

	/**
	 * Menyetel bendera keaktifan slot {@code dosen7}.
	 *
	 * @param dosen7Aktif bendera keaktifan, atau {@code null} untuk penyimpulan dari bobot
	 * @see #getDosen1Aktif()
	 */
	public void setDosen7Aktif(Boolean dosen7Aktif) {
		this.dosen7Aktif = dosen7Aktif;
	}

	/**
	 * Mengembalikan <b>kode peran</b> slot dosen pertama — pendamping teknis bagi label
	 * {@link #getDosen1()}, dipakai saat suatu keluaran memerlukan kode singkat/baku alih-alih
	 * teks bebas (mis. kode jabatan pada berita acara, SK penguji, atau pemetaan ke sistem lain).
	 *
	 * <p>{@code Skripsi.dataDosen(boolean)} mengemas kode ini sebagai argumen keempat
	 * {@code CommonVO} berdampingan dengan label dan object {@code Dosen}-nya. Berbeda dari label,
	 * kode <b>tidak</b> dipakai sebagai kunci pencocokan peran dan <b>tidak</b> punya nilai
	 * default: getter ini mengembalikan isi kolom apa adanya, termasuk {@code null} dan spasi di
	 * ujung. Pemanggil harus siap menerima {@code null}.</p>
	 *
	 * <p>Acuan pola bagi {@code getKode2()}, {@code getKode21()}, dan {@code getKode3()} sampai
	 * {@code getKode7()} — seluruhnya berperilaku sama, hanya slotnya yang berbeda.</p>
	 *
	 * @return kode peran slot {@code dosen1}, boleh {@code null}
	 * @see #getDosen1()
	 */
	public String getKode1() {
		return kode1;
	}

	/**
	 * Menyetel kode peran slot {@code dosen1}, apa adanya. Acuan pola bagi seluruh
	 * {@code setKodeN(String)}.
	 *
	 * @param kode1 kode peran; {@code null} diterima
	 * @see #getKode1()
	 */
	public void setKode1(String kode1) {
		this.kode1 = kode1;
	}

	/**
	 * Mengembalikan kode peran slot {@code dosen2}; perilakunya identik dengan
	 * {@link #getKode1()}.
	 *
	 * @return kode peran slot {@code dosen2}, boleh {@code null}
	 * @see #getKode1()
	 */
	public String getKode2() {
		return kode2;
	}

	/**
	 * Menyetel kode peran slot {@code dosen2}.
	 *
	 * @param kode2 kode peran; {@code null} diterima
	 * @see #getKode1()
	 */
	public void setKode2(String kode2) {
		this.kode2 = kode2;
	}

	/**
	 * Mengembalikan kode peran slot {@code dosen21} (Pembimbing III); perilakunya identik dengan
	 * {@link #getKode1()}.
	 *
	 * @return kode peran slot {@code dosen21}, boleh {@code null}
	 * @see #getKode1()
	 * @see #getDosen21()
	 */
	public String getKode21() {
		return kode21;
	}

	/**
	 * Menyetel kode peran slot {@code dosen21}.
	 *
	 * @param kode21 kode peran; {@code null} diterima
	 * @see #getKode1()
	 */
	public void setKode21(String kode21) {
		this.kode21 = kode21;
	}

	/**
	 * Mengembalikan kode peran slot {@code dosen3} (Penguji I); perilakunya identik dengan
	 * {@link #getKode1()}.
	 *
	 * @return kode peran slot {@code dosen3}, boleh {@code null}
	 * @see #getKode1()
	 */
	public String getKode3() {
		return kode3;
	}

	/**
	 * Menyetel kode peran slot {@code dosen3}.
	 *
	 * @param kode3 kode peran; {@code null} diterima
	 * @see #getKode1()
	 */
	public void setKode3(String kode3) {
		this.kode3 = kode3;
	}

	/**
	 * Mengembalikan kode peran slot {@code dosen4} (Penguji II); perilakunya identik dengan
	 * {@link #getKode1()}.
	 *
	 * @return kode peran slot {@code dosen4}, boleh {@code null}
	 * @see #getKode1()
	 */
	public String getKode4() {
		return kode4;
	}

	/**
	 * Menyetel kode peran slot {@code dosen4}.
	 *
	 * @param kode4 kode peran; {@code null} diterima
	 * @see #getKode1()
	 */
	public void setKode4(String kode4) {
		this.kode4 = kode4;
	}

	/**
	 * Mengembalikan kode peran slot {@code dosen5} (Penguji III); perilakunya identik dengan
	 * {@link #getKode1()}.
	 *
	 * @return kode peran slot {@code dosen5}, boleh {@code null}
	 * @see #getKode1()
	 */
	public String getKode5() {
		return kode5;
	}

	/**
	 * Menyetel kode peran slot {@code dosen5}.
	 *
	 * @param kode5 kode peran; {@code null} diterima
	 * @see #getKode1()
	 */
	public void setKode5(String kode5) {
		this.kode5 = kode5;
	}

	/**
	 * Mengembalikan kode peran slot {@code dosen6} (Penguji IV); perilakunya identik dengan
	 * {@link #getKode1()}.
	 *
	 * @return kode peran slot {@code dosen6}, boleh {@code null}
	 * @see #getKode1()
	 */
	public String getKode6() {
		return kode6;
	}

	/**
	 * Menyetel kode peran slot {@code dosen6}.
	 *
	 * @param kode6 kode peran; {@code null} diterima
	 * @see #getKode1()
	 */
	public void setKode6(String kode6) {
		this.kode6 = kode6;
	}

	/**
	 * Mengembalikan kode peran slot {@code dosen7} (Penguji V); perilakunya identik dengan
	 * {@link #getKode1()}.
	 *
	 * @return kode peran slot {@code dosen7}, boleh {@code null}
	 * @see #getKode1()
	 */
	public String getKode7() {
		return kode7;
	}

	/**
	 * Menyetel kode peran slot {@code dosen7}.
	 *
	 * @param kode7 kode peran; {@code null} diterima
	 * @see #getKode1()
	 */
	public void setKode7(String kode7) {
		this.kode7 = kode7;
	}

	/**
	 * Mengembalikan <b>kode jenis kegiatan mahasiswa versi lama</b> dalam bentuk teks, apa adanya
	 * (tanpa {@code trim}, boleh {@code null}).
	 *
	 * <p>Field ini adalah peninggalan sebelum {@link JenisKegiatanMahasiswa} menjadi entity
	 * tersendiri. Sekarang perannya hanya sebagai sumber migrasi: {@link #getJenisKegiatanMahasiswa()}
	 * membaca nilainya, mencari {@code JenisKegiatanMahasiswa} yang kodenya sama, lalu mengisi
	 * relasi yang sebenarnya. Tidak ada lagi kode aplikasi yang membaca {@code jenis} untuk
	 * keperluan lain, dan combo pada layar penyuntingan pun sudah dialihkan ke relasi (baris
	 * pemilihan berbasis {@code jenis} disisakan sebagai komentar di
	 * {@code FormatNilaiSkripsiAction}).</p>
	 *
	 * @return kode jenis kegiatan versi lama, boleh {@code null}
	 * @see #getJenisKegiatanMahasiswa()
	 */
	public String getJenis() {
		return jenis;
	}

	/**
	 * Menyetel kode jenis kegiatan versi lama, apa adanya. Praktis tidak dipanggil lagi oleh kode
	 * baru; isi {@link #setJenisKegiatanMahasiswa(JenisKegiatanMahasiswa)} sebagai gantinya.
	 *
	 * @param jenis kode jenis kegiatan versi lama; {@code null} diterima
	 * @see #getJenis()
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengembalikan skala nilai huruf khusus format ini; {@code null} berarti memakai skala bawaan
	 * mata kuliah.
	 *
	 * <p>Dipakai {@code Skripsi.getNilaiHuruf()}: skala diambil dari
	 * {@code Matakuliah.getJenisNilaiHuruf()}, lalu <b>ditimpa</b> oleh nilai getter ini bila tidak
	 * {@code null}. Gunanya agar sidang tugas akhir dapat memakai tabel konversi angka&rarr;huruf
	 * yang berbeda dari mata kuliah biasa (mis. hanya A/B/TIDAK LULUS) tanpa mengubah master mata
	 * kuliah.</p>
	 *
	 * <p>Perhatikan bahwa penggantian ini <b>tidak berlaku surut</b>: nilai huruf yang sudah
	 * tersimpan pada baris skripsi lama tidak dihitung ulang saat skala diubah.</p>
	 *
	 * <p>Berefek samping lewat {@link GeneralValueObject#check(Object)}, sama seperti
	 * {@link #getJurusan()}.</p>
	 *
	 * @return skala nilai huruf khusus, atau {@code null} bila mengikuti mata kuliah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_nilai_huruf", nullable = true)
	public JenisNilaiHurufMatakuliah getJenisNilaiHuruf() {
		jenisNilaiHuruf = check(jenisNilaiHuruf);
		return jenisNilaiHuruf;
	}

	/**
	 * Menyetel skala nilai huruf khusus format ini.
	 *
	 * @param jenisNilaiHuruf skala nilai huruf, atau {@code null} untuk mengikuti skala mata
	 *                        kuliah
	 * @see #getJenisNilaiHuruf()
	 */
	public void setJenisNilaiHuruf(JenisNilaiHurufMatakuliah jenisNilaiHuruf) {
		this.jenisNilaiHuruf = jenisNilaiHuruf;
	}

	/**
	 * Menyatakan apakah mahasiswa boleh mengubah sendiri agenda/jadwal bimbingan tugas akhirnya,
	 * atau hanya dosen/petugas yang boleh.
	 *
	 * <p><b>Default-nya {@code true}</b> (bukan {@code false} seperti kebanyakan bendera lain di
	 * class ini), sehingga format lama yang kolomnya masih kosong tetap mengizinkan mahasiswa
	 * mengatur jadwal bimbingannya. {@code PenjadwalanSkripsiHelper} membaca bendera ini lewat
	 * format nilai milik skripsi bersangkutan; skripsi yang belum punya format dianggap
	 * mengizinkan.</p>
	 *
	 * @return {@code true} bila mahasiswa boleh mengubah agenda/jadwal bimbingan
	 */
	public Boolean getMahasiswaBolehMengubahAgendaAtauJadwalBimbingan() {
		return mahasiswaBolehMengubahAgendaAtauJadwalBimbingan == null ? true
				: mahasiswaBolehMengubahAgendaAtauJadwalBimbingan;
	}

	/**
	 * Menyetel izin mahasiswa mengubah agenda/jadwal bimbingan. Menyimpan {@code null} berarti
	 * kembali ke default {@code true} — untuk melarang, kolomnya harus diisi {@code false}
	 * eksplisit.
	 *
	 * @param mahasiswaBolehMengubahAgendaAtauJadwalBimbingan izin mengubah jadwal; {@code null}
	 *                                                       dibaca sebagai {@code true}
	 * @see #getMahasiswaBolehMengubahAgendaAtauJadwalBimbingan()
	 */
	public void setMahasiswaBolehMengubahAgendaAtauJadwalBimbingan(
			Boolean mahasiswaBolehMengubahAgendaAtauJadwalBimbingan) {
		this.mahasiswaBolehMengubahAgendaAtauJadwalBimbingan = mahasiswaBolehMengubahAgendaAtauJadwalBimbingan;
	}

	/**
	 * Mengembalikan jenis kegiatan mahasiswa (kategori aktivitas tugas akhir) yang mewakili format
	 * ini pada pelaporan PDDikti/Feeder — {@code EksporAktifitasSkripsiFeeder},
	 * {@code EksporPesertaDosenSkripsiFeeder}, {@code EksporPesertaMahasiswaSkripsiFeeder} dan
	 * {@code FeederExporter} semuanya mengambil {@code getKode()} dari relasi ini sebagai jenis
	 * aktivitas yang dikirim.
	 *
	 * <p><b>Getter ini bekerja jauh lebih banyak daripada namanya menyiratkan</b> dan
	 * <b>berefek samping</b>:</p>
	 * <ol>
	 * <li>memanggil {@link GeneralValueObject#check(Object)} yang, bila entity detached, dapat
	 * membuka session Hibernate sendiri untuk memuat ulang proxy;</li>
	 * <li>bila relasinya masih {@code null} sedangkan {@link #getJenis()} terisi, ia melakukan
	 * <b>migrasi data on-the-fly</b>: seluruh {@code JenisKegiatanMahasiswa} dibaca dari cache
	 * {@code ConstantValues.ambilBerdasarClass(...)}, dicari yang {@code getKode()}-nya sama
	 * dengan {@code jenis} (tanpa peduli besar-kecil huruf), lalu hasilnya <b>disimpan ke field
	 * relasi</b>. Karena field berubah, entity dapat dianggap kotor oleh Hibernate dan
	 * ter-{@code UPDATE} saat flush walau pemanggil hanya "membaca".</li>
	 * </ol>
	 *
	 * <p>Seluruh proses dibungkus dua lapis {@code try/catch} yang hanya mencatat error (penanda
	 * {@code auto-audit} berasal dari inisiatif audit blok {@code catch} kosong, bukan dari
	 * pekerjaan Javadoc ini), sehingga kegagalan pencarian tidak pernah membatalkan pembacaan
	 * property — paling jauh mengembalikan {@code null}. Perhatikan pula bahwa kedua penanda
	 * {@code auto-audit} menyebut nomor baris versi lama file ini dan sudah tidak akurat.</p>
	 *
	 * @return jenis kegiatan mahasiswa untuk pelaporan, atau {@code null} bila belum ditetapkan dan
	 *         tidak ada padanan untuk {@link #getJenis()}
	 * @see #getJenis()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kegiatan_mahasiswa", nullable = true)
	public JenisKegiatanMahasiswa getJenisKegiatanMahasiswa() {
		jenisKegiatanMahasiswa = check(jenisKegiatanMahasiswa);
		try {
			if (jenisKegiatanMahasiswa == null && getJenis() != null && !getJenis().trim().isEmpty()) {
				for (Object o : ConstantValues.ambilBerdasarClass(JenisKegiatanMahasiswa.class).values()) {
					try {
						JenisKegiatanMahasiswa oo = (JenisKegiatanMahasiswa) o;
						if (oo.getKode().equalsIgnoreCase(getJenis().trim())) {
							jenisKegiatanMahasiswa = oo;
							break;
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/FormatNilaiSkripsi.java:1214");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/FormatNilaiSkripsi.java:1219");
		}
		return jenisKegiatanMahasiswa;
	}

	/**
	 * Menyetel jenis kegiatan mahasiswa untuk pelaporan Feeder. Tidak menyentuh field warisan
	 * {@link #getJenis()}, sehingga keduanya dapat berbeda; yang dipakai pelaporan adalah relasi
	 * ini.
	 *
	 * @param jenisKegiatanMahasiswa jenis kegiatan, atau {@code null}
	 * @see #getJenisKegiatanMahasiswa()
	 */
	public void setJenisKegiatanMahasiswa(JenisKegiatanMahasiswa jenisKegiatanMahasiswa) {
		this.jenisKegiatanMahasiswa = jenisKegiatanMahasiswa;
	}
}
