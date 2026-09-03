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
import ais.database.model.VoKunci;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>PERIODE TUTUP BUKU</b> (closing) akuntansi — tabel {@code akunting.closing}.
 *
 * <p>Satu baris {@code Closing} adalah satu <b>tanggal batas</b> beserta nama dan keterangan
 * periodenya. Entity ini tidak menyimpan angka apa pun: bukan saldo, bukan jurnal penutup,
 * bukan laba ditahan. Ia hanya <b>penanda batas waktu</b>. Seluruh dampaknya terhadap buku
 * besar bekerja lewat satu kolom di entity lain, yaitu kolom {@code closing} pada
 * {@link ais.database.model.akunting.GrupTransaksi} (header jurnal double-entry). Jurnal
 * yang kolom {@code closing}-nya terisi berarti <b>sudah masuk periode tertutup</b>.</p>
 *
 * <h3>Bedakan dari "Tutup Buku" jurnal penutup</h3>
 * <p>Di repo ini ada dua hal berbeda yang sama-sama disebut tutup buku. Entity ini adalah
 * yang <b>lama/inti</b>: sekadar menandai periode. Yang menerbitkan <b>jurnal penutup</b>
 * akun nominal ke Laba Ditahan adalah modul terpisah ({@code TutupBukuHelper}, aksi REST
 * berawalan {@code tutup_buku_}, dan layar {@code siklus_tutup_buku.zul}). Keduanya tidak
 * saling memanggil. Menandai periode di sini <b>tidak</b> menerbitkan jurnal penutup, dan
 * menerbitkan jurnal penutup <b>tidak</b> mengunci periode.</p>
 *
 * <h3>Bagaimana jurnal ditautkan ke sebuah closing (TERVERIFIKASI)</h3>
 * <p>Penautan bukan pekerjaan entity ini melainkan dua prosedur kembar yang menulis SQL
 * langsung ke tabel jurnal:</p>
 * <ul>
 *   <li>{@code ClosingAction.reload()} (layar ZK) —
 *   {@code update akunting.grup_transaksi set closing=<id> where date(tanggal_transaksi) &lt;= <tanggal>};</li>
 *   <li>{@code ClosingApiHelper.tautkanUlang(Session)} (jalur REST/POS) — SQL setara,
 *   berparameter.</li>
 * </ul>
 * <p>Keduanya menelusuri seluruh baris closing dari <b>tanggal terbaru ke terlama</b>,
 * sehingga langkah yang lebih tua menimpa hasil langkah yang lebih baru untuk rentangnya
 * sendiri. Hasil akhirnya: tiap jurnal berakhir pada closing <b>paling awal</b> yang masih
 * mencakup tanggalnya. Dua catatan penting tentang prosedur ini:</p>
 * <ol>
 *   <li>Ia <b>tidak pernah</b> menulis {@code closing = NULL}. Sekali sebuah jurnal
 *   ditautkan, penautan ulang hanya dapat memindahkannya ke closing lain, tidak pernah
 *   melepasnya. Satu-satunya jalur yang benar-benar melepas adalah penghapusan closing
 *   lewat REST (lihat di bawah).</li>
 *   <li>SQL-nya <b>tanpa penyaring apa pun selain tanggal</b>: tidak ada filter tenant,
 *   satuan kerja, workspace, maupun jenis jurnal.</li>
 * </ol>
 *
 * <h3>Apa persisnya efek "closing" pada jurnal (TERVERIFIKASI)</h3>
 * <p>Kolom {@code GrupTransaksi.closing} yang terisi menutup tiga pintu sekaligus:</p>
 * <ul>
 *   <li><b>Pembatalan posting</b> — seluruh SQL pembatalan pada berkas {@code Posting*Action}
 *   menyertakan {@code and closing is null}; tombol "Batalkan Posting" di
 *   {@code GrupTransaksiAction} hanya tampil bila {@code getClosing() == null}; dan
 *   {@code NewUiJournalService.unpost} melempar {@code IllegalStateException}
 *   ("Posting jurnal closing tidak dapat dibatalkan").</li>
 *   <li><b>Perubahan dan penghapusan jurnal</b> — tombol Ubah dan Hapus baris jurnal di
 *   {@code GrupTransaksiAction} juga disyaratkan {@code closing == null}, dan
 *   {@code NewUiJournalService.guardEditable} menolak dengan "Jurnal telah masuk closing".
 *   Lampiran bukti transaksi pun ikut dikunci ({@code NewUiJournalService.upload}).</li>
 *   <li><b>Posting baru</b> — {@code NewUiJournalService.post} menolak memposting jurnal
 *   yang sudah masuk closing.</li>
 * </ul>
 * <p>Selain itu ada penjaga <b>di hulu</b> yang tidak memakai kolom tersebut melainkan
 * membandingkan tanggal terhadap {@code max(tanggal)} seluruh tabel ini:
 * {@code CommonAkunting} (dan {@code NewUiJournalService.guardDate}) menolak menyimpan
 * jurnal <b>baru</b> yang bertanggal sebelum tanggal closing terakhir. Jadi periode
 * tertutup tidak hanya beku terhadap dokumen lama, tetapi juga tidak dapat disisipi
 * dokumen baru.</p>
 *
 * <h3>Cache global — entity inilah sumbernya (TERVERIFIKASI)</h3>
 * <p>{@link ais.database.model.akunting.GrupTransaksi#getClosing()} memindai "cache global"
 * lewat {@code ConstantValues.ambilBerdasarClass(Closing.class)}. Cache itu adalah
 * {@code MemoryCacheUtil} yang diisi saat startup oleh {@code InitData} — kelas ini
 * terdaftar eksplisit pada salah satu panggilan {@code initClasses(...)}, berdampingan
 * dengan master statis seperti {@code Kelas}, {@code Penduduk}, dan {@code NomorSurat}.
 * Jadi ya: <b>seluruh isi tabel ini dimuat penuh ke memori proses</b> dan dipakai kembali
 * oleh setiap pembacaan {@code getClosing()}. Konsekuensi yang perlu diingat:</p>
 * <ul>
 *   <li>cache bersifat <b>satu untuk seluruh instalasi</b>, tidak dipecah per tenant;</li>
 *   <li>getter di {@code GrupTransaksi} memakainya untuk <b>memindahkan</b> jurnal ke
 *   periode closing lain — sebuah getter dengan efek tulis. Penjaganya
 *   ({@code if (closing != null)}) memastikan pemindahan tidak pernah mengembalikan nilai
 *   ke {@code null}, sehingga jalur itu <b>tidak</b> membuka kunci pembatalan;</li>
 *   <li>kelas ini juga terdaftar pada {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN}, yaitu
 *   daftar entity yang dikecualikan dari pembersihan data — periode tutup buku sengaja
 *   diperlakukan sebagai data referensi yang tidak boleh ikut terhapus.</li>
 * </ul>
 * <p><b>Koreksi terhadap catatan lama:</b> kekhawatiran bahwa pemindaian itu melempar
 * {@link NullPointerException} bila {@code tanggal} bernilai {@code null}
 * <b>tidak terbukti</b>. {@link #getTanggal()} tidak pernah mengembalikan {@code null}: ia
 * menggantinya dengan tanggal hari ini. Yang sebenarnya terjadi jauh lebih halus dan justru
 * lebih berbahaya — lihat peringatan pada {@link #getTanggal()}.</p>
 *
 * <h3>Cakupan tenant: GLOBAL, bukan per tenant (TERVERIFIKASI)</h3>
 * <p>Entity ini <b>tidak memiliki kolom {@code sekolah} maupun {@code yayasan}</b>, dan
 * {@link ais.database.model.akunting.GrupTransaksi} juga tidak memilikinya. SQL penautan
 * pada {@code reload()}/{@code tautkanUlang()} tidak menyaring tenant, penjaga tanggal di
 * {@code CommonAkunting} memakai {@code max(tanggal)} seluruh tabel, dan cache yang dipindai
 * {@code getClosing()} juga satu untuk semua. Artinya <b>satu pengguna yang menutup buku
 * mengunci jurnal SELURUH tenant pada instalasi yang sama</b>, dan siapa pun yang berhak
 * atas layar ini melihat serta mengubah periode milik semua tenant. Ini konsisten dengan
 * desain modul akuntansi (buku besarnya memang satu), tetapi harus disadari pada instalasi
 * multi-yayasan: batas periode tidak dapat dibedakan per unit.</p>
 *
 * <h3>Membuka kembali periode yang sudah ditutup — siapa yang bisa (TERVERIFIKASI)</h3>
 * <p>Tidak ada tombol "buka periode". Yang ada adalah efek samping dari tiga aksi:</p>
 * <ol>
 *   <li><b>Hapus closing lewat REST</b> ({@code ClosingApiHelper.hapus}) — satu-satunya
 *   jalur yang benar-benar membuka: ia menjalankan
 *   {@code update akunting.grup_transaksi set closing = NULL where closing = ?} sebelum
 *   menghapus barisnya, lalu menautkan ulang sisanya. Seluruh jurnal periode itu kembali
 *   dapat dibatalkan, diubah, dan dihapus.</li>
 *   <li><b>Hapus closing lewat layar ZK</b> — tombol Hapus baris hanya menghapus baris
 *   {@code Closing}; tidak ada pelepasan jurnal. Karena relasi di {@code GrupTransaksi}
 *   dipetakan {@code @NotFound(IGNORE)}, kolom {@code closing} yang menunjuk baris yang
 *   sudah tiada membuat {@code getClosing()} mengembalikan {@code null} — sehingga tombol
 *   Ubah/Hapus/Batalkan Posting kembali <b>tampak</b> aktif — sementara SQL pembatalan
 *   yang menyertakan {@code and closing is null} tetap melewati baris itu karena kolomnya
 *   secara fisik masih terisi. Keadaan pecah dua ini membuat aksi tampak berhasil padahal
 *   tidak mengubah apa pun.</li>
 *   <li><b>Ubah tanggal closing</b> — pada mode ubah, kotak tanggal tidak dinonaktifkan
 *   (penonaktifan hanya berlaku di mode tambah, saat tanggal dihitung otomatis dari closing
 *   terakhir). Memundurkan tanggal <b>tidak</b> melepas jurnal yang telanjur tertaut,
 *   karena penautan ulang tidak pernah menulis {@code NULL}; memajukan tanggal mengunci
 *   jurnal tambahan seketika.</li>
 * </ol>
 * <p>Perlindungan yang tersedia adalah <b>kunci baris</b> ({@link #getDikunci()}, warisan
 * {@link ais.database.model.VoKunci}): closing yang dikunci tidak dapat dihapus dari layar
 * ZK ({@code CommonUiFactoryHelper.copyEditDeleteButtons} mematikan tombol Hapus untuk
 * setiap {@code VoKunci} yang terkunci) maupun diubah/dihapus lewat REST. Perlu dicatat
 * bahwa penjaga ZK itu <b>hanya menutup Hapus, bukan Ubah</b>: tanggal batas sebuah closing
 * yang sudah dikunci masih dapat digeser dari layar. Pembukaan kunci sendiri dibatasi
 * berbeda di dua jalur — di ZK hanya pengguna yang mengunci yang boleh membuka
 * (dan hanya bila {@code Common.getApakahAdminBolehKunci()} mengizinkan), sedangkan di REST
 * pembatasan "pengunci yang sama" itu tidak ada.</p>
 *
 * <h3>Gerbang hak akses</h3>
 * <p>Layar ZK-nya ({@code closing.zul}) <b>bukan menu tersendiri</b>: ia dimuat sebagai
 * salah satu tab {@code PostingJurnalAction.TABS}. Hak Tambah/Ubah/Hapus atas periode tutup
 * buku karena itu <b>diwarisi dari menu induk "Posting Jurnal"</b> lewat
 * {@code CommonPrivilages.checkPrevilages}, yang membaca menu aktif di sesi — pola pewarisan
 * hak menu induk yang sama seperti pada master keuangan lain. Pada jalur REST, gerbangnya
 * adalah {@code ClosingApiHelper.bolehAksi}, yang mengembalikan {@code true} ketika peran
 * pengguna tidak terbaca ({@code role == null}) — pola fail-open yang perlu diperhatikan
 * khusus di sini, karena aksi yang dijaganya termasuk penghapusan closing, yaitu satu-satunya
 * jalur yang membuka kembali buku yang sudah ditutup.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Identitas &amp; penyajian</b>: {@link #getId()}, {@link #setId(Long)},
 *   {@link #toString()};</li>
 *   <li><b>Isi periode</b>: {@link #getNama()}, {@link #setNama(String)},
 *   {@link #getKeterangan()}, {@link #setKeterangan(String)}, dan yang paling menentukan,
 *   {@link #getTanggal()}/{@link #setTanggal(java.util.Date)};</li>
 *   <li><b>Jejak audit otomatis</b>: {@link #getOleh()}, {@link #setOleh(String)},
 *   {@link #getOlehId()}, {@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(java.util.Date)},
 *   serta kait {@code onUpdate()};</li>
 *   <li><b>Kunci baris &amp; alur SOP</b>: {@link #getDikunci()},
 *   {@link #setDikunci(ais.database.model.Tbmuser)}, {@link #getDisposisiSop()},
 *   {@link #setDisposisiSop(ais.database.model.sop.DisposisiSop)}.</li>
 * </ul>
 *
 * <h3>Hal-hal non-obvious</h3>
 * <ul>
 *   <li><b>Akses properti, bukan field.</b> {@code @Id} dipasang pada {@link #getId()},
 *   sehingga Hibernate membaca dan menulis setiap nilai lewat getter-nya. Semua kelakuan
 *   khusus di dalam getter ikut berlaku saat memuat, saat dirty-check, dan saat menyimpan —
 *   termasuk pemangkasan spasi {@link #getNama()} dan penggantian {@code null} pada
 *   {@link #getTanggal()}.</li>
 *   <li><b>{@code dynamicInsert}/{@code dynamicUpdate} aktif</b>, sehingga hanya kolom yang
 *   benar-benar berubah yang ikut dalam pernyataan SQL.</li>
 *   <li><b>{@code @Audited} (Envers)</b> menggandakan setiap versi baris ke tabel revisi.
 *   Riwayat perubahan nama periode ditampilkan di layar lewat
 *   {@code RevisiHelper.createNewRevisi}. Menghapus sebuah closing tidak menghapus jejak
 *   revisinya.</li>
 *   <li><b>Setter teks audit menolak nilai kosong.</b> {@link #setOleh(String)} dan
 *   {@link #setOlehId(String)} keluar diam-diam bila argumennya {@code null} atau berisi
 *   spasi saja; nilai yang sudah ada tidak bisa dikosongkan lewat setter.</li>
 *   <li><b>Bukan penanda tenant.</b> Sekali lagi: tidak ada {@code sekolah}/{@code yayasan}
 *   di sini.</li>
 *   <li><b>{@code serialVersionUID} bukan penanda apa pun.</b> Nilainya identik dengan
 *   {@link ais.database.model.akunting.GrupTransaksi} dan ratusan entity lain di repo ini —
 *   sisa keluaran generator hbm2java, bukan tanda kekerabatan.</li>
 *   <li><b>Induknya bukan entity terpetakan.</b> {@code Closing extends VoKunci extends
 *   DataSop extends GeneralValueObject}; {@code GeneralValueObject} adalah POJO abstrak
 *   biasa (bukan {@code @Entity}/{@code @MappedSuperclass}), sehingga properti yang
 *   dideklarasikan ulang di sini memang <b>keharusan teknis</b>, bukan duplikasi keliru.</li>
 * </ul>
 *
 * @see ais.database.model.akunting.GrupTransaksi
 * @see ais.database.model.VoKunci
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "closing")
public class Closing extends VoKunci {

	/**
	 * Penanda versi serialisasi bawaan generator hbm2java.
	 *
	 * <p>Nilainya identik dengan {@link ais.database.model.akunting.GrupTransaksi} dan
	 * ratusan entity lain di repo ini; itu <b>bukan</b> tanda hubungan antar kelas,
	 * melainkan konstanta yang sama yang disalin generator ke setiap keluarannya.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris periode tutup buku; {@code null} selama belum tersimpan. */
	private Long id;

	/** Nama pengguna terakhir yang menyimpan baris ini; diisi otomatis oleh interceptor audit. */
	private String oleh;

	/** Id pengguna terakhir yang menyimpan baris ini; diisi otomatis oleh interceptor audit. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang menyimpan periode ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah melewati interceptor audit.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan terakhir.
	 *
	 * <p>Argumen {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b>:
	 * method keluar tanpa mengubah apa pun, sehingga nilai lama tetap bertahan dan jejak
	 * audit tidak bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna; nilai kosong tidak berpengaruh.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau spasi saja
	 * diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; nilai kosong tidak berpengaruh.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang menyimpan periode ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA sekaligus deklarasi field waktu perubahan — keduanya berada pada
	 * satu baris sumber, persis seperti disisipkan oleh perkakas audit repo ini.
	 *
	 * <p>{@code onUpdate()} dipanggil Hibernate tepat sebelum setiap {@code UPDATE} baris
	 * ini dan menyerahkan pengisian jejak audit ({@code oleh}, {@code olehId},
	 * {@code tanggal_dirubah}) kepada
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}. Field
	 * {@code tanggal_dirubah} sendiri diberi nilai awal waktu pembuatan objek melalui
	 * {@code WaktuUtil.getDate()}, sehingga baris baru pun sudah membawa cap waktu.</p>
	 *
	 * <p><b>Jangan pisahkan baris ini</b> menjadi beberapa baris tanpa alasan: bentuknya
	 * seragam di seluruh entity repo dan dipakai sebagai penanda oleh perkakas penyisip.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir.
	 *
	 * <p>Biasanya tidak dipanggil kode aplikasi; pengisiannya diserahkan ke interceptor
	 * audit pada {@code onUpdate()}.</p>
	 *
	 * @param tanggal_dirubah cap waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu perubahan terakhir baris periode ini.
	 *
	 * @return cap waktu perubahan; tidak pernah {@code null} karena field diberi nilai awal
	 *         saat objek dibuat.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berupa {@code id + "-" + nama}.
	 *
	 * <p>Membaca <b>field</b> {@code nama} secara langsung, bukan lewat {@link #getNama()},
	 * sehingga spasi di ujung nama tidak dipangkas di sini. Bentuknya dipakai pada log dan
	 * keluaran diagnostik {@code ClosingAction.reload()}.</p>
	 *
	 * @return teks {@code "<id>-<nama>"}; kedua bagian dapat berupa {@code null} bila belum terisi.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama/judul periode tutup buku, mis. "Closing Desember 2025"; wajib diisi. */
	private String nama;

	/**
	 * <b>Tanggal batas periode</b> — satu-satunya field yang benar-benar menentukan perilaku
	 * entity ini: seluruh jurnal bertanggal transaksi pada atau sebelum tanggal ini ditautkan
	 * ke periode ini dan ikut terkunci.
	 */
	private Date tanggal;

	/** Keterangan bebas periode (kolom {@code text}); tidak memengaruhi penautan jurnal. */
	private String keterangan;

	/** Pengguna yang mengunci baris periode ini; {@code null} berarti belum dikunci. */
	private Tbmuser dikunci;

	/** Disposisi SOP yang menyertai baris ini bila alur persetujuan SOP dipakai; opsional. */
	private DisposisiSop disposisiSop;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate dan dipakai layar ZK maupun jalur REST
	 * untuk membangun baris periode baru sebelum {@link #setTanggal(java.util.Date)} dan
	 * {@link #setNama(String)} diisi.
	 */
	public Closing() {
	}

	/**
	 * Kunci utama baris periode tutup buku.
	 *
	 * <p>Nilai inilah yang ditulis ke kolom {@code closing} pada
	 * {@link ais.database.model.akunting.GrupTransaksi} oleh prosedur penautan, dan yang
	 * dipakai layar sebagai parameter URL {@code grup_transaksi.zul?closing=<id>} untuk
	 * menampilkan isi periode.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris.
	 *
	 * @param id kunci utama; umumnya diisi Hibernate, bukan kode aplikasi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama/judul periode tutup buku, sudah dipangkas spasi ujungnya.
	 *
	 * <p>Karena entity ini memakai akses properti, pemangkasan berlaku juga saat Hibernate
	 * menyimpan — nilai yang tersimpan di kolom adalah versi yang sudah di-{@code trim}.</p>
	 *
	 * @return nama periode tanpa spasi ujung, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama/judul periode.
	 *
	 * <p>Kekosongan divalidasi di lapisan pemanggil ({@code ClosingAction.onSave} dan
	 * {@code ClosingApiHelper.simpan} sama-sama menolak nama kosong), bukan di sini.</p>
	 *
	 * @param nama nama periode.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas periode.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas periode. Tidak memengaruhi penautan jurnal sama sekali.
	 *
	 * @param keterangan keterangan periode; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * <b>Tanggal batas periode tutup buku</b> — nilai yang menentukan jurnal mana yang terkunci.
	 *
	 * <p>Dibaca oleh hampir semua mekanisme penutupan buku: prosedur penautan
	 * ({@code ClosingAction.reload()} dan {@code ClosingApiHelper.tautkanUlang}), penjaga
	 * tanggal jurnal baru di {@code CommonAkunting} serta
	 * {@code NewUiJournalService.guardDate} (yang memakai {@code max(tanggal)} seluruh tabel),
	 * dan pemindaian cache pada
	 * {@link ais.database.model.akunting.GrupTransaksi#getClosing()}.</p>
	 *
	 * <p><b>PERINGATAN — getter ini tidak polos.</b> Bila field {@code tanggal} bernilai
	 * {@code null}, method mengembalikan <b>tanggal hari ini</b> ({@code WaktuUtil.getDate()}),
	 * bukan {@code null}. Akibatnya:</p>
	 * <ul>
	 *   <li>tidak akan pernah terjadi {@link NullPointerException} pada pemanggil yang
	 *   langsung memakai hasilnya — termasuk pemindaian cache di {@code GrupTransaksi} yang
	 *   sering dikira rawan NPE;</li>
	 *   <li>sebagai gantinya, baris dengan kolom tanggal kosong berperilaku seolah menutup
	 *   buku <b>sampai hari ini</b>, dan jawabannya <b>berubah setiap hari</b>. Prosedur
	 *   penautan yang dijalankan besok akan mengunci jurnal yang kemarin masih terbuka;</li>
	 *   <li>karena entity memakai akses properti, Hibernate juga menyimpan hasil getter ini.
	 *   Baris yang disimpan dengan {@code tanggal} kosong karena itu <b>membekukan</b>
	 *   tanggal hari penyimpanan ke dalam kolom.</li>
	 * </ul>
	 * <p>Layar ZK tidak pernah menyimpan tanggal kosong (kotak tanggalnya wajib), tetapi
	 * baris hasil impor atau tulisan langsung ke basis data bisa saja demikian.</p>
	 *
	 * @return tanggal batas periode; tidak pernah {@code null} — tanggal hari ini bila kolomnya kosong.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		return tanggal == null ? WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menyetel tanggal batas periode.
	 *
	 * <p><b>Ini operasi paling berdampak pada seluruh entity ini.</b> Perubahan tanggal
	 * tidak berlaku seketika terhadap jurnal; efeknya muncul saat prosedur penautan
	 * dijalankan sesudahnya ({@code ClosingAction.onSave} menjadwalkannya lewat timer,
	 * jalur REST memanggilnya langsung). Dua arah perubahan tidak simetris:</p>
	 * <ul>
	 *   <li><b>memajukan</b> tanggal mengunci jurnal tambahan pada penautan berikutnya;</li>
	 *   <li><b>memundurkan</b> tanggal <b>tidak melepas</b> jurnal yang telanjur tertaut,
	 *   karena prosedur penautan tidak pernah menulis {@code closing = NULL}.</li>
	 * </ul>
	 * <p>Keunikan tanggal antar baris divalidasi di pemanggil
	 * ({@code ClosingAction.checkNamaClosing} dan padanannya di jalur REST), bukan oleh
	 * batasan basis data.</p>
	 *
	 * @param tanggal tanggal batas periode; {@code null} akan dibaca sebagai hari ini oleh
	 *                {@link #getTanggal()}.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}


	/**
	 * Pengguna yang mengunci baris periode ini; {@code null} berarti periode belum dikunci.
	 *
	 * <p>Kunci baris adalah satu-satunya perlindungan terhadap penghapusan periode:
	 * {@code CommonUiFactoryHelper.copyEditDeleteButtons} mematikan tombol Hapus untuk setiap
	 * {@link ais.database.model.VoKunci} yang terkunci, dan jalur REST menolak menghapus
	 * maupun mengubah closing yang terkunci. Perlu dicatat bahwa penjaga di layar ZK
	 * <b>hanya menutup Hapus, tidak menutup Ubah</b>.</p>
	 *
	 * <p><b>Getter dengan tulis-balik.</b> Method menugaskan kembali field-nya dengan hasil
	 * {@code check(...)} milik {@code GeneralValueObject} (de-proxy/kanonikalisasi lewat
	 * cache, session yang tersedia, lalu session baru). Instance yang dikembalikan bisa
	 * berbeda dari yang tersimpan sebelumnya. Efek samping ini nyata: karena Hibernate
	 * memanggil getter saat flush, penulisan di dalamnya mengubah konteks persistensi yang
	 * sedang ditelusuri, dan {@code session.update()} atas closing pernah meledak dengan
	 * {@code ConcurrentModificationException}. Karena itu jalur REST sengaja mengunci dan
	 * membuka kunci lewat SQL langsung ke kolom {@code dikunci}, tidak lewat entity ini.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila periode belum dikunci.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menyetel pengguna pengunci periode.
	 *
	 * <p>Dipanggil dari widget kunci bersama {@code GeneralValueObject.tampilKunci}: nilai
	 * pengguna aktif untuk mengunci, {@code null} untuk membuka. Widget itu sendiri yang
	 * membatasi siapa boleh membuka (hanya pengunci yang sama, dan hanya bila
	 * {@code Common.getApakahAdminBolehKunci()} mengizinkan); di lapisan entity tidak ada
	 * pembatasan apa pun.</p>
	 *
	 * @param dikunci pengguna pengunci; {@code null} untuk membuka kunci.
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Disposisi SOP yang menyertai baris periode ini, bila alur persetujuan SOP dipakai.
	 *
	 * <p>Opsional dan tidak memengaruhi penguncian jurnal sama sekali; kehadirannya membuat
	 * penghapusan baris lewat tombol Hapus standar terlebih dahulu melewati
	 * {@code SopUtil.hapusDisposisi}. Seperti {@link #getDikunci()}, getter ini
	 * <b>menulis balik</b> hasil {@code check(...)} ke field-nya.</p>
	 *
	 * @return disposisi SOP terkait, atau {@code null} bila tidak ada.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP untuk baris periode ini.
	 *
	 * <p><b>Setter penjaga: menolak menghapus tautan.</b> Argumen {@code null} atau objek
	 * yang belum ber-id dibuang lebih dulu oleh penjaga di baris pertama, sehingga disposisi
	 * yang sudah terpasang tidak dapat dilepas lewat setter ini.</p>
	 *
	 * <p>Ekspresi ternary sesudah penjaga itu <b>tidak pernah memilih cabang pertamanya</b>:
	 * pada titik tersebut {@code disposisiSop} dipastikan tidak {@code null} dan ber-id,
	 * sehingga syarat {@code (disposisiSop == null || disposisiSop.getId() == null)} selalu
	 * salah dan hasilnya selalu argumen baru. Bentuk itu sisa penyeragaman lintas entity;
	 * dibiarkan apa adanya karena keluarannya sudah benar.</p>
	 *
	 * @param disposisiSop disposisi SOP; {@code null} atau objek tanpa id diabaikan.
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}
}
