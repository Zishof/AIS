package ais.database.model.file;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.sql.Blob;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Konten biner jurnal pada database streaming, tanpa relasi ORM lintas SessionFactory.
 *
 * <h2>Kedudukan dalam paket ini</h2>
 * <p>Entitas ini sengaja <b>berdiri sendiri</b> dan bukan turunan {@link FileFotoLain}
 * seperti hampir seluruh entitas berkas lain di paket {@code ais.database.model.file}.
 * Perbedaannya bukan kebetulan, dan ada baiknya disadari sebelum menambahkan kode yang
 * menyamakan keduanya:</p>
 * <ul>
 *   <li>{@code FileFotoLain} dan keturunannya melayani lampiran umum dengan pencarian
 *       berbasis pasangan (acuan pemilik, {@code jenis}), penghapusan lunak lewat nilai
 *       sentinel, dan lapis cache metadata di berkas. Entitas ini tidak memakai satu pun
 *       mekanisme itu.</li>
 *   <li>Entitas ini melayani berkas unggahan modul jurnal ilmiah, dengan alur unggah
 *       yang jauh lebih ketat: berkas melewati rangkaian keadaan yang eksplisit
 *       ({@code storageState}), dicocokkan ukuran dan sidik jarinya, dan diberi kunci
 *       idempotensi supaya percobaan unggah yang sama tidak menghasilkan dua baris.</li>
 * </ul>
 * <p>Satu-satunya pemakainya adalah {@code ais.action.master.jurnal.JurnalFileService},
 * dan pemetaannya didaftarkan pada {@code hibernate.streaming.cfg.xml} &mdash; bukan pada
 * konfigurasi Hibernate utama. Entitas ini karena itu hidup pada {@code SessionFactory}
 * streaming yang terpisah.</p>
 *
 * <h2>Mengapa tidak ada relasi ORM</h2>
 * <p>Kaitan ke metadata berkas ({@code RepoBitstream}) hanya berupa angka pada
 * {@code repoBitstreamId}, bukan relasi {@code @ManyToOne}. Ini <b>keharusan teknis,
 * bukan kelalaian pemodelan</b>: entitas ini berada pada {@code SessionFactory} yang
 * berbeda dari entitas metadata, dan Hibernate tidak dapat membentuk relasi yang
 * melintasi dua {@code SessionFactory}. Hal yang sama berlaku bagi {@code createdBy} dan
 * {@code updatedBy}, yang menyimpan userid sebagai teks alih-alih menunjuk entitas
 * pengguna. Jangan "memperbaiki" salah satunya menjadi relasi; yang akan terjadi adalah
 * kegagalan pemetaan saat aplikasi dijalankan.</p>
 *
 * <h2>Dua kekangan unik dan perannya</h2>
 * <ul>
 *   <li>{@code uk_lampiran_jurnal_idempotency} pada {@code idempotency_key} &mdash;
 *       menjadikan basis data sebagai penjaga terakhir terhadap unggahan ganda. Kunci
 *       ini dibentuk {@code JurnalFileService} sebagai
 *       {@code <sumber> + ":REPO_BITSTREAM:" + <id metadata>}, sehingga percobaan ulang
 *       atas berkas yang sama ditolak basis data, bukan sekadar oleh pemeriksaan di
 *       kode yang bisa saja kalah balapan.</li>
 *   <li>{@code uk_lampiran_jurnal_bitstream} pada {@code repo_bitstream_id} &mdash;
 *       memastikan satu metadata berkas hanya punya satu isi. Perhatikan bahwa
 *       {@code JurnalFileService} tetap menulis kueri
 *       {@code from LampiranJurnal where repoBitstreamId=:r order by id desc} dan
 *       mengambil hasil pertama; bentuk itu adalah sisa kehati-hatian dari sebelum
 *       kekangan ini ada, dan dengan kekangan tersebut hasilnya paling banyak satu
 *       baris.</li>
 * </ul>
 *
 * <p>{@code dynamicInsert}/{@code dynamicUpdate} diaktifkan supaya pernyataan SQL hanya
 * memuat kolom yang benar-benar terisi atau berubah &mdash; berguna pada tabel yang salah
 * satu kolomnya adalah objek biner besar, karena kolom itu tidak ikut ditulis ulang pada
 * pembaruan yang hanya menyentuh keadaan.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "public", name = "lampiran_jurnal", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lampiran_jurnal_idempotency", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_lampiran_jurnal_bitstream", columnNames = "repo_bitstream_id") })
public class LampiranJurnal implements Serializable {
    /** Versi serialisasi; tetap {@code 1} selama bentuk field tidak berubah. */
    private static final long serialVersionUID = 1L;

    /**
     * Primary key dan empat nilai numerik pendamping.
     *
     * <p>{@code id} dibangkitkan basis data secara {@code IDENTITY} sehingga nilainya
     * berurutan; {@code repoBitstreamId} menghubungkan baris ini ke metadata berkasnya
     * tanpa relasi ORM; {@code declaredSize} adalah ukuran yang dilaporkan pengunggah
     * sedangkan {@code actualSize} ukuran yang benar-benar terbaca saat isi disimpan;
     * {@code fileVersion} disalin dari metadata dan menandai versi keberapa berkas ini.</p>
     */
    private Long id, repoBitstreamId, declaredSize, actualSize, fileVersion;

    /**
     * Isi berkas sebagai objek biner besar.
     *
     * <p>Dipetakan ke kolom {@code file_content} yang pada PostgreSQL berupa Large Object.
     * Membacanya menuntut koneksi yang sedang berada di dalam transaksi; pembacaan di luar
     * transaksi gagal dengan pesan tentang mode auto-commit. Karena itu pengisian dan
     * pembacaannya selalu melewati {@code JurnalFileService} yang mengurus transaksinya.</p>
     */
    private Blob content;

    /**
     * Lima nilai teks yang menggambarkan berkas dan tahapannya.
     *
     * <p>{@code originalFileName} adalah nama berkas yang sudah dibersihkan pemanggil
     * (lihat {@code safeName(...)} pada {@code JurnalFileService}), bukan nama mentah dari
     * klien. {@code declaredMimeType} berasal dari klien sedangkan
     * {@code detectedMimeType} berasal dari pembacaan beberapa bita pertama isi berkas;
     * keduanya sengaja disimpan terpisah supaya ketidakcocokan antara yang diakui dan yang
     * sebenarnya dapat diperiksa, bukan tertimpa diam-diam. {@code checksumSha256}
     * menyimpan sidik jari isi berkas, dan {@code journalStage} menandai tahap penerbitan
     * tempat berkas ini diunggah.</p>
     */
    private String originalFileName, declaredMimeType, detectedMimeType, checksumSha256, journalStage;

    /**
     * Enam nilai teks yang menyimpan keadaan berkas serta jejak audit penggunanya.
     *
     * <p>Tiga yang pertama adalah mesin keadaan: {@code storageState} melacak perjalanan
     * isi berkas, {@code scanState} hasil pemindaian keamanan, dan
     * {@code quarantineState} status karantinanya. {@code idempotencyKey} adalah kunci
     * penangkal unggahan ganda yang dijaga kekangan unik.</p>
     * <p>{@code createdBy} dan {@code updatedBy} menyimpan userid sebagai teks &mdash;
     * <b>field audit bayangan</b>, sengaja bukan relasi ke entitas pengguna karena alasan
     * lintas {@code SessionFactory} yang dijelaskan pada Javadoc kelas. Nilainya disalin
     * pada saat kejadian sehingga tetap terbaca meski pengguna yang bersangkutan kelak
     * dihapus.</p>
     */
    private String storageState, scanState, quarantineState, idempotencyKey, createdBy, updatedBy;

    /**
     * Waktu pembuatan dan pembaruan terakhir, keduanya berisi waktu saat objek dibentuk
     * sebagai nilai awal.
     *
     * <p>Nilai awal {@code new Date()} dipasang pada deklarasi supaya baris tidak pernah
     * tersimpan dengan kolom waktu kosong &mdash; kedua kolomnya dideklarasikan
     * {@code nullable = false}. Pemanggil tetap menimpanya dengan waktu yang seragam pada
     * saat penyimpanan.</p>
     * <p>Perhatikan bahwa nilai awal itu diambil saat objek Java dibentuk, bukan saat
     * baris benar-benar ditulis; keduanya dapat berbeda bila objek sempat menunggu.
     * Perhatikan pula bahwa kedua field ini <b>tidak diperbarui otomatis</b> oleh
     * lifecycle Hibernate &mdash; setiap perubahan keadaan wajib menyetel
     * {@code updatedAt} sendiri, dan {@code JurnalFileService} memang melakukannya.</p>
     */
    private Date createdAt = new Date(), updatedAt = new Date();

    /**
     * Konstruktor tanpa argumen yang diwajibkan Hibernate untuk membentuk instance saat
     * memuat baris dari basis data.
     *
     * <p>Sengaja dibiarkan kosong. Seluruh pengisian nilai dilakukan
     * {@code JurnalFileService.newLampiran(...)}, yang menjadi satu-satunya tempat
     * kombinasi keadaan awal ditentukan; membentuk objek ini secara langsung di tempat
     * lain berarti melewatkan penetapan keadaan tersebut.</p>
     */
    public LampiranJurnal() {}

    /**
     * Primary key baris ini, dibangkitkan basis data.
     *
     * <p>Kolomnya {@code insertable = false} karena nilainya berasal dari urutan
     * {@code IDENTITY} milik basis data, bukan dari kode. Nilainya berurutan dan mudah
     * ditebak, jadi jangan memperlakukannya sebagai penanda rahasia.</p>
     *
     * @return primary key, atau {@code null} bila baris belum tersimpan
     */
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="id",insertable=false,unique=true,nullable=false) public Long getId(){return id;}

    /**
     * Menyetel primary key; disediakan untuk Hibernate.
     *
     * <p>Tidak seharusnya dipanggil kode aplikasi: kolomnya tidak ikut ditulis saat
     * {@code INSERT}, sehingga nilai yang dipasang sendiri akan diabaikan pada baris baru
     * dan hanya membingungkan pada baris yang sudah ada.</p>
     *
     * @param v primary key
     */
    public void setId(Long v){id=v;}

    /**
     * Penghubung ke metadata berkas ({@code RepoBitstream}) yang memiliki isi ini.
     *
     * <p>Angka biasa, bukan relasi ORM &mdash; lihat Javadoc kelas untuk alasannya.
     * Dijaga kekangan unik {@code uk_lampiran_jurnal_bitstream} sehingga satu metadata
     * hanya dapat memiliki satu baris isi.</p>
     *
     * @return id metadata berkas; tidak pernah {@code null} pada baris tersimpan
     */
    @Column(name="repo_bitstream_id",nullable=false) public Long getRepoBitstreamId(){return repoBitstreamId;}

    /**
     * Menyetel penghubung ke metadata berkas.
     *
     * <p>Wajib diisi sebelum penyimpanan; kolomnya {@code nullable = false}. Nilai yang
     * bertabrakan dengan baris lain akan ditolak basis data lewat kekangan uniknya, dan
     * itu memang penjagaan yang diinginkan &mdash; jangan menambal dengan pemeriksaan di
     * kode yang bisa kalah balapan.</p>
     *
     * @param v id metadata berkas
     */
    public void setRepoBitstreamId(Long v){repoBitstreamId=v;}

    /**
     * Isi berkas sebagai objek biner besar.
     *
     * <p><b>Prasyarat pembacaan:</b> aliran dari {@code Blob} ini hanya sah dibaca ketika
     * koneksinya sedang berada di dalam transaksi. Di luar itu pembacaan gagal dengan
     * keluhan tentang mode auto-commit. Pemanggil sebaiknya melewati
     * {@code JurnalFileService} alih-alih membaca langsung dari sini.</p>
     *
     * @return isi berkas, atau {@code null} bila baris belum diisi
     */
    @Column(name="file_content",nullable=false) public Blob getContent(){return content;}

    /**
     * Menyetel isi berkas.
     *
     * <p>Kolomnya {@code nullable = false}, sehingga baris tidak dapat disimpan sebelum
     * isinya ada. Inilah yang membuat alur unggah memakai keadaan
     * {@code PENDING_CONTENT} lebih dahulu: metadata dan isi tidak selalu tiba bersamaan.</p>
     *
     * @param v isi berkas
     */
    public void setContent(Blob v){content=v;}

    /**
     * Nama berkas asli sebagaimana disimpan, sesudah dibersihkan pemanggil.
     *
     * <p>Nilainya <b>bukan</b> nama mentah dari klien: {@code JurnalFileService}
     * melewatkannya melalui {@code safeName(...)} lebih dahulu. Pembersihan itu tidak
     * dilakukan di kelas ini, jadi kode lain yang mengisi kolom ini secara langsung tidak
     * mendapat perlindungan yang sama &mdash; masukkan lewat service tersebut.</p>
     *
     * @return nama berkas, paling panjang 255 karakter
     */
    @Column(name="original_file_name",nullable=false,length=255) public String getOriginalFileName(){return originalFileName;}

    /**
     * Menyetel nama berkas asli.
     *
     * <p>Setter murni tanpa pembersihan maupun pemeriksaan panjang; nilai yang melebihi
     * 255 karakter baru ditolak basis data pada saat penyimpanan.</p>
     *
     * @param v nama berkas yang sudah dibersihkan pemanggil
     */
    public void setOriginalFileName(String v){originalFileName=v;}

    /**
     * Tipe MIME yang <b>diakui</b> pengunggah, apa adanya dari klien.
     *
     * <p>Disimpan terpisah dari {@link #getDetectedMimeType()} dengan sengaja. Nilai ini
     * berasal dari pihak luar dan karenanya tidak boleh dipercaya sendirian; ia hanya
     * berguna bila dibandingkan dengan tipe yang benar-benar terdeteksi dari isi berkas.</p>
     *
     * @return tipe MIME menurut klien, paling panjang 100 karakter
     */
    @Column(name="declared_mime_type",nullable=false,length=100) public String getDeclaredMimeType(){return declaredMimeType;}

    /**
     * Menyetel tipe MIME yang diakui pengunggah.
     *
     * @param v tipe MIME menurut klien
     */
    public void setDeclaredMimeType(String v){declaredMimeType=v;}

    /**
     * Tipe MIME yang benar-benar <b>terdeteksi</b> dari beberapa bita pertama isi berkas.
     *
     * <p>Diisi {@code JurnalFileService.sniffMime(...)} yang mengenali PDF, PNG, JPEG, dan
     * TIFF dari pola bita awalnya. Untuk berkas berformat kemasan zip, pendeteksian tidak
     * dapat memastikan isinya sehingga nilai yang diakui klien dipakai apa adanya &mdash;
     * artinya pada golongan berkas itu kolom ini tidak menambah keyakinan apa pun.</p>
     *
     * @return tipe MIME menurut isi berkas, paling panjang 100 karakter
     */
    @Column(name="detected_mime_type",nullable=false,length=100) public String getDetectedMimeType(){return detectedMimeType;}

    /**
     * Menyetel tipe MIME hasil pendeteksian isi berkas.
     *
     * @param v tipe MIME menurut isi berkas
     */
    public void setDetectedMimeType(String v){detectedMimeType=v;}

    /**
     * Ukuran berkas yang <b>dilaporkan</b> pengunggah, dalam bita.
     *
     * <p>Sama seperti tipe MIME, nilai ini berasal dari pihak luar. Gunanya adalah untuk
     * dibandingkan dengan {@link #getActualSize()} setelah isi berkas benar-benar
     * tersimpan; selisih di antara keduanya menandakan unggahan yang terpotong atau
     * laporan yang tidak jujur.</p>
     *
     * @return ukuran menurut pengunggah, dalam bita
     */
    @Column(name="declared_size",nullable=false) public Long getDeclaredSize(){return declaredSize;}

    /**
     * Menyetel ukuran berkas yang dilaporkan pengunggah.
     *
     * <p>Wajib terisi sebelum penyimpanan; kolomnya {@code nullable = false}.</p>
     *
     * @param v ukuran menurut pengunggah, dalam bita
     */
    public void setDeclaredSize(Long v){declaredSize=v;}

    /**
     * Ukuran berkas yang <b>sesungguhnya</b> terbaca saat isi disimpan, dalam bita.
     *
     * <p>Berbeda dari saudaranya, kolom ini boleh {@code null}: nilainya belum diketahui
     * pada saat baris pertama kali dibuat, dan baru terisi setelah isi berkas mengalir
     * masuk. Nilai {@code null} karena itu berarti "isi belum tersimpan", bukan "ukurannya
     * nol".</p>
     *
     * @return ukuran sebenarnya dalam bita, atau {@code null} bila isi belum tersimpan
     */
    @Column(name="actual_size") public Long getActualSize(){return actualSize;}

    /**
     * Menyetel ukuran berkas yang sesungguhnya terbaca.
     *
     * @param v ukuran sebenarnya dalam bita
     */
    public void setActualSize(Long v){actualSize=v;}

    /**
     * Sidik jari SHA-256 atas isi berkas, dalam bentuk heksadesimal.
     *
     * <p>Panjang kolomnya 64 karakter, tepat sepanjang SHA-256 heksadesimal. Boleh
     * {@code null} karena baru dapat dihitung setelah seluruh isi terbaca. Gunanya untuk
     * memastikan isi yang tersimpan sama dengan yang dikirim, dan untuk mengenali dua
     * unggahan berisi berkas yang identik.</p>
     *
     * @return sidik jari heksadesimal, atau {@code null} bila belum dihitung
     */
    @Column(name="checksum_sha256",length=64) public String getChecksumSha256(){return checksumSha256;}

    /**
     * Menyetel sidik jari SHA-256 isi berkas.
     *
     * <p>Setter murni: bentuk maupun panjang nilainya tidak diperiksa di sini.</p>
     *
     * @param v sidik jari heksadesimal
     */
    public void setChecksumSha256(String v){checksumSha256=v;}

    /**
     * Tahap penerbitan tempat berkas ini diunggah.
     *
     * <p>Berupa teks bebas sepanjang paling banyak 60 karakter, bukan enumerasi &mdash;
     * nilainya diteruskan pemanggil apa adanya. Karena tidak ada daftar nilai yang sah di
     * kelas ini, kekeliruan penulisan tidak akan tertangkap sampai ada yang menyaring
     * berdasarkan kolom ini dan menemukan hasilnya kosong.</p>
     *
     * @return penanda tahap penerbitan
     */
    @Column(name="journal_stage",nullable=false,length=60) public String getJournalStage(){return journalStage;}

    /**
     * Menyetel tahap penerbitan berkas ini.
     *
     * @param v penanda tahap penerbitan
     */
    public void setJournalStage(String v){journalStage=v;}

    /**
     * Nomor versi berkas, disalin dari metadata pada saat baris dibuat.
     *
     * <p>Nilainya <b>tidak</b> dihitung entitas ini melainkan diambil dari
     * {@code RepoBitstream.getFileVersion()}. Karena disalin, ia tetap menunjukkan versi
     * pada saat pengunggahan meski metadata sumbernya kelak berubah.</p>
     *
     * @return nomor versi berkas
     */
    @Column(name="file_version",nullable=false) public Long getFileVersion(){return fileVersion;}

    /**
     * Menyetel nomor versi berkas.
     *
     * @param v nomor versi berkas
     */
    public void setFileVersion(Long v){fileVersion=v;}

    /**
     * Keadaan penyimpanan isi berkas &mdash; mesin keadaan utama entitas ini.
     *
     * <p>Nilai yang muncul pada {@code JurnalFileService}, berurutan:
     * {@code "PENDING_CONTENT"} (baris dibuat, isi belum masuk), {@code "VERIFIED"} (isi
     * masuk dan lolos pemeriksaan), {@code "LINKED"} (tertaut ke metadata), lalu
     * {@code "AVAILABLE"} (siap disajikan). Kegagalan menandai metadatanya dengan
     * {@code "FAILED"}.</p>
     * <p>Perpindahan ke {@code "AVAILABLE"} dijaga: hanya baris yang sedang berkeadaan
     * {@code "VERIFIED"} atau {@code "LINKED"} yang boleh naik, selainnya ditolak dengan
     * {@code IllegalStateException}. Penjagaan itu berada di service, bukan di kelas ini
     * &mdash; setter di bawah menerima nilai apa pun.</p>
     *
     * @return keadaan penyimpanan saat ini
     */
    @Column(name="storage_state",nullable=false,length=40) public String getStorageState(){return storageState;}

    /**
     * Menyetel keadaan penyimpanan isi berkas.
     *
     * <p><b>Tidak ada penjagaan perpindahan keadaan di sini.</b> Setter ini menerima teks
     * apa pun, termasuk lompatan langsung ke {@code "AVAILABLE"} yang melewati seluruh
     * pemeriksaan. Aturan perpindahan ditegakkan {@code JurnalFileService}; kode yang
     * memanggil setter ini di luar service tersebut melewati aturan itu sepenuhnya.</p>
     *
     * @param v keadaan penyimpanan yang baru
     */
    public void setStorageState(String v){storageState=v;}

    /**
     * Hasil pemindaian keamanan atas isi berkas.
     *
     * <p>Nilai yang dipasang {@code JurnalFileService} saat baris dibuat adalah
     * {@code "NOT_CONFIGURED"} &mdash; menandakan bahwa pemindai memang <b>belum
     * dipasang</b> pada lingkungan ini, bukan bahwa berkasnya sudah diperiksa dan bersih.
     * Kolom ini karena itu jangan dibaca sebagai jaminan keamanan berkas; ia mencatat
     * apakah pemeriksaan pernah dilakukan.</p>
     *
     * @return keadaan pemindaian keamanan
     */
    @Column(name="scan_state",nullable=false,length=30) public String getScanState(){return scanState;}

    /**
     * Menyetel hasil pemindaian keamanan.
     *
     * @param v keadaan pemindaian keamanan
     */
    public void setScanState(String v){scanState=v;}

    /**
     * Status karantina berkas.
     *
     * <p>Nilai awal yang dipasang saat baris dibuat adalah
     * {@code "RELEASED_BY_POLICY"} &mdash; berkas dilepas dari karantina berdasarkan
     * kebijakan, bukan berdasarkan hasil pemeriksaan. Bacalah bersama
     * {@link #getScanState()}: pada lingkungan tanpa pemindai, kedua kolom ini menyatakan
     * bahwa berkas dilepas tanpa pernah diperiksa.</p>
     *
     * @return status karantina
     */
    @Column(name="quarantine_state",nullable=false,length=30) public String getQuarantineState(){return quarantineState;}

    /**
     * Menyetel status karantina berkas.
     *
     * @param v status karantina
     */
    public void setQuarantineState(String v){quarantineState=v;}

    /**
     * Kunci idempotensi &mdash; penangkal unggahan ganda yang dijaga basis data.
     *
     * <p>Dibentuk {@code JurnalFileService} sebagai
     * {@code <sumber> + ":REPO_BITSTREAM:" + <id metadata>} dan dijaga kekangan unik
     * {@code uk_lampiran_jurnal_idempotency}. Karena penjagaannya berada di basis data,
     * dua permintaan yang tiba bersamaan tidak dapat sama-sama lolos &mdash; sesuatu yang
     * tidak dapat dijamin oleh pemeriksaan "cek dahulu, baru simpan" di kode.</p>
     *
     * @return kunci idempotensi, paling panjang 160 karakter
     */
    @Column(name="idempotency_key",nullable=false,length=160) public String getIdempotencyKey(){return idempotencyKey;}

    /**
     * Menyetel kunci idempotensi.
     *
     * <p>Wajib terisi sebelum penyimpanan. Nilai yang sudah dipakai baris lain akan
     * ditolak basis data; tangani penolakan itu sebagai "permintaan ini sudah pernah
     * diproses", bukan sebagai kesalahan yang perlu dicoba ulang.</p>
     *
     * @param v kunci idempotensi
     */
    public void setIdempotencyKey(String v){idempotencyKey=v;}

    /**
     * Userid pengguna yang membuat baris ini &mdash; field audit bayangan.
     *
     * <p>Disimpan sebagai teks, bukan relasi ke entitas pengguna, karena alasan lintas
     * {@code SessionFactory} yang dijelaskan pada Javadoc kelas. Nilainya disalin pada
     * saat kejadian sehingga tetap terbaca meski penggunanya kelak dihapus.</p>
     *
     * @return userid pembuat baris
     */
    @Column(name="created_by",nullable=false,length=100) public String getCreatedBy(){return createdBy;}

    /**
     * Menyetel userid pembuat baris.
     *
     * <p>Setter murni: tidak ada pemeriksaan bahwa userid tersebut benar-benar ada.</p>
     *
     * @param v userid pembuat baris
     */
    public void setCreatedBy(String v){createdBy=v;}

    /**
     * Userid pengguna yang terakhir mengubah baris ini &mdash; field audit bayangan.
     *
     * <p>Diperbarui {@code JurnalFileService} pada setiap perpindahan keadaan. Karena
     * pembaruannya dilakukan pemanggil dan bukan oleh lifecycle Hibernate, perubahan yang
     * dilakukan lewat jalur lain tidak akan tercermin di sini.</p>
     *
     * @return userid pengubah terakhir
     */
    @Column(name="updated_by",nullable=false,length=100) public String getUpdatedBy(){return updatedBy;}

    /**
     * Menyetel userid pengubah terakhir.
     *
     * @param v userid pengubah terakhir
     */
    public void setUpdatedBy(String v){updatedBy=v;}

    /**
     * Waktu baris ini dibuat.
     *
     * <p>Bernilai awal waktu pembentukan objek Java, lalu ditimpa pemanggil dengan waktu
     * yang seragam bersama {@link #getUpdatedAt()} pada saat penyimpanan. Disimpan sebagai
     * {@code TIMESTAMP} tanpa zona waktu, jadi maknanya bergantung pada zona waktu server.</p>
     *
     * @return waktu pembuatan; tidak pernah {@code null}
     */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;}

    /**
     * Menyetel waktu pembuatan baris.
     *
     * <p>Tidak dijaga terhadap penimpaan: nilai yang sudah tersimpan dapat diubah kapan
     * saja lewat setter ini, dan tidak ada yang mencegahnya. Perlakukan sebagai catatan
     * yang hanya ditulis sekali oleh jalur pembuatan.</p>
     *
     * @param v waktu pembuatan
     */
    public void setCreatedAt(Date v){createdAt=v;}

    /**
     * Waktu perubahan terakhir baris ini.
     *
     * <p><b>Tidak diperbarui otomatis.</b> Tidak ada anotasi lifecycle maupun pemicu yang
     * menyetelnya; setiap jalur yang mengubah baris wajib memanggil
     * {@link #setUpdatedAt(Date)} sendiri. {@code JurnalFileService} melakukannya pada
     * setiap perpindahan keadaan, tetapi jalur lain yang mengubah baris tanpa menyetel
     * kolom ini akan meninggalkan waktu yang menyesatkan.</p>
     *
     * @return waktu perubahan terakhir; tidak pernah {@code null}
     */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="updated_at",nullable=false) public Date getUpdatedAt(){return updatedAt;}

    /**
     * Menyetel waktu perubahan terakhir.
     *
     * <p>Harus dipanggil setiap kali baris diubah; lihat catatan pada
     * {@link #getUpdatedAt()}.</p>
     *
     * @param v waktu perubahan terakhir
     */
    public void setUpdatedAt(Date v){updatedAt=v;}
}
