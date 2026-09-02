package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

/**
 * Entity <b>satu pesan chat</b> (tabel {@code public.chat_message}) — baris isi percakapan yang
 * menyimpan pengirim ({@code from_user}), penerima ({@code to_user}), badan pesan
 * ({@code message}), sebuah penanda status bebas ({@code status}), sebuah kode
 * ({@code kode}), judul/label ({@code nama}), keterangan ({@code keterangan}), serta relasi
 * {@code ManyToOne} opsional ke <i>header</i> percakapan {@link ChatConvertation}. Dibangkitkan
 * {@code hbm2java} pada 16 April 2010 berpasangan dengan {@code ChatConvertation}
 * ({@code header} percakapan) sebagai satu rancangan chat berbasis dua tabel:
 * <b>percakapan &rarr; pesan</b>.
 *
 * <h3>PERINGATAN UTAMA: entity ini YATIM — bukan mesin chat yang benar-benar berjalan</h3>
 * <p>Penelusuran menyeluruh atas seluruh <i>source tree</i> (Java, ZUL, JSP, XML, JSON, SQL)
 * menemukan bahwa <b>tidak ada satu baris kode pun yang membaca atau menulis entity ini</b>.
 * Rujukan yang ada hanya dua, dan tidak satu pun berupa pemakaian nyata:</p>
 * <ol>
 * <li>baris pemetaan {@code <mapping class="ais.database.model.ChatMessage" />} di
 * {@code src/hibernate.cfg.xml} (berdampingan dengan {@code ChatConvertation}) — sehingga
 * tabelnya tetap dikenal dan skemanya tetap dipelihara Hibernate;</li>
 * <li>satu entri di manifes {@code WEB-INF/generic-crud/manifests/general_value_object_inventory}
 * berstatus {@code ELIGIBLE_METADATA_FIRST} (kandidat CRUD generik, <b>masih disabled</b>).</li>
 * </ol>
 * <p>Tidak ada {@code Action}, {@code Helper}, servlet API, maupun layar ZUL/JSP yang
 * menyentuhnya. Praktis class ini adalah <b>sisa rancangan chat 2010 yang tak pernah
 * dituntaskan</b>, yang tetap ikut ter-<i>deploy</i> karena masih terdaftar di konfigurasi
 * Hibernate. Polanya sama persis dengan {@link MenuMobile}: entity terpetakan tanpa pemilik.</p>
 *
 * <h3>JEBAKAN PENAMAAN — apa yang SEBENARNYA melayani fitur chat AIS</h3>
 * <p>Modul chat yang benar-benar berjalan di aplikasi <b>tidak memakai tabel ini sama
 * sekali</b>. Mesin chat nyata ada di paket {@code ais.action.master.chat}
 * ({@code ChatUsers} &rarr; {@code ChatWindow} &rarr; {@code ChatRoom} &rarr; {@code Chatter}
 * &rarr; {@code ChatUtil}) dan seluruhnya berporos pada entity {@link Pesan} (tabel
 * {@code public.pesan}) — <b>bukan</b> {@code ChatMessage}. Perbedaan rancangannya cukup besar,
 * sehingga keduanya tidak saling menggantikan:</p>
 * <ul>
 * <li><b>Identitas lawan bicara.</b> Di sini pengirim/penerima hanya sepasang {@link String}
 * bebas ({@link #getFrom()}/{@link #getTo()}) tanpa <i>foreign key</i> apa pun. Di {@link Pesan}
 * dipakai <b>delapan relasi</b> ber-FK: empat sisi pengirim ({@code tbmuser}, {@code mahasiswa},
 * {@code dosen}, {@code pegawai}) dan empat sisi penerima ({@code tbmuserf}, {@code mahasiswaf},
 * {@code dosenf}, {@code pegawaif}), sehingga penyaringan "pesan untuk saya dari dia" bisa
 * dilakukan di level SQL. Rancangan {@code ChatMessage} tidak menyediakan sarana itu.</li>
 * <li><b>Konteks kelas.</b> {@link Pesan} punya relasi {@code perkuliahan} plus flag
 * {@code semua} untuk siaran ke seluruh peserta kelas; di sini tidak ada padanannya.</li>
 * <li><b>Waktu kirim.</b> {@link Pesan} punya kolom {@code waktu} tersendiri. Di sini
 * <b>tidak ada kolom waktu kirim</b> — lihat catatan pada {@link #getTanggal_dirubah()}.</li>
 * <li><b>Siklus hidup baris.</b> Baris {@link Pesan} bersifat sementara: setelah dirender ke
 * layar penerima, {@code Chatter.renderMessages(...)} <b>menghapusnya</b> (pesan japri) atau
 * menandainya sudah diterima pada kolom {@code diterimaOleh} (pesan siaran). Rancangan
 * {@code ChatMessage}+{@code ChatConvertation} justru berbentuk arsip permanen berbasis
 * percakapan.</li>
 * </ul>
 * <p>Kesimpulannya: bila suatu saat ada yang hendak "memperbaiki chat", <b>jangan mulai dari
 * file ini</b> — mulailah dari {@link Pesan} dan paket {@code ais.action.master.chat}.</p>
 *
 * <h3>Hubungan dengan {@link ChatConvertation}</h3>
 * <p>{@link ChatConvertation} (tabel {@code chat_convertation}) adalah <i>header</i>-nya:
 * ia menyimpan {@code from_username}/{@code to_username}, {@code nama} (judul percakapan) dan
 * {@code keterangan}. Relasi diarahkan satu arah dari sisi pesan lewat
 * {@link #getChatConvertation()} ({@code ManyToOne}, kolom {@code chat_convertation}); sisi
 * {@code ChatConvertation} <b>tidak</b> punya koleksi balik ke {@code ChatMessage}, sehingga
 * pengambilan daftar pesan sebuah percakapan harus lewat query eksplisit.</p>
 * <p><b>Duplikasi yang perlu diwaspadai:</b> pasangan {@link #getFrom()}/{@link #getTo()} di
 * sini menduplikasi {@code fromUsername}/{@code toUsername} milik header. Karena keduanya
 * bertipe {@link String} tanpa FK dan tanpa satu pun kode yang mengisinya, <b>tidak ada
 * mekanisme apa pun yang menjaga konsistensi keduanya</b>. Bila entity ini kelak dihidupkan,
 * salah satu dari kedua sumber kebenaran itu harus dipilih dan yang lain dibuang.</p>
 *
 * <h3>Relasi dengan {@code GeneralValueObject}</h3>
 * <p>Class ini turunan langsung {@link ais.database.model.GeneralValueObject}, yang
 * <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa —
 * Hibernate <b>tidak</b> memetakan properti induknya. Karena itu deklarasi ULANG {@link #id},
 * {@link #oleh}, {@link #olehId} dan {@link #tanggal_dirubah} di sini <b>bukan bug atau
 * duplikasi ceroboh</b>, melainkan keharusan teknis supaya keempat kolom itu ikut terpetakan.
 * Konsekuensinya field-field tersebut <b>membayangi (shadow)</b> field senama milik induk;
 * yang terbaca dari luar selalu versi milik {@code ChatMessage} ini.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ol>
 * <li><b>Identitas &amp; penyajian:</b> {@link #getId()} (PK {@code IDENTITY}),
 * {@link #getNama()}, {@link #getKeterangan()}, {@link #getKode()}, {@link #toString()}, plus
 * konstruktor tanpa argumen {@link #ChatMessage()}.</li>
 * <li><b>Isi percakapan:</b> {@link #getFrom()} (pengirim), {@link #getTo()} (penerima),
 * {@link #getMessage()} (badan pesan), {@link #getStatus()} (penanda status/baca),
 * {@link #getChatConvertation()} (induk percakapan).</li>
 * <li><b>Jejak audit (deklarasi ulang milik induk):</b> {@link #getOleh()},
 * {@link #getOlehId()}, {@link #getTanggal_dirubah()}, kait {@link #onUpdate()}.</li>
 * </ol>
 * <p>Tidak ada method bisnis, tidak ada method query statis, tidak ada validasi, dan tidak ada
 * konstanta status. Seluruh isi class adalah pasangan getter/setter murni ditambah
 * {@link #toString()} dan kait audit — konsisten dengan status yatimnya.</p>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui pemelihara</h3>
 * <ul>
 * <li><b>Tidak ada getter yang berefek samping.</b> Diverifikasi langsung atas seluruh isi file
 * ini: tidak ada getter yang menulis balik ke field (tidak ada <i>lazy init</i> seperti
 * {@code MenuMobile.getId()}/{@code getAktif()}), tidak ada getter yang membuka/menutup
 * {@code Session} Hibernate, dan tidak ada getter destruktif (yang menghapus/mengubah baris DB).
 * {@link #getNama()} memang memanggil {@code trim()}, tetapi hasilnya hanya dikembalikan dan
 * <b>tidak</b> disimpan kembali ke field. Membaca objek ini aman dari <i>dirty checking</i>.</li>
 * <li><b>Tetapi dua SETTER-nya berefek "menolak diam-diam".</b> {@link #setOleh(String)} dan
 * {@link #setOlehId(String)} langsung {@code return} bila nilai baru {@code null} atau kosong,
 * sehingga jejak audit yang sudah terisi <b>tidak dapat dikosongkan lagi</b> lewat setter. Ini
 * pola berulang di seluruh keluarga {@code GeneralValueObject}, bukan kekhususan file ini.</li>
 * <li><b>{@code from} dan {@code to} adalah kata kunci SQL</b>, karena itu kolomnya sengaja
 * dinamai {@code from_user}/{@code to_user} lewat {@code @Column}. Nama <i>property</i> Java-nya
 * tetap {@code from}/{@code to} (keduanya legal di Java, bukan kata kunci) — jadi query HQL
 * harus memakai {@code from}/{@code to}, sedangkan SQL native memakai {@code from_user}/
 * {@code to_user}.</li>
 * <li><b>Tiga property tanpa {@code @Column}:</b> {@link #getKode()}, {@link #getMessage()} dan
 * {@link #getStatus()} mengandalkan penamaan default Hibernate, sehingga terpetakan ke kolom
 * {@code kode}, {@code message} dan {@code status}. Perubahan strategi penamaan global akan
 * memengaruhi ketiganya, tidak seperti property lain yang namanya dikunci eksplisit.</li>
 * <li><b>{@code status} adalah {@link String} bebas, bukan boolean "sudah dibaca".</b> Tidak ada
 * konstanta, {@code enum}, maupun validasi yang membatasi nilainya, dan tidak ada kode yang
 * pernah mengisinya. Jangan berasumsi ada semantik tertentu di baliknya.</li>
 * <li><b>{@code serialVersionUID} identik dengan {@link ChatConvertation} dan {@link Pesan}</b>
 * ({@code 2463821577548439808L}). Ini artefak salin-tempel dari templat yang sama, bukan tanda
 * ketiganya kompatibel secara serialisasi.</li>
 * <li><b>{@code @Audited} (Hibernate Envers) aktif.</b> Setiap perubahan baris juga ditulis ke
 * tabel revisi {@code chat_message_AUD}. Artinya, seandainya tabel ini kelak benar-benar
 * dipakai menyimpan pesan pribadi, <b>menghapus baris pesan tidak menghapus isinya dari
 * riwayat</b> — salinan lamanya tetap tersimpan permanen di tabel audit.</li>
 * </ul>
 *
 * <h3>Catatan keamanan/privasi bila entity ini kelak dihidupkan</h3>
 * <p>Selama masih yatim, class ini <b>tidak</b> menimbulkan risiko aktif — tidak ada jalur baca
 * maupun tulis. Yang perlu dicatat adalah risiko <i>laten</i>: entity ini terpetakan Hibernate
 * dan terdaftar sebagai kandidat CRUD generik, padahal isinya (badan pesan pribadi berikut
 * identitas kedua belah pihak) termasuk data paling sensitif dalam sistem. Rancangannya sendiri
 * <b>tidak menyediakan satu pun sarana penegakan kepemilikan</b>: tanpa relasi ke
 * {@link Tbmuser}, penyaringan "hanya pesan milik saya" hanya bisa dilakukan dengan mencocokkan
 * string {@code from_user}/{@code to_user} — cara yang rapuh dan mudah terlewat. Setiap upaya
 * menghidupkan entity ini harus lebih dulu menetapkan gerbang otorisasi per-baris di sisi
 * server, bukan sekadar menyembunyikan tombol di UI.</p>
 *
 * @see ChatConvertation
 * @see Pesan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "chat_message")

public class ChatMessage extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya <b>identik</b> dengan milik
	 * {@link ChatConvertation} dan {@link Pesan} — artefak salin-tempel templat entity, bukan
	 * penanda kompatibilitas antar-class.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Primary key baris pesan, kolom {@code id}. Dideklarasikan ulang di sini (bukan diwarisi
	 * efektif) karena {@code GeneralValueObject} tidak dipetakan Hibernate. Lihat
	 * {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama/label pengguna terakhir yang menyimpan baris ini (jejak audit). Diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan oleh layar. Deklarasi ulang
	 * milik {@code GeneralValueObject}.
	 */
	private String oleh;
	/**
	 * {@code userId} pengguna terakhir yang menyimpan baris ini (jejak audit), pasangan dari
	 * {@link #oleh}. Deklarasi ulang milik {@code GeneralValueObject}.
	 */
	private String olehId;

	/**
	 * Mengembalikan {@code userId} pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return id pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel {@code userId} pengguna penyimpan terakhir. <b>Menolak diam-diam</b> nilai
	 * {@code null} maupun string kosong/spasi: pada kasus itu method langsung {@code return}
	 * tanpa mengubah apa pun, sehingga nilai lama tetap dipertahankan dan jejak audit yang sudah
	 * terisi tidak bisa dikosongkan lewat setter ini.
	 *
	 * @param olehId id pengguna penyimpan; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir. Sama seperti {@link #setOlehId(String)},
	 * <b>menolak diam-diam</b> nilai {@code null} maupun string kosong/spasi.
	 *
	 * @param oleh nama pengguna penyimpan; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} sekaligus deklarasi field {@link #tanggal_dirubah} (keduanya
	 * ditulis pada satu baris fisik oleh generator, format ini dipertahankan apa adanya).
	 *
	 * <p>{@code onUpdate()} dipanggil <b>otomatis oleh Hibernate/JPA</b> tepat sebelum baris ini
	 * di-{@code UPDATE}, lalu mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang menyegarkan
	 * {@link #tanggal_dirubah} serta {@link #oleh}/{@link #olehId} dari pengguna sesi berjalan.
	 * Tidak ada padanan {@code @PrePersist}, sehingga jejak audit baris <b>baru</b> hanya
	 * mengandalkan nilai awal field.</p>
	 *
	 * <p>Field {@link #tanggal_dirubah} sendiri diinisialisasi seketika saat objek dibuat dengan
	 * {@code ais.ui.util.WaktuUtil.getDate()} (waktu server, menghormati penyetelan zona/offset
	 * aplikasi), bukan {@code new Date()} langsung.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini. Umumnya <b>tidak</b> dipanggil kode aplikasi:
	 * pengisiannya diserahkan ke kait {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; {@code null} diterima apa adanya
	 *                        (berbeda dengan {@link #setOleh(String)} yang menolak nilai kosong)
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, presisi
	 * {@code TIMESTAMP}).
	 *
	 * <p><b>Catatan penting:</b> entity ini <b>tidak punya kolom waktu kirim pesan</b> tersendiri
	 * — tidak ada padanan {@code Pesan.waktu}. Karena field ini diinisialisasi saat objek dibuat
	 * dan baru berubah pada {@code UPDATE} berikutnya, nilainya efektif merangkap sebagai waktu
	 * pembuatan pesan <i>selama baris itu belum pernah diubah</i>. Begitu baris disunting sekali
	 * saja, waktu kirim aslinya hilang tanpa jejak (kecuali ditelusuri lewat tabel audit Envers
	 * {@code chat_message_AUD}).</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris ini dalam bentuk {@code id + "-" + nama}, mengikuti
	 * konvensi {@code toString()} keluarga entity AIS.
	 *
	 * <p>Dua hal non-obvious: (1) method ini membaca <b>field</b> {@link #nama} secara langsung,
	 * bukan lewat {@link #getNama()}, sehingga hasilnya <b>tidak di-{@code trim()}</b>;
	 * (2) tidak ada penjagaan {@code null} — bila {@link #id} atau {@link #nama} kosong,
	 * hasilnya berisi literal {@code "null"} (mis. {@code "null-null"}), bukan
	 * {@link NullPointerException}. Perhatikan pula bahwa yang ditampilkan adalah judul/label
	 * pesan, <b>bukan</b> {@link #getMessage() isi pesannya} — jadi {@code toString()} ini tidak
	 * membocorkan badan percakapan ke log atau dialog UI.</p>
	 *
	 * @return gabungan id dan nama, mis. {@code "12-Percakapan awal"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Judul/label pesan, kolom {@code nama} bertipe {@code text}. Bukan badan pesan — isi
	 * percakapan ada di {@link #message}.
	 */
	private String nama;
	/**
	 * Pengirim pesan sebagai teks bebas, kolom {@code from_user}. Tanpa <i>foreign key</i> ke
	 * {@link Tbmuser} atau entity identitas mana pun.
	 */
	private String from;
	/**
	 * Penerima pesan sebagai teks bebas, kolom {@code to_user}. Tanpa <i>foreign key</i>, sama
	 * seperti {@link #from}.
	 */
	private String to;
	/** Keterangan/catatan tambahan bebas, kolom {@code keterangan}. */
	private String keterangan;
	/**
	 * Kode/penanda bebas pesan, dipetakan ke kolom {@code kode} lewat penamaan default Hibernate
	 * (tanpa {@code @Column} eksplisit).
	 */
	private String kode;
	/**
	 * <b>Badan pesan</b> — isi percakapan yang sesungguhnya, dipetakan ke kolom {@code message}
	 * lewat penamaan default Hibernate. Tidak diberi {@code columnDefinition = "text"} seperti
	 * {@link #nama}, sehingga panjangnya tunduk pada default dialek (umumnya
	 * {@code varchar(255)}) bila skema dibangkitkan otomatis.
	 */
	private String message;
	/**
	 * Penanda status pesan sebagai {@link String} bebas (kolom {@code status}) — bukan boolean
	 * "sudah dibaca". Tidak ada konstanta, {@code enum}, atau validasi yang membatasi nilainya.
	 */
	private String status;
	/**
	 * Induk percakapan tempat pesan ini bernaung; boleh {@code null}. Lihat
	 * {@link #getChatConvertation()}.
	 */
	private ChatConvertation chatConvertation;

	/**
	 * Konstruktor tanpa argumen. Wajib ada untuk Hibernate (instansiasi lewat refleksi) dan
	 * dipakai pula oleh jalur CRUD generik berbasis metadata. Tidak melakukan inisialisasi
	 * apa pun selain nilai awal field {@link #tanggal_dirubah}.
	 */
	public ChatMessage() {

	}

	/**
	 * Mengembalikan primary key baris pesan (kolom {@code id}).
	 *
	 * <p>Nilai dibangkitkan database dengan strategi {@code IDENTITY} dan kolomnya ditandai
	 * {@code insertable = false}, sehingga nilai yang disetel manual lewat {@link #setId(Long)}
	 * <b>tidak</b> ikut dikirim pada {@code INSERT} — Hibernate selalu mengambil id hasil
	 * generasi database. Berbeda dengan beberapa entity lain di paket ini, getter ini
	 * <b>tidak</b> punya mekanisme <i>lazy init</i>/nilai cadangan: bila baris belum tersimpan,
	 * hasilnya {@code null}.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key baris pesan. Praktis hanya dipakai Hibernate saat memuat baris;
	 * pengisian manual tidak berpengaruh pada {@code INSERT} (lihat {@link #getId()}).
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan judul/label pesan (kolom {@code nama}, tipe {@code text}), sudah
	 * di-{@code trim()}.
	 *
	 * <p><b>Diverifikasi:</b> hasil {@code trim()} hanya dikembalikan, <b>tidak</b> ditulis balik
	 * ke field — membaca getter ini tidak mengubah keadaan objek dan tidak memicu
	 * <i>dirty checking</i> Hibernate. Konsekuensinya, nilai yang tersimpan di database bisa
	 * tetap mengandung spasi tepi meski pembacanya selalu menerima versi terpangkas; pembanding
	 * yang memakai field langsung (mis. {@link #toString()}) karena itu bisa berbeda hasil.</p>
	 *
	 * @return judul pesan tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel judul/label pesan. Nilai disimpan apa adanya (tanpa {@code trim()}; pemangkasan
	 * baru terjadi saat dibaca lewat {@link #getNama()}).
	 *
	 * @param nama judul pesan; boleh {@code null}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/catatan tambahan pesan (kolom {@code keterangan}, boleh
	 * {@code null}). Tidak di-{@code trim()}, berbeda dengan {@link #getNama()}.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan/catatan tambahan pesan.
	 *
	 * @param keterangan keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan <b>pengirim</b> pesan sebagai teks bebas (kolom {@code from_user}).
	 *
	 * <p>Nama kolom sengaja dibedakan dari nama property karena {@code from} adalah kata kunci
	 * SQL yang tidak bisa dipakai sebagai nama kolom tanpa <i>quoting</i>. Untuk HQL gunakan
	 * nama property {@code from}; untuk SQL native gunakan {@code from_user}.</p>
	 *
	 * <p>Tidak ada <i>foreign key</i> ke {@link Tbmuser}/{@link Mahasiswa}/{@code Dosen}, dan
	 * tidak ada konvensi yang menetapkan apakah isinya {@code userId}, NIM, atau nama tampilan —
	 * karena tidak ada satu pun kode yang pernah mengisinya. Bandingkan dengan {@link Pesan}
	 * yang memakai relasi ber-FK untuk sisi pengirim.</p>
	 *
	 * @return identitas pengirim sebagai teks, atau {@code null} bila belum diisi
	 */
	@Column(name = "from_user")
	public String getFrom() {
		return from;
	}

	/**
	 * Menyetel pengirim pesan. Tidak ada validasi apa pun terhadap format maupun keberadaan
	 * pengguna yang dirujuk.
	 *
	 * @param from identitas pengirim sebagai teks; boleh {@code null}
	 */
	public void setFrom(String from) {
		this.from = from;
	}

	/**
	 * Mengembalikan <b>penerima</b> pesan sebagai teks bebas (kolom {@code to_user}).
	 * Pertimbangan penamaan kolom dan ketiadaan <i>foreign key</i> sama persis dengan
	 * {@link #getFrom()}.
	 *
	 * @return identitas penerima sebagai teks, atau {@code null} bila belum diisi
	 */
	@Column(name = "to_user")
	public String getTo() {
		return to;
	}

	/**
	 * Menyetel penerima pesan. Tanpa validasi format maupun pengecekan keberadaan pengguna.
	 *
	 * @param to identitas penerima sebagai teks; boleh {@code null}
	 */
	public void setTo(String to) {
		this.to = to;
	}

	/**
	 * Mengembalikan kode/penanda bebas pesan.
	 *
	 * <p>Property ini <b>tidak beranotasi {@code @Column}</b>, sehingga dipetakan ke kolom
	 * {@code kode} lewat penamaan default Hibernate. Tidak ada aturan format, keunikan, maupun
	 * pembangkit kode otomatis untuk field ini.</p>
	 *
	 * @return kode pesan, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode/penanda bebas pesan.
	 *
	 * @param kode kode pesan; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan <b>badan pesan</b> — isi percakapan yang sesungguhnya (kolom {@code message},
	 * hasil penamaan default karena tanpa {@code @Column}).
	 *
	 * <p>Inilah satu-satunya field pada entity ini yang benar-benar berisi data pribadi
	 * pengguna. Berbeda dengan {@link #getNama()} yang diberi {@code columnDefinition = "text"},
	 * kolom ini memakai tipe default dialek (umumnya {@code varchar(255)} bila skema
	 * dibangkitkan Hibernate) — batas panjang yang jelas terlalu pendek untuk pesan chat pada
	 * umumnya, dan menjadi indikator lain bahwa rancangan ini tidak pernah diuji pakai.</p>
	 *
	 * @return isi pesan, atau {@code null} bila belum diisi
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Menyetel badan pesan. Tidak ada pembatasan panjang, penyaringan HTML, maupun
	 * <i>escaping</i> di level entity — tanggung jawab itu ada pada lapisan pemanggil (yang saat
	 * ini belum ada).
	 *
	 * @param message isi pesan; boleh {@code null}
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Mengembalikan penanda status pesan (kolom {@code status}, penamaan default).
	 *
	 * <p>Bertipe {@link String} bebas, <b>bukan</b> boolean "sudah dibaca" dan bukan pula
	 * {@code enum}. Tidak ada konstanta status yang didefinisikan di class ini maupun di
	 * {@link ChatConvertation}, dan tidak ada kode yang pernah membaca atau menulisnya — jadi
	 * jangan berasumsi ada kosakata nilai baku (mis. {@code "BARU"}/{@code "DIBACA"}) di
	 * baliknya. Bila entity ini dihidupkan, kosakata status perlu ditetapkan lebih dulu.</p>
	 *
	 * @return status pesan, atau {@code null} bila belum diisi
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Menyetel penanda status pesan. Menerima nilai apa pun tanpa validasi.
	 *
	 * @param status status pesan; boleh {@code null}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan <i>header</i> percakapan tempat pesan ini bernaung (kolom penghubung
	 * {@code chat_convertation}, boleh {@code null}).
	 *
	 * <p>Beberapa catatan pemetaan yang perlu diperhatikan:</p>
	 * <ul>
	 * <li><b>Pengambilan bersifat EAGER.</b> Anotasi {@code @ManyToOne} di sini tidak menyetel
	 * {@code fetch}, sehingga memakai default JPA yaitu {@code EAGER} — setiap kali baris
	 * {@code ChatMessage} dimuat, baris {@link ChatConvertation}-nya ikut dimuat.
	 * {@code @Fetch(FetchMode.SELECT)} menentukan <i>caranya</i>: lewat {@code SELECT} terpisah,
	 * bukan {@code JOIN} pada query yang sama. Kombinasi ini berarti memuat N pesan menghasilkan
	 * N query tambahan (pola N+1). Bandingkan {@link Pesan} yang justru menandai seluruh
	 * relasinya {@code FetchType.LAZY}.</li>
	 * <li><b>Cascade {@code PERSIST} dan {@code MERGE}.</b> Menyimpan sebuah {@code ChatMessage}
	 * ikut menyimpan/menggabungkan {@link ChatConvertation} yang tertaut padanya. Tidak ada
	 * {@code CascadeType.REMOVE}, sehingga menghapus pesan tidak menghapus percakapan
	 * induknya — perilaku yang memang diinginkan.</li>
	 * <li><b>Relasi satu arah.</b> {@link ChatConvertation} tidak punya koleksi balik ke
	 * {@code ChatMessage}, jadi daftar pesan sebuah percakapan hanya bisa diperoleh lewat query
	 * eksplisit terhadap kolom {@code chat_convertation}.</li>
	 * </ul>
	 *
	 * @return header percakapan, atau {@code null} bila pesan tidak dikaitkan ke percakapan
	 *         mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "chat_convertation", nullable = true)
	public ChatConvertation getChatConvertation() {
		return chatConvertation;
	}

	/**
	 * Menyetel <i>header</i> percakapan induk pesan ini. Karena relasinya satu arah, method ini
	 * adalah satu-satunya cara menautkan pesan ke percakapan; tidak ada sisi sebaliknya yang
	 * perlu disinkronkan.
	 *
	 * @param chatConvertation header percakapan; boleh {@code null}
	 */
	public void setChatConvertation(ChatConvertation chatConvertation) {
		this.chatConvertation = chatConvertation;
	}

}
