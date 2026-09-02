package ais.database.model;

// Generated Apr 23, 2010 12:45:00 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.Criteria;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.KegiatanHelper;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.CommonPMB;
import ais.common.ConstantValues;
import ais.common.RequestContext;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DisposisiSop;

/**
 * Entity <b>biodata calon mahasiswa</b> — satu baris = satu <i>pendaftar</i> pada modul PMB
 * (Penerimaan Mahasiswa Baru), yaitu orang yang sudah mengisi formulir pendaftaran tetapi
 * BELUM tentu menjadi {@link Mahasiswa} resmi. Dipetakan ke tabel {@code public.biodata_calon_mahasiswa},
 * diaudit Hibernate Envers ({@code @Audited}), dan memakai {@code dynamicInsert/dynamicUpdate}
 * sehingga hanya kolom yang benar-benar berubah yang ikut di-INSERT/UPDATE (penting: tabel ini
 * sangat lebar, ratusan kolom).
 *
 * <h3>Posisi dalam alur PMB</h3>
 * Tahapan yang benar-benar terbaca dari kode (bukan asumsi):
 * <ol>
 * <li><b>Pendaftaran.</b> Baris dibuat lewat formulir PMB
 * ({@code ais.action.master.pmb.FormBiodataCalonMahasiswaAction},
 * {@code BiodataCalonMahasiswaAction}) atau lewat unggahan berkas massal
 * ({@link UploadBiodataCalonMahasiswa}). Pendaftar memilih {@link GelombangPendaftaran},
 * {@link Paket}, dan sampai lima pilihan program studi ({@code prodi1}..{@code prodi5}).
 * {@code noRegistrasi} (juga terbaca lewat {@link #getKode()}) adalah identitas alaminya.</li>
 * <li><b>Pembayaran biaya pendaftaran.</b> {@link #chekPembayaranRegistrasi()} membuat/menyambung
 * {@link Kegiatan} bertipe {@code ConstantUtil#PENDAFTARAN_CALON_MAHASISWA}.</li>
 * <li><b>Seleksi.</b> Nilai/jawaban seleksi TIDAK disimpan sebagai kolom terpisah, melainkan
 * diserialisasi ke dalam {@code parameterTambahan} (lihat {@link #ambilDataParameterTambahan()});
 * skornya dihitung ulang setiap kali dibaca oleh {@link #getTotalSkor()} dan
 * {@link #getRincianSkor()}. Ujian tulis dicatat lewat {@code hasilUjianMahasiswa} milik
 * {@link VOMahasiswa}.</li>
 * <li><b>Penetapan kelulusan.</b> Operator mengisi {@link #getProdiLulus()} (prodi tempat pendaftar
 * diterima) dan {@link #getStatusLulus()} ({@link #LULUS}/{@link #TIDAK_LULUS}). Kebalikannya:
 * {@code ditolak} dan {@code mundur} mematikan status kelulusan (lihat {@link #getStatusLulus()}
 * dan {@link #getProdiLulus()} yang mengembalikan 0/{@code null} bila salah satunya aktif).</li>
 * <li><b>Pembangkitan NIM &amp; konversi menjadi Mahasiswa.</b> {@code NimGenerator} per institusi
 * ({@code ais.action.master.pmb.nim}) membangkitkan NIM, lalu
 * {@code ais.common.CommonPMB#saveMahasiswa(...)} membuat/memperbarui {@link Mahasiswa} +
 * {@link BiodataMahasiswa}. Lihat {@link #getNimGenerated()} dan
 * {@link #getGenerateNimOtomatis()}.</li>
 * <li><b>Daftar ulang.</b> {@link #chekPembayaranDaftarUlang()} membuat/menyambung {@link Kegiatan}
 * bertipe {@code ConstantUtil#PENDAFTARAN_ULANG_MAHASISWA_BARU}.</li>
 * </ol>
 *
 * <h3>Hubungan dengan {@link Mahasiswa} / {@link BiodataMahasiswa}: disalin DAN ditautkan</h3>
 * Keduanya terjadi, jadi jangan menganggap salah satu saja:
 * <ul>
 * <li><b>Disalin.</b> {@code CommonPMB.saveMahasiswa} menyalin nilai (nama, alamat, email, kelamin,
 * tempat/tanggal lahir, jenjang, prodi lulus, kewarganegaraan, data pindahan, program, jenis
 * seleksi, dst.) ke {@link Mahasiswa}, lalu {@code CommonPMB.saveBiodataMahasiswa} menyalin sisa
 * biodata ke {@link BiodataMahasiswa}. Perubahan biodata calon SETELAH konversi tidak otomatis
 * merambat ke sana.</li>
 * <li><b>Ditautkan dua arah.</b> {@code calonMahasiswa.setMahasiswa(mahasiswa)} (kolom
 * {@code mahasiswa} di sini) dan {@code mahasiswa.setBiodataCalonMahasiswa(calon.getId())} (kolom
 * {@code biodata_calon_mahasiswa_long} di sana, UNIQUE). Lihat
 * {@link Mahasiswa#getBiodataCalonMahasiswaData()}.</li>
 * <li><b>Sesudah tertaut, sebagian getter di sini berbalik menjadi "cermin" mahasiswa resmi</b>:
 * {@link #getNim()}, {@link #getNama()}, {@link #getProdiLulus()}, dan {@link #getTanggalMasuk()}
 * membaca ulang dari {@link #getMahasiswa()} sehingga nilai lokal bisa tertimpa.</li>
 * </ul>
 * Jalur terpisah: {@code mahasiswaAlumni} ({@link #getMahasiswaAlumni()}) dipakai bila pendaftar
 * adalah alumni kampus yang sama (mis. lanjut dari D3 ke S1). Puluhan getter biodata di kelas ini
 * memakai alumni tersebut sebagai <i>fallback</i> ketika field lokalnya masih kosong, supaya
 * pendaftar tidak perlu mengetik ulang data yang sudah ada.
 *
 * <h3>Peringatan penting: getter di kelas ini TIDAK murni</h3>
 * Sangat banyak getter di sini yang (a) memutasi field-nya sendiri, (b) menarik nilai dari entity
 * lain, bahkan (c) membuka session Hibernate dan menulis baris master baru
 * ({@link #getPropinsiCalon()} lewat {@code findOrCreatePropinsi}). Karena getter-getter itu
 * SEKALIGUS properti persistence Hibernate, nilai hasil turunan tersebut <b>ikut ditulis ke kolom
 * database saat flush</b>. Itu memang disengaja (mekanisme "perbaikan data lama otomatis saat
 * dibaca"), tetapi berarti: memanggil getter bisa mengubah data, dan getter dipanggil sangat sering
 * (setiap baris grid, setiap dirty-check). Beberapa getter karena itu diberi guard/cache — lihat
 * {@code paketDivalidasiUntukGelombangId} pada {@link #terapkanKonsistensiPaketGelombang()}.
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 * <li><b>Audit &amp; identitas</b>: {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #getId()}, {@link #getKode()},
 * {@link #toString()}, {@link #compareTo(GeneralValueObject)}.</li>
 * <li><b>Biodata pribadi</b>: nama, tempat/tanggal lahir, jenis kelamin, agama, kewarganegaraan,
 * alamat + wilayah (kelurahan/kecamatan/kota/propinsi), kontak (hp, telepon, email), fisik
 * (tinggi/berat/golongan darah), minat/organisasi/bahasa.</li>
 * <li><b>Data orang tua/wali</b>: {@code namaAyah}, {@code namaIbu}, {@code namaWali} beserta
 * pendidikan, pekerjaan, pendapatan, dan alamat masing-masing.</li>
 * <li><b>Sekolah asal</b>: {@code asalSd}/{@code asalSmp}/{@code asalSma},
 * {@link #getNamaSekolahAsal()}, {@link #getJenisSekolah()}, {@link #getJurusanSekolah()},
 * akreditasi, wilayah sekolah, {@code tahunKelulusan}.</li>
 * <li><b>Jalur pendaftaran &amp; seleksi</b>: {@link #getGelombangPendaftaran()},
 * {@link #getPaket()}, {@link #getJenisSeleksi()}, {@link #getKelompokJenisSeleksi()},
 * {@link #getProdi1()}..{@link #getProdi5()}, {@link #getKelompokCalonMahasiswa()},
 * {@link #getAfiliasiCalonMahasiswa()}, {@link #getStatusAwalMahasiswa()}.</li>
 * <li><b>Hasil seleksi &amp; konversi</b>: {@link #getStatusLulus()}, {@link #getProdiLulus()},
 * {@link #getDitolak()}, {@link #getMundur()}, {@link #getNimGenerated()}, {@link #getNim()},
 * {@link #getMahasiswa()}, {@link #getTanggalDiterima()}, {@link #getTanggalMasuk()}.</li>
 * <li><b>Pembayaran</b>: {@link #chekPembayaranRegistrasi()},
 * {@link #chekPembayaranDaftarUlang()}, {@link #getStatusPembayaran()},
 * {@link #getTanggalPembayaranRegistrasi()}, {@link #getTanggalPembayaranDaftarUlang()}.</li>
 * <li><b>Parameter tambahan &amp; skor</b>: {@link #getParameterTambahan()},
 * {@link #ambilDataParameterTambahan()}, {@link #populateParameterTambahan(List)},
 * {@link #getTotalSkor()}, {@link #ambilSkor(ParameterTambahan)}, {@link #getRincianSkor()}.</li>
 * <li><b>Utilitas statis/kuota</b>: {@link #hitungJumlahPendaftarKuota(Session, PaketJurusanPmb,
 * GelombangPendaftaran, String, Long)},
 * {@link #kuotaPaketJurusanMasihTersedia(Session, PaketJurusanPmb, GelombangPendaftaran, String,
 * Long)}, {@link #populatePilihanFakultas()} dan kerabatnya.</li>
 * <li><b>Penyaji UI/tautan</b>: {@link #tampilkanEmail(Component)}, {@link #tampilkanHp(Component)},
 * {@link #ambilHp()}, {@link #urlLogin()}, {@link #putPhoto(Map)}.</li>
 * </ul>
 *
 * <p>Kontrak umum {@code id}/{@code equals}/{@code compareTo}/{@code check()}/{@code retreive()}
 * TIDAK dijelaskan ulang di sini — lihat {@link ais.database.model.GeneralValueObject}. Perilaku
 * kegiatan/pembayaran bersama mahasiswa ada di {@link VOMahasiswa}.</p>
 *
 * @see Mahasiswa
 * @see BiodataMahasiswa
 * @see GelombangPendaftaran
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "biodata_calon_mahasiswa")
public class BiodataCalonMahasiswa extends VOMahasiswa {

	/**
	 * Nilai {@code statusLulus} untuk pendaftar yang DINYATAKAN LULUS seleksi.
	 *
	 * @see #getStatusLulus()
	 */
	public final static Integer LULUS = 1;

	/**
	 * Nilai {@code statusLulus} untuk pendaftar yang TIDAK lulus seleksi (juga dipakai sebagai
	 * nilai awal/bawaan sebelum kelulusan ditetapkan).
	 *
	 * @see #getStatusLulus()
	 */
	public final static Integer TIDAK_LULUS = 0;

	/**
	 * Versi serialisasi Java. Jangan diubah agar sesi/cache lama tetap terbaca.
	 */
	private static final long serialVersionUID = 1995121655114539247L;
	/** Primary key baris ini (kolom {@code id}, IDENTITY). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit ringan). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit ringan). */
	private String olehId;

	/**
	 * Id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna; {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null}/kosong DIABAIKAN (nilai lama dipertahankan) — jejak
	 * audit tidak boleh terhapus hanya karena pemanggil tidak tahu identitas penggunanya.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir. Nilai {@code null}/kosong DIABAIKAN, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna; {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate SEBELUM setiap UPDATE baris ini dan
	 * mendelegasikan pencatatan waktu/pelaku perubahan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 *
	 * <p>Jangan dipanggil manual dari kode aplikasi.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek dibuat, lalu dimutakhirkan
	 * oleh {@link #onUpdate()} melalui interceptor audit.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini. Juga dipakai {@link #getTanggalDiterima()} sebagai
	 * perkiraan tanggal diterima bila kolom tanggal diterima belum pernah diisi.
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: {@code id-noRegistrasi-nama}. Dipakai di log, pesan kesalahan, dan
	 * label komponen ZK.
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getNama()}, yang bisa menarik nama dari
	 * {@link #getMahasiswa()}/{@code mahasiswaAlumni} dan memutasi field {@code nama}.</p>
	 *
	 * @return teks gabungan id, nomor registrasi, dan nama.
	 */
	public String toString() {
		return id + "-" + getKode() + "-" + getNama();
	}

	/**
	 * Salinan HANYA-BACA dari kolom {@code no_registrasi} (lihat {@link #getKode()}).
	 */
	private String kode;

	/**
	 * Kode alami baris ini, yaitu nomor registrasi pendaftar — dipetakan ke kolom
	 * {@code no_registrasi} yang SAMA dengan {@link #getNoRegistrasi()} tetapi
	 * {@code insertable = false, updatable = false}, jadi properti ini murni pembacaan dan tidak
	 * pernah menulis kolom tersebut.
	 *
	 * @return nomor registrasi apa adanya (tanpa normalisasi); {@code null} bila belum ada.
	 * @see #getNoRegistrasi()
	 */
	@Column(name = "no_registrasi", unique = true, insertable = false, updatable = false, length = 20)
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan salinan hanya-baca nomor registrasi.
	 *
	 * @param kode nomor registrasi.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	private String no_kwitansi;
	private String nama;
	private String kk;
	private String nisn;
	private String username;
	private String password;
	private String gelar;
	private String alamat;
	private String nomorSuratKelulusan;
	private Date tanggalSuratKelulusan;

	private String rt;
	private String rw;
	private String tempatLahir;
	private Date tanggalLahir;
	private String formatedtanggallahir;
	private String jenisKelamin;
	private Negara asalNegara;
	private String kewarganegaraan;
	private Agama agama;
	private String kodePos;
	private String hp;

	private String namaAyah;
	private String jurusanSekolahLain;
	private String pendidikanAyah;
	private PekerjaanOrangTua pekerjaanAyah;

	private String namaIbu;
	private String pendidikanIbu;
	private String pekerjaanIbu;
	private String namaUntukIjazah;
	private String noIjazah;
	private String ukuranJaket;
	private Integer tinggiBadan;
	private Integer pernahMenetapDiLuarNegeri;
	private Integer beratBadan;
	private String teleponRumah;

	private String suratIzinMengemudi;
	private String kendaraanKuliah;
	private Integer pernahMemimpinOrganisasi;
	private String namaOrganisasi;
	private String hobi;
	private String minatSeni;
	private String kemampuanBahasa1;
	private String kemampuanBahasa2;
	private String kemampuanBahasa3;
	private String asalSma;
	private String alamatAsalSma;
	private String asalSmp;
	private String alamatAsalSmp;
	private String asalSd;
	private String alamatAsalSd;
	private String golonganDarah;
	private Integer statusNikah;
	private String bahasa;
	private String dariNamaDosenKaryawan;

	private String noRegistrasi;
	private String noUjian;
	private Integer pin;
	private String pinPassword;
	private Integer statusPembayaran;
	private String nim;
	private String kewarganegaraan_asli;
	private Boolean generateNimOtomatis;

	// tambahan field (penyesuaian dengan modul PMB yg ada sekarang)

	private Boolean alamatSama;

	private JenisKartuIdentitasMahasiswaBaru jenisKartuIdentitas;
	private String noIdentitas;
	private String email;
	private String dusunCalon;
	private String kelurahanCalon;
	private Wilayah kecamatanCalon;
	private String kecamatan_calon;
	private Propinsi propinsiCalon;
	private Kota kotaCalon;

	private JenisSekolahMahasiswaBaru jenisSekolah;
	private String akreditasiSekolah;
	private String infoKampusDariMana;
	private String namaTemanInfoKampusDariMana;
	private String keteranganInfoKampusDariMana;
	private String kodePosSekolah;
	private Wilayah kecamatanSekolah;
	private Propinsi propinsiSekolah;
	private Kota kotaSekolah;
	private String noTelpSekolah;
	private String tahunKelulusan;
	private JurusanSekolahMahasiswaBaru jurusanSekolah;
	private String jurusanS1;
	private String jurusanS2;

	private String namaWali;
	private String noTelpOrtu;

	private PendapatanOrangTua pendapatanOrtu;
	private PendidikanOrangTua pendidikanOrtu;

	private PendapatanOrangTua pendapatanOrtuIbu;
	private PendidikanOrangTua pendidikanOrtuIbu;
	private PekerjaanOrangTua pekerjaanAyahIbu;

	private PendapatanOrangTua pendapatanOrtuWali;
	private PendidikanOrangTua pendidikanOrtuWali;
	private PekerjaanOrangTua pekerjaanAyahWali;

	private String alamatOrtu;
	private String rtOrtu;
	private String rwOrtu;
	private String kodePosOrtu;
	private Wilayah kecamatanOrtu;
	private String kelurahanOrtu;
	private Propinsi propinsiOrtu;
	private Kota kotaOrtu;

	private Paket paket;
	private Jurusan prodi1;
	private Jurusan prodi2;
	private Jurusan prodi3;
	private Jurusan prodi4;
	private Jurusan prodi5;

	private Konsentrasi konsentrasi;

	private Boolean ditolak;
	private Boolean mundur;

	private Jenjang jenjang;
	private Integer statusLulus = 0;
	private Jurusan prodiLulus;
	private Integer nimGenerated = 0;
	private Integer cetakKartu = 0;

	private String program;
	private String programNIM;

	private JenisSeleksi jenisSeleksi;
	private JenisSeleksi jenisSeleksiDipilih;
	private Date tanggalDaftar = ais.ui.util.WaktuUtil.getDate();
	private Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
	private String ipk;
	private String file;
	private String semesterMulai;
	private String tahunAkademik;
	private GelombangPendaftaran gelombangPendaftaran;
	private GelombangPendaftaran gelombangPendaftaranDiterima;
	private String jenisSemester;

	private Date tanggalPendaftaran = ais.ui.util.WaktuUtil.getDate();

	private Kegiatan pembayaranRegistrasi;
	private Kegiatan pembayaranDaftarUlang;

	private Date tanggalPembayaranRegistrasi;
	private Date tanggalPembayaranDaftarUlang;

	private Mahasiswa mahasiswa;
	private Mahasiswa mahasiswaAlumni;
	private UploadBiodataCalonMahasiswa uploadBiodataCalonMahasiswa;

	private Boolean pernyataan;

	private Boolean merupakanPindahan;
	private String pindahanDariKampus;
	private String pindahanDariProdi;
	private String nimLamaSebelumPindah;
	private Integer pindahDariKampusLamaDiSemester = 0;
	private Date tanggalPindah = ais.ui.util.WaktuUtil.getDate();
	private String keteranganPindah;
	private StatusAwalMahasiswa statusAwalMahasiswa;
	private StatusAwalMahasiswa statusAwalDiterima;
	private String parameterTambahan;
	private String parameterTambahanInds;

	private KelompokCalonMahasiswa kelompokCalonMahasiswa;
	private Integer totalSkor;
	private String rincianSkor;

	private Boolean telahLogin;
	private Date waktuLogin = ais.ui.util.WaktuUtil.getDate();
	private NamaSekolahAsal namaSekolahAsal;
	private Boolean udah;
	private String keterangan;
	private PerguruanTinggiLain pindahanDari;

	private String instansiAsal;
	private Wilayah kotaInstansi;
	private String jabatanDiInstansiAsal;

	private AfiliasiCalonMahasiswa afiliasiCalonMahasiswa;
	private Mahasiswa afiliasiMahasiswa;
	private Pegawai afiliasiPegawai;

	private Date tanggalDiterima;
	private KelompokJenisSeleksi kelompokJenisSeleksi;
	private Date tanggalMasuk;

	private Boolean aktif;
	private Tbmuser dikunci;
	private DisposisiSop disposisiSop;
	private KelasPmb kelasPmb;

	public Set<Fakultas> populatePilihanFakultas() {
		Set<Fakultas> fakultas = new HashSet<Fakultas>();
		addFakultas(fakultas, getProdi1());
		addFakultas(fakultas, getProdi2());
		addFakultas(fakultas, getProdi3());
		addFakultas(fakultas, getProdi4());
		addFakultas(fakultas, getProdi5());
		addFakultas(fakultas, getProdiLulus());
		return fakultas;
	}

	public Set<Long> populatePilihanFakultasIds() {
		Set<Long> fakultasIds = new HashSet<Long>();
		addFakultasId(fakultasIds, getProdi1());
		addFakultasId(fakultasIds, getProdi2());
		addFakultasId(fakultasIds, getProdi3());
		addFakultasId(fakultasIds, getProdi4());
		addFakultasId(fakultasIds, getProdi5());
		addFakultasId(fakultasIds, getProdiLulus());
		return fakultasIds;
	}

	public List<Jurusan> populatePilihanJurusan() {
		List<Jurusan> jurusans = new ArrayList<Jurusan>();
		addJurusan(jurusans, getProdi1());
		addJurusan(jurusans, getProdi2());
		addJurusan(jurusans, getProdi3());
		addJurusan(jurusans, getProdi4());
		addJurusan(jurusans, getProdi5());
		addJurusan(jurusans, getProdiLulus());
		return jurusans;
	}

	public List<Long> populatePilihanJurusanIds() {
		List<Long> jurusanIds = new ArrayList<Long>();
		addJurusanId(jurusanIds, getProdi1());
		addJurusanId(jurusanIds, getProdi2());
		addJurusanId(jurusanIds, getProdi3());
		addJurusanId(jurusanIds, getProdi4());
		addJurusanId(jurusanIds, getProdi5());
		addJurusanId(jurusanIds, getProdiLulus());
		return jurusanIds;
	}

	private static void addFakultas(Set<Fakultas> fakultas, Jurusan jurusan) {
		try {
			if (fakultas != null && jurusan != null && jurusan.getFakultas() != null) {
				fakultas.add(jurusan.getFakultas());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:387");
		}
	}

	private static void addFakultasId(Set<Long> fakultasIds, Jurusan jurusan) {
		try {
			if (fakultasIds != null && jurusan != null && jurusan.getFakultas() != null
					&& jurusan.getFakultas().getId() != null) {
				fakultasIds.add(jurusan.getFakultas().getId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:397");
		}
	}

	private static void addJurusan(List<Jurusan> jurusans, Jurusan jurusan) {
		if (jurusans != null && jurusan != null && !jurusans.contains(jurusan)) {
			jurusans.add(jurusan);
		}
	}

	private static void addJurusanId(List<Long> jurusanIds, Jurusan jurusan) {
		if (jurusanIds != null && jurusan != null && jurusan.getId() != null && !jurusanIds.contains(jurusan.getId())) {
			jurusanIds.add(jurusan.getId());
		}
	}

	/**
	 * Menghitung jumlah pendaftar aktif yang memakai kombinasi paket + prodi pada
	 * PaketJurusanPmb. Digunakan untuk validasi kuota PMB.
	 */
	public static int hitungJumlahPendaftarKuota(Session session, PaketJurusanPmb paketJurusanPmb,
			GelombangPendaftaran gelombangPendaftaran, String tahunAkademik, Long abaikanBiodataId) {
		if (paketJurusanPmb == null || paketJurusanPmb.getPaket() == null || paketJurusanPmb.getJurusan() == null) {
			return 0;
		}
		Session useSession = session;
		try {
			if (useSession == null) {
				useSession = HibernateUtil.currentSession();
			}

			Criteria criteria = useSession.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(Restrictions.isNull("ditolak"), Restrictions.eq("ditolak", false)))
					.add(Restrictions.or(Restrictions.isNull("mundur"), Restrictions.eq("mundur", false)))
					.add(Restrictions.eq("paket", paketJurusanPmb.getPaket()));

			if (tahunAkademik != null && tahunAkademik.trim().length() > 0) {
				criteria.add(Restrictions.eq("tahunAkademik", tahunAkademik.trim()));
			}
			if (paketJurusanPmb.getKuotaBerlakuPerGelombang() && gelombangPendaftaran != null
					&& gelombangPendaftaran.getId() != null) {
				criteria.add(Restrictions.eq("gelombangPendaftaran", gelombangPendaftaran));
			}
			if (abaikanBiodataId != null) {
				criteria.add(Restrictions.ne("id", abaikanBiodataId));
			}

			Jurusan jurusan = paketJurusanPmb.getJurusan();
			Criterion jurusanDipilih = Restrictions.or(Restrictions.eq("prodi1", jurusan),
					Restrictions.or(Restrictions.eq("prodi2", jurusan),
							Restrictions.or(Restrictions.eq("prodi3", jurusan),
									Restrictions.or(Restrictions.eq("prodi4", jurusan),
											Restrictions.or(Restrictions.eq("prodi5", jurusan),
													Restrictions.eq("prodiLulus", jurusan))))));
			criteria.add(jurusanDipilih);
			criteria.setProjection(Projections.rowCount());
			criteria.setCacheable(false);
			Number count = (Number) criteria.uniqueResult();
			return count == null ? 0 : count.intValue();
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:460");
			}
			return 0;
		}
	}

	public static boolean kuotaPaketJurusanMasihTersedia(Session session, PaketJurusanPmb paketJurusanPmb,
			GelombangPendaftaran gelombangPendaftaran, String tahunAkademik, Long abaikanBiodataId) {
		if (paketJurusanPmb == null) {
			return true;
		}
		Integer kuota = paketJurusanPmb.getKuota();
		if (kuota == null || kuota.intValue() <= 0) {
			return true;
		}
		return hitungJumlahPendaftarKuota(session, paketJurusanPmb, gelombangPendaftaran, tahunAkademik,
				abaikanBiodataId) < kuota.intValue();
	}

	public BiodataCalonMahasiswa() {
	}

	public BiodataCalonMahasiswa(Long id) {
		this.id = id;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "alamat", length = 255)
	public String getAlamat() {

		try {
			if ((alamat == null || alamat.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getAlamat() != null
					&& !mahasiswaAlumni.ambilBiodata().getAlamat().isEmpty()) {
				alamat = mahasiswaAlumni.ambilBiodata().getAlamat();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:506");
			// TODO: handle exception
		}

		return this.alamat;
	}

	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	@Column(name = "nama_ayah", columnDefinition = "text")
	public String getNamaAyah() {

		try {
			if ((namaAyah == null || namaAyah.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getNamaAyah() != null
					&& !mahasiswaAlumni.ambilBiodata().getNamaAyah().isEmpty()) {
				namaAyah = mahasiswaAlumni.ambilBiodata().getNamaAyah();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:526");
			// TODO: handle exception
		}

		return this.namaAyah;
	}

	public void setNamaAyah(String namaAyah) {
		this.namaAyah = namaAyah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pekerjaan_orang_tua", nullable = true)
	public PekerjaanOrangTua getPekerjaanAyah() {
		pekerjaanAyah = check(pekerjaanAyah);
		return this.pekerjaanAyah;
	}

	public void setPekerjaanAyah(PekerjaanOrangTua pekerjaanAyah) {
		this.pekerjaanAyah = pekerjaanAyah;
	}

	@Column(name = "nama_ibu", columnDefinition = "text")
	public String getNamaIbu() {
		if (namaIbu != null) {
			namaIbu = org.apache.commons.lang3.StringUtils.replace(namaIbu, "  ", " ");
			namaIbu = org.apache.commons.lang3.StringUtils.replace(namaIbu, "  ", " ");
			namaIbu = org.apache.commons.lang3.StringUtils.replace(namaIbu, "  ", " ");
			namaIbu = namaIbu.trim();
		}

		try {
			if ((namaIbu == null || namaIbu.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getNamaIbu() != null
					&& !mahasiswaAlumni.ambilBiodata().getNamaIbu().isEmpty()) {
				namaIbu = mahasiswaAlumni.ambilBiodata().getNamaIbu();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:563");
			// TODO: handle exception
		}

		return this.namaIbu;
	}

	public void setNamaIbu(String namaIbu) {
		this.namaIbu = namaIbu;
	}

	@Column(name = "pekerjaan_ibu", length = 150)
	public String getPekerjaanIbu() {

		return this.pekerjaanIbu;
	}

	public void setPekerjaanIbu(String pekerjaanIbu) {
		this.pekerjaanIbu = pekerjaanIbu;
	}

	@Column(name = "nama_untuk_ijazah", columnDefinition = "text")
	public String getNamaUntukIjazah() {
		return this.namaUntukIjazah;
	}

	public void setNamaUntukIjazah(String namaUntukIjazah) {
		this.namaUntukIjazah = namaUntukIjazah;
	}

	@Column(name = "no_ijazah", columnDefinition = "text")
	public String getNoIjazah() {
		return this.noIjazah;
	}

	public void setNoIjazah(String noIjazah) {
		this.noIjazah = noIjazah;
	}

	@Column(name = "ukuran_jaket", columnDefinition = "text")
	public String getUkuranJaket() {
		return this.ukuranJaket;
	}

	public void setUkuranJaket(String ukuranJaket) {
		this.ukuranJaket = ukuranJaket;
	}

	@Column(name = "tinggi_badan")
	public Integer getTinggiBadan() {
		return this.tinggiBadan;
	}

	public void setTinggiBadan(Integer tinggiBadan) {
		this.tinggiBadan = tinggiBadan;
	}

	@Column(name = "pernah_menetap_di_luar_negeri")
	public Integer getPernahMenetapDiLuarNegeri() {
		return this.pernahMenetapDiLuarNegeri;
	}

	public void setPernahMenetapDiLuarNegeri(Integer pernahMenetapDiLuarNegeri) {
		this.pernahMenetapDiLuarNegeri = pernahMenetapDiLuarNegeri;
	}

	@Column(name = "berat_badan")
	public Integer getBeratBadan() {
		return this.beratBadan;
	}

	public void setBeratBadan(Integer beratBadan) {
		this.beratBadan = beratBadan;
	}

	@Column(name = "telepon_rumah", length = 20)
	public String getTeleponRumah() {

		return this.teleponRumah == null ? "" : teleponRumah.trim().replaceAll("[^\\d.]", "");
	}

	public void setTeleponRumah(String teleponRumah) {
		this.teleponRumah = teleponRumah;
	}

	@Column(name = "hp", length = 20)
	public String getHp() {
		// Gunakan local variable agar this.hp tidak dimutasi oleh getter.
		// tampilkanHp() sudah menangani teleponRumah secara terpisah untuk tampilan.
		String hpResult = this.hp == null ? "" : this.hp;

		try {
			if (hpResult.trim().isEmpty() && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getHp() != null
					&& !mahasiswaAlumni.ambilBiodata().getHp().isEmpty()) {
				hpResult = mahasiswaAlumni.ambilBiodata().getHp();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:660");
			// ignore
		}

		// Strip tanda petik (') di depan — konvensi user untuk format teks di Excel
		if (hpResult != null) {
			while (hpResult.startsWith("'")) {
				hpResult = hpResult.substring(1);
			}
		}

		return hpResult == null ? "" : hpResult.trim().replaceAll("[^\\d.]", "");
	}

	public void setHp(String hp) {
		this.hp = hp;
	}

	@Column(name = "surat_izin_mengemudi", length = 255)
	public String getSuratIzinMengemudi() {
		return this.suratIzinMengemudi;
	}

	public void setSuratIzinMengemudi(String suratIzinMengemudi) {
		this.suratIzinMengemudi = suratIzinMengemudi;
	}

	@Column(name = "kendaraan_kuliah", length = 255)
	public String getKendaraanKuliah() {
		return this.kendaraanKuliah;
	}

	public void setKendaraanKuliah(String kendaraanKuliah) {
		this.kendaraanKuliah = kendaraanKuliah;
	}

	@Column(name = "pernah_memimpin_organisasi")
	public Integer getPernahMemimpinOrganisasi() {
		return this.pernahMemimpinOrganisasi;
	}

	public void setPernahMemimpinOrganisasi(Integer pernahMemimpinOrganisasi) {
		this.pernahMemimpinOrganisasi = pernahMemimpinOrganisasi;
	}

	@Column(name = "nama_organisasi", length = 255)
	public String getNamaOrganisasi() {
		return this.namaOrganisasi;
	}

	public void setNamaOrganisasi(String namaOrganisasi) {
		this.namaOrganisasi = namaOrganisasi;
	}

	@Column(name = "hobi")
	public String getHobi() {
		return this.hobi;
	}

	public void setHobi(String hobi) {
		this.hobi = hobi;
	}

	@Column(name = "minat_seni")
	public String getMinatSeni() {
		return this.minatSeni;
	}

	public void setMinatSeni(String minatSeni) {
		this.minatSeni = minatSeni;
	}

	@Column(name = "kemampuan_bahasa1", length = 255)
	public String getKemampuanBahasa1() {
		return this.kemampuanBahasa1;
	}

	public void setKemampuanBahasa1(String kemampuanBahasa1) {
		this.kemampuanBahasa1 = kemampuanBahasa1;
	}

	@Column(name = "kemampuan_bahasa2", length = 255)
	public String getKemampuanBahasa2() {
		return this.kemampuanBahasa2;
	}

	public void setKemampuanBahasa2(String kemampuanBahasa2) {
		this.kemampuanBahasa2 = kemampuanBahasa2;
	}

	@Column(name = "kemampuan_bahasa3", length = 255)
	public String getKemampuanBahasa3() {
		return this.kemampuanBahasa3;
	}

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
		return this.asalSma == null || this.asalSma.trim().equalsIgnoreCase("== Klik disini untuk pilih ==") ? ""
				: asalSma.trim();
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

	@Column(name = "asal_smp", length = 255)
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

	@Column(name = "asal_sd", length = 255)
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
		return this.golonganDarah;
	}

	public void setGolonganDarah(String golonganDarah) {
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
		if (kewarganegaraan == null) {
			kewarganegaraan = Mahasiswa.WNI;
		}
		return this.kewarganegaraan;
	}

	public void setKewarganegaraan(String kewarganegaraan) {
		this.kewarganegaraan = kewarganegaraan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "agama", nullable = true)
	public Agama getAgama() {
		agama = check(agama);

		try {
			if ((agama == null) && getMahasiswaAlumni() != null && mahasiswaAlumni.ambilBiodata() != null
					&& mahasiswaAlumni.ambilBiodata().getAgama() != null) {
				agama = mahasiswaAlumni.ambilBiodata().getAgama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:866");
			// TODO: handle exception
		}
		return this.agama;
	}

	public void setAgama(Agama agama) {
		this.agama = agama;
	}

	public void setRt(String rt) {
		this.rt = rt;
	}

	@Column(name = "rt", length = 20)
	public String getRt() {
		if (rt != null && rt.length() > 3) {
			rt = rt.substring(0, 3);
		}

		try {
			if ((rt == null || rt.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getRt() != null
					&& !mahasiswaAlumni.ambilBiodata().getRt().isEmpty()) {
				rt = mahasiswaAlumni.ambilBiodata().getRt();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:892");
			// TODO: handle exception
		}

		return rt;
	}

	public void setRw(String rw) {
		this.rw = rw;
	}

	@Column(name = "rw", length = 20)
	public String getRw() {
		if (rw != null && rw.length() > 3) {
			rw = rw.substring(0, 3);
		}

		try {
			if ((rw == null || rw.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getRw() != null
					&& !mahasiswaAlumni.ambilBiodata().getRw().isEmpty()) {
				rw = mahasiswaAlumni.ambilBiodata().getRw();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:915");
			// TODO: handle exception
		}

		return rw;
	}

	public void setTempatLahir(String tempatLahir) {
		this.tempatLahir = tempatLahir;
	}

	@Column(name = "tempat_lahir", columnDefinition = "text")
	public String getTempatLahir() {

		try {
			if ((tempatLahir == null || tempatLahir.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.getTempatlahir() != null && !mahasiswaAlumni.getTempatlahir().trim().isEmpty()) {
				tempatLahir = mahasiswaAlumni.getTempatlahir();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:934");
			// TODO: handle exception
		}
		return tempatLahir;
	}

	public void setTanggalLahir(Date tanggalLahir) {
		this.tanggalLahir = tanggalLahir;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_lahir")
	public Date getTanggalLahir() {

		try {
			if ((tanggalLahir == null) && getMahasiswaAlumni() != null && mahasiswaAlumni.getTanggallahir() != null) {
				tanggalLahir = mahasiswaAlumni.getTanggallahir();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:952");
			// TODO: handle exception
		}

		return tanggalLahir;
	}

	public void setJenisKelamin(String jenisKelamin) {
		this.jenisKelamin = jenisKelamin;
	}

	@Column(name = "jenis_kelamin", length = 20)
	public String getJenisKelamin() {

		try {
			if ((jenisKelamin == null || jenisKelamin.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.getKelamin() != null && !mahasiswaAlumni.getKelamin().trim().isEmpty()) {
				jenisKelamin = mahasiswaAlumni.getKelamin();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:971");
			// TODO: handle exception
		}

		return jenisKelamin;
	}

	public void setAsalNegara(Negara asalNegara) {
		this.asalNegara = asalNegara;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asal_negara", nullable = true)
	public Negara getAsalNegara() {

		try {
			if ((asalNegara == null) && getMahasiswaAlumni() != null && mahasiswaAlumni.getNegara() != null) {
				asalNegara = mahasiswaAlumni.getNegara();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:990");
			// TODO: handle exception
		}

		if (asalNegara == null) {
			asalNegara = ConstantValues.INDONESIA;
		}

		asalNegara = check(asalNegara);
		return asalNegara;
	}

	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	@Column(name = "kode_pos", length = 20)
	public String getKodePos() {

		try {
			if ((kodePos == null || kodePos.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getKodepos() != null
					&& !mahasiswaAlumni.ambilBiodata().getKodepos().isEmpty()) {
				kodePos = mahasiswaAlumni.ambilBiodata().getKodepos();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1015");
			// TODO: handle exception
		}

		if (kodePos != null && kodePos.length() > 8) {
			kodePos = kodePos.substring(0, 8);
		}
		return kodePos;
	}

	public void setDariNamaDosenKaryawan(String dariNamaDosenKaryawan) {
		this.dariNamaDosenKaryawan = dariNamaDosenKaryawan;
	}

	@Column(name = "dari_nama_dosen_karyawan", columnDefinition = "text")
	public String getDariNamaDosenKaryawan() {
		return dariNamaDosenKaryawan;
	}

	public void setPendidikanAyah(String pendidikanAyah) {
		this.pendidikanAyah = pendidikanAyah;
	}

	@Column(name = "pendidikan_ayah", length = 20)
	public String getPendidikanAyah() {
		return pendidikanAyah;
	}

	public void setPendidikanIbu(String pendidikanIbu) {
		this.pendidikanIbu = pendidikanIbu;
	}

	@Column(name = "pendidikan_ibu", length = 20)
	public String getPendidikanIbu() {
		return pendidikanIbu;
	}

	public void setNo_kwitansi(String no_kwitansi) {
		this.no_kwitansi = no_kwitansi;
	}

	@Column(name = "no_kwitansi", columnDefinition = "text")
	public String getNo_kwitansi() {
		return no_kwitansi;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "nama", columnDefinition = "text")
	public String getNama() {
		if (nama != null) {
			nama = org.apache.commons.lang3.StringUtils.replace(nama, "  ", " ");
			nama = org.apache.commons.lang3.StringUtils.replace(nama, "  ", " ");
			nama = org.apache.commons.lang3.StringUtils.replace(nama, "  ", " ");
			nama = nama.trim();
		}

		mahasiswa = getMahasiswa();
		if (mahasiswa != null) {
			nama = getMahasiswa().getNama();
		}

		if (getMahasiswaAlumni() != null && mahasiswaAlumni.getNama() != null) {
			nama = mahasiswaAlumni.getNama();
		}

		return this.nama == null ? null : this.nama.trim().toUpperCase();
	}

	public void setNoRegistrasi(String noRegistrasi) {
		this.noRegistrasi = noRegistrasi;
	}

	@Column(name = "no_registrasi", unique = true, length = 20)
	public String getNoRegistrasi() {
		return noRegistrasi == null || noRegistrasi.trim().isEmpty() ? null : noRegistrasi.trim();
	}

	public void setNoUjian(String noUjian) {
		this.noUjian = noUjian;
	}

	@Column(name = "no_ujian", length = 255)
	public String getNoUjian() {

		if (Common.bolehKonfigurasi("nomor_ujian_calon_mahasiswa_sama_dengan_no_reg", Konfigurasi.TIDAK_AKTIF)) {
			noUjian = getNoRegistrasi();
		}

		if (noUjian != null && noUjian.trim().isEmpty()) {
			noUjian = null;
		}
		return noUjian;
	}

	public void setPin(Integer pin) {
		this.pin = pin;
	}

	@Column(name = "pin")
	public Integer getPin() {
		return pin;
	}

	public void setStatusPembayaran(Integer statusPembayaran) {
		this.statusPembayaran = statusPembayaran;
	}

	@Column(name = "status_pembayaran", length = 1)
	public Integer getStatusPembayaran() {
		return statusPembayaran;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kartu_identitas_mahasiswa_baru", nullable = true)
	public JenisKartuIdentitasMahasiswaBaru getJenisKartuIdentitas() {
		jenisKartuIdentitas = check(jenisKartuIdentitas);
		return jenisKartuIdentitas;
	}

	public void setJenisKartuIdentitas(JenisKartuIdentitasMahasiswaBaru jenisKartuIdentitas) {
		this.jenisKartuIdentitas = jenisKartuIdentitas;
	}

	@Column(name = "no_identitas", length = 255)
	public String getNoIdentitas() {

		try {
			if ((noIdentitas == null || noIdentitas.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getNoIdentitas() != null
					&& !mahasiswaAlumni.ambilBiodata().getNoIdentitas().isEmpty()) {
				noIdentitas = mahasiswaAlumni.ambilBiodata().getNoIdentitas();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1150");
			// TODO: handle exception
		}

		return noIdentitas == null ? "" : noIdentitas.trim();
	}

	public void setNoIdentitas(String noIdentitas) {
		this.noIdentitas = noIdentitas;
	}

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
			mahasiswa = getMahasiswa();
			if (email.trim().isEmpty() && mahasiswa != null && mahasiswa.ambilEmail() != null
					&& !mahasiswa.ambilEmail().isEmpty()) {
				email = mahasiswa.ambilEmail();
			}

			if ((email == null || email.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.getEmail() != null) {
				email = mahasiswaAlumni.getEmail();
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1195");
			// TODO: handle exception
		}

		return this.email == null ? "" : email.trim();
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	@Column(name = "keluarahan_calon", length = 255)
	public String getKelurahanCalon() {

		try {
			if ((kelurahanCalon == null || kelurahanCalon.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getKelurahan() != null
					&& !mahasiswaAlumni.ambilBiodata().getKelurahan().isEmpty()) {
				kelurahanCalon = mahasiswaAlumni.ambilBiodata().getNoIdentitas();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1224");
			// TODO: handle exception
		}

		return kelurahanCalon;
	}

	public void setKelurahanCalon(String kelurahanCalon) {
		this.kelurahanCalon = kelurahanCalon;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kecamatan_calon_wilayah", nullable = true)
	public Wilayah getKecamatanCalon() {
		kecamatanCalon = check(kecamatanCalon);
		mahasiswaAlumni = check(mahasiswaAlumni);
		// Fallback ke data alumni jika null
		try {
			if ((kecamatanCalon == null) && mahasiswaAlumni != null && mahasiswaAlumni.ambilBiodata() != null
					&& mahasiswaAlumni.ambilBiodata().getKecamatan() != null) {
				kecamatanCalon = mahasiswaAlumni.ambilBiodata().getKecamatan();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1246");
		}

		// Logika perbaikan wilayah induk via Feeder
		if (kecamatanCalon != null && kecamatanCalon.getWilayahInduk() == null && kecamatanCalon.getFeeder() != null) {
			String targetFeeder = kecamatanCalon.getFeeder();
			Map<?, ?> mapWilayah = ConstantValues.ambilBerdasarClass(Wilayah.class);
			if (mapWilayah != null) {
				for (Object o : mapWilayah.values()) {
					if (o instanceof Wilayah) {
						Wilayah w = (Wilayah) o;
						if (w.getWilayahInduk() != null && targetFeeder.equals(w.getFeeder())) {
							kecamatanCalon = w;
							break;
						}
					}
				}
			}
		}
		return kecamatanCalon;
	}

	public void setKecamatanCalon(Wilayah kecamatan) {
		this.kecamatanCalon = kecamatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi_calon", nullable = true)
	public Propinsi getPropinsiCalon() {
		propinsiCalon = check(propinsiCalon);
		kecamatanCalon = check(kecamatanCalon);
		mahasiswaAlumni = check(mahasiswaAlumni);
		kotaCalon = check(kotaCalon);
		// 1. Coba ambil dari data alumni
		try {
			if ((propinsiCalon == null) && mahasiswaAlumni != null && mahasiswaAlumni.ambilBiodata() != null
					&& mahasiswaAlumni.ambilBiodata().getPropinsi() != null) {
				propinsiCalon = mahasiswaAlumni.ambilBiodata().getPropinsi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1285");
		}

		// 2. Coba ambil dari Kota Calon jika sudah ada
		if (propinsiCalon == null && kotaCalon != null && kotaCalon.getPropinsi() != null) {
			propinsiCalon = kotaCalon.getPropinsi();
		}

		// 3. LOGIKA BARU: Ambil dari Induknya Kecamatan (Kecamatan -> Kota -> Propinsi)
		if (propinsiCalon == null && kecamatanCalon != null && kecamatanCalon.getWilayahInduk() != null) {
			Wilayah wilayahKab = kecamatanCalon.getWilayahInduk();
			Wilayah wilayahProp = wilayahKab.getWilayahInduk(); // Induk kedua adalah Propinsi

			if (wilayahProp != null) {
				Session session = null;
				try {
					/* WAJIB session khusus (bukan thread-local): getter ini
					 * dipanggil Hibernate saat INSERT entity; memakai lalu
					 * menutup session thread-local membuat save berikutnya
					 * gagal "Session is closed!". */
					session = HibernateUtil.getSessionFactory().openSession();
					propinsiCalon = findOrCreatePropinsi(session, wilayahProp.getNama());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				} finally {
					closeSessionSafe(session);
				}
			}
		}

		return propinsiCalon;
	}

	public void setPropinsiCalon(Propinsi propinsiCalon) {
		this.propinsiCalon = propinsiCalon;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota_calon", nullable = true)
	public Kota getKotaCalon() {
		kotaCalon = check(kotaCalon);
		kecamatanCalon = check(kecamatanCalon);
		mahasiswaAlumni = check(mahasiswaAlumni);

		// 1. Coba ambil dari data alumni
		try {
			if ((kotaCalon == null) && mahasiswaAlumni != null && mahasiswaAlumni.ambilBiodata() != null
					&& mahasiswaAlumni.ambilBiodata().getKota() != null) {
				kotaCalon = mahasiswaAlumni.ambilBiodata().getKota();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1335");
		}

		// 2. LOGIKA BARU: Ambil dari Induk Pertama Kecamatan (Kecamatan -> Kota)
		if (kotaCalon == null && kecamatanCalon != null && kecamatanCalon.getWilayahInduk() != null) {
			Wilayah wilayahKab = kecamatanCalon.getWilayahInduk();

			Session session = null;
			try {
				/* WAJIB session khusus (bukan thread-local), lihat catatan di
				 * getPropinsiCalon: getter ini dieksekusi Hibernate di tengah
				 * proses save sehingga session thread-local tidak boleh
				 * dipakai apalagi ditutup dari sini. */
				session = HibernateUtil.getSessionFactory().openSession();
				// Pastikan Propinsi Calon sudah terisi untuk memfilter pencarian Kota
				Propinsi pCalon = getPropinsiCalon();
				if (pCalon != null) {
					kotaCalon = findBestMatchKota(session, pCalon, wilayahKab.getNama());
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				closeSessionSafe(session);
			}
		}

		return kotaCalon;
	}

	public void setKotaCalon(Kota kotaCalon) {
		this.kotaCalon = kotaCalon;
	}

	// --- HELPER METHODS UNTUK MENGURANGI DUPLIKASI DAN MENGHEMAT MEMORI ---

	/**
	 * Menutup HANYA session khusus milik getter ini. Versi lama ikut memanggil
	 * HibernateUtil.closeSession() yang menutup session THREAD-LOCAL - padahal
	 * getter ini dipanggil Hibernate di tengah save() entity lain, sehingga
	 * proses simpan berikutnya meledak "Session is closed!" (ElearningApiUtil
	 * simpanProperty dkk). Transaksi implisit di-rollback dulu agar koneksi
	 * tidak kembali ke pool berstatus "idle in transaction".
	 */
	private void closeSessionSafe(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.doWork(new org.hibernate.jdbc.Work() {
				public void execute(java.sql.Connection connection) throws java.sql.SQLException {
					if (connection != null && !connection.isClosed() && !connection.getAutoCommit()) {
						connection.rollback();
					}
				}
			});
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1390");
		}
		try {
			session.clear();
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1394");
		}
		try {
			session.disconnect();
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1398");
		}
		try {
			session.close();
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1402");
		}
	}

	private Propinsi findOrCreatePropinsi(Session session, String namaProp) {
		if (namaProp == null || namaProp.trim().isEmpty())
			return null;

		String cleanNamaTarget = StringUtils.replace(namaProp, "Prop.", "").trim().toLowerCase();
		List<Propinsi> list = ConstantValues.simpleList(session.createCriteria(Propinsi.class)
				.add(Restrictions.isNotNull("nama")).add(Restrictions.ne("nama", "")), Propinsi.class);

		Propinsi bestMatch = null;
		int minDistance = Integer.MAX_VALUE;

		for (Propinsi p : list) {
			String cleanName = StringUtils.replace(p.getNama(), "Prop.", "").trim().toLowerCase();
			int distance = StringUtils.getLevenshteinDistance(cleanName, cleanNamaTarget);
			if (distance < minDistance) {
				minDistance = distance;
				bestMatch = p;
			}
		}

		if (bestMatch != null && minDistance < 2)
			return bestMatch;

		// Jika tidak ketemu, buat baru
		Propinsi newProp = new Propinsi();
		newProp.setNama(namaProp.trim());
		newProp.setNegara(ConstantValues.INDONESIA);

		boolean isNewTx = false;
		if (!session.getTransaction().isActive()) {
			session.getTransaction().begin();
			isNewTx = true;
		}
		session.save(newProp);
		if (isNewTx)
			session.getTransaction().commit();

		return newProp;
	}

	private Kota findBestMatchKota(Session session, Propinsi p, String namaKab) {
		if (namaKab == null || p == null)
			return null;

		String cleanTarget = namaKab.replace("Kab.", "").replace("Kota", "").trim().toLowerCase();
		List<Kota> list = ConstantValues.simpleList(session.createCriteria(Kota.class)
				.add(Restrictions.eq("propinsi", p)).add(Restrictions.isNotNull("nama")), Kota.class);

		Kota bestMatch = null;
		int minDistance = Integer.MAX_VALUE;

		for (Kota k : list) {
			String cleanName = k.getNama().replace("Kab.", "").replace("Kota", "").trim().toLowerCase();
			int distance = StringUtils.getLevenshteinDistance(cleanName, cleanTarget);
			if (distance < minDistance) {
				minDistance = distance;
				bestMatch = k;
			}
		}
		return (minDistance < 2) ? bestMatch : null;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_sekolah_mahasiswa_baru", nullable = true)
	public JenisSekolahMahasiswaBaru getJenisSekolah() {
		jenisSekolah = check(jenisSekolah);

		if (getJurusanSekolah() != null && getJurusanSekolah().getJenisSekolahMahasiswaBaru() != null) {
			jenisSekolah = getJurusanSekolah().getJenisSekolahMahasiswaBaru();
		}

		return jenisSekolah;
	}

	public void setJenisSekolah(JenisSekolahMahasiswaBaru jenisSekolah) {
		this.jenisSekolah = jenisSekolah;
	}

	@Column(name = "akreditasi_sekolah", length = 10)
	public String getAkreditasiSekolah() {
		if (akreditasiSekolah == null) {
			akreditasiSekolah = "";
		}
		return akreditasiSekolah.trim();
	}

	public void setAkreditasiSekolah(String akreditasiSekolah) {
		this.akreditasiSekolah = akreditasiSekolah;
	}

	@Column(name = "kodepos_sekolah", length = 10)
	public String getKodePosSekolah() {
		if (kodePosSekolah != null && kodePosSekolah.length() > 8) {
			kodePosSekolah = kodePosSekolah.substring(0, 8);
		}
		return kodePosSekolah;
	}

	public void setKodePosSekolah(String kodePosSekolah) {
		this.kodePosSekolah = kodePosSekolah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kecamatan_sekolah_wilayah", nullable = true)
	public Wilayah getKecamatanSekolah() {
		kecamatanSekolah = check(kecamatanSekolah);

		if (kecamatanSekolah != null && kecamatanSekolah.getWilayahInduk() == null) {

			for (Object o : ConstantValues.ambilBerdasarClass(Wilayah.class).values()) {
				Wilayah w = (Wilayah) o;
				if (w != null && w.getFeeder() != null && kecamatanSekolah.getFeeder() != null
						&& kecamatanSekolah.getFeeder().equals(w.getFeeder()) && w.getWilayahInduk() != null) {
					kecamatanSekolah = w;
					break;
				}
			}

		}

		return kecamatanSekolah;
	}

	public void setKecamatanSekolah(Wilayah kecamatan) {
		this.kecamatanSekolah = kecamatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi_sekolah", nullable = true)
	public Propinsi getPropinsiSekolah() {
		propinsiSekolah = check(propinsiSekolah);
		return propinsiSekolah;
	}

	public void setPropinsiSekolah(Propinsi propinsiSekolah) {
		this.propinsiSekolah = propinsiSekolah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota_sekolah", nullable = true)
	public Kota getKotaSekolah() {
		kotaSekolah = check(kotaSekolah);
		return kotaSekolah;
	}

	public void setKotaSekolah(Kota kotaSekolah) {
		this.kotaSekolah = kotaSekolah;
	}

	@Column(name = "tahun_kelulusan", length = 10)
	public String getTahunKelulusan() {
		if (tahunKelulusan == null) {
			tahunKelulusan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "";
		}

		return tahunKelulusan;
	}

	public void setTahunKelulusan(String tahunKelulusan) {
		this.tahunKelulusan = tahunKelulusan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan_sekolah_mahasiswa_baru", nullable = true)
	public JurusanSekolahMahasiswaBaru getJurusanSekolah() {
		jurusanSekolah = check(jurusanSekolah);
		return jurusanSekolah;
	}

	public void setJurusanSekolah(JurusanSekolahMahasiswaBaru jurusanSekolah) {
		this.jurusanSekolah = jurusanSekolah;
	}

	@Column(name = "notelp_ortu", length = 20)
	public String getNoTelpOrtu() {

		try {
			if ((noTelpOrtu == null || noTelpOrtu.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getTelpAyah() != null
					&& !mahasiswaAlumni.ambilBiodata().getTelpAyah().isEmpty()) {
				noTelpOrtu = mahasiswaAlumni.ambilBiodata().getTelpAyah();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1588");
			// TODO: handle exception
		}

		return noTelpOrtu;
	}

	public void setNoTelpOrtu(String noTelpOrtu) {
		this.noTelpOrtu = noTelpOrtu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendapatan_ortu", nullable = true)
	public PendapatanOrangTua getPendapatanOrtu() {

		try {
			if ((pendapatanOrtu == null) && getMahasiswaAlumni() != null && mahasiswaAlumni.ambilBiodata() != null
					&& mahasiswaAlumni.ambilBiodata().getPendapatanOrtu() != null) {
				pendapatanOrtu = mahasiswaAlumni.ambilBiodata().getPendapatanOrtu();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1608");
			// TODO: handle exception
		}

		pendapatanOrtu = check(pendapatanOrtu);
		return pendapatanOrtu;
	}

	public void setPendapatanOrtu(PendapatanOrangTua pendapatanOrtu) {
		this.pendapatanOrtu = pendapatanOrtu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket_registrasi_mahasiswa", nullable = true)
	public Paket getPaket() {

		try {
			uploadBiodataCalonMahasiswa = getUploadBiodataCalonMahasiswa();

			if (uploadBiodataCalonMahasiswa != null && uploadBiodataCalonMahasiswa.getPaket() != null) {
				paket = uploadBiodataCalonMahasiswa.getPaket();
			} else {
				paket = check(paket);
			}
		} catch (Exception e) {
			paket = check(paket);
		}

		terapkanKonsistensiPaketGelombang();

		return paket;
	}

	/**
	 * Guard supaya query konsistensi di {@link #terapkanKonsistensiPaketGelombang()} tak diulang
	 * di SETIAP panggilan {@link #getPaket()} (getter ini dipanggil sangat sering saat merender
	 * daftar/grid calon mahasiswa) -- hanya diulang kalau id gelombang berbeda dari validasi
	 * terakhir. Sentinel -1L = "belum pernah divalidasi". Direset di {@link #setPaket(Paket)} dan
	 * {@link #setGelombangPendaftaran(GelombangPendaftaran)} supaya perubahan manual tetap
	 * tervalidasi ulang, bukan dilewati gara-gara id gelombang kebetulan sama seperti sebelumnya.
	 */
	private transient Long paketDivalidasiUntukGelombangId = -1L;

	/**
	 * Tegakkan konsistensi Paket vs Gelombang Pendaftaran -- dipanggil dari {@link #getPaket()}
	 * supaya data LAMA yang sudah terlanjur salah (paket tersimpan tidak termasuk daftar paket
	 * gelombangnya) otomatis terkoreksi setiap kali dibaca, tanpa perlu dibuka-simpan ulang manual
	 * lewat form. Kasus nyata pemicu: Gelombang "Reguler Gel 2" dibatasi hanya paket "Mandiri
	 * Reguler 2", tapi calon tersimpan dengan paket "Mandiri Reguler" -> tagihan di Setting Biaya
	 * tak pernah cocok karena kombinasi paket/gelombangnya sendiri sudah tidak valid.
	 *
	 * <p>
	 * SENGAJA membaca dari {@link GelombangPendaftaran#ambilCachePaketDiizinkan(Long)} (cache
	 * JVM-wide, diisi oleh {@code GelombangPendaftaranAction} saat admin memuat/menyimpan
	 * pengaturan paket gelombang), BUKAN query session di sini -- getter ini dipanggil sangat
	 * sering (tiap baris grid dsb.) dan bisa saja dipanggil dari konteks tanpa session ZK aktif,
	 * jadi {@code HibernateUtil.currentSession()} di sini berpotensi error.
	 *
	 * <p>
	 * Aturan:
	 * <ol>
	 * <li>Gelombang TIDAK membatasi paket (cache kosong utk gelombang ini -- baik karena memang
	 * tidak dibatasi, maupun karena pengaturannya belum pernah dibuka/disimpan lewat layar
	 * Gelombang Pendaftaran sejak server terakhir start) -> tidak diberlakukan apa pun.</li>
	 * <li>Gelombang membatasi ke TEPAT SATU paket -> paket DIPAKSA (override) ke paket itu.</li>
	 * <li>Gelombang membatasi ke LEBIH dari satu paket -> paket yang tersimpan harus salah satu
	 * dari daftar itu; kalau tidak cocok (termasuk kosong), paket dikosongkan (bukan dibiarkan
	 * salah secara diam-diam) supaya WAJIB dipilih ulang secara eksplisit dari opsi yang valid.</li>
	 * </ol>
	 */
	private void terapkanKonsistensiPaketGelombang() {
		try {
			GelombangPendaftaran gelombang = getGelombangPendaftaran();
			Long gelombangId = gelombang == null ? null : gelombang.getId();

			if (gelombangId != null && gelombangId.equals(paketDivalidasiUntukGelombangId)) {
				return;
			}
			paketDivalidasiUntukGelombangId = gelombangId;

			if (gelombangId == null) {
				return;
			}

			List<Long> idPaketDiizinkan = GelombangPendaftaran.ambilIdPaketDiizinkan(gelombangId);

			if (idPaketDiizinkan.isEmpty()) {
				return;
			}
			if (idPaketDiizinkan.size() == 1) {
				// TEPAT SATU paket: langsung resolve objek kanonik via ConstantValues.ambil dan
				// kembalikan -- tanpa perlu membangun List<Paket>. Bila resolve gagal (null),
				// paket lama dibiarkan apa adanya (jangan dikosongkan hanya karena cache belum siap).
				Paket tunggal = (Paket) ais.common.ConstantValues.ambil(Paket.class.getName(),
						idPaketDiizinkan.get(0));
				if (tunggal != null) {
					paket = tunggal;
				}
				return;
			}
			boolean cocok = paket != null && paket.getId() != null
					&& idPaketDiizinkan.contains(paket.getId());
			if (!cocok) {
				paket = null;
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"BiodataCalonMahasiswa.getPaket: terapkanKonsistensiPaketGelombang");
		}
	}

	public void setPaket(Paket paket) {
		this.paket = paket;
		paketDivalidasiUntukGelombangId = -1L;
	}

	@Column(name = "nama_wali", length = 255)
	public String getNamaWali() {

		try {
			if ((namaWali == null || namaWali.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getNamaWali() != null
					&& !mahasiswaAlumni.ambilBiodata().getNamaWali().isEmpty()) {
				namaWali = mahasiswaAlumni.ambilBiodata().getNamaWali();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1652");
			// TODO: handle exception
		}

		return namaWali;
	}

	public void setNamaWali(String namaWali) {
		this.namaWali = namaWali;
	}

	@Column(name = "alamat_ortu", columnDefinition = "text")
	public String getAlamatOrtu() {

		try {
			if ((alamatOrtu == null || alamatOrtu.trim().isEmpty()) && getMahasiswaAlumni() != null
					&& mahasiswaAlumni.ambilBiodata() != null && mahasiswaAlumni.ambilBiodata().getAlamat() != null
					&& !mahasiswaAlumni.ambilBiodata().getAlamat().isEmpty()) {
				alamatOrtu = mahasiswaAlumni.ambilBiodata().getAlamat();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:1672");
			// TODO: handle exception
		}

		return alamatOrtu;
	}

	public void setAlamatOrtu(String alamatOrtu) {
		this.alamatOrtu = alamatOrtu;
	}

	@Column(name = "rt_ortu", length = 10)
	public String getRtOrtu() {
		if (rtOrtu != null && rtOrtu.length() > 3) {
			rtOrtu = rtOrtu.substring(0, 3);
		}
		return rtOrtu;
	}

	public void setRtOrtu(String rtOrtu) {
		this.rtOrtu = rtOrtu;
	}

	@Column(name = "rw_ortu", length = 10)
	public String getRwOrtu() {
		if (rwOrtu != null && rwOrtu.length() > 3) {
			rwOrtu = rwOrtu.substring(0, 3);
		}
		return rwOrtu;
	}

	public void setRwOrtu(String rwOrtu) {
		this.rwOrtu = rwOrtu;
	}

	@Column(name = "kodepos_ortu", length = 10)
	public String getKodePosOrtu() {
		if (kodePosOrtu != null && kodePosOrtu.length() > 8) {
			kodePosOrtu = kodePosOrtu.substring(0, 8);
		}
		return kodePosOrtu;
	}

	public void setKodePosOrtu(String kodePosOrtu) {
		this.kodePosOrtu = kodePosOrtu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kecamatan_ortu_wilayah", nullable = true)
	public Wilayah getKecamatanOrtu() {
		kecamatanOrtu = check(kecamatanOrtu);

		if (kecamatanOrtu != null && kecamatanOrtu.getWilayahInduk() == null) {

			for (Object o : ConstantValues.ambilBerdasarClass(Wilayah.class).values()) {
				Wilayah w = (Wilayah) o;
				if (w != null && w.getFeeder() != null && kecamatanOrtu.getFeeder() != null
						&& kecamatanOrtu.getFeeder().equals(w.getFeeder()) && w.getWilayahInduk() != null) {
					kecamatanOrtu = w;
					break;
				}
			}

		}

		return kecamatanOrtu;
	}

	public void setKecamatanOrtu(Wilayah kecamatan) {
		this.kecamatanOrtu = kecamatan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi_ortu", nullable = true)
	public Propinsi getPropinsiOrtu() {
		propinsiOrtu = check(propinsiOrtu);
		return propinsiOrtu;
	}

	public void setPropinsiOrtu(Propinsi propinsiOrtu) {
		this.propinsiOrtu = propinsiOrtu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota_ortu", nullable = true)
	public Kota getKotaOrtu() {
		kotaOrtu = check(kotaOrtu);
		return kotaOrtu;
	}

	public void setKotaOrtu(Kota kotaOrtu) {
		this.kotaOrtu = kotaOrtu;
	}

	@Column(name = "kelurahan_ortu", columnDefinition = "text")
	public String getKelurahanOrtu() {
		return kelurahanOrtu;
	}

	public void setKelurahanOrtu(String kelurahanOrtu) {
		this.kelurahanOrtu = kelurahanOrtu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_orang_tua", nullable = true)
	public PendidikanOrangTua getPendidikanOrtu() {
		pendidikanOrtu = check(pendidikanOrtu);
		return pendidikanOrtu;
	}

	public void setPendidikanOrtu(PendidikanOrangTua pendidikanOrtu) {
		this.pendidikanOrtu = pendidikanOrtu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "prodi_1", nullable = true)
	public Jurusan getProdi1() {
		paket = getPaket();
		if (paket != null && paket.getJumlahProdiYgBolehDiambil() < 1) {
			prodi1 = null;
		}

		prodi1 = check(prodi1);
		return prodi1;
	}

	public void setProdi1(Jurusan prodi1) {
		this.prodi1 = prodi1;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "prodi_2", nullable = true)
	public Jurusan getProdi2() {
		paket = getPaket();
		if (paket != null && paket.getJumlahProdiYgBolehDiambil() < 2) {
			prodi2 = null;
		}
		prodi2 = check(prodi2);
		return prodi2;
	}

	public void setProdi2(Jurusan prodi2) {
		this.prodi2 = prodi2;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang", nullable = false)
	public Jenjang getJenjang() {

		prodiLulus = getProdiLulus();
		prodi1 = getProdi1();

		if (jenjang == null) {
			jenjang = ConstantValues.s1;
		}

		if (prodiLulus != null && prodiLulus.getJenjang() != null) {
			jenjang = prodiLulus.getJenjang();
		} else if (prodi1 != null && prodi1.getJenjang() != null) {
			jenjang = prodi1.getJenjang();
		}

		jenjang = check(jenjang);

		return jenjang;
	}

	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	@Column(name = "status_lulus", length = 1)
	public Integer getStatusLulus() {
		if (getMundur()) {
			return 0;
		}
		if (getDitolak()) {
			return 0;
		}
		return statusLulus;
	}

	public void setStatusLulus(Integer statusLulus) {
		this.statusLulus = statusLulus;
	}

	public Jurusan ambilJurusan() {
		if (getProdiLulus() != null) {
			return getProdiLulus();
		} else {
			return getProdi1();
		}
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "prodi_lulus", nullable = true)
	public Jurusan getProdiLulus() {

		if (getMundur()) {
			return null;
		}

		if (getDitolak()) {
			return null;
		}

		mahasiswa = getMahasiswa();

		if (mahasiswa != null) {
			prodiLulus = mahasiswa.getJurusan();
		}

		prodiLulus = check(prodiLulus);
		gelombangPendaftaran = check(gelombangPendaftaran);
		prodi1 = check(prodi1);

		if (prodiLulus == null && gelombangPendaftaran != null && gelombangPendaftaran.getOtomatisDiterimaSaatDaftar()
				&& prodi1 != null) {
			prodiLulus = prodi1;
		}

		return prodiLulus;
	}

	public void setProdiLulus(Jurusan prodiLulus) {
		this.prodiLulus = prodiLulus;
	}

	@Column(name = "nim_generated", length = 1)
	public Integer getNimGenerated() {
		return nimGenerated;
	}

	public void setNimGenerated(Integer nimGenerated) {
		this.nimGenerated = nimGenerated;
	}

	public void setProgram(String program) {
		this.program = program;
	}

	@Column(name = "program", length = 255)
	public String getProgram() {

		gelombangPendaftaran = getGelombangPendaftaran();

		if (program == null || program.trim().isEmpty()) {
			if (gelombangPendaftaran != null) {
				program = gelombangPendaftaran.getProgram();
			}
		}

		if (program == null || program.trim().isEmpty()) {
			program = "Reguler";
		}

		if (gelombangPendaftaran != null && gelombangPendaftaran.getTidakBolehMemilihProgramLain()
				&& gelombangPendaftaran.getProgram() != null && !gelombangPendaftaran.getProgram().trim().isEmpty()) {
			program = gelombangPendaftaran.getProgram();
		}

		return program;
	}

	public void setJenisSeleksi(JenisSeleksi jenisSeleksi) {
		this.jenisSeleksi = jenisSeleksi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_seleksi", nullable = true)
	public JenisSeleksi getJenisSeleksi() {

		if (getJenisSeleksiDipilih() != null) {
			jenisSeleksi = getJenisSeleksiDipilih();
		} else {
			gelombangPendaftaran = getGelombangPendaftaran();
			if (jenisSeleksi == null && gelombangPendaftaran != null
					&& gelombangPendaftaran.getJenisSeleksi() != null) {
				jenisSeleksi = gelombangPendaftaran.getJenisSeleksi();
			}
		}

		jenisSeleksi = check(jenisSeleksi);

		return jenisSeleksi;
	}

	public void setNim(String nim) {
		this.nim = nim;
	}

	@Column(name = "nim", length = 255, nullable = true)
	public String getNim() {
		mahasiswa = getMahasiswa();
		if (mahasiswa != null) {
			nim = mahasiswa.getNim();
		} else {
			nim = null;
		}
		return nim;
	}

	public void setTanggalDaftar(Date tanggalDaftar) {
		this.tanggalDaftar = tanggalDaftar;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_daftar")
	public Date getTanggalDaftar() {
		return tanggalDaftar;
	}

	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	@Column(name = "tahun")
	public Integer getTahun() {
		String tahunAkademik = getTahunAkademik();
		String[] bagianTahun = tahunAkademik == null ? null : StringUtils.split(tahunAkademik, "/");
		if (bagianTahun != null && bagianTahun.length > 0 && bagianTahun[0] != null
				&& bagianTahun[0].trim().matches("[0-9]+")) {
			tahun = Integer.valueOf(bagianTahun[0].trim());
		}
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	public void setCetakKartu(Integer cetakKartu) {
		this.cetakKartu = cetakKartu;
	}

	@Column(name = "cetak_kartu")
	public Integer getCetakKartu() {
		String setCetakKartu = retreive("setCetakKartu");
		if (setCetakKartu != null && !setCetakKartu.isEmpty() && setCetakKartu.trim().equalsIgnoreCase("1")) {
			cetakKartu = 1;
		}
		return cetakKartu;
	}

	public void setIpk(String ipk) {
		this.ipk = ipk;
	}

	@Column(name = "ipk")
	public String getIpk() {
		return ipk;
	}

	public void setJurusanS1(String jurusanS1) {
		this.jurusanS1 = jurusanS1;
	}

	@Column(name = "jurusan_s1")
	public String getJurusanS1() {
		return jurusanS1;
	}

	public void setJurusanS2(String jurusanS2) {
		this.jurusanS2 = jurusanS2;
	}

	@Column(name = "jurusan_s2")
	public String getJurusanS2() {
		return jurusanS2;
	}

	public void setFile(String file) {
		this.file = file;
	}

	@Column(name = "file")
	public String getFile() {
		return file;
	}

	public void setKewarganegaraan_asli(String kewarganegaraan_asli) {
		this.kewarganegaraan_asli = kewarganegaraan_asli;
	}

	@Column(name = "kewarganegaraan_asli")
	public String getKewarganegaraan_asli() {
		return kewarganegaraan_asli;
	}

	public void setProgramNIM(String programNIM) {
		this.programNIM = programNIM;
	}

	@Column(name = "program_nim")
	public String getProgramNIM() {
		return programNIM;
	}

	public void setSemesterMulai(String semesterMulai) {
		this.semesterMulai = semesterMulai;
	}

	@Column(name = "semester_mulai")
	public String getSemesterMulai() {

		gelombangPendaftaran = getGelombangPendaftaran();

		if (gelombangPendaftaran != null) {
			semesterMulai = gelombangPendaftaran.getJenisSemester();
		}

		if (semesterMulai == null) {
			semesterMulai = Perkuliahan.GANJIL;
		}

		return semesterMulai;
	}

	public String getTahunAkademik() {
		gelombangPendaftaran = getGelombangPendaftaran();
		if (gelombangPendaftaran != null) {
			tahunAkademik = gelombangPendaftaran.getTahunAkademik();
		}
		return tahunAkademik;
	}

	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran", nullable = true)
	public GelombangPendaftaran getGelombangPendaftaran() {

		if (getGelombangPendaftaranDiterima() != null) {
			gelombangPendaftaran = getGelombangPendaftaranDiterima();
		} else {
			gelombangPendaftaran = check(gelombangPendaftaran);
		}

		return gelombangPendaftaran;
	}

	public void setGelombangPendaftaran(GelombangPendaftaran gelombangPendaftaran) {
		this.gelombangPendaftaran = gelombangPendaftaran;
		paketDivalidasiUntukGelombangId = -1L;
	}

	public Date getTanggalPendaftaran() {
		if (tanggalPendaftaran == null) {
			tanggalPendaftaran = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalPendaftaran;
	}

	public void setTanggalPendaftaran(Date tanggalPendaftaran) {
		this.tanggalPendaftaran = tanggalPendaftaran;
	}

	public String getFormatedtanggallahir() {
		if (getTanggalLahir() != null) {
			formatedtanggallahir = Common.dateFormat2.get().format(getTanggalLahir());
		}
		return formatedtanggallahir;
	}

	public void setFormatedtanggallahir(String formatedtanggallahir) {
		this.formatedtanggallahir = formatedtanggallahir;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "prodi3", nullable = true)
	public Jurusan getProdi3() {

		paket = getPaket();

		if (paket != null && paket.getJumlahProdiYgBolehDiambil() < 3) {
			prodi3 = null;
		}
		prodi3 = check(prodi3);
		return prodi3;
	}

	public void setProdi3(Jurusan prodi3) {
		this.prodi3 = prodi3;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "prodi4", nullable = true)
	public Jurusan getProdi4() {

		paket = getPaket();

		if (paket != null && paket.getJumlahProdiYgBolehDiambil() < 4) {
			prodi4 = null;
		}
		prodi4 = check(prodi4);
		return prodi4;
	}

	public void setProdi4(Jurusan prodi4) {
		this.prodi4 = prodi4;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "prodi5", nullable = true)
	public Jurusan getProdi5() {

		paket = getPaket();

		if (paket != null && paket.getJumlahProdiYgBolehDiambil() < 5) {
			prodi5 = null;
		}
		prodi5 = check(prodi5);
		return prodi5;
	}

	public void setProdi5(Jurusan prodi5) {
		this.prodi5 = prodi5;
	}

	public String getJenisSemester() {
		jenisSemester = getSemesterMulai();
		return jenisSemester;
	}

	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_registrasi", nullable = true)
	public Kegiatan getPembayaranRegistrasi() {
		return pembayaranRegistrasi;
	}

	public void setPembayaranRegistrasi(Kegiatan pembayaranRegistrasi) {
		this.pembayaranRegistrasi = pembayaranRegistrasi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_daftar_ulang", nullable = true)
	public Kegiatan getPembayaranDaftarUlang() {
		return pembayaranDaftarUlang;
	}

	public void setPembayaranDaftarUlang(Kegiatan pembayaranDaftarUlang) {
		this.pembayaranDaftarUlang = pembayaranDaftarUlang;
	}

	public Boolean getGenerateNimOtomatis() {
		if (generateNimOtomatis == null) {
			generateNimOtomatis = true;
		}
		return generateNimOtomatis;
	}

	public void setGenerateNimOtomatis(Boolean generateNimOtomatis) {
		this.generateNimOtomatis = generateNimOtomatis;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);

		if (mahasiswa != null && getId() != null) {
			mahasiswa.setBiodataCalonMahasiswa(getId());
			mahasiswa.setBiodataCalonMahasiswaData(this);
		}

		return mahasiswa;
	}

	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	public Boolean getAlamatSama() {
		if (alamatSama == null) {
			alamatSama = false;
		}
		return alamatSama;
	}

	public void setAlamatSama(Boolean alamatSama) {
		this.alamatSama = alamatSama;
	}

	public String getDusunCalon() {
		return dusunCalon;
	}

	public void setDusunCalon(String dusunCalon) {
		this.dusunCalon = dusunCalon;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "upload_biodata_calon_mahasiswa", nullable = true)
	public UploadBiodataCalonMahasiswa getUploadBiodataCalonMahasiswa() {
		uploadBiodataCalonMahasiswa = check(uploadBiodataCalonMahasiswa);
		return uploadBiodataCalonMahasiswa;
	}

	public void setUploadBiodataCalonMahasiswa(UploadBiodataCalonMahasiswa uploadBiodataCalonMahasiswa) {
		this.uploadBiodataCalonMahasiswa = uploadBiodataCalonMahasiswa;
	}

	public Boolean getPernyataan() {
		if (pernyataan == null) {
			pernyataan = false;
		}
		return pernyataan;
	}

	public void setPernyataan(Boolean pernyataan) {
		this.pernyataan = pernyataan;
	}

	@Column(name = "kecamatan_calon")
	public String getKecamatan_calon() {
		if (kecamatanCalon != null) {
			kecamatan_calon = kecamatanCalon.getNama();
		}
		return kecamatan_calon;
	}

	public void setKecamatan_calon(String kecamatan_calon) {
		this.kecamatan_calon = kecamatan_calon;
	}

	public Boolean getMerupakanPindahan() {
		if (merupakanPindahan == null) {
			merupakanPindahan = false;
		}
		return merupakanPindahan;
	}

	public void setMerupakanPindahan(Boolean merupakanPindahan) {
		this.merupakanPindahan = merupakanPindahan;
	}

	public String getPindahanDariKampus() {

		if (!getMerupakanPindahan()) {
			pindahanDariKampus = "";
		}

		if (getPindahanDari() != null) {
			pindahanDariKampus = getPindahanDari().getNama();
		}

		return pindahanDariKampus == null ? "" : pindahanDariKampus.trim();
	}

	public void setPindahanDariKampus(String pindahanDariKampus) {
		this.pindahanDariKampus = pindahanDariKampus;
	}

	public Integer getPindahDariKampusLamaDiSemester() {

		if (!getMerupakanPindahan()) {
			pindahDariKampusLamaDiSemester = 0;
		}

		if (pindahDariKampusLamaDiSemester == null) {
			pindahDariKampusLamaDiSemester = 0;
		}
		return pindahDariKampusLamaDiSemester;
	}

	public void setPindahDariKampusLamaDiSemester(Integer pindahDariKampusLamaDiSemester) {
		this.pindahDariKampusLamaDiSemester = pindahDariKampusLamaDiSemester;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalPindah() {
		if (!getMerupakanPindahan()) {
			tanggalPindah = null;
		} else if (tanggalPindah == null) {
			tanggalPindah = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalPindah;
	}

	public void setTanggalPindah(Date tanggalPindah) {
		this.tanggalPindah = tanggalPindah;
	}

	public String getKeteranganPindah() {
		try {
			if (!getMerupakanPindahan()) {
				keteranganPindah = "";
			}
			if (keteranganPindah == null) {
				keteranganPindah = "";
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return keteranganPindah;
	}

	public void setKeteranganPindah(String keteranganPindah) {
		this.keteranganPindah = keteranganPindah;
	}

	public String getNimLamaSebelumPindah() {
		return nimLamaSebelumPindah;
	}

	public void setNimLamaSebelumPindah(String nimLamaSebelumPindah) {
		this.nimLamaSebelumPindah = nimLamaSebelumPindah;
	}

	public String getInfoKampusDariMana() {
		if (infoKampusDariMana == null) {
			infoKampusDariMana = "";
		}
		if (!infoKampusDariMana.trim().isEmpty() && !infoKampusDariMana.startsWith(";")) {
			infoKampusDariMana = ";" + infoKampusDariMana;
		}
		if (!infoKampusDariMana.trim().isEmpty() && !infoKampusDariMana.endsWith(";")) {
			infoKampusDariMana = infoKampusDariMana + ";";
		}
		return infoKampusDariMana.trim().toLowerCase();
	}

	public void setInfoKampusDariMana(String infoKampusDariMana) {
		this.infoKampusDariMana = infoKampusDariMana;
	}

	public String getKeteranganInfoKampusDariMana() {
		return keteranganInfoKampusDariMana;
	}

	public void setKeteranganInfoKampusDariMana(String keteranganInfoKampusDariMana) {
		this.keteranganInfoKampusDariMana = keteranganInfoKampusDariMana;
	}

	public String getNamaTemanInfoKampusDariMana() {
		return namaTemanInfoKampusDariMana;
	}

	public void setNamaTemanInfoKampusDariMana(String namaTemanInfoKampusDariMana) {
		this.namaTemanInfoKampusDariMana = namaTemanInfoKampusDariMana;
	}

	public String getPindahanDariProdi() {
		if (pindahanDariProdi == null) {
			pindahanDariProdi = "";
		}
		return pindahanDariProdi;
	}

	public void setPindahanDariProdi(String pindahanDariProdi) {
		this.pindahanDariProdi = pindahanDariProdi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_mahasiswa", nullable = true)
	public StatusAwalMahasiswa getStatusAwalMahasiswa() {
		/*
		 * Status yang ditetapkan saat calon mahasiswa diterima adalah override eksplisit.
		 * Kolom baru tetap NULL untuk data lama, sehingga seluruh aturan turunan lama
		 * (kelompok, afiliasi, gelombang, pindahan, dan fallback Baru) tetap berlaku.
		 */
		StatusAwalMahasiswa statusDiterima = getStatusAwalDiterima();
		if (statusDiterima != null) {
			return statusDiterima;
		}

		afiliasiCalonMahasiswa = getAfiliasiCalonMahasiswa();
		kelompokCalonMahasiswa = getKelompokCalonMahasiswa();

		/*
		 * URUTAN PRIORITAS: KELOMPOK dulu, baru AFILIASI.
		 *
		 * Sebelumnya afiliasi diperiksa lebih dulu dan SELALU menang. Akibatnya calon yang
		 * sudah dipindah lewat "Ambil Data Calon Mahasiswa Manual" ke kelompok lain (mis.
		 * dari "Baru-Beasiswa Gratispol" ke "Baru") statusnya ditarik balik ke status
		 * afiliasi. Karena getter ini adalah properti Hibernate (@JoinColumn di bawah),
		 * nilai tarikan itu ikut DITULIS ULANG ke kolom status_awal_mahasiswa saat flush,
		 * sehingga calon tsb muncul lagi di kelompok asal lewat jalur OTOMATIS - dan jalur
		 * otomatis memang tidak punya tombol hapus (lihat KelompokCalonMahasiswaDetailAction
		 * .initCriteria: cabang otomatis mensyaratkan kelompokCalonMahasiswa IS NULL).
		 * Operator jadi terjebak: dipindah, balik lagi, dan tak bisa dihapus dari sana.
		 *
		 * Kelompok yang ditetapkan EKSPLISIT adalah keputusan operator, jadi harus menang.
		 * Afiliasi tetap dipakai bila calon belum masuk kelompok mana pun, atau bila
		 * kelompoknya sendiri tidak menentukan status awal.
		 */

		// FIX LazyInitializationException "could not initialize proxy - no Session": proxy
		// kelompokCalonMahasiswa/statusAwalMahasiswa-nya bisa diakses dari konteks TANPA sesi
		// Hibernate aktif (mis. laporan/report/thread setelah sesi pemuat aslinya sudah
		// ditutup -- lihat CariDataPesertaUjianAction.genInfo, DaftarUlangMahasiswaBaruAction.
		// onCariMahasiswa). Jangan biarkan seluruh halaman/laporan gagal hanya krn field
		// turunan ini -- tangkap & lewati, statusAwalMahasiswa jatuh ke fallback di bawah.
		StatusAwalMahasiswa statusAwalDariKelompok = null;
		if (kelompokCalonMahasiswa != null) {
			try {
				statusAwalDariKelompok = kelompokCalonMahasiswa.getStatusAwalMahasiswa();
				statusAwalMahasiswa = statusAwalDariKelompok;
			} catch (org.hibernate.LazyInitializationException lie) { ais.common.ErrorAuditUtil.record(lie, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:2443");
			}
		}

		if (statusAwalDariKelompok == null && afiliasiCalonMahasiswa != null
				&& afiliasiCalonMahasiswa.getStatusAwalMahasiswa() != null) {
			statusAwalMahasiswa = afiliasiCalonMahasiswa.getStatusAwalMahasiswa();
		} else {
			gelombangPendaftaran = getGelombangPendaftaran();

			if (statusAwalMahasiswa == null && kelompokCalonMahasiswa == null && gelombangPendaftaran != null) {
				statusAwalMahasiswa = gelombangPendaftaran.getStatusAwalMahasiswaDefault();
			}

			// Kelompok yang ditetapkan secara EKSPLISIT selalu menjadi acuan utama statusAwalMahasiswa.
			// Default gelombang (harusIkutStatusAwalDefault) hanya berlaku bila tidak ada kelompok
			// yang memberikan status — mencegah override yang menyebabkan mismatch di tampilan kelompok.
			if (statusAwalDariKelompok == null) {
				if (gelombangPendaftaran != null && gelombangPendaftaran.getHarusIkutStatusAwalDefault()
						&& gelombangPendaftaran.getStatusAwalMahasiswaDefault() != null) {
					statusAwalMahasiswa = gelombangPendaftaran.getStatusAwalMahasiswaDefault();
				} else if (statusAwalMahasiswa == null) {
					statusAwalMahasiswa = ConstantValues.BARU;
				}
			}

			if (statusAwalDariKelompok != null && statusAwalDariKelompok.getPindahan()) {
				statusAwalMahasiswa = statusAwalDariKelompok;
			} else if (getMerupakanPindahan()) {
				statusAwalMahasiswa = ConstantValues.PINDAHAN;
			}

		}

		// Normalisasi harus dijalankan untuk SEMUA jalur prioritas, termasuk saat
		// status berasal dari afiliasi. Data lama dapat memiliki kolom
		// status_awal_mahasiswa NULL tanpa kelompok, afiliasi, atau default gelombang;
		// pada kondisi tersebut tampilkan dan gunakan master "Baru" sebagai default.
		if (statusAwalMahasiswa == null) {
			statusAwalMahasiswa = ConstantValues.BARU;
		}
		statusAwalMahasiswa = check(statusAwalMahasiswa);
		return statusAwalMahasiswa;
	}

	public void setStatusAwalMahasiswa(StatusAwalMahasiswa statusAwalMahasiswa) {
		this.statusAwalMahasiswa = statusAwalMahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_awal_diterima", nullable = true)
	public StatusAwalMahasiswa getStatusAwalDiterima() {
		statusAwalDiterima = check(statusAwalDiterima);
		return statusAwalDiterima;
	}

	public void setStatusAwalDiterima(StatusAwalMahasiswa statusAwalDiterima) {
		this.statusAwalDiterima = statusAwalDiterima;
	}

	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}
		return parameterTambahan;
	}

	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	public String getPinPassword() {
		return pinPassword;
	}

	public void setPinPassword(String pinPassword) {
		this.pinPassword = pinPassword;
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:2518");

			}

			ParameterTambahan parameterTambahan = null;
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
				parameterTambahan = (ParameterTambahan) ConstantValues.ambil(ParameterTambahan.class.getName(), id);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:2527");

			}
			String valLama = val;
			if (parameterTambahan != null
					&& parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.PILIHAN_CUSTOM)) {
				String[] ss = StringUtils.split(parameterTambahan.getNilaiDataInputan(), ";");
				for (String s : ss) {
					String[] ssss = StringUtils.split(s, ":");

					if (ssss.length > 1 && ssss[1].equalsIgnoreCase(val)) {
						val = ssss[0];
						valLama = ssss[1];
					}

				}
			}

			KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa = null;
			Long idkel = 1L;
			try {
				idkel = value.length > 5 ? Long.parseLong(value[5].trim()) : 1L;
				kelompokParameterTambahanCalonMahasiswa = (KelompokParameterTambahanCalonMahasiswa) ConstantValues
						.ambil(KelompokParameterTambahanCalonMahasiswa.class.getName(), idkel);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:2551");

			}

			if (parameterTambahan != null && kelompokParameterTambahanCalonMahasiswa != null) {
				String jenis = kelompokParameterTambahanCalonMahasiswa.getId() + "->" + parameterTambahan.getId();

				if (parameterTambahan.getHarusMenyertakanLampiran()) {

					LampiranLain lam = LampiranLain.ambil(getId(), jenis);
					if (lam != null) {
						try {
							url = lam.createLinkUri(false);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}

				}
			}

			if (parameterTambahan != null
					&& (parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.ANGKA)
							|| parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.TEXT_ANGKA))
					&& val != null && val.toLowerCase().contains("e")) {
				try {
					double vala = Double.parseDouble(val);
					BigDecimal bigDecimal = new BigDecimal(vala);// form to BigDecimal
					val = bigDecimal.toString();// get the String value
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:2580");
					// TODO: handle exception
				}
			}

			System.out.println("namaCol=> " + namaCol + ", lbl=> " + lbl + ", val=> " + val + ", valLama -> " + valLama
					+ ", url=>" + url);

			String[] param = lbl.split("->");

			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setName5(param[0]);
			commonVO.setName3(valLama);
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
				KelompokParameterTambahanCalonMahasiswa kelompokParameterTambahanCalonMahasiswa = (KelompokParameterTambahanCalonMahasiswa) row
						.getAttribute("kelompokParameterTambahanCalonMahasiswa");
				if (parameterTambahan != null && kelompokParameterTambahanCalonMahasiswa != null) {
					String jenis = kelompokParameterTambahanCalonMahasiswa.getId() + "->" + parameterTambahan.getId();

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);
					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);
					String url = "";
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(getId(), jenis);
						if (lam != null) {
							try {
								url = lam.createLinkUri(false);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

					String s = kelompokParameterTambahanCalonMahasiswa.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanCalonMahasiswa.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanCalonMahasiswa.getId() + "->" + parameterTambahan.getId()
							+ "<=>" + val + "<=>" + url + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		// System.out.println("parameterTambahanStr => " + parameterTambahanStr);
		// System.out.println("parameterTambahanInds => " + parameterTambahanInds);
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_calon_mahasiswa", nullable = true)
	public KelompokCalonMahasiswa getKelompokCalonMahasiswa() {
		kelompokCalonMahasiswa = check(kelompokCalonMahasiswa);
		return kelompokCalonMahasiswa;
	}

	public void setKelompokCalonMahasiswa(KelompokCalonMahasiswa kelompokCalonMahasiswa) {
		this.kelompokCalonMahasiswa = kelompokCalonMahasiswa;
	}

	public String getNoTelpSekolah() {
		return noTelpSekolah;
	}

	public void setNoTelpSekolah(String noTelpSekolah) {
		this.noTelpSekolah = noTelpSekolah;
	}

	public Integer getTotalSkor() {
		totalSkor = 0;
		if (!getParameterTambahan().isEmpty()) {
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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:2694");

				}

				if (parameterTambahan != null
						&& parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.PILIHAN_CUSTOM)) {
					// Data tersimpan bisa berisi teks non-angka murni (mis. "1. 450 Watt" atau
					// "KIP Kuliah (Memiliki KIP/PKH/KKS/KJP)") pada kolom skor - method ini
					// dipanggil Hibernate di setiap flush/dirty-check & ManajemenProperty.insertProperty,
					// jadi setiap parseInt per-field DIVALIDASI/di-guard sendiri agar 1 field
					// rusak tidak menggagalkan total perhitungan skor field lain. Kalau teks
					// diawali kode angka (mis. "1." pada "1. 450 Watt") kode itu yang dipakai;
					// kalau tidak ada angka sama sekali, skor default 0.
					String[] kol = StringUtils.split(val, ":");
					if (kol.length > 1) {
						String kolSkor = kol[1].trim();
						if (kolSkor.matches("-?\\d+")) {
							try {
								skor = Integer.parseInt(kolSkor);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:2704");

							}
						} else {
							skor = ekstrakSkorDariTeks(kolSkor);
						}
					} else {
						String valSkor = val.trim();
						if (valSkor.matches("-?\\d+")) {
							try {
								skor = Integer.parseInt(valSkor);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:2710");

							}
						} else {
							skor = ekstrakSkorDariTeks(valSkor);
						}
					}
				}
				totalSkor += skor;
			}
		}
		return totalSkor;
	}

	/**
	 * Ekstrak kode angka pertama di dalam teks jawaban PILIHAN_CUSTOM yang tidak
	 * murni numerik, mis. "1. 450 Watt" -> 1 (kode pilihan sebelum karakter
	 * non-digit pertama). Teks bisa DIAWALI karakter unicode non-ASCII (mis.
	 * simbol pembanding "≥ 8,50") - jangan hanya cek posisi 0, cari kemunculan
	 * digit pertama DI MANA SAJA dalam teks lalu ambil rangkaian digitnya.
	 * Kalau teks tidak mengandung angka sama sekali (mis.
	 * "KIP Kuliah (Memiliki KIP/PKH/KKS/KJP)") kembalikan 0 - JANGAN lempar
	 * exception, dipanggil di jalur Hibernate flush/insertProperty.
	 */
	/* OPTIMASI FASE 9: pola regex dikompilasi SEKALI (Pattern immutable & thread-safe),
	 * bukan tiap kali method dipanggil. Matcher tetap dibuat per panggilan karena
	 * Matcher TIDAK thread-safe. */
	private static final java.util.regex.Pattern POLA_ANGKA = java.util.regex.Pattern.compile("-?\\d+");

	private Integer ekstrakSkorDariTeks(String teks) {
		if (teks == null) {
			return 0;
		}
		String t = teks.trim();
		try {
			java.util.regex.Matcher m = POLA_ANGKA.matcher(t);
			if (m.find()) {
				return Integer.parseInt(m.group());
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:ekstrakSkorDariTeks");
		}
		return 0;
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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:2737");

				}

				if (parameterTambahan != null && parameterTambahan.getId().equals(parameterTambahanData.getId())
						&& parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.PILIHAN_CUSTOM)) {
					// lihat catatan defensif di getTotalSkor(): teks non-angka murni (mis.
					// "1. 450 Watt" / "KIP Kuliah (...)") tidak boleh menggagalkan seluruh
					// perhitungan skor field lain.
					String[] kol = StringUtils.split(val, ":");
					if (kol.length > 1) {
						String kolSkor = kol[1].trim();
						if (kolSkor.matches("-?\\d+")) {
							try {
								skor = Integer.parseInt(kolSkor);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:2747");

							}
						} else {
							skor = ekstrakSkorDariTeks(kolSkor);
						}
					} else {
						String valSkor = val.trim();
						if (valSkor.matches("-?\\d+")) {
							try {
								skor = Integer.parseInt(valSkor);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:2753");

							}
						} else {
							skor = ekstrakSkorDariTeks(valSkor);
						}
					}
				}
				totalSkor += skor;
			}
		}
		return totalSkor;
	}

	public void setTotalSkor(Integer totalSkor) {
		this.totalSkor = totalSkor;
	}

	public Boolean getTelahLogin() {

		telahLogin = getWaktuLogin() != null;
		return telahLogin;
	}

	public void setTelahLogin(Boolean telahLogin) {
		this.telahLogin = telahLogin;
	}

	public Date getWaktuLogin() {
		try {
			String login_terakhir = retreive("login_terakhir");
			if (login_terakhir != null && !login_terakhir.trim().isEmpty()) {
				waktuLogin = Common.dateFormat9.get().parse(login_terakhir);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/BiodataCalonMahasiswa.java:2785");
		}
		return waktuLogin;
	}

	public void setWaktuLogin(Date waktuLogin) {
		this.waktuLogin = waktuLogin;
	}

	public String getGelar() {
		return gelar;
	}

	public void setGelar(String gelar) {
		this.gelar = gelar;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getKk() {
		return kk;
	}

	public void setKk(String kk) {
		this.kk = kk;
	}

	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			BiodataCalonMahasiswa o = (BiodataCalonMahasiswa) arg0;
			return noRegistrasi.compareTo(o.noRegistrasi);
		} catch (Exception e) {
			return super.compareTo(arg0);
		}
	}

	@Column(name = "nomor_surat_kelulusan")
	public String getNomorSuratKelulusan() {
		return nomorSuratKelulusan;
	}

	public void setNomorSuratKelulusan(String nomorSuratKelulusan) {
		this.nomorSuratKelulusan = nomorSuratKelulusan;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalSuratKelulusan() {
		return tanggalSuratKelulusan;
	}

	public void setTanggalSuratKelulusan(Date tanggalSuratKelulusan) {
		this.tanggalSuratKelulusan = tanggalSuratKelulusan;
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

	public Boolean getDitolak() {
		return ditolak == null ? false : ditolak;
	}

	public void setDitolak(Boolean ditolak) {
		this.ditolak = ditolak;
	}

	@Column(name = "udah_baru")
	public Boolean getUdah() {
		return udah == null ? false : udah;
	}

	public void setUdah(Boolean udah) {
		this.udah = udah;
	}

	@Column(columnDefinition = "text")
	public String getJurusanSekolahLain() {
		return jurusanSekolahLain;
	}

	public void setJurusanSekolahLain(String jurusanSekolahLain) {
		this.jurusanSekolahLain = jurusanSekolahLain;
	}

	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan.trim();
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public Boolean getMundur() {
		return mundur == null ? false : mundur;
	}

	public void setMundur(Boolean mundur) {
		this.mundur = mundur;
	}

	public String getNisn() {
		return nisn == null ? "" : nisn.trim();
	}

	public void setNisn(String nisn) {
		this.nisn = nisn;
	}

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

	public void tampilkanHp(Component vbox) {
		try {

			String hp = getHp();
			String telp = getTeleponRumah();

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
			String hp = getHp();
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pindahan_dari")
	public PerguruanTinggiLain getPindahanDari() {
		pindahanDari = check(pindahanDari);
		return pindahanDari;
	}

	public void setPindahanDari(PerguruanTinggiLain pindahanDari) {
		this.pindahanDari = pindahanDari;
	}

	public String getInstansiAsal() {
		return instansiAsal == null ? "" : instansiAsal.trim();
	}

	public void setInstansiAsal(String instansiAsal) {
		this.instansiAsal = instansiAsal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota_instansi", nullable = true)
	public Wilayah getKotaInstansi() {
		kotaInstansi = check(kotaInstansi);
		return kotaInstansi;
	}

	public void setKotaInstansi(Wilayah kotaInstansi) {
		this.kotaInstansi = kotaInstansi;
	}

	public String getJabatanDiInstansiAsal() {
		return jabatanDiInstansiAsal == null ? "" : jabatanDiInstansiAsal.trim();
	}

	public void setJabatanDiInstansiAsal(String jabatanDiInstansiAsal) {
		this.jabatanDiInstansiAsal = jabatanDiInstansiAsal;
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
	@JoinColumn(name = "pendidikan_orang_tua_ibu", nullable = true)
	public PendidikanOrangTua getPendidikanOrtuIbu() {
		pendidikanOrtuIbu = check(pendidikanOrtuIbu);
		return pendidikanOrtuIbu;
	}

	public void setPendidikanOrtuIbu(PendidikanOrangTua pendidikanOrtuIbu) {
		this.pendidikanOrtuIbu = pendidikanOrtuIbu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pekerjaan_orang_tua_ibu", nullable = true)
	public PekerjaanOrangTua getPekerjaanAyahIbu() {
		pekerjaanAyahIbu = check(pekerjaanAyahIbu);
		return pekerjaanAyahIbu;
	}

	public void setPekerjaanAyahIbu(PekerjaanOrangTua pekerjaanAyahIbu) {
		this.pekerjaanAyahIbu = pekerjaanAyahIbu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendapatan_ortu_wali", nullable = true)
	public PendapatanOrangTua getPendapatanOrtuWali() {
		pendapatanOrtuWali = check(pendapatanOrtuWali);
		return pendapatanOrtuWali;
	}

	public void setPendapatanOrtuWali(PendapatanOrangTua pendapatanOrtuWali) {
		this.pendapatanOrtuWali = pendapatanOrtuWali;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_orang_tua_wali", nullable = true)
	public PendidikanOrangTua getPendidikanOrtuWali() {
		pendidikanOrtuWali = check(pendidikanOrtuWali);
		return pendidikanOrtuWali;
	}

	public void setPendidikanOrtuWali(PendidikanOrangTua pendidikanOrtuWali) {
		this.pendidikanOrtuWali = pendidikanOrtuWali;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pekerjaan_orang_tua_wali", nullable = true)
	public PekerjaanOrangTua getPekerjaanAyahWali() {
		pekerjaanAyahWali = check(pekerjaanAyahWali);
		return pekerjaanAyahWali;
	}

	public void setPekerjaanAyahWali(PekerjaanOrangTua pekerjaanAyahWali) {
		this.pekerjaanAyahWali = pekerjaanAyahWali;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "afiliasi_calon_mahasiswa", nullable = true)
	public AfiliasiCalonMahasiswa getAfiliasiCalonMahasiswa() {
		afiliasiCalonMahasiswa = check(afiliasiCalonMahasiswa);
		return afiliasiCalonMahasiswa;
	}

	public void setAfiliasiCalonMahasiswa(AfiliasiCalonMahasiswa afiliasiCalonMahasiswa) {
		this.afiliasiCalonMahasiswa = afiliasiCalonMahasiswa;
	}

	@Column(columnDefinition = "text")
	public String getRincianSkor() {
		rincianSkor = "";
		totalSkor = 0;
		try {
			if (!getParameterTambahan().isEmpty()) {
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
						parameterTambahan = (ParameterTambahan) ConstantValues.ambil(ParameterTambahan.class.getName(),
								id);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:3122");

					}

					if (parameterTambahan != null
							&& parameterTambahan.getTipeDataInputan().equals(ParameterTambahan.PILIHAN_CUSTOM)) {
						String[] kol = StringUtils.split(val, ":");
						if (kol.length > 1) {
							// Data tersimpan bisa berisi teks non-angka (mis. "S") pada kolom skor -
							// getter ini dipanggil Hibernate di setiap flush/audit entity, jadi
							// cek format angka DULU sebelum parseInt agar data rusak tidak memicu
							// exception rutin (bisa membatalkan transaksi flush). Default skor=0.
							String kolSkor = kol[1].trim();
							if (kolSkor.matches("-?\\d+")) {
								try {
									skor = Integer.parseInt(kolSkor);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:3132");

								}
							}
						} else {
							String valSkor = val.trim();
							if (valSkor.matches("-?\\d+")) {
								try {
									skor = Integer.parseInt(valSkor);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:3138");

								}
							}
						}

						String s = parameterTambahan.getLabelInputan() + "," + skor;
						rincianSkor += rincianSkor.isEmpty() ? s : ";" + s;
					}
					totalSkor += skor;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:3149");
			// TODO: handle exception
		}
		return rincianSkor;
	}

	public void setRincianSkor(String rincianSkor) {
		this.rincianSkor = rincianSkor;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalDiterima() {
		if (getProdiLulus() == null) {
			tanggalDiterima = null;
		} else if (tanggalDiterima == null) {
			tanggalDiterima = getTanggal_dirubah();
		}
		return tanggalDiterima;
	}

	public void setTanggalDiterima(Date tanggalDiterima) {
		this.tanggalDiterima = tanggalDiterima;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_jenis_seleksi", nullable = true)
	public KelompokJenisSeleksi getKelompokJenisSeleksi() {
		kelompokJenisSeleksi = check(kelompokJenisSeleksi);

		if (getJenisSeleksi() != null && getJenisSeleksi().getKelompokJenisSeleksi() != null) {
			kelompokJenisSeleksi = getJenisSeleksi().getKelompokJenisSeleksi();
		}

		return kelompokJenisSeleksi;
	}

	public void setKelompokJenisSeleksi(KelompokJenisSeleksi kelompokJenisSeleksi) {
		this.kelompokJenisSeleksi = kelompokJenisSeleksi;
	}

	public String urlLogin() throws Exception {
		String url = Common.getRequestHostWithProtocol();

		if (Common.bolehKonfigurasi("login_via_link_menggunakan_domain_masing_masing", Konfigurasi.TIDAK_AKTIF)) {

			Jurusan jurusan = getProdiLulus();
			if (jurusan == null) {
				jurusan = getProdi1();
			}

			HttpServletRequest request = null;
			if (ExecutionsCtrl.getCurrent() != null) {
				request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
			}

			if (request == null) {
				request = RequestContext.get();
			}
			url = "https://" + jurusan.getFakultas().getPerguruanTinggi().getDomain() + "/" + request.getContextPath();
		}

		String code = url + "/m?q=" + URLEncoder.encode(
				Common.desEncrypter.get().encrypt(getId() + "-BiodataCalonMahasiswa-abcdefghijklmnopqrstuvwxyz"), "UTF-8");
		return code;
	}

	public String ambilHp() {
		try {
			String hp = getHp();
			String telp = getTeleponRumah();

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

			String hp = getHp();
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_seleksi_pilih", nullable = true)
	public JenisSeleksi getJenisSeleksiDipilih() {
		if (getProdiLulus() == null) {
			jenisSeleksiDipilih = null;
		} else {
			jenisSeleksiDipilih = check(jenisSeleksiDipilih);
		}
		return jenisSeleksiDipilih;
	}

	public void setJenisSeleksiDipilih(JenisSeleksi jenisSeleksiDipilih) {
		this.jenisSeleksiDipilih = jenisSeleksiDipilih;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_diterima", nullable = true)
	public GelombangPendaftaran getGelombangPendaftaranDiterima() {
		if (getProdiLulus() == null) {
			gelombangPendaftaranDiterima = null;
		} else {
			gelombangPendaftaranDiterima = check(gelombangPendaftaranDiterima);
		}
		return gelombangPendaftaranDiterima;
	}

	public void setGelombangPendaftaranDiterima(GelombangPendaftaran gelombangPendaftaranDiterima) {
		this.gelombangPendaftaranDiterima = gelombangPendaftaranDiterima;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putPhoto(Map parameters) {
		try {
			BiodataCalonMahasiswa calonMahasiswa = this;

			FileFotoLain fotobiodataCalonMahasiswa = FileFotoLain.ambil(calonMahasiswa.getId(),
					FotoBiodataCalonMahasiswa.DEFAULT_JENIS, FotoBiodataCalonMahasiswa.class);

			if (fotobiodataCalonMahasiswa != null && fotobiodataCalonMahasiswa.ambilFile() != null) {
				parameters.put("foto", fotobiodataCalonMahasiswa.ambilFile().getAbsolutePath());
			} else if (fotobiodataCalonMahasiswa != null && fotobiodataCalonMahasiswa.getLink() != null
					&& fotobiodataCalonMahasiswa.getLink().toLowerCase().contains("dropbox")) {
				parameters.put("foto", fotobiodataCalonMahasiswa.dropboxLinkRaw());
			} else if (fotobiodataCalonMahasiswa != null && fotobiodataCalonMahasiswa.getGdrive() != null) {
				parameters.put("foto", fotobiodataCalonMahasiswa.exportGDriveUrl());
			} else if (fotobiodataCalonMahasiswa != null) {
				parameters.put("foto", fotobiodataCalonMahasiswa.createLinkUri());
			} else {
				File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
				parameters.put("foto", file.getAbsolutePath());
			}

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/BiodataCalonMahasiswa.java:3318");
		}
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "afiliasi_mahasiswa", nullable = true)
	public Mahasiswa getAfiliasiMahasiswa() {
		afiliasiMahasiswa = check(afiliasiMahasiswa);
		return afiliasiMahasiswa;
	}

	public void setAfiliasiMahasiswa(Mahasiswa afiliasiMahasiswa) {
		this.afiliasiMahasiswa = afiliasiMahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "afiliasi_pegawai", nullable = true)
	public Pegawai getAfiliasiPegawai() {
		afiliasiPegawai = check(afiliasiPegawai);
		return afiliasiPegawai;
	}

	public void setAfiliasiPegawai(Pegawai afiliasiPegawai) {
		this.afiliasiPegawai = afiliasiPegawai;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPembayaranRegistrasi() {
		if (pembayaranRegistrasi != null) {
			tanggalPembayaranRegistrasi = pembayaranRegistrasi.getTanggalBayarAwal();
		} else {
			tanggalPembayaranRegistrasi = null;
		}
		return tanggalPembayaranRegistrasi;
	}

	public void setTanggalPembayaranRegistrasi(Date tanggalPembayaranRegistrasi) {
		this.tanggalPembayaranRegistrasi = tanggalPembayaranRegistrasi;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPembayaranDaftarUlang() {
		if (pembayaranDaftarUlang != null) {
			tanggalPembayaranDaftarUlang = pembayaranDaftarUlang.getTanggalBayarAwal();
		} else {
			tanggalPembayaranDaftarUlang = null;
		}
		return tanggalPembayaranDaftarUlang;
	}

	public void setTanggalPembayaranDaftarUlang(Date tanggalPembayaranDaftarUlang) {
		this.tanggalPembayaranDaftarUlang = tanggalPembayaranDaftarUlang;
	}

	public void setTanggalMasuk(Date tanggalMasuk) {
		this.tanggalMasuk = tanggalMasuk;
	}

	@Column(name = "tanggal_masuk")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalMasuk() {
		if (getMahasiswa() != null) {
			tanggalMasuk = getMahasiswa().getTanggalMasuk();
		}
		return tanggalMasuk;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "konsentrasi", nullable = true)
	public Konsentrasi getKonsentrasi() {
		konsentrasi = check(konsentrasi);
		return konsentrasi;
	}

	public void setKonsentrasi(Konsentrasi konsentrasi) {
		this.konsentrasi = konsentrasi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa_alumni", nullable = true)
	public Mahasiswa getMahasiswaAlumni() {
		mahasiswaAlumni = check(mahasiswaAlumni);
		return mahasiswaAlumni;
	}

	public void setMahasiswaAlumni(Mahasiswa mahasiswaAlumni) {
		this.mahasiswaAlumni = mahasiswaAlumni;
	}

	public String getBahasa() {
		return bahasa;
	}

	public void setBahasa(String bahasa) {
		this.bahasa = bahasa;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	public Kegiatan chekPembayaranRegistrasi(Session session) {
		return prosesPembayaran(session, this.getPembayaranRegistrasi(), ConstantUtil.PENDAFTARAN_CALON_MAHASISWA, 0,
				true);
	}

	public Kegiatan chekPembayaranRegistrasi() {
		return prosesPembayaran(null, this.getPembayaranRegistrasi(), ConstantUtil.PENDAFTARAN_CALON_MAHASISWA, 0,
				true);
	}

	public Kegiatan chekPembayaranDaftarUlang(Session session) {
		return prosesPembayaran(session, this.getPembayaranDaftarUlang(), ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU,
				1, false);
	}

	public Kegiatan chekPembayaranDaftarUlang() {
		return prosesPembayaran(null, this.getPembayaranDaftarUlang(), ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU, 1,
				false);
	}

	/**
	 * Helper Method: Menangani logika pembayaran secara terpusat untuk menghindari
	 * duplikasi kode (DRY) dan memastikan manajemen memory / Hibernate Session
	 * ditangani secara efisien dan aman.
	 */
	private Kegiatan prosesPembayaran(Session sessionArg, Kegiatan kegiatanSaatIni, String constantJenis, int statusInt,
			boolean isRegistrasi) {
		// Pengecekan awal paling ringan (Memory-level), tidak memakan proses I/O DB
		if (kegiatanSaatIni != null && kegiatanSaatIni.getId() != null) {
			return kegiatanSaatIni;
		}

		JenisKegiatan jenisKegiatan = CommonPMB.pembayaranUtil.generateJenisKegiatan(constantJenis);
		Kegiatan kegiatan = this.ambilKegiatans(null, jenisKegiatan);

		Session session = sessionArg;
		boolean isOwnSession = false;
		org.hibernate.Transaction tx = null;
		boolean isOwnTx = false;

		try {
			// 1. Jika session tidak di-passing dari luar, buka session baru
			if (session == null) {
				session = HibernateUtil.getSessionFactory().openSession();
				isOwnSession = true;
			}

			// 2. Amankan transaksi: Cek apakah sudah ada transaksi aktif dari session
			// sebelumnya
			tx = session.getTransaction();
			if (tx == null || !tx.isActive()) {
				tx = session.beginTransaction();
				isOwnTx = true; // Tandai bahwa method ini yang menjadi "pemilik" transaksi
			}

			// 3. Panggil helper untuk pengecekan data
			if (kegiatan == null || kegiatan.getId() == null) {
				kegiatan = KegiatanHelper.checkKegiatanCalonMahasiswa(jenisKegiatan, this, statusInt,
						this.getTahunAkademik(), true, false, null, session);
			}

			if (kegiatan != null && this.getId() != null) {
				if (kegiatan.getId() == null) {
					session.save(kegiatan);
				}

				// Tentukan nama field secara dinamis untuk query HQL
				String fieldName = isRegistrasi ? "pembayaranRegistrasi" : "pembayaranDaftarUlang";

				// Menggunakan HQL UPDATE untuk menghindari Error 1664 Columns & Memory Leak
				String hql = "UPDATE BiodataCalonMahasiswa b SET b." + fieldName + " = :kegiatan WHERE b.id = :id";
				session.createQuery(hql).setParameter("kegiatan", kegiatan).setParameter("id", this.getId())
						.executeUpdate();

				// Set kembali ke local object supaya ke depannya tidak perlu hit query lagi
				if (isRegistrasi) {
					this.setPembayaranRegistrasi(kegiatan);
				} else {
					this.setPembayaranDaftarUlang(kegiatan);
				}
			}

			// 4. Commit HANYA jika kita yang memulai transaksi ini DAN transaksi masih
			// aktif
			// Ini adalah kunci untuk mencegah error "Transaction not successfully started"
			if (isOwnTx && tx != null && tx.isActive()) {
				tx.commit();
			}

		} catch (Exception e) {
			// 5. Rollback HANYA jika transaksi aktif dan kita yang memulainya
			if (isOwnTx && tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) {
					System.err.println("Gagal melakukan rollback: " + ex.getMessage());
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/BiodataCalonMahasiswa.java:3548");
		} finally {
			// 6. Tutup session HANYA jika kita yang membukanya di method ini
			if (isOwnSession && session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception ex) {
					System.err.println("Gagal menutup session: " + ex.getMessage());
				}
			}
		}

		return kegiatan;
	}
	
	
	
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_pmb")
	public KelasPmb getKelasPmb() {
		try {
			kelasPmb = check(kelasPmb);
		}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:3570");
			// TODO: handle exception
		}
		return kelasPmb;
	}
 
	public void setKelasPmb(KelasPmb kelasPmb) {
		this.kelasPmb = kelasPmb;
	}
}
