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
 * Entity penghubung <b>program beasiswa &harr; item biaya tambahan</b> &mdash; satu baris di tabel
 * {@code public.beasiswa_punya_item_biaya_tambahan} menyatakan bahwa satu
 * {@link Beasiswa} menanggung/menambahkan satu {@link ItemBiaya} tertentu, beserta
 * <b>nominal</b>-nya ({@link #getJumlah()}) dan kapan kaitan itu dibuat
 * ({@link #getTanggalDitambahkan()}).
 *
 * <p>Meskipun namanya diawali "BeasiswaPunya...", secara semantik baris ini <b>bukan</b> potongan
 * atau diskon yang otomatis mengurangi tagihan mahasiswa. Satu-satunya kode yang pernah membaca
 * {@link #getJumlah()} untuk keperluan keuangan
 * ({@code ais.action.ws.util.PembayaranUtil#constructDetailBiayaFromBeasiswa}) justru
 * <b>menambahkan</b> baris {@link DetailBiaya} baru senilai {@code jumlah} &mdash; lihat bagian
 * "Kuirk dan catatan penting" di bawah, termasuk fakta bahwa method tersebut saat ini tidak punya
 * pemanggil sama sekali.</p>
 *
 * <h2>Posisi dalam alur beasiswa</h2>
 * <p>Alur beasiswa lengkap didokumentasikan di {@link Beasiswa}; jangan diduplikasi di sini.
 * Ringkasnya, kelas ini adalah tahap terakhir rantai tersebut &mdash; bagian
 * "<b>dampak biaya</b>": setelah program beasiswa didefinisikan ({@link Beasiswa}), mahasiswa
 * mendaftar ({@link ais.database.model.beasiswa.MahasiswaDaftarBeasiswa},
 * {@link PengajuanBeasiswa}) dan penerima ditetapkan ({@link MahasiswaDapatBeasiswa}), barulah
 * kelas ini memetakan program itu ke komponen-komponen biaya yang terpengaruh.</p>
 *
 * <p>Relasi di sini <b>searah dari sisi anak</b>: {@link Beasiswa} tidak mendeklarasikan koleksi
 * {@code @OneToMany} ke kelas ini, sehingga untuk mengambil "semua item biaya tambahan program X"
 * seluruh kode pemanggil selalu menjalankan Criteria sendiri dengan
 * {@code Restrictions.eq("beasiswa", beasiswa)}.</p>
 *
 * <h2>Menggantikan relasi warisan {@code Beasiswa.itemBiayas}</h2>
 * <p>{@link Beasiswa#getItemBiayas()} adalah relasi {@code @ManyToMany} lama lewat tabel perantara
 * polos {@code beasiswa_has_penambahan_item_biaya}. Relasi itu sudah tidak dipakai lagi dan
 * digantikan entity ini, karena tabel perantara polos <b>tidak bisa menyimpan atribut per-kaitan</b>
 * &mdash; sedangkan justru {@code jumlah} dan {@code tanggalDitambahkan} yang menjadi alasan
 * keberadaan tabel ini. Keduanya masih terpetakan berdampingan; lihat catatan pada
 * {@link Beasiswa#getItemBiayas()} sebelum menghapus salah satunya.</p>
 *
 * <h2>Pemetaan Hibernate</h2>
 * <p>{@code @Entity} + {@code @Table(schema = "public", name = "beasiswa_punya_item_biaya_tambahan")},
 * {@code dynamicInsert}/{@code dynamicUpdate} aktif (hanya kolom yang benar-benar berubah ikut
 * dalam {@code INSERT}/{@code UPDATE}), dan {@code @Audited} sehingga setiap perubahan direkam
 * Hibernate Envers ke tabel bayangan {@code beasiswa_punya_item_biaya_tambahan_AUD}. Entity
 * didaftarkan eksplisit di {@code hibernate.cfg.xml}.</p>
 * <p>Pemetaan memakai <b>property access</b> (anotasi menempel pada getter), sehingga
 * <b>setiap pasangan getter/setter yang tidak dianotasi {@code @Transient} tetap dipetakan</b>.
 * Karena {@code ais.database.hibernate.MyNamingStrategy} adalah turunan
 * {@code DefaultNamingStrategy} (nama kolom = nama properti apa adanya, tanpa konversi ke
 * {@code under_score}), properti yang tidak diberi {@code @Column} jatuh ke kolom bernama persis
 * seperti propertinya: {@code jumlah}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah}. Hanya {@code id}, {@code beasiswa}, {@code item_biaya}, dan
 * {@code tanggal_ditambahkan} yang punya nama kolom eksplisit.</p>
 * <p>Kedua relasi ({@link #getBeasiswa()} dan {@link #getItemBiaya()}) memakai
 * {@code @ManyToOne} dengan {@code cascade = {PERSIST, MERGE}} dan
 * {@code @Fetch(FetchMode.SELECT)} &mdash; artinya induknya dimuat lewat query terpisah (bukan
 * {@code JOIN}), dan menyimpan baris ini juga ikut menyimpan/mem-<i>merge</i> objek
 * {@link Beasiswa}/{@link ItemBiaya} yang menempel padanya.</p>
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 * <p>Kelas induk <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}, melainkan POJO
 * abstrak biasa; Hibernate <b>tidak memetakan properti milik induk</b>. Karena itu deklarasi ulang
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini
 * <b>bukan duplikasi yang keliru, melainkan keharusan teknis</b> &mdash; tanpa deklarasi ulang,
 * kolom-kolom tersebut tidak akan pernah dipetakan. Pola yang sama muncul di hampir semua entity
 * repo ini.</p>
 * <p><b>Konsekuensi yang perlu diingat:</b> kelas ini <b>tidak</b> mendeklarasikan ulang
 * {@code nama}, {@code keterangan}, maupun {@code kode}. Ketiganya tetap ada sebagai properti Java
 * (diwarisi dari {@link GeneralValueObject}) sehingga {@code setNama(...)} tidak akan gagal
 * dikompilasi, tetapi nilainya <b>tidak pernah tersimpan ke basis data</b> dan
 * {@link GeneralValueObject#getNama()} pada baris hasil pembacaan ulang selalu {@code null}.
 * Jangan memakai properti-properti itu di sini. Manfaat yang benar-benar diwarisi dari induk
 * adalah kumpulan utilitas statis, terutama {@link GeneralValueObject#check(Object)} untuk
 * resolusi proxy lazy.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 * <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan hook {@code @PreUpdate}
 * {@link #onUpdate()}.</li>
 * <li><b>Identitas</b> &mdash; {@link #getId()}/{@link #setId(Long)} dan
 * {@link #toString()}.</li>
 * <li><b>Relasi</b> &mdash; {@link #getBeasiswa()}/{@link #setBeasiswa(Beasiswa)} dan
 * {@link #getItemBiaya()}/{@link #setItemBiaya(ItemBiaya)}; pasangan keduanya adalah kunci logis
 * baris ini.</li>
 * <li><b>Data kaitan</b> &mdash; {@link #getJumlah()}/{@link #setJumlah(Double)} (nominal) dan
 * {@link #getTanggalDitambahkan()}/{@link #setTanggalDitambahkan(Date)} (stempel waktu
 * pembuatan).</li>
 * </ol>
 * <p>Tidak ada satu pun method bisnis, validasi, atau query statis di kelas ini &mdash; seluruh
 * logikanya (pencarian, pembuatan, penghapusan, pengisian nominal) berada di kode pemanggil.</p>
 *
 * <h2>Verifikasi pola berulang repo ini</h2>
 * <p>Diperiksa langsung terhadap kode kelas ini, bukan diasumsikan dari entity lain:</p>
 * <ul>
 * <li><b>Getter yang menulis balik ke field atau ke basis data:</b> <b>TIDAK ADA</b>. Kedelapan
 * getter kelas ini murni mengembalikan field apa adanya. Ini <b>berbeda</b> dari induknya
 * {@link Beasiswa}, yang {@code getTanggalBuka()}/{@code getTanggalTutup()}-nya justru mengisi
 * field bila masih {@code null} (sekadar merender daftar bisa mengubah data di sana).</li>
 * <li><b>Getter yang membuka atau menutup session Hibernate:</b> <b>TIDAK ADA</b>. Kelas ini tidak
 * menyentuh {@code HibernateUtil} sama sekali.</li>
 * <li><b>Getter destruktif</b> (menghapus baris/relasi saat dibaca): <b>TIDAK ADA</b>.</li>
 * <li><b>Cache statis atau state bersama:</b> <b>TIDAK ADA</b>; satu-satunya anggota statis adalah
 * {@code serialVersionUID}.</li>
 * <li><b>Setter bergerbang:</b> ada, tapi hanya pada pasangan audit {@link #setOleh(String)} dan
 * {@link #setOlehId(String)}, yang <b>menelan diam-diam</b> nilai {@code null}/kosong (pola
 * standar repo ini). Setter lain menerima apa pun tanpa validasi.</li>
 * </ul>
 *
 * <h2>Kuirk dan catatan penting</h2>
 * <ol>
 * <li><b>Komentar generator "{@code Bank generated by hbm2java}" pada berkas asli adalah sisa
 * salin-tempel</b> dan sudah digantikan Javadoc ini. Kelas ini tidak berhubungan dengan
 * {@link Bank}; komentar yang sama membajak puluhan entity lain di repo ini (sumber aslinya memang
 * {@code Bank.java}).</li>
 * <li><b>Seluruh jalur UI dan keuangan entity ini saat ini DORMAN.</b> Penelusuran seluruh pohon
 * sumber menunjukkan:
 * <ul>
 * <li>{@code ais.action.master.BeasiswaAction#initItemBiaya(Beasiswa)} &mdash; satu-satunya layar
 * yang membangun panel "Item Biaya Tambahan", satu-satunya tempat {@code itemGrid} di-instansiasi,
 * dan satu-satunya tempat {@code AmbilDataItemBiayaHelper} dipakai &mdash; <b>tidak pernah
 * dipanggil dari mana pun</b> (method itu sendiri sudah ditandai
 * {@code @SuppressWarnings("unused")}). Akibatnya {@code BeasiswaPunyaItemBiayaTambahanRenderer},
 * {@code AmbilDataItemBiayaHelper}, dan {@code BeasiswaAction#loadData(Object)} praktis ikut
 * menjadi kode mati &mdash; dan seandainya {@code loadData(...)} suatu saat terpanggil dari jalur
 * lain, ia akan melempar {@code NullPointerException} karena {@code itemGrid} belum pernah
 * diisi.</li>
 * <li>{@code ais.action.ws.util.PembayaranUtil#constructDetailBiayaFromBeasiswa(...)} &mdash;
 * satu-satunya konsumen {@link #getJumlah()} &mdash; juga <b>tidak punya pemanggil sama
 * sekali</b>.</li>
 * </ul>
 * Karena itu, dalam kondisi kode saat ini tabel {@code beasiswa_punya_item_biaya_tambahan}
 * tidak bisa diisi maupun dibaca lewat UI mana pun. Ini catatan arsitektural, bukan izin untuk
 * menghapus: seperti banyak entity "yatim" lain di repo ini, baris-barisnya <b>tetap terjangkau
 * penuh lewat endpoint reflektif generik</b> ({@code /Data}, {@code /Api dataRinci}) yang
 * menjangkau semua entity Hibernate terpetakan tanpa peduli ada tidaknya layar UI aktif.</li>
 * <li><b>{@link #getJumlah()} tidak pernah terisi pada saat baris dibuat.</b> Satu-satunya jalur
 * pembuatan ({@code AmbilDataItemBiayaHelper#save()}) hanya memanggil
 * {@link #setBeasiswa(Beasiswa)} dan {@link #setItemBiaya(ItemBiaya)}; nominalnya baru diisi
 * belakangan lewat kotak angka pada {@code BeasiswaPunyaItemBiayaTambahanRenderer}. Setiap baris
 * yang belum pernah disunting manual karena itu ber-{@code jumlah} {@code null} &mdash; dan
 * {@code PembayaranUtil} meneruskan nilai itu apa adanya ke
 * {@code DetailBiaya#setNilaiBiaya(Double)} tanpa penjagaan {@code null}.</li>
 * <li><b>Tidak ada batasan keunikan pasangan ({@code beasiswa}, {@code item_biaya}).</b> Kedua
 * kolom relasi bahkan {@code nullable = true}. Kode pemanggil memperlakukan pasangan itu seolah
 * kunci logis: sebagian query memakai {@code setMaxResults(1).uniqueResult()} (aman), tetapi
 * listener {@code onCheck} pada {@code AmbilDataItemBiayaHelper.ItemBiayaRenderer} memakai
 * {@code uniqueResult()} <b>tanpa</b> {@code setMaxResults(1)} &mdash; akan melempar
 * {@code NonUniqueResultException} bila baris kembar sempat terbentuk.</li>
 * <li><b>Gerbang hak akses tidak lengkap pada panel pengelolanya</b> (relevan bila panel dorman di
 * atas suatu saat diaktifkan kembali). {@code BeasiswaAction} menghitung {@code edit} dan
 * {@code delete} dari {@code CommonPrivilages}, tetapi pada panel item biaya hanya tombol hapus
 * yang benar-benar bergerbang ({@code setVisible(delete)}); tombol "Ambil Item Biaya" dan kotak
 * angka nominal <b>tidak pernah di-{@code setDisabled(!edit)}</b>. Ini instance lain dari pola
 * "inversi hak akses" yang berulang di repo ini: gerbang READ/DELETE ada, gerbang UPDATE
 * nihil.</li>
 * </ol>
 *
 * @see Beasiswa
 * @see ItemBiaya
 * @see MahasiswaDapatBeasiswa
 * @see PengajuanBeasiswa
 * @see ais.database.dao.BeasiswaPunyaItemBiayaTambahanDao
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "beasiswa_punya_item_biaya_tambahan")

public class BeasiswaPunyaItemBiayaTambahan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dipatok tetap agar objek yang sudah ter-serialisasi (mis. di
	 * session ZK atau cache) tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2461822577548439808L;
	/** Kunci primer baris {@code beasiswa_punya_item_biaya_tambahan}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit); lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit); lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor} lewat hook
	 * {@link #onUpdate()}. Dipetakan ke kolom {@code olehId} (nama properti apa adanya, karena
	 * tidak ada {@code @Column} dan naming strategy repo ini tidak mengubah kapitalisasi).</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila baris belum pernah melalui
	 *         jalur yang mengisinya
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Bergerbang:</b> nilai {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b>
	 * &mdash; field lama dipertahankan, tidak ada exception maupun log. Ini pola standar repo ini
	 * agar jejak audit yang sudah ada tidak terhapus oleh proses batch yang tidak membawa
	 * identitas pengguna.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong/berspasi saja
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p><b>Bergerbang</b> dengan aturan yang persis sama seperti {@link #setOlehId(String)}: nilai
	 * {@code null} atau kosong/berspasi saja diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong/berspasi saja
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor} lewat hook
	 * {@link #onUpdate()}. Dipetakan ke kolom {@code oleh}.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b> {@code UPDATE}
	 * dijalankan untuk baris ini, lalu meneruskan objek ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang aktif.
	 *
	 * <p><b>Jangan dipanggil manual</b> &mdash; ini murni callback siklus hidup persistence.
	 * Implementasi wajib atas {@code abstract} {@code onUpdate()} di
	 * {@link GeneralValueObject}.</p>
	 *
	 * <p><b>Perhatikan:</b> hook ini <b>hanya</b> {@code @PreUpdate}, bukan {@code @PrePersist} &mdash;
	 * baris yang baru pertama kali di-{@code INSERT} tidak melewatinya, sehingga {@code oleh}/
	 * {@code olehId} baris baru tetap {@code null} sampai ada penyuntingan berikutnya.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini; lihat {@link #getTanggal_dirubah()}.
	 *
	 * <p>Diinisialisasi ke waktu server saat objek dibuat lewat
	 * {@code ais.ui.util.WaktuUtil#getDate()} (bukan {@code new Date()}, agar mengikuti
	 * zona/offset waktu yang dikonfigurasi aplikasi), lalu ditimpa
	 * {@code AuditTimestampInterceptor} pada setiap {@code UPDATE}.</p>
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya <b>tidak</b> dipanggil kode aplikasi melainkan oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}. Tidak ada penjagaan {@code null}
	 * di sini (berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}), jadi memanggilnya
	 * dengan {@code null} benar-benar mengosongkan kolom auditnya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null} (akan tersimpan sebagai
	 *                        {@code NULL})
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini, disimpan sebagai {@code TIMESTAMP} pada kolom
	 * {@code tanggal_dirubah}.
	 *
	 * <p>Jangan dikacaukan dengan {@link #getTanggalDitambahkan()}: yang ini adalah stempel
	 * <i>perubahan terakhir</i> (jejak audit, terus diperbarui), sedangkan yang itu adalah stempel
	 * <i>pembuatan kaitan</i> (data bisnis, idealnya tidak berubah).</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibuat di
	 *         memori, tetapi bisa {@code null} bila kolomnya memang kosong di basis data
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Label ringkas baris kaitan, berformat <code>&lt;beasiswa&gt;_&lt;item biaya&gt;</code>.
	 *
	 * <p>Merangkai hasil {@link Beasiswa#toString()} (format
	 * <code>nama-tglBuka-tglTutup-instansi</code>) dan {@link ItemBiaya#toString()} (format
	 * <code>id-kode-nama</code>) yang dipisah garis bawah.</p>
	 *
	 * <p><b>Dua hal yang perlu diperhatikan:</b></p>
	 * <ul>
	 * <li>Method ini membaca <b>field langsung</b>, bukan getter. Untuk kelas ini perbedaannya
	 * tidak berdampak (kedua getter relasi murni mengembalikan field), tetapi karena kedua field
	 * bisa berisi <b>proxy lazy</b> Hibernate, merangkainya tetap memicu inisialisasi proxy &mdash;
	 * artinya satu query tambahan per sisi, atau {@code LazyInitializationException} bila dipanggil
	 * di luar session yang masih terbuka.</li>
	 * <li>Tidak ada penjagaan {@code null}: baris dengan {@code beasiswa} atau {@code itemBiaya}
	 * kosong (dimungkinkan, karena kedua kolom {@code nullable = true}) menghasilkan teks yang
	 * memuat kata {@code "null"} apa adanya.</li>
	 * </ul>
	 *
	 * @return label ringkas untuk keperluan tampilan dan log
	 */
	public String toString() {
		return beasiswa + "_" + itemBiaya;
	}

	/** Program beasiswa yang dikaitkan; lihat {@link #getBeasiswa()}. */
	private Beasiswa beasiswa;
	/** Komponen biaya yang dikaitkan; lihat {@link #getItemBiaya()}. */
	private ItemBiaya itemBiaya;
	/**
	 * Waktu kaitan ini dibuat; lihat {@link #getTanggalDitambahkan()}. Diinisialisasi ke waktu
	 * server saat objek dibuat ({@code ais.ui.util.WaktuUtil#getDate()}) dan dalam praktiknya
	 * tidak pernah ditimpa kode pemanggil mana pun.
	 */
	private Date tanggalDitambahkan = ais.ui.util.WaktuUtil.getDate();
	/** Nominal biaya tambahan untuk kaitan ini; lihat {@link #getJumlah()}. */
	private Double jumlah;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate untuk meng-instansiasi entity saat
	 * memuat baris, sekaligus dipakai kode pemanggil untuk membuat kaitan baru
	 * ({@code AmbilDataItemBiayaHelper#save()}).
	 *
	 * <p>Tidak melakukan apa pun selain inisialisasi field bawaan pada deklarasinya
	 * ({@link #tanggal_dirubah} dan {@link #tanggalDitambahkan} terisi waktu server;
	 * {@link #jumlah} tetap {@code null}).</p>
	 */
	public BeasiswaPunyaItemBiayaTambahan() {
	}

	/**
	 * Kunci primer baris ini, dibangkitkan basis data ({@code IDENTITY}/sequence).
	 *
	 * <p>{@code insertable = false} pada {@code @Column} berarti nilai {@code id} <b>tidak pernah</b>
	 * ikut dikirim dalam {@code INSERT}; nilainya dibaca balik setelah baris tersimpan. Karena
	 * berurutan dan bisa ditebak, id ini <b>bukan</b> rahasia dan tidak boleh dipakai sebagai
	 * satu-satunya pelindung akses.</p>
	 *
	 * <p>Dipakai kode pemanggil terutama untuk pengurutan stabil daftar item
	 * ({@code addOrder(Order.asc("id"))}).</p>
	 *
	 * @return kunci primer baris, atau {@code null} bila objek belum pernah tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci primer baris ini.
	 *
	 * <p>Hampir tidak pernah dipanggil kode aplikasi &mdash; nilainya diisi Hibernate setelah
	 * {@code INSERT}. Mengubahnya pada objek yang sudah terkelola session akan membingungkan
	 * Hibernate; gunakan hanya untuk merakit objek detached secara sengaja.</p>
	 *
	 * @param id kunci primer yang akan dipasang
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan program beasiswa yang dikaitkan.
	 *
	 * <p>Karena relasinya ber-{@code cascade = {PERSIST, MERGE}}, objek {@link Beasiswa} yang
	 * dipasang di sini <b>ikut disimpan/di-merge</b> saat baris ini disimpan. Memasang instance
	 * {@link Beasiswa} hasil rakitan sendiri (bukan hasil pembacaan dari session) karena itu bisa
	 * membuat baris {@code beasiswa} baru yang tidak diinginkan.</p>
	 *
	 * <p>Tidak ada validasi: {@code null} diterima apa adanya (kolomnya memang
	 * {@code nullable = true}), dan tidak ada pemeriksaan apakah pasangan
	 * ({@code beasiswa}, {@code itemBiaya}) sudah pernah ada &mdash; pencegahan duplikat
	 * sepenuhnya menjadi tanggung jawab kode pemanggil.</p>
	 *
	 * @param beasiswa program beasiswa yang dikaitkan; boleh {@code null}
	 */
	public void setBeasiswa(Beasiswa beasiswa) {
		this.beasiswa = beasiswa;
	}

	/**
	 * Program beasiswa yang menanggung/menambahkan item biaya ini.
	 *
	 * <p>Dipetakan {@code @ManyToOne} ke kolom FK {@code beasiswa}. {@code @Fetch(FetchMode.SELECT)}
	 * membuat induknya dimuat lewat query terpisah, bukan {@code JOIN} &mdash; sehingga memuat
	 * banyak baris kaitan lalu membaca getter ini per baris menghasilkan pola N+1 query. Nilai yang
	 * dikembalikan bisa berupa <b>proxy lazy</b>; pakai {@link GeneralValueObject#check(Object)}
	 * bila objeknya perlu dipakai di luar session.</p>
	 *
	 * <p>Ini adalah paruh pertama kunci logis baris; seluruh kode pemanggil mencari kaitan dengan
	 * {@code Restrictions.eq("beasiswa", beasiswa)} digabung
	 * {@code Restrictions.eq("itemBiaya", itemBiaya)}.</p>
	 *
	 * @return program beasiswa terkait, atau {@code null} bila kolom FK-nya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "beasiswa", nullable = true)
	public Beasiswa getBeasiswa() {
		return beasiswa;
	}

	/**
	 * Menetapkan komponen biaya yang dikaitkan.
	 *
	 * <p>Berlaku catatan {@code cascade} yang sama seperti {@link #setBeasiswa(Beasiswa)}: objek
	 * {@link ItemBiaya} yang dipasang di sini ikut disimpan/di-merge saat baris ini disimpan.
	 * Tidak ada validasi maupun pencegahan duplikat.</p>
	 *
	 * @param itemBiaya komponen biaya yang dikaitkan; boleh {@code null}
	 */
	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * Komponen biaya ({@link ItemBiaya}) yang ditambahkan oleh program beasiswa ini &mdash; mis.
	 * baris master biaya buku, transport, atau komponen selain SPP.
	 *
	 * <p>Dipetakan {@code @ManyToOne} ke kolom FK {@code item_biaya}, dengan catatan
	 * {@code FetchMode.SELECT} (potensi N+1) dan proxy lazy yang sama seperti
	 * {@link #getBeasiswa()}. Ini paruh kedua kunci logis baris.</p>
	 *
	 * <p>Kode pemanggil membaca {@code kode}/{@code nama}/{@code deskripsi} dari objek ini untuk
	 * label grid <b>tanpa penjagaan {@code null}</b>, sehingga baris dengan FK kosong akan
	 * melempar {@code NullPointerException} saat dirender.</p>
	 *
	 * @return komponen biaya terkait, atau {@code null} bila kolom FK-nya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item_biaya", nullable = true)
	public ItemBiaya getItemBiaya() {
		return itemBiaya;
	}

	/**
	 * Menetapkan waktu pembuatan kaitan ini.
	 *
	 * <p><b>Tidak pernah dipanggil</b> dari mana pun di pohon sumber saat ini &mdash; nilainya
	 * sepenuhnya bergantung pada inisialisasi field pada deklarasinya. Tidak ada penjagaan
	 * {@code null}, padahal kolomnya {@code nullable = false}: memanggil method ini dengan
	 * {@code null} akan menunda kegagalan sampai {@code flush} berupa pelanggaran batasan basis
	 * data, bukan exception di titik pemanggilan.</p>
	 *
	 * @param tanggalDitambahkan waktu pembuatan kaitan
	 */
	public void setTanggalDitambahkan(Date tanggalDitambahkan) {
		this.tanggalDitambahkan = tanggalDitambahkan;
	}

	/**
	 * Waktu kaitan beasiswa&harr;item biaya ini dibuat, disimpan sebagai {@code TIMESTAMP} pada
	 * kolom wajib-isi {@code tanggal_ditambahkan}.
	 *
	 * <p>Nilainya berasal dari inisialisasi field ({@code ais.ui.util.WaktuUtil#getDate()} saat
	 * objek dibuat di memori) dan tidak pernah ditimpa kode pemanggil, sehingga secara praktis
	 * berperan sebagai stempel "dibuat pada". Perlu dicatat bahwa waktunya adalah saat
	 * <b>objek diinstansiasi</b>, bukan saat {@code INSERT} benar-benar tereksekusi &mdash;
	 * perbedaannya bisa terasa bila objek dibuat jauh sebelum disimpan.</p>
	 *
	 * <p>Bandingkan dengan {@link #getTanggal_dirubah()} yang merupakan stempel perubahan terakhir
	 * (jejak audit, terus diperbarui).</p>
	 *
	 * <p>Nilai ini <b>tidak</b> ditampilkan di layar mana pun dan tidak dipakai satu pun query
	 * penyaring; dalam kondisi kode saat ini perannya murni dokumentatif.</p>
	 *
	 * @return waktu pembuatan kaitan; tidak pernah {@code null} pada objek yang baru dibuat di
	 *         memori
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_ditambahkan", nullable = false)
	public Date getTanggalDitambahkan() {
		return tanggalDitambahkan;
	}

	/**
	 * Menetapkan nominal biaya tambahan untuk kaitan ini.
	 *
	 * <p>Satu-satunya pemanggil di pohon sumber adalah listener {@code onChange} kotak angka pada
	 * {@code BeasiswaAction.BeasiswaPunyaItemBiayaTambahanRenderer}, yang langsung menyusulnya
	 * dengan {@code Common.refreshUpdate(session, ...)} &mdash; artinya <b>setiap perubahan angka
	 * di layar langsung tersimpan ke basis data</b>, tanpa tombol simpan dan tanpa konfirmasi.
	 * (Jalur itu sendiri saat ini dorman; lihat Javadoc kelas.)</p>
	 *
	 * <p>Tidak ada validasi sama sekali: nilai negatif, nol, maupun {@code null} diterima apa
	 * adanya.</p>
	 *
	 * @param jumlah nominal biaya tambahan; boleh {@code null}
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Nominal biaya tambahan yang melekat pada kaitan ini, dipetakan ke kolom {@code jumlah}
	 * (tanpa {@code @Column} eksplisit, sehingga nama kolom = nama properti apa adanya).
	 *
	 * <p><b>Sering {@code null}.</b> Jalur pembuatan baris ({@code AmbilDataItemBiayaHelper#save()})
	 * hanya memasang kedua relasi dan tidak pernah memanggil {@link #setJumlah(Double)}, sehingga
	 * kaitan yang belum pernah disunting manual lewat kotak angka di layar akan ber-{@code jumlah}
	 * {@code null}.</p>
	 *
	 * <p><b>Cara nilai ini dipakai (bila jalurnya diaktifkan).</b> Konsumen keuangan satu-satunya
	 * adalah {@code ais.action.ws.util.PembayaranUtil#constructDetailBiayaFromBeasiswa(...)}: untuk
	 * setiap kaitan milik satu beasiswa, method itu membuat baris {@link DetailBiaya} baru yang
	 * menyalin dimensi (angkatan/fakultas/jurusan/program/semester/tahun akademik/dll.) dari salah
	 * satu {@link DetailBiaya} yang sudah ada, memasang {@link #getItemBiaya()} sebagai komponen
	 * biayanya, dan menetapkan {@code nilaiBiaya} = nilai getter ini. Jadi angka di sini adalah
	 * <b>tambahan biaya</b>, bukan potongan. Tiga catatan penting tentang method itu:
	 * <ul>
	 * <li>nilai {@code null} diteruskan apa adanya ke {@code DetailBiaya#setNilaiBiaya(Double)}
	 * tanpa penjagaan;</li>
	 * <li>setiap baris yang dibuatnya diberi nama tetap {@code "Untuk pembayaran Denda"} &mdash;
	 * label salin-tempel yang keliru, karena tidak ada kaitannya dengan denda;</li>
	 * <li>method itu tidak memeriksa apakah baris serupa sudah pernah dibuat, sehingga setiap
	 * pemanggilan menyisipkan baris {@link DetailBiaya} baru (ditandai sufiks
	 * {@code "___BEASISWA"} pada kolom {@code wnaAtauWni}).</li>
	 * </ul>
	 *
	 * @return nominal biaya tambahan, atau {@code null} bila belum pernah diisi
	 */
	public Double getJumlah() {
		return jumlah;
	}

}
