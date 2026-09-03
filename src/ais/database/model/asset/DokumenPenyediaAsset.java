package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Master data <b>jenis dokumen legalitas penyedia asset</b> (tabel
 * {@code asset.dokumen_penyedia_asset}) — katalog <i>jenis berkas</i> yang diminta dari
 * vendor/penyedia barang dan jasa pengadaan aset tetap, misalnya NPWP, akta pendirian, rekening
 * bank, dan pakta integritas.
 *
 * <h2>Kedudukan dalam klaster vendor</h2>
 * <p>Entitas ini adalah sisi "definisi" dari sepasang tabel:</p>
 * <ul>
 *   <li><b>{@code DokumenPenyediaAsset}</b> (kelas ini) — <i>katalog jenis dokumen</i>. Satu baris
 *       berarti "berkas jenis ini kami minta dari vendor". Baris di sini tidak terikat pada vendor
 *       mana pun dan tidak memuat berkas apa pun;</li>
 *   <li>{@link PenyediaAssetPunyaDokumen} — <i>pemenuhan per vendor</i>. Satu baris menyatakan
 *       bahwa {@link PenyediaAsset} tertentu telah menyerahkan jenis dokumen tertentu, lengkap
 *       dengan status verifikasi dan tanggal akhir masa berlaku.</li>
 * </ul>
 * <p>Dengan kata lain, hubungan {@code PenyediaAsset} ↔ {@code DokumenPenyediaAsset} adalah
 * <b>many-to-many</b> yang diwujudkan lewat entitas penghubung {@link PenyediaAssetPunyaDokumen};
 * berkas fisiknya sendiri tidak disimpan di kedua tabel ini melainkan pada lampiran terpisah yang
 * dikelola layar {@code PenyediaAssetAction}. Rincian relasi dan keterbatasannya
 * didokumentasikan pada {@link PenyediaAssetPunyaDokumen}.</p>
 *
 * <h2>Flag {@code wajib} bersifat deskriptif — tidak ada gerbang kelengkapan dokumen</h2>
 * <p>Kelas ini memiliki flag {@link #getWajib()} yang membedakan dokumen wajib dari dokumen
 * opsional, dan {@link #reloadDefault()} menyemai enam jenis sebagai wajib. Pembaca wajar menduga
 * ada validasi yang menolak vendor tanpa dokumen wajib lengkap sebelum vendor itu boleh dipakai
 * dalam transaksi pengadaan. <b>Validasi seperti itu tidak ada.</b></p>
 * <p>Penelusuran seluruh pemanggil {@code getWajib()} menunjukkan flag ini hanya dibaca untuk
 * ditampilkan sebagai teks:</p>
 * <ul>
 *   <li>{@code PenyediaAssetAction} menuliskan label {@code "Wajib"}/{@code "Opsional"} pada
 *       tabel verifikasi dokumen vendor;</li>
 *   <li>{@code PenyediaAssetPunyaDokumenAction} menuliskan label {@code "Ya"}/{@code "Tidak"};</li>
 *   <li>{@code DokumenPenyediaAssetAction} memakainya untuk mengisi keadaan checkbox pada layar
 *       CRUD katalog ini sendiri;</li>
 *   <li>{@code DasboardVendor} memakainya dalam HQL statistik kepatuhan dokumen — analitik
 *       baca-saja, bukan penjagaan.</li>
 * </ul>
 * <p>Tidak ada satu pun titik pembuatan transaksi pengadaan — permintaan, pemesanan (PO),
 * penerimaan, perjanjian kerja sama, maupun ketiga jenis pembayaran — yang memeriksa kelengkapan
 * dokumen wajib sebelum mengizinkan vendor dipakai. Layar-layar tersebut memang membaca
 * {@link PenyediaAssetPunyaDokumen}, tetapi semata untuk <i>mencetak</i> keterangan dokumen ke
 * parameter laporan (mis. {@code parameters.put("dokumen." + nama, temp.getKeterangan())}); nilai
 * yang kosong atau baris yang tidak ada sama sekali tidak menghentikan proses.</p>
 * <p><b>Kontras yang menegaskan.</b> Flag bernama sama pada modul SOP, {@code DokumenAlurSop},
 * justru <i>ditegakkan</i>: {@code DisposisiAlurSopAction} memeriksa
 * {@code dokumenAlurSop.getAktif() && dokumenAlurSop.getWajib()} dan {@code DisposisiSopAction}
 * memeriksa {@code dokumenAlurSop.getWajib()} sebagai prasyarat langkah disposisi. Jadi pola
 * "dokumen wajib yang benar-benar menghalangi" memang tersedia di basis kode ini — modul vendor
 * aset saja yang belum memakainya. Bila kepatuhan dokumen legal vendor perlu ditegakkan, tempat
 * yang tepat adalah lapisan {@code Action} pengadaan (atau alur SOP pendaftaran vendor), bukan
 * asumsi bahwa flag di sini sudah menjaganya.</p>
 *
 * <h2>Peringatan semantik NULL pada kolom {@code aktif}</h2>
 * <p>{@link #getAktif()} memperlakukan {@code null} sebagai {@code true} (pola baku AIS), dan
 * {@code DokumenPenyediaAssetAction} konsisten dengan itu ketika menyaring daftar
 * ({@code Restrictions.or(isNull("aktif"), eq("aktif", true))}). Namun {@link #reloadDefault()}
 * <b>tidak pernah</b> memanggil {@code setAktif(...)}, sehingga seluruh baris bawaan tersimpan
 * dengan kolom {@code aktif} bernilai NULL di basis data — walau di layar tampak tercentang
 * "Aktif" karena checkbox-nya diisi dari {@link #getAktif()}. Kode yang menyaring dengan
 * perbandingan langsung {@code aktif = true} pada tingkat HQL/SQL karena itu akan membuang seluruh
 * baris bawaan tanpa peringatan. Setiap query baru atas tabel ini harus memakai bentuk toleran
 * NULL, sebagaimana dilakukan layar CRUD-nya.</p>
 *
 * <h2>Struktur</h2>
 * <p>Kelas mengikuti cetakan master data ringan AIS dengan tambahan {@link #getWajib()} dan
 * {@link #getNomorUrut()}: kunci utama {@code IDENTITY}, trio
 * {@code kode}/{@code nama}/{@code keterangan}, flag {@code aktif}, serta bidang audit bayangan
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} yang wajib ada agar
 * {@code AuditTimestampInterceptor} dapat mengisinya lewat {@link #onUpdate()}. Entitas dianotasi
 * {@link Audited} sehingga riwayat perubahannya terekam Envers, serta memakai
 * {@code dynamicInsert}/{@code dynamicUpdate}.</p>
 *
 * @see PenyediaAssetPunyaDokumen
 * @see PenyediaAsset
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "dokumen_penyedia_asset")
public class DokumenPenyediaAsset extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilai warisan cetakan hbm2java yang dipakai bersama hampir seluruh entitas master data
	 * paket ini. Kesamaan nilai antar kelas tidak berbahaya karena verifikasi dilakukan per kelas,
	 * namun jangan mengubahnya tanpa alasan sebab objek entitas dapat ikut diserialisasi bersama
	 * sesi/desktop ZK.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan. Bidang audit bayangan hanya boleh diisi
	 * {@code AuditTimestampInterceptor}, dan penulisan kosong dari jalur salin/klon objek tidak
	 * boleh menghapus jejak audit yang sudah ada.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan agar jejak
	 * audit tidak terhapus.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui.
	 *
	 * <p>Interceptor-lah yang mengisi {@link #oleh}, {@link #olehId}, dan
	 * {@link #getTanggal_dirubah()} dari konteks pengguna aktif. Method sengaja {@code protected}
	 * dan tidak boleh dipanggil manual dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat dan diperbarui interceptor audit. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil {@code AuditTimestampInterceptor}, bukan oleh form.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berformat {@code "<id>-<nama>"}.
	 *
	 * <p>Dipakai untuk penelusuran log dan sebagai label bawaan sejumlah komponen ZK. Method ini
	 * membaca bidang {@link #nama} langsung, bukan lewat {@link #getNama()}, sehingga tidak
	 * melakukan {@code trim()}.</p>
	 *
	 * @return gabungan id dan nama; bagian id bernilai {@code "null"} bila baris belum tersimpan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Menyemai katalog jenis dokumen legalitas penyedia asset dengan tujuh baris bawaan bila tabel
	 * {@code asset.dokumen_penyedia_asset} masih benar-benar kosong.
	 *
	 * <p><b>Kapan dipanggil.</b> Method ini bagian dari rangkaian penyemaian master data yang
	 * dijalankan {@code InitData} pada saat aplikasi dimulai. Berbeda dari saudara-saudaranya
	 * ({@link JenisPenyediaAsset#reloadDefault()},
	 * {@link KategoriPenyediaAsset#reloadDefault()},
	 * {@link StatusPenyediaAsset#reloadDefault()}), method ini tidak menyimpan hasilnya ke
	 * konstanta statis mana pun — katalog dokumen selalu dibaca ulang dari basis data atau dari
	 * cache {@code ConstantValues} oleh pemakainya.</p>
	 *
	 * <p><b>Penjaga luar: semua-atau-tidak-sama-sekali.</b> Hal terpenting yang perlu dipahami dari
	 * method ini adalah penjaga di baris pertamanya. Ia mengambil <i>satu baris sembarang</i> dari
	 * tabel ({@code createCriteria(...).setMaxResults(1).uniqueResult()}) tanpa syarat apa pun,
	 * lalu seluruh blok penyemaian hanya dijalankan bila hasilnya {@code null}. Artinya:</p>
	 * <ul>
	 *   <li>Bila tabel benar-benar kosong, ketujuh baris bawaan dibuat;</li>
	 *   <li>Bila tabel sudah berisi <b>satu baris apa pun</b> — termasuk satu jenis dokumen yang
	 *       ditambahkan operator sebelum penyemaian pertama sempat berjalan — maka
	 *       <b>seluruh</b> baris bawaan dilewati dan tidak pernah dibuat. Instalasi seperti itu
	 *       akan berjalan tanpa NPWP, akta pendirian, pakta integritas, dan seterusnya di
	 *       katalognya, tanpa peringatan apa pun;</li>
	 *   <li>Karena penjaga yang sama, method ini <b>tidak dapat dipakai untuk menambal</b> katalog
	 *       yang sudah berjalan. Menambahkan jenis dokumen baru ke dalam daftar di kode ini tidak
	 *       akan berpengaruh pada instalasi mana pun yang tabelnya sudah terisi; penambahan harus
	 *       dilakukan lewat layar CRUD {@code DokumenPenyediaAssetAction} atau lewat skrip migrasi
	 *       tersendiri.</li>
	 * </ul>
	 * <p>Perhatikan bahwa pemeriksaan cari-per-nama di dalam kedua perulangan
	 * ({@code Restrictions.ilike("nama", ss, MatchMode.EXACT)}) sebenarnya sudah membuat blok itu
	 * idempoten per baris; penjaga luar itulah yang membuat idempotensi tersebut mubazir dan justru
	 * menimbulkan perilaku semua-atau-tidak-sama-sekali di atas.</p>
	 *
	 * <p><b>Isi yang disemai.</b> Perulangan pertama membuat enam jenis dokumen dengan
	 * {@code wajib = true}: "Identitas Perusahaan", "Rekening Bank", "Akta Pendirian Perusahaan",
	 * "NPWP", "Pakta Integritas", dan "Surat Pernyataan Kebenerana Dokumen". Perulangan kedua
	 * membuat satu jenis dengan {@code wajib = false}: "Akta Perubahan Terkahir Perusahaan".
	 * Masing-masing memperoleh {@code kode} berformat {@code "00" + urut}, {@code nomorUrut}
	 * berurutan, dan {@code keterangan} yang disalin dari namanya. Setiap baris disimpan dalam
	 * transaksi tersendiri ({@code begin()}/{@code save()}/{@code commit()}), sehingga penyemaian
	 * dapat berhenti separuh jalan bila terjadi galat — dan karena penjaga luar, sisa baris yang
	 * belum terbuat <b>tidak</b> akan dilengkapi pada pemanggilan berikutnya.</p>
	 *
	 * <p><b>Dua salah ketik pada nama bawaan.</b> Teks yang disemai memuat dua kesalahan ejaan yang
	 * sudah terlanjur menjadi data produksi: {@code "Surat Pernyataan Kebenerana Dokumen"}
	 * (semestinya "Kebenaran") dan {@code "Akta Perubahan Terkahir Perusahaan"} (semestinya
	 * "Terakhir"). Jangan memperbaiki ejaannya di kode ini saja: pencocokan memakai
	 * {@code MatchMode.EXACT}, sehingga kode yang diperbaiki tidak akan lagi mengenali baris lama
	 * dan — pada tabel yang kebetulan kosong — akan menyemai baris dengan nama baru sementara
	 * instalasi lain tetap memakai nama lama. Perbaikan ejaan harus disertai migrasi data yang
	 * memperbarui baris yang sudah ada.</p>
	 *
	 * <p><b>Kolom {@code aktif} dibiarkan NULL.</b> Method ini menyetel {@code wajib},
	 * {@code nama}, {@code kode}, {@code nomorUrut}, dan {@code keterangan}, tetapi tidak pernah
	 * memanggil {@code setAktif(...)}. Seluruh baris bawaan karena itu tersimpan dengan kolom
	 * {@code aktif} bernilai NULL. Di layar hal ini tidak terlihat — checkbox "Aktif" diisi dari
	 * {@link #getAktif()} yang memetakan {@code null} menjadi {@code true} — tetapi kode yang
	 * menyaring dengan perbandingan langsung {@code aktif = true} pada tingkat HQL/SQL akan
	 * membuang seluruh baris bawaan tanpa peringatan, karena perbandingan terhadap NULL
	 * menghasilkan UNKNOWN. Query baru atas tabel ini wajib memakai bentuk toleran NULL seperti
	 * yang dipakai {@code DokumenPenyediaAssetAction}.</p>
	 *
	 * <p><b>Penanganan galat.</b> Seluruh badan method dibungkus {@code try/catch} yang menelan
	 * setiap {@code Exception}, mencetak jejak tumpukan, dan mencatatkannya ke
	 * {@code ErrorAuditUtil}; method tidak pernah melempar ke pemanggil. Ini disengaja agar
	 * kegagalan penyemaian satu master data tidak menggagalkan inisialisasi aplikasi, tetapi
	 * berarti katalog dokumen yang gagal disemai hanya terdeteksi lewat log. Tidak ada
	 * {@code rollback()} eksplisit; pembersihan diserahkan ke {@code HibernateUtil.closeSession()}
	 * pada blok {@code finally}, yang selalu dijalankan.</p>
	 *
	 * @see #getWajib()
	 * @see PenyediaAssetPunyaDokumen
	 */
	public static void reloadDefault() {
		Session session = HibernateUtil.currentNativeSession();
		try {

			int urut = 1;
			DokumenPenyediaAsset DATA = (DokumenPenyediaAsset) session.createCriteria(DokumenPenyediaAsset.class)

					.setMaxResults(1).uniqueResult();

			if (DATA == null) {
				String[] data = new String[] { "Identitas Perusahaan", "Rekening Bank", "Akta Pendirian Perusahaan",
						"NPWP", "Pakta Integritas", "Surat Pernyataan Kebenerana Dokumen" };
				for (String ss : data) {
					DATA = (DokumenPenyediaAsset) session.createCriteria(DokumenPenyediaAsset.class)
							.add(Restrictions.ilike("nama", ss, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (DATA == null) {
						DATA = new DokumenPenyediaAsset();
						DATA.setWajib(true);
						DATA.setNama(ss);
						DATA.setKode("00" + urut);
						DATA.setNomorUrut(urut);
						DATA.setKeterangan(ss);
						session.getTransaction().begin();
						session.save(DATA);
						session.getTransaction().commit();
						urut++;
					}
				}

				data = new String[] { "Akta Perubahan Terkahir Perusahaan" };
				for (String ss : data) {
					DATA = (DokumenPenyediaAsset) session.createCriteria(DokumenPenyediaAsset.class)
							.add(Restrictions.ilike("nama", ss, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (DATA == null) {
						DATA = new DokumenPenyediaAsset();
						DATA.setWajib(false);
						DATA.setNama(ss);
						DATA.setNomorUrut(urut);
						DATA.setKode("00" + urut);
						DATA.setKeterangan(ss);
						session.getTransaction().begin();
						session.save(DATA);
						session.getTransaction().commit();
						urut++;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/asset/DokumenPenyediaAsset.java:125");
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/** Kode ringkas jenis dokumen; baris bawaan disemai berformat {@code "00" + nomor urut}. */
	private String kode;

	/** Nama jenis dokumen (mis. "NPWP"); menjadi kunci pencocokan {@code EXACT} pada {@link #reloadDefault()}. */
	private String nama;
	/** Keterangan bebas; disemai sama dengan {@link #nama} untuk baris bawaan. */
	private String keterangan;
	/** Flag aktif katalog; dibiarkan NULL oleh {@link #reloadDefault()} — lihat peringatan pada {@link #getAktif()}. */
	private Boolean aktif;
	/** Penanda dokumen wajib vs opsional; deskriptif saja, lihat {@link #getWajib()}. */
	private Boolean wajib;
	/** Nomor urut tampilan pada tabel verifikasi dokumen vendor. */
	private Integer nomorUrut;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate.
	 *
	 * <p>Dipakai pula oleh {@link #reloadDefault()} dan oleh form CRUD
	 * {@code DokumenPenyediaAssetAction} saat membuat baris baru.</p>
	 */
	public DokumenPenyediaAsset() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return id baris, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini.
	 *
	 * <p>Hanya untuk kebutuhan Hibernate dan penyalinan objek; jangan menyetel id secara manual
	 * pada baris yang akan disimpan sebagai data baru.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode jenis dokumen dalam bentuk yang aman untuk ditampilkan.
	 *
	 * <p>Mengganti {@code null} dengan string kosong dan memangkas spasi tepi sehingga pemanggil
	 * UI tidak perlu memeriksa {@code null}.</p>
	 *
	 * @return kode yang sudah dipangkas, atau string kosong bila belum diisi
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode jenis dokumen.
	 *
	 * <p>Tidak ada penjaminan keunikan pada tingkat entitas maupun basis data. Perhatikan pula
	 * bahwa {@link #reloadDefault()} membangkitkan kode dari penghitung {@code urut} yang hanya
	 * bertambah ketika sebuah baris benar-benar dibuat, sehingga kode bawaan tidak dijamin
	 * berurutan rapat bila penyemaian pernah berjalan sebagian.</p>
	 *
	 * @param kode kode ringkas; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis dokumen setelah dipangkas spasi tepinya.
	 *
	 * <p>Berbeda dari {@link #getKode()}, method ini <b>mempertahankan</b> {@code null} apa adanya.
	 * Pemanggil UI tetap perlu berjaga terhadap {@code null} walau kolomnya dideklarasikan
	 * {@code nullable = false} pada tingkat basis data.</p>
	 * <p>Nilai inilah yang dipakai layar-layar pengadaan sebagai kunci parameter laporan
	 * ({@code "dokumen." + getNama()}), sehingga mengganti nama jenis dokumen akan mengubah nama
	 * parameter yang diharapkan berkas template laporan.</p>
	 *
	 * @return nama jenis dokumen yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis dokumen.
	 *
	 * <p>{@link #reloadDefault()} mencocokkan baris bawaan berdasarkan nama dengan
	 * {@code MatchMode.EXACT}, dan layar pengadaan memakai nama ini sebagai kunci parameter
	 * laporan. Mengganti nama baris bawaan karena itu berdampak ganda: penyemaian tidak lagi
	 * mengenali baris tersebut, dan parameter laporan yang bersangkutan berubah nama.</p>
	 *
	 * @param nama nama jenis dokumen
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk jenis dokumen ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi (tidak dinormalkan menjadi string
	 *         kosong)
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas untuk jenis dokumen ini.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan flag aktif katalog ini, dengan {@code null} diperlakukan sebagai {@code true}.
	 *
	 * <p><b>Peringatan penting mengenai NULL.</b> Getter ini memetakan {@code null} menjadi
	 * {@code true}, tetapi pemetaan itu hanya berlaku di lapisan Java. Seluruh baris yang disemai
	 * {@link #reloadDefault()} tersimpan dengan kolom {@code aktif} bernilai NULL di basis data
	 * karena penyemaian tidak pernah memanggil {@link #setAktif(Boolean)}. Akibatnya:</p>
	 * <ul>
	 *   <li>Di layar, dokumen bawaan tampak <b>tercentang "Aktif"</b> — checkbox pada
	 *       {@code DokumenPenyediaAssetAction} diisi dari getter ini, dan hanya menulis ke basis
	 *       data ketika pengguna benar-benar mengubahnya. Baris bisa bertahan bernilai NULL selama
	 *       bertahun-tahun tanpa ada yang menyadarinya;</li>
	 *   <li>Query yang menyaring dengan perbandingan langsung {@code aktif = true} pada tingkat
	 *       HQL/SQL akan <b>membuang</b> seluruh baris tersebut, karena perbandingan terhadap NULL
	 *       menghasilkan UNKNOWN, bukan {@code false} maupun {@code true}.</li>
	 * </ul>
	 * <p>Idiom yang benar untuk tabel ini adalah bentuk toleran NULL seperti yang dipakai layar
	 * CRUD-nya: {@code Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif",
	 * true))}, atau padanan HQL {@code (aktif is null or aktif = true)}.</p>
	 * <p><b>Penegakan.</b> Menonaktifkan sebuah jenis dokumen tidak menghapus atau membatalkan
	 * baris {@link PenyediaAssetPunyaDokumen} yang sudah menunjuknya; berkas yang terlanjur
	 * diserahkan vendor tetap tercatat dan tetap tercetak pada parameter laporan pengadaan.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} bila dinonaktifkan
	 *         secara eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel flag aktif katalog ini.
	 *
	 * <p>Menyetel {@code null} mengembalikan perilaku bawaan {@link #getAktif()} menjadi
	 * {@code true} di lapisan Java, namun menyimpan NULL ke basis data dengan segala akibatnya
	 * yang diuraikan pada {@link #getAktif()}. Untuk baris baru, sebaiknya setel {@code true}
	 * secara eksplisit agar kolomnya terisi.</p>
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk menonaktifkan,
	 *              {@code null} untuk mengembalikan ke bawaan (dianggap aktif di Java, tersimpan
	 *              NULL di basis data)
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penanda apakah jenis dokumen ini <i>wajib</i> diserahkan vendor, dengan
	 * {@code null} diperlakukan sebagai {@code false}.
	 *
	 * <p><b>Default menutup, kebalikan dari {@link #getAktif()}.</b> Perhatikan asimetri ini:
	 * {@code aktif} yang belum disetel dianggap {@code true}, sedangkan {@code wajib} yang belum
	 * disetel dianggap {@code false}. Pilihan tersebut konservatif — jenis dokumen baru tidak
	 * mendadak menjadi wajib — namun juga berarti jenis dokumen hasil migrasi lama tidak akan
	 * pernah terhitung sebagai wajib sampai seseorang menyetelnya secara eksplisit.</p>
	 *
	 * <p><b>Flag ini tidak menghalangi apa pun.</b> Sebagaimana diuraikan pada Javadoc kelas,
	 * seluruh pemanggil {@code getWajib()} hanya menampilkannya sebagai teks
	 * ({@code "Wajib"}/{@code "Opsional"} di {@code PenyediaAssetAction},
	 * {@code "Ya"}/{@code "Tidak"} di {@code PenyediaAssetPunyaDokumenAction}), mengisi checkbox
	 * CRUD-nya sendiri, atau menghitung statistik di {@code DasboardVendor}. Tidak ada proses
	 * pengadaan yang menolak vendor karena dokumen wajibnya belum lengkap, belum diverifikasi,
	 * atau sudah kedaluwarsa. Vendor yang belum menyerahkan satu pun berkas tetap dapat dipilih di
	 * {@code AmbilDataPenyediaAssetBanbox} dan tetap dapat dipakai pada PO, penerimaan, perjanjian
	 * kerja sama, serta pembayaran.</p>
	 * <p>Sebagai pembanding di dalam basis kode yang sama, flag {@code wajib} pada
	 * {@code DokumenAlurSop} <i>ditegakkan</i> oleh {@code DisposisiAlurSopAction} dan
	 * {@code DisposisiSopAction} sebagai prasyarat langkah disposisi. Mekanisme penegakan karena
	 * itu bukan hal yang mustahil di sini — ia hanya belum dipasang untuk dokumen vendor aset.</p>
	 *
	 * @return {@code true} bila jenis dokumen ditandai wajib; {@code false} bila opsional atau
	 *         belum pernah disetel
	 * @see PenyediaAssetPunyaDokumen#getStatus()
	 */
	public Boolean getWajib() {
		return wajib == null ? false : wajib;
	}

	/**
	 * Menyetel penanda wajib untuk jenis dokumen ini.
	 *
	 * <p>Menyetel {@code true} hanya mengubah label yang ditampilkan dan angka statistik dasbor;
	 * ia tidak memasang validasi apa pun pada alur pengadaan.</p>
	 *
	 * @param wajib {@code true} bila dokumen wajib, {@code false}/{@code null} bila opsional
	 */
	public void setWajib(Boolean wajib) {
		this.wajib = wajib;
	}

	/**
	 * Mengembalikan nomor urut tampilan jenis dokumen ini, dengan {@code null} dinormalkan menjadi
	 * {@code 0}.
	 *
	 * <p>Dipakai untuk mengatur urutan baris pada tabel verifikasi dokumen vendor. Karena nilai
	 * yang belum disetel jatuh ke {@code 0}, jenis dokumen baru yang nomor urutnya belum diisi akan
	 * berkumpul di awal daftar. Tidak ada penjaminan keunikan nomor urut, sehingga dua jenis
	 * dokumen boleh memiliki nomor yang sama dan urutan relatif keduanya tidak ditentukan.</p>
	 *
	 * @return nomor urut, atau {@code 0} bila belum diisi
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampilan jenis dokumen ini.
	 *
	 * @param nomorUrut nomor urut; {@code null} akan dibaca kembali sebagai {@code 0}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

}
