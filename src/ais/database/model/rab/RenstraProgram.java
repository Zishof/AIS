package ais.database.model.rab;

// Generated Dec 19, 2009 10:58:09 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;



/**
 * Entitas <b>Renstra Program</b> (Rencana Strategis tingkat program) pada modul RAB, dipetakan ke
 * tabel {@code rab.renstra_program}. Satu baris mewakili satu program strategis milik sebuah satuan
 * kerja untuk satu <b>periode RENSTRA lima tahunan</b> yang dimulai pada {@link #getTahun()}.
 * Rumusan target dan pagu anggaran per tahun tidak disimpan di sini melainkan pada entitas anak
 * {@link RenstraProgramPunyaIndikator}, yang membawa {@code target1..target5} dan
 * {@code anggaran1..anggaran5} — satu pasang per tahun ke-1 sampai ke-5 dalam periode.
 *
 * <h2>Struktur klaster RENSTRA</h2>
 * <p>Verifikasi atas basis kode menunjukkan klaster ini hanya terdiri dari dua entitas dan
 * <b>berdiri sendiri</b> di dalam modul RAB:</p>
 * <ul>
 *   <li>{@code RenstraProgram} (berkas ini) — kepala dokumen: nama program, sasaran (teks),
 *   keterangan, tahun mulai periode, dan satuan kerja pemilik;</li>
 *   <li>{@link RenstraProgramPunyaIndikator} — baris rincian: rumusan indikator (teks), lokasi,
 *   serta target dan anggaran untuk lima tahun periode.</li>
 * </ul>
 * <p>Yang penting dipahami, klaster ini <b>tidak bertaut</b> ke bagian lain modul RAB:
 * {@code RenstraProgram} tidak berelasi ke {@link Workspace} (pohon program/kegiatan beranggaran),
 * tidak ke {@link Proyek}, tidak ke {@link OutputKegiatan}, dan tidak ke
 * {@link RencanaDanRealisasiOutputKegiatan}. Jadi meskipun secara konsep perencanaan RENSTRA
 * seharusnya menjadi payung bagi kegiatan dan outputnya, pada tingkat basis data hubungan itu
 * <b>tidak diwujudkan sebagai foreign key</b> — satu-satunya penghubung antar keduanya adalah
 * {@link SatuanKerja} yang sama. Rekonsiliasi antara dokumen RENSTRA dan capaian output kegiatan
 * karenanya harus dilakukan secara manual/laporan, bukan lewat penelusuran relasi.</p>
 *
 * <h2>PERINGATAN #1 — {@code sasaran} di sini adalah teks bebas, bukan relasi</h2>
 * <p>Properti {@link #getSasaran()} bertipe {@link String} dan dipetakan sebagai kolom {@code text}.
 * Ia <b>bukan</b> foreign key ke entitas {@link Sasaran} yang berada di paket yang sama, dan
 * {@code RenstraProgramAction} terverifikasi tidak pernah mengimpor
 * {@code ais.database.model.rab.Sasaran}. Hal serupa berlaku pada anaknya:
 * {@link RenstraProgramPunyaIndikator#getIndikator()} juga bertipe {@link String} dan bukan relasi
 * ke entitas {@link Indikator}. Dengan kata lain seluruh klaster RENSTRA memakai nomenklatur
 * berbentuk teks bebas dan sengaja melewatkan katalog {@code Sasaran}/{@code Indikator} yang sudah
 * tersedia di modul yang sama. Konsekuensinya: tidak ada jaminan konsistensi penamaan, dan laporan
 * yang ingin menggabungkan RENSTRA dengan data kinerja lain harus mencocokkan string.</p>
 *
 * <h2>PERINGATAN #2 — nama kolom {@code parent} untuk relasi satuan kerja</h2>
 * <p>Relasi {@link #getSatuanKerja()} dipetakan ke kolom bernama <b>{@code parent}</b>, bukan
 * {@code satuan_kerja} seperti pada seluruh entitas lain di paket ini ({@link Indikator},
 * {@link Sasaran}, {@link Proyek}, {@link Tor}, {@link RencanaDanRealisasiOutputKegiatan}). Ini
 * jebakan nyata saat menulis SQL atau migrasi langsung: kolom {@code parent} pada tabel
 * {@code rab.renstra_program} <b>bukan</b> referensi ke induk RENSTRA lain (entitas ini tidak
 * memiliki hierarki induk-anak sama sekali), melainkan referensi ke {@code rab.satuan_kerja}.</p>
 *
 * <h2>PERINGATAN #3 — dua getter berefek samping tanpa penjaga "objek baru"</h2>
 * <p>Baik {@link #getSatuanKerja()} maupun {@link #getTahun()} mengisi sendiri field-nya bila
 * bernilai {@code null}, dan <b>keduanya tidak memasang penjaga {@code id == null}</b> yang dipakai
 * entitas tetangga ({@link Indikator#getSatuanKerja()}, {@link Sasaran#getSatuanKerja()},
 * {@link Proyek#getSatuanKerja()}). Karena entitas ini memakai <i>property access</i> (anotasi
 * {@link Id} berada pada getter), Hibernate membaca kedua getter tersebut saat <i>dirty
 * checking</i>, sehingga nilai yang diisi otomatis ikut tertulis ke basis data pada {@code flush}
 * berikutnya. Uraian rincinya beserta dampaknya ada pada dokumentasi masing-masing method.</p>
 *
 * <h2>Pemetaan ORM</h2>
 * <p>Entitas memakai {@code dynamicInsert}/{@code dynamicUpdate} sehingga Hibernate hanya menulis
 * kolom yang benar-benar berubah, dan diberi {@link Audited} sehingga Hibernate Envers merekam
 * setiap revisi ke tabel bayangan pada skema {@code rab}. Kunci utama memakai strategi
 * {@link javax.persistence.GenerationType#IDENTITY}. Relasi {@code satuanKerja} tidak dinyatakan
 * {@code LAZY} sehingga mengikuti bawaan {@link ManyToOne} (eager), dengan {@link FetchMode#SELECT}
 * agar Hibernate memakai kueri terpisah alih-alih {@code JOIN}.</p>
 *
 * <h2>Catatan gaya kode</h2>
 * <p>Berkas asli memampatkan deklarasi field {@code oleh}/{@code olehId} beserta accessor-nya ke
 * dalam satu baris, dan menempelkan deklarasi field {@code tanggal_dirubah} di belakang method
 * {@link #onUpdate()}. Pada berkas ini deklarasi tersebut dipisah baris agar setiap anggota dapat
 * diberi dokumentasi, tanpa mengubah semantik apa pun.</p>
 *
 * @see RenstraProgramPunyaIndikator
 * @see SatuanKerja
 * @see ais.database.dao.rab.RenstraProgramDao
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "renstra_program")



public class RenstraProgram extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja disamakan dengan hampir seluruh entitas lain
	 * di paket {@code ais.database.model.rab} (hasil salin-tempel templat hbm2java), sehingga
	 * <b>tidak</b> bisa dipakai untuk membedakan tipe saat deserialisasi.
	 */
	private static final long serialVersionUID = -8738027816264807168L;

	/**
	 * Kunci utama basis data, dibangkitkan oleh kolom identity pada
	 * {@code rab.renstra_program.id}. Bernilai {@code null} selama objek belum pernah disimpan.
	 * Berbeda dari entitas tetangga sepaket, nilai field ini <b>tidak</b> dipakai sebagai penjaga
	 * pada {@link #getSatuanKerja()} maupun {@link #getTahun()}.
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
	 * Nama program strategis. Dipetakan sebagai kolom {@code text} sehingga rumusan panjang
	 * tertampung utuh. Divalidasi wajib isi oleh {@code RenstraProgramAction} sebelum penyimpanan.
	 */
	private String nama;

	/**
	 * Keterangan bebas atas program. Tidak diberi anotasi {@code @Column} eksplisit sehingga memakai
	 * pemetaan bawaan Hibernate (kolom {@code keterangan}, panjang bawaan 255) — perhatikan bahwa
	 * ini satu-satunya kolom teks pada entitas ini yang <b>tidak</b> bertipe {@code text}.
	 */
	private String keterangan;

	/**
	 * Rumusan sasaran program dalam bentuk <b>teks bebas</b>. Bukan foreign key ke entitas
	 * {@link Sasaran} — lihat peringatan pada dokumentasi kelas.
	 */
	private String sasaran;

	/**
	 * Tahun awal periode RENSTRA lima tahunan. Bersama {@code target1..5} dan {@code anggaran1..5}
	 * pada {@link RenstraProgramPunyaIndikator}, field ini menentukan tahun kalender yang diwakili
	 * setiap kolom bernomor: kolom ke-N berarti tahun {@code tahun + (N - 1)}. Diisi otomatis oleh
	 * {@link #getTahun()} bila masih {@code null}.
	 */
	private Integer tahun;

	/**
	 * Satuan kerja pemilik dokumen RENSTRA ini, dipetakan ke kolom {@code parent} (bukan
	 * {@code satuan_kerja}). Diisi otomatis oleh {@link #getSatuanKerja()} bila masih {@code null}.
	 */
	private SatuanKerja satuanKerja;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Juga dipakai {@code RenstraProgramAction} saat menekan tombol tambah data.
	 */
	public RenstraProgram() {
	}

	/**
	 * Konstruktor pintas yang langsung mengisi {@link #nama}, disediakan oleh generator hbm2java.
	 *
	 * @param nama nama program strategis.
	 */
	public RenstraProgram(String nama) {
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
	 * Mengembalikan nama program dengan spasi di ujung sudah dipangkas. Pemangkasan dilakukan pada
	 * getter (bukan setter), sehingga nilai di memori bisa saja masih mengandung spasi sementara
	 * nilai yang ditulis Hibernate ke basis data sudah terpangkas.
	 *
	 * @return nama program yang sudah dipangkas, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama program apa adanya, tanpa pemangkasan maupun validasi. Kewajiban isi ditegakkan
	 * di lapisan {@code RenstraProgramAction}, bukan di sini.
	 *
	 * @param nama nama program strategis.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas atas program, apa adanya tanpa pemangkasan spasi.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas atas program.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan rumusan sasaran program sebagai teks bebas, apa adanya tanpa pemangkasan spasi
	 * dan tanpa nilai bawaan bila kosong. Perlu ditegaskan sekali lagi: nilai yang dikembalikan
	 * <b>bukan</b> objek {@link Sasaran} melainkan {@link String}, sehingga pemanggil tidak dapat
	 * menelusurinya ke katalog sasaran. {@code RenstraProgramAction} menampilkannya langsung sebagai
	 * label pada daftar, sehingga nilai {@code null} akan tampil sebagai label kosong.
	 *
	 * @return teks sasaran, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getSasaran() {
		return sasaran;
	}

	/**
	 * Menyetel rumusan sasaran program sebagai teks bebas. Tidak ada validasi maupun pencocokan ke
	 * katalog {@link Sasaran}.
	 *
	 * @param sasaran teks sasaran.
	 */
	public void setSasaran(String sasaran) {
		this.sasaran = sasaran;
	}

	/**
	 * Mengembalikan tahun awal periode RENSTRA. Method ini <b>bukan getter murni</b>: bila field
	 * masih {@code null}, ia mengisi sendiri dengan tahun berjalan yang dibaca lewat
	 * {@code ais.ui.util.WaktuUtil#getCalendar()} (bukan {@code Calendar.getInstance()}, agar
	 * mengikuti zona waktu/penyesuaian waktu aplikasi) lalu menuliskannya kembali ke field.
	 *
	 * <h3>Mengapa efek samping ini perlu diperhatikan</h3>
	 * <p>Entitas ini memakai <i>property access</i> — anotasi {@link Id} melekat pada
	 * {@link #getId()}, bukan pada field — sehingga Hibernate membaca nilai properti dengan
	 * memanggil getter ini, termasuk saat melakukan <i>dirty checking</i> menjelang {@code flush}.
	 * Akibatnya pengisian otomatis di atas bukan sekadar nilai bawaan yang tampil di layar,
	 * melainkan nilai yang <b>benar-benar tertulis</b> ke kolom {@code tahun}. Berbeda dari getter
	 * serupa pada entitas tetangga, di sini <b>tidak ada</b> penjaga {@code id == null}, sehingga
	 * pengisian otomatis juga berlaku bagi baris yang <i>sudah tersimpan</i> dengan kolom
	 * {@code tahun} bernilai NULL. Baris warisan/hasil impor semacam itu akan diam-diam mendapat
	 * tahun berjalan saat pertama kali dibaca.</p>
	 *
	 * <h3>Jalur pemicu yang nyata</h3>
	 * <p>Ini bukan skenario teoretis: {@code RenstraProgramAction} memanggil {@code getTahun()} di
	 * dalam <i>row renderer</i>-nya untuk membentuk label kolom Tahun. Renderer dijalankan untuk
	 * <b>setiap baris</b> yang tampil di daftar, di dalam sesi Hibernate yang masih terbuka.
	 * Sekadar membuka layar daftar RENSTRA sudah cukup untuk memicu penulisan pada seluruh baris
	 * ber-{@code tahun} NULL yang kebetulan tampil di halaman tersebut.</p>
	 *
	 * <h3>Implikasi praktis</h3>
	 * <p>Dampaknya relatif jinak dibanding {@link #getSatuanKerja()} (tahun berjalan biasanya
	 * memang tebakan yang masuk akal), tetapi tetap berarti: setelah pembacaan pertama, informasi
	 * "tahun belum ditentukan" hilang permanen dan tidak dapat dibedakan dari "tahun sengaja
	 * disetel ke tahun ini". Karena itu, jangan memakai {@code tahun IS NULL} sebagai penanda
	 * dokumen RENSTRA yang belum lengkap. Untuk memeriksa kelengkapan, lakukan kueri SQL langsung
	 * sebelum entitas pernah dimuat, atau tambahkan kolom penanda terpisah.</p>
	 *
	 * @return tahun awal periode RENSTRA; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun awal periode RENSTRA secara eksplisit. Pengisian manual ini mengalahkan
	 * pengisian otomatis pada {@link #getTahun()}, karena getter hanya bertindak ketika field masih
	 * {@code null}. {@code RenstraProgramAction} mewajibkan pengguna mengisi kolom ini sebelum
	 * menyimpan.
	 *
	 * @param tahun tahun awal periode; menyetel {@code null} akan menghidupkan kembali pengisian
	 *              otomatis pada pembacaan berikutnya.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan satuan kerja pemilik dokumen RENSTRA ini. Method ini <b>bukan getter murni</b>,
	 * dan perilakunya berbeda secara penting dari getter serupa pada entitas tetangga sepaket —
	 * perbedaan itu perlu dipahami sebelum menyunting kode di sekitarnya.
	 *
	 * <h3>Perilaku</h3>
	 * <p>Bila field {@link #satuanKerja} bernilai {@code null}, method mengambil pengguna yang
	 * sedang aktif lewat {@code Common.getCurrentUser()} dan, bila pengguna tersebut ada, mengisi
	 * field dengan {@code tbmuser.ambilSatuanKerja()}. Berbeda dari
	 * {@link Indikator#getSatuanKerja()} dan {@link Sasaran#getSatuanKerja()}, di sini tidak ada
	 * jalur cadangan lewat konteks perpustakaan, dan tidak ada pembungkus
	 * {@code try/catch} — sebagai gantinya dipasang pemeriksaan {@code tbmuser != null} yang menutup
	 * penyebab {@code NullPointerException} yang paling lazim.</p>
	 *
	 * <h3>PERINGATAN — tidak ada penjaga {@code id == null}</h3>
	 * <p>Inilah perbedaan terpentingnya. {@link Indikator#getSatuanKerja()},
	 * {@link Sasaran#getSatuanKerja()}, {@link Proyek#getSatuanKerja()}, dan
	 * {@link RencanaDanRealisasiOutputKegiatan#getSatuanKerja()} semuanya mensyaratkan
	 * {@code this.id == null} sebelum mengisi otomatis, sehingga pengisian hanya berlaku bagi objek
	 * yang belum pernah disimpan. Method ini <b>tidak</b> memasang syarat tersebut. Karena entitas
	 * memakai <i>property access</i>, Hibernate memanggil getter ini saat <i>dirty checking</i>;
	 * dengan demikian sebuah baris yang <i>sudah ada</i> di basis data dengan kolom {@code parent}
	 * bernilai NULL akan mendapat nilai satuan kerja milik <b>pembacanya</b>, dan nilai itu ikut
	 * tertulis ke basis data pada {@code flush} berikutnya (ditambah satu revisi baru di tabel
	 * Envers). Kepemilikan data berpindah tanpa ada tindakan simpan yang disengaja pengguna.</p>
	 *
	 * <h3>Jalur pemicu yang nyata</h3>
	 * <p>{@code RenstraProgramAction} memanggil {@code getSatuanKerja()} di dalam <i>row
	 * renderer</i>-nya (untuk membentuk label revisi lewat {@code RevisiHelper.createNewRevisi})
	 * dan lagi di dalam {@code init()} saat form ubah dibuka. Renderer dijalankan untuk setiap baris
	 * yang tampil, di dalam sesi Hibernate yang masih terbuka. Selain itu kriteria pencarian layar
	 * tersebut hanya menyaring berdasarkan {@code nama} dan sama sekali <b>tidak menyaring per
	 * satuan kerja</b>, sehingga daftar memuat dokumen RENSTRA milik seluruh satker. Gabungan kedua
	 * hal ini berarti membuka layar daftar RENSTRA dapat menuliskan satuan kerja pembaca ke
	 * baris-baris ber-{@code parent} NULL milik siapa pun.</p>
	 *
	 * <h3>Yang harus dilakukan pemanggil</h3>
	 * <p>Jangan memanggil getter ini dari kode yang bertujuan sekadar memeriksa apakah pemilik sudah
	 * ditentukan — pemanggilannya sendiri menentukan pemilik. Untuk pemeriksaan murni, lakukan kueri
	 * SQL langsung ke kolom {@code parent}. Bila hendak memperbaiki perilaku ini, penambahan syarat
	 * {@code this.id == null} akan menyeragamkannya dengan seluruh entitas tetangga; perlu diingat
	 * bahwa perbaikan tersebut tidak memulihkan baris yang kepemilikannya sudah terlanjur berubah,
	 * sehingga audit data historis lewat tabel revisi Envers tetap diperlukan.</p>
	 *
	 * @return satuan kerja pemilik dokumen RENSTRA, atau {@code null} bila tidak ada pengguna aktif
	 *         maupun satuan kerja yang melekat padanya.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "parent", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (satuanKerja == null) {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null) {
				satuanKerja = tbmuser.ambilSatuanKerja();
			}
		}
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik dokumen RENSTRA secara eksplisit, ditulis ke kolom
	 * {@code parent}. Pengisian manual ini mengalahkan pengisian otomatis pada
	 * {@link #getSatuanKerja()}, karena getter hanya bertindak ketika field masih {@code null}.
	 * {@code RenstraProgramAction} memanggilnya saat menyimpan, setelah memastikan pengguna sudah
	 * memilih satuan kerja pada form. Tidak ada validasi bahwa satuan kerja yang diberikan berada
	 * dalam cakupan wewenang pengguna.
	 *
	 * @param satuanKerja satuan kerja pemilik; boleh {@code null}, tetapi menyetelnya ke
	 *                    {@code null} menghidupkan kembali pengisian otomatis pada pembacaan
	 *                    berikutnya.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
