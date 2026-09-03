package ais.database.model.sekolah;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate untuk tabel {@code sekolah.checklist_baru_penilaian_guru_oleh_siswa},
 * merepresentasikan satu baris rekap penilaian seorang {@link Guru} oleh seorang {@link Siswa}
 * untuk satu {@link JadwalPelajaran} tertentu (modul jenjang sekolah). Kombinasi
 * (siswa, jadwalPelajaran, guru) bersifat unik per baris &mdash; dijaga lewat kolom turunan
 * {@link #getKodeUnik()} yang dihitung otomatis dari id ketiga relasi tersebut.
 *
 * <h2>Peran bisnis: satu lembar jawaban angket evaluasi guru</h2>
 * <p>
 * Entity ini adalah <b>lembar jawaban</b> dari fitur yang dikenal pengguna sebagai
 * <i>"Angket Penilaian Guru"</i>: survei di mana <b>siswa menilai gurunya</b> (bukan sebaliknya).
 * Verifikasi arah penilaian dilakukan dari kode, bukan dari nama kelas: baris hanya pernah dibuat
 * pada jalur yang identitas pengisinya diambil dari sesi/token <i>siswa</i>
 * ({@code AngketGuruWindow} untuk klien web ZK dan {@code AngketUtilApi} untuk klien mobile,
 * keduanya mengambil {@code tbmuser.getSiswa()}), sedangkan {@link #getGuru()} selalu diisi guru
 * pengampu {@link JadwalPelajaran} yang sedang dinilai. Kolom {@link #getMasukan()} adalah kotak
 * teks bebas "Masukan/Saran/Komentar" yang ditulis siswa tentang guru tersebut.
 * </p>
 * <p>
 * Susunan pertanyaan angket berjenjang dan berada di entity lain (semuanya TERVERIFIKASI dari
 * kode, bukan dugaan dari kemiripan nama):
 * </p>
 * <ol>
 *   <li>{@link AngketPenilaianGuru} &mdash; angket induk; menentukan skala jawaban
 *       ({@code jumlahPilihan}) dan cakupan berlakunya (yayasan/sekolah/program/angkatan);</li>
 *   <li>{@link GrupChecklistPenilaianGuru} &mdash; kelompok/aspek pertanyaan di bawah angket
 *       induk; dapat memiliki {@code ParameterTambahanAngketUmum} yang jawabannya TIDAK disimpan
 *       di sini melainkan di {@link ais.database.model.IsiAngketParameterUmum};</li>
 *   <li>{@link ChecklistPenilaianGuru} &mdash; butir pertanyaan individual; <b>id butir inilah</b>
 *       yang menjadi kunci di dalam kolom teks {@link #getKeterangan()} pada baris ini.</li>
 * </ol>
 * <p>
 * Entity ini adalah <b>skema BARU</b>. Skema lamanya, {@link ChecklistPenilaianGuruOlehSiswa},
 * menyimpan satu baris per butir pertanyaan dan masih ada di basis data; keduanya hanya
 * bersinggungan lewat relasi opsional
 * {@link ChecklistPenilaianGuruOlehSiswa#getChecklistBaruPenilaianGuruOlehSiswa()} dan tidak saling
 * menyinkronkan. Padanan entity ini di jenjang perguruan tinggi adalah
 * {@code ChecklistBaruPenilaianDosenOlehMahasiswa} (mahasiswa menilai dosen per
 * {@code Perkuliahan}), berpola identik dan ditangani berdampingan di
 * {@code AngketUtilApi}/{@code AngketUtil}.
 * </p>
 *
 * <h2>Format terpadatkan kolom {@code keterangan} (bagian paling non-obvious)</h2>
 * <p>
 * Berbeda dari pola satu-baris-per-item-penilaian, seluruh jawaban siswa atas banyak butir
 * {@link ChecklistPenilaianGuru} untuk kombinasi ini disimpan <b>terpadatkan dalam satu kolom
 * teks</b> {@link #getKeterangan()}, dengan format per butir {@code "DATA<idButir>;<nilai><>ket"}
 * dan antar-butir dipisah {@code "___"}. Method {@link #setValue(Integer, Siswa, Guru,
 * JadwalPelajaran, ChecklistPenilaianGuru, String)}, {@link #getValue(ChecklistPenilaianGuru)},
 * {@link #getKeteranganValue(ChecklistPenilaianGuru)}, {@link #check(ChecklistPenilaianGuru)},
 * {@link #count()} dan {@link #ambilValue()} adalah satu-satunya jalur baca/tulis format
 * terpadatkan ini &mdash; dipakai alih-alih tabel anak terpisah agar seluruh hasil checklist satu
 * siswa-guru-jadwal dapat diperbarui dalam satu baris/transaksi (penting karena layar angket
 * melakukan <i>autosave</i> setiap kali radio button digeser).
 * </p>
 * <p>
 * <b>Jebakan pemisah.</b> Seluruh pemecahan record memakai
 * {@code org.apache.commons.lang.StringUtils.split(str, "___")}. Pada commons-lang 2 argumen kedua
 * adalah <b>himpunan karakter pemisah</b>, bukan string pemisah: efektifnya teks dipecah pada
 * <b>setiap karakter {@code '_'} tunggal</b> (deretan {@code '_'} beruntun dianggap satu pemisah).
 * Karena {@link #setValue} tidak membersihkan {@code '_'} dari keterangan yang diketik siswa,
 * keterangan yang memuat garis bawah akan terpecah; pecahan yang tidak diawali penanda
 * {@code "DATA"} akan <b>dibuang secara diam-diam</b> pada penyimpanan berikutnya (lihat perulangan
 * rekonstruksi di {@link #setValue}) &mdash; kehilangan data tanpa pesan galat. Karakter
 * {@code ';'}, {@code "<>"}, {@code "DATA"} dan {@code "data"} <i>memang</i> dibersihkan, sehingga
 * siswa tidak dapat <i>memalsukan</i> butir jawaban lain; yang bisa terjadi hanyalah pemotongan
 * teksnya sendiri.
 * </p>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Identitas &amp; audit</b>: {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Relasi</b>: {@link #getSiswa()} (penilai), {@link #getGuru()} (yang dinilai),
 *       {@link #getJadwalPelajaran()} (konteks mata pelajaran/kelas/tahun ajaran/semester).</li>
 *   <li><b>Kunci turunan</b>: {@link #getKodeUnik()}.</li>
 *   <li><b>Isi jawaban</b>: {@link #getKeterangan()} (terpadatkan) dan {@link #getMasukan()}
 *       (teks bebas satu per baris/lembar, bukan per butir).</li>
 *   <li><b>Mesin format terpadatkan</b>: {@link #setValue}, {@link #getValue},
 *       {@link #getKeteranganValue}, {@link #check(ChecklistPenilaianGuru)}, {@link #count()},
 *       {@link #ambilValue()}.</li>
 * </ul>
 *
 * <h2>Catatan teknis pewarisan</h2>
 * <p>
 * Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa &mdash; Hibernate
 * TIDAK memetakan properti induknya. Karena itu field {@code id}, {@code oleh}, {@code olehId} dan
 * {@code tanggal_dirubah} <b>sengaja dideklarasikan ulang</b> di kelas ini; itu keharusan teknis,
 * bukan duplikasi yang keliru. Konsekuensi halus yang berjalan benar: karena tanda tangan setter
 * sama persis, pemanggilan {@code setOleh}/{@code setOlehId}/{@code setTanggal_dirubah} dari
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} (yang melakukan cast ke
 * tipe induk) tetap tersalur ke setter kelas ini lewat <i>dynamic dispatch</i>, sehingga yang
 * terisi adalah field yang benar-benar terpetakan.
 * </p>
 * <p>
 * Perubahan (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate
 * Envers), dan setiap update otomatis memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
 * </p>
 *
 * <h2>Privasi identitas penilai &mdash; risiko struktural pada entity ini, DIPERBAIKI di lapisan
 * penyajian pada 3 Sep 2026</h2>
 * <p>
 * Baris ini menautkan <b>identitas siswa penilai</b> ({@link #getSiswa()}) ke <b>guru yang
 * dinilai</b> ({@link #getGuru()}) berikut komentar teks bebasnya ({@link #getMasukan()}). Layar
 * pengisian menampilkan ajakan "Isi penilaian guru dengan jujur dan objektif" tanpa menjanjikan
 * anonimitas, dan rancangan penyimpanan entity ini sendiri memang <b>tidak anonim sama sekali</b>
 * &mdash; identitas penilai tetap melekat di kolom {@code siswa} pada setiap baris. Tiga hal yang
 * perlu diketahui pembaca kode:
 * </p>
 * <ul>
 *   <li><b>Dasbor rekap SEBELUMNYA membuka identitas pengisi (sudah diperbaiki).</b>
 *       {@code ais.action.report.format1.akademik.LaporanAngketGuruDashboardWindow} menyusun panel
 *       "Masukan / Catatan Terbaru" dari pasangan {@code (guru, siswa, masukan)}. Sampai dengan
 *       3 Sep 2026 kolomnya berjudul "Pengisi / Catatan" dan nilainya berasal dari
 *       {@code String.valueOf(siswa)} &mdash; dan {@link Siswa#toString()} menghasilkan
 *       {@code "<id>-<nomorInduk>-<namaSiswa>"}, sehingga yang tampil adalah <b>NIS dan nama
 *       lengkap</b> siswa di samping komentarnya. Perbaikan mengganti nilai tersebut dengan token
 *       anonim per-sesi-pemuatan ({@code DashboardData.anonSiswaLabel(Siswa)}, mis. "Responden 3",
 *       stabil selama satu pemuatan dasbor agar beberapa masukan dari siswa yang sama tetap terlihat
 *       berasal dari responden yang sama tanpa membuka identitasnya) dan mengubah judul kolom/popup
 *       menjadi "Responden (Anonim)"; hal yang sama juga berlaku untuk kolom "Siswa" pada popup
 *       kartu "Angket Terisi" ({@code data.formRows}), yang sebelumnya memakai
 *       {@code safeToString(siswa)} dengan masalah identik. Perbaikan ini murni di lapisan
 *       penyajian (window laporan) &mdash; entity ini TIDAK diubah, karena {@link #getSiswa()}
 *       tetap dipakai sebagai kunci fungsional (lihat paragraf di bawah). Tombol ekspor PDF/Excel
 *       ({@code DashboardGridExportHelper.pasang}) membaca ulang teks yang SUDAH dirender di layar,
 *       sehingga ikut teranonimkan tanpa perubahan terpisah.</li>
 *   <li><b>Dasbor itu tidak memiliki gerbang hak akses miliknya sendiri</b> (tidak ada pemanggilan
 *       {@code checkPrevilages} di dalamnya) dan disisipkan sebagai salah satu tab pada
 *       {@code LaporanAngketDosenPerDosenWindow}; hak membukanya diwarisi sepenuhnya dari hak menu
 *       laporan induk. Filter guru pada dasbor (widget {@code AmbilDataGuruBanbox}) diisi otomatis
 *       dan dikunci ({@code setDisabled(true)}) ke guru milik sesi login bila penggunanya sendiri
 *       tercatat sebagai {@link Guru} &mdash; jadi pada pemakaian normal seorang guru hanya melihat
 *       baris penilaian atas dirinya sendiri, bukan guru lain; penguncian ini bersifat UI (client-side),
 *       bukan validasi ulang di {@code loadDashboardData}. Anonimisasi kolom "Responden" di atas
 *       adalah mitigasi utama karena tidak bergantung pada kekuatan penguncian tersebut.</li>
 *   <li><b>Riwayat Envers menyimpan selamanya.</b> Karena kelas ini {@link Audited}, menghapus atau
 *       mengosongkan {@link #getMasukan()} di baris utama tidak menghapus versi sebelumnya di tabel
 *       revisi; pasangan (siswa penilai, komentar) tetap dapat direkonstruksi dari sana oleh siapa
 *       pun yang punya akses ke tabel revisi Envers secara langsung (di luar cakupan perbaikan
 *       lapisan penyajian ini).</li>
 * </ul>
 * <p>
 * Entity ini sengaja TIDAK diubah oleh perbaikan privasi di atas: {@link #getSiswa()} dipakai
 * sebagai kunci fungsional (mencegah pengisian ganda, dan menjawab "apakah siswa ini masih punya
 * kewajiban mengisi angket" lewat {@code ais.common.ChecklistPenilaianGuruHelper} dan
 * {@code ais.common.AngketUtil}), jadi kolom {@code siswa} tetap harus menyimpan identitas asli.
 * </p>
 *
 * <h2>Cakupan tenant (yayasan/sekolah) &mdash; fail-open struktural</h2>
 * <p>
 * Entity ini <b>tidak memiliki kolom {@code sekolah} maupun {@code yayasan}</b>; cakupan tenant
 * hanya tersirat lewat relasi {@link #getSiswa()}/{@link #getGuru()}/{@link #getJadwalPelajaran()}.
 * Akibatnya, pada Generic CRUD v2 {@code GenericCrudAutoEntityAdapter.scopeBindings()} tidak dapat
 * memasang pembatas {@code sekolah}/{@code yayasan} (properti tidak ada), dan pembatas
 * {@code siswa}/{@code guru} hanya dipasang bila {@code roleId} pengguna mengandung kata
 * "siswa"/"guru". Untuk peran administratif lain (mis. tata usaha, kurikulum, kepala sekolah,
 * operator) himpunan pembatas menjadi <b>kosong</b>, sehingga daftar/ekspor mengembalikan seluruh
 * baris angket <b>lintas sekolah dan yayasan</b> di satu instalasi. Entity ini memang terjangkau
 * jalur tersebut: {@code webapp/WEB-INF/new/root/report/services/format1/akademik/
 * laporan_angket_guru_dashboard_service.jsp} mendaftarkannya pada {@code nuiServiceEntities} dan
 * menyertakan {@code _shared/services/dispatcher.jsp}. Untuk peran ber-{@code roleId} "guru"
 * pembatas {@code guru} justru <i>terpasang</i> &mdash; hasilnya tepat berisi baris penilaian atas
 * dirinya sendiri, lengkap dengan relasi {@link #getSiswa()}; di sini "scope" bukan pelindung,
 * melainkan justru jalur pembuka identitas penilai. Operasi hapus terkunci secara kebetulan
 * (adapter hanya melayani <i>soft delete</i> dan entity ini tidak punya kolom {@code aktif}), tetapi
 * penguncian itu tidak berlaku untuk baca/ekspor.
 * </p>
 *
 * @see ChecklistPenilaianGuru
 * @see GrupChecklistPenilaianGuru
 * @see AngketPenilaianGuru
 * @see ChecklistPenilaianGuruOlehSiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sekolah", name = "checklist_baru_penilaian_guru_oleh_siswa")
public class ChecklistBaruPenilaianGuruOlehSiswa extends GeneralValueObject {

	/** Versi serialisasi Java; tetap agar sesi ZK lama tidak gagal dideserialisasi setelah kelas diubah. */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris (kolom {@code id}, IDENTITY). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris; diisi jalur audit, dideklarasikan ulang karena induk tidak dipetakan Hibernate. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris; pasangan dari {@link #oleh}. */
	private String olehId;
	/** Waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat dan disegarkan tiap update lewat {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Siswa <b>penilai</b> (pengisi angket); wajib, bagian pertama {@link #kodeUnik}. */
	private Siswa siswa;
	/** Jadwal pelajaran yang menjadi konteks penilaian (mata pelajaran, kelas, tahun ajaran, semester); wajib. */
	private JadwalPelajaran jadwalPelajaran;
	/** Guru <b>yang dinilai</b> pada {@link #jadwalPelajaran} tersebut; wajib. */
	private Guru guru;
	/** Seluruh jawaban per butir dalam satu string terpadatkan; lihat penjelasan format pada Javadoc kelas. */
	private String keterangan = "";
	/** Kunci turunan {@code "<idSiswa>_<idJadwal>_<idGuru>"}; dihitung ulang oleh {@link #getKodeUnik()}. */
	private String kodeUnik = "";
	/** Komentar/saran teks bebas siswa untuk guru ini, berlaku satu per lembar (bukan per butir). */
	private String masukan = "";

	/** Constructor tanpa argumen yang diwajibkan Hibernate; seluruh field teks sudah berisi string kosong, bukan {@code null}. */
	public ChecklistBaruPenilaianGuruOlehSiswa() {
	}

	/**
	 * Kunci utama baris.
	 *
	 * @return id baris; {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menetapkan kunci utama; normalnya hanya dipanggil Hibernate setelah INSERT.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Id pengguna terakhir yang mengubah baris ini (jejak audit).
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null}/kosong diabaikan diam-diam (nilai lama
	 * dipertahankan), sehingga jejak audit tidak dapat dikosongkan kembali setelah pernah terisi.
	 * Dipanggil dari {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} lewat
	 * cast ke {@link GeneralValueObject}; berkat dynamic dispatch, yang terisi adalah field kelas
	 * ini (yang terpetakan), bukan field induk yang tidak dipetakan.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (jejak audit).
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir; nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@link javax.persistence.PreUpdate}: menyegarkan metadata audit
	 * ({@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)}, {@link #setOlehId(String)})
	 * tepat sebelum Hibernate menerbitkan pernyataan UPDATE.
	 *
	 * <p>Delegasinya, {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}, akan
	 * melewati pembaruan bila pembantu audit menyimpulkan tidak ada perubahan bisnis pada baris
	 * ini &mdash; jadi {@link #getTanggal_dirubah()} tidak selalu berubah pada setiap flush.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek baru karena
	 *         field diinisialisasi saat instansiasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Seluruh jawaban per butir dalam bentuk terpadatkan (lihat Javadoc kelas untuk formatnya).
	 *
	 * <p><b>Jangan tertukar</b> dengan {@link #getMasukan()}: yang ini adalah gabungan
	 * nilai+keterangan <i>per butir pertanyaan</i>, sedangkan {@code masukan} adalah satu komentar
	 * bebas untuk seluruh lembar. Nilai balik selalu sudah di-{@code trim} dan tidak pernah
	 * {@code null} &mdash; itulah yang menjaga kolom {@code nullable = false} tetap terisi walau
	 * {@link #setKeterangan(String)} sempat menerima {@code null}.</p>
	 *
	 * @return string terpadatkan jawaban, atau string kosong bila belum ada jawaban
	 */
	@Column(name = "keterangan", columnDefinition = "text", nullable = false)
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Menetapkan langsung string terpadatkan jawaban.
	 *
	 * <p>Hanya untuk Hibernate dan untuk inisialisasi baris kosong (mis.
	 * {@code AngketGuruWindow.getOrCreateChecklistGuru} memanggilnya dengan {@code ""}). Kode
	 * aplikasi yang ingin mengubah <i>satu butir</i> harus memakai
	 * {@link #setValue(Integer, Siswa, Guru, JadwalPelajaran, ChecklistPenilaianGuru, String)},
	 * karena method ini menimpa seluruh isi tanpa memvalidasi format.</p>
	 *
	 * @param keterangan string terpadatkan penuh; {@code null} diterima dan dinormalkan menjadi
	 *                   {@code ""} saat dibaca kembali
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Siswa <b>penilai</b> (pengisi angket) &mdash; bukan pihak yang dinilai.
	 *
	 * <p><b>Getter tidak murni:</b> nilai dilewatkan dulu ke
	 * {@link GeneralValueObject#check(Object)} dan hasilnya <b>ditulis balik</b> ke field. Itu
	 * mekanisme kanonikalisasi bersama seluruh model AIS (menyatukan referensi ke satu objek Java
	 * per id lewat EntityIdentityMap serta menginisialisasi proxy malas dengan aman), bukan efek
	 * samping yang tidak disengaja; tetap perlu diingat bahwa membaca properti ini dapat memicu
	 * query dan mengubah isi field.</p>
	 *
	 * @return siswa pengisi angket; secara skema tidak pernah {@code null} pada baris tersimpan
	 *         (kolom {@code nullable = false})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/**
	 * Menetapkan siswa pengisi angket.
	 *
	 * <p>Perhatikan bahwa {@link #setValue} memanggil setter ini pada <i>setiap</i> penyimpanan
	 * butir, sehingga baris yang sudah ada dapat diarahkan ulang ke siswa lain bila pemanggil
	 * mengirim siswa berbeda pada kombinasi kunci yang sama. Seluruh pemanggil yang ada mengambil
	 * siswa dari sesi/token, bukan dari parameter klien.</p>
	 *
	 * @param siswa siswa pengisi angket
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Jadwal pelajaran yang menjadi konteks penilaian &mdash; sekaligus sumber tahun ajaran,
	 * semester, mata pelajaran dan kelas yang dipakai penyaringan laporan
	 * ({@code LaporanAngketGuruDashboardWindow.matchJadwal}).
	 *
	 * <p>Getter tidak murni dengan alasan yang sama seperti {@link #getSiswa()}.</p>
	 *
	 * @return jadwal pelajaran terkait; secara skema tidak pernah {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_pelajaran", nullable = false)
	public JadwalPelajaran getJadwalPelajaran() {
		jadwalPelajaran = check(jadwalPelajaran);
		return jadwalPelajaran;
	}

	/**
	 * Menetapkan jadwal pelajaran konteks penilaian.
	 *
	 * <p>Pada jalur REST {@code AngketUtilApi} nilai ini diresolusi dari parameter mentah klien
	 * ({@code jadwalPelajaran_id}) tanpa pemeriksaan bahwa jadwal tersebut memang diikuti siswa
	 * pengisi ataupun berada di sekolah/yayasan yang sama; lihat catatan pada {@link #setValue}.</p>
	 *
	 * @param jadwalPelajaran jadwal pelajaran konteks penilaian
	 */
	public void setJadwalPelajaran(JadwalPelajaran jadwalPelajaran) {
		this.jadwalPelajaran = jadwalPelajaran;
	}

	/**
	 * Guru <b>yang dinilai</b> pada {@link #getJadwalPelajaran()} ini.
	 *
	 * <p>Satu jadwal dapat diampu lebih dari satu guru ({@code JadwalPelajaran.populateGuruBuNama()}),
	 * karena itu guru menjadi komponen kunci tersendiri: satu siswa mengisi lembar terpisah untuk
	 * tiap guru pengampu jadwal yang sama. Getter tidak murni dengan alasan yang sama seperti
	 * {@link #getSiswa()}.</p>
	 *
	 * @return guru yang dinilai; secara skema tidak pernah {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = false)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menetapkan guru yang dinilai.
	 *
	 * <p>Sama seperti {@link #setJadwalPelajaran(JadwalPelajaran)}, pada jalur REST nilai ini
	 * berasal dari parameter mentah klien ({@code guru_id}) tanpa verifikasi bahwa guru tersebut
	 * benar-benar mengampu jadwal yang dikirim.</p>
	 *
	 * @param guru guru yang dinilai
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Kunci unik turunan {@code "<idSiswa>_<idJadwalPelajaran>_<idGuru>"}, dihitung ulang dari
	 * relasi setiap kali diakses; nilai lama dikembalikan bila salah satu relasi belum tersimpan
	 * (belum ber-id).
	 *
	 * <p><b>Getter dengan efek samping tulis.</b> Selain mengembalikan nilai, method ini
	 * <b>menimpa field {@link #kodeUnik}</b> dan &mdash; karena ketiga getter relasi yang
	 * dipanggilnya juga tidak murni &mdash; dapat memicu inisialisasi proxy/query. Konsekuensi
	 * praktis: bila relasi baris yang sudah tersimpan diubah (mis. lewat {@link #setValue}),
	 * pembacaan properti ini saat <i>dirty check</i> akan menghasilkan kunci baru dan Hibernate
	 * menerbitkan UPDATE kolom {@code kode_unik} &mdash; berpotensi bentrok dengan baris lain yang
	 * sudah memakai kunci tersebut.</p>
	 *
	 * <p>Kolom dideklarasikan {@code unique = true}, tetapi itu hanya menghasilkan constraint bila
	 * skema memang dibuat/diselaraskan dari anotasi. Kode pemanggil tidak mengandalkannya: baik
	 * {@code AngketGuruWindow.getOrCreateChecklistGuru} maupun
	 * {@link Siswa#byKey(org.hibernate.Session, boolean)} membangun sendiri string kunci yang sama
	 * di sisi Java dan memakai peta dalam memori untuk mencegah baris kembar. Perhatikan pula
	 * bahwa {@link Siswa#ambilChecklistBaruPenilaianGuruOlehSiswa(org.hibernate.Session, boolean)}
	 * memakai format kunci <i>berbeda</i> ({@code "<idGuru>-<idJadwal>"}); kedua format tidak dapat
	 * dipertukarkan.</p>
	 *
	 * @return kunci unik gabungan; nilai tersimpan sebelumnya bila ada relasi yang belum ber-id
	 */
	@Column(name = "kode_unik", unique = true)
	public String getKodeUnik() {
		Siswa s = getSiswa();
		JadwalPelajaran jp = getJadwalPelajaran();
		Guru g = getGuru();
		if (s == null || s.getId() == null || jp == null || jp.getId() == null || g == null || g.getId() == null) {
			return kodeUnik;
		}
		kodeUnik = s.getId() + "_" + jp.getId() + "_" + g.getId();
		return kodeUnik;
	}

	/**
	 * Menetapkan kunci unik secara manual.
	 *
	 * <p>Praktis hanya dipakai Hibernate saat memuat baris: nilai apa pun yang ditetapkan di sini
	 * akan ditimpa {@link #getKodeUnik()} begitu ketiga relasi sudah ber-id.</p>
	 *
	 * @param kodeUnik kunci unik gabungan
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Komentar/saran teks bebas dari siswa untuk guru ini &mdash; satu nilai untuk seluruh lembar,
	 * bukan per butir pertanyaan.
	 *
	 * <p>Pengisiannya dapat diwajibkan lewat konfigurasi
	 * {@code masukan_penilaian_guru_harus_diisi} (divalidasi {@code AngketGuruWindow} sebelum
	 * jendela penilaian ditutup). Inilah kolom yang ditampilkan dasbor rekap berdampingan dengan
	 * identitas pengisi &mdash; lihat bagian privasi pada Javadoc kelas.</p>
	 *
	 * <p>Berbeda dari {@link #getKeterangan()}, nilai balik di sini <b>tidak</b> di-{@code trim};
	 * hanya {@code null} yang dinormalkan menjadi string kosong.</p>
	 *
	 * @return komentar bebas siswa, atau string kosong bila belum diisi
	 */
	@Column(name = "masukan", columnDefinition = "text", nullable = true)
	public String getMasukan() {
		return masukan == null ? "" : masukan;
	}

	/**
	 * Menetapkan komentar bebas siswa.
	 *
	 * <p><b>Berpotensi destruktif pada jalur REST.</b> {@code AngketUtilApi} memanggil setter ini
	 * tanpa syarat pada setiap penyimpanan butir, dengan nilai {@code null} bila payload tidak
	 * menyertakan atribut {@code masukan} &mdash; sehingga komentar yang sudah pernah ditulis
	 * siswa terhapus begitu klien menyimpan satu jawaban radio tanpa ikut mengirim ulang
	 * komentarnya. Jalur ZK ({@code AngketGuruWindow.onSave}) selalu mengirim isi kotak teks
	 * sehingga tidak terdampak.</p>
	 *
	 * @param masukan komentar bebas siswa; {@code null} diterima dan dibaca kembali sebagai
	 *                string kosong
	 */
	public void setMasukan(String masukan) {
		this.masukan = masukan;
	}

	/**
	 * Menghitung berapa butir pertanyaan yang sudah terisi pada lembar ini, yaitu jumlah kemunculan
	 * penanda {@code "DATA"} di dalam {@link #getKeterangan()}.
	 *
	 * <p>Dipakai layar/mobile untuk label "Telah diisi"/"Belum terisi" beserta teks
	 * "<i>n</i> dari <i>m</i> telah terisi" ({@code AngketUtilApi} dan {@code AngketGuruWindow}).
	 * Perhitungan ini aman dari teks siswa karena {@link #setValue} sudah mengganti substring
	 * {@code "DATA"}/{@code "data"} pada keterangan yang diketik siswa menjadi {@code "dat"};
	 * pencacahan sendiri bersifat peka huruf besar-kecil sehingga hanya penanda asli yang
	 * terhitung.</p>
	 *
	 * @return jumlah butir yang tersimpan pada lembar ini; {@code 0} bila belum ada jawaban
	 */
	public int count() {
		return StringUtils.countMatches(getKeterangan(), "DATA");
	}

	/**
	 * Memeriksa apakah satu butir pertanyaan tertentu sudah dijawab pada lembar ini.
	 *
	 * <p>Pemeriksaan dilakukan dengan mencari penanda {@code "DATA<id>;"} di dalam
	 * {@link #getKeterangan()}. Tanda titik koma di akhir pola penting: tanpa itu, butir ber-id
	 * {@code 12} akan ikut cocok dengan penanda butir {@code 123}.</p>
	 *
	 * <p>Perhatikan nama yang mudah tertukar: method ini <b>tidak ada hubungannya</b> dengan
	 * {@link GeneralValueObject#check(Object)} (kanonikalisasi entity) yang dipanggil getter
	 * relasi di kelas ini &mdash; keduanya hanya kebetulan bernama sama dan dibedakan oleh tipe
	 * argumennya.</p>
	 *
	 * @param checklistPenilaianGuru butir pertanyaan yang diperiksa
	 * @return {@code true} bila butir tersebut sudah punya jawaban tersimpan; {@code false} bila
	 *         belum, atau bila butir {@code null}/belum ber-id
	 */
	public boolean check(ChecklistPenilaianGuru checklistPenilaianGuru) {
		if (checklistPenilaianGuru == null || checklistPenilaianGuru.getId() == null) {
			return false;
		}
		String splBaru = "DATA" + checklistPenilaianGuru.getId() + ";";
		return StringUtils.indexOf(getKeterangan(), splBaru) > -1;
	}

	/**
	 * Membongkar seluruh isi {@link #getKeterangan()} menjadi daftar jawaban terstruktur.
	 *
	 * <p>Setiap elemen adalah {@code Object[]} berukuran 3 dengan susunan tetap:</p>
	 * <ol start="0">
	 *   <li>{@code Long} &mdash; id {@link ChecklistPenilaianGuru} (butir pertanyaan);</li>
	 *   <li>{@code Integer} &mdash; nilai/skor yang dipilih siswa;</li>
	 *   <li>{@code String} &mdash; keterangan teks per butir ({@code ""} bila tidak ada).</li>
	 * </ol>
	 *
	 * <p>Dipakai {@code AngketUtil.checkStatusChecklist} dan
	 * {@code ChecklistPenilaianGuruHelper.checkStatusChecklistGuru} untuk menyusun himpunan butir
	 * yang sudah dijawab (menentukan apakah siswa masih diwajibkan mengisi angket saat login),
	 * juga oleh {@code LaporanAngketGuruDashboardWindow} sebagai jalur cadangan agregasi nilai dan
	 * oleh {@code AngketGuruWindow} untuk menampilkan kembali jawaban atas pertanyaan yang sudah
	 * dinonaktifkan admin.</p>
	 *
	 * <p><b>Toleran terhadap data rusak:</b> pecahan yang tidak memuat penanda {@code "DATA"}
	 * dilewati, dan pecahan yang gagal diurai (id/nilai bukan angka) dibuang diam-diam &mdash;
	 * kegagalan hanya dicatat ke {@code ErrorAuditUtil}, tidak dilempar ke pemanggil. Lihat pula
	 * jebakan pemisah {@code '_'} pada Javadoc kelas.</p>
	 *
	 * @return daftar jawaban terstruktur, terurut sesuai urutan kemunculan pada kolom teks; list
	 *         kosong (bukan {@code null}) bila belum ada jawaban
	 */
	public List<Object[]> ambilValue() {
		List<Object[]> objects = new ArrayList<Object[]>();
		for (String s : StringUtils.split(getKeterangan(), "___")) {
			if (s == null || !s.contains("DATA")) {
				continue;
			}
			Object[] ss = new Object[3];
			try {
				String[] parts = s.split(";");
				ss[0] = Long.valueOf(parts[0].replaceAll("DATA", ""));
				String[] nilaiKet = parts[1].split("<>");
				ss[1] = Integer.valueOf(nilaiKet[0]);
				ss[2] = nilaiKet.length > 1 ? nilaiKet[1] : "";
				objects.add(ss);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/ChecklistBaruPenilaianGuruOlehSiswa.java:190");
			}
		}
		return objects;
	}

	/**
	 * Menuliskan (menambah atau menimpa) jawaban satu butir pertanyaan ke dalam kolom terpadatkan
	 * {@link #getKeterangan()}, sekaligus menetapkan ketiga relasi pemilik lembar ini.
	 *
	 * <p>Urutan kerjanya: (1) tolak diam-diam bila butir {@code null}/belum ber-id; (2) normalkan
	 * {@code nilai} {@code null} menjadi {@code 0}; (3) bersihkan {@code ket} dari karakter yang
	 * bermakna khusus pada format &mdash; {@code ';'} dan {@code "<>"} menjadi {@code '.'},
	 * {@code "DATA"}/{@code "data"} menjadi {@code "dat"} &mdash; agar teks siswa tidak dapat
	 * memalsukan butir jawaban lain atau mengacaukan {@link #count()}; (4) tetapkan
	 * {@link #setJadwalPelajaran}, {@link #setGuru}, {@link #setSiswa}; (5) susun ulang string
	 * terpadatkan dengan membuang entri lama butir yang sama lalu menambahkan entri baru di akhir.</p>
	 *
	 * <p><b>Efek samping yang perlu disadari:</b></p>
	 * <ul>
	 *   <li>Ketiga relasi ditimpa pada setiap pemanggilan, bukan hanya saat baris baru dibuat
	 *       &mdash; lihat catatan pada {@link #getKodeUnik()} tentang perubahan kunci pada baris
	 *       yang sudah tersimpan.</li>
	 *   <li>Field {@link #keterangan} ditulis <b>langsung</b> (bukan lewat
	 *       {@link #setKeterangan(String)}), sehingga urutan entri selalu berubah: butir yang baru
	 *       disimpan berpindah ke posisi terakhir.</li>
	 *   <li>Karakter {@code '_'} pada {@code ket} <b>tidak</b> dibersihkan; karena pemisah record
	 *       diproses sebagai himpunan karakter, keterangan bergaris bawah akan terpecah dan
	 *       pecahan yang tak berpenanda {@code "DATA"} dibuang pada pemanggilan berikutnya (lihat
	 *       Javadoc kelas).</li>
	 *   <li>Method ini <b>tidak</b> menyimpan ke basis data; pemanggil bertanggung jawab
	 *       memanggil {@code Common.refreshSaveOrUpdate} + flush/commit sendiri.</li>
	 * </ul>
	 *
	 * <p>Dipanggil dari dua tempat: {@code AngketGuruWindow.onSave} (autosave tiap perubahan radio
	 * atau keterangan di layar ZK) dan {@code AngketUtilApi} (endpoint simpan jawaban untuk klien
	 * mobile). Pada jalur REST, {@code siswa} diambil dari token sesi sehingga aman dari
	 * penyamaran identitas, tetapi {@code guru} dan {@code jadwalPelajaran} diresolusi dari id
	 * mentah kiriman klien tanpa pemeriksaan kepemilikan kelas maupun kesamaan sekolah/yayasan
	 * &mdash; artinya baris penilaian dapat dibuat terhadap guru/jadwal milik sekolah lain.</p>
	 *
	 * @param nilai                  skor pilihan siswa untuk butir ini (skala mengikuti
	 *                               {@code AngketPenilaianGuru.getJumlahPilihan()}); {@code null}
	 *                               diperlakukan sebagai {@code 0}
	 * @param siswa                  siswa pengisi; ditetapkan ke {@link #setSiswa(Siswa)}
	 * @param guru                   guru yang dinilai; ditetapkan ke {@link #setGuru(Guru)}
	 * @param jadwalPelajaran        konteks jadwal; ditetapkan ke
	 *                               {@link #setJadwalPelajaran(JadwalPelajaran)}
	 * @param checklistPenilaianGuru butir pertanyaan yang dijawab; bila {@code null} atau belum
	 *                               ber-id, seluruh pemanggilan diabaikan tanpa pesan galat
	 *                               (termasuk penetapan ketiga relasi)
	 * @param ket                    keterangan teks siswa untuk butir ini; {@code null}
	 *                               diperlakukan sebagai string kosong
	 */
	public void setValue(Integer nilai, Siswa siswa, Guru guru, JadwalPelajaran jadwalPelajaran,
			ChecklistPenilaianGuru checklistPenilaianGuru, String ket) {
		if (checklistPenilaianGuru == null || checklistPenilaianGuru.getId() == null) {
			return;
		}
		if (nilai == null) {
			nilai = Integer.valueOf(0);
		}
		ket = ket == null ? "" : ket;
		ket = org.apache.commons.lang3.StringUtils.replace(ket, ";", ".");
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "<>", ".");
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "DATA", "dat");
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "data", "dat");

		setJadwalPelajaran(jadwalPelajaran);
		setGuru(guru);
		setSiswa(siswa);

		String splBaru = "DATA" + checklistPenilaianGuru.getId() + ";";
		String newKeterangan = "";
		for (String s : StringUtils.split(getKeterangan(), "___")) {
			if (s != null && s.contains("DATA") && !s.startsWith(splBaru)) {
				newKeterangan += newKeterangan.isEmpty() ? s : "___" + s;
			}
		}
		String item = splBaru + nilai + "<>" + ket;
		newKeterangan += newKeterangan.isEmpty() ? item : "___" + item;
		keterangan = newKeterangan;
	}

	/**
	 * Membaca nilai/skor yang tersimpan untuk satu butir pertanyaan.
	 *
	 * <p>Mencari penanda {@code "DATA<id>;"} pada {@link #getKeterangan()}, memotong dari posisi
	 * itu hingga pemisah record berikutnya, lalu mengambil bagian sebelum {@code "<>"} sebagai
	 * angka. Dipakai untuk memilih radio button yang aktif saat form dibuka kembali
	 * ({@code AngketGuruWindow}) dan untuk mengisi atribut {@code nilai} pada respons JSON klien
	 * mobile ({@code AngketUtilApi}).</p>
	 *
	 * <p><b>Nilai balik menyatukan tiga keadaan berbeda menjadi satu:</b> butir tidak valid, butir
	 * belum dijawab, dan butir yang datanya gagal diurai sama-sama menghasilkan {@code 0}. Pemanggil
	 * yang perlu membedakan "belum dijawab" dari "dijawab dengan nilai 0" harus memakai
	 * {@link #check(ChecklistPenilaianGuru)} lebih dulu. Berbeda dari {@link #ambilValue()} dan
	 * {@link #getKeteranganValue(ChecklistPenilaianGuru)} yang mencatat kegagalan ke
	 * {@code ErrorAuditUtil}, kegagalan di sini dilaporkan lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)} sehingga dapat memunculkan pesan galat di
	 * layar bagi pengguna admin.</p>
	 *
	 * @param checklistPenilaianGuru butir pertanyaan yang dibaca
	 * @return skor tersimpan; {@code 0} bila butir {@code null}/belum ber-id, belum dijawab, atau
	 *         datanya tidak dapat diurai
	 */
	public Integer getValue(ChecklistPenilaianGuru checklistPenilaianGuru) {
		if (checklistPenilaianGuru == null || checklistPenilaianGuru.getId() == null) {
			return Integer.valueOf(0);
		}
		String splBaru = "DATA" + checklistPenilaianGuru.getId() + ";";
		int index = StringUtils.indexOf(getKeterangan(), splBaru);
		if (index > -1) {
			try {
				String nilai = StringUtils.substring(getKeterangan(), index);
				nilai = StringUtils.split(nilai, "___")[0];
				String[] sss = StringUtils.split(nilai, ";");
				nilai = sss[sss.length - 1].split("<>")[0];
				return Integer.valueOf(Integer.parseInt(nilai.trim()));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return Integer.valueOf(0);
	}

	/**
	 * Membaca keterangan teks yang tersimpan untuk satu butir pertanyaan (bagian setelah
	 * {@code "<>"} pada entri butir tersebut).
	 *
	 * <p>Kembaran {@link #getValue(ChecklistPenilaianGuru)} untuk sisi teks: dipakai mengisi ulang
	 * kotak keterangan per pertanyaan saat form dibuka kembali dan atribut {@code keterangan} pada
	 * respons JSON klien mobile. Hasilnya di-{@code trim}.</p>
	 *
	 * <p><b>Jangan tertukar</b> dengan {@link #getMasukan()} (komentar untuk seluruh lembar) maupun
	 * {@link #getKeterangan()} (string terpadatkan mentah). Kegagalan penguraian menghasilkan
	 * string kosong dan hanya dicatat ke {@code ErrorAuditUtil}.</p>
	 *
	 * @param checklistPenilaianGuru butir pertanyaan yang dibaca
	 * @return keterangan teks butir tersebut; string kosong bila butir {@code null}/belum ber-id,
	 *         belum dijawab, tidak diberi keterangan, atau datanya tidak dapat diurai
	 */
	public String getKeteranganValue(ChecklistPenilaianGuru checklistPenilaianGuru) {
		if (checklistPenilaianGuru == null || checklistPenilaianGuru.getId() == null) {
			return "";
		}
		String splBaru = "DATA" + checklistPenilaianGuru.getId() + ";";
		int index = StringUtils.indexOf(getKeterangan(), splBaru);
		if (index > -1) {
			try {
				String nilai = StringUtils.substring(getKeterangan(), index);
				nilai = StringUtils.split(nilai, "___")[0];
				String[] sss = StringUtils.split(nilai, ";");
				String[] nilaiKet = sss[sss.length - 1].split("<>");
				return nilaiKet.length > 1 ? nilaiKet[1].trim() : "";
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/ChecklistBaruPenilaianGuruOlehSiswa.java:259");
			}
		}
		return "";
	}

	/**
	 * Representasi teks objek: mengembalikan isi mentah kolom terpadatkan
	 * {@link #getKeterangan()}, bukan ringkasan siswa/guru seperti kebanyakan entity lain.
	 *
	 * <p><b>Perhatian saat menulis log:</b> nilai balik memuat seluruh jawaban beserta keterangan
	 * teks yang diketik siswa. Jangan mencetaknya ke log yang dapat dibaca luas &mdash; lihat
	 * bagian privasi pada Javadoc kelas.</p>
	 *
	 * @return isi {@link #getKeterangan()}; string kosong bila belum ada jawaban
	 */
	public String toString() {
		return getKeterangan();
	}
}
