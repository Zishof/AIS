package ais.database.model.akunting;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Pengajuan penggantian biaya pribadi pegawai (reimbursement).
 *
 * <p>
 * Sejak rework mengikuti pola {@link UangMuka}: dokumen ini digerakkan SOP
 * ({@code implements DataSop} + {@code disposisiSop}), persetujuan berada di
 * AKHIR alur disposisi (status/penyetuju diturunkan dari disposisi, bukan diset
 * manual), memilih Anggaran (Workspace/SatuanKerja) seperti uang muka, dan
 * setelah DISETUJUI otomatis masuk daftar DPC lewat
 * {@link DaftarPengajuanTransfer#simpanReimbursement}. Bedanya dengan uang muka:
 * tidak ada sumber Permintaan Pembelian (PR); rincian barang/biaya disimpan
 * sebagai JSON {@code formula} meniru pola Kas Kecil.
 * </p>
 *
 * <h2>Posisi dalam mesin keuangan</h2>
 *
 * <p>
 * Entity ini adalah <b>dokumen transaksi</b> (bukan master/katalog) di skema
 * {@code akunting}, tabel {@code reimbursement_pegawai}. Ia mewakili satu klaim
 * pegawai atas biaya yang <i>sudah</i> dikeluarkan dari kantong pribadi dan
 * diminta diganti perusahaan/yayasan. Karena biayanya sudah terjadi, arah
 * kasnya berlawanan dengan {@link UangMuka}: uang muka adalah <b>panjar di
 * depan</b> yang kelak harus dipertanggungjawabkan lewat
 * {@link Pertangungjawaban}, sedangkan reimbursement adalah <b>penggantian di
 * belakang</b> atas bukti yang sudah ada.
 * </p>
 *
 * <p>
 * <b>VERIFIKASI PENTING — dokumen SATU-LANGKAH, bukan siklus panjar+LPJ.</b>
 * Berbeda dari {@link UangMuka}/{@link Pertangungjawaban} dan
 * {@link KasBesar}/{@link PertangungjawabanKasBesar}, entity ini <b>tidak
 * punya pasangan LPJ</b> sama sekali: tidak ada kelas
 * {@code PertangungjawabanReimbursement}, tidak ada relasi balik ke dokumen
 * realisasi, dan tidak ada kolom "sisa/dikembalikan". Bukti pengeluaran
 * dilampirkan langsung pada pengajuan (rincian {@link #getFormula()} +
 * lampiran SOP), sehingga begitu DISETUJUI dokumen ini sudah final secara
 * dokumentasi dan tinggal dibayar. Seluruh eksekusi pembayaran diambil alih
 * {@link DaftarPengajuanTransfer} (DPC) dan
 * {@code ProsesTransferAction} — <b>bukan</b> oleh kolom-kolom pembayaran di
 * kelas ini (lihat peringatan "kolom tidur" di bawah).
 * </p>
 *
 * <h2>Siklus hidup klaim</h2>
 *
 * <ol>
 * <li><b>Pengajuan.</b> Pegawai (atau staf yang mewakilinya) membuka layar ZK
 * {@code reimbursement_pegawai.zul} ({@code ReimbursementPegawaiAction}),
 * memilih {@link JenisReimbursement}, mengisi judul, pegawai penerima, tanggal
 * pengeluaran, dan menambahkan baris rincian. Total baris rincian menjadi
 * {@link #getNominal()}; nomor dokumen {@link #getKode()} dibentuk
 * {@code generateCode()}. Status awal {@link #DIAJUKAN}.</li>
 * <li><b>Alur SOP.</b> Karena kelas ini {@code extends}
 * {@link ais.database.model.sop.DataSop}, formnya diinstansiasi mesin SOP
 * ({@code TampilanAlurSopAction} lewat {@code AlurSop.formInputan}) dan
 * dokumen ditautkan ke satu {@link DisposisiSop}. Setiap langkah disposisi
 * dicatat mesin SOP, bukan oleh kelas ini.</li>
 * <li><b>Persetujuan.</b> Terjadi di langkah <b>terakhir</b> alur disposisi.
 * Kelas ini tidak menyimpan keputusan itu secara mandiri: {@link #getStatus()},
 * {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()} dan
 * {@link #getAktif()} semuanya <b>menurunkan</b> nilainya dari
 * {@link DisposisiSop} setiap kali dibaca.</li>
 * <li><b>DPC (Daftar Pengajuan Cair/Transfer).</b> Begitu status menjadi
 * {@link #DISETUJUI}, dokumen didaftarkan ke
 * {@link DaftarPengajuanTransfer#simpanReimbursement(ReimbursementPegawai)}
 * lewat tiga jalur berbeda: timer 3,5 detik pasca-simpan di Action,
 * "safety-net" di renderer daftar, dan tombol Sinkronkan
 * ({@code SinkronDaftarPengajuanTransferHelper}). Jalur REST punya aksi
 * tersendiri ({@code reimbursement_ajukan_transfer} →
 * {@code TransferDpcUtil.ajukan}) yang menolak dokumen belum DISETUJUI.</li>
 * <li><b>Pembayaran &amp; jurnal.</b> Dilaksanakan pada baris DPC, bukan di
 * sini. Akun biaya yang dipakai menjurnal dibaca DPC dari
 * {@code getReimbursementPegawai().getAkun()} (fallback
 * {@link #getAkunBiaya()}), rekening tujuan dari
 * {@link #getRekeningPenerima()} dengan fallback {@code Pegawai.getNorek()},
 * dan nominalnya dari {@link #getNominal()}.</li>
 * </ol>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 *
 * <ul>
 * <li><b>Identitas dokumen</b> — {@link #getId()}, {@link #getKode()},
 * {@link #getKodeUnik()}, {@link #getNama()}, {@link #getDeskripsi()},
 * {@link #getKeterangan()}, {@link #getKategori()}, {@link #toString()}.</li>
 * <li><b>Nominal &amp; rincian</b> — {@link #getNominal()} (+ alias
 * {@link #getNilai()}), {@link #getPajakPersen()}, {@link #getFormula()}
 * (JSON baris biaya), {@link #getDibayarPegawai()}.</li>
 * <li><b>Pelaku</b> — {@link #getPegawai()} (penerima penggantian),
 * {@link #getAtasan()}, {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
 * {@link #getDiputuskanOleh()}, {@link #getDibayarOleh()}.</li>
 * <li><b>Waktu</b> — {@link #getTanggalPengeluaran()},
 * {@link #getTanggalPengajuan()}, {@link #getTanggalPersetujuan()},
 * {@link #getTanggalKeputusan()}, {@link #getTanggalAkuntansi()},
 * {@link #getTanggalPembayaran()}, {@link #getTanggalDirubah()}.</li>
 * <li><b>Anggaran (pola UangMuka)</b> — {@link #getJenisReimbursement()},
 * {@link #getTanpaAnggaran()}, {@link #getWorkspace()},
 * {@link #getSatuanKerja()}, {@link #getAkun()}.</li>
 * <li><b>SOP &amp; keputusan</b> — {@link #getDisposisiSop()},
 * {@link #getStatus()}, {@link #getAktif()}, {@link #getCatatanPengaju()},
 * {@link #getCatatanAtasan()}.</li>
 * <li><b>Hilir</b> — {@link #getDaftarPengajuanTransfer()},
 * {@link #getPenerimaanPengadaanMasterAsset()},
 * {@link #getPostingPengeluaran()}, {@link #getPostingPembayaran()}.</li>
 * </ul>
 *
 * <h2>Hal non-obvious yang WAJIB diketahui sebelum menyunting</h2>
 *
 * <ol>
 * <li><b>Empat getter menulis balik ke field (write-back), dua di antaranya
 * menyentuh jalur uang.</b> {@link #getKodeUnik()}, {@link #getStatus()},
 * {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
 * {@link #getAktif()}, {@link #getWorkspace()}, {@link #getSatuanKerja()} dan
 * {@link #getAkun()} <b>menghitung ulang lalu menimpa</b> field-nya sendiri
 * pada setiap pembacaan. Karena Hibernate dikonfigurasi <i>property access</i>
 * (anotasi ada di getter) + {@code dynamicUpdate}, sekadar <b>membaca</b>
 * entity dalam sesi hidup dapat menerbitkan {@code UPDATE} yang mengubah
 * kolom {@code akun}, {@code satuan_kerja}, {@code workspace},
 * {@code status}, {@code disetujui_oleh} dan {@code tanggal_persetujuan} di
 * basis data. Ini pola yang sama dengan {@code Transaksi.getAkun()} (b73) —
 * lihat butir keamanan di bawah.</li>
 * <li><b>{@link #setDisposisiSop(DisposisiSop)} diam-diam menolak</b> argumen
 * {@code null} atau ber-id {@code null}: tautan lama dipertahankan, tanpa
 * pesan galat. Konsekuensinya dokumen <b>tidak bisa dilepas</b> dari alur SOP
 * lewat setter ini.</li>
 * <li><b>{@link #getStatus()} tidak monoton.</b> Nilai tersimpan
 * {@link #DISETUJUI} akan <b>diturunkan kembali</b> menjadi {@link #DIAJUKAN}
 * begitu {@link #getDisetujuiOleh()} mengembalikan {@code null} — dan getter
 * itu sendiri memaksa {@code null} setiap kali {@code disposisiSop} ada tetapi
 * langkah "setuju"-nya belum terisi. Artinya persetujuan yang ditulis jalur
 * REST bisa <b>hilang saat dibaca</b> untuk dokumen yang ber-SOP, tetapi
 * <b>bertahan</b> untuk dokumen tanpa SOP.</li>
 * <li><b>Sebagian besar blok "pembayaran" adalah kolom tidur.</b> Verifikasi
 * pencarian seluruh repo: {@code setPostingPengeluaran}, {@code setPostingPembayaran},
 * {@code setAkunBiaya}, {@code setMetodePembayaran}, {@code setBankPenerima},
 * {@code setRekeningPenerima}, {@code setAkunPembayaran}, {@code setDibayarOleh},
 * {@code setTanggalPembayaran}, {@code setCatatanPembayaran},
 * {@code setTanggalAkuntansi} dan {@code setLampiranId} <b>tidak pernah
 * dipanggil satu kali pun</b> — kolomnya selalu {@code NULL}. Ikutannya:
 * (a) konstanta {@link #LUNAS} tidak pernah <b>diset</b> di mana pun (hanya
 * dibandingkan), jadi status "Lunas" praktis tak terjangkau dan hitungan
 * "Lunas" di dasbor selalu nol; (b) penjaga "sudah dijurnal sehingga tidak
 * boleh diubah" di {@code ReimbursementApiHelper} yang bersandar pada
 * {@code getPostingPengeluaran() != null} <b>tidak pernah menyala</b> — pola
 * "penjaga mati" yang sama dengan kaki pajak {@link Pertangungjawaban}
 * (batch 74); (c) cabang rekening tujuan di {@link DaftarPengajuanTransfer}
 * selalu jatuh ke {@code Pegawai.getNorek()}.</li>
 * <li><b>Komentar stale di {@link DaftarPengajuanTransfer}.</b> Getter
 * {@code DaftarPengajuanTransfer.getReimbursementPegawai()} membawa komentar
 * "{@code ReimbursementPegawai} tidak {@code @Audited}" — <b>keliru untuk
 * kode saat ini</b>: kelas ini justru ber-{@code @Audited} (lihat anotasi di
 * atas). Anotasi {@code targetAuditMode = NOT_AUDITED} di sana kini hanya
 * redundan, bukan penyelamat boot. Sebaliknya, komentar sejenis di
 * {@link #getJenisReimbursement()} <b>masih benar</b>: {@link JenisReimbursement}
 * memang tidak {@code @Audited}.</li>
 * <li><b>Tanpa kolom tenant.</b> Rantai warisan {@code ReimbursementPegawai
 * extends DataSop extends GeneralValueObject} <b>tidak membawa kolom
 * sekolah/yayasan sama sekali</b> — tabel ini de facto global untuk seluruh
 * instalasi. Lihat butir keamanan.</li>
 * </ol>
 *
 * <h2 id="keamanan">Catatan keamanan &amp; integritas finansial (hasil audit)</h2>
 *
 * <ol>
 * <li><b>Gerbang persetujuan jalur ZK: {@code task_78c0c5c2} TIDAK berlaku di
 * sini (verifikasi negatif).</b> {@code ReimbursementPegawaiAction} memang
 * punya field {@code persetujuan}, tetapi — berbeda dari 10+ Action modul
 * "uang" lain ({@code UangMukaAction}, {@code KasBesarAction},
 * {@code KasKecilAction}, {@code DanaTalanganAction},
 * {@code PertangungjawabanAction}, dst.) — Action ini <b>tidak pernah
 * membacanya dari parameter URL</b> ({@code execution.getParameter("persetujuan")}
 * nihil), tidak ada {@code .zul} yang menyetelnya, dan nilainya selalu
 * dipaksa {@code false}. Field itu sisa rework yang mati. Persetujuan
 * sesungguhnya dipegang mesin SOP, jadi gerbangnya bergantung pada konfigurasi
 * {@code AlurSop}, bukan pada halaman ini.</li>
 * <li><b>TETAPI jalur REST menyediakan persetujuan langsung tanpa gerbang
 * peran yang benar — perluasan {@code task_66986071}.</b>
 * {@code ReimbursementApiHelper.keputusan()} (aksi POS
 * {@code reimbursement_setujui} / {@code _tolak} / {@code _revisi}) menulis
 * {@code setStatus(DISETUJUI)} + {@code setDisetujuiOleh(tbmuser)} langsung,
 * <b>melewati alur SOP sepenuhnya</b>. Gerbangnya adalah
 * {@code bolehAksi(tbmuser, "approve")} yang <b>fail-open</b>: bila
 * {@code tbmuser.hakAkses()} mengembalikan {@code null} (pengguna tanpa peran)
 * method itu {@code return true} — memberi izin penuh, bukan menolak. Pola
 * fail-open yang persis sama dengan {@code task_66986071}, tetapi di sini yang
 * dijaga bukan CRUD master melainkan <b>gerbang persetujuan uang</b>. Selain
 * itu {@code keputusan()} tidak memeriksa apakah penyetuju adalah
 * {@link #getAtasan()} pegawai, tidak melarang <b>menyetujui pengajuan
 * sendiri</b>, dan tidak mengenal batas nominal — sehingga satu pengguna
 * ber-token POS tanpa peran dapat mengajukan lalu menyetujui klaimnya sendiri,
 * yang otomatis mengalir ke DPC dan dibayarkan. (Verifikasi menenangkan:
 * {@code PosApi} tetap menolak permintaan tanpa token valid, dan
 * {@code ReimbursementApiHelper.simpan()} <b>tidak</b> menerima status dari
 * klien — selalu memaksa {@link #DIAJUKAN} — jadi jalur simpan tidak
 * mengulang cacat {@code PertangungjawabanApiHelper} batch 74.)</li>
 * <li><b>Keterjangkauan {@code task_66986071} atas entity ini: TIDAK
 * LANGSUNG, tetapi berdampak.</b> {@code MasterKeuanganApiHelper} — helper
 * yang menjadi asal {@code task_66986071} — memelihara tujuh master keuangan
 * dan yang menyentuh modul ini adalah {@link JenisReimbursement} serta
 * {@code JenisPengeluaran} (rincian), <b>bukan</b> dokumen
 * {@code ReimbursementPegawai} sendiri. Dampak tak langsungnya nyata: untuk
 * jenis "tanpa anggaran", <b>akun biaya yang dijurnal diambil dari
 * {@code JenisReimbursement.getAkun()}</b> (lihat {@link #setAkun(Akun)} yang
 * dipanggil Action/REST dengan {@code jenis.getAkun()}), sehingga pengguna
 * tanpa peran yang lolos fail-open {@code MasterKeuanganApiHelper} dapat
 * <b>mengubah akun tujuan pembebanan seluruh klaim reimbursement</b> tanpa
 * pernah menyentuh dokumennya.</li>
 * <li><b>Getter destruktif menyentuh nominal/akun.</b> {@link #getAkun()}
 * menimpa akun dokumen dengan {@code workspace.getAkun()} setiap dibaca;
 * {@link #getSatuanKerja()} menimpa satuan kerja dengan milik workspace;
 * {@link #getWorkspace()} <b>menghapus</b> tautan anggaran menjadi
 * {@code null} bila {@link #getTanpaAnggaran()} bernilai benar. Dengan
 * {@code dynamicUpdate}, membaca dokumen lama setelah anggarannya diubah
 * dapat <b>memindahkan pembebanan akun jurnal secara retroaktif</b> tanpa
 * jejak tindakan pengguna. Nominalnya sendiri tidak dihitung ulang oleh
 * getter (aman), tetapi akun tujuan pembebanannya bisa berpindah.</li>
 * <li><b>Fail-open cakupan tenant — STRUKTURAL.</b> Entity ini tidak punya
 * kolom {@code sekolah}/{@code yayasan}, dan
 * {@code ReimbursementPegawaiAction.cariData()} tidak menambahkan pembatas
 * tenant apa pun: daftar 300 dokumen terakhir berisi klaim <b>seluruh
 * instalasi</b> lengkap dengan nama pegawai, nominal, dan (lewat DPC)
 * rekening tujuan. Sama polanya dengan {@code task_f1283f4a} pada dasbor
 * kas besar.</li>
 * <li><b>Zero-gate + pewarisan hak lewat menu induk.</b> Tidak satu pun dari
 * {@code ReimbursementPegawaiAction}, {@code ReimbursementDashboardAction},
 * dan {@code ReimbursementLaporanAction} memanggil {@code checkPrevilages()}
 * — halamannya bergantung sepenuhnya pada apakah menu induknya terpasang
 * bagi peran pengguna, pola pewarisan hak menu induk yang sudah dilacak sejak
 * batch 61/73.</li>
 * <li><b>Tulis-dari-jalur-render.</b> {@code ReimbursementRenderer} memasang
 * timer yang memanggil
 * {@link DaftarPengajuanTransfer#simpanReimbursement(ReimbursementPegawai)}
 * untuk setiap baris DISETUJUI yang belum punya DPC — <b>membuka daftar saja
 * sudah menerbitkan baris DPC baru</b>, tanpa tindakan pengguna dan tanpa
 * transaksi eksplisit (pola yang sama dengan batch 71).</li>
 * <li><b>Nomor dokumen berpotensi kembar.</b> {@code generateCode()} menyusun
 * nomor dari {@code Projections.rowCount()} (jumlah <i>baris</i>), bukan dari
 * nomor tertinggi yang pernah terbit — menghapus satu dokumen membuat nomor
 * berikutnya mengulang nomor yang sudah dipakai. Pola identik dengan
 * {@code FormatNis} (batch 69) dan nomor agenda {@code PengajuanSiswa}
 * (batch 70). Di kelas ini efeknya diredam {@link #getKodeUnik()} yang
 * menambahkan id dokumen/disposisi, tetapi nomor yang <b>dilihat dan dicetak
 * pengguna</b> tetap bisa kembar.</li>
 * </ol>
 *
 * @see UangMuka
 * @see Pertangungjawaban
 * @see JenisReimbursement
 * @see DaftarPengajuanTransfer
 * @see ais.database.model.sop.DataSop
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "akunting", name = "reimbursement_pegawai")
public class ReimbursementPegawai extends DataSop {
    /** Versi serialisasi. Sengaja {@code 1L} seperti mayoritas entity repo ini. */
    private static final long serialVersionUID = 1L;

    /**
     * Status awal setiap pengajuan: sudah dikirim pengaju, menunggu keputusan.
     * Juga status <i>fallback</i> {@link #getStatus()} bila kolom kosong dan
     * dipakai ulang oleh jalur REST setiap kali dokumen disimpan kembali.
     */
    public static final String DIAJUKAN = "Diajukan";

    /**
     * Dikembalikan kepada pengaju untuk diperbaiki (bukan ditutup). Status ini
     * <b>khas modul reimbursement</b> — modul Keuangan lain hanya mengenal
     * Diajukan/Disetujui/Ditolak. Hanya diset lewat jalur REST
     * ({@code reimbursement_revisi}); layar ZK tidak memilikinya karena
     * "revisi" di sana ditangani mekanisme kembalikan-disposisi milik SOP.
     */
    public static final String REVISI = "Revisi";

    /**
     * Ditolak. Pada dokumen ber-SOP nilai ini juga <b>disimpulkan</b>
     * {@link #getStatus()} bila langkah akhir disposisi adalah titik penolakan,
     * meskipun kolom {@code status} berisi hal lain.
     */
    public static final String DITOLAK = "Ditolak";

    /**
     * Disetujui — satu-satunya status yang membuat dokumen layak masuk DPC
     * ({@link DaftarPengajuanTransfer}). Pada dokumen ber-SOP nilai ini
     * disimpulkan dari terisinya penyetuju, bukan dari kolom.
     */
    public static final String DISETUJUI = "Disetujui";

    /**
     * Status terminal "sudah dibayar".
     *
     * <p><b>TIDAK PERNAH DISET.</b> Verifikasi seluruh repo: konstanta ini hanya
     * pernah <i>dibandingkan</i> (penjaga di REST helper, tombol Ubah/Hapus,
     * kolom dasbor/laporan) dan tidak pernah muncul di sisi kiri sebuah
     * {@code setStatus(...)}. Karena pelunasan dieksekusi pada baris DPC dan
     * tidak dipantulkan balik ke dokumen ini, status "Lunas" praktis tak
     * terjangkau: hitungan "Lunas" di dasbor selalu nol dan penjaga
     * "sudah lunas tidak boleh diubah" tidak pernah menyala.</p>
     */
    public static final String LUNAS = "Lunas";

    /** Kunci utama tabel; lihat {@link #getId()}. */
    private Long id;
    /** Nomor dokumen yang dilihat pengguna; lihat {@link #getKode()}. */
    private String kode;
    /** Kunci unik per-siklus SOP; lihat {@link #getKodeUnik()}. */
    private String kodeUnik;
    /** Judul pengajuan; lihat {@link #getNama()}. */
    private String nama;
    /** Keterangan bebas; lihat {@link #getKeterangan()}. */
    private String keterangan;
    /** Deskripsi (disamakan dengan judul oleh Action); lihat {@link #getDeskripsi()}. */
    private String deskripsi;
    /** Kategori pengelompokan laporan; lihat {@link #getKategori()}. */
    private String kategori;
    /** Total nilai klaim; lihat {@link #getNominal()}. */
    private Double nominal;
    /** Persentase pajak informatif; lihat {@link #getPajakPersen()}. */
    private Double pajakPersen;
    /** Penanda biaya sudah ditalangi pegawai; lihat {@link #getDibayarPegawai()}. */
    private Boolean dibayarPegawai;
    /** Tanggal biaya benar-benar dikeluarkan; lihat {@link #getTanggalPengeluaran()}. */
    private Date tanggalPengeluaran;
    /** Waktu pengajuan dikirim; lihat {@link #getTanggalPengajuan()}. */
    private Date tanggalPengajuan;
    /** Pegawai penerima penggantian; lihat {@link #getPegawai()}. */
    private Pegawai pegawai;
    /** Atasan langsung pegawai (informatif); lihat {@link #getAtasan()}. */
    private Pegawai atasan;
    /** Pengguna pembuat dokumen; lihat {@link #getDibuatOleh()}. */
    private Tbmuser dibuatOleh;
    /** Catatan dari pengaju; lihat {@link #getCatatanPengaju()}. */
    private String catatanPengaju;
    /** Id lampiran (kolom tidur); lihat {@link #getLampiranId()}. */
    private Long lampiranId;
    /** Status tersimpan — sering ditimpa turunan disposisi; lihat {@link #getStatus()}. */
    private String status;

    // --- Anggaran (pola UangMuka) ---
    /** Anggaran pembebanan; lihat {@link #getWorkspace()}. */
    private Workspace workspace;
    /** Satuan kerja pembebanan; lihat {@link #getSatuanKerja()}. */
    private SatuanKerja satuanKerja;
    /** Akun biaya efektif; lihat {@link #getAkun()}. */
    private Akun akun;
    /** Penanda klaim tanpa anggaran; lihat {@link #getTanpaAnggaran()}. */
    private Boolean tanpaAnggaran;
    /** Jenis reimbursement yang dipilih; lihat {@link #getJenisReimbursement()}. */
    private JenisReimbursement jenisReimbursement;

    // --- Rincian barang/biaya (pola KasKecil: JSON) ---
    /** Rincian baris biaya dalam bentuk JSON; lihat {@link #getFormula()}. */
    private String formula;

    // --- SOP + DPC ---
    /** Instance alur SOP penggerak dokumen; lihat {@link #getDisposisiSop()}. */
    private DisposisiSop disposisiSop;
    /** Baris DPC hasil persetujuan; lihat {@link #getDaftarPengajuanTransfer()}. */
    private DaftarPengajuanTransfer daftarPengajuanTransfer;
    /** BAST penerimaan barang (kolom tidur); lihat {@link #getPenerimaanPengadaanMasterAsset()}. */
    private ais.database.model.asset.PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset;
    /** Penyetuju — biasanya diturunkan dari disposisi; lihat {@link #getDisetujuiOleh()}. */
    private Tbmuser disetujuiOleh;
    /** Waktu persetujuan — diturunkan dari disposisi; lihat {@link #getTanggalPersetujuan()}. */
    private Date tanggalPersetujuan;

    /** Bendera aktif — selalu dihitung ulang; lihat {@link #getAktif()}. */
    private Boolean aktif;
    /** Catatan atasan/penolakan; lihat {@link #getCatatanAtasan()}. */
    private String catatanAtasan;
    /** Pengguna pemutus di jalur REST; lihat {@link #getDiputuskanOleh()}. */
    private Tbmuser diputuskanOleh;
    /** Waktu keputusan di jalur REST; lihat {@link #getTanggalKeputusan()}. */
    private Date tanggalKeputusan;
    /** Tanggal pengakuan akuntansi (kolom tidur); lihat {@link #getTanggalAkuntansi()}. */
    private Date tanggalAkuntansi;
    /** Akun biaya cadangan (kolom tidur); lihat {@link #getAkunBiaya()}. */
    private Akun akunBiaya;
    /** Cap posting pengeluaran (kolom tidur); lihat {@link #getPostingPengeluaran()}. */
    private PostingHistory postingPengeluaran;

    /** Metode pembayaran (kolom tidur); lihat {@link #getMetodePembayaran()}. */
    private String metodePembayaran;
    /** Bank penerima (kolom tidur); lihat {@link #getBankPenerima()}. */
    private String bankPenerima;
    /** Rekening penerima (kolom tidur); lihat {@link #getRekeningPenerima()}. */
    private String rekeningPenerima;
    /** Tanggal pembayaran (kolom tidur); lihat {@link #getTanggalPembayaran()}. */
    private Date tanggalPembayaran;
    /** Catatan pembayaran (kolom tidur); lihat {@link #getCatatanPembayaran()}. */
    private String catatanPembayaran;
    /** Akun kas/bank pembayar (kolom tidur); lihat {@link #getAkunPembayaran()}. */
    private Akun akunPembayaran;
    /** Pengguna pembayar (kolom tidur); lihat {@link #getDibayarOleh()}. */
    private Tbmuser dibayarOleh;
    /** Cap posting pembayaran (kolom tidur); lihat {@link #getPostingPembayaran()}. */
    private PostingHistory postingPembayaran;
    /**
     * Stempel perubahan terakhir. Diinisialisasi di deklarasi agar baris baru
     * tetap punya nilai walau {@code @PreUpdate} belum pernah jalan.
     */
    private Date tanggalDirubah = ais.ui.util.WaktuUtil.getDate();

    /**
     * Kunci utama tabel {@code akunting.reimbursement_pegawai}.
     *
     * <p>{@code insertable = false} karena nilainya dibangkitkan basis data
     * ({@code IDENTITY}).</p>
     *
     * @return id dokumen, atau {@code null} bila belum pernah disimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    /**
     * Menyetel kunci utama. Dipakai Hibernate; kode aplikasi tidak perlu
     * memanggilnya.
     *
     * @param id kunci utama
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Nomor dokumen yang dilihat, dicari, dan dicetak pengguna (mis.
     * {@code RMB-202609-0007}).
     *
     * <p>Dibangkitkan {@code ReimbursementPegawaiAction.generateCode()} pada
     * penyimpanan pertama, mengikuti format kustom entri "018 - Reimbursement
     * Pegawai" pada master Nomor Surat bila ada. Jalur REST memakai
     * {@code Common.getGeneratedBarCode()} sebagai gantinya, sehingga bentuk
     * nomor bisa berbeda antar jalur. Karena penomoran ZK berbasis
     * <i>jumlah baris</i>, nomor bisa kembar setelah ada dokumen dihapus —
     * lihat catatan keamanan kelas.</p>
     *
     * @return nomor dokumen; kolom {@code NOT NULL} sehingga selalu terisi pada
     *         baris tersimpan
     */
    @Column(nullable = false, length = 80)
    public String getKode() { return kode; }
    /**
     * Menyetel nomor dokumen.
     *
     * @param kode nomor dokumen hasil generator
     */
    public void setKode(String kode) { this.kode = kode; }

    /**
     * Kunci unik per-siklus SOP agar penyimpanan ganda (satu kode dipakai ulang
     * di siklus disposisi berbeda) tetap aman. Meniru {@link UangMuka#getKodeUnik}.
     *
     * <p><b>Getter penghitung (write-back).</b> Nilainya tidak pernah dibaca dari
     * kolom: setiap pembacaan menyusun ulang {@code kode + "_" + idDisposisi}
     * (atau {@code kode + "_" + idDokumen} bila belum ada disposisi) lalu
     * <b>menimpa</b> field {@link #kodeUnik}. Karena kolom ini
     * {@code unique = true} dan pemetaannya <i>property access</i>, pembacaan
     * dalam sesi hidup dapat menerbitkan {@code UPDATE} pada kolom unik
     * tersebut.</p>
     *
     * <p><b>Kasus tepi.</b> Untuk dokumen baru yang belum punya id maupun
     * disposisi, hasilnya berakhir {@code "..._null"} — dua dokumen baru yang
     * kebetulan berkode sama akan bertabrakan pada batasan unik. Dalam praktik
     * tidak terjadi karena {@link #getKode()} sudah unik saat dibangkitkan.</p>
     *
     * @return kunci unik gabungan kode dan siklus SOP; tidak pernah {@code null}
     *         selama {@link #getKode()} terisi
     */
    @Column(unique = true)
    public String getKodeUnik() {
        kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
        return kodeUnik;
    }
    /**
     * Menyetel kunci unik. Praktis tidak berpengaruh karena
     * {@link #getKodeUnik()} selalu menghitung ulang; disediakan agar Hibernate
     * dapat memuat kolomnya.
     *
     * @param kodeUnik nilai dari basis data
     */
    public void setKodeUnik(String kodeUnik) { this.kodeUnik = kodeUnik; }

    /**
     * Judul singkat pengajuan (mis. "Perjalanan dinas Surabaya 12-14 Agu").
     *
     * <p>Dipangkas spasi tepi saat dibaca — pemangkasan hanya pada nilai
     * kembalian, field aslinya tidak diubah, jadi getter ini <b>bukan</b>
     * write-back. Wajib diisi di kedua jalur (ZK dan REST).</p>
     *
     * @return judul terpangkas, atau {@code null} bila belum diisi
     */
    @Column(name = "nama", length = 255)
    public String getNama() { return nama == null ? null : nama.trim(); }
    /**
     * Menyetel judul pengajuan.
     *
     * @param nama judul; disimpan apa adanya (pemangkasan dilakukan getter)
     */
    public void setNama(String nama) { this.nama = nama; }

    /**
     * Keterangan bebas dari pengaju; ditampilkan sebagai kolom tersendiri di
     * daftar dan pada cetakan PDF.
     *
     * @return keterangan, atau {@code null}
     */
    @Column(name = "keterangan", columnDefinition = "text")
    public String getKeterangan() { return keterangan; }
    /**
     * Menyetel keterangan bebas.
     *
     * @param keterangan teks keterangan; boleh {@code null}
     */
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    /**
     * Deskripsi dokumen.
     *
     * <p>Dalam praktik <b>redundan dengan {@link #getNama()}</b>: Action ZK
     * menyalin nilai judul ke sini pada setiap simpan
     * ({@code setDeskripsi(nama.getValue().trim())}). Tetap dipertahankan
     * karena {@link #toString()} memakainya sebagai cadangan ketika judul
     * kosong (mis. untuk baris lama hasil impor).</p>
     *
     * @return deskripsi, atau {@code null}
     */
    @Column(columnDefinition = "text")
    public String getDeskripsi() { return deskripsi; }
    /**
     * Menyetel deskripsi dokumen.
     *
     * @param deskripsi teks deskripsi; boleh {@code null}
     */
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    /**
     * Kategori pengelompokan untuk laporan/dasbor (mis. "Transport", "Konsumsi").
     *
     * <p>Daftar pilihannya bukan tabel master melainkan konfigurasi teks
     * {@code kategori_reimbursement_pegawai} yang dibaca
     * {@code ReimbursementLaporanAction} lewat {@code Common.getKonfigurasi} —
     * perhatikan bahwa pembacaan konfigurasi di repo ini <b>menulis nilai
     * bawaan ke basis data</b> bila kuncinya belum ada. Action ZK mengisi
     * nilai {@code "Reimbursement"} bila pengaju membiarkannya kosong.</p>
     *
     * @return kategori, atau {@code null}
     */
    @Column(length = 100)
    public String getKategori() { return kategori; }
    /**
     * Menyetel kategori pengelompokan.
     *
     * @param kategori nama kategori; boleh {@code null}
     */
    public void setKategori(String kategori) { this.kategori = kategori; }

    /**
     * Total nilai klaim yang diminta diganti, dalam rupiah.
     *
     * <p><b>Bukan angka yang diketik pengguna.</b> Kedua jalur menghitungnya
     * sebagai jumlah seluruh baris rincian pada {@link #getFormula()}
     * (ZK: total baris form; REST: {@code hitungRincian(rincian)}), lalu
     * menyimpannya di sini. Nilai inilah yang dibawa
     * {@link DaftarPengajuanTransfer} sebagai nominal transfer dan yang
     * dijumlahkan dasbor/laporan.</p>
     *
     * <p><b>Kasus tepi.</b> Getter menormalkan {@code null} menjadi {@code 0.0}
     * agar penjumlahan laporan tidak melempar NPE; normalisasi ini hanya pada
     * nilai kembalian, kolomnya tetap boleh {@code NULL} di memori meskipun
     * dideklarasikan {@code NOT NULL} di basis data. Tidak ada validasi
     * non-negatif di level entity — nominal negatif akan diterima apa adanya.</p>
     *
     * @return total klaim; {@code 0.0} bila belum dihitung
     */
    @Column(nullable = false)
    public Double getNominal() { return nominal == null ? 0.0 : nominal; }
    /**
     * Menyetel total nilai klaim.
     *
     * @param nominal total rupiah hasil penjumlahan rincian; boleh {@code null}
     */
    public void setNominal(Double nominal) { this.nominal = nominal; }

    /**
     * Alias {@link #getNominal()} agar seragam dengan form pola UangMuka.
     *
     * <p>Bukan properti terpetakan (tanpa {@code @Column}) — murni jembatan
     * penamaan supaya komponen form generik yang mengharapkan properti
     * {@code nilai} pada dokumen keuangan tetap bekerja. Dipakai antara lain
     * oleh cetakan dan komponen ringkasan; setternya sendiri tidak dipanggil
     * di mana pun saat ini.</p>
     *
     * @return nilai yang sama dengan {@link #getNominal()}
     */
    public Double getNilai() { return getNominal(); }
    /**
     * Alias {@link #setNominal(Double)}.
     *
     * @param nilai total rupiah klaim
     */
    public void setNilai(Double nilai) { this.nominal = nilai; }

    /**
     * Persentase pajak yang melekat pada klaim (mis. {@code 2.0} untuk PPh 2%).
     *
     * <p><b>Informatif saja.</b> Tidak ada perhitungan yang memakai kolom ini:
     * {@link #getNominal()} tidak dipotong dengannya, dan
     * {@link DaftarPengajuanTransfer} mentransfer nominal penuh. Satu-satunya
     * penulisnya adalah jalur REST ({@code request.optDouble("pajakPersen", 0)});
     * form ZK tidak menampilkannya.</p>
     *
     * @return persentase pajak; {@code 0.0} bila belum diisi
     */
    @Column(name = "pajak_persen")
    public Double getPajakPersen() { return pajakPersen == null ? 0.0 : pajakPersen; }
    /**
     * Menyetel persentase pajak.
     *
     * @param pajakPersen persentase (bukan pecahan); boleh {@code null}
     */
    public void setPajakPersen(Double pajakPersen) { this.pajakPersen = pajakPersen; }

    /**
     * Penanda bahwa biaya benar-benar sudah ditalangi lebih dulu oleh pegawai.
     *
     * <p>Hanya ditulis jalur REST (default {@code true} bila klien tidak
     * mengirimnya). Nilainya tidak dipakai sebagai penjaga di mana pun —
     * dokumen dengan penanda {@code false} tetap dapat disetujui dan masuk DPC.
     * Getter menormalkan {@code null} menjadi {@link Boolean#FALSE}.</p>
     *
     * @return {@code true} bila ditandai sudah ditalangi pegawai
     */
    @Column(name = "dibayar_pegawai")
    public Boolean getDibayarPegawai() { return dibayarPegawai == null ? Boolean.FALSE : dibayarPegawai; }
    /**
     * Menyetel penanda biaya sudah ditalangi pegawai.
     *
     * @param dibayarPegawai penanda; boleh {@code null} (dibaca sebagai {@code false})
     */
    public void setDibayarPegawai(Boolean dibayarPegawai) { this.dibayarPegawai = dibayarPegawai; }

    /**
     * Tanggal biaya benar-benar dikeluarkan pegawai (tanggal pada bukti/nota).
     *
     * <p>Wajib diisi di kedua jalur. Disimpan sebagai {@code DATE} murni (tanpa
     * jam) dan menjadi salah satu sumbu penyaringan periode di laporan; jangan
     * dikacaukan dengan {@link #getTanggalPengajuan()} yang mencatat kapan
     * klaimnya diajukan.</p>
     *
     * @return tanggal pengeluaran, atau {@code null} pada dokumen lama
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "tanggal_pengeluaran")
    public Date getTanggalPengeluaran() { return tanggalPengeluaran; }
    /**
     * Menyetel tanggal pengeluaran biaya.
     *
     * @param tanggalPengeluaran tanggal pada bukti; boleh {@code null}
     */
    public void setTanggalPengeluaran(Date tanggalPengeluaran) { this.tanggalPengeluaran = tanggalPengeluaran; }

    /**
     * Waktu pengajuan pertama kali dikirim.
     *
     * <p>Diisi sekali saja ("sticky"): kedua jalur hanya menuliskannya bila
     * masih {@code null}, sehingga penyimpanan ulang/revisi tidak menggeser
     * waktu pengajuan. Inilah kolom yang dipakai filter rentang tanggal pada
     * layar daftar ({@code Restrictions.ge/le("tanggalPengajuan", ...)}) —
     * konsekuensinya dokumen lama tanpa nilai ini <b>hilang</b> dari hasil
     * pencarian begitu pengguna mengisi salah satu batas tanggal.</p>
     *
     * @return waktu pengajuan, atau {@code null}
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_pengajuan")
    public Date getTanggalPengajuan() { return tanggalPengajuan; }
    /**
     * Menyetel waktu pengajuan.
     *
     * @param tanggalPengajuan waktu kirim; boleh {@code null}
     */
    public void setTanggalPengajuan(Date tanggalPengajuan) { this.tanggalPengajuan = tanggalPengajuan; }

    /**
     * Pegawai penerima penggantian — pihak yang uangnya akan dikembalikan.
     *
     * <p>Wajib dipilih dari daftar di kedua jalur. Dari relasi inilah
     * {@link DaftarPengajuanTransfer} mengambil bank dan nomor rekening tujuan
     * transfer ({@code Pegawai.getBank()} / {@code Pegawai.getNorek()}), karena
     * {@link #getRekeningPenerima()} tidak pernah terisi. Getter memanggil
     * {@link ais.database.model.GeneralValueObject#check(Object)} untuk
     * meresolusi proxy lazy sebelum mengembalikannya.</p>
     *
     * <p><b>Catatan kontrol akses.</b> Tidak ada pembatas bahwa pegawai yang
     * dipilih harus pengguna yang sedang login — pengguna mana pun yang bisa
     * membuka layar/aksi ini dapat mengajukan klaim atas nama pegawai lain.</p>
     *
     * @return pegawai penerima, atau {@code null} pada dokumen belum lengkap
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pegawai")
    public Pegawai getPegawai() { pegawai = check(pegawai); return pegawai; }
    /**
     * Menyetel pegawai penerima penggantian.
     *
     * @param pegawai pegawai tujuan; boleh {@code null}
     */
    public void setPegawai(Pegawai pegawai) { this.pegawai = pegawai; }

    /**
     * Atasan langsung pegawai pada saat pengajuan dibuat.
     *
     * <p><b>Informatif, bukan gerbang.</b> Action mengisinya sekali dari
     * {@code Pegawai.getAtasanlangsung()} — dan bila pegawai tidak punya atasan
     * terdaftar, <b>dirinya sendiri</b> yang dicatat sebagai atasan
     * ({@code p.getAtasanlangsung() == null ? p : ...}). Tidak ada satu pun
     * jalur persetujuan (SOP maupun REST) yang membandingkan penyetuju dengan
     * nilai kolom ini, sehingga relasi ini tidak membatasi siapa pun.</p>
     *
     * @return atasan langsung, atau {@code null} pada dokumen lama
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atasan")
    public Pegawai getAtasan() { atasan = check(atasan); return atasan; }
    /**
     * Menyetel atasan langsung pegawai.
     *
     * @param atasan pegawai atasan; boleh {@code null}
     */
    public void setAtasan(Pegawai atasan) { this.atasan = atasan; }

    /**
     * Pengguna yang membuat dokumen ini.
     *
     * <p><b>Getter write-back berkondisi.</b> Bila dokumen sudah tertaut alur
     * SOP dan langkah awal disposisi ({@code disposisiStart}) punya pengaju,
     * nilai field <b>ditimpa</b> dengan pengaju disposisi tersebut — sumber
     * kebenaran identitas pembuat berpindah ke mesin SOP, dan pengisian awal
     * dari sesi ({@code Common.getCurrentUser()}) dianggap sementara.
     * Konsekuensi teknis: dengan {@code dynamicUpdate}, membaca dokumen ber-SOP
     * dalam sesi hidup dapat menerbitkan {@code UPDATE} pada kolom
     * {@code dibuat_oleh}.</p>
     *
     * <p>Jalur REST menuliskannya hanya bila masih {@code null}, sekaligus
     * sebagai penanda "dokumen baru" untuk mengisi
     * {@link #getTanggalPengajuan()}.</p>
     *
     * @return pengguna pembuat (atau pengaju disposisi awal), atau {@code null}
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "dibuat_oleh")
    public Tbmuser getDibuatOleh() {
        dibuatOleh = check(dibuatOleh);
        if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
                && getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
            dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
        }
        return dibuatOleh;
    }
    /**
     * Menyetel pengguna pembuat dokumen.
     *
     * @param dibuatOleh pengguna pembuat; boleh {@code null}
     */
    public void setDibuatOleh(Tbmuser dibuatOleh) { this.dibuatOleh = dibuatOleh; }

    /**
     * Catatan bebas dari pengaju untuk penyetuju (penjelasan konteks klaim).
     *
     * <p>Hanya ditulis jalur REST; form ZK memakai {@link #getKeterangan()}.
     * Berpasangan dengan {@link #getCatatanAtasan()} yang merupakan arah
     * sebaliknya.</p>
     *
     * @return catatan pengaju, atau {@code null}
     */
    @Column(name = "catatan_pengaju", columnDefinition = "text")
    public String getCatatanPengaju() { return catatanPengaju; }
    /**
     * Menyetel catatan pengaju.
     *
     * @param catatanPengaju teks catatan; boleh {@code null}
     */
    public void setCatatanPengaju(String catatanPengaju) { this.catatanPengaju = catatanPengaju; }

    /**
     * Id berkas lampiran bukti pengeluaran.
     *
     * <p><b>Kolom tidur.</b> Verifikasi seluruh repo: {@code setLampiranId}
     * tidak pernah dipanggil, sehingga kolom ini selalu {@code NULL}. Lampiran
     * bukti dalam praktik menempel pada {@link DisposisiSop} (mekanisme lampiran
     * SOP), bukan pada dokumen ini. Pertahankan saat menyunting — kemungkinan
     * sisa rancangan pra-rework yang dibiarkan untuk kompatibilitas skema.</p>
     *
     * @return id lampiran; praktis selalu {@code null}
     */
    @Column(name = "lampiran_id")
    public Long getLampiranId() { return lampiranId; }
    /**
     * Menyetel id lampiran. Tidak dipanggil di mana pun saat ini.
     *
     * @param lampiranId id berkas lampiran; boleh {@code null}
     */
    public void setLampiranId(Long lampiranId) { this.lampiranId = lampiranId; }

    /**
     * Status diturunkan dari disposisi SOP (persetujuan di akhir alur): DISETUJUI
     * bila penyetuju terisi, DITOLAK bila langkah akhir adalah titik penolakan,
     * selain itu memakai nilai tersimpan (default DIAJUKAN). Meniru
     * {@link UangMuka#getStatus}.
     *
     * <p><b>Urutan evaluasi persisnya</b> (penting karena aturan belakangan
     * menimpa aturan sebelumnya):</p>
     * <ol>
     * <li>Bila {@link #getDisetujuiOleh()} tidak {@code null} → {@link #DISETUJUI}.</li>
     * <li>Bila tidak, dan nilai tersimpan kebetulan {@link #DISETUJUI} → nilainya
     * <b>diturunkan kembali</b> menjadi {@link #DIAJUKAN}. Inilah alasan status
     * di sini tidak monoton: persetujuan yang pernah tercatat bisa "hilang"
     * hanya karena penyetujunya tidak lagi terselesaikan dari disposisi.</li>
     * <li>Bila langkah akhir disposisi ({@code disposisiEnd}) menunjuk
     * {@code AlurSop} yang ditandai sebagai titik penolakan →
     * {@link #DITOLAK}, menimpa hasil langkah 1 sekalipun.</li>
     * <li>Nilai kosong/blank dikembalikan sebagai {@link #DIAJUKAN}.</li>
     * </ol>
     *
     * <p><b>Efek samping.</b> Getter ini <b>menimpa field {@link #status}</b>
     * pada langkah 1-3; dengan pemetaan <i>property access</i> dan
     * {@code dynamicUpdate}, membacanya dalam sesi hidup dapat menerbitkan
     * {@code UPDATE} pada kolom {@code status}.</p>
     *
     * <p><b>Kasus tepi.</b> {@link #LUNAS} dan {@link #REVISI} tidak pernah
     * dihasilkan logika di atas — keduanya hanya bisa muncul dari nilai
     * tersimpan; dan karena {@code LUNAS} tidak pernah ditulis siapa pun, satu-
     * satunya status non-turunan yang nyata di produksi adalah {@code REVISI}
     * dari jalur REST. Perhatikan pula bahwa langkah 3 <b>tidak</b> mengosongkan
     * {@link #getDisetujuiOleh()}, sehingga dokumen dapat tampil "Ditolak"
     * sambil tetap menampilkan nama penyetuju di daftar.</p>
     *
     * <p><b>Dipanggil dari:</b> renderer daftar, dasbor, laporan, penjaga tombol
     * Ubah/Hapus, {@code TransferDpcUtil.ajukan}, dan penjaga status di
     * {@code ReimbursementApiHelper}.</p>
     *
     * @return salah satu konstanta status; tidak pernah {@code null} maupun kosong
     */
    @Column(length = 30)
    public String getStatus() {
        if (getDisetujuiOleh() != null) {
            status = DISETUJUI;
        } else if (status != null && status.equals(DISETUJUI)) {
            status = DIAJUKAN;
        }

        DisposisiSop d = getDisposisiSop();
        if (d != null && d.getDisposisiEnd() != null && d.getDisposisiEnd().getAlurSop() != null
                && d.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
            status = DITOLAK;
        }

        return status == null || status.trim().isEmpty() ? DIAJUKAN : status;
    }
    /**
     * Menyetel status dokumen.
     *
     * <p><b>Efek samping.</b> Menyetel {@link #DITOLAK} <b>ikut mengosongkan</b>
     * {@link #setDisetujuiOleh(Tbmuser)} dan
     * {@link #setTanggalPersetujuan(Date)}, supaya jejak persetujuan lama tidak
     * tertinggal pada dokumen yang ditolak. Perhatikan bahwa pembersihan ini
     * <b>tidak</b> terjadi pada penolakan yang disimpulkan {@link #getStatus()}
     * dari disposisi — hanya pada penyetelan eksplisit (jalur REST).</p>
     *
     * <p>Nilai lain disimpan apa adanya tanpa validasi terhadap daftar
     * konstanta; string sembarang akan diterima dan ditampilkan mentah di
     * daftar.</p>
     *
     * @param status salah satu konstanta status; boleh {@code null}
     */
    public void setStatus(String status) {
        if (status != null && status.equals(DITOLAK)) {
            setDisetujuiOleh(null);
            setTanggalPersetujuan(null);
        }
        this.status = status;
    }

    /**
     * Catatan atasan/pemutus — alasan penolakan atau permintaan revisi.
     *
     * <p>Ditulis jalur REST {@code keputusan()} dan diwajibkan tidak kosong
     * untuk keputusan tolak/revisi, agar pengaju tahu apa yang harus
     * diperbaiki. Pada keputusan setujui catatannya opsional.</p>
     *
     * @return catatan atasan, atau {@code null}
     */
    @Column(name = "catatan_atasan", columnDefinition = "text")
    public String getCatatanAtasan() { return catatanAtasan; }
    /**
     * Menyetel catatan atasan/pemutus.
     *
     * @param catatanAtasan teks catatan; boleh {@code null}
     */
    public void setCatatanAtasan(String catatanAtasan) { this.catatanAtasan = catatanAtasan; }

    /**
     * Pengguna yang mengambil keputusan terakhir (setujui/tolak/revisi) lewat
     * jalur REST.
     *
     * <p>Berbeda dari {@link #getDisetujuiOleh()}, kolom ini <b>tidak
     * diturunkan</b> dari disposisi dan tetap terisi walau keputusannya adalah
     * penolakan — jadi inilah satu-satunya jejak identitas pemutus yang
     * bertahan untuk dokumen yang ditolak lewat REST. Persetujuan yang terjadi
     * di mesin SOP tidak mengisi kolom ini sama sekali.</p>
     *
     * @return pengguna pemutus, atau {@code null} bila keputusan datang dari SOP
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diputuskan_oleh")
    public Tbmuser getDiputuskanOleh() { diputuskanOleh = check(diputuskanOleh); return diputuskanOleh; }
    /**
     * Menyetel pengguna pemutus.
     *
     * @param diputuskanOleh pengguna; boleh {@code null}
     */
    public void setDiputuskanOleh(Tbmuser diputuskanOleh) { this.diputuskanOleh = diputuskanOleh; }

    /**
     * Waktu keputusan jalur REST diambil.
     *
     * <p>Selalu diisi {@code WaktuUtil.getDate()} (waktu server) oleh
     * {@code ReimbursementApiHelper.keputusan()}, tanpa opsi menyetel dari
     * klien — berbeda dari {@link #getTanggalPersetujuan()} yang justru
     * <b>boleh</b> ditentukan klien.</p>
     *
     * @return waktu keputusan, atau {@code null}
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_keputusan")
    public Date getTanggalKeputusan() { return tanggalKeputusan; }
    /**
     * Menyetel waktu keputusan.
     *
     * @param tanggalKeputusan waktu keputusan; boleh {@code null}
     */
    public void setTanggalKeputusan(Date tanggalKeputusan) { this.tanggalKeputusan = tanggalKeputusan; }

    /**
     * Tanggal pengakuan akuntansi (periode jurnal yang dituju).
     *
     * <p><b>Kolom tidur.</b> {@code setTanggalAkuntansi} tidak pernah dipanggil
     * di seluruh repo, jadi kolom ini selalu {@code NULL}. Tanggal jurnal yang
     * sebenarnya ditentukan {@link DaftarPengajuanTransfer} saat DPC diproses.</p>
     *
     * @return tanggal akuntansi; praktis selalu {@code null}
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "tanggal_akuntansi")
    public Date getTanggalAkuntansi() { return tanggalAkuntansi; }
    /**
     * Menyetel tanggal pengakuan akuntansi. Tidak dipanggil di mana pun saat ini.
     *
     * @param tanggalAkuntansi tanggal jurnal; boleh {@code null}
     */
    public void setTanggalAkuntansi(Date tanggalAkuntansi) { this.tanggalAkuntansi = tanggalAkuntansi; }

    /**
     * Akun biaya cadangan pada dokumen.
     *
     * <p><b>Kolom tidur di sisi tulis, HIDUP di sisi baca.</b>
     * {@code setAkunBiaya} tidak pernah dipanggil (selalu {@code NULL}), tetapi
     * {@link DaftarPengajuanTransfer} <b>membacanya sebagai fallback</b> ketika
     * {@link #getAkun()} kosong untuk menentukan akun pembebanan jurnal. Karena
     * penulisnya tidak ada, fallback itu tidak pernah menolong: dokumen tanpa
     * {@link #getAkun()} akan berujung tanpa akun sama sekali dan
     * <b>dilewati diam-diam</b> oleh mesin posting — gejala klasik yang sulit
     * dilacak karena tidak ada pesan galat di dokumennya.</p>
     *
     * @return akun biaya cadangan; praktis selalu {@code null}
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "akun_biaya")
    public Akun getAkunBiaya() { akunBiaya = check(akunBiaya); return akunBiaya; }
    /**
     * Menyetel akun biaya cadangan. Tidak dipanggil di mana pun saat ini.
     *
     * @param akunBiaya akun beban; boleh {@code null}
     */
    public void setAkunBiaya(Akun akunBiaya) { this.akunBiaya = akunBiaya; }

    /**
     * Cap posting jurnal pengeluaran (pengakuan beban) untuk dokumen ini.
     *
     * <p><b>Kolom tidur — dan penjaganya mati.</b> {@code setPostingPengeluaran}
     * tidak pernah dipanggil di seluruh repo, sehingga kolom ini selalu
     * {@code NULL}. Padahal {@code ReimbursementApiHelper} memakai
     * {@code getPostingPengeluaran() != null} sebagai penjaga "sudah dijurnal
     * sehingga tidak boleh diubah/disetujui/ditolak" di tiga tempat — penjaga
     * yang <b>tidak pernah menyala</b>. Akibatnya dokumen yang jurnalnya sudah
     * terbentuk lewat DPC tetap bisa diubah nominalnya atau dibalik statusnya
     * lewat REST. Pola "penjaga mati" ini sama dengan kaki pajak
     * {@link Pertangungjawaban} (batch 74).</p>
     *
     * @return cap posting pengeluaran; praktis selalu {@code null}
     * @see PostingHistory
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "posting_pengeluaran")
    public PostingHistory getPostingPengeluaran() { postingPengeluaran = check(postingPengeluaran); return postingPengeluaran; }
    /**
     * Menyetel cap posting jurnal pengeluaran. Tidak dipanggil di mana pun saat ini.
     *
     * @param postingPengeluaran cap posting; boleh {@code null}
     */
    public void setPostingPengeluaran(PostingHistory postingPengeluaran) { this.postingPengeluaran = postingPengeluaran; }

    /**
     * Metode pembayaran penggantian (mis. "Transfer", "Tunai").
     *
     * <p><b>Kolom tidur</b> — tidak pernah ditulis. Metode sesungguhnya
     * ditentukan pada baris DPC ({@code CaraPembayaranTransfer}).</p>
     *
     * @return metode pembayaran; praktis selalu {@code null}
     */
    @Column(name = "metode_pembayaran", length = 20)
    public String getMetodePembayaran() { return metodePembayaran; }
    /**
     * Menyetel metode pembayaran. Tidak dipanggil di mana pun saat ini.
     *
     * @param metodePembayaran nama metode; boleh {@code null}
     */
    public void setMetodePembayaran(String metodePembayaran) { this.metodePembayaran = metodePembayaran; }

    /**
     * Nama bank penerima transfer penggantian.
     *
     * <p><b>Kolom tidur</b> — tidak pernah ditulis. {@link DaftarPengajuanTransfer}
     * mengambil bank tujuan dari {@code getPegawai().getBank()} (lalu
     * {@link #getAkunPembayaran()}, lalu {@link #getAkun()}) dan sama sekali
     * tidak melirik kolom ini.</p>
     *
     * @return nama bank penerima; praktis selalu {@code null}
     */
    @Column(name = "bank_penerima", length = 150)
    public String getBankPenerima() { return bankPenerima; }
    /**
     * Menyetel nama bank penerima. Tidak dipanggil di mana pun saat ini.
     *
     * @param bankPenerima nama bank; boleh {@code null}
     */
    public void setBankPenerima(String bankPenerima) { this.bankPenerima = bankPenerima; }

    /**
     * Nomor rekening penerima transfer penggantian.
     *
     * <p><b>Kolom tidur di sisi tulis, HIDUP di sisi baca.</b>
     * {@code setRekeningPenerima} tidak pernah dipanggil, tetapi
     * {@link DaftarPengajuanTransfer} memeriksanya lebih dulu sebagai nomor
     * rekening tujuan sebelum jatuh ke {@code getPegawai().getNorek()}. Karena
     * kolomnya selalu kosong, cabang pertama itu praktis mati dan seluruh
     * transfer selalu memakai rekening master pegawai — artinya klaim
     * <b>tidak bisa</b> diarahkan ke rekening lain per dokumen (secara
     * kebetulan justru menutup satu vektor pengalihan dana).</p>
     *
     * @return nomor rekening penerima; praktis selalu {@code null}
     */
    @Column(name = "rekening_penerima", length = 100)
    public String getRekeningPenerima() { return rekeningPenerima; }
    /**
     * Menyetel nomor rekening penerima. Tidak dipanggil di mana pun saat ini.
     *
     * @param rekeningPenerima nomor rekening; boleh {@code null}
     */
    public void setRekeningPenerima(String rekeningPenerima) { this.rekeningPenerima = rekeningPenerima; }

    /**
     * Tanggal penggantian dibayarkan kepada pegawai.
     *
     * <p><b>Kolom tidur</b> — tidak pernah ditulis; tanggal realisasi ada pada
     * baris DPC. Bersama {@link #LUNAS} yang juga tak pernah diset, inilah
     * sebabnya dokumen ini tidak pernah menampilkan bukti pelunasan sendiri.</p>
     *
     * @return tanggal pembayaran; praktis selalu {@code null}
     */
    @Temporal(TemporalType.DATE)
    @Column(name = "tanggal_pembayaran")
    public Date getTanggalPembayaran() { return tanggalPembayaran; }
    /**
     * Menyetel tanggal pembayaran. Tidak dipanggil di mana pun saat ini.
     *
     * @param tanggalPembayaran tanggal realisasi; boleh {@code null}
     */
    public void setTanggalPembayaran(Date tanggalPembayaran) { this.tanggalPembayaran = tanggalPembayaran; }

    /**
     * Catatan petugas keuangan saat membayarkan penggantian.
     *
     * <p><b>Kolom tidur</b> — tidak pernah ditulis.</p>
     *
     * @return catatan pembayaran; praktis selalu {@code null}
     */
    @Column(name = "catatan_pembayaran", columnDefinition = "text")
    public String getCatatanPembayaran() { return catatanPembayaran; }
    /**
     * Menyetel catatan pembayaran. Tidak dipanggil di mana pun saat ini.
     *
     * @param catatanPembayaran teks catatan; boleh {@code null}
     */
    public void setCatatanPembayaran(String catatanPembayaran) { this.catatanPembayaran = catatanPembayaran; }

    /**
     * Akun kas/bank sumber dana pembayaran penggantian.
     *
     * <p><b>Kolom tidur di sisi tulis, HIDUP di sisi baca.</b> Tidak pernah
     * ditulis, namun dibaca {@link DaftarPengajuanTransfer} sebagai cadangan
     * kedua untuk menentukan bank sumber. Akun kas/bank yang sesungguhnya
     * dipakai berasal dari master {@code CaraPembayaranTransfer} pada DPC.</p>
     *
     * @return akun kas/bank pembayar; praktis selalu {@code null}
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "akun_pembayaran")
    public Akun getAkunPembayaran() { akunPembayaran = check(akunPembayaran); return akunPembayaran; }
    /**
     * Menyetel akun kas/bank pembayar. Tidak dipanggil di mana pun saat ini.
     *
     * @param akunPembayaran akun kas/bank; boleh {@code null}
     */
    public void setAkunPembayaran(Akun akunPembayaran) { this.akunPembayaran = akunPembayaran; }

    /**
     * Pengguna (petugas keuangan) yang membayarkan penggantian.
     *
     * <p><b>Kolom tidur</b> — tidak pernah ditulis; jejak pembayar ada pada
     * baris DPC.</p>
     *
     * @return pengguna pembayar; praktis selalu {@code null}
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dibayar_oleh")
    public Tbmuser getDibayarOleh() { dibayarOleh = check(dibayarOleh); return dibayarOleh; }
    /**
     * Menyetel pengguna pembayar. Tidak dipanggil di mana pun saat ini.
     *
     * @param dibayarOleh pengguna; boleh {@code null}
     */
    public void setDibayarOleh(Tbmuser dibayarOleh) { this.dibayarOleh = dibayarOleh; }

    /**
     * Cap posting jurnal pembayaran (pelunasan kas/bank) untuk dokumen ini.
     *
     * <p><b>Kolom tidur</b> — tidak pernah ditulis. Jurnal pelunasan dibentuk
     * dari baris DPC, yang menyimpan cap postingnya sendiri.</p>
     *
     * @return cap posting pembayaran; praktis selalu {@code null}
     * @see PostingHistory
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "posting_pembayaran")
    public PostingHistory getPostingPembayaran() { postingPembayaran = check(postingPembayaran); return postingPembayaran; }
    /**
     * Menyetel cap posting jurnal pembayaran. Tidak dipanggil di mana pun saat ini.
     *
     * @param postingPembayaran cap posting; boleh {@code null}
     */
    public void setPostingPembayaran(PostingHistory postingPembayaran) { this.postingPembayaran = postingPembayaran; }

    // =========================================================================
    // Anggaran (pola UangMuka)
    // =========================================================================

    /**
     * Penanda bahwa klaim ini <b>tidak</b> membebani anggaran (Workspace).
     *
     * <p>Bukan pilihan bebas pengaju: kedua jalur menurunkannya dari master
     * ({@code !JenisReimbursement.getMenggunakanAnggaran()}). Bila
     * {@code true}, {@link #getWorkspace()} dipaksa {@code null} dan akun biaya
     * diambil dari {@code JenisReimbursement.getAkun()}.</p>
     *
     * <p><b>Perhatikan:</b> properti ini tidak beranotasi {@code @Column},
     * namun Hibernate tetap memetakannya karena kelas ini memakai
     * <i>property access</i> dan properti publik tanpa {@code @Transient}
     * dianggap terpetakan (kolom {@code tanpa_anggaran}).</p>
     *
     * @return {@code true} bila klaim tidak membebani anggaran; {@code false}
     *         bila belum diisi
     */
    public Boolean getTanpaAnggaran() { return tanpaAnggaran == null ? false : tanpaAnggaran; }
    /**
     * Menyetel penanda klaim tanpa anggaran.
     *
     * @param tanpaAnggaran penanda; boleh {@code null} (dibaca sebagai {@code false})
     */
    public void setTanpaAnggaran(Boolean tanpaAnggaran) { this.tanpaAnggaran = tanpaAnggaran; }

    /**
     * Jenis reimbursement yang dipilih pengaju (menentukan wajib-anggaran vs akun tetap).
     *
     * <p>Wajib dipilih di kedua jalur — pengajuan ditolak bila kosong. Master
     * inilah yang menentukan dua hal sekaligus: apakah pengaju harus memilih
     * Anggaran ({@code menggunakanAnggaran}), dan — untuk jenis tanpa anggaran —
     * <b>akun biaya mana yang akan dibebani jurnal</b> ({@code JenisReimbursement.getAkun()}
     * disalin ke {@link #setAkun(Akun)} saat simpan).</p>
     *
     * <p><b>Catatan keamanan.</b> Karena akun pembebanan berasal dari master
     * ini, siapa pun yang bisa menyunting {@link JenisReimbursement} bisa
     * memindahkan pembebanan seluruh klaim tanpa-anggaran — dan gerbang REST
     * master itu ({@code MasterKeuanganApiHelper.bolehAksi}) fail-open untuk
     * pengguna tanpa peran ({@code task_66986071}). Lihat
     * <a href="#keamanan">catatan keamanan kelas</a>.</p>
     *
     * @return jenis reimbursement, atau {@code null} pada dokumen lama
     * @see JenisReimbursement
     */
    // targetAuditMode NOT_AUDITED WAJIB: JenisReimbursement tidak @Audited, sedangkan
    // entitas ini @Audited -- tanpa ini Envers menolak boot ("could not init listeners").
    @Audited(targetAuditMode = org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "jenis_reimbursement", nullable = true)
    public JenisReimbursement getJenisReimbursement() { jenisReimbursement = check(jenisReimbursement); return jenisReimbursement; }
    /**
     * Menyetel jenis reimbursement.
     *
     * @param jenisReimbursement master jenis; boleh {@code null}
     */
    public void setJenisReimbursement(JenisReimbursement jenisReimbursement) { this.jenisReimbursement = jenisReimbursement; }

    /**
     * Anggaran (Workspace) yang dibebani klaim ini.
     *
     * <p><b>Getter destruktif.</b> Bila {@link #getTanpaAnggaran()} bernilai
     * benar, field {@link #workspace} <b>dikosongkan menjadi {@code null}</b> —
     * bukan sekadar disembunyikan dari nilai kembalian. Dengan
     * <i>property access</i> + {@code dynamicUpdate}, membaca dokumen dalam
     * sesi hidup setelah jenisnya diubah menjadi "tanpa anggaran" cukup untuk
     * menerbitkan {@code UPDATE} yang <b>memutus tautan anggaran secara
     * permanen</b> — riwayat pembebanan anggaran dokumen lama hilang tanpa
     * tindakan pengguna.</p>
     *
     * <p>Selain itu getter ini adalah sumber turunan bagi
     * {@link #getSatuanKerja()} dan {@link #getAkun()}, sehingga efek di atas
     * merambat ke keduanya.</p>
     *
     * @return anggaran pembebanan, atau {@code null} bila klaim tanpa anggaran
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace", nullable = true)
    public Workspace getWorkspace() {
        if (getTanpaAnggaran()) {
            workspace = null;
        } else {
            workspace = check(workspace);
        }
        return workspace;
    }
    /**
     * Menyetel anggaran pembebanan.
     *
     * @param workspace anggaran; {@code null} untuk klaim tanpa anggaran
     */
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }

    /**
     * Satuan kerja (unit) yang dibebani klaim ini.
     *
     * <p><b>Getter destruktif.</b> Bila anggaran terisi dan anggaran itu punya
     * satuan kerja, field {@link #satuanKerja} <b>ditimpa</b> dengan satuan
     * kerja milik anggaran — nilai yang dipilih pengaju kalah oleh nilai
     * turunan. Baru bila anggaran kosong, nilai tersimpan dipakai (setelah
     * resolusi proxy). Sama seperti {@link #getWorkspace()}, penimpaan ini bisa
     * ikut tersimpan ke basis data hanya dengan membaca dokumen.</p>
     *
     * <p><b>Kasus tepi.</b> Untuk klaim tanpa anggaran, Action mengisi satuan
     * kerja dari {@code JenisReimbursement.getSatuanKerja()} saat simpan,
     * sehingga unit pembebanan pun mengikuti master.</p>
     *
     * @return satuan kerja pembebanan, atau {@code null}
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "satuan_kerja", nullable = true)
    public SatuanKerja getSatuanKerja() {
        if (getWorkspace() != null && getWorkspace().getSatuanKerja() != null) {
            satuanKerja = getWorkspace().getSatuanKerja();
        } else {
            satuanKerja = check(satuanKerja);
        }
        return satuanKerja;
    }
    /**
     * Menyetel satuan kerja pembebanan.
     *
     * @param satuanKerja unit pembebanan; boleh {@code null}
     */
    public void setSatuanKerja(SatuanKerja satuanKerja) { this.satuanKerja = satuanKerja; }

    /**
     * Akun biaya efektif yang akan dibebani jurnal atas klaim ini.
     *
     * <p><b>Getter destruktif — titik paling sensitif kelas ini.</b> Setelah
     * meresolusi proxy, bila anggaran terisi maka field {@link #akun}
     * <b>ditimpa</b> dengan {@code workspace.getAkun()}. Karena pemetaannya
     * <i>property access</i> dan entity ber-{@code dynamicUpdate}, sekadar
     * <b>membaca</b> dokumen dalam sesi Hibernate hidup dapat menerbitkan
     * {@code UPDATE} pada kolom {@code akun} — memindahkan atribusi akun buku
     * besar dokumen lama ke akun anggaran yang <i>berlaku sekarang</i>. Pola
     * yang sama persis dengan {@code Transaksi.getAkun()} (batch 73).
     * Konsekuensi praktis: mengubah akun pada sebuah Workspace mengubah akun
     * pembebanan seluruh klaim lama yang menunjuk anggaran itu, secara
     * retroaktif, tanpa jejak tindakan pengguna.</p>
     *
     * <p><b>Penanganan galat.</b> Kegagalan resolusi lazy pada
     * {@code getWorkspace().getAkun()} ditangkap dan hanya dicatat
     * {@code ErrorAuditUtil} — nilai kembalian jatuh ke akun tersimpan, jadi
     * kegagalan bersifat senyap bagi pengguna.</p>
     *
     * <p><b>Dibaca dari:</b> {@link DaftarPengajuanTransfer} untuk memilih akun
     * jurnal (dengan {@link #getAkunBiaya()} sebagai cadangan yang tak pernah
     * terisi).</p>
     *
     * @return akun pembebanan efektif, atau {@code null} bila belum ditentukan
     *         (dokumen akan dilewati mesin posting tanpa pesan galat)
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "akun", nullable = true)
    public Akun getAkun() {
        akun = check(akun);
        try {
            if (getWorkspace() != null) {
                akun = getWorkspace().getAkun();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawai.getAkun");
        }
        return akun;
    }
    /**
     * Menyetel akun biaya efektif.
     *
     * <p>Dipanggil kedua jalur khusus untuk klaim tanpa anggaran, dengan nilai
     * {@code JenisReimbursement.getAkun()}. Untuk klaim ber-anggaran nilainya
     * tidak diset di sini (dan tetap akan ditimpa {@link #getAkun()}).</p>
     *
     * @param akun akun beban; boleh {@code null}
     */
    public void setAkun(Akun akun) { this.akun = akun; }

    // =========================================================================
    // Rincian barang/biaya (pola KasKecil: JSON)
    // =========================================================================

    /**
     * Rincian baris barang/biaya klaim, disimpan sebagai <b>teks JSON</b>
     * (bukan tabel anak), meniru pola {@code KasKecil}.
     *
     * <p>Bentuknya array objek dengan kunci {@code key}, {@code akun},
     * {@code nama}, {@code qty}, {@code harga}, {@code jumlah}, dan
     * {@code tanggal}. Total seluruh baris menjadi {@link #getNominal()}.
     * Jalur REST menuliskan {@code "[]"} bila klien tidak mengirim rincian.</p>
     *
     * <p><b>Implikasi yang perlu disadari.</b> Karena rinciannya bukan entity,
     * (a) tidak ada FK yang memvalidasi akun per baris — akun yang dihapus di
     * bagan akun tetap tertinggal sebagai teks; (b) rincian <b>tidak muncul di
     * laporan buku besar</b> mana pun; (c) tidak ada penjaga yang memastikan
     * jumlah baris JSON masih sama dengan {@link #getNominal()} setelah
     * penyuntingan langsung di basis data; dan (d) Envers menyimpan seluruh
     * JSON sebagai satu kolom teks di tabel {@code _aud}, sehingga perubahan
     * satu baris tampak sebagai perubahan seluruh rincian.</p>
     *
     * @return teks JSON rincian, atau {@code null} pada dokumen lama
     */
    @Column(columnDefinition = "text")
    public String getFormula() { return formula; }
    /**
     * Menyetel rincian baris biaya dalam bentuk teks JSON.
     *
     * @param formula teks JSON array rincian; boleh {@code null}
     */
    public void setFormula(String formula) { this.formula = formula; }

    // =========================================================================
    // SOP + penyetuju turunan + DPC (pola UangMuka)
    // =========================================================================

    /**
     * Instance alur SOP yang menggerakkan dokumen ini.
     *
     * <p>Inilah sumber kebenaran bagi {@link #getStatus()},
     * {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
     * {@link #getAktif()} dan {@link #getDibuatOleh()}. Dokumen yang dibuat
     * lewat jalur REST tidak punya disposisi ({@code null}), sehingga keempat
     * properti turunan itu jatuh ke nilai tersimpan.</p>
     *
     * <p>Getter hanya meresolusi proxy lazy ({@code check}) — tidak menghitung
     * apa pun.</p>
     *
     * @return instance alur SOP, atau {@code null} bila dokumen tidak ber-SOP
     * @see ais.database.model.sop.DataSop
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "disposisi_sop", nullable = true)
    public DisposisiSop getDisposisiSop() {
        disposisiSop = check(disposisiSop);
        return disposisiSop;
    }
    /**
     * Menautkan dokumen ke sebuah instance alur SOP.
     *
     * <p><b>Penolakan senyap.</b> Argumen {@code null} atau ber-id {@code null}
     * <b>diabaikan</b> — method langsung {@code return} tanpa mengubah apa pun
     * dan tanpa memberi tanda apa pun kepada pemanggil. Rancangan ini mencegah
     * mesin SOP menimpa tautan yang sudah benar dengan objek kosong hasil
     * binding form, tetapi juga berarti <b>tautan SOP tidak bisa dilepas</b>
     * lewat setter ini: sekali dokumen masuk alur, ia tidak bisa dikeluarkan
     * kecuali lewat SQL langsung.</p>
     *
     * @param disposisiSop instance alur SOP yang sudah tersimpan (punya id);
     *                     {@code null} atau ber-id {@code null} diabaikan
     */
    public void setDisposisiSop(DisposisiSop disposisiSop) {
        if (disposisiSop == null || disposisiSop.getId() == null) {
            return;
        }
        this.disposisiSop = disposisiSop;
    }

    /**
     * Pengguna yang menyetujui klaim ini.
     *
     * <p><b>Getter penghitung/destruktif — penentu status dokumen.</b> Bila
     * dokumen ber-SOP, nilainya sepenuhnya <b>dikendalikan disposisi</b>:</p>
     * <ul>
     * <li>langkah "setuju" ({@code disposisiSetuju}) punya pengaju → field
     * ditimpa dengan pengaju itu;</li>
     * <li>langkah "setuju" belum ada atau pengajunya kosong → field
     * <b>dipaksa {@code null}</b>, menghapus penyetuju yang mungkin sudah
     * tersimpan di kolom.</li>
     * </ul>
     * <p>Cabang kedua itulah yang membuat persetujuan hasil jalur REST
     * <b>lenyap saat dibaca</b> pada dokumen yang juga ber-SOP (dan, lewat
     * {@link #getStatus()}, menurunkan statusnya kembali ke {@link #DIAJUKAN}).
     * Untuk dokumen tanpa SOP tidak ada penimpaan sama sekali, sehingga
     * persetujuan REST bertahan.</p>
     *
     * <p><b>Efek samping.</b> Penimpaan di atas mengenai field terpetakan, jadi
     * pembacaan dalam sesi hidup dapat menerbitkan {@code UPDATE} pada kolom
     * {@code disetujui_oleh}. Nilai kembalian melewati {@code check()} sekali
     * lagi untuk memastikan proxy sudah teresolusi.</p>
     *
     * <p><b>Catatan keamanan.</b> Penulisan langsung lewat
     * {@code ReimbursementApiHelper.keputusan()} tidak memeriksa apakah
     * penyetuju adalah {@link #getAtasan()}, tidak melarang menyetujui
     * pengajuan sendiri, dan gerbang perannya fail-open untuk pengguna tanpa
     * peran. Lihat <a href="#keamanan">catatan keamanan kelas</a>.</p>
     *
     * @return pengguna penyetuju, atau {@code null} bila belum disetujui
     */
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

        return check(disetujuiOleh);
    }
    /**
     * Menyetel pengguna penyetuju.
     *
     * <p>Dipanggil {@link #setStatus(String)} dengan {@code null} saat dokumen
     * ditolak, dan oleh jalur REST saat keputusan diambil. Pada dokumen ber-SOP
     * nilai yang diset di sini akan ditimpa lagi oleh
     * {@link #getDisetujuiOleh()}.</p>
     *
     * @param disetujuiOleh pengguna penyetuju; {@code null} untuk menghapus
     */
    public void setDisetujuiOleh(Tbmuser disetujuiOleh) { this.disetujuiOleh = disetujuiOleh; }

    /**
     * Waktu klaim disetujui.
     *
     * <p><b>Getter penghitung/destruktif</b>, mengikuti
     * {@link #getDisetujuiOleh()}: bila langkah "setuju" pada disposisi punya
     * pengaju, field ditimpa dengan {@code disposisiSetuju.getWaktu()}; bila
     * langkah itu belum ada/pengajunya kosong, field <b>dipaksa {@code null}</b>.
     * Dokumen tanpa SOP mempertahankan nilai tersimpannya.</p>
     *
     * <p><b>Penanganan galat.</b> Seluruh blok dibungkus {@code try/catch} untuk
     * menjinakkan {@code LazyInitializationException} saat entity dibaca di luar
     * sesi (mis. saat merender daftar dari objek detached); kegagalan hanya
     * dicatat {@code ErrorAuditUtil} dan nilai tersimpan dikembalikan apa
     * adanya. Artinya di luar sesi, tanggal persetujuan yang ditampilkan bisa
     * <b>usang</b> dibanding kondisi disposisi terkini.</p>
     *
     * <p><b>Kasus tepi.</b> Jalur REST mengizinkan klien <b>menentukan sendiri</b>
     * tanggal persetujuan ({@code request "tanggalPersetujuan"}, jatuh ke waktu
     * server bila tidak dikirim) — tanggal persetujuan pada dokumen tanpa SOP
     * karenanya tidak dapat dianggap sebagai stempel waktu tepercaya.</p>
     *
     * @return waktu persetujuan, atau {@code null} bila belum disetujui
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_persetujuan")
    public Date getTanggalPersetujuan() {
        try {
            if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
                    && getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
                tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
            }
            if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
                    || getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
                tanggalPersetujuan = null;
            }
        } catch (Exception exLazy) {
            ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) ReimbursementPegawai.getTanggalPersetujuan-lazy");
        }
        return tanggalPersetujuan;
    }
    /**
     * Menyetel waktu persetujuan.
     *
     * @param tanggalPersetujuan waktu persetujuan; {@code null} untuk menghapus
     */
    public void setTanggalPersetujuan(Date tanggalPersetujuan) { this.tanggalPersetujuan = tanggalPersetujuan; }

    /**
     * Aktif kecuali disposisi non-aktif atau ditolak di titik akhir (pola UangMuka).
     *
     * <p><b>Getter penghitung: nilai tersimpan selalu dibuang.</b> Method ini
     * memulai dengan memaksa {@code aktif = TRUE} sebelum mengevaluasi apa pun,
     * sehingga apa pun yang pernah diset lewat {@link #setAktif(Boolean)} —
     * termasuk penonaktifan manual — <b>tidak berpengaruh</b> pada dokumen yang
     * tidak ber-SOP. Nilai {@code false} hanya mungkin muncul dari dua sebab,
     * keduanya bersumber pada disposisi: disposisinya sendiri non-aktif, atau
     * langkah akhirnya adalah titik penolakan.</p>
     *
     * <p><b>Perhatikan</b> properti ini tidak beranotasi {@code @Column} namun
     * tetap terpetakan (property access), sehingga hasil hitungan ikut
     * tersimpan ke kolom {@code aktif}. Dipakai sebagai filter opsional di layar
     * daftar (checkbox "aktif"), yang menyaringnya di memori setelah query —
     * bukan di SQL.</p>
     *
     * @return {@code true} bila dokumen masih berlaku; {@code false} bila
     *         disposisinya non-aktif atau berakhir di titik penolakan
     */
    public Boolean getAktif() {
        aktif = Boolean.TRUE;
        DisposisiSop d = getDisposisiSop();
        if (d != null && !d.getAktif()) {
            aktif = false;
        }
        if (d != null && d.getDisposisiEnd() != null && d.getDisposisiEnd().getAlurSop() != null
                && d.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
            aktif = false;
        }
        return aktif;
    }

    /**
     * Menyetel bendera aktif.
     *
     * <p>Praktis tanpa efek karena {@link #getAktif()} selalu menghitung ulang
     * dari nol; disediakan agar Hibernate dapat memuat kolomnya dan agar jalur
     * REST dapat menginisialisasi dokumen baru.</p>
     *
     * @param aktif bendera aktif; boleh {@code null}
     */
    public void setAktif(Boolean aktif) {
        this.aktif = aktif;
    }

    /**
     * Baris Daftar Pengajuan Cair/Transfer (DPC) yang dibuat dari klaim ini.
     *
     * <p>Terisi setelah dokumen berstatus {@link #DISETUJUI} lewat
     * {@link DaftarPengajuanTransfer#simpanReimbursement(ReimbursementPegawai)}.
     * Dari titik itu, seluruh eksekusi pembayaran dan penjurnalan berpindah ke
     * baris DPC — dokumen ini tidak lagi menyimpan jejak pelunasannya sendiri
     * (lihat blok kolom pembayaran yang tidur).</p>
     *
     * <p>Getter relasi polos <b>tanpa</b> {@code check()} — berbeda dari relasi
     * lain di kelas ini. Pemanggil yang bekerja di luar sesi Hibernate harus
     * bersiap menghadapi proxy yang belum teresolusi. {@code FetchMode.SELECT}
     * dipakai agar relasi ini tidak ikut di-join pada setiap pemuatan daftar.</p>
     *
     * <p><b>Perhatikan efek samping jalur render:</b> renderer daftar memasang
     * timer yang membuat baris DPC untuk setiap dokumen disetujui yang kolom
     * ini masih kosong — membuka layar daftar saja sudah menerbitkan baris DPC
     * baru.</p>
     *
     * @return baris DPC, atau {@code null} bila belum diajukan ke proses transfer
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
    public DaftarPengajuanTransfer getDaftarPengajuanTransfer() { return daftarPengajuanTransfer; }
    /**
     * Menautkan klaim ini ke sebuah baris DPC.
     *
     * @param daftarPengajuanTransfer baris DPC; boleh {@code null}
     */
    public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
        this.daftarPengajuanTransfer = daftarPengajuanTransfer;
    }

    /**
     * BAST penerimaan barang untuk reimbursement ini (klon pola UangMuka) — terisi saat barang diterima.
     *
     * <p><b>Kolom tidur di sisi entity ini.</b> {@code setPenerimaanPengadaanMasterAsset}
     * tidak pernah dipanggil dari modul reimbursement; satu-satunya penyebutan
     * di luar kelas ini ada pada
     * {@code PenerimaanPengadaanMasterAssetAction}, yaitu sisi <b>seberang</b>
     * relasi. Praktisnya kolom {@code penerimaan_pengadaan_master_asset} pada
     * tabel ini tetap {@code NULL}: pola BAST disalin utuh dari
     * {@link UangMuka} pada saat rework, meski reimbursement tidak melewati
     * jalur pengadaan barang.</p>
     *
     * <p>Getter relasi polos tanpa {@code check()}.</p>
     *
     * @return BAST penerimaan barang; praktis selalu {@code null}
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "penerimaan_pengadaan_master_asset", nullable = true)
    public ais.database.model.asset.PenerimaanPengadaanMasterAsset getPenerimaanPengadaanMasterAsset() {
        return penerimaanPengadaanMasterAsset;
    }
    /**
     * Menautkan klaim ini ke BAST penerimaan barang.
     *
     * @param penerimaanPengadaanMasterAsset dokumen BAST; boleh {@code null}
     */
    public void setPenerimaanPengadaanMasterAsset(
            ais.database.model.asset.PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) {
        this.penerimaanPengadaanMasterAsset = penerimaanPengadaanMasterAsset;
    }

    /**
     * Stempel waktu perubahan terakhir baris.
     *
     * <p>Diisi pada konstruksi objek dan diperbarui {@link #onUpdate()} setiap
     * {@code UPDATE}. Karena banyak getter di kelas ini menulis balik ke
     * fieldnya sendiri, stempel ini <b>dapat bergeser tanpa ada tindakan
     * pengguna</b> — jangan pakai sebagai bukti penyuntingan manual; untuk itu
     * gunakan tabel Envers {@code reimbursement_pegawai_aud}.</p>
     *
     * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek baru
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_dirubah")
    public Date getTanggalDirubah() { return tanggalDirubah; }
    /**
     * Menyetel stempel waktu perubahan terakhir.
     *
     * @param tanggalDirubah waktu perubahan; boleh {@code null}
     */
    public void setTanggalDirubah(Date tanggalDirubah) { this.tanggalDirubah = tanggalDirubah; }

    /**
     * Callback JPA yang memperbarui {@link #getTanggalDirubah()} tepat sebelum
     * setiap {@code UPDATE}.
     *
     * <p>Memakai {@code WaktuUtil.getDate()} (waktu server yang sudah
     * disesuaikan zona aplikasi), bukan {@code new Date()}. Tidak ada
     * {@code @PrePersist} pasangannya — nilai awal cukup disediakan
     * inisialisasi field.</p>
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() { tanggalDirubah = ais.ui.util.WaktuUtil.getDate(); }

    /**
     * Representasi teks singkat dokumen: {@code "<kode> - <judul>"}, dengan
     * {@link #getDeskripsi()} sebagai cadangan bila judul kosong.
     *
     * <p><b>Membaca field langsung, bukan getter</b> — sehingga tidak memicu
     * write-back dan tidak menyentuh relasi lazy (aman dipanggil pada objek
     * detached). Konsekuensinya nilai yang ditampilkan adalah isi kolom apa
     * adanya, tanpa pemangkasan spasi yang dilakukan {@link #getNama()}.</p>
     *
     * <p><b>Kasus tepi.</b> Bila {@link #getKode()} masih {@code null} (dokumen
     * belum pernah disimpan), hasilnya diawali literal {@code "null - "}.
     * Dipakai antara lain sebagai label pilihan pada layar yang menautkan
     * dokumen ini.</p>
     *
     * @return teks ringkas dokumen
     */
    public String toString() { return kode + " - " + (nama == null ? deskripsi : nama); }
}
