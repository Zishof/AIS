package ais.database.model;

// Generated Apr 23, 20010 12:45:00 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.LampiranLain;

/**
 * Entity Hibernate untuk <b>biodata mahasiswa</b> pada tabel {@code public.biodata_mahasiswa} —
 * "lembar data pribadi" yang melengkapi {@link Mahasiswa}. Di AIS data mahasiswa sengaja dipecah
 * dua: kolom yang dipakai proses akademik sehari-hari (NIM, jurusan, angkatan, status, nama,
 * kelamin, tanggal lahir, dsb.) tinggal di {@link Mahasiswa}, sedangkan data pribadi rinci yang
 * jarang dibaca tetapi banyak jumlahnya (alamat lengkap sampai RT/RW/kodepos, kontak, data ayah/
 * ibu/wali, riwayat sekolah asal, dokumen identitas, dan puluhan "parameter tambahan" bebas)
 * ditempatkan di sini. Pemisahan itu menjaga tabel {@code mahasiswa} tetap ramping saat di-query
 * massal, dengan konsekuensi setiap layar yang butuh data pribadi harus mengambil biodata secara
 * eksplisit.
 *
 * <h3>Hubungan dengan {@link Mahasiswa}</h3>
 * <p>Secara logika hubungannya satu-ke-satu, tetapi di kode dipetakan sebagai
 * {@code @ManyToOne} lewat kolom {@code mahasiswa} (tanpa {@code unique = true}), sehingga basis
 * data secara teknis TIDAK mencegah munculnya dua baris biodata untuk satu mahasiswa. Titik masuk
 * yang benar adalah {@link Mahasiswa#ambilBiodata()} / {@link Mahasiswa#ambilBiodata(boolean)} —
 * perhatikan bahwa varian {@code ambilBiodata(true)} akan <b>MEMBUAT</b> baris biodata baru bila
 * belum ada (menulis ke basis data, bukan sekadar membaca). Arah sebaliknya dipakai lewat
 * {@link #getMahasiswa()}.</p>
 *
 * <p>Kapan dipakai terpisah, kapan bersama:</p>
 * <ul>
 * <li><b>Bersama</b> — layar isian/cetak yang menampilkan profil utuh: {@code BiodataMahasiswaAction},
 * daftar ulang mahasiswa baru/lama, cetak kartu &amp; formulir registrasi, laporan
 * ({@code CommonReportHelper}), ekspor Feeder/PDDikti.</li>
 * <li><b>Terpisah</b> — proses akademik (KRS, nilai, presensi, keuangan) cukup memakai
 * {@link Mahasiswa} dan tidak menyentuh kelas ini sama sekali.</li>
 * </ul>
 *
 * <h3>Pengelompokan kolom</h3>
 * <ul>
 * <li><b>Alamat &amp; wilayah</b> — {@code alamat}, {@code dusun}, {@code rt}, {@code rw},
 * {@code kelurahan}, {@code kodepos}, {@link Wilayah} {@code kecamatan}, {@link Kota},
 * {@link Propinsi}, {@link JenisTinggalMahasiswa}, {@link AlatTransportasiMahasiswa}.</li>
 * <li><b>Kontak</b> — {@code teleponRumah}, {@code hp}, {@code hpProvider} +
 * {@link OperatorSeluler}, {@code email}, {@code emailAtasan}.</li>
 * <li><b>Keluarga</b> — trio ayah/ibu/wali yang hampir simetris: nama, tanggal lahir, NIK (ayah &amp;
 * ibu saja), telepon, {@link PekerjaanOrangTua} &amp; {@link Pekerjaan}, {@link PendidikanOrangTua}
 * &amp; {@link Jenjang}, {@link Penghasilan} &amp; {@link PendapatanOrangTua}, plus dua kolom
 * teks hasil format {@code penghasilanAyah}/{@code penghasilanIbu} dan angka
 * {@code penghasilanOrangTua}. Ditambah {@code bersaudara} (jumlah saudara) dan {@code noKK}.</li>
 * <li><b>Riwayat sekolah asal</b> — {@link JenisSekolahMahasiswaBaru}, {@link NamaSekolahAsal},
 * {@code npsn}, {@code asalSma}/{@code alamatAsalSma}, {@code asalSmp}/{@code alamatAsalSmp},
 * {@code asalSd}/{@code alamatAsalSd}, {@code apakahPernahPaud}, {@code apakahPernahTk}.</li>
 * <li><b>Dokumen &amp; nomor identitas</b> — {@code noIdentitas} (NIK), {@code nisn}, {@code nirm},
 * {@code npwp}, {@code noIjazah}, {@code namaUntukIjazah}, {@code suratIzinMengemudi},
 * {@code no_rek_bri}/{@code cabangBri}.</li>
 * <li><b>Data fisik &amp; personal</b> — {@code tinggiBadan}, {@code beratBadan},
 * {@code golonganDarah}, {@code ukuranJaket}, {@code statusNikah}, {@code kewarganegaraan},
 * {@link Agama}, {@code pernahMenetapDiLuarNegeri}, {@code hobi}, {@code minatSeni},
 * {@code kemampuanBahasa1..3}, {@code pernahMemimpinOrganisasi}/{@code namaOrganisasi},
 * {@code kendaraanKuliah}.</li>
 * <li><b>Pekerjaan mahasiswa &amp; program pascasarjana</b> — {@code biayaStudi},
 * {@code kodeKerjaan}, {@code tempatKerja}, {@code kodeTempatKerjaPt}/{@code kodeTempatKerjaPs},
 * {@code nidnPromotor} dan {@code nidnKoPromotor1..4} (untuk program doktor).</li>
 * <li><b>Sertifikasi</b> — {@code punyaSkpi}, {@code punyaSertifikatBahasaInggris},
 * {@code punyaSertifikatBahasaArab}.</li>
 * <li><b>Parameter tambahan</b> — empat kolom {@code text} berisi data terenkode: lihat bagian
 * khusus di bawah.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 * <li><b>Getter/setter sederhana</b> — mayoritas method di kelas ini; sekadar membaca/menulis
 * field.</li>
 * <li><b>Getter penormal ("self-healing")</b> — {@link #getRt()}, {@link #getRw()},
 * {@link #getKodepos()}, {@link #getNoIdentitas()}, {@link #getNisn()}, {@link #getNirm()},
 * {@link #getNpwp()}, {@link #getNikAyah()}, {@link #getNikIbu()}, {@link #getTeleponRumah()},
 * {@link #getNamaIbu()}, {@link #getAsalSmp()}, {@link #getAsalSd()}. Membersihkan karakter non
 * angka, membuang tanda hubung, memotong panjang, dan mengganti nilai sentinel lama (deretan
 * {@code "0000..."}) menjadi string kosong.</li>
 * <li><b>Getter penyelaras (sinkronisasi lintas entity)</b> — mengambil nilai dari
 * {@link Mahasiswa} atau dari {@link BiodataCalonMahasiswa} (data pendaftaran/PMB) bila kolom di
 * sini masih kosong: {@link #getTinggiBadan()}, {@link #getBeratBadan()}, {@link #getHp()},
 * {@link #getTeleponRumah()}, {@link #getNoIdentitas()}, {@link #getNisn()},
 * {@link #getGolonganDarah()}, {@link #getAgama()}, {@link #getEmail()}.</li>
 * <li><b>Getter penurun (derived)</b> — nilai dihitung dari relasi lain:
 * {@link #getPenghasilanAyah()}, {@link #getPenghasilanIbu()}, {@link #getPenghasilanOrangTua()},
 * {@link #getAsalSma()}, {@link #getNpsn()}, {@link #getHpProvider()}.</li>
 * <li><b>Getter dengan akses basis data</b> (paling berat, lihat peringatan di bawah) —
 * {@link #getKecamatan()}, {@link #getPropinsi()}, {@link #getKota()},
 * {@link #getOperatorSeluler()} dan helper {@link #findOrCreatePropinsi(Session, String)}.</li>
 * <li><b>Parameter tambahan</b> — {@link #populateParameterTambahan(List)},
 * {@link #ambilDataParameterTambahan()}, {@link #populateParameterTambahanAlumni(List)},
 * {@link #ambilDataParameterTambahanAlumni()}, {@link #ambilSkor(ParameterTambahan)}.</li>
 * <li><b>Lain-lain</b> — {@link #putPhoto(Map)} (delegasi cetak/laporan), {@link #toString()},
 * {@link #onUpdate()} (callback JPA), serta helper statis {@link #potongKolom50(String)}.</li>
 * </ol>
 *
 * <h3>Parameter tambahan (kolom teks terenkode)</h3>
 * <p>AIS mengizinkan tiap perguruan tinggi menambah field isian sendiri tanpa mengubah skema.
 * Nilainya tidak disimpan sebagai baris tabel, melainkan diserialkan ke kolom {@code text}
 * sebagai daftar baris (pemisah baris {@code "\n"}) dengan pemisah kolom literal
 * <code>&lt;=&gt;</code>. Ada dua pasang kolom:</p>
 * <ul>
 * <li>{@code parameterTambahan} (versi "berlabel", untuk ditampilkan/dicetak) dan
 * {@code parameterTambahanInds} (versi "ber-ID", untuk mengisi ulang form) — diisi oleh
 * {@link #populateParameterTambahan(List)} dari
 * {@code ais.action.master.helper.ParameterTambahanMahasiswaListener}.</li>
 * <li>{@code parameterTambahanAlumni} / {@code parameterTambahanIndsAlumni} — kembarannya untuk
 * kuesioner alumni (tracer study), diisi oleh {@link #populateParameterTambahanAlumni(List)} dari
 * {@code ais.action.master.helper.ParameterTambahanAlumniListener}.</li>
 * </ul>
 * <p>Format satu baris versi berlabel (8 ruas):
 * {@code namaKelompok->labelInputan <=> nilai <=> urlLampiran <=> nomorUrut <=> idParameter <=>
 * idKelompok <=> indexKe <=> keterangan}; versi ber-ID hanya 4 ruas:
 * {@code idKelompok->idParameter <=> nilai <=> urlLampiran <=> keterangan}.</p>
 *
 * <h3>Peringatan penting bagi pembaca kode</h3>
 * <ul>
 * <li><b>Getter di kelas ini bukan getter murni.</b> Hampir semuanya <i>menulis balik</i> ke
 * field yang dibacanya, dan sebagian melakukan query basis data. Karena Hibernate memanggil
 * getter saat {@code flush}/{@code dirty check}, memanggil satu getter bisa memicu UPDATE yang
 * tidak diminta pemanggil. Jangan berasumsi "sekadar baca".</li>
 * <li><b>{@link #getPropinsi()} dan {@link #getKota()} bisa menulis baris baru.</b> Keduanya
 * mencoba menebak propinsi/kota dari hierarki {@link Wilayah} kecamatan memakai jarak
 * Levenshtein, dan bila tidak ada yang cukup mirip, {@link #findOrCreatePropinsi(Session, String)}
 * akan {@code session.save(...)} + {@code commit()} sebuah {@link Propinsi} BARU. Sebuah getter
 * yang memasukkan baris master baru adalah perilaku yang sangat tidak lazim — catat baik-baik
 * sebelum memakai kelas ini dalam proses batch.</li>
 * <li><b>Keduanya juga menutup sesi Hibernate</b> ({@code session.disconnect()},
 * {@code session.close()}, {@code HibernateUtil.closeSession()}) di blok {@code finally}. Bila
 * getter dipanggil di tengah unit of work milik pemanggil, sesi itu ikut tertutup.</li>
 * <li><b>Rantai fallback ke PMB.</b> Banyak getter mencari nilai pengganti ke
 * {@link BiodataCalonMahasiswa} melalui {@code mahasiswa.getBiodataCalonMahasiswa()} (ID, bukan
 * relasi) dan {@code ConstantValues.ambil(...)}. Nilai hasil fallback ditulis ke field, jadi bisa
 * ikut tersimpan ke basis data pada flush berikutnya.</li>
 * <li><b>Field audit di-shadow.</b> {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah}
 * dideklarasikan ulang di kelas ini padahal {@link GeneralValueObject} sudah punya field bernama
 * sama. Yang terpetakan Hibernate adalah properti milik kelas ini; field milik induk menjadi
 * tidak terpakai untuk entity ini. Perilaku ini disengaja atau tidak, belum jelas — jangan
 * mengandalkan nilai field induk saat men-debug audit baris biodata.</li>
 * <li>Kontrak umum {@code id}/{@code equals}/{@code compareTo}/{@code check(...)} beserta
 * mekanisme cache berkas diwarisi dari {@link GeneralValueObject} dan tidak diulang di sini.</li>
 * </ul>
 *
 * <p>Anotasi {@code @Audited} (Hibernate Envers) membuat setiap perubahan baris terekam ke tabel
 * revisi, sedangkan {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya menulis
 * kolom yang benar-benar berubah.</p>
 *
 * @see Mahasiswa
 * @see BiodataCalonMahasiswa
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "biodata_mahasiswa")
public class BiodataMahasiswa extends GeneralValueObject {

	/**
	 * Memotong sebuah nilai teks agar aman dimasukkan ke kolom basis data berlebar 50 karakter.
	 *
	 * <p>Melakukan tiga hal sekaligus: {@code null} diubah menjadi string kosong (bukan
	 * dilewatkan apa adanya), spasi di ujung dibuang ({@code trim}), lalu bila masih lebih dari
	 * 50 karakter dipotong pada karakter ke-50. Dipakai oleh {@link #getAsalSma()}.</p>
	 *
	 * <p><b>Catatan.</b> Angka 50 di sini tidak sinkron dengan anotasi {@code @Column(length = 255)}
	 * pada {@link #getAsalSma()}; artinya nama sekolah asal yang panjang terpotong lebih agresif
	 * daripada yang sebenarnya muat di kolom. Perilaku ini dibiarkan apa adanya (kemungkinan
	 * warisan lebar kolom lama) dan hanya dicatat di sini.</p>
	 *
	 * @param nilai teks yang akan dipotong, boleh {@code null}
	 * @return teks hasil {@code trim} dengan panjang maksimal 50 karakter; string kosong bila
	 *         {@code nilai} adalah {@code null} (tidak pernah mengembalikan {@code null})
	 */
	private static String potongKolom50(String nilai) {
		if (nilai == null) {
			return "";
		}
		nilai = nilai.trim();
		return nilai.length() > 50 ? nilai.substring(0, 50) : nilai;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1995121656114539247L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris biodata ini.
	 *
	 * <p>Diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor} lewat
	 * {@link #onUpdate()}. Perhatikan bahwa field {@code olehId} di kelas ini menutupi
	 * ({@code shadow}) field bernama sama milik {@link GeneralValueObject} — lihat Javadoc kelas.</p>
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila baris belum pernah diaudit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir.
	 *
	 * <p><b>Bukan setter biasa:</b> nilai {@code null} atau string kosong/berisi spasi saja
	 * DIABAIKAN diam-diam — nilai lama tetap dipertahankan. Konsekuensinya jejak audit tidak bisa
	 * dihapus lewat setter ini, dan pemanggil yang mengira sudah mengosongkan field akan keliru.</p>
	 *
	 * @param olehId ID pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: masukan {@code null} atau kosong diabaikan diam-diam
	 * sehingga nilai audit sebelumnya tidak tertimpa.</p>
	 *
	 * @param oleh nama/identitas pengguna pengubah; diabaikan bila {@code null} atau kosong
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
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 * @see #setOleh(String)
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dijalankan otomatis oleh penyedia persistensi tepat sebelum
	 * baris biodata ini di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)}, dan {@link #setOlehId(String)}
	 * dengan waktu sekarang dan identitas pengguna yang sedang login. Interceptor tersebut akan
	 * melewati pengisian bila {@code AuditTrailHelper} menilai tidak ada perubahan bisnis nyata pada
	 * entity, sehingga UPDATE "kosong" tidak mengotori kolom audit.</p>
	 *
	 * <p>Tidak pernah dipanggil manual dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Biasanya diisi otomatis lewat {@link #onUpdate()}; jarang dipanggil langsung.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris biodata ini.
	 *
	 * <p>Field diinisialisasi ke waktu sekarang ({@code WaktuUtil.getDate()}) saat objek dibuat,
	 * sehingga entity baru sudah punya nilai sebelum sempat disimpan.</p>
	 *
	 * @return waktu perubahan terakhir (kolom {@code TIMESTAMP})
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity: nama/identitas {@link Mahasiswa} pemilik biodata ini.
	 *
	 * <p>Memanggil {@link #getMahasiswa()} lebih dulu (sehingga proxy Hibernate ikut ter-resolve) lalu
	 * merangkaikannya dengan string kosong. <b>Kuirk:</b> bila biodata belum tertaut ke mahasiswa mana
	 * pun, hasilnya adalah literal {@code "null"}, bukan string kosong — hati-hati bila dipakai
	 * langsung sebagai label di UI.</p>
	 *
	 * @return hasil {@code toString()} milik mahasiswa terkait, atau {@code "null"} bila tidak ada
	 */
	public String toString() {
		mahasiswa = getMahasiswa();
		return mahasiswa + "";
	}

	private Mahasiswa mahasiswa;
	private String alamat;
	private String namaAyah;
	private Date tanggalLahirAyah;
	private PekerjaanOrangTua pekerjaanAyah;
	private Pekerjaan jenisPekerjaanAyah;
	private PendidikanOrangTua pendidikanAyah;
	private Penghasilan jenisPenghasilanAyah;
	private Penghasilan jenisPenghasilanIbu;
	private Penghasilan jenisPenghasilanWali;

	private Jenjang jenjangPendidikanAyah;
	private Jenjang jenjangPendidikanIbu;
	private Jenjang jenjangPendidikanWali;

	private String penghasilanAyah;
	private String email;

	private String namaWali;
	private Date tanggalLahirWali;
	private PekerjaanOrangTua pekerjaanWali;
	private Pekerjaan jenisPekerjaanIbu;
	private Pekerjaan jenisPekerjaanWali;
	private PendidikanOrangTua pendidikanWali;
	private PendapatanOrangTua pendapatanWali;

	private Long penghasilanOrangTua;
	private String namaIbu;
	private Date tanggalLahirIbu;
	private PekerjaanOrangTua pekerjaanIbu;
	private PendidikanOrangTua pendidikanIbu;
	private String penghasilanIbu;
	private String namaUntukIjazah;
	private String noIjazah;
	private String ukuranJaket;
	private Integer tinggiBadan;
	private Integer pernahMenetapDiLuarNegeri = 0;
	private Integer beratBadan;
	private String teleponRumah;
	private String hp;
	private String hpProvider;
	private OperatorSeluler operatorSeluler;
	private String suratIzinMengemudi;
	private String kendaraanKuliah;
	private Integer pernahMemimpinOrganisasi;
	private String namaOrganisasi;
	private String hobi;
	private String minatSeni;
	private String kemampuanBahasa1;
	private String kemampuanBahasa2;
	private String kemampuanBahasa3;

	private JenisSekolahMahasiswaBaru jenisSekolah;
	private String npsn;
	private NamaSekolahAsal namaSekolahAsal;
	private String asalSma;
	private String alamatAsalSma;
	private String asalSmp;
	private String alamatAsalSmp;
	private String asalSd;
	private String alamatAsalSd;
	private String golonganDarah;
	private Integer statusNikah;
	private String kewarganegaraan;
	private Agama agama;

	private String biayaStudi;
	private String kodeKerjaan;
	private String tempatKerja;
	private String kodeTempatKerjaPt;
	private String kodeTempatKerjaPs;

	private String nidnPromotor;
	private String nidnKoPromotor1;
	private String nidnKoPromotor2;
	private String nidnKoPromotor3;
	private String nidnKoPromotor4;

	private Integer bersaudara;
	private String no_rek_bri;
	private String cabangBri;
	private String noKK;
	private String rt;
	private String rw;
	private String kodepos;
	private String kelurahan;
	private Wilayah kecamatan;
	private Propinsi propinsi;
	private Kota kota;
	private JenisTinggalMahasiswa jenisTinggalMahasiswa;
	private AlatTransportasiMahasiswa alatTransportasiMahasiswa;

	private String noIdentitas;
	private String dusun;
	private PendapatanOrangTua pendapatanOrtu;
	private PendapatanOrangTua pendapatanOrtuIbu;

	private Boolean apakahPernahPaud;
	private Boolean apakahPernahTk;

	private String parameterTambahan;
	private String parameterTambahanInds;

	private String parameterTambahanAlumni;
	private String parameterTambahanIndsAlumni;

	private String nisn;
	private String nirm;
	private String npwp;
	private String nikAyah;
	private String nikIbu;

	private String telpAyah, telpIbu, telpWali;

	private Boolean punyaSkpi;
	private Boolean punyaSertifikatBahasaInggris;
	private Boolean punyaSertifikatBahasaArab;

	private String emailAtasan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Tidak mengisi apa pun; seluruh nilai awal berasal dari inisialisasi field
	 * ({@code pernahMenetapDiLuarNegeri = 0}, {@code tanggal_dirubah} = waktu sekarang). Untuk
	 * mendapatkan biodata milik seorang mahasiswa, jangan membuat objek ini secara manual — pakai
	 * {@link Mahasiswa#ambilBiodata()} agar tidak muncul baris biodata ganda.</p>
	 */
	public BiodataMahasiswa() {
	}

	/**
	 * Kunci utama baris biodata (kolom {@code id}, {@code IDENTITY}/serial).
	 *
	 * <p>Selain sebagai primary key, nilai ini juga dipakai sebagai "pemilik" berkas lampiran
	 * parameter tambahan — lihat pemanggilan {@code LampiranLain.ambil(getId(), jenis)} di
	 * {@link #populateParameterTambahan(List)}.</p>
	 *
	 * @return ID baris, atau {@code null} bila entity belum tersimpan
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Umumnya hanya dipanggil Hibernate setelah INSERT.
	 *
	 * @param id nilai kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mahasiswa pemilik biodata ini (kolom {@code mahasiswa}).
	 *
	 * <p>Nilainya dilewatkan {@code check(...)} milik {@link GeneralValueObject} sehingga proxy
	 * Hibernate yang sudah tidak terhubung sesi tetap dapat dipakai. Relasi dipetakan
	 * {@code @ManyToOne} lazy walaupun secara domain satu-ke-satu; lihat Javadoc kelas mengenai
	 * implikasi tidak adanya batasan {@code unique} di basis data.</p>
	 *
	 * <p>Dipanggil ulang-ulang dari dalam kelas ini sendiri (mis. {@link #getTinggiBadan()},
	 * {@link #getHp()}, {@link #getEmail()}) sebagai pintu masuk sinkronisasi nilai.</p>
	 *
	 * @return mahasiswa pemilik biodata, atau {@code null} bila baris belum tertaut
	 * @see Mahasiswa#ambilBiodata()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return this.mahasiswa;
	}

	/**
	 * Menautkan biodata ini ke seorang mahasiswa.
	 *
	 * @param mahasiswa mahasiswa pemilik biodata
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Alamat tempat tinggal mahasiswa (kolom {@code alamat}).
	 *
	 * <p>Bila field masih {@code null} diganti string kosong lebih dulu, lalu hasilnya di-{@code trim}.
	 * Karena itu getter ini tidak pernah mengembalikan {@code null} — aman dipakai langsung di UI dan
	 * laporan.</p>
	 *
	 * @return alamat tanpa spasi di ujung; string kosong bila belum diisi
	 */
	@Column(name = "alamat")
	public String getAlamat() {
		if (alamat == null) {
			alamat = "";
		}
		return this.alamat.trim();
	}

	/**
	 * Menyetel alamat tempat tinggal.
	 *
	 * @param alamat alamat lengkap (baris jalan/nomor rumah); RT/RW, kelurahan, kecamatan, kota, dan
	 *         kodepos disimpan pada kolom terpisah
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Nama ayah kandung (kolom {@code nama_ayah}, maks. 100 karakter).
	 *
	 * @return nama ayah apa adanya, bisa {@code null}
	 */
	@Column(name = "nama_ayah", length = 100)
	public String getNamaAyah() {
		return this.namaAyah;
	}

	/**
	 * Menyetel nama ayah kandung.
	 *
	 * @param namaAyah nama ayah
	 */
	public void setNamaAyah(String namaAyah) {
		this.namaAyah = namaAyah;
	}

	/**
	 * Kategori pekerjaan ayah menurut daftar acuan {@link PekerjaanOrangTua} (kolom
	 * {@code id_pekerjaan_ayah}).
	 *
	 * <p>Jangan tertukar dengan {@link #getJenisPekerjaanAyah()} yang memakai daftar acuan berbeda
	 * ({@link Pekerjaan}) — dua kolom ini hidup berdampingan karena kebutuhan pelaporan yang berbeda.</p>
	 *
	 * @return kategori pekerjaan ayah, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "id_pekerjaan_ayah", nullable = true)
	public PekerjaanOrangTua getPekerjaanAyah() {
		pekerjaanAyah = check(pekerjaanAyah);
		return this.pekerjaanAyah;
	}

	/**
	 * Menyetel kategori pekerjaan ayah.
	 *
	 * @param pekerjaanAyah acuan {@link PekerjaanOrangTua}
	 */
	public void setPekerjaanAyah(PekerjaanOrangTua pekerjaanAyah) {
		this.pekerjaanAyah = pekerjaanAyah;
	}

	/**
	 * Nama ibu kandung (kolom {@code nama_ibu}, maks. 100 karakter), sudah dirapikan.
	 *
	 * <p><b>Efek samping:</b> nilai field ikut diperbaiki, bukan sekadar hasil kembaliannya. Spasi
	 * ganda dimampatkan menjadi satu spasi — dilakukan tiga kali berturut-turut sehingga deret sampai
	 * delapan spasi ikut rapi — lalu di-{@code trim}. Perbaikan ini akan ikut tersimpan bila entity
	 * di-flush setelahnya.</p>
	 *
	 * @return nama ibu yang sudah dirapikan, atau {@code null} bila memang belum diisi
	 */
	@Column(name = "nama_ibu", length = 100)
	public String getNamaIbu() {

		if (namaIbu != null) {
			namaIbu = org.apache.commons.lang3.StringUtils.replace(namaIbu, "  ", " ");
			namaIbu = org.apache.commons.lang3.StringUtils.replace(namaIbu, "  ", " ");
			namaIbu = org.apache.commons.lang3.StringUtils.replace(namaIbu, "  ", " ");
			namaIbu = namaIbu.trim();
		}

		return this.namaIbu;
	}

	/**
	 * Menyetel nama ibu kandung.
	 *
	 * @param namaIbu nama ibu (perapian spasi dilakukan saat dibaca, lihat {@link #getNamaIbu()})
	 */
	public void setNamaIbu(String namaIbu) {
		this.namaIbu = namaIbu;
	}

	/**
	 * Kategori pekerjaan ibu menurut daftar acuan {@link PekerjaanOrangTua} (kolom
	 * {@code id_pekerjaan_ibu}).
	 *
	 * @return kategori pekerjaan ibu, atau {@code null} bila belum dipilih
	 * @see #getJenisPekerjaanIbu()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "id_pekerjaan_ibu", nullable = true)
	public PekerjaanOrangTua getPekerjaanIbu() {
		pekerjaanIbu = check(pekerjaanIbu);
		return this.pekerjaanIbu;
	}

	/**
	 * Menyetel kategori pekerjaan ibu.
	 *
	 * @param pekerjaanIbu acuan {@link PekerjaanOrangTua}
	 */
	public void setPekerjaanIbu(PekerjaanOrangTua pekerjaanIbu) {
		this.pekerjaanIbu = pekerjaanIbu;
	}

	/**
	 * Nama sebagaimana akan dicetak pada ijazah (kolom {@code nama_untuk_ijazah}).
	 *
	 * <p>Sengaja dipisah dari nama pada {@link Mahasiswa} karena ijazah sering memakai ejaan resmi
	 * akta kelahiran yang berbeda dengan nama panggilan/administratif sehari-hari.</p>
	 *
	 * @return nama untuk ijazah, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama_untuk_ijazah", length = 100)
	public String getNamaUntukIjazah() {
		return this.namaUntukIjazah;
	}

	/**
	 * Menyetel nama yang akan dicetak pada ijazah.
	 *
	 * @param namaUntukIjazah nama versi ijazah
	 */
	public void setNamaUntukIjazah(String namaUntukIjazah) {
		this.namaUntukIjazah = namaUntukIjazah;
	}

	/**
	 * Nomor ijazah pendidikan sebelumnya / ijazah yang diterbitkan (kolom {@code no_ijazah}).
	 *
	 * @return nomor ijazah, atau {@code null} bila belum diisi
	 */
	@Column(name = "no_ijazah", length = 100)
	public String getNoIjazah() {
		return this.noIjazah;
	}

	/**
	 * Menyetel nomor ijazah.
	 *
	 * @param noIjazah nomor ijazah
	 */
	public void setNoIjazah(String noIjazah) {
		this.noIjazah = noIjazah;
	}

	/**
	 * Ukuran jaket almamater (kolom {@code ukuran_jaket}), mis. {@code "L"} atau {@code "XL"}.
	 * Dipakai panitia orientasi mahasiswa baru untuk rekap pengadaan.
	 *
	 * @return ukuran jaket, atau {@code null} bila belum diisi
	 */
	@Column(name = "ukuran_jaket", length = 100)
	public String getUkuranJaket() {
		return this.ukuranJaket;
	}

	/**
	 * Menyetel ukuran jaket almamater.
	 *
	 * @param ukuranJaket kode ukuran bebas-teks
	 */
	public void setUkuranJaket(String ukuranJaket) {
		this.ukuranJaket = ukuranJaket;
	}

	/**
	 * Tinggi badan mahasiswa dalam sentimeter (kolom {@code tinggi_badan}), dengan sinkronisasi dua
	 * tingkat.
	 *
	 * <p>Urutan penentuan nilai:</p>
	 * <ol>
	 * <li>Bila {@link Mahasiswa#getTinggi_badan()} terisi dan lebih besar dari 0, nilai itu
	 * MENIMPA field di sini (data pada tabel {@code mahasiswa} dianggap lebih mutakhir).</li>
	 * <li>Bila setelah langkah 1 nilai masih kosong atau &lt; 1, dan mahasiswa punya rujukan
	 * {@link BiodataCalonMahasiswa} ({@code mahasiswa.getBiodataCalonMahasiswa()} berisi ID &gt; 0),
	 * data pendaftaran PMB dibaca lewat {@code ConstantValues.ambil(...)} dan dipakai bila &gt; 1.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> field {@code tinggiBadan} (dan {@code mahasiswa}) ditulis ulang, jadi
	 * hasil sinkronisasi bisa ikut tersimpan pada flush berikutnya. Seluruh proses dibungkus
	 * {@code try}/{@code catch} yang menelan kegagalan (dicatat lewat {@code ErrorAuditUtil}), sehingga
	 * kegagalan pembacaan PMB tidak pernah menggagalkan pembacaan biodata.</p>
	 *
	 * @return tinggi badan dalam cm, atau {@code null} bila tidak ada sumber data mana pun
	 */
	@Column(name = "tinggi_badan")
	public Integer getTinggiBadan() {
		try {
			mahasiswa = getMahasiswa();
			if (mahasiswa != null && mahasiswa.getTinggi_badan() != null && mahasiswa.getTinggi_badan() > 0) {
				tinggiBadan = mahasiswa.getTinggi_badan();
			}

			if (mahasiswa != null && mahasiswa.getBiodataCalonMahasiswa() != null
					&& mahasiswa.getBiodataCalonMahasiswa() > 0L && (tinggiBadan == null || tinggiBadan < 1)) {
				BiodataCalonMahasiswa b = (BiodataCalonMahasiswa) ConstantValues
						.ambil(BiodataCalonMahasiswa.class.getName(), mahasiswa.getBiodataCalonMahasiswa());
				if (b != null && b.getTinggiBadan() != null && b.getTinggiBadan() > 1) {
					tinggiBadan = b.getTinggiBadan();
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/BiodataMahasiswa.java:354");
		}
		return this.tinggiBadan;
	}

	/**
	 * Menyetel tinggi badan (cm).
	 *
	 * <p>Perhatikan bahwa nilai ini dapat ditimpa kembali oleh {@link #getTinggiBadan()} bila
	 * {@link Mahasiswa} punya nilai tinggi badan sendiri.</p>
	 *
	 * @param tinggiBadan tinggi badan dalam sentimeter
	 */
	public void setTinggiBadan(Integer tinggiBadan) {
		this.tinggiBadan = tinggiBadan;
	}

	/**
	 * Penanda apakah mahasiswa pernah menetap di luar negeri (kolom
	 * {@code pernah_menetap_di_luar_negeri}).
	 *
	 * <p>Bertipe {@code Integer} dan diperlakukan sebagai boolean bergaya lama: {@code 0} = tidak,
	 * {@code 1} = pernah. Field diinisialisasi {@code 0} pada deklarasi sehingga entity baru sudah
	 * bernilai "tidak".</p>
	 *
	 * @return {@code 0} atau {@code 1}; secara teori bisa {@code null} bila basis data menyimpan NULL
	 */
	@Column(name = "pernah_menetap_di_luar_negeri")
	public Integer getPernahMenetapDiLuarNegeri() {
		return this.pernahMenetapDiLuarNegeri;
	}

	/**
	 * Menyetel penanda pernah menetap di luar negeri.
	 *
	 * @param pernahMenetapDiLuarNegeri {@code 0} untuk tidak, {@code 1} untuk pernah
	 */
	public void setPernahMenetapDiLuarNegeri(Integer pernahMenetapDiLuarNegeri) {
		this.pernahMenetapDiLuarNegeri = pernahMenetapDiLuarNegeri;
	}

	/**
	 * Berat badan mahasiswa dalam kilogram (kolom {@code berat_badan}), dengan sinkronisasi dua
	 * tingkat yang persis sama pola dan efek sampingnya dengan {@link #getTinggiBadan()}: prioritas
	 * pertama {@link Mahasiswa#getBerat_badan()}, lalu data PMB {@link BiodataCalonMahasiswa}.
	 *
	 * @return berat badan dalam kg, atau {@code null} bila tidak ada sumber data
	 * @see #getTinggiBadan()
	 */
	@Column(name = "berat_badan")
	public Integer getBeratBadan() {
		try {
			mahasiswa = getMahasiswa();
			if (mahasiswa != null && mahasiswa.getBerat_badan() != null && mahasiswa.getBerat_badan() > 0) {
				beratBadan = mahasiswa.getBerat_badan();
			}

			if (mahasiswa != null && mahasiswa.getBiodataCalonMahasiswa() != null
					&& mahasiswa.getBiodataCalonMahasiswa() > 0L && (beratBadan == null || beratBadan < 1)) {
				BiodataCalonMahasiswa b = (BiodataCalonMahasiswa) ConstantValues
						.ambil(BiodataCalonMahasiswa.class.getName(), mahasiswa.getBiodataCalonMahasiswa());
				if (b != null && b.getBeratBadan() != null && b.getBeratBadan() > 1) {
					beratBadan = b.getBeratBadan();
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/BiodataMahasiswa.java:389");
		}
		return this.beratBadan;
	}

	/**
	 * Menyetel berat badan (kg).
	 *
	 * @param beratBadan berat badan dalam kilogram
	 */
	public void setBeratBadan(Integer beratBadan) {
		this.beratBadan = beratBadan;
	}

	/**
	 * Nomor telepon rumah (kolom {@code telepon_rumah}), sudah dinormalisasi dan disinkronkan.
	 *
	 * <p>Rangkaian pembersihan yang dijalankan setiap kali dipanggil:</p>
	 * <ol>
	 * <li>Bila isinya bukan angka murni, semua karakter selain digit dan titik dibuang.</li>
	 * <li>{@code null} diganti string kosong.</li>
	 * <li>Awalan {@code "+62"} diubah menjadi {@code "0"}, tanda hubung dibuang.</li>
	 * <li>Nilai sentinel lama berupa deretan tujuh belas nol dianggap "tidak ada" dan dikosongkan.</li>
	 * <li>Bila hasilnya masih mengandung {@code "0000"} dan mahasiswa punya data PMB, nomor HP dari
	 * {@link BiodataCalonMahasiswa#getHp()} dipakai sebagai pengganti.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> field ikut ditimpa hasil normalisasi. Dua blok pemotongan panjang
	 * (minimal 10, maksimal 20 digit) sudah dinonaktifkan (dikomentari) — jangan diaktifkan kembali
	 * tanpa memeriksa lebar kolom.</p>
	 *
	 * @return nomor telepon rumah hasil normalisasi; bisa string kosong, praktis tidak pernah
	 *         {@code null} setelah pemanggilan pertama
	 */
	@Column(name = "telepon_rumah", length = 200)
	public String getTeleponRumah() {
		try {
			mahasiswa = getMahasiswa();
			if (teleponRumah != null && !Common.isNumber(teleponRumah)) {
				teleponRumah = teleponRumah.replaceAll("[^\\d.]", "");
			}

			if (teleponRumah == null) {
				teleponRumah = "";
			}

			teleponRumah = org.apache.commons.lang3.StringUtils.replace(teleponRumah, "+62", "0");
			teleponRumah = org.apache.commons.lang3.StringUtils.replace(teleponRumah, "-", "");

//			if (teleponRumah.length() < 10) {
//				teleponRumah = (teleponRumah + "00000000000000000").substring(0, 9);
//			}

//			if (teleponRumah.length() > 20) {
//				teleponRumah = teleponRumah.substring(0, 20);
//			}

			if (teleponRumah.contains("00000000000000000")) {
				teleponRumah = "";
			}

			if (mahasiswa != null && mahasiswa.getBiodataCalonMahasiswa() != null
					&& mahasiswa.getBiodataCalonMahasiswa() > 0L && teleponRumah.contains("0000")) {
				BiodataCalonMahasiswa b = (BiodataCalonMahasiswa) ConstantValues
						.ambil(BiodataCalonMahasiswa.class.getName(), mahasiswa.getBiodataCalonMahasiswa());
				if (b != null && b.getHp() != null) {
					teleponRumah = b.getHp();
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/BiodataMahasiswa.java:434");
		}
		return this.teleponRumah;
	}

	/**
	 * Menyetel nomor telepon rumah apa adanya (normalisasi terjadi saat dibaca).
	 *
	 * @param teleponRumah nomor telepon rumah
	 */
	public void setTeleponRumah(String teleponRumah) {
		this.teleponRumah = teleponRumah;
	}

	/**
	 * Nomor telepon seluler mahasiswa (kolom {@code hp}), dengan fallback ke data pendaftaran.
	 *
	 * <p>Bila field masih kosong dan mahasiswa punya rujukan {@link BiodataCalonMahasiswa}
	 * ({@code mahasiswa.getBiodataCalonMahasiswa()} berisi ID &gt; 0), nomor HP dari formulir PMB
	 * disalin ke sini. Setelah itu nilai sentinel lama {@code "08100000000000000000"} (dipakai versi
	 * lama sebagai penanda "belum diisi") dikosongkan.</p>
	 *
	 * <p>Blok normalisasi lengkap seperti pada {@link #getTeleponRumah()} — pembuangan karakter non
	 * angka, konversi {@code "+62"}, pemotongan panjang — masih ada di badan method tetapi seluruhnya
	 * DIKOMENTARI. Jadi berbeda dengan telepon rumah, nomor HP tidak dibersihkan; nilai bisa saja
	 * mengandung spasi, tanda hubung, atau awalan {@code "+62"}. Pemanggil yang butuh format seragam
	 * harus menormalisasi sendiri.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code hp} dan {@code mahasiswa} ditulis ulang. Kegagalan
	 * pembacaan data PMB ditelan dan dicatat lewat {@code ErrorAuditUtil}.</p>
	 *
	 * @return nomor HP, bisa {@code null} bila tidak pernah diisi dan tidak ada data PMB
	 */
	@Column(name = "hp", length = 200)
	public String getHp() {

//		if (hp != null && !Common.isNumber(hp)) {
//			hp = hp.replaceAll("[^\\d.]", "");
//		}
//
//		if (hp == null) {
//			hp = "0810000000000000000000";
//		}
//
//		hp = org.apache.commons.lang3.StringUtils.replace(hp, "+62", "0");
//		hp = org.apache.commons.lang3.StringUtils.replace(hp, "-", "");
//
//		if (hp.length() < 10) {
//			hp = (hp + "00000000000000000").substring(0, 10);
//		}
//
//		if (hp.length() > 20) {
//			hp = hp.substring(0, 20);
//		}
//
//		if (hp.contains("0000000000") && !getTeleponRumah().contains("0000000000")) {
//			hp = getTeleponRumah();
//		}

		try {
			mahasiswa = getMahasiswa();
			if (mahasiswa != null && mahasiswa.getBiodataCalonMahasiswa() != null
					&& mahasiswa.getBiodataCalonMahasiswa() > 0L && (hp == null || hp.isEmpty())) {
				BiodataCalonMahasiswa b = (BiodataCalonMahasiswa) ConstantValues
						.ambil(BiodataCalonMahasiswa.class.getName(), mahasiswa.getBiodataCalonMahasiswa());
				if (b != null && b.getHp() != null) {
					hp = b.getHp();
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/BiodataMahasiswa.java:480");
		}

		if (hp != null && hp.equalsIgnoreCase("08100000000000000000")) {
			hp = "";
		}

		return this.hp;
	}

	/**
	 * Menyetel nomor telepon seluler.
	 *
	 * @param hp nomor HP (tidak dinormalisasi, lihat {@link #getHp()})
	 */
	public void setHp(String hp) {
		this.hp = hp;
	}

	/**
	 * Nomor Surat Izin Mengemudi (kolom {@code surat_izin_mengemudi}, maks. 50 karakter).
	 *
	 * @return nomor SIM, atau {@code null} bila tidak ada
	 */
	@Column(name = "surat_izin_mengemudi", length = 50)
	public String getSuratIzinMengemudi() {
		return this.suratIzinMengemudi;
	}

	/**
	 * Menyetel nomor Surat Izin Mengemudi.
	 *
	 * @param suratIzinMengemudi nomor SIM
	 */
	public void setSuratIzinMengemudi(String suratIzinMengemudi) {
		this.suratIzinMengemudi = suratIzinMengemudi;
	}

	/**
	 * Kendaraan yang biasa dipakai ke kampus (kolom {@code kendaraan_kuliah}, maks. 50 karakter),
	 * berupa teks bebas.
	 *
	 * <p>Berbeda dengan {@link #getAlatTransportasiMahasiswa()} yang memakai daftar acuan baku
	 * {@link AlatTransportasiMahasiswa} untuk pelaporan; kolom ini isian bebas.</p>
	 *
	 * @return keterangan kendaraan, atau {@code null} bila belum diisi
	 */
	@Column(name = "kendaraan_kuliah", length = 50)
	public String getKendaraanKuliah() {
		return this.kendaraanKuliah;
	}

	/**
	 * Menyetel keterangan kendaraan yang dipakai ke kampus.
	 *
	 * @param kendaraanKuliah teks bebas
	 */
	public void setKendaraanKuliah(String kendaraanKuliah) {
		this.kendaraanKuliah = kendaraanKuliah;
	}

	/**
	 * Penanda apakah mahasiswa pernah menjadi pemimpin organisasi (kolom
	 * {@code pernah_memimpin_organisasi}), boolean bergaya {@code Integer}: {@code 0} = tidak,
	 * {@code 1} = pernah.
	 *
	 * <p><b>Efek samping ringan:</b> nilai {@code null} diubah menjadi {@code 0} dan ditulis ke field,
	 * sehingga baris lama yang menyimpan NULL akan "terisi" 0 pada penyimpanan berikutnya.</p>
	 *
	 * @return {@code 0} atau {@code 1}, tidak pernah {@code null}
	 * @see #getNamaOrganisasi()
	 */
	@Column(name = "pernah_memimpin_organisasi")
	public Integer getPernahMemimpinOrganisasi() {
		if (pernahMemimpinOrganisasi == null) {
			pernahMemimpinOrganisasi = 0;
		}
		return this.pernahMemimpinOrganisasi;
	}

	/**
	 * Menyetel penanda pernah memimpin organisasi.
	 *
	 * @param pernahMemimpinOrganisasi {@code 0} untuk tidak, {@code 1} untuk pernah
	 */
	public void setPernahMemimpinOrganisasi(Integer pernahMemimpinOrganisasi) {
		this.pernahMemimpinOrganisasi = pernahMemimpinOrganisasi;
	}

	/**
	 * Nama organisasi yang pernah dipimpin (kolom {@code nama_organisasi}, maks. 50 karakter).
	 * Pelengkap {@link #getPernahMemimpinOrganisasi()}.
	 *
	 * @return nama organisasi, atau {@code null} bila tidak ada
	 */
	@Column(name = "nama_organisasi", length = 50)
	public String getNamaOrganisasi() {
		return this.namaOrganisasi;
	}

	/**
	 * Menyetel nama organisasi yang pernah dipimpin.
	 *
	 * @param namaOrganisasi nama organisasi
	 */
	public void setNamaOrganisasi(String namaOrganisasi) {
		this.namaOrganisasi = namaOrganisasi;
	}

	/**
	 * Hobi mahasiswa (kolom {@code hobi}), teks bebas. Dipakai untuk profil dan pemetaan minat
	 * kegiatan kemahasiswaan.
	 *
	 * @return hobi, atau {@code null} bila belum diisi
	 */
	@Column(name = "hobi")
	public String getHobi() {
		return this.hobi;
	}

	/**
	 * Menyetel hobi mahasiswa.
	 *
	 * @param hobi teks bebas
	 */
	public void setHobi(String hobi) {
		this.hobi = hobi;
	}

	/**
	 * Minat di bidang seni (kolom {@code minat_seni}), teks bebas — pelengkap {@link #getHobi()}
	 * untuk penjaringan unit kegiatan mahasiswa.
	 *
	 * @return minat seni, atau {@code null} bila belum diisi
	 */
	@Column(name = "minat_seni")
	public String getMinatSeni() {
		return this.minatSeni;
	}

	/**
	 * Menyetel minat di bidang seni.
	 *
	 * @param minatSeni teks bebas
	 */
	public void setMinatSeni(String minatSeni) {
		this.minatSeni = minatSeni;
	}

	/**
	 * Bahasa asing yang dikuasai, slot pertama (kolom {@code kemampuan_bahasa1}, maks. 50 karakter).
	 *
	 * <p>Tiga slot bahasa disediakan sebagai kolom terpisah ({@code kemampuanBahasa1..3}) alih-alih
	 * tabel anak, sehingga jumlah bahasa yang bisa dicatat dibatasi tiga.</p>
	 *
	 * @return nama/keterangan bahasa, atau {@code null} bila belum diisi
	 */
	@Column(name = "kemampuan_bahasa1", length = 50)
	public String getKemampuanBahasa1() {
		return this.kemampuanBahasa1;
	}

	/**
	 * Menyetel bahasa asing slot pertama.
	 *
	 * @param kemampuanBahasa1 teks bebas
	 */
	public void setKemampuanBahasa1(String kemampuanBahasa1) {
		this.kemampuanBahasa1 = kemampuanBahasa1;
	}

	/**
	 * Bahasa asing yang dikuasai, slot kedua (kolom {@code kemampuan_bahasa2}, maks. 50 karakter).
	 *
	 * @return nama/keterangan bahasa, atau {@code null} bila belum diisi
	 * @see #getKemampuanBahasa1()
	 */
	@Column(name = "kemampuan_bahasa2", length = 50)
	public String getKemampuanBahasa2() {
		return this.kemampuanBahasa2;
	}

	/**
	 * Menyetel bahasa asing slot kedua.
	 *
	 * @param kemampuanBahasa2 teks bebas
	 */
	public void setKemampuanBahasa2(String kemampuanBahasa2) {
		this.kemampuanBahasa2 = kemampuanBahasa2;
	}

	/**
	 * Bahasa asing yang dikuasai, slot ketiga (kolom {@code kemampuan_bahasa3}, maks. 50 karakter).
	 *
	 * @return nama/keterangan bahasa, atau {@code null} bila belum diisi
	 * @see #getKemampuanBahasa1()
	 */
	@Column(name = "kemampuan_bahasa3", length = 50)
	public String getKemampuanBahasa3() {
		return this.kemampuanBahasa3;
	}

	/**
	 * Menyetel bahasa asing slot ketiga.
	 *
	 * @param kemampuanBahasa3 teks bebas
	 */
	public void setKemampuanBahasa3(String kemampuanBahasa3) {
		this.kemampuanBahasa3 = kemampuanBahasa3;
	}

	@Column(name = "asal_sma", length = 255)
	public String getAsalSma() {
		namaSekolahAsal = getNamaSekolahAsal();
		if (namaSekolahAsal != null && namaSekolahAsal.getNama() != null
				&& !namaSekolahAsal.getNama().trim().isEmpty()) {
			asalSma = namaSekolahAsal.getNama();
		}
		return potongKolom50(this.asalSma);
	}

	public void setAsalSma(String asalSma) {
		this.asalSma = asalSma;
	}

	@Column(name = "alamat_asal_sma")
	public String getAlamatAsalSma() {
		return this.alamatAsalSma;
	}

	public void setAlamatAsalSma(String alamatAsalSma) {
		this.alamatAsalSma = alamatAsalSma;
	}

	@Column(name = "asal_smp", length = 50)
	public String getAsalSmp() {
		return this.asalSmp == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSmp.trim(), "'", ""), "\"", "");
	}

	public void setAsalSmp(String asalSmp) {
		this.asalSmp = asalSmp;
	}

	@Column(name = "alamat_asal_smp")
	public String getAlamatAsalSmp() {
		return this.alamatAsalSmp;
	}

	public void setAlamatAsalSmp(String alamatAsalSmp) {
		this.alamatAsalSmp = alamatAsalSmp;
	}

	@Column(name = "asal_sd", length = 50)
	public String getAsalSd() {
		return this.asalSd == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSd.trim(), "'", ""), "\"", "");
	}

	public void setAsalSd(String asalSd) {
		this.asalSd = asalSd;
	}

	@Column(name = "alamat_asal_sd")
	public String getAlamatAsalSd() {
		return this.alamatAsalSd;
	}

	public void setAlamatAsalSd(String alamatAsalSd) {
		this.alamatAsalSd = alamatAsalSd;
	}

	@Column(name = "golongan_darah", length = 10)
	public String getGolonganDarah() {
		if (mahasiswa != null && mahasiswa.getGolongan_darah() != null && !mahasiswa.getGolongan_darah().isEmpty()) {
			golonganDarah = mahasiswa.getGolongan_darah();
		}
		return this.golonganDarah;
	}

	public void setGolonganDarah(String golonganDarah) {
		// Kolom DB golongan_darah varchar(10). Field ini diisi dari Textbox bebas
		// (BiodataMahasiswaAction) tanpa batas panjang di sisi UI, sehingga input yang
		// tidak wajar (mis. user salah ketik kalimat panjang) bisa memicu
		// "value too long for type character varying(10)" saat INSERT/UPDATE. Potong
		// aman di sini (bukan mengubah skema) supaya simpan tetap jalan.
		if (golonganDarah != null && golonganDarah.length() > 10) {
			golonganDarah = golonganDarah.substring(0, 10);
		}
		this.golonganDarah = golonganDarah;
	}

	@Column(name = "status_nikah")
	public Integer getStatusNikah() {
		if (statusNikah == null) {
			statusNikah = 0;
		}
		return this.statusNikah;
	}

	public void setStatusNikah(Integer statusNikah) {
		this.statusNikah = statusNikah;
	}

	@Column(name = "kewarganegaraan", length = 10)
	public String getKewarganegaraan() {
		return this.kewarganegaraan;
	}

	public void setKewarganegaraan(String kewarganegaraan) {
		this.kewarganegaraan = kewarganegaraan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "agama", nullable = true)
	public Agama getAgama() {
		mahasiswa = getMahasiswa();
		if (mahasiswa != null && mahasiswa.getAgama() != null) {
			agama = mahasiswa.getAgama();
		}
		agama = check(agama);
		return this.agama;
	}

	public void setAgama(Agama agama) {
		this.agama = agama;
	}

	public void setPenghasilanAyah(String penghasilanAyah) {
		this.penghasilanAyah = penghasilanAyah;
	}

	public String getPenghasilanAyah() {
		if (pendapatanOrtu != null) {
			penghasilanAyah = "Rp. " + Common.numberFormat.get().format(pendapatanOrtu.getMulaiDari()) + " s.d "
					+ Common.numberFormat.get().format(pendapatanOrtu.getSampai());
		}
		return penghasilanAyah;
	}

	public void setPenghasilanIbu(String penghasilanIbu) {
		this.penghasilanIbu = penghasilanIbu;
	}

	public String getPenghasilanIbu() {
		if (pendapatanOrtuIbu != null) {
			penghasilanIbu = "Rp. " + Common.numberFormat.get().format(pendapatanOrtuIbu.getMulaiDari()) + " s.d "
					+ Common.numberFormat.get().format(pendapatanOrtuIbu.getSampai());
		}
		return penghasilanIbu;
	}

	@Column(name = "kode_biaya_studi")
	public String getBiayaStudi() {
		return biayaStudi;
	}

	public void setBiayaStudi(String biayaStudi) {
		this.biayaStudi = biayaStudi;
	}

	@Column(name = "kode_kerjaan")
	public String getKodeKerjaan() {
		return kodeKerjaan;
	}

	public void setKodeKerjaan(String kodeKerjaan) {
		this.kodeKerjaan = kodeKerjaan;
	}

	@Column(name = "tempat_kerja")
	public String getTempatKerja() {
		return tempatKerja;
	}

	public void setTempatKerja(String tempatKerja) {
		this.tempatKerja = tempatKerja;
	}

	@Column(name = "kode_tempat_kerja_pt")
	public String getKodeTempatKerjaPt() {
		return kodeTempatKerjaPt;
	}

	public void setKodeTempatKerjaPt(String kodeTempatKerjaPt) {
		this.kodeTempatKerjaPt = kodeTempatKerjaPt;
	}

	@Column(name = "kode_tempat_kerja_ps")
	public String getKodeTempatKerjaPs() {
		return kodeTempatKerjaPs;
	}

	public void setKodeTempatKerjaPs(String kodeTempatKerjaPs) {
		this.kodeTempatKerjaPs = kodeTempatKerjaPs;
	}

	@Column(name = "nidn_promotor")
	public String getNidnPromotor() {
		return nidnPromotor;
	}

	public void setNidnPromotor(String nidnPromotor) {
		this.nidnPromotor = nidnPromotor;
	}

	@Column(name = "nidn_kopromotor1")
	public String getNidnKoPromotor1() {
		return nidnKoPromotor1;
	}

	public void setNidnKoPromotor1(String nidnKoPromotor1) {
		this.nidnKoPromotor1 = nidnKoPromotor1;
	}

	@Column(name = "nidn_kopromotor2")
	public String getNidnKoPromotor2() {
		return nidnKoPromotor2;
	}

	public void setNidnKoPromotor2(String nidnKoPromotor2) {
		this.nidnKoPromotor2 = nidnKoPromotor2;
	}

	@Column(name = "nidn_kopromotor3")
	public String getNidnKoPromotor3() {
		return nidnKoPromotor3;
	}

	public void setNidnKoPromotor3(String nidnKoPromotor3) {
		this.nidnKoPromotor3 = nidnKoPromotor3;
	}

	@Column(name = "nidn_kopromotor4")
	public String getNidnKoPromotor4() {
		return nidnKoPromotor4;
	}

	public void setNidnKoPromotor4(String nidnKoPromotor4) {
		this.nidnKoPromotor4 = nidnKoPromotor4;
	}

	@Column(name = "penghasilan_orang_tua")
	public Long getPenghasilanOrangTua() {
		if (pendapatanOrtu != null) {
			penghasilanOrangTua = pendapatanOrtu.getSampai().longValue();
		}
		return penghasilanOrangTua;
	}

	public void setPenghasilanOrangTua(Long penghasilanOrangTua) {
		this.penghasilanOrangTua = penghasilanOrangTua;
	}

	@Column(name = "bersaudara")
	public Integer getBersaudara() {
		if (bersaudara == null || bersaudara <= 0) {
			bersaudara = 1;
		}
		return bersaudara;
	}

	public void setBersaudara(Integer bersaudara) {
		this.bersaudara = bersaudara;
	}

	@Column(name = "no_rek_bri")
	public String getNo_rek_bri() {
		return no_rek_bri;
	}

	public void setNo_rek_bri(String no_rek_bri) {
		this.no_rek_bri = no_rek_bri;
	}

	@Column(name = "cabang_bri")
	public String getCabangBri() {
		return cabangBri;
	}

	public void setCabangBri(String cabangBri) {
		this.cabangBri = cabangBri;
	}

	@Column(name = "no_kk")
	public String getNoKK() {
		return noKK;
	}

	public void setNoKK(String noKK) {
		this.noKK = noKK;
	}

	public String getRt() {

		if (rt != null && !Common.isNumber(rt)) {
			rt = rt.replaceAll("[^\\d.]", "");
		}

		if (rt == null) {
			rt = "";
		}

		if (rt.equalsIgnoreCase("00")) {
			rt = "";
		}

		rt = org.apache.commons.lang3.StringUtils.replace(rt, "-", "");

		return rt;
	}

	public void setRt(String rt) {
		this.rt = rt;
	}

	public String getRw() {

		if (rw != null && !Common.isNumber(rw)) {
			rw = rw.replaceAll("[^\\d.]", "");
		}

		if (rw == null) {
			rw = "";
		}

		if (rw.equalsIgnoreCase("00")) {
			rw = "";
		}

		rw = org.apache.commons.lang3.StringUtils.replace(rw, "-", "");

		return rw;
	}

	public void setRw(String rw) {
		this.rw = rw;
	}

	public String getKodepos() {

		if (kodepos != null && !Common.isNumber(kodepos)) {
			kodepos = kodepos.replaceAll("[^\\d.]", "");
		}

		if (kodepos == null) {
			kodepos = "";
		}

		if (kodepos.contains("0000")) {
			kodepos = "";
		}

		kodepos = org.apache.commons.lang3.StringUtils.replace(kodepos, "-", "");

		return kodepos.trim();
	}

	public void setKodepos(String kodepos) {
		this.kodepos = kodepos;
	}

	public String getKelurahan() {
		if (kelurahan == null || kelurahan.trim().isEmpty()) {
			kelurahan = "-";
		}
		return kelurahan.trim();
	}

	public void setKelurahan(String kelurahan) {
		this.kelurahan = kelurahan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kecamatan_wilayah", nullable = true)
	public Wilayah getKecamatan() {
		kecamatan = check(kecamatan);

		if (kecamatan != null && kecamatan.getWilayahInduk() == null && kecamatan.getFeeder() != null) {
			String targetFeeder = kecamatan.getFeeder();
			Map<?, ?> mapWilayah = ConstantValues.ambilBerdasarClass(Wilayah.class);

			if (mapWilayah != null) {
				for (Object o : mapWilayah.values()) {
					if (o instanceof Wilayah) {
						Wilayah w = (Wilayah) o;
						if (w.getWilayahInduk() != null && targetFeeder.equals(w.getFeeder())) {
							kecamatan = w;
							break;
						}
					}
				}
			}
		}

		return kecamatan;
	}

	public void setKecamatan(Wilayah kecamatan) {
		this.kecamatan = kecamatan;
	}

	// Helper Method untuk menghilangkan duplikasi di getPropinsi dan getKota
	@SuppressWarnings({ "deprecation" })
	private Propinsi findOrCreatePropinsi(Session session, String namaProp) {
		if (namaProp == null || namaProp.trim().isEmpty()) {
			return null;
		}

		String cleanNamaProp = org.apache.commons.lang3.StringUtils.replace(namaProp, "Prop.", "").trim().toLowerCase();

		List<Propinsi> propinsis = ConstantValues.simpleList(session.createCriteria(Propinsi.class)
				.add(Restrictions.isNotNull("nama")).add(Restrictions.ne("nama", "")), Propinsi.class);

		Propinsi selectedPropinsi = null;
		int minDistance = Integer.MAX_VALUE;

		// Mengganti TreeMap dengan pencarian linear agar jauh lebih hemat memori
		for (Propinsi p : propinsis) {
			String namaP = p.getNama();
			if (namaP != null) {
				String cleanNamaP = org.apache.commons.lang3.StringUtils.replace(namaP, "Prop.", "").trim()
						.toLowerCase();
				int distance = org.apache.commons.lang3.StringUtils.getLevenshteinDistance(cleanNamaP, cleanNamaProp);

				if (distance < minDistance) {
					minDistance = distance;
					selectedPropinsi = p;
				}
			}
		}

		if (selectedPropinsi != null && minDistance < 2) {
			return selectedPropinsi;
		}

		// Jika tidak ditemukan kecocokan (distance >= 2), buat baru
		selectedPropinsi = new Propinsi();
		selectedPropinsi.setNama(namaProp.trim());
		selectedPropinsi.setNegara(ConstantValues.INDONESIA);

		org.hibernate.Transaction tx = session.getTransaction();
		boolean isNewTransaction = false;

		if (tx != null && !tx.isActive()) {
			tx.begin();
			isNewTransaction = true;
		}

		session.save(selectedPropinsi);

		if (isNewTransaction) {
			tx.commit();
		}

		return selectedPropinsi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi", nullable = true)
	public Propinsi getPropinsi() {
		propinsi = check(propinsi);
		kecamatan = getKecamatan();
		kota = getKota();

		if (kota != null && kota.getPropinsi() != null) {
			propinsi = kota.getPropinsi();
		} else if (propinsi == null && kecamatan != null && kecamatan.getWilayahInduk() != null) {
			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				Wilayah wilayahKab = kecamatan.getWilayahInduk();
				Wilayah wilayahProp = wilayahKab.getWilayahInduk();

				if (wilayahProp != null) {
					propinsi = findOrCreatePropinsi(session, wilayahProp.getNama());
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				try {
					session.disconnect();
					session.close();
					HibernateUtil.closeSession();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/BiodataMahasiswa.java:1045");
					// Abaikan jika error saat menutup session
				}
			}
		}

		return propinsi;
	}

	public void setPropinsi(Propinsi propinsi) {
		this.propinsi = propinsi;
	}

	@SuppressWarnings({ "deprecation" })
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota", nullable = true)
	public Kota getKota() {
		kota = check(kota);
		kecamatan = getKecamatan();

		if (kota == null && kecamatan != null && kecamatan.getWilayahInduk() != null) {
			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				Wilayah wilayahKab = kecamatan.getWilayahInduk();
				Wilayah wilayahProp = wilayahKab.getWilayahInduk();

				Propinsi selectedPropinsi = null;
				if (wilayahProp != null) {
					selectedPropinsi = findOrCreatePropinsi(session, wilayahProp.getNama());
					propinsi = selectedPropinsi; // Set nilai ke variable global seperti logika aslinya
				}

				if (selectedPropinsi != null) {
					List<Kota> kotas = ConstantValues.simpleList(
							session.createCriteria(Kota.class).add(Restrictions.eq("propinsi", selectedPropinsi))
									.add(Restrictions.isNotNull("nama")).add(Restrictions.ne("nama", "")),
							Kota.class);

					String namaKab = wilayahKab.getNama();
					if (namaKab != null) {
						String cleanNamaKab = org.apache.commons.lang3.StringUtils.replace(namaKab, "Kab.", "");
						cleanNamaKab = org.apache.commons.lang3.StringUtils.replace(cleanNamaKab, "Kota", "").trim()
								.toLowerCase();

						Kota bestKota = null;
						int minDistance = Integer.MAX_VALUE;

						// Mengganti TreeMap dengan pencarian linear hemat memori
						for (Kota k : kotas) {
							String namaK = k.getNama();
							if (namaK != null) {
								String cleanNamaK = org.apache.commons.lang3.StringUtils.replace(namaK, "Kab.", "");
								cleanNamaK = org.apache.commons.lang3.StringUtils.replace(cleanNamaK, "Kota", "").trim()
										.toLowerCase();

								int distance = org.apache.commons.lang3.StringUtils.getLevenshteinDistance(cleanNamaK,
										cleanNamaKab);

								if (distance < minDistance) {
									minDistance = distance;
									bestKota = k;
								}
							}
						}

						if (bestKota != null && minDistance < 2) {
							kota = bestKota;
						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				try {
					session.disconnect();
					session.close();
					HibernateUtil.closeSession();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/BiodataMahasiswa.java:1123");
					// Abaikan jika error saat menutup session
				}
			}
		}
		return kota;
	}

	public void setKota(Kota kota) {
		this.kota = kota;
	}

	public String getNoIdentitas() {
		if (noIdentitas != null && !Common.isNumber(noIdentitas)) {
			noIdentitas = noIdentitas.replaceAll("[^\\d.]", "");
		}

		noIdentitas = org.apache.commons.lang3.StringUtils.replace(noIdentitas, "-", "");

		if (noIdentitas == null || noIdentitas.trim().isEmpty()) {

			try {
				mahasiswa = getMahasiswa();
				if (mahasiswa != null && mahasiswa.getBiodataCalonMahasiswa() != null
						&& mahasiswa.getBiodataCalonMahasiswa() > 0L
						&& (noIdentitas == null || noIdentitas.trim().isEmpty() || noIdentitas.contains("0000"))) {
					BiodataCalonMahasiswa b = (BiodataCalonMahasiswa) ConstantValues
							.ambil(BiodataCalonMahasiswa.class.getName(), mahasiswa.getBiodataCalonMahasiswa());
					if (b != null && b.getNoIdentitas() != null && !b.getNoIdentitas().isEmpty()) {
						noIdentitas = b.getNoIdentitas();
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/BiodataMahasiswa.java:1156");
			}
		}

		return noIdentitas == null ? "" : noIdentitas.trim();
	}

	public void setNoIdentitas(String noIdentitas) {
		this.noIdentitas = noIdentitas;
	}

	public String getDusun() {
		if (dusun == null) {
			dusun = "";
		}
		return dusun;
	}

	public void setDusun(String dusun) {
		this.dusun = dusun;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendapatan_ortu", nullable = true)
	public PendapatanOrangTua getPendapatanOrtu() {
		pendapatanOrtu = check(pendapatanOrtu);
		return pendapatanOrtu;
	}

	public void setPendapatanOrtu(PendapatanOrangTua pendapatanOrtu) {
		this.pendapatanOrtu = pendapatanOrtu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendapatan_ortu_ibu", nullable = true)
	public PendapatanOrangTua getPendapatanOrtuIbu() {
		pendapatanOrtuIbu = check(pendapatanOrtuIbu);
		return pendapatanOrtuIbu;
	}

	public void setPendapatanOrtuIbu(PendapatanOrangTua pendapatanOrtuIbu) {
		this.pendapatanOrtuIbu = pendapatanOrtuIbu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_sekolah", nullable = true)
	public JenisSekolahMahasiswaBaru getJenisSekolah() {
		jenisSekolah = check(jenisSekolah);
		return jenisSekolah;
	}

	public void setJenisSekolah(JenisSekolahMahasiswaBaru jenisSekolah) {
		this.jenisSekolah = jenisSekolah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_ayah", nullable = true)
	public PendidikanOrangTua getPendidikanAyah() {
		pendidikanAyah = check(pendidikanAyah);
		return pendidikanAyah;
	}

	public void setPendidikanAyah(PendidikanOrangTua pendidikanAyah) {
		this.pendidikanAyah = pendidikanAyah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_ibu", nullable = true)
	public PendidikanOrangTua getPendidikanIbu() {
		pendidikanIbu = check(pendidikanIbu);
		return pendidikanIbu;
	}

	public void setPendidikanIbu(PendidikanOrangTua pendidikanIbu) {
		this.pendidikanIbu = pendidikanIbu;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalLahirAyah() {
		return tanggalLahirAyah;
	}

	public void setTanggalLahirAyah(Date tanggalLahirAyah) {
		this.tanggalLahirAyah = tanggalLahirAyah;
	}

	public String getNamaWali() {
		return namaWali;
	}

	public void setNamaWali(String namaWali) {
		this.namaWali = namaWali;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalLahirWali() {
		return tanggalLahirWali;
	}

	public void setTanggalLahirWali(Date tanggalLahirWali) {
		this.tanggalLahirWali = tanggalLahirWali;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pekerjaan_wali", nullable = true)
	public PekerjaanOrangTua getPekerjaanWali() {
		pekerjaanWali = check(pekerjaanWali);
		return pekerjaanWali;
	}

	public void setPekerjaanWali(PekerjaanOrangTua pekerjaanWali) {
		this.pekerjaanWali = pekerjaanWali;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_wali", nullable = true)
	public PendidikanOrangTua getPendidikanWali() {
		pendidikanWali = check(pendidikanWali);
		return pendidikanWali;
	}

	public void setPendidikanWali(PendidikanOrangTua pendidikanWali) {
		this.pendidikanWali = pendidikanWali;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendapatan_wali", nullable = true)
	public PendapatanOrangTua getPendapatanWali() {
		pendapatanWali = check(pendapatanWali);
		return pendapatanWali;
	}

	public void setPendapatanWali(PendapatanOrangTua pendapatanWali) {
		this.pendapatanWali = pendapatanWali;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalLahirIbu() {
		return tanggalLahirIbu;
	}

	public void setTanggalLahirIbu(Date tanggalLahirIbu) {
		this.tanggalLahirIbu = tanggalLahirIbu;
	}

	public String getEmail() {
		if (email == null) {
			email = "";
		}

		mahasiswa = getMahasiswa();
		if (email.trim().isEmpty() && mahasiswa != null && mahasiswa.ambilEmail() != null
				&& !mahasiswa.ambilEmail().isEmpty()) {
			email = mahasiswa.ambilEmail();
		}

		return email.trim();
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_tinggal_mahasiswa", nullable = true)
	public JenisTinggalMahasiswa getJenisTinggalMahasiswa() {
		jenisTinggalMahasiswa = check(jenisTinggalMahasiswa);
		return jenisTinggalMahasiswa;
	}

	public void setJenisTinggalMahasiswa(JenisTinggalMahasiswa jenisTinggalMahasiswa) {
		this.jenisTinggalMahasiswa = jenisTinggalMahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alat_transportasi_mahasiswa", nullable = true)
	public AlatTransportasiMahasiswa getAlatTransportasiMahasiswa() {
		alatTransportasiMahasiswa = check(alatTransportasiMahasiswa);
		return alatTransportasiMahasiswa;
	}

	public void setAlatTransportasiMahasiswa(AlatTransportasiMahasiswa alatTransportasiMahasiswa) {
		this.alatTransportasiMahasiswa = alatTransportasiMahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_pendidikan_ayah", nullable = true)
	public Jenjang getJenjangPendidikanAyah() {
		jenjangPendidikanAyah = check(jenjangPendidikanAyah);
		return jenjangPendidikanAyah;
	}

	public void setJenjangPendidikanAyah(Jenjang jenjangPendidikanAyah) {
		this.jenjangPendidikanAyah = jenjangPendidikanAyah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_pendidikan_ibu", nullable = true)
	public Jenjang getJenjangPendidikanIbu() {
		jenjangPendidikanIbu = check(jenjangPendidikanIbu);
		return jenjangPendidikanIbu;
	}

	public void setJenjangPendidikanIbu(Jenjang jenjangPendidikanIbu) {
		this.jenjangPendidikanIbu = jenjangPendidikanIbu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_ayah", nullable = true)
	public Pekerjaan getJenisPekerjaanAyah() {
		jenisPekerjaanAyah = check(jenisPekerjaanAyah);
		return jenisPekerjaanAyah;
	}

	public void setJenisPekerjaanAyah(Pekerjaan jenisPekerjaanAyah) {
		this.jenisPekerjaanAyah = jenisPekerjaanAyah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_ibu", nullable = true)
	public Pekerjaan getJenisPekerjaanIbu() {
		jenisPekerjaanIbu = check(jenisPekerjaanIbu);
		return jenisPekerjaanIbu;
	}

	public void setJenisPekerjaanIbu(Pekerjaan jenisPekerjaanIbu) {
		this.jenisPekerjaanIbu = jenisPekerjaanIbu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penghasilan_ayah", nullable = true)
	public Penghasilan getJenisPenghasilanAyah() {
		jenisPenghasilanAyah = check(jenisPenghasilanAyah);
		return jenisPenghasilanAyah;
	}

	public void setJenisPenghasilanAyah(Penghasilan jenisPenghasilanAyah) {
		this.jenisPenghasilanAyah = jenisPenghasilanAyah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penghasilan_ibu", nullable = true)
	public Penghasilan getJenisPenghasilanIbu() {
		jenisPenghasilanIbu = check(jenisPenghasilanIbu);
		return jenisPenghasilanIbu;
	}

	public void setJenisPenghasilanIbu(Penghasilan jenisPenghasilanIbu) {
		this.jenisPenghasilanIbu = jenisPenghasilanIbu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penghasilan_wali", nullable = true)
	public Penghasilan getJenisPenghasilanWali() {
		jenisPenghasilanWali = check(jenisPenghasilanWali);
		return jenisPenghasilanWali;
	}

	public void setJenisPenghasilanWali(Penghasilan jenisPenghasilanWali) {
		this.jenisPenghasilanWali = jenisPenghasilanWali;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_pendidikan_wali", nullable = true)
	public Jenjang getJenjangPendidikanWali() {
		jenjangPendidikanWali = check(jenjangPendidikanWali);
		return jenjangPendidikanWali;
	}

	public void setJenjangPendidikanWali(Jenjang jenjangPendidikanWali) {
		this.jenjangPendidikanWali = jenjangPendidikanWali;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_wali", nullable = true)
	public Pekerjaan getJenisPekerjaanWali() {
		jenisPekerjaanWali = check(jenisPekerjaanWali);
		return jenisPekerjaanWali;
	}

	public void setJenisPekerjaanWali(Pekerjaan jenisPekerjaanWali) {
		this.jenisPekerjaanWali = jenisPekerjaanWali;
	}

	public Boolean getApakahPernahPaud() {
		if (apakahPernahPaud == null) {
			apakahPernahPaud = false;
		}
		return apakahPernahPaud;
	}

	public void setApakahPernahPaud(Boolean apakahPernahPaud) {
		this.apakahPernahPaud = apakahPernahPaud;
	}

	public Boolean getApakahPernahTk() {
		if (apakahPernahTk == null) {
			apakahPernahTk = false;
		}
		return apakahPernahTk;
	}

	public void setApakahPernahTk(Boolean apakahPernahTk) {
		this.apakahPernahTk = apakahPernahTk;
	}

	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		// if(parameterTambahan.trim().isEmpty() &&
		// !parameterTambahan.startsWith("{"))
		// {
		//
		// }
		//
		return parameterTambahan;
	}

	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	public List<CommonVO> ambilDataParameterTambahan() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getParameterTambahan().split("\n");
		for (int j = 0; j < splNama.length; j++) {
			CommonVO commonVO = new CommonVO();
			String namaCol = splNama.length > j ? splNama[j] : "";

			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = 1;
			try {
				nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataMahasiswa.java:1495");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataMahasiswa.java:1501");

			}
			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	public void populateParameterTambahan(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				KelompokParameterTambahanMahasiswa kelompokParameterTambahanMahasiswa = (KelompokParameterTambahanMahasiswa) row
						.getAttribute("kelompokParameterTambahanMahasiswa");
				Long indexKe = (Long) row.getAttribute("indexKe");
				if (parameterTambahan != null && kelompokParameterTambahanMahasiswa != null) {
					String jenis = kelompokParameterTambahanMahasiswa.getId() + "->" + parameterTambahan.getId();

					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);
					String ket = keterangan == null ? "" : keterangan.getValue().trim();

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);

					String url = "";
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(getId(), jenis);
						if (lam != null) {
							try {
								url = lam.createLinkUri();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

					System.out.println("ket => " + ket);

					String s = kelompokParameterTambahanMahasiswa.getNama() + "->" + parameterTambahan.getLabelInputan()
							+ "<=>" + val + "<=>" + url + "<=>" + parameterTambahan.getNomorUrut() + "<=>"
							+ parameterTambahan.getId() + "<=>" + kelompokParameterTambahanMahasiswa.getId() + "<=>"
							+ (indexKe == null ? 0 : indexKe) + "<=>" + ket;

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanMahasiswa.getId() + "->" + parameterTambahan.getId() + "<=>"
							+ val + "<=>" + url + "<=>" + ket;
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}
		return parameterTambahanInds;
	}

	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	public void setNirm(String nirm) {
		this.nirm = nirm;
	}

	public String getNirm() {
		if (nirm == null) {
			nirm = "";
		}

		if (!Common.isNumber(nirm)) {
			nirm = nirm.replaceAll("[^\\d.]", "");
		}

		if (nirm.length() > 20) {
			nirm = nirm.substring(0, 20);
		}
		return nirm.trim();
	}

	public String getNisn() {

		try {

			if (mahasiswa != null && mahasiswa.getBiodataCalonMahasiswa() != null
					&& mahasiswa.getBiodataCalonMahasiswa() > 0L && (nisn == null || nisn.isEmpty())) {
				BiodataCalonMahasiswa b = (BiodataCalonMahasiswa) ConstantValues
						.ambil(BiodataCalonMahasiswa.class.getName(), mahasiswa.getBiodataCalonMahasiswa());
				if (b != null && b.getNisn() != null && !b.getNisn().isEmpty()) {
					nisn = b.getNisn();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataMahasiswa.java:1616");
//			e.printStackTrace();
		}

		if (nisn == null) {
			nisn = "";
		}

		if (!Common.isNumber(nisn)) {
			nisn = nisn.replaceAll("[^\\d.]", "");
		}

		if (nisn.length() > 10) {
			nisn = nisn.substring(0, 10);
		}
		return nisn.trim();
	}

	public void setNisn(String nisn) {
		this.nisn = nisn;
	}

	public String getNpwp() {

		if (npwp == null) {
			npwp = "";
		}

		if (!Common.isNumber(npwp)) {
			npwp = npwp.replaceAll("[^\\d.]", "");
		}

		if (npwp.length() > 15) {
			npwp = npwp.substring(0, 15);
		}
		return npwp;
	}

	public void setNpwp(String npwp) {
		this.npwp = npwp;
	}

	public String getNikAyah() {
		if (nikAyah != null && !Common.isNumber(nikAyah)) {
			nikAyah = nikAyah.replaceAll("[^\\d.]", "");
		}

		nikAyah = org.apache.commons.lang3.StringUtils.replace(nikAyah, "-", "");

		if (nikAyah == null || nikAyah.trim().equalsIgnoreCase("00000")) {
			nikAyah = "";
		}
		return nikAyah;
	}

	public void setNikAyah(String nikAyah) {
		this.nikAyah = nikAyah;
	}

	public String getNikIbu() {
		if (nikIbu != null && !Common.isNumber(nikIbu)) {
			nikIbu = nikIbu.replaceAll("[^\\d.]", "");
		}

		nikIbu = org.apache.commons.lang3.StringUtils.replace(nikIbu, "-", "");

		if (nikIbu == null || nikIbu.trim().equalsIgnoreCase("00000")) {
			nikIbu = "";
		}
		return nikIbu;
	}

	public void setNikIbu(String nikIbu) {
		this.nikIbu = nikIbu;
	}

	@Column(columnDefinition = "text")
	public String getParameterTambahanAlumni() {
		if (parameterTambahanAlumni == null) {
			parameterTambahanAlumni = "";
		}
		return parameterTambahanAlumni;
	}

	public void setParameterTambahanAlumni(String parameterTambahanAlumni) {
		this.parameterTambahanAlumni = parameterTambahanAlumni;
	}

	public void populateParameterTambahanAlumni(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanIndsAlumni = "";
		Long indexKe = null;
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				KelompokParameterTambahanAlumni kelompokParameterTambahanAlumni = (KelompokParameterTambahanAlumni) row
						.getAttribute("kelompokParameterTambahanAlumni");
				indexKe = (Long) row.getAttribute("indexKe");
				if (parameterTambahan != null && kelompokParameterTambahanAlumni != null) {
					String jenis = kelompokParameterTambahanAlumni.getId() + "->" + parameterTambahan.getId();

					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);
					String ket = keterangan == null ? "" : keterangan.getValue().trim();
					String val = ParameterTambahan.ambilVal(row, parameterTambahan);

					String url = "";
					if (parameterTambahan.getHarusMenyertakanLampiran()) {
						LampiranLain lam = LampiranLain.ambil(getId(), jenis);
						if (lam != null) {
							try {
								url = lam.createLinkUri();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

					String s = kelompokParameterTambahanAlumni.getNama() + "->" + parameterTambahan.getLabelInputan()
							+ "<=>" + val + "<=>" + url + "<=>" + parameterTambahan.getNomorUrut() + "<=>"
							+ parameterTambahan.getId() + "<=>" + kelompokParameterTambahanAlumni.getId() + "<=>"
							+ (indexKe == null ? 0 : indexKe) + "<=>" + ket;
					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanAlumni.getId() + "->" + parameterTambahan.getId() + "<=>"
							+ val + "<=>" + url + "<=>" + ket;
					parameterTambahanIndsAlumni += parameterTambahanIndsAlumni.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		if (indexKe != null) {
			setParameterTambahanIndsAlumni(getParameterTambahanIndsAlumni() + parameterTambahanIndsAlumni);
			setParameterTambahanAlumni(getParameterTambahanAlumni() + parameterTambahanStr);
		} else {
			setParameterTambahanIndsAlumni(parameterTambahanIndsAlumni);
			setParameterTambahanAlumni(parameterTambahanStr);
		}
	}

	public List<CommonVO> ambilDataParameterTambahanAlumni() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getParameterTambahanAlumni().split("\n");
		for (int j = 0; j < splNama.length; j++) {
			CommonVO commonVO = new CommonVO();
			String namaCol = splNama.length > j ? splNama[j] : "";

			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = 1;
			try {
				nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
			} catch (Exception e) {

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) {

			}
			Long idKel = 1L;
			try {
				idKel = value.length > 5 ? Long.parseLong(value[5].trim()) : 1L;
			} catch (Exception e) {

			}
			Long noIndex = 0L;
			try {
				noIndex = value.length > 6 ? Long.parseLong(value[6].trim()) : 0L;
			} catch (Exception e) {

			}
			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setName3(idKel.toString());
			commonVO.setName4(noIndex.toString());
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	@Column(columnDefinition = "text")
	public String getParameterTambahanIndsAlumni() {
		if (parameterTambahanIndsAlumni == null) {
			parameterTambahanIndsAlumni = "";
		}
		return parameterTambahanIndsAlumni;
	}

	public void setParameterTambahanIndsAlumni(String parameterTambahanIndsAlumni) {
		this.parameterTambahanIndsAlumni = parameterTambahanIndsAlumni;
	}

	public String getNpsn() {
		namaSekolahAsal = getNamaSekolahAsal();
		if (namaSekolahAsal != null && namaSekolahAsal.getKode() != null
				&& !namaSekolahAsal.getKode().trim().isEmpty()) {
			npsn = namaSekolahAsal.getKode();
		}
		return npsn;
	}

	public void setNpsn(String npsn) {
		this.npsn = npsn;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nama_sekolah_asal", nullable = true)
	public NamaSekolahAsal getNamaSekolahAsal() {
		namaSekolahAsal = check(namaSekolahAsal);
		return namaSekolahAsal;
	}

	public void setNamaSekolahAsal(NamaSekolahAsal namaSekolahAsal) {
		this.namaSekolahAsal = namaSekolahAsal;
	}

	public Boolean getPunyaSkpi() {
		return punyaSkpi == null ? false : punyaSkpi;
	}

	public void setPunyaSkpi(Boolean punyaSkpi) {
		this.punyaSkpi = punyaSkpi;
	}

	public Boolean getPunyaSertifikatBahasaInggris() {
		return punyaSertifikatBahasaInggris == null ? false : punyaSertifikatBahasaInggris;
	}

	public void setPunyaSertifikatBahasaInggris(Boolean punyaSertifikatBahasaInggris) {
		this.punyaSertifikatBahasaInggris = punyaSertifikatBahasaInggris;
	}

	public Boolean getPunyaSertifikatBahasaArab() {
		return punyaSertifikatBahasaArab == null ? false : punyaSertifikatBahasaArab;
	}

	public void setPunyaSertifikatBahasaArab(Boolean punyaSertifikatBahasaArab) {
		this.punyaSertifikatBahasaArab = punyaSertifikatBahasaArab;
	}

	public String getTelpAyah() {
		return telpAyah;
	}

	public void setTelpAyah(String telpAyah) {
		this.telpAyah = telpAyah;
	}

	public String getTelpIbu() {
		return telpIbu;
	}

	public void setTelpIbu(String telpIbu) {
		this.telpIbu = telpIbu;
	}

	public String getTelpWali() {
		return telpWali;
	}

	public void setTelpWali(String telpWali) {
		this.telpWali = telpWali;
	}

	public String getHpProvider() {
		operatorSeluler = getOperatorSeluler();
		if (operatorSeluler != null) {
			hpProvider = operatorSeluler.getNama();
		}
		return hpProvider;
	}

	public void setHpProvider(String hpProvider) {
		this.hpProvider = hpProvider;
	}

	@SuppressWarnings("unchecked")
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "operator_seluler", nullable = true)
	public OperatorSeluler getOperatorSeluler() {
		operatorSeluler = check(operatorSeluler);

		if (operatorSeluler == null && hpProvider != null && !hpProvider.trim().isEmpty()) {
			Map<Long, OperatorSeluler> maps = ConstantValues.ambilBerdasarClass(OperatorSeluler.class);
			for (OperatorSeluler m : maps.values()) {
				if (m != null && (m.getNama().toLowerCase().contains(hpProvider.trim().toLowerCase())
						|| hpProvider.toLowerCase().contains(m.getNama().trim().toLowerCase()))) {
					operatorSeluler = m;
					break;
				}
			}
		}

		return operatorSeluler;
	}

	public void setOperatorSeluler(OperatorSeluler operatorSeluler) {
		this.operatorSeluler = operatorSeluler;
	}

	public Integer ambilSkor(ParameterTambahan parameterTambahanData) {
		Integer totalSkor = 0;
		if (!getParameterTambahan().isEmpty() && parameterTambahanData != null) {
			String[] splNama = getParameterTambahan().split("\n");
			for (int j = 0; j < splNama.length; j++) {
				Integer skor = 0;
				String namaCol = splNama.length > j ? splNama[j] : "";

				String[] value = namaCol.split("<=>");
				String val = value.length > 1 ? value[1].trim() : "";

				ParameterTambahan parameterTambahan = null;
				Long id = 1L;
				try {
					id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
					parameterTambahan = (ParameterTambahan) ConstantValues.ambil(ParameterTambahan.class.getName(), id);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataMahasiswa.java:1949");

				}

				if (parameterTambahan != null && parameterTambahan.getId().equals(parameterTambahanData.getId())
						&& parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.PILIHAN_CUSTOM)) {
					String[] kol = StringUtils.split(val, ":");
					if (kol.length > 1) {
						try {
							skor = Integer.parseInt(kol[1].trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataMahasiswa.java:1959");

						}
					} else {
						try {
							skor = Integer.parseInt(val.trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataMahasiswa.java:1965");

						}
					}
				}
				totalSkor += skor;
			}
		}
		return totalSkor;
	}

	public String getEmailAtasan() {
		return emailAtasan == null ? "" : emailAtasan.trim();
	}

	public void setEmailAtasan(String emailAtasan) {
		this.emailAtasan = emailAtasan;
	}

	@SuppressWarnings({ "rawtypes" })
	public void putPhoto(Map parameters) {
		try {
			BiodataMahasiswa biodataMahasiswa = this;
			biodataMahasiswa.getMahasiswa().putPhoto(parameters);

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/BiodataMahasiswa.java:1991");
		}
	}
}
