package ais.database.model.rab;

// Generated Dec 19, 2009 10:58:09 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.GeneralValueObject;



/**
 * Entitas baris rincian <b>Renstra Program Punya Indikator</b> pada modul RAB, dipetakan ke tabel
 * {@code rab.renstra_program_punya_indikator}. Setiap baris menyatakan satu indikator kinerja milik
 * sebuah {@link RenstraProgram}, lengkap dengan <b>target dan pagu anggaran untuk lima tahun</b>
 * periode Rencana Strategis: {@code target1}..{@code target5} berpasangan dengan
 * {@code anggaran1}..{@code anggaran5}.
 *
 * <h2>Bagaimana nomor 1..5 dipetakan ke tahun kalender</h2>
 * <p>Angka pada nama kolom adalah <b>nomor urut tahun dalam periode</b>, bukan tahun kalender.
 * Tahun kalender yang diwakili kolom ke-N adalah {@code renstraProgram.getTahun() + (N - 1)},
 * dengan {@link RenstraProgram#getTahun()} sebagai tahun awal periode. Konsekuensi penting: baris
 * ini <b>tidak menyimpan tahun apa pun sendiri</b>. Ia sepenuhnya bergantung pada induknya untuk
 * penanggalan, sehingga mengubah {@code tahun} pada {@link RenstraProgram} akan menggeser makna
 * seluruh kolom bernomor pada semua barisnya sekaligus — tanpa satu pun angka di sini ikut berubah.
 * Tidak ada mekanisme yang mencegah atau memperingatkan pergeseran tersebut.</p>
 *
 * <h2>PERINGATAN — {@code indikator} di sini adalah teks bebas, bukan relasi</h2>
 * <p>Properti {@link #getIndikator()} bertipe {@link String} dan dipetakan sebagai kolom
 * {@code text}. Ia <b>bukan</b> foreign key ke entitas {@link Indikator} yang berada di paket yang
 * sama. Pola ini konsisten dengan induknya, yang juga menyimpan {@link RenstraProgram#getSasaran()}
 * sebagai teks bebas alih-alih relasi ke entitas {@link Sasaran}. Jadi seluruh klaster RENSTRA
 * sengaja melewatkan katalog nomenklatur {@code Indikator}/{@code Sasaran} yang sudah tersedia di
 * modul yang sama, dan memakai string lepas. Akibatnya laporan yang hendak menggabungkan RENSTRA
 * dengan modul lain (mis. kinerja pegawai di modul LKP, yang justru memakai entitas
 * {@link Indikator}) harus mencocokkan teks, bukan menelusuri relasi.</p>
 *
 * <h2>Tidak ada kolom tenant — pembatasan diwarisi dari induk</h2>
 * <p>Entitas ini tidak memiliki relasi {@link SatuanKerja}. Kepemilikan tenant sepenuhnya melekat
 * pada {@link #getRenstraProgram()}. Pemuatannya di {@code RenstraProgramAction} pun dilakukan
 * lewat {@code Restrictions.eq("renstraProgram", renstraProgram)}, sehingga baris rincian hanya
 * seketat induknya. Perlu dicatat bahwa {@code RenstraProgramAction.initCriteria(...)} tidak
 * menyaring induk berdasarkan satuan kerja sama sekali — sehingga secara efektif nilai
 * {@code target1..5} dan {@code anggaran1..5} milik seluruh satuan kerja terbaca oleh siapa pun
 * yang punya akses ke layar RENSTRA. Ini instansi dari pola berulang "filter tenant lemah/hilang".
 * Relasi {@code renstraProgram} sendiri dinyatakan {@code nullable = true}, jadi baris yatim tanpa
 * induk secara teknis mungkin ada; baris semacam itu tidak akan pernah tampil di layar mana pun
 * karena satu-satunya kueri pemuatnya menyaring berdasarkan induk.</p>
 *
 * <h2>Field {@code nama} berstatus tidur</h2>
 * <p>Verifikasi atas basis kode menunjukkan {@link #getNama()} dan {@link #setNama(String)}
 * <b>tidak pernah dipanggil</b> dari mana pun: baik {@code RenstraProgramAction} maupun
 * {@code RenstraProgramPunyaIndikatorHelper} hanya menyunting kolom
 * indikator/lokasi/target/anggaran/keterangan. Kolomnya tetap ada di basis data dan tetap diaudit
 * Envers, tetapi selalu bernilai NULL pada instalasi normal. Konstruktor
 * {@link #RenstraProgramPunyaIndikator(String)} yang mengisinya juga tidak punya pemanggil.</p>
 *
 * <h2>Total anggaran dihitung di layar, tidak disimpan</h2>
 * <p>{@code RenstraProgramPunyaIndikatorHelper} menampilkan kolom "Total" berisi jumlah
 * {@code anggaran1}..{@code anggaran5} dan menghitung ulang nilainya setiap kali salah satu kotak
 * anggaran berubah. Nilai itu <b>tidak</b> memiliki field maupun kolom padanan di entitas ini —
 * murni turunan tampilan. Jangan mencarinya di basis data; hitunglah dari kelima kolom penyusunnya.
 * Perlu diketahui pula bahwa helper tersebut hanya menulis ke objek in-memory
 * ({@code session.update(...)}-nya dikomentari), sehingga persistensi baru terjadi ketika layar
 * induk menyimpan formulir secara keseluruhan.</p>
 *
 * <h2>Seluruh getter numerik dan {@code getIndikator()} berefek samping</h2>
 * <p>Sepuluh getter target/anggaran menormalkan {@code null} menjadi {@code 0.0}, dan
 * {@link #getIndikator()} menormalkan {@code null} menjadi string kosong — semuanya dengan
 * <b>menuliskan hasil normalisasi kembali ke field</b>. Karena entitas memakai <i>property
 * access</i> (anotasi {@link Id} melekat pada {@link #getId()}), Hibernate memanggil getter-getter
 * ini saat <i>dirty checking</i>, sehingga normalisasi tersebut benar-benar tertulis ke basis data
 * pada {@code flush} berikutnya. Dampaknya: perbedaan antara "belum diisi" (NULL) dan "sengaja
 * dinilai nol" (0) hilang permanen setelah baris dibaca sekali. Uraian lengkapnya ada pada
 * dokumentasi {@link #getTarget1()} dan {@link #getIndikator()}; getter sejenis lainnya merujuk ke
 * sana agar tidak berulang.</p>
 *
 * <h2>Pemetaan ORM</h2>
 * <p>Entitas memakai {@code dynamicInsert}/{@code dynamicUpdate} sehingga Hibernate hanya menulis
 * kolom yang benar-benar berubah, dan diberi {@link Audited} sehingga Hibernate Envers merekam
 * setiap revisi ke tabel bayangan pada skema {@code rab}. Kunci utama memakai strategi
 * {@link javax.persistence.GenerationType#IDENTITY}. Relasi {@code renstraProgram} tidak dinyatakan
 * {@code LAZY} sehingga mengikuti bawaan {@link ManyToOne} (eager), dengan {@link FetchMode#SELECT}
 * agar Hibernate memakai kueri terpisah alih-alih {@code JOIN} — inilah sebabnya
 * {@link #getRenstraProgram()} tidak perlu memanggil {@code check(...)}. Kesepuluh kolom numerik
 * tidak diberi anotasi {@code @Column} sehingga memakai pemetaan bawaan
 * ({@code double precision} yang boleh NULL).</p>
 *
 * <h2>Catatan gaya kode</h2>
 * <p>Berkas asli memampatkan deklarasi field {@code oleh}/{@code olehId} beserta accessor-nya ke
 * dalam satu baris, dan menempelkan deklarasi field {@code tanggal_dirubah} di belakang method
 * {@link #onUpdate()}. Pada berkas ini deklarasi tersebut dipisah baris agar setiap anggota dapat
 * diberi dokumentasi, tanpa mengubah semantik apa pun.</p>
 *
 * @see RenstraProgram
 * @see ais.database.dao.rab.RenstraProgramPunyaIndikatorDao
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "renstra_program_punya_indikator")



public class RenstraProgramPunyaIndikator extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja disamakan dengan hampir seluruh entitas lain
	 * di paket {@code ais.database.model.rab} (hasil salin-tempel templat hbm2java), sehingga
	 * <b>tidak</b> bisa dipakai untuk membedakan tipe saat deserialisasi.
	 */
	private static final long serialVersionUID = -8738027816264807168L;

	/**
	 * Kunci utama basis data, dibangkitkan oleh kolom identity pada
	 * {@code rab.renstra_program_punya_indikator.id}. Bernilai {@code null} selama objek belum
	 * pernah disimpan.
	 */
	private Long id;

	/**
	 * Field audit bayangan: nama pengguna terakhir yang mengubah baris ini. Diisi lewat
	 * {@link #setOleh(String)} oleh lapisan interceptor/penyimpanan, bukan oleh pengguna.
	 */
	private String oleh;

	/**
	 * Field audit bayangan: identitas (id pengguna) terakhir yang mengubah baris ini. Pasangan dari
	 * {@link #oleh}, diisi oleh lapisan interceptor/penyimpanan.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Setter ini <b>menolak diam-diam</b> nilai
	 * {@code null} maupun string kosong/spasi sehingga jejak audit yang sudah terisi tidak terhapus
	 * oleh proses penyalinan objek atau pengikatan form yang mengirim nilai kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau kosong diabaikan sehingga jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum operasi {@code UPDATE}, mendelegasikan
	 * pemutakhiran stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak boleh dipanggil
	 * langsung dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}

	/**
	 * Field audit bayangan: stempel waktu perubahan terakhir. Diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil#getDate()} (bukan {@code new Date()}, agar mengikuti zona
	 * waktu/penyesuaian waktu aplikasi) dan diperbarui otomatis oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya diisi otomatis lewat {@link #onUpdate()};
	 * pemanggilan manual hanya relevan pada skenario impor/migrasi data.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai kolom {@code TIMESTAMP}.
	 *
	 * @return waktu penyimpanan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Field <b>tidur</b>: tidak pernah dibaca maupun ditulis oleh kode aplikasi mana pun (lihat
	 * catatan pada dokumentasi kelas). Kolomnya tetap ada dan tetap diaudit Envers, tetapi selalu
	 * bernilai NULL pada instalasi normal.
	 */
	private String nama;

	/**
	 * Keterangan bebas atas baris indikator ini. Disunting langsung di grid oleh
	 * {@code RenstraProgramPunyaIndikatorHelper}. Dipetakan sebagai kolom {@code text}.
	 */
	private String keterangan;

	/**
	 * Dokumen RENSTRA induk yang memiliki baris indikator ini. Sekaligus satu-satunya sumber
	 * penanda tenant dan sumber tahun awal periode bagi kolom {@code target1..5}/
	 * {@code anggaran1..5}.
	 */
	private RenstraProgram renstraProgram;

	/**
	 * Rumusan indikator dalam bentuk <b>teks bebas</b>. Bukan foreign key ke entitas
	 * {@link Indikator} — lihat peringatan pada dokumentasi kelas.
	 */
	private String indikator;

	/**
	 * Lokasi pelaksanaan yang berkaitan dengan indikator ini, dalam bentuk teks bebas. Dipetakan
	 * sebagai kolom {@code text}.
	 */
	private String lokasi;

	/** Target capaian untuk tahun ke-1 periode RENSTRA. */
	private Double target1;
	/** Target capaian untuk tahun ke-2 periode RENSTRA. */
	private Double target2;
	/** Target capaian untuk tahun ke-3 periode RENSTRA. */
	private Double target3;
	/** Target capaian untuk tahun ke-4 periode RENSTRA. */
	private Double target4;
	/** Target capaian untuk tahun ke-5 periode RENSTRA. */
	private Double target5;

	/** Pagu anggaran untuk tahun ke-1 periode RENSTRA. */
	private Double anggaran1;
	/** Pagu anggaran untuk tahun ke-2 periode RENSTRA. */
	private Double anggaran2;
	/** Pagu anggaran untuk tahun ke-3 periode RENSTRA. */
	private Double anggaran3;
	/** Pagu anggaran untuk tahun ke-4 periode RENSTRA. */
	private Double anggaran4;
	/** Pagu anggaran untuk tahun ke-5 periode RENSTRA. */
	private Double anggaran5;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Juga dipakai {@code RenstraProgramPunyaIndikatorHelper} saat menambah baris pada grid.
	 */
	public RenstraProgramPunyaIndikator() {
	}

	/**
	 * Konstruktor pintas yang mengisi {@link #nama}, disediakan oleh generator hbm2java. Karena
	 * {@code nama} berstatus tidur pada entitas ini, konstruktor ini <b>tidak punya pemanggil</b>
	 * di basis kode.
	 *
	 * @param nama nilai untuk field {@code nama} yang tidak terpakai.
	 */
	public RenstraProgramPunyaIndikator(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Normalnya diisi Hibernate setelah {@code INSERT}; pengisian manual hanya
	 * relevan pada skenario impor/migrasi.
	 *
	 * @param id kunci utama baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan isi field {@link #nama} yang berstatus tidur, dengan spasi di ujung dipangkas.
	 * Tidak ada pemanggil di basis kode; praktis selalu mengembalikan {@code null}.
	 *
	 * @return nilai {@code nama} yang sudah dipangkas, atau {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel field {@link #nama} yang berstatus tidur. Tidak ada pemanggil di basis kode.
	 *
	 * @param nama nilai yang hendak disimpan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas atas baris indikator ini, apa adanya tanpa pemangkasan spasi.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas atas baris indikator ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan dokumen RENSTRA induk. Getter murni tanpa efek samping: relasi ini dipetakan
	 * eager ({@link ManyToOne} tanpa {@code fetch = LAZY}) sehingga nilainya sudah berupa objek
	 * nyata dan tidak perlu dipaksa lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return dokumen RENSTRA induk, atau {@code null} bila baris ini yatim (secara teknis mungkin
	 *         karena kolom dinyatakan {@code nullable}, meskipun tidak dapat dicapai lewat layar).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "renstra_program", nullable = true)
	public RenstraProgram getRenstraProgram() {
		return renstraProgram;
	}

	/**
	 * Menyetel dokumen RENSTRA induk. Dipanggil {@code RenstraProgramAction} tepat sebelum
	 * menyimpan setiap baris grid, agar baris yang baru ditambahkan terikat ke induk yang sedang
	 * disunting.
	 *
	 * @param renstraProgram dokumen RENSTRA induk.
	 */
	public void setRenstraProgram(RenstraProgram renstraProgram) {
		this.renstraProgram = renstraProgram;
	}

	/**
	 * Mengembalikan rumusan indikator sebagai teks bebas, menormalkan {@code null} menjadi string
	 * kosong. Method ini <b>bukan getter murni</b>: hasil normalisasi dituliskan kembali ke field
	 * {@link #indikator}, bukan sekadar dikembalikan.
	 *
	 * <h3>Mengapa efek samping ini berarti</h3>
	 * <p>Entitas memakai <i>property access</i> (anotasi {@link Id} melekat pada {@link #getId()}),
	 * sehingga Hibernate membaca nilai properti dengan memanggil getter ini — termasuk saat
	 * melakukan <i>dirty checking</i> menjelang {@code flush}. Akibatnya normalisasi
	 * {@code null} menjadi {@code ""} bukan sekadar kenyamanan tampilan, melainkan perubahan yang
	 * <b>benar-benar tertulis</b> ke kolom {@code indikator} pada basis data, sekaligus mencatat
	 * satu revisi baru di tabel Envers. Setelah pembacaan pertama, baris yang semula bernilai NULL
	 * menjadi bernilai string kosong dan kedua keadaan itu tidak lagi dapat dibedakan.</p>
	 *
	 * <h3>Implikasi bagi kueri</h3>
	 * <p>Jangan memakai {@code indikator IS NULL} untuk mencari baris yang indikatornya belum
	 * dirumuskan — setelah baris pernah dimuat aplikasi, kondisi yang benar adalah
	 * {@code indikator IS NULL OR indikator = ''}. Hal yang sama berlaku pada laporan dan proses
	 * pembersihan data. Sifat "menormalkan sambil menulis" ini dipakai seragam di seluruh getter
	 * bernilai bawaan pada entitas ini; lihat juga {@link #getTarget1()}.</p>
	 *
	 * @return rumusan indikator; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	@Column(columnDefinition = "text")
	public String getIndikator() {
		if (indikator == null) {
			indikator = "";
		}
		return indikator;
	}

	/**
	 * Menyetel rumusan indikator sebagai teks bebas. Tidak ada validasi maupun pencocokan ke katalog
	 * {@link Indikator}.
	 *
	 * @param indikator teks rumusan indikator.
	 */
	public void setIndikator(String indikator) {
		this.indikator = indikator;
	}

	/**
	 * Mengembalikan lokasi pelaksanaan sebagai teks bebas, apa adanya tanpa pemangkasan spasi dan
	 * tanpa normalisasi {@code null} — berbeda dari {@link #getIndikator()} yang menormalkannya.
	 *
	 * @return teks lokasi, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getLokasi() {
		return lokasi;
	}

	/**
	 * Menyetel lokasi pelaksanaan sebagai teks bebas.
	 *
	 * @param lokasi teks lokasi.
	 */
	public void setLokasi(String lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengembalikan target capaian tahun ke-1 periode RENSTRA, menormalkan {@code null} menjadi
	 * {@code 0.0}. Method ini <b>bukan getter murni</b>: hasil normalisasi dituliskan kembali ke
	 * field.
	 *
	 * <h3>Pola bersama sepuluh getter numerik</h3>
	 * <p>Bentuk yang sama dipakai oleh {@link #getTarget2()} sampai {@link #getTarget5()} dan
	 * {@link #getAnggaran1()} sampai {@link #getAnggaran5()}. Uraian di sini berlaku bagi
	 * kesepuluhnya; dokumentasi masing-masing merujuk balik ke sini agar tidak berulang.</p>
	 *
	 * <h3>Mengapa efek samping ini berarti</h3>
	 * <p>Entitas memakai <i>property access</i> (anotasi {@link Id} melekat pada {@link #getId()}),
	 * sehingga Hibernate membaca nilai properti dengan memanggil getter ini, termasuk saat
	 * melakukan <i>dirty checking</i> menjelang {@code flush}. Normalisasi {@code null} menjadi
	 * {@code 0.0} karenanya <b>benar-benar tertulis</b> ke basis data dan mencatat satu revisi baru
	 * di tabel Envers. Karena {@code RenstraProgramPunyaIndikatorHelper} membaca kesepuluh getter
	 * ini saat membangun kotak-kotak isian dan saat menghitung kolom "Total", sekadar membuka layar
	 * detail satu dokumen RENSTRA sudah cukup untuk menormalkan seluruh kolom NULL pada seluruh
	 * baris rincian dokumen tersebut.</p>
	 *
	 * <h3>Implikasi bagi pelaporan</h3>
	 * <p>Setelah pembacaan pertama, "target/anggaran tahun ini belum direncanakan" (NULL) tidak lagi
	 * dapat dibedakan dari "target/anggaran tahun ini memang nol" (0). Bagi dokumen RENSTRA yang
	 * periodenya belum seluruhnya terisi — hal yang lumrah pada tahun-tahun awal — perbedaan ini
	 * bermakna: laporan yang menghitung "jumlah tahun yang sudah direncanakan" akan menghitung
	 * berlebih, dan rata-rata pagu per tahun akan terkoreksi ke bawah oleh nol-nol semu. Bila
	 * kelengkapan perencanaan perlu dilacak, sediakan penanda terpisah alih-alih mengandalkan NULL.
	 * Untuk memeriksa keadaan asli data, lakukan kueri SQL langsung sebelum baris pernah dimuat
	 * aplikasi, atau telusuri revisi paling awal pada tabel Envers.</p>
	 *
	 * <h3>Catatan pembulatan</h3>
	 * <p>Nilai bertipe {@link Double}, bukan {@code BigDecimal}. Untuk angka pagu anggaran yang
	 * besar, penjumlahan kelima kolom di layar dapat memunculkan galat pembulatan biner. Jangan
	 * memakai hasilnya sebagai angka akuntansi yang mengikat tanpa pembulatan eksplisit.</p>
	 *
	 * @return target tahun ke-1; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTarget1() {
		if (target1 == null) {
			target1 = 0.0;
		}
		return target1;
	}

	/**
	 * Menyetel target capaian tahun ke-1 periode RENSTRA. Menerima {@code null}, yang akan
	 * dinormalkan menjadi {@code 0.0} pada pembacaan berikutnya.
	 *
	 * @param target1 target tahun ke-1.
	 */
	public void setTarget1(Double target1) {
		this.target1 = target1;
	}

	/**
	 * Mengembalikan target capaian tahun ke-2 periode RENSTRA, menormalkan {@code null} menjadi
	 * {@code 0.0} sambil menuliskannya kembali ke field. Lihat {@link #getTarget1()} untuk uraian
	 * lengkap efek samping dan implikasinya.
	 *
	 * @return target tahun ke-2; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTarget2() {
		if (target2 == null) {
			target2 = 0.0;
		}
		return target2;
	}

	/**
	 * Menyetel target capaian tahun ke-2 periode RENSTRA.
	 *
	 * @param target2 target tahun ke-2.
	 */
	public void setTarget2(Double target2) {
		this.target2 = target2;
	}

	/**
	 * Mengembalikan target capaian tahun ke-3 periode RENSTRA, menormalkan {@code null} menjadi
	 * {@code 0.0} sambil menuliskannya kembali ke field. Lihat {@link #getTarget1()} untuk uraian
	 * lengkap efek samping dan implikasinya.
	 *
	 * @return target tahun ke-3; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTarget3() {
		if (target3 == null) {
			target3 = 0.0;
		}
		return target3;
	}

	/**
	 * Menyetel target capaian tahun ke-3 periode RENSTRA.
	 *
	 * @param target3 target tahun ke-3.
	 */
	public void setTarget3(Double target3) {
		this.target3 = target3;
	}

	/**
	 * Mengembalikan target capaian tahun ke-4 periode RENSTRA, menormalkan {@code null} menjadi
	 * {@code 0.0} sambil menuliskannya kembali ke field. Lihat {@link #getTarget1()} untuk uraian
	 * lengkap efek samping dan implikasinya.
	 *
	 * @return target tahun ke-4; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTarget4() {
		if (target4 == null) {
			target4 = 0.0;
		}
		return target4;
	}

	/**
	 * Menyetel target capaian tahun ke-4 periode RENSTRA.
	 *
	 * @param target4 target tahun ke-4.
	 */
	public void setTarget4(Double target4) {
		this.target4 = target4;
	}

	/**
	 * Mengembalikan target capaian tahun ke-5 periode RENSTRA, menormalkan {@code null} menjadi
	 * {@code 0.0} sambil menuliskannya kembali ke field. Lihat {@link #getTarget1()} untuk uraian
	 * lengkap efek samping dan implikasinya.
	 *
	 * @return target tahun ke-5; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getTarget5() {
		if (target5 == null) {
			target5 = 0.0;
		}
		return target5;
	}

	/**
	 * Menyetel target capaian tahun ke-5 periode RENSTRA.
	 *
	 * @param target5 target tahun ke-5.
	 */
	public void setTarget5(Double target5) {
		this.target5 = target5;
	}

	/**
	 * Mengembalikan pagu anggaran tahun ke-1 periode RENSTRA, menormalkan {@code null} menjadi
	 * {@code 0.0} sambil menuliskannya kembali ke field. Lihat {@link #getTarget1()} untuk uraian
	 * lengkap efek samping, implikasi pelaporan, dan catatan pembulatan {@link Double}. Nilai ini
	 * termasuk yang dijumlahkan {@code RenstraProgramPunyaIndikatorHelper} untuk kolom "Total" yang
	 * hanya ada di layar.
	 *
	 * @return pagu anggaran tahun ke-1; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getAnggaran1() {
		if (anggaran1 == null) {
			anggaran1 = 0.0;
		}
		return anggaran1;
	}

	/**
	 * Menyetel pagu anggaran tahun ke-1 periode RENSTRA. Tidak ada validasi nilai negatif maupun
	 * pembatasan terhadap pagu induk mana pun.
	 *
	 * @param anggaran1 pagu anggaran tahun ke-1.
	 */
	public void setAnggaran1(Double anggaran1) {
		this.anggaran1 = anggaran1;
	}

	/**
	 * Mengembalikan pagu anggaran tahun ke-2 periode RENSTRA, menormalkan {@code null} menjadi
	 * {@code 0.0} sambil menuliskannya kembali ke field. Lihat {@link #getTarget1()} untuk uraian
	 * lengkap.
	 *
	 * @return pagu anggaran tahun ke-2; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getAnggaran2() {
		if (anggaran2 == null) {
			anggaran2 = 0.0;
		}
		return anggaran2;
	}

	/**
	 * Menyetel pagu anggaran tahun ke-2 periode RENSTRA.
	 *
	 * @param anggaran2 pagu anggaran tahun ke-2.
	 */
	public void setAnggaran2(Double anggaran2) {
		this.anggaran2 = anggaran2;
	}

	/**
	 * Mengembalikan pagu anggaran tahun ke-3 periode RENSTRA, menormalkan {@code null} menjadi
	 * {@code 0.0} sambil menuliskannya kembali ke field. Lihat {@link #getTarget1()} untuk uraian
	 * lengkap.
	 *
	 * @return pagu anggaran tahun ke-3; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getAnggaran3() {
		if (anggaran3 == null) {
			anggaran3 = 0.0;
		}
		return anggaran3;
	}

	/**
	 * Menyetel pagu anggaran tahun ke-3 periode RENSTRA.
	 *
	 * @param anggaran3 pagu anggaran tahun ke-3.
	 */
	public void setAnggaran3(Double anggaran3) {
		this.anggaran3 = anggaran3;
	}

	/**
	 * Mengembalikan pagu anggaran tahun ke-4 periode RENSTRA, menormalkan {@code null} menjadi
	 * {@code 0.0} sambil menuliskannya kembali ke field. Lihat {@link #getTarget1()} untuk uraian
	 * lengkap.
	 *
	 * @return pagu anggaran tahun ke-4; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getAnggaran4() {
		if (anggaran4 == null) {
			anggaran4 = 0.0;
		}
		return anggaran4;
	}

	/**
	 * Menyetel pagu anggaran tahun ke-4 periode RENSTRA.
	 *
	 * @param anggaran4 pagu anggaran tahun ke-4.
	 */
	public void setAnggaran4(Double anggaran4) {
		this.anggaran4 = anggaran4;
	}

	/**
	 * Mengembalikan pagu anggaran tahun ke-5 periode RENSTRA, menormalkan {@code null} menjadi
	 * {@code 0.0} sambil menuliskannya kembali ke field. Lihat {@link #getTarget1()} untuk uraian
	 * lengkap.
	 *
	 * @return pagu anggaran tahun ke-5; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Double getAnggaran5() {
		if (anggaran5 == null) {
			anggaran5 = 0.0;
		}
		return anggaran5;
	}

	/**
	 * Menyetel pagu anggaran tahun ke-5 periode RENSTRA.
	 *
	 * @param anggaran5 pagu anggaran tahun ke-5.
	 */
	public void setAnggaran5(Double anggaran5) {
		this.anggaran5 = anggaran5;
	}

}
