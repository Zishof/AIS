package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;

/**
 * Entity Hibernate untuk <b>definisi mata kuliah</b> pada tabel {@code public.matakuliah}:
 * satu baris = satu mata kuliah generik di kurikulum (kode, nama, bobot SKS, capaian
 * pembelajaran), <b>tanpa</b> keterikatan pada tahun akademik, semester, kelas, dosen,
 * atau mahasiswa mana pun.
 *
 * <h3>Beda dengan {@link Perkuliahan} — definisi vs instansiasi</h3>
 * <p>{@code Matakuliah} adalah <i>definisi</i>; {@link Perkuliahan} adalah <i>instansiasi</i>
 * (penawaran) mata kuliah itu untuk satu periode. Satu baris {@code Matakuliah} melahirkan
 * banyak baris {@code Perkuliahan}: kelas A/B/C, semester ganjil/genap, semester pendek, tahun
 * ajaran berbeda. Karena itu {@code Perkuliahan} sengaja TIDAK menyimpan bobot SKS sendiri —
 * bobot selalu dibaca ulang lewat {@code getMatakuliah().getSks()}. Konsekuensi praktisnya:
 * mengubah SKS di sini <b>langsung mengubah</b> bobot semua kelas (termasuk kelas periode lampau)
 * yang menunjuk mata kuliah ini; ini kerap dipakai sengaja, tapi juga sumber kejutan pada
 * transkrip lama. Kalau perubahan tidak boleh menyentuh angkatan lama, praktik yang dipakai di
 * repo ini adalah membuat baris {@code Matakuliah} <i>baru</i> lalu menghubungkannya lewat
 * {@link MatakuliahEkivalen} (lihat bagian "Ekivalensi" di bawah), bukan mengedit baris lama.</p>
 *
 * <h3>Identitas</h3>
 * <ul>
 * <li>{@code kode} — kode mata kuliah, WAJIB ({@code nullable = false}). Tidak ada
 * <i>unique constraint</i> di level pemetaan ini; keunikan (biasanya per prodi) dijaga oleh
 * lapisan aksi/UI, bukan oleh DB. Perhatikan efek samping normalisasi di {@link #getKode()}.</li>
 * <li>{@code nama} (WAJIB) dan {@code namaEn} — nama Indonesia dan Inggris; {@code singkatan}
 * untuk tampilan sempit (jadwal, kartu ujian).</li>
 * <li>{@code prefix} ({@link Prefix}) — awalan kode yang dikelola sebagai master terpisah.</li>
 * <li>{@code jurusan} ({@link Jurusan}) — prodi/jurusan pemilik, WAJIB ({@code nullable = false}).
 * Pembatasan pengambilan lintas prodi diatur {@link #getBolehDiambilProdiLain()}.</li>
 * </ul>
 *
 * <h3>Bobot SKS dan rinciannya</h3>
 * <p>{@code sks} adalah bobot total yang muncul di KRS/KHS/transkrip. Di sampingnya ada
 * <b>rincian per-bentuk-pembelajaran</b> yang dipakai terutama untuk ekspor Feeder PDDikti:
 * {@code sksDiskusi} (tatap muka/teori), {@code sksPraktek}, {@code sksPraktekLapangan},
 * {@code sksSimulasi}, serta {@code sksSubMk} untuk mata kuliah yang dipecah menjadi modul.</p>
 * <p><b>Penting:</b> bendera {@code terdapatPraktek}, {@code terdapatDiskusi},
 * {@code terdapatSimulasi}, {@code terdapatPraktekLapangan}, {@code merupakanMkPraktek}, dan
 * {@code merupakanMkTeori} adalah <b>nilai turunan</b>. Getter-nya menghitung ulang dari angka
 * SKS dan MENIMPA isi kolomnya; apa pun yang tersimpan di DB (atau di-{@code set} pemanggil)
 * akan diabaikan. Jangan pernah memakai kolom-kolom itu di klausa {@code Restrictions}/HQL
 * dengan asumsi isinya mutakhir — nilai di DB baru menyusul setelah baris ini ter-<i>flush</i>
 * sekali lagi. Selain itu {@link #getSksDiskusi()} akan <b>mengisi sendiri</b> {@code sksDiskusi}
 * dengan {@code getSks()} bila seluruh rincian bernilai nol.</p>
 *
 * <h3>Penggolongan</h3>
 * <ul>
 * <li>{@code status} — teks bebas ("Wajib" default, "Pilihan", "Wajib Peminatan", ...).
 * Ini DENORMALISASI dari master {@link StatusMatakuliah}: yang disimpan hanya <i>nama</i>-nya,
 * bukan foreign key, sehingga mengganti nama di master tidak ikut mengubah baris ini
 * (lihat pencocokan berbasis nama di {@code FeederExporterGenerator}).</li>
 * <li>{@code jenisMatakuliah} — teks bebas dari tiga pilihan UI (mata kuliah dalam kampus,
 * luar kampus, atau lainnya); label diambil dari berkas bahasa, jadi nilainya ikut bahasa
 * aktif saat penyimpanan.</li>
 * <li>{@code kelompokMatakuliah} ({@link KelompokMatakuliah}) dan
 * {@code tingkatKesulitanMatakuliah} ({@link TingkatKesulitanMatakuliah}).</li>
 * <li>{@code extraKulikuler}, {@code merupakanModul}, {@code merupakanPraPerkuliahan},
 * {@code merupakanPerkuliahanUmum}, {@code aktif}, {@code milikUniversitas}
 * (lihat catatan cacat pada {@link #getMilikUniversitas()}).</li>
 * <li>{@code jumlahMaksimalSksJikaAmbilMkIni} — pagu SKS KRS bila mata kuliah ini diambil
 * (default 30), dipakai pada validasi beban studi.</li>
 * </ul>
 *
 * <h3>Kurikulum / OBE</h3>
 * <p>{@code deskripsiPembelajaran} dan {@code capaianPembelajaranProdi} adalah teks bebas.
 * Empat kolom berikutnya — {@code bahanKajian} ({@link BahanKajian}), {@code capaianLulusan},
 * {@code profilLulusan}, {@code capaianPembelajaranLulusan} — menyimpan <b>daftar id
 * ber-format CSV yang dibungkus koma</b> ({@code ",12,45,"}). Bentuk berpembungkus ini disengaja
 * agar pencarian keanggotaan cukup dengan {@code contains(",id,")}. Keempat getter-nya memakai
 * algoritma normalisasi yang sama: rapikan koma ganda, buang duplikat, bungkus ulang; satu
 * konsekuensinya adalah <b>urutan elemen tidak dipertahankan</b> (lihat {@link #getBahanKajian()}).</p>
 *
 * <h3>Kelengkapan dokumen &amp; ujian</h3>
 * <p>{@code adaSap}, {@code adaSilabus}, {@code adaBahanAjar}, {@code adaAcaraPraktek},
 * {@code adaDiktat} adalah penanda administratif untuk laporan kelengkapan perangkat ajar.
 * {@code terdapatUts}/{@code terdapatUas} (default {@code true}) menentukan apakah kelas dari
 * mata kuliah ini punya UTS/UAS — dibaca antara lain oleh perhitungan tagihan biaya ujian
 * ({@code PembayaranNominalModifikasiHelper}), jadi mematikannya berdampak ke keuangan,
 * bukan sekadar ke tampilan.</p>
 *
 * <h3>Ekivalensi</h3>
 * <p>Relasi "mata kuliah A setara dengan mata kuliah B" TIDAK disimpan sebagai field di sini,
 * melainkan sebagai baris {@link MatakuliahEkivalen}. Yang ada di kelas ini adalah enam method
 * pengelola <i>flag store</i>-nya: sebuah berkas JSON di luar basis data (kunci
 * {@code "ekivalen_<id>"}) berisi peta <code>{id MatakuliahEkivalen &rarr; path berkas}</code>.
 * Alurnya {@link #reInitEkivalen()} (bangun ulang dari DB) &rarr; {@link #ambilEkivalen(String)}
 * (baca cepat tanpa query), dengan {@link #populateEkivalen(MatakuliahEkivalen)} dan
 * {@link #removeEkivalen(Serializable)} sebagai penambal inkremental yang dipanggil dari
 * {@code AuditListener}. Pola berkas JSON yang sama dipakai {@link Perkuliahan} untuk daftar
 * peserta. <b>Asimetri yang perlu diingat:</b> {@code reInitEkivalen()} mengumpulkan baris di
 * mana {@code this} muncul di sisi MANA PUN, sedangkan {@code ambilEkivalen()} hanya
 * meresolusi baris yang {@code this}-nya berada di sisi sumber.</p>
 *
 * <h3>Relasi yang dipegang entity LAIN</h3>
 * <p>Kelas ini nyaris tidak punya koleksi; hampir semua relasi dimiliki pihak seberang, jadi
 * carilah di sana, bukan di sini: {@link KurikulumPunyaMatakuliah} (penempatan pada
 * {@link Kurikulum} beserta semesternya — inilah sebabnya "semester ke berapa" BUKAN field di
 * kelas ini), {@link MatakuliahPrasyarat} (prasyarat, sampai 10 slot + nilai minimal lulus),
 * {@link MatakuliahEkivalen}, {@link KelompokMatakuliahPunyaMatakuliah},
 * {@link MatakuliahPunyaBukuBahanAjar}, {@link MatakuliahBerbayar},
 * {@link MatakuliahAwalKonversi}, dan tentu {@link Perkuliahan}.</p>
 *
 * <h3>Jebakan: getter yang tidak bebas efek samping</h3>
 * <p>Pemetaan Hibernate di kelas ini memakai <i>property access</i> (anotasi menempel pada
 * getter), sehingga <b>nilai yang di-flush ke DB adalah nilai yang dikembalikan getter</b>, bukan
 * isi field mentah. Sejumlah getter di sini menulis balik ke field-nya sendiri, sehingga sekadar
 * MEMBACA entity yang masih <i>attached</i> dapat memicu {@code UPDATE} spontan sekaligus baris
 * revisi Envers baru (kelas ini {@link Audited}). Yang berperilaku begitu:
 * {@link #getKode()} (bila konfigurasi {@code matakuliah_tanpa_spasi} aktif),
 * {@link #getStatus()}, {@link #getSks()}, {@link #getMilikUniversitas()},
 * {@link #getSksDiskusi()} dan seluruh keluarga {@code getTerdapat*}/{@code getMerupakanMk*},
 * serta empat getter CSV OBE. Selain itu {@link #reInitEkivalen()} dan
 * {@link #ambilEkivalen(String)} <b>menutup {@code Session} Hibernate milik pemanggil</b>
 * ({@code session.close()} + {@link HibernateUtil#closeSession()}) — jangan panggil keduanya di
 * tengah transaksi yang masih akan dipakai.</p>
 *
 * <h3>Warisan {@link GeneralValueObject}</h3>
 * <p>Kontrak umum {@code id}/{@code equals}/{@code hashCode}/{@code compareTo}, resolusi proxy
 * lazy lewat {@link GeneralValueObject#check(Object)} (dipakai semua getter relasi di kelas ini),
 * cache entity, dan penanda {@code udah}/{@code belum} dijelaskan lengkap di
 * {@link ais.database.model.GeneralValueObject} — jangan diulang di sini.</p>
 *
 * @see Perkuliahan
 * @see KurikulumPunyaMatakuliah
 * @see MatakuliahEkivalen
 * @see MatakuliahPrasyarat
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "matakuliah")
public class Matakuliah extends GeneralValueObject {

	/**
	 * Versi serialisasi. Entity ini ikut diserialisasi ke cache {@code MemoryCacheUtil}
	 * dan ke berkas JSON, jadi nilainya tidak boleh diubah tanpa alasan.
	 */
	private static final long serialVersionUID = -480809958171449633L;
	/** Primary key {@code public.matakuliah.id}, IDENTITY. */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Field bayangan.</b> {@link GeneralValueObject} sudah punya field {@code oleh}
	 * beserta getter/setter-nya; deklarasi ulang di sini menutupi ({@code shadow}) milik induk,
	 * sehingga {@code ((GeneralValueObject) mk).oleh} dan {@code mk.oleh} adalah dua slot
	 * berbeda. Pola bayangan yang sama muncul di banyak entity repo ini (lihat
	 * {@code Dosen}, {@code Pegawai}). Selalu akses lewat {@link #getOleh()}/{@link #setOleh(String)}
	 * agar konsisten dengan pemetaan Hibernate kelas ini.</p>
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini. Field bayangan atas
	 * {@code GeneralValueObject.olehId} — lihat catatan pada {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir pengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah.
	 *
	 * <p>Nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> (bukan disimpan
	 * sebagai null) sehingga jejak audit yang sudah ada tidak bisa terhapus oleh jalur
	 * penyimpanan yang kebetulan tidak menyertakan identitas pengguna.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Nilai null/kosong diabaikan, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir pengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyegarkan stempel waktu audit tepat sebelum
	 * {@code UPDATE} dikirim ke DB.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}. Dipanggil oleh
	 * penyedia persistensi, bukan oleh kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Field bayangan atas
	 * {@code GeneralValueObject.tanggal_dirubah} (lihat catatan pada {@link #oleh}); diinisialisasi
	 * ke waktu sekarang saat object dibuat, lalu diperbarui {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi TIMESTAMP).
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks {@code "<id>-<kode>-<nama>"}, mis. {@code "1204-RPL211401-REKAYASA PERANGKAT LUNAK"}.
	 *
	 * <p>Dipakai luas pada log dan pada komponen ZK yang menampilkan mata kuliah apa adanya.
	 * <b>Perhatikan:</b> memanggil {@link #getKode()} dan {@link #getNama()}, jadi ikut membawa
	 * efek samping normalisasi {@code getKode()} — sekadar mencetak entity ini bisa mengubah
	 * isi field {@code kode}.</p>
	 *
	 * @return gabungan id, kode, dan nama dipisah tanda hubung
	 */
	public String toString() {
		return getId() + "-" + getKode() + "-" + getNama();
	}

	// public static final String MATAKULIAH_UIN = "UIN";
	// public static final String MATAKULIAH_LUAR_UIN = "Luar UIN";

	/** Master awalan kode mata kuliah; opsional. */
	private Prefix prefix;
	/** Master tingkat kesulitan; opsional, hanya dipakai untuk pelaporan. */
	private TingkatKesulitanMatakuliah tingkatKesulitanMatakuliah;
	/** Master kelompok/rumpun mata kuliah (MKU, MKDK, ...); opsional. */
	private KelompokMatakuliah kelompokMatakuliah;
	/** Kode mata kuliah; WAJIB. Dinormalisasi saat dibaca — lihat {@link #getKode()}. */
	private String kode;
	/** Nama mata kuliah (Bahasa Indonesia); WAJIB. */
	private String nama;
	/** Nama mata kuliah dalam Bahasa Inggris; opsional. */
	private String namaEn;
	/** Bobot SKS total yang muncul di KRS/KHS/transkrip. */
	private Integer sks;
	/** Pagu SKS KRS bila mata kuliah ini diambil; default 30 lewat getter. */
	private Integer jumlahMaksimalSksJikaAmbilMkIni;
	/** Bobot SKS per sub-mata-kuliah bila {@code merupakanModul} bernilai true. */
	private Double sksSubMk;
	/** Status kurikuler sebagai teks bebas, hasil denormalisasi nama {@link StatusMatakuliah}. */
	private String status = "Wajib";
	/** Prodi/jurusan pemilik; WAJIB ({@code nullable = false}). */
	private Jurusan jurusan;
	/** Singkatan untuk tampilan sempit (jadwal, kartu ujian). */
	private String singkatan;
	/** Keberadaan mata kuliah (dalam kampus / luar kampus / lainnya) sebagai teks bebas. */
	private String jenisMatakuliah;
	/** Catatan bebas; tidak dipakai logika bisnis mana pun. */
	private String keterangan;
	/** Lihat cacat yang didokumentasikan pada {@link #getMilikUniversitas()} — praktis selalu true. */
	private Boolean milikUniversitas;
	/** Izin pengambilan oleh mahasiswa prodi lain; default true. */
	private Boolean bolehDiambilProdiLain = true;
	/** Turunan dari {@code sksPraktek}; ditimpa ulang di {@link #getTerdapatPraktek()}. */
	private Boolean terdapatPraktek = false;
	/** Rincian SKS bentuk pembelajaran praktikum. */
	private Integer sksPraktek = 0;
	/** Turunan dari {@code sksDiskusi}; ditimpa ulang di {@link #getTerdapatDiskusi()}. */
	private Boolean terdapatDiskusi = false;
	/** Turunan dari {@code sksPraktek}; ditimpa ulang di {@link #getMerupakanMkPraktek()}. */
	private Boolean merupakanMkPraktek = false;
	/** Turunan dari {@code sksDiskusi}; ditimpa ulang di {@link #getMerupakanMkTeori()}. */
	private Boolean merupakanMkTeori = false;
	/** Penanda mata kuliah yang dipecah menjadi modul-modul ber-{@code sksSubMk}. */
	private Boolean merupakanModul = false;

	/** Turunan dari {@code sksSimulasi}; ditimpa ulang di {@link #getTerdapatSimulasi()}. */
	private Boolean terdapatSimulasi = false;
	/** Turunan dari {@code sksPraktekLapangan}; ditimpa ulang di {@link #getTerdapatPraktekLapangan()}. */
	private Boolean terdapatPraktekLapangan = false;
	/** Penanda mata kuliah matrikulasi/pra-perkuliahan (di luar beban kurikulum normal). */
	private Boolean merupakanPraPerkuliahan;
	/** Penanda mata kuliah umum lintas prodi. */
	private Boolean merupakanPerkuliahanUmum;

	/** Apakah kelas dari mata kuliah ini menyelenggarakan UTS; default true. Berdampak ke tagihan biaya ujian. */
	private Boolean terdapatUts = true;
	/** Apakah kelas dari mata kuliah ini menyelenggarakan UAS; default true. Berdampak ke tagihan biaya ujian. */
	private Boolean terdapatUas = true;

	/** Rincian SKS bentuk pembelajaran tatap muka/teori ("diskusi"). */
	private Integer sksDiskusi = 0;
	/** Penanda kegiatan ekstrakurikuler (dikecualikan dari KHS pada {@code LaporanKHS}). */
	private Boolean extraKulikuler;
	/** Rincian SKS bentuk pembelajaran praktik lapangan. */
	private Integer sksPraktekLapangan;
	/** Rincian SKS bentuk pembelajaran simulasi. */
	private Integer sksSimulasi;

	/** Penanda kelengkapan SAP. */
	private Boolean adaSap;
	/** Penanda kelengkapan silabus. */
	private Boolean adaSilabus;
	/** Penanda kelengkapan bahan ajar. */
	private Boolean adaBahanAjar;
	/** Penanda kelengkapan acara praktek. */
	private Boolean adaAcaraPraktek;
	/** Penanda kelengkapan diktat. */
	private Boolean adaDiktat;

	/** Awal masa berlaku definisi mata kuliah; opsional, presisi DATE. */
	private Date tanggalMulai;
	/** Akhir masa berlaku definisi mata kuliah; opsional, presisi DATE. */
	private Date tanggalSampai;

	/** Metode/bahasan perkuliahan sebagai teks bebas; diekspor ke Feeder. */
	private String metodeKuliah;

	/** Id mata kuliah di Feeder PDDikti ({@code id_matkul}); kunci sinkronisasi. */
	private String feeder;
	/** Cadangan/riwayat id Feeder lain (kolom {@code text}); dipakai saat id Feeder berganti. */
	private String feeders;

	/** Deskripsi pembelajaran (teks panjang). */
	private String deskripsiPembelajaran;
	/** Capaian pembelajaran tingkat prodi (teks panjang). */
	private String capaianPembelajaranProdi;

	/** Penanda mata kuliah masih dipakai; default true lewat getter. */
	private Boolean aktif;

	/**
	 * Slot transien untuk menempelkan deskripsi kurikulum saat entity dipakai di layar.
	 *
	 * <p><b>Tidak terpakai.</b> Penelusuran seluruh pohon sumber (termasuk berkas {@code .zul})
	 * tidak menemukan satu pun pembaca atau penulis field ini di luar deklarasinya. Karena
	 * {@code transient} dan {@code public}, field ini juga tidak ikut dipetakan Hibernate maupun
	 * diserialisasi. Aman dihapus, tetapi dibiarkan agar tidak memecah kompilasi kode luar.</p>
	 */
	public transient String descKurikulum;

	/** CSV id {@code BahanKajian} berpembungkus koma — lihat {@link #getBahanKajian()}. */
	private String bahanKajian;
	/** CSV id capaian lulusan berpembungkus koma — lihat {@link #getCapaianLulusan()}. */
	private String capaianLulusan;
	/** CSV id profil lulusan berpembungkus koma — lihat {@link #getProfilLulusan()}. */
	private String profilLulusan;
	/** CSV id CPL berpembungkus koma — lihat {@link #getCapaianPembelajaranLulusan()}. */
	private String capaianPembelajaranLulusan;
	/** Skema konversi nilai angka ke huruf khusus mata kuliah ini; bila null dipakai skema global. */
	private JenisNilaiHurufMatakuliah jenisNilaiHuruf;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JavaBeans.
	 *
	 * <p>Menghasilkan object kosong: {@code id} null (belum tersimpan), {@code status} "Wajib",
	 * {@code bolehDiambilProdiLain} true, {@code terdapatUts}/{@code terdapatUas} true, dan
	 * rincian SKS bernilai nol — sesuai nilai awal field masing-masing.</p>
	 */
	public Matakuliah() {
	}

	/**
	 * Membuat rujukan ringan ke sebuah baris mata kuliah hanya berbekal primary key.
	 *
	 * <p>Object hasil konstruktor ini <b>tidak berisi data</b> selain id; seluruh field lain
	 * masih nilai awalnya. Bentuk ini dipakai sebagai kriteria pencarian
	 * ({@code Restrictions.eq("matakuliah", new Matakuliah(id))}) dan sebagai nilai FK saat
	 * menyimpan relasi, bukan untuk dibaca isinya. Untuk memuat data sungguhan, lewatkan
	 * object ini ke {@link GeneralValueObject#check(Object)} atau muat dari session.</p>
	 *
	 * @param id primary key mata kuliah
	 */
	public Matakuliah(Long id) {
		this.id = id;
	}

	/**
	 * Membuat mata kuliah baru dengan kode dan nama, tanpa bobot SKS.
	 *
	 * <p>{@code sks} tetap {@code null} sehingga {@link #getSks()} akan mengembalikan 0.
	 * Perhatikan {@code jurusan} juga belum terisi padahal kolomnya {@code nullable = false} —
	 * penyimpanan akan gagal sebelum {@link #setJurusan(Jurusan)} dipanggil.</p>
	 *
	 * @param kode kode mata kuliah
	 * @param nama nama mata kuliah
	 */
	public Matakuliah(String kode, String nama) {
		this.kode = kode;
		this.nama = nama;
	}

	/**
	 * Membuat mata kuliah baru lengkap dengan bobot SKS totalnya.
	 *
	 * <p>Rincian per-bentuk-pembelajaran ({@code sksDiskusi} dan kawan-kawan) tidak ikut diisi;
	 * {@link #getSksDiskusi()} nanti akan mengisinya sendiri dengan nilai {@code sks} ini pada
	 * pembacaan pertama. Sama seperti konstruktor dua argumen, {@code jurusan} belum terisi.</p>
	 *
	 * @param kode kode mata kuliah
	 * @param nama nama mata kuliah
	 * @param sks  bobot SKS total
	 */
	public Matakuliah(String kode, String nama, Integer sks) {
		this.kode = kode;
		this.nama = nama;
		this.sks = sks;
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Kolom {@code id} bertipe IDENTITY dan {@code insertable = false}: nilainya dihasilkan
	 * basis data saat {@code INSERT}, jadi entity baru ber-{@code id} null sampai ter-flush.
	 * Kontrak {@code id} terhadap {@code equals}/{@code hashCode}/{@code compareTo} dijelaskan
	 * di {@link ais.database.model.GeneralValueObject}.</p>
	 *
	 * @return primary key, atau {@code null} bila entity belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key.
	 *
	 * <p>Normalnya diisi Hibernate seusai {@code INSERT}. Mengubahnya secara manual pada entity
	 * yang sudah persistent akan mengacaukan identitas dan cache — hindari.</p>
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode mata kuliah yang sudah dinormalisasi.
	 *
	 * <p><b>Normalisasi.</b> Bila konfigurasi sistem {@code matakuliah_tanpa_spasi} aktif
	 * ({@link ais.common.MemoryDbUtil#apakahTanpaSpasi()}), seluruh tanda hubung dan spasi
	 * dibuang dari kode sehingga {@code "RPL 211-401"} menjadi {@code "RPL211401"}. Kode
	 * {@code null} diubah menjadi string kosong. Hasil akhir selalu di-{@code trim()}.</p>
	 *
	 * <p><b>Efek samping — penting.</b> Normalisasi ini <b>ditulis balik ke field</b>
	 * {@code kode}, bukan sekadar dihitung untuk nilai kembali. Karena kelas ini dipetakan
	 * dengan <i>property access</i>, sekadar membaca kode pada entity yang masih attached bisa
	 * membuat Hibernate menganggap baris kotor lalu mengirim {@code UPDATE} beserta satu baris
	 * revisi Envers baru. Jadi mengaktifkan {@code matakuliah_tanpa_spasi} bukan hanya mengubah
	 * tampilan: lambat laun kode di basis data ikut kehilangan spasi/tanda hubungnya secara
	 * permanen, dan pencocokan dengan kode Feeder PDDikti yang masih memakai pemisah bisa
	 * meleset. Nilai cache ({@link ais.common.MemoryDbUtil}) hanya dibaca ulang setelah
	 * {@code resetLocalReferences()}, sehingga perubahan konfigurasi tidak langsung terasa.</p>
	 *
	 * @return kode mata kuliah ter-trim; string kosong bila kolom kosong, tidak pernah null
	 */
	@Column(name = "kode", nullable = false, length = 100)
	public String getKode() {
		if (MemoryDbUtil.apakahTanpaSpasi() && kode != null) {
			kode = org.apache.commons.lang3.StringUtils.replace(kode, "-", "");
			kode = org.apache.commons.lang3.StringUtils.replace(kode, " ", "");
			kode = org.apache.commons.lang3.StringUtils.replace(kode, " ", "");
		}
		if (kode == null) {
			kode = "";
		}
		return this.kode.trim();
	}

	/**
	 * Menyetel kode mata kuliah apa adanya, tanpa normalisasi.
	 *
	 * <p>Normalisasi baru terjadi saat dibaca lewat {@link #getKode()}, jadi nilai yang
	 * disimpan di sini bisa berbeda dari yang nanti dikembalikan.</p>
	 *
	 * @param kode kode mata kuliah; boleh null
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama mata kuliah (Bahasa Indonesia) yang sudah di-trim.
	 *
	 * <p>Berbeda dengan {@link #getKode()}, method ini bebas efek samping dan tetap
	 * mengembalikan {@code null} bila kolom kosong — pemanggil wajib menjaga NPE.</p>
	 *
	 * @return nama mata kuliah ter-trim, atau {@code null}
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama mata kuliah.
	 *
	 * @param nama nama mata kuliah; kolom {@code nullable = false} sehingga null akan
	 *             menggagalkan penyimpanan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan bobot SKS total mata kuliah ini.
	 *
	 * <p>Inilah angka yang dipakai seluruh perhitungan beban studi: KRS, KHS, IP/IPK, transkrip,
	 * batas SKS dosen, sampai tagihan per-SKS. {@link Perkuliahan} tidak menyimpan SKS sendiri
	 * dan selalu membaca nilai ini.</p>
	 *
	 * <p><b>Efek samping.</b> Bila kolom masih {@code null}, field diisi 0 (bukan hanya nilai
	 * kembalinya), sehingga pembacaan pertama pada entity attached dapat memicu {@code UPDATE}.
	 * Karena null diperlakukan sebagai 0, mata kuliah yang belum diisi bobotnya tidak akan
	 * terdeteksi sebagai "belum diisi" oleh pemanggil — bedakan sendiri bila perlu.</p>
	 *
	 * @return bobot SKS total; 0 bila belum diisi, tidak pernah null
	 */
	@Column(name = "sks")
	public Integer getSks() {
		if (sks == null) {
			sks = 0;
		}
		return this.sks;
	}

	/**
	 * Menyetel bobot SKS total.
	 *
	 * <p>Ingat dampaknya bersifat retroaktif: seluruh {@link Perkuliahan} dan
	 * {@code Detailperkuliahan} dari periode mana pun yang menunjuk mata kuliah ini akan
	 * langsung memakai bobot baru.</p>
	 *
	 * @param sks bobot SKS total; null akan dibaca sebagai 0
	 */
	public void setSks(Integer sks) {
		this.sks = sks;
	}

	/**
	 * Menyetel status kurikuler sebagai teks bebas.
	 *
	 * <p>Nilai yang lazim: {@code "Wajib"}, {@code "Pilihan"}, {@code "Wajib Peminatan"},
	 * mengikuti nama pada master {@link StatusMatakuliah}. Karena yang disimpan hanya teks,
	 * penulisan yang berbeda ejaan tidak akan cocok saat diekspor ke Feeder.</p>
	 *
	 * @param status nama status; null/kosong akan dibaca sebagai "Wajib"
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan status kurikuler mata kuliah ({@code "Wajib"}, {@code "Pilihan"}, ...).
	 *
	 * <p>Ini teks bebas hasil denormalisasi nama {@link StatusMatakuliah} — bukan foreign key.
	 * {@code FeederExporterGenerator} memetakannya balik ke master dengan membandingkan nama
	 * secara <i>case-insensitive</i>, jadi nama master yang berubah akan memutus pemetaan itu
	 * tanpa pesan kesalahan.</p>
	 *
	 * <p><b>Efek samping.</b> Nilai null/kosong diisi ulang menjadi {@code "Wajib"} pada field,
	 * bukan hanya pada nilai kembali.</p>
	 *
	 * @return status kurikuler ter-trim; {@code "Wajib"} bila kolom kosong, tidak pernah null
	 */
	@Column(name = "status", length = 255)
	public String getStatus() {
		if (status == null || status.trim().isEmpty()) {
			status = "Wajib";
		}
		return status.trim();
	}

	/**
	 * Menyetel prodi/jurusan pemilik mata kuliah.
	 *
	 * @param jurusan prodi pemilik; kolom {@code nullable = false} sehingga null menggagalkan
	 *                penyimpanan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan prodi/jurusan pemilik mata kuliah.
	 *
	 * <p>Relasi WAJIB ({@code nullable = false}) dan menjadi dasar hampir semua penyaringan
	 * daftar mata kuliah di UI, serta sumber {@code id_prodi} saat ekspor Feeder
	 * ({@code matakuliah.getJurusan().getFeeder()}). Pemilikan prodi TIDAK dengan sendirinya
	 * melarang prodi lain mengambil mata kuliah ini — itu diatur
	 * {@link #getBolehDiambilProdiLain()}.</p>
	 *
	 * <p>Dipetakan LAZY; {@link GeneralValueObject#check(Object)} meresolusi proxy dan menyimpan
	 * hasilnya kembali ke field, sehingga getter ini tetap aman dipanggil pada entity yang sudah
	 * detached.</p>
	 *
	 * @return prodi pemilik; secara teori bisa null pada baris lama yang datanya tidak lengkap
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = false)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel singkatan mata kuliah.
	 *
	 * @param singkatan singkatan (maksimal 100 karakter); boleh null
	 */
	public void setSingkatan(String singkatan) {
		this.singkatan = singkatan;
	}

	/**
	 * Mengembalikan singkatan mata kuliah untuk tampilan sempit (jadwal, kartu ujian).
	 *
	 * <p>Tidak di-trim dan tidak diberi nilai pengganti — bisa {@code null}. Tidak ada logika
	 * yang menurunkannya otomatis dari {@link #getNama()}; bila kosong, pemanggil sendiri yang
	 * harus menyediakan cadangan.</p>
	 *
	 * @return singkatan, atau {@code null} bila belum diisi
	 */
	@Column(name = "singkatan", length = 100)
	public String getSingkatan() {
		return singkatan;
	}

	/**
	 * Mengembalikan master awalan kode ({@link Prefix}) mata kuliah ini.
	 *
	 * <p>Relasi opsional, dipetakan LAZY dan diresolusi
	 * {@link GeneralValueObject#check(Object)}. Awalan ini bersifat keterangan; ia TIDAK
	 * digabungkan otomatis ke {@link #getKode()} — kode tetap disimpan utuh apa adanya.</p>
	 *
	 * @return master prefix, atau {@code null} bila tidak dipakai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "prefix")
	public Prefix getPrefix() {
		prefix = check(prefix);
		return prefix;
	}

	/**
	 * Menyetel master awalan kode mata kuliah.
	 *
	 * @param prefix master {@link Prefix}; boleh null
	 */
	public void setPrefix(Prefix prefix) {
		this.prefix = prefix;
	}

	/**
	 * Mengembalikan master tingkat kesulitan mata kuliah.
	 *
	 * <p>Relasi opsional pada kolom {@code tingkat_kesulitan}, dipetakan LAZY dan diresolusi
	 * {@link GeneralValueObject#check(Object)}. Nilainya murni deskriptif: penelusuran pohon
	 * sumber hanya menemukan satu pemakai (layar master mata kuliah) dan tidak ada perhitungan
	 * bisnis yang bergantung padanya.</p>
	 *
	 * @return master tingkat kesulitan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tingkat_kesulitan")
	public TingkatKesulitanMatakuliah getTingkatKesulitanMatakuliah() {
		tingkatKesulitanMatakuliah = check(tingkatKesulitanMatakuliah);
		return tingkatKesulitanMatakuliah;
	}

	/**
	 * Menyetel master tingkat kesulitan mata kuliah.
	 *
	 * @param tingkatKesulitanMatakuliah master tingkat kesulitan; boleh null
	 */
	public void setTingkatKesulitanMatakuliah(TingkatKesulitanMatakuliah tingkatKesulitanMatakuliah) {
		this.tingkatKesulitanMatakuliah = tingkatKesulitanMatakuliah;
	}

	/**
	 * Menyetel keberadaan mata kuliah sebagai teks bebas.
	 *
	 * @param jenisMatakuliah label keberadaan (maksimal 50 karakter); boleh null
	 */
	public void setJenisMatakuliah(String jenisMatakuliah) {
		this.jenisMatakuliah = jenisMatakuliah;
	}

	/**
	 * Mengembalikan keberadaan mata kuliah (di dalam kampus, di luar kampus, atau lainnya).
	 *
	 * <p>Nilainya teks bebas yang diisi dari combobox pada {@code MatakuliahAction}, dan
	 * label combobox itu diambil dari berkas bahasa ({@code keberadaan_matakuliah_kampus},
	 * {@code keberadaan_matakuliah_luar_kampus}, {@code keberadaan_matakuliah_lain}).
	 * Konsekuensinya <b>isi kolom ikut bahasa antarmuka saat penyimpanan</b>, sehingga
	 * membandingkannya dengan literal tetap tidak aman. Seluruh pemakainya di repo ini
	 * memang hanya menampilkannya sebagai label.</p>
	 *
	 * @return label keberadaan mata kuliah, atau {@code null}
	 */
	@Column(name = "jenis_matakuliah", length = 50)
	public String getJenisMatakuliah() {
		return jenisMatakuliah;
	}

	/**
	 * Menyetel catatan bebas.
	 *
	 * @param keterangan catatan; boleh null
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan catatan bebas mata kuliah. Murni deskriptif, tidak dipakai logika apa pun.
	 *
	 * @return catatan, atau {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengembalikan penanda "milik universitas" — <b>selalu {@code true}</b>.
	 *
	 * <p><b>Cacat yang didokumentasikan, bukan diperbaiki.</b> Pemeriksaan {@code if
	 * (milikUniversitas == null)} yang seharusnya membungkus penugasan telah dikomentari,
	 * sehingga baris {@code milikUniversitas = true} berjalan tanpa syarat. Akibatnya nilai
	 * apa pun yang tersimpan di kolom {@code milik_universitas} — termasuk {@code false} yang
	 * sengaja disetel lewat {@link #setMilikUniversitas(Boolean)} — akan ditimpa menjadi
	 * {@code true} begitu getter ini dipanggil, dan karena pemetaan memakai <i>property
	 * access</i>, nilai {@code true} itulah yang akhirnya di-flush ke basis data. Praktis
	 * kolom ini mati untuk {@code Matakuliah}.</p>
	 *
	 * <p>Untungnya dampaknya terbatas: penelusuran pohon sumber menunjukkan tidak ada satu pun
	 * pemanggil {@code matakuliah.getMilikUniversitas()}. Kolom {@code milikUniversitas} yang
	 * benar-benar dipakai (penyaringan dosen lintas prodi) adalah milik {@code Dosen}, entity
	 * yang berbeda — jangan tertukar.</p>
	 *
	 * @return selalu {@code true}
	 */
	@Column(name = "milik_universitas")
	public Boolean getMilikUniversitas() {
		// if (milikUniversitas == null) {
		milikUniversitas = true;
		// }
		return milikUniversitas;
	}

	public void setMilikUniversitas(Boolean milikUniversitas) {
		this.milikUniversitas = milikUniversitas;
	}

	public String getNamaEn() {
		return namaEn;
	}

	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	public Boolean getBolehDiambilProdiLain() {
		if (bolehDiambilProdiLain == null) {
			bolehDiambilProdiLain = true;
		}
		return bolehDiambilProdiLain;
	}

	public void setBolehDiambilProdiLain(Boolean bolehDiambilProdiLain) {
		this.bolehDiambilProdiLain = bolehDiambilProdiLain;
	}

	public Boolean getTerdapatPraktek() {
		terdapatPraktek = getSksPraktek() > 0;
		return terdapatPraktek;
	}

	public void setTerdapatPraktek(Boolean terdapatPraktek) {
		this.terdapatPraktek = terdapatPraktek;
	}

	public Boolean getTerdapatDiskusi() {
		terdapatDiskusi = getSksDiskusi() > 0;
		return terdapatDiskusi;
	}

	public void setTerdapatDiskusi(Boolean terdapatDiskusi) {
		this.terdapatDiskusi = terdapatDiskusi;
	}

	public Integer getSksPraktek() {
		if (sksPraktek == null) {
			sksPraktek = 0;
		}

		return sksPraktek;
	}

	public void setSksPraktek(Integer sksPraktek) {
		this.sksPraktek = sksPraktek;
	}

	public Integer getSksDiskusi() {
		if (sksDiskusi == null) {
			sksDiskusi = 0;
		}

		int total = sksDiskusi + getSksPraktek() + getSksPraktekLapangan() + getSksSimulasi();
		if (total == 0 && getSks() > 0) {
			sksDiskusi = getSks();
		}

		return sksDiskusi;
	}

	public void setSksDiskusi(Integer sksDiskusi) {
		this.sksDiskusi = sksDiskusi;
	}

	public Boolean getMerupakanMkPraktek() {
		if (merupakanMkPraktek == null) {
			merupakanMkPraktek = false;
		}

		if (getSksPraktek() > 0) {
			merupakanMkPraktek = true;
		} else {
			merupakanMkPraktek = false;
		}

		return merupakanMkPraktek;
	}

	public void setMerupakanMkPraktek(Boolean merupakanMkPraktek) {
		this.merupakanMkPraktek = merupakanMkPraktek;
	}

	public Boolean getMerupakanMkTeori() {
		if (merupakanMkTeori == null) {
			merupakanMkTeori = false;
		}

		if (getSksDiskusi() > 0) {
			merupakanMkTeori = true;
		} else {
			merupakanMkTeori = false;
		}

		return merupakanMkTeori;
	}

	public void setMerupakanMkTeori(Boolean merupakanMkTeori) {
		this.merupakanMkTeori = merupakanMkTeori;
	}

	public Boolean getExtraKulikuler() {
		if (extraKulikuler == null) {
			extraKulikuler = false;
		}
		return extraKulikuler;
	}

	public void setExtraKulikuler(Boolean extraKulikuler) {
		this.extraKulikuler = extraKulikuler;
	}

	public Integer getSksPraktekLapangan() {
		if (sksPraktekLapangan == null) {
			sksPraktekLapangan = 0;
		}
		return sksPraktekLapangan;
	}

	public void setSksPraktekLapangan(Integer sksPraktekLapangan) {
		this.sksPraktekLapangan = sksPraktekLapangan;
	}

	public Integer getSksSimulasi() {
		if (sksSimulasi == null) {
			sksSimulasi = 0;
		}
		return sksSimulasi;
	}

	public void setSksSimulasi(Integer sksSimulasi) {
		this.sksSimulasi = sksSimulasi;
	}

	public Boolean getTerdapatSimulasi() {
		terdapatSimulasi = getSksSimulasi() > 0;
		return terdapatSimulasi;
	}

	public void setTerdapatSimulasi(Boolean terdapatSimulasi) {
		this.terdapatSimulasi = terdapatSimulasi;
	}

	public Boolean getTerdapatPraktekLapangan() {
		terdapatPraktekLapangan = getSksPraktekLapangan() > 0;
		return terdapatPraktekLapangan;
	}

	public void setTerdapatPraktekLapangan(Boolean terdapatPraktekLapangan) {
		this.terdapatPraktekLapangan = terdapatPraktekLapangan;
	}

	public Boolean getAdaSap() {
		if (adaSap == null) {
			adaSap = false;
		}
		return adaSap;
	}

	public void setAdaSap(Boolean adaSap) {
		this.adaSap = adaSap;
	}

	public Boolean getAdaSilabus() {
		if (adaSilabus == null) {
			adaSilabus = false;
		}
		return adaSilabus;
	}

	public void setAdaSilabus(Boolean adaSilabus) {
		this.adaSilabus = adaSilabus;
	}

	public Boolean getAdaBahanAjar() {
		if (adaBahanAjar == null) {
			adaBahanAjar = false;
		}
		return adaBahanAjar;
	}

	public void setAdaBahanAjar(Boolean adaBahanAjar) {
		this.adaBahanAjar = adaBahanAjar;
	}

	public Boolean getAdaAcaraPraktek() {
		if (adaAcaraPraktek == null) {
			adaAcaraPraktek = false;
		}
		return adaAcaraPraktek;
	}

	public void setAdaAcaraPraktek(Boolean adaAcaraPraktek) {
		this.adaAcaraPraktek = adaAcaraPraktek;
	}

	public Boolean getAdaDiktat() {
		if (adaDiktat == null) {
			adaDiktat = false;
		}
		return adaDiktat;
	}

	public void setAdaDiktat(Boolean adaDiktat) {
		this.adaDiktat = adaDiktat;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalSampai() {
		return tanggalSampai;
	}

	public void setTanggalSampai(Date tanggalSampai) {
		this.tanggalSampai = tanggalSampai;
	}

	public String getMetodeKuliah() {
		return metodeKuliah;
	}

	public void setMetodeKuliah(String metodeKuliah) {
		this.metodeKuliah = metodeKuliah;
	}

	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	@Column(name = "terdapat_uts")
	public Boolean getTerdapatUts() {
		if (terdapatUts == null) {
			terdapatUts = true;
		}
		return terdapatUts;
	}

	public void setTerdapatUts(Boolean terdapatUts) {
		this.terdapatUts = terdapatUts;
	}

	@Column(name = "terdapat_uas")
	public Boolean getTerdapatUas() {
		if (terdapatUas == null) {
			terdapatUas = true;
		}
		return terdapatUas;
	}

	public void setTerdapatUas(Boolean terdapatUas) {
		this.terdapatUas = terdapatUas;
	}

	@Column(columnDefinition = "text")
	public String getFeeders() {
		if (feeders == null) {
			feeders = "";
		}
		return feeders;
	}

	public void setFeeders(String feeders) {
		this.feeders = feeders;
	}

	@Column(columnDefinition = "text")
	public String getDeskripsiPembelajaran() {
		return deskripsiPembelajaran == null ? "" : deskripsiPembelajaran.trim();
	}

	public void setDeskripsiPembelajaran(String deskripsiPembelajaran) {
		this.deskripsiPembelajaran = deskripsiPembelajaran;
	}

	@Column(columnDefinition = "text")
	public String getCapaianPembelajaranProdi() {
		return capaianPembelajaranProdi == null ? "" : capaianPembelajaranProdi.trim();
	}

	public void setCapaianPembelajaranProdi(String capaianPembelajaranProdi) {
		this.capaianPembelajaranProdi = capaianPembelajaranProdi;
	}

	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	public Boolean getMerupakanModul() {
		return merupakanModul == null ? false : merupakanModul;
	}

	public void setMerupakanModul(Boolean merupakanModul) {
		this.merupakanModul = merupakanModul;
	}

	public Double getSksSubMk() {
		return sksSubMk == null ? 0.0 : sksSubMk;
	}

	public void setSksSubMk(Double sksSubMk) {
		this.sksSubMk = sksSubMk;
	}

	public Boolean getMerupakanPraPerkuliahan() {
		return merupakanPraPerkuliahan == null ? false : merupakanPraPerkuliahan;
	}

	public void setMerupakanPraPerkuliahan(Boolean merupakanPraPerkuliahan) {
		this.merupakanPraPerkuliahan = merupakanPraPerkuliahan;
	}

	public Boolean getMerupakanPerkuliahanUmum() {
		return merupakanPerkuliahanUmum == null ? false : merupakanPerkuliahanUmum;
	}

	public void setMerupakanPerkuliahanUmum(Boolean merupakanPerkuliahanUmum) {
		this.merupakanPerkuliahanUmum = merupakanPerkuliahanUmum;
	}

	public String ambilLokasiEkivalen() {
		File file = Common.getFileLocation(this, "ekivalen_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Matakuliah.java:659");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiEkivalen(String data) {
		File file = Common.getFileLocation(this, "ekivalen_" + getId().toString());
		try {
//			System.out.println(this + ", Tulis file " + file + ", data " + data);
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Matakuliah.java:670");
		}
	}

	public List<MatakuliahEkivalen> reInitEkivalen() {
		List<MatakuliahEkivalen> matakuliahEkivalens = new ArrayList<MatakuliahEkivalen>();
		try {
			Session session = HibernateUtil.currentNativeSession();
			matakuliahEkivalens = ConstantValues.simpleList(session.createCriteria(MatakuliahEkivalen.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions
							.or(Restrictions.eq("matakuliah", this), Restrictions.eq("matakuliahEkivalen", this))),
					MatakuliahEkivalen.class);
			JSONObject c = new JSONObject();
			for (MatakuliahEkivalen matakuliahEkivalen : matakuliahEkivalens) {
				c.put(matakuliahEkivalen.getId().toString(), matakuliahEkivalen.getOrCreateFileLocation());

//				System.out.println("reInitEkivalen Mk " + matakuliahEkivalen.getMatakuliah() + " ekivalen dengan "
//						+ matakuliahEkivalen.getMatakuliahEkivalen() + ", data => " + matakuliahEkivalens);
			}
			tulisLokasiEkivalen(c.toString());
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
		} catch (Exception ee) {
			ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/database/model/Matakuliah.java:696");
		}
		HibernateUtil.closeSession();
		return matakuliahEkivalens;
	}

	@SuppressWarnings("unchecked")
	public List<MatakuliahEkivalen> ambilEkivalen(String nim) {
		List<MatakuliahEkivalen> matakuliahEkivalens = new ArrayList<MatakuliahEkivalen>();
		List<Long> idsBelumAda = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiEkivalen());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(MatakuliahEkivalen.class, key);
						if (generalValueObject != null) {
							MatakuliahEkivalen matakuliahEkivalen = (MatakuliahEkivalen) generalValueObject;
							// reInitEkivalen() mencari record di MANA PUN "this" muncul (sisi matakuliah
							// ATAU matakuliahEkivalen, utk keperluan cache-invalidation), tapi resolusi di
							// bawah SELALU mengasumsikan "this" ada di sisi matakuliah (sumber) dan
							// mengambil matakuliahEkivalen (target). Tanpa filter arah ini, matakuliah yang
							// jadi TARGET satu mapping (mis. RPL211401) bisa salah ter-resolve ke dirinya
							// sendiri atau ke mapping lain saat ia jg SUMBER mapping berbeda.
							if (matakuliahEkivalen.getMatakuliah() != null
									&& matakuliahEkivalen.getMatakuliah().getId() != null
									&& this.getId() != null
									&& this.getId().equals(matakuliahEkivalen.getMatakuliah().getId())
									&& matakuliahEkivalen.getMatakuliahEkivalen() != null
									&& matakuliahEkivalen.getAktif()) {

								if (nim != null && !nim.trim().isEmpty()
										&& matakuliahEkivalen.getKhususUntukNim() != null
										&& !matakuliahEkivalen.getKhususUntukNim().trim().isEmpty()) {
									if (matakuliahEkivalen.getKhususUntukNim().contains("," + nim.trim() + ",")) {
										matakuliahEkivalens.add(matakuliahEkivalen);
									}
								} else {
									matakuliahEkivalens.add(matakuliahEkivalen);
								}

								System.out.println("Mk " + matakuliahEkivalen.getMatakuliah() + " ekivalen dengan "
										+ matakuliahEkivalen.getMatakuliahEkivalen() + ", data => "
										+ matakuliahEkivalens);

							}
						} else {
							idsBelumAda.add(Long.parseLong(key));
						}

					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Matakuliah.java:752");
				}
			}

			if (!idsBelumAda.isEmpty()) {
				//System.out.println("idsBelumAda MatakuliahEkivalen -> " + idsBelumAda);
				Session session = HibernateUtil.currentNativeSession();
				List<MatakuliahEkivalen> matakuliahEkivalensTemp = session.createCriteria(MatakuliahEkivalen.class)
						.add(Restrictions.in("id", idsBelumAda)).list();
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				HibernateUtil.closeSession();
				for (MatakuliahEkivalen matakuliahEkivalen : matakuliahEkivalensTemp) {
					masukkanData(MatakuliahEkivalen.class, matakuliahEkivalen);
					// Filter arah yang sama seperti loop di atas: "this" wajib di sisi matakuliah (sumber).
					if (matakuliahEkivalen.getMatakuliah() != null
							&& matakuliahEkivalen.getMatakuliah().getId() != null && this.getId() != null
							&& this.getId().equals(matakuliahEkivalen.getMatakuliah().getId())
							&& matakuliahEkivalen.getMatakuliahEkivalen() != null
							&& matakuliahEkivalen.getAktif()) {

						if (nim != null && !nim.trim().isEmpty() && matakuliahEkivalen.getKhususUntukNim() != null
								&& !matakuliahEkivalen.getKhususUntukNim().trim().isEmpty()) {
							if (matakuliahEkivalen.getKhususUntukNim().contains("," + nim.trim() + ",")) {
								matakuliahEkivalens.add(matakuliahEkivalen);
							}
						} else {
							matakuliahEkivalens.add(matakuliahEkivalen);
						}

					}
				}
				matakuliahEkivalensTemp = null;
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Matakuliah.java:791");
			matakuliahEkivalens = reInitEkivalen();

		}
		return matakuliahEkivalens;
	}

	public void populateEkivalen(MatakuliahEkivalen matakuliahEkivalen) {
		try {
			JSONObject c = new JSONObject(ambilLokasiEkivalen());
			c.put(matakuliahEkivalen.getId().toString(), matakuliahEkivalen.getOrCreateFileLocation());
			tulisLokasiEkivalen(c.toString());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Matakuliah.java:804");
		}
	}

	public void removeEkivalen(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiEkivalen());
			c.put(id.toString(), "");
			tulisLokasiEkivalen(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Matakuliah.java:813");
		}
	}

	public Integer getJumlahMaksimalSksJikaAmbilMkIni() {
		return jumlahMaksimalSksJikaAmbilMkIni == null ? 30 : jumlahMaksimalSksJikaAmbilMkIni;
	}

	public void setJumlahMaksimalSksJikaAmbilMkIni(Integer jumlahMaksimalSksJikaAmbilMkIni) {
		this.jumlahMaksimalSksJikaAmbilMkIni = jumlahMaksimalSksJikaAmbilMkIni;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_matakuliah", nullable = true)
	public KelompokMatakuliah getKelompokMatakuliah() {
		kelompokMatakuliah = check(kelompokMatakuliah);
		return kelompokMatakuliah;
	}

	public void setKelompokMatakuliah(KelompokMatakuliah kelompokMatakuliah) {
		this.kelompokMatakuliah = kelompokMatakuliah;
	}

	@Column(name = "bahan_kajian", nullable = true, columnDefinition = "text")
	public String getBahanKajian() {

		bahanKajian = (bahanKajian == null || bahanKajian.trim().equalsIgnoreCase(",") ? ""
				: "," + bahanKajian.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (bahanKajian.equals(",")) {
			bahanKajian = "";
		} else if (bahanKajian.equals(",,")) {
			bahanKajian = "";
		} else if (bahanKajian.equals(",,,")) {
			bahanKajian = "";
		} else if (bahanKajian.equals(",,,,")) {
			bahanKajian = "";
		}
		if (bahanKajian != null && !bahanKajian.trim().isEmpty()) {
			Set<String> strings = new HashSet<String>(Arrays.asList(bahanKajian.split(",")));
			bahanKajian = "";
			for (String s : strings) {
				if (!s.trim().isEmpty()) {
					bahanKajian += bahanKajian.isEmpty() ? s : "," + s;
				}
			}
		}
		return bahanKajian == null ? "" : "," + bahanKajian.trim() + ",";
	}

	public void setBahanKajian(String bahanKajian) {
		this.bahanKajian = bahanKajian;
	}

	@Column(name = "capaian_lulusan", nullable = true, columnDefinition = "text")
	public String getCapaianLulusan() {

		capaianLulusan = (capaianLulusan == null || capaianLulusan.trim().equalsIgnoreCase(",") ? ""
				: "," + capaianLulusan.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (capaianLulusan.equals(",")) {
			capaianLulusan = "";
		} else if (capaianLulusan.equals(",,")) {
			capaianLulusan = "";
		} else if (capaianLulusan.equals(",,,")) {
			capaianLulusan = "";
		} else if (capaianLulusan.equals(",,,,")) {
			capaianLulusan = "";
		}

		if (capaianLulusan != null && !capaianLulusan.trim().isEmpty()) {
			Set<String> strings = new HashSet<String>(Arrays.asList(capaianLulusan.split(",")));
			capaianLulusan = "";
			for (String s : strings) {
				if (!s.trim().isEmpty()) {
					capaianLulusan += capaianLulusan.isEmpty() ? s : "," + s;
				}
			}
		}

		return capaianLulusan == null ? "" : "," + capaianLulusan.trim() + ",";
	}

	public void setCapaianLulusan(String capaianLulusan) {
		this.capaianLulusan = capaianLulusan;
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

	@Column(name = "capaian_pembelajaran_lulusan", nullable = true, columnDefinition = "text")
	public String getCapaianPembelajaranLulusan() {
		capaianPembelajaranLulusan = (capaianPembelajaranLulusan == null
				|| capaianPembelajaranLulusan.trim().equalsIgnoreCase(",") ? ""
						: "," + capaianPembelajaranLulusan.trim() + ",")
				.replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (capaianPembelajaranLulusan.equals(",")) {
			capaianPembelajaranLulusan = "";
		} else if (capaianPembelajaranLulusan.equals(",,")) {
			capaianPembelajaranLulusan = "";
		} else if (capaianPembelajaranLulusan.equals(",,,")) {
			capaianPembelajaranLulusan = "";
		} else if (capaianPembelajaranLulusan.equals(",,,,")) {
			capaianPembelajaranLulusan = "";
		}

		if (capaianPembelajaranLulusan != null && !capaianPembelajaranLulusan.trim().isEmpty()) {
			Set<String> strings = new HashSet<String>(Arrays.asList(capaianPembelajaranLulusan.split(",")));
			capaianPembelajaranLulusan = "";
			for (String s : strings) {
				if (!s.trim().isEmpty()) {
					capaianPembelajaranLulusan += capaianPembelajaranLulusan.isEmpty() ? s : "," + s;
				}
			}
		}

		return capaianPembelajaranLulusan == null ? "" : "," + capaianPembelajaranLulusan.trim() + ",";
	}

	public void setCapaianPembelajaranLulusan(String capaianPembelajaranLulusan) {
		this.capaianPembelajaranLulusan = capaianPembelajaranLulusan;
	}

	@Column(name = "profil_lulusan", nullable = true, columnDefinition = "text")
	public String getProfilLulusan() {
		profilLulusan = (profilLulusan == null || profilLulusan.trim().equalsIgnoreCase(",") ? ""
				: "," + profilLulusan.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (profilLulusan.equals(",")) {
			profilLulusan = "";
		} else if (profilLulusan.equals(",,")) {
			profilLulusan = "";
		} else if (profilLulusan.equals(",,,")) {
			profilLulusan = "";
		} else if (profilLulusan.equals(",,,,")) {
			profilLulusan = "";
		}

		if (profilLulusan != null && !profilLulusan.trim().isEmpty()) {
			Set<String> strings = new HashSet<String>(Arrays.asList(profilLulusan.split(",")));
			profilLulusan = "";
			for (String s : strings) {
				if (!s.trim().isEmpty()) {
					profilLulusan += profilLulusan.isEmpty() ? s : "," + s;
				}
			}
		}

		return profilLulusan == null ? "" : "," + profilLulusan.trim() + ",";
	}

	public void setProfilLulusan(String profilLulusan) {
		this.profilLulusan = profilLulusan;
	}
}
