package ais.database.model;

// Generated Dec 28, 2009 5:57:44 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.KonfigurasiPromptHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.MyMessageboxConfig;

/**
 * Entity tugas akhir / skripsi milik <b>satu</b> mahasiswa (tabel {@code public.skripsi}).
 *
 * <p>Satu baris {@code Skripsi} mewakili keseluruhan riwayat satu tugas akhir: judul yang
 * disetujui, susunan dosen pembimbing dan penguji, jadwal seminar dan sidang, rincian nilai
 * dari tiap dosen, sampai nilai huruf dan status kelulusan. Class ini dihasilkan semula oleh
 * hbm2java, namun sudah lama berkembang menjadi tempat menaruh logika bisnis tugas akhir —
 * banyak getter di sini <b>bukan</b> getter polos, melainkan menghitung, menormalkan, bahkan
 * mengubah state (lihat bagian "Getter yang berefek samping" di bawah).</p>
 *
 * <h3>Posisi dalam hierarki</h3>
 * <pre>
 * GeneralValueObject
 *   └─ ais.database.model.sop.DataSop      (integrasi SOP/disposisi)
 *        └─ VoKunci                        (kontrak penguncian baris, {@code dikunci})
 *             └─ VOPembelajaran            (kontrak "sesi pembelajaran": course, urutkanotomatis, dsb.)
 *                  └─ Skripsi              (class ini)
 * </pre>
 * Selain itu {@code Skripsi} meng-implement {@link VOPesertaPembelajaran}, sehingga sebuah
 * skripsi bisa diperlakukan sekaligus sebagai "peserta pembelajaran" yang mengacu pada dirinya
 * sendiri (lihat {@link #ambilVOPembelajaran()}). Kontrak umum {@code id}/{@code equals}/
 * {@code compareTo}/{@link GeneralValueObject#check(Object)} diwarisi apa adanya dari
 * {@link ais.database.model.GeneralValueObject} — jangan diduplikasi penjelasannya di sini.
 *
 * <h3>Alur hidup satu tugas akhir</h3>
 * <ol>
 * <li><b>Pengajuan judul / proposal</b> — mahasiswa mengisi {@link MahasiswaRequestTugasAkhir}
 * (entity terpisah, punya siklus penilaian proposal sendiri). Bila permintaan itu ditautkan ke
 * baris {@code Skripsi} ({@link #getMahasiswaRequestTugasAkhir()}), <b>data dosen pembimbing
 * dan tanggal seminar pada {@code Skripsi} akan dibayangi (di-override) oleh nilai dari
 * permintaan tersebut</b> — lihat {@link #getPembimbing()}, {@link #getKetuaSidang()},
 * {@link #getPembimbing3()}, {@link #getTanggalSeminar()}, {@link #getReferensi()}.</li>
 * <li><b>Penetapan pembimbing</b> — {@link #simpanDosen(Dosen, String)} mengisi slot dosen
 * berdasarkan <i>label</i> slot yang dikonfigurasi di {@link FormatNilaiSkripsi}. Batas beban
 * bimbingan per dosen per semester divalidasi oleh
 * {@link #checkMaksSksDosen(Dosen, String, String, Integer)}.</li>
 * <li><b>Bimbingan</b> — pertemuan bimbingan disimpan di tabel {@code pertemuan} (entity
 * {@link Pertemuan}) dengan kolom relasi {@code skripsi} terisi; rentangnya dicatat di
 * {@link #getAwalBimbingan()}/{@link #getAkhirBimbingan()} dan lamanya diturunkan oleh
 * {@link #getSelesaiDalamBulan()}. Riwayat revisi naskah ditangani class terpisah
 * {@code ais.action.master.helper.RevisiSkripsiHelper}.</li>
 * <li><b>Seminar proposal</b> — {@link #getTanggalSeminar()}.</li>
 * <li><b>Pendaftaran sidang</b> — mahasiswa masuk
 * {@link GelombangPendaftaranSidangTugasAkhir}, lalu dijadwalkan lewat
 * {@link JadwalSidangTugasAkhir} yang memasok tanggal dan ruang sidang
 * ({@link #getTanggalSidang()}, {@link #getRuangSidang()}).</li>
 * <li><b>Sidang &amp; penilaian</b> — tiap dosen mengisi nilai per komponen
 * ({@link KomponenPenilaianSkripsi}); nilainya <b>tidak</b> disimpan sebagai tabel anak,
 * melainkan dikemas jadi satu string CSV pada kolom {@code detail_nilai} (lihat "Format
 * {@code detailNilai}" di bawah). Nilai ringkas per slot dosen disimpan di kolom
 * {@code nilai_*} dan digabung berbobot oleh {@link #getTotalNilai()}.</li>
 * <li><b>Kelulusan</b> — {@link #getNilaiHuruf()} memetakan {@code totalNilai} ke huruf sesuai
 * aturan jurusan/fakultas/angkatan, lalu {@link #getLulus()} menyimpulkan lulus atau tidak.
 * {@link #getTelahSidang()} menandai sidang sudah berlangsung.</li>
 * <li><b>Konversi ke KHS &amp; pelaporan</b> — bila tugas akhir juga muncul sebagai mata kuliah
 * di kartu hasil studi, barisnya ditaut lewat {@link #getDetailperkuliahan()}; ekspor ke
 * PDDikti memakai {@link #getFeeder()} dan {@link #dataDosen(boolean)}.</li>
 * </ol>
 *
 * <h3>Delapan "slot" dosen dan penamaan kolom yang tertukar</h3>
 * <p>Susunan dosen tugas akhir bersifat konfigurabel: {@link FormatNilaiSkripsi} menyediakan
 * delapan slot ({@code dosen1}, {@code dosen2}, {@code dosen21}, {@code dosen3} .. {@code dosen7}),
 * masing-masing punya label bebas, kode, bendera aktif, dan persentase bobot nilai. Pemetaan
 * slot ke kolom di tabel {@code skripsi} adalah:</p>
 * <table border="1" summary="Pemetaan slot dosen ke kolom skripsi">
 * <tr><th>Slot pada FormatNilaiSkripsi</th><th>Label default</th><th>Kolom dosen</th><th>Kolom nilai</th></tr>
 * <tr><td>{@code dosen1}</td><td>Pembimbing I</td><td>{@code pembimbing}</td><td><b>{@code nilai_ketua_sidang}</b></td></tr>
 * <tr><td>{@code dosen2}</td><td>Pembimbing II</td><td>{@code ketua_sidang}</td><td><b>{@code nilai_pembimbing}</b></td></tr>
 * <tr><td>{@code dosen21}</td><td>Pembimbing III</td><td>{@code pembimbing3}</td><td>{@code nilai_pembimbing3}</td></tr>
 * <tr><td>{@code dosen3}</td><td>Penguji I</td><td>{@code penguji1}</td><td>{@code nilai_penguji1}</td></tr>
 * <tr><td>{@code dosen4}</td><td>Penguji II</td><td>{@code penguji2}</td><td>{@code nilai_penguji2}</td></tr>
 * <tr><td>{@code dosen5}</td><td>Penguji III</td><td>{@code penguji3}</td><td>{@code nilai_penguji_3}</td></tr>
 * <tr><td>{@code dosen6}</td><td>Penguji IV</td><td>{@code penguji4}</td><td>{@code nilai_penguji_4}</td></tr>
 * <tr><td>{@code dosen7}</td><td>Penguji V</td><td>{@code penguji5}</td><td>{@code nilai_penguji_5}</td></tr>
 * </table>
 * <p><b>PERHATIAN (jebakan lama, jangan "dirapikan" tanpa migrasi data):</b> khusus dua slot
 * pertama, kolom <i>orangnya</i> dan kolom <i>nilainya</i> tertukar penamaannya. Orang yang
 * mengisi slot 1 disimpan di kolom {@code pembimbing}, tetapi nilainya disimpan di kolom
 * {@code nilai_ketua_sidang}; sebaliknya orang slot 2 di kolom {@code ketua_sidang} dengan
 * nilai di {@code nilai_pembimbing}. Silang ini <i>konsisten</i> di seluruh aplikasi —
 * {@link #dataDosen(boolean)} dan {@link #simpanDosen(Dosen, String)} memakai kolom orang,
 * {@link #cariNilaiDariDosen(Dosen, String, Boolean)} dan {@link #getTotalNilai()} memakai
 * kolom nilai, dan {@code FormatNilaiSkripsi.getDosen1Aktif()} pun memakai
 * {@code prosentasiNilaiKetuaSidang} sebagai default — sehingga aplikasi menampilkan angka yang
 * benar. Yang salah hanya <b>namanya</b>. Query SQL langsung, laporan ad-hoc, atau integrasi
 * yang membaca tabel {@code skripsi} apa adanya akan salah baca bila mengasumsikan
 * {@code nilai_pembimbing} adalah nilai dari dosen di kolom {@code pembimbing}.</p>
 *
 * <h3>Format kolom {@code detailNilai}</h3>
 * <p>Rincian nilai per komponen per dosen disimpan sebagai satu string, bukan tabel anak.
 * Entri dipisah titik koma, token dalam entri dipisah koma:</p>
 * <pre>
 * &lt;idKomponenPenilaianSkripsi&gt;,&lt;idDosen&gt;,&lt;nilai&gt;,0,&lt;bobot&gt;,&lt;sudahDiverifikasi&gt;
 * </pre>
 * <p>Token ke-4 selalu ditulis konstan {@code 0} dan tidak pernah dibaca. Token ke-5
 * ({@code bobot} komponen) diperlakukan sebagai "persen" oleh
 * {@link #hitungTotalNilai(Boolean, Dosen, List)} dan dinormalkan terhadap jumlah seluruh bobot
 * milik dosen yang sama, jadi bobot tidak wajib berjumlah 100. Penulis/pembacanya:
 * {@link #populateDetailNilai(KomponenPenilaianSkripsi, Dosen, Double, Boolean)},
 * {@link #refreshNilaiKeDefault(Dosen)}, {@link #bersihkanNilaiKeDefault()},
 * {@link #retreiveDetailNilai(KomponenPenilaianSkripsi, Dosen)}, dan
 * {@link #retreiveDetailVerifikasiNilai(SkripsiPunyaKomponenPenilaianSkripsi, Dosen)}.</p>
 *
 * <h3>Getter yang berefek samping (jangan dianggap murni)</h3>
 * <ul>
 * <li>Banyak getter relasi memanggil {@link GeneralValueObject#check(Object)} yang, bila entity
 * sudah detached, <b>membuka session Hibernate sendiri</b> untuk memuat ulang proxy.</li>
 * <li>{@link #getMahasiswaRequestTugasAkhir()} <b>menulis balik ke entity lain</b>: ia memanggil
 * {@code setSkr(id)} pada {@link MahasiswaRequestTugasAkhir}, yang menyunting kolom JSON entity
 * tersebut sehingga baris {@code mahasiswa_request_tugas_akhir} ikut ter-{@code UPDATE} saat
 * flush — padahal pemanggil hanya "membaca".</li>
 * <li>{@link #getTotalNilai()}, {@link #getNilaiHuruf()}, {@link #getLulus()},
 * {@link #getTelahSidang()}, {@link #getSetujuiSidang()}, {@link #getSmt()},
 * {@link #getSemester()}, {@link #getTahunAkademik()}, {@link #getTahun()},
 * {@link #getWaktuSidang()}, {@link #getWaktuSampaiSidang()}, {@link #getCatatanPenting()},
 * {@link #getPersetujuanPenguji1()} dan saudara-saudaranya <b>menulis ke field</b> saat dibaca,
 * sehingga sekadar menampilkan entity di grid bisa membuat Hibernate menganggapnya kotor dan
 * menerbitkan {@code UPDATE}.</li>
 * <li>{@link #getPembimbing()}, {@link #getKetuaSidang()}, {@link #getPenguji1()} dan
 * seterusnya bisa <b>meng-null-kan</b> field dosen di memori bila slot yang bersangkutan
 * dinonaktifkan di {@link FormatNilaiSkripsi}. Bila entity lalu di-flush, relasi dosen itu
 * benar-benar hilang dari database.</li>
 * </ul>
 * <p>Tidak ditemukan pola "peta lokasi berkas JSON" ({@code ambilLokasi}/{@code tulisLokasi}/
 * {@code bersihkanLokasi}) seperti pada {@link Pertemuan} di class ini; lampiran berkas tugas
 * akhir dikelola di luar entity ini.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 * <li><b>Audit &amp; identitas</b>: {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}, {@link #getId()}.</li>
 * <li><b>Relasi</b>: mahasiswa, delapan slot dosen, ruang, jadwal sidang, gelombang
 * pendaftaran, format nilai, detail perkuliahan, disposisi SOP, {@link #getDikunci()}.</li>
 * <li><b>Naskah</b>: {@link #getJudul()}, {@link #getJudulen()}, {@link #getKeyword()},
 * {@link #getAbstrack()}, {@link #getReferensi()}.</li>
 * <li><b>Penilaian</b>: {@link #getTotalNilai()}, {@link #getNilaiHuruf()},
 * {@link #hitungTotalNilai(Boolean, Dosen, List)}, {@link #cariNilaiDariDosen(Dosen, String, Boolean)},
 * dan kelompok pengelola {@code detailNilai}.</li>
 * <li><b>Jadwal &amp; tahap</b>: seminar, sidang, waktu, {@link #getSetujuiSidang()},
 * {@link #getTelahSidang()}, persetujuan pembimbing/penguji.</li>
 * <li><b>Periode akademik</b>: {@link #getSemester()}, {@link #getTahunAkademik()},
 * {@link #getTahun()}, {@link #getSmt()}, plus implementasi kontrak {@code ambil*} dari
 * {@link VOPembelajaran}.</li>
 * <li><b>Utilitas statis</b>: hanya {@link #checkMaksSksDosen(Dosen, String, String, Integer)}.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see VOPembelajaran
 * @see Mahasiswa
 * @see MahasiswaRequestTugasAkhir
 * @see FormatNilaiSkripsi
 * @see JadwalSidangTugasAkhir
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "skripsi")
public class Skripsi extends VOPembelajaran implements VOPesertaPembelajaran {

	/**
	 *
	 */
	private static final long serialVersionUID = 7796386124639612695L;
	/** Primary key tabel {@code skripsi} (identity/serial di PostgreSQL). */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini (field audit). Perhatikan bahwa
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} <b>dideklarasikan ulang</b> di class ini
	 * meskipun {@link GeneralValueObject} sudah menyediakan konsep yang sama — pola "field audit
	 * yang di-shadow" yang berulang di banyak entity AIS. Akibatnya nilai audit yang dipakai
	 * adalah milik class ini, bukan milik superclass.
	 */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; lihat catatan shadowing pada {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris skripsi ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau string kosong
	 * <b>diabaikan diam-diam</b> (nilai lama dipertahankan) supaya jejak audit tidak terhapus oleh
	 * proses yang tidak mengetahui identitas pengguna.
	 *
	 * @param olehId id pengguna; diabaikan bila kosong/{@code null}
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * kosong/{@code null} diabaikan agar tidak menghapus jejak audit yang sudah ada.
	 *
	 * @param oleh nama pengguna; diabaikan bila kosong/{@code null}
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris skripsi ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini
	 * diterbitkan, dan meneruskan entity ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(Object)} untuk memutakhirkan
	 * stempel waktu/pengguna audit. Tidak untuk dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu server saat object dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code null}, sehingga baris baru selalu
	 * punya nilai walau belum pernah disimpan. Lihat catatan shadowing pada {@link #oleh}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; biasanya diisi otomatis oleh interceptor audit
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang baru dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berbentuk {@code "<id>-<judul>"}. Dipakai luas oleh komponen ZK
	 * (combobox/listbox) sebagai label default. Membaca field {@code judul} secara mentah, jadi
	 * bisa menghasilkan {@code "12-null"} bila judul belum diisi — sengaja tidak lewat
	 * {@link #getJudul()} agar {@code toString()} tetap bebas efek samping.
	 *
	 * @return gabungan id dan judul skripsi
	 */
	public String toString() {
		return id + "-" + judul;
	}

	/** Pengguna yang sedang mengunci baris ini; lihat kontrak {@code VoKunci}. */
	private Tbmuser dikunci;

	/**
	 * Mengembalikan pengguna yang mengunci baris skripsi ini (implementasi kontrak
	 * {@code VoKunci}). Selama kolom ini terisi, modul lain tidak boleh menyunting baris.
	 * Hasilnya dilewatkan {@link GeneralValueObject#check(Object)} sehingga proxy lazy yang sudah
	 * detached pun tetap terpakai — perhatikan bahwa {@code check} bisa membuka session Hibernate
	 * sendiri.
	 *
	 * @return pengguna pengunci, atau {@code null} bila baris tidak terkunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Mengunci atau membuka kunci baris skripsi ini.
	 *
	 * @param dikunci pengguna pemegang kunci, atau {@code null} untuk melepas kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Menyusun peta dosen <b>pembimbing</b> skripsi ini untuk keperluan agregasi lintas skripsi
	 * (mis. rekap beban bimbingan per dosen di dasbor).
	 *
	 * <p>Kunci peta sengaja berbentuk {@code "<idSkripsi>-<idDosen>"} — bukan id dosen saja —
	 * supaya pemanggil dapat menggabungkan hasil dari banyak skripsi dengan {@code putAll} tanpa
	 * entri saling menimpa ketika satu dosen membimbing beberapa skripsi.</p>
	 *
	 * <p>Yang dimasukkan adalah tiga slot pembimbing: {@link #getPembimbing()} (slot 1,
	 * label default "Pembimbing I"), {@link #getKetuaSidang()} (slot 2, label default
	 * "Pembimbing II" — nama kolomnya menyesatkan, lihat Javadoc class) dan
	 * {@link #getPembimbing3()}. Karena memanggil getter, slot yang dinonaktifkan pada
	 * {@link FormatNilaiSkripsi} akan ikut di-null-kan di memori.</p>
	 *
	 * @return peta berkunci {@code "<idSkripsi>-<idDosen>"} berisi dosen pembimbing yang terisi;
	 *         peta kosong bila belum ada pembimbing sama sekali
	 */
	public Map<String, Dosen> populateDosenPembimbing() {
		Map<String, Dosen> dosens = new HashMap<String, Dosen>();

		if (getKetuaSidang() != null) {
			dosens.put(getId() + "-" + getKetuaSidang().getId(), getKetuaSidang());
		}
		if (getPembimbing() != null) {
			dosens.put(getId() + "-" + getPembimbing().getId(), getPembimbing());
		}
		if (getPembimbing3() != null) {
			dosens.put(getId() + "-" + getPembimbing3().getId(), getPembimbing3());
		}
		return dosens;
	}

	/**
	 * Menyusun peta dosen <b>penguji</b> skripsi ini, pasangan dari
	 * {@link #populateDosenPembimbing()} dan memakai bentuk kunci yang sama
	 * ({@code "<idSkripsi>-<idDosen>"}).
	 *
	 * <p>Hanya empat slot penguji pertama yang dimasukkan: {@link #getPenguji1()} sampai
	 * {@link #getPenguji4()}. <b>Slot {@code penguji5} tidak ikut</b> — bila perguruan tinggi
	 * mengaktifkan slot ke-8 pada {@link FormatNilaiSkripsi}, penguji V tidak akan muncul pada
	 * rekap yang memakai method ini.</p>
	 *
	 * @return peta berkunci {@code "<idSkripsi>-<idDosen>"} berisi penguji I sampai IV yang
	 *         terisi; peta kosong bila belum ada penguji
	 */
	public Map<String, Dosen> populateDosenPenguji() {
		Map<String, Dosen> dosens = new HashMap<String, Dosen>();

		if (getPenguji1() != null) {
			dosens.put(getId() + "-" + getPenguji1().getId(), getPenguji1());
		}
		if (getPenguji2() != null) {
			dosens.put(getId() + "-" + getPenguji2().getId(), getPenguji2());
		}
		if (getPenguji3() != null) {
			dosens.put(getId() + "-" + getPenguji3().getId(), getPenguji3());
		}
		if (getPenguji4() != null) {
			dosens.put(getId() + "-" + getPenguji4().getId(), getPenguji4());
		}
		return dosens;
	}

	/**
	 * Memeriksa apakah menambahkan sejumlah bimbingan/pengujian baru akan <b>melewati batas</b>
	 * beban tugas akhir seorang dosen pada satu tahun akademik dan jenis semester tertentu.
	 *
	 * <p>Ambang diambil dari konfigurasi aplikasi
	 * {@code maksimal_bimbingan_dosen_mengajar_dalam_satu_semester} (default {@code "50"}); bila
	 * konfigurasi tidak ada atau tidak bisa di-parse, nilai 50 tetap dipakai.
	 * <b>Awas:</b> {@code Common.getKonfigurasi(kunci, default)} menuliskan baris konfigurasi
	 * default ke database bila kunci belum ada, jadi pemanggilan pertama method ini bisa membuat
	 * baris konfigurasi baru.</p>
	 *
	 * <p>Jumlah beban dihitung dengan satu {@code Criteria} {@code rowCount} atas tabel
	 * {@code skripsi}: baris dihitung bila dosen menempati <i>salah satu</i> dari slot
	 * {@code pembimbing}, {@code ketuaSidang}, {@code penguji1}, {@code penguji2},
	 * {@code penguji3}, {@code pembimbing3}, {@code penguji4}, <b>dan tahun akademiknya sama</b>,
	 * <b>dan</b> paritas semesternya cocok ({@code semester % 2 = 1} untuk
	 * {@link Perkuliahan#GANJIL}, {@code = 0} untuk genap). Bila {@code tahunAkademik} atau
	 * {@code semester} {@code null}, syarat yang bersangkutan diganti {@code false} sehingga
	 * hasil hitung pasti 0 dan method mengembalikan {@code false} — jadi memanggil tanpa periode
	 * berarti "tidak ada pembatasan", bukan "hitung semua periode".</p>
	 *
	 * <p><b>Efek samping UI:</b> bila batas terlampaui, method menampilkan dialog peringatan
	 * ZK lewat {@code KonfigurasiPromptHelper.tampilkanPeringatanDenganOpsiUbah(...)} yang juga
	 * menawarkan admin mengubah nilai konfigurasi di tempat. Method ini karenanya <b>hanya aman
	 * dipanggil dari thread event ZK</b>, bukan dari batch/scheduler. Selain itu ia mencetak
	 * ringkasan perhitungan ke {@code System.out}.</p>
	 *
	 * <p><b>Kuirk yang ditemukan (tidak diperbaiki):</b></p>
	 * <ul>
	 * <li>Slot {@code penguji5} <b>tidak</b> ikut dihitung, sehingga beban dosen yang hanya
	 * menempati slot penguji V selalu tampak nol.</li>
	 * <li>Pemeriksaan {@code dosen == null} pada pembentukan {@code criterion} tidak pernah bisa
	 * benar karena method sudah {@code return false} lebih dulu untuk argumen {@code null}.</li>
	 * <li>Nama method menyebut "SKS" padahal yang dihitung adalah <i>jumlah baris skripsi</i>,
	 * bukan satuan kredit.</li>
	 * </ul>
	 *
	 * <p>Dipanggil dari {@code ais.action.master.SkripsiAction} saat admin memilih dosen untuk
	 * tiap slot; {@link MahasiswaRequestTugasAkhir} punya method serupa untuk tahap proposal.</p>
	 *
	 * @param dosen             dosen yang hendak ditambahi beban; {@code null} berarti "tidak
	 *                          melebihi batas"
	 * @param tahunAkademik     tahun akademik yang diperiksa (mis. {@code "2025/2026"});
	 *                          {@code null} mematikan pemeriksaan
	 * @param semester          {@link Perkuliahan#GANJIL} atau {@link Perkuliahan#GENAP};
	 *                          {@code null} mematikan pemeriksaan
	 * @param tambahanMengajar  jumlah bimbingan yang hendak ditambahkan; dijumlahkan dengan beban
	 *                          berjalan sebelum dibandingkan dengan ambang. Tidak boleh
	 *                          {@code null} (akan {@code NullPointerException} saat unboxing)
	 * @return {@code true} bila batas <b>terlampaui</b> (pemanggil harus membatalkan penambahan),
	 *         {@code false} bila masih boleh
	 */
	public static boolean checkMaksSksDosen(Dosen dosen, String tahunAkademik, String semester,
			Integer tambahanMengajar) {
		if (dosen == null) {
			return false;
		}

		int maksimal_bimbingan_dosen_mengajar_dalam_satu_semester = 50;
		try {
			maksimal_bimbingan_dosen_mengajar_dalam_satu_semester = Integer.parseInt(Common
					.getKonfigurasi("maksimal_bimbingan_dosen_mengajar_dalam_satu_semester", "50").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Skripsi.java:160");

		}

		Session session = HibernateUtil.currentSession();

		Criterion criterion = dosen == null ? Restrictions.sqlRestriction("false")
				: Restrictions.or(Restrictions.eq("pembimbing", dosen), Restrictions.eq("ketuaSidang", dosen));

		criterion = Restrictions.or(criterion, Restrictions.eq("penguji1", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji2", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji3", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("pembimbing3", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("penguji4", dosen));

		Number q = ((Number) session.createCriteria(Skripsi.class)

				.add(criterion)
				.add(tahunAkademik == null ? Restrictions.sqlRestriction("false")
						: Restrictions.eq("tahunAkademik", tahunAkademik))

				.add(semester == null ? Restrictions.sqlRestriction("false")
						: Restrictions.sqlRestriction(
								"this_.semester % 2 = " + (semester.equals(Perkuliahan.GANJIL) ? "1" : "0")))

				.setProjection(Projections.rowCount()).uniqueResult());

		int jumlahMengajar = q == null ? 0 : q.intValue();

		System.out.println("dosen => " + dosen + ", tahunAkademik => " + tahunAkademik + ", semester => " + semester
				+ ", maksimal_bimbingan_dosen_mengajar_dalam_satu_semester => "
				+ maksimal_bimbingan_dosen_mengajar_dalam_satu_semester + ", jumlahMengajar => " + jumlahMengajar);

		boolean hasil = maksimal_bimbingan_dosen_mengajar_dalam_satu_semester < (tambahanMengajar + jumlahMengajar);

		if (hasil) {
			try {
				KonfigurasiPromptHelper.tampilkanPeringatanDenganOpsiUbah(
						"Dosen dengan nama " + dosen.getNama() + " telah membimbing di tahun akademik " + tahunAkademik
								+ " semester " + semester + " sebanyak " + jumlahMengajar
								+ " bimbingan. Anda tidak bisa menambah " + tambahanMengajar
								+ " bimbingan lagi, karena maksimal jumlah bimbingan yang diajar oleh dosen adalah "
								+ maksimal_bimbingan_dosen_mengajar_dalam_satu_semester,
						"Peringatan", "maksimal_bimbingan_dosen_mengajar_dalam_satu_semester", "50");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
			return hasil;
		}

		return false;
	}

	/** Judul tugas akhir dalam bahasa Indonesia (kolom {@code text}). */
	private String judul;
	/** Judul tugas akhir dalam bahasa Inggris; bila kosong, {@link #getJudulen()} memakai {@link #judul}. */
	private String judulen;
	/** Kata kunci naskah, dipakai pencarian repositori tugas akhir. */
	private String keyword;
	/** Dosen slot {@code dosen1} (label default "Pembimbing I"); nilainya justru di {@link #nilaiKetuaSidang}. */
	private Dosen pembimbing;
	/** Dosen slot {@code dosen2} (label default "Pembimbing II"); nilainya justru di {@link #nilaiPembimbing}. */
	private Dosen ketuaSidang;
	/** Dosen slot {@code dosen3} (label default "Penguji I"). */
	private Dosen penguji1;
	/** Dosen slot {@code dosen4} (label default "Penguji II"). */
	private Dosen penguji2;
	/** Dosen slot {@code dosen5} (label default "Penguji III"). */
	private Dosen penguji3;
	/** Nilai dari dosen <b>slot 1</b> ({@link #pembimbing}) — lihat tabel pemetaan di Javadoc class. */
	private Double nilaiKetuaSidang = 0.0;
	/** Nilai dari dosen <b>slot 2</b> ({@link #ketuaSidang}) — lihat tabel pemetaan di Javadoc class. */
	private Double nilaiPembimbing = 0.0;
	/** Nilai dari {@link #pembimbing3} (slot {@code dosen21}). */
	private Double nilaiPembimbing3 = 0.0;
	/** Nilai dari {@link #penguji1}. */
	private Double nilaiPenguji1 = 0.0;
	/** Nilai dari {@link #penguji2}. */
	private Double nilaiPenguji2 = 0.0;
	/** Nilai dari {@link #penguji3}. */
	private Double nilaiPenguji3 = 0.0;
	/** Nilai akhir gabungan berbobot; dihitung ulang oleh {@link #getTotalNilai()}. */
	private Double totalNilai = 0.0;
	/** Nilai ujian komprehensif (bila jalur tugas akhir mensyaratkannya); murni disimpan, tidak dihitung. */
	private Double nilaikomprehensif = 0.0;
	/** Nilai huruf hasil pemetaan {@link #totalNilai}; dihitung ulang oleh {@link #getNilaiHuruf()}. */
	private String nilaiHuruf;
	/** Bobot IP tugas akhir; hanya disimpan/dibaca, tidak pernah dihitung di class ini. */
	private Double totalIP;
	/** Status kelulusan; disimpulkan {@link #getLulus()} dari {@link #nilaiHuruf} bila masih {@code null}. */
	private Boolean lulus;
	/** Tanggal sidang; bila kosong diambil dari {@link #jadwalSidangTugasAkhir}. */
	private Date tanggalSidang;
	/** Tanggal seminar proposal; dibayangi nilai dari {@link #mahasiswaRequestTugasAkhir} bila ada. */
	private Date tanggalSeminar;
	/** Mahasiswa pemilik tugas akhir ini (kolom {@code mahasiswa}, {@code NOT NULL}). */
	private Mahasiswa mahasiswa;
	/** Penanda sidang sudah berlangsung: {@code 0} belum, {@code 1} sudah. */
	private Integer telahSidang = 0;
	/** Persetujuan pelaksanaan sidang; diturunkan otomatis oleh {@link #getSetujuiSidang()}. */
	private Boolean setujuiSidang;
	/** Ruang sidang; bila jadwal sidang punya ruang, ruang jadwal yang menang. */
	private Ruang ruangSidang;
	/** Jam mulai sidang dalam format waktu {@code Common.timeFormat}. */
	private String waktuSidang;
	/** Jam selesai sidang dalam format waktu {@code Common.timeFormat}. */
	private String waktuSampaiSidang;
	/** Abstrak naskah (kolom {@code text}). Ejaan kolom memang {@code abstrack}. */
	private String abstrack;

	/** Jalur tugas akhir (mis. skripsi/non-skripsi/jalur khusus); teks bebas dari master. */
	private String jalurSkripsi;
	/** Tipe/kategori tugas akhir; teks bebas dari master. */
	private String tipeSkripsi;
	/** Tanggal mulai masa bimbingan. */
	private Date awalBimbingan;
	/** Tanggal akhir masa bimbingan; bersama {@link #awalBimbingan} menentukan {@link #selesaiDalamBulan}. */
	private Date akhirBimbingan;
	/** Dosen slot {@code dosen21} (label default "Pembimbing III"). */
	private Dosen pembimbing3;
	/** Syarat kelulusan TOEFL sudah terpenuhi. */
	private Boolean lulusToefl;
	/** Syarat kelulusan TOAFL (bahasa Arab) sudah terpenuhi. */
	private Boolean lulusToafl;

	/** Konfigurasi susunan slot dosen, bobot nilai, dan aturan nilai huruf untuk skripsi ini. */
	private FormatNilaiSkripsi formatNilaiSkripsi;
	/** Referensi item keuangan/tagihan terkait sidang; disimpan mentah, tanpa logika di class ini. */
	private Long itemRef;
	/** Jadwal sidang yang memasok tanggal dan ruang sidang. */
	private JadwalSidangTugasAkhir jadwalSidangTugasAkhir;
	/** Permintaan/pengajuan tugas akhir tahap proposal; membayangi beberapa field skripsi ini. */
	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;

	/** Baris KHS tempat tugas akhir ini muncul sebagai mata kuliah (bila dikonversi). */
	private Detailperkuliahan detailperkuliahan;
	/** Semester tempuh mahasiswa saat tugas akhir; paritasnya menentukan ganjil/genap. */
	private Integer semester;
	/** Tahun akademik pelaksanaan, mis. {@code "2025/2026"}. */
	private String tahunAkademik;
	/** Dosen slot {@code dosen6} (label default "Penguji IV"). */
	private Dosen penguji4;
	/** Nilai dari {@link #penguji4}. */
	private Double nilaiPenguji4;
	/** Rincian nilai per komponen per dosen dalam satu string CSV; lihat format di Javadoc class. */
	private String detailNilai;
	/** Keterangan lokasi ujian (teks bebas, mis. daring/luring beserta tautan). */
	private String lokasiUjian;
	/** Nomor SK penetapan pembimbing/penguji. */
	private String nomorSk;
	/** Tanggal SK penetapan pembimbing/penguji. */
	private Date tglSk;
	/** Lama penyelesaian dalam bulan; dihitung ulang oleh {@link #getSelesaiDalamBulan()}. */
	private Integer selesaiDalamBulan;
	/** Tahun awal tahun akademik; diturunkan {@link #getTahun()} dari {@link #tahunAkademik}. */
	private Integer tahun;

	/** Format nilai (komponen KHS) yang berbeda dari {@link #formatNilaiSkripsi}; dipakai saat konversi ke KHS. */
	private FormatNilai formatNilai;
	/** Gelombang pendaftaran sidang tugas akhir yang diikuti mahasiswa. */
	private GelombangPendaftaranSidangTugasAkhir gelombangPendaftaranSidangTugasAkhir;

	/** Kode semester gabungan {@code <tahun><1|2>} untuk pelaporan; diturunkan {@link #getSmt()}. */
	private String smt;
	/** Pola perulangan jadwal bimbingan (default {@code "Mingguan"}). */
	private String jenis;
	/** Bila {@code true} (default), penjadwalan bimbingan melewati hari libur nasional. */
	private Boolean lewatiTanggalMerahNasional;

	/** Persetujuan naskah oleh pembimbing slot 1; murni manual, tidak diturunkan dari nilai. */
	private Boolean persetujuanPembimbing1;
	/** Persetujuan naskah oleh pembimbing slot 2; murni manual, tidak diturunkan dari nilai. */
	private Boolean persetujuanPembimbing2;
	/** Persetujuan naskah oleh pembimbing III; murni manual, tidak diturunkan dari nilai. */
	private Boolean persetujuanPembimbing3;

	/** Persetujuan penguji I; otomatis menjadi {@code true} begitu nilainya &gt; 0,1. */
	private Boolean persetujuanPenguji1;
	/** Persetujuan penguji II; otomatis menjadi {@code true} begitu nilainya &gt; 0,1. */
	private Boolean persetujuanPenguji2;
	/** Persetujuan penguji III; otomatis menjadi {@code true} begitu nilainya &gt; 0,1. */
	private Boolean persetujuanPenguji3;
	/** Persetujuan penguji IV; otomatis menjadi {@code true} begitu nilainya &gt; 0,1. */
	private Boolean persetujuanPenguji4;

	/** Daftar referensi/pustaka dalam bentuk JSON array; bisa diwarisi dari pengajuan proposal. */
	private String referensi;

	/** Id padanan di PDDikti Feeder untuk sinkronisasi data tugas akhir. */
	private String feeder;
	/** Dosen slot {@code dosen7} (label default "Penguji V"); slot terbaru, beberapa method lama belum menyertakannya. */
	private Dosen penguji5;
	/** Persetujuan penguji V; otomatis menjadi {@code true} begitu nilainya &gt; 0,1. */
	private Boolean persetujuanPenguji5;
	/** Nilai dari {@link #penguji5}. */
	private Double nilaiPenguji5;

	/** Bila {@code true}, rincian nilai disembunyikan dari portal mahasiswa. */
	private Boolean sembunyikanNilaiKemahasiswa;

	/**
	 * Mengembalikan kode semester gabungan berformat {@code <tahun><1|2>} (mis. {@code "20251"}
	 * untuk semester ganjil tahun 2025) yang dipakai pelaporan dan ekspor Feeder.
	 *
	 * <p>Nilai <b>selalu dihitung ulang</b> dari {@link #getTahun()} dan {@link #getSemester()}
	 * bila keduanya tersedia: {@code tahun} disambung dengan {@code "2"} untuk semester genap
	 * (paritas {@code semester % 2 == 0}) atau {@code "1"} untuk ganjil. Jadi nilai yang pernah
	 * di-{@link #setSmt(String)} akan tertimpa. Bila hasilnya tetap kosong atau justru
	 * mengandung teks {@code "null"} (kasus data lama), dipakai penyelamat berupa tahun kalender
	 * berjalan digabung dengan paritas semester akademik yang sedang aktif.</p>
	 *
	 * <p><b>Efek samping:</b> menulis field {@link #smt} (dan lewat getter yang dipanggilnya,
	 * juga {@link #tahun}, {@link #semester}, dan {@link #tahunAkademik}), sehingga sekadar
	 * membaca kode semester dapat menandai entity kotor bagi Hibernate.</p>
	 *
	 * @return kode semester lima/enam karakter; tidak pernah {@code null}
	 */
	@Column(name = "smt", length = 6)
	public String getSmt() {
		Integer tahunAwal = getTahun();
		Integer semesterAktif = getSemester();
		if (tahunAwal != null && semesterAktif != null) {
			smt = tahunAwal + (semesterAktif % 2 == 0 ? "2" : "1");
		}
		if (smt == null || smt.trim().isEmpty() || smt.trim().toLowerCase().contains("null")) {
			smt = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
					+ (Common.isNowSemensterGanjil() ? "1" : "2");
		}
		return smt;
	}

	/**
	 * Menyetel kode semester gabungan. Perlu diketahui bahwa {@link #getSmt()} menghitung ulang
	 * nilai ini setiap kali dipanggil, sehingga nilai yang disetel di sini hanya bertahan selama
	 * tahun/semester belum bisa diturunkan.
	 *
	 * @param smt kode semester berformat {@code <tahun><1|2>}
	 */
	public void setSmt(String smt) {
		this.smt = smt;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate. Semua field tetap pada nilai default
	 * (beberapa nilai numerik sudah diinisialisasi {@code 0.0} dan {@link #tanggal_dirubah} ke
	 * waktu sekarang).
	 */
	public Skripsi() {
	}

	/**
	 * Konstruktor peninggalan hbm2java untuk kolom-kolom {@code NOT NULL} versi lama. Hanya
	 * mengisi lima kolom nilai dan tidak menyentuh relasi wajib {@link #mahasiswa}, sehingga
	 * object hasil konstruktor ini belum siap disimpan. Praktiknya kode aplikasi memakai
	 * {@link #Skripsi()} lalu mengisi lewat setter.
	 *
	 * @param nilaiKetuaSidang nilai dosen slot {@code dosen1} (kolom {@code nilai_ketua_sidang})
	 * @param nilaiPembimbing  nilai dosen slot {@code dosen2} (kolom {@code nilai_pembimbing})
	 * @param nilaiPenguji1    nilai penguji I
	 * @param nilaiPenguji2    nilai penguji II
	 * @param totalNilai       nilai akhir gabungan
	 */
	public Skripsi(Double nilaiKetuaSidang, Double nilaiPembimbing, Double nilaiPenguji1, Double nilaiPenguji2,
			Double totalNilai) {
		this.nilaiKetuaSidang = nilaiKetuaSidang;
		this.nilaiPembimbing = nilaiPembimbing;
		this.nilaiPenguji1 = nilaiPenguji1;
		this.nilaiPenguji2 = nilaiPenguji2;
		this.totalNilai = totalNilai;
	}

	/**
	 * Primary key baris skripsi. Dihasilkan database ({@code IDENTITY}) dan tidak ikut
	 * di-{@code INSERT}.
	 *
	 * @return id baris, atau {@code null} untuk object yang belum pernah disimpan
	 * @see ais.database.model.GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = true)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Hanya untuk keperluan Hibernate atau penyalinan object; mengubah id
	 * object yang sudah persistent akan mengacaukan identitas entity.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan judul tugas akhir yang sudah dipangkas spasi tepinya.
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, field diisi string kosong lebih dulu
	 * sehingga getter tidak pernah mengembalikan {@code null} — tetapi ini juga berarti membaca
	 * judul dapat mengubah state entity dari {@code null} menjadi {@code ""}.</p>
	 *
	 * @return judul skripsi; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(name = "judul", columnDefinition = "text")
	public String getJudul() {
		if (judul == null) {
			judul = "";
		}
		return this.judul.trim();
	}

	/**
	 * Menyetel judul tugas akhir.
	 *
	 * @param judul judul naskah; disimpan apa adanya (pemangkasan dilakukan di getter)
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Mengembalikan dosen slot {@code dosen1} — label defaultnya "Pembimbing I", disimpan di
	 * kolom {@code pembimbing}, dan <b>nilainya</b> berada di kolom {@code nilai_ketua_sidang}
	 * (lihat tabel pemetaan pada Javadoc class).
	 *
	 * <p>Getter ini adalah acuan pola bagi seluruh getter slot dosen di class ini. Urutan
	 * kerjanya:</p>
	 * <ol>
	 * <li>Meresolusi proxy {@link #mahasiswaRequestTugasAkhir} dan {@link #formatNilaiSkripsi}
	 * lewat {@link GeneralValueObject#check(Object)} — yang untuk entity detached bisa membuka
	 * session Hibernate sendiri.</li>
	 * <li><b>Override dari tahap proposal:</b> bila skripsi ini tertaut ke sebuah
	 * {@link MahasiswaRequestTugasAkhir} yang sudah menetapkan {@code dosen1}, dosen dari
	 * pengajuan itulah yang dipakai dan <b>ditulis balik</b> ke field {@link #pembimbing}. Jadi
	 * mengganti pembimbing lewat {@link #setPembimbing(Dosen)} tidak akan bertahan selama
	 * pengajuan proposal masih memuat dosen lain.</li>
	 * <li><b>Penonaktifan slot:</b> selain itu, bila {@link FormatNilaiSkripsi} menyatakan slot
	 * {@code dosen1} tidak aktif, field di-set {@code null}. Bila entity kemudian di-flush,
	 * relasi ini benar-benar terhapus dari database.</li>
	 * </ol>
	 *
	 * @return dosen slot {@code dosen1}, atau {@code null} bila belum ditetapkan atau slotnya
	 *         dinonaktifkan
	 * @see #simpanDosen(Dosen, String)
	 * @see #dataDosen(boolean)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembimbing", nullable = true)
	public Dosen getPembimbing() {
		mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
		formatNilaiSkripsi = check(formatNilaiSkripsi);
		pembimbing = check(pembimbing);
		if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getDosen1() != null) {
			pembimbing = mahasiswaRequestTugasAkhir.getDosen1();
		}

		else if (formatNilaiSkripsi != null && !formatNilaiSkripsi.getDosen1Aktif()) {
			pembimbing = null;
		}

		return this.pembimbing;
	}

	/**
	 * Menyetel dosen slot {@code dosen1}. Ingat bahwa {@link #getPembimbing()} dapat menimpa
	 * nilai ini dari {@link MahasiswaRequestTugasAkhir}.
	 *
	 * @param pembimbing dosen pembimbing I, boleh {@code null}
	 */
	public void setPembimbing(Dosen pembimbing) {
		this.pembimbing = pembimbing;
	}

	/**
	 * Mengembalikan dosen slot {@code dosen2} — label defaultnya "Pembimbing II", disimpan di
	 * kolom {@code ketua_sidang}, dan <b>nilainya</b> berada di kolom {@code nilai_pembimbing}.
	 *
	 * <p>Perilakunya sama dengan {@link #getPembimbing()} (override dari pengajuan proposal lewat
	 * {@code getDosen2()}, dan penonaktifan slot lewat {@code dosen2Aktif}), hanya urutannya
	 * sedikit berbeda: di sini {@code check} atas field dosen dilakukan <i>setelah</i> override,
	 * bukan sebelumnya.</p>
	 *
	 * @return dosen slot {@code dosen2}, atau {@code null} bila belum ditetapkan atau slotnya
	 *         dinonaktifkan
	 * @see #getPembimbing()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ketua_sidang", nullable = true)
	public Dosen getKetuaSidang() {
		mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
		formatNilaiSkripsi = check(formatNilaiSkripsi);
		if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getDosen2() != null) {
			ketuaSidang = mahasiswaRequestTugasAkhir.getDosen2();
		}

		else if (formatNilaiSkripsi != null && !formatNilaiSkripsi.getDosen2Aktif()) {
			ketuaSidang = null;
		}

		ketuaSidang = check(ketuaSidang);
		return this.ketuaSidang;
	}

	/**
	 * Menyetel dosen slot {@code dosen2}. Dapat ditimpa kembali oleh {@link #getKetuaSidang()}
	 * bila pengajuan proposal memuat dosen lain.
	 *
	 * @param ketuaSidang dosen pembimbing II / ketua sidang, boleh {@code null}
	 */
	public void setKetuaSidang(Dosen ketuaSidang) {
		this.ketuaSidang = ketuaSidang;
	}

	/**
	 * Mengembalikan dosen slot {@code dosen3} (label default "Penguji I", kolom
	 * {@code penguji1}).
	 *
	 * <p>Berbeda dengan dua slot pembimbing pertama, slot penguji <b>tidak</b> mengambil override
	 * dari {@link MahasiswaRequestTugasAkhir} — pengajuan proposal hanya menetapkan pembimbing.
	 * Yang tetap berlaku adalah penonaktifan slot: bila {@code formatNilaiSkripsi.dosen3Aktif}
	 * bernilai salah, field di-set {@code null} (dan hilang dari database bila entity di-flush).
	 * Pemanggilan {@code check(mahasiswaRequestTugasAkhir)} di baris pertama tetap dilakukan
	 * meski hasilnya tidak dipakai.</p>
	 *
	 * @return dosen penguji I, atau {@code null} bila belum ditetapkan atau slotnya dinonaktifkan
	 * @see #getPembimbing()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penguji1", nullable = true)
	public Dosen getPenguji1() {
		mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
		formatNilaiSkripsi = check(formatNilaiSkripsi);
		if (formatNilaiSkripsi != null && !formatNilaiSkripsi.getDosen3Aktif()) {
			penguji1 = null;
		}

		penguji1 = check(penguji1);
		return this.penguji1;
	}

	/**
	 * Menyetel dosen penguji I (slot {@code dosen3}).
	 *
	 * @param penguji1 dosen penguji I, boleh {@code null}
	 */
	public void setPenguji1(Dosen penguji1) {
		this.penguji1 = penguji1;
	}

	/**
	 * Mengembalikan dosen slot {@code dosen4} (label default "Penguji II", kolom
	 * {@code penguji2}). Perilakunya identik dengan {@link #getPenguji1()}, hanya bendera
	 * aktifnya {@code dosen4Aktif}.
	 *
	 * @return dosen penguji II, atau {@code null} bila belum ditetapkan atau slotnya
	 *         dinonaktifkan
	 * @see #getPenguji1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penguji2", nullable = true)
	public Dosen getPenguji2() {
		mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
		formatNilaiSkripsi = check(formatNilaiSkripsi);
		if (formatNilaiSkripsi != null && !formatNilaiSkripsi.getDosen4Aktif()) {
			penguji2 = null;
		}

		penguji2 = check(penguji2);
		return this.penguji2;
	}

	/**
	 * Menyetel dosen penguji II (slot {@code dosen4}).
	 *
	 * @param penguji2 dosen penguji II, boleh {@code null}
	 */
	public void setPenguji2(Dosen penguji2) {
		this.penguji2 = penguji2;
	}

	/**
	 * Mengembalikan nilai yang diberikan dosen slot {@code dosen1} ({@link #getPembimbing()}),
	 * meski nama kolomnya {@code nilai_ketua_sidang} — lihat tabel pemetaan pada Javadoc class.
	 * Null-safe: nilai yang belum diisi dilaporkan {@code 0.0} sehingga aman untuk unboxing.
	 *
	 * @return nilai dosen slot {@code dosen1}; {@code 0.0} bila belum dinilai
	 */
	@Column(name = "nilai_ketua_sidang", nullable = true, precision = 15)
	public Double getNilaiKetuaSidang() {
		return this.nilaiKetuaSidang == null ? 0.0 : this.nilaiKetuaSidang;
	}

	/**
	 * Menyetel nilai dari dosen slot {@code dosen1}.
	 *
	 * @param nilaiKetuaSidang nilai angka; {@code null} berarti "belum dinilai"
	 */
	public void setNilaiKetuaSidang(Double nilaiKetuaSidang) {
		this.nilaiKetuaSidang = nilaiKetuaSidang;
	}

	/**
	 * Mengembalikan nilai yang diberikan dosen slot {@code dosen2} ({@link #getKetuaSidang()}),
	 * meski nama kolomnya {@code nilai_pembimbing}. Null-safe seperti
	 * {@link #getNilaiKetuaSidang()}.
	 *
	 * @return nilai dosen slot {@code dosen2}; {@code 0.0} bila belum dinilai
	 */
	@Column(name = "nilai_pembimbing", nullable = true, precision = 15)
	public Double getNilaiPembimbing() {
		return this.nilaiPembimbing == null ? 0.0 : this.nilaiPembimbing;
	}

	/**
	 * Menyetel nilai dari dosen slot {@code dosen2}.
	 *
	 * @param nilaiPembimbing nilai angka; {@code null} berarti "belum dinilai"
	 */
	public void setNilaiPembimbing(Double nilaiPembimbing) {
		this.nilaiPembimbing = nilaiPembimbing;
	}

	/**
	 * Mengembalikan nilai dari penguji I ({@link #getPenguji1()}), null-safe ke {@code 0.0}.
	 *
	 * @return nilai penguji I; {@code 0.0} bila belum dinilai
	 */
	@Column(name = "nilai_penguji1", nullable = true, precision = 15)
	public Double getNilaiPenguji1() {
		return this.nilaiPenguji1 == null ? 0.0 : this.nilaiPenguji1;
	}

	/**
	 * Menyetel nilai dari penguji I. Nilai &gt; 0,1 membuat {@link #getPersetujuanPenguji1()}
	 * otomatis melaporkan sudah disetujui.
	 *
	 * @param nilaiPenguji1 nilai angka; {@code null} berarti "belum dinilai"
	 */
	public void setNilaiPenguji1(Double nilaiPenguji1) {
		this.nilaiPenguji1 = nilaiPenguji1;
	}

	/**
	 * Mengembalikan nilai dari penguji II ({@link #getPenguji2()}), null-safe ke {@code 0.0}.
	 *
	 * @return nilai penguji II; {@code 0.0} bila belum dinilai
	 */
	@Column(name = "nilai_penguji2", nullable = true, precision = 15)
	public Double getNilaiPenguji2() {
		return this.nilaiPenguji2 == null ? 0.0 : this.nilaiPenguji2;
	}

	/**
	 * Menyetel nilai dari penguji II. Nilai &gt; 0,1 membuat {@link #getPersetujuanPenguji2()}
	 * otomatis melaporkan sudah disetujui.
	 *
	 * @param nilaiPenguji2 nilai angka; {@code null} berarti "belum dinilai"
	 */
	public void setNilaiPenguji2(Double nilaiPenguji2) {
		this.nilaiPenguji2 = nilaiPenguji2;
	}

	/**
	 * Menghitung ulang lalu mengembalikan <b>nilai akhir gabungan</b> tugas akhir.
	 *
	 * <p>Perhitungan hanya berjalan bila {@link #getFormatNilaiSkripsi()} tersedia <i>dan</i>
	 * minimal satu dari delapan nilai per slot sudah terisi (&gt; 0,1). Rumusnya adalah jumlah
	 * berbobot: setiap nilai slot dikalikan persentase yang dikonfigurasi di
	 * {@link FormatNilaiSkripsi} lalu dibagi 100. Bila totalnya berbeda dari
	 * {@link #totalNilai} yang tersimpan, field diperbarui — <b>getter ini karena itu menulis
	 * state</b> dan dapat memicu {@code UPDATE} saat flush.</p>
	 *
	 * <p>Persentase yang dipakai mengikuti pemetaan slot yang tertukar namanya:
	 * {@code prosentasiNilaiKetuaSidang} berpasangan dengan {@link #getNilaiKetuaSidang()}
	 * (dosen slot {@code dosen1}), dan seterusnya — lihat tabel pada Javadoc class. Karena
	 * pembagi selalu 100 (bukan jumlah persentase slot yang aktif), <b>total akan lebih kecil
	 * dari semestinya bila persentase seluruh slot tidak berjumlah 100</b>.</p>
	 *
	 * <p>Seluruh badan perhitungan dibungkus {@code try/catch} yang menelan exception (hanya
	 * dicatat ke audit), jadi kegagalan menghasilkan nilai lama, bukan error. Blok komentar
	 * "KE-FIX" di dalam method mencatat perbaikan lama: nilai dibaca lewat getter null-safe agar
	 * sidang yang baru dinilai sebagian tidak melempar {@code NullPointerException} saat
	 * unboxing — NPE itu dulu merembet ke {@link #getNilaiHuruf()}/{@link #getLulus()} dan ke
	 * proses lain yang membaca entity ini.</p>
	 *
	 * @return nilai akhir gabungan; {@code 0.0} bila belum ada nilai sama sekali, tidak pernah
	 *         {@code null}
	 * @see #getNilaiHuruf()
	 * @see #hitungTotalNilai(Boolean, Dosen, List)
	 */
	@Column(name = "total_nilai", nullable = true, precision = 15)
	public Double getTotalNilai() {

		try {
			formatNilaiSkripsi = getFormatNilaiSkripsi();

			// KE-FIX (NullPointerException getTotalNilai): field nilaiKetuaSidang/nilaiPembimbing/
			// nilaiPembimbing3/nilaiPenguji1/nilaiPenguji2/nilaiPenguji3 adalah Double (boxed) yang
			// BOLEH null (belum dinilai), tapi kode di bawah sebelumnya membaca field MENTAH-nya
			// langsung (bukan lewat getter null-safe getNilaiKetuaSidang()/dst) sehingga unboxing
			// otomatis pada "> 0.1" dan perkalian "*" melempar NPE begitu SATU SAJA dari nilai-nilai
			// itu belum diisi -- sering terjadi krn sidang skripsi biasanya diisi bertahap oleh
			// masing-masing penguji. NPE ini menembus getNilaiHuruf()/getLulus() dan mengganggu
			// proses lain yg membaca entity Skripsi (mis. RepositorySyncService, dirty-check flush
			// Hibernate). Perbaiki dgn membaca lewat getter null-safe (default 0.0) supaya nilai yg
			// belum diisi dihitung sbg 0, bukan melempar NPE dan membatalkan seluruh perhitungan.
			double nilaiKetuaSidangSafe = getNilaiKetuaSidang();
			double nilaiPembimbingSafe = getNilaiPembimbing();
			double nilaiPembimbing3Safe = getNilaiPembimbing3();
			double nilaiPenguji1Safe = getNilaiPenguji1();
			double nilaiPenguji2Safe = getNilaiPenguji2();
			double nilaiPenguji3Safe = getNilaiPenguji3();

			if (formatNilaiSkripsi != null && (nilaiKetuaSidangSafe > 0.1 || nilaiPembimbingSafe > 0.1
					|| nilaiPembimbing3Safe > 0.1 || nilaiPenguji1Safe > 0.1 || nilaiPenguji2Safe > 0.1
					|| nilaiPenguji3Safe > 0.1 || getNilaiPenguji4() > 0.1 || getNilaiPenguji5() > 0.1)) {

				Double nilaiKetuaV = nilaiKetuaSidangSafe * (formatNilaiSkripsi.getProsentasiNilaiKetuaSidang()) / 100.0;
				Double nilaiPembimbingV = nilaiPembimbingSafe * (formatNilaiSkripsi.getProsentasiNilaiPembimbing()) / 100.0;
				Double nilaiPembimbing3V = nilaiPembimbing3Safe * (formatNilaiSkripsi.getProsentasiNilaiPembimbing3())
						/ 100.0;
				Double nilaiPenguji1V = nilaiPenguji1Safe * (formatNilaiSkripsi.getProsentasiNilaiPenguji1()) / 100.0;
				Double nilaiPenguji2V = nilaiPenguji2Safe * (formatNilaiSkripsi.getProsentasiNilaiPenguji2()) / 100.0;

				Double nilaiPenguji3V = nilaiPenguji3Safe * (formatNilaiSkripsi.getProsentasiNilaiPenguji3()) / 100.0;

				Double nilaiPenguji4V = getNilaiPenguji4() * (formatNilaiSkripsi.getProsentasiNilaiPenguji4()) / 100.0;

				Double nilaiPenguji5V = getNilaiPenguji5() * (formatNilaiSkripsi.getProsentasiNilaiPenguji5()) / 100.0;

				Double jumlahTotal = nilaiKetuaV + nilaiPembimbingV + nilaiPembimbing3V + nilaiPenguji1V
						+ nilaiPenguji2V + nilaiPenguji3V + nilaiPenguji4V + nilaiPenguji5V;
				// System.out.println("jumlahTotal => " + jumlahTotal + ",
				// totalNilai = " + totalNilai);
				if (totalNilai == null || !jumlahTotal.equals(totalNilai)) {
					totalNilai = jumlahTotal;

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Skripsi.java:482");
			// Common.tampilErrorJikaAdmin(e);
		}

		if (totalNilai == null) {
			totalNilai = 0.0;
		}
		return this.totalNilai;
	}

	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	@Column(name = "nilai_huruf", length = 50)
	public String getNilaiHuruf() {
		totalNilai = getTotalNilai();
		try {
			/* Getter entity juga dipanggil Hibernate saat dirty-check dan laporan pada
			 * object detached. Jangan memaksa proxy mahasiswa/jurusan/fakultas melakukan
			 * lazy-load tanpa session; gunakan nilai_huruf tersimpan sampai relasi tersedia. */
			if (mahasiswa != null && !Hibernate.isInitialized(mahasiswa)) {
				return nilaiHuruf == null ? "-" : nilaiHuruf.trim();
			}
			// Skripsi baru (mis. saat onAdd) belum tentu punya mahasiswa/jurusan
			// terisi -> jangan lanjut hitung nilai huruf, cukup kembalikan "-".
			Jurusan jurusanNilai = mahasiswa == null ? null : mahasiswa.getJurusan();
			if (jurusanNilai == null) {
				return "-";
			}
			if (!Hibernate.isInitialized(jurusanNilai)) {
				return nilaiHuruf == null ? "-" : nilaiHuruf.trim();
			}
			Fakultas fakultasNilai = jurusanNilai.getFakultas();
			if (fakultasNilai != null && !Hibernate.isInitialized(fakultasNilai)) {
				return nilaiHuruf == null ? "-" : nilaiHuruf.trim();
			}

			Matakuliah matakuliah = detailperkuliahan == null ? null
					: detailperkuliahan.getPerkuliahan() != null ? detailperkuliahan.getPerkuliahan().getMatakuliah()
							: detailperkuliahan.getMatakuliahKonversi();

			JenisNilaiHurufMatakuliah jenisNilaiHuruf = matakuliah == null ? null : matakuliah.getJenisNilaiHuruf();

			if (formatNilaiSkripsi != null && formatNilaiSkripsi.getJenisNilaiHuruf() != null) {
				jenisNilaiHuruf = formatNilaiSkripsi.getJenisNilaiHuruf();
			}

			NilaiHuruf a = detailperkuliahan == null
					? Common.getNilaiHuruf(totalNilai, mahasiswa.getTahunangkatan(), jurusanNilai,
							fakultasNilai, Common.getCurrentTahunAkademik(),
							Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
							matakuliah == null ? "" : matakuliah.getKode(), jenisNilaiHuruf)
					: Common.getNilaiHuruf(totalNilai, mahasiswa.getTahunangkatan(), jurusanNilai,
							fakultasNilai, detailperkuliahan.getTahunAkademik(),
							detailperkuliahan.getPerkuliahan() == null ? null
									: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
							matakuliah == null ? "" : matakuliah.getKode(), jenisNilaiHuruf);
			nilaiHuruf = a == null ? "" : a.getNilaiHuruf();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Skripsi.java:521");
			// Common.tampilErrorJikaAdmin(e);
		}
		return this.nilaiHuruf == null ? "-" : this.nilaiHuruf.trim();
	}

	public void setNilaiHuruf(String nilaiHuruf) {
		this.nilaiHuruf = nilaiHuruf;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_sidang", length = 0)
	public Date getTanggalSidang() {
		if (tanggalSidang == null && jadwalSidangTugasAkhir != null) {
			tanggalSidang = jadwalSidangTugasAkhir.getMulai();
		}
		return this.tanggalSidang;
	}

	public void setTanggalSidang(Date tanggalSidang) {
		this.tanggalSidang = tanggalSidang;
	}

	public String getJenis() {
		return jenis == null ? "Mingguan" : jenis;
	}

	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	public Boolean getLewatiTanggalMerahNasional() {
		return lewatiTanggalMerahNasional == null ? true : lewatiTanggalMerahNasional;
	}

	public void setLewatiTanggalMerahNasional(Boolean lewatiTanggalMerahNasional) {
		this.lewatiTanggalMerahNasional = lewatiTanggalMerahNasional;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	public void setTelahSidang(Integer telahSidang) {
		this.telahSidang = telahSidang;
	}

	@Column(name = "telah_sidang", length = 1)
	public Integer getTelahSidang() {
		if (telahSidang == null) {
			telahSidang = 0;
		}

		if (totalNilai != null && totalNilai > 1.0) {
			telahSidang = 1;
		}

		return telahSidang;
	}

	public void setWaktuSidang(String waktuSidang) {
		this.waktuSidang = waktuSidang;
	}

	@Column(name = "waktu_sidang", length = 50)
	public String getWaktuSidang() {
		if (waktuSidang == null || waktuSidang.trim().isEmpty()) {
			waktuSidang = Common.timeFormat.get().format(ais.ui.util.WaktuUtil.getDate());
		}

		return waktuSidang;
	}

	public void setWaktuSampaiSidang(String waktuSampaiSidang) {
		this.waktuSampaiSidang = waktuSampaiSidang;
	}

	@Column(name = "waktu_sampai_sidang", length = 50)
	public String getWaktuSampaiSidang() {
		if (waktuSampaiSidang == null || waktuSampaiSidang.trim().isEmpty()) {
			waktuSampaiSidang = Common.timeFormat.get().format(ais.ui.util.WaktuUtil.getDate());
		}
		return waktuSampaiSidang;
	}

	public void setTanggalSeminar(Date tanggalSeminar) {
		this.tanggalSeminar = tanggalSeminar;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_seminar", length = 0)
	public Date getTanggalSeminar() {
		mahasiswaRequestTugasAkhir = getMahasiswaRequestTugasAkhir();
		if (mahasiswaRequestTugasAkhir != null && mahasiswaRequestTugasAkhir.getTanggalSeminar() != null) {
			tanggalSeminar = mahasiswaRequestTugasAkhir.getTanggalSeminar();
		}
		return tanggalSeminar;
	}

	public void setNilaikomprehensif(Double nilaikomprehensif) {
		this.nilaikomprehensif = nilaikomprehensif;
	}

	@Column(name = "nilaikomprehensif", nullable = true, precision = 15)
	public Double getNilaikomprehensif() {
		return nilaikomprehensif;
	}

	public void setAbstrack(String abstrack) {
		this.abstrack = abstrack;
	}

	@Column(name = "abstrack", columnDefinition = "text")
	public String getAbstrack() {
		if (abstrack == null) {
			abstrack = "";
		}
		return abstrack;
	}

	public void setNilaiPenguji3(Double nilaiPenguji3) {
		this.nilaiPenguji3 = nilaiPenguji3;
	}

	@Column(name = "nilai_penguji_3", nullable = true, precision = 15)
	public Double getNilaiPenguji3() {
		return nilaiPenguji3 == null ? 0.0 : nilaiPenguji3;
	}

	public void setNilaiPenguji4(Double nilaiPenguji4) {
		this.nilaiPenguji4 = nilaiPenguji4;
	}

	@Column(name = "nilai_penguji_4", nullable = true, precision = 15)
	public Double getNilaiPenguji4() {
		return nilaiPenguji4 == null ? 0.0 : nilaiPenguji4;
	}

	public void setNilaiPenguji5(Double nilaiPenguji5) {
		this.nilaiPenguji5 = nilaiPenguji5;
	}

	@Column(name = "nilai_penguji_5", nullable = true, precision = 15)
	public Double getNilaiPenguji5() {
		return nilaiPenguji5 == null ? 0.0 : nilaiPenguji5;
	}

	public void setFormatNilaiSkripsi(FormatNilaiSkripsi formatNilaiSkripsi) {
		this.formatNilaiSkripsi = formatNilaiSkripsi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_nilai_skripsi", nullable = true)
	public FormatNilaiSkripsi getFormatNilaiSkripsi() {
		formatNilaiSkripsi = check(formatNilaiSkripsi);
		return formatNilaiSkripsi;
	}

	public void setPenguji3(Dosen penguji3) {
		this.penguji3 = penguji3;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penguji3", nullable = true)
	public Dosen getPenguji3() {
		mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
		formatNilaiSkripsi = check(formatNilaiSkripsi);
		if (formatNilaiSkripsi != null && !formatNilaiSkripsi.getDosen5Aktif()) {
			penguji3 = null;
		}
		penguji3 = check(penguji3);
		return penguji3;
	}

	public void setPenguji4(Dosen penguji4) {
		this.penguji4 = penguji4;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penguji4", nullable = true)
	public Dosen getPenguji4() {
		mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
		formatNilaiSkripsi = check(formatNilaiSkripsi);
		if (formatNilaiSkripsi != null && !formatNilaiSkripsi.getDosen6Aktif()) {
			penguji4 = null;
		}

		penguji4 = check(penguji4);
		return penguji4;
	}

	public void setPenguji5(Dosen penguji5) {
		this.penguji5 = penguji5;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penguji5", nullable = true)
	public Dosen getPenguji5() {
		mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
		formatNilaiSkripsi = check(formatNilaiSkripsi);
		if (formatNilaiSkripsi != null && !formatNilaiSkripsi.getDosen7Aktif()) {
			penguji5 = null;
		}

		penguji5 = check(penguji5);
		return penguji5;
	}

	public void setJalurSkripsi(String jalurSkripsi) {
		this.jalurSkripsi = jalurSkripsi;
	}

	@Column(name = "jalur_skripsi")
	public String getJalurSkripsi() {
		return jalurSkripsi;
	}

	public void setTipeSkripsi(String tipeSkripsi) {
		this.tipeSkripsi = tipeSkripsi;
	}

	@Column(name = "tipe_skripsi")
	public String getTipeSkripsi() {
		return tipeSkripsi;
	}

	public void setAwalBimbingan(Date awalBimbingan) {
		this.awalBimbingan = awalBimbingan;
	}

	@Column(name = "awal_bimbingan")
	public Date getAwalBimbingan() {
		return awalBimbingan;
	}

	public void setAkhirBimbingan(Date akhirBimbingan) {
		this.akhirBimbingan = akhirBimbingan;
	}

	@Column(name = "akhir_bimbingan")
	public Date getAkhirBimbingan() {
		return akhirBimbingan;
	}

	public void setPembimbing3(Dosen pembimbing3) {
		this.pembimbing3 = pembimbing3;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembimbing3")
	public Dosen getPembimbing3() {
		try {
			mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
			formatNilaiSkripsi = check(formatNilaiSkripsi);
			if (pembimbing3 == null && mahasiswaRequestTugasAkhir != null
					&& mahasiswaRequestTugasAkhir.getDosen3() != null) {
				pembimbing3 = mahasiswaRequestTugasAkhir.getDosen3();
			}

			else if (formatNilaiSkripsi != null && !formatNilaiSkripsi.getDosen21Aktif()) {
				pembimbing3 = null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Skripsi.java:790");
			// TODO: handle exception
		}

		pembimbing3 = check(pembimbing3);
		return pembimbing3;
	}

	public void setRuangSidang(Ruang ruangSidang) {
		this.ruangSidang = ruangSidang;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang_sidang", nullable = true)
	public Ruang getRuangSidang() {
		jadwalSidangTugasAkhir = getJadwalSidangTugasAkhir();
		if (jadwalSidangTugasAkhir != null && jadwalSidangTugasAkhir.getRuangSidang() != null) {
			ruangSidang = jadwalSidangTugasAkhir.getRuangSidang();
		}
		ruangSidang = check(ruangSidang);
		return ruangSidang;
	}

	public void setLulusToefl(Boolean lulusToefl) {
		this.lulusToefl = lulusToefl;
	}

	@Column(name = "lulusToefl")
	public Boolean getLulusToefl() {
		return lulusToefl;
	}

	public Boolean getLulusToafl() {
		return lulusToafl;
	}

	public void setLulusToafl(Boolean lulusToafl) {
		this.lulusToafl = lulusToafl;
	}

	public Long getItemRef() {
		return itemRef;
	}

	public void setItemRef(Long itemRef) {
		this.itemRef = itemRef;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_sidang_tugas_akhir", nullable = true)
	public JadwalSidangTugasAkhir getJadwalSidangTugasAkhir() {
		jadwalSidangTugasAkhir = check(jadwalSidangTugasAkhir);
		return jadwalSidangTugasAkhir;
	}

	public void setJadwalSidangTugasAkhir(JadwalSidangTugasAkhir jadwalSidangTugasAkhir) {
		this.jadwalSidangTugasAkhir = jadwalSidangTugasAkhir;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa_request_tugas_akhir", nullable = true)
	public MahasiswaRequestTugasAkhir getMahasiswaRequestTugasAkhir() {
		mahasiswaRequestTugasAkhir = check(mahasiswaRequestTugasAkhir);
		if (mahasiswaRequestTugasAkhir != null && getId() != null) {
			mahasiswaRequestTugasAkhir.setSkr(getId().toString());
		}
		return mahasiswaRequestTugasAkhir;
	}

	public void setMahasiswaRequestTugasAkhir(MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir) {
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detail_perkuliahan", nullable = true)
	public Detailperkuliahan getDetailperkuliahan() {
		return detailperkuliahan;
	}

	public void setDetailperkuliahan(Detailperkuliahan detailperkuliahan) {
		this.detailperkuliahan = detailperkuliahan;
	}

	public Boolean getSetujuiSidang() {
		if (setujuiSidang == null) {
			setujuiSidang = false;
			if (jadwalSidangTugasAkhir != null) {
				setujuiSidang = true;
			}
		}

		if (getTelahSidang().equals(1)) {
			setujuiSidang = true;
		}

		return setujuiSidang;
	}

	public void setSetujuiSidang(Boolean setujuiSidang) {
		this.setujuiSidang = setujuiSidang;
	}

	@Column(name = "semester", nullable = true)
	public Integer getSemester() {
		if (semester == null && mahasiswa != null) {
			semester = mahasiswa.currentSemester();
		}

		if (semester == null) {
			semester = 0;
		}
		return semester;
	}

	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	@Column(name = "tahun_akademik", nullable = true)
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}

		// if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan()
		// != null) {
		// tahunAkademik = detailperkuliahan.getTahunAkademik();
		// }

		return tahunAkademik;
	}

	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	@Column(columnDefinition = "text")
	public String getKeyword() {
		return keyword == null ? "" : keyword.trim();
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public Boolean getLulus() {
		nilaiHuruf = getNilaiHuruf();
		if (lulus == null && nilaiHuruf != null) {
			if (nilaiHuruf.isEmpty() || nilaiHuruf.toUpperCase().contains("D") || nilaiHuruf.toUpperCase().contains("E")
					|| nilaiHuruf.toUpperCase().contains("T")) {
				lulus = false;
			} else {
				lulus = true;
			}
		} else if (lulus == null) {
			lulus = true;
		}

		if (nilaiHuruf == null) {
			lulus = false;
		}

		return lulus;
	}

	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	public Double getTotalIP() {
		return totalIP == null ? 0.0 : totalIP;
	}

	public void setTotalIP(Double totalIP) {
		this.totalIP = totalIP;
	}

	@SuppressWarnings("unchecked")
	public void reloadSkripsiPunyaKomponenPenilaianSkripsi(Session session, Dosen dosen) {

		refreshNilaiKeDefault(dosen);

		String formatbaru = "";
		List<SkripsiPunyaKomponenPenilaianSkripsi> skripsiPunyaKomponenPenilaianSkripsis = session
				.createCriteria(SkripsiPunyaKomponenPenilaianSkripsi.class)
				.add(Restrictions.eq("formatNilaiSkripsi", formatNilaiSkripsi)).add(Restrictions.isNull("parent"))
				.add(Restrictions.gt("persen", 0.01)).createCriteria("statusPertemuan")
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("id")).list();
		for (SkripsiPunyaKomponenPenilaianSkripsi skripsiPunyaKomponenPenilaianSkripsi : skripsiPunyaKomponenPenilaianSkripsis) {
			try {
				Double jumlah = retreiveDetailNilai(skripsiPunyaKomponenPenilaianSkripsi.getKomponenPenilaianSkripsi(),
						dosen);
				Boolean verivy = retreiveDetailVerifikasiNilai(skripsiPunyaKomponenPenilaianSkripsi, dosen);
				String aformatBaru = skripsiPunyaKomponenPenilaianSkripsi.getKomponenPenilaianSkripsi().getId() + ","
						+ dosen.getId() + "," + jumlah + ",0,"
						+ skripsiPunyaKomponenPenilaianSkripsi.getKomponenPenilaianSkripsi().getBobot() + "," + verivy;
				formatbaru += formatbaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		detailNilai = formatbaru;
	}

	public Double hitungTotalNilai(Boolean gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase, Dosen dosen) {
		return hitungTotalNilai(gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase, dosen, null);
	}

	public Double hitungTotalNilai(Boolean gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase, Dosen dosen,
			List<Long> skripsiPunyaKomponenPenilaianSkripsis) {

		refreshNilaiKeDefault(dosen);
		if (gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase) {
			if (skripsiPunyaKomponenPenilaianSkripsis == null) {
				bersihkanNilaiKeDefault();
			} else {
				bersihkanNilaiKeDefault(skripsiPunyaKomponenPenilaianSkripsis);
			}
		}

		String str = getDetailNilai();
		Double totalPersen = 0.0;
		Double total = 0.0;
		if (str != null && !str.trim().isEmpty()) {
			String[] s = StringUtils.split(str, ";");
			Map<Long, Object[]> nilais = new HashMap<Long, Object[]>();
			for (String ss : s) {
				try {
					String[] sss = StringUtils.split(ss, ",");
					if (sss == null || sss.length < 5 || nilaiKosongAtauNull(sss[0])
							|| nilaiKosongAtauNull(sss[1])) {
						continue;
					}
					Long dosenId = Long.parseLong(sss[1].trim());
					if (dosenId.equals(dosen.getId())) {
						Long idSkripsiPunyaKomponenPenilaianSkripsi = Long.parseLong(sss[0].trim());
						Double persen = parseDoubleNilaiAman(sss[4]);
						if (persen != null) {
							Double n = parseDoubleNilaiAman(sss[2]);
							if (n == null) {
								n = Double.valueOf(0.0);
							}
							nilais.put(idSkripsiPunyaKomponenPenilaianSkripsi, new Object[] { n, persen });

							totalPersen += persen;

						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			System.out.println("totalPersen -> " + totalPersen);

			if (totalPersen > 0.001) {
				for (Long skripsiPunyaKomponenPenilaianSkripsi : nilais.keySet()) {
					try {
						Double n = (Double) nilais.get(skripsiPunyaKomponenPenilaianSkripsi)[0];
						Double persen = (Double) nilais.get(skripsiPunyaKomponenPenilaianSkripsi)[1];
						total += (n * (persen / totalPersen));

						System.out.println("n -> " + n + " persen -> " + persen + " -> total -> " + total);

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		}

		return total;
	}

	private boolean nilaiKosongAtauNull(String nilai) {
		return nilai == null || nilai.trim().length() == 0 || "null".equalsIgnoreCase(nilai.trim());
	}

	private Double parseDoubleNilaiAman(String nilai) {
		if (nilai == null) {
			return null;
		}
		String s = nilai.trim();
		if (s.length() == 0 || "null".equalsIgnoreCase(s)) {
			return null;
		}
		try {
			return Double.valueOf(Double.parseDouble(s));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(nilai-skripsi-tidak-valid-dilewati) src/ais/database/model/Skripsi.java:parseDoubleNilaiAman nilai="
							+ nilai);
			return null;
		}
	}

	public void populateDetailNilai(KomponenPenilaianSkripsi komponenPenilaianSkripsi, Dosen dosen, Double jumlah,
			Boolean verify) {
		if (jumlah != null && jumlah < 0.01) {
			verify = false;
		}
		if (komponenPenilaianSkripsi != null) {
			String formatBaru = "";
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = nn.split(",");
					if (!s[0].trim().isEmpty()) {
						Long formatId = Long.parseLong(s[0]);
						Long dosenId = Long.parseLong(s[1]);
						if (komponenPenilaianSkripsi.getId().equals(formatId) && dosen.getId().equals(dosenId)) {
							aformatBaru = komponenPenilaianSkripsi.getId() + "," + dosen.getId() + "," + jumlah + ",0,"
									+ komponenPenilaianSkripsi.getBobot() + "," + verify;
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
				String aformatBaru = komponenPenilaianSkripsi.getId() + "," + dosen.getId() + "," + jumlah + ",0,"
						+ komponenPenilaianSkripsi.getBobot() + "," + verify;
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			detailNilai = formatBaru;
		}

	}

	@SuppressWarnings("unchecked")
	public void bersihkanNilaiKeDefault() {
		Session session = HibernateUtil.currentSession();
		List<Long> skripsiPunyaKomponenPenilaianSkripsis = session
				.createCriteria(SkripsiPunyaKomponenPenilaianSkripsi.class)
				.createAlias("komponenPenilaianSkripsi", "komponenPenilaianSkripsi")
				.setProjection(Projections.groupProperty("komponenPenilaianSkripsi.id"))
				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianSkripsi.jurusan"),
						Restrictions.eq("komponenPenilaianSkripsi.jurusan", getMahasiswa().getJurusan())))
				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianSkripsi.fakultas"),
						Restrictions.eq("komponenPenilaianSkripsi.fakultas",
								getMahasiswa().getJurusan().getFakultas())))
				.add(Restrictions.or(Restrictions.isNull("komponenPenilaianSkripsi.aktif"),
						Restrictions.eq("komponenPenilaianSkripsi.aktif", true)))
				.add(Restrictions.eq("formatNilaiSkripsi", formatNilaiSkripsi)).list();
		bersihkanNilaiKeDefault(skripsiPunyaKomponenPenilaianSkripsis);
	}

	public void bersihkanNilaiKeDefault(List<Long> skripsiPunyaKomponenPenilaianSkripsis) {
		String formatbaru = "";

		if (detailNilai != null && !detailNilai.trim().isEmpty()) {
			String[] s = StringUtils.split(detailNilai, ";");

			for (String ss : s) {
				String[] sss = StringUtils.split(ss, ",");
				Long idSkripsiPunyaKomponenPenilaianSkripsi = Long.parseLong(sss[0].trim());

				if (skripsiPunyaKomponenPenilaianSkripsis.contains(idSkripsiPunyaKomponenPenilaianSkripsi)) {
					formatbaru += formatbaru.isEmpty() ? ss : ";" + ss;
				}
			}

			detailNilai = formatbaru;
		}
	}

	@SuppressWarnings("unchecked")
	public void refreshNilaiKeDefault(Dosen dosen) {
		if ((detailNilai == null || detailNilai.trim().isEmpty()) && totalNilai != null && totalNilai > 1.0) {
			String formatbaru = "";
			Session session = HibernateUtil.currentSession();
			List<SkripsiPunyaKomponenPenilaianSkripsi> skripsiPunyaKomponenPenilaianSkripsis = session
					.createCriteria(SkripsiPunyaKomponenPenilaianSkripsi.class)
					.createAlias("komponenPenilaianSkripsi", "komponenPenilaianSkripsi")

					.add(Restrictions.or(Restrictions.isNull("komponenPenilaianSkripsi.jurusan"),
							Restrictions.eq("komponenPenilaianSkripsi.jurusan", getMahasiswa().getJurusan())))
					.add(Restrictions.or(Restrictions.isNull("komponenPenilaianSkripsi.fakultas"),
							Restrictions.eq("komponenPenilaianSkripsi.fakultas",
									getMahasiswa().getJurusan().getFakultas())))

					.add(Restrictions.or(Restrictions.isNull("komponenPenilaianSkripsi.aktif"),
							Restrictions.eq("komponenPenilaianSkripsi.aktif", true)))
					.add(Restrictions.eq("formatNilaiSkripsi", formatNilaiSkripsi)).list();

			for (SkripsiPunyaKomponenPenilaianSkripsi skripsiPunyaKomponenPenilaianSkripsi : skripsiPunyaKomponenPenilaianSkripsis) {

				String aformatBaru = skripsiPunyaKomponenPenilaianSkripsi.getKomponenPenilaianSkripsi().getId() + ","
						+ dosen.getId() + "," + totalNilai + ",0,"
						+ skripsiPunyaKomponenPenilaianSkripsi.getKomponenPenilaianSkripsi().getBobot() + ",false";
				formatbaru += formatbaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}
			skripsiPunyaKomponenPenilaianSkripsis = null;
			detailNilai = formatbaru;
		}
	}

	public Double retreiveDetailNilai(KomponenPenilaianSkripsi formatIdSource, Dosen dosen) {

		refreshNilaiKeDefault(dosen);

		if (formatIdSource != null && formatIdSource.getId() != null) {
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					// GUARD NumberFormatException "For input string: \"\"": token bisa kosong
					// (mis. detailNilai berformat "id,,nilai,..." atau entri belum lengkap).
					// trim+isEmpty dulu sebelum parseLong/parseDouble -- token kosong DILEWATI
					// (continue ke entri berikutnya di "nilais"), tanpa merusak parsing entri lain.
					if (s.length < 3 || !tokenAngkaValid(s[0]) || !tokenAngkaValid(s[1]) || !tokenAngkaValid(s[2])) {
						continue;
					}
					Long formatId = Long.parseLong(s[0].trim());
					Long dosenId = Long.parseLong(s[1].trim());
					if (formatIdSource.getId().equals(formatId) && dosen.getId().equals(dosenId)) {
						return Double.parseDouble(s[2].trim());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Skripsi.java:1185");

				}
			}
		}

		return 0.0;
	}

	public Boolean retreiveDetailVerifikasiNilai(SkripsiPunyaKomponenPenilaianSkripsi formatIdSource, Dosen dosen) {

		refreshNilaiKeDefault(dosen);

		if (formatIdSource != null && formatIdSource.getId() != null) {
			String[] nilais = detailNilai == null ? new String[] {} : detailNilai.split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					// token kosong/tak lengkap dilewati agar tidak NumberFormatException (pola sama dgn retreiveDetailNilai)
					if (s.length < 3 || !tokenAngkaValid(s[0]) || !tokenAngkaValid(s[1]) || !tokenAngkaValid(s[2])) {
						continue;
					}
					Long formatId = Long.parseLong(s[0].trim());
					Long dosenId = Long.parseLong(s[1].trim());
					if (formatIdSource.getId().equals(formatId) && dosen.getId().equals(dosenId)) {
						if (Double.parseDouble(s[2].trim()) < 0.01) {
							return false;
						}
						return s.length > 5 && Boolean.parseBoolean(s[5]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Skripsi.java:1211");

				}
			}
		}

		return false;
	}

	private boolean tokenAngkaValid(String nilai) {
		return nilai != null && !nilai.trim().isEmpty() && !"null".equalsIgnoreCase(nilai.trim());
	}

	@Column(columnDefinition = "text")
	public String getDetailNilai() {
		return detailNilai == null ? "" : detailNilai.trim();
	}

	public void setDetailNilai(String detailNilai) {
		this.detailNilai = detailNilai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "format_nilai", nullable = true)
	public FormatNilai getFormatNilai() {
		return formatNilai;
	}

	public void setFormatNilai(FormatNilai formatNilai) {
		this.formatNilai = formatNilai;
	}

	@Column(columnDefinition = "text")
	public String getLokasiUjian() {
		return lokasiUjian == null ? "" : lokasiUjian;
	}

	public void setLokasiUjian(String lokasiUjian) {
		this.lokasiUjian = lokasiUjian;
	}

	public String getNomorSk() {
		return nomorSk == null ? "" : nomorSk;
	}

	public void setNomorSk(String nomorSk) {
		this.nomorSk = nomorSk;
	}

	@Temporal(TemporalType.DATE)
	public Date getTglSk() {
		return tglSk;
	}

	public void setTglSk(Date tglSk) {
		this.tglSk = tglSk;
	}

	public Integer getSelesaiDalamBulan() {
		if (getAwalBimbingan() != null && getAkhirBimbingan() != null) {
			LocalDate jamesBirthDay = new LocalDate(getAwalBimbingan());
			LocalDate now = new LocalDate(getAkhirBimbingan());
			selesaiDalamBulan = Months.monthsBetween(jamesBirthDay, now).getMonths();
		}
		return selesaiDalamBulan;
	}

	public void setSelesaiDalamBulan(Integer selesaiDalamBulan) {
		this.selesaiDalamBulan = selesaiDalamBulan;
	}

	public Integer getTahun() {
		String tahunAkademikAktif = getTahunAkademik();
		Integer tahunDariTahunAkademik = ekstrakTahunAkademik(tahunAkademikAktif);
		if (tahunDariTahunAkademik != null) {
			tahun = tahunDariTahunAkademik;
		}
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	private Integer ekstrakTahunAkademik(String tahunAkademikAktif) {
		if (tahunAkademikAktif == null) {
			return null;
		}
		String nilai = tahunAkademikAktif.trim();
		if (nilai.length() == 0) {
			return null;
		}
		String[] bagian = StringUtils.split(nilai, "/");
		Integer tahunAwal = parseTahunAkademik(bagian != null && bagian.length > 0 ? bagian[0] : nilai);
		if (tahunAwal != null) {
			return tahunAwal;
		}
		for (int i = 0; i <= nilai.length() - 4; i++) {
			String kandidat = nilai.substring(i, i + 4);
			tahunAwal = parseTahunAkademik(kandidat);
			if (tahunAwal != null) {
				return tahunAwal;
			}
		}
		return null;
	}

	private Integer parseTahunAkademik(String nilai) {
		if (nilai == null) {
			return null;
		}
		nilai = nilai.trim();
		if (nilai.length() != 4) {
			return null;
		}
		for (int i = 0; i < nilai.length(); i++) {
			if (!Character.isDigit(nilai.charAt(i))) {
				return null;
			}
		}
		int tahunParsed = Integer.valueOf(nilai).intValue();
		if (tahunParsed < 1900 || tahunParsed > 2200) {
			return null;
		}
		return Integer.valueOf(tahunParsed);
	}

	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_sidang_tugas_akhir", nullable = true)
	public GelombangPendaftaranSidangTugasAkhir getGelombangPendaftaranSidangTugasAkhir() {
		gelombangPendaftaranSidangTugasAkhir = check(gelombangPendaftaranSidangTugasAkhir);
		return gelombangPendaftaranSidangTugasAkhir;
	}

	public void setGelombangPendaftaranSidangTugasAkhir(
			GelombangPendaftaranSidangTugasAkhir gelombangPendaftaranSidangTugasAkhir) {
		this.gelombangPendaftaranSidangTugasAkhir = gelombangPendaftaranSidangTugasAkhir;
	}

	@Column(name = "judulen", columnDefinition = "text")
	public String getJudulen() {
		return judulen == null ? getJudul() : judulen.trim();
	}

	public void setJudulen(String judulen) {
		this.judulen = judulen;
	}

	@Override
	public String ambilTahunAkademik() {
		return getTahunAkademik();
	}

	@Override
	public Integer ambilSemester() {
		return getSemester();
	}

	@Override
	public String ambilJenisSemester() {
		return getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
	}

	@Override
	public VOPembelajaran ambilVOPembelajaran() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 1;
	}

	public Boolean getPersetujuanPembimbing1() {
		return persetujuanPembimbing1 == null ? false : persetujuanPembimbing1;
	}

	public void setPersetujuanPembimbing1(Boolean persetujuanPembimbing1) {
		this.persetujuanPembimbing1 = persetujuanPembimbing1;
	}

	public Boolean getPersetujuanPembimbing2() {
		return persetujuanPembimbing2 == null ? false : persetujuanPembimbing2;
	}

	public void setPersetujuanPembimbing2(Boolean persetujuanPembimbing2) {
		this.persetujuanPembimbing2 = persetujuanPembimbing2;
	}

	public Boolean getPersetujuanPembimbing3() {
		return persetujuanPembimbing3 == null ? false : persetujuanPembimbing3;
	}

	public void setPersetujuanPembimbing3(Boolean persetujuanPembimbing3) {
		this.persetujuanPembimbing3 = persetujuanPembimbing3;
	}

	public Boolean getPersetujuanPenguji1() {
		if (getNilaiPenguji1() > 0.1) {
			persetujuanPenguji1 = true;
		}
		return persetujuanPenguji1 == null ? false : persetujuanPenguji1;
	}

	public void setPersetujuanPenguji1(Boolean persetujuanPenguji1) {
		this.persetujuanPenguji1 = persetujuanPenguji1;
	}

	public Boolean getPersetujuanPenguji2() {
		if (getNilaiPenguji2() > 0.1) {
			persetujuanPenguji2 = true;
		}
		return persetujuanPenguji2 == null ? false : persetujuanPenguji2;
	}

	public void setPersetujuanPenguji2(Boolean persetujuanPenguji2) {
		this.persetujuanPenguji2 = persetujuanPenguji2;
	}

	public Boolean getPersetujuanPenguji3() {
		if (getNilaiPenguji3() > 0.1) {
			persetujuanPenguji3 = true;
		}
		return persetujuanPenguji3 == null ? false : persetujuanPenguji3;
	}

	public void setPersetujuanPenguji3(Boolean persetujuanPenguji3) {
		this.persetujuanPenguji3 = persetujuanPenguji3;
	}

	public Boolean getPersetujuanPenguji4() {
		if (getNilaiPenguji4() > 0.1) {
			persetujuanPenguji4 = true;
		}
		return persetujuanPenguji4 == null ? false : persetujuanPenguji4;
	}

	public void setPersetujuanPenguji4(Boolean persetujuanPenguji4) {
		this.persetujuanPenguji4 = persetujuanPenguji4;
	}

	public Boolean getPersetujuanPenguji5() {
		if (getNilaiPenguji5() > 0.1) {
			persetujuanPenguji5 = true;
		}
		return persetujuanPenguji5 == null ? false : persetujuanPenguji5;
	}

	public void setPersetujuanPenguji5(Boolean persetujuanPenguji5) {
		this.persetujuanPenguji5 = persetujuanPenguji5;
	}

	private String course;
	private String catatanPenting;
	private Boolean tanpaPerbaikan;
	private String catatanDosen;
	private Boolean urutkanotomatis;

	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	@Column(columnDefinition = "text")
	public String getCatatanPenting() {
		if (catatanPenting == null || catatanPenting.trim().isEmpty()) {
			catatanPenting = "Batas waktu penyelesaian perbaikan naskah Tugas Akhir adalah maksimal 14 (empat belas) hari kerja, "
					+ "terhitung sejak hari ini tanggal ................ sampai dengan tanggal ................ "
					+ "Hari kerja dihitung dari Senin sampai Sabtu, tidak termasuk hari minggu, libur nasional dan/atau cuti bersama.\n\n";
			catatanPenting += "Apabila mahasiswa terlambat menyelesaikan perbaikan melewati batas waktu yang telah ditentukan "
					+ "di berita acara, maka nilai Tugas Akhir akan diturunkan satu tingkat.\n\n";
			catatanPenting += "Apabila melewati 2 (dua) minggu dari batas waktu yang telah dituliskan di berita acara, maka "
					+ "mahasiswa wajib mengikuti ujian ulang sesuai dengan ketentuan dari Akademik.\n";
		}
		return catatanPenting;
	}

	public void setCatatanPenting(String catatanPenting) {
		this.catatanPenting = catatanPenting;
	}

	public Boolean getTanpaPerbaikan() {
		return tanpaPerbaikan == null ? false : tanpaPerbaikan;
	}

	public void setTanpaPerbaikan(Boolean tanpaPerbaikan) {
		this.tanpaPerbaikan = tanpaPerbaikan;
	}

	@Column(columnDefinition = "text")
	public String getCatatanDosen() {
		return catatanDosen == null || catatanDosen.trim().isEmpty() ? new JSONObject().toString()
				: catatanDosen.trim();
	}

	public void setCatatanDosen(String catatanDosen) {
		this.catatanDosen = catatanDosen;
	}

	public List<CommonVO> dataDosen(boolean semua) {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		Skripsi skripsi = this;
		if (skripsi.getFormatNilaiSkripsi() == null) {
			return commonVOs;
		}

		if ((semua || skripsi.getPembimbing() != null) && skripsi.getFormatNilaiSkripsi().getDosen1Aktif()) {
			commonVOs
					.add(new CommonVO(skripsi.getPembimbing() == null ? "" : skripsi.getPembimbing().getId().toString(),
							skripsi.getFormatNilaiSkripsi().getDosen1(), skripsi.getPembimbing(),
							skripsi.getFormatNilaiSkripsi().getKode1()));
		}
		if ((semua || skripsi.getKetuaSidang() != null) && skripsi.getFormatNilaiSkripsi().getDosen2Aktif()) {
			commonVOs.add(
					new CommonVO(skripsi.getKetuaSidang() == null ? "" : skripsi.getKetuaSidang().getId().toString(),
							skripsi.getFormatNilaiSkripsi().getDosen2(), skripsi.getKetuaSidang(),
							skripsi.getFormatNilaiSkripsi().getKode2()));
		}

		if ((semua || skripsi.getPembimbing3() != null) && skripsi.getFormatNilaiSkripsi().getDosen21Aktif()) {
			commonVOs.add(
					new CommonVO(skripsi.getPembimbing3() == null ? "" : skripsi.getPembimbing3().getId().toString(),
							skripsi.getFormatNilaiSkripsi().getDosen21(), skripsi.getPembimbing3(),
							skripsi.getFormatNilaiSkripsi().getKode21()));
		}
		if ((semua || skripsi.getPenguji1() != null) && skripsi.getFormatNilaiSkripsi().getDosen3Aktif()) {
			commonVOs.add(new CommonVO(skripsi.getPenguji1() == null ? "" : skripsi.getPenguji1().getId().toString(),
					skripsi.getFormatNilaiSkripsi().getDosen3(), skripsi.getPenguji1(),
					skripsi.getFormatNilaiSkripsi().getKode3()));
		}
		if ((semua || skripsi.getPenguji2() != null) && skripsi.getFormatNilaiSkripsi().getDosen4Aktif()) {
			commonVOs.add(new CommonVO(skripsi.getPenguji2() == null ? "" : skripsi.getPenguji2().getId().toString(),
					skripsi.getFormatNilaiSkripsi().getDosen4(), skripsi.getPenguji2(),
					skripsi.getFormatNilaiSkripsi().getKode4()));
		}
		if ((semua || skripsi.getPenguji3() != null) && skripsi.getFormatNilaiSkripsi().getDosen5Aktif()) {
			commonVOs.add(new CommonVO(skripsi.getPenguji3() == null ? "" : skripsi.getPenguji3().getId().toString(),
					skripsi.getFormatNilaiSkripsi().getDosen5(), skripsi.getPenguji3(),
					skripsi.getFormatNilaiSkripsi().getKode5()));
		}
		if ((semua || skripsi.getPenguji4() != null) && skripsi.getFormatNilaiSkripsi().getDosen6Aktif()) {
			commonVOs.add(new CommonVO(skripsi.getPenguji4() == null ? "" : skripsi.getPenguji4().getId().toString(),
					skripsi.getFormatNilaiSkripsi().getDosen6(), skripsi.getPenguji4(),
					skripsi.getFormatNilaiSkripsi().getKode6()));
		}

		if ((semua || skripsi.getPenguji5() != null) && skripsi.getFormatNilaiSkripsi().getDosen7Aktif()) {
			commonVOs.add(new CommonVO(skripsi.getPenguji5() == null ? "" : skripsi.getPenguji5().getId().toString(),
					skripsi.getFormatNilaiSkripsi().getDosen7(), skripsi.getPenguji5(),
					skripsi.getFormatNilaiSkripsi().getKode7()));
		}

		return commonVOs;
	}

	public Double cariNilaiDariDosen(Dosen dosen, String jenis,
			Boolean gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase) {
		Skripsi skripsi = this;
		Double nilaiPembimbing = 0.0;
		if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen1())) {
			skripsi.setNilaiKetuaSidang(nilaiPembimbing = skripsi
					.hitungTotalNilai(gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase, dosen));
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen2())) {
			skripsi.setNilaiPembimbing(nilaiPembimbing = skripsi
					.hitungTotalNilai(gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase, dosen));
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen21())) {
			skripsi.setNilaiPembimbing3(nilaiPembimbing = skripsi
					.hitungTotalNilai(gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase, dosen));
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen3())) {
			skripsi.setNilaiPenguji1(nilaiPembimbing = skripsi
					.hitungTotalNilai(gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase, dosen));
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen4())) {
			skripsi.setNilaiPenguji2(nilaiPembimbing = skripsi
					.hitungTotalNilai(gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase, dosen));
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen5())) {
			skripsi.setNilaiPenguji3(nilaiPembimbing = skripsi
					.hitungTotalNilai(gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase, dosen));
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen6())) {
			skripsi.setNilaiPenguji4(nilaiPembimbing = skripsi
					.hitungTotalNilai(gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase, dosen));
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen7())) {
			skripsi.setNilaiPenguji5(nilaiPembimbing = skripsi
					.hitungTotalNilai(gunakanSkripsiPunyaKomponenPenilaianSkripsiDariDatabase, dosen));
		}
		return nilaiPembimbing;
	}

	public void simpanDosen(Dosen dosen, String jenis) {
		Skripsi skripsi = this;
		if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen1())) {
			skripsi.setPembimbing(dosen);
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen2())) {
			skripsi.setKetuaSidang(dosen);
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen21())) {
			skripsi.setPembimbing3(dosen);
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen3())) {
			skripsi.setPenguji1(dosen);
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen4())) {
			skripsi.setPenguji2(dosen);
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen5())) {
			skripsi.setPenguji3(dosen);
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen6())) {
			skripsi.setPenguji4(dosen);
		} else if (jenis.equals(skripsi.getFormatNilaiSkripsi().getDosen7())) {
			skripsi.setPenguji5(dosen);
		}
	}

	@Column(columnDefinition = "text")
	public String getReferensi() {

		if (referensi == null || referensi.trim().isEmpty() || referensi.trim().equals(new JSONArray().toString())) {
			try {
				if (mahasiswaRequestTugasAkhir != null
						&& !mahasiswaRequestTugasAkhir.getReferensi().equals(new JSONArray().toString())) {
					referensi = mahasiswaRequestTugasAkhir.getReferensi();
				}
			} catch (org.hibernate.LazyInitializationException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Skripsi.java:1594");
				// Proxy mahasiswaRequestTugasAkhir tidak bisa diinisialisasi (entity Skripsi sudah
				// detached / session ditutup, mis. dari onAddExternal). Jangan memaksa load lazy —
				// pakai nilai referensi yang sudah ada agar tidak melempar LazyInitializationException.
			}
		}

		return referensi == null || referensi.trim().isEmpty() ? new JSONArray().toString() : referensi.trim();
	}

	public void setReferensi(String referensi) {
		this.referensi = referensi;
	}

	public Double getNilaiPembimbing3() {
		return nilaiPembimbing3 == null ? 0.0 : nilaiPembimbing3;
	}

	public void setNilaiPembimbing3(Double nilaiPembimbing3) {
		this.nilaiPembimbing3 = nilaiPembimbing3;
	}

	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	public Boolean getSembunyikanNilaiKemahasiswa() {
		return sembunyikanNilaiKemahasiswa == null ? false : sembunyikanNilaiKemahasiswa;
	}

	public void setSembunyikanNilaiKemahasiswa(Boolean sembunyikanNilaiKemahasiswa) {
		this.sembunyikanNilaiKemahasiswa = sembunyikanNilaiKemahasiswa;
	}

	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

	private DisposisiSop disposisiSop;
	private Boolean aktif;

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
	
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif; 
	}
}
