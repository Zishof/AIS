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
 * Entity master <b>Akun Pajak</b> (tabel {@code akunting.akun_pajak}) — katalog sederhana yang
 * memasangkan sebuah <b>nama jenis pungutan pajak</b> dengan <b>satu akun buku besar bersaldo
 * normal kredit</b> dan sebuah <b>tarif persen</b>. Satu baris kira-kira berbunyi: "PPh 23 &rarr;
 * akun 2-1400 Hutang PPh 23 &rarr; 2%".
 *
 * <h2>KOREKSI PENTING: ini BUKAN tabel jembatan Akun&harr;Pajak</h2>
 * <p>Nama kelas mudah disalahpahami sebagai relasi many-to-many antara {@code Akun} (bagan akun)
 * dan {@code Pajak} (dokumen setoran pajak). <b>Itu tidak benar</b> dan sudah diverifikasi dari
 * kode: kelas {@code ais.database.model.akunting.Pajak} sama sekali <b>tidak</b> menyebut kelas
 * ini — tidak ada field, tidak ada koleksi, tidak ada import. Entity ini juga bukan master tarif
 * yang dipakai dokumen setoran tersebut; master tarif untuk {@code Pajak} adalah
 * {@code JenisPajakBarang} dan {@code JenisPajakPpn} di paket {@code ais.database.model.asset}.
 * Entity ini <b>berdiri sendiri</b>, dengan tepat satu relasi keluar: many-to-one opsional ke
 * {@code Akun}. Tidak ada satu pun entity lain yang menunjuk balik ke sini.</p>
 *
 * <h2>Status pemakaian: master HIDUP dengan hilir MATI</h2>
 * <p>Ini kuirk terpenting kelas ini. Layar CRUD-nya benar-benar hidup dan bisa dibuka pengguna,
 * tetapi <b>tidak ada satu pun konsumen yang membaca datanya</b>. Hasil penelusuran seluruh repo:</p>
 * <ul>
 *   <li><b>Hidup:</b> {@code ais.action.master.akunting.AkunPajakAction} +
 *       {@code WEB-INF/z/x/y/pages/master/akunting/akun_pajak.zul} — daftar, cari, tambah, ubah,
 *       hapus. Ini satu-satunya permukaan yang benar-benar dipakai.</li>
 *   <li><b>Mati (dikomentari):</b> {@code Transaksi} punya field {@code akunPajak} yang
 *       <b>dikomentari</b> (lihat {@code Transaksi.java}), begitu pula {@code TemplateTransaksi}
 *       yang mengomentari field, {@code @JoinColumn(name = "akun_pajak")}, getter, dan setter
 *       sekaligus. Artinya baris jurnal <b>tidak pernah</b> merujuk katalog ini; mesin posting
 *       tidak pernah membaca kolom {@code persen} maupun {@code akun} dari sini.</li>
 *   <li><b>Mati (yatim):</b> pemilih multi-baris
 *       {@code ais.action.master.akunting.helper.AmbilDataAkunPajakBanyak} — satu-satunya tempat
 *       di seluruh repo yang benar-benar <b>menghitung</b> nominal pajak dari katalog ini
 *       ({@code nilai * persen / 100} pada kolom "Jumlah Pajak") — <b>tidak pernah di-{@code new}
 *       oleh siapa pun</b>. Nol pemanggil. Jadi rumus tarif itu tidak pernah dieksekusi.</li>
 *   <li><b>Bukan REST:</b> berkas di {@code WEB-INF/new/akunting/...} hanya <em>scaffold</em>
 *       metadata hasil generator, bukan endpoint yang membaca/menulis tabel ini.</li>
 * </ul>
 * <p><b>Konsekuensi praktis:</b> kolom {@code persen} adalah <b>tarif yang tidak pernah
 * diterapkan</b>. Mengubahnya tidak mengubah angka pajak di dokumen mana pun, dan tidak
 * menggerakkan jurnal apa pun. Bila kelak dua kaki yang dikomentari di {@code Transaksi}/
 * {@code TemplateTransaksi} dihidupkan, entity ini langsung berubah menjadi sumber angka
 * finansial — dan seluruh catatan di bawah (terutama soal cakupan tenant) berubah dari "risiko
 * kertas" menjadi risiko jurnal.</p>
 *
 * <h2>Cakupan tenant: TIDAK ADA sama sekali</h2>
 * <p>Entity ini <b>tidak punya kolom tenant apa pun</b> — tidak ada {@code sekolah}, tidak ada
 * {@code yayasan}, tidak ada {@code satuanKerja}. Ini bukan kasus "fail-open kondisional" (filter
 * ada tapi dilewati saat data pengguna tidak lengkap) seperti pada banyak entity lain; di sini
 * memang <b>tidak ada apa pun untuk difilter</b>. Sejalan dengan itu,
 * {@code AkunPajakAction.initCriteria} membangun {@code Criteria} tanpa penyaring tenant sama
 * sekali — hanya {@code ilike} pada {@code nama}. Akibatnya katalog ini <b>global untuk seluruh
 * instalasi</b>: setiap pengguna yang bisa membuka menunya melihat, mengubah, dan menghapus baris
 * milik seluruh tenant. Polanya sama dengan {@code Closing} (penanda tutup buku yang juga tanpa
 * kolom tenant). Dampaknya saat ini rendah semata-mata karena hilirnya mati (lihat di atas).</p>
 *
 * <h2>Hubungan ke {@code Akun}</h2>
 * <p>Relasi {@link #getAkun()} adalah many-to-one <b>opsional</b> ({@code nullable = true}) dengan
 * {@code fetch = LAZY} dan {@code cascade = {PERSIST, MERGE}}. Di lapisan UI, akun dipilih lewat
 * {@code AmbilDataAkunKreditBanbox} yang menyetel {@code debetCredit = Akun.CREDIT} ({@code -1})
 * sehingga <b>hanya akun bersaldo normal kredit</b> yang muncul — konsisten dengan gagasan bahwa
 * pajak yang dipotong menjadi <em>kewajiban</em> (hutang pajak). Penyaringan itu murni kosmetik
 * di layar: tidak ada validasi di entity maupun di {@code onSave} yang menolak akun debet bila
 * data disisipkan lewat jalur lain. Kolom {@code akun} juga tetap boleh {@code null} di level
 * database meski {@code AkunPajakAction.onSave} menolak menyimpan akun kosong dari layar.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 *   <li><b>Identitas:</b> {@link #getId()} / {@link #setId(Long)} — kunci utama {@code IDENTITY}.</li>
 *   <li><b>Isi bisnis:</b> {@link #getNama()} / {@link #setNama(String)},
 *       {@link #getKeterangan()} / {@link #setKeterangan(String)},
 *       {@link #getAkun()} / {@link #setAkun(Akun)},
 *       {@link #getPersen()} / {@link #setPersen(Double)}.</li>
 *   <li><b>Jejak audit (deklarasi ulang dari base class):</b> {@link #getOleh()} /
 *       {@link #setOleh(String)}, {@link #getOlehId()} / {@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()} / {@link #setTanggal_dirubah(Date)}, plus kait
 *       {@code @PreUpdate onUpdate()}.</li>
 *   <li><b>Presentasi:</b> {@link #toString()} — dipakai combobox/label ZK.</li>
 * </ol>
 *
 * <h2>Catatan teknis: deklarasi ulang field audit BUKAN bug</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} sudah ada di
 * {@link ais.database.model.GeneralValueObject}, namun <b>wajib</b> dideklarasikan ulang di sini.
 * Alasannya: {@code GeneralValueObject} adalah POJO abstrak biasa — <b>bukan</b> {@code @Entity}
 * dan bukan {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan properti induknya sama
 * sekali. Tanpa deklarasi ulang, ketiga kolom itu tidak akan pernah tersimpan. Pola yang sama
 * dipakai di seluruh entity repo ini.</p>
 *
 * <h2>Audit dan riwayat revisi</h2>
 * <p>Kelas ditandai {@link Audited} (Hibernate Envers), sehingga setiap versi baris digandakan ke
 * tabel revisi {@code akunting.akun_pajak_aud}. Layar daftar menayangkan riwayat itu lewat
 * {@code RevisiHelper.createNewRevisi(...)}. Perlu diingat bahwa gerbang untuk melihat tabel
 * revisi <b>bukan</b> hak menu, melainkan daftar id role/pengguna pada konfigurasi
 * {@code boleh_lihat_revisi} (default {@code "am,amp"}).</p>
 *
 * <h2>Kuirk lain yang perlu diketahui</h2>
 * <ul>
 *   <li>Komentar {@code hbm2java} di atas anotasi berbunyi <i>"Bank generated by hbm2java"</i> —
 *       sisa salin-tempel dari entity {@code Bank}, bukan keterangan kelas ini.</li>
 *   <li>{@code serialVersionUID} bernilai {@code 2463821577548439808L}, konstanta boilerplate yang
 *       <b>dipakai bersama ratusan entity lain</b> di repo ini (mis. {@code Akun},
 *       {@code AkunArusKas}, {@code Agama}) — bukan sidik jari kelas ini dan tidak boleh dipakai
 *       untuk menyimpulkan hubungan kekerabatan antar-kelas.</li>
 *   <li>Hanya ada kait {@code @PreUpdate}; <b>tidak ada</b> {@code @PrePersist}. Pada baris yang
 *       baru dibuat, {@code oleh}/{@code olehId} tetap {@code null} dan {@code tanggal_dirubah}
 *       hanya berisi nilai inisialisasi field. Jejak "siapa membuat" baru terisi pada penyimpanan
 *       berikutnya.</li>
 *   <li>{@link #getNama()} me-{@code trim()} saat dibaca sementara {@link #setNama(String)} tidak
 *       me-{@code trim()} saat ditulis — spasi tepi tetap tersimpan di database dan ikut
 *       memengaruhi pencarian {@code ilike}/pengurutan, meski tidak terlihat di layar.</li>
 *   <li>Kelas dipetakan dengan <b>akses properti</b> (anotasi berada di getter), digabung
 *       {@code dynamicInsert}/{@code dynamicUpdate}. Artinya efek samping apa pun di dalam getter
 *       ikut terjadi saat Hibernate membaca entity — lihat catatan pada {@link #getAkun()} dan
 *       {@link #toString()}.</li>
 * </ul>
 *
 * <h2>Verifikasi negatif (hal-hal yang TIDAK berlaku di sini)</h2>
 * <ul>
 *   <li><b>Tidak ada permukaan REST fail-open.</b> {@code MasterKeuanganApiHelper} — helper yang
 *       {@code bolehAksi()}-nya memberi izin penuh kepada peran yang tidak terbaca — hanya melayani
 *       tujuh master keuangan ({@code JenisUangMuka}, {@code JenisKasKecil}, {@code JenisKasBesar},
 *       {@code JenisReimbursement}, {@code JenisPengeluaran}, {@code KategoriBiayaSales},
 *       {@code CaraPembayaranTransfer}). <b>{@code AkunPajak} tidak termasuk</b>, sehingga celah
 *       tersebut tidak menjangkau entity ini.</li>
 *   <li><b>Getter tidak destruktif.</b> {@link #getAkun()} memang menugaskan hasil balik ke field
 *       ({@code akun = check(akun)}), tetapi itu semata-mata resolusi proxy lazy standar
 *       {@code GeneralValueObject} — bukan penimpaan nilai bisnis seperti pada
 *       {@code Transaksi.getAkun()} yang menimpa {@code akun} dengan {@code akunOver}. Di sini
 *       tidak ada kolom bayangan, jadi membaca baris tidak pernah memindahkan atribusi akun.</li>
 *   <li><b>Tidak ada pewarisan hak lewat menu induk.</b> {@code akun_pajak.zul} tidak pernah
 *       di-{@code include} dari halaman lain; layar ini hanya dicapai sebagai menu tersendiri,
 *       sehingga {@code CommonPrivilages.checkPrevilages} membaca {@code currentMenu} yang memang
 *       milik layar ini.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see Akun
 * @see ais.database.dao.akunting.AkunPajakDao
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "akun_pajak")
public class AkunPajak extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p><b>Bukan sidik jari kelas ini.</b> Nilai {@code 2463821577548439808L} adalah konstanta
	 * boilerplate yang disalin ke ratusan entity di repo ini (mis. {@code Akun},
	 * {@code AkunArusKas}, {@code AcaraHasTransaksi}, {@code Agama}). Kesamaan nilai antar-kelas
	 * tidak berarti kelas-kelas itu berkerabat atau hasil salinan satu sama lain. Jangan mengubah
	 * nilainya: entity ini bisa berada di sesi ZK yang diserialisasi, dan perubahan nilai membuat
	 * sesi lama gagal dipulihkan.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris ({@code akunting.akun_pajak.id}). Lihat {@link #getId()}. */
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
	 * {@code @PreUpdate}. Karena tidak ada kait {@code @PrePersist}, nilainya masih {@code null}
	 * pada baris yang baru pertama kali disimpan.</p>
	 *
	 * <p>Tanpa anotasi {@code @Column}, sehingga dipetakan ke kolom bernama default sesuai
	 * strategi penamaan Hibernate yang berlaku.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila baris belum pernah diubah
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Perilaku non-obvious:</b> panggilan dengan {@code null} atau string kosong/whitespace
	 * <b>diabaikan diam-diam</b> (langsung {@code return} tanpa menyentuh field). Ini disengaja
	 * agar jalur simpan yang kebetulan tidak membawa identitas pengguna — proses batch, impor,
	 * atau permintaan tanpa sesi web — tidak <em>menghapus</em> jejak audit yang sudah benar.
	 * Konsekuensinya: jejak audit tidak pernah bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan tanpa efek
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks singkat baris ini, dipakai label dan combobox ZK.
	 *
	 * <p><b>Format:</b> {@code "&lt;akun&gt; - &lt;persen&gt;%"}, mis. {@code "2-1400 Hutang PPh 23 - 2.0%"}.
	 * Bila akun belum diisi, bagian akun menjadi string kosong sehingga hasilnya berbentuk
	 * {@code " - 2.0%"}.</p>
	 *
	 * <p><b>Efek samping (penting):</b> baris pertama method ini menugaskan
	 * {@code akun = getAkun()}, jadi <b>memanggil {@code toString()} menulis ke field
	 * {@code akun}</b>. Penugasan itu sebenarnya mubazir — {@link #getAkun()} sudah menugaskan
	 * hasil {@code check(...)} ke field yang sama — tetapi konsekuensinya nyata: {@code toString()}
	 * bukan operasi baca murni, ia dapat memicu resolusi proxy lazy dan (lewat {@code check})
	 * membuka sesi Hibernate cadangan bila entity sudah detached. Jangan memanggilnya di jalur
	 * panas atau di luar konteks sesi tanpa pertimbangan.</p>
	 *
	 * <p><b>Kuirk kedua:</b> nilai persen dibaca dari <b>field</b> {@code persen} secara langsung,
	 * bukan lewat {@link #getPersen()}. Karena {@link #setPersen(Double)} menerima {@code null}
	 * (dan {@code MyDoublebox} yang kosong memang menghasilkan {@code null}), keluarannya bisa
	 * berbunyi harfiah {@code " - null%"} alih-alih {@code "0%"} — berbeda dengan renderer grid
	 * yang menormalkan {@code null} menjadi {@code "0 %"}.</p>
	 *
	 * @return teks gabungan akun dan tarif persen; tidak pernah {@code null}
	 */
	public String toString() {
		akun = getAkun();
		return (akun == null ? "" : akun.toString()) + " - " + persen + "%";
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p>Berperilaku identik dengan {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/whitespace <b>diabaikan diam-diam</b> sehingga jejak audit lama tidak tertimpa oleh
	 * jalur simpan yang tidak membawa identitas pengguna.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan tanpa efek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh kait {@code @PreUpdate}; masih {@code null} untuk baris yang baru
	 * pertama kali disimpan karena tidak ada kait {@code @PrePersist}.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait siklus hidup JPA sebelum UPDATE, sekaligus deklarasi field stempel waktu perubahan.
	 *
	 * <p><b>Perhatian pemeliharaan:</b> baris berikut memuat <b>dua hal sekaligus</b> pada satu
	 * baris fisik — method {@code onUpdate()} beranotasi {@code @javax.persistence.PreUpdate} dan
	 * deklarasi field {@code tanggal_dirubah}. Bentuk ini valid namun mudah terlewat saat
	 * menyunting; jangan memecah atau menyisipkan apa pun di tengahnya tanpa menguji ulang.</p>
	 *
	 * <p><b>Apa yang dilakukan {@code onUpdate()}:</b> mendelegasikan ke
	 * {@code AuditTimestampInterceptor.ubah(this)}, yang (a) melewati pembaruan stempel bila
	 * {@code AuditTrailHelper} menilai tidak ada perubahan bisnis nyata, lalu (b) mengisi
	 * {@code tanggal_dirubah}, {@code oleh}, dan {@code olehId} dari konteks pengguna aktif.
	 * Dipanggil oleh Hibernate, <b>bukan</b> oleh kode aplikasi. Tidak ada padanan
	 * {@code @PrePersist}, sehingga INSERT pertama tidak melewati jalur ini.</p>
	 *
	 * <p><b>Field {@code tanggal_dirubah}:</b> diinisialisasi ke waktu pembuatan object melalui
	 * {@code ais.ui.util.WaktuUtil.getDate()}, sehingga baris baru tetap punya stempel wajar
	 * meski kait {@code @PreUpdate} belum pernah berjalan. Deklarasi ulang dari
	 * {@link ais.database.model.GeneralValueObject} — wajib, lihat Javadoc kelas.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil oleh {@code AuditTimestampInterceptor.ubah(...)} dari kait
	 * {@code @PreUpdate}, bukan oleh kode layar. Berbeda dari {@link #setOleh(String)}, setter ini
	 * <b>tidak</b> menyaring {@code null} — memanggilnya dengan {@code null} benar-benar
	 * mengosongkan stempel.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@link TemporalType#TIMESTAMP} (tanggal + jam). Dipakai antara lain oleh
	 * tampilan riwayat revisi Envers pada layar master.</p>
	 *
	 * @return waktu perubahan terakhir; untuk baris baru berisi waktu object dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama jenis pungutan pajak (mis. "PPh 21", "PPN"). Lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas, opsional. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Akun buku besar bersaldo normal kredit yang dipasangkan dengan jenis pajak ini.
	 * Opsional di level database. Lihat {@link #getAkun()}.
	 */
	private Akun akun;

	/**
	 * Tarif pajak dalam satuan persen (mis. {@code 2.0} berarti 2%), default {@code 0.0}.
	 *
	 * <p><b>Tarif ini tidak pernah diterapkan pada dokumen apa pun</b> — satu-satunya kode yang
	 * menghitung darinya ({@code AmbilDataAkunPajakBanyak}) tidak pernah dipakai. Lihat Javadoc
	 * kelas, bagian "Status pemakaian".</p>
	 */
	private Double persen = 0.0;

	/**
	 * Konstruktor default tanpa argumen.
	 *
	 * <p>Wajib ada untuk Hibernate (instansiasi lewat refleksi saat memuat baris) dan dipakai
	 * langsung oleh {@code AkunPajakAction.onAdd} untuk memulai mode "Tambah". Object hasil
	 * konstruktor ini punya {@code id} {@code null}, {@code persen} {@code 0.0}, dan
	 * {@code tanggal_dirubah} berisi waktu saat ini.</p>
	 */
	public AkunPajak() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dihasilkan database ({@code IDENTITY}); kolom ditandai {@code insertable = false}
	 * sehingga nilai tidak pernah dikirim pada INSERT. Nilai {@code null} adalah penanda baku
	 * "baris baru" — {@code AkunPajakAction} memakainya untuk memilih judul dialog
	 * ("Tambah" vs "Ubah") dan memilih {@code save()} vs {@code update()} pada DAO.</p>
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris ini.
	 *
	 * <p>Dipanggil Hibernate setelah INSERT. Kode aplikasi sebaiknya tidak memanggilnya: mengubah
	 * id object yang sudah terpersist akan membingungkan sesi Hibernate.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jenis pungutan pajak, sudah di-{@code trim()}.
	 *
	 * <p>Kolom {@code nama} bersifat {@code nullable = false} sepanjang 255 karakter, dan
	 * {@code AkunPajakAction.onSave} menolak menyimpan nama kosong. Nilai ini juga menjadi kunci
	 * pencarian di layar ({@code Restrictions.ilike("nama", ..., ANYWHERE)}) dan kunci pengurutan
	 * daftar.</p>
	 *
	 * <p><b>Asimetri yang perlu diperhatikan:</b> getter ini me-{@code trim()} <b>saat membaca</b>,
	 * sementara {@link #setNama(String)} menyimpan apa adanya. Spasi tepi karena itu tetap ada di
	 * database (dan ikut memengaruhi hasil {@code ilike} serta pengurutan) walau tidak pernah
	 * terlihat di layar.</p>
	 *
	 * <p><b>Fitur isi-otomatis:</b> pada dialog tambah/ubah, memilih akun lewat banbox akan
	 * menimpa isian nama dengan {@code toString()} akun yang dipilih. Nama karena itu sering
	 * berbentuk "kode - nama akun", bukan nama pajak yang diketik manual.</p>
	 *
	 * @return nama jenis pajak tanpa spasi tepi, atau {@code null} bila field memang {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama jenis pungutan pajak.
	 *
	 * <p>Menyimpan nilai apa adanya — tanpa {@code trim()} dan tanpa validasi panjang. Pembatasan
	 * "wajib diisi" ditegakkan di layar ({@code AkunPajakAction.onSave}), bukan di sini; batas 255
	 * karakter hanya ditegakkan database.</p>
	 *
	 * @param nama nama jenis pajak
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk baris ini.
	 *
	 * <p>Opsional ({@code nullable = true}) dan tanpa batas panjang eksplisit. Hanya ditampilkan
	 * sebagai label pada kolom terakhir grid daftar; tidak dipakai logika apa pun.</p>
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan akun buku besar yang dipasangkan dengan jenis pajak ini.
	 *
	 * <p><b>Relasi:</b> many-to-one opsional ke {@link Akun} lewat kolom {@code akun},
	 * {@code fetch = LAZY}, {@code cascade = {PERSIST, MERGE}}. Karena cascade tersebut, menyimpan
	 * baris {@code AkunPajak} ikut mem-persist/merge object {@code Akun} yang tertaut — hati-hati
	 * bila menyetel {@code Akun} yang dibangun manual, bukan hasil {@code load} dari database.</p>
	 *
	 * <p><b>Efek samping (penulisan field):</b> getter menugaskan {@code akun = check(akun)}.
	 * {@code check(...)} milik {@link ais.database.model.GeneralValueObject} mencoba
	 * me-<em>resolve</em> proxy lazy secara bertahap (penanda sudah-terinisialisasi &rarr; cache
	 * in-memory &rarr; session aktif &rarr; pembacaan ulang lewat session baru), dan hasilnya
	 * ditugaskan kembali ke field supaya pemanggilan berikutnya murah. Ini berarti sekadar
	 * <b>membaca</b> relasi bisa memicu query database bila entity sudah detached.</p>
	 *
	 * <p><b>Bukan getter destruktif.</b> Berbeda dari {@code Transaksi.getAkun()} yang menimpa
	 * {@code akun} dengan nilai kolom bayangan {@code akunOver} secara permanen, di sini tidak ada
	 * kolom bayangan sama sekali — nilai bisnis tidak pernah berubah karena dibaca. Yang berubah
	 * hanya representasi proxy menjadi object nyata.</p>
	 *
	 * <p><b>Batasan yang tidak ditegakkan:</b> UI hanya menawarkan akun bersaldo normal kredit
	 * ({@code AmbilDataAkunKreditBanbox} menyetel {@code debetCredit = Akun.CREDIT}), tetapi tidak
	 * ada validasi di entity yang menolak akun debet, dan kolomnya tetap {@code nullable = true}.</p>
	 *
	 * @return akun buku besar terkait; {@code null} bila belum dipasangkan atau bila proxy gagal
	 *         di-resolve
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menetapkan akun buku besar yang dipasangkan dengan jenis pajak ini.
	 *
	 * <p>Dipanggil {@code AkunPajakAction.onSave} dengan object {@link Akun} yang diambil dari
	 * atribut komponen banbox. Tidak ada normalisasi maupun validasi: object dengan id {@code null}
	 * akan ikut di-persist karena {@code cascade = PERSIST}.</p>
	 *
	 * @param akun akun buku besar terkait; boleh {@code null} untuk melepas kaitan
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan tarif pajak dalam satuan persen (mis. {@code 2.0} untuk 2%).
	 *
	 * <p>Tanpa anotasi {@code @Column}; dipetakan ke kolom bernama default sesuai strategi penamaan
	 * Hibernate yang berlaku. Nilai awal field adalah {@code 0.0}, tetapi bisa menjadi {@code null}
	 * setelah {@link #setPersen(Double)} dipanggil dengan {@code null} (mis. ketika kotak isian
	 * persen di dialog dikosongkan). Karena itu setiap pembacaan harus menganggap {@code null}
	 * mungkin terjadi — renderer grid menormalkannya menjadi {@code "0 %"}, sedangkan
	 * {@link #toString()} tidak.</p>
	 *
	 * <p><b>Tarif ini tidak pernah dipakai menghitung apa pun.</b> Satu-satunya rumus di repo yang
	 * membacanya ({@code nilai * persen / 100} pada {@code AmbilDataAkunPajakBanyak}) berada di
	 * kelas yang tidak pernah diinstansiasi; kaki {@code akunPajak} pada {@code Transaksi} dan
	 * {@code TemplateTransaksi} dikomentari. Lihat Javadoc kelas.</p>
	 *
	 * @return tarif dalam persen, atau {@code null}
	 */
	public Double getPersen() {
		return persen;
	}

	/**
	 * Menetapkan tarif pajak dalam satuan persen.
	 *
	 * <p><b>Tanpa validasi rentang:</b> nilai negatif maupun di atas 100 diterima apa adanya, dan
	 * {@code null} tidak disaring. {@code AkunPajakAction.onSave} juga tidak memvalidasinya — ia
	 * meneruskan langsung hasil {@code MyDoublebox.getValue()}, yang bernilai {@code null} bila
	 * kotak isian dikosongkan.</p>
	 *
	 * @param persen tarif dalam persen (mis. {@code 10.0} untuk 10%); boleh {@code null}
	 */
	public void setPersen(Double persen) {
		this.persen = persen;
	}

}
