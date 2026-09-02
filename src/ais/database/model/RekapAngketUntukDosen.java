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

/**
 * Baris rekapitulasi (agregat) hasil angket penilaian dosen oleh mahasiswa, versi yang
 * diperuntukkan bagi <b>konsumsi dosen</b> — tabel {@code public.rekap_angket_untuk_dosen}.
 *
 * <h3>Posisi dalam alur evaluasi dosen</h3>
 * <p>Alur evaluasi dosen di AIS terdiri atas empat lapis:</p>
 * <ol>
 *   <li><b>Definisi instrumen</b> — {@link AngketPenilaianDosen} (borang angket, dibatasi
 *   fakultas/jurusan/program/angkatan) berisi {@link GrupChecklistPenilaianDosen} (kelompok
 *   pertanyaan), yang berisi butir-butir {@link ChecklistPenilaianDosen} (pertanyaan, punya
 *   {@code bobot} dan daftar {@code pilihan}).</li>
 *   <li><b>Jawaban mentah</b> — {@link ChecklistBaruPenilaianDosenOlehMahasiswa}: satu baris per
 *   mahasiswa per perkuliahan per dosen, memuat seluruh jawaban dalam bentuk terkodekan
 *   ({@code ambilValue()} membongkarnya menjadi tripel <i>id butir, nilai pilihan 1..5,
 *   keterangan bebas</i>). Baris inilah yang memuat identitas penilai
 *   ({@code getMahasiswa()}).</li>
 *   <li><b>Rekapitulasi</b> — jawaban mentah diringkas menjadi baris-baris entity ini (dan
 *   kembarannya {@link RekapAngketDosen}), sehingga laporan tidak perlu membongkar ulang ribuan
 *   baris jawaban setiap kali dicetak.</li>
 *   <li><b>Pelaporan</b> — template JasperReports membaca tabel rekap secara langsung lewat SQL
 *   asli, bukan lewat Hibernate.</li>
 * </ol>
 *
 * <h3>Granularitas satu baris</h3>
 * <p>Satu baris mewakili kombinasi <b>{@link Dosen} &times; {@link Perkuliahan} &times; butir
 * {@link ChecklistPenilaianDosen} &times; {@link #getTahunAkademik() tahun akademik} &times;
 * {@link #getJenisSemester() jenis semester}</b>. Tidak ada <i>unique constraint</i> atas
 * kombinasi itu — keunikan sepenuhnya bergantung pada disiplin proses yang mengisinya.</p>
 *
 * <h3>Arti kolom nilai (TIDAK intuitif)</h3>
 * <p>{@link #getNilai1() nilai1}..{@link #getNilai5() nilai5} <b>bukan</b> cacah responden dan
 * <b>bukan</b> skor rata-rata, melainkan <b>akumulasi bobot butir</b> yang dikelompokkan menurut
 * opsi jawaban yang dipilih. Mengacu pada rutin rekap kembarannya
 * ({@code LaporanRekapAngketDosenPerJurusanWindow.tambahNilai()}), setiap jawaban mahasiswa
 * menambahkan {@code ChecklistPenilaianDosen.getBobot()} ke ember yang sesuai: memilih opsi 3
 * menambah bobot ke {@code nilai3}, dan seterusnya. Konsekuensinya nilai kolom-kolom ini
 * sebanding dengan <i>jumlah responden &times; bobot butir</i>, bukan dengan jumlah responden
 * saja — dua butir dengan bobot berbeda tidak bisa dibandingkan langsung.</p>
 * <p>{@link #getTotal() total} adalah akumulasi bobot seluruh respons untuk butir tersebut
 * (idealnya sama dengan jumlah {@code nilai1..nilai5}), sehingga bisa dipakai sebagai penyebut
 * saat menghitung proporsi tiap opsi. Perhatikan bahwa satu-satunya laporan yang membaca tabel
 * ini justru <b>mengabaikan</b> kolom {@code total} yang tersimpan dan menghitung ulang
 * {@code sum(nilai1+...+nilai5)} di SQL — kolom {@code total} praktis hanya data mati bagi
 * pembaca yang ada.</p>
 *
 * <h3>Hubungan dengan {@link RekapAngketDosen} — dan implikasi anonimitas</h3>
 * <p>Entity ini kembar hampir persis dengan {@link RekapAngketDosen} (tabel
 * {@code rekap_angket_dosen}): urutan field, anotasi, dan {@code serialVersionUID}-nya sama.
 * Perbedaannya ada dua, dan keduanya penting:</p>
 * <ul>
 *   <li>Entity ini <b>tidak punya kolom {@code pemilih}</b>. Pada kembarannya, {@code pemilih}
 *   diisi daftar id {@link Mahasiswa} para pengisi angket yang digabung dengan koma, dan string
 *   yang <i>sama</i> ditempelkan ke <i>setiap</i> baris rekap satu kali proses hitung ulang.
 *   Ketiadaan kolom itu di sini berarti tabel ini secara struktural <b>tidak dapat</b> membocorkan
 *   identitas mahasiswa penilai.</li>
 *   <li>Entity ini <b>tidak punya kolom {@code keterangan}</b>, yaitu gabungan komentar/masukan
 *   bebas mahasiswa (pada kembarannya dirangkai memakai pemisah penanda). Teks bebas adalah
 *   jalur de-anonimisasi yang lazim (gaya bahasa, rujukan situasi kelas), sehingga ketiadaannya
 *   memperkuat sifat anonim rekap ini.</li>
 * </ul>
 * <p>Dengan kata lain, entity ini adalah varian rekap yang <b>sudah dibersihkan dari data pribadi
 * penilai</b> — konsisten dengan namanya ("untuk dosen": aman diperlihatkan kepada objek yang
 * dinilai). Perlu dicatat bahwa {@link #getDosen() dosen} yang <i>dinilai</i> tetap tersimpan
 * penuh; yang dianonimkan adalah pihak <i>penilai</i>.</p>
 * <p><b>Batas jaminan itu perlu dipahami dengan jujur:</b> anonimitas di sini hanya berlaku pada
 * <i>tabel ini</i> dan pada satu laporan yang membacanya. Pada lapis penyimpanan aslinya tidak ada
 * anonimitas sama sekali — {@link ChecklistBaruPenilaianDosenOlehMahasiswa} menyimpan FK
 * {@code mahasiswa}, {@code dosen}, dan {@code perkuliahan} yang ketiganya
 * {@code nullable = false}, ditambah stempel waktu pengisian, tanpa pseudonimisasi maupun token
 * pemutus keterkaitan. Jadi entity ini adalah <i>bentuk sajian</i> yang anonim di atas data yang
 * secara struktural sepenuhnya dapat diatribusikan, bukan bukti bahwa angket AIS dirancang
 * anonim.</p>
 *
 * <h3>Siapa yang menulis dan membaca tabel ini</h3>
 * <p><b>Tidak ada satu baris kode Java pun</b> di codebase ini yang membuat, menyimpan, atau
 * memperbarui {@code RekapAngketUntukDosen}; kelas ini hanya terdaftar di
 * {@code hibernate.cfg.xml}. Tombol "Hitung Ulang Angket" pada layar-layar laporan angket
 * mengisi tabel <i>kembarannya</i> ({@code rekap_angket_dosen}), bukan tabel ini.</p>
 * <p>Satu-satunya pembaca adalah template JasperReports
 * {@code webapp/report/rekap_angket_dosen_per_prodi_untuk_dosen.jrxml}, yang dipanggil dari
 * {@code ais.action.report.format1.akademik.LaporanAngketDosenPerJurusanUntukDosenWindow}.
 * Kueri template itu identik kata-per-kata dengan {@code rekap_angket_dosen_per_prodi.jrxml}
 * <i>kecuali</i> nama tabel sumbernya. Selama tidak ada proses lain (job basis data, skrip ETL,
 * atau trigger di luar repositori ini) yang menyalin isi {@code rekap_angket_dosen} ke
 * {@code rekap_angket_untuk_dosen}, laporan versi dosen tersebut akan selalu kosong sementara
 * laporan versi admin terisi — perbedaan yang mudah salah dibaca sebagai "belum ada yang mengisi
 * angket".</p>
 * <p>Karena terpetakan Hibernate, entity ini tetap terjangkau oleh endpoint reflektif generik
 * ({@code /Data}, {@code /Api dataRinci}) meskipun tidak punya layar master maupun kode pemanggil
 * — "tidak dipakai" tidak berarti "tidak terjangkau".</p>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ul>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *   {@link #onUpdate()}. Deklarasi ulang properti warisan, lihat catatan di bawah.</li>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)} dan {@link #toString()}.</li>
 *   <li><b>Relasi (dimensi rekap)</b> — {@link #getDosen()},
 *   {@link #getChecklistPenilaianDosen()}, {@link #getPerkuliahan()}; ketiganya
 *   {@code ManyToOne} lazy dan memakai pola resolusi {@code check()}.</li>
 *   <li><b>Dimensi skalar</b> — {@link #getTahunAkademik()}, {@link #getJenisSemester()}.</li>
 *   <li><b>Ukuran (measure)</b> — {@link #getNilai1()}..{@link #getNilai5()} dan
 *   {@link #getTotal()}.</li>
 * </ul>
 *
 * <h3>Catatan teknis yang tidak terlihat dari kode</h3>
 * <ul>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} bukan
 *   duplikasi yang keliru, melainkan KEHARUSAN TEKNIS.</b>
 *   {@link ais.database.model.GeneralValueObject} adalah POJO abstrak biasa — bukan
 *   {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate sama sekali tidak
 *   memetakan properti yang dideklarasikan di sana. Properti apa pun yang ingin disimpan ke
 *   kolom harus dideklarasikan ulang di kelas turunan seperti di sini.</li>
 *   <li><b>Komentar generator "Bank generated by hbm2java" salah.</b> Blok itu tersalin dari
 *   {@code Bank.java} (satu-satunya file tempat komentar tersebut benar) ke puluhan entity lain
 *   sepanjang sejarah proyek; abaikan sebagai penanda asal-usul.</li>
 *   <li>{@code serialVersionUID} {@code 2463821577548439808L} dipakai bersama oleh banyak entity
 *   AIS lain (termasuk {@link RekapAngketDosen} dan {@link AngketPenilaianDosen}). Nilainya tidak
 *   membedakan kelas apa pun.</li>
 *   <li>Kelas dianotasi {@link Audited}, sehingga Envers menyalin setiap versi baris ke tabel
 *   bayangan {@code rekap_angket_untuk_dosen_aud}. Salinan itu bertahan meski baris aslinya
 *   dihapus.</li>
 *   <li>{@code dynamicInsert}/{@code dynamicUpdate} aktif: Hibernate hanya menyertakan kolom yang
 *   benar-benar terisi/berubah dalam pernyataan SQL-nya.</li>
 *   <li>Tidak ada callback {@code @PrePersist} — hanya {@link #onUpdate()} ({@code @PreUpdate}).
 *   Baris yang dibuat lalu tidak pernah diubah tidak akan punya jejak pembuat pada kolom
 *   {@code oleh}/{@code olehId}.</li>
 * </ul>
 *
 * @see RekapAngketDosen
 * @see ChecklistBaruPenilaianDosenOlehMahasiswa
 * @see ChecklistPenilaianDosen
 * @see AngketPenilaianDosen
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "rekap_angket_untuk_dosen")

public class RekapAngketUntukDosen extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini identik dengan puluhan entity AIS lain (hasil salin-tempel
	 * kerangka entity), jadi tidak bisa dipakai untuk membedakan kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code IDENTITY} tabel {@code rekap_angket_untuk_dosen}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi otomatis oleh {@link #onUpdate()}. */
	private String olehId;

	/**
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila baris belum
	 *         pernah di-update
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah. Nilai {@code null} atau yang hanya berisi spasi
	 * <b>diabaikan diam-diam</b> dan nilai lama dipertahankan — akibatnya kolom ini tidak pernah
	 * bisa dikosongkan kembali lewat setter.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan</b> dan nilai lama dipertahankan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila baris belum
	 *         pernah di-update
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari pengguna sesi berjalan lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum baris
	 * di-UPDATE. Tidak ada padanan {@code @PrePersist}, jadi pembuat baris tidak tercatat di
	 * kolom-kolom ini (lihat javadoc kelas). Pada baris deklarasi yang sama juga dideklarasikan
	 * field {@code tanggal_dirubah}, yang diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga baris baru tetap punya stempel waktu
	 * meski belum pernah di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Berbeda dari {@link #setOleh(String)}, setter ini
	 * <b>tidak</b> menolak {@code null} — memanggilnya dengan {@code null} akan mengosongkan
	 * kolom. Nilai apa pun yang disetel di sini akan ditimpa oleh {@link #onUpdate()} pada UPDATE
	 * berikutnya.
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir (presisi {@code TIMESTAMP}); untuk objek yang baru
	 *         dibuat berisi waktu server saat instansiasi, bukan {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas berformat
	 * {@code id-dosen-checklistPenilaianDosen-tahunAkademik-jenisSemester}, dipakai untuk log dan
	 * penelusuran.
	 *
	 * <p><b>Perhatian:</b> method ini membaca <b>field</b> {@code dosen} dan
	 * {@code checklistPenilaianDosen} secara langsung, <i>bukan</i> lewat {@link #getDosen()} /
	 * {@link #getChecklistPenilaianDosen()}, sehingga pola resolusi {@code check()} dilewati.
	 * Bila objek ini sudah lepas dari {@code Session} Hibernate yang memuatnya, perangkaian
	 * String akan memanggil {@code toString()} pada proxy lazy yang belum terinisialisasi — yang
	 * dapat melempar {@code LazyInitializationException} dari dalam {@code toString()} itu
	 * sendiri, atau (bila proxy sempat diinisialisasi) memicu query tambahan. Karena itu jangan
	 * memakai method ini di jalur penanganan error yang harus dijamin tidak melempar exception.
	 * Field {@link #getPerkuliahan() perkuliahan} sengaja/kebetulan tidak ikut dicetak, padahal ia
	 * termasuk dimensi kunci baris ini — dua baris untuk perkuliahan berbeda menghasilkan teks
	 * yang tidak terbedakan.</p>
	 *
	 * @return ringkasan lima dimensi baris ini sebagai satu String bertanda pisah tanda hubung
	 */
	public String toString() {
		return id + "-" + dosen + "-" + checklistPenilaianDosen + "-" + tahunAkademik + "-" + jenisSemester;
	}

	/** Dosen yang <b>dinilai</b> pada baris rekap ini (bukan pihak penilai). */
	private Dosen dosen;
	/** Kelas/perkuliahan tempat penilaian berlangsung; menentukan jurusan, program, dan periode. */
	private Perkuliahan perkuliahan;
	/** Butir pertanyaan angket yang direkap oleh baris ini. */
	private ChecklistPenilaianDosen checklistPenilaianDosen;
	/** Tahun akademik periode angket, mis. {@code "2014/2015"}; disalin dari perkuliahan. */
	private String tahunAkademik;
	/** Jenis semester periode angket ({@code "Ganjil"}/{@code "Genap"}); disalin dari perkuliahan. */
	private String jenisSemester;
	/** Akumulasi bobot untuk responden yang memilih opsi jawaban ke-1. */
	private Integer nilai1;
	/** Akumulasi bobot untuk responden yang memilih opsi jawaban ke-2. */
	private Integer nilai2;
	/** Akumulasi bobot untuk responden yang memilih opsi jawaban ke-3. */
	private Integer nilai3;
	/** Akumulasi bobot untuk responden yang memilih opsi jawaban ke-4. */
	private Integer nilai4;
	/** Akumulasi bobot untuk responden yang memilih opsi jawaban ke-5. */
	private Integer nilai5;
	/** Akumulasi bobot seluruh respons untuk butir ini; berperan sebagai penyebut proporsi. */
	private Integer total;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Seluruh field dibiarkan
	 * {@code null} kecuali {@code tanggal_dirubah} yang langsung berisi waktu server.
	 */
	public RekapAngketUntukDosen() {
	}

	/**
	 * @return primary key baris ini, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Kolom {@code id} dipetakan {@code insertable = false} dan dibangkitkan
	 * basis data ({@code IDENTITY}), sehingga nilai yang disetel manual tidak akan ikut disertakan
	 * pada INSERT — setter ini praktis hanya dipakai oleh Hibernate saat memuat baris.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan dosen yang <b>dinilai</b> pada baris rekap ini.
	 *
	 * <p>Relasi ini {@code FetchType.LAZY}, karena itu getter memakai pola standar entity AIS:
	 * hasil {@code check()} <b>ditugaskan kembali ke field</b> sebelum dikembalikan. Efek samping
	 * yang perlu disadari: (a) field {@code dosen} bisa berganti menunjuk instance <i>lain</i>
	 * (instance kanonik dari {@code EntityIdentityMap}, dari cache, atau hasil reload) — jadi
	 * getter ini memang menulis ke state objek, meski tidak menulis apa pun ke basis data; dan
	 * (b) bila objek sudah <i>detached</i>, {@code check()} dapat membuka {@code Session}
	 * Hibernate <b>miliknya sendiri</b> untuk memuat ulang dan menutupnya kembali di
	 * {@code finally} — sesi milik pemanggil tidak pernah disentuh maupun ditutup. Resolusi yang
	 * gagal bersifat senyap: {@code check()} tidak pernah melempar exception dan tidak pernah
	 * mengubah argumen non-null menjadi {@code null}, sehingga getter ini <b>tidak destruktif</b>.
	 *
	 * @return dosen yang dinilai, atau {@code null} bila kolom {@code dosen} kosong
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menyetel dosen yang dinilai. Karena {@code cascade} mencakup {@code PERSIST}/{@code MERGE},
	 * menyimpan baris rekap ini juga akan mem-persist/merge objek dosen yang belum tersimpan.
	 *
	 * @param dosen dosen yang dinilai; boleh {@code null} (kolom nullable)
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan butir pertanyaan angket yang direkap baris ini. Butir inilah pemilik
	 * {@code bobot} yang diakumulasikan ke {@link #getNilai1() nilai1}..{@link #getNilai5()
	 * nilai5}, sekaligus pemilik {@code grupChecklistPenilaianDosen} yang dipakai laporan untuk
	 * mengelompokkan baris.
	 *
	 * <p>Sama seperti {@link #getDosen()}, getter ini menulis balik hasil {@code check()} ke
	 * field (bukan ke basis data) dan dapat memicu pembukaan sesi Hibernate sementara di dalam
	 * {@code check()}; tidak destruktif.</p>
	 *
	 * @return butir checklist yang direkap, atau {@code null} bila kolom
	 *         {@code checklist_penilaian_dosen} kosong
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "checklist_penilaian_dosen", nullable = true)
	public ChecklistPenilaianDosen getChecklistPenilaianDosen() {
		checklistPenilaianDosen = check(checklistPenilaianDosen);
		return checklistPenilaianDosen;
	}

	/**
	 * Menyetel butir pertanyaan angket yang direkap baris ini. {@code cascade}
	 * {@code PERSIST}/{@code MERGE} berlaku seperti pada {@link #setDosen(Dosen)}.
	 *
	 * @param checklistPenilaianDosen butir checklist; boleh {@code null}
	 */
	public void setChecklistPenilaianDosen(ChecklistPenilaianDosen checklistPenilaianDosen) {
		this.checklistPenilaianDosen = checklistPenilaianDosen;
	}

	/**
	 * Mengembalikan tahun akademik periode angket, mis. {@code "2014/2015"}. Nilainya redundan
	 * terhadap {@code getPerkuliahan().getTahunAjaran()} dan disalin saat rekap dibentuk, agar
	 * laporan bisa memfilter periode tanpa menjoin tabel perkuliahan.
	 *
	 * @return tahun akademik apa adanya (tanpa normalisasi), atau {@code null} bila kolom kosong
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Menyetel tahun akademik periode angket.
	 *
	 * @param tahunAkademik tahun akademik, mis. {@code "2014/2015"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan jenis semester periode angket ({@code "Ganjil"}/{@code "Genap"}). Seperti
	 * {@link #getTahunAkademik()}, nilainya salinan dari
	 * {@code getPerkuliahan().getGanjilGenap()}.
	 *
	 * @return jenis semester apa adanya, atau {@code null} bila kolom kosong
	 */
	public String getJenisSemester() {
		return jenisSemester;
	}

	/**
	 * Menyetel jenis semester periode angket.
	 *
	 * @param jenisSemester {@code "Ganjil"} atau {@code "Genap"}
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	/**
	 * Mengembalikan akumulasi bobot untuk opsi jawaban ke-1 pada butir ini.
	 *
	 * <p><b>Berbeda dari kembarannya</b> {@code RekapAngketDosen.getNilai1()} yang memetakan
	 * {@code null} menjadi {@code 0}, getter ini mengembalikan {@code null} apa adanya. Pemanggil
	 * Java yang melakukan <i>unboxing</i> langsung (mis. penjumlahan aritmetika) akan terkena
	 * {@code NullPointerException} untuk baris yang kolom nilainya belum terisi. Laporan yang ada
	 * tidak terkena masalah ini karena menjumlahkan lewat {@code sum()} di SQL, yang mengabaikan
	 * {@code NULL}.</p>
	 *
	 * @return akumulasi bobot opsi ke-1, atau {@code null} bila kolom belum pernah diisi
	 */
	public Integer getNilai1() {
		return nilai1;
	}

	/**
	 * Menyetel akumulasi bobot untuk opsi jawaban ke-1.
	 *
	 * @param nilai1 akumulasi bobot; boleh {@code null}
	 */
	public void setNilai1(Integer nilai1) {
		this.nilai1 = nilai1;
	}

	/**
	 * Mengembalikan akumulasi bobot untuk opsi jawaban ke-2 pada butir ini. Perilaku {@code null}
	 * sama seperti {@link #getNilai1()} (tidak dinormalisasi menjadi {@code 0}).
	 *
	 * @return akumulasi bobot opsi ke-2, atau {@code null} bila kolom belum pernah diisi
	 */
	public Integer getNilai2() {
		return nilai2;
	}

	/**
	 * Menyetel akumulasi bobot untuk opsi jawaban ke-2.
	 *
	 * @param nilai2 akumulasi bobot; boleh {@code null}
	 */
	public void setNilai2(Integer nilai2) {
		this.nilai2 = nilai2;
	}

	/**
	 * Mengembalikan akumulasi bobot untuk opsi jawaban ke-3 pada butir ini. Perilaku {@code null}
	 * sama seperti {@link #getNilai1()} (tidak dinormalisasi menjadi {@code 0}).
	 *
	 * @return akumulasi bobot opsi ke-3, atau {@code null} bila kolom belum pernah diisi
	 */
	public Integer getNilai3() {
		return nilai3;
	}

	/**
	 * Menyetel akumulasi bobot untuk opsi jawaban ke-3.
	 *
	 * @param nilai3 akumulasi bobot; boleh {@code null}
	 */
	public void setNilai3(Integer nilai3) {
		this.nilai3 = nilai3;
	}

	/**
	 * Mengembalikan akumulasi bobot untuk opsi jawaban ke-4 pada butir ini. Perilaku {@code null}
	 * sama seperti {@link #getNilai1()} (tidak dinormalisasi menjadi {@code 0}).
	 *
	 * @return akumulasi bobot opsi ke-4, atau {@code null} bila kolom belum pernah diisi
	 */
	public Integer getNilai4() {
		return nilai4;
	}

	/**
	 * Menyetel akumulasi bobot untuk opsi jawaban ke-4.
	 *
	 * @param nilai4 akumulasi bobot; boleh {@code null}
	 */
	public void setNilai4(Integer nilai4) {
		this.nilai4 = nilai4;
	}

	/**
	 * Mengembalikan akumulasi bobot untuk opsi jawaban ke-5 pada butir ini. Perilaku {@code null}
	 * sama seperti {@link #getNilai1()} (tidak dinormalisasi menjadi {@code 0}).
	 *
	 * @return akumulasi bobot opsi ke-5, atau {@code null} bila kolom belum pernah diisi
	 */
	public Integer getNilai5() {
		return nilai5;
	}

	/**
	 * Menyetel akumulasi bobot untuk opsi jawaban ke-5.
	 *
	 * @param nilai5 akumulasi bobot; boleh {@code null}
	 */
	public void setNilai5(Integer nilai5) {
		this.nilai5 = nilai5;
	}

	/**
	 * Mengembalikan akumulasi bobot seluruh respons untuk butir ini — idealnya sama dengan jumlah
	 * {@link #getNilai1() nilai1}..{@link #getNilai5() nilai5}, sehingga cocok dipakai sebagai
	 * penyebut saat menghitung proporsi tiap opsi.
	 *
	 * <p>Konsistensi itu tidak ditegakkan di mana pun: tidak ada constraint basis data maupun
	 * validasi Java yang memastikan {@code total} sama dengan jumlah kelima kolom nilai. Satu-
	 * satunya laporan pembaca tabel ini pun mengabaikan kolom {@code total} dan menghitung
	 * ulang jumlahnya di SQL. Perilaku {@code null} sama seperti {@link #getNilai1()}.</p>
	 *
	 * @return akumulasi bobot seluruh respons, atau {@code null} bila kolom belum pernah diisi
	 */
	public Integer getTotal() {
		return total;
	}

	/**
	 * Menyetel akumulasi bobot seluruh respons untuk butir ini.
	 *
	 * @param total akumulasi bobot total; boleh {@code null}
	 */
	public void setTotal(Integer total) {
		this.total = total;
	}

	/**
	 * Mengembalikan perkuliahan (kelas) tempat penilaian berlangsung. Lewat objek inilah laporan
	 * menurunkan jurusan, fakultas, program, masa perkuliahan, dan status semester pendek yang
	 * dipakai sebagai filter — perhatikan bahwa {@code perkuliahan} adalah satu-satunya jalan
	 * menuju dimensi-dimensi tersebut, sehingga baris dengan {@code perkuliahan} kosong akan
	 * tersaring habis oleh {@code inner join} di kueri laporan.
	 *
	 * <p>Sama seperti {@link #getDosen()}, getter ini menulis balik hasil {@code check()} ke field
	 * (bukan ke basis data) dan dapat memicu pembukaan sesi Hibernate sementara di dalam
	 * {@code check()}; tidak destruktif.</p>
	 *
	 * @return perkuliahan terkait, atau {@code null} bila kolom {@code perkuliahan} kosong
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perkuliahan", nullable = true)
	public Perkuliahan getPerkuliahan() {
		perkuliahan = check(perkuliahan);
		return perkuliahan;
	}

	/**
	 * Menyetel perkuliahan tempat penilaian berlangsung. {@code cascade}
	 * {@code PERSIST}/{@code MERGE} berlaku seperti pada {@link #setDosen(Dosen)}.
	 *
	 * @param perkuliahan perkuliahan terkait; boleh {@code null}
	 */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

}
