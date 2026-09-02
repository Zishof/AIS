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

	/**
	 * Nama SMA/sederajat asal (kolom {@code asal_sma}), diturunkan dari relasi bila ada.
	 *
	 * <p>Bila {@link #getNamaSekolahAsal()} terisi dan namanya tidak kosong, nama dari daftar acuan
	 * {@link NamaSekolahAsal} MENIMPA teks bebas yang tersimpan di kolom {@code asal_sma} — relasi
	 * dianggap sumber kebenaran, kolom teks hanya cadangan untuk data lama sebelum daftar sekolah baku
	 * dipakai. Hasil akhirnya dilewatkan {@link #potongKolom50(String)}.</p>
	 *
	 * <p><b>Kuirk:</b> anotasi kolom menyatakan panjang 255 tetapi nilainya dipotong pada 50 karakter.
	 * Nama sekolah yang panjang akan terpotong walaupun sebenarnya muat di basis data. Selain itu
	 * {@code null} berubah menjadi string kosong.</p>
	 *
	 * @return nama SMA asal, maksimal 50 karakter, tidak pernah {@code null}
	 */
	@Column(name = "asal_sma", length = 255)
	public String getAsalSma() {
		namaSekolahAsal = getNamaSekolahAsal();
		if (namaSekolahAsal != null && namaSekolahAsal.getNama() != null
				&& !namaSekolahAsal.getNama().trim().isEmpty()) {
			asalSma = namaSekolahAsal.getNama();
		}
		return potongKolom50(this.asalSma);
	}

	/**
	 * Menyetel nama SMA asal sebagai teks bebas.
	 *
	 * <p>Nilai ini bisa ditimpa saat dibaca bila {@link #getNamaSekolahAsal()} terisi.</p>
	 *
	 * @param asalSma nama sekolah asal
	 */
	public void setAsalSma(String asalSma) {
		this.asalSma = asalSma;
	}

	/**
	 * Alamat SMA/sederajat asal (kolom {@code alamat_asal_sma}).
	 *
	 * @return alamat sekolah asal, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_sma")
	public String getAlamatAsalSma() {
		return this.alamatAsalSma;
	}

	/**
	 * Menyetel alamat SMA asal.
	 *
	 * @param alamatAsalSma alamat sekolah
	 */
	public void setAlamatAsalSma(String alamatAsalSma) {
		this.alamatAsalSma = alamatAsalSma;
	}

	/**
	 * Nama SMP/sederajat asal (kolom {@code asal_smp}, maks. 50 karakter), sudah dibersihkan.
	 *
	 * <p>Tanda kutip tunggal dan ganda dibuang serta spasi ujung di-{@code trim}. Pembersihan kutip
	 * ini penting karena nilai tersebut ikut dirangkai ke berkas ekspor/laporan berformat teks yang
	 * akan rusak bila mengandung kutip. Berbeda dengan getter penormal lain, method ini TIDAK menulis
	 * balik ke field — pembersihan hanya berlaku pada nilai kembalian.</p>
	 *
	 * @return nama SMP asal tanpa kutip; string kosong bila field {@code null}
	 */
	@Column(name = "asal_smp", length = 50)
	public String getAsalSmp() {
		return this.asalSmp == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSmp.trim(), "'", ""), "\"", "");
	}

	/**
	 * Menyetel nama SMP asal.
	 *
	 * @param asalSmp nama sekolah asal
	 */
	public void setAsalSmp(String asalSmp) {
		this.asalSmp = asalSmp;
	}

	/**
	 * Alamat SMP/sederajat asal (kolom {@code alamat_asal_smp}).
	 *
	 * @return alamat sekolah asal, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_smp")
	public String getAlamatAsalSmp() {
		return this.alamatAsalSmp;
	}

	/**
	 * Menyetel alamat SMP asal.
	 *
	 * @param alamatAsalSmp alamat sekolah
	 */
	public void setAlamatAsalSmp(String alamatAsalSmp) {
		this.alamatAsalSmp = alamatAsalSmp;
	}

	/**
	 * Nama SD/sederajat asal (kolom {@code asal_sd}, maks. 50 karakter), dibersihkan dari tanda kutip
	 * tunggal dan ganda serta di-{@code trim} — sama persis perlakuannya dengan {@link #getAsalSmp()}.
	 *
	 * @return nama SD asal tanpa kutip; string kosong bila field {@code null}
	 */
	@Column(name = "asal_sd", length = 50)
	public String getAsalSd() {
		return this.asalSd == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSd.trim(), "'", ""), "\"", "");
	}

	/**
	 * Menyetel nama SD asal.
	 *
	 * @param asalSd nama sekolah asal
	 */
	public void setAsalSd(String asalSd) {
		this.asalSd = asalSd;
	}

	/**
	 * Alamat SD/sederajat asal (kolom {@code alamat_asal_sd}).
	 *
	 * @return alamat sekolah asal, atau {@code null} bila belum diisi
	 */
	@Column(name = "alamat_asal_sd")
	public String getAlamatAsalSd() {
		return this.alamatAsalSd;
	}

	/**
	 * Menyetel alamat SD asal.
	 *
	 * @param alamatAsalSd alamat sekolah
	 */
	public void setAlamatAsalSd(String alamatAsalSd) {
		this.alamatAsalSd = alamatAsalSd;
	}

	/**
	 * Golongan darah mahasiswa (kolom {@code golongan_darah}, maks. 10 karakter).
	 *
	 * <p>Bila {@link Mahasiswa} punya nilai golongan darah sendiri yang tidak kosong, nilai itu
	 * MENIMPA field di sini.</p>
	 *
	 * <p><b>Dua kuirk yang perlu diketahui.</b> Pertama, pemeriksaan memakai field {@code mahasiswa}
	 * langsung, BUKAN {@link #getMahasiswa()} — jadi sinkronisasi hanya terjadi bila field tersebut
	 * kebetulan sudah terisi oleh pemanggilan getter lain sebelumnya; pada instance yang baru dimuat
	 * Hibernate, sinkronisasi ini diam-diam dilewati. Kedua, penimpaan menulis langsung ke field
	 * sehingga pemotongan panjang di {@link #setGolonganDarah(String)} TIDAK berlaku; bila nilai di
	 * tabel {@code mahasiswa} lebih dari 10 karakter, penyimpanan bisa gagal dengan
	 * {@code value too long for type character varying(10)}.</p>
	 *
	 * @return golongan darah, atau {@code null} bila belum diisi
	 */
	@Column(name = "golongan_darah", length = 10)
	public String getGolonganDarah() {
		if (mahasiswa != null && mahasiswa.getGolongan_darah() != null && !mahasiswa.getGolongan_darah().isEmpty()) {
			golonganDarah = mahasiswa.getGolongan_darah();
		}
		return this.golonganDarah;
	}

	/**
	 * Menyetel golongan darah, dengan pengaman panjang.
	 *
	 * <p>Nilai lebih dari 10 karakter dipotong supaya tidak melanggar lebar kolom
	 * {@code varchar(10)} — lihat komentar di badan method mengenai isian bebas dari
	 * {@code BiodataMahasiswaAction}. Pengaman ini hanya berlaku lewat setter; jalur penimpaan di
	 * {@link #getGolonganDarah()} melewatinya.</p>
	 *
	 * @param golonganDarah golongan darah; dipotong pada 10 karakter bila lebih panjang
	 */
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

	/**
	 * Status pernikahan mahasiswa (kolom {@code status_nikah}), kode numerik.
	 *
	 * <p><b>Efek samping ringan:</b> nilai {@code null} diganti {@code 0} dan ditulis ke field.</p>
	 *
	 * @return kode status nikah, tidak pernah {@code null}
	 */
	@Column(name = "status_nikah")
	public Integer getStatusNikah() {
		if (statusNikah == null) {
			statusNikah = 0;
		}
		return this.statusNikah;
	}

	/**
	 * Menyetel kode status pernikahan.
	 *
	 * @param statusNikah kode status nikah
	 */
	public void setStatusNikah(Integer statusNikah) {
		this.statusNikah = statusNikah;
	}

	/**
	 * Kewarganegaraan mahasiswa (kolom {@code kewarganegaraan}, maks. 10 karakter), umumnya berisi
	 * kode negara singkat seperti {@code "ID"}.
	 *
	 * @return kode kewarganegaraan, atau {@code null} bila belum diisi
	 */
	@Column(name = "kewarganegaraan", length = 10)
	public String getKewarganegaraan() {
		return this.kewarganegaraan;
	}

	/**
	 * Menyetel kewarganegaraan.
	 *
	 * @param kewarganegaraan kode/nama kewarganegaraan (maks. 10 karakter di basis data)
	 */
	public void setKewarganegaraan(String kewarganegaraan) {
		this.kewarganegaraan = kewarganegaraan;
	}

	/**
	 * Agama mahasiswa menurut daftar acuan {@link Agama} (kolom {@code agama}).
	 *
	 * <p>Bila {@link Mahasiswa} sudah punya agama, nilainya MENIMPA field di sini — tabel
	 * {@code mahasiswa} diperlakukan sebagai sumber kebenaran. Berbeda dengan
	 * {@link #getGolonganDarah()}, di sini {@link #getMahasiswa()} benar-benar dipanggil sehingga
	 * sinkronisasi juga berjalan pada instance yang baru dimuat. Setelah itu nilai dilewatkan
	 * {@code check(...)} milik {@link GeneralValueObject} agar proxy Hibernate yang lepas sesi tetap
	 * aman dipakai.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code agama} dan {@code mahasiswa} ditulis ulang.</p>
	 *
	 * @return agama mahasiswa, atau {@code null} bila belum ditentukan di kedua tempat
	 */
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

	/**
	 * Menyetel agama mahasiswa.
	 *
	 * <p>Nilai dapat ditimpa kembali oleh {@link #getAgama()} bila {@link Mahasiswa} punya agama
	 * sendiri.</p>
	 *
	 * @param agama acuan {@link Agama}
	 */
	public void setAgama(Agama agama) {
		this.agama = agama;
	}

	/**
	 * Menyetel keterangan penghasilan ayah dalam bentuk teks bebas.
	 *
	 * <p>Nilai ini akan ditimpa oleh {@link #getPenghasilanAyah()} bila
	 * {@link #getPendapatanOrtu()} terisi.</p>
	 *
	 * @param penghasilanAyah teks penghasilan
	 */
	public void setPenghasilanAyah(String penghasilanAyah) {
		this.penghasilanAyah = penghasilanAyah;
	}

	/**
	 * Keterangan penghasilan ayah dalam bentuk teks siap tampil.
	 *
	 * <p>Bila field relasi {@code pendapatanOrtu} ({@link PendapatanOrangTua}) terisi, teks disusun
	 * ulang dari rentang pendapatan menjadi bentuk
	 * <code>"Rp. {mulaiDari} s.d {sampai}"</code> dengan pemformat angka {@code Common.numberFormat}
	 * (sebuah {@code ThreadLocal}, jadi aman dipakai lintas thread). Bila relasi kosong, teks yang
	 * tersimpan di kolom dikembalikan apa adanya — biasanya data lama sebelum daftar rentang
	 * pendapatan baku dipakai.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code penghasilanAyah} ditimpa hasil format. Pemeriksaan memakai
	 * field {@code pendapatanOrtu} langsung, bukan {@link #getPendapatanOrtu()}, sehingga pada
	 * instance yang belum menyentuh getter relasi tersebut pemformatan bisa terlewat.</p>
	 *
	 * @return teks penghasilan ayah, bisa {@code null} bila kolom kosong dan relasi belum terisi
	 */
	public String getPenghasilanAyah() {
		if (pendapatanOrtu != null) {
			penghasilanAyah = "Rp. " + Common.numberFormat.get().format(pendapatanOrtu.getMulaiDari()) + " s.d "
					+ Common.numberFormat.get().format(pendapatanOrtu.getSampai());
		}
		return penghasilanAyah;
	}

	/**
	 * Menyetel keterangan penghasilan ibu dalam bentuk teks bebas.
	 *
	 * @param penghasilanIbu teks penghasilan
	 */
	public void setPenghasilanIbu(String penghasilanIbu) {
		this.penghasilanIbu = penghasilanIbu;
	}

	/**
	 * Keterangan penghasilan ibu dalam bentuk teks siap tampil — kembaran persis
	 * {@link #getPenghasilanAyah()}, tetapi bersumber dari relasi {@code pendapatanOrtuIbu}.
	 *
	 * @return teks penghasilan ibu, bisa {@code null}
	 * @see #getPenghasilanAyah()
	 */
	public String getPenghasilanIbu() {
		if (pendapatanOrtuIbu != null) {
			penghasilanIbu = "Rp. " + Common.numberFormat.get().format(pendapatanOrtuIbu.getMulaiDari()) + " s.d "
					+ Common.numberFormat.get().format(pendapatanOrtuIbu.getSampai());
		}
		return penghasilanIbu;
	}

	/**
	 * Kode sumber biaya studi (kolom {@code kode_biaya_studi}), mis. biaya sendiri, beasiswa, atau
	 * tanggungan instansi. Dipakai untuk pelaporan PDDikti/Feeder.
	 *
	 * @return kode biaya studi, atau {@code null} bila belum diisi
	 */
	@Column(name = "kode_biaya_studi")
	public String getBiayaStudi() {
		return biayaStudi;
	}

	/**
	 * Menyetel kode sumber biaya studi.
	 *
	 * @param biayaStudi kode biaya studi
	 */
	public void setBiayaStudi(String biayaStudi) {
		this.biayaStudi = biayaStudi;
	}

	/**
	 * Kode jenis pekerjaan mahasiswa sendiri (kolom {@code kode_kerjaan}) — relevan untuk kelas
	 * karyawan/program pascasarjana yang mahasiswanya sudah bekerja.
	 *
	 * @return kode pekerjaan, atau {@code null} bila belum diisi
	 */
	@Column(name = "kode_kerjaan")
	public String getKodeKerjaan() {
		return kodeKerjaan;
	}

	/**
	 * Menyetel kode jenis pekerjaan mahasiswa.
	 *
	 * @param kodeKerjaan kode pekerjaan
	 */
	public void setKodeKerjaan(String kodeKerjaan) {
		this.kodeKerjaan = kodeKerjaan;
	}

	/**
	 * Nama tempat mahasiswa bekerja (kolom {@code tempat_kerja}).
	 *
	 * @return nama tempat kerja, atau {@code null} bila belum diisi
	 */
	@Column(name = "tempat_kerja")
	public String getTempatKerja() {
		return tempatKerja;
	}

	/**
	 * Menyetel nama tempat kerja mahasiswa.
	 *
	 * @param tempatKerja nama instansi/perusahaan
	 */
	public void setTempatKerja(String tempatKerja) {
		this.tempatKerja = tempatKerja;
	}

	/**
	 * Kode perguruan tinggi tempat mahasiswa bekerja (kolom {@code kode_tempat_kerja_pt}).
	 *
	 * <p>Dipakai bila mahasiswa adalah dosen/tenaga kependidikan yang sedang studi lanjut: kode PT
	 * tempat ia mengajar dilaporkan ke PDDikti berpasangan dengan
	 * {@link #getKodeTempatKerjaPs()}.</p>
	 *
	 * @return kode PT tempat kerja, atau {@code null} bila tidak relevan
	 */
	@Column(name = "kode_tempat_kerja_pt")
	public String getKodeTempatKerjaPt() {
		return kodeTempatKerjaPt;
	}

	/**
	 * Menyetel kode perguruan tinggi tempat kerja.
	 *
	 * @param kodeTempatKerjaPt kode PT
	 */
	public void setKodeTempatKerjaPt(String kodeTempatKerjaPt) {
		this.kodeTempatKerjaPt = kodeTempatKerjaPt;
	}

	/**
	 * Kode program studi tempat mahasiswa bekerja (kolom {@code kode_tempat_kerja_ps}), pasangan
	 * {@link #getKodeTempatKerjaPt()}.
	 *
	 * @return kode program studi tempat kerja, atau {@code null} bila tidak relevan
	 */
	@Column(name = "kode_tempat_kerja_ps")
	public String getKodeTempatKerjaPs() {
		return kodeTempatKerjaPs;
	}

	/**
	 * Menyetel kode program studi tempat kerja.
	 *
	 * @param kodeTempatKerjaPs kode program studi
	 */
	public void setKodeTempatKerjaPs(String kodeTempatKerjaPs) {
		this.kodeTempatKerjaPs = kodeTempatKerjaPs;
	}

	/**
	 * NIDN dosen promotor (kolom {@code nidn_promotor}) untuk mahasiswa program doktor.
	 *
	 * <p>Disimpan sebagai NIDN mentah, bukan relasi ke {@link Dosen}, karena promotor bisa berasal
	 * dari perguruan tinggi lain yang datanya tidak ada di basis data ini.</p>
	 *
	 * @return NIDN promotor, atau {@code null} bila tidak ada
	 */
	@Column(name = "nidn_promotor")
	public String getNidnPromotor() {
		return nidnPromotor;
	}

	/**
	 * Menyetel NIDN dosen promotor.
	 *
	 * @param nidnPromotor NIDN promotor
	 */
	public void setNidnPromotor(String nidnPromotor) {
		this.nidnPromotor = nidnPromotor;
	}

	/**
	 * NIDN ko-promotor pertama (kolom {@code nidn_kopromotor1}).
	 *
	 * @return NIDN ko-promotor 1, atau {@code null} bila tidak ada
	 * @see #getNidnPromotor()
	 */
	@Column(name = "nidn_kopromotor1")
	public String getNidnKoPromotor1() {
		return nidnKoPromotor1;
	}

	/**
	 * Menyetel NIDN ko-promotor pertama.
	 *
	 * @param nidnKoPromotor1 NIDN ko-promotor
	 */
	public void setNidnKoPromotor1(String nidnKoPromotor1) {
		this.nidnKoPromotor1 = nidnKoPromotor1;
	}

	/**
	 * NIDN ko-promotor kedua (kolom {@code nidn_kopromotor2}).
	 *
	 * @return NIDN ko-promotor 2, atau {@code null} bila tidak ada
	 * @see #getNidnPromotor()
	 */
	@Column(name = "nidn_kopromotor2")
	public String getNidnKoPromotor2() {
		return nidnKoPromotor2;
	}

	/**
	 * Menyetel NIDN ko-promotor kedua.
	 *
	 * @param nidnKoPromotor2 NIDN ko-promotor
	 */
	public void setNidnKoPromotor2(String nidnKoPromotor2) {
		this.nidnKoPromotor2 = nidnKoPromotor2;
	}

	/**
	 * NIDN ko-promotor ketiga (kolom {@code nidn_kopromotor3}).
	 *
	 * @return NIDN ko-promotor 3, atau {@code null} bila tidak ada
	 * @see #getNidnPromotor()
	 */
	@Column(name = "nidn_kopromotor3")
	public String getNidnKoPromotor3() {
		return nidnKoPromotor3;
	}

	/**
	 * Menyetel NIDN ko-promotor ketiga.
	 *
	 * @param nidnKoPromotor3 NIDN ko-promotor
	 */
	public void setNidnKoPromotor3(String nidnKoPromotor3) {
		this.nidnKoPromotor3 = nidnKoPromotor3;
	}

	/**
	 * NIDN ko-promotor keempat (kolom {@code nidn_kopromotor4}) — slot terakhir yang tersedia.
	 *
	 * @return NIDN ko-promotor 4, atau {@code null} bila tidak ada
	 * @see #getNidnPromotor()
	 */
	@Column(name = "nidn_kopromotor4")
	public String getNidnKoPromotor4() {
		return nidnKoPromotor4;
	}

	/**
	 * Menyetel NIDN ko-promotor keempat.
	 *
	 * @param nidnKoPromotor4 NIDN ko-promotor
	 */
	public void setNidnKoPromotor4(String nidnKoPromotor4) {
		this.nidnKoPromotor4 = nidnKoPromotor4;
	}

	/**
	 * Penghasilan orang tua sebagai angka (kolom {@code penghasilan_orang_tua}), dipakai antara lain
	 * untuk penentuan kelompok UKT dan seleksi beasiswa.
	 *
	 * <p>Bila relasi {@code pendapatanOrtu} terisi, nilainya diambil dari BATAS ATAS rentang
	 * ({@link PendapatanOrangTua#getSampai()}) — bukan nilai tengah maupun batas bawah — lalu ditulis
	 * ke field. Pilihan batas atas ini memengaruhi ambang kelayakan bantuan, jadi jangan diubah tanpa
	 * memeriksa aturan UKT yang berlaku.</p>
	 *
	 * <p><b>Risiko:</b> pemeriksaan hanya menguji {@code pendapatanOrtu != null}; bila baris acuan ada
	 * tetapi kolom {@code sampai}-nya {@code null}, pemanggilan {@code longValue()} melempar
	 * {@code NullPointerException} yang TIDAK ditangkap di sini.</p>
	 *
	 * @return penghasilan orang tua dalam rupiah, atau {@code null} bila tidak ada sumber data
	 */
	@Column(name = "penghasilan_orang_tua")
	public Long getPenghasilanOrangTua() {
		if (pendapatanOrtu != null) {
			penghasilanOrangTua = pendapatanOrtu.getSampai().longValue();
		}
		return penghasilanOrangTua;
	}

	/**
	 * Menyetel penghasilan orang tua dalam rupiah.
	 *
	 * @param penghasilanOrangTua nominal penghasilan
	 */
	public void setPenghasilanOrangTua(Long penghasilanOrangTua) {
		this.penghasilanOrangTua = penghasilanOrangTua;
	}

	/**
	 * Jumlah bersaudara termasuk mahasiswa yang bersangkutan (kolom {@code bersaudara}).
	 *
	 * <p><b>Efek samping:</b> nilai {@code null}, {@code 0}, atau negatif diganti {@code 1} dan
	 * ditulis ke field. Akibatnya "belum diisi" tidak dapat dibedakan dari "anak tunggal" — keduanya
	 * terbaca sebagai 1.</p>
	 *
	 * @return jumlah bersaudara, minimal 1, tidak pernah {@code null}
	 */
	@Column(name = "bersaudara")
	public Integer getBersaudara() {
		if (bersaudara == null || bersaudara <= 0) {
			bersaudara = 1;
		}
		return bersaudara;
	}

	/**
	 * Menyetel jumlah bersaudara.
	 *
	 * @param bersaudara jumlah saudara termasuk dirinya; nilai &lt;= 0 akan dibaca sebagai 1
	 */
	public void setBersaudara(Integer bersaudara) {
		this.bersaudara = bersaudara;
	}

	/**
	 * Nomor rekening bank mahasiswa (kolom {@code no_rek_bri}).
	 *
	 * <p>Penamaan menyebut BRI karena awalnya khusus untuk penyaluran beasiswa lewat bank tersebut;
	 * pada praktiknya dipakai untuk nomor rekening bank apa pun.</p>
	 *
	 * @return nomor rekening, atau {@code null} bila belum diisi
	 */
	@Column(name = "no_rek_bri")
	public String getNo_rek_bri() {
		return no_rek_bri;
	}

	/**
	 * Menyetel nomor rekening bank mahasiswa.
	 *
	 * @param no_rek_bri nomor rekening
	 */
	public void setNo_rek_bri(String no_rek_bri) {
		this.no_rek_bri = no_rek_bri;
	}

	/**
	 * Kantor cabang bank tempat rekening {@link #getNo_rek_bri()} dibuka (kolom {@code cabang_bri}).
	 *
	 * @return nama cabang bank, atau {@code null} bila belum diisi
	 */
	@Column(name = "cabang_bri")
	public String getCabangBri() {
		return cabangBri;
	}

	/**
	 * Menyetel kantor cabang bank.
	 *
	 * @param cabangBri nama cabang
	 */
	public void setCabangBri(String cabangBri) {
		this.cabangBri = cabangBri;
	}

	/**
	 * Nomor Kartu Keluarga (kolom {@code no_kk}).
	 *
	 * <p>Berbeda dengan nomor identitas lain di kelas ini, nilai ini TIDAK dinormalisasi saat dibaca —
	 * dikembalikan apa adanya termasuk kemungkinan spasi dan tanda hubung.</p>
	 *
	 * @return nomor KK, atau {@code null} bila belum diisi
	 */
	@Column(name = "no_kk")
	public String getNoKK() {
		return noKK;
	}

	/**
	 * Menyetel nomor Kartu Keluarga.
	 *
	 * @param noKK nomor KK
	 */
	public void setNoKK(String noKK) {
		this.noKK = noKK;
	}

	/**
	 * Nomor RT tempat tinggal, sudah dinormalisasi.
	 *
	 * <p>Rangkaian pembersihan: karakter selain digit dan titik dibuang bila isinya bukan angka murni,
	 * {@code null} diganti string kosong, nilai {@code "00"} (penanda "tidak ada" versi lama) ikut
	 * dikosongkan, lalu tanda hubung dibuang.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code rt} ditimpa hasil normalisasi sehingga bisa ikut tersimpan
	 * pada flush berikutnya.</p>
	 *
	 * @return nomor RT hasil normalisasi; string kosong bila tidak ada, tidak pernah {@code null}
	 */
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

	/**
	 * Menyetel nomor RT apa adanya (normalisasi terjadi saat dibaca).
	 *
	 * @param rt nomor RT
	 */
	public void setRt(String rt) {
		this.rt = rt;
	}

	/**
	 * Nomor RW tempat tinggal, dinormalisasi dengan aturan yang sama persis dengan {@link #getRt()}.
	 *
	 * @return nomor RW hasil normalisasi; string kosong bila tidak ada, tidak pernah {@code null}
	 * @see #getRt()
	 */
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

	/**
	 * Menyetel nomor RW apa adanya (normalisasi terjadi saat dibaca).
	 *
	 * @param rw nomor RW
	 */
	public void setRw(String rw) {
		this.rw = rw;
	}

	/**
	 * Kode pos alamat tempat tinggal, sudah dinormalisasi.
	 *
	 * <p>Karakter selain digit dan titik dibuang, {@code null} diganti string kosong, tanda hubung
	 * dibuang, dan nilai yang MENGANDUNG {@code "0000"} di mana pun dianggap data sampah lalu
	 * dikosongkan. Perlu dicatat bahwa pemeriksaan memakai {@code contains}, bukan kesamaan penuh,
	 * sehingga kode pos sah yang kebetulan memuat empat nol berurutan (mis. {@code "00001"}) juga ikut
	 * terhapus.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code kodepos} ditimpa hasil normalisasi.</p>
	 *
	 * @return kode pos hasil normalisasi, sudah di-{@code trim}; string kosong bila tidak ada
	 */
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

	/**
	 * Menyetel kode pos apa adanya (normalisasi terjadi saat dibaca).
	 *
	 * @param kodepos kode pos
	 */
	public void setKodepos(String kodepos) {
		this.kodepos = kodepos;
	}

	/**
	 * Nama kelurahan/desa tempat tinggal.
	 *
	 * <p><b>Efek samping:</b> nilai {@code null} atau kosong diganti tanda hubung {@code "-"} dan
	 * ditulis ke field. Penggantian ini membuat isian wajib pada formulir cetak dan berkas ekspor
	 * tidak pernah tampil kosong, tetapi juga berarti "belum diisi" tersimpan sebagai
	 * {@code "-"} di basis data setelah flush berikutnya.</p>
	 *
	 * @return nama kelurahan yang sudah di-{@code trim}, atau {@code "-"} bila belum diisi; tidak
	 *         pernah {@code null}
	 */
	public String getKelurahan() {
		if (kelurahan == null || kelurahan.trim().isEmpty()) {
			kelurahan = "-";
		}
		return kelurahan.trim();
	}

	/**
	 * Menyetel nama kelurahan/desa.
	 *
	 * @param kelurahan nama kelurahan; {@code null}/kosong akan dibaca sebagai {@code "-"}
	 */
	public void setKelurahan(String kelurahan) {
		this.kelurahan = kelurahan;
	}

	/**
	 * Kecamatan tempat tinggal menurut pohon wilayah PDDikti/Feeder (kolom
	 * {@code kecamatan_wilayah}).
	 *
	 * <p>{@link Wilayah} adalah tabel berjenjang: propinsi &rarr; kabupaten/kota &rarr; kecamatan,
	 * dihubungkan lewat {@code wilayahInduk}, dan tiap simpul punya kode {@code feeder}. Kolom di
	 * sini menunjuk simpul tingkat kecamatan, sehingga kabupaten dan propinsi dapat ditelusuri ke
	 * atas — itulah yang dimanfaatkan {@link #getKota()} dan {@link #getPropinsi()}.</p>
	 *
	 * <p><b>Perbaikan otomatis data cacat.</b> Bila simpul yang tersimpan ternyata TIDAK punya induk
	 * (anomali data: baris wilayah yatim) tetapi punya kode {@code feeder}, method ini memuat SELURUH
	 * daftar {@link Wilayah} dari cache {@code ConstantValues.ambilBerdasarClass(...)} lalu mencari
	 * simpul lain dengan kode {@code feeder} sama yang punya induk, dan mengganti referensi ke simpul
	 * itu. Dengan kata lain baris duplikat yang "sehat" dipakai menggantikan yang rusak.</p>
	 *
	 * <p><b>Efek samping &amp; biaya:</b> field {@code kecamatan} ditulis ulang sehingga penggantian
	 * referensi bisa ikut tersimpan pada flush berikutnya; pemindaian menelusuri seluruh isi cache
	 * wilayah (puluhan ribu baris) setiap kali kondisi anomali terpenuhi. Selain itu getter ini
	 * dipanggil di awal {@link #getKota()} dan {@link #getPropinsi()}, jadi biayanya menular ke
	 * keduanya.</p>
	 *
	 * @return simpul wilayah tingkat kecamatan, atau {@code null} bila belum diisi
	 */
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

	/**
	 * Menyetel kecamatan tempat tinggal.
	 *
	 * @param kecamatan simpul {@link Wilayah} tingkat kecamatan
	 */
	public void setKecamatan(Wilayah kecamatan) {
		this.kecamatan = kecamatan;
	}

	/**
	 * Mencari {@link Propinsi} yang namanya paling mirip dengan {@code namaProp}, dan
	 * <b>membuatkan baris baru bila tidak ada yang cukup mirip</b>.
	 *
	 * <p>Helper bersama untuk {@link #getPropinsi()} dan {@link #getKota()} — keduanya perlu
	 * menerjemahkan nama propinsi versi pohon {@link Wilayah} (Feeder) menjadi baris {@link Propinsi}
	 * versi AIS, dan nama di kedua sumber sering berbeda tipis (imbuhan {@code "Prop."}, beda ejaan,
	 * beda kapitalisasi).</p>
	 *
	 * <p>Alurnya:</p>
	 * <ol>
	 * <li>Nama masukan dibersihkan: imbuhan {@code "Prop."} dibuang, di-{@code trim}, dijadikan huruf
	 * kecil.</li>
	 * <li>Seluruh {@link Propinsi} yang namanya tidak {@code null} dan tidak kosong diambil lewat
	 * {@code Criteria}, lalu dibandingkan satu per satu memakai jarak Levenshtein
	 * ({@code commons-lang3}). Komentar di badan method mencatat bahwa pencarian linear ini sengaja
	 * menggantikan {@code TreeMap} demi menghemat memori.</li>
	 * <li>Kandidat dengan jarak terkecil dipakai bila jaraknya &lt; 2 (toleransi maksimal satu
	 * karakter berbeda).</li>
	 * <li>Bila tidak ada yang lolos, {@link Propinsi} BARU dibuat dengan nama masukan apa adanya dan
	 * negara {@code ConstantValues.INDONESIA}, lalu di-{@code save} ke basis data. Bila transaksi
	 * sesi sedang tidak aktif, method membuka transaksi sendiri dan meng-{@code commit}-nya.</li>
	 * </ol>
	 *
	 * <p><b>Peringatan.</b> Ini adalah operasi TULIS yang dipicu dari dalam getter. Menampilkan
	 * biodata seorang mahasiswa yang alamatnya tidak dikenali dapat menambah baris master
	 * {@link Propinsi} baru — termasuk kemungkinan propinsi "hantu" hasil salah ketik. Jangan
	 * memanggil rantai {@link #getPropinsi()}/{@link #getKota()} dalam proses batch tanpa menyadari
	 * hal ini. Ambang jarak 2 juga cukup ketat sehingga selisih dua huruf saja sudah memicu
	 * pembuatan baris baru.</p>
	 *
	 * @param session sesi Hibernate yang dipakai untuk query dan penyimpanan; disediakan pemanggil
	 *         dan TIDAK ditutup di sini (penutupan dilakukan pemanggil pada blok {@code finally})
	 * @param namaProp nama propinsi versi pohon {@link Wilayah}; boleh {@code null}/kosong
	 * @return propinsi yang cocok atau yang baru dibuat; {@code null} hanya bila {@code namaProp}
	 *         {@code null} atau kosong
	 */
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

	/**
	 * Propinsi tempat tinggal (kolom {@code propinsi}), dengan penurunan otomatis dari kota atau dari
	 * pohon {@link Wilayah}.
	 *
	 * <p>Urutan penentuan nilai:</p>
	 * <ol>
	 * <li>Nilai tersimpan dilewatkan {@code check(...)}, lalu {@link #getKecamatan()} dan
	 * {@link #getKota()} dipanggil (keduanya sendiri bisa melakukan query dan penulisan).</li>
	 * <li>Bila {@link #getKota()} menghasilkan kota yang punya propinsi, propinsi kota itu MENIMPA
	 * nilai di sini — konsistensi kota&ndash;propinsi diutamakan.</li>
	 * <li>Bila propinsi masih kosong tetapi kecamatan punya induk, hierarki ditelusuri dua tingkat ke
	 * atas (kecamatan &rarr; kabupaten &rarr; propinsi) dan namanya diterjemahkan lewat
	 * {@link #findOrCreatePropinsi(Session, String)} — yang dapat MENYIMPAN baris propinsi baru.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping berat.</b> Method ini membuka sesi Hibernate native sendiri
	 * ({@code HibernateUtil.currentNativeSession()}) dan pada blok {@code finally} memanggil
	 * {@code session.disconnect()}, {@code session.close()}, dan {@code HibernateUtil.closeSession()}.
	 * Bila getter terpanggil di tengah unit of work milik pemanggil (termasuk saat Hibernate melakukan
	 * {@code dirty check} sebelum flush), sesi itu ikut tertutup. Kegagalan di jalur utama dilaporkan
	 * lewat {@code Common.tampilErrorJikaAdmin}; kegagalan penutupan sesi ditelan dan dicatat
	 * {@code ErrorAuditUtil}. Perhatikan pula bahwa {@code session} bisa masih {@code null} saat blok
	 * {@code finally} berjalan (bila pembukaan sesi gagal) — {@code NullPointerException} yang
	 * timbul ikut tertelan di sana.</p>
	 *
	 * @return propinsi tempat tinggal, atau {@code null} bila tidak dapat ditentukan
	 */
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

	/**
	 * Menyetel propinsi tempat tinggal.
	 *
	 * <p>Nilai dapat ditimpa kembali oleh {@link #getPropinsi()} bila kota yang tersimpan menunjuk
	 * propinsi lain.</p>
	 *
	 * @param propinsi acuan {@link Propinsi}
	 */
	public void setPropinsi(Propinsi propinsi) {
		this.propinsi = propinsi;
	}

	/**
	 * Kota/kabupaten tempat tinggal (kolom {@code kota}), dengan penurunan otomatis dari pohon
	 * {@link Wilayah} memakai pencocokan nama.
	 *
	 * <p>Bila kolom masih kosong sementara {@link #getKecamatan()} menghasilkan simpul yang punya
	 * induk, method menelusuri kecamatan &rarr; kabupaten &rarr; propinsi lalu:</p>
	 * <ol>
	 * <li>menerjemahkan nama propinsi lewat {@link #findOrCreatePropinsi(Session, String)} — dan
	 * menyimpan hasilnya juga ke field {@code propinsi} (efek samping lintas properti yang disengaja,
	 * lihat komentar di badan method);</li>
	 * <li>mengambil seluruh {@link Kota} milik propinsi tersebut;</li>
	 * <li>membandingkan nama kabupaten versi Feeder dengan nama tiap kota memakai jarak Levenshtein
	 * setelah kata {@code "Kab."} dan {@code "Kota"} dibuang dari kedua sisi;</li>
	 * <li>memakai kandidat terdekat hanya bila jaraknya &lt; 2.</li>
	 * </ol>
	 *
	 * <p>Berbeda dengan propinsi, kota TIDAK pernah dibuatkan baris baru — bila tidak ada yang cukup
	 * mirip, nilainya tetap {@code null}.</p>
	 *
	 * <p><b>Efek samping berat:</b> sama seperti {@link #getPropinsi()} — membuka sesi Hibernate
	 * native sendiri dan menutupnya (beserta {@code HibernateUtil.closeSession()}) pada blok
	 * {@code finally}, serta berpotensi menyimpan baris {@link Propinsi} baru lewat helper.</p>
	 *
	 * @return kota/kabupaten tempat tinggal, atau {@code null} bila tidak dapat ditentukan
	 */
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

	/**
	 * Menyetel kota/kabupaten tempat tinggal.
	 *
	 * @param kota acuan {@link Kota}
	 */
	public void setKota(Kota kota) {
		this.kota = kota;
	}

	/**
	 * Nomor identitas kependudukan (NIK/KTP) mahasiswa, sudah dinormalisasi dan disinkronkan.
	 *
	 * <p>Karakter selain digit dan titik dibuang bila isinya bukan angka murni, lalu tanda hubung
	 * dihilangkan. Bila hasilnya kosong, method mencoba mengambil NIK dari formulir pendaftaran
	 * {@link BiodataCalonMahasiswa} melalui {@code mahasiswa.getBiodataCalonMahasiswa()} dan
	 * {@code ConstantValues.ambil(...)}.</p>
	 *
	 * <p><b>Kuirk:</b> syarat pengambilan dari PMB di dalam blok tersebut juga menerima nilai yang
	 * mengandung {@code "0000"}, padahal blok itu hanya bisa tercapai ketika nilai sudah kosong —
	 * sisa penyederhanaan kondisi versi lama yang kini tidak berpengaruh apa-apa. Kegagalan pembacaan
	 * data PMB ditelan dan dicatat lewat {@code ErrorAuditUtil}.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code noIdentitas} dan {@code mahasiswa} ditulis ulang.</p>
	 *
	 * @return NIK hasil normalisasi yang sudah di-{@code trim}; string kosong bila tidak ada, tidak
	 *         pernah {@code null}
	 */
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

	/**
	 * Menyetel nomor identitas kependudukan apa adanya (normalisasi terjadi saat dibaca).
	 *
	 * @param noIdentitas NIK/nomor KTP
	 */
	public void setNoIdentitas(String noIdentitas) {
		this.noIdentitas = noIdentitas;
	}

	/**
	 * Nama dusun/dukuh pada alamat tempat tinggal.
	 *
	 * <p><b>Efek samping ringan:</b> nilai {@code null} diganti string kosong dan ditulis ke
	 * field.</p>
	 *
	 * @return nama dusun; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	public String getDusun() {
		if (dusun == null) {
			dusun = "";
		}
		return dusun;
	}

	/**
	 * Menyetel nama dusun/dukuh.
	 *
	 * @param dusun nama dusun
	 */
	public void setDusun(String dusun) {
		this.dusun = dusun;
	}

	/**
	 * Rentang pendapatan ayah menurut daftar acuan {@link PendapatanOrangTua} (kolom
	 * {@code pendapatan_ortu}).
	 *
	 * <p>Relasi inilah sumber nilai turunan {@link #getPenghasilanAyah()} (teks siap tampil) dan
	 * {@link #getPenghasilanOrangTua()} (angka batas atas rentang). Nilainya dilewatkan
	 * {@code check(...)} milik {@link GeneralValueObject}.</p>
	 *
	 * @return rentang pendapatan ayah, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendapatan_ortu", nullable = true)
	public PendapatanOrangTua getPendapatanOrtu() {
		pendapatanOrtu = check(pendapatanOrtu);
		return pendapatanOrtu;
	}

	/**
	 * Menyetel rentang pendapatan ayah.
	 *
	 * @param pendapatanOrtu acuan {@link PendapatanOrangTua}
	 */
	public void setPendapatanOrtu(PendapatanOrangTua pendapatanOrtu) {
		this.pendapatanOrtu = pendapatanOrtu;
	}

	/**
	 * Rentang pendapatan ibu menurut daftar acuan {@link PendapatanOrangTua} (kolom
	 * {@code pendapatan_ortu_ibu}); sumber nilai turunan {@link #getPenghasilanIbu()}.
	 *
	 * @return rentang pendapatan ibu, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendapatan_ortu_ibu", nullable = true)
	public PendapatanOrangTua getPendapatanOrtuIbu() {
		pendapatanOrtuIbu = check(pendapatanOrtuIbu);
		return pendapatanOrtuIbu;
	}

	/**
	 * Menyetel rentang pendapatan ibu.
	 *
	 * @param pendapatanOrtuIbu acuan {@link PendapatanOrangTua}
	 */
	public void setPendapatanOrtuIbu(PendapatanOrangTua pendapatanOrtuIbu) {
		this.pendapatanOrtuIbu = pendapatanOrtuIbu;
	}

	/**
	 * Jenis sekolah asal mahasiswa baru menurut daftar acuan {@link JenisSekolahMahasiswaBaru}
	 * (kolom {@code jenis_sekolah}), mis. SMA negeri/swasta, MA, atau SMK.
	 *
	 * @return jenis sekolah asal, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_sekolah", nullable = true)
	public JenisSekolahMahasiswaBaru getJenisSekolah() {
		jenisSekolah = check(jenisSekolah);
		return jenisSekolah;
	}

	/**
	 * Menyetel jenis sekolah asal.
	 *
	 * @param jenisSekolah acuan {@link JenisSekolahMahasiswaBaru}
	 */
	public void setJenisSekolah(JenisSekolahMahasiswaBaru jenisSekolah) {
		this.jenisSekolah = jenisSekolah;
	}

	/**
	 * Tingkat pendidikan ayah menurut daftar acuan {@link PendidikanOrangTua} (kolom
	 * {@code pendidikan_ayah}).
	 *
	 * <p>Hidup berdampingan dengan {@link #getJenjangPendidikanAyah()} yang memakai daftar acuan
	 * {@link Jenjang} — dua kolom untuk konsep serupa, masing-masing melayani format pelaporan yang
	 * berbeda.</p>
	 *
	 * @return tingkat pendidikan ayah, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_ayah", nullable = true)
	public PendidikanOrangTua getPendidikanAyah() {
		pendidikanAyah = check(pendidikanAyah);
		return pendidikanAyah;
	}

	/**
	 * Menyetel tingkat pendidikan ayah.
	 *
	 * @param pendidikanAyah acuan {@link PendidikanOrangTua}
	 */
	public void setPendidikanAyah(PendidikanOrangTua pendidikanAyah) {
		this.pendidikanAyah = pendidikanAyah;
	}

	/**
	 * Tingkat pendidikan ibu menurut daftar acuan {@link PendidikanOrangTua} (kolom
	 * {@code pendidikan_ibu}).
	 *
	 * @return tingkat pendidikan ibu, atau {@code null} bila belum dipilih
	 * @see #getJenjangPendidikanIbu()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_ibu", nullable = true)
	public PendidikanOrangTua getPendidikanIbu() {
		pendidikanIbu = check(pendidikanIbu);
		return pendidikanIbu;
	}

	/**
	 * Menyetel tingkat pendidikan ibu.
	 *
	 * @param pendidikanIbu acuan {@link PendidikanOrangTua}
	 */
	public void setPendidikanIbu(PendidikanOrangTua pendidikanIbu) {
		this.pendidikanIbu = pendidikanIbu;
	}

	/**
	 * Tanggal lahir ayah, disimpan sebagai {@code DATE} (tanpa komponen jam).
	 *
	 * @return tanggal lahir ayah, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalLahirAyah() {
		return tanggalLahirAyah;
	}

	/**
	 * Menyetel tanggal lahir ayah.
	 *
	 * @param tanggalLahirAyah tanggal lahir
	 */
	public void setTanggalLahirAyah(Date tanggalLahirAyah) {
		this.tanggalLahirAyah = tanggalLahirAyah;
	}

	/**
	 * Nama wali mahasiswa — pihak yang bertanggung jawab bila mahasiswa tidak tinggal bersama orang
	 * tua kandung, atau bila orang tua sudah tiada.
	 *
	 * @return nama wali, atau {@code null} bila tidak ada
	 */
	public String getNamaWali() {
		return namaWali;
	}

	/**
	 * Menyetel nama wali.
	 *
	 * @param namaWali nama wali
	 */
	public void setNamaWali(String namaWali) {
		this.namaWali = namaWali;
	}

	/**
	 * Tanggal lahir wali, disimpan sebagai {@code DATE}.
	 *
	 * @return tanggal lahir wali, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalLahirWali() {
		return tanggalLahirWali;
	}

	/**
	 * Menyetel tanggal lahir wali.
	 *
	 * @param tanggalLahirWali tanggal lahir
	 */
	public void setTanggalLahirWali(Date tanggalLahirWali) {
		this.tanggalLahirWali = tanggalLahirWali;
	}

	/**
	 * Kategori pekerjaan wali menurut daftar acuan {@link PekerjaanOrangTua} (kolom
	 * {@code pekerjaan_wali}).
	 *
	 * @return kategori pekerjaan wali, atau {@code null} bila belum dipilih
	 * @see #getJenisPekerjaanWali()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pekerjaan_wali", nullable = true)
	public PekerjaanOrangTua getPekerjaanWali() {
		pekerjaanWali = check(pekerjaanWali);
		return pekerjaanWali;
	}

	/**
	 * Menyetel kategori pekerjaan wali.
	 *
	 * @param pekerjaanWali acuan {@link PekerjaanOrangTua}
	 */
	public void setPekerjaanWali(PekerjaanOrangTua pekerjaanWali) {
		this.pekerjaanWali = pekerjaanWali;
	}

	/**
	 * Tingkat pendidikan wali menurut daftar acuan {@link PendidikanOrangTua} (kolom
	 * {@code pendidikan_wali}).
	 *
	 * @return tingkat pendidikan wali, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_wali", nullable = true)
	public PendidikanOrangTua getPendidikanWali() {
		pendidikanWali = check(pendidikanWali);
		return pendidikanWali;
	}

	/**
	 * Menyetel tingkat pendidikan wali.
	 *
	 * @param pendidikanWali acuan {@link PendidikanOrangTua}
	 */
	public void setPendidikanWali(PendidikanOrangTua pendidikanWali) {
		this.pendidikanWali = pendidikanWali;
	}

	/**
	 * Rentang pendapatan wali menurut daftar acuan {@link PendapatanOrangTua} (kolom
	 * {@code pendapatan_wali}).
	 *
	 * <p>Berbeda dengan pendapatan ayah dan ibu, rentang wali TIDAK punya properti teks turunan
	 * sepadan {@link #getPenghasilanAyah()}/{@link #getPenghasilanIbu()} — asimetri yang perlu
	 * diingat saat menyusun laporan.</p>
	 *
	 * @return rentang pendapatan wali, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendapatan_wali", nullable = true)
	public PendapatanOrangTua getPendapatanWali() {
		pendapatanWali = check(pendapatanWali);
		return pendapatanWali;
	}

	/**
	 * Menyetel rentang pendapatan wali.
	 *
	 * @param pendapatanWali acuan {@link PendapatanOrangTua}
	 */
	public void setPendapatanWali(PendapatanOrangTua pendapatanWali) {
		this.pendapatanWali = pendapatanWali;
	}

	/**
	 * Tanggal lahir ibu, disimpan sebagai {@code DATE}.
	 *
	 * @return tanggal lahir ibu, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalLahirIbu() {
		return tanggalLahirIbu;
	}

	/**
	 * Menyetel tanggal lahir ibu.
	 *
	 * @param tanggalLahirIbu tanggal lahir
	 */
	public void setTanggalLahirIbu(Date tanggalLahirIbu) {
		this.tanggalLahirIbu = tanggalLahirIbu;
	}

	/**
	 * Alamat surel mahasiswa, dengan fallback ke surel yang tercatat pada {@link Mahasiswa}.
	 *
	 * <p>Bila kolom di sini kosong, nilai diambil dari {@link Mahasiswa#ambilEmail()} (yang punya
	 * rantai fallback sendiri) dan ditulis ke field. Hasil akhirnya di-{@code trim}.</p>
	 *
	 * <p>Dipakai antara lain oleh fitur broadcast/pengiriman surel massal dan pencetakan formulir.
	 * Perhatikan bahwa tidak ada validasi format apa pun di sini — nilai bisa saja bukan alamat surel
	 * yang sah.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code email} dan {@code mahasiswa} ditulis ulang.</p>
	 *
	 * @return alamat surel yang sudah di-{@code trim}; string kosong bila tidak ada di kedua sumber,
	 *         tidak pernah {@code null}
	 */
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

	/**
	 * Menyetel alamat surel mahasiswa.
	 *
	 * @param email alamat surel (tidak divalidasi)
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Jenis tempat tinggal mahasiswa menurut daftar acuan {@link JenisTinggalMahasiswa} (kolom
	 * {@code jenis_tinggal_mahasiswa}), mis. bersama orang tua, kos, atau asrama. Termasuk data wajib
	 * pelaporan PDDikti.
	 *
	 * @return jenis tempat tinggal, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_tinggal_mahasiswa", nullable = true)
	public JenisTinggalMahasiswa getJenisTinggalMahasiswa() {
		jenisTinggalMahasiswa = check(jenisTinggalMahasiswa);
		return jenisTinggalMahasiswa;
	}

	/**
	 * Menyetel jenis tempat tinggal mahasiswa.
	 *
	 * @param jenisTinggalMahasiswa acuan {@link JenisTinggalMahasiswa}
	 */
	public void setJenisTinggalMahasiswa(JenisTinggalMahasiswa jenisTinggalMahasiswa) {
		this.jenisTinggalMahasiswa = jenisTinggalMahasiswa;
	}

	/**
	 * Alat transportasi yang biasa dipakai mahasiswa menurut daftar acuan
	 * {@link AlatTransportasiMahasiswa} (kolom {@code alat_transportasi_mahasiswa}). Juga data wajib
	 * pelaporan PDDikti.
	 *
	 * <p>Bedakan dari {@link #getKendaraanKuliah()} yang berupa teks bebas.</p>
	 *
	 * @return alat transportasi, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alat_transportasi_mahasiswa", nullable = true)
	public AlatTransportasiMahasiswa getAlatTransportasiMahasiswa() {
		alatTransportasiMahasiswa = check(alatTransportasiMahasiswa);
		return alatTransportasiMahasiswa;
	}

	/**
	 * Menyetel alat transportasi mahasiswa.
	 *
	 * @param alatTransportasiMahasiswa acuan {@link AlatTransportasiMahasiswa}
	 */
	public void setAlatTransportasiMahasiswa(AlatTransportasiMahasiswa alatTransportasiMahasiswa) {
		this.alatTransportasiMahasiswa = alatTransportasiMahasiswa;
	}

	/**
	 * Jenjang pendidikan ayah menurut daftar acuan {@link Jenjang} (kolom
	 * {@code jenjang_pendidikan_ayah}) — jenjang yang sama dipakai untuk program studi (D3, S1, S2,
	 * dan seterusnya).
	 *
	 * <p>Pasangan "kembar" dari {@link #getPendidikanAyah()} yang memakai acuan
	 * {@link PendidikanOrangTua}. Keduanya diisi dari layar yang berbeda dan bisa saja tidak
	 * konsisten satu sama lain — tidak ada mekanisme penyelaras di kelas ini.</p>
	 *
	 * @return jenjang pendidikan ayah, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_pendidikan_ayah", nullable = true)
	public Jenjang getJenjangPendidikanAyah() {
		jenjangPendidikanAyah = check(jenjangPendidikanAyah);
		return jenjangPendidikanAyah;
	}

	/**
	 * Menyetel jenjang pendidikan ayah.
	 *
	 * @param jenjangPendidikanAyah acuan {@link Jenjang}
	 */
	public void setJenjangPendidikanAyah(Jenjang jenjangPendidikanAyah) {
		this.jenjangPendidikanAyah = jenjangPendidikanAyah;
	}

	/**
	 * Jenjang pendidikan ibu menurut daftar acuan {@link Jenjang} (kolom
	 * {@code jenjang_pendidikan_ibu}).
	 *
	 * @return jenjang pendidikan ibu, atau {@code null} bila belum dipilih
	 * @see #getJenjangPendidikanAyah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_pendidikan_ibu", nullable = true)
	public Jenjang getJenjangPendidikanIbu() {
		jenjangPendidikanIbu = check(jenjangPendidikanIbu);
		return jenjangPendidikanIbu;
	}

	/**
	 * Menyetel jenjang pendidikan ibu.
	 *
	 * @param jenjangPendidikanIbu acuan {@link Jenjang}
	 */
	public void setJenjangPendidikanIbu(Jenjang jenjangPendidikanIbu) {
		this.jenjangPendidikanIbu = jenjangPendidikanIbu;
	}

	/**
	 * Jenis pekerjaan ayah menurut daftar acuan {@link Pekerjaan} (kolom
	 * {@code jenis_pekerjaan_ayah}) — daftar pekerjaan umum yang juga dipakai modul lain.
	 *
	 * <p>Pasangan "kembar" dari {@link #getPekerjaanAyah()} yang memakai acuan
	 * {@link PekerjaanOrangTua}. Sama seperti pasangan pendidikan, keduanya tidak saling
	 * diselaraskan.</p>
	 *
	 * @return jenis pekerjaan ayah, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_ayah", nullable = true)
	public Pekerjaan getJenisPekerjaanAyah() {
		jenisPekerjaanAyah = check(jenisPekerjaanAyah);
		return jenisPekerjaanAyah;
	}

	/**
	 * Menyetel jenis pekerjaan ayah.
	 *
	 * @param jenisPekerjaanAyah acuan {@link Pekerjaan}
	 */
	public void setJenisPekerjaanAyah(Pekerjaan jenisPekerjaanAyah) {
		this.jenisPekerjaanAyah = jenisPekerjaanAyah;
	}

	/**
	 * Jenis pekerjaan ibu menurut daftar acuan {@link Pekerjaan} (kolom
	 * {@code jenis_pekerjaan_ibu}).
	 *
	 * @return jenis pekerjaan ibu, atau {@code null} bila belum dipilih
	 * @see #getJenisPekerjaanAyah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_ibu", nullable = true)
	public Pekerjaan getJenisPekerjaanIbu() {
		jenisPekerjaanIbu = check(jenisPekerjaanIbu);
		return jenisPekerjaanIbu;
	}

	/**
	 * Menyetel jenis pekerjaan ibu.
	 *
	 * @param jenisPekerjaanIbu acuan {@link Pekerjaan}
	 */
	public void setJenisPekerjaanIbu(Pekerjaan jenisPekerjaanIbu) {
		this.jenisPekerjaanIbu = jenisPekerjaanIbu;
	}

	/**
	 * Kategori penghasilan ayah menurut daftar acuan {@link Penghasilan} (kolom
	 * {@code jenis_penghasilan_ayah}).
	 *
	 * <p>Kolom ketiga yang menyentuh soal penghasilan ayah, di samping relasi
	 * {@link #getPendapatanOrtu()} (rentang nominal) dan kolom teks
	 * {@link #getPenghasilanAyah()}. Tidak ada penyelarasan otomatis di antara ketiganya.</p>
	 *
	 * @return kategori penghasilan ayah, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penghasilan_ayah", nullable = true)
	public Penghasilan getJenisPenghasilanAyah() {
		jenisPenghasilanAyah = check(jenisPenghasilanAyah);
		return jenisPenghasilanAyah;
	}

	/**
	 * Menyetel kategori penghasilan ayah.
	 *
	 * @param jenisPenghasilanAyah acuan {@link Penghasilan}
	 */
	public void setJenisPenghasilanAyah(Penghasilan jenisPenghasilanAyah) {
		this.jenisPenghasilanAyah = jenisPenghasilanAyah;
	}

	/**
	 * Kategori penghasilan ibu menurut daftar acuan {@link Penghasilan} (kolom
	 * {@code jenis_penghasilan_ibu}).
	 *
	 * @return kategori penghasilan ibu, atau {@code null} bila belum dipilih
	 * @see #getJenisPenghasilanAyah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penghasilan_ibu", nullable = true)
	public Penghasilan getJenisPenghasilanIbu() {
		jenisPenghasilanIbu = check(jenisPenghasilanIbu);
		return jenisPenghasilanIbu;
	}

	/**
	 * Menyetel kategori penghasilan ibu.
	 *
	 * @param jenisPenghasilanIbu acuan {@link Penghasilan}
	 */
	public void setJenisPenghasilanIbu(Penghasilan jenisPenghasilanIbu) {
		this.jenisPenghasilanIbu = jenisPenghasilanIbu;
	}

	/**
	 * Kategori penghasilan wali menurut daftar acuan {@link Penghasilan} (kolom
	 * {@code jenis_penghasilan_wali}).
	 *
	 * @return kategori penghasilan wali, atau {@code null} bila belum dipilih
	 * @see #getJenisPenghasilanAyah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_penghasilan_wali", nullable = true)
	public Penghasilan getJenisPenghasilanWali() {
		jenisPenghasilanWali = check(jenisPenghasilanWali);
		return jenisPenghasilanWali;
	}

	/**
	 * Menyetel kategori penghasilan wali.
	 *
	 * @param jenisPenghasilanWali acuan {@link Penghasilan}
	 */
	public void setJenisPenghasilanWali(Penghasilan jenisPenghasilanWali) {
		this.jenisPenghasilanWali = jenisPenghasilanWali;
	}

	/**
	 * Jenjang pendidikan wali menurut daftar acuan {@link Jenjang} (kolom
	 * {@code jenjang_pendidikan_wali}).
	 *
	 * @return jenjang pendidikan wali, atau {@code null} bila belum dipilih
	 * @see #getJenjangPendidikanAyah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang_pendidikan_wali", nullable = true)
	public Jenjang getJenjangPendidikanWali() {
		jenjangPendidikanWali = check(jenjangPendidikanWali);
		return jenjangPendidikanWali;
	}

	/**
	 * Menyetel jenjang pendidikan wali.
	 *
	 * @param jenjangPendidikanWali acuan {@link Jenjang}
	 */
	public void setJenjangPendidikanWali(Jenjang jenjangPendidikanWali) {
		this.jenjangPendidikanWali = jenjangPendidikanWali;
	}

	/**
	 * Jenis pekerjaan wali menurut daftar acuan {@link Pekerjaan} (kolom
	 * {@code jenis_pekerjaan_wali}).
	 *
	 * @return jenis pekerjaan wali, atau {@code null} bila belum dipilih
	 * @see #getJenisPekerjaanAyah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pekerjaan_wali", nullable = true)
	public Pekerjaan getJenisPekerjaanWali() {
		jenisPekerjaanWali = check(jenisPekerjaanWali);
		return jenisPekerjaanWali;
	}

	/**
	 * Menyetel jenis pekerjaan wali.
	 *
	 * @param jenisPekerjaanWali acuan {@link Pekerjaan}
	 */
	public void setJenisPekerjaanWali(Pekerjaan jenisPekerjaanWali) {
		this.jenisPekerjaanWali = jenisPekerjaanWali;
	}

	/**
	 * Penanda apakah mahasiswa pernah menempuh PAUD (Pendidikan Anak Usia Dini) — salah satu butir
	 * data riwayat pendidikan yang diminta PDDikti.
	 *
	 * <p><b>Efek samping ringan:</b> nilai {@code null} diganti {@code false} dan ditulis ke field,
	 * sehingga "belum diisi" tidak dapat dibedakan dari "tidak pernah".</p>
	 *
	 * @return {@code true} bila pernah PAUD, {@code false} bila tidak/belum diisi; tidak pernah
	 *         {@code null}
	 */
	public Boolean getApakahPernahPaud() {
		if (apakahPernahPaud == null) {
			apakahPernahPaud = false;
		}
		return apakahPernahPaud;
	}

	/**
	 * Menyetel penanda pernah menempuh PAUD.
	 *
	 * @param apakahPernahPaud {@code true} bila pernah
	 */
	public void setApakahPernahPaud(Boolean apakahPernahPaud) {
		this.apakahPernahPaud = apakahPernahPaud;
	}

	/**
	 * Penanda apakah mahasiswa pernah menempuh TK (Taman Kanak-kanak), pasangan
	 * {@link #getApakahPernahPaud()} dan diperlakukan sama: {@code null} menjadi {@code false}.
	 *
	 * @return {@code true} bila pernah TK, {@code false} bila tidak/belum diisi; tidak pernah
	 *         {@code null}
	 */
	public Boolean getApakahPernahTk() {
		if (apakahPernahTk == null) {
			apakahPernahTk = false;
		}
		return apakahPernahTk;
	}

	/**
	 * Menyetel penanda pernah menempuh TK.
	 *
	 * @param apakahPernahTk {@code true} bila pernah
	 */
	public void setApakahPernahTk(Boolean apakahPernahTk) {
		this.apakahPernahTk = apakahPernahTk;
	}

	/**
	 * Isi kolom {@code text} "parameter tambahan" versi BERLABEL — data isian tambahan buatan
	 * masing-masing perguruan tinggi, diserialkan menjadi satu string.
	 *
	 * <p>Format: satu baris per isian (pemisah {@code "\n"}), tiap baris berisi delapan ruas yang
	 * dipisah literal <code>&lt;=&gt;</code>:
	 * {@code namaKelompok->labelInputan <=> nilai <=> urlLampiran <=> nomorUrut <=> idParameter <=>
	 * idKelompok <=> indexKe <=> keterangan}. Versi ini dipakai untuk menampilkan dan mencetak
	 * (labelnya sudah manusiawi); untuk mengisi ulang formulir dipakai
	 * {@link #getParameterTambahanInds()}.</p>
	 *
	 * <p><b>Efek samping ringan:</b> nilai {@code null} diganti string kosong dan ditulis ke field.
	 * Blok kondisi yang sebagian dikomentari di badan method adalah sisa rencana migrasi ke format
	 * JSON yang tidak jadi dipakai.</p>
	 *
	 * @return string terenkode; string kosong bila belum ada isian, tidak pernah {@code null}
	 * @see #populateParameterTambahan(List)
	 * @see #ambilDataParameterTambahan()
	 */
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

	/**
	 * Menyetel isi kolom parameter tambahan versi berlabel secara mentah.
	 *
	 * <p>Umumnya tidak dipanggil langsung — pakai {@link #populateParameterTambahan(List)} yang
	 * menyusun format terenkodenya dengan benar.</p>
	 *
	 * @param parameterTambahan string terenkode
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Membongkar string {@link #getParameterTambahan()} menjadi daftar {@link CommonVO} siap tampil.
	 *
	 * <p>Kebalikan dari {@link #populateParameterTambahan(List)}. Tiap baris dipecah pada
	 * <code>&lt;=&gt;</code> dan dipetakan ke {@link CommonVO}:</p>
	 * <ul>
	 * <li>ruas ke-0 &rarr; {@code name} (label gabungan {@code namaKelompok->labelInputan}),</li>
	 * <li>ruas ke-1 &rarr; {@code name1} (nilai isian),</li>
	 * <li>ruas ke-2 &rarr; {@code name2} (URL lampiran),</li>
	 * <li>ruas ke-3 &rarr; {@code nomorUrut} (default 1 bila gagal di-parse),</li>
	 * <li>ruas ke-4 &rarr; {@code id} (ID {@link ParameterTambahan}, default 1 bila gagal).</li>
	 * </ul>
	 * <p>Hasilnya diurutkan memakai {@link Collections#sort(List)} yang bersandar pada
	 * {@code CommonVO.compareTo} — urut menurut {@code nomorUrut} lalu {@code id}.</p>
	 *
	 * <p><b>Asimetri yang perlu diketahui:</b> {@link #populateParameterTambahan(List)} menulis
	 * DELAPAN ruas, tetapi method ini hanya membaca lima yang pertama — {@code idKelompok},
	 * {@code indexKe}, dan {@code keterangan} diabaikan. Kembarannya untuk alumni,
	 * {@link #ambilDataParameterTambahanAlumni()}, membaca sampai ruas ke-6. Kegagalan
	 * {@code parseInt}/{@code parseLong} ditelan dan nilai default dipakai.</p>
	 *
	 * <p><b>Kuirk:</b> pada string kosong, {@code split} tetap menghasilkan satu elemen kosong,
	 * sehingga method mengembalikan satu {@link CommonVO} "hampa" (label dan nilai kosong,
	 * {@code id} = 1) — bukan daftar kosong. Pemanggil yang menampilkan hasilnya langsung perlu
	 * menyaring baris tanpa label.</p>
	 *
	 * <p>Dipakai luas oleh layar dan laporan, antara lain {@code MahasiswaAction},
	 * {@code CommonReportHelper}, {@code CetakRegistrasiAction}, dan berbagai dasbor rekap parameter
	 * tambahan.</p>
	 *
	 * @return daftar {@link CommonVO} terurut; tidak pernah {@code null}
	 */
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

	/**
	 * Memanen nilai seluruh baris formulir parameter tambahan di layar ZK, lalu menyimpannya ke dua
	 * kolom {@code text}: {@link #setParameterTambahan(String)} (versi berlabel) dan
	 * {@link #setParameterTambahanInds(String)} (versi ber-ID).
	 *
	 * <p>Dipanggil dari {@code ais.action.master.helper.ParameterTambahanMahasiswaListener} — baik
	 * dari {@code onSave(BiodataMahasiswa)} maupun dari {@code EventListener} internalnya — dengan
	 * daftar {@link Row} yang dibangun listener tersebut. Tiap baris membawa atribut:
	 * {@code "parameterTambahan"} ({@link ParameterTambahan}),
	 * {@code "kelompokParameterTambahanMahasiswa"} ({@link KelompokParameterTambahanMahasiswa}),
	 * {@code "indexKe"} ({@link Long}), dan {@code "keterangan"} (sebuah {@code Textbox}).</p>
	 *
	 * <p>Untuk tiap baris yang lengkap:</p>
	 * <ol>
	 * <li>nilai isian diambil lewat {@code ParameterTambahan.ambilVal(row, parameterTambahan)} yang
	 * tahu cara membaca komponen ZK sesuai tipe inputan;</li>
	 * <li>bila parameter mewajibkan lampiran, berkas dicari dengan
	 * {@code LampiranLain.ambil(getId(), idKelompok + "->" + idParameter)} dan URL unduhnya
	 * disertakan — perhatikan bahwa pemilik lampiran adalah ID BIODATA, bukan ID mahasiswa;</li>
	 * <li>baris versi berlabel (8 ruas) dan versi ber-ID (4 ruas) dirangkai dan digabung dengan
	 * pemisah {@code "\n"}.</li>
	 * </ol>
	 *
	 * <p><b>Perilaku MENIMPA.</b> Kedua kolom ditulis ulang seluruhnya, bukan ditambahkan. Berbeda
	 * dengan {@link #populateParameterTambahanAlumni(List)} yang bisa menyambung. Karena itu
	 * memanggil method ini dengan daftar baris yang tidak lengkap akan MENGHAPUS isian yang tidak
	 * ikut ditampilkan.</p>
	 *
	 * <p><b>Catatan lain:</b> masih ada {@code System.out.println("ket => " + ket)} yang tertinggal
	 * dari proses debug dan mencetak keterangan tiap baris ke log server. Kegagalan per baris ditelan
	 * lewat {@code Common.tampilErrorJikaAdmin} sehingga satu baris rusak tidak membatalkan
	 * sisanya.</p>
	 *
	 * @param parameterRows daftar baris formulir ZK; bila {@code null} atau kosong method langsung
	 *         kembali tanpa mengubah apa pun (isian lama tetap aman)
	 */
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

	/**
	 * Isi kolom {@code text} parameter tambahan versi BER-ID.
	 *
	 * <p>Format: satu baris per isian, empat ruas dipisah <code>&lt;=&gt;</code>:
	 * {@code idKelompok->idParameter <=> nilai <=> urlLampiran <=> keterangan}. Versi ini dipakai
	 * untuk MENGISI ULANG formulir (mencocokkan nilai ke komponen berdasarkan ID, bukan berdasarkan
	 * label yang bisa berubah), sedangkan versi berlabel dipakai untuk tampilan dan cetak.</p>
	 *
	 * <p><b>Efek samping ringan:</b> nilai {@code null} diganti string kosong dan ditulis ke
	 * field.</p>
	 *
	 * @return string terenkode; string kosong bila belum ada isian, tidak pernah {@code null}
	 * @see #getParameterTambahan()
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}
		return parameterTambahanInds;
	}

	/**
	 * Menyetel isi kolom parameter tambahan versi ber-ID secara mentah.
	 *
	 * <p>Umumnya diisi oleh {@link #populateParameterTambahan(List)}, bukan dipanggil langsung.</p>
	 *
	 * @param parameterTambahanInds string terenkode
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Menyetel NIRM (Nomor Induk Registrasi Mahasiswa) apa adanya; normalisasi terjadi saat dibaca.
	 *
	 * @param nirm NIRM
	 */
	public void setNirm(String nirm) {
		this.nirm = nirm;
	}

	/**
	 * NIRM (Nomor Induk Registrasi Mahasiswa) — nomor induk versi Kopertais/lembaga keagamaan yang
	 * berdampingan dengan NIM biasa — sudah dinormalisasi.
	 *
	 * <p>{@code null} diganti string kosong, karakter selain digit dan titik dibuang bila isinya bukan
	 * angka murni, lalu dipotong maksimal 20 karakter.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code nirm} ditimpa hasil normalisasi.</p>
	 *
	 * @return NIRM hasil normalisasi yang sudah di-{@code trim}; string kosong bila belum diisi
	 */
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

	/**
	 * NISN (Nomor Induk Siswa Nasional) dari jenjang sekolah sebelumnya, dengan fallback ke data
	 * pendaftaran dan normalisasi.
	 *
	 * <p>Bila kolom kosong dan mahasiswa punya rujukan {@link BiodataCalonMahasiswa}, NISN dari
	 * formulir PMB disalin. Setelah itu: {@code null} menjadi string kosong, karakter non angka
	 * dibuang, dan hasilnya dipotong maksimal 10 karakter (panjang baku NISN).</p>
	 *
	 * <p><b>Kuirk:</b> berbeda dengan {@link #getHp()} atau {@link #getTinggiBadan()}, blok fallback
	 * di sini memakai field {@code mahasiswa} langsung tanpa memanggil {@link #getMahasiswa()} lebih
	 * dulu. Pada instance yang baru dimuat Hibernate dan belum pernah menyentuh getter relasi,
	 * fallback ke data PMB diam-diam tidak berjalan. Kegagalan pembacaan PMB ditelan dan dicatat
	 * lewat {@code ErrorAuditUtil}.</p>
	 *
	 * @return NISN hasil normalisasi yang sudah di-{@code trim}; string kosong bila tidak ada
	 */
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

	/**
	 * Menyetel NISN apa adanya; normalisasi terjadi saat dibaca.
	 *
	 * @param nisn NISN
	 */
	public void setNisn(String nisn) {
		this.nisn = nisn;
	}

	/**
	 * NPWP (Nomor Pokok Wajib Pajak) mahasiswa, sudah dinormalisasi.
	 *
	 * <p>{@code null} diganti string kosong, karakter selain digit dan titik dibuang, lalu dipotong
	 * maksimal 15 karakter (panjang NPWP tanpa pemisah).</p>
	 *
	 * <p>Berbeda dengan kebanyakan getter penormal lain di kelas ini, hasil akhirnya TIDAK
	 * di-{@code trim} sebelum dikembalikan.</p>
	 *
	 * @return NPWP hasil normalisasi; string kosong bila belum diisi, tidak pernah {@code null}
	 */
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

	/**
	 * Menyetel NPWP apa adanya; normalisasi terjadi saat dibaca.
	 *
	 * @param npwp NPWP
	 */
	public void setNpwp(String npwp) {
		this.npwp = npwp;
	}

	/**
	 * NIK (Nomor Induk Kependudukan) ayah, sudah dinormalisasi.
	 *
	 * <p>Karakter selain digit dan titik dibuang bila isinya bukan angka murni, tanda hubung
	 * dihilangkan, lalu nilai sentinel lama {@code "00000"} dianggap "tidak ada" dan dikosongkan.
	 * Pemanggilan {@code StringUtils.replace} aman terhadap {@code null} sehingga urutan
	 * pemeriksaannya tidak menimbulkan {@code NullPointerException}.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code nikAyah} ditimpa hasil normalisasi. Berbeda dengan
	 * {@link #getNoIdentitas()}, tidak ada pemotongan panjang maupun fallback ke data PMB di sini.</p>
	 *
	 * @return NIK ayah hasil normalisasi; string kosong bila tidak ada
	 */
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

	/**
	 * Menyetel NIK ayah apa adanya; normalisasi terjadi saat dibaca.
	 *
	 * @param nikAyah NIK ayah
	 */
	public void setNikAyah(String nikAyah) {
		this.nikAyah = nikAyah;
	}

	/**
	 * NIK (Nomor Induk Kependudukan) ibu, dinormalisasi dengan aturan yang sama persis dengan
	 * {@link #getNikAyah()}.
	 *
	 * <p>Perlu dicatat bahwa NIK wali TIDAK punya kolom sendiri — hanya ayah dan ibu.</p>
	 *
	 * @return NIK ibu hasil normalisasi; string kosong bila tidak ada
	 */
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

	/**
	 * Menyetel NIK ibu apa adanya; normalisasi terjadi saat dibaca.
	 *
	 * @param nikIbu NIK ibu
	 */
	public void setNikIbu(String nikIbu) {
		this.nikIbu = nikIbu;
	}

	/**
	 * Isi kolom {@code text} parameter tambahan ALUMNI versi berlabel — jawaban kuesioner alumni
	 * (tracer study) yang formatnya sama persis dengan {@link #getParameterTambahan()}, hanya saja
	 * kelompoknya memakai {@link KelompokParameterTambahanAlumni}.
	 *
	 * <p><b>Efek samping ringan:</b> {@code null} diganti string kosong dan ditulis ke field.</p>
	 *
	 * @return string terenkode; string kosong bila belum ada isian, tidak pernah {@code null}
	 * @see #populateParameterTambahanAlumni(List)
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanAlumni() {
		if (parameterTambahanAlumni == null) {
			parameterTambahanAlumni = "";
		}
		return parameterTambahanAlumni;
	}

	/**
	 * Menyetel isi kolom parameter tambahan alumni versi berlabel secara mentah.
	 *
	 * @param parameterTambahanAlumni string terenkode
	 */
	public void setParameterTambahanAlumni(String parameterTambahanAlumni) {
		this.parameterTambahanAlumni = parameterTambahanAlumni;
	}

	/**
	 * Kembaran {@link #populateParameterTambahan(List)} untuk kuesioner ALUMNI: memanen nilai baris
	 * formulir ZK ke {@link #setParameterTambahanAlumni(String)} dan
	 * {@link #setParameterTambahanIndsAlumni(String)}.
	 *
	 * <p>Dipanggil dari {@code ais.action.master.helper.ParameterTambahanAlumniListener}. Perbedaan
	 * dengan versi mahasiswa:</p>
	 * <ul>
	 * <li>atribut kelompok pada {@link Row} bernama {@code "kelompokParameterTambahanAlumni"} dan
	 * bertipe {@link KelompokParameterTambahanAlumni};</li>
	 * <li><b>mode sambung.</b> Bila baris terakhir yang diproses punya atribut {@code "indexKe"} yang
	 * tidak {@code null}, hasil rangkaian DITAMBAHKAN di belakang isi kolom yang sudah ada, bukan
	 * menimpanya. Ini melayani kuesioner yang boleh diisi berulang (mis. satu blok pertanyaan per
	 * riwayat pekerjaan). Bila {@code null}, kolom ditimpa seperti biasa;</li>
	 * <li>tidak ada {@code System.out.println} sisa debug.</li>
	 * </ul>
	 *
	 * <p><b>Kuirk mode sambung.</b> Keputusan menyambung atau menimpa ditentukan oleh nilai
	 * {@code indexKe} baris TERAKHIR (variabel dideklarasikan di luar perulangan dan terus ditimpa),
	 * bukan oleh keseluruhan daftar. Bila daftar bercampur — sebagian baris ber-{@code indexKe} dan
	 * sebagian tidak — perilakunya bergantung urutan baris. Selain itu penyambungan dilakukan tanpa
	 * menyisipkan {@code "\n"} pemisah, sehingga baris terakhir isian lama dan baris pertama isian
	 * baru bisa menempel jadi satu baris.</p>
	 *
	 * @param parameterRows daftar baris formulir ZK; bila {@code null} atau kosong method langsung
	 *         kembali tanpa mengubah apa pun
	 */
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

	/**
	 * Membongkar string {@link #getParameterTambahanAlumni()} menjadi daftar {@link CommonVO} siap
	 * tampil — kembaran {@link #ambilDataParameterTambahan()} untuk kuesioner alumni.
	 *
	 * <p>Membaca lebih banyak ruas daripada versi mahasiswa: selain label (ruas 0), nilai (1), URL
	 * lampiran (2), {@code nomorUrut} (3), dan ID parameter (4), method ini juga mengambil ID
	 * kelompok (ruas 5) ke {@code name3} dan {@code indexKe} (ruas 6) ke {@code name4}. Dua ruas
	 * terakhir itulah yang memungkinkan pengelompokan ulang isian berulang di layar dan laporan
	 * tracer study. Semua {@code parse} gagal ditelan diam-diam dan diganti nilai default.</p>
	 *
	 * <p>Hasil diurutkan lewat {@code CommonVO.compareTo}. Seperti versi mahasiswa, string kosong
	 * tetap menghasilkan satu {@link CommonVO} "hampa".</p>
	 *
	 * <p>Dipakai oleh {@code MahasiswaAction} dan {@code CommonReportHelper}.</p>
	 *
	 * @return daftar {@link CommonVO} terurut; tidak pernah {@code null}
	 */
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

	/**
	 * Isi kolom {@code text} parameter tambahan ALUMNI versi ber-ID (empat ruas per baris), dipakai
	 * untuk mengisi ulang formulir kuesioner alumni.
	 *
	 * <p><b>Efek samping ringan:</b> {@code null} diganti string kosong dan ditulis ke field.</p>
	 *
	 * @return string terenkode; string kosong bila belum ada isian, tidak pernah {@code null}
	 * @see #getParameterTambahanInds()
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanIndsAlumni() {
		if (parameterTambahanIndsAlumni == null) {
			parameterTambahanIndsAlumni = "";
		}
		return parameterTambahanIndsAlumni;
	}

	/**
	 * Menyetel isi kolom parameter tambahan alumni versi ber-ID secara mentah.
	 *
	 * @param parameterTambahanIndsAlumni string terenkode
	 */
	public void setParameterTambahanIndsAlumni(String parameterTambahanIndsAlumni) {
		this.parameterTambahanIndsAlumni = parameterTambahanIndsAlumni;
	}

	/**
	 * NPSN (Nomor Pokok Sekolah Nasional) sekolah asal, diturunkan dari relasi bila ada.
	 *
	 * <p>Bila {@link #getNamaSekolahAsal()} terisi dan kodenya tidak kosong, kode dari daftar acuan
	 * {@link NamaSekolahAsal} MENIMPA nilai kolom — pola yang sama dengan {@link #getAsalSma()} untuk
	 * nama sekolahnya. Tidak ada normalisasi angka di sini.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code npsn} dan {@code namaSekolahAsal} ditulis ulang.</p>
	 *
	 * @return NPSN, atau {@code null} bila kolom kosong dan relasi tidak punya kode
	 */
	public String getNpsn() {
		namaSekolahAsal = getNamaSekolahAsal();
		if (namaSekolahAsal != null && namaSekolahAsal.getKode() != null
				&& !namaSekolahAsal.getKode().trim().isEmpty()) {
			npsn = namaSekolahAsal.getKode();
		}
		return npsn;
	}

	/**
	 * Menyetel NPSN sekolah asal.
	 *
	 * <p>Nilai dapat ditimpa saat dibaca bila {@link #getNamaSekolahAsal()} punya kode.</p>
	 *
	 * @param npsn NPSN
	 */
	public void setNpsn(String npsn) {
		this.npsn = npsn;
	}

	/**
	 * Sekolah asal menurut daftar acuan baku {@link NamaSekolahAsal} (kolom
	 * {@code nama_sekolah_asal}).
	 *
	 * <p>Relasi ini adalah sumber kebenaran bagi dua kolom teks turunan: {@link #getAsalSma()} (nama)
	 * dan {@link #getNpsn()} (kode NPSN). Kolom teks tetap dipertahankan untuk data lama yang
	 * diinput sebelum daftar sekolah baku tersedia.</p>
	 *
	 * @return sekolah asal, atau {@code null} bila belum dipilih dari daftar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nama_sekolah_asal", nullable = true)
	public NamaSekolahAsal getNamaSekolahAsal() {
		namaSekolahAsal = check(namaSekolahAsal);
		return namaSekolahAsal;
	}

	/**
	 * Menyetel sekolah asal dari daftar acuan.
	 *
	 * <p>Perhatikan bahwa menyetel relasi ini akan menimpa {@link #getAsalSma()} dan
	 * {@link #getNpsn()} pada pembacaan berikutnya.</p>
	 *
	 * @param namaSekolahAsal acuan {@link NamaSekolahAsal}
	 */
	public void setNamaSekolahAsal(NamaSekolahAsal namaSekolahAsal) {
		this.namaSekolahAsal = namaSekolahAsal;
	}

	/**
	 * Penanda apakah mahasiswa/alumni memiliki SKPI (Surat Keterangan Pendamping Ijazah).
	 *
	 * <p>{@code null} dibaca sebagai {@code false}. Berbeda dengan {@link #getApakahPernahPaud()},
	 * di sini field TIDAK ditulis ulang — hanya nilai kembaliannya yang disesuaikan, sehingga tidak
	 * ada efek samping penyimpanan.</p>
	 *
	 * @return {@code true} bila punya SKPI, {@code false} bila tidak/belum diisi; tidak pernah
	 *         {@code null}
	 */
	public Boolean getPunyaSkpi() {
		return punyaSkpi == null ? false : punyaSkpi;
	}

	/**
	 * Menyetel penanda kepemilikan SKPI.
	 *
	 * @param punyaSkpi {@code true} bila punya
	 */
	public void setPunyaSkpi(Boolean punyaSkpi) {
		this.punyaSkpi = punyaSkpi;
	}

	/**
	 * Penanda kepemilikan sertifikat kemampuan bahasa Inggris (mis. TOEFL/IELTS), sering menjadi
	 * syarat kelulusan atau pendaftaran wisuda.
	 *
	 * <p>{@code null} dibaca sebagai {@code false} tanpa menulis ke field.</p>
	 *
	 * @return {@code true} bila punya, {@code false} bila tidak/belum diisi; tidak pernah {@code null}
	 */
	public Boolean getPunyaSertifikatBahasaInggris() {
		return punyaSertifikatBahasaInggris == null ? false : punyaSertifikatBahasaInggris;
	}

	/**
	 * Menyetel penanda kepemilikan sertifikat bahasa Inggris.
	 *
	 * @param punyaSertifikatBahasaInggris {@code true} bila punya
	 */
	public void setPunyaSertifikatBahasaInggris(Boolean punyaSertifikatBahasaInggris) {
		this.punyaSertifikatBahasaInggris = punyaSertifikatBahasaInggris;
	}

	/**
	 * Penanda kepemilikan sertifikat kemampuan bahasa Arab — pasangan
	 * {@link #getPunyaSertifikatBahasaInggris()}, relevan pada perguruan tinggi keagamaan Islam.
	 *
	 * <p>{@code null} dibaca sebagai {@code false} tanpa menulis ke field.</p>
	 *
	 * @return {@code true} bila punya, {@code false} bila tidak/belum diisi; tidak pernah {@code null}
	 */
	public Boolean getPunyaSertifikatBahasaArab() {
		return punyaSertifikatBahasaArab == null ? false : punyaSertifikatBahasaArab;
	}

	/**
	 * Menyetel penanda kepemilikan sertifikat bahasa Arab.
	 *
	 * @param punyaSertifikatBahasaArab {@code true} bila punya
	 */
	public void setPunyaSertifikatBahasaArab(Boolean punyaSertifikatBahasaArab) {
		this.punyaSertifikatBahasaArab = punyaSertifikatBahasaArab;
	}

	/**
	 * Nomor telepon ayah.
	 *
	 * <p>Ketiga nomor telepon orang tua/wali ({@code telpAyah}, {@code telpIbu}, {@code telpWali})
	 * dideklarasikan pada satu baris field dan dikembalikan apa adanya — TIDAK dinormalisasi seperti
	 * {@link #getTeleponRumah()}. Nilainya bisa mengandung spasi, tanda hubung, awalan {@code "+62"},
	 * atau bahkan beberapa nomor sekaligus.</p>
	 *
	 * @return nomor telepon ayah, atau {@code null} bila belum diisi
	 */
	public String getTelpAyah() {
		return telpAyah;
	}

	/**
	 * Menyetel nomor telepon ayah.
	 *
	 * @param telpAyah nomor telepon (tidak dinormalisasi)
	 */
	public void setTelpAyah(String telpAyah) {
		this.telpAyah = telpAyah;
	}

	/**
	 * Nomor telepon ibu, dikembalikan apa adanya tanpa normalisasi.
	 *
	 * @return nomor telepon ibu, atau {@code null} bila belum diisi
	 * @see #getTelpAyah()
	 */
	public String getTelpIbu() {
		return telpIbu;
	}

	/**
	 * Menyetel nomor telepon ibu.
	 *
	 * @param telpIbu nomor telepon (tidak dinormalisasi)
	 */
	public void setTelpIbu(String telpIbu) {
		this.telpIbu = telpIbu;
	}

	/**
	 * Nomor telepon wali, dikembalikan apa adanya tanpa normalisasi.
	 *
	 * @return nomor telepon wali, atau {@code null} bila belum diisi
	 * @see #getTelpAyah()
	 */
	public String getTelpWali() {
		return telpWali;
	}

	/**
	 * Menyetel nomor telepon wali.
	 *
	 * @param telpWali nomor telepon (tidak dinormalisasi)
	 */
	public void setTelpWali(String telpWali) {
		this.telpWali = telpWali;
	}

	/**
	 * Nama operator seluler nomor {@link #getHp()}, dalam bentuk teks.
	 *
	 * <p>Bila relasi {@link #getOperatorSeluler()} terisi, namanya MENIMPA teks yang tersimpan.
	 * Bersama-sama keduanya membentuk pasangan dua arah yang tidak lazim: teks bebas ini dipakai
	 * {@link #getOperatorSeluler()} untuk MENEBAK relasi ketika relasi masih kosong, dan sebaliknya
	 * relasi dipakai di sini untuk menimpa teks. Yang mana yang "menang" bergantung pada mana yang
	 * lebih dulu terisi.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code hpProvider} dan {@code operatorSeluler} ditulis ulang.</p>
	 *
	 * @return nama operator seluler, atau {@code null} bila keduanya kosong
	 */
	public String getHpProvider() {
		operatorSeluler = getOperatorSeluler();
		if (operatorSeluler != null) {
			hpProvider = operatorSeluler.getNama();
		}
		return hpProvider;
	}

	/**
	 * Menyetel nama operator seluler sebagai teks bebas.
	 *
	 * @param hpProvider nama operator, mis. {@code "Telkomsel"}
	 */
	public void setHpProvider(String hpProvider) {
		this.hpProvider = hpProvider;
	}

	/**
	 * Operator seluler nomor HP mahasiswa menurut daftar acuan {@link OperatorSeluler} (kolom
	 * {@code operator_seluler}), dengan penebakan dari teks bebas.
	 *
	 * <p>Bila relasi masih kosong sementara {@link #getHpProvider()} berisi teks, seluruh daftar
	 * {@link OperatorSeluler} diambil dari cache {@code ConstantValues.ambilBerdasarClass(...)} dan
	 * dicocokkan dua arah tanpa memedulikan besar-kecil huruf: nama operator memuat teks, ATAU teks
	 * memuat nama operator. Kandidat pertama yang cocok langsung dipakai.</p>
	 *
	 * <p><b>Kelemahan pencocokan:</b> aturan "saling memuat" ini longgar — teks pendek atau nama
	 * operator yang merupakan bagian dari nama operator lain bisa menghasilkan kecocokan yang keliru,
	 * dan karena perulangan berhenti pada kandidat pertama, hasilnya bergantung urutan isi cache
	 * (tidak deterministik antar-restart). Selain itu {@code m.getNama()} diasumsikan tidak
	 * {@code null}; baris acuan tanpa nama akan melempar {@code NullPointerException} yang tidak
	 * ditangkap di sini.</p>
	 *
	 * <p><b>Efek samping:</b> field {@code operatorSeluler} ditulis ulang sehingga hasil tebakan bisa
	 * ikut tersimpan ke basis data pada flush berikutnya.</p>
	 *
	 * @return operator seluler, atau {@code null} bila relasi kosong dan tidak ada yang cocok
	 */
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

	/**
	 * Menyetel operator seluler dari daftar acuan.
	 *
	 * <p>Menyetel relasi ini membuat {@link #getHpProvider()} menimpa teks operator dengan nama dari
	 * acuan.</p>
	 *
	 * @param operatorSeluler acuan {@link OperatorSeluler}
	 */
	public void setOperatorSeluler(OperatorSeluler operatorSeluler) {
		this.operatorSeluler = operatorSeluler;
	}

	/**
	 * Menghitung total SKOR yang tersimpan pada jawaban parameter tambahan untuk satu butir
	 * {@link ParameterTambahan} tertentu.
	 *
	 * <p>Mekanisme ini memungkinkan sebuah pertanyaan isian tambahan berfungsi sebagai penentu
	 * NOMINAL: pilihan jawaban bertipe {@code PILIHAN_CUSTOM} boleh ditulis dalam bentuk
	 * {@code "label:angka"}, dan angka itulah skornya. Dipakai untuk menghitung biaya yang bergantung
	 * jawaban mahasiswa — lihat {@code Kegiatan} (biaya kegiatan/pendaftaran, lewat
	 * {@code mahasiswa.ambilBiodata().ambilSkor(...)}) dan {@code sekolah.NominalBiaya}.</p>
	 *
	 * <p>Alurnya: string {@link #getParameterTambahan()} dipecah per baris; tiap baris diambil ruas
	 * ke-1 (nilai) dan ruas ke-4 (ID parameter), lalu objek {@link ParameterTambahan} dimuat dari
	 * cache {@code ConstantValues}. Bila ID-nya sama dengan {@code parameterTambahanData} DAN tipe
	 * inputannya {@code PILIHAN_CUSTOM}, nilainya di-parse: bila memuat titik dua, bagian SESUDAH
	 * titik dua yang diambil; bila tidak, seluruh nilai diparse sebagai angka. Hasil tiap baris
	 * dijumlahkan.</p>
	 *
	 * <p><b>Catatan.</b> Semua kegagalan {@code parse} ditelan (dicatat {@code ErrorAuditUtil}) dan
	 * menghasilkan skor 0 untuk baris itu, sehingga jawaban yang formatnya keliru diam-diam bernilai
	 * nol alih-alih memunculkan kesalahan. Bila butir yang sama muncul lebih dari sekali (isian
	 * berulang), skornya BERTAMBAH — perilaku yang diinginkan untuk biaya per-item, tetapi bisa
	 * mengejutkan bila baris ganda muncul karena data kotor.</p>
	 *
	 * @param parameterTambahanData butir parameter tambahan yang skornya ingin dijumlahkan; bila
	 *         {@code null} hasilnya 0
	 * @return total skor, {@code 0} bila tidak ada jawaban yang cocok; tidak pernah {@code null}
	 */
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

	/**
	 * Alamat surel atasan mahasiswa di tempat kerja — dipakai pada program kelas karyawan dan
	 * pascasarjana, mis. untuk mengirim permintaan izin atau rekomendasi.
	 *
	 * @return alamat surel atasan yang sudah di-{@code trim}; string kosong bila belum diisi, tidak
	 *         pernah {@code null}
	 */
	public String getEmailAtasan() {
		return emailAtasan == null ? "" : emailAtasan.trim();
	}

	/**
	 * Menyetel alamat surel atasan di tempat kerja.
	 *
	 * @param emailAtasan alamat surel (tidak divalidasi)
	 */
	public void setEmailAtasan(String emailAtasan) {
		this.emailAtasan = emailAtasan;
	}

	/**
	 * Menaruh foto mahasiswa ke dalam peta parameter laporan.
	 *
	 * <p>Sekadar delegasi: {@code getMahasiswa().putPhoto(parameters)} — foto memang milik entity
	 * {@link Mahasiswa}, bukan biodata. Method ini ada agar pemanggil yang sudah memegang objek
	 * biodata tidak perlu menelusuri relasi sendiri; dipakai antara lain oleh
	 * {@code ais.action.report.CommonReportHelper} saat menyiapkan parameter cetak JasperReports.</p>
	 *
	 * <p><b>Efek samping:</b> {@code parameters} diubah di tempat (entri foto ditambahkan). Bila
	 * biodata belum tertaut mahasiswa, {@code NullPointerException} yang timbul DITELAN di sini
	 * (hanya dicetak ke {@code stderr} dan dicatat {@code ErrorAuditUtil}) sehingga laporan tetap
	 * tercetak tanpa foto, tanpa peringatan bagi pengguna.</p>
	 *
	 * @param parameters peta parameter laporan yang akan diisi; tipe mentah ({@code raw}) mengikuti
	 *         API JasperReports
	 */
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
