package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
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

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.A;
import org.zkoss.zul.Toolbarbutton;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Bank;
import ais.database.model.Kota;
import ais.database.model.Propinsi;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.database.model.akunting.Akun;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entitas master <b>penyedia asset</b> (tabel {@code asset.penyedia_asset}) — data induk
 * vendor/penyedia barang dan jasa yang dipakai seluruh alur pengadaan aset tetap: permintaan,
 * pemesanan (PO), penerimaan, perjanjian kerja sama, retur, hingga pembayaran DP, termin, dan
 * pelunasan.
 *
 * <p>Ini adalah entitas terbesar di paket {@code ais.database.model.asset} dan menjadi pusat
 * klaster vendor bersama {@link JenisPenyediaAsset}, {@link KategoriPenyediaAsset},
 * {@link StatusPenyediaAsset}, {@link JenisPekerjaanPenyedia}, serta pasangan katalog-pemenuhan
 * dokumen {@link DokumenPenyediaAsset}/{@link PenyediaAssetPunyaDokumen}. Padanannya di modul lain
 * adalah {@code koperasi.SupplierInventoryProfile} dan {@code inventory.Pedagang}.</p>
 *
 * <h2>Isi data</h2>
 * <p>Satu baris menghimpun beberapa kelompok informasi yang secara historis tumbuh bertahap:</p>
 * <ul>
 *   <li><b>Identitas dan kontak</b> — {@link #getKode()}, {@link #getNama()},
 *       {@link #getPemilik()}, alamat lengkap beserta {@link #getKota()},
 *       {@link #getPropinsi()}, {@link #getKecamatan()}, kode pos, telepon, faks, kontak
 *       personal, surel, serta koordinat {@link #getLongitude()}/{@link #getLatitude()};</li>
 *   <li><b>Klasifikasi</b> — jenis badan usaha, kategori peran rantai pasok, status
 *       pusat/cabang, dan hingga lima bidang {@link JenisPekerjaanPenyedia} yang dapat
 *       ditangani vendor;</li>
 *   <li><b>Legalitas</b> — dua set data akta yang sejajar: akta pendirian awal
 *       (nomor, tanggal, notaris, nomor dan tanggal pengesahan) serta akta perubahan terakhir
 *       dengan lima bidang yang sama, ditambah {@link #getNpwp()};</li>
 *   <li><b>Keuangan</b> — {@link #getNoRek()}, {@link #getAtasNama()},
 *       {@link #getBankUtama()}, daftar rekening tambahan dalam bentuk JSON pada
 *       {@link #getBank()}, dan {@link #getAkunUtang()} yang menautkan vendor ke bagan akun;</li>
 *   <li><b>Alur persetujuan</b> — kelas mewarisi {@code DataSop}, sehingga pendaftaran vendor
 *       melewati disposisi SOP; {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
 *       {@link #getTanggalPersetujuan()}, dan {@link #getAktif()} seluruhnya diturunkan dari
 *       disposisi tersebut.</li>
 * </ul>
 *
 * <h2>Gerbang pemakaian vendor</h2>
 * <p><b>{@link #getAktif()} adalah satu-satunya gerbang yang benar-benar ditegakkan.</b> Pemilih
 * vendor {@code AmbilDataPenyediaAssetBanbox} — komponen yang dipakai layar-layar pengadaan untuk
 * memilih vendor — menyaring dengan {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}
 * pada kedua jalurnya (pencarian daftar maupun penebusan langsung berdasarkan kode), sehingga
 * vendor yang nonaktif atau yang disposisinya berakhir pada langkah penolakan tidak muncul dan
 * tidak dapat dipilih untuk transaksi baru.</p>
 * <p>Perlu ditegaskan apa yang <b>tidak</b> menjadi gerbang, karena ketiganya mudah disalahpahami
 * sebagai pengaman:</p>
 * <ul>
 *   <li>{@link #getStatusPenyediaAsset()} <b>bukan</b> status kelayakan/blacklist. Nilainya hanya
 *       "Pusat" dan "Cabang", editornya sudah dikomentari di {@code PenyediaAssetAction}, dan
 *       tidak ada proses transaksi yang membacanya. Menambah baris status bernama "Blacklist"
 *       tidak akan menghalangi apa pun — cara yang benar untuk memblokir vendor adalah
 *       menonaktifkan flag {@code aktif}-nya atau menolaknya lewat SOP;</li>
 *   <li>kelengkapan dan masa berlaku <b>dokumen legal tidak divalidasi</b> sebelum vendor dipakai.
 *       Layar-layar pengadaan membaca {@link PenyediaAssetPunyaDokumen} semata untuk mencetak
 *       keterangannya ke parameter laporan; vendor tanpa NPWP, akta, atau pakta integritas tetap
 *       dapat dipakai pada PO, penerimaan, perjanjian, dan pembayaran;</li>
 *   <li>flag {@code aktif} pada ketiga tabel klasifikasi tidak berpengaruh pada vendor yang sudah
 *       menunjuknya.</li>
 * </ul>
 * <p><b>Evaluasi/rating vendor tidak disimpan pada entitas ini.</b> Tidak ada kolom skor kinerja,
 * peringkat, atau catatan sanksi. Penilaian kinerja penyedia hanya ada sebagai <i>analitik
 * baca-saja</i> di luar entitas: {@code DasboardAnalisisVendor} menghitung nominal transaksi,
 * kuantitas dipesan/diterima, kepatuhan dokumen, serta skor kualitas/ketepatan waktu/layanan yang
 * dibaca dari objek BAST secara refleksi (dengan daftar nama method alternatif dan
 * <i>fallback</i> konservatif bila field penilaian belum tersedia), sedangkan
 * {@code DasboardVendor} menyajikan statistik populasi vendor. Angka-angka itu tidak pernah
 * ditulis kembali ke sini dan tidak memengaruhi kelayakan vendor pada transaksi berikutnya.</p>
 *
 * <h2>Cakupan data: global, tanpa penyaringan satuan kerja</h2>
 * <p>Entitas ini <b>tidak memiliki</b> kolom satuan kerja, unit, maupun penanda tenant lainnya.
 * Konsekuensinya seluruh vendor bersifat global: setiap pengguna yang berhak membuka layar
 * pengadaan dapat melihat dan memilih vendor mana pun, dan statistik dasbor menjumlahkan seluruh
 * populasi tanpa pemisahan unit. Ini tampaknya memang disengaja untuk master data vendor, namun
 * perlu disadari saat menambahkan laporan atau pembatasan akses baru — tidak ada kolom yang bisa
 * dipakai untuk menyaring per unit tanpa perubahan skema.</p>
 *
 * <h2>Pola yang perlu diwaspadai saat menyunting kelas ini</h2>
 * <p><b>Getter yang memutasi state.</b> Sebagian besar getter relasi di kelas ini tidak bebas efek
 * samping: ia menulis kembali hasil resolusi atau nilai bawaan ke bidang instansnya. Yang paling
 * berdampak adalah {@link #getAktif()}, {@link #getPropinsi()}, {@link #getKecamatan()},
 * {@link #getJenisPenyediaAsset()}, {@link #getKategoriPenyediaAsset()},
 * {@link #getStatusPenyediaAsset()}, {@link #getAtasNama()}, {@link #getAkunUtang()},
 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
 * {@link #getTahun()}, dan {@link #getBulan()}. Karena objek yang terpasang pada sesi Hibernate
 * akan ter-<i>flush</i> otomatis, nilai yang "hanya dibaca" oleh laporan atau dasbor dapat ikut
 * tertulis ke basis data. Jangan menambah efek samping baru pada getter, dan berhati-hatilah
 * memanggil getter kelas ini dari kode baca-saja yang berjalan di dalam transaksi.</p>
 * <p><b>Bidang audit bayangan.</b> {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah}
 * beserta {@link #onUpdate()} adalah keharusan teknis agar {@code AuditTimestampInterceptor}
 * dapat bekerja, bukan duplikasi yang bisa dihapus. Setternya sengaja mengabaikan masukan
 * kosong.</p>
 * <p><b>Metode tampilan di dalam entitas.</b> {@link #tampilkanEmail(Component)},
 * {@link #tampilkanHp(Component)}, {@link #putPhoto(Map)}, dan
 * {@link #reloadGaleries(PenyediaAsset)} membangun komponen ZK atau membuka sesi Hibernate sendiri
 * dari dalam kelas model. Ini melanggar pemisahan lapisan namun sudah terlanjur dipakai luas;
 * perlakukan sebagai utilitas warisan dan jangan menambah yang baru.</p>
 *
 * @see PenyediaAssetPunyaDokumen
 * @see DokumenPenyediaAsset
 * @see JenisPenyediaAsset
 * @see KategoriPenyediaAsset
 * @see StatusPenyediaAsset
 * @see DataSop
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "penyedia_asset")
public class PenyediaAsset extends DataSop {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilai warisan cetakan hbm2java yang dipakai bersama hampir seluruh entitas paket ini.
	 * Kesamaan nilai antar kelas tidak berbahaya karena verifikasi dilakukan per kelas, namun
	 * jangan mengubahnya tanpa alasan sebab objek vendor dapat ikut diserialisasi bersama
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
	 * Mengembalikan id pengguna yang terakhir mengubah data vendor ini.
	 *
	 * <p>Berbeda dari {@link #getDibuatOleh()} dan {@link #getDisetujuiOleh()} yang diturunkan dari
	 * disposisi SOP, bidang ini mencatat penyuntingan terakhir apa pun — termasuk perubahan yang
	 * tidak melewati alur persetujuan.</p>
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah data vendor ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan. Bidang audit bayangan hanya boleh diisi
	 * {@code AuditTimestampInterceptor}, dan penulisan kosong dari jalur salin/klon objek tidak
	 * boleh menghapus jejak audit yang sudah ada.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah data vendor ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan agar jejak
	 * audit tidak terhapus.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah data vendor ini.
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
	 * <p>Perhatikan bahwa kait ini dijalankan pada <i>setiap</i> pembaruan baris, termasuk
	 * pembaruan yang dipicu tanpa sengaja oleh getter yang memutasi state (lihat Javadoc kelas).
	 * Jejak audit karena itu dapat menunjukkan perubahan yang tidak pernah dilakukan pengguna
	 * secara sadar.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat dan diperbarui interceptor audit. */
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
	 * Mengembalikan stempel waktu perubahan terakhir baris vendor ini.
	 *
	 * <p>Jangan mengacaukannya dengan {@link #getTanggalPembuatan()} (waktu pendaftaran) maupun
	 * {@link #getTanggalPersetujuan()} (waktu disposisi menyetujui).</p>
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
	 * <p>Dipakai untuk penelusuran log dan sebagai label bawaan sejumlah komponen ZK. Method
	 * membaca bidang {@link #nama} secara langsung, bukan lewat {@link #getNama()}, sehingga tidak
	 * melakukan {@code trim()} dan tidak memicu resolusi relasi malas apa pun — perilaku yang
	 * memang diinginkan agar {@code toString()} aman dipanggil di luar sesi Hibernate.</p>
	 *
	 * @return gabungan id dan nama; bagian id bernilai {@code "null"} bila baris belum tersimpan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Cache statik galeri foto vendor: id vendor &rarr; peta (tautan berkas &rarr; deskripsi).
	 *
	 * <p>Diisi {@link #reloadGaleries(PenyediaAsset)} dan dibaca lapisan tampilan untuk
	 * menampilkan galeri foto pada profil vendor tanpa perlu menembak basis data berulang kali.</p>
	 *
	 * <p><b>Peringatan.</b> Bidang ini {@code public static} dan bertipe {@link HashMap} biasa,
	 * dengan tiga akibat yang perlu disadari:</p>
	 * <ul>
	 *   <li><b>Dibagi seluruh pengguna.</b> Isinya bukan milik satu sesi atau satu desktop ZK
	 *       melainkan milik seluruh JVM, sehingga galeri yang dimuat satu pengguna terlihat oleh
	 *       semua pengguna lain. Untuk foto vendor hal ini tidak sensitif, tetapi jangan menitipkan
	 *       data yang bergantung pada hak akses ke peta ini;</li>
	 *   <li><b>Tidak pernah dibersihkan.</b> Tidak ada mekanisme kedaluwarsa maupun pembatasan
	 *       ukuran; entri hanya bertambah seiring vendor yang pernah dibuka dan bertahan sampai
	 *       aplikasi dijalankan ulang. Pada instalasi dengan vendor sangat banyak, ini adalah
	 *       pertumbuhan memori yang tidak berbatas;</li>
	 *   <li><b>Tidak aman untuk banyak thread.</b> {@code HashMap} yang ditulis bersamaan dari
	 *       beberapa permintaan dapat rusak strukturnya. {@link #reloadGaleries(PenyediaAsset)}
	 *       melakukan {@code put} pada peta luar dan {@code clear}/{@code put} pada peta dalam
	 *       tanpa penguncian apa pun, sehingga dua pengguna yang membuka vendor yang sama pada saat
	 *       bersamaan dapat saling mengosongkan hasil.</li>
	 * </ul>
	 * <p>Jangan mengubah bidang ini menjadi sumber kebenaran; ia semata cache tampilan yang boleh
	 * hilang kapan saja.</p>
	 */
	public static Map<Long, Map<String, String>> galeries = new HashMap<Long, Map<String, String>>();

	/**
	 * Memuat ulang isi cache {@link #galeries} untuk satu vendor dari lampiran yang tersimpan.
	 *
	 * <p><b>Cara kerja.</b> Method membuka sesi <i>streaming</i> tersendiri lewat
	 * {@code StreamingHibernateUtil} — bukan sesi permintaan yang sedang berjalan — lalu mengambil
	 * seluruh {@link LampiranLain} milik vendor dengan dua syarat: {@code ref} sama dengan id
	 * vendor, dan {@code jenis} diawali {@code "Galery_PenyediaAsset_"}
	 * ({@code MatchMode.START}), diurutkan menaik menurut id. Untuk setiap lampiran, tautan
	 * berkasnya dihitung {@code FileFotoLain.ambilLinkLampiranLain(...)} dan dipetakan ke
	 * deskripsi lampiran. Peta dalam untuk vendor bersangkutan dibuat bila belum ada, lalu
	 * dikosongkan sebelum diisi ulang sehingga hasilnya mencerminkan keadaan terkini, bukan
	 * gabungan dengan pemuatan sebelumnya.</p>
	 *
	 * <p><b>Awalan {@code jenis} yang longgar.</b> Penyaringan memakai awalan, bukan kecocokan
	 * persis, karena setiap foto galeri menyimpan penanda tambahan di belakang awalan tersebut.
	 * Perlu disadari bahwa {@code ref} hanyalah angka id tanpa penanda kelas: pemisahan antar-jenis
	 * pemilik lampiran sepenuhnya bergantung pada awalan {@code jenis} ini. Menambah jenis lampiran
	 * baru yang awalannya kebetulan sama akan membuatnya ikut terbaca sebagai foto galeri
	 * vendor.</p>
	 *
	 * <p><b>Pengelolaan sesi dan galat.</b> Pada jalur sukses sesi ditutup lewat
	 * {@code StreamingHibernateUtil.getInstance().closeSession()} dan daftar lampiran dilepas
	 * dengan menyetelnya ke {@code null} (kebiasaan lama untuk membantu pengumpul sampah; tidak
	 * berpengaruh nyata pada JVM modern). Pada jalur gagal, blok {@code catch} memanggil
	 * {@code rollbackTransaction()} lalu mencetak jejak tumpukan dan mencatatkannya ke
	 * {@code ErrorAuditUtil}. Perhatikan dua hal: penutupan sesi <b>tidak</b> berada di blok
	 * {@code finally}, sehingga jalur gagal mengandalkan {@code rollbackTransaction()} untuk
	 * membereskan sesi; dan method ini <b>tidak pernah melempar</b> ke pemanggil, sehingga
	 * kegagalan pemuatan hanya tampak sebagai galeri yang kosong atau usang di layar.</p>
	 *
	 * <p><b>Keamanan thread.</b> Sebagaimana diuraikan pada {@link #galeries}, tidak ada penguncian
	 * sama sekali. Dua pemanggilan bersamaan untuk vendor yang sama dapat saling menimpa, dan
	 * pembaca peta dalam dapat menyaksikan keadaan setengah terisi tepat setelah
	 * {@code data.clear()}.</p>
	 *
	 * @param penyediaAsset vendor yang galerinya dimuat ulang; harus sudah memiliki id, sebab
	 *                      id itulah yang dipakai sebagai {@code ref} pencarian lampiran.
	 *                      Melewatkan {@code null} akan memicu {@code NullPointerException} yang
	 *                      langsung tertelan blok {@code catch} sehingga tidak terlihat pemanggil
	 * @see #galeries
	 * @see #putPhoto(Map)
	 */
	@SuppressWarnings("unchecked")
	public static void reloadGaleries(PenyediaAsset penyediaAsset) {
		try {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq("ref", penyediaAsset.getId()))
					.add(Restrictions.ilike("jenis", "Galery_PenyediaAsset_", MatchMode.START)).list();

			Map<String, String> data = galeries.get(penyediaAsset.getId());
			if (data == null) {
				data = new HashMap<String, String>();
				galeries.put(penyediaAsset.getId(), data);
			}
			data.clear();
			for (LampiranLain lampiran : lampiranLains) {
				String link = FileFotoLain.ambilLinkLampiranLain(lampiran, false, false, LampiranLain.class);
				data.put(link, lampiran.getDeskripsi());
			}

			StreamingHibernateUtil.getInstance().closeSession();
			lampiranLains = null;
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/asset/PenyediaAsset.java:135");
		}
	}

	/** Kode vendor; dapat diturunkan otomatis dari id — lihat {@link #getKode()}. */
	private String kode;
	/** Nama badan usaha vendor. */
	private String nama;
	/** Nama pemilik/penanggung jawab badan usaha. */
	private String pemilik;
	/** Kota alamat vendor; menentukan {@link #propinsi} lewat {@link #getPropinsi()}. */
	private Kota kota;
	/** Propinsi alamat vendor; ditimpa dari {@link #kota} bila kota terisi. */
	private Propinsi propinsi;
	/** Kecamatan alamat vendor; disimpan sebagai {@link Wilayah} dan dikoreksi {@link #getKecamatan()}. */
	private Wilayah kecamatan;
	/** Bentuk badan usaha vendor; bawaannya {@link JenisPenyediaAsset#PERUSAHAAN_UMUM}. */
	private JenisPenyediaAsset jenisPenyediaAsset;
	/** Peran vendor dalam rantai pasok; bawaannya {@link KategoriPenyediaAsset#PEDAGANG_LANGSUNG}. */
	private KategoriPenyediaAsset kategoriPenyediaAsset;
	/** Alamat jalan vendor. */
	private String alamat;
	/** Kode pos alamat vendor. */
	private String kodePos;
	/** Nomor telepon vendor; dipakai {@link #tampilkanHp(Component)} untuk membuat tautan WhatsApp. */
	private String telp;
	/** Nomor faks vendor. */
	private String fax;
	/** Nama orang yang dapat dihubungi di pihak vendor. */
	private String kontak;
	/** Alamat surel vendor; dipakai {@link #tampilkanEmail(Component)} dan dijaga keunikannya oleh layar CRUD. */
	private String email;
	/** Catatan bebas tentang vendor. */
	private String keterangan;
	/** Kedudukan organisatoris vendor (Pusat/Cabang); BUKAN status kelayakan — lihat {@link StatusPenyediaAsset}. */
	private StatusPenyediaAsset statusPenyediaAsset;
	/** Koordinat bujur lokasi vendor, disimpan sebagai teks. */
	private String longitude;
	/** Koordinat lintang lokasi vendor, disimpan sebagai teks. */
	private String latitude;
	/** Penanda vendor dapat dipakai; diturunkan dari disposisi SOP — lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Daftar rekening bank tambahan dalam bentuk teks JSON; lihat {@link #getBank()}. */
	private String bank;

	/** Nomor akta pendirian perusahaan. */
	private String noAktaPendirian;
	/** Tanggal akta pendirian perusahaan. */
	private Date tanggalAktaPendirian;
	/** Nama notaris yang membuat akta pendirian. */
	private String namaNotaris;
	/** Nomor surat pengesahan akta pendirian oleh instansi berwenang. */
	private String noPengesahan;
	/** Tanggal surat pengesahan akta pendirian. */
	private Date tanggalPengesahan;

	/** Nomor akta perubahan terakhir perusahaan. */
	private String noAktaPendirianAkhir;
	/** Tanggal akta perubahan terakhir perusahaan. */
	private Date tanggalAktaPendirianAkhir;
	/** Nama notaris yang membuat akta perubahan terakhir. */
	private String namaNotarisAkhir;
	/** Nomor surat pengesahan akta perubahan terakhir. */
	private String noPengesahanAkhir;
	/** Tanggal surat pengesahan akta perubahan terakhir. */
	private Date tanggalPengesahanAkhir;

	/** Nomor pokok wajib pajak vendor; keunikannya divalidasi layar CRUD, bukan oleh skema. */
	private String npwp;
	/** Nomor rekening bank utama vendor untuk pembayaran. */
	private String noRek;
	/** Nama pemilik rekening; bawaannya mengikuti {@link #nama} — lihat {@link #getAtasNama()}. */
	private String atasNama;
	/** Bank pemilik {@link #noRek}. */
	private Bank bankUtama;
	/** Akun utang bagan akun untuk vendor ini; punya nilai bawaan dari konfigurasi — lihat {@link #getAkunUtang()}. */
	private Akun akunUtang;
	/** Waktu pendaftaran vendor; lihat peringatan pada {@link #getTanggalPembuatan()}. */
	private Date tanggalPembuatan;
	/** Penyetuju vendor; diturunkan dari disposisi SOP — lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;
	/** Waktu persetujuan; diturunkan dari disposisi SOP — lihat {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;
	/** Pendaftar vendor; diturunkan dari disposisi SOP — lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Disposisi SOP pendaftaran vendor; sumber kebenaran bagi status aktif dan data persetujuan. */
	private DisposisiSop disposisiSop;

	/** Bidang pekerjaan pertama yang dapat ditangani vendor. */
	private JenisPekerjaanPenyedia jenisPekerjaanPenyedia1;
	/** Bidang pekerjaan kedua yang dapat ditangani vendor. */
	private JenisPekerjaanPenyedia jenisPekerjaanPenyedia2;
	/** Bidang pekerjaan ketiga yang dapat ditangani vendor. */
	private JenisPekerjaanPenyedia jenisPekerjaanPenyedia3;
	/** Bidang pekerjaan keempat yang dapat ditangani vendor. */
	private JenisPekerjaanPenyedia jenisPekerjaanPenyedia4;
	/** Bidang pekerjaan kelima yang dapat ditangani vendor. */
	private JenisPekerjaanPenyedia jenisPekerjaanPenyedia5;

	/** Templat penomoran surat untuk pengajuan penyedia; bawaannya {@code NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA}. */
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;
	/** Bulan pendaftaran untuk keperluan penomoran surat; bawaannya bulan berjalan. */
	private Integer bulan;
	/** Tahun pendaftaran untuk keperluan penomoran surat; bawaannya tahun berjalan. */
	private Integer tahun;
	/** Formula/konfigurasi tambahan dalam bentuk teks JSON; lihat {@link #getFormula()}. */
	private String formula;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate.
	 *
	 * <p>Dipakai pula oleh {@code PenyediaAssetAction} saat membuat vendor baru. Objek yang baru
	 * dibuat belum memiliki id, sehingga sejumlah getter berperilaku berbeda hingga vendor
	 * tersimpan: {@link #getJenisPenyediaAsset()}, {@link #getKategoriPenyediaAsset()}, dan
	 * {@link #getStatusPenyediaAsset()} baru menyuntikkan nilai bawaannya setelah
	 * {@link #getId()} tidak lagi {@code null}.</p>
	 */
	public PenyediaAsset() {
	}

	/**
	 * Mengembalikan kunci utama baris vendor ini.
	 *
	 * <p>Nilai ini dipakai luas sebagai penghubung: sebagai {@code ref} lampiran galeri foto
	 * (lihat {@link #reloadGaleries(PenyediaAsset)}), sebagai kunci cache {@link #galeries}, dan
	 * sebagai sumber {@link #getKode()} otomatis bagi vendor yang kodenya belum diisi.</p>
	 *
	 * @return id vendor, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris vendor ini.
	 *
	 * <p>Hanya untuk kebutuhan Hibernate dan penyalinan objek; jangan menyetel id secara manual
	 * pada baris yang akan disimpan sebagai data baru, terlebih karena id ini menjadi kunci
	 * pelekatan lampiran dan sumber kode otomatis.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama badan usaha vendor setelah dipangkas spasi tepinya.
	 *
	 * <p>Method <b>mempertahankan</b> {@code null} apa adanya dan hanya memangkas bila nilainya
	 * ada, sehingga pemanggil tetap perlu berjaga terhadap {@code null} walau kolomnya
	 * dideklarasikan {@code nullable = false} pada tingkat basis data.</p>
	 *
	 * @return nama vendor yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama badan usaha vendor.
	 *
	 * <p>Perhatikan bahwa {@link #getAtasNama()} memakai nilai ini sebagai bawaan nama pemilik
	 * rekening: mengganti nama vendor <i>setelah</i> {@code atasNama} sempat terisi otomatis tidak
	 * akan memperbarui nama pemilik rekening tersebut.</p>
	 *
	 * @param nama nama badan usaha
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas tentang vendor, dengan {@code null} dinormalkan menjadi string
	 * kosong.
	 *
	 * @return catatan bebas, atau string kosong bila belum diisi; tidak pernah {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan;
	}

	/**
	 * Menyetel catatan bebas tentang vendor.
	 *
	 * @param keterangan teks catatan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan alamat jalan vendor, dengan {@code null} dinormalkan menjadi string kosong.
	 *
	 * @return alamat, atau string kosong bila belum diisi; tidak pernah {@code null}
	 */
	public String getAlamat() {
		return alamat == null ? "" : alamat;
	}

	/**
	 * Menyetel alamat jalan vendor.
	 *
	 * @param alamat alamat; boleh {@code null}
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan kode pos alamat vendor, dengan {@code null} dinormalkan menjadi string kosong.
	 *
	 * @return kode pos, atau string kosong bila belum diisi; tidak pernah {@code null}
	 */
	public String getKodePos() {
		return kodePos == null ? "" : kodePos;
	}

	/**
	 * Menyetel kode pos alamat vendor.
	 *
	 * @param kodePos kode pos; boleh {@code null}
	 */
	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	/**
	 * Mengembalikan nomor telepon vendor, dengan {@code null} dinormalkan menjadi string kosong.
	 *
	 * <p>Nilai mentahnya dapat berisi nomor sentinel yang secara historis dipakai sebagai penanda
	 * "tidak ada nomor" ({@code "0000000000"} dan sejenisnya); {@link #tampilkanHp(Component)}
	 * menyaringnya saat menampilkan, tetapi getter ini mengembalikan apa adanya.</p>
	 *
	 * @return nomor telepon, atau string kosong bila belum diisi; tidak pernah {@code null}
	 */
	public String getTelp() {
		return telp == null ? "" : telp;
	}

	/**
	 * Menyetel nomor telepon vendor.
	 *
	 * @param telp nomor telepon; boleh {@code null}
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Mengembalikan nomor faks vendor, dengan {@code null} dinormalkan menjadi string kosong.
	 *
	 * @return nomor faks, atau string kosong bila belum diisi; tidak pernah {@code null}
	 */
	public String getFax() {
		return fax == null ? "" : fax;
	}

	/**
	 * Menyetel nomor faks vendor.
	 *
	 * @param fax nomor faks; boleh {@code null}
	 */
	public void setFax(String fax) {
		this.fax = fax;
	}

	/**
	 * Mengembalikan nama orang yang dapat dihubungi di pihak vendor, dengan {@code null}
	 * dinormalkan menjadi string kosong.
	 *
	 * @return nama kontak, atau string kosong bila belum diisi; tidak pernah {@code null}
	 */
	public String getKontak() {
		return kontak == null ? "" : kontak;
	}

	/**
	 * Menyetel nama orang yang dapat dihubungi di pihak vendor.
	 *
	 * @param kontak nama kontak; boleh {@code null}
	 */
	public void setKontak(String kontak) {
		this.kontak = kontak;
	}

	/**
	 * Mengembalikan alamat surel vendor, dengan {@code null} dinormalkan menjadi string kosong.
	 *
	 * <p>Surel bukan sekadar data kontak pada modul ini: {@code PenyediaAssetAction} memakainya
	 * untuk memeriksa pendaftaran ganda ({@code createCriteria(...).add(eq("email", …))}) dan
	 * sebagai dasar pembuatan akun pengguna bagi vendor. Menyunting surel karena itu berdampak di
	 * luar data alamat semata.</p>
	 *
	 * @return alamat surel, atau string kosong bila belum diisi; tidak pernah {@code null}
	 */
	public String getEmail() {
		return email == null ? "" : email;
	}

	/**
	 * Menyetel alamat surel vendor.
	 *
	 * <p>Keunikan surel tidak ditegakkan skema basis data melainkan hanya diperiksa layar CRUD
	 * sebelum menyimpan; penulisan lewat jalur lain dapat menghasilkan surel kembar.</p>
	 *
	 * @param email alamat surel; boleh {@code null}
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Mengembalikan kode vendor, dengan pembangkitan otomatis dari id bagi vendor yang sudah
	 * selesai disposisinya namun kodenya masih kosong.
	 *
	 * <p><b>Logika lengkap.</b> Kode dibangkitkan otomatis hanya bila <i>ketiga</i> syarat berikut
	 * terpenuhi sekaligus: vendor sudah tersimpan ({@code getId() != null}), disposisi SOP-nya ada
	 * dan sudah mencapai langkah akhir ({@code getDisposisiSop().getDisposisiEnd() != null}), dan
	 * kolom {@code kode} masih {@code null} atau kosong. Dalam hal itu kode dihitung sebagai id
	 * yang dilapisi nol di depan hingga lima karakter, lewat
	 * {@code Common.maxPanjangAkhir("000000" + getId(), 5)}. Bila salah satu syarat tidak
	 * terpenuhi, method mengembalikan kode tersimpan yang sudah dipangkas, atau string kosong bila
	 * belum ada.</p>
	 *
	 * <p><b>Kode turunan tidak disimpan.</b> Berbeda dari banyak getter lain di kelas ini, method
	 * ini <b>tidak</b> menulis hasilnya kembali ke bidang {@link #kode}. Kode otomatis karena itu
	 * bersifat tampilan semata: ia dihitung ulang setiap kali dibaca dan kolom {@code kode} di
	 * basis data tetap kosong. Akibat yang perlu disadari, pencarian yang menembak kolom secara
	 * langsung tidak akan menemukan vendor lewat kode otomatisnya —
	 * {@code AmbilDataPenyediaAssetBanbox} menebus vendor dengan
	 * {@code Restrictions.ilike("kode", …, MatchMode.EXACT)} atas kolom mentah, sehingga vendor
	 * berkode otomatis tidak dapat ditemukan dengan mengetikkan kode yang tampil di layar.</p>
	 *
	 * <p><b>Panjang lima karakter.</b> Karena {@code maxPanjangAkhir} mengambil lima karakter
	 * terakhir, id yang melewati 99999 akan menghasilkan kode yang terpotong dan tidak lagi unik
	 * (mis. id 100001 dan id 200001 sama-sama menghasilkan {@code "00001"}). Batas ini jauh dari
	 * jangkauan populasi vendor pada umumnya, namun perlu diketahui sebelum mengandalkan kode
	 * otomatis sebagai pengenal unik.</p>
	 *
	 * <p><b>Ketergantungan pada disposisi.</b> Syarat {@code getDisposisiEnd() != null} berarti
	 * vendor yang masih dalam proses persetujuan tampil tanpa kode, dan kodenya baru muncul setelah
	 * disposisi mencapai akhir — termasuk bila akhir tersebut adalah penolakan. Pemanggilan
	 * {@link #getDisposisiSop()} di dalam method ini juga berarti getter ini memicu resolusi
	 * relasi malas, sehingga tidak sepenuhnya murni membaca.</p>
	 *
	 * @return kode vendor tersimpan yang sudah dipangkas; atau kode otomatis lima digit bila
	 *         syaratnya terpenuhi; atau string kosong. Tidak pernah {@code null}
	 */
	public String getKode() {
		return getId() != null && getDisposisiSop() != null && getDisposisiSop().getDisposisiEnd() != null
				&& (kode == null || kode.isEmpty()) ? ais.common.Common.maxPanjangAkhir("000000" + getId(), 5)
						: (kode == null ? "" : kode.trim());
	}

	/**
	 * Menyetel kode vendor.
	 *
	 * <p>Mengisi kode secara eksplisit mematikan pembangkitan otomatis pada {@link #getKode()}.
	 * Keunikan kode diperiksa layar CRUD ({@code PenyediaAssetAction} menghitung baris lain dengan
	 * kode sama sebelum menyimpan), bukan oleh batasan basis data.</p>
	 *
	 * @param kode kode vendor; boleh {@code null} untuk mengaktifkan kembali kode otomatis
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kecamatan alamat vendor, dengan upaya perbaikan otomatis bila wilayah yang
	 * tersimpan ternyata tidak memiliki wilayah induk.
	 *
	 * <p><b>Dua tahap.</b> Pertama, proksi malas diselesaikan lewat {@code check(...)} milik
	 * {@code GeneralValueObject}. Kedua — dan ini bagian yang tidak biasa — bila wilayah hasil
	 * resolusi tidak {@code null} namun {@code getWilayahInduk()} bernilai {@code null}, method
	 * menelusuri <b>seluruh</b> {@link Wilayah} yang tersimpan di cache
	 * {@code ConstantValues.ambilBerdasarClass(Wilayah.class)} untuk mencari wilayah lain yang
	 * kode {@code feeder}-nya sama persis dan yang <i>memiliki</i> wilayah induk, lalu memakainya
	 * sebagai pengganti.</p>
	 *
	 * <p><b>Mengapa ada.</b> Perbaikan ini menambal data wilayah kembar yang lazim terjadi pada
	 * impor dari sumber luar: satu kode feeder yang sama dapat menghasilkan dua baris wilayah, satu
	 * lengkap dengan induknya dan satu lagi yatim. Vendor yang terlanjur menunjuk baris yatim akan
	 * kehilangan informasi kabupaten/kota-nya pada laporan berjenjang; method ini memindahkannya ke
	 * baris yang lengkap.</p>
	 *
	 * <p><b>Konsekuensi yang perlu disadari.</b> Perbaikan tersebut <b>menulis kembali</b> ke
	 * bidang instans ({@code kecamatan = w}), sehingga getter ini memutasi state objek. Bila objek
	 * vendor sedang terpasang pada sesi Hibernate, penggantian tersebut dapat ikut ter-<i>flush</i>
	 * ke basis data — artinya sekadar membuka daftar vendor atau mencetak laporan dapat mengubah
	 * kolom kecamatan tanpa tindakan pengguna. Perilaku ini boleh jadi memang diinginkan sebagai
	 * penambalan bertahap, tetapi harus disadari saat menelusuri riwayat Envers yang mencatat
	 * perubahan yang tak seorang pun mengaku melakukannya.</p>
	 * <p>Selain itu penelusuran dilakukan atas <i>seluruh</i> koleksi wilayah pada setiap
	 * pemanggilan yang wilayahnya yatim, tanpa indeks maupun penyimpanan hasil; pada layar daftar
	 * yang menampilkan ratusan vendor, biayanya berlipat. Pencocokan berhenti pada kandidat pertama
	 * yang ditemukan, dan karena iterasi atas {@code values()} sebuah peta tidak menjamin urutan,
	 * kandidat yang terpilih dapat berbeda antar-pemanggilan bila ada lebih dari satu wilayah
	 * berinduk dengan feeder yang sama.</p>
	 *
	 * @return kecamatan vendor setelah resolusi dan kemungkinan perbaikan, atau {@code null} bila
	 *         belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kecamatan_wilayah", nullable = true)
	public Wilayah getKecamatan() {
		kecamatan = check(kecamatan);

		if (kecamatan != null && kecamatan.getWilayahInduk() == null) {

			for (Object o : ConstantValues.ambilBerdasarClass(Wilayah.class).values()) {
				Wilayah w = (Wilayah) o;
				if (w != null && w.getFeeder() != null && kecamatan.getFeeder() != null
						&& kecamatan.getFeeder().equals(w.getFeeder()) && w.getWilayahInduk() != null) {
					kecamatan = w;
					break;
				}
			}

		}

		return kecamatan;
	}

	/**
	 * Menyetel kecamatan alamat vendor.
	 *
	 * <p>Nilai yang disetel di sini masih dapat diganti {@link #getKecamatan()} bila wilayah yang
	 * diberikan tidak memiliki wilayah induk.</p>
	 *
	 * @param kecamatan wilayah tingkat kecamatan; boleh {@code null}
	 */
	public void setKecamatan(Wilayah kecamatan) {
		this.kecamatan = kecamatan;
	}

	/**
	 * Mengembalikan propinsi alamat vendor, yang <b>diturunkan dari kota</b> bila kota terisi.
	 *
	 * <p>Method menyelesaikan proksi malas {@link #propinsi} dan {@link #kota}, lalu — bila kota
	 * ada dan kota tersebut memiliki propinsi — <b>menimpa</b> propinsi dengan propinsi milik kota.
	 * Dengan kata lain kota adalah sumber kebenaran dan propinsi hanyalah nilai turunan yang
	 * disimpan demi kemudahan pelaporan.</p>
	 * <p><b>Efek samping.</b> Penimpaan tersebut menulis kembali ke bidang instans, sehingga
	 * membaca propinsi dapat mengubah data vendor dan perubahan itu dapat ikut ter-<i>flush</i>
	 * bila objek terpasang pada sesi. Propinsi yang sengaja diisi berbeda dari propinsi kotanya
	 * karena itu tidak akan bertahan — nilai tersebut akan hilang pada pembacaan berikutnya.
	 * Sebaliknya, bila kota dikosongkan, propinsi terakhir yang pernah diturunkan akan
	 * <i>bertahan</i> karena tidak ada cabang yang mengosongkannya kembali; vendor dapat berakhir
	 * dengan propinsi yang tidak lagi berkaitan dengan alamatnya.</p>
	 *
	 * @return propinsi vendor, diturunkan dari kota bila memungkinkan; atau {@code null} bila
	 *         keduanya belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi", nullable = true)
	public Propinsi getPropinsi() {
		propinsi = check(propinsi);
		kota = check(kota);
		if (kota != null && kota.getPropinsi() != null) {
			propinsi = kota.getPropinsi();
		}

		return propinsi;
	}

	/**
	 * Menyetel propinsi alamat vendor.
	 *
	 * <p>Nilai yang disetel di sini akan ditimpa {@link #getPropinsi()} pada pembacaan berikutnya
	 * bila {@link #getKota()} terisi dan memiliki propinsi. Untuk mengubah propinsi secara efektif,
	 * ubahlah kotanya.</p>
	 *
	 * @param propinsi propinsi; boleh {@code null}
	 */
	public void setPropinsi(Propinsi propinsi) {
		this.propinsi = propinsi;
	}

	/**
	 * Mengembalikan kota alamat vendor setelah proksi malasnya diselesaikan {@code check(...)}.
	 *
	 * <p>Kota adalah sumber kebenaran bagi {@link #getPropinsi()}. Method menulis kembali hasil
	 * resolusi ke bidang instansnya agar resolusi tidak diulang pada setiap pembacaan; efek samping
	 * ini ringan dibanding penimpaan pada {@link #getPropinsi()}, namun tetap berarti getter tidak
	 * bebas efek samping dan tidak aman dipanggil bersamaan dari beberapa thread atas objek yang
	 * sama.</p>
	 *
	 * @return kota vendor, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota", nullable = true)
	public Kota getKota() {
		kota = check(kota);
		return kota;
	}

	/**
	 * Menyetel kota alamat vendor.
	 *
	 * <p>Karena {@link #getPropinsi()} menurunkan propinsi dari kota, mengubah kota secara tidak
	 * langsung mengubah propinsi vendor pada pembacaan berikutnya.</p>
	 *
	 * @param kota kota; boleh {@code null}
	 */
	public void setKota(Kota kota) {
		this.kota = kota;
	}

	/**
	 * Mengembalikan bentuk badan usaha vendor, dengan penyuntikan nilai bawaan bagi vendor yang
	 * sudah tersimpan namun jenisnya belum diisi.
	 *
	 * <p>Bila {@code getId() != null} dan {@link #jenisPenyediaAsset} masih {@code null}, bidang
	 * diisi {@link JenisPenyediaAsset#PERUSAHAAN_UMUM}. Bila tidak, proksi malasnya diselesaikan
	 * lewat {@code check(...)}. Perhatikan bahwa kedua cabang sama-sama <b>menulis kembali</b> ke
	 * bidang instans, sehingga getter ini memutasi state dan nilai bawaan tersebut dapat ikut
	 * ter-<i>flush</i> ke basis data tanpa tindakan pengguna.</p>
	 * <p>Konstanta {@link JenisPenyediaAsset#PERUSAHAAN_UMUM} adalah instance yang terlepas dari
	 * sesi (disiapkan {@code reloadDefault()} pada sesi yang segera ditutup) dan bernilai
	 * {@code null} bila penyemaian gagal; dalam keadaan itu getter ini akan mengembalikan
	 * {@code null} untuk vendor yang jenisnya kosong.</p>
	 * <p>Untuk vendor yang <b>belum</b> tersimpan, cabang bawaan tidak dijalankan sehingga
	 * jenisnya tetap {@code null} sampai pengguna memilihnya atau vendor tersimpan.</p>
	 *
	 * @return bentuk badan usaha vendor, atau {@code null} bila vendor belum tersimpan dan jenisnya
	 *         belum dipilih, atau bila penyemaian nilai bawaan gagal
	 * @see JenisPenyediaAsset
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penyedia_asset", nullable = true)
	public JenisPenyediaAsset getJenisPenyediaAsset() {
		if (getId() != null && jenisPenyediaAsset == null) {
			jenisPenyediaAsset = JenisPenyediaAsset.PERUSAHAAN_UMUM;
		} else {
			jenisPenyediaAsset = check(jenisPenyediaAsset);
		}
		return jenisPenyediaAsset;
	}

	/**
	 * Menyetel bentuk badan usaha vendor.
	 *
	 * @param jenisPenyediaAsset jenis penyedia; {@code null} akan digantikan nilai bawaan pada
	 *                           pembacaan berikutnya bila vendor sudah tersimpan
	 */
	public void setJenisPenyediaAsset(JenisPenyediaAsset jenisPenyediaAsset) {
		this.jenisPenyediaAsset = jenisPenyediaAsset;
	}

	/**
	 * Mengembalikan peran vendor dalam rantai pasok, dengan penyuntikan nilai bawaan bagi vendor
	 * yang sudah tersimpan namun kategorinya belum diisi.
	 *
	 * <p>Perilakunya sejajar dengan {@link #getJenisPenyediaAsset()}: bila
	 * {@code getId() != null} dan bidangnya {@code null}, diisi
	 * {@link KategoriPenyediaAsset#PEDAGANG_LANGSUNG}; selain itu proksi malasnya diselesaikan
	 * {@code check(...)}. Kedua cabang menulis kembali ke bidang instans, sehingga nilai bawaan
	 * dapat ikut tersimpan ke basis data tanpa tindakan pengguna. Akibat lanjutannya pada
	 * pelaporan: kategori "Pedagang Langsung" akan tampak mendominasi populasi vendor bukan karena
	 * data lapangan demikian, melainkan karena ia nilai bawaan bagi setiap vendor yang kategorinya
	 * belum pernah diisi.</p>
	 *
	 * @return peran vendor dalam rantai pasok, atau {@code null} bila vendor belum tersimpan dan
	 *         kategorinya belum dipilih, atau bila penyemaian nilai bawaan gagal
	 * @see KategoriPenyediaAsset
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kategori_penyedia_asset", nullable = true)
	public KategoriPenyediaAsset getKategoriPenyediaAsset() {
		if (getId() != null && kategoriPenyediaAsset == null) {
			kategoriPenyediaAsset = KategoriPenyediaAsset.PEDAGANG_LANGSUNG;
		} else {
			kategoriPenyediaAsset = check(kategoriPenyediaAsset);
		}
		return kategoriPenyediaAsset;
	}

	/**
	 * Menyetel peran vendor dalam rantai pasok.
	 *
	 * @param kategoriPenyediaAsset kategori penyedia; {@code null} akan digantikan nilai bawaan
	 *                              pada pembacaan berikutnya bila vendor sudah tersimpan
	 */
	public void setKategoriPenyediaAsset(KategoriPenyediaAsset kategoriPenyediaAsset) {
		this.kategoriPenyediaAsset = kategoriPenyediaAsset;
	}

	/**
	 * Mengembalikan penanda apakah vendor boleh dipakai, diturunkan dari keadaan disposisi SOP-nya.
	 *
	 * <p><b>Inilah satu-satunya gerbang vendor yang benar-benar ditegakkan di AIS.</b> Pemilih
	 * vendor {@code AmbilDataPenyediaAssetBanbox} menyaring dengan
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} pada kedua jalurnya, sehingga
	 * vendor yang nilainya {@code false} tidak muncul di daftar pilihan dan tidak dapat ditebus
	 * lewat kode untuk transaksi baru. Perlu dicatat bahwa penyaringan itu bekerja atas
	 * <b>kolom basis data</b>, bukan atas nilai balik method ini — lihat catatan penyimpanan di
	 * bawah.</p>
	 *
	 * <p><b>Cara penurunan.</b> Method membaca disposisi lewat {@link #getDisposisiSop()} lalu
	 * menerapkan dua aturan yang keduanya hanya dapat <i>menutup</i>:</p>
	 * <ol>
	 *   <li>bila disposisi ada namun {@code getAktif()}-nya {@code false}, vendor dijadikan tidak
	 *       aktif;</li>
	 *   <li>bila disposisi ada dan langkah akhirnya berada pada alur SOP yang ditandai sebagai
	 *       titik penolakan ({@code getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()}),
	 *       vendor dijadikan tidak aktif.</li>
	 * </ol>
	 * <p>Setelah itu nilai dikembalikan dengan {@code aktif == null ? true : aktif}.</p>
	 *
	 * <p><b>Gerbang satu arah.</b> Perhatikan bahwa tidak ada cabang yang pernah menyetel
	 * {@code aktif = true}. Penurunan ini hanya dapat menonaktifkan, tidak pernah mengaktifkan
	 * kembali. Konsekuensinya: bila sebuah disposisi yang semula ditolak kemudian diperbaiki
	 * sehingga tidak lagi berakhir pada langkah penolakan, vendor <b>tetap</b> nonaktif sampai
	 * seseorang menyetelnya kembali secara eksplisit lewat {@link #setAktif(Boolean)}. Sifat satu
	 * arah ini aman dari sisi pengendalian (tidak ada vendor yang diam-diam hidup kembali), namun
	 * mudah menimbulkan kebingungan operasional ketika vendor yang sudah disetujui ulang tetap
	 * tidak muncul di daftar pilihan.</p>
	 *
	 * <p><b>Default membuka.</b> Nilai {@code null} dibaca sebagai {@code true}. Vendor lama yang
	 * kolomnya belum pernah terisi — dan vendor baru sebelum disposisinya berjalan — karena itu
	 * dianggap aktif. Ini konsisten dengan penyaringan {@code isNull("aktif")} pada pemilih vendor,
	 * yang juga meloloskan baris ber-NULL.</p>
	 *
	 * <p><b>Getter yang memutasi state.</b> Method menulis ke bidang {@link #aktif} maupun
	 * {@link #disposisiSop}. Bila objek vendor terpasang pada sesi Hibernate, penonaktifan yang
	 * dihitung di sini dapat ikut ter-<i>flush</i> ke kolom basis data — dan justru mekanisme
	 * itulah yang membuat penyaringan berbasis kolom pada pemilih vendor ikut memperhitungkan
	 * penolakan disposisi. Sisi buruknya, sekadar membuka layar daftar atau menjalankan laporan
	 * dapat menulis perubahan ke basis data, dan riwayat Envers akan mencatatnya seolah ada
	 * penyuntingan.</p>
	 *
	 * @return {@code true} bila vendor boleh dipakai atau statusnya belum pernah disetel;
	 *         {@code false} bila disposisinya tidak aktif atau berakhir pada penolakan, atau bila
	 *         dinonaktifkan secara eksplisit
	 * @see #setAktif(Boolean)
	 * @see #getDisposisiSop()
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda apakah vendor boleh dipakai.
	 *
	 * <p>Ini adalah jalur yang benar untuk memblokir vendor secara manual — bukan menambah baris
	 * "Blacklist" pada {@link StatusPenyediaAsset}, yang tidak berefek apa pun. Menyetel
	 * {@code false} akan menyembunyikan vendor dari pemilih vendor sehingga tidak dapat dipakai
	 * pada transaksi baru; transaksi lama yang sudah terlanjur menunjuknya <b>tidak</b> terpengaruh
	 * dan tetap dapat dilanjutkan.</p>
	 * <p>Menyetel {@code true} dapat dibatalkan kembali oleh {@link #getAktif()} pada pembacaan
	 * berikutnya bila disposisi vendor tidak aktif atau berakhir pada penolakan; untuk
	 * mengaktifkan vendor yang pernah ditolak, keadaan disposisinya harus dibereskan lebih
	 * dahulu.</p>
	 *
	 * @param aktif {@code true} untuk mengizinkan, {@code false} untuk memblokir, {@code null}
	 *              untuk mengembalikan ke bawaan (dianggap aktif)
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan disposisi SOP pendaftaran vendor ini setelah proksi malasnya diselesaikan
	 * {@code check(...)}.
	 *
	 * <p>Disposisi adalah sumber kebenaran bagi empat nilai turunan: {@link #getAktif()},
	 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, dan
	 * {@link #getTanggalPersetujuan()}. Ia juga menentukan apakah {@link #getKode()} membangkitkan
	 * kode otomatis. Karena itu getter ini dipanggil bersarang dari banyak getter lain, dan
	 * pemanggilan tersebut dapat memicu {@code LazyInitializationException} bila objek vendor
	 * sudah lepas dari sesi — masalah yang ditangani secara eksplisit pada
	 * {@link #getTanggalPersetujuan()}.</p>
	 *
	 * @return disposisi SOP vendor, atau {@code null} bila vendor belum melewati alur persetujuan
	 * @see DataSop
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP pendaftaran vendor ini, dengan penjagaan terhadap penimpaan oleh
	 * disposisi kosong.
	 *
	 * <p><b>Penjaga masuk.</b> Masukan yang {@code null} atau yang belum memiliki id ditolak lewat
	 * {@code return} awal, sehingga disposisi yang sudah tersimpan tidak dapat dihapus atau
	 * ditimpa oleh objek disposisi baru yang belum tersimpan. Penjagaan ini penting karena
	 * hilangnya disposisi akan membuat vendor kehilangan jejak persetujuannya sekaligus
	 * mengembalikan {@link #getAktif()} ke nilai bawaan {@code true}.</p>
	 *
	 * <p><b>Catatan mengenai ekspresi terner.</b> Setelah penjaga masuk, badan method mengevaluasi
	 * sebuah ekspresi terner yang syaratnya memuat kembali pemeriksaan
	 * {@code (disposisiSop == null || disposisiSop.getId() == null)}. Bagian itu <b>tidak dapat
	 * bernilai benar</b> pada titik tersebut, sebab kedua keadaan itu sudah dipulangkan oleh
	 * penjaga masuk. Dengan demikian syaratnya selalu bernilai salah dan ekspresi selalu memilih
	 * cabang kedua, yaitu menugaskan parameter apa adanya. Sisa pemeriksaan itu adalah peninggalan
	 * dari versi sebelum penjaga masuk ditambahkan; ia tidak berbahaya namun menyesatkan pembaca
	 * dan sebaiknya tidak dijadikan contoh pada kode baru.</p>
	 *
	 * @param disposisiSop disposisi yang sudah tersimpan; {@code null} atau yang belum ber-id
	 *                     diabaikan diam-diam
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Mengembalikan kedudukan organisatoris vendor (Pusat/Cabang), dengan penyuntikan nilai bawaan
	 * bagi vendor yang sudah tersimpan namun statusnya belum diisi.
	 *
	 * <p><b>Bukan status kelayakan.</b> Terlepas dari namanya, nilai ini tidak menyatakan boleh
	 * atau tidaknya vendor dipakai. Nilai yang tersedia hanya "Pusat" dan "Cabang", editornya pada
	 * form vendor sudah dikomentari di {@code PenyediaAssetAction}, dan tidak ada satu pun proses
	 * transaksi yang membacanya — pemakaiannya terbatas pada label daftar, kolom laporan, filter
	 * pencarian, dan pengelompokan dasbor. Untuk memblokir vendor, gunakan
	 * {@link #setAktif(Boolean)}. Uraian lengkapnya ada pada {@link StatusPenyediaAsset}.</p>
	 *
	 * <p>Bila {@code getId() != null} dan bidangnya {@code null}, diisi
	 * {@link StatusPenyediaAsset#PUSAT}; selain itu proksi malasnya diselesaikan {@code check(...)}.
	 * Kedua cabang menulis kembali ke bidang instans sehingga nilai bawaan dapat ikut tersimpan.
	 * Karena editornya dinonaktifkan, method inilah yang praktis menjadi satu-satunya penulis kolom
	 * status, dan akibatnya hampir seluruh populasi vendor bernilai "Pusat" — daya diskriminasi
	 * kolom ini pada laporan mendekati nol.</p>
	 *
	 * @return kedudukan organisatoris vendor, atau {@code null} bila vendor belum tersimpan dan
	 *         statusnya belum dipilih, atau bila penyemaian nilai bawaan gagal
	 * @see StatusPenyediaAsset
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_penyedia_asset", nullable = true)
	public StatusPenyediaAsset getStatusPenyediaAsset() {
		if (getId() != null && statusPenyediaAsset == null) {
			statusPenyediaAsset = StatusPenyediaAsset.PUSAT;
		} else {
			statusPenyediaAsset = check(statusPenyediaAsset);
		}

		return statusPenyediaAsset;
	}

	/**
	 * Menyetel kedudukan organisatoris vendor.
	 *
	 * <p>Dalam pemakaian normal setter ini tidak pernah dipanggil dari UI, sebab kontrol
	 * penyuntingnya pada form vendor sudah dikomentari. Menyetel nilai selain "Pusat"/"Cabang"
	 * tidak dilarang, tetapi juga tidak menghasilkan perilaku apa pun.</p>
	 *
	 * @param statusPenyediaAsset kedudukan organisatoris; {@code null} akan digantikan nilai bawaan
	 *                            pada pembacaan berikutnya bila vendor sudah tersimpan
	 */
	public void setStatusPenyediaAsset(StatusPenyediaAsset statusPenyediaAsset) {
		this.statusPenyediaAsset = statusPenyediaAsset;
	}

	/**
	 * Mengembalikan koordinat bujur lokasi vendor.
	 *
	 * <p>Disimpan sebagai teks, bukan angka, sehingga formatnya tidak divalidasi model — nilai
	 * dengan koma desimal, spasi, atau notasi derajat dapat tersimpan apa adanya. Berbeda dari
	 * sebagian besar getter teks di kelas ini, method ini <b>tidak</b> menormalkan {@code null}
	 * menjadi string kosong.</p>
	 *
	 * @return koordinat bujur, atau {@code null} bila belum diisi
	 */
	public String getLongitude() {
		return longitude;
	}

	/**
	 * Menyetel koordinat bujur lokasi vendor.
	 *
	 * @param longitude koordinat bujur sebagai teks; boleh {@code null}
	 */
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	/**
	 * Mengembalikan koordinat lintang lokasi vendor.
	 *
	 * <p>Sama seperti {@link #getLongitude()}, disimpan sebagai teks tanpa validasi format dan
	 * mempertahankan {@code null} apa adanya.</p>
	 *
	 * @return koordinat lintang, atau {@code null} bila belum diisi
	 */
	public String getLatitude() {
		return latitude;
	}

	/**
	 * Menyetel koordinat lintang lokasi vendor.
	 *
	 * @param latitude koordinat lintang sebagai teks; boleh {@code null}
	 */
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	/**
	 * Nilai bawaan untuk kolom bertipe JSON pada kelas ini: teks larik JSON kosong
	 * ({@code "[]"}).
	 *
	 * <p>Dikembalikan {@link #getBank()} dan {@link #getFormula()} ketika kolomnya {@code null}
	 * atau kosong, sehingga pemanggil selalu menerima JSON yang sah dan dapat langsung
	 * mem-<i>parse</i>-nya tanpa memeriksa {@code null}.</p>
	 * <p><b>Peringatan:</b> bidang ini {@code public static} namun <b>tidak</b> {@code final},
	 * sehingga secara teknis dapat ditugasi ulang dari mana saja dan akan memengaruhi seluruh
	 * instance vendor di JVM. Perlakukan sebagai konstanta dan jangan pernah menugasinya ulang.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan daftar rekening bank tambahan vendor dalam bentuk teks JSON.
	 *
	 * <p>Kolomnya bertipe {@code text} dan menyimpan larik JSON berisi objek-objek rekening.
	 * Layar-layar pengadaan mem-<i>parse</i>-nya lalu meratakan setiap kunci objek ke parameter
	 * laporan berpola {@code "bank_" + indeks + "." + kunci}, sehingga struktur JSON di sini
	 * langsung menentukan nama parameter yang tersedia bagi berkas templat laporan. Mengubah nama
	 * kunci pada JSON berarti mengubah nama parameter laporan.</p>
	 * <p>Bila kolom {@code null} atau kosong, method mengembalikan {@link #DEFAULT_FORMULA}, yaitu
	 * larik JSON kosong — bukan {@code null} dan bukan string kosong — sehingga pemanggil dapat
	 * mem-<i>parse</i>-nya dengan aman.</p>
	 * <p>Tidak ada validasi bahwa isi kolom benar-benar JSON yang sah; teks rusak baru terdeteksi
	 * saat pemanggil mencoba mem-<i>parse</i>-nya, dan layar pengadaan membungkus penguraian
	 * tersebut dalam {@code try/catch} yang menelan galat sehingga rekening tambahan sekadar tidak
	 * muncul di laporan.</p>
	 * <p>Jangan mengacaukannya dengan {@link #getBankUtama()}, yang merupakan relasi ke master
	 * {@link Bank} bagi rekening utama {@link #getNoRek()}.</p>
	 *
	 * @return teks JSON daftar rekening tambahan; tidak pernah {@code null} maupun kosong
	 */
	@Column(name = "bank", nullable = true, columnDefinition = "text")
	public String getBank() {
		return bank == null || bank.isEmpty() ? DEFAULT_FORMULA : bank;
	}

	/**
	 * Menyetel daftar rekening bank tambahan vendor dalam bentuk teks JSON.
	 *
	 * <p>Tidak ada validasi struktur maupun keabsahan JSON pada tingkat model; pemanggil
	 * bertanggung jawab menuliskan teks yang sah. Menyetel {@code null} atau string kosong membuat
	 * {@link #getBank()} membaca kembali {@link #DEFAULT_FORMULA}.</p>
	 *
	 * @param bank teks JSON daftar rekening; boleh {@code null}
	 */
	public void setBank(String bank) {
		this.bank = bank;
	}

	public String getNoAktaPendirian() {
		return noAktaPendirian;
	}

	public void setNoAktaPendirian(String noAktaPendirian) {
		this.noAktaPendirian = noAktaPendirian;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalAktaPendirian() {
		return tanggalAktaPendirian;
	}

	public void setTanggalAktaPendirian(Date tanggalAktaPendirian) {
		this.tanggalAktaPendirian = tanggalAktaPendirian;
	}

	public String getNamaNotaris() {
		return namaNotaris;
	}

	public void setNamaNotaris(String namaNotaris) {
		this.namaNotaris = namaNotaris;
	}

	public String getNoPengesahan() {
		return noPengesahan;
	}

	public void setNoPengesahan(String noPengesahan) {
		this.noPengesahan = noPengesahan;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalPengesahan() {
		return tanggalPengesahan;
	}

	public void setTanggalPengesahan(Date tanggalPengesahan) {
		this.tanggalPengesahan = tanggalPengesahan;
	}

	public String getNoAktaPendirianAkhir() {
		return noAktaPendirianAkhir;
	}

	public void setNoAktaPendirianAkhir(String noAktaPendirianAkhir) {
		this.noAktaPendirianAkhir = noAktaPendirianAkhir;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalAktaPendirianAkhir() {
		return tanggalAktaPendirianAkhir;
	}

	public void setTanggalAktaPendirianAkhir(Date tanggalAktaPendirianAkhir) {
		this.tanggalAktaPendirianAkhir = tanggalAktaPendirianAkhir;
	}

	public String getNamaNotarisAkhir() {
		return namaNotarisAkhir;
	}

	public void setNamaNotarisAkhir(String namaNotarisAkhir) {
		this.namaNotarisAkhir = namaNotarisAkhir;
	}

	public String getNoPengesahanAkhir() {
		return noPengesahanAkhir;
	}

	public void setNoPengesahanAkhir(String noPengesahanAkhir) {
		this.noPengesahanAkhir = noPengesahanAkhir;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalPengesahanAkhir() {
		return tanggalPengesahanAkhir;
	}

	public void setTanggalPengesahanAkhir(Date tanggalPengesahanAkhir) {
		this.tanggalPengesahanAkhir = tanggalPengesahanAkhir;
	}

	public String getNpwp() {
		return npwp;
	}

	public void setNpwp(String npwp) {
		this.npwp = npwp;
	}

	public void tampilkanEmail(Component vbox) {
		String email = getEmail();
		Toolbarbutton a;
		(a = new ais.ui.util.MyToolbarbuttonConfig(email)).setParent(vbox);
		if (email != null && !email.trim().isEmpty()) {
			a.setImage("/img/svg/mail-send-line.svg");
			a.setStyle("font-size:9px;");
			a.setTarget("_blank");
			a.setHref("mailto:" + email);
		}

	}

	public void tampilkanHp(Component vbox) {
		try {

			String hp = getTelp();
			String telp = getTelp();

			Toolbarbutton a;
			(a = new ais.ui.util.MyToolbarbuttonConfig(
					(hp == null || hp.toString().trim().equals("08100000000000000000")
							|| hp.toString().trim().equals("0000000000") ? "" : hp)
							+ (telp == null || telp.toString().trim().isEmpty()
									|| telp.toString().trim().equals("00000000000000000000")
									|| telp.toString().trim().equals("000000000")
											? ""
											: (hp == null || hp.toString().trim().isEmpty()
													|| hp.toString().trim().equals("08100000000000000000")
													|| hp.toString().trim().equals("0000000000") ? "" : " / ") + telp)))
					.setParent(vbox);

			if (telp != null && !telp.trim().isEmpty() && hp != null && hp.equals(telp)) {
				a.setLabel(hp);
			}

			if (hp == null || hp.toString().trim().isEmpty() || hp.toString().trim().equals("08100000000000000000")
					|| hp.toString().trim().equals("0000000000")) {
				hp = telp;
			}

			if (hp != null && !hp.trim().isEmpty()
					&& !(hp == null || hp.toString().trim().isEmpty()
							|| hp.toString().trim().equals("00000000000000000000")
							|| hp.toString().trim().equals("000000000"))) {
				hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
				hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
				hp = !hp.startsWith("+") ? "+62" + hp : hp;
				a.setStyle("font-size:9px;");
				a.setImage("/img/svg/whats.svg");
				a.setTarget("_blank");
				a.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text=Halo,+apa+kabar?");
			}
		} catch (Exception e) {
			A a;
			String hp = getTelp();
			(a = new A(hp)).setParent(vbox);
			if (hp != null && !hp.trim().isEmpty()
					&& !(hp == null || hp.toString().trim().isEmpty()
							|| hp.toString().trim().equals("00000000000000000000")
							|| hp.toString().trim().equals("000000000"))) {
				hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
				hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
				hp = !hp.startsWith("+") ? "+62" + hp : hp;
				a.setStyle("font-size:9px;");
				a.setImage("/img/svg/whats.svg");
				a.setTarget("_blank");
				a.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text=Halo,+apa+kabar?");
			}
		}
	}

	public String getNoRek() {
		return noRek;
	}

	public void setNoRek(String noRek) {
		this.noRek = noRek;
	}

	public String getAtasNama() {
		if (atasNama == null || atasNama.isEmpty()) {
			atasNama = getNama();
		}
		return atasNama;
	}

	public void setAtasNama(String atasNama) {
		this.atasNama = atasNama;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_utama_id", nullable = true)
	public Bank getBankUtama() {
		bankUtama = check(bankUtama);
		return bankUtama;
	}

	public void setBankUtama(Bank bankUtama) {
		this.bankUtama = bankUtama;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_utang", nullable = true)
	public Akun getAkunUtang() {
		akunUtang = check(akunUtang);

		if (akunUtang == null) {
			try {
				String akunUtangIdDefaultD = Common.getKonfigurasi("akun_utang_id_default_data", "").getNilai().trim();
				if (!akunUtangIdDefaultD.isEmpty()) {
					akunUtang = (Akun) ConstantValues.ambil(Akun.class.getName(), Long.parseLong(akunUtangIdDefaultD));
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/asset/PenyediaAsset.java:647");
				// TODO: handle exception
			}
		}

		return akunUtang;
	}

	public void setAkunUtang(Akun akunUtang) {
		this.akunUtang = akunUtang;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putPhoto(Map parameters) {
		try {
			PenyediaAsset penyediaAsset = this;
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

			List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq("ref", penyediaAsset.getId()))
					.add(Restrictions.ilike("jenis", "Galery_PenyediaAsset_", MatchMode.START)).list();

			LampiranLain fotopenyediaAsset = lampiranLains.isEmpty() ? null : lampiranLains.get(0);

			if (fotopenyediaAsset != null && fotopenyediaAsset.ambilFile() != null) {
				parameters.put("foto", fotopenyediaAsset.ambilFile().getAbsolutePath());
			} else

			if (fotopenyediaAsset != null && fotopenyediaAsset.dropboxLinkRaw() != null
					&& !fotopenyediaAsset.dropboxLinkRaw().trim().isEmpty()) {
				parameters.put("foto", fotopenyediaAsset.dropboxLinkRaw());
			} else if (fotopenyediaAsset != null && fotopenyediaAsset.getGdrive() != null
					&& !fotopenyediaAsset.getGdrive().trim().isEmpty()) {
				parameters.put("foto", fotopenyediaAsset.exportGDriveUrl());
			} else if (fotopenyediaAsset != null) {
				parameters.put("foto", fotopenyediaAsset.createLinkUri());
			} else {
				File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
				parameters.put("foto", file.getAbsolutePath());
			}

			int size = 0;
			for (LampiranLain lampiranLain : lampiranLains) {
				size++;

				if (lampiranLain != null && lampiranLain.dropboxLinkRaw() != null
						&& !lampiranLain.dropboxLinkRaw().trim().isEmpty()) {
					parameters.put("foto_" + size, lampiranLain.dropboxLinkRaw());
				} else if (lampiranLain != null && lampiranLain.getGdrive() != null
						&& !lampiranLain.getGdrive().trim().isEmpty()) {
					parameters.put("foto_" + size, lampiranLain.exportGDriveUrl());
				} else if (lampiranLain != null) {
					parameters.put("foto_" + size, lampiranLain.createLinkUri());
				} else {
					File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
					parameters.put("foto_" + size, file.getAbsolutePath());
				}

			}

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/asset/PenyediaAsset.java:710");
		}
	}

	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}
		return dibuatOleh;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}
		return disetujuiOleh;
	}

	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/PenyediaAsset.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_penyedia_1", nullable = true)
	public JenisPekerjaanPenyedia getJenisPekerjaanPenyedia1() {
		jenisPekerjaanPenyedia1 = check(jenisPekerjaanPenyedia1);
		return jenisPekerjaanPenyedia1;
	}

	public void setJenisPekerjaanPenyedia1(JenisPekerjaanPenyedia jenisPekerjaanPenyedia1) {
		this.jenisPekerjaanPenyedia1 = jenisPekerjaanPenyedia1;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_penyedia_2", nullable = true)
	public JenisPekerjaanPenyedia getJenisPekerjaanPenyedia2() {
		jenisPekerjaanPenyedia2 = check(jenisPekerjaanPenyedia2);
		return jenisPekerjaanPenyedia2;
	}

	public void setJenisPekerjaanPenyedia2(JenisPekerjaanPenyedia jenisPekerjaanPenyedia2) {
		this.jenisPekerjaanPenyedia2 = jenisPekerjaanPenyedia2;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_penyedia_3", nullable = true)
	public JenisPekerjaanPenyedia getJenisPekerjaanPenyedia3() {
		jenisPekerjaanPenyedia3 = check(jenisPekerjaanPenyedia3);
		return jenisPekerjaanPenyedia3;
	}

	public void setJenisPekerjaanPenyedia3(JenisPekerjaanPenyedia jenisPekerjaanPenyedia3) {
		this.jenisPekerjaanPenyedia3 = jenisPekerjaanPenyedia3;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_penyedia_4", nullable = true)
	public JenisPekerjaanPenyedia getJenisPekerjaanPenyedia4() {
		jenisPekerjaanPenyedia4 = check(jenisPekerjaanPenyedia4);
		return jenisPekerjaanPenyedia4;
	}

	public void setJenisPekerjaanPenyedia4(JenisPekerjaanPenyedia jenisPekerjaanPenyedia4) {
		this.jenisPekerjaanPenyedia4 = jenisPekerjaanPenyedia4;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_penyedia_5", nullable = true)
	public JenisPekerjaanPenyedia getJenisPekerjaanPenyedia5() {
		jenisPekerjaanPenyedia5 = check(jenisPekerjaanPenyedia5);
		return jenisPekerjaanPenyedia5;
	}

	public void setJenisPekerjaanPenyedia5(JenisPekerjaanPenyedia jenisPekerjaanPenyedia5) {
		this.jenisPekerjaanPenyedia5 = jenisPekerjaanPenyedia5;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PENGAJUAN_PENYEDIA;
		} else {
			nomorSuratAlurPengadaan = check(nomorSuratAlurPengadaan);
		}
		return nomorSuratAlurPengadaan;
	}

	public void setNomorSuratAlurPengadaan(NomorSuratAlurPengadaan nomorSuratAlurPengadaan) {
		this.nomorSuratAlurPengadaan = nomorSuratAlurPengadaan;
	}

	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	public String getPemilik() {
		return pemilik;
	}

	public void setPemilik(String pemilik) {
		this.pemilik = pemilik;
	}

	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? DEFAULT_FORMULA : formula;
	}

	public void setFormula(String formula) {
		this.formula = formula;
	}
}
