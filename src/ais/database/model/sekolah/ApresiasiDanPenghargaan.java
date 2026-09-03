package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity MASTER "paket" pemetaan apresiasi &harr; penghargaan pada modul sekolah
 * (tabel {@code sekolah.apresiasi_dan_penghargaan}).
 *
 * <h2>Apa ini SEBENARNYA (hasil verifikasi kode, bukan tebakan dari nama kelas)</h2>
 * <p>Nama kelas terdengar seperti "catatan apresiasi dan penghargaan yang diterima seseorang",
 * tetapi <b>bukan itu perannya</b>. Kelas ini <b>tidak punya relasi ke {@code Siswa} maupun
 * {@code Guru}</b>, tidak punya kolom tanggal kejadian, dan tidak punya kolom nilai/poin.
 * Yang dimilikinya hanyalah nama paket, cakupan tenant, dan <b>dua koleksi
 * {@code @ManyToMany} ke dua tabel master yang benar-benar terpisah</b>. Dengan kata lain
 * baris di sini adalah sebuah <i>template</i>/<i>paket</i>: "Jenis Apresiasi Dan Penghargaan"
 * (persis judul dialog di {@code ApresiasiDanPenghargaanAction}) yang menetapkan
 * <b>butir apresiasi apa saja</b> dan <b>bentuk penghargaan apa saja</b> yang boleh dipilih
 * ketika petugas nanti mencatat penerimaannya untuk seorang siswa.</p>
 *
 * <p>Struktur ini <b>sama persis</b> dengan
 * {@link ais.database.model.sekolah.PelanggaranDanHukuman} (sisi negatif/disiplin) — dua
 * {@code @ManyToMany} ke dua master berbeda, tanpa satu pun relasi ke subjek. Keluarga ini
 * adalah cerminan positif dari keluarga pelanggaran, dengan rantai empat lapis yang sejajar:</p>
 * <ol>
 *   <li><b>Master butir</b> — Apresiasi (kelas {@code Apresiasi}, kolom {@code kredit}) dan
 *       {@link ais.database.model.sekolah.Penghargaan} (kolom {@code poin}). Padanan sisi
 *       negatif: {@link ais.database.model.sekolah.Pelanggaran} dan
 *       {@link ais.database.model.sekolah.Hukuman}.</li>
 *   <li><b>Master paket</b> — <i>kelas ini</i>: memetakan sekumpulan butir Apresiasi ke
 *       sekumpulan butir {@code Penghargaan}. Padanan: {@code PelanggaranDanHukuman}.</li>
 *   <li><b>Transaksi per siswa</b> — {@link ais.database.model.sekolah.ApresiasiSiswa}:
 *       menunjuk satu {@code Siswa}, satu paket (kelas ini), tanggal ({@code waktu}), tahun
 *       ajaran, dan <b>salinan pilihan</b> butir apresiasi/penghargaan yang benar-benar
 *       diberikan (dua {@code @ManyToMany} sendiri ke tabel
 *       {@code apresiasi_siswa_has_apresiasi} dan {@code apresiasi_siswa_has_penghargaan}).
 *       Padanan: {@code PelanggaranSiswa}.</li>
 *   <li><b>Pelaporan</b> — {@code LaporanRaporSiswa} merangkum {@code ApresiasiSiswa} per siswa
 *       dan menjumlahkan {@code poin} penghargaan untuk dicetak di rapor.</li>
 * </ol>
 *
 * <p>Perhatikan bahwa <b>angka tidak disimpan di sini</b>: {@code kredit} milik butir apresiasi
 * dan {@code poin} milik butir penghargaan hidup di kedua master butir, bukan di paket. Paket
 * hanya menentukan <i>daftar pilihan</i>.</p>
 *
 * <h2>Siapa yang memakai baris ini</h2>
 * <ul>
 *   <li>{@code ais.action.master.sekolah.ApresiasiDanPenghargaanAction} — layar master
 *       (grid + dialog tambah/ubah). Di dialog, dua blok checkbox mengisi
 *       {@link #getApresiasis()} dan {@link #getPenghargaans()}.</li>
 *   <li>{@code ais.action.master.sekolah.ApresiasiSiswaAction} — layar transaksi. Combobox
 *       "Jenis apresiasi" memilih satu paket, lalu {@code loadApresiasi()}/{@code loadPenghargaan()}
 *       <b>menurunkan daftar checkbox</b> dari kedua koleksi paket tersebut. Jadi paket inilah
 *       yang menentukan apa yang boleh dicentang petugas.</li>
 *   <li>{@code ais.action.report.format1.sekolah.LaporanRaporSiswa} — hanya membaca
 *       {@link #getNama()} sebagai label baris apresiasi di rapor.</li>
 *   <li>{@code ais.action.master.apresiasi.DasbordApresiasi} — menampilkan {@link #getNama()}
 *       sebagai kategori "jenis" pada agregasi dasbor.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, callback {@link #onUpdate()},
 *       plus anotasi {@link Audited} (Envers menulis tabel riwayat).</li>
 *   <li><b>Cakupan tenant</b> — {@link #getSekolah()}, {@link #getYayasan()}.</li>
 *   <li><b>Deskripsi paket</b> — {@link #getNama()}, {@link #getKeterangan()},
 *       {@link #getAktif()}.</li>
 *   <li><b>Isi paket</b> — {@link #getApresiasis()}, {@link #getPenghargaans()}.</li>
 *   <li><b>Konstruktor</b> — {@link #ApresiasiDanPenghargaan()} (wajib untuk Hibernate) dan
 *       {@link #ApresiasiDanPenghargaan(long, String)} (peninggalan hbm2java, tidak dipakai
 *       kode aplikasi).</li>
 * </ul>
 *
 * <h2>Catatan pemetaan yang tidak kentara</h2>
 * <ul>
 *   <li><b>Field {@code oleh}/{@code olehId}/{@code tanggal_dirubah}/{@code id} sengaja
 *       dideklarasikan ULANG di kelas ini, dan itu BUKAN duplikasi yang bisa dihapus.</b>
 *       {@link GeneralValueObject} adalah POJO abstrak biasa — <b>bukan</b> {@code @Entity}
 *       maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti
 *       induknya sama sekali. Tanpa deklarasi ulang di sini, keempat kolom itu tidak akan
 *       pernah tersimpan. Lihat {@link GeneralValueObject} untuk uraian lengkapnya.</li>
 *   <li>Akses Hibernate di sini adalah <b>property access</b> (anotasi menempel di getter),
 *       jadi setiap getter yang menulis balik ke field ikut menentukan apa yang di-{@code flush}
 *       ke database. {@link #getYayasan()} adalah contohnya — lihat dokumentasi method itu.</li>
 *   <li>{@code aktif} <b>tidak</b> beranotasi {@code @Column}, jadi ia jatuh ke nama kolom
 *       bawaan {@code aktif}. Ia tetap dipetakan (bukan {@code @Transient}).</li>
 *   <li>Kedua {@code @ManyToMany} memakai {@code cascade = MERGE} saja: menyimpan paket
 *       <b>tidak</b> membuat baris master butir baru, hanya baris di tabel jembatan.
 *       Tidak ada {@code CascadeType.REMOVE}, sehingga menghapus paket tidak menghapus
 *       butir masternya (yang memang benar untuk relasi master&harr;master).</li>
 *   <li>{@code @OrderBy("nama asc")} pada kedua koleksi hanya menambahkan {@code ORDER BY} pada
 *       SQL pemuatan; karena tipe field-nya {@link HashSet}, urutan itu <b>tidak terjaga</b>
 *       setelah masuk memori. Pemanggil yang butuh urutan harus mengurutkan sendiri (dan
 *       {@code ApresiasiDanPenghargaanAction} memang melakukannya, dengan efek samping yang
 *       dijelaskan di {@link #getApresiasis()}).</li>
 *   <li>{@code serialVersionUID} di sini bernilai sama persis dengan milik {@code Apresiasi},
 *       {@code Penghargaan}, dan {@code ApresiasiSiswa} — hasil salin-tempel generator, bukan
 *       tanda bahwa kelas-kelas itu kompatibel secara serialisasi.</li>
 * </ul>
 *
 * <h2>Hasil pemeriksaan pola berulang yang sudah dikenal di codebase ini</h2>
 * <ul>
 *   <li><b>Getter yang menulis balik (destruktif) — ADA</b>, pada {@link #getYayasan()}:
 *       setiap kali paket dibaca, {@code yayasan} ditimpa dari {@code sekolah.getYayasan()}.
 *       {@link #getSekolah()} juga menulis balik, tetapi hanya hasil resolusi proxy
 *       ({@code check(...)}), bukan nilai lain.</li>
 *   <li><b>{@code getKeterangan()} membalik kontrak kelas induk — ADA.</b>
 *       {@link GeneralValueObject#getKeterangan()} dijamin tidak pernah mengembalikan
 *       {@code null} (mengembalikan {@code ""}), tetapi {@link #getKeterangan()} di kelas ini
 *       meng-override-nya dan mengembalikan field mentah yang <b>bisa {@code null}</b>.
 *       Akibat lanjutannya dijelaskan pada method tersebut.</li>
 *   <li><b>Penciutan senyap oleh {@code TreeSet} — ADA (versi ringan)</b>, bukan di kelas ini
 *       melainkan pada pembacanya; lihat {@link #getApresiasis()}.</li>
 *   <li><b>{@code compareTo()} yang dipangkas — TIDAK ADA di kelas ini</b>: kelas ini tidak
 *       meng-override {@code compareTo()}, jadi berlaku implementasi berjenjang milik
 *       {@link GeneralValueObject#compareTo(GeneralValueObject)}.</li>
 *   <li><b>Cakupan tenant yang fail-open — ADA di lapis pemakai.</b> Ringkasnya:
 *       {@code ApresiasiDanPenghargaanAction.initCriteria()} sama sekali tidak menyaring paket
 *       menurut sekolah/yayasan pengguna yang login (hanya menurut isi combobox pencarian),
 *       dan daftar checkbox butir Apresiasi/{@code Penghargaan} di dialognya di-query
 *       <b>tanpa syarat tenant apa pun</b> — hanya {@code aktif}. Pola dan temuannya identik
 *       dengan yang sudah tercatat untuk {@code PelanggaranDanHukuman}.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see ais.database.model.sekolah.Penghargaan
 * @see ais.database.model.sekolah.ApresiasiSiswa
 * @see ais.database.model.sekolah.PelanggaranDanHukuman
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "apresiasi_dan_penghargaan", schema = "sekolah")
public class ApresiasiDanPenghargaan extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya diwarisi apa adanya dari template generator hbm2java dan <b>identik</b> dengan
	 * milik {@code Apresiasi}, {@link ais.database.model.sekolah.Penghargaan}, dan
	 * {@link ais.database.model.sekolah.ApresiasiSiswa}. Kesamaan itu kebetulan salin-tempel;
	 * jangan disimpulkan sebagai jaminan kompatibilitas antar kelas.</p>
	 */
	private static final long serialVersionUID = -7490758846785025664L;

	/**
	 * Kunci primer paket, kolom {@code id} ({@code IDENTITY}/auto increment).
	 *
	 * <p>Dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; lihat catatan pada dokumentasi kelas.</p>
	 */
	private Long id;

	/** Nama pengguna yang terakhir mengubah baris; diisi otomatis oleh {@link #onUpdate()}. */
	private String oleh;

	/** Id pengguna yang terakhir mengubah baris; diisi otomatis oleh {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila baris belum pernah diubah
	 *         lewat jalur yang memicu {@link #onUpdate()}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan perilaku <b>"abaikan nilai kosong"</b>.
	 *
	 * <p>Bila {@code olehId} bernilai {@code null} atau hanya berisi spasi, method langsung
	 * {@code return} dan <b>nilai lama dipertahankan</b>. Konsekuensinya: jejak audit tidak bisa
	 * "dikosongkan" secara tidak sengaja, tetapi juga tidak bisa direset secara sengaja lewat
	 * setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/blanko diabaikan tanpa efek
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Perilaku "abaikan nilai kosong" persis sama dengan {@link #setOlehId(String)} — lihat
	 * penjelasan di sana.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/blanko diabaikan tanpa efek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} + deklarasi field {@code tanggal_dirubah} — keduanya
	 * berbagi satu baris fisik karena bentukan generator; jangan dipisah tanpa alasan kuat
	 * agar diff terhadap berkas sekerabat tetap mudah dibaca.
	 *
	 * <p><b>Callback:</b> Hibernate memanggil {@code onUpdate()} TEPAT SEBELUM setiap
	 * {@code UPDATE} baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna aktif. <b>Jangan dipanggil
	 * manual</b> — ini titik masuk milik provider persistence.</p>
	 *
	 * <p>Perlu diingat entity ini punya getter yang menulis balik ({@link #getYayasan()}),
	 * sehingga {@code UPDATE} — dan karenanya callback ini — bisa terpicu bahkan pada alur yang
	 * secara logis hanya "membaca" paket.</p>
	 *
	 * <p><b>Field:</b> {@code tanggal_dirubah} diinisialisasi ke waktu server saat object
	 * dibuat ({@code ais.ui.util.WaktuUtil.getDate()}), sehingga baris baru selalu punya
	 * stempel waktu walaupun belum pernah melewati {@code @PreUpdate}. Tidak ada callback
	 * {@code @PrePersist} di kelas ini.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; {@code null} diterima apa adanya.
	 *
	 * <p>Umumnya dipanggil oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()},
	 * bukan oleh kode layar.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi
	 * {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang baru dibuat
	 *         di memori, tetapi bisa {@code null} bila kolomnya kosong di database
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Sekolah pemilik paket (kolom {@code sekolah_id}); lihat {@link #getSekolah()}. */
	private Sekolah sekolah;

	/**
	 * Yayasan pemilik paket (kolom {@code yayasan_id}). Nilainya <b>diturunkan ulang</b> dari
	 * {@link #sekolah} setiap kali {@link #getYayasan()} dipanggil.
	 */
	private Yayasan yayasan;

	/** Keterangan bebas tentang paket; lihat {@link #getKeterangan()} soal kontrak {@code null}. */
	private String keterangan;

	/** Nama paket, wajib diisi (kolom {@code nama}, {@code nullable = false}). */
	private String nama;

	/**
	 * Penanda paket masih dipakai. Dibaca lewat {@link #getAktif()} yang memperlakukan
	 * {@code null} sebagai {@code true}.
	 */
	private Boolean aktif;

	/** Butir penghargaan yang tercakup paket ini; lihat {@link #getPenghargaans()}. */
	private Set<Penghargaan> penghargaans = new HashSet<Penghargaan>();

	/**
	 * Mengembalikan himpunan butir {@link Penghargaan} yang tercakup paket ini (tabel jembatan
	 * {@code sekolah.apresiasi_dan_penghargaan_has_penghargaan}).
	 *
	 * <p><b>Peran fungsional.</b> Inilah daftar yang muncul sebagai checkbox "Penghargaan" pada
	 * layar transaksi {@code ApresiasiSiswaAction}: begitu petugas memilih sebuah paket,
	 * {@code loadPenghargaan()} melakukan {@code session.refresh(paket)} lalu mengiterasi
	 * koleksi ini. Butir penghargaan yang <b>tidak</b> ada di paket tidak akan pernah bisa
	 * dipilih untuk siswa lewat paket tersebut. Label checkbox-nya juga menampilkan
	 * {@code poin} butir bila lebih besar dari 0,1.</p>
	 *
	 * <p><b>Yang dikembalikan adalah koleksi terkelola, bukan salinan.</b> Untuk entity yang
	 * masih terikat session, ini adalah {@code PersistentSet} milik Hibernate — memodifikasinya
	 * (mis. {@code add}/{@code remove}) langsung mengubah state yang akan di-{@code flush}.
	 * {@code ApresiasiDanPenghargaanAction} memang memakainya seperti itu: variabel
	 * {@code selectedPenghargaan} adalah <b>alias</b> ke koleksi ini, dan event {@code onClick}
	 * checkbox menambah/menghapus anggota secara langsung. Akibatnya tombol <b>"Batal" pada
	 * dialog tidak benar-benar membatalkan</b> perubahan centang — tombol itu hanya
	 * menyembunyikan window, sementara mutasinya sudah menempel pada entity terkelola. Perilaku
	 * ini sama persis dengan yang tercatat pada {@code PelanggaranDanHukuman}.</p>
	 *
	 * <p><b>Urutan.</b> {@code @OrderBy("nama asc")} hanya berlaku saat SQL pemuatan; tipe
	 * konkretnya {@link HashSet}, jadi urutan iterasi di memori tidak dijamin.</p>
	 *
	 * @return himpunan butir penghargaan milik paket; tidak pernah {@code null} (default
	 *         {@link HashSet} kosong), namun bisa berupa koleksi terkelola yang hidup
	 */
	@ManyToMany(targetEntity = Penghargaan.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "apresiasi_dan_penghargaan_has_penghargaan", schema = "sekolah", joinColumns = @JoinColumn(name = "apresiasi_dan_penghargaan"), inverseJoinColumns = @JoinColumn(name = "penghargaan"))
	public Set<Penghargaan> getPenghargaans() {
		return penghargaans;
	}

	/**
	 * Mengganti seluruh himpunan butir penghargaan paket ini.
	 *
	 * <p>Tanpa validasi dan tanpa penyalinan defensif — referensi yang dioper disimpan apa
	 * adanya. Pada {@code ApresiasiDanPenghargaanAction.onSave()} argumennya justru koleksi yang
	 * sama yang tadi diambil dari {@link #getPenghargaans()}, sehingga pemanggilan ini efektif
	 * menjadi no-op; perubahan sebenarnya sudah terjadi lewat mutasi langsung pada koleksi.</p>
	 *
	 * @param penghargaans himpunan butir penghargaan yang baru; menyetel {@code null} akan
	 *                     membuat getter mengembalikan {@code null} dan berpotensi memicu
	 *                     {@code NullPointerException} pada pembacanya
	 */
	public void setPenghargaans(Set<Penghargaan> penghargaans) {
		this.penghargaans = penghargaans;
	}

	/** Butir apresiasi yang tercakup paket ini; lihat {@link #getApresiasis()}. */
	private Set<Apresiasi> apresiasis = new HashSet<Apresiasi>();

	/**
	 * Mengembalikan himpunan butir apresiasi (kelas {@code Apresiasi}) yang tercakup paket ini
	 * (tabel jembatan {@code sekolah.apresiasi_dan_penghargaan_has_apresiasi}).
	 *
	 * <p><b>Peran fungsional.</b> Simetris dengan {@link #getPenghargaans()}: koleksi inilah
	 * yang menjadi daftar checkbox "Apresiasi" pada layar transaksi
	 * {@code ApresiasiSiswaAction.loadApresiasi()}.</p>
	 *
	 * <p><b>Koleksi terkelola, bukan salinan</b> — seluruh catatan tentang alias
	 * {@code selectedApresiasi} dan tombol "Batal" yang tidak membatalkan berlaku sama seperti
	 * dijelaskan pada {@link #getPenghargaans()}.</p>
	 *
	 * <p><b>Kuirk penciutan senyap (versi ringan).</b> Perender grid master membungkus koleksi
	 * ini dengan {@code new TreeSet<Apresiasi>(...)} sebelum menomori dan menampilkannya. Karena
	 * {@code Apresiasi} memakai {@link GeneralValueObject#compareTo(GeneralValueObject)} yang
	 * berjenjang, dan {@code nomorUrut}/{@code nim} tidak pernah dipetakan untuk entity ini,
	 * pembandingan jatuh ke {@code nama}. Dua butir apresiasi ber-{@code nama} sama persis
	 * karenanya dianggap setara dan <b>salah satunya hilang dari tampilan</b> — hanya di grid
	 * master, karena layar transaksi mengiterasi koleksi mentah tanpa {@code TreeSet}. Data di
	 * database tetap utuh; yang menyesatkan adalah tampilannya.</p>
	 *
	 * @return himpunan butir apresiasi milik paket; tidak pernah {@code null} secara default
	 */
	@ManyToMany(targetEntity = Apresiasi.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "apresiasi_dan_penghargaan_has_apresiasi", schema = "sekolah", joinColumns = @JoinColumn(name = "apresiasi_dan_penghargaan"), inverseJoinColumns = @JoinColumn(name = "apresiasi"))
	public Set<Apresiasi> getApresiasis() {
		return apresiasis;
	}

	/**
	 * Mengganti seluruh himpunan butir apresiasi paket ini.
	 *
	 * <p>Karakteristiknya sama dengan {@link #setPenghargaans(Set)}: tanpa validasi, tanpa
	 * salinan defensif, dan pada alur simpan layar master praktis menjadi no-op karena argumennya
	 * adalah koleksi yang sama.</p>
	 *
	 * @param apresiasis himpunan butir apresiasi yang baru
	 */
	public void setApresiasis(Set<Apresiasi> apresiasis) {
		this.apresiasis = apresiasis;
	}

	/**
	 * Konstruktor tanpa argumen.
	 *
	 * <p><b>Wajib ada</b> untuk Hibernate/JPA (instansiasi lewat refleksi) dan dipakai layar
	 * master saat menekan tombol Tambah ({@code onAdd()} membuat instance kosong lalu
	 * mengisinya lewat dialog). Kedua koleksi sudah terinisialisasi {@link HashSet} kosong,
	 * dan {@code tanggal_dirubah} sudah berisi waktu server.</p>
	 */
	public ApresiasiDanPenghargaan() {
	}

	/**
	 * Konstruktor ringkas bawaan generator hbm2java: mengisi kunci primer dan nama sekaligus.
	 *
	 * <p><b>Tidak dipakai kode aplikasi</b> (hasil penelusuran: nol pemanggil di luar berkas
	 * ini). Perhatikan bahwa konstruktor ini menyetel {@code id} secara manual, padahal kolom
	 * {@code id} berstrategi {@code IDENTITY} dan beranotasi {@code insertable = false} —
	 * memakainya untuk membuat baris baru bukan jalur yang benar. Untuk memuat baris yang sudah
	 * ada gunakan session Hibernate, bukan konstruktor ini.</p>
	 *
	 * @param id   kunci primer yang diisikan langsung ke field
	 * @param nama nama paket
	 */
	public ApresiasiDanPenghargaan(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci primer paket (kolom {@code id}).
	 *
	 * <p>Strategi {@code IDENTITY} dengan {@code insertable = false}: nilainya dibangkitkan
	 * database saat {@code INSERT} dan tidak pernah dikirim aplikasi. Konsekuensi praktisnya,
	 * id berurutan dan mudah ditebak — relevan bila suatu saat ada endpoint yang menerima id
	 * paket dari klien.</p>
	 *
	 * <p>Dipakai layar master untuk membedakan mode Tambah ({@code id == null}) dari mode Ubah,
	 * dan sebagai kunci {@code session.load(...)} pada {@code onSave()}.</p>
	 *
	 * @return kunci primer, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer. Tanpa validasi.
	 *
	 * <p>Hanya untuk dipakai Hibernate; kode aplikasi tidak boleh menyetel id secara manual.</p>
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik paket (kolom {@code sekolah_id}, {@code LAZY}).
	 *
	 * <p><b>Efek samping:</b> sebelum dikembalikan, nilainya dilewatkan
	 * {@link GeneralValueObject#check(Object)} yang <b>meresolusi proxy lazy</b> (cache
	 * in-memory &rarr; session aktif &rarr; session baru) dan hasilnya <b>ditulis balik</b> ke
	 * field. Jadi getter ini bukan pembaca murni. Bila keempat jalur resolusi gagal,
	 * {@code check(...)} mengembalikan argumen apa adanya — perilaku ini yang membuat pemanggil
	 * tidak pernah mendapat exception, tetapi juga tidak bisa membedakan "tidak ada sekolah"
	 * dari "gagal memuat sekolah".</p>
	 *
	 * <p>Nilai {@code null} berarti paket <b>tidak terikat sekolah mana pun</b>. Layar master
	 * mewajibkan sekolah diisi pada {@code onSave()}, tetapi jalur lain (impor Excel lewat
	 * {@code Common.uploadData}, data bawaan, atau penyuntingan langsung di database) tidak
	 * dijamin melakukannya.</p>
	 *
	 * @return sekolah pemilik paket, atau {@code null} bila belum/tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik paket, dengan <b>normalisasi object setengah jadi</b>.
	 *
	 * <p>Bila {@code sekolah} bernilai {@code null} <i>atau</i> ber-{@code id} {@code null},
	 * field disetel ke {@code null}. Aturan ini melindungi dari item combobox "-- Semua --"
	 * atau instance kosong yang belum tersimpan: alih-alih menyimpan referensi ke baris yang
	 * tidak ada, relasi dikosongkan.</p>
	 *
	 * <p><b>Perhatian:</b> karena itu, menyimpan paket dengan sekolah yang belum di-persist
	 * akan <b>diam-diam kehilangan</b> relasinya, bukan memunculkan error.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau ber-id {@code null} dinormalisasi
	 *                menjadi {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik paket (kolom {@code yayasan_id}, {@code LAZY}) —
	 * <b>getter destruktif</b>.
	 *
	 * <p><b>Efek samping yang harus disadari.</b> Method ini tidak sekadar membaca:</p>
	 * <ol>
	 *   <li>memanggil {@link #getSekolah()} (yang sendirinya menulis balik field
	 *       {@code sekolah}),</li>
	 *   <li>bila sekolah ada, <b>menimpa</b> field {@code yayasan} dengan
	 *       {@code sekolah.getYayasan()} — mengabaikan apa pun yang tersimpan di kolom
	 *       {@code yayasan_id},</li>
	 *   <li>melewatkan hasilnya ke {@link GeneralValueObject#check(Object)} dan menulis balik
	 *       sekali lagi.</li>
	 * </ol>
	 *
	 * <p>Karena Hibernate memakai <b>property access</b>, nilai hasil penimpaan inilah yang
	 * dibaca saat {@code flush}. Artinya: cukup dengan <i>membaca</i> paket di dalam sebuah
	 * transaksi, kolom {@code yayasan_id} bisa ikut ter-{@code UPDATE} agar konsisten dengan
	 * yayasan milik sekolahnya — sekaligus memicu {@link #onUpdate()} dan menambah revisi
	 * Envers. Sisi baiknya, {@code sekolah} dan {@code yayasan} praktis tidak bisa
	 * berkontradiksi; sisi buruknya, nilai {@code yayasan} yang disetel manual lewat
	 * {@link #setYayasan(Yayasan)} akan hilang begitu sekolahnya terisi.</p>
	 *
	 * <p>Bila {@link #getSekolah()} mengembalikan {@code null}, field {@code yayasan} yang ada
	 * dipertahankan (hanya diresolusi proxy-nya).</p>
	 *
	 * @return yayasan pemilik paket, atau {@code null} bila tidak terisi dan tidak bisa
	 *         diturunkan dari sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik paket, dengan normalisasi yang sama seperti
	 * {@link #setSekolah(Sekolah)}: {@code null} atau object ber-{@code id} {@code null}
	 * disimpan sebagai {@code null}.
	 *
	 * <p><b>Nilai yang disetel di sini tidak permanen</b> selama {@link #getSekolah()} tidak
	 * {@code null}: pemanggilan {@link #getYayasan()} berikutnya akan menimpanya dengan yayasan
	 * milik sekolah. Lihat penjelasan pada getter tersebut.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null}/ber-id {@code null} dinormalisasi menjadi
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan keterangan bebas paket (kolom {@code keterangan}).
	 *
	 * <p><b>Override ini MEMBALIK kontrak kelas induk.</b>
	 * {@link GeneralValueObject#getKeterangan()} sengaja menormalkan {@code null} menjadi
	 * {@code ""} sehingga pemanggil tidak perlu memeriksa {@code null}; versi di kelas ini
	 * mengembalikan field mentah, jadi <b>{@code null} sangat mungkin terjadi</b> (kolomnya
	 * memang boleh kosong dan layar master tidak mewajibkannya).</p>
	 *
	 * <p>Dua akibat konkret:</p>
	 * <ul>
	 *   <li>Kode umum yang menganggap kontrak induk berlaku — mis. memanggil
	 *       {@code .trim()}/{@code .isEmpty()} langsung — bisa melempar
	 *       {@code NullPointerException} untuk entity ini.</li>
	 *   <li>Cabang keempat {@link GeneralValueObject#compareTo(GeneralValueObject)} (pembanding
	 *       {@code keterangan}) yang di kelas lain <i>selalu</i> memenuhi syarat non-null, di
	 *       sini bisa terlewati; bila {@code nama} kebetulan juga tidak memenuhi syarat,
	 *       {@code compareTo} jatuh ke {@code 0} — "dianggap setara".</li>
	 * </ul>
	 *
	 * @return keterangan paket, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas paket. Tanpa validasi; {@code null} dan string kosong diterima
	 * apa adanya (tidak dinormalisasi).
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nama paket (kolom {@code nama}, {@code nullable = false}).
	 *
	 * <p>Ini <b>satu-satunya properti paket yang dilihat pengguna akhir di luar layar
	 * masternya</b>: dipakai sebagai label item combobox "Jenis apresiasi" pada layar transaksi,
	 * sebagai kategori "jenis" pada {@code DasbordApresiasi}, sebagai judul entri revisi
	 * ({@code RevisiHelper.createNewRevisi}), dan dicetak di rapor siswa lewat
	 * {@code LaporanRaporSiswa}. Mengubah nama paket karenanya langsung mengubah tampilan
	 * riwayat lama, karena {@code ApresiasiSiswa} menyimpan <b>referensi</b> ke paket, bukan
	 * salinan namanya.</p>
	 *
	 * <p>Tidak ada batasan unik pada kolom ini, sehingga dua paket bernama sama bisa hidup
	 * berdampingan dan tampil identik di combobox transaksi.</p>
	 *
	 * @return nama paket; secara skema tidak boleh {@code null}, walau object yang baru dibuat
	 *         di memori masih bisa mengembalikan {@code null} sebelum diisi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama paket. Tanpa validasi di lapis entity — pengecekan "harus diisi" dilakukan
	 * layar master ({@code onSave()}), bukan di sini.
	 *
	 * @param nama nama paket yang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan status aktif paket, dengan <b>default {@code true} bila kolomnya
	 * {@code null}</b> (baris lama/hasil impor yang tidak pernah mengisi kolom ini tetap
	 * dianggap aktif).
	 *
	 * <p>Berbeda dengan {@link #getYayasan()}, normalisasi ini <b>tidak</b> ditulis balik ke
	 * field: nilai {@code null} di database tetap {@code null}.</p>
	 *
	 * <p><b>Kuirk cakupan: bendera ini hanya sebagian dihormati.</b> Di layar master, checkbox
	 * "Aktif" pada grid membaca/menulis nilai ini dan langsung menyimpannya. Namun pada layar
	 * transaksi {@code ApresiasiSiswaAction}, combobox pemilih paket diisi dengan
	 * {@code Common.insertCombo(..., ApresiasiDanPenghargaan.class)} <b>tanpa satu pun
	 * kriteria</b> — sehingga paket yang sudah dinonaktifkan <b>tetap muncul dan tetap bisa
	 * dipilih</b> untuk transaksi baru. Menonaktifkan paket karenanya bukan cara yang efektif
	 * untuk menghentikan pemakaiannya. (Bandingkan dengan daftar checkbox butir Apresiasi dan
	 * {@code Penghargaan} di layar master, yang justru <i>menyaring</i> {@code aktif}.)</p>
	 *
	 * @return {@code true} bila paket aktif atau kolomnya {@code null}; {@code false} hanya bila
	 *         kolomnya benar-benar berisi {@code false}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif paket. Tanpa validasi; {@code null} diterima dan akan terbaca
	 * sebagai {@code true} lewat {@link #getAktif()}.
	 *
	 * <p>Pemanggil utamanya adalah listener {@code onCheck} checkbox "Aktif" di grid layar
	 * master, yang langsung menyusulinya dengan penyimpanan.</p>
	 *
	 * @param aktif status aktif yang baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
