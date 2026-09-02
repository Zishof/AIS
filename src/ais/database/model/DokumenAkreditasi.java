package ais.database.model;

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

import ais.database.model.rab.SatuanKerja;
import ais.ui.util.WaktuUtil;

/**
 * Entity Hibernate untuk tabel {@code public.dokumen_akreditasi}: satu simpul di dalam
 * <b>pohon arsip dokumen</b> milik sebuah ruang akreditasi ({@link Akreditasi}). Satu baris
 * dapat berperan sebagai <i>folder</i> ("Ruang Arsip") maupun sebagai <i>berkas</i>
 * ("File Dokumen") — lihat bagian "Folder atau berkas?" di bawah; tidak ada kolom yang
 * membedakan keduanya.
 *
 * <h2>Peran dalam alur akreditasi</h2>
 * <p>{@link Akreditasi} adalah <i>ruang</i>/kategori arsip (mis. satu siklus akreditasi
 * BAN-PT/LAM sebuah program studi, audit eksternal keuangan, atau kategori dokumen mutu
 * internal). Entity ini adalah <b>isi</b> ruang tersebut: pohon folder dan berkas bukti
 * pendukung (borang, SK, sertifikat, notulen, dsb.) yang diunggah pengelola agar dapat
 * ditelusuri asesor/auditor maupun publik.</p>
 *
 * <p><b>Penting — batas peran terhadap borang.</b> Entity ini adalah <b>lemari berkas</b>,
 * bukan sumber angka borang. Berbeda dengan entity akreditasi lain seperti
 * {@link OrganisasiDosen} (yang isinya dibaca langsung oleh generator borang BAN-PT dan
 * karena itu rentan salah petakan kolom), tidak ada satu pun kode di codebase ini yang
 * menurunkan nilai sel borang dari kolom-kolom {@code dokumen_akreditasi}. Konsumennya hanya
 * menampilkan/mengekspor daftar dokumen apa adanya:</p>
 * <ul>
 *   <li>{@link ais.action.master.DokumenAkreditasiAction} — penjelajah dokumen (pohon +
 *       daftar tabel) yang tertanam pada baris {@link ais.action.master.AkreditasiAction}
 *       yang diperluas; juga dipakai dalam mode ringkas oleh dasbor.</li>
 *   <li>{@link ais.action.master.helper.util.DokumenAkreditasiTreeModel} — model pohon ZK,
 *       tempat seluruh query anak/jumlah-anak dan filter satuan kerja berada.</li>
 *   <li>{@link ais.action.master.dashboard.admin.DashboardDokumenAkreditasi} — dasbor
 *       "Sistem Informasi Dokumen" (dipasang lewat {@code WEB-INF/z/x/y/document.zul}).</li>
 *   <li>{@link ais.action.servlet.Document} — servlet portal DMS {@code /document}
 *       (JSP, di luar ZK) untuk penelusuran katalog dan unduh berkas.</li>
 *   <li>{@link ais.action.master.AkreditasiAction#getDspace} /
 *       {@code getDspaceDokumenAkreditasi} — ekspor rekursif satu sub-pohon dokumen menjadi
 *       item/koleksi di repositori DSpace ({@link DspaceInformation}).</li>
 *   <li>{@link ais.action.master.obe.PikobeAction} — tab "Dokumen Akreditasi Pendukung"
 *       pada paket informasi kurikulum OBE per program studi (daftar baca-saja).</li>
 * </ul>
 *
 * <h2>Folder atau berkas?</h2>
 * <p>Tidak ada kolom penanda jenis. Pembedaan dilakukan <b>di luar entity</b>, oleh dua
 * pemeriksaan yang berbeda dan tidak selalu sepakat:</p>
 * <ul>
 *   <li><b>Ada lampiran?</b> — {@code LampiranLain.ambil(getId(), DokumenAkreditasi.class.getName())}
 *       menghasilkan baris {@link ais.database.model.file.LampiranLain} bila simpul ini
 *       memiliki berkas terunggah. Dipakai {@code DokumenAkreditasiAction} dan servlet DMS
 *       untuk memilih ikon/label "File Dokumen" versus "Ruang Arsip" dan untuk memunculkan
 *       tautan unduh. Perhatikan bahwa relasi ini <b>bukan</b> foreign key: keterkaitan
 *       hanya lewat pasangan {@code (ref = id, clazz = nama kelas ini)}.</li>
 *   <li><b>Punya anak?</b> — jumlah baris yang {@code induk}-nya simpul ini. Servlet DMS
 *       memakai kriteria inilah untuk melabeli baris sebagai "Sub Ruang" versus "Dokumen".</li>
 * </ul>
 * <p>Konsekuensinya satu simpul bisa sekaligus punya anak dan punya lampiran, dan kedua
 * konsumen di atas akan melabelinya berbeda. Teks bantuan pada form penyuntingan meminta
 * pengguna memilih salah satu peran secara manual (kosongkan lampiran bila dipakai sebagai
 * ruang arsip; kompres jadi ZIP bila lampirannya lebih dari satu) — konvensi ini
 * <b>tidak ditegakkan</b> oleh kode mana pun.</p>
 *
 * <h2>Hierarki dan urutan</h2>
 * <p>Pohon dibentuk lewat self-reference {@link #getInduk()} ({@code induk IS NULL} berarti
 * simpul akar di dalam ruang akreditasinya). Urutan tampil di seluruh konsumen konsisten:
 * {@link #getNomorUrut()}, lalu {@link #getKode()}, lalu {@link #getNama()}. Tidak ada
 * batas kedalaman dan tidak ada pencegahan siklus pada level entity — pemanggil yang menelusuri
 * ke atas/bawah harus menjaganya sendiri (mis. himpunan {@code visited} di
 * {@code Document.buildDokumenBreadcrumbs} dan pemeriksaan duplikat di
 * {@code DokumenAkreditasiTreeModel.getChildDeepSet}).</p>
 *
 * <h2>Cakupan akses</h2>
 * <p>Dua kolom membatasi siapa yang boleh melihat satu dokumen, dan keduanya diterapkan oleh
 * <b>pemanggil</b>, bukan oleh entity:</p>
 * <ul>
 *   <li>{@link #getSatuanKerja()} pada baris ini — dokumen tanpa satuan kerja dianggap
 *       "umum"/milik semua; dokumen bersatuan-kerja hanya tampil bagi pengguna yang cakupan
 *       satuan kerjanya ({@code SekolahUtil.ambilSatuanKerjas()}) mencakup satuan kerja itu
 *       atau induknya.</li>
 *   <li>{@code kodeGrupPengguna} pada {@link Akreditasi} induknya — membatasi ruang arsip ke
 *       daftar role tertentu.</li>
 * </ul>
 * <p><b>Kuirk yang perlu diketahui pembaca kode:</b> seluruh penerap filter satuan kerja untuk
 * entity ini bersifat <i>fail-open</i> — bila himpunan satuan kerja pengguna kosong, filter
 * diganti {@code 1=1} (lihat {@code DokumenAkreditasiAction.initCriteria},
 * {@code DokumenAkreditasiTreeModel.createSatuanKerjaRestriction},
 * {@code Document.addSatuanKerjaCriterion} dan {@code Document.isSatuanKerjaVisible}), sehingga
 * seluruh dokumen ruang tersebut terlihat. Ini pola yang sama dengan yang tercatat di seluruh
 * aplikasi, bukan kekhususan entity ini.</p>
 *
 * <h2>Warisan dari {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} adalah POJO abstrak biasa — <b>bukan</b> {@code @Entity} atau
 * {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti induknya. Karena itu
 * {@link #getId() id}, {@link #getOleh() oleh}, {@link #getOlehId() olehId},
 * {@link #getTanggal_dirubah() tanggal_dirubah}, serta {@code kode}/{@code nama}/
 * {@code keterangan}/{@code nomorUrut} <b>wajib</b> dideklarasikan ulang di kelas ini agar
 * ikut terpetakan; pengulangan itu keharusan teknis, bukan duplikasi yang bisa dibersihkan.
 * Hanya {@code nama} dan {@code keterangan} yang diberi {@link Column} eksplisit; sisanya
 * memakai nama properti apa adanya sebagai nama kolom (strategi penamaan bawaan JPA — tidak
 * ada {@code naming_strategy} khusus di {@code hibernate.cfg.xml}).</p>
 *
 * <h2>Audit dan riwayat</h2>
 * <p>Kelas ditandai {@link Audited} (Hibernate Envers) sehingga setiap create/update/delete
 * merekam revisi; riwayat itulah yang ditampilkan tombol revisi
 * ({@code RevisiHelper.createNewRevisi}) di penjelajah dan dasbor. Terpisah dari Envers,
 * {@link #onUpdate()} memperbarui {@link #getTanggal_dirubah()} lewat
 * {@link javax.persistence.PreUpdate}, sementara {@code oleh}/{@code olehId} diisi
 * interceptor audit yang sama.</p>
 *
 * @see Akreditasi
 * @see ais.database.model.file.LampiranLain
 * @see ais.action.master.DokumenAkreditasiAction
 * @see ais.action.master.helper.util.DokumenAkreditasiTreeModel
 * @see ais.action.servlet.Document
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "dokumen_akreditasi")
public class DokumenAkreditasi extends GeneralValueObject {

    private static final long serialVersionUID = 2463821577548439808L;

    /** Primary key {@code dokumen_akreditasi.id}; lihat {@link #getId()}. */
    private Long id;
    /** Nama pengguna terakhir yang mengubah baris; lihat {@link #getOleh()}. */
    private String oleh;
    /** Id/username pengguna terakhir yang mengubah baris; lihat {@link #getOlehId()}. */
    private String olehId;
    /**
     * Waktu perubahan terakhir; lihat {@link #getTanggal_dirubah()}. Diinisialisasi ke waktu
     * pembuatan object di JVM ({@link WaktuUtil#getDate()}) sehingga baris baru sudah bernilai
     * walau belum pernah melalui {@link #onUpdate()}.
     */
    private Date tanggal_dirubah = WaktuUtil.getDate();

    /** Kode ringkas dokumen (mis. nomor butir borang); lihat {@link #getKode()}. */
    private String kode;
    /** Nama ruang arsip/berkas — satu-satunya kolom wajib isi dari form; lihat {@link #getNama()}. */
    private String nama;
    /** Keterangan bebas (kolom {@code text}); lihat {@link #getKeterangan()}. */
    private String keterangan;
    /** Tanggal dokumen (tanggal terbit/berlaku, bukan tanggal unggah); lihat {@link #getTanggalDokumen()}. */
    private Date tanggalDokumen;
    /** Ruang akreditasi pemilik dokumen (wajib); lihat {@link #getAkreditasi()}. */
    private Akreditasi akreditasi;
    /** Urutan tampil di antara saudara sekandung; lihat {@link #getNomorUrut()}. */
    private Integer nomorUrut;
    /** Penanda aktif/tampil; lihat {@link #getAktif()}. */
    private Boolean aktif;
    /** Folder/dokumen induk pada pohon arsip; {@code null} berarti simpul akar. Lihat {@link #getInduk()}. */
    private DokumenAkreditasi induk;
    /** Satuan kerja pemilik dokumen, penentu cakupan akses; lihat {@link #getSatuanKerja()}. */
    private SatuanKerja satuanKerja;

    /**
     * Konstruktor tanpa argumen yang dibutuhkan Hibernate untuk instansiasi hasil query.
     * Seluruh field kosong kecuali {@link #getTanggal_dirubah()} yang sudah terisi waktu saat ini.
     */
    public DokumenAkreditasi() {
    }

    /**
     * Konstruktor pembuatan simpul baru pada posisi tertentu di pohon arsip. Dipakai layar
     * penjelajah untuk tiga aksi: "Tambah Ruang/File" pada toolbar (induk = folder yang sedang
     * dibuka, boleh {@code null} untuk akar), tombol "Tambah Child" pada satu baris pohon
     * (induk = baris tersebut), dan "Copy" (induk = induk baris sumber, lalu kode/nama/
     * keterangan/nomor urut disalin oleh pemanggil).
     *
     * <p>Hanya kedua relasi ini yang diisi; {@code nama} tetap kosong sehingga
     * {@code DokumenAkreditasiAction.onSave} akan menolak penyimpanan sampai pengguna mengisinya.</p>
     *
     * @param akreditasi ruang akreditasi pemilik dokumen (wajib; kolom {@code akreditasi} NOT NULL)
     * @param induk      folder induk tempat simpul baru dipasang, {@code null} untuk simpul akar
     */
    public DokumenAkreditasi(Akreditasi akreditasi, DokumenAkreditasi induk) {
        this.akreditasi = akreditasi;
        this.induk = induk;
    }

    /**
     * Primary key baris ({@code IDENTITY}, dibangkitkan database — karena itu
     * {@code insertable = false}). Selain sebagai kunci Hibernate, nilai ini juga dipakai
     * sebagai <b>kunci penghubung tak-terpetakan</b> ke {@link ais.database.model.file.LampiranLain}
     * (kolom {@code ref}) dan ke {@link DspaceInformation}, serta muncul apa adanya di URL
     * portal DMS ({@code /document?akreditasi=...&induk=...} dan {@code ?action=download&id=...}).
     *
     * @return id baris, atau {@code null} bila belum tersimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() {
        return id;
    }

    /**
     * Menyetel primary key. Praktis hanya dipanggil Hibernate; kode aplikasi memperoleh id dari
     * database setelah penyimpanan.
     *
     * @param id id baris
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Id/username pengguna yang terakhir menyimpan baris ini, diisi interceptor audit.
     *
     * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
     */
    public String getOlehId() {
        return olehId;
    }

    /**
     * Menyetel id pengguna pengubah terakhir. Mengikuti kontrak {@link GeneralValueObject}:
     * nilai {@code null} atau berisi spasi saja <b>diabaikan senyap</b>, sehingga nilai audit
     * yang sudah ada tidak pernah tertimpa oleh nilai kosong.
     *
     * @param olehId id pengguna; diabaikan bila {@code null}/kosong
     */
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) {
            return;
        }
        this.olehId = olehId;
    }

    /**
     * Nama pengguna yang terakhir menyimpan baris ini, diisi interceptor audit.
     *
     * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
     */
    public String getOleh() {
        return oleh;
    }

    /**
     * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
     * {@code null}/kosong diabaikan senyap agar jejak audit lama tidak hilang.
     *
     * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
     */
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) {
            return;
        }
        this.oleh = oleh;
    }

    /**
     * Callback {@link javax.persistence.PreUpdate}: dipanggil Hibernate tepat sebelum
     * {@code UPDATE} dieksekusi, mendelegasikan ke
     * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang memperbarui
     * {@link #getTanggal_dirubah()} (dan bila tersedia konteks pengguna, {@code oleh}/{@code olehId}).
     *
     * <p>Hanya berjalan pada pembaruan baris yang sudah ada — bukan pada {@code INSERT}; nilai
     * awal {@code tanggal_dirubah} untuk baris baru berasal dari inisialisasi field.</p>
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    /**
     * Waktu perubahan terakhir baris (presisi {@code TIMESTAMP}), dipelihara {@link #onUpdate()}.
     * Bukan tanggal dokumen — untuk itu gunakan {@link #getTanggalDokumen()}.
     *
     * @return waktu perubahan terakhir; tidak pernah {@code null} pada object hasil konstruksi normal
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    /**
     * Menyetel waktu perubahan terakhir. Tanpa validasi; normalnya hanya dipanggil interceptor audit.
     *
     * @param tanggal_dirubah waktu perubahan terakhir
     */
    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }

    /**
     * Representasi teks {@code "<id>-<nama>"} (bagian id kosong bila baris belum tersimpan).
     * Dipakai komponen ZK generik (kombo/daftar) dan keluaran debug; tidak dipakai sebagai
     * kunci bisnis di mana pun.
     *
     * @return gabungan id dan nama dokumen, dipisahkan tanda hubung
     */
    @Override
    public String toString() {
        return (id == null ? "" : id.toString()) + "-" + getNama();
    }

    /**
     * Kode ringkas dokumen (mis. nomor butir borang "4.5.1"), dinormalisasi: {@code null}
     * dikembalikan sebagai string kosong dan spasi tepi dipangkas.
     *
     * <p>Normalisasi ini <b>hanya berlaku saat membaca</b> — {@link #setKode(String)} menyimpan
     * nilai apa adanya, sehingga kolom di database bisa berisi {@code null} maupun teks berspasi
     * tepi. Perbedaan itu terasa pada pencarian ({@code ilike} bekerja atas nilai mentah di
     * database, bukan atas hasil getter ini) dan pada pengurutan
     * {@code addOrder(Order.asc("kode"))} yang juga memakai nilai mentah.</p>
     *
     * <p>Menguatkan kontrak {@link GeneralValueObject#getKode()} yang boleh mengembalikan
     * {@code null}; pemanggil di kelas ini (mis. {@code kode.isEmpty()} pada dasbor) mengandalkan
     * jaminan non-null tersebut.</p>
     *
     * @return kode dokumen sudah dipangkas, atau {@code ""} bila belum diisi; tidak pernah {@code null}
     */
    public String getKode() {
        return kode == null ? "" : kode.trim();
    }

    /**
     * Menyetel kode dokumen apa adanya, tanpa validasi maupun pemangkasan. Layar penyuntingan
     * sudah memangkas sendiri sebelum memanggil method ini; jalur impor Excel tidak.
     *
     * @param kode kode dokumen, boleh {@code null}
     */
    public void setKode(String kode) {
        this.kode = kode;
    }

    /**
     * Nama ruang arsip/berkas — label utama yang tampil di pohon, tabel, breadcrumb portal DMS,
     * dan judul item DSpace hasil ekspor. Kolom {@code NOT NULL} sepanjang 255 karakter, dan
     * satu-satunya kolom yang divalidasi wajib-isi oleh form penyuntingan.
     *
     * <p>Sama seperti {@link #getKode()}, nilai {@code null} dinormalisasi menjadi {@code ""}
     * dan spasi tepi dipangkas saat dibaca — menguatkan kontrak
     * {@link GeneralValueObject#getNama()} yang boleh mengembalikan {@code null}.</p>
     *
     * @return nama dokumen sudah dipangkas, atau {@code ""} bila belum diisi; tidak pernah {@code null}
     */
    @Column(name = "nama", nullable = false, length = 255)
    public String getNama() {
        return nama == null ? "" : nama.trim();
    }

    /**
     * Menyetel nama dokumen apa adanya, tanpa validasi. Pemeriksaan wajib-isi dilakukan di layar
     * ({@code DokumenAkreditasiAction.onSave}), bukan di sini — menyimpan entity ini lewat jalur
     * lain (impor Excel, kode batch) dengan nama kosong akan gagal di level constraint database.
     *
     * @param nama nama dokumen
     */
    public void setNama(String nama) {
        this.nama = nama;
    }

    /**
     * Keterangan bebas dokumen (kolom {@code text}, tanpa batas panjang praktis). Ditampilkan
     * sebagai baris deskripsi di pohon, tabel, dan kartu katalog portal DMS, serta ikut
     * dicari kata kuncinya bersama {@code nama} dan {@code kode}.
     *
     * <p>Mematuhi kontrak {@link GeneralValueObject#getKeterangan()}: {@code null} dinormalisasi
     * menjadi {@code ""} sehingga pemanggil tidak perlu memeriksa {@code null}. Berbeda dengan
     * {@link #getKode()}/{@link #getNama()}, di sini spasi tepi <b>tidak</b> dipangkas.</p>
     *
     * @return keterangan dokumen, atau {@code ""} bila belum diisi; tidak pernah {@code null}
     */
    @Column(name = "keterangan", columnDefinition = "text", nullable = true)
    public String getKeterangan() {
        return keterangan == null ? "" : keterangan;
    }

    /**
     * Menyetel keterangan dokumen apa adanya, tanpa validasi maupun penyaringan HTML.
     *
     * @param keterangan keterangan dokumen, boleh {@code null}
     */
    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    /**
     * Ruang akreditasi pemilik dokumen ini (kolom {@code akreditasi}, {@code NOT NULL}). Menentukan
     * konteks lengkap dokumen: jenis/lembaga/lingkup/tingkat akreditasi, jurusan, tahun, sekaligus
     * pembatas role lewat {@code kodeGrupPengguna} milik ruang tersebut.
     *
     * <p><b>Non-obvious:</b> relasi ini {@code LAZY}, dan getter memanggil
     * {@link GeneralValueObject#check(Object)} lalu <b>menugaskan kembali hasilnya ke field</b>.
     * Itu idiom baku seluruh entity AIS untuk meresolusi proxy lazy yang session-nya sudah
     * tertutup (cache in-memory, lalu inisialisasi proxy, lalu reload lewat session baru). Penugasan
     * kembali hanyalah memoisasi hasil resolusi di dalam object — <b>bukan</b> penulisan balik ke
     * database dan tidak memicu {@code UPDATE}; lihat penjelasan lengkap di
     * {@link GeneralValueObject}.</p>
     *
     * <p>Cascade {@code PERSIST}/{@code MERGE} berarti menyimpan dokumen ikut menyimpan ruang
     * akreditasi yang belum tersimpan; tidak ada cascade {@code REMOVE}, sehingga menghapus dokumen
     * tidak pernah menyentuh ruang akreditasinya.</p>
     *
     * @return ruang akreditasi pemilik dokumen, sudah diresolusi bila sebelumnya berupa proxy lazy
     * @see GeneralValueObject#check(Object)
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "akreditasi", nullable = false)
    public Akreditasi getAkreditasi() {
        akreditasi = check(akreditasi);
        return akreditasi;
    }

    /**
     * Menyetel ruang akreditasi pemilik dokumen. Tanpa validasi: tidak ada pemeriksaan bahwa nilai
     * ini konsisten dengan ruang akreditasi milik {@link #getInduk()}, sehingga secara teknis satu
     * sub-pohon dapat "pindah ruang" sebagian. Layar penyuntingan selalu mengisinya dari ruang
     * yang sedang dibuka, jadi ketidakkonsistenan hanya mungkin dari jalur impor/kode batch.
     *
     * @param akreditasi ruang akreditasi pemilik dokumen
     */
    public void setAkreditasi(Akreditasi akreditasi) {
        this.akreditasi = akreditasi;
    }

    /**
     * Tanggal dokumen (presisi {@code DATE}) — tanggal terbit/berlaku berkas menurut isinya,
     * berbeda dari {@link #getTanggal_dirubah()} yang mencatat waktu penyuntingan baris.
     *
     * <p><b>Non-obvious:</b> bila kolom {@code null}, getter mengembalikan <b>tanggal hari ini</b>
     * ({@link WaktuUtil#getDate()}), bukan {@code null}. Nilai pengganti ini tidak ditulis ke field
     * maupun ke database (bukan getter destruktif), tetapi bersifat menular lewat form: layar
     * penyuntingan mengisi datebox dengan hasil getter ini, lalu {@code onSave} menyimpan kembali
     * isi datebox tersebut — sehingga <b>menyunting dan menyimpan ulang dokumen lama yang tanggalnya
     * kosong akan memateraikan tanggal hari itu ke database</b>. Konsekuensi lain: tampilan tidak
     * pernah bisa membedakan "tanggal belum diisi" dari "dokumen bertanggal hari ini".</p>
     *
     * @return tanggal dokumen, atau tanggal hari ini bila kolom masih kosong; tidak pernah {@code null}
     */
    @Temporal(TemporalType.DATE)
    public Date getTanggalDokumen() {
        return tanggalDokumen == null ? WaktuUtil.getDate() : tanggalDokumen;
    }

    /**
     * Menyetel tanggal dokumen apa adanya, tanpa validasi (tanggal masa depan pun diterima).
     * Nilai {@code null} tersimpan sebagai {@code null} — tetapi tidak akan pernah terbaca kembali
     * sebagai {@code null}; lihat {@link #getTanggalDokumen()}.
     *
     * @param tanggalDokumen tanggal dokumen, boleh {@code null}
     */
    public void setTanggalDokumen(Date tanggalDokumen) {
        this.tanggalDokumen = tanggalDokumen;
    }

    /**
     * Urutan tampil dokumen di antara saudara sekandung (kunci pengurutan pertama di seluruh
     * konsumen, sebelum {@code kode} dan {@code nama}).
     *
     * <p><b>Non-obvious:</b> nilai {@code null} maupun nilai {@code <= 0} dinormalisasi menjadi
     * {@code 1} saat dibaca. Sama seperti {@link #getTanggalDokumen()}, nilai pengganti ini tidak
     * ditulis ke field, tetapi menular lewat form (intbox diisi dari getter, lalu {@code onSave}
     * menyimpan isinya) sehingga penyimpanan ulang memateraikan {@code 1} ke database.</p>
     *
     * <p>Perlu diperhatikan bahwa pengurutan di database memakai <b>nilai mentah kolom</b>, bukan
     * hasil normalisasi ini: baris ber-{@code nomor_urut} {@code null} atau negatif akan terurut
     * sesuai aturan {@code ORDER BY} database (mis. {@code NULL} terakhir pada PostgreSQL untuk
     * urutan menaik), <b>tidak</b> seolah-olah bernilai 1 seperti yang ditampilkan getter.
     * Menguatkan kontrak {@link GeneralValueObject#getNomorUrut()} yang boleh {@code null}.</p>
     *
     * @return nomor urut positif; {@code 1} bila kolom kosong atau tidak positif; tidak pernah {@code null}
     */
    public Integer getNomorUrut() {
        return nomorUrut == null || nomorUrut.intValue() <= 0 ? Integer.valueOf(1) : nomorUrut;
    }

    /**
     * Menyetel nomor urut tampil apa adanya, tanpa validasi. Nilai {@code null}/nol/negatif
     * diterima database tetapi akan tampil sebagai {@code 1}; lihat {@link #getNomorUrut()}.
     *
     * @param nomorUrut nomor urut tampil, boleh {@code null}
     */
    public void setNomorUrut(Integer nomorUrut) {
        this.nomorUrut = nomorUrut;
    }

    /**
     * Penanda apakah dokumen ditampilkan. Dokumen non-aktif disembunyikan dari portal DMS publik
     * ({@code Document}) dan dari dasbor, tetapi <b>tetap tampil</b> di layar pengelolaan
     * {@code DokumenAkreditasiAction} (yang justru menyediakan checkbox untuk mengubahnya) —
     * jadi ini penanda publikasi, bukan penghapusan lunak.
     *
     * <p><b>Non-obvious:</b> {@code null} dibaca sebagai {@link Boolean#TRUE} — dokumen lama yang
     * belum pernah disentuh kolom ini dianggap <b>aktif</b>. Perilaku ini konsisten dengan kriteria
     * query para konsumen yang selalu menulis {@code aktif IS NULL OR aktif = true}.</p>
     *
     * @return {@code true} bila dokumen aktif/tampil; {@code true} juga bila kolom masih kosong
     */
    public Boolean getAktif() {
        return aktif == null ? Boolean.TRUE : aktif;
    }

    /**
     * Menyetel penanda aktif. Dipanggil langsung oleh checkbox "Aktif" di pohon dan di tabel
     * penjelajah, yang segera menyimpannya lewat {@code Common.refreshSaveOrUpdate} — perubahan
     * bersifat instan tanpa dialog konfirmasi.
     *
     * @param aktif {@code true} untuk menampilkan dokumen, {@code false} untuk menyembunyikannya
     */
    public void setAktif(Boolean aktif) {
        this.aktif = aktif;
    }

    /**
     * Folder/dokumen induk pada pohon arsip (kolom {@code induk}, boleh {@code null}).
     * {@code null} berarti simpul ini berada di level akar ruang akreditasinya — dan justru
     * nilai {@code null} inilah yang dipakai konsumen sebagai kriteria "daftar akar"
     * ({@code Restrictions.isNull("induk")}).
     *
     * <p>Seperti {@link #getAkreditasi()}, getter meresolusi proxy lazy lewat
     * {@link GeneralValueObject#check(Object)} dan memoisasi hasilnya ke field (bukan penulisan
     * balik ke database). Karena resolusi terjadi per-akses, penelusuran ke atas
     * ({@code cursor = cursor.getInduk()}) berpotensi memicu satu query per tingkat kedalaman;
     * pemanggil yang membangun breadcrumb sudah membatasi diri dengan himpunan {@code visited}.</p>
     *
     * <p>Tidak ada batasan yang mencegah siklus ({@code A → B → A}) maupun simpul yang menjadi
     * induk dirinya sendiri: layar penyuntingan tidak pernah menawarkan pemindahan induk secara
     * bebas, tetapi jalur impor Excel dapat menuliskan kolom {@code induk} apa saja.</p>
     *
     * @return dokumen induk, atau {@code null} bila simpul ini berada di akar
     * @see GeneralValueObject#check(Object)
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "induk", nullable = true)
    public DokumenAkreditasi getInduk() {
        induk = check(induk);
        return induk;
    }

    /**
     * Menyetel dokumen induk apa adanya, tanpa validasi siklus maupun konsistensi ruang akreditasi.
     *
     * @param induk dokumen induk, {@code null} untuk menempatkan simpul di akar
     */
    public void setInduk(DokumenAkreditasi induk) {
        this.induk = induk;
    }

    /**
     * Satuan kerja pemilik dokumen (kolom {@code satuan_kerja}, boleh {@code null}). Menentukan
     * cakupan akses: {@code null} berarti dokumen umum yang terlihat semua pengguna, sedangkan
     * nilai terisi membatasi dokumen ke pengguna yang cakupan satuan kerjanya mencakup satuan
     * kerja ini atau salah satu induknya pada pohon {@code SatuanKerja}.
     *
     * <p>Penegakan pembatasan itu sepenuhnya berada di sisi pemanggil (lihat bagian "Cakupan
     * akses" pada dokumentasi kelas, termasuk catatan perilaku <i>fail-open</i>-nya); entity ini
     * hanya menyimpan nilainya.</p>
     *
     * <p>Seperti dua relasi lainnya, getter meresolusi proxy lazy lewat
     * {@link GeneralValueObject#check(Object)} dan memoisasi hasilnya ke field — bukan penulisan
     * balik ke database.</p>
     *
     * @return satuan kerja pemilik dokumen, atau {@code null} bila dokumen bersifat umum
     * @see GeneralValueObject#check(Object)
     */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "satuan_kerja", nullable = true)
    public SatuanKerja getSatuanKerja() {
        satuanKerja = check(satuanKerja);
        return satuanKerja;
    }

    /**
     * Menyetel satuan kerja pemilik dokumen. Tanpa validasi: tidak diperiksa apakah pengguna yang
     * menyimpan benar-benar berhak atas satuan kerja tersebut, sehingga dokumen dapat dipindahkan
     * ke satuan kerja mana pun yang muncul di komponen pemilih.
     *
     * @param satuanKerja satuan kerja pemilik, {@code null} untuk menjadikan dokumen umum
     */
    public void setSatuanKerja(SatuanKerja satuanKerja) {
        this.satuanKerja = satuanKerja;
    }
}
