package ais.database.model;

// Generated Dec 16, 2009 9:07:17 AM by Hibernate Tools 3.2.4.CR1

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

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
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Box;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.MatakuliahPrasyaratAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.RequestContext;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.recruitment.JadwalUjianPegawai;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JadwalPertemuanPSB;
import ais.database.model.sekolah.JadwalUjianPSB;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.WaktuUtil;

/**
 * Satu <b>sesi/pertemuan</b> yang terjadwal pada satu tanggal, dengan jam mulai-selesai, ruang,
 * topik/materi, daftar kehadiran, lampiran, tugas, dan diskusinya sendiri.
 *
 * <h3>Yang PALING sering disalahpahami: Pertemuan bukan hanya milik Perkuliahan</h3>
 * <p>Walaupun pemakaian terbanyak memang "satu tatap muka dari satu {@link Perkuliahan}", tabel
 * {@code public.pertemuan} sebenarnya dipakai bersama oleh <b>enam belas</b> jenis induk yang
 * berbeda. Tepat SATU di antara kolom-kolom relasi berikut yang terisi pada satu baris; sisanya
 * {@code null}:</p>
 * <ul>
 *   <li>{@link Perkuliahan} — tatap muka kuliah (kasus terbanyak);</li>
 *   <li>{@link ais.database.model.sekolah.JadwalPelajaran} — tatap muka sekolah (guru/siswa);</li>
 *   <li>{@link ais.database.model.sekolah.KelasLesSiswa} — pertemuan kelas les;</li>
 *   <li>{@link MahasiswaRequestTugasAkhir} dan {@link Skripsi} — sesi bimbingan / sidang / revisi;</li>
 *   <li>{@link KrsMahasiswa} — konsultasi dengan Pembimbing Akademik;</li>
 *   <li>{@link ais.database.model.kkn.KelompokKkn} dan {@link ais.database.model.pkl.KelompokPkl} —
 *       pertemuan kelompok KKN/PKL;</li>
 *   <li>{@link JadwalUjianPMB}, {@link ais.database.model.sekolah.JadwalUjianPSB},
 *       {@link ais.database.model.sekolah.JadwalPertemuanPSB},
 *       {@link ais.database.model.recruitment.JadwalUjianPegawai} — jadwal ujian/pertemuan
 *       penerimaan mahasiswa/siswa/pegawai baru;</li>
 *   <li>{@link FormulirKegiatan} — kegiatan berbasis formulir pendaftaran;</li>
 *   <li>{@link PertemuanPunyaGrupPertemuan} — pertemuan pada "grup pertemuan" generik
 *       (konsultasi lain);</li>
 *   <li>{@link ais.database.model.kursus.KomponenDataProdukKursus} — sesi produk kursus;</li>
 *   <li>{@link Wisuda} — sesi wisuda.</li>
 * </ul>
 * <p>Konsekuensinya HAMPIR SETIAP method non-sepele di kelas ini berbentuk rantai
 * {@code if (perkuliahan != null) ... else if (jadwalPelajaran != null) ...}. Bila menambah jenis
 * induk baru, rantai itu harus ditambah di BANYAK tempat sekaligus — setidaknya {@link #untuk()},
 * {@link #toString()}, {@link #info()}, {@link #warna()}, {@link #ambilVOPembelajaran()},
 * {@link #ambilDosen()}, {@link #ambilDosenId()}, {@link #dosenUtama()}, {@link #ambilMahasiswa()},
 * {@link #getProgram()}, {@link #getFakultasId()}, {@link #getJurusanId()}, {@link #getSekolahId()},
 * {@link #getYayasanId()}, {@link #getTa()}, {@link #getSmt()}, {@link #getJurusan()},
 * {@link #getSekolah()}, {@link #getDosens()}, {@link #getMahasiswas()}, {@link #getGurus()},
 * {@link #getSiswas()}, {@link #generateJitsiLink(HttpServletRequest)}, dan
 * {@link #reInitKelompokParameterTambahanPertemuan(org.hibernate.Session)}. Dua titik pusat yang
 * paling layak dijadikan pegangan adalah {@link #untuk()} (nama jenis induk) dan
 * {@link #ambilVOPembelajaran()} (induk sebagai {@link VOPembelajaran}).</p>
 *
 * <h3>Posisi dalam hierarki</h3>
 * <p>Kelas ini {@code extends} {@link Tugas} (BUKAN langsung {@link GeneralValueObject}). Artinya
 * satu Pertemuan sekaligus BISA berperan sebagai tugas: ia punya {@code judultugas}/{@code isitugas},
 * {@code formatNilai}, {@code prosentase}, dan {@code syaratMengumpulkanTugas} sendiri. Kontrak
 * umum {@code id}/{@code equals}/{@code compareTo}/{@code check(...)}/{@code udah(...)}/
 * {@code belum(...)}/{@code ambilData(...)} diwarisi dari {@link GeneralValueObject} — lihat
 * dokumentasi di kelas itu, jangan diulang di sini.</p>
 *
 * <h3>Kehadiran disimpan sebagai satu kolom teks, bukan tabel anak</h3>
 * <p>Seluruh daftar hadir satu pertemuan ditaruh di satu kolom {@code text} bernama
 * {@code absensi}. Formatnya: baris dipisah {@code ';'}, kolom dipisah {@code ','}, dengan
 * sembilan slot tetap:</p>
 * <pre>
 * 0 ref               id peserta/pengajar (Mahasiswa/Siswa/Dosen/Guru/Pegawai)
 * 1 statusabsensi.id
 * 2 statusabsensi.kode   "M" = masuk/hadir, "-" dianggap belum diisi
 * 3 statusabsensi.nama
 * 4 pengajuanIzinTidakMasukPerkuliahan.id
 * 5 keterangan
 * 6 mulai                jam mulai kehadiran (hanya diisi bila kode "M")
 * 7 sampai               jam selesai kehadiran (hanya diisi bila kode "M")
 * 8 jenis                "Mahasiswa" | "Siswa" | "Dosen" | "Guru" | "Pegawai"
 * </pre>
 * <p>Slot {@code jenis} di ujung baris itulah yang dipakai {@link #hitungStatusDosen(Dosen)} dan
 * kawan-kawan untuk memisahkan baris pengajar dari baris peserta (lewat {@code endsWith}). Karena
 * {@code ','} adalah pemisah kolom, {@link #populate(Long, Statusabsensi, String,
 * PengajuanIzinTidakMasukPerkuliahan, String, String, String)} MENGGANTI setiap {@code ','} di
 * dalam keterangan menjadi {@code '_'} dan setiap {@code ';'} menjadi {@code "..\n"} sebelum
 * menulis — teks keterangan asli memang tidak dapat dikembalikan utuh.</p>
 * <p>Tiga kolom lain memakai tata letak sembilan slot yang SAMA tetapi arti slot 4 berubah menjadi
 * {@code dosen.id} (dosen yang mengonfirmasi), sehingga pencarian barisnya memakai pasangan
 * (ref, dosen):</p>
 * <ul>
 *   <li>{@code keteranganKonfirmasi} — konfirmasi kehadiran peserta oleh dosen
 *       ({@link #populateKonfirmasi(Long, Statusabsensi, String, String, String, String, Dosen)});</li>
 *   <li>{@code keteranganSesuaiDenganRps} — penilaian "materi sesuai RPS?" oleh dosen
 *       ({@link #populateKonfirmasiRps(Long, Long, String, String, String, String, Dosen)});</li>
 *   <li>{@code keteranganSesuaiOlehAkademik} — verifikasi yang sama oleh bagian akademik
 *       ({@link #populateOlehAkademik(String, Long, String, String, String, String, Dosen)}); di
 *       sini {@code ref} bertipe {@link String}, bukan {@link Long}.</li>
 * </ul>
 *
 * <h3>Koleksi anak disimpan sebagai "peta lokasi" berkas JSON, bukan relasi Hibernate</h3>
 * <p>Semua koleksi anak Pertemuan (lampiran, video, audio, diskusi, ujian, tugas, izin, parameter
 * tambahan) TIDAK dipetakan sebagai {@code @OneToMany}. Sebagai gantinya, tiap jenis anak punya
 * satu berkas indeks JSON di disk berisi pasangan {@code id -> id} (atau {@code id -> path berkas
 * cache}), diakses lewat kuintet method dengan pola nama yang selalu sama:</p>
 * <ol>
 *   <li>{@code ambilLokasiXxx()} — baca isi berkas indeks (kembalikan JSON kosong bila belum ada);</li>
 *   <li>{@code tulisLokasiXxx(String)} — tulis ulang berkas indeks;</li>
 *   <li>{@code bersihkanLokasiXxx()} — hapus berkas indeks;</li>
 *   <li>{@code reInitXxx(Session)} — bangun ulang indeks dari query Hibernate (mahal, hanya
 *       dipanggil saat {@code udah("...")} bernilai {@code false});</li>
 *   <li>{@code populateXxx(...)} / {@code removeXxx(...)} — tambah/hapus satu entri di indeks;</li>
 *   <li>{@code ambilXxxTotal()} / {@code ambilJumlahXxx()} / {@code ambilXxx(map, mulai, banyak)} —
 *       baca, hitung, dan potong halaman.</li>
 * </ol>
 * <p>Penanda "indeks sudah pernah dibangun" adalah {@code udah(namaJenis)} dari
 * {@link GeneralValueObject}; {@code belum(namaJenis)} membatalkannya sehingga indeks dibangun
 * ulang pada akses berikutnya. Nama jenis TIDAK selalu sama dengan nama method — misalnya
 * {@link #ambilTugasPertemuanTotal()} memakai kunci {@code "pertemuan_tugas"} sedangkan
 * {@link #ambilPertemuanPunyaUjianTotal(Tbmuser)} memakai {@code "pertemuan_punya_Ujian"} (dengan
 * huruf U besar). Salah menuliskan kunci berarti indeks dibangun ulang pada SETIAP akses.</p>
 * <p>Parsing JSON indeks dilakukan lewat {@link #bacaJsonObjekAman(String)},
 * {@link #jsonObjekAtauKosong(String)}, dan {@link #jsonObjekUntukTulis(String)} — bacalah Javadoc
 * ketiganya sebelum menambah pembaca/penulis indeks baru, karena jalur baca dan jalur tulis
 * SENGAJA berbeda perilaku saat menemui indeks rusak.</p>
 *
 * <h3>Getter di kelas ini TIDAK murni</h3>
 * <p>Karena Hibernate memetakan kelas ini lewat akses properti, banyak getter sekaligus melakukan
 * normalisasi, pengisian nilai bawaan dari induk, pembacaan konfigurasi, bahkan query. Beberapa
 * yang perlu diwaspadai: {@link #getTopik()} menghapus awalan "Pertemuan ke ..." dan menulis balik
 * ke field; {@link #getWaktuMulai()}/{@link #getWaktuSelesai()} menyalin jam dari
 * {@link Perkuliahan} dan menormalkan {@code "HH:mm"} menjadi {@code "HH.mm"}; {@link #getRuang()}
 * mewarisi ruang perkuliahan; {@link #getIndikator()}, {@link #getWaktupembelajaran()},
 * {@link #getPengalamanBelajar()}, dan {@link #getTugasDanPenilaian()} mengambil teks bawaan dari
 * tabel {@link Konfigurasi} (yang, bila kunci belum ada, IKUT MENULIS nilai bawaan ke basis data);
 * {@link #getKurikulumPunyaMatakuliahDetail()} bahkan dapat MEMBUAT template format bimbingan baru;
 * {@link #getMahasiswas()} menjalankan query dan diberi penjaga anti-rekursi karena autoflush
 * Hibernate bisa memanggilnya ulang di tengah eksekusinya sendiri. Nilai hasil normalisasi ini ikut
 * tersimpan saat flush berikutnya.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Identitas &amp; perbandingan</b>: {@link #getId()}, {@link #compareTo(GeneralValueObject)}
 *       (mengurutkan berdasarkan tanggal+jam lewat {@link #toTglDanWaktu()}), {@link #toString()},
 *       {@link #untuk()}, {@link #info()}, {@link #warna()}.</li>
 *   <li><b>Jadwal</b>: {@link #getTanggal()}, {@link #getTanggalRealisasi()},
 *       {@link #getTanggalEdit()}, {@link #getWaktuMulai()}, {@link #getWaktuSelesai()},
 *       {@link #getRuang()}, {@link #getPertemuanKe()}, {@link #getPertemuanManual()},
 *       {@link #apakahTerlewat()}.</li>
 *   <li><b>Kehadiran</b>: {@link #populate(Long, Statusabsensi, String, String, String)} dan
 *       seluruh keluarga {@code retreiveAbsensiXxx(...)} serta {@code hitungStatusXxx(...)};
 *       {@link #bolehUbahAbsen(Tbmuser)}, {@link #bolehUbahAbsenSaja(Tbmuser)},
 *       {@link #apakahAdaDosenYangMasuk()}, {@link #apakahAdaGuruYangMasuk()}.</li>
 *   <li><b>Materi/RPS</b>: {@link #getTopik()}, {@link #getIndikator()},
 *       {@link #getMetodePembelajaran()}, {@link #getPengalamanBelajar()},
 *       {@link #getTugasDanPenilaian()}, {@link #getBukuRujukan1()}, {@link #getBukuRujukan2()},
 *       {@link #getWaktupembelajaran()}, plus jalur verifikasi RPS
 *       ({@code populateKonfirmasiRps}, {@code populateOlehAkademik}).</li>
 *   <li><b>Peserta &amp; pengajar</b>: {@link #ambilDosen()}, {@link #ambilDosenId()},
 *       {@link #dosenUtama()}, {@link #ambilGuru()}, {@link #ambilMahasiswa()},
 *       {@link #ambilSiswa()}, {@link #apakahMahasiswaPesertaDisetujuiLangsung(Mahasiswa)}.</li>
 *   <li><b>Pembelajaran daring</b>: {@link #getOnlineMenggunakan()} dengan konstanta
 *       {@link #JITSI}/{@link #GOOGLE_MEET}/{@link #ZOOM}/{@link #BBB}/{@link #SKYPE}/{@link #WA}/
 *       {@link #LAIN}, {@link #getZoomLink()}, {@link #getMeetLink()}, {@link #getBbbLink()},
 *       {@link #getSkypeLink()}, {@link #getWaLink()}, {@link #getLainLink()},
 *       {@link #generateJitsiLink()}.</li>
 *   <li><b>Koleksi anak</b>: enam kelompok kuintet {@code Lokasi/reInit/remove/populate/ambil}
 *       yang dijelaskan di atas.</li>
 *   <li><b>Denormalisasi untuk pencarian</b>: {@link #getTa()}, {@link #getSmt()},
 *       {@link #getJurusan()}, {@link #getSekolah()}, {@link #getFakultasId()},
 *       {@link #getJurusanId()}, {@link #getSekolahId()}, {@link #getYayasanId()},
 *       {@link #getProgram()}, {@link #getDosens()}, {@link #getMahasiswas()},
 *       {@link #getGurus()}, {@link #getSiswas()} — semuanya menghitung ulang nilainya dari induk
 *       setiap kali dipanggil lalu menyimpannya ke kolom sendiri agar bisa difilter di query.</li>
 *   <li><b>UI</b>: {@link #tampilMk(Box)} dan {@link #populateParameterTambahan(java.util.List)}
 *       menerima/membentuk komponen ZK langsung dari model — tidak lazim untuk sebuah entity,
 *       tetapi memang begitu adanya.</li>
 * </ul>
 *
 * <h3>Catatan lain</h3>
 * <p>Kelas ini {@link Audited} (Hibernate Envers), sehingga setiap perubahan properti terekam ke
 * tabel revisi. {@link #onUpdate()} mengisi cap waktu ubah lewat
 * {@code AuditTimestampInterceptor}. Tanggal yang lebih tua dari 1 Januari 2000 dianggap RUSAK dan
 * diperlakukan sebagai {@code null} — lihat {@link #bersihkanTanggalRusak(Date)}.</p>
 *
 * @see GeneralValueObject
 * @see Tugas
 * @see Perkuliahan
 * @see VOPembelajaran
 * @see ais.action.master.helper.PenjadwalanHelper
 * @see ais.action.master.helper.AbsensiHelper
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pertemuan")
public class Pertemuan extends Tugas {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5246402588142446110L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Bentuk satu kunci urut berupa teks {@code yyyyMMdd + jamMulai + jamSelesai + id} yang dapat
	 * dibandingkan secara leksikografis.
	 *
	 * <p>Dipakai oleh {@link #compareTo(GeneralValueObject)} agar daftar pertemuan terurut menurut
	 * tanggal, lalu jam mulai, lalu jam selesai, lalu id (sebagai pemecah seri agar dua pertemuan
	 * berjadwal identik tetap punya urutan yang stabil).</p>
	 *
	 * <p>Bagian yang kosong diisi nilai pengganti yang tetap menjaga urutan: tanggal kosong menjadi
	 * {@code "00000000"} dan jam kosong menjadi {@code "00.00"}, sehingga pertemuan tanpa jadwal
	 * selalu berada paling awal. Perhatikan bahwa id TIDAK di-padding, jadi id "10" berada sebelum
	 * id "9" secara leksikografis — hal ini hanya berpengaruh pada dua pertemuan yang tanggal dan
	 * jamnya benar-benar sama.</p>
	 *
	 * @return kunci urut gabungan tanggal, jam mulai, jam selesai, dan id; tidak pernah {@code null}
	 * @see #compareTo(GeneralValueObject)
	 */
	public String toTglDanWaktu() {
		String tgl = getTanggal() == null ? "00000000" : Common.dateFormat8.get().format(getTanggal());
		tgl += getWaktuMulai() == null ? "00.00" : getWaktuMulai();
		tgl += getWaktuSelesai() == null ? "00.00" : getWaktuSelesai();
		tgl += getId() == null ? "0" : getId().toString();
		return tgl;
	}

	/**
	 * Paksa seluruh indeks koleksi anak pertemuan ini dibangun ulang dari basis data.
	 *
	 * <p>Membuka satu {@code Session} Hibernate native, lalu berturut-turut menghapus indeks izin
	 * tidak masuk, menandai indeks {@code KelompokParameterTambahanPertemuan} sebagai "belum ada"
	 * ({@code belum(...)}), dan menjalankan {@code reInit...} untuk diskusi, ujian, tugas pertemuan,
	 * tugas kelompok, izin tidak masuk, serta kelompok parameter tambahan.</p>
	 *
	 * <p><b>Efek samping berat.</b> Method ini menjalankan sejumlah query dan menulis ulang beberapa
	 * berkas indeks JSON di disk; jangan dipanggil di dalam perulangan atau pada jalur render daftar.
	 * Pemakaian yang wajar adalah setelah data anak diubah dari luar alur normal (impor massal,
	 * perbaikan data) sehingga indeks di disk tidak lagi selaras dengan basis data.</p>
	 *
	 * <p>Session ditutup di akhir, dan {@code HibernateUtil.closeSession()} tetap dipanggil walaupun
	 * terjadi exception di tengah jalan. Exception apa pun ditelan (hanya dicatat ke
	 * {@code ErrorAuditUtil}), jadi pemanggil TIDAK dapat mengetahui bila pembangunan ulang gagal
	 * sebagian.</p>
	 *
	 * @see #reInitPertemuanPunyaDiskusi(Session)
	 * @see #reInitPertemuanPunyaUjian(Session)
	 * @see #reInitTugasPertemuan(Session)
	 * @see #reInitTugasKelompok(Session)
	 * @see #reInitPengajuanIzinTidakMasukPerkuliahan(Session)
	 * @see #reInitKelompokParameterTambahanPertemuan(Session)
	 */
	public void refreshData() {
		Pertemuan pertemuan = this;
		try {
			Session session = HibernateUtil.currentNativeSession();
			pertemuan.bersihkanLokasiPengajuanIzinTidakMasukPerkuliahan();
			pertemuan.belum("KelompokParameterTambahanPertemuan");
			pertemuan.reInitPertemuanPunyaDiskusi(session);
			pertemuan.reInitPertemuanPunyaUjian(session);
			pertemuan.reInitTugasPertemuan(session);
			pertemuan.reInitTugasKelompok(session);
			pertemuan.reInitPengajuanIzinTidakMasukPerkuliahan(session);
			pertemuan.reInitKelompokParameterTambahanPertemuan(session);
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:126");
			// TODO: handle exception
		}
		HibernateUtil.closeSession();
	}

	/**
	 * Urutkan dua Pertemuan menurut jadwalnya (tanggal, jam mulai, jam selesai, id).
	 *
	 * <p>Bila {@code arg0} bukan {@link Pertemuan}, perbandingan diserahkan ke implementasi induk
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} (yang berbasis id).</p>
	 *
	 * <p>Setiap exception ditelan dan menghasilkan {@code 0} ("dianggap sama"). Konsekuensinya:
	 * pada data rusak, urutan menjadi tidak konsisten dan {@link java.util.TreeSet}/
	 * {@link java.util.Collections#sort(java.util.List)} dapat berperilaku aneh — ini keputusan
	 * kode lama, bukan perilaku yang diinginkan.</p>
	 *
	 * @param arg0 objek pembanding; boleh bukan Pertemuan
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}
	 * @see #toTglDanWaktu()
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			if (arg0 instanceof Pertemuan) {
				Pertemuan p = (Pertemuan) arg0;
				return toTglDanWaktu().compareTo(p.toTglDanWaktu());
			} else {
				return super.compareTo(arg0);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:142");
		}

		return 0;
	}

	/**
	 * Id pengguna yang terakhir dicatat sebagai pelaku perubahan pertemuan ini.
	 *
	 * @return id pelaku, atau {@code null} bila belum pernah diisi
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Isi id pelaku perubahan — <b>hanya bila nilainya berarti</b>.
	 *
	 * <p>Berbeda dari setter biasa: nilai {@code null} atau string kosong DIABAIKAN sehingga nilai
	 * lama tetap dipertahankan. Jadi jejak pelaku terakhir tidak bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pelaku; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Isi nama pelaku perubahan — <b>hanya bila nilainya berarti</b>.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: {@code null} atau string kosong diabaikan agar
	 * jejak pelaku terakhir tidak terhapus tanpa sengaja oleh alur simpan yang tidak mengisinya.</p>
	 *
	 * @param oleh nama pelaku; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir dicatat sebagai pelaku perubahan pertemuan ini.
	 *
	 * @return nama pelaku, atau {@code null} bila belum pernah diisi
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	private String ta;
	private String smt;
	private Jurusan jurusan;
	private Sekolah sekolah;
	private String dosens;
	private String mahasiswas;

	/* Guard anti-rekursi: getMahasiswas adalah property Hibernate yang
	 * menjalankan query; saat autoFlush, Hibernate bisa memanggil getter ini
	 * lagi di tengah eksekusinya sendiri -> StackOverflow. */
	private transient boolean sedangHitungMahasiswas = false;

	private String gurus;
	private String siswas;

	/**
	 * Apakah batas waktu untuk mengisi/mengubah absensi pertemuan ini sudah lewat?
	 *
	 * <p>Alur perhitungan:</p>
	 * <ol>
	 *   <li>{@code selisih} = jarak hari antara hari ini dan tanggal pertemuan, diambil nilai
	 *       mutlaknya lalu <b>dikurangi satu</b> (hari pertemuan itu sendiri tidak dihitung).
	 *       Pertemuan tanpa tanggal menghasilkan {@code selisih} nol sehingga tidak pernah
	 *       dianggap terlewat.</li>
	 *   <li>{@code toleransiHari} diambil dari
	 *       {@link Perkuliahan#getBatasWaktuBolehAbsenKehadiran()}. Bila pertemuan ini bukan milik
	 *       sebuah {@link Perkuliahan}, dipakai angka besar {@code 1000} — praktis "tak terbatas".</li>
	 *   <li>Bila konfigurasi {@code jumlah_hari_batas_waktu_pakai_default} AKTIF, toleransi per
	 *       perkuliahan itu ditimpa oleh konfigurasi global
	 *       {@code jumlah_hari_batas_waktu_dalam_hari}. Nilai yang tidak dapat diurai menjadi angka
	 *       diabaikan diam-diam sehingga toleransi sebelumnya tetap dipakai.</li>
	 * </ol>
	 *
	 * <p>Hasil {@code true} hanya mungkin bila
	 * {@link #getPerkulaiahnOnlineHarusSesuaiJadwal()} juga {@code true}; pertemuan yang boleh
	 * diabsen di luar jadwal tidak pernah dianggap terlewat.</p>
	 *
	 * <p><b>Catatan:</b> karena memakai nilai mutlak, pertemuan yang tanggalnya masih JAUH DI MASA
	 * DEPAN juga dilaporkan "terlewat".</p>
	 *
	 * @return {@code true} bila absensi sudah di luar batas waktu yang diizinkan
	 * @see #bolehUbahAbsen(Tbmuser)
	 * @see Konfigurasi
	 */
	public boolean apakahTerlewat() {
		Pertemuan pertemuan = this;
		Date currentDate = WaktuUtil.getDate();
		Integer selisih = pertemuan.getTanggal() == null ? 0
				: Math.abs(Common.getBetweenTwoDates(currentDate, pertemuan.getTanggal())) - 1;

		Integer toleransiHari = pertemuan.getPerkuliahan() == null ? 1000
				: pertemuan.getPerkuliahan().getBatasWaktuBolehAbsenKehadiran();
		if (Common.bolehKonfigurasi("jumlah_hari_batas_waktu_pakai_default", Konfigurasi.TIDAK_AKTIF)) {
			try {
				toleransiHari = Integer
						.parseInt(Common.getKonfigurasi("jumlah_hari_batas_waktu_dalam_hari", "0").getNilai().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:197");
				// TODO: handle exception
			}
		}

		boolean terlewat = pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal() && selisih > toleransiHari;
		return terlewat;
	}

	/**
	 * Bolehkah pengguna ini mengubah absensi pertemuan, <b>dengan</b> memperhitungkan batas waktu?
	 *
	 * <p>Aturannya, hasil {@code true} diperoleh bila salah satu dari dua hal berikut terpenuhi:</p>
	 * <ul>
	 *   <li>pengguna BUKAN peserta didik (bukan mahasiswa, bukan siswa, bukan calon mahasiswa,
	 *       bukan calon siswa — praktis: dosen, guru, atau petugas akademik) DAN batas waktu belum
	 *       terlewat menurut {@link #apakahTerlewat()}; atau</li>
	 *   <li>pengguna adalah mahasiswa yang ditetapkan sebagai <b>asisten absen</b> pada
	 *       {@link Perkuliahan} induk — dalam hal ini batas waktu tidak diberlakukan.</li>
	 * </ul>
	 *
	 * <p>Peran asisten absen hanya dikenali bila induk pertemuan adalah sebuah {@link Perkuliahan};
	 * untuk jenis induk lain, mahasiswa selalu ditolak.</p>
	 *
	 * <p><b>Perhatian:</b> {@code tbmuser} di-dereference tanpa penjagaan {@code null} pada baris
	 * penentu utama, sehingga memanggil method ini dengan {@code null} melempar
	 * {@link NullPointerException} — walaupun pemeriksaan {@code tbmuser != null} muncul di bagian
	 * lain method yang sama.</p>
	 *
	 * @param tbmuser pengguna yang sedang diperiksa; tidak boleh {@code null}
	 * @return {@code true} bila pengguna berhak mengubah absensi pertemuan ini sekarang
	 * @see #bolehUbahAbsenSaja(Tbmuser)
	 * @see #apakahTerlewat()
	 */
	public boolean bolehUbahAbsen(Tbmuser tbmuser) {
		boolean terlewat = apakahTerlewat();
		Pertemuan pertemuan = this;
		VOPembelajaran pembelajaran = ambilVOPembelajaran();
		boolean mahasiswaBolehUbahAbsen = false;
		if (tbmuser != null && tbmuser.getMahasiswa() != null && pembelajaran != null
				&& (pembelajaran instanceof Perkuliahan)) {
			mahasiswaBolehUbahAbsen = ((Perkuliahan) pembelajaran).merupakanAsistenAbsen(tbmuser.getMahasiswa());
		}

		boolean bolehUbah = (tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null
				&& (!pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal() || !terlewat)) || mahasiswaBolehUbahAbsen;

		if (!mahasiswaBolehUbahAbsen) {
			if (tbmuser != null && (tbmuser.getMahasiswa() != null
					|| tbmuser.getSiswa() != null && tbmuser.getBiodataCalonMahasiswa() != null
					|| tbmuser.getCalonSiswa() != null)) {
				bolehUbah = false;
			}
		}
		return bolehUbah;
	}

	/**
	 * Versi {@link #bolehUbahAbsen(Tbmuser)} yang mengabaikan batas waktu.
	 *
	 * <p>Isinya sama persis dengan {@link #bolehUbahAbsen(Tbmuser)} kecuali bahwa syarat
	 * {@link #apakahTerlewat()} tidak ikut diperiksa: yang diuji hanya PERAN pengguna. Dipakai di
	 * tempat yang perlu tahu "orang ini secara peran memang boleh menyentuh absensi" terlepas dari
	 * apakah jendela waktunya masih terbuka — misalnya untuk memutuskan apakah tombol perlu
	 * ditampilkan (walau dalam keadaan nonaktif) atau disembunyikan sama sekali.</p>
	 *
	 * <p>Sama seperti {@link #bolehUbahAbsen(Tbmuser)}, {@code tbmuser} bernilai {@code null} akan
	 * melempar {@link NullPointerException}.</p>
	 *
	 * @param tbmuser pengguna yang sedang diperiksa; tidak boleh {@code null}
	 * @return {@code true} bila peran pengguna mengizinkan pengubahan absensi
	 * @see #bolehUbahAbsen(Tbmuser)
	 */
	public boolean bolehUbahAbsenSaja(Tbmuser tbmuser) {
		VOPembelajaran pembelajaran = ambilVOPembelajaran();
		boolean mahasiswaBolehUbahAbsen = false;
		if (tbmuser != null && tbmuser.getMahasiswa() != null && pembelajaran != null
				&& (pembelajaran instanceof Perkuliahan)) {
			mahasiswaBolehUbahAbsen = ((Perkuliahan) pembelajaran).merupakanAsistenAbsen(tbmuser.getMahasiswa());
		}

		boolean bolehUbah = (tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getMahasiswa() == null
				&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null) || mahasiswaBolehUbahAbsen;

		if (!mahasiswaBolehUbahAbsen) {
			if (tbmuser != null && (tbmuser.getMahasiswa() != null
					|| tbmuser.getSiswa() != null && tbmuser.getBiodataCalonMahasiswa() != null
					|| tbmuser.getCalonSiswa() != null)) {
				bolehUbah = false;
			}
		}
		return bolehUbah;
	}

	/**
	 * Kait daur hidup JPA: dijalankan otomatis tepat sebelum baris pertemuan di-{@code UPDATE}.
	 *
	 * <p>Menyerahkan pengisian cap waktu/pelaku perubahan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}. Jangan dipanggil manual — penyedia
	 * persistensi yang memanggilnya.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setel cap waktu perubahan terakhir pertemuan ini.
	 *
	 * @param tanggal_dirubah cap waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu perubahan terakhir pertemuan ini.
	 *
	 * <p>Field-nya diinisialisasi ke waktu sekarang saat objek dibuat, jadi pertemuan yang baru
	 * dibentuk di memori sudah punya nilai walaupun belum pernah disimpan.</p>
	 *
	 * @return cap waktu perubahan terakhir
	 * @see #onUpdate()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nama sederhana kelas induk pertemuan ini — inti dari sifat polimorfik {@link Pertemuan}.
	 *
	 * <p>Menyegarkan seluruh asosiasi induk lewat getter-nya masing-masing (agar proxy Hibernate
	 * yang basi ikut diperbarui, lihat {@code check(...)} di {@link GeneralValueObject}), lalu
	 * mengembalikan {@link Class#getSimpleName()} dari induk PERTAMA yang tidak {@code null}
	 * menurut urutan pemeriksaan di dalam method.</p>
	 *
	 * <p>Urutan itu penting: bila karena data rusak ada lebih dari satu kolom induk terisi, yang
	 * dilaporkan hanyalah yang pertama ditemukan.</p>
	 *
	 * <p><b>Perhatikan dua ketidaklengkapan</b> yang memang ada di kode: {@code jadwalPertemuanPSB}
	 * dan {@code wisuda} disegarkan/dipakai di tempat lain tetapi TIDAK punya cabangnya sendiri di
	 * sini, sehingga pertemuan yang hanya bertaut ke salah satu dari keduanya menghasilkan
	 * {@code null}.</p>
	 *
	 * @return nama sederhana kelas induk (mis. {@code "Perkuliahan"}), atau {@code null} bila tidak
	 *         ada induk yang dikenali
	 * @see #ambilVOPembelajaran()
	 * @see #info()
	 */
	public String untuk() {
		perkuliahan = getPerkuliahan();
		jadwalUjianPMB = getJadwalUjianPMB();
		mahasiswaRequestTugasAkhir = getMahasiswaRequestTugasAkhir();
		kelompokKkn = getKelompokKkn();
		kelompokPkl = getKelompokPkl();
		skripsi = getSkripsi();
		krsMahasiswa = getKrsMahasiswa();
		jadwalUjianPSB = getJadwalUjianPSB();
		jadwalPertemuanPSB = getJadwalPertemuanPSB();
		jadwalUjianPegawai = getJadwalUjianPegawai();
		jadwalPelajaran = getJadwalPelajaran();
		pertemuanPunyaGrupPertemuan = getPertemuanPunyaGrupPertemuan();
		formulirKegiatan = getFormulirKegiatan();
		komponenDataProdukKursus = getKomponenDataProdukKursus();
		kelasLesSiswa = getKelasLesSiswa();

		if (perkuliahan != null) {
			return Perkuliahan.class.getSimpleName();
		} else if (jadwalUjianPMB != null) {
			return JadwalUjianPMB.class.getSimpleName();
		} else if (mahasiswaRequestTugasAkhir != null) {
			return MahasiswaRequestTugasAkhir.class.getSimpleName();
		} else if (kelompokKkn != null) {
			return KelompokKkn.class.getSimpleName();
		} else if (kelompokPkl != null) {
			return KelompokPkl.class.getSimpleName();
		} else if (skripsi != null) {
			return Skripsi.class.getSimpleName();
		} else if (krsMahasiswa != null) {
			return KrsMahasiswa.class.getSimpleName();
		} else if (jadwalUjianPSB != null) {
			return JadwalUjianPSB.class.getSimpleName();
		} else if (jadwalUjianPegawai != null) {
			return JadwalUjianPegawai.class.getSimpleName();
		} else if (jadwalPelajaran != null) {
			return JadwalPelajaran.class.getSimpleName();
		} else if (pertemuanPunyaGrupPertemuan != null) {
			return PertemuanPunyaGrupPertemuan.class.getSimpleName();
		} else if (formulirKegiatan != null) {
			return FormulirKegiatan.class.getSimpleName();
		} else if (komponenDataProdukKursus != null) {
			return KomponenDataProdukKursus.class.getSimpleName();
		} else if (kelasLesSiswa != null) {
			return KelasLesSiswa.class.getSimpleName();
		} else {
			return null;
		}
	}

	/**
	 * Wakil teks untuk keperluan log dan penelusuran, bukan untuk ditampilkan ke pengguna.
	 *
	 * <p>Bentuknya {@code id-topik-pertemuanKe} disusul potongan {@code infoSimple()} dari SETIAP
	 * induk yang tidak {@code null} (praktis hanya satu). Untuk teks yang ramah pengguna, pakai
	 * {@link #info()}.</p>
	 *
	 * <p><b>Bukan operasi murah.</b> Seperti {@link #untuk()}, method ini memanggil semua getter
	 * induk sehingga dapat memicu inisialisasi proxy Hibernate — dan {@code infoSimple()} milik
	 * induk dapat menjalankan query lagi. Hindari memanggilnya di dalam perulangan panas atau di
	 * dalam pesan log yang selalu dirangkai walau level lognya mati.</p>
	 *
	 * @return ringkasan teks pertemuan beserta induknya
	 */
	public String toString() {
		perkuliahan = getPerkuliahan();
		jadwalUjianPMB = getJadwalUjianPMB();
		mahasiswaRequestTugasAkhir = getMahasiswaRequestTugasAkhir();
		kelompokKkn = getKelompokKkn();
		kelompokPkl = getKelompokPkl();
		skripsi = getSkripsi();
		krsMahasiswa = getKrsMahasiswa();
		jadwalUjianPSB = getJadwalUjianPSB();
		jadwalPertemuanPSB = getJadwalPertemuanPSB();
		jadwalUjianPegawai = getJadwalUjianPegawai();
		jadwalPelajaran = getJadwalPelajaran();
		pertemuanPunyaGrupPertemuan = getPertemuanPunyaGrupPertemuan();
		formulirKegiatan = getFormulirKegiatan();
		komponenDataProdukKursus = getKomponenDataProdukKursus();
		kelasLesSiswa = getKelasLesSiswa();

		return id + "-" + topik + "-" + pertemuanKe + (perkuliahan == null ? "" : "-" + perkuliahan.infoSimple())
				+ (jadwalUjianPMB == null ? "" : "-" + jadwalUjianPMB.infoSimple())
				+ (mahasiswaRequestTugasAkhir == null ? "" : "-" + mahasiswaRequestTugasAkhir.infoSimple())
				+ (kelompokKkn == null ? "" : "-" + kelompokKkn.infoSimple())
				+ (kelompokPkl == null ? "" : "-" + kelompokPkl.infoSimple())
				+ (skripsi == null ? "" : "-" + skripsi.infoSimple())
				+ (kelasLesSiswa == null ? "" : "-" + kelasLesSiswa.infoSimple())
				+ (krsMahasiswa == null ? "" : "-" + krsMahasiswa.infoSimple())
				+ (jadwalUjianPSB == null ? "" : "-" + jadwalUjianPSB.infoSimple())
				+ (jadwalPertemuanPSB == null ? "" : "-" + jadwalPertemuanPSB.infoSimple())
				+ (jadwalUjianPegawai == null ? "" : "-" + jadwalUjianPegawai.infoSimple())
				+ (jadwalPelajaran == null ? "" : "-" + jadwalPelajaran.infoSimple())
				+ (pertemuanPunyaGrupPertemuan == null ? "" : "-" + pertemuanPunyaGrupPertemuan.infoSimple())
				+ (formulirKegiatan == null ? "" : "-" + formulirKegiatan.infoSimple())
				+ (komponenDataProdukKursus == null ? "" : "-" + komponenDataProdukKursus.infoSimple());
	}

	private Boolean mandiri;

	private Perkuliahan perkuliahan;
	private JadwalUjianPMB jadwalUjianPMB;
	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;
	private KelompokKkn kelompokKkn;
	private KelompokPkl kelompokPkl;
	private Skripsi skripsi;
	private KrsMahasiswa krsMahasiswa;
	private JadwalUjianPSB jadwalUjianPSB;
	private JadwalPertemuanPSB jadwalPertemuanPSB;
	private JadwalUjianPegawai jadwalUjianPegawai;
	private JadwalPelajaran jadwalPelajaran;
	private KelasLesSiswa kelasLesSiswa;
	private PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan;
	private FormulirKegiatan formulirKegiatan;
	private KomponenDataProdukKursus komponenDataProdukKursus;

	private Date tanggalEdit;

	private Long petugas;
	private Long petugas2;
	private Long petugas3;
	private Long petugas4;
	private Long pjDosen;

	private String topik;
	private String catatan;
	private String indikator;
	private String waktupembelajaran;
	private String pengalamanBelajar;
	private String tugasDanPenilaian;
	private String bukuRujukan1;
	private String bukuRujukan2;
	private String dosenTamu;
	private String dosenTamu2;
	private Long dosenPengganti;

	private String guruTamu;
	private String guruTamu2;
	private Long guruPengganti;

	private String metodePembelajaran = "";
	private Date tanggal;
	private Date tanggalRealisasi;
	private StatusPertemuan statusPertemuan;
	private Integer pertemuanKe = 1;
	private Integer pertemuanManual = 1;

	private String waktuMulai = "";
	private String waktuSelesai = "";
	private Ruang ruang;

	private String isitugas = "";
	private String judultugas = "";

	private Date mulai;
	private Date selesai;

	private Long copyDariPertemuan;
	private String absensi;

	private Long kurikulumPunyaMatakuliahDetail;
	private Long fakultasId;
	private Long jurusanId;
	private Long sekolahId;
	private Long yayasanId;
	private String program;
	private FormatNilai formatNilai;
	private Double prosentase;

	private Boolean publikasikanStreaming;
	private Boolean tampilkanJamAbsensiBagiMahasiswa;

	private SyaratUjian syaratMengumpulkanTugas;

	private Boolean komentarDitutup;
	private Boolean izinkanUploadLampiranDiKomentar;
	private Boolean izinkanUploadLampiranDiGrive;

	private String calendarEvent;
	private String zoomLink;
	private String bbbLink;
	private Integer onlineMenggunakan;
	private String skypeLink;
	private String waLink;
	private String mhsYgTidakIkut;
	private String meetLink;
	private String lainLink;

	private Boolean perkulaiahnOnlineHarusSesuaiJadwal;
	private String parameterTambahanInds;
	private String parameterTambahan;
	private String mhsBolehUploadUlang;

	private Boolean mahasiswaBolehAbsenMenggunakanFoto;
	private Boolean dosenBolehAbsenMenggunakanFoto;

	private Integer bolehAbsenSebelumWaktuMulaiDalamMenit;
	private Integer bolehAbsenSetelahWaktuMulaiDalamMenit;
	private Wisuda wisuda;

	private String syaratAkses;
	private Lokasi lokasi;
	private Double jarak;
	private String keteranganKonfirmasi;
	private String keteranganSesuaiDenganRps;
	private String keteranganSesuaiOlehAkademik;
	private JenisItemPenilaianSiswa jenisItemPenilaianSiswa;
	private Boolean aktif;
	private Boolean sesuai;
	private GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa;
	private GrupPenilaian grupPenilaian;
	private String keteranganNilai;
	private String keteranganNilaiLama;
	private String formatNilais;
	private Boolean sudahDiproses;

	/** Konstruktor kosong yang diwajibkan Hibernate/JPA. */
	public Pertemuan() {
	}

	/**
	 * Kunci utama pertemuan (kolom {@code id}, IDENTITY, diisi oleh basis data).
	 *
	 * @return id pertemuan, atau {@code null} bila belum pernah disimpan
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setel kunci utama pertemuan. Normalnya diisi oleh Hibernate, bukan oleh kode aplikasi.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kelas kuliah induk, bila pertemuan ini memang tatap muka perkuliahan.
	 *
	 * <p>Ini induk yang paling sering terisi. Nilai {@code null} berarti pertemuan ini milik jenis
	 * induk lain — lihat {@link #untuk()} untuk daftar lengkapnya.</p>
	 *
	 * <p>Proxy disegarkan lewat {@code check(...)} milik {@link GeneralValueObject} sebelum
	 * dikembalikan, sehingga aman dipakai walaupun objek pertemuan sempat lepas dari session.</p>
	 *
	 * @return {@link Perkuliahan} induk, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perkuliahan", nullable = true)
	public Perkuliahan getPerkuliahan() {
		perkuliahan = check(perkuliahan);
		return this.perkuliahan;
	}

	/**
	 * Tetapkan kelas kuliah induk pertemuan ini.
	 *
	 * <p>Ingat aturan "tepat satu induk": mengisi relasi ini sementara relasi induk lain juga
	 * terisi akan membuat {@link #untuk()}, {@link #info()}, dan seluruh rantai
	 * {@code if/else if} lainnya memilih induk yang pertama ditemukan saja.</p>
	 *
	 * @param perkuliahan kelas kuliah induk, boleh {@code null}
	 */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * Topik/pokok bahasan pertemuan, sudah dibersihkan.
	 *
	 * <p><b>Getter ini mengubah keadaan objek.</b> Dua pembersihan dilakukan dan hasilnya ditulis
	 * balik ke field, sehingga ikut tersimpan pada flush berikutnya:</p>
	 * <ol>
	 *   <li>Bila topik diawali teks "pertemuan ke" (tidak peka huruf besar/kecil), awalan
	 *       {@code "Pertemuan ke <n>,"} dan {@code "Pertemuan ke"} dibuang. Tujuannya menghapus
	 *       penomoran yang dahulu ikut diketik ke dalam topik, karena nomor pertemuan sekarang
	 *       punya kolom sendiri ({@link #getPertemuanKe()}).</li>
	 *   <li>{@code filterTidakBoleh(...)} dari {@link GeneralValueObject} membuang potongan yang
	 *       dianggap berbahaya/tidak diizinkan.</li>
	 * </ol>
	 *
	 * <p>Topik yang belum diisi menghasilkan teks pengganti {@code "Pembahasan tentang ..."}, bukan
	 * {@code null} — jadi jangan memakai hasil method ini untuk menguji "apakah topik sudah diisi".</p>
	 *
	 * @return topik yang sudah dibersihkan dan di-{@code trim}; tidak pernah {@code null}
	 */
	@Column(name = "topik", columnDefinition = "text")
	public String getTopik() {

		try {
			if (topik != null && topik.trim().toLowerCase().startsWith("pertemuan ke")) {
				topik = topik.replaceAll("Pertemuan ke " + pertemuanKe + ",", "").replaceAll("Pertemuan ke", "");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:501");
			// TODO: handle exception
		}

		topik = filterTidakBoleh(topik);

		return this.topik == null ? "Pembahasan tentang ..." : topik.trim();
	}

	/**
	 * Setel topik/pokok bahasan pertemuan (disimpan apa adanya; pembersihan terjadi saat dibaca).
	 *
	 * @param topik teks topik
	 * @see #getTopik()
	 */
	public void setTopik(String topik) {
		this.topik = topik;
	}

	/**
	 * Buku rujukan utama pertemuan (bagian dari isian RPS).
	 *
	 * <p>Nilainya dilewatkan {@code filterTidakBoleh(...)} dan hasil bersihnya ditulis balik ke
	 * field. Nilai kosong menghasilkan string kosong, bukan {@code null}.</p>
	 *
	 * @return buku rujukan pertama yang sudah dibersihkan; tidak pernah {@code null}
	 * @see #getBukuRujukan2()
	 */
	@Column(name = "buku_rujukan1", columnDefinition = "text")
	public String getBukuRujukan1() {
		bukuRujukan1 = filterTidakBoleh(bukuRujukan1);
		return this.bukuRujukan1 == null ? "" : bukuRujukan1.trim();
	}

	/**
	 * Setel buku rujukan utama pertemuan.
	 *
	 * @param bukuRujukan1 teks buku rujukan
	 */
	public void setBukuRujukan1(String bukuRujukan1) {
		this.bukuRujukan1 = bukuRujukan1;
	}

	/**
	 * Buku rujukan kedua pertemuan (bagian dari isian RPS).
	 *
	 * <p>Sama seperti {@link #getBukuRujukan1()} nilainya dibersihkan dengan
	 * {@code filterTidakBoleh(...)}, TETAPI berbeda dalam dua hal: hasilnya TIDAK di-{@code trim}
	 * dan nilai kosong dikembalikan sebagai {@code null}, bukan string kosong. Perbedaan ini
	 * tampaknya tidak disengaja, jadi pemanggil sebaiknya tetap menjaga terhadap {@code null}.</p>
	 *
	 * @return buku rujukan kedua yang sudah dibersihkan, atau {@code null}
	 */
	@Column(name = "buku_rujukan2", columnDefinition = "text")
	public String getBukuRujukan2() {
		bukuRujukan2 = filterTidakBoleh(bukuRujukan2);
		return this.bukuRujukan2;
	}

	/**
	 * Setel buku rujukan kedua pertemuan.
	 *
	 * @param bukuRujukan2 teks buku rujukan
	 */
	public void setBukuRujukan2(String bukuRujukan2) {
		this.bukuRujukan2 = bukuRujukan2;
	}

	/**
	 * Nama dosen tamu pertama yang mengisi pertemuan ini (teks bebas, bukan relasi ke
	 * {@link Dosen}).
	 *
	 * @return nama dosen tamu, atau {@code null}
	 * @see #getDosenTamu2()
	 * @see #getGuruTamu()
	 */
	@Column(name = "dosen_tamu", columnDefinition = "text")
	public String getDosenTamu() {
		return this.dosenTamu;
	}

	/**
	 * Setel nama dosen tamu pertama.
	 *
	 * @param dosenTamu nama dosen tamu
	 */
	public void setDosenTamu(String dosenTamu) {
		this.dosenTamu = dosenTamu;
	}

	// P3 anti-korupsi tanggal: ambang tanggal valid minimum (2000-01-01 UTC). Nilai < ambang
	// (mis. epoch 0 = 01-01-1970, dari cache/serialisasi MapDB yang tak-selaras) dianggap RUSAK dan
	// diperlakukan sbg null. Pemakai getTanggal()/getTanggalRealisasi() sudah aman-null (renderer
	// tampilkan "-", toTglDanWaktu → "00000000"). Karena Pertemuan dipetakan property-access, ini
	// juga mencegah nilai epoch tertulis balik ke kolom 'tanggal'/'tanggal_realisasi' (persist null,
	// bukan 1970). Tak ada pertemuan sah bertanggal < 2000 di sistem akademik → aman.
	private static final long TANGGAL_MIN_VALID_MS = 946684800000L; // 2000-01-01T00:00:00Z

	/**
	 * Perlakukan tanggal yang lebih tua dari ambang {@code TANGGAL_MIN_VALID_MS} (1 Januari 2000)
	 * sebagai RUSAK, yaitu kembalikan {@code null}.
	 *
	 * <p>Rasionalnya dijelaskan pada komentar konstanta di atas: nilai seperti epoch 0
	 * (1 Januari 1970) berasal dari cache/serialisasi yang tidak selaras, bukan dari data akademik
	 * yang sah. Karena {@link Pertemuan} dipetakan lewat akses properti, penyaring ini dipasang di
	 * getter DAN setter sehingga nilai rusak tidak hanya disembunyikan dari tampilan tetapi juga
	 * tidak tertulis balik ke kolom {@code tanggal}/{@code tanggal_realisasi}.</p>
	 *
	 * @param d tanggal yang diperiksa; boleh {@code null}
	 * @return {@code d} apa adanya bila masuk akal, atau {@code null} bila {@code null}/terlalu tua
	 * @see #getTanggal()
	 * @see #getTanggalRealisasi()
	 */
	private static Date bersihkanTanggalRusak(Date d) {
		return (d != null && d.getTime() < TANGGAL_MIN_VALID_MS) ? null : d;
	}

	/**
	 * Tanggal pelaksanaan pertemuan.
	 *
	 * <p><b>Getter ini mengubah keadaan objek.</b> Dua penyesuaian dilakukan sebelum nilai
	 * dikembalikan, dan keduanya menulis balik ke field:</p>
	 * <ol>
	 *   <li>bila {@code tanggal} kosong tetapi {@code mulai} terisi, tanggal diambil dari
	 *       {@code mulai} (kompatibilitas dengan data lama yang hanya mengisi {@code mulai});</li>
	 *   <li>bila {@link #getTanggalEdit()} terisi, nilai itu MENIMPA tanggal — kolom
	 *       {@code tanggalEdit} berfungsi sebagai penggeser jadwal yang selalu menang.</li>
	 * </ol>
	 *
	 * <p>Hasil akhirnya masih dilewatkan {@link #bersihkanTanggalRusak(Date)}, sehingga tanggal
	 * sebelum tahun 2000 dikembalikan sebagai {@code null}.</p>
	 *
	 * @return tanggal pertemuan, atau {@code null} bila belum dijadwalkan/nilainya rusak
	 * @see #getTanggalRealisasi()
	 * @see #getTanggalEdit()
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal", length = 0)
	public Date getTanggal() {
		if (tanggal == null && mulai != null) {
			tanggal = mulai;
		}

		if (getTanggalEdit() != null) {
			tanggal = getTanggalEdit();
		}

		return bersihkanTanggalRusak(this.tanggal);
	}

	/**
	 * Setel tanggal pelaksanaan pertemuan.
	 *
	 * <p>Nilai yang lebih tua dari 1 Januari 2000 DIBUANG (tersimpan sebagai {@code null}) — lihat
	 * {@link #bersihkanTanggalRusak(Date)}.</p>
	 *
	 * @param tanggal tanggal pertemuan; boleh {@code null}
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = bersihkanTanggalRusak(tanggal);
	}

	/**
	 * Setel nomor urut pertemuan hasil penomoran otomatis.
	 *
	 * @param pertemuanKe nomor pertemuan
	 * @see #getPertemuanKe()
	 */
	public void setPertemuanKe(Integer pertemuanKe) {
		this.pertemuanKe = pertemuanKe;
	}

	/**
	 * Nomor pertemuan ke berapa dalam rangkaian pembelajaran induknya.
	 *
	 * <p>Sistem menyimpan DUA nomor: {@code pertemuanKe} (hasil penomoran otomatis menurut urutan
	 * jadwal) dan {@code pertemuanManual} (nomor yang diketik sendiri oleh pengguna). Yang berlaku
	 * ditentukan oleh saklar {@code urutkanotomatis} pada induk
	 * ({@link VOPembelajaran#getUrutkanotomatis()}): bila penomoran otomatis DIMATIKAN, getter ini
	 * mengambil alih nilai dari {@link #getPertemuanManual()} dan menulisnya ke field
	 * {@code pertemuanKe}.</p>
	 *
	 * <p>{@link #getPertemuanManual()} melakukan hal yang berkebalikan, sehingga kedua getter itu
	 * saling menyalin tergantung saklar tersebut. Karena keduanya saling memanggil hanya pada
	 * cabang yang berlawanan, tidak terjadi rekursi tak berujung.</p>
	 *
	 * <p>Nilai kosong dilaporkan sebagai {@code 1}, bukan {@code null}.</p>
	 *
	 * @return nomor pertemuan yang berlaku; tidak pernah {@code null}
	 * @see #getPertemuanManual()
	 */
	@Column(name = "pertemuan_ke", length = 10)
	public Integer getPertemuanKe() {
		VOPembelajaran pembelajaran = ambilVOPembelajaran();
		if (pembelajaran != null && !pembelajaran.getUrutkanotomatis()) {
			pertemuanKe = getPertemuanManual();
		}
		return pertemuanKe == null ? 1 : pertemuanKe;
	}

	/**
	 * Setel jenis sesi pertemuan (tatap muka, ujian, praktikum, dan sebagainya).
	 *
	 * @param statusPertemuan jenis sesi; {@code null} berarti kembali ke bawaan saat dibaca
	 * @see #getStatusPertemuan()
	 */
	public void setStatusPertemuan(StatusPertemuan statusPertemuan) {
		this.statusPertemuan = statusPertemuan;
	}

	/**
	 * Ambil daftar {@code userId} dari seluruh akun {@link Tbmuser} aktif yang bertaut ke satu
	 * entitas pemilik.
	 *
	 * <p>Dipakai oleh {@link #populate(Long, Statusabsensi, String,
	 * PengajuanIzinTidakMasukPerkuliahan, String, String, String)} untuk menemukan akun mana saja
	 * yang harus menerima notifikasi kehadiran seorang guru, dosen, atau pegawai. Untuk mahasiswa
	 * dan siswa hal ini tidak diperlukan karena notifikasinya dialamatkan ke NIM/nomor induk
	 * langsung.</p>
	 *
	 * <p>Akun dianggap aktif bila kolom {@code aktif} bernilai {@code true} ATAU masih
	 * {@code null} (data lama yang belum pernah diisi).</p>
	 *
	 * <p>Membuka {@code Session} Hibernate native sendiri dan SELALU menutupnya di blok
	 * {@code finally} — penting karena pemanggilnya berjalan di thread terpisah, di luar session
	 * milik permintaan web.</p>
	 *
	 * @param properti nama properti relasi pada {@link Tbmuser}, mis. {@code "guru"},
	 *                 {@code "dosen"}, {@code "pegawai"}; digabung menjadi {@code properti + ".id"}
	 * @param ref      id entitas pemilik yang dicari
	 * @return daftar {@code userId}; kosong bila tidak ada yang cocok
	 */
	@SuppressWarnings("unchecked")
	private List<String> ambilUsernamePemilik(String properti, Long ref) {
		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			return session.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq(properti + ".id", ref))
					.setProjection(Projections.property("userId")).list();
		} finally {
			if (session != null && session.isOpen()) {
				session.clear();
				if (session.isConnected()) {
					session.disconnect();
				}
				session.close();
			}
		}
	}

	/**
	 * Jenis sesi pertemuan ini (tatap muka, ujian, praktikum, dan sebagainya).
	 *
	 * <p>Kolomnya {@code NOT NULL}, dan getter ini menjamin hal tersebut: bila belum diisi, nilai
	 * bawaan {@code ConstantValues.TATAP_MUKA} DIPASANG ke field sehingga ikut tersimpan pada
	 * flush berikutnya. Jadi pertemuan tanpa jenis eksplisit otomatis menjadi tatap muka.</p>
	 *
	 * <p>Jenis sesi ikut menentukan perilaku lain, mis. {@link #fileContent(boolean)} selalu
	 * melaporkan "ada lampiran" untuk sesi yang bertanda ujian.</p>
	 *
	 * @return jenis sesi; tidak pernah {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_pertemuan", nullable = false)
	public StatusPertemuan getStatusPertemuan() {
		statusPertemuan = check(statusPertemuan);
		if (statusPertemuan == null) {
			statusPertemuan = ConstantValues.TATAP_MUKA;
		}
		return statusPertemuan;
	}

	/**
	 * Setel jam mulai pertemuan.
	 *
	 * <p>String kosong dinormalkan menjadi {@code null} agar hanya ada satu representasi
	 * "belum diisi".</p>
	 *
	 * @param waktuMulai jam mulai, biasanya berformat {@code "HH.mm"}; boleh {@code null}/kosong
	 * @see #getWaktuMulai()
	 */
	public void setWaktuMulai(String waktuMulai) {
		this.waktuMulai = waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/**
	 * Jam mulai pertemuan sebagai teks berformat {@code "HH.mm"} (titik, bukan titik dua).
	 *
	 * <p><b>Getter ini mengubah keadaan objek</b> dan melakukan tiga hal berurutan, semuanya
	 * menulis balik ke field:</p>
	 * <ol>
	 *   <li><b>Warisi dari perkuliahan.</b> Bila pertemuan ini milik sebuah {@link Perkuliahan} dan
	 *       jam mulainya belum diisi (atau masih berupa {@code "00.00"}), jam diambil dari
	 *       {@link Perkuliahan#getWaktuMulai()}. Karena {@code "00.00"} diperlakukan sebagai "belum
	 *       diisi", pertemuan yang memang benar-benar mulai tengah malam TIDAK dapat dinyatakan.</li>
	 *   <li><b>Normalkan pemisah.</b> {@code "HH:mm"} diubah menjadi {@code "HH.mm"}.</li>
	 *   <li><b>Buang bagian tanggal.</b> Bila nilainya mengandung spasi (mis. hasil salin tempel
	 *       {@code "2026-09-02 08.00"}), yang diambil adalah potongan SETELAH spasi pertama.</li>
	 * </ol>
	 *
	 * @return jam mulai berformat {@code "HH.mm"}, atau {@code null} bila belum ada
	 * @see #getWaktuSelesai()
	 */
	@Column(name = "waktu_mulai", length = 20)
	public String getWaktuMulai() {
		perkuliahan = getPerkuliahan();
		if (perkuliahan != null
				&& (waktuMulai == null || waktuMulai.trim().equalsIgnoreCase("00.00") || waktuMulai.trim().isEmpty())) {
			waktuMulai = perkuliahan.getWaktuMulai();
		}

		try {
			if (waktuMulai != null && StringUtils.contains(waktuMulai, ":")) {
				String[] spl = StringUtils.split(waktuMulai, ":");
				waktuMulai = spl[0] + "." + spl[1];
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:617");

		}

		try {
			if (waktuMulai != null && StringUtils.contains(waktuMulai.trim(), " ")) {
				String[] spl = StringUtils.split(waktuMulai.trim(), " ");
				waktuMulai = spl[1];
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:626");

		}

		return waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/**
	 * Setel jam selesai pertemuan; string kosong dinormalkan menjadi {@code null}.
	 *
	 * @param waktuSelesai jam selesai berformat {@code "HH.mm"}; boleh {@code null}/kosong
	 * @see #getWaktuSelesai()
	 */
	public void setWaktuSelesai(String waktuSelesai) {
		this.waktuSelesai = waktuSelesai == null || waktuSelesai.trim().equals("") ? null : waktuSelesai.trim();
	}

	/**
	 * Jam selesai pertemuan sebagai teks berformat {@code "HH.mm"}.
	 *
	 * <p>Perilakunya cermin persis {@link #getWaktuMulai()}: mewarisi jam dari {@link Perkuliahan}
	 * induk bila kosong atau masih {@code "00.00"}, menormalkan {@code "HH:mm"} menjadi
	 * {@code "HH.mm"}, dan membuang bagian tanggal bila nilainya mengandung spasi. Semua hasil
	 * normalisasi ditulis balik ke field.</p>
	 *
	 * @return jam selesai berformat {@code "HH.mm"}, atau {@code null} bila belum ada
	 * @see #getWaktuMulai()
	 */
	@Column(name = "waktu_selesai", length = 20)
	public String getWaktuSelesai() {
		perkuliahan = getPerkuliahan();
		if (perkuliahan != null && (waktuSelesai == null || waktuSelesai.trim().equalsIgnoreCase("00.00")
				|| waktuSelesai.trim().isEmpty())) {
			waktuSelesai = perkuliahan.getWaktuSelesai();
		}

		try {
			if (waktuSelesai != null && StringUtils.contains(waktuSelesai, ":")) {
				String[] spl = StringUtils.split(waktuSelesai, ":");
				waktuSelesai = spl[0] + "." + spl[1];
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:650");

		}

		try {
			if (waktuSelesai != null && StringUtils.contains(waktuSelesai.trim(), " ")) {
				String[] spl = StringUtils.split(waktuSelesai.trim(), " ");
				waktuSelesai = spl[1];
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:659");

		}

		return waktuSelesai == null || waktuSelesai.trim().equals("") ? null : waktuSelesai.trim();
	}

	/**
	 * Setel ruang tempat pertemuan berlangsung.
	 *
	 * @param ruang ruang; {@code null} berarti "ikuti ruang perkuliahan" saat dibaca
	 * @see #getRuang()
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Ruang tempat pertemuan berlangsung.
	 *
	 * <p><b>Getter ini mengubah keadaan objek.</b> Bila ruang pertemuan belum ditentukan sementara
	 * {@link Perkuliahan} induk punya ruang, ruang perkuliahan itu DISALIN ke field pertemuan
	 * (sehingga ikut tersimpan). Artinya pertemuan mewarisi ruang kelas kuliahnya, kecuali memang
	 * dipindah ke ruang lain.</p>
	 *
	 * @return ruang pertemuan, atau {@code null} bila tidak ada ruang di pertemuan maupun induknya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		perkuliahan = getPerkuliahan();
		if (perkuliahan != null && ruang == null && perkuliahan.getRuang() != null) {
			ruang = perkuliahan.getRuang();
		}
		ruang = check(ruang);
		return ruang;
	}

	/**
	 * Setel metode pembelajaran yang dipakai pada pertemuan ini, dengan pemotongan pengaman.
	 *
	 * <p>Kolom {@code metode_pembelajaran} masih {@code varchar(255)} di sebagian tenant, sedangkan
	 * data OBE dapat menggabungkan beberapa metode sekaligus sehingga melampaui batas itu dan
	 * MENGGAGALKAN seluruh {@code UPDATE} baris pertemuan. Karena itu pemotongan ke 255 karakter
	 * dipasang persis di batas model, agar semua jalur simpan ikut terlindungi tanpa perlu
	 * mengubah tiap pemanggil.</p>
	 *
	 * <p>Konsekuensinya: nilai yang terlalu panjang DIPOTONG diam-diam, tanpa peringatan.</p>
	 *
	 * @param metodePembelajaran metode pembelajaran; dipotong bila lebih dari 255 karakter
	 */
	public void setMetodePembelajaran(String metodePembelajaran) {
		// Kolom lama di sebagian tenant masih varchar(255). Data OBE dapat terbentuk
		// dari gabungan beberapa metode sehingga lebih panjang dan menggagalkan seluruh
		// update Pertemuan. Batasi tepat di boundary model agar semua jalur simpan aman.
		this.metodePembelajaran = metodePembelajaran != null && metodePembelajaran.length() > 255
				? metodePembelajaran.substring(0, 255) : metodePembelajaran;
	}

	/**
	 * Metode pembelajaran yang dipakai pada pertemuan ini (mis. ceramah, diskusi, praktikum).
	 *
	 * @return metode pembelajaran yang sudah di-{@code trim}; string kosong bila belum diisi
	 * @see #setMetodePembelajaran(String)
	 */
	@Column(name = "metode_pembelajaran", length = 255)
	public String getMetodePembelajaran() {
		return metodePembelajaran == null ? "" : metodePembelajaran.trim();
	}

	/**
	 * Setel nama dosen tamu kedua.
	 *
	 * @param dosenTamu2 nama dosen tamu kedua
	 */
	public void setDosenTamu2(String dosenTamu2) {
		this.dosenTamu2 = dosenTamu2;
	}

	/**
	 * Nama dosen tamu kedua (teks bebas, bukan relasi ke {@link Dosen}).
	 *
	 * @return nama dosen tamu kedua, atau {@code null}
	 * @see #getDosenTamu()
	 */
	@Column(name = "dosen_tamu_2", length = 255)
	public String getDosenTamu2() {
		return dosenTamu2;
	}

	/**
	 * Setel catatan bebas pertemuan (disimpan apa adanya; pembersihan terjadi saat dibaca).
	 *
	 * @param catatan teks catatan
	 * @see #getCatatan()
	 */
	public void setCatatan(String catatan) {
		this.catatan = catatan;
	}

	/**
	 * Catatan bebas mengenai pertemuan ini.
	 *
	 * <p><b>Getter ini mengubah keadaan objek</b>: {@code null} diganti string kosong, lalu isinya
	 * dilewatkan {@code filterTidakBoleh(...)} dan hasil bersihnya ditulis balik ke field.</p>
	 *
	 * @return catatan yang sudah dibersihkan dan di-{@code trim}; tidak pernah {@code null}
	 */
	@Column(name = "catatan", columnDefinition = "text")
	public String getCatatan() {
		if (catatan == null) {
			catatan = "";
		}

		catatan = filterTidakBoleh(catatan);
		return this.catatan.trim();
	}

	/**
	 * Setel isi/uraian tugas yang melekat pada pertemuan ini.
	 *
	 * @param isitugas uraian tugas
	 * @see Tugas
	 */
	public void setIsitugas(String isitugas) {
		this.isitugas = isitugas;
	}

	/**
	 * Isi/uraian tugas yang melekat pada pertemuan ini.
	 *
	 * <p>Bagian dari kontrak {@link Tugas}: satu Pertemuan sekaligus dapat berperan sebagai tugas
	 * yang dikumpulkan peserta. Tugas dianggap "ada" bila {@link #getJudultugas()} tidak kosong.</p>
	 *
	 * @return uraian tugas, atau {@code null} bila pertemuan ini bukan tugas
	 * @see #getJudultugas()
	 */
	@Column(name = "isitugas", columnDefinition = "text")
	public String getIsitugas() {
		return isitugas;
	}

	/**
	 * Cap waktu mulai — dipakai terutama pada jalur tugas dan tampilan kalender.
	 *
	 * <p><b>Getter ini mengubah keadaan objek</b> dengan dua aturan yang mudah membingungkan:</p>
	 * <ol>
	 *   <li>Bila {@code mulai} kosong DAN pertemuan ini bukan tugas (judul tugas kosong),
	 *       {@code mulai} diisi dari {@code tanggal} dan tersimpan ke field. Perhatikan bahwa yang
	 *       dibaca adalah field {@code tanggal} mentah, BUKAN {@link #getTanggal()}, sehingga
	 *       penimpaan oleh {@code tanggalEdit} tidak berlaku di sini.</li>
	 *   <li>Bila setelah itu {@code mulai} masih kosong, yang DIKEMBALIKAN adalah waktu sekarang —
	 *       tetapi nilai itu tidak disimpan ke field. Akibatnya method ini tidak pernah
	 *       mengembalikan {@code null} dan hasilnya bisa berbeda pada dua pemanggilan berturut-turut
	 *       untuk objek yang sama.</li>
	 * </ol>
	 *
	 * @return cap waktu mulai; tidak pernah {@code null}
	 * @see #getSelesai()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getMulai() {
		if (mulai == null && getJudultugas().trim().isEmpty()) {
			mulai = tanggal;
		}

		return mulai == null ? WaktuUtil.getDate() : mulai;
	}

	/**
	 * Setel cap waktu mulai.
	 *
	 * @param mulai cap waktu mulai; boleh {@code null}
	 * @see #getMulai()
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Cap waktu selesai. Berbeda dari {@link #getMulai()}, getter ini mengembalikan nilai apa
	 * adanya tanpa nilai pengganti apa pun.
	 *
	 * @return cap waktu selesai, atau {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSelesai() {
		return selesai;
	}

	/**
	 * Setel cap waktu selesai.
	 *
	 * @param selesai cap waktu selesai; boleh {@code null}
	 */
	public void setSelesai(Date selesai) {
		this.selesai = selesai;
	}

	/**
	 * Jadwal ujian penerimaan mahasiswa baru yang menjadi induk pertemuan ini.
	 *
	 * @return {@link JadwalUjianPMB} induk, atau {@code null} bila pertemuan ini bukan sesi ujian PMB
	 * @see #untuk()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_ujian_pmb", nullable = true)
	public JadwalUjianPMB getJadwalUjianPMB() {
		jadwalUjianPMB = check(jadwalUjianPMB);
		return jadwalUjianPMB;
	}

	/**
	 * Tetapkan jadwal ujian PMB sebagai induk pertemuan ini.
	 *
	 * @param jadwalUjianPMB jadwal ujian PMB; boleh {@code null}
	 */
	public void setJadwalUjianPMB(JadwalUjianPMB jadwalUjianPMB) {
		this.jadwalUjianPMB = jadwalUjianPMB;
	}

	/**
	 * Pengajuan bimbingan tugas akhir yang menjadi induk pertemuan ini (sesi bimbingan).
	 *
	 * @return {@link MahasiswaRequestTugasAkhir} induk, atau {@code null}
	 * @see #untuk()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa_request_tugas_akhir", nullable = true)
	public MahasiswaRequestTugasAkhir getMahasiswaRequestTugasAkhir() {
		mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
		return mahasiswaRequestTugasAkhir;
	}

	/**
	 * Tetapkan pengajuan bimbingan tugas akhir sebagai induk pertemuan ini.
	 *
	 * @param mahasiswaRequestTugasAkhir pengajuan bimbingan; boleh {@code null}
	 */
	public void setMahasiswaRequestTugasAkhir(MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir) {
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
	}

	/**
	 * Skripsi yang menjadi induk pertemuan ini (sesi sidang atau revisi).
	 *
	 * @return {@link Skripsi} induk, atau {@code null}
	 * @see #untuk()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "skripsi", nullable = true)
	public Skripsi getSkripsi() {
		skripsi = check(skripsi);
		return skripsi;
	}

	/**
	 * Tetapkan skripsi sebagai induk pertemuan ini.
	 *
	 * @param skripsi skripsi; boleh {@code null}
	 */
	public void setSkripsi(Skripsi skripsi) {
		this.skripsi = skripsi;
	}

	/**
	 * Id pertemuan asal, bila pertemuan ini dibuat dengan cara menyalin pertemuan lain.
	 *
	 * <p>Disimpan sebagai id mentah, bukan relasi, jadi tidak ada jaminan referensial: pertemuan
	 * asalnya bisa saja sudah dihapus.</p>
	 *
	 * @return id pertemuan asal, atau {@code null} bila bukan hasil salinan
	 */
	public Long getCopyDariPertemuan() {
		return copyDariPertemuan;
	}

	/**
	 * Catat id pertemuan asal yang disalin menjadi pertemuan ini.
	 *
	 * @param copyDariPertemuan id pertemuan asal
	 */
	public void setCopyDariPertemuan(Long copyDariPertemuan) {
		this.copyDariPertemuan = copyDariPertemuan;
	}

	/**
	 * Kelompok KKN yang menjadi induk pertemuan ini.
	 *
	 * <p>Getter ini penting dipakai (alih-alih membaca field langsung): pada perbaikan KE-20
	 * ditemukan bahwa memakai proxy {@code kelompokKkn} yang basi tanpa session aktif menyebabkan
	 * {@code LazyInitializationException}. {@code check(...)} di sini memastikan proxy tersegarkan.</p>
	 *
	 * @return {@link KelompokKkn} induk, atau {@code null}
	 * @see #ambilDosen()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kkn", nullable = true)
	public KelompokKkn getKelompokKkn() {
		kelompokKkn = check(kelompokKkn);
		return kelompokKkn;
	}

	/**
	 * Tetapkan kelompok KKN sebagai induk pertemuan ini.
	 *
	 * @param kelompokKkn kelompok KKN; boleh {@code null}
	 */
	public void setKelompokKkn(KelompokKkn kelompokKkn) {
		this.kelompokKkn = kelompokKkn;
	}

	/**
	 * Kelompok PKL yang menjadi induk pertemuan ini.
	 *
	 * @return {@link KelompokPkl} induk, atau {@code null}
	 * @see #untuk()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_pkl", nullable = true)
	public KelompokPkl getKelompokPkl() {
		kelompokPkl = check(kelompokPkl);
		return kelompokPkl;
	}

	/**
	 * Tetapkan kelompok PKL sebagai induk pertemuan ini.
	 *
	 * @param kelompokPkl kelompok PKL; boleh {@code null}
	 */
	public void setKelompokPkl(KelompokPkl kelompokPkl) {
		this.kelompokPkl = kelompokPkl;
	}

	/**
	 * Penanda pertemuan "mandiri" (belajar mandiri, tanpa pengajar hadir).
	 *
	 * <p>Getter ini menulis nilai bawaan {@code false} ke field bila masih {@code null}, sehingga
	 * ikut tersimpan pada flush berikutnya.</p>
	 *
	 * @return {@code true} bila pertemuan bersifat mandiri; tidak pernah {@code null}
	 */
	public Boolean getMandiri() {
		if (mandiri == null) {
			mandiri = false;
		}
		return mandiri;
	}

	/**
	 * Setel penanda pertemuan mandiri.
	 *
	 * @param mandiri {@code true} bila pertemuan bersifat mandiri
	 */
	public void setMandiri(Boolean mandiri) {
		this.mandiri = mandiri;
	}

	/**
	 * Judul tugas yang melekat pada pertemuan ini.
	 *
	 * <p>Judul yang TIDAK kosong adalah penanda bahwa pertemuan ini sekaligus berperan sebagai
	 * {@link Tugas} — beberapa tempat memakainya persis begitu, mis. {@link #getMulai()} dan
	 * penyaring pada {@link #reInitTugasPertemuan(Session)}.</p>
	 *
	 * @return judul tugas yang sudah di-{@code trim}; string kosong bila pertemuan ini bukan tugas
	 * @see #getIsitugas()
	 */
	public String getJudultugas() {
		return judultugas == null ? "" : judultugas.trim();
	}

	/**
	 * Setel judul tugas pertemuan ini.
	 *
	 * @param judultugas judul tugas; kosongkan agar pertemuan tidak dianggap tugas
	 */
	public void setJudultugas(String judultugas) {
		this.judultugas = judultugas;
	}

	/**
	 * Indikator capaian pembelajaran pertemuan (isian RPS).
	 *
	 * <p><b>Getter ini mengubah keadaan objek dan dapat MENULIS ke basis data.</b> Bila indikator
	 * belum diisi, teks bawaan diambil dari konfigurasi
	 * {@code default_indikator_pembelajaran} lewat {@code Common.getKonfigurasi(...)} — dan
	 * {@code getKonfigurasi} akan MENYIMPAN nilai bawaan itu ke tabel {@link Konfigurasi} bila
	 * kuncinya belum ada. Jadi sekadar membaca indikator sebuah pertemuan baru dapat menimbulkan
	 * baris konfigurasi baru.</p>
	 *
	 * <p>Selain itu, untuk pertemuan sekolah (induknya
	 * {@link ais.database.model.sekolah.JadwalPelajaran}), kata "Mahasiswa"/"mahasiswa" pada teks
	 * indikator diganti menjadi "Siswa"/"siswa" agar peristilahannya cocok.</p>
	 *
	 * @return teks indikator; praktis tidak pernah {@code null} karena selalu ada nilai bawaan
	 * @see Konfigurasi
	 */
	@Column(columnDefinition = "text")
	public String getIndikator() {
		indikator = indikator == null ? Common
				.getKonfigurasi("default_indikator_pembelajaran", "Mahasiswa mampu menjelaskan dan mendiskusikan ....")
				.getNilai() : indikator.trim();

		if (indikator != null && getJadwalPelajaran() != null) {
			indikator = org.apache.commons.lang3.StringUtils.replace(indikator, "Mahasiswa", "Siswa");
			indikator = org.apache.commons.lang3.StringUtils.replace(indikator, "mahasiswa", "siswa");
		}

		return indikator;
	}

	/**
	 * Setel indikator capaian pembelajaran.
	 *
	 * @param indikator teks indikator; {@code null} berarti pakai teks bawaan saat dibaca
	 * @see #getIndikator()
	 */
	public void setIndikator(String indikator) {
		this.indikator = indikator;
	}

	/**
	 * Alokasi waktu pembelajaran sebagai teks bebas (mis. {@code "3 x 50 menit"}).
	 *
	 * <p>Bila belum diisi, nilainya dihitung: untuk pertemuan perkuliahan dirangkai dari jumlah SKS
	 * mata kuliah ({@code sks + " x 50 menit"}), selain itu diambil dari konfigurasi
	 * {@code default_waktu_pembelajaran}. Sama seperti {@link #getIndikator()}, pembacaan
	 * konfigurasi ini DAPAT MENULIS baris konfigurasi baru bila kuncinya belum ada.</p>
	 *
	 * <p><b>Perhatian:</b> cabang perkuliahan membaca field {@code perkuliahan} secara LANGSUNG,
	 * bukan lewat {@link #getPerkuliahan()}. Jadi bila proxy belum tersegarkan, nilai bawaannya
	 * jatuh ke konfigurasi global walaupun pertemuan ini sebenarnya milik sebuah perkuliahan.</p>
	 *
	 * @return teks alokasi waktu; praktis tidak pernah {@code null}
	 */
	public String getWaktupembelajaran() {
		return waktupembelajaran == null
				? (perkuliahan != null ? perkuliahan.getMatakuliah().getSks() + " x 50 menit"
						: Common.getKonfigurasi("default_waktu_pembelajaran", "... x 50 menit").getNilai())
				: waktupembelajaran.trim();
	}

	/**
	 * Setel alokasi waktu pembelajaran.
	 *
	 * @param waktupembelajaran teks alokasi waktu; {@code null} berarti pakai nilai hitungan/bawaan
	 * @see #getWaktupembelajaran()
	 */
	public void setWaktupembelajaran(String waktupembelajaran) {
		this.waktupembelajaran = waktupembelajaran;
	}

	/**
	 * Pengalaman belajar yang direncanakan pada pertemuan ini (isian RPS).
	 *
	 * <p>Bila belum diisi, teks bawaan diambil dari konfigurasi
	 * {@code default_pengalaman_belajar} — dengan efek samping penulisan konfigurasi yang sama
	 * seperti dijelaskan pada {@link #getIndikator()}.</p>
	 *
	 * @return teks pengalaman belajar; praktis tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getPengalamanBelajar() {
		return pengalamanBelajar == null ? Common
				.getKonfigurasi("default_pengalaman_belajar", "Menyimak, Mengamati, Mendiskusikan, dan Menjawab soal")
				.getNilai() : pengalamanBelajar.trim();
	}

	/**
	 * Setel pengalaman belajar yang direncanakan.
	 *
	 * @param pengalamanBelajar teks pengalaman belajar
	 * @see #getPengalamanBelajar()
	 */
	public void setPengalamanBelajar(String pengalamanBelajar) {
		this.pengalamanBelajar = pengalamanBelajar;
	}

	/**
	 * Rencana tugas dan kriteria penilaian pertemuan ini (isian RPS).
	 *
	 * <p>Bila belum diisi, teks bawaan diambil dari konfigurasi
	 * {@code default_tugas_dan_penilaian} — dengan efek samping penulisan konfigurasi yang sama
	 * seperti dijelaskan pada {@link #getIndikator()}.</p>
	 *
	 * @return teks tugas dan penilaian; praktis tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getTugasDanPenilaian() {
		return tugasDanPenilaian == null
				? Common.getKonfigurasi("default_tugas_dan_penilaian",
						"Ketepatan menjelaskan...., Ketepatan menyebutkan..., dan lain sebagainya").getNilai()
				: tugasDanPenilaian.trim();
	}

	/**
	 * Setel rencana tugas dan kriteria penilaian.
	 *
	 * @param tugasDanPenilaian teks tugas dan penilaian
	 * @see #getTugasDanPenilaian()
	 */
	public void setTugasDanPenilaian(String tugasDanPenilaian) {
		this.tugasDanPenilaian = tugasDanPenilaian;
	}

	/**
	 * Seluruh daftar hadir pertemuan ini sebagai SATU string berformat khusus.
	 *
	 * <p>Format sembilan slot per baris (dipisah {@code ';'} antar baris dan {@code ','} antar
	 * slot) dijelaskan lengkap pada Javadoc kelas {@link Pertemuan}. Jangan mengurai string ini
	 * sendiri di kode pemanggil — pakai keluarga {@code retreiveAbsensiXxx(Long)} dan
	 * {@code hitungStatusXxx(...)} yang sudah menangani baris rusak/tidak lengkap.</p>
	 *
	 * <p><b>Getter ini mengubah keadaan objek</b> untuk satu kasus perbaikan data yang sangat
	 * spesifik: kemunculan teks {@code "9.400"} diganti {@code "09.40"} dan hasilnya ditulis balik
	 * ke field. Ini menambal jam yang pernah tersimpan salah format oleh versi lama.</p>
	 *
	 * @return string absensi mentah yang sudah di-{@code trim}; string kosong bila belum ada
	 *         kehadiran tercatat
	 * @see #populate(Long, Statusabsensi, String, String, String)
	 */
	@Column(name = "absensi", columnDefinition = "text")
	public String getAbsensi() {
		if (absensi != null && StringUtils.contains(absensi, "9.400")) {
			absensi = org.apache.commons.lang3.StringUtils.replace(absensi, "9.400", "09.40");
		}
		return absensi == null ? "" : absensi.trim();
	}

	/**
	 * Timpa SELURUH string absensi pertemuan ini.
	 *
	 * <p><b>Berbahaya bila dipakai sembarangan:</b> string yang diberikan menggantikan seluruh
	 * daftar hadir, bukan menambah satu baris. Untuk mencatat/mengubah kehadiran satu orang, pakai
	 * {@link #populate(Long, Statusabsensi, String, String, String)} yang menjaga baris peserta
	 * lain tetap utuh.</p>
	 *
	 * @param absensi string absensi berformat sembilan slot (lihat Javadoc kelas)
	 */
	public void setAbsensi(String absensi) {
		this.absensi = absensi;
	}

	/**
	 * Nama jenis sesi pertemuan yang aman disisipkan ke teks notifikasi.
	 *
	 * <p>Mengembalikan string kosong (bukan {@code "null"}) bila jenis sesi atau namanya belum ada,
	 * supaya kalimat notifikasi tidak mengandung kata "null".</p>
	 *
	 * @return nama jenis sesi, atau string kosong
	 * @see #getStatusPertemuan()
	 */
	private String namaStatusPertemuanUntukNotifikasi() {
		StatusPertemuan status = getStatusPertemuan();
		return status == null || status.getNama() == null ? "" : status.getNama();
	}

	/**
	 * Catat/ubah kehadiran satu orang pada pertemuan ini — bentuk ringkas tanpa keterangan dan
	 * tanpa tautan pengajuan izin.
	 *
	 * <p>Meneruskan ke {@link #populate(Long, Statusabsensi, String,
	 * PengajuanIzinTidakMasukPerkuliahan, String, String, String)} dengan {@code keterangan} dan
	 * {@code pengajuanIzinTidakMasukPerkuliahan} bernilai {@code null}. Perhatikan bahwa
	 * {@code null} di sana TIDAK berarti "kosongkan", melainkan "pertahankan nilai yang sudah ada"
	 * — lihat Javadoc method lengkapnya.</p>
	 *
	 * @param ref           id orang yang kehadirannya dicatat (Mahasiswa/Siswa/Dosen/Guru/Pegawai)
	 * @param statusabsensi status kehadiran yang ditetapkan
	 * @param mulai         jam mulai kehadiran (hanya berarti bila kode status {@code "M"})
	 * @param sampai        jam selesai kehadiran (hanya berarti bila kode status {@code "M"})
	 * @param jenis         jenis orang: {@code "Mahasiswa"}, {@code "Siswa"}, {@code "Dosen"},
	 *                      {@code "Guru"}, atau {@code "Pegawai"}
	 * @see #populate(Long, Statusabsensi, String, PengajuanIzinTidakMasukPerkuliahan, String,
	 *      String, String)
	 */
	public void populate(Long ref, Statusabsensi statusabsensi, String mulai, String sampai, String jenis) {
		populate(ref, statusabsensi, null, null, mulai, sampai, jenis);
	}

	/**
	 * Catat/ubah kehadiran satu orang pada pertemuan ini, dan (bila hadir) kirim notifikasi
	 * ucapan selamat kepadanya.
	 *
	 * <p>Ini adalah <b>satu-satunya</b> jalan yang benar untuk menyentuh daftar hadir: method ini
	 * merakit ulang seluruh string {@code absensi} sambil menjaga baris peserta lain tetap utuh.
	 * Jangan memanggil {@link #setAbsensi(String)} langsung.</p>
	 *
	 * <h4>Apa yang dilakukan</h4>
	 * <ol>
	 *   <li>Bila {@code ref} atau {@code statusabsensi} {@code null}, method langsung berhenti
	 *       tanpa mengubah apa pun dan tanpa memberi tahu pemanggil.</li>
	 *   <li>Bila kode status BUKAN {@code "M"} (tidak hadir), {@code mulai} dan {@code sampai}
	 *       dipaksa menjadi string kosong — jam kehadiran hanya berarti untuk orang yang hadir.</li>
	 *   <li>Bila kode status {@code "M"}, satu <b>thread baru</b> dijalankan untuk menyusun dan
	 *       menyimpan notifikasi (lihat bagian di bawah).</li>
	 *   <li>Karakter {@code ';'} pada {@code keterangan} diganti {@code "..\n"} dan {@code ','}
	 *       diganti {@code '_'}, karena keduanya adalah pemisah pada format penyimpanan. Teks
	 *       aslinya TIDAK dapat dipulihkan.</li>
	 *   <li>String {@code absensi} lama dipecah per baris. Baris milik {@code ref} ditulis ulang
	 *       dengan nilai baru; baris lain disalin apa adanya. Bila {@code ref} belum punya baris,
	 *       satu baris baru ditambahkan di akhir.</li>
	 *   <li>Hasil rakitan ditulis ke field {@code absensi} secara LANGSUNG (bukan lewat setter),
	 *       sehingga tersimpan pada flush berikutnya.</li>
	 * </ol>
	 *
	 * <h4>Arti {@code null} pada parameter opsional</h4>
	 * <p>Untuk {@code keterangan}, {@code pengajuanIzinTidakMasukPerkuliahan}, {@code mulai},
	 * {@code sampai}, dan {@code jenis}, nilai {@code null} berarti <b>"pertahankan nilai yang
	 * sudah tersimpan"</b> — nilai lamanya dibaca kembali lewat
	 * {@link #retreiveAbsensiKeterangan(Long)}, {@link #retreivePengajuanIzinId(Long)},
	 * {@link #retreiveAbsensiMulai(Long)}, {@link #retreiveAbsensiSampai(Long)}, dan
	 * {@link #retreiveAbsensiJenis(Long)}. Untuk benar-benar mengosongkan sebuah slot, kirim string
	 * kosong, bukan {@code null}.</p>
	 *
	 * <h4>Notifikasi berjalan di thread terpisah</h4>
	 * <p>Ketika kehadiran dicatat sebagai hadir, sebuah {@link Thread} baru merangkai kalimat
	 * ucapan selamat yang berbeda-beda untuk Siswa, Mahasiswa, Guru, Dosen, dan Pegawai (lengkap
	 * dengan sapaan "Pagi/Siang/Sore/Malam" menurut jam server), lalu menyimpannya lewat
	 * {@code MailSender.simpanNotif(...)}. Thread ini membuka session Hibernate sendiri dan
	 * menutupnya di blok {@code finally}. Konsekuensi yang perlu disadari:</p>
	 * <ul>
	 *   <li>kegagalan notifikasi TIDAK memengaruhi pencatatan kehadiran, dan sebaliknya pemanggil
	 *       tidak akan pernah tahu bila notifikasi gagal;</li>
	 *   <li>thread dibuat langsung ({@code new Thread(...).start()}), tanpa kolam thread — pencatatan
	 *       kehadiran massal berarti membuat sangat banyak thread sekaligus;</li>
	 *   <li>thread mengakses {@code Pertemuan.this} dari luar session pemanggil, sehingga nilai
	 *       yang dibacanya bisa berbeda dari yang dilihat pemanggil;</li>
	 *   <li>bila {@code jenis} kosong, thread langsung berhenti sehingga tidak ada notifikasi.</li>
	 * </ul>
	 *
	 * <p><b>Kejanggalan yang ditemukan (tidak diperbaiki, hanya dicatat):</b> pada cabang Dosen,
	 * nama kelas ditulis sebagai teks tetap {@code " kelas B1-HK"} alih-alih memakai variabel
	 * {@code kls} yang sudah dihitung tepat di atasnya — tampaknya sisa data uji coba yang
	 * tertinggal.</p>
	 *
	 * @param ref                                 id orang yang kehadirannya dicatat
	 * @param statusabsensi                       status kehadiran yang ditetapkan; {@code null}
	 *                                            membatalkan seluruh operasi
	 * @param keterangan                          keterangan bebas; {@code null} berarti pertahankan
	 *                                            nilai lama
	 * @param pengajuanIzinTidakMasukPerkuliahan  pengajuan izin terkait; {@code null} berarti
	 *                                            pertahankan nilai lama
	 * @param mulai                               jam mulai kehadiran; {@code null} berarti
	 *                                            pertahankan nilai lama
	 * @param sampai                              jam selesai kehadiran; {@code null} berarti
	 *                                            pertahankan nilai lama
	 * @param jenis                               {@code "Mahasiswa"}/{@code "Siswa"}/{@code "Dosen"}/
	 *                                            {@code "Guru"}/{@code "Pegawai"}; {@code null}
	 *                                            berarti pertahankan nilai lama
	 * @see #getAbsensi()
	 * @see ais.action.master.helper.AbsensiHelper
	 */
	public void populate(final Long ref, Statusabsensi statusabsensi, String keterangan,
			PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan, String mulai, String sampai,
			final String jenis) {
		if (ref != null && statusabsensi != null) {

			if (statusabsensi.getKode() == null || !statusabsensi.getKode().equals("M")) {
				mulai = "";
				sampai = "";
			}

			if (statusabsensi.getKode() != null && statusabsensi.getKode().equals("M")) {

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {
							if (jenis == null || jenis.trim().length() == 0) {
								return;
							}

						String waktu = "";
						int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
						if (jam >= 10 && jam < 15) {
							waktu = "Siang";
						} else if (jam >= 15 && jam < 18) {
							waktu = "Sore";
						} else if (jam >= 18 && jam <= 24) {
							waktu = "Malam";
						} else {
							waktu = "Pagi";
						}

						String ket = VOPembelajaran.infoSimple(Pertemuan.this);

						if (jenis.equalsIgnoreCase("Siswa")) {
							Siswa siswa = (Siswa) ConstantValues.ambil(Siswa.class.getName(), ref);
							if (siswa != null) {
								JSONArray userIds = new JSONArray();
								String ninSiswa = siswa.getNomorIndukNasional();
								userIds.put(ninSiswa == null || ninSiswa.isEmpty() ? siswa.getNomorInduk()
										: ninSiswa);
								String recipientsTemp = null;

								Integer smt = getJadwalPelajaran() != null ? getJadwalPelajaran().getSemester() : null;
								String ta = getJadwalPelajaran() != null ? getJadwalPelajaran().getTahunAjaran() : null;
								String kls = getJadwalPelajaran() == null ? null
										: (getJadwalPelajaran().getKelas() != null
												? getJadwalPelajaran().getKelas().getNama()
												: (getJadwalPelajaran().getKelasLesSiswa() != null
														? getJadwalPelajaran().getKelasLesSiswa().getNama()
														: ""));

								ket = "Selamat " + waktu + " siswa/i "
										+ (siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama())
										+ ". Kami ingin mengucapkan selamat kepada siswa atas nama " + siswa.getNama()
										+ " yang telah berhasil melakukan absen kehadiran pada pertemuan ke-"
										+ pertemuanKe + ", hari "
										+ (getTanggal() == null ? "" : Common.dateFormat6.get().format(getTanggal()))
										+ ", pukul " + (getWaktuMulai() == null ? "" : " " + getWaktuMulai()) + " "
										+ (getWaktuSelesai() == null ? "" : " hingga " + getWaktuSelesai() + " ")
										+ ", pada " + info() + (ta == null ? "" : " tahun pelajaran " + ta)
										+ (smt == null ? "" : " semester " + (smt.equals(1) ? "ganjil" : "genap"))
										+ (kls == null ? "" : " kelas " + kls) + " sesi "
										+ namaStatusPertemuanUntukNotifikasi()
										+ ". Kehadiran Anda sangat kami hargai dan semoga Anda selalu bersemangat dalam mengikuti "
										+ (jadwalPelajaran == null ? " kegiatan ini" : " mata pelajaran ini")
										+ ". Terima kasih.";

								MailSender.simpanNotif(userIds, recipientsTemp, "Info kehadiran siswa pada " + info(),
										ket, Pertemuan.this);

							}
						} else if (jenis.equalsIgnoreCase("Mahasiswa")) {
							Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.ambil(Mahasiswa.class.getName(), ref);
							if (mahasiswa != null) {
								JSONArray userIds = new JSONArray();
								userIds.put(mahasiswa.getNim());
								String recipientsTemp = null;

								Integer smt = getPerkuliahan() == null ? null : getPerkuliahan().getSemester();
								String ta = getPerkuliahan() == null ? null : getPerkuliahan().getTahunAjaran();
								String kls = getPerkuliahan() == null ? null : getPerkuliahan().getKelas();

								ket = "Selamat " + waktu + " mahasiswa/i "
										+ (mahasiswa.getJurusan() == null
												|| mahasiswa.getJurusan().getFakultas() == null
												|| mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null
														? ""
														: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi()
																.getNama())
										+ ". Kami ingin mengucapkan selamat kepada mahasiswa atas nama "
										+ mahasiswa.getNama() + " dengan NIM " + mahasiswa.getNim()
										+ " yang telah berhasil melakukan absen kehadiran pada pertemuan ke-"
										+ pertemuanKe + ", hari "
										+ (getTanggal() == null ? "" : Common.dateFormat6.get().format(getTanggal()))
										+ ", pukul " + (getWaktuMulai() == null ? "" : " " + getWaktuMulai()) + " "
										+ (getWaktuSelesai() == null ? "" : " hingga " + getWaktuSelesai() + " ")
										+ ", pada " + info() + (ta == null ? "" : " tahun akademik " + ta)
										+ (smt == null ? "" : " semester " + smt) + (kls == null ? "" : " kelas " + kls)
										+ " sesi " + namaStatusPertemuanUntukNotifikasi()
										+ ". Kehadiran Anda sangat kami hargai dan semoga Anda selalu bersemangat dalam mengikuti "
										+ (perkuliahan == null ? " kegiatan ini" : "perkuliahan") + ". Terima kasih.";

								MailSender.simpanNotif(userIds, recipientsTemp,
										"Info kehadiran mahasiswa pada " + info(), ket, Pertemuan.this);
							}
						} else if (jenis.equalsIgnoreCase("Guru")) {
							List<String> usernames = ambilUsernamePemilik("guru", ref);
							if (!usernames.isEmpty()) {
								JSONArray userIds = new JSONArray();
								for (String s : usernames) {
									userIds.put(s);
								}
								String recipientsTemp = null;
								Guru guru = (Guru) ConstantValues.ambil(Guru.class.getName(), ref);
								if (guru != null) {
									Integer smt = getJadwalPelajaran() == null ? null : getJadwalPelajaran().getSemester();
									String ta = getJadwalPelajaran() == null ? null : getJadwalPelajaran().getTahunAjaran();
									String kls = getJadwalPelajaran() == null ? null
											: (getJadwalPelajaran().getKelas() != null
													? getJadwalPelajaran().getKelas().getNama()
													: (getJadwalPelajaran().getKelasLesSiswa() != null
															? getJadwalPelajaran().getKelasLesSiswa().getNama()
															: ""));

									ket = "Selamat " + waktu + " bapak/ibu guru "
											+ (guru.getSekolah() == null ? "" : guru.getSekolah().getNama())
											+ ". Kami ingin mengucapkan selamat kepada guru atas nama " + guru.getNama()
											+ " yang telah berhasil melakukan absen kehadiran pada pertemuan ke-"
											+ pertemuanKe + ", hari "
											+ (getTanggal() == null ? ""
													: Common.dateFormat6.get().format(getTanggal()))
											+ ", pukul " + (getWaktuMulai() == null ? "" : " " + getWaktuMulai()) + " "
											+ (getWaktuSelesai() == null ? "" : " hingga " + getWaktuSelesai() + " ")
											+ ", pada " + info() + (ta == null ? "" : " tahun pelajaran " + ta)
											+ (smt == null ? "" : " semester " + (smt.equals(1) ? "ganjil" : "genap"))
											+ (kls == null ? "" : " kelas " + kls) + " sesi "
											+ namaStatusPertemuanUntukNotifikasi()
											+ ". Kehadiran Anda sangat kami hargai dan semoga Anda selalu bersemangat dalam mengajar "
											+ (jadwalPelajaran == null ? " kegiatan ini" : " mata pelajaran ini")
											+ ". Terima kasih.";

									MailSender.simpanNotif(userIds, recipientsTemp,
											"Info kehadiran guru pada " + info(), ket, Pertemuan.this);

								}
							}
						} else if (jenis.equalsIgnoreCase("Dosen")) {
							List<String> usernames = ambilUsernamePemilik("dosen", ref);
							if (!usernames.isEmpty()) {
								JSONArray userIds = new JSONArray();
								for (String s : usernames) {
									userIds.put(s);
								}
								String recipientsTemp = null;

								Dosen dosen = (Dosen) ConstantValues.ambil(Dosen.class.getName(), ref);
								if (dosen != null) {
									Integer smt = getPerkuliahan() == null ? null : getPerkuliahan().getSemester();
									String ta = getPerkuliahan() == null ? null : getPerkuliahan().getTahunAjaran();
									String kls = getPerkuliahan() == null ? null : getPerkuliahan().getKelas();

									ket = "Selamat " + waktu + " bapak/ibu dosen "
											+ (dosen.getPerguruanTinggi() == null ? ""
													: dosen.getPerguruanTinggi().getNama())
											+ ". Kami ingin mengucapkan selamat kepada dosen atas nama "
											+ dosen.getNama() + " dengan NIDN " + dosen.getNidn()
											+ " yang telah berhasil melakukan absen kehadiran pada pertemuan ke-"
											+ pertemuanKe + ", hari "
											+ (getTanggal() == null ? ""
													: Common.dateFormat6.get().format(getTanggal()))
											+ ", pukul " + (getWaktuMulai() == null ? "" : " " + getWaktuMulai()) + " "
											+ (getWaktuSelesai() == null ? "" : " hingga " + getWaktuSelesai() + " ")
											+ ", pada " + info() + (ta == null ? "" : " tahun akademik " + ta)
											+ (smt == null ? "" : " semester " + smt)
											+ (kls == null ? "" : " kelas B1-HK") + " sesi "
											+ namaStatusPertemuanUntukNotifikasi()
											+ ". Kehadiran Anda sangat kami hargai dan semoga Anda selalu bersemangat dalam mengajar "
											+ (perkuliahan == null ? " kegiatan ini" : "perkuliahan ini")
											+ ". Terima kasih.";

									MailSender.simpanNotif(userIds, recipientsTemp,
											"Info kehadiran dosen pada " + info(), ket, Pertemuan.this);

								}

							}
						} else if (jenis.equalsIgnoreCase("Pegawai")) {
							// Fitur Voucher Pegawai (kehadiran Kajian) -- mengikuti persis pola cabang Guru/Dosen
							// di atas, disederhanakan (tanpa konteks jadwalPelajaran/kelas akademik yang tidak
							// relevan untuk kehadiran Kajian/kegiatan non-akademik).
							List<String> usernames = ambilUsernamePemilik("pegawai", ref);
							if (!usernames.isEmpty()) {
								JSONArray userIds = new JSONArray();
								for (String s : usernames) {
									userIds.put(s);
								}
								String recipientsTemp = null;

								Pegawai pegawai = (Pegawai) ConstantValues.ambil(Pegawai.class.getName(), ref);
								if (pegawai != null) {
									ket = "Selamat " + waktu + " Bapak/Ibu " + pegawai.getNama()
											+ ". Kami ingin mengucapkan selamat, kehadiran Anda pada pertemuan ke-"
											+ pertemuanKe + ", hari "
											+ (getTanggal() == null ? ""
													: Common.dateFormat6.get().format(getTanggal()))
											+ ", pukul " + (getWaktuMulai() == null ? "" : " " + getWaktuMulai()) + " "
											+ (getWaktuSelesai() == null ? "" : " hingga " + getWaktuSelesai() + " ")
											+ ", pada " + info() + " sesi " + namaStatusPertemuanUntukNotifikasi()
											+ " telah tercatat. Terima kasih.";

									MailSender.simpanNotif(userIds, recipientsTemp,
											"Info kehadiran pegawai pada " + info(), ket, Pertemuan.this);
								}
							}
						}

											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();
			}

			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ";", "..\n");
			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ",", "_");
			String formatBaru = "";
			String[] nilais = getAbsensi().split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = nn.split(",");
					if (!s[0].trim().isEmpty()) {
						Long formatId = Long.parseLong(s[0]);
						if (ref.equals(formatId)) {
							aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode() + ","
									+ statusabsensi.getNama() + ","
									+ (pengajuanIzinTidakMasukPerkuliahan == null ? retreivePengajuanIzinId(ref)
											: pengajuanIzinTidakMasukPerkuliahan.getId())
									+ "," + (keterangan == null ? retreiveAbsensiKeterangan(ref) : keterangan) + ","
									+ (mulai == null ? retreiveAbsensiMulai(ref) : mulai) + ","
									+ (sampai == null ? retreiveAbsensiSampai(ref) : sampai) + ","
									+ (jenis == null ? retreiveAbsensiJenis(ref) : jenis);
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!ada) {
				String aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode() + ","
						+ statusabsensi.getNama() + ","
						+ (pengajuanIzinTidakMasukPerkuliahan == null ? retreivePengajuanIzinId(ref)
								: pengajuanIzinTidakMasukPerkuliahan.getId())
						+ "," + (keterangan == null ? retreiveAbsensiKeterangan(ref) : keterangan) + ","
						+ (mulai == null ? retreiveAbsensiMulai(ref) : mulai) + ","
						+ (sampai == null ? retreiveAbsensiSampai(ref) : sampai) + ","
						+ (jenis == null ? retreiveAbsensiJenis(ref) : jenis);
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			// System.out.println("formatBaru => " + formatBaru);

			absensi = formatBaru;
		}

	}

	/**
	 * Ambil {@code statusabsensi.id} (slot 1) dari baris absensi milik {@code ref}.
	 *
	 * <p>Salah satu dari keluarga pembaca {@code retreiveAbsensiXxx(Long)} yang semuanya bekerja
	 * dengan pola sama: pecah {@link #getAbsensi()} per {@code ';'}, pecah tiap baris per
	 * {@code ','}, cocokkan slot 0 dengan {@code ref}, lalu kembalikan slot yang diminta. Baris
	 * yang rusak/tidak lengkap dilewati diam-diam (exception ditangkap dan dicatat, bukan
	 * dilempar), sehingga satu baris rusak tidak menggagalkan pembacaan baris lain.</p>
	 *
	 * @param ref id orang yang dicari; {@code null} langsung menghasilkan nilai "tidak ditemukan"
	 * @return id status absensi, atau {@code -1L} bila tidak ada baris yang cocok
	 * @see #retreiveAbsensiKode(Long)
	 */
	public Long retreiveAbsensiId(Long ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					Long formatId = s[0].isEmpty() ? -1L : Long.parseLong(s[0]);
					if (ref.equals(formatId)) {
						return Long.parseLong(s[1]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1172");

				}
			}
		}

		return -1L;
	}

	/**
	 * Ambil id {@link PengajuanIzinTidakMasukPerkuliahan} (slot 4) dari baris absensi milik
	 * {@code ref}.
	 *
	 * <p>Menghubungkan satu baris ketidakhadiran dengan surat izin yang mendasarinya.</p>
	 *
	 * @param ref id orang yang dicari
	 * @return id pengajuan izin, atau {@code -1L} bila tidak ada/baris tidak ditemukan
	 * @see #retreiveAbsensiId(Long)
	 */
	public Long retreivePengajuanIzinId(Long ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					if (ref.equals(formatId)) {
						return Long.parseLong(s[4]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1192");

				}
			}
		}

		return -1L;
	}

	/**
	 * Apakah ada MINIMAL SATU dosen yang tercatat hadir pada pertemuan ini?
	 *
	 * <p>Menelusuri baris absensi yang berakhiran {@code "dosen"} (slot 8 = jenis) dan memeriksa
	 * apakah kode statusnya (slot 2) sama dengan {@code "M"}.</p>
	 *
	 * <p>Ini adalah penanda de-facto bahwa pertemuan benar-benar TERLAKSANA, dan dipakai persis
	 * begitu oleh {@link #getTanggalRealisasi()}: tanggal realisasi hanya ada bila ada dosen yang
	 * masuk, dan dikosongkan kembali bila tidak.</p>
	 *
	 * @return {@code true} bila setidaknya satu dosen berstatus hadir
	 * @see #apakahAdaGuruYangMasuk()
	 * @see #getTanggalRealisasi()
	 */
	public Boolean apakahAdaDosenYangMasuk() {

		String[] nilais = getAbsensi().split(";");
		for (String nn : nilais) {
			try {
				if (nn.toLowerCase().endsWith("dosen")) {
					String[] s = nn.split(",");
					if (s[2].equalsIgnoreCase("M")) {
						return true;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1212");

			}
		}

		return false;
	}

	/**
	 * Apakah ada MINIMAL SATU guru yang tercatat hadir pada pertemuan ini?
	 *
	 * <p>Padanan {@link #apakahAdaDosenYangMasuk()} untuk jenjang sekolah: menelusuri baris
	 * berakhiran {@code "guru"} dan memeriksa kode status {@code "M"}.</p>
	 *
	 * <p><b>Catatan:</b> berbeda dari padanan dosennya, hasil method ini TIDAK ikut menentukan
	 * {@link #getTanggalRealisasi()} — tanggal realisasi pertemuan sekolah karenanya tidak pernah
	 * terisi otomatis oleh kehadiran guru.</p>
	 *
	 * @return {@code true} bila setidaknya satu guru berstatus hadir
	 * @see #apakahAdaDosenYangMasuk()
	 */
	public Boolean apakahAdaGuruYangMasuk() {

		String[] nilais = getAbsensi().split(";");
		for (String nn : nilais) {
			try {
				if (nn.toLowerCase().endsWith("guru")) {
					String[] s = nn.split(",");
					if (s[2].equalsIgnoreCase("M")) {
						return true;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1231");

			}
		}

		return false;
	}

	/**
	 * Ambil KODE status kehadiran (slot 2) dari baris absensi milik {@code ref}.
	 *
	 * <p>Ini pembaca yang paling sering dipakai UI: kodenya pendek ({@code "M"} = masuk/hadir,
	 * selain itu bergantung data {@link Statusabsensi} tenant, mis. izin/sakit/alpa).</p>
	 *
	 * <p>Varian ini punya penjagaan paling lengkap di antara keluarganya — panjang array, slot
	 * kosong, dan {@link NumberFormatException} ditangani terpisah sehingga baris rusak dilewati
	 * tanpa mengganggu baris berikutnya.</p>
	 *
	 * @param ref id orang yang dicari
	 * @return kode status, atau {@code "-"} bila belum ada catatan kehadiran untuk orang itu
	 * @see #retreiveAbsensiNama(Long)
	 */
	public String retreiveAbsensiKode(Long ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length < 1 || s[0] == null || s[0].isEmpty()) {
						continue;
					}
					Long formatId;
					try {
						formatId = Long.parseLong(s[0]);
					} catch (NumberFormatException nfe) {
						ais.common.ErrorAuditUtil.record(nfe, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1246");
						continue;
					}
					if (ref.equals(formatId) && s.length > 2) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1250");

				}
			}
		}

		return "-";
	}

	/**
	 * Ambil NAMA status kehadiran (slot 3) dari baris absensi milik {@code ref}.
	 *
	 * <p>Nama panjang yang siap ditampilkan, mis. {@code "Masuk"}, {@code "Izin"}, {@code "Sakit"}.
	 * Nilainya adalah salinan {@link Statusabsensi#getNama()} pada saat kehadiran dicatat, jadi
	 * mengganti nama status di master data TIDAK mengubah baris absensi lama.</p>
	 *
	 * @param ref id orang yang dicari
	 * @return nama status, atau {@code "-"} bila tidak ditemukan
	 * @see #retreiveAbsensiKode(Long)
	 */
	public String retreiveAbsensiNama(Long ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					if (ref.equals(formatId)) {
						return s[3];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1270");

				}
			}
		}

		return "-";
	}

	/**
	 * Ambil KETERANGAN kehadiran (slot 5) dari baris absensi milik {@code ref}.
	 *
	 * <p>Memakai {@code split(",", 9)} — batas sembilan bagian — sehingga koma yang tersisa di
	 * dalam slot terakhir tidak memecah baris lebih jauh. Ingat bahwa koma pada keterangan asli
	 * sudah diganti {@code '_'} saat ditulis oleh {@link #populate(Long, Statusabsensi, String,
	 * PengajuanIzinTidakMasukPerkuliahan, String, String, String)}, jadi teks yang dikembalikan
	 * bukan teks yang persis diketik pengguna.</p>
	 *
	 * @param ref id orang yang dicari
	 * @return keterangan, atau string kosong bila tidak ditemukan
	 */
	public String retreiveAbsensiKeterangan(Long ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					if (ref.equals(formatId)) {
						return s[5];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1290");

				}
			}
		}

		return "";
	}

	/**
	 * Ambil JAM MULAI kehadiran (slot 6) dari baris absensi milik {@code ref}.
	 *
	 * <p>Hanya terisi untuk orang yang berstatus hadir ({@code "M"}); untuk status lain
	 * {@link #populate(Long, Statusabsensi, String, PengajuanIzinTidakMasukPerkuliahan, String,
	 * String, String)} sengaja mengosongkannya.</p>
	 *
	 * <p>Di dalam badan method terdapat penjagaan baris kosong yang tertulis DUA KALI persis sama
	 * (sisa perbaikan {@link NumberFormatException} yang tumpang tindih). Duplikasi itu tidak
	 * berbahaya, hanya mubazir.</p>
	 *
	 * @param ref id orang yang dicari
	 * @return jam mulai kehadiran, atau string kosong
	 * @see #retreiveAbsensiSampai(Long)
	 */
	public String retreiveAbsensiMulai(Long ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					// FIX NumberFormatException: baris data absensi kosong/rusak (s[0]="") memang
					// bisa terjadi (mis. record belum lengkap) -- lewati saja, bukan error, jangan
					// panggil parseLong pada string kosong.
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					if (ref.equals(formatId)) {
						return s[6];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1310");

				}
			}
		}

		return "";
	}

	/**
	 * Ambil JAM SELESAI kehadiran (slot 7) dari baris absensi milik {@code ref}.
	 *
	 * @param ref id orang yang dicari
	 * @return jam selesai kehadiran, atau string kosong
	 * @see #retreiveAbsensiMulai(Long)
	 */
	public String retreiveAbsensiSampai(Long ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					if (ref.equals(formatId)) {
						return s[7];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1330");

				}
			}
		}

		return "";
	}

	/**
	 * Ambil JENIS orang (slot 8) dari baris absensi milik {@code ref}.
	 *
	 * <p>Nilainya salah satu dari {@code "Mahasiswa"}, {@code "Siswa"}, {@code "Dosen"},
	 * {@code "Guru"}, atau {@code "Pegawai"}. Karena berada di ujung baris, slot inilah yang
	 * dipakai keluarga {@code hitungStatusXxx(...)} untuk memisahkan baris peserta dari baris
	 * pengajar dengan {@code endsWith} yang murah.</p>
	 *
	 * <p>Perhatikan bahwa {@code "Mahasiswa"} juga berakhiran {@code "siswa"}, sehingga
	 * {@link #hitungStatus(Mahasiswa)} sengaja menerima keduanya sekaligus.</p>
	 *
	 * @param ref id orang yang dicari
	 * @return jenis orang, atau string kosong bila tidak ditemukan
	 */
	public String retreiveAbsensiJenis(Long ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					if (ref.equals(formatId)) {
						return s[8];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1350");

				}
			}
		}

		return "";
	}

	
	/**
	 * Hitung berapa kali tiap kode status kehadiran muncul pada baris-baris bertanda
	 * {@code "Siswa"}.
	 *
	 * <p>Hasilnya berupa peta {@code kode -> jumlah}, mis. {@code {"M": 1, "S": 0}}. Kode
	 * {@code "-"} (belum diisi) TIDAK ikut dihitung, sehingga peta yang kosong berarti "belum ada
	 * kehadiran yang tercatat".</p>
	 *
	 * <p>Pada satu objek Pertemuan, satu siswa hanya punya satu baris, jadi nilai hitungannya
	 * paling banyak 1. Method ini menjadi berguna justru ketika hasilnya digabungkan untuk banyak
	 * pertemuan sekaligus (rekap kehadiran satu semester) — di situlah angka lebih dari satu
	 * muncul.</p>
	 *
	 * @param siswa siswa yang dihitung; {@code null} berarti hitung SEMUA siswa pada pertemuan ini
	 * @return peta kode status ke jumlah kemunculan; kosong bila tidak ada yang cocok
	 * @see #hitungStatus(Mahasiswa)
	 * @see #hitungStatus()
	 */
	public Map<String, Integer> hitungStatusSiswa(Siswa siswa) {

		Map<String, Integer> jumlah = new HashMap<String, Integer>();
		Pertemuan pertemuan = this;
		String[] nilais = pertemuan.getAbsensi().split(";");
		for (String nn : nilais) {
			try {
				if (nn.toLowerCase().endsWith("siswa")) {
					String[] s = nn.split(",");
					Long formatId = null;
					try {
						formatId = Long.parseLong(s[0]);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1372");
					}
					if (siswa == null || (formatId != null && siswa.getId().equals(formatId))) {
						String kode = s[2];
						if (!kode.equalsIgnoreCase("-")) {
							if (jumlah.containsKey(kode)) {
								jumlah.put(kode, jumlah.get(kode) + 1);
							} else {
								jumlah.put(kode, 1);
							}
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1385");

			}
		}

		return jumlah;
	}

	/**
	 * Hitung kemunculan tiap kode status kehadiran pada baris peserta didik.
	 *
	 * <p>Penyaringnya menerima baris berakhiran {@code "mahasiswa"} <b>maupun</b> {@code "siswa"}.
	 * Karena kata "mahasiswa" sendiri berakhiran "siswa", syarat kedua sebenarnya sudah mencakup
	 * yang pertama — jadi method ini menghitung mahasiswa DAN siswa sekaligus, bukan hanya
	 * mahasiswa seperti yang mungkin disangka dari namanya.</p>
	 *
	 * <p>Sama seperti {@link #hitungStatusSiswa(Siswa)}, kode {@code "-"} diabaikan.</p>
	 *
	 * @param mahasiswa mahasiswa yang dihitung; {@code null} berarti hitung semua peserta
	 * @return peta kode status ke jumlah kemunculan
	 * @see #hitungStatusSiswa(Siswa)
	 */
	public Map<String, Integer> hitungStatus(Mahasiswa mahasiswa) {

		Map<String, Integer> jumlah = new HashMap<String, Integer>();
		Pertemuan pertemuan = this;
		String[] nilais = pertemuan.getAbsensi().split(";");
		for (String nn : nilais) {
			try {
				if (nn.toLowerCase().endsWith("mahasiswa") || nn.toLowerCase().endsWith("siswa")) {
					String[] s = nn.split(",");
					Long formatId = null;
					try {
						formatId = Long.parseLong(s[0]);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1405");
					}
					if (mahasiswa == null || (formatId != null && mahasiswa.getId().equals(formatId))) {
						String kode = s[2];
						if (!kode.equalsIgnoreCase("-")) {
							if (jumlah.containsKey(kode)) {
								jumlah.put(kode, jumlah.get(kode) + 1);
							} else {
								jumlah.put(kode, 1);
							}
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1418");

			}
		}

		return jumlah;
	}

	/**
	 * Rekap status kehadiran SELURUH peserta didik pada pertemuan ini.
	 *
	 * <p><b>Hati-hati dengan cara penggabungannya:</b> hasil {@link #hitungStatusSiswa(Siswa)}
	 * dimasukkan dengan {@code putAll}, yang berarti MENIMPA — bukan menjumlahkan — nilai kode yang
	 * sama dari {@link #hitungStatus(Mahasiswa)}. Karena {@link #hitungStatus(Mahasiswa)} sudah
	 * ikut menghitung baris siswa (lihat Javadoc-nya), penimpaan itu tidak menyebabkan penggandaan,
	 * tetapi pada pertemuan yang bercampur mahasiswa dan siswa angkanya menjadi angka siswa saja
	 * untuk kode yang muncul di keduanya. Untuk rekap yang tepat pada kasus campuran, panggil kedua
	 * method itu terpisah.</p>
	 *
	 * @return peta kode status ke jumlah kemunculan
	 */
	public Map<String, Integer> hitungStatus() {
		Map<String, Integer> map = hitungStatus(null);
		map.putAll(hitungStatusSiswa(null));
		return map;
	}

	/**
	 * Rekap status kehadiran SELURUH dosen pada pertemuan ini.
	 *
	 * @return peta kode status ke jumlah kemunculan
	 * @see #hitungStatusDosen(Dosen)
	 */
	public Map<String, Integer> hitungStatusDosen() {
		return hitungStatusDosen(null);
	}

	/**
	 * Hitung kemunculan tiap kode status kehadiran pada baris bertanda {@code "Dosen"}.
	 *
	 * <p>Berbeda dari versi peserta didik, penyaring {@code endsWith("dosen")} di sini tidak
	 * bertabrakan dengan jenis lain, sehingga hitungannya bersih.</p>
	 *
	 * @param dosen dosen yang dihitung; {@code null} berarti hitung semua dosen
	 * @return peta kode status ke jumlah kemunculan
	 * @see #apakahAdaDosenYangMasuk()
	 */
	public Map<String, Integer> hitungStatusDosen(Dosen dosen) {

		Map<String, Integer> jumlah = new HashMap<String, Integer>();
		Pertemuan pertemuan = this;
		String[] nilais = pertemuan.getAbsensi().split(";");
		for (String nn : nilais) {
			try {
				if (nn.toLowerCase().endsWith("dosen")) {
					String[] s = nn.split(",");
					String kode = s[2];
					Long formatId = null;
					try {
						formatId = Long.parseLong(s[0]);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1449");
					}
					if (dosen == null || (formatId != null && dosen.getId().equals(formatId))) {
						if (!kode.equalsIgnoreCase("-")) {

							if (jumlah.containsKey(kode)) {
								jumlah.put(kode, jumlah.get(kode) + 1);
							} else {
								jumlah.put(kode, 1);
							}
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1462");

			}
		}

		return jumlah;
	}

	/**
	 * Rekap status kehadiran SELURUH guru pada pertemuan ini.
	 *
	 * @return peta kode status ke jumlah kemunculan
	 * @see #hitungStatusGuru(Guru)
	 */
	public Map<String, Integer> hitungStatusGuru() {
		return hitungStatusGuru(null);
	}

	/**
	 * Hitung kemunculan tiap kode status kehadiran pada baris bertanda {@code "Guru"}.
	 *
	 * <p>Padanan {@link #hitungStatusDosen(Dosen)} untuk jenjang sekolah.</p>
	 *
	 * @param guru guru yang dihitung; {@code null} berarti hitung semua guru
	 * @return peta kode status ke jumlah kemunculan
	 */
	public Map<String, Integer> hitungStatusGuru(Guru guru) {

		Map<String, Integer> jumlah = new HashMap<String, Integer>();
		Pertemuan pertemuan = this;
		String[] nilais = pertemuan.getAbsensi().split(";");
		for (String nn : nilais) {
			try {
				if (nn.toLowerCase().endsWith("guru")) {
					String[] s = nn.split(",");
					String kode = s[2];
					Long formatId = null;
					try {
						formatId = Long.parseLong(s[0]);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1487");
					}
					if (guru == null || (formatId != null && guru.getId().equals(formatId))) {
						if (!kode.equalsIgnoreCase("-")) {

							if (jumlah.containsKey(kode)) {
								jumlah.put(kode, jumlah.get(kode) + 1);
							} else {
								jumlah.put(kode, 1);
							}
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1500");

			}
		}

		return jumlah;
	}

	/**
	 * Id petugas pertama yang ditugaskan pada pertemuan ini (mis. pengawas ujian).
	 *
	 * <p>Disimpan sebagai id mentah, bukan relasi. Ada empat slot petugas
	 * ({@code petugas}..{@code petugas4}) yang dipakai berbeda-beda tergantung jenis induk
	 * pertemuan.</p>
	 *
	 * @return id petugas, atau {@code null}
	 * @see #getPetugas2()
	 * @see #getPetugas3()
	 * @see #getPetugas4()
	 */
	public Long getPetugas() {
		return petugas;
	}

	/**
	 * Setel id petugas pertama.
	 *
	 * @param petugas id petugas
	 */
	public void setPetugas(Long petugas) {
		this.petugas = petugas;
	}

	/**
	 * Id petugas kedua yang ditugaskan pada pertemuan ini.
	 *
	 * @return id petugas kedua, atau {@code null}
	 * @see #getPetugas()
	 */
	public Long getPetugas2() {
		return petugas2;
	}

	/**
	 * Setel id petugas kedua.
	 *
	 * @param petugas2 id petugas kedua
	 */
	public void setPetugas2(Long petugas2) {
		this.petugas2 = petugas2;
	}

	/**
	 * Id petugas ketiga yang ditugaskan pada pertemuan ini.
	 *
	 * @return id petugas ketiga, atau {@code null}
	 * @see #getPetugas()
	 */
	public Long getPetugas3() {
		return petugas3;
	}

	/**
	 * Setel id petugas ketiga.
	 *
	 * @param petugas3 id petugas ketiga
	 */
	public void setPetugas3(Long petugas3) {
		this.petugas3 = petugas3;
	}

	/**
	 * Id detail kurikulum/template format bimbingan yang dipakai pertemuan ini.
	 *
	 * <p><b>Getter ini dapat MENULIS BARIS BARU ke basis data.</b> Untuk pertemuan bimbingan tugas
	 * akhir yang merupakan pertemuan PERTAMA ({@link #getPertemuanKe()} bernilai 1), belum punya
	 * nilai, dan induknya punya format nilai proposal, method ini memanggil
	 * {@code TemplateFormatBimbingan.createDefaultTemplateFormatBimbingan(...)} yang MEMBUAT
	 * template bawaan, lalu menyimpan id-nya ke field. Jadi sekadar membaca properti ini bisa
	 * menimbulkan data baru — perilaku yang sangat tidak lazim untuk sebuah getter dan patut
	 * diingat saat menelusuri "dari mana template ini muncul".</p>
	 *
	 * <p>Seluruh kegagalan ditelan (dicetak dan dicatat), sehingga nilai tetap {@code null} bila
	 * pembuatan template gagal.</p>
	 *
	 * @return id detail kurikulum/template, atau {@code null}
	 */
	@Column(name = "kurikulum_punya_matakuliahDetail")
	public Long getKurikulumPunyaMatakuliahDetail() {

		try {
			mahasiswaRequestTugasAkhir = getMahasiswaRequestTugasAkhir();
			if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() != null
					&& kurikulumPunyaMatakuliahDetail == null && getPertemuanKe().equals(1)) {
				Session session = HibernateUtil.currentSession();
				kurikulumPunyaMatakuliahDetail = TemplateFormatBimbingan.createDefaultTemplateFormatBimbingan(session,
						mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi()).getId();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:1544");
		}

		return kurikulumPunyaMatakuliahDetail;
	}

	/**
	 * Setel id detail kurikulum/template format bimbingan.
	 *
	 * @param kurikulumPunyaMatakuliahDetail id template
	 * @see #getKurikulumPunyaMatakuliahDetail()
	 */
	public void setKurikulumPunyaMatakuliahDetail(Long kurikulumPunyaMatakuliahDetail) {
		this.kurikulumPunyaMatakuliahDetail = kurikulumPunyaMatakuliahDetail;
	}

	/**
	 * KRS mahasiswa yang menjadi induk pertemuan ini (sesi konsultasi Pembimbing Akademik).
	 *
	 * <p>Berbeda dari kebanyakan relasi induk lain, getter ini TIDAK memanggil {@code check(...)},
	 * sehingga proxy yang basi dikembalikan apa adanya.</p>
	 *
	 * @return {@link KrsMahasiswa} induk, atau {@code null}
	 * @see #untuk()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "krs_mahasiswa", nullable = true)
	public KrsMahasiswa getKrsMahasiswa() {
		return krsMahasiswa;
	}

	/**
	 * Tetapkan KRS mahasiswa sebagai induk pertemuan ini.
	 *
	 * @param krsMahasiswa KRS mahasiswa; boleh {@code null}
	 */
	public void setKrsMahasiswa(KrsMahasiswa krsMahasiswa) {
		this.krsMahasiswa = krsMahasiswa;
	}

	/**
	 * Id dosen pengganti yang mengisi pertemuan ini menggantikan dosen pengampu.
	 *
	 * <p>Disimpan sebagai id mentah, bukan relasi ke {@link Dosen}.</p>
	 *
	 * @return id dosen pengganti, atau {@code null} bila pertemuan diisi dosen pengampu sendiri
	 * @see #getGuruPengganti()
	 */
	public Long getDosenPengganti() {
		return dosenPengganti;
	}

	/**
	 * Setel id dosen pengganti.
	 *
	 * @param dosenPengganti id dosen pengganti
	 */
	public void setDosenPengganti(Long dosenPengganti) {
		this.dosenPengganti = dosenPengganti;
	}

	/**
	 * Tanggal pertemuan benar-benar TERLAKSANA (berbeda dari tanggal terjadwal).
	 *
	 * <p><b>Getter ini menghitung ulang dan mengubah keadaan objek setiap kali dipanggil.</b>
	 * Aturannya bergantung sepenuhnya pada {@link #apakahAdaDosenYangMasuk()}:</p>
	 * <ul>
	 *   <li><b>Ada dosen hadir</b> — bila tanggal realisasi masih kosong, diisi dari
	 *       {@link #getTanggal()}; bila itu pun kosong, diisi waktu sekarang. Selain itu, tanggal
	 *       realisasi yang jatuh di tahun 1970 (sisa nilai epoch yang rusak) diganti kembali dengan
	 *       {@link #getTanggal()}.</li>
	 *   <li><b>Tidak ada dosen hadir</b> — tanggal realisasi DIKOSONGKAN ({@code null}).</li>
	 * </ul>
	 *
	 * <p>Akibat penting: menghapus/mengubah kehadiran dosen sehingga tidak ada lagi yang berstatus
	 * hadir akan MENGHAPUS tanggal realisasi yang sudah tersimpan pada flush berikutnya. Nilai yang
	 * diisi manual lewat {@link #setTanggalRealisasi(Date)} juga tidak akan bertahan bila syarat di
	 * atas tidak terpenuhi.</p>
	 *
	 * <p>Hasil akhir tetap dilewatkan {@link #bersihkanTanggalRusak(Date)}.</p>
	 *
	 * @return tanggal realisasi, atau {@code null} bila pertemuan belum terlaksana
	 * @see #getTanggal()
	 * @see #apakahAdaDosenYangMasuk()
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalRealisasi() {

		if (apakahAdaDosenYangMasuk()) {
			if (tanggalRealisasi == null) {
				tanggalRealisasi = getTanggal();
			}
			if (tanggalRealisasi == null) {
				tanggalRealisasi = ais.ui.util.WaktuUtil.getDate();
			}

			if (tanggalRealisasi != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(tanggalRealisasi);
				if (calendar.get(Calendar.YEAR) == 1970) {
					tanggalRealisasi = getTanggal();
				}
			}
		} else {
			tanggalRealisasi = null;
		}
		return bersihkanTanggalRusak(tanggalRealisasi);
	}

	/**
	 * Setel tanggal realisasi pertemuan.
	 *
	 * <p>Ingat bahwa {@link #getTanggalRealisasi()} menghitung ulang nilai ini setiap kali
	 * dipanggil, sehingga nilai yang diisi di sini dapat ditimpa atau dikosongkan kembali.</p>
	 *
	 * @param tanggalRealisasi tanggal realisasi; nilai sebelum tahun 2000 dibuang
	 */
	public void setTanggalRealisasi(Date tanggalRealisasi) {
		this.tanggalRealisasi = bersihkanTanggalRusak(tanggalRealisasi);
	}

	/**
	 * Kode program studi/jenjang pertemuan ini, hasil denormalisasi dari induk.
	 *
	 * <p>Salah satu dari sekelompok properti "denormalisasi untuk pencarian": nilainya dihitung
	 * ulang dari induk SETIAP KALI getter dipanggil lalu disimpan ke kolomnya sendiri, supaya
	 * laporan dan pencarian dapat menyaring langsung di tingkat SQL tanpa menempuh join berlapis
	 * ke induk yang berbeda-beda jenisnya.</p>
	 *
	 * <p>Sumber nilainya, menurut urutan pemeriksaan: {@link Perkuliahan}, KKN, PKL, {@link Skripsi},
	 * bimbingan tugas akhir, lalu {@link KrsMahasiswa}. Jenis induk lain tidak menetapkan program,
	 * sehingga nilai lama dipertahankan.</p>
	 *
	 * <p><b>Perhatian:</b> cabang KKN membaca field {@code kelompokKkn} secara langsung, bukan
	 * hasil {@link #getKelompokKkn()} yang baru saja disegarkan di baris sebelumnya — pola yang
	 * juga muncul pada {@link #getFakultasId()} dan {@link #getJurusanId()}. Seluruh exception
	 * ditelan, sehingga kegagalan menempuh rantai induk hanya berarti nilai tidak berubah.</p>
	 *
	 * @return kode program, atau {@code null}
	 * @see #getFakultasId()
	 * @see #getJurusanId()
	 */
	public String getProgram() {
		try {
			perkuliahan = getPerkuliahan();
			kelompokPkl = getKelompokPkl();
			mahasiswaRequestTugasAkhir = getMahasiswaRequestTugasAkhir();
			skripsi = getSkripsi();
			krsMahasiswa = getKrsMahasiswa();
			formulirKegiatan = getFormulirKegiatan();
			pertemuanPunyaGrupPertemuan = getPertemuanPunyaGrupPertemuan();
			if (perkuliahan != null) {
				program = perkuliahan.getProgram();
			} else if (kelompokKkn != null) {
				program = kelompokKkn.getKkn().getProgram();
			} else if (kelompokPkl != null) {
				program = kelompokPkl.getPkl().getProgram();
			} else if (skripsi != null) {
				program = skripsi.getMahasiswa().getProgram();
			} else if (mahasiswaRequestTugasAkhir != null) {
				program = mahasiswaRequestTugasAkhir.getMahasiswa().getProgram();
			} else if (krsMahasiswa != null) {
				program = krsMahasiswa.getMahasiswa().getProgram();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1623");
			// TODO: handle exception
		}
		return program;
	}

	/**
	 * Setel kode program hasil denormalisasi.
	 *
	 * <p>Nilai ini dihitung ulang oleh {@link #getProgram()}, jadi pengisian manual jarang
	 * bertahan.</p>
	 *
	 * @param program kode program
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Id fakultas pertemuan ini, hasil denormalisasi dari induk.
	 *
	 * <p>Bagian dari kelompok properti denormalisasi (lihat {@link #getProgram()}). Nilainya
	 * ditelusuri dari induk: perkuliahan &rarr; jurusan &rarr; fakultas; KKN/PKL &rarr; fakultas
	 * kegiatan; skripsi/bimbingan/KRS &rarr; jurusan mahasiswa &rarr; fakultas.</p>
	 *
	 * <p><b>Perhatian:</b> semua cabang membaca field induk secara LANGSUNG tanpa menyegarkannya
	 * lebih dulu lewat getter. Bila proxy belum terinisialisasi atau objek sedang lepas dari
	 * session, rantai penelusuran gagal, exception ditelan, dan nilai lama dipertahankan.</p>
	 *
	 * @return id fakultas, atau {@code null}
	 * @see #getJurusanId()
	 */
	public Long getFakultasId() {
		try {
			if (perkuliahan != null && perkuliahan.getJurusan() != null) {
				fakultasId = perkuliahan.getJurusan().getFakultas().getId();
			} else if (kelompokKkn != null) {
				fakultasId = kelompokKkn.getKkn().getFakultas() == null ? null
						: kelompokKkn.getKkn().getFakultas().getId();
			} else if (kelompokPkl != null) {
				fakultasId = kelompokPkl.getPkl().getFakultas() == null ? null
						: kelompokPkl.getPkl().getFakultas().getId();
			} else if (skripsi != null) {
				fakultasId = skripsi.getMahasiswa().getJurusan().getFakultas().getId();
			} else if (mahasiswaRequestTugasAkhir != null) {
				fakultasId = mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFakultas().getId();
			} else if (krsMahasiswa != null) {
				fakultasId = krsMahasiswa.getMahasiswa().getJurusan().getFakultas().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1650");
			// TODO: handle exception
		}
		return fakultasId;
	}

	/**
	 * Setel id fakultas hasil denormalisasi.
	 *
	 * @param fakultasId id fakultas
	 * @see #getFakultasId()
	 */
	public void setFakultasId(Long fakultasId) {
		this.fakultasId = fakultasId;
	}

	/**
	 * Id jurusan/program studi pertemuan ini, hasil denormalisasi dari induk.
	 *
	 * <p>Kembarannya {@link #getFakultasId()}, satu tingkat lebih rendah dalam hierarki organisasi,
	 * dengan pola penelusuran dan keterbatasan yang sama persis.</p>
	 *
	 * <p>Jangan tertukar dengan {@link #getJurusan()} yang mengembalikan objek {@link Jurusan}
	 * penuh: keduanya menempuh rantai induk yang BERBEDA (mis. untuk pertemuan formulir kegiatan,
	 * hanya {@link #getJurusan()} yang punya cabangnya), sehingga hasil keduanya dapat tidak
	 * selaras.</p>
	 *
	 * @return id jurusan, atau {@code null}
	 * @see #getJurusan()
	 */
	public Long getJurusanId() {
		try {
			if (perkuliahan != null && perkuliahan.getJurusan() != null) {
				jurusanId = perkuliahan.getJurusan().getId();
			} else if (kelompokKkn != null) {
				jurusanId = kelompokKkn.getKkn().getJurusan() == null ? null
						: kelompokKkn.getKkn().getJurusan().getId();
			} else if (kelompokPkl != null) {
				jurusanId = kelompokPkl.getPkl().getJurusan() == null ? null
						: kelompokPkl.getPkl().getJurusan().getId();
			} else if (skripsi != null) {
				jurusanId = skripsi.getMahasiswa().getJurusan().getId();
			} else if (mahasiswaRequestTugasAkhir != null) {
				jurusanId = mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getId();
			} else if (krsMahasiswa != null) {
				jurusanId = krsMahasiswa.getMahasiswa().getJurusan().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1677");
			// TODO: handle exception
		}
		return jurusanId;
	}

	/**
	 * Setel id jurusan hasil denormalisasi.
	 *
	 * @param jurusanId id jurusan
	 * @see #getJurusanId()
	 */
	public void setJurusanId(Long jurusanId) {
		this.jurusanId = jurusanId;
	}

	/**
	 * Format penilaian yang dipakai bila pertemuan ini berperan sebagai tugas.
	 *
	 * <p>Implementasi properti abstrak milik {@link Tugas}.</p>
	 *
	 * @return format nilai, atau {@code null}
	 * @see Tugas
	 * @see #getFormatNilais()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "format_nilai", nullable = true)
	public FormatNilai getFormatNilai() {
		return formatNilai;
	}

	/**
	 * Setel format penilaian tugas pertemuan ini.
	 *
	 * @param formatNilai format nilai; boleh {@code null}
	 */
	public void setFormatNilai(FormatNilai formatNilai) {
		this.formatNilai = formatNilai;
	}

	/**
	 * Bobot pertemuan/tugas ini dalam persen terhadap komponen nilai induknya.
	 *
	 * <p>Nilai kosong dilaporkan sebagai {@code 100.0}, artinya "menanggung seluruh bobot".
	 * Implementasi properti abstrak milik {@link Tugas}.</p>
	 *
	 * @return bobot dalam persen; tidak pernah {@code null}
	 */
	public Double getProsentase() {
		return prosentase == null ? 100.0 : prosentase;
	}

	/**
	 * Setel bobot pertemuan/tugas ini dalam persen.
	 *
	 * @param prosentase bobot dalam persen; {@code null} berarti 100
	 */
	public void setProsentase(Double prosentase) {
		this.prosentase = prosentase;
	}

	/**
	 * Daftar pasangan warna ({@code "warnaGelap,warnaTerang"}) untuk membedakan jenis pertemuan
	 * di tampilan kalender.
	 *
	 * <p>Diisi sekali lewat blok inisialisasi statis di bawahnya, dan dipetakan ke jenis induk oleh
	 * {@link #warna()}. Karena bertipe {@code public static} dan bukan koleksi tak-terubah, isinya
	 * DAPAT diubah dari mana saja — jangan diandalkan sebagai konstanta.</p>
	 *
	 * @see #warna()
	 */
	public static List<String> warnas = new ArrayList<String>();

	static {
		warnas.add("#A32929,#D96666");
		warnas.add("#88880E,#BFBF4D");
		warnas.add("#7A367A,#B373B3");
		warnas.add("#3467CE,#668CD9");
		warnas.add("#0D7813,#4CB052");
		warnas.add("#88880E,#BFBF4D");
		warnas.add("navy,aqua");
		warnas.add("green,magenta");
		warnas.add("#a432a8,aqua");
		warnas.add("#960008,#072c63");
	}

	/**
	 * Daftar dosen yang berkaitan dengan pertemuan ini, diurutkan menurut nama.
	 *
	 * <p>Karena pertemuan bisa bertaut ke banyak jenis induk, "dosen" di sini berarti hal yang
	 * berbeda-beda: dosen pengampu untuk {@link Perkuliahan}, dosen pembimbing untuk KKN/PKL,
	 * pembimbing tugas akhir/skripsi, dosen PA untuk {@link KrsMahasiswa}, atau dosen pemilik grup
	 * pertemuan. Yang dipakai adalah induk PERTAMA yang tidak {@code null}.</p>
	 *
	 * <p>Seluruh asosiasi induk disegarkan lebih dulu lewat getter-nya masing-masing. Ini bukan
	 * kebiasaan kosmetik: komentar KE-20 di dalam method mencatat bahwa {@code kelompokKkn} dahulu
	 * TIDAK disegarkan, sehingga proxy lama dipakai tanpa session aktif dan
	 * {@code populateDosenBuNama()} melempar {@code LazyInitializationException}.</p>
	 *
	 * <p><b>Mahal.</b> Setiap cabang memanggil {@code populateDosenBuNama()} milik induk yang
	 * menjalankan query. Jangan dipanggil di dalam perulangan render.</p>
	 *
	 * @return daftar dosen; kosong bila jenis induk pertemuan ini tidak mengenal dosen (mis.
	 *         jadwal pelajaran sekolah — pakai {@link #ambilGuru()} untuk itu)
	 * @see #ambilDosenId()
	 * @see #dosenUtama()
	 */
	public List<Dosen> ambilDosen() {
		perkuliahan = getPerkuliahan();
		// KE-20: kelompokKkn TIDAK di-refresh via getter (beda dgn asosiasi lain di method
		// ini) -> proxy lama terpakai tanpa Session aktif -> LazyInitializationException
		// saat populateDosenBuNama() memaksa inisialisasi. getKelompokKkn() (via check())
		// memastikan proxy valid/ter-refresh sebelum dipakai.
		kelompokKkn = getKelompokKkn();
		kelompokPkl = getKelompokPkl();
		mahasiswaRequestTugasAkhir = getMahasiswaRequestTugasAkhir();
		skripsi = getSkripsi();
		krsMahasiswa = getKrsMahasiswa();
		formulirKegiatan = getFormulirKegiatan();
		pertemuanPunyaGrupPertemuan = getPertemuanPunyaGrupPertemuan();
		List<Dosen> dosens = new ArrayList<Dosen>();
		if (perkuliahan != null) {
			dosens = perkuliahan.populateDosenBuNama();
		} else if (kelompokKkn != null) {
			dosens = kelompokKkn.populateDosenBuNama();
		} else if (kelompokPkl != null) {
			dosens = kelompokPkl.populateDosenBuNama();
		} else if (mahasiswaRequestTugasAkhir != null) {
			dosens = mahasiswaRequestTugasAkhir.populateDosenBuNama();
		} else if (skripsi != null) {
			dosens = skripsi.populateDosenBuNama();
		} else if (krsMahasiswa != null) {
			dosens = krsMahasiswa.populateDosenBuNama();
		} else if (pertemuanPunyaGrupPertemuan != null && pertemuanPunyaGrupPertemuan.getGrupPertemuan() != null) {
			dosens = pertemuanPunyaGrupPertemuan.populateDosenBuNama();
		}
		return dosens;
	}

	/**
	 * Versi hemat {@link #ambilDosen()} yang hanya mengembalikan id dosen.
	 *
	 * <p>Rantai {@code if/else if}-nya sama persis, tetapi memanggil {@code populateDosenBuId()}
	 * sehingga hanya kolom id yang diambil — jauh lebih murah bila yang dibutuhkan sekadar
	 * pemeriksaan keanggotaan atau perakitan kolom denormalisasi
	 * {@link #getDosens()}.</p>
	 *
	 * @return daftar id dosen; kosong bila jenis induk tidak mengenal dosen
	 * @see #ambilDosen()
	 * @see #getPjDosen()
	 */
	public List<Long> ambilDosenId() {
		perkuliahan = getPerkuliahan();
		// KE-20 (pola sama dgn ambilDosen()): refresh kelompokKkn via getter sebelum dipakai.
		kelompokKkn = getKelompokKkn();
		kelompokPkl = getKelompokPkl();
		mahasiswaRequestTugasAkhir = getMahasiswaRequestTugasAkhir();
		skripsi = getSkripsi();
		krsMahasiswa = getKrsMahasiswa();
		formulirKegiatan = getFormulirKegiatan();
		pertemuanPunyaGrupPertemuan = getPertemuanPunyaGrupPertemuan();
		List<Long> dosens = new ArrayList<Long>();
		if (perkuliahan != null) {
			dosens = perkuliahan.populateDosenBuId();
		} else if (kelompokKkn != null) {
			dosens = kelompokKkn.populateDosenBuId();
		} else if (kelompokPkl != null) {
			dosens = kelompokPkl.populateDosenBuId();
		} else if (mahasiswaRequestTugasAkhir != null) {
			dosens = mahasiswaRequestTugasAkhir.populateDosenBuId();
		} else if (skripsi != null) {
			dosens = skripsi.populateDosenBuId();
		} else if (krsMahasiswa != null) {
			dosens = krsMahasiswa.populateDosenBuId();
		} else if (pertemuanPunyaGrupPertemuan != null && pertemuanPunyaGrupPertemuan.getGrupPertemuan() != null) {
			dosens = pertemuanPunyaGrupPertemuan.populateDosenBuId();
		}
		return dosens;
	}

	/**
	 * Daftar guru yang mengajar pada pertemuan ini, diurutkan menurut nama.
	 *
	 * <p>Padanan {@link #ambilDosen()} untuk jenjang sekolah, tetapi jauh lebih sederhana: hanya
	 * induk {@link ais.database.model.sekolah.JadwalPelajaran} yang punya guru. Untuk jenis induk
	 * lain hasilnya selalu kosong.</p>
	 *
	 * @return daftar guru; kosong bila pertemuan ini bukan pertemuan jadwal pelajaran
	 * @see #ambilDosen()
	 */
	public List<Guru> ambilGuru() {
		jadwalPelajaran = getJadwalPelajaran();
		List<Guru> gurus = new ArrayList<Guru>();
		if (jadwalPelajaran != null) {
			gurus = jadwalPelajaran.populateGuruBuNama();
		}
		return gurus;
	}

	/**
	 * Satu dosen penanggung jawab utama pertemuan ini.
	 *
	 * <p>Mengikuti rantai induk yang sama dengan {@link #ambilDosen()}, tetapi mengambil satu dosen
	 * "yang pertama" menurut arti masing-masing induk: dosen pengampu pertama untuk
	 * {@link Perkuliahan}, pembimbing pertama untuk KKN/PKL, pembimbing skripsi, dosen PA untuk
	 * {@link KrsMahasiswa}, atau dosen pemilik grup pertemuan.</p>
	 *
	 * <p>Lebih murah daripada {@link #ambilDosen()} karena tidak menjalankan
	 * {@code populateDosenBuNama()}, cukup membaca satu relasi.</p>
	 *
	 * @return dosen utama, atau {@code null} bila jenis induk tidak mengenalnya
	 * @see #getPjDosen()
	 */
	public Dosen dosenUtama() {
		perkuliahan = getPerkuliahan();
		// KE-20 (pola sama dgn ambilDosen()): refresh kelompokKkn via getter sebelum dipakai.
		kelompokKkn = getKelompokKkn();
		kelompokPkl = getKelompokPkl();
		mahasiswaRequestTugasAkhir = getMahasiswaRequestTugasAkhir();
		skripsi = getSkripsi();
		krsMahasiswa = getKrsMahasiswa();
		formulirKegiatan = getFormulirKegiatan();
		pertemuanPunyaGrupPertemuan = getPertemuanPunyaGrupPertemuan();
		Dosen dosen = null;
		if (perkuliahan != null) {
			dosen = perkuliahan.getDosen1();
		} else if (kelompokKkn != null) {
			dosen = kelompokKkn.getDosen_pembimbing1();
		} else if (kelompokPkl != null) {
			dosen = kelompokPkl.getDosen_pembimbing1();
		} else if (mahasiswaRequestTugasAkhir != null) {
			dosen = mahasiswaRequestTugasAkhir.getDosen1();
		} else if (skripsi != null) {
			dosen = skripsi.getPembimbing();
		} else if (krsMahasiswa != null) {
			dosen = krsMahasiswa.getDosenPa();
		} else if (pertemuanPunyaGrupPertemuan != null && pertemuanPunyaGrupPertemuan.getGrupPertemuan() != null) {
			dosen = pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen();
		}
		return dosen;
	}

	/**
	 * Pasangan warna kalender untuk pertemuan ini, sesuai jenis induknya.
	 *
	 * <p>Mengembalikan satu entri {@link #warnas} berbentuk {@code "warnaGelap,warnaTerang"} agar
	 * tampilan kalender dapat membedakan sekilas antara kuliah, KKN, PKL, bimbingan, skripsi,
	 * konsultasi PA, pelajaran sekolah, kegiatan, dan wisuda. Jenis induk yang tidak punya
	 * pemetaan sendiri memakai warna cadangan (indeks 6).</p>
	 *
	 * <p><b>Dua kejanggalan yang memang ada di kode</b> (dicatat, tidak diperbaiki):</p>
	 * <ul>
	 *   <li>{@code kelompokKkn}, {@code jadwalPelajaran}, dan {@code wisuda} diuji lewat FIELD
	 *       tanpa disegarkan lebih dahulu — berbeda dari asosiasi lain di method yang sama yang
	 *       memang dipanggil lewat getter. Akibatnya cabang-cabang itu bisa terlewat.</li>
	 *   <li>Indeks 1 dan 5 pada {@link #warnas} berisi pasangan warna yang IDENTIK
	 *       ({@code "#88880E,#BFBF4D"}), sehingga pertemuan KKN dan konsultasi PA tampil berwarna
	 *       sama.</li>
	 * </ul>
	 *
	 * @return pasangan warna berformat {@code "warnaGelap,warnaTerang"}
	 * @see #warnas
	 * @see #info()
	 */
	public String warna() {
		perkuliahan = getPerkuliahan();
		kelompokPkl = getKelompokPkl();
		mahasiswaRequestTugasAkhir = getMahasiswaRequestTugasAkhir();
		skripsi = getSkripsi();
		krsMahasiswa = getKrsMahasiswa();
		formulirKegiatan = getFormulirKegiatan();
		pertemuanPunyaGrupPertemuan = getPertemuanPunyaGrupPertemuan();
		String warna = "";
		if (perkuliahan != null) {
			warna = warnas.get(0);
		} else if (kelompokKkn != null) {
			warna = warnas.get(1);
		} else if (kelompokPkl != null) {
			warna = warnas.get(2);
		} else if (mahasiswaRequestTugasAkhir != null) {
			warna = warnas.get(3);
		} else if (skripsi != null) {
			warna = warnas.get(4);
		} else if (krsMahasiswa != null) {
			warna = warnas.get(5);
		} else if (jadwalPelajaran != null) {
			warna = warnas.get(7);
		} else if (formulirKegiatan != null) {
			warna = warnas.get(8);
		} else if (wisuda != null) {
			warna = warnas.get(9);
		} else {
			warna = warnas.get(6);
		}

		return warna;
	}

	/**
	 * Keterangan singkat pertemuan yang SIAP DITAMPILKAN kepada pengguna.
	 *
	 * <p>Inilah padanan "ramah pengguna" dari {@link #toString()}. Kalimatnya disusun sesuai jenis
	 * induk, mis. {@code "Matakuliah Basis Data semester 3 kelas A - Tatap Muka"},
	 * {@code "KKN (Kelompok 7) - Tatap Muka"}, {@code "Bimbingan (Proposal) (Status : ...)"},
	 * {@code "Konsultasi Pembimbing Akademik"}, {@code "Ujian PPDB Gelombang 1"},
	 * {@code "Wisuda <moto>"}, atau {@code "Konsultasi lain"} sebagai cadangan. Nama jenis sesi
	 * ({@link #getStatusPertemuan()}) ditempelkan di ujung pada sebagian besar cabang.</p>
	 *
	 * <p>Hasil akhirnya dilewatkan {@code Common.getBahasaConfig(...)} sehingga istilah dapat
	 * disesuaikan per tenant (mis. "Matakuliah" menjadi istilah lain).</p>
	 *
	 * <p>Dipakai antara lain sebagai judul/isi notifikasi kehadiran yang dirakit
	 * {@link #populate(Long, Statusabsensi, String, PengajuanIzinTidakMasukPerkuliahan, String,
	 * String, String)}.</p>
	 *
	 * <p><b>Catatan:</b> {@code jadwalUjianPMB}, {@code jadwalUjianPSB},
	 * {@code jadwalPertemuanPSB}, dan {@code jadwalUjianPegawai} diuji lewat FIELD tanpa
	 * disegarkan, sehingga cabangnya bisa terlewat bila proxy belum terinisialisasi. Seluruh
	 * exception ditelan dan menghasilkan keterangan kosong.</p>
	 *
	 * @return keterangan siap tampil; string kosong bila penyusunan gagal
	 * @see #toString()
	 * @see #untuk()
	 */
	public String info() {
		perkuliahan = getPerkuliahan();
		kelompokKkn = getKelompokKkn();
		kelompokPkl = getKelompokPkl();
		skripsi = getSkripsi();
		mahasiswaRequestTugasAkhir = getMahasiswaRequestTugasAkhir();
		krsMahasiswa = getKrsMahasiswa();
		formulirKegiatan = getFormulirKegiatan();
		jadwalPelajaran = getJadwalPelajaran();
		wisuda = getWisuda();

		String warna = "";
		try {
			if (perkuliahan != null) {
				warna = "Matakuliah "
						+ (perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama())
						+ " semester " + (perkuliahan.getSemester()) + " kelas " + perkuliahan.getKelas() + " "
						+ (getStatusPertemuan() == null ? "" : " - " + getStatusPertemuan().getNama());
			} else if (kelompokKkn != null) {
				warna = "KKN (" + kelompokKkn.getNama_kelompok() + ")"
						+ (getStatusPertemuan() == null ? "" : " - " + getStatusPertemuan().getNama());
			} else if (kelompokPkl != null) {
				warna = "PKL (" + kelompokPkl.getNama_kelompok() + ")"
						+ (getStatusPertemuan() == null ? "" : " - " + getStatusPertemuan().getNama());
			} else if (skripsi != null) {
				warna = "Sidang / Revisi "
						+ (skripsi.getFormatNilaiSkripsi() == null ? ""
								: "(" + skripsi.getFormatNilaiSkripsi().getNama() + ")")
						+ (getStatusPertemuan() == null ? "" : " - " + getStatusPertemuan().getNama());
			} else if (mahasiswaRequestTugasAkhir != null) {
				warna = "Bimbingan "
						+ (mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() == null ? ""
								: "(" + mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getNama() + ")")
						+ (mahasiswaRequestTugasAkhir.getStatus() == null ? ""
								: " (Status : " + mahasiswaRequestTugasAkhir.getStatus() + ")")
						+ (getStatusPertemuan() == null ? "" : " - " + getStatusPertemuan().getNama());
			} else if (krsMahasiswa != null) {
				warna = "Konsultasi Pembimbing Akademik"
						+ (getStatusPertemuan() == null ? "" : " - " + getStatusPertemuan().getNama());
			} else if (formulirKegiatan != null) {
				warna = "Kegiatan \"" + formulirKegiatan.getNama() + "\""
						+ (getStatusPertemuan() == null ? "" : " - " + getStatusPertemuan().getNama());
			} else if (jadwalPelajaran != null) {
				warna = jadwalPelajaran.getMatapelajaran().getNama() + " "
						+ (getStatusPertemuan() == null ? "" : " - " + getStatusPertemuan().getNama());
			} else if (jadwalUjianPMB != null) {
				warna = "Ujian PMB " + (jadwalUjianPMB.getNama());
			} else if (jadwalUjianPSB != null) {
				warna = "Ujian PPDB " + (jadwalUjianPSB.getNama());
			} else if (jadwalPertemuanPSB != null) {
				warna = "Pertemuan PPDB " + (jadwalPertemuanPSB.getNama());
			} else if (jadwalUjianPegawai != null) {
				warna = "Ujian Calon Pegawai " + (jadwalUjianPegawai.getNama());
			} else if (wisuda != null) {
				warna = "Wisuda " + (wisuda.getMoto());
			} else {
				pertemuanPunyaGrupPertemuan = getPertemuanPunyaGrupPertemuan();
				warna = (pertemuanPunyaGrupPertemuan != null && pertemuanPunyaGrupPertemuan.getGrupPertemuan() != null
						? pertemuanPunyaGrupPertemuan.getGrupPertemuan().getJenis() + " ("
								+ pertemuanPunyaGrupPertemuan.getGrupPertemuan().getNama() + ")"
						: "Konsultasi lain")
						+ (getStatusPertemuan() == null ? "" : " - " + getStatusPertemuan().getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:1917");
			// TODO: handle exception
		}

		return Common.getBahasaConfig(warna);
	}

	/**
	 * Bolehkah rekaman pertemuan ini dipublikasikan lewat modul streaming?
	 *
	 * @return {@code true} bila boleh dipublikasikan; {@code false} bila belum pernah diisi
	 * @see #ambilVideoPertemuanTotal()
	 */
	public Boolean getPublikasikanStreaming() {
		return publikasikanStreaming == null ? false : publikasikanStreaming;
	}

	/**
	 * Setel izin publikasi rekaman pertemuan lewat modul streaming.
	 *
	 * @param publikasikanStreaming {@code true} bila boleh dipublikasikan
	 */
	public void setPublikasikanStreaming(Boolean publikasikanStreaming) {
		this.publikasikanStreaming = publikasikanStreaming;
	}

	/**
	 * Jadwal ujian penerimaan siswa baru yang menjadi induk pertemuan ini.
	 *
	 * @return {@link JadwalUjianPSB} induk, atau {@code null}
	 * @see #untuk()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jadwal_ujian_psb", nullable = true)
	public JadwalUjianPSB getJadwalUjianPSB() {
		return jadwalUjianPSB;
	}

	/**
	 * Tetapkan jadwal ujian PSB sebagai induk pertemuan ini.
	 *
	 * @param jadwalUjianPSB jadwal ujian PSB; boleh {@code null}
	 */
	public void setJadwalUjianPSB(JadwalUjianPSB jadwalUjianPSB) {
		this.jadwalUjianPSB = jadwalUjianPSB;
	}

	/**
	 * Jadwal pelajaran sekolah yang menjadi induk pertemuan ini.
	 *
	 * <p>Induk terpenting kedua setelah {@link Perkuliahan}: keberadaannya mengubah pertemuan dari
	 * konteks perguruan tinggi (dosen/mahasiswa) menjadi konteks sekolah (guru/siswa), dan ikut
	 * memengaruhi peristilahan pada {@link #getIndikator()} serta pemilihan sumber data pada
	 * {@link #ambilGuru()}, {@link #ambilSiswa()}, {@link #getGurus()}, dan {@link #getSiswas()}.</p>
	 *
	 * @return {@link ais.database.model.sekolah.JadwalPelajaran} induk, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_pelajaran", nullable = true)
	public JadwalPelajaran getJadwalPelajaran() {
		jadwalPelajaran = check(jadwalPelajaran);
		return jadwalPelajaran;
	}

	/**
	 * Tetapkan jadwal pelajaran sekolah sebagai induk pertemuan ini.
	 *
	 * @param jadwalPelajaran jadwal pelajaran; boleh {@code null}
	 */
	public void setJadwalPelajaran(JadwalPelajaran jadwalPelajaran) {
		this.jadwalPelajaran = jadwalPelajaran;
	}

	/**
	 * Nama guru tamu pertama (teks bebas, bukan relasi ke {@link Guru}).
	 *
	 * <p>Padanan sekolah dari {@link #getDosenTamu()}.</p>
	 *
	 * @return nama guru tamu, atau {@code null}
	 */
	public String getGuruTamu() {
		return guruTamu;
	}

	/**
	 * Setel nama guru tamu pertama.
	 *
	 * @param guruTamu nama guru tamu
	 */
	public void setGuruTamu(String guruTamu) {
		this.guruTamu = guruTamu;
	}

	/**
	 * Nama guru tamu kedua (teks bebas, bukan relasi ke {@link Guru}).
	 *
	 * @return nama guru tamu kedua, atau {@code null}
	 * @see #getGuruTamu()
	 */
	public String getGuruTamu2() {
		return guruTamu2;
	}

	/**
	 * Setel nama guru tamu kedua.
	 *
	 * @param guruTamu2 nama guru tamu kedua
	 */
	public void setGuruTamu2(String guruTamu2) {
		this.guruTamu2 = guruTamu2;
	}

	/**
	 * Id guru pengganti yang mengisi pertemuan ini menggantikan guru pengampu.
	 *
	 * <p>Padanan sekolah dari {@link #getDosenPengganti()}; disimpan sebagai id mentah, bukan
	 * relasi.</p>
	 *
	 * @return id guru pengganti, atau {@code null}
	 */
	public Long getGuruPengganti() {
		return guruPengganti;
	}

	/**
	 * Setel id guru pengganti.
	 *
	 * @param guruPengganti id guru pengganti
	 */
	public void setGuruPengganti(Long guruPengganti) {
		this.guruPengganti = guruPengganti;
	}

	/**
	 * Id sekolah pertemuan ini, hasil denormalisasi dari induk.
	 *
	 * <p>Ditelusuri dari jadwal pelajaran, atau dari gelombang pendaftaran pada jadwal ujian/
	 * pertemuan PSB. Untuk pertemuan perguruan tinggi nilainya tetap {@code null}.</p>
	 *
	 * <p>Satu-satunya getter denormalisasi di kelas ini yang secara tegas <b>menjaga terhadap proxy
	 * yang belum terinisialisasi</b>: setiap cabang memeriksa {@code Hibernate.isInitialized(...)}
	 * lebih dulu, dan {@code RuntimeException} ditangkap dengan komentar eksplisit bahwa getter ini
	 * juga dipanggil saat objek sedang lepas dari session (audit), sehingga nilai snapshot yang
	 * sudah ada harus dipertahankan alih-alih dihitung ulang.</p>
	 *
	 * @return id sekolah, atau {@code null}
	 * @see #getSekolah()
	 * @see #getYayasanId()
	 */
	public Long getSekolahId() {
		try {
			if (jadwalPelajaran != null && Hibernate.isInitialized(jadwalPelajaran)
					&& jadwalPelajaran.getSekolah() != null) {
				sekolahId = jadwalPelajaran.getSekolah().getId();
			} else if (jadwalUjianPSB != null && Hibernate.isInitialized(jadwalUjianPSB)
					&& jadwalUjianPSB.getGelombangPendaftaranPsb() != null
					&& jadwalUjianPSB.getGelombangPendaftaranPsb().getSekolah() != null) {
				sekolahId = jadwalUjianPSB.getGelombangPendaftaranPsb().getSekolah().getId();
			} else if (jadwalPertemuanPSB != null && Hibernate.isInitialized(jadwalPertemuanPSB)
					&& jadwalPertemuanPSB.getGelombangPendaftaranPsb() != null
					&& jadwalPertemuanPSB.getGelombangPendaftaranPsb().getSekolah() != null) {
				sekolahId = jadwalPertemuanPSB.getGelombangPendaftaranPsb().getSekolah().getId();
			}
		} catch (RuntimeException ignore) {
			// Getter dipanggil juga saat audit objek detached; pertahankan nilai snapshot yang sudah ada.
		}
		return sekolahId;
	}

	/**
	 * Setel id sekolah hasil denormalisasi.
	 *
	 * @param sekolahId id sekolah
	 * @see #getSekolahId()
	 */
	public void setSekolahId(Long sekolahId) {
		this.sekolahId = sekolahId;
	}

	/**
	 * Id yayasan pertemuan ini, hasil denormalisasi dari induk.
	 *
	 * <p>Satu tingkat di atas {@link #getSekolahId()} dalam hierarki organisasi sekolah, dengan
	 * rantai penelusuran yang sama (jadwal pelajaran atau gelombang pendaftaran PSB).</p>
	 *
	 * <p><b>Berbeda dari {@link #getSekolahId()}</b>, method ini TIDAK memeriksa
	 * {@code Hibernate.isInitialized(...)} dan TIDAK punya {@code try/catch}. Pada objek yang
	 * sedang lepas dari session, pemanggilannya dapat melempar
	 * {@code LazyInitializationException} keluar — ketidakselarasan yang layak diingat.</p>
	 *
	 * @return id yayasan, atau {@code null}
	 */
	public Long getYayasanId() {
		if (jadwalPelajaran != null && jadwalPelajaran.getYayasan() != null) {
			yayasanId = jadwalPelajaran.getYayasan().getId();
		} else if (jadwalUjianPSB != null && jadwalUjianPSB.getGelombangPendaftaranPsb() != null
				&& jadwalUjianPSB.getGelombangPendaftaranPsb().getYayasan() != null) {
			yayasanId = jadwalUjianPSB.getGelombangPendaftaranPsb().getYayasan().getId();
		} else if (jadwalPertemuanPSB != null && jadwalPertemuanPSB.getGelombangPendaftaranPsb() != null
				&& jadwalPertemuanPSB.getGelombangPendaftaranPsb().getYayasan() != null) {
			yayasanId = jadwalPertemuanPSB.getGelombangPendaftaranPsb().getYayasan().getId();
		}
		return yayasanId;
	}

	/**
	 * Setel id yayasan hasil denormalisasi.
	 *
	 * @param yayasanId id yayasan
	 * @see #getYayasanId()
	 */
	public void setYayasanId(Long yayasanId) {
		this.yayasanId = yayasanId;
	}

	/**
	 * Bolehkah jam kehadiran (slot mulai/sampai) diperlihatkan kepada peserta didik?
	 *
	 * <p>Saklar tampilan saja; tidak memengaruhi apa yang tersimpan pada string absensi.</p>
	 *
	 * @return {@code true} bila jam absensi boleh ditampilkan; {@code false} bila belum diisi
	 * @see #retreiveAbsensiMulai(Long)
	 */
	public Boolean getTampilkanJamAbsensiBagiMahasiswa() {
		return tampilkanJamAbsensiBagiMahasiswa == null ? false : tampilkanJamAbsensiBagiMahasiswa;
	}

	/**
	 * Setel saklar penampilan jam kehadiran bagi peserta didik.
	 *
	 * @param tampilkanJamAbsensiBagiMahasiswa {@code true} bila jam absensi boleh ditampilkan
	 */
	public void setTampilkanJamAbsensiBagiMahasiswa(Boolean tampilkanJamAbsensiBagiMahasiswa) {
		this.tampilkanJamAbsensiBagiMahasiswa = tampilkanJamAbsensiBagiMahasiswa;
	}

	// ------ PENGAJUAN IZIN

	/**
	 * Baca "peta lokasi" pengajuan izin tidak masuk milik pertemuan ini.
	 *
	 * <p><b>Ini method rujukan untuk seluruh pola {@code ambilLokasiXxx()} di kelas ini</b>
	 * (diskusi, ujian, lampiran, video, audio, tugas, tugas kelompok, kelompok parameter
	 * tambahan) — kuintet lainnya bekerja persis sama dan hanya menautkan balik ke sini.</p>
	 *
	 * <h4>Apa itu "peta lokasi"</h4>
	 * <p>Koleksi anak {@link Pertemuan} TIDAK dipetakan sebagai {@code @OneToMany}. Sebagai
	 * gantinya, tiap jenis anak punya satu berkas JSON di disk (letaknya ditentukan
	 * {@code Common.getFileLocation(this, namaBerkas)}) yang isinya peta {@code "id" -> "id"} —
	 * atau, untuk jenis anak yang isinya ikut di-cache ke berkas, {@code "id" -> "path berkas"}.
	 * Peta itulah yang dibaca alih-alih menjalankan query, sehingga menampilkan daftar pertemuan
	 * beserta jumlah anaknya tidak berubah menjadi ledakan query N+1.</p>
	 *
	 * <p>Berkas yang belum ada, kosong, atau gagal dibaca menghasilkan {@code VOMahasiswa.dataJSON}
	 * (JSON kosong) — bukan {@code null} dan bukan exception. Karena itu pemanggil tidak dapat
	 * membedakan "belum pernah dibangun" dari "sudah dibangun tetapi memang tidak ada isinya";
	 * yang membedakan keduanya adalah penanda {@code udah("PengajuanIzinTidakMasukPerkuliahan")}
	 * dari {@link GeneralValueObject}.</p>
	 *
	 * <p><b>Perhatian:</b> {@code getId()} dipanggil tanpa penjagaan {@code null}, jadi memanggil
	 * method ini pada pertemuan yang belum tersimpan melempar {@link NullPointerException}.
	 * Bandingkan dengan {@link #ambilLokasiPertemuanPunyaDiskusi()} yang sudah menjaganya.</p>
	 *
	 * @return isi peta lokasi sebagai teks JSON; JSON kosong bila belum ada
	 * @see #tulisLokasiPengajuanIzinTidakMasukPerkuliahan(String)
	 * @see #reInitPengajuanIzinTidakMasukPerkuliahan(Session)
	 */
	public String ambilLokasiPengajuanIzinTidakMasukPerkuliahan() {
		File file = Common.getFileLocation(this, "pengajuan_izin_tidak_masuk_perkuliahan_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2028");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Tulis ulang seluruh isi peta lokasi pengajuan izin milik pertemuan ini.
	 *
	 * <p>Menimpa berkas, bukan menambahkan. Pemanggil yang ingin menambah/menghapus SATU entri
	 * harus membaca peta lebih dulu (lewat {@link #jsonObjekUntukTulis(String)}), mengubahnya, lalu
	 * menulis kembali — persis yang dilakukan
	 * {@link #populatePengajuanIzinTidakMasukPerkuliahan(Long)} dan
	 * {@link #removePengajuanIzinTidakMasukPerkuliahan(Serializable)}.</p>
	 *
	 * @param data isi peta lokasi baru sebagai teks JSON
	 * @see #ambilLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public void tulisLokasiPengajuanIzinTidakMasukPerkuliahan(String data) {
		File file = Common.getFileLocation(this, "pengajuan_izin_tidak_masuk_perkuliahan_" + getId().toString());
		ais.common.BacaTulisUtil.tulis(file, data);
	}

	/**
	 * Hapus berkas peta lokasi pengajuan izin milik pertemuan ini.
	 *
	 * <p>Dipanggil di awal {@link #reInitPengajuanIzinTidakMasukPerkuliahan(Session)} sebelum peta
	 * dibangun ulang, dan oleh {@link #refreshData()}. Peta yang hilang akan dibangun ulang
	 * otomatis pada akses berikutnya.</p>
	 *
	 * @see #ambilLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public void bersihkanLokasiPengajuanIzinTidakMasukPerkuliahan() {
		File file = Common.getFileLocation(this, "pengajuan_izin_tidak_masuk_perkuliahan_" + getId().toString());
		BacaTulisUtil.doHapus(file, "pengajuan_izin_tidak_masuk_perkuliahan");

	}

	/**
	 * Bangun ulang peta lokasi pengajuan izin dari basis data.
	 *
	 * <p><b>Ini method rujukan untuk seluruh pola {@code reInitXxx(Session)} di kelas ini.</b>
	 * Alurnya selalu: jalankan satu query proyeksi id anak yang menunjuk ke pertemuan ini (terurut
	 * menurut id), hapus peta lama, tulis peta kosong, lalu masukkan tiap id lewat
	 * {@code populateXxx(...)}.</p>
	 *
	 * <p><b>Mahal, dan hanya boleh dijalankan bila perlu.</b> Pemanggil di kelas ini selalu
	 * membungkusnya dengan pemeriksaan {@code if (!udah("PengajuanIzinTidakMasukPerkuliahan"))}
	 * sehingga pembangunan ulang hanya terjadi sekali, sampai penanda itu dibatalkan dengan
	 * {@code belum(...)}. Salah menuliskan nama penanda berarti peta dibangun ulang pada SETIAP
	 * akses.</p>
	 *
	 * <p>Varian ini menulis berkas berkali-kali (sekali per anak) karena
	 * {@link #populatePengajuanIzinTidakMasukPerkuliahan(Long)} membaca-ubah-tulis setiap kali.
	 * Bandingkan dengan {@link #reInitPertemuanPunyaDiskusi(Session)} yang sudah dioptimalkan
	 * merakit JSON di memori dan menulis SEKALI saja.</p>
	 *
	 * @param session session Hibernate yang sudah terbuka; tidak ditutup oleh method ini
	 * @see #ambilPengajuanIzinTidakMasukPerkuliahanTotal()
	 */
	@SuppressWarnings("unchecked")
	public void reInitPengajuanIzinTidakMasukPerkuliahan(Session session) {
		List<Long> pengajuanIzinTidakMasukPerkuliahans = session
				.createCriteria(PengajuanIzinTidakMasukPerkuliahan.class).addOrder(Order.asc("id"))
				.setProjection(Projections.property("id")).add(Restrictions.eq("pertemuan", this)).list();
		bersihkanLokasiPengajuanIzinTidakMasukPerkuliahan();
		tulisLokasiPengajuanIzinTidakMasukPerkuliahan(new JSONObject().toString());
		for (Long pengajuanIzinTidakMasukPerkuliahanid : pengajuanIzinTidakMasukPerkuliahans) {
			populatePengajuanIzinTidakMasukPerkuliahan(pengajuanIzinTidakMasukPerkuliahanid);
		}
		pengajuanIzinTidakMasukPerkuliahans = null;
	}

	/**
	 * Tandai satu pengajuan izin sebagai TIDAK ADA lagi di peta lokasi pertemuan ini.
	 *
	 * <p><b>Ini method rujukan untuk seluruh pola {@code removeXxx(...)} di kelas ini.</b>
	 * Perhatikan bahwa entri tidak benar-benar dibuang dari peta: nilainya diganti string KOSONG.
	 * Semua pembaca memang melewati entri bernilai kosong, jadi hasil akhirnya sama, tetapi
	 * berkas peta terus membesar seiring waktu dan baru menyusut ketika {@code reInitXxx(...)}
	 * membangunnya ulang.</p>
	 *
	 * <p>Method ini hanya menyentuh peta lokasi; ia TIDAK menghapus baris anaknya dari basis data.
	 * Penghapusan baris adalah tanggung jawab pemanggil.</p>
	 *
	 * <p>Peta yang isinya rusak menyebabkan {@link #jsonObjekUntukTulis(String)} melempar, dan
	 * exception itu tertangkap di sini sehingga penyimpanan DIBATALKAN — peta lama sengaja
	 * dibiarkan utuh agar tidak tertimpa peta baru yang kosong.</p>
	 *
	 * @param id id anak yang dikeluarkan dari peta
	 * @see #populatePengajuanIzinTidakMasukPerkuliahan(Long)
	 */
	public void removePengajuanIzinTidakMasukPerkuliahan(Serializable id) {
		try {
			JSONObject c = jsonObjekUntukTulis(ambilLokasiPengajuanIzinTidakMasukPerkuliahan());
			c.put(id.toString(), "");
			tulisLokasiPengajuanIzinTidakMasukPerkuliahan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2062");

		}
	}

	/**
	 * Daftarkan satu pengajuan izin ke peta lokasi pertemuan ini.
	 *
	 * <p><b>Ini method rujukan untuk seluruh pola {@code populateXxx(...)} di kelas ini.</b>
	 * Membaca peta, menambahkan pasangan {@code "id" -> "id"}, lalu menulisnya kembali. Id yang
	 * sudah ada akan tertulis ulang dengan nilai yang sama, jadi memanggilnya berulang aman.</p>
	 *
	 * <p>Argumen {@code null} membuat method langsung berhenti tanpa mengubah apa pun. Kegagalan
	 * apa pun ditelan sehingga pemanggil tidak akan tahu bila pendaftaran gagal — akibatnya anak
	 * yang baru tersimpan bisa "tidak terlihat" sampai peta dibangun ulang.</p>
	 *
	 * @param pengajuanIzinTidakMasukPerkuliahan id pengajuan izin yang didaftarkan; {@code null}
	 *                                           diabaikan
	 * @see #removePengajuanIzinTidakMasukPerkuliahan(Serializable)
	 */
	public void populatePengajuanIzinTidakMasukPerkuliahan(Long pengajuanIzinTidakMasukPerkuliahan) {
		try {
			if (pengajuanIzinTidakMasukPerkuliahan == null) {
				return;
			}

			JSONObject c = jsonObjekUntukTulis(ambilLokasiPengajuanIzinTidakMasukPerkuliahan());
			c.put(pengajuanIzinTidakMasukPerkuliahan.toString(), pengajuanIzinTidakMasukPerkuliahan.toString());
			tulisLokasiPengajuanIzinTidakMasukPerkuliahan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2076");
		}
	}

	/**
	 * Adakah setidaknya satu pengajuan izin tidak masuk pada pertemuan ini?
	 *
	 * <p><b>Tidak hemat:</b> dijawab dengan memuat SELURUH daftar lewat
	 * {@link #ambilPengajuanIzinTidakMasukPerkuliahanTotal()} lalu memeriksa ukurannya. Bila yang
	 * dibutuhkan hanya jumlah, {@link #ambilJumlahPengajuanIzinTidakMasukPerkuliahan()} lebih
	 * murah karena tidak memuat objeknya.</p>
	 *
	 * @return {@code true} bila ada minimal satu pengajuan izin
	 */
	public boolean punyaPengajuanIzinTidakMasukPerkuliahan() {
		List<PengajuanIzinTidakMasukPerkuliahan> pengajuanIzinTidakMasukPerkuliahansa = ambilPengajuanIzinTidakMasukPerkuliahanTotal();
		int ada = pengajuanIzinTidakMasukPerkuliahansa.size();
		pengajuanIzinTidakMasukPerkuliahansa = null;
		return ada > 0;
	}

	/**
	 * Jumlah SELURUH pengajuan izin tidak masuk pada pertemuan ini.
	 *
	 * @return banyaknya pengajuan izin
	 * @see #ambilJumlahPengajuanIzinTidakMasukPerkuliahan(Mahasiswa)
	 */
	public int ambilJumlahPengajuanIzinTidakMasukPerkuliahan() {
		return ambilJumlahPengajuanIzinTidakMasukPerkuliahan(null);
	}

	/**
	 * Jumlah pengajuan izin tidak masuk pada pertemuan ini, dengan penyaring mahasiswa opsional.
	 *
	 * <p>Membangun ulang peta lokasi lebih dulu bila penandanya belum ada. Setelah itu peta
	 * ditelusuri:</p>
	 * <ul>
	 *   <li>{@code mahasiswa == null} — tiap entri yang tidak kosong dihitung LANGSUNG dari peta,
	 *       tanpa memuat objek anaknya. Ini jalur yang murah.</li>
	 *   <li>{@code mahasiswa != null} — tiap entri dimuat objeknya lewat {@code ambilData(...)}
	 *       untuk membandingkan pemiliknya. Jauh lebih mahal, dan {@code NullPointerException}
	 *       akan terjadi (lalu ditelan, sehingga entri itu terlewat) bila objek anaknya sudah
	 *       tidak ada.</li>
	 * </ul>
	 *
	 * <p>Setiap anak yang dimuat di-{@code setPertemuan(this)} agar tidak perlu memuat ulang
	 * induknya dari basis data.</p>
	 *
	 * @param mahasiswa mahasiswa yang disaring; {@code null} berarti hitung semua
	 * @return banyaknya pengajuan izin yang cocok
	 */
	@SuppressWarnings("unchecked")
	public int ambilJumlahPengajuanIzinTidakMasukPerkuliahan(Mahasiswa mahasiswa) {
		if (!udah("PengajuanIzinTidakMasukPerkuliahan")) {
			reInitPengajuanIzinTidakMasukPerkuliahan(HibernateUtil.currentSession());
		}
		int jumlah = 0;
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiPengajuanIzinTidakMasukPerkuliahan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						if (mahasiswa != null) {
							PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = (PengajuanIzinTidakMasukPerkuliahan) ambilData(
									PengajuanIzinTidakMasukPerkuliahan.class, s, true);
							pengajuanIzinTidakMasukPerkuliahan.setPertemuan(this);
							if (pengajuanIzinTidakMasukPerkuliahan.getMahasiswa() != null
									&& pengajuanIzinTidakMasukPerkuliahan.getMahasiswa().getId()
											.equals(mahasiswa.getId())) {
								jumlah++;
							}
							pengajuanIzinTidakMasukPerkuliahan = null;
						} else {
							jumlah++;
						}

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2120");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2124");

		}
		return jumlah;
	}

	/**
	 * Cari pengajuan izin tidak masuk milik seorang SISWA pada pertemuan ini.
	 *
	 * <p>Menelusuri peta lokasi (dibangun ulang lebih dulu bila perlu), memuat tiap anak, dan
	 * berhenti pada yang pertama cocok.</p>
	 *
	 * <p><b>Perhatikan:</b> bila {@code siswa} bernilai {@code null}, seluruh badan perulangan
	 * dilewati sehingga hasilnya selalu {@code null} — bukan "ambil sembarang". Ini berbeda dari
	 * konvensi {@code null == semua} yang dipakai
	 * {@link #ambilJumlahPengajuanIzinTidakMasukPerkuliahan(Mahasiswa)}.</p>
	 *
	 * @param siswa siswa yang dicari izinnya
	 * @return pengajuan izin milik siswa itu, atau {@code null} bila tidak ada
	 * @see #ambilPengajuanIzinTidakMasukPerkuliahan(VOMahasiswa)
	 */
	@SuppressWarnings("unchecked")
	public PengajuanIzinTidakMasukPerkuliahan ambilPengajuanIzinTidakMasukPerkuliahan(Siswa siswa) {

		if (!udah("PengajuanIzinTidakMasukPerkuliahan")) {
			reInitPengajuanIzinTidakMasukPerkuliahan(HibernateUtil.currentSession());
		}

		PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahanMahasiswa = null;
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiPengajuanIzinTidakMasukPerkuliahan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						if (siswa != null) {
							PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = (PengajuanIzinTidakMasukPerkuliahan) ambilData(
									PengajuanIzinTidakMasukPerkuliahan.class, s, true);
							pengajuanIzinTidakMasukPerkuliahan.setPertemuan(this);
							if (pengajuanIzinTidakMasukPerkuliahan.getSiswa() != null
									&& pengajuanIzinTidakMasukPerkuliahan.getSiswa().getId().equals(siswa.getId())) {
								pengajuanIzinTidakMasukPerkuliahanMahasiswa = pengajuanIzinTidakMasukPerkuliahan;
								break;
							}
						}

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2159");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2163");

		}
		return pengajuanIzinTidakMasukPerkuliahanMahasiswa;
	}

	/**
	 * Cari pengajuan izin tidak masuk milik seorang MAHASISWA pada pertemuan ini.
	 *
	 * <p>Kembaran {@link #ambilPengajuanIzinTidakMasukPerkuliahan(Siswa)} untuk jenjang perguruan
	 * tinggi, dengan alur dan keterbatasan yang sama (termasuk: {@code null} menghasilkan
	 * {@code null}, bukan "ambil sembarang").</p>
	 *
	 * @param mahasiswa mahasiswa yang dicari izinnya
	 * @return pengajuan izin milik mahasiswa itu, atau {@code null} bila tidak ada
	 */
	@SuppressWarnings("unchecked")
	public PengajuanIzinTidakMasukPerkuliahan ambilPengajuanIzinTidakMasukPerkuliahan(VOMahasiswa mahasiswa) {

		if (!udah("PengajuanIzinTidakMasukPerkuliahan")) {
			reInitPengajuanIzinTidakMasukPerkuliahan(HibernateUtil.currentSession());
		}

		PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahanMahasiswa = null;
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiPengajuanIzinTidakMasukPerkuliahan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						if (mahasiswa != null) {
							PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan = (PengajuanIzinTidakMasukPerkuliahan) ambilData(
									PengajuanIzinTidakMasukPerkuliahan.class, s, true);
							pengajuanIzinTidakMasukPerkuliahan.setPertemuan(this);
							if (pengajuanIzinTidakMasukPerkuliahan.getMahasiswa() != null
									&& pengajuanIzinTidakMasukPerkuliahan.getMahasiswa().getId()
											.equals(mahasiswa.getId())) {
								pengajuanIzinTidakMasukPerkuliahanMahasiswa = pengajuanIzinTidakMasukPerkuliahan;
								break;
							}
						}

					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2199");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2203");

		}
		return pengajuanIzinTidakMasukPerkuliahanMahasiswa;
	}

	/**
	 * Seluruh pengajuan izin tidak masuk pada pertemuan ini.
	 *
	 * <p>Mengumpulkan lebih dulu seluruh kunci dari peta lokasi, lalu memuat objeknya SEKALIGUS
	 * lewat {@code ambilDataBanyak(...)} — bukan satu per satu di dalam perulangan. Ini pola yang
	 * lebih baik daripada beberapa pembaca lain di kelas ini dan sebaiknya ditiru bila menambah
	 * koleksi anak baru.</p>
	 *
	 * @return daftar pengajuan izin; kosong bila tidak ada
	 * @see #punyaPengajuanIzinTidakMasukPerkuliahan()
	 */
	@SuppressWarnings("unchecked")
	public List<PengajuanIzinTidakMasukPerkuliahan> ambilPengajuanIzinTidakMasukPerkuliahanTotal() {
		if (!udah("PengajuanIzinTidakMasukPerkuliahan")) {
			reInitPengajuanIzinTidakMasukPerkuliahan(HibernateUtil.currentSession());
		}
		List<String> keysData = new ArrayList<String>();
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiPengajuanIzinTidakMasukPerkuliahan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						keysData.add(s);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2225");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2229");

		}
		List<PengajuanIzinTidakMasukPerkuliahan> pengajuanIzinTidakMasukPerkuliahansa = ambilDataBanyak(
				PengajuanIzinTidakMasukPerkuliahan.class, keysData);
		return pengajuanIzinTidakMasukPerkuliahansa;
	}

	// ------ PENGAJUAN IZIN

	/**
	 * Baca peta lokasi diskusi ({@link PertemuanPunyaDiskusi}) milik pertemuan ini.
	 *
	 * <p>Mengikuti pola yang dijelaskan lengkap pada
	 * {@link #ambilLokasiPengajuanIzinTidakMasukPerkuliahan()}, dengan satu perbaikan penting:
	 * pertemuan yang belum punya id langsung mengembalikan JSON kosong alih-alih melempar
	 * {@link NullPointerException}.</p>
	 *
	 * @return isi peta lokasi diskusi sebagai teks JSON; JSON kosong bila belum ada
	 * @see #ambilLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public String ambilLokasiPertemuanPunyaDiskusi() {
		if (getId() == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(this, "pertemuan_punya_diskusi_" + getId().toString());
		try {
			String data = BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2244");
			// Catch sengaja diabaikan agar me-return fallback default JSON jika file tidak
			// ada/rusak
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Tulis ulang seluruh isi peta lokasi diskusi milik pertemuan ini.
	 *
	 * @param data isi peta lokasi baru sebagai teks JSON
	 * @see #tulisLokasiPengajuanIzinTidakMasukPerkuliahan(String)
	 */
	public void tulisLokasiPertemuanPunyaDiskusi(String data) {
		File file = Common.getFileLocation(this, "pertemuan_punya_diskusi_" + getId().toString());
		BacaTulisUtil.tulis(file, data);
	}

	/**
	 * Hapus berkas peta lokasi diskusi milik pertemuan ini.
	 *
	 * @see #bersihkanLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public void bersihkanLokasiPertemuanPunyaDiskusi() {
		File file = Common.getFileLocation(this, "pertemuan_punya_diskusi_" + getId().toString());
		BacaTulisUtil.doHapus(file, "pertemuan_punya_diskusi");
	}

	/**
	 * Bangun ulang peta lokasi diskusi dari basis data.
	 *
	 * <p>Varian yang <b>sudah dioptimalkan</b>: berbeda dari
	 * {@link #reInitPengajuanIzinTidakMasukPerkuliahan(Session)} yang menulis berkas sekali per
	 * anak, method ini merakit seluruh {@link JSONObject} di memori lalu menulis ke disk SATU KALI
	 * saja. Pola inilah yang sebaiknya diikuti bila menambah/memperbaiki kuintet lain.</p>
	 *
	 * @param session session Hibernate yang sudah terbuka; tidak ditutup oleh method ini
	 * @see #reInitPengajuanIzinTidakMasukPerkuliahan(Session)
	 */
	@SuppressWarnings("unchecked")
	public void reInitPertemuanPunyaDiskusi(Session session) {
		List<Long> pertemuanPunyaDiskusis = session.createCriteria(PertemuanPunyaDiskusi.class)
				.setProjection(Projections.property("id")).addOrder(Order.asc("id"))
				.add(Restrictions.eq("pertemuan", this)).list();

		bersihkanLokasiPertemuanPunyaDiskusi();

		// OPTIMASI EKSTREM I/O & MEMORY:
		// Merakit JSON di RAM 1x dan hanya menulis ke file 1x, bukan berkali-kali di
		// dalam loop.
		try {
			JSONObject c = new JSONObject();
			for (Long id : pertemuanPunyaDiskusis) {
				if (id != null) {
					c.put(id.toString(), id.toString());
				}
			}
			tulisLokasiPertemuanPunyaDiskusi(c.toString());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:2281");
		}
		pertemuanPunyaDiskusis = null;
	}

	/**
	 * Keluarkan satu diskusi dari peta lokasi pertemuan ini.
	 *
	 * <p>Sedikit lebih rapi daripada {@link #removePengajuanIzinTidakMasukPerkuliahan(Serializable)}:
	 * berkas hanya ditulis ulang bila kuncinya memang ada di peta, dan {@code id} bernilai
	 * {@code null} ditolak lebih dahulu.</p>
	 *
	 * @param id id diskusi yang dikeluarkan; {@code null} diabaikan
	 * @see #removePengajuanIzinTidakMasukPerkuliahan(Serializable)
	 */
	public void removePertemuanPunyaDiskusi(Serializable id) {
		if (id == null)
			return;
		try {
			JSONObject c = jsonObjekUntukTulis(ambilLokasiPertemuanPunyaDiskusi());
			if (c.has(id.toString())) {
				c.put(id.toString(), "");
				tulisLokasiPertemuanPunyaDiskusi(c.toString());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2295");
		}
	}

	/**
	 * Daftarkan satu diskusi ke peta lokasi pertemuan ini.
	 *
	 * <p><b>Parameter {@code tulisUlang} TIDAK DIPAKAI sama sekali</b> di badan method — peta
	 * selalu ditulis ulang. Parameter itu ada demi keseragaman tanda tangan dengan
	 * {@code populateXxx(...)} lain di kelas ini; jangan berharap nilainya mengubah perilaku.</p>
	 *
	 * @param pertemuanPunyaDiskusiid id diskusi yang didaftarkan; {@code null} diabaikan
	 * @param tulisUlang              tidak dipakai
	 * @see #populatePengajuanIzinTidakMasukPerkuliahan(Long)
	 */
	public void populatePertemuanPunyaDiskusi(Long pertemuanPunyaDiskusiid, boolean tulisUlang) {
		if (pertemuanPunyaDiskusiid == null)
			return;
		try {
			JSONObject c = jsonObjekUntukTulis(ambilLokasiPertemuanPunyaDiskusi());
			c.put(pertemuanPunyaDiskusiid.toString(), pertemuanPunyaDiskusiid.toString());
			tulisLokasiPertemuanPunyaDiskusi(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2306");
		}
	}

	/**
	 * Adakah setidaknya satu diskusi pada pertemuan ini?
	 *
	 * <p>Memuat himpunan id diskusi lewat {@link #ambilPertemuanPunyaDiskusiTotal(boolean)} lalu
	 * memeriksa apakah kosong. Perhatikan bahwa method itu, bila peta lokasi menyimpan id yang
	 * belum ada di cache, akan MEMBUKA SESSION dan menjalankan query — jadi pertanyaan yang
	 * terdengar sepele ini bisa menyentuh basis data.</p>
	 *
	 * @return {@code true} bila ada minimal satu diskusi
	 */
	public boolean punyaDiskusi() {
		TreeSet<Long> pertemuanPunyaDiskusisa = ambilPertemuanPunyaDiskusiTotal(false);
		boolean ada = (pertemuanPunyaDiskusisa != null && !pertemuanPunyaDiskusisa.isEmpty());
		pertemuanPunyaDiskusisa = null;
		return ada;
	}

	/**
	 * Jumlah SELURUH diskusi pada pertemuan ini.
	 *
	 * @return banyaknya diskusi
	 * @see #ambilJumlahPertemuanPunyaDiskusi(Mahasiswa, Dosen)
	 */
	public int ambilJumlahPertemuanPunyaDiskusi() {
		return ambilJumlahPertemuanPunyaDiskusi(null, null);
	}

	/**
	 * Jumlah diskusi pada pertemuan ini, dengan penyaring penulis opsional.
	 *
	 * <p>Menghitung dalam dua tahap:</p>
	 * <ol>
	 *   <li><b>Dari cache.</b> Peta lokasi ditelusuri; untuk tiap kunci, objek diskusinya dicari di
	 *       cache lewat {@code ambilData(...)}. Bila ketemu, pemiliknya dibandingkan dengan
	 *       penyaring. Bila TIDAK ketemu, id-nya dikumpulkan ke daftar {@code idsBelumAda}.</li>
	 *   <li><b>Susulan ke basis data.</b> Bila {@code idsBelumAda} tidak kosong, satu session baru
	 *       dibuka dan diskusi yang belum ter-cache diambil dengan klausa {@code IN}, DIPOTONG per
	 *       999 id. Pemotongan ini disengaja untuk menghindari batas jumlah item klausa {@code IN}
	 *       pada basis data (komentar di kode menyebut ORA-01795). Hasilnya dimasukkan ke cache
	 *       lewat {@code masukkanData(...)} agar pemanggilan berikutnya tidak perlu query lagi.</li>
	 * </ol>
	 *
	 * <p>Bila kedua penyaring {@code null} (hitung semua), tahap dua tetap dijalankan untuk id yang
	 * belum ter-cache tetapi hasilnya TIDAK menambah hitungan — karena penambahan hanya terjadi di
	 * dalam cabang {@code mahasiswa != null}/{@code dosen != null}. Padahal pada tahap satu, entri
	 * peta yang tidak kosong sudah dihitung lebih dulu, sehingga totalnya tetap benar; query
	 * susulannya sendiri menjadi pekerjaan yang mubazir untuk kasus ini.</p>
	 *
	 * <p>Pertemuan yang belum punya id langsung menghasilkan {@code 0}. Session susulan selalu
	 * ditutup di blok {@code finally}.</p>
	 *
	 * @param mahasiswa mahasiswa penulis yang disaring; {@code null} berarti tidak menyaring
	 * @param dosen     dosen penulis yang disaring; {@code null} berarti tidak menyaring
	 * @return banyaknya diskusi yang cocok
	 */
	@SuppressWarnings("unchecked")
	public int ambilJumlahPertemuanPunyaDiskusi(Mahasiswa mahasiswa, Dosen dosen) {
		if (getId() == null) {
			return 0;
		}
		int jumlah = 0;
		List<Long> idsBelumAda = new ArrayList<Long>();
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiPertemuanPunyaDiskusi());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (s != null && !s.trim().isEmpty()) {
						if (mahasiswa != null) {
							GeneralValueObject generalValueObject = ambilData(PertemuanPunyaDiskusi.class, key);
							if (generalValueObject != null) {
								PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) generalValueObject;
								pertemuanPunyaDiskusi.setPertemuan(this);
								if (pertemuanPunyaDiskusi.getMahasiswa() != null
										&& pertemuanPunyaDiskusi.getMahasiswa().getId().equals(mahasiswa.getId())) {
									jumlah++;
								}
							} else {
								idsBelumAda.add(Long.parseLong(key));
							}
						} else if (dosen != null) {
							GeneralValueObject generalValueObject = ambilData(PertemuanPunyaDiskusi.class, key);
							if (generalValueObject != null) {
								PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) generalValueObject;
								pertemuanPunyaDiskusi.setPertemuan(this);
								if (pertemuanPunyaDiskusi.getDosen() != null
										&& pertemuanPunyaDiskusi.getDosen().getId().equals(dosen.getId())) {
									jumlah++;
								}
							} else {
								idsBelumAda.add(Long.parseLong(key));
							}
						} else {
							jumlah++;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2361");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2364");
		}

		if (!idsBelumAda.isEmpty()) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();

				// OPTIMASI: Batas maksimal In Clause dipotong per 999 untuk mencegah error
				// ORA-01795 di Database
				int maxBatch = 999;
				for (int i = 0; i < idsBelumAda.size(); i += maxBatch) {
					List<Long> batchList = idsBelumAda.subList(i, Math.min(idsBelumAda.size(), i + maxBatch));
					List<PertemuanPunyaDiskusi> pertemuanPunyaDiskusis = session
							.createCriteria(PertemuanPunyaDiskusi.class).add(Restrictions.in("id", batchList)).list();

					for (PertemuanPunyaDiskusi pertemuanPunyaDiskusi : pertemuanPunyaDiskusis) {
						masukkanData(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusi);
						pertemuanPunyaDiskusi.setPertemuan(this);

						if (mahasiswa != null) {
							if (pertemuanPunyaDiskusi.getMahasiswa() != null
									&& pertemuanPunyaDiskusi.getMahasiswa().getId().equals(mahasiswa.getId())) {
								jumlah++;
							}
						} else if (dosen != null) {
							if (pertemuanPunyaDiskusi.getDosen() != null
									&& pertemuanPunyaDiskusi.getDosen().getId().equals(dosen.getId())) {
								jumlah++;
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:2398");
			} finally {
				if (session != null && session.isOpen()) {
					try {
						session.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2403");
					}
				}
			}
		}

		return jumlah;
	}

	/**
	 * Kumpulkan id seluruh diskusi dari BANYAK pertemuan sekaligus, sambil melaporkan kemajuannya
	 * ke sebuah label ZK.
	 *
	 * <p>Satu-satunya method statis pada keluarga diskusi. Dipakai halaman yang menampilkan forum
	 * gabungan lintas pertemuan (mis. seluruh pertemuan satu kelas kuliah), sehingga peta lokasi
	 * tiap pertemuan dibaca berurutan dan hasilnya digabung ke satu himpunan.</p>
	 *
	 * <p>Sama seperti {@link #ambilJumlahPertemuanPunyaDiskusi(Mahasiswa, Dosen)}, id yang belum
	 * ada di cache dikumpulkan dan diambil menyusul dengan klausa {@code IN} yang dipotong per 999.
	 * Pada tahap susulan itu ada satu perbedaan yang mudah terlewat: diskusi hanya dimasukkan bila
	 * {@code getIsi()} TIDAK kosong, sedangkan pada tahap pembacaan cache semua diskusi dimasukkan
	 * tanpa memeriksa isinya. Jadi diskusi berisi kosong bisa ikut atau tidak ikut tergantung
	 * kebetulan sudah ter-cache atau belum.</p>
	 *
	 * <p>Bila {@code label} diberikan, teks kemajuan ({@code "Ambil data NN%"}) ditulis ke label
	 * itu pada setiap pertemuan. Karena method ini menyentuh komponen ZK, ia hanya aman dipanggil
	 * dari thread yang memang memegang desktop ZK tersebut.</p>
	 *
	 * @param urutkan    {@code true} untuk urutan id menaik; {@code false} untuk menurun
	 *                   (yang terbaru lebih dulu)
	 * @param pertemuans id pertemuan yang dijelajahi; {@code null}/kosong menghasilkan himpunan
	 *                   kosong
	 * @param label      label ZK penampil kemajuan; boleh {@code null}
	 * @return himpunan id diskusi, terurut sesuai {@code urutkan}
	 * @see #ambilPertemuanPunyaDiskusiTotal(boolean)
	 */
	@SuppressWarnings("unchecked")
	public static TreeSet<Long> ambilPertemuanPunyaDiskusiTotalSemua(boolean urutkan, Collection<Long> pertemuans,
			Label label) {
		TreeSet<Long> pertemuanPunyaDiskusisa = urutkan ? new TreeSet<Long>()
				: new TreeSet<Long>(Collections.reverseOrder());

		if (pertemuans == null || pertemuans.isEmpty()) {
			return pertemuanPunyaDiskusisa;
		}

		List<Long> idsBelumAda = new ArrayList<Long>();
		int size = pertemuans.size();
		int index = 0;
		for (Long pertemuanid : pertemuans) {
			if (pertemuanid == null)
				continue;
			try {
				index++;
				if (label != null) {
					String s = "Ambil data " + Common.numberFormat.get().format((index * 100.0) / size) + "%";
					label.setValue(s);
				}

				// System menggunakan method static GeneralValueObject
				GeneralValueObject voObj = GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (voObj != null) {
					Pertemuan pertemuan = (Pertemuan) voObj;
					JSONObject c = jsonObjekAtauKosong(pertemuan.ambilLokasiPertemuanPunyaDiskusi());
					Iterator<String> keys = c.keys();
					while (keys.hasNext()) {
						String key = keys.next();
						try {
							String s = c.getString(key);
							if (s != null && !s.trim().isEmpty()) {
								GeneralValueObject generalValueObject = ambilData(PertemuanPunyaDiskusi.class, key);
								if (generalValueObject != null) {
									PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) generalValueObject;
									pertemuanPunyaDiskusisa.add(pertemuanPunyaDiskusi.getId());
								} else {
									idsBelumAda.add(Long.parseLong(key));
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2454");
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2458");
			}
		}

		if (!idsBelumAda.isEmpty()) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();

				// OPTIMASI: Chunking list to max 999. Anti-crash database.
				int maxBatch = 999;
				for (int i = 0; i < idsBelumAda.size(); i += maxBatch) {
					List<Long> batchList = idsBelumAda.subList(i, Math.min(idsBelumAda.size(), i + maxBatch));
					List<PertemuanPunyaDiskusi> pertemuanPunyaDiskusis = session
							.createCriteria(PertemuanPunyaDiskusi.class).add(Restrictions.in("id", batchList)).list();

					for (PertemuanPunyaDiskusi pertemuanPunyaDiskusi : pertemuanPunyaDiskusis) {
						masukkanData(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusi);
						if (pertemuanPunyaDiskusi != null && pertemuanPunyaDiskusi.getIsi() != null
								&& !pertemuanPunyaDiskusi.getIsi().isEmpty()) {
							pertemuanPunyaDiskusisa.add(pertemuanPunyaDiskusi.getId());
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:2483");
			} finally {
				if (session != null && session.isOpen()) {
					try {
						session.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2488");
					}
				}
			}
		}

		return pertemuanPunyaDiskusisa;
	}

	/**
	 * Kumpulkan id seluruh diskusi pada pertemuan INI.
	 *
	 * <p>Versi satu-pertemuan dari
	 * {@link #ambilPertemuanPunyaDiskusiTotalSemua(boolean, Collection, Label)}, dengan strategi
	 * dua tahap yang sama (baca cache, lalu ambil susulan per 999 id) dan ketidakselarasan
	 * penyaringan {@code getIsi()} yang sama.</p>
	 *
	 * <p>Berbeda dari versi statisnya, tiap diskusi yang ditemukan di-{@code setPertemuan(this)}
	 * sehingga induknya tidak perlu dimuat ulang.</p>
	 *
	 * <p><b>Catatan:</b> method ini TIDAK memeriksa {@code udah("...")} maupun memanggil
	 * {@link #reInitPertemuanPunyaDiskusi(Session)}. Peta lokasi yang belum pernah dibangun
	 * menghasilkan himpunan kosong — pembangunannya diserahkan ke {@link #refreshData()} atau ke
	 * alur yang menambah diskusi.</p>
	 *
	 * @param urutkan {@code true} untuk urutan id menaik; {@code false} untuk menurun
	 * @return himpunan id diskusi
	 */
	@SuppressWarnings("unchecked")
	public TreeSet<Long> ambilPertemuanPunyaDiskusiTotal(boolean urutkan) {
		TreeSet<Long> pertemuanPunyaDiskusisa = urutkan ? new TreeSet<Long>()
				: new TreeSet<Long>(Collections.reverseOrder());
		List<Long> idsBelumAda = new ArrayList<Long>();
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiPertemuanPunyaDiskusi());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (s != null && !s.trim().isEmpty()) {
						GeneralValueObject generalValueObject = ambilData(PertemuanPunyaDiskusi.class, key);
						if (generalValueObject != null) {
							PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) generalValueObject;
							pertemuanPunyaDiskusi.setPertemuan(this);
							pertemuanPunyaDiskusisa.add(pertemuanPunyaDiskusi.getId());
						} else {
							idsBelumAda.add(Long.parseLong(key));
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2519");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2522");
		}

		if (!idsBelumAda.isEmpty()) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();

				int maxBatch = 999;
				for (int i = 0; i < idsBelumAda.size(); i += maxBatch) {
					List<Long> batchList = idsBelumAda.subList(i, Math.min(idsBelumAda.size(), i + maxBatch));
					List<PertemuanPunyaDiskusi> pertemuanPunyaDiskusis = session
							.createCriteria(PertemuanPunyaDiskusi.class).add(Restrictions.in("id", batchList)).list();

					for (PertemuanPunyaDiskusi pertemuanPunyaDiskusi : pertemuanPunyaDiskusis) {
						masukkanData(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusi);
						if (pertemuanPunyaDiskusi != null && pertemuanPunyaDiskusi.getIsi() != null
								&& !pertemuanPunyaDiskusi.getIsi().isEmpty()) {
							pertemuanPunyaDiskusi.setPertemuan(this);
							pertemuanPunyaDiskusisa.add(pertemuanPunyaDiskusi.getId());
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:2546");
			} finally {
				if (session != null && session.isOpen()) {
					try {
						session.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2551");
					}
				}
			}
		}

		return pertemuanPunyaDiskusisa;
	}

	/**
	 * Saring satu tingkat hierarki diskusi lalu potong satu halaman darinya.
	 *
	 * <p>Diskusi tersusun sebagai pohon: diskusi akar tidak punya {@code parent}, balasan
	 * menunjuk ke diskusi induknya. Method ini menyaring himpunan id yang sudah dikumpulkan
	 * {@link #ambilPertemuanPunyaDiskusiTotal(boolean)} menjadi satu tingkat saja:</p>
	 * <ul>
	 *   <li>{@code parent == null} — ambil diskusi AKAR (yang {@code parent}-nya juga
	 *       {@code null});</li>
	 *   <li>{@code parent != null} — ambil balasan LANGSUNG dari diskusi itu.</li>
	 * </ul>
	 *
	 * <p>Penomoran halaman dihitung SETELAH penyaringan, sehingga {@code mulai} mengacu pada
	 * urutan di dalam tingkat yang bersangkutan, bukan pada seluruh himpunan.</p>
	 *
	 * <p>Yang dikembalikan adalah daftar ID, bukan objeknya — pemanggil memuat sendiri objek yang
	 * benar-benar ditampilkan.</p>
	 *
	 * @param parent                  diskusi induk; {@code null} berarti ambil tingkat akar
	 * @param pertemuanPunyaDiskusisa himpunan id yang akan disaring; {@code null}/kosong
	 *                                menghasilkan daftar kosong
	 * @param mulai                   indeks awal halaman (berbasis nol) di dalam tingkat itu
	 * @param banyak                  banyaknya entri per halaman
	 * @return daftar id diskusi pada halaman yang diminta
	 */
	public List<Long> ambilPertemuanPunyaDiskusi(PertemuanPunyaDiskusi parent, TreeSet<Long> pertemuanPunyaDiskusisa,
			int mulai, int banyak) {
		int index = 0;
		List<Long> pertemuanPunyaDiskusis = new ArrayList<Long>();

		if (pertemuanPunyaDiskusisa == null || pertemuanPunyaDiskusisa.isEmpty()) {
			return pertemuanPunyaDiskusis;
		}

		for (Long pertemuanPunyaDiskusiId : pertemuanPunyaDiskusisa) {
			if (pertemuanPunyaDiskusiId == null)
				continue;
			try {
				PertemuanPunyaDiskusi pertemuanPunyaDiskusi = (PertemuanPunyaDiskusi) GeneralValueObject
						.ambilData(PertemuanPunyaDiskusi.class, pertemuanPunyaDiskusiId.toString());
				if (pertemuanPunyaDiskusi != null) {
					// Pengecekan logika hierarki / child-parent yang lebih aman dan rapi
					boolean isRootDiskusi = (parent == null && pertemuanPunyaDiskusi.getParent() == null);
					boolean isChildDiskusi = (parent != null && pertemuanPunyaDiskusi.getParent() != null
							&& parent.getId().equals(pertemuanPunyaDiskusi.getParent().getId()));

					if (isRootDiskusi || isChildDiskusi) {
						if (index >= mulai && index < (mulai + banyak)) {
							pertemuanPunyaDiskusis.add(pertemuanPunyaDiskusi.getId());
						}
						index++;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2588");
			}
		}
		return pertemuanPunyaDiskusis;
	}

	/**
	 * Cabut pertemuan ini dari peta lokasi milik SETIAP dosen yang berkaitan dengannya.
	 *
	 * <p>Pasangan {@link #tambahPertemuan()}. Selain punya peta lokasi anak sendiri, {@link Dosen}
	 * juga memelihara peta lokasi berisi pertemuan-pertemuan miliknya (dipakai untuk menampilkan
	 * agenda dosen tanpa query). Method ini menjaga peta di sisi dosen tetap selaras ketika sebuah
	 * pertemuan dihapus.</p>
	 *
	 * <p>Kumpulan dosen ditentukan dari jenis induk, dengan rantai yang MIRIP tetapi TIDAK SAMA
	 * dengan {@link #ambilDosen()}: di sini dipakai {@code populateDosen().values()} untuk
	 * bimbingan/skripsi/KKN/PKL, dan {@code pertemuanPunyaGrupPertemuan} maupun
	 * {@code formulirKegiatan} TIDAK punya cabangnya. Akibatnya, pertemuan pada grup pertemuan atau
	 * formulir kegiatan tidak pernah tercabut dari peta lokasi dosennya — peta itu akan menyimpan
	 * id pertemuan yang sudah tiada sampai dibangun ulang.</p>
	 *
	 * <p>Method ini TIDAK menghapus baris pertemuan dari basis data; penghapusan itu tanggung
	 * jawab pemanggil.</p>
	 *
	 * @see #tambahPertemuan()
	 */
	public void hapusPertemuan() {
		Collection<Dosen> dosens = null;
		if (getPerkuliahan() != null) {
			dosens = getPerkuliahan().populateDosenBuNama();
		} else if (getMahasiswaRequestTugasAkhir() != null) {
			dosens = getMahasiswaRequestTugasAkhir().populateDosen().values();
		} else if (getSkripsi() != null) {
			dosens = getSkripsi().populateDosen().values();
		} else if (getKelompokKkn() != null) {
			dosens = getKelompokKkn().populateDosen().values();
		} else if (getKelompokPkl() != null) {
			dosens = getKelompokPkl().populateDosen().values();
		} else if (getKrsMahasiswa() != null && getKrsMahasiswa().getDosenPa() != null) {
			dosens = new ArrayList<Dosen>();
			dosens.add(getKrsMahasiswa().getDosenPa());
		}
		if (dosens != null) {
			for (Dosen dosen : dosens) {
				dosen.removePertemuan(getId());
			}
		}
		dosens = null;
	}

	/**
	 * Daftarkan pertemuan ini ke peta lokasi milik SETIAP dosen yang berkaitan dengannya.
	 *
	 * <p>Pasangan {@link #hapusPertemuan()}, dengan rantai penentuan dosen yang sama persis
	 * (termasuk ketidaklengkapan yang sama untuk grup pertemuan dan formulir kegiatan).</p>
	 *
	 * <p>Berbeda dari {@link #hapusPertemuan()}, setiap pendaftaran dibungkus {@code try/catch}
	 * sendiri sehingga kegagalan pada satu dosen tidak menghentikan pendaftaran ke dosen lain —
	 * tetapi juga tidak dilaporkan ke pemanggil.</p>
	 *
	 * <p>Panggil setelah pertemuan tersimpan dan sudah punya id, karena yang didaftarkan adalah
	 * objek pertemuan itu sendiri.</p>
	 *
	 * @see #hapusPertemuan()
	 */
	public void tambahPertemuan() {
		Collection<Dosen> dosens = null;
		if (getPerkuliahan() != null) {
			dosens = getPerkuliahan().populateDosenBuNama();
		} else if (getMahasiswaRequestTugasAkhir() != null) {
			dosens = getMahasiswaRequestTugasAkhir().populateDosen().values();
		} else if (getSkripsi() != null) {
			dosens = getSkripsi().populateDosen().values();
		} else if (getKelompokKkn() != null) {
			dosens = getKelompokKkn().populateDosen().values();
		} else if (getKelompokPkl() != null) {
			dosens = getKelompokPkl().populateDosen().values();
		} else if (getKrsMahasiswa() != null && getKrsMahasiswa().getDosenPa() != null) {
			dosens = new ArrayList<Dosen>();
			dosens.add(getKrsMahasiswa().getDosenPa());
		}
		if (dosens != null) {
			for (Dosen dosen : dosens) {
				try {
					dosen.populatePertemuan(this, true);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2638");
					// TODO: handle exception
				}
			}
		}
		dosens = null;
	}

	/**
	 * Baca peta lokasi ujian ({@link PertemuanPunyaUjian}) milik pertemuan ini.
	 *
	 * <p>Mengikuti pola pada {@link #ambilLokasiPengajuanIzinTidakMasukPerkuliahan()}.</p>
	 *
	 * <p><b>Awas huruf besar.</b> Nama berkasnya {@code "pertemuan_punya_Ujian_<id>"} dengan huruf
	 * U BESAR, dan penanda {@code udah(...)}/{@code belum(...)} untuk jenis ini juga
	 * {@code "pertemuan_punya_Ujian"}. Menuliskannya dengan huruf kecil akan menunjuk peta yang
	 * berbeda, sehingga peta dibangun ulang pada setiap akses.</p>
	 *
	 * @return isi peta lokasi ujian sebagai teks JSON; JSON kosong bila belum ada
	 * @see #ambilLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public String ambilLokasiPertemuanPunyaUjian() {
		File file = Common.getFileLocation(this, "pertemuan_punya_Ujian_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2652");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Tulis ulang seluruh isi peta lokasi ujian milik pertemuan ini.
	 *
	 * <p>Berbeda dari {@link #tulisLokasiPengajuanIzinTidakMasukPerkuliahan(String)}, kegagalan
	 * penulisan di sini ditelan sehingga pemanggil tidak akan tahu bila peta gagal disimpan.</p>
	 *
	 * @param data isi peta lokasi baru sebagai teks JSON
	 */
	public void tulisLokasiPertemuanPunyaUjian(String data) {
		File file = Common.getFileLocation(this, "pertemuan_punya_Ujian_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2661");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Hapus berkas peta lokasi ujian milik pertemuan ini.
	 *
	 * @see #bersihkanLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public void bersihkanLokasiPertemuanPunyaUjian() {
		File file = Common.getFileLocation(this, "pertemuan_punya_Ujian_" + getId().toString());
		BacaTulisUtil.doHapus(file, "pertemuan_punya_Ujian");

	}

	/**
	 * Bangun ulang peta lokasi ujian dari basis data.
	 *
	 * <p>Berbeda dari {@code reInitXxx(...)} lain, urutannya bukan sekadar menurut id melainkan
	 * menurut NAMA UJIAN lebih dulu (lewat {@code createAlias("ujian", "ujian")}), baru id.
	 * Karena {@link #ambilPertemuanPunyaUjianTotal(String, Tbmuser)} pada akhirnya mengumpulkan
	 * hasilnya ke {@link TreeMap} berkunci id, urutan itu praktis tidak terlihat lagi di
	 * hasil akhir.</p>
	 *
	 * <p>Pertemuan yang belum punya id menghasilkan daftar kosong (bukan exception), sehingga peta
	 * tetap ditulis dalam keadaan kosong.</p>
	 *
	 * @param session session Hibernate yang sudah terbuka; tidak ditutup oleh method ini
	 * @see #reInitPengajuanIzinTidakMasukPerkuliahan(Session)
	 */
	@SuppressWarnings("unchecked")
	public void reInitPertemuanPunyaUjian(Session session) {
		List<Long> pertemuanPunyaUjians = this.getId() == null ? new ArrayList<Long>()
				: session.createCriteria(PertemuanPunyaUjian.class).createAlias("ujian", "ujian")
						.addOrder(Order.asc("ujian.nama")).addOrder(Order.asc("id"))
						.setProjection(Projections.property("id")).add(Restrictions.eq("pertemuan", this)).list();
		bersihkanLokasiPertemuanPunyaUjian();
		tulisLokasiPertemuanPunyaUjian(new JSONObject().toString());
		for (Long pertemuanPunyaUjianid : pertemuanPunyaUjians) {
			populatePertemuanPunyaUjian(pertemuanPunyaUjianid, true);
		}
		pertemuanPunyaUjians = null;
	}

	/**
	 * Keluarkan satu ujian dari peta lokasi pertemuan ini.
	 *
	 * @param id id ujian yang dikeluarkan
	 * @see #removePengajuanIzinTidakMasukPerkuliahan(Serializable)
	 */
	public void removePertemuanPunyaUjian(Serializable id) {
		try {
			JSONObject c = jsonObjekUntukTulis(ambilLokasiPertemuanPunyaUjian());
			c.put(id.toString(), "");
			tulisLokasiPertemuanPunyaUjian(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2692");

		}
	}

	/**
	 * Daftarkan satu ujian ke peta lokasi pertemuan ini.
	 *
	 * <p>Seperti {@link #populatePertemuanPunyaDiskusi(Long, boolean)}, parameter {@code tulisUlang}
	 * TIDAK dipakai di badan method.</p>
	 *
	 * @param pertemuanPunyaUjianid id ujian yang didaftarkan; {@code null} diabaikan
	 * @param tulisUlang            tidak dipakai
	 * @see #populatePengajuanIzinTidakMasukPerkuliahan(Long)
	 */
	public void populatePertemuanPunyaUjian(Long pertemuanPunyaUjianid, boolean tulisUlang) {
		try {
			if (pertemuanPunyaUjianid == null) {
				return;
			}

			JSONObject c = jsonObjekUntukTulis(ambilLokasiPertemuanPunyaUjian());
			c.put(pertemuanPunyaUjianid.toString(), pertemuanPunyaUjianid.toString());
			tulisLokasiPertemuanPunyaUjian(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2706");
		}
	}

	/**
	 * Adakah ujian dengan nama tertentu yang TERLIHAT oleh pengguna ini pada pertemuan ini?
	 *
	 * <p>Perhatikan bahwa jawabannya bergantung pada {@code tbmuser}: bagi peserta didik,
	 * {@link #ambilPertemuanPunyaUjianTotal(String, Tbmuser)} menyembunyikan ujian yang jendela
	 * waktunya sudah lewat. Jadi pertanyaan "apakah pertemuan ini punya ujian X" dapat dijawab
	 * berbeda untuk dosen dan mahasiswa pada saat yang sama.</p>
	 *
	 * @param namaUjian nama ujian yang dicari (pencocokan sebagian, tidak peka huruf besar/kecil);
	 *                  kosong berarti ujian apa pun
	 * @param tbmuser   pengguna yang menjadi sudut pandang penyaringan; {@code null} berarti tanpa
	 *                  penyaringan waktu
	 * @return {@code true} bila ada ujian yang cocok dan terlihat
	 * @see #ambilPertemuanPunyaUjianTotal(String, Tbmuser)
	 */
	public boolean punyaUjian(String namaUjian, Tbmuser tbmuser) {
		TreeMap<Long, PertemuanPunyaUjian> pertemuanPunyaUjiansa = ambilPertemuanPunyaUjianTotal(namaUjian, tbmuser);
		int ada = pertemuanPunyaUjiansa.size();
		pertemuanPunyaUjiansa = null;
		return ada > 0;
	}

	/**
	 * Jumlah SELURUH ujian pada pertemuan ini.
	 *
	 * @return banyaknya ujian
	 * @see #ambilJumlahPertemuanPunyaUjian(Mahasiswa, BiodataCalonMahasiswa)
	 */
	public int ambilJumlahPertemuanPunyaUjian() {
		return ambilJumlahPertemuanPunyaUjian(null, null);
	}

	/**
	 * Jumlah ujian pada pertemuan ini — <b>atau</b> jumlah HASIL ujian milik satu peserta.
	 *
	 * <p>Arti angka yang dikembalikan BERUBAH tergantung argumennya, jadi bacalah dengan saksama:</p>
	 * <ul>
	 *   <li>kedua argumen {@code null} — yang dihitung adalah banyaknya UJIAN pada pertemuan ini,
	 *       langsung dari peta lokasi tanpa memuat objek apa pun;</li>
	 *   <li>salah satu argumen terisi — untuk tiap ujian, method memuat objeknya lalu MENJUMLAHKAN
	 *       banyaknya HASIL ujian ({@code ambilHasilUjianMahasiswa(...)}) milik peserta itu. Jadi
	 *       hasilnya bisa jauh lebih besar daripada jumlah ujian, dan artinya "berapa kali peserta
	 *       ini sudah mengerjakan".</li>
	 * </ul>
	 *
	 * <p>Kedua cabang berargumen memanggil {@code ambilHasilUjianMahasiswa(mahasiswa,
	 * biodataCalonMahasiswa, true)} dengan argumen yang persis sama, sehingga isinya sebenarnya
	 * duplikat — pemisahan cabangnya tidak mengubah apa pun.</p>
	 *
	 * <p>Peta lokasi dibangun ulang lebih dulu bila penanda {@code "pertemuan_punya_Ujian"} belum
	 * ada; session yang dipakai untuk itu ditutup lewat {@code HibernateUtil.closeSession()}.</p>
	 *
	 * @param mahasiswa             mahasiswa yang hasil ujiannya dihitung; boleh {@code null}
	 * @param biodataCalonMahasiswa calon mahasiswa yang hasil ujiannya dihitung; boleh {@code null}
	 * @return banyaknya ujian, atau banyaknya hasil ujian peserta (lihat penjelasan di atas)
	 */
	@SuppressWarnings("unchecked")
	public int ambilJumlahPertemuanPunyaUjian(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		if (!udah("pertemuan_punya_Ujian")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitPertemuanPunyaUjian(session);
			HibernateUtil.closeSession();
		}
		int jumlah = 0;
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiPertemuanPunyaUjian());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						if (mahasiswa != null) {
							GeneralValueObject generalValueObject = ambilData(PertemuanPunyaUjian.class, key, true);
							if (generalValueObject != null) {
								PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) generalValueObject;
								pertemuanPunyaUjian.setPertemuan(this);
								List<Long> d = pertemuanPunyaUjian.ambilHasilUjianMahasiswa(mahasiswa,
										biodataCalonMahasiswa, true);
								jumlah += d.size();
								d = null;
							}
						} else if (biodataCalonMahasiswa != null) {
							GeneralValueObject generalValueObject = ambilData(PertemuanPunyaUjian.class, key, true);
							if (generalValueObject != null) {
								PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) generalValueObject;
								pertemuanPunyaUjian.setPertemuan(this);
								List<Long> d = pertemuanPunyaUjian.ambilHasilUjianMahasiswa(mahasiswa,
										biodataCalonMahasiswa, true);
								jumlah += d.size();
								d = null;
							}
						} else {
							jumlah++;
						}

					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:2764");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:2768");
		}
		return jumlah;
	}

	/**
	 * Seluruh ujian pada pertemuan ini yang terlihat oleh {@code tbmuser}.
	 *
	 * @param tbmuser pengguna yang menjadi sudut pandang penyaringan
	 * @return peta {@code id -> ujian}
	 * @see #ambilPertemuanPunyaUjianTotal(String, Tbmuser)
	 */
	public TreeMap<Long, PertemuanPunyaUjian> ambilPertemuanPunyaUjianTotal(Tbmuser tbmuser) {
		return ambilPertemuanPunyaUjianTotal("", tbmuser);
	}

	/**
	 * Ujian pada pertemuan ini, disaring menurut nama dan menurut hak lihat pengguna.
	 *
	 * <h4>Penyaringan menurut peran</h4>
	 * <p>Pengguna yang BUKAN peserta didik (bukan mahasiswa, siswa, calon mahasiswa, atau calon
	 * siswa) — dan juga {@code tbmuser} bernilai {@code null} — melihat SEMUA ujian. Peserta didik
	 * hanya melihat ujian yang tidak bertanda
	 * {@code tidakDitampilkanJikaWaktuSudahTerlewat}, atau yang saat ini masih berada dalam
	 * rentang {@code mulaiUjian}..{@code sampaiUjian}.</p>
	 *
	 * <h4>Dua sumber data</h4>
	 * <p>Untuk tiap entri peta lokasi, objek ujian dicari lebih dulu di cache. Bila tidak ada, nilai
	 * entri diperlakukan sebagai PATH BERKAS dan isinya dibaca sebagai JSON lalu diubah menjadi
	 * objek. Jalur cadangan inilah yang membuat daftar ujian tetap tampil walau cache sudah
	 * kosong.</p>
	 *
	 * <p><b>Ketidakselarasan yang perlu diketahui:</b> penyaring {@code nama} hanya diterapkan pada
	 * jalur BERKAS, tidak pada jalur cache. Akibatnya, memanggil method ini dengan nama tertentu
	 * dapat mengembalikan ujian yang namanya tidak cocok, tergantung ujian itu kebetulan sudah
	 * ter-cache atau belum.</p>
	 *
	 * <p>Berkas yang ada tetapi isinya bukan JSON objek dilewati diam-diam lewat
	 * {@link #bacaJsonObjekAman(String)} — sengaja, agar log tidak dibanjiri pada tiap siklus
	 * warmup cache.</p>
	 *
	 * @param nama    penggalan nama ujian yang dicari (tidak peka huruf besar/kecil);
	 *                {@code null}/kosong berarti tanpa penyaringan nama
	 * @param tbmuser pengguna yang menjadi sudut pandang; {@code null} berarti melihat semua
	 * @return peta {@code id -> ujian}, terurut menaik menurut id; kosong bila pertemuan belum
	 *         punya id
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<Long, PertemuanPunyaUjian> ambilPertemuanPunyaUjianTotal(String nama, Tbmuser tbmuser) {
		TreeMap<Long, PertemuanPunyaUjian> pertemuanPunyaUjiansa = new TreeMap<Long, PertemuanPunyaUjian>();
		if (getId() == null) {
			return pertemuanPunyaUjiansa;
		}
		if (!udah("pertemuan_punya_Ujian")) {
			Session session = HibernateUtil.currentNativeSession();
			try {
				reInitPertemuanPunyaUjian(session);
			} finally {
				if (session != null && session.isOpen()) {
					session.clear();
					if (session.isConnected()) {
						session.disconnect();
					}
					session.close();
				}
			}
		}

		try {
			Date sekarang = WaktuUtil.getDate();
			JSONObject c = jsonObjekAtauKosong(ambilLokasiPertemuanPunyaUjian());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(PertemuanPunyaUjian.class, key);
						if (generalValueObject != null) {
							PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) generalValueObject;
							pertemuanPunyaUjian.setPertemuan(this);

							if (tbmuser == null || (tbmuser != null && tbmuser.getMahasiswa() == null
									&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
									&& tbmuser.getCalonSiswa() == null)) {
								pertemuanPunyaUjiansa.put(pertemuanPunyaUjian.getId(), pertemuanPunyaUjian);
							} else if (!pertemuanPunyaUjian.getTidakDitampilkanJikaWaktuSudahTerlewat()
									|| Common.isDateBetween(sekarang, pertemuanPunyaUjian.getMulaiUjian(),
											pertemuanPunyaUjian.getSampaiUjian())) {
								pertemuanPunyaUjiansa.put(pertemuanPunyaUjian.getId(), pertemuanPunyaUjian);
							}

						} else {
							File file = new File(s);
							if (file != null && file.exists()) {
								JSONObject isiBerkas = bacaJsonObjekAman(ais.common.BacaTulisUtil.baca(file));
								if (isiBerkas == null) {
									// Berkas ADA tetapi isinya bukan JSON objek (kosong/terpotong/format lama).
									// Dilewati -- hasil akhirnya sama dengan perilaku lama yang melewatinya
									// lewat exception, tanpa membanjiri log pada tiap siklus warmup cache.
									continue;
								}
								PertemuanPunyaUjian pertemuanPunyaUjian = (PertemuanPunyaUjian) Common.convertToObject(
										isiBerkas, PertemuanPunyaUjian.class);

								if (pertemuanPunyaUjian != null && (nama == null || nama.trim().isEmpty()
										|| pertemuanPunyaUjian.getNama().toLowerCase().contains(nama.toLowerCase()))) {
									pertemuanPunyaUjian.setPertemuan(this);
									if (tbmuser == null || (tbmuser != null && tbmuser.getMahasiswa() == null
											&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
											&& tbmuser.getCalonSiswa() == null)) {
										pertemuanPunyaUjiansa.put(pertemuanPunyaUjian.getId(), pertemuanPunyaUjian);
									} else if (!pertemuanPunyaUjian.getTidakDitampilkanJikaWaktuSudahTerlewat()
											|| Common.isDateBetween(sekarang, pertemuanPunyaUjian.getMulaiUjian(),
													pertemuanPunyaUjian.getSampaiUjian())) {
										pertemuanPunyaUjiansa.put(pertemuanPunyaUjian.getId(), pertemuanPunyaUjian);
									}
								}
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2838");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2842");

		}
		return pertemuanPunyaUjiansa;
	}

	/**
	 * Potong satu halaman dari peta ujian yang sudah dikumpulkan.
	 *
	 * <p>Bentuk paling umum dari pola {@code ambilXxx(map, mulai, banyak)} di kelas ini: menelusuri
	 * peta menurut urutan kuncinya dan mengambil entri pada rentang
	 * {@code [mulai, mulai + banyak)}. Seluruh peta tetap ditelusuri sampai habis walau halaman
	 * yang diminta sudah penuh.</p>
	 *
	 * @param pertemuanPunyaUjiansa peta hasil {@link #ambilPertemuanPunyaUjianTotal(String, Tbmuser)};
	 *                              {@code null} melempar {@link NullPointerException}
	 * @param mulai                 indeks awal halaman (berbasis nol)
	 * @param banyak                banyaknya entri per halaman
	 * @return daftar ujian pada halaman yang diminta
	 */
	public List<PertemuanPunyaUjian> ambilPertemuanPunyaUjian(TreeMap<Long, PertemuanPunyaUjian> pertemuanPunyaUjiansa,
			int mulai, int banyak) {

		int index = 0;
		List<PertemuanPunyaUjian> pertemuanPunyaUjians = new ArrayList<PertemuanPunyaUjian>();
		Iterator<Long> i = pertemuanPunyaUjiansa.keySet().iterator();
		while (i.hasNext()) {
			PertemuanPunyaUjian pertemuanPunyaUjian = pertemuanPunyaUjiansa.get(i.next());

			if (index >= mulai && index < (mulai + banyak)) {
				pertemuanPunyaUjians.add(pertemuanPunyaUjian);
			}
			index++;

		}
		return pertemuanPunyaUjians;
	}

	/**
	 * Baca peta lokasi lampiran ({@link PertemuanFileContent}) milik satu pertemuan.
	 *
	 * <p>Mengikuti pola pada {@link #ambilLokasiPengajuanIzinTidakMasukPerkuliahan()}, dengan dua
	 * perbedaan penting:</p>
	 * <ul>
	 *   <li><b>Statis dan menerima id.</b> Kelompok lampiran/video/audio dibuat statis agar dapat
	 *       dipanggil dari sisi ANAK (mis. {@link PertemuanFileContent} yang hanya menyimpan id
	 *       induknya sebagai {@code Long}, bukan sebagai relasi) tanpa perlu memuat objek
	 *       {@link Pertemuan}-nya lebih dulu.</li>
	 *   <li><b>Nilai peta adalah PATH BERKAS, bukan id.</b> Isi tiap lampiran ikut di-cache ke
	 *       berkas tersendiri, dan peta menyimpan lokasi berkas itu — lihat
	 *       {@link #populatePertemuanFileContent(PertemuanFileContent, boolean)}. Inilah yang
	 *       memungkinkan {@link #ambilPertemuanFileContentTotal(boolean)} tetap menampilkan
	 *       lampiran walau cache objeknya sudah kosong.</li>
	 * </ul>
	 *
	 * <p>{@code id} bernilai {@code null} menghasilkan JSON kosong, bukan exception.</p>
	 *
	 * @param id id pertemuan pemilik lampiran
	 * @return isi peta lokasi lampiran sebagai teks JSON; JSON kosong bila belum ada
	 * @see #ambilLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public static String ambilLokasiPertemuanFileContent(Serializable id) {
		if (id == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(Pertemuan.class, id, "pertemuan_file_content_" + id.toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2872");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Tulis ulang seluruh isi peta lokasi lampiran milik satu pertemuan.
	 *
	 * @param data isi peta lokasi baru sebagai teks JSON
	 * @param id   id pertemuan pemilik lampiran
	 * @see #ambilLokasiPertemuanFileContent(Serializable)
	 */
	public static void tulisLokasiPertemuanFileContent(String data, Serializable id) {
		File file = Common.getFileLocation(Pertemuan.class, id, "pertemuan_file_content_" + id.toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2881");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Hapus berkas peta lokasi lampiran milik pertemuan ini.
	 *
	 * <p>Berbeda dari pasangan baca/tulisnya yang statis, method ini bersifat instans dan memakai
	 * {@code getId()} tanpa penjagaan {@code null}.</p>
	 *
	 * @see #bersihkanLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public void bersihkanLokasiPertemuanFileContent() {
		File file = Common.getFileLocation(this, "pertemuan_file_content_" + getId().toString());
		BacaTulisUtil.doHapus(file, "pertemuan_file_content");

	}

	/**
	 * Bangun ulang peta lokasi lampiran dari basis data, sekaligus menambal tautan yang hilang.
	 *
	 * <p>Berbeda dari {@code reInitXxx(...)} lain yang hanya mengambil ID, method ini memuat objek
	 * {@link PertemuanFileContent} seutuhnya karena ada perbaikan data yang harus dilakukan
	 * sambil jalan. Untuk tiap lampiran bernama {@code "link"} yang belum punya {@code link}
	 * tetapi punya berkas:</p>
	 * <ul>
	 *   <li>bila berkasnya benar-benar ada di disk, tautan dibuatkan lewat
	 *       {@code LampiranLain.ambilLinkLampiranLain(...)}, dipasang ke objek, dimasukkan ke
	 *       cache, dan didaftarkan ke peta;</li>
	 *   <li>bila berkasnya tidak ada, lampiran itu DILEWATI — tidak masuk peta sama sekali,
	 *       sehingga praktis hilang dari tampilan sampai berkasnya dipulihkan.</li>
	 * </ul>
	 * <p>Lampiran {@code "link"} yang tidak punya {@code link} maupun berkas juga dilewati.
	 * Lampiran biasa langsung dimasukkan cache dan didaftarkan.</p>
	 *
	 * <p>Perhatikan bahwa penyaring query memakai {@code Restrictions.eq("pertemuan", this.getId())}
	 * — dibandingkan dengan ID, karena di sisi {@link PertemuanFileContent} induknya memang
	 * disimpan sebagai {@code Long}, bukan relasi. Ini berbeda dari blok izin/diskusi yang
	 * membandingkan dengan objek {@code this}.</p>
	 *
	 * <p>Satu-satunya {@code reInitXxx} pada kelompok ini yang MELEMPARKAN exception keluar
	 * ({@code throws Exception}); pemanggilnya di kelas ini menangkap dan mencatatnya.</p>
	 *
	 * @param session session Hibernate yang sudah terbuka; tidak ditutup oleh method ini
	 * @throws Exception bila query atau pembacaan berkas gagal
	 * @see #ambilPertemuanFileContentTotal(boolean)
	 */
	@SuppressWarnings("unchecked")
	public void reInitPertemuanFileContent(Session session) throws Exception {
		List<PertemuanFileContent> pertemuanFileContents = session.createCriteria(PertemuanFileContent.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("pertemuan", this.getId())).list();
		bersihkanLokasiPertemuanFileContent();
		tulisLokasiPertemuanFileContent(new JSONObject().toString(), getId());
		for (PertemuanFileContent pertemuanFileContent : pertemuanFileContents) {

			if (pertemuanFileContent.getNama().equalsIgnoreCase("link") && pertemuanFileContent.getLink() == null
					&& pertemuanFileContent.getFoto() != null) {
				File fileData = pertemuanFileContent.ambilFile();
				if (fileData != null && fileData.exists()) {
					String link = LampiranLain.ambilLinkLampiranLain(fileData);
					pertemuanFileContent.setLink(link);
//					System.out.println("link pertemuan " + pertemuanFileContent + " baru " + link);
					masukkanData(PertemuanFileContent.class, pertemuanFileContent);
					populatePertemuanFileContent(pertemuanFileContent, true);
				} else {
//					System.out.println("link pertemuan " + pertemuanFileContent
//							+ " tidak valid dan file " + fileData.getAbsolutePath() + " tidak ada");
				}
			} else if (pertemuanFileContent.getNama().equalsIgnoreCase("link") && pertemuanFileContent.getLink() == null
					&& pertemuanFileContent.getFoto() == null) {
//				System.out.println("link pertemuan " + pertemuanFileContent + " tidak valid");
			} else {
				masukkanData(PertemuanFileContent.class, pertemuanFileContent);
				populatePertemuanFileContent(pertemuanFileContent, true);
			}

		}
		pertemuanFileContents = null;
	}

	/**
	 * Keluarkan satu lampiran dari peta lokasi.
	 *
	 * <p><b>Perhatikan kekeliruan pemakaian id di sini</b> (dicatat, tidak diperbaiki): parameter
	 * {@code id} adalah id LAMPIRAN, tetapi nilai itu juga dipakai sebagai id PERTEMUAN ketika
	 * memanggil {@link #ambilLokasiPertemuanFileContent(Serializable)} dan
	 * {@link #tulisLokasiPertemuanFileContent(String, Serializable)}. Akibatnya method ini membaca
	 * dan menulis peta milik "pertemuan" bernomor sama dengan id lampiran, bukan peta pertemuan
	 * yang sebenarnya memiliki lampiran itu. Bandingkan dengan
	 * {@link #populatePertemuanFileContent(PertemuanFileContent, boolean)} yang benar karena
	 * mengambil id induk lewat {@code pertemuanFileContent.getPertemuan()}.</p>
	 *
	 * @param id id lampiran yang dikeluarkan
	 * @see #removePengajuanIzinTidakMasukPerkuliahan(Serializable)
	 */
	public static void removePertemuanFileContent(Serializable id) {
		try {
			JSONObject c = jsonObjekUntukTulis(ambilLokasiPertemuanFileContent(id));
			c.put(id.toString(), "");
			tulisLokasiPertemuanFileContent(c.toString(), id);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2931");

		}
	}

	/**
	 * Daftarkan satu lampiran ke peta lokasi pertemuan pemiliknya.
	 *
	 * <p>Berbeda dari {@code populateXxx(...)} lain yang cuma menyimpan {@code "id" -> "id"},
	 * method ini memanggil {@code pertemuanFileContent.write()} yang MENULIS BERKAS CACHE berisi
	 * bentuk JSON lampiran, lalu menyimpan {@code "id" -> "path berkas"} ke peta. Berkas itulah
	 * yang dibaca sebagai sumber cadangan oleh
	 * {@link #ambilPertemuanFileContentTotal(boolean)} ketika objeknya tidak ada di cache.</p>
	 *
	 * <p>Id pertemuan diambil dari {@code pertemuanFileContent.getPertemuan()}, sehingga peta yang
	 * disentuh selalu peta milik induk yang benar.</p>
	 *
	 * <p>Parameter {@code tulisUlang} tidak berpengaruh: kedua cabang ekspresi kondisionalnya
	 * menghasilkan nilai yang sama persis.</p>
	 *
	 * @param pertemuanFileContent lampiran yang didaftarkan; {@code null} diabaikan
	 * @param tulisUlang           tidak berpengaruh
	 * @see #populatePengajuanIzinTidakMasukPerkuliahan(Long)
	 */
	public static void populatePertemuanFileContent(PertemuanFileContent pertemuanFileContent, boolean tulisUlang) {
		try {
			if (pertemuanFileContent == null) {
				return;
			}

			JSONObject c = jsonObjekUntukTulis(ambilLokasiPertemuanFileContent(pertemuanFileContent.getPertemuan()));
			c.put(pertemuanFileContent.getId().toString(), pertemuanFileContent.write().getAbsolutePath());
			tulisLokasiPertemuanFileContent(c.toString(), pertemuanFileContent.getPertemuan());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2945");
		}
	}

	/**
	 * Adakah lampiran pada pertemuan ini (memakai peta lokasi yang sudah ada)?
	 *
	 * @return {@code true} bila ada lampiran atau pertemuan ini bertanda ujian
	 * @see #fileContent(boolean)
	 */
	public boolean fileContent() {
		return fileContent(false);
	}

	/**
	 * Adakah lampiran pada pertemuan ini?
	 *
	 * <p><b>Ada jalan pintas yang mudah mengejutkan:</b> bila {@link #getStatusPertemuan()}
	 * bertanda ujian, method ini langsung mengembalikan {@code true} TANPA memeriksa apakah
	 * lampirannya benar-benar ada. Alasannya, sesi ujian selalu dianggap punya berkas soal.
	 * Jadi hasil {@code true} di sini tidak menjamin
	 * {@link #ambilPertemuanFileContentTotal(boolean)} akan mengembalikan sesuatu.</p>
	 *
	 * @param refresh {@code true} untuk memaksa peta lokasi dibangun ulang dari basis data lebih
	 *                dahulu (mahal); {@code false} untuk memakai peta yang ada
	 * @return {@code true} bila ada lampiran atau pertemuan ini bertanda ujian
	 */
	public boolean fileContent(boolean refresh) {

		if (getStatusPertemuan() != null && getStatusPertemuan().getUjian()) {
			return true;
		}

		TreeMap<Long, PertemuanFileContent> pertemuanFileContentsa = ambilPertemuanFileContentTotal(refresh);
		int ada = pertemuanFileContentsa.size();
		pertemuanFileContentsa = null;
		return ada > 0;
	}

	/**
	 * Banyaknya lampiran pada pertemuan ini.
	 *
	 * <p>Menghitung LANGSUNG dari peta lokasi (entri yang nilainya tidak kosong), tanpa memuat
	 * objek lampiran mana pun — jauh lebih murah daripada
	 * {@code ambilPertemuanFileContentTotal().size()}.</p>
	 *
	 * <p>Peta dibangun ulang lebih dulu bila penanda {@code "pertemuan_file_content"} belum ada;
	 * pembangunan itu memakai session dari {@link StreamingHibernateUtil}, bukan
	 * {@link HibernateUtil} biasa, karena lampiran termasuk data berukuran besar. Perhatikan bahwa
	 * pada jalur ini session tersebut TIDAK ditutup — berbeda dari
	 * {@link #ambilPertemuanFileContentTotal(boolean)} yang menutupnya.</p>
	 *
	 * <p>Angka yang dikembalikan bisa LEBIH BESAR daripada ukuran hasil
	 * {@link #ambilPertemuanFileContentTotal(boolean)}, karena di sini lampiran {@code "link"}
	 * yang rusak (tanpa tautan dan tanpa berkas) tetap ikut dihitung, sedangkan di sana
	 * disaring keluar.</p>
	 *
	 * @return banyaknya entri lampiran pada peta lokasi; {@code 0} bila pertemuan belum punya id
	 */
	@SuppressWarnings("unchecked")
	public int ambilJumlahPertemuanFileContent() {
		if (getId() == null) {
			return 0;
		}
		if (!udah("pertemuan_file_content")) {
			Session session = StreamingHibernateUtil.getInstance().currentSession();
			try {
				reInitPertemuanFileContent(session);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:2973");
			}
		}
		int jumlah = 0;
		try {
			JSONObject c = bacaJsonObjekAman(ambilLokasiPertemuanFileContent(getId()));
			if (c == null) {
				// Peta lokasi belum ada / rusak: kembalikan 0 seperti perilaku lama (dulu
				// new JSONObject melempar lalu ditangkap catch di bawah, dan jumlah tetap 0)
				// -- hanya tanpa exception dan tanpa stack trace berulang.
				return jumlah;
			}
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						jumlah++;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2988");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:2992");

		}
		return jumlah;
	}

	/**
	 * Ubah teks menjadi {@link JSONObject} HANYA bila teks itu memang berbentuk objek JSON.
	 *
	 * <p>KE-FIX JSONException "A JSONObject text must begin with '{' at 0 [character 1 line 1]"
	 * (terlihat lewat ElearningRingkasanCache.warmupSekarang -> hitungAgg ->
	 * ambilPertemuanFileContentTotal): pemanggil DULU langsung memanggil
	 * {@code new JSONObject(teks)} setelah memastikan berkasnya ADA, tanpa memeriksa ISI-nya.
	 * Berkas lampiran pertemuan yang kosong, terpotong, atau berformat lama membuat parser
	 * melempar. Exception itu memang tertangkap sehingga baris tersebut dilewati dan proses
	 * tidak gagal, TETAPI jejaknya dicatat penuh ke ErrorAuditUtil pada SETIAP siklus warmup
	 * cache e-learning -- membanjiri log dengan kondisi data lama yang sudah diketahui.</p>
	 *
	 * <p>Hasil akhirnya SAMA PERSIS dengan perilaku lama (baris berisi data tidak valid tetap
	 * dilewati); yang hilang hanya exception dan stack trace berulangnya. Mengembalikan
	 * {@code null} berarti "bukan JSON objek yang dapat dipakai".</p>
	 */
	private static JSONObject bacaJsonObjekAman(String teks) {
		if (teks == null) {
			return null;
		}
		String rapi = teks.trim();
		if (rapi.length() == 0 || rapi.charAt(0) != '{') {
			return null;
		}
		try {
			return new JSONObject(rapi);
		} catch (Exception abaikan) {
			return null;
		}
	}

	/**
	 * Sama seperti {@link #bacaJsonObjekAman(String)} tetapi TIDAK PERNAH mengembalikan
	 * {@code null}: teks yang bukan JSON objek menghasilkan objek KOSONG.
	 *
	 * <p>KE-FIX JSONException "A JSONObject text must begin with '{' at 0": seluruh pembaca peta
	 * lokasi di kelas ini DULU memanggil {@code jsonObjekAtauKosong(ambilLokasiXxx())} langsung. Bila
	 * kolom penyimpan peta itu kosong, berisi spasi, atau rusak, parser melempar. Exception itu
	 * memang tertangkap try/catch di tiap pemanggil sehingga method mengembalikan nilai
	 * default-nya (0 / false / list kosong / map kosong), TETAPI jejaknya dicatat penuh ke
	 * ErrorAuditUtil setiap kali dipanggil -- dan pemanggilnya termasuk warmup cache e-learning
	 * yang berjalan berkala.</p>
	 *
	 * <p><b>Perilaku dipertahankan.</b> Objek kosong membuat {@code keys()} tidak menghasilkan
	 * apa pun, sehingga badan perulangan tidak pernah dijalankan dan method mengembalikan nilai
	 * default yang SAMA PERSIS seperti ketika exception dilempar lalu ditangkap. Untuk pemanggil
	 * yang MENULIS kembali peta itu, memulai dari objek kosong juga merupakan satu-satunya
	 * perilaku yang masuk akal -- sama dengan inisialisasi bawaan kelas ini yang memang memakai
	 * {@code new JSONObject().toString()}. Tanpa ini, peta yang sekali rusak membuat fitur
	 * terkait tidak akan pernah bisa dipakai lagi karena setiap penambahan selalu melempar.</p>
	 */
	private static JSONObject jsonObjekAtauKosong(String teks) {
		JSONObject hasil = bacaJsonObjekAman(teks);
		return hasil == null ? new JSONObject() : hasil;
	}

	/**
	 * Varian untuk jalur TULIS: baca peta lokasi, ubah, lalu simpan kembali.
	 *
	 * <p>Berbeda dari {@link #jsonObjekAtauKosong(String)}, method ini SENGAJA melempar bila teks
	 * yang tersimpan tidak kosong tetapi juga bukan JSON objek yang sah. Alasannya: pada jalur
	 * tulis, memulai dari objek kosong berarti peta lama DITIMPA peta baru, sehingga entri yang
	 * sebenarnya masih tersimpan (walau untuk sementara tidak terbaca) hilang permanen. Pada
	 * jalur baca hal itu tidak berbahaya karena tidak ada yang disimpan kembali.</p>
	 *
	 * <p>Peta yang memang BELUM ADA (null / kosong / hanya spasi) tetap menghasilkan objek kosong
	 * -- itu keadaan awal yang wajar dan aman untuk ditulis, sama dengan inisialisasi bawaan
	 * kelas ini yang memakai {@code new JSONObject().toString()}.</p>
	 *
	 * <p>Exception yang dilempar ditangkap try/catch milik tiap pemanggil, sehingga penyimpanan
	 * DIBATALKAN dan teks lama tetap utuh untuk diperiksa/diperbaiki. Kejadian ini dipicu aksi
	 * pengguna (menambah/menghapus lampiran), bukan proses berkala, jadi pencatatannya tidak
	 * akan membanjiri log seperti jalur warmup cache.</p>
	 */
	private static JSONObject jsonObjekUntukTulis(String teks) {
		if (teks == null || teks.trim().length() == 0) {
			return new JSONObject();
		}
		JSONObject hasil = bacaJsonObjekAman(teks);
		if (hasil == null) {
			throw new IllegalStateException("Peta lokasi tersimpan RUSAK (bukan JSON objek yang sah)."
					+ " Penyimpanan dibatalkan agar entri lama tidak tertimpa peta baru."
					+ " Perbaiki isi kolom penyimpan peta itu lebih dahulu.");
		}
		return hasil;
	}

	/**
	 * Seluruh lampiran pertemuan ini, memakai peta lokasi yang sudah ada.
	 *
	 * @return peta {@code id -> lampiran}, terurut MENURUN menurut id
	 * @see #ambilPertemuanFileContentTotal(boolean)
	 */
	public TreeMap<Long, PertemuanFileContent> ambilPertemuanFileContentTotal() {
		return ambilPertemuanFileContentTotal(false);
	}

	/**
	 * Seluruh lampiran pertemuan ini.
	 *
	 * <p>Seperti {@link #ambilPertemuanPunyaUjianTotal(String, Tbmuser)}, tiap entri peta dicari
	 * lebih dulu di cache; bila tidak ada, nilai entri diperlakukan sebagai PATH BERKAS dan isinya
	 * dibaca sebagai JSON lalu diubah menjadi objek. Kedua jalur menerapkan penyaringan yang sama
	 * untuk lampiran bernama {@code "link"}:</p>
	 * <ul>
	 *   <li>tanpa {@code link} tetapi punya berkas yang ADA di disk &rarr; tautan dibuatkan lalu
	 *       lampiran dimasukkan;</li>
	 *   <li>tanpa {@code link} dan berkasnya TIDAK ada &rarr; dilewati;</li>
	 *   <li>tanpa {@code link} dan tanpa berkas sama sekali &rarr; dilewati.</li>
	 * </ul>
	 * <p>Karena penyaringan ini, hasilnya bisa lebih sedikit daripada
	 * {@link #ambilJumlahPertemuanFileContent()}.</p>
	 *
	 * <p>Urutannya MENURUN ({@code Collections.reverseOrder()}) sehingga lampiran terbaru muncul
	 * lebih dulu.</p>
	 *
	 * <p>Berkas yang ada tetapi isinya bukan JSON objek dilewati dengan pesan ke
	 * {@code System.out} — satu-satunya tempat pada kelompok ini yang mencetak, sedangkan padanan
	 * video/audio melewatinya diam-diam.</p>
	 *
	 * @param refresh {@code true} untuk memaksa peta lokasi dibangun ulang dari basis data lebih
	 *                dahulu (mahal: memuat seluruh objek lampiran dan menulis berkas cache-nya)
	 * @return peta {@code id -> lampiran}, terurut menurun menurut id
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<Long, PertemuanFileContent> ambilPertemuanFileContentTotal(boolean refresh) {
		if (!udah("pertemuan_file_content") || refresh) {
			Session session = StreamingHibernateUtil.getInstance().currentSession();
			try {
				reInitPertemuanFileContent(session);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:3010");
			}
			StreamingHibernateUtil.getInstance().closeSession();
		}
		TreeMap<Long, PertemuanFileContent> pertemuanFileContentsa = new TreeMap<Long, PertemuanFileContent>(
				Collections.reverseOrder());
		try {
			JSONObject c = bacaJsonObjekAman(ambilLokasiPertemuanFileContent(getId()));
			if (c == null) {
				// Peta lokasi belum ada / rusak: kembalikan map kosong seperti perilaku lama
				// (dulu new JSONObject melempar lalu ditangkap catch di bawah dan map tetap
				// kosong) -- hanya tanpa exception dan tanpa stack trace berulang.
				return pertemuanFileContentsa;
			}
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(PertemuanFileContent.class, key);
						if (generalValueObject != null) {
							PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) generalValueObject;
							pertemuanFileContent.setPertemuan(this.getId());
							if (pertemuanFileContent.getNama().equalsIgnoreCase("link")
									&& pertemuanFileContent.getLink() == null
									&& pertemuanFileContent.getFoto() != null) {
								File fileData = pertemuanFileContent.ambilFile();
								if (fileData != null && fileData.exists()) {
									String link = LampiranLain.ambilLinkLampiranLain(fileData);
									pertemuanFileContent.setLink(link);
//									System.out.println("link pertemuan " + pertemuanFileContent + " baru " + link);
									pertemuanFileContentsa.put(pertemuanFileContent.getId(), pertemuanFileContent);
								} else {
//									System.out.println("link pertemuan " + pertemuanFileContent
//											+ " tidak valid dan file " + fileData.getAbsolutePath() + " tidak ada");
								}
							} else if (pertemuanFileContent.getNama().equalsIgnoreCase("link")
									&& pertemuanFileContent.getLink() == null
									&& pertemuanFileContent.getFoto() == null) {
//								System.out.println("link pertemuan " + pertemuanFileContent + " tidak valid");
							} else {
								pertemuanFileContentsa.put(pertemuanFileContent.getId(), pertemuanFileContent);
							}
						} else {

							File file = new File(s);
							if (file != null && file.exists()) {
								JSONObject isiBerkas = bacaJsonObjekAman(ais.common.BacaTulisUtil.baca(file));
								if (isiBerkas == null) {
									// Berkas ADA tetapi isinya bukan JSON objek (kosong, terpotong, atau
									// format lama). Lewati baris ini -- hasil akhirnya sama dengan perilaku
									// lama yang melewatinya lewat exception, tanpa membanjiri log.
									System.out.println("Pertemuan " + getId()
											+ ": isi berkas lampiran bukan JSON objek yang valid, dilewati -> "
											+ file.getAbsolutePath());
									continue;
								}
								PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) Common
										.convertToObject(isiBerkas, PertemuanFileContent.class);
								pertemuanFileContent.setPertemuan(this.getId());

								if (pertemuanFileContent.getNama().equalsIgnoreCase("link")
										&& pertemuanFileContent.getLink() == null
										&& pertemuanFileContent.getFoto() != null) {
									File fileData = pertemuanFileContent.ambilFile();
									if (fileData != null && fileData.exists()) {
										String link = LampiranLain.ambilLinkLampiranLain(fileData);
										pertemuanFileContent.setLink(link);
//										System.out.println("link pertemuan " + pertemuanFileContent + " baru " + link);
										pertemuanFileContentsa.put(pertemuanFileContent.getId(), pertemuanFileContent);
									} else {
//										System.out.println("link pertemuan " + pertemuanFileContent
//												+ " tidak valid dan file " + fileData.getAbsolutePath() + " tidak ada");
									}
								} else if (pertemuanFileContent.getNama().equalsIgnoreCase("link")
										&& pertemuanFileContent.getLink() == null
										&& pertemuanFileContent.getFoto() == null) {
//									System.out.println("link pertemuan " + pertemuanFileContent + " tidak valid");
								} else {
									pertemuanFileContentsa.put(pertemuanFileContent.getId(), pertemuanFileContent);
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:3082");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:3086");
		}
		return pertemuanFileContentsa;
	}

	// public List<PertemuanFileContent> ambilPertemuanFileContent(
	// TreeMap<Long, PertemuanFileContent> pertemuanFileContentsa, int mulai,
	// int banyak) {
	//
	// int index = 0;
	// List<PertemuanFileContent> pertemuanFileContents = new
	// ArrayList<PertemuanFileContent>();
	// Iterator<Long> i = pertemuanFileContentsa.keySet().iterator();
	// while (i.hasNext()) {
	// PertemuanFileContent pertemuanFileContent =
	// pertemuanFileContentsa.get(i.next());
	//
	// if (index >= mulai && index < (mulai + banyak)) {
	// pertemuanFileContents.add(pertemuanFileContent);
	// }
	// index++;
	//
	// }
	// return pertemuanFileContents;
	// }

	/**
	 * Baca peta lokasi rekaman video ({@link VideoPertemuan}) milik satu pertemuan.
	 *
	 * <p>Kembaran {@link #ambilLokasiPertemuanFileContent(Serializable)} untuk rekaman video:
	 * statis, menerima id pertemuan, dan nilai petanya berupa PATH BERKAS cache.</p>
	 *
	 * @param id id pertemuan pemilik video
	 * @return isi peta lokasi video sebagai teks JSON; JSON kosong bila belum ada
	 * @see #ambilLokasiPertemuanFileContent(Serializable)
	 */
	public static String ambilLokasiVideoPertemuan(Serializable id) {
		if (id == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(Pertemuan.class, id, "video_pertemuan_" + id.toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3118");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Tulis ulang seluruh isi peta lokasi video milik satu pertemuan.
	 *
	 * @param data isi peta lokasi baru sebagai teks JSON
	 * @param id   id pertemuan pemilik video
	 * @see #ambilLokasiVideoPertemuan(Serializable)
	 */
	public static void tulisLokasiVideoPertemuan(String data, Serializable id) {
		File file = Common.getFileLocation(Pertemuan.class, id, "video_pertemuan_" + id.toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3127");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Hapus berkas peta lokasi video milik pertemuan ini.
	 *
	 * @see #bersihkanLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public void bersihkanLokasiVideoPertemuan() {
		File file = Common.getFileLocation(this, "video_pertemuan_" + getId().toString());
		BacaTulisUtil.doHapus(file, "video_pertemuan");

	}

	/**
	 * Bangun ulang peta lokasi video dari basis data, sekaligus menambal tautan yang hilang.
	 *
	 * <p>Salinan persis {@link #reInitPertemuanFileContent(Session)} untuk {@link VideoPertemuan},
	 * termasuk penanganan entri bernama {@code "link"} yang belum punya tautan dan penyaringan
	 * video yang berkasnya hilang.</p>
	 *
	 * @param session session Hibernate yang sudah terbuka; tidak ditutup oleh method ini
	 * @throws Exception bila query atau pembacaan berkas gagal
	 * @see #reInitPertemuanFileContent(Session)
	 */
	@SuppressWarnings("unchecked")
	public void reInitVideoPertemuan(Session session) throws Exception {
		List<VideoPertemuan> videoPertemuans = session.createCriteria(VideoPertemuan.class).addOrder(Order.asc("id"))
				.add(Restrictions.eq("pertemuan", this.getId())).list();
		bersihkanLokasiVideoPertemuan();
		tulisLokasiVideoPertemuan(new JSONObject().toString(), getId());
		for (VideoPertemuan videoPertemuan : videoPertemuans) {

			if (videoPertemuan.getNama().equalsIgnoreCase("link") && videoPertemuan.getLink() == null
					&& videoPertemuan.getFoto() != null) {
				File fileData = videoPertemuan.ambilFile();
				if (fileData != null && fileData.exists()) {
					String link = LampiranLain.ambilLinkLampiranLain(fileData);
					videoPertemuan.setLink(link);
//					System.out.println("link video " + videoPertemuan + " baru " + link);
					masukkanData(VideoPertemuan.class, videoPertemuan);
					populateVideoPertemuan(videoPertemuan, true);
				} else {
//					System.out.println("link video " + videoPertemuan + " tidak valid dan file "
//							+ fileData.getAbsolutePath() + " tidak ada");
				}
			} else if (videoPertemuan.getNama().equalsIgnoreCase("link") && videoPertemuan.getLink() == null
					&& videoPertemuan.getFoto() == null) {
//				System.out.println("link video " + videoPertemuan + " tidak valid");
			} else {
				masukkanData(VideoPertemuan.class, videoPertemuan);
				populateVideoPertemuan(videoPertemuan, true);
			}

		}
		videoPertemuans = null;
	}

	/**
	 * Keluarkan satu video dari peta lokasi.
	 *
	 * <p>Sedikit lebih aman daripada {@link #removePertemuanFileContent(Serializable)} karena
	 * menolak {@code id} bernilai {@code null}, TETAPI mengulang kekeliruan yang sama: {@code id}
	 * video dipakai juga sebagai id pertemuan saat membaca/menulis peta, sehingga peta yang
	 * disentuh bukan peta induk yang sebenarnya.</p>
	 *
	 * @param id id video yang dikeluarkan; {@code null} diabaikan
	 * @see #removePertemuanFileContent(Serializable)
	 */
	public static void removeVideoPertemuan(Serializable id) {
		if (id == null) {
			return;
		}
		try {
			JSONObject c = jsonObjekUntukTulis(ambilLokasiVideoPertemuan(id));
			c.put(id.toString(), "");
			tulisLokasiVideoPertemuan(c.toString(), id);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3177");

		}
	}

	/**
	 * Daftarkan satu video ke peta lokasi pertemuan pemiliknya.
	 *
	 * <p>Sama seperti {@link #populatePertemuanFileContent(PertemuanFileContent, boolean)}:
	 * memanggil {@code write()} yang menulis berkas cache, lalu menyimpan
	 * {@code "id" -> "path berkas"} ke peta milik induk yang benar.</p>
	 *
	 * @param videoPertemuan video yang didaftarkan; {@code null} diabaikan
	 * @param tulisUlang     tidak dipakai
	 */
	public static void populateVideoPertemuan(VideoPertemuan videoPertemuan, boolean tulisUlang) {
		try {
			if (videoPertemuan == null) {
				return;
			}

			JSONObject c = jsonObjekUntukTulis(ambilLokasiVideoPertemuan(videoPertemuan.getPertemuan()));
			c.put(videoPertemuan.getId().toString(), videoPertemuan.write().getAbsolutePath());
			tulisLokasiVideoPertemuan(c.toString(), videoPertemuan.getPertemuan());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3191");
		}
	}

	/**
	 * Adakah rekaman video pada pertemuan ini?
	 *
	 * <p>Berbeda dari {@link #fileContent()}, tidak ada jalan pintas untuk sesi ujian — jawabannya
	 * murni bergantung pada isi peta. Namun tetap tidak hemat: seluruh daftar dimuat lewat
	 * {@link #ambilVideoPertemuanTotal()} lalu ukurannya diperiksa. Pakai
	 * {@link #ambilJumlahVideoPertemuan()} bila hanya butuh jumlah.</p>
	 *
	 * @return {@code true} bila ada minimal satu video
	 */
	public boolean videoPertemuan() {

		TreeMap<Long, VideoPertemuan> videoPertemuansa = ambilVideoPertemuanTotal();
		int ada = videoPertemuansa.size();
		videoPertemuansa = null;
		return ada > 0;
	}

	/**
	 * Banyaknya rekaman video pada pertemuan ini.
	 *
	 * <p>Dihitung langsung dari peta lokasi tanpa memuat objek video. Sama seperti
	 * {@link #ambilJumlahPertemuanFileContent()}, angkanya bisa lebih besar daripada ukuran hasil
	 * {@link #ambilVideoPertemuanTotal()} karena entri {@code "link"} yang rusak tetap dihitung
	 * di sini tetapi disaring keluar di sana.</p>
	 *
	 * <p>Session {@link StreamingHibernateUtil} yang dipakai untuk membangun ulang peta TIDAK
	 * ditutup pada jalur ini — berbeda dari {@link #ambilVideoPertemuanTotal()}.</p>
	 *
	 * @return banyaknya entri video pada peta lokasi; {@code 0} bila pertemuan belum punya id
	 */
	@SuppressWarnings("unchecked")
	public int ambilJumlahVideoPertemuan() {
		if (getId() == null) {
			return 0;
		}
		if (!udah("video_pertemuan")) {
			Session session = StreamingHibernateUtil.getInstance().currentSession();
			try {
				reInitVideoPertemuan(session);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:3211");
			}
		}
		int jumlah = 0;
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiVideoPertemuan(getId()));
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						jumlah++;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3226");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3230");

		}
		return jumlah;
	}

	/**
	 * Seluruh rekaman video pertemuan ini.
	 *
	 * <p>Alurnya sama persis dengan {@link #ambilPertemuanFileContentTotal(boolean)}: baca peta,
	 * cari objek di cache, jatuhkan ke pembacaan berkas cache bila tidak ada, dan saring entri
	 * {@code "link"} yang rusak. Urutannya juga MENURUN sehingga rekaman terbaru muncul lebih
	 * dulu.</p>
	 *
	 * <p>Berbeda dari padanan lampirannya, method ini tidak punya parameter {@code refresh}:
	 * peta hanya dibangun ulang bila penanda {@code "video_pertemuan"} memang belum ada.</p>
	 *
	 * @return peta {@code id -> video}, terurut menurun menurut id; kosong bila pertemuan belum
	 *         punya id
	 * @see #ambilPertemuanFileContentTotal(boolean)
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<Long, VideoPertemuan> ambilVideoPertemuanTotal() {
		TreeMap<Long, VideoPertemuan> videoPertemuansa = new TreeMap<Long, VideoPertemuan>(Collections.reverseOrder());
		if (getId() == null) {
			return videoPertemuansa;
		}
		if (!udah("video_pertemuan")) {
			Session session = StreamingHibernateUtil.getInstance().currentSession();
			try {
				reInitVideoPertemuan(session);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:3244");
			}
			StreamingHibernateUtil.getInstance().closeSession();
		}
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiVideoPertemuan(getId()));
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(VideoPertemuan.class, key);
						if (generalValueObject != null) {
							VideoPertemuan videoPertemuan = (VideoPertemuan) generalValueObject;
							videoPertemuan.setPertemuan(this.getId());

							if ("link".equalsIgnoreCase(videoPertemuan.getNama()) && videoPertemuan.getLink() == null
									&& videoPertemuan.getFoto() != null) {
								File fileData = videoPertemuan.ambilFile();
								if (fileData != null && fileData.exists()) {
									String link = LampiranLain.ambilLinkLampiranLain(fileData);
									videoPertemuan.setLink(link);
//									System.out.println("link video " + videoPertemuan + " baru " + link);
									videoPertemuansa.put(videoPertemuan.getId(), videoPertemuan);
								} else {
//									System.out.println("link video " + videoPertemuan + " tidak valid dan file "
//											+ fileData.getAbsolutePath() + " tidak ada");
								}
							} else if ("link".equalsIgnoreCase(videoPertemuan.getNama())
									&& videoPertemuan.getLink() == null && videoPertemuan.getFoto() == null) {
//								System.out.println("link video " + videoPertemuan + " tidak valid");
							} else {
								videoPertemuansa.put(videoPertemuan.getId(), videoPertemuan);
							}

						} else {

							File file = new File(s);
							if (file != null && file.exists()) {
								JSONObject isiBerkas = bacaJsonObjekAman(ais.common.BacaTulisUtil.baca(file));
								if (isiBerkas == null) {
									// Berkas ADA tetapi isinya bukan JSON objek (kosong/terpotong/format lama).
									// Dilewati -- hasil akhirnya sama dengan perilaku lama yang melewatinya
									// lewat exception, tanpa membanjiri log pada tiap siklus warmup cache.
									continue;
								}
								VideoPertemuan videoPertemuan = (VideoPertemuan) Common.convertToObject(
										isiBerkas, VideoPertemuan.class);
								videoPertemuan.setPertemuan(this.getId());

								if ("link".equalsIgnoreCase(videoPertemuan.getNama())
										&& videoPertemuan.getLink() == null && videoPertemuan.getFoto() != null) {
									File fileData = videoPertemuan.ambilFile();
									if (fileData != null && fileData.exists()) {
										String link = LampiranLain.ambilLinkLampiranLain(fileData);
										videoPertemuan.setLink(link);
//										System.out.println("link video " + videoPertemuan + " baru " + link);
										videoPertemuansa.put(videoPertemuan.getId(), videoPertemuan);
									} else {
//										System.out.println("link video " + videoPertemuan + " tidak valid dan file "
//												+ fileData.getAbsolutePath() + " tidak ada");
									}
								} else if ("link".equalsIgnoreCase(videoPertemuan.getNama())
										&& videoPertemuan.getLink() == null && videoPertemuan.getFoto() == null) {
//									System.out.println("link video " + videoPertemuan + " tidak valid");
								} else {
									videoPertemuansa.put(videoPertemuan.getId(), videoPertemuan);
								}
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3311");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3315");

		}
		return videoPertemuansa;
	}

	/**
	 * Potong satu halaman dari peta video yang sudah dikumpulkan.
	 *
	 * @param videoPertemuansa peta hasil {@link #ambilVideoPertemuanTotal()}
	 * @param mulai            indeks awal halaman (berbasis nol)
	 * @param banyak           banyaknya entri per halaman
	 * @return daftar video pada halaman yang diminta
	 * @see #ambilPertemuanPunyaUjian(TreeMap, int, int)
	 */
	public List<VideoPertemuan> ambilVideoPertemuan(TreeMap<Long, VideoPertemuan> videoPertemuansa, int mulai,
			int banyak) {

		int index = 0;
		List<VideoPertemuan> videoPertemuans = new ArrayList<VideoPertemuan>();
		Iterator<Long> i = videoPertemuansa.keySet().iterator();
		while (i.hasNext()) {
			VideoPertemuan videoPertemuan = videoPertemuansa.get(i.next());

			if (index >= mulai && index < (mulai + banyak)) {
				videoPertemuans.add(videoPertemuan);
			}
			index++;

		}
		return videoPertemuans;
	}

	/**
	 * Baca peta lokasi rekaman audio ({@link AudioPertemuan}) milik satu pertemuan.
	 *
	 * <p>Kembaran {@link #ambilLokasiVideoPertemuan(Serializable)} untuk rekaman audio; seluruh
	 * blok audio adalah salinan blok video dengan tipe yang diganti.</p>
	 *
	 * @param id id pertemuan pemilik audio
	 * @return isi peta lokasi audio sebagai teks JSON; JSON kosong bila belum ada
	 * @see #ambilLokasiPertemuanFileContent(Serializable)
	 */
	public static String ambilLokasiAudioPertemuan(Serializable id) {
		if (id == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(Pertemuan.class, id, "audio_pertemuan_" + id.toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3345");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Tulis ulang seluruh isi peta lokasi audio milik satu pertemuan.
	 *
	 * @param data isi peta lokasi baru sebagai teks JSON
	 * @param id   id pertemuan pemilik audio
	 * @see #ambilLokasiAudioPertemuan(Serializable)
	 */
	public static void tulisLokasiAudioPertemuan(String data, Serializable id) {
		File file = Common.getFileLocation(Pertemuan.class, id, "audio_pertemuan_" + id.toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3354");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Hapus berkas peta lokasi audio milik pertemuan ini.
	 *
	 * @see #bersihkanLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public void bersihkanLokasiAudioPertemuan() {
		File file = Common.getFileLocation(this, "audio_pertemuan_" + getId().toString());
		BacaTulisUtil.doHapus(file, "audio_pertemuan");

	}

	/**
	 * Bangun ulang peta lokasi audio dari basis data, sekaligus menambal tautan yang hilang.
	 *
	 * <p>Salinan {@link #reInitVideoPertemuan(Session)} untuk {@link AudioPertemuan}.</p>
	 *
	 * @param session session Hibernate yang sudah terbuka; tidak ditutup oleh method ini
	 * @throws Exception bila query atau pembacaan berkas gagal
	 * @see #reInitPertemuanFileContent(Session)
	 */
	@SuppressWarnings("unchecked")
	public void reInitAudioPertemuan(Session session) throws Exception {
		List<AudioPertemuan> audioPertemuans = session.createCriteria(AudioPertemuan.class).addOrder(Order.asc("id"))
				.add(Restrictions.eq("pertemuan", this.getId())).list();
		bersihkanLokasiAudioPertemuan();
		tulisLokasiAudioPertemuan(new JSONObject().toString(), getId());
		for (AudioPertemuan audioPertemuan : audioPertemuans) {

			if (audioPertemuan.getNama().equalsIgnoreCase("link") && audioPertemuan.getLink() == null
					&& audioPertemuan.getFoto() != null) {
				File fileData = audioPertemuan.ambilFile();
				if (fileData != null && fileData.exists()) {
					String link = LampiranLain.ambilLinkLampiranLain(fileData);
					audioPertemuan.setLink(link);
//					System.out.println("link audio " + audioPertemuan + " baru " + link);
					masukkanData(AudioPertemuan.class, audioPertemuan);
					populateAudioPertemuan(audioPertemuan, true);
				} else {
//					System.out.println("link audio " + audioPertemuan + " tidak valid dan file "
//							+ fileData.getAbsolutePath() + " tidak ada");
				}
			} else if (audioPertemuan.getNama().equalsIgnoreCase("link") && audioPertemuan.getLink() == null
					&& audioPertemuan.getFoto() == null) {
//				System.out.println("link audio " + audioPertemuan + " tidak valid");
			} else {
				masukkanData(AudioPertemuan.class, audioPertemuan);
				populateAudioPertemuan(audioPertemuan, true);
			}
		}
		audioPertemuans = null;
	}

	/**
	 * Keluarkan satu audio dari peta lokasi.
	 *
	 * <p>Mengulang kekeliruan yang sama dengan {@link #removePertemuanFileContent(Serializable)}
	 * dan {@link #removeVideoPertemuan(Serializable)}: id audio dipakai juga sebagai id pertemuan.
	 * Bahkan tanpa penjagaan {@code null} yang ada pada versi videonya.</p>
	 *
	 * @param id id audio yang dikeluarkan
	 * @see #removePertemuanFileContent(Serializable)
	 */
	public static void removeAudioPertemuan(Serializable id) {
		try {
			JSONObject c = jsonObjekUntukTulis(ambilLokasiAudioPertemuan(id));
			c.put(id.toString(), "");
			tulisLokasiAudioPertemuan(c.toString(), id);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3403");

		}
	}

	/**
	 * Daftarkan satu audio ke peta lokasi pertemuan pemiliknya.
	 *
	 * @param audioPertemuan audio yang didaftarkan; {@code null} diabaikan
	 * @param tulisUlang     tidak dipakai
	 * @see #populatePertemuanFileContent(PertemuanFileContent, boolean)
	 */
	public static void populateAudioPertemuan(AudioPertemuan audioPertemuan, boolean tulisUlang) {
		try {
			if (audioPertemuan == null) {
				return;
			}

			JSONObject c = jsonObjekUntukTulis(ambilLokasiAudioPertemuan(audioPertemuan.getPertemuan()));
			c.put(audioPertemuan.getId().toString(), audioPertemuan.write().getAbsolutePath());
			tulisLokasiAudioPertemuan(c.toString(), audioPertemuan.getPertemuan());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3417");
		}
	}

	/**
	 * Adakah rekaman audio pada pertemuan ini?
	 *
	 * @return {@code true} bila ada minimal satu audio
	 * @see #videoPertemuan()
	 */
	public boolean audioPertemuan() {

		TreeMap<Long, AudioPertemuan> audioPertemuansa = ambilAudioPertemuanTotal();
		int ada = audioPertemuansa.size();
		audioPertemuansa = null;
		return ada > 0;
	}

	/**
	 * Banyaknya rekaman audio pada pertemuan ini.
	 *
	 * <p>Dihitung langsung dari peta lokasi tanpa memuat objek audio.</p>
	 *
	 * @return banyaknya entri audio pada peta lokasi; {@code 0} bila pertemuan belum punya id
	 * @see #ambilJumlahVideoPertemuan()
	 */
	@SuppressWarnings("unchecked")
	public int ambilJumlahAudioPertemuan() {
		if (getId() == null) {
			return 0;
		}
		if (!udah("audio_pertemuan")) {
			Session session = StreamingHibernateUtil.getInstance().currentSession();
			try {
				reInitAudioPertemuan(session);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:3437");
			}
		}
		int jumlah = 0;
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiAudioPertemuan(getId()));
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						jumlah++;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3452");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3456");

		}
		return jumlah;
	}

	/**
	 * Seluruh rekaman audio pertemuan ini.
	 *
	 * <p>Salinan {@link #ambilVideoPertemuanTotal()} untuk {@link AudioPertemuan}, dengan satu
	 * perbedaan yang perlu diketahui: method ini TIDAK menjaga terhadap {@code getId()} bernilai
	 * {@code null} di awal, sehingga memanggilnya pada pertemuan yang belum tersimpan berbeda
	 * perilakunya dari padanan videonya.</p>
	 *
	 * @return peta {@code id -> audio}, terurut menurun menurut id
	 * @see #ambilVideoPertemuanTotal()
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<Long, AudioPertemuan> ambilAudioPertemuanTotal() {
		if (!udah("audio_pertemuan")) {
			Session session = StreamingHibernateUtil.getInstance().currentSession();
			try {
				reInitAudioPertemuan(session);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:3470");
			}
			StreamingHibernateUtil.getInstance().closeSession();
		}
		TreeMap<Long, AudioPertemuan> audioPertemuansa = new TreeMap<Long, AudioPertemuan>(Collections.reverseOrder());
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiAudioPertemuan(getId()));
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(AudioPertemuan.class, key);
						if (generalValueObject != null) {
							AudioPertemuan audioPertemuan = (AudioPertemuan) generalValueObject;
							audioPertemuan.setPertemuan(this.getId());

							if (audioPertemuan.getNama().equalsIgnoreCase("link") && audioPertemuan.getLink() == null
									&& audioPertemuan.getFoto() != null) {
								File fileData = audioPertemuan.ambilFile();
								if (fileData != null && fileData.exists()) {
									String link = LampiranLain.ambilLinkLampiranLain(fileData);
									audioPertemuan.setLink(link);
//									System.out.println("link audio " + audioPertemuan + " baru " + link);
									audioPertemuansa.put(audioPertemuan.getId(), audioPertemuan);
								} else {
//									System.out.println("link audio " + audioPertemuan + " tidak valid dan file "
//											+ fileData.getAbsolutePath() + " tidak ada");
								}
							} else if (audioPertemuan.getNama().equalsIgnoreCase("link")
									&& audioPertemuan.getLink() == null && audioPertemuan.getFoto() == null) {
//								System.out.println("link audio " + audioPertemuan + " tidak valid");
							} else {
								audioPertemuansa.put(audioPertemuan.getId(), audioPertemuan);
							}

						} else {

							File file = new File(s);
							if (file != null && file.exists()) {
								JSONObject isiBerkas = bacaJsonObjekAman(ais.common.BacaTulisUtil.baca(file));
								if (isiBerkas == null) {
									// Berkas ADA tetapi isinya bukan JSON objek (kosong/terpotong/format lama).
									// Dilewati -- hasil akhirnya sama dengan perilaku lama yang melewatinya
									// lewat exception, tanpa membanjiri log pada tiap siklus warmup cache.
									continue;
								}
								AudioPertemuan audioPertemuan = (AudioPertemuan) Common.convertToObject(
										isiBerkas, AudioPertemuan.class);
								audioPertemuan.setPertemuan(this.getId());

								if (audioPertemuan.getNama().equalsIgnoreCase("link")
										&& audioPertemuan.getLink() == null && audioPertemuan.getFoto() != null) {
									File fileData = audioPertemuan.ambilFile();
									if (fileData != null && fileData.exists()) {
										String link = LampiranLain.ambilLinkLampiranLain(fileData);
										audioPertemuan.setLink(link);
//										System.out.println("link audio " + audioPertemuan + " baru " + link);
										audioPertemuansa.put(audioPertemuan.getId(), audioPertemuan);
									} else {
//										System.out.println("link audio " + audioPertemuan + " tidak valid dan file "
//												+ fileData.getAbsolutePath() + " tidak ada");
									}
								} else if (audioPertemuan.getNama().equalsIgnoreCase("link")
										&& audioPertemuan.getLink() == null && audioPertemuan.getFoto() == null) {
//									System.out.println("link audio " + audioPertemuan + " tidak valid");
								} else {
									audioPertemuansa.put(audioPertemuan.getId(), audioPertemuan);
								}
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3537");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3541");

		}
		return audioPertemuansa;
	}

	/**
	 * Potong satu halaman dari peta audio yang sudah dikumpulkan.
	 *
	 * @param audioPertemuansa peta hasil {@link #ambilAudioPertemuanTotal()}
	 * @param mulai            indeks awal halaman (berbasis nol)
	 * @param banyak           banyaknya entri per halaman
	 * @return daftar audio pada halaman yang diminta
	 * @see #ambilPertemuanPunyaUjian(TreeMap, int, int)
	 */
	public List<AudioPertemuan> ambilAudioPertemuan(TreeMap<Long, AudioPertemuan> audioPertemuansa, int mulai,
			int banyak) {

		int index = 0;
		List<AudioPertemuan> audioPertemuans = new ArrayList<AudioPertemuan>();
		Iterator<Long> i = audioPertemuansa.keySet().iterator();
		while (i.hasNext()) {
			AudioPertemuan audioPertemuan = audioPertemuansa.get(i.next());

			if (index >= mulai && index < (mulai + banyak)) {
				audioPertemuans.add(audioPertemuan);
			}
			index++;

		}
		return audioPertemuans;
	}

	/**
	 * Syarat yang harus dipenuhi peserta sebelum boleh mengumpulkan tugas pertemuan ini.
	 *
	 * <p>Implementasi properti abstrak milik {@link Tugas}.</p>
	 *
	 * @return syarat pengumpulan tugas, atau {@code null} bila tanpa syarat
	 * @see Tugas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "syarat_mengumpulkan_tugas", nullable = true)
	public SyaratUjian getSyaratMengumpulkanTugas() {
		syaratMengumpulkanTugas = check(syaratMengumpulkanTugas);
		return syaratMengumpulkanTugas;
	}

	/**
	 * Setel syarat pengumpulan tugas pertemuan ini.
	 *
	 * @param syaratMengumpulkanTugas syarat pengumpulan; boleh {@code null}
	 */
	public void setSyaratMengumpulkanTugas(SyaratUjian syaratMengumpulkanTugas) {
		this.syaratMengumpulkanTugas = syaratMengumpulkanTugas;
	}

	/**
	 * Keanggotaan grup pertemuan yang menjadi induk pertemuan ini.
	 *
	 * <p>Ini induk "penampung serbaguna": dipakai untuk sesi konsultasi/kegiatan yang tidak masuk
	 * salah satu kategori akademik baku. {@link #info()} menyebutnya {@code "Konsultasi lain"} bila
	 * grupnya pun tidak ada.</p>
	 *
	 * @return {@link PertemuanPunyaGrupPertemuan} induk, atau {@code null}
	 * @see #untuk()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertemuan_punya_grup_pertemuan", nullable = true)
	public PertemuanPunyaGrupPertemuan getPertemuanPunyaGrupPertemuan() {
		return pertemuanPunyaGrupPertemuan;
	}

	/**
	 * Tetapkan keanggotaan grup pertemuan sebagai induk pertemuan ini.
	 *
	 * @param pertemuanPunyaGrupPertemuan keanggotaan grup pertemuan; boleh {@code null}
	 */
	public void setPertemuanPunyaGrupPertemuan(PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan) {
		this.pertemuanPunyaGrupPertemuan = pertemuanPunyaGrupPertemuan;
	}

	/**
	 * Baca peta lokasi tugas perorangan ({@link TugasPertemuan}) milik pertemuan ini.
	 *
	 * <p>Kembali ke gaya kuintet yang menyimpan {@code "id" -> "id"} (bukan path berkas), seperti
	 * blok izin/diskusi/ujian. Nama berkas dan penanda cache-nya {@code "pertemuan_tugas"} —
	 * perhatikan bahwa nama itu TIDAK sama dengan nama kelas anaknya, jadi jangan menebaknya dari
	 * nama method.</p>
	 *
	 * <p>Jangan tertukar antara TIGA hal berbeda yang semuanya "tugas" di sekitar kelas ini:
	 * {@link Tugas} (kelas induk {@link Pertemuan} sendiri), {@link TugasPertemuan} (tugas
	 * perorangan anak pertemuan), dan {@link TugasKelompok} (tugas kelompok anak pertemuan).</p>
	 *
	 * @return isi peta lokasi tugas sebagai teks JSON; JSON kosong bila belum ada atau pertemuan
	 *         belum punya id
	 * @see #ambilLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public String ambilLokasiTugasPertemuan() {
		if (getId() == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(this, "pertemuan_tugas_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3593");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Tulis ulang seluruh isi peta lokasi tugas perorangan milik pertemuan ini.
	 *
	 * @param data isi peta lokasi baru sebagai teks JSON
	 * @see #ambilLokasiTugasPertemuan()
	 */
	public void tulisLokasiTugasPertemuan(String data) {
		File file = Common.getFileLocation(this, "pertemuan_tugas_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3602");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Hapus berkas peta lokasi tugas perorangan milik pertemuan ini.
	 *
	 * @see #bersihkanLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public void bersihkanLokasiTugasPertemuan() {
		File file = Common.getFileLocation(this, "pertemuan_tugas_" + getId().toString());
		BacaTulisUtil.doHapus(file, "pertemuan_tugas");

	}

	/**
	 * Bangun ulang peta lokasi tugas perorangan dari basis data.
	 *
	 * <p>Query-nya menambahkan satu penyaring yang tidak ada pada kuintet lain:
	 * {@code Restrictions.ne("judultugas", "")} — tugas yang judulnya masih kosong (draf) sengaja
	 * TIDAK dimasukkan ke peta. Penyaring yang sama diulang lagi saat membaca di
	 * {@link #ambilTugasPertemuanTotal()}.</p>
	 *
	 * <p><b>Catatan:</b> penyaring {@code ne("judultugas", "")} tidak menangkap judul bernilai
	 * {@code NULL} (perbandingan dengan {@code NULL} di SQL tidak menghasilkan benar), sehingga
	 * tugas berjudul {@code NULL} tetap masuk peta dan baru tersaring saat dibaca.</p>
	 *
	 * @param session session Hibernate yang sudah terbuka; tidak ditutup oleh method ini
	 * @see #reInitPengajuanIzinTidakMasukPerkuliahan(Session)
	 */
	@SuppressWarnings("unchecked")
	public void reInitTugasPertemuan(Session session) {
		List<Long> tugasPertemuans = session.createCriteria(TugasPertemuan.class).addOrder(Order.asc("id"))
				.setProjection(Projections.property("id")).add(Restrictions.ne("judultugas", ""))
				.add(Restrictions.eq("pertemuan", this.getId())).list();
		bersihkanLokasiTugasPertemuan();
		tulisLokasiTugasPertemuan(new JSONObject().toString());
		for (Long tugasPertemuan : tugasPertemuans) {
			populateTugasPertemuan(tugasPertemuan, true);
		}
		tugasPertemuans = null;
	}

	/**
	 * Keluarkan satu tugas perorangan dari peta lokasi pertemuan ini.
	 *
	 * @param id id tugas yang dikeluarkan
	 * @see #removePengajuanIzinTidakMasukPerkuliahan(Serializable)
	 */
	public void removeTugasPertemuan(Serializable id) {
		try {
			JSONObject c = jsonObjekUntukTulis(ambilLokasiTugasPertemuan());
			c.put(id.toString(), "");
			tulisLokasiTugasPertemuan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3632");

		}
	}

	/**
	 * Daftarkan satu tugas perorangan ke peta lokasi pertemuan ini.
	 *
	 * @param tugasPertemuan id tugas yang didaftarkan; {@code null} diabaikan
	 * @param tulisUlang     tidak dipakai
	 * @see #populatePengajuanIzinTidakMasukPerkuliahan(Long)
	 */
	public void populateTugasPertemuan(Long tugasPertemuan, boolean tulisUlang) {
		try {
			if (tugasPertemuan == null) {
				return;
			}
			JSONObject c = jsonObjekUntukTulis(ambilLokasiTugasPertemuan());
			c.put(tugasPertemuan.toString(), tugasPertemuan.toString());
			tulisLokasiTugasPertemuan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3645");
		}
	}

	/**
	 * Seluruh tugas perorangan pada pertemuan ini.
	 *
	 * <p>Hanya membaca dari cache ({@code ambilData(..., true)}); tidak ada jalur cadangan
	 * pembacaan berkas seperti pada lampiran/video/audio, dan tidak ada query susulan seperti pada
	 * diskusi. Entri peta yang objeknya tidak ada di cache akan HILANG dari hasil.</p>
	 *
	 * <p>Tugas yang judulnya kosong disaring keluar — pengulangan penyaring yang sudah diterapkan
	 * saat peta dibangun oleh {@link #reInitTugasPertemuan(Session)}, dan di sinilah tugas
	 * berjudul {@code NULL} akhirnya tersaring (lewat {@link #getJudultugas()} yang mengubah
	 * {@code null} menjadi string kosong).</p>
	 *
	 * <p>Session yang dipakai untuk membangun ulang peta ditutup dengan benar di blok
	 * {@code finally}.</p>
	 *
	 * @return peta {@code id -> tugas}, terurut MENAIK menurut id; kosong bila pertemuan belum
	 *         punya id
	 * @see #ambilTugasKelompokTotal(boolean)
	 * @see #ambilTugasTotalSemua()
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<Long, TugasPertemuan> ambilTugasPertemuanTotal() {
		TreeMap<Long, TugasPertemuan> tugasPertemuansa = new TreeMap<Long, TugasPertemuan>();
		if (getId() == null) {
			return tugasPertemuansa;
		}
		if (!udah("pertemuan_tugas")) {
			Session session = HibernateUtil.currentNativeSession();
			try {
				reInitTugasPertemuan(session);
			} finally {
				if (session != null && session.isOpen()) {
					session.clear();
					if (session.isConnected()) {
						session.disconnect();
					}
					session.close();
				}
			}
		}
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiTugasPertemuan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						GeneralValueObject generalValueObject = ambilData(TugasPertemuan.class, key, true);
						if (generalValueObject != null) {
							TugasPertemuan tugasPertemuan = (TugasPertemuan) generalValueObject;
							if (tugasPertemuan != null && tugasPertemuan.getId() != null
									&& !tugasPertemuan.getJudultugas().isEmpty()) {
								tugasPertemuan.setPertemuan(this.getId());
								tugasPertemuansa.put(tugasPertemuan.getId(), tugasPertemuan);
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3675");
//					e.printStackTrace();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3679");
//			e.printStackTrace();
		}
		return tugasPertemuansa;
	}

	/**
	 * Gabungan seluruh tugas perorangan DAN tugas kelompok pada pertemuan ini.
	 *
	 * <p>Hasilnya berkunci id dan bertipe {@link Tugas} (induk bersama {@link TugasPertemuan} dan
	 * {@link TugasKelompok}), sehingga UI dapat menampilkan keduanya dalam satu daftar.</p>
	 *
	 * <p><b>Peringatan penting:</b> kedua peta digabung dengan {@code putAll} pada kunci id.
	 * {@link TugasPertemuan} dan {@link TugasKelompok} adalah tabel yang BERBEDA dengan urutan id
	 * masing-masing, sehingga sebuah tugas perorangan dan sebuah tugas kelompok sangat mungkin
	 * punya id yang sama. Bila itu terjadi, tugas kelompok MENIMPA tugas perorangan dan tugas
	 * perorangan itu hilang dari daftar tanpa jejak. Ini risiko nyata, bukan teoretis.</p>
	 *
	 * @return peta {@code id -> tugas} gabungan
	 * @see #ambilTugasPertemuanTotal()
	 * @see #ambilTugasKelompokTotal()
	 */
	public TreeMap<Long, Tugas> ambilTugasTotalSemua() {
		TreeMap<Long, Tugas> tugasPertemuansa = new TreeMap<Long, Tugas>();
		tugasPertemuansa.putAll(ambilTugasPertemuanTotal());
		tugasPertemuansa.putAll(ambilTugasKelompokTotal());
		return tugasPertemuansa;
	}

	/**
	 * Potong satu halaman dari peta tugas perorangan yang sudah dikumpulkan.
	 *
	 * @param tugasPertemuansa peta hasil {@link #ambilTugasPertemuanTotal()}
	 * @param mulai            indeks awal halaman (berbasis nol)
	 * @param banyak           banyaknya entri per halaman
	 * @return daftar tugas pada halaman yang diminta
	 * @see #ambilPertemuanPunyaUjian(TreeMap, int, int)
	 */
	public List<TugasPertemuan> ambilTugasPertemuan(TreeMap<Long, TugasPertemuan> tugasPertemuansa, int mulai,
			int banyak) {

		int index = 0;
		List<TugasPertemuan> tugasPertemuans = new ArrayList<TugasPertemuan>();
		Iterator<Long> i = tugasPertemuansa.keySet().iterator();
		while (i.hasNext()) {
			TugasPertemuan tugasPertemuan = tugasPertemuansa.get(i.next());

			if (index >= mulai && index < (mulai + banyak)) {
				tugasPertemuans.add(tugasPertemuan);
			}
			index++;

		}
		return tugasPertemuans;
	}

	/**
	 * Baca peta lokasi tugas kelompok ({@link TugasKelompok}) milik pertemuan ini.
	 *
	 * <p>Nama berkas dan penanda cache-nya {@code "kelompok_tugas"} — perhatikan urutan katanya
	 * TERBALIK dari nama kelasnya ({@code TugasKelompok}), dan berbeda pula dari
	 * {@code "pertemuan_tugas"} milik tugas perorangan.</p>
	 *
	 * @return isi peta lokasi tugas kelompok sebagai teks JSON; JSON kosong bila belum ada
	 * @see #ambilLokasiTugasPertemuan()
	 */
	public String ambilLokasiTugasKelompok() {
		File file = Common.getFileLocation(this, "kelompok_tugas_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3716");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Tulis ulang seluruh isi peta lokasi tugas kelompok milik pertemuan ini.
	 *
	 * @param data isi peta lokasi baru sebagai teks JSON
	 * @see #ambilLokasiTugasKelompok()
	 */
	public void tulisLokasiTugasKelompok(String data) {
		File file = Common.getFileLocation(this, "kelompok_tugas_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3725");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Hapus berkas peta lokasi tugas kelompok milik pertemuan ini.
	 *
	 * @see #bersihkanLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public void bersihkanLokasiTugasKelompok() {
		File file = Common.getFileLocation(this, "kelompok_tugas_" + getId().toString());
		BacaTulisUtil.doHapus(file, "kelompok_tugas");

	}

	/**
	 * Bangun ulang peta lokasi tugas kelompok dari basis data.
	 *
	 * <p>Sejajar {@link #reInitTugasPertemuan(Session)}, tetapi menyaring dengan
	 * {@code Restrictions.ne("judul", "")} — perhatikan nama propertinya {@code "judul"}, BUKAN
	 * {@code "judultugas"} seperti pada tugas perorangan, padahal pembacanya
	 * ({@link #ambilTugasKelompokTotal(boolean)}) menyaring memakai {@code getJudultugas()}.
	 * Ketidakselarasan nama ini patut diingat bila suatu saat tugas kelompok tampak "hilang" atau
	 * "muncul" tak terduga.</p>
	 *
	 * @param session session Hibernate yang sudah terbuka; tidak ditutup oleh method ini
	 * @see #reInitTugasPertemuan(Session)
	 */
	@SuppressWarnings("unchecked")
	public void reInitTugasKelompok(Session session) {
		List<Long> tugasKelompoks = session.createCriteria(TugasKelompok.class).addOrder(Order.asc("id"))
				.setProjection(Projections.property("id")).add(Restrictions.ne("judul", ""))
				.add(Restrictions.eq("pertemuan", this.getId())).list();
		bersihkanLokasiTugasKelompok();
		tulisLokasiTugasKelompok(new JSONObject().toString());
		for (Long tugasKelompok : tugasKelompoks) {
			populateTugasKelompok(tugasKelompok, true);
		}
		tugasKelompoks = null;
	}

	/**
	 * Keluarkan satu tugas kelompok dari peta lokasi pertemuan ini.
	 *
	 * @param id id tugas kelompok yang dikeluarkan
	 * @see #removePengajuanIzinTidakMasukPerkuliahan(Serializable)
	 */
	public void removeTugasKelompok(Serializable id) {
		try {
			JSONObject c = jsonObjekUntukTulis(ambilLokasiTugasKelompok());
			c.put(id.toString(), "");
			tulisLokasiTugasKelompok(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3755");

		}
	}

	/**
	 * Daftarkan satu tugas kelompok ke peta lokasi pertemuan ini.
	 *
	 * @param tugasKelompok id tugas kelompok yang didaftarkan; {@code null} diabaikan
	 * @param tulisUlang    tidak dipakai
	 * @see #populatePengajuanIzinTidakMasukPerkuliahan(Long)
	 */
	public void populateTugasKelompok(Long tugasKelompok, boolean tulisUlang) {
		try {
			if (tugasKelompok == null) {
				return;
			}
			JSONObject c = jsonObjekUntukTulis(ambilLokasiTugasKelompok());
			c.put(tugasKelompok.toString(), tugasKelompok.toString());
			tulisLokasiTugasKelompok(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3768");
		}
	}

	/**
	 * Seluruh tugas kelompok pada pertemuan ini, memakai peta lokasi yang sudah ada.
	 *
	 * @return peta {@code id -> tugas kelompok}, terurut menaik menurut id
	 * @see #ambilTugasKelompokTotal(boolean)
	 */
	public TreeMap<Long, TugasKelompok> ambilTugasKelompokTotal() {
		return ambilTugasKelompokTotal(false);
	}

	/**
	 * Seluruh tugas kelompok pada pertemuan ini.
	 *
	 * <p>Sejajar {@link #ambilTugasPertemuanTotal()}: hanya membaca dari cache, tanpa jalur
	 * cadangan berkas maupun query susulan, dan menyaring tugas yang judulnya kosong.</p>
	 *
	 * <p><b>Berbeda dari padanan perorangannya</b>, method ini TIDAK menjaga terhadap
	 * {@code getId()} bernilai {@code null}, dan session yang dibuka untuk membangun ulang peta
	 * ditutup lewat {@code HibernateUtil.closeSession()} biasa — bukan di dalam blok
	 * {@code finally}. Bila {@link #reInitTugasKelompok(Session)} melempar, session itu tidak
	 * tertutup.</p>
	 *
	 * @param refresh {@code true} untuk memaksa peta dibangun ulang dari basis data lebih dahulu
	 * @return peta {@code id -> tugas kelompok}, terurut menaik menurut id
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<Long, TugasKelompok> ambilTugasKelompokTotal(boolean refresh) {
		if (!udah("kelompok_tugas") || refresh) {
			Session session = HibernateUtil.currentNativeSession();
			reInitTugasKelompok(session);
			HibernateUtil.closeSession();
		}

		TreeMap<Long, TugasKelompok> tugasKelompoksa = new TreeMap<Long, TugasKelompok>();
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiTugasKelompok());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						GeneralValueObject generalValueObject = ambilData(TugasKelompok.class, key, true);
						if (generalValueObject != null) {
							TugasKelompok tugasKelompok = (TugasKelompok) generalValueObject;
							if (tugasKelompok != null && tugasKelompok.getId() != null
									&& !tugasKelompok.getJudultugas().isEmpty()) {
								tugasKelompok.setPertemuan(this.getId());
								tugasKelompoksa.put(tugasKelompok.getId(), tugasKelompok);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:3804");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:3808");
		}
		return tugasKelompoksa;
	}

	/**
	 * Potong satu halaman dari peta tugas kelompok yang sudah dikumpulkan.
	 *
	 * @param tugasKelompoksa peta hasil {@link #ambilTugasKelompokTotal(boolean)}
	 * @param mulai           indeks awal halaman (berbasis nol)
	 * @param banyak          banyaknya entri per halaman
	 * @return daftar tugas kelompok pada halaman yang diminta
	 * @see #ambilPertemuanPunyaUjian(TreeMap, int, int)
	 */
	public List<TugasKelompok> ambilTugasKelompok(TreeMap<Long, TugasKelompok> tugasKelompoksa, int mulai, int banyak) {

		int index = 0;
		List<TugasKelompok> tugasKelompoks = new ArrayList<TugasKelompok>();
		Iterator<Long> i = tugasKelompoksa.keySet().iterator();
		while (i.hasNext()) {
			TugasKelompok tugasKelompok = tugasKelompoksa.get(i.next());

			if (index >= mulai && index < (mulai + banyak)) {
				tugasKelompoks.add(tugasKelompok);
			}
			index++;

		}
		return tugasKelompoks;
	}

	/**
	 * Baca peta lokasi kelompok parameter tambahan
	 * ({@link KelompokParameterTambahanPertemuan}) yang berlaku untuk pertemuan ini.
	 *
	 * <p>"Parameter tambahan" adalah isian dinamis per tenant: administrator mendefinisikan
	 * sendiri kolom-kolom yang harus diisi pada formulir pertemuan, dan
	 * {@link KelompokParameterTambahanPertemuan} mengelompokkannya. Berbeda dari koleksi anak lain,
	 * kelompok ini BUKAN milik pertemuan — ia master data yang berlaku bagi banyak pertemuan
	 * sekaligus; peta lokasi di sini hanya meng-cache "kelompok mana saja yang berlaku untuk
	 * pertemuan berjenis induk seperti ini".</p>
	 *
	 * <p>Nama berkas dan penanda cache-nya {@code "KelompokParameterTambahanPertemuan"} — satu-
	 * satunya di kelas ini yang memakai gaya penulisan kelas (huruf besar campur), bukan
	 * huruf kecil bergaris bawah.</p>
	 *
	 * @return isi peta lokasi sebagai teks JSON; JSON kosong bila belum ada
	 * @see #ambilLokasiPengajuanIzinTidakMasukPerkuliahan()
	 * @see #getParameterTambahan()
	 */
	public String ambilLokasiKelompokParameterTambahanPertemuan() {
		File file = Common.getFileLocation(this, "KelompokParameterTambahanPertemuan_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3836");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Tulis ulang seluruh isi peta lokasi kelompok parameter tambahan milik pertemuan ini.
	 *
	 * @param data isi peta lokasi baru sebagai teks JSON
	 * @see #ambilLokasiKelompokParameterTambahanPertemuan()
	 */
	public void tulisLokasiKelompokParameterTambahanPertemuan(String data) {
		File file = Common.getFileLocation(this, "KelompokParameterTambahanPertemuan_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3845");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Hapus berkas peta lokasi kelompok parameter tambahan milik pertemuan ini.
	 *
	 * @see #bersihkanLokasiPengajuanIzinTidakMasukPerkuliahan()
	 */
	public void bersihkanLokasiKelompokParameterTambahanPertemuan() {
		File file = Common.getFileLocation(this, "KelompokParameterTambahanPertemuan_" + getId().toString());
		BacaTulisUtil.doHapus(file, "KelompokParameterTambahanPertemuan");

	}

	/**
	 * Bangun ulang peta lokasi kelompok parameter tambahan yang berlaku bagi pertemuan ini.
	 *
	 * <p>Ini {@code reInitXxx(...)} yang paling berbeda dari lainnya karena TIDAK mencari anak
	 * milik pertemuan ini, melainkan mencari <b>master data yang cocok dengan JENIS INDUK</b>
	 * pertemuan ini:</p>
	 * <ol>
	 *   <li>Sebuah {@link Criterion} dipilih berdasarkan induk mana yang terisi. Setiap jenis induk
	 *       punya kolom penanda boolean sendiri di {@code ParameterTambahanPertemuan} (mis.
	 *       {@code perkuliahan}, {@code jadwalPelajaran}, {@code skripsi}), dan penyaringnya
	 *       menjadi {@code eq(namaKolom, true)}.</li>
	 *   <li>Bila TIDAK ada induk yang dikenali, penyaringnya adalah
	 *       {@code Restrictions.sqlRestriction("false")} — sengaja tidak pernah cocok, sehingga
	 *       hasilnya kosong alih-alih mengambil semua. Nilai awal inilah yang membuat method aman
	 *       untuk jenis induk yang belum didukung.</li>
	 *   <li>Query menggabungkan {@code parameterTambahan} dan
	 *       {@code kelompokParameterTambahanPertemuan}, menyaring keduanya harus {@code aktif},
	 *       lalu mengelompokkan menurut kelompoknya sehingga yang keluar adalah daftar kelompok
	 *       unik.</li>
	 *   <li>Tiap kelompok dimasukkan ke cache dan didaftarkan ke peta lewat
	 *       {@link #populateKelompokParameterTambahanPertemuan(KelompokParameterTambahanPertemuan,
	 *       boolean)}.</li>
	 * </ol>
	 *
	 * <p><b>Perhatikan:</b> {@code kelasLesSiswa}, {@code jadwalUjianPegawai},
	 * {@code komponenDataProdukKursus}, dan {@code wisuda} tidak punya cabang di sini, sehingga
	 * pertemuan dengan induk itu tidak pernah mendapat parameter tambahan apa pun.</p>
	 *
	 * <p>Karena hasilnya bergantung pada JENIS induk (bukan pertemuan tertentu), peta yang sama
	 * praktis berisi hal yang sama untuk semua pertemuan sejenis — namun tetap disimpan per
	 * pertemuan.</p>
	 *
	 * @param session session Hibernate yang sudah terbuka; tidak ditutup oleh method ini
	 * @see #ambilKelompokParameterTambahanPertemuanTotal()
	 */
	@SuppressWarnings("unchecked")
	public void reInitKelompokParameterTambahanPertemuan(Session session) {
		Pertemuan pertemuan = this;
		Criterion criterion = Restrictions.sqlRestriction("false");

		if (pertemuan.getPerkuliahan() != null) {
			criterion = Restrictions.eq("perkuliahan", true);
		} else if (pertemuan.getJadwalUjianPMB() != null) {
			criterion = Restrictions.eq("jadwalUjianPMB", true);
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			criterion = Restrictions.eq("mahasiswaRequestTugasAkhir", true);
		} else if (pertemuan.getKelompokKkn() != null) {
			criterion = Restrictions.eq("kelompokKkn", true);
		} else if (pertemuan.getKelompokPkl() != null) {
			criterion = Restrictions.eq("kelompokPkl", true);
		} else if (pertemuan.getSkripsi() != null) {
			criterion = Restrictions.eq("skripsi", true);
		} else if (pertemuan.getKrsMahasiswa() != null) {
			criterion = Restrictions.eq("krsMahasiswa", true);
		} else if (pertemuan.getJadwalUjianPSB() != null) {
			criterion = Restrictions.eq("jadwalUjianPSB", true);
		} else if (pertemuan.getJadwalPertemuanPSB() != null) {
			criterion = Restrictions.eq("jadwalPertemuanPSB", true);
		} else if (pertemuan.getJadwalPelajaran() != null) {
			criterion = Restrictions.eq("jadwalPelajaran", true);
		} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null) {
			criterion = Restrictions.eq("pertemuanPunyaGrupPertemuan", true);
		} else if (pertemuan.getFormulirKegiatan() != null) {
			criterion = Restrictions.eq("formulirKegiatan", true);
		}
		List<KelompokParameterTambahanPertemuan> kelompokParameterTambahanPertemuans = session
				.createCriteria(ParameterTambahanPertemuan.class).add(criterion)
				.createAlias("parameterTambahan", "parameterTambahan")
				.createAlias("kelompokParameterTambahanPertemuan", "kelompokParameterTambahanPertemuan")
				.add(Restrictions.eq("parameterTambahan.aktif", true))
				.add(Restrictions.eq("kelompokParameterTambahanPertemuan.aktif", true))
				.setProjection(Projections.groupProperty("kelompokParameterTambahanPertemuan")).list();

		bersihkanLokasiKelompokParameterTambahanPertemuan();
		tulisLokasiKelompokParameterTambahanPertemuan(new JSONObject().toString());
		for (KelompokParameterTambahanPertemuan kelompokParameterTambahanPertemuan : kelompokParameterTambahanPertemuans) {
			masukkanData(KelompokParameterTambahanPertemuan.class, kelompokParameterTambahanPertemuan);
			populateKelompokParameterTambahanPertemuan(kelompokParameterTambahanPertemuan, true);
		}
		kelompokParameterTambahanPertemuans = null;
	}

	/**
	 * Keluarkan satu kelompok parameter tambahan dari peta lokasi pertemuan ini.
	 *
	 * @param id id kelompok yang dikeluarkan
	 * @see #removePengajuanIzinTidakMasukPerkuliahan(Serializable)
	 */
	public void removeKelompokParameterTambahanPertemuan(Serializable id) {
		try {
			JSONObject c = jsonObjekUntukTulis(ambilLokasiKelompokParameterTambahanPertemuan());
			c.put(id.toString(), "");
			tulisLokasiKelompokParameterTambahanPertemuan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3909");

		}
	}

	/**
	 * Daftarkan satu kelompok parameter tambahan ke peta lokasi pertemuan ini.
	 *
	 * <p>Seperti kelompok lampiran/video/audio, nilai yang disimpan adalah PATH berkas hasil
	 * {@code write()}, sehingga isinya dapat dibaca kembali walau cache objek sudah kosong.</p>
	 *
	 * <p>Parameter {@code tulisUlang} sama sekali tidak berpengaruh: kedua cabang ekspresi
	 * kondisionalnya memanggil {@code write().getAbsolutePath()} yang sama persis.</p>
	 *
	 * @param kelompokParameterTambahanPertemuan kelompok yang didaftarkan; {@code null} diabaikan
	 * @param tulisUlang                         tidak berpengaruh
	 * @see #populatePertemuanFileContent(PertemuanFileContent, boolean)
	 */
	public void populateKelompokParameterTambahanPertemuan(
			KelompokParameterTambahanPertemuan kelompokParameterTambahanPertemuan, boolean tulisUlang) {
		try {
			if (kelompokParameterTambahanPertemuan == null) {
				return;
			}
			JSONObject c = jsonObjekUntukTulis(ambilLokasiKelompokParameterTambahanPertemuan());
			c.put(kelompokParameterTambahanPertemuan.getId().toString(),
					tulisUlang ? kelompokParameterTambahanPertemuan.write().getAbsolutePath()
							: kelompokParameterTambahanPertemuan.write().getAbsolutePath());
			tulisLokasiKelompokParameterTambahanPertemuan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:3925");
		}
	}

	/**
	 * Banyaknya kelompok parameter tambahan yang berlaku bagi pertemuan ini.
	 *
	 * <p>Berbeda dari {@link #ambilJumlahPertemuanFileContent()} yang menghitung entri peta begitu
	 * saja, method ini MEMUAT tiap kelompok (dari cache, atau dari berkas cache sebagai cadangan)
	 * dan hanya menghitung yang namanya tidak kosong. Jadi biayanya setara dengan
	 * {@link #ambilKelompokParameterTambahanPertemuanTotal()}, bukan lebih murah.</p>
	 *
	 * @return banyaknya kelompok parameter tambahan yang berlaku
	 */
	@SuppressWarnings("unchecked")
	public int ambilJumlahKelompokParameterTambahanPertemuan() {
		if (!udah("KelompokParameterTambahanPertemuan")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitKelompokParameterTambahanPertemuan(session);
			HibernateUtil.closeSession();
		}
		int jumlah = 0;
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiKelompokParameterTambahanPertemuan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(KelompokParameterTambahanPertemuan.class,
								key);
						if (generalValueObject != null) {
							KelompokParameterTambahanPertemuan kelompokParameterTambahanPertemuan = (KelompokParameterTambahanPertemuan) generalValueObject;
							if (kelompokParameterTambahanPertemuan != null
									&& !kelompokParameterTambahanPertemuan.getNama().isEmpty()) {
								jumlah++;
							}
						} else {

							File file = new File(s);
							if (file != null && file.exists()) {
								JSONObject isiBerkas = bacaJsonObjekAman(ais.common.BacaTulisUtil.baca(file));
								if (isiBerkas == null) {
									// Berkas ADA tetapi isinya bukan JSON objek (kosong/terpotong/format lama).
									// Dilewati -- hasil akhirnya sama dengan perilaku lama yang melewatinya
									// lewat exception, tanpa membanjiri log pada tiap siklus warmup cache.
									continue;
								}
								KelompokParameterTambahanPertemuan kelompokParameterTambahanPertemuan = (KelompokParameterTambahanPertemuan) Common
										.convertToObject(isiBerkas, KelompokParameterTambahanPertemuan.class);
								if (kelompokParameterTambahanPertemuan != null
										&& !kelompokParameterTambahanPertemuan.getNama().isEmpty()) {
									jumlah++;
								}
							}
						}

					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:3970");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:3974");
		}
		return jumlah;
	}

	/**
	 * Seluruh kelompok parameter tambahan yang berlaku bagi pertemuan ini.
	 *
	 * <p>Membaca peta lokasi dengan dua sumber seperti kelompok lampiran: cache lebih dulu, lalu
	 * berkas cache sebagai cadangan. Kelompok yang namanya kosong disaring keluar.</p>
	 *
	 * <p>Inilah daftar yang dipakai UI untuk membentuk baris-baris isian dinamis, yang kemudian
	 * dikembalikan ke model lewat {@link #populateParameterTambahan(java.util.List)}.</p>
	 *
	 * @return peta {@code id -> kelompok parameter tambahan}, terurut menaik menurut id
	 * @see #populateParameterTambahan(java.util.List)
	 * @see #ambilDataParameterTambahan()
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<Long, KelompokParameterTambahanPertemuan> ambilKelompokParameterTambahanPertemuanTotal() {
		if (!udah("KelompokParameterTambahanPertemuan")) {
			Session session = HibernateUtil.currentNativeSession();
			reInitKelompokParameterTambahanPertemuan(session);
			HibernateUtil.closeSession();
		}
		TreeMap<Long, KelompokParameterTambahanPertemuan> kelompokParameterTambahanPertemuansa = new TreeMap<Long, KelompokParameterTambahanPertemuan>();
		try {
			JSONObject c = jsonObjekAtauKosong(ambilLokasiKelompokParameterTambahanPertemuan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(KelompokParameterTambahanPertemuan.class,
								key);
						if (generalValueObject != null) {
							KelompokParameterTambahanPertemuan kelompokParameterTambahanPertemuan = (KelompokParameterTambahanPertemuan) generalValueObject;
							if (kelompokParameterTambahanPertemuan != null
									&& !kelompokParameterTambahanPertemuan.getNama().isEmpty()) {
								kelompokParameterTambahanPertemuansa.put(kelompokParameterTambahanPertemuan.getId(),
										kelompokParameterTambahanPertemuan);
							}
						} else {

							File file = new File(s);
							if (file != null && file.exists()) {
								JSONObject isiBerkas = bacaJsonObjekAman(ais.common.BacaTulisUtil.baca(file));
								if (isiBerkas == null) {
									// Berkas ADA tetapi isinya bukan JSON objek (kosong/terpotong/format lama).
									// Dilewati -- hasil akhirnya sama dengan perilaku lama yang melewatinya
									// lewat exception, tanpa membanjiri log pada tiap siklus warmup cache.
									continue;
								}
								KelompokParameterTambahanPertemuan kelompokParameterTambahanPertemuan = (KelompokParameterTambahanPertemuan) Common
										.convertToObject(isiBerkas, KelompokParameterTambahanPertemuan.class);
								if (kelompokParameterTambahanPertemuan != null
										&& !kelompokParameterTambahanPertemuan.getNama().isEmpty()) {
									kelompokParameterTambahanPertemuansa.put(kelompokParameterTambahanPertemuan.getId(),
											kelompokParameterTambahanPertemuan);
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4021");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4025");
		}
		return kelompokParameterTambahanPertemuansa;
	}

	/**
	 * Apakah kolom komentar/diskusi pertemuan ini sudah ditutup?
	 *
	 * @return {@code true} bila komentar ditutup; {@code false} bila belum pernah diisi
	 * @see #punyaDiskusi()
	 */
	public Boolean getKomentarDitutup() {
		return komentarDitutup == null ? false : komentarDitutup;
	}

	/**
	 * Setel penutupan kolom komentar/diskusi pertemuan ini.
	 *
	 * @param komentarDitutup {@code true} untuk menutup komentar
	 */
	public void setKomentarDitutup(Boolean komentarDitutup) {
		this.komentarDitutup = komentarDitutup;
	}

	/**
	 * Bolehkah peserta melampirkan berkas ketika menulis komentar/diskusi?
	 *
	 * @return {@code true} bila lampiran diizinkan; {@code false} bila belum pernah diisi
	 * @see #getIzinkanUploadLampiranDiGrive()
	 */
	public Boolean getIzinkanUploadLampiranDiKomentar() {
		return izinkanUploadLampiranDiKomentar == null ? false : izinkanUploadLampiranDiKomentar;
	}

	/**
	 * Setel izin melampirkan berkas pada komentar/diskusi.
	 *
	 * @param izinkanUploadLampiranDiKomentar {@code true} bila lampiran diizinkan
	 */
	public void setIzinkanUploadLampiranDiKomentar(Boolean izinkanUploadLampiranDiKomentar) {
		this.izinkanUploadLampiranDiKomentar = izinkanUploadLampiranDiKomentar;
	}

	/**
	 * Formulir kegiatan yang menjadi induk pertemuan ini.
	 *
	 * <p>Dipakai untuk kegiatan berbasis pendaftaran (seminar, pelatihan, lomba) yang pesertanya
	 * datang dari {@code FormulirKegiatanPeserta}, bukan dari kelas kuliah — lihat cabang
	 * bersangkutan pada {@link #ambilMahasiswa()}.</p>
	 *
	 * @return {@link FormulirKegiatan} induk, atau {@code null}
	 * @see #untuk()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "formulir_kegiatan", nullable = true)
	public FormulirKegiatan getFormulirKegiatan() {
		formulirKegiatan = check(formulirKegiatan);
		return formulirKegiatan;
	}

	/**
	 * Tetapkan formulir kegiatan sebagai induk pertemuan ini.
	 *
	 * @param formulirKegiatan formulir kegiatan; boleh {@code null}
	 */
	public void setFormulirKegiatan(FormulirKegiatan formulirKegiatan) {
		this.formulirKegiatan = formulirKegiatan;
	}

	/**
	 * Bentuk komponen ZK berisi ringkasan mata kuliah/pelajaran pertemuan ini ke dalam
	 * {@code vbox}.
	 *
	 * <p><b>Method UI di dalam kelas entity</b> — tidak lazim, tetapi memang begitu adanya di
	 * codebase ini. Yang ditambahkan ke {@code vbox}, menurut jenis induk:</p>
	 * <ul>
	 *   <li>selalu: satu {@link Label} berisi {@link #getTopik()};</li>
	 *   <li><b>perkuliahan</b>: daftar dosen pengampu, kotak revisi ({@code RevisiHelper}) berisi
	 *       kode/nama/SKS mata kuliah dan tahun kurikulum, daftar mata kuliah prasyarat, lalu
	 *       hari/jam/ruang perkuliahan;</li>
	 *   <li><b>jadwal pelajaran</b>: daftar guru, kotak revisi berisi kode/nama mata pelajaran,
	 *       lalu hari/jam/ruang;</li>
	 *   <li>jenis induk lain: tidak ada tambahan apa pun selain label topik.</li>
	 * </ul>
	 *
	 * <p>Hanya aman dipanggil dari thread yang memegang desktop ZK. Berbeda dari kebanyakan method
	 * lain di kelas ini, exception TIDAK ditelan melainkan dilempar ke pemanggil.</p>
	 *
	 * <p><b>Catatan:</b> variabel lokal {@code pertemuan} di dalamnya diisi {@code this} lalu
	 * berulang kali diuji {@code != null} — pemeriksaan yang tidak pernah bisa gagal.</p>
	 *
	 * @param vbox wadah ZK tempat komponen ditambahkan
	 * @throws Exception bila penyusunan komponen atau pembacaan data gagal
	 * @see #populateParameterTambahan(java.util.List)
	 */
	public void tampilMk(Box vbox) throws Exception {
		Pertemuan pertemuan = this;
		new Label(pertemuan == null ? "" : pertemuan.getTopik()).setParent(vbox);
		if (pertemuan != null && pertemuan.getPerkuliahan() != null) {
			Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(vbox, perkuliahan, true);
			Kurikulum kurikulum = perkuliahan.getKurikulum();
			Vbox a = RevisiHelper.createNewRevisi(Perkuliahan.class, perkuliahan,
					perkuliahan.getMatakuliah().getKode() + "-" + perkuliahan.getMatakuliah().getNama() + " "
							+ perkuliahan.getMatakuliah().getSks() + " sks "
							+ (kurikulum == null ? "" : " (Kurikulum:" + kurikulum.getTahun() + ")"));
			a.setParent(vbox);

			MatakuliahPrasyaratAction.tampilPrasyarat(a, perkuliahan.getMatakuliah());
			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(vbox, perkuliahan);
		} else if (pertemuan != null && pertemuan.getJadwalPelajaran() != null) {
			JadwalPelajaran jadwalPelajaran = pertemuan.getJadwalPelajaran();
			Common.displayGuruJadwalPelajaran(vbox, jadwalPelajaran, true);

			Vbox a = RevisiHelper.createNewRevisi(JadwalPelajaran.class, jadwalPelajaran,
					jadwalPelajaran.getMatapelajaran().getKode() + "-" + jadwalPelajaran.getMatapelajaran().getNama());
			a.setParent(vbox);

			Common.displayHariJamRuanganJadwalPelajaranUmum(vbox, jadwalPelajaran);
		}
	}

	/**
	 * Data mentah acara kalender (Google Calendar) yang tertaut ke pertemuan ini, sebagai teks
	 * JSON.
	 *
	 * <p>Nilai kosong dikembalikan sebagai JSON objek kosong ({@code "{}"}) sehingga pemanggil
	 * selalu aman mem-parsingnya. Isi objek ini dibaca lewat {@code retreive(kunci)} dari
	 * {@link GeneralValueObject} — {@link #getOnlineMenggunakan()} dan {@link #getMeetLink()}
	 * memakainya untuk mengambil kunci {@code "hangoutLink"}.</p>
	 *
	 * @return teks JSON acara kalender; {@code "{}"} bila belum ada
	 * @see #getMeetLink()
	 */
	@Column(columnDefinition = "text")
	public String getCalendarEvent() {
		return calendarEvent == null || calendarEvent.trim().isEmpty() ? new JSONObject().toString() : calendarEvent;
	}

	/**
	 * Setel data mentah acara kalender pertemuan ini.
	 *
	 * @param calendarEvent teks JSON acara kalender
	 */
	public void setCalendarEvent(String calendarEvent) {
		this.calendarEvent = calendarEvent;
	}

	/**
	 * Nilai {@link #getOnlineMenggunakan()}: pertemuan daring tidak diaktifkan.
	 *
	 * <p>Perhatikan bahwa seluruh konstanta di kelompok ini bertipe {@code public static Integer}
	 * (bukan {@code static final int}), sehingga nilainya DAPAT diubah dari mana saja dan
	 * membandingkannya dengan {@code ==} tidak dijamin benar untuk nilai di luar rentang cache
	 * {@link Integer}. Bandingkan dengan {@code equals(...)}.</p>
	 */
	public static Integer TIDAK_AKTIF = 0;
	/** Nilai {@link #getOnlineMenggunakan()}: memakai Jitsi Meet — lihat {@link #generateJitsiLink()}. */
	public static Integer JITSI = 1;
	/** Nilai {@link #getOnlineMenggunakan()}: memakai Google Meet — lihat {@link #getMeetLink()}. */
	public static Integer GOOGLE_MEET = 2;
	/** Nilai {@link #getOnlineMenggunakan()}: memakai Zoom — lihat {@link #getZoomLink()}. */
	public static Integer ZOOM = 3;
	/** Nilai {@link #getOnlineMenggunakan()}: memakai BigBlueButton — lihat {@link #getBbbLink()}. */
	public static Integer BBB = 4;
	/** Nilai {@link #getOnlineMenggunakan()}: memakai Skype — lihat {@link #getSkypeLink()}. */
	public static Integer SKYPE = 5;
	/** Nilai {@link #getOnlineMenggunakan()}: memakai WhatsApp — lihat {@link #getWaLink()}. */
	public static Integer WA = 6;
	/** Nilai {@link #getOnlineMenggunakan()}: memakai layanan lain — lihat {@link #getLainLink()}. */
	public static Integer LAIN = 7;

	/**
	 * Layanan pertemuan daring yang dipakai pertemuan ini.
	 *
	 * <p>Nilainya salah satu konstanta {@link #TIDAK_AKTIF}, {@link #JITSI}, {@link #GOOGLE_MEET},
	 * {@link #ZOOM}, {@link #BBB}, {@link #SKYPE}, {@link #WA}, atau {@link #LAIN}.</p>
	 *
	 * <p><b>Getter ini mengubah keadaan objek.</b> Bila belum pernah diisi, layanan ditebak dari
	 * data kalender: adanya {@code "hangoutLink"} pada {@link #getCalendarEvent()} berarti
	 * {@link #GOOGLE_MEET}, selain itu {@link #JITSI}. Hasil tebakan itu ditulis ke field sehingga
	 * ikut tersimpan. Perhatikan bahwa nilai bawaannya {@link #JITSI}, BUKAN
	 * {@link #TIDAK_AKTIF} — jadi pertemuan yang tidak pernah dimaksudkan daring pun akan
	 * melaporkan memakai Jitsi.</p>
	 *
	 * @return kode layanan daring; tidak pernah {@code null}
	 */
	public Integer getOnlineMenggunakan() {
		if (onlineMenggunakan == null) {
			String hangoutLink = retreive("hangoutLink");
			if (hangoutLink != null && !hangoutLink.trim().isEmpty()) {
				onlineMenggunakan = GOOGLE_MEET;
			} else {
				onlineMenggunakan = JITSI;
			}
		}
		return onlineMenggunakan;
	}

	/**
	 * Setel layanan pertemuan daring yang dipakai.
	 *
	 * @param onlineMenggunakan salah satu konstanta {@link #TIDAK_AKTIF}..{@link #LAIN}
	 * @see #getOnlineMenggunakan()
	 */
	public void setOnlineMenggunakan(Integer onlineMenggunakan) {
		this.onlineMenggunakan = onlineMenggunakan;
	}

	/**
	 * Bolehkah peserta melampirkan berkas lewat penyimpanan awan ("Grive") pada pertemuan ini?
	 *
	 * @return {@code true} bila diizinkan; {@code false} bila belum pernah diisi
	 * @see #getIzinkanUploadLampiranDiKomentar()
	 */
	public Boolean getIzinkanUploadLampiranDiGrive() {
		return izinkanUploadLampiranDiGrive == null ? false : izinkanUploadLampiranDiGrive;
	}

	/**
	 * Setel izin melampirkan berkas lewat penyimpanan awan.
	 *
	 * @param izinkanUploadLampiranDiGrive {@code true} bila diizinkan
	 */
	public void setIzinkanUploadLampiranDiGrive(Boolean izinkanUploadLampiranDiGrive) {
		this.izinkanUploadLampiranDiGrive = izinkanUploadLampiranDiGrive;
	}

	/**
	 * Tautan ruang Zoom pertemuan ini.
	 *
	 * <p><b>Pola bersama seluruh getter tautan daring</b> ({@link #getZoomLink()},
	 * {@link #getBbbLink()}, {@link #getSkypeLink()}, {@link #getWaLink()},
	 * {@link #getLainLink()}, dan sebagian {@link #getMeetLink()}): bila nilainya mengandung
	 * SPASI — pertanda pengguna menempelkan seluruh undangan rapat, bukan hanya URL-nya —
	 * {@code Common.getUrls(...)} dipakai untuk memungut URL PERTAMA dari teks itu, dan hasilnya
	 * DITULIS BALIK ke field sehingga teks undangan aslinya HILANG pada penyimpanan berikutnya.
	 * Bila tidak ada URL sama sekali di dalamnya, nilainya menjadi string kosong.</p>
	 *
	 * <p>Nilai kosong dikembalikan sebagai {@code null}, bukan string kosong.</p>
	 *
	 * <p>Di dalam kode terdapat blok yang dikomentari untuk mewarisi tautan dari
	 * {@link VOPembelajaran} induk; pewarisan itu sengaja dinonaktifkan, jadi tiap pertemuan
	 * memegang tautannya sendiri.</p>
	 *
	 * @return URL Zoom, atau {@code null} bila belum diisi
	 * @see #getOnlineMenggunakan()
	 */
	@Column(columnDefinition = "text")
	public String getZoomLink() {

		try {

//			if (zoomLink == null || zoomLink.trim().isEmpty()) {
//				VOPembelajaran pembelajaran = ambilVOPembelajaran();
//				if (pembelajaran != null) {
//					zoomLink = pembelajaran.retreive("zoomLink");
//				}
//			}

			if (zoomLink != null && !zoomLink.trim().isEmpty() && zoomLink.toLowerCase().trim().contains(" ")) {
				List<String> urls = Common.getUrls(zoomLink.trim());
				zoomLink = urls.isEmpty() ? "" : urls.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4143");
		}

		return zoomLink == null || zoomLink.trim().isEmpty() ? null : zoomLink.trim();
	}

	/**
	 * Setel tautan ruang Zoom pertemuan ini (disimpan apa adanya; pemungutan URL terjadi saat
	 * dibaca).
	 *
	 * @param zoomLink URL atau teks undangan Zoom
	 * @see #getZoomLink()
	 */
	public void setZoomLink(String zoomLink) {
//		try {
//			if (zoomLink != null && !zoomLink.trim().isEmpty()) {
//				VOPembelajaran pembelajaran = ambilVOPembelajaran();
//				if (pembelajaran != null) {
//					pembelajaran.put(zoomLink, "zoomLink");
//				}
//			}
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4157");
//			// TODO: handle exception
//		}
		this.zoomLink = zoomLink;
	}

	/**
	 * Tautan ruang Google Meet pertemuan ini.
	 *
	 * <p>Satu-satunya tautan daring yang punya SUMBER LUAR: bila
	 * {@link #getCalendarEvent()} memuat kunci {@code "hangoutLink"}, nilai itulah yang dipakai
	 * dan ditulis balik ke field — menimpa apa pun yang diisi manual. Tautan Meet karenanya
	 * mengikuti acara Google Calendar yang tertaut, bukan sebaliknya.</p>
	 *
	 * <p>Bila tidak ada tautan dari kalender, berlaku pola pemungutan URL yang sama seperti
	 * {@link #getZoomLink()}. Sebagai tambahan, nilai yang diawali {@code "meet.google.com"} tanpa
	 * skema otomatis diberi awalan {@code "https://"}.</p>
	 *
	 * @return URL Google Meet, atau {@code null} bila belum diisi
	 * @see #getOnlineMenggunakan()
	 */
	@Column(columnDefinition = "text")
	public String getMeetLink() {
		String linkcalendar = retreive("hangoutLink");
		if (linkcalendar != null && !linkcalendar.trim().isEmpty()) {
			meetLink = linkcalendar.trim();
		} else {

//			if (meetLink == null || meetLink.trim().isEmpty()) {
//				VOPembelajaran pembelajaran = ambilVOPembelajaran();
//				if (pembelajaran != null) {
//					meetLink = pembelajaran.retreive("meetLink");
//				}
//			}

			try {
				if (meetLink != null && !meetLink.trim().isEmpty() && meetLink.toLowerCase().trim().contains(" ")) {
					List<String> urls = Common.getUrls(meetLink.trim());
					meetLink = urls.isEmpty() ? "" : urls.get(0);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4183");
			}
		}

		if (meetLink != null && meetLink.toLowerCase().startsWith("meet.google.com")) {
			meetLink = "https://" + meetLink;
		}

		return meetLink == null || meetLink.trim().isEmpty() ? null : meetLink.trim();
	}

	/**
	 * Setel tautan Google Meet pertemuan ini.
	 *
	 * <p>Ingat bahwa {@link #getMeetLink()} akan MENIMPA nilai ini bila acara kalender yang
	 * tertaut memuat {@code "hangoutLink"}.</p>
	 *
	 * @param meetLink URL atau teks undangan Google Meet
	 * @see #getMeetLink()
	 */
	public void setMeetLink(String meetLink) {
//		try {
//			if (meetLink != null && !meetLink.trim().isEmpty()) {
//				VOPembelajaran pembelajaran = ambilVOPembelajaran();
//				if (pembelajaran != null) {
//					pembelajaran.put(meetLink, "meetLink");
//				}
//			}
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4202");
//			// TODO: handle exception
//		}
		this.meetLink = meetLink;
	}

	/**
	 * Tautan ruang BigBlueButton pertemuan ini.
	 *
	 * <p>Mengikuti pola pemungutan URL yang dijelaskan pada {@link #getZoomLink()}.</p>
	 *
	 * @return URL BigBlueButton, atau {@code null} bila belum diisi
	 * @see #getOnlineMenggunakan()
	 */
	@Column(columnDefinition = "text")
	public String getBbbLink() {

		try {

//			if (bbbLink == null || bbbLink.trim().isEmpty()) {
//				VOPembelajaran pembelajaran = ambilVOPembelajaran();
//				if (pembelajaran != null) {
//					bbbLink = pembelajaran.retreive("bbbLink");
//				}
//			}

			if (bbbLink != null && !bbbLink.trim().isEmpty() && bbbLink.toLowerCase().trim().contains(" ")) {
				List<String> urls = Common.getUrls(bbbLink.trim());
				bbbLink = urls.isEmpty() ? "" : urls.get(0);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4224");
//			e.printStackTrace();
		}

		return bbbLink == null || bbbLink.trim().isEmpty() ? null : bbbLink.trim();
	}

	/**
	 * Setel tautan ruang BigBlueButton pertemuan ini.
	 *
	 * @param bbbLink URL atau teks undangan BigBlueButton
	 * @see #getBbbLink()
	 */
	public void setBbbLink(String bbbLink) {
//		try {
//			if (bbbLink != null && !bbbLink.trim().isEmpty()) {
//				VOPembelajaran pembelajaran = ambilVOPembelajaran();
//				if (pembelajaran != null) {
//					pembelajaran.put(bbbLink, "bbbLink");
//				}
//			}
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4239");
//			// TODO: handle exception
//		}
		this.bbbLink = bbbLink;
	}

	/**
	 * Tautan ruang Skype pertemuan ini.
	 *
	 * <p>Mengikuti pola pemungutan URL yang dijelaskan pada {@link #getZoomLink()}.</p>
	 *
	 * @return URL Skype, atau {@code null} bila belum diisi
	 * @see #getOnlineMenggunakan()
	 */
	@Column(columnDefinition = "text")
	public String getSkypeLink() {

		try {
//			if (skypeLink == null || skypeLink.trim().isEmpty()) {
//				VOPembelajaran pembelajaran = ambilVOPembelajaran();
//				if (pembelajaran != null) {
//					skypeLink = pembelajaran.retreive("skypeLink");
//				}
//			}

			if (skypeLink != null && !skypeLink.trim().isEmpty() && skypeLink.toLowerCase().trim().contains(" ")) {
				List<String> urls = Common.getUrls(skypeLink.trim());
				skypeLink = urls.isEmpty() ? "" : urls.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4261");
		}

		return skypeLink == null || skypeLink.trim().isEmpty() ? null : skypeLink.trim();
	}

	/**
	 * Setel tautan ruang Skype pertemuan ini.
	 *
	 * @param skypeLink URL atau teks undangan Skype
	 * @see #getSkypeLink()
	 */
	public void setSkypeLink(String skypeLink) {
//		try {
//			if (skypeLink != null && !skypeLink.trim().isEmpty()) {
//				VOPembelajaran pembelajaran = ambilVOPembelajaran();
//				if (pembelajaran != null) {
//					pembelajaran.put(skypeLink, "skypeLink");
//				}
//			}
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4275");
//			// TODO: handle exception
//		}
		this.skypeLink = skypeLink;
	}

	/**
	 * Induk pertemuan ini sebagai {@link VOPembelajaran} — abstraksi bersama seluruh jenis induk.
	 *
	 * <p><b>Ini titik pusat kedua sifat polimorfik {@link Pertemuan}</b>, berpasangan dengan
	 * {@link #untuk()}. Bila {@link #untuk()} menjawab "induknya jenis apa" dalam bentuk teks,
	 * method ini menyerahkan OBJEK induknya lewat antarmuka bersama, sehingga pemanggil dapat
	 * menanyakan hal-hal umum seperti {@link VOPembelajaran#getUrutkanotomatis()} tanpa peduli
	 * jenis induknya.</p>
	 *
	 * <p>Dipakai antara lain oleh {@link #getPertemuanKe()}, {@link #getPertemuanManual()},
	 * {@link #bolehUbahAbsen(Tbmuser)}, dan {@link #bolehUbahAbsenSaja(Tbmuser)}.</p>
	 *
	 * <p><b>Perhatikan pola pemeriksaannya:</b> setiap cabang menguji FIELD secara langsung
	 * ({@code if (perkuliahan != null)}) tetapi mengambil nilainya lewat GETTER
	 * ({@code pembelajaran = getPerkuliahan()}). Artinya proxy yang belum terinisialisasi dapat
	 * membuat cabangnya terlewat, walaupun begitu cabang yang terpilih selalu menghasilkan objek
	 * yang sudah tersegarkan.</p>
	 *
	 * <p>Cakupannya paling lengkap di kelas ini — enam belas jenis induk, termasuk
	 * {@code kelasLesSiswa}, {@code wisuda}, dan {@code jadwalPertemuanPSB} yang justru tidak punya
	 * cabang di {@link #untuk()}.</p>
	 *
	 * @return induk sebagai {@link VOPembelajaran}, atau {@code null} bila tidak ada yang terisi
	 * @see #untuk()
	 */
	public VOPembelajaran ambilVOPembelajaran() {
		VOPembelajaran pembelajaran = null;
		if (perkuliahan != null) {
			pembelajaran = getPerkuliahan();
		} else if (kelompokKkn != null) {
			pembelajaran = getKelompokKkn();
		} else if (kelompokPkl != null) {
			pembelajaran = getKelompokPkl();
		} else if (mahasiswaRequestTugasAkhir != null) {
			pembelajaran = getMahasiswaRequestTugasAkhir();
		} else if (skripsi != null) {
			pembelajaran = getSkripsi();
		} else if (krsMahasiswa != null) {
			pembelajaran = getKrsMahasiswa();
		} else if (jadwalPelajaran != null) {
			pembelajaran = getJadwalPelajaran();
		} else if (kelasLesSiswa != null) {
			pembelajaran = getKelasLesSiswa();
		} else if (formulirKegiatan != null) {
			pembelajaran = getFormulirKegiatan();
		} else if (wisuda != null) {
			pembelajaran = getWisuda();
		} else if (jadwalUjianPMB != null) {
			pembelajaran = getJadwalUjianPMB();
		} else if (jadwalUjianPSB != null) {
			pembelajaran = getJadwalUjianPSB();
		} else if (jadwalPertemuanPSB != null) {
			pembelajaran = getJadwalPertemuanPSB();
		} else if (jadwalUjianPegawai != null) {
			pembelajaran = getJadwalUjianPegawai();
		} else if (pertemuanPunyaGrupPertemuan != null) {
			pembelajaran = getPertemuanPunyaGrupPertemuan();
		}

		return pembelajaran;
	}

	/**
	 * Bentuk tautan ruang Jitsi untuk pertemuan ini, mengambil sendiri permintaan HTTP yang aktif.
	 *
	 * <p>Mencoba mengambil {@link HttpServletRequest} dari eksekusi ZK yang sedang berjalan; bila
	 * tidak ada (mis. dipanggil dari thread latar), diambil dari {@code RequestContext}. Hasilnya
	 * diteruskan ke {@link #generateJitsiLink(HttpServletRequest)}.</p>
	 *
	 * @return URL ruang Jitsi
	 * @throws Exception bila permintaan tidak tersedia atau penyusunan tautan gagal
	 * @see #generateJitsiLink(HttpServletRequest)
	 */
	public String generateJitsiLink() throws Exception {
		HttpServletRequest request = null;
		if (ExecutionsCtrl.getCurrent() != null) {
			request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		}

		if (request == null) {
			request = RequestContext.get();
		}
		return generateJitsiLink(request);
	}

	/**
	 * Bentuk tautan ruang Jitsi untuk pertemuan ini.
	 *
	 * <p>Berbeda dari tautan daring lain yang disimpan sebagai kolom, tautan Jitsi selalu DIHITUNG
	 * — tidak ada kolom {@code jitsiLink}. Nama ruangnya dibentuk agar stabil dan dapat ditebak
	 * ulang, sehingga semua peserta pertemuan yang sama selalu masuk ke ruang yang sama.</p>
	 *
	 * <h4>Cara nama ruang disusun</h4>
	 * <ol>
	 *   <li>Sebuah penanda dipilih menurut jenis induk. Untuk {@link Perkuliahan} dipakai
	 *       <b>nama mata kuliah + id perkuliahan</b>; untuk jenis lain dipakai satu huruf awalan
	 *       ({@code C_} jadwal pelajaran, {@code D_} ujian PMB, {@code E_} bimbingan, {@code F_}
	 *       KKN, {@code G_} PKL, {@code H_} skripsi, {@code I_} ujian PSB, {@code J_} grup
	 *       pertemuan, {@code K_} KRS) diikuti id induknya. Cadangannya {@code "A_" + id
	 *       pertemuan}.</li>
	 *   <li>Untuk induk selain perkuliahan, nama konteks aplikasi ditempelkan di depan agar dua
	 *       tenant berbeda tidak bertabrakan di server Jitsi bersama. <b>Perkuliahan sengaja
	 *       TIDAK diberi awalan itu</b>, sehingga dua tenant yang punya mata kuliah bernama sama
	 *       dengan id perkuliahan sama akan berbagi ruang yang sama.</li>
	 *   <li>Seluruh karakter selain huruf dan angka diganti garis bawah, dijadikan huruf kecil,
	 *       dipecah per spasi lalu disambung dengan garis bawah, dan garis bawah ganda diringkas
	 *       (tiga kali berturut-turut — cukup untuk kasus nyata, tetapi tidak menjamin habis untuk
	 *       deretan garis bawah yang sangat panjang).</li>
	 *   <li>Nama itu ditempelkan ke alamat server dari konfigurasi
	 *       {@code alamat_server_video_conference} (bawaan {@code https://meet.jit.si}).</li>
	 * </ol>
	 *
	 * <p><b>Kejanggalan yang dicatat:</b> cabang {@code jadwalUjianPSB} muncul DUA KALI
	 * ({@code I_} dan {@code L_}); cabang kedua tidak pernah tercapai karena cabang pertama sudah
	 * menangkapnya lebih dulu. Selain itu {@code jadwalPertemuanPSB}, {@code jadwalUjianPegawai},
	 * {@code formulirKegiatan}, {@code kelasLesSiswa}, dan {@code wisuda} tidak punya cabang,
	 * sehingga semuanya jatuh ke cadangan {@code "A_" + id pertemuan} — yang justru menghasilkan
	 * ruang unik per pertemuan, bukan per rangkaian.</p>
	 *
	 * @param request permintaan HTTP aktif, dipakai mengambil nama konteks aplikasi
	 * @return URL lengkap ruang Jitsi
	 * @throws Exception bila penyandian nama konteks gagal
	 * @see #getOnlineMenggunakan()
	 */
	public String generateJitsiLink(HttpServletRequest request) throws Exception {
		Pertemuan pertemuan = this;
		String id = "A_" + pertemuan.getId();
		if (pertemuan.getPerkuliahan() != null) {
			id = pertemuan.getPerkuliahan().getMatakuliah().getNama() + " " + pertemuan.getPerkuliahan().getId();
		} else if (pertemuan.getJadwalPelajaran() != null) {
			id = "C_" + pertemuan.getJadwalPelajaran().getId();
		} else if (pertemuan.getJadwalUjianPMB() != null) {
			id = "D_" + pertemuan.getJadwalUjianPMB().getId();
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			id = "E_" + pertemuan.getMahasiswaRequestTugasAkhir().getId();
		} else if (pertemuan.getKelompokKkn() != null) {
			id = "F_" + pertemuan.getKelompokKkn().getId();
		} else if (pertemuan.getKelompokPkl() != null) {
			id = "G_" + pertemuan.getKelompokPkl().getId();
		} else if (pertemuan.getSkripsi() != null) {
			id = "H_" + pertemuan.getSkripsi().getId();
		} else if (pertemuan.getJadwalUjianPSB() != null) {
			id = "I_" + pertemuan.getJadwalUjianPSB().getId();
		} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null) {
			id = "J_" + pertemuan.getPertemuanPunyaGrupPertemuan().getGrupPertemuan().getId();
		} else if (pertemuan.getKrsMahasiswa() != null) {
			id = "K_" + pertemuan.getKrsMahasiswa().getId();
		} else if (pertemuan.getJadwalUjianPSB() != null) {
			id = "L_" + pertemuan.getJadwalUjianPSB().getId();
		}

		String kodeStream = (pertemuan.getPerkuliahan() != null ? ""
				: URLEncoder.encode(org.apache.commons.lang3.StringUtils.replace(request.getContextPath(), "/", ""),
						"UTF-8") + "_")
				+ id;
		try {
			String[] words = kodeStream.replaceAll("[^a-zA-Z0-9 ]", "_").toLowerCase().split("\\s+");
			kodeStream = "";
			for (String w : words) {
				kodeStream += kodeStream.isEmpty() ? w : "_" + w;
			}

			kodeStream = kodeStream.replaceAll("__", "_");
			kodeStream = kodeStream.replaceAll("__", "_");
			kodeStream = kodeStream.replaceAll("__", "_");

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4373");
		}
		String server = Common.getKonfigurasi("alamat_server_video_conference", "https://meet.jit.si").getNilai() + "/"
				+ kodeStream;
		return server;
	}

	/**
	 * Tautan grup/percakapan WhatsApp pertemuan ini.
	 *
	 * <p>Mengikuti pola pemungutan URL yang dijelaskan pada {@link #getZoomLink()}.</p>
	 *
	 * @return URL WhatsApp, atau {@code null} bila belum diisi
	 * @see #getOnlineMenggunakan()
	 */
	@Column(columnDefinition = "text")
	public String getWaLink() {
//		if (waLink == null || waLink.trim().isEmpty()) {
//			VOPembelajaran pembelajaran = ambilVOPembelajaran();
//			if (pembelajaran != null) {
//				waLink = pembelajaran.retreive("waLink");
//			}
//		}

		try {
			if (waLink != null && !waLink.trim().isEmpty() && waLink.toLowerCase().trim().contains(" ")) {
				List<String> urls = Common.getUrls(waLink.trim());
				waLink = urls.isEmpty() ? "" : urls.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4395");
		}

		return waLink == null || waLink.trim().isEmpty() ? null : waLink.trim();
	}

	/**
	 * Setel tautan WhatsApp pertemuan ini.
	 *
	 * @param waLink URL atau teks undangan WhatsApp
	 * @see #getWaLink()
	 */
	public void setWaLink(String waLink) {
//		try {
//			if (waLink != null && !waLink.trim().isEmpty()) {
//				VOPembelajaran pembelajaran = ambilVOPembelajaran();
//				if (pembelajaran != null) {
//					pembelajaran.put(waLink, "waLink");
//				}
//			}
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4409");
//			// TODO: handle exception
//		}
		this.waLink = waLink;
	}

	/**
	 * Daftar id peserta yang DIKECUALIKAN dari pertemuan ini, sebagai teks berpembatas koma.
	 *
	 * <p>Formatnya sengaja dibuat {@code ",id1,id2,id3,"} — diawali DAN diakhiri koma — supaya
	 * pemeriksaan keanggotaan cukup dilakukan dengan mencari substring {@code ",id,"} tanpa
	 * risiko id "1" ikut cocok dengan id "12".</p>
	 *
	 * <p><b>Getter ini mengubah keadaan objek</b>: setiap kali dipanggil, nilainya dirakit ulang
	 * dengan menambahkan koma pembungkus lalu meringkas koma ganda (dengan tiga kali
	 * {@code replaceAll(",,", ",")}), dan hasilnya ditulis balik ke field. Beberapa bentuk yang
	 * tersisa berupa koma saja ({@code ","}, {@code ",,"}, {@code ",,,"}) dinormalkan menjadi
	 * string kosong.</p>
	 *
	 * <p>Tiga kali peringkasan cukup untuk data nyata, tetapi tidak menjamin habis untuk deretan
	 * koma yang sangat panjang. Pola yang sama dipakai {@link #getMhsBolehUploadUlang()}.</p>
	 *
	 * @return daftar id berpembatas koma, atau string kosong; tidak pernah {@code null}
	 * @see #getMhsBolehUploadUlang()
	 */
	@Column(columnDefinition = "text")
	public String getMhsYgTidakIkut() {
		mhsYgTidakIkut = (mhsYgTidakIkut == null || mhsYgTidakIkut.trim().equalsIgnoreCase(",") ? ""
				: "," + mhsYgTidakIkut.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (mhsYgTidakIkut.equals(",")) {
			mhsYgTidakIkut = "";
		} else if (mhsYgTidakIkut.equals(",,")) {
			mhsYgTidakIkut = "";
		} else if (mhsYgTidakIkut.equals(",,,")) {
			mhsYgTidakIkut = "";
		}
		return mhsYgTidakIkut == null ? "" : mhsYgTidakIkut.trim();
	}

	/**
	 * Setel daftar id peserta yang dikecualikan dari pertemuan ini.
	 *
	 * @param mhsYgTidakIkut daftar id berpembatas koma
	 * @see #getMhsYgTidakIkut()
	 */
	public void setMhsYgTidakIkut(String mhsYgTidakIkut) {
		this.mhsYgTidakIkut = mhsYgTidakIkut;
	}

	/**
	 * Daftar mahasiswa peserta pertemuan ini.
	 *
	 * <p>Sumber pesertanya berbeda-beda menurut jenis induk: peserta kelas kuliah, anggota
	 * kelompok KKN/PKL, mahasiswa bimbingan/skripsi/KRS (satu orang saja), peserta yang mendaftar
	 * lewat {@link FormulirKegiatan}, atau mahasiswa pemilik keanggotaan grup pertemuan.</p>
	 *
	 * <p>Seperti {@link #ambilDosen()}, seluruh asosiasi induk disegarkan lebih dulu lewat
	 * getter-nya — komentar KE-20 di dalam method mencatat bahwa proxy {@code kelompokKkn} yang
	 * basi pernah menyebabkan {@code LazyInitializationException}.</p>
	 *
	 * <p><b>Mahal:</b> setiap cabang menjalankan query. Untuk pertemuan sekolah, hasilnya SELALU
	 * kosong — pakai {@link #ambilSiswa()}. Seluruh exception ditelan sehingga kegagalan hanya
	 * tampak sebagai daftar yang lebih pendek atau kosong.</p>
	 *
	 * <p>Perhatikan bahwa daftar ini TIDAK memperhitungkan {@link #getMhsYgTidakIkut()};
	 * penyaringan peserta yang dikecualikan adalah tanggung jawab pemanggil.</p>
	 *
	 * @return daftar mahasiswa peserta; kosong bila jenis induk tidak mengenal mahasiswa
	 * @see #ambilSiswa()
	 * @see #apakahMahasiswaPesertaDisetujuiLangsung(Mahasiswa)
	 */
	public List<Mahasiswa> ambilMahasiswa() {
		List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
		try {
			perkuliahan = getPerkuliahan();
			// KE-20 (pola sama dgn ambilDosen()): refresh kelompokKkn via getter sebelum
			// dipakai -- proxy lama tanpa Session aktif -> LazyInitializationException saat
			// ambilMahasiswaDapatKelompokKkn() memaksa inisialisasi.
			kelompokKkn = getKelompokKkn();
			kelompokPkl = getKelompokPkl();
			mahasiswaRequestTugasAkhir = getMahasiswaRequestTugasAkhir();
			skripsi = getSkripsi();
			krsMahasiswa = getKrsMahasiswa();
			formulirKegiatan = getFormulirKegiatan();
			pertemuanPunyaGrupPertemuan = getPertemuanPunyaGrupPertemuan();

			if (perkuliahan != null) {
				mahasiswas = perkuliahan.ambilMahasiswa();
			} else if (kelompokKkn != null) {
				Collection<MahasiswaDapatKelompokKkn> kel = kelompokKkn.ambilMahasiswaDapatKelompokKkn(false);
				for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : kel) {
					mahasiswas.add(mahasiswaDapatKelompokKkn.getMahasiswa());
				}
				kel = null;
			} else if (kelompokPkl != null) {
				Collection<MahasiswaDapatKelompokPkl> kel = kelompokPkl.ambilMahasiswaDapatKelompokPkl(false);

				for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : kel) {
					mahasiswas.add(mahasiswaDapatKelompokPkl.getMahasiswa());
				}
				kel = null;
			} else if (mahasiswaRequestTugasAkhir != null) {
				mahasiswas.add(mahasiswaRequestTugasAkhir.getMahasiswa());
			} else if (skripsi != null) {
				mahasiswas.add(skripsi.getMahasiswa());
			} else if (krsMahasiswa != null) {
				mahasiswas.add(krsMahasiswa.getMahasiswa());
			} else if (formulirKegiatan != null) {
				mahasiswas = ConstantValues
						.simpleList(HibernateUtil.currentSession().createCriteria(FormulirKegiatanPeserta.class)
								.add(Restrictions.eq("formulirKegiatan", formulirKegiatan))
								.setProjection(Projections.groupProperty("mahasiswa.id")), Mahasiswa.class, false);
			} else if (pertemuanPunyaGrupPertemuan != null) {
				mahasiswas.add(pertemuanPunyaGrupPertemuan.getMahasiswa());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4474");
			// TODO: handle exception
		}
		return mahasiswas;
	}

	/**
	 * Validasi peserta yang selalu membaca database untuk pertemuan perkuliahan.
	 * Pertemuan non-perkuliahan tetap memakai sumber pesertanya masing-masing.
	 *
	 * <p>Apakah seorang mahasiswa memang peserta sah pertemuan ini? Jawabannya ditempuh lewat dua
	 * jalur berbeda, dan perbedaan itu disengaja:</p>
	 * <ul>
	 *   <li><b>Pertemuan perkuliahan</b> — pertanyaan diteruskan ke
	 *       {@link Perkuliahan#apakahMahasiswaPesertaDisetujuiLangsung(Mahasiswa)} yang SELALU
	 *       membaca basis data. Ini penting karena peserta kelas kuliah dapat berubah (KRS
	 *       disetujui/dibatalkan) dan jawaban dari cache berisiko usang — misalnya membiarkan
	 *       mahasiswa yang sudah membatalkan KRS tetap dapat mengisi absensi.</li>
	 *   <li><b>Jenis induk lain</b> — dicocokkan terhadap hasil {@link #ambilMahasiswa()}, yaitu
	 *       sumber peserta masing-masing induk.</li>
	 * </ul>
	 *
	 * <p>Mahasiswa {@code null} atau yang belum punya id selalu ditolak.</p>
	 *
	 * @param mahasiswa mahasiswa yang diperiksa
	 * @return {@code true} bila mahasiswa itu peserta sah pertemuan ini
	 * @see #ambilMahasiswa()
	 */
	public boolean apakahMahasiswaPesertaDisetujuiLangsung(Mahasiswa mahasiswa) {
		if (mahasiswa == null || mahasiswa.getId() == null) {
			return false;
		}
		Perkuliahan dataPerkuliahan = getPerkuliahan();
		if (dataPerkuliahan != null) {
			return dataPerkuliahan.apakahMahasiswaPesertaDisetujuiLangsung(mahasiswa);
		}
		for (Mahasiswa peserta : ambilMahasiswa()) {
			if (peserta != null && mahasiswa.getId().equals(peserta.getId())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Daftar siswa peserta pertemuan ini.
	 *
	 * <p>Padanan sekolah dari {@link #ambilMahasiswa()}, jauh lebih sederhana: hanya pertemuan
	 * dengan induk {@link ais.database.model.sekolah.JadwalPelajaran} yang punya siswa, dan
	 * daftarnya diambil dari anggota kelas ({@code KelasSiswaPunyaSiswa}) pada jadwal itu.</p>
	 *
	 * <p>Membuka {@code Session} Hibernate native sendiri, tetapi TIDAK menutupnya — session
	 * mengandalkan pengelolaan siklus hidup per-thread di tempat lain. Seluruh exception ditelan
	 * sehingga kegagalan hanya tampak sebagai daftar kosong.</p>
	 *
	 * @return daftar siswa peserta; kosong bila pertemuan ini bukan pertemuan jadwal pelajaran
	 * @see #ambilMahasiswa()
	 * @see #ambilGuru()
	 */
	public List<Siswa> ambilSiswa() {
		List<Siswa> siswas = new ArrayList<Siswa>();
		try {
			if (getJadwalPelajaran() != null) {
				Session session = HibernateUtil.currentNativeSession();
				siswas = ConstantValues.simpleList(
						session.createCriteria(KelasSiswaPunyaSiswa.class)
								.setProjection(Projections.property("siswa.id"))
								.add(Restrictions.eq("kelasSiswa", getJadwalPelajaran().getKelas())),
						Siswa.class, false);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4491");
			// TODO: handle exception
		}
		return siswas;
	}

	/**
	 * Haruskah kehadiran pertemuan daring ini mengikuti jadwal yang ditetapkan?
	 *
	 * <p>Saklar penentu apakah batas waktu absensi diberlakukan. Bila {@code true},
	 * {@link #apakahTerlewat()} dapat melaporkan pertemuan sudah lewat dan
	 * {@link #bolehUbahAbsen(Tbmuser)} menolak pengubahan; bila {@code false}, absensi boleh
	 * diisi kapan saja.</p>
	 *
	 * <p>Nilai bawaannya {@code true} (bukan {@code false}) — jadi pertemuan yang tidak pernah
	 * diatur bersifat KETAT terhadap jadwal.</p>
	 *
	 * <p>Perhatikan ejaan nama propertinya: {@code Perkulaiahn}, salah ketik yang sudah terlanjur
	 * dipakai di kolom basis data dan di seluruh pemanggil, sehingga tidak dapat diperbaiki tanpa
	 * migrasi.</p>
	 *
	 * @return {@code true} bila kehadiran harus sesuai jadwal; tidak pernah {@code null}
	 * @see #apakahTerlewat()
	 */
	public Boolean getPerkulaiahnOnlineHarusSesuaiJadwal() {
//		if (perkuliahan != null && perkuliahan.getWaktuPerkuliahanOnlineBebas()) {
//			perkulaiahnOnlineHarusSesuaiJadwal = true;
//		}
		return perkulaiahnOnlineHarusSesuaiJadwal == null ? true : perkulaiahnOnlineHarusSesuaiJadwal;
	}

	/**
	 * Setel saklar "kehadiran harus sesuai jadwal".
	 *
	 * @param perkulaiahnOnlineHarusSesuaiJadwal {@code true} untuk memberlakukan batas waktu;
	 *                                           {@code null} berarti kembali ke bawaan {@code true}
	 * @see #getPerkulaiahnOnlineHarusSesuaiJadwal()
	 */
	public void setPerkulaiahnOnlineHarusSesuaiJadwal(Boolean perkulaiahnOnlineHarusSesuaiJadwal) {
		this.perkulaiahnOnlineHarusSesuaiJadwal = perkulaiahnOnlineHarusSesuaiJadwal;
	}

	/**
	 * Nilai isian dinamis pertemuan ini dalam bentuk SIAP TAMPIL, sebagai satu string berformat
	 * khusus.
	 *
	 * <p>Bersama {@link #getParameterTambahanInds()}, kolom ini menyimpan jawaban atas
	 * parameter-parameter tambahan yang didefinisikan administrator (lihat
	 * {@link #ambilKelompokParameterTambahanPertemuanTotal()}). Keduanya ditulis sekaligus oleh
	 * {@link #populateParameterTambahan(java.util.List)}.</p>
	 *
	 * <p><b>Format:</b> baris dipisah baris baru ({@code '\n'}), kolom dipisah penanda tiga
	 * karakter {@code "<=>"}. Urutan kolomnya:</p>
	 * <pre>
	 * 0 label       "namaKelompok-&gt;labelInputan" (siap ditampilkan)
	 * 1 nilai
	 * 2 url         tautan lampiran, bila parameternya mewajibkan lampiran
	 * 3 nomorUrut
	 * 4 idParameterTambahan
	 * 5 idKelompokParameterTambahanPertemuan
	 * 6 indexKe
	 * 7 keterangan
	 * </pre>
	 *
	 * <p>Untuk MEMBACA isinya, pakai {@link #ambilDataParameterTambahan()} — jangan mengurai
	 * string ini sendiri.</p>
	 *
	 * @return string parameter tambahan; string kosong bila belum ada, tidak pernah {@code null}
	 * @see #getParameterTambahanInds()
	 * @see #ambilDataParameterTambahan()
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		return parameterTambahan;
	}

	/**
	 * Setel string parameter tambahan siap tampil.
	 *
	 * <p>Normalnya diisi oleh {@link #populateParameterTambahan(java.util.List)}, bukan langsung
	 * oleh kode lain.</p>
	 *
	 * @param parameterTambahan string berformat {@code "<=>"} (lihat {@link #getParameterTambahan()})
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Urai {@link #getParameterTambahan()} menjadi daftar {@link CommonVO} yang siap ditampilkan,
	 * terurut menurut nomor urut.
	 *
	 * <p>Pemetaan kolom ke properti {@link CommonVO}:</p>
	 * <ul>
	 *   <li>kolom 0 &rarr; {@code name} (label);</li>
	 *   <li>kolom 1 &rarr; {@code name1} (nilai);</li>
	 *   <li>kolom 2 &rarr; {@code name2} (url lampiran);</li>
	 *   <li>kolom 3 &rarr; {@code nomorUrut}, gagal urai menjadi {@code 1};</li>
	 *   <li>kolom 4 &rarr; {@code id}, gagal urai menjadi {@code 1}.</li>
	 * </ul>
	 *
	 * <p>Baris tak lengkap tidak dilewati melainkan diisi nilai kosong, sehingga string parameter
	 * yang kosong pun tetap menghasilkan SATU entri kosong (karena {@code "".split("\n")}
	 * menghasilkan array berisi satu string kosong). Pemanggil perlu menyadari hal ini bila
	 * memakai ukuran daftar sebagai penanda "ada isian atau tidak".</p>
	 *
	 * <p>Pengurutan akhir memakai {@link Comparable} milik {@link CommonVO}, yang berbasis
	 * {@code nomorUrut}.</p>
	 *
	 * @return daftar isian dinamis terurut; tidak pernah {@code null}
	 * @see #getParameterTambahan()
	 * @see #populateParameterTambahan(java.util.List)
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
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4535");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4541");

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
	 * Baca nilai isian dinamis dari baris-baris komponen ZK dan simpan ke
	 * {@link #getParameterTambahan()} serta {@link #getParameterTambahanInds()}.
	 *
	 * <p>Method terpanjang di kelas ini, dan satu-satunya yang MENGAMBIL data langsung dari
	 * komponen UI. Tiap {@link Row} diharapkan membawa atribut {@code "parameterTambahan"},
	 * opsional {@code "parameterTambahan_2"}, {@code "kelompokParameterTambahanPertemuan"},
	 * {@code "indexKe"}, dan {@code "keterangan"} (sebuah {@link Textbox}).</p>
	 *
	 * <h4>Tiga mode penyimpanan nilai</h4>
	 * <p>Inilah bagian yang paling mudah salah dipahami. Satu parameter dapat menyimpan nilainya
	 * sebagai teks biasa ATAU sebagai JSON objek berkunci, tergantung sifat kelompoknya:</p>
	 * <ul>
	 *   <li><b>Biasa</b> — nilai disimpan apa adanya, satu nilai untuk seluruh pertemuan.</li>
	 *   <li><b>{@code untukDosenDanAdmin}</b> — nilai lama diurai sebagai JSON, lalu nilai baru
	 *       dimasukkan dengan kunci <b>id PERTEMUAN</b>. Dengan begitu satu parameter dapat
	 *       menyimpan jawaban berbeda per pertemuan.</li>
	 *   <li><b>{@code diisiPerPeserta}</b> — sama, tetapi kuncinya <b>id MAHASISWA</b> (atau id
	 *       SISWA bila penggunanya siswa), diambil dari {@code Common.getCurrentUser()}. Jadi
	 *       jawaban tersimpan per peserta.</li>
	 * </ul>
	 * <p>Keterangan diperlakukan persis sama seperti nilai pada ketiga mode itu.</p>
	 *
	 * <h4>Dua string yang ditulis</h4>
	 * <p>{@link #getParameterTambahan()} menyimpan bentuk SIAP TAMPIL (memakai nama kelompok dan
	 * label inputan), sedangkan {@link #getParameterTambahanInds()} menyimpan bentuk BERBASIS ID
	 * (memakai {@code "idKelompok->idParameter"}). Yang kedua itulah yang dibaca kembali di awal
	 * method ini untuk mengambil nilai lama sebelum digabung.</p>
	 *
	 * <h4>Parameter bertingkat</h4>
	 * <p>Bila baris membawa {@code parameterTambahan_2}, satu entri TAMBAHAN ditulis dengan kunci
	 * bertingkat {@code "idKelompok->idParameter2->idParameter"}, untuk isian yang bergantung pada
	 * jawaban isian lain.</p>
	 *
	 * <h4>Efek samping dan catatan</h4>
	 * <ul>
	 *   <li>Kedua kolom ditulis lewat setter di akhir method; tidak ada penyimpanan ke basis data
	 *       — pemanggil yang harus menyimpan {@link Pertemuan}-nya.</li>
	 *   <li>Untuk parameter yang mewajibkan lampiran, tautannya diambil dari
	 *       {@code LampiranLain.ambil(getId(), jenis)}.</li>
	 *   <li><b>Ada {@code System.out.println("ket => " ...)} yang tertinggal</b> dan dijalankan
	 *       untuk SETIAP baris pada setiap penyimpanan — sisa penelusuran yang mengotori log
	 *       produksi.</li>
	 *   <li>Kegagalan per baris ditangkap {@code Common.tampilErrorJikaAdmin(e)}, sehingga hanya
	 *       terlihat oleh admin; baris yang gagal tidak masuk hasil tanpa pemberitahuan ke
	 *       pengguna biasa.</li>
	 *   <li>{@code parameterRows} yang {@code null} atau kosong membuat method langsung berhenti —
	 *       nilai lama TIDAK terhapus.</li>
	 * </ul>
	 *
	 * @param parameterRows baris-baris komponen ZK yang memuat isian dinamis
	 * @see #getParameterTambahan()
	 * @see #getParameterTambahanInds()
	 * @see #ambilKelompokParameterTambahanPertemuanTotal()
	 */
	public void populateParameterTambahan(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		Tbmuser tbmuser = Common.getCurrentUser();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				ParameterTambahan parameterTambahan_2 = (ParameterTambahan) row.getAttribute("parameterTambahan_2");

				KelompokParameterTambahanPertemuan kelompokParameterTambahanPertemuan = (KelompokParameterTambahanPertemuan) row
						.getAttribute("kelompokParameterTambahanPertemuan");
				Long indexKe = (Long) row.getAttribute("indexKe");
				if (parameterTambahan != null && kelompokParameterTambahanPertemuan != null) {
					String jenis = kelompokParameterTambahanPertemuan.getId() + "->" + parameterTambahan.getId();

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

					String valAsli = "";
					String ketAsli = "";
					String[] spl = getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							valAsli = value.length > 1 ? value[1].trim() : "";

							try {
								ketAsli = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4606");

							}

							if (kelompokParameterTambahanPertemuan.getUntukDosenDanAdmin()) {
								try {
									JSONObject jsonObject = new JSONObject();
									try {
										jsonObject = new JSONObject(valAsli);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4615");

									}
									jsonObject.put(getId().toString(), val);
									val = jsonObject.toString();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4621");
								}

								try {
									JSONObject jsonObject = new JSONObject();
									try {
										jsonObject = new JSONObject(ketAsli);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4628");

									}
									jsonObject.put(getId().toString(), ket);
									ket = jsonObject.toString();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4634");
								}
							}

							else if (mahasiswa != null && kelompokParameterTambahanPertemuan.getDiisiPerPeserta()) {
								try {
									JSONObject jsonObject = new JSONObject();
									try {
										jsonObject = new JSONObject(valAsli);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4643");

									}
									jsonObject.put(mahasiswa.getId().toString(), val);
									val = jsonObject.toString();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4649");
								}

								try {
									JSONObject jsonObject = new JSONObject();
									try {
										jsonObject = new JSONObject(ketAsli);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4656");

									}
									jsonObject.put(mahasiswa.getId().toString(), ket);
									ket = jsonObject.toString();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4662");
								}
							} else if (siswa != null && kelompokParameterTambahanPertemuan.getDiisiPerPeserta()) {
								try {
									JSONObject jsonObject = new JSONObject();
									try {
										jsonObject = new JSONObject(valAsli);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4669");

									}
									jsonObject.put(siswa.getId().toString(), val);
									val = jsonObject.toString();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4675");
								}

								try {
									JSONObject jsonObject = new JSONObject();
									try {
										jsonObject = new JSONObject(ketAsli);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4682");

									}
									jsonObject.put(siswa.getId().toString(), ket);
									ket = jsonObject.toString();
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4688");
								}
							}

						}
					}

					System.out.println("ket => " + ket + ", val => " + val);

					String s = kelompokParameterTambahanPertemuan.getNama() + "->" + parameterTambahan.getLabelInputan()
							+ "<=>" + val + "<=>" + url + "<=>" + parameterTambahan.getNomorUrut() + "<=>"
							+ parameterTambahan.getId() + "<=>" + kelompokParameterTambahanPertemuan.getId() + "<=>"
							+ (indexKe == null ? 0 : indexKe) + "<=>" + ket;

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanPertemuan.getId() + "->" + parameterTambahan.getId() + "<=>"
							+ val + "<=>" + url + "<=>" + ket;
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;

					if (parameterTambahan_2 != null) {
						valAsli = "";
						ketAsli = "";

						ket = keterangan == null ? "" : keterangan.getValue().trim();
						val = ParameterTambahan.ambilVal(row, parameterTambahan_2, "component_2");

						jenis = kelompokParameterTambahanPertemuan.getId() + "->" + parameterTambahan_2.getId() + "->"
								+ parameterTambahan.getId();

						for (String d : spl) {
							String[] value = d.split("<=>");
							if (value[0].trim().equalsIgnoreCase(jenis)) {
								valAsli = value.length > 1 ? value[1].trim() : "";

								try {
									ketAsli = value.length > 0 ? value[value.length - 1] : "";
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4725");

								}

								if (kelompokParameterTambahanPertemuan.getUntukDosenDanAdmin()) {
									try {
										JSONObject jsonObject = new JSONObject();
										try {
											jsonObject = new JSONObject(valAsli);
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4734");

										}
										jsonObject.put(getId().toString(), val);
										val = jsonObject.toString();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4740");
									}

									try {
										JSONObject jsonObject = new JSONObject();
										try {
											jsonObject = new JSONObject(ketAsli);
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4747");

										}
										jsonObject.put(getId().toString(), ket);
										ket = jsonObject.toString();
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4753");
									}
								}
							}
						}

						s = kelompokParameterTambahanPertemuan.getNama() + "->" + parameterTambahan_2.getLabelInputan()
								+ "->" + parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
								+ parameterTambahan_2.getNomorUrut() + "<=>" + parameterTambahan_2.getId() + "<=>"
								+ kelompokParameterTambahanPertemuan.getId() + "<=>" + (indexKe == null ? 0 : indexKe)
								+ "<=>" + ket;

						parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

						sIds = kelompokParameterTambahanPertemuan.getId() + "->" + parameterTambahan_2.getId() + "->"
								+ parameterTambahan.getId() + "<=>" + val + "<=>" + url + "<=>" + ket;
						parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	/**
	 * Nilai isian dinamis pertemuan ini dalam bentuk BERBASIS ID.
	 *
	 * <p>Kembaran {@link #getParameterTambahan()} yang memakai id alih-alih label, sehingga tetap
	 * dapat dibaca walau administrator mengganti nama kelompok atau label inputan. Inilah string
	 * yang dibaca kembali oleh {@link #populateParameterTambahan(java.util.List)} untuk mengambil
	 * nilai lama.</p>
	 *
	 * <p><b>Format:</b> baris dipisah baris baru, kolom dipisah {@code "<=>"}:</p>
	 * <pre>
	 * 0 kunci       "idKelompok-&gt;idParameter", atau bertingkat
	 *               "idKelompok-&gt;idParameter2-&gt;idParameter"
	 * 1 nilai       teks biasa, atau JSON objek berkunci id pertemuan/peserta
	 * 2 url         tautan lampiran
	 * 3 keterangan  teks biasa, atau JSON objek berkunci id pertemuan/peserta
	 * </pre>
	 *
	 * @return string parameter tambahan berbasis id; string kosong bila belum ada
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
	 * Setel string parameter tambahan berbasis id.
	 *
	 * <p>Normalnya diisi oleh {@link #populateParameterTambahan(java.util.List)}.</p>
	 *
	 * @param parameterTambahanInds string berformat {@code "<=>"} berbasis id
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Daftar id peserta yang diberi izin MENGUNGGAH ULANG tugas pertemuan ini.
	 *
	 * <p>Memakai format dan mekanisme normalisasi yang sama persis dengan
	 * {@link #getMhsYgTidakIkut()}: teks {@code ",id1,id2,"} berpembungkus koma, dirakit ulang dan
	 * ditulis balik ke field setiap kali getter dipanggil.</p>
	 *
	 * <p>Biasanya dipakai untuk memberi kelonggaran kepada peserta tertentu yang salah unggah,
	 * tanpa membuka unggah ulang bagi seluruh kelas.</p>
	 *
	 * @return daftar id berpembatas koma, atau string kosong; tidak pernah {@code null}
	 * @see #getMhsYgTidakIkut()
	 */
	@Column(columnDefinition = "text")
	public String getMhsBolehUploadUlang() {
		mhsBolehUploadUlang = (mhsBolehUploadUlang == null || mhsBolehUploadUlang.trim().equalsIgnoreCase(",") ? ""
				: "," + mhsBolehUploadUlang.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (mhsBolehUploadUlang.equals(",")) {
			mhsBolehUploadUlang = "";
		} else if (mhsBolehUploadUlang.equals(",,")) {
			mhsBolehUploadUlang = "";
		} else if (mhsBolehUploadUlang.equals(",,,")) {
			mhsBolehUploadUlang = "";
		}
		return mhsBolehUploadUlang == null ? "" : mhsBolehUploadUlang.trim();
	}

	/**
	 * Setel daftar id peserta yang boleh mengunggah ulang tugas.
	 *
	 * @param mhsBolehUploadUlang daftar id berpembatas koma
	 * @see #getMhsBolehUploadUlang()
	 */
	public void setMhsBolehUploadUlang(String mhsBolehUploadUlang) {
		this.mhsBolehUploadUlang = mhsBolehUploadUlang;
	}

	/**
	 * Tautan pertemuan daring memakai layanan LAIN yang tidak punya kolom sendiri.
	 *
	 * <p>Penampung serbaguna untuk {@link #LAIN}; mengikuti pola pemungutan URL yang dijelaskan
	 * pada {@link #getZoomLink()}.</p>
	 *
	 * @return URL layanan lain, atau {@code null} bila belum diisi
	 * @see #getOnlineMenggunakan()
	 */
	@Column(columnDefinition = "text")
	public String getLainLink() {
//		if (lainLink == null || lainLink.trim().isEmpty()) {
//			VOPembelajaran pembelajaran = ambilVOPembelajaran();
//			if (pembelajaran != null) {
//				lainLink = pembelajaran.retreive("link_online");
//			}
//		}

		try {
			if (lainLink != null && !lainLink.trim().isEmpty() && lainLink.toLowerCase().trim().contains(" ")) {
				List<String> urls = Common.getUrls(lainLink.trim());
				lainLink = urls.isEmpty() ? "" : urls.get(0);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Pertemuan.java:4827");
		}

		return lainLink == null || lainLink.trim().isEmpty() ? null : lainLink.trim();
	}

	/**
	 * Setel tautan layanan daring lain.
	 *
	 * @param lainLink URL layanan lain
	 * @see #getLainLink()
	 */
	public void setLainLink(String lainLink) {
		this.lainLink = lainLink;
	}

	/**
	 * Bolehkah dosen mengisi absensi dengan swafoto (bukti kehadiran berupa foto)?
	 *
	 * <p><b>Getter ini mengubah keadaan objek dan menerapkan aturan "yang paling ketat
	 * menang".</b> Bila {@link Perkuliahan} induk MELARANG absen berfoto, nilai di pertemuan ini
	 * dipaksa {@code false} dan ditulis ke field. Sebaliknya bila perkuliahan MENGIZINKAN, nilai
	 * pertemuan tidak diubah — jadi pertemuan dapat lebih ketat daripada perkuliahannya, tetapi
	 * tidak dapat lebih longgar.</p>
	 *
	 * <p>Nilai bawaannya {@code true}.</p>
	 *
	 * @return {@code true} bila dosen boleh absen memakai foto; tidak pernah {@code null}
	 * @see #getMahasiswaBolehAbsenMenggunakanFoto()
	 */
	public Boolean getDosenBolehAbsenMenggunakanFoto() {

		perkuliahan = getPerkuliahan();
		if (perkuliahan != null && !perkuliahan.getDosenBolehAbsenMenggunakanFoto()) {
			dosenBolehAbsenMenggunakanFoto = false;
		}

		return dosenBolehAbsenMenggunakanFoto == null ? true : dosenBolehAbsenMenggunakanFoto;
	}

	/**
	 * Setel izin dosen mengisi absensi dengan swafoto.
	 *
	 * <p>Nilai {@code true} dapat ditimpa kembali menjadi {@code false} oleh
	 * {@link #getDosenBolehAbsenMenggunakanFoto()} bila perkuliahan induk melarangnya.</p>
	 *
	 * @param dosenBolehAbsenMenggunakanFoto {@code true} bila diizinkan
	 */
	public void setDosenBolehAbsenMenggunakanFoto(Boolean dosenBolehAbsenMenggunakanFoto) {
		this.dosenBolehAbsenMenggunakanFoto = dosenBolehAbsenMenggunakanFoto;
	}

	/**
	 * Bolehkah mahasiswa mengisi absensi dengan swafoto?
	 *
	 * <p>Berlaku aturan "yang paling ketat menang" yang sama seperti
	 * {@link #getDosenBolehAbsenMenggunakanFoto()}: larangan di tingkat {@link Perkuliahan}
	 * memaksa nilai di sini menjadi {@code false}.</p>
	 *
	 * <p>Nilai bawaannya {@code true}.</p>
	 *
	 * @return {@code true} bila mahasiswa boleh absen memakai foto; tidak pernah {@code null}
	 * @see #getDosenBolehAbsenMenggunakanFoto()
	 */
	public Boolean getMahasiswaBolehAbsenMenggunakanFoto() {

		perkuliahan = getPerkuliahan();
		if (perkuliahan != null && !perkuliahan.getMahasiswaBolehAbsenMenggunakanFoto()) {
			mahasiswaBolehAbsenMenggunakanFoto = false;
		}

		return mahasiswaBolehAbsenMenggunakanFoto == null ? true : mahasiswaBolehAbsenMenggunakanFoto;
	}

	/**
	 * Setel izin mahasiswa mengisi absensi dengan swafoto.
	 *
	 * @param mahasiswaBolehAbsenMenggunakanFoto {@code true} bila diizinkan
	 * @see #getMahasiswaBolehAbsenMenggunakanFoto()
	 */
	public void setMahasiswaBolehAbsenMenggunakanFoto(Boolean mahasiswaBolehAbsenMenggunakanFoto) {
		this.mahasiswaBolehAbsenMenggunakanFoto = mahasiswaBolehAbsenMenggunakanFoto;
	}

	/**
	 * Berapa menit SEBELUM jam mulai absensi sudah boleh diisi.
	 *
	 * <p>Bersama {@link #getBolehAbsenSetelahWaktuMulaiDalamMenit()} membentuk jendela waktu
	 * absensi di sekitar jam mulai pertemuan.</p>
	 *
	 * <p><b>Getter ini mengubah keadaan objek:</b> bila {@link Perkuliahan} induk mengaktifkan
	 * {@code bolehAbsenWaktuIkutiPerkuliahan}, nilai dari perkuliahan MENIMPA nilai pertemuan dan
	 * ditulis ke field. Berbeda dari saklar swafoto, di sini yang berlaku adalah "perkuliahan
	 * menang", bukan "yang paling ketat menang".</p>
	 *
	 * <p>Nilai bawaannya {@code 30} menit.</p>
	 *
	 * @return toleransi menit sebelum jam mulai; tidak pernah {@code null}
	 * @see #getBolehAbsenSetelahWaktuMulaiDalamMenit()
	 */
	public Integer getBolehAbsenSebelumWaktuMulaiDalamMenit() {
		perkuliahan = getPerkuliahan();
		if (perkuliahan != null && perkuliahan.getBolehAbsenWaktuIkutiPerkuliahan()) {
			bolehAbsenSebelumWaktuMulaiDalamMenit = perkuliahan.getBolehAbsenSebelumWaktuMulaiDalamMenit();
		}
		return bolehAbsenSebelumWaktuMulaiDalamMenit == null ? 30 : bolehAbsenSebelumWaktuMulaiDalamMenit;
	}

	/**
	 * Setel toleransi menit sebelum jam mulai untuk pengisian absensi.
	 *
	 * @param mahasiswaBolehAbsenSebelumWaktuMulaiDalamMenit toleransi dalam menit
	 * @see #getBolehAbsenSebelumWaktuMulaiDalamMenit()
	 */
	public void setBolehAbsenSebelumWaktuMulaiDalamMenit(Integer mahasiswaBolehAbsenSebelumWaktuMulaiDalamMenit) {
		this.bolehAbsenSebelumWaktuMulaiDalamMenit = mahasiswaBolehAbsenSebelumWaktuMulaiDalamMenit;
	}

	/**
	 * Berapa menit SETELAH jam mulai absensi masih boleh diisi.
	 *
	 * <p>Pasangan {@link #getBolehAbsenSebelumWaktuMulaiDalamMenit()}, dengan penimpaan dari
	 * {@link Perkuliahan} yang serupa dan nilai bawaan {@code 30} menit.</p>
	 *
	 * <p><b>Ketidakselarasan yang dicatat:</b> berbeda dari pasangannya, method ini menguji field
	 * {@code perkuliahan} secara LANGSUNG tanpa memanggil {@link #getPerkuliahan()} lebih dahulu.
	 * Bila proxy perkuliahan belum terinisialisasi, penimpaan dari perkuliahan TERLEWAT di sini
	 * padahal terjadi pada pasangannya — sehingga jendela sebelum dan sesudah bisa berasal dari
	 * sumber yang berbeda.</p>
	 *
	 * @return toleransi menit setelah jam mulai; tidak pernah {@code null}
	 * @see #getBolehAbsenSebelumWaktuMulaiDalamMenit()
	 */
	public Integer getBolehAbsenSetelahWaktuMulaiDalamMenit() {
		if (perkuliahan != null && perkuliahan.getBolehAbsenWaktuIkutiPerkuliahan()) {
			bolehAbsenSetelahWaktuMulaiDalamMenit = perkuliahan.getBolehAbsenSetelahWaktuMulaiDalamMenit();
		}
		return bolehAbsenSetelahWaktuMulaiDalamMenit == null ? 30 : bolehAbsenSetelahWaktuMulaiDalamMenit;
	}

	/**
	 * Setel toleransi menit setelah jam mulai untuk pengisian absensi.
	 *
	 * @param bolehAbsenSetelahWaktuMulaiDalamMenit toleransi dalam menit
	 * @see #getBolehAbsenSetelahWaktuMulaiDalamMenit()
	 */
	public void setBolehAbsenSetelahWaktuMulaiDalamMenit(Integer bolehAbsenSetelahWaktuMulaiDalamMenit) {
		this.bolehAbsenSetelahWaktuMulaiDalamMenit = bolehAbsenSetelahWaktuMulaiDalamMenit;
	}

	/**
	 * Tetapkan wisuda sebagai induk pertemuan ini.
	 *
	 * @param wisuda wisuda; boleh {@code null}
	 * @see #getWisuda()
	 */
	public void setWisuda(Wisuda wisuda) {
		this.wisuda = wisuda;
	}

	/**
	 * Wisuda yang menjadi induk pertemuan ini.
	 *
	 * <p>Salah satu jenis induk yang dipakai {@link #warna()}, {@link #info()}, dan
	 * {@link #ambilVOPembelajaran()} tetapi TIDAK punya cabang di {@link #untuk()} — sehingga
	 * pertemuan wisuda menghasilkan {@code null} dari method itu.</p>
	 *
	 * @return {@link Wisuda} induk, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "wisuda", nullable = true)
	public Wisuda getWisuda() {
		wisuda = check(wisuda);
		return wisuda;
	}

	/**
	 * Syarat akses pertemuan ini sebagai teks JSON.
	 *
	 * <p>Nilai kosong dikembalikan sebagai JSON objek kosong ({@code "{}"}) sehingga pemanggil
	 * selalu aman mem-parsingnya.</p>
	 *
	 * @return teks JSON syarat akses; {@code "{}"} bila belum ada
	 */
	@Column(columnDefinition = "text")
	public String getSyaratAkses() {
		return syaratAkses == null || syaratAkses.trim().isEmpty() ? new JSONObject().toString() : syaratAkses;
	}

	/**
	 * Setel syarat akses pertemuan ini.
	 *
	 * @param syaratAkses teks JSON syarat akses
	 * @see #getSyaratAkses()
	 */
	public void setSyaratAkses(String syaratAkses) {
		this.syaratAkses = syaratAkses;
	}

	/**
	 * Catatan konfirmasi kehadiran peserta OLEH DOSEN, sebagai satu string berformat khusus.
	 *
	 * <p>Kolom ini adalah lapisan kedua di atas {@link #getAbsensi()}: peserta (atau sistem)
	 * mencatat kehadiran di {@code absensi}, lalu dosen MENGONFIRMASI catatan itu di sini.</p>
	 *
	 * <p>Tata letaknya sembilan slot yang SAMA dengan {@code absensi} (baris dipisah {@code ';'},
	 * slot dipisah {@code ','}), dengan satu perbedaan penting: <b>slot 4 berisi {@code dosen.id}
	 * dosen yang mengonfirmasi</b>, bukan id pengajuan izin. Karena itu pencarian barisnya memakai
	 * PASANGAN (ref, dosen) — satu peserta dapat punya beberapa baris konfirmasi dari dosen
	 * berbeda.</p>
	 *
	 * <p>Jangan mengurai string ini sendiri; pakai keluarga
	 * {@code retreiveAbsensiXxxKonfirmasi(Long, Dosen)}.</p>
	 *
	 * @return string konfirmasi mentah yang sudah di-{@code trim}; string kosong bila belum ada
	 * @see #populateKonfirmasi(Long, Statusabsensi, String, String, String, String, Dosen)
	 * @see #getAbsensi()
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganKonfirmasi() {
		return keteranganKonfirmasi == null ? "" : keteranganKonfirmasi.trim();
	}

	/**
	 * Timpa SELURUH string konfirmasi kehadiran pertemuan ini.
	 *
	 * <p>Untuk mengubah konfirmasi satu peserta, pakai
	 * {@link #populateKonfirmasi(Long, Statusabsensi, String, String, String, String, Dosen)}
	 * yang menjaga baris lain tetap utuh.</p>
	 *
	 * @param keteranganKonfirmasi string konfirmasi berformat sembilan slot
	 */
	public void setKeteranganKonfirmasi(String keteranganKonfirmasi) {
		this.keteranganKonfirmasi = keteranganKonfirmasi;
	}

	/**
	 * Catat konfirmasi kehadiran seorang peserta oleh dosen — bentuk ringkas tanpa keterangan.
	 *
	 * @param ref           id peserta yang dikonfirmasi
	 * @param statusabsensi status kehadiran hasil konfirmasi
	 * @param mulai         jam mulai kehadiran
	 * @param sampai        jam selesai kehadiran
	 * @param jenis         jenis peserta
	 * @param dosen         dosen yang mengonfirmasi; tidak boleh {@code null}
	 * @see #populateKonfirmasi(Long, Statusabsensi, String, String, String, String, Dosen)
	 */
	public void populateKonfirmasi(Long ref, Statusabsensi statusabsensi, String mulai, String sampai, String jenis,
			Dosen dosen) {
		populateKonfirmasi(ref, statusabsensi, null, mulai, sampai, jenis, dosen);
	}

	/**
	 * Catat/ubah konfirmasi kehadiran seorang peserta oleh dosen tertentu.
	 *
	 * <p>Kembaran {@link #populate(Long, Statusabsensi, String,
	 * PengajuanIzinTidakMasukPerkuliahan, String, String, String)} untuk kolom
	 * {@link #getKeteranganKonfirmasi()}, dengan tiga perbedaan pokok:</p>
	 * <ul>
	 *   <li><b>Kunci pencarian barisnya PASANGAN (ref, dosen)</b>, bukan {@code ref} saja —
	 *       slot 4 diisi {@code dosen.getId()}. Karena itu beberapa dosen dapat mengonfirmasi
	 *       peserta yang sama secara terpisah, masing-masing punya barisnya sendiri.</li>
	 *   <li>Tidak ada notifikasi; tidak ada thread yang dijalankan.</li>
	 *   <li>Kegagalan per baris ditangani {@code Common.tampilErrorJikaAdmin(e)} sehingga hanya
	 *       tampak oleh admin.</li>
	 * </ul>
	 *
	 * <p>Perilaku lain identik: {@code ref}/{@code statusabsensi} {@code null} membatalkan seluruh
	 * operasi; status selain {@code "M"} mengosongkan jam; {@code ';'} dan {@code ','} pada
	 * keterangan diganti; dan nilai {@code null} pada parameter opsional berarti
	 * <b>"pertahankan nilai lama"</b> (dibaca kembali lewat keluarga
	 * {@code retreiveAbsensiXxxKonfirmasi(Long, Dosen)}), bukan "kosongkan".</p>
	 *
	 * <p>Hasilnya ditulis langsung ke field {@code keteranganKonfirmasi}.</p>
	 *
	 * <p><b>Perhatian:</b> {@code dosen} di-dereference tanpa penjagaan {@code null}, sehingga
	 * memanggilnya dengan {@code null} melempar {@link NullPointerException} — yang, di dalam
	 * perulangan, akan tertangkap dan hanya dilaporkan kepada admin.</p>
	 *
	 * @param ref           id peserta yang dikonfirmasi
	 * @param statusabsensi status kehadiran hasil konfirmasi; {@code null} membatalkan operasi
	 * @param keterangan    keterangan bebas; {@code null} berarti pertahankan nilai lama
	 * @param mulai         jam mulai kehadiran; {@code null} berarti pertahankan nilai lama
	 * @param sampai        jam selesai kehadiran; {@code null} berarti pertahankan nilai lama
	 * @param jenis         jenis peserta; {@code null} berarti pertahankan nilai lama
	 * @param dosen         dosen yang mengonfirmasi; tidak boleh {@code null}
	 * @see #getKeteranganKonfirmasi()
	 */
	public void populateKonfirmasi(Long ref, Statusabsensi statusabsensi, String keterangan, String mulai,
			String sampai, String jenis, Dosen dosen) {
		if (ref != null && statusabsensi != null) {

			if (statusabsensi.getKode() == null || !statusabsensi.getKode().equals("M")) {
				mulai = "";
				sampai = "";
			}

			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ";", "..\n");
			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ",", "_");
			String formatBaru = "";
			String[] nilais = getKeteranganKonfirmasi().split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = nn.split(",");
					if (!s[0].trim().isEmpty()) {
						Long formatId = Long.parseLong(s[0]);
						Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
						if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
							aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode() + ","
									+ statusabsensi.getNama() + "," + dosen.getId() + ","
									+ (keterangan == null ? retreiveAbsensiKeteranganKonfirmasi(ref, dosen)
											: keterangan)
									+ "," + (mulai == null ? retreiveAbsensiMulaiKonfirmasi(ref, dosen) : mulai) + ","
									+ (sampai == null ? retreiveAbsensiSampaiKonfirmasi(ref, dosen) : sampai) + ","
									+ (jenis == null ? retreiveAbsensiJenisKonfirmasi(ref, dosen) : jenis);
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!ada) {
				String aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode() + ","
						+ statusabsensi.getNama() + "," + dosen.getId() + ","
						+ (keterangan == null ? retreiveAbsensiKeteranganKonfirmasi(ref, dosen) : keterangan) + ","
						+ (mulai == null ? retreiveAbsensiMulaiKonfirmasi(ref, dosen) : mulai) + ","
						+ (sampai == null ? retreiveAbsensiSampaiKonfirmasi(ref, dosen) : sampai) + ","
						+ (jenis == null ? retreiveAbsensiJenisKonfirmasi(ref, dosen) : jenis);
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

//			System.out.println("formatBaru => " + formatBaru);

			keteranganKonfirmasi = formatBaru;
		}
	}

	/**
	 * Ambil {@code statusabsensi.id} (slot 1) dari baris konfirmasi milik pasangan (ref, dosen).
	 *
	 * <p><b>Method rujukan untuk seluruh keluarga {@code retreiveAbsensiXxxKonfirmasi(Long,
	 * Dosen)}.</b> Polanya sama dengan keluarga {@code retreiveAbsensiXxx(Long)} pada kolom
	 * {@code absensi}, hanya saja barisnya dicocokkan dengan DUA syarat: slot 0 sama dengan
	 * {@code ref} DAN slot 4 sama dengan id dosen.</p>
	 *
	 * <p>Varian ini punya penjagaan paling lengkap di keluarganya (memeriksa panjang array dan
	 * slot kosong sebelum {@code parseLong}), hasil perbaikan {@link NumberFormatException} pada
	 * data konfirmasi lama yang tidak lengkap. Sisa keluarganya masih memakai
	 * {@code s[4].isEmpty()} secara langsung, yang dapat melempar
	 * {@link ArrayIndexOutOfBoundsException} pada baris pendek — exception itu tertangkap dan
	 * barisnya dilewati, jadi akibatnya hanya baris rusak yang terlewat.</p>
	 *
	 * @param ref   id peserta yang dicari
	 * @param dosen dosen yang mengonfirmasi
	 * @return id status absensi, atau {@code -1L} bila tidak ada baris yang cocok
	 * @see #retreiveAbsensiId(Long)
	 */
	public Long retreiveAbsensiIdKonfirmasi(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganKonfirmasi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					// FIX NumberFormatException "For input string: """: baris keterangan
					// konfirmasi lama/belum lengkap bisa punya token kosong atau kurang dari
					// yang diharapkan -> guard eksplisit sebelum parseLong, selaras dgn
					// retreiveAbsensiKode/retreiveAbsensiMulai yang sudah diperbaiki serupa.
					if (s.length < 2 || s[0] == null || s[0].trim().length() == 0
							|| s[1] == null || s[1].trim().length() == 0) {
						continue;
					}
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = (s.length < 5 || s[4] == null || s[4].trim().length() == 0) ? -1L
							: Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return Long.parseLong(s[1]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:4992");

				}
			}
		}

		return -1L;
	}

	/**
	 * Ambil isi slot 4 dari baris konfirmasi milik pasangan (ref, dosen).
	 *
	 * <p><b>Nama method ini menyesatkan</b> (dicatat, tidak diperbaiki). Pada kolom
	 * {@code absensi}, slot 4 memang berisi id pengajuan izin — lihat
	 * {@link #retreivePengajuanIzinId(Long)}. Namun pada kolom {@code keteranganKonfirmasi},
	 * slot 4 berisi <b>id DOSEN yang mengonfirmasi</b>. Karena method ini mencocokkan slot 4
	 * dengan {@code dosen.getId()} lalu mengembalikan slot 4 itu juga, hasilnya SELALU sama
	 * dengan {@code dosen.getId()} bila barisnya ketemu, dan {@code -1L} bila tidak.</p>
	 *
	 * <p>Dengan kata lain method ini praktis hanya berguna sebagai pemeriksaan "adakah baris
	 * konfirmasi dari dosen ini untuk peserta ini"; nilainya bukan id pengajuan izin.</p>
	 *
	 * @param ref   id peserta yang dicari
	 * @param dosen dosen yang mengonfirmasi
	 * @return id dosen bila barisnya ada, atau {@code -1L}
	 */
	public Long retreivePengajuanIzinIdKonfirmasi(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganKonfirmasi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return Long.parseLong(s[4]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5013");

				}
			}
		}

		return -1L;
	}

	/**
	 * Ambil KODE status kehadiran hasil konfirmasi (slot 2) untuk pasangan (ref, dosen).
	 *
	 * @param ref   id peserta yang dicari
	 * @param dosen dosen yang mengonfirmasi
	 * @return kode status, atau {@code "-"} bila belum dikonfirmasi dosen itu
	 * @see #retreiveAbsensiIdKonfirmasi(Long, Dosen)
	 * @see #retreiveAbsensiKode(Long)
	 */
	public String retreiveAbsensiKodeKonfirmasi(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganKonfirmasi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5034");

				}
			}
		}

		return "-";
	}

	/**
	 * Ambil NAMA status kehadiran hasil konfirmasi (slot 3) untuk pasangan (ref, dosen).
	 *
	 * @param ref   id peserta yang dicari
	 * @param dosen dosen yang mengonfirmasi
	 * @return nama status, atau {@code "-"} bila belum dikonfirmasi dosen itu
	 * @see #retreiveAbsensiIdKonfirmasi(Long, Dosen)
	 */
	public String retreiveAbsensiNamaKonfirmasi(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganKonfirmasi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[3];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5055");

				}
			}
		}

		return "-";
	}

	/**
	 * Ambil KETERANGAN konfirmasi (slot 5) untuk pasangan (ref, dosen).
	 *
	 * @param ref   id peserta yang dicari
	 * @param dosen dosen yang mengonfirmasi
	 * @return keterangan, atau string kosong
	 * @see #retreiveAbsensiIdKonfirmasi(Long, Dosen)
	 */
	public String retreiveAbsensiKeteranganKonfirmasi(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganKonfirmasi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[5];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5076");

				}
			}
		}

		return "";
	}

	/**
	 * Ambil JAM MULAI kehadiran hasil konfirmasi (slot 6) untuk pasangan (ref, dosen).
	 *
	 * @param ref   id peserta yang dicari
	 * @param dosen dosen yang mengonfirmasi
	 * @return jam mulai, atau string kosong
	 * @see #retreiveAbsensiIdKonfirmasi(Long, Dosen)
	 */
	public String retreiveAbsensiMulaiKonfirmasi(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganKonfirmasi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[6];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5097");

				}
			}
		}

		return "";
	}

	/**
	 * Ambil JAM SELESAI kehadiran hasil konfirmasi (slot 7) untuk pasangan (ref, dosen).
	 *
	 * @param ref   id peserta yang dicari
	 * @param dosen dosen yang mengonfirmasi
	 * @return jam selesai, atau string kosong
	 * @see #retreiveAbsensiIdKonfirmasi(Long, Dosen)
	 */
	public String retreiveAbsensiSampaiKonfirmasi(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganKonfirmasi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[7];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5118");

				}
			}
		}

		return "";
	}

	/**
	 * Ambil JENIS peserta pada baris konfirmasi (slot 8) untuk pasangan (ref, dosen).
	 *
	 * @param ref   id peserta yang dicari
	 * @param dosen dosen yang mengonfirmasi
	 * @return jenis peserta, atau string kosong
	 * @see #retreiveAbsensiIdKonfirmasi(Long, Dosen)
	 */
	public String retreiveAbsensiJenisKonfirmasi(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganKonfirmasi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[8];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5139");

				}
			}
		}

		return "";
	}

	/**
	 * Id dosen penanggung jawab pertemuan ini.
	 *
	 * <p><b>Getter ini mengubah keadaan objek dan dapat menjalankan query.</b> Bila belum diisi,
	 * nilainya diambil dari dosen PERTAMA hasil {@link #ambilDosenId()} lalu ditulis ke field
	 * sehingga ikut tersimpan. Karena {@link #ambilDosenId()} menempuh rantai induk dan
	 * menjalankan query, membaca properti ini pada pertemuan yang belum punya penanggung jawab
	 * tidaklah murah.</p>
	 *
	 * <p>Berbeda dari {@link #dosenUtama()} yang mengembalikan objek {@link Dosen} dan TIDAK
	 * menyimpan apa pun, nilai di sini menjadi permanen begitu terhitung — pergantian dosen
	 * pengampu di kemudian hari tidak lagi mengubahnya.</p>
	 *
	 * @return id dosen penanggung jawab, atau {@code null} bila tidak ada dosen sama sekali
	 * @see #dosenUtama()
	 * @see #ambilDosenId()
	 */
	public Long getPjDosen() {
		if (pjDosen == null) {
			List<Long> dosens = ambilDosenId();
			if (!dosens.isEmpty()) {
				pjDosen = dosens.get(0);
			}
		}
		return pjDosen;
	}

	/**
	 * Setel id dosen penanggung jawab pertemuan ini.
	 *
	 * @param pjDosen id dosen penanggung jawab
	 * @see #getPjDosen()
	 */
	public void setPjDosen(Long pjDosen) {
		this.pjDosen = pjDosen;
	}

	/**
	 * Jadwal ujian penerimaan pegawai yang menjadi induk pertemuan ini.
	 *
	 * @return {@link JadwalUjianPegawai} induk, atau {@code null}
	 * @see #untuk()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jadwal_ujian_pegawai", nullable = true)
	public JadwalUjianPegawai getJadwalUjianPegawai() {
		return jadwalUjianPegawai;
	}

	/**
	 * Tetapkan jadwal ujian pegawai sebagai induk pertemuan ini.
	 *
	 * @param jadwalUjianPegawai jadwal ujian pegawai; boleh {@code null}
	 */
	public void setJadwalUjianPegawai(JadwalUjianPegawai jadwalUjianPegawai) {
		this.jadwalUjianPegawai = jadwalUjianPegawai;
	}

	/**
	 * Komponen produk kursus yang menjadi induk pertemuan ini.
	 *
	 * @return {@link KomponenDataProdukKursus} induk, atau {@code null}
	 * @see #untuk()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "komponen_data_produk_kursus", nullable = true)
	public KomponenDataProdukKursus getKomponenDataProdukKursus() {
		komponenDataProdukKursus = check(komponenDataProdukKursus);
		return komponenDataProdukKursus;
	}

	/**
	 * Tetapkan komponen produk kursus sebagai induk pertemuan ini.
	 *
	 * @param komponenDataProdukKursus komponen produk kursus; boleh {@code null}
	 */
	public void setKomponenDataProdukKursus(KomponenDataProdukKursus komponenDataProdukKursus) {
		this.komponenDataProdukKursus = komponenDataProdukKursus;
	}

	/**
	 * Jadwal pertemuan penerimaan siswa baru yang menjadi induk pertemuan ini.
	 *
	 * <p>Salah satu jenis induk yang dipakai {@link #info()}, {@link #getSekolahId()}, dan
	 * {@link #ambilVOPembelajaran()} tetapi TIDAK punya cabang di {@link #untuk()}.</p>
	 *
	 * @return {@link JadwalPertemuanPSB} induk, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jadwal_pertemuan_psb", nullable = true)
	public JadwalPertemuanPSB getJadwalPertemuanPSB() {
		return jadwalPertemuanPSB;
	}

	/**
	 * Tetapkan jadwal pertemuan PSB sebagai induk pertemuan ini.
	 *
	 * @param jadwalPertemuanPSB jadwal pertemuan PSB; boleh {@code null}
	 */
	public void setJadwalPertemuanPSB(JadwalPertemuanPSB jadwalPertemuanPSB) {
		this.jadwalPertemuanPSB = jadwalPertemuanPSB;
	}

	/**
	 * Titik lokasi geografis tempat pertemuan berlangsung.
	 *
	 * <p>Berbeda dari {@link #getRuang()} yang menunjuk ruang kelas, ini koordinat yang dipakai
	 * untuk memverifikasi absensi berbasis lokasi: peserta harus berada dalam radius
	 * {@link #getJarak()} dari titik ini.</p>
	 *
	 * @return lokasi pertemuan, atau {@code null} bila tanpa pembatasan lokasi
	 * @see #getJarak()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	/**
	 * Setel titik lokasi geografis pertemuan.
	 *
	 * @param lokasi lokasi; boleh {@code null}
	 * @see #getLokasi()
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Radius toleransi absensi berbasis lokasi, di sekitar {@link #getLokasi()}.
	 *
	 * <p>Nilai bawaannya {@code 1.0}. Satuannya tidak dinyatakan di kelas ini — pemakai
	 * ({@code AbsensiHelper} dan sejenisnya) yang menetapkan artinya, jadi periksa di sana sebelum
	 * mengandalkan angkanya.</p>
	 *
	 * @return radius toleransi; tidak pernah {@code null}
	 * @see #getLokasi()
	 */
	public Double getJarak() {
		return jarak == null ? 1.0 : jarak;
	}

	/**
	 * Setel radius toleransi absensi berbasis lokasi.
	 *
	 * @param jarak radius toleransi; {@code null} berarti kembali ke bawaan {@code 1.0}
	 * @see #getJarak()
	 */
	public void setJarak(Double jarak) {
		this.jarak = jarak;
	}

	/**
	 * Jenis item penilaian siswa yang dikaitkan dengan pertemuan ini (jalur sekolah).
	 *
	 * <p>Bersama {@link #getGrupKategoriItemPenilaianSiswa()} dan {@link #getGrupPenilaian()}
	 * menempatkan pertemuan ini di dalam struktur penilaian rapor sekolah.</p>
	 *
	 * @return jenis item penilaian, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_item_penilaian_siswa", nullable = true)
	public JenisItemPenilaianSiswa getJenisItemPenilaianSiswa() {
		jenisItemPenilaianSiswa = check(jenisItemPenilaianSiswa);
		return jenisItemPenilaianSiswa;
	}

	/**
	 * Setel jenis item penilaian siswa pertemuan ini.
	 *
	 * @param jenisItemPenilaianSiswa jenis item penilaian; boleh {@code null}
	 */
	public void setJenisItemPenilaianSiswa(JenisItemPenilaianSiswa jenisItemPenilaianSiswa) {
		this.jenisItemPenilaianSiswa = jenisItemPenilaianSiswa;
	}

	/**
	 * Apakah pertemuan ini masih aktif?
	 *
	 * <p>Penanda penghapusan lunak: pertemuan yang tidak aktif biasanya disembunyikan dari daftar
	 * alih-alih dihapus dari basis data. Nilai bawaannya {@code true}, sehingga data lama yang
	 * belum pernah diisi tetap dianggap aktif.</p>
	 *
	 * @return {@code true} bila pertemuan aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Setel status aktif pertemuan ini.
	 *
	 * @param aktif {@code false} untuk menyembunyikan pertemuan
	 * @see #getAktif()
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Grup kategori item penilaian siswa yang dikaitkan dengan pertemuan ini (jalur sekolah).
	 *
	 * @return grup kategori item penilaian, atau {@code null}
	 * @see #getJenisItemPenilaianSiswa()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_kategori_item_penilaian_siswa", nullable = true)
	public GrupKategoriItemPenilaianSiswa getGrupKategoriItemPenilaianSiswa() {
		grupKategoriItemPenilaianSiswa = check(grupKategoriItemPenilaianSiswa);
		return grupKategoriItemPenilaianSiswa;
	}

	/**
	 * Setel grup kategori item penilaian siswa pertemuan ini.
	 *
	 * @param grupKategoriItemPenilaianSiswa grup kategori; boleh {@code null}
	 */
	public void setGrupKategoriItemPenilaianSiswa(GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa) {
		this.grupKategoriItemPenilaianSiswa = grupKategoriItemPenilaianSiswa;
	}

	/**
	 * Grup penilaian yang dikaitkan dengan pertemuan ini (jalur sekolah).
	 *
	 * @return grup penilaian, atau {@code null}
	 * @see #getJenisItemPenilaianSiswa()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_penilaian", nullable = true)
	public GrupPenilaian getGrupPenilaian() {
		grupPenilaian = check(grupPenilaian);
		return grupPenilaian;
	}

	/**
	 * Setel grup penilaian pertemuan ini.
	 *
	 * @param grupPenilaian grup penilaian; boleh {@code null}
	 */
	public void setGrupPenilaian(GrupPenilaian grupPenilaian) {
		this.grupPenilaian = grupPenilaian;
	}

	/**
	 * Id petugas keempat yang ditugaskan pada pertemuan ini.
	 *
	 * <p>Terpisah jauh dari {@link #getPetugas()}..{@link #getPetugas3()} di dalam berkas karena
	 * ditambahkan belakangan, tetapi perannya sama.</p>
	 *
	 * @return id petugas keempat, atau {@code null}
	 * @see #getPetugas()
	 */
	public Long getPetugas4() {
		return petugas4;
	}

	/**
	 * Setel id petugas keempat.
	 *
	 * @param petugas4 id petugas keempat
	 */
	public void setPetugas4(Long petugas4) {
		this.petugas4 = petugas4;
	}

	/**
	 * Kelas les siswa yang menjadi induk pertemuan ini.
	 *
	 * <p>Salah satu jenis induk yang dikenali {@link #untuk()}, {@link #toString()}, dan
	 * {@link #ambilVOPembelajaran()}, tetapi TIDAK punya cabang di {@link #warna()} maupun
	 * {@link #info()} — sehingga pertemuan les tampil dengan warna cadangan dan keterangan
	 * {@code "Konsultasi lain"}.</p>
	 *
	 * @return {@link KelasLesSiswa} induk, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_les_siswa", nullable = true)
	public KelasLesSiswa getKelasLesSiswa() {
		kelasLesSiswa = check(kelasLesSiswa);
		return kelasLesSiswa;
	}

	public void setKelasLesSiswa(KelasLesSiswa kelasLesSiswa) {
		this.kelasLesSiswa = kelasLesSiswa;
	}

	@Column(columnDefinition = "text")
	public String getKeteranganSesuaiDenganRps() {
		return keteranganSesuaiDenganRps == null ? "" : keteranganSesuaiDenganRps.trim();
	}

	public void setKeteranganSesuaiDenganRps(String keteranganSesuaiDenganRps) {
		this.keteranganSesuaiDenganRps = keteranganSesuaiDenganRps;
	}

	public void populateKonfirmasiRps(Long ref, Long status, String mulai, String sampai, String jenis, Dosen dosen) {
		populateKonfirmasiRps(ref, status, null, mulai, sampai, jenis, dosen);
	}

	public void populateKonfirmasiRps(Long ref, Long status, String keterangan, String mulai, String sampai,
			String jenis, Dosen dosen) {
		if (ref != null && status != null) {

			String nama = "Belum Ditentukan";
			if (status.equals(1L)) {
				nama = "Sesuai";
			}
			if (status.equals(2L)) {
				nama = "Tidak Sesuai";
			}

			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ";", "..\n");
			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ",", "_");
			String formatBaru = "";
			String[] nilais = getKeteranganSesuaiDenganRps().split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = nn.split(",");
					if (!s[0].trim().isEmpty()) {
						Long formatId = Long.parseLong(s[0]);
						Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
						if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
							aformatBaru = ref + "," + status + "," + status + "," + nama + "," + dosen.getId() + ","
									+ (keterangan == null ? retreiveAbsensiKeteranganSesuaiDenganRps(ref, dosen)
											: keterangan)
									+ "," + (mulai == null ? retreiveAbsensiMulaiKonfirmasiRps(ref, dosen) : mulai)
									+ "," + (sampai == null ? retreiveAbsensiSampaiKonfirmasiRps(ref, dosen) : sampai)
									+ "," + (jenis == null ? retreiveAbsensiJenisKonfirmasiRps(ref, dosen) : jenis);
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!ada) {
				String aformatBaru = ref + "," + status + "," + status + "," + nama + "," + dosen.getId() + ","
						+ (keterangan == null ? retreiveAbsensiKeteranganSesuaiDenganRps(ref, dosen) : keterangan) + ","
						+ (mulai == null ? retreiveAbsensiMulaiKonfirmasiRps(ref, dosen) : mulai) + ","
						+ (sampai == null ? retreiveAbsensiSampaiKonfirmasiRps(ref, dosen) : sampai) + ","
						+ (jenis == null ? retreiveAbsensiJenisKonfirmasiRps(ref, dosen) : jenis);
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

//			System.out.println("formatBaru => " + formatBaru);

			keteranganSesuaiDenganRps = formatBaru;
		}
	}

	public Long retreiveAbsensiIdKonfirmasiRps(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiDenganRps().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return Long.parseLong(s[1]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5358");

				}
			}
		}

		return -1L;
	}

	public Long retreivePengajuanIzinIdKonfirmasiRps(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiDenganRps().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return Long.parseLong(s[4]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5379");

				}
			}
		}

		return -1L;
	}

	public String retreiveAbsensiKodeKonfirmasiRps(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiDenganRps().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5400");

				}
			}
		}

		return "-";
	}

	public String retreiveAbsensiNamaKonfirmasiRps(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiDenganRps().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[3];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5421");

				}
			}
		}

		return "-";
	}

	public String retreiveAbsensiKeteranganSesuaiDenganRps(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiDenganRps().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[5];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5442");

				}
			}
		}

		return "";
	}

	public String retreiveAbsensiMulaiKonfirmasiRps(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiDenganRps().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[6];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5463");

				}
			}
		}

		return "";
	}

	public String retreiveAbsensiSampaiKonfirmasiRps(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiDenganRps().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[7];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5484");

				}
			}
		}

		return "";
	}

	public String retreiveAbsensiJenisKonfirmasiRps(Long ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiDenganRps().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length == 0 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[8];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5505");

				}
			}
		}

		return "";
	}

	public Boolean getSesuai() {
		return sesuai == null ? false : sesuai;
	}

	public void setSesuai(Boolean sesuai) {
		this.sesuai = sesuai;
	}

	public Integer getPertemuanManual() {
		VOPembelajaran pembelajaran = ambilVOPembelajaran();
		if (pembelajaran != null && pembelajaran.getUrutkanotomatis()) {
			pertemuanManual = getPertemuanKe();
		}
		return pertemuanManual == null ? (pertemuanKe == null ? 1 : pertemuanKe) : pertemuanManual;
	}

	public void setPertemuanManual(Integer pertemuanManual) {
		this.pertemuanManual = pertemuanManual;
	}

	@Column(columnDefinition = "text")
	public String getKeteranganSesuaiOlehAkademik() {
		return keteranganSesuaiOlehAkademik == null ? "" : keteranganSesuaiOlehAkademik.trim();
	}

	public void setKeteranganSesuaiOlehAkademik(String keteranganSesuaiOlehAkademik) {
		this.keteranganSesuaiOlehAkademik = keteranganSesuaiOlehAkademik;
	}

	public void populateOlehAkademik(String ref, Long status, String mulai, String sampai, String jenis, Dosen dosen) {
		populateOlehAkademik(ref, status, null, mulai, sampai, jenis, dosen);
	}

	public void populateOlehAkademik(String ref, Long status, String keterangan, String mulai, String sampai,
			String jenis, Dosen dosen) {
		if (ref != null && status != null) {

			String nama = "Belum Ditentukan";
			if (status.equals(1L)) {
				nama = "Sesuai";
			}
			if (status.equals(2L)) {
				nama = "Tidak Sesuai";
			}

			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ";", "..\n");
			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ",", "_");
			String formatBaru = "";
			String[] nilais = getKeteranganSesuaiOlehAkademik().split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = nn.split(",");
					if (!s[0].trim().isEmpty()) {
						String formatId = (s[0]);
						Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
						if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
							aformatBaru = ref + "," + status + "," + status + "," + nama + "," + dosen.getId() + ","
									+ (keterangan == null ? retreiveAbsensiKeteranganSesuaiOlehAkademik(ref, dosen)
											: keterangan)
									+ "," + (mulai == null ? retreiveAbsensiMulaiOlehAkademik(ref, dosen) : mulai) + ","
									+ (sampai == null ? retreiveAbsensiSampaiOlehAkademik(ref, dosen) : sampai) + ","
									+ (jenis == null ? retreiveAbsensiJenisOlehAkademik(ref, dosen) : jenis);
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!ada) {
				String aformatBaru = ref + "," + status + "," + status + "," + nama + "," + dosen.getId() + ","
						+ (keterangan == null ? retreiveAbsensiKeteranganSesuaiOlehAkademik(ref, dosen) : keterangan)
						+ "," + (mulai == null ? retreiveAbsensiMulaiOlehAkademik(ref, dosen) : mulai) + ","
						+ (sampai == null ? retreiveAbsensiSampaiOlehAkademik(ref, dosen) : sampai) + ","
						+ (jenis == null ? retreiveAbsensiJenisOlehAkademik(ref, dosen) : jenis);
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

//			System.out.println("formatBaru => " + formatBaru);

			keteranganSesuaiOlehAkademik = formatBaru;
		}
	}

	public Long retreiveAbsensiIdOlehAkademik(String ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiOlehAkademik().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length < 5) continue;
					String formatId = (s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return Long.parseLong(s[1]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5618");

				}
			}
		}

		return -1L;
	}

	public Long retreivePengajuanIzinIdOlehAkademik(String ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiOlehAkademik().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length < 5) continue;
					String formatId = (s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return Long.parseLong(s[4]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5639");

				}
			}
		}

		return -1L;
	}

	public String retreiveAbsensiKodeOlehAkademik(String ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiOlehAkademik().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length < 5) continue;
					String formatId = (s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5660");

				}
			}
		}

		return "-";
	}

	public String retreiveAbsensiNamaOlehAkademik(String ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiOlehAkademik().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					if (s.length < 5) continue;
					String formatId = (s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[3];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5681");

				}
			}
		}

		return "-";
	}

	public String retreiveAbsensiKeteranganSesuaiOlehAkademik(String ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiOlehAkademik().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length < 6) continue;
					String formatId = (s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[5];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5702");

				}
			}
		}

		return "";
	}

	public String retreiveAbsensiMulaiOlehAkademik(String ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiOlehAkademik().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length < 7) continue;
					String formatId = (s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[6];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5723");

				}
			}
		}

		return "";
	}

	public String retreiveAbsensiSampaiOlehAkademik(String ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiOlehAkademik().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length < 8) continue;
					String formatId = (s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[7];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5744");

				}
			}
		}

		return "";
	}

	public String retreiveAbsensiJenisOlehAkademik(String ref, Dosen dosen) {

		if (ref != null) {
			String[] nilais = getKeteranganSesuaiOlehAkademik().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					if (s.length < 9) continue;
					String formatId = (s[0]);
					Long dsn = s[4].isEmpty() ? -1L : Long.parseLong(s[4]);
					if (ref.equals(formatId) && dsn.equals(dosen.getId())) {
						return s[8];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5765");

				}
			}
		}

		return "";
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalEdit() {
		return tanggalEdit;
	}

	public void setTanggalEdit(Date tanggalEdit) {
		this.tanggalEdit = tanggalEdit;
	}

	public static String DEFAULT_FORMULA = new JSONObject().toString();

	@Column(columnDefinition = "text", name = "keterangannilai")
	public String getKeteranganNilaiLama() {
		return keteranganNilaiLama == null || keteranganNilaiLama.trim().isEmpty() ? DEFAULT_FORMULA
				: keteranganNilaiLama;
	}

	public void setKeteranganNilaiLama(String keteranganNilaiLama) {
		this.keteranganNilaiLama = keteranganNilaiLama;
	}

	@Override
	@Column(columnDefinition = "text", name = "keterangan_nilai_baru")
	public String getKeteranganNilai() {
		return keteranganNilai == null || keteranganNilai.trim().isEmpty() ? getKeteranganNilaiLama() : keteranganNilai;
	}

	@Override
	public void setKeteranganNilai(String keteranganNilai) {
		this.keteranganNilai = keteranganNilai;
	}

	@Column(columnDefinition = "text")
	public String getFormatNilais() {
		return formatNilais == null || formatNilais.trim().isEmpty() ? JSON : formatNilais;
	}

	public void setFormatNilais(String formatNilais) {
		this.formatNilais = formatNilais;
	}

	public String getTa() {
		try {
			if (getPerkuliahan() != null) {
				ta = getPerkuliahan().getTahunAjaran();
			} else if (getJadwalPelajaran() != null) {
				ta = getJadwalPelajaran().getTahunAjaran();
			} else if (getJadwalUjianPMB() != null && getJadwalUjianPMB().getUjianPMB() != null) {
				ta = getJadwalUjianPMB().getUjianPMB().getTahunAkademik();
			} else if (getMahasiswaRequestTugasAkhir() != null) {
				ta = getMahasiswaRequestTugasAkhir().getTahunAkademik();
			} else if (getSkripsi() != null) {
				ta = getSkripsi().getTahunAkademik();
			} else if (getKelompokKkn() != null && getKelompokKkn().getKkn() != null) {
				ta = getKelompokKkn().getKkn().getTahunAkademik();
			} else if (getKelompokPkl() != null && getKelompokPkl().getPkl() != null) {
				ta = getKelompokPkl().getPkl().getTahunAkademik();
			} else if (getKrsMahasiswa() != null) {
				ta = getKrsMahasiswa().getTahunAkademik();
			} else if (getJadwalUjianPSB() != null && getJadwalUjianPSB().getGelombangPendaftaranPsb() != null) {
				ta = getJadwalUjianPSB().getGelombangPendaftaranPsb().getTahunAjaran();
			} else if (getJadwalPertemuanPSB() != null
					&& getJadwalPertemuanPSB().getGelombangPendaftaranPsb() != null) {
				ta = getJadwalPertemuanPSB().getGelombangPendaftaranPsb().getTahunAjaran();
			} else if (getJadwalUjianPegawai() != null
					&& getJadwalUjianPegawai().getGelombangPendaftaranPegawai() != null) {
				ta = null;
			} else if (getKelasLesSiswa() != null) {
				ta = null;
			} else if (getPertemuanPunyaGrupPertemuan() != null
					&& getPertemuanPunyaGrupPertemuan().getGrupPertemuan() != null) {
				ta = getPertemuanPunyaGrupPertemuan().getGrupPertemuan().getTahunAkademik();
			} else if (getFormulirKegiatan() != null) {
				ta = getFormulirKegiatan().getTahunAkademik();
			} else if (getKomponenDataProdukKursus() != null) {
				ta = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5851");
			// TODO: handle exception
		}
		return ta;
	}

	public void setTa(String ta) {
		this.ta = ta;
	}

	public String getSmt() {
		try {
			if (getPerkuliahan() != null) {
				smt = getPerkuliahan().getGanjilGenap();
			} else if (getJadwalPelajaran() != null) {
				smt = getJadwalPelajaran().getSemester().equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
			} else if (getJadwalUjianPMB() != null && getJadwalUjianPMB().getUjianPMB() != null
					&& getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null) {
				smt = getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran().getJenisSemester();
			} else if (getMahasiswaRequestTugasAkhir() != null) {
				smt = getMahasiswaRequestTugasAkhir().getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
			} else if (getSkripsi() != null) {
				smt = getSkripsi().getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
			} else if (getKelompokKkn() != null && getKelompokKkn().getKkn() != null) {
				smt = getKelompokKkn().getKkn().getSemester();
			} else if (getKelompokPkl() != null && getKelompokPkl().getPkl() != null) {
				smt = getKelompokPkl().getPkl().getSemester();
			} else if (getKrsMahasiswa() != null) {
				smt = getKrsMahasiswa().getSemester() % 2 == 1 ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
			} else if (getJadwalUjianPSB() != null && getJadwalUjianPSB().getGelombangPendaftaranPsb() != null) {
				smt = Perkuliahan.GANJIL;
			} else if (getJadwalPertemuanPSB() != null
					&& getJadwalPertemuanPSB().getGelombangPendaftaranPsb() != null) {
				smt = Perkuliahan.GANJIL;
			} else if (getJadwalUjianPegawai() != null
					&& getJadwalUjianPegawai().getGelombangPendaftaranPegawai() != null) {
				smt = null;
			} else if (getKelasLesSiswa() != null) {
				smt = null;
			} else if (getPertemuanPunyaGrupPertemuan() != null
					&& getPertemuanPunyaGrupPertemuan().getGrupPertemuan() != null) {
				smt = getPertemuanPunyaGrupPertemuan().getGrupPertemuan().getJenisSemester();
			} else if (getFormulirKegiatan() != null) {
				smt = getFormulirKegiatan().getSemester();
			} else if (getKomponenDataProdukKursus() != null) {
				smt = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5898");
			// TODO: handle exception
		}
		return smt;
	}

	public void setSmt(String smt) {
		this.smt = smt;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		try {
			if (getPerkuliahan() != null) {
				jurusan = getPerkuliahan().getJurusan();
			} else if (getJadwalPelajaran() != null) {
				jurusan = null;
			} else if (getJadwalUjianPMB() != null && getJadwalUjianPMB().getUjianPMB() != null
					&& getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null) {
				jurusan = null;
			} else if (getMahasiswaRequestTugasAkhir() != null) {
				jurusan = getMahasiswaRequestTugasAkhir().getMahasiswa().getJurusan();
			} else if (getSkripsi() != null) {
				jurusan = getSkripsi().getMahasiswa().getJurusan();
			} else if (getKelompokKkn() != null && getKelompokKkn().getKkn() != null) {
				jurusan = getKelompokKkn().getKkn().getJurusan();
			} else if (getKelompokPkl() != null && getKelompokPkl().getPkl() != null) {
				jurusan = getKelompokPkl().getPkl().getJurusan();
			} else if (getKrsMahasiswa() != null) {
				jurusan = getKrsMahasiswa().getMahasiswa().getJurusan();
			} else if (getJadwalUjianPSB() != null && getJadwalUjianPSB().getGelombangPendaftaranPsb() != null) {
				jurusan = null;
			} else if (getJadwalPertemuanPSB() != null
					&& getJadwalPertemuanPSB().getGelombangPendaftaranPsb() != null) {
				jurusan = null;
			} else if (getJadwalUjianPegawai() != null
					&& getJadwalUjianPegawai().getGelombangPendaftaranPegawai() != null) {
				jurusan = null;
			} else if (getKelasLesSiswa() != null) {
				jurusan = null;
			} else if (getPertemuanPunyaGrupPertemuan() != null
					&& getPertemuanPunyaGrupPertemuan().getGrupPertemuan() != null) {
				jurusan = null;
			} else if (getFormulirKegiatan() != null) {
				jurusan = getFormulirKegiatan().getJurusan();
			} else if (getKomponenDataProdukKursus() != null) {
				jurusan = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5947");
			// TODO: handle exception
		}
		return jurusan;
	}

	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		try {
			if (getJadwalPertemuanPSB() != null && getJadwalPertemuanPSB().getGelombangPendaftaranPsb() != null) {
				sekolah = getJadwalPertemuanPSB().getGelombangPendaftaranPsb().getSekolah();
			} else if (getJadwalPelajaran() != null) {
				sekolah = getJadwalPelajaran().getSekolah();
			} else if (getFormulirKegiatan() != null) {
				sekolah = getFormulirKegiatan().getSekolah();
			} else {
				sekolah = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:5970");
			// TODO: handle exception
		}
		return sekolah;
	}

	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah;
	}

	@Column(columnDefinition = "text")
	public String getDosens() {
		try {
			dosens = ",";
			if (getPerkuliahan() != null) {
				for (Long id : getPerkuliahan().populateDosenBuId()) {
					dosens += id + ",";
				}
			} else if (getJadwalPelajaran() != null) {
				dosens = null;
			} else if (getJadwalUjianPMB() != null && getJadwalUjianPMB().getUjianPMB() != null
					&& getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null) {
				dosens = null;
			} else if (getMahasiswaRequestTugasAkhir() != null) {
				for (Long id : getMahasiswaRequestTugasAkhir().populateDosenBuId()) {
					dosens += id + ",";
				}
			} else if (getSkripsi() != null) {
				for (Long id : getSkripsi().populateDosenBuId()) {
					dosens += id + ",";
				}
			} else if (getKelompokKkn() != null && getKelompokKkn().getKkn() != null) {
				for (Long id : getKelompokKkn().populateDosenBuId()) {
					dosens += id + ",";
				}
			} else if (getKelompokPkl() != null && getKelompokPkl().getPkl() != null) {
				for (Long id : getKelompokPkl().populateDosenBuId()) {
					dosens += id + ",";
				}
			} else if (getKrsMahasiswa() != null) {
				dosens = "," + (getKrsMahasiswa().getDosenPa() == null ? "" : getKrsMahasiswa().getDosenPa().getId())
						+ ",";
			} else if (getJadwalUjianPSB() != null && getJadwalUjianPSB().getGelombangPendaftaranPsb() != null) {
				dosens = null;
			} else if (getJadwalPertemuanPSB() != null
					&& getJadwalPertemuanPSB().getGelombangPendaftaranPsb() != null) {
				dosens = null;
			} else if (getJadwalUjianPegawai() != null
					&& getJadwalUjianPegawai().getGelombangPendaftaranPegawai() != null) {
				dosens = null;
			} else if (getKelasLesSiswa() != null) {
				dosens = null;
			} else if (getPertemuanPunyaGrupPertemuan() != null
					&& getPertemuanPunyaGrupPertemuan().getGrupPertemuan() != null) {
				for (Long id : getPertemuanPunyaGrupPertemuan().populateDosenBuId()) {
					dosens += id + ",";
				}
			} else if (getFormulirKegiatan() != null) {
				for (Long id : getFormulirKegiatan().populateDosenBuId()) {
					dosens += id + ",";
				}
			} else if (getKomponenDataProdukKursus() != null) {
				dosens = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:6034");
			// TODO: handle exception
		}
		return dosens;
	}

	public void setDosens(String dosens) {
		this.dosens = dosens;
	}

	@Column(columnDefinition = "text")
	public String getMahasiswas() {
		if (sedangHitungMahasiswas) {
			/* Dipanggil ulang oleh flush Hibernate di tengah perhitungan:
			 * kembalikan nilai apa adanya, jangan rekursi. */
			return mahasiswas;
		}
		sedangHitungMahasiswas = true;
		try {
			mahasiswas = ",";
			if (getPerkuliahan() != null) {
				for (Long id : getPerkuliahan().ambilMahasiswaById()) {
					mahasiswas += id + ",";
				}
			} else if (getJadwalPelajaran() != null) {
				mahasiswas = null;
			} else if (getJadwalUjianPMB() != null && getJadwalUjianPMB().getUjianPMB() != null
					&& getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null) {
				mahasiswas = null;
			} else if (getMahasiswaRequestTugasAkhir() != null) {
				mahasiswas = "," + getMahasiswaRequestTugasAkhir().getMahasiswa().getId() + ",";
			} else if (getSkripsi() != null) {
				mahasiswas = "," + getSkripsi().getMahasiswa().getId() + ",";
			} else if (getKelompokKkn() != null && getKelompokKkn().getKkn() != null) {
				for (Long id : getKelompokKkn().ambilMahasiswaById()) {
					mahasiswas += id + ",";
				}
			} else if (getKelompokPkl() != null && getKelompokPkl().getPkl() != null) {
				for (Long id : getKelompokPkl().ambilMahasiswaById()) {
					mahasiswas += id + ",";
				}
			} else if (getKrsMahasiswa() != null) {
				mahasiswas = "," + getKrsMahasiswa().getMahasiswa().getId() + ",";
			} else if (getJadwalUjianPSB() != null && getJadwalUjianPSB().getGelombangPendaftaranPsb() != null) {
				mahasiswas = null;
			} else if (getJadwalPertemuanPSB() != null
					&& getJadwalPertemuanPSB().getGelombangPendaftaranPsb() != null) {
				mahasiswas = null;
			} else if (getJadwalUjianPegawai() != null
					&& getJadwalUjianPegawai().getGelombangPendaftaranPegawai() != null) {
				mahasiswas = null;
			} else if (getKelasLesSiswa() != null) {
				mahasiswas = null;
			} else if (getPertemuanPunyaGrupPertemuan() != null
					&& getPertemuanPunyaGrupPertemuan().getGrupPertemuan() != null) {
				for (Long id : getPertemuanPunyaGrupPertemuan().ambilMahasiswaById()) {
					mahasiswas += id + ",";
				}
			} else if (getFormulirKegiatan() != null) {
				for (Long id : getFormulirKegiatan().ambilMahasiswaById()) {
					mahasiswas += id + ",";
				}
			} else if (getKomponenDataProdukKursus() != null) {
				mahasiswas = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:6099");
			// TODO: handle exception
		} finally {
			sedangHitungMahasiswas = false;
		}
		return mahasiswas;
	}

	public void setMahasiswas(String mahasiswas) {
		this.mahasiswas = mahasiswas;
	}

	@Column(columnDefinition = "text")
	public String getGurus() {
		try {
			gurus = ",";
			if (getJadwalPelajaran() != null) {
				for (Long id : getJadwalPelajaran().populateGuruBuId()) {
					gurus += id + ",";
				}
			} else if (getFormulirKegiatan() != null) {
				for (Long id : getFormulirKegiatan().populateGuruBuId()) {
					gurus += id + ",";
				}
			} else {
				gurus = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:6126");
			// TODO: handle exception
		}
		return gurus;
	}

	public void setGurus(String gurus) {
		this.gurus = gurus;
	}

	@Column(columnDefinition = "text")
	public String getSiswas() {
		try {
			siswas = ",";
			if (getJadwalPelajaran() != null) {
				for (Long id : getJadwalPelajaran().ambilSiswaById()) {
					siswas += id + ",";
				}
			} else if (getFormulirKegiatan() != null) {
				for (Long id : getFormulirKegiatan().ambilSiswaById()) {
					siswas += id + ",";
				}
			} else {
				siswas = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Pertemuan.java:6151");
			// TODO: handle exception
		}
		return siswas;
	}

	public void setSiswas(String siswas) {
		this.siswas = siswas;
	}

	@Column(name = "sudah_diproses")
	public Boolean getSudahDiproses() {
		return sudahDiproses == null ? true : sudahDiproses;
	}

	public void setSudahDiproses(Boolean sudahDiproses) {
		this.sudahDiproses = sudahDiproses;
	}
}
