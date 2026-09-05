package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.repo_bitstream} pada
 * modul repositori institusional (mirip DSpace — lihat juga {@code ais.ui.dspace.DspaceCommon},
 * {@link RepoItem}, {@link RepoCollection}) untuk skripsi/tesis/jurnal.
 * Merepresentasikan satu berkas fisik (bitstream) yang menjadi bagian dari
 * satu item repositori ({@code itemId}, referensi ke {@link RepoItem} tanpa
 * relasi JPA eksplisit) — path penyimpanan di server ({@code pathSistem}),
 * metadata berkas ({@code namaFile}, {@code mimeType}, {@code ukuranByte},
 * {@code checksum}), pengelompokan bundle ala DSpace ({@code bundleName},
 * default {@code "ORIGINAL"}), serta kebijakan akses ({@code accessPolicy},
 * default {@code "OPEN_ACCESS"}).
 *
 * <p>
 * {@code sourceClass}/{@code sourceId} menyimpan referensi polimorfik opsional
 * ke entitas asal yang mengunggah berkas ini (mis. tugas akhir/jurnal),
 * sedangkan {@code dspaceUuid} menyimpan UUID padanan bila berkas ini
 * bermigrasi dari/ke instalasi DSpace asli. Sejumlah field tambahan mendukung
 * alur kerja pengelolaan berkas: pemeriksaan plagiarisme Turnitin
 * ({@code turnitinSubmitted}/{@code turnitinSubmittedAt}), pemindaian virus
 * ({@code virusScanStatus}, default {@code "PENDING"}; {@code virusScannedAt}),
 * validitas tanda tangan digital ({@code signatureValid}), penomoran versi
 * berkas ({@code fileVersion}, default {@code 1}), tahapan/jenis naskah pada
 * alur review jurnal ala OJS ({@code journalStage}, {@code journalGenre},
 * {@code reviewRound}), serta status penyimpanan konten ({@code storageState},
 * default {@code "PENDING_CONTENT"}) dan referensi konten terkait
 * ({@code contentRef}). Diaudit lewat Hibernate Envers ({@code @Audited}).
 * </p>
 *
 * <h2>Relasi dengan mekanisme lampiran generik ({@code ais.database.model.file.LampiranLain})</h2>
 * <p>
 * Berkas fisik entitas ini <b>tidak</b> memakai mekanisme lampiran generik AIS sebagai jalur
 * utama: penyimpanan primer adalah kolom milik entitas sendiri ({@code pathSistem}, path
 * dikelola langsung oleh modul repositori, independen dari tabel {@code LampiranLain}). Namun
 * {@code RepositoryPublicService.resolveBitstreamFile(RepoBitstream)} memakai
 * {@code LampiranLain} sebagai jalur <b>fallback sekunder</b>: bila {@code pathSistem} kosong
 * atau berkas terkelolanya tidak ditemukan, ia mencari baris {@code LampiranLain} dengan
 * {@code ref = getId()} dan {@code jenis = RepoBitstream.class.getName()} — pola
 * ref+jenis-namespace yang sama dipakai luas di modul lain pada basis kode ini (lihat entri
 * memori proyek soal perbaikan tabrakan jenis-namespace lampiran). Konsekuensinya: FK ke
 * {@code LampiranLain} bersifat opsional dan longgar (id + nama kelas sebagai string, bukan FK
 * database sesungguhnya), dipakai untuk kompatibilitas mundur pada berkas yang sempat diunggah
 * lewat jalur lampiran generik sebelum modul repositori punya penyimpanan berkasnya sendiri.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "repo_bitstream")
public class RepoBitstream extends GeneralValueObject {

    private static final long serialVersionUID = 1L;
    /** Primary key baris ini. Lihat {@link #getId()}. */
    private Long id;
    /** Id {@link RepoItem} yang memiliki berkas ini. Lihat {@link #getItemId()}. */
    private Long itemId;
    /** Nama berkas asli. Lihat {@link #getNamaFile()}. */
    private String namaFile;
    /** Tipe MIME berkas. Lihat {@link #getMimeType()}. */
    private String mimeType;
    /** Path fisik penyimpanan berkas di server. Lihat {@link #getPathSistem()}. */
    private String pathSistem;
    /** Ukuran berkas dalam byte. Lihat {@link #getUkuranByte()}. */
    private Long ukuranByte;
    /** Checksum (mis. SHA/MD5) isi berkas untuk verifikasi integritas. Lihat {@link #getChecksum()}. */
    private String checksum;
    /** Nama bundle ala DSpace (pengelompokan berkas dalam satu item). Lihat {@link #getBundleName()}. */
    private String bundleName;
    /** Deskripsi berkas. Lihat {@link #getDescription()}. */
    private String description;
    /** Kebijakan akses berkas ini. Lihat {@link #getAccessPolicy()}. */
    private String accessPolicy;
    /** Nama kelas entitas AIS sumber unggahan berkas ini. Lihat {@link #getSourceClass()}. */
    private String sourceClass;
    /** Id baris pada entitas sumber ({@link #sourceClass}). Lihat {@link #getSourceId()}. */
    private Long sourceId;
    /** UUID padanan berkas ini pada instalasi DSpace eksternal. Lihat {@link #getDspaceUuid()}. */
    private String dspaceUuid;
    /** Penanda berkas utama/representatif item ini. Lihat {@link #getPrimaryFile()}. */
    private Boolean primaryFile;
    /** Penanda berkas sudah dikirim ke Turnitin. Lihat {@link #getTurnitinSubmitted()}. */
    private Boolean turnitinSubmitted;
    /** Waktu berkas dikirim ke Turnitin. Lihat {@link #getTurnitinSubmittedAt()}. */
    private Date turnitinSubmittedAt;
    /** Status pemindaian virus berkas ini. Lihat {@link #getVirusScanStatus()}. */
    private String virusScanStatus;
    /** Waktu pemindaian virus terakhir dijalankan. Lihat {@link #getVirusScannedAt()}. */
    private Date virusScannedAt;
    /** Hasil validasi tanda tangan digital berkas ini. Lihat {@link #getSignatureValid()}. */
    private Boolean signatureValid;
    /** Nomor versi berkas ini. Lihat {@link #getFileVersion()}. */
    private Long fileVersion;
    /** Tahapan naskah pada alur review jurnal ala OJS (mis. submission/review/copyediting/production). Lihat {@link #getJournalStage()}. */
    private String journalStage;
    /** Jenis/genre naskah ala OJS (mis. manuscript, cover letter). Lihat {@link #getJournalGenre()}. */
    private String journalGenre;
    /** Nomor putaran review naskah ini. Lihat {@link #getReviewRound()}. */
    private Integer reviewRound;
    /** Status penyimpanan konten fisik berkas ini. Lihat {@link #getStorageState()}. */
    private String storageState;
    /** Referensi ke konten terkait (mis. baris penyimpanan biner di tabel lain). Lihat {@link #getContentRef()}. */
    private Long contentRef;

    // Audit & Default Fields
    /** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
    private String oleh;
    /** Id pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
    private String olehId;
    /** Stempel waktu perubahan terakhir; diinisialisasi ke waktu pembuatan object. Lihat {@link #getTanggal_dirubah()}. */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    /** Penanda aktif/nonaktif (soft-delete) baris ini. Lihat {@link #getAktif()}. */
    private Boolean aktif;

    /** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
    public RepoBitstream() {}

    /**
     * Mengembalikan primary key baris ini. {@code insertable = false} karena nilai dihasilkan
     * database ({@code IDENTITY}).
     * @return primary key, {@code null} untuk entity yang belum tersimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return this.id; }
    /** Menyetel primary key. Tanpa validasi. @param id primary key baru */
    public void setId(Long id) { this.id = id; }

    /**
     * Id {@link RepoItem} yang memiliki berkas ini. Relasi disimpan sebagai id polos
     * ({@code Long}), BUKAN {@code @ManyToOne} — untuk memuat objek item, pemanggil harus
     * melakukan query eksplisit sendiri. Wajib diisi ({@code nullable = false}).
     * @return id item pemilik, tidak pernah {@code null} pada baris tersimpan
     */
    @Column(name = "item_id", nullable = false)
    public Long getItemId() { return this.itemId; }
    /** Menyetel id item pemilik. @param itemId id {@link RepoItem} baru */
    public void setItemId(Long itemId) { this.itemId = itemId; }

    /**
     * Nama berkas asli (nama tampilan, bukan nama fisik penyimpanan — lihat {@link #getPathSistem()}
     * untuk itu). Wajib diisi ({@code nullable = false}). Berbeda dari kebanyakan field
     * {@code String} lain di kelas ini, getter ini tidak melakukan normalisasi
     * null-ke-string-kosong maupun pemangkasan spasi.
     * @return nama berkas apa adanya
     */
    @Column(name = "nama_file", nullable = false, length = 255)
    public String getNamaFile() { return this.namaFile; }
    /** Menyetel nama berkas. @param namaFile nama baru */
    public void setNamaFile(String namaFile) { this.namaFile = namaFile; }

    /**
     * Tipe MIME berkas (mis. {@code application/pdf}), dipakai untuk menentukan cara penyajian
     * berkas ke klien (header {@code Content-Type}).
     * @return tipe MIME apa adanya, boleh {@code null}
     */
    @Column(name = "mime_type", length = 100)
    public String getMimeType() { return this.mimeType; }
    /** Menyetel tipe MIME. @param mimeType tipe MIME baru */
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    /**
     * Path fisik penyimpanan berkas ini di server (mis. lokasi pada disk/direktori penyimpanan
     * repositori). Wajib diisi ({@code nullable = false}). <b>Sensitif</b>: nilai ini adalah
     * detail infrastruktur internal server dan sebaiknya tidak pernah diekspos apa adanya ke
     * klien/API publik — pola yang benar (lihat {@code RepositoryPublicService.toBitstream(...)})
     * adalah mengembalikan hanya {@link #getId()}/{@link #getNamaFile()}/{@link #getMimeType()}
     * ke klien dan menyajikan konten lewat endpoint unduhan yang menerapkan gerbang otorisasi.
     * @return path fisik berkas apa adanya
     */
    @Column(name = "path_sistem", nullable = false, length = 500)
    public String getPathSistem() { return this.pathSistem; }
    /** Menyetel path fisik penyimpanan. @param pathSistem path baru */
    public void setPathSistem(String pathSistem) { this.pathSistem = pathSistem; }

    /**
     * Ukuran berkas dalam byte, dicatat saat unggah untuk ditampilkan ke pengguna tanpa perlu
     * mengakses berkas fisik.
     * @return ukuran berkas dalam byte, boleh {@code null} bila belum tercatat
     */
    @Column(name = "ukuran_byte")
    public Long getUkuranByte() { return this.ukuranByte; }
    /** Menyetel ukuran berkas. @param ukuranByte ukuran baru dalam byte */
    public void setUkuranByte(Long ukuranByte) { this.ukuranByte = ukuranByte; }

    /**
     * Checksum (mis. SHA-256/MD5) isi berkas, dipakai memverifikasi integritas berkas belum
     * berubah sejak diunggah (deteksi korupsi penyimpanan atau modifikasi tak sah).
     * @return checksum berkas, boleh {@code null} bila belum dihitung
     */
    @Column(name = "checksum", length = 64)
    public String getChecksum() { return this.checksum; }
    /** Menyetel checksum berkas. @param checksum checksum baru */
    public void setChecksum(String checksum) { this.checksum = checksum; }

    /**
     * Nama bundle ala DSpace yang menaungi berkas ini dalam satu item — pola pengelompokan
     * berkas standar DSpace, mis. {@code "ORIGINAL"} (berkas utama/terbitan), {@code "LICENSE"}
     * (berkas lisensi), {@code "THUMBNAIL"} (gambar pratinjau). Default {@code "ORIGINAL"} bila
     * kolom {@code null}.
     * @return nama bundle, default {@code "ORIGINAL"}
     */
    @Column(name = "bundle_name", length = 80)
    public String getBundleName() { return bundleName == null ? "ORIGINAL" : bundleName.trim(); }
    /** Menyetel nama bundle. @param bundleName nama bundle baru */
    public void setBundleName(String bundleName) { this.bundleName = bundleName; }

    /**
     * Deskripsi berkas ini (mis. keterangan singkat isi berkas untuk pengguna).
     * @return deskripsi, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "description", columnDefinition = "TEXT")
    public String getDescription() { return description == null ? "" : description.trim(); }
    /** Menyetel deskripsi berkas. @param description deskripsi baru */
    public void setDescription(String description) { this.description = description; }

    /**
     * Kebijakan akses berkas ini secara individual (mis. {@code "OPEN_ACCESS"},
     * {@code "RESTRICTED"}) — dapat berbeda dari {@link RepoItem#getAccessPolicy()} milik item
     * induknya; layer publik ({@code RepositoryPublicService.canDownload(RepoItem, RepoBitstream)})
     * mensyaratkan KEDUANYA bernilai {@code "OPEN_ACCESS"} (atau kosong pada sisi berkas) sebelum
     * mengizinkan unduhan — satu berkas dengan kebijakan lebih ketat dari itemnya tetap
     * terlindungi meski item induknya terbuka publik. Default {@code "OPEN_ACCESS"} bila kolom
     * {@code null}.
     * @return kebijakan akses berkas, default {@code "OPEN_ACCESS"}
     */
    @Column(name = "access_policy", length = 40)
    public String getAccessPolicy() { return accessPolicy == null ? "OPEN_ACCESS" : accessPolicy.trim(); }
    /** Menyetel kebijakan akses berkas. @param accessPolicy kebijakan baru */
    public void setAccessPolicy(String accessPolicy) { this.accessPolicy = accessPolicy; }

    /**
     * Nama kelas entitas AIS asal berkas ini diunggah (referensi polimorfik opsional, bukan
     * {@code @ManyToOne}), dipasangkan dengan {@link #getSourceId()}.
     * @return nama kelas entitas sumber, dipangkas spasi; {@code null} bila tidak ada
     */
    @Column(name = "source_class", length = 255)
    public String getSourceClass() { return sourceClass == null ? null : sourceClass.trim(); }
    /** Menyetel nama kelas entitas sumber. @param sourceClass nama kelas baru */
    public void setSourceClass(String sourceClass) { this.sourceClass = sourceClass; }

    /**
     * Id baris pada entitas sumber ({@link #getSourceClass()}) yang menjadi asal berkas ini.
     * @return id baris sumber, atau {@code null} bila tidak ada
     */
    @Column(name = "source_id")
    public Long getSourceId() { return sourceId; }
    /** Menyetel id baris sumber. @param sourceId id baru */
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    /**
     * UUID padanan berkas ini pada instalasi DSpace eksternal (bitstream UUID DSpace), bila
     * berkas ini bermigrasi dari/ke sana.
     * @return UUID DSpace, tidak pernah {@code null} (string kosong bila tidak ada)
     */
    @Column(name = "dspace_uuid", length = 80)
    public String getDspaceUuid() { return dspaceUuid == null ? "" : dspaceUuid.trim(); }
    /** Menyetel UUID DSpace padanan. @param dspaceUuid UUID baru */
    public void setDspaceUuid(String dspaceUuid) { this.dspaceUuid = dspaceUuid; }

    /**
     * Menandakan berkas ini adalah berkas utama/representatif item (mis. dipakai sebagai berkas
     * yang ditampilkan langsung pada pratinjau, saat item memiliki banyak berkas dalam bundle
     * yang sama). Tidak ada pemaksaan "hanya satu primary per item" di level entitas ini — bila
     * aturan itu diperlukan, penegakannya menjadi tanggung jawab layer service pemanggil.
     * @return {@code true} bila berkas ini ditandai primary, default {@code false}
     */
    @Column(name = "primary_file")
    public Boolean getPrimaryFile() { return primaryFile == null ? false : primaryFile; }
    /** Menyetel penanda primary file. @param primaryFile nilai baru */
    public void setPrimaryFile(Boolean primaryFile) { this.primaryFile = primaryFile; }

    /**
     * Menandakan berkas ini sudah dikirim ke layanan deteksi plagiarisme Turnitin.
     * @return {@code true} bila sudah dikirim, default {@code false}
     */
    @Column(name = "turnitin_submitted")
    public Boolean getTurnitinSubmitted() { return turnitinSubmitted == null ? false : turnitinSubmitted; }
    /** Menyetel penanda dikirim ke Turnitin. @param turnitinSubmitted nilai baru */
    public void setTurnitinSubmitted(Boolean turnitinSubmitted) { this.turnitinSubmitted = turnitinSubmitted; }

    /**
     * Waktu berkas ini dikirim ke Turnitin.
     * @return waktu pengiriman, boleh {@code null} bila belum pernah dikirim
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "turnitin_submitted_at")
    public Date getTurnitinSubmittedAt() { return turnitinSubmittedAt; }
    /** Menyetel waktu pengiriman ke Turnitin. @param turnitinSubmittedAt waktu baru */
    public void setTurnitinSubmittedAt(Date turnitinSubmittedAt) { this.turnitinSubmittedAt = turnitinSubmittedAt; }

    /**
     * Status pemindaian virus atas isi berkas ini (mis. {@code "PENDING"}, {@code "CLEAN"},
     * {@code "INFECTED"}, {@code "FAILED"}) — nilai bebas teks, tidak dibatasi enum di level
     * entitas. Default {@code "PENDING"} bila kolom {@code null} (komentar asli pada kode
     * menjelaskan kolom ini dibuat nullable agar {@code hbm2ddl} dapat menambah kolom ke tabel
     * lama yang sudah berisi data tanpa gagal). <b>Perhatian keamanan bagi pemanggil:</b> entitas
     * ini tidak menggerbangi apa pun berdasarkan status ini — bila alur unduhan publik harus
     * menolak berkas yang belum {@code "CLEAN"}, pengecekan tersebut wajib dilakukan eksplisit
     * oleh layer service, karena default {@code "PENDING"} (bukan {@code "CLEAN"}) berarti berkas
     * yang lupa dipindai akan tetap dianggap "belum diperiksa", bukan otomatis aman.
     * @return status pemindaian virus, default {@code "PENDING"}
     */
    @Column(name = "virus_scan_status", length = 30)
    public String getVirusScanStatus() { return virusScanStatus == null ? "PENDING" : virusScanStatus.trim(); }
    /** Menyetel status pemindaian virus. @param virusScanStatus status baru */
    public void setVirusScanStatus(String virusScanStatus) { this.virusScanStatus = virusScanStatus; }

    /**
     * Waktu pemindaian virus terakhir dijalankan atas berkas ini.
     * @return waktu pemindaian terakhir, boleh {@code null} bila belum pernah dipindai
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "virus_scanned_at")
    public Date getVirusScannedAt() { return virusScannedAt; }
    /** Menyetel waktu pemindaian virus. @param virusScannedAt waktu baru */
    public void setVirusScannedAt(Date virusScannedAt) { this.virusScannedAt = virusScannedAt; }

    /**
     * Hasil validasi tanda tangan digital berkas ini (mis. untuk dokumen resmi yang wajib
     * ditandatangani secara elektronik). Default {@code FALSE} bila kolom {@code null} — beda
     * dengan kebanyakan flag {@code Boolean} lain di kelas ini yang juga default {@code false},
     * di sini eksplisit dipilih {@code Boolean.FALSE} (bukan literal {@code false}) sebagai
     * penegasan bahwa berkas yang belum diverifikasi dianggap TIDAK valid tanda tangannya
     * (fail-closed), bukan diasumsikan valid.
     * @return {@code true} hanya bila tanda tangan sudah diverifikasi valid
     */
    @Column(name = "signature_valid")
    public Boolean getSignatureValid() { return signatureValid == null ? Boolean.FALSE : signatureValid; }
    /** Menyetel hasil validasi tanda tangan. @param signatureValid nilai baru */
    public void setSignatureValid(Boolean signatureValid) { this.signatureValid = signatureValid; }

    /**
     * Nomor versi berkas ini (mis. saat pengunggah mengganti berkas dengan revisi baru tanpa
     * mengubah metadata item). Default {@code 1L} bila kolom {@code null}.
     * @return nomor versi berkas, default {@code 1L}
     */
    @Column(name = "file_version")
    public Long getFileVersion() { return fileVersion == null ? Long.valueOf(1L) : fileVersion; }
    /** Menyetel nomor versi berkas. @param fileVersion nomor baru */
    public void setFileVersion(Long fileVersion) { this.fileVersion = fileVersion; }

    /**
     * Tahapan naskah pada alur review jurnal ala OJS (Open Journal Systems) tempat berkas ini
     * berada (mis. submission, review, copyediting, production) — hanya relevan bila item induk
     * berasal dari modul jurnal.
     * @return tahapan naskah OJS, boleh {@code null}
     */
    @Column(name="journal_stage",length=60) public String getJournalStage(){return journalStage;}
    /** Menyetel tahapan naskah OJS. @param v tahapan baru */
    public void setJournalStage(String v){journalStage=v;}
    /**
     * Jenis/genre naskah ala OJS (mis. manuscript, cover letter, review form) yang menjelaskan
     * peran berkas ini dalam submisi jurnal.
     * @return genre naskah OJS, boleh {@code null}
     */
    @Column(name="journal_genre",length=80) public String getJournalGenre(){return journalGenre;}
    /** Menyetel genre naskah OJS. @param v genre baru */
    public void setJournalGenre(String v){journalGenre=v;}
    /**
     * Nomor putaran (round) review naskah ini pada alur jurnal — bertambah setiap kali naskah
     * dikembalikan untuk revisi dan direview ulang.
     * @return nomor putaran review, boleh {@code null}
     */
    @Column(name="review_round") public Integer getReviewRound(){return reviewRound;}
    /** Menyetel nomor putaran review. @param v nomor baru */
    public void setReviewRound(Integer v){reviewRound=v;}
    /**
     * Status penyimpanan konten fisik berkas ini (mis. {@code "PENDING_CONTENT"} bila baris
     * metadata sudah dibuat tetapi berkas fisiknya belum selesai diunggah/disalin ke penyimpanan
     * — pola umum pada alur unggah asinkron/chunked). Default {@code "PENDING_CONTENT"} bila
     * kolom {@code null}, sehingga pemanggil yang menyajikan berkas ke pengguna sebaiknya
     * memeriksa status ini dahulu sebelum mengasumsikan berkas fisik sudah tersedia di
     * {@link #getPathSistem()}.
     * @return status penyimpanan konten, default {@code "PENDING_CONTENT"}
     */
    @Column(name="storage_state",length=40) public String getStorageState(){return storageState==null?"PENDING_CONTENT":storageState;}
    /** Menyetel status penyimpanan konten. @param v status baru */
    public void setStorageState(String v){storageState=v;}
    /**
     * Referensi ke konten terkait (mis. baris penyimpanan biner pada tabel lain, dipakai oleh
     * mekanisme penyimpanan konten yang belum tentu berupa path filesystem langsung).
     * @return id referensi konten, boleh {@code null}
     */
    @Column(name="content_ref") public Long getContentRef(){return contentRef;}
    /** Menyetel referensi konten. @param v referensi baru */
    public void setContentRef(Long v){contentRef=v;}

    // --- Audit Methods ---
    /**
     * Mengembalikan id pengguna yang terakhir mengubah baris ini. Field ini didefinisikan ulang
     * sebagai field privat khusus kelas ini (bukan mewarisi field {@code olehId} milik
     * {@link GeneralValueObject}) karena kolom {@code oleh_id} yang dipetakan lewat anotasi
     * {@code @Column} harus dideklarasikan pada kelas yang memiliki getter fisiknya.
     * @return id pengguna pengubah terakhir, boleh {@code null}
     */
    public String getOlehId() { return olehId; }
    /**
     * Menyetel id pengguna pengubah terakhir, dengan validasi non-trivial: nilai {@code null}
     * atau kosong/spasi diabaikan diam-diam agar jejak audit yang sudah terisi tidak terhapus.
     * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
     */
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) return;
        this.olehId = olehId;
    }

    /**
     * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
     * @return nama pengguna pengubah terakhir, boleh {@code null}
     */
    public String getOleh() { return oleh; }
    /**
     * Menyetel nama pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
     * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam.
     * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
     */
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) return;
        this.oleh = oleh;
    }

    /**
     * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat
     * sebelum setiap {@code UPDATE}, memperbarui {@link #getTanggal_dirubah()} lewat
     * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    /**
     * Mengembalikan stempel waktu perubahan terakhir baris ini, diperbarui otomatis lewat
     * {@link #onUpdate()} pada setiap update.
     * @return waktu perubahan terakhir
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() { return tanggal_dirubah; }
    /** Menyetel stempel waktu perubahan terakhir secara manual. Tanpa validasi. @param tanggal_dirubah waktu baru */
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

    /**
     * Menandakan baris ini aktif (belum soft-delete).
     * @return {@code true} bila baris aktif, default {@code true} bila kolom {@code null}
     */
    public Boolean getAktif() { return aktif == null ? true : aktif; }
    /** Menyetel penanda aktif. @param aktif nilai aktif baru */
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    /**
     * Representasi teks ringkas berkas ini untuk debugging/log: {@code "<id>-<namaFile>"}.
     * Meng-override {@link GeneralValueObject#toString()} yang berformat {@code "kode - nama"}.
     * @return string berformat {@code "<id>-<namaFile>"}
     */
    public String toString() {
        return id + "-" + namaFile;
    }
}
