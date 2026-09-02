package ais.database.model;

// Generated Apr 23, 2010 12:45:00 AM by Hibernate Tools 3.2.4.CR1

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

import ais.common.ConstantValues;

/**
 * Entity biodata rinci (data pribadi) seorang dosen &mdash; tabel {@code public.biodata_dosen}.
 *
 * <h3>Peran dan pembagian tanggung jawab dengan {@link Dosen}</h3>
 * <p>AIS memecah data seorang pengajar menjadi dua tabel, persis seperti pasangan
 * {@link Mahasiswa}/{@link BiodataMahasiswa} dan {@link Pegawai}/{@link BiodataPegawai}:</p>
 * <ul>
 *   <li>{@link Dosen} &mdash; data <b>kepegawaian dan akademik</b>: NIDN/NIP, homebase jurusan &amp;
 *   fakultas, jabatan fungsional, golongan, status aktif, jadwal mengajar, bimbingan, dan
 *   seterusnya. Inilah entity yang dirujuk dari mana-mana (perkuliahan, pertemuan, skripsi, KRS,
 *   penggajian) sehingga barisnya wajib tetap ramping dan murah dibaca.</li>
 *   <li>{@code BiodataDosen} (kelas ini) &mdash; data <b>pribadi</b> yang jarang dibaca dan pada
 *   praktiknya hanya muncul pada satu formulir ({@code ais.action.master.BiodataDosenAction}) serta
 *   pada ekspor Feeder/EMIS: alamat administratif lengkap (RT/RW/dusun/kelurahan/kecamatan/kota/
 *   propinsi/negara/kode pos), keluarga (ayah, ibu, suami/istri beserta pekerjaannya), data fisik
 *   (tinggi, berat, golongan darah), kontak (telepon rumah, HP), dokumen (KTP, SIM), riwayat
 *   pendidikan SD sampai S3, lima slot bidang keahlian, organisasi, hobi, minat seni, dan
 *   kemampuan bahasa.</li>
 * </ul>
 *
 * <h3>Bagaimana instance kelas ini biasanya dibuat/dimuat: {@link Dosen#ambilBiodata()}</h3>
 * <p>Hampir tidak ada kode di AIS yang membuat {@code new BiodataDosen()} sendiri. Pintu masuk
 * standarnya adalah {@link Dosen#ambilBiodata()} / {@link Dosen#ambilBiodata(boolean)}. Baca
 * Javadoc method itu sebelum memakai kelas ini; ringkasnya:</p>
 * <ol>
 *   <li>Field {@code Dosen.biodataDosen} yang sudah terisi lengkap dipakai langsung (tanpa query).</li>
 *   <li>Bila kosong, seluruh isi cache {@code ConstantValues.ambilBerdasarClass(BiodataDosen.class)}
 *   ditelusuri untuk mencari baris yang dosennya cocok.</li>
 *   <li>Bila masih kosong, dijalankan {@code Criteria} atas kolom {@code dosen} lewat session native
 *   Hibernate; diambil baris ber-<b>ID terbesar</b> ({@code addOrder(Order.desc("id"))}) &mdash;
 *   isyarat jelas bahwa skema TIDAK mencegah adanya lebih dari satu baris biodata untuk dosen yang
 *   sama (kolom {@code dosen} hanya {@code nullable = false}, bukan {@code unique}).</li>
 *   <li><b>Efek samping yang penting diketahui:</b> pada varian tanpa argumen (dan
 *   {@code ambilBiodata(true)}), bila biodata tetap tidak ditemukan maka satu baris
 *   {@code biodata_dosen} <b>KOSONG DIBUAT DAN DI-COMMIT ke basis data</b> lewat transaksi yang
 *   dibuka sendiri di situ. Jadi sekadar "membaca" biodata seorang dosen dapat menambah baris baru.
 *   Pemanggil yang benar-benar hanya ingin membaca wajib memakai {@code ambilBiodata(false)}.</li>
 *   <li>Rangkaian di atas juga <b>menutup session Hibernate milik thread pemanggil</b>
 *   ({@code HibernateUtil.closeSession()} dipanggil setelah query dan setelah penyimpanan). Kode
 *   yang memegang entity lazy lain di sekitar pemanggilan itu bisa mendadak melihat
 *   {@code LazyInitializationException}.</li>
 * </ol>
 * <p>Jalur pemanggilan yang paling sering: formulir biodata dosen, pemeriksaan kelengkapan
 * {@code BiodataDosenAction.checkBiodataDosen(Dosen)} saat dosen login, ekspor
 * {@code ais.action.master.feeder.util.FeederExporter} dan laporan EMIS, serta &mdash; ini yang
 * mudah terlewat &mdash; SELURUH getter {@link BiodataPegawai}: pegawai yang merangkap dosen
 * mengambil nilai biodatanya dari sini lewat {@code Pegawai.getDosen().ambilBiodata()}. Artinya
 * membuka layar biodata seorang pegawai dapat membuat baris {@code biodata_dosen} baru.</p>
 *
 * <h3>Arah baca data: kelas ini MENGAMBIL sebagian nilainya dari {@link Dosen}</h3>
 * <p>Berbeda dari {@link BiodataPegawai} yang membayangi <i>hampir semua</i> getter-nya dari sini,
 * pada kelas ini hanya <b>lima</b> getter yang menyalin nilai dari entity {@link Dosen} dan
 * <b>menimpa field-nya sendiri</b> saat dibaca:</p>
 * <ul>
 *   <li>{@link #getAlamat()} &larr; {@code Dosen.getAlamat()} &mdash; tanpa penjagaan null.</li>
 *   <li>{@link #getNoKtp()} &larr; {@code Dosen.getKtp()} &mdash; tanpa penjagaan null.</li>
 *   <li>{@link #getNoIdentitas()} &larr; {@code Dosen.getKtp()} &mdash; hanya bila KTP dosen terisi.</li>
 *   <li>{@link #getTeleponRumah()} &larr; {@code Dosen.getTelp()} &mdash; hanya bila field lokal kosong.</li>
 *   <li>{@link #getHp()} &larr; {@code Dosen.getHp()} &mdash; hanya bila HP dosen terisi.</li>
 * </ul>
 * <p>Untuk kelima properti itu {@link Dosen} adalah sumber kebenaran dan kolom di
 * {@code biodata_dosen} sekadar salinan. Konsekuensi praktisnya: nilai yang baru diset lewat
 * setter dapat "hilang" pada pembacaan berikutnya, dan karena penimpaan terjadi pada field object
 * yang bisa sedang <i>managed</i> oleh {@link org.hibernate.Session} sementara entity dipetakan
 * {@code dynamicUpdate = true}, sekadar <b>membaca</b> biodata dapat memicu {@code UPDATE} pada
 * tabel {@code biodata_dosen} saat flush &mdash; pembacaan berubah menjadi penulisan.</p>
 *
 * <h3>Getter yang menulis nilai default ke dirinya sendiri</h3>
 * <p>Pola serupa (tanpa melibatkan {@link Dosen}) dipakai enam getter lain: mereka mengisi nilai
 * default ke field bila masih {@code null}, sehingga default itu ikut tersimpan pada flush
 * berikutnya walaupun pengguna tidak pernah mengisi apa pun:</p>
 * <ul>
 *   <li>{@link #getTinggiBadan()} dan {@link #getBeratBadan()} &rarr; {@code 0}</li>
 *   <li>{@link #getStatusNikah()} &rarr; {@code 0} (belum menikah)</li>
 *   <li>{@link #getKewarganegaraan()} &rarr; {@link Mahasiswa#WNI}</li>
 *   <li>{@link #getKelurahan()} &rarr; {@code "-"}</li>
 *   <li>{@link #getKewarganegaraanFeeder()} &rarr; {@code "ID"}</li>
 * </ul>
 * <p>Pengecualiannya {@link #getNegara()}: fallback {@link ConstantValues#INDONESIA} dikembalikan
 * <b>tanpa</b> ditulis ke field, jadi getter itu satu-satunya default yang tidak berefek ke
 * database. Perbedaan kecil ini disengaja atau tidak &mdash; yang jelas perilakunya berbeda dari
 * lima saudaranya di atas.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan {@link #onUpdate()}.</li>
 *   <li><b>Identitas &amp; representasi</b> &mdash; {@link #getId()}/{@link #setId(Long)},
 *   {@link #BiodataDosen()}, {@link #toString()}.</li>
 *   <li><b>Relasi utama</b> &mdash; {@link #getDosen()}/{@link #setDosen(Dosen)}.</li>
 *   <li><b>Alamat administratif</b> &mdash; {@link #getAlamat()}, {@link #getRt()}, {@link #getRw()},
 *   {@link #getDusun()}, {@link #getKelurahan()}, {@link #getKecamatan()}, {@link #getKota()},
 *   {@link #getPropinsi()}, {@link #getNegara()}, {@link #getKodepos()}.</li>
 *   <li><b>Keluarga</b> &mdash; ayah ({@link #getNamaAyah()}, {@link #getPekerjaanAyah()}), ibu
 *   ({@link #getNamaIbu()}, {@link #getPekerjaanIbu()}), suami/istri
 *   ({@link #getNamaSuamiIstri()}, {@link #getNipSuamiIstri()},
 *   {@link #getPekerjaanSuamiIstri()}), dan {@link #getStatusNikah()}.</li>
 *   <li><b>Data pribadi &amp; dokumen</b> &mdash; {@link #getTinggiBadan()},
 *   {@link #getBeratBadan()}, {@link #getGolonganDarah()}, {@link #getAgama()},
 *   {@link #getKewarganegaraan()}, {@link #getKewarganegaraanFeeder()}, {@link #getNoKtp()},
 *   {@link #getNoIdentitas()}, {@link #getSuratIzinMengemudi()}, {@link #getKendaraanKuliah()},
 *   {@link #getPernahMenetapDiLuarNegeri()}.</li>
 *   <li><b>Kontak</b> &mdash; {@link #getTeleponRumah()}, {@link #getHp()}.</li>
 *   <li><b>Organisasi, hobi, bahasa</b> &mdash; {@link #getPernahMemimpinOrganisasi()},
 *   {@link #getNamaOrganisasi()}, {@link #getHobi()}, {@link #getMinatSeni()},
 *   {@link #getKemampuanBahasa1()}..{@link #getKemampuanBahasa3()}.</li>
 *   <li><b>Riwayat pendidikan</b> &mdash; SD/SMP/SMA ({@link #getAsalSd()}, {@link #getAsalSmp()},
 *   {@link #getAsalSma()} + alamat masing-masing) dan S1/S2/S3 ({@link #getAsalS1()}..
 *   {@link #getAsalS3()} + alamat masing-masing).</li>
 *   <li><b>Keahlian &amp; gelar</b> &mdash; {@link #getKeahliah1()},
 *   {@link #getKeahlian2()}..{@link #getKeahlian5()}, {@link #getGelarAkademikProf()}.</li>
 * </ul>
 * <p>Tidak ada method bisnis, query statis, maupun helper UI di kelas ini; seluruh isinya adalah
 * pasangan getter/setter properti Hibernate. Semua logika formulir ada di
 * {@code ais.action.master.BiodataDosenAction}, dan CRUD generiknya di
 * {@link ais.database.dao.BiodataDosenDaoImpl} (turunan tipis {@code GenericHibernateDao} tanpa
 * method tambahan).</p>
 *
 * <h3>Properti yang membayangi {@link GeneralValueObject}</h3>
 * <p>{@link GeneralValueObject} sudah mendeklarasikan {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} lengkap dengan getter/setter-nya. Kelas ini <b>mendeklarasikan ulang
 * keempatnya</b> sebagai field privat sendiri dan menimpa (override) accessor-nya. Efeknya: yang
 * dibaca/ditulis adalah salinan milik kelas ini, sedangkan salinan di induk tetap ada tapi tidak
 * pernah terisi &mdash; method induk yang membaca field induk secara langsung (bukan lewat getter)
 * akan melihat {@code null}. Pola ini konsisten dengan {@link BiodataPegawai},
 * {@link BiodataMahasiswa}, dan {@link BiodataCalonMahasiswa}; sejauh audit inisiatif Javadoc ini,
 * SELURUH entity model AIS melakukannya. Kontrak {@code equals}/{@code compareTo}/{@code check()}
 * di induk tetap memakai {@code getId()} sehingga tidak terpengaruh.</p>
 *
 * <h3>Resolusi relasi lazy</h3>
 * <p>Seluruh getter {@code @ManyToOne} di kelas ini ({@link #getDosen()}, {@link #getAgama()},
 * {@link #getNegara()}, {@link #getKecamatan()}, {@link #getKota()}, {@link #getPropinsi()},
 * {@link #getPekerjaanAyah()}, {@link #getPekerjaanIbu()}, {@link #getPekerjaanSuamiIstri()})
 * memanggil {@link GeneralValueObject#check(Object)} lebih dulu agar proxy lazy yang sudah
 * <i>detached</i> tetap terpakai tanpa {@code LazyInitializationException}. Penjelasan lengkap
 * empat tahap resolusi (identity map &rarr; cache &rarr; {@code Hibernate.initialize} &rarr; reload
 * lewat session baru) ada di Javadoc {@link GeneralValueObject}.</p>
 *
 * <h3>Kuirk yang ditemukan saat pendokumentasian (belum diperbaiki)</h3>
 * <ul>
 *   <li><b>Anotasi {@code @Column} di setter.</b> {@code @Column(name = "alamat_asal_s2")} terpasang
 *   pada {@link #setAlamatAsalS2(String)}, bukan pada {@link #getAlamatAsalS2()}. Entity ini memakai
 *   <i>property access</i> ({@code @Id} ada di {@link #getId()}), jadi Hibernate hanya membaca
 *   anotasi dari getter dan anotasi pada setter itu <b>diabaikan</b> &mdash; kolomnya jatuh ke nama
 *   default. Bug yang sama persis juga ada di {@link BiodataPegawai}.</li>
 *   <li><b>Sembilan belas properti tanpa {@code @Column}.</b> {@code rt}, {@code rw},
 *   {@code kodepos}, {@code kelurahan}, {@code noIdentitas}, {@code dusun}, {@code namaSuamiIstri},
 *   {@code nipSuamiIstri}, {@code kewarganegaraanFeeder}, dan {@code alamatAsalS2} tidak
 *   dianotasi. Karena {@code ais.database.hibernate.MyNamingStrategy} adalah turunan
 *   {@code DefaultNamingStrategy} (nama kolom = nama properti apa adanya, tanpa konversi ke
 *   snake_case), kolom fisiknya bercampur gaya: {@code no_ktp} bersebelahan dengan
 *   {@code noIdentitas} dan {@code alamatAsalS2}. Query SQL ad-hoc atas tabel ini wajib
 *   memperhatikan hal tersebut.</li>
 *   <li><b>Salah eja properti keahlian.</b> Properti pertama bernama {@code keahliah1} (bukan
 *   {@code keahlian1}) sementara empat sisanya {@code keahlian2}..{@code keahlian5}; kolomnya
 *   justru konsisten salah eja semua: {@code keahliah1}..{@code keahliah5}. Sudah telanjur dipakai
 *   lapisan UI sehingga tidak bisa diganti tanpa menyentuh pemanggil.</li>
 *   <li><b>Dua pemeriksaan kelengkapan yang tidak pernah bisa gagal.</b>
 *   {@code BiodataDosenAction.checkBiodataDosen(Dosen)} mewajibkan sepuluh properti terisi lewat
 *   {@code Common.checkIsNull(BiodataDosen.class, ...)}, yang membaca nilai lewat
 *   {@code ClassMetadata.getPropertyValue(...)} &mdash; artinya lewat getter. Karena
 *   {@link #getKelurahan()} selalu mengembalikan minimal {@code "-"} dan {@link #getStatusNikah()}
 *   selalu mengembalikan minimal {@code 0}, dua dari sepuluh pemeriksaan itu efektif mati.</li>
 *   <li><b>{@link #toString()} tidak aman terhadap proxy detached.</b> Ia memakai <i>field</i>
 *   {@code dosen} secara langsung, bukan {@link #getDosen()}, sehingga tidak melewati
 *   {@code check()}. Pada entity yang sudah lepas dari session, memanggilnya dapat melempar
 *   {@code LazyInitializationException} &mdash; termasuk dari dalam debugger atau logging.</li>
 *   <li><b>{@link #getAlamat()} dan {@link #getNoKtp()} menimpa tanpa penjagaan null.</b> Bila
 *   {@link Dosen} belum punya alamat/KTP, membaca getter di sini akan MENGOSONGKAN nilai yang sudah
 *   telanjur terisi di {@code biodata_dosen}, dan pengosongan itu ikut tersimpan saat flush.
 *   Bandingkan dengan {@link #getNoIdentitas()}, {@link #getTeleponRumah()}, dan {@link #getHp()}
 *   yang menjaga kondisinya.</li>
 *   <li><b>{@code serialVersionUID} kembar.</b> Nilainya identik dengan milik
 *   {@link BiodataPegawai} ({@code 1995121656124539247L}) &mdash; sisa salin-tempel; tidak
 *   berbahaya karena serialisasi Java mencocokkan nama kelas juga.</li>
 *   <li><b>Komentar generator salah nama.</b> Javadoc kelas hasil hbm2java semula berbunyi
 *   "BiodataMahasiswa generated by hbm2java"; sudah digantikan oleh dokumentasi ini.</li>
 *   <li><b>{@link #setOleh(String)} dan {@link #setOlehId(String)} tidak bisa mengosongkan nilai</b>
 *   &mdash; keduanya langsung {@code return} bila argumennya {@code null}/kosong.</li>
 * </ul>
 *
 * <h3>Perbedaan dengan {@link BiodataMahasiswa} yang perlu diketahui</h3>
 * <p>{@link BiodataMahasiswa} punya getter wilayah "pintar" yang, bila propinsi/kota belum terisi,
 * <b>membuat baris master {@code Propinsi}/{@code Kota} baru</b> ({@code findOrCreatePropinsi},
 * {@code findOrCreateKota}, pencocokan nama berbasis jarak Levenshtein) sambil membuka lalu menutup
 * session Hibernate sendiri. <b>Kelas ini TIDAK punya perilaku itu.</b>
 * {@link #getKecamatan()} dan {@link #getKota()} hanya memanggil {@code check()};
 * {@link #getPropinsi()} paling jauh hanya menurunkan propinsi dari kota <i>di memori</i>. Tidak
 * ada satu pun method di file ini yang membuka {@code Session}, memulai transaksi, atau memanggil
 * {@code HibernateUtil.closeSession()}. Penulisan diam-diam ke database pada alur biodata dosen
 * seluruhnya berasal dari luar file ini &mdash; yaitu dari
 * {@link Dosen#ambilBiodata(boolean)}.</p>
 *
 * @see GeneralValueObject
 * @see Dosen
 * @see Dosen#ambilBiodata()
 * @see BiodataPegawai
 * @see BiodataMahasiswa
 * @see ais.action.master.BiodataDosenAction
 * @see ais.database.dao.BiodataDosenDaoImpl
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "biodata_dosen")
public class BiodataDosen extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Entity ini ikut diserialkan saat disimpan di session HTTP/ZK atau
	 * cache berkas, jadi nilainya jangan diubah tanpa alasan. Nilainya kebetulan identik dengan
	 * milik {@link BiodataPegawai} (sisa salin-tempel) &mdash; tidak masalah karena mekanisme
	 * serialisasi juga mencocokkan nama kelas.
	 */
	private static final long serialVersionUID = 1995121656124539247L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris biodata ini (jejak audit ringan).
	 * Membayangi properti bernama sama di {@link GeneralValueObject}.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna pengubah terakhir. <b>Nilai {@code null} atau string kosong diabaikan</b>
	 * (method langsung keluar), sehingga jejak audit tidak bisa dikosongkan lewat setter ini.
	 *
	 * @param olehId ID pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir. <b>Nilai {@code null} atau string kosong diabaikan</b>
	 * (method langsung keluar), sehingga jejak audit tidak bisa dikosongkan lewat setter ini.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris biodata ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum baris ini di-{@code UPDATE},
	 * dan mendelegasikan pengisian jejak audit ({@code oleh}, {@code olehId},
	 * {@code tanggal_dirubah}) ke {@code AuditTimestampInterceptor.ubah(this)}. Ini implementasi
	 * satu-satunya method {@code abstract} milik {@link GeneralValueObject}; jangan dipanggil
	 * manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir baris biodata ini. Normalnya diisi otomatis oleh
	 * {@link #onUpdate()}; pemanggilan manual hanya dipakai saat migrasi/impor data.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris biodata ini. Nilai awalnya diisi waktu
	 * pembuatan object ({@code WaktuUtil.getDate()}), bukan {@code null}.
	 *
	 * @return timestamp perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks biodata ini, yaitu representasi {@link Dosen} pemiliknya.
	 *
	 * <p><b>Perhatian:</b> method ini membaca <i>field</i> {@code dosen} secara langsung, TIDAK
	 * lewat {@link #getDosen()}, sehingga tidak melewati {@link GeneralValueObject#check(Object)}.
	 * Pada entity yang sudah lepas dari {@link org.hibernate.Session}, memanggilnya dapat melempar
	 * {@code LazyInitializationException} &mdash; termasuk secara tak sengaja dari logging atau
	 * inspeksi debugger. Bila relasi belum terisi, hasilnya string {@code "null"}.</p>
	 *
	 * @return representasi dosen pemilik biodata ini
	 */
	public String toString() {
		return dosen + "";
	}

	private Dosen dosen;
	private String alamat;
	private String namaAyah;
	private PekerjaanOrangTua pekerjaanAyah;
	private String namaIbu;
	private PekerjaanOrangTua pekerjaanIbu;
	private Integer tinggiBadan;
	private Integer pernahMenetapDiLuarNegeri;
	private Integer beratBadan;
	private String teleponRumah;
	private String hp;
	private String suratIzinMengemudi;
	private String kendaraanKuliah;
	private Integer pernahMemimpinOrganisasi;
	private String namaOrganisasi;
	private String hobi;
	private String minatSeni;
	private String kemampuanBahasa1;
	private String kemampuanBahasa2;
	private String kemampuanBahasa3;
	private String asalS1;
	private String alamatAsalS1;
	private String asalS2;
	private String alamatAsalS2;
	private String asalS3;
	private String alamatAsalS3;
	private String keahliah1;
	private String keahlian2;
	private String keahlian3;
	private String keahlian4;
	private String keahlian5;
	private String asalSma;
	private String alamatAsalSma;
	private String asalSmp;
	private String alamatAsalSmp;
	private String asalSd;
	private String alamatAsalSd;
	private String golonganDarah;
	private Integer statusNikah;
	private String kewarganegaraan;
	private Negara negara;
	private Agama agama;
	private String noKtp;
	private String gelarAkademikProf;

	private String rt;
	private String rw;
	private String kodepos;
	private String kelurahan;
	private Wilayah kecamatan;
	private Propinsi propinsi;
	private Kota kota;
	private String kewarganegaraanFeeder;

	private String noIdentitas;
	private String dusun;

	private String namaSuamiIstri;
	private String nipSuamiIstri;
	private PekerjaanOrangTua pekerjaanSuamiIstri;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Object hasilnya belum punya {@code id}
	 * maupun relasi {@link Dosen}; keduanya wajib diisi sebelum disimpan karena kolom
	 * {@code dosen} bersifat {@code nullable = false}. Untuk memperoleh biodata seorang dosen,
	 * gunakan {@link Dosen#ambilBiodata()} alih-alih membuat instance sendiri.
	 */
	public BiodataDosen() {
	}

	/**
	 * Mengembalikan kunci primer baris {@code biodata_dosen} ini. Kolomnya
	 * {@code insertable = false} karena nilainya dibangkitkan basis data
	 * ({@code GenerationType.IDENTITY}, sequence PostgreSQL). Membayangi
	 * {@code GeneralValueObject#getId()}, dan menjadi dasar {@code equals}/{@code compareTo} di
	 * induk.
	 *
	 * @return ID baris, atau {@code null} bila belum tersimpan
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci primer baris ini. Normalnya hanya dilakukan Hibernate setelah {@code INSERT};
	 * pemanggilan manual dipakai pada skenario impor/migrasi.
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan alamat tempat tinggal dosen (bagian "jalan" dari alamat administratif; RT, RW,
	 * dusun, kelurahan, dan seterusnya disimpan terpisah).
	 *
	 * <p><b>Bukan getter murni.</b> Bila relasi {@link Dosen} terisi, nilai
	 * {@code Dosen.getAlamat()} <b>ditimpakan ke field {@code alamat} milik object ini</b> setiap
	 * kali dibaca &mdash; {@link Dosen} diperlakukan sebagai sumber kebenaran alamat. Dua akibatnya:
	 * (1) nilai yang baru saja diisi lewat {@link #setAlamat(String)} bisa hilang pada pembacaan
	 * berikutnya, dan (2) karena entity dipetakan {@code dynamicUpdate = true}, penimpaan pada
	 * object yang sedang <i>managed</i> memicu {@code UPDATE} kolom {@code alamat} saat flush,
	 * meski pemanggil merasa hanya membaca.</p>
	 *
	 * <p><b>Kuirk:</b> penimpaan dilakukan <b>tanpa memeriksa null</b>. Bila alamat dosen kosong,
	 * membaca getter ini akan <i>mengosongkan</i> alamat yang sudah telanjur tersimpan di
	 * {@code biodata_dosen}. Bandingkan {@link #getTeleponRumah()} dan {@link #getHp()} yang
	 * menjaga kondisinya.</p>
	 *
	 * @return alamat tempat tinggal, mengikuti {@link Dosen} bila relasinya ada
	 */
	@Column(name = "alamat")
	public String getAlamat() {

		if (getDosen() != null) {
			alamat = getDosen().getAlamat();
		}

		return this.alamat;
	}

	/**
	 * Mengisi alamat tempat tinggal dosen. Perhatikan bahwa {@link #getAlamat()} akan menimpanya
	 * kembali dari {@link Dosen} pada pembacaan berikutnya bila relasi dosen terisi.
	 *
	 * @param alamat alamat tempat tinggal
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan nama ayah kandung dosen (dipakai formulir biodata; ekspor Feeder memakai nama
	 * ibu, bukan ayah).
	 *
	 * @return nama ayah, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_ayah", length = 100)
	public String getNamaAyah() {
		return this.namaAyah;
	}

	/**
	 * Mengisi nama ayah kandung dosen.
	 *
	 * @param namaAyah nama ayah
	 */
	public void setNamaAyah(String namaAyah) {
		this.namaAyah = namaAyah;
	}

	/**
	 * Mengembalikan pekerjaan ayah dosen sebagai referensi ke master
	 * {@link PekerjaanOrangTua}. Proxy lazy diresolusi lebih dulu lewat
	 * {@link GeneralValueObject#check(Object)} agar tetap aman dipanggil pada entity yang sudah
	 * detached.
	 *
	 * @return master pekerjaan ayah, atau {@code null} bila belum dipilih
	 * @see GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "id_pekerjaan_ayah", nullable = true)
	public PekerjaanOrangTua getPekerjaanAyah() {
		pekerjaanAyah = check(pekerjaanAyah);
		return this.pekerjaanAyah;
	}

	/**
	 * Mengisi pekerjaan ayah dosen. Relasi memakai {@code cascade = {PERSIST, MERGE}}, jadi master
	 * pekerjaan yang belum tersimpan akan ikut disimpan saat biodata ini di-{@code persist}.
	 *
	 * @param pekerjaanAyah master pekerjaan ayah
	 */
	public void setPekerjaanAyah(PekerjaanOrangTua pekerjaanAyah) {
		this.pekerjaanAyah = pekerjaanAyah;
	}

	/**
	 * Mengembalikan nama ibu kandung dosen. Properti ini ikut diekspor ke Feeder sebagai
	 * {@code nm_ibu_kandung} (dipotong maksimal 50 karakter).
	 *
	 * @return nama ibu, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_ibu", length = 100)
	public String getNamaIbu() {
		return this.namaIbu;
	}

	/**
	 * Mengisi nama ibu kandung dosen.
	 *
	 * @param namaIbu nama ibu
	 */
	public void setNamaIbu(String namaIbu) {
		this.namaIbu = namaIbu;
	}

	/**
	 * Mengembalikan pekerjaan ibu dosen sebagai referensi ke master {@link PekerjaanOrangTua},
	 * dengan resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return master pekerjaan ibu, atau {@code null} bila belum dipilih
	 * @see GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "id_pekerjaan_ibu", nullable = true)
	public PekerjaanOrangTua getPekerjaanIbu() {
		pekerjaanIbu = check(pekerjaanIbu);
		return this.pekerjaanIbu;
	}

	/**
	 * Mengisi pekerjaan ibu dosen.
	 *
	 * @param pekerjaanIbu master pekerjaan ibu
	 */
	public void setPekerjaanIbu(PekerjaanOrangTua pekerjaanIbu) {
		this.pekerjaanIbu = pekerjaanIbu;
	}

	/**
	 * Mengembalikan tinggi badan dosen dalam sentimeter.
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, nilai {@code 0} <b>ditulis ke field</b>
	 * sebelum dikembalikan, sehingga default itu ikut tersimpan pada flush berikutnya. Getter ini
	 * karena itu tidak pernah mengembalikan {@code null} &mdash; pemanggil tidak bisa membedakan
	 * "belum diisi" dari "diisi nol".</p>
	 *
	 * @return tinggi badan dalam cm; {@code 0} bila belum diisi
	 */
	@Column(name = "tinggi_badan")
	public Integer getTinggiBadan() {
		if (tinggiBadan == null) {
			tinggiBadan = 0;
		}
		return this.tinggiBadan;
	}

	/**
	 * Mengisi tinggi badan dosen dalam sentimeter.
	 *
	 * @param tinggiBadan tinggi badan (cm)
	 */
	public void setTinggiBadan(Integer tinggiBadan) {
		this.tinggiBadan = tinggiBadan;
	}

	/**
	 * Mengembalikan penanda apakah dosen pernah menetap di luar negeri (konvensi biner
	 * {@code 0}/{@code 1} seperti properti "pernah*" lain di keluarga biodata). Tidak diberi nilai
	 * default, jadi dapat {@code null} bila belum pernah diisi.
	 *
	 * @return penanda pernah menetap di luar negeri, atau {@code null} bila belum diisi
	 */
	@Column(name = "pernah_menetap_di_luar_negeri")
	public Integer getPernahMenetapDiLuarNegeri() {
		return this.pernahMenetapDiLuarNegeri;
	}

	/**
	 * Mengisi penanda pernah menetap di luar negeri.
	 *
	 * @param pernahMenetapDiLuarNegeri penanda biner ({@code 0} tidak, {@code 1} pernah)
	 */
	public void setPernahMenetapDiLuarNegeri(Integer pernahMenetapDiLuarNegeri) {
		this.pernahMenetapDiLuarNegeri = pernahMenetapDiLuarNegeri;
	}

	/**
	 * Mengembalikan berat badan dosen dalam kilogram.
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getTinggiBadan()} &mdash; bila field masih
	 * {@code null}, nilai {@code 0} ditulis ke field sehingga ikut tersimpan pada flush
	 * berikutnya.</p>
	 *
	 * @return berat badan dalam kg; {@code 0} bila belum diisi
	 */
	@Column(name = "berat_badan")
	public Integer getBeratBadan() {
		if (beratBadan == null) {
			beratBadan = 0;
		}
		return this.beratBadan;
	}

	/**
	 * Mengisi berat badan dosen dalam kilogram.
	 *
	 * @param beratBadan berat badan (kg)
	 */
	public void setBeratBadan(Integer beratBadan) {
		this.beratBadan = beratBadan;
	}

	/**
	 * Mengembalikan nomor telepon rumah dosen; diekspor ke Feeder sebagai {@code no_tel_rmh}.
	 *
	 * <p><b>Bukan getter murni.</b> Bila field lokal masih kosong dan relasi {@link Dosen} terisi,
	 * nilai {@code Dosen.getTelp()} <b>disalin ke field {@code teleponRumah}</b> sehingga ikut
	 * tersimpan pada flush berikutnya. Berbeda dengan {@link #getAlamat()}, penyalinan di sini
	 * hanya terjadi ketika nilai lokal benar-benar kosong, jadi isian manual tidak tertimpa.</p>
	 *
	 * <p>Perhatikan pula bahwa method ini menulis ulang field {@code dosen} dari
	 * {@link #getDosen()} (mengambil hasil resolusi proxy lazy) &mdash; efek yang sama sekali tidak
	 * terlihat dari namanya.</p>
	 *
	 * @return nomor telepon rumah, jatuh balik ke nomor telepon pada {@link Dosen}
	 */
	@Column(name = "telepon_rumah", length = 200)
	public String getTeleponRumah() {
		dosen = getDosen();
		if (dosen != null && (teleponRumah == null || teleponRumah.trim().isEmpty())) {
			teleponRumah = dosen.getTelp();
		}
		return this.teleponRumah;
	}

	/**
	 * Mengisi nomor telepon rumah dosen.
	 *
	 * @param teleponRumah nomor telepon rumah
	 */
	public void setTeleponRumah(String teleponRumah) {
		this.teleponRumah = teleponRumah;
	}

	/**
	 * Mengembalikan nomor HP dosen; diekspor ke Feeder sebagai {@code no_hp}.
	 *
	 * <p><b>Bukan getter murni.</b> Bila {@code Dosen.getHp()} terisi, nilainya <b>selalu
	 * menimpa</b> field {@code hp} milik object ini &mdash; {@link Dosen} adalah sumber kebenaran
	 * nomor HP, dan isian manual pada formulir biodata akan tergeser pada pembacaan berikutnya.
	 * Penimpaan itu ikut tersimpan saat flush. Ketika HP dosen kosong, nilai lokal dibiarkan apa
	 * adanya.</p>
	 *
	 * <p>Catatan: {@code Dosen.getHp()} sendiri sudah membersihkan seluruh karakter non-digit,
	 * jadi nilai yang tersalin ke sini selalu berupa angka saja. Method ini juga menulis ulang
	 * field {@code dosen} dari {@link #getDosen()}.</p>
	 *
	 * @return nomor HP, mengikuti {@link Dosen} bila dosen punya nomor HP
	 */
	@Column(name = "hp", length = 200)
	public String getHp() {
		dosen = getDosen();
		if (dosen != null && dosen.getHp() != null && !dosen.getHp().isEmpty()) {
			hp = dosen.getHp();
		}
		return this.hp;
	}

	/**
	 * Mengisi nomor HP dosen. Perhatikan bahwa {@link #getHp()} akan menimpanya kembali dari
	 * {@link Dosen} bila dosen punya nomor HP.
	 *
	 * @param hp nomor HP
	 */
	public void setHp(String hp) {
		this.hp = hp;
	}

	/**
	 * Mengembalikan nomor Surat Izin Mengemudi dosen.
	 *
	 * <p>Catatan pembanding: getter senama di {@link BiodataPegawai} keliru menyalin nomor HP,
	 * bukan nomor SIM. Versi di kelas ini benar &mdash; getter murni tanpa efek samping.</p>
	 *
	 * @return nomor SIM, atau {@code null} bila belum diisi
	 */
	@Column(name = "surat_izin_mengemudi", length = 50)
	public String getSuratIzinMengemudi() {
		return this.suratIzinMengemudi;
	}

	/**
	 * Mengisi nomor Surat Izin Mengemudi dosen.
	 *
	 * @param suratIzinMengemudi nomor SIM
	 */
	public void setSuratIzinMengemudi(String suratIzinMengemudi) {
		this.suratIzinMengemudi = suratIzinMengemudi;
	}

	/**
	 * Mengembalikan jenis kendaraan yang dipakai dosen menuju kampus (isian bebas pada formulir
	 * biodata; nama propertinya warisan dari template biodata mahasiswa).
	 *
	 * @return keterangan kendaraan, atau {@code null} bila belum diisi
	 */
	@Column(name = "kendaraan_kuliah", length = 50)
	public String getKendaraanKuliah() {
		return this.kendaraanKuliah;
	}

	/**
	 * Mengisi jenis kendaraan yang dipakai dosen menuju kampus.
	 *
	 * @param kendaraanKuliah keterangan kendaraan
	 */
	public void setKendaraanKuliah(String kendaraanKuliah) {
		this.kendaraanKuliah = kendaraanKuliah;
	}

	/**
	 * Mengembalikan penanda apakah dosen pernah memimpin organisasi (konvensi biner
	 * {@code 0}/{@code 1}). Tidak diberi default, jadi dapat {@code null}.
	 *
	 * @return penanda pernah memimpin organisasi, atau {@code null} bila belum diisi
	 */
	@Column(name = "pernah_memimpin_organisasi")
	public Integer getPernahMemimpinOrganisasi() {
		return this.pernahMemimpinOrganisasi;
	}

	/**
	 * Mengisi penanda pernah memimpin organisasi.
	 *
	 * @param pernahMemimpinOrganisasi penanda biner ({@code 0} tidak, {@code 1} pernah)
	 */
	public void setPernahMemimpinOrganisasi(Integer pernahMemimpinOrganisasi) {
		this.pernahMemimpinOrganisasi = pernahMemimpinOrganisasi;
	}

	/**
	 * Mengembalikan nama organisasi yang pernah dipimpin/diikuti dosen.
	 *
	 * @return nama organisasi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_organisasi", length = 50)
	public String getNamaOrganisasi() {
		return this.namaOrganisasi;
	}

	/**
	 * Mengisi nama organisasi yang pernah dipimpin/diikuti dosen.
	 *
	 * @param namaOrganisasi nama organisasi
	 */
	public void setNamaOrganisasi(String namaOrganisasi) {
		this.namaOrganisasi = namaOrganisasi;
	}

	/**
	 * Mengembalikan hobi dosen (isian bebas).
	 *
	 * @return hobi, atau {@code null} bila belum diisi
	 */
	@Column(name = "hobi")
	public String getHobi() {
		return this.hobi;
	}

	/**
	 * Mengisi hobi dosen.
	 *
	 * @param hobi keterangan hobi
	 */
	public void setHobi(String hobi) {
		this.hobi = hobi;
	}

	/**
	 * Mengembalikan minat seni dosen (isian bebas).
	 *
	 * @return minat seni, atau {@code null} bila belum diisi
	 */
	@Column(name = "minat_seni")
	public String getMinatSeni() {
		return this.minatSeni;
	}

	/**
	 * Mengisi minat seni dosen.
	 *
	 * @param minatSeni keterangan minat seni
	 */
	public void setMinatSeni(String minatSeni) {
		this.minatSeni = minatSeni;
	}

	/**
	 * Mengembalikan kemampuan bahasa slot ke-1. Ketiga slot bahasa ({@code kemampuan_bahasa1}..
	 * {@code kemampuan_bahasa3}) adalah isian bebas tanpa master, jadi urutannya tidak punya makna
	 * khusus selain urutan pengisian pada formulir.
	 *
	 * @return kemampuan bahasa ke-1, atau {@code null} bila belum diisi
	 */
	@Column(name = "kemampuan_bahasa1", length = 50)
	public String getKemampuanBahasa1() {
		return this.kemampuanBahasa1;
	}

	/**
	 * Mengisi kemampuan bahasa slot ke-1.
	 *
	 * @param kemampuanBahasa1 keterangan kemampuan bahasa
	 */
	public void setKemampuanBahasa1(String kemampuanBahasa1) {
		this.kemampuanBahasa1 = kemampuanBahasa1;
	}

	/**
	 * Mengembalikan kemampuan bahasa slot ke-2.
	 *
	 * @return kemampuan bahasa ke-2, atau {@code null} bila belum diisi
	 * @see #getKemampuanBahasa1()
	 */
	@Column(name = "kemampuan_bahasa2", length = 50)
	public String getKemampuanBahasa2() {
		return this.kemampuanBahasa2;
	}

	/**
	 * Mengisi kemampuan bahasa slot ke-2.
	 *
	 * @param kemampuanBahasa2 keterangan kemampuan bahasa
	 */
	public void setKemampuanBahasa2(String kemampuanBahasa2) {
		this.kemampuanBahasa2 = kemampuanBahasa2;
	}

	/**
	 * Mengembalikan kemampuan bahasa slot ke-3.
	 *
	 * @return kemampuan bahasa ke-3, atau {@code null} bila belum diisi
	 * @see #getKemampuanBahasa1()
	 */
	@Column(name = "kemampuan_bahasa3", length = 50)
	public String getKemampuanBahasa3() {
		return this.kemampuanBahasa3;
	}

	/**
	 * Mengisi kemampuan bahasa slot ke-3.
	 *
	 * @param kemampuanBahasa3 keterangan kemampuan bahasa
	 */
	public void setKemampuanBahasa3(String kemampuanBahasa3) {
		this.kemampuanBahasa3 = kemampuanBahasa3;
	}

	/**
	 * Mengembalikan nama SMA/sederajat asal dosen, sudah dibersihkan: string di-{@code trim} dan
	 * seluruh tanda kutip tunggal ({@code '}) maupun ganda ({@code "}) dibuang; {@code null}
	 * dikembalikan sebagai string kosong.
	 *
	 * <p>Pembersihan kutip ini bukan kosmetik &mdash; ia melindungi laporan/ekspor yang masih
	 * merangkai SQL atau CSV dengan penyambungan string. Karena pembersihan dilakukan pada nilai
	 * yang dikembalikan dan <b>tidak</b> ditulis balik ke field, getter ini asimetris terhadap
	 * setternya: {@code setAsalSma("SMA 'X'")} lalu {@link #getAsalSma()} mengembalikan
	 * {@code "SMA X"}, sementara yang tersimpan di basis data tetap versi bertanda kutip.</p>
	 *
	 * @return nama SMA asal tanpa tanda kutip; string kosong bila belum diisi (tidak pernah
	 *         {@code null})
	 */
	@Column(name = "asal_sma", length = 50)
	public String getAsalSma() {
		return this.asalSma == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSma.trim(), "'", ""), "\"", "");
	}

	/**
	 * Mengisi nama SMA/sederajat asal dosen apa adanya; pembersihan tanda kutip dilakukan saat
	 * dibaca.
	 *
	 * @param asalSma nama SMA asal
	 * @see #getAsalSma()
	 */
	public void setAsalSma(String asalSma) {
		this.asalSma = asalSma;
	}

	/**
	 * Mengembalikan alamat SMA/sederajat asal dosen. Berbeda dari {@link #getAsalSma()}, alamat
	 * dikembalikan apa adanya tanpa pembersihan tanda kutip.
	 *
	 * @return alamat SMA asal, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_sma")
	public String getAlamatAsalSma() {
		return this.alamatAsalSma;
	}

	/**
	 * Mengisi alamat SMA/sederajat asal dosen.
	 *
	 * @param alamatAsalSma alamat SMA asal
	 */
	public void setAlamatAsalSma(String alamatAsalSma) {
		this.alamatAsalSma = alamatAsalSma;
	}

	/**
	 * Mengembalikan nama SMP/sederajat asal dosen, dibersihkan dengan aturan yang sama seperti
	 * {@link #getAsalSma()}.
	 *
	 * @return nama SMP asal tanpa tanda kutip; string kosong bila belum diisi
	 * @see #getAsalSma()
	 */
	@Column(name = "asal_smp", length = 50)
	public String getAsalSmp() {
		return this.asalSmp == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSmp.trim(), "'", ""), "\"", "");
	}

	/**
	 * Mengisi nama SMP/sederajat asal dosen apa adanya.
	 *
	 * @param asalSmp nama SMP asal
	 */
	public void setAsalSmp(String asalSmp) {
		this.asalSmp = asalSmp;
	}

	/**
	 * Mengembalikan alamat SMP/sederajat asal dosen, apa adanya.
	 *
	 * @return alamat SMP asal, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_smp")
	public String getAlamatAsalSmp() {
		return this.alamatAsalSmp;
	}

	/**
	 * Mengisi alamat SMP/sederajat asal dosen.
	 *
	 * @param alamatAsalSmp alamat SMP asal
	 */
	public void setAlamatAsalSmp(String alamatAsalSmp) {
		this.alamatAsalSmp = alamatAsalSmp;
	}

	/**
	 * Mengembalikan nama SD/sederajat asal dosen, dibersihkan dengan aturan yang sama seperti
	 * {@link #getAsalSma()}.
	 *
	 * @return nama SD asal tanpa tanda kutip; string kosong bila belum diisi
	 * @see #getAsalSma()
	 */
	@Column(name = "asal_sd", length = 50)
	public String getAsalSd() {
		return this.asalSd == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSd.trim(), "'", ""), "\"", "");
	}

	/**
	 * Mengisi nama SD/sederajat asal dosen apa adanya.
	 *
	 * @param asalSd nama SD asal
	 */
	public void setAsalSd(String asalSd) {
		this.asalSd = asalSd;
	}

	/**
	 * Mengembalikan alamat SD/sederajat asal dosen, apa adanya.
	 *
	 * @return alamat SD asal, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_sd")
	public String getAlamatAsalSd() {
		return this.alamatAsalSd;
	}

	/**
	 * Mengisi alamat SD/sederajat asal dosen.
	 *
	 * @param alamatAsalSd alamat SD asal
	 */
	public void setAlamatAsalSd(String alamatAsalSd) {
		this.alamatAsalSd = alamatAsalSd;
	}

	/**
	 * Mengembalikan golongan darah dosen sebagai teks bebas (mis. {@code "O"}, {@code "AB+"});
	 * tidak ada master maupun validasi nilai.
	 *
	 * @return golongan darah, atau {@code null} bila belum diisi
	 */
	@Column(name = "golongan_darah", length = 10)
	public String getGolonganDarah() {
		return this.golonganDarah;
	}

	/**
	 * Mengisi golongan darah dosen.
	 *
	 * @param golonganDarah golongan darah
	 */
	public void setGolonganDarah(String golonganDarah) {
		this.golonganDarah = golonganDarah;
	}

	/**
	 * Mengembalikan status pernikahan dosen. Konvensi nilainya biner &mdash; ekspor Feeder
	 * menerjemahkannya sebagai {@code stat_kawin = getStatusNikah().equals(0) ? 0 : 1}, jadi
	 * {@code 0} berarti belum menikah dan nilai lain berarti menikah.
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, nilai {@code 0} <b>ditulis ke field</b>
	 * sehingga ikut tersimpan pada flush berikutnya. Akibat lanjutannya, pemeriksaan kelengkapan
	 * {@code BiodataDosenAction.checkBiodataDosen(Dosen)} yang mewajibkan properti
	 * {@code "statusNikah"} terisi <b>tidak pernah bisa gagal</b>: validasi membaca nilai lewat
	 * {@code ClassMetadata.getPropertyValue(...)}, yang pada pemetaan property-access memanggil
	 * getter ini dan selalu menerima angka, bukan {@code null}.</p>
	 *
	 * @return status pernikahan; {@code 0} bila belum diisi
	 */
	@Column(name = "status_nikah")
	public Integer getStatusNikah() {
		if (statusNikah == null) {
			statusNikah = 0;
		}
		return this.statusNikah;
	}

	/**
	 * Mengisi status pernikahan dosen.
	 *
	 * @param statusNikah {@code 0} belum menikah, nilai lain menikah
	 */
	public void setStatusNikah(Integer statusNikah) {
		this.statusNikah = statusNikah;
	}

	/**
	 * Mengembalikan kewarganegaraan dosen dalam notasi internal AIS ({@code "WNI"}/{@code "WNA"}).
	 * Untuk ekspor Feeder dipakai properti terpisah {@link #getKewarganegaraanFeeder()} yang
	 * memakai kode negara dua huruf.
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null} atau kosong, nilai {@link Mahasiswa#WNI}
	 * ({@code "WNI"}) ditulis ke field sehingga ikut tersimpan pada flush berikutnya. Konstantanya
	 * sengaja dipinjam dari {@link Mahasiswa} agar notasi seragam lintas entity biodata.</p>
	 *
	 * @return kewarganegaraan; {@code "WNI"} bila belum diisi
	 */
	@Column(name = "kewarganegaraan", length = 10)
	public String getKewarganegaraan() {
		if (kewarganegaraan == null || kewarganegaraan.trim().isEmpty()) {
			kewarganegaraan = Mahasiswa.WNI;
		}
		return this.kewarganegaraan;
	}

	/**
	 * Mengisi kewarganegaraan dosen dalam notasi internal AIS.
	 *
	 * @param kewarganegaraan {@code "WNI"} atau {@code "WNA"}
	 */
	public void setKewarganegaraan(String kewarganegaraan) {
		this.kewarganegaraan = kewarganegaraan;
	}

	/**
	 * Mengembalikan agama dosen sebagai referensi ke master {@link Agama}, dengan resolusi proxy
	 * lazy lewat {@link GeneralValueObject#check(Object)}. Ekspor Feeder memakai
	 * {@code getAgama().getFeeder()} dan jatuh balik ke kode {@code 1} bila relasi ini kosong.
	 *
	 * @return master agama, atau {@code null} bila belum dipilih
	 * @see GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "id_agama", nullable = true)
	public Agama getAgama() {
		agama = check(agama);
		return this.agama;
	}

	/**
	 * Mengisi agama dosen.
	 *
	 * @param agama master agama
	 */
	public void setAgama(Agama agama) {
		this.agama = agama;
	}

	/**
	 * Mengisi dosen pemilik biodata ini. Kolom {@code dosen} bersifat {@code nullable = false},
	 * jadi relasi ini wajib diisi sebelum baris disimpan. Pada alur normal, pengisian dilakukan
	 * oleh {@link Dosen#ambilBiodata(boolean)} saat ia membuat baris biodata kosong.
	 *
	 * @param dosen dosen pemilik biodata
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan dosen pemilik biodata ini &mdash; satu-satunya relasi wajib pada entity ini
	 * ({@code nullable = false}). Proxy lazy diresolusi lebih dulu lewat
	 * {@link GeneralValueObject#check(Object)}, sehingga getter ini aman dipanggil pada entity yang
	 * sudah lepas dari {@link org.hibernate.Session} (bandingkan {@link #toString()} yang tidak
	 * aman karena membaca field langsung).
	 *
	 * <p>Perhatikan bahwa tidak ada relasi balik {@code @OneToOne} di {@link Dosen}; pencarian
	 * biodata dari sisi dosen dilakukan {@link Dosen#ambilBiodata(boolean)} lewat {@code Criteria}
	 * atas kolom ini, dan karena kolomnya tidak {@code unique}, skema memperbolehkan lebih dari
	 * satu baris biodata per dosen (yang dipakai adalah baris ber-ID terbesar).</p>
	 *
	 * @return dosen pemilik biodata; secara praktis tidak pernah {@code null} pada baris tersimpan
	 * @see Dosen#ambilBiodata()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Mengisi nama perguruan tinggi asal jenjang S1 dosen.
	 *
	 * @param asalS1 nama perguruan tinggi S1
	 */
	public void setAsalS1(String asalS1) {
		this.asalS1 = asalS1;
	}

	/**
	 * Mengembalikan nama perguruan tinggi asal jenjang S1 dosen. Tidak dibersihkan tanda kutip
	 * (berbeda dari {@link #getAsalSma()}) dan dapat {@code null}.
	 *
	 * @return nama perguruan tinggi S1, atau {@code null} bila belum diisi
	 */
	@Column(name = "asal_s1", length = 100)
	public String getAsalS1() {
		return asalS1;
	}

	/**
	 * Mengisi alamat perguruan tinggi asal jenjang S1 dosen.
	 *
	 * @param alamatAsalS1 alamat perguruan tinggi S1
	 */
	public void setAlamatAsalS1(String alamatAsalS1) {
		this.alamatAsalS1 = alamatAsalS1;
	}

	/**
	 * Mengembalikan alamat perguruan tinggi asal jenjang S1 dosen.
	 *
	 * @return alamat perguruan tinggi S1, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_s1", length = 255)
	public String getAlamatAsalS1() {
		return alamatAsalS1;
	}

	/**
	 * Mengisi nama perguruan tinggi asal jenjang S2 dosen.
	 *
	 * @param asalS2 nama perguruan tinggi S2
	 */
	public void setAsalS2(String asalS2) {
		this.asalS2 = asalS2;
	}

	/**
	 * Mengembalikan nama perguruan tinggi asal jenjang S2 dosen.
	 *
	 * @return nama perguruan tinggi S2, atau {@code null} bila belum diisi
	 */
	@Column(name = "asal_s2", length = 100)
	public String getAsalS2() {
		return asalS2;
	}

	/**
	 * Mengisi alamat perguruan tinggi asal jenjang S2 dosen.
	 *
	 * <p><b>Kuirk (jangan ditiru):</b> anotasi {@code @Column(name = "alamat_asal_s2")} terpasang
	 * pada setter ini, bukan pada {@link #getAlamatAsalS2()}. Entity ini memakai <i>property
	 * access</i> ({@code @Id} berada di {@link #getId()}), jadi Hibernate hanya membaca anotasi
	 * dari getter dan anotasi di sini <b>diabaikan sepenuhnya</b>. Akibatnya properti
	 * {@code alamatAsalS2} jatuh ke penamaan default {@code MyNamingStrategy} (turunan
	 * {@code DefaultNamingStrategy}, nama kolom = nama properti apa adanya), sehingga kolom
	 * fisiknya {@code alamatAsalS2} &mdash; bukan {@code alamat_asal_s2} seperti saudara-saudaranya
	 * {@code alamat_asal_s1} dan {@code alamat_asal_s3}. Bug yang sama persis juga ada di
	 * {@link BiodataPegawai}. Tidak diperbaiki di sini karena mengubahnya berarti mengubah nama
	 * kolom yang sudah berisi data.</p>
	 *
	 * @param alamatAsalS2 alamat perguruan tinggi S2
	 */
	@Column(name = "alamat_asal_s2", length = 255)
	public void setAlamatAsalS2(String alamatAsalS2) {
		this.alamatAsalS2 = alamatAsalS2;
	}

	/**
	 * Mengembalikan alamat perguruan tinggi asal jenjang S2 dosen.
	 *
	 * @return alamat perguruan tinggi S2, atau {@code null} bila belum diisi
	 * @see #setAlamatAsalS2(String) untuk catatan anotasi {@code @Column} yang salah tempat
	 */
	public String getAlamatAsalS2() {
		return alamatAsalS2;
	}

	/**
	 * Mengisi nama perguruan tinggi asal jenjang S3 dosen.
	 *
	 * @param asalS3 nama perguruan tinggi S3
	 */
	public void setAsalS3(String asalS3) {
		this.asalS3 = asalS3;
	}

	/**
	 * Mengembalikan nama perguruan tinggi asal jenjang S3 dosen.
	 *
	 * @return nama perguruan tinggi S3, atau {@code null} bila belum diisi
	 */
	@Column(name = "asal_s3", length = 100)
	public String getAsalS3() {
		return asalS3;
	}

	/**
	 * Mengisi alamat perguruan tinggi asal jenjang S3 dosen.
	 *
	 * @param alamatAsalS3 alamat perguruan tinggi S3
	 */
	public void setAlamatAsalS3(String alamatAsalS3) {
		this.alamatAsalS3 = alamatAsalS3;
	}

	/**
	 * Mengembalikan alamat perguruan tinggi asal jenjang S3 dosen.
	 *
	 * @return alamat perguruan tinggi S3, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_s3", length = 255)
	public String getAlamatAsalS3() {
		return alamatAsalS3;
	}

	/**
	 * Mengisi bidang keahlian slot ke-1 dosen.
	 *
	 * <p>Nama properti sengaja dibiarkan salah eja ({@code keahliah1}, bukan {@code keahlian1});
	 * ejaan itu sudah telanjur dipakai lapisan UI dan tidak bisa diganti tanpa menyentuh
	 * pemanggil.</p>
	 *
	 * @param keahliah1 keterangan bidang keahlian
	 */
	public void setKeahliah1(String keahliah1) {
		this.keahliah1 = keahliah1;
	}

	/**
	 * Mengembalikan bidang keahlian slot ke-1 dosen. Lima slot keahlian
	 * ({@code keahliah1}..{@code keahliah5}) adalah isian bebas tanpa master; urutannya tidak punya
	 * makna khusus.
	 *
	 * <p><b>Catatan penamaan:</b> ini satu-satunya slot yang <i>nama propertinya</i> ikut salah eja
	 * ({@code keahliah1}); slot 2..5 bernama {@code keahlian2}..{@code keahlian5}. Nama
	 * <i>kolomnya</i> justru konsisten salah eja untuk kelimanya:
	 * {@code keahliah1}..{@code keahliah5}.</p>
	 *
	 * @return bidang keahlian ke-1, atau {@code null} bila belum diisi
	 */
	@Column(name = "keahliah1", length = 100)
	public String getKeahliah1() {
		return keahliah1;
	}

	/**
	 * Mengisi bidang keahlian slot ke-2 dosen.
	 *
	 * @param keahlian2 keterangan bidang keahlian
	 */
	public void setKeahlian2(String keahlian2) {
		this.keahlian2 = keahlian2;
	}

	/**
	 * Mengembalikan bidang keahlian slot ke-2 dosen (kolom {@code keahliah2}).
	 *
	 * @return bidang keahlian ke-2, atau {@code null} bila belum diisi
	 * @see #getKeahliah1()
	 */
	@Column(name = "keahliah2", length = 100)
	public String getKeahlian2() {
		return keahlian2;
	}

	/**
	 * Mengisi bidang keahlian slot ke-3 dosen.
	 *
	 * @param keahlian3 keterangan bidang keahlian
	 */
	public void setKeahlian3(String keahlian3) {
		this.keahlian3 = keahlian3;
	}

	/**
	 * Mengembalikan bidang keahlian slot ke-3 dosen (kolom {@code keahliah3}).
	 *
	 * @return bidang keahlian ke-3, atau {@code null} bila belum diisi
	 * @see #getKeahliah1()
	 */
	@Column(name = "keahliah3", length = 100)
	public String getKeahlian3() {
		return keahlian3;
	}

	/**
	 * Mengisi bidang keahlian slot ke-4 dosen.
	 *
	 * @param keahlian4 keterangan bidang keahlian
	 */
	public void setKeahlian4(String keahlian4) {
		this.keahlian4 = keahlian4;
	}

	/**
	 * Mengembalikan bidang keahlian slot ke-4 dosen (kolom {@code keahliah4}).
	 *
	 * @return bidang keahlian ke-4, atau {@code null} bila belum diisi
	 * @see #getKeahliah1()
	 */
	@Column(name = "keahliah4", length = 100)
	public String getKeahlian4() {
		return keahlian4;
	}

	/**
	 * Mengisi bidang keahlian slot ke-5 dosen.
	 *
	 * @param keahlian5 keterangan bidang keahlian
	 */
	public void setKeahlian5(String keahlian5) {
		this.keahlian5 = keahlian5;
	}

	/**
	 * Mengembalikan bidang keahlian slot ke-5 dosen (kolom {@code keahliah5}).
	 *
	 * @return bidang keahlian ke-5, atau {@code null} bila belum diisi
	 * @see #getKeahliah1()
	 */
	@Column(name = "keahliah5", length = 100)
	public String getKeahlian5() {
		return keahlian5;
	}

	/**
	 * Mengisi nomor KTP dosen pada baris biodata. Perhatikan bahwa {@link #getNoKtp()} akan
	 * menimpanya kembali dari {@link Dosen} pada pembacaan berikutnya.
	 *
	 * @param noKtp nomor KTP/NIK
	 */
	public void setNoKtp(String noKtp) {
		this.noKtp = noKtp;
	}

	/**
	 * Mengembalikan nomor KTP/NIK dosen.
	 *
	 * <p><b>Bukan getter murni.</b> Bila relasi {@link Dosen} terisi, nilai {@code Dosen.getKtp()}
	 * <b>selalu ditimpakan</b> ke field {@code noKtp} milik object ini &mdash; {@link Dosen} adalah
	 * sumber kebenaran nomor KTP. Penimpaan pada object yang sedang <i>managed</i> memicu
	 * {@code UPDATE} kolom {@code no_ktp} saat flush.</p>
	 *
	 * <p><b>Kuirk:</b> seperti {@link #getAlamat()}, penimpaan dilakukan <b>tanpa memeriksa
	 * null</b>. Bila KTP pada entity dosen kosong, membaca getter ini akan <i>mengosongkan</i>
	 * nomor KTP yang sudah telanjur tersimpan di {@code biodata_dosen}. Bandingkan
	 * {@link #getNoIdentitas()} yang membaca sumber yang sama namun menjaga kondisinya.</p>
	 *
	 * @return nomor KTP/NIK, mengikuti {@link Dosen} bila relasinya ada
	 */
	@Column(name = "no_ktp")
	public String getNoKtp() {
		if (getDosen() != null) {
			noKtp = getDosen().getKtp();
		}
		return noKtp;
	}

	/**
	 * Mengisi gelar akademik/profesional dosen (isian bebas, mis. {@code "Prof."}). Nama properti
	 * disingkat {@code gelarAkademikProf} sementara kolomnya bernama lengkap
	 * {@code gelar_akademik_profesional}.
	 *
	 * @param gelarAkademikProf gelar akademik atau profesional
	 */
	public void setGelarAkademikProf(String gelarAkademikProf) {
		this.gelarAkademikProf = gelarAkademikProf;
	}

	/**
	 * Mengembalikan gelar akademik/profesional dosen.
	 *
	 * @return gelar akademik/profesional, atau {@code null} bila belum diisi
	 */
	@Column(name = "gelar_akademik_profesional")
	public String getGelarAkademikProf() {
		return gelarAkademikProf;
	}

	/**
	 * Mengembalikan nomor RT tempat tinggal dosen. Diekspor ke Feeder sebagai {@code rt} dengan
	 * pemotongan numerik maksimal 2 digit.
	 *
	 * <p><b>Catatan pemetaan:</b> properti ini tidak dianotasi {@code @Column}, jadi nama kolomnya
	 * mengikuti default {@code MyNamingStrategy} &mdash; yaitu {@code rt} apa adanya.</p>
	 *
	 * @return nomor RT, atau {@code null} bila belum diisi
	 */
	public String getRt() {
		return rt;
	}

	/**
	 * Mengisi nomor RT tempat tinggal dosen.
	 *
	 * @param rt nomor RT
	 */
	public void setRt(String rt) {
		this.rt = rt;
	}

	/**
	 * Mengembalikan nomor RW tempat tinggal dosen. Diekspor ke Feeder sebagai {@code rw} dengan
	 * pemotongan numerik maksimal 2 digit. Tidak dianotasi {@code @Column}; kolomnya {@code rw}.
	 *
	 * @return nomor RW, atau {@code null} bila belum diisi
	 */
	public String getRw() {
		return rw;
	}

	/**
	 * Mengisi nomor RW tempat tinggal dosen.
	 *
	 * @param rw nomor RW
	 */
	public void setRw(String rw) {
		this.rw = rw;
	}

	/**
	 * Mengembalikan kode pos alamat dosen. Diekspor ke Feeder sebagai {@code kode_pos} dengan
	 * pemotongan numerik maksimal 5 digit. Tidak dianotasi {@code @Column}; kolomnya
	 * {@code kodepos}.
	 *
	 * @return kode pos, atau {@code null} bila belum diisi
	 */
	public String getKodepos() {
		return kodepos;
	}

	/**
	 * Mengisi kode pos alamat dosen.
	 *
	 * @param kodepos kode pos
	 */
	public void setKodepos(String kodepos) {
		this.kodepos = kodepos;
	}

	/**
	 * Mengembalikan nama desa/kelurahan tempat tinggal dosen. Diekspor ke Feeder sebagai
	 * {@code ds_kel} (maksimal 40 karakter). Tidak dianotasi {@code @Column}; kolomnya
	 * {@code kelurahan}.
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null} atau kosong, tanda hubung {@code "-"}
	 * <b>ditulis ke field</b> sehingga ikut tersimpan pada flush berikutnya. Ini membuat kolom
	 * praktis tidak pernah kosong &mdash; dan sebagai akibat lanjutan, pemeriksaan kelengkapan
	 * {@code BiodataDosenAction.checkBiodataDosen(Dosen)} yang mewajibkan properti
	 * {@code "kelurahan"} terisi <b>tidak pernah bisa gagal</b>, karena validasi membaca nilai
	 * lewat getter ini (via {@code ClassMetadata.getPropertyValue(...)}) dan selalu menerima
	 * {@code "-"} alih-alih {@code null}. Data Feeder pun akan berisi {@code "-"}, bukan nama
	 * kelurahan sebenarnya, bila pengguna tidak pernah mengisinya.</p>
	 *
	 * @return nama desa/kelurahan; {@code "-"} bila belum diisi
	 */
	public String getKelurahan() {
		if (kelurahan == null || kelurahan.trim().isEmpty()) {
			kelurahan = "-";
		}
		return kelurahan;
	}

	/**
	 * Mengisi nama desa/kelurahan tempat tinggal dosen.
	 *
	 * @param kelurahan nama desa/kelurahan
	 */
	public void setKelurahan(String kelurahan) {
		this.kelurahan = kelurahan;
	}

	/**
	 * Mengembalikan kecamatan tempat tinggal dosen sebagai referensi ke master {@link Wilayah}
	 * (kolom {@code kecamatan_wilayah}), dengan resolusi proxy lazy lewat
	 * {@link GeneralValueObject#check(Object)}. Kode wilayah Feeder-nya diekspor sebagai
	 * {@code id_wil}, dengan nilai cadangan {@code "000000"} bila relasi ini kosong.
	 *
	 * <p><b>Perbedaan dengan {@link BiodataMahasiswa}:</b> getter senama di sana melakukan
	 * penelusuran ulang cache {@link Wilayah} untuk menukar wilayah tanpa induk dengan wilayah
	 * ber-induk yang kode Feeder-nya sama. Versi di sini <b>tidak</b> punya logika itu &mdash;
	 * murni {@code check()} saja, tanpa query dan tanpa penulisan master baru.</p>
	 *
	 * @return master wilayah kecamatan, atau {@code null} bila belum dipilih
	 * @see GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kecamatan_wilayah", nullable = true)
	public Wilayah getKecamatan() {
		kecamatan = check(kecamatan);
		return kecamatan;
	}

	/**
	 * Mengisi kecamatan tempat tinggal dosen.
	 *
	 * @param kecamatan master wilayah kecamatan
	 */
	public void setKecamatan(Wilayah kecamatan) {
		this.kecamatan = kecamatan;
	}

	/**
	 * Mengembalikan propinsi tempat tinggal dosen sebagai referensi ke master {@link Propinsi},
	 * dengan resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Turunan dari kota.</b> Bila {@link #getKota()} terisi dan kota itu punya propinsi,
	 * propinsi kota tersebut <b>menimpa</b> nilai propinsi milik object ini &mdash; kota
	 * diperlakukan sebagai sumber kebenaran, sehingga pasangan kota/propinsi tidak bisa
	 * inkonsisten. Penimpaan terjadi di memori pada field yang bisa sedang <i>managed</i>, jadi ia
	 * ikut tersimpan sebagai {@code UPDATE} kolom {@code propinsi} saat flush.</p>
	 *
	 * <p><b>Perbedaan penting dengan {@link BiodataMahasiswa}:</b> getter senama di sana, bila
	 * propinsi masih kosong, membuka {@link org.hibernate.Session} sendiri dan <b>membuat baris
	 * master {@code Propinsi} baru</b> ({@code findOrCreatePropinsi}) dari nama wilayah induk
	 * kecamatan, lalu menutup session milik thread pemanggil. Method ini <b>tidak</b> melakukan
	 * apa pun dari itu: tidak ada session, tidak ada transaksi, dan tidak ada penambahan baris
	 * master.</p>
	 *
	 * @return master propinsi, atau {@code null} bila belum dipilih dan tidak dapat diturunkan dari
	 *         kota
	 * @see GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi", nullable = true)
	public Propinsi getPropinsi() {
		propinsi = check(propinsi);
		kota = check(kota);
		if (kota != null && kota.getPropinsi() != null) {
			propinsi = kota.getPropinsi();
		}

		return propinsi;
	}

	/**
	 * Mengisi propinsi tempat tinggal dosen. Perhatikan bahwa {@link #getPropinsi()} akan
	 * menimpanya dari propinsi milik {@link #getKota()} bila kota terisi.
	 *
	 * @param propinsi master propinsi
	 */
	public void setPropinsi(Propinsi propinsi) {
		this.propinsi = propinsi;
	}

	/**
	 * Mengembalikan kota/kabupaten tempat tinggal dosen sebagai referensi ke master {@link Kota},
	 * dengan resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)}. Nilainya juga
	 * dipakai {@link #getPropinsi()} untuk menurunkan propinsi.
	 *
	 * <p><b>Perbedaan dengan {@link BiodataMahasiswa}:</b> getter senama di sana membuka session
	 * Hibernate dan <b>membuat baris master {@code Kota} baru</b> ({@code findOrCreateKota}, dengan
	 * pencocokan nama berbasis jarak Levenshtein) ketika kota belum terisi tetapi kecamatan punya
	 * wilayah induk. Method ini murni {@code check()} &mdash; tidak ada penulisan master baru dan
	 * tidak ada session yang dibuka maupun ditutup.</p>
	 *
	 * @return master kota/kabupaten, atau {@code null} bila belum dipilih
	 * @see GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota", nullable = true)
	public Kota getKota() {
		kota = check(kota);
		return kota;
	}

	/**
	 * Mengisi kota/kabupaten tempat tinggal dosen.
	 *
	 * @param kota master kota/kabupaten
	 */
	public void setKota(Kota kota) {
		this.kota = kota;
	}

	/**
	 * Mengembalikan nomor identitas (NIK/KTP) dosen. Properti ini termasuk sepuluh data yang
	 * diwajibkan {@code BiodataDosenAction.checkBiodataDosen(Dosen)}. Tidak dianotasi
	 * {@code @Column}, sehingga nama kolomnya mengikuti nama properti apa adanya:
	 * {@code noIdentitas} (bercampur gaya dengan kolom {@code no_ktp} di tabel yang sama).
	 *
	 * <p><b>Bukan getter murni.</b> Bila relasi {@link Dosen} terisi <i>dan</i> nomor KTP dosen
	 * tidak {@code null}, nilai itu disalin ke field {@code noIdentitas} sehingga ikut tersimpan
	 * pada flush berikutnya. Berbeda dari {@link #getNoKtp()} yang membaca sumber yang sama,
	 * penyalinan di sini dijaga terhadap {@code null} sehingga nilai lokal tidak pernah
	 * terhapus.</p>
	 *
	 * <p>Secara isi properti ini duplikat {@link #getNoKtp()}: keduanya sama-sama berasal dari
	 * {@code Dosen.getKtp()} namun disimpan di dua kolom berbeda. Yang dipakai formulir dan
	 * validasi kelengkapan adalah {@code noIdentitas}.</p>
	 *
	 * @return nomor identitas/NIK, mengikuti {@link Dosen} bila KTP dosen terisi
	 */
	public String getNoIdentitas() {
		if (getDosen() != null && getDosen().getKtp() != null) {
			noIdentitas = getDosen().getKtp();
		}
		return noIdentitas;
	}

	/**
	 * Mengisi nomor identitas (NIK/KTP) dosen. Perhatikan bahwa {@link #getNoIdentitas()} akan
	 * menimpanya dari {@link Dosen} bila KTP dosen terisi.
	 *
	 * @param noIdentitas nomor identitas/NIK
	 */
	public void setNoIdentitas(String noIdentitas) {
		this.noIdentitas = noIdentitas;
	}

	/**
	 * Mengembalikan nama dusun/kampung tempat tinggal dosen. Diekspor ke Feeder sebagai
	 * {@code nm_dsn} (maksimal 40 karakter) dan termasuk data yang diwajibkan
	 * {@code BiodataDosenAction.checkBiodataDosen(Dosen)}. Tidak dianotasi {@code @Column};
	 * kolomnya {@code dusun}.
	 *
	 * @return nama dusun/kampung, atau {@code null} bila belum diisi
	 */
	public String getDusun() {
		return dusun;
	}

	/**
	 * Mengisi nama dusun/kampung tempat tinggal dosen.
	 *
	 * @param dusun nama dusun/kampung
	 */
	public void setDusun(String dusun) {
		this.dusun = dusun;
	}

	/**
	 * Mengembalikan nama suami/istri dosen. Diekspor ke Feeder sebagai {@code nm_suami_istri}
	 * (maksimal 50 karakter). Tidak dianotasi {@code @Column}; kolomnya {@code namaSuamiIstri}.
	 *
	 * @return nama suami/istri, atau {@code null} bila belum diisi
	 */
	public String getNamaSuamiIstri() {
		return namaSuamiIstri;
	}

	/**
	 * Mengisi nama suami/istri dosen.
	 *
	 * @param namaSuamiIstri nama suami/istri
	 */
	public void setNamaSuamiIstri(String namaSuamiIstri) {
		this.namaSuamiIstri = namaSuamiIstri;
	}

	/**
	 * Mengembalikan NIP suami/istri dosen (bila yang bersangkutan juga pegawai negeri). Diekspor ke
	 * Feeder sebagai {@code nip_suami_istri} (maksimal 18 karakter). Tidak dianotasi
	 * {@code @Column}; kolomnya {@code nipSuamiIstri}.
	 *
	 * @return NIP suami/istri, atau {@code null} bila belum diisi
	 */
	public String getNipSuamiIstri() {
		return nipSuamiIstri;
	}

	/**
	 * Mengisi NIP suami/istri dosen.
	 *
	 * @param nipSuamiIstri NIP suami/istri
	 */
	public void setNipSuamiIstri(String nipSuamiIstri) {
		this.nipSuamiIstri = nipSuamiIstri;
	}

	/**
	 * Mengembalikan pekerjaan suami/istri dosen sebagai referensi ke master
	 * {@link PekerjaanOrangTua} (master yang sama dipakai ulang untuk pekerjaan pasangan, bukan
	 * hanya orang tua), dengan resolusi proxy lazy lewat
	 * {@link GeneralValueObject#check(Object)}. Ekspor Feeder mengirim <i>nama</i> pekerjaannya
	 * sebagai {@code id_pekerjaan_suami_istri}, bukan kode Feeder.
	 *
	 * @return master pekerjaan suami/istri, atau {@code null} bila belum dipilih
	 * @see GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pekerjaan_suami_istri", nullable = true)
	public PekerjaanOrangTua getPekerjaanSuamiIstri() {
		pekerjaanSuamiIstri = check(pekerjaanSuamiIstri);
		return pekerjaanSuamiIstri;
	}

	/**
	 * Mengisi pekerjaan suami/istri dosen.
	 *
	 * @param pekerjaanSuamiIstri master pekerjaan suami/istri
	 */
	public void setPekerjaanSuamiIstri(PekerjaanOrangTua pekerjaanSuamiIstri) {
		this.pekerjaanSuamiIstri = pekerjaanSuamiIstri;
	}

	/**
	 * Mengembalikan negara asal/kewarganegaraan dosen sebagai referensi ke master {@link Negara},
	 * dengan resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)}. Bila relasi belum
	 * dipilih, dikembalikan {@link ConstantValues#INDONESIA} sebagai nilai cadangan.
	 *
	 * <p><b>Beda dari getter berdefault lain di kelas ini:</b> nilai cadangan Indonesia
	 * <b>tidak</b> ditulis ke field {@code negara} &mdash; hanya dikembalikan. Jadi getter ini satu-
	 * satunya default yang murni baca dan tidak berefek ke basis data (kolom {@code negara} tetap
	 * {@code NULL}). Bandingkan {@link #getKelurahan()}, {@link #getKewarganegaraan()}, dan
	 * {@link #getKewarganegaraanFeeder()} yang menulis defaultnya.</p>
	 *
	 * <p><b>Jebakan:</b> {@link ConstantValues#INDONESIA} adalah field statis yang diisi saat
	 * inisialisasi data aplikasi; sebelum inisialisasi itu berjalan nilainya {@code null}, sehingga
	 * getter ini tetap bisa mengembalikan {@code null}.</p>
	 *
	 * @return master negara; {@link ConstantValues#INDONESIA} bila relasi belum dipilih
	 * @see GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "negara", nullable = true)
	public Negara getNegara() {
		negara = check(negara);
		return negara == null ? ConstantValues.INDONESIA : negara;
	}

	/**
	 * Mengisi negara asal/kewarganegaraan dosen.
	 *
	 * @param negara master negara
	 */
	public void setNegara(Negara negara) {
		this.negara = negara;
	}

	/**
	 * Mengembalikan kode kewarganegaraan dalam notasi Feeder, yaitu kode negara dua huruf
	 * (mis. {@code "ID"}). Berbeda dari {@link #getKewarganegaraan()} yang memakai notasi internal
	 * {@code "WNI"}/{@code "WNA"}; keduanya disimpan terpisah dan tidak saling disinkronkan. Nilai
	 * ini diekspor ke Feeder sebagai {@code kewarganegaraan} (dipotong 2 karakter). Tidak dianotasi
	 * {@code @Column}; kolomnya {@code kewarganegaraanFeeder}.
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, nilai {@code "ID"} <b>ditulis ke
	 * field</b> sehingga ikut tersimpan pada flush berikutnya. Perhatikan bahwa penjagaannya hanya
	 * terhadap {@code null} &mdash; string kosong dibiarkan apa adanya, berbeda dari
	 * {@link #getKewarganegaraan()} yang juga memeriksa string kosong.</p>
	 *
	 * @return kode negara dua huruf; {@code "ID"} bila belum diisi
	 */
	public String getKewarganegaraanFeeder() {
		if (kewarganegaraanFeeder == null) {
			kewarganegaraanFeeder = "ID";
		}
		return kewarganegaraanFeeder;
	}

	/**
	 * Mengisi kode kewarganegaraan dalam notasi Feeder (kode negara dua huruf).
	 *
	 * @param kewarganegaraanFeeder kode negara dua huruf, mis. {@code "ID"}
	 */
	public void setKewarganegaraanFeeder(String kewarganegaraanFeeder) {
		this.kewarganegaraanFeeder = kewarganegaraanFeeder;
	}

}
