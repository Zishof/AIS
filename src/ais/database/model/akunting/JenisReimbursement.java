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

import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;

/**
 * <h3>Katalog "Jenis Reimbursement" — master pendek yang menentukan perilaku
 * seluruh pengajuan reimbursement pegawai.</h3>
 *
 * <p>Entity ini memetakan tabel {@code akunting.jenis_reimbursement}. Ia
 * <b>bukan dokumen</b>: tidak punya nominal, tidak punya status, tidak pernah
 * diposting ke buku besar, dan tidak pernah masuk antrean pembayaran. Perannya
 * murni sebagai <i>katalog</i> (data master) yang dipilih satu kali di kepala
 * setiap klaim {@link ais.database.model.akunting.ReimbursementPegawai}, dan
 * dari pilihan itulah dua keputusan penting diturunkan:</p>
 *
 * <ul>
 *   <li><b>menggunakanAnggaran = true</b> (default): pengaju WAJIB memilih
 *       Anggaran (Workspace) pada form pengajuan; akun mengikuti anggaran.</li>
 *   <li><b>menggunakanAnggaran = false</b> ("Tanpa Anggaran"): {@link #getAkun()}
 *       ditentukan DI SINI oleh admin, sehingga pengaju tidak perlu memilih
 *       akun di setiap pengajuan. Admin boleh membuat beberapa jenis tanpa
 *       anggaran dengan akun/satuan kerja berbeda.</li>
 * </ul>
 *
 * <p>Dua baris default dibuat saat bootstrap Tomcat oleh
 * {@code InitIndex.initDefaultJenisReimbursement()} — "Menggunakan Anggaran"
 * dan "Tanpa Anggaran". Seed itu <b>idempoten</b>: tabelnya dibuat dengan
 * {@code CREATE TABLE IF NOT EXISTS} dan barisnya hanya disisipkan bila tabel
 * masih benar-benar kosong ({@code WHERE NOT EXISTS (SELECT 1 FROM ...)}),
 * sehingga penyuntingan admin tidak pernah tertimpa restart. Konsekuensinya
 * juga perlu disadari: bila admin menghapus/menonaktifkan kedua baris itu,
 * restart <b>tidak</b> mengembalikannya.</p>
 *
 * <h4>Bagaimana akun di sini sampai ke jurnal</h4>
 *
 * <p>Rantainya panjang dan — ini bagian yang paling sering disalahpahami —
 * <b>bersifat snapshot, bukan pembacaan hidup</b>:</p>
 *
 * <ol>
 *   <li>Saat klaim disimpan (baik dari layar ZK {@code ReimbursementPegawaiAction.onSave}
 *       maupun dari REST {@code ReimbursementApiHelper.simpan}), kode yang sama
 *       dijalankan: bila jenisnya "tanpa anggaran", nilai {@link #getAkun()}
 *       <b>disalin</b> ke {@code ReimbursementPegawai.setAkun(...)} dan
 *       {@code tanpaAnggaran} diisi {@code true}; bila jenisnya "ber-anggaran",
 *       {@code akun} klaim dibiarkan {@code null} dan diambil dari Workspace.</li>
 *   <li>Klaim yang disetujui disinkronkan menjadi baris Daftar Pengajuan
 *       Transfer (DPC) oleh {@code SinkronDaftarPengajuanTransferHelper} →
 *       {@code DaftarPengajuanTransfer.simpanReimbursement(...)}.</li>
 *   <li>{@code DaftarPengajuanTransfer.getAkun()} memilih akun lawan jurnal;
 *       untuk cabang reimbursement ia membaca
 *       {@code getReimbursementPegawai().getAkun()} (salinan langkah 1), lalu
 *       {@code getAkunBiaya()} sebagai cadangan.</li>
 *   <li>{@code PostingProsesTransferAction} memakai akun itu sebagai sisi debet
 *       saat pencairan dijurnal.</li>
 * </ol>
 *
 * <p><b>Perbedaan penting dengan saudara-saudaranya.</b> Pada cabang Uang Muka,
 * Kas Besar, Dana Talangan, dan Penggantian Kas Kecil,
 * {@code DaftarPengajuanTransfer.getAkun()} membaca masternya <b>secara langsung
 * saat posting</b> (mis. {@code getUangMuka().getJenisUangMuka().getAkun()}) —
 * di sana mengubah akun master benar-benar <i>retroaktif</i> terhadap dokumen
 * lama yang belum diposting. Cabang reimbursement TIDAK begitu: karena akunnya
 * sudah disalin ke dokumen, mengubah {@link #getAkun()} hari ini <b>tidak</b>
 * memindahkan pembebanan klaim yang sudah tersimpan. Yang terpengaruh adalah
 * (a) semua klaim yang disimpan SETELAH perubahan, dan (b) klaim lama yang
 * kebetulan disunting ulang (penyuntingan menyimpan ulang salinan akunnya).
 * Ini <b>memperhalus</b>, bukan membatalkan, catatan keamanan di
 * {@code ReimbursementPegawai.getJenisReimbursement()}: jendela penyalahgunaannya
 * ke depan, bukan ke belakang.</p>
 *
 * <h4><a id="keamanan">Catatan keamanan — DUA jalur fail-open independen</a></h4>
 *
 * <p>Katalog sekecil ini disentuh oleh <b>dua</b> helper REST berbeda, dan
 * keduanya memakai pola gerbang {@code bolehAksi()} yang identik dan sama-sama
 * <b>fail-open</b> saat {@code Tbmuser.hakAkses()} mengembalikan {@code null}
 * (peran dikembalikan sebagai izin PENUH, bukan penolakan) — pola yang dilacak
 * sebagai {@code task_66986071}:</p>
 *
 * <ul>
 *   <li><b>Jalur tulis-master</b> — {@code MasterKeuanganApiHelper}, dipanggil
 *       {@code PosApi} untuk aksi berawalan <code>master_keuangan_</code>.
 *       Entity ini terdaftar di sana sebagai tipe {@code "jenis_reimbursement"}
 *       (salah satu dari tujuh master keuangan) dan bisa
 *       <b>dibuat/diubah/dihapus</b> lewat {@code simpan}/{@code hapus}.</li>
 *   <li><b>Jalur dokumen</b> — {@code ReimbursementApiHelper}, dipanggil
 *       {@code PosApi} untuk aksi berawalan <code>reimbursement_</code>. Di
 *       sini entity ini dibaca (bukan ditulis), tetapi gerbang yang fail-open
 *       itu menjaga <b>approve/reject</b> — sehingga pengguna tanpa peran
 *       terbaca dapat mengajukan klaim yang memakai jenis ini lalu
 *       <b>menyetujui klaimnya sendiri</b>, yang kemudian mengalir otomatis ke
 *       antrean pembayaran.</li>
 * </ul>
 *
 * <p>Gabungan keduanya berarti satu akun yang cache perannya sedang anomali
 * dapat: memindahkan akun biaya jenis "tanpa anggaran" (jalur 1) → mengajukan
 * klaim atas jenis itu → menyetujui sendiri klaimnya (jalur 2) → klaim masuk
 * DPC dan dijurnal ke akun pilihannya. Niat penulisnya jelas fail-closed:
 * kunci menu {@code "master_keuangan"} maupun {@code "reimbursement"} sama-sama
 * terdaftar di {@code EbisnisMenuKatalog.KUNCI_DEFAULT_NONAKTIF}. Niat itu
 * tidak pernah tereksekusi karena (a) gerbang pertama
 * {@code PosApi.bolehAksesActionKantin} tidak punya cabang untuk kedua awalan
 * dan jatuh ke {@code return true}, dan (b) cabang {@code role == null} di
 * lapis kedua meloloskan lebih dulu sebelum {@code EbisnisMenuKatalog} sempat
 * dikonsultasikan.</p>
 *
 * <p><b>Pembacaan tidak digerbangi sama sekali.</b> {@code MasterKeuanganApiHelper.daftar}
 * dan {@code ReimbursementApiHelper.opsi} keduanya menayangkan seluruh isi
 * tabel ini (nama, keterangan, id akun, kode+nama akun hasil {@code LEFT JOIN}
 * ke {@code akunting.akun}) hanya berbekal autentikasi, tanpa memeriksa hak
 * menu — jadi pemetaan akun internal terbaca oleh setiap pemegang token API.</p>
 *
 * <p><b>Tidak ada kolom tenant.</b> Entity ini tidak punya relasi ke
 * {@code Sekolah}/{@code Yayasan} sama sekali — ini bukan fail-open kondisional
 * seperti pada entity ber-{@code sekolah} yang filternya bisa lolos, melainkan
 * ketiadaan konsep tenant. {@link #getSatuanKerja()} bersifat opsional dan
 * hanya berperan sebagai <i>prefill</i>/label ("(semua)" bila kosong), bukan
 * penyaring: daftar di layar ZK ({@code createCriteria(...).addOrder(asc("id"))}),
 * di {@code MasterKeuanganApiHelper.daftar}, maupun di
 * {@code ReimbursementApiHelper.opsi} sama-sama <b>tanpa klausa penyaring
 * cakupan apa pun</b>. Semua pengguna semua tenant melihat dan dapat memilih
 * jenis milik siapa pun.</p>
 *
 * <p><b>Tidak ada jejak audit.</b> Kelas ini sengaja <b>tidak</b> beranotasi
 * {@code @Audited} — {@code ReimbursementPegawai} bahkan harus menandai
 * relasinya {@code targetAuditMode = NOT_AUDITED} agar Envers mau boot. Artinya
 * tidak ada tabel {@code _aud} untuk katalog ini: bila akun biaya dipindahkan,
 * satu-satunya sisa jejak adalah stempel {@link #getTanggalDirubah()} — dan
 * kolom itu hanya menyimpan <i>kapan</i>, tidak pernah <i>oleh siapa</i>.
 * Perubahan akun pembebanan pada katalog ini praktis tidak dapat direkonstruksi
 * secara forensik.</p>
 *
 * <h4>Verifikasi negatif yang menenangkan</h4>
 *
 * <ul>
 *   <li><b>Layar ZK justru digerbangi ketat.</b> Tab "Jenis Reimbursement" di
 *       {@code reimbursement_pegawai.zul} dikelola
 *       {@code ReimbursementPegawaiAction.onJenisReimbursement}, dan baik tombol
 *       "Tambah Jenis"/"Ubah" maupun {@code bukaFormJenis} dijaga
 *       {@code bolehKelolaJenis()} yang mensyaratkan peran
 *       {@code Tbmrole.ADMINISTRATOR} — bukan sekadar hak menu. Ini gerbang ZK
 *       <b>paling ketat</b> di antara katalog akun sejenis ({@code JenisKasBesar},
 *       {@code JenisKasKecil}, {@code CaraPembayaranTransfer}). Ironisnya justru
 *       jalur REST-lah yang longgar, sehingga pewarisan hak lewat menu induk
 *       (pola berulang di modul lain: punya menu dokumen ⇒ dapat CRUD master di
 *       tab yang disisipkan) <b>TIDAK berlaku</b> di sini.</li>
 *   <li><b>Pola "checkbox grid tanpa gerbang" ({@code task_0a06e418}) TIDAK
 *       berlaku.</b> Grid daftar jenis di layar ZK hanya berisi {@code Label}
 *       untuk kolom "Menggunakan Anggaran" dan "Aktif" — tidak ada
 *       {@code Checkbox} yang menulis langsung ke basis data dari baris grid.
 *       Semua penyuntingan harus lewat form yang sudah digerbangi.</li>
 *   <li><b>Tidak ada tombol Hapus di ZK</b> sama sekali. Penghapusan hanya
 *       mungkin lewat {@code MasterKeuanganApiHelper.hapus}, yang lebih dulu
 *       menolak bila jenisnya sudah dipakai dokumen
 *       ({@code SELECT count(*) FROM akunting.reimbursement_pegawai WHERE
 *       jenis_reimbursement = ?} &gt; 0) — jadi riwayat dokumen lama tidak bisa
 *       diputus dari akunnya. Jenis yang belum pernah dipakai tetap dapat
 *       dihapus lewat jalur fail-open itu.</li>
 *   <li><b>{@link #getAkun()} bukan getter destruktif.</b> Penugasan
 *       {@code akun = check(akun)} hanya menormalkan proxy lazy menjadi instance
 *       kanonik ({@code EntityIdentityMap}) — berbeda tajam dari
 *       {@code Transaksi.getAkun()} yang menimpa akun dengan {@code akunOver},
 *       atau {@code Transitori.getTransfer()} yang menugaskan nilai baru tanpa
 *       syarat. Membaca entity ini tidak mengubah maknanya.</li>
 * </ul>
 *
 * <h4>Kuirk yang perlu diketahui</h4>
 *
 * <ul>
 *   <li><b>Semantik PUT pada payload REST parsial.</b>
 *       {@code MasterKeuanganApiHelper.simpan} menimpa SELURUH kolom setiap kali
 *       dipanggil. Yang paling berbahaya:
 *       {@code request.optBoolean("menggunakanAnggaran", false)} — bawaannya
 *       {@code false}, berlawanan dengan bawaan {@link #getMenggunakanAnggaran()}
 *       yang {@code true}. Klien yang memperbarui jenis tanpa menyertakan medan
 *       itu akan <b>diam-diam mengubah jenis "Menggunakan Anggaran" menjadi
 *       "Tanpa Anggaran"</b>; sejak saat itu pengajuan tidak lagi menuntut
 *       Anggaran dan justru menuntut {@link #getAkun()} yang kemungkinan besar
 *       masih kosong, sehingga semua pengajuan jenis itu ditolak dengan pesan
 *       "Akun ... belum ditentukan". Nasib serupa menimpa {@code satuanKerja},
 *       {@code keterangan}, dan {@code akun} yang ikut di-{@code null}-kan bila
 *       tidak dikirim.</li>
 *   <li><b>Tidak ada kolom {@code kode}.</b> Berbeda dari {@code JenisUangMuka},
 *       {@code JenisKasKecil}, {@code JenisKasBesar}, dan
 *       {@code CaraPembayaranTransfer}, katalog ini diidentifikasi hanya lewat
 *       {@link #getNama()}. {@code MasterKeuanganApiHelper} menyadarinya dan
 *       menandai {@code punyaKode=false} serta mengirim {@code kode} sebagai
 *       string kosong. Tidak ada indeks unik pada {@code nama} — dua jenis
 *       bernama sama, dengan akun berbeda, bisa hidup berdampingan dan hanya
 *       dapat dibedakan pengaju lewat sufiks satuan kerja pada
 *       {@link #toString()}.</li>
 *   <li><b>{@code CascadeType.PERSIST/MERGE} pada {@link #getAkun()} dan
 *       {@link #getSatuanKerja()}.</b> Menyimpan katalog ini ikut me-{@code merge}
 *       objek {@code Akun}/{@code SatuanKerja} yang menempel padanya — bila
 *       instance yang di-{@code set} adalah objek detached yang sudah dimodifikasi,
 *       perubahan itu ikut tertulis ke bagan akun. Layar ZK dan REST sama-sama
 *       hanya menempelkan instance hasil {@code session.get}/pilihan banbox,
 *       jadi dalam praktik aman; catat saja bila kelak ada pemanggil baru.</li>
 *   <li><b>{@link #getNama()} memangkas spasi saat dibaca, {@link #toString()}
 *       tidak memakainya.</b> {@code toString()} membaca <i>field</i>
 *       {@code nama} dan {@code satuanKerja} secara langsung, bukan lewat
 *       getter — jadi ia melewati normalisasi {@code check()}. Bila
 *       {@code satuanKerja} masih berupa proxy Hibernate yang belum
 *       terinisialisasi dan sesinya sudah tertutup, pengaksesannya melempar
 *       {@code LazyInitializationException} yang <b>ditelan</b> blok
 *       {@code catch} — hasilnya label jenis tampil tanpa sufiks satuan kerja,
 *       diam-diam, tanpa galat yang terlihat pengguna.</li>
 * </ul>
 *
 * <h4>Pengelompokan method</h4>
 *
 * <ol>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}.</li>
 *   <li><b>Deskriptif</b> — {@link #getNama()}, {@link #getKeterangan()}.</li>
 *   <li><b>Perilaku pengajuan</b> — {@link #getMenggunakanAnggaran()}, satu-satunya
 *       saklar yang mengubah bentuk formulir pengajuan.</li>
 *   <li><b>Pemetaan jurnal &amp; cakupan</b> — {@link #getAkun()},
 *       {@link #getSatuanKerja()}.</li>
 *   <li><b>Ketersediaan</b> — {@link #getAktif()}.</li>
 *   <li><b>Stempel perubahan</b> — {@link #getTanggalDirubah()} dan kait
 *       {@link #onUpdate()}.</li>
 *   <li><b>Penyajian</b> — {@link #toString()}.</li>
 * </ol>
 *
 * <p><b>Catatan pewarisan.</b> Kelas ini memperluas
 * {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa
 * — Hibernate tidak memetakan properti induknya. Karena itu setiap kolom yang
 * dibutuhkan harus dideklarasikan ulang di kelas ini; kemiripan nama dengan
 * properti induk bukan duplikasi yang keliru, melainkan keharusan teknis.
 * Manfaat yang tetap diwarisi adalah utilitas seperti
 * {@code check(...)} untuk menormalkan proxy lazy.</p>
 *
 * @see ais.database.model.akunting.ReimbursementPegawai
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.rab.SatuanKerja
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "akunting", name = "jenis_reimbursement")
public class JenisReimbursement extends GeneralValueObject {
    private static final long serialVersionUID = 1L;

    /** Kunci utama tabel {@code akunting.jenis_reimbursement}; lihat {@link #getId()}. */
    private Long id;
    /** Nama jenis yang tampil di combo pengajuan; lihat {@link #getNama()}. */
    private String nama;
    /** Penjelasan bebas untuk admin; lihat {@link #getKeterangan()}. */
    private String keterangan;
    /** Saklar wajib-anggaran; lihat {@link #getMenggunakanAnggaran()}. */
    private Boolean menggunakanAnggaran;
    /** Akun biaya tetap untuk jenis tanpa anggaran; lihat {@link #getAkun()}. */
    private Akun akun;
    /** Satuan kerja bawaan (prefill, bukan penyaring); lihat {@link #getSatuanKerja()}. */
    private SatuanKerja satuanKerja;
    /** Ketersediaan jenis di combo pengajuan; lihat {@link #getAktif()}. */
    private Boolean aktif;
    /**
     * Stempel perubahan terakhir; lihat {@link #getTanggalDirubah()}.
     *
     * <p>Diberi nilai awal di deklarasi (bukan lewat {@code @PrePersist}), sehingga
     * baris baru sudah berstempel sejak {@code new JenisReimbursement()}. Pembaruan
     * berikutnya diurus {@link #onUpdate()}.</p>
     */
    private Date tanggalDirubah = ais.ui.util.WaktuUtil.getDate();

    /**
     * Kunci utama tabel {@code akunting.jenis_reimbursement}.
     *
     * <p>Dibangkitkan basis data ({@code bigserial}, strategi {@code IDENTITY}),
     * karena itu kolomnya {@code insertable = false} — nilai yang di-{@code set}
     * manual tidak akan dikirim pada {@code INSERT}. Inilah nilai yang dirujuk
     * kolom {@code reimbursement_pegawai.jenis_reimbursement}, dan yang dikirim
     * REST sebagai {@code id}/{@code jenisReimbursementId}.</p>
     *
     * @return id baris, atau {@code null} bila entity belum tersimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    /**
     * Menyetel kunci utama. Hanya dipakai Hibernate saat memuat baris.
     *
     * @param id kunci utama; boleh {@code null} untuk entity baru
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Nama jenis sebagaimana tampil di combo "Jenis Reimbursement" pada form
     * pengajuan dan di kolom pertama grid master.
     *
     * <p>Wajib diisi di kedua jalur tulis: layar ZK menolak dengan pesan "Nama
     * jenis wajib diisi", dan {@code MasterKeuanganApiHelper.simpan} menolak
     * dengan "Nama Jenis Reimbursement wajib diisi". Tidak ada indeks unik,
     * sehingga nama kembar dimungkinkan (lihat catatan kuirk pada dokumentasi
     * kelas).</p>
     *
     * <p><b>Getter ini memangkas spasi saat dibaca, tanpa menulis balik</b> —
     * nilai di basis data tetap apa adanya. {@link #toString()} sengaja tidak
     * memakai getter ini melainkan field mentahnya, lalu memangkasnya sendiri.</p>
     *
     * @return nama jenis yang sudah dipangkas spasinya, atau {@code null} bila
     *         kolomnya kosong
     */
    @Column(name = "nama", length = 255)
    public String getNama() { return nama == null ? null : nama.trim(); }
    /**
     * Menyetel nama jenis.
     *
     * @param nama nama jenis; boleh {@code null}, tetapi kedua jalur tulis
     *             menolaknya lebih dulu
     */
    public void setNama(String nama) { this.nama = nama; }

    /**
     * Penjelasan bebas untuk admin — misalnya batasan penggunaan jenis ini atau
     * siapa yang boleh memakainya.
     *
     * <p>Kolom {@code text} tanpa batas panjang, tidak pernah ikut memengaruhi
     * jurnal maupun validasi. Ditayangkan di grid master ZK, di
     * {@code MasterKeuanganApiHelper.daftar}, dan menjadi salah satu kolom yang
     * dicari kata kuncinya ({@code ILIKE}) pada pencarian daftar master.</p>
     *
     * @return keterangan apa adanya (tidak dipangkas), atau {@code null}
     */
    @Column(name = "keterangan", columnDefinition = "text")
    public String getKeterangan() { return keterangan; }
    /**
     * Menyetel keterangan.
     *
     * <p>Perhatikan bahwa {@code MasterKeuanganApiHelper.simpan} selalu menulis
     * nilai ini dari payload ({@code optString("keterangan","")}), sehingga
     * pembaruan REST yang tidak menyertakan medan ini akan mengosongkannya.</p>
     *
     * @param keterangan teks bebas; boleh {@code null}
     */
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    /**
     * Saklar utama katalog ini: apakah pengaju wajib memilih Anggaran (Workspace).
     *
     * <p><b>Default TRUE</b> — getter mengembalikan {@link Boolean#TRUE} bila
     * kolomnya {@code null}, sehingga baris lama atau baris yang disisipkan
     * langsung ke basis data tanpa mengisi kolom ini berperilaku sebagai jenis
     * ber-anggaran (perilaku yang lebih ketat, karena menuntut Workspace).</p>
     *
     * <p><b>Efeknya bercabang jauh:</b></p>
     * <ul>
     *   <li>Di layar ZK, {@code ReimbursementPegawaiAction.aturBarisAnggaran()}
     *       memakainya untuk menampilkan/menyembunyikan baris "Anggaran" versus
     *       baris info akun tetap, dan untuk memilih jenis bawaan saat form
     *       dibuka.</li>
     *   <li>Di {@code onSave} maupun di {@code ReimbursementApiHelper.simpan},
     *       nilai ini menentukan validasi mana yang berlaku: bila {@code true},
     *       Workspace wajib; bila {@code false}, {@link #getAkun()} yang wajib
     *       sudah terisi — pengajuan ditolak dengan pesan yang menyuruh admin
     *       melengkapi akun pada tab Jenis Reimbursement.</li>
     *   <li>Nilainya diturunkan ke dokumen sebagai
     *       {@code ReimbursementPegawai.setTanpaAnggaran(!menggunakanAnggaran)},
     *       lalu ikut menentukan apakah {@code workspace} dokumen dikosongkan
     *       dan apakah {@link #getAkun()} disalin ke dokumen.</li>
     * </ul>
     *
     * <p><b>Waspadai bawaan yang berlawanan di REST.</b>
     * {@code MasterKeuanganApiHelper.simpan} membacanya dengan bawaan
     * {@code false} — kebalikan dari getter ini. Lihat catatan kuirk pada
     * dokumentasi kelas.</p>
     *
     * @return {@code true} bila pengaju wajib memilih Anggaran; {@code true}
     *         pula bila kolomnya belum pernah diisi
     */
    @Column(name = "menggunakan_anggaran")
    public Boolean getMenggunakanAnggaran() { return menggunakanAnggaran == null ? Boolean.TRUE : menggunakanAnggaran; }
    /**
     * Menyetel saklar wajib-anggaran.
     *
     * <p>Mengubah nilai ini <b>tidak</b> menyentuh klaim yang sudah tersimpan —
     * dokumen menyimpan salinannya sendiri pada kolom {@code tanpa_anggaran}.
     * Yang berubah adalah perilaku pengajuan berikutnya.</p>
     *
     * @param menggunakanAnggaran saklar; boleh {@code null} (dibaca sebagai
     *                            {@code true})
     */
    public void setMenggunakanAnggaran(Boolean menggunakanAnggaran) { this.menggunakanAnggaran = menggunakanAnggaran; }

    /**
     * Akun biaya tetap untuk jenis <b>tanpa</b> anggaran — inilah kolom paling
     * bernilai di katalog ini.
     *
     * <p>Wajib diisi admin pada jenis tanpa anggaran; layar ZK bahkan menolak
     * menyimpan jenis semacam itu tanpa akun ("Akun wajib dipilih untuk Jenis
     * Reimbursement TANPA anggaran — akun inilah yang dipakai semua pengajuan
     * jenis ini"). Jalur REST <b>tidak</b> menegakkan syarat itu saat menyimpan
     * master (penyimpanan sengaja diizinkan agar admin dapat melengkapi
     * bertahap); yang terjadi kemudian adalah pengajuan atas jenis tersebut
     * ditolak sampai akunnya dilengkapi. {@code MasterKeuanganApiHelper.daftar}
     * menandai keadaan ini lewat medan {@code akunLengkap}/{@code belumLengkap}
     * — untuk tipe {@code jenis_reimbursement} akun ditandai
     * {@code wajibUntukJurnal = false} karena jenis ber-anggaran memang tidak
     * memerlukannya.</p>
     *
     * <p><b>Bagaimana nilainya sampai ke buku besar</b> dijelaskan lengkap pada
     * dokumentasi kelas: ia <b>disalin</b> ke {@code ReimbursementPegawai.akun}
     * saat klaim disimpan, bukan dibaca ulang saat posting. Karena itu mengubah
     * akun di sini memindahkan pembebanan klaim <i>berikutnya</i>, bukan klaim
     * yang sudah tersimpan.</p>
     *
     * <p><b>Getter ini tidak destruktif.</b> Penugasan {@code akun = check(akun)}
     * hanya menukar proxy lazy dengan instance kanonik dari
     * {@code EntityIdentityMap} — identitas dan nilainya sama, tidak ada
     * penimpaan semantik seperti pada {@code Transaksi.getAkun()}.</p>
     *
     * <p><b>Catatan keamanan.</b> Kolom inilah yang menjadi taruhan kedua jalur
     * fail-open ({@code task_66986071}) yang diuraikan pada
     * <a href="#keamanan">catatan keamanan kelas</a>: siapa pun yang lolos
     * {@code MasterKeuanganApiHelper.bolehAksi} dapat mengarahkan seluruh klaim
     * tanpa-anggaran berikutnya ke akun mana pun di bagan akun, dan karena
     * entity ini tidak {@code @Audited}, perubahan itu tidak meninggalkan
     * riwayat siapa pun.</p>
     *
     * @return akun biaya yang sudah teresolusi dari proxy lazy, atau
     *         {@code null} bila belum ditentukan (lazim pada jenis ber-anggaran)
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "akun", nullable = true)
    public Akun getAkun() { akun = check(akun); return akun; }
    /**
     * Menyetel akun biaya tetap.
     *
     * <p>Dipanggil dari form ZK (nilai diambil dari atribut {@code "akun"} pada
     * {@code AmbilDataAkunBanbox}) dan dari {@code MasterKeuanganApiHelper.simpan}
     * (hasil {@code session.get(Akun.class, akunId)}; {@code null} bila
     * {@code akunId} tidak dikirim atau bernilai 0 — sehingga pembaruan REST
     * parsial dapat mengosongkan akun tanpa disengaja).</p>
     *
     * <p>Karena relasinya ber-{@code cascade} {@code PERSIST}/{@code MERGE},
     * menempelkan instance {@code Akun} detached yang sudah dimodifikasi akan
     * ikut menuliskan modifikasi itu ke bagan akun saat katalog disimpan.</p>
     *
     * @param akun akun beban; boleh {@code null}
     */
    public void setAkun(Akun akun) { this.akun = akun; }

    /**
     * Satuan kerja bawaan bagi pengajuan yang memakai jenis ini — bersifat
     * <b>prefill dan label</b>, bukan penyaring cakupan.
     *
     * <p>Perannya ada tiga, semuanya kosmetik atau memudahkan pengisian:</p>
     * <ul>
     *   <li>{@code ReimbursementPegawaiAction.aturBarisAnggaran()} mengisikan
     *       nilai ini ke banbox "Satuan Kerja" pada form pengajuan <b>hanya bila
     *       pengguna belum memilih sendiri</b> — pengaju tetap bebas
     *       menggantinya.</li>
     *   <li>{@code onSave} memakainya sebagai cadangan terakhir untuk
     *       {@code ReimbursementPegawai.setSatuanKerja(...)}, sesudah pilihan
     *       pengguna dan sesudah satuan kerja milik Workspace.</li>
     *   <li>{@link #toString()} menambahkannya sebagai sufiks dalam kurung agar
     *       jenis bernama mirip dapat dibedakan di combo.</li>
     * </ul>
     *
     * <p><b>Bukan penyaring.</b> Tidak ada satu pun query di layar ZK maupun di
     * kedua helper REST yang menyaring daftar jenis berdasarkan satuan kerja
     * pengguna; grid master bahkan menampilkan "(semua)" bila kolom ini kosong.
     * Ditambah ketiadaan kolom tenant pada entity ini, katalog ini efektif
     * global. {@code MasterKeuanganApiHelper.opsi} pun menayangkan seluruh
     * {@code rab.satuan_kerja} ({@code LIMIT 500}) tanpa filter, dan
     * {@code simpan} menerima {@code satuanKerjaId} apa pun tanpa memeriksa
     * relevansinya bagi pemanggil.</p>
     *
     * <p>Getter ini bersifat normalisasi proxy saja (lihat {@link #getAkun()}),
     * tidak destruktif.</p>
     *
     * @return satuan kerja bawaan yang sudah teresolusi, atau {@code null} bila
     *         jenis ini berlaku untuk semua satuan kerja
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "satuan_kerja", nullable = true)
    public SatuanKerja getSatuanKerja() { satuanKerja = check(satuanKerja); return satuanKerja; }
    /**
     * Menyetel satuan kerja bawaan.
     *
     * <p>Sama seperti {@link #setAkun(Akun)}, pembaruan lewat
     * {@code MasterKeuanganApiHelper.simpan} selalu menulis kolom ini — payload
     * tanpa {@code satuanKerjaId} akan mengosongkannya.</p>
     *
     * @param satuanKerja satuan kerja bawaan; {@code null} berarti "berlaku
     *                    untuk semua"
     */
    public void setSatuanKerja(SatuanKerja satuanKerja) { this.satuanKerja = satuanKerja; }

    /**
     * Penanda ketersediaan jenis di formulir pengajuan.
     *
     * <p><b>Default TRUE</b> bila kolomnya {@code null}. Nonaktifkan — jangan
     * hapus — adalah cara yang dianjurkan untuk memensiunkan sebuah jenis;
     * {@code MasterKeuanganApiHelper.hapus} bahkan menyarankannya secara
     * eksplisit ketika penghapusan ditolak karena jenisnya sudah dipakai
     * dokumen.</p>
     *
     * <p>Yang menegakkan bendera ini:</p>
     * <ul>
     *   <li>Layar ZK melewati jenis non-aktif saat mengisi combo pengajuan
     *       ({@code if (!Boolean.TRUE.equals(j.getAktif())) continue;}).</li>
     *   <li>{@code ReimbursementApiHelper.opsi} menyaringnya di SQL
     *       ({@code WHERE COALESCE(jr.aktif,true) = true}).</li>
     * </ul>
     *
     * <p><b>Yang TIDAK menegakkannya:</b> grid master ZK dan
     * {@code MasterKeuanganApiHelper.daftar} sengaja menampilkan jenis non-aktif
     * juga (memang layar administrasi), dan — lebih penting —
     * {@code ReimbursementApiHelper.simpan} <b>tidak memeriksa</b> bendera ini
     * saat menerima {@code jenisReimbursementId}. Klien REST yang mengirim id
     * jenis yang sudah dinonaktifkan tetap dapat membuat klaim atasnya; bendera
     * ini hanya menyembunyikan jenis dari daftar pilihan, bukan menolaknya.
     * Klaim lama yang menunjuk jenis non-aktif juga tetap utuh dan tetap
     * terjurnal.</p>
     *
     * @return {@code true} bila jenis masih ditawarkan; {@code true} pula bila
     *         kolomnya belum pernah diisi
     */
    @Column(name = "aktif")
    public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
    /**
     * Menyetel penanda ketersediaan.
     *
     * @param aktif penanda; boleh {@code null} (dibaca sebagai {@code true})
     */
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    /**
     * Stempel waktu perubahan terakhir baris ini.
     *
     * <p>Diisi otomatis dua kali: sekali oleh penginisialisasi field saat
     * {@code new JenisReimbursement()} (mencakup {@code INSERT}), dan setiap
     * kali baris diperbarui oleh kait {@link #onUpdate()}. Tidak ada jalur yang
     * menyetelnya secara manual.</p>
     *
     * <p><b>Ini satu-satunya jejak forensik yang dimiliki katalog ini.</b> Karena
     * kelas ini tidak {@code @Audited}, tidak ada tabel {@code _aud} yang
     * merekam nilai akun sebelumnya, dan tidak ada kolom "diubah oleh" — bila
     * akun biaya dipindahkan, yang tersisa hanyalah <i>kapan</i> perubahan itu
     * terjadi. Kolom ini juga tidak ditayangkan di layar ZK maupun di kedua
     * helper REST, jadi hanya terbaca lewat basis data.</p>
     *
     * @return waktu perubahan terakhir; praktis tidak pernah {@code null} untuk
     *         baris yang dibuat lewat entity ini, tetapi bisa {@code null} untuk
     *         baris yang disisipkan lewat SQL mentah
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_dirubah")
    public Date getTanggalDirubah() { return tanggalDirubah; }
    /**
     * Menyetel stempel perubahan.
     *
     * <p>Disediakan demi kelengkapan kontrak JavaBean/Hibernate; tidak ada
     * pemanggil di seluruh repo selain Hibernate saat memuat baris. Nilai yang
     * diset manual akan ditimpa {@link #onUpdate()} pada penyimpanan
     * berikutnya.</p>
     *
     * @param tanggalDirubah stempel waktu; boleh {@code null}
     */
    public void setTanggalDirubah(Date tanggalDirubah) { this.tanggalDirubah = tanggalDirubah; }

    /**
     * Kait siklus hidup JPA yang menyegarkan {@link #getTanggalDirubah()} tepat
     * sebelum setiap {@code UPDATE}.
     *
     * <p>Dipanggil <b>hanya oleh Hibernate</b>, tidak pernah dari kode aplikasi,
     * dan hanya pada pembaruan — bukan pada penyisipan (peran itu diambil alih
     * penginisialisasi field {@code tanggalDirubah}). Waktu diambil dari
     * {@code ais.ui.util.WaktuUtil.getDate()} agar konsisten dengan sumber waktu
     * yang dipakai seluruh aplikasi, bukan {@code new Date()} langsung.</p>
     *
     * <p>Penugasannya menyentuh <i>field</i>, bukan setter, sehingga tidak
     * memicu efek samping lain. Karena kelas memakai {@code dynamicUpdate},
     * kolom ini ikut serta dalam pernyataan {@code UPDATE} yang dibangun
     * Hibernate.</p>
     *
     * <p><b>Batasannya perlu disadari:</b> kait ini tidak menyala untuk
     * perubahan yang dilakukan lewat SQL mentah (mis. seed
     * {@code InitIndex.initDefaultJenisReimbursement()} yang menulis
     * {@code now()} sendiri) maupun untuk penyuntingan langsung di basis data.</p>
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() { tanggalDirubah = ais.ui.util.WaktuUtil.getDate(); }

    /**
     * Label jenis untuk ditampilkan di antarmuka: nama, ditambah nama satuan
     * kerja dalam kurung bila jenis ini terikat satuan kerja tertentu.
     *
     * <p>Inilah teks yang muncul sebagai item combo "Jenis Reimbursement" di
     * form pengajuan ZK ({@code jenisReimbursementCombo.appendItem(j.toString())}).
     * Sufiks satuan kerja penting di sana karena katalog ini tidak punya kolom
     * {@code kode} dan tidak punya indeks unik pada nama — tanpa sufiks itu, dua
     * jenis bernama sama dengan akun berbeda tidak dapat dibedakan pengaju.</p>
     *
     * <p><b>Membaca field secara langsung, bukan lewat getter.</b> Method ini
     * memakai {@code nama} dan {@code satuanKerja} mentah, sehingga melewati
     * normalisasi proxy {@code check(...)} yang dilakukan
     * {@link #getSatuanKerja()}. Konsekuensinya: bila {@code satuanKerja} masih
     * berupa proxy Hibernate yang belum terinisialisasi dan sesinya sudah
     * ditutup, pemanggilan {@code getId()}/{@code getNama()} padanya melempar
     * {@code LazyInitializationException}.</p>
     *
     * <p><b>Kegagalan ditelan secara sengaja.</b> Exception apa pun dari blok
     * satuan kerja ditangkap dan hanya dicatat ke
     * {@code ais.common.ErrorAuditUtil}; hasilnya label tetap dikembalikan,
     * hanya tanpa sufiks. Pilihan ini masuk akal untuk sebuah label (lebih baik
     * combo tampil dengan nama saja daripada seluruh form gagal dirender),
     * tetapi berarti masalah lazy-loading di sini tidak pernah terlihat
     * pengguna — hanya muncul di catatan audit galat.</p>
     *
     * <p>Tiga syarat harus terpenuhi agar sufiks ditambahkan: {@code satuanKerja}
     * tidak {@code null}, {@code id}-nya tidak {@code null} (menyaring instance
     * kosong hasil banbox yang belum dipilih), dan {@code nama}-nya tidak
     * {@code null}.</p>
     *
     * @return nama jenis yang sudah dipangkas spasinya, opsional diikuti
     *         {@code " (nama satuan kerja)"}; string kosong bila nama belum
     *         diisi — tidak pernah {@code null}
     */
    public String toString() {
        String s = nama == null ? "" : nama.trim();
        try {
            if (satuanKerja != null && satuanKerja.getId() != null && satuanKerja.getNama() != null) {
                s += " (" + satuanKerja.getNama() + ")";
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) JenisReimbursement.toString");
        }
        return s;
    }
}
