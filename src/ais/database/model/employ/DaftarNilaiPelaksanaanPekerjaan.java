package ais.database.model.employ;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;



/**
 * Entity JPA/Hibernate untuk daftar nilai pelaksanaan pekerjaan (dikenal luas sebagai DP3) —
 * dokumen evaluasi kinerja tahunan pegawai yang mencatat delapan unsur penilaian standar
 * kepegawaian: kesetiaan, prestasi kerja, tanggung jawab, ketaatan, kejujuran, kerjasama,
 * prakarsa, dan kepemimpinan. Setiap unsur membawa tiga komponen paralel: nilai angka
 * ({@code Double}, mis. {@link #kesetiaan}), nilai sebutan/predikat tekstual (mis. "Amat Baik",
 * mis. {@link #sebutankesetiaan}), dan keterangan naratif tambahan (mis. {@link
 * #keterangankesetiaan}). Baris {@link #jumlah} dan {@link #rataRata} menampung agregat (total
 * dan rata-rata) dari kedelapan unsur tersebut, masing-masing dengan sebutan dan keterangan
 * sendiri ({@link #sebutanjumlah}/{@link #keteranganjumlah}, {@link #sebutanrataRata}/{@link
 * #keteranganrataRata}).
 *
 * <p><b>Pihak yang terlibat dalam penilaian:</b> tiga referensi {@link Pegawai} menandai peran —
 * {@link #yangDinilai} (pegawai yang dinilai), {@link #penilai} (pejabat penilai, lazimnya atasan
 * langsung pegawai yang dinilai), dan {@link #atasanPenilai} (atasan dari pejabat penilai, yang
 * secara normatif mengesahkan hasil penilaian atasan langsung). Ketiganya murni kolom data
 * (foreign key {@code nullable = true}, tanpa validasi kepemilikan atau hak akses saat disimpan)
 * — lihat catatan gerbang approval di bawah.
 *
 * <p><b>PENTING — tidak ada gerbang approval/finalisasi pada entity maupun tabelnya:</b> tidak
 * ada kolom status (draft/final/disetujui) ataupun flag lock di {@code
 * employ.daftar_nilai_pelaksanaan_pekerjaan}. Satu-satunya pemakai entity ini di luar DAO, {@code
 * ais.action.master.employ.DaftarNilaiPelaksanaanPekerjaanAction}, mengizinkan siapa pun yang
 * memegang privilese {@code UPDATE} pada menu ini untuk membuka kembali dan mengubah SELURUH
 * field nilai/sebutan/keterangan kapan saja setelah data tersimpan — termasuk baris milik pegawai
 * lain, dan tanpa batas waktu setelah periode penilaian berlalu — tanpa pengecekan bahwa {@code
 * penilai} yang sedang login adalah pejabat penilai yang sah untuk baris tersebut, dan tanpa
 * pengecekan status pengesahan oleh {@link #atasanPenilai} (karena memang tidak direpresentasikan
 * di skema data). Controller tersebut pada dasarnya menyediakan CRUD penuh berbasis privilese menu
 * semata, bukan berbasis peran/kepemilikan atas baris data. Validasi rentang nilai (0–100) hanya
 * diterapkan di listener UI untuk kolom {@code kesetiaan} ({@code
 * DaftarNilaiPelaksanaanPekerjaanAction.init(...)}, listener {@code onChange} pada Doublebox
 * kesetiaan); ketujuh unsur nilai lainnya ({@link #prestasiKerja}, {@link #tanggungJawab}, {@link
 * #ketaatan}, {@link #kejujuran}, {@link #kerjasama}, {@link #prakarsa}, {@link #kepimpinan}) —
 * beserta {@link #jumlah} dan {@link #rataRata} — tidak memiliki listener/validasi rentang di UI
 * maupun constraint di level database/entity; nilai berapa pun, termasuk negatif atau di atas 100,
 * dapat tersimpan tanpa penolakan.
 *
 * <p><b>Relasi terhadap kenaikan pangkat/kenaikan gaji berkala — diverifikasi TIDAK ADA secara
 * struktural:</b> penelusuran kode ({@code grep} atas {@code ais.database.model.employ.KenaikanPangkat}
 * dan {@code ais.database.model.employ.KenaikanGajiBerkala}) tidak menemukan foreign key, query,
 * maupun referensi apa pun dari kedua kelas tersebut ke {@link DaftarNilaiPelaksanaanPekerjaan},
 * maupun sebaliknya. Entity ini berdiri sendiri (hanya dipakai oleh {@code
 * DaftarNilaiPelaksanaanPekerjaanAction} dan DAO-nya, {@code DaftarNilaiPelaksanaanPekerjaanDao}).
 * Secara konseptual, dalam praktik birokrasi kepegawaian nyata DP3/SKP memang lazim menjadi salah
 * satu syarat administratif kenaikan pangkat maupun kenaikan gaji berkala, tetapi di codebase AIS
 * ini TIDAK ADA gerbang otomatis pada proses kenaikan pangkat/KGB yang membaca, mensyaratkan, atau
 * memvalidasi keberadaan maupun nilai baris {@link DaftarNilaiPelaksanaanPekerjaan} — pengecekan
 * semacam itu, bila memang dilakukan secara organisasi, murni manual/di luar sistem ini. Dengan
 * demikian pola bypass approval SK kenaikan pangkat yang tercatat pada {@code task_b62255d9} (SK
 * kenaikan pangkat belum disetujui tetap dapat menggerakkan gaji) TIDAK berulang secara langsung
 * di file ini karena tidak ada jalur struktural yang menghubungkan keduanya. Risiko yang berdiri
 * sendiri pada file ini adalah ketiadaan gerbang finalisasi pada dokumen penilaian kinerja itu
 * sendiri — ini memperkuat pola arsitektur berulang "gerbang approval tanpa cek kepemilikan/status"
 * yang sudah tercatat pada modul-modul kepegawaian lain, bukan merupakan varian baru dari pola
 * bypass otomatis {@code task_b62255d9}.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code String nama}, {@code Date tanggal_dirubah}, {@code Pegawai
 * yangDinilai}, {@code Pegawai penilai}; pemetaan persistence: tabel {@code
 * employ.daftar_nilai_pelaksanaan_pekerjaan}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getKeterangan()}, {@code getNama()}); mutasi data ({@code
 * setOlehId()}, {@code onUpdate()}, {@code setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code
 * setKeterangan()}); operasi domain lain ({@code toString()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see ais.action.master.employ.DaftarNilaiPelaksanaanPekerjaanAction
 * @see ais.database.dao.employ.DaftarNilaiPelaksanaanPekerjaanDao
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "daftar_nilai_pelaksanaan_pekerjaan")



public class DaftarNilaiPelaksanaanPekerjaan extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	/** Primary key surrogate (identity) baris daftar nilai pelaksanaan pekerjaan. */
	private Long id;
	/**
	 * Nama/identitas pengguna yang terakhir menyimpan/mengubah baris ini, ditulis otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor} sebagai bagian dari mekanisme
	 * audit shadow bersama {@link #olehId} dan {@link #tanggal_dirubah}. Bukan field yang diisi
	 * manual oleh pengguna maupun ditampilkan sebagai bagian dari form penilaian di {@code
	 * DaftarNilaiPelaksanaanPekerjaanAction}.
	 */
	private String oleh;
	/**
	 * Id pengguna yang terakhir menyimpan/mengubah baris ini, pasangan {@link #oleh} pada
	 * mekanisme audit shadow yang sama. Diisi otomatis oleh interceptor; lihat {@link
	 * #setOlehId(String)} untuk guard nilai kosong/null.
	 */
	private String olehId;

	/**
	 * Accessor untuk {@link #olehId} — id pengguna terakhir yang menyimpan/mengubah baris audit
	 * ini. Nilai diisi oleh {@code AuditTimestampInterceptor} pada saat insert/update, bukan oleh
	 * form penilaian.
	 *
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi
	 */
	public String getOlehId() {return olehId;}
	/**
	 * Mutator untuk {@link #olehId} dengan guard: nilai {@code null} atau string kosong/whitespace
	 * diabaikan sepenuhnya (field lama dipertahankan) agar interceptor audit tidak pernah
	 * menimpa id pengguna yang sudah tercatat dengan nilai kosong.
	 *
	 * @param olehId id pengguna yang akan dicatat; diabaikan bila {@code null} atau kosong setelah
	 *               di-{@code trim()}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}
	/**
	 * Catatan bebas mengenai baris penilaian ini. Juga dipakai sebagai representasi string entity
	 * lewat {@link #toString()} (mis. untuk label pada komponen {@code RevisiHelper}).
	 */
	private String keterangan;
	/** Nama/label bebas untuk baris daftar nilai pelaksanaan pekerjaan ini. */
	private String nama;
	/**
	 * Hook lifecycle JPA yang dipanggil sebelum setiap operasi UPDATE, mendelegasikan ke {@code
	 * ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} untuk memutakhirkan metadata
	 * audit ({@link #oleh}, {@link #olehId}, {@link #tanggal_dirubah}) entity ini. Lihat javadoc
	 * {@link #onUpdate()} untuk detail lengkap.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Timestamp perubahan terakhir baris ini, diinisialisasi ke waktu saat instance dibuat lewat
	 * {@code ais.ui.util.WaktuUtil.getDate()} dan dimutakhirkan otomatis oleh {@link #onUpdate()}
	 * pada setiap update melalui {@code AuditTimestampInterceptor}. Bukan bagian dari data
	 * penilaian kinerja itu sendiri (kesetiaan/prestasi kerja/dst.) — murni metadata audit.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Pegawai yang dinilai — subjek dari dokumen penilaian kinerja ini. Relasi {@code
	 * @ManyToOne} nullable tanpa validasi bahwa pemanggil berwenang menilai pegawai tersebut;
	 * pengecekan wewenang sepenuhnya bergantung pada privilese menu ({@code
	 * CommonPrivilages.CREATE}/{@code UPDATE}) di {@code DaftarNilaiPelaksanaanPekerjaanAction},
	 * bukan pada relasi struktural/organisasi pegawai.
	 */
	private Pegawai yangDinilai;
	/**
	 * Pejabat penilai — lazimnya atasan langsung dari {@link #yangDinilai} yang mengisi nilai pada
	 * baris ini. Sama seperti {@link #yangDinilai}, relasi ini murni kolom data; sistem tidak
	 * memvalidasi bahwa pegawai yang sedang login adalah {@code penilai} yang tercatat di baris
	 * ini sebelum mengizinkan perubahan nilai (lihat catatan gerbang approval pada javadoc kelas).
	 */
	private Pegawai penilai;
	/**
	 * Atasan dari pejabat penilai — pihak yang secara normatif mengesahkan/menyetujui hasil
	 * penilaian {@link #penilai}. Tidak ada representasi status "telah disahkan" di skema data;
	 * field ini murni kolom referensi pegawai, bukan gerbang approval fungsional. Lihat catatan
	 * "tidak ada gerbang approval/finalisasi" pada javadoc kelas.
	 */
	private Pegawai atasanPenilai;

	/** Nilai angka (0–100 menurut konvensi UI, namun TIDAK divalidasi di level entity/DB) untuk unsur "Kesetiaan". */
	private Double kesetiaan;
	/** Nilai angka untuk unsur "Prestasi Kerja"; tidak ada validasi rentang di UI maupun entity/DB. */
	private Double prestasiKerja;
	/** Nilai angka untuk unsur "Tanggung Jawab"; tidak ada validasi rentang di UI maupun entity/DB. */
	private Double tanggungJawab;
	/** Nilai angka untuk unsur "Ketaatan"; tidak ada validasi rentang di UI maupun entity/DB. */
	private Double ketaatan;
	/** Nilai angka untuk unsur "Kejujuran"; tidak ada validasi rentang di UI maupun entity/DB. */
	private Double kejujuran;
	/** Nilai angka untuk unsur "Kerjasama"; tidak ada validasi rentang di UI maupun entity/DB. */
	private Double kerjasama;
	/** Nilai angka untuk unsur "Prakarsa"; tidak ada validasi rentang di UI maupun entity/DB. */
	private Double prakarsa;
	/** Nilai angka untuk unsur "Kepemimpinan"; tidak ada validasi rentang di UI maupun entity/DB. */
	private Double kepimpinan;
	/** Total (jumlah) nilai angka kedelapan unsur penilaian; dihitung/diisi manual lewat form UI, bukan formula otomatis pada entity ini. */
	private Double jumlah;
	/** Rata-rata nilai angka kedelapan unsur penilaian; sama seperti {@link #jumlah}, diisi manual lewat form UI tanpa formula otomatis pada entity ini. */
	private Double rataRata;

	/** Nilai sebutan/predikat tekstual (mis. "Amat Baik") untuk unsur "Kesetiaan", pasangan dari {@link #kesetiaan}. */
	private String sebutankesetiaan;
	/** Nilai sebutan/predikat tekstual untuk unsur "Prestasi Kerja", pasangan dari {@link #prestasiKerja}. */
	private String sebutanprestasiKerja;
	/** Nilai sebutan/predikat tekstual untuk unsur "Tanggung Jawab", pasangan dari {@link #tanggungJawab}. */
	private String sebutantanggungJawab;
	/** Nilai sebutan/predikat tekstual untuk unsur "Ketaatan", pasangan dari {@link #ketaatan}. */
	private String sebutanketaatan;
	/** Nilai sebutan/predikat tekstual untuk unsur "Kejujuran", pasangan dari {@link #kejujuran}. */
	private String sebutankejujuran;
	/** Nilai sebutan/predikat tekstual untuk unsur "Kerjasama", pasangan dari {@link #kerjasama}. */
	private String sebutankerjasama;
	/** Nilai sebutan/predikat tekstual untuk unsur "Prakarsa", pasangan dari {@link #prakarsa}. */
	private String sebutanprakarsa;
	/** Nilai sebutan/predikat tekstual untuk unsur "Kepemimpinan", pasangan dari {@link #kepimpinan}. */
	private String sebutankepimpinan;
	/** Nilai sebutan/predikat tekstual untuk baris {@link #jumlah}. */
	private String sebutanjumlah;
	/** Nilai sebutan/predikat tekstual untuk baris {@link #rataRata}. */
	private String sebutanrataRata;

	/** Keterangan naratif tambahan untuk unsur "Kesetiaan", pasangan dari {@link #kesetiaan}/{@link #sebutankesetiaan}. */
	private String keterangankesetiaan;
	/** Keterangan naratif tambahan untuk unsur "Prestasi Kerja". */
	private String keteranganprestasiKerja;
	/** Keterangan naratif tambahan untuk unsur "Tanggung Jawab". */
	private String keterangantanggungJawab;
	/** Keterangan naratif tambahan untuk unsur "Ketaatan". */
	private String keteranganketaatan;
	/** Keterangan naratif tambahan untuk unsur "Kejujuran". */
	private String keterangankejujuran;
	/** Keterangan naratif tambahan untuk unsur "Kerjasama". */
	private String keterangankerjasama;
	/** Keterangan naratif tambahan untuk unsur "Prakarsa". */
	private String keteranganprakarsa;
	/** Keterangan naratif tambahan untuk unsur "Kepemimpinan". */
	private String keterangankepimpinan;
	/** Keterangan naratif tambahan untuk baris {@link #jumlah}. */
	private String keteranganjumlah;
	/** Keterangan naratif tambahan untuk baris {@link #rataRata}. */
	private String keteranganrataRata;

	/**
	 * Accessor primary key. Nilai {@code null} menandakan entity belum pernah dipersist (baris
	 * baru yang belum {@code save()}).
	 *
	 * @return id baris ini, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mutator primary key. Kolom dipetakan {@code insertable = false} sehingga nilai ini hanya
	 * relevan untuk keperluan Hibernate internal (mis. pemetaan hasil query) — id sesungguhnya
	 * dihasilkan basis data lewat strategi {@code IDENTITY} saat insert.
	 *
	 * @param id nilai id yang hendak diset pada instance ini
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mutator untuk {@link #oleh} dengan guard: nilai {@code null} atau string kosong/whitespace
	 * diabaikan sepenuhnya sehingga nama pengguna audit yang sudah tercatat tidak pernah tertimpa
	 * nilai kosong.
	 *
	 * @param oleh nama/identitas pengguna yang akan dicatat; diabaikan bila {@code null} atau
	 *             kosong setelah di-{@code trim()}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Accessor untuk {@link #oleh} — nama/identitas pengguna terakhir yang menyimpan/mengubah
	 * baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Mutator untuk {@link #tanggal_dirubah}. Nilai normalnya dimutakhirkan otomatis lewat {@link
	 * #onUpdate()}; pemanggilan manual method ini di luar interceptor audit tidak direkomendasikan
	 * karena dapat menimpa jejak waktu perubahan yang sesungguhnya.
	 *
	 * @param tanggal_dirubah timestamp perubahan yang hendak diset
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Accessor untuk {@link #tanggal_dirubah} — timestamp perubahan terakhir baris ini.
	 *
	 * @return timestamp perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string entity ini, yaitu isi {@link #keterangan} apa adanya (tanpa fallback
	 * bila {@code null}). Dipakai antara lain oleh komponen UI yang menampilkan label ringkas
	 * untuk baris data ini.
	 *
	 * @return isi field {@link #keterangan}, dapat berupa {@code null}
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Accessor untuk {@link #keterangan}.
	 *
	 * @return catatan bebas baris ini, dapat {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mutator untuk {@link #keterangan}. Tidak ada guard nilai kosong/null di sini (berbeda dari
	 * {@link #setOleh(String)}/{@link #setOlehId(String)}) — nilai apa pun, termasuk {@code null},
	 * langsung diterima.
	 *
	 * @param keterangan catatan bebas yang hendak diset
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Accessor untuk {@link #nama}.
	 *
	 * @return nama/label bebas baris ini, dapat {@code null}
	 */
	@Column(name = "nama", nullable = true)
	public String getNama() {
		return nama;
	}

	/**
	 * Mutator untuk {@link #nama}.
	 *
	 * @param nama nama/label bebas yang hendak diset
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Accessor untuk {@link #yangDinilai} — pegawai subjek penilaian. Relasi {@code @ManyToOne}
	 * dengan {@code cascade = {PERSIST, MERGE}} dan {@code fetch} eksplisit {@code SELECT} (lazy,
	 * query terpisah saat diakses), kolom join {@code yang_dinilai} nullable.
	 *
	 * @return pegawai yang dinilai pada baris ini, dapat {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "yang_dinilai", nullable = true)
	public Pegawai getYangDinilai() {
		return yangDinilai;
	}

	/**
	 * Mutator untuk {@link #yangDinilai}. Tidak ada pengecekan bahwa pegawai yang diset di sini
	 * konsisten dengan konteks pemanggil (mis. bukan pegawai yang sedang login) — validasi
	 * kepemilikan sepenuhnya berada di luar entity ini (lihat catatan gerbang approval pada
	 * javadoc kelas).
	 *
	 * @param yangDinilai pegawai yang hendak diset sebagai subjek penilaian
	 */
	public void setYangDinilai(Pegawai yangDinilai) {
		this.yangDinilai = yangDinilai;
	}

	/**
	 * Accessor untuk {@link #penilai} — pejabat penilai. Relasi {@code @ManyToOne} dengan {@code
	 * cascade = {PERSIST, MERGE}} dan {@code fetch} eksplisit {@code SELECT}, kolom join {@code
	 * penilai} nullable.
	 *
	 * @return pejabat penilai pada baris ini, dapat {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penilai", nullable = true)
	public Pegawai getPenilai() {
		return penilai;
	}

	/**
	 * Mutator untuk {@link #penilai}. Tidak ada validasi bahwa pegawai yang diset benar-benar
	 * berwenang menilai {@link #yangDinilai} (mis. relasi atasan-bawahan organisasi); pengecekan
	 * semacam itu tidak diimplementasikan di jalur simpan manapun yang ditemukan.
	 *
	 * @param penilai pegawai yang hendak diset sebagai pejabat penilai
	 */
	public void setPenilai(Pegawai penilai) {
		this.penilai = penilai;
	}

	/**
	 * Accessor untuk {@link #atasanPenilai} — atasan dari pejabat penilai. Relasi {@code
	 * @ManyToOne} dengan {@code cascade = {PERSIST, MERGE}} dan {@code fetch} eksplisit {@code
	 * SELECT}, kolom join {@code atasan_penilai} nullable.
	 *
	 * @return atasan pejabat penilai pada baris ini, dapat {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "atasan_penilai", nullable = true)
	public Pegawai getAtasanPenilai() {
		return atasanPenilai;
	}

	/**
	 * Mutator untuk {@link #atasanPenilai}. Mengisi field ini TIDAK merepresentasikan tindakan
	 * "mengesahkan/menyetujui" apa pun secara fungsional — tidak ada flag status pengesahan yang
	 * ikut berubah; field ini murni referensi pegawai seperti {@link #yangDinilai}/{@link
	 * #penilai}.
	 *
	 * @param atasanPenilai pegawai yang hendak diset sebagai atasan pejabat penilai
	 */
	public void setAtasanPenilai(Pegawai atasanPenilai) {
		this.atasanPenilai = atasanPenilai;
	}

	/**
	 * Accessor untuk {@link #kesetiaan}.
	 *
	 * @return nilai angka unsur "Kesetiaan", dapat {@code null}
	 */
	public Double getKesetiaan() {
		return kesetiaan;
	}

	/**
	 * Mutator untuk {@link #kesetiaan}. Entity tidak memvalidasi rentang nilai; validasi 0–100
	 * (bila ada) hanya terjadi di listener UI {@code DaftarNilaiPelaksanaanPekerjaanAction}
	 * sebelum method ini dipanggil — pemanggil lain (mis. import batch, service lain) dapat
	 * menyimpan nilai di luar rentang tersebut tanpa penolakan.
	 *
	 * @param kesetiaan nilai angka yang hendak diset untuk unsur "Kesetiaan"
	 */
	public void setKesetiaan(Double kesetiaan) {
		this.kesetiaan = kesetiaan;
	}

	/**
	 * Accessor untuk {@link #prestasiKerja}.
	 *
	 * @return nilai angka unsur "Prestasi Kerja", dapat {@code null}
	 */
	public Double getPrestasiKerja() {
		return prestasiKerja;
	}

	/**
	 * Mutator untuk {@link #prestasiKerja}. Tidak ada validasi rentang di level entity maupun UI
	 * (berbeda dari {@link #kesetiaan} yang memiliki validasi 0–100 di UI) — nilai berapa pun
	 * diterima.
	 *
	 * @param prestasiKerja nilai angka yang hendak diset untuk unsur "Prestasi Kerja"
	 */
	public void setPrestasiKerja(Double prestasiKerja) {
		this.prestasiKerja = prestasiKerja;
	}

	/**
	 * Accessor untuk {@link #tanggungJawab}.
	 *
	 * @return nilai angka unsur "Tanggung Jawab", dapat {@code null}
	 */
	public Double getTanggungJawab() {
		return tanggungJawab;
	}

	/**
	 * Mutator untuk {@link #tanggungJawab}. Tidak ada validasi rentang di level entity maupun UI.
	 *
	 * @param tanggungJawab nilai angka yang hendak diset untuk unsur "Tanggung Jawab"
	 */
	public void setTanggungJawab(Double tanggungJawab) {
		this.tanggungJawab = tanggungJawab;
	}

	/**
	 * Accessor untuk {@link #ketaatan}.
	 *
	 * @return nilai angka unsur "Ketaatan", dapat {@code null}
	 */
	public Double getKetaatan() {
		return ketaatan;
	}

	/**
	 * Mutator untuk {@link #ketaatan}. Tidak ada validasi rentang di level entity maupun UI.
	 *
	 * @param ketaatan nilai angka yang hendak diset untuk unsur "Ketaatan"
	 */
	public void setKetaatan(Double ketaatan) {
		this.ketaatan = ketaatan;
	}

	/**
	 * Accessor untuk {@link #kejujuran}.
	 *
	 * @return nilai angka unsur "Kejujuran", dapat {@code null}
	 */
	public Double getKejujuran() {
		return kejujuran;
	}

	/**
	 * Mutator untuk {@link #kejujuran}. Tidak ada validasi rentang di level entity maupun UI.
	 *
	 * @param kejujuran nilai angka yang hendak diset untuk unsur "Kejujuran"
	 */
	public void setKejujuran(Double kejujuran) {
		this.kejujuran = kejujuran;
	}

	/**
	 * Accessor untuk {@link #kerjasama}.
	 *
	 * @return nilai angka unsur "Kerjasama", dapat {@code null}
	 */
	public Double getKerjasama() {
		return kerjasama;
	}

	/**
	 * Mutator untuk {@link #kerjasama}. Tidak ada validasi rentang di level entity maupun UI.
	 *
	 * @param kerjasama nilai angka yang hendak diset untuk unsur "Kerjasama"
	 */
	public void setKerjasama(Double kerjasama) {
		this.kerjasama = kerjasama;
	}

	/**
	 * Accessor untuk {@link #prakarsa}.
	 *
	 * @return nilai angka unsur "Prakarsa", dapat {@code null}
	 */
	public Double getPrakarsa() {
		return prakarsa;
	}

	/**
	 * Mutator untuk {@link #prakarsa}. Tidak ada validasi rentang di level entity maupun UI.
	 *
	 * @param prakarsa nilai angka yang hendak diset untuk unsur "Prakarsa"
	 */
	public void setPrakarsa(Double prakarsa) {
		this.prakarsa = prakarsa;
	}

	/**
	 * Accessor untuk {@link #kepimpinan}.
	 *
	 * @return nilai angka unsur "Kepemimpinan", dapat {@code null}
	 */
	public Double getKepimpinan() {
		return kepimpinan;
	}

	/**
	 * Mutator untuk {@link #kepimpinan}. Tidak ada validasi rentang di level entity maupun UI.
	 *
	 * @param kepimpinan nilai angka yang hendak diset untuk unsur "Kepemimpinan"
	 */
	public void setKepimpinan(Double kepimpinan) {
		this.kepimpinan = kepimpinan;
	}

	/**
	 * Accessor untuk {@link #sebutankesetiaan}.
	 *
	 * @return sebutan/predikat tekstual unsur "Kesetiaan", dapat {@code null}
	 */
	public String getSebutankesetiaan() {
		return sebutankesetiaan;
	}

	/**
	 * Mutator untuk {@link #sebutankesetiaan}. Nilai ini diisi otomatis oleh listener {@code
	 * onChange} pada UI lewat pengindeksan langsung {@code NilaiHelper.nilais[nilai.intValue()]}
	 * (kata bilangan 0–100 dalam Bahasa Indonesia, BUKAN predikat kinerja resmi seperti "Amat
	 * Baik"/"Baik"/"Cukup"/"Kurang") setiap kali {@link #kesetiaan} berubah di form; entity ini
	 * sendiri tidak memaksakan konsistensi antara nilai angka dan sebutannya — mutasi manual di
	 * luar UI dapat membuat keduanya tidak sinkron.
	 *
	 * @param sebutankesetiaan sebutan/predikat tekstual yang hendak diset
	 */
	public void setSebutankesetiaan(String sebutankesetiaan) {
		this.sebutankesetiaan = sebutankesetiaan;
	}

	/**
	 * Accessor untuk {@link #sebutanprestasiKerja}.
	 *
	 * @return sebutan/predikat tekstual unsur "Prestasi Kerja", dapat {@code null}
	 */
	public String getSebutanprestasiKerja() {
		return sebutanprestasiKerja;
	}

	/**
	 * Mutator untuk {@link #sebutanprestasiKerja}. Berbeda dari {@link #sebutankesetiaan}, field
	 * ini TIDAK memiliki listener otomatis di UI — nilai diisi manual sepenuhnya oleh pengguna
	 * lewat textbox, tanpa keterkaitan otomatis dengan {@link #prestasiKerja}.
	 *
	 * @param sebutanprestasiKerja sebutan/predikat tekstual yang hendak diset
	 */
	public void setSebutanprestasiKerja(String sebutanprestasiKerja) {
		this.sebutanprestasiKerja = sebutanprestasiKerja;
	}

	/**
	 * Accessor untuk {@link #sebutantanggungJawab}.
	 *
	 * @return sebutan/predikat tekstual unsur "Tanggung Jawab", dapat {@code null}
	 */
	public String getSebutantanggungJawab() {
		return sebutantanggungJawab;
	}

	/**
	 * Mutator untuk {@link #sebutantanggungJawab}. Diisi manual lewat UI, tidak ada listener
	 * otomatis (lihat catatan pada {@link #setSebutanprestasiKerja(String)}).
	 *
	 * @param sebutantanggungJawab sebutan/predikat tekstual yang hendak diset
	 */
	public void setSebutantanggungJawab(String sebutantanggungJawab) {
		this.sebutantanggungJawab = sebutantanggungJawab;
	}

	/**
	 * Accessor untuk {@link #sebutanketaatan}.
	 *
	 * @return sebutan/predikat tekstual unsur "Ketaatan", dapat {@code null}
	 */
	public String getSebutanketaatan() {
		return sebutanketaatan;
	}

	/**
	 * Mutator untuk {@link #sebutanketaatan}. Diisi manual lewat UI, tidak ada listener otomatis.
	 *
	 * @param sebutanketaatan sebutan/predikat tekstual yang hendak diset
	 */
	public void setSebutanketaatan(String sebutanketaatan) {
		this.sebutanketaatan = sebutanketaatan;
	}

	/**
	 * Accessor untuk {@link #sebutankejujuran}.
	 *
	 * @return sebutan/predikat tekstual unsur "Kejujuran", dapat {@code null}
	 */
	public String getSebutankejujuran() {
		return sebutankejujuran;
	}

	/**
	 * Mutator untuk {@link #sebutankejujuran}. Diisi manual lewat UI, tidak ada listener otomatis.
	 *
	 * @param sebutankejujuran sebutan/predikat tekstual yang hendak diset
	 */
	public void setSebutankejujuran(String sebutankejujuran) {
		this.sebutankejujuran = sebutankejujuran;
	}

	/**
	 * Accessor untuk {@link #sebutankerjasama}.
	 *
	 * @return sebutan/predikat tekstual unsur "Kerjasama", dapat {@code null}
	 */
	public String getSebutankerjasama() {
		return sebutankerjasama;
	}

	/**
	 * Mutator untuk {@link #sebutankerjasama}. Diisi manual lewat UI, tidak ada listener otomatis.
	 *
	 * @param sebutankerjasama sebutan/predikat tekstual yang hendak diset
	 */
	public void setSebutankerjasama(String sebutankerjasama) {
		this.sebutankerjasama = sebutankerjasama;
	}

	/**
	 * Accessor untuk {@link #sebutanprakarsa}.
	 *
	 * @return sebutan/predikat tekstual unsur "Prakarsa", dapat {@code null}
	 */
	public String getSebutanprakarsa() {
		return sebutanprakarsa;
	}

	/**
	 * Mutator untuk {@link #sebutanprakarsa}. Diisi manual lewat UI, tidak ada listener otomatis.
	 *
	 * @param sebutanprakarsa sebutan/predikat tekstual yang hendak diset
	 */
	public void setSebutanprakarsa(String sebutanprakarsa) {
		this.sebutanprakarsa = sebutanprakarsa;
	}

	/**
	 * Accessor untuk {@link #sebutankepimpinan}.
	 *
	 * @return sebutan/predikat tekstual unsur "Kepemimpinan", dapat {@code null}
	 */
	public String getSebutankepimpinan() {
		return sebutankepimpinan;
	}

	/**
	 * Mutator untuk {@link #sebutankepimpinan}. Diisi manual lewat UI, tidak ada listener otomatis.
	 *
	 * @param sebutankepimpinan sebutan/predikat tekstual yang hendak diset
	 */
	public void setSebutankepimpinan(String sebutankepimpinan) {
		this.sebutankepimpinan = sebutankepimpinan;
	}

	/**
	 * Accessor untuk {@link #keterangankesetiaan}.
	 *
	 * @return keterangan naratif tambahan unsur "Kesetiaan", dapat {@code null}
	 */
	public String getKeterangankesetiaan() {
		return keterangankesetiaan;
	}

	/**
	 * Mutator untuk {@link #keterangankesetiaan}.
	 *
	 * @param keterangankesetiaan keterangan naratif tambahan yang hendak diset
	 */
	public void setKeterangankesetiaan(String keterangankesetiaan) {
		this.keterangankesetiaan = keterangankesetiaan;
	}

	/**
	 * Accessor untuk {@link #keteranganprestasiKerja}.
	 *
	 * @return keterangan naratif tambahan unsur "Prestasi Kerja", dapat {@code null}
	 */
	public String getKeteranganprestasiKerja() {
		return keteranganprestasiKerja;
	}

	/**
	 * Mutator untuk {@link #keteranganprestasiKerja}.
	 *
	 * @param keteranganprestasiKerja keterangan naratif tambahan yang hendak diset
	 */
	public void setKeteranganprestasiKerja(String keteranganprestasiKerja) {
		this.keteranganprestasiKerja = keteranganprestasiKerja;
	}

	/**
	 * Accessor untuk {@link #keterangantanggungJawab}.
	 *
	 * @return keterangan naratif tambahan unsur "Tanggung Jawab", dapat {@code null}
	 */
	public String getKeterangantanggungJawab() {
		return keterangantanggungJawab;
	}

	/**
	 * Mutator untuk {@link #keterangantanggungJawab}.
	 *
	 * @param keterangantanggungJawab keterangan naratif tambahan yang hendak diset
	 */
	public void setKeterangantanggungJawab(String keterangantanggungJawab) {
		this.keterangantanggungJawab = keterangantanggungJawab;
	}

	/**
	 * Accessor untuk {@link #keteranganketaatan}.
	 *
	 * @return keterangan naratif tambahan unsur "Ketaatan", dapat {@code null}
	 */
	public String getKeteranganketaatan() {
		return keteranganketaatan;
	}

	/**
	 * Mutator untuk {@link #keteranganketaatan}.
	 *
	 * @param keteranganketaatan keterangan naratif tambahan yang hendak diset
	 */
	public void setKeteranganketaatan(String keteranganketaatan) {
		this.keteranganketaatan = keteranganketaatan;
	}

	/**
	 * Accessor untuk {@link #keterangankejujuran}.
	 *
	 * @return keterangan naratif tambahan unsur "Kejujuran", dapat {@code null}
	 */
	public String getKeterangankejujuran() {
		return keterangankejujuran;
	}

	/**
	 * Mutator untuk {@link #keterangankejujuran}.
	 *
	 * @param keterangankejujuran keterangan naratif tambahan yang hendak diset
	 */
	public void setKeterangankejujuran(String keterangankejujuran) {
		this.keterangankejujuran = keterangankejujuran;
	}

	/**
	 * Accessor untuk {@link #keterangankerjasama}.
	 *
	 * @return keterangan naratif tambahan unsur "Kerjasama", dapat {@code null}
	 */
	public String getKeterangankerjasama() {
		return keterangankerjasama;
	}

	/**
	 * Mutator untuk {@link #keterangankerjasama}.
	 *
	 * @param keterangankerjasama keterangan naratif tambahan yang hendak diset
	 */
	public void setKeterangankerjasama(String keterangankerjasama) {
		this.keterangankerjasama = keterangankerjasama;
	}

	/**
	 * Accessor untuk {@link #keteranganprakarsa}.
	 *
	 * @return keterangan naratif tambahan unsur "Prakarsa", dapat {@code null}
	 */
	public String getKeteranganprakarsa() {
		return keteranganprakarsa;
	}

	/**
	 * Mutator untuk {@link #keteranganprakarsa}.
	 *
	 * @param keteranganprakarsa keterangan naratif tambahan yang hendak diset
	 */
	public void setKeteranganprakarsa(String keteranganprakarsa) {
		this.keteranganprakarsa = keteranganprakarsa;
	}

	/**
	 * Accessor untuk {@link #keterangankepimpinan}.
	 *
	 * @return keterangan naratif tambahan unsur "Kepemimpinan", dapat {@code null}
	 */
	public String getKeterangankepimpinan() {
		return keterangankepimpinan;
	}

	/**
	 * Mutator untuk {@link #keterangankepimpinan}.
	 *
	 * @param keterangankepimpinan keterangan naratif tambahan yang hendak diset
	 */
	public void setKeterangankepimpinan(String keterangankepimpinan) {
		this.keterangankepimpinan = keterangankepimpinan;
	}

	/**
	 * Accessor untuk {@link #jumlah}.
	 *
	 * @return nilai total (jumlah) kedelapan unsur penilaian, dapat {@code null}
	 */
	public Double getJumlah() {
		return jumlah;
	}

	/**
	 * Mutator untuk {@link #jumlah}. Nilai ini diisi manual lewat form UI (Doublebox terpisah),
	 * BUKAN dihitung otomatis dari penjumlahan {@link #kesetiaan}..{@link #kepimpinan} oleh
	 * entity ini maupun oleh {@code DaftarNilaiPelaksanaanPekerjaanAction.onSave(...)} — total
	 * dapat tidak konsisten dengan kedelapan unsur penyusunnya tanpa penolakan sistem.
	 *
	 * @param jumlah nilai total yang hendak diset
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Accessor untuk {@link #rataRata}.
	 *
	 * @return nilai rata-rata kedelapan unsur penilaian, dapat {@code null}
	 */
	public Double getRataRata() {
		return rataRata;
	}

	/**
	 * Mutator untuk {@link #rataRata}. Sama seperti {@link #setJumlah(Double)}, nilai ini diisi
	 * manual lewat form UI tanpa formula otomatis yang menjamin konsistensi terhadap kedelapan
	 * unsur penyusunnya.
	 *
	 * @param rataRata nilai rata-rata yang hendak diset
	 */
	public void setRataRata(Double rataRata) {
		this.rataRata = rataRata;
	}

	/**
	 * Accessor untuk {@link #sebutanjumlah}.
	 *
	 * @return sebutan/predikat tekstual untuk baris {@link #jumlah}, dapat {@code null}
	 */
	public String getSebutanjumlah() {
		return sebutanjumlah;
	}

	/**
	 * Mutator untuk {@link #sebutanjumlah}. Diisi manual lewat UI, tidak ada listener otomatis.
	 *
	 * @param sebutanjumlah sebutan/predikat tekstual yang hendak diset
	 */
	public void setSebutanjumlah(String sebutanjumlah) {
		this.sebutanjumlah = sebutanjumlah;
	}

	/**
	 * Accessor untuk {@link #sebutanrataRata}.
	 *
	 * @return sebutan/predikat tekstual untuk baris {@link #rataRata}, dapat {@code null}
	 */
	public String getSebutanrataRata() {
		return sebutanrataRata;
	}

	/**
	 * Mutator untuk {@link #sebutanrataRata}. Diisi manual lewat UI, tidak ada listener otomatis.
	 *
	 * @param sebutanrataRata sebutan/predikat tekstual yang hendak diset
	 */
	public void setSebutanrataRata(String sebutanrataRata) {
		this.sebutanrataRata = sebutanrataRata;
	}

	/**
	 * Accessor untuk {@link #keteranganjumlah}.
	 *
	 * @return keterangan naratif tambahan untuk baris {@link #jumlah}, dapat {@code null}
	 */
	public String getKeteranganjumlah() {
		return keteranganjumlah;
	}

	/**
	 * Mutator untuk {@link #keteranganjumlah}.
	 *
	 * @param keteranganjumlah keterangan naratif tambahan yang hendak diset
	 */
	public void setKeteranganjumlah(String keteranganjumlah) {
		this.keteranganjumlah = keteranganjumlah;
	}

	/**
	 * Accessor untuk {@link #keteranganrataRata}.
	 *
	 * @return keterangan naratif tambahan untuk baris {@link #rataRata}, dapat {@code null}
	 */
	public String getKeteranganrataRata() {
		return keteranganrataRata;
	}

	/**
	 * Mutator untuk {@link #keteranganrataRata}.
	 *
	 * @param keteranganrataRata keterangan naratif tambahan yang hendak diset
	 */
	public void setKeteranganrataRata(String keteranganrataRata) {
		this.keteranganrataRata = keteranganrataRata;
	}

}
