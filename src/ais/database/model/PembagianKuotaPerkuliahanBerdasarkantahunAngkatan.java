package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.Calendar;
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

/**
 * Entity master <b>pembagian kuota kelas perkuliahan berdasarkan rentang tahun angkatan
 * mahasiswa</b> (tabel {@code pembagian_kuota_perkuliahan_berdasarkan_tahun_angkatan}).
 *
 * <p>Satu baris mengikat sebuah {@link Perkuliahan} (satu kelas mata kuliah pada satu
 * semester) dengan sebuah rentang tahun angkatan
 * ({@link #getTahunMulai()}&nbsp;&hellip;&nbsp;{@link #getTahunSampai()}) dan sebuah angka
 * {@link #getKuota()}. Saat mahasiswa mengisi KRS, kuota inilah yang dipakai sebagai
 * <b>kapasitas kelas efektif</b> untuk mahasiswa yang tahun angkatannya jatuh di rentang
 * tersebut — menggantikan {@code Perkuliahan.getKapasitasKelas()} yang berlaku umum.</p>
 *
 * <h2>Semantik yang mudah disalahpahami: ini BUKAN penjatahan kursi</h2>
 * <p>Nama entity mengesankan "sekian kursi dicadangkan untuk angkatan sekian", tetapi
 * implementasinya bukan demikian. Pada setiap titik penegakan, angka yang dibandingkan
 * dengan kuota adalah <b>jumlah peserta kelas secara keseluruhan</b>
 * ({@code KrsUtilHelper.ambilJumlahDetailperkuliahan(session, perkuliahan, reload)} →
 * {@code Perkuliahan.ambilJumlahDetailperkuliahan()}), yang menghitung SEMUA peserta tanpa
 * memandang angkatan. Jadi yang sesungguhnya terjadi:</p>
 * <ul>
 *   <li>kuota berperan sebagai <b>batas atas total kelas yang berbeda-beda per angkatan</b>,
 *       bukan sebagai jatah terpisah;</li>
 *   <li>kelas berkapasitas 40 dengan kuota 10 untuk angkatan 2023 akan menolak mahasiswa
 *       angkatan 2023 begitu jumlah peserta kelas mencapai 10 — <b>walau kesepuluh peserta
 *       itu berasal dari angkatan lain</b>, dan tidak ada satu pun kursi yang benar-benar
 *       tersimpan untuk angkatan 2023;</li>
 *   <li>karena itu efeknya "siapa cepat dia dapat" dengan ambang berbeda per angkatan, dan
 *       urutan pengisian KRS antar angkatan menentukan hasil akhirnya.</li>
 * </ul>
 *
 * <h2>Alur pemakaian</h2>
 * <p>Seluruh pembacaan melewati satu pintu:
 * {@code KrsUtilHelper.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(session,
 * perkuliahan, tahunangkatan, reload)}, yang memilih baris dengan
 * {@code tahunMulai <= tahunangkatan <= tahunSampai}, diurutkan {@code kuota} menurun dan
 * dibatasi satu hasil, lalu meng-cache hasilnya sebagai berkas JSON sementara berkunci
 * {@code perkuliahan.id + "_" + tahunangkatan}. Pemanggil helper itu:</p>
 * <ol>
 *   <li>{@code AmbilDataPerkuliahanHelper} — layar pemilihan KRS ZK: menentukan label
 *       "Tersedia"/"Penuh", warna baris, serta {@code setDisabled} pada checkbox; dan pada
 *       jalur penyimpanan KRS-nya, <b>menolak</b> penambahan peserta ketika jumlah peserta
 *       setelah penambahan melampaui kuota (pesan "kapasitas kelas untuk perkuliahan ini
 *       telah penuh").</li>
 *   <li>{@code AmbilDataPaketPerkuliahanHelper} dan
 *       {@code ais.common.newui.akademik.NewUiKrsPaketController} — jalur KRS paket.</li>
 *   <li>{@code ais.action.servlet.api.ElearningApiUtil} — API mobile/e-learning: nilai
 *       kuota dikirim ke klien sebagai field {@code kapasitasKelas}, sehingga aplikasi
 *       mobile menampilkan kapasitas yang sudah "diterjemahkan" untuk angkatan mahasiswa
 *       yang sedang login.</li>
 * </ol>
 * <p>Penulisannya berasal dari layar master
 * {@code /pages/master/pembagian_kuota_perkuliahan_berdasarkan_tahun_angkatan.zul}
 * ({@code PembagianKuotaPerkuliahanBerdasarkantahunAngkatanAction}), yang selalu dibuka
 * dalam konteks satu {@link Perkuliahan} tertentu (parameter URL {@code perkuliahan}).</p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Jejak audit yang dideklarasikan ulang</b> — {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()} beserta setter-nya dan
 *       {@link #onUpdate()}. Lihat catatan {@link GeneralValueObject} di bawah: pengulangan
 *       ini KEHARUSAN teknis, bukan duplikasi yang perlu dibersihkan.</li>
 *   <li><b>Identitas baris</b> — {@link #getNama()}, {@link #getKeterangan()},
 *       {@link #toString()}.</li>
 *   <li><b>Rentang angkatan</b> — {@link #getTahunMulai()}, {@link #getTahunSampai()}.
 *       Keduanya memakai nilai default saat kolomnya kosong (lihat kuirk).</li>
 *   <li><b>Angka kuota</b> — {@link #getKuota()} (nilai yang benar-benar ditegakkan) dan
 *       {@link #getPersen()} (turunan, hanya untuk tampilan).</li>
 *   <li><b>Relasi</b> — {@link #getPerkuliahan()}, satu-satunya {@code @ManyToOne} di kelas
 *       ini dan kolom wajib ({@code nullable = false}).</li>
 *   <li><b>Cache berkas JSON</b> — {@link #write()}, {@link #getFileLocation()},
 *       {@link #getOrCreateFileLocation()}, {@link #setFileLocation(String)}.</li>
 * </ul>
 * <p>Tidak ada {@code equals}/{@code hashCode} yang di-override, tidak ada method query
 * statis, dan tidak ada validasi domain di kelas ini; seluruh query dan validasi hidup di
 * {@code KrsUtilHelper} serta Action layar master.</p>
 *
 * <h2>Kehalusan dan kuirk yang perlu diketahui</h2>
 * <ul>
 *   <li><b>{@link #getPersen()} menimpa field-nya sendiri setiap kali dibaca.</b> Nilai
 *       persen dihitung ulang dari {@code kuota} dan {@code Perkuliahan.kapasitasKelas},
 *       lalu ditulis balik ke field. Karena anotasi pemetaan berada pada getter (Hibernate
 *       memakai <i>property access</i>), nilai turunan itulah yang dibaca saat
 *       insert/dirty-check — sehingga angka persen yang diisi manual (misal lewat endpoint
 *       generik) <b>tidak akan bertahan</b> selama kapasitas kelas terisi. Layar master
 *       sendiri tidak pernah mengisi persen; kolom ini murni turunan untuk tampilan.</li>
 *   <li><b>{@link #getKuota()} juga menulis balik, dan default-nya berbahaya di dua arah.</b>
 *       Bila kolom {@code kuota} kosong: (a) jika kelas punya kapasitas &gt; 0, kuota
 *       di-set menjadi <b>seluruh</b> kapasitas kelas — baris itu efektif tidak membatasi
 *       apa pun (fail-open); (b) jika kelas tidak punya kapasitas (null/0), kuota menjadi
 *       <b>0</b> — dan karena pemanggil menimpa {@code kapasitasKelas} dengan angka itu,
 *       seluruh mahasiswa pada rentang angkatan tersebut langsung dinyatakan "Penuh"
 *       (fail-closed total). Satu baris kosong yang sama karenanya bisa berarti "tanpa
 *       batas" atau "kelas tertutup", tergantung data {@link Perkuliahan}-nya.</li>
 *   <li><b>{@link #getTahunMulai()}/{@link #getTahunSampai()} juga menulis balik.</b>
 *       Kolom kosong masing-masing menjadi {@code 0} dan <b>tahun berjalan saat pembacaan
 *       pertama</b>. Konsekuensi property access sama seperti di atas: rentang tersebut
 *       berpotensi ikut tersimpan permanen pada flush berikutnya, sehingga sebuah baris
 *       tanpa batas akhir "membeku" pada tahun ketika ia kebetulan pertama kali dibaca.</li>
 *   <li><b>Baris yang tumpang tindih tidak dicegah, dan dimenangkan oleh kuota TERBESAR.</b>
 *       Query pemilih memakai {@code addOrder(Order.desc("kuota")).setMaxResults(1)}, jadi
 *       bila dua baris sama-sama mencakup satu angkatan, yang berlaku adalah yang paling
 *       longgar. Tidak ada unique constraint, tidak ada validasi
 *       {@code tahunMulai <= tahunSampai}, dan tidak ada pemeriksaan tumpang tindih di layar
 *       master — kuirk yang sekeluarga dengan {@code PesanRuangan} dan
 *       {@code DendaPembayaran}.</li>
 *   <li><b>{@link #getNama()} tidak pernah diisi pengguna.</b> Layar master mengisinya
 *       otomatis dengan {@code perkuliahan.toString()} tepat sebelum menyimpan. Karena
 *       kolomnya {@code nullable = false}, baris yang dibuat lewat jalur lain (endpoint CRUD
 *       generik) tanpa mengisi {@code nama} akan gagal insert. Getter melakukan
 *       {@code trim()} hanya pada nilai balik — tidak menulis balik ke field.</li>
 *   <li><b>{@code fileLocation} adalah properti TERPETAKAN, bukan transient.</b> Hanya
 *       {@link #getOrCreateFileLocation()} yang diberi {@code @javax.persistence.Transient};
 *       {@link #getFileLocation()} polos, sehingga Hibernate memetakannya ke kolom
 *       {@code fileLocation} (dibuat otomatis oleh {@code hbm2ddl.auto=update}). Akibatnya
 *       memanggil {@link #getOrCreateFileLocation()} atas objek yang masih persisten akan
 *       <b>mengotori</b> entity dan berpotensi memicu UPDATE — plus satu revisi Envers,
 *       karena kelas ini {@link Audited @Audited} — pada alur yang secara logika hanya
 *       membaca kuota. {@code KrsUtilHelper} memanggilnya persis di jalur baca KRS.
 *       Pemisahan getter murni vs getter yang menulis ini adalah hasil r77028 ("make
 *       persisted file location getters side-effect free"): {@link #getFileLocation()}
 *       sekarang aman, efek sampingnya dipindah ke method bernama eksplisit.</li>
 *   <li><b>Hasil dari cache adalah objek DETACHED.</b> Jalur cache {@code KrsUtilHelper}
 *       merekonstruksi objek dari JSON lalu menyuntikkan ulang {@link Perkuliahan} yang
 *       hidup. Selama cache belum kedaluwarsa dan {@code reload} tidak {@code true},
 *       perubahan kuota di layar master <b>belum tentu langsung terlihat</b> pada layar
 *       KRS.</li>
 *   <li><b>{@link #write()} tanpa argumen menutupi (shadow) {@code write(String...)} milik
 *       kelas induk</b> untuk pemanggilan tanpa argumen — resolusi overload Java memilih
 *       method fixed-arity lebih dulu daripada varargs. Argumen yang diteruskan,
 *       {@code Perkuliahan.class.getName()}, bukan daftar properti melainkan
 *       <b>daftar kelas yang dikecualikan</b> ({@code clazzPengecualian} pada
 *       {@code Common.convertToJsonObject}) — mencegah serialisasi berputar ke relasi
 *       induknya.</li>
 *   <li><b>Javadoc bawaan hbm2java salah kelas.</b> Sebelum revisi ini header file tertulis
 *       "Bank generated by hbm2java" — sisa salin-tempel generator, tidak ada hubungannya
 *       dengan bank.</li>
 * </ul>
 *
 * <h2>Catatan kontrol akses</h2>
 * <p>{@code PembagianKuotaPerkuliahanBerdasarkantahunAngkatanAction} memanggil
 * {@code Common.doCheckSecurity()} pada {@code doBeforeCompose}, tetapi panggilan itu
 * bermuara ke {@code CommonPrivilages.doCheckPrevilagesRead()} yang hanya benar-benar
 * menegakkan pemeriksaan untuk 12 halaman pada daftar {@code CommonPrivilages.MUST_CHECKED}
 * — halaman ini <b>tidak</b> termasuk, jadi panggilan tersebut tidak menegakkan apa pun.
 * Lebih jauh, pada {@code doAfterCompose} pemeriksaan sesi dan
 * {@code checkPrevilages(READ)} hanya dijalankan pada cabang {@code else}: bila permintaan
 * membawa parameter URL {@code perkuliahan}, kedua pemeriksaan itu <b>dilewati
 * seluruhnya</b> dan daftar kuota kelas tetap dirender. Tombol Tambah/Ubah/Hapus tetap
 * tersembunyi (privilege bernilai false tanpa pengguna), sehingga dampak langsungnya
 * keterbacaan konfigurasi, bukan penulisan — namun polanya ("gerbang dilewati oleh parameter
 * URL") perlu diketahui pembaca kode. Jalur tulis yang sesungguhnya berisiko ada di luar
 * kelas ini: entity ini juga terjangkau endpoint reflektif generik {@code /Data} dan
 * {@code /Api}, dan mengubah kuota di sana berarti mengubah siapa yang boleh mengambil
 * KRS.</p>
 *
 * <h2>Tentang {@link GeneralValueObject}</h2>
 * <p>{@code GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti apa pun miliknya. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di setiap entity turunan agar
 * ikut terpetakan. Pengulangan tersebut adalah keharusan teknis, bukan bug atau duplikasi
 * yang perlu dirapikan. Method {@link #write()} dan {@link #getOrCreateFileLocation()}
 * memanfaatkan mesin cache JSON milik induk — lihat
 * {@link GeneralValueObject#write(Integer, String...)}.</p>
 *
 * @see GeneralValueObject
 * @see Perkuliahan
 * @see Detailperkuliahan
 * @see Matakuliah
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pembagian_kuota_perkuliahan_berdasarkan_tahun_angkatan")

public class PembagianKuotaPerkuliahanBerdasarkantahunAngkatan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini dibangkitkan sekali saat kelas dibuat dan harus
	 * dipertahankan apa adanya; mengubahnya memutus kompatibilitas deserialisasi objek yang
	 * tersimpan di session/cache ZK maupun yang dikirim antar node.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Primary key baris kuota ini (kolom {@code id}, IDENTITY). Dideklarasikan ulang di sini
	 * karena {@link GeneralValueObject} tidak dipetakan Hibernate — lihat Javadoc kelas.
	 */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi jejak audit aplikasi. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum
	 *         pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null}, string kosong, atau string yang hanya berisi
	 * spasi <b>diabaikan diam-diam</b> — method langsung {@code return} dan nilai lama
	 * dipertahankan, tanpa exception dan tanpa log. Ini disengaja agar jejak audit tidak
	 * terhapus oleh binding UI yang mengirim nilai kosong, tetapi berarti field ini tidak
	 * bisa dikosongkan lewat setter.</p>
	 *
	 * @param olehId id pengguna; nilai kosong/blank tidak berefek apa pun.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Berperilaku sama dengan {@link #setOlehId(String)}: nilai {@code null}/kosong/blank
	 * diabaikan diam-diam sehingga jejak audit lama tidak tertimpa nilai hampa.</p>
	 *
	 * @param oleh nama pengguna; nilai kosong/blank tidak berefek apa pun.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum
	 *         pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA yang dijalankan otomatis <b>tepat sebelum UPDATE</b> baris ini,
	 * mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} untuk memperbarui
	 * {@link #tanggal_dirubah} (dan jejak pengguna bila konteksnya tersedia).
	 *
	 * <p>Tidak pernah dipanggil manual dari kode mana pun — pemicunya adalah provider
	 * JPA/Hibernate. Perhatikan tidak ada pasangan {@code @PrePersist}: pada INSERT,
	 * {@code tanggal_dirubah} hanya berisi nilai inisialisasi field.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Waktu perubahan terakhir baris ini. Diinisialisasi ke waktu sekarang lewat
	 * {@code WaktuUtil.getDate()} saat objek dibuat, lalu diperbarui otomatis oleh
	 * {@link #onUpdate()} pada setiap UPDATE.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir; menimpa nilai yang ada tanpa validasi.
	 *                        Umumnya diisi {@code AuditTimestampInterceptor}, bukan kode
	 *                        aplikasi.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini (presisi TIMESTAMP). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini: {@code id + "-" + nama}.
	 *
	 * <p>Membaca field {@link #nama} secara langsung (bukan lewat {@link #getNama()}), jadi
	 * nilainya tidak di-{@code trim} dan bisa berbunyi {@code "null-null"} untuk objek baru
	 * yang belum tersimpan. Dipakai untuk log/debug dan pilihan combobox generik; bukan
	 * bagian dari kontrak data.</p>
	 *
	 * @return gabungan id dan nama baris ini.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Label baris kuota. Tidak diisi pengguna: layar master mengisinya otomatis dengan
	 * {@code perkuliahan.toString()} saat menyimpan. Kolomnya {@code NOT NULL}.
	 */
	private String nama;
	/** Catatan bebas dari petugas akademik, misal alasan pembatasan. Boleh kosong. */
	private String keterangan;

	/** Batas bawah rentang tahun angkatan yang dicakup baris ini (inklusif). */
	private Integer tahunMulai;
	/** Batas atas rentang tahun angkatan yang dicakup baris ini (inklusif). */
	private Integer tahunSampai;

	/**
	 * Persentase kuota terhadap kapasitas kelas. Nilai turunan untuk tampilan saja —
	 * dihitung ulang dan ditimpa setiap kali {@link #getPersen()} dipanggil.
	 */
	private Double persen;
	/**
	 * Jumlah peserta maksimum yang berlaku bagi mahasiswa pada rentang angkatan ini. Nilai
	 * inilah yang menggantikan {@code Perkuliahan.kapasitasKelas} pada penegakan KRS.
	 */
	private Integer kuota;

	/** Kelas perkuliahan yang dibatasi baris ini; kolom wajib ({@code nullable = false}). */
	private Perkuliahan perkuliahan;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk instansiasi entity. Juga dipakai
	 * layar master saat pengguna menekan tombol Tambah; seluruh field diisi setelahnya lewat
	 * setter.
	 */
	public PembagianKuotaPerkuliahanBerdasarkantahunAngkatan() {
	}

	/**
	 * @return primary key baris ini, atau {@code null} bila belum pernah disimpan. Kolom
	 *         {@code id} bertipe IDENTITY dan {@code insertable = false}, jadi nilainya
	 *         dibangkitkan basis data.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key baris ini. Diisi Hibernate setelah insert; mengisinya manual pada
	 *           objek yang sudah persisten bukan cara yang didukung.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Label baris kuota, sudah dibersihkan spasi di ujungnya.
	 *
	 * <p>{@code trim()} hanya diterapkan pada nilai balik — field aslinya tidak ditulis
	 * ulang, jadi getter ini tidak punya efek samping. Isinya bukan masukan pengguna: layar
	 * master menyalin {@code perkuliahan.toString()} ke sini tepat sebelum menyimpan.</p>
	 *
	 * @return label baris ini tanpa spasi di ujung, atau {@code null} bila kolomnya kosong.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama label baris; disimpan apa adanya tanpa validasi maupun {@code trim}.
	 *             Kolomnya {@code NOT NULL}, sehingga membiarkannya {@code null} akan
	 *             menggagalkan insert.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return catatan bebas petugas akademik, atau {@code null} bila tidak diisi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan catatan bebas; boleh {@code null} dan disimpan apa adanya.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Batas bawah rentang tahun angkatan yang dicakup baris ini (inklusif).
	 *
	 * <p><b>Getter yang menulis balik:</b> bila kolomnya kosong, field di-set menjadi
	 * {@code 0} — artinya "berlaku sejak angkatan berapa pun". Karena Hibernate memetakan
	 * kelas ini lewat <i>property access</i> (anotasi ada di getter), nilai default itu ikut
	 * terbaca saat dirty-check dan berpotensi tersimpan permanen ke kolom pada flush
	 * berikutnya.</p>
	 *
	 * @return batas bawah rentang angkatan; tidak pernah {@code null}.
	 */
	public Integer getTahunMulai() {
		if (tahunMulai == null) {
			tahunMulai = 0;
		}
		return tahunMulai;
	}

	/**
	 * @param tahunMulai batas bawah rentang angkatan (inklusif). Tidak ada validasi bahwa
	 *                   nilainya &le; {@link #getTahunSampai()}, dan tidak ada pemeriksaan
	 *                   tumpang tindih dengan baris lain pada perkuliahan yang sama.
	 */
	public void setTahunMulai(Integer tahunMulai) {
		this.tahunMulai = tahunMulai;
	}

	/**
	 * Batas atas rentang tahun angkatan yang dicakup baris ini (inklusif).
	 *
	 * <p><b>Getter yang menulis balik:</b> bila kolomnya kosong, field di-set menjadi
	 * <b>tahun berjalan saat pembacaan itu terjadi</b>
	 * ({@code WaktuUtil.getCalendar().get(Calendar.YEAR)}) — bukan nilai "tak terbatas".
	 * Sama seperti {@link #getTahunMulai()}, nilai default tersebut berpotensi ikut tersimpan
	 * permanen lewat dirty-check, sehingga rentang sebuah baris bisa "membeku" pada tahun
	 * ketika ia kebetulan pertama kali dibaca — dan sejak itu tidak lagi mencakup angkatan
	 * yang lebih baru.</p>
	 *
	 * @return batas atas rentang angkatan; tidak pernah {@code null}.
	 */
	public Integer getTahunSampai() {
		if (tahunSampai == null) {
			tahunSampai = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahunSampai;
	}

	/**
	 * @param tahunSampai batas atas rentang angkatan (inklusif); tanpa validasi rentang
	 *                    maupun pemeriksaan tumpang tindih.
	 */
	public void setTahunSampai(Integer tahunSampai) {
		this.tahunSampai = tahunSampai;
	}

	/**
	 * Persentase kuota terhadap kapasitas kelas, untuk kolom tampilan di layar master.
	 *
	 * <p><b>Getter yang menghitung ulang dan menimpa field:</b> selama {@link #getPerkuliahan()}
	 * ada dan kapasitas kelasnya &gt; 0, nilai persen selalu dihitung ulang sebagai
	 * {@code kuota * 100 / kapasitasKelas} lalu ditulis ke field — nilai apa pun yang pernah
	 * disimpan di kolom {@code persen} akan tergantikan. Karena pemetaan memakai property
	 * access, hasil hitungan itulah yang dibaca Hibernate saat insert/dirty-check, sehingga
	 * kolom {@code persen} praktis adalah kolom turunan. Bila perkuliahan belum ter-set atau
	 * kapasitasnya kosong/0, nilai lama dikembalikan apa adanya (bisa {@code null}).</p>
	 *
	 * <p>Perhatikan pemanggilan {@link #getKuota()} di dalamnya: pembacaan persen ikut memicu
	 * efek samping default kuota (lihat {@link #getKuota()}).</p>
	 *
	 * @return persentase kuota terhadap kapasitas kelas, atau {@code null} bila belum pernah
	 *         dihitung maupun diisi.
	 */
	public Double getPersen() {
		if (perkuliahan != null && perkuliahan.getKapasitasKelas() != null && perkuliahan.getKapasitasKelas() > 0) {
			persen = (getKuota().doubleValue() * 100.0) / perkuliahan.getKapasitasKelas().doubleValue();
		}
		return persen;
	}

	/**
	 * @param persen persentase kuota. Menyetel nilai ini <b>tidak bertahan</b> selama kelas
	 *               punya kapasitas &gt; 0, karena {@link #getPersen()} akan menghitung ulang
	 *               dan menimpanya pada pembacaan berikutnya.
	 */
	public void setPersen(Double persen) {
		this.persen = persen;
	}

	/**
	 * Jumlah peserta maksimum yang berlaku bagi mahasiswa pada rentang angkatan ini — angka
	 * yang benar-benar ditegakkan saat pengisian KRS.
	 *
	 * <p><b>Getter yang menulis balik, dengan dua default yang berlawanan arah:</b></p>
	 * <ul>
	 *   <li>kolom kosong <i>dan</i> kelas punya kapasitas &gt; 0 → kuota di-set sama dengan
	 *       <b>seluruh</b> kapasitas kelas, sehingga baris ini efektif tidak membatasi apa pun
	 *       (fail-open);</li>
	 *   <li>kolom kosong <i>dan</i> kelas tidak punya kapasitas (null atau 0) → kuota menjadi
	 *       <b>0</b>. Karena pemanggil di jalur KRS menimpa {@code kapasitasKelas} dengan
	 *       nilai ini, seluruh mahasiswa pada rentang angkatan tersebut langsung berstatus
	 *       "Penuh" (fail-closed total).</li>
	 * </ul>
	 * <p>Sama seperti getter rentang tahun, nilai default itu ditulis ke field dan
	 * berpotensi tersimpan permanen lewat dirty-check property access.</p>
	 *
	 * <p>Ingat semantik penegakannya (lihat Javadoc kelas): angka ini dibandingkan dengan
	 * jumlah peserta kelas <b>secara keseluruhan</b>, bukan jumlah peserta dari angkatan yang
	 * bersangkutan.</p>
	 *
	 * @return kuota efektif untuk rentang angkatan ini; tidak pernah {@code null}.
	 */
	public Integer getKuota() {
		if (kuota == null && perkuliahan != null && perkuliahan.getKapasitasKelas() != null
				&& perkuliahan.getKapasitasKelas() > 0) {
			kuota = perkuliahan.getKapasitasKelas();
		}

		if (kuota == null) {
			kuota = 0;
		}

		return kuota;
	}

	/**
	 * @param kuota jumlah peserta maksimum bagi rentang angkatan ini. Tidak ada validasi
	 *              bahwa nilainya &le; kapasitas kelas maupun &ge; 0; layar master hanya
	 *              memastikan kolomnya terisi.
	 */
	public void setKuota(Integer kuota) {
		this.kuota = kuota;
	}

	/**
	 * Kelas perkuliahan yang dibatasi baris kuota ini.
	 *
	 * <p>Relasi wajib ({@code nullable = false}) dengan {@code FetchMode.SELECT} dan cascade
	 * {@code PERSIST}/{@code MERGE} — menyimpan baris kuota ikut mem-persist/merge objek
	 * {@link Perkuliahan} yang menempel padanya. Getter ini mengembalikan field apa adanya;
	 * bila objeknya masih proxy lazy, mengakses propertinya di luar session terbuka akan
	 * memicu {@code LazyInitializationException}.</p>
	 *
	 * @return perkuliahan pemilik baris kuota ini, atau {@code null} pada objek baru yang
	 *         belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perkuliahan", nullable = false)
	public Perkuliahan getPerkuliahan() {
		return perkuliahan;
	}

	/**
	 * @param perkuliahan kelas perkuliahan pemilik baris ini. Juga dipanggil
	 *                    {@code KrsUtilHelper} untuk menyuntikkan kembali perkuliahan yang
	 *                    hidup ke objek hasil rekonstruksi dari cache JSON (objek detached),
	 *                    supaya {@link #getPersen()}/{@link #getKuota()} punya rujukan
	 *                    kapasitas kelas.
	 */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * Lokasi berkas cache JSON milik baris ini. <b>Properti terpetakan</b> (kolom
	 * {@code fileLocation}), bukan transient — lihat Javadoc kelas.
	 */
	private String fileLocation;

	/**
	 * Menulis snapshot JSON baris ini ke berkas cache sementara lalu mencatat lokasinya di
	 * {@link #fileLocation}.
	 *
	 * <p>Mendelegasikan ke {@code GeneralValueObject.write(String...)} dengan argumen
	 * {@code Perkuliahan.class.getName()}. Argumen itu bukan daftar properti yang ikut
	 * ditulis melainkan <b>daftar kelas yang dikecualikan</b> ({@code clazzPengecualian} pada
	 * {@code Common.convertToJsonObject}) — tanpa pengecualian ini serialisasi akan menelusuri
	 * balik ke {@link Perkuliahan} beserta seluruh grafnya.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #setFileLocation(String)}, sehingga pada objek
	 * yang masih persisten entity menjadi kotor dan berpotensi memicu UPDATE beserta satu
	 * revisi Envers pada flush berikutnya. Perlu diingat juga bahwa kelas induk tidak selalu
	 * benar-benar menulis berkas (ada penjaga kedalaman rekursi dan cache konstanta), tetapi
	 * tetap mengembalikan {@link File} penunjuk lokasi — nilai balik non-{@code null} bukan
	 * jaminan berkasnya ada di disk.</p>
	 *
	 * <p>Method tanpa argumen ini menutupi ({@code shadow}) varargs
	 * {@code GeneralValueObject.write(String...)} untuk pemanggilan tanpa argumen: resolusi
	 * overload Java memilih method fixed-arity lebih dulu.</p>
	 *
	 * @return berkas cache hasil penulisan, atau berkas penunjuk lokasi bila penulisan
	 *         dilewati kelas induk.
	 * @see GeneralValueObject#write(Integer, String...)
	 */
	public File write() {
		File f = write(Perkuliahan.class.getName());
		setFileLocation(f.getAbsolutePath());
		return f;
	}

	/**
	 * Getter murni (tanpa efek samping) untuk lokasi berkas cache JSON.
	 *
	 * <p>Sejak r77028 method ini sengaja hanya membaca field: ia <b>tidak</b> menulis berkas
	 * dan tidak mengubah state, supaya Hibernate maupun serialisasi generik boleh
	 * memanggilnya kapan saja. Bila lokasi cache perlu dijamin ada, pakai
	 * {@link #getOrCreateFileLocation()}.</p>
	 *
	 * @return path berkas cache terakhir, atau {@code null} bila belum pernah ditulis.
	 */
	public String getFileLocation() {
		return fileLocation;
	}

	/**
	 * Mengembalikan lokasi berkas cache JSON baris ini, <b>membuatnya lebih dulu bila
	 * perlu</b>.
	 *
	 * <p>Berkas ditulis ulang lewat {@link #write()} bila salah satu benar: lokasi belum
	 * pernah diisi, lokasi tidak berakhiran {@code <id>.json} (artinya cache milik id lain —
	 * misal objek disalin atau id baru diberikan setelah insert), atau berkasnya sudah tidak
	 * ada di disk.</p>
	 *
	 * <p><b>Efek samping — penting:</b> method ini menulis ke disk dan mengubah
	 * {@link #fileLocation}. Karena {@link #getFileLocation()} adalah properti terpetakan,
	 * memanggil method ini atas objek yang masih persisten dapat memicu UPDATE baris dan satu
	 * revisi Envers, padahal alur pemanggilnya secara logika hanya membaca. Itulah yang
	 * terjadi di {@code KrsUtilHelper.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan}:
	 * setelah baris kuota ditemukan, path hasil method ini disimpan sebagai kunci cache
	 * sementara supaya pembacaan berikutnya tidak menyentuh basis data. Anotasi
	 * {@code @Transient} pada method ini mencegah Hibernate memanggilnya sendiri saat
	 * dirty-check.</p>
	 *
	 * @return path berkas cache JSON yang dijamin sudah diupayakan ada; bisa tetap merujuk
	 *         berkas yang tidak jadi ditulis bila kelas induk melewati penulisan.
	 */
	@javax.persistence.Transient
	public String getOrCreateFileLocation() {
		if (fileLocation == null || !fileLocation.endsWith(getId() + ".json")
				|| java.nio.file.Files.notExists(java.nio.file.Paths.get(fileLocation))) {
			write();
		}
		return fileLocation;
	}

	/**
	 * @param fileLocation path berkas cache JSON. Umumnya diisi {@link #write()}, bukan kode
	 *                     aplikasi; nilainya ikut tersimpan ke kolom {@code fileLocation}.
	 */
	public void setFileLocation(String fileLocation) {
		this.fileLocation = fileLocation;
	}
}
