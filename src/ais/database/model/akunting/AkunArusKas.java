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

import ais.database.model.GeneralValueObject;

/**
 * Entity master <b>Akun Arus Kas</b> ({@code akunting.akun_arus_kas}): sebuah <b>daftar tanda</b>
 * (penanda/whitelist) berisi akun-akun bagan akun yang oleh operator ditandai "berhubungan dengan
 * arus kas", disertai satu {@link #getKeterangan() keterangan} bebas. Satu baris kira-kira berbunyi:
 * "1-1100 Kas Besar &rarr; <i>penerimaan tunai loket</i>".
 *
 * <h2>VERIFIKASI: apa entity ini sesungguhnya (dan apa yang BUKAN)</h2>
 * <p>Nama kelas menyesatkan dalam dua arah sekaligus, dan keduanya sudah diverifikasi langsung dari
 * kode — bukan disimpulkan dari nama:</p>
 * <ul>
 *   <li><b>BUKAN katalog kategori arus kas.</b> Tidak ada satu pun kolom yang menyimpan kategori
 *       Operasional/Investasi/Pendanaan. Yang ada hanya {@link #getAkun() akun},
 *       {@link #getNama() nama} (yang bukan nama sendiri — lihat di bawah), dan
 *       {@link #getKeterangan() keterangan} berupa teks bebas tanpa daftar nilai sah. Tidak mungkin
 *       kelas ini menjadi tabel referensi tiga kategori standar PSAK/IAS&nbsp;7.</li>
 *   <li><b>BUKAN baris transaksional arus kas.</b> Tidak ada nominal, tidak ada
 *       debet/kredit, tidak ada tanggal transaksi, tidak ada periode/bulan/tahun, tidak ada relasi
 *       ke {@link Transaksi} maupun {@link GrupTransaksi}, dan tidak ada cap posting
 *       ({@code PostingHistory}). Isi baris tidak berubah ketika jurnal bertambah.</li>
 *   <li><b>Yang tersisa:</b> katalog satu-kolom-bermakna — sekadar penanda "akun ini dipilih" plus
 *       catatan. Semantik penandaan itu <b>tidak pernah ditegakkan kode mana pun</b> (lihat bagian
 *       status pemakaian), sehingga arti sesungguhnya sepenuhnya bergantung pada kebiasaan
 *       operator.</li>
 * </ul>
 *
 * <h2>VERIFIKASI: hubungan dengan {@code Akun.aktifitas} — TIDAK ADA</h2>
 * <p>Dugaan wajar bahwa entity ini adalah katalog yang dirujuk kolom
 * {@link ais.database.model.akunting.Akun#getAktifitas() Akun.aktifitas} <b>terbantah</b>. Fakta
 * dari kode:</p>
 * <ul>
 *   <li>{@code Akun.aktifitas} adalah kolom {@code String} biasa, bukan foreign key. Nilai sahnya
 *       adalah tiga konstanta yang <b>di-hardcode di dalam {@code Akun} sendiri</b>:
 *       {@link ais.database.model.akunting.Akun#OPERASI},
 *       {@link ais.database.model.akunting.Akun#INVESTASI}, dan
 *       {@link ais.database.model.akunting.Akun#PENDANAAN}; combobox pemilihnya di
 *       {@code AkunAction} diisi dari konstanta itu, bukan dari query ke tabel ini.</li>
 *   <li>{@link ais.database.model.akunting.Akun} <b>tidak punya field, koleksi, maupun import</b>
 *       yang menunjuk ke kelas ini. Arah sebaliknya juga tunggal: entity ini memegang satu
 *       many-to-one opsional ke {@code Akun} dan tidak ada entity lain yang menunjuk balik ke sini.</li>
 *   <li>Hubungan satu-satunya bersifat <b>tidak langsung dan tanpa penegakan</b>: kedua-duanya
 *       kebetulan berbicara tentang gagasan "arus kas" pada objek yang sama ({@code Akun}), lewat
 *       dua mekanisme yang tidak saling tahu.</li>
 * </ul>
 * <p>Karena itu semua penyebutan {@code Akun.aktifitas} sebagai "klasifikasi arus kas yang
 * benar-benar dipakai" <b>tidak boleh dibaca sebagai deskripsi kelas ini</b>. Klasifikasi yang
 * betul-betul dieksekusi laporan adalah rantai
 * {@code kelompok_laporan_punya_akun &rarr; kelompok_laporan &rarr; jenis_laporan} (yang keterangannya
 * mengandung kata "arus") dengan label seksi diambil dari {@code master_grup_laporan.nama}; kolom
 * {@code akun.aktifitas} hanya dipakai sebagai <b>cadangan</b> bila akun belum dipetakan di sana.
 * Keduanya dirakit di {@code LaporanKantinUtil} pada laporan berkunci
 * {@code "akn_arus_kas_aktivitas"}. Tabel {@code akun_arus_kas} tidak muncul di rantai itu sama
 * sekali.</p>
 *
 * <h2>Status pemakaian: NOL konsumen data, dan layar ZK-nya sudah putus</h2>
 * <p>Penelusuran menyeluruh seluruh repo (Java, ZUL, JSP, JRXML, SQL, JS, XML) atas nama kelas
 * {@code AkunArusKas} maupun nama tabel {@code akun_arus_kas} memberi hasil berikut:</p>
 * <ul>
 *   <li><b>Nol pembaca data.</b> Tidak ada satu pun Action, Helper, ApiHelper, servlet, atau JSP
 *       yang menanyakan tabel ini untuk keperluan apa pun selain menampilkannya kembali di layar
 *       CRUD-nya sendiri. Tidak ada mesin posting, tidak ada validasi, tidak ada pemilih (banbox)
 *       yang menyaring akun berdasarkan keanggotaan di tabel ini.</li>
 *   <li><b>Nol pemakaian di laporan arus kas.</b> Ini penting karena persis di sinilah kelas ini
 *       "seharusnya" berperan. Semua laporan arus kas yang hidup mengabaikannya:
 *       <ul>
 *         <li>{@code webapp/report/akunting/laporan_arus_12_bulan.jrxml} dan kembarannya
 *             {@code laporan_arus_kas.jrxml} / {@code laporan_arus_31_hari.jrxml} /
 *             {@code laporan_arus_30_hari.jrxml} — akun yang dilaporkan datang dari
 *             <b>parameter {@code $P{akun}}</b> yang dipilih manual pengguna lewat
 *             {@code AmbilDataAkunDebetBanbox} di {@code LaporanArus12Bulan}/{@code LaporanArus31Hari},
 *             bukan dari daftar di tabel ini.</li>
 *         <li>{@code webapp/report/akunting/laporan_keuangan_arus_kasi.jrxml} — memakai rantai
 *             {@code kelompok_laporan_punya_akun}/{@code kelompok_laporan}/{@code master_grup_laporan}
 *             (lihat {@link MasterGrupLaporan}), sama sekali bukan tabel ini.</li>
 *         <li>{@code LaporanKantinUtil} (laporan keuangan berbasis jurnal untuk koperasi/yayasan)
 *             mengenali akun Kas/Bank lewat <b>heuristik</b> — {@code bank_id is not null or norek is
 *             not null or nama ilike '%kas%' or nama ilike '%bank%'} — padahal tabel ini adalah
 *             tempat yang tepat untuk daftar itu. Bukti kuat bahwa penulis laporan tidak mengetahui
 *             (atau tidak mempercayai) keberadaan tabel ini.</li>
 *       </ul></li>
 *   <li><b>Layar ZK legacy sudah putus.</b> {@code ais.action.master.akunting.AkunArusKasAction}
 *       lengkap dan sehat (daftar, cari, paging, tambah, ubah, hapus, ekspor Excel, unggah Excel),
 *       tetapi <b>berkas ZUL yang menerapkannya tidak ada</b> di {@code webapp}: tidak ada
 *       {@code akun_arus_kas.zul}, dan tidak ada satu pun dari ~1.500 ZUL yang menyebut kelas Action
 *       ini pada atribut {@code apply}. Manifest generator merekam hal yang sama secara mandiri —
 *       {@code webapp/WEB-INF/new/akunting/catalog.json} mencantumkan
 *       {@code "legacyRefCount": 0} untuk halaman {@code akun_arus_kas}, sementara tetangganya
 *       {@code akun} bernilai 2. Tanpa ZUL, {@code GenericAutowireComposer} tidak pernah
 *       dikomposisi, sehingga seluruh Action itu adalah kode mati.</li>
 *   <li><b>Permukaan yang masih mungkin hidup: New UI generik.</b>
 *       {@code webapp/WEB-INF/new/akunting/uiux/akun_arus_kas.jsp} dan
 *       {@code .../services/akun_arus_kas_service.jsp} bukan sekadar tempelan metadata: service-nya
 *       menyerahkan permintaan ke {@code dispatcher.jsp}, yang memanggil
 *       {@code GenericCrudDefinitionRegistry.tryAutoRegister(...)} dengan
 *       {@code nuiServiceEntities = {"AkunArusKas"}}. Bila definisi berhasil dibangun, request
 *       diteruskan ke {@code GenericCrudHttpController} — artinya CRUD sungguhan atas tabel ini
 *       masih dapat berjalan lewat New UI, asalkan ada baris menu yang dipetakan ke rute
 *       {@code akunting/akun_arus_kas}.</li>
 * </ul>
 * <p><b>Kesimpulan status:</b> ini <b>variasi lebih parah</b> dari pola "master hidup dengan hilir
 * mati" yang ditemukan pada {@link AkunPajak}. Di sana layar CRUD-nya masih benar-benar bisa dibuka
 * dan hanya konsumennya yang dikomentari; di sini <b>hulu dan hilir sama-sama putus</b> — layar
 * lama tidak bisa dibuka karena ZUL-nya hilang, dan bahkan bila operator memelihara data lewat New
 * UI, tidak ada satu pun kode yang pernah membacanya. Berbeda dari entity tidur murni
 * ({@code TemplateTransaksi}, {@code TemplateGrupTransaksi}, {@code DetailLaporanPertahun}) yang nol
 * permukaan tulis, entity ini masih punya <b>satu</b> permukaan tulis yang berpotensi aktif.</p>
 *
 * <h2>Kolom {@code nama}: denormalisasi yang ditulis ulang saat dibaca</h2>
 * <p>Kolom {@code nama} <b>bukan</b> nama milik baris ini. {@link #getNama()} menugaskan
 * {@code nama = getAkun() == null ? "" : getAkun().getNama()} <b>tanpa syarat</b> setiap kali
 * dipanggil, jadi isinya selalu salinan nama akun terkait pada saat pembacaan terakhir. Karena
 * kelas dipetakan dengan <b>akses properti</b> (anotasi ada di getter) dan Hibernate memanggil
 * getter saat memuat maupun saat flush, konsekuensinya:</p>
 * <ul>
 *   <li>Nilai yang pernah disimpan di kolom {@code nama} <b>tidak dapat dipertahankan</b>. Sekali
 *       baris dibaca lalu di-flush, isinya ditimpa nama akun. {@link #setNama(String)} secara
 *       praktis tidak berpengaruh apa pun.</li>
 *   <li>Bila {@code akun} bernilai {@code null} atau referensinya patah, kolom {@code nama}
 *       ditimpa menjadi string kosong. Kolom dianotasi {@code nullable = false}, dan string kosong
 *       memenuhi batasan NOT NULL, jadi kerusakan ini <b>tidak pernah memicu error</b> — hanya
 *       menghapus jejak nama secara diam-diam.</li>
 *   <li>Nilai balikan di-{@code trim()} tetapi hasil trim <b>tidak</b> ditulis balik ke field, jadi
 *       spasi tepi milik nama akun tetap ikut tersimpan di kolom ini.</li>
 *   <li>Penulisan ulang itu juga <b>dicatat Envers</b>: setiap flush yang mengubah {@code nama}
 *       melahirkan satu revisi baru di {@code akunting.akun_arus_kas_aud}, meski pengguna tidak
 *       mengetik apa pun.</li>
 * </ul>
 * <p>Sisi baiknya, karena nilainya selalu diturunkan ulang, mengganti nama akun di master
 * {@code Akun} tidak meninggalkan salinan basi di sini — berbeda dari pola <i>snapshot</i> pada
 * {@code JenisReimbursement}. Sisi buruknya, kolom ini tidak dapat dipakai sebagai jejak historis
 * apa pun.</p>
 *
 * <h2>Cakupan tenant: tidak ada sama sekali</h2>
 * <p>Entity ini <b>tidak memiliki kolom tenant apa pun</b> — tidak ada {@code sekolah}, tidak ada
 * {@code yayasan}, tidak ada {@code satuanKerja}, tidak ada {@code workspace}. Ini bukan kasus
 * "fail-open kondisional" (penyaring ada tetapi dilewati bila data pengguna tidak lengkap),
 * melainkan ketiadaan total: memang tidak ada apa pun untuk disaring. Sejalan dengan itu,
 * {@code AkunArusKasAction.initCriteria} membangun {@code Criteria} tanpa penyaring tenant sama
 * sekali — hanya {@code ilike} pada {@code akun.kode}/{@code akun.nama}. Katalog ini karena itu
 * bersifat <b>global untuk seluruh instalasi</b>. Polanya sama dengan {@link AkunPajak} dan
 * {@code Closing}. Dampaknya saat ini rendah semata-mata karena datanya tidak pernah dibaca kode
 * mana pun; bila kelak entity ini dihidupkan sebagai penentu akun kas pada laporan, sifat global
 * ini langsung menjadi masalah nyata karena satu tenant dapat mengubah daftar akun kas tenant lain.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 *   <li><b>Identitas:</b> {@link #getId()} / {@link #setId(Long)} — kunci utama {@code IDENTITY}.</li>
 *   <li><b>Isi bisnis:</b> {@link #getAkun()} / {@link #setAkun(Akun)} (satu-satunya isi yang
 *       benar-benar dipilih pengguna), {@link #getKeterangan()} / {@link #setKeterangan(String)},
 *       serta {@link #getNama()} / {@link #setNama(String)} yang sesungguhnya adalah cermin dari
 *       {@code akun} dan bukan isian mandiri.</li>
 *   <li><b>Jejak audit (deklarasi ulang dari base class):</b> {@link #getOleh()} /
 *       {@link #setOleh(String)}, {@link #getOlehId()} / {@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()} / {@link #setTanggal_dirubah(Date)}, plus kait
 *       {@code @PreUpdate onUpdate()}.</li>
 *   <li><b>Presentasi:</b> {@link #toString()} — dipakai label/combobox ZK.</li>
 * </ol>
 *
 * <h2>Catatan teknis: deklarasi ulang field audit BUKAN bug</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} sudah ada di
 * {@link ais.database.model.GeneralValueObject}, namun <b>wajib</b> dideklarasikan ulang di sini.
 * Alasannya: {@code GeneralValueObject} adalah POJO abstrak biasa — <b>bukan</b> {@code @Entity} dan
 * bukan {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti induknya sama
 * sekali. Tanpa deklarasi ulang, ketiga kolom itu tidak akan pernah tersimpan. Hal yang sama berlaku
 * untuk {@code nama} dan {@code keterangan}. Pola ini dipakai di seluruh entity repo ini.</p>
 *
 * <h2>Audit dan riwayat revisi</h2>
 * <p>Kelas ditandai {@link Audited} (Hibernate Envers), sehingga setiap versi baris digandakan ke
 * tabel revisi {@code akunting.akun_arus_kas_aud}. {@code AkunArusKasAction} menayangkan riwayat itu
 * lewat {@code RevisiHelper.createNewRevisi(...)} — meskipun, seperti dijelaskan di atas, layar itu
 * sendiri tidak dapat dibuka karena ZUL-nya tidak ada. Gerbang untuk melihat tabel revisi
 * <b>bukan</b> hak menu, melainkan daftar id role/pengguna pada konfigurasi
 * {@code boleh_lihat_revisi}.</p>
 *
 * <h2>Kuirk lain yang perlu diketahui</h2>
 * <ul>
 *   <li><b>Kolom "Kode" di layar selalu kosong.</b> {@code AkunArusKasRenderer.render(...)}
 *       menampilkan {@code akunArusKas.getKode()} sebagai kolom pertama grid. {@code getKode()}
 *       diwarisi dari {@link ais.database.model.GeneralValueObject} dan mengembalikan field
 *       {@code kode} milik base class — field yang <b>tidak dipetakan Hibernate</b> (base class bukan
 *       entity) dan <b>tidak pernah di-{@code setKode(...)}</b> di mana pun untuk kelas ini. Jadi
 *       kolom itu permanen {@code null}. Ironisnya {@code initCriteria} mengurutkan dan mencari pada
 *       {@code akun.kode} yang benar — hanya penayangannya yang salah alamat. Bug ini kini tidak
 *       terlihat siapa pun karena layarnya sudah putus.</li>
 *   <li>Komentar {@code hbm2java} di atas anotasi berbunyi <i>"Bank generated by hbm2java"</i> —
 *       sisa salin-tempel dari entity {@code Bank}, bukan keterangan kelas ini. Komentar identik
 *       muncul juga di {@link AkunPajak}.</li>
 *   <li>{@code serialVersionUID} bernilai {@code 2463821577548439808L}, konstanta boilerplate yang
 *       <b>dipakai bersama ratusan entity lain</b> di repo ini (mis. {@code Akun}, {@link AkunPajak},
 *       {@code Agama}) — bukan sidik jari kelas ini dan tidak boleh dipakai untuk menyimpulkan
 *       hubungan kekerabatan antar-kelas.</li>
 *   <li>Hanya ada kait {@code @PreUpdate}; <b>tidak ada</b> {@code @PrePersist}. Pada baris yang baru
 *       dibuat, {@code oleh}/{@code olehId} tetap {@code null} dan {@code tanggal_dirubah} hanya
 *       berisi nilai inisialisasi field. Jejak "siapa membuat" baru terisi pada penyimpanan
 *       berikutnya.</li>
 *   <li>{@link #getOleh()}, {@link #getOlehId()}, dan {@link #getTanggal_dirubah()} tidak memakai
 *       {@code @Column}, sehingga nama kolomnya mengikuti strategi penamaan default Hibernate; pada
 *       PostgreSQL {@code olehId} terlipat menjadi {@code olehid}. Perhatikan hal ini saat menulis
 *       SQL mentah.</li>
 *   <li>Kelas dipetakan dengan <b>akses properti</b> digabung {@code dynamicInsert}/
 *       {@code dynamicUpdate}. Artinya efek samping di dalam getter ikut terjadi saat Hibernate
 *       membaca maupun menulis entity — lihat catatan pada {@link #getNama()}, {@link #getAkun()},
 *       dan {@link #toString()}.</li>
 *   <li>Pemilih akun di layar adalah {@code AmbilDataAkunBanbox} <b>tanpa</b> penyaring
 *       debet/kredit, berbeda dari {@link AkunPajak} yang memakai {@code AmbilDataAkunKreditBanbox}.
 *       Akun apa pun — termasuk akun beban atau akun induk non-daun — boleh ditandai di sini; tidak
 *       ada validasi entity maupun basis data yang membatasinya.</li>
 * </ul>
 *
 * <h2>Verifikasi negatif (hal-hal yang TIDAK berlaku di sini)</h2>
 * <ul>
 *   <li><b>Tidak terjangkau celah fail-open {@code bolehAksi()}.</b> {@code MasterKeuanganApiHelper}
 *       — helper REST yang memberi izin penuh kepada peran yang tidak terbaca — hanya melayani
 *       master {@code JenisUangMuka}, {@code JenisKasKecil}, {@code JenisKasBesar},
 *       {@code JenisReimbursement}, {@code JenisPengeluaran}, {@code KategoriBiayaSales}, dan
 *       {@code CaraPembayaranTransfer}. <b>{@code AkunArusKas} tidak termasuk</b>, dan tidak ada
 *       {@code *ApiHelper} lain di seluruh repo yang menyebut kelas ini. Nol permukaan REST lama.</li>
 *   <li><b>Permukaan New UI-nya fail-CLOSED.</b> {@code NewUiRouteGuard.isActionAuthorized}
 *       mengembalikan {@code false} sebagai cabang terakhir untuk aksi yang tidak dikenal, dan
 *       {@code permissionFor} mengembalikan {@code NewUiPermission.none()} bila simpul menu tidak
 *       ditemukan. Ini kebalikan dari pola fail-open pada helper API lama — tidak ada eskalasi hak
 *       lewat jalur ini.</li>
 *   <li><b>Tidak ada pewarisan hak lewat menu induk.</b> Pola "layar disisipkan sebagai tab dari
 *       menu lain sehingga {@code currentMenu} salah" tidak mungkin terjadi di sini: tidak ada ZUL
 *       sama sekali, jadi tidak ada yang bisa di-{@code include} dari halaman mana pun.</li>
 *   <li><b>{@link #getAkun()} tidak destruktif secara bisnis.</b> Penugasan {@code akun = check(akun)}
 *       murni resolusi proxy lazy standar {@code GeneralValueObject} — bukan penimpaan nilai bisnis
 *       seperti {@code Transaksi.getAkun()} yang menimpa {@code akun} dengan {@code akunOver}. Tidak
 *       ada kolom bayangan di sini. Getter yang benar-benar menulis nilai bisnis adalah
 *       {@link #getNama()}, dan yang ditulisnya hanya kolom denormalisasi.</li>
 *   <li><b>Tidak ada integritas finansial yang dipertaruhkan.</b> Tidak ada nominal, tidak ada
 *       jurnal, tidak ada gerbang persetujuan, dan tidak ada laporan yang membaca tabel ini —
 *       sehingga tidak satu pun task integritas finansial yang berjalan menyentuh kelas ini.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see Akun
 * @see AkunPajak
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "akun_arus_kas")
public class AkunArusKas extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p><b>Bukan sidik jari kelas ini.</b> Nilai {@code 2463821577548439808L} adalah konstanta
	 * boilerplate yang disalin ke ratusan entity di repo ini (mis. {@code Akun}, {@link AkunPajak},
	 * {@code Agama}). Kesamaan nilai antar-kelas tidak berarti kelas-kelas itu berkerabat atau hasil
	 * salinan satu sama lain. Jangan mengubah nilainya: entity ini bisa berada di sesi ZK yang
	 * diserialisasi, dan perubahan nilai membuat sesi lama gagal dipulihkan.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris ({@code akunting.akun_arus_kas.id}). Lihat {@link #getId()}. */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Deklarasi ulang dari
	 * {@link ais.database.model.GeneralValueObject} — wajib karena base class bukan entity/
	 * mapped-superclass. Lihat {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * Id pengguna terakhir yang mengubah baris ini. Deklarasi ulang dari
	 * {@link ais.database.model.GeneralValueObject}. Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code AuditTimestampInterceptor.ubah(this)} lewat kait
	 * {@code @PreUpdate}. Karena tidak ada kait {@code @PrePersist}, nilainya masih {@code null} pada
	 * baris yang baru pertama kali disimpan.</p>
	 *
	 * <p>Tanpa anotasi {@code @Column}, sehingga dipetakan ke kolom bernama default sesuai strategi
	 * penamaan Hibernate yang berlaku (pada PostgreSQL terlipat menjadi {@code olehid}).</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila baris belum pernah diperbarui
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam.</b> Bila {@code olehId} bernilai {@code null} atau
	 * hanya berisi spasi, method langsung {@code return} tanpa mengubah apa pun dan tanpa
	 * memberitahu pemanggil. Konsekuensinya jejak audit yang sudah terisi <b>tidak dapat dikosongkan
	 * kembali</b> lewat setter ini. Pola penjagaan yang sama dipakai di seluruh entity repo ini
	 * (lihat {@link #setOleh(String)}).</p>
	 *
	 * @param olehId id pengguna pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan representasi teks baris ini untuk label/combobox ZK: cukup {@code toString()}
	 * dari akun yang ditandai, atau string kosong bila akun belum diisi.
	 *
	 * <p><b>Menimpa perilaku base class.</b> {@link ais.database.model.GeneralValueObject#toString()}
	 * merakit {@code kode + " - " + nama}; di sini format itu diabaikan sepenuhnya. Karena
	 * {@code getKode()} pada kelas ini selalu {@code null} (lihat catatan kuirk di dokumentasi
	 * kelas), penimpaan ini sekaligus menyelamatkan tampilan dari awalan kosong.</p>
	 *
	 * <p><b>Menulis ke field.</b> Baris pertama menugaskan {@code akun = getAkun()}, jadi memanggil
	 * {@code toString()} <b>menulis ke field {@code akun}</b>. Efeknya jinak — yang ditulis hanyalah
	 * hasil {@code check(...)} ke field yang sama — tetapi konsekuensi lainnya nyata: {@code toString()}
	 * dapat memicu inisialisasi proxy lazy, dan {@code check(...)} berhak membuka sesi Hibernate baru
	 * bila entity sudah <i>detached</i>. Jangan memanggilnya di jalur yang sensitif terhadap jumlah
	 * query, misalnya di dalam perulangan render ribuan baris.</p>
	 *
	 * @return {@code toString()} akun terkait, atau {@code ""} bila {@code akun} kosong; tidak pernah
	 *         {@code null}
	 */
	public String toString() {
		akun = getAkun();
		return (akun == null ? "" : akun.toString());
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Berperilaku identik dengan {@link #setOlehId(String)}: nilai {@code null} atau yang hanya
	 * berisi spasi diabaikan secara diam-diam, sehingga jejak audit tidak bisa dihapus lewat setter
	 * ini.</p>
	 *
	 * @param oleh nama pengguna pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis lewat kait {@code @PreUpdate}; masih {@code null} pada baris yang baru
	 * pertama kali disimpan. Tanpa anotasi {@code @Column}.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diperbarui
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait siklus hidup JPA yang menyegarkan jejak audit tepat sebelum baris diperbarui, sekaligus
	 * deklarasi field {@code tanggal_dirubah} (keduanya sengaja ditulis pada satu baris fisik oleh
	 * generator; jangan dirapikan tanpa perlu karena pola yang sama muncul di ratusan entity).
	 *
	 * <p><b>Apa yang dilakukan {@code onUpdate()}:</b> mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan {@link #setTanggal_dirubah(Date)}
	 * dari pengguna sesi yang sedang aktif. Method ini <b>tidak pernah dipanggil kode aplikasi</b> —
	 * hanya oleh penyedia JPA saat {@code UPDATE}. Ia mengimplementasikan method {@code abstract}
	 * {@code onUpdate()} milik {@link ais.database.model.GeneralValueObject}.</p>
	 *
	 * <p><b>Tidak ada padanan {@code @PrePersist}</b>, sehingga pada {@code INSERT} pertama ketiga
	 * kolom audit tidak diisi siapa pun kecuali nilai awal {@code tanggal_dirubah} di bawah.</p>
	 *
	 * <p><b>Field {@code tanggal_dirubah}:</b> stempel waktu perubahan terakhir, diinisialisasi ke
	 * {@code ais.ui.util.WaktuUtil.getDate()} (waktu <em>server aplikasi</em>, bukan waktu basis
	 * data) pada saat objek Java dibuat — termasuk untuk objek yang dimuat dari database sebelum
	 * Hibernate menimpanya dengan nilai kolom.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Tanpa validasi: berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, nilai
	 * {@code null} diterima apa adanya. Umumnya dipanggil oleh {@code AuditTimestampInterceptor}
	 * lewat kait {@code @PreUpdate}, bukan oleh kode layar.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini, dipetakan sebagai
	 * {@code TIMESTAMP}.
	 *
	 * <p>Nilainya berasal dari jam server aplikasi (lihat catatan pada kait {@code @PreUpdate} di
	 * atas), sehingga tidak bisa dipakai untuk mengurutkan kejadian lintas node yang jamnya tidak
	 * tersinkron.</p>
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibuat
	 *         karena field diinisialisasi di deklarasinya
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Salinan denormalisasi nama akun terkait. <b>Bukan isian mandiri</b> — selalu ditulis ulang oleh
	 * {@link #getNama()}. Deklarasi ulang dari {@link ais.database.model.GeneralValueObject}, wajib
	 * karena base class tidak dipetakan Hibernate.
	 */
	private String nama;

	/**
	 * Catatan bebas operator tentang peran akun ini dalam arus kas. Satu-satunya kolom teks yang
	 * benar-benar diisi manusia. Deklarasi ulang dari
	 * {@link ais.database.model.GeneralValueObject}. Lihat {@link #getKeterangan()}.
	 */
	private String keterangan;

	/**
	 * Akun bagan akun yang ditandai oleh baris ini — satu-satunya isi bisnis sesungguhnya. Relasi
	 * many-to-one opsional; lihat {@link #getAkun()}.
	 */
	private Akun akun;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Juga dipakai langsung oleh {@code AkunArusKasAction.onAdd(...)} untuk membuka formulir dalam
	 * mode tambah ({@code getId() == null} menjadi penanda modenya). Tidak melakukan inisialisasi
	 * apa pun selain inisialisasi field pada deklarasinya masing-masing.</p>
	 */
	public AkunArusKas() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dibangkitkan basis data dengan strategi {@code IDENTITY} dan karenanya dipetakan
	 * {@code insertable = false} — nilai yang di-{@link #setId(Long)} sebelum {@code INSERT} tidak
	 * pernah dikirim ke basis data. Nilai {@code null} berarti baris belum tersimpan; kode layar
	 * memakai fakta itu untuk membedakan mode tambah dari mode ubah.</p>
	 *
	 * @return kunci utama, atau {@code null} bila entity belum pernah disimpan
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
	 * <p>Praktis hanya dipakai Hibernate sesudah {@code INSERT}. Menyetelnya secara manual pada
	 * entity baru tidak berpengaruh terhadap nilai yang tersimpan (kolom {@code insertable = false}),
	 * tetapi <b>berpengaruh terhadap alur layar</b> karena {@code AkunArusKasAction} memakai
	 * {@code getId() != null} sebagai penanda "mode ubah" dan akan memanggil
	 * {@code session.load(AkunArusKas.class, id)}.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama akun terkait — <b>dan menuliskannya kembali ke field {@code nama} setiap
	 * kali dipanggil</b>.
	 *
	 * <p><b>Getter destruktif.</b> Baris pertama menugaskan tanpa syarat
	 * {@code nama = getAkun() == null ? "" : getAkun().getNama()}. Kolom {@code nama} karena itu
	 * <b>bukan data mandiri</b> melainkan cermin dari relasi {@link #getAkun()}, dan nilai apa pun
	 * yang pernah ditulis {@link #setNama(String)} akan hilang pada pembacaan berikutnya. Karena
	 * kelas ini memakai akses properti, Hibernate memanggil getter ini saat memuat maupun saat
	 * flush — sehingga penulisan ulang terjadi juga tanpa keterlibatan kode aplikasi.</p>
	 *
	 * <p><b>Efek samping yang perlu diketahui:</b></p>
	 * <ul>
	 *   <li>Bila {@code akun} {@code null} (kolom FK memang {@code nullable = true}) atau
	 *       referensinya patah, kolom {@code nama} <b>ditimpa menjadi string kosong</b> — bukan
	 *       {@code null}, sehingga batasan {@code nullable = false} tetap terpenuhi dan kehilangan
	 *       data terjadi diam-diam tanpa error.</li>
	 *   <li>Setiap perubahan hasil penulisan ulang ini dicatat Envers sebagai revisi baru di
	 *       {@code akunting.akun_arus_kas_aud}, meski tidak ada pengguna yang mengetik apa pun.</li>
	 *   <li>Nilai balikan di-{@code trim()}, tetapi hasil trim <b>tidak</b> ditulis balik ke field;
	 *       spasi tepi milik nama akun tetap tersimpan di kolom.</li>
	 *   <li>Memanggil {@link #getAkun()} berarti method ini dapat memicu inisialisasi proxy lazy dan
	 *       — lewat {@code check(...)} — pembukaan sesi Hibernate baru bila entity sudah
	 *       <i>detached</i>. Hindari memanggilnya di dalam perulangan render besar.</li>
	 * </ul>
	 *
	 * <p><b>Dipanggil dari:</b> Hibernate (baca/flush), {@code AkunArusKasRenderer.render(...)}
	 * lewat {@code RevisiHelper.createNewRevisi(...)} untuk teks tautan riwayat, dan mesin ekspor
	 * Excel generik {@code Common.cetakData(...)}.</p>
	 *
	 * @return nama akun terkait yang sudah di-{@code trim()}, {@code ""} bila akun kosong, atau
	 *         {@code null} hanya bila nama akun sendiri bernilai {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		nama = getAkun() == null ? "" : getAkun().getNama();
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel salinan nama akun.
	 *
	 * <p><b>Secara praktis tidak berpengaruh:</b> nilai apa pun yang disetel di sini akan ditimpa
	 * oleh {@link #getNama()} pada pembacaan berikutnya — termasuk pembacaan yang dilakukan Hibernate
	 * sendiri saat flush. Setter ini ada semata-mata agar pasangan getter/setter lengkap bagi
	 * Hibernate dan bagi mesin unggah/ekspor generik. Untuk mengubah nama yang tampil, ubah akun
	 * yang ditunjuk lewat {@link #setAkun(Akun)} atau ubah nama akun di master {@code Akun}.</p>
	 *
	 * @param nama salinan nama akun; akan ditimpa pada pembacaan berikutnya
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas operator tentang peran akun ini dalam arus kas.
	 *
	 * <p>Diisi lewat {@code Textbox} multibaris pada formulir {@code AkunArusKasAction.init(...)}.
	 * Tidak ada daftar nilai sah, tidak ada validasi panjang di sisi entity, dan kolom
	 * {@code nullable = true}. <b>Menimpa</b> {@link ais.database.model.GeneralValueObject#getKeterangan()}
	 * yang menyubstitusi {@code null} menjadi {@code ""}; di sini {@code null} dikembalikan apa
	 * adanya, jadi pemanggil harus siap menerima {@code null}.</p>
	 *
	 * @return keterangan baris ini, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas operator.
	 *
	 * <p>Tanpa validasi dan tanpa {@code trim()} — spasi tepi tersimpan apa adanya. Dipanggil dari
	 * {@code AkunArusKasAction.onSave(...)} dengan isi {@code Textbox} keterangan.</p>
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan {@link Akun} yang ditandai baris ini, sesudah proxy lazy diresolusi.
	 *
	 * <p><b>Bentuk relasi:</b> many-to-one <b>opsional</b> ({@code nullable = true}) dengan
	 * {@code fetch = LAZY} dan {@code cascade = {PERSIST, MERGE}}. Kolom FK bernama {@code akun}.
	 * Kaskade PERSIST/MERGE berarti menyimpan baris ini ikut menyimpan perubahan pada objek
	 * {@code Akun} yang menempel — perhatikan bila objek akun diambil dari sesi lain lalu
	 * dimodifikasi. Tidak ada kaskade REMOVE, jadi menghapus baris ini tidak pernah menghapus akun.</p>
	 *
	 * <p><b>Penugasan ke field bukan penimpaan bisnis.</b> {@code akun = check(akun)} adalah pola
	 * resolusi proxy standar {@link ais.database.model.GeneralValueObject#check(Object)}: mencari
	 * instance kanonik di {@code EntityIdentityMap}, lalu menginisialisasi lewat sesi yang tersedia,
	 * lalu — bila entity sudah <i>detached</i> — memuat ulang dengan {@code openSession()} sendiri.
	 * {@code check(...)} tidak pernah melempar exception dan tidak pernah mengembalikan {@code null}
	 * untuk argumen non-null; kegagalan resolusi bersifat senyap. Berbeda dari
	 * {@code Transaksi.getAkun()}, tidak ada kolom bayangan yang menimpa nilai di sini — atribusi
	 * akun tidak pernah berpindah hanya karena baris dibaca.</p>
	 *
	 * <p><b>Biaya tersembunyi:</b> karena tahap ketiga {@code check(...)} dapat membuka sesi
	 * Hibernate baru, memanggil getter ini di dalam perulangan atas banyak baris berpotensi memicu
	 * satu sesi per baris. {@link #getNama()} dan {@link #toString()} keduanya memanggil getter ini,
	 * jadi biaya itu ikut terbawa ke sana.</p>
	 *
	 * <p><b>Tidak ada penyaringan jenis akun.</b> Formulir memakai {@code AmbilDataAkunBanbox} polos
	 * tanpa batasan debet/kredit maupun "hanya akun daun", dan entity tidak memvalidasi apa pun —
	 * akun jenis apa saja bisa masuk daftar ini.</p>
	 *
	 * @return akun yang ditandai, atau {@code null} bila baris belum/tidak menunjuk akun mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menyetel akun yang ditandai baris ini.
	 *
	 * <p>Tanpa validasi: {@code null} diterima, dan tidak ada pemeriksaan apakah akun itu bersaldo
	 * normal debet/kredit, merupakan akun daun, atau milik satuan kerja pengguna. Satu-satunya
	 * penjagaan ada di lapisan layar — {@code AkunArusKasAction.onSave(...)} menolak menyimpan bila
	 * banbox akun kosong — sehingga jalur lain (mesin unggah Excel generik, CRUD generik New UI,
	 * atau SQL langsung) tetap bisa membuat baris tanpa akun. Baris seperti itu tidak menyebabkan
	 * error, hanya membuat {@link #getNama()} menuliskan string kosong ke kolom {@code nama}.</p>
	 *
	 * <p>Perhatikan juga bahwa perubahan akun di sini <b>tidak</b> perlu diikuti pembaruan kolom
	 * {@code nama} secara manual: kolom itu diturunkan ulang otomatis pada pembacaan berikutnya.</p>
	 *
	 * @param akun akun yang akan ditandai; boleh {@code null}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

}
