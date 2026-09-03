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

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.surat.NomorSurat;

/**
 * <h2>NomorSuratAlurKeuangan &mdash; katalog "alur penomoran" dokumen keuangan</h2>
 *
 * <p><b>Untuk apa.</b> Entity ini adalah <b>tabel jembatan/katalog</b> yang memetakan
 * <i>satu jenis dokumen finansial</i> ke <i>satu template penomoran</i>
 * ({@link ais.database.model.surat.NomorSurat}). Ia menjawab pertanyaan tunggal:
 * "dokumen Uang Muka (atau Kas Besar, atau DPC, &hellip;) nomor suratnya dicetak
 * memakai format yang mana?". Tabelnya berada di skema {@code akunting}, nama
 * {@code nomor_surat_alur_keuangan}, dan isinya <b>sangat kecil</b> &mdash; secara
 * bawaan hanya 11 baris, satu baris per jenis dokumen (lihat daftar di bawah).</p>
 *
 * <p><b>Yang PENTING dipahami: entity ini BUKAN pencacah.</b> Meski namanya
 * mengandung "Nomor Surat", kelas ini <b>tidak menyimpan counter</b>, tidak punya
 * kolom "nomor terakhir", dan tidak punya satu pun method pembangkit nomor. Seluruh
 * kolom bisnisnya hanya {@code kode}, {@code nama}, {@code keterangan}, dan FK
 * {@code nomor_surat}. Angka urut, aturan reset (per tahun/bulan/tanggal), jumlah nol
 * di depan, serta susunan segmen format semuanya milik {@link NomorSurat}. Peran kelas
 * ini murni <i>indirection</i>: modul keuangan tidak menyebut template nomor surat
 * secara langsung, melainkan lewat baris katalog ini, sehingga admin bisa memindahkan
 * satu modul ke template lain (lewat layar master, tanpa deploy ulang) hanya dengan
 * mengganti FK-nya.</p>
 *
 * <h3>Sebelas jenis dokumen yang dilayani</h3>
 * <p>Pasangan (kode, nama, keterangan) bawaan didefinisikan pada larik {@link #S} dan
 * ditanam ke DB oleh {@link #reloadDefault()}. Daftar lengkapnya:</p>
 * <ol>
 *   <li><b>001</b> &mdash; "Uang Muka" (keterangan "Kas Advance") &rarr; dokumen panjar
 *       pegawai {@code UangMuka}; konstanta {@link #UANG_MUKA}, cache {@link #UANG_MUKA_DATA}.</li>
 *   <li><b>002</b> &mdash; "Dana Talangan" (keterangan "Kas Besar") &rarr; {@code DanaTalangan};
 *       {@link #DANA_TALANGAN} / {@link #DANA_TALANGAN_DATA}.</li>
 *   <li><b>003</b> &mdash; "Pertanggungjawaban" &rarr; LPJ uang muka pegawai
 *       {@code Pertangungjawaban}; {@link #PERTANGGUNGJAWABAN} / {@link #PERTANGGUNGJAWABAN_DATA}.</li>
 *   <li><b>004</b> &mdash; "Kas Kecil" &rarr; {@code KasKecil}; {@link #KAS_KECIL} /
 *       {@link #KAS_KECIL_DATA}.</li>
 *   <li><b>005</b> &mdash; "Pembayaran Pembelian" (keterangan "Penggantian Kas Kecil") &rarr;
 *       {@code PenggantianKasKecil}; {@link #PENGGANTIAN_KAS_KECIL} /
 *       {@link #PENGGANTIAN_KAS_KECIL_DATA}.</li>
 *   <li><b>006</b> &mdash; "DPC" (Daftar Pengajuan Cek) &rarr; batch transfer
 *       {@code ProsesTransfer}; {@link #DAFTAR_PENGAJUAN_CHEK} / {@link #DPC}.</li>
 *   <li><b>007</b> &mdash; "Kas Besar" &rarr; {@code KasBesar}; {@link #KAS_BESAR} /
 *       {@link #KAS_BESAR_DATA}.</li>
 *   <li><b>008</b> &mdash; "Pertanggungjawaban Kas Besar" &rarr; {@code PertangungjawabanKasBesar};
 *       {@link #PERTANGGUNGJAWABAN_KAS_BESAR} / {@link #PERTANGGUNGJAWABAN_KAS_BESAR_DATA}.</li>
 *   <li><b>009</b> &mdash; "Transaksi Koperasi" &rarr; {@code TransaksiKoperasi} (paket
 *       {@code koperasi}); {@link #TRANSAKSI_KOPERASI} / {@link #TRANSAKSI_KOPERASI_DATA}.</li>
 *   <li><b>010</b> &mdash; "Standing Instruction" &rarr; {@code ProsesTransferStandingInstruction};
 *       {@link #STANDING_INSTRUCTION} / {@link #SI}.</li>
 *   <li><b>011</b> &mdash; "Reimbursement Pegawai" &rarr; {@code ReimbursementPegawai};
 *       {@link #REIMBURSEMENT} / {@link #REIMBURSEMENT_DATA}.</li>
 * </ol>
 *
 * <h3>Bagaimana nomor benar-benar dibuat (rantai lengkap)</h3>
 * <p>Kelas ini hanya menyediakan bahan; pembangkitannya terjadi di lapisan Action/API,
 * dengan pola yang sama di 9+ berkas ({@code UangMukaAction.generateCode},
 * {@code KasBesarAction}, {@code KasKecilAction}, {@code PenggantianKasKecilAction},
 * {@code DanaTalanganAction}, {@code PertangungjawabanAction},
 * {@code PertangungjawabanKasBesarAction}, {@code ProsesTransferAction},
 * {@code ProsesTransferStandingInstructionAction}, {@code TransaksiKoperasiAction},
 * {@code ReimbursementPegawaiAction}, plus kembaran REST-nya di
 * {@code *ApiHelper.formatNomor*}). Langkahnya:</p>
 * <ol>
 *   <li>Baca cache statik yang cocok (mis. {@code NomorSuratAlurKeuangan.UANG_MUKA_DATA}).
 *       Bila cache atau {@code getNomorSurat()}-nya {@code null}, penomoran resmi
 *       <b>dilewati diam-diam</b> dan dokumen memakai {@code Common.getGeneratedBarCode()}
 *       (kode acak, bukan nomor surat berformat).</li>
 *   <li>Ambil angka urut: bila {@code NomorSurat.getGunakanIndexUrut()} bernilai true,
 *       dipakai kolom counter {@code nomor_index}; bila tidak, dipakai
 *       {@code getindex(...)} milik Action, yang menghitung <b>{@code rowCount()} + 1</b>
 *       atas dokumen sejenis dalam cakupan reset yang berlaku.</li>
 *   <li>Format string lewat {@link NomorSurat#format(Long, java.util.Date)}.</li>
 *   <li>Naikkan counter lewat {@link NomorSurat#tambahIndexNomorSurat(NomorSurat)} (hanya
 *       untuk mode index urut), lalu lewatkan hasilnya ke
 *       {@code ais.action.master.KodeUnikUtil.pastikanUnik(...)} yang, bila kode sudah
 *       terpakai, menempelkan sufiks {@code "-2"}, {@code "-3"}, &hellip;</li>
 * </ol>
 *
 * <h3>Risiko nomor kembar (VERIFIKASI &mdash; sejalan dengan temuan FormatNis/PengajuanSiswa)</h3>
 * <ul>
 *   <li><b>Mode non-index memakai {@code rowCount()}</b>, yaitu <i>jumlah baris yang ada</i>,
 *       bukan <i>nomor tertinggi yang pernah terbit</i>. Menghapus satu dokumen membuat
 *       cacah turun sehingga nomor berikutnya <b>mengulang nomor yang sudah dipakai</b>.
 *       Ini pola cacat yang identik dengan mesin NIS/nomor agenda di modul lain.</li>
 *   <li><b>Baca-lalu-naikkan tidak atomik.</b> Pada mode index urut, angka dibaca dari
 *       {@code getNomorIndex()} <i>di luar</i> blok tersinkronisasi, baru kemudian
 *       {@code tambahIndexNomorSurat} (yang {@code synchronized static}) menaikkannya di
 *       transaksi terpisah. Dua permintaan bersamaan bisa membaca angka yang sama sebelum
 *       salah satunya sempat menaikkan. Sinkronisasinya pun hanya se-JVM &mdash; tidak
 *       aman pada deployment multi-node.</li>
 *   <li><b>Tidak ada indeks unik / sequence DB.</b> Satu-satunya penyelamat adalah
 *       {@code KodeUnikUtil.pastikanUnik}, yang bersifat cek-lalu-tulis (TOCTOU, bukan
 *       kunci) dan hanya memeriksa <i>class entity yang sama</i>. Akibat praktisnya:
 *       nomor surat resmi bisa muncul sebagai {@code &hellip;/001-2}, {@code &hellip;/001-3},
 *       bentuk yang tidak dikenal dokumen fisik.</li>
 *   <li><b>Satu template dibagi lintas tenant.</b> Entity ini <b>tidak punya kolom
 *       sekolah/yayasan/satuan kerja sama sekali</b>, dan cache statiknya global se-JVM.
 *       Jadi seluruh tenant pada satu instalasi berbagi baris katalog yang sama, dan
 *       karenanya berbagi satu {@link NomorSurat} beserta satu counter
 *       {@code nomor_index}. Nomor menjadi <b>berselang-seling antar tenant</b>
 *       (tenant A mendapat 001, tenant B 002, tenant A 003) dan urutan dokumen satu
 *       tenant tidak lagi rapat &mdash; sekaligus membocorkan volume aktivitas tenant lain.
 *       Pemisahan per satuan kerja hanya mungkin lewat placeholder teks
 *       ({@code KODE_SATKER}/{@code SATKER}) di dalam format, bukan lewat counter terpisah.</li>
 * </ul>
 *
 * <h3>Kuirk &amp; jebakan lain</h3>
 * <ul>
 *   <li><b>Kunci pencarian adalah {@code nama}, bukan {@code kode}.</b> {@link #reloadDefault()}
 *       mencocokkan baris dengan {@code Restrictions.eq("nama", &hellip;)}. Padahal {@code nama}
 *       adalah label yang bisa diubah pengguna (lewat jendela revisi/"Pakai" di layar master).
 *       Mengganti nama satu baris <b>memutus ikatan statiknya secara permanen</b>: cache
 *       menjadi {@code null}, dan untuk enam jenis yang punya cadangan pembuatan otomatis
 *       (lihat di bawah) baris baru akan <b>diduplikasi</b> dengan kode yang sama.</li>
 *   <li><b>Hanya sebagian jenis punya jaring pengaman.</b> Blok
 *       {@code if (X == null) { buat baru }} pada {@link #reloadDefault()} hanya ada untuk
 *       DPC(006), Reimbursement(011), Standing Instruction(010), Kas Besar(007),
 *       Pertanggungjawaban Kas Besar(008) dan Transaksi Koperasi(009) &mdash; yaitu jenis-jenis
 *       yang ditambahkan belakangan setelah tabel produksi terlanjur terisi. Lima jenis
 *       tertua (001 Uang Muka, 002 Dana Talangan, 003 Pertanggungjawaban, 004 Kas Kecil,
 *       005 Pembayaran Pembelian) <b>tidak punya cadangan</b>: kalau barisnya hilang atau
 *       namanya berubah, cache-nya tetap {@code null} selamanya dan modul terkait diam-diam
 *       jatuh ke kode acak.</li>
 *   <li><b>Penjaga seed adalah cacah SELURUH tabel.</b> Perulangan penanaman {@link #S}
 *       hanya berjalan bila {@code rowCount() == 0}. Satu baris saja sudah cukup untuk
 *       membatalkan seluruh penanaman &mdash; itulah sebabnya blok cadangan per-jenis di atas
 *       harus ada.</li>
 *   <li><b>Nilai konstanta tidak selalu sama dengan namanya.</b> {@link #PENGGANTIAN_KAS_KECIL}
 *       bernilai {@code "Pembayaran Pembelian"} (bukan "Penggantian Kas Kecil" &mdash; teks itu
 *       hanya muncul sebagai <i>keterangan</i>), dan keterangan bawaan Dana Talangan justru
 *       berbunyi {@code "Kas Besar"}, sama persis dengan keterangan jenis 007.</li>
 *   <li><b>Pemetaan salah pada satu konsumen.</b> {@code PertangungjawabanKasBesar
 *       .getNomorSuratAlurKeuangan()} memasang {@link #PERTANGGUNGJAWABAN_DATA} (kode 003)
 *       padahal seharusnya {@link #PERTANGGUNGJAWABAN_KAS_BESAR_DATA} (kode 008); karena
 *       getter itu menulis balik ke field, ketidakcocokan tersebut <b>tersimpan permanen</b>
 *       begitu dokumen dibaca sekali. Sepuluh konsumen lain sudah memakai konstanta yang
 *       benar. Rinciannya didokumentasikan di berkas tersebut.</li>
 *   <li><b>Tidak ada kolom "aktif".</b> Sebuah jenis dokumen tidak bisa dinonaktifkan; satu-
 *       satunya cara mematikan penomoran resmi adalah mengosongkan FK {@code nomor_surat},
 *       yang efeknya bukan penolakan melainkan diam-diam beralih ke kode acak.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ol>
 *   <li><b>Konstanta nama jenis dokumen</b> (11 {@code String}) &mdash; kunci pencarian baris.</li>
 *   <li><b>Larik benih {@link #S}</b> &mdash; definisi baris bawaan berformat
 *       {@code "kode;nama;keterangan"}.</li>
 *   <li><b>Cache statik</b> (11 field {@code NomorSuratAlurKeuangan}) &mdash; hasil pencarian
 *       yang dipegang di memori JVM dan dibaca langsung oleh Action/API.</li>
 *   <li><b>{@link #reloadDefault()}</b> &mdash; satu-satunya method dengan logika nyata:
 *       menanam benih bila perlu, lalu mengisi kesebelas cache.</li>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #getOlehId()},
 *       {@link #getTanggal_dirubah()} dan hook {@code @PreUpdate}; ditambah
 *       {@link Audited} (Envers) yang menggandakan tiap versi ke tabel
 *       {@code nomor_surat_alur_keuangan_aud}.</li>
 *   <li><b>Properti persisten</b> &mdash; {@link #getId()}, {@link #getKode()},
 *       {@link #getNama()}, {@link #getKeterangan()}, {@link #getNomorSurat()}.</li>
 * </ol>
 *
 * <h3>Catatan teknis pemetaan</h3>
 * <p>Kelas mewarisi {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa &mdash;
 * Hibernate tidak memetakan properti induk. Karena itu {@code id}, {@code oleh},
 * {@code olehId} dan {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di sini;
 * pengulangan itu keharusan teknis, bukan duplikasi yang keliru. Pemetaan memakai
 * <i>property access</i> (anotasi menempel di getter), sehingga Hibernate memanggil
 * getter saat flush &mdash; termasuk {@link #getNomorSurat()} yang meresolusi proxy lewat
 * {@code check(...)} milik kelas induk. {@code dynamicInsert}/{@code dynamicUpdate}
 * aktif, jadi hanya kolom yang benar-benar berubah yang ikut di-SQL-kan.</p>
 *
 * <h3>Siapa memanggil apa</h3>
 * <p>{@link #reloadDefault()} dipanggil dari dua tempat: {@code ais.common.InitData}
 * (rangkaian {@code reloadDefaults()} saat aplikasi mulai) dan
 * {@code NomorSuratAlurKeuanganAction.doAfterCompose} (setiap kali layar master dibuka,
 * plus sekali lagi lewat timer setelah admin mengganti template di grid, agar transaksi
 * baru langsung memakai template baru tanpa restart). Cache statiknya dibaca oleh 11
 * Action ZK, 8 helper REST, dan 10 entity dokumen. Layar master sendiri dijaga hanya
 * oleh {@code Common.doCheckSecurity()} (pemeriksaan login), tanpa {@code checkPrevilages()}
 * apa pun &mdash; sedangkan padanan REST-nya, {@code NomorSuratKeuanganApiHelper.bolehAksi()},
 * mengembalikan {@code true} saat peran pengguna {@code null} (fail-open).</p>
 *
 * @see ais.database.model.surat.NomorSurat
 * @see ais.database.model.GeneralValueObject
 * @see ais.common.InitData
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "nomor_surat_alur_keuangan")
public class NomorSuratAlurKeuangan extends GeneralValueObject {

	/**
	 * Nama baris katalog untuk dokumen <b>Uang Muka</b> (panjar pegawai), kode "001".
	 * Dipakai sebagai kunci pencarian {@code nama} di {@link #reloadDefault()} dan sebagai
	 * label yang tampil di layar master.
	 */
	public static final String UANG_MUKA = "Uang Muka";
	/**
	 * Nama baris katalog untuk dokumen <b>Dana Talangan</b>, kode "002". Keterangan
	 * bawaannya justru berbunyi "Kas Besar" (lihat kuirk pada dokumentasi kelas).
	 */
	public static final String DANA_TALANGAN = "Dana Talangan";
	/**
	 * Nama baris katalog untuk <b>LPJ uang muka pegawai</b> ({@code Pertangungjawaban}),
	 * kode "003".
	 */
	public static final String PERTANGGUNGJAWABAN = "Pertanggungjawaban";
	/**
	 * Nama baris katalog untuk <b>LPJ kas besar</b> ({@code PertangungjawabanKasBesar}),
	 * kode "008". Perhatikan bahwa entity konsumennya keliru memakai
	 * {@link #PERTANGGUNGJAWABAN_DATA} alih-alih {@link #PERTANGGUNGJAWABAN_KAS_BESAR_DATA}.
	 */
	public static final String PERTANGGUNGJAWABAN_KAS_BESAR = "Pertanggungjawaban Kas Besar";
	/** Nama baris katalog untuk dokumen <b>Kas Kecil</b>, kode "004". */
	public static final String KAS_KECIL = "Kas Kecil";
	/**
	 * Nama baris katalog untuk dokumen <b>Penggantian Kas Kecil</b>, kode "005".
	 *
	 * <p><b>Perhatian:</b> nilainya {@code "Pembayaran Pembelian"}, tidak sama dengan nama
	 * konstantanya. Teks "Penggantian Kas Kecil" hanya dipakai sebagai <i>keterangan</i>
	 * baris pada {@link #S}. Mengubah nilai konstanta ini akan memutus ikatan dengan baris
	 * yang sudah tersimpan di database produksi.</p>
	 */
	public static final String PENGGANTIAN_KAS_KECIL = "Pembayaran Pembelian";
	/**
	 * Nama baris katalog untuk <b>Daftar Pengajuan Cek</b> (batch transfer
	 * {@code ProsesTransfer}), kode "006". Nilainya ringkas, {@code "DPC"}.
	 */
	public static final String DAFTAR_PENGAJUAN_CHEK = "DPC";
	/**
	 * Nama baris katalog untuk <b>Standing Instruction</b>
	 * ({@code ProsesTransferStandingInstruction}), kode "010".
	 */
	public static final String STANDING_INSTRUCTION = "Standing Instruction";
	/** Nama baris katalog untuk dokumen <b>Kas Besar</b>, kode "007". */
	public static final String KAS_BESAR = "Kas Besar";
	/**
	 * Nama baris katalog untuk <b>Transaksi Koperasi</b>
	 * ({@code ais.database.model.koperasi.TransaksiKoperasi}), kode "009" &mdash; satu-satunya
	 * jenis yang konsumennya berada di luar paket {@code akunting}.
	 */
	public static final String TRANSAKSI_KOPERASI = "Transaksi Koperasi";
	/**
	 * Nama baris katalog untuk <b>Reimbursement Pegawai</b>, kode "011" &mdash; jenis terbaru
	 * yang ditambahkan. Konsumennya, entity {@code ReimbursementPegawai}, tidak memiliki
	 * kolom {@code nomor_surat_alur_keuangan}; hanya {@code ReimbursementPegawaiAction}
	 * yang membaca {@link #REIMBURSEMENT_DATA} sebagai cadangan template.
	 */
	public static final String REIMBURSEMENT = "Reimbursement Pegawai";

	/**
	 * Definisi <b>baris bawaan</b> katalog, satu {@code String} per jenis dokumen dengan
	 * format {@code "kode;nama;keterangan"} (pemisah titik koma).
	 *
	 * <p>Hanya dibaca oleh perulangan penanaman di {@link #reloadDefault()}, dan hanya bila
	 * tabel benar-benar kosong. Urutan elemen menentukan urutan penyisipan, sehingga id
	 * hasil {@code IDENTITY} pada instalasi baru biasanya sejajar dengan urutan kode &mdash;
	 * <i>kecuali</i> jenis 010/011 yang di larik ini muncul setelah 009.</p>
	 *
	 * <p><b>Catatan:</b> perubahan pada larik ini <b>tidak</b> mempengaruhi instalasi yang
	 * tabelnya sudah terisi; baris lama tidak pernah diperbarui dari sini. Untuk jenis yang
	 * belum ada di instalasi lama, penambahannya justru dilakukan oleh blok cadangan
	 * per-jenis di {@link #reloadDefault()}, bukan oleh larik ini.</p>
	 */
	public static final String[] S = new String[] { "001;" + UANG_MUKA + ";Kas Advance",
			"002;" + DANA_TALANGAN + ";Kas Besar", "003;" + PERTANGGUNGJAWABAN + ";Pertanggungjawaban",
			"004;" + KAS_KECIL + ";Kas Kecil", "005;" + PENGGANTIAN_KAS_KECIL + ";Penggantian Kas Kecil",
			"006;" + DAFTAR_PENGAJUAN_CHEK + ";DPC", "007;" + KAS_BESAR + ";Kas Besar",
			"008;" + PERTANGGUNGJAWABAN_KAS_BESAR + ";Pertanggungjawaban Kas Besar",
			"009;" + TRANSAKSI_KOPERASI + ";Transaksi Koperasi",
			"010;" + STANDING_INSTRUCTION + ";Standing Instruction",
			"011;" + REIMBURSEMENT + ";Reimbursement Pegawai" };

	/**
	 * Cache statik baris katalog <b>Uang Muka</b> (kode "001"), diisi {@link #reloadDefault()}.
	 *
	 * <p>Dibaca oleh {@code UangMukaAction.generateCode}, {@code UangMukaApiHelper}, dan
	 * {@code UangMuka.getNomorSuratAlurKeuangan()} (yang menulis balik nilai ini ke field
	 * dokumen bila masih kosong). <b>Tidak</b> punya blok pembuatan cadangan di
	 * {@link #reloadDefault()}: bila barisnya hilang/berganti nama, field ini tetap
	 * {@code null} dan penomoran uang muka diam-diam jatuh ke kode acak.</p>
	 *
	 * <p><b>Global se-JVM</b> &mdash; tidak ada pemisahan per tenant maupun per satuan kerja.</p>
	 */
	public static NomorSuratAlurKeuangan UANG_MUKA_DATA;
	/**
	 * Cache statik baris <b>Dana Talangan</b> (kode "002"). Dibaca
	 * {@code DanaTalanganAction}, {@code DanaTalanganApiHelper}, dan
	 * {@code DanaTalangan.getNomorSuratAlurKeuangan()}. Tanpa blok pembuatan cadangan.
	 */
	public static NomorSuratAlurKeuangan DANA_TALANGAN_DATA;
	/**
	 * Cache statik baris <b>Pertanggungjawaban</b> (kode "003", LPJ uang muka pegawai).
	 *
	 * <p>Dibaca {@code PertangungjawabanAction}, {@code PertangungjawabanApiHelper},
	 * {@code Pertangungjawaban.getNomorSuratAlurKeuangan()} &mdash; dan, <b>keliru</b>, juga
	 * oleh {@code PertangungjawabanKasBesar.getNomorSuratAlurKeuangan()} yang semestinya
	 * memakai {@link #PERTANGGUNGJAWABAN_KAS_BESAR_DATA}. Tanpa blok pembuatan cadangan.</p>
	 */
	public static NomorSuratAlurKeuangan PERTANGGUNGJAWABAN_DATA;
	/**
	 * Cache statik baris <b>Pertanggungjawaban Kas Besar</b> (kode "008").
	 *
	 * <p>Dipakai dengan benar oleh {@code PertangungjawabanKasBesarAction} dan
	 * {@code PertangungjawabanKasBesarApiHelper} saat <i>membangkitkan</i> nomor, tetapi
	 * <b>tidak</b> oleh entity {@code PertangungjawabanKasBesar} saat <i>menyimpan</i>
	 * relasinya &mdash; sehingga nomor yang tercetak berasal dari template 008 sedangkan FK
	 * yang tersimpan menunjuk baris 003. Punya blok pembuatan cadangan di
	 * {@link #reloadDefault()}.</p>
	 */
	public static NomorSuratAlurKeuangan PERTANGGUNGJAWABAN_KAS_BESAR_DATA;
	/**
	 * Cache statik baris <b>Kas Kecil</b> (kode "004"). Dibaca {@code KasKecilAction},
	 * {@code KasKecilApiHelper}, {@code KasKecil.getNomorSuratAlurKeuangan()}. Tanpa blok
	 * pembuatan cadangan.
	 */
	public static NomorSuratAlurKeuangan KAS_KECIL_DATA;
	/**
	 * Cache statik baris <b>Kas Besar</b> (kode "007"). Dibaca {@code KasBesarAction},
	 * {@code KasBesarApiHelper}, {@code KasBesar.getNomorSuratAlurKeuangan()}. Punya blok
	 * pembuatan cadangan di {@link #reloadDefault()}.
	 */
	public static NomorSuratAlurKeuangan KAS_BESAR_DATA;
	/**
	 * Cache statik baris <b>Transaksi Koperasi</b> (kode "009"). Dibaca
	 * {@code TransaksiKoperasiAction} dan
	 * {@code ais.database.model.koperasi.TransaksiKoperasi.getNomorSuratAlurKeuangan()}.
	 * Punya blok pembuatan cadangan.
	 */
	public static NomorSuratAlurKeuangan TRANSAKSI_KOPERASI_DATA;
	/**
	 * Cache statik baris <b>Penggantian Kas Kecil</b> (kode "005", nama tersimpan
	 * "Pembayaran Pembelian"). Dibaca {@code PenggantianKasKecilAction},
	 * {@code PenggantianKasKecilApiHelper},
	 * {@code PenggantianKasKecil.getNomorSuratAlurKeuangan()}. Tanpa blok pembuatan cadangan.
	 */
	public static NomorSuratAlurKeuangan PENGGANTIAN_KAS_KECIL_DATA;
	/**
	 * Cache statik baris <b>Standing Instruction</b> (kode "010"). Penamaan field ini
	 * memang singkat (tanpa akhiran {@code _DATA}), berbeda gaya dari mayoritas cache lain.
	 * Dibaca {@code ProsesTransferStandingInstructionAction} dan
	 * {@code ProsesTransferStandingInstruction.getNomorSuratAlurKeuangan()}. Punya blok
	 * pembuatan cadangan.
	 */
	public static NomorSuratAlurKeuangan SI;
	/**
	 * Cache statik baris <b>DPC / Daftar Pengajuan Cek</b> (kode "006"), penamaan singkat
	 * seperti {@link #SI}. Dibaca {@code ProsesTransferAction},
	 * {@code ProsesTransferApiHelper} (yang bahkan memanggil
	 * {@code setNomorSuratAlurKeuangan(DPC)} secara eksplisit) dan
	 * {@code ProsesTransfer.getNomorSuratAlurKeuangan()}. Punya blok pembuatan cadangan.
	 */
	public static NomorSuratAlurKeuangan DPC;
	/**
	 * Cache statik baris <b>Reimbursement Pegawai</b> (kode "011").
	 *
	 * <p>Hanya dibaca {@code ReimbursementPegawaiAction}, itu pun sebagai <i>cadangan</i>
	 * bila template nomor surat tidak ditemukan lewat jalur lain; entity
	 * {@code ReimbursementPegawai} sendiri tidak memiliki relasi ke katalog ini. Punya blok
	 * pembuatan cadangan di {@link #reloadDefault()}.</p>
	 */
	public static NomorSuratAlurKeuangan REIMBURSEMENT_DATA;

	/**
	 * Menanam baris katalog bawaan bila perlu, lalu <b>mengisi ulang kesebelas cache
	 * statik</b> dari database.
	 *
	 * <p><b>Tujuan.</b> Membuat konstanta {@code *_DATA}/{@link #SI}/{@link #DPC} siap pakai
	 * bagi seluruh Action dan helper REST modul keuangan, sehingga mereka cukup membaca
	 * field statik tanpa melakukan query sendiri saat membangkitkan nomor dokumen.</p>
	 *
	 * <p><b>Urutan kerja.</b></p>
	 * <ol>
	 *   <li>Mengambil {@code HibernateUtil.currentNativeSession()}.</li>
	 *   <li>Menghitung {@code rowCount()} <b>seluruh tabel</b>. Bila hasilnya nol, seluruh
	 *       elemen {@link #S} dipecah dengan {@code split(";")} lalu disimpan satu per satu,
	 *       masing-masing dalam transaksinya sendiri
	 *       ({@code begin()}/{@code save()}/{@code commit()} per baris).</li>
	 *   <li>Mencari baris untuk Uang Muka, Dana Talangan, Pertanggungjawaban, Kas Kecil dan
	 *       Penggantian Kas Kecil berdasarkan kolom {@code nama} ({@code setMaxResults(1)});
	 *       hasilnya bisa {@code null} dan <b>tidak ada penggantinya</b>.</li>
	 *   <li>Mencari lalu, bila tidak ketemu, <b>membuat</b> baris DPC(006),
	 *       Reimbursement(011), Standing Instruction(010), Kas Besar(007),
	 *       Pertanggungjawaban Kas Besar(008) dan Transaksi Koperasi(009). Blok-blok inilah
	 *       jalur migrasi untuk instalasi lama yang tabelnya sudah terisi sebelum jenis-jenis
	 *       tersebut diperkenalkan &mdash; sekaligus alasan mengapa penjaga {@code count == 0}
	 *       di langkah 2 tidak menghalangi penambahan jenis baru.</li>
	 *   <li>Menutup sesi: {@code disconnect()} + {@code close()} bila masih terbuka, lalu
	 *       {@code HibernateUtil.closeSession()}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping.</b> Method ini <b>menulis ke database</b> &mdash; bukan sekadar
	 * membaca. Sekali dipanggil pada tabel kosong ia menyisipkan 11 baris; pada tabel yang
	 * sudah terisi ia tetap bisa menyisipkan hingga 6 baris (blok cadangan). Ia juga
	 * <b>menutup sesi Hibernate</b>: bila dipanggil dari dalam alur ZK/permintaan HTTP yang
	 * masih akan memakai sesi yang sama sesudahnya, sisa alur itu berisiko menemui sesi
	 * tertutup &mdash; pola yang sama sudah tercatat pada layar master kas besar/kas kecil.
	 * Setiap penyisipan juga menghasilkan revisi Envers di tabel
	 * {@code nomor_surat_alur_keuangan_aud}.</p>
	 *
	 * <p><b>Kasus tepi.</b></p>
	 * <ul>
	 *   <li>Pencocokan memakai {@code nama}, bukan {@code kode}. Bila admin mengubah nama
	 *       satu baris, pencarian gagal; untuk enam jenis bercadangan hasilnya adalah
	 *       <b>baris duplikat</b> dengan kode yang sama, untuk lima jenis lainnya cache tetap
	 *       {@code null}.</li>
	 *   <li>Bila tabel berisi lebih dari satu baris bernama sama, {@code setMaxResults(1)}
	 *       memilih salah satu tanpa urutan yang ditentukan &mdash; hasilnya bisa berbeda antar
	 *       pemanggilan.</li>
	 *   <li>Baris yang baru dibuat oleh blok cadangan <b>tidak diisi FK
	 *       {@code nomorSurat}</b>; sampai admin memilih template lewat layar master,
	 *       {@code getNomorSurat()} bernilai {@code null} sehingga Action terkait melewati
	 *       penomoran resmi dan memakai kode acak.</li>
	 *   <li>Tidak ada penanganan pengecualian: kegagalan query/simpan menyebar ke pemanggil.
	 *       Karena setiap baris memakai transaksi terpisah, kegagalan di tengah meninggalkan
	 *       tabel <b>terisi sebagian</b>, dan pemanggilan berikutnya tidak akan mengulang
	 *       penanaman (cacah tabel sudah bukan nol lagi).</li>
	 *   <li>Tidak {@code synchronized}: dua permintaan yang bersamaan-sama menjalankan method
	 *       ini pada tabel kosong dapat menyisipkan baris kembar.</li>
	 * </ul>
	 *
	 * <p><b>Dipanggil dari.</b> {@code ais.common.InitData.reloadDefaults()} saat aplikasi
	 * mulai, dan {@code NomorSuratAlurKeuanganAction.doAfterCompose} setiap kali layar master
	 * "Nomor Surat Alur Keuangan" dibuka &mdash; ditambah sekali lagi lewat timer ZK setiap kali
	 * admin mengganti template pada grid, supaya transaksi berikutnya langsung memakai
	 * template baru tanpa perlu restart server.</p>
	 */
	public static void reloadDefault() {
		// Selalu pakai session DEDICATED (openSession) — TIDAK memakai currentNativeSession
		// bersama milik alur ZK yang sedang berjalan (dipanggil dari
		// NomorSuratAlurKeuanganAction.doAfterCompose dan dari timer sesudah admin ganti
		// template), sama seperti pola NomorSurat.tambahIndexNomorSurat. Menutup sesi bersama
		// di sini akan memicu "Session is closed!" pada langkah berikutnya di alur ZK tersebut,
		// jadi HANYA session dedicated ini yang ditutup di finally.
		Session session = HibernateUtil.openSession();
		try {
			int count = ((Number) session.createCriteria(NomorSuratAlurKeuangan.class).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			if (count == 0) {

				for (String s : S) {
					NomorSuratAlurKeuangan nomorSuratAlurPengadaan = new NomorSuratAlurKeuangan();
					nomorSuratAlurPengadaan.setKode(s.split(";")[0]);
					nomorSuratAlurPengadaan.setNama(s.split(";")[1]);
					nomorSuratAlurPengadaan.setKeterangan(s.split(";")[2]);
					session.getTransaction().begin();
					session.save(nomorSuratAlurPengadaan);
					session.getTransaction().commit();
				}

			}

			UANG_MUKA_DATA = (NomorSuratAlurKeuangan) session.createCriteria(NomorSuratAlurKeuangan.class)
					.add(Restrictions.eq("nama", UANG_MUKA)).setMaxResults(1).uniqueResult();

			DANA_TALANGAN_DATA = (NomorSuratAlurKeuangan) session.createCriteria(NomorSuratAlurKeuangan.class)
					.add(Restrictions.eq("nama", DANA_TALANGAN)).setMaxResults(1).uniqueResult();

			PERTANGGUNGJAWABAN_DATA = (NomorSuratAlurKeuangan) session.createCriteria(NomorSuratAlurKeuangan.class)
					.add(Restrictions.eq("nama", PERTANGGUNGJAWABAN)).setMaxResults(1).uniqueResult();

			KAS_KECIL_DATA = (NomorSuratAlurKeuangan) session.createCriteria(NomorSuratAlurKeuangan.class)
					.add(Restrictions.eq("nama", KAS_KECIL)).setMaxResults(1).uniqueResult();

			PENGGANTIAN_KAS_KECIL_DATA = (NomorSuratAlurKeuangan) session.createCriteria(NomorSuratAlurKeuangan.class)
					.add(Restrictions.eq("nama", PENGGANTIAN_KAS_KECIL)).setMaxResults(1).uniqueResult();

			DPC = (NomorSuratAlurKeuangan) session.createCriteria(NomorSuratAlurKeuangan.class)
					.add(Restrictions.eq("nama", DAFTAR_PENGAJUAN_CHEK)).setMaxResults(1).uniqueResult();

			if (DPC == null) {
				DPC = new NomorSuratAlurKeuangan();
				DPC.setKode("006");
				DPC.setNama(DAFTAR_PENGAJUAN_CHEK);
				DPC.setKeterangan(DAFTAR_PENGAJUAN_CHEK);
				session.getTransaction().begin();
				session.save(DPC);
				session.getTransaction().commit();
			}

			REIMBURSEMENT_DATA = (NomorSuratAlurKeuangan) session.createCriteria(NomorSuratAlurKeuangan.class)
					.add(Restrictions.eq("nama", REIMBURSEMENT)).setMaxResults(1).uniqueResult();

			if (REIMBURSEMENT_DATA == null) {
				REIMBURSEMENT_DATA = new NomorSuratAlurKeuangan();
				REIMBURSEMENT_DATA.setKode("011");
				REIMBURSEMENT_DATA.setNama(REIMBURSEMENT);
				REIMBURSEMENT_DATA.setKeterangan("Reimbursement Pegawai");
				session.getTransaction().begin();
				session.save(REIMBURSEMENT_DATA);
				session.getTransaction().commit();
			}

			SI = (NomorSuratAlurKeuangan) session.createCriteria(NomorSuratAlurKeuangan.class)
					.add(Restrictions.eq("nama", STANDING_INSTRUCTION)).setMaxResults(1).uniqueResult();

			if (SI == null) {
				SI = new NomorSuratAlurKeuangan();
				SI.setKode("010");
				SI.setNama(STANDING_INSTRUCTION);
				SI.setKeterangan(STANDING_INSTRUCTION);
				session.getTransaction().begin();
				session.save(SI);
				session.getTransaction().commit();
			}

			KAS_BESAR_DATA = (NomorSuratAlurKeuangan) session.createCriteria(NomorSuratAlurKeuangan.class)
					.add(Restrictions.eq("nama", KAS_BESAR)).setMaxResults(1).uniqueResult();

			if (KAS_BESAR_DATA == null) {
				KAS_BESAR_DATA = new NomorSuratAlurKeuangan();
				KAS_BESAR_DATA.setKode("007");
				KAS_BESAR_DATA.setNama(KAS_BESAR);
				KAS_BESAR_DATA.setKeterangan(KAS_BESAR);
				session.getTransaction().begin();
				session.save(KAS_BESAR_DATA);
				session.getTransaction().commit();
			}

			PERTANGGUNGJAWABAN_KAS_BESAR_DATA = (NomorSuratAlurKeuangan) session
					.createCriteria(NomorSuratAlurKeuangan.class).add(Restrictions.eq("nama", PERTANGGUNGJAWABAN_KAS_BESAR))
					.setMaxResults(1).uniqueResult();

			if (PERTANGGUNGJAWABAN_KAS_BESAR_DATA == null) {
				PERTANGGUNGJAWABAN_KAS_BESAR_DATA = new NomorSuratAlurKeuangan();
				PERTANGGUNGJAWABAN_KAS_BESAR_DATA.setKode("008");
				PERTANGGUNGJAWABAN_KAS_BESAR_DATA.setNama(PERTANGGUNGJAWABAN_KAS_BESAR);
				PERTANGGUNGJAWABAN_KAS_BESAR_DATA.setKeterangan(PERTANGGUNGJAWABAN_KAS_BESAR);
				session.getTransaction().begin();
				session.save(PERTANGGUNGJAWABAN_KAS_BESAR_DATA);
				session.getTransaction().commit();
			}

			TRANSAKSI_KOPERASI_DATA = (NomorSuratAlurKeuangan) session.createCriteria(NomorSuratAlurKeuangan.class)
					.add(Restrictions.eq("nama", TRANSAKSI_KOPERASI)).setMaxResults(1).uniqueResult();

			if (TRANSAKSI_KOPERASI_DATA == null) {
				TRANSAKSI_KOPERASI_DATA = new NomorSuratAlurKeuangan();
				TRANSAKSI_KOPERASI_DATA.setKode("009");
				TRANSAKSI_KOPERASI_DATA.setNama(TRANSAKSI_KOPERASI);
				TRANSAKSI_KOPERASI_DATA.setKeterangan(TRANSAKSI_KOPERASI);
				session.getTransaction().begin();
				session.save(TRANSAKSI_KOPERASI_DATA);
				session.getTransaction().commit();
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Versi serialisasi Java.
	 *
	 * <p><b>Catatan:</b> nilainya identik dengan {@code serialVersionUID} milik
	 * {@link ais.database.model.surat.NomorSurat} dan sejumlah entity lain &mdash; jejak khas
	 * kelas hasil salin-tempel dari cetakan hbm2java yang sama, bukan nilai yang dihitung
	 * per kelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel; dihasilkan database ({@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyimpan baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini (jejak audit). Getter polos.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna penyimpan.
	 *
	 * <p><b>Non-obvious:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} maupun
	 * string kosong/spasi &mdash; nilai lama dipertahankan, tidak ada pengecualian yang
	 * dilempar. Jadi jejak audit tidak bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna penyimpan.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-{@code trim}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini. Getter polos.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} plus deklarasi field stempel waktu perubahan &mdash; keduanya
	 * ditulis pada satu baris fisik oleh perkakas penambah audit.
	 *
	 * <p>{@code onUpdate()} dijalankan Hibernate tepat sebelum setiap {@code UPDATE} baris ini
	 * dan meneruskan objek ke {@code AuditTimestampInterceptor.ubah(this)}, yang memperbarui
	 * {@link #tanggal_dirubah} (dan jejak {@code oleh}/{@code olehId} bila konteks pengguna
	 * tersedia). Hook <b>tidak</b> menyala pada {@code INSERT}; karena itu field
	 * {@code tanggal_dirubah} sudah diberi nilai awal {@code WaktuUtil.getDate()} saat objek
	 * dibuat.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir. Setter polos, tanpa validasi.
	 *
	 * <p>Umumnya diisi otomatis oleh {@code AuditTimestampInterceptor} lewat hook
	 * {@code @PreUpdate}; pengisian manual akan ditimpa pada penyimpanan berikutnya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi {@code TIMESTAMP}). Getter polos.
	 *
	 * @return waktu perubahan terakhir; untuk objek baru berisi waktu pembuatan objek
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks berformat {@code "id-nama"}, mis. {@code "3-Pertanggungjawaban"}.
	 *
	 * <p><b>Non-obvious:</b> membaca field {@code nama} secara langsung (bukan lewat
	 * {@link #getNama()}), sehingga hasilnya <b>tidak</b> di-{@code trim}. Untuk objek yang
	 * belum tersimpan, bagian id berbunyi {@code "null"}.</p>
	 *
	 * @return gabungan id dan nama dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode numerik jenis dokumen ("001"&ndash;"011"), disimpan sebagai teks agar nol di
	 * depannya tidak hilang. Lihat {@link #getKode()}.
	 */
	private String kode;
	/**
	 * Nama jenis dokumen &mdash; sekaligus <b>kunci pencarian</b> yang dipakai
	 * {@link #reloadDefault()}. Lihat {@link #getNama()}.
	 */
	private String nama;
	/** Keterangan bebas yang tampil di layar master. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/**
	 * Template penomoran yang dipakai jenis dokumen ini. Boleh {@code null} (berarti tidak
	 * ada penomoran resmi). Lihat {@link #getNomorSurat()}.
	 */
	private NomorSurat nomorSurat;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Seluruh properti dibiarkan
	 * kosong, kecuali {@link #tanggal_dirubah} yang otomatis terisi waktu sekarang.
	 */
	public NomorSuratAlurKeuangan() {
	}

	/**
	 * Mengembalikan kunci utama baris katalog. Getter polos.
	 *
	 * <p>Kolom {@code id} dipetakan {@code insertable = false} sehingga nilainya sepenuhnya
	 * ditentukan database ({@code IDENTITY}); menetapkannya sendiri sebelum {@code save}
	 * tidak berpengaruh.</p>
	 *
	 * @return id baris, atau {@code null} untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Setter polos; umumnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jenis dokumen, <b>sudah di-{@code trim}</b>.
	 *
	 * <p><b>Penting.</b> Kolom ini bukan sekadar label tampilan: {@link #reloadDefault()}
	 * mencocokkan baris berdasarkan nilai ini terhadap konstanta {@link #UANG_MUKA},
	 * {@link #KAS_BESAR}, dan seterusnya. Mengubah nama sebuah baris (mis. lewat jendela
	 * revisi pada layar master) <b>memutus ikatannya dengan modul terkait</b>. Perhatikan
	 * pula bahwa pencocokan di {@code reloadDefault()} dilakukan di sisi database terhadap
	 * nilai <i>asli</i> kolom, sedangkan getter ini mengembalikan versi ter-{@code trim} &mdash;
	 * spasi di ujung nama karenanya membuat pencarian gagal meski tampilannya identik.</p>
	 *
	 * @return nama jenis dokumen tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama jenis dokumen. Setter polos, tanpa validasi maupun {@code trim}.
	 *
	 * <p>Kolom dipetakan {@code nullable = false}; menyimpan objek dengan nama {@code null}
	 * akan ditolak database, bukan oleh setter ini.</p>
	 *
	 * @param nama nama jenis dokumen; sebaiknya salah satu konstanta pada kelas ini
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris katalog. Getter polos (tidak di-{@code trim}).
	 *
	 * @return keterangan, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas. Setter polos, tanpa validasi.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kode jenis dokumen ("001"&ndash;"011"). Getter polos.
	 *
	 * <p><b>Non-obvious:</b> properti ini <b>tidak</b> punya anotasi {@code @Column} sendiri,
	 * jadi Hibernate memetakannya ke kolom bawaan bernama {@code kode}. Nilainya bersifat
	 * informatif untuk tampilan &mdash; tidak satu pun jalur kode mencari baris berdasarkan
	 * kode ini (pencarian selalu lewat {@link #getNama()}), dan tidak ada indeks unik yang
	 * mencegah dua baris berkode sama.</p>
	 *
	 * @return kode jenis dokumen, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan kode jenis dokumen. Setter polos, tanpa validasi format.
	 *
	 * @param kode kode tiga digit sebagai teks, mis. {@code "008"}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan template penomoran ({@link NomorSurat}) yang dipakai jenis dokumen ini.
	 *
	 * <p><b>Cara kerja.</b> Nilai field dilewatkan ke {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject}, yang meresolusi proxy Hibernate yang
	 * malas/terlepas menjadi objek nyata (atau {@code null} bila targetnya sudah tidak ada).
	 * Hasil resolusi ditulis balik ke field &mdash; jadi getter ini <b>mengubah keadaan objek</b>,
	 * meski tidak mengubah <i>identitas</i> baris yang ditunjuk (berbeda dengan getter
	 * write-back di beberapa entity dokumen yang justru mengganti relasi dengan nilai
	 * default).</p>
	 *
	 * <p><b>Kasus tepi.</b> Relasi ini {@code nullable} dan <b>sering benar-benar kosong</b>
	 * untuk baris yang dibuat oleh blok cadangan {@link #reloadDefault()}. Setiap pemanggil
	 * di Action/API karenanya memeriksa {@code getNomorSurat() == null} dan, bila kosong,
	 * melewati penomoran resmi lalu memakai kode acak &mdash; kegagalan yang <b>senyap</b>,
	 * tanpa pesan ke pengguna. Karena dipetakan {@code FetchType.LAZY}, pembacaan di luar
	 * sesi Hibernate aktif dapat memicu {@code LazyInitializationException} yang ditangani
	 * {@code check(...)}.</p>
	 *
	 * <p><b>Cascade.</b> {@code PERSIST} dan {@code MERGE} diteruskan ke {@link NomorSurat},
	 * sehingga menyimpan baris katalog ikut menyimpan template baru yang belum tersimpan.
	 * Tidak ada {@code REMOVE}: menghapus baris katalog tidak menghapus templatenya.</p>
	 *
	 * @return template penomoran yang aktif, atau {@code null} bila belum dipilih admin
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat", nullable = true)
	public NomorSurat getNomorSurat() {
		nomorSurat = check(nomorSurat);
		return nomorSurat;
	}

	/**
	 * Menetapkan template penomoran untuk jenis dokumen ini. Setter polos, tanpa validasi
	 * maupun normalisasi objek berid-{@code null}.
	 *
	 * <p><b>Dipanggil dari.</b> Terutama {@code NomorSuratAlurKeuanganAction}, saat admin
	 * memilih template lain lewat bandbox pada grid layar master; perubahannya langsung
	 * disimpan ({@code Common.refreshUpdate}) lalu {@link #reloadDefault()} dijalankan ulang
	 * lewat timer supaya cache statik ikut segar. Perubahan ini berlaku <b>global</b> untuk
	 * semua tenant, karena entity ini tidak memiliki kolom cakupan apa pun.</p>
	 *
	 * @param nomorSurat template penomoran; {@code null} berarti jenis dokumen ini tidak
	 *                   memiliki penomoran resmi
	 */
	public void setNomorSurat(NomorSurat nomorSurat) {
		this.nomorSurat = nomorSurat;
	}

}
