package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
 * Entity master <b>bidang/cabang prestasi guru</b> — tabel <code>sekolah.cabang_prestasi_guru</code>.
 *
 * <p><b>Domain TERVERIFIKASI dari kode, bukan dugaan.</b> Pemetaan keras di {@link #getKode()}
 * membuktikan bahwa yang dicatat tabel ini adalah <i>bidang lomba</i> tempat guru berprestasi:
 * <code>Seni</code>, <code>Olah Raga</code>, <code>Kejuaraan Ilmiah</code>, dan <code>Lain-Lain</code>.
 * Ini BUKAN tingkat/cakupan kejuaraan — dimensi itu dipegang entity pasangannya
 * {@link ais.database.model.sekolah.KategoriPrestasiGuru} (Internasional/Nasional/.../Kecamatan).
 * Satu baris {@link ais.database.model.sekolah.PrestasiGuru} karena itu menjawab dua pertanyaan lewat
 * dua FK terpisah: "di bidang apa" (kelas ini, kolom <code>cabang_prestasi_guru</code>) dan "seberapa
 * tinggi tingkatnya" (kolom <code>kategori_prestasi_guru</code>).</p>
 *
 * <p><b>Kembaran lintas modul — keluarga berjumlah lima.</b> Kelas ini adalah salinan sisi-GURU dari
 * satu keluarga entity yang sama persis. Pemetaan empat label ke empat angka pada {@link #getKode()}
 * identik karakter demi karakter di lima kelas sekaligus:
 * {@link ais.database.model.CabangPrestasiMahasiswa}, {@link ais.database.model.CabangPrestasiDosen},
 * {@link ais.database.model.CabangPrestasiPegawai},
 * {@link ais.database.model.sekolah.CabangPrestasiSiswa}, dan kelas ini. Kelimanya bahkan berbagi
 * {@link #serialVersionUID} yang sama ({@code 2463821577548439808L}) — jejak generator, bukan tanda
 * hubungan warisan.</p>
 *
 * <p><b>Asal-usul angka pada {@code kode}.</b> Nilai 1/2/3/9 adalah kode <i>jenis prestasi</i> milik
 * PDDikti/Neo Feeder. Jejak aslinya hanya utuh pada kembaran perguruan tinggi
 * {@link ais.database.model.CabangPrestasiMahasiswa} — satu-satunya dari kelima kelas yang masih
 * membawa kolom <code>feeder</code> dan benar-benar mengirimkannya sebagai
 * <code>id_jenis_prestasi</code> lewat eksportir Feeder. Versi guru ini (seperti versi siswa, dosen,
 * dan pegawai) adalah hasil <i>porting</i> TANPA kolom <code>feeder</code>, dan modul sekolah tidak
 * punya integrasi Feeder sama sekali. Konsekuensinya: di sini {@code kode} praktis kolom mati — tidak
 * ada satu pun pembaca di seluruh repositori.</p>
 *
 * <p><b>Struktur.</b> Entity ini sangat ramping — hanya {@code id}, {@code kode}, {@code nama},
 * {@code keterangan}, ditambah jejak audit warisan ({@code oleh}, {@code olehId},
 * {@code tanggal_dirubah}). Tidak ada koleksi balik ke {@link ais.database.model.sekolah.PrestasiGuru}
 * (relasi satu arah dari sisi transaksi), tidak ada FK induk/anak, tidak ada kolom {@code aktif}
 * maupun {@code nomorUrut}, dan — ini penting — <b>tidak ada kolom {@code sekolah} maupun
 * {@code yayasan}</b>. Tabel ini memang katalog GLOBAL satu instalasi, dipakai bersama seluruh sekolah
 * dan yayasan; ketiadaan filter tenant pada pembacanya bukan kebocoran karena tidak ada dimensi tenant
 * yang bisa disaring.</p>
 *
 * <p><b>Pengelompokan anggota.</b></p>
 * <ul>
 * <li><i>Identitas &amp; kunci</i> — {@link #getId()}/{@link #setId(Long)}, {@link #serialVersionUID}.</li>
 * <li><i>Jejak audit warisan</i> — {@link #getOleh()}/{@link #setOleh(String)},
 *     {@link #getOlehId()}/{@link #setOlehId(String)},
 *     {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback {@link #onUpdate()}.</li>
 * <li><i>Muatan bisnis</i> — {@link #getKode()}/{@link #setKode(String)},
 *     {@link #getNama()}/{@link #setNama(String)}, {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 * <li><i>Infrastruktur</i> — konstruktor {@link #CabangPrestasiGuru()} dan {@link #toString()}.</li>
 * </ul>
 *
 * <p><b>Hal non-obvious yang WAJIB diketahui sebelum menyunting kelas ini.</b></p>
 * <ol>
 * <li><b>{@link #getKode()} adalah getter DESTRUKTIF (menulis balik ke field).</b> Getter ini bukan
 *     pembaca murni: bila {@code nama} cocok salah satu dari empat label baku, ia MENIMPA field
 *     {@code kode}. Karena pemetaan Hibernate kelas ini berbasis <i>property access</i> (anotasi
 *     {@code @Id} berada di getter) dan {@code getKode()} tidak diberi {@code @Transient}, hasil
 *     timpaan itu ikut ditulis ke kolom <code>kode</code> pada flush berikutnya. Rinciannya di Javadoc
 *     method tersebut. Pola identik dengan
 *     {@link ais.database.model.sekolah.KategoriPrestasiGuru#getKode()} dan
 *     {@link ais.database.model.sekolah.CabangPrestasiSiswa#getKode()}.</li>
 * <li><b>Tidak ada kolom {@code aktif} dan tidak ada {@code nomorUrut}.</b> Seluruh baris selalu
 *     tampil, sehingga bug berulang "kolom aktif tak pernah ditulis layar master" tidak berlaku di
 *     sini. Begitu pula pola "{@code getNomorUrut()} non-null yang meruntuhkan {@code TreeSet}" —
 *     kelas ini tidak meng-override {@code getNomorUrut()} maupun {@code compareTo()}, dan tidak
 *     pernah dimasukkan ke koleksi terurut mana pun.</li>
 * <li><b>Komentar generator "Bank generated by hbm2java" pada versi lama adalah salah salin.</b>
 *     Kelas ini tidak ada hubungannya dengan entity Bank; string yang sama tersalin ke belasan berkas
 *     lain di repositori. Komentar itu digantikan Javadoc ini.</li>
 * <li><b>Warisan {@link ais.database.model.GeneralValueObject} bukan {@code @MappedSuperclass}.</b>
 *     Kelas induk adalah POJO abstrak biasa sehingga Hibernate TIDAK memetakan propertinya. Karena itu
 *     {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} sengaja DIDEKLARASIKAN
 *     ULANG di sini — ini keharusan teknis pemetaan, bukan duplikasi yang bisa dibersihkan. Jangan
 *     menghapusnya.</li>
 * <li><b>{@code @Audited} (Hibernate Envers) aktif.</b> Setiap penulisan — termasuk penulisan tak
 *     sengaja akibat butir 1 — menghasilkan satu revisi baru di tabel audit.</li>
 * </ol>
 *
 * <p><b>Siapa yang membaca/menulis baris ini — dan kejutannya.</b> Berbeda dari kembaran siswa yang
 * punya layar master penuh ({@code ais.action.master.sekolah.CabangPrestasiSiswaAction}) DAN penyemai
 * otomatis empat baris baku di {@code PrestasiSiswaAction} ({@code session.save} untuk
 * Seni/Olah&nbsp;Raga/Kejuaraan&nbsp;Ilmiah/Lain-Lain), kelas ini <b>tidak punya keduanya</b>. Tidak
 * ada kelas {@code PrestasiGuruAction} sama sekali di repositori. Seluruh rujukan hanya tiga:
 * (a) field {@code cabangPrestasiGuru} pada {@link ais.database.model.sekolah.PrestasiGuru};
 * (b) kombo "Pilih Cabang" dan penyaring multi-pilih pada layar
 * <code>/WEB-INF/baru/modul/prestasi/_prestasi_guru.jsp</code>, yang memuat daftar lewat endpoint
 * generik <code>/Data</code> ({@code action=daftar}); dan (c) pembacaan {@code getNama()} untuk label
 * kolom "cabang" pada ekspor/dasbor {@code ais.action.master.prestasi.DasbordPrestasi.muatGuru(...)}
 * serta rekap SQL di <code>_dashboard_prestasi_guru.jsp</code>. <b>Tidak ada satu pun kode Java yang
 * membuat, menyunting, atau menyemai baris kelas ini.</b> Akibat praktisnya: pada instalasi baru tabel
 * <code>cabang_prestasi_guru</code> kosong selamanya dan kombo "Pilih Cabang" pada formulir Prestasi
 * Guru selalu kosong, kecuali baris diisi lewat SQL langsung atau lewat panggilan
 * {@code action=simpanDataRinci} ke <code>/Data</code> dengan
 * {@code class=ais.database.model.sekolah.CabangPrestasiGuru} — jalur yang terbuka bagi pengguna
 * terautentikasi mana pun. Entity ini juga terdaftar di manifest generic-CRUD
 * (<code>webapp/WEB-INF/generic-crud/manifests/general_value_object_inventory.json</code>) dengan
 * status <code>ELIGIBLE_METADATA_FIRST</code>, artinya layar CRUD-nya memang direncanakan tetapi belum
 * pernah dibangkitkan.</p>
 *
 * <p><b>PERINGATAN KEAMANAN — fail-open cakupan personalia guru BERLAKU JUGA untuk entity ini.</b>
 * Temuan yang didokumentasikan pada {@link ais.database.model.sekolah.KategoriPrestasiGuru} sudah
 * DIVERIFIKASI ULANG dan relevan penuh di sini, karena kedua entity dipakai pada LAYAR YANG SAMA
 * (<code>_prestasi_guru.jsp</code> dan <code>_dashboard_prestasi_guru.jsp</code>). Ringkasnya: ketiga
 * variabel penentu lingkup di-hardcode {@code null} dengan panggilan aslinya DIKOMENTARI
 * ({@code Yayasan loginSebagaiYayasan = null; // tbmuser.getYayasan();} dan seterusnya, termasuk
 * {@code // tbmuser.getGuru()}), sehingga cabang penyaring {@code guru}/{@code sekolah}/{@code yayasan}
 * tidak pernah dieksekusi dan akun guru biasa melihat serta mengekspor prestasi SELURUH guru lintas
 * sekolah/yayasan. Uraian lengkapnya tidak diulang di sini — lihat Javadoc kelas
 * {@link ais.database.model.sekolah.KategoriPrestasiGuru}. Dua catatan TAMBAHAN yang khusus menyangkut
 * kelas ini:</p>
 * <ul>
 * <li><b>Penyaring "Cabang" pada layar daftar menyebut kolom yang tidak ada.</b>
 *     <code>_prestasi_guru.jsp</code> menyusun klausa
 *     {@code filters.push("this_.cabang_prestasi_guru_id IN (...)")}, padahal kolom fisiknya bernama
 *     <code>cabang_prestasi_guru</code> tanpa akhiran <code>_id</code> — lihat
 *     {@code @JoinColumn(name = "cabang_prestasi_guru")} pada
 *     {@link ais.database.model.sekolah.PrestasiGuru#getCabangPrestasiGuru()}. Klausa itu diteruskan
 *     APA ADANYA ke {@code Restrictions.sqlRestriction(...)} oleh
 *     {@code ais.action.servlet.api.DaftarDataService}, sehingga memilih cabang pada penyaring
 *     menghasilkan galat SQL, bukan hasil tersaring. Penyaring "Kategori" di baris berikutnya
 *     mengidap salah nama yang sama. Bukti pembanding: berkas
 *     <code>_dashboard_prestasi_guru.jsp</code> memakai nama kolom yang BENAR pada rekap cabang
 *     ({@code JOIN sekolah.cabang_prestasi_guru c ON p.cabang_prestasi_guru = c.id}).</li>
 * <li><b>Rekap cabang di dasbor dibangun sebagai string SQL di peramban</b> lalu dikirim ke
 *     <code>/Data</code> dengan {@code action:"sql"}. Nilai {@code baseWhere} yang seharusnya menjadi
 *     pembatas hak akses ikut disusun di sisi klien — jadi bahkan bila ketiga variabel lingkup di atas
 *     dipulihkan, pembatas itu tetap dapat diganti pemanggil. Endpoint tersebut kini punya lapis
 *     pertahanan {@code ais.common.SqlSecurityGuard.checkReadSql(...)}, tetapi lapis itu dikendalikan
 *     konfigurasi {@code mode_proteksi_sql_endpoint} yang <i>default</i>-nya nonaktif. Temuan ini
 *     sudah tercakup task audit-luas yang berjalan; tidak dibuat catatan terpisah.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.PrestasiGuru
 * @see ais.database.model.sekolah.KategoriPrestasiGuru
 * @see ais.database.model.sekolah.CabangPrestasiSiswa
 * @see ais.database.model.CabangPrestasiMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "cabang_prestasi_guru")
public class CabangPrestasiGuru extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya sengaja dipertahankan apa adanya. Nilai yang sama dipakai pula oleh keempat kembaran
	 * cabang prestasi lainnya, oleh {@link ais.database.model.sekolah.KategoriPrestasiGuru}, dan oleh
	 * {@link ais.database.model.sekolah.PrestasiGuru}, karena berkas-berkas itu lahir dari salinan
	 * generator yang sama — bukan indikasi hubungan warisan apa pun. Instance entity ikut
	 * terserialisasi saat ZK menyimpan state komponen ke dalam session, sehingga mengubah nilai ini
	 * dapat mematahkan session lama yang masih hidup.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama tabel <code>sekolah.cabang_prestasi_guru</code>.
	 *
	 * <p>Dideklarasikan ulang karena {@link ais.database.model.GeneralValueObject} bukan
	 * {@code @MappedSuperclass}. Dipetakan lewat {@link #getId()}.</p>
	 */
	private Long id;
	/**
	 * Nama tampilan pengguna yang terakhir menyimpan baris ini (jejak audit warisan).
	 *
	 * <p>Diisi otomatis oleh lapisan penyimpanan bersama, bukan oleh formulir pengguna.</p>
	 */
	private String oleh;
	/**
	 * Identitas login (user id) pengguna yang terakhir menyimpan baris ini (jejak audit warisan).
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas login pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return user id penyimpan terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identitas login pengguna penyimpan terakhir.
	 *
	 * <p><b>Perhatikan penjaga di awal method:</b> nilai {@code null} atau string kosong/spasi
	 * DIABAIKAN diam-diam — nilai lama dipertahankan. Ini kontrak keluarga
	 * {@link ais.database.model.GeneralValueObject}: jejak audit tidak boleh terhapus oleh pemanggil
	 * yang kebetulan tidak punya konteks pengguna (misalnya proses batch atau penyemaian data).</p>
	 *
	 * @param olehId user id penyimpan; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama tampilan pengguna penyimpan terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan agar jejak audit
	 * yang sudah ada tidak tertimpa nilai hampa.</p>
	 *
	 * @param oleh nama pengguna penyimpan; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampilan pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nama penyimpan terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyegarkan jejak audit sesaat sebelum baris di-<i>update</i>.
	 *
	 * <p>Mendelegasikan seluruh pekerjaan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setTanggal_dirubah(Date)} beserta {@link #setOleh(String)}/{@link #setOlehId(String)}
	 * dari konteks pengguna yang sedang aktif. Dipanggil oleh penyedia persistensi, bukan oleh kode
	 * aplikasi.</p>
	 *
	 * <p><b>Efek samping penting:</b> callback ini juga ikut berjalan pada <i>update</i> yang tidak
	 * disengaja — misalnya update yang dipicu oleh getter destruktif {@link #getKode()} — sehingga
	 * stempel waktu dan nama penyimpan dapat berubah tanpa ada yang benar-benar menyunting baris.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris yang sama seperti pada
	 * berkas aslinya; posisinya dipertahankan agar diff terhadap berkas kembarannya tetap bersih.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan oleh
	 * formulir. Berbeda dari {@link #setOleh(String)}, method ini TIDAK punya penjaga nilai kosong
	 * sehingga {@code null} akan benar-benar menghapus stempel.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Diinisialisasi ke waktu pembuatan objek oleh {@code ais.ui.util.WaktuUtil.getDate()} sehingga
	 * baris baru tidak pernah bernilai {@code null}. Dipetakan sebagai {@code TIMESTAMP}.</p>
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas untuk keperluan log dan penelusuran, berbentuk
	 * <code>&lt;id&gt;-&lt;nama&gt;</code>.
	 *
	 * <p><b>Catatan:</b> method ini membaca field {@code nama} secara LANGSUNG, bukan lewat
	 * {@link #getNama()}, sehingga spasi di ujung nama TIDAK dipangkas di sini. Untuk baris yang belum
	 * tersimpan, {@code id} masih {@code null} sehingga hasilnya berawalan <code>"null-"</code>.</p>
	 *
	 * @return gabungan id dan nama cabang, dipisahkan tanda hubung.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode numerik jenis/bidang prestasi dalam bentuk teks ("1", "2", "3", "9").
	 *
	 * <p>Diturunkan otomatis dari {@code nama} oleh {@link #getKode()}. Lihat Javadoc getter itu untuk
	 * sifat destruktifnya. Di modul sekolah, kolom ini tidak dibaca oleh siapa pun.</p>
	 */
	private String kode;

	/**
	 * Nama bidang/cabang prestasi — inti data kelas ini.
	 *
	 * <p>Empat nilai baku yang dikenali {@link #getKode()}: <code>Seni</code>,
	 * <code>Olah Raga</code>, <code>Kejuaraan Ilmiah</code>, <code>Lain-Lain</code>. Kolom bersifat
	 * <code>NOT NULL</code>.</p>
	 */
	private String nama;

	/**
	 * Keterangan bebas untuk baris cabang (opsional).
	 *
	 * <p>Tidak dibaca oleh logika bisnis mana pun; murni catatan administratif.</p>
	 */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen.
	 *
	 * <p>Wajib ada agar Hibernate dapat meng-instansiasi entity saat memuat baris dari basis data, dan
	 * agar endpoint generik <code>/Data</code> dapat membentuk objek baru lewat refleksi. Seluruh field
	 * dibiarkan bernilai awal ({@code tanggal_dirubah} sudah terisi waktu saat ini lewat inisialisasi
	 * field).</p>
	 */
	public CabangPrestasiGuru() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Anotasi {@code @Id} berada di getter, sehingga SELURUH pemetaan kelas ini memakai
	 * <i>property access</i> — Hibernate membaca nilai properti melalui getter, bukan langsung dari
	 * field. Fakta ini yang membuat {@link #getKode()} bisa berdampak ke basis data. Nilai dibangkitkan
	 * basis data ({@code IDENTITY}), karena itu kolom ditandai {@code insertable = false}.</p>
	 *
	 * @return id baris, atau {@code null} bila baris belum pernah disimpan.
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
	 * <p>Diisi oleh Hibernate setelah {@code INSERT}. Jangan diubah manual pada entity yang sudah
	 * terkelola sesi — Hibernate akan menganggapnya baris lain.</p>
	 *
	 * @param id kunci utama baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode numerik jenis prestasi — <b>getter DESTRUKTIF, bukan pembaca murni</b>.
	 *
	 * <p><b>Tujuan.</b> Menerjemahkan {@code nama} yang berupa label baku menjadi kode angka
	 * PDDikti/Neo Feeder, dengan pemetaan sebagai berikut (perbandingan <i>case-insensitive</i>):</p>
	 * <table border="1" summary="Pemetaan nama cabang prestasi ke kode">
	 * <tr><th>nama</th><th>kode</th></tr>
	 * <tr><td>Kejuaraan Ilmiah</td><td>1</td></tr>
	 * <tr><td>Seni</td><td>2</td></tr>
	 * <tr><td>Olah Raga</td><td>3</td></tr>
	 * <tr><td>Lain-Lain</td><td>9</td></tr>
	 * </table>
	 *
	 * <p>Perhatikan bahwa urutan pemeriksaan di dalam kode (Seni, Olah Raga, Kejuaraan Ilmiah,
	 * Lain-Lain) tidak sama dengan urutan angkanya — bukan kesalahan, hanya urutan penulisan asli yang
	 * dipertahankan sama persis di kelima kembaran.</p>
	 *
	 * <p><b>EFEK SAMPING — ini bagian terpenting.</b> Method ini bukan hanya menghitung nilai
	 * kembalian; ia MENUGASKAN hasilnya ke field {@code kode} ({@code kode = "2";} dan seterusnya).
	 * Karena kelas ini dipetakan dengan <i>property access</i> (lihat {@link #getId()}) dan getter ini
	 * tidak diberi {@code @Transient}, properti {@code kode} ikut dipetakan ke kolom <code>kode</code>.
	 * Setiap kali Hibernate membaca properti — termasuk saat pemeriksaan <i>dirty checking</i>
	 * menjelang flush — nilai hasil timpaan itulah yang terbaca. Bila nilai tersebut berbeda dari nilai
	 * yang tersimpan di basis data, Hibernate menerbitkan {@code UPDATE} yang tidak pernah diminta
	 * pengguna, memicu {@link #onUpdate()} (stempel waktu dan nama penyimpan berubah) dan menghasilkan
	 * satu revisi baru di tabel audit Envers.</p>
	 *
	 * <p><b>Konsekuensi lanjutan.</b> Nilai {@code kode} yang sengaja diisi berbeda lewat
	 * {@link #setKode(String)} tidak akan bertahan selama {@code nama} masih cocok salah satu label
	 * baku — timpaan terjadi setiap kali properti dibaca. Sebaliknya, untuk {@code nama} di luar
	 * keempat label, method ini tidak mengubah apa pun dan nilai lama {@code kode} dikembalikan apa
	 * adanya (termasuk {@code null}). Perilaku ini identik dengan
	 * {@link ais.database.model.sekolah.CabangPrestasiSiswa#getKode()} dan ketiga kembaran lainnya di
	 * modul perguruan tinggi.</p>
	 *
	 * <p><b>Siapa yang memanggil.</b> Tidak ada kode aplikasi di repositori ini yang membaca
	 * {@code kode} untuk keputusan bisnis; pemanggil nyatanya adalah Hibernate sendiri (pemetaan
	 * properti) dan endpoint generik <code>/Data</code> saat menyerialisasi seluruh properti entity ke
	 * JSON bagi kombo "Pilih Cabang". Justru karena itu efek sampingnya mudah luput dari perhatian.</p>
	 *
	 * <p><b>Jangan "memperbaiki" method ini tanpa rencana migrasi:</b> menambahkan {@code @Transient}
	 * akan menghilangkan kolom dari pemetaan (dan dari DDL yang dibangkitkan), sedangkan sekadar
	 * menghapus penugasan akan membuat baris lama yang kodenya salah tidak pernah lagi terkoreksi.</p>
	 *
	 * @return kode jenis prestasi sebagai teks, atau {@code null} bila belum pernah diisi dan
	 *         {@code nama} tidak cocok label baku mana pun.
	 */
	public String getKode() {
		if (nama != null) {
			if (nama.equalsIgnoreCase("Seni")) {
				kode = "2";
			} else if (nama.equalsIgnoreCase("Olah Raga")) {
				kode = "3";
			} else if (nama.equalsIgnoreCase("Kejuaraan Ilmiah")) {
				kode = "1";
			} else if (nama.equalsIgnoreCase("Lain-Lain")) {
				kode = "9";
			}
		}
		return kode;
	}

	/**
	 * Menetapkan kode jenis prestasi secara manual.
	 *
	 * <p><b>Perhatikan:</b> nilai yang diset di sini bersifat sementara bila {@code nama} cocok salah
	 * satu dari empat label baku — {@link #getKode()} akan menimpanya kembali pada pembacaan
	 * berikutnya. Lihat Javadoc getter tersebut.</p>
	 *
	 * @param kode kode jenis prestasi sebagai teks; boleh {@code null}.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama bidang/cabang prestasi, sudah dipangkas spasi di kedua ujungnya.
	 *
	 * <p>Kolom <code>nama</code> bersifat wajib ({@code nullable = false}) dengan panjang maksimum 255
	 * karakter. Nilai inilah yang ditampilkan sebagai label pilihan pada kombo "Pilih Cabang" dan
	 * penyaring multi-pilih di layar Prestasi Guru, sebagai kolom "cabang" pada
	 * {@code DasbordPrestasi.muatGuru(...)}, serta sebagai label baris pada rekap cabang di
	 * <code>_dashboard_prestasi_guru.jsp</code>.</p>
	 *
	 * <p><b>Catatan:</b> pemangkasan dilakukan di getter, bukan di setter, sehingga nilai yang tersimpan
	 * di basis data bisa saja masih mengandung spasi ujung. Karena pemetaan berbasis property access,
	 * nilai terpangkas itulah yang dibandingkan saat <i>dirty checking</i> — baris lama yang namanya
	 * berspasi ujung akan ter-{@code UPDATE} sendiri pada flush berikutnya.</p>
	 *
	 * @return nama cabang tanpa spasi di ujung, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama bidang/cabang prestasi.
	 *
	 * <p>Nilai disimpan apa adanya tanpa pemangkasan maupun validasi. Agar {@link #getKode()} dapat
	 * memetakan kode dengan benar, gunakan salah satu dari empat label baku yang didaftar pada Javadoc
	 * getter tersebut.</p>
	 *
	 * @param nama nama cabang/bidang prestasi; wajib terisi sebelum disimpan karena kolomnya
	 *             {@code NOT NULL}.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris cabang.
	 *
	 * <p>Kolom opsional, tidak dipakai logika bisnis mana pun.</p>
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris cabang.
	 *
	 * @param keterangan catatan administratif; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
