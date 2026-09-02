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

	public void setKodeMatakuliah(String kodeMatakuliah) {
		this.kodeMatakuliah = kodeMatakuliah;
	}

	public Integer getMinimalSks() {
		return minimalSks == null ? 0 : minimalSks;
	}

	public void setMinimalSks(Integer minimalSks) {
		this.minimalSks = minimalSks;
	}

	public Double getMinimalIpk() {
		return minimalIpk == null ? 0.0 : minimalIpk;
	}

	public void setMinimalIpk(Double minimalIpk) {
		this.minimalIpk = minimalIpk;
	}

	public Double getMinimalAngkaKredit() {
		return minimalAngkaKredit == null ? 0.0 : minimalAngkaKredit;
	}

	public void setMinimalAngkaKredit(Double minimalAngkaKredit) {
		this.minimalAngkaKredit = minimalAngkaKredit;
	}

	@javax.persistence.Column(columnDefinition = "text")
	public String getMatkulPrasyaratLulus() {
		return matkulPrasyaratLulus;
	}

	public void setMatkulPrasyaratLulus(String matkulPrasyaratLulus) {
		this.matkulPrasyaratLulus = matkulPrasyaratLulus;
	}

	public Boolean getHarusLunas() {
		return harusLunas == null ? false : harusLunas;
	}

	public void setHarusLunas(Boolean harusLunas) {
		this.harusLunas = harusLunas;
	}

	public Double getProsentaseLunas() {
		return prosentaseLunas == null ? 0.0 : prosentaseLunas;
	}

	public void setProsentaseLunas(Double prosentaseLunas) {
		this.prosentaseLunas = prosentaseLunas;
	}

	public Boolean getHarusMengembalikanBukuPerpustakaan() {
		return harusMengembalikanBukuPerpustakaan == null ? false : harusMengembalikanBukuPerpustakaan;
	}

	public void setHarusMengembalikanBukuPerpustakaan(Boolean harusMengembalikanBukuPerpustakaan) {
		this.harusMengembalikanBukuPerpustakaan = harusMengembalikanBukuPerpustakaan;
	}

	public String getKodeItemBiaya() {
		return kodeItemBiaya == null ? "" : kodeItemBiaya.trim();
	}

	public void setKodeItemBiaya(String kodeItemBiaya) {
		this.kodeItemBiaya = kodeItemBiaya;
	}

	public Double getBobot() {
		return bobot == null ? 100.0 : bobot;
	}

	public void setBobot(Double bobot) {
		this.bobot = bobot;
	}

	public Boolean getTidakWajibMengambilMkTertentu() {
		return tidakWajibMengambilMkTertentu == null ? false : tidakWajibMengambilMkTertentu;
	}

	public void setTidakWajibMengambilMkTertentu(Boolean tidakWajibMengambilMkTertentu) {
		this.tidakWajibMengambilMkTertentu = tidakWajibMengambilMkTertentu;
	}

	public String getUploadLampiran1() {
		return uploadLampiran1 == null ? "" : uploadLampiran1.trim();
	}

	public void setUploadLampiran1(String uploadLampiran1) {
		this.uploadLampiran1 = uploadLampiran1;
	}

	public String getUploadLampiran2() {
		return uploadLampiran2 == null ? "" : uploadLampiran2.trim();
	}

	public void setUploadLampiran2(String uploadLampiran2) {
		this.uploadLampiran2 = uploadLampiran2;
	}

	public String getUploadLampiran3() {
		return uploadLampiran3 == null ? "" : uploadLampiran3.trim();
	}

	public void setUploadLampiran3(String uploadLampiran3) {
		this.uploadLampiran3 = uploadLampiran3;
	}

	public String getUploadLampiran4() {
		return uploadLampiran4 == null ? "" : uploadLampiran4.trim();
	}

	public void setUploadLampiran4(String uploadLampiran4) {
		this.uploadLampiran4 = uploadLampiran4;
	}

	public String getUploadLampiran5() {
		return uploadLampiran5 == null ? "" : uploadLampiran5.trim();
	}

	public void setUploadLampiran5(String uploadLampiran5) {
		this.uploadLampiran5 = uploadLampiran5;
	}

	public String getUploadLampiran6() {
		return uploadLampiran6 == null ? "" : uploadLampiran6.trim();
	}

	public void setUploadLampiran6(String uploadLampiran6) {
		this.uploadLampiran6 = uploadLampiran6;
	}

	public String getUploadLampiran7() {
		return uploadLampiran7 == null ? "" : uploadLampiran7.trim();
	}

	public void setUploadLampiran7(String uploadLampiran7) {
		this.uploadLampiran7 = uploadLampiran7;
	}

	public String getUploadLampiran8() {
		return uploadLampiran8 == null ? "" : uploadLampiran8.trim();
	}

	public void setUploadLampiran8(String uploadLampiran8) {
		this.uploadLampiran8 = uploadLampiran8;
	}

	public String getUploadLampiran9() {
		return uploadLampiran9 == null ? "" : uploadLampiran9.trim();
	}

	public void setUploadLampiran9(String uploadLampiran9) {
		this.uploadLampiran9 = uploadLampiran9;
	}

	public String getUploadLampiran10() {
		return uploadLampiran10 == null ? "" : uploadLampiran10.trim();
	}

	public void setUploadLampiran10(String uploadLampiran10) {
		this.uploadLampiran10 = uploadLampiran10;
	}

	public Boolean getUploadLampiran1Wajib() {
		return uploadLampiran1Wajib == null ? false : uploadLampiran1Wajib;
	}

	public void setUploadLampiran1Wajib(Boolean uploadLampiran1Wajib) {
		this.uploadLampiran1Wajib = uploadLampiran1Wajib;
	}

	public Boolean getUploadLampiran2Wajib() {
		return uploadLampiran2Wajib == null ? false : uploadLampiran2Wajib;
	}

	public void setUploadLampiran2Wajib(Boolean uploadLampiran2Wajib) {
		this.uploadLampiran2Wajib = uploadLampiran2Wajib;
	}

	public Boolean getUploadLampiran3Wajib() {
		return uploadLampiran3Wajib == null ? false : uploadLampiran3Wajib;
	}

	public void setUploadLampiran3Wajib(Boolean uploadLampiran3Wajib) {
		this.uploadLampiran3Wajib = uploadLampiran3Wajib;
	}

	public Boolean getUploadLampiran4Wajib() {
		return uploadLampiran4Wajib == null ? false : uploadLampiran4Wajib;
	}

	public void setUploadLampiran4Wajib(Boolean uploadLampiran4Wajib) {
		this.uploadLampiran4Wajib = uploadLampiran4Wajib;
	}

	public Boolean getUploadLampiran5Wajib() {
		return uploadLampiran5Wajib == null ? false : uploadLampiran5Wajib;
	}

	public void setUploadLampiran5Wajib(Boolean uploadLampiran5Wajib) {
		this.uploadLampiran5Wajib = uploadLampiran5Wajib;
	}

	public Boolean getUploadLampiran6Wajib() {
		return uploadLampiran6Wajib == null ? false : uploadLampiran6Wajib;
	}

	public void setUploadLampiran6Wajib(Boolean uploadLampiran6Wajib) {
		this.uploadLampiran6Wajib = uploadLampiran6Wajib;
	}

	public Boolean getUploadLampiran7Wajib() {
		return uploadLampiran7Wajib == null ? false : uploadLampiran7Wajib;
	}

	public void setUploadLampiran7Wajib(Boolean uploadLampiran7Wajib) {
		this.uploadLampiran7Wajib = uploadLampiran7Wajib;
	}

	public Boolean getUploadLampiran8Wajib() {
		return uploadLampiran8Wajib == null ? false : uploadLampiran8Wajib;
	}

	public void setUploadLampiran8Wajib(Boolean uploadLampiran8Wajib) {
		this.uploadLampiran8Wajib = uploadLampiran8Wajib;
	}

	public Boolean getUploadLampiran9Wajib() {
		return uploadLampiran9Wajib == null ? false : uploadLampiran9Wajib;
	}

	public void setUploadLampiran9Wajib(Boolean uploadLampiran9Wajib) {
		this.uploadLampiran9Wajib = uploadLampiran9Wajib;
	}

	public Boolean getUploadLampiran10Wajib() {
		return uploadLampiran10Wajib == null ? false : uploadLampiran10Wajib;
	}

	public void setUploadLampiran10Wajib(Boolean uploadLampiran10Wajib) {
		this.uploadLampiran10Wajib = uploadLampiran10Wajib;
	}

	public String getUploadLampiran11() {
		return uploadLampiran11 == null ? "" : uploadLampiran11.trim();
	}

	public void setUploadLampiran11(String uploadLampiran11) {
		this.uploadLampiran11 = uploadLampiran11;
	}

	public String getUploadLampiran12() {
		return uploadLampiran12 == null ? "" : uploadLampiran12.trim();
	}

	public void setUploadLampiran12(String uploadLampiran12) {
		this.uploadLampiran12 = uploadLampiran12;
	}

	public String getUploadLampiran13() {
		return uploadLampiran13 == null ? "" : uploadLampiran13.trim();
	}

	public void setUploadLampiran13(String uploadLampiran13) {
		this.uploadLampiran13 = uploadLampiran13;
	}

	public String getUploadLampiran14() {
		return uploadLampiran14 == null ? "" : uploadLampiran14.trim();
	}

	public void setUploadLampiran14(String uploadLampiran14) {
		this.uploadLampiran14 = uploadLampiran14;
	}

	public String getUploadLampiran15() {
		return uploadLampiran15 == null ? "" : uploadLampiran15.trim();
	}

	public void setUploadLampiran15(String uploadLampiran15) {
		this.uploadLampiran15 = uploadLampiran15;
	}

	public Boolean getUploadLampiran11Wajib() {
		return uploadLampiran11Wajib == null ? false : uploadLampiran11Wajib;
	}

	public void setUploadLampiran11Wajib(Boolean uploadLampiran11Wajib) {
		this.uploadLampiran11Wajib = uploadLampiran11Wajib;
	}

	public Boolean getUploadLampiran12Wajib() {
		return uploadLampiran12Wajib == null ? false : uploadLampiran12Wajib;
	}

	public void setUploadLampiran12Wajib(Boolean uploadLampiran12Wajib) {
		this.uploadLampiran12Wajib = uploadLampiran12Wajib;
	}

	public Boolean getUploadLampiran13Wajib() {
		return uploadLampiran13Wajib == null ? false : uploadLampiran13Wajib;
	}

	public void setUploadLampiran13Wajib(Boolean uploadLampiran13Wajib) {
		this.uploadLampiran13Wajib = uploadLampiran13Wajib;
	}

	public Boolean getUploadLampiran14Wajib() {
		return uploadLampiran14Wajib == null ? false : uploadLampiran14Wajib;
	}

	public void setUploadLampiran14Wajib(Boolean uploadLampiran14Wajib) {
		this.uploadLampiran14Wajib = uploadLampiran14Wajib;
	}

	public Boolean getUploadLampiran15Wajib() {
		return uploadLampiran15Wajib == null ? false : uploadLampiran15Wajib;
	}

	public void setUploadLampiran15Wajib(Boolean uploadLampiran15Wajib) {
		this.uploadLampiran15Wajib = uploadLampiran15Wajib;
	}

	public Long getTipeItem1() {
		return tipeItem1;
	}

	public void setTipeItem1(Long tipeItem1) {
		this.tipeItem1 = tipeItem1;
	}

	public Long getTipeItem2() {
		return tipeItem2;
	}

	public void setTipeItem2(Long tipeItem2) {
		this.tipeItem2 = tipeItem2;
	}

	public Long getTipeItem3() {
		return tipeItem3;
	}

	public void setTipeItem3(Long tipeItem3) {
		this.tipeItem3 = tipeItem3;
	}

	public Long getTipeItem4() {
		return tipeItem4;
	}

	public void setTipeItem4(Long tipeItem4) {
		this.tipeItem4 = tipeItem4;
	}

	public Long getTipeItem5() {
		return tipeItem5;
	}

	public void setTipeItem5(Long tipeItem5) {
		this.tipeItem5 = tipeItem5;
	}

	public Long getTipeItem6() {
		return tipeItem6;
	}

	public void setTipeItem6(Long tipeItem6) {
		this.tipeItem6 = tipeItem6;
	}

	public Long getTipeItem7() {
		return tipeItem7;
	}

	public void setTipeItem7(Long tipeItem7) {
		this.tipeItem7 = tipeItem7;
	}

	public Long getTipeItem8() {
		return tipeItem8;
	}

	public void setTipeItem8(Long tipeItem8) {
		this.tipeItem8 = tipeItem8;
	}

	public Long getTipeItem9() {
		return tipeItem9;
	}

	public void setTipeItem9(Long tipeItem9) {
		this.tipeItem9 = tipeItem9;
	}

	public Long getTipeItem10() {
		return tipeItem10;
	}

	public void setTipeItem10(Long tipeItem10) {
		this.tipeItem10 = tipeItem10;
	}

	public Long getTipeItem11() {
		return tipeItem11;
	}

	public void setTipeItem11(Long tipeItem11) {
		this.tipeItem11 = tipeItem11;
	}

	public Long getTipeItem12() {
		return tipeItem12;
	}

	public void setTipeItem12(Long tipeItem12) {
		this.tipeItem12 = tipeItem12;
	}

	public Long getTipeItem13() {
		return tipeItem13;
	}

	public void setTipeItem13(Long tipeItem13) {
		this.tipeItem13 = tipeItem13;
	}

	public Long getTipeItem14() {
		return tipeItem14;
	}

	public void setTipeItem14(Long tipeItem14) {
		this.tipeItem14 = tipeItem14;
	}

	public Long getTipeItem15() {
		return tipeItem15;
	}

	public void setTipeItem15(Long tipeItem15) {
		this.tipeItem15 = tipeItem15;
	}

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

	public void setKodeMatakuliahDan(String kodeMatakuliahDan) {
		this.kodeMatakuliahDan = kodeMatakuliahDan;
	}

	@Column(name = "tahun_angkatan")
	public String getTahunAngkatan() {
		return tahunAngkatan == null ? "" : tahunAngkatan.trim();
	}

	public void setTahunAngkatan(String tahunAngkatan) {
		this.tahunAngkatan = tahunAngkatan;
	}

	public Boolean getSekaliBayar() {
		return sekaliBayar == null ? false : sekaliBayar;
	}

	public void setSekaliBayar(Boolean sekaliBayar) {
		this.sekaliBayar = sekaliBayar;
	}

	public String getUploadLampiran16() {
		return uploadLampiran16 == null ? "" : uploadLampiran16.trim();
	}

	public void setUploadLampiran16(String uploadLampiran16) {
		this.uploadLampiran16 = uploadLampiran16;
	}

	public String getUploadLampiran17() {
		return uploadLampiran17 == null ? "" : uploadLampiran17.trim();
	}

	public void setUploadLampiran17(String uploadLampiran17) {
		this.uploadLampiran17 = uploadLampiran17;
	}

	public String getUploadLampiran18() {
		return uploadLampiran18 == null ? "" : uploadLampiran18.trim();
	}

	public void setUploadLampiran18(String uploadLampiran18) {
		this.uploadLampiran18 = uploadLampiran18;
	}

	public String getUploadLampiran19() {
		return uploadLampiran19 == null ? "" : uploadLampiran19.trim();
	}

	public void setUploadLampiran19(String uploadLampiran19) {
		this.uploadLampiran19 = uploadLampiran19;
	}

	public String getUploadLampiran20() {
		return uploadLampiran20 == null ? "" : uploadLampiran20.trim();
	}

	public void setUploadLampiran20(String uploadLampiran20) {
		this.uploadLampiran20 = uploadLampiran20;
	}

	public Boolean getUploadLampiran16Wajib() {
		return uploadLampiran16Wajib == null ? false : uploadLampiran16Wajib;
	}

	public void setUploadLampiran16Wajib(Boolean uploadLampiran16Wajib) {
		this.uploadLampiran16Wajib = uploadLampiran16Wajib;
	}

	public Boolean getUploadLampiran17Wajib() {
		return uploadLampiran17Wajib == null ? false : uploadLampiran17Wajib;
	}

	public void setUploadLampiran17Wajib(Boolean uploadLampiran17Wajib) {
		this.uploadLampiran17Wajib = uploadLampiran17Wajib;
	}

	public Boolean getUploadLampiran18Wajib() {
		return uploadLampiran18Wajib == null ? false : uploadLampiran18Wajib;
	}

	public void setUploadLampiran18Wajib(Boolean uploadLampiran18Wajib) {
		this.uploadLampiran18Wajib = uploadLampiran18Wajib;
	}

	public Boolean getUploadLampiran19Wajib() {
		return uploadLampiran19Wajib == null ? false : uploadLampiran19Wajib;
	}

	public void setUploadLampiran19Wajib(Boolean uploadLampiran19Wajib) {
		this.uploadLampiran19Wajib = uploadLampiran19Wajib;
	}

	public Boolean getUploadLampiran20Wajib() {
		return uploadLampiran20Wajib == null ? false : uploadLampiran20Wajib;
	}

	public void setUploadLampiran20Wajib(Boolean uploadLampiran20Wajib) {
		this.uploadLampiran20Wajib = uploadLampiran20Wajib;
	}

	public Long getTipeItem16() {
		return tipeItem16;
	}

	public void setTipeItem16(Long tipeItem16) {
		this.tipeItem16 = tipeItem16;
	}

	public Long getTipeItem17() {
		return tipeItem17;
	}

	public void setTipeItem17(Long tipeItem17) {
		this.tipeItem17 = tipeItem17;
	}

	public Long getTipeItem18() {
		return tipeItem18;
	}

	public void setTipeItem18(Long tipeItem18) {
		this.tipeItem18 = tipeItem18;
	}

	public Long getTipeItem19() {
		return tipeItem19;
	}

	public void setTipeItem19(Long tipeItem19) {
		this.tipeItem19 = tipeItem19;
	}

	public Long getTipeItem20() {
		return tipeItem20;
	}

	public void setTipeItem20(Long tipeItem20) {
		this.tipeItem20 = tipeItem20;
	}

	public Double getProsentasiNilaiPembimbing3() {
		return prosentasiNilaiPembimbing3 == null ? 0.0 : prosentasiNilaiPembimbing3;
	}

	public void setProsentasiNilaiPembimbing3(Double prosentasiNilaiPembimbing3) {
		this.prosentasiNilaiPembimbing3 = prosentasiNilaiPembimbing3;
	}

	public String getDosen21() {
		return dosen21 == null || dosen21.trim().isEmpty() ? "Pembimbing III" : dosen21;
	}

	public void setDosen21(String dosen21) {
		this.dosen21 = dosen21;
	}

	public Boolean getTidakBolehDipilihMahasiswa() {
		return tidakBolehDipilihMahasiswa == null ? false : tidakBolehDipilihMahasiswa;
	}

	public void setTidakBolehDipilihMahasiswa(Boolean tidakBolehDipilihMahasiswa) {
		this.tidakBolehDipilihMahasiswa = tidakBolehDipilihMahasiswa;
	}

	public Double getProsentasiNilaiPenguji5() {
		return prosentasiNilaiPenguji5 == null ? 0.0 : prosentasiNilaiPenguji5;
	}

	public void setProsentasiNilaiPenguji5(Double prosentasiNilaiPenguji5) {
		this.prosentasiNilaiPenguji5 = prosentasiNilaiPenguji5;
	}

	public String getDosen7() {
		return dosen7 == null || dosen7.trim().isEmpty() ? "Penguji V" : dosen7;
	}

	public void setDosen7(String dosen7) {
		this.dosen7 = dosen7;
	}

	public Boolean getDosen1Aktif() {
		return dosen1Aktif == null ? getProsentasiNilaiKetuaSidang() > 0.1 : dosen1Aktif;
	}

	public void setDosen1Aktif(Boolean dosen1Aktif) {
		this.dosen1Aktif = dosen1Aktif;
	}

	public Boolean getDosen2Aktif() {
		return dosen2Aktif == null ? getProsentasiNilaiPembimbing() > 0.1 : dosen2Aktif;
	}

	public void setDosen2Aktif(Boolean dosen2Aktif) {
		this.dosen2Aktif = dosen2Aktif;
	}

	public Boolean getDosen21Aktif() {
		return dosen21Aktif == null ? getProsentasiNilaiPembimbing3() > 0.1 : dosen21Aktif;
	}

	public void setDosen21Aktif(Boolean dosen21Aktif) {
		this.dosen21Aktif = dosen21Aktif;
	}

	public Boolean getDosen3Aktif() {
		return dosen3Aktif == null ? getProsentasiNilaiPenguji1() > 0.1 : dosen3Aktif;
	}

	public void setDosen3Aktif(Boolean dosen3Aktif) {
		this.dosen3Aktif = dosen3Aktif;
	}

	public Boolean getDosen4Aktif() {
		return dosen4Aktif == null ? getProsentasiNilaiPenguji2() > 0.1 : dosen4Aktif;
	}

	public void setDosen4Aktif(Boolean dosen4Aktif) {
		this.dosen4Aktif = dosen4Aktif;
	}

	public Boolean getDosen5Aktif() {
		return dosen5Aktif == null ? getProsentasiNilaiPenguji3() > 0.1 : dosen5Aktif;
	}

	public void setDosen5Aktif(Boolean dosen5Aktif) {
		this.dosen5Aktif = dosen5Aktif;
	}

	public Boolean getDosen6Aktif() {
		return dosen6Aktif == null ? getProsentasiNilaiPenguji4() > 0.1 : dosen6Aktif;
	}

	public void setDosen6Aktif(Boolean dosen6Aktif) {
		this.dosen6Aktif = dosen6Aktif;
	}

	public Boolean getDosen7Aktif() {
		return dosen7Aktif == null ? getProsentasiNilaiPenguji5() > 0.1 : dosen7Aktif;
	}

	public void setDosen7Aktif(Boolean dosen7Aktif) {
		this.dosen7Aktif = dosen7Aktif;
	}

	public String getKode1() {
		return kode1;
	}

	public void setKode1(String kode1) {
		this.kode1 = kode1;
	}

	public String getKode2() {
		return kode2;
	}

	public void setKode2(String kode2) {
		this.kode2 = kode2;
	}

	public String getKode21() {
		return kode21;
	}

	public void setKode21(String kode21) {
		this.kode21 = kode21;
	}

	public String getKode3() {
		return kode3;
	}

	public void setKode3(String kode3) {
		this.kode3 = kode3;
	}

	public String getKode4() {
		return kode4;
	}

	public void setKode4(String kode4) {
		this.kode4 = kode4;
	}

	public String getKode5() {
		return kode5;
	}

	public void setKode5(String kode5) {
		this.kode5 = kode5;
	}

	public String getKode6() {
		return kode6;
	}

	public void setKode6(String kode6) {
		this.kode6 = kode6;
	}

	public String getKode7() {
		return kode7;
	}

	public void setKode7(String kode7) {
		this.kode7 = kode7;
	}

	public String getJenis() {
		return jenis;
	}

	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_nilai_huruf", nullable = true)
	public JenisNilaiHurufMatakuliah getJenisNilaiHuruf() {
		jenisNilaiHuruf = check(jenisNilaiHuruf);
		return jenisNilaiHuruf;
	}

	public void setJenisNilaiHuruf(JenisNilaiHurufMatakuliah jenisNilaiHuruf) {
		this.jenisNilaiHuruf = jenisNilaiHuruf;
	}

	public Boolean getMahasiswaBolehMengubahAgendaAtauJadwalBimbingan() {
		return mahasiswaBolehMengubahAgendaAtauJadwalBimbingan == null ? true
				: mahasiswaBolehMengubahAgendaAtauJadwalBimbingan;
	}

	public void setMahasiswaBolehMengubahAgendaAtauJadwalBimbingan(
			Boolean mahasiswaBolehMengubahAgendaAtauJadwalBimbingan) {
		this.mahasiswaBolehMengubahAgendaAtauJadwalBimbingan = mahasiswaBolehMengubahAgendaAtauJadwalBimbingan;
	}

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

	public void setJenisKegiatanMahasiswa(JenisKegiatanMahasiswa jenisKegiatanMahasiswa) {
		this.jenisKegiatanMahasiswa = jenisKegiatanMahasiswa;
	}
}
