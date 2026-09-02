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

	/**
	 * Kumpulan {@link Fakultas} yang tersentuh oleh pilihan program studi pendaftar ini, yaitu
	 * fakultas dari {@code prodi1}..{@code prodi5} DITAMBAH fakultas dari {@link #getProdiLulus()}.
	 *
	 * <p>Dipakai untuk penyaringan hak akses/laporan per fakultas: satu pendaftar bisa memilih
	 * prodi lintas fakultas, jadi ia harus terlihat oleh operator fakultas mana pun yang
	 * bersangkutan. Hasilnya {@link HashSet} sehingga fakultas yang sama tidak berulang.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil seluruh getter prodi, yang masing-masing bisa
	 * mengosongkan prodi ketika {@link Paket} membatasi jumlah pilihan (lihat
	 * {@link #getProdi1()}).</p>
	 *
	 * @return himpunan fakultas (mungkin kosong, tidak pernah {@code null}).
	 */
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

	/**
	 * Versi "hanya id" dari {@link #populatePilihanFakultas()} — mengembalikan id fakultas, bukan
	 * objeknya, supaya bisa langsung dipakai pada klausa {@code IN (...)} query tanpa menahan
	 * objek entity di memori.
	 *
	 * @return himpunan id fakultas (mungkin kosong, tidak pernah {@code null}).
	 */
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

	/**
	 * Daftar program studi yang dipilih pendaftar ini: {@code prodi1}..{@code prodi5} ditambah
	 * {@link #getProdiLulus()}, tanpa duplikat dan tanpa {@code null}.
	 *
	 * <p>Berbeda dengan {@link #populatePilihanFakultas()}, hasilnya {@link ArrayList} sehingga
	 * URUTAN PILIHAN dipertahankan (pilihan 1 lebih dulu daripada pilihan 2, dst.) — urutan itu
	 * bermakna dalam seleksi.</p>
	 *
	 * @return daftar prodi terurut sesuai prioritas pilihan (mungkin kosong, tidak pernah
	 *         {@code null}).
	 */
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

	/**
	 * Versi "hanya id" dari {@link #populatePilihanJurusan()}, urutan prioritas pilihan tetap
	 * dipertahankan.
	 *
	 * @return daftar id prodi (mungkin kosong, tidak pernah {@code null}).
	 */
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

	/**
	 * Menambahkan fakultas milik {@code jurusan} ke dalam {@code fakultas} bila semuanya tidak
	 * {@code null}. Helper untuk {@link #populatePilihanFakultas()}.
	 *
	 * <p>Seluruh badan method dibungkus {@code try/catch}: {@code jurusan.getFakultas()} bisa
	 * melempar {@code LazyInitializationException} bila prodi berupa proxy Hibernate yang sesinya
	 * sudah ditutup. Kegagalan satu pilihan sengaja hanya dilewati (dicatat ke
	 * {@code ErrorAuditUtil}) supaya pilihan lain tetap terkumpul.</p>
	 *
	 * @param fakultas himpunan tujuan; diabaikan bila {@code null}.
	 * @param jurusan  prodi sumber; diabaikan bila {@code null} atau tanpa fakultas.
	 */
	private static void addFakultas(Set<Fakultas> fakultas, Jurusan jurusan) {
		try {
			if (fakultas != null && jurusan != null && jurusan.getFakultas() != null) {
				fakultas.add(jurusan.getFakultas());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:387");
		}
	}

	/**
	 * Varian id dari {@link #addFakultas(Set, Jurusan)} — menambahkan id fakultas milik
	 * {@code jurusan}. Helper untuk {@link #populatePilihanFakultasIds()}.
	 *
	 * @param fakultasIds himpunan id tujuan; diabaikan bila {@code null}.
	 * @param jurusan     prodi sumber; diabaikan bila {@code null}, tanpa fakultas, atau
	 *                    fakultasnya belum punya id.
	 */
	private static void addFakultasId(Set<Long> fakultasIds, Jurusan jurusan) {
		try {
			if (fakultasIds != null && jurusan != null && jurusan.getFakultas() != null
					&& jurusan.getFakultas().getId() != null) {
				fakultasIds.add(jurusan.getFakultas().getId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswa.java:397");
		}
	}

	/**
	 * Menambahkan {@code jurusan} ke {@code jurusans} bila belum ada di dalamnya (uji duplikat
	 * memakai {@code equals} berbasis id dari {@link ais.database.model.GeneralValueObject}).
	 * Helper untuk {@link #populatePilihanJurusan()}.
	 *
	 * @param jurusans daftar tujuan; diabaikan bila {@code null}.
	 * @param jurusan  prodi yang hendak ditambahkan; diabaikan bila {@code null} atau sudah ada.
	 */
	private static void addJurusan(List<Jurusan> jurusans, Jurusan jurusan) {
		if (jurusans != null && jurusan != null && !jurusans.contains(jurusan)) {
			jurusans.add(jurusan);
		}
	}

	/**
	 * Varian id dari {@link #addJurusan(List, Jurusan)}. Helper untuk
	 * {@link #populatePilihanJurusanIds()}.
	 *
	 * @param jurusanIds daftar id tujuan; diabaikan bila {@code null}.
	 * @param jurusan    prodi sumber; diabaikan bila {@code null}, belum punya id, atau id-nya
	 *                   sudah ada di daftar.
	 */
	private static void addJurusanId(List<Long> jurusanIds, Jurusan jurusan) {
		if (jurusanIds != null && jurusan != null && jurusan.getId() != null && !jurusanIds.contains(jurusan.getId())) {
			jurusanIds.add(jurusan.getId());
		}
	}

	/**
	 * Menghitung jumlah pendaftar aktif yang memakai kombinasi paket + prodi pada
	 * PaketJurusanPmb. Digunakan untuk validasi kuota PMB.
	 *
	 * <p>Kriteria pencacahan (semuanya lewat Hibernate {@link Criteria} dengan
	 * {@code Projections.rowCount()}, jadi tidak ada entity yang dimuat ke memori):</p>
	 * <ul>
	 * <li>{@code aktif} bernilai {@code true} ATAU masih {@code NULL} (data lama sebelum kolom ini
	 * ada dianggap aktif);</li>
	 * <li>{@code ditolak} dan {@code mundur} bernilai {@code false} atau {@code NULL} — pendaftar
	 * yang ditolak/mengundurkan diri tidak lagi memakan kuota;</li>
	 * <li>{@code paket} sama persis dengan paket pada {@code paketJurusanPmb};</li>
	 * <li>{@code tahunAkademik} sama, bila argumennya diisi;</li>
	 * <li>{@code gelombangPendaftaran} sama, HANYA bila
	 * {@code paketJurusanPmb.getKuotaBerlakuPerGelombang()} bernilai true (kuota per gelombang) —
	 * kalau tidak, kuota dihitung lintas gelombang;</li>
	 * <li>prodi pada {@code paketJurusanPmb} muncul di SALAH SATU dari {@code prodi1}..{@code prodi5}
	 * atau {@code prodiLulus} (OR berantai).</li>
	 * </ul>
	 *
	 * <p>Cache Hibernate sengaja dimatikan ({@code setCacheable(false)}) karena angka ini dipakai
	 * untuk keputusan kuota yang harus melihat kondisi terkini.</p>
	 *
	 * @param session           session Hibernate yang dipakai; bila {@code null} dipakai
	 *                          {@code HibernateUtil.currentSession()}. Session TIDAK ditutup oleh
	 *                          method ini.
	 * @param paketJurusanPmb   kombinasi paket+prodi yang kuotanya diperiksa; bila {@code null}
	 *                          atau paket/jurusannya {@code null} langsung mengembalikan 0.
	 * @param gelombangPendaftaran gelombang yang sedang diproses; hanya dipakai bila kuota berlaku
	 *                          per gelombang.
	 * @param tahunAkademik     tahun akademik penyaring; boleh {@code null}/kosong untuk lintas
	 *                          tahun.
	 * @param abaikanBiodataId  id baris yang TIDAK ikut dihitung — dipakai saat menyunting
	 *                          pendaftar yang sudah tersimpan agar dirinya sendiri tidak dianggap
	 *                          memakan kuota; boleh {@code null}.
	 * @return jumlah pendaftar yang memakan kuota tersebut; 0 bila terjadi kegagalan apa pun
	 *         (kesalahan hanya ditampilkan kepada admin lewat {@code Common.tampilErrorJikaAdmin}).
	 * @see #kuotaPaketJurusanMasihTersedia(Session, PaketJurusanPmb, GelombangPendaftaran, String,
	 *      Long)
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

	/**
	 * Apakah kuota untuk kombinasi paket + prodi tertentu MASIH tersedia (jumlah pendaftar saat ini
	 * masih di bawah kuota)?
	 *
	 * <p>Kebijakan "tidak dibatasi": bila {@code paketJurusanPmb} {@code null}, atau kuotanya
	 * {@code null}/&le;&nbsp;0, method ini mengembalikan {@code true} — artinya <b>kuota kosong
	 * berarti tanpa batas, bukan nol</b>. Jangan dibalik logikanya.</p>
	 *
	 * @param session           session Hibernate; boleh {@code null}.
	 * @param paketJurusanPmb   kombinasi paket+prodi yang diperiksa.
	 * @param gelombangPendaftaran gelombang yang sedang diproses.
	 * @param tahunAkademik     tahun akademik penyaring; boleh {@code null}.
	 * @param abaikanBiodataId  id pendaftar yang dikecualikan dari pencacahan; boleh {@code null}.
	 * @return {@code true} bila masih boleh menerima pendaftar baru pada kombinasi tersebut.
	 * @see #hitungJumlahPendaftarKuota(Session, PaketJurusanPmb, GelombangPendaftaran, String, Long)
	 */
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

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Seluruh field memakai nilai awal deklarasi
	 * ({@code tanggalDaftar}/{@code tanggalPendaftaran} = waktu server, {@code tahun} = tahun
	 * berjalan, {@code statusLulus} = 0, dst.).
	 */
	public BiodataCalonMahasiswa() {
	}

	/**
	 * Membuat objek "kulit" yang hanya berisi id — dipakai sebagai referensi ringan pada kriteria
	 * pencarian atau relasi, tanpa memuat seluruh kolom dari database.
	 *
	 * @param id primary key baris yang diwakili.
	 */
	public BiodataCalonMahasiswa(Long id) {
		this.id = id;
	}

	/**
	 * Primary key baris ini (kolom {@code id}, IDENTITY/serial PostgreSQL, karena itu
	 * {@code insertable = false}: nilainya ditentukan database saat INSERT).
	 *
	 * @return id baris; {@code null} bila objek belum pernah disimpan.
	 * @see ais.database.model.GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Umumnya hanya dipanggil Hibernate.
	 *
	 * @param id primary key baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Alamat tempat tinggal pendaftar.
	 *
	 * <p>Bila field lokal masih kosong DAN pendaftar tercatat sebagai alumni kampus ini
	 * ({@link #getMahasiswaAlumni()}), nilainya diambil dari biodata mahasiswa alumni tersebut —
	 * pola <i>fallback alumni</i> yang dipakai puluhan getter biodata di kelas ini. Karena getter
	 * ini juga properti Hibernate, nilai hasil fallback ikut TERTULIS ke kolom saat flush
	 * berikutnya.</p>
	 *
	 * @return alamat; {@code null} bila belum diisi dan tidak ada sumber alumni.
	 */
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

	/**
	 * Menetapkan alamat tempat tinggal pendaftar.
	 *
	 * @param alamat alamat baru.
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Nama ayah pendaftar. Memakai pola <i>fallback alumni</i> yang sama dengan
	 * {@link #getAlamat()}.
	 *
	 * @return nama ayah; {@code null} bila belum diisi dan tidak ada sumber alumni.
	 */
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

	/**
	 * Menetapkan nama ayah pendaftar.
	 *
	 * @param namaAyah nama ayah.
	 */
	public void setNamaAyah(String namaAyah) {
		this.namaAyah = namaAyah;
	}

	/**
	 * Pekerjaan ayah pendaftar (referensi master {@link PekerjaanOrangTua}).
	 *
	 * @return pekerjaan ayah; {@code null} bila belum dipilih.
	 * @see ais.database.model.GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pekerjaan_orang_tua", nullable = true)
	public PekerjaanOrangTua getPekerjaanAyah() {
		pekerjaanAyah = check(pekerjaanAyah);
		return this.pekerjaanAyah;
	}

	/**
	 * Menetapkan pekerjaan ayah pendaftar.
	 *
	 * @param pekerjaanAyah master pekerjaan orang tua.
	 */
	public void setPekerjaanAyah(PekerjaanOrangTua pekerjaanAyah) {
		this.pekerjaanAyah = pekerjaanAyah;
	}

	/**
	 * Nama ibu pendaftar, dinormalisasi: spasi ganda dirapatkan (tiga kali berturut-turut, cukup
	 * untuk menangani sampai delapan spasi beruntun) lalu dipangkas ujungnya. Setelah itu barulah
	 * pola <i>fallback alumni</i> ({@link #getAlamat()}) diterapkan bila hasilnya masih kosong.
	 *
	 * <p><b>Efek samping:</b> hasil normalisasi ditulis balik ke field, jadi tersimpan ke database
	 * pada flush berikutnya.</p>
	 *
	 * @return nama ibu yang sudah dirapikan; {@code null} bila belum diisi dan tidak ada sumber
	 *         alumni.
	 */
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

	/**
	 * Menetapkan nama ibu pendaftar (normalisasi dilakukan di {@link #getNamaIbu()}, bukan di
	 * sini).
	 *
	 * @param namaIbu nama ibu.
	 */
	public void setNamaIbu(String namaIbu) {
		this.namaIbu = namaIbu;
	}

	/**
	 * Pekerjaan ibu sebagai teks bebas.
	 *
	 * <p><b>Kuirk:</b> berbeda dari {@link #getPekerjaanAyah()} yang memakai master
	 * {@link PekerjaanOrangTua}, kolom ini hanya {@code varchar(150)}. Master untuk ibu ada di
	 * properti terpisah {@link #getPekerjaanAyahIbu()} (penamaan warisan, artinya "pekerjaan orang
	 * tua — ibu").</p>
	 *
	 * @return nama pekerjaan ibu; {@code null} bila belum diisi.
	 */
	@Column(name = "pekerjaan_ibu", length = 150)
	public String getPekerjaanIbu() {

		return this.pekerjaanIbu;
	}

	/**
	 * Menetapkan pekerjaan ibu (teks bebas).
	 *
	 * @param pekerjaanIbu nama pekerjaan.
	 */
	public void setPekerjaanIbu(String pekerjaanIbu) {
		this.pekerjaanIbu = pekerjaanIbu;
	}

	/**
	 * Nama sebagaimana harus tercetak pada ijazah (bisa berbeda ejaan/gelar dari
	 * {@link #getNama()}).
	 *
	 * @return nama untuk ijazah; {@code null} bila belum diisi.
	 */
	@Column(name = "nama_untuk_ijazah", columnDefinition = "text")
	public String getNamaUntukIjazah() {
		return this.namaUntukIjazah;
	}

	/**
	 * Menetapkan nama untuk ijazah.
	 *
	 * @param namaUntukIjazah nama untuk ijazah.
	 */
	public void setNamaUntukIjazah(String namaUntukIjazah) {
		this.namaUntukIjazah = namaUntukIjazah;
	}

	/**
	 * Nomor ijazah sekolah asal pendaftar.
	 *
	 * @return nomor ijazah; {@code null} bila belum diisi.
	 */
	@Column(name = "no_ijazah", columnDefinition = "text")
	public String getNoIjazah() {
		return this.noIjazah;
	}

	/**
	 * Menetapkan nomor ijazah sekolah asal.
	 *
	 * @param noIjazah nomor ijazah.
	 */
	public void setNoIjazah(String noIjazah) {
		this.noIjazah = noIjazah;
	}

	/**
	 * Ukuran jaket almamater yang diminta pendaftar (data operasional daftar ulang).
	 *
	 * @return ukuran jaket; {@code null} bila belum diisi.
	 */
	@Column(name = "ukuran_jaket", columnDefinition = "text")
	public String getUkuranJaket() {
		return this.ukuranJaket;
	}

	/**
	 * Menetapkan ukuran jaket almamater.
	 *
	 * @param ukuranJaket ukuran jaket.
	 */
	public void setUkuranJaket(String ukuranJaket) {
		this.ukuranJaket = ukuranJaket;
	}

	/**
	 * Tinggi badan pendaftar dalam sentimeter (dipakai formulir kesehatan/seleksi tertentu).
	 *
	 * @return tinggi badan; {@code null} bila belum diisi.
	 */
	@Column(name = "tinggi_badan")
	public Integer getTinggiBadan() {
		return this.tinggiBadan;
	}

	/**
	 * Menetapkan tinggi badan (cm).
	 *
	 * @param tinggiBadan tinggi badan.
	 */
	public void setTinggiBadan(Integer tinggiBadan) {
		this.tinggiBadan = tinggiBadan;
	}

	/**
	 * Penanda pernah menetap di luar negeri (dipakai formulir data diri; 0/1, bukan boolean).
	 *
	 * @return penanda; {@code null} bila belum diisi.
	 */
	@Column(name = "pernah_menetap_di_luar_negeri")
	public Integer getPernahMenetapDiLuarNegeri() {
		return this.pernahMenetapDiLuarNegeri;
	}

	/**
	 * Menetapkan penanda pernah menetap di luar negeri.
	 *
	 * @param pernahMenetapDiLuarNegeri penanda 0/1.
	 */
	public void setPernahMenetapDiLuarNegeri(Integer pernahMenetapDiLuarNegeri) {
		this.pernahMenetapDiLuarNegeri = pernahMenetapDiLuarNegeri;
	}

	/**
	 * Berat badan pendaftar dalam kilogram.
	 *
	 * @return berat badan; {@code null} bila belum diisi.
	 */
	@Column(name = "berat_badan")
	public Integer getBeratBadan() {
		return this.beratBadan;
	}

	/**
	 * Menetapkan berat badan (kg).
	 *
	 * @param beratBadan berat badan.
	 */
	public void setBeratBadan(Integer beratBadan) {
		this.beratBadan = beratBadan;
	}

	/**
	 * Nomor telepon rumah, SUDAH dibersihkan: semua karakter selain angka dan titik dibuang
	 * (menghapus spasi, tanda hubung, tanda kurung, awalan {@code +}, dan tanda petik bawaan
	 * tempel-dari-Excel).
	 *
	 * <p><b>Perhatikan:</b> yang dikembalikan adalah hasil pembersihan, sedangkan field aslinya
	 * TIDAK dimutasi — berbeda dari {@link #getHp()} versi lama. Nilai kembali tidak pernah
	 * {@code null}, melainkan string kosong.</p>
	 *
	 * @return nomor telepon rumah berisi digit saja; {@code ""} bila belum diisi.
	 */
	@Column(name = "telepon_rumah", length = 20)
	public String getTeleponRumah() {

		return this.teleponRumah == null ? "" : teleponRumah.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Menetapkan nomor telepon rumah (mentah; pembersihan dilakukan di
	 * {@link #getTeleponRumah()}).
	 *
	 * @param teleponRumah nomor telepon rumah.
	 */
	public void setTeleponRumah(String teleponRumah) {
		this.teleponRumah = teleponRumah;
	}

	/**
	 * Nomor HP pendaftar, sudah dibersihkan menjadi digit saja.
	 *
	 * <p>Langkah yang dijalankan, berurutan:</p>
	 * <ol>
	 * <li>bila kosong, pakai pola <i>fallback alumni</i> ({@link #getMahasiswaAlumni()});</li>
	 * <li>buang tanda petik ({@code '}) di depan — konvensi pengguna agar Excel memperlakukan
	 * nomor sebagai teks;</li>
	 * <li>buang semua karakter selain angka dan titik.</li>
	 * </ol>
	 *
	 * <p><b>Catatan implementasi penting:</b> seluruh proses memakai variabel lokal sehingga field
	 * {@code this.hp} TIDAK ikut dimutasi oleh getter ini (perbaikan atas versi lama).
	 * {@link #tampilkanHp(Component)} menangani penggabungan dengan telepon rumah untuk
	 * tampilan.</p>
	 *
	 * @return nomor HP berisi digit saja; {@code ""} bila tidak ada.
	 */
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

	/**
	 * Menetapkan nomor HP (mentah; pembersihan dilakukan di {@link #getHp()}).
	 *
	 * @param hp nomor HP.
	 */
	public void setHp(String hp) {
		this.hp = hp;
	}

	/**
	 * Nomor SIM yang dimiliki pendaftar (data pelengkap formulir).
	 *
	 * @return nomor/keterangan SIM; {@code null} bila belum diisi.
	 */
	@Column(name = "surat_izin_mengemudi", length = 255)
	public String getSuratIzinMengemudi() {
		return this.suratIzinMengemudi;
	}

	/**
	 * Menetapkan nomor/keterangan SIM.
	 *
	 * @param suratIzinMengemudi nomor/keterangan SIM.
	 */
	public void setSuratIzinMengemudi(String suratIzinMengemudi) {
		this.suratIzinMengemudi = suratIzinMengemudi;
	}

	/**
	 * Kendaraan yang direncanakan dipakai untuk kuliah (data pelengkap formulir, dipakai untuk
	 * perencanaan parkir/transportasi).
	 *
	 * @return keterangan kendaraan; {@code null} bila belum diisi.
	 */
	@Column(name = "kendaraan_kuliah", length = 255)
	public String getKendaraanKuliah() {
		return this.kendaraanKuliah;
	}

	/**
	 * Menetapkan keterangan kendaraan untuk kuliah.
	 *
	 * @param kendaraanKuliah keterangan kendaraan.
	 */
	public void setKendaraanKuliah(String kendaraanKuliah) {
		this.kendaraanKuliah = kendaraanKuliah;
	}

	/**
	 * Penanda pernah memimpin organisasi (0/1) — bahan pertimbangan seleksi jalur prestasi.
	 *
	 * @return penanda; {@code null} bila belum diisi.
	 */
	@Column(name = "pernah_memimpin_organisasi")
	public Integer getPernahMemimpinOrganisasi() {
		return this.pernahMemimpinOrganisasi;
	}

	/**
	 * Menetapkan penanda pernah memimpin organisasi.
	 *
	 * @param pernahMemimpinOrganisasi penanda 0/1.
	 */
	public void setPernahMemimpinOrganisasi(Integer pernahMemimpinOrganisasi) {
		this.pernahMemimpinOrganisasi = pernahMemimpinOrganisasi;
	}

	/**
	 * Nama organisasi yang pernah diikuti/dipimpin pendaftar.
	 *
	 * @return nama organisasi; {@code null} bila belum diisi.
	 */
	@Column(name = "nama_organisasi", length = 255)
	public String getNamaOrganisasi() {
		return this.namaOrganisasi;
	}

	/**
	 * Menetapkan nama organisasi.
	 *
	 * @param namaOrganisasi nama organisasi.
	 */
	public void setNamaOrganisasi(String namaOrganisasi) {
		this.namaOrganisasi = namaOrganisasi;
	}

	/**
	 * Hobi pendaftar (teks bebas).
	 *
	 * @return hobi; {@code null} bila belum diisi.
	 */
	@Column(name = "hobi")
	public String getHobi() {
		return this.hobi;
	}

	/**
	 * Menetapkan hobi.
	 *
	 * @param hobi hobi.
	 */
	public void setHobi(String hobi) {
		this.hobi = hobi;
	}

	/**
	 * Minat seni pendaftar (teks bebas) — dipakai penempatan unit kegiatan mahasiswa.
	 *
	 * @return minat seni; {@code null} bila belum diisi.
	 */
	@Column(name = "minat_seni")
	public String getMinatSeni() {
		return this.minatSeni;
	}

	/**
	 * Menetapkan minat seni.
	 *
	 * @param minatSeni minat seni.
	 */
	public void setMinatSeni(String minatSeni) {
		this.minatSeni = minatSeni;
	}

	/**
	 * Kemampuan bahasa ke-1 (slot bebas; tidak ada master bahasa, murni teks).
	 *
	 * @return kemampuan bahasa pertama; {@code null} bila belum diisi.
	 */
	@Column(name = "kemampuan_bahasa1", length = 255)
	public String getKemampuanBahasa1() {
		return this.kemampuanBahasa1;
	}

	/**
	 * Menetapkan kemampuan bahasa ke-1.
	 *
	 * @param kemampuanBahasa1 keterangan kemampuan bahasa.
	 */
	public void setKemampuanBahasa1(String kemampuanBahasa1) {
		this.kemampuanBahasa1 = kemampuanBahasa1;
	}

	/**
	 * Kemampuan bahasa ke-2.
	 *
	 * @return kemampuan bahasa kedua; {@code null} bila belum diisi.
	 */
	@Column(name = "kemampuan_bahasa2", length = 255)
	public String getKemampuanBahasa2() {
		return this.kemampuanBahasa2;
	}

	/**
	 * Menetapkan kemampuan bahasa ke-2.
	 *
	 * @param kemampuanBahasa2 keterangan kemampuan bahasa.
	 */
	public void setKemampuanBahasa2(String kemampuanBahasa2) {
		this.kemampuanBahasa2 = kemampuanBahasa2;
	}

	/**
	 * Kemampuan bahasa ke-3.
	 *
	 * @return kemampuan bahasa ketiga; {@code null} bila belum diisi.
	 */
	@Column(name = "kemampuan_bahasa3", length = 255)
	public String getKemampuanBahasa3() {
		return this.kemampuanBahasa3;
	}

	/**
	 * Menetapkan kemampuan bahasa ke-3.
	 *
	 * @param kemampuanBahasa3 keterangan kemampuan bahasa.
	 */
	public void setKemampuanBahasa3(String kemampuanBahasa3) {
		this.kemampuanBahasa3 = kemampuanBahasa3;
	}

	/**
	 * Nama SMA/sederajat asal pendaftar.
	 *
	 * <p>Sumber kebenaran sebenarnya adalah master {@link #getNamaSekolahAsal()}: bila master itu
	 * terisi, namanya MENIMPA teks bebas {@code asalSma} (dan tertulis ke kolom saat flush). Teks
	 * penampung ZK {@code "== Klik disini untuk pilih =="} yang pernah tersimpan diperlakukan
	 * sebagai kosong.</p>
	 *
	 * @return nama SMA asal yang sudah dipangkas; {@code ""} bila kosong atau masih berupa teks
	 *         penampung.
	 */
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

	/**
	 * Menetapkan nama SMA asal (teks bebas). Ingat: {@link #getAsalSma()} dapat menimpanya dengan
	 * nama dari master {@link NamaSekolahAsal}.
	 *
	 * @param asalSma nama SMA asal.
	 */
	public void setAsalSma(String asalSma) {
		this.asalSma = asalSma;
	}

	/**
	 * Alamat SMA/sederajat asal.
	 *
	 * @return alamat sekolah; {@code null} bila belum diisi.
	 */
	@Column(name = "alamat_asal_sma")
	public String getAlamatAsalSma() {
		return this.alamatAsalSma;
	}

	/**
	 * Menetapkan alamat SMA asal.
	 *
	 * @param alamatAsalSma alamat sekolah.
	 */
	public void setAlamatAsalSma(String alamatAsalSma) {
		this.alamatAsalSma = alamatAsalSma;
	}

	/**
	 * Nama SMP asal, sudah dipangkas dan dibersihkan dari tanda petik tunggal maupun ganda (data
	 * hasil impor spreadsheet kerap membawa tanda petik pembungkus teks).
	 *
	 * @return nama SMP asal; {@code ""} bila belum diisi.
	 */
	@Column(name = "asal_smp", length = 255)
	public String getAsalSmp() {
		return this.asalSmp == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSmp.trim(), "'", ""), "\"", "");
	}

	/**
	 * Menetapkan nama SMP asal (mentah; pembersihan dilakukan di {@link #getAsalSmp()}).
	 *
	 * @param asalSmp nama SMP asal.
	 */
	public void setAsalSmp(String asalSmp) {
		this.asalSmp = asalSmp;
	}

	/**
	 * Alamat SMP asal.
	 *
	 * @return alamat sekolah; {@code null} bila belum diisi.
	 */
	@Column(name = "alamat_asal_smp")
	public String getAlamatAsalSmp() {
		return this.alamatAsalSmp;
	}

	/**
	 * Menetapkan alamat SMP asal.
	 *
	 * @param alamatAsalSmp alamat sekolah.
	 */
	public void setAlamatAsalSmp(String alamatAsalSmp) {
		this.alamatAsalSmp = alamatAsalSmp;
	}

	/**
	 * Nama SD asal, dibersihkan dengan cara yang sama seperti {@link #getAsalSmp()}.
	 *
	 * @return nama SD asal; {@code ""} bila belum diisi.
	 */
	@Column(name = "asal_sd", length = 255)
	public String getAsalSd() {
		return this.asalSd == null ? ""
				: org.apache.commons.lang3.StringUtils
						.replace(org.apache.commons.lang3.StringUtils.replace(this.asalSd.trim(), "'", ""), "\"", "");
	}

	/**
	 * Menetapkan nama SD asal (mentah; pembersihan dilakukan di {@link #getAsalSd()}).
	 *
	 * @param asalSd nama SD asal.
	 */
	public void setAsalSd(String asalSd) {
		this.asalSd = asalSd;
	}

	/**
	 * Alamat SD asal.
	 *
	 * @return alamat sekolah; {@code null} bila belum diisi.
	 */
	@Column(name = "alamat_asal_sd")
	public String getAlamatAsalSd() {
		return this.alamatAsalSd;
	}

	/**
	 * Menetapkan alamat SD asal.
	 *
	 * @param alamatAsalSd alamat sekolah.
	 */
	public void setAlamatAsalSd(String alamatAsalSd) {
		this.alamatAsalSd = alamatAsalSd;
	}

	/**
	 * Golongan darah pendaftar (teks bebas, mis. {@code "O"}, {@code "AB+"}).
	 *
	 * @return golongan darah; {@code null} bila belum diisi.
	 */
	@Column(name = "golongan_darah", length = 10)
	public String getGolonganDarah() {
		return this.golonganDarah;
	}

	/**
	 * Menetapkan golongan darah.
	 *
	 * @param golonganDarah golongan darah.
	 */
	public void setGolonganDarah(String golonganDarah) {
		this.golonganDarah = golonganDarah;
	}

	/**
	 * Status pernikahan pendaftar (0 = belum menikah).
	 *
	 * <p><b>Efek samping:</b> bila kolomnya masih {@code NULL} (data lama), field diisi 0 dan nilai
	 * itu ikut tersimpan pada flush berikutnya — normalisasi disengaja agar laporan tidak
	 * kosong.</p>
	 *
	 * @return status nikah; tidak pernah {@code null}.
	 */
	@Column(name = "status_nikah")
	public Integer getStatusNikah() {
		if (statusNikah == null) {
			statusNikah = 0;
		}
		return this.statusNikah;
	}

	/**
	 * Menetapkan status pernikahan.
	 *
	 * @param statusNikah status nikah (0 = belum menikah).
	 */
	public void setStatusNikah(Integer statusNikah) {
		this.statusNikah = statusNikah;
	}

	/**
	 * Kewarganegaraan pendaftar.
	 *
	 * <p><b>Efek samping:</b> bila masih {@code null}, diisi bawaan {@link Mahasiswa#WNI} dan nilai
	 * itu ikut tersimpan pada flush berikutnya.</p>
	 *
	 * @return kode kewarganegaraan; tidak pernah {@code null}.
	 */
	@Column(name = "kewarganegaraan", length = 10)
	public String getKewarganegaraan() {
		if (kewarganegaraan == null) {
			kewarganegaraan = Mahasiswa.WNI;
		}
		return this.kewarganegaraan;
	}

	/**
	 * Menetapkan kewarganegaraan.
	 *
	 * @param kewarganegaraan kode kewarganegaraan (mis. {@link Mahasiswa#WNI}).
	 */
	public void setKewarganegaraan(String kewarganegaraan) {
		this.kewarganegaraan = kewarganegaraan;
	}

	/**
	 * Agama pendaftar (master {@link Agama}). Memakai pola <i>fallback alumni</i>: bila belum
	 * dipilih, diambil dari biodata mahasiswa alumni bila ada.
	 *
	 * @return master agama; {@code null} bila belum dipilih dan tidak ada sumber alumni.
	 */
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

	/**
	 * Menetapkan agama pendaftar.
	 *
	 * @param agama master agama.
	 */
	public void setAgama(Agama agama) {
		this.agama = agama;
	}

	/**
	 * Menetapkan nomor RT alamat pendaftar (pemotongan panjang dilakukan di {@link #getRt()}).
	 *
	 * @param rt nomor RT.
	 */
	public void setRt(String rt) {
		this.rt = rt;
	}

	/**
	 * Nomor RT alamat pendaftar, DIPOTONG maksimal 3 karakter (kolom database hanya menampung
	 * format RT tiga digit; data impor kerap membawa teks panjang). Bila kosong, dipakai pola
	 * <i>fallback alumni</i>.
	 *
	 * <p><b>Efek samping:</b> pemotongan ditulis balik ke field, jadi tersimpan permanen pada
	 * flush berikutnya.</p>
	 *
	 * @return nomor RT (maksimal 3 karakter); {@code null} bila belum diisi.
	 */
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

	/**
	 * Menetapkan nomor RW alamat pendaftar (pemotongan panjang dilakukan di {@link #getRw()}).
	 *
	 * @param rw nomor RW.
	 */
	public void setRw(String rw) {
		this.rw = rw;
	}

	/**
	 * Nomor RW alamat pendaftar. Perlakuannya identik dengan {@link #getRt()}: dipotong maksimal 3
	 * karakter, memakai pola <i>fallback alumni</i>, dan hasil pemotongan tersimpan permanen.
	 *
	 * @return nomor RW (maksimal 3 karakter); {@code null} bila belum diisi.
	 */
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

	/**
	 * Menetapkan tempat lahir.
	 *
	 * @param tempatLahir kota/tempat lahir.
	 */
	public void setTempatLahir(String tempatLahir) {
		this.tempatLahir = tempatLahir;
	}

	/**
	 * Tempat lahir pendaftar. Bila kosong, diambil dari {@code mahasiswaAlumni.getTempatlahir()}
	 * — perhatikan bahwa sumbernya di sini adalah entity {@link Mahasiswa} langsung, bukan
	 * {@link BiodataMahasiswa}-nya seperti kebanyakan fallback lain di kelas ini.
	 *
	 * @return tempat lahir; {@code null} bila belum diisi dan tidak ada sumber alumni.
	 */
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

	/**
	 * Menetapkan tanggal lahir.
	 *
	 * @param tanggalLahir tanggal lahir.
	 */
	public void setTanggalLahir(Date tanggalLahir) {
		this.tanggalLahir = tanggalLahir;
	}

	/**
	 * Tanggal lahir pendaftar; bila kosong diambil dari {@code mahasiswaAlumni.getTanggallahir()}.
	 *
	 * <p>Ikut disalin ke {@link Mahasiswa} saat konversi PMB, dan dipakai
	 * {@code CommonPMB.saveMahasiswa} sebagai salah satu kunci pencocokan mahasiswa yang mungkin
	 * sudah ada (nama + tanggal lahir + tahun angkatan + prodi).</p>
	 *
	 * @return tanggal lahir; {@code null} bila belum diisi dan tidak ada sumber alumni.
	 */
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

	/**
	 * Menetapkan jenis kelamin.
	 *
	 * @param jenisKelamin kode jenis kelamin.
	 */
	public void setJenisKelamin(String jenisKelamin) {
		this.jenisKelamin = jenisKelamin;
	}

	/**
	 * Jenis kelamin pendaftar; bila kosong diambil dari {@code mahasiswaAlumni.getKelamin()}.
	 *
	 * @return kode jenis kelamin; {@code null} bila belum diisi dan tidak ada sumber alumni.
	 */
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

	/**
	 * Menetapkan negara asal.
	 *
	 * @param asalNegara master negara.
	 */
	public void setAsalNegara(Negara asalNegara) {
		this.asalNegara = asalNegara;
	}

	/**
	 * Negara asal pendaftar. Urutan penentuan: field lokal, lalu fallback ke
	 * {@code mahasiswaAlumni.getNegara()}, dan terakhir bawaan {@code ConstantValues.INDONESIA}.
	 *
	 * <p><b>Efek samping:</b> bawaan Indonesia ikut TERTULIS ke kolom saat flush — jadi baris yang
	 * sebelumnya {@code NULL} akan permanen menjadi Indonesia begitu getter ini pernah
	 * dipanggil.</p>
	 *
	 * @return master negara; tidak pernah {@code null} selama master Indonesia tersedia.
	 */
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

	/**
	 * Menetapkan kode pos alamat pendaftar (pemotongan panjang dilakukan di
	 * {@link #getKodePos()}).
	 *
	 * @param kodePos kode pos.
	 */
	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	/**
	 * Kode pos alamat pendaftar; memakai pola <i>fallback alumni</i> lalu DIPOTONG maksimal 8
	 * karakter (dan pemotongan itu tersimpan permanen pada flush berikutnya).
	 *
	 * @return kode pos; {@code null} bila belum diisi dan tidak ada sumber alumni.
	 */
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

	/**
	 * Menetapkan nama dosen/karyawan yang merekomendasikan atau memperkenalkan kampus kepada
	 * pendaftar.
	 *
	 * @param dariNamaDosenKaryawan nama dosen/karyawan perujuk.
	 */
	public void setDariNamaDosenKaryawan(String dariNamaDosenKaryawan) {
		this.dariNamaDosenKaryawan = dariNamaDosenKaryawan;
	}

	/**
	 * Nama dosen/karyawan perujuk — dipakai bersama {@link #getInfoKampusDariMana()} untuk
	 * menelusuri efektivitas promosi PMB.
	 *
	 * @return nama perujuk; {@code null} bila belum diisi.
	 */
	@Column(name = "dari_nama_dosen_karyawan", columnDefinition = "text")
	public String getDariNamaDosenKaryawan() {
		return dariNamaDosenKaryawan;
	}

	/**
	 * Menetapkan pendidikan terakhir ayah (teks bebas).
	 *
	 * @param pendidikanAyah keterangan pendidikan.
	 */
	public void setPendidikanAyah(String pendidikanAyah) {
		this.pendidikanAyah = pendidikanAyah;
	}

	/**
	 * Pendidikan terakhir ayah sebagai teks bebas (versi bermaster ada di
	 * {@link #getPendidikanOrtu()}).
	 *
	 * @return keterangan pendidikan ayah; {@code null} bila belum diisi.
	 */
	@Column(name = "pendidikan_ayah", length = 20)
	public String getPendidikanAyah() {
		return pendidikanAyah;
	}

	/**
	 * Menetapkan pendidikan terakhir ibu (teks bebas).
	 *
	 * @param pendidikanIbu keterangan pendidikan.
	 */
	public void setPendidikanIbu(String pendidikanIbu) {
		this.pendidikanIbu = pendidikanIbu;
	}

	/**
	 * Pendidikan terakhir ibu sebagai teks bebas (versi bermaster ada di
	 * {@link #getPendidikanOrtuIbu()}).
	 *
	 * @return keterangan pendidikan ibu; {@code null} bila belum diisi.
	 */
	@Column(name = "pendidikan_ibu", length = 20)
	public String getPendidikanIbu() {
		return pendidikanIbu;
	}

	/**
	 * Menetapkan nomor kwitansi pembayaran pendaftaran.
	 *
	 * @param no_kwitansi nomor kwitansi.
	 */
	public void setNo_kwitansi(String no_kwitansi) {
		this.no_kwitansi = no_kwitansi;
	}

	/**
	 * Nomor kwitansi pembayaran biaya pendaftaran (catatan manual loket; pencatatan resminya lewat
	 * {@link #getPembayaranRegistrasi()}).
	 *
	 * @return nomor kwitansi; {@code null} bila belum diisi.
	 */
	@Column(name = "no_kwitansi", columnDefinition = "text")
	public String getNo_kwitansi() {
		return no_kwitansi;
	}

	/**
	 * Menetapkan nama pendaftar (normalisasi dan penimpaan dilakukan di {@link #getNama()}).
	 *
	 * @param nama nama pendaftar.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Nama pendaftar — salah satu getter paling "bermuatan" di kelas ini. Urutan yang dijalankan:
	 * <ol>
	 * <li>spasi ganda dirapatkan (tiga lintasan) lalu dipangkas;</li>
	 * <li>bila sudah ada {@link Mahasiswa} resmi hasil konversi PMB, nama DITIMPA dengan
	 * {@code mahasiswa.getNama()} — mahasiswa resmi adalah sumber kebenaran setelah konversi;</li>
	 * <li>bila pendaftar adalah alumni, nama ditimpa lagi dengan {@code mahasiswaAlumni.getNama()};</li>
	 * <li>nilai kembali di-{@code trim()} dan di-HURUF BESAR-kan.</li>
	 * </ol>
	 *
	 * <p><b>Perhatikan dua hal.</b> (1) Yang dikembalikan adalah versi huruf besar, tetapi yang
	 * TERSIMPAN ke kolom adalah nilai field (hasil langkah 1-3) yang belum tentu huruf besar —
	 * jadi hasil getter tidak sama persis dengan isi kolom. (2) Langkah 2 dan 3 membuat perubahan
	 * nama di sisi mahasiswa resmi/alumni ikut merambat ke baris calon ini pada flush
	 * berikutnya.</p>
	 *
	 * @return nama pendaftar dalam huruf besar; {@code null} bila belum ada nama sama sekali.
	 */
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

	/**
	 * Menetapkan nomor registrasi pendaftar.
	 *
	 * @param noRegistrasi nomor registrasi.
	 */
	public void setNoRegistrasi(String noRegistrasi) {
		this.noRegistrasi = noRegistrasi;
	}

	/**
	 * Nomor registrasi pendaftar — identitas alami baris ini (kolom {@code no_registrasi}, UNIQUE)
	 * dan kunci pengurutan pada {@link #compareTo(GeneralValueObject)}.
	 *
	 * <p>String kosong dinormalkan menjadi {@code null} supaya batasan UNIQUE tidak dilanggar oleh
	 * banyak baris yang sama-sama berisi {@code ""}. Properti hanya-baca untuk nilai yang sama ada
	 * di {@link #getKode()}.</p>
	 *
	 * @return nomor registrasi terpangkas; {@code null} bila kosong.
	 */
	@Column(name = "no_registrasi", unique = true, length = 20)
	public String getNoRegistrasi() {
		return noRegistrasi == null || noRegistrasi.trim().isEmpty() ? null : noRegistrasi.trim();
	}

	/**
	 * Menetapkan nomor ujian seleksi.
	 *
	 * @param noUjian nomor ujian.
	 */
	public void setNoUjian(String noUjian) {
		this.noUjian = noUjian;
	}

	/**
	 * Nomor ujian seleksi (nomor peserta tes masuk).
	 *
	 * <p>Bila konfigurasi {@code nomor_ujian_calon_mahasiswa_sama_dengan_no_reg} aktif, nomor ujian
	 * DIPAKSA sama dengan {@link #getNoRegistrasi()} — institusi yang tidak memakai penomoran ujian
	 * terpisah. String kosong dinormalkan menjadi {@code null}. Keduanya ditulis balik ke field
	 * sehingga tersimpan pada flush berikutnya.</p>
	 *
	 * @return nomor ujian; {@code null} bila kosong.
	 */
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

	/**
	 * Menetapkan PIN pendaftaran.
	 *
	 * @param pin nomor PIN.
	 */
	public void setPin(Integer pin) {
		this.pin = pin;
	}

	/**
	 * PIN pendaftaran — nomor yang dibeli/diberikan kepada pendaftar untuk mengakses formulir PMB
	 * daring. Kata sandinya ada di {@link #getPinPassword()}.
	 *
	 * @return nomor PIN; {@code null} bila tidak memakai mekanisme PIN.
	 */
	@Column(name = "pin")
	public Integer getPin() {
		return pin;
	}

	/**
	 * Menetapkan penanda status pembayaran ringkas.
	 *
	 * @param statusPembayaran penanda status pembayaran.
	 */
	public void setStatusPembayaran(Integer statusPembayaran) {
		this.statusPembayaran = statusPembayaran;
	}

	/**
	 * Penanda ringkas status pembayaran pendaftaran (kolom {@code status_pembayaran}, 1 digit).
	 *
	 * <p>Ini hanya penanda datar warisan; keadaan pembayaran yang sesungguhnya ditentukan oleh
	 * entity {@link Kegiatan} pada {@link #getPembayaranRegistrasi()} dan
	 * {@link #getPembayaranDaftarUlang()}.</p>
	 *
	 * @return penanda status pembayaran; {@code null} bila belum diisi.
	 */
	@Column(name = "status_pembayaran", length = 1)
	public Integer getStatusPembayaran() {
		return statusPembayaran;
	}

	/**
	 * Jenis kartu identitas yang dipakai pendaftar (KTP/KK/paspor, master
	 * {@link JenisKartuIdentitasMahasiswaBaru}); nomornya ada di {@link #getNoIdentitas()}.
	 *
	 * @return master jenis kartu identitas; {@code null} bila belum dipilih.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kartu_identitas_mahasiswa_baru", nullable = true)
	public JenisKartuIdentitasMahasiswaBaru getJenisKartuIdentitas() {
		jenisKartuIdentitas = check(jenisKartuIdentitas);
		return jenisKartuIdentitas;
	}

	/**
	 * Menetapkan jenis kartu identitas.
	 *
	 * @param jenisKartuIdentitas master jenis kartu identitas.
	 */
	public void setJenisKartuIdentitas(JenisKartuIdentitasMahasiswaBaru jenisKartuIdentitas) {
		this.jenisKartuIdentitas = jenisKartuIdentitas;
	}

	/**
	 * Nomor kartu identitas pendaftar (NIK KTP dan sejenisnya, sesuai
	 * {@link #getJenisKartuIdentitas()}). Memakai pola <i>fallback alumni</i>.
	 *
	 * @return nomor identitas terpangkas; {@code ""} bila belum diisi.
	 */
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

	/**
	 * Menetapkan nomor kartu identitas.
	 *
	 * @param noIdentitas nomor identitas.
	 */
	public void setNoIdentitas(String noIdentitas) {
		this.noIdentitas = noIdentitas;
	}

	/**
	 * Alamat surel pendaftar. Kolom ini bisa memuat LEBIH DARI SATU alamat, dipisahkan koma (lihat
	 * {@link #appendEmail(String)}), karena itu getter melakukan pembersihan berlapis:
	 * <ol>
	 * <li>koma ganda ({@code ,,}) dirapatkan sampai lima lintasan;</li>
	 * <li>{@code null} dan nilai yang hanya berisi {@code ","} dianggap kosong;</li>
	 * <li>bila masih kosong, diambil dari {@code mahasiswa.ambilEmail()} (mahasiswa resmi hasil
	 * konversi), lalu dari {@code mahasiswaAlumni.getEmail()};</li>
	 * <li>tanda petik ({@code '}) di depan dibuang berulang — konvensi teks Excel.</li>
	 * </ol>
	 *
	 * <p>Seluruh badan method dibungkus {@code try/catch} karena dipanggil dari jalur render dan
	 * flush yang tidak boleh gagal hanya gara-gara data surel rusak.</p>
	 *
	 * @return alamat surel (bisa berupa daftar dipisah koma); {@code ""} bila tidak ada.
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

	/**
	 * Menetapkan (MENIMPA) alamat surel. Untuk menambah alamat baru tanpa menghapus yang lama,
	 * pakai {@link #appendEmail(String)}.
	 *
	 * @param email alamat surel.
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * MENAMBAHKAN satu alamat surel ke daftar surel pendaftar (dipisahkan koma), bukan menimpanya.
	 *
	 * <p>Penambahan dilewati bila: alamat sudah termuat di dalam nilai sekarang (uji substring
	 * {@code StringUtils.contains}), alamatnya kosong, formatnya tidak valid menurut
	 * {@code Common.isValidEmailAddress}, atau diawali {@code @}. Bila nilai sekarang kosong,
	 * alamat baru menjadi satu-satunya isi (tanpa koma di depan).</p>
	 *
	 * <p><b>Kuirk:</b> uji duplikat memakai substring, jadi {@code "budi@x.com"} dianggap sudah ada
	 * ketika daftar berisi {@code "pak.budi@x.com"}.</p>
	 *
	 * @param email alamat surel yang hendak ditambahkan; diabaikan bila tidak lolos syarat di atas.
	 */
	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	/**
	 * Kelurahan/desa alamat pendaftar (kolom {@code keluarahan_calon} — salah eja bawaan skema,
	 * jangan "diperbaiki" tanpa migrasi).
	 *
	 * <p><b>BUG yang ditemukan saat pendokumentasian (TIDAK diperbaiki di sini):</b> cabang
	 * fallback alumni MENGUJI {@code ambilBiodata().getKelurahan()} tetapi MENGAMBIL
	 * {@code ambilBiodata().getNoIdentitas()}. Akibatnya, ketika kelurahan calon masih kosong dan
	 * yang bersangkutan adalah alumni, kolom kelurahan bisa terisi NOMOR IDENTITAS — dan karena
	 * getter ini properti Hibernate, nilai salah itu ikut tersimpan saat flush. Perlu ditangani
	 * pada task perbaikan tersendiri (butuh pembersihan data lama juga).</p>
	 *
	 * @return nama kelurahan/desa; {@code null} bila belum diisi.
	 */
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

	/**
	 * Menetapkan kelurahan/desa alamat pendaftar.
	 *
	 * @param kelurahanCalon nama kelurahan/desa.
	 */
	public void setKelurahanCalon(String kelurahanCalon) {
		this.kelurahanCalon = kelurahanCalon;
	}

	/**
	 * Kecamatan alamat pendaftar sebagai {@link Wilayah} (pohon wilayah berkode Feeder/PDDikti,
	 * berbeda dari pasangan master {@link Kota}/{@link Propinsi} lama).
	 *
	 * <p>Dua perbaikan otomatis dijalankan di sini:</p>
	 * <ol>
	 * <li><b>Fallback alumni</b> dari {@code mahasiswaAlumni.ambilBiodata().getKecamatan()};</li>
	 * <li><b>Perbaikan wilayah yatim.</b> Bila wilayah tersimpan tidak punya
	 * {@code wilayahInduk} tetapi punya kode {@code feeder}, seluruh cache wilayah
	 * ({@code ConstantValues.ambilBerdasarClass(Wilayah.class)}) ditelusuri untuk mencari baris
	 * berkode Feeder SAMA yang induknya lengkap, lalu baris itulah yang dipakai. Ini menambal data
	 * hasil impor yang kehilangan rantai kecamatan &rarr; kota &rarr; propinsi.</li>
	 * </ol>
	 *
	 * @return wilayah kecamatan; {@code null} bila belum dipilih dan tidak ada sumber alumni.
	 * @see #getKotaCalon()
	 * @see #getPropinsiCalon()
	 */
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

	/**
	 * Menetapkan kecamatan alamat pendaftar.
	 *
	 * @param kecamatan wilayah kecamatan.
	 */
	public void setKecamatanCalon(Wilayah kecamatan) {
		this.kecamatanCalon = kecamatan;
	}

	/**
	 * Propinsi alamat pendaftar (master {@link Propinsi}), diisi otomatis berjenjang bila masih
	 * kosong. Urutan usaha:
	 * <ol>
	 * <li>fallback alumni dari {@code mahasiswaAlumni.ambilBiodata().getPropinsi()};</li>
	 * <li>propinsi milik {@link #getKotaCalon()} bila kota sudah terisi;</li>
	 * <li>menyusuri pohon {@link Wilayah}: kecamatan &rarr; induk pertama (kabupaten/kota) &rarr;
	 * induk kedua (propinsi), lalu mencocokkan NAMA propinsi itu ke master {@link Propinsi} lewat
	 * {@code findOrCreatePropinsi} — <b>yang akan MEMBUAT baris master baru bila tidak ada yang
	 * mirip</b>.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping berat, sengaja:</b> langkah 3 membuka session Hibernate TERSENDIRI
	 * ({@code openSession}, bukan session thread-local) dan bisa menulis ke tabel master. Alasannya
	 * ada pada komentar di dalam method: getter ini dipanggil Hibernate DI TENGAH proses INSERT
	 * entity lain, sehingga memakai lalu menutup session thread-local akan membuat penyimpanan
	 * berikutnya gagal dengan "Session is closed!". Session khusus itu ditutup rapi lewat
	 * {@link #closeSessionSafe(Session)}.</p>
	 *
	 * @return master propinsi; {@code null} bila tidak ada satu pun sumber yang bisa dipakai.
	 */
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

	/**
	 * Menetapkan propinsi alamat pendaftar.
	 *
	 * @param propinsiCalon master propinsi.
	 */
	public void setPropinsiCalon(Propinsi propinsiCalon) {
		this.propinsiCalon = propinsiCalon;
	}

	/**
	 * Kota/kabupaten alamat pendaftar (master {@link Kota}), diisi otomatis bila masih kosong:
	 * <ol>
	 * <li>fallback alumni dari {@code mahasiswaAlumni.ambilBiodata().getKota()};</li>
	 * <li>menyusuri pohon {@link Wilayah}: kecamatan &rarr; induk pertama (kabupaten/kota), lalu
	 * mencocokkan namanya ke master {@link Kota} DALAM propinsi hasil {@link #getPropinsiCalon()}
	 * memakai {@code findBestMatchKota}.</li>
	 * </ol>
	 *
	 * <p>Sama seperti {@link #getPropinsiCalon()}, langkah 2 memakai session Hibernate tersendiri
	 * (lihat alasannya di sana). Bedanya: bila tidak ada kota yang cukup mirip, method ini TIDAK
	 * membuat master baru — hasilnya tetap {@code null}.</p>
	 *
	 * @return master kota/kabupaten; {@code null} bila tidak ditemukan.
	 */
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

	/**
	 * Menetapkan kota/kabupaten alamat pendaftar.
	 *
	 * @param kotaCalon master kota.
	 */
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
	 *
	 * <p>Urutan penutupan: {@code rollback} transaksi implisit (lewat {@code doWork} pada koneksi
	 * JDBC, hanya bila autocommit mati), {@code clear()}, {@code disconnect()}, lalu
	 * {@code close()}. Tiap langkah dibungkus {@code try/catch} sendiri agar kegagalan satu langkah
	 * tidak menghalangi langkah berikutnya — session WAJIB berakhir tertutup.</p>
	 *
	 * @param session session khusus yang hendak ditutup; {@code null} diabaikan.
	 * @see #getPropinsiCalon()
	 * @see #getKotaCalon()
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

	/**
	 * Mencari master {@link Propinsi} yang paling mirip dengan {@code namaProp}, dan MEMBUATNYA
	 * bila tidak ada yang cukup mirip.
	 *
	 * <p>Pencocokan: awalan {@code "Prop."} dibuang dan nama di-huruf-kecil-kan pada kedua sisi,
	 * lalu dicari jarak Levenshtein terkecil terhadap seluruh master propinsi yang namanya tidak
	 * kosong. Ambang penerimaan adalah jarak &lt; 2 (toleransi satu salah ketik). Bila tidak ada
	 * yang lolos, propinsi BARU disimpan dengan negara {@code ConstantValues.INDONESIA}.</p>
	 *
	 * <p><b>Efek samping:</b> menulis baris master baru. Transaksi dimulai sendiri HANYA bila
	 * session belum punya transaksi aktif, dan hanya transaksi yang dimulai sendiri itu yang
	 * di-{@code commit} — supaya tidak merusak transaksi milik pemanggil.</p>
	 *
	 * @param session  session Hibernate khusus milik pemanggil (lihat {@link #getPropinsiCalon()}).
	 * @param namaProp nama propinsi yang dicari; {@code null}/kosong menghasilkan {@code null}.
	 * @return master propinsi yang cocok atau yang baru dibuat.
	 */
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

	/**
	 * Mencari master {@link Kota} DI DALAM propinsi {@code p} yang namanya paling mirip dengan
	 * {@code namaKab}.
	 *
	 * <p>Awalan {@code "Kab."} dan kata {@code "Kota"} dibuang pada kedua sisi sebelum dibandingkan
	 * dengan jarak Levenshtein; ambangnya sama dengan {@link #findOrCreatePropinsi(Session, String)},
	 * yaitu jarak &lt; 2. <b>Berbeda dari propinsi, kota TIDAK pernah dibuat otomatis</b> — bila
	 * tidak ada yang cukup mirip, hasilnya {@code null}.</p>
	 *
	 * @param session session Hibernate khusus milik pemanggil.
	 * @param p       propinsi pembatas pencarian; {@code null} menghasilkan {@code null}.
	 * @param namaKab nama kabupaten/kota yang dicari; {@code null} menghasilkan {@code null}.
	 * @return master kota yang paling mirip, atau {@code null} bila tidak ada yang lolos ambang.
	 */
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

	/**
	 * Jenis sekolah asal (SMA/SMK/MA/dsb., master {@link JenisSekolahMahasiswaBaru}).
	 *
	 * <p>Bila {@link #getJurusanSekolah()} sudah dipilih dan jurusan itu terikat pada suatu jenis
	 * sekolah, jenis sekolah DITIMPA dari sana — jurusan sekolah lebih spesifik sehingga menjadi
	 * sumber kebenaran. Nilai hasil penimpaan ikut tersimpan pada flush berikutnya.</p>
	 *
	 * @return master jenis sekolah; {@code null} bila belum bisa ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_sekolah_mahasiswa_baru", nullable = true)
	public JenisSekolahMahasiswaBaru getJenisSekolah() {
		jenisSekolah = check(jenisSekolah);

		if (getJurusanSekolah() != null && getJurusanSekolah().getJenisSekolahMahasiswaBaru() != null) {
			jenisSekolah = getJurusanSekolah().getJenisSekolahMahasiswaBaru();
		}

		return jenisSekolah;
	}

	/**
	 * Menetapkan jenis sekolah asal. Ingat: {@link #getJenisSekolah()} dapat menimpanya berdasarkan
	 * jurusan sekolah.
	 *
	 * @param jenisSekolah master jenis sekolah.
	 */
	public void setJenisSekolah(JenisSekolahMahasiswaBaru jenisSekolah) {
		this.jenisSekolah = jenisSekolah;
	}

	/**
	 * Akreditasi sekolah asal (teks bebas, mis. {@code "A"}). Nilai {@code null} dinormalkan
	 * menjadi string kosong dan tersimpan demikian pada flush berikutnya.
	 *
	 * @return akreditasi terpangkas; tidak pernah {@code null}.
	 */
	@Column(name = "akreditasi_sekolah", length = 10)
	public String getAkreditasiSekolah() {
		if (akreditasiSekolah == null) {
			akreditasiSekolah = "";
		}
		return akreditasiSekolah.trim();
	}

	/**
	 * Menetapkan akreditasi sekolah asal.
	 *
	 * @param akreditasiSekolah akreditasi.
	 */
	public void setAkreditasiSekolah(String akreditasiSekolah) {
		this.akreditasiSekolah = akreditasiSekolah;
	}

	/**
	 * Kode pos sekolah asal, dipotong maksimal 8 karakter (pemotongan tersimpan permanen, sama
	 * seperti {@link #getKodePos()}).
	 *
	 * @return kode pos sekolah; {@code null} bila belum diisi.
	 */
	@Column(name = "kodepos_sekolah", length = 10)
	public String getKodePosSekolah() {
		if (kodePosSekolah != null && kodePosSekolah.length() > 8) {
			kodePosSekolah = kodePosSekolah.substring(0, 8);
		}
		return kodePosSekolah;
	}

	/**
	 * Menetapkan kode pos sekolah asal.
	 *
	 * @param kodePosSekolah kode pos.
	 */
	public void setKodePosSekolah(String kodePosSekolah) {
		this.kodePosSekolah = kodePosSekolah;
	}

	/**
	 * Kecamatan lokasi sekolah asal ({@link Wilayah}), dengan perbaikan "wilayah yatim" yang sama
	 * seperti {@link #getKecamatanCalon()}: bila wilayah tersimpan tanpa induk, dicari padanannya
	 * berdasarkan kode Feeder pada cache wilayah lalu dipakai yang induknya lengkap.
	 *
	 * <p>Berbeda dari alamat pendaftar, di sini TIDAK ada fallback alumni dan tidak ada pengisian
	 * otomatis propinsi/kota.</p>
	 *
	 * @return wilayah kecamatan sekolah; {@code null} bila belum dipilih.
	 */
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

	/**
	 * Menetapkan kecamatan lokasi sekolah asal.
	 *
	 * @param kecamatan wilayah kecamatan sekolah.
	 */
	public void setKecamatanSekolah(Wilayah kecamatan) {
		this.kecamatanSekolah = kecamatan;
	}

	/**
	 * Propinsi lokasi sekolah asal (master {@link Propinsi}). Tidak ada pengisian otomatis.
	 *
	 * @return master propinsi sekolah; {@code null} bila belum dipilih.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi_sekolah", nullable = true)
	public Propinsi getPropinsiSekolah() {
		propinsiSekolah = check(propinsiSekolah);
		return propinsiSekolah;
	}

	/**
	 * Menetapkan propinsi lokasi sekolah asal.
	 *
	 * @param propinsiSekolah master propinsi.
	 */
	public void setPropinsiSekolah(Propinsi propinsiSekolah) {
		this.propinsiSekolah = propinsiSekolah;
	}

	/**
	 * Kota/kabupaten lokasi sekolah asal (master {@link Kota}). Tidak ada pengisian otomatis.
	 *
	 * @return master kota sekolah; {@code null} bila belum dipilih.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota_sekolah", nullable = true)
	public Kota getKotaSekolah() {
		kotaSekolah = check(kotaSekolah);
		return kotaSekolah;
	}

	/**
	 * Menetapkan kota/kabupaten lokasi sekolah asal.
	 *
	 * @param kotaSekolah master kota.
	 */
	public void setKotaSekolah(Kota kotaSekolah) {
		this.kotaSekolah = kotaSekolah;
	}

	/**
	 * Tahun kelulusan dari sekolah asal, sebagai teks.
	 *
	 * <p><b>Efek samping:</b> bila masih {@code null}, diisi TAHUN BERJALAN dan nilai itu tersimpan
	 * pada flush berikutnya — asumsi bawaan "lulusan tahun ini" yang perlu diingat saat menganalisis
	 * data pendaftar lama.</p>
	 *
	 * @return tahun kelulusan; tidak pernah {@code null}.
	 */
	@Column(name = "tahun_kelulusan", length = 10)
	public String getTahunKelulusan() {
		if (tahunKelulusan == null) {
			tahunKelulusan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) + "";
		}

		return tahunKelulusan;
	}

	/**
	 * Menetapkan tahun kelulusan dari sekolah asal.
	 *
	 * @param tahunKelulusan tahun kelulusan.
	 */
	public void setTahunKelulusan(String tahunKelulusan) {
		this.tahunKelulusan = tahunKelulusan;
	}

	/**
	 * Jurusan/peminatan di sekolah asal (IPA/IPS/Bahasa/kompetensi keahlian SMK, master
	 * {@link JurusanSekolahMahasiswaBaru}). Menjadi sumber penimpaan bagi
	 * {@link #getJenisSekolah()}.
	 *
	 * @return master jurusan sekolah; {@code null} bila belum dipilih.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan_sekolah_mahasiswa_baru", nullable = true)
	public JurusanSekolahMahasiswaBaru getJurusanSekolah() {
		jurusanSekolah = check(jurusanSekolah);
		return jurusanSekolah;
	}

	/**
	 * Menetapkan jurusan/peminatan di sekolah asal.
	 *
	 * @param jurusanSekolah master jurusan sekolah.
	 */
	public void setJurusanSekolah(JurusanSekolahMahasiswaBaru jurusanSekolah) {
		this.jurusanSekolah = jurusanSekolah;
	}

	/**
	 * Nomor telepon orang tua/wali. Memakai pola <i>fallback alumni</i>, dengan sumber
	 * {@code ambilBiodata().getTelpAyah()}.
	 *
	 * @return nomor telepon orang tua; {@code null} bila belum diisi dan tidak ada sumber alumni.
	 */
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

	/**
	 * Menetapkan nomor telepon orang tua/wali.
	 *
	 * @param noTelpOrtu nomor telepon.
	 */
	public void setNoTelpOrtu(String noTelpOrtu) {
		this.noTelpOrtu = noTelpOrtu;
	}

	/**
	 * Rentang pendapatan ayah (master {@link PendapatanOrangTua}), memakai pola <i>fallback
	 * alumni</i>. Dipakai penilaian kemampuan bayar/beasiswa.
	 *
	 * @return master pendapatan; {@code null} bila belum dipilih dan tidak ada sumber alumni.
	 */
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

	/**
	 * Menetapkan rentang pendapatan ayah.
	 *
	 * @param pendapatanOrtu master pendapatan orang tua.
	 */
	public void setPendapatanOrtu(PendapatanOrangTua pendapatanOrtu) {
		this.pendapatanOrtu = pendapatanOrtu;
	}

	/**
	 * Paket pendaftaran yang diambil pendaftar (master {@link Paket}) — penentu berapa banyak
	 * pilihan prodi yang boleh diisi (lihat {@link #getProdi1()}..{@link #getProdi5()}) sekaligus
	 * acuan tagihan pada Setting Biaya.
	 *
	 * <p>Urutan penentuan: bila baris ini berasal dari unggahan massal dan
	 * {@link UploadBiodataCalonMahasiswa} sudah menentukan paket, paket ITU yang dipakai; kalau
	 * tidak, dipakai paket tersimpan. Setelah itu {@link #terapkanKonsistensiPaketGelombang()}
	 * dijalankan untuk menegakkan kecocokan paket dengan gelombang.</p>
	 *
	 * @return master paket; {@code null} bila belum dipilih atau dikosongkan oleh pemeriksaan
	 *         konsistensi.
	 */
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

	/**
	 * Menetapkan paket pendaftaran dan MERESET guard validasi
	 * {@code paketDivalidasiUntukGelombangId} ke sentinel {@code -1L}, supaya
	 * {@link #terapkanKonsistensiPaketGelombang()} memeriksa ulang pilihan manual ini alih-alih
	 * melewatinya karena id gelombangnya kebetulan sama.
	 *
	 * @param paket master paket pendaftaran.
	 */
	public void setPaket(Paket paket) {
		this.paket = paket;
		paketDivalidasiUntukGelombangId = -1L;
	}

	/**
	 * Nama wali pendaftar (pihak penanggung jawab selain ayah/ibu). Memakai pola <i>fallback
	 * alumni</i>.
	 *
	 * @return nama wali; {@code null} bila belum diisi dan tidak ada sumber alumni.
	 */
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

	/**
	 * Menetapkan nama wali pendaftar.
	 *
	 * @param namaWali nama wali.
	 */
	public void setNamaWali(String namaWali) {
		this.namaWali = namaWali;
	}

	/**
	 * Alamat orang tua/wali. Memakai pola <i>fallback alumni</i> dengan sumber alamat pada biodata
	 * mahasiswa alumni.
	 *
	 * @return alamat orang tua; {@code null} bila belum diisi dan tidak ada sumber alumni.
	 */
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

	/**
	 * Menetapkan alamat orang tua/wali.
	 *
	 * @param alamatOrtu alamat orang tua.
	 */
	public void setAlamatOrtu(String alamatOrtu) {
		this.alamatOrtu = alamatOrtu;
	}

	/**
	 * Nomor RT alamat orang tua, dipotong maksimal 3 karakter (pemotongan tersimpan permanen, sama
	 * seperti {@link #getRt()}).
	 *
	 * @return nomor RT orang tua; {@code null} bila belum diisi.
	 */
	@Column(name = "rt_ortu", length = 10)
	public String getRtOrtu() {
		if (rtOrtu != null && rtOrtu.length() > 3) {
			rtOrtu = rtOrtu.substring(0, 3);
		}
		return rtOrtu;
	}

	/**
	 * Menetapkan nomor RT alamat orang tua.
	 *
	 * @param rtOrtu nomor RT.
	 */
	public void setRtOrtu(String rtOrtu) {
		this.rtOrtu = rtOrtu;
	}

	/**
	 * Nomor RW alamat orang tua, dipotong maksimal 3 karakter.
	 *
	 * @return nomor RW orang tua; {@code null} bila belum diisi.
	 */
	@Column(name = "rw_ortu", length = 10)
	public String getRwOrtu() {
		if (rwOrtu != null && rwOrtu.length() > 3) {
			rwOrtu = rwOrtu.substring(0, 3);
		}
		return rwOrtu;
	}

	/**
	 * Menetapkan nomor RW alamat orang tua.
	 *
	 * @param rwOrtu nomor RW.
	 */
	public void setRwOrtu(String rwOrtu) {
		this.rwOrtu = rwOrtu;
	}

	/**
	 * Kode pos alamat orang tua, dipotong maksimal 8 karakter.
	 *
	 * @return kode pos orang tua; {@code null} bila belum diisi.
	 */
	@Column(name = "kodepos_ortu", length = 10)
	public String getKodePosOrtu() {
		if (kodePosOrtu != null && kodePosOrtu.length() > 8) {
			kodePosOrtu = kodePosOrtu.substring(0, 8);
		}
		return kodePosOrtu;
	}

	/**
	 * Menetapkan kode pos alamat orang tua.
	 *
	 * @param kodePosOrtu kode pos.
	 */
	public void setKodePosOrtu(String kodePosOrtu) {
		this.kodePosOrtu = kodePosOrtu;
	}

	/**
	 * Kecamatan alamat orang tua ({@link Wilayah}), dengan perbaikan "wilayah yatim" berbasis kode
	 * Feeder yang sama seperti {@link #getKecamatanSekolah()}.
	 *
	 * @return wilayah kecamatan orang tua; {@code null} bila belum dipilih.
	 */
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

	/**
	 * Menetapkan kecamatan alamat orang tua.
	 *
	 * @param kecamatan wilayah kecamatan.
	 */
	public void setKecamatanOrtu(Wilayah kecamatan) {
		this.kecamatanOrtu = kecamatan;
	}

	/**
	 * Propinsi alamat orang tua (master {@link Propinsi}).
	 *
	 * @return master propinsi; {@code null} bila belum dipilih.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi_ortu", nullable = true)
	public Propinsi getPropinsiOrtu() {
		propinsiOrtu = check(propinsiOrtu);
		return propinsiOrtu;
	}

	/**
	 * Menetapkan propinsi alamat orang tua.
	 *
	 * @param propinsiOrtu master propinsi.
	 */
	public void setPropinsiOrtu(Propinsi propinsiOrtu) {
		this.propinsiOrtu = propinsiOrtu;
	}

	/**
	 * Kota/kabupaten alamat orang tua (master {@link Kota}).
	 *
	 * @return master kota; {@code null} bila belum dipilih.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota_ortu", nullable = true)
	public Kota getKotaOrtu() {
		kotaOrtu = check(kotaOrtu);
		return kotaOrtu;
	}

	/**
	 * Menetapkan kota/kabupaten alamat orang tua.
	 *
	 * @param kotaOrtu master kota.
	 */
	public void setKotaOrtu(Kota kotaOrtu) {
		this.kotaOrtu = kotaOrtu;
	}

	/**
	 * Kelurahan/desa alamat orang tua (teks bebas).
	 *
	 * @return nama kelurahan/desa; {@code null} bila belum diisi.
	 */
	@Column(name = "kelurahan_ortu", columnDefinition = "text")
	public String getKelurahanOrtu() {
		return kelurahanOrtu;
	}

	/**
	 * Menetapkan kelurahan/desa alamat orang tua.
	 *
	 * @param kelurahanOrtu nama kelurahan/desa.
	 */
	public void setKelurahanOrtu(String kelurahanOrtu) {
		this.kelurahanOrtu = kelurahanOrtu;
	}

	/**
	 * Pendidikan terakhir ayah versi bermaster (master {@link PendidikanOrangTua}); versi teks
	 * bebasnya ada di {@link #getPendidikanAyah()}.
	 *
	 * @return master pendidikan orang tua; {@code null} bila belum dipilih.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan_orang_tua", nullable = true)
	public PendidikanOrangTua getPendidikanOrtu() {
		pendidikanOrtu = check(pendidikanOrtu);
		return pendidikanOrtu;
	}

	/**
	 * Menetapkan pendidikan terakhir ayah (bermaster).
	 *
	 * @param pendidikanOrtu master pendidikan orang tua.
	 */
	public void setPendidikanOrtu(PendidikanOrangTua pendidikanOrtu) {
		this.pendidikanOrtu = pendidikanOrtu;
	}

	/**
	 * Pilihan program studi ke-1 (prioritas tertinggi).
	 *
	 * <p><b>Efek samping penting:</b> bila {@link #getPaket()} membatasi jumlah prodi yang boleh
	 * diambil menjadi kurang dari 1, pilihan ini DIKOSONGKAN — dan karena getter merupakan properti
	 * Hibernate, pengosongan itu ikut tersimpan ke database pada flush berikutnya. Aturan yang sama
	 * berlaku bertingkat pada {@link #getProdi2()}..{@link #getProdi5()} (masing-masing dengan
	 * ambang 2..5).</p>
	 *
	 * @return prodi pilihan pertama; {@code null} bila belum dipilih atau tidak diizinkan paket.
	 */
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

	/**
	 * Menetapkan pilihan program studi ke-1.
	 *
	 * @param prodi1 prodi pilihan pertama.
	 */
	public void setProdi1(Jurusan prodi1) {
		this.prodi1 = prodi1;
	}

	/**
	 * Pilihan program studi ke-2; dikosongkan bila paket hanya mengizinkan kurang dari 2 pilihan
	 * (lihat {@link #getProdi1()}).
	 *
	 * @return prodi pilihan kedua; {@code null} bila belum dipilih atau tidak diizinkan paket.
	 */
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

	/**
	 * Menetapkan pilihan program studi ke-2.
	 *
	 * @param prodi2 prodi pilihan kedua.
	 */
	public void setProdi2(Jurusan prodi2) {
		this.prodi2 = prodi2;
	}

	/**
	 * Jenjang pendidikan yang dituju (D3/S1/S2/dst., master {@link Jenjang}) — kolom NOT NULL.
	 *
	 * <p>Nilainya selalu diturunkan, dengan urutan: bawaan {@code ConstantValues.s1} bila masih
	 * kosong, lalu DITIMPA jenjang milik {@link #getProdiLulus()} bila pendaftar sudah diterima,
	 * atau jenjang milik {@link #getProdi1()} bila belum. Jadi jenjang mengikuti prodi, bukan
	 * sebaliknya, dan hasil turunannya tersimpan pada flush berikutnya.</p>
	 *
	 * @return master jenjang; praktis tidak pernah {@code null}.
	 */
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

	/**
	 * Menetapkan jenjang. Ingat: {@link #getJenjang()} akan menimpanya mengikuti prodi.
	 *
	 * @param jenjang master jenjang.
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	/**
	 * Status kelulusan seleksi: {@link #LULUS} (1) atau {@link #TIDAK_LULUS} (0).
	 *
	 * <p><b>Dua kondisi memaksa hasil 0 tanpa melihat nilai tersimpan:</b> pendaftar
	 * {@link #getMundur()} atau {@link #getDitolak()}. Berbeda dengan banyak getter lain di kelas
	 * ini, pemaksaan tersebut TIDAK ditulis balik ke field — jadi kolom database bisa saja tetap
	 * berisi 1 sementara getter mengembalikan 0. Jangan menyimpulkan status kelulusan dari kolom
	 * mentah; selalu lewat getter ini.</p>
	 *
	 * @return 1 bila lulus; 0 bila tidak lulus, ditolak, atau mengundurkan diri.
	 */
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

	/**
	 * Menetapkan status kelulusan seleksi.
	 *
	 * @param statusLulus {@link #LULUS} atau {@link #TIDAK_LULUS}.
	 */
	public void setStatusLulus(Integer statusLulus) {
		this.statusLulus = statusLulus;
	}

	/**
	 * Program studi "efektif" pendaftar ini: prodi tempat ia DITERIMA bila sudah ada, kalau belum
	 * dipakai pilihan pertamanya.
	 *
	 * <p>Dipakai laporan dan penyaringan yang perlu menempatkan setiap pendaftar pada satu prodi,
	 * baik yang sudah maupun yang belum diumumkan hasil seleksinya.</p>
	 *
	 * @return prodi lulus, atau prodi pilihan pertama; {@code null} bila keduanya kosong.
	 */
	public Jurusan ambilJurusan() {
		if (getProdiLulus() != null) {
			return getProdiLulus();
		} else {
			return getProdi1();
		}
	}

	/**
	 * Program studi tempat pendaftar DITERIMA — penanda utama bahwa seleksi sudah menghasilkan
	 * keputusan positif. Banyak properti lain bergantung padanya:
	 * {@link #getJenisSeleksiDipilih()}, {@link #getGelombangPendaftaranDiterima()}, dan
	 * {@link #getTanggalDiterima()} semuanya dikosongkan selama prodi lulus masih {@code null}.
	 *
	 * <p>Urutan penentuan:</p>
	 * <ol>
	 * <li>{@code null} bila pendaftar {@link #getMundur()} atau {@link #getDitolak()} (langsung
	 * keluar, tanpa menyentuh field);</li>
	 * <li>bila sudah ada {@link Mahasiswa} resmi, prodi lulus DITIMPA dengan
	 * {@code mahasiswa.getJurusan()} — mahasiswa resmi jadi sumber kebenaran setelah konversi;</li>
	 * <li>bila masih kosong dan gelombangnya bersifat "otomatis diterima saat daftar"
	 * ({@code GelombangPendaftaran#getOtomatisDiterimaSaatDaftar()}), prodi lulus diisi dari
	 * {@link #getProdi1()} — inilah mekanisme jalur tanpa seleksi.</li>
	 * </ol>
	 *
	 * @return prodi tempat diterima; {@code null} bila belum diterima, ditolak, atau mundur.
	 */
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

	/**
	 * Menetapkan program studi tempat pendaftar diterima. Dipanggil antara lain oleh
	 * {@code CommonPMB} saat memproses hasil seleksi, berbarengan dengan
	 * {@link #setStatusLulus(Integer)}.
	 *
	 * @param prodiLulus prodi tempat diterima; {@code null} untuk membatalkan penerimaan.
	 */
	public void setProdiLulus(Jurusan prodiLulus) {
		this.prodiLulus = prodiLulus;
	}

	/**
	 * Penanda bahwa NIM sudah pernah dibangkitkan untuk pendaftar ini (0/1, bawaan 0).
	 *
	 * @return penanda pembangkitan NIM.
	 * @see #getGenerateNimOtomatis()
	 * @see #getNim()
	 */
	@Column(name = "nim_generated", length = 1)
	public Integer getNimGenerated() {
		return nimGenerated;
	}

	/**
	 * Menetapkan penanda bahwa NIM sudah dibangkitkan.
	 *
	 * @param nimGenerated 0 atau 1.
	 */
	public void setNimGenerated(Integer nimGenerated) {
		this.nimGenerated = nimGenerated;
	}

	/**
	 * Menetapkan program perkuliahan. Ingat: {@link #getProgram()} dapat menimpanya mengikuti
	 * pengaturan gelombang.
	 *
	 * @param program nama program (mis. {@code "Reguler"}).
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Program perkuliahan yang diambil (Reguler, Karyawan, Kelas Malam, dsb.).
	 *
	 * <p>Urutan penentuan: nilai tersimpan; bila kosong diambil dari program milik
	 * {@link #getGelombangPendaftaran()}; bila masih kosong dipakai bawaan {@code "Reguler"}.
	 * Terakhir, bila gelombang menyetel {@code tidakBolehMemilihProgramLain} dan punya program
	 * sendiri, program DIPAKSA mengikuti gelombang — mengalahkan pilihan pendaftar. Semua hasil
	 * turunan tersimpan pada flush berikutnya.</p>
	 *
	 * @return nama program; tidak pernah {@code null}.
	 */
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

	/**
	 * Menetapkan jenis seleksi. Ingat: {@link #getJenisSeleksi()} menurunkan nilainya dari
	 * {@link #getJenisSeleksiDipilih()} atau gelombang.
	 *
	 * @param jenisSeleksi master jenis seleksi.
	 */
	public void setJenisSeleksi(JenisSeleksi jenisSeleksi) {
		this.jenisSeleksi = jenisSeleksi;
	}

	/**
	 * Jenis seleksi/jalur masuk yang BERLAKU bagi pendaftar ini (master {@link JenisSeleksi}).
	 *
	 * <p>Urutan: bila {@link #getJenisSeleksiDipilih()} sudah terisi — yaitu jalur yang ditetapkan
	 * saat pendaftar DITERIMA — jalur itulah yang dipakai; kalau belum, dipakai nilai tersimpan,
	 * dan bila itu pun kosong diambil dari jenis seleksi milik
	 * {@link #getGelombangPendaftaran()}.</p>
	 *
	 * <p>Nilai ini ikut disalin ke {@link Mahasiswa} saat konversi PMB.</p>
	 *
	 * @return master jenis seleksi; {@code null} bila tidak ada sumber sama sekali.
	 */
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

	/**
	 * Menetapkan NIM pada baris calon ini.
	 *
	 * <p><b>Perhatikan:</b> nilai yang diset di sini hanya bertahan selama {@link #getMahasiswa()}
	 * masih {@code null}; begitu mahasiswa resmi ada, {@link #getNim()} menimpanya. Dipakai
	 * {@code CommonPMB} sebagai penampung sementara NIM hasil {@code NimGenerator} sebelum
	 * {@code saveMahasiswa} dijalankan.</p>
	 *
	 * @param nim NIM hasil pembangkitan.
	 */
	public void setNim(String nim) {
		this.nim = nim;
	}

	/**
	 * NIM (Nomor Induk Mahasiswa) — <b>cermin murni dari mahasiswa resmi</b>, bukan data milik
	 * baris ini: nilainya diambil dari {@code getMahasiswa().getNim()}, dan bila mahasiswa resmi
	 * belum ada, field NIM justru DIKOSONGKAN ({@code null}).
	 *
	 * <p>Artinya NIM baru muncul setelah tahap pembangkitan NIM + konversi
	 * ({@code CommonPMB.saveMahasiswa}) selesai. Nilai yang disetel lewat {@link #setNim(String)}
	 * akan hilang begitu getter ini dipanggil dalam keadaan belum ada mahasiswa resmi.</p>
	 *
	 * @return NIM mahasiswa resmi; {@code null} bila pendaftar belum menjadi mahasiswa.
	 */
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

	/**
	 * Menetapkan tanggal pendaftaran.
	 *
	 * @param tanggalDaftar tanggal pendaftaran.
	 */
	public void setTanggalDaftar(Date tanggalDaftar) {
		this.tanggalDaftar = tanggalDaftar;
	}

	/**
	 * Tanggal (dan jam) pendaftaran; diisi waktu server saat objek dibuat.
	 *
	 * <p><b>Kuirk:</b> ada dua properti bermakna serupa — {@code tanggalDaftar} (kolom
	 * {@code tanggal_daftar}, dikembalikan apa adanya) dan {@link #getTanggalPendaftaran()} (kolom
	 * terpisah, diisi waktu server bila masih {@code null}). Keduanya dipelihara berdampingan;
	 * periksa layar/laporan yang bersangkutan sebelum memilih salah satu.</p>
	 *
	 * @return waktu pendaftaran; {@code null} bila sengaja dikosongkan.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_daftar")
	public Date getTanggalDaftar() {
		return tanggalDaftar;
	}

	/**
	 * Menetapkan tahun angkatan. Ingat: {@link #getTahun()} menurunkannya kembali dari tahun
	 * akademik bila formatnya memungkinkan.
	 *
	 * @param tahun tahun angkatan.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Tahun angkatan pendaftar, yang ikut disalin menjadi {@code tahunangkatan} pada
	 * {@link Mahasiswa} saat konversi.
	 *
	 * <p>Diturunkan dari {@link #getTahunAkademik()}: bagian sebelum garis miring (mis.
	 * {@code "2026/2027"} &rarr; 2026) dipakai bila berupa angka. Bila tahun akademik tidak
	 * tersedia/tidak berformat demikian dan field masih kosong, dipakai tahun berjalan. Hasil
	 * turunan tersimpan pada flush berikutnya.</p>
	 *
	 * @return tahun angkatan; tidak pernah {@code null}.
	 */
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

	/**
	 * Menetapkan penanda kartu peserta sudah dicetak.
	 *
	 * @param cetakKartu 0 atau 1.
	 */
	public void setCetakKartu(Integer cetakKartu) {
		this.cetakKartu = cetakKartu;
	}

	/**
	 * Penanda bahwa kartu peserta ujian sudah dicetak (bawaan 0).
	 *
	 * <p>Selain kolomnya sendiri, nilai ini bisa diangkat menjadi 1 oleh properti berkas
	 * {@code setCetakKartu} yang dibaca lewat {@code retreive(...)} milik
	 * {@link ais.database.model.GeneralValueObject} — mekanisme penyimpanan properti ringan di luar
	 * kolom tabel.</p>
	 *
	 * @return 1 bila kartu sudah dicetak; 0/{@code null} bila belum.
	 */
	@Column(name = "cetak_kartu")
	public Integer getCetakKartu() {
		String setCetakKartu = retreive("setCetakKartu");
		if (setCetakKartu != null && !setCetakKartu.isEmpty() && setCetakKartu.trim().equalsIgnoreCase("1")) {
			cetakKartu = 1;
		}
		return cetakKartu;
	}

	/**
	 * Menetapkan IPK pendidikan sebelumnya.
	 *
	 * @param ipk IPK sebagai teks.
	 */
	public void setIpk(String ipk) {
		this.ipk = ipk;
	}

	/**
	 * IPK jenjang pendidikan sebelumnya, disimpan sebagai TEKS (bukan angka) — relevan untuk
	 * pendaftar pascasarjana atau alih jenjang.
	 *
	 * @return IPK; {@code null} bila belum diisi.
	 */
	@Column(name = "ipk")
	public String getIpk() {
		return ipk;
	}

	/**
	 * Menetapkan nama jurusan S1 sebelumnya.
	 *
	 * @param jurusanS1 nama jurusan.
	 */
	public void setJurusanS1(String jurusanS1) {
		this.jurusanS1 = jurusanS1;
	}

	/**
	 * Nama jurusan S1 yang pernah ditempuh (teks bebas; diisi pendaftar pascasarjana).
	 *
	 * @return nama jurusan S1; {@code null} bila tidak relevan.
	 */
	@Column(name = "jurusan_s1")
	public String getJurusanS1() {
		return jurusanS1;
	}

	/**
	 * Menetapkan nama jurusan S2 sebelumnya.
	 *
	 * @param jurusanS2 nama jurusan.
	 */
	public void setJurusanS2(String jurusanS2) {
		this.jurusanS2 = jurusanS2;
	}

	/**
	 * Nama jurusan S2 yang pernah ditempuh (teks bebas; diisi pendaftar program doktor).
	 *
	 * @return nama jurusan S2; {@code null} bila tidak relevan.
	 */
	@Column(name = "jurusan_s2")
	public String getJurusanS2() {
		return jurusanS2;
	}

	/**
	 * Menetapkan nama berkas lampiran warisan.
	 *
	 * @param file nama/lokasi berkas.
	 */
	public void setFile(String file) {
		this.file = file;
	}

	/**
	 * Nama/lokasi berkas lampiran warisan. Lampiran yang dipakai sekarang dikelola lewat
	 * {@link ais.database.model.file.LampiranLain} dan
	 * {@link ais.database.model.file.FotoBiodataCalonMahasiswa}, bukan kolom ini.
	 *
	 * @return nama berkas; {@code null} bila tidak dipakai.
	 */
	@Column(name = "file")
	public String getFile() {
		return file;
	}

	/**
	 * Menetapkan kewarganegaraan asli.
	 *
	 * @param kewarganegaraan_asli kode kewarganegaraan asli.
	 */
	public void setKewarganegaraan_asli(String kewarganegaraan_asli) {
		this.kewarganegaraan_asli = kewarganegaraan_asli;
	}

	/**
	 * Kewarganegaraan ASLI pendaftar, terpisah dari {@link #getKewarganegaraan()}. Dipakai
	 * {@code CommonPMB.saveMahasiswa} untuk memutuskan apakah {@code Mahasiswa.negara} diisi
	 * Indonesia (nilainya sama dengan {@link Mahasiswa#WNI}) atau dibiarkan {@code null}.
	 *
	 * @return kode kewarganegaraan asli; {@code null} bila belum diisi.
	 */
	@Column(name = "kewarganegaraan_asli")
	public String getKewarganegaraan_asli() {
		return kewarganegaraan_asli;
	}

	/**
	 * Menetapkan kode program untuk penyusunan NIM.
	 *
	 * @param programNIM kode program pada pola NIM.
	 */
	public void setProgramNIM(String programNIM) {
		this.programNIM = programNIM;
	}

	/**
	 * Kode program yang dipakai sebagai salah satu segmen pola NIM oleh implementasi
	 * {@code NimGenerator} tertentu (berbeda dari {@link #getProgram()} yang merupakan nama program
	 * perkuliahan untuk tampilan).
	 *
	 * @return kode program NIM; {@code null} bila pola NIM institusi tidak memakainya.
	 */
	@Column(name = "program_nim")
	public String getProgramNIM() {
		return programNIM;
	}

	/**
	 * Menetapkan semester mulai. Ingat: {@link #getSemesterMulai()} menimpanya dari gelombang.
	 *
	 * @param semesterMulai jenis semester (mis. {@link Perkuliahan#GANJIL}).
	 */
	public void setSemesterMulai(String semesterMulai) {
		this.semesterMulai = semesterMulai;
	}

	/**
	 * Semester pertama perkuliahan bagi pendaftar ini (ganjil/genap).
	 *
	 * <p>Bila {@link #getGelombangPendaftaran()} ada, nilainya SELALU ditimpa dengan jenis semester
	 * milik gelombang tersebut (bukan hanya ketika kosong); bila tidak ada, bawaannya
	 * {@link Perkuliahan#GANJIL}. Ikut disalin ke {@link Mahasiswa} saat konversi.</p>
	 *
	 * @return jenis semester mulai; tidak pernah {@code null}.
	 */
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

	/**
	 * Tahun akademik pendaftaran (mis. {@code "2026/2027"}).
	 *
	 * <p>SELALU ditimpa dari {@link #getGelombangPendaftaran()} bila gelombangnya ada — gelombang
	 * adalah pemilik tahun akademik, baris calon hanya menyalinnya. Menjadi sumber turunan bagi
	 * {@link #getTahun()} dan penyaring pada
	 * {@link #hitungJumlahPendaftarKuota(Session, PaketJurusanPmb, GelombangPendaftaran, String,
	 * Long)}.</p>
	 *
	 * @return tahun akademik; {@code null} bila belum ada gelombang maupun nilai tersimpan.
	 */
	public String getTahunAkademik() {
		gelombangPendaftaran = getGelombangPendaftaran();
		if (gelombangPendaftaran != null) {
			tahunAkademik = gelombangPendaftaran.getTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik. Ingat: {@link #getTahunAkademik()} menimpanya dari gelombang.
	 *
	 * @param tahunAkademik tahun akademik.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Gelombang pendaftaran yang BERLAKU bagi pendaftar ini (master {@link GelombangPendaftaran})
	 * — penentu tahun akademik, semester mulai, program, jenis seleksi, status awal bawaan, dan
	 * daftar paket yang boleh dipilih.
	 *
	 * <p>Bila {@link #getGelombangPendaftaranDiterima()} sudah terisi — yaitu gelombang yang
	 * ditetapkan saat pendaftar DITERIMA — gelombang itulah yang menang. Pola "nilai saat diterima
	 * mengalahkan nilai saat mendaftar" ini sama dengan
	 * {@link #getJenisSeleksi()}/{@link #getJenisSeleksiDipilih()} dan
	 * {@link #getStatusAwalMahasiswa()}/{@link #getStatusAwalDiterima()}.</p>
	 *
	 * @return master gelombang pendaftaran; {@code null} bila belum dipilih.
	 */
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
