package ais.database.model.sekolah;

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

import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate untuk tabel {@code sekolah.grup_checklist_penilaian_guru}, merepresentasikan
 * satu <b>kelompok/aspek penilaian</b> pada checklist penilaian guru jenjang sekolah
 * (SD/SMP/SMA) — mis. "Kedisiplinan", "Penguasaan Materi". Baris pada tabel ini tidak menyimpan
 * pertanyaan maupun nilai apa pun: ia hanya <b>judul bagian</b> yang menaungi sekumpulan butir
 * pertanyaan {@link ChecklistPenilaianGuru}, sekaligus menjadi <b>jangkar</b> tempat parameter
 * tambahan ({@link ais.database.model.ParameterTambahanAngketUmum}) dan penjadwalan pengisian
 * ({@link ais.database.model.JadwalChecklistPenilaianUmum}) digantungkan.
 *
 * <h2>Domain TERVERIFIKASI: angket guru DIISI OLEH SISWA, bukan supervisi kepala sekolah</h2>
 * <p>Perlu ditegaskan di awal karena nama tabel "penilaian kinerja guru" mudah disalahartikan
 * sebagai instrumen supervisi/observasi kelas oleh kepala sekolah. Penelusuran seluruh
 * konsumennya menunjukkan yang sebaliknya — ini adalah <b>angket/kuesioner umpan balik yang
 * diisi SISWA atas guru yang mengajarnya</b>:</p>
 * <ul>
 *   <li>Label menu resminya adalah <b>"Grup Angket"</b>, bertetangga dengan menu
 *       <b>"Angket Siswa"</b> ({@code checklist_penilaian_guru.zul}) pada snapshot menu
 *       ({@code ais.common.MenuSnapshotData}); judul dialog tambah/ubahnya
 *       "Tambah/Ubah Grup Penilaian Guru", judul tab pertamanya "Grup Angket Penilaian".</li>
 *   <li>Layar pengisiannya, {@code ais.action.master.helper.generic.AngketGuruWindow}, dibuka
 *       dari portal <b>siswa</b> dan menyaring grup memakai identitas siswa yang sedang login
 *       (sekolah, yayasan, dan angkatan siswa), lalu menulis jawabannya ke
 *       {@link ChecklistBaruPenilaianGuruOlehSiswa} per kombinasi siswa-guru-jadwal pelajaran.</li>
 *   <li>Header angketnya, {@link AngketPenilaianGuru}, punya penanda eksplisit
 *       {@code untukSiswa}; seluruh query grup yang dipakai runtime mensyaratkan penanda itu
 *       bernilai {@code true} (atau {@code null}).</li>
 *   <li>Endpoint mobile {@code ais.action.servlet.api.AngketUtilApi} mengambil daftar grup ini
 *       berdasarkan {@code siswa.getSekolah()}/{@code program}/{@code tahunMasuk}.</li>
 * </ul>
 * <p>Konsekuensi praktisnya: data yang bergantung pada entity ini adalah <b>penilaian guru oleh
 * siswa</b> — data personalia yang sensitif secara reputasi. Entity ini sendiri hanya memuat
 * label, tetapi ia menentukan <i>bagian mana</i> dari angket yang tampil ke siswa mana, sehingga
 * kekeliruan pada baris ini merambat langsung ke lingkup pengumpulan data tersebut (lihat
 * catatan pada {@link #getAngketPenilaianGuru()}).</p>
 *
 * <h2>Posisi dalam rantai angket guru (4 lapis)</h2>
 * <ol>
 *   <li>{@link AngketPenilaianGuru} — header/periode angket. <b>Seluruh cakupan tenant ada di
 *       sini</b>: {@code yayasan}, {@code sekolah}, {@code program}, {@code angkatan},
 *       {@code untukSiswa}/{@code untukGuru}, {@code jumlahPilihan} (banyak opsi skala), dan
 *       {@code tampilKeterangan}.</li>
 *   <li><b>{@code GrupChecklistPenilaianGuru} (kelas ini)</b> — kelompok/aspek penilaian, dengan
 *       saklar {@link #getAktif()} sendiri.</li>
 *   <li>{@link ChecklistPenilaianGuru} — butir pertanyaan yang dijawab siswa dengan skala radio
 *       1..N.</li>
 *   <li>{@link ChecklistBaruPenilaianGuruOlehSiswa} — baris transaksi berisi seluruh jawaban satu
 *       siswa atas satu guru pada satu jadwal pelajaran.</li>
 * </ol>
 * <p><b>Kelas ini TIDAK punya kolom {@code sekolah}/{@code yayasan} sendiri.</b> Setiap
 * penyaringan tenant atas sebuah grup dilakukan lewat join ke atas
 * ({@code grup → angketPenilaianGuru}), dan atas sebuah butir lewat join dua tingkat
 * ({@code butir → grup → angket}). Itulah sebabnya FK {@link #getAngketPenilaianGuru()} yang
 * salah/kosong berdampak jauh lebih besar daripada sekadar salah label — lihat bagian kuirk.</p>
 * <p>Padanan jenjang perguruan tinggi dari kelas ini adalah
 * {@link ais.database.model.GrupChecklistPenilaianDosen} (kelompok butir angket dosen oleh
 * mahasiswa). Struktur field dan anotasi disalin kata-per-kata dari sana; kemiripan itu warisan
 * salin-tempel generator, bukan hubungan semantik antar-modul.</p>
 *
 * <h2>Status pemakaian: HIDUP PENUH, dan dipakai lintas UI/REST/laporan</h2>
 * <p>Berbeda dari beberapa kerabat di modul ini yang ternyata yatim, tipe ini dirujuk oleh
 * berkas Java yang benar-benar berjalan:</p>
 * <ul>
 *   <li>{@code ais.action.master.sekolah.GrupChecklistPenilaianGuruAction} — layar master CRUD
 *       (menu "Grup Angket"), sekaligus <b>tuan rumah</b> tiga tab layar lain (lihat bagian
 *       keamanan).</li>
 *   <li>{@code ais.action.master.sekolah.ChecklistPenilaianGuruAction} — layar master butir;
 *       memakai tipe ini sebagai isi combo "Grup" dan filter pencarian.</li>
 *   <li>{@code ais.action.master.helper.generic.AngketGuruWindow} — layar pengisian angket oleh
 *       siswa; setiap grup menjadi satu kartu/section pada formulir, dengan judul
 *       {@code angket.getIsi() + " - " + grup.getIsi()}.</li>
 *   <li>{@code ais.action.servlet.api.AngketUtilApi} — endpoint mobile; grup diserialisasi ke
 *       JSON (kunci {@code "grup"}) beserta jumlah butirnya.</li>
 *   <li>{@code ais.action.report.format1.akademik.LaporanAngketGuruDashboardWindow} — dasbor
 *       rekap; menghitung {@code totalKelompok} dan menggambar bar rata-rata nilai per kelompok.</li>
 *   <li>{@code ais.common.ChecklistPenilaianHelper} — SQL native untuk gerbang "ada jadwal angket
 *       guru yang harus diisi" ({@code adaJadwalAngketGuruDariJadwalUmum}), lewat join
 *       {@code jadwal_checklist_penilaian_umum.grup_checklist_penilaian_guru}.</li>
 *   <li>{@code ais.common.InitData} — tipe ini termasuk yang dipra-muat sebagai data master awal
 *       ({@code initClasses}), penanda bahwa tabel diperlakukan sebagai master permanen.</li>
 *   <li>{@code ais.common.InitIndex} — membuat indeks
 *       {@code ON public.parameter_tambahan_angket_umum (grup_checklist_penilaian_guru)},
 *       bukti bahwa jangkar parameter tambahan memang jalur panas.</li>
 * </ul>
 *
 * <h2>Kuirk, jebakan, dan temuan bagi pembaca kode</h2>
 * <ul>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *       bug.</b> {@link GeneralValueObject} adalah POJO abstrak biasa — bukan {@code @Entity}
 *       maupun {@code @MappedSuperclass} — sehingga Hibernate sama sekali tidak memetakan
 *       properti induknya. Setiap entity konkret <b>wajib</b> mendeklarasikan ulang keempat
 *       properti audit tersebut agar terpetakan. Lihat {@link GeneralValueObject}.</li>
 *   <li><b>Renderer grid menulis balik FK angket secara senyap.</b>
 *       {@code GrupChecklistPenilaianGuruRenderer.render()} mengeksekusi
 *       {@code if (grup.getAngketPenilaianGuru() == null &amp;&amp; angket != null)
 *       grup.setAngketPenilaianGuru(angket);} pada objek yang <b>persisten</b>. Karena Hibernate
 *       melakukan dirty-checking, sekadar <i>membuka daftar</i> sudah cukup untuk menulis FK ke
 *       basis data. Detail lengkap dan dampak lintas-sekolahnya ada di
 *       {@link #getAngketPenilaianGuru()}.</li>
 *   <li><b>Auto-seed angket induk.</b> {@code doAfterCompose()} pada layar master membuat
 *       {@link AngketPenilaianGuru} berkode {@code "001.000"} berjudul
 *       {@code "EVALUASI PENILAIAN PEMBELAJARAN"} bila tabel angket masih kosong — <b>tanpa</b>
 *       mengisi {@code yayasan}/{@code sekolah}, sehingga angket hasil semaian selalu berlaku
 *       GLOBAL untuk semua sekolah.</li>
 *   <li><b>Dua konvensi "aktif" yang berbeda hidup berdampingan.</b> Seluruh pembaca runtime
 *       (siswa/API/laporan) memakai {@code (aktif = true OR aktif IS NULL)} — sejalan dengan
 *       {@link #getAktif()}. Sebaliknya filter "Hanya Aktif" pada layar master memakai
 *       {@code Restrictions.eq("aktif", true)} saja. Akibatnya baris dengan {@code aktif = NULL}
 *       (mis. hasil unggah Excel atau insert lain) <b>tetap tampil ke siswa</b> tetapi
 *       <b>menghilang</b> dari daftar admin ketika filter itu dicentang. Ini varian ringan dari
 *       pola "kolom aktif tak pernah ditulis" yang sudah dikenal proyek ini: dialog tambah/ubah
 *       memang tidak punya isian {@code aktif} sama sekali (hanya kotak centang di grid yang
 *       menulisnya), tetapi di sini akibatnya <b>tidak</b> membuat baris baru mati — justru
 *       sebaliknya, baris baru langsung hidup.</li>
 *   <li><b>{@code keterangan} bukan petunjuk untuk siswa.</b> Lihat {@link #getKeterangan()};
 *       teks ini tidak pernah dirender pada formulir pengisian siswa (petunjuk yang tampil
 *       diambil dari {@code AngketPenilaianGuru.getPetunjuk()}), namun tetap ikut terkirim pada
 *       payload JSON endpoint mobile.</li>
 *   <li><b>Tidak ada koleksi balik.</b> Entity ini tidak memiliki {@code Set&lt;ChecklistPenilaianGuru&gt;};
 *       relasi hanya satu arah dari sisi butir. Setiap kebutuhan "butir milik grup ini" ditulis
 *       ulang sebagai {@code Criteria} terpisah di masing-masing pemanggil.</li>
 *   <li><b>{@link #serialVersionUID}</b> bernilai sama dengan ratusan entity lain di repo ini
 *       (konstanta boilerplate salin-tempel), jadi jangan dipakai sebagai petunjuk kekerabatan.</li>
 * </ul>
 *
 * <h2>Catatan keamanan (hasil verifikasi pola berulang)</h2>
 * <p>Pola-pola yang biasa ditemukan pada entity master modul ini diperiksa satu per satu:</p>
 * <ul>
 *   <li><b>Gerbang hak akses layar master: ADA dan cukup benar.</b>
 *       {@code doBeforeCompose()} memanggil {@code Common.doCheckSecurity()};
 *       {@code doAfterCompose()} memaksa logoff bila {@code READ} tidak dipenuhi; tombol Tambah
 *       digerbangi {@code CREATE}, tombol Ubah/Hapus per baris digerbangi {@code UPDATE}/
 *       {@code DELETE}, tombol unggah massal menuntut ketiganya sekaligus, dan kotak centang
 *       "Aktif" di grid memakai {@code setDisabled(!edit)}. <b>Pola "Intbox nomor urut tanpa
 *       gerbang" (batch 52) TIDAK ADA di sini</b> — layar ini memang tidak punya kolom nomor
 *       urut yang dapat disunting inline.</li>
 *   <li><b>Pewarisan hak lewat menu induk: ADA — dan layar ini adalah sisi PEMBERINYA.</b>
 *       {@code grup_checklist_penilaian_guru.zul} memiliki entri menu sendiri ("Grup Angket"),
 *       tetapi tiga tab lain di dalamnya —
 *       {@code /pages/master/grup_checklist_penilaian_umum.zul},
 *       {@code /pages/master/jadwal_checklist_penilaian_umum.zul}, dan
 *       {@code /pages/master/grup_kuesioner_umum.zul} — <b>tidak punya entri menu sama sekali</b>
 *       pada {@code MenuSnapshotData}. Ketiganya disisipkan lewat {@code MyInclude} dari
 *       {@code onGrupAngketUmum()}/{@code onJadwalAngketUmum()}/{@code onGrupKuosioner()},
 *       sehingga {@code checkPrevilages()} yang dijalankan action masing-masing menguji hak menu
 *       <i>induk</i> (Grup Angket) ini, bukan hak layar yang sesungguhnya dibuka. Siapa pun yang
 *       diberi hak kelola Grup Angket otomatis memperoleh hak setara atas master angket UMUM
 *       (lintas modul dosen/umum) dan atas <b>penjadwalan</b> angket. Ini instance baru dari pola
 *       yang sudah dicatat proyek ini; yang membedakan, di sini pemicunya adalah tab
 *       {@code MyInclude} pada layar bermenu, bukan layar tak bermenu yang menumpang.</li>
 *   <li><b>Cakupan tenant: bergantung konteks, bukan ditegakkan query.</b> {@code initCriteria()}
 *       menyaring lewat {@code angketPenilaianGuru.sekolah}/{@code .yayasan}/{@code .program},
 *       tetapi <i>hanya bila combo pencarian yang bersangkutan punya pilihan</i>; bila tidak,
 *       yang dipasang adalah {@code Restrictions.sqlRestriction("1=1")}. Combo itu diisi
 *       {@code Common.initYayasanDanSekolahDanSemua(...)} yang memilihkan (dan menonaktifkan,
 *       untuk non-admin) sekolah/yayasan pengguna <b>bila konteks sekolah aktif diketahui</b>.
 *       Untuk pengguna tanpa konteks sekolah, daftar grup <b>seluruh sekolah/yayasan</b> tampil
 *       dan dapat disunting/dihapus. Perlu dicatat bahwa data yang terpapar di sini hanyalah
 *       metadata katalog (nama kelompok + keterangan admin), bukan nilai penilaian guru — nilai
 *       tersimpan di {@link ChecklistBaruPenilaianGuruOlehSiswa}. Meski begitu, karena entity ini
 *       menentukan bagian angket yang tampil ke siswa, kemampuan menyunting lintas tenant di sini
 *       dapat dipakai untuk <b>mengubah instrumen penilaian guru sekolah lain</b> — bobot risiko
 *       yang lebih tinggi daripada katalog metadata biasa.</li>
 *   <li><b>Getter destruktif/write-back pada entity ini sendiri: TIDAK ADA.</b> Semua getter di
 *       berkas ini bebas efek samping kecuali resolusi proxy lazy pada
 *       {@link #getAngketPenilaianGuru()} (yang memang perilaku baku {@code check()} seluruh
 *       repo). Perilaku destruktifnya berada di <i>renderer</i>, bukan di entity.</li>
 *   <li><b>{@link #getKeterangan()} membalik kontrak: TIDAK ADA.</b> Getter ini mengembalikan
 *       field apa adanya, tanpa fallback maupun penulisan balik.</li>
 * </ul>
 *
 * <p>Perubahan (create/update) tercatat historisnya lewat {@link Audited} (Hibernate Envers), dan
 * setiap update memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see AngketPenilaianGuru
 * @see ChecklistPenilaianGuru
 * @see ChecklistBaruPenilaianGuruOlehSiswa
 * @see ais.database.model.GrupChecklistPenilaianDosen
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sekolah", name = "grup_checklist_penilaian_guru")
public class GrupChecklistPenilaianGuru extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya identik dengan ratusan entity lain di repo ini
	 * (konstanta boilerplate hasil salin-tempel), jadi <b>bukan</b> petunjuk kekerabatan antar
	 * kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci utama baris ({@code id}, {@code bigserial}). Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} tidak dipetakan Hibernate — lihat catatan pada Javadoc kelas.
	 */
	private Long id;

	/**
	 * Nama tampil pengguna terakhir yang mengubah baris ini (kolom {@code oleh}), diisi otomatis
	 * oleh {@link ais.database.hibernate.AuditTimestampInterceptor}. Dideklarasikan ulang atas
	 * alasan pemetaan yang sama dengan {@link #id}.
	 */
	private String oleh;

	/**
	 * Id/username pengguna terakhir yang mengubah baris ini (kolom {@code oleh_id}), pasangan
	 * teknis dari {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Cap waktu perubahan terakhir (kolom {@code tanggal_dirubah}). Diinisialisasi ke waktu
	 * pembuatan objek lewat {@link ais.ui.util.WaktuUtil#getDate()} agar baris baru tidak pernah
	 * bernilai {@code null}, lalu diperbarui pada setiap update oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Angket penilaian guru induk tempat grup ini berada (FK {@code angket_penilaian_guru},
	 * nullable). Inilah satu-satunya jalur cakupan tenant/periode bagi grup — lihat
	 * {@link #getAngketPenilaianGuru()}.
	 */
	private AngketPenilaianGuru angketPenilaianGuru;

	/**
	 * Nama/judul grup checklist, mis. "Kedisiplinan", "Penguasaan Materi" (kolom {@code isi},
	 * {@code text}, wajib diisi). Dipakai sebagai judul section pada formulir siswa dan sebagai
	 * label pengurutan/pencarian.
	 */
	private String isi;

	/**
	 * Catatan bebas untuk administrator (kolom {@code keterangan}, {@code text}, opsional).
	 * <b>Bukan</b> petunjuk pengisian bagi siswa — lihat {@link #getKeterangan()}.
	 */
	private String keterangan;

	/**
	 * Saklar aktif grup (kolom {@code aktif}, nullable). {@code null} diperlakukan sebagai
	 * <b>aktif</b> baik oleh {@link #getAktif()} maupun oleh query pembaca runtime — lihat
	 * catatan konvensi ganda pada Javadoc kelas.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk instansiasi entity. Dipakai
	 * juga oleh layar master saat menekan tombol Tambah ({@code onAdd()} membuat instance kosong,
	 * lalu {@code onSave()} mengisi {@link #setAngketPenilaianGuru(AngketPenilaianGuru)},
	 * {@link #setIsi(String)}, dan {@link #setKeterangan(String)}).
	 */
	public GrupChecklistPenilaianGuru() {
	}

	/**
	 * Mengembalikan kunci utama baris. Bernilai {@code null} selama objek masih transient
	 * (belum tersimpan); layar master memanfaatkan hal itu untuk memilih judul dialog
	 * "Tambah" vs "Ubah" dan untuk memutuskan perlu-tidaknya {@code session.load()} sebelum
	 * menyimpan.
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menetapkan kunci utama baris. Normalnya hanya dipanggil Hibernate setelah {@code INSERT}
	 * (kolom {@code insertable = false}, nilai dihasilkan sequence basis data); kode aplikasi
	 * tidak seharusnya memanggilnya sendiri.
	 *
	 * @param id id baris yang akan dipasang
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan id/username pengguna terakhir yang mengubah baris ini.
	 *
	 * @return isi kolom {@code oleh_id}, boleh {@code null} untuk baris yang belum pernah diaudit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id/username pengguna pengubah terakhir.
	 * <p>
	 * <b>Perilaku non-obvious:</b> setter ini <b>menolak secara senyap</b> nilai {@code null}
	 * maupun string kosong/berisi spasi — dalam kasus itu nilai lama dipertahankan, bukan
	 * ditimpa. Pola ini konsisten di seluruh entity repo agar jejak audit yang sudah ada tidak
	 * hilang ketika sebuah objek disalin/di-merge tanpa membawa konteks pengguna.
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan nama tampil pengguna terakhir yang mengubah baris ini.
	 *
	 * @return isi kolom {@code oleh}, boleh {@code null} untuk baris yang belum pernah diaudit
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama tampil pengguna pengubah terakhir. Sama seperti
	 * {@link #setOlehId(String)}, nilai {@code null}/kosong <b>diabaikan secara senyap</b>
	 * sehingga jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@link javax.persistence.PreUpdate} yang dijalankan tepat sebelum setiap
	 * {@code UPDATE} baris ini. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}, yang memperbarui
	 * {@link #tanggal_dirubah} serta mengisi {@link #oleh}/{@link #olehId} dari konteks pengguna
	 * aktif.
	 * <p>
	 * <b>Efek samping penting:</b> karena callback ini terpasang, setiap perubahan yang terdeteksi
	 * dirty-checking Hibernate — termasuk penulisan balik FK angket yang dilakukan renderer grid
	 * (lihat {@link #getAngketPenilaianGuru()}) — akan menghasilkan cap waktu baru <i>dan</i>
	 * revisi baru pada tabel audit Envers, atas nama pengguna yang sekadar membuka daftar.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return isi kolom {@code tanggal_dirubah}; untuk objek baru bernilai waktu pembuatan objek,
	 *         bukan {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan cap waktu perubahan terakhir. Umumnya diisi otomatis lewat {@link #onUpdate()};
	 * pemanggilan manual hanya wajar pada jalur impor/migrasi data yang ingin mempertahankan cap
	 * waktu asli.
	 *
	 * @param tanggal_dirubah cap waktu yang akan dipasang
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan {@link AngketPenilaianGuru} induk tempat grup ini bernaung (FK
	 * {@code angket_penilaian_guru}), setelah melewati {@code check()} milik
	 * {@link GeneralValueObject} yang meresolusi proxy lazy Hibernate menjadi instance nyata
	 * (canonical) bila memungkinkan. Penugasan ulang ke field pada baris pertama adalah bagian
	 * dari mekanisme resolusi itu, bukan mutasi data — nilai FK yang tersimpan tidak berubah.
	 *
	 * <h3>Mengapa relasi ini kritis</h3>
	 * <p>Kelas ini tidak punya kolom {@code sekolah}/{@code yayasan}/{@code program}/
	 * {@code angkatan} sendiri. Seluruh penentuan "grup ini berlaku untuk siswa yang mana"
	 * dilakukan dengan join ke angket induk:</p>
	 * <ul>
	 *   <li>{@code AngketGuruWindow.buildGrupCriteria()} — {@code angketPenilaianGuru.untukSiswa}
	 *       harus {@code true}/{@code null}, sekolah &amp; yayasan harus cocok dengan milik siswa
	 *       (atau {@code NULL} = berlaku global), angkatan harus cocok (atau kosong/{@code NULL}).</li>
	 *   <li>{@code AngketUtilApi} — pemeriksaan setara untuk jalur mobile, ditambah kecocokan
	 *       {@code program}.</li>
	 *   <li>{@code GrupChecklistPenilaianGuruAction.initCriteria()} — filter pencarian admin
	 *       memakai alias {@code angketPenilaianGuru} yang sama.</li>
	 * </ul>
	 * <p>Karena {@code @JoinColumn} di sini {@code nullable = true}, grup <b>yatim</b> (FK
	 * {@code NULL}) mungkin ada — misalnya hasil unggah Excel massal. Grup yatim tidak akan pernah
	 * lolos filter {@code untukSiswa} pada jalur pengisian, sehingga secara efektif tak terlihat
	 * oleh siswa.</p>
	 *
	 * <h3>Jebakan: penulisan balik FK oleh renderer grid</h3>
	 * <p>{@code GrupChecklistPenilaianGuruRenderer.render()} pada layar master menjalankan:</p>
	 * <pre>
	 * if (grup.getAngketPenilaianGuru() == null &amp;&amp; angket != null) {
	 *     grup.setAngketPenilaianGuru(angket);
	 * }
	 * </pre>
	 * <p>di mana {@code angket} adalah hasil
	 * {@code createCriteria(AngketPenilaianGuru.class).setMaxResults(1).uniqueResult()} —
	 * <b>satu baris sembarang</b>, tanpa {@code addOrder} dan <b>tanpa filter sekolah/yayasan
	 * sama sekali</b>. Objek {@code grup} berasal dari {@code Criteria.list()} sehingga berstatus
	 * persisten; dirty-checking Hibernate akan menuliskan FK barunya ke basis data pada flush
	 * berikutnya. Konsekuensinya:</p>
	 * <ul>
	 *   <li>Sekadar <b>membuka daftar</b> (hak {@code READ} sudah cukup) dapat mengubah data —
	 *       tanpa konfirmasi, tanpa tombol simpan.</li>
	 *   <li>Angket yang dipilih bisa milik <b>sekolah/yayasan lain</b>. Grup yatim yang tadinya
	 *       tak terpakai bisa mendadak melekat ke instrumen penilaian sekolah lain, lalu ikut
	 *       tampil (beserta seluruh butirnya) pada angket yang diisi siswa sekolah tersebut.</li>
	 *   <li>Setiap penulisan itu menghasilkan revisi Envers baru atas nama pengguna yang membuka
	 *       layar, mengaburkan jejak audit siapa yang sebenarnya mengubah pemetaan angket.</li>
	 * </ul>
	 * <p>Pola persis yang sama juga ada satu tingkat lebih dalam di
	 * {@code ChecklistPenilaianGuruAction} (butir yatim ditempelkan ke grup sembarang hasil
	 * {@code setMaxResults(1)}). Perbaikan yang benar adalah memindahkan penetapan default ini ke
	 * {@code onSave()} atau menjadikannya murni tampilan (variabel lokal), bukan memanggil setter
	 * pada objek persisten di dalam renderer.</p>
	 *
	 * @return angket induk yang sudah teresolusi, atau {@code null} bila grup ini yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "angket_penilaian_guru", nullable = true)
	public AngketPenilaianGuru getAngketPenilaianGuru() {
		angketPenilaianGuru = check(angketPenilaianGuru);
		return angketPenilaianGuru;
	}

	/**
	 * Menetapkan angket penilaian guru induk. Dipanggil dari {@code onSave()} layar master
	 * (setelah validasi "Nama Angket harus diisi", sehingga jalur normal tidak pernah menyimpan
	 * {@code null}) dan — inilah yang perlu diwaspadai — dari renderer grid sebagai penulisan
	 * balik senyap; lihat {@link #getAngketPenilaianGuru()}.
	 *
	 * @param angketPenilaianGuru angket induk; {@code null} diperbolehkan oleh pemetaan
	 *                            ({@code nullable = true}) dan menandakan grup yatim
	 */
	public void setAngketPenilaianGuru(AngketPenilaianGuru angketPenilaianGuru) {
		this.angketPenilaianGuru = angketPenilaianGuru;
	}

	/**
	 * Mengembalikan nama/judul grup (kolom {@code isi}, wajib). Nilai inilah yang tampil sebagai
	 * judul section pada formulir angket siswa (digabung judul angket:
	 * {@code angket.getIsi() + " - " + grup.getIsi()}), sebagai label pada combo "Grup" di layar
	 * master butir, sebagai sumbu pengurutan {@code Order.asc("isi")} di hampir semua query, dan
	 * sebagai kolom yang dicari filter "Nama Grup" ({@code ilike ANYWHERE}).
	 *
	 * @return judul grup; secara pemetaan {@code nullable = false}, namun jalur unggah massal
	 *         tetap dapat menghasilkan nilai kosong
	 */
	@Column(name = "isi", nullable = false, columnDefinition = "text")
	public String getIsi() {
		return isi;
	}

	/**
	 * Menetapkan nama/judul grup. Dipanggil dari {@code onSave()} layar master setelah validasi
	 * "Nama Grup harus diisi" (string kosong ditolak dengan messagebox), dan dari jalur impor
	 * Excel {@code Common.uploadData(...)} yang <b>tidak</b> menjalankan validasi tersebut.
	 *
	 * @param isi judul grup yang akan dipasang
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/**
	 * Mengembalikan catatan bebas administrator untuk grup ini (kolom {@code keterangan},
	 * opsional). Getter ini mengembalikan field <b>apa adanya</b> — tanpa fallback, tanpa
	 * normalisasi, dan tanpa penulisan balik (pola "{@code getKeterangan()} membalik kontrak"
	 * yang dikenal di beberapa entity lain repo ini <b>tidak</b> berlaku di sini).
	 * <p>
	 * <b>Jangan salah kira sebagai petunjuk pengisian.</b> Teks ini tidak pernah dirender pada
	 * formulir angket siswa; petunjuk yang tampil di sana berasal dari
	 * {@code AngketPenilaianGuru.getPetunjuk()}. Pembacanya hanya kolom "Keterangan" pada grid
	 * layar master, isian awal dialog ubah, dan berkas ekspor/impor
	 * ({@code contents = {id, angketPenilaianGuru, isi, aktif, keterangan}}).
	 * <p>
	 * <b>Catatan paparan:</b> endpoint mobile {@code AngketUtilApi} menyerialisasi objek grup ke
	 * JSON lewat {@code Common.insertProperty(GrupChecklistPenilaianGuru.class, g, ..., "grup", 1)},
	 * sehingga catatan internal yang ditulis admin di sini <b>ikut terkirim ke perangkat siswa</b>
	 * meski tidak ditampilkan oleh antarmuka mana pun. Hindari menaruh informasi internal/sensitif
	 * pada kolom ini.
	 *
	 * @return catatan administrator, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan catatan bebas administrator. Diisi dari textbox "Keterangan" pada dialog
	 * tambah/ubah layar master (dan dari jalur impor Excel); tanpa validasi maupun batasan
	 * panjang di sisi aplikasi (kolom bertipe {@code text}).
	 *
	 * @param keterangan catatan yang akan dipasang; {@code null} diperbolehkan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif grup, dengan <b>{@code null} diperlakukan sebagai aktif</b>
	 * ({@code aktif == null ? Boolean.TRUE : aktif}).
	 * <p>
	 * Konvensi "null = aktif" ini <b>konsisten</b> dengan seluruh pembaca runtime, yang menyaring
	 * memakai {@code Restrictions.or(eq("aktif", true), isNull("aktif"))} — {@code AngketGuruWindow}
	 * (formulir siswa, baik untuk grup maupun untuk butir di dalamnya), {@code AngketUtilApi}
	 * (mobile), {@code LaporanAngketGuruDashboardWindow} (rekap, saat opsi "hanya aktif" dipilih),
	 * dan SQL native {@code ChecklistPenilaianHelper} ({@code gkh.aktif=true or gkh.aktif is null}).
	 * <p>
	 * <b>Ketidakselarasan yang perlu diketahui:</b> filter "Hanya Aktif" pada layar master
	 * memakai {@code Restrictions.eq("aktif", true)} <i>saja</i>. Baris dengan {@code aktif = NULL}
	 * — yang tidak dapat dihindari karena dialog tambah/ubah tidak punya isian {@code aktif} sama
	 * sekali, sehingga baris baru selalu lahir {@code NULL} — akan <b>hilang dari daftar admin</b>
	 * saat filter itu dicentang, padahal siswa tetap melihatnya. Ini kebalikan dari pola "kolom
	 * aktif tak pernah ditulis" yang biasa ditemukan (di sana baris baru tak pernah muncul untuk
	 * pengguna akhir); di sini baris baru justru langsung tayang, dan yang tersembunyi adalah
	 * pandangan administratornya.
	 * <p>
	 * Nilai {@code aktif} sesungguhnya hanya ditulis lewat dua jalur: kotak centang "Aktif" pada
	 * grid layar master (digerbangi hak {@code UPDATE}, langsung menyimpan lewat
	 * {@code Common.refreshSaveOrUpdate}) dan impor Excel massal.
	 * <p>
	 * <b>Efek berantai:</b> menonaktifkan grup akan menyembunyikan <i>seluruh butir</i> di
	 * bawahnya dari formulir siswa, karena {@code buildChecklistCriteria()} mensyaratkan
	 * {@code grupChecklistPenilaianGuru.aktif} aktif selain {@code aktif} butir itu sendiri.
	 * Jawaban yang terlanjur tersimpan tidak ikut hilang: {@code AngketGuruWindow} menampilkannya
	 * kembali sebagai kartu read-only "Riwayat Jawaban pada Pertanyaan yang Sudah Dinonaktifkan".
	 *
	 * @return {@link Boolean#TRUE} bila grup aktif atau kolomnya {@code NULL};
	 *         {@link Boolean#FALSE} hanya bila kolom benar-benar berisi {@code false}. Tidak
	 *         pernah mengembalikan {@code null}.
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/**
	 * Menetapkan status aktif grup. Dipanggil dari listener {@code onCheck} kotak centang "Aktif"
	 * pada grid layar master (yang langsung menyimpan barisnya) dan dari jalur impor Excel.
	 * <p>
	 * Perhatikan asimetri dengan {@link #getAktif()}: setter ini menyimpan {@code null} apa adanya
	 * bila diberi {@code null}, sedangkan getter menerjemahkannya menjadi {@code TRUE}. Karena itu
	 * membaca kembali nilai yang baru saja di-set tidak selalu menghasilkan nilai yang sama.
	 *
	 * @param aktif status aktif; {@code null} berarti "belum pernah ditentukan" dan akan dibaca
	 *              sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Representasi teks ringkas baris ini dalam format {@code "<id>-<isi>"} (mis.
	 * {@code "12-Kedisiplinan"}), dengan setiap bagian yang {@code null} diganti string kosong —
	 * objek transient tanpa judul menghasilkan string kosong, bukan {@code NullPointerException}.
	 * <p>
	 * Dipakai untuk keperluan diagnostik/log; combo dan grid pada layar master <b>tidak</b>
	 * memakai method ini melainkan memformat sendiri dari properti bernama
	 * ({@code Common.insertCombo(..., "isi", "angketPenilaianGuru", ...)}).
	 *
	 * @return gabungan id dan judul grup; tidak pernah {@code null}
	 */
	public String toString() {
		return (id == null ? "" : id + "-") + (isi == null ? "" : isi);
	}
}
