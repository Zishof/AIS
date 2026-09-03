package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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
 * Katalog MATA PELAJARAN sekolah — entity master paling inti pada modul
 * {@code ais.database.model.sekolah}. Satu baris mewakili satu mata pelajaran milik satu
 * {@link Sekolah} (mis. "Matematika", "Bahasa Indonesia", "Fikih"), lengkap dengan kode,
 * nama multibahasa, kelompok kurikuler, skema penilaian yang dipakai, dan KKM (Kriteria
 * Ketuntasan Minimal) default.
 *
 * <h2>Kedudukan dalam modul sekolah</h2>
 * <p>Entity ini adalah <b>simpul rujukan</b> hampir seluruh alur akademik sekolah. Sekitar
 * <b>73 berkas Java</b> di pohon sumber ini menyebut tipe {@code Matapelajaran}. Delapan entity
 * lain memegang kolom FK ke tabel ini:</p>
 * <ul>
 *   <li>{@link SubMatapelajaran} — pemecahan satu mapel menjadi beberapa sub-mapel
 *   (mis. "Fisika" &rarr; "Fisika Teori"/"Fisika Praktikum").</li>
 *   <li>{@link KurikulumPunyaMatapelajaran} — pemasangan mapel ke satu
 *   {@link KurikulumSekolah} beserta jumlah jam pelajarannya.</li>
 *   <li>{@link MatapelajaranPunyaBukuBahanAjar} — daftar buku bahan ajar per mapel.</li>
 *   <li>{@link JadwalPelajaran} — jadwal/kelas pembelajaran yang mengajarkan mapel ini.</li>
 *   <li>{@link GuruMengajar} — penugasan guru pada mapel.</li>
 *   <li>{@link KelasLesSiswa} — kelas les/bimbingan tambahan per mapel.</li>
 *   <li>{@code ais.database.model.BankSoal} dan {@code ais.database.model.Ujian} — bank soal
 *   dan ujian yang terikat pada mapel.</li>
 * </ul>
 * <p>Selain FK di atas, id baris ini juga dirujuk <b>tanpa relasi (soft reference)</b> dari kolom
 * teks {@code mpYgTidakDiambil} pada {@link KelasSiswa}/{@link KelasSiswaPunyaSiswa}: kolom itu
 * menyimpan JSON berisi array id mata pelajaran yang TIDAK diambil siswa, dan diterjemahkan
 * kembali lewat cache {@code ConstantValues.ambil(Matapelajaran.class.getName(), id)}. Karena
 * bukan FK, penghapusan baris di sini <b>tidak</b> tertahan constraint database dan daftar
 * pengecualian tersebut berubah makna secara diam-diam.</p>
 *
 * <h2>PERINGATAN: dua katalog mata pelajaran yang berbeda</h2>
 * <p>Di codebase ini terdapat <b>dua</b> katalog mata pelajaran yang namanya nyaris sama dan
 * <b>tidak saling terhubung sama sekali</b> (tanpa FK, tanpa sinkronisasi):</p>
 * <ul>
 *   <li><b>Entity ini</b> — tabel {@code sekolah.matapelajaran}. Dipakai seluruh alur
 *   <i>operasional akademik</i>: kurikulum, jadwal, guru mengajar, penilaian/rapor, bank soal,
 *   ujian, bahan ajar.</li>
 *   <li>{@code ais.database.model.MatapelajaranSekolah} — tabel {@code public.matapelajaran_sekolah}.
 *   Dipakai alur <i>penerimaan siswa baru</i>: {@link GelombangPendaftaranPsbPunyaMatapelajaran}
 *   dan {@link CalonSiswaPunyaVerifikasiMatapelajaran} (verifikasi nilai rapor calon siswa)
 *   menunjuk ke sana, BUKAN ke entity ini.</li>
 * </ul>
 * <p>Konsekuensi praktis: daftar mapel untuk verifikasi rapor PSB harus dipelihara TERPISAH dari
 * daftar mapel operasional. Jangan berasumsi id/kode antara kedua tabel itu sepadan.</p>
 * <p>Catatan penamaan serupa: {@link KompetensiDasarMatapelajaran} — meski namanya mengandung
 * "Matapelajaran" — <b>tidak</b> memiliki relasi apa pun ke entity ini; berkas tersebut adalah
 * hasil salin-tempel {@link JenisJadwalPelajaran} dan bahkan dipetakan ke tabel
 * {@code sekolah.jenis_jadwal_pelajaran}.</p>
 *
 * <h2>Layar pengelola dan hak akses</h2>
 * <p>CRUD-nya adalah {@code /pages/master/sekolah/matapelajaran.zul} yang dikendalikan
 * {@code ais.action.master.sekolah.MatapelajaranAction}. Menu didaftarkan
 * {@code MenuInitializer}/{@code MenuSnapshotData} dengan id <b>83459</b> ("Mata Pelajaran",
 * induk {@code 570008}).</p>
 * <p><b>Pewarisan hak lewat menu induk (pola arsitektur berulang — TERVERIFIKASI di sini).</b>
 * Layar ini adalah {@code tabbox} dua tab; tab kedua ("Sub Matapelajaran") menyisipkan
 * {@code /pages/master/sekolah/sub_matapelajaran.zul} lewat {@code MyInclude}. Berkas
 * {@code sub_matapelajaran.zul} <b>tidak pernah didaftarkan sebagai menu tersendiri</b>, sedangkan
 * {@code CommonPrivilages.checkPrevilages(...)} menentukan hak dari {@code Common.getCurrentMenu()}
 * — yaitu menu halaman yang sedang dibuka, "Mata Pelajaran". Akibatnya hak CREATE/UPDATE/DELETE
 * pada menu "Mata Pelajaran" <b>otomatis</b> menjadi hak CRUD penuh atas {@link SubMatapelajaran},
 * tanpa pernah bisa dipisahkan oleh administrator.</p>
 * <p><b>Cakupan tenant bersifat fail-open.</b> {@code MatapelajaranAction.initCriteria()} hanya
 * menambahkan filter {@code sekolah}/{@code yayasan} bila combo pencarian benar-benar terpilih;
 * bila tidak, yang ditambahkan adalah {@code Restrictions.sqlRestriction("1=1")}. Kombinasinya
 * dengan {@code InitComboUtil.initYayasanDanSekolahDanSemua(...)} — yang hanya mengunci combo
 * ketika konteks sekolah/yayasan aktif atau pengguna terikat sekolah — berarti pengguna
 * <b>tanpa</b> keterikatan sekolah/yayasan melihat SELURUH mata pelajaran seluruh instalasi.
 * Pola picker {@code AmbilDataMatapelajaranBanbox} sama persis.</p>
 * <p><b>Tombol ekspor tidak bergerbang.</b> Pada {@code doAfterCompose()}, tombol
 * {@code Common.cetakData(...)} (unduh Excel seluruh hasil pencarian) dipasang tanpa pemeriksaan
 * hak sama sekali — hak BACA cukup untuk mengunduh katalog mapel. Sebaliknya tombol unggah massal
 * <i>bergerbang benar</i> ({@code upload.setVisible(add.isVisible() && edit && delete)}) dan layak
 * dijadikan contoh POSITIF.</p>
 *
 * <h2>Kolom yang TIDAK dipetakan: {@code keterangan}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @MappedSuperclass}, sehingga properti
 * {@code keterangan} milik kelas induk <b>tidak menjadi kolom</b> pada tabel ini. Layar tetap
 * menampilkan dan menerima isian "Keterangan", {@code onSave()} tetap memanggil
 * {@code setKeterangan(...)} — namun nilainya hanya hidup di memori dan hilang begitu request
 * selesai. Bukti eksplisit ada di {@code MatapelajaranAction.doAfterCompose()}: daftar kolom
 * ekspor/impor sengaja TIDAK memuat {@code "keterangan"}, dengan komentar kode bahwa
 * memasukkannya membuat Hibernate mencari properti tak terpetakan dan melempar
 * {@code QueryException}. Ini instance lain dari pola yang sudah tercatat pada
 * {@code StatusAwalSiswa}/{@code JenisPenilaian}/{@code GrupPenilaian}.</p>
 *
 * <h2>Pengurutan: {@code urutan} vs {@code nomorUrut}</h2>
 * <p>Entity ini memiliki kolom urutan tampilnya sendiri, {@link #getUrutan()} (kolom
 * {@code urutan}), dan layar daftar mengurutkan lewat SQL
 * ({@code Order.asc("urutan")} lalu {@code Order.asc("nama")}). Namun
 * {@link GeneralValueObject#compareTo(GeneralValueObject)} memakai properti {@code nomorUrut}
 * milik kelas induk — yang <b>tidak dipetakan</b> di sini dan selalu {@code null}. Jadi setiap
 * pengurutan in-memory ({@code Collections.sort}, {@code TreeSet}, {@code TreeMap}) atas
 * {@code Matapelajaran} jatuh ke cabang {@code nama} dan <b>mengabaikan kolom urutan</b>. Bila
 * urutan tampil harus dihormati, urutkan di level query seperti yang dilakukan
 * {@code MatapelajaranAction}, bukan mengandalkan {@code compareTo}.</p>
 *
 * <h2>Getter yang menulis balik (destruktif) — pola arsitektur berulang</h2>
 * <p>Hibernate memetakan kelas ini dengan <b>property access</b> (anotasi berada pada getter).
 * Artinya setiap normalisasi yang dilakukan getter <b>ikut ditulis ke database</b> saat flush,
 * bukan sekadar mempercantik tampilan:</p>
 * <ul>
 *   <li>{@link #getYayasan()} <b>menimpa</b> field {@code yayasan} dengan
 *   {@code sekolah.getYayasan()} setiap kali dipanggil — nilai yayasan yang disetel pemanggil
 *   akan kalah oleh yayasan milik sekolah.</li>
 *   <li>{@link #getUrutan()} mengubah {@code null} menjadi {@code 0}, {@link #getKkm()} menjadi
 *   {@code 70.0}, {@link #getAktif()}/{@link #getTerdapatNilaiKeterampilan()}/
 *   {@link #getTerdapatNilaiPredikat()} menjadi {@code true}.</li>
 *   <li>{@link #getNamaEn()}/{@link #getNamaAr()}/{@link #getNamaCh()} mengembalikan
 *   {@link #getNama()} bila kosong — dan karena {@code MatapelajaranAction.init(...)} mengisi
 *   textbox dari getter tersebut lalu {@code onSave()} menuliskannya kembali, sekali data disimpan
 *   lewat form, ketiga kolom nama asing <b>terisi permanen</b> dengan nama Indonesia. Fallback
 *   dinamisnya hilang: mengganti nama Indonesia tidak lagi ikut mengganti nama Inggris/Arab/
 *   Tionghoa.</li>
 *   <li>{@link #getCapaianPembelajaranProdi()}/{@link #getDeskripsiPembelajaran()} melakukan
 *   {@code trim()} dan mengubah {@code null} menjadi {@code ""}.</li>
 * </ul>
 * <p>Berbeda dengan instance destruktif yang berbahaya pada entity lain ({@code ItemBiayaSekolah},
 * {@code NominalBiaya}), penulisan balik di sini bersifat <b>pengisian nilai default</b> dan tidak
 * menghapus data yang sudah ada — kecuali efek "materialisasi nama asing" pada butir ketiga.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}/{@link #setId(Long)}, dua constructor.</li>
 *   <li><b>Relasi</b>: {@link #getSekolah()}, {@link #getYayasan()}, {@link #getJenisPenilaian()},
 *   {@link #getKelompokMatapelajaran()} beserta setter-nya.</li>
 *   <li><b>Identifikasi &amp; label</b>: {@link #getKode()}, {@link #getNama()},
 *   {@link #getSingkatan()}, {@link #getNamaEn()}, {@link #getNamaAr()}, {@link #getNamaCh()}.</li>
 *   <li><b>Perilaku akademik</b>: {@link #getKkm()}, {@link #getUrutan()}, {@link #getAktif()},
 *   {@link #getTerdapatNilaiKeterampilan()}, {@link #getTerdapatNilaiPredikat()}.</li>
 *   <li><b>Teks kurikuler</b>: {@link #getCapaianPembelajaranProdi()},
 *   {@link #getDeskripsiPembelajaran()}.</li>
 * </ol>
 * <p>Kelas ini <b>tidak</b> memiliki method bisnis: seluruh logika (validasi, penjadwalan,
 * perhitungan nilai) berada di Action/Helper pemanggil.</p>
 *
 * @see GeneralValueObject
 * @see SubMatapelajaran
 * @see KelompokMatapelajaran
 * @see KurikulumPunyaMatapelajaran
 * @see MatapelajaranPunyaBukuBahanAjar
 * @see JenisPenilaian
 * @see ais.action.master.sekolah.MatapelajaranAction
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "matapelajaran", schema = "sekolah")
public class Matapelajaran extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap; jangan diubah agar sesi ZK dan cache yang menyimpan
	 * instance lama tetap dapat dideserialisasi.
	 */
	private static final long serialVersionUID = -8569861041232209706L;

	/** Kunci utama baris, dibangkitkan basis data (IDENTITY); {@code null} untuk objek baru. */
	private Long id;

	/** Nama pengguna terakhir yang menyimpan baris ini; diisi otomatis interceptor audit. */
	private String oleh;

	/** Id pengguna terakhir yang menyimpan baris ini; diisi otomatis interceptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah, dengan penjaga anti-timpa.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} atau string kosong/whitespace <b>diabaikan
	 * diam-diam</b> dan nilai lama dipertahankan. Perilaku ini disengaja agar jejak audit yang
	 * sudah benar tidak terhapus oleh alur yang kebetulan menyetel ulang properti dengan nilai
	 * kosong (mis. penyalinan objek, impor Excel, atau pengikatan formulir).</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan tanpa pesan kesalahan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah, dengan penjaga anti-timpa yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan dan nilai lama
	 * dipertahankan.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan tanpa pesan kesalahan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyegarkan metadata audit ({@code oleh}, {@code olehId},
	 * {@code tanggal_dirubah}) sesaat sebelum Hibernate mengeksekusi UPDATE.
	 *
	 * <p>Merupakan implementasi satu-satunya method {@code abstract} milik
	 * {@link GeneralValueObject}. Dipanggil <b>oleh Hibernate</b>, bukan oleh kode aplikasi.
	 * Perhatikan bahwa callback ini hanya berjalan pada UPDATE — pada INSERT, pengisian audit
	 * ditangani {@code AuditTimestampInterceptor} di level interceptor session.</p>
	 *
	 * @see ais.database.hibernate.AuditTimestampInterceptor
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir baris. Diinisialisasi ke waktu server saat objek dibuat
	 * ({@code WaktuUtil.getDate()}) sehingga baris baru selalu punya timestamp walau alur
	 * pemanggil lupa mengisinya.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi; biasanya dipanggil
	 * {@code AuditTimestampInterceptor}, bukan oleh Action.
	 *
	 * @param tanggal_dirubah waktu perubahan baru; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris (presisi TIMESTAMP).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         constructor kelas ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Skema penilaian yang berlaku bagi mapel ini (kolom {@code jenis_penilaian_id}). Menjadi
	 * puncak rantai penilaian {@code JenisPenilaian} &rarr; {@code DetailJenisPenilaian} &rarr;
	 * {@code GrupPenilaian} &rarr; ... yang membentuk struktur kolom rapor.
	 */
	private JenisPenilaian jenisPenilaian;

	/** Sekolah pemilik baris (kolom {@code sekolah_id}, {@code NOT NULL}); kunci tenant utama. */
	private Sekolah sekolah;

	/**
	 * Kode ringkas mata pelajaran (mis. {@code "MTK"}). Wajib diisi oleh layar CRUD, namun
	 * <b>tidak</b> dijamin unik oleh constraint database maupun oleh validasi
	 * {@code MatapelajaranAction.onSave()} (yang hanya menolak kode kosong).
	 *
	 * <p>Field ini menutupi (<i>shadow</i>) properti {@code kode} milik {@link GeneralValueObject};
	 * karena {@link #getKode()} meng-override getter induk, {@code toString()} induk tetap
	 * menghasilkan {@code "kode - nama"} yang benar.</p>
	 */
	private String kode;

	/**
	 * Nama mata pelajaran dalam bahasa Indonesia (kolom {@code nama}, {@code NOT NULL}). Menjadi
	 * kunci pengurutan in-memory (lihat catatan {@code compareTo} pada Javadoc kelas) dan sumber
	 * fallback bagi ketiga nama asing.
	 */
	private String nama;

	/** Nama mata pelajaran dalam aksara Arab; kosong berarti ikut {@link #getNama()}. */
	private String namaAr;

	/** Nama mata pelajaran dalam bahasa Inggris; kosong berarti ikut {@link #getNama()}. */
	private String namaEn;

	/** Nama mata pelajaran dalam aksara Tionghoa; kosong berarti ikut {@link #getNama()}. */
	private String namaCh;

	/**
	 * Singkatan nama untuk tampilan sempit (kolom {@code singkatan}, {@code NOT NULL} di level
	 * pemetaan). Perhatikan {@code MatapelajaranAction.onSave()} <b>tidak</b> memvalidasi kolom ini
	 * sehingga menyimpan singkatan kosong akan ditolak database, bukan oleh pesan ramah.
	 */
	private String singkatan;

	/**
	 * Nomor urut tampil pada daftar dan rapor (kolom {@code urutan}). Dipakai sebagai kunci
	 * pengurutan SQL pertama oleh {@code MatapelajaranAction.initCriteria(true)}.
	 */
	private Integer urutan;

	/**
	 * Yayasan pemilik (kolom {@code yayasan_id}). <b>Turunan</b>, bukan masukan bebas: nilainya
	 * selalu diselaraskan ke {@code sekolah.getYayasan()} oleh {@link #getYayasan()}.
	 */
	private Yayasan yayasan;

	/**
	 * Penanda mapel masih dipakai. {@code null} dibaca sebagai {@code true} oleh
	 * {@link #getAktif()}.
	 *
	 * <p><b>Kuirk layar:</b> berkas {@code matapelajaran.zul} mendeklarasikan checkbox
	 * {@code searchaktif} berlabel "Tampilkan hanya yang aktif" dalam keadaan tercentang, tetapi
	 * {@code MatapelajaranAction} tidak memiliki field bernama {@code searchaktif} dan
	 * {@code initCriteria()} tidak pernah memfilter kolom {@code aktif}. Kendali itu
	 * <b>mati total</b>: daftar selalu memuat mapel nonaktif sekalipun. Bandingkan dengan catatan
	 * serupa pada {@link KelompokMatapelajaran}.</p>
	 */
	private Boolean aktif;

	/**
	 * Penanda mapel memiliki komponen nilai keterampilan.
	 *
	 * <p><b>Tidak terpakai:</b> di seluruh pohon sumber, properti ini hanya disentuh oleh getter
	 * dan setter di berkas ini sendiri — tidak ada Action, Helper, laporan, maupun API yang
	 * membacanya. Karena {@link #getTerdapatNilaiKeterampilan()} mengembalikan {@code true}
	 * untuk {@code null} dan pemetaan memakai property access, kolomnya praktis selalu bernilai
	 * {@code true}.</p>
	 */
	private Boolean terdapatNilaiKeterampilan;

	/**
	 * Penanda mapel memiliki komponen nilai predikat (A/B/C).
	 *
	 * <p><b>Tidak terpakai</b> dengan alasan yang sama seperti
	 * {@link #terdapatNilaiKeterampilan}.</p>
	 */
	private Boolean terdapatNilaiPredikat;

	/**
	 * Capaian pembelajaran / kompetensi mapel (kolom {@code text}).
	 *
	 * <p><b>Non-obvious:</b> layar CRUD {@code matapelajaran.zul} <b>tidak</b> menyediakan isian
	 * untuk kolom ini. Satu-satunya penulis adalah
	 * {@code PenjadwalanSiswaHelper}: ketika guru mengubah "Capaian / Kompetensi" pada sebuah
	 * {@link JadwalPelajaran} <b>dan</b> kolom mapel-nya masih kosong, teks itu di-<i>backfill</i>
	 * ke baris master ini dan langsung disimpan. Jadi suntingan pada satu jadwal dapat mengubah
	 * data master milik seluruh sekolah, tanpa layar mana pun untuk meninjau atau
	 * mengoreksinya.</p>
	 */
	private String capaianPembelajaranProdi;

	/**
	 * Deskripsi pembelajaran mapel (kolom {@code text}). Diisi lewat mekanisme <i>backfill</i> dari
	 * {@link JadwalPelajaran} yang sama seperti {@link #capaianPembelajaranProdi}.
	 *
	 * <p><b>Kode mati terkait:</b> {@code JadwalPelajaranAction} (sekitar baris 4842) dan
	 * {@code PertemuanJadwalPelajaranAction} (sekitar baris 466) memuat blok
	 * {@code if (!jadwal.getDeskripsiPembelajaran().isEmpty() &&
	 * jadwal.getMatapelajaran().getDeskripsiPembelajaran().isEmpty()) { }} dengan <b>badan
	 * kosong</b> — niat semula agar cetakan RPP/silabus jatuh balik ke deskripsi master tidak
	 * pernah diselesaikan, sehingga cetakan selalu memakai deskripsi milik jadwal saja.</p>
	 */
	private String deskripsiPembelajaran;

	/**
	 * Kelompok kurikuler mapel (kolom {@code kelompok_matapelajaran_id}, boleh {@code null}) —
	 * mis. "Kelompok A (Wajib)"/"Muatan Lokal". Menentukan pengelompokan blok kolom pada rapor.
	 */
	private KelompokMatapelajaran kelompokMatapelajaran;

	/**
	 * KKM (Kriteria Ketuntasan Minimal) default mapel. {@code null} dibaca sebagai {@code 70.0}
	 * oleh {@link #getKkm()}.
	 */
	private Double kkm;

	/**
	 * Constructor default tanpa argumen. WAJIB ada karena Hibernate membutuhkannya untuk membuat
	 * instance saat hidrasi entity dari hasil query, dan dipakai
	 * {@code MatapelajaranAction.onAdd()} untuk membuka form tambah.
	 */
	public Matapelajaran() {
	}

	/**
	 * Constructor ringkas untuk membuat objek dengan kolom wajib terisi.
	 *
	 * <p><b>Non-obvious:</b> parameter {@code sekolah} dilewatkan melalui penjaga yang sama dengan
	 * {@link #setSekolah(Sekolah)} — objek {@link Sekolah} yang belum tersimpan (id masih
	 * {@code null}) diperlakukan sebagai {@code null} agar tidak memicu penyimpanan berantai lewat
	 * {@code CascadeType.PERSIST}. Parameter {@code id} bertipe primitif {@code long} sehingga
	 * constructor ini tidak dapat dipakai untuk membuat baris baru tanpa id.</p>
	 *
	 * @param id        kunci utama baris yang sudah ada
	 * @param sekolah   sekolah pemilik; objek tanpa id diperlakukan sebagai {@code null}
	 * @param nama      nama mata pelajaran (bahasa Indonesia)
	 * @param singkatan singkatan nama
	 */
	public Matapelajaran(long id, Sekolah sekolah, String nama, String singkatan) {
		this.id = id;
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
		this.nama = nama;
		this.singkatan = singkatan;
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan basis data
	 * (strategi IDENTITY). Nilai {@code null} berarti objek belum pernah disimpan — dipakai
	 * {@code MatapelajaranAction.init(...)} untuk membedakan judul "Tambah" dan "Ubah", serta
	 * {@code onSave()} untuk memilih antara INSERT dan {@code session.load(...)} + UPDATE.</p>
	 *
	 * @return id baris, atau {@code null} untuk objek baru
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris. Tanpa validasi; umumnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan skema penilaian yang dipakai mapel ini, dengan resolusi proxy lazy.
	 *
	 * <p><b>Efek samping:</b> hasil {@link GeneralValueObject#check(Object)} ditulis kembali ke
	 * field sehingga proxy Hibernate diganti objek nyata. Ini menghindari
	 * {@code LazyInitializationException} bila objek dipakai setelah session ditutup, tetapi juga
	 * berarti getter ini dapat memicu query database.</p>
	 * <p>Nilai ini dibaca antara lain oleh {@code MatapelajaranAction.MatapelajaranRenderer}
	 * (kolom "Jenis Penilaian") dan oleh rantai pembentukan kolom rapor.</p>
	 *
	 * @return jenis penilaian mapel, atau {@code null} bila belum ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penilaian_id")
	public JenisPenilaian getJenisPenilaian() {
		jenisPenilaian = check(jenisPenilaian);
		return this.jenisPenilaian;
	}

	/**
	 * Menyetel skema penilaian mapel. Tanpa validasi.
	 *
	 * <p>Layar CRUD hanya menawarkan {@link JenisPenilaian} yang {@code sekolah}-nya {@code null}
	 * (global) atau sama dengan sekolah terpilih, namun batasan itu murni di lapisan UI —
	 * pemanggil lain dapat memasang jenis penilaian milik sekolah mana pun.</p>
	 *
	 * @param jenisPenilaian jenis penilaian baru; {@code null} diterima
	 */
	public void setJenisPenilaian(JenisPenilaian jenisPenilaian) {
		this.jenisPenilaian = jenisPenilaian;
	}

	/**
	 * Mengembalikan sekolah pemilik mapel, dengan resolusi proxy lazy (menulis balik ke field
	 * seperti {@link #getJenisPenilaian()}).
	 *
	 * <p>Kolom {@code sekolah_id} dipetakan {@code nullable = false}: baris tanpa sekolah akan
	 * ditolak basis data. Nilai ini adalah <b>satu-satunya kunci tenant nyata</b> entity ini —
	 * lihat catatan fail-open pada Javadoc kelas.</p>
	 *
	 * @return sekolah pemilik; secara praktis tidak pernah {@code null} untuk baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id", nullable = false)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik.
	 *
	 * <p><b>Non-obvious:</b> objek {@link Sekolah} yang belum tersimpan (id {@code null})
	 * dikonversi menjadi {@code null} agar {@code CascadeType.PERSIST} tidak ikut menyimpan
	 * sekolah setengah jadi. Akibat sampingannya, memasang sekolah transient <b>tidak</b> memicu
	 * kesalahan di sini melainkan pelanggaran {@code NOT NULL} saat flush.</p>
	 *
	 * @param sekolah sekolah pemilik; objek tanpa id diperlakukan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik — <b>selalu diturunkan ulang dari sekolah</b>.
	 *
	 * <p><b>Efek samping (getter destruktif):</b> bila {@link #getSekolah()} tidak {@code null},
	 * field {@code yayasan} <b>ditimpa</b> dengan {@code sekolah.getYayasan()} sebelum
	 * dikembalikan. Karena pemetaan memakai property access, nilai turunan itulah yang tertulis ke
	 * kolom {@code yayasan_id} saat flush. Konsekuensinya nilai apa pun yang disetel lewat
	 * {@link #setYayasan(Yayasan)} — termasuk pilihan pengguna pada form — akan kalah oleh yayasan
	 * milik sekolah. Perilaku ini menjaga konsistensi tenant, tetapi membuat kolom
	 * {@code yayasan_id} <b>tidak dapat</b> dipakai untuk menyimpan yayasan yang berbeda dari
	 * sekolahnya.</p>
	 * <p>Field tetap ditulis melalui {@code check(...)} sehingga proxy lazy juga diresolusi.</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila sekolah maupun field belum terisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik, dengan penjaga objek transient yang sama seperti
	 * {@link #setSekolah(Sekolah)}.
	 *
	 * <p><b>Perhatian:</b> nilai yang disetel di sini bersifat sementara — {@link #getYayasan()}
	 * akan menimpanya dengan yayasan milik sekolah pada pembacaan berikutnya.</p>
	 *
	 * @param yayasan yayasan baru; objek tanpa id diperlakukan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan kode ringkas mata pelajaran apa adanya (tanpa normalisasi).
	 *
	 * <p>Meng-override {@link GeneralValueObject#getKode()} sehingga {@code toString()} induk
	 * menghasilkan {@code "<kode> - <nama>"}. Kode ini dipakai luas pada label jadwal, absensi,
	 * kalender, dan parameter laporan ({@code kode_matapelajaran}).</p>
	 *
	 * @return kode mapel, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode ringkas mata pelajaran. Tanpa validasi maupun normalisasi; keunikan menjadi
	 * tanggung jawab pemanggil.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama mata pelajaran dalam bahasa Indonesia.
	 *
	 * <p>Meng-override {@link GeneralValueObject#getNama()} dan memetakannya ke kolom
	 * {@code nama} yang {@code NOT NULL}. Menjadi kunci pengurutan in-memory serta sumber fallback
	 * {@link #getNamaEn()}/{@link #getNamaAr()}/{@link #getNamaCh()}.</p>
	 *
	 * @return nama mapel; {@code null} hanya mungkin pada objek yang belum tersimpan
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama mata pelajaran. Tanpa validasi — penolakan nama kosong dilakukan
	 * {@code MatapelajaranAction.onSave()}, bukan di sini.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan singkatan nama mata pelajaran.
	 *
	 * @return singkatan mapel, atau {@code null} bila belum diisi
	 */
	@Column(name = "singkatan", nullable = false)
	public String getSingkatan() {
		return this.singkatan;
	}

	/**
	 * Menyetel singkatan nama mata pelajaran. Tanpa validasi, padahal kolomnya dipetakan
	 * {@code NOT NULL} dan layar CRUD tidak memeriksanya.
	 *
	 * @param singkatan singkatan baru
	 */
	public void setSingkatan(String singkatan) {
		this.singkatan = singkatan;
	}

	/**
	 * Mengembalikan nomor urut tampil, dengan normalisasi {@code null} &rarr; {@code 0}.
	 *
	 * <p><b>Efek samping:</b> karena pemetaan memakai property access, nilai {@code 0} hasil
	 * normalisasi itulah yang ditulis ke kolom {@code urutan} saat flush. Baris yang masuk lewat
	 * SQL mentah/migrasi tetap dapat bernilai {@code NULL} di basis data dan — pada PostgreSQL —
	 * akan berada di akhir hasil {@code Order.asc("urutan")}.</p>
	 * <p><b>Perhatian:</b> nilai ini <b>tidak</b> dipakai
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)}, yang membaca properti
	 * {@code nomorUrut} milik induk (tak dipetakan di sini, selalu {@code null}). Pengurutan
	 * in-memory karenanya jatuh ke nama.</p>
	 *
	 * @return nomor urut tampil; tidak pernah {@code null}
	 */
	@Column(name = "urutan")
	public Integer getUrutan() {
		return this.urutan == null ? 0 : urutan;
	}

	/**
	 * Menyetel nomor urut tampil. Tanpa validasi; nilai {@code null} akan terbaca sebagai
	 * {@code 0}.
	 *
	 * @param urutan nomor urut baru
	 */
	public void setUrutan(Integer urutan) {
		this.urutan = urutan;
	}

	/**
	 * Mengembalikan penanda aktif, dengan normalisasi {@code null} &rarr; {@code true} (mapel
	 * lama tanpa nilai dianggap masih dipakai).
	 *
	 * <p>Dipakai {@code MatapelajaranAction.MatapelajaranRenderer} untuk menyalakan checkbox
	 * "Aktif" per baris, serta oleh picker {@code AmbilDataMatapelajaranBanbox} yang menyaring
	 * {@code isNull("aktif") OR eq("aktif", true)}. Layar daftar utama sendiri
	 * <b>tidak</b> menyaring kolom ini — lihat catatan pada field {@link #aktif}.</p>
	 *
	 * @return {@code true} bila mapel masih dipakai; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif. Tanpa validasi.
	 *
	 * <p>Dipanggil langsung dari listener {@code onCheck} checkbox pada baris daftar, yang
	 * kemudian menyimpan perubahan lewat {@code Common.refreshSaveOrUpdate(...)} tanpa tombol
	 * Simpan. Checkbox tersebut <b>bergerbang benar</b> ({@code setDisabled(!edit)}).</p>
	 *
	 * @param aktif penanda aktif baru; {@code null} akan terbaca sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan teks capaian pembelajaran/kompetensi, dengan normalisasi: {@code null} menjadi
	 * {@code ""} dan spasi tepi dipangkas.
	 *
	 * <p><b>Efek samping:</b> nilai hasil {@code trim()} inilah yang ditulis ke kolom saat flush
	 * (property access). Pemanggil ({@code PenjadwalanSiswaHelper}, {@code JadwalPelajaranAction})
	 * memakainya lewat {@code isEmpty()} untuk memutuskan perlu tidaknya <i>backfill</i> dari
	 * jadwal pelajaran.</p>
	 *
	 * @return capaian pembelajaran; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getCapaianPembelajaranProdi() {
		return capaianPembelajaranProdi == null ? "" : capaianPembelajaranProdi.trim();
	}

	/**
	 * Menyetel teks capaian pembelajaran/kompetensi. Tanpa validasi.
	 *
	 * <p>Satu-satunya pemanggil produksi adalah {@code PenjadwalanSiswaHelper}, yang menuliskan
	 * teks milik sebuah {@link JadwalPelajaran} ke baris master ini. Tidak ada layar untuk
	 * mengoreksinya kembali.</p>
	 *
	 * @param capaianPembelajaranProdi teks capaian pembelajaran baru
	 */
	public void setCapaianPembelajaranProdi(String capaianPembelajaranProdi) {
		this.capaianPembelajaranProdi = capaianPembelajaranProdi;
	}

	/**
	 * Mengembalikan deskripsi pembelajaran, dengan normalisasi yang sama seperti
	 * {@link #getCapaianPembelajaranProdi()} ({@code null} &rarr; {@code ""}, dipangkas).
	 *
	 * @return deskripsi pembelajaran; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getDeskripsiPembelajaran() {
		return deskripsiPembelajaran == null ? "" : deskripsiPembelajaran.trim();
	}

	/**
	 * Menyetel deskripsi pembelajaran. Tanpa validasi; diisi lewat <i>backfill</i> dari
	 * {@link JadwalPelajaran}, bukan lewat layar CRUD mapel.
	 *
	 * @param deskripsiPembelajaran deskripsi pembelajaran baru
	 */
	public void setDeskripsiPembelajaran(String deskripsiPembelajaran) {
		this.deskripsiPembelajaran = deskripsiPembelajaran;
	}

	/**
	 * Mengembalikan kelompok kurikuler mapel, dengan resolusi proxy lazy (menulis balik ke field).
	 *
	 * <p>Boleh {@code null} di level pemetaan meski layar CRUD mewajibkannya
	 * ({@code onSave()} menolak simpan bila combo "Kelompok Matapelajaran" belum dipilih) — baris
	 * hasil impor Excel atau SQL mentah dapat tetap kosong.</p>
	 *
	 * @return kelompok mapel, atau {@code null} bila belum ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_matapelajaran_id", nullable = true)
	public KelompokMatapelajaran getKelompokMatapelajaran() {
		kelompokMatapelajaran = check(kelompokMatapelajaran);
		return kelompokMatapelajaran;
	}

	/**
	 * Menyetel kelompok kurikuler mapel. Tanpa validasi; sama seperti
	 * {@link #setJenisPenilaian(JenisPenilaian)}, pembatasan ke kelompok milik sekolah yang sama
	 * hanya berlaku di lapisan UI.
	 *
	 * @param kelompokMatapelajaran kelompok mapel baru; {@code null} diterima
	 */
	public void setKelompokMatapelajaran(KelompokMatapelajaran kelompokMatapelajaran) {
		this.kelompokMatapelajaran = kelompokMatapelajaran;
	}

	/**
	 * Mengembalikan penanda "mapel memiliki nilai keterampilan", dengan normalisasi {@code null}
	 * &rarr; {@code true}.
	 *
	 * <p><b>Tidak ada pemanggil</b> di luar berkas ini — lihat catatan pada field
	 * {@link #terdapatNilaiKeterampilan}. Kolomnya tetap ditulis {@code true} setiap flush.</p>
	 *
	 * @return selalu {@code true} kecuali baris secara eksplisit disetel {@code false}
	 */
	public Boolean getTerdapatNilaiKeterampilan() {
		return terdapatNilaiKeterampilan == null ? true : terdapatNilaiKeterampilan;
	}

	/**
	 * Menyetel penanda "mapel memiliki nilai keterampilan". Tanpa validasi dan tanpa pemanggil
	 * produksi.
	 *
	 * @param terdapatNilaiKeterampilan penanda baru
	 */
	public void setTerdapatNilaiKeterampilan(Boolean terdapatNilaiKeterampilan) {
		this.terdapatNilaiKeterampilan = terdapatNilaiKeterampilan;
	}

	/**
	 * Mengembalikan penanda "mapel memiliki nilai predikat", dengan normalisasi {@code null}
	 * &rarr; {@code true}. Sama seperti {@link #getTerdapatNilaiKeterampilan()}, tidak ada
	 * pemanggil di luar berkas ini.
	 *
	 * @return selalu {@code true} kecuali baris secara eksplisit disetel {@code false}
	 */
	public Boolean getTerdapatNilaiPredikat() {
		return terdapatNilaiPredikat == null ? true : terdapatNilaiPredikat;
	}

	/**
	 * Menyetel penanda "mapel memiliki nilai predikat". Tanpa validasi dan tanpa pemanggil
	 * produksi.
	 *
	 * @param terdapatNilaiPredikat penanda baru
	 */
	public void setTerdapatNilaiPredikat(Boolean terdapatNilaiPredikat) {
		this.terdapatNilaiPredikat = terdapatNilaiPredikat;
	}

	/**
	 * Mengembalikan KKM (Kriteria Ketuntasan Minimal) mapel, dengan normalisasi {@code null}
	 * &rarr; {@code 70.0}.
	 *
	 * <p><b>Efek samping:</b> nilai default 70,0 itu ikut tertulis ke kolom saat flush (property
	 * access). Karena {@code MatapelajaranAction} mengisi {@code MyDoublebox} dari getter ini,
	 * mengosongkan kotak KKM pada form tidak mengembalikan keadaan "belum ditentukan" — yang
	 * tersimpan tetap 70,0.</p>
	 * <p><b>Pemakaian:</b> nilai ini bukan sekadar tampilan. {@code GrupPenilaianUtil}
	 * <b>menyubstitusikan</b> angka KKM ke dalam string rumus penilaian (token {@code " kkm "}
	 * atau {@code " KKM "}) yang lalu dievaluasi untuk menentukan predikat/ketuntasan; nilainya
	 * juga masuk ke parameter laporan rapor ({@code LaporanRaporSiswa}), SK guru, dan jadwal.
	 * Mengubah KKM sebuah mapel karenanya berpengaruh langsung pada hasil evaluasi nilai
	 * siswa.</p>
	 *
	 * @return KKM mapel; tidak pernah {@code null}
	 */
	public Double getKkm() {
		return kkm == null ? 70.0 : kkm;
	}

	/**
	 * Menyetel KKM mapel. Tanpa validasi rentang — nilai negatif atau di atas 100 diterima apa
	 * adanya dan akan ikut masuk ke rumus penilaian.
	 *
	 * @param kkm KKM baru; {@code null} akan terbaca sebagai {@code 70.0}
	 */
	public void setKkm(Double kkm) {
		this.kkm = kkm;
	}

	/**
	 * Mengembalikan nama mapel dalam bahasa Inggris, dengan <b>fallback</b> ke {@link #getNama()}
	 * bila kolomnya {@code null}.
	 *
	 * <p><b>Non-obvious:</b> fallback ini bersifat sekali jalan. {@code MatapelajaranAction.init(...)}
	 * mengisi textbox "Nama (English)" dari getter ini, dan {@code onSave()} menuliskannya kembali
	 * lewat {@link #setNamaEn(String)}. Begitu sebuah baris pernah disimpan lewat form, kolom
	 * {@code namaEn} berisi salinan nama Indonesia dan tidak lagi mengikuti perubahan
	 * {@link #getNama()}. Renderer daftar memanfaatkan kesamaan tersebut untuk menyembunyikan
	 * baris nama duplikat ({@code if (!getNama().equalsIgnoreCase(getNamaEn()))}).</p>
	 * <p>Perhatikan fallback memakai perbandingan {@code null} saja: string kosong {@code ""}
	 * dikembalikan apa adanya dan <b>tidak</b> jatuh balik ke nama Indonesia.</p>
	 *
	 * @return nama Inggris, atau nama Indonesia bila kolomnya belum diisi
	 */
	public String getNamaEn() {
		return namaEn == null ? getNama() : namaEn;
	}

	/**
	 * Menyetel nama mapel dalam bahasa Inggris. Tanpa validasi.
	 *
	 * @param namaEn nama Inggris baru; {@code null} mengaktifkan kembali fallback ke
	 *               {@link #getNama()}
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * Mengembalikan nama mapel dalam aksara Arab, dengan fallback ke {@link #getNama()} bila
	 * kolomnya {@code null}. Berlaku catatan "fallback sekali jalan" yang sama seperti
	 * {@link #getNamaEn()}.
	 *
	 * <p>Baris form untuk kolom ini disembunyikan ({@code row.setVisible(false)}) di
	 * {@code MatapelajaranAction}, tetapi {@code onSave()} tetap menuliskannya — sehingga
	 * materialisasi nama Indonesia ke kolom ini terjadi tanpa terlihat pengguna.</p>
	 *
	 * @return nama Arab, atau nama Indonesia bila kolomnya belum diisi
	 */
	public String getNamaAr() {
		return namaAr == null ? getNama() : namaAr;
	}

	/**
	 * Menyetel nama mapel dalam aksara Arab. Tanpa validasi.
	 *
	 * @param namaAr nama Arab baru; {@code null} mengaktifkan kembali fallback ke
	 *               {@link #getNama()}
	 */
	public void setNamaAr(String namaAr) {
		this.namaAr = namaAr;
	}

	/**
	 * Mengembalikan nama mapel dalam aksara Tionghoa, dengan fallback ke {@link #getNama()} bila
	 * kolomnya {@code null}. Barisnya juga disembunyikan pada form — lihat {@link #getNamaAr()}.
	 *
	 * @return nama Tionghoa, atau nama Indonesia bila kolomnya belum diisi
	 */
	public String getNamaCh() {
		return namaCh == null ? getNama() : namaCh;
	}

	/**
	 * Menyetel nama mapel dalam aksara Tionghoa. Tanpa validasi.
	 *
	 * @param namaCh nama Tionghoa baru; {@code null} mengaktifkan kembali fallback ke
	 *               {@link #getNama()}
	 */
	public void setNamaCh(String namaCh) {
		this.namaCh = namaCh;
	}
}
