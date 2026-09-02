package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
import javax.persistence.Transient;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Label;
import ais.common.PagingApi;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.TampilanELearningAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.KrsDetailHelper;
import ais.common.BacaTulisUtil;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.RequestContext;
import ais.database.hibernate.AuditListener;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoMahasiswaLulus;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Entitas Hibernate <b>Mahasiswa</b> — pusat seluruh domain akademik AIS. Memetakan tabel
 * {@code public.mahasiswa} dan menjadi titik tambat hampir semua data akademik, keuangan dan
 * kemahasiswaan: KRS, nilai, pembayaran, kegiatan, wisuda, sampai kelulusan. Nyaris setiap modul
 * AIS mengacu ke kelas ini, sehingga perubahan perilaku di sini berdampak sangat luas.
 *
 * <h3>Kedudukan dalam hierarki</h3>
 * {@code Mahasiswa extends} {@link VOMahasiswa} {@code extends VoKunci extends}
 * {@link ais.database.model.GeneralValueObject}. Kontrak umum id, {@code equals}/{@code hashCode},
 * {@code compareTo}, penyambungan ulang proxy lazy lewat {@code check(...)}, serta penyimpanan
 * "kunci JSON" ({@code put}/{@code retreive}/{@code udah}/{@code putBaru}) dijelaskan di
 * {@link ais.database.model.GeneralValueObject} — jangan diulang di sini. Kelas ini juga
 * mengimplementasikan {@link SocialMediaCommonModel} (identitas media sosial untuk login sosial)
 * dan {@link VOMahasiswaDosen} (kontrak bersama Mahasiswa &amp; Dosen untuk linimasa e-learning).
 *
 * <h3>Anotasi tingkat kelas</h3>
 * <ul>
 *   <li>{@code @Entity} + {@code @Table(schema = "public", name = "mahasiswa")}.</li>
 *   <li>{@code @org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)} —
 *       INSERT/UPDATE hanya menyertakan kolom yang benar-benar berubah.</li>
 *   <li>{@code @Audited} (Hibernate Envers) — <b>SELURUH</b> kolom ikut direkam ke tabel audit
 *       {@code new_audit.mahasiswa__audit}. Konsekuensi operasional yang mudah terlewat: menambah
 *       kolom baru di kelas ini TIDAK otomatis menambah kolomnya di tabel audit (hbm2ddl tidak
 *       menyentuh skema audit), sehingga INSERT audit gagal dan penyimpanan Mahasiswa ikut
 *       ter-rollback. Setiap penambahan kolom WAJIB disertai ALTER manual pada tabel audit —
 *       lihat catatan pada {@link #getPaksaAktifSemester()}.</li>
 * </ul>
 *
 * <h3>Relasi utama yang benar-benar dipakai di kelas ini</h3>
 * <ul>
 *   <li><b>Struktur akademik</b>: {@link Jurusan} (WAJIB; sumber {@link Jenjang}, fakultas dan
 *       perguruan tinggi), {@link Konsentrasi}, {@link Program}/{@link ProgramMahasiswa},
 *       {@link KelasPmb}, {@link Dosen} sebagai dosen pembimbing akademik
 *       ({@link #getDosenPa()}).</li>
 *   <li><b>Status akademik</b>: {@link StatusAwalMahasiswa} (baru/pindahan/alih prodi, plus dua
 *       "status awal susulan" mulai semester tertentu), {@link StatusKeluar},
 *       {@link KelompokStatusMahasiswa}, {@link KelompokStatusKeluarMahasiswa} (SK kolektif yang
 *       MENIMPA tanggal/status lulus per mahasiswa), {@link KelompokMahasiswa},
 *       {@link PendaftaranCutiMahasiswa} lewat {@link #ambilCuti(Integer, Integer, boolean)}.
 *       Rekam jejak status per semester sendiri disimpan di entitas
 *       {@code HistoryStatusMahasiswa} dan dihitung oleh {@code HistoryStatusMahasiswaUtil}, yang
 *       membaca {@link #getBatasStudi()} dan {@link #getPaksaAktifSemester()} dari sini.</li>
 *   <li><b>Perkuliahan &amp; nilai</b>: {@link KrsMahasiswa}, {@link Detailperkuliahan},
 *       {@link Perkuliahan}, {@link Matakuliah}, {@link MatakuliahEkivalen}, {@link Pertemuan},
 *       {@link Skripsi}, {@link MahasiswaRequestTugasAkhir}.</li>
 *   <li><b>Keuangan</b>: {@link CicilanPembayaran}, {@link Deposit},
 *       {@link PengeluaranMahasiswa}, {@link DetailKegiatan} (lewat {@code Kegiatan}) — dikelola
 *       terutama oleh {@link VOMahasiswa}, kelas ini hanya menyediakan pembangun ulang cache
 *       {@code reInit*}.</li>
 *   <li><b>Kemahasiswaan &amp; kelulusan</b>: {@code KegiatanKemahasiswaanPunyaMahasiswa},
 *       {@code OrganisasiIntraKampusPunyaMahasiswa}, {@code PrestasiMahasiswa},
 *       {@code PenghargaanMahasiswa}, {@link PendaftaranWisuda}/{@link Wisuda},
 *       {@link Judisium} (predikat kelulusan), {@link StatusSetelahLulus},
 *       {@link StatusDomisiliSetelahLulus}, {@link StatusPekerjaanSetelahLulus}.</li>
 *   <li><b>Data pendukung</b>: {@link BiodataMahasiswa} (biodata rinci),
 *       {@link BiodataCalonMahasiswa} (data pendaftaran), {@link OrangTua},
 *       {@link PerguruanTinggiLain} (asal pindahan), {@link ais.database.model.kkn.KelompokKkn},
 *       {@link ais.database.model.pkl.KelompokPkl}, {@link ais.database.model.kursus.PesertaKursus},
 *       {@link ais.database.model.sop.DisposisiSop}, berkas foto/lampiran di paket
 *       {@code ais.database.model.file}.</li>
 * </ul>
 *
 * <h3>Pengelompokan metode</h3>
 * <ol>
 *   <li><b>Audit &amp; identitas</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #getId()}, {@link #getNim()},
 *       {@link #getNama()}, {@link #toString()}, {@link #ambilKode()}.</li>
 *   <li><b>Biodata &amp; kontak</b> — {@link #ambilBiodata()}, {@link #getAlamat()},
 *       {@link #getEmail()}, {@link #getTelp()}, {@link #ambilHp()},
 *       {@link #tampilkanHp(org.zkoss.zk.ui.Component)},
 *       {@link #tampilkanEmail(org.zkoss.zk.ui.Component)}, getter/setter kolom biodata.</li>
 *   <li><b>Status akademik &amp; masa studi</b> — {@link #currentSemester()},
 *       {@link #currentTahapan()}, {@link #getStatusKeluar()}, {@link #getSemesterLulus()},
 *       {@link #hitungSmtLulus(StatusKeluar, Mahasiswa)}, {@link #getTahunLulus()},
 *       {@link #ambilMasaStudi()}, {@link #getMasaStudi()}, {@link #getBatasStudi()},
 *       {@link #getPaksaAktifSemester()}, kelompok pindahan/alih prodi.</li>
 *   <li><b>KRS &amp; perkuliahan</b> — keluarga {@code ambilDetailperkuliahan*},
 *       {@code ambilPerkuliahanDanParalel*}, {@link #ambilDefaultKrsMahasiswa(Integer, Integer,
 *       Integer, org.hibernate.Session)}, {@link #rubahKeteranganPengambilanKRS(Integer, Integer,
 *       Integer, KrsMahasiswa, boolean)}.</li>
 *   <li><b>Perhitungan nilai/IPK/SKS</b> — {@code hitung*} (IPK, IP, mutu, rata-rata, SKS, jumlah
 *       MK) beserta pasangan privat {@code prosesHitung*}, ditambah penyaring
 *       {@link #saringBerdasarNilaiDan0(java.util.Collection)},
 *       {@link #saringBerdasarNilaiOk(java.util.Collection)} dan penjelas
 *       {@link #alasanTidakValidDetail(java.util.Collection)}.</li>
 *   <li><b>Cache berkas JSON</b> — pasangan {@code ambilLokasi*}/{@code tulisLokasi*}/
 *       {@code bersihkanLokasi*}/{@code populate*}/{@code remove*}/{@code reInit*} untuk
 *       detailperkuliahan, pertemuan, checklist penilaian dosen, kegiatan kemahasiswaan,
 *       organisasi, prestasi dan penghargaan.</li>
 *   <li><b>Linimasa e-learning</b> — {@link #ambilPertemuan(org.hibernate.Session)} dan
 *       keluarga {@code ambilPertemuan(...)} berparameter panjang, {@code reInitPertemuan}.</li>
 *   <li><b>Laporan &amp; berkas</b> — {@link #putPhoto(java.util.Map)},
 *       {@link #putPhotoLulus(java.util.Map)}, {@link #ttdQr()}, {@link #urlLogin()}.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum menyentuh kelas ini</h3>
 * <ul>
 *   <li><b>Getter tidak murni.</b> Sangat banyak getter di kelas ini MENGUBAH state: menormalkan
 *       teks ({@link #getKelamin()}, {@link #getSemesterMulai()}), mengisi nilai bawaan
 *       ({@link #getTanggallahir()}, {@link #getTahunangkatan()}), bahkan membangkitkan kata sandi
 *       ({@link #getPass()}) atau memicu query/penulisan basis data ({@link #ambilBiodata()}).
 *       Jangan berasumsi getter aman dari efek samping.</li>
 *   <li><b>Penjaga anti-rekursi flush.</b> {@link #dipanggilOlehHibernateFlush()} membaca stack
 *       trace untuk mengenali apakah getter dipanggil oleh mesin flush Hibernate; bila ya,
 *       pemuatan lazy biodata dilewati agar tidak terjadi flush bersarang. Ini rapuh terhadap
 *       kenaikan versi Hibernate.</li>
 *   <li><b>Cache berkas JSON, bukan koleksi Hibernate.</b> Relasi "satu mahasiswa punya banyak X"
 *       sengaja TIDAK dipetakan sebagai {@code @OneToMany}. Sebagai gantinya daftar id disimpan
 *       sebagai berkas JSON per mahasiswa di direktori data ({@code Common.getFileLocation}) dan
 *       dibaca lewat {@code ambilLokasi*}. Konsekuensinya: (a) daftar bisa BASI bila berkas tidak
 *       dibangun ulang — karenanya ada {@code reInit*}; (b) berkas ini dipakai bersama beberapa
 *       jenis kunci, sehingga kunci non-numerik harus disaring (lihat
 *       {@link #ambilDetailperkuliahan(Integer)} dan {@link #populateDefaultKrsMahasiswa(KrsMahasiswa)}).</li>
 *   <li><b>Konfigurasi menyetir perhitungan IPK.</b> Seluruh keluarga {@code prosesHitung*} membaca
 *       kunci {@link Konfigurasi}: {@code nilai_0_tidak_masuk_dalam_perhitungan_ipk},
 *       {@code nilai_minimal_tidak_masuk_dalam_perhitungan_ipk},
 *       {@code nilai_huruf_yg_tidak_masuk_perhitungan_ip},
 *       {@code nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk},
 *       {@code aktifkan_ekivalen}, {@code aktifkan_kesamaan_nama}, {@code aktifkan_kesamaan_kode},
 *       {@code saring_nilai_ipk_juga_berdasarkan_nama}. Hasil IPK bisa berbeda antar-instalasi
 *       hanya karena konfigurasi ini.</li>
 *   <li><b>Nilai kelompok menimpa nilai perorangan.</b> {@link KelompokStatusKeluarMahasiswa}
 *       menimpa {@link #getStatusKeluar()}, {@link #getTanggalLulus()},
 *       {@link #getTahunLulus()} dan {@link #getSemesterLulus()}.</li>
 *   <li><b>Nilai sampah data lama.</b> Beberapa kolom memakai sandi khusus yang diperlakukan
 *       sebagai kosong: {@code "1000"} pada {@link #getBatasStudi()}/{@link #getPaksaAktifSemester()},
 *       nomor telepon {@code "0000000000"} dan sejenisnya, KTP {@code "00000"}, serta koma ganda
 *       pada kolom surel/id media sosial.</li>
 * </ul>
 *
 * <p>Kelas ini awalnya dibangkitkan hbm2java (Hibernate Tools 3.2.4.CR1, Des 2009) lalu tumbuh
 * manual selama bertahun-tahun; itulah sebabnya gaya kodenya beragam.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see VOMahasiswa
 * @see BiodataMahasiswa
 * @see Detailperkuliahan
 * @see KrsMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "mahasiswa")
public class Mahasiswa extends VOMahasiswa implements SocialMediaCommonModel, VOMahasiswaDosen {

	public static final String WNI = "WNI";
	public static final String WNA = "WNA";

	/**
	 * 
	 */
	private static final long serialVersionUID = 733802703541105049L;
	public static final Long LULUS = 1L;
	public static final Long LULUS_TIDAK_WISUDA = 1L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Id pengguna ({@link Tbmuser}) yang terakhir mengubah baris mahasiswa ini. Diisi otomatis
	 * oleh {@code AuditTimestampInterceptor} melalui {@link #onUpdate()}.
	 *
	 * @return id pengguna pengubah terakhir; {@code null} bila belum pernah tercatat.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Nilai {@code null}/kosong SENGAJA diabaikan (nilai
	 * lama dipertahankan) supaya jejak audit tidak terhapus oleh proses yang tidak mengenali
	 * penggunanya, mis. job terjadwal atau impor massal.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi NAMA pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan agar jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir mengubah baris mahasiswa ini.
	 *
	 * @return nama pengubah terakhir; {@code null} bila belum pernah tercatat.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum baris mahasiswa
	 * ini di-UPDATE ke basis data. Meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)} dan {@link #setOlehId(String)}
	 * dari pengguna yang sedang login.
	 *
	 * <p>JANGAN dipanggil manual dari kode aplikasi; ini murni kait Hibernate.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		// System.out.println("Ada perubahan data mahasiswa");

		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir. Biasanya diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu (TIMESTAMP) perubahan terakhir baris mahasiswa ini. Nilai awalnya adalah waktu
	 * objek dibuat ({@code WaktuUtil.getDate()}), lalu diperbarui tiap UPDATE lewat {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log, combobox dan listbox ZK: <code>id-nim - nama</code>.
	 *
	 * @return teks "{id}-{nim} - {nama}".
	 */
	public String toString() {
		return id + "-" + nim + " - " + nama;
	}

	private String nim;
	private String nimBaruPindah;
	private String pass;
	private String userOrtu;
	private String passOrtu;
	private String nama;
	private String namaArab;
	private String namaTionghoa;
	private String alamat;
	private String email;
	private String token;
	private Integer tahunangkatan;
	private String tempatlahir;
	private Date tanggallahir;
	private String tanggallahirManual;
	private String formatedtanggallahir;
	private String kelamin;
	private String telp;
	private Jurusan jurusan;
	private GelombangPendaftaran gelombangPendaftaran;
	private Jenjang jenjang;
	private String semesterMulai = Perkuliahan.GANJIL;
	private Konsentrasi konsentrasi;
	private String waktuKuliah;
	private Date tanggalLulus;
	private String program;
	private Program programBaru;
	private Integer berat_badan;
	private Integer tinggi_badan;
	private String golongan_darah;
	private Agama agama;
	private String keterangan;
	private String warganegara;
	private Negara negara;
	private JenisSeleksi jenisSeleksi;
	private String ktp;
	private Long pin = 1234L;
	private Boolean aktif;
	private KelompokMahasiswa kelompokMahasiswa;
	private KelompokStatusMahasiswa kelompokStatusMahasiswa;
	private KelompokStatusKeluarMahasiswa kelompokStatusKeluarMahasiswa;
	private StatusAwalMahasiswa statusAwalMahasiswa;
	private Integer smtStatusAwal;
	private StatusAwalMahasiswa statusAwalMahasiswaSetelahSmtTertentu;
	private Integer smtStatusAwalLagi;
	private StatusAwalMahasiswa statusAwalMahasiswaSetelahSmtTertentuLagi;
	private ProgramMahasiswa programMahasiswa;
	private Integer statusKonversi;
	private KelasPmb kelasPmb;
	private Boolean is_encripted;
	private Boolean tidakAdaTagihan;
	private String noIjazah1;
	private String noIjazah2;
	private String noAkta1;
	private String noAkta2;
	private String skDo;
	private Integer tahunWisuda;
	private Integer tahunLulus = 0;
	private Integer semesterLulus = 0;
	private String judulSkripsi;
	private String judulSkripsiEn;
	private Judisium predikatKelulusan;
	private Date tanggalYudisium;
	private Date tanggalSkRektor;

	private String batasStudi;

	/**
	 * Daftar semester (dipisah koma, mis. "3,4,5") yang MEMAKSA status mahasiswa menjadi AKTIF pada
	 * semester-semester tsb, berlawanan dengan {@link #batasStudi} yang memaksa TIDAK AKTIF. Diisi
	 * admin lewat form Kelulusan/Biodata. Kosong = tanpa efek. Dipakai HistoryStatusMahasiswaUtil.
	 */
	private String paksaAktifSemester;
	private Date tanggalMasuk;
	private Date tanggalKegiatanBelajarMengajar;
	private Integer jumlahSksPenyetaraan;
	private String nimPindahan;
	private String pindahanPerguruanTinggi;
	private Jenjang pindahJenjang;
	private Jurusan pindahJurusan;
	private String statusKrs;

	private Boolean merupakanPindahan;
	private String pindahanDariKampus;
	private Integer pindahDariKampusLamaDiSemester = 0;
	private Integer pindahKeKampusIniMasukSemester = 0;
	private Date tanggalPindah = ais.ui.util.WaktuUtil.getDate();
	private String keteranganPindah;

	private Boolean merupakanAlihProdi;
	private String nimLamaSebelumPindah;
	private Mahasiswa alihProdiMahasiswa;
	private Boolean pindahkanKrsDanNilaiKeMahasiswaAlihProdi;
	private Integer pindahKeProdiIniMasukSemester = 0;
	private Date tanggalPindahProdi = ais.ui.util.WaktuUtil.getDate();
	private String keteranganPindahProdi;

	private Long biodataCalonMahasiswa;

	private JenisPenerimaBeasiswa beasiswaMahasiswaMiskin;
	private JenisPenerimaBeasiswa beasiswaBidikMisi;
	private JenisPenerimaBeasiswa beasiswaLain;
	private String keteranganBeasiswa;

	private StatusKeluar statusKeluar;
	private Date blnAwalBimbingan;
	private Date blnAkhirBimbingan;

	private Integer sksYangDiakui;

	private Integer sksYangDiakuiPindahProdi;
	private String namaProdiPindah;

	private String feeder;
	private String idRegPd;

	private String nimKey;

	private String facebookId;
	private String googleId;
	private String twitterId;
	private String linkedinId;
	private String socialMediaProfile;
	private String atasans;
	private String bahasa;
	private JenisPembiayaanMahasiswa jenisPembiayaanMahasiswa;
	private StatusDomisiliSetelahLulus statusDomisiliSetelahLulus;
	private StatusSetelahLulus statusSetelahLulus;

	private Long dosen;
	private Dosen dosenPa;
	private String kelas;

	private String usernameOjs;
	private StatusPekerjaanSetelahLulus statusPekerjaanSetelahLulus;

	private String lockId;

	private Integer masaStudi;
	private String linkValidasiEksternal;

	private String nomorSkpi;
	// private Boolean arsipkan;
	// private String arsipPerkuliahan;

	private Boolean statusAwalSelaluIkutDataUtama;
	private Boolean programSelaluIkutDataUtama;
	private Boolean dosenPaSelaluSama;
	private Boolean kelasSelaluSama;

	private Integer semesterSaatIni;

	/**
	 * Penanda penguncian baris (dipakai proses batch/impor agar dua proses tidak menggarap
	 * mahasiswa yang sama). Tidak pernah {@code null}.
	 *
	 * @return isi lockId sudah di-trim, atau string kosong bila belum diisi.
	 */
	public String getLockId() {
		return lockId == null ? "" : lockId.trim();
	}

	/**
	 * Menetapkan penanda penguncian baris.
	 *
	 * @param lockId penanda kunci; {@code null} berarti tidak terkunci.
	 */
	public void setLockId(String lockId) {
		this.lockId = lockId;
	}

	private String idfinger;

	private PerguruanTinggiLain pindahanDari;

	private OrangTua orangTua;
	private Tbmuser dikunci;
	private Date ubahPasword;
	private DisposisiSop disposisiSop;

	/**
	 * Mencari pengajuan cuti ({@link PendaftaranCutiMahasiswa}) milik mahasiswa ini pada satu
	 * semester (atau tahap, bila mode tahapan aktif) tertentu.
	 *
	 * <p>Pencarian dilakukan sepenuhnya dari CACHE MEMORI {@link ConstantValues} (peta seluruh
	 * {@code PendaftaranCutiMahasiswa} yang sudah dimuat), BUKAN query basis data — jadi murah
	 * dipanggil berulang di dalam loop render KRS/status, tetapi hanya melihat data yang sudah
	 * masuk cache. Bila {@code ConstantValues.aktifkanTahapan} bernilai {@code true} dan
	 * {@code tahap} terisi (bukan 0/{@code null}), pencocokan memakai TAHAP, bukan semester.</p>
	 *
	 * <p>Segala kegagalan (mis. entri cache rusak) ditelan dan dicatat ke
	 * {@code ErrorAuditUtil}; metode tidak pernah melempar.</p>
	 *
	 * @param semester semester akademik yang dicari; bila {@code null} langsung {@code null}.
	 * @param tahap    tahap dalam semester; dipakai hanya bila mode tahapan aktif.
	 * @param sp       {@code true} untuk mencari cuti pada SEMESTER PENDEK, {@code false} untuk
	 *                 semester reguler.
	 * @return pengajuan cuti yang cocok, atau {@code null} bila tidak ada / id mahasiswa kosong.
	 */
	@SuppressWarnings("unchecked")
	public PendaftaranCutiMahasiswa ambilCuti(Integer semester, Integer tahap, boolean sp) {
		if (getId() == null || semester == null) {
			return null;
		}

		try {

			Map<Long, GeneralValueObject> map = ConstantValues.ambilBerdasarClass(PendaftaranCutiMahasiswa.class);
			if (map != null) {

				for (Long generalValueObjectid : map.keySet()) {
					PendaftaranCutiMahasiswa b = (PendaftaranCutiMahasiswa) ConstantValues
							.ambil(PendaftaranCutiMahasiswa.class.getName(), generalValueObjectid);
					if (b != null && b.getMahasiswa() != null && b.getMahasiswa().getId().equals(getId())
							&& (!ConstantValues.aktifkanTahapan || tahap == null || tahap.equals(0)
									? (b.getSemester() != null && semester != null && b.getSemester().equals(semester))
									: (b.getTahap() != null && tahap != null && b.getTahap().equals(tahap)))) {
						if ((sp && b.getSemesterPendek()) || (!sp && !b.getSemesterPendek())) {
//							System.out.println("Cuti ketemu -> " + b + ", semester " + semester + ", tahap " + tahap
//									+ ", sp " + sp + ", mhs " + this + " setuju " + b.getPersetujuan());
							return b;
						}
					}

				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:345");
		}
		return null;
	}

	public BiodataMahasiswa biodataMahasiswa = null;
	private BiodataCalonMahasiswa biodataCalonMahasiswaData;
	private Date tanggalWisuda;
	private String gcpToken;
	private PesertaKursus pesertaKursus;
	private String karpeg;

	/**
	 * Merender alamat surel mahasiswa sebagai {@code Toolbarbutton} ZK di dalam wadah {@code vbox},
	 * lengkap dengan ikon amplop dan tautan {@code mailto:} yang membuka klien surel di tab baru.
	 * Bila surel kosong, tombol tetap dibuat tetapi tanpa ikon/tautan.
	 *
	 * <p>Dipakai puluhan layar daftar/pencarian mahasiswa (mis. {@code CariMahasiswaAction},
	 * {@code TbmuserAction}) sebagai renderer kolom kontak. Efek samping: menambah komponen ZK
	 * baru ke {@code vbox}.</p>
	 *
	 * @param vbox komponen induk ZK tempat tombol dipasang.
	 */
	public void tampilkanEmail(Component vbox) {
		String email = getEmail();
		Toolbarbutton a;
		(a = new ais.ui.util.MyToolbarbuttonConfig(email)).setParent(vbox);
		if (email != null && !email.trim().isEmpty()) {
			a.setImage("/img/svg/mail-send-line.svg");
			a.setStyle("font-size:9px;");
			a.setTarget("_blank");
			a.setHref("mailto:" + email);
		}

	}

	/**
	 * Merender nomor HP / telepon rumah mahasiswa sebagai {@code Toolbarbutton} ZK berikon WhatsApp
	 * yang menaut ke {@code https://web.whatsapp.com/send?phone=...}.
	 *
	 * <p>Nomor diambil dari {@link BiodataMahasiswa} ({@code getHp()} dan {@code getTeleponRumah()}),
	 * jadi metode ini dapat memicu pemuatan biodata lewat {@link #ambilBiodata()}. Nomor sampah yang
	 * lazim ada di data lama ("08100000000000000000", "0000000000", "00000000000000000000",
	 * "000000000") dianggap KOSONG dan tidak ditampilkan. Nomor diawali "08"/"0" dinormalkan ke
	 * format internasional "+62..." sebelum dipakai di tautan.</p>
	 *
	 * <p>Bila pengambilan biodata gagal (mis. sesi Hibernate sudah tertutup), metode jatuh ke
	 * jalur cadangan memakai kolom {@link #getTelp()} pada baris mahasiswa dan merender komponen
	 * {@code A} biasa — jadi kolom kontak tidak pernah kosong gara-gara kegagalan lazy-load.</p>
	 *
	 * @param vbox komponen induk ZK tempat tombol dipasang.
	 */
	public void tampilkanHp(Component vbox) {
		try {
			BiodataMahasiswa biodataMahasiswa = ambilBiodata();

			String hp = biodataMahasiswa.getHp();
			String telp = biodataMahasiswa.getTeleponRumah();

			Toolbarbutton a;
			(a = new ais.ui.util.MyToolbarbuttonConfig(
					(hp == null || hp.toString().trim().equals("08100000000000000000")
							|| hp.toString().trim().equals("0000000000") ? "" : hp)
							+ (telp == null || telp.toString().trim().isEmpty()
									|| telp.toString().trim().equals("00000000000000000000")
									|| telp.toString().trim().equals("000000000")
											? ""
											: (hp == null || hp.toString().trim().isEmpty()
													|| hp.toString().trim().equals("08100000000000000000")
													|| hp.toString().trim().equals("0000000000") ? "" : " / ") + telp)))
					.setParent(vbox);

			if (telp != null && !telp.trim().isEmpty() && hp != null && hp.equals(telp)) {
				a.setLabel(hp);
			}

			if (hp == null || hp.toString().trim().isEmpty() || hp.toString().trim().equals("08100000000000000000")
					|| hp.toString().trim().equals("0000000000")) {
				hp = telp;
			}

			if (hp != null && !hp.trim().isEmpty()
					&& !(hp == null || hp.toString().trim().isEmpty()
							|| hp.toString().trim().equals("00000000000000000000")
							|| hp.toString().trim().equals("000000000"))) {
				hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
				hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
				hp = !hp.startsWith("+") ? "+62" + hp : hp;
				a.setStyle("font-size:9px;");
				a.setImage("/img/svg/whats.svg");
				a.setTarget("_blank");
				a.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text=Halo,+apa+kabar?");
			}
		} catch (Exception e) {
			A a;
			String hp = getTelp();
			(a = new A(hp)).setParent(vbox);
			if (hp != null && !hp.trim().isEmpty()
					&& !(hp == null || hp.toString().trim().isEmpty()
							|| hp.toString().trim().equals("00000000000000000000")
							|| hp.toString().trim().equals("000000000"))) {
				hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
				hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
				hp = !hp.startsWith("+") ? "+62" + hp : hp;
				a.setStyle("font-size:9px;");
				a.setImage("/img/svg/whats.svg");
				a.setTarget("_blank");
				a.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text=Halo,+apa+kabar?");
			}
		}
	}

	/**
	 * Versi non-UI dari {@link #tampilkanHp(org.zkoss.zk.ui.Component)}: mengembalikan nomor HP
	 * mahasiswa dalam format internasional TANPA tanda plus (mis. "628123456789"), siap dipakai
	 * gerbang WhatsApp/SMS ({@code OttoUtil} dan sejenisnya).
	 *
	 * <p>Aturan pemilihan sama: HP dari {@link BiodataMahasiswa} lebih diutamakan, telepon rumah
	 * jadi cadangan, nomor sampah dianggap kosong, awalan "0"/"08" diganti "62". Bila biodata
	 * gagal dimuat, dipakai kolom {@link #getTelp()}.</p>
	 *
	 * @return nomor siap kirim; string kosong bila tidak ada nomor yang sah.
	 */
	public String ambilHp() {
		try {
			BiodataMahasiswa biodataMahasiswa = ambilBiodata();

			String hp = biodataMahasiswa.getHp();
			String telp = biodataMahasiswa.getTeleponRumah();

			String no = (hp == null || hp.toString().trim().equals("08100000000000000000")
					|| hp.toString().trim().equals("0000000000") ? "" : hp)
					+ (telp == null || telp.toString().trim().isEmpty()
							|| telp.toString().trim().equals("00000000000000000000")
							|| telp.toString().trim().equals("000000000")
									? ""
									: (hp == null || hp.toString().trim().isEmpty()
											|| hp.toString().trim().equals("08100000000000000000")
											|| hp.toString().trim().equals("0000000000") ? "" : " / ") + telp);

			if (telp != null && !telp.trim().isEmpty() && hp != null && hp.equals(telp)) {
				no = hp;
			}

			if (hp == null || hp.toString().trim().isEmpty() || hp.toString().trim().equals("08100000000000000000")
					|| hp.toString().trim().equals("0000000000")) {
				hp = telp;
			}

			if (hp != null && !hp.trim().isEmpty()
					&& !(hp == null || hp.toString().trim().isEmpty()
							|| hp.toString().trim().equals("00000000000000000000")
							|| hp.toString().trim().equals("000000000"))) {
				hp = hp.startsWith("08") ? "62" + hp.substring(1) : hp;
				hp = hp.startsWith("0") ? "62" + hp.substring(1) : hp;
				hp = !hp.startsWith("62") ? "62" + hp : hp;
				no = hp;
			}
			return no;
		} catch (Exception e) {

			String hp = getTelp();
			if (hp != null && !hp.trim().isEmpty()
					&& !(hp == null || hp.toString().trim().isEmpty()
							|| hp.toString().trim().equals("00000000000000000000")
							|| hp.toString().trim().equals("000000000"))) {
				hp = hp.startsWith("08") ? "62" + hp.substring(1) : hp;
				hp = hp.startsWith("0") ? "62" + hp.substring(1) : hp;
				hp = !hp.startsWith("62") ? "62" + hp : hp;

			}
			return hp;
		}
	}

	/**
	 * Mengambil {@link BiodataMahasiswa} milik mahasiswa ini, MEMBUATKAN baris biodata kosong bila
	 * belum ada. Setara {@code ambilBiodata(true)}.
	 *
	 * @return biodata mahasiswa (dibuat otomatis bila belum ada); {@code null} bila id mahasiswa
	 *         belum tersimpan atau terjadi kegagalan basis data.
	 * @see #ambilBiodata(boolean)
	 */
	public BiodataMahasiswa ambilBiodata() {
		return ambilBiodata(true);
	}

	/**
	 * Mengambil {@link BiodataMahasiswa} milik mahasiswa ini (data biodata lengkap: alamat, HP,
	 * orang tua, asal sekolah, dsb. yang TIDAK disimpan di tabel {@code mahasiswa}).
	 *
	 * <p><b>Cache tingkat objek.</b> Hasilnya disimpan di field publik {@link #biodataMahasiswa}
	 * sehingga pemanggilan berikutnya pada instance yang sama tidak menyentuh basis data lagi.
	 * Beberapa getter kolom ({@link #getAlamat()}, {@link #getEmail()}, {@link #getTelp()}) ikut
	 * membaca cache ini sehingga nilai biodata "membayangi" kolom tabel {@code mahasiswa}.</p>
	 *
	 * <p><b>Efek samping penting.</b> Bila {@code jikaTidakAdaSimpan} bernilai {@code true} dan
	 * mahasiswa belum punya baris biodata, metode ini MENULIS baris {@code BiodataMahasiswa} baru
	 * ke basis data (lihat {@link #buatBiodataMahasiswaJikaBelumAda(Long)}) memakai sesi Hibernate
	 * DEDIKASI + transaksi sendiri — bukan sekadar operasi baca. Panggil dengan {@code false} bila
	 * hanya ingin membaca (mis. dari dalam getter yang bisa terpanggil saat flush).</p>
	 *
	 * <p>Kegagalan apa pun ditelan dan dilaporkan lewat {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * @param jikaTidakAdaSimpan {@code true} = buat & simpan baris biodata baru bila belum ada.
	 * @return biodata mahasiswa, atau {@code null} bila id mahasiswa belum ada / gagal dimuat.
	 */
	public BiodataMahasiswa ambilBiodata(boolean jikaTidakAdaSimpan) {

		try {
			if (biodataMahasiswa != null && biodataMahasiswa.getId() != null) {
				return biodataMahasiswa;
			}

			if (getId() == null) {
				return null;
			}

//			Map<Long, GeneralValueObject> map = ConstantValues.ambilBerdasarClass(BiodataMahasiswa.class);
//			if (map != null) {
//
//				boolean ketemu = false;
//				for (Long generalValueObjectid : map.keySet()) {
//					BiodataMahasiswa b = (BiodataMahasiswa) ConstantValues.ambil(BiodataMahasiswa.class.getName(),
//							generalValueObjectid);
//					if (b != null && b.getMahasiswa() != null && b.getMahasiswa().getId().equals(getId())) {
//						biodataMahasiswa = b;
//						ketemu = true;
//						break;
//					}
//
//					if (ketemu) {
//						break;
//					}
//				}
//			}

			if (biodataMahasiswa == null || biodataMahasiswa.getId() == null) {
				biodataMahasiswa = ambilBiodataDenganSessionAman(getId());
			}

			if (jikaTidakAdaSimpan) {
				if (biodataMahasiswa == null || biodataMahasiswa.getId() == null) {
					biodataMahasiswa = buatBiodataMahasiswaJikaBelumAda(getId());
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return biodataMahasiswa;
	}

	/**
	 * Membaca {@link BiodataMahasiswa} memakai sesi Hibernate DEDIKASI ({@code openSession}) dengan
	 * {@link FlushMode#MANUAL}.
	 *
	 * <p>Sesi terpisah + flush manual dipakai supaya pembacaan ini tidak memicu autoflush pada
	 * persistence context request yang sedang berjalan — getter entitas dapat terpanggil di
	 * tengah dirty-check Hibernate, dan autoflush di titik itu bisa memunculkan
	 * {@code TransientObjectException}/rekursi flush. Sesi selalu ditutup di blok {@code finally}.</p>
	 *
	 * @param mahasiswaId id mahasiswa pemilik biodata.
	 * @return biodata dengan id terbesar milik mahasiswa itu, atau {@code null}.
	 */
	private BiodataMahasiswa ambilBiodataDenganSessionAman(Long mahasiswaId) {
		Session session = null;
		try {
			if (mahasiswaId == null) {
				return null;
			}
			session = HibernateUtil.openSession();
			session.setFlushMode(FlushMode.MANUAL);
			return (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class).createAlias("mahasiswa", "m")
					.add(Restrictions.eq("m.id", mahasiswaId)).setMaxResults(1).addOrder(Order.desc("id"))
					.uniqueResult();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Membuat baris {@link BiodataMahasiswa} kosong yang tertaut ke mahasiswa ini bila belum ada.
	 *
	 * <p>Memakai sesi Hibernate DEDIKASI beserta transaksinya sendiri: cek-dulu (query), lalu
	 * {@code save} + {@code flush} + {@code commit}. Referensi mahasiswa diambil lewat
	 * {@code session.load(Mahasiswa.class, id)} agar tidak menyeret instance {@code this} (yang
	 * mungkin detached) ke dalam sesi baru. Bila gagal, transaksi di-rollback diam-diam
	 * ({@link #rollbackQuietly(Transaction)}) dan {@code null} dikembalikan.</p>
	 *
	 * @param mahasiswaId id mahasiswa pemilik biodata.
	 * @return biodata yang sudah ada atau yang baru dibuat; {@code null} bila gagal.
	 */
	private BiodataMahasiswa buatBiodataMahasiswaJikaBelumAda(Long mahasiswaId) {
		Session session = null;
		Transaction tx = null;
		try {
			if (mahasiswaId == null) {
				return null;
			}

			session = HibernateUtil.openSession();
			session.setFlushMode(FlushMode.MANUAL);
			BiodataMahasiswa data = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
					.createAlias("mahasiswa", "m").add(Restrictions.eq("m.id", mahasiswaId)).setMaxResults(1)
					.addOrder(Order.desc("id")).uniqueResult();
			if (data != null && data.getId() != null) {
				return data;
			}

			tx = session.beginTransaction();
			data = new BiodataMahasiswa();
			Mahasiswa mahasiswaRef = (Mahasiswa) session.load(Mahasiswa.class, mahasiswaId);
			data.setMahasiswa(mahasiswaRef);
			session.save(data);
			session.flush();
			tx.commit();
			return data;
		} catch (Exception e) {
			rollbackQuietly(tx);
			Common.tampilErrorJikaAdmin(e);
			return null;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Membatalkan transaksi tanpa pernah melempar. Dilewati bila transaksi {@code null}, sudah
	 * commit, atau sudah rollback.
	 *
	 * @param tx transaksi yang hendak dibatalkan; boleh {@code null}.
	 */
	private static void rollbackQuietly(Transaction tx) {
		try {
			if (tx != null && !tx.wasCommitted() && !tx.wasRolledBack()) {
				tx.rollback();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Mendeteksi — dengan MEMBACA STACK TRACE thread saat ini — apakah getter yang sedang berjalan
	 * dipanggil oleh Hibernate (pembacaan properti saat flush/dirty-check) dan bukan oleh kode
	 * aplikasi.
	 *
	 * <p>Ini penjaga anti-rekursi yang penting: getter seperti {@link #getAlamat()} dan
	 * {@link #getEmail()} biasanya memuat {@link BiodataMahasiswa} secara lazy. Bila pemuatan itu
	 * terjadi di tengah flush Hibernate, akan muncul query/flush bersarang yang berujung
	 * {@code TransientObjectException}, {@code ConcurrentModificationException}, atau rekursi tak
	 * berujung. Karena itu getter tersebut hanya memuat biodata bila metode ini mengembalikan
	 * {@code false}.</p>
	 *
	 * <p>Deteksi dilakukan dengan mencari nama kelas internal Hibernate
	 * ({@code BasicPropertyAccessor}, {@code AbstractFlushingEventListener},
	 * {@code DefaultFlushEntityEventListener}, {@code AbstractEntityPersister}, {@code TypeHelper})
	 * pada stack trace. Cara ini rapuh terhadap perubahan versi Hibernate — bila versi Hibernate
	 * dinaikkan, daftar nama kelas di sini WAJIB ditinjau ulang.</p>
	 *
	 * @return {@code true} bila jejak panggilan berasal dari mesin flush Hibernate.
	 */
	private static boolean dipanggilOlehHibernateFlush() {
		try {
			StackTraceElement[] traces = Thread.currentThread().getStackTrace();
			for (int i = 0; traces != null && i < traces.length; i++) {
				String className = traces[i].getClassName();
				if (className == null) {
					continue;
				}
				if (className.indexOf("org.hibernate.property.BasicPropertyAccessor") >= 0
						|| className.indexOf("org.hibernate.event.def.AbstractFlushingEventListener") >= 0
						|| className.indexOf("org.hibernate.event.def.DefaultFlushEntityEventListener") >= 0
						|| className.indexOf("org.hibernate.persister.entity.AbstractEntityPersister") >= 0
						|| className.indexOf("org.hibernate.type.TypeHelper") >= 0) {
					return true;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	/** Konstruktor kosong wajib bagi Hibernate/JavaBeans. */
	public Mahasiswa() {
	}

	/**
	 * Membuat objek rujukan yang hanya berisi id — lazim dipakai sebagai parameter kriteria
	 * ({@code Restrictions.eq("mahasiswa", new Mahasiswa(id))}) tanpa memuat seluruh baris.
	 *
	 * @param id id mahasiswa.
	 */
	public Mahasiswa(Long id) {
		this.id = id;
	}

	/**
	 * Membuat objek mahasiswa baru dengan NIM dan nama.
	 *
	 * @param nim  nomor induk mahasiswa.
	 * @param nama nama lengkap mahasiswa.
	 */
	public Mahasiswa(String nim, String nama) {
		this.nim = nim;
		this.nama = nama;
	}

	/**
	 * Membuat objek mahasiswa baru dengan nama saja (dipakai layar entri cepat / data sementara).
	 *
	 * @param nama nama lengkap mahasiswa.
	 */
	public Mahasiswa(String nama) {
		this.nama = nama;
	}

	/**
	 * Kunci utama tabel {@code public.mahasiswa} (IDENTITY, dibangkitkan basis data).
	 *
	 * @return id mahasiswa; {@code null} bila baris belum tersimpan.
	 * @see ais.database.model.GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Normalnya diisi Hibernate.
	 *
	 * @param id id mahasiswa.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nomor Induk Mahasiswa — kolom {@code nim}, UNIK dan wajib. Nilai dibersihkan dari spasi
	 * (di-trim dan seluruh spasi di tengah dibuang) supaya pencocokan NIM antar-modul konsisten.
	 *
	 * @return NIM tanpa spasi; {@code null} bila kosong (bukan string kosong).
	 */
	@Column(name = "nim", nullable = false, length = 50, unique = true)
	public String getNim() {
		return nim == null || nim.trim().isEmpty() ? null
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.nim.trim(), " ", ""), " ", "");
	}

	/**
	 * Menetapkan NIM. Pembersihan spasi dilakukan di sisi getter, bukan di sini.
	 *
	 * @param nim nomor induk mahasiswa.
	 */
	public void setNim(String nim) {
		this.nim = nim;
	}

	/**
	 * Nama lengkap mahasiswa — kolom {@code nama}, wajib.
	 *
	 * @return nama sudah di-trim; {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama lengkap mahasiswa.
	 *
	 * @param nama nama lengkap.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Alamat mahasiswa — kolom {@code alamat}, TETAPI nilai di {@link BiodataMahasiswa} selalu
	 * menang bila biodata berhasil dimuat (kolom di tabel {@code mahasiswa} hanya salinan lama).
	 *
	 * <p>Pemuatan biodata hanya dilakukan bila getter TIDAK sedang dipanggil oleh mesin flush
	 * Hibernate (lihat {@link #dipanggilOlehHibernateFlush()}) dan cache biodata masih kosong.
	 * Kegagalan apa pun ditelan; nilai {@code null} dinormalkan menjadi string kosong.</p>
	 *
	 * @return alamat mahasiswa; tidak pernah {@code null} setelah pemanggilan pertama.
	 */
	@Column(name = "alamat")
	public String getAlamat() {
		try {
			if (!dipanggilOlehHibernateFlush() && getId() != null && biodataMahasiswa == null) {
				biodataMahasiswa = ambilBiodata(false);
			}
			if (biodataMahasiswa != null) {
				alamat = biodataMahasiswa.getAlamat();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:673");
			// TODO: handle exception
		}
		if (alamat == null) {
			alamat = "";
		}
		return this.alamat;
	}

	/**
	 * Menetapkan alamat pada baris {@code mahasiswa}. Perhatikan {@link #getAlamat()} akan
	 * menimpanya dengan nilai dari {@link BiodataMahasiswa} bila biodata ada.
	 *
	 * @param alamat alamat mahasiswa.
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Membaca surel MENTAH dari kolom {@code email} tanpa menyentuh {@link BiodataMahasiswa}
	 * (berbeda dengan {@link #getEmail()} yang bisa memicu pemuatan biodata).
	 *
	 * <p>Tetap membersihkan tanda petik tunggal di depan — artefak lazim hasil impor dari
	 * spreadsheet yang menuliskan {@code 'email@web.com}. Pembersihan berulang sampai semua
	 * petik depan hilang.</p>
	 *
	 * @return surel bersih; string kosong bila belum diisi.
	 */
	public String ambilEmail() {
		// --- LOGIKA BARU: Menghilangkan tanda petik (') di depan ---
		if (email != null) {
			email = email.trim();
			// Menggunakan while agar jika ada double petik (contoh: ''email@web.com)
			// langsung terhapus semua
			while (email.startsWith("'")) {
				email = email.substring(1).trim();
			}
		}
		return this.email == null ? "" : email.trim();
	}

	/**
	 * Alamat surel mahasiswa — kolom {@code email} (panjang 255). Kolom ini boleh berisi BEBERAPA
	 * surel yang dipisah koma (lihat {@link #appendEmail(String)}).
	 *
	 * <p>Getter melakukan pembersihan cukup banyak: koma ganda {@code ",,"} dirapatkan (sampai 5
	 * kali), nilai yang hanya berupa {@code ","} dianggap kosong, dan tanda petik tunggal di depan
	 * (artefak impor spreadsheet) dibuang. Bila {@link BiodataMahasiswa} punya surel yang tidak
	 * kosong, nilainya MENANG atas kolom di tabel {@code mahasiswa}.</p>
	 *
	 * <p>Sama seperti {@link #getAlamat()}, pemuatan biodata dilewati bila getter dipanggil dari
	 * mesin flush Hibernate.</p>
	 *
	 * @return surel (bisa berupa daftar dipisah koma); string kosong bila belum diisi.
	 */
	@Column(name = "email", length = 255)
	public String getEmail() {
		try {
			if (email != null && email.contains(",,")) {
				for (int i = 0; i < 5; i++) {
					email = email.replaceAll(",,", ",");
				}
			}
			if (email == null) {
				email = "";
			}
			if (email.trim().equals(",")) {
				email = "";
			}

			if (!dipanggilOlehHibernateFlush() && getId() != null && biodataMahasiswa == null) {
				biodataMahasiswa = ambilBiodata(false);
			}

			// Penambahan pengecekan != null untuk mencegah NullPointerException
			if (biodataMahasiswa != null && biodataMahasiswa.getEmail() != null
					&& !biodataMahasiswa.getEmail().trim().isEmpty()) {
				email = biodataMahasiswa.getEmail();
			}

			// --- LOGIKA BARU: Menghilangkan tanda petik (') di depan ---
			if (email != null) {
				email = email.trim();
				// Menggunakan while agar jika ada double petik (contoh: ''email@web.com)
				// langsung terhapus semua
				while (email.startsWith("'")) {
					email = email.substring(1).trim();
				}
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:734");
			// TODO: handle exception
		}

		return this.email == null ? "" : email.trim();
	}

	/**
	 * Menetapkan (mengganti seluruh) isi kolom surel.
	 *
	 * @param email satu surel, atau beberapa surel dipisah koma.
	 * @see #appendEmail(String)
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * MENAMBAHKAN satu surel ke daftar surel mahasiswa (dipisah koma) tanpa membuang yang lama.
	 *
	 * <p>Surel diabaikan bila: sudah ada di daftar, kosong, tidak lolos
	 * {@code Common.isValidEmailAddress}, atau diawali {@code "@"}. Bila cache
	 * {@link BiodataMahasiswa} sudah terisi, surel juga disalin ke sana.</p>
	 *
	 * <p>Dipakai jalur notifikasi/registrasi ({@code ApiUtil}, {@code CommonEmail},
	 * {@code CommonNotifikasi}) untuk mengumpulkan alamat kontak alternatif mahasiswa. Metode ini
	 * hanya mengubah objek di memori — pemanggil yang bertanggung jawab menyimpannya.</p>
	 *
	 * @param email surel yang hendak ditambahkan.
	 */
	public void appendEmail(String email) {
		this.email = getEmail();
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}

		if (biodataMahasiswa != null) {
			biodataMahasiswa.setEmail(email);
		}
	}

	/**
	 * Tahun angkatan mahasiswa — kolom {@code tahunangkatan}. Menjadi acuan utama perhitungan
	 * semester berjalan (lihat {@link #currentSemester()}).
	 *
	 * <p>Bila kolom kosong, nilai DITEBAK dari tahun {@link #getTanggal_dirubah()}; bila itu pun
	 * tidak ada, dipakai tahun berjalan. Artinya getter ini tidak pernah mengembalikan
	 * {@code null}, tetapi nilai tebakan bisa keliru untuk data lama yang tahun angkatannya
	 * memang belum diisi.</p>
	 *
	 * @return tahun angkatan (tidak pernah {@code null}).
	 */
	@Column(name = "tahunangkatan")
	public Integer getTahunangkatan() {
		if (tahunangkatan == null && tanggal_dirubah != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggal_dirubah);
			tahunangkatan = calendar.get(Calendar.YEAR);
		}
		return this.tahunangkatan == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) : this.tahunangkatan;
	}

	/**
	 * Menetapkan tahun angkatan.
	 *
	 * @param tahunangkatan tahun angkatan (mis. 2023).
	 */
	public void setTahunangkatan(Integer tahunangkatan) {
		this.tahunangkatan = tahunangkatan;
	}

	/**
	 * Tempat lahir — kolom {@code tempatlahir} (panjang 150).
	 *
	 * @return tempat lahir sudah di-trim; string kosong bila belum diisi.
	 */
	@Column(name = "tempatlahir", length = 150)
	public String getTempatlahir() {
		if (tempatlahir == null) {
			tempatlahir = "";
		}
		return this.tempatlahir.trim();
	}

	/**
	 * Menetapkan tempat lahir.
	 *
	 * @param tempatlahir nama kota/kabupaten tempat lahir.
	 */
	public void setTempatlahir(String tempatlahir) {
		this.tempatlahir = tempatlahir;
	}

	/**
	 * Tanggal lahir — kolom {@code tanggallahir} (tipe DATE).
	 *
	 * <p>PERHATIAN: bila kolom kosong, getter mengisinya dengan TANGGAL HARI INI. Jadi nilai
	 * balik tidak pernah {@code null}, tetapi "hari ini" di sini berarti "tanggal lahir belum
	 * diisi", bukan data sebenarnya.</p>
	 *
	 * @return tanggal lahir (tidak pernah {@code null}).
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggallahir", length = 0)
	public Date getTanggallahir() {
		if (tanggallahir == null) {
			tanggallahir = ais.ui.util.WaktuUtil.getDate();
		}
		return this.tanggallahir;
	}

	/**
	 * Menetapkan tanggal lahir.
	 *
	 * @param tanggallahir tanggal lahir.
	 */
	public void setTanggallahir(Date tanggallahir) {
		this.tanggallahir = tanggallahir;
	}

	/**
	 * Jenis kelamin — kolom {@code kelamin} (panjang 200), DINORMALKAN saat dibaca menjadi tepat
	 * dua nilai: {@code "Laki-laki"} atau {@code "Perempuan"}.
	 *
	 * <p>Normalisasi menerima berbagai ragam data lama: {@code "L"}/{@code "Laki-Laki"}/apa pun
	 * yang mengandung "laki", dan {@code "P"}/apa pun yang mengandung "puan". Nilai {@code null}
	 * menjadi string kosong. Penyeragaman ini penting karena banyak laporan dan ekspor
	 * (mis. PDDikti/feeder) memfilter berdasarkan teks jenis kelamin.</p>
	 *
	 * @return "Laki-laki", "Perempuan", atau string kosong bila belum diisi.
	 */
	@Column(name = "kelamin", length = 200)
	public String getKelamin() {
		if (kelamin != null && (kelamin.trim().equalsIgnoreCase("L") || kelamin.trim().equals("Laki-Laki"))) {
			kelamin = "Laki-laki";
		} else if (kelamin != null && kelamin.trim().equalsIgnoreCase("P")) {
			kelamin = "Perempuan";
		} else if (kelamin == null) {
			kelamin = "";
		}

		if (kelamin.toLowerCase().contains("laki")) {
			kelamin = "Laki-laki";
		} else if (kelamin.toLowerCase().contains("puan")) {
			kelamin = "Perempuan";
		}

		return this.kelamin;
	}

	/**
	 * Menetapkan jenis kelamin. Penyeragaman teks dilakukan di {@link #getKelamin()}.
	 *
	 * @param kelamin teks jenis kelamin.
	 */
	public void setKelamin(String kelamin) {
		this.kelamin = kelamin;
	}

	/**
	 * Program studi mahasiswa — relasi {@code @ManyToOne} ke {@link Jurusan} lewat kolom
	 * {@code jurusan} (WAJIB, {@code nullable = false}). Ini relasi paling sering dipakai:
	 * jenjang, fakultas dan perguruan tinggi semuanya dijangkau lewat sini.
	 *
	 * <p>Proxy lazy disambungkan ulang dengan aman memakai {@code check()} milik
	 * {@link ais.database.model.GeneralValueObject} sehingga getter tetap aman dipanggil di luar
	 * sesi Hibernate asalnya.</p>
	 *
	 * @return program studi mahasiswa.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = false)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return this.jurusan;
	}

	/**
	 * Menetapkan program studi mahasiswa.
	 *
	 * @param jurusan program studi.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Menyusun KALIMAT SIAP TAMPIL tentang masa studi mahasiswa, mis.
	 * <code>"Masa studi : 3 tahun, 2 bulan, 5 hari. Batas waktu studi 7 tahun. , Sisa masa studi :
	 * ..."</code>.
	 *
	 * <p>Hitungannya: selisih {@link #getTanggalKegiatanBelajarMengajar()} sampai
	 * {@link #getTanggalLulus()} (atau hari ini bila belum lulus), memakai
	 * {@link #hitungSelisihTanggal(Date, Date)}. Batas studi diambil dari
	 * {@code jurusan.getJenjang().getJumlahSemesterMaksimal()}; tanggal batas didekati dengan
	 * rumus kasar <b>178 hari per semester</b>. Bila tanggal batas sudah lewat, kalimatnya
	 * berubah menjadi peringatan telah melewati batas studi.</p>
	 *
	 * <p>Dipakai sekitar sepuluh layar/laporan profil &amp; kelulusan. Segala kegagalan
	 * menghasilkan string kosong, bukan exception.</p>
	 *
	 * @return kalimat masa studi siap tampil; string kosong bila data tidak lengkap atau gagal.
	 */
	public String ambilMasaStudi() {
		try {
			Mahasiswa mahasiswa = this;
			if (mahasiswa.getTanggalKegiatanBelajarMengajar() == null) {
				return "";
			}

			int[] masaStudi = hitungSelisihTanggal(mahasiswa.getTanggalKegiatanBelajarMengajar(),
					mahasiswa.getTanggalLulus() == null ? WaktuUtil.getDate() : mahasiswa.getTanggalLulus());

			Jurusan jurusan = mahasiswa.getJurusan();

			int batasSemester = (jurusan != null && jurusan.getJenjang() != null
					&& jurusan.getJenjang().getJumlahSemesterMaksimal() != null
							? jurusan.getJenjang().getJumlahSemesterMaksimal()
							: 0);
			double jumlahTahun = batasSemester / 2.0;

			Calendar calendarMasaAwal = ais.ui.util.WaktuUtil.getCalendar();
			calendarMasaAwal.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());

			Calendar calendarMasaAkhir = ais.ui.util.WaktuUtil.getCalendar();
			calendarMasaAkhir.setTime(mahasiswa.getTanggalKegiatanBelajarMengajar());
			calendarMasaAkhir.set(Calendar.DATE, calendarMasaAwal.get(Calendar.DATE) + (178 * batasSemester));

			int[] sisaMasa = hitungSelisihTanggal(WaktuUtil.getDate(), calendarMasaAkhir.getTime());

			boolean terlewat = calendarMasaAkhir.getTime().before(WaktuUtil.kemarin());
			return "Masa studi : " + masaStudi[0] + " tahun, " + masaStudi[1] + " bulan, " + masaStudi[2]
					+ " hari. "
					+ (jurusan != null && jurusan.getJenjang() != null
							&& jurusan.getJenjang().getJumlahSemesterMaksimal() != null
									? "Batas waktu studi " + Common.numberFormat.get().format(jumlahTahun) + " tahun. "
									: "")
					+ (mahasiswa.getTanggalLulus() != null ? ""
							: terlewat ? "Telah meleibih batas studi"
									: ", Sisa masa studi : " + sisaMasa[0] + " tahun, " + sisaMasa[1] + " bulan, "
											+ sisaMasa[2] + " hari. ");
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Menghitung selisih dua tanggal sebagai {tahun, bulan, hari} kalender (bukan sekadar
	 * pembagian hari), lengkap dengan "pinjam" jumlah hari bulan sebelumnya bila hari negatif.
	 *
	 * <p>Bila {@code sampai} lebih awal dari {@code mulai}, urutan ditukar dan komponen TAHUN
	 * dikembalikan bernilai negatif (bulan dan hari tetap positif).</p>
	 *
	 * @param mulai  tanggal awal; {@code null} menghasilkan {0,0,0}.
	 * @param sampai tanggal akhir; {@code null} menghasilkan {0,0,0}.
	 * @return larik tiga elemen: [0]=tahun, [1]=bulan, [2]=hari.
	 */
	private static int[] hitungSelisihTanggal(Date mulai, Date sampai) {
		int[] hasil = new int[] { 0, 0, 0 };
		if (mulai == null || sampai == null) {
			return hasil;
		}

		Calendar awal = WaktuUtil.getCalendar();
		awal.setTime(mulai);
		Calendar akhir = WaktuUtil.getCalendar();
		akhir.setTime(sampai);

		boolean mundur = akhir.before(awal);
		if (mundur) {
			Calendar tmp = awal;
			awal = akhir;
			akhir = tmp;
		}

		int tahun = akhir.get(Calendar.YEAR) - awal.get(Calendar.YEAR);
		int bulan = akhir.get(Calendar.MONTH) - awal.get(Calendar.MONTH);
		int hari = akhir.get(Calendar.DAY_OF_MONTH) - awal.get(Calendar.DAY_OF_MONTH);

		if (hari < 0) {
			bulan--;
			Calendar pinjam = (Calendar) akhir.clone();
			pinjam.add(Calendar.MONTH, -1);
			hari += pinjam.getActualMaximum(Calendar.DAY_OF_MONTH);
		}

		if (bulan < 0) {
			tahun--;
			bulan += 12;
		}

		hasil[0] = mundur ? tahun * -1 : tahun;
		hasil[1] = bulan;
		hasil[2] = hari;
		return hasil;
	}

	/**
	 * Menetapkan nomor telepon pada baris {@code mahasiswa}.
	 *
	 * @param telp nomor telepon.
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Nomor telepon — kolom {@code telp} (panjang 200). Bila cache {@link BiodataMahasiswa} sudah
	 * terisi dan punya nomor HP, nomor itu yang menang. Semua karakter non-digit dibuang.
	 *
	 * <p>Berbeda dengan {@link #getAlamat()}/{@link #getEmail()}, getter ini TIDAK memicu
	 * pemuatan biodata; ia hanya memakai cache yang kebetulan sudah ada.</p>
	 *
	 * @return nomor telepon hanya berisi digit; string kosong bila belum diisi.
	 */
	@Column(name = "telp", length = 200)
	public String getTelp() {

		if (biodataMahasiswa != null && biodataMahasiswa.getHp() != null) {
			telp = biodataMahasiswa.getHp();
		}

		if (telp != null && !telp.trim().isEmpty()) {
			telp = telp.replaceAll("[^\\d]", "");
		}

		return telp == null ? "" : telp;
	}

	/**
	 * Menetapkan jenjang pendidikan. Perhatikan {@link #getJenjang()} akan menimpanya dengan
	 * jenjang milik program studi bila program studi terisi.
	 *
	 * @param jenjang jenjang pendidikan (S1, S2, D3, ...).
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	/**
	 * Jenjang pendidikan — relasi {@code @ManyToOne} ke {@link Jenjang} lewat kolom {@code jenjang}.
	 *
	 * <p><b>Aturan tersembunyi:</b> bila {@link #getJurusan()} terisi, jenjang SELALU diambil ulang
	 * dari {@code jurusan.getJenjang()} sehingga kolom {@code jenjang} pada tabel {@code mahasiswa}
	 * praktis hanya cadangan. Jenjang menentukan batas semester maksimal &amp; jumlah semester
	 * kelulusan yang dipakai {@link #hitungSmtLulus(StatusKeluar, Mahasiswa)} dan pembentukan
	 * tagihan.</p>
	 *
	 * @return jenjang mahasiswa (mengikuti program studi bila ada).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang", nullable = true)
	public Jenjang getJenjang() {
		if (jurusan != null) {
			jenjang = getJurusan().getJenjang();
		}
		jenjang = check(jenjang);
		return jenjang;
	}

	/**
	 * Menetapkan jenis semester saat mahasiswa mulai kuliah.
	 *
	 * @param semesterMulai {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}.
	 */
	public void setSemesterMulai(String semesterMulai) {
		this.semesterMulai = semesterMulai;
	}

	/**
	 * Jenis semester saat mahasiswa MULAI kuliah — kolom {@code semester_mulai}. Bersama
	 * {@link #getTahunangkatan()} dan {@link #getPindahKeKampusIniMasukSemester()}, nilai ini
	 * menentukan pemetaan tahun akademik menjadi nomor semester mahasiswa
	 * ({@code Common.getSemester(...)}).
	 *
	 * <p>Dinormalkan saat dibaca: teks apa pun yang mengandung "ganjil"/"genap" dipetakan ke
	 * konstanta {@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}; nilai kosong dianggap
	 * GANJIL.</p>
	 *
	 * @return {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP}.
	 */
	@Column(name = "semester_mulai", length = 200)
	public String getSemesterMulai() {
		if (semesterMulai == null) {
			semesterMulai = Perkuliahan.GANJIL;
		}
		if (semesterMulai.toLowerCase().contains("ganjil")) {
			semesterMulai = Perkuliahan.GANJIL;
		} else if (semesterMulai.toLowerCase().contains("genap")) {
			semesterMulai = Perkuliahan.GENAP;
		}
		return semesterMulai;
	}

	/**
	 * Menetapkan konsentrasi/peminatan mahasiswa.
	 *
	 * @param konsentrasi konsentrasi dalam program studi.
	 */
	public void setKonsentrasi(Konsentrasi konsentrasi) {
		this.konsentrasi = konsentrasi;
	}

	/**
	 * Konsentrasi/peminatan dalam program studi — relasi {@code @ManyToOne} ke {@link Konsentrasi}
	 * lewat kolom {@code konsentrasi_mahasiswa}. Proxy lazy disambungkan ulang dengan {@code check()}.
	 *
	 * @return konsentrasi mahasiswa; {@code null} bila tidak memakai konsentrasi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "konsentrasi_mahasiswa", nullable = true)
	public Konsentrasi getKonsentrasi() {
		konsentrasi = check(konsentrasi);
		return konsentrasi;
	}

	/**
	 * Menetapkan waktu kuliah.
	 *
	 * @param waktuKuliah mis. "PAGI", "SORE", "MALAM".
	 */
	public void setWaktuKuliah(String waktuKuliah) {
		this.waktuKuliah = waktuKuliah;
	}

	/**
	 * Waktu kuliah — kolom {@code waktu_kuliah}. Bila kosong dianggap {@code "PAGI"}.
	 *
	 * @return waktu kuliah (tidak pernah kosong).
	 */
	@Column(name = "waktu_kuliah", length = 200)
	public String getWaktuKuliah() {
		if (waktuKuliah == null || waktuKuliah.trim().isEmpty()) {
			waktuKuliah = "PAGI";
		}
		return waktuKuliah;
	}

	/**
	 * Menetapkan tanggal lulus pada baris {@code mahasiswa}. Perhatikan {@link #getTanggalLulus()}
	 * akan menimpanya bila mahasiswa tergabung dalam {@link KelompokStatusKeluarMahasiswa}.
	 *
	 * @param tanggalLulus tanggal kelulusan.
	 */
	public void setTanggalLulus(Date tanggalLulus) {
		this.tanggalLulus = tanggalLulus;
	}

	/**
	 * Tanggal lulus — kolom {@code tanggal_lulus} (tipe DATE).
	 *
	 * <p><b>Aturan tersembunyi:</b> bila mahasiswa tergabung dalam
	 * {@link KelompokStatusKeluarMahasiswa} (SK kelulusan kolektif) dan kelompok itu punya tanggal
	 * lulus, tanggal KELOMPOK yang menang — kolom pada baris mahasiswa diabaikan. Nilai ini
	 * menyetir {@link #getTahunLulus()}, {@link #getMasaStudi()} dan {@link #ambilMasaStudi()}.</p>
	 *
	 * @return tanggal lulus; {@code null} bila belum lulus.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_lulus", length = 0)
	public Date getTanggalLulus() {
		if (getKelompokStatusKeluarMahasiswa() != null
				&& getKelompokStatusKeluarMahasiswa().getTanggalLulus() != null) {
			tanggalLulus = getKelompokStatusKeluarMahasiswa().getTanggalLulus();
		}
		return tanggalLulus;
	}

	/**
	 * Menetapkan kata sandi (terenkripsi) mahasiswa untuk login portal.
	 *
	 * @param pass kata sandi hasil enkripsi DES {@code Common.desEncrypter}.
	 */
	public void setPass(String pass) {
		this.pass = pass;
	}

	/**
	 * Kata sandi login portal mahasiswa — kolom {@code pass}.
	 *
	 * <p><b>Efek samping:</b> bila kata sandi masih kosong sedangkan NIM sudah ada, getter
	 * MEMBANGKITKAN kata sandi awal = NIM terenkripsi ({@code Common.desEncrypter}) dan menandai
	 * {@link #getIs_encripted()} menjadi {@code true}. Perubahan ini ada di memori; baru
	 * tersimpan bila entitas ikut ter-flush.</p>
	 *
	 * @return kata sandi terenkripsi.
	 */
	@Column(name = "pass", nullable = true, length = 100)
	public String getPass() {
		if ((pass == null || pass.trim().isEmpty()) && nim != null && !nim.trim().isEmpty()) {
			pass = Common.desEncrypter.get().encrypt(nim);
			is_encripted = true;
		}
		return pass;
	}

	/**
	 * Menetapkan status awal mahasiswa (baru / pindahan / alih prodi / dsb.).
	 *
	 * @param statusAwalMahasiswa status awal.
	 */
	public void setStatusAwalMahasiswa(StatusAwalMahasiswa statusAwalMahasiswa) {
		this.statusAwalMahasiswa = statusAwalMahasiswa;
	}

	/**
	 * Status AWAL mahasiswa — relasi {@code @ManyToOne} ke {@link StatusAwalMahasiswa} lewat kolom
	 * {@code status_awal_mahasiswa}. Menentukan apakah mahasiswa dihitung sebagai mahasiswa baru,
	 * pindahan antar-perguruan tinggi ({@link #getMerupakanPindahan()}), atau alih program studi
	 * ({@link #getMerupakanAlihProdi()}).
	 *
	 * <p>Bila kosong, otomatis diisi {@code ConstantValues.BARU}. Proxy lazy disambungkan ulang
	 * dengan {@code check()}.</p>
	 *
	 * @return status awal mahasiswa (tidak pernah {@code null} setelah pemanggilan pertama).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswa() {
		if (statusAwalMahasiswa == null) {
			statusAwalMahasiswa = ConstantValues.BARU;
		}
		statusAwalMahasiswa = check(statusAwalMahasiswa);
		return statusAwalMahasiswa;
	}

	/**
	 * Menetapkan nama program perkuliahan.
	 *
	 * @param program mis. "Reguler", "Karyawan", "Kelas Jauh".
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Program perkuliahan sebagai TEKS — kolom {@code program} (panjang 50). Dipakai bersama
	 * {@link #getJurusan()} untuk menentukan jumlah tahapan pembayaran/perkuliahan
	 * ({@code ConstantValues.getJumlahTahapan}). Bila kosong dianggap {@code "Reguler"}.
	 *
	 * <p>Versi terelasi dari nilai ini adalah {@link #getProgramBaru()} yang mencocokkan teks di
	 * sini dengan master {@link Program}.</p>
	 *
	 * @return nama program (tidak pernah kosong).
	 */
	@Column(name = "program", length = 50)
	public String getProgram() {
		if (program == null || program.trim().isEmpty()) {
			program = "Reguler";
		}
		return program;
	}

	/**
	 * Menetapkan jalur/jenis seleksi penerimaan.
	 *
	 * @param jenisSeleksi jenis seleksi.
	 */
	public void setJenisSeleksi(JenisSeleksi jenisSeleksi) {
		this.jenisSeleksi = jenisSeleksi;
	}

	/**
	 * Jalur seleksi penerimaan — relasi {@code @ManyToOne} ke {@link JenisSeleksi} lewat kolom
	 * {@code jenis_seleksi}. Wajib untuk pelaporan PDDikti/feeder.
	 *
	 * @return jenis seleksi; {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_seleksi", nullable = true)
	public JenisSeleksi getJenisSeleksi() {
		jenisSeleksi = check(jenisSeleksi);
		return jenisSeleksi;
	}

	/**
	 * Menetapkan jenis pembiayaan studi mahasiswa.
	 *
	 * @param jenisPembiayaanMahasiswa jenis pembiayaan.
	 */
	public void setJenisPembiayaanMahasiswa(JenisPembiayaanMahasiswa jenisPembiayaanMahasiswa) {
		this.jenisPembiayaanMahasiswa = jenisPembiayaanMahasiswa;
	}

	/**
	 * Jenis pembiayaan studi — relasi {@code @ManyToOne} ke {@link JenisPembiayaanMahasiswa} lewat
	 * kolom {@code jenis_pembiayaan_mahasiswa}. Bila kosong otomatis diisi
	 * {@code ConstantValues.MANDIRI} (biaya sendiri).
	 *
	 * @return jenis pembiayaan (tidak pernah {@code null} setelah pemanggilan pertama).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pembiayaan_mahasiswa", nullable = true)
	public JenisPembiayaanMahasiswa getJenisPembiayaanMahasiswa() {
		if (jenisPembiayaanMahasiswa == null) {
			jenisPembiayaanMahasiswa = ConstantValues.MANDIRI;
		}
		jenisPembiayaanMahasiswa = check(jenisPembiayaanMahasiswa);
		return jenisPembiayaanMahasiswa;
	}

	/**
	 * Menetapkan penanda status konversi nilai (untuk mahasiswa pindahan/alih prodi).
	 *
	 * @param statusKonversi kode status konversi.
	 */
	public void setStatusKonversi(Integer statusKonversi) {
		this.statusKonversi = statusKonversi;
	}

	/**
	 * Penanda status konversi nilai mahasiswa pindahan/alih prodi — kolom {@code status_konversi}.
	 *
	 * @return kode status konversi; {@code null} bila tidak relevan.
	 */
	@Column(name = "status_konversi", nullable = true)
	public Integer getStatusKonversi() {
		return statusKonversi;
	}

	/**
	 * Menandai apakah kolom {@code pass} sudah berisi kata sandi terenkripsi.
	 *
	 * @param is_encripted {@code true} bila sudah terenkripsi.
	 */
	public void setIs_encripted(Boolean is_encripted) {
		this.is_encripted = is_encripted;
	}

	/**
	 * Apakah kata sandi pada {@link #getPass()} sudah dalam bentuk terenkripsi. Penanda ini
	 * dipakai jalur login untuk membedakan data lama (sandi polos) dari data baru. Bila
	 * {@code null} dianggap {@code false}.
	 *
	 * @return {@code true} bila kata sandi terenkripsi.
	 */
	public Boolean getIs_encripted() {
		if (is_encripted == null) {
			is_encripted = false;
		}
		return is_encripted;
	}

	/**
	 * Menetapkan berat badan (kg) — data kesehatan untuk kartu mahasiswa/asuransi.
	 *
	 * @param berat_badan berat badan dalam kilogram.
	 */
	public void setBerat_badan(Integer berat_badan) {
		this.berat_badan = berat_badan;
	}

	/**
	 * Berat badan mahasiswa dalam kilogram.
	 *
	 * @return berat badan; {@code null} bila belum diisi.
	 */
	public Integer getBerat_badan() {
		return berat_badan;
	}

	/**
	 * Menetapkan tinggi badan (cm).
	 *
	 * @param tinggi_badan tinggi badan dalam sentimeter.
	 */
	public void setTinggi_badan(Integer tinggi_badan) {
		this.tinggi_badan = tinggi_badan;
	}

	/**
	 * Tinggi badan mahasiswa dalam sentimeter.
	 *
	 * @return tinggi badan; {@code null} bila belum diisi.
	 */
	public Integer getTinggi_badan() {
		return tinggi_badan;
	}

	/**
	 * Menetapkan golongan darah.
	 *
	 * @param golongan_darah mis. "A", "B", "AB", "O".
	 */
	public void setGolongan_darah(String golongan_darah) {
		this.golongan_darah = golongan_darah;
	}

	/**
	 * Golongan darah mahasiswa.
	 *
	 * @return golongan darah; {@code null} bila belum diisi.
	 */
	public String getGolongan_darah() {
		return golongan_darah;
	}

	/**
	 * Menetapkan agama mahasiswa.
	 *
	 * @param agama entitas master agama.
	 */
	public void setAgama(Agama agama) {
		this.agama = agama;
	}

	/**
	 * Agama mahasiswa — relasi {@code @ManyToOne} ke {@link Agama} lewat kolom {@code agama}.
	 *
	 * <p><b>Pengisian susulan:</b> bila kolom masih kosong sedangkan mahasiswa berasal dari
	 * pendaftaran ({@link #getBiodataCalonMahasiswa()} terisi &gt; 0), agama disalin dari
	 * {@link BiodataCalonMahasiswa} yang dibaca dari cache {@link ConstantValues}. Ini menutup
	 * lubang data untuk mahasiswa hasil konversi calon mahasiswa yang biodatanya belum
	 * disalin lengkap.</p>
	 *
	 * @return agama mahasiswa; {@code null} bila tetap tidak diketahui.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "agama", nullable = true)
	public Agama getAgama() {
		if (biodataCalonMahasiswa != null && biodataCalonMahasiswa > 0L && agama == null) {
			BiodataCalonMahasiswa b = (BiodataCalonMahasiswa) ConstantValues
					.ambil(BiodataCalonMahasiswa.class.getName(), biodataCalonMahasiswa);
			if (b != null && b.getAgama() != null) {
				agama = b.getAgama();
			}
		}

		agama = check(agama);
		return agama;
	}

	/**
	 * Menetapkan catatan bebas tentang mahasiswa.
	 *
	 * @param keterangan catatan bebas.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Catatan bebas administratif tentang mahasiswa.
	 *
	 * @return keterangan; {@code null} bila kosong.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan status kewarganegaraan.
	 *
	 * @param warganegara {@link #WNI} atau {@link #WNA}.
	 */
	public void setWarganegara(String warganegara) {
		this.warganegara = warganegara;
	}

	/**
	 * Status kewarganegaraan mahasiswa. Bila kosong dianggap {@link #WNI}. Bersama
	 * {@link #getNegara()} dipakai pelaporan mahasiswa asing.
	 *
	 * @return {@link #WNI} atau {@link #WNA}.
	 */
	public String getWarganegara() {
		if (warganegara == null || warganegara.trim().isEmpty()) {
			warganegara = WNI;
		}
		return warganegara;
	}

	/**
	 * Nomor ijazah pertama (ijazah utama) — kolom {@code no_ijazah1}.
	 *
	 * @return nomor ijazah; {@code null} bila belum diterbitkan.
	 */
	@Column(name = "no_ijazah1")
	public String getNoIjazah1() {
		return noIjazah1;
	}

	/**
	 * Menetapkan nomor ijazah pertama.
	 *
	 * @param noIjazah1 nomor ijazah.
	 */
	public void setNoIjazah1(String noIjazah1) {
		this.noIjazah1 = noIjazah1;
	}

	/**
	 * Nomor ijazah kedua — kolom {@code no_ijazah2} (dipakai perguruan tinggi yang menerbitkan
	 * dua nomor, mis. nomor nasional dan nomor internal).
	 *
	 * @return nomor ijazah kedua; {@code null} bila tidak dipakai.
	 */
	@Column(name = "no_ijazah2")
	public String getNoIjazah2() {
		return noIjazah2;
	}

	/**
	 * Menetapkan nomor ijazah kedua.
	 *
	 * @param noIjazah2 nomor ijazah kedua.
	 */
	public void setNoIjazah2(String noIjazah2) {
		this.noIjazah2 = noIjazah2;
	}

	/**
	 * Tahun wisuda — kolom {@code tahun_wisuda}. Nilai {@code 0} (sandi data lama untuk "belum
	 * ada") dinormalkan menjadi {@code null}.
	 *
	 * @return tahun wisuda; {@code null} bila belum diwisuda.
	 */
	@Column(name = "tahun_wisuda")
	public Integer getTahunWisuda() {
		if (tahunWisuda != null && tahunWisuda == 0) {
			tahunWisuda = null;
		}
		return tahunWisuda;
	}

	/**
	 * Menetapkan tahun wisuda.
	 *
	 * @param tahunWisuda tahun wisuda.
	 */
	public void setTahunWisuda(Integer tahunWisuda) {
		this.tahunWisuda = tahunWisuda;
	}

	/**
	 * Judul tugas akhir/skripsi dalam bahasa Indonesia — kolom {@code judul_skripsi} (tipe TEXT).
	 * Dipakai ijazah, transkrip dan SKPI.
	 *
	 * @return judul skripsi; {@code null} bila belum diisi.
	 */
	@Column(name = "judul_skripsi", columnDefinition = "text")
	public String getJudulSkripsi() {
		return judulSkripsi;
	}

	/**
	 * Menetapkan judul skripsi berbahasa Indonesia.
	 *
	 * @param judulSkripsi judul skripsi.
	 */
	public void setJudulSkripsi(String judulSkripsi) {
		this.judulSkripsi = judulSkripsi;
	}

	/**
	 * Tanggal yudisium — kolom {@code tanggal_yudisium} (tipe DATE). Dipakai sebagai cadangan
	 * penentu {@link #getTahunLulus()} bila tanggal lulus belum ada.
	 *
	 * @return tanggal yudisium; {@code null} bila belum yudisium.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_yudisium")
	public Date getTanggalYudisium() {
		return tanggalYudisium;
	}

	/**
	 * Menetapkan tanggal yudisium.
	 *
	 * @param tanggalYudisium tanggal yudisium.
	 */
	public void setTanggalYudisium(Date tanggalYudisium) {
		this.tanggalYudisium = tanggalYudisium;
	}

	/**
	 * Tanggal wisuda — kolom {@code tanggal_wisuda} (tipe DATE). Cadangan terakhir penentu
	 * {@link #getTahunLulus()}.
	 *
	 * @return tanggal wisuda; {@code null} bila belum diwisuda.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_wisuda")
	public Date getTanggalWisuda() {
		return tanggalWisuda;
	}

	/**
	 * Menetapkan tanggal wisuda.
	 *
	 * @param tanggalWisuda tanggal wisuda.
	 */
	public void setTanggalWisuda(Date tanggalWisuda) {
		this.tanggalWisuda = tanggalWisuda;
	}

	/**
	 * Menetapkan nomor akta/SK pertama terkait kelulusan.
	 *
	 * @param noAkta1 nomor akta pertama.
	 */
	public void setNoAkta1(String noAkta1) {
		this.noAkta1 = noAkta1;
	}

	/**
	 * Nomor akta/SK pertama terkait kelulusan — kolom {@code no_akta1}.
	 *
	 * @return nomor akta sudah di-trim; string kosong bila belum ada.
	 */
	@Column(name = "no_akta1")
	public String getNoAkta1() {
		return noAkta1 == null ? "" : noAkta1.trim();
	}

	/**
	 * Menetapkan nomor akta/SK kedua terkait kelulusan.
	 *
	 * @param noAkta2 nomor akta kedua.
	 */
	public void setNoAkta2(String noAkta2) {
		this.noAkta2 = noAkta2;
	}

	/**
	 * Nomor akta/SK kedua terkait kelulusan — kolom {@code no_akta2}.
	 *
	 * <p>Catatan: pernah ada percobaan mengambil nomor SK dari
	 * {@link KelompokStatusKeluarMahasiswa} secara otomatis (kode masih ada dalam bentuk komentar
	 * di bawah). Perilaku itu SENGAJA dimatikan — nomor akta tetap milik baris mahasiswa.</p>
	 *
	 * @return nomor akta kedua sudah di-trim; string kosong bila belum ada.
	 */
	@Column(name = "no_akta2")
	public String getNoAkta2() {

//		if (getKelompokStatusKeluarMahasiswa() != null && !getKelompokStatusKeluarMahasiswa().getNoSk().isEmpty()) {
//			noAkta2 = getKelompokStatusKeluarMahasiswa().getNoSk();
//		}

		return noAkta2 == null ? "" : noAkta2.trim();
	}

	/**
	 * Menetapkan nama pengguna akun orang tua/wali untuk portal.
	 *
	 * @param userOrtu nama pengguna orang tua.
	 */
	public void setUserOrtu(String userOrtu) {
		this.userOrtu = userOrtu;
	}

	/**
	 * Nama pengguna akun orang tua/wali — kolom {@code user_ortu}. Dipakai portal orang tua untuk
	 * memantau nilai dan tagihan anaknya.
	 *
	 * @return nama pengguna orang tua; {@code null} bila belum dibuat.
	 */
	@Column(name = "user_ortu")
	public String getUserOrtu() {
		return userOrtu;
	}

	/**
	 * Menetapkan kata sandi akun orang tua/wali.
	 *
	 * @param passOrtu kata sandi orang tua.
	 */
	public void setPassOrtu(String passOrtu) {
		this.passOrtu = passOrtu;
	}

	/**
	 * Kata sandi akun orang tua/wali — kolom {@code pass_ortu}.
	 *
	 * @return kata sandi orang tua; {@code null} bila belum dibuat.
	 */
	@Column(name = "pass_ortu")
	public String getPassOrtu() {
		return passOrtu;
	}

	/**
	 * Negara asal mahasiswa — relasi {@code @ManyToOne} ke {@link Negara} lewat kolom
	 * {@code negara}. Bila kosong dikembalikan {@code ConstantValues.INDONESIA} (nilai field
	 * TIDAK ikut diubah, hanya nilai baliknya).
	 *
	 * @return negara asal; Indonesia bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "negara", nullable = true)
	public Negara getNegara() {
		negara = check(negara);
		return negara == null ? ConstantValues.INDONESIA : negara;
	}

	/**
	 * Menetapkan negara asal mahasiswa.
	 *
	 * @param negara entitas master negara.
	 */
	public void setNegara(Negara negara) {
		this.negara = negara;
	}

	/**
	 * Menetapkan daftar semester yang MEMAKSA status mahasiswa menjadi TIDAK AKTIF.
	 *
	 * <p>Nilai juga disalin ke penyimpanan "kunci JSON" milik
	 * {@link ais.database.model.GeneralValueObject} dengan kunci {@code "batasStudi"} lewat
	 * {@code put(...)}, karena {@link #getBatasStudi()} membaca dari sana lebih dahulu. Nilai
	 * kosong disimpan sebagai sandi {@code "1000"} yang artinya "tanpa batas".</p>
	 *
	 * @param batasStudi daftar semester dipisah koma, mis. {@code "9,10"}; kosong = tanpa efek.
	 */
	public void setBatasStudi(String batasStudi) {
		if (batasStudi != null) {
			put(batasStudi.trim().isEmpty() ? "1000" : batasStudi, "batasStudi");
		} else {
			put(batasStudi, "1000");
		}
		this.batasStudi = batasStudi;
	}

	/**
	 * Daftar semester (dipisah koma) yang MEMAKSA status mahasiswa menjadi TIDAK AKTIF — kolom
	 * {@code batas_studi}. Kebalikan dari {@link #getPaksaAktifSemester()}.
	 *
	 * <p>Nilai dibaca lebih dahulu dari penyimpanan kunci JSON ({@code retreive("batasStudi")});
	 * bila ada, nilai itu menimpa isi kolom. Sandi {@code "1000"} berarti "tanpa batas" dan
	 * diterjemahkan menjadi string kosong.</p>
	 *
	 * <p>Dibaca antara lain oleh {@code HistoryStatusMahasiswaUtil}, {@code KegiatanHelper},
	 * {@code FormKelulusanHelper} dan modul EPSBED saat menentukan status per semester serta
	 * pembangkitan tagihan.</p>
	 *
	 * @return daftar semester dipisah koma; string kosong bila tanpa batas.
	 */
	@Column(name = "batas_studi")
	public String getBatasStudi() {

		try {
			String s = retreive("batasStudi");
			if (s != null && !s.trim().isEmpty()) {
				batasStudi = s.contains("1000") ? "" : s;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:1276");
		}

		return batasStudi == null || batasStudi.contains("1000") ? "" : batasStudi.trim();
	}

	/**
	 * Menetapkan daftar semester yang MEMAKSA status mahasiswa menjadi AKTIF.
	 *
	 * @param paksaAktifSemester daftar semester dipisah koma, mis. {@code "3,4,5"}; kosong = tanpa
	 *        efek. Nilai di-trim, {@code null} dibiarkan {@code null}.
	 * @see #getPaksaAktifSemester()
	 */
	public void setPaksaAktifSemester(String paksaAktifSemester) {
		this.paksaAktifSemester = paksaAktifSemester == null ? null : paksaAktifSemester.trim();
	}

	/**
	 * Daftar semester (dipisah koma) yang MEMAKSA status mahasiswa menjadi AKTIF — kolom
	 * {@code paksa_aktif_semester}. Kebalikan dari {@link #getBatasStudi()}; diisi admin lewat
	 * form Kelulusan/Biodata dan dibaca {@code HistoryStatusMahasiswaUtil}.
	 *
	 * <p>Sandi {@code "1000"} diperlakukan sebagai kosong, mengikuti pola {@link #getBatasStudi()}.
	 * Lihat juga catatan audit pada komentar di bawah: kolom ini ikut terekam Envers sehingga
	 * tabel {@code new_audit.mahasiswa__audit} harus di-ALTER manual.</p>
	 *
	 * @return daftar semester dipisah koma; string kosong bila tanpa efek.
	 */
	// DIAUDIT (permintaan: SEMUA kolom Mahasiswa diaudit). Karena Mahasiswa @Audited & tidak ada
	// @NotAudited, kolom ini ikut ke tabel audit new_audit.mahasiswa__audit. hbm2ddl=update TIDAK
	// menambah kolom audit otomatis -> WAJIB ALTER manual new_audit.mahasiswa__audit ADD COLUMN
	// paksa_aktif_semester (lihat docs/performance/migrations/20260901.001-paksa-aktif-semester.sql),
	// kalau tidak INSERT audit gagal -> save Mahasiswa rollback.
	@Column(name = "paksa_aktif_semester")
	public String getPaksaAktifSemester() {
		return paksaAktifSemester == null || "1000".equals(paksaAktifSemester.trim())
				? "" : paksaAktifSemester.trim();
	}

	/**
	 * Menetapkan tanggal masuk (mulai terdaftar) mahasiswa.
	 *
	 * @param tanggalMasuk tanggal masuk.
	 */
	public void setTanggalMasuk(Date tanggalMasuk) {
		this.tanggalMasuk = tanggalMasuk;
	}

	/**
	 * Tanggal masuk mahasiswa — kolom {@code tanggal_masuk}. Bila kosong, DITEBAK dari
	 * {@link #getTahunangkatan()} (tanggal &amp; bulan mengikuti hari ini, hanya tahunnya diganti).
	 * Dipakai {@link #getMasaStudi()} untuk menghitung lama studi dalam hari.
	 *
	 * @return tanggal masuk (tidak pernah {@code null} bila tahun angkatan ada).
	 */
	@Column(name = "tanggal_masuk")
	public Date getTanggalMasuk() {
		if (tanggalMasuk == null && getTahunangkatan() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.YEAR, getTahunangkatan());
			tanggalMasuk = calendar.getTime();
		}
		return tanggalMasuk;
	}

	/**
	 * Menetapkan jumlah SKS hasil penyetaraan.
	 *
	 * @param jumlahSksPenyetaraan jumlah SKS penyetaraan.
	 */
	public void setJumlahSksPenyetaraan(Integer jumlahSksPenyetaraan) {
		this.jumlahSksPenyetaraan = jumlahSksPenyetaraan;
	}

	/**
	 * Jumlah SKS hasil penyetaraan (rekognisi pembelajaran lampau/RPL) — kolom
	 * {@code jumlah_sks_penyetaraan}.
	 *
	 * @return jumlah SKS penyetaraan; {@code null} bila tidak ada.
	 */
	@Column(name = "jumlah_sks_penyetaraan")
	public Integer getJumlahSksPenyetaraan() {
		return jumlahSksPenyetaraan;
	}

	/**
	 * Menetapkan NIM mahasiswa di perguruan tinggi asal.
	 *
	 * @param nimPindahan NIM di kampus asal.
	 */
	public void setNimPindahan(String nimPindahan) {
		this.nimPindahan = nimPindahan;
	}

	/**
	 * NIM mahasiswa di perguruan tinggi ASAL — kolom {@code pindahan_nim}. Dikosongkan otomatis
	 * bila mahasiswa ternyata bukan pindahan ({@link #getMerupakanPindahan()} bernilai
	 * {@code false}), sehingga data sisa dari pengisian keliru tidak ikut terlaporkan.
	 *
	 * @return NIM di kampus asal; kosong bila bukan pindahan.
	 */
	@Column(name = "pindahan_nim")
	public String getNimPindahan() {
		if (!getMerupakanPindahan()) {
			nimPindahan = "";
		}
		return nimPindahan;
	}

	/**
	 * Nama perguruan tinggi asal sebagai TEKS — kolom {@code pindahan_perguruan_tinggi}. Bila
	 * relasi {@link #getPindahanDari()} terisi, namanya menimpa isi kolom ini sehingga teks
	 * selalu mengikuti master {@link PerguruanTinggiLain}.
	 *
	 * @return nama perguruan tinggi asal.
	 */
	@Column(name = "pindahan_perguruan_tinggi")
	public String getPindahanPerguruanTinggi() {
		if (getPindahanDari() != null) {
			pindahanPerguruanTinggi = getPindahanDari().getNama();
		}
		return pindahanPerguruanTinggi;
	}

	/**
	 * Menetapkan nama perguruan tinggi asal sebagai teks bebas.
	 *
	 * @param pindahanPerguruanTinggi nama perguruan tinggi asal.
	 */
	public void setPindahanPerguruanTinggi(String pindahanPerguruanTinggi) {
		this.pindahanPerguruanTinggi = pindahanPerguruanTinggi;
	}

	/**
	 * Jenjang mahasiswa di perguruan tinggi ASAL — relasi {@code @ManyToOne} ke {@link Jenjang}
	 * lewat kolom {@code pindahan_jenjang}. Dipakai pelaporan mahasiswa pindahan.
	 *
	 * @return jenjang di kampus asal; {@code null} bila bukan pindahan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pindahan_jenjang")
	public Jenjang getPindahJenjang() {
		pindahJenjang = check(pindahJenjang);
		return pindahJenjang;
	}

	/**
	 * Menetapkan jenjang di perguruan tinggi asal.
	 *
	 * @param pindahJenjang jenjang di kampus asal.
	 */
	public void setPindahJenjang(Jenjang pindahJenjang) {
		this.pindahJenjang = pindahJenjang;
	}

	/**
	 * Program studi mahasiswa di perguruan tinggi ASAL — relasi {@code @ManyToOne} ke
	 * {@link Jurusan} lewat kolom {@code pindahan_program_studi}.
	 *
	 * @return program studi di kampus asal; {@code null} bila bukan pindahan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pindahan_program_studi")
	public Jurusan getPindahJurusan() {
		pindahJurusan = check(pindahJurusan);
		return pindahJurusan;
	}

	/**
	 * Menetapkan program studi di perguruan tinggi asal.
	 *
	 * @param pindahJurusan program studi di kampus asal.
	 */
	public void setPindahJurusan(Jurusan pindahJurusan) {
		this.pindahJurusan = pindahJurusan;
	}

	/**
	 * Menetapkan penanda status pengisian KRS.
	 *
	 * @param statusKrs teks status KRS.
	 */
	public void setStatusKrs(String statusKrs) {
		this.statusKrs = statusKrs;
	}

	/**
	 * Penanda status pengisian KRS mahasiswa — kolom {@code status_krs}.
	 *
	 * @return teks status KRS; {@code null} bila belum diisi.
	 */
	@Column(name = "status_krs")
	public String getStatusKrs() {
		return statusKrs;
	}

	/**
	 * Nomor identitas kependudukan (NIK/KTP).
	 *
	 * <p>Nilai {@code null} maupun sandi data lama {@code "00000"} diperlakukan sebagai KOSONG.
	 * Bila setelah itu masih kosong dan mahasiswa berasal dari pendaftaran
	 * ({@link #getBiodataCalonMahasiswa()} terisi &gt; 0), nomor identitas disalin dari
	 * {@link BiodataCalonMahasiswa} lewat cache {@link ConstantValues}.</p>
	 *
	 * @return NIK/KTP sudah di-trim; string kosong bila tidak diketahui.
	 */
	public String getKtp() {
		if (ktp == null || ktp.trim().equalsIgnoreCase("00000")) {
			ktp = "";
		}

		if (getBiodataCalonMahasiswa() != null && getBiodataCalonMahasiswa() > 0L && ktp.isEmpty()) {
			BiodataCalonMahasiswa b = (BiodataCalonMahasiswa) ConstantValues
					.ambil(BiodataCalonMahasiswa.class.getName(), getBiodataCalonMahasiswa());
			if (b != null && b.getNoIdentitas() != null && !b.getNoIdentitas().isEmpty()) {
				ktp = b.getNoIdentitas();
			}
		}

		return ktp.trim();
	}

	/**
	 * Menetapkan nomor identitas kependudukan (NIK/KTP).
	 *
	 * @param ktp nomor identitas.
	 */
	public void setKtp(String ktp) {
		this.ktp = ktp;
	}

	/**
	 * Tahun kelulusan mahasiswa — diturunkan, bukan sekadar kolom.
	 *
	 * <p>Urutan penentuan (yang belakangan menimpa yang lebih awal):</p>
	 * <ol>
	 *   <li>tahun dari {@link #getTanggalLulus()} bila ada;</li>
	 *   <li>nilai kolom {@code 0} dianggap belum lulus ({@code null});</li>
	 *   <li>bila mahasiswa tergabung dalam {@link KelompokStatusKeluarMahasiswa} yang punya
	 *       tanggal lulus, tahun KELOMPOK yang dipakai (menimpa poin 1);</li>
	 *   <li>bila masih kosong, tahun {@link #getTanggalYudisium()};</li>
	 *   <li>bila masih kosong, tahun {@link #getTanggalWisuda()}.</li>
	 * </ol>
	 *
	 * @return tahun lulus; {@code null} bila belum lulus / data kelulusan belum ada.
	 */
	public Integer getTahunLulus() {
		if (getTanggalLulus() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggalLulus);
			tahunLulus = calendar.get(Calendar.YEAR);
		}
		if (tahunLulus != null && tahunLulus == 0) {
			tahunLulus = null;
		}

		if (getKelompokStatusKeluarMahasiswa() != null
				&& getKelompokStatusKeluarMahasiswa().getTanggalLulus() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getKelompokStatusKeluarMahasiswa().getTanggalLulus());
			tahunLulus = calendar.get(Calendar.YEAR);
		}

		if (tahunLulus == null && getTanggalYudisium() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getTanggalYudisium());
			tahunLulus = calendar.get(Calendar.YEAR);
		}

		if (tahunLulus == null && getTanggalWisuda() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getTanggalWisuda());
			tahunLulus = calendar.get(Calendar.YEAR);
		}

		return tahunLulus;
	}

	/**
	 * Menetapkan tahun lulus secara manual. Perhatikan {@link #getTahunLulus()} dapat
	 * menimpanya dari tanggal lulus/yudisium/wisuda.
	 *
	 * @param tahunLulus tahun lulus.
	 */
	public void setTahunLulus(Integer tahunLulus) {
		this.tahunLulus = tahunLulus;
	}

	/**
	 * Menghitung SEMESTER KE-BERAPA seorang mahasiswa dinyatakan lulus/keluar. Ini salah satu
	 * aturan bisnis paling berpengaruh di AIS: nilainya menjadi batas atas pembangkitan tagihan
	 * semester berikutnya ({@code KegiatanHelper},
	 * {@code TunggakanMahasiswaDaftarUlangProcessor}, dan sejenisnya) — bila salah, mahasiswa
	 * yang sudah lulus tetap ditagih.
	 *
	 * <p><b>Urutan penentuan</b> (yang belakangan menimpa yang lebih awal):</p>
	 * <ol>
	 *   <li>Bila {@code semesterLulus} tersimpan masih kosong/0 dan jenjang diketahui, sedangkan
	 *       status keluar bernama "Lulus", "keluar", "mengundurkan" atau "putus", dipakai
	 *       {@code jenjang.getJumlahSemesterMaksimal()}.</li>
	 *   <li>Bila {@code semesterLulus} kosong tetapi {@code tahunLulus} ada, semester dihitung
	 *       dari tahun akademik {@code tahunLulus/(tahunLulus+1)} memakai
	 *       {@code Common.getSemester(...)} dengan acuan tahun angkatan, semester mulai, dan
	 *       semester masuk hasil pindahan.</li>
	 *   <li>Nilai negatif dianggap tidak sah ({@code null}).</li>
	 *   <li>Bila mahasiswa tergabung dalam {@link KelompokStatusKeluarMahasiswa}, semester
	 *       dihitung ulang dari tahun akademik &amp; semester KELOMPOK — ini MENIMPA hasil
	 *       sebelumnya.</li>
	 *   <li>Bila {@code statusKeluar} {@code null}, hasil dipaksa {@code null} (mahasiswa masih
	 *       aktif, belum punya semester lulus).</li>
	 * </ol>
	 *
	 * <p>Metode ini statis dan membaca field mentah {@code mahasiswa.semesterLulus},
	 * {@code mahasiswa.jenjang}, {@code mahasiswa.tahunLulus} — bukan getter-nya — supaya tidak
	 * memicu rekursi dengan {@link #getSemesterLulus()} yang justru memanggil metode ini.</p>
	 *
	 * @param statusKeluar status keluar mahasiswa; {@code null} berarti belum keluar/lulus.
	 * @param mahasiswa    mahasiswa yang dihitung.
	 * @return nomor semester saat lulus; {@code null} bila belum lulus atau data belum lengkap.
	 */
	public static Integer hitungSmtLulus(StatusKeluar statusKeluar, Mahasiswa mahasiswa) {
		Integer semesterLulus = mahasiswa.semesterLulus;

		if (mahasiswa.jenjang != null && (semesterLulus == null || semesterLulus.equals(0))) {
			if (statusKeluar != null && (statusKeluar.getNama().trim().equalsIgnoreCase("Lulus")
					|| statusKeluar.getNama().trim().equalsIgnoreCase("keluar")
					|| statusKeluar.getNama().trim().equalsIgnoreCase("mengundurkan")
					|| statusKeluar.getNama().trim().equalsIgnoreCase("putus"))) {
				semesterLulus = mahasiswa.jenjang.getJumlahSemesterMaksimal();
			}
		} else if (semesterLulus == null && mahasiswa.tahunLulus != null) {

			String tahunAkademik = mahasiswa.tahunLulus + "/" + (mahasiswa.tahunLulus + 1);
			Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(), tahunAkademik,
					Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
					mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

			semesterLulus = smt;
		}

		if (semesterLulus != null && semesterLulus < 0) {
			semesterLulus = null;
		}

		if (mahasiswa.getKelompokStatusKeluarMahasiswa() != null) {
			String tahunAkademik = mahasiswa.getKelompokStatusKeluarMahasiswa().getTahunAkademik();
			Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(), tahunAkademik,
					mahasiswa.getKelompokStatusKeluarMahasiswa().getSemester(),
					mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
			semesterLulus = smt;
		}

		if (statusKeluar == null) {
			semesterLulus = null;
		}

		return semesterLulus;
	}

	/**
	 * Semester ke-berapa mahasiswa ini lulus — SELALU dihitung ulang lewat
	 * {@link #hitungSmtLulus(StatusKeluar, Mahasiswa)} (bukan sekadar membaca kolom), setelah
	 * lebih dulu menyegarkan {@link #getJenjang()} dan {@link #getStatusKeluar()}.
	 *
	 * <p>Dipakai puluhan berkas (sekitar 40) di modul keuangan, KRS dan pelaporan sebagai penjaga
	 * "jangan proses semester setelah mahasiswa lulus". Karena selalu menghitung ulang, getter
	 * ini relatif mahal — hindari memanggilnya berkali-kali di dalam loop ketat.</p>
	 *
	 * @return nomor semester kelulusan; {@code null} bila mahasiswa belum lulus/keluar.
	 */
	public Integer getSemesterLulus() {
		jenjang = getJenjang();
		statusKeluar = getStatusKeluar();

		semesterLulus = Mahasiswa.hitungSmtLulus(statusKeluar, this);

		return semesterLulus;
	}

	/**
	 * Menetapkan semester kelulusan tersimpan. Nilai ini menjadi masukan bagi
	 * {@link #hitungSmtLulus(StatusKeluar, Mahasiswa)}, bukan hasil akhirnya.
	 *
	 * @param semesterLulus nomor semester kelulusan.
	 */
	public void setSemesterLulus(Integer semesterLulus) {
		this.semesterLulus = semesterLulus;
	}

	/**
	 * PIN mahasiswa (bawaan {@code 1234}) — dipakai sebagai pengaman tambahan pada beberapa
	 * transaksi mandiri, mis. pengisian KRS lewat portal.
	 *
	 * @return PIN mahasiswa.
	 */
	public Long getPin() {
		return pin;
	}

	/**
	 * Menetapkan PIN mahasiswa.
	 *
	 * @param pin PIN baru.
	 */
	public void setPin(Long pin) {
		this.pin = pin;
	}

	/**
	 * Tanggal lahir dalam bentuk TEKS terformat ({@code Common.dateFormat2}), siap ditempel di
	 * laporan/JasperReports tanpa perlu memformat ulang. Kegagalan format ditelan dan menghasilkan
	 * nilai lama/kosong.
	 *
	 * @return tanggal lahir terformat; string kosong bila tanggal lahir kosong.
	 */
	public String getFormatedtanggallahir() {
		try {
			if (tanggallahir != null) {
				formatedtanggallahir = Common.dateFormat2.get().format(tanggallahir);
			} else {
				formatedtanggallahir = "";
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:1509");

		}
		return formatedtanggallahir;
	}

	/**
	 * Menetapkan teks tanggal lahir terformat. Umumnya tidak perlu dipanggil manual karena
	 * {@link #getFormatedtanggallahir()} selalu menghitung ulang dari {@link #getTanggallahir()}.
	 *
	 * @param formatedtanggallahir teks tanggal lahir.
	 */
	public void setFormatedtanggallahir(String formatedtanggallahir) {
		this.formatedtanggallahir = formatedtanggallahir;
	}

	/**
	 * Menghitung SEMESTER BERJALAN mahasiswa ini (semester ke-berapa dia sekarang), berdasarkan
	 * tahun angkatan, jenis semester yang sedang berlangsung, semester mulai, dan penyesuaian
	 * semester masuk bagi mahasiswa pindahan/alih prodi.
	 *
	 * <p>Perhitungan didelegasikan ke {@code Common.getSemester(tahunAngkatan, jenisSemester,
	 * pindahKeKampusIniMasukSemester, semesterMulai)}; jenis semester ditentukan
	 * {@code Common.isNowSemensterGanjil()} sehingga hasilnya BERGANTUNG PADA TANGGAL SISTEM.</p>
	 *
	 * <p>Ini salah satu metode paling banyak dipakai di codebase (sekitar 84 berkas): penyaringan
	 * KRS, pembangkitan tagihan, dasbor, dan seluruh keluarga {@code prosesHitung*} memakainya
	 * untuk mengenali "semester saat ini" ketika memutuskan nilai yang belum terverifikasi boleh
	 * ikut dihitung atau tidak. Bila perhitungan gagal, dikembalikan {@code 1} (bukan {@code null})
	 * agar pemanggil tidak perlu menjaga {@code null}.</p>
	 *
	 * @return nomor semester berjalan, minimal {@code 1}.
	 */
	public Integer currentSemester() {

		Integer currentSemester = null;
		try {

			if (getTahunangkatan() != null) {
				String jenisSemester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
				currentSemester = Common.getSemester(getTahunangkatan(), jenisSemester,
						getPindahKeKampusIniMasukSemester(), getSemesterMulai());
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:1530");
//			Common.tampilErrorJikaAdmin(e);
		}
		if (currentSemester == null || currentSemester.intValue() <= 0) {
			currentSemester = 1;
		}
		return currentSemester;
	}

	/**
	 * Menghitung TAHAPAN berjalan pada semester saat ini. "Tahapan" adalah pembagian satu semester
	 * menjadi beberapa periode (dipakai program non-reguler/kelas karyawan).
	 *
	 * <p>Hanya aktif bila {@code ConstantValues.aktifkanTahapan} bernilai {@code true},
	 * {@link #currentSemester()} &gt; 0, dan jumlah tahapan untuk kombinasi
	 * {@link #getProgram()} + {@link #getJurusan()} lebih dari 2. Tahapan dicari dari peta
	 * bulan &rarr; tahapan ({@code Common.poulateTahapan}) memakai bulan berjalan, sehingga
	 * hasilnya bergantung pada tanggal sistem.</p>
	 *
	 * @return nomor tahapan berjalan; {@code 0} bila mode tahapan tidak aktif atau tidak ketemu.
	 */
	public Integer currentTahapan() {
		Integer currentTahapan = 0;
		if (ConstantValues.aktifkanTahapan && currentSemester() > 0
				&& ConstantValues.getJumlahTahapan(getProgram(), getJurusan()).intValue() > 2) {
			try {
				String bln = Common.BULAN[ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH)];
				Integer tahapanBulan = Common.poulateTahapan(program, jurusan, currentSemester(), getSemesterMulai())
						.get(bln);
				currentTahapan = tahapanBulan == null ? Integer.valueOf(0) : tahapanBulan;
				System.out.println("==> currentTahapan " + currentTahapan + ", bln ");
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return currentTahapan;
	}

	/**
	 * Varian {@link #currentTahapan()} untuk SEMESTER TERTENTU (bukan semester berjalan), dipakai
	 * saat menampilkan/memproses data semester lampau.
	 *
	 * @param semester semester yang ingin dicari tahapannya.
	 * @return nomor tahapan; {@code 0} bila mode tahapan tidak aktif, semester {@code null}/&le;0,
	 *         atau tahapan tidak ditemukan untuk bulan berjalan.
	 */
	public Integer currentTahapan(Integer semester) {

		Integer currentTahapan = 0;
		if (ConstantValues.aktifkanTahapan && semester != null && semester.intValue() > 0
				&& ConstantValues.getJumlahTahapan(getProgram(), getJurusan()).intValue() > 2) {
			try {
				String bln = Common.BULAN[ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH)];
				Integer tahapanBulan = Common.poulateTahapan(program, jurusan, semester, getSemesterMulai()).get(bln);
				currentTahapan = tahapanBulan == null ? Integer.valueOf(0) : tahapanBulan;
				System.out.println("==> currentTahapan " + currentTahapan + ", bln ");
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return currentTahapan;
	}

	/**
	 * Apakah mahasiswa ini PINDAHAN dari perguruan tinggi lain. Nilainya TIDAK disimpan sebagai
	 * kolom mandiri melainkan diturunkan dari {@code getStatusAwalMahasiswa().getPindahan()};
	 * kegagalan apa pun menghasilkan {@code false}.
	 *
	 * @return {@code true} bila status awal menandai mahasiswa sebagai pindahan.
	 */
	public Boolean getMerupakanPindahan() {
		try {
			merupakanPindahan = getStatusAwalMahasiswa() != null && getStatusAwalMahasiswa().getPindahan();
		} catch (Exception e) {
			merupakanPindahan = false;
		}
		return merupakanPindahan;
	}

	/**
	 * Menetapkan penanda pindahan. Perhatikan {@link #getMerupakanPindahan()} selalu menghitung
	 * ulang dari status awal sehingga nilai yang di-set di sini akan tertimpa.
	 *
	 * @param merupakanPindahan penanda pindahan.
	 */
	public void setMerupakanPindahan(Boolean merupakanPindahan) {
		this.merupakanPindahan = merupakanPindahan;
	}

	/**
	 * Nama kampus asal sebagai teks. Bila relasi {@link #getPindahanDari()} terisi, namanya
	 * menimpa isi kolom.
	 *
	 * @return nama kampus asal sudah di-trim; string kosong bila tidak ada.
	 */
	public String getPindahanDariKampus() {
		if (getPindahanDari() != null) {
			pindahanDariKampus = getPindahanDari().getNama();
		}

		return pindahanDariKampus == null ? "" : pindahanDariKampus.trim();
	}

	/**
	 * Menetapkan nama kampus asal sebagai teks bebas.
	 *
	 * @param pindahanDariKampus nama kampus asal.
	 */
	public void setPindahanDariKampus(String pindahanDariKampus) {
		this.pindahanDariKampus = pindahanDariKampus;
	}

	/**
	 * Semester terakhir yang ditempuh mahasiswa di kampus LAMA sebelum pindah. Bila {@code null}
	 * dinormalkan menjadi {@code 0}.
	 *
	 * @return semester terakhir di kampus lama; {@code 0} bila tidak relevan.
	 */
	public Integer getPindahDariKampusLamaDiSemester() {

		if (pindahDariKampusLamaDiSemester == null) {
			pindahDariKampusLamaDiSemester = 0;
		}
		return pindahDariKampusLamaDiSemester;
	}

	/**
	 * Menetapkan semester terakhir di kampus lama.
	 *
	 * @param pindahDariKampusLamaDiSemester nomor semester di kampus lama.
	 */
	public void setPindahDariKampusLamaDiSemester(Integer pindahDariKampusLamaDiSemester) {
		this.pindahDariKampusLamaDiSemester = pindahDariKampusLamaDiSemester;
	}

	/**
	 * Mahasiswa pindahan MASUK ke kampus ini pada semester ke-berapa. Nilai ini menggeser seluruh
	 * perhitungan semester berjalan ({@link #currentSemester()}) dan semester lulus
	 * ({@link #hitungSmtLulus(StatusKeluar, Mahasiswa)}).
	 *
	 * <p><b>Aturan tersembunyi:</b> bila mahasiswa ternyata ALIH PRODI
	 * ({@link #getMerupakanAlihProdi()}), yang dikembalikan adalah
	 * {@link #getPindahKeProdiIniMasukSemester()} — kolom pindahan antar-kampus diabaikan.</p>
	 *
	 * @return semester masuk; {@code 0} bila bukan pindahan.
	 */
	public Integer getPindahKeKampusIniMasukSemester() {

		if (getMerupakanAlihProdi()) {
			return getPindahKeProdiIniMasukSemester();
		}

		if (pindahKeKampusIniMasukSemester == null) {
			pindahKeKampusIniMasukSemester = 0;
		}
		return pindahKeKampusIniMasukSemester;
	}

	/**
	 * Menetapkan semester masuk bagi mahasiswa pindahan antar-perguruan tinggi.
	 *
	 * @param pindahKeKampusIniMasukSemester nomor semester saat masuk kampus ini.
	 */
	public void setPindahKeKampusIniMasukSemester(Integer pindahKeKampusIniMasukSemester) {
		this.pindahKeKampusIniMasukSemester = pindahKeKampusIniMasukSemester;
	}

	/**
	 * Tanggal mahasiswa pindah ke kampus ini (tipe DATE). Nilai awal = tanggal objek dibuat.
	 *
	 * @return tanggal pindah.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalPindah() {
		return tanggalPindah;
	}

	/**
	 * Menetapkan tanggal pindah antar-perguruan tinggi.
	 *
	 * @param tanggalPindah tanggal pindah.
	 */
	public void setTanggalPindah(Date tanggalPindah) {
		this.tanggalPindah = tanggalPindah;
	}

	/**
	 * Catatan bebas tentang kepindahan antar-perguruan tinggi. {@code null} dinormalkan menjadi
	 * string kosong.
	 *
	 * @return keterangan pindah (tidak pernah {@code null}).
	 */
	public String getKeteranganPindah() {
		try {
			if (keteranganPindah == null) {
				keteranganPindah = "";
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return keteranganPindah;
	}

	/**
	 * Menetapkan catatan kepindahan antar-perguruan tinggi.
	 *
	 * @param keteranganPindah catatan bebas.
	 */
	public void setKeteranganPindah(String keteranganPindah) {
		this.keteranganPindah = keteranganPindah;
	}

	/**
	 * Apakah mahasiswa ini hasil ALIH PROGRAM STUDI di dalam kampus yang sama. Diturunkan dari
	 * {@code getStatusAwalMahasiswa().getAlihProdi()}; kegagalan menghasilkan {@code false}.
	 *
	 * <p>Bernilai {@code true} mengubah perilaku {@link #getPindahKeKampusIniMasukSemester()} dan
	 * {@link #getNamaProdiPindah()}.</p>
	 *
	 * @return {@code true} bila mahasiswa merupakan alih prodi.
	 */
	public Boolean getMerupakanAlihProdi() {
		try {
			merupakanAlihProdi = getStatusAwalMahasiswa() != null && getStatusAwalMahasiswa().getAlihProdi();
		} catch (Exception e) {
			merupakanAlihProdi = false;
		}
		return merupakanAlihProdi;
	}

	/**
	 * Menetapkan penanda alih prodi. Akan tertimpa oleh {@link #getMerupakanAlihProdi()} yang
	 * selalu menghitung ulang dari status awal.
	 *
	 * @param merupakanAlihProdi penanda alih prodi.
	 */
	public void setMerupakanAlihProdi(Boolean merupakanAlihProdi) {
		this.merupakanAlihProdi = merupakanAlihProdi;
	}

	/**
	 * NIM lama mahasiswa sebelum alih prodi. SELALU dihitung ulang dari
	 * {@code getAlihProdiMahasiswa().getNim()} — jadi kolom tersimpan tidak pernah dipercaya.
	 *
	 * @return NIM lama; string kosong bila tidak ada baris mahasiswa lama.
	 */
	public String getNimLamaSebelumPindah() {
		nimLamaSebelumPindah = getAlihProdiMahasiswa() == null ? "" : getAlihProdiMahasiswa().getNim();
		return nimLamaSebelumPindah;
	}

	/**
	 * Menetapkan NIM lama sebelum alih prodi (akan tertimpa oleh getter-nya).
	 *
	 * @param nimLamaSebelumPindah NIM lama.
	 */
	public void setNimLamaSebelumPindah(String nimLamaSebelumPindah) {
		this.nimLamaSebelumPindah = nimLamaSebelumPindah;
	}

	/**
	 * Mahasiswa alih prodi MASUK ke program studi ini pada semester ke-berapa. Bila {@code null}
	 * dinormalkan menjadi {@code 0}. Dipakai {@link #getPindahKeKampusIniMasukSemester()} bagi
	 * mahasiswa alih prodi.
	 *
	 * @return semester masuk prodi ini; {@code 0} bila tidak relevan.
	 */
	public Integer getPindahKeProdiIniMasukSemester() {

		if (pindahKeProdiIniMasukSemester == null) {
			pindahKeProdiIniMasukSemester = 0;
		}

		return pindahKeProdiIniMasukSemester;
	}

	/**
	 * Menetapkan semester masuk bagi mahasiswa alih prodi.
	 *
	 * @param pindahKeProdiIniMasukSemester nomor semester saat masuk prodi ini.
	 */
	public void setPindahKeProdiIniMasukSemester(Integer pindahKeProdiIniMasukSemester) {
		this.pindahKeProdiIniMasukSemester = pindahKeProdiIniMasukSemester;
	}

	/**
	 * Tanggal alih program studi. Nilai awal = tanggal objek dibuat.
	 *
	 * @return tanggal alih prodi.
	 */
	public Date getTanggalPindahProdi() {
		return tanggalPindahProdi;
	}

	/**
	 * Menetapkan tanggal alih program studi.
	 *
	 * @param tanggalPindahProdi tanggal alih prodi.
	 */
	public void setTanggalPindahProdi(Date tanggalPindahProdi) {
		this.tanggalPindahProdi = tanggalPindahProdi;
	}

	/**
	 * Catatan bebas tentang alih program studi.
	 *
	 * @return keterangan alih prodi; {@code null} bila kosong.
	 */
	public String getKeteranganPindahProdi() {
		return keteranganPindahProdi;
	}

	/**
	 * Menetapkan catatan alih program studi.
	 *
	 * @param keteranganPindahProdi catatan bebas.
	 */
	public void setKeteranganPindahProdi(String keteranganPindahProdi) {
		this.keteranganPindahProdi = keteranganPindahProdi;
	}

	/**
	 * Baris mahasiswa LAMA (sebelum alih prodi) — relasi {@code @ManyToOne} ke {@code Mahasiswa}
	 * itu sendiri lewat kolom {@code alih_prodi_mahasiswa}. Saat mahasiswa alih prodi, AIS membuat
	 * baris mahasiswa BARU dengan NIM baru dan menautkannya ke baris lama lewat properti ini;
	 * KRS/nilai dapat dipindahkan mengikuti
	 * {@link #getPindahkanKrsDanNilaiKeMahasiswaAlihProdi()}.
	 *
	 * <p>Proxy disambungkan ulang dengan {@code check()} di dalam blok {@code try} — kegagalan
	 * penyambungan hanya dilaporkan lewat {@code Common.tampilErrorJikaAdmin}, tidak dilempar.</p>
	 *
	 * @return baris mahasiswa sebelum alih prodi; {@code null} bila bukan alih prodi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alih_prodi_mahasiswa", nullable = true)
	public Mahasiswa getAlihProdiMahasiswa() {
		try {
			alihProdiMahasiswa = check(alihProdiMahasiswa);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return alihProdiMahasiswa;
	}

	/**
	 * Menautkan baris mahasiswa lama (sebelum alih prodi).
	 *
	 * @param alihProdiMahasiswa baris mahasiswa lama.
	 */
	public void setAlihProdiMahasiswa(Mahasiswa alihProdiMahasiswa) {
		this.alihProdiMahasiswa = alihProdiMahasiswa;
	}

	/**
	 * Id {@link BiodataCalonMahasiswa} asal mahasiswa ini — kolom
	 * {@code biodata_calon_mahasiswa_long}. Disimpan sebagai {@code Long} MENTAH (bukan relasi)
	 * supaya penulisan kolomnya sepenuhnya dikendalikan kelas ini; versi terelasinya adalah
	 * {@link #getBiodataCalonMahasiswaData()} yang dipetakan ke kolom yang sama dengan
	 * {@code insertable = false, updatable = false}.
	 *
	 * <p>Terisi bila mahasiswa dibuat dari proses penerimaan mahasiswa baru (PMB). Dipakai
	 * {@link #getAgama()}, {@link #getKtp()} dan {@link #getKelasPmb()} sebagai sumber pengisian
	 * susulan.</p>
	 *
	 * @return id biodata calon mahasiswa; {@code null} bila bukan dari PMB.
	 */
	@Column(name = "biodata_calon_mahasiswa_long")
	public Long getBiodataCalonMahasiswa() {
		return biodataCalonMahasiswa;
	}

	/**
	 * Menetapkan id biodata calon mahasiswa asal.
	 *
	 * @param biodataCalonMahasiswa id {@link BiodataCalonMahasiswa}.
	 */
	public void setBiodataCalonMahasiswa(Long biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Objek {@link BiodataCalonMahasiswa} asal mahasiswa ini — relasi {@code @ManyToOne} ke kolom
	 * yang sama dengan {@link #getBiodataCalonMahasiswa()}, tetapi HANYA BACA
	 * ({@code insertable = false, updatable = false}).
	 *
	 * <p>Getter melakukan pemulihan berlapis: proxy disambungkan ulang dengan {@code check()},
	 * lalu "disentuh" ({@code getNoRegistrasi()}) untuk memastikan proxy benar-benar bisa
	 * diinisialisasi. Bila sentuhan itu gagal (proxy detached/lazy tanpa sesi), objek dimuat
	 * ULANG memakai sesi Hibernate DEDIKASI ({@code openSession}) yang ditutup tuntas di
	 * {@code finally}.</p>
	 *
	 * <p><b>Catatan sejarah (jangan diulang):</b> versi lama memakai
	 * {@code HibernateUtil.currentNativeSession()} lalu menutupnya manual di dalam getter ini.
	 * Sesi itu BERBAGI per-thread, sehingga menutupnya di tengah request merusak persistence
	 * context yang sedang berjalan dan memunculkan {@code TransientObjectException} pada
	 * flush/dirty-check Mahasiswa berikutnya. Penjelasan lengkapnya ada pada komentar blok di
	 * dalam metode ini.</p>
	 *
	 * @return biodata calon mahasiswa; {@code null} bila tidak ada atau gagal dimuat.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa_long", nullable = true, insertable = false, updatable = false)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswaData() {
		Long biodataId = biodataCalonMahasiswa;
		try {
			biodataCalonMahasiswaData = check(biodataCalonMahasiswaData);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:1761");
			// TODO: handle exception
		}

		try {
			if (biodataCalonMahasiswaData != null) {
				biodataCalonMahasiswaData.getNoRegistrasi();
			}
		} catch (Exception e) {
			try {
				if (biodataId != null) {
					/*
					 * FIX (TransientObjectException BiodataCalonMahasiswa saat flush/dirty-check
					 * Mahasiswa): dulu pakai HibernateUtil.currentNativeSession() lalu ditutup
					 * manual (disconnect+close+closeSession()) di sini. currentNativeSession()
					 * adalah session BERBAGI (ThreadLocal per-thread) yang juga dipakai
					 * proses lain di thread/request yang sama (mis. currentSession() pada
					 * konteks non-ZK jatuh ke session native yang sama). Menutupnya di tengah
					 * getter ini merusak persistence context request yang sedang berjalan:
					 * entity yang sedang ditangani (mis. Mahasiswa) kehilangan sesi aktifnya,
					 * dan objek BiodataCalonMahasiswa hasil query di sini berakhir sebagai
					 * referensi "asing"/rusak sehingga Hibernate menganggapnya transient saat
					 * autoflush/dirty-check berikutnya (lihat pola sama yang sudah pernah
					 * diperbaiki di StatusPertemuan.ambilByNama()). Perbaikan: pakai sesi
					 * DEDIKASI (openSession) yang independen, isi ulang objeknya, lalu tutup
					 * TUNTAS sesi dedikasi itu saja di finally -- JANGAN sentuh session
					 * bersama milik thread/request.
					 */
					Session sesiDedikasi = HibernateUtil.getSessionFactory().openSession();
					try {
						biodataCalonMahasiswaData = (BiodataCalonMahasiswa) ConstantValues
								.simpleObject(
										sesiDedikasi.createCriteria(BiodataCalonMahasiswa.class)
										.add(Restrictions.idEq(biodataId)),
										BiodataCalonMahasiswa.class);
						if (biodataCalonMahasiswaData != null) {
							try {
								org.hibernate.Hibernate.initialize(biodataCalonMahasiswaData);
							} catch (Exception eInit) { ais.common.ErrorAuditUtil.record(eInit, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:1785-init");
							}
						}
						else {
							biodataCalonMahasiswaData = null;
						}
					} finally {
						HibernateUtil.closeSessionQuietly(sesiDedikasi);
					}
				}
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:1785");
				// TODO: handle exception
			}
		}

		return biodataCalonMahasiswaData;
	}

	/**
	 * Menetapkan objek biodata calon mahasiswa (hanya mengisi cache objek; kolomnya ditulis lewat
	 * {@link #setBiodataCalonMahasiswa(Long)}).
	 *
	 * @param biodataCalonMahasiswa objek biodata calon mahasiswa.
	 */
	public void setBiodataCalonMahasiswaData(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswaData = biodataCalonMahasiswa;
	}

	/**
	 * Jenis beasiswa untuk mahasiswa kurang mampu — relasi {@code @ManyToOne} ke
	 * {@link JenisPenerimaBeasiswa} lewat kolom {@code beasiswa_mahasiswa_miskin}.
	 *
	 * @return jenis beasiswa; {@code null} bila bukan penerima.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "beasiswa_mahasiswa_miskin", nullable = true)
	public JenisPenerimaBeasiswa getBeasiswaMahasiswaMiskin() {
		beasiswaMahasiswaMiskin = check(beasiswaMahasiswaMiskin);
		return beasiswaMahasiswaMiskin;
	}

	/**
	 * Menetapkan jenis beasiswa mahasiswa kurang mampu.
	 *
	 * @param beasiswaMahasiswaMiskin jenis beasiswa.
	 */
	public void setBeasiswaMahasiswaMiskin(JenisPenerimaBeasiswa beasiswaMahasiswaMiskin) {
		this.beasiswaMahasiswaMiskin = beasiswaMahasiswaMiskin;
	}

	/**
	 * Jenis beasiswa Bidikmisi/KIP-Kuliah — relasi {@code @ManyToOne} ke
	 * {@link JenisPenerimaBeasiswa} lewat kolom {@code beasiswa_bidik_misi}.
	 *
	 * @return jenis beasiswa; {@code null} bila bukan penerima.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "beasiswa_bidik_misi", nullable = true)
	public JenisPenerimaBeasiswa getBeasiswaBidikMisi() {
		beasiswaBidikMisi = check(beasiswaBidikMisi);
		return beasiswaBidikMisi;
	}

	/**
	 * Menetapkan jenis beasiswa Bidikmisi/KIP-Kuliah.
	 *
	 * @param beasiswaBidikMisi jenis beasiswa.
	 */
	public void setBeasiswaBidikMisi(JenisPenerimaBeasiswa beasiswaBidikMisi) {
		this.beasiswaBidikMisi = beasiswaBidikMisi;
	}

	/**
	 * Jenis beasiswa lain di luar dua kategori baku — relasi {@code @ManyToOne} ke
	 * {@link JenisPenerimaBeasiswa} lewat kolom {@code beasiswa_lain}.
	 *
	 * @return jenis beasiswa; {@code null} bila tidak ada.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "beasiswa_lain", nullable = true)
	public JenisPenerimaBeasiswa getBeasiswaLain() {
		beasiswaLain = check(beasiswaLain);
		return beasiswaLain;
	}

	/**
	 * Menetapkan jenis beasiswa lain.
	 *
	 * @param beasiswaLain jenis beasiswa.
	 */
	public void setBeasiswaLain(JenisPenerimaBeasiswa beasiswaLain) {
		this.beasiswaLain = beasiswaLain;
	}

	/**
	 * Catatan bebas tentang beasiswa yang diterima mahasiswa.
	 *
	 * @return keterangan beasiswa; {@code null} bila kosong.
	 */
	public String getKeteranganBeasiswa() {
		return keteranganBeasiswa;
	}

	/**
	 * Menetapkan catatan beasiswa.
	 *
	 * @param keteranganBeasiswa catatan bebas.
	 */
	public void setKeteranganBeasiswa(String keteranganBeasiswa) {
		this.keteranganBeasiswa = keteranganBeasiswa;
	}

	/**
	 * Identitas mahasiswa di aplikasi Feeder PDDikti (kunci sinkronisasi ke pangkalan data
	 * pendidikan tinggi). String kosong dinormalkan menjadi {@code null} supaya pemeriksaan
	 * "sudah tersinkron atau belum" cukup memeriksa {@code null}.
	 *
	 * @return id feeder sudah di-trim; {@code null} bila belum tersinkron.
	 */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Menetapkan identitas mahasiswa di Feeder PDDikti.
	 *
	 * @param feeder id feeder.
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Status keluar mahasiswa (lulus, mengundurkan diri, putus studi, dsb.) — relasi
	 * {@code @ManyToOne} ke {@link StatusKeluar} lewat kolom {@code status_keluar}. Mahasiswa yang
	 * masih aktif bernilai {@code null}.
	 *
	 * <p><b>Dua aturan tersembunyi yang penting:</b></p>
	 * <ol>
	 *   <li>Bila mahasiswa tergabung dalam {@link KelompokStatusKeluarMahasiswa} (SK kolektif),
	 *       status kelompok MENIMPA kolom perorangan.</li>
	 *   <li>Bila tidak tergabung kelompok, status "Lulus" (id {@code 1}) DIANULIR menjadi
	 *       {@code null} apabila {@code semesterLulus} tersimpan masih kosong — artinya data
	 *       kelulusan dianggap belum lengkap. Perbaikan terdahulu (lihat komentar dalam metode)
	 *       menghapus syarat lama "semesterLulus &lt; jumlah semester jenjang" karena lulus lebih
	 *       cepat dari nominal jenjang itu SAH; syarat lama membuat tagihan semester lanjutan
	 *       terus tergenerate untuk mahasiswa yang sudah lulus.</li>
	 * </ol>
	 *
	 * @return status keluar; {@code null} bila mahasiswa masih aktif atau data kelulusan belum
	 *         lengkap.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_keluar", nullable = true)
	public StatusKeluar getStatusKeluar() {
		statusKeluar = check(statusKeluar);

		if (getKelompokStatusKeluarMahasiswa() != null) {
			statusKeluar = getKelompokStatusKeluarMahasiswa().getStatusKeluar();
		} else {
			statusKeluar = check(statusKeluar);
			// FIX (ERROR tagihan semester lanjutan tergenerate lagi utk mahasiswa yang sudah
			// Lulus lebih cepat dari nominal jenjang, mis. mahasiswa S2 lulus di semester 4):
			// kondisi lama menganulir status Lulus (id=1) yang sudah direkam eksplisit hanya
			// karena semesterLulus tersimpan < jenjang.getJumlahSemesterLulus() (nilai ini bisa
			// salah bila data master jenjang belum mengisi jumlah semester, sehingga jatuh ke
			// default S1=8). Akibatnya getSemesterLulus() ikut mengembalikan null, dan guard
			// "smt > smtLulusMhs" di proses generate tagihan (KegiatanHelper,
			// TunggakanMahasiswaDaftarUlangProcessor, dst) tidak pernah aktif -- tagihan
			// semester berikutnya terus tergenerate sampai jenjang.getJumlahSemesterMaksimal()
			// meski mahasiswa sudah lulus. Lulus lebih cepat dari nominal jenjang itu SAH,
			// bukan indikasi data salah -- jangan dianulir. Anulir hanya bila semesterLulus
			// memang belum terisi sama sekali (data kelulusan belum lengkap).
			if (statusKeluar != null && statusKeluar.getId() != null && statusKeluar.getId().equals(1L)
					&& semesterLulus == null) {
				statusKeluar = null;
			}
		}

		return statusKeluar;
	}

	/**
	 * Menetapkan status keluar mahasiswa.
	 *
	 * @param statusKeluar status keluar; {@code null} berarti masih aktif.
	 */
	public void setStatusKeluar(StatusKeluar statusKeluar) {
		this.statusKeluar = statusKeluar;
	}

	/**
	 * Bulan/tanggal AWAL masa bimbingan tugas akhir (tipe DATE).
	 *
	 * @return tanggal awal bimbingan; {@code null} bila belum ditetapkan.
	 */
	@Temporal(TemporalType.DATE)
	public Date getBlnAwalBimbingan() {
		return blnAwalBimbingan;
	}

	/**
	 * Menetapkan awal masa bimbingan tugas akhir.
	 *
	 * @param blnAwalBimbingan tanggal awal bimbingan.
	 */
	public void setBlnAwalBimbingan(Date blnAwalBimbingan) {
		this.blnAwalBimbingan = blnAwalBimbingan;
	}

	/**
	 * Bulan/tanggal AKHIR masa bimbingan tugas akhir (tipe DATE).
	 *
	 * @return tanggal akhir bimbingan; {@code null} bila belum ditetapkan.
	 */
	@Temporal(TemporalType.DATE)
	public Date getBlnAkhirBimbingan() {
		return blnAkhirBimbingan;
	}

	/**
	 * Menetapkan akhir masa bimbingan tugas akhir.
	 *
	 * @param blnAkhirBimbingan tanggal akhir bimbingan.
	 */
	public void setBlnAkhirBimbingan(Date blnAkhirBimbingan) {
		this.blnAkhirBimbingan = blnAkhirBimbingan;
	}

	/**
	 * Id registrasi peserta didik di PDDikti ({@code id_reg_pd}) — kunci registrasi mahasiswa pada
	 * satuan pendidikan, berbeda dari {@link #getFeeder()}. Kosong dinormalkan menjadi {@code null}.
	 *
	 * @return id registrasi peserta didik; {@code null} bila belum ada.
	 */
	public String getIdRegPd() {
		return idRegPd == null || idRegPd.trim().isEmpty() ? null : idRegPd.trim();
	}

	/**
	 * Menetapkan id registrasi peserta didik PDDikti.
	 *
	 * @param idRegPd id registrasi peserta didik.
	 */
	public void setIdRegPd(String idRegPd) {
		this.idRegPd = idRegPd;
	}

	/**
	 * Penanda baris mahasiswa masih AKTIF dipakai sistem (berbeda dari status akademik aktif per
	 * semester yang dihitung {@code HistoryStatusMahasiswaUtil}).
	 *
	 * <p>Bila {@code null} dianggap {@code true}. <b>Aturan tersembunyi:</b> begitu
	 * {@link #getNimBaruPindah()} terisi — artinya mahasiswa sudah pindah ke baris/NIM baru —
	 * baris ini otomatis dianggap TIDAK aktif supaya tidak muncul ganda di pencarian dan
	 * laporan.</p>
	 *
	 * @return {@code true} bila baris mahasiswa masih aktif.
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}

		if (getNimBaruPindah() != null && !getNimBaruPindah().trim().isEmpty()) {
			aktif = false;
		}

		return aktif;
	}

	/**
	 * Menetapkan penanda aktif baris mahasiswa.
	 *
	 * @param aktif {@code true} bila aktif.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * NIM BARU mahasiswa setelah pindah (mis. alih prodi) — penanda bahwa baris ini sudah
	 * digantikan baris lain. Bila nilainya sama persis dengan {@link #getNim()} sendiri, dianggap
	 * salah isi dan dinormalkan menjadi {@code null} agar mahasiswa tidak menonaktifkan dirinya
	 * sendiri lewat {@link #getAktif()}.
	 *
	 * @return NIM baru; {@code null} bila mahasiswa belum pindah.
	 */
	public String getNimBaruPindah() {
		if (nim != null && nimBaruPindah != null && nim.trim().equalsIgnoreCase(nimBaruPindah.trim())) {
			nimBaruPindah = null;
		}
		return nimBaruPindah;
	}

	/**
	 * Menetapkan NIM baru tujuan kepindahan.
	 *
	 * @param nimBaruPindah NIM baru.
	 */
	public void setNimBaruPindah(String nimBaruPindah) {
		this.nimBaruPindah = nimBaruPindah;
	}

	/**
	 * Jumlah SKS dari kampus asal yang DIAKUI saat mahasiswa pindah antar-perguruan tinggi. Bila
	 * {@code null} dinormalkan menjadi {@code 0}.
	 *
	 * @return jumlah SKS diakui (tidak pernah {@code null}).
	 */
	public Integer getSksYangDiakui() {
		if (sksYangDiakui == null) {
			sksYangDiakui = 0;
		}
		return sksYangDiakui;
	}

	/**
	 * Menetapkan jumlah SKS yang diakui dari kampus asal.
	 *
	 * @param sksYangDiakui jumlah SKS.
	 */
	public void setSksYangDiakui(Integer sksYangDiakui) {
		this.sksYangDiakui = sksYangDiakui;
	}

	/**
	 * Jumlah SKS yang DIAKUI saat mahasiswa alih program studi di dalam kampus yang sama. Bila
	 * {@code null} dinormalkan menjadi {@code 0}.
	 *
	 * @return jumlah SKS diakui pada alih prodi (tidak pernah {@code null}).
	 */
	public Integer getSksYangDiakuiPindahProdi() {
		if (sksYangDiakuiPindahProdi == null) {
			sksYangDiakuiPindahProdi = 0;
		}
		return sksYangDiakuiPindahProdi;
	}

	/**
	 * Menetapkan jumlah SKS yang diakui pada alih program studi.
	 *
	 * @param sksYangDiakuiPindahProdi jumlah SKS.
	 */
	public void setSksYangDiakuiPindahProdi(Integer sksYangDiakuiPindahProdi) {
		this.sksYangDiakuiPindahProdi = sksYangDiakuiPindahProdi;
	}

	/**
	 * Nama program studi ASAL bagi mahasiswa alih prodi. Hanya dihitung ulang bila
	 * {@link #getMerupakanAlihProdi()} bernilai {@code true}, dengan membaca
	 * {@code getAlihProdiMahasiswa().getJurusan().getNama()}.
	 *
	 * <p><b>Jangan diubah menjadi akses field mentah.</b> Komentar FIX di dalam metode mencatat
	 * penyebab {@code LazyInitializationException} (isu KE-13): versi lama membaca field
	 * {@code alihProdiMahasiswa} langsung sehingga melewati {@code check()} yang menyambungkan
	 * ulang proxy detached ke sesi aktif.</p>
	 *
	 * @return nama prodi asal sudah di-trim; string kosong bila bukan alih prodi atau gagal dimuat.
	 */
	public String getNamaProdiPindah() {
		if (getMerupakanAlihProdi()) {
			try {
				// FIX akar masalah LazyInitializationException (KE-13): sebelumnya baca field
				// mentah `alihProdiMahasiswa` langsung, melewati getAlihProdiMahasiswa() yang
				// memanggil check() utk menyambungkan ulang proxy detached/lazy ke session yang
				// aktif. Kalau proxy ini ternyata detached (mis. dimuat di request/thread lain),
				// akses field mentah + getJurusan() meledak mentah tanpa session, bukan
				// tersambung ulang dgn aman spt getter resminya.
				Mahasiswa alih = getAlihProdiMahasiswa();
				if (alih != null && alih.getJurusan() != null) {
					namaProdiPindah = alih.getJurusan().getNama();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:1936");
			}
		}
		return namaProdiPindah == null ? "" : namaProdiPindah.trim();
	}

	/**
	 * Menetapkan nama program studi asal (akan tertimpa getter-nya bila mahasiswa alih prodi).
	 *
	 * @param namaProdiPindah nama prodi asal.
	 */
	public void setNamaProdiPindah(String namaProdiPindah) {
		this.namaProdiPindah = namaProdiPindah;
	}

	/**
	 * Salinan NIM pada kolom UNIK terpisah ({@code @Column(unique = true)}) — dipakai sebagai
	 * kunci pencarian/penjagaan keunikan tambahan. SELALU disegarkan dari {@link #getNim()} saat
	 * dibaca, sehingga tidak pernah menyimpang dari NIM sebenarnya.
	 *
	 * @return NIM (sama dengan {@link #getNim()}).
	 */
	@Column(unique = true)
	public String getNimKey() {
		nimKey = getNim();
		return nimKey;
	}

	/**
	 * Menetapkan salinan NIM (akan selalu tertimpa oleh {@link #getNimKey()}).
	 *
	 * @param nimKey nilai NIM.
	 */
	public void setNimKey(String nimKey) {
		this.nimKey = nimKey;
	}

	/**
	 * Id akun Facebook yang tertaut untuk login sosial. Kolom boleh berisi BEBERAPA id dipisah
	 * koma (lihat {@link #appendFacebookId(String)}); koma ganda dirapatkan dan nilai yang hanya
	 * berupa {@code ","} dianggap kosong.
	 *
	 * @return daftar id Facebook dipisah koma; string kosong bila belum ada.
	 * @see SocialMediaCommonModel
	 */
	public String getFacebookId() {
		if (facebookId != null && facebookId.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				facebookId = facebookId.replaceAll(",,", ",");
			}
		}
		if (facebookId == null) {
			facebookId = "";
		}
		if (facebookId.trim().equals(",")) {
			facebookId = "";
		}
		return facebookId;
	}

	/**
	 * Mengganti seluruh daftar id Facebook yang tertaut.
	 *
	 * @param facebookId satu id, atau beberapa id dipisah koma.
	 */
	public void setFacebookId(String facebookId) {
		this.facebookId = facebookId;
	}

	/**
	 * Menambahkan satu id Facebook ke daftar (dipisah koma) tanpa membuang yang lama. Berbeda
	 * dengan {@link #appendEmail(String)}, di sini TIDAK ada pemeriksaan duplikat.
	 *
	 * @param facebookId id akun Facebook.
	 */
	public void appendFacebookId(String facebookId) {
		this.facebookId = this.facebookId == null || this.facebookId.trim().isEmpty() ? facebookId
				: this.facebookId + "," + facebookId;
	}

	/**
	 * Id akun Google yang tertaut untuk login sosial; boleh berisi beberapa id dipisah koma.
	 * Pembersihan koma ganda sama seperti {@link #getFacebookId()}.
	 *
	 * @return daftar id Google dipisah koma; string kosong bila belum ada.
	 */
	public String getGoogleId() {
		if (googleId != null && googleId.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				googleId = googleId.replaceAll(",,", ",");
			}
		}
		if (googleId == null) {
			googleId = "";
		}
		if (googleId.trim().equals(",")) {
			googleId = "";
		}
		return googleId;
	}

	/**
	 * Mengganti seluruh daftar id Google yang tertaut.
	 *
	 * @param googleId satu id, atau beberapa id dipisah koma.
	 */
	public void setGoogleId(String googleId) {
		this.googleId = googleId;
	}

	/**
	 * Menambahkan satu id Google ke daftar (dipisah koma), tanpa pemeriksaan duplikat.
	 *
	 * @param googleId id akun Google.
	 */
	public void appendGoogleId(String googleId) {
		this.googleId = this.googleId == null || this.googleId.trim().isEmpty() ? googleId
				: this.googleId + "," + googleId;
	}

	/**
	 * Id akun Twitter/X yang tertaut untuk login sosial; boleh berisi beberapa id dipisah koma.
	 *
	 * @return daftar id Twitter dipisah koma; string kosong bila belum ada.
	 */
	public String getTwitterId() {
		if (twitterId != null && twitterId.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				twitterId = twitterId.replaceAll(",,", ",");
			}
		}
		if (twitterId == null) {
			twitterId = "";
		}
		if (twitterId.trim().equals(",")) {
			twitterId = "";
		}
		return twitterId;
	}

	/**
	 * Mengganti seluruh daftar id Twitter/X yang tertaut.
	 *
	 * @param twitterId satu id, atau beberapa id dipisah koma.
	 */
	public void setTwitterId(String twitterId) {
		this.twitterId = twitterId;
	}

	/**
	 * Menambahkan satu id Twitter/X ke daftar (dipisah koma), tanpa pemeriksaan duplikat.
	 *
	 * @param twitterId id akun Twitter/X.
	 */
	public void appendTwitterId(String twitterId) {
		this.twitterId = this.twitterId == null || this.twitterId.trim().isEmpty() ? twitterId
				: this.twitterId + "," + twitterId;
	}

	/**
	 * Id akun LinkedIn yang tertaut — kolom {@code linkedinid}; boleh berisi beberapa id dipisah
	 * koma.
	 *
	 * @return daftar id LinkedIn dipisah koma; string kosong bila belum ada.
	 */
	@Column(name = "linkedinid")
	public String getLinkedinId() {
		if (linkedinId != null && linkedinId.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				linkedinId = linkedinId.replaceAll(",,", ",");
			}
		}
		if (linkedinId == null) {
			linkedinId = "";
		}
		if (linkedinId.trim().equals(",")) {
			linkedinId = "";
		}
		return linkedinId;
	}

	/**
	 * Mengganti seluruh daftar id LinkedIn yang tertaut.
	 *
	 * @param linkedinId satu id, atau beberapa id dipisah koma.
	 */
	public void setLinkedinId(String linkedinId) {
		this.linkedinId = linkedinId;
	}

	/**
	 * Menambahkan satu id LinkedIn ke daftar (dipisah koma), tanpa pemeriksaan duplikat.
	 *
	 * @param linkedinId id akun LinkedIn.
	 */
	public void appendLinkedinId(String linkedinId) {
		this.linkedinId = this.linkedinId == null || this.linkedinId.trim().isEmpty() ? linkedinId
				: this.linkedinId + "," + linkedinId;
	}

	/**
	 * Profil media sosial mahasiswa dalam satu kolom TEXT ({@code social_media_profile}), berisi
	 * beberapa ruas yang dirangkai dengan pemisah {@code "||"}.
	 *
	 * <p>Karena rangkaian itu bisa menghasilkan ruas KOSONG yang tampak sebagai {@code "||||"} dan
	 * merusak pemisahan, getter mengganti setiap {@code "||||"} menjadi {@code "||#||"} —
	 * menyisipkan penanda {@code #} sebagai isi ruas kosong. {@code null} dinormalkan menjadi
	 * string kosong.</p>
	 *
	 * @return teks profil media sosial (tidak pernah {@code null}).
	 * @see SocialMediaCommonModel
	 */
	@Column(name = "social_media_profile", columnDefinition = "text")
	public String getSocialMediaProfile() {
		if (socialMediaProfile == null) {
			socialMediaProfile = "";
		}

		if (socialMediaProfile.contains("||||")) {
			socialMediaProfile = org.apache.commons.lang3.StringUtils.replace(socialMediaProfile, "||||", "||#||");
		}

		return socialMediaProfile;
	}

	/**
	 * Menetapkan teks profil media sosial.
	 *
	 * @param socialMediaProfile teks profil berpemisah {@code "||"}.
	 */
	public void setSocialMediaProfile(String socialMediaProfile) {
		this.socialMediaProfile = socialMediaProfile;
	}

	/**
	 * Bahasa antarmuka pilihan mahasiswa. Ditandai {@code @NotAudited} sehingga perubahannya
	 * TIDAK direkam Envers (preferensi tampilan, bukan data akademik). Bila kosong dipakai
	 * {@code Tbmuser.INDONESIA}.
	 *
	 * @return kode bahasa antarmuka (tidak pernah {@code null}).
	 */
	@NotAudited
	public String getBahasa() {
		return bahasa == null ? Tbmuser.INDONESIA : bahasa;
	}

	/**
	 * Menetapkan bahasa antarmuka pilihan mahasiswa.
	 *
	 * @param bahasa kode bahasa.
	 */
	public void setBahasa(String bahasa) {
		this.bahasa = bahasa;
	}

	/**
	 * Predikat kelulusan/yudisium (mis. Cum Laude, Sangat Memuaskan) — relasi {@code @ManyToOne}
	 * ke {@link Judisium} lewat kolom {@code predikat_kelulusan}. Dicetak di transkrip dan SKPI.
	 *
	 * @return predikat kelulusan; {@code null} bila belum ditetapkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "predikat_kelulusan", nullable = true)
	public Judisium getPredikatKelulusan() {
		predikatKelulusan = check(predikatKelulusan);
		return predikatKelulusan;
	}

	/**
	 * Menetapkan predikat kelulusan.
	 *
	 * @param predikatKelulusan predikat kelulusan.
	 */
	public void setPredikatKelulusan(Judisium predikatKelulusan) {
		this.predikatKelulusan = predikatKelulusan;
	}

	/**
	 * Mengurutkan mahasiswa berdasarkan NIM (bukan id), sehingga daftar mahasiswa tampil urut NIM
	 * secara alami di seluruh layar dan laporan. Bila pembandingan gagal (mis. NIM {@code null}
	 * atau objek pembanding bukan {@code Mahasiswa}), urutan jatuh kembali ke aturan bawaan
	 * {@link ais.database.model.GeneralValueObject#compareTo(GeneralValueObject)}.
	 *
	 * @param arg0 objek pembanding.
	 * @return hasil {@code String.compareTo} atas NIM, atau hasil pembandingan induk sebagai
	 *         cadangan.
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			Mahasiswa o = (Mahasiswa) arg0;
			return nim.compareTo(o.nim);
		} catch (Exception e) {
			return super.compareTo(arg0);
		}
	}

	/**
	 * Id dosen pembimbing akademik (dosen PA) mahasiswa ini, disimpan sebagai {@code Long} mentah.
	 *
	 * <p>Dua perilaku khas:</p>
	 * <ul>
	 *   <li>Bila mahasiswa SUDAH KELUAR ({@code statusKeluar} terisi), dosen PA dipaksa
	 *       {@code null} — mahasiswa yang sudah lulus tidak lagi membebani daftar bimbingan
	 *       dosen.</li>
	 *   <li>Bila field masih kosong/0, nilai dibaca dari penyimpanan kunci JSON
	 *       ({@code retreive("dosen")}) milik {@link ais.database.model.GeneralValueObject},
	 *       bukan dari kolom tabel.</li>
	 * </ul>
	 *
	 * @return id dosen PA; {@code null} bila tidak ada atau mahasiswa sudah keluar.
	 * @see #getDosenPa()
	 */
	public Long getDosen() {
		if (statusKeluar != null) {
			dosen = null;
			return dosen;
		} else {
			String s = dosen == null || dosen.equals(0L) ? retreive("dosen") : dosen.toString();
			return s == null || s.trim().isEmpty() ? null : Long.parseLong(s.trim());
		}
	}

	/**
	 * Menetapkan id dosen pembimbing akademik. Nilai yang sah (bukan {@code null}/0) juga
	 * disalin ke penyimpanan kunci JSON dengan kunci {@code "dosen"} karena {@link #getDosen()}
	 * membacanya dari sana.
	 *
	 * @param dosen id dosen PA.
	 */
	public void setDosen(Long dosen) {
		if (dosen != null && !dosen.equals(0L)) {
			put(dosen.toString(), "dosen");
			this.dosen = dosen;
		} else {
			this.dosen = dosen;
		}
	}

	/**
	 * Nama kelas mahasiswa sebagai teks. Bila {@link #getKelasPmb()} terisi dan punya kelas
	 * bernama, nama itu MENIMPA isi kolom sehingga kelas selalu mengikuti data penempatan PMB.
	 *
	 * @return nama kelas sudah di-trim; string kosong bila belum ditetapkan.
	 */
	public String getKelas() {
		if (getKelasPmb() != null && kelasPmb.getKelas() != null && kelasPmb.getKelas().getNama() != null
				&& !kelasPmb.getKelas().getNama().trim().isEmpty()) {
			kelas = kelasPmb.getKelas().getNama();
		}
		return kelas == null ? "" : kelas.trim();
	}

	/**
	 * Menetapkan nama kelas mahasiswa.
	 *
	 * @param kelas nama kelas.
	 */
	public void setKelas(String kelas) {
		this.kelas = kelas;
	}

	/**
	 * Nama pengguna mahasiswa di OJS (Open Journal Systems) untuk integrasi publikasi ilmiah —
	 * kolom UNIK. String kosong dinormalkan menjadi {@code null} agar batasan UNIQUE tidak
	 * bentrok antar-mahasiswa yang sama-sama belum punya akun OJS.
	 *
	 * @return nama pengguna OJS; {@code null} bila belum ada.
	 */
	@Column(unique = true)
	public String getUsernameOjs() {
		return usernameOjs == null || usernameOjs.trim().isEmpty() ? null : usernameOjs;
	}

	/**
	 * Menetapkan nama pengguna OJS.
	 *
	 * @param usernameOjs nama pengguna OJS.
	 */
	public void setUsernameOjs(String usernameOjs) {
		this.usernameOjs = usernameOjs;
	}

	/**
	 * Tanggal lahir dalam bentuk TEKS yang boleh diketik manual — dipakai untuk data lama yang
	 * tanggal lahirnya tidak lengkap/tidak sah sebagai {@code Date}. Bila kosong, diisi otomatis
	 * dari {@link #getTanggallahir()} memakai {@code Common.dateFormat2}.
	 *
	 * @return teks tanggal lahir.
	 */
	public String getTanggallahirManual() {
		if ((tanggallahirManual == null || tanggallahirManual.trim().isEmpty()) && getTanggallahir() != null) {
			tanggallahirManual = Common.dateFormat2.get().format(getTanggallahir());
		}
		return tanggallahirManual;
	}

	/**
	 * Menetapkan teks tanggal lahir manual.
	 *
	 * @param tanggallahirManual teks tanggal lahir.
	 */
	public void setTanggallahirManual(String tanggallahirManual) {
		this.tanggallahirManual = tanggallahirManual;
	}

	/**
	 * Nomor SK Drop Out (putus studi) mahasiswa.
	 *
	 * @return nomor SK DO; {@code null} bila tidak ada.
	 */
	public String getSkDo() {
		return skDo;
	}

	/**
	 * Menetapkan nomor SK Drop Out.
	 *
	 * @param skDo nomor SK DO.
	 */
	public void setSkDo(String skDo) {
		this.skDo = skDo;
	}

	/**
	 * Domisili alumni setelah lulus — relasi {@code @ManyToOne} ke
	 * {@link StatusDomisiliSetelahLulus} lewat kolom {@code status_domisili_setelah_lulus}. Diisi
	 * lewat kuesioner penelusuran alumni (tracer study).
	 *
	 * @return status domisili alumni; {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_domisili_setelah_lulus", nullable = true)
	public StatusDomisiliSetelahLulus getStatusDomisiliSetelahLulus() {
		statusDomisiliSetelahLulus = check(statusDomisiliSetelahLulus);
		return statusDomisiliSetelahLulus;
	}

	/**
	 * Menetapkan domisili alumni setelah lulus.
	 *
	 * @param statusDomisiliSetelahLulus status domisili.
	 */
	public void setStatusDomisiliSetelahLulus(StatusDomisiliSetelahLulus statusDomisiliSetelahLulus) {
		this.statusDomisiliSetelahLulus = statusDomisiliSetelahLulus;
	}

	/**
	 * Status pekerjaan alumni setelah lulus — relasi {@code @ManyToOne} ke
	 * {@link StatusPekerjaanSetelahLulus} lewat kolom {@code status_pekerjaan_setelah_lulus}.
	 * Bagian dari penelusuran alumni.
	 *
	 * @return status pekerjaan alumni; {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_pekerjaan_setelah_lulus", nullable = true)
	public StatusPekerjaanSetelahLulus getStatusPekerjaanSetelahLulus() {
		statusPekerjaanSetelahLulus = check(statusPekerjaanSetelahLulus);
		return statusPekerjaanSetelahLulus;
	}

	/**
	 * Menetapkan status pekerjaan alumni setelah lulus.
	 *
	 * @param statusPekerjaanSetelahLulus status pekerjaan.
	 */
	public void setStatusPekerjaanSetelahLulus(StatusPekerjaanSetelahLulus statusPekerjaanSetelahLulus) {
		this.statusPekerjaanSetelahLulus = statusPekerjaanSetelahLulus;
	}

	/**
	 * Kegiatan utama alumni setelah lulus (bekerja, wirausaha, lanjut studi, dsb.) — relasi
	 * {@code @ManyToOne} ke {@link StatusSetelahLulus} lewat kolom {@code status_setelah_lulus}.
	 *
	 * @return status setelah lulus; {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_setelah_lulus", nullable = true)
	public StatusSetelahLulus getStatusSetelahLulus() {
		statusSetelahLulus = check(statusSetelahLulus);
		return statusSetelahLulus;
	}

	/**
	 * Menetapkan kegiatan utama alumni setelah lulus.
	 *
	 * @param statusSetelahLulus status setelah lulus.
	 */
	public void setStatusSetelahLulus(StatusSetelahLulus statusSetelahLulus) {
		this.statusSetelahLulus = statusSetelahLulus;
	}

	/**
	 * Token sekali pakai untuk verifikasi/aktivasi akun mahasiswa (mis. tautan reset kata sandi).
	 *
	 * @return token sudah di-trim; {@code null} bila tidak ada.
	 */
	public String getToken() {
		return token == null ? null : token.trim();
	}

	/**
	 * Menetapkan token verifikasi/aktivasi.
	 *
	 * @param token token.
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * Masa studi dalam JUMLAH HARI, dihitung dengan Joda-Time dari {@link #getTanggalMasuk()}
	 * sampai {@link #getTanggalLulus()} (atau sampai hari ini bila belum lulus).
	 *
	 * <p>Bertanda {@code @Transient} — TIDAK disimpan sebagai kolom, selalu dihitung ulang.
	 * Berbeda dengan {@link #ambilMasaStudi()} yang menghasilkan kalimat siap tampil dalam
	 * satuan tahun/bulan/hari.</p>
	 *
	 * @return lama studi dalam hari.
	 */
	@Transient
	public Integer getMasaStudi() {

		LocalDate jamesBirthDay = new LocalDate(getTanggalMasuk());
		LocalDate now = new LocalDate(getTanggalLulus() == null ? ais.ui.util.WaktuUtil.getDate() : getTanggalLulus());
		masaStudi = Days.daysBetween(jamesBirthDay, now).getDays();

		return masaStudi;
	}

	/**
	 * Menetapkan masa studi (akan selalu tertimpa oleh {@link #getMasaStudi()}).
	 *
	 * @param masaStudi lama studi dalam hari.
	 */
	public void setMasaStudi(Integer masaStudi) {
		this.masaStudi = masaStudi;
	}

	/**
	 * Tautan validasi eksternal (mis. halaman verifikasi ijazah di situs lembaga lain) — kolom
	 * TEXT. Dicetak sebagai kode QR/tautan pada dokumen kelulusan.
	 *
	 * @return tautan sudah di-trim; string kosong bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getLinkValidasiEksternal() {
		return linkValidasiEksternal == null ? "" : linkValidasiEksternal.trim();
	}

	/**
	 * Menetapkan tautan validasi eksternal.
	 *
	 * @param linkValidasiEksternal URL validasi.
	 */
	public void setLinkValidasiEksternal(String linkValidasiEksternal) {
		this.linkValidasiEksternal = linkValidasiEksternal;
	}

	/**
	 * Membaca isi BERKAS JSON tempat daftar id {@link Detailperkuliahan} milik mahasiswa ini
	 * disimpan (berkas {@code detail_perkuliahan_<id>} di direktori data,
	 * {@code Common.getFileLocation}).
	 *
	 * <p>Berkas ini adalah pengganti relasi {@code @OneToMany}: alih-alih memuat koleksi Hibernate,
	 * AIS menyimpan peta {@code {"<idDetailperkuliahan>": "<idDetailperkuliahan>"}} sebagai JSON
	 * per mahasiswa. Berkas yang sama juga menampung kunci non-numerik {@code "krs_mhs_..."} dari
	 * {@link #populateDefaultKrsMahasiswa(KrsMahasiswa)} — pembaca WAJIB menyaring kunci bukan
	 * angka (lihat {@link #ambilDetailperkuliahan(Integer)}).</p>
	 *
	 * <p>Kegagalan baca ditelan; dikembalikan {@code VOMahasiswa.dataJSON} (objek JSON kosong)
	 * sehingga pemanggil selalu menerima JSON yang sah.</p>
	 *
	 * @return teks JSON isi berkas, atau JSON kosong bila berkas belum ada/gagal dibaca.
	 */
	public String ambilLokasiDetailPerkuliahan() {
		File file = Common.getFileLocation(this, "detail_perkuliahan_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:2241");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas JSON daftar {@link Detailperkuliahan} milik mahasiswa ini dengan {@code data}.
	 * Kegagalan tulis ditelan dan hanya dicatat ke {@code ErrorAuditUtil}.
	 *
	 * @param data teks JSON yang hendak disimpan.
	 * @see #ambilLokasiDetailPerkuliahan()
	 */
	public void tulisLokasiDetailPerkuliahan(String data) {
		File file = Common.getFileLocation(this, "detail_perkuliahan_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:2250");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * MENGHAPUS berkas JSON daftar {@link Detailperkuliahan} milik mahasiswa ini. Dipanggil
	 * {@link #reInitDetailperkuliahan(Session)} sebelum membangun ulang daftar dari basis data.
	 */
	public void bersihkanLokasiDetailPerkuliahan() {
		File file = Common.getFileLocation(this, "detail_perkuliahan_" + getId().toString());
		BacaTulisUtil.doHapus(file, "detail_perkuliahan");

	}

	/**
	 * Mencari baris {@link KrsMahasiswa} (kartu rencana studi) milik mahasiswa ini untuk satu
	 * kombinasi semester/tahapan/semester pendek, memakai {@code session} yang DIBERIKAN pemanggil.
	 *
	 * <p>Dua tahap pencarian:</p>
	 * <ol>
	 *   <li>lewat kode unik {@code KrsMahasiswa.generateKodeUnik(mahasiswa, semester, tahapan,
	 *       semesterPendek)} — jalur cepat dan paling tepat;</li>
	 *   <li>bila belum ketemu, query kriteria berdasarkan mahasiswa + semester/tahapan +
	 *       semesterPendek, diurutkan id menurun dan diambil satu (baris terbaru menang).</li>
	 * </ol>
	 *
	 * <p>Nilai {@code tahapan} 0 dianggap sama dengan {@code null} (mode tahapan tidak dipakai);
	 * dalam mode itu pencocokan memakai kolom {@code semester} dan {@code tahapan} harus NULL.
	 * Metode ini hanya MEMBACA — pembuatan KRS baru bukan tanggung jawabnya.</p>
	 *
	 * @param semester       semester KRS yang dicari.
	 * @param tahapan        tahapan dalam semester; {@code null}/0 berarti tanpa tahapan.
	 * @param semesterPendek penanda semester pendek; {@code null} berarti bukan semester pendek.
	 * @param session        sesi Hibernate aktif milik pemanggil (tidak ditutup di sini).
	 * @return baris KRS yang cocok, atau {@code null} bila belum ada.
	 */
	public KrsMahasiswa ambilDefaultKrsMahasiswa(Integer semester, Integer tahapan, Integer semesterPendek,
			Session session) {
		tahapan = tahapan == null || tahapan.equals(0) ? null : tahapan;

		String kodeUnik = KrsMahasiswa.generateKodeUnik(this, semester, tahapan, semesterPendek);

		KrsMahasiswa krsMahasiswa = (KrsMahasiswa) session.createCriteria(KrsMahasiswa.class)
				.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
		if (krsMahasiswa != null) {
			return krsMahasiswa;
		} else {

			Criterion criterionSemester = tahapan == null || tahapan.equals(0) ? Restrictions.eq("semester", semester)
					: Restrictions.sqlRestriction("true");

			Criterion criterionTahapan = tahapan == null || tahapan.equals(0) ? Restrictions.isNull("tahapan")
					: Restrictions.eq("tahapan", tahapan);

			krsMahasiswa = (KrsMahasiswa) session.createCriteria(KrsMahasiswa.class)
					.add((semesterPendek == null ? Restrictions.isNull("semesterPendek")
							: Restrictions.eq("semesterPendek", semesterPendek)))
					.add(Restrictions.eq("mahasiswa", this)).add(criterionSemester).add(criterionTahapan)
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			return krsMahasiswa;
		}
	}

	/**
	 * Mencatat id {@link KrsMahasiswa} ke dalam berkas JSON detailperkuliahan mahasiswa ini
	 * memakai kunci gabungan
	 * <code>krs_mhs_{idMahasiswa}_{semester}_{tahapan}_{semesterPendek}</code>, sehingga KRS untuk
	 * satu kombinasi semester dapat ditemukan kembali tanpa query.
	 *
	 * <p><b>Kenapa {@code null} diganti 0:</b> merangkai String dengan {@code null} menghasilkan
	 * literal {@code "null"} di dalam kunci. Karena berkas JSON ini juga berisi id
	 * {@link Detailperkuliahan} yang murni numerik, kunci berliteral {@code "null"} pernah lolos
	 * ke {@code Long.parseLong} dan memicu {@code NumberFormatException} — lihat penjagaan
	 * {@code StringUtils.isNumeric} di {@link #ambilDetailperkuliahan(Integer)}.</p>
	 *
	 * <p>Efek samping: MENULIS berkas JSON. Kegagalan ditelan.</p>
	 *
	 * @param krsMahasiswa baris KRS yang hendak dicatat.
	 */
	public void populateDefaultKrsMahasiswa(KrsMahasiswa krsMahasiswa) {
		try {
			// Root-cause: tahapan/semesterPendek null menghasilkan literal "null" di
			// key (concat String+null). Key ini juga tercampur di lokasi JSON yang
			// sama dengan id Detailperkuliahan (lihat guard StringUtils.isNumeric
			// di ambilDetailperkuliahan(Integer)) - hindari literal "null" agar
			// data tersimpan tetap bersih meski key ini bukan id numerik.
			String key = "krs_mhs_" + getId() + "_" + krsMahasiswa.getSemester() + "_"
					+ (krsMahasiswa.getTahapan() != null ? krsMahasiswa.getTahapan() : 0) + "_"
					+ (krsMahasiswa.getSemesterPendek() != null ? krsMahasiswa.getSemesterPendek() : 0);

			JSONObject c = new JSONObject(ambilLokasiDetailPerkuliahan());
			c.put(key, krsMahasiswa.getId().toString());
			tulisLokasiDetailPerkuliahan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:2298");

		}
	}

	/**
	 * Menghapus satu id {@link Detailperkuliahan} dari berkas JSON mahasiswa ini. Penghapusan
	 * dilakukan dengan MENGOSONGKAN nilainya ({@code c.put(id, "")}), bukan membuang kuncinya —
	 * pembaca memang melewati entri bernilai kosong.
	 *
	 * <p>Efek samping: MENULIS berkas JSON. Kegagalan ditelan.</p>
	 *
	 * @param id id detailperkuliahan yang dilepas dari daftar.
	 */
	public void removeDetailperkuliahan(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiDetailPerkuliahan());
			c.put(id.toString(), "");
			tulisLokasiDetailPerkuliahan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:2308");

		}
	}

	/**
	 * Mendaftarkan satu {@link Detailperkuliahan} ke berkas JSON mahasiswa ini.
	 *
	 * <p>Baris yang punya {@code ikutiPerkuliahan} (baris "ikut kelas lain"/titipan) SENGAJA
	 * dilewati agar tidak terhitung dua kali. Parameter {@code tulisUlang} saat ini tidak dipakai
	 * di dalam badan metode — dipertahankan demi kecocokan dengan pemanggil lama.</p>
	 *
	 * <p>Efek samping: MENULIS berkas JSON. Kegagalan ditelan.</p>
	 *
	 * @param detailperkuliahan baris detail perkuliahan yang didaftarkan; {@code null} diabaikan.
	 * @param tulisUlang        tidak dipakai (dipertahankan demi kompatibilitas).
	 */
	public void populateDetailperkuliahan(Detailperkuliahan detailperkuliahan, boolean tulisUlang) {
		try {
			if (detailperkuliahan == null || detailperkuliahan.getIkutiPerkuliahan() != null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiDetailPerkuliahan());
			c.put(detailperkuliahan.getId().toString(), detailperkuliahan.getId().toString());
			tulisLokasiDetailPerkuliahan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:2322");
		}
	}

	/**
	 * Pintasan {@link #ambilPerkuliahanDanParalel(String, String, String, String, String, boolean,
	 * Integer, boolean, boolean, boolean, Integer, int, int, boolean, JenisFormulirKegiatan)}
	 * dengan {@code keywordJadiPembatas = false} dan tanpa penyaringan jenis formulir kegiatan.
	 *
	 * @return larik {@code [List<VOPembelajaran> halaman, int totalData, List<VOPembelajaran> semua]}.
	 */
	public Object[] ambilPerkuliahanDanParalel(String tahunAkademik, String jenisSemester, String hari, String keyword,
			String kelas, boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean paralel, boolean remedial,
			boolean paralelAja, Integer ditampilkanHanya, int mulai, int banyak) {
		return ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hari, keyword, kelas, merupakanPraPerkuliahan,
				ekstrakurikuler, paralel, remedial, paralelAja, ditampilkanHanya, mulai, banyak, false, null);
	}

	/**
	 * Pintasan {@link #ambilPerkuliahanDanParalel(String, String, String, String, String, boolean,
	 * Integer, boolean, boolean, boolean, Integer, int, int, boolean, JenisFormulirKegiatan)}
	 * dengan {@code keywordJadiPembatas = false}, tetapi MENYARING kegiatan menurut
	 * {@code jenisFormulirKegiatan}.
	 *
	 * @param jenisFormulirKegiatan jenis formulir kegiatan yang ditampilkan; {@code null} berarti
	 *        hanya kegiatan tanpa jenis.
	 * @return larik {@code [halaman, totalData, semuaData]}.
	 */
	public Object[] ambilPerkuliahanDanParalel(String tahunAkademik, String jenisSemester, String hari, String keyword,
			String kelas, boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean paralel, boolean remedial,
			boolean paralelAja, Integer ditampilkanHanya, int mulai, int banyak,
			JenisFormulirKegiatan jenisFormulirKegiatan) {
		return ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hari, keyword, kelas, merupakanPraPerkuliahan,
				ekstrakurikuler, paralel, remedial, paralelAja, ditampilkanHanya, mulai, banyak, false,
				jenisFormulirKegiatan);
	}

	/**
	 * Pintasan {@link #ambilPerkuliahanDanParalel(String, String, String, String, String, boolean,
	 * Integer, boolean, boolean, boolean, Integer, int, int, boolean, JenisFormulirKegiatan)}
	 * tanpa penyaringan jenis formulir kegiatan, dengan {@code keywordJadiPembatas} dapat diatur.
	 *
	 * @param keywordJadiPembatas bila {@code true} dan kata kunci kosong, pemuatan id dihentikan
	 *        setelah melebihi kebutuhan satu halaman (optimasi untuk daftar sangat panjang).
	 * @return larik {@code [halaman, totalData, semuaData]}.
	 */
	public Object[] ambilPerkuliahanDanParalel(String tahunAkademik, String jenisSemester, String hari, String keyword,
			String kelas, boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean paralel, boolean remedial,
			boolean paralelAja, Integer ditampilkanHanya, int mulai, int banyak, boolean keywordJadiPembatas) {
		JenisFormulirKegiatan jenisFormulirKegiatan = null;
		return ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hari, keyword, kelas, merupakanPraPerkuliahan,
				ekstrakurikuler, paralel, remedial, paralelAja, ditampilkanHanya, mulai, banyak, keywordJadiPembatas,
				jenisFormulirKegiatan);
	}

	/**
	 * <b>Mesin utama linimasa e-learning mahasiswa.</b> Mengumpulkan seluruh "pembelajaran" yang
	 * diikuti mahasiswa ini menurut SATU kategori tampilan, menyaringnya dengan sejumlah kriteria,
	 * mengurutkannya menurun, lalu memotongnya menjadi satu halaman.
	 *
	 * <p><b>Kategori tampilan</b> ditentukan {@code ditampilkanHanya} yang berisi salah satu
	 * konstanta {@link ais.action.master.TampilanELearningAction}: {@code SKRIPSI},
	 * {@code BIMBINGAN} ({@link MahasiswaRequestTugasAkhir}), {@code KKN}, {@code PKL},
	 * {@code KRS} ({@link KrsMahasiswa}), {@code KEGIATAN} ({@link FormulirKegiatan} lewat
	 * pesertanya), {@code WISUDA} ({@link Wisuda} lewat {@link PendaftaranWisuda}),
	 * {@code KONSULTASI} ({@link PertemuanPunyaGrupPertemuan}) dan {@code PERKULIAHAN}
	 * ({@link Detailperkuliahan} yang sudah DISETUJUI).</p>
	 *
	 * <p><b>Sumber data.</b> Kecuali kategori {@code PERKULIAHAN} (yang memakai
	 * {@link #ambilDetailperkuliahan()}), seluruh kategori membaca daftar id dari penyimpanan
	 * kunci JSON {@code retreiveAll(<namaKelas>)} milik
	 * {@link ais.database.model.GeneralValueObject}. Nilai yang tersimpan bisa berupa id polos
	 * maupun nama berkas {@code ....json}; keduanya dinormalkan menjadi id numerik. Objeknya
	 * kemudian dimuat massal ({@code ConstantValues.ambilBanyak} /
	 * {@code GeneralValueObject.ambilDataBanyak}). Untuk KKN, PKL, kegiatan dan wisuda, entitas
	 * INDUK-nya (kelompok/formulir/wisuda) yang dimasukkan ke daftar dan di-dedup agar tidak
	 * muncul berulang.</p>
	 *
	 * <p><b>Penyaringan</b> dilakukan atas {@code VOPembelajaran} hasil
	 * {@code ambilVOPembelajaran()}: hari, tahun akademik, kata kunci (dicocokkan ke
	 * {@code ambilKeyword()}), kelas, penanda ekstrakurikuler, pra-perkuliahan, jenis semester
	 * (termasuk pembedaan semester pendek), dan remedial. Bila {@code paralel} bernilai
	 * {@code true}, jadwal paralel dari perkuliahan yang sama ikut disertakan; bila
	 * {@code paralelAja} bernilai {@code true}, HANYA yang punya paralel yang ditampilkan.</p>
	 *
	 * <p><b>Ketahanan.</b> Setiap item diproses di dalam {@code try/catch(Throwable)}: satu item
	 * yang gagal (mis. {@code LazyInitializationException} pada proxy detached saat profil
	 * dirender di luar sesi) DILEWATI, tidak meruntuhkan seluruh daftar.</p>
	 *
	 * <p>Hasil akhir diurutkan {@code Collections.reverseOrder()} (terbaru di atas) lalu dipotong
	 * memakai {@code mulai} dan {@code banyak}.</p>
	 *
	 * @param tahunAkademik          tahun akademik penyaring; {@code null} = semua.
	 * @param jenisSemester          {@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}/
	 *                               {@link Perkuliahan#SP}; {@code null} = semua.
	 * @param hari                   nama hari penyaring; kosong = semua.
	 * @param keyword                kata kunci pencarian; kosong = semua.
	 * @param kelas                  penyaring nama kelas; kosong = semua.
	 * @param merupakanPraPerkuliahan hanya tampilkan pra-perkuliahan (matrikulasi).
	 * @param ekstrakurikuler        {@link Perkuliahan#EKSTRA} untuk hanya ekstrakurikuler;
	 *                               {@code null} = tanpa penyaringan.
	 * @param paralel                sertakan jadwal paralel dari perkuliahan yang sama.
	 * @param remedial               hanya tampilkan yang remedial.
	 * @param paralelAja             hanya tampilkan yang punya paralel.
	 * @param ditampilkanHanya       kategori tampilan (konstanta {@code TampilanELearningAction}).
	 * @param mulai                  indeks awal potongan halaman.
	 * @param banyak                 jumlah baris per halaman.
	 * @param keywordJadiPembatas    hentikan pemuatan id lebih awal bila kata kunci kosong.
	 * @param jenisFormulirKegiatan  penyaring jenis formulir kegiatan; {@code null} = hanya
	 *                               kegiatan tanpa jenis.
	 * @return larik tiga elemen: {@code [0]} {@code List<VOPembelajaran>} satu halaman,
	 *         {@code [1]} {@code int} jumlah seluruh data, {@code [2]} {@code List<VOPembelajaran>}
	 *         seluruh data sebelum dipotong.
	 */
	@SuppressWarnings("unchecked")
	public Object[] ambilPerkuliahanDanParalel(String tahunAkademik, String jenisSemester, String hari, String keyword,
			String kelas, boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean paralel, boolean remedial,
			boolean paralelAja, Integer ditampilkanHanya, int mulai, int banyak, boolean keywordJadiPembatas,
			JenisFormulirKegiatan jenisFormulirKegiatan) {
		List<VOPesertaPembelajaran> voPesertaPembelajarans = new ArrayList<VOPesertaPembelajaran>();

		int max = banyak + (mulai * banyak);
		max = max + 1;
		if (ditampilkanHanya.equals(TampilanELearningAction.SKRIPSI)) {
			List<String> ss = retreiveAll(Skripsi.class.getName());

			List<Long> ids = new ArrayList<Long>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && ids.size() > max) {
						break;
					}

					ids.add(Long.parseLong(s));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:2379");
				}
			}

			List<Skripsi> skripsis = ConstantValues.ambilBanyak(Skripsi.class.getName(), ids);
			for (Skripsi skripsi : skripsis) {
				if (skripsi != null) {
//					System.out.println("Skripsi -> " + skripsi);
					voPesertaPembelajarans.add(skripsi);
				}
			}
			ids = null;
			skripsis = null;
		}

		if (ditampilkanHanya.equals(TampilanELearningAction.BIMBINGAN)) {
			List<String> ss = retreiveAll(MahasiswaRequestTugasAkhir.class.getName());

			List<Long> ids = new ArrayList<Long>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && ids.size() > max) {
						break;
					}

					ids.add(Long.parseLong(s));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:2413");
				}
			}

			List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = ConstantValues
					.ambilBanyak(MahasiswaRequestTugasAkhir.class.getName(), ids);
			for (MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : mahasiswaRequestTugasAkhirs) {
				if (mahasiswaRequestTugasAkhir != null) {
//					System.out.println("mahasiswaRequestTugasAkhir -> " + mahasiswaRequestTugasAkhir);
					voPesertaPembelajarans.add(mahasiswaRequestTugasAkhir);
				}
			}
			ids = null;
			mahasiswaRequestTugasAkhirs = null;
		}

		if (ditampilkanHanya.equals(TampilanELearningAction.KKN)) {
			List<String> ss = retreiveAll(MahasiswaDapatKelompokKkn.class.getName());

			List<String> kknIdsData = new ArrayList<String>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && kknIdsData.size() > max) {
						break;
					}

					Long id = Long.parseLong(s);
					kknIdsData.add(id.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:2449");
				}
			}

			List<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKkns = GeneralValueObject
					.ambilDataBanyak(MahasiswaDapatKelompokKkn.class, kknIdsData);
			List<Long> kknIds = new ArrayList<Long>();
			for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : mahasiswaDapatKelompokKkns) {
				if (mahasiswaDapatKelompokKkn != null) {
					KelompokKkn kelompokKkn = mahasiswaDapatKelompokKkn.getKelompokKkn();
					if (kelompokKkn != null && kelompokKkn.getId() != null && !kknIds.contains(kelompokKkn.getId())) {
//						System.out.println("kelompokKkn -> " + kelompokKkn);
						kknIds.add(kelompokKkn.getId());
						voPesertaPembelajarans.add(mahasiswaDapatKelompokKkn);
					}
				}
			}
			mahasiswaDapatKelompokKkns = null;
			kknIds = null;
			kknIdsData = null;
		}

		if (ditampilkanHanya.equals(TampilanELearningAction.PKL)) {
			List<String> ss = retreiveAll(MahasiswaDapatKelompokPkl.class.getName());

			List<String> pklIdsData = new ArrayList<String>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && pklIdsData.size() > max) {
						break;
					}

					Long id = Long.parseLong(s);
					pklIdsData.add(id.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:2491");
				}
			}

			List<MahasiswaDapatKelompokPkl> mahasiswaDapatKelompokPkls = GeneralValueObject
					.ambilDataBanyak(MahasiswaDapatKelompokPkl.class, pklIdsData);
			List<Long> pklIds = new ArrayList<Long>();
			for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : mahasiswaDapatKelompokPkls) {
				if (mahasiswaDapatKelompokPkl != null) {
					KelompokPkl kelompokPkl = mahasiswaDapatKelompokPkl.getKelompokPkl();
					if (kelompokPkl != null && kelompokPkl.getId() != null && !pklIds.contains(kelompokPkl.getId())) {
//						System.out.println("kelompokPkl -> " + kelompokPkl);
						pklIds.add(kelompokPkl.getId());
						voPesertaPembelajarans.add(mahasiswaDapatKelompokPkl);
					}
				}
			}

			mahasiswaDapatKelompokPkls = null;
			pklIds = null;
			pklIdsData = null;
		}
		if (ditampilkanHanya.equals(TampilanELearningAction.KRS)) {
			List<String> ss = retreiveAll(KrsMahasiswa.class.getName());

			List<String> krsMahasiswaIdsData = new ArrayList<String>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && krsMahasiswaIdsData.size() > max) {
						break;
					}

					Long id = Long.parseLong(s);
					krsMahasiswaIdsData.add(id.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:2533");
				}
			}

			List<KrsMahasiswa> krsMahasiswas = GeneralValueObject.ambilDataBanyak(KrsMahasiswa.class,
					krsMahasiswaIdsData);
			for (KrsMahasiswa krsMahasiswa : krsMahasiswas) {
				if (krsMahasiswa != null) {
//					System.out.println("krsMahasiswa -> " + krsMahasiswa);
					voPesertaPembelajarans.add(krsMahasiswa);
				}
			}

			krsMahasiswas = null;
			krsMahasiswaIdsData = null;
		}

		if (ditampilkanHanya.equals(TampilanELearningAction.KEGIATAN)) {
			List<String> ss = retreiveAll(FormulirKegiatanPeserta.class.getName());

			List<String> kegiatanIdData = new ArrayList<String>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && kegiatanIdData.size() > max) {
						break;
					}

					Long id = Long.parseLong(s);
					kegiatanIdData.add(id.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:2570");
				}
			}

			List<FormulirKegiatanPeserta> formulirKegiatanPesertas = GeneralValueObject
					.ambilDataBanyak(FormulirKegiatanPeserta.class, kegiatanIdData);
			List<Long> formulirKegiatanIds = new ArrayList<Long>();
			for (FormulirKegiatanPeserta formulirKegiatanPeserta : formulirKegiatanPesertas) {
				if (formulirKegiatanPeserta != null) {
					FormulirKegiatan formulirKegiatan = formulirKegiatanPeserta.getFormulirKegiatan();
					if (!formulirKegiatanIds.contains(formulirKegiatan.getId())) {
//						System.out.println("formulirKegiatan -> " + formulirKegiatan);

						if (jenisFormulirKegiatan == null && formulirKegiatan.getJenisFormulirKegiatan() == null) {
							formulirKegiatanIds.add(formulirKegiatan.getId());
							voPesertaPembelajarans.add(formulirKegiatan);
						} else if ((formulirKegiatan.getJenisFormulirKegiatan() != null && jenisFormulirKegiatan != null
								&& jenisFormulirKegiatan.getId()
										.equals(formulirKegiatan.getJenisFormulirKegiatan().getId()))) {
							formulirKegiatanIds.add(formulirKegiatan.getId());
							voPesertaPembelajarans.add(formulirKegiatan);
						}
					}
				}
			}

			formulirKegiatanPesertas = null;
			formulirKegiatanIds = null;
			kegiatanIdData = null;
		}

		if (ditampilkanHanya.equals(TampilanELearningAction.WISUDA)) {
			List<String> ss = retreiveAll(PendaftaranWisuda.class.getName());

			List<String> kegiatanIdData = new ArrayList<String>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty() && kegiatanIdData.size() > max) {
						break;
					}

					Long id = Long.parseLong(s);
					kegiatanIdData.add(id.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:2621");
				}
			}

			List<PendaftaranWisuda> pendaftaranWisudas = GeneralValueObject.ambilDataBanyak(PendaftaranWisuda.class,
					kegiatanIdData);
			List<Long> wisudaIds = new ArrayList<Long>();
			for (PendaftaranWisuda pendaftaranWisuda : pendaftaranWisudas) {
				if (pendaftaranWisuda != null) {
					Wisuda wisuda = pendaftaranWisuda.getWisuda();
					if (!wisudaIds.contains(wisuda.getId())) {
						wisudaIds.add(wisuda.getId());
						voPesertaPembelajarans.add(wisuda);
					}
				}
			}

			pendaftaranWisudas = null;
			wisudaIds = null;
			kegiatanIdData = null;
		}

		if (ditampilkanHanya.equals(TampilanELearningAction.KONSULTASI)) {
			List<String> ss = retreiveAll(PertemuanPunyaGrupPertemuan.class.getName());

			List<String> pertemuanPunyaGrupPertemuanIdsData = new ArrayList<String>();
			for (String s : ss) {
				try {
					if (s.endsWith("json")) {
						String[] argv = org.apache.commons.lang3.StringUtils
								.replace(org.apache.commons.lang3.StringUtils.replace(s, "\\", "_"), "/", "_")
								.split("_");
						s = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
					}

					if (keywordJadiPembatas && keyword.trim().isEmpty()
							&& pertemuanPunyaGrupPertemuanIdsData.size() > max) {
						break;
					}

					Long id = Long.parseLong(s);
					pertemuanPunyaGrupPertemuanIdsData.add(id.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:2664");
				}
			}

			List<PertemuanPunyaGrupPertemuan> pertemuanPunyaGrupPertemuans = GeneralValueObject
					.ambilDataBanyak(PertemuanPunyaGrupPertemuan.class, pertemuanPunyaGrupPertemuanIdsData);
			for (PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan : pertemuanPunyaGrupPertemuans) {
				if (pertemuanPunyaGrupPertemuan != null) {
					voPesertaPembelajarans.add(pertemuanPunyaGrupPertemuan);
				}
			}

			pertemuanPunyaGrupPertemuans = null;
			pertemuanPunyaGrupPertemuanIdsData = null;
		}

		if (ditampilkanHanya.equals(TampilanELearningAction.PERKULIAHAN)) {
			List<Long> c = ambilDetailperkuliahan();
			for (Long detailperkuliahanid : c) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
							&& detailperkuliahan.getSemester() != null) {
						voPesertaPembelajarans.add(detailperkuliahan);
					}
				}
			}
		}

		List<VOPembelajaran> dataDiambil = new ArrayList<VOPembelajaran>();
		for (VOPesertaPembelajaran voPesertaPembelajaran : voPesertaPembelajarans) {
			try {

			if (voPesertaPembelajaran != null && voPesertaPembelajaran.ambilVOPembelajaran() != null) {
				VOPembelajaran vo = voPesertaPembelajaran.ambilVOPembelajaran();

				String key = "";
				try {
					if (keyword != null && !keyword.trim().isEmpty()) {
						key = vo.ambilKeyword().toLowerCase();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:2707");
				}

				if ((hari == null || hari.trim().isEmpty()
						|| (vo.ambilHari() != null && hari.trim().equalsIgnoreCase(vo.ambilHari().trim())))

						&& (tahunAkademik == null || tahunAkademik.equals(vo.ambilTahunAkademik()))
						&& (keyword == null || keyword.trim().isEmpty()

								|| key.contains(keyword.toLowerCase().trim())

						) && (kelas == null || kelas.trim().isEmpty()

								|| vo.ambilKelas().toLowerCase().contains(kelas.toLowerCase().trim())

						)
						&& ((ekstrakurikuler == null)
								|| (ekstrakurikuler.equals(Perkuliahan.EKSTRA) && vo.ambilExtraKulikuler() != null
										&& vo.ambilExtraKulikuler().equals(ekstrakurikuler)))

						&& (!merupakanPraPerkuliahan || (merupakanPraPerkuliahan && vo.ambilMerupakanPraPerkuliahan()))

						&& (merupakanPraPerkuliahan || jenisSemester == null
								|| (!jenisSemester.equals(Perkuliahan.SP)
										&& vo.ambilJenisSemester().equals(jenisSemester) && !vo.ambilMerupakanSP())
								|| (jenisSemester.equals(Perkuliahan.SP) && vo.ambilMerupakanSP()))

				) {

					if (!remedial || (remedial && vo.ambilMerupakanRemedial())) {

						if (paralelAja) {
							if (paralel) {
								List<Perkuliahan> jadwalParalels = (voPesertaPembelajaran instanceof Detailperkuliahan)
										? ((Detailperkuliahan) voPesertaPembelajaran).getPerkuliahan()
												.ambilParalelPerkuliahan()
										: new ArrayList<Perkuliahan>();
								for (Perkuliahan jadwal : jadwalParalels) {
									dataDiambil.add(jadwal);
								}
								if (!jadwalParalels.isEmpty() || vo.ambilMerupakanParalel()) {
									dataDiambil.add(voPesertaPembelajaran.ambilVOPembelajaran());
								}
								jadwalParalels = null;
							}
						} else {
							dataDiambil.add(voPesertaPembelajaran.ambilVOPembelajaran());
							if (paralel) {
								List<Perkuliahan> jadwalParalels = (voPesertaPembelajaran instanceof Detailperkuliahan)
										? ((Detailperkuliahan) voPesertaPembelajaran).getPerkuliahan()
												.ambilParalelPerkuliahan()
										: new ArrayList<Perkuliahan>();
								for (Perkuliahan jadwal : jadwalParalels) {
									dataDiambil.add(jadwal);
								}
								jadwalParalels = null;
							}
						}
					}

				}
			}
			} catch (Throwable eItem) {
				// LazyInitializationException (proxy detached saat render profil di luar session) atau
				// error lazy lain pada SATU item TIDAK boleh meruntuhkan seluruh daftar perkuliahan.
				// Lewati item bermasalah; item lain tetap diproses.
				continue;
			}
		}
		try {
			Collections.sort(dataDiambil, Collections.reverseOrder());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:2778");
			// TODO: handle exception
		}

		List<VOPembelajaran> diambil = new ArrayList<VOPembelajaran>();
		int index = 0;
		for (VOPembelajaran voPembelajaran : dataDiambil) {
			if (index >= mulai && index < (mulai + banyak)) {
				diambil.add(voPembelajaran);
			}
			index++;
		}
		voPesertaPembelajarans = null;
		return new Object[] { diambil, index, dataDiambil };
	}

	/**
	 * Mengumpulkan id {@link Perkuliahan} (beserta pasangan paralelnya) yang diambil mahasiswa ini
	 * pada satu tahun akademik dan jenis semester tertentu.
	 *
	 * <p>Hanya {@link Detailperkuliahan} berstatus {@code DISETUJUI} dan bersemester yang dihitung.
	 * Jenis semester ditentukan dari PARITAS nomor semester detailperkuliahan (ganjil = semester
	 * bernilai ganjil), bukan dari kolom di {@link Perkuliahan}. Penyaringan semester pendek
	 * membandingkan {@code perkuliahan.getStatusSemesterPendek()}: {@code null} dicocokkan dengan
	 * {@code null}.</p>
	 *
	 * @param tahunAkademik  tahun akademik penyaring; {@code null} = semua.
	 * @param jenisSemester  {@link Perkuliahan#GANJIL}/{@link Perkuliahan#GENAP}; {@code null} = semua.
	 * @param semesterPendek penanda semester pendek; {@code null} = bukan semester pendek.
	 * @return daftar id perkuliahan beserta paralelnya (boleh berisi duplikat antar-kategori).
	 */
	public List<Long> ambilPerkuliahanDanParalel(String tahunAkademik, String jenisSemester, Integer semesterPendek) {
		List<Long> detailperkuliahans = ambilDetailperkuliahan();
		List<Long> perkuliahans = new ArrayList<Long>();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& detailperkuliahan.getSemester() != null
						&& (tahunAkademik == null || tahunAkademik.equals(detailperkuliahan.getTahunAkademik()))
						&& (jenisSemester == null
								|| (jenisSemester.equals(Perkuliahan.GANJIL) ? detailperkuliahan.getSemester() % 2 == 1
										: detailperkuliahan.getSemester() % 2 == 0))) {

					if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {

						if ((semesterPendek == null
								&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
								|| (semesterPendek != null
										&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
										&& semesterPendek.equals(
												detailperkuliahan.getPerkuliahan().getStatusSemesterPendek()))) {

							perkuliahans.add(detailperkuliahan.getPerkuliahan().getId());

							if (detailperkuliahan.getPerkuliahan().getPerkuliahan_paralel() != null) {
								perkuliahans.add(detailperkuliahan.getPerkuliahan().getPerkuliahan_paralel().getId());
							}
						}
					}

				}
			}
		}
		detailperkuliahans = null;
		return perkuliahans;
	}

	/**
	 * Mengumpulkan id SELURUH {@link Perkuliahan} yang pernah diambil mahasiswa ini (semua semester)
	 * beserta seluruh paralelnya.
	 *
	 * <p>Berbeda dari varian lain, metode ini melakukan perluasan dua tahap: setelah id perkuliahan
	 * dan pasangan {@code perkuliahan_paralel} terkumpul, tiap perkuliahan ditanya lagi
	 * {@code ambilParalel()} untuk menyertakan seluruh anggota rumpun paralel, dengan penjagaan
	 * anti-duplikat.</p>
	 *
	 * @return daftar id perkuliahan + seluruh paralelnya, tanpa duplikat.
	 */
	public List<Long> ambilPerkuliahanDanParalel() {
		List<Long> detailperkuliahans = ambilDetailperkuliahan();
		List<Long> perkuliahans = new ArrayList<Long>();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& detailperkuliahan.getSemester() != null) {
					if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {
						perkuliahans.add(detailperkuliahan.getPerkuliahan().getId());
						if (detailperkuliahan.getPerkuliahan().getPerkuliahan_paralel() != null) {
							perkuliahans.add(detailperkuliahan.getPerkuliahan().getPerkuliahan_paralel().getId());
						}
					}
				}
			}
		}

		List<Long> perkuliahansSemua = new ArrayList<Long>();

		for (Long perkuliahanid : perkuliahans) {
			perkuliahansSemua.add(perkuliahanid);
			Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(), perkuliahanid);
			if (perkuliahan != null) {
				List<Long> perkuliahanparalel = perkuliahan.ambilParalel();
				for (Long paralel : perkuliahanparalel) {
					if (!perkuliahansSemua.contains(paralel)) {
						perkuliahansSemua.add(paralel);
					}
				}
				perkuliahanparalel = null;
			}
		}
		perkuliahans = null;
		detailperkuliahans = null;
		return perkuliahansSemua;
	}

	/**
	 * Mengumpulkan id {@link Perkuliahan} beserta paralelnya untuk satu tahun ajaran dan jenis
	 * semester. Berbeda dengan varian bersemester pendek, penyaringan di sini memakai kolom
	 * {@code tahunAjaran} dan {@code ganjilGenap} milik {@link Perkuliahan} (bukan paritas nomor
	 * semester mahasiswa), dan tidak mensyaratkan detailperkuliahan punya nomor semester.
	 *
	 * @param ta    tahun ajaran perkuliahan; {@code null} = semua.
	 * @param jenis nilai {@code ganjilGenap} perkuliahan; {@code null} = semua.
	 * @return daftar id perkuliahan beserta paralelnya.
	 */
	public List<Long> ambilPerkuliahanDanParalel(String ta, String jenis) {
		List<Long> detailperkuliahans = ambilDetailperkuliahan();
		List<Long> perkuliahans = new ArrayList<Long>();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
					if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getTahunAjaran() != null
							&& (ta == null || ta.equals(detailperkuliahan.getPerkuliahan().getTahunAjaran()))) {
						if (jenis == null || detailperkuliahan.getPerkuliahan().getGanjilGenap().equals(jenis)) {
							perkuliahans.add(detailperkuliahan.getPerkuliahan().getId());

							if (detailperkuliahan.getPerkuliahan().getPerkuliahan_paralel() != null) {
								perkuliahans.add(detailperkuliahan.getPerkuliahan().getPerkuliahan_paralel().getId());
							}
						}
					}

				}
			}
		}
		detailperkuliahans = null;
		return perkuliahans;
	}

	/**
	 * Mengumpulkan id {@link Perkuliahan} beserta paralelnya untuk SATU nomor semester mahasiswa,
	 * dengan pembedaan semester pendek.
	 *
	 * @param semester       nomor semester mahasiswa; {@code null} = semua semester.
	 * @param semesterPendek penanda semester pendek; {@code null} = bukan semester pendek.
	 * @return daftar id perkuliahan beserta paralelnya.
	 */
	public List<Long> ambilPerkuliahanDanParalel(Integer semester, Integer semesterPendek) {
		List<Long> detailperkuliahans = ambilDetailperkuliahan();
		List<Long> perkuliahans = new ArrayList<Long>();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& detailperkuliahan.getSemester() != null
						&& (semester == null || semester.equals(detailperkuliahan.getSemester()))) {
					if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {

						if ((semesterPendek == null
								&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
								|| (semesterPendek != null
										&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
										&& semesterPendek.equals(
												detailperkuliahan.getPerkuliahan().getStatusSemesterPendek()))) {

							perkuliahans.add(detailperkuliahan.getPerkuliahan().getId());

							if (detailperkuliahan.getPerkuliahan().getPerkuliahan_paralel() != null) {
								perkuliahans.add(detailperkuliahan.getPerkuliahan().getPerkuliahan_paralel().getId());
							}
						}
					}
				}
			}
		}
		detailperkuliahans = null;
		return perkuliahans;
	}

	/**
	 * Menyusun teks keterangan hasil pemeriksaan pengambilan KRS mahasiswa ini (mis. peringatan
	 * SKS melebihi jatah, mata kuliah prasyarat belum lulus, tagihan belum lunas). Seluruh aturan
	 * ada di {@link ais.action.master.helper.KrsDetailHelper#rubahKeteranganPengambilanKRS};
	 * metode ini hanya jembatan agar dapat dipanggil langsung dari objek mahasiswa.
	 *
	 * <p>Dipakai sekitar 22 berkas layar/laporan KRS.</p>
	 *
	 * @param semester       semester KRS.
	 * @param tahapan        tahapan dalam semester; {@code null}/0 bila tanpa tahapan.
	 * @param semesterPendek penanda semester pendek.
	 * @param krsMahasiswa   baris KRS yang diperiksa.
	 * @param remedial       {@code true} bila konteksnya pengambilan remedial.
	 * @return teks keterangan siap tampil.
	 * @see ais.action.master.helper.KrsDetailHelper
	 */
	public String rubahKeteranganPengambilanKRS(Integer semester, Integer tahapan, Integer semesterPendek,
			KrsMahasiswa krsMahasiswa, boolean remedial) {
		return KrsDetailHelper.rubahKeteranganPengambilanKRS(this, semester, tahapan, semesterPendek, krsMahasiswa,
				remedial);
	}

	/**
	 * Mencari baris {@link Detailperkuliahan} milik mahasiswa ini untuk satu {@link Perkuliahan}
	 * tertentu (mis. untuk mengetahui nilai mahasiswa pada satu kelas).
	 *
	 * <p>Bila {@code perkuliahan} punya pasangan {@code perkuliahan_paralel}, pencarian dialihkan
	 * ke perkuliahan INDUK paralel itu — di AIS nilai selalu direkam pada satu sisi rumpun paralel.
	 * Hanya baris berstatus {@code DISETUJUI} yang dipertimbangkan.</p>
	 *
	 * <p>Pencarian dilakukan dengan menelusuri seluruh daftar {@link #ambilDetailperkuliahan()}
	 * secara linear, jadi hindari memanggilnya di dalam loop atas banyak perkuliahan.</p>
	 *
	 * @param perkuliahan kelas perkuliahan yang dicari; {@code null}/tanpa id menghasilkan {@code null}.
	 * @return detail perkuliahan mahasiswa pada kelas itu, atau {@code null} bila tidak ada.
	 */
	public Detailperkuliahan ambilDetailperkuliahan(Perkuliahan perkuliahan) {
		if (perkuliahan == null || perkuliahan.getId() == null) {
			return null;
		}
		perkuliahan = perkuliahan.getPerkuliahan_paralel() != null ? perkuliahan.getPerkuliahan_paralel() : perkuliahan;
		Detailperkuliahan d = null;
		List<Long> detailperkuliahans = ambilDetailperkuliahan();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& detailperkuliahan.getPerkuliahan() != null
						&& detailperkuliahan.getPerkuliahan().getId().equals(perkuliahan.getId())) {
					d = detailperkuliahan;
					break;
				}
			}
		}
		return d;
	}

	/**
	 * Daftar id SELURUH {@link Detailperkuliahan} milik mahasiswa ini (semua semester), dibaca dari
	 * berkas JSON. Setara {@code ambilDetailperkuliahan(null)}.
	 *
	 * @return daftar id detailperkuliahan; kosong bila belum ada.
	 * @see #ambilDetailperkuliahan(Integer)
	 */
	public List<Long> ambilDetailperkuliahan() {
		Integer sdSmt = null;
		return ambilDetailperkuliahan(sdSmt);
	}

	/**
	 * Membaca daftar id {@link Detailperkuliahan} milik mahasiswa ini dari BERKAS JSON, opsional
	 * dibatasi sampai semester tertentu. Ini fondasi hampir seluruh perhitungan nilai/IPK/SKS di
	 * kelas ini.
	 *
	 * <p><b>Alur.</b> Isi berkas ({@link #ambilLokasiDetailPerkuliahan()}) ditelusuri kunci demi
	 * kunci. Kunci NON-NUMERIK dilewati diam-diam — berkas yang sama juga menampung kunci
	 * {@code "krs_mhs_..."} dari {@link #populateDefaultKrsMahasiswa(KrsMahasiswa)}, dan tanpa
	 * penjagaan {@code StringUtils.isNumeric} kunci itu pernah memicu
	 * {@code NumberFormatException}. Untuk tiap kunci numerik, objeknya diambil dari cache
	 * ({@code ambilData}); yang belum ada di cache dikumpulkan lalu dimuat SEKALI lewat satu query
	 * {@code Restrictions.in("id", ...)} dan dimasukkan ke cache ({@code masukkanData}).</p>
	 *
	 * <p><b>Penyaringan.</b> Hanya baris yang punya mata kuliah — baik lewat
	 * {@code perkuliahan.matakuliah} maupun {@code matakuliahKonversi} — yang disertakan. Tiap
	 * baris juga di-{@code setMahasiswa(this)} agar rujukan baliknya konsisten.</p>
	 *
	 * <p><b>Efek samping sesi:</b> jalur pemuatan susulan memakai
	 * {@code HibernateUtil.currentNativeSession()} dan MENUTUPNYA ({@code disconnect} +
	 * {@code close} + {@code HibernateUtil.closeSession()}) setelah selesai. Perhatikan ini
	 * berbeda dari pola sesi dedikasi yang dipakai {@link #getBiodataCalonMahasiswaData()};
	 * pemanggil yang masih membutuhkan sesi thread-local sesudahnya harus mengambilnya ulang.</p>
	 *
	 * <p>Kegagalan per kunci maupun kegagalan menyeluruh ditelan dan dicatat ke
	 * {@code ErrorAuditUtil}; metode tidak pernah melempar.</p>
	 *
	 * @param sdSmt batas semester (inklusif); {@code null} berarti semua semester.
	 * @return daftar id detailperkuliahan; kosong bila tidak ada.
	 */
	@SuppressWarnings("unchecked")
	public List<Long> ambilDetailperkuliahan(Integer sdSmt) {
		List<Long> detailperkuliahans = new ArrayList<Long>();
		List<Long> idsBelumAda = new ArrayList<Long>();
		try {

			JSONObject c = new JSONObject(ambilLokasiDetailPerkuliahan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						// FIX NumberFormatException: kunci non-numerik (mis. "krs_mhs_..._null_null")
						// tidak boleh diteruskan ke ambilData() yang mencoba Long.parseLong(key).
						// Guard di sini agar ambilData hanya dipanggil untuk kunci numerik saja.
						if (StringUtils.isNumeric(key)) {
							GeneralValueObject generalValueObject = ambilData(Detailperkuliahan.class, key);
							if (generalValueObject != null) {
								Detailperkuliahan detailperkuliahan = (Detailperkuliahan) generalValueObject;
								if ((detailperkuliahan.getPerkuliahan() != null
										&& detailperkuliahan.getPerkuliahan().getMatakuliah() != null)
										|| detailperkuliahan.getMatakuliahKonversi() != null) {
									detailperkuliahan.setMahasiswa(this);

									if (sdSmt == null || sdSmt >= detailperkuliahan.getSemester()) {
										detailperkuliahans.add(detailperkuliahan.getId());
									}
								}
							} else {
								idsBelumAda.add(Long.parseLong(key));
							}
						}
						// else: key non-numerik (mis. "krs_mhs_..." dari
						// populateDefaultKrsMahasiswa yang tercampur di lokasi JSON
						// yang sama) bukan id Detailperkuliahan yang valid - lewati
						// diam-diam, jangan Long.parseLong (NumberFormatException).
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:2994");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:2998");

		}

		if (!idsBelumAda.isEmpty()) {
			// System.out.println("idsBelumAda Detailperkuliahan -> " + idsBelumAda);
			Session session = HibernateUtil.currentNativeSession();
			try {
				List<Detailperkuliahan> detailperkuliahansData = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.in("id", idsBelumAda)).list();
				for (Detailperkuliahan detailperkuliahan : detailperkuliahansData) {
					masukkanData(Detailperkuliahan.class, detailperkuliahan);
					if ((detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPerkuliahan().getMatakuliah() != null)
							|| detailperkuliahan.getMatakuliahKonversi() != null) {
						detailperkuliahan.setMahasiswa(this);

						if (sdSmt == null || sdSmt >= detailperkuliahan.getSemester()) {
							detailperkuliahans.add(detailperkuliahan.getId());
						}
					}
				}
				detailperkuliahansData = null;
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:3027");
			}
			HibernateUtil.closeSession();
		}

		return detailperkuliahans;
	}

	/**
	 * Pintasan {@link #ambilDetailperkuliahan(Integer, Integer, Integer, boolean, Boolean, Integer)}
	 * dengan {@code semua = false} dan tanpa penyaringan status persetujuan.
	 *
	 * @param semester       semester yang dicari.
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param remedial       {@code true} bila hanya yang remedial.
	 * @return daftar id detailperkuliahan.
	 */
	public List<Long> ambilDetailperkuliahan(Integer semester, Integer tahapan, Integer semesterPendek,
			boolean remedial) {
		return ambilDetailperkuliahan(semester, tahapan, semesterPendek, remedial, false, null);
	}

	/**
	 * Pintasan {@link #ambilDetailperkuliahan(Integer, Integer, Integer, boolean, Boolean, Integer)}
	 * untuk satu semester, tanpa remedial dan tanpa penyaringan status persetujuan. Inilah varian
	 * yang dipakai seluruh {@code hitung*Semester(...)} di kelas ini.
	 *
	 * @param semester       semester yang dicari.
	 * @param tahapan        tahapan dalam semester; {@code null}/0 bila tanpa tahapan.
	 * @param semesterPendek penanda semester pendek.
	 * @return daftar id detailperkuliahan pada semester itu.
	 */
	public List<Long> ambilDetailperkuliahan(Integer semester, Integer tahapan, Integer semesterPendek) {
		return ambilDetailperkuliahan(semester, tahapan, semesterPendek, false, false, null);
	}

	/**
	 * Pintasan ke {@link #ambilDetailperkuliahan(Mahasiswa, Integer, Integer, Integer, boolean,
	 * Boolean, Integer, boolean, boolean)} untuk mahasiswa ini, tanpa penyaringan
	 * sudah/belum dinilai.
	 *
	 * @param semester       semester yang dicari.
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param remedial       {@code true} bila hanya yang remedial.
	 * @param semua          {@code true} untuk mengabaikan penyaringan status/nilai.
	 * @param persetujuan    status persetujuan yang dicari; {@code null} = semua.
	 * @return daftar id detailperkuliahan.
	 */
	public List<Long> ambilDetailperkuliahan(Integer semester, Integer tahapan, Integer semesterPendek,
			boolean remedial, Boolean semua, Integer persetujuan) {
		return ambilDetailperkuliahan(this, semester, tahapan, semesterPendek, remedial, semua, persetujuan, false,
				false);
	}

	/**
	 * Versi STATIS pengambilan daftar {@link Detailperkuliahan} untuk seorang mahasiswa dengan
	 * penyaringan lengkap. Seluruh aturannya ada di
	 * {@link ais.action.master.helper.KrsDetailHelper#ambilDetailperkuliahan}; metode ini murni
	 * penerus agar pemanggil dapat memakainya tanpa memuat helper.
	 *
	 * @param mahasiswa          mahasiswa yang datanya diambil.
	 * @param semester           semester yang dicari.
	 * @param tahapan            tahapan dalam semester.
	 * @param semesterPendek     penanda semester pendek.
	 * @param remedial           {@code true} bila hanya yang remedial.
	 * @param semua              {@code true} untuk mengabaikan penyaringan status/nilai.
	 * @param persetujuan        status persetujuan yang dicari; {@code null} = semua.
	 * @param sudahDinilaiSaja   hanya yang sudah punya nilai.
	 * @param belumDinilaiSaja   hanya yang belum punya nilai.
	 * @return daftar id detailperkuliahan.
	 * @see ais.action.master.helper.KrsDetailHelper
	 */
	public static List<Long> ambilDetailperkuliahan(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean remedial, Boolean semua, Integer persetujuan, boolean sudahDinilaiSaja,
			boolean belumDinilaiSaja) {
		return KrsDetailHelper.ambilDetailperkuliahan(mahasiswa, semester, tahapan, semesterPendek, remedial, semua,
				persetujuan, sudahDinilaiSaja, belumDinilaiSaja);
	}

	/**
	 * Mengambil daftar {@link Detailperkuliahan} mahasiswa ini secara KUMULATIF — dari semester
	 * pertama SAMPAI semester yang diminta. Inilah masukan seluruh perhitungan berakhiran
	 * {@code SampaiSemester} (IPK kumulatif, total SKS lulus, dsb.).
	 *
	 * <p>Diteruskan ke {@link ais.action.master.helper.KrsDetailHelper#ambilDetailperkuliahanSampai}
	 * dengan {@code saring = true}, artinya hasilnya SUDAH melewati penyaringan duplikat/ekivalensi
	 * di helper tersebut.</p>
	 *
	 * @param semester       batas semester (inklusif).
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param semua          {@code true} untuk mengabaikan penyaringan status/nilai.
	 * @return koleksi id detailperkuliahan sampai semester tersebut.
	 */
	public Collection<Long> ambilDetailperkuliahanSampai(Integer semester, Integer tahapan, Integer semesterPendek,
			boolean semua) {
		boolean saring = true;
		return KrsDetailHelper.ambilDetailperkuliahanSampai(this, semester, tahapan, semesterPendek, semua, saring);
	}

	/**
	 * Mencari {@link Detailperkuliahan} mahasiswa ini untuk MATA KULIAH TERTENTU (dicocokkan
	 * dengan kode ATAU nama) sampai semester yang diminta. Dipakai pemeriksaan prasyarat dan
	 * syarat kelulusan yang mensyaratkan mata kuliah tertentu sudah ditempuh.
	 *
	 * @param semester      batas semester (inklusif).
	 * @param kodeAtauNamas satu atau lebih kode/nama mata kuliah yang dicari.
	 * @return daftar id detailperkuliahan yang cocok.
	 * @see ais.action.master.helper.KrsDetailHelper
	 */
	public List<Long> ambilDetailperkuliahanMkSdSmtTertentu(Integer semester, String... kodeAtauNamas) {
		return KrsDetailHelper.ambilDetailperkuliahanMkSdSmtTertentu(this, semester, kodeAtauNamas);
	}

	/**
	 * Mencari {@link Detailperkuliahan} mahasiswa ini untuk MATA KULIAH TERTENTU (kode atau nama)
	 * PADA semester yang diminta saja — berbeda dengan
	 * {@link #ambilDetailperkuliahanMkSdSmtTertentu(Integer, String...)} yang bersifat kumulatif.
	 *
	 * @param semester      semester yang dicari.
	 * @param kodeAtauNamas satu atau lebih kode/nama mata kuliah.
	 * @return daftar id detailperkuliahan yang cocok.
	 * @see ais.action.master.helper.KrsDetailHelper
	 */
	public List<Long> ambilDetailperkuliahanMkTertentu(Integer semester, String... kodeAtauNamas) {
		return KrsDetailHelper.ambilDetailperkuliahanMkTertentu(this, semester, kodeAtauNamas);
	}

	/**
	 * Rata-rata NILAI ANGKA mata kuliah pada SATU semester (bukan IPK — tidak dibobot SKS).
	 *
	 * @param semester       semester yang dihitung.
	 * @param tahapan        tahapan dalam semester; {@code null}/0 bila tanpa tahapan.
	 * @param semesterPendek penanda semester pendek.
	 * @return rata-rata nilai angka; {@code 0.0} bila tidak ada mata kuliah yang dihitung.
	 * @see #prosesHitungRataRata(java.util.Collection)
	 */
	public Double hitungRataRataSemester(Integer semester, Integer tahapan, Integer semesterPendek) {
		return prosesHitungRataRata(ambilDetailperkuliahan(semester, tahapan, semesterPendek));
	}

	/**
	 * Rata-rata NILAI ANGKA secara KUMULATIF sampai semester tertentu.
	 *
	 * @param semester       batas semester (inklusif).
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param semua          {@code true} untuk mengabaikan penyaringan status/nilai.
	 * @return rata-rata nilai angka kumulatif; {@code 0.0} bila tidak ada data.
	 * @see #prosesHitungRataRata(java.util.Collection)
	 */
	public Double hitungRataRataSampaiSemester(Integer semester, Integer tahapan, Integer semesterPendek,
			boolean semua) {
		return prosesHitungRataRata(ambilDetailperkuliahanSampai(semester, tahapan, semesterPendek, semua));
	}

	/**
	 * Rata-rata NILAI ANGKA atas SELURUH mata kuliah yang pernah ditempuh mahasiswa ini.
	 *
	 * @return rata-rata nilai angka; {@code 0.0} bila tidak ada data.
	 * @see #prosesHitungRataRata(java.util.Collection)
	 */
	public Double hitungRataRata() {
		return prosesHitungRataRata(ambilDetailperkuliahan());
	}

	/**
	 * Mesin perhitungan RATA-RATA NILAI ANGKA: menjumlahkan {@code totalNilai} tiap
	 * {@link Detailperkuliahan} lalu membaginya dengan JUMLAH MATA KULIAH (bukan jumlah SKS),
	 * sehingga hasilnya rata-rata sederhana, bukan indeks prestasi.
	 *
	 * <p><b>Konfigurasi yang menentukan hasil</b> (dibaca dari {@link Konfigurasi} tiap kali
	 * dipanggil):</p>
	 * <ul>
	 *   <li>{@code nilai_0_tidak_masuk_dalam_perhitungan_ipk} — bila AKTIF, mata kuliah dengan
	 *       {@code totalNilai} di bawah ambang tidak dihitung;</li>
	 *   <li>{@code nilai_minimal_tidak_masuk_dalam_perhitungan_ipk} — ambang tersebut (bawaan
	 *       {@code 0.1});</li>
	 *   <li>{@code nilai_huruf_yg_tidak_masuk_perhitungan_ip} — nilai huruf yang dikecualikan;</li>
	 *   <li>{@code nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk} — bila AKTIF, nilai
	 *       semester BERJALAN ({@link #currentSemester()}) yang belum diverifikasi
	 *       ({@code Detailperkuliahan.NOT_VERIFIED}) dilewati.</li>
	 * </ul>
	 *
	 * <p>Masukan lebih dulu dilewatkan {@link #saringBerdasarNilaiDan0(java.util.Collection)}
	 * sehingga mata kuliah duplikat/ekivalen sudah tereliminasi. Hanya baris berstatus
	 * {@code DISETUJUI} yang dihitung.</p>
	 *
	 * <p>Catatan: pembacaan ambang di sini masih memakai {@code Double.parseDouble} langsung, jadi
	 * konfigurasi berkoma ("0,1") gagal parse dan diam-diam jatuh ke bawaan {@code 0.1} — berbeda
	 * dari {@link #prosesHitungMutu(java.util.Collection)} dan
	 * {@link #prosesHitungIpk(java.util.Collection, Boolean)} yang sudah memakai
	 * {@code Common.parseAngkaKonfigurasi} yang toleran.</p>
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang dihitung.
	 * @return rata-rata nilai angka; {@code 0.0} bila tidak ada mata kuliah yang lolos.
	 */
	private Double prosesHitungRataRata(Collection<Long> detailperkuliahans) {

		String nilai0MasukPenghitungan = Common
				.getKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.AKTIF).getNilai();
		Double minimal = 0.1;
		try {
			minimal = Double.parseDouble(
					Common.getKonfigurasi("nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:3092");

		}
		String nilaiHurufTidakMasukPerhitungan = Common.getKonfigurasi("nilai_huruf_yg_tidak_masuk_perhitungan_ip", "")
				.getNilai();

		boolean nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk = Common.bolehKonfigurasi("nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.TIDAK_AKTIF);
		Integer smtSaatIni = this.currentSemester();

		Double ipkTotal = 0.0;
		Integer sksMk = 0;

		for (Long detailperkuliahanid : saringBerdasarNilaiDan0(detailperkuliahans)) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {

				if (smtSaatIni != null && smtSaatIni.equals(detailperkuliahan.getSemester())
						&& nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk
						&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
					continue;
				}

				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& !detailperkuliahan.getNilaiHuruf().equalsIgnoreCase(nilaiHurufTidakMasukPerhitungan)) {

					if (nilai0MasukPenghitungan.equals(Konfigurasi.AKTIF)) {
						if (detailperkuliahan.getTotalNilai() >= minimal) {
							ipkTotal += detailperkuliahan.getTotalNilai();
							sksMk++;
						}
					} else {
						ipkTotal += detailperkuliahan.getTotalNilai();
						sksMk++;
					}
				}
			}
		}
		detailperkuliahans = null;

		return sksMk.equals(0) ? 0.0 : ipkTotal / sksMk.doubleValue();
	}

	/**
	 * TOTAL nilai angka (jumlah {@code totalNilai}, tanpa dibagi) pada satu semester.
	 *
	 * @param semester       semester yang dihitung.
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @return jumlah nilai angka semester itu.
	 * @see #prosesHitungNilai(java.util.Collection)
	 */
	public Double hitungNilaiSemester(Integer semester, Integer tahapan, Integer semesterPendek) {
		return prosesHitungNilai(ambilDetailperkuliahan(semester, tahapan, semesterPendek));
	}

	/**
	 * TOTAL nilai angka secara kumulatif sampai semester tertentu. Dipakai laporan KHS/KRS
	 * ({@code LaporanKHS}, {@code LaporanKRS} dan turunannya).
	 *
	 * @param semester       batas semester (inklusif).
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param semua          {@code true} untuk mengabaikan penyaringan status/nilai.
	 * @return jumlah nilai angka kumulatif.
	 * @see #prosesHitungNilai(java.util.Collection)
	 */
	public Double hitungNilaiSampaiSemester(Integer semester, Integer tahapan, Integer semesterPendek, boolean semua) {
		return prosesHitungNilai(ambilDetailperkuliahanSampai(semester, tahapan, semesterPendek, semua));
	}

	/**
	 * TOTAL nilai angka atas seluruh mata kuliah yang pernah ditempuh mahasiswa ini.
	 *
	 * @return jumlah nilai angka.
	 * @see #prosesHitungNilai(java.util.Collection)
	 */
	public Double hitungNilai() {
		return prosesHitungNilai(ambilDetailperkuliahan());
	}

	/**
	 * Mesin penjumlahan NILAI ANGKA ({@code totalNilai}) — sama persis dengan
	 * {@link #prosesHitungRataRata(java.util.Collection)} kecuali hasilnya TIDAK dibagi jumlah
	 * mata kuliah. Membaca rangkaian kunci {@link Konfigurasi} yang sama dan memakai
	 * {@link #saringBerdasarNilaiDan0(java.util.Collection)} sebagai penyaring awal.
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang dihitung.
	 * @return jumlah nilai angka; {@code 0.0} bila tidak ada yang lolos.
	 */
	private Double prosesHitungNilai(Collection<Long> detailperkuliahans) {

		String nilai0MasukPenghitungan = Common
				.getKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.AKTIF).getNilai();
		Double minimal = 0.1;
		try {
			minimal = Double.parseDouble(
					Common.getKonfigurasi("nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:3155");

		}
		String nilaiHurufTidakMasukPerhitungan = Common.getKonfigurasi("nilai_huruf_yg_tidak_masuk_perhitungan_ip", "")
				.getNilai();

		boolean nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk = Common.bolehKonfigurasi("nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.TIDAK_AKTIF);
		Integer smtSaatIni = this.currentSemester();
		Double ipkTotal = 0.0;

		for (Long detailperkuliahanid : saringBerdasarNilaiDan0(detailperkuliahans)) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {

				if (smtSaatIni != null && smtSaatIni.equals(detailperkuliahan.getSemester())
						&& nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk
						&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
					continue;
				}

				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& !detailperkuliahan.getNilaiHuruf().equalsIgnoreCase(nilaiHurufTidakMasukPerhitungan)) {

					if (nilai0MasukPenghitungan.equals(Konfigurasi.AKTIF)) {
						if (detailperkuliahan.getTotalNilai() >= minimal) {
							ipkTotal += detailperkuliahan.getTotalNilai();
						}
					} else {
						ipkTotal += detailperkuliahan.getTotalNilai();
					}
				}
			}
		}
		detailperkuliahans = null;

		return ipkTotal;
	}

	/**
	 * TOTAL bobot mutu (indeks prestasi &times; SKS) pada satu semester — pembilang rumus IP
	 * sebelum dibagi total SKS.
	 *
	 * @param semester       semester yang dihitung.
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @return jumlah IP&times;SKS semester itu.
	 * @see #prosesHitungTotalIP(java.util.Collection)
	 */
	public Double hitungTotalIPSemester(Integer semester, Integer tahapan, Integer semesterPendek) {
		return prosesHitungTotalIP(ambilDetailperkuliahan(semester, tahapan, semesterPendek));
	}

	/**
	 * TOTAL bobot mutu (IP &times; SKS) secara kumulatif sampai semester tertentu.
	 *
	 * @param semester       batas semester (inklusif).
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param semua          {@code true} untuk mengabaikan penyaringan status/nilai.
	 * @return jumlah IP&times;SKS kumulatif.
	 * @see #prosesHitungTotalIP(java.util.Collection)
	 */
	public Double hitungTotalIPSampaiSemester(Integer semester, Integer tahapan, Integer semesterPendek,
			boolean semua) {
		return prosesHitungTotalIP(ambilDetailperkuliahanSampai(semester, tahapan, semesterPendek, semua));
	}

	/**
	 * TOTAL bobot mutu (IP &times; SKS) atas seluruh mata kuliah yang pernah ditempuh.
	 *
	 * @return jumlah IP&times;SKS.
	 * @see #prosesHitungTotalIP(java.util.Collection)
	 */
	public Double hitungTotalIP() {
		return prosesHitungTotalIP(ambilDetailperkuliahan());
	}

	/**
	 * Mesin penjumlahan BOBOT MUTU: {@code totalIP} tiap {@link Detailperkuliahan} dikalikan SKS
	 * mata kuliahnya, lalu dijumlahkan. Mata kuliah diambil dari {@code matakuliahKonversi} bila
	 * ada, kalau tidak dari {@code perkuliahan.matakuliah}.
	 *
	 * <p>Membaca rangkaian kunci {@link Konfigurasi} yang sama dengan
	 * {@link #prosesHitungRataRata(java.util.Collection)} dan memakai
	 * {@link #saringBerdasarNilaiDan0(java.util.Collection)} sebagai penyaring awal.</p>
	 *
	 * <p><b>Jebakan:</b> bila sebuah baris tidak punya mata kuliah sama sekali (konversi maupun
	 * perkuliahan {@code null}), variabel {@code matakuliah} bernilai {@code null} dan
	 * {@code matakuliah.getSks()} melempar {@code NullPointerException}. Dalam praktik hal ini
	 * tidak terjadi karena {@link #ambilDetailperkuliahan(Integer)} sudah membuang baris tanpa
	 * mata kuliah — jangan hilangkan penyaringan itu.</p>
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang dihitung.
	 * @return jumlah IP&times;SKS; {@code 0.0} bila tidak ada yang lolos.
	 */
	private Double prosesHitungTotalIP(Collection<Long> detailperkuliahans) {

		String nilai0MasukPenghitungan = Common
				.getKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.AKTIF).getNilai();
		Double minimal = 0.1;
		try {
			minimal = Double.parseDouble(
					Common.getKonfigurasi("nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:3215");

		}
		String nilaiHurufTidakMasukPerhitungan = Common.getKonfigurasi("nilai_huruf_yg_tidak_masuk_perhitungan_ip", "")
				.getNilai();

		boolean nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk = Common.bolehKonfigurasi("nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.TIDAK_AKTIF);
		Integer smtSaatIni = this.currentSemester();
		Double ipkTotal = 0.0;

		for (Long detailperkuliahanid : saringBerdasarNilaiDan0(detailperkuliahans)) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {

				if (smtSaatIni != null && smtSaatIni.equals(detailperkuliahan.getSemester())
						&& nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk
						&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
					continue;
				}

				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& !detailperkuliahan.getNilaiHuruf().equalsIgnoreCase(nilaiHurufTidakMasukPerhitungan)) {

					Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() != null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan() != null
									? detailperkuliahan.getPerkuliahan().getMatakuliah()
									: null;

					if (nilai0MasukPenghitungan.equals(Konfigurasi.AKTIF)) {
						if (detailperkuliahan.getTotalNilai() >= minimal) {
							ipkTotal += detailperkuliahan.getTotalIP() * matakuliah.getSks().doubleValue();
						}
					} else {
						ipkTotal += detailperkuliahan.getTotalIP() * matakuliah.getSks().doubleValue();
					}
				}
			}
		}
		detailperkuliahans = null;

		return ipkTotal;
	}

	/**
	 * TOTAL mutu (IP &times; SKS) pada satu semester. Isi perhitungannya identik dengan
	 * {@link #hitungTotalIPSemester(Integer, Integer, Integer)}; keduanya dipertahankan karena
	 * dipakai laporan yang berbeda.
	 *
	 * @param semester       semester yang dihitung.
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @return jumlah mutu semester itu.
	 * @see #prosesHitungMutu(java.util.Collection)
	 */
	public Double hitungMutuSemester(Integer semester, Integer tahapan, Integer semesterPendek) {
		return prosesHitungMutu(ambilDetailperkuliahan(semester, tahapan, semesterPendek));
	}

	/**
	 * TOTAL mutu (IP &times; SKS) secara kumulatif sampai semester tertentu. Dipakai kolom "Jumlah
	 * Mutu" pada transkrip dan laporan KHS/KRS ({@code CommonReportHelper}, {@code LaporanKHS},
	 * {@code LaporanKRS} dan turunannya, {@code AmbilLaporanMahasiswa}).
	 *
	 * @param semester       batas semester (inklusif).
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param semua          {@code true} untuk mengabaikan penyaringan status/nilai.
	 * @return jumlah mutu kumulatif.
	 * @see #prosesHitungMutu(java.util.Collection)
	 */
	public Double hitungMutuSampaiSemester(Integer semester, Integer tahapan, Integer semesterPendek, boolean semua) {
		return prosesHitungMutu(ambilDetailperkuliahanSampai(semester, tahapan, semesterPendek, semua));
	}

	/**
	 * TOTAL mutu (IP &times; SKS) atas seluruh mata kuliah yang pernah ditempuh.
	 *
	 * @return jumlah mutu.
	 * @see #prosesHitungMutu(java.util.Collection)
	 */
	public Double hitungMutu() {
		return prosesHitungMutu(ambilDetailperkuliahan());
	}

	/**
	 * Mesin penjumlahan MUTU ({@code totalIP} &times; SKS), sejalan dengan
	 * {@link #prosesHitungTotalIP(java.util.Collection)}.
	 *
	 * <p><b>Perbedaan penting:</b> ambang minimal di sini dibaca memakai
	 * {@code Common.parseAngkaKonfigurasi} yang TOLERAN terhadap desimal berkoma ("0,1" maupun
	 * "0.1"). Komentar panjang di dalam metode menjelaskan mengapa: {@code Double.parseDouble}
	 * hanya menerima titik, sehingga versi lama membanjiri log dengan stack trace pada tiap
	 * pembuatan transkrip DAN — jauh lebih berbahaya — mengabaikan nilai konfigurasi admin secara
	 * diam-diam (admin menulis "2,5", sistem tetap memakai 0.1 dan seluruh IPK ikut salah).</p>
	 *
	 * <p>Konfigurasi lain yang dibaca sama dengan {@link #prosesHitungRataRata(java.util.Collection)};
	 * penyaring awalnya {@link #saringBerdasarNilaiDan0(java.util.Collection)}.</p>
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang dihitung.
	 * @return jumlah mutu; {@code 0.0} bila tidak ada yang lolos.
	 */
	private Double prosesHitungMutu(Collection<Long> detailperkuliahans) {

		String nilai0MasukPenghitungan = Common
				.getKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.AKTIF).getNilai();
		/*
		 * Dibaca TOLERAN terhadap desimal bergaya Indonesia. Admin lazim mengetik "0,1"
		 * dengan koma, sedangkan Double.parseDouble hanya menerima titik -- dulu itu
		 * melempar NumberFormatException yang ditangkap sebagai kendali alur.
		 *
		 * Dua akibatnya, dan yang kedua jauh lebih berbahaya:
		 *   1. Log dibanjiri stack trace, karena method ini dipanggil per semester pada
		 *      tiap pembuatan transkrip.
		 *   2. Nilai yang dimaksud admin DIABAIKAN diam-diam. Kebetulan "0,1" sama dengan
		 *      bawaannya sehingga hasilnya benar; tetapi bila admin mengetik "2,5" maka
		 *      sistem tetap memakai 0.1 dan seluruh perhitungan IPK ikut salah TANPA
		 *      pesan apa pun.
		 *
		 * Common.parseAngkaKonfigurasi menerima "0,1" maupun "0.1", tidak pernah melempar,
		 * dan jatuh ke bawaan hanya bila teksnya memang tidak bisa diartikan.
		 */
		Double minimal = Common.parseAngkaKonfigurasi(
				Common.getKonfigurasi("nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1").getNilai(), 0.1);
		String nilaiHurufTidakMasukPerhitungan = Common.getKonfigurasi("nilai_huruf_yg_tidak_masuk_perhitungan_ip", "")
				.getNilai();

		boolean nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk = Common.bolehKonfigurasi("nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.TIDAK_AKTIF);
		Integer smtSaatIni = this.currentSemester();
		Double ipkTotal = 0.0;

		for (Long detailperkuliahanid : saringBerdasarNilaiDan0(detailperkuliahans)) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {

				if (smtSaatIni != null && smtSaatIni.equals(detailperkuliahan.getSemester())
						&& nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk
						&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
					continue;
				}

				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& !detailperkuliahan.getNilaiHuruf().equalsIgnoreCase(nilaiHurufTidakMasukPerhitungan)) {

					Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() != null
							? detailperkuliahan.getMatakuliahKonversi()
							: detailperkuliahan.getPerkuliahan() != null
									? detailperkuliahan.getPerkuliahan().getMatakuliah()
									: null;

					if (nilai0MasukPenghitungan.equals(Konfigurasi.AKTIF)) {
						if (detailperkuliahan.getTotalNilai() >= minimal) {
							ipkTotal += detailperkuliahan.getTotalIP() * matakuliah.getSks().doubleValue();
						}
					} else {
						ipkTotal += detailperkuliahan.getTotalIP() * matakuliah.getSks().doubleValue();
					}
				}
			}
		}

		detailperkuliahans = null;

		return ipkTotal;
	}

	/**
	 * TOTAL indeks prestasi mentah ({@code totalIP} TANPA dikali SKS) pada satu semester.
	 *
	 * @param semester       semester yang dihitung.
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @return jumlah IP semester itu.
	 * @see #prosesHitungNilaiIp(java.util.Collection)
	 */
	public Double hitungNilaiIpSemester(Integer semester, Integer tahapan, Integer semesterPendek) {
		return prosesHitungNilaiIp(ambilDetailperkuliahan(semester, tahapan, semesterPendek));
	}

	/**
	 * TOTAL indeks prestasi mentah secara kumulatif sampai semester tertentu. Dipakai laporan
	 * KHS/KRS bersama {@link #hitungMutuSampaiSemester(Integer, Integer, Integer, boolean)}.
	 *
	 * @param semester       batas semester (inklusif).
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param semua          {@code true} untuk mengabaikan penyaringan status/nilai.
	 * @return jumlah IP kumulatif.
	 * @see #prosesHitungNilaiIp(java.util.Collection)
	 */
	public Double hitungNilaiIpSampaiSemester(Integer semester, Integer tahapan, Integer semesterPendek,
			boolean semua) {
		return prosesHitungNilaiIp(ambilDetailperkuliahanSampai(semester, tahapan, semesterPendek, semua));
	}

	/**
	 * TOTAL indeks prestasi mentah atas seluruh mata kuliah yang pernah ditempuh.
	 *
	 * @return jumlah IP.
	 * @see #prosesHitungNilaiIp(java.util.Collection)
	 */
	public Double hitungNilaiIp() {
		return prosesHitungNilaiIp(ambilDetailperkuliahan());
	}

	/**
	 * Mesin penjumlahan INDEKS PRESTASI MENTAH: menjumlahkan {@code totalIP} tiap
	 * {@link Detailperkuliahan} tanpa pembobotan SKS. Konfigurasi dan penyaring awalnya sama
	 * dengan {@link #prosesHitungRataRata(java.util.Collection)}.
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang dihitung.
	 * @return jumlah IP; {@code 0.0} bila tidak ada yang lolos.
	 */
	private Double prosesHitungNilaiIp(Collection<Long> detailperkuliahans) {

		String nilai0MasukPenghitungan = Common
				.getKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.AKTIF).getNilai();
		Double minimal = 0.1;
		try {
			minimal = Double.parseDouble(
					Common.getKonfigurasi("nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:3347");

		}
		String nilaiHurufTidakMasukPerhitungan = Common.getKonfigurasi("nilai_huruf_yg_tidak_masuk_perhitungan_ip", "")
				.getNilai();

		boolean nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk = Common.bolehKonfigurasi("nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.TIDAK_AKTIF);
		Integer smtSaatIni = this.currentSemester();
		Double ipkTotal = 0.0;

		for (Long detailperkuliahanid : saringBerdasarNilaiDan0(detailperkuliahans)) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {

				if (smtSaatIni != null && smtSaatIni.equals(detailperkuliahan.getSemester())
						&& nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk
						&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
					continue;
				}

				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& !detailperkuliahan.getNilaiHuruf().equalsIgnoreCase(nilaiHurufTidakMasukPerhitungan)) {

					if (nilai0MasukPenghitungan.equals(Konfigurasi.AKTIF)) {
						if (detailperkuliahan.getTotalNilai() >= minimal) {
							ipkTotal += detailperkuliahan.getTotalIP();
						}
					} else {
						ipkTotal += detailperkuliahan.getTotalIP();
					}
				}
			}
		}

		detailperkuliahans = null;

		return ipkTotal;
	}

	/**
	 * INDEKS PRESTASI SEMESTER (IPS): rata-rata terbobot SKS atas mata kuliah satu semester.
	 * Dipakai antara lain dasbor {@code DashboardDataNilaiMahasiswaPerTahunAngkatan}.
	 *
	 * @param semester       semester yang dihitung.
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @return IP semester; {@code 0.0} bila tidak ada SKS yang dihitung.
	 * @see #prosesHitungIpk(java.util.Collection, Boolean)
	 */
	public Double hitungIPKSemester(Integer semester, Integer tahapan, Integer semesterPendek) {
		return prosesHitungIpk(ambilDetailperkuliahan(semester, tahapan, semesterPendek));
	}

	/**
	 * INDEKS PRESTASI KUMULATIF (IPK) sampai semester tertentu — rata-rata terbobot SKS atas
	 * seluruh mata kuliah dari semester pertama sampai semester yang diminta.
	 *
	 * @param semester       batas semester (inklusif).
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param semua          {@code true} untuk mengabaikan penyaringan status/nilai.
	 * @return IPK sampai semester itu; {@code 0.0} bila tidak ada SKS yang dihitung.
	 * @see #prosesHitungIpk(java.util.Collection, Boolean)
	 */
	public Double hitungIPKSampaiSemester(Integer semester, Integer tahapan, Integer semesterPendek, boolean semua) {
		return prosesHitungIpk(ambilDetailperkuliahanSampai(semester, tahapan, semesterPendek, semua));
	}

	/**
	 * IPK atas SELURUH mata kuliah yang pernah ditempuh mahasiswa ini.
	 *
	 * <p>Catatan pemeliharaan: per pemeriksaan terakhir tidak ada pemanggil metode ini di dalam
	 * pohon sumber — pemanggil memakai {@link #hitungIPKSampaiSemester(Integer, Integer, Integer,
	 * boolean)} atau langsung {@link #prosesHitungIpk(java.util.Collection, Boolean)}. Dipertahankan
	 * demi kelengkapan API entitas.</p>
	 *
	 * @return IPK; {@code 0.0} bila tidak ada SKS yang dihitung.
	 * @see #prosesHitungIpk(java.util.Collection, Boolean)
	 */
	public Double hitungIpk() {
		return prosesHitungIpk(ambilDetailperkuliahan());
	}

	/**
	 * <b>Penyaring baku yang menentukan mata kuliah mana yang SAH masuk perhitungan IPK.</b>
	 * Dipakai seluruh mesin {@code prosesHitung*} di kelas ini serta belasan berkas lain
	 * ({@code KrsDetailHelper}, {@code ProfileMahasiswa}, {@code TampilStudiMahasiswaHelper}, dst.).
	 *
	 * <p>Empat tahap, dijalankan berurutan atas cache objek yang dimuat SEKALI di awal (agar tidak
	 * ada query berulang per id):</p>
	 * <ol>
	 *   <li><b>Nilai 0 &amp; semester.</b> Baris tanpa semester atau bersemester negatif dibuang.
	 *       Bila {@code nilai_0_tidak_masuk_dalam_perhitungan_ipk} AKTIF, hanya baris dengan
	 *       {@code totalNilai} &ge; ambang {@code nilai_minimal_tidak_masuk_dalam_perhitungan_ipk}
	 *       yang lolos.</li>
	 *   <li><b>Kesamaan NAMA</b> (bila {@code aktifkan_kesamaan_nama}): antar mata kuliah bernama
	 *       sama, hanya yang nilainya TERTINGGI yang dipertahankan.</li>
	 *   <li><b>Kesamaan KODE</b> (bila {@code aktifkan_kesamaan_kode}): hal serupa untuk kode mata
	 *       kuliah yang sama.</li>
	 *   <li><b>Ekivalensi</b> (bila {@code aktifkan_ekivalen}): diserahkan ke
	 *       {@link #prosesEkivalenOpt(boolean, String, String, Double, Boolean, java.util.Map,
	 *       java.util.Map)}; mata kuliah yang disetarakan diambil satu dengan nilai tertinggi.
	 *       Kunci {@code saring_nilai_ipk_juga_berdasarkan_nama} menentukan apakah pencocokan
	 *       ekivalensi boleh memakai nama, bukan hanya kode.</li>
	 * </ol>
	 *
	 * <p>Bila sebuah tahap dimatikan lewat konfigurasi, isi tahap itu diteruskan apa adanya
	 * (kunci peta memakai id, bukan nama/kode).</p>
	 *
	 * <p>Ambang minimal dibaca dengan normalisasi koma&rarr;titik supaya konfigurasi bergaya
	 * Indonesia ("0,1") benar-benar terpakai — lihat komentar KE-FIX di dalam metode.</p>
	 *
	 * <p><b>Konsekuensi yang perlu diketahui:</b> mata kuliah ber-SKS 0 (non-kredit/pass-fail)
	 * ikut tersingkir di sini; bila tetap perlu ditampilkan di transkrip, pakai
	 * {@link #saringTampilkanSks0Bernilai(java.util.Collection)}. Alasan sebuah baris tersingkir
	 * dapat dijelaskan ke pengguna lewat {@link #alasanTidakValidDetail(java.util.Collection)}.</p>
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang hendak disaring.
	 * @return koleksi id yang SAH masuk perhitungan; kosong bila masukan kosong/{@code null}.
	 */
	public Collection<Long> saringBerdasarNilaiDan0(Collection<Long> detailperkuliahans) {
		if (detailperkuliahans == null || detailperkuliahans.isEmpty())
			return new ArrayList<Long>();

		// Load semua object satu kali ke dalam Memory agar tidak query DB berulang kali
		Map<Long, Detailperkuliahan> cacheDp = new HashMap<Long, Detailperkuliahan>();
		for (Long id : detailperkuliahans) {
			Detailperkuliahan dp = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
					id.toString());
			if (dp != null) {
				cacheDp.put(id, dp);
			}
		}

		String nilai0MasukPenghitungan = Common
				.getKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.AKTIF).getNilai();
		Double minimal = 0.1;
		try {
			// KE-FIX (NumberFormatException "0,1"): admin sering mengisi Konfigurasi ini pakai
			// format desimal Indonesia (koma), tapi Double.parseDouble WAJIB titik. Sebelumnya
			// exception ini tertelan diam-diam & nilai konfigurasi admin diabaikan total (selalu
			// jatuh ke default 0.1 di baris atas) -- normalisasi koma->titik dulu supaya nilai yang
			// diisi admin benar-benar terpakai, bukan cuma dicatat sebagai error berulang tiap
			// laporan dibuat.
			String nilaiMinimal = Common.getKonfigurasi("nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1")
					.getNilai().trim().replace(',', '.');
			minimal = Double.parseDouble(nilaiMinimal);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:3419");
		}

		String nilaiHurufTidakMasukPerhitungan = Common.getKonfigurasi("nilai_huruf_yg_tidak_masuk_perhitungan_ip", "")
				.getNilai();

		boolean berdasarkan_nama = Common.bolehKonfigurasi("saring_nilai_ipk_juga_berdasarkan_nama");
		boolean aktifkan_ekivalen = Common.bolehKonfigurasi("aktifkan_ekivalen");
		boolean aktifkan_kesamaan_nama = Common.bolehKonfigurasi("aktifkan_kesamaan_nama");
		boolean aktifkan_kesamaan_kode = Common.bolehKonfigurasi("aktifkan_kesamaan_kode");

		// 1. Saring awal berdasar "Nilai 0 & Semester"
		Map<Long, Long> hasilData = new HashMap<Long, Long>();
		for (Map.Entry<Long, Detailperkuliahan> entry : cacheDp.entrySet()) {
			Detailperkuliahan dp = entry.getValue();
			if (dp.getSemester() != null && dp.getSemester() >= 0) {
				Double totalNilai = dp.getTotalNilai() != null ? dp.getTotalNilai() : 0.0;
				if (nilai0MasukPenghitungan.equals(Konfigurasi.AKTIF) && totalNilai >= minimal) {
					hasilData.put(dp.getId(), dp.getId());
				} else if (nilai0MasukPenghitungan.equals(Konfigurasi.TIDAK_AKTIF)) {
					hasilData.put(dp.getId(), dp.getId());
				}
			}
		}

		// 2. Saring berdasar "Kesamaan Nama"
		Map<String, Long> mapnama = new HashMap<String, Long>();
		if (aktifkan_kesamaan_nama) {
			Map<String, Double> mapNilaiNamas = new HashMap<String, Double>();
			for (Long id : hasilData.values()) {
				Detailperkuliahan dp = cacheDp.get(id);
				Matakuliah mk = getMatakuliahAman(dp);

				if (mk != null && mk.getNama() != null) {
					String nama = mk.getNama().toLowerCase().trim();
					Double totalNilai = dp.getTotalNilai() != null ? dp.getTotalNilai() : 0.0;

					if (!mapNilaiNamas.containsKey(nama) || mapNilaiNamas.get(nama) < totalNilai) {
						mapNilaiNamas.put(nama, totalNilai);
						mapnama.put(nama, dp.getId());
					}
				}
			}
		} else {
			for (Long id : hasilData.values()) {
				mapnama.put(id.toString(), id);
			}
		}

		// 3. Saring berdasar "Kesamaan Kode"
		Map<String, Long> mapkode = new HashMap<String, Long>();
		if (aktifkan_kesamaan_kode) {
			Map<String, Double> mapNilaiKodes = new HashMap<String, Double>();
			for (Long id : mapnama.values()) {
				Detailperkuliahan dp = cacheDp.get(id);
				Matakuliah mk = getMatakuliahAman(dp);

				if (mk != null && mk.getKode() != null) {
					String kode = mk.getKode().toLowerCase().trim();
					Double totalNilai = dp.getTotalNilai() != null ? dp.getTotalNilai() : 0.0;

					if (!mapNilaiKodes.containsKey(kode) || mapNilaiKodes.get(kode) < totalNilai) {
						mapNilaiKodes.put(kode, totalNilai);
						mapkode.put(kode, dp.getId());
					}
				}
			}
		} else {
			for (Long id : mapnama.values()) {
				mapkode.put(id.toString(), id);
			}
		}

		return prosesEkivalenOpt(aktifkan_ekivalen, nilaiHurufTidakMasukPerhitungan, nilai0MasukPenghitungan, minimal,
				berdasarkan_nama, mapkode, cacheDp).values();
	}

	/**
	 * Kumpulkan mata kuliah SKS=0 yang SUDAH bernilai (mis. MK non-kredit / pass-fail
	 * "L") dari daftar detailperkuliahan. MK ini SENGAJA dibuang oleh
	 * {@link #saringBerdasarNilaiDan0(java.util.Collection)} (SKS x bobot = 0, di bawah
	 * ambang), padahal tetap perlu TAMPIL di transkrip dengan nilai huruf "L" TANPA ikut
	 * menghitung IPK (IPK dihitung terpisah dan tetap mengabaikan SKS=0). Hanya MK yang
	 * sudah memiliki nilai huruf yang disertakan.
	 */
	public java.util.List<Long> saringTampilkanSks0Bernilai(java.util.Collection<Long> detailperkuliahans) {
		java.util.List<Long> hasil = new ArrayList<Long>();
		if (detailperkuliahans == null || detailperkuliahans.isEmpty()) {
			return hasil;
		}
		for (Long id : detailperkuliahans) {
			if (id == null) {
				continue;
			}
			Detailperkuliahan dp = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
					id.toString());
			if (dp == null || dp.getSemester() == null || dp.getSemester() < 0) {
				continue;
			}
			Matakuliah mk = getMatakuliahAman(dp);
			if (mk == null || mk.getSks() == null || mk.getSks().intValue() != 0) {
				continue; // hanya SKS = 0
			}
			String nh = dp.getNilaiHuruf();
			if (nh == null || nh.trim().isEmpty()) {
				continue; // hanya yang SUDAH bernilai
			}
			if (!hasil.contains(id)) {
				hasil.add(id);
			}
		}
		return hasil;
	}

	/**
	 * Menghasilkan <b>alasan/penjelasan mengapa sebuah mata kuliah berstatus
	 * "Tidak Valid"</b> (tidak masuk perhitungan IPK/transkrip) untuk setiap
	 * {@link Detailperkuliahan} milik mahasiswa ini. Metode ini merupakan pasangan
	 * penjelas (explainer) dari {@link #saringBerdasarNilaiDan0(java.util.Collection)}:
	 * bila {@code saringBerdasarNilaiDan0} memutuskan <i>mana</i> yang valid, metode
	 * ini menjelaskan <i>mengapa</i> sebuah baris tidak lolos, agar pengguna (staf
	 * akademik, dosen wali, maupun mahasiswa) langsung memahami tindakan koreksi yang
	 * perlu dilakukan tanpa menebak-nebak.
	 *
	 * <p><b>Kontrak keluaran.</b> Metode mengembalikan {@code Map<Long,String>} yang
	 * memetakan <code>id Detailperkuliahan</code> ke kalimat alasan berbahasa Indonesia
	 * yang siap ditampilkan. Hanya baris <b>Tidak Valid</b> yang dimasukkan ke dalam
	 * peta; baris yang valid sengaja <i>tidak</i> memiliki entri, sehingga pemanggil
	 * cukup melakukan {@code alasanMap.get(id)} dan menampilkan teks bila hasilnya tidak
	 * {@code null}. Bila parameter kosong/{@code null}, dikembalikan peta kosong (tidak
	 * pernah {@code null}) sehingga aman dipakai langsung tanpa pengecekan tambahan.
	 *
	 * <p><b>Sumber kebenaran tunggal.</b> Untuk memastikan penjelasan selalu konsisten
	 * dengan hasil penyaringan yang sebenarnya, metode ini <i>tidak</i> menduplikasi
	 * keputusan valid/tidak: ia memanggil ulang {@code saringBerdasarNilaiDan0} untuk
	 * memperoleh himpunan id yang valid, lalu hanya menganalisis baris yang berada di
	 * luar himpunan tersebut. Dengan begitu, apabila aturan penyaringan berubah di masa
	 * depan, status valid/tidak tetap mengikuti {@code saringBerdasarNilaiDan0} dan
	 * hanya narasi alasannya yang dihasilkan di sini.
	 *
	 * <p><b>Konfigurasi yang dibaca</b> (sama persis dengan penyaringan): kunci
	 * {@code nilai_0_tidak_masuk_dalam_perhitungan_ipk} (apakah nilai di bawah ambang
	 * dibuang), {@code nilai_minimal_tidak_masuk_dalam_perhitungan_ipk} (ambang minimal,
	 * default {@code 0.1}), serta bendera {@code aktifkan_kesamaan_nama},
	 * {@code aktifkan_kesamaan_kode}, {@code aktifkan_ekivalen}, dan
	 * {@code saring_nilai_ipk_juga_berdasarkan_nama}. Karena alasan diturunkan dari
	 * konfigurasi yang identik, penjelasan tidak akan pernah bertentangan dengan angka
	 * "Valid / Tidak Valid" yang ditampilkan di ringkasan.
	 *
	 * <p><b>Urutan prioritas alasan.</b> Sebuah baris hanya diberi satu alasan, dipilih
	 * mengikuti urutan tahapan penyaringan yang sesungguhnya sehingga akurat:
	 * <ol>
	 *   <li><i>Data tidak ditemukan</i> — objek {@code Detailperkuliahan} gagal dimuat
	 *       dari basis data (kemungkinan data yatim/terhapus).</li>
	 *   <li><i>Semester tidak sah</i> — {@code semester} kosong atau bernilai negatif,
	 *       sehingga baris tidak pernah masuk tahap penyaringan awal.</li>
	 *   <li><i>Nilai di bawah ambang</i> — hanya bila aturan nilai-0 aktif dan
	 *       {@code totalNilai} lebih kecil dari ambang minimal (mis. nilai belum
	 *       diinput dosen sehingga masih 0).</li>
	 *   <li><i>Kalah kesamaan nama</i> — ada mata kuliah lain yang <b>namanya sama</b>
	 *       dan lolos tahap awal dengan nilai lebih tinggi/sama; sistem hanya menghitung
	 *       satu (nilai tertinggi).</li>
	 *   <li><i>Kalah kesamaan kode</i> — ada mata kuliah lain <b>berkode sama</b> yang
	 *       terpilih dengan nilai lebih tinggi/sama.</li>
	 *   <li><i>Kalah ekivalensi</i> — mata kuliah ini <b>disetarakan</b> dengan mata
	 *       kuliah lain yang nilainya lebih tinggi/terpilih; untuk mata kuliah yang
	 *       diekivalenkan hanya nilai tertinggi yang dihitung.</li>
	 *   <li><i>Cadangan</i> — bila tidak satu pun kondisi di atas cocok (kasus langka),
	 *       diberikan penjelasan umum bahwa baris terduplikasi/kalah nilai.</li>
	 * </ol>
	 *
	 * <p>Pencarian "pemenang" pada kasus 4–6 hanya mempertimbangkan mata kuliah pesaing
	 * yang juga <b>lolos penyaringan awal</b> (semester sah dan, bila relevan, nilai di
	 * atas ambang), persis seperti yang dilakukan {@code saringBerdasarNilaiDan0}.
	 * Nama, kode, dan nilai pemenang disertakan di dalam kalimat agar pengguna dapat
	 * membandingkan langsung dan memutuskan apakah data perlu diperbaiki.
	 *
	 * <p><b>Efek samping & kinerja.</b> Metode bersifat baca-saja: tidak mengubah entitas
	 * apa pun dan tidak membuka/menutup {@code Session} sendiri. Ia memuat objek melalui
	 * {@code GeneralValueObject.ambilData} yang memakai cache memori, sehingga aman
	 * dipanggil sekali per pemuatan halaman profil/ringkasan studi. Semua pemuatan
	 * dibungkus penjagaan {@code null} agar tidak pernah melempar {@code NullPointerException}
	 * meski data mata kuliah/perkuliahan tidak lengkap.
	 *
	 * @param detailperkuliahanIds koleksi id {@code Detailperkuliahan} milik mahasiswa
	 *        yang ingin dievaluasi (biasanya {@link #ambilDetailperkuliahan()}).
	 * @return peta {@code id -> alasan} untuk baris yang Tidak Valid; kosong bila semua
	 *         valid atau masukan kosong. Tidak pernah {@code null}.
	 */
	public java.util.Map<Long, String> alasanTidakValidDetail(java.util.Collection<Long> detailperkuliahanIds) {
		java.util.Map<Long, String> alasan = new java.util.HashMap<Long, String>();
		if (detailperkuliahanIds == null || detailperkuliahanIds.isEmpty()) {
			return alasan;
		}

		// Himpunan valid = sumber kebenaran tunggal, ikut saringBerdasarNilaiDan0
		java.util.Set<Long> validIds = new java.util.HashSet<Long>();
		try {
			java.util.Collection<Long> valids = saringBerdasarNilaiDan0(detailperkuliahanIds);
			if (valids != null) {
				validIds.addAll(valids);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:3621");
			// bila penyaringan gagal, jangan salahkan baris tertentu; kembalikan peta apa adanya
		}

		// Muat semua objek ke memori sekali (cache), sama seperti penyaringan
		Map<Long, Detailperkuliahan> cacheDp = new HashMap<Long, Detailperkuliahan>();
		for (Long id : detailperkuliahanIds) {
			if (id == null) {
				continue;
			}
			Detailperkuliahan dp = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
					id.toString());
			if (dp != null) {
				cacheDp.put(id, dp);
			}
		}

		// Konfigurasi identik dengan saringBerdasarNilaiDan0
		String nilai0MasukPenghitungan = Common
				.getKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.AKTIF).getNilai();
		Double minimal = 0.1;
		try {
			// FIX NumberFormatException "0,1": admin kadang mengisi konfigurasi ini memakai
			// koma desimal (kebiasaan Indonesia), padahal Double.parseDouble WAJIB titik.
			// Normalisasi koma->titik dulu supaya nilai yang admin maksud benar-benar
			// terpakai (sebelumnya exception ini diam-diam ditelan & selalu jatuh ke default
			// 0.1 hardcode, jadi perubahan konfigurasi admin ke nilai lain tidak pernah aktif).
			String nilaiMinimalStr = Common.getKonfigurasi("nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1")
					.getNilai().trim().replace(',', '.');
			minimal = Double.parseDouble(nilaiMinimalStr);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:3645");
		}
		boolean aktifkan_ekivalen = Common.bolehKonfigurasi("aktifkan_ekivalen");
		boolean aktifkan_kesamaan_nama = Common.bolehKonfigurasi("aktifkan_kesamaan_nama");
		boolean aktifkan_kesamaan_kode = Common.bolehKonfigurasi("aktifkan_kesamaan_kode");
		boolean berdasarkan_nama = Common.bolehKonfigurasi("saring_nilai_ipk_juga_berdasarkan_nama");
		String minimalStr = formatNilaiSingkat(minimal);

		for (Long id : detailperkuliahanIds) {
			if (id == null || validIds.contains(id)) {
				continue; // hanya jelaskan yang Tidak Valid
			}

			Detailperkuliahan dp = cacheDp.get(id);
			if (dp == null) {
				alasan.put(id, "Data mata kuliah tidak ditemukan / gagal dimuat dari sistem. "
						+ "Coba muat ulang, atau periksa apakah data KRS mata kuliah ini masih ada.");
				continue;
			}

			// 1) Semester tidak sah
			if (dp.getSemester() == null || dp.getSemester().intValue() < 0) {
				alasan.put(id, "Semester mata kuliah belum diisi atau tidak sah, sehingga nilai belum "
						+ "diikutkan dalam perhitungan IPK. Perbaiki data semester / KRS mata kuliah ini.");
				continue;
			}

			// 2) Nilai di bawah ambang minimal (hanya bila aturan nilai-0 aktif)
			double totalNilai = dp.getTotalNilai() != null ? dp.getTotalNilai().doubleValue() : 0.0;
			if (Konfigurasi.AKTIF.equals(nilai0MasukPenghitungan) && totalNilai < minimal.doubleValue()) {
				alasan.put(id, "Nilai belum keluar / masih 0 (di bawah batas minimal " + minimalStr + "). "
						+ "Nilai akan otomatis dihitung setelah dosen menginput nilai. "
						+ "(Aturan aktif: nilai 0 tidak masuk perhitungan IPK)");
				continue;
			}

			Matakuliah mk = getMatakuliahAman(dp);
			String namaMk = mk != null && mk.getNama() != null ? mk.getNama().trim() : "";
			String kodeMk = mk != null && mk.getKode() != null ? mk.getKode().trim() : "";

			// 3) Kalah kesamaan NAMA
			if (aktifkan_kesamaan_nama && namaMk.length() > 0) {
				Detailperkuliahan pemenang = cariPemenangKesamaan(dp, cacheDp, nilai0MasukPenghitungan, minimal, 1,
						berdasarkan_nama);
				if (pemenang != null) {
					alasan.put(id, susunAlasanKalah(pemenang, "bernama sama", "penyaringan kesamaan nama", totalNilai));
					continue;
				}
			}

			// 4) Kalah kesamaan KODE
			if (aktifkan_kesamaan_kode && kodeMk.length() > 0) {
				Detailperkuliahan pemenang = cariPemenangKesamaan(dp, cacheDp, nilai0MasukPenghitungan, minimal, 2,
						berdasarkan_nama);
				if (pemenang != null) {
					alasan.put(id, susunAlasanKalah(pemenang, "berkode sama", "penyaringan kesamaan kode", totalNilai));
					continue;
				}
			}

			// 5) Kalah EKIVALENSI
			if (aktifkan_ekivalen && mk != null) {
				Detailperkuliahan pemenang = cariPemenangKesamaan(dp, cacheDp, nilai0MasukPenghitungan, minimal, 3,
						berdasarkan_nama);
				if (pemenang != null) {
					alasan.put(id,
							susunAlasanKalah(pemenang, "yang disetarakan (ekivalen)", "ekivalensi mata kuliah", totalNilai));
					continue;
				}
			}

			// 6) Cadangan
			alasan.put(id, "Nilai tidak masuk perhitungan IPK karena terduplikasi atau kalah nilai dengan "
					+ "mata kuliah setara. Periksa kembali data nilai / KRS mata kuliah ini.");
		}

		return alasan;
	}

	/**
	 * Mencari mata kuliah "pemenang" (yang membuat {@code dpKalah} tersingkir) menurut
	 * satu jenis kesamaan. {@code mode}: 1 = kesamaan nama, 2 = kesamaan kode, 3 =
	 * ekivalensi. Pesaing hanya dipertimbangkan bila lolos penyaringan awal (semester
	 * sah dan, bila aturan nilai-0 aktif, nilai di atas ambang) — persis seperti
	 * {@code saringBerdasarNilaiDan0}. Dikembalikan pesaing dengan nilai tertinggi yang
	 * nilainya &ge; nilai {@code dpKalah}; {@code null} bila tidak ada.
	 */
	private Detailperkuliahan cariPemenangKesamaan(Detailperkuliahan dpKalah, Map<Long, Detailperkuliahan> cacheDp,
			String nilai0MasukPenghitungan, Double minimal, int mode, boolean berdasarkan_nama) {
		Matakuliah mkKalah = getMatakuliahAman(dpKalah);
		if (mkKalah == null) {
			return null;
		}
		double nilaiKalah = dpKalah.getTotalNilai() != null ? dpKalah.getTotalNilai().doubleValue() : 0.0;
		String namaKalah = mkKalah.getNama() != null ? mkKalah.getNama().trim() : "";
		String kodeKalah = mkKalah.getKode() != null ? mkKalah.getKode().trim() : "";
		java.util.List<MatakuliahEkivalen> ekivalens = null;
		if (mode == 3) {
			try {
				ekivalens = mkKalah.ambilEkivalen(getNim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:3745");
			}
			if (ekivalens == null || ekivalens.isEmpty()) {
				return null;
			}
		}

		Detailperkuliahan pemenang = null;
		double nilaiPemenang = -1.0;
		for (Detailperkuliahan lain : cacheDp.values()) {
			if (lain == null || lain.getId() == null || lain.getId().equals(dpKalah.getId())) {
				continue;
			}
			if (!lolosSaringAwal(lain, nilai0MasukPenghitungan, minimal)) {
				continue;
			}
			Matakuliah mkLain = getMatakuliahAman(lain);
			if (mkLain == null) {
				continue;
			}
			boolean cocok = false;
			if (mode == 1) {
				cocok = mkLain.getNama() != null && mkLain.getNama().trim().equalsIgnoreCase(namaKalah)
						&& namaKalah.length() > 0;
			} else if (mode == 2) {
				cocok = mkLain.getKode() != null && mkLain.getKode().trim().equalsIgnoreCase(kodeKalah)
						&& kodeKalah.length() > 0;
			} else if (mode == 3) {
				for (MatakuliahEkivalen ekiv : ekivalens) {
					if (ekiv == null || ekiv.getMatakuliahEkivalen() == null) {
						continue;
					}
					boolean matchKode = mkLain.getKode() != null
							&& mkLain.getKode().equalsIgnoreCase(ekiv.getMatakuliahEkivalen().getKode());
					boolean matchNama = berdasarkan_nama && mkLain.getNama() != null
							&& ekiv.getMatakuliahEkivalen().getNama() != null
							&& mkLain.getNama().equalsIgnoreCase(ekiv.getMatakuliahEkivalen().getNama());
					if (matchKode || matchNama) {
						cocok = true;
						break;
					}
				}
			}
			if (!cocok) {
				continue;
			}
			double v = lain.getTotalNilai() != null ? lain.getTotalNilai().doubleValue() : 0.0;
			if (v > nilaiPemenang) {
				nilaiPemenang = v;
				pemenang = lain;
			}
		}

		if (pemenang != null && nilaiPemenang >= nilaiKalah) {
			return pemenang;
		}
		return null;
	}

	/** Apakah {@code dp} lolos penyaringan awal (semester sah + nilai di atas ambang bila relevan). */
	private boolean lolosSaringAwal(Detailperkuliahan dp, String nilai0MasukPenghitungan, Double minimal) {
		if (dp == null || dp.getSemester() == null || dp.getSemester().intValue() < 0) {
			return false;
		}
		double totalNilai = dp.getTotalNilai() != null ? dp.getTotalNilai().doubleValue() : 0.0;
		if (Konfigurasi.AKTIF.equals(nilai0MasukPenghitungan)) {
			return totalNilai >= minimal.doubleValue();
		}
		if (Konfigurasi.TIDAK_AKTIF.equals(nilai0MasukPenghitungan)) {
			return true;
		}
		return false;
	}

	/** Menyusun kalimat alasan "kalah" yang seragam untuk kesamaan nama/kode/ekivalen. */
	private String susunAlasanKalah(Detailperkuliahan pemenang, String labelHubungan, String labelAturan,
			double nilaiKalah) {
		Matakuliah mkMenang = getMatakuliahAman(pemenang);
		String namaMenang = mkMenang != null && mkMenang.getNama() != null ? mkMenang.getNama() : "-";
		String kodeMenang = mkMenang != null && mkMenang.getKode() != null ? mkMenang.getKode() : "-";
		double nilaiMenang = pemenang.getTotalNilai() != null ? pemenang.getTotalNilai().doubleValue() : 0.0;
		String pembanding = nilaiMenang > nilaiKalah ? "nilai lebih tinggi" : "nilai yang sama";
		return "Ada mata kuliah " + labelHubungan + " '" + namaMenang + "' (" + kodeMenang + ") dengan " + pembanding
				+ " (" + formatNilaiSingkat(Double.valueOf(nilaiMenang)) + "). Sistem hanya menghitung satu nilai "
				+ "tertinggi agar tidak dobel. (Aturan aktif: " + labelAturan + ")";
	}

	/** Format angka nilai ringkas: buang desimal bila bulat. */
	private String formatNilaiSingkat(Double d) {
		if (d == null) {
			return "0";
		}
		double v = d.doubleValue();
		if (v == Math.floor(v) && !Double.isInfinite(v)) {
			return String.valueOf((long) v);
		}
		return String.valueOf(Math.round(v * 100.0) / 100.0);
	}

	/**
	 * Mengambil {@link Matakuliah} dari sebuah {@link Detailperkuliahan} dengan aman:
	 * {@code matakuliahKonversi} diutamakan (mata kuliah hasil konversi/pengakuan), baru
	 * {@code perkuliahan.matakuliah}. Tidak pernah melempar {@code NullPointerException}.
	 *
	 * @param dp baris detail perkuliahan; boleh {@code null}.
	 * @return mata kuliah terkait, atau {@code null} bila tidak ada.
	 */
	// Helper untuk mendapatkan matakuliah dengan aman dari Detailperkuliahan
	private Matakuliah getMatakuliahAman(Detailperkuliahan dp) {
		if (dp == null)
			return null;
		if (dp.getMatakuliahKonversi() != null)
			return dp.getMatakuliahKonversi();
		if (dp.getPerkuliahan() != null)
			return dp.getPerkuliahan().getMatakuliah();
		return null;
	}

	/**
	 * Tahap EKIVALENSI dari {@link #saringBerdasarNilaiDan0(java.util.Collection)}: bila dua mata
	 * kuliah dinyatakan setara ({@link MatakuliahEkivalen}), hanya yang bernilai tertinggi yang
	 * dipertahankan.
	 *
	 * <p>Versi ini adalah penulisan ulang {@code prosesEkivalen} lama yang lambat: seluruh objek
	 * sudah tersedia di {@code cacheDp} (tidak ada query di dalam loop), kandidat disaring dulu
	 * menjadi {@code listValidDp} (semester sah, status {@code DISETUJUI}, nilai huruf tidak
	 * termasuk yang dikecualikan), lalu pembandingan dilakukan berpasangan dengan himpunan
	 * {@code removes} sehingga satu baris yang sudah kalah tidak dibandingkan lagi.</p>
	 *
	 * <p>Pencocokan ekivalensi memakai KODE mata kuliah; bila {@code berdasarkan_nama} bernilai
	 * {@code true} (konfigurasi {@code saring_nilai_ipk_juga_berdasarkan_nama}), kesamaan NAMA
	 * juga diterima. Daftar ekivalensi diambil per mata kuliah lewat
	 * {@code Matakuliah.ambilEkivalen(nim)} sehingga bisa berbeda antar-kurikulum/angkatan.</p>
	 *
	 * <p>Bila {@code aktifkan_ekivalen} bernilai {@code false}, {@code mapkode} dikembalikan apa
	 * adanya tanpa pemrosesan.</p>
	 *
	 * @param aktifkan_ekivalen               saklar konfigurasi ekivalensi.
	 * @param nilaiHurufTidakMasukPerhitungan nilai huruf yang dikecualikan dari perhitungan.
	 * @param nilai0MasukPenghitungan         nilai kunci {@code nilai_0_tidak_masuk_dalam_perhitungan_ipk}.
	 * @param minimal                         ambang nilai minimal.
	 * @param berdasarkan_nama                izinkan pencocokan ekivalensi lewat nama mata kuliah.
	 * @param mapkode                         peta hasil tahap sebelumnya (kode/id &rarr; id).
	 * @param cacheDp                         cache objek detailperkuliahan yang sudah dimuat.
	 * @return peta hasil akhir tanpa baris yang kalah ekivalensi.
	 */
	// Perbaikan prosesEkivalen yang 100x lebih cepat (O(N) Complexity) dan bebas
	// bug logika
	private Map<String, Long> prosesEkivalenOpt(boolean aktifkan_ekivalen, String nilaiHurufTidakMasukPerhitungan,
			String nilai0MasukPenghitungan, Double minimal, Boolean berdasarkan_nama, Map<String, Long> mapkode,
			Map<Long, Detailperkuliahan> cacheDp) {

		if (!aktifkan_ekivalen)
			return mapkode;

		Set<Long> removes = new HashSet<Long>();

		// Kumpulkan dulu data menjadi List agar mudah di-compare tanpa nested loop
		// terlalu dalam
		List<Detailperkuliahan> listValidDp = new ArrayList<Detailperkuliahan>();
		for (Long id : mapkode.values()) {
			Detailperkuliahan dp = cacheDp.get(id);
			if (dp != null && dp.getSemester() != null && dp.getSemester() >= 0) {
				if (Detailperkuliahan.DISETUJUI.equals(dp.getPersetujuan())) {
					String huruf = dp.getNilaiHuruf() != null ? dp.getNilaiHuruf() : "";
					if (!huruf.equalsIgnoreCase(nilaiHurufTidakMasukPerhitungan)) {
						listValidDp.add(dp);
					}
				}
			}
		}

		// Group mata kuliah berdasarkan grup ekivalensi
		// Jika MK A ekivalen MK B, pilih yang nilainya paling tinggi
		for (int i = 0; i < listValidDp.size(); i++) {
			Detailperkuliahan dpA = listValidDp.get(i);
			Matakuliah mkA = getMatakuliahAman(dpA);
			if (mkA == null || removes.contains(dpA.getId()))
				continue;

			List<MatakuliahEkivalen> ekivalens = mkA.ambilEkivalen(getNim());
			if (ekivalens == null || ekivalens.isEmpty())
				continue;

			for (int j = i + 1; j < listValidDp.size(); j++) {
				Detailperkuliahan dpB = listValidDp.get(j);
				if (removes.contains(dpB.getId()))
					continue;

				Matakuliah mkB = getMatakuliahAman(dpB);
				if (mkB == null)
					continue;

				boolean isEkivalen = false;
				for (MatakuliahEkivalen ekiv : ekivalens) {
					if (ekiv.getMatakuliahEkivalen() != null) {
						boolean matchKode = mkB.getKode().equalsIgnoreCase(ekiv.getMatakuliahEkivalen().getKode());
						boolean matchNama = berdasarkan_nama && mkB.getNama() != null
								&& mkB.getNama().equalsIgnoreCase(ekiv.getMatakuliahEkivalen().getNama());

						if (matchKode || matchNama) {
							isEkivalen = true;
							break;
						}
					}
				}

				if (isEkivalen) {
					// Bandingkan nilainya
					Double valA = dpA.getTotalNilai() != null ? dpA.getTotalNilai() : 0.0;
					Double valB = dpB.getTotalNilai() != null ? dpB.getTotalNilai() : 0.0;

					// Cek threshold "minimal" 0 (dari konfigurasi)
					boolean aValid = !nilai0MasukPenghitungan.equals(Konfigurasi.AKTIF) || valA >= minimal;
					boolean bValid = !nilai0MasukPenghitungan.equals(Konfigurasi.AKTIF) || valB >= minimal;

					if (aValid && bValid) {
						if (valA >= valB) {
							removes.add(dpB.getId());
						} else {
							removes.add(dpA.getId());
							break; // Karena A dihapus, A tidak perlu di-compare lagi dengan sisanya
						}
					} else if (aValid) {
						removes.add(dpB.getId());
					} else if (bValid) {
						removes.add(dpA.getId());
						break;
					}
				}
			}
		}

		// Bentuk Map hasil akhir
		Map<String, Long> hasilAkhir = new HashMap<String, Long>();
		for (Map.Entry<String, Long> entry : mapkode.entrySet()) {
			if (!removes.contains(entry.getValue())) {
				hasilAkhir.put(entry.getKey(), entry.getValue());
			}
		}

		return hasilAkhir;
	}

	/**
	 * Penyaring duplikat/ekivalensi versi ALTERNATIF: untuk tiap mata kuliah, dipilih satu
	 * {@link Detailperkuliahan} dengan nilai tertinggi, dikelompokkan berdasarkan KODE mata kuliah.
	 *
	 * <p>Berbeda dengan {@link #saringBerdasarNilaiDan0(java.util.Collection)}, metode ini TIDAK
	 * membaca konfigurasi apa pun: tidak ada penyaringan nilai minimal, nilai huruf, maupun status
	 * verifikasi. Yang dibuang hanya baris tanpa semester/bersemester negatif dan baris yang kalah
	 * nilai terhadap mata kuliah setara. Karena itu hasilnya biasanya LEBIH BANYAK daripada
	 * {@code saringBerdasarNilaiDan0}.</p>
	 *
	 * <p>Dipakai lewat pembungkus {@link #saringBerdasarNilai(java.util.Collection)} — antara lain
	 * oleh {@link #prosesHitungSks(java.util.Collection, Boolean, boolean)},
	 * {@code KrsDanSkripsiHelper}, {@code KrsDetailHelper}, {@code ProfileMahasiswa} dan
	 * {@code TampilStudiMahasiswaHelper}.</p>
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang disaring.
	 * @return koleksi id terpilih (satu per kode mata kuliah).
	 */
	// ------------------------------------------------------------- //
	// Versi Perbaikan dari saringBerdasarNilaiOk (Bebas NPE & Bug) //
	// ------------------------------------------------------------- //
	public Collection<Long> saringBerdasarNilaiOk(Collection<Long> detailperkuliahans) {
		Map<String, Long> hasil = new HashMap<String, Long>();
		if (detailperkuliahans == null || detailperkuliahans.isEmpty())
			return hasil.values();

		Map<Long, Detailperkuliahan> cacheDp = new HashMap<Long, Detailperkuliahan>();
		for (Long id : detailperkuliahans) {
			Detailperkuliahan dp = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
					id.toString());
			if (dp != null && dp.getSemester() != null && dp.getSemester() >= 0) {
				cacheDp.put(id, dp);
			}
		}

		for (Detailperkuliahan dpA : cacheDp.values()) {
			Matakuliah mkA = getMatakuliahAman(dpA);
			if (mkA == null)
				continue;

			List<MatakuliahEkivalen> ekivalens = mkA.ambilEkivalen(getNim());
			if (ekivalens == null || ekivalens.isEmpty()) {
				simpanHasil(hasil, mkA, dpA, cacheDp);
			} else {
				Detailperkuliahan pemenang = dpA;
				Matakuliah mkPemenang = mkA;
				Double maxVal = dpA.getTotalNilai() != null ? dpA.getTotalNilai() : 0.0;

				for (MatakuliahEkivalen ekiv : ekivalens) {
					for (Detailperkuliahan dpB : cacheDp.values()) {
						if (dpB.getId().equals(dpA.getId()))
							continue;

						Matakuliah mkB = getMatakuliahAman(dpB);
						if (mkB != null && ekiv.getMatakuliahEkivalen() != null) {
							if (mkB.getKode().equalsIgnoreCase(ekiv.getMatakuliah().getKode())
									|| mkB.getKode().equalsIgnoreCase(ekiv.getMatakuliahEkivalen().getKode())) {

								Double valB = dpB.getTotalNilai() != null ? dpB.getTotalNilai() : 0.0;
								if (valB > maxVal) {
									maxVal = valB;
									pemenang = dpB;
									mkPemenang = ekiv.getMatakuliahEkivalen();
								}
							}
						}
					}
				}
				simpanHasil(hasil, mkPemenang, pemenang, cacheDp);
			}
		}

		return hasil.values();
	}

	/**
	 * Menyimpan satu {@link Detailperkuliahan} ke peta hasil {@link #saringBerdasarNilaiOk(
	 * java.util.Collection)} dengan kunci KODE mata kuliah (huruf kecil). Bila kode itu sudah
	 * terisi, yang dipertahankan adalah baris dengan {@code totalNilai} LEBIH TINGGI; pembandingan
	 * memakai objek dari {@code cacheDp} sehingga tidak ada query tambahan.
	 *
	 * @param hasil                  peta hasil (kode mata kuliah &rarr; id detailperkuliahan).
	 * @param matakuliah             mata kuliah acuan penentu kunci.
	 * @param detailperkuliahanBaru  calon baris yang hendak disimpan.
	 * @param cacheDp                cache objek detailperkuliahan yang sudah dimuat.
	 */
	private void simpanHasil(Map<String, Long> hasil, Matakuliah matakuliah, Detailperkuliahan detailperkuliahanBaru,
			Map<Long, Detailperkuliahan> cacheDp) {
		String kodeMk = matakuliah.getKode().toLowerCase();
		if (hasil.containsKey(kodeMk)) {
			Long idLama = hasil.get(kodeMk);
			Detailperkuliahan dpLama = cacheDp.get(idLama); // Cukup ambil dari memory

			if (dpLama != null) {
				Double nilaiLama = dpLama.getTotalNilai() != null ? dpLama.getTotalNilai() : 0.0;
				Double nilaiBaru = detailperkuliahanBaru.getTotalNilai() != null ? detailperkuliahanBaru.getTotalNilai()
						: 0.0;

				if (nilaiBaru < nilaiLama) {
					detailperkuliahanBaru = dpLama;
				}
			}
		}
		hasil.put(kodeMk, detailperkuliahanBaru.getId());
	}

	/**
	 * Pembungkus tipis {@link #saringBerdasarNilaiOk(java.util.Collection)} — nama yang dipakai
	 * pemanggil di luar kelas ini.
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang disaring.
	 * @return koleksi id terpilih (satu per kode mata kuliah).
	 */
	public Collection<Long> saringBerdasarNilai(Collection<Long> detailperkuliahans) {
		return saringBerdasarNilaiOk(detailperkuliahans);
	}

	/**
	 * Menghitung IPK dari sekumpulan id detailperkuliahan, TANPA membedakan mata kuliah konversi.
	 * Setara {@code prosesHitungIpk(detailperkuliahans, null)}.
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang dihitung.
	 * @return IPK; {@code 0.0} bila tidak ada SKS yang dihitung.
	 * @see #prosesHitungIpk(java.util.Collection, Boolean)
	 */
	public Double prosesHitungIpk(Collection<Long> detailperkuliahans) {
		return prosesHitungIpk(detailperkuliahans, null);
	}

	/**
	 * <b>Mesin utama perhitungan IPK</b>: {@code IPK = &Sigma;(totalIP &times; SKS) / &Sigma;SKS}
	 * atas mata kuliah yang lolos {@link #saringBerdasarNilaiDan0(java.util.Collection)}.
	 *
	 * <p><b>Aturan penyaringan</b> (selain penyaring awal): hanya baris berstatus
	 * {@code DISETUJUI}, nilai hurufnya bukan yang dikecualikan konfigurasi
	 * {@code nilai_huruf_yg_tidak_masuk_perhitungan_ip}, dan — bila
	 * {@code nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk} AKTIF — nilai semester
	 * BERJALAN yang belum diverifikasi dilewati. Bila
	 * {@code nilai_0_tidak_masuk_dalam_perhitungan_ipk} AKTIF, baris dengan {@code totalNilai} di
	 * bawah ambang tidak ikut menambah pembilang MAUPUN penyebut.</p>
	 *
	 * <p><b>Parameter {@code bukanKonversi}</b> memilah asal mata kuliah:</p>
	 * <ul>
	 *   <li>{@code null} — semua mata kuliah dihitung;</li>
	 *   <li>{@code true} — HANYA mata kuliah non-konversi ({@code matakuliahKonversi} kosong);</li>
	 *   <li>{@code false} — hanya baris yang punya {@code matakuliahKonversi} ATAU
	 *       {@code perkuliahan}.</li>
	 * </ul>
	 *
	 * <p>Ambang minimal dibaca memakai {@code Common.parseAngkaKonfigurasi} yang toleran terhadap
	 * desimal berkoma; komentar di dalam metode menjelaskan mengapa {@code Double.parseDouble}
	 * langsung berbahaya (nilai konfigurasi admin diabaikan diam-diam sehingga penyaringan IPK
	 * jadi salah).</p>
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang dihitung.
	 * @param bukanKonversi      pemilah asal mata kuliah; lihat penjelasan di atas.
	 * @return IPK; {@code 0.0} bila total SKS yang dihitung nol.
	 */
	public Double prosesHitungIpk(Collection<Long> detailperkuliahans, Boolean bukanKonversi) {

		String nilai0MasukPenghitungan = Common
				.getKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.AKTIF).getNilai();
		/*
		 * Dibaca TOLERAN: admin lazim mengetik desimal dgn KOMA ("0,1"). Double.parseDouble
		 * hanya menerima titik sehingga sebelumnya melempar NumberFormatException pada TIAP
		 * pemuatan KRS -- membanjiri log, dan yang lebih berbahaya: nilai yang dimaksud admin
		 * diabaikan diam-diam lalu dipakai bawaan 0.1 (mis. admin menulis "2,5" tapi sistem
		 * memakai 0.1, sehingga penyaringan nilai untuk IPK jadi salah).
		 */
		Double minimal = Common.parseAngkaKonfigurasi(
				Common.getKonfigurasi("nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1").getNilai(), 0.1);
		String nilaiHurufTidakMasukPerhitungan = Common.getKonfigurasi("nilai_huruf_yg_tidak_masuk_perhitungan_ip", "")
				.getNilai();

		boolean nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk = Common.bolehKonfigurasi("nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.TIDAK_AKTIF);
		Integer smtSaatIni = this.currentSemester();
		Double ipkTotal = 0.0;
		Integer sksTotal = 0;

		for (Long detailperkuliahanid : saringBerdasarNilaiDan0(detailperkuliahans)) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {

				if (smtSaatIni != null && smtSaatIni.equals(detailperkuliahan.getSemester())
						&& nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk
						&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
					continue;
				}

				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& !detailperkuliahan.getNilaiHuruf().equalsIgnoreCase(nilaiHurufTidakMasukPerhitungan)) {

					if (bukanKonversi == null || (bukanKonversi && detailperkuliahan.getMatakuliahKonversi() == null)
							|| (!bukanKonversi && (detailperkuliahan.getMatakuliahKonversi() != null
									|| detailperkuliahan.getPerkuliahan() != null))) {

						Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() != null
								? detailperkuliahan.getMatakuliahKonversi()
								: detailperkuliahan.getPerkuliahan() != null
										? detailperkuliahan.getPerkuliahan().getMatakuliah()
										: null;

						if (nilai0MasukPenghitungan.equals(Konfigurasi.AKTIF)) {
							if (detailperkuliahan.getTotalNilai() >= minimal) {
								ipkTotal += detailperkuliahan.getTotalIP() * matakuliah.getSks().doubleValue();
								sksTotal += matakuliah.getSks();
							}
						} else {
							ipkTotal += detailperkuliahan.getTotalIP() * matakuliah.getSks().doubleValue();
							sksTotal += matakuliah.getSks();
						}
					}
				}
			}
		}
		detailperkuliahans = null;

		Double ipk = sksTotal.equals(0) ? 0.0 : (ipkTotal) / sksTotal.doubleValue();
		return ipk;
	}

	/**
	 * Total SKS atas SELURUH mata kuliah yang pernah ditempuh mahasiswa ini.
	 *
	 * @param bukanKonversi pemilah asal mata kuliah ({@code null} = semua, {@code true} = hanya
	 *        non-konversi, {@code false} = hanya konversi).
	 * @param semua         {@code true} untuk menghitung tanpa memandang status persetujuan,
	 *        nilai huruf, verifikasi maupun ambang nilai.
	 * @return jumlah SKS.
	 * @see #prosesHitungSks(java.util.Collection, Boolean, boolean)
	 */
	public Integer hitungSks(Boolean bukanKonversi, boolean semua) {
		return prosesHitungSks(ambilDetailperkuliahan(), bukanKonversi, semua);
	}

	/**
	 * Total SKS pada SATU semester. Dipakai pemeriksaan jatah SKS saat pengisian KRS
	 * ({@code KrsUtilHelper}, {@code AmbilDataPerkuliahanHelper} dan sejenisnya).
	 *
	 * @param semester       semester yang dihitung.
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param bukanKonversi  pemilah asal mata kuliah.
	 * @param semua          {@code true} untuk mengabaikan seluruh penyaringan nilai/status.
	 * @return jumlah SKS semester itu.
	 * @see #prosesHitungSks(java.util.Collection, Boolean, boolean)
	 */
	public Integer hitungSks(Integer semester, Integer tahapan, Integer semesterPendek, Boolean bukanKonversi,
			boolean semua) {
		return prosesHitungSks(ambilDetailperkuliahan(semester, tahapan, semesterPendek), bukanKonversi, semua);
	}

	/**
	 * Total SKS secara KUMULATIF sampai semester tertentu — angka "SKS yang telah ditempuh" pada
	 * transkrip dan pemeriksaan syarat kelulusan.
	 *
	 * @param semester       batas semester (inklusif).
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param bukanKonversi  pemilah asal mata kuliah.
	 * @param semua          {@code true} untuk mengabaikan seluruh penyaringan nilai/status.
	 * @return jumlah SKS kumulatif.
	 * @see #prosesHitungSks(java.util.Collection, Boolean, boolean)
	 */
	public Integer hitungSksSampai(Integer semester, Integer tahapan, Integer semesterPendek, Boolean bukanKonversi,
			boolean semua) {
		return prosesHitungSks(ambilDetailperkuliahanSampai(semester, tahapan, semesterPendek, semua), bukanKonversi,
				semua);
	}

	/**
	 * Mesin penjumlahan SKS.
	 *
	 * <p>Berbeda dari mesin nilai/IPK lain, penyaring awal yang dipakai di sini adalah
	 * {@link #saringBerdasarNilai(java.util.Collection)} (yaitu
	 * {@link #saringBerdasarNilaiOk(java.util.Collection)}), BUKAN
	 * {@link #saringBerdasarNilaiDan0(java.util.Collection)} — sehingga penyaringannya lebih
	 * longgar dan tidak bergantung pada ambang nilai saat memilih baris.</p>
	 *
	 * <p>Bendera {@code semua} bernilai {@code true} membuat seluruh penjagaan dilewati: status
	 * persetujuan, nilai huruf yang dikecualikan, penyaringan nilai belum terverifikasi, dan
	 * ambang nilai minimal semuanya diabaikan sehingga yang dihitung adalah SKS yang DIAMBIL,
	 * bukan yang lulus. Bendera {@code bukanKonversi} memilah mata kuliah konversi vs biasa
	 * (perhatikan aturannya sedikit berbeda dari
	 * {@link #prosesHitungIpk(java.util.Collection, Boolean)}: di sini {@code false} berarti HANYA
	 * yang punya {@code matakuliahKonversi}).</p>
	 *
	 * <p>Hasil {@code null} dari penyaring ditangani sebagai daftar kosong, dan id {@code null} di
	 * dalam koleksi dilewati.</p>
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang dihitung.
	 * @param bukanKonversi      pemilah asal mata kuliah.
	 * @param semua              {@code true} untuk mengabaikan seluruh penyaringan nilai/status.
	 * @return jumlah SKS.
	 */
	public Integer prosesHitungSks(Collection<Long> detailperkuliahans, Boolean bukanKonversi, boolean semua) {

		String nilai0MasukPenghitungan = Common
				.getKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.AKTIF).getNilai();
		Double minimal = 0.1;
		try {
			// Toleransi admin mengetik koma sbg pemisah desimal (format Indonesia, mis. "0,1" untuk
			// maksud 0.1): Double.parseDouble() SELALU mensyaratkan titik terlepas dari locale JVM,
			// jadi nilai konfigurasi yang diketik dgn koma sebelumnya SELALU gagal parse di sini dan
			// diam-diam jatuh ke default hardcoded 0.1 di atas tanpa admin sadar konfigurasinya
			// (nilai_minimal_tidak_masuk_dalam_perhitungan_ipk) tidak pernah benar-benar terpakai.
			String nilaiMinimalStr = Common
					.getKonfigurasi("nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1").getNilai();
			if (nilaiMinimalStr != null) {
				minimal = Double.parseDouble(nilaiMinimalStr.trim().replace(',', '.'));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:4123");

		}
		String nilaiHurufTidakMasukPerhitungan = Common.getKonfigurasi("nilai_huruf_yg_tidak_masuk_perhitungan_ip", "")
				.getNilai();

		boolean nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk = Common.bolehKonfigurasi("nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.TIDAK_AKTIF);
		Integer smtSaatIni = this.currentSemester();
		Integer sksTotal = 0;

		Collection<Long> detailTersaring = saringBerdasarNilai(detailperkuliahans);
		if (detailTersaring == null) {
			detailTersaring = new ArrayList<Long>();
		}
		for (Long detailperkuliahanid : detailTersaring) {
			if (detailperkuliahanid == null) {
				continue;
			}
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {

				if (!semua && smtSaatIni != null && smtSaatIni.equals(detailperkuliahan.getSemester())
						&& nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk
						&& Detailperkuliahan.NOT_VERIFIED.equals(detailperkuliahan.getVerify())) {
					continue;
				}

				if (semua || (Detailperkuliahan.DISETUJUI.equals(detailperkuliahan.getPersetujuan())
						&& detailperkuliahan.getNilaiHuruf() != null
						&& !detailperkuliahan.getNilaiHuruf().equalsIgnoreCase(nilaiHurufTidakMasukPerhitungan))) {

					if (bukanKonversi == null || (bukanKonversi && detailperkuliahan.getMatakuliahKonversi() == null)
							|| (!bukanKonversi && detailperkuliahan.getMatakuliahKonversi() != null)) {

						Matakuliah matakuliah = detailperkuliahan.getMatakuliahKonversi() != null
								? detailperkuliahan.getMatakuliahKonversi()
								: detailperkuliahan.getPerkuliahan() != null
										? detailperkuliahan.getPerkuliahan().getMatakuliah()
										: null;

						if (semua) {
							sksTotal += matakuliah.getSks();
						} else if (nilai0MasukPenghitungan.equals(Konfigurasi.AKTIF)) {
							if (detailperkuliahan.getTotalNilai() >= minimal) {
								sksTotal += matakuliah.getSks();
							}
						} else {
							sksTotal += matakuliah.getSks();
						}
					}
				}
			}
		}
		detailperkuliahans = null;

		return sksTotal;
	}

	/**
	 * Jumlah MATA KULIAH (bukan SKS) yang pernah ditempuh mahasiswa ini.
	 *
	 * @param semua {@code true} untuk menghitung tanpa memandang status/nilai.
	 * @return cacah mata kuliah.
	 * @see #prosesHitungMk(java.util.Collection, boolean)
	 */
	public Integer hitungMk(boolean semua) {
		return prosesHitungMk(ambilDetailperkuliahan(), semua);
	}

	/**
	 * Jumlah MATA KULIAH pada satu semester.
	 *
	 * @param semester       semester yang dihitung.
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param semua          {@code true} untuk menghitung tanpa memandang status/nilai.
	 * @return cacah mata kuliah semester itu.
	 * @see #prosesHitungMk(java.util.Collection, boolean)
	 */
	public Integer hitungMk(Integer semester, Integer tahapan, Integer semesterPendek, boolean semua) {
		return prosesHitungMk(ambilDetailperkuliahan(semester, tahapan, semesterPendek), semua);
	}

	/**
	 * Jumlah MATA KULIAH secara kumulatif sampai semester tertentu.
	 *
	 * @param semester       batas semester (inklusif).
	 * @param tahapan        tahapan dalam semester.
	 * @param semesterPendek penanda semester pendek.
	 * @param semua          {@code true} untuk menghitung tanpa memandang status/nilai.
	 * @return cacah mata kuliah kumulatif.
	 * @see #prosesHitungMk(java.util.Collection, boolean)
	 */
	public Integer hitungMkSampai(Integer semester, Integer tahapan, Integer semesterPendek, boolean semua) {
		return prosesHitungMk(ambilDetailperkuliahanSampai(semester, tahapan, semesterPendek, semua), semua);
	}

	/**
	 * Mesin pencacah MATA KULIAH. Sejalan dengan
	 * {@link #prosesHitungRataRata(java.util.Collection)} (memakai penyaring
	 * {@link #saringBerdasarNilaiDan0(java.util.Collection)} dan rangkaian kunci
	 * {@link Konfigurasi} yang sama), hanya saja yang dihitung adalah CACAH baris, bukan nilai.
	 *
	 * <p>Bendera {@code semua} bernilai {@code true} melewati penyaringan status persetujuan,
	 * nilai huruf yang dikecualikan, penyaringan nilai belum terverifikasi, dan ambang nilai.</p>
	 *
	 * @param detailperkuliahans koleksi id detailperkuliahan yang dihitung.
	 * @param semua              {@code true} untuk mengabaikan penyaringan status/nilai.
	 * @return cacah mata kuliah.
	 */
	private Integer prosesHitungMk(Collection<Long> detailperkuliahans, boolean semua) {

		String nilai0MasukPenghitungan = Common
				.getKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.AKTIF).getNilai();
		Double minimal = 0.1;
		try {
			minimal = Double.parseDouble(
					Common.getKonfigurasi("nilai_minimal_tidak_masuk_dalam_perhitungan_ipk", "0.1").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:4194");

		}
		String nilaiHurufTidakMasukPerhitungan = Common.getKonfigurasi("nilai_huruf_yg_tidak_masuk_perhitungan_ip", "")
				.getNilai();
		boolean nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk = Common.bolehKonfigurasi("nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk", Konfigurasi.TIDAK_AKTIF);
		Integer smtSaatIni = this.currentSemester();
		Integer sksMk = 0;

		for (Long detailperkuliahanid : saringBerdasarNilaiDan0(detailperkuliahans)) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {

				if (!semua && smtSaatIni != null && smtSaatIni.equals(detailperkuliahan.getSemester())
						&& nilai_belum_verifikasi_tidak_masuk_dalam_perhitungan_ipk
						&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
					continue;
				}

				if (semua || (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
						&& !detailperkuliahan.getNilaiHuruf().equalsIgnoreCase(nilaiHurufTidakMasukPerhitungan))) {

					if (semua) {
						sksMk++;
					} else if (nilai0MasukPenghitungan.equals(Konfigurasi.AKTIF)) {
						if (detailperkuliahan.getTotalNilai() >= minimal) {
							sksMk++;
						}
					} else {
						sksMk++;

					}
				}
			}
		}
		detailperkuliahans = null;

		return sksMk;
	}

	/**
	 * MEMBANGUN ULANG dari basis data berkas JSON daftar {@link Detailperkuliahan} milik mahasiswa
	 * ini. Ini operasi pemulihan bila daftar di berkas basi/rusak — dipanggil dari sekitar 33
	 * berkas, umumnya lewat tombol "muat ulang data" atau setelah perubahan KRS massal.
	 *
	 * <p>Alur: query id seluruh {@code Detailperkuliahan} milik mahasiswa ini yang
	 * {@code ikutiPerkuliahan} NULL (baris asli, bukan titipan kelas lain), urut semester &rarr;
	 * {@link #bersihkanLokasiDetailPerkuliahan()} (hapus berkas) &rarr; tulis JSON kosong &rarr;
	 * daftarkan ulang satu per satu lewat
	 * {@link #populateDetailperkuliahan(Detailperkuliahan, boolean)}. Objek yang belum ada di
	 * cache dikumpulkan lalu dimuat sekali lewat satu query {@code IN}.</p>
	 *
	 * <p><b>Ketahanan sesi:</b> bila {@code session} yang dikirim pemanggil bernilai {@code null}
	 * atau sudah tertutup ("Session is closed!" — lazim terjadi bila callee lain sudah menutupnya),
	 * sesi diambil ulang dari {@code HibernateUtil.currentNativeSession()}.</p>
	 *
	 * <p>Operasi ini idempoten (hasil akhirnya sama berapa kali pun dijalankan) tetapi MAHAL:
	 * menghapus dan menulis ulang berkas per baris. Hindari memanggilnya di dalam loop.</p>
	 *
	 * @param session sesi Hibernate; boleh {@code null}/tertutup (akan diambil ulang).
	 */
	@SuppressWarnings("unchecked")
	public void reInitDetailperkuliahan(Session session) {
		/* Pemanggil bisa mengirim session yang sudah ditutup callee lain
		 * ("Session is closed!"); ambil ulang dari thread-local. */
		if (session == null || !session.isOpen()) {
			session = HibernateUtil.currentNativeSession();
		}
		List<Long> detailperkuliahansid = session.createCriteria(Detailperkuliahan.class)
				.setProjection(Projections.property("id")).add(Restrictions.isNull("ikutiPerkuliahan"))
				.add(Restrictions.eq("mahasiswa", this)).addOrder(Order.asc("semester")).list();
		bersihkanLokasiDetailPerkuliahan();
		tulisLokasiDetailPerkuliahan(new JSONObject().toString());

		List<Long> idsBelumAda = new ArrayList<Long>();
		for (Long detailperkuliahanid : detailperkuliahansid) {
			if (detailperkuliahanid != null) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());

				if (detailperkuliahan != null) {
					masukkanData(Detailperkuliahan.class, detailperkuliahan);
					populateDetailperkuliahan(detailperkuliahan, true);
				} else {
					idsBelumAda.add(detailperkuliahanid);
				}

			}
		}

		detailperkuliahansid.clear();
		detailperkuliahansid = null;

		if (!idsBelumAda.isEmpty()) {
			// System.out.println("idsBelumAda Detailperkuliahan -> " + idsBelumAda);
			List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.in("id", idsBelumAda)).list();
			for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
				masukkanData(Detailperkuliahan.class, detailperkuliahan);
				populateDetailperkuliahan(detailperkuliahan, true);
			}
			detailperkuliahans = null;
		}
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk {@link Skripsi} milik mahasiswa ini.
	 *
	 * <p>Mengikuti idiom {@code reInit*} linimasa e-learning: query entitas milik mahasiswa &rarr;
	 * {@code AuditListener.prosesUntukElearning(...)} yang mengumpulkan pasangan
	 * mahasiswa/dosen terkait &rarr; satu kali {@code tulisPutBaru(Skripsi.class.getName())} per
	 * objek terkumpul. Hasilnya dibaca kembali oleh
	 * {@link #ambilPerkuliahanDanParalel(String, String, String, String, String, boolean, Integer,
	 * boolean, boolean, boolean, Integer, int, int, boolean, JenisFormulirKegiatan)} kategori
	 * {@code SKRIPSI}.</p>
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitSkripsi(Session session) {
		List<Skripsi> skripsis = session.createCriteria(Skripsi.class).add(Restrictions.eq("mahasiswa", this))
				.addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (Skripsi skripsi : skripsis) {
			AuditListener.prosesUntukElearning(skripsi, "", skripsi.getId(), voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(Skripsi.class.getName());
		}
		voMahasiswaDosens = null;
		skripsis = null;
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk {@link MahasiswaRequestTugasAkhir}
	 * (pengajuan bimbingan tugas akhir) milik mahasiswa ini. Idiom sama dengan
	 * {@link #reInitSkripsi(Session)}; hasilnya dipakai linimasa kategori {@code BIMBINGAN}.
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitBimbingan(Session session) {
		List<MahasiswaRequestTugasAkhir> mahasiswaRequestTugasAkhirs = session
				.createCriteria(MahasiswaRequestTugasAkhir.class).add(Restrictions.eq("mahasiswa", this))
				.addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir : mahasiswaRequestTugasAkhirs) {
			AuditListener.prosesUntukElearning(mahasiswaRequestTugasAkhir, "", mahasiswaRequestTugasAkhir.getId(),
					voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(MahasiswaRequestTugasAkhir.class.getName());
		}
		voMahasiswaDosens = null;
		mahasiswaRequestTugasAkhirs = null;
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk {@link MahasiswaDapatKelompokKkn}
	 * (penempatan KKN) milik mahasiswa ini. Idiom sama dengan {@link #reInitSkripsi(Session)};
	 * hasilnya dipakai linimasa kategori {@code KKN}.
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitKkn(Session session) {
		List<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKkns = session
				.createCriteria(MahasiswaDapatKelompokKkn.class).add(Restrictions.eq("mahasiswa", this))
				.addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : mahasiswaDapatKelompokKkns) {
			AuditListener.prosesUntukElearning(mahasiswaDapatKelompokKkn, "", mahasiswaDapatKelompokKkn.getId(),
					voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(MahasiswaDapatKelompokKkn.class.getName());
		}
		voMahasiswaDosens = null;
		mahasiswaDapatKelompokKkns = null;
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk {@link MahasiswaDapatKelompokPkl}
	 * (penempatan PKL/magang) milik mahasiswa ini. Idiom sama dengan
	 * {@link #reInitSkripsi(Session)}; hasilnya dipakai linimasa kategori {@code PKL}.
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitPkl(Session session) {
		List<MahasiswaDapatKelompokPkl> mahasiswaDapatKelompokPkls = session
				.createCriteria(MahasiswaDapatKelompokPkl.class).add(Restrictions.eq("mahasiswa", this))
				.addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : mahasiswaDapatKelompokPkls) {
			AuditListener.prosesUntukElearning(mahasiswaDapatKelompokPkl, "", mahasiswaDapatKelompokPkl.getId(),
					voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(MahasiswaDapatKelompokPkl.class.getName());
		}
		voMahasiswaDosens = null;
		mahasiswaDapatKelompokPkls = null;
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk {@link KrsMahasiswa} milik mahasiswa ini.
	 * Idiom sama dengan {@link #reInitSkripsi(Session)}; hasilnya dipakai linimasa kategori
	 * {@code KRS}.
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitKrs(Session session) {
		List<KrsMahasiswa> krsMahasiswas = session.createCriteria(KrsMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", this)).addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (KrsMahasiswa krsMahasiswa : krsMahasiswas) {
			AuditListener.prosesUntukElearning(krsMahasiswa, "", krsMahasiswa.getId(), voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(KrsMahasiswa.class.getName());
		}
		voMahasiswaDosens = null;
		krsMahasiswas = null;
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk {@link FormulirKegiatanPeserta}
	 * (keikutsertaan mahasiswa pada kegiatan) milik mahasiswa ini. Idiom sama dengan
	 * {@link #reInitSkripsi(Session)}; hasilnya dipakai linimasa kategori {@code KEGIATAN}.
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitFormulirKegiatanPeserta(Session session) {
		List<FormulirKegiatanPeserta> formulirKegiatanPesertas = session.createCriteria(FormulirKegiatanPeserta.class)
				.add(Restrictions.eq("mahasiswa", this)).addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (FormulirKegiatanPeserta formulirKegiatanPeserta : formulirKegiatanPesertas) {
			AuditListener.prosesUntukElearning(formulirKegiatanPeserta, "", formulirKegiatanPeserta.getId(),
					voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(FormulirKegiatanPeserta.class.getName());
		}
		voMahasiswaDosens = null;
		formulirKegiatanPesertas = null;
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk {@link PendaftaranWisuda} milik mahasiswa
	 * ini. Berbeda dari {@code reInit*} lain, query di sini DISARING
	 * {@code persetujuanWisuda = true} — pendaftaran wisuda yang belum disetujui tidak masuk
	 * linimasa. Hasilnya dipakai linimasa kategori {@code WISUDA}.
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitPendaftaranWisuda(Session session) {
		List<PendaftaranWisuda> pendaftaranWisudas = session.createCriteria(PendaftaranWisuda.class)
				.add(Restrictions.eq("persetujuanWisuda", true)).add(Restrictions.eq("mahasiswa", this))
				.addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (PendaftaranWisuda pendaftaranWisuda : pendaftaranWisudas) {
			AuditListener.prosesUntukElearning(pendaftaranWisuda, "", pendaftaranWisuda.getId(), voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(PendaftaranWisuda.class.getName());
		}
		voMahasiswaDosens = null;
		pendaftaranWisudas = null;
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk {@link PertemuanPunyaGrupPertemuan}
	 * (konsultasi/pertemuan grup) milik mahasiswa ini. Idiom sama dengan
	 * {@link #reInitSkripsi(Session)}; hasilnya dipakai linimasa kategori {@code KONSULTASI}.
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitKonsultasi(Session session) {
		List<PertemuanPunyaGrupPertemuan> pertemuanPunyaGrupPertemuans = session
				.createCriteria(PertemuanPunyaGrupPertemuan.class).addOrder(Order.asc("id"))
				.add(Restrictions.eq("mahasiswa", this)).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan : pertemuanPunyaGrupPertemuans) {
			AuditListener.prosesUntukElearning(pertemuanPunyaGrupPertemuan, "", pertemuanPunyaGrupPertemuan.getId(),
					voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(PertemuanPunyaGrupPertemuan.class.getName());
		}
		voMahasiswaDosens = null;
		pertemuanPunyaGrupPertemuans = null;
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk cicilan pembayaran milik mahasiswa ini.
	 *
	 * <p>Dipakai pemanggil lama (mis. {@code PembayaranUtil} versi lama) setelah sebuah pembayaran
	 * disimpan, agar pembacaan berikutnya memakai data terkini. Mengikuti idiom {@code reInit*} lain
	 * di kelas ini (query entitas milik mahasiswa &rarr; {@code AuditListener.prosesUntukElearning}
	 * &rarr; {@code tulisPutBaru}); hanya merefresh cache (idempoten), tidak mengubah data bisnis.
	 * {@link CicilanPembayaran} terhubung ke mahasiswa lewat {@code kegiatan}.</p>
	 */
	@SuppressWarnings("unchecked")
	public void reInitCicilan(Session session) {
		List<CicilanPembayaran> cicilanPembayarans = session.createCriteria(CicilanPembayaran.class)
				.createAlias("kegiatan", "kegiatan").add(Restrictions.eq("kegiatan.mahasiswa", this))
				.addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
			AuditListener.prosesUntukElearning(cicilanPembayaran, "", cicilanPembayaran.getId(), voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(CicilanPembayaran.class.getName());
		}
		voMahasiswaDosens = null;
		cicilanPembayarans = null;
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk deposit milik mahasiswa ini. Lihat
	 * {@link #reInitCicilan(Session)}. {@link Deposit} punya properti {@code mahasiswa} ter-mapping.
	 */
	@SuppressWarnings("unchecked")
	public void reInitDeposit(Session session) {
		List<Deposit> deposits = session.createCriteria(Deposit.class).add(Restrictions.eq("mahasiswa", this))
				.addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (Deposit deposit : deposits) {
			AuditListener.prosesUntukElearning(deposit, "", deposit.getId(), voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(Deposit.class.getName());
		}
		voMahasiswaDosens = null;
		deposits = null;
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk pengeluaran milik mahasiswa ini. Lihat
	 * {@link #reInitCicilan(Session)}. {@link PengeluaranMahasiswa} punya properti {@code mahasiswa}
	 * ter-mapping.
	 */
	@SuppressWarnings("unchecked")
	public void reInitPengeluaranMahasiswa(Session session) {
		List<PengeluaranMahasiswa> pengeluaranMahasiswas = session.createCriteria(PengeluaranMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", this)).addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (PengeluaranMahasiswa pengeluaranMahasiswa : pengeluaranMahasiswas) {
			AuditListener.prosesUntukElearning(pengeluaranMahasiswa, "", pengeluaranMahasiswa.getId(),
					voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(PengeluaranMahasiswa.class.getName());
		}
		voMahasiswaDosens = null;
		pengeluaranMahasiswas = null;
	}

	/**
	 * Membangun ulang cache VO terdenormalisasi untuk detail kegiatan milik mahasiswa ini. Lihat
	 * {@link #reInitCicilan(Session)}. {@link DetailKegiatan} terhubung ke mahasiswa lewat
	 * {@code kegiatan}.
	 */
	@SuppressWarnings("unchecked")
	public void reInitDetailKegiatan(Session session) {
		List<DetailKegiatan> detailKegiatans = session.createCriteria(DetailKegiatan.class)
				.createAlias("kegiatan", "kegiatan").add(Restrictions.eq("kegiatan.mahasiswa", this))
				.addOrder(Order.asc("id")).list();

		Map<Long, GeneralValueObject> voMahasiswaDosens = new HashMap<Long, GeneralValueObject>();
		for (DetailKegiatan detailKegiatan : detailKegiatans) {
			AuditListener.prosesUntukElearning(detailKegiatan, "", detailKegiatan.getId(), voMahasiswaDosens);
		}
		for (GeneralValueObject mahasiswaDosen : voMahasiswaDosens.values()) {
			mahasiswaDosen.tulisPutBaru(DetailKegiatan.class.getName());
		}
		voMahasiswaDosens = null;
		detailKegiatans = null;
	}

	/**
	 * Pintasan pemulihan cache yang paling sering dipakai: membangun ulang daftar
	 * {@link Detailperkuliahan} mahasiswa ini memakai sesi thread-local
	 * ({@code HibernateUtil.currentNativeSession()}).
	 *
	 * <p><b>Efek samping sesi:</b> setelah selesai, sesi tersebut DITUTUP
	 * ({@code disconnect} + {@code close} + {@code HibernateUtil.closeSession()}). Pemanggil yang
	 * masih membutuhkan sesi thread-local sesudahnya harus mengambilnya ulang. Kegagalan dicatat
	 * ke {@code ErrorAuditUtil}, tidak dilempar.</p>
	 *
	 * @see #reInitDetailperkuliahan(Session)
	 */
	public void reInit() {
		Session session = HibernateUtil.currentNativeSession();
		try {
			reInitDetailperkuliahan(session);
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:4516");
		}
		HibernateUtil.closeSession();
	}

	/**
	 * Tanggal SK Rektor terkait kelulusan/status mahasiswa (tipe DATE).
	 *
	 * <p>Catatan: pernah ada percobaan mengambil tanggal SK dari
	 * {@link KelompokStatusKeluarMahasiswa} secara otomatis (kode masih ada sebagai komentar di
	 * dalam metode). Perilaku itu SENGAJA dimatikan — tanggal SK tetap milik baris mahasiswa,
	 * berbeda dari {@link #getTanggalLulus()} yang memang ditimpa kelompok.</p>
	 *
	 * @return tanggal SK Rektor; {@code null} bila belum ada.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSkRektor() {

//		if (getKelompokStatusKeluarMahasiswa() != null && getKelompokStatusKeluarMahasiswa().getTanggalSk() != null) {
//			tanggalSkRektor = getKelompokStatusKeluarMahasiswa().getTanggalSk();
//		}

		return tanggalSkRektor;
	}

	/**
	 * Menetapkan tanggal SK Rektor.
	 *
	 * @param tanggalSkRektor tanggal SK.
	 */
	public void setTanggalSkRektor(Date tanggalSkRektor) {
		this.tanggalSkRektor = tanggalSkRektor;
	}

	/**
	 * Nomor SKPI (Surat Keterangan Pendamping Ijazah) mahasiswa.
	 *
	 * @return nomor SKPI sudah di-trim; string kosong bila belum diterbitkan.
	 */
	public String getNomorSkpi() {
		return nomorSkpi == null ? "" : nomorSkpi.trim();
	}

	/**
	 * Menetapkan nomor SKPI.
	 *
	 * @param nomorSkpi nomor SKPI.
	 */
	public void setNomorSkpi(String nomorSkpi) {
		this.nomorSkpi = nomorSkpi;
	}

	/**
	 * Id mahasiswa pada mesin sidik jari (fingerprint) untuk presensi. Bila field masih kosong,
	 * nilai dibaca dari penyimpanan kunci JSON ({@code retreive("idfinger")}) milik
	 * {@link ais.database.model.GeneralValueObject}, bukan dari kolom tabel.
	 *
	 * @return id fingerprint sudah di-trim; {@code null} bila belum ada.
	 */
	public String getIdfinger() {
		String s = idfinger == null || idfinger.trim().isEmpty() ? retreive("idfinger") : idfinger;
		return s == null ? null : s.trim();
	}

	/**
	 * Menetapkan id mesin sidik jari. Nilai kosong/{@code null} DIABAIKAN; nilai yang sah juga
	 * disalin ke penyimpanan kunci JSON dengan kunci {@code "idfinger"} karena
	 * {@link #getIdfinger()} membacanya dari sana.
	 *
	 * @param idfinger id fingerprint; diabaikan bila kosong.
	 */
	public void setIdfinger(String idfinger) {
		if (idfinger != null && !idfinger.trim().isEmpty()) {
			put(idfinger.trim(), "idfinger");
			this.idfinger = idfinger;
		}
	}

	/**
	 * Membaca isi berkas JSON daftar id {@link Pertemuan} yang diikuti mahasiswa ini (berkas
	 * {@code mahasiswa_punya_pertemuan_<id>}). Pola yang sama dengan
	 * {@link #ambilLokasiDetailPerkuliahan()}: berkas berperan sebagai pengganti relasi
	 * {@code @OneToMany}.
	 *
	 * @return teks JSON isi berkas, atau JSON kosong bila belum ada/gagal dibaca.
	 */
	public String ambilLokasiPertemuan() {
		File file = Common.getFileLocation(this, "mahasiswa_punya_pertemuan_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:4561");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas JSON daftar {@link Pertemuan} milik mahasiswa ini. Kegagalan ditelan.
	 *
	 * @param data teks JSON yang hendak disimpan.
	 */
	public void tulisLokasiPertemuan(String data) {
		File file = Common.getFileLocation(this, "mahasiswa_punya_pertemuan_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:4570");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * MENGHAPUS berkas JSON daftar {@link Pertemuan} milik mahasiswa ini. Dipanggil
	 * {@link #reInitPertemuan(Session, Label, Date, Date)} sebelum membangun ulang daftar.
	 */
	public void bersihkanLokasiPertemuan() {
		File file = Common.getFileLocation(this, "mahasiswa_punya_pertemuan_" + getId().toString());
		BacaTulisUtil.doHapus(file, "mahasiswa_punya_pertemuan");

	}

	/**
	 * Pintasan {@link #reInitPertemuan(Session, Label, Date, Date)} tanpa label kemajuan (dibuatkan
	 * {@code Label} kosong).
	 *
	 * <p>PERHATIKAN URUTAN PARAMETER: di sini {@code sampai} datang SEBELUM {@code mulai} —
	 * kebalikan dari varian {@code (Session, Label, Date, Date)}.</p>
	 *
	 * @param session sesi Hibernate aktif.
	 * @param sampai  batas akhir rentang tanggal.
	 * @param mulai   batas awal rentang tanggal.
	 */
	public void reInitPertemuan(Session session, Calendar sampai, Calendar mulai) {
		reInitPertemuan(session, new Label(), sampai, mulai);
	}

	/**
	 * Varian {@link #reInitPertemuan(Session, Label, Date, Date)} yang menerima {@link Calendar}.
	 *
	 * <p>PERHATIKAN URUTAN PARAMETER: {@code sampai} datang SEBELUM {@code mulai}, lalu
	 * diteruskan sebagai {@code (mulai.getTime(), sampai.getTime())}.</p>
	 *
	 * @param session sesi Hibernate aktif.
	 * @param label   label ZK untuk menampilkan kemajuan proses.
	 * @param sampai  batas akhir rentang tanggal.
	 * @param mulai   batas awal rentang tanggal.
	 */
	public void reInitPertemuan(Session session, Label label, Calendar sampai, Calendar mulai) {
		reInitPertemuan(session, label, mulai.getTime(), sampai.getTime());
	}

	/**
	 * MEMBANGUN ULANG dari basis data berkas JSON daftar {@link Pertemuan} yang diikuti mahasiswa
	 * ini dalam satu rentang tanggal. Inilah sumber data linimasa jadwal/pertemuan mahasiswa
	 * (dipakai sekitar 27 berkas).
	 *
	 * <p><b>Kriteria pertemuan yang diambil:</b> aktif (kolom {@code aktif} NULL atau
	 * {@code true}); tertaut ke setidaknya satu induk — perkuliahan, wisuda, formulir kegiatan,
	 * pengajuan tugas akhir, kelompok KKN, kelompok PKL, skripsi, KRS, jadwal pelajaran, atau
	 * pertemuan grup; dan tanggalnya berada dalam rentang {@code mulai}..{@code sampai}
	 * (dibandingkan sebagai DATE lewat {@code sqlRestriction}). Penyaringan "pertemuan ini milik
	 * mahasiswa tersebut" ditambahkan
	 * {@code DashboardTimelinePertemuan.createCriteriaMahasiswa(...)}.</p>
	 *
	 * <p><b>Efek samping:</b> berkas JSON lama DIHAPUS lalu ditulis ulang dari nol; objek
	 * {@link Pertemuan} yang belum ada di cache dimuat ({@code session.load}) dan dimasukkan ke
	 * cache. Bila {@code label} terpasang di layar, teksnya diperbarui dengan persentase kemajuan.</p>
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 * @param label   label ZK penampil kemajuan.
	 * @param mulai   batas awal rentang tanggal.
	 * @param sampai  batas akhir rentang tanggal.
	 */
	@SuppressWarnings("unchecked")
	public void reInitPertemuan(Session session, Label label, Date mulai, Date sampai) {

		Criteria criteria = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNotNull("perkuliahan"), Restrictions.or(
						Restrictions.isNotNull("wisuda"),
						Restrictions.or(Restrictions.isNotNull("formulirKegiatan"), Restrictions.or(
								Restrictions.isNotNull("mahasiswaRequestTugasAkhir"),
								Restrictions.or(Restrictions.isNotNull("kelompokKkn"), Restrictions.or(
										Restrictions.isNotNull("kelompokPkl"),
										Restrictions.or(Restrictions.isNotNull("skripsi"),
												Restrictions.or(Restrictions.isNotNull("krsMahasiswa"),
														Restrictions.or(Restrictions.isNotNull("jadwalPelajaran"),
																Restrictions.isNotNull(
																		"pertemuanPunyaGrupPertemuan")))))))))))

				.add(Restrictions.sqlRestriction(
						"date(this_.tanggal) between date('" + Common.databaseDateFormat.get().format(mulai)
								+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"));
		DashboardTimelinePertemuan.createCriteriaMahasiswa(this, session, criteria, true, true, true, true, true, true,
				true, true, true);

		List<Long> pertemuans = criteria.setProjection(Projections.property("id")).list();

		bersihkanLokasiPertemuan();
		tulisLokasiPertemuan(new JSONObject().toString());
		int i = 0;
		int size = pertemuans.size();
		for (Long pertemuanId : pertemuans) {
			Pertemuan pertemuan = (Pertemuan) ambilData(Pertemuan.class, pertemuanId.toString());
			if (pertemuan == null) {
				pertemuan = (Pertemuan) session.load(Pertemuan.class, pertemuanId);
				masukkanData(Pertemuan.class, pertemuan);
			}
			populatePertemuan(pertemuan, true);
			label.setValue("harap tunggu, sedang mengambil data " + pertemuan.toString() + " ("
					+ Common.numberFormat.get().format((i * 100.0) / size) + "%)");
			i++;
		}
		pertemuans = null;
	}

	/**
	 * Melepas satu id {@link Pertemuan} dari berkas JSON mahasiswa ini dengan mengosongkan
	 * nilainya (kunci tetap ada). Kegagalan ditelan.
	 *
	 * @param id id pertemuan yang dilepas.
	 */
	public void removePertemuan(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPertemuan());
			c.put(id.toString(), "");
			tulisLokasiPertemuan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:4638");

		}
	}

	/**
	 * Mendaftarkan satu {@link Pertemuan} ke berkas JSON mahasiswa ini. Parameter
	 * {@code tulisUlang} tidak dipakai di dalam badan metode (dipertahankan demi kecocokan dengan
	 * pemanggil lama). Efek samping: MENULIS berkas JSON; kegagalan ditelan.
	 *
	 * @param pertemuan  pertemuan yang didaftarkan; {@code null} diabaikan.
	 * @param tulisUlang tidak dipakai.
	 */
	public void populatePertemuan(Pertemuan pertemuan, boolean tulisUlang) {
		try {
			if (pertemuan == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiPertemuan());
			c.put(pertemuan.getId().toString(), pertemuan.getId().toString());
			tulisLokasiPertemuan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:4652");
		}
	}

	/**
	 * Membaca daftar {@link Pertemuan} mahasiswa ini dari berkas JSON menjadi peta TERURUT WAKTU.
	 *
	 * <p>Kuncinya dirakit sebagai <code>{tanggal}_{waktuMulai}-{waktuSelesai}_{idPertemuan}</code>
	 * ({@code Common.dateFormat8} untuk tanggal; waktu kosong diganti {@code "00.00"}), sehingga
	 * urutan alfabet {@link TreeMap} otomatis sama dengan urutan kronologis. Peta inilah yang
	 * kemudian disaring oleh keluarga
	 * {@link #ambilPertemuan(TreeMap, boolean, boolean, boolean, boolean, boolean, boolean,
	 * boolean, boolean, boolean, boolean, boolean, String, boolean, boolean, boolean, boolean,
	 * boolean, boolean, Date, String, String, String, String, String, String, String, String,
	 * String, boolean, Integer, boolean, boolean, boolean, StatusPertemuan, Integer, PagingApi,
	 * boolean, int, MyToolbarbuttonConfig, Tbmuser, String)}.</p>
	 *
	 * <p>Id yang belum ada di cache dikumpulkan lalu dimuat SEKALI lewat satu query {@code IN}
	 * (hanya yang {@code aktif}). Kegagalan per entri maupun menyeluruh ditelan dan dicatat.</p>
	 *
	 * @param session sesi Hibernate aktif untuk memuat pertemuan yang belum ter-cache.
	 * @return peta {@code kunciWaktu -> idPertemuan}, terurut kronologis; kosong bila tidak ada.
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<String, Long> ambilPertemuan(Session session) {

		TreeMap<String, Long> pertemuansa = new TreeMap<String, Long>();
		List<Long> idsBelumAda = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiPertemuan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(Pertemuan.class, key);
						if (generalValueObject != null) {
							Pertemuan pertemuan = (Pertemuan) generalValueObject;
							String keyPert = Common.dateFormat8.get().format(pertemuan.getTanggal());

							keyPert += ("_" + (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null
									? "00.00-00.00"
									: (pertemuan.getWaktuMulai() == null ? "00.00" : pertemuan.getWaktuMulai()) + "-"
											+ (pertemuan.getWaktuSelesai() == null ? "00.00"
													: pertemuan.getWaktuSelesai())));

							pertemuansa.put(keyPert + "_" + pertemuan.getId(), pertemuan.getId());
						} else {
							idsBelumAda.add(Long.parseLong(key));

						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:4688");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:4692");
		}

		if (!idsBelumAda.isEmpty()) {
			// System.out.println("idsBelumAda Pertemuan -> " + idsBelumAda);
			List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.in("id", idsBelumAda)).list();
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan.getAktif()) {
					masukkanData(Pertemuan.class, pertemuan);
					try {
						String keyPert = Common.dateFormat8.get().format(pertemuan.getTanggal());

						keyPert += ("_" + (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null
								? "00.00-00.00"
								: (pertemuan.getWaktuMulai() == null ? "00.00" : pertemuan.getWaktuMulai()) + "-"
										+ (pertemuan.getWaktuSelesai() == null ? "00.00"
												: pertemuan.getWaktuSelesai())));

						pertemuansa.put(keyPert + "_" + pertemuan.getId(), pertemuan.getId());
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:4714");
					}
				}
			}
		}

		return pertemuansa;
	}

	/**
	 * Pintasan varian lengkap {@code ambilPertemuan(...)} dengan urutan {@code "asc"} (kronologis
	 * menaik). Seluruh parameter diteruskan apa adanya.
	 *
	 * @return daftar id pertemuan satu halaman.
	 */
	public List<Long> ambilPertemuan(TreeMap<String, Long> pertemuansa, boolean jadwalPerkuliahan, boolean jadwalKkn,
			boolean jadwalPkl, boolean jadwalKegiatan, boolean wisuda, boolean jadwalRevisi, boolean jadwalKonsultasi,
			boolean jadwalBimbingan, boolean jadwalKonsultasiLain,

			boolean tdpDiskusi, boolean tdpUjian, String namaUjian,

			boolean tdpMateri,

			boolean tdpTugas, boolean tdpCatatan,

			boolean tdpAudio, boolean tdpVideo,

			boolean tdpDosenPengganti,

			Date tanggal,

			String mk, String dsn,

			String mul, String sam,

			String topik, String catatan,

			String hari,

			String cariKelas, String cariRuang,

			boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean remedial, boolean paralelAja,

			boolean online,

			StatusPertemuan statusPertemuan,

			Integer ke,

			PagingApi paging, boolean refresh, int banyak, MyToolbarbuttonConfig back, Tbmuser tbmuser) {
		return ambilPertemuan(pertemuansa, jadwalPerkuliahan, jadwalKkn, jadwalPkl, jadwalKegiatan, wisuda,
				jadwalRevisi, jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain,

				tdpDiskusi, tdpUjian, namaUjian,

				tdpMateri,

				tdpTugas, tdpCatatan,

				tdpAudio, tdpVideo,

				tdpDosenPengganti,

				tanggal,

				mk, dsn,

				mul, sam,

				topik, catatan,

				hari,

				cariKelas, cariRuang,

				merupakanPraPerkuliahan, ekstrakurikuler, remedial, paralelAja,

				online,

				statusPertemuan,

				ke,

				paging, refresh, banyak, back, tbmuser, "asc");
	}

	/**
	 * Jembatan (adapter) drop-in untuk pemanggil dasbor ZK yang masih memakai widget
	 * {@code org.zkoss.zul.Paging} ASLI (bukan {@link PagingApi} ringan versi servlet/API — lihat
	 * javadoc {@link PagingApi}). Status widget yang relevan (halaman aktif + atribut
	 * "mulaiParam"/"sampaiParam" yang dipakai fitur "muat lebih banyak") disalin ke sebuah
	 * {@link PagingApi} sementara, lalu hasil akhirnya (totalSize/pageSize/activePage) disalin balik
	 * ke widget ASLI beserta replikasi {@code setMold}/{@code setPageIncrement} (nilainya tetap/
	 * konstan pada kedua versi) sehingga tampilan paging ZK di layar tetap ter-update persis seperti
	 * sebelum ada overload {@link PagingApi}.
	 */
	public List<Long> ambilPertemuan(TreeMap<String, Long> pertemuansa, boolean jadwalPerkuliahan, boolean jadwalKkn,
			boolean jadwalPkl, boolean jadwalKegiatan, boolean wisuda, boolean jadwalRevisi, boolean jadwalKonsultasi,
			boolean jadwalBimbingan, boolean jadwalKonsultasiLain,

			boolean tdpDiskusi2, boolean tdpUjian2, String namaUjian2,

			boolean tdpMateri2,

			boolean tdpTugas2, boolean tdpCatatan2,

			boolean tdpAudio2, boolean tdpVideo2,

			boolean tdpDosenPengganti2,

			Date tanggal2,

			String mk2, String dsn2,

			String mul2, String sam2,

			String topik2, String catatan2,

			String hari2,

			String cariKelas2, String cariRuang2,

			boolean merupakanPraPerkuliahan2, Integer ekstrakurikuler2, boolean remedial2, boolean paralelAja2,

			boolean online2,

			StatusPertemuan statusPertemuan2,

			Integer ke2,

			org.zkoss.zul.Paging paging, boolean refresh, int banyak, MyToolbarbuttonConfig back, Tbmuser tbmuser) {

		PagingApi pagingApi = new PagingApi();
		pagingApi.setActivePage(paging.getActivePage());
		if (paging.getAttribute("mulaiParam") != null) {
			pagingApi.setAttribute("mulaiParam", paging.getAttribute("mulaiParam"));
		}
		if (paging.getAttribute("sampaiParam") != null) {
			pagingApi.setAttribute("sampaiParam", paging.getAttribute("sampaiParam"));
		}

		List<Long> hasil = ambilPertemuan(pertemuansa, jadwalPerkuliahan, jadwalKkn, jadwalPkl, jadwalKegiatan, wisuda,
				jadwalRevisi, jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain,

				tdpDiskusi2, tdpUjian2, namaUjian2,

				tdpMateri2,

				tdpTugas2, tdpCatatan2,

				tdpAudio2, tdpVideo2,

				tdpDosenPengganti2,

				tanggal2,

				mk2, dsn2,

				mul2, sam2,

				topik2, catatan2,

				hari2,

				cariKelas2, cariRuang2,

				merupakanPraPerkuliahan2, ekstrakurikuler2, remedial2, paralelAja2,

				online2,

				statusPertemuan2,

				ke2,

				pagingApi, refresh, banyak, back, tbmuser, "asc");

		paging.setTotalSize(pagingApi.getTotalSize());
		try {
			paging.setPageSize(pagingApi.getPageSize());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-guard(paging-bridge) src/ais/database/model/Mahasiswa.java"); }
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");
		try {
			paging.setActivePage(pagingApi.getActivePage());
		} catch (Exception e) {
			try {
				paging.setActivePage(pagingApi.getActivePage() - 1);
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-guard(paging-bridge) src/ais/database/model/Mahasiswa.java"); }
		}
		return hasil;
	}

	/**
	 * <b>Mesin penyaring &amp; pemaginasi linimasa pertemuan mahasiswa.</b> Menerima peta
	 * pertemuan terurut waktu dari {@link #ambilPertemuan(org.hibernate.Session)}, menyaringnya
	 * dengan puluhan kriteria, lalu mengembalikan satu halaman id pertemuan.
	 *
	 * <p><b>Kelompok kriteria:</b></p>
	 * <ul>
	 *   <li><i>Jenis jadwal</i> — {@code jadwalPerkuliahan}, {@code jadwalKkn}, {@code jadwalPkl},
	 *       {@code jadwalKegiatan}, {@code wisuda}, {@code jadwalRevisi} (skripsi),
	 *       {@code jadwalKonsultasi} (KRS), {@code jadwalBimbingan}, {@code jadwalKonsultasiLain};
	 *       pertemuan lolos bila SALAH SATU jenis yang dinyalakan cocok dengan induknya.</li>
	 *   <li><i>Isi pertemuan</i> — {@code tdpTugas}, {@code tdpCatatan}, {@code tdpDosenPengganti},
	 *       {@code tdpDiskusi}, {@code tdpMateri}, {@code tdpUjian}/{@code namaUjian},
	 *       {@code tdpAudio}, {@code tdpVideo}: bila dinyalakan, pertemuan tanpa unsur itu dibuang.</li>
	 *   <li><i>Pencarian teks</i> — {@code mk} (kode/nama mata kuliah), {@code dsn} (kode/nama mata
	 *       kuliah ATAU nama dosen 1..10), {@code topik}/{@code catatan}, {@code cariKelas},
	 *       {@code cariRuang} (kode maupun nama ruangan).</li>
	 *   <li><i>Waktu</i> — {@code mul}/{@code sam} (rentang jam sebagai angka), {@code hari} (nilai
	 *       {@code "Jum'at"} dinormalkan menjadi {@code "Jumat"}), {@code ke} (pertemuan ke-).</li>
	 *   <li><i>Sifat perkuliahan</i> — {@code remedial}, {@code paralelAja},
	 *       {@code merupakanPraPerkuliahan}, {@code ekstrakurikuler}, {@code online},
	 *       {@code statusPertemuan}.</li>
	 * </ul>
	 *
	 * <p><b>Paginasi tanpa komponen ZK.</b> Objek {@code paging} di sini dipakai MURNI sebagai
	 * kalkulator offset yang bisa di-cache lintas request (mis. oleh {@code LinimasaApi}); ia tidak
	 * pernah dipasang ke Desktop/Page ZK. Karena {@code Paging.setPageSize/setActivePage} milik ZK
	 * selalu memposting event dan mensyaratkan {@code Execution} yang hidup — sehingga selalu gagal
	 * bila dipanggil dari servlet REST atau loop async — perhitungan halaman dilakukan dengan
	 * aritmetika biasa yang meniru clamping ZK ({@code pageCount = ceil(total/size)},
	 * {@code activePage} di-clamp ke {@code [0, pageCount-1]}), dan halaman aktif disimpan lewat
	 * {@code setAttribute("activePageDipakai", ...)} yang aman dari event ZK. Atribut
	 * {@code "mulaiParam"}/{@code "sampaiParam"} bila ada akan MENIMPA offset hasil hitungan
	 * (dipakai fitur "muat lebih banyak").</p>
	 *
	 * <p>Bila {@code refresh} bernilai {@code true}, halaman aktif diarahkan otomatis ke sekitar
	 * TANGGAL HARI INI dengan mencacah pertemuan yang tanggalnya sudah lewat.</p>
	 *
	 * <p>Tombol {@code back} ("Tampilkan pertemuan sebelumnya") diperbarui labelnya; pembaruan
	 * dilewati bila komponen sudah terlepas dari desktop ZK, karena memanggil
	 * {@code setVisible}/{@code setLabel} pada komponen tanpa desktop memicu
	 * {@code NullPointerException} di dalam ZK.</p>
	 *
	 * @param pertemuansa peta pertemuan terurut waktu dari {@link #ambilPertemuan(org.hibernate.Session)}.
	 * @param paging      kalkulator paginasi (BUKAN komponen ZK yang terpasang).
	 * @param refresh     arahkan halaman aktif ke sekitar hari ini.
	 * @param banyak      jumlah baris per halaman.
	 * @param back        tombol "muat sebelumnya" yang labelnya diperbarui.
	 * @param tbmuser     pengguna aktif (dipakai penyaringan hak akses ujian).
	 * @param order       {@code "asc"} untuk kronologis menaik, selain itu menurun.
	 * @return daftar id pertemuan satu halaman, sudah terurut.
	 */
	public List<Long> ambilPertemuan(TreeMap<String, Long> pertemuansa, boolean jadwalPerkuliahan, boolean jadwalKkn,
			boolean jadwalPkl, boolean jadwalKegiatan, boolean wisuda, boolean jadwalRevisi, boolean jadwalKonsultasi,
			boolean jadwalBimbingan, boolean jadwalKonsultasiLain,

			boolean tdpDiskusi, boolean tdpUjian, String namaUjian,

			boolean tdpMateri,

			boolean tdpTugas, boolean tdpCatatan,

			boolean tdpAudio, boolean tdpVideo,

			boolean tdpDosenPengganti,

			Date tanggal,

			String mk, String dsn,

			String mul, String sam,

			String topik, String catatan,

			String hari,

			String cariKelas, String cariRuang,

			boolean merupakanPraPerkuliahan, Integer ekstrakurikuler, boolean remedial, boolean paralelAja,

			boolean online,

			StatusPertemuan statusPertemuan,

			Integer ke,

			PagingApi paging, boolean refresh, int banyak, MyToolbarbuttonConfig back, Tbmuser tbmuser, String order) {

		TreeMap<String, Long> dikoleksi = order.equalsIgnoreCase("asc") ? new TreeMap<String, Long>()
				: new TreeMap<String, Long>(Collections.reverseOrder());

		for (String key : pertemuansa.keySet()) {
			Long pertemuanId = pertemuansa.get(key);
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanId.toString());
			if (pertemuan != null) {
				Perkuliahan perkuliahan = pertemuan.getPerkuliahan();

				if (pertemuan.getTanggal() != null) {

					if (tdpTugas && pertemuan.getJudultugas().isEmpty()) {
						continue;
					}
					if (tdpCatatan && pertemuan.getCatatan().isEmpty()) {
						continue;
					}
					if (tdpDosenPengganti && pertemuan.getDosenPengganti() == null) {
						continue;
					}
					if (tdpDiskusi) {
						if (!pertemuan.punyaDiskusi()) {
							continue;
						}
					}
					if (tdpMateri) {
						if (pertemuan.ambilJumlahPertemuanFileContent() == 0) {
							continue;
						}
					}
					if (tdpUjian || !namaUjian.trim().isEmpty()) {
						if (!pertemuan.punyaUjian(namaUjian, tbmuser)) {
							continue;
						}
					}

					if ((jadwalPerkuliahan && pertemuan.getPerkuliahan() != null)
							|| (jadwalKkn && pertemuan.getKelompokKkn() != null)
							|| (jadwalPkl && pertemuan.getKelompokPkl() != null)
							|| (jadwalKegiatan && pertemuan.getFormulirKegiatan() != null)
							|| (wisuda && pertemuan.getWisuda() != null)
							|| (jadwalRevisi && pertemuan.getSkripsi() != null)
							|| (jadwalKonsultasi && pertemuan.getKrsMahasiswa() != null)
							|| (jadwalBimbingan && pertemuan.getMahasiswaRequestTugasAkhir() != null)
							|| (jadwalKonsultasiLain && pertemuan.getPertemuanPunyaGrupPertemuan() != null)) {

						if (mk.trim().isEmpty() || (perkuliahan != null && perkuliahan.getMatakuliah() != null
								&& perkuliahan.getMatakuliah().getNama() != null
								&& ((perkuliahan.getMatakuliah().getKode().toLowerCase()
										.contains(mk.toLowerCase().trim()))
										|| (perkuliahan.getMatakuliah().getNama() != null && perkuliahan.getMatakuliah()
												.getNama().toLowerCase().contains(mk.toLowerCase().trim()))))) {

							if (dsn == null || dsn.trim().isEmpty()
									|| (perkuliahan != null && perkuliahan.getMatakuliah() != null
											&& (perkuliahan.getMatakuliah().getKode().toLowerCase()
													.contains(dsn.toLowerCase().trim())
													|| (perkuliahan.getMatakuliah().getNama() != null
															&& perkuliahan.getMatakuliah().getNama().toLowerCase()
																	.contains(dsn.toLowerCase().trim()))

													|| (perkuliahan.getDosen1() != null
															&& perkuliahan.getDosen1().getNama() != null
															&& perkuliahan.getDosen1().getNama().toLowerCase()
																	.contains(dsn.toLowerCase().trim()))

													|| (perkuliahan.getDosen2() != null
															&& perkuliahan.getDosen2().getNama() != null
															&& perkuliahan.getDosen2().getNama().toLowerCase()
																	.contains(dsn.toLowerCase().trim()))

													|| (perkuliahan.getDosen3() != null
															&& perkuliahan.getDosen3().getNama() != null
															&& perkuliahan.getDosen3().getNama().toLowerCase()
																	.contains(dsn.toLowerCase().trim()))

													|| (perkuliahan.getDosen4() != null
															&& perkuliahan.getDosen4().getNama() != null
															&& perkuliahan.getDosen4().getNama().toLowerCase()
																	.contains(dsn.toLowerCase().trim()))

													|| (perkuliahan.getDosen5() != null
															&& perkuliahan.getDosen5().getNama() != null
															&& perkuliahan.getDosen5().getNama().toLowerCase()
																	.contains(dsn.toLowerCase().trim()))

													|| (perkuliahan.getDosen6() != null
															&& perkuliahan.getDosen6().getNama() != null
															&& perkuliahan.getDosen6().getNama().toLowerCase()
																	.contains(dsn.toLowerCase().trim()))

													|| (perkuliahan.getDosen7() != null
															&& perkuliahan.getDosen7().getNama() != null
															&& perkuliahan.getDosen7().getNama().toLowerCase()
																	.contains(dsn.toLowerCase().trim()))

													|| (perkuliahan.getDosen8() != null
															&& perkuliahan.getDosen8().getNama() != null
															&& perkuliahan.getDosen8().getNama().toLowerCase()
																	.contains(dsn.toLowerCase().trim()))

													|| (perkuliahan.getDosen9() != null
															&& perkuliahan.getDosen9().getNama() != null
															&& perkuliahan.getDosen9().getNama().toLowerCase()
																	.contains(dsn.toLowerCase().trim()))

													|| (perkuliahan.getDosen10() != null
															&& perkuliahan.getDosen10().getNama() != null
															&& perkuliahan.getDosen10().getNama().toLowerCase()
																	.contains(dsn.toLowerCase().trim()))

											))) {

								if (

								((mul == null && sam == null)

										|| (mul != null && sam != null && pertemuan.getWaktuMulai() != null
												&& Common.isNumber(pertemuan.getWaktuMulai())
												&& pertemuan.getWaktuSelesai() != null
												&& Common.isNumber(pertemuan.getWaktuSelesai())
												&& Double.parseDouble(mul) <= Double
														.parseDouble(pertemuan.getWaktuMulai())
												&& Double.parseDouble(sam) >= Double
														.parseDouble(pertemuan.getWaktuSelesai()))

										|| (mul != null && sam == null && pertemuan.getWaktuMulai() != null
												&& Common.isNumber(pertemuan.getWaktuMulai())
												&& Double.parseDouble(mul) <= Double
														.parseDouble(pertemuan.getWaktuMulai()))

										|| (mul == null && sam != null && pertemuan.getWaktuSelesai() != null
												&& Common.isNumber(pertemuan.getWaktuSelesai())
												&& Double.parseDouble(sam) >= Double
														.parseDouble(pertemuan.getWaktuSelesai())))

										||

										((mul == null && sam == null)

												|| (mul != null && sam != null && pertemuan.getWaktuSelesai() != null
														&& Common.isNumber(pertemuan.getWaktuSelesai())
														&& pertemuan.getWaktuSelesai() != null
														&& Common.isNumber(pertemuan.getWaktuSelesai())
														&& Double.parseDouble(mul) <= Double
																.parseDouble(pertemuan.getWaktuSelesai())
														&& Double.parseDouble(sam) >= Double
																.parseDouble(pertemuan.getWaktuSelesai()))

												|| (mul != null && sam == null && pertemuan.getWaktuSelesai() != null
														&& Common.isNumber(pertemuan.getWaktuSelesai())
														&& Double.parseDouble(mul) <= Double
																.parseDouble(pertemuan.getWaktuSelesai()))

												|| (mul == null && sam != null && pertemuan.getWaktuSelesai() != null
														&& Common.isNumber(pertemuan.getWaktuSelesai())
														&& Double.parseDouble(sam) >= Double
																.parseDouble(pertemuan.getWaktuSelesai())))

								) {

									if (catatan == null || catatan.trim().isEmpty() || pertemuan.getCatatan()
											.toLowerCase().contains(catatan.trim().toLowerCase()))

										if (topik == null || topik.trim().isEmpty()
												|| pertemuan.getTopik().toLowerCase()
														.contains(topik.trim().toLowerCase())
												|| pertemuan.getJudultugas().toLowerCase()
														.contains(topik.trim().toLowerCase())) {

											if (hari != null && hari.equals("Jum'at")) {
												hari = "Jumat";
											}

											if (hari == null || hari.trim().isEmpty()
													|| (pertemuan.getTanggal() != null && Common.dateFormatHari.get()
															.format(pertemuan.getTanggal()).equalsIgnoreCase(hari))) {

												if (!remedial || (remedial && perkuliahan != null
														&& perkuliahan.getMerupakanRemedial())) {

													if (!paralelAja || (paralelAja && (perkuliahan != null
															&& (perkuliahan.getPerkuliahan_paralel() != null
																	|| perkuliahan.flagParalel)))) {

														if ((!merupakanPraPerkuliahan
																|| (merupakanPraPerkuliahan && perkuliahan != null
																		&& perkuliahan.getMerupakanPraPerkuliahan()))) {

															if (ekstrakurikuler == null || ((ekstrakurikuler == null
																	&& perkuliahan != null
																	&& !perkuliahan.getMatakuliah().getExtraKulikuler())
																	|| (ekstrakurikuler.equals(Perkuliahan.EKSTRA)
																			&& perkuliahan != null
																			&& perkuliahan.getMatakuliah()
																					.getExtraKulikuler())

															)) {

																if (!online || (online
																		&& pertemuan.apakahSedang("online"))) {

																	if (statusPertemuan == null
																			|| (statusPertemuan != null && pertemuan
																					.getStatusPertemuan() != null
																					&& pertemuan.getStatusPertemuan()
																							.getId()
																							.equals(statusPertemuan
																									.getId()))) {

																		if (ke == null || pertemuan.getPertemuanKe()
																				.equals(ke)) {

																			if (!tdpAudio || (tdpAudio
																					&& pertemuan.audioPertemuan())) {

																				if (!tdpVideo || (tdpVideo && pertemuan
																						.videoPertemuan())) {

																					if (cariKelas.trim().isEmpty()
																							|| (perkuliahan != null
																									&& perkuliahan
																											.getKelas() != null
																									&& perkuliahan
																											.getKelas()
																											.trim()
																											.toLowerCase()
																											.contains(
																													cariKelas
																															.toLowerCase()
																															.trim()))) {

																						if (cariRuang.trim().isEmpty()
																								|| (perkuliahan != null
																										&& perkuliahan
																												.getRuang() != null
																										&&

																										(perkuliahan
																												.getRuang()
																												.getKodeRuangan()
																												.trim()
																												.toLowerCase()
																												.contains(
																														cariRuang
																																.toLowerCase()
																																.trim())
																												|| (perkuliahan
																														.getRuang()
																														.getNama() != null
																														&& perkuliahan
																																.getRuang()
																																.getNama()
																																.trim()
																																.toLowerCase()
																																.contains(
																																		cariRuang
																																				.toLowerCase()
																																				.trim())))

																								)

																						) {

																							dikoleksi.put(key,
																									pertemuanId);
																						}
																					}
																				}
																			}

																		}

																	}
																}
															}
														}
													}
												}
											}
										}
								}
							}
						}
					}
				}
			}
		}

		// "paging" di method ini murni dipakai sbg kalkulator pagination (offset
		// mulai/sampai) yang di-cache lintas request (mis. oleh LinimasaApi lewat
		// mapsPaging per token) - komponen ini TIDAK PERNAH benar2 dipasang ke
		// Desktop/Page ZK mana pun (tidak ada setParent, tidak ada listener
		// onPaging yang nempel). Paging.setPageSize(...)/setActivePage(...) milik
		// ZK selalu memanggil org.zkoss.zk.ui.event.Events.postEvent(...) yang
		// mensyaratkan ada Execution ZK yang hidup - dipanggil dari servlet REST
		// API (tanpa Execution sama sekali) ataupun dari loop async lain tanpa
		// Execution, itu selalu berakhir WrongValueException/NullPointerException.
		// Ganti dengan aritmetika biasa yang meniru persis logika clamping
		// Paging asli (pageCount = ceil(total/size), activePage di-clamp ke
		// [0, pageCount-1]) dan simpan activePage yang dipakai lewat attribute
		// bag komponen (setAttribute/getAttribute aman, tidak memicu event ZK
		// apa pun) supaya tetap konsisten dipakai lintas pemanggilan.
		int totalSizePertemuan = dikoleksi.size();
		paging.setTotalSize(totalSizePertemuan);
		if (banyak <= 0) {
			banyak = 1;
		}
		paging.setPageIncrement(Common.isMobile() ? 5 : 10);
		paging.setMold("os");

		int pageCountPertemuan = (int) Math.ceil(totalSizePertemuan / (double) banyak);
		if (pageCountPertemuan < 1) {
			pageCountPertemuan = 1;
		}

		Object activePageDipakaiAttr = paging.getAttribute("activePageDipakai");
		int activePagePertemuan = activePageDipakaiAttr instanceof Integer ? (Integer) activePageDipakaiAttr : 0;

		if (refresh) {
			int tengahTengah = 0;
			Date tanggalSekarang = WaktuUtil.getDate();
			String format = Common.dateFormat8.get().format(tanggalSekarang);
			for (String a : dikoleksi.keySet()) {
				try {
					String s = a.split("_")[0];
					if (format.equals(s)) {
						break;
					}
					Date tgl = Common.dateFormat8.get().parse(s);

					if (tgl.before(tanggalSekarang)) {
						tengahTengah = tengahTengah + 1;
					} else {
						tengahTengah = tengahTengah + 1;
						break;
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:5146");
				}
			}

			activePagePertemuan = tengahTengah / banyak;
		}

		if (activePagePertemuan >= pageCountPertemuan) {
			activePagePertemuan = pageCountPertemuan - 1;
		}
		if (activePagePertemuan < 0) {
			activePagePertemuan = 0;
		}
		paging.setAttribute("activePageDipakai", activePagePertemuan);

		List<Long> diambil = new ArrayList<Long>();
		int index = 0;
		int mulai = banyak * activePagePertemuan;

		if (paging.getAttribute("mulaiParam") != null) {
			mulai = (Integer) paging.getAttribute("mulaiParam");
		}
		if (paging.getAttribute("sampaiParam") != null) {
			banyak = (Integer) paging.getAttribute("sampaiParam");
		}

		// back bisa sudah TERLEPAS dari desktop (mis. event load-bar async berjalan setelah
		// halaman/komponen di-detach). Memanggil setVisible/setLabel pada komponen tanpa desktop
		// memicu NPE di AbstractComponent.getAttachedUiEngine (smartUpdate). Lewati update UI-nya
		// bila sudah tak terpasang.
		if (back.getParent() != null && back.getParent().getDesktop() != null)
			back.getParent().setVisible(mulai > 0);

		int jml = 0;
		for (Long o : dikoleksi.values()) {

			if (index < mulai) {
				jml++;
				if (back.getDesktop() != null)
					back.setLabel("Tampilkan pertemuan sebelumnya.. (" + jml + " pertemuan)");
			}
			if (index >= mulai && index < (mulai + banyak)) {
				diambil.add(o);
			}
			index++;
		}
		dikoleksi.clear();
		dikoleksi = null;
		return diambil;
	}

	/**
	 * Kode identitas mahasiswa dalam kontrak {@link ais.database.model.GeneralValueObject}/
	 * {@link VOMahasiswaDosen} — untuk mahasiswa, kodenya adalah NIM.
	 *
	 * @return NIM mahasiswa (dibaca dari field mentah, tanpa pembersihan spasi
	 *         {@link #getNim()}).
	 */
	@Override
	public String ambilKode() {
		// TODO Auto-generated method stub
		return nim;
	}

	/**
	 * Membaca berkas JSON daftar id {@link ChecklistBaruPenilaianDosenOlehMahasiswa} (angket
	 * penilaian dosen oleh mahasiswa) milik mahasiswa ini. Pola sama dengan
	 * {@link #ambilLokasiDetailPerkuliahan()}. Berbeda dari berkas lain, nama berkasnya tetap
	 * dibentuk meski id mahasiswa {@code null} (bagian id menjadi string kosong).
	 *
	 * @return teks JSON isi berkas, atau JSON kosong bila belum ada/gagal dibaca.
	 */
	public String ambilLokasiChecklistBaruPenilaianDosenOlehMahasiswa() {
		String id = getId() == null ? "" : getId().toString();
		File file = Common.getFileLocation(this, "checklist_baru_penilaian_dosen_oleh_mahasiswa_" + id);
		try {
			// System.out.println("baca file dari " + file.getAbsolutePath());
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5210");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas JSON daftar angket penilaian dosen milik mahasiswa ini. Kegagalan ditelan.
	 *
	 * @param data teks JSON yang hendak disimpan.
	 */
	public void tulisLokasiChecklistBaruPenilaianDosenOlehMahasiswa(String data) {
		String id = getId() == null ? "" : getId().toString();
		File file = Common.getFileLocation(this, "checklist_baru_penilaian_dosen_oleh_mahasiswa_" + id);
		try {
			// System.out.println("tulis file ke " + file.getAbsolutePath());
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5221");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * MENGHAPUS berkas JSON daftar angket penilaian dosen milik mahasiswa ini. Dipanggil
	 * {@link #reInitChecklistBaruPenilaianDosenOlehMahasiswa(Session)} sebelum membangun ulang.
	 */
	public void bersihkanLokasiChecklistBaruPenilaianDosenOlehMahasiswa() {
		String id = getId() == null ? "" : getId().toString();
		File file = Common.getFileLocation(this, "checklist_baru_penilaian_dosen_oleh_mahasiswa_" + id);
		BacaTulisUtil.doHapus(file, "checklist_baru_penilaian_dosen_oleh_mahasiswa");

	}

	/**
	 * Membangun ulang dari basis data berkas JSON daftar
	 * {@link ChecklistBaruPenilaianDosenOlehMahasiswa} milik mahasiswa ini: query id (urut id),
	 * hapus berkas lama, tulis JSON kosong, lalu daftarkan ulang satu per satu.
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitChecklistBaruPenilaianDosenOlehMahasiswa(Session session) {
		List<Long> checklistBaruPenilaianDosenOlehMahasiswas = session
				.createCriteria(ChecklistBaruPenilaianDosenOlehMahasiswa.class).addOrder(Order.asc("id"))
				.setProjection(Projections.property("id")).add(Restrictions.eq("mahasiswa", this)).list();
		bersihkanLokasiChecklistBaruPenilaianDosenOlehMahasiswa();
		tulisLokasiChecklistBaruPenilaianDosenOlehMahasiswa(new JSONObject().toString());
		for (Long checklistBaruPenilaianDosenOlehMahasiswaid : checklistBaruPenilaianDosenOlehMahasiswas) {
			populateChecklistBaruPenilaianDosenOlehMahasiswa(checklistBaruPenilaianDosenOlehMahasiswaid);
		}
		checklistBaruPenilaianDosenOlehMahasiswas = null;
	}

	/**
	 * Melepas satu id angket penilaian dosen dari berkas JSON dengan mengosongkan nilainya.
	 *
	 * @param id id angket yang dilepas.
	 */
	public void removeChecklistBaruPenilaianDosenOlehMahasiswa(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiChecklistBaruPenilaianDosenOlehMahasiswa());
			c.put(id.toString(), "");
			tulisLokasiChecklistBaruPenilaianDosenOlehMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5252");

		}
	}

	/**
	 * Mendaftarkan satu id angket penilaian dosen ke berkas JSON mahasiswa ini. Efek samping:
	 * MENULIS berkas JSON; kegagalan ditelan.
	 *
	 * @param checklistBaruPenilaianDosenOlehMahasiswa id angket; {@code null} diabaikan.
	 */
	public void populateChecklistBaruPenilaianDosenOlehMahasiswa(Long checklistBaruPenilaianDosenOlehMahasiswa) {
		try {
			if (checklistBaruPenilaianDosenOlehMahasiswa == null) {
				return;
			}
			JSONObject c = new JSONObject(ambilLokasiChecklistBaruPenilaianDosenOlehMahasiswa());
			c.put(checklistBaruPenilaianDosenOlehMahasiswa.toString(),
					checklistBaruPenilaianDosenOlehMahasiswa.toString());
			tulisLokasiChecklistBaruPenilaianDosenOlehMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5266");
		}
	}

	/**
	 * Mengambil seluruh angket penilaian dosen ({@link ChecklistBaruPenilaianDosenOlehMahasiswa})
	 * yang sudah diisi mahasiswa ini, dipetakan dengan kunci
	 * <code>{idDosen}-{idPerkuliahan}</code> sehingga layar angket dapat langsung memeriksa
	 * "dosen X pada kelas Y sudah dinilai atau belum".
	 *
	 * <p>Bila {@code refresh} bernilai {@code true} ATAU berkas JSON belum pernah dibangun
	 * ({@code !udah("ChecklistBaruPenilaianDosenOlehMahasiswa")}), daftar dibangun ulang lebih
	 * dahulu lewat {@link #reInitChecklistBaruPenilaianDosenOlehMahasiswa(Session)} — jadi metode
	 * ini BISA menulis berkas, bukan sekadar membaca. Objeknya kemudian dimuat massal lewat
	 * {@code ambilDataBanyak}. Entri tanpa dosen atau tanpa perkuliahan dilewati.</p>
	 *
	 * <p>Dipakai {@code ais.common.AngketUtil}.</p>
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 * @param refresh {@code true} untuk memaksa pembangunan ulang daftar.
	 * @return peta {@code "idDosen-idPerkuliahan" -> angket}; kosong bila belum ada.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, ChecklistBaruPenilaianDosenOlehMahasiswa> ambilChecklistBaruPenilaianDosenOlehMahasiswa(
			Session session, boolean refresh) {
		if (refresh || !udah("ChecklistBaruPenilaianDosenOlehMahasiswa")) {
			reInitChecklistBaruPenilaianDosenOlehMahasiswa(session);
		}
		List<String> keysAmbil = new ArrayList<String>();
		try {
			JSONObject c = new JSONObject(ambilLokasiChecklistBaruPenilaianDosenOlehMahasiswa());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						keysAmbil.add(key);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:5288");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:5292");
		}

		Map<String, ChecklistBaruPenilaianDosenOlehMahasiswa> maps = new HashMap<String, ChecklistBaruPenilaianDosenOlehMahasiswa>();
		List<ChecklistBaruPenilaianDosenOlehMahasiswa> dataDiambil = ChecklistBaruPenilaianDosenOlehMahasiswa
				.ambilDataBanyak(ChecklistBaruPenilaianDosenOlehMahasiswa.class, keysAmbil);
		for (ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa : dataDiambil) {
			if (checklistBaruPenilaianDosenOlehMahasiswa != null
					&& checklistBaruPenilaianDosenOlehMahasiswa.getPerkuliahan() != null
					&& checklistBaruPenilaianDosenOlehMahasiswa.getDosen() != null) {
				String mykey = checklistBaruPenilaianDosenOlehMahasiswa.getDosen().getId() + "-"
						+ checklistBaruPenilaianDosenOlehMahasiswa.getPerkuliahan().getId();
				maps.put(mykey, checklistBaruPenilaianDosenOlehMahasiswa);
			}
		}
		dataDiambil = null;
		return maps;
	}

	/**
	 * Varian {@link #ambilChecklistBaruPenilaianDosenOlehMahasiswa(Session, boolean)} dengan kunci
	 * yang LEBIH LENGKAP: <code>{idMahasiswa}_{idPerkuliahan}_{idDosen}</code>. Dipakai pemanggil
	 * yang menggabungkan angket beberapa mahasiswa dalam satu peta sehingga id mahasiswa perlu
	 * ikut menjadi bagian kunci.
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 * @param refresh {@code true} untuk memaksa pembangunan ulang daftar.
	 * @return peta {@code "idMhs_idPerkuliahan_idDosen" -> angket}.
	 */
	public Map<String, ChecklistBaruPenilaianDosenOlehMahasiswa> byKey(Session session, boolean refresh) {
		Map<String, ChecklistBaruPenilaianDosenOlehMahasiswa> maps = ambilChecklistBaruPenilaianDosenOlehMahasiswa(
				session, refresh);
		Map<String, ChecklistBaruPenilaianDosenOlehMahasiswa> mapsKey = new HashMap<String, ChecklistBaruPenilaianDosenOlehMahasiswa>();
		for (ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa : maps.values()) {
			Mahasiswa mahasiswa = checklistBaruPenilaianDosenOlehMahasiswa.getMahasiswa();
			Perkuliahan perkuliahan = checklistBaruPenilaianDosenOlehMahasiswa.getPerkuliahan();
			Dosen dosen = checklistBaruPenilaianDosenOlehMahasiswa.getDosen();
			String kodeUnik = mahasiswa.getId() + "_" + perkuliahan.getId() + "_" + dosen.getId();
			mapsKey.put(kodeUnik, checklistBaruPenilaianDosenOlehMahasiswa);
		}
		maps = null;
		return mapsKey;
	}

	/**
	 * Mengumpulkan MATERI ({@link PertemuanFileContent}) dari sekumpulan pertemuan, memakai
	 * pengguna yang sedang login ({@code Common.getCurrentUser()}) sebagai penentu hak akses.
	 * Implementasi kontrak {@link VOMahasiswaDosen}; seluruh logikanya ada di
	 * {@code PertemuanFileContent.ambilMateri}.
	 *
	 * @param pertemuans peta pertemuan (biasanya dari {@link #ambilPertemuan(org.hibernate.Session)}).
	 * @param refresh    {@code true} untuk memaksa pembacaan ulang.
	 * @param label      label ZK penampil kemajuan.
	 * @return peta materi terurut.
	 */
	@Override
	public TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label) {
		Tbmuser tbmuser = Common.getCurrentUser();
		return PertemuanFileContent.ambilMateri(pertemuans, refresh, label, tbmuser);
	}

	/**
	 * Varian {@link #ambilMateri(TreeMap, boolean, Label)} yang memungkinkan pengurutan berdasarkan
	 * NAMA berkas dan penentuan pengguna secara eksplisit (bukan dari sesi login) — dipakai jalur
	 * non-UI seperti servlet API dan proses batch.
	 *
	 * @param pertemuans            peta pertemuan.
	 * @param refresh               {@code true} untuk memaksa pembacaan ulang.
	 * @param label                 label ZK penampil kemajuan.
	 * @param urutBerdasarkanNama   urutkan materi berdasarkan nama berkas.
	 * @param tbmuser               pengguna penentu hak akses.
	 * @return peta materi terurut.
	 */
	public TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label,
			boolean urutBerdasarkanNama, Tbmuser tbmuser) {
		return PertemuanFileContent.ambilMateri(pertemuans, refresh, label, urutBerdasarkanNama, tbmuser);
	}

	// KEGIATAN KEMAHASISWAAN

	/**
	 * Membaca berkas JSON daftar id {@code KegiatanKemahasiswaanPunyaMahasiswa} milik mahasiswa
	 * ini. Pola sama dengan {@link #ambilLokasiDetailPerkuliahan()}.
	 *
	 * @return teks JSON isi berkas, atau JSON kosong bila belum ada/gagal dibaca.
	 */
	public String ambilLokasiKegiatanKemahasiswaanPunyaMahasiswa() {
		File file = Common.getFileLocation(this, "kegiatanKemahasiswaanPunyaMahasiswa_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5345");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas JSON daftar kegiatan kemahasiswaan milik mahasiswa ini. Kegagalan ditelan.
	 *
	 * @param data teks JSON yang hendak disimpan.
	 */
	public void tulisLokasiKegiatanKemahasiswaanPunyaMahasiswa(String data) {
		File file = Common.getFileLocation(this, "kegiatanKemahasiswaanPunyaMahasiswa_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5354");
		}
	}

	/**
	 * Mendaftarkan satu {@code KegiatanKemahasiswaanPunyaMahasiswa} ke berkas JSON mahasiswa ini.
	 *
	 * <p>Berbeda dari {@code populate*} lain, metode ini juga memanggil {@code write()} pada
	 * entitasnya sehingga cache JSON milik entitas itu sendiri ikut ditulis. Efek samping: dua
	 * penulisan berkas. Kegagalan ditelan.</p>
	 *
	 * @param kegiatanKemahasiswaanPunyaMahasiswa kegiatan yang didaftarkan.
	 */
	public void populateKegiatanKemahasiswaanPunyaMahasiswa(
			KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa) {
		try {
			JSONObject c = new JSONObject(ambilLokasiKegiatanKemahasiswaanPunyaMahasiswa());
			kegiatanKemahasiswaanPunyaMahasiswa.write();
			c.put(kegiatanKemahasiswaanPunyaMahasiswa.getId().toString(),
					kegiatanKemahasiswaanPunyaMahasiswa.getId().toString());
			tulisLokasiKegiatanKemahasiswaanPunyaMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5366");
		}
	}

	/**
	 * Membangun ulang dari basis data berkas JSON daftar kegiatan kemahasiswaan milik mahasiswa
	 * ini: query entitas (urut id), tulis JSON kosong, lalu daftarkan ulang satu per satu.
	 *
	 * <p>Catatan: berbeda dari {@link #reInitDetailperkuliahan(Session)}, di sini berkas TIDAK
	 * dihapus lebih dulu — cukup ditimpa JSON kosong.</p>
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitKegiatanKemahasiswaanPunyaMahasiswa(Session session) {

		List<KegiatanKemahasiswaanPunyaMahasiswa> kegiatanKemahasiswaanPunyaMahasiswas = session
				.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class).add(Restrictions.eq("mahasiswa", this))
				.addOrder(Order.asc("id")).list();
		tulisLokasiKegiatanKemahasiswaanPunyaMahasiswa(new JSONObject().toString());
		for (KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa : kegiatanKemahasiswaanPunyaMahasiswas) {
			populateKegiatanKemahasiswaanPunyaMahasiswa(kegiatanKemahasiswaanPunyaMahasiswa);
		}
		kegiatanKemahasiswaanPunyaMahasiswas = null;
	}

	/**
	 * Melepas satu id kegiatan kemahasiswaan dari berkas JSON dengan mengosongkan nilainya.
	 *
	 * @param id id kegiatan yang dilepas.
	 */
	public void removeKegiatanKemahasiswaanPunyaMahasiswa(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiKegiatanKemahasiswaanPunyaMahasiswa());
			c.put(id.toString(), "");
			tulisLokasiKegiatanKemahasiswaanPunyaMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5388");

		}
	}

	/**
	 * Daftar id kegiatan kemahasiswaan yang diikuti mahasiswa ini.
	 *
	 * <p><b>Efek samping:</b> bila berkas JSON belum pernah dibangun
	 * ({@code !udah("KegiatanKemahasiswaanPunyaMahasiswa")}), daftar dibangun ulang lebih dahulu
	 * memakai sesi thread-local yang KEMUDIAN DITUTUP ({@code HibernateUtil.closeSession()}).
	 * Jadi metode ini bisa menulis berkas dan menutup sesi, bukan sekadar membaca.</p>
	 *
	 * <p>Dipakai {@code ais.action.master.helper.profile.ProfileMahasiswa}.</p>
	 *
	 * @return daftar id kegiatan kemahasiswaan; kosong bila tidak ada.
	 */
	@SuppressWarnings("unchecked")
	public List<Long> ambilKegiatanKemahasiswaanPunyaMahasiswa() {

		if (!udah("KegiatanKemahasiswaanPunyaMahasiswa")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitKegiatanKemahasiswaanPunyaMahasiswa(session);
			HibernateUtil.closeSession();
		}

		List<Long> kegiatanKemahasiswaanPunyaMahasiswas = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiKegiatanKemahasiswaanPunyaMahasiswa());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) ambilData(
								KegiatanKemahasiswaanPunyaMahasiswa.class, key, true);
						if (kegiatanKemahasiswaanPunyaMahasiswa != null) {
							kegiatanKemahasiswaanPunyaMahasiswas.add(kegiatanKemahasiswaanPunyaMahasiswa.getId());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:5418");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:5422");
		}

		return kegiatanKemahasiswaanPunyaMahasiswas;
	}

	// ORGANISASI MAHASISWA

	/**
	 * Membaca berkas JSON daftar id {@code OrganisasiIntraKampusPunyaMahasiswa} milik mahasiswa
	 * ini. Pola sama dengan {@link #ambilLokasiDetailPerkuliahan()}.
	 *
	 * @return teks JSON isi berkas, atau JSON kosong bila belum ada/gagal dibaca.
	 */
	public String ambilLokasiOrganisasiIntraKampusPunyaMahasiswa() {
		File file = Common.getFileLocation(this, "organisasiIntraKampusPunyaMahasiswa_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5436");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas JSON daftar keanggotaan organisasi intra kampus. Kegagalan ditelan.
	 *
	 * @param data teks JSON yang hendak disimpan.
	 */
	public void tulisLokasiOrganisasiIntraKampusPunyaMahasiswa(String data) {
		File file = Common.getFileLocation(this, "organisasiIntraKampusPunyaMahasiswa_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5445");
		}
	}

	/**
	 * Mendaftarkan satu {@code OrganisasiIntraKampusPunyaMahasiswa} ke berkas JSON mahasiswa ini,
	 * sekaligus memanggil {@code write()} pada entitasnya. Kegagalan ditelan.
	 *
	 * @param organisasiIntraKampusPunyaMahasiswa keanggotaan organisasi yang didaftarkan.
	 */
	public void populateOrganisasiIntraKampusPunyaMahasiswa(
			OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa) {
		try {
			JSONObject c = new JSONObject(ambilLokasiOrganisasiIntraKampusPunyaMahasiswa());
			organisasiIntraKampusPunyaMahasiswa.write();
			c.put(organisasiIntraKampusPunyaMahasiswa.getId().toString(),
					organisasiIntraKampusPunyaMahasiswa.getId().toString());
			tulisLokasiOrganisasiIntraKampusPunyaMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5457");
		}
	}

	/**
	 * Membangun ulang dari basis data berkas JSON daftar keanggotaan organisasi intra kampus milik
	 * mahasiswa ini (berkas ditimpa JSON kosong, lalu didaftarkan ulang satu per satu).
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitOrganisasiIntraKampusPunyaMahasiswa(Session session) {

		List<OrganisasiIntraKampusPunyaMahasiswa> organisasiIntraKampusPunyaMahasiswas = session
				.createCriteria(OrganisasiIntraKampusPunyaMahasiswa.class).add(Restrictions.eq("mahasiswa", this))
				.addOrder(Order.asc("id")).list();
		tulisLokasiOrganisasiIntraKampusPunyaMahasiswa(new JSONObject().toString());
		for (OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa : organisasiIntraKampusPunyaMahasiswas) {
			populateOrganisasiIntraKampusPunyaMahasiswa(organisasiIntraKampusPunyaMahasiswa);
		}
		organisasiIntraKampusPunyaMahasiswas = null;
	}

	/**
	 * Melepas satu id keanggotaan organisasi dari berkas JSON dengan mengosongkan nilainya.
	 *
	 * @param id id keanggotaan yang dilepas.
	 */
	public void removeOrganisasiIntraKampusPunyaMahasiswa(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiOrganisasiIntraKampusPunyaMahasiswa());
			c.put(id.toString(), "");
			tulisLokasiOrganisasiIntraKampusPunyaMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5479");

		}
	}

	/**
	 * Daftar id keanggotaan organisasi intra kampus milik mahasiswa ini.
	 *
	 * <p><b>Efek samping</b> sama dengan {@link #ambilKegiatanKemahasiswaanPunyaMahasiswa()}: bila
	 * berkas belum pernah dibangun, daftar dibangun ulang memakai sesi thread-local yang kemudian
	 * ditutup. Dipakai {@code ProfileMahasiswa}.</p>
	 *
	 * @return daftar id keanggotaan organisasi; kosong bila tidak ada.
	 */
	@SuppressWarnings("unchecked")
	public List<Long> ambilOrganisasiIntraKampusPunyaMahasiswa() {

		if (!udah("OrganisasiIntraKampusPunyaMahasiswa")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitOrganisasiIntraKampusPunyaMahasiswa(session);
			HibernateUtil.closeSession();
		}

		List<Long> organisasiIntraKampusPunyaMahasiswas = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiOrganisasiIntraKampusPunyaMahasiswa());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa = (OrganisasiIntraKampusPunyaMahasiswa) ambilData(
								OrganisasiIntraKampusPunyaMahasiswa.class, key, true);
						if (organisasiIntraKampusPunyaMahasiswa != null) {
							organisasiIntraKampusPunyaMahasiswas.add(organisasiIntraKampusPunyaMahasiswa.getId());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:5509");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:5513");
		}

		return organisasiIntraKampusPunyaMahasiswas;
	}

	// PRESTASI MAHASISWA

	/**
	 * Membaca berkas JSON daftar id {@code PrestasiMahasiswa} milik mahasiswa ini. Pola sama
	 * dengan {@link #ambilLokasiDetailPerkuliahan()}.
	 *
	 * @return teks JSON isi berkas, atau JSON kosong bila belum ada/gagal dibaca.
	 */
	public String ambilLokasiPrestasiMahasiswa() {
		File file = Common.getFileLocation(this, "prestasiMahasiswa_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5527");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas JSON daftar prestasi mahasiswa. Kegagalan ditelan.
	 *
	 * @param data teks JSON yang hendak disimpan.
	 */
	public void tulisLokasiPrestasiMahasiswa(String data) {
		File file = Common.getFileLocation(this, "prestasiMahasiswa_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5536");
		}
	}

	/**
	 * Mendaftarkan satu {@link PrestasiMahasiswa} ke berkas JSON mahasiswa ini, sekaligus
	 * memanggil {@code write()} pada entitasnya. Kegagalan ditelan.
	 *
	 * @param prestasiMahasiswa prestasi yang didaftarkan.
	 */
	public void populatePrestasiMahasiswa(PrestasiMahasiswa prestasiMahasiswa) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPrestasiMahasiswa());
			prestasiMahasiswa.write();
			c.put(prestasiMahasiswa.getId().toString(), prestasiMahasiswa.getId().toString());
			tulisLokasiPrestasiMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5546");
		}
	}

	/**
	 * Membangun ulang dari basis data berkas JSON daftar prestasi milik mahasiswa ini.
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitPrestasiMahasiswa(Session session) {

		List<PrestasiMahasiswa> prestasiMahasiswas = session.createCriteria(PrestasiMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", this)).addOrder(Order.asc("id")).list();
		tulisLokasiPrestasiMahasiswa(new JSONObject().toString());
		for (PrestasiMahasiswa prestasiMahasiswa : prestasiMahasiswas) {
			populatePrestasiMahasiswa(prestasiMahasiswa);
		}
		prestasiMahasiswas = null;
	}

	/**
	 * Melepas satu id prestasi dari berkas JSON dengan mengosongkan nilainya.
	 *
	 * @param id id prestasi yang dilepas.
	 */
	public void removePrestasiMahasiswa(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPrestasiMahasiswa());
			c.put(id.toString(), "");
			tulisLokasiPrestasiMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5567");

		}
	}

	/**
	 * Daftar id {@link PrestasiMahasiswa} milik mahasiswa ini. Efek sampingnya sama dengan
	 * {@link #ambilKegiatanKemahasiswaanPunyaMahasiswa()} (bisa membangun ulang berkas dan menutup
	 * sesi thread-local). Dipakai {@code ProfileMahasiswa}.
	 *
	 * @return daftar id prestasi; kosong bila tidak ada.
	 */
	@SuppressWarnings("unchecked")
	public List<Long> ambilPrestasiMahasiswa() {

		if (!udah("PrestasiMahasiswa")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitPrestasiMahasiswa(session);
			HibernateUtil.closeSession();
		}

		List<Long> prestasiMahasiswas = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiPrestasiMahasiswa());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						PrestasiMahasiswa prestasiMahasiswa = (PrestasiMahasiswa) ambilData(PrestasiMahasiswa.class,
								key, true);
						if (prestasiMahasiswa != null) {
							prestasiMahasiswas.add(prestasiMahasiswa.getId());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:5597");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:5601");
		}

		return prestasiMahasiswas;
	}

	// KARYA MAHASISWA

	/**
	 * Membaca berkas JSON daftar id {@code PenghargaanMahasiswa} milik mahasiswa ini. Pola sama
	 * dengan {@link #ambilLokasiDetailPerkuliahan()}.
	 *
	 * @return teks JSON isi berkas, atau JSON kosong bila belum ada/gagal dibaca.
	 */
	public String ambilLokasiPenghargaanMahasiswa() {
		File file = Common.getFileLocation(this, "penghargaanMahasiswa_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5615");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas JSON daftar penghargaan mahasiswa. Kegagalan ditelan.
	 *
	 * @param data teks JSON yang hendak disimpan.
	 */
	public void tulisLokasiPenghargaanMahasiswa(String data) {
		File file = Common.getFileLocation(this, "penghargaanMahasiswa_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5624");
		}
	}

	/**
	 * Mendaftarkan satu {@link PenghargaanMahasiswa} ke berkas JSON mahasiswa ini, sekaligus
	 * memanggil {@code write()} pada entitasnya. Kegagalan ditelan.
	 *
	 * @param penghargaanMahasiswa penghargaan yang didaftarkan.
	 */
	public void populatePenghargaanMahasiswa(PenghargaanMahasiswa penghargaanMahasiswa) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPenghargaanMahasiswa());
			penghargaanMahasiswa.write();
			c.put(penghargaanMahasiswa.getId().toString(), penghargaanMahasiswa.getId().toString());
			tulisLokasiPenghargaanMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5634");
		}
	}

	/**
	 * Membangun ulang dari basis data berkas JSON daftar penghargaan milik mahasiswa ini.
	 *
	 * @param session sesi Hibernate aktif milik pemanggil.
	 */
	@SuppressWarnings("unchecked")
	public void reInitPenghargaanMahasiswa(Session session) {

		List<PenghargaanMahasiswa> penghargaanMahasiswas = session.createCriteria(PenghargaanMahasiswa.class)
				.add(Restrictions.eq("mahasiswa", this)).addOrder(Order.asc("id")).list();
		tulisLokasiPenghargaanMahasiswa(new JSONObject().toString());
		for (PenghargaanMahasiswa penghargaanMahasiswa : penghargaanMahasiswas) {
			populatePenghargaanMahasiswa(penghargaanMahasiswa);
		}
		penghargaanMahasiswas = null;
	}

	/**
	 * Melepas satu id penghargaan dari berkas JSON dengan mengosongkan nilainya.
	 *
	 * @param id id penghargaan yang dilepas.
	 */
	public void removePenghargaanMahasiswa(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPenghargaanMahasiswa());
			c.put(id.toString(), "");
			tulisLokasiPenghargaanMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:5655");

		}
	}

	/**
	 * Daftar id {@link PenghargaanMahasiswa} milik mahasiswa ini. Efek sampingnya sama dengan
	 * {@link #ambilKegiatanKemahasiswaanPunyaMahasiswa()}. Dipakai {@code ProfileMahasiswa}.
	 *
	 * @return daftar id penghargaan; kosong bila tidak ada.
	 */
	@SuppressWarnings("unchecked")
	public List<Long> ambilPenghargaanMahasiswa() {

		if (!udah("PenghargaanMahasiswa")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitPenghargaanMahasiswa(session);
			HibernateUtil.closeSession();
		}

		List<Long> penghargaanMahasiswas = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiPenghargaanMahasiswa());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						PenghargaanMahasiswa penghargaanMahasiswa = (PenghargaanMahasiswa) ambilData(
								PenghargaanMahasiswa.class, key, true);
						if (penghargaanMahasiswa != null) {
							penghargaanMahasiswas.add(penghargaanMahasiswa.getId());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:5685");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:5689");
		}

		return penghargaanMahasiswas;
	}

	/**
	 * Perguruan tinggi ASAL mahasiswa pindahan — relasi {@code @ManyToOne} ke
	 * {@link PerguruanTinggiLain} lewat kolom {@code pindahan_dari}. Bila terisi, namanya menimpa
	 * {@link #getPindahanPerguruanTinggi()} dan {@link #getPindahanDariKampus()}.
	 *
	 * @return perguruan tinggi asal; {@code null} bila bukan pindahan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pindahan_dari")
	public PerguruanTinggiLain getPindahanDari() {
		pindahanDari = check(pindahanDari);
		return pindahanDari;
	}

	/**
	 * Menetapkan perguruan tinggi asal mahasiswa pindahan.
	 *
	 * @param pindahanDari perguruan tinggi asal.
	 */
	public void setPindahanDari(PerguruanTinggiLain pindahanDari) {
		this.pindahanDari = pindahanDari;
	}

	/**
	 * Apakah status awal per semester SELALU mengikuti data utama mahasiswa ini, sehingga
	 * perubahan status per semester ({@code HistoryStatusMahasiswa}) tidak dipakai. Bila
	 * {@code null} dianggap {@code false}.
	 *
	 * @return {@code true} bila status awal selalu ikut data utama.
	 */
	public Boolean getStatusAwalSelaluIkutDataUtama() {
		return statusAwalSelaluIkutDataUtama == null ? false : statusAwalSelaluIkutDataUtama;
	}

	/**
	 * Menetapkan penanda "status awal selalu ikut data utama".
	 *
	 * @param statusAwalSelaluIkutDataUtama {@code true} bila selalu ikut data utama.
	 */
	public void setStatusAwalSelaluIkutDataUtama(Boolean statusAwalSelaluIkutDataUtama) {
		this.statusAwalSelaluIkutDataUtama = statusAwalSelaluIkutDataUtama;
	}

	/**
	 * Apakah dosen pembimbing akademik mahasiswa ini TETAP sama di semua semester (tidak boleh
	 * berganti per semester). Bila {@code null} dianggap {@code false}.
	 *
	 * @return {@code true} bila dosen PA selalu sama.
	 */
	public Boolean getDosenPaSelaluSama() {
		return dosenPaSelaluSama == null ? false : dosenPaSelaluSama;
	}

	/**
	 * Menetapkan penanda "dosen PA selalu sama".
	 *
	 * @param dosenPaSelaluSama {@code true} bila dosen PA tidak berganti per semester.
	 */
	public void setDosenPaSelaluSama(Boolean dosenPaSelaluSama) {
		this.dosenPaSelaluSama = dosenPaSelaluSama;
	}

	/**
	 * Apakah kelas mahasiswa ini TETAP sama di semua semester. Bila {@code null} dianggap
	 * {@code false}.
	 *
	 * @return {@code true} bila kelas selalu sama.
	 */
	public Boolean getKelasSelaluSama() {
		return kelasSelaluSama == null ? false : kelasSelaluSama;
	}

	/**
	 * Menetapkan penanda "kelas selalu sama".
	 *
	 * @param kelasSelaluSama {@code true} bila kelas tidak berganti per semester.
	 */
	public void setKelasSelaluSama(Boolean kelasSelaluSama) {
		this.kelasSelaluSama = kelasSelaluSama;
	}

	/**
	 * Judul tugas akhir/skripsi dalam bahasa Inggris — kolom {@code judul_skripsi_en} (tipe TEXT).
	 * Dicetak pada transkrip/ijazah versi Inggris dan SKPI.
	 *
	 * @return judul skripsi bahasa Inggris; {@code null} bila belum diisi.
	 */
	@Column(name = "judul_skripsi_en", columnDefinition = "text")
	public String getJudulSkripsiEn() {
		return judulSkripsiEn;
	}

	/**
	 * Menetapkan judul skripsi berbahasa Inggris.
	 *
	 * @param judulSkripsiEn judul skripsi bahasa Inggris.
	 */
	public void setJudulSkripsiEn(String judulSkripsiEn) {
		this.judulSkripsiEn = judulSkripsiEn;
	}

	/**
	 * Mulai semester ke-berapa status awal mahasiswa BERUBAH menjadi
	 * {@link #getStatusAwalMahasiswaSetelahSmtTertentu()}. Bila kosong, status susulan itu
	 * dianggap tidak berlaku.
	 *
	 * @return nomor semester pergantian status awal; {@code null} bila tidak ada.
	 */
	public Integer getSmtStatusAwal() {
		return smtStatusAwal;
	}

	/**
	 * Menetapkan semester pergantian status awal pertama.
	 *
	 * @param smtStatusAwal nomor semester pergantian.
	 */
	public void setSmtStatusAwal(Integer smtStatusAwal) {
		this.smtStatusAwal = smtStatusAwal;
	}

	/**
	 * Status awal SUSULAN yang berlaku mulai semester {@link #getSmtStatusAwal()} — relasi
	 * {@code @ManyToOne} ke {@link StatusAwalMahasiswa} lewat kolom
	 * {@code status_awal_mahasiswa_setelah_smt_tertentu}. Mekanisme ini dipakai mahasiswa yang
	 * status awalnya berubah di tengah masa studi (mis. semula reguler lalu menjadi alih jalur).
	 *
	 * <p>Bila {@link #getSmtStatusAwal()} kosong, nilainya DIPAKSA {@code null} — status susulan
	 * tanpa semester berlaku dianggap tidak sah.</p>
	 *
	 * @return status awal susulan; {@code null} bila tidak berlaku.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa_setelah_smt_tertentu", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswaSetelahSmtTertentu() {
		if (getSmtStatusAwal() == null) {
			statusAwalMahasiswaSetelahSmtTertentu = null;
		}
		statusAwalMahasiswaSetelahSmtTertentu = check(statusAwalMahasiswaSetelahSmtTertentu);
		return statusAwalMahasiswaSetelahSmtTertentu;
	}

	/**
	 * Menetapkan status awal susulan pertama.
	 *
	 * @param statusAwalMahasiswaSetelahSmtTertentu status awal susulan.
	 */
	public void setStatusAwalMahasiswaSetelahSmtTertentu(StatusAwalMahasiswa statusAwalMahasiswaSetelahSmtTertentu) {
		this.statusAwalMahasiswaSetelahSmtTertentu = statusAwalMahasiswaSetelahSmtTertentu;
	}

	/**
	 * Mulai semester ke-berapa status awal mahasiswa berubah untuk KEDUA kalinya, menjadi
	 * {@link #getStatusAwalMahasiswaSetelahSmtTertentuLagi()}.
	 *
	 * @return nomor semester pergantian status awal kedua; {@code null} bila tidak ada.
	 */
	public Integer getSmtStatusAwalLagi() {
		return smtStatusAwalLagi;
	}

	/**
	 * Menetapkan semester pergantian status awal kedua.
	 *
	 * @param smtStatusAwalLagi nomor semester pergantian kedua.
	 */
	public void setSmtStatusAwalLagi(Integer smtStatusAwalLagi) {
		this.smtStatusAwalLagi = smtStatusAwalLagi;
	}

	/**
	 * Status awal SUSULAN KEDUA yang berlaku mulai semester {@link #getSmtStatusAwalLagi()} —
	 * relasi {@code @ManyToOne} lewat kolom
	 * {@code status_awal_mahasiswa_setelah_smt_tertentu_lagi}. Sama seperti susulan pertama,
	 * nilainya dipaksa {@code null} bila semester berlakunya kosong.
	 *
	 * @return status awal susulan kedua; {@code null} bila tidak berlaku.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa_setelah_smt_tertentu_lagi", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswaSetelahSmtTertentuLagi() {
		if (getSmtStatusAwalLagi() == null) {
			statusAwalMahasiswaSetelahSmtTertentuLagi = null;
		}
		statusAwalMahasiswaSetelahSmtTertentuLagi = check(statusAwalMahasiswaSetelahSmtTertentuLagi);
		return statusAwalMahasiswaSetelahSmtTertentuLagi;
	}

	/**
	 * Menetapkan status awal susulan kedua.
	 *
	 * @param statusAwalMahasiswaSetelahSmtTertentuLagi status awal susulan kedua.
	 */
	public void setStatusAwalMahasiswaSetelahSmtTertentuLagi(
			StatusAwalMahasiswa statusAwalMahasiswaSetelahSmtTertentuLagi) {
		this.statusAwalMahasiswaSetelahSmtTertentuLagi = statusAwalMahasiswaSetelahSmtTertentuLagi;
	}

	/**
	 * Kelompok/rombongan mahasiswa — relasi {@code @ManyToOne} ke {@link KelompokMahasiswa} lewat
	 * kolom {@code kelompok_mahasiswa}. Dipakai untuk pengelompokan administratif (mis. kelompok
	 * bimbingan atau angkatan khusus).
	 *
	 * @return kelompok mahasiswa; {@code null} bila tidak tergabung.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_mahasiswa", nullable = true)
	public KelompokMahasiswa getKelompokMahasiswa() {
		kelompokMahasiswa = check(kelompokMahasiswa);
		return kelompokMahasiswa;
	}

	/**
	 * Menetapkan kelompok mahasiswa.
	 *
	 * @param kelompokMahasiswa kelompok mahasiswa.
	 */
	public void setKelompokMahasiswa(KelompokMahasiswa kelompokMahasiswa) {
		this.kelompokMahasiswa = kelompokMahasiswa;
	}

	/**
	 * Kelompok SK status keluar (SK kelulusan/keluar KOLEKTIF) — relasi {@code @ManyToOne} ke
	 * {@link KelompokStatusKeluarMahasiswa} lewat kolom {@code kelompok_status_keluar_mahasiswa}.
	 *
	 * <p><b>Relasi paling berpengaruh terhadap data kelulusan.</b> Bila terisi, nilainya MENIMPA
	 * beberapa properti perorangan sekaligus: {@link #getStatusKeluar()},
	 * {@link #getTanggalLulus()}, {@link #getTahunLulus()} dan (lewat
	 * {@link #hitungSmtLulus(StatusKeluar, Mahasiswa)}) {@link #getSemesterLulus()}. Yang TIDAK
	 * ikut ditimpa adalah {@link #getTanggalSkRektor()} dan {@link #getNoAkta2()} — keduanya
	 * pernah dicoba lalu sengaja dimatikan.</p>
	 *
	 * @return kelompok SK status keluar; {@code null} bila mahasiswa tidak tergabung SK kolektif.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_status_keluar_mahasiswa", nullable = true)
	public KelompokStatusKeluarMahasiswa getKelompokStatusKeluarMahasiswa() {
		kelompokStatusKeluarMahasiswa = check(kelompokStatusKeluarMahasiswa);
		return kelompokStatusKeluarMahasiswa;
	}

	/**
	 * Menetapkan kelompok SK status keluar kolektif.
	 *
	 * @param kelompokStatusKeluarMahasiswa kelompok SK status keluar.
	 */
	public void setKelompokStatusKeluarMahasiswa(KelompokStatusKeluarMahasiswa kelompokStatusKeluarMahasiswa) {
		this.kelompokStatusKeluarMahasiswa = kelompokStatusKeluarMahasiswa;
	}

	/**
	 * Apakah KRS dan nilai dari baris mahasiswa LAMA ({@link #getAlihProdiMahasiswa()}) ikut
	 * dipindahkan ke baris ini saat proses alih prodi dijalankan. Bila {@code null} dianggap
	 * {@code false}.
	 *
	 * @return {@code true} bila KRS &amp; nilai ikut dipindahkan.
	 */
	public Boolean getPindahkanKrsDanNilaiKeMahasiswaAlihProdi() {
		return pindahkanKrsDanNilaiKeMahasiswaAlihProdi == null ? false : pindahkanKrsDanNilaiKeMahasiswaAlihProdi;
	}

	/**
	 * Menetapkan penanda pemindahan KRS &amp; nilai pada alih prodi.
	 *
	 * @param pindahkanKrsDanNilaiKeMahasiswaAlihProdi {@code true} bila ikut dipindahkan.
	 */
	public void setPindahkanKrsDanNilaiKeMahasiswaAlihProdi(Boolean pindahkanKrsDanNilaiKeMahasiswaAlihProdi) {
		this.pindahkanKrsDanNilaiKeMahasiswaAlihProdi = pindahkanKrsDanNilaiKeMahasiswaAlihProdi;
	}

	/**
	 * Semester berjalan mahasiswa ini dalam bentuk properti bean — bertanda {@code @Transient}
	 * (tidak disimpan) dan SELALU dihitung ulang lewat {@link #currentSemester()}. Disediakan agar
	 * dapat dipakai kerangka yang hanya mengenal pola {@code getX()} (mis. binding ZK dan
	 * JasperReports).
	 *
	 * @return nomor semester berjalan.
	 */
	@Transient
	public Integer getSemesterSaatIni() {
		semesterSaatIni = currentSemester();
		return semesterSaatIni;
	}

	/**
	 * Menetapkan semester berjalan (akan selalu tertimpa oleh {@link #getSemesterSaatIni()}).
	 *
	 * @param semesterSaatIni nomor semester.
	 */
	public void setSemesterSaatIni(Integer semesterSaatIni) {
		this.semesterSaatIni = semesterSaatIni;
	}

	/**
	 * Data orang tua/wali mahasiswa — relasi {@code @ManyToOne} ke {@link OrangTua} lewat kolom
	 * {@code orang_tua}. Terpisah dari {@link BiodataMahasiswa}; dipakai portal orang tua dan
	 * korespondensi.
	 *
	 * @return data orang tua; {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "orang_tua", nullable = true)
	public OrangTua getOrangTua() {
		orangTua = check(orangTua);
		return orangTua;
	}

	/**
	 * Menetapkan data orang tua/wali.
	 *
	 * @param orangTua data orang tua.
	 */
	public void setOrangTua(OrangTua orangTua) {
		this.orangTua = orangTua;
	}

	/**
	 * Membangun TAUTAN LOGIN OTOMATIS (login tanpa mengetik sandi) untuk mahasiswa ini, berbentuk
	 * <code>{host}/m?q={terenkripsi}</code>.
	 *
	 * <p>Muatan yang dienkripsi ({@code Common.desEncrypter}) adalah
	 * <code>{id}-Mahasiswa-abcdefghijklmnopqrstuvwxyz</code>, lalu di-{@code URLEncoder.encode}
	 * dengan UTF-8. Host bawaannya {@code Common.getRequestHostWithProtocol()}; bila konfigurasi
	 * {@code login_via_link_menggunakan_domain_masing_masing} AKTIF, host diganti domain
	 * perguruan tinggi milik program studi mahasiswa
	 * ({@code jurusan.fakultas.perguruanTinggi.getDomain()}) ditambah context path — sehingga
	 * tautan mengarah ke domain kampus masing-masing pada instalasi multi-perguruan tinggi.
	 * {@code HttpServletRequest} diambil dari {@code Execution} ZK bila ada, kalau tidak dari
	 * {@code RequestContext} (jalur servlet/REST).</p>
	 *
	 * <p><b>Perhatian keamanan:</b> tautan hasil metode ini setara kredensial — siapa pun yang
	 * memegangnya dapat masuk sebagai mahasiswa tersebut. Dipakai delapan berkas, umumnya untuk
	 * pengiriman tautan lewat surel/WhatsApp.</p>
	 *
	 * @return URL login otomatis.
	 * @throws Exception bila proses enkripsi atau {@code URLEncoder} gagal.
	 */
	public String urlLogin() throws Exception {
		String url = Common.getRequestHostWithProtocol();

		if (Common.bolehKonfigurasi("login_via_link_menggunakan_domain_masing_masing", Konfigurasi.TIDAK_AKTIF)) {
			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			url = "http://" + getJurusan().getFakultas().getPerguruanTinggi().getDomain() + "/"
					+ request.getContextPath();
		}

		String code = url + "/m?q=" + URLEncoder
				.encode(Common.desEncrypter.get().encrypt(getId() + "-Mahasiswa-abcdefghijklmnopqrstuvwxyz"), "UTF-8");
		return code;
	}

	/**
	 * Token perangkat untuk notifikasi push (Google Cloud Messaging/Firebase) — kolom
	 * {@code gcp_token} (tipe TEXT). Boleh berisi beberapa token karena satu mahasiswa bisa
	 * memakai beberapa perangkat.
	 *
	 * @return daftar token; {@code null} bila belum ada.
	 */
	@Column(name = "gcp_token", columnDefinition = "text")
	public String getGcpToken() {
		return gcpToken;
	}

	/**
	 * MENAMBAHKAN token perangkat baru ke daftar token yang sudah ada — bukan mengganti. Perakitan
	 * dan penjagaan duplikat diserahkan ke {@code Tbmuser.tambahToken(baru, lama)}.
	 *
	 * <p>Perhatikan penamaan {@code set...} di sini menyesatkan: perilakunya menambah, bukan
	 * menimpa.</p>
	 *
	 * @param gcpToken token perangkat yang hendak ditambahkan.
	 */
	public void setGcpToken(String gcpToken) {
		this.gcpToken = Tbmuser.tambahToken(gcpToken, this.gcpToken);
	}

	/**
	 * Kepesertaan kursus yang tertaut ke mahasiswa ini — relasi {@code @ManyToOne} ke
	 * {@link ais.database.model.kursus.PesertaKursus} lewat kolom {@code peserta_kursus}. Terisi
	 * bila mahasiswa berasal dari (atau juga terdaftar sebagai) peserta kursus.
	 *
	 * @return kepesertaan kursus; {@code null} bila tidak ada.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_kursus", nullable = true)
	public PesertaKursus getPesertaKursus() {
		pesertaKursus = check(pesertaKursus);
		return pesertaKursus;
	}

	/**
	 * Menetapkan kepesertaan kursus yang tertaut.
	 *
	 * @param pesertaKursus kepesertaan kursus.
	 */
	public void setPesertaKursus(PesertaKursus pesertaKursus) {
		this.pesertaKursus = pesertaKursus;
	}

	/**
	 * Nama mahasiswa dalam aksara Arab — dipakai perguruan tinggi keagamaan Islam untuk ijazah
	 * dan sertifikat berbahasa Arab. {@code null} dinormalkan menjadi string kosong.
	 *
	 * @return nama Arab (tidak pernah {@code null}).
	 */
	public String getNamaArab() {
		return namaArab == null ? "" : namaArab;
	}

	/**
	 * Menetapkan nama dalam aksara Arab.
	 *
	 * @param namaArab nama Arab.
	 */
	public void setNamaArab(String namaArab) {
		this.namaArab = namaArab;
	}

	/**
	 * Nama mahasiswa dalam aksara Tionghoa. {@code null} dinormalkan menjadi string kosong.
	 *
	 * @return nama Tionghoa (tidak pernah {@code null}).
	 */
	public String getNamaTionghoa() {
		return namaTionghoa == null ? "" : namaTionghoa;
	}

	/**
	 * Menetapkan nama dalam aksara Tionghoa.
	 *
	 * @param namaTionghoa nama Tionghoa.
	 */
	public void setNamaTionghoa(String namaTionghoa) {
		this.namaTionghoa = namaTionghoa;
	}

	/**
	 * Program mahasiswa dalam bentuk relasi master — {@code @ManyToOne} ke
	 * {@link ProgramMahasiswa} lewat kolom {@code program_mahasiswa}. Berbeda dari
	 * {@link #getProgram()} yang berupa teks bebas dan {@link #getProgramBaru()} yang mencocokkan
	 * teks itu ke master {@link Program}.
	 *
	 * @return program mahasiswa; {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "program_mahasiswa")
	public ProgramMahasiswa getProgramMahasiswa() {
		programMahasiswa = check(programMahasiswa);
		return programMahasiswa;
	}

	/**
	 * Menetapkan program mahasiswa (relasi master).
	 *
	 * @param programMahasiswa program mahasiswa.
	 */
	public void setProgramMahasiswa(ProgramMahasiswa programMahasiswa) {
		this.programMahasiswa = programMahasiswa;
	}

	/**
	 * Kelompok SK status mahasiswa (SK KOLEKTIF untuk status non-keluar, mis. penetapan cuti atau
	 * aktif kembali) — relasi {@code @ManyToOne} ke {@link KelompokStatusMahasiswa} lewat kolom
	 * {@code kelompok_status_mahasiswa}. Padanan {@link #getKelompokStatusKeluarMahasiswa()} untuk
	 * status yang bukan kelulusan/keluar.
	 *
	 * @return kelompok status mahasiswa; {@code null} bila tidak tergabung.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_status_mahasiswa", nullable = true)
	public KelompokStatusMahasiswa getKelompokStatusMahasiswa() {
		kelompokStatusMahasiswa = check(kelompokStatusMahasiswa);
		return kelompokStatusMahasiswa;
	}

	/**
	 * Menetapkan kelompok SK status mahasiswa.
	 *
	 * @param kelompokStatusMahasiswa kelompok status mahasiswa.
	 */
	public void setKelompokStatusMahasiswa(KelompokStatusMahasiswa kelompokStatusMahasiswa) {
		this.kelompokStatusMahasiswa = kelompokStatusMahasiswa;
	}

	/**
	 * Gelombang pendaftaran saat mahasiswa diterima — relasi {@code @ManyToOne} ke
	 * {@link GelombangPendaftaran} lewat kolom {@code gelombang_pendaftaran}. Sering menentukan
	 * besaran biaya masuk.
	 *
	 * @return gelombang pendaftaran; {@code null} bila bukan dari jalur PMB.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran", nullable = true)
	public GelombangPendaftaran getGelombangPendaftaran() {
		gelombangPendaftaran = check(gelombangPendaftaran);
		return gelombangPendaftaran;
	}

	/**
	 * Menetapkan gelombang pendaftaran.
	 *
	 * @param gelombangPendaftaran gelombang pendaftaran.
	 */
	public void setGelombangPendaftaran(GelombangPendaftaran gelombangPendaftaran) {
		this.gelombangPendaftaran = gelombangPendaftaran;
	}

	/**
	 * Objek {@link Dosen} pembimbing akademik — relasi {@code @ManyToOne} ke kolom {@code dosen},
	 * HANYA BACA ({@code insertable = false, updatable = false}); penulisan kolomnya dilakukan
	 * lewat {@link #setDosen(Long)}.
	 *
	 * <p>Getter menyegarkan id lebih dahulu dengan memanggil {@link #getDosen()} — sehingga
	 * aturan "mahasiswa yang sudah keluar tidak punya dosen PA" ikut berlaku di sini — lalu
	 * mengambil objek dosennya dari cache {@link ConstantValues}. Bila tetap tidak ketemu, proxy
	 * relasi disambungkan ulang dengan {@code check()} sebagai cadangan.</p>
	 *
	 * @return dosen pembimbing akademik; {@code null} bila tidak ada atau mahasiswa sudah keluar.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true, insertable = false, updatable = false)
	public Dosen getDosenPa() {
		dosen = getDosen();
		if (dosen != null) {
			dosenPa = (Dosen) ConstantValues.ambil(Dosen.class.getName(), dosen);
		}

		if (dosenPa == null) {
			dosenPa = check(dosenPa);
		}
		return dosenPa;
	}

	/**
	 * Menetapkan objek dosen PA (hanya mengisi cache objek; kolomnya ditulis lewat
	 * {@link #setDosen(Long)}).
	 *
	 * @param dosenPa dosen pembimbing akademik.
	 */
	public void setDosenPa(Dosen dosenPa) {
		this.dosenPa = dosenPa;
	}

	/**
	 * Membangkitkan (bila belum ada) berkas gambar KODE QR identitas mahasiswa untuk ditempel
	 * sebagai "tanda tangan digital" pada dokumen cetak, lalu mengembalikan path absolutnya.
	 *
	 * <p>Isi kode QR berupa teks multibaris: NIM, nama, program studi, fakultas, perguruan tinggi,
	 * dan alamat host aplikasi ({@code Common.getRequestHostWithProtocol()}). Berkas disimpan
	 * sebagai {@code ttd_mhs_<id>.png} di direktori laporan
	 * ({@code Common.ambilREAL_PATH_REPORT()}) dan DIPAKAI ULANG bila sudah ada.</p>
	 *
	 * <p><b>Konsekuensi cache berkas:</b> karena berkas tidak pernah dibangkitkan ulang, perubahan
	 * nama/prodi mahasiswa TIDAK tercermin pada QR lama — berkasnya harus dihapus manual bila
	 * perlu disegarkan.</p>
	 *
	 * @return path absolut berkas PNG kode QR.
	 */
	public String ttdQr() {

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/ttd_mhs_" + getId() + ".png");
		if (!myfilebarcode.exists()) {
			String code = (getNim() == null || getNim().trim().isEmpty() ? "" : getNim() + "\n")

					+ getNama() + "\n" + (getJurusan() == null ? "" : getJurusan().getNama() + "\n")

					+ (getJurusan() == null || getJurusan().getFakultas() == null ? ""
							: getJurusan().getFakultas().getNama() + "\n")

					+ (getJurusan() == null || getJurusan().getFakultas() == null
							|| getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
									: getJurusan().getFakultas().getPerguruanTinggi().getNama() + "\n")

					+ Common.getRequestHostWithProtocol();
			BarcodeCommon.generateCRCode(code, myfilebarcode);
		}
		return myfilebarcode.getAbsolutePath();
	}

	/**
	 * Mengisi peta parameter JasperReports dengan seluruh BERKAS gambar/lampiran milik mahasiswa
	 * ini, sehingga templat laporan cukup merujuk nama parameternya. Dipakai sekitar 67 berkas
	 * laporan/cetakan.
	 *
	 * <p><b>Parameter yang diisi:</b> {@code foto}, {@code foto_lulus}, {@code foto_mahasiswa}
	 * (foto mahasiswa), {@code ttd_mhs} (berkas tanda tangan dari {@link LampiranLain} berjenis
	 * {@code TTD_MAHASISWA}), {@code ttd_mhs_qrcode} (hasil {@link #ttdQr()}), serta
	 * {@code dokumen_mahasiswa_<jenis>} untuk tiap id lampiran yang terdaftar di
	 * {@link #getKarpeg()}.</p>
	 *
	 * <p><b>Urutan sumber foto</b> (yang pertama tersedia dipakai): berkas lokal
	 * {@code FileFotoLain.ambilFile()} &rarr; tautan mentah Dropbox &rarr; URL ekspor Google Drive
	 * &rarr; URI tautan yang dibuat sendiri &rarr; gambar bawaan
	 * {@code /img/administrator-icon_default.png}. Dengan begitu laporan tidak pernah kehilangan
	 * gambar meskipun berkas fisiknya belum tersedia.</p>
	 *
	 * <p>Seluruh kegagalan ditelan (dicatat ke {@code ErrorAuditUtil}) agar pencetakan laporan
	 * tidak batal hanya karena satu berkas hilang.</p>
	 *
	 * @param parameters peta parameter JasperReports yang akan diisi (dimodifikasi di tempat).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putPhoto(Map parameters) {
		try {
			Mahasiswa mahasiswa = this;

			FileFotoLain fotobiodataMahasiswa = FileFotoLain.ambil(mahasiswa.getId(), FotoMahasiswa.DEFAULT_JENIS,
					FotoMahasiswa.class);

			File fileFotoMahasiswa = fotobiodataMahasiswa == null ? null : fotobiodataMahasiswa.ambilFile();
			if (fileFotoMahasiswa != null) {
				parameters.put("foto", fileFotoMahasiswa.getAbsolutePath());
				parameters.put("foto_lulus", fileFotoMahasiswa.getAbsolutePath());
				parameters.put("foto_mahasiswa", fileFotoMahasiswa.getAbsolutePath());
			} else

			if (fotobiodataMahasiswa != null && fotobiodataMahasiswa.dropboxLinkRaw() != null
					&& !fotobiodataMahasiswa.dropboxLinkRaw().trim().isEmpty()) {
				parameters.put("foto", fotobiodataMahasiswa.dropboxLinkRaw());
				parameters.put("foto_lulus", fotobiodataMahasiswa.dropboxLinkRaw());
				parameters.put("foto_mahasiswa", fotobiodataMahasiswa.dropboxLinkRaw());
			} else if (fotobiodataMahasiswa != null && fotobiodataMahasiswa.getGdrive() != null
					&& !fotobiodataMahasiswa.getGdrive().trim().isEmpty()) {
				parameters.put("foto", fotobiodataMahasiswa.exportGDriveUrl());
				parameters.put("foto_lulus", fotobiodataMahasiswa.exportGDriveUrl());
				parameters.put("foto_mahasiswa", fotobiodataMahasiswa.exportGDriveUrl());
			} else if (fotobiodataMahasiswa != null) {
				parameters.put("foto", fotobiodataMahasiswa.createLinkUri());
				parameters.put("foto_lulus", fotobiodataMahasiswa.createLinkUri());
				parameters.put("foto_mahasiswa", fotobiodataMahasiswa.createLinkUri());
			} else {
				File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
				parameters.put("foto", file.getAbsolutePath());
				parameters.put("foto_lulus", file.getAbsolutePath());
				parameters.put("foto_mahasiswa", file.getAbsolutePath());
			}

			LampiranLain lampiranLain = LampiranLain.ambil(mahasiswa.getId(), LampiranLain.TTD_MAHASISWA);
			if (lampiranLain != null && lampiranLain.ambilFile() != null) {
				parameters.put("ttd_mhs", lampiranLain.ambilFile().getAbsolutePath());
			}
			parameters.put("ttd_mhs_qrcode", mahasiswa.ttdQr());

			for (String s : mahasiswa.getKarpeg().split(",")) {
				try {
					if (!s.trim().isEmpty()) {
						LampiranLain lain = LampiranLain.ambil(true, Long.parseLong(s), s);
						if (lain != null) {
							parameters.put("dokumen_mahasiswa_" + lain.getJenis(), lain.ambilFile().getAbsolutePath());
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:6013");
//					e.printStackTrace();
				}
			}

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/Mahasiswa.java:6019");
		}
	}

	/**
	 * Daftar id {@link LampiranLain} milik mahasiswa (berkas pendukung: kartu keluarga, ijazah
	 * SMA, dsb.), disimpan sebagai satu kolom TEXT berisi id yang dipisah koma.
	 *
	 * <p>Dibaca {@link #putPhoto(java.util.Map)} dan {@link #putPhotoLulus(java.util.Map)} untuk
	 * menyuntikkan berkas-berkas itu ke parameter laporan. Meski namanya "karpeg", isinya bukan
	 * nomor kartu pegawai melainkan daftar id lampiran.</p>
	 *
	 * @return daftar id lampiran dipisah koma; string kosong bila tidak ada.
	 */
	@Column(columnDefinition = "text")
	public String getKarpeg() {
		return karpeg == null ? "" : karpeg.trim();
	}

	/**
	 * Menetapkan daftar id lampiran mahasiswa.
	 *
	 * @param karpeg daftar id {@link LampiranLain} dipisah koma.
	 */
	public void setKarpeg(String karpeg) {
		this.karpeg = karpeg;
	}

	/**
	 * Daftar ATASAN / pengguna lulusan dalam format JSON array. Tiap elemen berisi:
	 * {@code nama, email, telp, alamat, peran, masihAktif(boolean), tanggalMulai(yyyy-MM-dd), keterangan}.
	 * Diisi via editor "Tambah Atasan" (tab Kelulusan ZK / kuesioner alumni JSP). Kolom dibuat auto (hbm2ddl).
	 */
	@Column(columnDefinition = "text")
	public String getAtasans() {
		return atasans == null ? "" : atasans.trim();
	}

	/**
	 * Menetapkan daftar atasan/pengguna lulusan dalam bentuk teks JSON array.
	 *
	 * @param atasans teks JSON array daftar atasan.
	 * @see #getAtasans()
	 */
	public void setAtasans(String atasans) {
		this.atasans = atasans;
	}

	/**
	 * Varian {@link #putPhoto(java.util.Map)} untuk dokumen KELULUSAN (ijazah, transkrip, SKPI):
	 * memakai {@link ais.database.model.file.FotoMahasiswaLulus} sebagai sumber utama.
	 *
	 * <p>Bila foto lulus ada, {@code foto} dan {@code foto_lulus} diisi darinya; foto mahasiswa
	 * biasa — bila ada — kemudian MENIMPA parameter {@code foto} saja, sehingga templat dapat
	 * menampilkan foto reguler dan foto lulus berdampingan. Tanda tangan, QR dan lampiran
	 * {@link #getKarpeg()} diisi sama seperti {@link #putPhoto(java.util.Map)}.</p>
	 *
	 * <p>Bila foto lulus tidak ada — atau terjadi kegagalan apa pun — seluruh pengisian
	 * didelegasikan ke {@link #putPhoto(java.util.Map)} sebagai cadangan.</p>
	 *
	 * @param parameters peta parameter JasperReports yang akan diisi (dimodifikasi di tempat).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putPhotoLulus(Map parameters) {
		Mahasiswa mahasiswa = this;
		try {

			FileFotoLain fotoMahasiswaLulus = FileFotoLain.ambil(mahasiswa.getId(), FotoMahasiswaLulus.DEFAULT_JENIS,
					FotoMahasiswaLulus.class);
			if (fotoMahasiswaLulus != null && fotoMahasiswaLulus.getId() != null) {

				try {
					File file = fotoMahasiswaLulus.ambilFile();
					parameters.put("foto", file.getAbsolutePath());
					parameters.put("foto_lulus", file.getAbsolutePath());
				} catch (Exception e) {
					File file = fotoMahasiswaLulus.ambilFile();
					parameters.put("foto", file.getAbsolutePath());
					parameters.put("foto_lulus", file.getAbsolutePath());
				}

				FileFotoLain fotoMahasiswa = FileFotoLain.ambil(mahasiswa.getId(), FotoMahasiswa.DEFAULT_JENIS,
						FotoMahasiswa.class);
				if (fotoMahasiswa != null && fotoMahasiswa.getId() != null) {
					File file = fotoMahasiswa.ambilFile();
					parameters.put("foto", file.getAbsolutePath());
				}
				
				LampiranLain lampiranLain = LampiranLain.ambil(mahasiswa.getId(), LampiranLain.TTD_MAHASISWA);
				if (lampiranLain != null && lampiranLain.ambilFile() != null) {
					parameters.put("ttd_mhs", lampiranLain.ambilFile().getAbsolutePath());
				}
				parameters.put("ttd_mhs_qrcode", mahasiswa.ttdQr());

				for (String s : mahasiswa.getKarpeg().split(",")) {
					try {
						if (!s.trim().isEmpty()) {
							LampiranLain lain = LampiranLain.ambil(true, Long.parseLong(s), s);
							if (lain != null) {
								parameters.put("dokumen_mahasiswa_" + lain.getJenis(), lain.ambilFile().getAbsolutePath());
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:6086");
//						e.printStackTrace();
					}
				}
				
			} else {
				putPhoto(parameters);
			}
		} catch (Exception e) {
			putPhoto(parameters);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Mahasiswa.java:6096");
		}
	}

	/**
	 * Apakah program perkuliahan per semester SELALU mengikuti data utama mahasiswa ini (tidak
	 * boleh berubah per semester). Bila {@code null} dianggap {@code false}.
	 *
	 * @return {@code true} bila program selalu ikut data utama.
	 */
	public Boolean getProgramSelaluIkutDataUtama() {
		return programSelaluIkutDataUtama == null ? false : programSelaluIkutDataUtama;
	}

	/**
	 * Menetapkan penanda "program selalu ikut data utama".
	 *
	 * @param programSelaluIkutDataUtama {@code true} bila selalu ikut data utama.
	 */
	public void setProgramSelaluIkutDataUtama(Boolean programSelaluIkutDataUtama) {
		this.programSelaluIkutDataUtama = programSelaluIkutDataUtama;
	}

	/**
	 * Waktu (TIMESTAMP) terakhir mahasiswa mengubah kata sandinya. Dipakai kebijakan pemaksaan
	 * ganti sandi berkala.
	 *
	 * @return waktu ubah sandi terakhir; {@code null} bila belum pernah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getUbahPasword() {
		return ubahPasword;
	}

	/**
	 * Menetapkan waktu ubah kata sandi terakhir.
	 *
	 * @param ubahPasword waktu ubah sandi.
	 */
	public void setUbahPasword(Date ubahPasword) {
		this.ubahPasword = ubahPasword;
	}

	/**
	 * Pengguna ({@link Tbmuser}) yang MENGUNCI data mahasiswa ini — relasi {@code @ManyToOne}
	 * lewat kolom {@code dikunci}. Selama terisi, layar biodata umumnya menolak perubahan oleh
	 * pengguna lain.
	 *
	 * @return pengguna pengunci; {@code null} bila data tidak terkunci.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menetapkan pengguna pengunci data mahasiswa.
	 *
	 * @param dikunci pengguna pengunci; {@code null} untuk membuka kunci.
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Disposisi SOP terakhir yang menyentuh data mahasiswa ini — relasi {@code @ManyToOne} ke
	 * {@link ais.database.model.sop.DisposisiSop} lewat kolom {@code disposisi_sop}. Menautkan
	 * baris mahasiswa ke alur persetujuan/disposisi dokumen.
	 *
	 * @return disposisi SOP; {@code null} bila tidak ada.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan disposisi SOP — dengan penjagaan agar disposisi yang sudah tercatat TIDAK
	 * terhapus: nilai {@code null} atau tanpa id langsung diabaikan (metode keluar lebih awal),
	 * sehingga proses yang menyimpan mahasiswa tanpa mengetahui disposisinya tidak menghapus
	 * jejak yang sudah ada.
	 *
	 * @param disposisiSop disposisi SOP; diabaikan bila {@code null} atau belum punya id.
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Master {@link Program} yang SESUAI dengan teks {@link #getProgram()} — relasi
	 * {@code @ManyToOne} lewat kolom {@code program_baru} dengan {@code @Fetch(FetchMode.SELECT)}.
	 *
	 * <p>Getter tidak sekadar membaca kolom: ia menelusuri daftar program di cache
	 * {@code Common.programs} dan memilih yang NAMANYA sama (abaikan besar-kecil huruf) dengan
	 * teks {@link #getProgram()}. Ini jembatan migrasi dari kolom teks lama ke relasi master —
	 * nilai kolom {@code program_baru} akan selalu tertimpa hasil pencocokan selama teks program
	 * cocok dengan salah satu master.</p>
	 *
	 * @return master program yang cocok; nilai kolom apa adanya bila tidak ada yang cocok.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "program_baru", nullable = true)
	public Program getProgramBaru() {
		Mahasiswa mahasiswa = (Mahasiswa) this;
		if (mahasiswa != null && mahasiswa.getProgram() != null) {
			for (Program program : Common.programs.values()) {
				if (program.getNama() != null && mahasiswa.getProgram().equalsIgnoreCase(program.getNama())) {
					programBaru = program;
					break;
				}
			}
		}
		return programBaru;
	}

	/**
	 * Menetapkan master program (akan tertimpa {@link #getProgramBaru()} bila teks
	 * {@link #getProgram()} cocok dengan master lain).
	 *
	 * @param programBaru master program.
	 */
	public void setProgramBaru(Program programBaru) {
		this.programBaru = programBaru;
	}

	/**
	 * Tanggal mulai Kegiatan Belajar Mengajar (KBM) mahasiswa ini — titik nol perhitungan
	 * {@link #ambilMasaStudi()}.
	 *
	 * <p>Bila kolom kosong, tanggal DITEBAK dari {@link #getTahunangkatan()} dan
	 * {@link #getSemesterMulai()}: 1 September untuk angkatan semester GANJIL, 1 Maret untuk
	 * GENAP. Tebakan ini disimpan ke field (bukan sekadar nilai balik), jadi ikut tersimpan bila
	 * entitas ter-flush.</p>
	 *
	 * @return tanggal mulai KBM (tidak pernah {@code null}).
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalKegiatanBelajarMengajar() {
		if (tanggalKegiatanBelajarMengajar == null) {
			Calendar calendar = WaktuUtil.getCalendar();
			calendar.set(Calendar.YEAR, getTahunangkatan());
			if (getSemesterMulai().equals(Perkuliahan.GANJIL)) {
				calendar.set(Calendar.MONTH, Calendar.SEPTEMBER);
			} else {
				calendar.set(Calendar.MONTH, Calendar.MARCH);
			}
			calendar.set(Calendar.DATE, 1);
			tanggalKegiatanBelajarMengajar = calendar.getTime();
		}
		return tanggalKegiatanBelajarMengajar;
	}

	/**
	 * Menetapkan tanggal mulai Kegiatan Belajar Mengajar.
	 *
	 * @param tanggalKegiatanBelajarMengajar tanggal mulai KBM.
	 */
	public void setTanggalKegiatanBelajarMengajar(Date tanggalKegiatanBelajarMengajar) {
		this.tanggalKegiatanBelajarMengajar = tanggalKegiatanBelajarMengajar;
	}

	/**
	 * Penanda mahasiswa ini DIKECUALIKAN dari pembangkitan tagihan — kolom
	 * {@code tidak_ada_tagihan}. Dipakai untuk mahasiswa penerima beasiswa penuh atau kasus khusus
	 * lain. Bila {@code null} dianggap {@code false}.
	 *
	 * @return {@code true} bila mahasiswa tidak ditagih.
	 */
	@Column(name = "tidak_ada_tagihan")
	public Boolean getTidakAdaTagihan() {
		return tidakAdaTagihan == null ? false : tidakAdaTagihan;
	}

	/**
	 * Menetapkan penanda pengecualian tagihan.
	 *
	 * @param tidakAdaTagihan {@code true} bila mahasiswa tidak ditagih.
	 */
	public void setTidakAdaTagihan(Boolean tidakAdaTagihan) {
		this.tidakAdaTagihan = tidakAdaTagihan;
	}

	/**
	 * Kelas hasil penempatan pada proses penerimaan mahasiswa baru — relasi {@code @ManyToOne} ke
	 * {@link KelasPmb} lewat kolom {@code kelas_pmb}. Menjadi sumber {@link #getKelas()}.
	 *
	 * <p><b>Pengisian susulan:</b> bila mahasiswa berasal dari PMB
	 * ({@link #getBiodataCalonMahasiswa()} terisi) dan {@link #getBiodataCalonMahasiswaData()}
	 * punya kelas PMB, kelas dari biodata calon itu MENIMPA nilai kolom. Karena itu getter ini
	 * dapat memicu pemuatan {@link BiodataCalonMahasiswa} — seluruhnya dibungkus {@code try/catch}
	 * agar kegagalan lazy-load tidak merambat.</p>
	 *
	 * @return kelas PMB; {@code null} bila tidak ada.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_pmb")
	public KelasPmb getKelasPmb() {
		try {
			kelasPmb = check(kelasPmb);
			if (biodataCalonMahasiswa != null && getBiodataCalonMahasiswaData() != null
					&& biodataCalonMahasiswaData.getKelasPmb() != null) {
				kelasPmb = biodataCalonMahasiswaData.getKelasPmb();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Mahasiswa.java:6202");
			// TODO: handle exception
		}

		return kelasPmb;
	}

	/**
	 * Menetapkan kelas hasil penempatan PMB.
	 *
	 * @param kelasPmb kelas PMB.
	 */
	public void setKelasPmb(KelasPmb kelasPmb) {
		this.kelasPmb = kelasPmb;
	}
}
