package ais.database.model.sekolah;

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
import ais.database.model.Tbmuser;

/**
 * Entity penghubung antara satu <b>aturan diskon biaya sekolah</b> ({@link DiskonSiswa}) dengan
 * satu <b>penerima diskon</b>, yaitu seorang {@link Siswa} atau seorang {@link CalonSiswa}.
 *
 * <h2>Arti nama kelas (terverifikasi dari kode, bukan dugaan)</h2>
 * <p>Nama {@code DiskonSiswaPunyaSiswa} mengikuti konvensi penamaan tabel-jembatan di repo ini,
 * yaitu <i>&lt;induk&gt;Punya&lt;anggota&gt;</i>: <b>{@code DiskonSiswa}</b> (aturan diskon)
 * <i>punya</i> <b>{@code Siswa}</b> (penerima). Jadi kelas ini <b>BUKAN</b> relasi antara dua
 * siswa (mis. kakak-adik kembar atau siswa &quot;rujukan&quot;), melainkan relasi
 * many-to-many antara aturan diskon dan siswa penerimanya. Kekerabatan antar-siswa memang
 * menjadi <i>alasan</i> lahirnya sebagian baris (lihat daftar jenis di bawah), tetapi identitas
 * saudara/orang tua/alumni pembanding hanya disimpan sebagai teks bebas pada
 * {@link #getKeterangan()} &mdash; tidak ada relasi kedua ke tabel {@code siswa}.</p>
 *
 * <p>Kedua relasi ke sisi penerima ({@link #getSiswa()} dan {@link #getCalonSiswa()}) adalah
 * dua <i>alternatif</i> untuk satu peran yang sama, bukan dua peran berbeda:</p>
 * <ul>
 *   <li>{@link #getSiswa()} &mdash; penerima yang sudah berstatus siswa aktif (jalur
 *       {@code AmbilDataSiswaForDiskonSiswaHelper}, dari layar Siswa);</li>
 *   <li>{@link #getCalonSiswa()} &mdash; penerima yang masih berstatus pendaftar PSB (jalur
 *       {@code AmbilDataCalonCalonSiswaForDiskonSiswaHelper}, dari layar Calon Siswa).</li>
 * </ul>
 * <p>Keduanya {@code nullable}. Saat seorang calon siswa akhirnya diterima dan berubah menjadi
 * siswa, {@link #getSiswa()} <b>mengisi sendiri</b> field {@code siswa} dari
 * {@code calonSiswa.getSiswa()} sehingga baris yang semula hanya menunjuk calon siswa otomatis
 * ikut menunjuk siswa (lihat catatan efek samping pada method tersebut).</p>
 *
 * <h2>Jenis diskon yang memakai entity ini (terverifikasi)</h2>
 * <p>Daftar jenis diambil dari konstanta {@code DiskonSiswa.JENIS}, dan generator baris massal
 * yang mengisinya adalah method statis di {@link DiskonSiswa} yang dipicu tombol
 * &quot;Sinkronkan Penerima&quot; pada {@code DiskonSiswaPunyaSiswaHelper}:</p>
 * <ul>
 *   <li>{@code Diskon Alumni} &rarr; {@code DiskonSiswa.prosesAlumni(..)} &mdash; siswa yang
 *       pernah bersekolah di sekolah yang sama pada angkatan lebih awal (dicocokkan
 *       nama&nbsp;+&nbsp;tanggal lahir);</li>
 *   <li>{@code Diskon Anak Alumni} &rarr; {@code DiskonSiswa.prosesAnakAlumni(..)} &mdash; siswa
 *       yang nama&nbsp;+&nbsp;tanggal lahir ayah/ibunya cocok dengan alumni yang sudah lulus;</li>
 *   <li>{@code Diskon Saudara} &rarr; {@code DiskonSiswa.prosesSaudara(..)} &mdash; dua siswa
 *       aktif atau lebih dengan nama&nbsp;+&nbsp;tanggal lahir ayah/ibu yang identik (inilah
 *       jalur untuk kakak-adik, termasuk kembar);</li>
 *   <li>{@code Diskon Saudara Alumni} &rarr; {@code DiskonSiswa.prosesSaudaraAlumni(..)};</li>
 *   <li>{@code Diskon Anak Pegawai Tetap} / {@code Diskon Anak Pegawai Honorer} &rarr;
 *       {@code DiskonSiswa.prosesAnakPegawai(.., TipeMasaKerja)} &mdash; anak karyawan yayasan;</li>
 *   <li>{@code Diskon Semua Siswa} &rarr; {@code DiskonSiswa.prosesSemua(..)} &mdash; seluruh
 *       calon siswa pada gelombang PSB tahun ajaran terkait.</li>
 * </ul>
 * <p><b>Tidak ada</b> jenis &quot;beasiswa yatim&quot; pada daftar ini; dugaan semacam itu tidak
 * didukung kode.</p>
 *
 * <h2>Status HIDUP atau YATIM &mdash; HASIL VERIFIKASI: <b>HIDUP</b></h2>
 * <p>Berbeda dengan sebagian anggota keluarga {@code ItemBiayaPunya*} yang ternyata
 * <i>write-only</i>, entity ini benar-benar dibaca oleh mesin tagihan. Dua pembaca independen
 * terkonfirmasi, keduanya <b>menyaring {@code setujui = TRUE}</b>:</p>
 * <ol>
 *   <li><b>{@code TagihanDiskonSiswaHelper.hitungDiskon(..)}</b> &mdash; jalur otomatis. Untuk
 *       setiap {@link Tagihan} dicari baris {@code DiskonSiswaPunyaSiswa} milik siswa/calon
 *       siswa tagihan tersebut, pada tahun ajaran yang sama, yang aturan diskonnya aktif dan
 *       mencantumkan item biaya tagihan tersebut. Nilai potongan diambil dari
 *       {@link DiskonSiswaItemBiaya} lalu ditulis ke {@code tagihan.diskon},
 *       {@code tagihan.diskonTidakLangsung}, dan {@code tagihan.diskonSiswa}.</li>
 *   <li><b>{@code DiskonSiswaSyncHelper.sinkronkan(..)}</b> &mdash; jalur manual (tombol
 *       &quot;Singkronkan Tagihan&quot; di {@code DiskonSiswaAction} dan di layar penerima).
 *       Berangkat dari aturan diskon, mengambil <i>seluruh</i> penerima yang {@code setujui},
 *       lalu memperbarui tagihan tiap penerima per item biaya.</li>
 * </ol>
 * <p>Karena itu baris pada tabel ini punya <b>dampak finansial nyata</b>: mencentang
 * {@link #getSetujui()} langsung mengubah nominal tagihan siswa.</p>
 *
 * <h2>Kanal penulis (5 jalur)</h2>
 * <ol>
 *   <li>{@code AmbilDataSiswaForDiskonSiswaHelper.save()} &mdash; dialog pemilihan siswa massal;
 *       mengisi {@link #setOleh(String)}, {@link #setTbmuser(Tbmuser)}, dan
 *       {@link #setDiubahDari(String)} dengan {@code "SiswaAction"}.</li>
 *   <li>{@code AmbilDataCalonCalonSiswaForDiskonSiswaHelper.save()} &mdash; idem untuk calon
 *       siswa; {@link #setDiubahDari(String)} diisi {@code "CalonSiswaAction"}.</li>
 *   <li>{@code DiskonSiswa.proses*(..)} &mdash; tujuh generator massal di atas; hanya mengisi
 *       {@code diskonSiswa}, {@code siswa}/{@code calonSiswa}, dan {@code keterangan}
 *       (<b>tanpa</b> {@code diubahDari}/{@code tbmuser}).</li>
 *   <li>{@code DiskonSiswaPunyaSiswaHelper} &mdash; sunting {@code keterangan} inline, centang
 *       {@code setujui}, dan hapus baris.</li>
 *   <li>Unggah Excel massal lewat {@code Common.uploadData(this, DiskonSiswaPunyaSiswa.class, ..)}
 *       pada helper yang sama (kolom {@code id}, {@code diskonSiswa}, {@code siswa},
 *       {@code calonSiswa}, {@code keterangan}, {@code setujui}).</li>
 * </ol>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Jejak audit warisan</b> (dideklarasikan ulang &mdash; lihat catatan
 *       {@code GeneralValueObject} di bawah): {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, dan callback {@link #onUpdate()}.</li>
 *   <li><b>Relasi inti</b>: {@link #getDiskonSiswa()} (wajib), {@link #getSiswa()} dan
 *       {@link #getCalonSiswa()} (dua alternatif penerima), {@link #getTbmuser()} (pencatat).</li>
 *   <li><b>Atribut</b>: {@link #getKeterangan()} (alasan diskon, teks bebas),
 *       {@link #getSetujui()} (gerbang persetujuan yang dibaca mesin tagihan), dan
 *       {@link #getDiubahDari()} (jejak layar asal).</li>
 *   <li><b>Utilitas</b>: {@link #toString()} dan konstruktor tanpa argumen.</li>
 * </ul>
 * <p>Kelas ini <b>tidak</b> memiliki method query statis, tidak seperti induknya
 * {@link DiskonSiswa} yang memuat seluruh generator {@code proses*}.</p>
 *
 * <h2>Kuirk dan hal non-obvious</h2>
 * <ul>
 *   <li><b>Nama tabel bersufiks {@code _baru}</b>: {@code sekolah.diskon_siswa_punya_siswa_baru}.
 *       Nama kelas tidak menyebut &quot;baru&quot;, sehingga pencarian berbasis nama tabel lama
 *       ({@code diskon_siswa_punya_siswa}) tidak akan menemukan data yang dipakai runtime.</li>
 *   <li><b>{@link #getSiswa()} bersifat mutatif</b> &mdash; menulis balik field {@code siswa}.
 *       Karena pemetaan Hibernate di sini berbasis <i>property access</i> ditambah
 *       {@code dynamicUpdate}, nilai hasil tulis-balik itu ikut ter-flush ke kolom FK
 *       {@code siswa}. Ini adalah mekanisme yang membuat baris PSB otomatis &quot;naik kelas&quot;
 *       menjadi baris siswa; sekaligus berarti membaca getter dapat mengubah data.</li>
 *   <li><b>Biaya tersembunyi {@link #getSiswa()}</b>: fallback-nya memanggil
 *       {@code CalonSiswa.getSiswa()}, yang bila calon siswa sudah diterima namun belum tertaut
 *       akan <i>memindai seluruh cache {@code Siswa}</i> dan mencocokkan secara heuristik
 *       (nama&nbsp;+&nbsp;tahun masuk&nbsp;+&nbsp;tanggal lahir). Salah cocok pada heuristik itu
 *       berarti diskon menempel pada siswa yang keliru.</li>
 *   <li><b>{@link #getTbmuser()} bersifat destruktif</b> &mdash; mengembalikan {@code null} bila
 *       pencatatnya adalah akun siswa, dan (property access + {@code dynamicUpdate}) nilai
 *       {@code null} itu ditulis ke kolom FK {@code tbmuser} pada penyimpanan berikutnya.
 *       Instance berulang dari pola yang sama pada {@code OrganisasiDosenPunyaDosen} dan
 *       {@code KegiatanKedosenanPunyaDosen}.</li>
 *   <li><b>{@link #toString()} juga mutatif</b> dan membaca {@code diskonSiswa} lewat field
 *       langsung tanpa {@code check(..)}, sehingga berisiko {@code LazyInitializationException}
 *       pada object yang sudah <i>detached</i>.</li>
 *   <li><b>{@link #getSetujui()} menormalkan {@code null} menjadi {@code false}</b>. Karena
 *       baris hasil generator {@code DiskonSiswa.proses*} tidak pernah menyetel {@code setujui},
 *       Hibernate menyimpan {@code false} lewat getter ini &mdash; artinya penerima hasil
 *       penjaringan otomatis <b>tidak</b> langsung mendapat potongan; harus dicentang manual
 *       lebih dulu. Ini gerbang persetujuan yang disengaja, bukan bug.</li>
 *   <li><b>Ketidakselarasan dua mesin diskon</b>: {@code TagihanDiskonSiswaHelper.hitungDiskon(..)}
 *       memasang {@code addOrder(Order.desc("id")).setMaxResults(1)} pada kriterianya, sehingga
 *       hanya <b>satu</b> baris (yang terbaru) yang pernah diproses walaupun kode di bawahnya
 *       berbentuk loop akumulasi. Siswa yang berhak atas beberapa aturan diskon sekaligus hanya
 *       menerima aturan terakhir lewat jalur otomatis, sementara jalur manual
 *       {@code DiskonSiswaSyncHelper} memproses semua aturan (masing-masing menimpa nilai diskon
 *       tagihan, bukan menjumlahkan).</li>
 *   <li><b>Import tak terpakai</b>: {@code TagihanUtil} dan {@code TagihanUtilCalonSiswa}
 *       meng-import kelas ini tetapi tidak pernah memakainya (perhitungan diskon sudah
 *       dipindahkan ke {@code TagihanDiskonSiswaHelper}).</li>
 *   <li><b>Terdaftar sebagai kelas yang tidak dibersihkan</b> pada
 *       {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN}, sehingga instance-nya bertahan di cache dan
 *       sering ditemui dalam keadaan <i>detached</i>. Juga terdaftar di
 *       {@code InitData.initClasses(..)} untuk pemanasan cache awal; tidak ada baris bawaan yang
 *       di-seed &mdash; seluruh isi tabel berasal dari lima kanal penulis di atas.</li>
 *   <li><b>Lampiran</b>: berkas bukti bernama kategori &quot;Surat Dapat Diskon&quot; dilekatkan
 *       lewat {@code LampiranLain} dengan kunci {@code (id, DiskonSiswaPunyaSiswa.class)}.</li>
 * </ul>
 *
 * <h2>Catatan keamanan</h2>
 * <ul>
 *   <li>Penyaring &quot;orang tua hanya melihat anaknya&quot; pada
 *       {@code DiskonSiswaPunyaSiswaHelper.initCriteria(..)} bersifat <b>fail-open</b>: filter
 *       hanya dipasang bila {@code tbmuser.getOrangTua().ambilAnakSiswa()} tidak kosong. Bila
 *       koleksi anak kosong, seluruh penerima diskon (nama, NIS/NISN, sekolah, dan keterangan
 *       yang memuat nama saudara / nama orang tua / status kepegawaian orang tua) ikut tampil.
 *       Pola identik dengan temuan yang sudah tercatat pada audit akses lintas-tenant.</li>
 *   <li>{@code DiskonSiswaAction.initCriteria(..)} menyaring sekolah/yayasan <b>hanya</b> bila
 *       pengguna memilihnya sendiri pada combobox pencarian; pilihan &quot;Semua&quot;
 *       menghasilkan {@code 1=1}. Sub-query pencarian berdasarkan nama/NIS siswa pada method
 *       yang sama juga tidak memiliki batasan sekolah/yayasan, dan tombol ekspor
 *       &quot;Download Diskon Calon Siswa&quot; mengekspor kolom identitas penerima mengikuti
 *       kriteria tersebut.</li>
 *   <li>Persetujuan diskon hanya dijaga privilese generik {@code CommonPrivilages.UPDATE} pada
 *       modul ini; tidak ada peran khusus &quot;penyetuju diskon&quot;, padahal centang tersebut
 *       berdampak langsung pada nominal tagihan.</li>
 * </ul>
 *
 * <h2>Catatan pewarisan {@code GeneralValueObject}</h2>
 * <p>{@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate tidak memetakan
 * properti miliknya. Karena itu deklarasi ulang {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>bukan duplikasi yang keliru</b>, melainkan keharusan
 * teknis agar kolom-kolom tersebut ikut terpetakan. Method {@code check(..)} yang dipakai
 * getter-getter relasi di bawah diwarisi dari kelas induk tersebut.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see DiskonSiswa
 * @see DiskonSiswaItemBiaya
 * @see Siswa
 * @see CalonSiswa
 * @see Tagihan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "diskon_siswa_punya_siswa_baru")
public class DiskonSiswaPunyaSiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja <b>sama persis</b> dengan milik
	 * {@link DiskonSiswa} ({@code 2463821577548439808L}) karena kedua kelas lahir dari generator
	 * hbm2java pada berkas pemetaan yang sama; kesamaan ini tidak berpengaruh apa pun karena
	 * {@code serialVersionUID} hanya dibandingkan antar-versi kelas yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer baris, dipetakan ke kolom {@code id} bertipe {@code IDENTITY} (berurutan). */
	private Long id;

	/** Nama/identitas tampil pengguna yang terakhir menyimpan baris ini. */
	private String oleh;

	/** Id teknis pengguna yang terakhir menyimpan baris ini. */
	private String olehId;

	/**
	 * Mengembalikan id teknis pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id teknis pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> setter ini <i>menolak diam-diam</i> nilai {@code null} maupun
	 * string kosong/spasi &mdash; nilai lama dipertahankan. Akibatnya jejak audit tidak dapat
	 * dikosongkan kembali setelah pernah terisi.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan diam-diam.
	 *
	 * <p>Dipanggil eksplisit oleh {@code AmbilDataSiswaForDiskonSiswaHelper.save()} dan
	 * {@code AmbilDataCalonCalonSiswaForDiskonSiswaHelper.save()} dengan
	 * {@code Common.getCurrentUser().getUserId()}; pada jalur lain pengisiannya diserahkan ke
	 * {@link #onUpdate()}.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@link javax.persistence.PreUpdate}: dijalankan Hibernate tepat sebelum
	 * pernyataan {@code UPDATE} baris ini dikirim ke database.
	 *
	 * <p>Isinya mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang memperbarui
	 * {@link #tanggal_dirubah} beserta {@link #oleh}/{@link #olehId} dari konteks pengguna yang
	 * sedang aktif. Merupakan implementasi method {@code abstract} milik
	 * {@link GeneralValueObject}, sehingga setiap entity turunan wajib menyediakannya.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah state instance ini sesaat sebelum flush. Tidak pernah
	 * dipanggil langsung oleh kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini. Diinisialisasi ke waktu <b>pembuatan object</b>
	 * lewat {@code ais.ui.util.WaktuUtil.getDate()} (mengikuti zona waktu aplikasi, bukan
	 * {@code new Date()} polos), lalu diperbarui {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi apa pun.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada instance baru karena field
	 *         sudah diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam bentuk
	 * {@code "<aturan diskon> - <siswa> - <calon siswa>"}.
	 *
	 * <p><b>Efek samping (non-obvious):</b> method ini <i>bukan</i> pembaca murni. Ia memanggil
	 * {@link #getSiswa()} dan {@link #getCalonSiswa()} lalu menugaskan hasilnya kembali ke field
	 * {@code siswa}/{@code calonSiswa}, sehingga ikut memicu seluruh efek samping getter tersebut
	 * (resolusi proxy, tulis-balik FK {@code siswa}, dan pencocokan heuristik
	 * {@code CalonSiswa.getSiswa()}).</p>
	 *
	 * <p><b>Risiko:</b> bagian {@code diskonSiswa} dibaca dari field langsung <b>tanpa</b>
	 * {@code check(..)}, sehingga pemanggilan pada object yang sudah <i>detached</i> dapat
	 * melempar {@code LazyInitializationException} &mdash; termasuk saat dipakai untuk pesan log
	 * atau debugging.</p>
	 *
	 * @return gabungan teks aturan diskon, siswa, dan calon siswa (bagian yang {@code null}
	 *         tercetak sebagai {@code "null"})
	 */
	public String toString() {
		siswa = getSiswa();
		calonSiswa = getCalonSiswa();
		return diskonSiswa + " - " + siswa + " - " + calonSiswa;
	}

	/** Aturan diskon induk; wajib terisi (kolom {@code diskon_siswa} {@code NOT NULL}). */
	private DiskonSiswa diskonSiswa;

	/** Penerima diskon berstatus siswa aktif; {@code null} bila penerimanya masih calon siswa. */
	private Siswa siswa;

	/** Penerima diskon berstatus pendaftar PSB; {@code null} bila baris dibuat dari sisi siswa. */
	private CalonSiswa calonSiswa;

	/**
	 * Jejak nama layar asal perubahan ({@code "SiswaAction"} atau {@code "CalonSiswaAction"}).
	 * Hanya diisi oleh dua dialog pemilihan massal; baris hasil generator
	 * {@code DiskonSiswa.proses*} membiarkannya {@code null}.
	 */
	private String diubahDari;

	/** Akun pengguna pencatat baris ini; lihat kuirk destruktif pada {@link #getTbmuser()}. */
	private Tbmuser tbmuser;

	/**
	 * Alasan pemberian diskon dalam bentuk teks bebas, mis. {@code "A,B adalah saudara"},
	 * {@code "Merupakan anak dari karyawan <nama>"}, atau
	 * {@code "Alumni <sekolah> angkatan <tahun> lulus tahun <tahun>"}. Dihasilkan otomatis oleh
	 * generator dan dapat disunting langsung dari grid penerima.
	 */
	private String keterangan;

	/**
	 * Gerbang persetujuan. Hanya baris dengan nilai {@code TRUE} yang dibaca mesin tagihan
	 * ({@code TagihanDiskonSiswaHelper} dan {@code DiskonSiswaSyncHelper}).
	 */
	private Boolean setujui;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate untuk instansiasi entity. Seluruh
	 * relasi dan atribut dibiarkan {@code null}; pengisian dilakukan pemanggil lewat setter.
	 */
	public DiskonSiswaPunyaSiswa() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@code IDENTITY}), sehingga bersifat berurutan dan mudah ditebak &mdash; relevan untuk
	 * penilaian risiko enumerasi pada lampiran &quot;Surat Dapat Diskon&quot; yang berkunci id
	 * ini.</p>
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
	 * Menyetel kunci primer baris ini. Hanya dipakai Hibernate dan jalur impor massal; kode
	 * aplikasi biasa tidak perlu memanggilnya.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan aturan diskon induk baris ini, setelah diresolusi dari kemungkinan proxy
	 * lazy lewat {@code check(..)} warisan {@link GeneralValueObject}.
	 *
	 * <p>Nilai ini yang menentukan besaran potongan (lewat {@link DiskonSiswaItemBiaya}),
	 * mode persen atau nominal ({@code DiskonSiswa.getMenggunkanPersen()}), tahun ajaran, item
	 * biaya yang dipotong, serta apakah diskon memotong tagihan langsung
	 * ({@code DiskonSiswa.getMemotongTagihan()}).</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(..)} ditulis balik ke field {@code diskonSiswa}
	 * (mengganti proxy dengan instance kanonik), bukan sekadar dikembalikan.</p>
	 *
	 * @return aturan diskon induk; secara skema tidak boleh {@code null} pada baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diskon_siswa", nullable = false)
	public DiskonSiswa getDiskonSiswa() {
		diskonSiswa = check(diskonSiswa);
		return diskonSiswa;
	}

	/**
	 * Menyetel aturan diskon induk. Dipanggil seluruh kanal penulis sebelum menyimpan.
	 *
	 * @param diskonSiswa aturan diskon induk
	 */
	public void setDiskonSiswa(DiskonSiswa diskonSiswa) {
		this.diskonSiswa = diskonSiswa;
	}

	/**
	 * Mengembalikan penerima diskon berstatus siswa aktif, dengan <b>fallback dan tulis-balik</b>
	 * dari sisi calon siswa.
	 *
	 * <p>Alur lengkapnya:</p>
	 * <ol>
	 *   <li>Bila field {@code siswa} masih {@code null}, {@link #getCalonSiswa()} dipanggil; bila
	 *       calon siswa itu sudah memiliki siswa padanan ({@code calonSiswa.getSiswa() != null}),
	 *       nilai tersebut <b>ditulis ke field {@code siswa}</b>.</li>
	 *   <li>Bila tidak ada padanan, field tetap diresolusi lewat {@code check(..)} (hasilnya
	 *       tetap {@code null}).</li>
	 *   <li>Bila field sudah terisi, cukup diresolusi lewat {@code check(..)}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping penting:</b> karena pemetaan berbasis <i>property access</i> dan entity
	 * memakai {@code dynamicUpdate}, tulis-balik pada langkah 1 ikut ter-flush ke kolom FK
	 * {@code siswa}. Inilah mekanisme yang membuat baris diskon calon siswa otomatis berlaku
	 * untuk siswa yang bersangkutan setelah ia resmi diterima, tanpa proses migrasi terpisah.
	 * Konsekuensinya, sekadar <i>membaca</i> getter ini dapat mengubah data.</p>
	 *
	 * <p><b>Biaya tersembunyi:</b> {@code CalonSiswa.getSiswa()} tidak selalu murah &mdash; bila
	 * calon siswa berstatus diterima namun belum tertaut, method itu memindai seluruh cache
	 * {@code Siswa} dan mencocokkan secara heuristik berdasarkan nama, tahun masuk, dan tanggal
	 * lahir. Kesalahan pencocokan berarti diskon menempel pada siswa yang keliru.</p>
	 *
	 * <p>Dipakai mesin tagihan lewat kriteria {@code dpsSiswa.id} pada
	 * {@code TagihanDiskonSiswaHelper.hitungDiskon(..)} dan langsung lewat getter ini pada
	 * {@code DiskonSiswaSyncHelper.sinkronkanPenerima(..)}.</p>
	 *
	 * @return siswa penerima diskon, atau {@code null} bila penerima masih berupa calon siswa
	 *         yang belum punya padanan siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {

		if (siswa == null) {
			calonSiswa = getCalonSiswa();
			if (calonSiswa != null && calonSiswa.getSiswa() != null) {
				siswa = calonSiswa.getSiswa();
			} else {
				siswa = check(siswa);
			}
		} else {
			siswa = check(siswa);
		}

		return siswa;
	}

	/**
	 * Menyetel penerima diskon berstatus siswa aktif. Tanpa validasi; boleh {@code null} bila
	 * penerima dicatat lewat {@link #setCalonSiswa(CalonSiswa)}.
	 *
	 * @param siswa siswa penerima diskon
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan akun pengguna yang mencatat baris ini &mdash; <b>getter destruktif</b>.
	 *
	 * <p>Setelah resolusi proxy lewat {@code check(..)}, method ini mengembalikan {@code null}
	 * bila akun tersebut ternyata milik seorang siswa ({@code tbmuser.getSiswa() != null}),
	 * agar identitas akun siswa tidak ikut tampil sebagai pencatat. Namun karena pemetaan
	 * berbasis <i>property access</i> ditambah {@code dynamicUpdate}, nilai {@code null} yang
	 * dikembalikan itulah yang dibaca Hibernate saat flush berikutnya, sehingga kolom FK
	 * {@code tbmuser} <b>ikut dikosongkan permanen</b> di database.</p>
	 *
	 * <p>Perhatikan pula bahwa field {@code tbmuser} sendiri tetap menyimpan referensi aslinya;
	 * yang di-null-kan hanya nilai kembalian &mdash; sehingga hasil {@link #getTbmuser()} dan isi
	 * field dapat berbeda dalam satu instance yang sama.</p>
	 *
	 * <p>Instance berulang dari pola yang sudah tercatat pada {@code OrganisasiDosenPunyaDosen},
	 * {@code OrganisasiIntraKampusPunyaMahasiswa}, dan {@code KegiatanKedosenanPunyaDosen}.</p>
	 *
	 * @return akun pencatat, atau {@code null} bila belum terisi <i>atau</i> bila akun tersebut
	 *         adalah akun siswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser != null && tbmuser.getSiswa() != null ? null : tbmuser;
	}

	/**
	 * Menyetel akun pengguna pencatat baris ini. Hanya diisi oleh dua dialog pemilihan massal
	 * ({@code AmbilDataSiswaForDiskonSiswaHelper} dan
	 * {@code AmbilDataCalonCalonSiswaForDiskonSiswaHelper}) dengan
	 * {@code Common.getCurrentUser()}.
	 *
	 * @param tbmuser akun pengguna pencatat
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan nama layar asal perubahan terakhir.
	 *
	 * <p>Nilainya berupa {@code Class.getSimpleName()} dari layar pemanggil, praktis hanya
	 * {@code "SiswaAction"} atau {@code "CalonSiswaAction"}. Tidak dibaca kode mana pun saat ini
	 * (murni jejak forensik), dan tetap {@code null} pada baris hasil generator
	 * {@code DiskonSiswa.proses*}.</p>
	 *
	 * <p>Tidak diberi anotasi kolom, sehingga dipetakan Hibernate ke kolom bawaan
	 * {@code diubahDari}.</p>
	 *
	 * @return nama layar asal, atau {@code null} bila tidak tercatat
	 */
	public String getDiubahDari() {
		return diubahDari;
	}

	/**
	 * Menyetel nama layar asal perubahan. Tanpa validasi.
	 *
	 * @param diubahDari nama sederhana kelas layar asal
	 */
	public void setDiubahDari(String diubahDari) {
		this.diubahDari = diubahDari;
	}

	/**
	 * Mengembalikan alasan pemberian diskon dalam bentuk teks bebas.
	 *
	 * <p>Diisi otomatis oleh generator di {@link DiskonSiswa}, dengan format yang berbeda per
	 * jenis diskon, mis. {@code "<nama-nama> adalah saudara"},
	 * {@code "<nama-nama> adalah saudara alumni <nama>"},
	 * {@code "Merupakan anak dari karyawan <nama>"},
	 * {@code "Merupakan anak dari alumni <nama>"}, atau
	 * {@code "Alumni <sekolah> angkatan <tahun> lulus tahun <tahun>"}. Dapat disunting langsung
	 * dari grid penerima ({@code DiskonSiswaPunyaSiswaHelper}) dan tersimpan otomatis saat
	 * berubah.</p>
	 *
	 * <p><b>Catatan privasi:</b> isi teks ini kerap memuat nama saudara kandung, nama orang tua,
	 * dan status kepegawaian orang tua &mdash; data yang lebih sensitif daripada sekadar nama
	 * siswa, dan ikut terbawa pada ekspor Excel penerima diskon.</p>
	 *
	 * <p><b>Kontrak:</b> berbeda dengan {@code GeneralValueObject}, override ini dapat
	 * mengembalikan {@code null} (instance berulang pola &quot;getKeterangan() membalik
	 * kontrak&quot;). Kolom dipetakan {@code columnDefinition = "text"} sehingga panjangnya tidak
	 * dibatasi.</p>
	 *
	 * @return alasan pemberian diskon, atau {@code null} bila tidak diisi
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel alasan pemberian diskon. Tanpa validasi maupun pemangkasan panjang.
	 *
	 * @param keterangan teks alasan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status persetujuan penerima diskon ini, dengan normalisasi {@code null}
	 * menjadi {@code false}.
	 *
	 * <p><b>Ini adalah gerbang finansial entity ini.</b> Kedua mesin diskon menyaring
	 * {@code Restrictions.eq("setujui", Boolean.TRUE)}, sehingga baris yang belum dicentang tidak
	 * pernah memotong tagihan. Karena generator massal {@code DiskonSiswa.proses*} tidak pernah
	 * memanggil {@link #setSetujui(Boolean)}, Hibernate membaca getter ini saat penyimpanan dan
	 * menuliskan {@code false} &mdash; penjaringan otomatis menghasilkan kandidat, bukan diskon
	 * yang langsung berlaku.</p>
	 *
	 * <p>Konsekuensi lain dari normalisasi ini: nilai {@code null} tidak pernah bertahan di
	 * database setelah baris pernah tersimpan ulang, sehingga penyaring berbasis {@code TRUE}
	 * di atas aman dari perilaku tiga-nilai SQL.</p>
	 *
	 * <p>Di UI, centang ini dirender {@code DiskonSiswaPunyaSiswaHelper.DetailDiskonSiswaRenderer}
	 * dan hanya aktif bila pengguna memiliki privilese {@code CommonPrivilages.UPDATE}.</p>
	 *
	 * @return {@code true} bila diskon sudah disetujui, {@code false} bila belum atau belum
	 *         pernah diisi
	 */
	public Boolean getSetujui() {
		return setujui == null ? false : setujui;
	}

	/**
	 * Menyetel status persetujuan penerima diskon.
	 *
	 * <p>Dipanggil dari listener {@code onCheck} checkbox &quot;Setujui&quot; pada grid penerima,
	 * yang langsung menyimpan baris. Setelah itu tagihan terkait baru ikut berubah pada
	 * sinkronisasi berikutnya (tombol &quot;Singkronkan Tagihan&quot; atau perhitungan otomatis
	 * {@code TagihanDiskonSiswaHelper}).</p>
	 *
	 * @param setujui status persetujuan baru; {@code null} akan dibaca sebagai {@code false} oleh
	 *                {@link #getSetujui()}
	 */
	public void setSetujui(Boolean setujui) {
		this.setujui = setujui;
	}

	/**
	 * Mengembalikan penerima diskon berstatus pendaftar PSB, setelah diresolusi dari kemungkinan
	 * proxy lazy lewat {@code check(..)}.
	 *
	 * <p>Terisi pada baris yang dibuat dari layar Calon Siswa
	 * ({@code AmbilDataCalonCalonSiswaForDiskonSiswaHelper}) maupun dari generator
	 * {@code DiskonSiswa.prosesSemua(..)}, {@code prosesAlumni(..)}, dan
	 * {@code prosesSaudara*(..)} pada cabang calon siswa. Dipakai mesin tagihan lewat kriteria
	 * {@code dpsCalonSiswa.id} untuk memotong tagihan yang masih tercatat atas nama calon siswa
	 * (jalur {@code TagihanUtilCalonSiswa}).</p>
	 *
	 * <p><b>Efek samping:</b> hasil {@code check(..)} ditulis balik ke field {@code calonSiswa}.
	 * Method ini juga ikut dipanggil {@link #getSiswa()} dan {@link #toString()}.</p>
	 *
	 * @return calon siswa penerima diskon, atau {@code null} bila penerima dicatat dari sisi
	 *         siswa aktif
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		calonSiswa = check(calonSiswa);
		return calonSiswa;
	}

	/**
	 * Menyetel penerima diskon berstatus pendaftar PSB. Tanpa validasi; boleh {@code null}.
	 *
	 * @param calonSiswa calon siswa penerima diskon
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

}
