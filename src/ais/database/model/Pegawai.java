package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.net.URLEncoder;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.A;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.employ.helper.MasaKerjaUtil;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.Golongan;
import ais.database.model.employ.Insentif;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.employ.Makan;
import ais.database.model.employ.MasaKerja;
import ais.database.model.employ.Pendidikan;
import ais.database.model.employ.TipeMasaKerja;
import ais.database.model.employ.TipePegawai;
import ais.database.model.employ.Transport;
import ais.database.model.employ.UnitKerja;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.LampiranLain;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.library.Perpustakaan;
import ais.database.model.payroll.AsuransiPegawai;
import ais.database.model.payroll.Cabang;
import ais.database.model.payroll.Departemen;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.JatahCuti;
import ais.database.model.payroll.JenisGajiPegawai;
import ais.database.model.payroll.KelompokParameterTambahanGajiPegawai;
import ais.database.model.payroll.LevelJabatan;
import ais.database.model.payroll.PtkpPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sirs.Bagian;
import ais.ui.util.WaktuUtil;

/**
 * Entity kepegawaian (tabel {@code public.pegawai}) — <b>satu baris per orang yang digaji /
 * dikelola oleh modul HRD &amp; payroll</b>. Ini adalah "kartu induk kepegawaian": nomor induk,
 * biodata, unit kerja, jabatan, riwayat masa kerja, rekening pembayaran, asuransi, PTKP, dan
 * parameter penggajian tambahan semuanya berlabuh di sini.
 *
 * <h3>Posisi dalam domain: {@code Pegawai} versus {@link Dosen} versus {@link Guru}</h3>
 * <p>{@code Pegawai} dan {@link Dosen} adalah <b>saudara</b>, bukan induk-anak: keduanya
 * {@code extends} {@link Karyawan} {@code extends} {@link GeneralValueObject}. Pembagiannya bukan
 * "dosen vs non-dosen" seperti yang sering dikira, melainkan <b>peran akademik vs administrasi
 * kepegawaian</b>:</p>
 * <ul>
 * <li>{@link Dosen} menyimpan peran <i>akademik perguruan tinggi</i> (NIDN, jabatan fungsional
 * akademik, jurusan/fakultas, perkuliahan, penelitian).</li>
 * <li>{@link Guru} ({@code ais.database.model.sekolah.Guru}) menyimpan peran <i>akademik
 * sekolah</i> (NUPTK, sekolah, mata pelajaran).</li>
 * <li>{@code Pegawai} menyimpan peran <i>kepegawaian</i>: siapa pun yang menerima gaji —
 * staf administrasi, pustakawan, laboran, satpam, <b>maupun</b> dosen dan guru.</li>
 * </ul>
 * <p><b>Jadi overlap/dwifungsi bukan pengecualian, melainkan keadaan normal.</b> Satu orang yang
 * mengajar akan punya <i>dua</i> baris: satu di {@code dosen} (atau {@code guru}) dan satu di
 * {@code pegawai}, saling menunjuk. Ada tiga bentuk baris {@code Pegawai} yang mungkin, dan
 * {@link #getTipePegawai()} menyimpulkannya secara otomatis:</p>
 * <table border="1" summary="Bentuk baris Pegawai">
 * <tr><th>Kondisi</th><th>Arti</th><th>{@link TipePegawai} hasil</th></tr>
 * <tr><td>{@code dosen != null}</td><td>proyeksi kepegawaian dari seorang dosen</td>
 * <td>{@code TipePegawai.DOSEN}</td></tr>
 * <tr><td>{@code guru != null}</td><td>proyeksi kepegawaian dari seorang guru sekolah</td>
 * <td>{@code TipePegawai.GURU}</td></tr>
 * <tr><td>keduanya {@code null}</td><td>pegawai murni (tenaga kependidikan/administrasi)</td>
 * <td>{@code TipePegawai.STAF}</td></tr>
 * </table>
 * <p>Baris {@code Pegawai} untuk dosen/guru <b>dibuat otomatis</b>, bukan diinput manual — lihat
 * {@link #createDataPegawaiDariDosen(Session, Dosen)} dan
 * {@link #createDataPegawaiDariGuru(Session, Guru)} yang dipanggil antara lain saat dosen login
 * pertama kali. Tautan baliknya <b>tidak simetris</b>: {@code Pegawai.dosen} adalah relasi
 * Hibernate {@code @ManyToOne}, sedangkan {@code Dosen.pegawaiId} hanya {@code Long} biasa;
 * sebaliknya {@code Guru.pegawai} memang relasi penuh.</p>
 *
 * <h3>Pola paling penting: getter "cermin" (read-through) ke Dosen/Guru</h3>
 * <p>Untuk pegawai yang berasal dari dosen/guru, biodata <b>tidak</b> dianggap milik
 * {@code Pegawai}. Puluhan getter di kelas ini berbentuk:</p>
 * <pre>
 * public String getX() {
 *     dosen = getDosen();
 *     if (dosen != null) { this.x = dosen.getX(); }   // <b>menimpa</b> field lokal
 *     guru = getGuru();
 *     if (guru != null)  { this.x = guru.getX(); }     // guru menang atas dosen
 *     return x;
 * }
 * </pre>
 * <p>Konsekuensi yang wajib disadari:</p>
 * <ul>
 * <li>Nilai kolom {@code pegawai.x} di basis data <b>diabaikan</b> selama {@code dosen}/{@code guru}
 * terisi — kolom itu hanya bayangan yang ikut tertulis ulang saat flush berikutnya.</li>
 * <li>Getter-nya <b>tidak murni</b>: memodifikasi state objek dan dapat memicu inisialisasi proxy
 * lazy (query tambahan). Jangan panggil di dalam loop besar tanpa perlu.</li>
 * <li>Setter umumnya <b>hanya</b> menulis field lokal, sehingga langsung "hilang" pada pembacaan
 * berikutnya. Pengecualian yang menulis balik ke sumber: {@link #setAlamat(String)} dan
 * {@link #setKtp(String)}.</li>
 * <li>Urutan {@code dosen} lalu {@code guru} berarti bila (secara data kotor) keduanya terisi,
 * nilai dari {@link Guru} yang menang.</li>
 * </ul>
 * <p>Getter yang mengikuti pola cermin ini: {@link #getNama()}, {@link #getAlamat()},
 * {@link #getEmail()}, {@link #getTelp()}, {@link #getHp()}, {@link #getKelamin()},
 * {@link #getTempatlahir()}, {@link #getTanggallahir()}, {@link #getKtp()}, {@link #getNpwp()},
 * {@link #getCode()}, {@link #getMycode()}, {@link #getGolongan()}, {@link #getJabatan()},
 * {@link #getTetap()}, {@link #getIdfinger()}, {@link #getPendidikan()}, {@link #getAgama()},
 * {@link #getStatusPegawai()}, {@link #getStatusKepegawaian()}, {@link #getStatusPerkawinan()},
 * {@link #getAlamatJalan()}, {@link #getAlamatKelurahan()}, {@link #getAlamatKecamatan()},
 * {@link #getAlamatKabupaten()}, {@link #getAlamatPropinsi()}, {@link #getGelarDepan()},
 * {@link #getGelarBelakang()}, {@link #getLintang()}, {@link #getBujur()}.</p>
 *
 * <h3>Pengelompokan data</h3>
 * <ul>
 * <li><b>Identitas &amp; kontak</b> — {@code nama}, {@code panggilan}, {@code gelarDepan}/
 * {@code gelarBelakang}, {@code code} (NIP/NIK utama), {@code mycode}, {@code nipLama},
 * {@code ktp}, {@code npwp}, {@code nomorKartuKeluarga}, {@code namaIbuKandung},
 * {@code email} (boleh banyak, dipisah koma — lihat {@link #appendEmail(String)}), {@code telp},
 * {@code hp}, {@code idfinger} (ID pada mesin absensi sidik jari).</li>
 * <li><b>Kontak darurat</b> — {@code namaDarurat}, {@code telpDarurat}, {@code statusDarurat}
 * (hubungan: istri/anak/…); {@code golonganDarah} melengkapinya.</li>
 * <li><b>Alamat</b> — {@code alamat} bebas-teks plus rincian berjenjang {@code alamatJalan},
 * {@code alamatKelurahan}, {@code alamatKecamatan}, {@code alamatKabupaten},
 * {@code alamatPropinsi}, serta relasi {@link Propinsi}/{@link Kota} dan koordinat
 * {@code lintang}/{@code bujur}.</li>
 * <li><b>Ciri fisik</b> — {@code keteranganBadanTinggi}, {@code …Berat}, {@code …Rambut},
 * {@code …BentukMuka}, {@code …WarnaKulit}, {@code …CiriKhas}, {@code …Cacat} (dipakai untuk
 * kartu pegawai/berkas personalia).</li>
 * <li><b>Unit organisasi</b> — {@link UnitKerja}, {@link SatuanKerja} (unit anggaran, dengan
 * rantai fallback berlapis di {@link #getSatuanKerja()}), {@link Cabang}, {@link Departemen},
 * {@link Bagian} (SIRS/rumah sakit), serta {@code tendikFakultas}/{@code tendikJurusan}/
 * {@code tendikSekolah} sebagai penempatan tenaga kependidikan.</li>
 * <li><b>Status &amp; ikatan kerja</b> — {@link StatusPegawai} (aktif/cuti/pensiun …),
 * {@link StatusKepegawaian}, {@link IkatanKerjaDosen}, {@link TipePegawai}, {@link TipeMasaKerja},
 * {@link MasaKerja}, {@code tetap}, {@code aktif}, {@code usiaPensiun} (default 55),
 * {@link JenisTenagaKependidikan}, {@code sertifikasi}, {@link Pendidikan}.</li>
 * <li><b>Jabatan</b> — {@link JabatanStruktural}, {@link JabatanFungsional}, {@link LevelJabatan},
 * {@code pangkat}, {@code golongan}, {@code jabatan} (bebas-teks turunan), {@code tmtJabatan},
 * plus atasan: {@code atasanlangsung}/{@code atasanlangsung2}/{@code atasanlangsung3} (referensi
 * {@code Pegawai} lain) dan {@link JenisJabatan} {@code atasan}/{@code atasanPendukung}/
 * {@code atasanPendukungCadangan} (atasan berbasis <i>jenis jabatan</i>, dipakai alur persetujuan).</li>
 * <li><b>Penggajian</b> — sampai <b>lima slot pembayaran paralel</b>: {@link FormatItemGaji}
 * 1–5, {@link Bank} 1–5, {@code norek} 1–5, {@code ditransferAtasNama} 1–5, {@code caraPembayaran}
 * 1–5 (konstanta {@link #CARA_BAYAR_DITRASFER}, {@link #CARA_BAYAR_TUNAI},
 * {@link #CARA_BAYAR_LAINNYA}). Slot dipasangkan lewat {@link #ambilBank(FormatItemGaji)} dan
 * {@link #ambilNoRek(FormatItemGaji)}. Lengkapnya: {@link JenisGajiPegawai}, {@link PtkpPegawai}
 * (PTKP pajak), {@code nilaiGaji}, {@code tunjanganKinerja}, {@code persenKpiDefault},
 * {@code jpDefault}, {@code jumlahAnak}, {@code jamsostek}, {@code karis}, {@code askes},
 * {@code taspen}, {@code karpeg}, dan {@link AsuransiPegawai} 1–4 beserta nomor polisnya.</li>
 * <li><b>Cuti</b> — {@code jatahCutiTahunan} (default 12) dengan penimpaan per-tahun-masa-kerja
 * dari {@link JatahCuti}; lihat {@link #ambilJatahCuti()}.</li>
 * <li><b>Riwayat/berkas bebas</b> — {@code kedinasan}, {@code penghargaan}, {@code sangsi},
 * {@code keterangan}, {@code hobi}, {@code bahasa} (bahasa antarmuka), {@code deskripsiPendidikan},
 * {@code spesialisasi1..3}, {@code calonPegawai} (ID pelamar asal, bukan relasi).</li>
 * <li><b>Parameter tambahan gaji</b> — {@code parameterTambahan} dan {@code parameterTambahanInds}:
 * dua representasi teks multi-baris dari isian dinamis per pegawai. Lihat
 * {@link #populateParameterTambahan(java.util.List)} dan {@link #ambilDataParameterTambahan()}.</li>
 * <li><b>Jejak audit ringan</b> — {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, di samping
 * audit penuh Hibernate Envers lewat {@code @Audited}.</li>
 * </ul>
 *
 * <h3>Empat rentang masa kerja yang berjalan paralel</h3>
 * <p>Riwayat kerja tidak disimpan sebagai koleksi, melainkan sebagai empat pasang tanggal
 * mulai/selesai yang berdiri sendiri:</p>
 * <ol>
 * <li>{@code tanggalMulaiPengalanKerja}/{@code tanggalSampaiPengalanKerja} — pengalaman kerja
 * <i>sebelum</i> masuk institusi (nama field salah eja: "Pengalan" seharusnya "Pengalaman");</li>
 * <li>{@code tanggalmasukHonorer}/{@code tanggalkeluarHonorer};</li>
 * <li>{@code tanggalmasukSemiTetap}/{@code tanggalkeluarSemiTetap};</li>
 * <li>{@code tanggalmasuk}/{@code tanggalkeluar} — masa tetap.</li>
 * </ol>
 * <p>Getter keempat pasangan itu <b>disaring</b> oleh {@link #getTipeMasaKerja()}: pegawai berstatus
 * {@code Honorer} hanya "melihat" tanggal honorer, {@code Semi_Tetap} melihat honorer +
 * semi-tetap, {@code Tetap} melihat semuanya; di luar itu getter mengembalikan {@code null}
 * meski kolomnya berisi. {@link #getAwalmasuk()} menggabungkannya menjadi tanggal mulai bekerja
 * paling awal. Masing-masing punya sepasang penghitung {@code ambilMasaKerjaTahun*}/
 * {@code ambilMasaKerjaBulan*} berbasis {@link java.time.Period}.</p>
 *
 * <h3>Penggajian dinamis lewat {@link KenaikanPangkat}</h3>
 * <p>Komponen gaji tidak disimpan di baris pegawai, melainkan <b>dihitung ulang</b> untuk tanggal
 * tertentu. {@link #ambilKenaikanPangkat(Date)} memilih SK kenaikan pangkat yang berlaku pada
 * tanggal itu (dari cache {@code ConstantValues}, bukan query), lalu:</p>
 * <ul>
 * <li>{@link #ambilGajiPokok(Date)} — gaji pokok, dengan tiga jalur: nilai ditentukan langsung di
 * SK, dihitung otomatis dari golongan + masa kerja, atau dicari di tabel master
 * {@link GajiPokok} berdasarkan golongan &amp; tahun masa kerja;</li>
 * <li>{@link #ambilInsentif(Date)}, {@link #ambilMakan(Date)}, {@link #ambilTransport(Date)} —
 * pola pencarian yang sama untuk {@link Insentif}, {@link Makan}, {@link Transport};</li>
 * <li>{@link #getJabatanStruktural()} dan {@link #getJabatanFungsional()} — <b>diturunkan</b> dari
 * SK yang berlaku <i>kemarin</i> ({@code WaktuUtil.kemarin()}), bukan dari kolomnya sendiri.</li>
 * </ul>
 *
 * <h3>Hal-hal non-obvious</h3>
 * <ul>
 * <li><b>Field bayangan.</b> Sama seperti {@link Dosen}, kelas ini mendeklarasikan ulang
 * {@code code}, {@code mycode}, {@code nama}, {@code alamat}, {@code email}, {@code telp},
 * {@code kelamin}, {@code tempatlahir}, {@code pangkat}, {@code golongan}, {@code jabatan},
 * {@code spesialisasi1..3}, {@code tanggallahir}, {@code tetap}, dan {@code idfinger} meskipun
 * semuanya sudah ada di {@link Karyawan}. Getter/setter di sini menutupi versi induk (mis.
 * {@code Karyawan.getNama()} mengembalikan {@code code + "-" + nama}, versi ini tidak), dan yang
 * dipetakan Hibernate adalah accessor kelas ini. Field milik {@link Karyawan} praktis mati.</li>
 * <li><b>Yang <i>tidak</i> dibayangi.</b> {@code jurusan} dan {@code fakultas} di {@link Karyawan}
 * TIDAK dideklarasikan ulang dan getter-nya tidak di-override, sehingga
 * {@code pegawai.getJurusan()}/{@code getFakultas()} selalu {@code null} pada praktiknya.
 * Padanan yang benar-benar dipakai adalah {@link #getTendikJurusan()} dan
 * {@link #getTendikFakultas()}.</li>
 * <li><b>Getter dengan efek samping berat.</b> {@link #getSatuanKerja()} dapat menetapkan satuan
 * kerja dari user yang sedang login untuk entity baru; {@link #getAktif()} memaksa {@code false}
 * bila status bernama "Pensiun"; {@link #getIdfinger()} membaca cache berkas.</li>
 * <li><b>Ketahanan terhadap cache MapDB tertutup.</b> {@link #ambilKenaikanPangkat(Date, Collection)}
 * membungkus iterasi dengan {@code catch (Throwable)} karena getter ini dipanggil Hibernate saat
 * flush; bila cache sudah ditutup (shutdown/redeploy) iterasi melempar {@code Error}, dan
 * membiarkannya lolos akan menggagalkan penyimpanan data.</li>
 * <li><b>Ketahanan terhadap sesi tertutup.</b> {@link #getTendikFakultas()},
 * {@link #getTendikJurusan()}, dan {@link #getTendikSekolah()} menangkap
 * {@code HibernateException} agar laporan yang membaca objek detached tidak gagal.</li>
 * <li><b>Warisan salin-tempel dari {@link Dosen}.</b> Komentar generator di atas kelas ini tertulis
 * "Dosen generated by hbm2java" dan {@code serialVersionUID}-nya identik dengan milik
 * {@link Dosen}. Tidak berbahaya, tapi menjelaskan asal-usul banyak kemiripan struktur.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see Karyawan
 * @see Dosen
 * @see Guru
 * @see KenaikanPangkat
 * @see MasaKerjaUtil
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pegawai")
public class Pegawai extends Karyawan {

	/**
	 * Versi serialisasi Java. Nilainya dikunci agar objek {@code Pegawai} yang tersimpan di sesi ZK
	 * atau cache berkas tetap terbaca setelah kelas ini ditambahi field baru. Catatan: nilainya
	 * kebetulan sama persis dengan {@link Dosen#serialVersionUID} karena kelas ini dulu disalin
	 * dari sana.
	 */
	private static final long serialVersionUID = -5130925140455694214L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris pegawai ini (jejak audit ringan,
	 * terpisah dari audit penuh Envers).
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna pengubah terakhir. Nilai {@code null} atau kosong <b>diabaikan</b> supaya
	 * jejak audit yang sudah tercatat tidak terhapus oleh proses latar yang tidak membawa konteks
	 * pengguna (mis. job terjadwal).
	 *
	 * @param olehId ID pengguna pengubah; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * null/kosong diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris pegawai ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pencatatan stempel waktu/pengguna ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum Hibernate
	 * menulis {@code UPDATE} untuk baris ini. Dipanggil oleh provider persistence, tidak pernah
	 * dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan objek dan diperbarui
	 * lewat {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log dan komponen ZK berbentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Sengaja membaca <b>field</b> {@code nama} secara langsung, bukan {@link #getNama()},
	 * sehingga tidak memicu resolusi proxy {@link Dosen}/{@link Guru}. Efeknya: untuk pegawai yang
	 * berasal dari dosen/guru dan belum pernah dibaca lewat {@code getNama()}, bagian nama bisa
	 * tampil {@code null}.</p>
	 *
	 * @return string {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String idfinger;
	private String code;
	private String mycode;
	private String nama;
	private OrangTua orangTua;
	private String ktp;
	private String alamat;
	private String email;
	private String telp;
	private String telpDarurat;
	private String namaDarurat;
	private String statusDarurat;
	private String golonganDarah;
	private String nomorKartuKeluarga;
	private String namaIbuKandung;
	private String kelamin;
	private String tempatlahir;
	private String pangkat;
	private String golongan;
	private String jabatan;
	private String spesialisasi1;
	private String spesialisasi2;
	private String spesialisasi3;
	private Date tanggallahir;
	private Date awalmasuk;
	private Date tanggalmasuk;
	private Date tanggalkeluar;
	private Date tanggalmasukHonorer;
	private Date tanggalkeluarHonorer;
	private Date tanggalmasukSemiTetap;
	private Date tanggalkeluarSemiTetap;
	private Date tanggalMulaiPengalanKerja;
	private Date tanggalSampaiPengalanKerja;
	private String jamsostek;

	private Bagian bagian;
	private UnitKerja unitKerja;
	private IkatanKerjaDosen ikatanKerjaDosen;
	private TipePegawai tipePegawai;

	private TipeMasaKerja tipeMasaKerja;
	private MasaKerja masaKerja;
	private Integer tetap = 0;

	private StatusPegawai statusPegawai;
	private SatuanKerja satuanKerja;

	private Agama agama;
	private String statusPerkawinan;
	private String alamatJalan;
	private String alamatKelurahan;
	private Boolean sertifikasi;
	private String alamatKecamatan;
	private String alamatKabupaten;
	private String alamatPropinsi;
	private Pendidikan pendidikan;
	private String deskripsiPendidikan;
	private JenisGajiPegawai jenisGajiPegawai;
	private Propinsi propinsi;
	private Kota kota;

	private String keteranganBadanTinggi;
	private String keteranganBadanBerat;
	private String keteranganBadanRambut;
	private String keteranganBadanBentukMuka;
	private String keteranganBadanWarnaKulit;
	private String keteranganBadanCiriKhas;
	private String keteranganBadanCacat;

	/** Nilai {@code jenis}: pegawai dengan jabatan fungsional. @see #getJenis() */
	public static final String JENIS_FUNGSIONAL = "Fungsional";
	/** Nilai {@code jenis}: pegawai dengan jabatan struktural. @see #getJenis() */
	public static final String JENIS_STRUKTURAL = "Struktural";
	/** Nilai {@code jenis}: pegawai honorer/kontrak. @see #getJenis() */
	public static final String JENIS_HONORER = "Honorer";
	/**
	 * Nilai {@code jenis}: tenaga alih daya (<i>outsourcing</i>). Ejaan konstanta memang
	 * "Outsourching" dan nilainya tersimpan apa adanya di basis data — jangan diperbaiki tanpa
	 * migrasi data.
	 *
	 * @see #getJenis()
	 */
	public static final String JENIS_OUTSOURCHING = "Outsourching";

	/**
	 * Nilai {@code caraPembayaran}: gaji ditransfer ke rekening bank. Ini <b>default</b> untuk slot
	 * pembayaran pertama ({@link #getCaraPembayaran()}). Ejaan konstanta memang kurang satu huruf
	 * ("DITRASFER"); nilai stringnya benar.
	 */
	public static final String CARA_BAYAR_DITRASFER = "Transfer";
	/** Nilai {@code caraPembayaran}: gaji dibayar tunai. */
	public static final String CARA_BAYAR_TUNAI = "Tunai";
	/**
	 * Nilai {@code caraPembayaran}: cara pembayaran lain. Ini default untuk slot pembayaran
	 * ke-2 sampai ke-5 ({@link #getCaraPembayaran2()} dan seterusnya).
	 */
	public static final String CARA_BAYAR_LAINNYA = "Lain-lain";

	private String jenis;

	private String panggilan;
	private String gelarDepan;
	private String gelarBelakang;
	private String darah;
	private String hp;

	private Bank bank;
	private String norek;

	private Bank bank2;
	private String norek2;

	private Bank bank3;
	private String norek3;

	private Bank bank4;
	private String norek4;

	private Bank bank5;
	private String norek5;

	private String ditransferKe;
	private String karis;
	private String askes;
	private String taspen;
	private String karpeg;
	private String npwp;
	private String keterangan;
	private String nipLama;
	private Date tmtJabatan;

	private String hobi;

	private Integer usiaPensiun;
	private Dosen dosen;
	private String bahasa;

	private Cabang cabang;
	private Departemen departemen;
	private LevelJabatan levelJabatan;
	private String caraPembayaran;
	private String ditransferAtasNama;
	private String ditransferAtasNama2;
	private String ditransferAtasNama3;
	private String ditransferAtasNama4;
	private String ditransferAtasNama5;
	private Integer jumlahAnak;
	private JenisTenagaKependidikan jenisTenagaKependidikan;
	private Guru guru;
	private Boolean aktif;
	private Double tunjanganKinerja;
	private StatusKepegawaian statusKepegawaian;

	private JenisJabatan atasan;
	private JenisJabatan atasanPendukung;
	private JenisJabatan atasanPendukungCadangan;
	private String kedinasan;
	private String penghargaan;
	private String sangsi;
	private Pegawai atasanlangsung;
	private Pegawai atasanlangsung2;
	private Pegawai atasanlangsung3;
	private Integer jatahCutiTahunan;

	private FormatItemGaji formatItemGaji;
	private JabatanFungsional jabatanFungsional;
	private JabatanStruktural jabatanStruktural;

	private Long calonPegawai;
	private String lintang;
	private String bujur;

	private PtkpPegawai ptkpPegawai;

	private AsuransiPegawai asuransiPegawai1;
	private AsuransiPegawai asuransiPegawai2;
	private AsuransiPegawai asuransiPegawai3;
	private AsuransiPegawai asuransiPegawai4;

	private String nomorAsuransiPegawai1;
	private String nomorAsuransiPegawai2;
	private String nomorAsuransiPegawai3;
	private String nomorAsuransiPegawai4;

	private Double nilaiGaji;
	private Double persenKpiDefault;
	private Double jpDefault;
	private String parameterTambahan;
	private String parameterTambahanInds;

	private Fakultas tendikFakultas;
	private Jurusan tendikJurusan;
	private Sekolah tendikSekolah;
	private String caraPembayaran2;
	private String caraPembayaran3;
	private String caraPembayaran4;
	private String caraPembayaran5;
	private FormatItemGaji formatItemGaji2;
	private FormatItemGaji formatItemGaji3;
	private FormatItemGaji formatItemGaji4;
	private FormatItemGaji formatItemGaji5;

	/**
	 * Varian "berdiri sendiri" dari {@link #createDataPegawaiDariDosen(Session, Dosen)} yang
	 * membuka {@code currentNativeSession()} sendiri, mengerjakan provisioning, lalu menutup sesi
	 * itu kembali.
	 *
	 * <p><b>Kapan memakai yang mana:</b> gunakan varian ini hanya dari konteks yang <i>tidak</i>
	 * sedang berada di dalam unit-of-work request (mis. job latar atau utilitas baris perintah).
	 * Dari dalam event ZK atau servlet, pakai varian bersession agar tidak membuka koneksi kedua
	 * yang bisa saling mengunci baris yang sama.</p>
	 *
	 * @param dosen dosen yang ingin dipastikan punya baris {@code Pegawai}
	 * @return baris {@code Pegawai} milik dosen tersebut (yang sudah ada atau yang baru dibuat)
	 * @see #createDataPegawaiDariDosen(Session, Dosen)
	 */
	public static Pegawai createDataPegawaiDariDosen(Dosen dosen) {
		Session session = HibernateUtil.currentNativeSession();
		Pegawai pegawai = createDataPegawaiDariDosen(session, dosen);
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		return pegawai;
	}

	/**
	 * <b>Auto-provisioning kepegawaian untuk seorang dosen</b>: memastikan setiap {@link Dosen}
	 * punya tepat satu baris {@code Pegawai} yang aktif, dan bahwa kedua baris saling menunjuk.
	 * Ini adalah jembatan utama antara modul akademik dan modul kepegawaian/payroll.
	 *
	 * <p>Alur kerjanya:</p>
	 * <ol>
	 * <li>Pastikan {@code session} masih terbuka lewat {@code HibernateUtil.ensureOpenSession()}
	 * (lihat komentar AKAR di dalam kode — pemanggil bisa mengoper sesi yang sudah tertutup).</li>
	 * <li>Cari {@code Pegawai} dengan {@code dosen} = dosen ini dan {@code aktif} null/true,
	 * ambil ID terbesar. Bila tidak ada, ulangi pencarian tanpa syarat {@code aktif} (mengambil
	 * kembali baris yang sudah dinonaktifkan alih-alih membuat duplikat).</li>
	 * <li>Bila tetap tidak ada, <b>buat baris baru</b> dalam transaksinya sendiri, lalu salin foto
	 * dosen ({@link FotoDosen}) menjadi foto pegawai ({@link FotoPegawai}) melalui
	 * {@code StreamingHibernateUtil} — sesi terpisah khusus data biner besar.</li>
	 * <li>Terakhir, tulis balik {@code Dosen.pegawaiId} agar tautan dua arah lengkap. Dosen dimuat
	 * <b>ulang</b> di sesi yang sama sebelum di-update untuk menghindari galat "illegally attempted
	 * to associate a proxy with two open Sessions".</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> menulis ke basis data (INSERT {@code pegawai}, INSERT
	 * {@code foto_pegawai}, UPDATE {@code dosen}) dan mengelola transaksinya sendiri. Kegagalan
	 * pada langkah penulisan balik <b>tidak</b> dilempar ke pemanggil, hanya ditampilkan lewat
	 * {@code Common.tampilErrorJikaAdmin()}, sehingga login dosen tidak ikut gagal.</p>
	 *
	 * <p><b>Dipanggil dari:</b> jalur login/otorisasi dosen ({@code FilterLoginAis} lewat
	 * {@code checkBlokirDanKuotaDosen()}) dan layar-layar master kepegawaian yang perlu menyiapkan
	 * data pegawai untuk dosen yang belum terdaftar di HRD.</p>
	 *
	 * <p><b>Kuirk yang ditemukan (tidak diperbaiki di sini):</b> syarat penulisan balik berbunyi
	 * {@code dosen.getPegawaiId() == null || !dosen.getId().equals(pegawai.getId())} — bagian kedua
	 * membandingkan PK <i>dosen</i> dengan PK <i>pegawai</i>, dua ruang ID yang tidak berhubungan.
	 * Kemungkinan yang dimaksud adalah {@code dosen.getPegawaiId()}. Akibatnya blok update
	 * hampir selalu dieksekusi ulang (boros, tapi idempoten).</p>
	 *
	 * @param session sesi Hibernate milik pemanggil; boleh sudah tertutup, akan diganti otomatis
	 * @param dosen   dosen yang ingin dipastikan punya baris {@code Pegawai}
	 * @return baris {@code Pegawai} milik dosen tersebut, tidak pernah {@code null}
	 */
	public static Pegawai createDataPegawaiDariDosen(Session session, Dosen dosen) {

		// FIX SessionException "Session is closed!" (ForeignKeys$Nullifier.errorIfClosed) saat
		// auto-provisioning Pegawai untuk Dosen yang login pertama kali via FilterLoginAis.
		// checkBlokirDanKuotaDosen() mengoper session native miliknya sendiri ke sini, tapi pada
		// kondisi tertentu (mis. request lain di thread yang sama sempat menutup ThreadLocal native
		// session ini lebih dulu) session yang diterima bisa sudah closed di titik ini. Pakai pola
		// baku HibernateUtil.ensureOpenSession() (dipakai di banyak tempat lain utk kasus sama)
		// sebelum query/save apa pun -- no-op bila session masih hidup, atau ambil pengganti native
		// session yang open bila sudah mati, sehingga save() di bawah tidak lagi menabrak session
		// tertutup.
		session = HibernateUtil.ensureOpenSession(session);

		Pegawai pegawai = (Pegawai) (ConstantValues
				.simpleObject(session.createCriteria(Pegawai.class).add(Restrictions.eq("dosen", dosen))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.desc("id")).setMaxResults(1), Pegawai.class));

		if (pegawai == null) {
			pegawai = (Pegawai) (ConstantValues.simpleObject(session.createCriteria(Pegawai.class)
					.add(Restrictions.eq("dosen", dosen)).addOrder(Order.desc("id")).setMaxResults(1), Pegawai.class));

		}

		if (pegawai == null) {
			pegawai = new Pegawai();
			pegawai.setDosen(dosen);
			session.getTransaction().begin();
			session.save(pegawai);
			session.getTransaction().commit();

			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			FotoDosen fotoDosen = (FotoDosen) streamingSession.createCriteria(FotoDosen.class)
					.add(Restrictions.eq("dosen", dosen.getId())).setMaxResults(1).uniqueResult();
			if (fotoDosen != null) {
				FotoPegawai fotoPegawai = new FotoPegawai();
				fotoPegawai.setNama(fotoDosen.getNama());
				fotoPegawai.setKeterangan(fotoDosen.getKeterangan());
				fotoPegawai.setPegawai(pegawai.getId());

				fotoPegawai.setFoto(fotoDosen.getFoto());

				streamingSession.getTransaction().begin();
				streamingSession.save(fotoPegawai);
				streamingSession.getTransaction().commit();
			}
			StreamingHibernateUtil.getInstance().closeSession();
		}

		if (dosen.getPegawaiId() == null || !dosen.getId().equals(pegawai.getId())) {
			// Sama seperti jalur Guru: hindari proxy dua session + konflik transaksi dengan
			// Common.refreshSaveOrUpdate. Muat ulang Dosen di session yang sama, simpan bersih.
			org.hibernate.Transaction txDosen = null;
			try {
				txDosen = session.beginTransaction();
				Dosen dosenDb = (Dosen) session.get(Dosen.class, dosen.getId());
				if (dosenDb != null) {
					dosenDb.setPegawaiId(pegawai.getId());
					session.update(dosenDb);
				}
				txDosen.commit();
			} catch (Exception exDosen) {
				if (txDosen != null && txDosen.isActive()) {
					try {
						txDosen.rollback();
					} catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:389");
					}
				}
				Common.tampilErrorJikaAdmin(exDosen);
			}
		}
		return pegawai;
	}

	/**
	 * Varian praktis {@link #createDataPegawaiDariGuru(Session, Guru)} yang memakai
	 * {@code HibernateUtil.currentSession()} — <b>unit-of-work request yang sedang berjalan</b>,
	 * bukan sesi native baru.
	 *
	 * <p>Perbedaannya dengan padanan untuk dosen memang disengaja dan penting: method ini biasanya
	 * dipanggil dari event ZK yang baru saja menyimpan {@link Guru}. Membuka sesi native kedua lalu
	 * meng-{@code UPDATE} baris {@code guru} yang sama akan membuat koneksi kedua menunggu lock
	 * milik transaksi request sendiri sampai {@code statement_timeout} (deadlock diri sendiri).
	 * Karena itu sesi request dipakai ulang dan <b>tidak boleh</b> ditutup manual di sini.</p>
	 *
	 * @param guru guru yang ingin dipastikan punya baris {@code Pegawai}
	 * @return baris {@code Pegawai} milik guru tersebut
	 * @see #createDataPegawaiDariGuru(Session, Guru)
	 */
	public static Pegawai createDataPegawaiDariGuru(Guru guru) {
		// Dipanggil dari event ZK yang baru saja menyimpan Guru lewat currentSession().
		// Membuka currentNativeSession() kedua lalu UPDATE baris Guru yang sama membuat
		// koneksi kedua menunggu lock milik transaksi request sendiri sampai statement_timeout.
		// Gunakan unit-of-work request yang sama; currentSession dikelola framework dan tidak
		// boleh ditutup manual di sini.
		return createDataPegawaiDariGuru(HibernateUtil.currentSession(), guru);
	}

	/**
	 * <b>Auto-provisioning kepegawaian untuk seorang guru sekolah</b> — padanan
	 * {@link #createDataPegawaiDariDosen(Session, Dosen)} pada jalur {@link Guru}.
	 *
	 * <p>Alurnya sejajar: cari {@code Pegawai} aktif milik guru ini, jatuh ke pencarian tanpa
	 * syarat {@code aktif} bila tidak ketemu, lalu buat baru sambil menyalin {@link FotoGuru}
	 * menjadi {@link FotoPegawai} lewat {@code StreamingHibernateUtil}. Terakhir tautan balik
	 * {@code Guru.pegawai} diisi. Berbeda dengan jalur dosen, di sini tautannya adalah relasi
	 * Hibernate penuh, bukan sekadar kolom ID.</p>
	 *
	 * <p><b>Penanganan transaksi yang perlu diperhatikan.</b> Method ini mendeteksi apakah
	 * {@code session} sudah punya transaksi aktif milik pemanggil:</p>
	 * <ul>
	 * <li><b>Ada transaksi pemanggil</b> — method menumpang di dalamnya dan tidak commit sendiri.
	 * Bila terjadi galat, galat itu <b>dilempar ulang</b> sebagai {@link RuntimeException} karena
	 * transaksi request sudah tidak aman dilanjutkan dan harus di-rollback oleh framework.</li>
	 * <li><b>Tidak ada transaksi pemanggil</b> — method membuka dan menutup transaksinya sendiri,
	 * dan galat hanya dilaporkan lewat {@code Common.tampilErrorJikaAdmin()}.</li>
	 * </ul>
	 *
	 * <p><b>Efek samping:</b> INSERT {@code pegawai}, INSERT {@code foto_pegawai}, UPDATE
	 * {@code guru}. Guru dimuat ulang di sesi yang sama sebelum di-update untuk menghindari
	 * "illegally attempted to associate a proxy with two open Sessions".</p>
	 *
	 * @param session sesi Hibernate yang akan dipakai; boleh membawa transaksi aktif
	 * @param guru    guru yang ingin dipastikan punya baris {@code Pegawai}
	 * @return baris {@code Pegawai} milik guru tersebut
	 * @throws RuntimeException bila penulisan tautan balik gagal <i>dan</i> transaksi milik
	 *                          pemanggil sedang aktif
	 */
	public static Pegawai createDataPegawaiDariGuru(Session session, Guru guru) {

		Pegawai pegawai = (Pegawai) (ConstantValues.simpleObject(session.createCriteria(Pegawai.class)
				.add(Restrictions.eq("guru", guru))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1),
				Pegawai.class));

		if (pegawai == null) {
			pegawai = (Pegawai) (ConstantValues.simpleObject(session.createCriteria(Pegawai.class)
					.add(Restrictions.eq("guru", guru)).addOrder(Order.desc("id")).setMaxResults(1), Pegawai.class));

		}

		if (pegawai == null) {
			pegawai = new Pegawai();
			pegawai.setGuru(guru);
			session.save(pegawai);

			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			FotoGuru fotoGuru = (FotoGuru) streamingSession.createCriteria(FotoGuru.class)
					.add(Restrictions.eq("guru", guru.getId())).setMaxResults(1).uniqueResult();
			if (fotoGuru != null) {
				FotoPegawai fotoPegawai = new FotoPegawai();
				fotoPegawai.setNama(fotoGuru.getNama());
				fotoPegawai.setKeterangan(fotoGuru.getKeterangan());
				fotoPegawai.setPegawai(pegawai.getId());

				fotoPegawai.setFoto(fotoGuru.getFoto());

				streamingSession.getTransaction().begin();
				streamingSession.save(fotoPegawai);
				streamingSession.getTransaction().commit();
			}
			StreamingHibernateUtil.getInstance().closeSession();
		}

		if (guru.getPegawai() == null || !guru.getPegawai().getId().equals(pegawai.getId())) {
			// AKAR: guru berasal dari session lain -> menyimpannya di `session` (currentNativeSession)
			// memicu "illegally attempted to associate a proxy with two open Sessions". Selain itu
			// Common.refreshSaveOrUpdate() mengelola/rollback transaksinya sendiri sehingga
			// commit manual di baris berikut gagal ("Transaction not successfully started").
			// FIX: muat ULANG Guru pada session yang sama, set pegawai, simpan dalam SATU transaksi bersih.
			org.hibernate.Transaction txGuru = null;
			boolean transaksiMilikPemanggil = session.getTransaction() != null
					&& session.getTransaction().isActive();
			try {
				if (!transaksiMilikPemanggil) {
					txGuru = session.beginTransaction();
				}
				Guru guruDb = (Guru) session.get(Guru.class, guru.getId());
				if (guruDb != null && (guruDb.getPegawai() == null
						|| !pegawai.getId().equals(guruDb.getPegawai().getId()))) {
					guruDb.setPegawai(pegawai);
					session.update(guruDb);
					session.flush();
				}
				if (!transaksiMilikPemanggil && txGuru != null) {
					txGuru.commit();
				}
				guru.setPegawai(pegawai);
			} catch (Exception exGuru) {
				if (txGuru != null && txGuru.isActive()) {
					try {
						txGuru.rollback();
					} catch (Exception ign) { ais.common.ErrorAuditUtil.record(ign, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:465");
					}
				}
				if (transaksiMilikPemanggil) {
					// Jangan menelan kegagalan flush pada currentSession: transaksi request
					// sudah tidak aman untuk dilanjutkan dan harus di-rollback oleh framework.
					throw new RuntimeException("Gagal mengaitkan data Guru dengan Pegawai dalam transaksi yang sama", exGuru);
				}
				Common.tampilErrorJikaAdmin(exGuru);
			}
		}
		return pegawai;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA untuk instansiasi lewat refleksi. Juga
	 * dipakai layar input saat membuat pegawai baru.
	 */
	public Pegawai() {
	}

	/**
	 * Membuat instance "kerangka" yang hanya membawa ID. Berguna untuk membangun kriteria/relasi
	 * tanpa memuat baris penuh.
	 *
	 * <p><b>Perhatian:</b> objek hasil konstruktor ini <i>transient</i>, bukan hasil
	 * {@code session.load()}. Seluruh field lain masih {@code null}, jadi jangan disimpan
	 * ({@code save}/{@code update}) karena akan menimpa baris asli dengan nilai kosong.</p>
	 *
	 * @param id PK baris {@code pegawai}
	 */
	public Pegawai(Long id) {
		this.id = id;
	}

	/**
	 * Membuat proyeksi kepegawaian dari sebuah {@link Dosen} <b>tanpa menyentuh basis data</b>.
	 * Bila dosen sudah punya tautan {@code pegawaiId}, ID itu ikut dipasang sehingga objek ini
	 * merujuk baris yang benar; bila belum, objek tetap tanpa ID (belum tersimpan).
	 *
	 * <p>Ini jalur "ringan": tidak membuat baris baru dan tidak menyalin foto. Untuk provisioning
	 * yang sungguhan pakai {@link #createDataPegawaiDariDosen(Session, Dosen)}.</p>
	 *
	 * @param dosen dosen sumber; boleh {@code null}
	 */
	public Pegawai(Dosen dosen) {
		if (dosen != null && dosen.getPegawaiId() != null) {
			this.id = dosen.getPegawaiId();
		}
		this.dosen = dosen;
	}

	/**
	 * Konstruktor biodata ringkas warisan hasil <i>generate</i> hbm2java. Mengisi langsung field
	 * lokal tanpa melewati setter, jadi tidak ada penulisan balik ke {@link Dosen}/{@link Guru}.
	 *
	 * @param nama         nama lengkap pegawai
	 * @param alamat       alamat bebas-teks
	 * @param email        alamat surel (boleh beberapa, dipisah koma)
	 * @param telp         nomor telepon
	 * @param kelamin      jenis kelamin ("Laki-laki"/"Perempuan", lihat {@link #getKelamin()})
	 * @param tempatlahir  kota kelahiran
	 * @param tanggallahir tanggal lahir
	 */
	public Pegawai(String nama, String alamat, String email, String telp, String kelamin, String tempatlahir,
			Date tanggallahir) {
		this.nama = nama;
		this.alamat = alamat;
		this.email = email;
		this.telp = telp;
		this.kelamin = kelamin;
		this.tempatlahir = tempatlahir;
		this.tanggallahir = tanggallahir;
	}

	/**
	 * Primary key baris {@code pegawai}, dibangkitkan oleh basis data ({@code IDENTITY}/sequence).
	 * Kolomnya {@code insertable = false} sehingga nilai yang di-set manual diabaikan saat INSERT.
	 *
	 * @return PK pegawai, atau {@code null} bila entity belum pernah disimpan
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi PK. Praktis hanya dipakai Hibernate dan konstruktor kerangka
	 * {@link #Pegawai(Long)}.
	 *
	 * @param id PK pegawai
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama lengkap pegawai — <b>getter cermin</b>: bila pegawai ini berasal dari {@link Dosen} atau
	 * {@link Guru}, nama diambil dari sana dan menimpa field lokal (guru menang atas dosen). Nilai
	 * {@code null} dinormalkan menjadi string kosong, dan hasil akhirnya di-{@code trim()}.
	 *
	 * <p>Berbeda dengan {@code Karyawan.getNama()} yang mengembalikan {@code code + "-" + nama},
	 * versi ini mengembalikan nama saja.</p>
	 *
	 * <p><b>Efek samping:</b> memicu resolusi proxy {@code dosen}/{@code guru} dan menulis ke field
	 * {@code nama}, {@code dosen}, {@code guru}.</p>
	 *
	 * @return nama pegawai tanpa spasi tepi; string kosong bila tidak ada data
	 */
	@Column(name = "nama", length = 150)
	public String getNama() {

		dosen = getDosen();
		if (dosen != null) {
			this.nama = dosen.getNama();
		}

		guru = getGuru();
		if (guru != null) {
			this.nama = guru.getNama();
		}

		if (nama == null) {
			nama = "";
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama pada field lokal saja. Untuk pegawai turunan dosen/guru nilai ini akan tertimpa
	 * lagi pada pemanggilan {@link #getNama()} berikutnya — ubah nama di {@link Dosen}/{@link Guru}
	 * bila ingin permanen.
	 *
	 * @param nama nama lengkap
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Alamat bebas-teks — <b>getter cermin</b> ke {@code Dosen.getAlamat()} /
	 * {@code Guru.getAlamatGuru()}.
	 *
	 * @return alamat pegawai, atau {@code null} bila belum diisi
	 * @see #getAlamatJalan()
	 */
	@Column(name = "alamat")
	public String getAlamat() {
		dosen = getDosen();
		if (dosen != null) {
			this.alamat = dosen.getAlamat();
		}

		guru = getGuru();
		if (guru != null) {
			this.alamat = guru.getAlamatGuru();
		}

		return this.alamat;
	}

	/**
	 * Mengisi alamat. <b>Salah satu dari sedikit setter yang menulis balik ke sumber</b>: bila
	 * pegawai ini turunan dosen/guru, alamat ikut di-set pada {@link Dosen} dan/atau {@link Guru}
	 * sehingga perubahan tidak hilang saat {@link #getAlamat()} dipanggil lagi.
	 *
	 * <p><b>Efek samping:</b> memutasi entity {@code Dosen}/{@code Guru} yang terkait; bila
	 * keduanya <i>managed</i>, perubahan itu akan ikut tersimpan pada flush berikutnya.</p>
	 *
	 * @param alamat alamat bebas-teks
	 */
	public void setAlamat(String alamat) {
		dosen = getDosen();
		if (dosen != null) {
			dosen.setAlamat(alamat);
		}

		guru = getGuru();
		if (guru != null) {
			guru.setAlamatGuru(alamat);
		}
		this.alamat = alamat;
	}

	/**
	 * Alamat surel pegawai. Kolom ini boleh berisi <b>beberapa alamat sekaligus, dipisah koma</b>
	 * (lihat {@link #appendEmail(String)}), sehingga getter membersihkan koma ganda dan nilai yang
	 * hanya berisi koma menjadi string kosong.
	 *
	 * <p><b>Kuirk:</b> setelah pembersihan itu, nilai tetap ditimpa dari {@link Dosen}/{@link Guru}
	 * bila ada — jadi untuk pegawai turunan, hasil normalisasi di awal method terbuang percuma dan
	 * yang dikembalikan adalah surel mentah milik dosen/guru.</p>
	 *
	 * @return daftar surel dipisah koma; string kosong bila tidak ada
	 */
	@Column(name = "email", length = 255)
	public String getEmail() {
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
		dosen = getDosen();
		if (dosen != null) {
			this.email = dosen.getEmail();
		}

		guru = getGuru();
		if (guru != null) {
			this.email = guru.getAlamatEmail();
		}

		return this.email;
	}

	/**
	 * Mengganti seluruh daftar surel dengan nilai baru (tanpa validasi). Untuk <i>menambah</i> satu
	 * alamat pakai {@link #appendEmail(String)}.
	 *
	 * @param email daftar surel dipisah koma
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Menambahkan satu alamat surel ke daftar, dipisah koma, dengan tiga penjagaan: alamat yang
	 * <b>sudah ada</b> (uji substring) dilewati, alamat yang tidak lolos
	 * {@code Common.isValidEmailAddress()} ditolak, dan alamat yang diawali {@code "@"} (domain
	 * saja) ditolak.
	 *
	 * <p>Meng-override {@code Karyawan.appendEmail(String)} dengan badan yang sama, tetapi bekerja
	 * pada field {@code email} milik kelas ini (lihat catatan "field bayangan" di Javadoc kelas).
	 * Bekerja langsung pada field, jadi normalisasi {@link #getEmail()} tidak ikut dijalankan.</p>
	 *
	 * @param email satu alamat surel yang ingin ditambahkan; null/kosong/tidak valid diabaikan
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
	 * Nomor telepon (rumah/kantor) — <b>getter cermin</b> ke {@code Dosen.getTelp()} /
	 * {@code Guru.getTeleponGuru()}.
	 *
	 * @return nomor telepon apa adanya, atau {@code null}
	 * @see #getHp()
	 * @see #ambilNoHp()
	 */
	@Column(name = "telp", length = 100)
	public String getTelp() {
		dosen = getDosen();
		if (dosen != null) {
			this.telp = dosen.getTelp();
		}
		guru = getGuru();
		if (guru != null) {
			this.telp = guru.getTeleponGuru();
		}
		return this.telp;
	}

	/**
	 * Mengisi nomor telepon pada field lokal saja.
	 *
	 * @param telp nomor telepon
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Jenis kelamin dalam bentuk <b>kanonik</b> {@code "Laki-laki"} atau {@code "Perempuan"}.
	 *
	 * <p>Getter ini melakukan pembersihan data warisan yang cukup agresif: nilai {@code null}
	 * dianggap {@code "Laki-laki"}, singkatan {@code "L"}/{@code "P"} dan variasi kapitalisasi
	 * ({@code "Laki-Laki"}) dinormalkan, lalu sebagai jaring terakhir setiap nilai yang
	 * <i>mengandung</i> "laki" atau "puan" dipetakan ke bentuk kanoniknya. Nilai dari
	 * {@link Dosen}/{@link Guru} diambil lebih dulu bila ada, sehingga normalisasi juga berlaku
	 * untuk data mereka.</p>
	 *
	 * <p><b>Catatan:</b> default ke {@code "Laki-laki"} untuk data kosong berarti getter ini tidak
	 * pernah mengembalikan {@code null} — jangan dipakai untuk mendeteksi "belum diisi".</p>
	 *
	 * @return {@code "Laki-laki"} atau {@code "Perempuan"} (string kosong hanya pada kasus tepi)
	 */
	@Column(name = "kelamin", length = 20)
	public String getKelamin() {
		if (kelamin == null) {
			kelamin = "Laki-laki";
		}
		dosen = getDosen();
		if (dosen != null) {
			this.kelamin = dosen.getKelamin();
		}
		guru = getGuru();
		if (guru != null) {
			this.kelamin = guru.getJenisKelamin();
		}

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

		return this.kelamin == null ? "" : kelamin.trim();
	}

	/**
	 * Mengisi jenis kelamin mentah (tanpa normalisasi — normalisasi dilakukan
	 * {@link #getKelamin()}).
	 *
	 * @param kelamin jenis kelamin
	 */
	public void setKelamin(String kelamin) {
		this.kelamin = kelamin;
	}

	/**
	 * Kota/tempat kelahiran — <b>getter cermin</b> ke {@code Dosen.getTempatlahir()} /
	 * {@code Guru.getTempatLahir()}. Nilai {@code null} dinormalkan menjadi string kosong sebelum
	 * pencerminan.
	 *
	 * @return tempat lahir; string kosong bila tidak ada data
	 */
	@Column(name = "tempatlahir", length = 100)
	public String getTempatlahir() {
		if (tempatlahir == null) {
			tempatlahir = "";
		}
		dosen = getDosen();
		if (dosen != null) {
			this.tempatlahir = dosen.getTempatlahir();
		}
		guru = getGuru();
		if (guru != null) {
			this.tempatlahir = guru.getTempatLahir();
		}
		return this.tempatlahir;
	}

	/**
	 * Mengisi tempat lahir pada field lokal saja.
	 *
	 * @param tempatlahir kota/tempat kelahiran
	 */
	public void setTempatlahir(String tempatlahir) {
		this.tempatlahir = tempatlahir;
	}

	/**
	 * Tanggal lahir — <b>getter cermin</b> ke {@code Dosen.getTanggallahir()} /
	 * {@code Guru.getTanggalLahir()}. Dipakai antara lain untuk menghitung usia pensiun
	 * (bandingkan {@link #getUsiaPensiun()}).
	 *
	 * @return tanggal lahir, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggallahir", length = 0)
	public Date getTanggallahir() {
		dosen = getDosen();
		if (dosen != null) {
			this.tanggallahir = dosen.getTanggallahir();
		}
		guru = getGuru();
		if (guru != null) {
			this.tanggallahir = guru.getTanggalLahir();
		}
		return this.tanggallahir;
	}

	/**
	 * Mengisi tanggal lahir pada field lokal saja.
	 *
	 * @param tanggallahir tanggal lahir
	 */
	public void setTanggallahir(Date tanggallahir) {
		this.tanggallahir = tanggallahir;
	}

	/**
	 * Mengisi pangkat kepegawaian (teks bebas, mis. "Penata Muda Tk. I").
	 *
	 * @param pangkat nama pangkat
	 */
	public void setPangkat(String pangkat) {
		this.pangkat = pangkat;
	}

	/**
	 * Pangkat kepegawaian sebagai teks bebas. Berbeda dari {@link Golongan} pada
	 * {@link KenaikanPangkat} yang merupakan master terstruktur; field ini hanya keterangan.
	 *
	 * @return nama pangkat, atau {@code null}
	 */
	@Column(name = "pangkat", length = 255)
	public String getPangkat() {
		return pangkat;
	}

	/**
	 * Mengisi nomor induk utama pegawai (NIP/NIK internal).
	 *
	 * @param code nomor induk
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Nomor induk utama pegawai (NIP/NIK internal).
	 *
	 * <p>Berbeda dengan getter cermin lain, di sini pencerminan bersifat <b>fallback</b>: nilai
	 * dari {@code Dosen.getCode()} lalu {@code Guru.getNip()} hanya dipakai bila kolom lokal masih
	 * kosong. Jadi nomor induk yang sudah diisi khusus untuk baris kepegawaian tetap menang.</p>
	 *
	 * @return nomor induk; string kosong (bukan {@code null}) bila tidak ada data
	 * @see #getMycode()
	 * @see #getNipLama()
	 */
	@Column(name = "code", length = 50)
	public String getCode() {
		if (code == null || code.trim().isEmpty()) {
			dosen = getDosen();
			if (dosen != null) {
				this.code = dosen.getCode();
			}
			if (code == null || code.trim().isEmpty()) {
				guru = getGuru();
				if (guru != null) {
					this.code = guru.getNip();
				}
			}
		}
		return code == null ? "" : code;
	}

	/**
	 * Mengisi bendera pegawai tetap.
	 *
	 * @param tetap {@code 1} untuk tetap, {@code 0} untuk tidak tetap
	 */
	public void setTetap(Integer tetap) {
		this.tetap = tetap;
	}

	/**
	 * Bendera pegawai tetap dalam bentuk angka ({@code 1} = tetap, {@code 0} = tidak). Dipakai
	 * {@link #getIkatanKerjaDosen()} untuk menyimpulkan ikatan kerja default.
	 *
	 * <p><b>Cermin sebagian:</b> nilai ditimpa dari {@code Dosen.getTetap()} bila ada, tetapi
	 * <b>tidak</b> dari {@link Guru} — jadi untuk pegawai turunan guru nilainya tetap dari kolom
	 * lokal.</p>
	 *
	 * @return {@code 1}/{@code 0}, atau {@code null} bila kolom kosong dan tidak ada dosen
	 */
	@Column(name = "tetap", length = 1)
	public Integer getTetap() {
		dosen = getDosen();
		if (dosen != null) {
			this.tetap = dosen.getTetap();
		}

		return tetap;
	}

	/**
	 * Mengisi nomor induk alternatif/lokal.
	 *
	 * @param mycode nomor induk alternatif
	 */
	public void setMycode(String mycode) {
		this.mycode = mycode;
	}

	/**
	 * Nomor induk alternatif (kode internal institusi, di samping {@link #getCode()}).
	 * Dicerminkan dari {@code Dosen.getMycode()} / {@code Guru.getKode()}, tetapi hanya bila nilai
	 * sumbernya <b>tidak kosong</b> — sehingga kode lokal tidak terhapus oleh data sumber kosong.
	 *
	 * @return nomor induk alternatif tanpa spasi tepi; string kosong bila tidak ada
	 */
	public String getMycode() {
		dosen = getDosen();
		if (dosen != null && dosen.getMycode() != null && !dosen.getMycode().trim().isEmpty()) {
			this.mycode = dosen.getMycode();
		}
		guru = getGuru();
		if (guru != null && guru.getKode() != null && !guru.getKode().trim().isEmpty()) {
			this.mycode = guru.getKode();
		}
		return mycode == null ? "" : mycode.trim();
	}

	/**
	 * Mengisi nama jabatan bebas-teks. Umumnya tidak perlu, karena {@link #getJabatan()} menghitung
	 * ulang nilainya dari relasi jabatan yang berlaku.
	 *
	 * @param jabatan nama jabatan
	 */
	public void setJabatan(String jabatan) {
		this.jabatan = jabatan;
	}

	/**
	 * Nama jabatan sebagai teks siap tampil. Nilainya <b>diturunkan</b>, bukan sekadar dibaca dari
	 * kolom, dengan urutan prioritas:
	 *
	 * <ol>
	 * <li>{@code Dosen.getJabatan()} bila pegawai ini turunan dosen;</li>
	 * <li>{@link JabatanStruktural} yang berlaku — dan ini <b>menimpa</b> nilai dari dosen;</li>
	 * <li>{@link JabatanFungsional} yang berlaku;</li>
	 * <li>{@link LevelJabatan};</li>
	 * <li>kolom {@code jabatan} apa adanya bila semua di atas kosong.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping berat:</b> {@link #getJabatanStruktural()} dan
	 * {@link #getJabatanFungsional()} masing-masing memindai seluruh cache {@link KenaikanPangkat},
	 * jadi getter ini jauh dari murah — hindari memanggilnya berulang di dalam loop render.</p>
	 *
	 * @return nama jabatan siap tampil, atau {@code null} bila tidak ada sama sekali
	 */
	public String getJabatan() {
		dosen = getDosen();
		if (dosen != null) {
			this.jabatan = dosen.getJabatan();
		}

		if (getJabatanStruktural() != null) {
			jabatan = getJabatanStruktural().getNama();
		} else if (getJabatanFungsional() != null) {
			jabatan = getJabatanFungsional().getNama();
		} else if (getLevelJabatan() != null) {
			jabatan = getLevelJabatan().getNama();
		}
		return jabatan;
	}

	/**
	 * Mengisi bidang keahlian utama.
	 *
	 * @param spesialisasi1 bidang keahlian pertama
	 */
	public void setSpesialisasi1(String spesialisasi1) {
		this.spesialisasi1 = spesialisasi1;
	}

	/**
	 * Bidang keahlian utama pegawai (teks bebas).
	 *
	 * @return bidang keahlian pertama, atau {@code null}
	 */
	public String getSpesialisasi1() {
		return spesialisasi1;
	}

	/**
	 * Mengisi bidang keahlian kedua.
	 *
	 * @param spesialisasi2 bidang keahlian kedua
	 */
	public void setSpesialisasi2(String spesialisasi2) {
		this.spesialisasi2 = spesialisasi2;
	}

	/**
	 * Bidang keahlian kedua pegawai (teks bebas).
	 *
	 * @return bidang keahlian kedua, atau {@code null}
	 */
	public String getSpesialisasi2() {
		return spesialisasi2;
	}

	/**
	 * Mengisi bidang keahlian ketiga.
	 *
	 * @param spesialisasi3 bidang keahlian ketiga
	 */
	public void setSpesialisasi3(String spesialisasi3) {
		this.spesialisasi3 = spesialisasi3;
	}

	/**
	 * Bidang keahlian ketiga pegawai (teks bebas).
	 *
	 * @return bidang keahlian ketiga, atau {@code null}
	 */
	public String getSpesialisasi3() {
		return spesialisasi3;
	}

	/**
	 * Mengisi golongan kepegawaian bebas-teks.
	 *
	 * @param golongan nama golongan (mis. "III/b")
	 */
	public void setGolongan(String golongan) {
		this.golongan = golongan;
	}

	/**
	 * Golongan kepegawaian sebagai teks bebas — <b>getter cermin</b> ke {@code Dosen.getGolongan()}
	 * (tanpa jalur guru).
	 *
	 * <p>Jangan tertukar dengan {@link Golongan} terstruktur yang melekat pada
	 * {@link KenaikanPangkat}; yang dipakai untuk menghitung gaji adalah yang terstruktur, bukan
	 * teks ini.</p>
	 *
	 * @return nama golongan, atau {@code null}
	 */
	public String getGolongan() {
		dosen = getDosen();
		if (dosen != null) {
			this.golongan = dosen.getGolongan();
		}
		return golongan;
	}

	/**
	 * Nomor KTP/NIK kependudukan. Dicerminkan dari {@code Dosen.getKtp()} / {@code Guru.getNik()},
	 * tetapi hanya bila nilai sumbernya tidak kosong sehingga NIK yang sudah diisi di baris
	 * kepegawaian tidak terhapus.
	 *
	 * @return nomor KTP/NIK, atau {@code null}
	 */
	public String getKtp() {
		dosen = getDosen();
		if (dosen != null && dosen.getKtp() != null && !dosen.getKtp().trim().isEmpty()) {
			this.ktp = dosen.getKtp();
		}
		guru = getGuru();
		if (guru != null && guru.getNik() != null && !guru.getNik().trim().isEmpty()) {
//			System.out.println("guru.getNik() -> " + guru.getNik());
			this.ktp = guru.getNik();
		}
		return ktp;
	}

	/**
	 * Mengisi nomor KTP/NIK. Bersama {@link #setAlamat(String)}, ini setter yang <b>menulis balik
	 * ke sumber</b>: NIK ikut di-set pada {@link Guru} ({@code setNik}) dan {@link Dosen}
	 * ({@code setKtp}) bila nilai barunya tidak kosong, supaya tidak hilang saat
	 * {@link #getKtp()} dipanggil lagi.
	 *
	 * @param ktp nomor KTP/NIK; nilai kosong tetap disimpan lokal tapi tidak diteruskan ke sumber
	 */
	public void setKtp(String ktp) {
		guru = getGuru();
		if (guru != null && ktp != null && !ktp.trim().isEmpty()) {
			guru.setNik(ktp);
		}
		dosen = getDosen();
		if (dosen != null && ktp != null && !ktp.trim().isEmpty()) {
			dosen.setKtp(ktp);
		}
		this.ktp = ktp;
	}

	/**
	 * Status pegawai (aktif, cuti di luar tanggungan, pensiun, dan seterusnya) —
	 * <b>getter cermin</b> ke {@link Dosen}/{@link Guru}, lalu di-{@code check()} agar proxy yang
	 * sudah lepas dari sesi digantikan instance dari cache.
	 *
	 * <p><b>Tidak pernah mengembalikan {@code null}</b>: bila semua sumber kosong, nilainya
	 * dipaksa ke {@code ConstantValues.AKTIF_PEGAWAI}. Konsekuensinya, data yang statusnya belum
	 * pernah diisi otomatis terbaca sebagai aktif.</p>
	 *
	 * @return status pegawai; default aktif
	 * @see #getAktif()
	 * @see GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_pegawai")
	public StatusPegawai getStatusPegawai() {
		dosen = getDosen();
		if (dosen != null) {
			this.statusPegawai = dosen.getStatusPegawai();
		}

		guru = getGuru();
		if (guru != null) {
			this.statusPegawai = guru.getStatusPegawai();
		}
		statusPegawai = check(statusPegawai);

		if (statusPegawai == null) {
			statusPegawai = ais.common.ConstantValues.AKTIF_PEGAWAI;
		}

		return statusPegawai;
	}

	/**
	 * Mengisi status pegawai.
	 *
	 * @param statusPegawai status pegawai; {@code null} akan berubah menjadi "aktif" pada
	 *                      pembacaan berikutnya
	 */
	public void setStatusPegawai(StatusPegawai statusPegawai) {
		this.statusPegawai = statusPegawai;
	}

	/**
	 * Status kepegawaian (mis. PNS, tetap yayasan, kontrak) — <b>getter cermin</b> ke
	 * {@link Dosen}/{@link Guru} lalu di-{@code check()}.
	 *
	 * <p>Berbeda dengan {@link #getStatusPegawai()}, getter ini <b>boleh</b> mengembalikan
	 * {@code null} bila memang belum diisi.</p>
	 *
	 * @return status kepegawaian, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_kepegawaian")
	public StatusKepegawaian getStatusKepegawaian() {
		dosen = getDosen();
		if (dosen != null) {
			this.statusKepegawaian = dosen.getStatusKepegawaian();
		}
		guru = getGuru();
		if (guru != null) {
			this.statusKepegawaian = guru.getStatusKepegawaian();
		}
		statusKepegawaian = check(statusKepegawaian);
		return statusKepegawaian;
	}

	/**
	 * Mengisi status kepegawaian.
	 *
	 * @param statusKepegawaian status kepegawaian
	 */
	public void setStatusKepegawaian(StatusKepegawaian statusKepegawaian) {
		this.statusKepegawaian = statusKepegawaian;
	}

	/**
	 * Unit kerja tempat pegawai ditempatkan (struktur organisasi kepegawaian). Berbeda dari
	 * {@link #getSatuanKerja()} yang merupakan unit <i>anggaran</i>.
	 *
	 * @return unit kerja, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "unit_kerja", nullable = true)
	public UnitKerja getUnitKerja() {
		unitKerja = check(unitKerja);
		return unitKerja;
	}

	/**
	 * Mengisi unit kerja.
	 *
	 * @param unitKerja unit kerja
	 */
	public void setUnitKerja(UnitKerja unitKerja) {
		this.unitKerja = unitKerja;
	}

	/**
	 * Satuan kerja (unit anggaran/RAB) pegawai. Ini <b>salah satu getter paling berefek samping</b>
	 * di kelas ini karena satuan kerja jarang diisi langsung dan harus disimpulkan dari konteks.
	 *
	 * <p>Urutan penentuannya:</p>
	 * <ol>
	 * <li><b>Penugasan eksplisit.</b> {@code SatuanKerjaPegawai.ambilSatuanKerja(this)} — bila
	 * pegawai punya penugasan satuan kerja tersendiri, itu yang menang dan method langsung
	 * selesai.</li>
	 * <li>Nilai kolom sendiri, setelah {@code check()}.</li>
	 * <li><b>Warisan struktur akademik, hanya bila unit tersebut mewajibkan</b> (bendera
	 * {@code getDosenHarusPakaiSatuanKerja()} / {@code getGuruHarusPakaiSatuanKerja()}):
	 * jurusan dosen, lalu fakultas dosen, lalu perguruan tinggi dosen, lalu sekolah guru.</li>
	 * <li>Bila masih kosong: sekolah guru atau perguruan tinggi dosen <i>tanpa</i> syarat bendera.</li>
	 * <li>Bila masih kosong <b>dan entity ini belum tersimpan</b> ({@code id == null}): satuan
	 * kerja milik pengguna yang sedang login ({@code Common.getCurrentUser().ambilSatuanKerja()}),
	 * lalu satuan kerja {@link Perpustakaan} yang sedang aktif. Inilah yang membuat pegawai baru
	 * otomatis masuk ke satuan kerja operator yang menginput.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> menulis ke field {@code satuanKerja}, {@code dosen}, {@code guru};
	 * dapat membaca sesi pengguna yang sedang berjalan; dapat memicu beberapa inisialisasi proxy
	 * lazy. Beberapa langkah dibungkus {@code try/catch} agar data tidak lengkap tidak
	 * menggagalkan pembacaan.</p>
	 *
	 * @return satuan kerja hasil penyimpulan, atau {@code null} bila semua jalur gagal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {

		SatuanKerja s = SatuanKerjaPegawai.ambilSatuanKerja(this);
		if (s != null && s.getId() != null) {
			satuanKerja = s;
			return satuanKerja;
		}

		satuanKerja = check(satuanKerja);

		if (getDosen() != null && getDosen().getJurusan() != null && getDosen().getJurusan().getSatuanKerja() != null
				&& getDosen().getJurusan().getDosenHarusPakaiSatuanKerja()) {
			satuanKerja = getDosen().getJurusan().getSatuanKerja();
		}

		else if (getDosen() != null && getDosen().getFakultas() != null
				&& getDosen().getFakultas().getSatuanKerja() != null
				&& getDosen().getFakultas().getDosenHarusPakaiSatuanKerja()) {
			satuanKerja = getDosen().getFakultas().getSatuanKerja();
		}

		else if (getDosen() != null && getDosen().getPerguruanTinggi() != null
				&& getDosen().getPerguruanTinggi().getSatuanKerja() != null
				&& getDosen().getPerguruanTinggi().getDosenHarusPakaiSatuanKerja()) {
			satuanKerja = getDosen().getPerguruanTinggi().getSatuanKerja();
		}

		else if (getGuru() != null && getGuru().getSekolah() != null && getGuru().getSekolah().getSatuanKerja() != null
				&& getGuru().getSekolah().getGuruHarusPakaiSatuanKerja()) {
			satuanKerja = getGuru().getSekolah().getSatuanKerja();
		}

		else if (satuanKerja == null) {
			guru = getGuru();
			dosen = getDosen();
			if (this.guru != null && guru.getSekolah() != null && guru.getSekolah().getSatuanKerja() != null) {
				try {
					this.satuanKerja = guru.getSekolah().getSatuanKerja();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:926");
				}
			} else if (this.dosen != null && dosen.getPerguruanTinggi() != null
					&& dosen.getPerguruanTinggi().getSatuanKerja() != null) {
				try {
					this.satuanKerja = dosen.getPerguruanTinggi().getSatuanKerja();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:932");
				}
			} else if (this.satuanKerja == null && this.id == null) {
				try {
					SatuanKerja satuanKerja = null;
					Tbmuser currentUser = Common.getCurrentUser();
					if (currentUser != null) {
						satuanKerja = currentUser.ambilSatuanKerja();
					}
					Perpustakaan currentPerpustakaan = Common.getCurrentPerpustakaan();
					if (satuanKerja == null && currentPerpustakaan != null) {
						satuanKerja = currentPerpustakaan.getSatuanKerja();
					}
					this.satuanKerja = satuanKerja;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:942");
				}
			}
		}
		return satuanKerja;
	}

	/**
	 * Mengisi satuan kerja secara eksplisit. Perhatikan bahwa {@link #getSatuanKerja()} tetap dapat
	 * menimpanya bila ada penugasan {@code SatuanKerjaPegawai} atau bila unit akademik mewajibkan
	 * satuan kerja tertentu.
	 *
	 * @param satuanKerja satuan kerja
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Agama pegawai — <b>getter cermin</b>, dengan sumber yang berbeda per jalur: untuk dosen
	 * diambil dari {@code Dosen.ambilBiodata().getAgama()} (entity biodata terpisah), untuk guru
	 * langsung dari {@code Guru.getAgama()}.
	 *
	 * <p>Perhatikan urutannya: {@code check()} dijalankan lebih dulu, baru pencerminan, sehingga
	 * nilai dari dosen/guru selalu menang.</p>
	 *
	 * @return agama, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "agama", nullable = true)
	public Agama getAgama() {
		agama = check(agama);

		dosen = getDosen();
		if (dosen != null) {
			BiodataDosen biodataDosen = dosen.ambilBiodata();
			if (biodataDosen != null) {
				this.agama = biodataDosen.getAgama();
			}
		}
		guru = getGuru();
		if (guru != null) {
			this.agama = guru.getAgama();
		}
		return agama;
	}

	/**
	 * Mengisi agama.
	 *
	 * @param agama agama pegawai
	 */
	public void setAgama(Agama agama) {
		this.agama = agama;
	}

	/**
	 * Status perkawinan sebagai teks siap tampil.
	 *
	 * <p>Untuk jalur dosen nilainya <b>diterjemahkan</b> dari kode angka
	 * {@code BiodataDosen.getStatusNikah()}: {@code 0} = "Belum Nikah", {@code 1} = "Nikah",
	 * {@code 2} = "Janda", {@code 3} = "Duda", nilai lain menjadi string kosong. Untuk jalur guru
	 * nilainya sudah berupa teks dan dipakai apa adanya.</p>
	 *
	 * <p><b>Catatan ketahanan:</b> penerjemahan memanggil {@code .equals()} pada hasil
	 * {@code getStatusNikah()} tanpa penjagaan null — biodata dosen yang kolom status nikahnya
	 * kosong dapat memicu {@link NullPointerException} di sini.</p>
	 *
	 * @return status perkawinan siap tampil, atau {@code null} bila tidak ada data
	 */
	public String getStatusPerkawinan() {
		dosen = getDosen();
		if (dosen != null) {
			BiodataDosen biodataDosen = dosen.ambilBiodata();
			if (biodataDosen != null) {
				this.statusPerkawinan = biodataDosen.getStatusNikah().equals(0) ? "Belum Nikah"
						: biodataDosen.getStatusNikah().equals(1) ? "Nikah"
								: biodataDosen.getStatusNikah().equals(2) ? "Janda"
										: biodataDosen.getStatusNikah().equals(3) ? "Duda" : "";
			}
		}

		guru = getGuru();
		if (guru != null) {
			this.statusPerkawinan = guru.getStatusNikah();
		}
		return statusPerkawinan;
	}

	/**
	 * Mengisi status perkawinan (teks bebas).
	 *
	 * @param statusPerkawinan status perkawinan
	 */
	public void setStatusPerkawinan(String statusPerkawinan) {
		this.statusPerkawinan = statusPerkawinan;
	}

	/**
	 * Bagian "jalan" dari alamat berjenjang — dicerminkan dari {@code BiodataDosen.getAlamat()}
	 * atau {@code Guru.getAlamatGuru()}.
	 *
	 * @return alamat jalan, atau {@code null}
	 * @see #getAlamat()
	 */
	public String getAlamatJalan() {
		dosen = getDosen();
		if (dosen != null) {
			BiodataDosen biodataDosen = dosen.ambilBiodata();
			if (biodataDosen != null) {
				this.alamatJalan = biodataDosen.getAlamat();
			}
		}
		guru = getGuru();
		if (guru != null) {
			this.alamatJalan = guru.getAlamatGuru();
		}
		return alamatJalan;
	}

	/**
	 * Mengisi bagian jalan dari alamat.
	 *
	 * @param alamatJalan nama jalan/nomor rumah
	 */
	public void setAlamatJalan(String alamatJalan) {
		this.alamatJalan = alamatJalan;
	}

	/**
	 * Kelurahan/desa domisili — dicerminkan dari {@code BiodataDosen.getKelurahan()} atau
	 * {@code Guru.getKelurahan()}.
	 *
	 * @return nama kelurahan, atau {@code null}
	 */
	public String getAlamatKelurahan() {
		dosen = getDosen();
		if (dosen != null) {
			BiodataDosen biodataDosen = dosen.ambilBiodata();
			if (biodataDosen != null) {
				this.alamatKelurahan = biodataDosen.getKelurahan();
			}
		}
		guru = getGuru();
		if (guru != null) {
			this.alamatKelurahan = guru.getKelurahan();
		}
		return alamatKelurahan;
	}

	/**
	 * Mengisi kelurahan/desa domisili.
	 *
	 * @param alamatKelurahan nama kelurahan
	 */
	public void setAlamatKelurahan(String alamatKelurahan) {
		this.alamatKelurahan = alamatKelurahan;
	}

	/**
	 * Kabupaten/kota domisili. Untuk dosen diambil dari {@code BiodataDosen.getKota().getNama()}.
	 *
	 * <p><b>Kuirk (dicatat, tidak diperbaiki):</b> pada jalur {@link Guru} nilai yang dihitung
	 * (nama wilayah induk kecamatan) justru ditulis ke field {@code alamatPropinsi}, bukan
	 * {@code alamatKabupaten} — tampak seperti salah salin. Akibatnya untuk pegawai turunan guru,
	 * getter ini mengembalikan nilai kolom lama dan {@link #getAlamatPropinsi()} sempat tertimpa
	 * nama kabupaten sebelum dihitung ulang oleh getter-nya sendiri.</p>
	 *
	 * @return nama kabupaten/kota, atau {@code null}
	 */
	public String getAlamatKabupaten() {
		dosen = getDosen();
		if (dosen != null) {
			BiodataDosen biodataDosen = dosen.ambilBiodata();
			if (biodataDosen != null) {
				this.alamatKabupaten = biodataDosen.getKota() == null ? "" : biodataDosen.getKota().getNama();
			}
		}
		guru = getGuru();
		if (guru != null) {
			this.alamatPropinsi = guru.getKecamatan() == null || guru.getKecamatan().getWilayahInduk() == null ? ""
					: guru.getKecamatan().getWilayahInduk().getNama();
		}
		return alamatKabupaten;
	}

	/**
	 * Mengisi kabupaten/kota domisili.
	 *
	 * @param alamatKabupaten nama kabupaten/kota
	 */
	public void setAlamatKabupaten(String alamatKabupaten) {
		this.alamatKabupaten = alamatKabupaten;
	}

	/**
	 * Provinsi domisili. Untuk dosen diambil dari {@code BiodataDosen.getPropinsi().getNama()};
	 * untuk guru ditelusuri dua tingkat ke atas dari kecamatan
	 * ({@code kecamatan → wilayahInduk (kabupaten) → wilayahInduk (provinsi)}), dengan penjagaan
	 * null di setiap tingkat.
	 *
	 * @return nama provinsi, atau {@code null}/string kosong bila rantai wilayah tidak lengkap
	 * @see #getPropinsi()
	 */
	public String getAlamatPropinsi() {
		dosen = getDosen();
		if (dosen != null) {
			BiodataDosen biodataDosen = dosen.ambilBiodata();
			if (biodataDosen != null) {
				this.alamatPropinsi = biodataDosen.getPropinsi() == null ? "" : biodataDosen.getPropinsi().getNama();
			}
		}
		guru = getGuru();
		if (guru != null) {
			this.alamatPropinsi = guru.getKecamatan() == null || guru.getKecamatan().getWilayahInduk() == null
					|| guru.getKecamatan().getWilayahInduk().getWilayahInduk() == null ? ""
							: guru.getKecamatan().getWilayahInduk().getWilayahInduk().getNama();
		}
		return alamatPropinsi;
	}

	/**
	 * Mengisi provinsi domisili.
	 *
	 * @param alamatPropinsi nama provinsi
	 */
	public void setAlamatPropinsi(String alamatPropinsi) {
		this.alamatPropinsi = alamatPropinsi;
	}

	/**
	 * Tinggi badan (teks bebas, satuan tidak dipaksakan) — bagian dari blok "ciri fisik" untuk
	 * berkas personalia/kartu pegawai.
	 *
	 * @return keterangan tinggi badan, atau {@code null}
	 */
	public String getKeteranganBadanTinggi() {
		return keteranganBadanTinggi;
	}

	/**
	 * Mengisi keterangan tinggi badan.
	 *
	 * @param keteranganBadanTinggi keterangan tinggi badan
	 */
	public void setKeteranganBadanTinggi(String keteranganBadanTinggi) {
		this.keteranganBadanTinggi = keteranganBadanTinggi;
	}

	/**
	 * Berat badan (teks bebas) — bagian dari blok ciri fisik.
	 *
	 * @return keterangan berat badan, atau {@code null}
	 */
	public String getKeteranganBadanBerat() {
		return keteranganBadanBerat;
	}

	/**
	 * Mengisi keterangan berat badan.
	 *
	 * @param keteranganBadanBerat keterangan berat badan
	 */
	public void setKeteranganBadanBerat(String keteranganBadanBerat) {
		this.keteranganBadanBerat = keteranganBadanBerat;
	}

	/**
	 * Ciri rambut — bagian dari blok ciri fisik.
	 *
	 * @return keterangan rambut, atau {@code null}
	 */
	public String getKeteranganBadanRambut() {
		return keteranganBadanRambut;
	}

	/**
	 * Mengisi keterangan rambut.
	 *
	 * @param keteranganBadanRambut keterangan rambut
	 */
	public void setKeteranganBadanRambut(String keteranganBadanRambut) {
		this.keteranganBadanRambut = keteranganBadanRambut;
	}

	/**
	 * Bentuk muka — bagian dari blok ciri fisik.
	 *
	 * @return keterangan bentuk muka, atau {@code null}
	 */
	public String getKeteranganBadanBentukMuka() {
		return keteranganBadanBentukMuka;
	}

	/**
	 * Mengisi keterangan bentuk muka.
	 *
	 * @param keteranganBadanBentukMuka keterangan bentuk muka
	 */
	public void setKeteranganBadanBentukMuka(String keteranganBadanBentukMuka) {
		this.keteranganBadanBentukMuka = keteranganBadanBentukMuka;
	}

	/**
	 * Warna kulit — bagian dari blok ciri fisik.
	 *
	 * @return keterangan warna kulit, atau {@code null}
	 */
	public String getKeteranganBadanWarnaKulit() {
		return keteranganBadanWarnaKulit;
	}

	/**
	 * Mengisi keterangan warna kulit.
	 *
	 * @param keteranganBadanWarnaKulit keterangan warna kulit
	 */
	public void setKeteranganBadanWarnaKulit(String keteranganBadanWarnaKulit) {
		this.keteranganBadanWarnaKulit = keteranganBadanWarnaKulit;
	}

	/**
	 * Ciri khas/tanda pengenal fisik lain (tahi lalat, bekas luka, dan sebagainya).
	 *
	 * @return keterangan ciri khas, atau {@code null}
	 */
	public String getKeteranganBadanCiriKhas() {
		return keteranganBadanCiriKhas;
	}

	/**
	 * Mengisi keterangan ciri khas fisik.
	 *
	 * @param keteranganBadanCiriKhas keterangan ciri khas
	 */
	public void setKeteranganBadanCiriKhas(String keteranganBadanCiriKhas) {
		this.keteranganBadanCiriKhas = keteranganBadanCiriKhas;
	}

	/**
	 * Keterangan disabilitas/cacat tubuh, dipakai antara lain untuk pelaporan pegawai
	 * berkebutuhan khusus.
	 *
	 * @return keterangan cacat, atau {@code null}
	 */
	public String getKeteranganBadanCacat() {
		return keteranganBadanCacat;
	}

	/**
	 * Mengisi keterangan disabilitas/cacat tubuh.
	 *
	 * @param keteranganBadanCacat keterangan cacat
	 */
	public void setKeteranganBadanCacat(String keteranganBadanCacat) {
		this.keteranganBadanCacat = keteranganBadanCacat;
	}

	/**
	 * Hobi/minat pegawai (teks bebas, informasi personalia).
	 *
	 * @return hobi, atau {@code null}
	 */
	public String getHobi() {
		return hobi;
	}

	/**
	 * Mengisi hobi.
	 *
	 * @param hobi hobi/minat
	 */
	public void setHobi(String hobi) {
		this.hobi = hobi;
	}

	/**
	 * Jenis kepegawaian sebagai teks bebas. Nilai yang dikenali ada pada konstanta
	 * {@link #JENIS_FUNGSIONAL}, {@link #JENIS_STRUKTURAL}, {@link #JENIS_HONORER}, dan
	 * {@link #JENIS_OUTSOURCHING}, tetapi kolomnya tidak divalidasi sehingga nilai lain juga
	 * mungkin tersimpan.
	 *
	 * @return jenis kepegawaian, atau {@code null}
	 */
	public String getJenis() {
		return jenis;
	}

	/**
	 * Mengisi jenis kepegawaian.
	 *
	 * @param jenis salah satu konstanta {@code JENIS_*}, atau teks bebas
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Menghitung <b>gaji pokok</b> yang berlaku bagi pegawai ini pada tanggal tertentu. Ini salah
	 * satu method bisnis inti kelas: nilai gaji pokok tidak disimpan di baris pegawai, melainkan
	 * disimpulkan ulang setiap kali dibutuhkan dari SK {@link KenaikanPangkat} yang berlaku.
	 *
	 * <p>Alur penentuan, berhenti pada jalur pertama yang cocok:</p>
	 * <ol>
	 * <li><b>Tanpa SK aktif atau entity belum tersimpan</b> ({@code getId() == null}) → {@code null}.</li>
	 * <li><b>Nilai ditentukan langsung di SK</b> ({@code getGajiLangsungDitentukanDisini()}) →
	 * dikembalikan objek {@link GajiPokok} <i>sintetis</i> (tidak tersimpan di basis data) yang
	 * hanya berisi {@code nilaiGaji} dari SK.</li>
	 * <li><b>Penggajian otomatis berdasarkan masa kerja</b>
	 * ({@code getGajiPokokOtomatisMasaKerja()}) → didelegasikan ke
	 * {@code MasaKerjaUtil.cariGajiPokok(this, golongan, sekarang)}, sehingga nilainya selalu
	 * mengikuti tahun masa kerja terkini dan <b>mengabaikan</b> gaji pokok manual yang tersimpan.</li>
	 * <li><b>Kenaikan gaji berkala</b> ({@code getTerdapatKenaikanGajiBerkala()} dengan bulan
	 * berkala dan tanggal mulai terisi) → tahun masa kerja dihitung dari tanggal mulai SK yang
	 * "dibulatkan" ke tanggal 1 pada bulan berkala, lalu dicocokkan ke tabel master
	 * {@link GajiPokok} untuk golongan yang sama: cari yang masa kerjanya <i>persis</i> sama,
	 * kalau tidak ada ambil entri pertama yang masa kerjanya <i>lebih besar</i>.</li>
	 * <li><b>Selain itu</b> → bila SK sudah menunjuk {@link GajiPokok} langsung, itu yang dipakai;
	 * kalau tidak, pencocokan tabel master yang sama dijalankan dengan tahun masa kerja dari
	 * {@code MasaKerjaUtil.masaKerja(this).getYears()}.</li>
	 * </ol>
	 *
	 * <p>Kandidat tabel master selalu disaring dulu agar {@code tanggalEfektif}-nya sudah lewat
	 * (atau sama dengan) {@code sekarang}, sehingga tarif yang belum berlaku tidak ikut terpilih.</p>
	 *
	 * <p><b>Catatan:</b> seluruh badan method dibungkus {@code try/catch} — kegagalan apa pun
	 * menghasilkan {@code null}, bukan exception, karena method ini dipanggil dari layar
	 * biodata/slip gaji yang tidak boleh ikut gagal. Masih ada dua {@code System.out.println}
	 * peninggalan debug di jalur ini.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code BiodataPegawaiAction}, {@code BiodataPegawaiAccountAction},
	 * {@code BiodataPegawaiSimpleAction}, dan modul penggajian lain.</p>
	 *
	 * @param sekarang tanggal acuan perhitungan (biasanya tanggal periode gaji)
	 * @return gaji pokok yang berlaku — bisa entri master, bisa objek sintetis dari SK — atau
	 *         {@code null} bila tidak dapat ditentukan
	 * @see #ambilKenaikanPangkat(Date)
	 * @see #ambilInsentif(Date)
	 * @see MasaKerjaUtil#cariGajiPokok(Pegawai, Golongan, Date)
	 */
	public GajiPokok ambilGajiPokok(Date sekarang) {
		GajiPokok gajiPokok = null;

		try {
			if (getId() != null) {
				List<KenaikanPangkat> kenaikanPangkats = ambilKenaikanPangkat(sekarang);
				KenaikanPangkat kenaikanPangkat = kenaikanPangkats.isEmpty() ? null : kenaikanPangkats.get(0);

				if (kenaikanPangkat != null && kenaikanPangkat.getGajiLangsungDitentukanDisini()) {
					GajiPokok gajiPokokTemp = new GajiPokok();
					gajiPokokTemp.setGaji(kenaikanPangkat.getNilaiGaji());
					return gajiPokokTemp;
				}

				// Cekbox "Penggajian Otomatis Berdasarkan Masa Kerja": bila aktif, Gaji Pokok dihitung
				// DINAMIS dari golongan + masa kerja terkini (mengabaikan gaji pokok manual tersimpan),
				// sehingga nilai selalu mengikuti tahun masa kerja pegawai sesuai tabel master.
				if (kenaikanPangkat != null && kenaikanPangkat.getGajiPokokOtomatisMasaKerja()) {
					GajiPokok auto = ais.action.master.employ.helper.MasaKerjaUtil.cariGajiPokok(this,
							kenaikanPangkat.getGolongan(), sekarang);
					if (auto != null) {
						return auto;
					}
				}

				if (kenaikanPangkat != null) {
					gajiPokok = kenaikanPangkat.getGajiPokok();
				}

				String s = Common.dateFormat1.get().format(sekarang);
				if (kenaikanPangkat != null && kenaikanPangkat.getTerdapatKenaikanGajiBerkala()
						&& kenaikanPangkat.getKenaikanBerkalaBulan() != null && kenaikanPangkat.getMulai() != null) {

					Golongan gol = kenaikanPangkat.getGajiPokok() == null ? kenaikanPangkat.getGolongan()
							: kenaikanPangkat.getGajiPokok().getGolongan();
					GajiPokok gp = null;
					if (gol != null) {

						Date mulai = kenaikanPangkat.getMulai();
						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(mulai);
						calendar.set(Calendar.DATE, 1);
						calendar.set(Calendar.MONTH, kenaikanPangkat.getKenaikanBerkalaBulan() - 1);

						String ActualDate = Common.databaseDateFormat.get().format(calendar.getTime());
						java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
						java.time.LocalDate currentdate = java.time.LocalDate.now();
						Period period = Period.between(dt, currentdate);
						Integer masaKerjaTahun = period.getYears();
						List<GajiPokok> gajiPokoks = new ArrayList<GajiPokok>();

						for (Object o : ConstantValues.ambilBerdasarClass(GajiPokok.class).values()) {
							GajiPokok g = (GajiPokok) o;
							if (g.getMasaKerja() != null && g.getGolongan() != null
									&& g.getGolongan().getId().equals(gol.getId())) {
								if (g.getTanggalEfektif().before(sekarang)
										|| Common.dateFormat1.get().format(g.getTanggalEfektif()).equals(s)) {
									gajiPokoks.add(g);
								}

							}
						}

						for (GajiPokok g : gajiPokoks) {
							if (masaKerjaTahun.equals(g.getMasaKerja())) {
								gp = g;
								break;
							}
						}

						if (gp == null) {
							for (GajiPokok g : gajiPokoks) {
								if (masaKerjaTahun < g.getMasaKerja()) {
									gp = g;
									break;
								}
							}
						}

						gajiPokoks = null;
					}
					if (gp != null) {
						gajiPokok = gp;
					}
				} else {

					if (kenaikanPangkat != null && kenaikanPangkat.getGajiPokok() != null) {
						return kenaikanPangkat.getGajiPokok();
					}

					Golongan golonganPegawai = kenaikanPangkat == null ? null : kenaikanPangkat.getGolongan();
					Integer masaKerjaTahun = 1;
					try {
//				Double d = MasaKerjaUtil.hitung(this);
						Period period = MasaKerjaUtil.masaKerja(this);
						masaKerjaTahun = period == null ? 0 : period.getYears();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pegawai.java:1248");
					}

					if (golonganPegawai != null && masaKerjaTahun != null) {

						List<GajiPokok> gajiPokoks = new ArrayList<GajiPokok>();

						GajiPokok gp = null;
						for (Object o : ConstantValues.ambilBerdasarClass(GajiPokok.class).values()) {
							GajiPokok g = (GajiPokok) o;
							if (g.getMasaKerja() != null && g.getGolongan() != null
									&& g.getGolongan().getId().equals(golonganPegawai.getId())) {
								if (g.getTanggalEfektif().before(sekarang)
										|| Common.dateFormat1.get().format(g.getTanggalEfektif()).equals(s)) {
									gajiPokoks.add(g);
								}

							}
						}

						for (GajiPokok g : gajiPokoks) {
							if (masaKerjaTahun.equals(g.getMasaKerja())) {
								gp = g;
								break;
							}
						}

						if (gp == null) {
							for (GajiPokok g : gajiPokoks) {
								if (masaKerjaTahun < g.getMasaKerja()) {
									gp = g;
									break;
								}
							}
						}
						gajiPokoks = null;
						gajiPokok = gp;
					}

					System.out.println("masaKerjaTahun -> " + masaKerjaTahun);
				}
				kenaikanPangkats = null;
				System.out.println("gajiPokok -> " + gajiPokok);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pegawai.java:1293");
		}

		return gajiPokok;
	}

	/**
	 * Menghitung <b>insentif</b> yang berlaku bagi pegawai ini pada tanggal tertentu. Strukturnya
	 * sejajar dengan {@link #ambilGajiPokok(Date)}, hanya tabel masternya {@link Insentif}:
	 *
	 * <ol>
	 * <li>{@code getGajiLangsungDitentukanDisini()} → objek {@link Insentif} sintetis berisi
	 * {@code nilaiInsentif} dari SK;</li>
	 * <li>SK punya kenaikan berkala → cocokkan tabel master berdasarkan golongan dan tahun masa
	 * kerja yang dihitung dari tanggal mulai SK pada bulan berkala;</li>
	 * <li>selain itu → pakai {@link Insentif} yang ditunjuk SK, atau cocokkan tabel master dengan
	 * tahun masa kerja dari {@code MasaKerjaUtil.hitung(this)}.</li>
	 * </ol>
	 *
	 * <p><b>Perbedaan halus dari {@link #ambilGajiPokok(Date)} yang layak diketahui:</b> method ini
	 * <i>tidak</i> mengenal opsi "penggajian otomatis berdasarkan masa kerja", dan pada jalur
	 * terakhir memakai {@code MasaKerjaUtil.hitung(this)} (nilai {@code Double} yang dipangkas
	 * dengan {@code intValue()}) sementara {@code ambilGajiPokok} memakai
	 * {@code MasaKerjaUtil.masaKerja(this).getYears()}. Keduanya bisa memberi tahun masa kerja yang
	 * berbeda untuk pegawai yang sama.</p>
	 *
	 * @param sekarang tanggal acuan perhitungan
	 * @return insentif yang berlaku, atau {@code null} bila tidak dapat ditentukan
	 * @see #ambilGajiPokok(Date)
	 */
	public Insentif ambilInsentif(Date sekarang) {
		Insentif insentif = null;

		try {
			if (getId() != null) {
				List<KenaikanPangkat> kenaikanPangkats = ambilKenaikanPangkat(sekarang);
				KenaikanPangkat kenaikanPangkat = kenaikanPangkats.isEmpty() ? null : kenaikanPangkats.get(0);

				if (kenaikanPangkat != null && kenaikanPangkat.getGajiLangsungDitentukanDisini()) {
					insentif = new Insentif();
					insentif.setInsentif(kenaikanPangkat.getNilaiInsentif());
					return insentif;
				}

				if (kenaikanPangkat != null) {
					insentif = kenaikanPangkat.getInsentif();
				}

				String s = Common.dateFormat1.get().format(sekarang);
				if (kenaikanPangkat != null && kenaikanPangkat.getTerdapatKenaikanGajiBerkala()
						&& kenaikanPangkat.getKenaikanBerkalaBulan() != null && kenaikanPangkat.getMulai() != null) {

					Golongan gol = kenaikanPangkat.getGajiPokok() == null ? kenaikanPangkat.getGolongan()
							: kenaikanPangkat.getGajiPokok().getGolongan();
					Insentif gp = null;
					if (gol != null) {

						Date mulai = kenaikanPangkat.getMulai();
						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(mulai);
						calendar.set(Calendar.DATE, 1);
						calendar.set(Calendar.MONTH, kenaikanPangkat.getKenaikanBerkalaBulan() - 1);

						String ActualDate = Common.databaseDateFormat.get().format(calendar.getTime());
						java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
						java.time.LocalDate currentdate = java.time.LocalDate.now();
						Period period = Period.between(dt, currentdate);
						Integer masaKerjaTahun = period.getYears();

						List<Insentif> insentifs = new ArrayList<Insentif>();

						for (Object o : ConstantValues.ambilBerdasarClass(Insentif.class).values()) {
							Insentif g = (Insentif) o;
							if (g.getMasaKerja() != null && g.getGolongan() != null
									&& g.getGolongan().getId().equals(gol.getId())) {
								if (g.getTanggalEfektif().before(sekarang)
										|| Common.dateFormat1.get().format(g.getTanggalEfektif()).equals(s)) {
									insentifs.add(g);
								}

							}
						}

						for (Insentif g : insentifs) {
							if (masaKerjaTahun.equals(g.getMasaKerja())) {
								gp = g;
								break;
							}
						}

						if (gp == null) {
							for (Insentif g : insentifs) {
								if (masaKerjaTahun < g.getMasaKerja()) {
									gp = g;
									break;
								}
							}
						}
						insentifs = null;

					}

					if (gp != null) {
						insentif = gp;
					}

				} else {

					if (kenaikanPangkat != null && kenaikanPangkat.getInsentif() != null) {
						return kenaikanPangkat.getInsentif();
					}

					Golongan golonganPegawai = kenaikanPangkat == null ? null : kenaikanPangkat.getGolongan();
					Integer masaKerjaTahun = 1;
					try {
						Double d = MasaKerjaUtil.hitung(this);
						masaKerjaTahun = d == null ? 1 : d.intValue();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pegawai.java:1388");
					}

					if (golonganPegawai != null && masaKerjaTahun != null) {

						List<Insentif> insentifs = new ArrayList<Insentif>();

						Insentif gp = null;
						for (Object o : ConstantValues.ambilBerdasarClass(Insentif.class).values()) {
							Insentif g = (Insentif) o;
							if (g.getMasaKerja() != null && g.getGolongan() != null
									&& g.getGolongan().getId().equals(golonganPegawai.getId())) {
								if (g.getTanggalEfektif().before(sekarang)
										|| Common.dateFormat1.get().format(g.getTanggalEfektif()).equals(s)) {
									insentifs.add(g);
								}

							}
						}

						for (Insentif g : insentifs) {
							if (masaKerjaTahun.equals(g.getMasaKerja())) {
								gp = g;
								break;
							}
						}

						if (gp == null) {
							for (Insentif g : insentifs) {
								if (masaKerjaTahun < g.getMasaKerja()) {
									gp = g;
									break;
								}
							}
						}
						insentifs = null;
						insentif = gp;
					}
				}

				kenaikanPangkats = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:1430");
			// TODO: handle exception
		}

		return insentif;
	}

	/**
	 * Menghitung <b>tunjangan makan</b> yang berlaku pada tanggal tertentu. Versi paling sederhana
	 * dari keluarga {@code ambil*} berbasis SK: bila {@link KenaikanPangkat} yang berlaku sudah
	 * menunjuk {@link Makan}, itu langsung dikembalikan; kalau tidak, dicari di tabel master
	 * berdasarkan golongan SK dan tahun masa kerja ({@code MasaKerjaUtil.hitung(this)}), dengan
	 * aturan cocok-persis lalu ambil-yang-lebih-besar seperti pada {@link #ambilGajiPokok(Date)}.
	 *
	 * <p>Tidak mengenal jalur "nilai ditentukan langsung di SK" maupun kenaikan berkala.</p>
	 *
	 * @param sekarang tanggal acuan perhitungan
	 * @return tunjangan makan yang berlaku, atau {@code null}
	 * @see #ambilGajiPokok(Date)
	 */
	public Makan ambilMakan(Date sekarang) {
		Makan makan = null;
		try {
			if (getId() != null) {
				List<KenaikanPangkat> kenaikanPangkats = ambilKenaikanPangkat(sekarang);
				KenaikanPangkat kenaikanPangkat = kenaikanPangkats.isEmpty() ? null : kenaikanPangkats.get(0);

				if (kenaikanPangkat != null && kenaikanPangkat.getMakan() != null) {
					return kenaikanPangkat.getMakan();
				}

				Golongan golonganPegawai = kenaikanPangkat == null ? null : kenaikanPangkat.getGolongan();
				Integer masaKerjaTahun = 1;
				try {
					Double d = MasaKerjaUtil.hitung(this);
					masaKerjaTahun = d == null ? 1 : d.intValue();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pegawai.java:1455");
				}
				String s = Common.dateFormat1.get().format(sekarang);
				if (golonganPegawai != null && masaKerjaTahun != null) {

					List<Makan> makans = new ArrayList<Makan>();

					Makan gp = null;
					for (Object o : ConstantValues.ambilBerdasarClass(Makan.class).values()) {
						Makan g = (Makan) o;
						if (g.getMasaKerja() != null && g.getGolongan() != null
								&& g.getGolongan().getId().equals(golonganPegawai.getId())) {
							if (g.getTanggalEfektif().before(sekarang)
									|| Common.dateFormat1.get().format(g.getTanggalEfektif()).equals(s)) {
								makans.add(g);
							}

						}
					}

					for (Makan g : makans) {
						if (masaKerjaTahun.equals(g.getMasaKerja())) {
							gp = g;
							break;
						}
					}

					if (gp == null) {
						for (Makan g : makans) {
							if (masaKerjaTahun < g.getMasaKerja()) {
								gp = g;
								break;
							}
						}
					}
					makans = null;
					makan = gp;
				}

				kenaikanPangkats = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:1496");
			// TODO: handle exception
		}

		return makan;
	}

	/**
	 * Menghitung <b>tunjangan transport</b> yang berlaku pada tanggal tertentu. Badannya identik
	 * dengan {@link #ambilMakan(Date)}, hanya tabel masternya {@link Transport}.
	 *
	 * @param sekarang tanggal acuan perhitungan
	 * @return tunjangan transport yang berlaku, atau {@code null}
	 * @see #ambilMakan(Date)
	 * @see #ambilGajiPokok(Date)
	 */
	public Transport ambilTransport(Date sekarang) {
		Transport transport = null;
		try {
			if (getId() != null) {
				List<KenaikanPangkat> kenaikanPangkats = ambilKenaikanPangkat(sekarang);
				KenaikanPangkat kenaikanPangkat = kenaikanPangkats.isEmpty() ? null : kenaikanPangkats.get(0);

				if (kenaikanPangkat != null && kenaikanPangkat.getTransport() != null) {
					return kenaikanPangkat.getTransport();
				}

				Golongan golonganPegawai = kenaikanPangkat == null ? null : kenaikanPangkat.getGolongan();
				Integer masaKerjaTahun = 1;
				try {
					Double d = MasaKerjaUtil.hitung(this);
					masaKerjaTahun = d == null ? 1 : d.intValue();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pegawai.java:1521");
				}
				String s = Common.dateFormat1.get().format(sekarang);
				if (golonganPegawai != null && masaKerjaTahun != null) {

					List<Transport> transports = new ArrayList<Transport>();

					Transport gp = null;
					for (Object o : ConstantValues.ambilBerdasarClass(Transport.class).values()) {
						Transport g = (Transport) o;
						if (g.getMasaKerja() != null && g.getGolongan() != null
								&& g.getGolongan().getId().equals(golonganPegawai.getId())) {
							if (g.getTanggalEfektif().before(sekarang)
									|| Common.dateFormat1.get().format(g.getTanggalEfektif()).equals(s)) {
								transports.add(g);
							}

						}
					}

					for (Transport g : transports) {
						if (masaKerjaTahun.equals(g.getMasaKerja())) {
							gp = g;
							break;
						}
					}

					if (gp == null) {
						for (Transport g : transports) {
							if (masaKerjaTahun < g.getMasaKerja()) {
								gp = g;
								break;
							}
						}
					}
					transports = null;
					transport = gp;
				}

				kenaikanPangkats = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:1562");
			// TODO: handle exception
		}

		return transport;
	}

	/**
	 * Kecamatan domisili — dicerminkan dari {@code BiodataDosen.getKecamatan().getNama()} atau
	 * {@code Guru.getKecamatan().getNama()}, dengan penjagaan null (menjadi string kosong).
	 *
	 * @return nama kecamatan, string kosong bila relasi wilayah kosong, atau {@code null} bila
	 *         tidak ada sumber sama sekali
	 */
	public String getAlamatKecamatan() {
		dosen = getDosen();
		if (dosen != null) {
			BiodataDosen biodataDosen = dosen.ambilBiodata();
			if (biodataDosen != null) {
				this.alamatKecamatan = biodataDosen.getKecamatan() == null ? "" : biodataDosen.getKecamatan().getNama();
			}
		}
		guru = getGuru();
		if (guru != null) {
			this.alamatKecamatan = guru.getKecamatan() == null ? "" : guru.getKecamatan().getNama();
		}
		return alamatKecamatan;
	}

	/**
	 * Mengisi kecamatan domisili.
	 *
	 * @param alamatKecamatan nama kecamatan
	 */
	public void setAlamatKecamatan(String alamatKecamatan) {
		this.alamatKecamatan = alamatKecamatan;
	}

	/**
	 * Membandingkan dua {@link TipeMasaKerja} berdasarkan ID secara <b>null-safe</b>. Dipakai oleh
	 * seluruh getter tanggal masuk/keluar untuk menentukan rentang mana yang boleh ditampilkan.
	 *
	 * <p>BUG FIX (NPE di {@link #getAwalmasuk()} &amp; getter tipe-masa-kerja lain): sebelumnya
	 * kode langsung memanggil {@code getTipeMasaKerja().getId().equals(...)}. Untuk data Pegawai
	 * lama/tidak lengkap, {@link #getTipeMasaKerja()} bisa mengembalikan objek non-null tapi
	 * {@code getId()}-nya {@code null} (mis. entitas master belum tersimpan/ID hilang) —
	 * memanggil {@code .equals()} pada referensi null melempar {@link NullPointerException}.
	 * Helper ini membandingkan ID secara null-safe tanpa mengubah hasil untuk data yang
	 * valid/lengkap.</p>
	 *
	 * @param a tipe masa kerja pertama; boleh {@code null}
	 * @param b tipe masa kerja kedua; boleh {@code null}
	 * @return {@code true} hanya bila keduanya non-null, ID {@code a} non-null, dan kedua ID sama
	 */
	private static boolean tipeMasaKerjaCocok(TipeMasaKerja a, TipeMasaKerja b) {
		if (a == null || b == null) {
			return false;
		}
		Long idA = a.getId();
		Long idB = b.getId();
		return idA != null && idA.equals(idB);
	}

	/**
	 * Tanggal mulai berstatus <b>pegawai tetap</b> — rentang keempat (paling akhir) dari empat
	 * rentang masa kerja yang berjalan paralel di kelas ini.
	 *
	 * <p>Nilainya <b>disaring oleh {@link #getTipeMasaKerja()}</b>:</p>
	 * <ul>
	 * <li>tipe = {@code Tetap} → kolom dikembalikan; bila kolom kosong, dipakai <b>tanggal hari
	 * ini</b> ({@code WaktuUtil.getDate()}) sebagai pengganti;</li>
	 * <li>tipe lain (Honorer/Semi Tetap) → {@code null}, walaupun kolomnya berisi — pegawai yang
	 * belum tetap memang tidak boleh punya tanggal masuk tetap;</li>
	 * <li>bila master {@code TipeMasaKerja.Tetap} belum terinisialisasi ({@code null}, mis. saat
	 * aplikasi baru naik) → kolom dikembalikan apa adanya.</li>
	 * </ul>
	 *
	 * <p><b>Perhatikan efek "hari ini" itu:</b> pegawai tetap tanpa tanggal masuk akan terbaca
	 * seolah baru masuk hari ini, sehingga {@link #ambilMasaKerjaTahun()} menghasilkan 0.</p>
	 *
	 * @return tanggal masuk sebagai pegawai tetap, atau {@code null} bila tipe masa kerjanya tidak
	 *         mengizinkan
	 * @see #getAwalmasuk()
	 * @see #getTanggalkeluar()
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalmasuk() {
		if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null
				&& tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)) {
			return tanggalmasuk == null ? WaktuUtil.getDate() : tanggalmasuk;
		} else if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null
				&& !tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)) {
			return null;
		} else {
			return tanggalmasuk;
		}
	}

	/**
	 * Mengisi tanggal mulai berstatus pegawai tetap.
	 *
	 * @param tanggalmasuk tanggal masuk sebagai pegawai tetap
	 */
	public void setTanggalmasuk(Date tanggalmasuk) {
		this.tanggalmasuk = tanggalmasuk;
	}

	/**
	 * Nama panggilan/sapaan sehari-hari.
	 *
	 * @return nama panggilan, atau {@code null}
	 */
	public String getPanggilan() {
		return panggilan;
	}

	/**
	 * Mengisi nama panggilan.
	 *
	 * @param panggilan nama panggilan
	 */
	public void setPanggilan(String panggilan) {
		this.panggilan = panggilan;
	}

	/**
	 * Gelar akademik yang ditulis di depan nama (mis. "Dr.", "Prof."). Dicerminkan dari
	 * {@code Dosen.getGelarDepan()} hanya bila nilai di sana tidak kosong, sehingga gelar yang
	 * diisi di baris kepegawaian tidak terhapus.
	 *
	 * @return gelar depan, atau {@code null}
	 * @see #getGelarBelakang()
	 */
	public String getGelarDepan() {
		if (getDosen() != null && getDosen().getGelarDepan() != null && !getDosen().getGelarDepan().trim().isEmpty()) {
			gelarDepan = getDosen().getGelarDepan();
		}
		return gelarDepan;
	}

	/**
	 * Mengisi gelar depan.
	 *
	 * @param gelarDepan gelar akademik di depan nama
	 */
	public void setGelarDepan(String gelarDepan) {
		this.gelarDepan = gelarDepan;
	}

	/**
	 * Gelar akademik yang ditulis di belakang nama (mis. "S.Kom., M.T."). Dicerminkan dari
	 * {@code Dosen.getGelarBelakang()} hanya bila nilai di sana tidak kosong.
	 *
	 * @return gelar belakang, atau {@code null}
	 * @see #getGelarDepan()
	 */
	public String getGelarBelakang() {
		if (getDosen() != null && getDosen().getGelarBelakang() != null
				&& !getDosen().getGelarBelakang().trim().isEmpty()) {
			gelarBelakang = getDosen().getGelarBelakang();
		}
		return gelarBelakang;
	}

	/**
	 * Mengisi gelar belakang.
	 *
	 * @param gelarBelakang gelar akademik di belakang nama
	 */
	public void setGelarBelakang(String gelarBelakang) {
		this.gelarBelakang = gelarBelakang;
	}

	/**
	 * Golongan darah versi lama (kolom {@code darah}). Bidang yang benar-benar dipakai layar
	 * biodata sekarang adalah {@link #getGolonganDarah()}; field ini dipertahankan untuk data
	 * historis.
	 *
	 * @return golongan darah lama, atau {@code null}
	 * @see #getGolonganDarah()
	 */
	public String getDarah() {
		return darah;
	}

	/**
	 * Mengisi golongan darah versi lama.
	 *
	 * @param darah golongan darah
	 */
	public void setDarah(String darah) {
		this.darah = darah;
	}

	/**
	 * Nomor telepon seluler, <b>sudah dibersihkan</b>: semua karakter selain digit dan titik
	 * dibuang ({@code replaceAll("[^\\d.]", "")}), sehingga spasi, tanda hubung, dan tanda kurung
	 * hilang. Nilainya dicerminkan dari {@code Dosen.getHp()} / {@code Guru.getHp()} bila ada.
	 *
	 * <p><b>Konsekuensi:</b> awalan {@code +} pada nomor internasional ikut terbuang. Untuk nomor
	 * yang siap dipakai WhatsApp (dengan awalan {@code +62}) gunakan {@link #ambilNoHp()}.</p>
	 *
	 * @return nomor HP berisi digit saja; string kosong bila tidak ada
	 * @see #ambilNoHp()
	 * @see #tampilkanHp(Component, String)
	 */
	public String getHp() {
		dosen = getDosen();
		if (dosen != null) {
			this.hp = dosen.getHp();
		}
		guru = getGuru();
		if (guru != null) {
			this.hp = guru.getHp();
		}
		return hp == null || hp.isEmpty() ? "" : hp.trim().replaceAll("[^\\d.]", "");
	}

	/**
	 * Mengisi nomor HP mentah (pembersihan dilakukan oleh {@link #getHp()}).
	 *
	 * @param hp nomor telepon seluler
	 */
	public void setHp(String hp) {
		this.hp = hp;
	}

	/**
	 * Bank tujuan transfer untuk <b>slot pembayaran pertama</b>. Kelas ini menyediakan lima slot
	 * paralel ({@code bank}…{@code bank5}) agar satu pegawai bisa menerima beberapa komponen gaji
	 * di rekening berbeda.
	 *
	 * @return bank slot 1, atau {@code null}
	 * @see #ambilBank(FormatItemGaji)
	 * @see #getNorek()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank", nullable = true)
	public Bank getBank() {
		bank = check(bank);
		return bank;
	}

	/**
	 * Mengisi bank slot 1.
	 *
	 * @param bank bank tujuan transfer
	 */
	public void setBank(Bank bank) {
		this.bank = bank;
	}

	/**
	 * Nomor rekening untuk slot pembayaran pertama.
	 *
	 * @return nomor rekening slot 1, atau {@code null}
	 * @see #ambilNoRek(FormatItemGaji)
	 */
	public String getNorek() {
		return norek;
	}

	/**
	 * Mengisi nomor rekening slot 1.
	 *
	 * @param norek nomor rekening
	 */
	public void setNorek(String norek) {
		this.norek = norek;
	}

	/**
	 * Nomor Karis/Karsu (kartu istri/suami) pegawai negeri.
	 *
	 * @return nomor Karis/Karsu, atau {@code null}
	 */
	public String getKaris() {
		return karis;
	}

	/**
	 * Mengisi nomor Karis/Karsu.
	 *
	 * @param karis nomor Karis/Karsu
	 */
	public void setKaris(String karis) {
		this.karis = karis;
	}

	/**
	 * Nomor kepesertaan asuransi kesehatan (Askes/BPJS Kesehatan).
	 *
	 * @return nomor Askes, atau {@code null}
	 */
	public String getAskes() {
		return askes;
	}

	/**
	 * Mengisi nomor Askes/BPJS Kesehatan.
	 *
	 * @param askes nomor kepesertaan
	 */
	public void setAskes(String askes) {
		this.askes = askes;
	}

	/**
	 * Nomor Taspen (tabungan/asuransi pensiun pegawai negeri).
	 *
	 * @return nomor Taspen, atau {@code null}
	 */
	public String getTaspen() {
		return taspen;
	}

	/**
	 * Mengisi nomor Taspen.
	 *
	 * @param taspen nomor Taspen
	 */
	public void setTaspen(String taspen) {
		this.taspen = taspen;
	}

	/**
	 * Kolom {@code karpeg} — meskipun namanya "kartu pegawai", isinya <b>bukan</b> sekadar satu
	 * nomor: {@link #putPhoto(Map)} memperlakukannya sebagai <b>daftar ID {@code LampiranLain}
	 * dipisah koma</b>, yaitu berkas-berkas dokumen pegawai yang dilampirkan ke laporan. Bertipe
	 * {@code text} agar daftarnya bisa panjang.
	 *
	 * @return isi kolom tanpa spasi tepi; string kosong bila belum diisi
	 * @see #putPhoto(Map)
	 */
	@Column(columnDefinition = "text")
	public String getKarpeg() {
		return karpeg == null ? "" : karpeg.trim();
	}

	/**
	 * Mengisi kolom {@code karpeg} (daftar ID lampiran dipisah koma).
	 *
	 * @param karpeg daftar ID lampiran
	 */
	public void setKarpeg(String karpeg) {
		this.karpeg = karpeg;
	}

	/**
	 * NPWP pegawai — <b>getter cermin</b> ke {@code Dosen.getNpwp()} / {@code Guru.getNpwp()}.
	 * Dipakai bersama {@link PtkpPegawai} untuk perhitungan PPh 21.
	 *
	 * @return NPWP, atau {@code null}
	 */
	public String getNpwp() {
		dosen = getDosen();
		if (dosen != null) {
			this.npwp = dosen.getNpwp();
		}
		guru = getGuru();
		if (guru != null) {
			this.npwp = guru.getNpwp();
		}
		return npwp;
	}

	/**
	 * Mengisi NPWP pada field lokal saja.
	 *
	 * @param npwp nomor NPWP
	 */
	public void setNpwp(String npwp) {
		this.npwp = npwp;
	}

	/**
	 * Provinsi sebagai <b>relasi master wilayah</b> (berbeda dari {@link #getAlamatPropinsi()} yang
	 * hanya teks turunan).
	 *
	 * @return provinsi, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "propinsi", nullable = true)
	public Propinsi getPropinsi() {
		propinsi = check(propinsi);
		return propinsi;
	}

	/**
	 * Mengisi relasi provinsi.
	 *
	 * @param propinsi provinsi
	 */
	public void setPropinsi(Propinsi propinsi) {
		this.propinsi = propinsi;
	}

	/**
	 * Kota/kabupaten sebagai relasi master wilayah (berbeda dari {@link #getAlamatKabupaten()}
	 * yang hanya teks turunan).
	 *
	 * @return kota, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kota", nullable = true)
	public Kota getKota() {
		kota = check(kota);
		return kota;
	}

	/**
	 * Mengisi relasi kota/kabupaten.
	 *
	 * @param kota kota
	 */
	public void setKota(Kota kota) {
		this.kota = kota;
	}

	/**
	 * Catatan bebas tentang pegawai.
	 *
	 * @return keterangan, atau {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi catatan bebas.
	 *
	 * @param keterangan keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Usia pensiun pegawai dalam tahun. Bila kolom kosong, getter <b>menetapkan</b> default
	 * {@code 55} ke field (bukan sekadar mengembalikannya), sehingga nilai itu ikut tersimpan pada
	 * flush berikutnya.
	 *
	 * <p>Batas usia pensiun berbeda-beda per jenis jabatan (dosen bisa 65/70), jadi nilai default
	 * ini hanya aman untuk staf administrasi — periksa data sebelum memakainya untuk proyeksi
	 * pensiun.</p>
	 *
	 * @return usia pensiun dalam tahun, tidak pernah {@code null}
	 */
	public Integer getUsiaPensiun() {
		if (usiaPensiun == null) {
			usiaPensiun = 55;
		}
		return usiaPensiun;
	}

	/**
	 * Mengisi usia pensiun.
	 *
	 * @param usiaPensiun usia pensiun dalam tahun
	 */
	public void setUsiaPensiun(Integer usiaPensiun) {
		this.usiaPensiun = usiaPensiun;
	}

	/**
	 * NIP lama (format sebelum penomoran baru), disimpan terpisah agar dokumen historis tetap
	 * dapat ditelusuri. Ikut dicetak pada QR tanda tangan, lihat {@link #ttdQr()}.
	 *
	 * @return NIP lama, atau {@code null}
	 * @see #getCode()
	 */
	@Column(name = "niplama")
	public String getNipLama() {
		return nipLama;
	}

	/**
	 * Mengisi NIP lama.
	 *
	 * @param nipLama NIP lama
	 */
	public void setNipLama(String nipLama) {
		this.nipLama = nipLama;
	}

	/**
	 * TMT (terhitung mulai tanggal) jabatan yang sedang diemban.
	 *
	 * @return tanggal mulai menjabat, atau {@code null}
	 * @see #getJabatan()
	 */
	@Column(name = "tmt_jabatan")
	public Date getTmtJabatan() {
		return tmtJabatan;
	}

	/**
	 * Mengisi TMT jabatan.
	 *
	 * @param tmtJabatan tanggal mulai menjabat
	 */
	public void setTmtJabatan(Date tmtJabatan) {
		this.tmtJabatan = tmtJabatan;
	}

	/**
	 * Relasi ke {@link Dosen} bila baris kepegawaian ini merupakan proyeksi seorang dosen.
	 * <b>Inilah sakelar utama</b> yang mengaktifkan seluruh pola getter "cermin" di kelas ini
	 * (lihat Javadoc kelas), dan yang membuat {@link #getTipePegawai()} menyimpulkan
	 * {@code TipePegawai.DOSEN}.
	 *
	 * <p>Kolomnya {@code unique = true}: satu dosen paling banyak punya satu baris pegawai.
	 * Tautan baliknya bukan relasi Hibernate, melainkan kolom {@code Dosen.pegawaiId}.</p>
	 *
	 * @return dosen sumber, atau {@code null} bila ini pegawai murni/guru
	 * @see #createDataPegawaiDariDosen(Session, Dosen)
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", unique = true, nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Mengaitkan baris kepegawaian ini dengan seorang dosen. Perubahan ini mengubah perilaku
	 * puluhan getter lain (menjadi cermin data dosen) — jangan dipakai untuk sekadar "menandai"
	 * relasi sementara.
	 *
	 * @param dosen dosen sumber
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Bahasa antarmuka pilihan pegawai, default {@code Tbmuser.INDONESIA} bila belum pernah
	 * dipilih. {@code @NotAudited} karena preferensi tampilan tidak perlu masuk tabel audit
	 * Envers.
	 *
	 * @return kode bahasa antarmuka, tidak pernah {@code null}
	 */
	@NotAudited
	public String getBahasa() {
		return bahasa == null ? Tbmuser.INDONESIA : bahasa;
	}

	/**
	 * Mengisi bahasa antarmuka.
	 *
	 * @param bahasa kode bahasa
	 */
	public void setBahasa(String bahasa) {
		this.bahasa = bahasa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	/**
	 * Cabang (unit organisasi payroll) tempat pegawai terdaftar.
	 *
	 * @return cabang, atau {@code null}
	 * @see #getDepartemen()
	 */
	@JoinColumn(name = "cabang", nullable = true)
	public Cabang getCabang() {
		cabang = check(cabang);
		return cabang;
	}

	/**
	 * Mengisi cabang.
	 *
	 * @param cabang cabang payroll
	 */
	public void setCabang(Cabang cabang) {
		this.cabang = cabang;
	}

	/**
	 * Departemen (unit organisasi payroll di bawah cabang).
	 *
	 * @return departemen, atau {@code null}
	 * @see #getCabang()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "departemen", nullable = true)
	public Departemen getDepartemen() {
		departemen = check(departemen);
		return departemen;
	}

	/**
	 * Mengisi departemen.
	 *
	 * @param departemen departemen payroll
	 */
	public void setDepartemen(Departemen departemen) {
		this.departemen = departemen;
	}

	/**
	 * Level jabatan pada struktur payroll. Menjadi <b>pilihan terakhir</b> sumber nama jabatan di
	 * {@link #getJabatan()}, setelah jabatan struktural dan fungsional.
	 *
	 * @return level jabatan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "level_jabatan", nullable = true)
	public LevelJabatan getLevelJabatan() {
		levelJabatan = check(levelJabatan);
		return levelJabatan;
	}

	/**
	 * Mengisi level jabatan.
	 *
	 * @param levelJabatan level jabatan
	 */
	public void setLevelJabatan(LevelJabatan levelJabatan) {
		this.levelJabatan = levelJabatan;
	}

	/**
	 * Nama bank tujuan transfer sebagai teks siap cetak. Bila relasi {@link #getBank()} terisi,
	 * kolom ini <b>ditimpa</b> dengan nama bank tersebut, sehingga isian manual hanya bertahan
	 * selama belum ada relasi bank.
	 *
	 * @return nama bank tujuan transfer, atau {@code null}
	 */
	public String getDitransferKe() {
		if (getBank() != null) {
			ditransferKe = getBank().getNama();
		}
		return ditransferKe;
	}

	/**
	 * Mengisi nama bank tujuan transfer secara manual.
	 *
	 * @param ditransferKe nama bank
	 */
	public void setDitransferKe(String ditransferKe) {
		this.ditransferKe = ditransferKe;
	}

	/**
	 * Tanggal berakhirnya masa <b>pegawai tetap</b> (pensiun/keluar). Disaring
	 * {@link #getTipeMasaKerja()} seperti {@link #getTanggalmasuk()}: hanya tampil untuk tipe
	 * {@code Tetap}, {@code null} untuk tipe lain.
	 *
	 * <p><b>Catatan struktur:</b> cabang {@code if} pertama dan cabang {@code else} terakhir
	 * mengembalikan nilai yang sama, jadi efektifnya hanya cabang tengah ("bukan tetap →
	 * {@code null}") yang berpengaruh. Berbeda dari {@link #getTanggalmasuk()}, di sini tidak ada
	 * penggantian dengan tanggal hari ini.</p>
	 *
	 * @return tanggal keluar, atau {@code null}
	 */
	public Date getTanggalkeluar() {
		if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null
				&& tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)) {
			return tanggalkeluar;
		} else if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null
				&& !tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)) {
			return null;
		} else {
			return tanggalkeluar;
		}
	}

	/**
	 * Mengisi tanggal keluar/pensiun pegawai tetap.
	 *
	 * @param tanggalkeluar tanggal berakhirnya masa kerja tetap
	 */
	public void setTanggalkeluar(Date tanggalkeluar) {
		this.tanggalkeluar = tanggalkeluar;
	}

	/**
	 * Nomor kepesertaan Jamsostek/BPJS Ketenagakerjaan.
	 *
	 * @return nomor Jamsostek, atau {@code null}
	 */
	public String getJamsostek() {
		return jamsostek;
	}

	/**
	 * Mengisi nomor Jamsostek/BPJS Ketenagakerjaan.
	 *
	 * @param jamsostek nomor kepesertaan
	 */
	public void setJamsostek(String jamsostek) {
		this.jamsostek = jamsostek;
	}

	/**
	 * Cara pembayaran untuk <b>slot pembayaran pertama</b>, dengan default
	 * {@link #CARA_BAYAR_DITRASFER} bila kolom kosong.
	 *
	 * <p>Perhatikan bahwa default slot 1 berbeda dari slot 2–5 yang memakai
	 * {@link #CARA_BAYAR_LAINNYA} — slot pertama diasumsikan gaji utama yang ditransfer.</p>
	 *
	 * @return salah satu konstanta {@code CARA_BAYAR_*}; tidak pernah {@code null}
	 */
	public String getCaraPembayaran() {
		return caraPembayaran == null || caraPembayaran.trim().isEmpty() ? Pegawai.CARA_BAYAR_DITRASFER
				: caraPembayaran;
	}

	/**
	 * Mengisi cara pembayaran slot 1.
	 *
	 * @param caraPembayaran salah satu konstanta {@code CARA_BAYAR_*}
	 */
	public void setCaraPembayaran(String caraPembayaran) {
		this.caraPembayaran = caraPembayaran;
	}

	/**
	 * Cara pembayaran slot 2, default {@link #CARA_BAYAR_LAINNYA} bila kolom kosong.
	 *
	 * @return salah satu konstanta {@code CARA_BAYAR_*}; tidak pernah {@code null}
	 * @see #getCaraPembayaran()
	 */
	public String getCaraPembayaran2() {
		return caraPembayaran2 == null || caraPembayaran2.trim().isEmpty() ? Pegawai.CARA_BAYAR_LAINNYA
				: caraPembayaran2;
	}

	/**
	 * Mengisi cara pembayaran slot 2.
	 *
	 * @param caraPembayaran2 salah satu konstanta {@code CARA_BAYAR_*}
	 */
	public void setCaraPembayaran2(String caraPembayaran2) {
		this.caraPembayaran2 = caraPembayaran2;
	}

	/**
	 * Cara pembayaran slot 3, default {@link #CARA_BAYAR_LAINNYA} bila kolom kosong.
	 *
	 * @return salah satu konstanta {@code CARA_BAYAR_*}; tidak pernah {@code null}
	 * @see #getCaraPembayaran()
	 */
	public String getCaraPembayaran3() {
		return caraPembayaran3 == null || caraPembayaran3.trim().isEmpty() ? Pegawai.CARA_BAYAR_LAINNYA
				: caraPembayaran3;
	}

	/**
	 * Mengisi cara pembayaran slot 3.
	 *
	 * @param caraPembayaran3 salah satu konstanta {@code CARA_BAYAR_*}
	 */
	public void setCaraPembayaran3(String caraPembayaran3) {
		this.caraPembayaran3 = caraPembayaran3;
	}

	/**
	 * Nama pemilik rekening slot 1, jatuh ke nama pegawai bila belum diisi (kasus umum: rekening
	 * atas nama sendiri). Berguna saat gaji ditransfer ke rekening pihak lain (mis. ahli waris).
	 *
	 * <p><b>Perhatikan:</b> nilai pengganti diambil dari <b>field</b> {@code nama}, bukan
	 * {@link #getNama()}, sehingga untuk pegawai turunan dosen/guru yang belum pernah dibaca
	 * lewat {@code getNama()} hasilnya bisa {@code null}.</p>
	 *
	 * @return nama pemilik rekening slot 1
	 */
	public String getDitransferAtasNama() {
		return ditransferAtasNama == null || ditransferAtasNama.trim().isEmpty() ? nama : ditransferAtasNama;
	}

	/**
	 * Mengisi nama pemilik rekening slot 1.
	 *
	 * @param ditransferAtasNama nama pemilik rekening
	 */
	public void setDitransferAtasNama(String ditransferAtasNama) {
		this.ditransferAtasNama = ditransferAtasNama;
	}

	/**
	 * Jumlah anak (tanggungan), default {@code 0}. Menjadi masukan perhitungan tunjangan keluarga
	 * dan PTKP bersama {@link #getPtkpPegawai()}.
	 *
	 * @return jumlah anak; tidak pernah {@code null}
	 */
	public Integer getJumlahAnak() {
		return jumlahAnak == null ? 0 : jumlahAnak;
	}

	/**
	 * Mengisi jumlah anak/tanggungan.
	 *
	 * @param jumlahAnak jumlah anak
	 */
	public void setJumlahAnak(Integer jumlahAnak) {
		this.jumlahAnak = jumlahAnak;
	}

	/**
	 * Jenjang pendidikan terakhir — <b>getter cermin</b> ke {@code Dosen.getPendidikan()} /
	 * {@code Guru.getPendidikan()}, lalu di-{@code check()}.
	 *
	 * @return jenjang pendidikan, atau {@code null}
	 * @see #getDeskripsiPendidikan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan", nullable = true)
	public Pendidikan getPendidikan() {
		dosen = getDosen();
		if (dosen != null) {
			this.pendidikan = dosen.getPendidikan();
		}
		guru = getGuru();
		if (guru != null) {
			this.pendidikan = guru.getPendidikan();
		}
		pendidikan = check(pendidikan);
		return pendidikan;
	}

	/**
	 * Mengisi jenjang pendidikan terakhir.
	 *
	 * @param pendidikan jenjang pendidikan
	 */
	public void setPendidikan(Pendidikan pendidikan) {
		this.pendidikan = pendidikan;
	}

	/**
	 * Uraian bebas pendidikan (nama program studi, institusi, tahun lulus) sebagai pelengkap
	 * relasi terstruktur {@link #getPendidikan()}.
	 *
	 * @return deskripsi pendidikan, atau {@code null}
	 */
	public String getDeskripsiPendidikan() {
		return deskripsiPendidikan;
	}

	/**
	 * Mengisi uraian pendidikan.
	 *
	 * @param deskripsiPendidikan deskripsi pendidikan
	 */
	public void setDeskripsiPendidikan(String deskripsiPendidikan) {
		this.deskripsiPendidikan = deskripsiPendidikan;
	}

	/**
	 * Jenis tenaga kependidikan (pustakawan, laboran, teknisi, administrasi, dan sebagainya) —
	 * dipakai untuk pelaporan ketenagaan.
	 *
	 * @return jenis tenaga kependidikan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_tenaga_kependidikan", nullable = true)
	public JenisTenagaKependidikan getJenisTenagaKependidikan() {
		jenisTenagaKependidikan = check(jenisTenagaKependidikan);
		return jenisTenagaKependidikan;
	}

	/**
	 * Mengisi jenis tenaga kependidikan.
	 *
	 * @param jenisTenagaKependidikan jenis tenaga kependidikan
	 */
	public void setJenisTenagaKependidikan(JenisTenagaKependidikan jenisTenagaKependidikan) {
		this.jenisTenagaKependidikan = jenisTenagaKependidikan;
	}

	/**
	 * Menandai apakah pegawai sudah tersertifikasi (sertifikasi pendidik/profesi), default
	 * {@code false} bila kolom kosong.
	 *
	 * @return {@code true} bila tersertifikasi; tidak pernah {@code null}
	 */
	public Boolean getSertifikasi() {
		return sertifikasi == null ? false : sertifikasi;
	}

	/**
	 * Mengisi bendera sertifikasi.
	 *
	 * @param sertifikasi {@code true} bila tersertifikasi
	 */
	public void setSertifikasi(Boolean sertifikasi) {
		this.sertifikasi = sertifikasi;
	}

	/**
	 * Relasi ke {@link Guru} bila baris kepegawaian ini merupakan proyeksi seorang guru sekolah.
	 * Sakelar kembar {@link #getDosen()}: mengaktifkan pola getter "cermin" jalur guru dan membuat
	 * {@link #getTipePegawai()} menyimpulkan {@code TipePegawai.GURU}.
	 *
	 * <p>Berbeda dari jalur dosen, tautan baliknya juga relasi Hibernate penuh
	 * ({@code Guru.pegawai}), dan kolom di sini <b>tidak</b> ditandai unik.</p>
	 *
	 * @return guru sumber, atau {@code null}
	 * @see #createDataPegawaiDariGuru(Session, Guru)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Mengaitkan baris kepegawaian ini dengan seorang guru.
	 *
	 * @param guru guru sumber
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * ID pegawai pada mesin absensi sidik jari.
	 *
	 * <p>Punya <b>dua lapis sumber</b>. Pertama pencerminan dari {@code Dosen.getIdfinger()} lalu
	 * {@code Guru.getIdfinger()} — masing-masing dibungkus {@code try/catch} tersendiri karena
	 * getter sumber dapat melempar {@link NullPointerException} pada data lama. Bila hasilnya
	 * masih kosong, nilai diambil dari <b>cache berkas</b> milik {@link GeneralValueObject}
	 * lewat {@code retreive("idfinger")}, yaitu tempat {@link #setIdfinger(String)} menyimpannya.</p>
	 *
	 * @return ID sidik jari tanpa spasi tepi, atau {@code null} bila tidak ada di mana pun
	 * @see #setIdfinger(String)
	 */
	public String getIdfinger() {
		try {
			dosen = getDosen();
			if (dosen != null && !dosen.getIdfinger().trim().isEmpty()) {
				idfinger = dosen.getIdfinger();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:1981");
			// TODO: handle exception
		}

		try {
			guru = getGuru();
			if (guru != null && !guru.getIdfinger().trim().isEmpty()) {
				idfinger = guru.getIdfinger();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:1990");
			// TODO: handle exception
		}

		String s = idfinger == null || idfinger.trim().isEmpty() ? retreive("idfinger") : idfinger;
		return s == null ? null : s.trim();
	}

	/**
	 * Mengisi ID sidik jari <b>dan</b> menuliskannya ke cache berkas
	 * ({@code GeneralValueObject.put(nilai, "idfinger")}) supaya tetap terbaca meski kolomnya
	 * belum tersimpan ke basis data.
	 *
	 * <p>Nilai {@code null}/kosong <b>diabaikan sepenuhnya</b> — ID sidik jari tidak bisa dihapus
	 * lewat setter ini.</p>
	 *
	 * @param idfinger ID pada mesin absensi; diabaikan bila null/kosong
	 */
	public void setIdfinger(String idfinger) {
		if (idfinger != null && !idfinger.trim().isEmpty()) {
			put(idfinger.trim(), "idfinger");
			this.idfinger = idfinger;
		}
	}

	/**
	 * Menandai apakah baris kepegawaian ini masih aktif — dipakai sebagai saringan utama di hampir
	 * semua daftar pegawai (termasuk pencarian di
	 * {@link #createDataPegawaiDariDosen(Session, Dosen)}).
	 *
	 * <p><b>Efek samping penting:</b> bila {@link #getStatusPegawai()} bernama "Pensiun"
	 * (perbandingan tanpa peduli huruf besar/kecil), getter ini <b>menulis</b> {@code false} ke
	 * field {@code aktif}, sehingga perubahan itu ikut tersimpan pada flush berikutnya. Jadi
	 * mengubah status menjadi pensiun akan menonaktifkan pegawai secara otomatis pada pembacaan
	 * pertama, bukan lewat proses tersendiri.</p>
	 *
	 * <p>Default {@code true} bila kolom kosong, sehingga data lama terbaca sebagai aktif.</p>
	 *
	 * @return {@code true} bila pegawai aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {

		statusPegawai = check(statusPegawai);
		if (statusPegawai != null && statusPegawai.getNama() != null
				&& statusPegawai.getNama().equalsIgnoreCase("Pensiun")) {
			aktif = false;
		}

		return aktif == null ? true : aktif;
	}

	/**
	 * Mengaktifkan/menonaktifkan baris kepegawaian. Perhatikan bahwa {@link #getAktif()} dapat
	 * memaksa nilainya kembali ke {@code false} bila status pegawai adalah "Pensiun".
	 *
	 * @param aktif {@code true} untuk aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Nilai tunjangan kinerja (tukin) tetap milik pegawai, default {@code 0.0}.
	 *
	 * @return tunjangan kinerja; tidak pernah {@code null}
	 * @see #getPersenKpiDefault()
	 */
	public Double getTunjanganKinerja() {
		return tunjanganKinerja == null ? 0.0 : tunjanganKinerja;
	}

	/**
	 * Mengisi tunjangan kinerja.
	 *
	 * @param tunjanganKinerja nilai tunjangan kinerja
	 */
	public void setTunjanganKinerja(Double tunjanganKinerja) {
		this.tunjanganKinerja = tunjanganKinerja;
	}

	/**
	 * Atasan langsung pertama — referensi ke baris {@code Pegawai} lain. Dipakai alur persetujuan
	 * (cuti, izin, LKP/target kerja) untuk menentukan ke siapa pengajuan diteruskan.
	 *
	 * <p><b>Penyimpulan lintas modul:</b> bila pegawai ini turunan dosen dan dosen tersebut punya
	 * {@code atasanlangsung} (disimpan sebagai <i>ID dosen</i>, bukan relasi), method menelusuri
	 * dosen atasan itu di cache {@code ConstantValues}, lalu dari {@code pegawaiId}-nya mengambil
	 * baris {@code Pegawai} yang bersangkutan dan <b>menimpa</b> nilai kolom sendiri. Jadi untuk
	 * dosen, hierarki atasan versi modul akademik menang atas isian modul kepegawaian.</p>
	 *
	 * @return atasan langsung, atau {@code null} bila tidak ada
	 * @see #getAtasanlangsung2()
	 * @see #getAtasan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "atasanlangsung", nullable = true)
	public Pegawai getAtasanlangsung() {
		atasanlangsung = check(atasanlangsung);
		dosen = getDosen();
		if (dosen != null && dosen.getAtasanlangsung() != null) {
			Dosen atasan = (Dosen) ConstantValues.ambil(Dosen.class.getName(), dosen.getAtasanlangsung());
			if (atasan != null && atasan.getPegawaiId() != null) {
				Pegawai pegawai = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), atasan.getPegawaiId());
				if (pegawai != null) {
					atasanlangsung = pegawai;
				}
			}
		}
		return atasanlangsung;
	}

	/**
	 * Mengisi atasan langsung pertama. Untuk pegawai turunan dosen, nilai ini dapat ditimpa lagi
	 * oleh {@link #getAtasanlangsung()}.
	 *
	 * @param atasanlangsung pegawai atasan
	 */
	public void setAtasanlangsung(Pegawai atasanlangsung) {
		this.atasanlangsung = atasanlangsung;
	}

	/**
	 * Mencari entri {@link JatahCuti} yang cocok untuk pegawai ini pada <b>tahun masa kerja</b>
	 * saat ini (bukan tahun kalender).
	 *
	 * <p>Memindai seluruh cache {@code ConstantValues} untuk {@link JatahCuti} dan mengembalikan
	 * entri pertama yang pegawainya sama dan kolom {@code tahun}-nya sama dengan
	 * {@link #ambilMasaKerjaTahun()}. Ini yang memungkinkan jatah cuti bertambah seiring lama
	 * bekerja.</p>
	 *
	 * <p>Seluruh badan dibungkus {@code try/catch} — kegagalan menghasilkan {@code null}, bukan
	 * exception.</p>
	 *
	 * @return entri jatah cuti untuk tahun masa kerja sekarang, atau {@code null} bila tidak ada
	 * @see #getJatahCutiTahunan()
	 */
	public JatahCuti ambilJatahCuti() {
		try {
			Integer masaKerja = this.ambilMasaKerjaTahun();
			for (Object o : ConstantValues.ambilBerdasarClass(JatahCuti.class).values()) {
				JatahCuti jatahCuti = (JatahCuti) o;
				if (jatahCuti.getPegawai() != null && getId() != null && jatahCuti.getTahun() != null
						&& jatahCuti.getPegawai().getId().equals(getId()) && masaKerja.equals(jatahCuti.getTahun())) {
					return jatahCuti;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:2059");
		}
		return null;
	}

	/**
	 * Jatah cuti tahunan pegawai dalam hari.
	 *
	 * <p>Bila ada entri {@link JatahCuti} khusus untuk tahun masa kerja sekarang
	 * ({@link #ambilJatahCuti()}), nilainya dipakai <b>dan ditulis</b> ke field {@code jatahCutiTahunan}.
	 * Kalau tidak ada, dipakai kolom sendiri, atau default {@code 12} bila kolom kosong.</p>
	 *
	 * @return jatah cuti tahunan dalam hari; tidak pernah {@code null}
	 */
	public Integer getJatahCutiTahunan() {

		JatahCuti jatahCuti = ambilJatahCuti();
		if (jatahCuti != null) {
			jatahCutiTahunan = jatahCuti.getJatahCutiTahunan();
			return jatahCutiTahunan;
		}

		return jatahCutiTahunan == null ? 12 : jatahCutiTahunan;
	}

	/**
	 * Mengisi jatah cuti tahunan khusus untuk pegawai ini. Nilai ini tetap dapat ditimpa oleh entri
	 * {@link JatahCuti} per tahun masa kerja.
	 *
	 * @param jatahCutiTahunan jumlah hari cuti tahunan
	 */
	public void setJatahCutiTahunan(Integer jatahCutiTahunan) {
		this.jatahCutiTahunan = jatahCutiTahunan;
	}

	/**
	 * Atasan langsung kedua (jalur persetujuan cadangan/paralel). Berbeda dari
	 * {@link #getAtasanlangsung()}, tidak ada penyimpulan dari hierarki dosen — hanya kolomnya
	 * sendiri.
	 *
	 * @return atasan langsung kedua, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "atasanlangsung2", nullable = true)
	public Pegawai getAtasanlangsung2() {
		atasanlangsung2 = check(atasanlangsung2);
		return atasanlangsung2;
	}

	/**
	 * Mengisi atasan langsung kedua.
	 *
	 * @param atasanlangsung2 pegawai atasan kedua
	 */
	public void setAtasanlangsung2(Pegawai atasanlangsung2) {
		this.atasanlangsung2 = atasanlangsung2;
	}

	/**
	 * Atasan langsung ketiga (jenjang persetujuan paling atas yang disediakan kelas ini).
	 *
	 * @return atasan langsung ketiga, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "atasanlangsung3", nullable = true)
	public Pegawai getAtasanlangsung3() {
		atasanlangsung3 = check(atasanlangsung3);
		return atasanlangsung3;
	}

	/**
	 * Mengisi atasan langsung ketiga.
	 *
	 * @param atasanlangsung3 pegawai atasan ketiga
	 */
	public void setAtasanlangsung3(Pegawai atasanlangsung3) {
		this.atasanlangsung3 = atasanlangsung3;
	}

	/**
	 * Pemformat tanggal {@code yyyy-MM-dd} yang dipakai seluruh method {@code ambilMasaKerja*} dan
	 * {@code ambil*} penggajian untuk mengubah {@link Date} menjadi {@link java.time.LocalDate}.
	 *
	 * <p><b>Catatan:</b> field ini {@code public static} dan <b>tidak</b> {@code final}, jadi
	 * secara teknis bisa diubah dari mana saja. {@link DateTimeFormatter} sendiri thread-safe
	 * (tidak seperti {@code SimpleDateFormat}), sehingga berbagi satu instance aman selama tidak
	 * ada yang menimpanya.</p>
	 */
	public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	public Integer ambilMasaKerjaTahun() {
		return ambilMasaKerjaTahun(getTanggalmasuk(), getTanggalkeluar());
	}

	public Integer ambilMasaKerjaTahun(Date tgl, Date tglSampai) {
		Integer masaKerjaTahun = 0;
		if (tgl != null) {

			String ActualDate = Common.databaseDateFormat.get().format(tgl);
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = tglSampai == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(tglSampai), formatter);

			Period period = Period.between(dt, currentdate);

			masaKerjaTahun = period.getYears();

		}
		return masaKerjaTahun;
	}

	public Integer ambilMasaKerjaBulan() {
		return ambilMasaKerjaBulan(getTanggalmasuk(), getTanggalkeluar());
	}

	public Integer ambilMasaKerjaBulan(Date tgl, Date tglSampai) {
		Integer masaKerjaBulan = 0;
		if (tgl != null) {

			String ActualDate = Common.databaseDateFormat.get().format(tgl);
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = tglSampai == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(tglSampai), formatter);

			Period period = Period.between(dt, currentdate);

			masaKerjaBulan = period.getMonths();

		}
		return masaKerjaBulan;
	}

	public Integer ambilMasaKerjaTahunPengalamanKerja() {
		return ambilMasaKerjaTahunPengalamanKerja(getTanggalMulaiPengalanKerja(), getTanggalSampaiPengalanKerja());
	}

	public Integer ambilMasaKerjaTahunPengalamanKerja(Date tgl, Date tglSampai) {
		Integer masaKerja = 0;
		if (tgl != null) {

			String ActualDate = Common.databaseDateFormat.get().format(tgl);
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = tglSampai == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(tglSampai), formatter);

			Period period = Period.between(dt, currentdate);

			masaKerja = period.getYears();

		}
		return masaKerja;
	}

	public Integer ambilMasaKerjaBulanPengalamanKerja() {
		return ambilMasaKerjaBulanPengalamanKerja(getTanggalMulaiPengalanKerja(), getTanggalSampaiPengalanKerja());
	}

	public Integer ambilMasaKerjaBulanPengalamanKerja(Date tgl, Date tglSampai) {
		Integer masaKerja = 0;
		if (tgl != null) {

			String ActualDate = Common.databaseDateFormat.get().format(tgl);
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = tglSampai == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(tglSampai), formatter);

			Period period = Period.between(dt, currentdate);

			masaKerja = period.getMonths();

		}
		return masaKerja;
	}

	public Integer ambilMasaKerjaTahunHonorer() {
		return ambilMasaKerjaTahunHonorer(getTanggalmasukHonorer(), getTanggalkeluarHonorer());
	}

	public Integer ambilMasaKerjaTahunHonorer(Date tgl, Date tglSampai) {
		Integer masaKerja = 0;
		if (tgl != null) {

			String ActualDate = Common.databaseDateFormat.get().format(tgl);
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = tglSampai == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(tglSampai), formatter);

			Period period = Period.between(dt, currentdate);

			masaKerja = period.getYears();

		}
		return masaKerja;
	}

	public Integer ambilMasaKerjaBulanHonorer() {
		return ambilMasaKerjaBulanHonorer(getTanggalmasukHonorer(), getTanggalkeluarHonorer());
	}

	public Integer ambilMasaKerjaBulanHonorer(Date tgl, Date tglSampai) {
		Integer masaKerja = 0;
		if (tgl != null) {

			String ActualDate = Common.databaseDateFormat.get().format(tgl);
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = tglSampai == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(tglSampai), formatter);

			Period period = Period.between(dt, currentdate);

			masaKerja = period.getMonths();

		}
		return masaKerja;
	}

	public Integer ambilMasaKerjaTahunSemiTetap() {
		return ambilMasaKerjaTahunSemiTetap(getTanggalmasukSemiTetap(), getTanggalkeluarSemiTetap());
	}

	public Integer ambilMasaKerjaTahunSemiTetap(Date tgl, Date tglSampai) {
		Integer masaKerja = 0;
		if (tgl != null) {

			String ActualDate = Common.databaseDateFormat.get().format(tgl);
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = tglSampai == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(tglSampai), formatter);

			Period period = Period.between(dt, currentdate);

			masaKerja = period.getYears();

		}
		return masaKerja;
	}

	public Integer ambilMasaKerjaBulanSemiTetap() {
		return ambilMasaKerjaBulanSemiTetap(getTanggalmasukSemiTetap(), getTanggalkeluarSemiTetap());
	}

	public Integer ambilMasaKerjaBulanSemiTetap(Date tgl, Date tglSampai) {
		Integer masaKerja = 0;
		if (tgl != null) {

			String ActualDate = Common.databaseDateFormat.get().format(tgl);
			java.time.LocalDate dt = java.time.LocalDate.parse(ActualDate, formatter);
			java.time.LocalDate currentdate = tglSampai == null ? java.time.LocalDate.now()
					: java.time.LocalDate.parse(Common.databaseDateFormat.get().format(tglSampai), formatter);

			Period period = Period.between(dt, currentdate);

			masaKerja = period.getMonths();

		}
		return masaKerja;
	}

	public List<GajiPokok> ambilKeseuaianGajiPokok(Date sekarang) {

		List<GajiPokok> gajiPokoks = new ArrayList<GajiPokok>();
		String s = Common.dateFormat1.get().format(sekarang);
		Integer masaKerjaTahun = ambilMasaKerjaTahun();
		for (Object o : ConstantValues.ambilBerdasarClass(GajiPokok.class).values()) {
			GajiPokok gajiPokok = (GajiPokok) o;
			if (sekarang.after(gajiPokok.getTanggalEfektif())
					|| s.equalsIgnoreCase(Common.dateFormat1.get().format(gajiPokok.getTanggalEfektif()))) {
				if (masaKerjaTahun != null && masaKerjaTahun >= gajiPokok.getMasaKerja()) {
					gajiPokoks.add(gajiPokok);
				}
			}
		}

		Collections.sort(gajiPokoks, Collections.reverseOrder());
		return gajiPokoks;
	}

	public FormatKpiDetail ambilFormatKpiDetail(Date sekarang) {
		List<FormatKpiDetail> formatKpiDetails = new ArrayList<FormatKpiDetail>();
		String s = Common.dateFormat1.get().format(sekarang);
		for (Object o : ConstantValues.ambilBerdasarClass(FormatKpiDetail.class).values()) {
			FormatKpiDetail formatKpiDetail = (FormatKpiDetail) o;
			if (formatKpiDetail.getPegawai().getId().equals(getId()) && formatKpiDetail.getAktif()
					&& (sekarang.after(formatKpiDetail.getTanggalEfektif())
							|| s.equalsIgnoreCase(Common.dateFormat1.get().format(formatKpiDetail.getTanggalEfektif())))) {
				formatKpiDetails.add(formatKpiDetail);
			}
		}

		Collections.sort(formatKpiDetails);

		FormatKpiDetail formatKpiDetail = formatKpiDetails.isEmpty() ? null : formatKpiDetails.get(0);
		formatKpiDetails = null;
		return formatKpiDetail;
	}

	public JabatanStruktural ambilJabatanStruktural(List<KenaikanPangkat> kenaikanPangkats) {
		for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
			if (kenaikanPangkat.getJabatanStruktural() != null) {
				return kenaikanPangkat.getJabatanStruktural();
			}
		}
		return null;
	}

	public JabatanFungsional ambilJabatanFungsional(List<KenaikanPangkat> kenaikanPangkats) {
		for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
			if (kenaikanPangkat.getJabatanFungsional() != null) {
				return kenaikanPangkat.getJabatanFungsional();
			}
		}
		return null;
	}

	public Jabatan ambilJabatan(List<KenaikanPangkat> kenaikanPangkats) {

		for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
			if (kenaikanPangkat.getJabatan() != null) {
				return kenaikanPangkat.getJabatan();
			}
		}
		return null;
	}

	public List<JabatanStruktural> ambilJabatanStrukturals(List<KenaikanPangkat> kenaikanPangkats) {
		List<JabatanStruktural> jabatanStrukturals = new ArrayList<JabatanStruktural>();
		for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
			if (kenaikanPangkat.getJabatanStruktural() != null) {
				jabatanStrukturals.add(kenaikanPangkat.getJabatanStruktural());
			}
		}
		return jabatanStrukturals;
	}

	public List<JabatanFungsional> ambilJabatanFungsionals(List<KenaikanPangkat> kenaikanPangkats) {
		List<JabatanFungsional> fungsionals = new ArrayList<JabatanFungsional>();
		for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
			if (kenaikanPangkat.getJabatanFungsional() != null) {
				fungsionals.add(kenaikanPangkat.getJabatanFungsional());
			}
		}
		return fungsionals;
	}

	public List<Jabatan> ambilJabatans(List<KenaikanPangkat> kenaikanPangkats) {
		List<Jabatan> jabatans = new ArrayList<Jabatan>();
		for (KenaikanPangkat kenaikanPangkat : kenaikanPangkats) {
			if (kenaikanPangkat.getJabatan() != null) {
				jabatans.add(kenaikanPangkat.getJabatan());
			}
		}
		return jabatans;
	}

	@SuppressWarnings("rawtypes")
	public List<KenaikanPangkat> ambilKenaikanPangkatData(Date sekarang) {
		Collection pangkats = ambilKoleksiKenaikanPangkatAman();
		return ambilKenaikanPangkat(sekarang, pangkats);
	}

	@SuppressWarnings("rawtypes")
	public List<KenaikanPangkat> ambilKenaikanPangkat(Date sekarang) {
		Collection pangkats = ambilKoleksiKenaikanPangkatAman();
		return ambilKenaikanPangkat(sekarang, pangkats);
	}

	/**
	 * Mengambil koleksi {@link KenaikanPangkat} dari cache {@code ConstantValues} dengan aman.
	 * Bila cache MapDB sedang/sudah ditutup (mis. saat shutdown/redeploy), pengambilan dapat
	 * melempar {@code IllegalAccessError "DB has been closed"} (Error, bukan Exception). Karena
	 * method ini dipakai dari getter yang dipanggil Hibernate saat flush, kegagalan tidak boleh
	 * menggagalkan flush → kembalikan koleksi kosong.
	 */
	@SuppressWarnings("rawtypes")
	private Collection ambilKoleksiKenaikanPangkatAman() {
		try {
			java.util.Map map = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class);
			return map == null ? new ArrayList() : map.values();
		} catch (Throwable t) {
			return new ArrayList();
		}
	}

	@SuppressWarnings("rawtypes")
	public List<KenaikanPangkat> ambilKenaikanPangkat(Date sekarang, Collection pangkats) {
		List<KenaikanPangkat> kenaikanPangkats = new ArrayList<KenaikanPangkat>();
		if (pangkats == null || sekarang == null) {
			return kenaikanPangkats;
		}
		String s = Common.dateFormat1.get().format(sekarang);
		// CATATAN: 'pangkats' bisa berupa koleksi yang didukung MapDB (cache ConstantValues). Saat
		// shutdown/redeploy cache bisa SUDAH ditutup sementara thread latar masih jalan -> iterator
		// melempar IllegalAccessError "DB has been closed" (sebuah Error, BUKAN Exception, sehingga
		// catch(Exception) per-item tidak menangkapnya). Getter ini dipanggil Hibernate saat snapshot
		// properti (flush); jika melempar, flush gagal & data tak tersimpan. Maka SELURUH iterasi
		// dibungkus catch(Throwable): bila cache tertutup, kembalikan apa yang sudah terkumpul.
		try {
			for (Object o : pangkats) {
				try {
					KenaikanPangkat kenaikanPangkat = (KenaikanPangkat) o;
					if (kenaikanPangkat.getPegawai().getId().equals(getId()) && kenaikanPangkat.getMenjabat()
							&& kenaikanPangkat.getStatus() && (sekarang.after(kenaikanPangkat.getMulai())
									|| s.equalsIgnoreCase(Common.dateFormat1.get().format(kenaikanPangkat.getMulai())))) {
						kenaikanPangkats.add(kenaikanPangkat);
					} else if (kenaikanPangkat.getPegawai().getId().equals(getId()) && kenaikanPangkat.getMulai() != null
							&& kenaikanPangkat.getSampai() != null
							&& (sekarang.after(kenaikanPangkat.getMulai())
									|| s.equalsIgnoreCase(Common.dateFormat1.get().format(kenaikanPangkat.getMulai())))
							&& (sekarang.before(kenaikanPangkat.getSampai())
									|| s.equalsIgnoreCase(Common.dateFormat1.get().format(kenaikanPangkat.getSampai())))) {
						kenaikanPangkats.add(kenaikanPangkat);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:2425");
					// TODO: handle exception
				}
			}
		} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:2429");
			// Cache MapDB tertutup / tidak bisa di-iterasi -> jangan gagalkan flush; pakai hasil parsial.
		}
		try {
			Collections.sort(kenaikanPangkats);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:2434");
		}
		return kenaikanPangkats;
	}

	public List<FormatItemGaji> ambilFormatItemGajis() {
		List<FormatItemGaji> formatItemGajis = new ArrayList<FormatItemGaji>();
		if (getFormatItemGaji() != null) {
			formatItemGajis.add(formatItemGaji);
		}
		if (getFormatItemGaji2() != null) {
			formatItemGajis.add(formatItemGaji2);
		}
		if (getFormatItemGaji3() != null) {
			formatItemGajis.add(formatItemGaji3);
		}
		if (getFormatItemGaji4() != null) {
			formatItemGajis.add(formatItemGaji4);
		}
		if (getFormatItemGaji5() != null) {
			formatItemGajis.add(formatItemGaji5);
		}
		return formatItemGajis;
	}

	public List<Long> ambilFormatItemGajisId() {
		List<Long> formatItemGajis = new ArrayList<Long>();
		if (getFormatItemGaji() != null) {
			formatItemGajis.add(formatItemGaji.getId());
		}
		if (getFormatItemGaji2() != null) {
			formatItemGajis.add(formatItemGaji2.getId());
		}
		if (getFormatItemGaji3() != null) {
			formatItemGajis.add(formatItemGaji3.getId());
		}
		if (getFormatItemGaji4() != null) {
			formatItemGajis.add(formatItemGaji4.getId());
		}
		if (getFormatItemGaji5() != null) {
			formatItemGajis.add(formatItemGaji5.getId());
		}
		return formatItemGajis;
	}

	public Bank ambilBank(FormatItemGaji formatItemGaji) {
		if (formatItemGaji == null || formatItemGaji.getId() == null) {
			return getBank();
		}

		int index = 1;
		for (Long idF : ambilFormatItemGajisId()) {
			if (formatItemGaji.getId().equals(idF)) {
				break;
			} else {
				index++;
			}
		}

		System.out.println("formatItemGaji " + formatItemGaji + ", index " + index);

		if (index == 1) {
			return getBank();
		} else if (index == 2) {
			return getBank2();
		} else if (index == 3) {
			return getBank3();
		} else if (index == 4) {
			return getBank4();
		} else if (index == 5) {
			return getBank5();
		} else {
			return getBank();
		}
	}

	public String ambilNoRek(FormatItemGaji formatItemGaji) {
		if (formatItemGaji == null || formatItemGaji.getId() == null) {
			return getNorek();
		}

		int index = 1;
		for (Long idF : ambilFormatItemGajisId()) {
			if (formatItemGaji.getId().equals(idF)) {
				break;
			} else {
				index++;
			}
		}

		if (index == 1) {
			return getNorek();
		} else if (index == 2) {
			return getNorek2();
		} else if (index == 3) {
			return getNorek3();
		} else if (index == 4) {
			return getNorek4();
		} else if (index == 5) {
			return getNorek5();
		} else {
			return getNorek();
		}
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_item_gaji", nullable = true)
	public FormatItemGaji getFormatItemGaji() {
		formatItemGaji = check(formatItemGaji);
		return formatItemGaji;
	}

	public void setFormatItemGaji(FormatItemGaji formatItemGaji) {
		this.formatItemGaji = formatItemGaji;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_item_gaji2", nullable = true)
	public FormatItemGaji getFormatItemGaji2() {
		formatItemGaji2 = check(formatItemGaji2);
		return formatItemGaji2;
	}

	public void setFormatItemGaji2(FormatItemGaji formatItemGaji2) {
		this.formatItemGaji2 = formatItemGaji2;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_item_gaji3", nullable = true)
	public FormatItemGaji getFormatItemGaji3() {
		formatItemGaji3 = check(formatItemGaji3);
		return formatItemGaji3;
	}

	public void setFormatItemGaji3(FormatItemGaji formatItemGaji3) {
		this.formatItemGaji3 = formatItemGaji3;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_fungsional", nullable = true)
	public JabatanFungsional getJabatanFungsional() {
		List<KenaikanPangkat> kenaikanPangkats = ambilKenaikanPangkat(WaktuUtil.kemarin());
		jabatanFungsional = ambilJabatanFungsional(kenaikanPangkats);
		kenaikanPangkats = null;
		return jabatanFungsional;
	}

	public void setJabatanFungsional(JabatanFungsional jabatanFungsional) {
		this.jabatanFungsional = jabatanFungsional;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_struktural", nullable = true)
	public JabatanStruktural getJabatanStruktural() {
		List<KenaikanPangkat> kenaikanPangkats = ambilKenaikanPangkat(WaktuUtil.kemarin());
		jabatanStruktural = ambilJabatanStruktural(kenaikanPangkats);
		kenaikanPangkats = null;
		return jabatanStruktural;
	}

	public void setJabatanStruktural(JabatanStruktural jabatanStruktural) {
		this.jabatanStruktural = jabatanStruktural;
	}

	public Long getCalonPegawai() {
		return calonPegawai;
	}

	public void setCalonPegawai(Long calonPegawai) {
		this.calonPegawai = calonPegawai;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalMulaiPengalanKerja() {
		return tanggalMulaiPengalanKerja;
	}

	public void setTanggalMulaiPengalanKerja(Date tanggalMulaiPengalanKerja) {
		this.tanggalMulaiPengalanKerja = tanggalMulaiPengalanKerja;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalSampaiPengalanKerja() {
		return tanggalSampaiPengalanKerja;
	}

	public void setTanggalSampaiPengalanKerja(Date tanggalSampaiPengalanKerja) {
		this.tanggalSampaiPengalanKerja = tanggalSampaiPengalanKerja;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalmasukSemiTetap() {
		if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null && TipeMasaKerja.Semi_Tetap != null
				&& (tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Semi_Tetap))) {
			return tanggalmasukSemiTetap;
		} else if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null && TipeMasaKerja.Semi_Tetap != null
				&& !(tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Semi_Tetap))) {
			return null;
		} else {
			return tanggalmasukSemiTetap;
		}
	}

	public void setTanggalmasukSemiTetap(Date tanggalmasukSemiTetap) {
		this.tanggalmasukSemiTetap = tanggalmasukSemiTetap;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalkeluarSemiTetap() {
		if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null && TipeMasaKerja.Semi_Tetap != null
				&& (tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Semi_Tetap))) {
			return tanggalkeluarSemiTetap;
		} else if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null && TipeMasaKerja.Semi_Tetap != null
				&& !(tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Semi_Tetap))) {
			return null;
		} else {
			return tanggalkeluarSemiTetap;
		}
	}

	public void setTanggalkeluarSemiTetap(Date tanggalkeluarSemiTetap) {
		this.tanggalkeluarSemiTetap = tanggalkeluarSemiTetap;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalmasukHonorer() {
		if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null && TipeMasaKerja.Honorer != null
				&& TipeMasaKerja.Semi_Tetap != null
				&& (tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Honorer)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Semi_Tetap))) {
			return tanggalmasukHonorer;
		} else if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null && TipeMasaKerja.Honorer != null
				&& TipeMasaKerja.Semi_Tetap != null
				&& !(tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Honorer)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Semi_Tetap))) {
			return null;
		} else {
			return tanggalmasukHonorer;
		}
	}

	public void setTanggalmasukHonorer(Date tanggalmasukHonorer) {
		this.tanggalmasukHonorer = tanggalmasukHonorer;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalkeluarHonorer() {
		if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null && TipeMasaKerja.Honorer != null
				&& TipeMasaKerja.Semi_Tetap != null
				&& (tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Honorer)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Semi_Tetap))) {
			return tanggalkeluarHonorer;
		} else if (getTipeMasaKerja() != null && TipeMasaKerja.Tetap != null && TipeMasaKerja.Honorer != null
				&& TipeMasaKerja.Semi_Tetap != null
				&& !(tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Tetap)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Honorer)
						|| tipeMasaKerjaCocok(getTipeMasaKerja(), TipeMasaKerja.Semi_Tetap))) {
			return null;
		} else {
			return tanggalkeluarHonorer;
		}
	}

	public void setTanggalkeluarHonorer(Date tanggalkeluarHonorer) {
		this.tanggalkeluarHonorer = tanggalkeluarHonorer;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_pegawai")
	public TipePegawai getTipePegawai() {
		if (getDosen() != null && TipePegawai.DOSEN != null) {
			tipePegawai = TipePegawai.DOSEN;
		} else if (getGuru() != null && TipePegawai.GURU != null) {
			tipePegawai = TipePegawai.GURU;
		} else if (tipePegawai == null && TipePegawai.STAF != null) {
			tipePegawai = TipePegawai.STAF;
		} else {
			tipePegawai = check(tipePegawai);
		}
		return tipePegawai;
	}

	public void setTipePegawai(TipePegawai tipePegawai) {
		this.tipePegawai = tipePegawai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_masa_kerja")
	public TipeMasaKerja getTipeMasaKerja() {
		if (tipeMasaKerja == null) {
			tipeMasaKerja = TipeMasaKerja.Honorer;
		} else {
			tipeMasaKerja = check(tipeMasaKerja);
		}
		return tipeMasaKerja;
	}

	public void setTipeMasaKerja(TipeMasaKerja tipeMasaKerja) {
		this.tipeMasaKerja = tipeMasaKerja;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "masa_kerja")
	public MasaKerja getMasaKerja() {
		masaKerja = check(masaKerja);
		return masaKerja;
	}

	public void setMasaKerja(MasaKerja masaKerja) {
		this.masaKerja = masaKerja;
	}

	public String getLintang() {
		guru = getGuru();
		if (guru != null) {
			lintang = guru.getLintang();
		}
		return lintang;
	}

	public void setLintang(String lintang) {
		this.lintang = lintang;
	}

	public String getBujur() {
		if (guru != null) {
			bujur = guru.getBujur();
		}
		return bujur;
	}

	public void setBujur(String bujur) {
		this.bujur = bujur;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ptkp_pegawai")
	public PtkpPegawai getPtkpPegawai() {
		ptkpPegawai = check(ptkpPegawai);
		return ptkpPegawai;
	}

	public void setPtkpPegawai(PtkpPegawai ptkpPegawai) {
		this.ptkpPegawai = ptkpPegawai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi_pegawai1")
	public AsuransiPegawai getAsuransiPegawai1() {
		asuransiPegawai1 = check(asuransiPegawai1);
		return asuransiPegawai1;
	}

	public void setAsuransiPegawai1(AsuransiPegawai asuransiPegawai1) {
		this.asuransiPegawai1 = asuransiPegawai1;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi_pegawai2")
	public AsuransiPegawai getAsuransiPegawai2() {
		asuransiPegawai2 = check(asuransiPegawai2);
		return asuransiPegawai2;
	}

	public void setAsuransiPegawai2(AsuransiPegawai asuransiPegawai2) {
		this.asuransiPegawai2 = asuransiPegawai2;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi_pegawai3")
	public AsuransiPegawai getAsuransiPegawai3() {
		asuransiPegawai3 = check(asuransiPegawai3);
		return asuransiPegawai3;
	}

	public void setAsuransiPegawai3(AsuransiPegawai asuransiPegawai3) {
		this.asuransiPegawai3 = asuransiPegawai3;
	}

	public String getTelpDarurat() {
		return telpDarurat;
	}

	public void setTelpDarurat(String telpDarurat) {
		this.telpDarurat = telpDarurat;
	}

	public Double getPersenKpiDefault() {
		return persenKpiDefault == null ? 0.0 : persenKpiDefault;
	}

	public void setPersenKpiDefault(Double persenKpiDefault) {
		this.persenKpiDefault = persenKpiDefault;
	}

	public Double getNilaiGaji() {
		return nilaiGaji == null ? 0.0 : nilaiGaji;
	}

	public void setNilaiGaji(Double nilaiGaji) {
		this.nilaiGaji = nilaiGaji;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi_pegawai4")
	public AsuransiPegawai getAsuransiPegawai4() {
		asuransiPegawai4 = check(asuransiPegawai4);
		return asuransiPegawai4;
	}

	public void setAsuransiPegawai4(AsuransiPegawai asuransiPegawai4) {
		this.asuransiPegawai4 = asuransiPegawai4;
	}

	public String getNamaDarurat() {
		return namaDarurat;
	}

	public void setNamaDarurat(String namaDarurat) {
		this.namaDarurat = namaDarurat;
	}

	public String getStatusDarurat() {
		return statusDarurat;
	}

	public void setStatusDarurat(String statusDarurat) {
		this.statusDarurat = statusDarurat;
	}

	/**
	 * Golongan darah pegawai (mis. {@code "A"}, {@code "B"}, {@code "O"}, {@code "AB"}, dengan/ tanpa
	 * rhesus). Kolom {@code golongan_darah} aditif &amp; nullable (dibuat otomatis oleh
	 * {@code hbm2ddl.auto=update}). {@code @NotAudited} — seperti {@code getBahasa()} — agar Envers tidak
	 * mengauditnya sehingga TIDAK perlu menyelaraskan tabel audit {@code new_audit.pegawai__audit}.
	 */
	@org.hibernate.envers.NotAudited
	@javax.persistence.Column(name = "golongan_darah", length = 14)
	public String getGolonganDarah() {
		return golonganDarah;
	}

	public void setGolonganDarah(String golonganDarah) {
		this.golonganDarah = golonganDarah;
	}

	/**
	 * Nomor Kartu Keluarga (KK) pegawai. Kolom {@code nomor_kartu_keluarga} aditif &amp; nullable
	 * (hbm2ddl.auto=update). {@code @NotAudited} agar tak perlu sinkron tabel audit.
	 */
	@org.hibernate.envers.NotAudited
	@javax.persistence.Column(name = "nomor_kartu_keluarga", length = 30)
	public String getNomorKartuKeluarga() {
		return nomorKartuKeluarga;
	}

	public void setNomorKartuKeluarga(String nomorKartuKeluarga) {
		this.nomorKartuKeluarga = nomorKartuKeluarga;
	}

	/**
	 * Nama ibu kandung pegawai. Kolom {@code nama_ibu_kandung} aditif &amp; nullable
	 * (hbm2ddl.auto=update). {@code @NotAudited} agar tak perlu sinkron tabel audit.
	 */
	@org.hibernate.envers.NotAudited
	@javax.persistence.Column(name = "nama_ibu_kandung", length = 255)
	public String getNamaIbuKandung() {
		return namaIbuKandung;
	}

	public void setNamaIbuKandung(String namaIbuKandung) {
		this.namaIbuKandung = namaIbuKandung;
	}

	public Double getJpDefault() {
		return jpDefault == null ? 0.0 : jpDefault;
	}

	public void setJpDefault(Double jpDefault) {
		this.jpDefault = jpDefault;
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:2906");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:2912");

			}

			// System.out.println("namaCol=> " + namaCol + ", lbl=> " + lbl + ", val=> " +
			// val + ", url=>" + url);

			String[] param = lbl.split("->");

			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setName5(param[0]);
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
				KelompokParameterTambahanGajiPegawai kelompokParameterTambahanGajiPegawai = (KelompokParameterTambahanGajiPegawai) row
						.getAttribute("kelompokParameterTambahanGajiPegawai");
				if (parameterTambahan != null && kelompokParameterTambahanGajiPegawai != null) {
					String jenis = kelompokParameterTambahanGajiPegawai.getId() + "->" + parameterTambahan.getId();

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);
					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);
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

					String s = kelompokParameterTambahanGajiPegawai.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanGajiPegawai.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanGajiPegawai.getId() + "->" + parameterTambahan.getId()
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_gaji_pegawai", nullable = true)
	public JenisGajiPegawai getJenisGajiPegawai() {
		jenisGajiPegawai = check(jenisGajiPegawai);
		return jenisGajiPegawai;
	}

	public void setJenisGajiPegawai(JenisGajiPegawai jenisGajiPegawai) {
		this.jenisGajiPegawai = jenisGajiPegawai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ikatan_kerja_dosen")
	public IkatanKerjaDosen getIkatanKerjaDosen() {

		try {
			Dosen dosenData = getDosen();
			if (dosenData != null && dosenData.getIkatanKerjaDosen() != null) {
				ikatanKerjaDosen = dosenData.getIkatanKerjaDosen();
			} else {
				ikatanKerjaDosen = check(ikatanKerjaDosen);
				if (ikatanKerjaDosen == null) {
					/*
					 * Hindari getTetap() karena getter tersebut kembali me-resolve getDosen().
					 * Pada proxy/data legacy hal itu dapat memicu NPE berulang. Kolom scalar
					 * tetap sudah mempunyai default 0 dan aman dibandingkan langsung.
					 */
					if (Integer.valueOf(1).equals(tetap)) {
						ikatanKerjaDosen = ConstantValues.DOSEN_TETAP;
					} else {
						ikatanKerjaDosen = ConstantValues.DOSEN_HONORER;
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:3030");
			// TODO: handle exception
		}
		return ikatanKerjaDosen;
	}

	public void setIkatanKerjaDosen(IkatanKerjaDosen ikatanKerjaDosen) {
		this.ikatanKerjaDosen = ikatanKerjaDosen;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tendik_fakultas", nullable = true)
	public Fakultas getTendikFakultas() {
		try {
			if (dosen != null && dosen.getFakultas() != null) {
				tendikFakultas = dosen.getFakultas();
			} else if (dosen == null) {
				tendikFakultas = check(tendikFakultas);
			}
		} catch (org.hibernate.HibernateException detachedRelation) {
			// Laporan dapat membaca Pegawai setelah sesi asal ditutup. Pertahankan nilai
			// tendik yang sudah tersedia tanpa memaksa inisialisasi proxy Dosen.
		}

		return tendikFakultas;
	}

	public void setTendikFakultas(Fakultas tendikFakultas) {
		this.tendikFakultas = tendikFakultas;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tendik_jurusan", nullable = true)
	public Jurusan getTendikJurusan() {
		try {
			if (dosen != null && dosen.getJurusan() != null) {
				tendikJurusan = dosen.getJurusan();
			} else if (dosen == null) {
				tendikJurusan = check(tendikJurusan);
			}
		} catch (org.hibernate.HibernateException detachedRelation) {
			// Jangan membuka kembali relasi lazy dari koneksi laporan yang sudah ditutup.
		}

		return tendikJurusan;
	}

	public void setTendikJurusan(Jurusan tendikJurusan) {
		this.tendikJurusan = tendikJurusan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tendik_sekolah", nullable = true)
	public Sekolah getTendikSekolah() {
		try {
			if (guru != null && guru.getSekolah() != null) {
				tendikSekolah = guru.getSekolah();
			} else if (guru == null) {
				tendikSekolah = check(tendikSekolah);
			}
		} catch (org.hibernate.HibernateException detachedRelation) {
			// Jangan membuka kembali relasi lazy dari koneksi laporan yang sudah ditutup.
		}

		return tendikSekolah;
	}

	public void setTendikSekolah(Sekolah tendikSekolah) {
		this.tendikSekolah = tendikSekolah;
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

	public String ambilNoHp() {
		String hp = getHp();
		String telp = getTelp();
		if (hp == null || hp.toString().trim().isEmpty() || hp.toString().trim().equals("08100000000000000000")
				|| hp.toString().trim().equals("0000000000")) {
			hp = telp;
		}

		if (hp != null && !hp.trim().isEmpty() && !(hp == null || hp.toString().trim().isEmpty()
				|| hp.toString().trim().equals("00000000000000000000") || hp.toString().trim().equals("000000000"))) {
			hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
			hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
			hp = !hp.startsWith("+") ? "+62" + hp : hp;
		}
		return hp;
	}

	public void tampilkanHp(Component vbox, String pesan) throws Exception {
		try {

			String hp = getHp();
			String telp = getTelp();

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
				a.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text="
						+ URLEncoder.encode(pesan.replaceAll("<br>", "\n"), "UTF-8"));
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
				a.setHref("https://web.whatsapp.com/send?phone=" + hp + "&text="
						+ URLEncoder.encode(pesan.replaceAll("<br>", "\n"), "UTF-8"));
			}
		}
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "atasan", nullable = true)
	public JenisJabatan getAtasan() {
		atasan = check(atasan);
		return atasan;
	}

	public void setAtasan(JenisJabatan atasan) {
		this.atasan = atasan;
	}

	public String ttdQr() {

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/ttd_peg_" + getId() + ".png");
		if (!myfilebarcode.exists()) {
			SatuanKerja satuanKerja = getSatuanKerja();
			String code = (getCode() == null || getCode().trim().isEmpty() ? "" : getCode() + "\n")
					+ (getNipLama() == null || getNipLama().trim().isEmpty() ? "" : getNipLama() + "\n")
					+ (getMycode() == null || getMycode().trim().isEmpty() ? "" : getMycode() + "\n")
					+ (getDosen() == null || getDosen().getNidn() == null || getDosen().getNidn().trim().isEmpty() ? ""
							: getDosen().getNidn() + "\n")
					+ (getGuru() == null || getGuru().getNuptk() == null || getGuru().getNuptk().trim().isEmpty() ? ""
							: getGuru().getNuptk() + "\n")

					+ getNama() + "\n" + (satuanKerja == null ? "" : satuanKerja.getNama() + "\n")
					+ (getTendikSekolah() == null
							? (getTendikFakultas() == null || getTendikFakultas().getPerguruanTinggi() == null ? ""
									: getTendikFakultas().getPerguruanTinggi().getNama() + "\n")
							: getTendikSekolah().getNama() + "\n")
					+ (getTendikFakultas() == null ? "" : getTendikFakultas().getNama() + "\n")
					+ (getTendikJurusan() == null ? "" : getTendikJurusan().getNama() + "\n")

					+ Common.dateFormat1.get().format(WaktuUtil.getDate()) + "\n" + Common.getRequestHostWithProtocol();
			BarcodeCommon.generateCRCode(code, myfilebarcode);
		}
		return myfilebarcode.getAbsolutePath();
	}

	@Temporal(TemporalType.DATE)
	public Date getAwalmasuk() {
		// BUG FIX (NPE): getTanggalMulaiPengalanKerja/getTanggalmasukHonorer/
		// getTanggalmasukSemiTetap/getTanggalmasuk sekarang null-safe (lihat
		// tipeMasaKerjaCocok di atas), tapi dibungkus try-catch juga di sini
		// sebagai jaring pengaman terakhir supaya data Pegawai lama/tak lengkap
		// tidak meng-crash pemanggil reflection (ManajemenProperty.
		// processNestedJsonProperties / ElearningApiUtil.dataRinci).
		try {
			awalmasuk = getTanggalMulaiPengalanKerja();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pegawai.java:getAwalmasuk");
			awalmasuk = null;
		}
		if (awalmasuk == null || (getTanggalmasukHonorer() != null && awalmasuk.after(getTanggalmasukHonorer()))) {
			awalmasuk = getTanggalmasukHonorer();
		}
		if (awalmasuk == null || (getTanggalmasukSemiTetap() != null && awalmasuk.after(getTanggalmasukSemiTetap()))) {
			awalmasuk = getTanggalmasukSemiTetap();
		}
		if (awalmasuk == null || (getTanggalmasuk() != null && awalmasuk.after(getTanggalmasuk()))) {
			awalmasuk = getTanggalmasuk();
		}

		return awalmasuk;
	}

	public void setAwalmasuk(Date awalmasuk) {
		this.awalmasuk = awalmasuk;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putPhoto(Map parameters) {
		try {
			Pegawai pegawai = this;

			FileFotoLain fotobiodataPegawai = FileFotoLain.ambil(pegawai.getId(), FotoPegawai.DEFAULT_JENIS,
					FotoPegawai.class);

			if (fotobiodataPegawai != null && fotobiodataPegawai.ambilFile() != null) {
				parameters.put("foto", fotobiodataPegawai.ambilFile().getAbsolutePath());
				parameters.put("foto_pegawai", fotobiodataPegawai.ambilFile().getAbsolutePath());
			} else

			if (fotobiodataPegawai != null && fotobiodataPegawai.dropboxLinkRaw() != null
					&& !fotobiodataPegawai.dropboxLinkRaw().trim().isEmpty()) {
				parameters.put("foto", fotobiodataPegawai.dropboxLinkRaw());
				parameters.put("foto_pegawai", fotobiodataPegawai.dropboxLinkRaw());
			} else if (fotobiodataPegawai != null && fotobiodataPegawai.getGdrive() != null
					&& !fotobiodataPegawai.getGdrive().trim().isEmpty()) {
				parameters.put("foto", fotobiodataPegawai.exportGDriveUrl());
				parameters.put("foto_pegawai", fotobiodataPegawai.exportGDriveUrl());
			} else if (fotobiodataPegawai != null) {
				parameters.put("foto", fotobiodataPegawai.createLinkUri());
				parameters.put("foto_pegawai", fotobiodataPegawai.createLinkUri());
			} else {
				File file = new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
				parameters.put("foto", file.getAbsolutePath());
				parameters.put("foto_pegawai", file.getAbsolutePath());
			}

			if (pegawai.getDosen() != null) {
				pegawai.getDosen().putPhoto(parameters);
			} else if (pegawai.getGuru() != null) {
				pegawai.getGuru().putPhoto(parameters);
			}

			LampiranLain lampiranLain = LampiranLain.ambil(pegawai.getId(), LampiranLain.TTD_PEGAWAI);
			if (lampiranLain != null && lampiranLain.ambilFile() != null) {
				parameters.put("ttd_pegawai", lampiranLain.ambilFile().getAbsolutePath());
			}
			parameters.put("ttd_pegawai_qrcode", pegawai.ttdQr());

			for (String s : pegawai.getKarpeg().split(",")) {
				try {
					if (!s.trim().isEmpty()) {
						LampiranLain lain = LampiranLain.ambil(true, Long.parseLong(s), s);
						if (lain != null) {
							parameters.put("dokumen_pegawai_" + lain.getJenis(), lain.ambilFile().getAbsolutePath());
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pegawai.java:3290");
//					e.printStackTrace();
				}
			}

		} catch (Exception e1) {
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/database/model/Pegawai.java:3296");
		}
	}

	public List<Tbmrole> ambilHakAkses() {
		List<Tbmrole> tbmroles = new ArrayList<Tbmrole>();

		if (this.getId() != null) {
			try {
				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser.ambilPegawai() != null && tbmuser.ambilPegawai().getId().equals(this.getId())
						&& !tbmuser.hakAkses().getMelihatDataSatkerLain()) {
					tbmroles.add(tbmuser.hakAkses());
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pegawai.java:3311");
			}
			if (tbmroles.isEmpty()) {
				Session session = HibernateUtil.currentNativeSession();
				try {
					List<Tbmuser> tbmusers = ConstantValues
							.simpleList(
									session.createCriteria(Tbmuser.class)
											.add(Restrictions.eq("pegawai.id", this.getId())).add(Restrictions
													.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
									Tbmuser.class);
					for (Tbmuser tbmuser : tbmusers) {
						tbmroles.addAll(tbmuser.ambilRoles());
					}
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pegawai.java:3331");
				}
				HibernateUtil.closeSession();
			}
		}
		return tbmroles;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "orang_tua", nullable = true)
	public OrangTua getOrangTua() {
		orangTua = check(orangTua);
		return orangTua;
	}

	public void setOrangTua(OrangTua orangTua) {
		this.orangTua = orangTua;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bagian", unique = true, nullable = true)
	public Bagian getBagian() {
		bagian = check(bagian);
		return bagian;
	}

	public void setBagian(Bagian bagian) {
		this.bagian = bagian;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank2", nullable = true)
	public Bank getBank2() {
		bank2 = check(bank2);
		return bank2;
	}

	public void setBank2(Bank bank2) {
		this.bank2 = bank2;
	}

	public String getNorek2() {
		return norek2;
	}

	public void setNorek2(String norek2) {
		this.norek2 = norek2;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank3", nullable = true)
	public Bank getBank3() {
		bank3 = check(bank3);
		return bank3;
	}

	public void setBank3(Bank bank3) {
		this.bank3 = bank3;
	}

	public String getNorek3() {
		return norek3;
	}

	public void setNorek3(String norek3) {
		this.norek3 = norek3;
	}

	public String getDitransferAtasNama2() {
		return getBank2() == null ? null
				: ditransferAtasNama2 == null || ditransferAtasNama2.trim().isEmpty() ? nama : ditransferAtasNama2;
	}

	public void setDitransferAtasNama2(String ditransferAtasNama2) {
		this.ditransferAtasNama2 = ditransferAtasNama2;
	}

	public String getDitransferAtasNama3() {
		return getBank3() == null ? null
				: ditransferAtasNama3 == null || ditransferAtasNama3.trim().isEmpty() ? nama : ditransferAtasNama3;
	}

	public void setDitransferAtasNama3(String ditransferAtasNama3) {
		this.ditransferAtasNama3 = ditransferAtasNama3;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_item_gaji_4", nullable = true)
	public FormatItemGaji getFormatItemGaji4() {
		formatItemGaji4 = check(formatItemGaji4);
		return formatItemGaji4;
	}

	public void setFormatItemGaji4(FormatItemGaji formatItemGaji4) {
		this.formatItemGaji4 = formatItemGaji4;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_item_gaji_5", nullable = true)
	public FormatItemGaji getFormatItemGaji5() {
		formatItemGaji5 = check(formatItemGaji5);
		return formatItemGaji5;
	}

	public void setFormatItemGaji5(FormatItemGaji formatItemGaji5) {
		this.formatItemGaji5 = formatItemGaji5;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank4", nullable = true)
	public Bank getBank4() {
		bank4 = check(bank4);
		return bank4;
	}

	public void setBank4(Bank bank4) {
		this.bank4 = bank4;
	}

	public String getNorek4() {
		return norek4;
	}

	public void setNorek4(String norek4) {
		this.norek4 = norek4;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank5", nullable = true)
	public Bank getBank5() {
		bank5 = check(bank5);
		return bank5;
	}

	public void setBank5(Bank bank5) {
		this.bank5 = bank5;
	}

	public String getNorek5() {
		return norek5;
	}

	public void setNorek5(String norek5) {
		this.norek5 = norek5;
	}

	public String getDitransferAtasNama4() {
		return getBank4() == null ? null
				: ditransferAtasNama4 == null || ditransferAtasNama4.trim().isEmpty() ? nama : ditransferAtasNama4;
	}

	public void setDitransferAtasNama4(String ditransferAtasNama4) {
		this.ditransferAtasNama4 = ditransferAtasNama4;
	}

	public String getDitransferAtasNama5() {
		return getBank5() == null ? null
				: ditransferAtasNama5 == null || ditransferAtasNama5.trim().isEmpty() ? nama : ditransferAtasNama5;
	}

	public void setDitransferAtasNama5(String ditransferAtasNama5) {
		this.ditransferAtasNama5 = ditransferAtasNama5;
	}

	public String getCaraPembayaran4() {
		return caraPembayaran4 == null || caraPembayaran4.trim().isEmpty() ? Pegawai.CARA_BAYAR_LAINNYA
				: caraPembayaran4;
	}

	public void setCaraPembayaran4(String caraPembayaran4) {
		this.caraPembayaran4 = caraPembayaran4;
	}

	public String getCaraPembayaran5() {
		return caraPembayaran5 == null || caraPembayaran5.trim().isEmpty() ? Pegawai.CARA_BAYAR_LAINNYA
				: caraPembayaran5;
	}

	public void setCaraPembayaran5(String caraPembayaran5) {
		this.caraPembayaran5 = caraPembayaran5;
	}

	public String getNomorAsuransiPegawai1() {
		return nomorAsuransiPegawai1;
	}

	public void setNomorAsuransiPegawai1(String nomorAsuransiPegawai1) {
		this.nomorAsuransiPegawai1 = nomorAsuransiPegawai1;
	}

	public String getNomorAsuransiPegawai2() {
		return nomorAsuransiPegawai2;
	}

	public void setNomorAsuransiPegawai2(String nomorAsuransiPegawai2) {
		this.nomorAsuransiPegawai2 = nomorAsuransiPegawai2;
	}

	public String getNomorAsuransiPegawai3() {
		return nomorAsuransiPegawai3;
	}

	public void setNomorAsuransiPegawai3(String nomorAsuransiPegawai3) {
		this.nomorAsuransiPegawai3 = nomorAsuransiPegawai3;
	}

	public String getNomorAsuransiPegawai4() {
		return nomorAsuransiPegawai4;
	}

	public void setNomorAsuransiPegawai4(String nomorAsuransiPegawai4) {
		this.nomorAsuransiPegawai4 = nomorAsuransiPegawai4;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "atasan_pendukung", nullable = true)
	public JenisJabatan getAtasanPendukung() {
		atasanPendukung = check(atasanPendukung);
		return atasanPendukung;
	}

	public void setAtasanPendukung(JenisJabatan atasanPendukung) {
		this.atasanPendukung = atasanPendukung;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "atasan_pendukung_cadangan", nullable = true)
	public JenisJabatan getAtasanPendukungCadangan() {
		atasanPendukungCadangan = check(atasanPendukungCadangan);
		return atasanPendukungCadangan;
	}

	public void setAtasanPendukungCadangan(JenisJabatan atasanPendukungCadangan) {
		this.atasanPendukungCadangan = atasanPendukungCadangan;
	}

	@Column(columnDefinition = "text")
	public String getKedinasan() {
		return kedinasan;
	}

	public void setKedinasan(String kedinasan) {
		this.kedinasan = kedinasan;
	}

	@Column(columnDefinition = "text")
	public String getPenghargaan() {
		return penghargaan;
	}

	public void setPenghargaan(String penghargaan) {
		this.penghargaan = penghargaan;
	}

	@Column(columnDefinition = "text")
	public String getSangsi() {
		return sangsi;
	}

	public void setSangsi(String sangsi) {
		this.sangsi = sangsi;
	}
}
