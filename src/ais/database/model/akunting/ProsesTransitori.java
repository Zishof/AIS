package ais.database.model.akunting;

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

import ais.database.model.Tbmuser;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Dokumen <b>header</b> (batch) untuk langkah <b>KEDUA</b> jalur pencairan dana bertahap
 * "transitori": pelepasan dana yang sedang mampir di <i>rekening/akun perantara</i> menuju akun
 * tujuan akhirnya. Dipetakan ke tabel <code>akunting.proses_transitori</code>.
 *
 * <h2>Apa arti "transitori" di sini &mdash; TERVERIFIKASI dari mesin postingnya</h2>
 *
 * <p>Istilah ini bukan sekadar label. Maknanya dapat dibaca langsung dari dua mesin posting yang
 * menyusun jurnalnya, dan keduanya memakai kolom yang sama,
 * {@code CaraPembayaranTransfer.akunTransitori}:</p>
 *
 * <ol>
 *   <li><b>Langkah pertama &mdash; dana MASUK ke akun perantara.</b> Saat sebuah
 *       {@code DaftarPengajuanTransfer} yang ditandai transitori
 *       ({@code DaftarPengajuanTransfer.getTransitori() == true}) diposting oleh
 *       {@code PostingProsesTransferAction}, akun DEBET-nya <b>ditukar</b>: bukan lagi akun tujuan
 *       baris pengajuan ({@code dpt.getAkun()}), melainkan
 *       {@code prosesTransfer.getCaraPembayaranTransfer().getAkunTransitori()}. Akun KREDIT tetap
 *       rekening bank sumber ({@code caraPembayaranTransfer.getAkun()}). Jurnalnya:
 *       <code>Dr Akun Transitori / Cr Bank</code> &mdash; uang sudah keluar dari bank, tetapi
 *       <b>atribusi akhirnya belum diakui</b>; dana "parkir" di akun perantara.</li>
 *   <li><b>Langkah kedua &mdash; dana KELUAR dari akun perantara.</b> Itulah dokumen ini.
 *       {@code PostingProsesTransitoriAction} membalik arahnya:
 *       DEBET = {@code dpt.getAkun()} (akun tujuan akhir yang sebenarnya), KREDIT =
 *       {@code caraPembayaranTransfer.getAkunTransitori()}. Jurnalnya:
 *       <code>Dr Akun Tujuan / Cr Akun Transitori</code> &mdash; saldo akun perantara dinolkan
 *       kembali dan bebannya berpindah ke akun yang semestinya.</li>
 * </ol>
 *
 * <p>Jadi "transitori" di modul ini berarti persis: <b>akun antara/penampungan sementara yang
 * disinggahi dana antara saat uang meninggalkan rekening bank dan saat pengakuan akun tujuannya
 * dilakukan</b>. Selama kedua langkah belum lengkap, saldo akun transitori bukan nol &mdash; itulah
 * ukuran dana yang sedang "menggantung". Bila langkah kedua tidak pernah dijalankan, dana tersebut
 * <b>tersangkut di neraca selamanya</b> pada akun perantara, dan laporan per akun tujuan
 * mengecilkan realisasinya.</p>
 *
 * <h2>Struktur dokumen: header, baris, dan asal barisnya</h2>
 *
 * <p>Entity ini adalah <b>header/wadah batch</b>, bukan pembawa nominal per baris. Barisnya adalah
 * {@link ais.database.model.akunting.Transitori} &mdash; relasi ditegakkan dari SISI ANAK
 * ({@code Transitori.prosesTransitori}, kolom {@code akunting.transitori.proses_transitori}); kelas
 * ini <b>tidak</b> memiliki koleksi balik, sehingga isi batch selalu dibaca lewat query terhadap
 * {@code Transitori}. Setiap baris {@code Transitori} sendiri menunjuk tepat satu
 * {@code DaftarPengajuanTransfer} (kolom {@code daftar_pengajuan_transfer_id}, {@code unique})
 * &mdash; yaitu baris pengajuan transfer yang sudah dicairkan pada langkah pertama. Dari sanalah
 * mesin posting mengambil nominal ({@code dpt.getNominal()}), akun tujuan ({@code dpt.getAkun()}),
 * dan &mdash; lewat {@code dpt.getProsesTransfer().getCaraPembayaranTransfer()} &mdash; akun
 * transitorinya. Cap posting per baris juga tersimpan di anak
 * ({@code Transitori.postingHistory}), bukan di header ini.</p>
 *
 * <p>Rantai lengkapnya: <code>DaftarPengajuanTransfer &rarr; ProsesTransfer (disetujui &rarr;
 * direalisasikan &rarr; diposting: Dr Transitori/Cr Bank) &rarr; Transitori &rarr;
 * ProsesTransitori (disetujui &rarr; diposting: Dr Tujuan/Cr Transitori)</code>.</p>
 *
 * <h2>Isi kolom</h2>
 *
 * <ul>
 *   <li><b>Identitas &amp; judul:</b> {@code id}, {@code nama} (judul batch, wajib),
 *       {@code keterangan}.</li>
 *   <li><b>Nominal ringkas:</b> {@code nilai} &mdash; total batch, <b>hasil hitungan ulang</b> oleh
 *       layar penyimpan, bukan angka yang dijurnal. Yang dijurnal selalu
 *       {@code DaftarPengajuanTransfer.nominal} per baris.</li>
 *   <li><b>Status:</b> {@code aktif} (bendera lunak, dipengaruhi disposisi SOP).</li>
 *   <li><b>Persetujuan:</b> {@code disetujuiOleh} + {@code tanggalPersetujuan}. Inilah
 *       <b>gerbang posting yang sesungguhnya</b>: {@code kriteriaPostingStatic} mensyaratkan
 *       {@code prosesTransitori.disetujuiOleh IS NOT NULL} sebelum satu baris pun boleh dijurnal,
 *       dan rentang tanggal dasbor posting difilter pada
 *       {@code prosesTransitori.tanggalPersetujuan}.</li>
 *   <li><b>Alur SOP:</b> {@code disposisiSop} &mdash; menautkan batch ke mesin disposisi
 *       {@link ais.database.model.sop.DisposisiSop}; beberapa getter di bawah membaca status
 *       disposisi ini dan <b>menimpa</b> nilai kolom lokalnya.</li>
 *   <li><b>Jejak audit ringan:</b> {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, ditambah
 *       {@code @Audited} (Hibernate Envers) yang menggandakan setiap versi baris ke tabel revisi
 *       {@code proses_transitori_aud}.</li>
 *   <li><b>Turunan:</b> {@code kodeUnik} &mdash; dihitung ulang setiap kali dibaca (lihat
 *       {@link #getKodeUnik()}).</li>
 * </ul>
 *
 * <h2>Pewarisan: {@code ProsesTransitori extends DataSop extends GeneralValueObject}</h2>
 *
 * <p>{@link ais.database.model.sop.DataSop} hanya menetapkan kontrak
 * {@code getDisposisiSop()}/{@code setDisposisiSop()}; seluruh perilaku umum (resolusi proxy
 * {@code check(...)}, {@code toString()} bawaan, {@code compareTo}, dsb.) berasal dari
 * {@link ais.database.model.GeneralValueObject}.</p>
 *
 * <p><b>PENTING &mdash; {@code GeneralValueObject} BUKAN {@code @Entity} maupun
 * {@code @MappedSuperclass}</b>, melainkan POJO abstrak biasa. Hibernate karena itu <b>tidak
 * memetakan satu pun properti induknya</b>. Konsekuensinya: setiap kolom yang ingin dipersistensi
 * <b>harus dideklarasikan ulang</b> di kelas entity. Field yang tampak "duplikat" dengan induknya
 * <b>bukan bug, melainkan keharusan teknis</b>. Lihat
 * {@link ais.database.model.GeneralValueObject} untuk uraian lengkap mekanisme ini.</p>
 *
 * <p><b>Kuirk yang lahir dari aturan di atas (khas kelas ini).</b> Seluruh saudara sekeluarga
 * dokumen SOP &mdash; {@code UangMuka}, {@code KasBesar}, {@code KasKecil}, {@code DanaTalangan},
 * {@code ProsesTransfer}, {@code Pertangungjawaban}, dan belasan dokumen aset &mdash; mendeklarasikan
 * ulang {@code private String kode;} beserta {@code getKode()}-nya. <b>Kelas ini tidak.</b>
 * Akibatnya {@code getKode()} yang terpakai adalah milik {@code GeneralValueObject}, yang membaca
 * field <i>transient</i> tak pernah terisi dan tak pernah tersimpan &mdash; jadi <b>selalu bernilai
 * {@code null}</b> untuk {@code ProsesTransitori}. Efek berantainya:
 * ({@link #getKodeUnik()} selalu menghasilkan awalan literal {@code "null"};
 * {@code toString()} yang di-override di bawah memang tidak memakainya, tetapi ekspor Excel layar
 * daftar mengekspor kolom {@code kodeUnik} apa adanya; dan filter ZUL berlabel "Judul/Kode Proses
 * Transitori" pada praktiknya hanya mencari pada {@code nama}). Menambahkan kolom {@code kode}
 * berarti perubahan skema, jadi jangan "dirapikan" tanpa migrasi.</p>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ol>
 *   <li><b>Jejak audit ringan:</b> {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas &amp; teks:</b> {@link #getId()}, {@link #getNama()}, {@link #getKeterangan()},
 *       {@link #toString()}, {@link #getKodeUnik()}.</li>
 *   <li><b>Nominal &amp; status:</b> {@link #getNilai()}, {@link #getAktif()}.</li>
 *   <li><b>Alur SOP &amp; persetujuan (getter berlogika, sebagian DESTRUKTIF):</b>
 *       {@link #getDisposisiSop()}, {@link #getDisetujuiOleh()},
 *       {@link #getTanggalPersetujuan()}, {@link #getTanggalPembuatan()}.</li>
 *   <li><b>Setter:</b> mayoritas trivial; tiga di antaranya ({@link #setOleh(String)},
 *       {@link #setOlehId(String)}, {@link #setDisposisiSop(DisposisiSop)}) <b>menolak diam-diam</b>
 *       nilai kosong/null (lihat masing-masing).</li>
 * </ol>
 *
 * <h2>Hal non-obvious yang wajib diketahui sebelum menyunting</h2>
 *
 * <ol>
 *   <li><b>Akses properti + {@code dynamicUpdate}: getter di kelas ini ikut MENULIS.</b> Anotasi
 *       pemetaan dipasang pada getter, sehingga Hibernate memakai <i>property access</i> dan
 *       memanggil getter-getter ini saat pemeriksaan <i>dirty</i> maupun saat <i>flush</i>. Getter
 *       yang menugaskan kembali ke field-nya ({@link #getAktif()}, {@link #getKodeUnik()},
 *       {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
 *       {@link #getTanggalPembuatan()}) karena itu dapat menerbitkan {@code UPDATE} <b>hanya karena
 *       entity-nya dibaca</b> di dalam sesi yang masih hidup. Ini bukan teori: dua di antaranya
 *       menyentuh kolom <b>persetujuan</b> &mdash; kolom yang menjadi gerbang posting jurnal.</li>
 *   <li><b>Tidak ada kolom tenant maupun satuan kerja.</b> Baik {@code ProsesTransitori} maupun
 *       {@code Transitori} tidak menyimpan sekolah/yayasan/satuan kerja. Query daftar
 *       ({@code ProsesTransitoriAction#criteria}) dan query posting
 *       ({@code PostingProsesTransitoriAction#kriteriaPostingStatic}) memang <b>tidak memfilter
 *       tenant sama sekali</b> &mdash; bukan fail-open kondisional, melainkan tidak ada kolomnya
 *       untuk difilter. Satuan kerja jurnal diambil dari satuan kerja PENGGUNA yang login, dengan
 *       cadangan satuan kerja pengajuan transfernya. Semua ini pola yang sama dengan temuan
 *       {@code task_f1283f4a}.</li>
 *   <li><b>Nominal header vs nominal yang dijurnal bisa berbeda.</b> {@link #getNilai()} diisi
 *       {@code ProsesTransitoriAction#onSave} sebagai jumlah baris yang <b>tercentang saat menyimpan
 *       saja</b>, sedangkan pelepasan centang <b>tidak</b> melepas tautan barisnya
 *       ({@code Transitori.prosesTransitori} tetap terisi). Baris yang dilepas centangnya karena itu
 *       hilang dari total header tetapi <b>tetap ikut dijurnal</b> oleh mesin posting. Pelepasan
 *       baris yang benar-benar memutus tautan hanya tersedia lewat REST
 *       ({@code ProsesTransitoriApiHelper#lepasItem}). Varian dari {@code task_a594425b}.</li>
 *   <li><b>Idempotensi posting bertumpu pada satu kolom di anak.</b> Yang mencegah satu baris
 *       dijurnal dua kali adalah {@code Transitori.postingHistory IS NULL} pada
 *       {@code kriteriaPostingStatic}, bukan penjaga di header ini. Pembatalan posting mengosongkan
 *       kolom tersebut, sehingga baris kembali menjadi kandidat posting.</li>
 *   <li><b>{@code Transitori.getTransfer()} selalu {@code true}</b> (logika aslinya dikomentari di
 *       entity anak). Kriteria {@code Restrictions.eq("transfer", true)} pada mesin posting karena
 *       itu selalu terpenuhi dan <b>bukan gerbang</b>. Jangan menyimpulkan sebaliknya dari nama
 *       kolomnya.</li>
 * </ol>
 *
 * <h2>Siapa yang memakai entity ini (terverifikasi lewat pencarian repo)</h2>
 *
 * <ul>
 *   <li>{@code ais.action.master.akunting.ProsesTransitoriAction} &mdash; layar ZK CRUD +
 *       persetujuan batch (menu {@code proses_transitori}). Tombol Setujui/Batalkan Persetujuan
 *       DIGERBANGI hak {@code APPROVE}/{@code REJECT}; Tambah/Ubah/Hapus digerbangi
 *       {@code CREATE}/{@code UPDATE}/{@code DELETE}.</li>
 *   <li>{@code ais.action.master.akunting.PostingProsesTransitoriAction} &mdash; layar posting
 *       jurnal per baris + {@code postingSemua}/{@code batalkanPostingSemua} massal.</li>
 *   <li>{@code ais.action.master.akunting.TransitoriAction} &mdash; layar baris
 *       {@code Transitori}.</li>
 *   <li>{@code ais.action.servlet.api.ProsesTransitoriApiHelper} (lewat {@code PosApi}, awalan aksi
 *       {@code proses_transitori_}) &mdash; jalur REST/POS untuk daftar, simpan, hapus, setujui,
 *       batal setuju, dan lepas baris.</li>
 *   <li>{@code ais.action.servlet.api.DraftJurnalApiHelper} &mdash; baris dasbor "Transitori",
 *       memanggil posting/batal-posting massal.</li>
 *   <li>{@code ais.common.EbisnisMenuKatalog} &mdash; pendaftaran kunci menu
 *       {@code proses_transitori} pada grup "Keuangan"; kunci ini ada di
 *       {@code KUNCI_DEFAULT_NONAKTIF} (niat <i>fail-closed</i>).</li>
 * </ul>
 *
 * <h2>Catatan otorisasi &amp; integritas (hasil verifikasi, bukan dugaan)</h2>
 *
 * <p><b>Verifikasi NEGATIF yang menenangkan.</b> Berbeda dari {@code ProsesTransfer} (langkah
 * pertama rantai yang sama), tiga cacat yang ditemukan di sana <b>TIDAK</b> terulang di sini:
 * (a) {@code PosApi} <b>punya</b> cabang untuk awalan {@code proses_transitori_}, sehingga
 * pendaftarannya di {@code KUNCI_DEFAULT_NONAKTIF} benar-benar tereksekusi; (b) semua tombol aksi
 * layar ZK &mdash; termasuk Setujui dan Batalkan Persetujuan &mdash; <b>punya</b> gerbang hak;
 * (c) konstruktor {@code ProsesTransitoriAction(boolean persetujuan)} <b>tidak pernah dipanggil dari
 * mana pun</b> dan {@code persetujuan} tidak dibaca dari parameter URL, sehingga pola bypass
 * {@code ?persetujuan=true} ({@code task_78c0c5c2}) <b>tidak berlaku</b> untuk dokumen ini &mdash;
 * baris "Status Persetujuan" pada formulirnya bahkan tidak pernah terlihat.</p>
 *
 * <p><b>Yang TETAP bermasalah.</b> Gerbang jalur REST-nya mengulang pola
 * {@code task_66986071}: {@code ProsesTransitoriApiHelper.bolehAksi()} mengembalikan {@code true}
 * ketika {@code tbmuser.hakAkses()} bernilai {@code null} &mdash; pengguna tanpa peran terbaca
 * diberi izin PENUH, termasuk <b>approve</b> dan <b>reject</b> batch pelepasan dana ini. Pola
 * identik ada pada {@code DraftJurnalApiHelper.bolehAksi()}, yang menggerbangi posting dan
 * <b>pembatalan posting massal</b> baris Transitori. Karena persetujuan batch inilah satu-satunya
 * gerbang keadaan sebelum jurnal terbit, kombinasi itu berarti akun tanpa peran dapat menyetujui
 * sendiri lalu memposting sendiri seluruh pelepasan dana pada rentang tanggal pilihannya. Tidak ada
 * pemisahan tugas di level dokumen: pembuat batch boleh menjadi penyetujunya sendiri, baik lewat ZK
 * maupun REST.</p>
 *
 * <p><b>Risiko dana "menggantung" di akun perantara.</b> Karena {@link #getDisetujuiOleh()} dan
 * {@link #getTanggalPersetujuan()} dapat <b>mengosongkan</b> kolom persetujuan hanya karena dibaca
 * (lihat masing-masing), sebuah batch yang sudah disetujui &mdash; bahkan yang barisnya sudah
 * dijurnal &mdash; bisa kehilangan status persetujuannya secara senyap. Setelah itu batch tersebut
 * <b>tidak lagi cocok</b> dengan {@code kriteriaPostingStatic}, sehingga barisnya hilang dari layar
 * posting <b>maupun</b> layar batal-posting: jurnalnya tetap ada di buku besar, tetapi tidak ada
 * lagi jalan membatalkannya dari UI. Bila hal itu terjadi sebelum posting, dananya berhenti di
 * langkah pertama dan saldo akun transitori tidak pernah dinolkan.</p>
 *
 * @see ais.database.model.akunting.Transitori
 * @see ais.database.model.akunting.CaraPembayaranTransfer
 * @see ais.database.model.akunting.DaftarPengajuanTransfer
 * @see ais.database.model.akunting.ProsesTransfer
 * @see ais.database.model.sop.DataSop
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "proses_transitori")
public class ProsesTransitori extends DataSop {

	/**
	 * Versi serialisasi Java. Nilainya <b>identik</b> dengan
	 * {@link ais.database.model.akunting.Transitori} &mdash; jejak khas bahwa kedua kelas lahir dari
	 * satu operasi salin-tempel generator hbm2java pada 2010. Jangan diubah: entity ini dipertukarkan
	 * lewat sesi ZK dan cache.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key tabel {@code akunting.proses_transitori}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/**
	 * Nama pengguna terakhir yang menyentuh baris ini, diisi oleh
	 * {@code AuditTimestampInterceptor}. Jejak audit ringan, bukan kolom bisnis.
	 */
	private String oleh;

	/** Id pengguna terakhir yang menyentuh baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} maupun string kosong/hanya spasi <b>ditolak diam-diam</b>
	 * (method langsung {@code return} tanpa menugaskan apa pun). Jejak audit lama karena itu tidak
	 * pernah bisa dihapus lewat setter ini &mdash; disengaja, supaya interceptor audit tidak
	 * menimpanya dengan nilai kosong.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai null/kosong
	 * <b>ditolak diam-diam</b>.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyerahkan pengisian jejak audit
	 * ({@link #oleh}, {@link #olehId}, {@link #tanggal_dirubah}) kepada
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum {@code UPDATE} dijalankan.
	 *
	 * <p>Dipanggil oleh Hibernate, tidak pernah oleh kode aplikasi. Perhatikan bahwa callback ini
	 * ikut menyala pada {@code UPDATE} yang lahir dari getter destruktif kelas ini &mdash; artinya
	 * jejak audit bisa tercatat untuk perubahan yang tidak pernah diminta pengguna.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object dan
	 * diperbarui oleh {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks batch: {@code "<id>-<nama>"}.
	 *
	 * <p><b>Non-obvious:</b> meng-override {@code toString()} bawaan
	 * {@link ais.database.model.GeneralValueObject} yang berformat {@code "kode - nama"}. Override
	 * ini kebetulan menyelamatkan tampilannya, karena {@code getKode()} pada kelas ini selalu
	 * {@code null} (lihat catatan kelas). Method membaca field {@link #nama} secara langsung, bukan
	 * lewat {@link #getNama()}, jadi spasi di ujung nama tidak dipangkas di sini.</p>
	 *
	 * @return gabungan id dan nama batch
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Judul batch transitori, wajib diisi (divalidasi di layar, bukan di entity). */
	private String nama;

	/** Keterangan bebas untuk batch. */
	private String keterangan;

	/**
	 * Total nominal batch. <b>Angka ringkas untuk tampilan/ekspor</b>, dihitung ulang oleh layar
	 * penyimpan; nominal yang benar-benar dijurnal selalu diambil dari
	 * {@code DaftarPengajuanTransfer.nominal} milik masing-masing baris.
	 */
	private Double nilai;

	/** Bendera aktif; dapat dipaksa {@code false} oleh status disposisi SOP (lihat {@link #getAktif()}). */
	private Boolean aktif;

	/** Tautan ke alur disposisi SOP yang menaungi batch ini; opsional. */
	private DisposisiSop disposisiSop;

	/** Tanggal pengajuan batch; dapat ditimpa oleh waktu mulai disposisi SOP (lihat {@link #getTanggalPembuatan()}). */
	private Date tanggalPembuatan;

	/** Pengguna yang menyetujui batch &mdash; gerbang keadaan sebelum barisnya boleh dijurnal. */
	private Tbmuser disetujuiOleh;

	/** Waktu persetujuan; dipakai sebagai tanggal filter rentang pada dasbor posting. */
	private Date tanggalPersetujuan;

	/**
	 * Constructor default tanpa argumen. WAJIB ada karena Hibernate membutuhkannya untuk membuat
	 * instance saat hidrasi entity dari hasil query, dan dipakai layar ZK saat menekan "Tambah".
	 */
	public ProsesTransitori() {
	}

	/**
	 * Mengembalikan primary key batch.
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key batch. Dipakai Hibernate saat hidrasi dan oleh kode yang membuat object
	 * penunjuk berisi id saja.
	 *
	 * @param id nilai primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan judul batch, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * @return judul batch tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel judul batch.
	 *
	 * @param nama judul batch
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan batch apa adanya (tanpa pemangkasan).
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan batch.
	 *
	 * @param keterangan teks bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif batch, <b>setelah diselaraskan dengan status disposisi SOP-nya</b>.
	 *
	 * <p><b>Cara kerja.</b> Method memanggil {@link #getDisposisiSop()} (yang me-resolve proxy lazy),
	 * lalu memaksa {@code aktif = false} pada dua keadaan:</p>
	 * <ol>
	 *   <li>disposisinya sendiri sudah tidak aktif ({@code disposisiSop.getAktif() == false}); atau</li>
	 *   <li>alur SOP-nya berhenti di simpul penolakan
	 *       ({@code disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()}).</li>
	 * </ol>
	 * <p>Bila {@code aktif} bernilai {@code null} (baris lama sebelum kolom ini terisi), hasilnya
	 * {@code true} &mdash; <b>fail-open</b> yang disengaja supaya data lama tetap tampil.</p>
	 *
	 * <p><b>Efek samping (DESTRUKTIF).</b> Method menugaskan hasilnya kembali ke field
	 * {@link #aktif} <i>dan</i> ke field {@link #disposisiSop}. Karena pemetaan kelas ini memakai
	 * <i>property access</i> dengan {@code dynamicUpdate}, sekadar <b>membaca</b> status batch di
	 * dalam sesi Hibernate yang masih hidup dapat menerbitkan {@code UPDATE} yang menonaktifkan
	 * baris secara permanen. Penonaktifan ini tidak dapat dikembalikan lewat getter; hanya
	 * {@link #setAktif(Boolean)} yang bisa.</p>
	 *
	 * <p><b>Kasus tepi.</b> Rantai {@code getDisposisiEnd().getAlurSop()} diperiksa null satu per
	 * satu, tetapi {@code disposisiSop.getAktif()} pada syarat pertama dipanggil tanpa penjaga
	 * &mdash; disposisi dengan kolom {@code aktif} bernilai {@code null} akan melempar
	 * {@code NullPointerException} saat <i>auto-unboxing</i>.</p>
	 *
	 * @return {@code true} bila batch masih dianggap aktif; tidak pernah {@code null}
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
	 * Menyetel bendera aktif batch secara langsung, melewati penyelarasan dengan disposisi SOP.
	 *
	 * @param aktif status aktif baru (boleh {@code null}, yang dibaca sebagai "aktif")
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan total nominal batch, dengan {@code null} disubstitusi menjadi {@code 0.0}.
	 *
	 * <p><b>Penting:</b> angka ini <b>bukan</b> yang dijurnal. Mesin posting selalu memakai
	 * {@code DaftarPengajuanTransfer.nominal} per baris. Nilai di sini hanya ringkasan tampilan/ekspor
	 * yang dihitung ulang oleh {@code ProsesTransitoriAction#onSave} dari baris-baris yang
	 * <b>tercentang saat itu</b>; baris yang dilepas centangnya tetap tertaut ke batch dan tetap akan
	 * dijurnal, sehingga total ini dapat lebih kecil daripada jumlah yang benar-benar dibukukan.</p>
	 *
	 * <p>Substitusi {@code null &rarr; 0.0} dilakukan tanpa menulis balik ke field, jadi getter ini
	 * <b>tidak</b> destruktif.</p>
	 *
	 * @return total nominal batch; {@code 0.0} bila belum pernah dihitung
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel total nominal batch.
	 *
	 * @param nilai total nominal hasil hitungan pemanggil
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/** Kode unik turunan; selalu dihitung ulang oleh {@link #getKodeUnik()}, tidak pernah dipakai apa adanya. */
	private String kodeUnik;

	/**
	 * Menghitung ulang lalu mengembalikan kode unik batch dengan rumus
	 * {@code getKode() + ("_" + disposisiSop.id)} bila ada disposisi, atau
	 * {@code getKode() + ("_" + id)} bila tidak.
	 *
	 * <p><b>Kuirk kelas ini.</b> {@code getKode()} yang terpanggil adalah milik
	 * {@link ais.database.model.GeneralValueObject} dan membaca field <i>transient</i> yang tidak
	 * pernah diisi maupun dipersistensi &mdash; sebab, tidak seperti semua saudara dokumen SOP-nya,
	 * {@code ProsesTransitori} <b>tidak mendeklarasikan ulang</b> kolom {@code kode}. Hasilnya karena
	 * itu <b>selalu berawalan literal {@code "null"}</b>, misalnya {@code "null_1234"}. Nilai ini
	 * ikut terekspor ke Excel dari layar daftar.</p>
	 *
	 * <p><b>Efek samping (DESTRUKTIF) dan kasus tepi.</b> Method menugaskan hasilnya ke field
	 * {@link #kodeUnik}, sehingga setiap pembacaan berpotensi menerbitkan {@code UPDATE}. Lebih
	 * jauh, karena {@code id} dibangkitkan basis data (IDENTITY) dan belum ada saat {@code INSERT}
	 * disusun, baris baru <b>tanpa disposisi SOP</b> selalu menulis nilai yang sama persis
	 * ({@code "null_null"}) pada kolom yang dianotasi {@code unique = true}. Bila indeks unik itu
	 * benar-benar terpasang di skema fisik, penyimpanan batch kedua akan ditolak basis data sampai
	 * baris pertama tersentuh {@code UPDATE} berikutnya (yang barulah mengisi id sebenarnya).
	 * Jangan "memperbaiki" ini dengan menambah kolom {@code kode} tanpa migrasi skema.</p>
	 *
	 * @return kode unik hasil hitungan ulang; tidak pernah {@code null}
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menyetel kode unik.
	 *
	 * <p><b>Non-obvious:</b> nilai yang disetel di sini praktis tidak berumur panjang &mdash;
	 * {@link #getKodeUnik()} akan menghitung ulang dan menimpanya pada pembacaan berikutnya. Setter
	 * ini pada dasarnya hanya ada untuk memenuhi kontrak JavaBean Hibernate saat hidrasi.</p>
	 *
	 * @param kodeUnik nilai kode unik dari basis data
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan disposisi SOP yang menaungi batch ini, setelah proxy lazy-nya di-resolve.
	 *
	 * <p>Implementasi kontrak abstrak {@link ais.database.model.sop.DataSop#getDisposisiSop()}.
	 * Pemanggilan {@code check(...)} milik {@link ais.database.model.GeneralValueObject} berusaha
	 * menginisialisasi proxy lewat cache, sesi berjalan, atau sesi baru &mdash; dan mengembalikan
	 * argumen apa adanya bila keempat tahapnya gagal. Karena itu hasilnya masih bisa berupa proxy
	 * yang belum terinisialisasi pada kasus terburuk.</p>
	 *
	 * <p><b>Efek samping:</b> hasil resolusi ditugaskan kembali ke field {@link #disposisiSop}.
	 * Ini menukar instance object, bukan mengubah nilai kolom, sehingga tidak mengubah data;
	 * tetapi biayanya bisa berupa satu query per pemanggilan bila cache meleset.</p>
	 *
	 * @return disposisi SOP batch, atau {@code null} bila batch tidak melalui alur SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP batch.
	 *
	 * <p><b>Non-obvious &mdash; penjaga berlapis yang membuat tautan ini praktis SEKALI PASANG.</b>
	 * Baris pertama menolak diam-diam argumen {@code null} maupun object yang id-nya {@code null},
	 * sehingga tautan yang sudah ada <b>tidak dapat dilepas lewat setter ini</b>. Ekspresi ternari
	 * pada baris kedua adalah sisa penjaga lama yang kini <b>tidak pernah bernilai benar</b>: syarat
	 * {@code (disposisiSop == null || disposisiSop.getId() == null)} sudah dijamin salah oleh
	 * penjaga pertama, jadi cabangnya selalu jatuh ke {@code disposisiSop}. Kode ini setara dengan
	 * penugasan biasa; dipertahankan apa adanya karena keluarga dokumen SOP lain memakai bentuk yang
	 * sama.</p>
	 *
	 * @param disposisiSop disposisi SOP baru; diabaikan bila null atau belum punya id
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Menyetel pengguna penyetuju batch.
	 *
	 * <p><b>Penting:</b> berbeda dari {@link #setOleh(String)} dan
	 * {@link #setDisposisiSop(DisposisiSop)}, setter ini <b>tidak</b> menolak {@code null} &mdash;
	 * memanggilnya dengan {@code null} benar-benar mencabut persetujuan batch, dan dengan itu
	 * mencabut kelayakan seluruh barisnya untuk diposting maupun dibatalkan postingnya. Dipanggil
	 * dari tombol Setujui/Batalkan Persetujuan layar ZK (digerbangi hak {@code APPROVE}/{@code REJECT}),
	 * dari {@code ProsesTransitoriAction#onSave}, dan dari
	 * {@code ProsesTransitoriApiHelper#setujui}/{@code #batalSetuju}. Jalur REST menolak pembatalan
	 * bila masih ada baris yang sudah dijurnal; jalur ZK <b>tidak</b> punya penjaga setara.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju, atau {@code null} untuk mencabut persetujuan
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Menyetel waktu persetujuan batch.
	 *
	 * <p>Selalu disetel berbarengan dengan {@link #setDisetujuiOleh(Tbmuser)} oleh semua pemanggil
	 * yang diketahui; nilai {@code null} berarti "belum/tidak lagi disetujui". Kolom ini juga menjadi
	 * sumbu filter rentang tanggal pada dasbor posting, sehingga mengubahnya memindahkan batch ke
	 * rentang lain.</p>
	 *
	 * @param tanggalPersetujuan waktu persetujuan, atau {@code null}
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan waktu persetujuan batch, <b>setelah divalidasi silang terhadap disposisi SOP-nya</b>.
	 *
	 * <p><b>Cara kerja.</b> Bila batch punya disposisi SOP tetapi disposisi itu belum mencatat
	 * pengajuan persetujuan &mdash; yaitu {@code disposisiSop.getDisposisiSetuju() == null} atau
	 * {@code disposisiSetuju.getDiajukanOleh() == null} &mdash; maka {@code tanggalPersetujuan}
	 * <b>dikosongkan</b>. Idenya: bagi dokumen yang berjalan lewat SOP, disposisilah sumber
	 * kebenaran persetujuan, bukan kolom lokal. Batch tanpa disposisi SOP tidak tersentuh syarat ini.</p>
	 *
	 * <p><b>Efek samping (DESTRUKTIF &mdash; menyentuh gerbang jurnal).</b> Pengosongan itu ditulis ke
	 * field, dan karena pemetaan memakai <i>property access</i> + {@code dynamicUpdate}, sekadar
	 * membaca dokumen di dalam sesi hidup dapat menerbitkan {@code UPDATE} yang menghapus stempel
	 * persetujuan yang sah. Berpasangan dengan {@link #getDisetujuiOleh()} yang melakukan hal setara
	 * pada kolom pelakunya, batch yang sudah disetujui &mdash; bahkan yang barisnya sudah dijurnal
	 * &mdash; dapat kehilangan status persetujuannya tanpa ada aksi pengguna. Setelah itu batch
	 * tersebut tidak lagi cocok dengan {@code PostingProsesTransitoriAction#kriteriaPostingStatic}
	 * (yang mensyaratkan {@code disetujuiOleh IS NOT NULL}), sehingga barisnya hilang dari layar
	 * posting maupun layar batal-posting: jurnalnya tetap ada, tetapi dokumennya tidak lagi terjangkau
	 * dari UI.</p>
	 *
	 * <p><b>Penanganan error.</b> Seluruh blok dibungkus {@code try/catch} yang menelan exception
	 * (dicatat lewat {@code ErrorAuditUtil}). Alasannya tercatat di kode: {@link #getDisposisiSop()}
	 * dapat mengembalikan instance kanonik/berbagi milik {@code AuditTimestampInterceptor} yang
	 * proxy-nya terikat ke {@code Session} lain yang sudah tertutup, sehingga tanpa penjaga ini
	 * getter akan melempar {@code LazyInitializationException} dari jalur render. Konsekuensinya:
	 * bila resolusi lazy gagal, validasi silang <b>dilewati diam-diam</b> dan nilai kolom lokal
	 * dipertahankan &mdash; hasil getter ini karena itu bisa berbeda antar-pemanggilan tergantung
	 * keadaan sesi.</p>
	 *
	 * @return waktu persetujuan, atau {@code null} bila belum disetujui atau disposisi SOP-nya belum
	 *         mencatat pengajuan persetujuan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		try {
			// FIX LazyInitializationException: getDisposisiSop() bisa berupa instance
			// canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke
			// Session lain yang sudah closed -> jangan biarkan getter ini crash, cukup
			// lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/akunting/ProsesTransitori.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/**
	 * Mengembalikan pengguna penyetuju batch, setelah proxy lazy-nya di-resolve dan
	 * <b>divalidasi silang terhadap disposisi SOP-nya</b>.
	 *
	 * <p><b>Cara kerja.</b> Dua langkah: (1) {@code check(...)} me-resolve proxy lazy
	 * {@link ais.database.model.Tbmuser}; (2) syarat yang sama persis dengan
	 * {@link #getTanggalPersetujuan()} diterapkan &mdash; bila batch punya disposisi SOP yang belum
	 * mencatat pengajuan persetujuan, penyetujunya <b>dikosongkan</b>.</p>
	 *
	 * <p><b>Efek samping (DESTRUKTIF &mdash; ini gerbang posting jurnal).</b> Nilai {@code null}
	 * ditulis ke field {@link #disetujuiOleh}, yang dipetakan ke kolom {@code disetujui_oleh}. Dengan
	 * <i>property access</i> + {@code dynamicUpdate}, membaca dokumen di dalam sesi hidup sudah cukup
	 * untuk menerbitkan {@code UPDATE} yang mencabut persetujuan. Ini kolom yang sama yang dipakai
	 * layar daftar untuk memutuskan boleh-tidaknya batch dihapus/diubah, dan yang dipakai mesin
	 * posting untuk memutuskan boleh-tidaknya barisnya dijurnal &mdash; lihat uraian dampaknya di
	 * {@link #getTanggalPersetujuan()}.</p>
	 *
	 * <p><b>Kasus tepi.</b> Berbeda dari kembarannya, method ini <b>tidak</b> dibungkus
	 * {@code try/catch}, sehingga {@code LazyInitializationException} dari
	 * {@code getDisposisiSop().getDisposisiSetuju()} akan merambat ke pemanggil. Selain itu
	 * {@link #getDisposisiSop()} dipanggil tiga kali di dalam satu method &mdash; masing-masing
	 * berpotensi memicu siklus resolusi {@code check(...)} tersendiri.</p>
	 *
	 * @return pengguna penyetuju, atau {@code null} bila belum disetujui atau disposisi SOP-nya belum
	 *         mencatat pengajuan persetujuan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}
		return disetujuiOleh;
	}

	/**
	 * Mengembalikan tanggal pengajuan batch, <b>diselaraskan dengan waktu mulai disposisi SOP-nya</b>.
	 *
	 * <p><b>Cara kerja.</b> Bila batch punya disposisi SOP yang simpul awalnya sudah diajukan
	 * seseorang ({@code disposisiStart.getDiajukanOleh() != null}), maka {@code tanggalPembuatan}
	 * <b>ditimpa</b> dengan {@code disposisiStart.getWaktu()}. Bila setelah itu nilainya masih
	 * {@code null}, dikembalikan waktu SEKARANG ({@code WaktuUtil.getDate()}) sebagai substitusi
	 * tampilan.</p>
	 *
	 * <p><b>Efek samping (DESTRUKTIF).</b> Penimpaan dari disposisi ditulis ke field, sehingga
	 * membaca dokumen dapat memindahkan tanggal pengajuan yang tersimpan. Ini penting karena layar
	 * daftar memfilter dengan {@code date(this_.tanggal_pembuatan) between ...} &mdash; batch dapat
	 * "berpindah" keluar dari rentang pencarian yang sebelumnya menampilkannya. Sebaliknya,
	 * substitusi "hari ini" pada baris {@code return} <b>tidak</b> ditulis balik: nilainya hanya
	 * hidup selama pemanggilan, jadi baris dengan {@code tanggal_pembuatan} kosong akan tampak
	 * bertanggal hari ini setiap kali dibuka tanpa pernah benar-benar tersimpan.</p>
	 *
	 * <p><b>Kasus tepi.</b> Tidak ada penjaga null pada {@code disposisiStart.getWaktu()} &mdash;
	 * bila disposisi awal ada, sudah diajukan, tetapi waktunya kosong, getter mengembalikan
	 * "hari ini" karena substitusi di baris terakhir menangkap {@code null} tersebut.</p>
	 *
	 * @return tanggal pengajuan batch; tidak pernah {@code null} (hari ini bila belum terisi)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
		}

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Menyetel tanggal pengajuan batch.
	 *
	 * <p>Diisi layar ZK dari datebox "Tanggal Pengajuan" (yang dikunci {@code readonly}, jadi
	 * praktis nilainya berasal dari {@link #getTanggalPembuatan()} saat form dibuka). Nilai yang
	 * disetel di sini dapat ditimpa kembali oleh getter-nya bila batch tertaut disposisi SOP.</p>
	 *
	 * @param tanggalPembuatan tanggal pengajuan batch
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

}
