package ais.database.model.kursus;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.json.JSONObject;

import ais.common.ConstantValues;
import ais.database.model.Pegawai;
import ais.database.model.StatusPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.Ujian;
import ais.database.model.VOPembelajaran;
import ais.database.model.library.Item;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>rincian komponen pembelajaran</b> di dalam satu {@link ProdukKursus}: satu baris
 * mewakili satu unit pembelajaran konkret (mis. satu buku wajib, satu ujian, atau satu sesi
 * pertemuan tatap muka/daring) yang menyusun isi sebuah produk kursus, lengkap dengan tutor,
 * durasi, harga, dan jadwal mulainya. Bila {@link ProdukKursus} adalah "apa yang dijual", entity
 * ini adalah "apa isinya" — daftar {@link KomponenDataProdukKursus} milik satu produk kursus
 * membentuk kurikulum/silabus konkret produk tersebut.
 *
 * <h3>Bukan mekanisme parameter tambahan generik</h3>
 * <p>Nama kelas ini ("komponen data") sekilas mengingatkan pada pola atribut dinamis generik
 * ({@code LampiranLain}/{@code ParameterTambahanAstract}) yang dipakai luas di modul lain AIS dan
 * pernah mengalami tabrakan namespace {@code jenis} (dicatat pada task_484d4bd0). <b>Kelas ini
 * TIDAK memakai mekanisme tersebut</b> — tidak ada import {@code LampiranLain} maupun
 * {@code ParameterTambahanAstract} di berkas ini. Sebagai gantinya, {@code komponenProdukKursus}
 * adalah field {@code String} biasa berisi salinan nilai konstanta {@link KomponenProdukKursus}
 * (mis. {@link KomponenProdukKursus#VIDEO}, {@link KomponenProdukKursus#PEMBELAJARAN_TATAP_MUKA}),
 * dan setiap "jenis" komponen (buku, ujian, pertemuan) diwakili oleh <b>field relasi khusus milik
 * kelas ini sendiri</b> ({@link #buku}, {@link #ujian}) — bukan lewat kolom {@code jenis} bersama
 * yang berpotensi bertabrakan dengan modul lain. Kesimpulan verifikasi: pola tabrakan namespace
 * {@code task_484d4bd0} <b>tidak berlaku</b> di sini; implementasinya independen.</p>
 *
 * <h3>Sumber nama/kode adalah relasi, bukan field-nya sendiri</h3>
 * <p>{@link #getKode()} dan {@link #getNama()} tidak semata membaca field {@link #kode}/{@link #nama}
 * miliknya sendiri: keduanya lebih dulu memuat ulang {@link #buku}/{@link #ujian} lewat getter
 * relasi (memicu {@code check(...)}), lalu — bila salah satu relasi terisi — menimpa field lokal
 * dengan nilai dari relasi tersebut ({@code Item.getIsbn()}/{@code Item.getNama()} untuk buku,
 * {@code Ujian.getKode()}/{@code Ujian.getNama()} untuk ujian) SEBELUM mengembalikannya. Efeknya:
 * kode/nama yang tampil selalu mengikuti buku/ujian yang sedang terpaut, bukan nilai yang mungkin
 * pernah diisi manual pada field {@code kode}/{@code nama} itu sendiri. Field lokal karena itu
 * hanya relevan untuk komponen yang jenisnya BUKAN buku maupun ujian (mis. video, latihan soal,
 * pertemuan tatap muka/jarak jauh, ekstra kurikuler — lihat {@link KomponenProdukKursus}) di mana
 * tidak ada relasi otoritatif untuk disalin.</p>
 *
 * <h3>Lima slot tutor tetap, bukan koleksi</h3>
 * <p>Komponen dapat diampu hingga lima tutor sekaligus, dimodelkan sebagai lima kolom
 * {@code ManyToOne} terpisah ({@link #tutor1}..{@link #tutor5}) alih-alih satu koleksi
 * {@code @OneToMany}/tabel jembatan. Ini pola kardinalitas-tetap yang berulang di banyak domain
 * AIS (bukan bug): {@link #populatePegawaiBuNama()} dan {@link #populatePegawai()} adalah cara
 * seragam mengakses kelima slot itu sebagai koleksi tanpa mengubah pemetaan kolomnya. Menambah
 * tutor ke-6 memerlukan migrasi skema, bukan sekadar perubahan kode Java.</p>
 *
 * <h3>Field "durasi" menyimpan jam, bukan tanggal</h3>
 * <p>{@link #getDurasi()} dipetakan {@code @Temporal(TemporalType.TIME)}: field bertipe {@code Date}
 * ini dipakai untuk menyimpan <b>lama waktu</b> (mis. "1 jam 30 menit") dengan menaruh angkanya pada
 * komponen jam:menit dari sebuah {@code Date}, bukan sebagai titik waktu kalender — nilai cadangan
 * saat kolom kosong sengaja dipaksa ke tengah malam ({@code 00:00:00}) hari ini, bukan waktu
 * pemanggilan, justru agar bagian tanggalnya tidak relevan/diabaikan pemanggil. Ini konvensi lama
 * (bukan bug) yang lazim dipakai lintas modul AIS untuk merepresentasikan durasi lewat tipe
 * {@code Date}/{@code TIME} alih-alih menit bulat.</p>
 *
 * <h3>Kelas dasar {@code VOPembelajaran}, bukan {@code GeneralValueObject} langsung</h3>
 * <p>Berbeda dari lima berkas lain di klaster ini, kelas ini {@code extends}
 * {@link ais.database.model.VOPembelajaran} (yang sendiri {@code extends}
 * {@link ais.database.model.VoKunci} {@code extends} {@link GeneralValueObject}). Ini mendaftarkan
 * komponen data produk kursus sebagai salah satu "pemilik pertemuan" yang sah di seluruh mesin
 * penjadwalan pertemuan lintas-modul milik {@code VOPembelajaran} — terlihat dari cabang
 * {@code this instanceof KomponenDataProdukKursus} pada restriksi query
 * {@code VOPembelajaran.reInitPertemuan}/{@code reInitTugas}/{@code reInitUjian}, yang menyaring
 * baris {@link Pertemuan} lewat kolom {@code komponenDataProdukKursus}. Konsekuensinya, entity ini
 * WAJIB mengimplementasikan empat method abstrak {@code VOPembelajaran}: {@link #getCourse()}/
 * {@link #setCourse(String)} (representasi JSON konten kelas) dan {@link #getUrutkanotomatis()}/
 * {@link #setUrutkanotomatis(Boolean)} (mode urut pertemuan otomatis vs manual), serta
 * {@link #ambilJumlahDetailperkuliahanLangsung()} yang di sini sekadar mengembalikan {@code 0}
 * (fitur detail-perkuliahan-langsung tidak berlaku untuk komponen kursus non-formal).</p>
 *
 * @see ProdukKursus produk kursus yang komponennya dirinci entity ini
 * @see KomponenProdukKursus master jenis komponen yang nilainya disalin ke {@link #komponenProdukKursus}
 * @see ais.database.model.VOPembelajaran mesin penjadwalan pertemuan lintas-modul yang mendaftarkan kelas ini
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "komponen_data_produk_kursus")
public class KomponenDataProdukKursus extends VOPembelajaran {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code komponen_data_produk_kursus}, dibangkitkan basis data (IDENTITY). */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah komponen data produk kursus ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Nilai {@code null}/kosong/spasi diabaikan diam-diam
	 * agar jejak audit yang sudah terisi tidak terhapus oleh jalur simpan tanpa identitas pengguna.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah komponen data produk kursus ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}



	/**
	 * Pengguna yang sedang "mengunci" baris ini untuk penyuntingan — pola lock kolaboratif yang
	 * ditemukan berulang pada entity AIS lain untuk mencegah dua pengguna menyunting komponen yang
	 * sama bersamaan. Kelas ini sendiri tidak menegakkan penolakan penyimpanan saat terkunci oleh
	 * pengguna lain; penegakannya (bila ada) berada di lapisan UI/action pemanggil.
	 */
	private Tbmuser dikunci;

	/**
	 * Mengembalikan pengguna yang sedang mengunci baris ini untuk penyuntingan. <b>Getter
	 * destruktif</b> ({@code check(...)}).
	 *
	 * @return pengguna pengunci, atau {@code null} bila tidak sedang terkunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menetapkan pengguna yang mengunci baris ini, atau melepas kunci dengan {@code null}.
	 *
	 * @param dikunci pengguna pengunci baru, atau {@code null} untuk melepas kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: mendelegasikan pencatatan stempel audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas komponen data produk kursus: {@code "id-nama"}. Karena
	 * {@link #getNama()} punya efek samping menyalin nama dari {@link #buku}/{@link #ujian}
	 * (lihat javadoc kelas), memanggil {@code toString()} pada object yang relasinya belum
	 * diinisialisasi turut memicu resolusi lazy kedua relasi tersebut.
	 *
	 * @return gabungan id dan nama komponen
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode komponen; lihat {@link #getKode()} untuk aturan penyalinan dari buku/ujian terpaut. */
	private String kode;
	/**
	 * Salinan nilai konstanta {@link KomponenProdukKursus} (mis. {@link KomponenProdukKursus#VIDEO},
	 * {@link KomponenProdukKursus#UJIAN}) yang menyatakan JENIS komponen pembelajaran ini. Field
	 * {@code String} biasa — bukan relasi {@code ManyToOne} ke {@link KomponenProdukKursus} — sehingga
	 * tidak ada penjaga integritas referensial yang mencegah nilai yang tidak pernah terdaftar pada
	 * {@link KomponenProdukKursus#s} tersimpan di sini.
	 */
	private String komponenProdukKursus;
	/** Nama komponen; lihat {@link #getNama()} untuk aturan penyalinan dari buku/ujian terpaut. */
	private String nama;
	/** Keterangan bebas komponen (kolom {@code keterangan}, bertipe {@code text}). */
	private String keterangan;
	/** Buku wajib/rujukan komponen ini, bila jenisnya buku/ebook; relasi opsional. */
	private Item buku;
	/** Ujian yang menjadi isi komponen ini, bila jenisnya ujian; relasi opsional. */
	private Ujian ujian;
	/** Status aktif/nonaktif komponen; {@code null} dianggap aktif oleh {@link #getAktif()}. */
	private Boolean aktif;
	/** Penanda harga komponen mengikuti nilai bawaan; {@code null} dianggap {@code true} oleh {@link #getHargaIkutDefault()}. */
	private Boolean hargaIkutDefault;
	/** Harga komponen bila tidak mengikuti bawaan; {@code null} dianggap {@code 0.0} oleh {@link #getHarga()}. */
	private Double harga;
	/** Tanggal mulai komponen; {@code null} dianggap tanggal hari ini oleh {@link #getMulai()}. */
	private Date mulai;
	/** Jumlah pertemuan komponen ini; {@code null} dianggap {@code 10} oleh {@link #getJumlahPertemuan()}. */
	private Integer jumlahPertemuan;
	/** Status pertemuan (tatap muka/daring) komponen ini; lihat {@link #getStatusPertemuan()} untuk turunan otomatisnya dari {@link #komponenProdukKursus}. */
	private StatusPertemuan statusPertemuan;
	/** Satuan kerja penyelenggara komponen ini; relasi opsional. */
	private SatuanKerja satuanKerja;
	/** Representasi JSON konten kelas (override kontrak abstrak {@code VOPembelajaran}); lihat {@link #getCourse()}. */
	private String course;
	/** Lama waktu (durasi) komponen, disimpan lewat komponen jam:menit sebuah {@code Date}; lihat javadoc kelas. */
	private Date durasi;

	/** Tutor pertama komponen ini; salah satu dari lima slot tutor tetap — lihat javadoc kelas. */
	private Pegawai tutor1;
	/** Tutor kedua komponen ini. */
	private Pegawai tutor2;
	/** Tutor ketiga komponen ini. */
	private Pegawai tutor3;
	/** Tutor keempat komponen ini. */
	private Pegawai tutor4;
	/** Tutor kelima komponen ini. */
	private Pegawai tutor5;
	/** Mode urut pertemuan otomatis (override kontrak abstrak {@code VOPembelajaran}); lihat {@link #getUrutkanotomatis()}. */
	private Boolean urutkanotomatis;

	/**
	 * Mengumpulkan kelima slot tutor ({@link #tutor1}..{@link #tutor5}) yang terisi menjadi satu
	 * {@link List}, dalam urutan tutor1→tutor5, melewati slot yang {@code null}. Setiap pemanggilan
	 * membangun list baru dan memicu resolusi lazy ({@code check(...)}) kelima getter tutor lewat
	 * pemanggilan {@link #getTutor1()}..{@link #getTutor5()}.
	 *
	 * @return daftar tutor yang terisi, urut slot; tidak pernah {@code null}, boleh kosong
	 */
	public List<Pegawai> populatePegawaiBuNama() {
		List<Pegawai> pegawais = new ArrayList<Pegawai>();

		if (getTutor1() != null) {
			pegawais.add(getTutor1());
		}
		if (getTutor2() != null) {
			pegawais.add(getTutor2());
		}
		if (getTutor3() != null) {
			pegawais.add(getTutor3());
		}
		if (getTutor4() != null) {
			pegawais.add(getTutor4());
		}
		if (getTutor5() != null) {
			pegawais.add(getTutor5());
		}

		return pegawais;
	}

	/**
	 * Mengumpulkan kelima slot tutor yang terisi menjadi satu {@link Map}, dengan kunci
	 * {@code "<idKomponen>-<idPegawai>"} agar unik lintas komponen ketika beberapa peta hasil
	 * method ini digabung oleh pemanggil (mis. direduksi ke satu peta gabungan seluruh komponen
	 * produk kursus). Nilai peta adalah object {@link Pegawai} yang sama seperti
	 * {@link #populatePegawaiBuNama()}; hanya representasi wadahnya yang berbeda.
	 *
	 * @return peta tutor yang terisi berkunci {@code "id-idPegawai"}; tidak pernah {@code null}, boleh kosong
	 */
	public Map<String, Pegawai> populatePegawai() {
		Map<String, Pegawai> pegawais = new HashMap<String, Pegawai>();

		if (getTutor1() != null) {
			pegawais.put(getId() + "-" + getTutor1().getId(), getTutor1());
		}
		if (getTutor2() != null) {
			pegawais.put(getId() + "-" + getTutor2().getId(), getTutor2());
		}
		if (getTutor3() != null) {
			pegawais.put(getId() + "-" + getTutor3().getId(), getTutor3());
		}
		if (getTutor4() != null) {
			pegawais.put(getId() + "-" + getTutor4().getId(), getTutor4());
		}
		if (getTutor5() != null) {
			pegawais.put(getId() + "-" + getTutor5().getId(), getTutor5());
		}

		return pegawais;
	}

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public KomponenDataProdukKursus() {
	}

	/**
	 * Mengembalikan primary key komponen data produk kursus.
	 *
	 * @return primary key, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; normalnya diisi otomatis oleh Hibernate.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode komponen. <b>Getter destruktif dengan sumber ganda</b>: sebelum
	 * mengembalikan nilai, method ini memuat ulang {@link #buku}/{@link #ujian} lewat
	 * {@link #getBuku()}/{@link #getUjian()} (memicu resolusi lazy), lalu — bila {@link #buku}
	 * terisi — menimpa field {@link #kode} dengan {@code Item.getIsbn()}, atau bila {@link #ujian}
	 * terisi — dengan {@code Ujian.getKode()}. Field {@link #kode} yang tersimpan sendiri karena
	 * itu hanya "menang" untuk komponen yang bukan buku maupun ujian (video, latihan soal,
	 * pertemuan, ekstra kurikuler). Efek penulisan-balik ini terjadi pada objek in-memory; nilai
	 * baru hanya benar-benar tersimpan ke basis data bila objek kemudian di-{@code save}/{@code update}.
	 *
	 * @return kode komponen (ISBN buku, kode ujian, atau {@link #kode} field), tidak pernah {@code null}
	 */
	public String getKode() {
		buku = getBuku();
		ujian = getUjian();

		if (buku != null) {
			kode = buku.getIsbn();
		} else if (ujian != null) {
			kode = ujian.getKode();
		}

		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode komponen secara manual. Perhatikan bahwa {@link #getKode()} akan menimpa nilai
	 * ini pada pemanggilan berikutnya bila {@link #buku} atau {@link #ujian} terisi — setter ini
	 * karena itu hanya efektif untuk komponen tanpa relasi buku/ujian.
	 *
	 * @param kode kode komponen baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama komponen, dengan aturan penyalinan sumber ganda yang identik dengan
	 * {@link #getKode()}: bila {@link #buku} terisi, nama disalin dari {@code Item.getNama()}; bila
	 * {@link #ujian} terisi, dari {@code Ujian.getNama()}. Hasil akhirnya dipangkas spasi tepi.
	 *
	 * @return nama komponen (dipangkas), atau {@code null} bila field {@link #nama} maupun kedua
	 *         relasi sumber kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		buku = getBuku();
		ujian = getUjian();

		if (buku != null) {
			nama = buku.getNama();
		} else if (ujian != null) {
			nama = ujian.getNama();
		}

		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama komponen secara manual. Sama seperti {@link #setKode(String)}, nilai ini dapat
	 * ditimpa oleh {@link #getNama()} bila relasi buku/ujian terisi.
	 *
	 * @param nama nama komponen baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas komponen. Getter murni-baca, tanpa normalisasi.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas komponen.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif/nonaktif komponen, menormalkan {@code null} menjadi {@code true}.
	 *
	 * @return {@code true} bila komponen aktif, tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyalakan atau mematikan komponen.
	 *
	 * @param aktif {@code true} bila komponen aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan salinan nilai konstanta {@link KomponenProdukKursus} yang menyatakan jenis
	 * komponen ini (mis. {@link KomponenProdukKursus#VIDEO}). Getter murni-baca; dapat mengembalikan
	 * {@code null} walau kolomnya {@code nullable = false} pada basis data, untuk object baru yang
	 * belum pernah diisi. <b>Perhatian:</b> {@link #getStatusPertemuan()} memanggil
	 * {@code .equals(...)} langsung atas field mentah {@link #komponenProdukKursus} (bukan lewat
	 * getter ini) tanpa pemeriksaan {@code null} lebih dulu — lihat javadoc method tersebut.
	 *
	 * @return jenis komponen, atau {@code null} bila belum diisi
	 */
	@Column(name = "komponen_produk_kursus", nullable = false, length = 255)
	public String getKomponenProdukKursus() {
		return komponenProdukKursus;
	}

	/**
	 * Mengisi jenis komponen. Tanpa validasi bahwa nilainya termasuk salah satu konstanta terdaftar
	 * pada {@link KomponenProdukKursus#s}.
	 *
	 * @param komponenProdukKursus jenis komponen baru
	 */
	public void setKomponenProdukKursus(String komponenProdukKursus) {
		this.komponenProdukKursus = komponenProdukKursus;
	}

	/**
	 * Mengembalikan buku wajib/rujukan komponen ini. <b>Getter destruktif</b> ({@code check(...)}).
	 *
	 * @return buku terpaut, atau {@code null} bila jenis komponen ini bukan buku/ebook
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "buku", nullable = true)
	public Item getBuku() {
		buku = check(buku);
		return buku;
	}

	/**
	 * Menetapkan buku wajib/rujukan komponen ini.
	 *
	 * @param buku buku baru, atau {@code null} untuk melepas relasi
	 */
	public void setBuku(Item buku) {
		this.buku = buku;
	}

	/**
	 * Mengembalikan ujian yang menjadi isi komponen ini. <b>Getter destruktif</b> ({@code check(...)}).
	 *
	 * @return ujian terpaut, atau {@code null} bila jenis komponen ini bukan ujian
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ujian", nullable = true)
	public Ujian getUjian() {
		ujian = check(ujian);
		return ujian;
	}

	/**
	 * Menetapkan ujian yang menjadi isi komponen ini.
	 *
	 * @param ujian ujian baru, atau {@code null} untuk melepas relasi
	 */
	public void setUjian(Ujian ujian) {
		this.ujian = ujian;
	}

	/**
	 * Mengembalikan penanda apakah harga komponen mengikuti nilai bawaan (mis. harga bawaan
	 * {@link ProdukKursus}/{@link KomponenProdukKursus}), menormalkan {@code null} menjadi
	 * {@code true}. Kelas ini sendiri tidak mendefinisikan APA nilai bawaan tersebut maupun logika
	 * penerapannya — bendera ini murni penanda yang maknanya ditentukan lapisan pemanggil.
	 *
	 * @return {@code true} bila harga mengikuti bawaan, tidak pernah {@code null}
	 */
	public Boolean getHargaIkutDefault() {
		return hargaIkutDefault == null ? true : hargaIkutDefault;
	}

	/**
	 * Menyalakan atau mematikan penanda harga-ikut-default.
	 *
	 * @param hargaIkutDefault {@code true} bila harga mengikuti bawaan
	 */
	public void setHargaIkutDefault(Boolean hargaIkutDefault) {
		this.hargaIkutDefault = hargaIkutDefault;
	}

	/**
	 * Mengembalikan harga komponen ini (dipakai bila {@link #getHargaIkutDefault()} bernilai
	 * {@code false}), menormalkan {@code null} menjadi {@code 0.0}.
	 *
	 * @return harga komponen, tidak pernah {@code null}
	 */
	public Double getHarga() {
		return harga == null ? 0.0 : harga;
	}

	/**
	 * Mengisi harga komponen ini. Tanpa validasi non-negatif.
	 *
	 * @param harga harga baru
	 */
	public void setHarga(Double harga) {
		this.harga = harga;
	}

	/**
	 * Mengembalikan tanggal mulai komponen, dipetakan {@code DATE}. Sama seperti
	 * {@link ProdukKursus#getMulai()}, method ini mengembalikan {@code WaktuUtil.getDate()} — waktu
	 * SAAT dipanggil — bila kolom kosong, bukan nilai tersimpan tetap; lihat javadoc method sejenis
	 * pada {@link ProdukKursus} untuk implikasi lengkapnya (dua pemanggilan berbeda hari dapat
	 * mengembalikan tanggal berbeda, dan nilai ini tidak ditulis balik ke field).
	 *
	 * @return tanggal mulai tersimpan, atau tanggal/waktu saat ini bila kolom kosong
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai == null ? WaktuUtil.getDate() : mulai;
	}

	/**
	 * Mengisi tanggal mulai komponen.
	 *
	 * @param mulai tanggal mulai baru
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan jumlah pertemuan komponen ini, menormalkan {@code null} menjadi {@code 10}
	 * (nilai bawaan sepuluh pertemuan bila operator belum menentukan).
	 *
	 * @return jumlah pertemuan, tidak pernah {@code null}
	 */
	public Integer getJumlahPertemuan() {
		return jumlahPertemuan == null ? 10 : jumlahPertemuan;
	}

	/**
	 * Mengisi jumlah pertemuan komponen ini.
	 *
	 * @param jumlahPertemuan jumlah pertemuan baru
	 */
	public void setJumlahPertemuan(Integer jumlahPertemuan) {
		this.jumlahPertemuan = jumlahPertemuan;
	}

	/**
	 * Mengembalikan status pertemuan (tatap muka/daring) komponen ini, dengan turunan otomatis dari
	 * jenis komponen: bila {@link #komponenProdukKursus} sama dengan
	 * {@link KomponenProdukKursus#PEMBELAJARAN_TATAP_MUKA}, hasilnya dipaksa
	 * {@code ConstantValues.TATAP_MUKA}; bila sama dengan
	 * {@link KomponenProdukKursus#PEMBELAJARAN_JARAK_JAUH}, dipaksa {@code ConstantValues.DARING};
	 * untuk jenis lain, method jatuh ke field {@link #statusPertemuan} tersimpan lewat
	 * {@code check(...)} (getter destruktif standar).
	 *
	 * <p><b>Risiko {@code NullPointerException}:</b> perbandingan jenis komponen memanggil
	 * {@code komponenProdukKursus.equals(...)} langsung atas field String mentah, <b>tanpa</b>
	 * pemeriksaan {@code null} terlebih dahulu (berbeda dari kebanyakan getter lain di kelas ini
	 * yang selalu menormalkan {@code null}). Kolom {@code komponen_produk_kursus} memang
	 * {@code nullable = false} pada basis data sehingga baris yang sudah tersimpan seharusnya selalu
	 * terisi, tetapi method ini akan melempar {@code NullPointerException} bila dipanggil pada
	 * object transient yang field-nya belum pernah diisi (mis. {@code new KomponenDataProdukKursus()}
	 * lalu langsung memanggil getter ini sebelum {@link #setKomponenProdukKursus(String)}).
	 *
	 * @return status pertemuan efektif, dapat {@code null} bila jenis komponen bukan tatap
	 *         muka/jarak jauh dan field {@link #statusPertemuan} juga belum diisi
	 * @throws NullPointerException bila {@link #komponenProdukKursus} bernilai {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_pertemuan", nullable = true)
	public StatusPertemuan getStatusPertemuan() {

		if (komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA)) {
			statusPertemuan = ConstantValues.TATAP_MUKA;
		} else if (komponenProdukKursus.equals(KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH)) {
			statusPertemuan = ConstantValues.DARING;
		} else {
			statusPertemuan = check(statusPertemuan);
		}

		return statusPertemuan;
	}

	/**
	 * Mengisi status pertemuan komponen ini secara manual. Perhatikan bahwa nilai ini ditimpa oleh
	 * {@link #getStatusPertemuan()} bila jenis komponen adalah tatap muka atau jarak jauh.
	 *
	 * @param statusPertemuan status pertemuan baru
	 */
	public void setStatusPertemuan(StatusPertemuan statusPertemuan) {
		this.statusPertemuan = statusPertemuan;
	}

	/**
	 * Mengembalikan satuan kerja penyelenggara komponen ini. <b>Getter destruktif</b>
	 * ({@code check(...)}).
	 *
	 * @return satuan kerja, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja penyelenggara komponen ini.
	 *
	 * @param satuanKerja satuan kerja baru, atau {@code null} untuk melepas relasi
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Implementasi kontrak abstrak {@link ais.database.model.VOPembelajaran#getCourse()}:
	 * mengembalikan representasi JSON konten kelas komponen ini, menormalkan {@code null}/kosong
	 * menjadi objek JSON kosong ({@code "{}"}) alih-alih {@code null}, sehingga pemanggil selalu
	 * menerima JSON yang bisa langsung di-parse.
	 *
	 * @return representasi JSON konten kelas, tidak pernah {@code null}/kosong
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/**
	 * Implementasi kontrak abstrak {@link ais.database.model.VOPembelajaran#setCourse(String)}:
	 * mengisi representasi JSON konten kelas komponen ini.
	 *
	 * @param course JSON konten kelas baru
	 */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * Implementasi kontrak abstrak
	 * {@link ais.database.model.VOPembelajaran#ambilJumlahDetailperkuliahanLangsung()}: selalu
	 * mengembalikan {@code 0} karena fitur "detail perkuliahan langsung" (dipakai domain
	 * perkuliahan/mahasiswa) tidak berlaku bagi komponen kursus non-formal.
	 *
	 * @return selalu {@code 0}
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Mengembalikan lama waktu (durasi) komponen ini, dipetakan {@code @Temporal(TemporalType.TIME)}
	 * — field {@code Date} ini menyimpan durasi lewat komponen jam:menit, bukan titik waktu kalender
	 * (lihat javadoc kelas). Bila kolom kosong, nilai cadangan dibangun sebagai tengah malam
	 * ({@code 00:00:00}) tanggal hari ini <b>dan ditulis balik ke field</b> {@link #durasi} — berbeda
	 * dari {@link #getMulai()}/{@link ProdukKursus#getMulai()} yang nilai cadangannya tidak
	 * persisten pada objek in-memory.
	 *
	 * @return durasi tersimpan, atau {@code Date} bertanda jam 00:00 bila kolom kosong
	 */
	@Temporal(TemporalType.TIME)
	public Date getDurasi() {
		if (durasi == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.HOUR_OF_DAY, 0);

			durasi = calendar.getTime();
		}
		return durasi;
	}

	/**
	 * Mengisi lama waktu (durasi) komponen ini.
	 *
	 * @param durasi durasi baru (komponen jam:menit dari {@code Date} yang bermakna)
	 */
	public void setDurasi(Date durasi) {
		this.durasi = durasi;
	}

	/**
	 * Mengembalikan tutor pada slot pertama. <b>Getter destruktif</b> ({@code check(...)}).
	 *
	 * @return tutor slot pertama, atau {@code null} bila slot ini kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tutor1", nullable = true)
	public Pegawai getTutor1() {
		tutor1 = check(tutor1);
		return tutor1;
	}

	/**
	 * Menetapkan tutor pada slot pertama.
	 *
	 * @param tutor1 tutor baru, atau {@code null} untuk mengosongkan slot
	 */
	public void setTutor1(Pegawai tutor1) {
		this.tutor1 = tutor1;
	}

	/**
	 * Mengembalikan tutor pada slot kedua. <b>Getter destruktif</b> ({@code check(...)}).
	 *
	 * @return tutor slot kedua, atau {@code null} bila slot ini kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tutor2", nullable = true)
	public Pegawai getTutor2() {
		tutor2 = check(tutor2);
		return tutor2;
	}

	/**
	 * Menetapkan tutor pada slot kedua.
	 *
	 * @param tutor2 tutor baru, atau {@code null} untuk mengosongkan slot
	 */
	public void setTutor2(Pegawai tutor2) {
		this.tutor2 = tutor2;
	}

	/**
	 * Mengembalikan tutor pada slot ketiga. <b>Getter destruktif</b> ({@code check(...)}).
	 *
	 * @return tutor slot ketiga, atau {@code null} bila slot ini kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tutor3", nullable = true)
	public Pegawai getTutor3() {
		tutor3 = check(tutor3);
		return tutor3;
	}

	/**
	 * Menetapkan tutor pada slot ketiga.
	 *
	 * @param tutor3 tutor baru, atau {@code null} untuk mengosongkan slot
	 */
	public void setTutor3(Pegawai tutor3) {
		this.tutor3 = tutor3;
	}

	/**
	 * Mengembalikan tutor pada slot keempat. <b>Getter destruktif</b> ({@code check(...)}).
	 *
	 * @return tutor slot keempat, atau {@code null} bila slot ini kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tutor4", nullable = true)
	public Pegawai getTutor4() {
		tutor4 = check(tutor4);
		return tutor4;
	}

	/**
	 * Menetapkan tutor pada slot keempat.
	 *
	 * @param tutor4 tutor baru, atau {@code null} untuk mengosongkan slot
	 */
	public void setTutor4(Pegawai tutor4) {
		this.tutor4 = tutor4;
	}

	/**
	 * Mengembalikan tutor pada slot kelima. <b>Getter destruktif</b> ({@code check(...)}).
	 *
	 * @return tutor slot kelima, atau {@code null} bila slot ini kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tutor5", nullable = true)
	public Pegawai getTutor5() {
		tutor5 = check(tutor5);
		return tutor5;
	}

	/**
	 * Menetapkan tutor pada slot kelima.
	 *
	 * @param tutor5 tutor baru, atau {@code null} untuk mengosongkan slot
	 */
	public void setTutor5(Pegawai tutor5) {
		this.tutor5 = tutor5;
	}

	/**
	 * Implementasi kontrak abstrak {@link ais.database.model.VOPembelajaran#getUrutkanotomatis()}:
	 * mengembalikan mode urut pertemuan otomatis milik komponen ini, menormalkan {@code null}
	 * menjadi {@code true}. Dipakai lintas-modul oleh mesin penjadwalan
	 * {@code VOPembelajaran.reInitPertemuan}/{@code reInitTugas} untuk menentukan apakah pertemuan
	 * diurutkan otomatis berdasarkan tanggal atau memakai nomor {@code pertemuanKe} manual.
	 *
	 * @return {@code true} bila pertemuan diurutkan otomatis, tidak pernah {@code null}
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/**
	 * Implementasi kontrak abstrak {@link ais.database.model.VOPembelajaran#setUrutkanotomatis(Boolean)}:
	 * menyalakan atau mematikan mode urut pertemuan otomatis komponen ini.
	 *
	 * @param urutkanotomatis {@code true} bila pertemuan diurutkan otomatis
	 */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}
}
