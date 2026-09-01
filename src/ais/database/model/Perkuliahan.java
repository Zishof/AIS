package ais.database.model;

// Generated Dec 12, 2009 7:42:38 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.io.Serializable;
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

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Combobox;

import ais.action.master.RencanaTahunAkademikAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyMessageboxConfig;

/**
 * Entity Hibernate untuk <b>kelas kuliah</b>: SATU penawaran konkret dari sebuah mata kuliah
 * pada satu tahun akademik, semester, dan kelas tertentu.
 *
 * <p><b>Beda dengan {@link Matakuliah}.</b> {@code Matakuliah} adalah definisi generik di
 * kurikulum — kode, nama, bobot SKS, deskripsi pembelajaran — dan tidak terikat waktu.
 * {@code Perkuliahan} adalah <i>instansiasi</i> mata kuliah itu untuk satu periode: mata kuliah
 * yang sama dapat melahirkan banyak baris {@code Perkuliahan} (kelas A, B, C; ganjil/genap;
 * semester pendek; tahun akademik berbeda). Karena itu bobot SKS TIDAK disimpan di sini, melainkan
 * selalu dibaca dari {@code getMatakuliah().getSks()} (lihat {@link #info(Dosen)} dan
 * {@link #checkMaksSksDosen(Dosen, String, String, Integer, Integer, Long)}).</p>
 *
 * <p>Baris ini adalah unit inti hampir seluruh proses akademik AIS: penjadwalan pertemuan,
 * pengambilan KRS, presensi/kehadiran, penilaian, e-learning, sampai ekspor Feeder PDDikti.</p>
 *
 * <h3>Identitas satu kelas kuliah</h3>
 * <ul>
 * <li>{@code matakuliah} — mata kuliah yang ditawarkan ({@link Matakuliah}).</li>
 * <li>{@code tahunAjaran} (mis. {@code "2025/2026"}), {@code semester} (angka semester kurikulum),
 * {@code ganjilGenap} ({@link #GANJIL}/{@link #GENAP}/{@link #SP}) dan
 * {@code statusSemesterPendek} ({@link #SEMESTER_PENDEK}).</li>
 * <li>{@code kelas} — label paralel kelas ("A", "B", ...); dapat diikat ke master
 * {@link Kelas} lewat {@code kelasref} (bila terisi, nama master menimpa teks bebas).</li>
 * <li>{@code jurusan}, {@code program} (Reguler/Karyawan/...), {@code kurikulum} dan
 * {@link KurikulumPunyaMatakuliah} (baris kurikulum yang memuat mata kuliah ini).</li>
 * </ul>
 *
 * <h3>Jadwal</h3>
 * {@code hari}, {@code waktuMulai}/{@code waktuSelesai} (String "HH.mm") beserta kembarannya dalam
 * bentuk numerik {@code waktuMulaiD}/{@code waktuSelesaiD} (dipakai untuk deteksi bentrok), {@code ruang}
 * ({@link Ruang}), {@code jamPerkuliahan} ({@link JamPerkuliahan} — bila terisi, jam master menimpa
 * teks bebas), {@code masaPerkuliahan} ({@link MasaPerkuliahan}), serta {@code tanggalMulaiPerkuliahan}
 * yang dihitung otomatis (lihat {@link #getTanggalMulaiPerkuliahan()}). Kelas tanpa jadwal ditandai
 * {@code merupakan_tanpa_jadwal_perkuliahan} dan mengosongkan hari/jam/ruang saat dibaca.
 *
 * <h3>Pola 10 slot dosen</h3>
 * Dosen pengampu TIDAK disimpan sebagai koleksi, melainkan sebagai sepuluh kolom terpisah
 * {@code dosen1} .. {@code dosen10} (masing-masing {@code @ManyToOne} ke {@link Dosen}), dengan
 * sepuluh kolom pendamping {@code feeder1} .. {@code feeder10} untuk id Feeder tiap pengampu.
 * Pola sepuluh slot yang sama muncul juga di beberapa entity lain dalam repo ini.
 * Jumlah slot terisi diringkas oleh {@link #getJumlahDosen()}, dan slot ke-<i>n</i> otomatis
 * dinolkan oleh getter-nya bila {@code getJumlahDosen()} kurang dari <i>n</i>. Iterasi atas pengampu
 * sebaiknya lewat {@code populateDosen()}/{@code populateDosenBuNama()}/{@code populateDosenBuId()}
 * dan pengecekan keanggotaan lewat {@code ada(Dosen)}, semuanya milik {@link VOPembelajaran},
 * bukan dengan menulis ulang sepuluh cabang {@code if}.
 *
 * <h3>Peserta, paralel, dan "flag store"</h3>
 * Peserta kelas adalah baris {@link Detailperkuliahan} (satu baris per mahasiswa per kelas, hasil
 * KRS). Daftar peserta TIDAK dibaca dari DB setiap kali, melainkan dari <i>flag store</i> berupa
 * berkas JSON di luar basis data ({@code detail_perkuliahan_<id>}) yang dibangun ulang oleh
 * {@link #reInitDetailperkuliahan(Session)} dan dibaca oleh keluarga
 * {@link #ambilDetailperkuliahan()}. Pola berkas JSON yang sama dipakai untuk asisten mahasiswa
 * ({@code MahasiswaJadiAsisten_<id>}), format nilai ({@code perkuliahan_punya_format_nilai_<id>}),
 * dan daftar kelas paralel ({@code paralel_<id>}). Penanda "sudah dibangun" disimpan lewat
 * {@code udah(String)}/{@code belum(String)} milik {@link GeneralValueObject}.
 *
 * <p><b>Kelas paralel.</b> Bila {@code perkuliahan_paralel} terisi, kelas ini "menempel" pada kelas
 * induk: hampir semua pembacaan peserta, format nilai, dan lokasi berkas JSON <i>di-short-circuit</i>
 * dan didelegasikan ke induknya. Akibatnya jumlah mahasiswa kelas paralel tidak pernah ditulis dari
 * jalur biasa — {@link #singkronkan(Session)} langkah (5) sengaja menambal hal ini.</p>
 *
 * <h3>Penilaian</h3>
 * {@code pembombotanNilai} ({@link PembombotanNilai}) menentukan komposisi komponen nilai, yang
 * dijabarkan menjadi baris {@link FormatNilai} lewat {@link #ambilFormatNilai(Session, boolean, boolean)}.
 * {@code dikunci} ({@link Tbmuser}) menandai kelas yang sedang dikunci seorang dosen; saat terkunci,
 * pembobotan dibekukan lewat {@code pembombotanNilaiBackup} sehingga perubahan konfigurasi global tidak
 * mengubah nilai yang sudah diproses. Ringkasan status kelas tersedia lewat {@link #ambilStatusKrs()},
 * {@link #ambilStatusPenilaian()}, {@link #populateInfoPersetujuan()}, dan konstanta
 * {@link #BELUM_ADA_MAHASISWA} .. {@link #SUDAH_DINILAI}.
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 * <li><b>Sinkronisasi</b> — {@link #singkronkan(Session)}, {@link #singkronkanMhs(Session)},
 * {@link #udah()}, {@link #belum()}.</li>
 * <li><b>Getter/setter kolom</b> — mayoritas isi berkas; banyak di antaranya BUKAN pembaca murni
 * (lihat catatan di bawah).</li>
 * <li><b>Utilitas statis</b> — {@link #checkDosen(Combobox, Combobox, Combobox, Component, Component,
 * Component, Component, Component, Component, Component, Component, Component, Component, Integer, Long)},
 * {@link #checkMaksSksDosen(Dosen, String, String, Integer, Integer, Long)},
 * {@link #hitungStatus(List, Long)}.</li>
 * <li><b>Flag store JSON</b> — pasangan {@code ambilLokasi*}/{@code tulisLokasi*}/{@code populate*}/
 * {@code remove*}/{@code reInit*} untuk peserta, asisten, format nilai, dan paralel.</li>
 * <li><b>Query peserta &amp; ringkasan</b> — {@link #ambilMahasiswa()}, {@link #ambilMahasiswaId(boolean)},
 * {@link #ambilDetailperkuliahan()} dan overload-nya, {@link #ambilStatusKrs()},
 * {@link #populateInfoPersetujuan()}.</li>
 * <li><b>Asisten mahasiswa</b> — {@link #ambilAsisten()}, {@link #merupakanAsisten(Mahasiswa)},
 * {@link #merupakanAsistenAbsen(Mahasiswa)}, {@link #merupakanAsistenNilai(Mahasiswa)}.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang perlu diingat</h3>
 * <ul>
 * <li><b>Getter berefek samping.</b> Banyak getter di sini menulis balik ke field, membuka
 * {@link Session} Hibernate, membaca konfigurasi global, bahkan menyimpan ke DB. Contoh:
 * {@link #getSemester()} memaksa {@code -1} untuk pra-perkuliahan, {@link #getHari()} menormalkan
 * "Jumat" menjadi "Jum'at", {@link #getDikunci()} membatalkan kunci bila penguncinya sudah tidak
 * mengampu, {@link #getKurikulumPunyaMatakuliah()} menambahkan id dosen ke baris kurikulum, dan
 * {@link #ambilKurikulumPunyaMatakuliah()} dapat menyimpan hasil pencariannya ke DB. Jangan
 * menganggap getter di sini murni/bebas efek samping.</li>
 * <li><b>Setter yang menolak nilai kosong.</b> {@link #setOleh(String)}, {@link #setOlehId(String)},
 * dan {@link #setPembombotanNilai(PembombotanNilai)} DIAM-DIAM mengabaikan argumen null/kosong —
 * nilai lama dipertahankan.</li>
 * <li><b>Query di dalam getter memakai {@link FlushMode#MANUAL}</b> pada beberapa tempat untuk
 * mencegah autoFlush memanggil getter lagi dan menimbulkan rekursi tak berujung.</li>
 * <li>{@code semesterPerkuliahan} sudah tidak dipakai (lihat komentar pada fieldnya).</li>
 * </ul>
 *
 * <p>Kontrak umum {@code id}/{@code equals}/{@code compareTo}/{@code check(...)}/{@code write()}/
 * {@code udah(...)} diwarisi dan dijelaskan di {@link GeneralValueObject}; perilaku pertemuan,
 * statistik kehadiran, dan iterasi dosen dijelaskan di {@link VOPembelajaran}.</p>
 *
 * @see Matakuliah
 * @see Detailperkuliahan
 * @see VOPembelajaran
 * @see GeneralValueObject
 * @see ais.action.master.helper.PenjadwalanHelper
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "perkuliahan")
public class Perkuliahan extends VOPembelajaran {
	/*
	 * Lock ber-strip untuk operasi read-modify-write JSON peserta. Beberapa listener Hibernate
	 * dapat memanggil populate/remove bersamaan untuk kelas yang sama; tanpa lock, dua thread
	 * membaca snapshot yang sama lalu saling menimpa dan cache dapat terisi JSON setengah jadi.
	 * Jumlah lock tetap agar tidak menambah map tanpa batas untuk setiap ID perkuliahan.
	 */
	private static final Object[] DETAIL_PERKULIAHAN_LOCKS = new Object[257];
	static {
		for (int i = 0; i < DETAIL_PERKULIAHAN_LOCKS.length; i++) {
			DETAIL_PERKULIAHAN_LOCKS[i] = new Object();
		}
	}

	/** Nilai {@code status_penilaian}: kelas belum punya peserta sama sekali. */
	public static final Integer BELUM_ADA_MAHASISWA = -1;
	/** Nilai {@code status_penilaian}: sudah ada peserta, tetapi belum satu pun dinilai. */
	public static final Integer BELUM_DINILAI = 0;
	/** Nilai {@code status_penilaian}: sebagian besar peserta belum dinilai. */
	public static final Integer SEBAGIAN_BESAR_BELUM_DINILAI = 1;
	/** Nilai {@code status_penilaian}: sebagian besar peserta sudah dinilai. */
	public static final Integer SEBAGIAN_BESAR_SUDAH_DINILAI = 2;
	/** Nilai {@code status_penilaian}: seluruh peserta sudah dinilai. */
	public static final Integer SUDAH_DINILAI = 3;

	/** Label semester genap, dipakai sebagai isi kolom {@code ganjil_genap}. */
	public static final String GENAP = "Genap";
	/** Label semester ganjil, dipakai sebagai isi kolom {@code ganjil_genap}. */
	public static final String GANJIL = "Ganjil";
	/**
	 * Label semester pendek, dipakai sebagai isi kolom {@code ganjil_genap} sekaligus kunci
	 * terjemahan {@code Common.getBahasaConfig(...)}.
	 */
	public static final String SP = "Semester Pendek";

	/** Nilai kolom {@code status_semesterpendek} untuk kelas semester pendek. */
	public static final Integer SEMESTER_PENDEK = 1;
	/** Penanda kelas ekstra; bernilai sama dengan {@link #SEMESTER_PENDEK}. */
	public static final Integer EKSTRA = 1;

	/**
	 * Penanda sementara (tidak dipersist) bahwa objek ini diperoleh sebagai anggota daftar kelas
	 * paralel. Diisi {@code true} oleh {@link #ambilParalel(Dosen)} agar pemanggil dapat
	 * membedakan kelas paralel dari kelas yang sedang dibuka.
	 */
	public transient boolean flagParalel = false;
	/**
	 * Versi serialisasi Java. Jangan diubah kecuali struktur field berubah tidak kompatibel;
	 * objek {@code Perkuliahan} ikut diserialisasi ke berkas cache/flag store.
	 */
	private static final long serialVersionUID = -6970840500825359503L;

	/**
	 * Membangun ulang SELURUH data turunan (flag store) milik kelas kuliah ini dari basis data.
	 *
	 * <p>Ini adalah aksi di balik tombol "Sinkronisasi" pada layar perkuliahan dan dipanggil pula
	 * dari batch pemeliharaan. Urutan langkahnya:</p>
	 * <ol>
	 * <li>{@link #singkronkanMhs(Session)} — bangun ulang flag store peserta sekaligus simpan
	 * {@code jumlah_mahasiswa} ke kolom.</li>
	 * <li>{@code reInitPertemuan(session)} — pertemuan, diskusi, daftar ujian, tugas, tugas
	 * kelompok, dan izin (lihat {@link VOPembelajaran}).</li>
	 * <li>{@code reInitUjian(session)} — peserta dan soal tiap ujian.</li>
	 * <li>{@link #reInitPerkuliahanDosen()}, {@code PembombotanNilai.setDefaultPembobotan(...)},
	 * {@link #reInitParalel(Session)}, {@link #reInitMahasiswaJadiAsisten(Session)}.</li>
	 * <li>Penjaminan {@code jumlah_mahasiswa} tersimpan, termasuk untuk kelas paralel yang
	 * di-short-circuit oleh {@link #ambilDetailperkuliahan(boolean, boolean)}.</li>
	 * <li>{@code write()} — persist seluruh flag hasil sinkronisasi.</li>
	 * <li>Buka kunci kelas bila {@link #getDikunci()} menunjuk dosen yang sudah tidak mengampu;
	 * langkah ini MENULIS ke DB dalam transaksinya sendiri.</li>
	 * </ol>
	 *
	 * <p><b>Isolasi kegagalan.</b> Setiap langkah dibungkus {@code try/catch} TERSENDIRI dengan
	 * sengaja: dulu semuanya berada dalam satu blok {@code try}, sehingga kegagalan langkah awal
	 * membatalkan seluruh langkah berikutnya dan hasil sinkron tidak jadi tertulis. Method ini
	 * karena itu tidak pernah melempar exception; kegagalan dicatat lewat
	 * {@code ErrorAuditUtil.record(...)}.</p>
	 *
	 * <p><b>Efek samping:</b> menulis banyak berkas flag store, memperbarui kolom
	 * {@code jumlah_mahasiswa}, dapat membuka/menutup transaksi pada {@code session}, dan dapat
	 * mengosongkan kolom {@code dikunci}.</p>
	 *
	 * <p>Dipanggil antara lain dari {@code PerkuliahanAction}, {@code PenilaianAction},
	 * {@code DosenAction}, {@code DetailperkuliahanHelper}, dan
	 * {@code CommonAcademicKrsNilaiHelper}.</p>
	 *
	 * @param session session Hibernate aktif; dipakai untuk query dan (pada langkah 7) transaksi
	 *                buka kunci. Tidak ditutup oleh method ini.
	 */
	public void singkronkan(Session session) {
		// Setiap langkah dibungkus try/catch TERSENDIRI agar KEGAGALAN satu langkah TIDAK
		// membatalkan langkah lainnya. Sebelumnya semua langkah berada dalam satu blok try,
		// sehingga bila langkah awal (mis. reInitPertemuan) melempar exception, langkah
		// berikutnya (reInitUjian, pembobotan, write) IKUT dilewati dan hasil sinkron tidak
		// jadi masuk ke flag store. Dengan isolasi per-langkah, sebanyak mungkin hasil
		// sinkronisasi tetap tertulis ke "tabel flag" walau ada satu bagian yang bermasalah.

		// 1) MAHASISWA: bangun ulang flag store peserta + SIMPAN jumlah mahasiswa ke kolom
		//    (identik dengan tombol Syn.Mhs). singkronkanMhs -> ambilDetailperkuliahan(true, true):
		//    refresh=true memaksa reInitDetailperkuliahan, simpanJmlMhs=true menyimpan jumlah_mahasiswa.
		try {
			singkronkanMhs(session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:95");
		}

		// 2) PERTEMUAN: flag pertemuan + diskusi + DAFTAR ujian + tugas + tugas kelompok + izin
		try {
			reInitPertemuan(session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:102");
		}

		// 3) UJIAN: DATA PESERTA ujian + SOAL-soal ujian ke flag store (identik tombol Syn.Ujian).
		//    reInitPertemuan hanya membangun DAFTAR ujian; peserta & soal disinkron di sini.
		try {
			reInitUjian(session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:110");
		}

		// 4) Pendukung: dosen pengampu, pembobotan nilai, kelas paralel, mahasiswa jadi asisten
		try {
			reInitPerkuliahanDosen();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:117");
		}
		try {
			PembombotanNilai.setDefaultPembobotan(this, session, true);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:122");
		}
		try {
			reInitParalel(session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:127");
		}
		try {
			reInitMahasiswaJadiAsisten(session);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:132");
		}

		// 5) JAMINAN jumlah_mahasiswa tersimpan ke kolom untuk SEMUA kelas, termasuk kelas
		//    PARALEL yang di-short-circuit oleh ambilDetailperkuliahan (delegasi ke induk tanpa
		//    simpanJmlMhs). Flag store peserta sudah dibangun di langkah (1), jadi di sini cukup
		//    membaca ukurannya (tanpa refresh ulang karena flag "udah" sudah aktif) lalu menyimpan
		//    langsung ke kolom jumlah_mahasiswa. Idempoten terhadap simpan di langkah (1).
		try {
			Collection<Long> mahasiswaIds = ambilDetailperkuliahan();
			reInitJumlahMhs(mahasiswaIds == null ? 0 : mahasiswaIds.size());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:144");
		}

		// 6) Persist seluruh flag hasil sinkronisasi ke "tabel flag"
		try {
			write();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:151");
		}

		// 7) Buka kunci bila dosen pengunci sudah tidak lagi mengampu (perilaku lama dipertahankan)
		try {
			Perkuliahan perkuliahan = this;
			if (perkuliahan.getDikunci() != null && perkuliahan.getDikunci().getDosen() != null
					&& !perkuliahan.ada(perkuliahan.getDikunci().getDosen())) {
				perkuliahan.setDikunci(null);
				session.getTransaction().begin();
				Common.refreshUpdate(session, perkuliahan);
				session.getTransaction().commit();
				System.out.println("Buka kunci " + perkuliahan + " ");
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:166");
		}

	}

	/**
	 * Membangun ulang flag store peserta saja (aksi tombol "Syn.Mhs") tanpa menyentuh pertemuan,
	 * ujian, maupun pembobotan.
	 *
	 * <p>Setara dengan {@code ambilDetailperkuliahan(true, true)}: {@code refresh=true} memaksa
	 * {@link #reInitDetailperkuliahan(Session)} membaca ulang tabel {@code detailperkuliahan},
	 * dan {@code simpanJmlMhs=true} menyimpan hasil hitungannya ke kolom {@code jumlah_mahasiswa}.
	 * Kegagalan hanya dicatat, tidak dilempar.</p>
	 *
	 * @param session tidak dipakai langsung — jalur di dalamnya membuka session sendiri; parameter
	 *                dipertahankan demi keseragaman tanda tangan langkah sinkronisasi.
	 * @see #singkronkan(Session)
	 */
	public void singkronkanMhs(Session session) {
		try {
			ambilDetailperkuliahan(true, true);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:175");
		}

	}

	/**
	 * Menyatakan apakah flag "{@code pertemuan_data_baru}" untuk kelas ini sudah dibangun.
	 *
	 * <p>Menyempitkan {@code udah()} generik milik {@link GeneralValueObject} ke flag pertemuan,
	 * sehingga pemanggil yang tidak menyebut nama flag tetap menanyakan hal yang tepat untuk
	 * sebuah kelas kuliah.</p>
	 *
	 * @return {@code true} bila flag pertemuan sudah terisi.
	 * @see GeneralValueObject#udah(String)
	 */
	public boolean udah() {
		return super.udah("pertemuan_data_baru");
	}

	/**
	 * Menghapus flag "{@code pertemuan_data_baru}" sehingga data pertemuan akan dibangun ulang
	 * pada pembacaan berikutnya.
	 *
	 * @see GeneralValueObject#belum(String)
	 */
	public void belum() {
		belum("pertemuan_data_baru");
	}

	/** Kunci utama tabel {@code perkuliahan} (identity, dibangkitkan basis data). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit sederhana). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit sederhana). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> argumen {@code null} atau kosong DIABAIKAN diam-diam sehingga nilai
	 * lama tetap dipertahankan — ini disengaja agar jejak audit tidak terhapus oleh proses yang
	 * tidak mengetahui identitas pengguna.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila null/kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, argumen null/kosong DIABAIKAN diam-diam.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila null/kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui stempel waktu audit sesaat sebelum baris ini
	 * ditulis kembali ke basis data.
	 *
	 * <p>Dipanggil oleh provider persistence, bukan oleh kode aplikasi.</p>
	 *
	 * @see ais.database.hibernate.AuditTimestampInterceptor
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir (presisi timestamp).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas satu kelas kuliah untuk log, combobox, dan pesan kesalahan.
	 *
	 * <p>Bentuknya: {@code id-kode-nama-<n>sks-hari-jamMulai-jamSelesai-ruang-namaDosen1..10},
	 * dengan bagian yang kosong dilewati. Sebelum merangkai, semua relasi dibaca lewat getter
	 * (bukan field) sehingga lazy proxy Hibernate ikut ter-resolve; artinya method ini dapat
	 * memicu pembacaan basis data dan TIDAK bebas efek samping.</p>
	 *
	 * @return deskripsi satu baris kelas kuliah.
	 */
	public String toString() {
		matakuliah = getMatakuliah();
		dosen1 = getDosen1();
		dosen2 = getDosen2();
		dosen3 = getDosen3();
		dosen4 = getDosen4();
		dosen5 = getDosen5();
		dosen6 = getDosen6();
		dosen7 = getDosen7();
		dosen8 = getDosen8();
		dosen9 = getDosen9();
		dosen10 = getDosen10();
		ruang = getRuang();

		return id
				+ (matakuliah == null ? ""
						: "-" + matakuliah.getKode() + "-" + matakuliah.getNama() + "-" + matakuliah.getSks() + "sks")
				+ (hari == null ? "" : "-" + hari) + (waktuMulai == null ? "" : "-" + waktuMulai)
				+ (waktuSelesai == null ? "" : "-" + waktuSelesai) + (ruang == null ? "" : "-" + ruang.getNama())
				+ (dosen1 == null ? "" : "-" + dosen1.getNama()) + (dosen2 == null ? "" : "-" + dosen2.getNama())
				+ (dosen3 == null ? "" : "-" + dosen3.getNama()) + (dosen4 == null ? "" : "-" + dosen4.getNama())
				+ (dosen5 == null ? "" : "-" + dosen5.getNama()) + (dosen6 == null ? "" : "-" + dosen6.getNama())
				+ (dosen7 == null ? "" : "-" + dosen7.getNama()) + (dosen8 == null ? "" : "-" + dosen8.getNama())
				+ (dosen9 == null ? "" : "-" + dosen9.getNama()) + (dosen10 == null ? "" : "-" + dosen10.getNama());
	}

	/**
	 * Deskripsi lengkap kelas kuliah dalam kalimat berlabel, tanpa dosen tambahan.
	 *
	 * @return teks informasi kelas.
	 * @see #info(Dosen)
	 */
	public String info() {
		return info(null);
	}

	/**
	 * Menyusun deskripsi lengkap kelas kuliah untuk judul grup laporan, header layar, dan
	 * lampiran surat.
	 *
	 * <p>Hasilnya berbentuk: <i>"Matakuliah: KODE-Nama (n SKS), Semester: 3 (Semester Pendek) A,
	 * Dosen : ..., Ruang: ..., Hari: ..., ... s.d ..., Tahun Akademik: 2025/2026, Jurusan: ..."</i>.
	 * Bagian hari/jam DIHILANGKAN bila kelas ditandai
	 * {@link #getMerupakan_tanpa_jadwal_perkuliahan()}; label "Jurusan" dilewatkan
	 * {@code Common.getBahasaConfig} agar mengikuti istilah yang dipakai institusi (mis. "Program
	 * Studi").</p>
	 *
	 * <p>Daftar dosen diambil lewat {@code populateDosen()} milik {@link VOPembelajaran} sehingga
	 * seluruh slot {@code dosen1..dosen10} yang terisi ikut tercantum.</p>
	 *
	 * @param dosenTambahan dosen yang ingin disebut selain pengampu resmi (mis. dosen pengganti
	 *                      pada satu pertemuan); boleh {@code null}.
	 * @return teks informasi kelas; tidak pernah {@code null}.
	 */
	public String info(Dosen dosenTambahan) {
		jurusan = getJurusan();
		matakuliah = getMatakuliah();
		String matkul1 = matakuliah == null ? "" : matakuliah.getKode() + "-" + matakuliah.getNama();

		String semester1 = getSemester() == null ? "" : getSemester().toString();

		if (getStatusSemesterPendek() != null && getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)) {
			semester1 = semester1 + " (" + Common.getBahasaConfig(Perkuliahan.SP) + ")";
		}

		Integer sks = matakuliah == null ? 0 : matakuliah.getSks();

		String kelas1 = getKelas();

		String dosen1 = "";
		for (Dosen dosen : populateDosen().values()) {
			dosen1 += dosen1.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
		}

		if (dosenTambahan != null) {
			dosen1 += dosen1.isEmpty() ? dosenTambahan.getNama() : ", " + dosenTambahan.getNama();
		}

		String ruang = getRuang() == null ? "" : getRuang().getNama();

		String harijam = (getMerupakan_tanpa_jadwal_perkuliahan() == null ? false
				: getMerupakan_tanpa_jadwal_perkuliahan()) ? ""
						: (", Hari: " + getHari() + ", " + getWaktuMulai() + " s.d " + getWaktuSelesai());

		String groupTxt = "Matakuliah: " + matkul1 + " (" + sks + " SKS), Semester: " + semester1 + " " + kelas1
				+ (dosen1.equals("") ? "" : ", Dosen : " + dosen1) + (ruang.equals("") ? "" : ", Ruang: " + ruang)
				+ harijam + ", Tahun Akademik: " + getTahunAjaran() + ", " + Common.getBahasaConfig("Jurusan") + ": "
				+ (jurusan == null ? "" : jurusan.getNama());
		return groupTxt;
	}

	/**
	 * Validasi beban mengajar SELURUH slot dosen pada form penjadwalan, langsung dari komponen ZK.
	 *
	 * <p>Method ini membaca nilai terpilih dari combobox tahun akademik, semester, dan mata kuliah,
	 * lalu memanggil {@link #checkMaksSksDosen(Dosen, String, String, Integer, Integer, Long)}
	 * untuk sepuluh komponen dosen. Objek {@link Dosen} tiap slot diambil dari atribut komponen
	 * bernama {@code "myValue"} (konvensi widget pencari dosen di repo ini). Semester ganjil/genap
	 * disimpulkan dari paritas angka semester terpilih.</p>
	 *
	 * <p><b>Efek samping:</b> lewat method yang dipanggilnya, dapat memunculkan
	 * {@link MyMessageboxConfig} berisi peringatan bahwa seorang dosen melampaui batas SKS atau
	 * batas jumlah kelas.</p>
	 *
	 * <p>Dipakai oleh {@code ais.action.master.helper.util.PenjadwalanUtil} sebelum menyimpan
	 * jadwal.</p>
	 *
	 * @param tahunAjaran combobox tahun akademik; bila belum ada pilihan, validasi dilewati.
	 * @param semester    combobox semester (nilai {@link Integer}); bila belum dipilih, validasi
	 *                    dilewati.
	 * @param matakuliah  combobox mata kuliah (nilai {@link Matakuliah}); sumber bobot SKS yang
	 *                    akan ditambahkan.
	 * @param dosen1      komponen slot dosen ke-1.
	 * @param dosen2      komponen slot dosen ke-2.
	 * @param dosen3      komponen slot dosen ke-3.
	 * @param dosen4      komponen slot dosen ke-4.
	 * @param dosen5      komponen slot dosen ke-5.
	 * @param dosen6      komponen slot dosen ke-6.
	 * @param dosen7      komponen slot dosen ke-7.
	 * @param dosen8      komponen slot dosen ke-8.
	 * @param dosen9      komponen slot dosen ke-9.
	 * @param dosen10     komponen slot dosen ke-10.
	 * @param SP          {@link #SEMESTER_PENDEK} bila yang dijadwalkan kelas semester pendek,
	 *                    {@code null} untuk semester reguler.
	 * @param id          id kelas yang sedang diedit agar tidak menghitung dirinya sendiri;
	 *                    {@code null} untuk kelas baru.
	 * @return {@code true} bila seluruh slot masih dalam batas (jadwal boleh disimpan),
	 *         {@code false} begitu ada satu dosen yang melampaui batas. Juga {@code true} bila
	 *         data form belum lengkap sehingga validasi belum dapat dilakukan.
	 */
	public static boolean checkDosen(Combobox tahunAjaran, Combobox semester, Combobox matakuliah, Component dosen1,
			Component dosen2, Component dosen3, Component dosen4, Component dosen5, Component dosen6, Component dosen7,
			Component dosen8, Component dosen9, Component dosen10, Integer SP, Long id) {

		if (matakuliah.getSelectedItem() == null) {
			return true;
		}
		if (tahunAjaran.getSelectedItem() == null) {
			return true;
		}
		if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
			return true;
		}

		String ta = (String) tahunAjaran.getSelectedItem().getValue();
		String sem = ((Integer) semester.getSelectedItem().getValue()) % 2 == 0 ? Perkuliahan.GENAP
				: Perkuliahan.GANJIL;
		Matakuliah m = (Matakuliah) matakuliah.getSelectedItem().getValue();

		if (Perkuliahan.checkMaksSksDosen((Dosen) dosen1.getAttribute("myValue"), ta, sem, m.getSks(), SP, id)) {
			return false;
		}
		if (Perkuliahan.checkMaksSksDosen((Dosen) dosen2.getAttribute("myValue"), ta, sem, m.getSks(), SP, id)) {
			return false;
		}
		if (Perkuliahan.checkMaksSksDosen((Dosen) dosen3.getAttribute("myValue"), ta, sem, m.getSks(), SP, id)) {
			return false;
		}
		if (Perkuliahan.checkMaksSksDosen((Dosen) dosen4.getAttribute("myValue"), ta, sem, m.getSks(), SP, id)) {
			return false;
		}
		if (Perkuliahan.checkMaksSksDosen((Dosen) dosen5.getAttribute("myValue"), ta, sem, m.getSks(), SP, id)) {
			return false;
		}
		if (Perkuliahan.checkMaksSksDosen((Dosen) dosen6.getAttribute("myValue"), ta, sem, m.getSks(), SP, id)) {
			return false;
		}
		if (Perkuliahan.checkMaksSksDosen((Dosen) dosen7.getAttribute("myValue"), ta, sem, m.getSks(), SP, id)) {
			return false;
		}
		if (Perkuliahan.checkMaksSksDosen((Dosen) dosen8.getAttribute("myValue"), ta, sem, m.getSks(), SP, id)) {
			return false;
		}
		if (Perkuliahan.checkMaksSksDosen((Dosen) dosen9.getAttribute("myValue"), ta, sem, m.getSks(), SP, id)) {
			return false;
		}
		if (Perkuliahan.checkMaksSksDosen((Dosen) dosen10.getAttribute("myValue"), ta, sem, m.getSks(), SP, id)) {
			return false;
		}

		return true;
	}

	/**
	 * Merekap jumlah kemunculan tiap kode status kehadiran untuk satu acuan (mahasiswa atau dosen)
	 * dari rangkaian token status pertemuan.
	 *
	 * <p>Tiap elemen {@code statusPertemuan} adalah satu pertemuan yang berisi beberapa catatan
	 * dipisah titik koma, dan tiap catatan berisi bidang dipisah koma dengan bentuk
	 * <code>&lt;idAcuan&gt;,&lt;...&gt;,&lt;kodeStatus&gt;</code>. Hanya catatan yang bidang
	 * pertamanya sama dengan {@code ref} yang dihitung, dan kode {@code "-"} (belum diisi)
	 * dilewati.</p>
	 *
	 * <p>Selain hitungan per kode (mis. {@code H}, {@code S}, {@code I}, {@code A}), peta hasil
	 * SELALU memuat kunci khusus {@code "T"} berisi total seluruh catatan yang terhitung.</p>
	 *
	 * <p><b>Catatan robustness:</b> token bisa mengandung elemen kosong akibat pemisah ganda atau
	 * di ujung string pada data lama; kondisi itu divalidasi eksplisit sebelum
	 * {@code Long.parseLong} agar tidak bergantung pada {@code catch} semata.</p>
	 *
	 * <p>Dipakai luas oleh layar presensi dan dasbor, antara lain {@code AbsensiHelper},
	 * {@code DetailperkuliahanAction}, {@code PenjadwalanHelper}, serta beberapa
	 * {@code Dashboard*} kehadiran/nilai.</p>
	 *
	 * @param statusPertemuan daftar token status per pertemuan; bila {@code null}, hasilnya hanya
	 *                        berisi {@code "T" -> 0}.
	 * @param ref             id acuan yang dicari (id mahasiswa atau id dosen).
	 * @return peta kode status ke jumlah kemunculan, plus kunci {@code "T"} berisi total.
	 */
	public static Map<String, Integer> hitungStatus(List<String> statusPertemuan, Long ref) {

		Map<String, Integer> jumlah = new HashMap<String, Integer>();
		if (statusPertemuan == null) {
			jumlah.put("T", 0);
			return jumlah;
		}
		int total = 0;
		for (String status : statusPertemuan) {
			String[] nilais = status.split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					// FIX NumberFormatException: token status pertemuan bisa
					// berisi elemen kosong (mis. "" hasil split trailing/ganda
					// pemisah ";"/",") -- validasi dulu sebelum parseLong,
					// jangan andalkan catch semata (menghindari exception utk
					// kasus data yg wajar/legacy, bukan hanya menutup error).
					if (s.length < 3 || s[0] == null || s[0].trim().isEmpty()) {
						continue;
					}
					Long formatId = Long.parseLong(s[0].trim());
					if (ref.equals(formatId)) {
						String kode = s[2];
						if (!kode.equals("-")) {
							if (jumlah.containsKey(kode)) {
								jumlah.put(kode, jumlah.get(kode) + 1);
								total++;
							} else {
								jumlah.put(kode, 1);
								total++;
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:376");

				}
			}
		}
		jumlah.put("T", total);
		return jumlah;
	}

	private Integer jumlahDosen = 1;
	private Integer jumlahMahasiswa;
	private String idSmt;

	private Matakuliah matakuliah;
	private Dosen dosen1;
	private Dosen dosen2;
	private Dosen dosen3;
	private Dosen dosen4;
	private Dosen dosen5;
	private Dosen dosen6;
	private Dosen dosen7;
	private Dosen dosen8;
	private Dosen dosen9;
	private Dosen dosen10;
	private Boolean aktif;
	private Jurusan jurusan;
	private Ruang ruang;
	private Integer semester;
	private Integer status_penilaian = BELUM_ADA_MAHASISWA;
	/**
	 * Field ini tidak ada gunanya
	 */
	private Integer semesterPerkuliahan;
	private Integer kapasitasKelas;

	private String program;
	private String jenis;
	private String waktuMulai = "00.00";
	private String waktuSelesai = "00.00";
	private String hari = "";
	private String tahunAjaran = "";
	private String kelas = "A";
	private Kelas kelasref;
	private String asrama;
	private String mode;
	private String lingkup;

	private String waktu = "PAGI";

	private Double waktuMulaiD = 0.0;
	private Double waktuSelesaiD = 0.0;

	private Kurikulum kurikulum;
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;

	private PembombotanNilai pembombotanNilai;
	private PembombotanNilai pembombotanNilaiBackup;

	private Integer statusSemesterPendek;
	private String ganjilGenap;
	// private String warna = "#88880E,#BFBF4D";

	// private Integer status_penilaian = BELUM_ADA_MAHASISWA;
	// private String deskripsi_status_penilaian;

	private Boolean merupakan_paralel;
	private Perkuliahan perkuliahan_paralel;
	private Boolean merupakan_tanpa_jadwal_perkuliahan = false;
	private Boolean merupakan_tanpa_dosen = false;
	private Boolean merupakan_tanpa_ruangan = false;

	private Date awalPerkuliahan;

	private JamPerkuliahan jamPerkuliahan;
	private MasaPerkuliahan masaPerkuliahan;
	private Integer planning_jumlah_tatap_muka;
	private Tbmuser dikunci;

	private Boolean minggu1 = true;
	private Boolean minggu2 = true;
	private Boolean minggu3 = true;
	private Boolean minggu4 = true;
	private Boolean minggu5 = true;

	private Boolean sembunyikanFormatPenilaian;

	private Boolean ambilMkDiluarSemesterKurikulum;

	private Date perkuliahanDimulai;
	private Date perkuliahanSampai;

	private Date tanggalMulaiPerkuliahan;
	private Boolean bolehMenentukanTanggalMulaiPerkuliahan;
	private Boolean lewatiTanggalMerahNasional;

	private String feeder;
	private String feeders;

	private String feeder1;
	private String feeder2;
	private String feeder3;
	private String feeder4;
	private String feeder5;
	private String feeder6;
	private String feeder7;
	private String feeder8;
	private String feeder9;
	private String feeder10;

	private JenisEvaluasi jenisEvaluasi;
	private Boolean abaikanWaktuBentrokDenganJadwalLain;
	private Boolean tampilkanSaatPengambilanKrs;
	// private Boolean janganAmbilSilabusDariKurikulum;
	private Boolean dosenBisaMerubahTanggalPerkuliahan;

	private String keterangan;
	private String keteranganJadwal;

	private String deskripsiPembelajaran;
	private String capaianPembelajaranProdi;

	private Boolean kehadiranDosenHarusDiinputSesuaiJadwal;
	private Boolean kehadiranDosenHarusDiinputDiIpYangDitentukan;

	private Boolean kehadiranMahasiswaHarusDiinputSesuaiJadwal;
	private Boolean kehadiranMahasiswaHarusDiinputDiIpYangDitentukan;

	private Boolean adminBolehMenginputKehadiranDiluarJadwalDanIp;

	private Boolean jumlahRencanaPertemuanMengikutiKurikulum = true;
	private Integer jumlahMaksimalPertemuan;

	private Boolean merupakanRemedial;
	private Boolean merupakanPraPerkuliahan;
	private Boolean merupakanPerkuliahanUmum;
	private Boolean merupakanTeamTeaching;
	private Boolean terdapatKegiatanPraktek;
	private Boolean dosenBolehVerifikasiNilaiSendiri;
	private Boolean waktuPerkuliahanOnlineBebas;
	private Boolean sembunyikanNilaiJikaBelumDiverifikasi;

	private Boolean mahasiswaBolehAbsenMenggunakanFoto;
	private Boolean dosenBolehAbsenMenggunakanFoto;

	private Boolean mahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen;

	private String pendahuluan;

	private Boolean nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir;
	private Boolean jikaAdaNilai0TidakMenghitungNilaiAkhir;
	private Boolean hanyaInputNilaiHuruf;

	private Boolean bolehAbsenWaktuIkutiPerkuliahan;
	private Integer bolehAbsenSebelumWaktuMulaiDalamMenit;
	private Integer bolehAbsenSetelahWaktuMulaiDalamMenit;
	private Boolean semuaPertemuanSesuaiRps;
	private Long semuaNilaiSesuaiRps;
	private String catatanSesuaiRps;
	private Double persenKehadiranDinilai0;

	private Integer batasWaktuBolehAbsenKehadiran;

	/**
	 * Teks pendahuluan/pengantar kelas yang ditampilkan pada halaman e-learning dan cetakan RPS.
	 *
	 * <p>Isi dilewatkan {@code filterTidakBoleh(...)} milik {@link GeneralValueObject} untuk
	 * membuang markup berbahaya sebelum ditampilkan; hasil bersihnya DITULIS BALIK ke field,
	 * jadi getter ini mengubah state objek.</p>
	 *
	 * @return teks pendahuluan yang sudah disaring dan di-trim; string kosong bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getPendahuluan() {

		pendahuluan = filterTidakBoleh(pendahuluan);

		return pendahuluan == null ? "" : pendahuluan.trim();
	}

	/**
	 * Mengisi teks pendahuluan kelas (disimpan apa adanya; penyaringan terjadi saat dibaca).
	 *
	 * @param pendahuluan teks pendahuluan, boleh {@code null}.
	 */
	public void setPendahuluan(String pendahuluan) {
		this.pendahuluan = pendahuluan;
	}

	/**
	 * Menyatakan apakah kelas ini diampu secara <i>team teaching</i>.
	 *
	 * <p>Bila belum pernah ditentukan secara eksplisit, nilainya DISIMPULKAN dari jumlah slot
	 * dosen yang terisi: lebih dari satu pengampu berarti team teaching.</p>
	 *
	 * @return {@code true} bila kelas diampu lebih dari satu dosen atau ditandai demikian.
	 * @see #getJumlahDosen()
	 */
	public Boolean getMerupakanTeamTeaching() {
		return merupakanTeamTeaching == null ? getJumlahDosen() > 1 : merupakanTeamTeaching;
	}

	/**
	 * Menetapkan penanda team teaching secara eksplisit, menimpa kesimpulan otomatis.
	 *
	 * @param merupakanTeamTeaching {@code null} untuk kembali mengikuti jumlah dosen.
	 */
	public void setMerupakanTeamTeaching(Boolean merupakanTeamTeaching) {
		this.merupakanTeamTeaching = merupakanTeamTeaching;
	}

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate/JPA untuk instansiasi entity.
	 */
	public Perkuliahan() {

	}

	/**
	 * Membuat objek rujukan ringan yang hanya membawa kunci utama.
	 *
	 * <p>Berguna untuk kriteria query dan perbandingan berbasis id tanpa memuat seluruh baris;
	 * kesetaraan objek mengikuti kontrak {@code equals}/{@code compareTo} di
	 * {@link GeneralValueObject}.</p>
	 *
	 * @param id kunci utama kelas kuliah.
	 */
	public Perkuliahan(Long id) {
		this.id = id;
	}

	/**
	 * Memeriksa apakah menambahkan satu kelas lagi akan membuat seorang dosen melampaui batas
	 * beban mengajar pada satu tahun akademik dan semester.
	 *
	 * <p>Dua batas diperiksa berurutan, keduanya dibaca dari tabel konfigurasi (default 50 bila
	 * konfigurasi tidak ada atau tidak berupa angka):</p>
	 * <ol>
	 * <li>{@code maksimal_dosen_mengajar_dalam_satu_semester} — batas total <b>SKS</b>; beban
	 * berjalan dihitung sebagai jumlah {@code matakuliah.sks} seluruh kelas aktif yang diampu.</li>
	 * <li>{@code maksimal_perkuliahan_dosen_mengajar_dalam_satu_semester} — batas <b>jumlah
	 * kelas</b>; penambahan selalu dihitung satu kelas.</li>
	 * </ol>
	 *
	 * <p>Pencarian kelas yang sudah diampu menyusun {@code OR} atas KESEPULUH kolom
	 * {@code dosen1..dosen10} (konsekuensi pola sepuluh slot: tidak ada tabel penghubung yang bisa
	 * di-join), dibatasi pada kelas aktif, tahun akademik dan semester yang sama, status semester
	 * pendek yang sama, dan mengecualikan kelas {@code id} bila sedang mengedit.</p>
	 *
	 * <p><b>Efek samping:</b> membuka dan menutup {@link Session} Hibernate sendiri, mencetak
	 * ringkasan ke {@code System.out}, dan — bila batas terlampaui — menampilkan
	 * {@link MyMessageboxConfig} kepada pengguna. Karena itu method ini hanya cocok dipanggil dari
	 * jalur UI, bukan dari batch tanpa desktop ZK.</p>
	 *
	 * @param dosen             dosen yang diperiksa; {@code null} berarti slot kosong dan langsung
	 *                          lolos.
	 * @param tahunAkademik     tahun akademik yang diperiksa, mis. {@code "2025/2026"}.
	 * @param semester          {@link #GANJIL} atau {@link #GENAP} (isi kolom {@code ganjil_genap}).
	 * @param tambahanMengajar  bobot SKS kelas yang hendak ditambahkan.
	 * @param SP                {@link #SEMESTER_PENDEK} untuk kelas semester pendek, {@code null}
	 *                          untuk semester reguler.
	 * @param id                id kelas yang sedang diedit agar tidak ikut dihitung; {@code null}
	 *                          untuk kelas baru.
	 * @return {@code true} bila batas TERLAMPAUI (penambahan harus ditolak), {@code false} bila
	 *         masih aman.
	 * @see #checkDosen(Combobox, Combobox, Combobox, Component, Component, Component, Component,
	 *      Component, Component, Component, Component, Component, Component, Integer, Long)
	 */
	public static boolean checkMaksSksDosen(Dosen dosen, String tahunAkademik, String semester,
			Integer tambahanMengajar, Integer SP, Long id) {
		if (dosen == null) {
			return false;
		}
		int maksimal_dosen_mengajar_dalam_satu_semester = 50;
		try {
			maksimal_dosen_mengajar_dalam_satu_semester = Integer.parseInt(
					Common.getKonfigurasi("maksimal_dosen_mengajar_dalam_satu_semester", "50").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:575");

		}

		int maksimal_perkuliahan_dosen_mengajar_dalam_satu_semester = 50;
		try {
			maksimal_perkuliahan_dosen_mengajar_dalam_satu_semester = Integer.parseInt(Common
					.getKonfigurasi("maksimal_perkuliahan_dosen_mengajar_dalam_satu_semester", "50").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:583");

		}

		Criterion criterion = dosen == null ? Restrictions.sqlRestriction("false")
				: Restrictions.or(Restrictions.eq("dosen1", dosen), Restrictions.eq("dosen2", dosen));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

		Object[] qq = null;
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			qq = ((Object[]) session.createCriteria(Perkuliahan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(SP == null ? Restrictions.isNull("statusSemesterPendek")
							: Restrictions.eq("statusSemesterPendek", SP))
					.add(id == null ? Restrictions.sqlRestriction("true") : Restrictions.ne("id", id)).add(criterion)
					.add(tahunAkademik == null ? Restrictions.sqlRestriction("false")
							: Restrictions.eq("tahunAjaran", tahunAkademik))

					.add(semester == null ? Restrictions.sqlRestriction("false")
							: Restrictions.eq("ganjilGenap", semester))

					.createAlias("matakuliah", "matakuliah").setProjection(Projections.projectionList()
							.add(Projections.sum("matakuliah.sks")).add(Projections.rowCount()))
					.uniqueResult());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:619");
		} finally {
			if (session != null && session.isOpen()) {
				session.clear();
				session.disconnect();
				session.close();
			}
		}

		Number q = (Number) (qq == null ? 0 : qq[0]);

		int jumlahMengajar = q == null ? 0 : q.intValue();

		System.out.println("dosen => " + dosen + ", tahunAkademik => " + tahunAkademik + ", semester => " + semester
				+ ", maksimal_dosen_mengajar_dalam_satu_semester => " + maksimal_dosen_mengajar_dalam_satu_semester
				+ ", jumlahMengajar => " + jumlahMengajar);

		boolean hasil = maksimal_dosen_mengajar_dalam_satu_semester < (tambahanMengajar + jumlahMengajar);

		if (hasil) {
			try {
				MyMessageboxConfig.show(
						"Dosen dengan nama " + dosen.getNama() + " telah mengajar di tahun akademik " + tahunAkademik
								+ " semester " + semester + (SP == null ? " semester pendek " : "") + " sebanyak "
								+ jumlahMengajar + " sks. Anda tidak bisa menambah " + tambahanMengajar
								+ " sks lagi, karena maksimal jumlah SKS yang diajar oleh dosen adalah "
								+ maksimal_dosen_mengajar_dalam_satu_semester,
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
			return hasil;
		}

		q = (Number) (qq == null ? 0 : qq[1]);

		jumlahMengajar = q == null ? 0 : q.intValue();
		tambahanMengajar = 1;

		System.out.println("dosen => " + dosen + ", tahunAkademik => " + tahunAkademik + ", semester => " + semester
				+ ", maksimal_perkuliahan_dosen_mengajar_dalam_satu_semester => "
				+ maksimal_perkuliahan_dosen_mengajar_dalam_satu_semester + ", jumlahMengajar => " + jumlahMengajar);

		hasil = maksimal_perkuliahan_dosen_mengajar_dalam_satu_semester < (tambahanMengajar + jumlahMengajar);

		if (hasil) {
			try {
				MyMessageboxConfig.show(
						"Dosen dengan nama " + dosen.getNama() + " telah mengajar di tahun akademik " + tahunAkademik
								+ " semester " + semester + (SP == null ? " semester pendek " : "") + " sebanyak "
								+ jumlahMengajar + " perkuliahan. Anda tidak bisa menambah " + tambahanMengajar
								+ " perkuliahan lagi, karena maksimal jumlah perkuliahan yang diajar oleh dosen adalah "
								+ maksimal_perkuliahan_dosen_mengajar_dalam_satu_semester,
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
			return hasil;
		}

		return false;
	}

	/**
	 * Kunci utama kelas kuliah (kolom {@code id}, identity basis data).
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan.
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Normalnya diisi Hibernate saat {@code insert}.
	 *
	 * @param id kunci utama.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mata kuliah yang ditawarkan oleh kelas ini — sumber kode, nama, dan bobot SKS.
	 *
	 * <p>Relasi ini WAJIB ({@code nullable = false}) karena kelas kuliah tidak berarti tanpa mata
	 * kuliahnya. Nilainya dilewatkan {@code check(...)} agar proxy lazy yang sudah tidak sah
	 * (mis. baris terhapus) tidak ikut dikembalikan.</p>
	 *
	 * @return mata kuliah yang ditawarkan.
	 * @see Matakuliah
	 * @see GeneralValueObject
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matakuliah", nullable = false)
	public Matakuliah getMatakuliah() {
		matakuliah = check(matakuliah);
		return this.matakuliah;
	}

	/**
	 * Menetapkan mata kuliah yang ditawarkan kelas ini.
	 *
	 * @param matakuliah mata kuliah dari kurikulum.
	 */
	public void setMatakuliah(Matakuliah matakuliah) {
		this.matakuliah = matakuliah;
	}

	/**
	 * Dosen pengampu slot ke-1 — slot utama yang selalu tersedia.
	 *
	 * <p>Berbeda dengan slot 2..10, slot pertama tidak dinolkan oleh {@link #getJumlahDosen()}.</p>
	 *
	 * @return dosen pengampu utama, atau {@code null} bila kelas belum berdosen.
	 * @see Dosen
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen1", nullable = true)
	public Dosen getDosen1() {
		dosen1 = check(dosen1);
		return this.dosen1;
	}

	/**
	 * Menetapkan dosen pengampu slot ke-1.
	 *
	 * @param dosen1 dosen pengampu utama, boleh {@code null}.
	 */
	public void setDosen1(Dosen dosen1) {
		this.dosen1 = dosen1;
	}

	/**
	 * Dosen pengampu slot ke-2.
	 *
	 * <p><b>Efek samping:</b> bila {@link #getJumlahDosen()} kurang dari 2 — artinya rantai slot
	 * terputus sebelum slot ini — field DIKOSONGKAN lebih dulu. Ini menjaga agar slot tidak
	 * "berlubang" (mis. dosen2 terisi sementara dosen1 kosong).</p>
	 *
	 * @return dosen pengampu ke-2, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen2", nullable = true)
	public Dosen getDosen2() {
		if (getJumlahDosen() < 2) {
			dosen2 = null;
		}
		dosen2 = check(dosen2);
		return this.dosen2;
	}

	/**
	 * Menetapkan dosen pengampu slot ke-2.
	 *
	 * @param dosen2 dosen pengampu ke-2, boleh {@code null}.
	 */
	public void setDosen2(Dosen dosen2) {
		this.dosen2 = dosen2;
	}

	/**
	 * Ruang tempat kelas ini dijadwalkan.
	 *
	 * @return ruang perkuliahan, atau {@code null} untuk kelas tanpa ruang (mis. daring).
	 * @see Ruang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		return this.ruang;
	}

	/**
	 * Menetapkan ruang perkuliahan.
	 *
	 * @param ruang ruang, boleh {@code null}.
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Semester kurikulum tempat mata kuliah ini seharusnya diambil (1, 2, 3, ...).
	 *
	 * <p><b>Efek samping:</b> untuk kelas pra-perkuliahan ({@link #getMerupakanPraPerkuliahan()})
	 * nilainya DIPAKSA menjadi {@code -1} — penanda bahwa kelas berada di luar struktur semester
	 * kurikulum. Nilai {@code null} dinormalkan menjadi {@code 0}.</p>
	 *
	 * @return angka semester kurikulum; {@code -1} untuk pra-perkuliahan, {@code 0} bila kosong.
	 */
	@Column(name = "semester")
	public Integer getSemester() {
		if (getMerupakanPraPerkuliahan()) {
			semester = -1;
		}
		return this.semester == null ? 0 : this.semester;
	}

	/**
	 * Menetapkan semester kurikulum kelas ini.
	 *
	 * @param semester angka semester kurikulum.
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Menetapkan jurusan/program studi penyelenggara kelas.
	 *
	 * @param jurusan jurusan penyelenggara, boleh {@code null} agar disimpulkan dari mata kuliah.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Jurusan/program studi penyelenggara kelas.
	 *
	 * <p><b>Efek samping:</b> bila kolom belum terisi, nilainya DISIMPULKAN dari jurusan pemilik
	 * mata kuliah dan ditulis balik ke field. Karena label "Jurusan" dapat diganti per institusi,
	 * teks tampilannya dilewatkan {@code Common.getBahasaConfig(...)} di
	 * {@link #info(Dosen)}.</p>
	 *
	 * @return jurusan penyelenggara, atau {@code null} bila mata kuliah pun belum berjurusan.
	 * @see Jurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		matakuliah = getMatakuliah();
		if (jurusan == null && matakuliah != null) {
			jurusan = matakuliah.getJurusan();
		}
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menetapkan jam mulai perkuliahan dalam bentuk teks ("HH.mm"); string kosong dinormalkan
	 * menjadi {@code null}.
	 *
	 * @param waktuMulai jam mulai, boleh {@code null}/kosong.
	 */
	public void setWaktuMulai(String waktuMulai) {
		this.waktuMulai = waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/**
	 * Jam mulai perkuliahan sebagai teks "HH.mm".
	 *
	 * <p><b>Efek samping / prioritas nilai:</b></p>
	 * <ol>
	 * <li>Bila kelas terikat master {@link JamPerkuliahan}, jam master MENIMPA teks bebas dan
	 * ditulis balik ke field — jadi mengubah master jam otomatis mengubah jadwal semua kelas yang
	 * memakainya.</li>
	 * <li>Bila kelas ditandai {@link #getMerupakan_tanpa_jadwal_perkuliahan()}, nilainya
	 * dikosongkan.</li>
	 * </ol>
	 *
	 * @return jam mulai "HH.mm", atau {@code null} bila kosong/tanpa jadwal.
	 * @see #getWaktuMulaiD()
	 */
	@Column(name = "waktu_mulai", length = 20)
	public String getWaktuMulai() {

		try {
			jamPerkuliahan = getJamPerkuliahan();
			if (jamPerkuliahan != null) {
				waktuMulai = jamPerkuliahan.getWaktuMulai();
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:782");
			// TODO: handle exception
		}

		if (getMerupakan_tanpa_jadwal_perkuliahan()) {
			waktuMulai = "";
		}
		return waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/**
	 * Menetapkan jam selesai perkuliahan dalam bentuk teks ("HH.mm"); string kosong dinormalkan
	 * menjadi {@code null}.
	 *
	 * @param waktuSelesai jam selesai, boleh {@code null}/kosong.
	 */
	public void setWaktuSelesai(String waktuSelesai) {
		this.waktuSelesai = waktuSelesai == null || waktuSelesai.trim().equals("") ? null : waktuSelesai.trim();
	}

	/**
	 * Jam selesai perkuliahan sebagai teks "HH.mm".
	 *
	 * <p>Aturan prioritasnya sama persis dengan {@link #getWaktuMulai()}: master
	 * {@link JamPerkuliahan} menimpa teks bebas, dan kelas tanpa jadwal dikosongkan. Getter ini
	 * juga menulis balik ke field.</p>
	 *
	 * @return jam selesai "HH.mm", atau {@code null} bila kosong/tanpa jadwal.
	 * @see #getWaktuSelesaiD()
	 */
	@Column(name = "waktu_selesai", length = 20)
	public String getWaktuSelesai() {

		try {
			jamPerkuliahan = getJamPerkuliahan();
			if (jamPerkuliahan != null) {
				waktuSelesai = jamPerkuliahan.getWaktuSelesai();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:804");
			// TODO: handle exception
		}

		if (getMerupakan_tanpa_jadwal_perkuliahan()) {
			waktuSelesai = "";
		}
		return waktuSelesai == null || waktuSelesai.trim().equals("") ? null : waktuSelesai.trim();
	}

	/**
	 * Menetapkan nama hari perkuliahan ("Senin" .. "Minggu").
	 *
	 * @param hari nama hari, boleh {@code null}/kosong untuk kelas tanpa jadwal.
	 */
	public void setHari(String hari) {
		this.hari = hari;
	}

	/**
	 * Nama hari perkuliahan.
	 *
	 * <p><b>Efek samping / normalisasi:</b> kelas tanpa jadwal dikosongkan, {@code null} menjadi
	 * string kosong, dan ejaan {@code "Jumat"} SELALU dinormalkan menjadi {@code "Jum'at"}.
	 * Normalisasi terakhir itu penting karena pembanding hari di seluruh sistem — termasuk
	 * penghitungan {@link #getTanggalMulaiPerkuliahan()} — memakai keluaran
	 * {@code Common.dateFormat4Week} yang menghasilkan {@code "Jumat"}, sehingga tanpa penyeragaman
	 * ini jadwal hari Jumat tidak pernah cocok.</p>
	 *
	 * @return nama hari yang sudah dinormalkan; string kosong bila tanpa jadwal.
	 */
	@Column(name = "hari", length = 20)
	public String getHari() {
		if (getMerupakan_tanpa_jadwal_perkuliahan()) {
			hari = "";
		}

		if (hari == null) {
			hari = "";
		}

		if (hari.trim().equalsIgnoreCase("Jumat")) {
			hari = "Jum'at";
		}

		return hari;
	}

	/**
	 * Menetapkan tahun akademik kelas ini, mis. {@code "2025/2026"}.
	 *
	 * @param tahunAjaran tahun akademik.
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * Tahun akademik penyelenggaraan kelas, mis. {@code "2025/2026"}.
	 *
	 * <p>Bersama {@link #getGanjilGenap()} dan {@link #getStatusSemesterPendek()}, nilai ini
	 * membentuk periode kelas dan menjadi bahan {@link #getIdSmt()} untuk Feeder PDDikti.</p>
	 *
	 * @return tahun akademik.
	 */
	@Column(name = "tahun_ajaran", length = 20)
	public String getTahunAjaran() {
		return tahunAjaran;
	}

	/**
	 * Menetapkan label kelas paralel dalam bentuk teks bebas ("A", "B", "Pagi", ...).
	 *
	 * @param kelas label kelas.
	 */
	public void setKelas(String kelas) {
		this.kelas = kelas;
	}

	/**
	 * Label kelas paralel ("A", "B", ...).
	 *
	 * <p><b>Efek samping:</b> bila kelas terikat master {@link Kelas} lewat {@code kelasref}, nama
	 * master MENIMPA teks bebas dan ditulis balik ke field — pola yang sama dengan
	 * {@link JamPerkuliahan} pada jam kuliah. {@code null} dinormalkan menjadi string kosong.</p>
	 *
	 * @return label kelas yang sudah di-trim; tidak pernah {@code null}.
	 * @see #getKelasref()
	 */
	@Column(name = "kelas", length = 255)
	public String getKelas() {
		try {
			kelasref = getKelasref();
			if (kelasref != null && kelasref.getNama() != null) {
				kelas = kelasref.getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:855");
			// TODO: handle exception
		}

		if (kelas == null) {
			kelas = "";
		}
		return kelas.trim();
	}

	/**
	 * Menetapkan sesi waktu perkuliahan ("PAGI", "SIANG", "MALAM").
	 *
	 * @param waktu label sesi waktu.
	 */
	public void setWaktu(String waktu) {
		this.waktu = waktu;
	}

	/**
	 * Sesi waktu perkuliahan ("PAGI", "SIANG", "MALAM") — dipakai untuk pengelompokan jadwal dan
	 * penyaringan laporan.
	 *
	 * <p><b>Efek samping:</b> nilai kosong dinormalkan menjadi {@code "PAGI"}, dan kelas tanpa
	 * jadwal dikosongkan; keduanya ditulis balik ke field.</p>
	 *
	 * @return label sesi waktu; string kosong bila kelas tanpa jadwal.
	 */
	@Column(name = "waktu", length = 20)
	public String getWaktu() {
		if (waktu == null || waktu.trim().isEmpty()) {
			waktu = "PAGI";
		}
		if (getMerupakan_tanpa_jadwal_perkuliahan()) {
			waktu = "";
		}
		return waktu;
	}

	/**
	 * Menetapkan program penyelenggaraan ("Reguler", "Karyawan", ...).
	 *
	 * @param program nama program.
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Program penyelenggaraan kelas ("Reguler", "Karyawan", ...).
	 *
	 * <p><b>Efek samping:</b> bila kosong, diisi otomatis dengan nama program default
	 * {@code ConstantValues.REGULER} dan ditulis balik ke field. Nilai ini ikut menentukan
	 * kurikulum mana yang dipakai pada {@link #ambilKurikulumPunyaMatakuliah()}.</p>
	 *
	 * @return nama program penyelenggaraan.
	 */
	@Column(name = "program", length = 20)
	public String getProgram() {
		if (ConstantValues.REGULER != null && program == null || program.trim().isEmpty()) {
			program = ConstantValues.REGULER.getNama();
		}
		return program;
	}

	/**
	 * Menetapkan skema pembobotan nilai kelas ini.
	 *
	 * <p><b>Perhatian:</b> argumen {@code null} DIABAIKAN diam-diam — skema lama dipertahankan.
	 * Ini disengaja agar proses yang gagal memuat pembobotan tidak menghapus skema yang sudah
	 * dipakai untuk menilai.</p>
	 *
	 * @param pembombotanNilai skema pembobotan; diabaikan bila {@code null}.
	 */
	public void setPembombotanNilai(PembombotanNilai pembombotanNilai) {
		if (pembombotanNilai == null) {
			return;
		}
		this.pembombotanNilai = pembombotanNilai;
	}

	/**
	 * Skema pembobotan nilai yang berlaku untuk kelas ini.
	 *
	 * <p>Urutan penentuannya berlapis:</p>
	 * <ol>
	 * <li>Bila kelas sedang <b>dikunci</b> ({@link #getDikunci()}) dan punya cadangan
	 * ({@link #getPembombotanNilaiBackup()}), skema cadangan yang dipakai — nilai yang sudah
	 * diproses tidak boleh berubah komposisinya di tengah jalan.</li>
	 * <li>Selain itu, dicari skema aktif yang ditandai wajib untuk tahun akademik + semester ini;
	 * skema semacam itu MENIMPA pilihan per kelas.</li>
	 * <li>Bila tidak ada, dipakai skema yang tersimpan pada kolom; bila itu pun kosong, dipakai
	 * {@code ConstantValues.DEFAULT_PEMBOBOTAN_NILAI}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> membaca cache konstanta global dan menulis balik hasilnya ke field.</p>
	 *
	 * @return skema pembobotan yang berlaku; praktis tidak pernah {@code null}.
	 * @see PembombotanNilai
	 * @see #ambilFormatNilai(Session, boolean, boolean)
	 */
	@SuppressWarnings("unchecked")
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembombotan_nilai", nullable = true)
	public PembombotanNilai getPembombotanNilai() {
		if (getDikunci() != null && getPembombotanNilaiBackup() != null) {
			pembombotanNilai = getPembombotanNilaiBackup();
		} else {

			Map<Long, PembombotanNilai> maps = ConstantValues.ambilBerdasarClass(PembombotanNilai.class);
			for (PembombotanNilai pembombotanNilai : maps.values()) {

				if (pembombotanNilai != null && pembombotanNilai.getAktif()
						&& pembombotanNilai.getWajibDitahunAkademikDanSemesterTertentu()) {

					if (pembombotanNilai.getTahunAkadmeik() != null && pembombotanNilai.getSemester() != null
							&& getTahunAjaran() != null && getGanjilGenap() != null
							&& pembombotanNilai.getTahunAkadmeik().equals(getTahunAjaran())
							&& pembombotanNilai.getSemester().equalsIgnoreCase(getGanjilGenap())) {
						this.pembombotanNilai = pembombotanNilai;
						return this.pembombotanNilai;
					}
				}
			}

			pembombotanNilai = check(pembombotanNilai);
			if (pembombotanNilai == null) {
				pembombotanNilai = ConstantValues.DEFAULT_PEMBOBOTAN_NILAI;
			}
		}
		return pembombotanNilai;

	}

	/**
	 * Menetapkan jam selesai dalam bentuk numerik.
	 *
	 * @param waktuSelesaiD jam selesai sebagai desimal, mis. {@code 10.30}.
	 */
	public void setWaktuSelesaiD(Double waktuSelesaiD) {
		this.waktuSelesaiD = waktuSelesaiD;
	}

	/**
	 * Jam selesai dalam bentuk numerik ({@code 10.30} untuk pukul 10.30) — dipakai untuk
	 * pembandingan rentang waktu, terutama deteksi bentrok jadwal, yang sulit dilakukan pada teks.
	 *
	 * <p><b>Efek samping:</b> nilai SELALU dihitung ulang dari {@link #getWaktuSelesai()} (dengan
	 * pemisah {@code :} atau {@code ,} disamakan menjadi titik) dan ditulis balik ke field, jadi
	 * kolom numerik ini adalah turunan, bukan sumber kebenaran. {@code null} dinormalkan menjadi
	 * {@code 0.0}.</p>
	 *
	 * @return jam selesai sebagai desimal; {@code 0.0} bila jam tidak terbaca.
	 */
	@Column(name = "waktu_selesai_d", length = 15, precision = 15, scale = 2)
	public Double getWaktuSelesaiD() {
		waktuSelesai = getWaktuSelesai();
		if (waktuSelesai != null && !waktuSelesai.trim().equals("")) {
			try {
				waktuSelesaiD = Double.parseDouble(waktuSelesai.trim().replaceAll(":", ".").replaceAll(",", "."));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:942");
				// TODO Auto-generated catch block
				// Common.tampilErrorJikaAdmin(e);
			}
		}
		if (waktuSelesaiD == null) {
			waktuSelesaiD = 0.0;
		}
		return waktuSelesaiD;
	}

	/**
	 * Menetapkan jam mulai dalam bentuk numerik.
	 *
	 * @param waktuMulaiD jam mulai sebagai desimal, mis. {@code 8.00}.
	 */
	public void setWaktuMulaiD(Double waktuMulaiD) {
		this.waktuMulaiD = waktuMulaiD;
	}

	/**
	 * Jam mulai dalam bentuk numerik; kembaran {@link #getWaktuSelesaiD()} dengan aturan
	 * penghitungan ulang dan normalisasi yang sama.
	 *
	 * @return jam mulai sebagai desimal; {@code 0.0} bila jam tidak terbaca.
	 */
	@Column(name = "waktu_mulai_d", length = 15, precision = 15, scale = 2)
	public Double getWaktuMulaiD() {
		waktuMulai = getWaktuMulai();
		if (waktuMulai != null && !waktuMulai.trim().equals("")) {
			try {
				waktuMulaiD = Double.parseDouble(waktuMulai.trim().replaceAll(":", ".").replaceAll(",", "."));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:963");
				// TODO Auto-generated catch block
//				Common.tampilErrorJikaAdmin(e);
			}
		}
		if (waktuMulaiD == null) {
			waktuMulaiD = 0.0;
		}
		return waktuMulaiD;
	}

	/**
	 * Menetapkan kurikulum acuan kelas ini.
	 *
	 * @param kurikulum kurikulum acuan, boleh {@code null}.
	 */
	public void setKurikulum(Kurikulum kurikulum) {
		this.kurikulum = kurikulum;
	}

	/**
	 * Kurikulum acuan kelas ini — menentukan baris {@link KurikulumPunyaMatakuliah} mana yang
	 * memasok RPS, capaian pembelajaran, dan jumlah rencana pertemuan.
	 *
	 * @return kurikulum acuan, atau {@code null}.
	 * @see Kurikulum
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kurikulum", nullable = true)
	public Kurikulum getKurikulum() {
		kurikulum = check(kurikulum);
		return kurikulum;
	}

	/**
	 * Menandai kelas sebagai kelas semester pendek.
	 *
	 * @param statusSemesterPendek {@link #SEMESTER_PENDEK} untuk kelas semester pendek,
	 *                             {@code null} untuk kelas reguler.
	 */
	public void setStatusSemesterPendek(Integer statusSemesterPendek) {
		this.statusSemesterPendek = statusSemesterPendek;
	}

	/**
	 * Penanda kelas semester pendek.
	 *
	 * <p>Perhatikan bahwa jalur-jalur query di file ini membedakan {@code null} (reguler) dari
	 * {@link #SEMESTER_PENDEK} dengan {@code isNull}/{@code eq}, bukan dengan angka {@code 0},
	 * sehingga nilai selain kedua itu tidak akan terjaring.</p>
	 *
	 * @return {@link #SEMESTER_PENDEK} bila kelas semester pendek, {@code null} bila reguler.
	 */
	@Column(name = "status_semesterpendek", nullable = true)
	public Integer getStatusSemesterPendek() {
		return statusSemesterPendek;
	}

	/**
	 * Menetapkan label semester secara eksplisit.
	 *
	 * @param ganjilGenap {@link #GANJIL}, {@link #GENAP}, atau {@link #SP}.
	 */
	public void setGanjilGenap(String ganjilGenap) {
		this.ganjilGenap = ganjilGenap;
	}

	/**
	 * Label semester penyelenggaraan: {@link #GANJIL}, {@link #GENAP}, atau {@link #SP}.
	 *
	 * <p><b>Efek samping:</b> bila kolom masih kosong, nilainya DISIMPULKAN lalu ditulis balik —
	 * {@link #SP} bila kelas semester pendek, selain itu dari paritas {@link #getSemester()}
	 * (genap untuk semester bernomor genap). Nilai inilah yang dipakai sebagai kunci pencocokan
	 * pada query beban dosen dan pencarian {@link PembombotanNilai} yang wajib per periode.</p>
	 *
	 * @return label semester; tidak kosong selama semester terisi.
	 */
	@Column(name = "ganjil_genap", nullable = true, length = 20)
	public String getGanjilGenap() {
		if (ganjilGenap == null || ganjilGenap.isEmpty()) {
			if (statusSemesterPendek != null && statusSemesterPendek.equals(SEMESTER_PENDEK)) {
				ganjilGenap = Perkuliahan.SP;
			} else if (getSemester() != null && (ganjilGenap == null || ganjilGenap.trim().isEmpty())) {
				ganjilGenap = getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
			}
		}
		return ganjilGenap;
	}

	/**
	 * Menetapkan penanda kelas paralel.
	 *
	 * <p>Praktis tidak berpengaruh: {@link #getMerupakan_paralel()} selalu menghitung ulang
	 * nilainya dari {@code perkuliahan_paralel}. Setter ini ada agar Hibernate dapat memetakan
	 * property.</p>
	 *
	 * @param merupakan_paralel penanda kelas paralel.
	 */
	public void setMerupakan_paralel(Boolean merupakan_paralel) {
		this.merupakan_paralel = merupakan_paralel;
	}

	/**
	 * Menyatakan apakah kelas ini merupakan kelas paralel yang menempel pada kelas induk.
	 *
	 * <p>Nilainya SELALU diturunkan dari ada/tidaknya {@link #getPerkuliahan_paralel()} dan
	 * ditulis balik ke field, sehingga nilai kolom di basis data tidak pernah menjadi sumber
	 * kebenaran.</p>
	 *
	 * @return {@code true} bila kelas ini menunjuk kelas induk.
	 */
	public Boolean getMerupakan_paralel() {
		merupakan_paralel = getPerkuliahan_paralel() != null;
		return merupakan_paralel;
	}

	/**
	 * Menjadikan kelas ini paralel dari kelas lain.
	 *
	 * @param perkuliahan_paralel kelas induk; {@code null} untuk kelas mandiri.
	 */
	public void setPerkuliahan_paralel(Perkuliahan perkuliahan_paralel) {
		this.perkuliahan_paralel = perkuliahan_paralel;
	}

	/**
	 * Kelas induk tempat kelas ini menempel (relasi rujuk-diri).
	 *
	 * <p>Ini adalah salah satu field paling berpengaruh di kelas ini: begitu terisi, hampir semua
	 * pembacaan peserta, format nilai, dan lokasi berkas flag store DIALIHKAN ke induk — lihat
	 * {@link #ambilMahasiswa()}, {@link #ambilMahasiswaId(boolean)},
	 * {@link #ambilDetailperkuliahan(String, String, String, boolean, boolean, boolean)},
	 * {@link #ambilFormatNilai(Session, boolean, boolean)}, {@link #ambilLokasiDetailPerkuliahan()},
	 * dan {@link #getFeeder()}. Konsekuensinya, kelas paralel tidak pernah menyimpan
	 * {@code jumlah_mahasiswa} lewat jalur biasa; {@link #singkronkan(Session)} menambalnya
	 * secara khusus.</p>
	 *
	 * @return kelas induk, atau {@code null} bila kelas ini berdiri sendiri.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perkuliahan_paralel", nullable = true)
	public Perkuliahan getPerkuliahan_paralel() {
		perkuliahan_paralel = check(perkuliahan_paralel);
		return perkuliahan_paralel;
	}

	/**
	 * Menandai kelas sebagai kelas tanpa jadwal tetap.
	 *
	 * @param merupakan_tanpa_jadwal_perkuliahan {@code null} agar disimpulkan otomatis dari
	 *                                           kosongnya hari dan jam.
	 */
	public void setMerupakan_tanpa_jadwal_perkuliahan(Boolean merupakan_tanpa_jadwal_perkuliahan) {
		this.merupakan_tanpa_jadwal_perkuliahan = merupakan_tanpa_jadwal_perkuliahan;
	}

	/**
	 * Menyatakan apakah kelas ini diselenggarakan tanpa jadwal hari/jam tetap (mis. kuliah daring
	 * mandiri, kelas blok, atau bimbingan).
	 *
	 * <p><b>Efek samping:</b> bila belum pernah ditentukan, nilainya DISIMPULKAN dan ditulis balik
	 * ke field: kelas dianggap tanpa jadwal bila hari kosong DAN jam mulai/selesai kosong.
	 * Penanda ini kemudian membuat {@link #getHari()}, {@link #getWaktuMulai()},
	 * {@link #getWaktuSelesai()}, dan {@link #getWaktu()} mengembalikan nilai kosong.</p>
	 *
	 * @return {@code true} bila kelas tidak memiliki jadwal tetap.
	 */
	public Boolean getMerupakan_tanpa_jadwal_perkuliahan() {
		if (merupakan_tanpa_jadwal_perkuliahan == null) {
			if ((hari == null || hari.trim().isEmpty()) && (waktuMulai == null || waktuSelesai == null
					|| waktuMulai.trim().isEmpty() || waktuSelesai.trim().isEmpty())) {
				merupakan_tanpa_jadwal_perkuliahan = true;
			} else {
				merupakan_tanpa_jadwal_perkuliahan = false;
			}
		}

		return merupakan_tanpa_jadwal_perkuliahan;
	}

	/**
	 * Menandai kelas yang sengaja dijadwalkan tanpa dosen pengampu.
	 *
	 * @param merupakan_tanpa_dosen penanda tanpa dosen.
	 */
	public void setMerupakan_tanpa_dosen(Boolean merupakan_tanpa_dosen) {
		this.merupakan_tanpa_dosen = merupakan_tanpa_dosen;
	}

	/**
	 * Menyatakan apakah kelas ini sengaja dijadwalkan tanpa dosen pengampu, sehingga validasi
	 * penjadwalan tidak menuntut slot dosen terisi.
	 *
	 * <p>Nilai {@code null} dinormalkan menjadi {@code false} dan ditulis balik ke field.</p>
	 *
	 * @return {@code true} bila kelas boleh tanpa dosen.
	 */
	public Boolean getMerupakan_tanpa_dosen() {
		if (merupakan_tanpa_dosen == null) {
			merupakan_tanpa_dosen = false;
		}
		return merupakan_tanpa_dosen;
	}

	/**
	 * Menandai kelas yang sengaja dijadwalkan tanpa ruang.
	 *
	 * @param merupakan_tanpa_ruangan penanda tanpa ruang.
	 */
	public void setMerupakan_tanpa_ruangan(Boolean merupakan_tanpa_ruangan) {
		this.merupakan_tanpa_ruangan = merupakan_tanpa_ruangan;
	}

	/**
	 * Menyatakan apakah kelas ini sengaja dijadwalkan tanpa ruang (mis. kuliah daring), sehingga
	 * pemeriksaan bentrok ruang dilewati.
	 *
	 * <p>Berbeda dengan {@link #getMerupakan_tanpa_dosen()}, getter ini TIDAK menormalkan
	 * {@code null}, jadi pemanggil harus siap menerima {@code null}.</p>
	 *
	 * @return {@code true}/{@code false}/{@code null} sesuai isi kolom.
	 */
	public Boolean getMerupakan_tanpa_ruangan() {
		return merupakan_tanpa_ruangan;
	}

	/**
	 * Kuota peserta kelas — batas jumlah mahasiswa yang boleh mengambil kelas ini lewat KRS.
	 *
	 * <p><b>Efek samping:</b> bila kolom kosong, diisi dengan kapasitas default global
	 * {@code Ruang.getDefaultKapasitas()} dan ditulis balik ke field.</p>
	 *
	 * <p>Penegakan kuota TIDAK dilakukan di entity ini melainkan pada jalur pengambilan KRS —
	 * lihat {@code AmbilDataIkutPerkuliahanHelper} dan
	 * {@code AmbilDataMahasiswaForPaketPerkuliahanHelper}, yang membandingkan jumlah peserta yang
	 * sudah masuk dengan nilai ini. Karena itu, sisa kursi dihitung sebagai
	 * {@code getKapasitasKelas() - getJumlahMahasiswa()} oleh pemanggil, bukan oleh method di
	 * sini.</p>
	 *
	 * @return kuota peserta kelas; tidak pernah {@code null}.
	 * @see #getJumlahMahasiswa()
	 */
	@Column(name = "kapasitas_kelas", nullable = true)
	public Integer getKapasitasKelas() {
		if (kapasitasKelas == null) {
			kapasitasKelas = Ruang.getDefaultKapasitas();
		}
		return kapasitasKelas;
	}

	/**
	 * Menetapkan kuota peserta kelas.
	 *
	 * @param kapasitasKelas kuota; {@code null} agar mengikuti kapasitas default global.
	 */
	public void setKapasitasKelas(Integer kapasitasKelas) {
		this.kapasitasKelas = kapasitasKelas;
	}

	/**
	 * Master jam perkuliahan (slot jam baku) yang dipakai kelas ini.
	 *
	 * <p>Bila terisi, jam master MENIMPA teks {@code waktuMulai}/{@code waktuSelesai} saat dibaca
	 * — lihat {@link #getWaktuMulai()}.</p>
	 *
	 * @return master jam perkuliahan, atau {@code null} bila jam diisi bebas.
	 * @see JamPerkuliahan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_perkuliahan", nullable = true)
	public JamPerkuliahan getJamPerkuliahan() {
		jamPerkuliahan = check(jamPerkuliahan);
		return jamPerkuliahan;
	}

	/**
	 * Menetapkan master jam perkuliahan.
	 *
	 * @param jamPerkuliahan slot jam baku, boleh {@code null}.
	 */
	public void setJamPerkuliahan(JamPerkuliahan jamPerkuliahan) {
		this.jamPerkuliahan = jamPerkuliahan;
	}

	/**
	 * Menetapkan rencana jumlah tatap muka kelas ini.
	 *
	 * @param planning_jumlah_tatap_muka jumlah tatap muka yang direncanakan.
	 */
	public void setPlanning_jumlah_tatap_muka(Integer planning_jumlah_tatap_muka) {
		this.planning_jumlah_tatap_muka = planning_jumlah_tatap_muka;
	}

	/**
	 * Rencana jumlah tatap muka kelas ini (kolom lama; batas pertemuan yang benar-benar dipakai
	 * pembangkit pertemuan adalah {@link #getJumlahMaksimalPertemuan()}).
	 *
	 * @return jumlah tatap muka yang direncanakan, atau {@code null}.
	 */
	public Integer getPlanning_jumlah_tatap_muka() {
		return planning_jumlah_tatap_muka;
	}

	/**
	 * Pengguna yang sedang <b>mengunci</b> kelas ini.
	 *
	 * <p>Kelas terkunci berarti nilai dan pembobotannya dibekukan: {@link #getPembombotanNilai()}
	 * beralih ke cadangan {@link #getPembombotanNilaiBackup()} sehingga perubahan konfigurasi
	 * global tidak lagi menggeser komposisi nilai yang sudah diproses.</p>
	 *
	 * <p><b>Efek samping:</b> getter ini MEMBATALKAN kunci (mengembalikan {@code null}) bila dosen
	 * milik pengguna pengunci ternyata sudah tidak berada di antara pengampu kelas — pemeriksaan
	 * dilakukan terhadap {@code populateDosenBuId()}. Pembatalan di sini hanya di memori;
	 * penulisan ke basis data terjadi pada langkah terakhir {@link #singkronkan(Session)}.</p>
	 *
	 * @return pengguna pengunci, atau {@code null} bila kelas tidak (lagi) terkunci.
	 * @see Tbmuser
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci", nullable = true)
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);

		try {
			if (dikunci != null && dikunci.getDosen() != null && dikunci.getDosen().getId() != null) {
				List<Long> dosenIds = populateDosenBuId();
				if (dosenIds == null || !dosenIds.contains(dikunci.getDosen().getId())) {
					dikunci = null;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:1108");
			// TODO: handle exception
		}
		return dikunci;
	}

	/**
	 * Menetapkan (atau membuka) kunci kelas.
	 *
	 * @param dikunci pengguna pengunci; {@code null} untuk membuka kunci.
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Kolom lama {@code semester_perkuliahan}.
	 *
	 * <p><b>Sudah tidak dipakai</b> — lihat catatan pada deklarasi fieldnya. Semester kelas yang
	 * berlaku dibaca dari {@link #getSemester()}. Dipertahankan hanya agar pemetaan Hibernate dan
	 * kolom basis data lama tetap konsisten.</p>
	 *
	 * @return isi kolom apa adanya.
	 */
	@Column(name = "semester_perkuliahan")
	public Integer getSemesterPerkuliahan() {
		return semesterPerkuliahan;
	}

	/**
	 * Mengisi kolom lama {@code semester_perkuliahan} yang sudah tidak dipakai.
	 *
	 * @param semesterPerkuliahan nilai kolom.
	 */
	public void setSemesterPerkuliahan(Integer semesterPerkuliahan) {
		this.semesterPerkuliahan = semesterPerkuliahan;
	}

	/**
	 * Menyatakan apakah kelas ini berlangsung pada pekan ke-1 dalam pola bulanan.
	 *
	 * <p>Lima penanda {@code minggu1}..{@code minggu5} dipakai untuk jadwal yang tidak berulang
	 * setiap pekan (mis. kelas yang hanya berjalan pada pekan ganjil). Semuanya default
	 * {@code true} — artinya kelas berjalan pada semua pekan — dan nilai {@code null} ditulis
	 * balik menjadi {@code true}.</p>
	 *
	 * @return {@code true} bila kelas berjalan pada pekan ke-1.
	 */
	public Boolean getMinggu1() {
		if (minggu1 == null) {
			minggu1 = true;
		}
		return minggu1;
	}

	/**
	 * Menetapkan apakah kelas berjalan pada pekan ke-1.
	 *
	 * @param minggu1 penanda pekan ke-1.
	 */
	public void setMinggu1(Boolean minggu1) {
		this.minggu1 = minggu1;
	}

	/**
	 * Penanda pekan ke-2; aturannya sama dengan {@link #getMinggu1()}.
	 *
	 * @return {@code true} bila kelas berjalan pada pekan ke-2.
	 */
	public Boolean getMinggu2() {
		if (minggu2 == null) {
			minggu2 = true;
		}
		return minggu2;
	}

	/**
	 * Menetapkan apakah kelas berjalan pada pekan ke-2.
	 *
	 * @param minggu2 penanda pekan ke-2.
	 */
	public void setMinggu2(Boolean minggu2) {
		this.minggu2 = minggu2;
	}

	/**
	 * Penanda pekan ke-3; aturannya sama dengan {@link #getMinggu1()}.
	 *
	 * @return {@code true} bila kelas berjalan pada pekan ke-3.
	 */
	public Boolean getMinggu3() {
		if (minggu3 == null) {
			minggu3 = true;
		}
		return minggu3;
	}

	/**
	 * Menetapkan apakah kelas berjalan pada pekan ke-3.
	 *
	 * @param minggu3 penanda pekan ke-3.
	 */
	public void setMinggu3(Boolean minggu3) {
		this.minggu3 = minggu3;
	}

	/**
	 * Penanda pekan ke-4; aturannya sama dengan {@link #getMinggu1()}.
	 *
	 * @return {@code true} bila kelas berjalan pada pekan ke-4.
	 */
	public Boolean getMinggu4() {
		if (minggu4 == null) {
			minggu4 = true;
		}
		return minggu4;
	}

	/**
	 * Menetapkan apakah kelas berjalan pada pekan ke-4.
	 *
	 * @param minggu4 penanda pekan ke-4.
	 */
	public void setMinggu4(Boolean minggu4) {
		this.minggu4 = minggu4;
	}

	/**
	 * Penanda pekan ke-5; aturannya sama dengan {@link #getMinggu1()}.
	 *
	 * @return {@code true} bila kelas berjalan pada pekan ke-5.
	 */
	public Boolean getMinggu5() {
		if (minggu5 == null) {
			minggu5 = true;
		}
		return minggu5;
	}

	/**
	 * Menetapkan apakah kelas berjalan pada pekan ke-5.
	 *
	 * @param minggu5 penanda pekan ke-5.
	 */
	public void setMinggu5(Boolean minggu5) {
		this.minggu5 = minggu5;
	}

	/**
	 * Dosen pengampu slot ke-3.
	 *
	 * <p>Seperti slot 2 dan seterusnya, field DIKOSONGKAN lebih dulu bila
	 * {@link #getJumlahDosen()} kurang dari 3, sehingga rantai sepuluh slot tidak pernah
	 * berlubang.</p>
	 *
	 * @return dosen pengampu ke-3, atau {@code null}.
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen3", nullable = true)
	public Dosen getDosen3() {
		if (getJumlahDosen() < 3) {
			dosen3 = null;
		}
		dosen3 = check(dosen3);
		return dosen3;
	}

	/**
	 * Menetapkan dosen pengampu slot ke-3.
	 *
	 * @param dosen3 dosen pengampu ke-3, boleh {@code null}.
	 */
	public void setDosen3(Dosen dosen3) {
		this.dosen3 = dosen3;
	}

	/**
	 * Dosen pengampu slot ke-4.
	 *
	 * <p>Seperti slot 2 dan seterusnya, field DIKOSONGKAN lebih dulu bila
	 * {@link #getJumlahDosen()} kurang dari 4, sehingga rantai sepuluh slot tidak pernah
	 * berlubang.</p>
	 *
	 * @return dosen pengampu ke-4, atau {@code null}.
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen4", nullable = true)
	public Dosen getDosen4() {
		if (getJumlahDosen() < 4) {
			dosen4 = null;
		}
		dosen4 = check(dosen4);
		return dosen4;
	}

	/**
	 * Menetapkan dosen pengampu slot ke-4.
	 *
	 * @param dosen4 dosen pengampu ke-4, boleh {@code null}.
	 */
	public void setDosen4(Dosen dosen4) {
		this.dosen4 = dosen4;
	}

	/**
	 * Dosen pengampu slot ke-5.
	 *
	 * <p>Seperti slot 2 dan seterusnya, field DIKOSONGKAN lebih dulu bila
	 * {@link #getJumlahDosen()} kurang dari 5, sehingga rantai sepuluh slot tidak pernah
	 * berlubang.</p>
	 *
	 * @return dosen pengampu ke-5, atau {@code null}.
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen5", nullable = true)
	public Dosen getDosen5() {
		if (getJumlahDosen() < 5) {
			dosen5 = null;
		}
		dosen5 = check(dosen5);
		return dosen5;
	}

	/**
	 * Menetapkan dosen pengampu slot ke-5.
	 *
	 * @param dosen5 dosen pengampu ke-5, boleh {@code null}.
	 */
	public void setDosen5(Dosen dosen5) {
		this.dosen5 = dosen5;
	}

	/**
	 * Dosen pengampu slot ke-6.
	 *
	 * <p>Seperti slot 2 dan seterusnya, field DIKOSONGKAN lebih dulu bila
	 * {@link #getJumlahDosen()} kurang dari 6, sehingga rantai sepuluh slot tidak pernah
	 * berlubang.</p>
	 *
	 * @return dosen pengampu ke-6, atau {@code null}.
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen6", nullable = true)
	public Dosen getDosen6() {
		if (getJumlahDosen() < 6) {
			dosen6 = null;
		}
		dosen6 = check(dosen6);
		return dosen6;
	}

	/**
	 * Menetapkan dosen pengampu slot ke-6.
	 *
	 * @param dosen6 dosen pengampu ke-6, boleh {@code null}.
	 */
	public void setDosen6(Dosen dosen6) {
		this.dosen6 = dosen6;
	}

	/**
	 * Dosen pengampu slot ke-7.
	 *
	 * <p>Seperti slot 2 dan seterusnya, field DIKOSONGKAN lebih dulu bila
	 * {@link #getJumlahDosen()} kurang dari 7, sehingga rantai sepuluh slot tidak pernah
	 * berlubang.</p>
	 *
	 * @return dosen pengampu ke-7, atau {@code null}.
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen7", nullable = true)
	public Dosen getDosen7() {
		if (getJumlahDosen() < 7) {
			dosen7 = null;
		}
		dosen7 = check(dosen7);
		return dosen7;
	}

	/**
	 * Menetapkan dosen pengampu slot ke-7.
	 *
	 * @param dosen7 dosen pengampu ke-7, boleh {@code null}.
	 */
	public void setDosen7(Dosen dosen7) {
		this.dosen7 = dosen7;
	}

	/**
	 * Dosen pengampu slot ke-8.
	 *
	 * <p>Seperti slot 2 dan seterusnya, field DIKOSONGKAN lebih dulu bila
	 * {@link #getJumlahDosen()} kurang dari 8, sehingga rantai sepuluh slot tidak pernah
	 * berlubang.</p>
	 *
	 * @return dosen pengampu ke-8, atau {@code null}.
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen8", nullable = true)
	public Dosen getDosen8() {
		if (getJumlahDosen() < 8) {
			dosen8 = null;
		}
		dosen8 = check(dosen8);
		return dosen8;
	}

	/**
	 * Menetapkan dosen pengampu slot ke-8.
	 *
	 * @param dosen8 dosen pengampu ke-8, boleh {@code null}.
	 */
	public void setDosen8(Dosen dosen8) {
		this.dosen8 = dosen8;
	}

	/**
	 * Dosen pengampu slot ke-9.
	 *
	 * <p>Seperti slot 2 dan seterusnya, field DIKOSONGKAN lebih dulu bila
	 * {@link #getJumlahDosen()} kurang dari 9, sehingga rantai sepuluh slot tidak pernah
	 * berlubang.</p>
	 *
	 * @return dosen pengampu ke-9, atau {@code null}.
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen9", nullable = true)
	public Dosen getDosen9() {
		if (getJumlahDosen() < 9) {
			dosen9 = null;
		}
		dosen9 = check(dosen9);
		return dosen9;
	}

	/**
	 * Menetapkan dosen pengampu slot ke-9.
	 *
	 * @param dosen9 dosen pengampu ke-9, boleh {@code null}.
	 */
	public void setDosen9(Dosen dosen9) {
		this.dosen9 = dosen9;
	}

	/**
	 * Dosen pengampu slot ke-10.
	 *
	 * <p>Seperti slot 2 dan seterusnya, field DIKOSONGKAN lebih dulu bila
	 * {@link #getJumlahDosen()} kurang dari 10, sehingga rantai sepuluh slot tidak pernah
	 * berlubang.</p>
	 *
	 * @return dosen pengampu ke-10, atau {@code null}.
	 * @see #getDosen1()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen10", nullable = true)
	public Dosen getDosen10() {
		if (getJumlahDosen() < 10) {
			dosen10 = null;
		}
		dosen10 = check(dosen10);
		return dosen10;
	}

	/**
	 * Menetapkan dosen pengampu slot ke-10.
	 *
	 * @param dosen10 dosen pengampu ke-10, boleh {@code null}.
	 */
	public void setDosen10(Dosen dosen10) {
		this.dosen10 = dosen10;
	}

	/**
	 * Menghitung berapa slot dosen yang terisi secara BERURUTAN dari slot ke-1.
	 *
	 * <p>Implementasinya berupa rantai {@code if/else if} menurun dari 10 ke 1 yang menuntut
	 * SELURUH slot sebelumnya juga terisi. Akibatnya nilai ini bukan sekadar "berapa dosen ada",
	 * melainkan panjang rantai slot yang tidak berlubang: bila {@code dosen1} kosong sementara
	 * {@code dosen2} terisi, hasilnya tetap {@code 0}.</p>
	 *
	 * <p>Nilai inilah yang dipakai getter {@code getDosen2()}..{@code getDosen10()} untuk
	 * mengosongkan slot di luar rantai, dipakai {@link #getMerupakanTeamTeaching()} untuk
	 * menyimpulkan team teaching, dan dipakai {@link #populateInfoPersetujuan()} sebagai penyebut
	 * persentase kehadiran dosen.</p>
	 *
	 * <p><b>Catatan:</b> pemeriksaan dilakukan langsung atas FIELD, bukan lewat getter, justru
	 * untuk menghindari rekursi tak berujung dengan getter slot dosen yang memanggil method ini.
	 * Hasilnya ditulis balik ke field {@code jumlahDosen}.</p>
	 *
	 * @return jumlah slot dosen terisi berurutan, {@code 0}..{@code 10}.
	 */
	public Integer getJumlahDosen() {

		if (dosen1 != null && dosen2 != null && dosen3 != null && dosen4 != null && dosen5 != null && dosen6 != null
				&& dosen7 != null && dosen8 != null && dosen9 != null && dosen10 != null) {
			jumlahDosen = 10;
		} else if (dosen1 != null && dosen2 != null && dosen3 != null && dosen4 != null && dosen5 != null
				&& dosen6 != null && dosen7 != null && dosen8 != null && dosen9 != null) {
			jumlahDosen = 9;
		} else if (dosen1 != null && dosen2 != null && dosen3 != null && dosen4 != null && dosen5 != null
				&& dosen6 != null && dosen7 != null && dosen8 != null) {
			jumlahDosen = 8;
		} else if (dosen1 != null && dosen2 != null && dosen3 != null && dosen4 != null && dosen5 != null
				&& dosen6 != null && dosen7 != null) {
			jumlahDosen = 7;
		} else if (dosen1 != null && dosen2 != null && dosen3 != null && dosen4 != null && dosen5 != null
				&& dosen6 != null) {
			jumlahDosen = 6;
		} else if (dosen1 != null && dosen2 != null && dosen3 != null && dosen4 != null && dosen5 != null) {
			jumlahDosen = 5;
		} else if (dosen1 != null && dosen2 != null && dosen3 != null && dosen4 != null) {
			jumlahDosen = 4;
		} else if (dosen1 != null && dosen2 != null && dosen3 != null) {
			jumlahDosen = 3;
		} else if (dosen1 != null && dosen2 != null) {
			jumlahDosen = 2;
		} else if (dosen1 != null) {
			jumlahDosen = 1;
		} else {
			jumlahDosen = 0;
		}

		return jumlahDosen;
	}

	/**
	 * Mengisi cache jumlah dosen.
	 *
	 * <p>Praktis tidak berpengaruh karena {@link #getJumlahDosen()} selalu menghitung ulang; ada
	 * agar Hibernate dan form ZK dapat memetakan property.</p>
	 *
	 * @param jumlahDosen jumlah slot dosen.
	 */
	public void setJumlahDosen(Integer jumlahDosen) {
		this.jumlahDosen = jumlahDosen;
	}

	/**
	 * Tanggal awal rentang penyelenggaraan kelas (batas bawah pembangkitan pertemuan).
	 *
	 * @return tanggal mulai, atau {@code null} bila mengikuti {@link MasaPerkuliahan}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getPerkuliahanDimulai() {
		return perkuliahanDimulai;
	}

	/**
	 * Menetapkan tanggal awal rentang penyelenggaraan kelas.
	 *
	 * @param perkuliahanDimulai tanggal mulai, boleh {@code null}.
	 */
	public void setPerkuliahanDimulai(Date perkuliahanDimulai) {
		this.perkuliahanDimulai = perkuliahanDimulai;
	}

	/**
	 * Tanggal akhir rentang penyelenggaraan kelas (batas atas pembangkitan pertemuan).
	 *
	 * @return tanggal selesai, atau {@code null} bila mengikuti {@link MasaPerkuliahan}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getPerkuliahanSampai() {
		return perkuliahanSampai;
	}

	/**
	 * Menetapkan tanggal akhir rentang penyelenggaraan kelas.
	 *
	 * @param perkuliahanSampai tanggal selesai, boleh {@code null}.
	 */
	public void setPerkuliahanSampai(Date perkuliahanSampai) {
		this.perkuliahanSampai = perkuliahanSampai;
	}

	/**
	 * Kode kelas pada Feeder PDDikti (kolom {@code feeder_kode}).
	 *
	 * <p><b>Efek samping:</b> untuk kelas paralel, kode DIAMBIL ALIH dari kelas induk dan ditulis
	 * balik ke field — konsisten dengan kenyataan bahwa kelas paralel bukan kelas terpisah di mata
	 * Feeder. String kosong dinormalkan menjadi {@code null}.</p>
	 *
	 * @return kode kelas di Feeder, atau {@code null} bila belum pernah disinkronkan.
	 * @see #getFeeders()
	 * @see #getIdSmt()
	 */
	@Column(name = "feeder_kode", unique = false)
	public String getFeeder() {
		if (getPerkuliahan_paralel() != null && getPerkuliahan_paralel().getFeeder() != null) {
			feeder = getPerkuliahan_paralel().getFeeder();
		}
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Menetapkan kode kelas pada Feeder PDDikti.
	 *
	 * @param feeder kode kelas dari Feeder.
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Id dosen pengampu slot ke-1 pada Feeder PDDikti.
	 *
	 * <p>Sepuluh kolom {@code feeder1}..{@code feeder10} adalah pendamping pola sepuluh slot
	 * {@code dosen1}..{@code dosen10}: satu kolom per slot, dipakai saat mengirim penugasan dosen
	 * ke Feeder.</p>
	 *
	 * @return id Feeder dosen ke-1, atau {@code null} bila belum disinkronkan.
	 * @see #getDosen1()
	 */
	public String getFeeder1() {
		return feeder1;
	}

	/**
	 * Menetapkan id Feeder untuk dosen pengampu slot ke-1.
	 *
	 * @param feeder1 id Feeder dosen ke-1.
	 */
	public void setFeeder1(String feeder1) {
		this.feeder1 = feeder1;
	}

	/**
	 * Id dosen pengampu slot ke-2 pada Feeder PDDikti.
	 *
	 * <p>Sepuluh kolom {@code feeder1}..{@code feeder10} adalah pendamping pola sepuluh slot
	 * {@code dosen1}..{@code dosen10}: satu kolom per slot, dipakai saat mengirim penugasan dosen
	 * ke Feeder.</p>
	 *
	 * @return id Feeder dosen ke-2, atau {@code null} bila belum disinkronkan.
	 * @see #getDosen2()
	 */
	public String getFeeder2() {
		return feeder2;
	}

	/**
	 * Menetapkan id Feeder untuk dosen pengampu slot ke-2.
	 *
	 * @param feeder2 id Feeder dosen ke-2.
	 */
	public void setFeeder2(String feeder2) {
		this.feeder2 = feeder2;
	}

	/**
	 * Id dosen pengampu slot ke-3 pada Feeder PDDikti.
	 *
	 * <p>Sepuluh kolom {@code feeder1}..{@code feeder10} adalah pendamping pola sepuluh slot
	 * {@code dosen1}..{@code dosen10}: satu kolom per slot, dipakai saat mengirim penugasan dosen
	 * ke Feeder.</p>
	 *
	 * @return id Feeder dosen ke-3, atau {@code null} bila belum disinkronkan.
	 * @see #getDosen3()
	 */
	public String getFeeder3() {
		return feeder3;
	}

	/**
	 * Menetapkan id Feeder untuk dosen pengampu slot ke-3.
	 *
	 * @param feeder3 id Feeder dosen ke-3.
	 */
	public void setFeeder3(String feeder3) {
		this.feeder3 = feeder3;
	}

	/**
	 * Id dosen pengampu slot ke-4 pada Feeder PDDikti.
	 *
	 * <p>Sepuluh kolom {@code feeder1}..{@code feeder10} adalah pendamping pola sepuluh slot
	 * {@code dosen1}..{@code dosen10}: satu kolom per slot, dipakai saat mengirim penugasan dosen
	 * ke Feeder.</p>
	 *
	 * @return id Feeder dosen ke-4, atau {@code null} bila belum disinkronkan.
	 * @see #getDosen4()
	 */
	public String getFeeder4() {
		return feeder4;
	}

	/**
	 * Menetapkan id Feeder untuk dosen pengampu slot ke-4.
	 *
	 * @param feeder4 id Feeder dosen ke-4.
	 */
	public void setFeeder4(String feeder4) {
		this.feeder4 = feeder4;
	}

	/**
	 * Id dosen pengampu slot ke-5 pada Feeder PDDikti.
	 *
	 * <p>Sepuluh kolom {@code feeder1}..{@code feeder10} adalah pendamping pola sepuluh slot
	 * {@code dosen1}..{@code dosen10}: satu kolom per slot, dipakai saat mengirim penugasan dosen
	 * ke Feeder.</p>
	 *
	 * @return id Feeder dosen ke-5, atau {@code null} bila belum disinkronkan.
	 * @see #getDosen5()
	 */
	public String getFeeder5() {
		return feeder5;
	}

	/**
	 * Menetapkan id Feeder untuk dosen pengampu slot ke-5.
	 *
	 * @param feeder5 id Feeder dosen ke-5.
	 */
	public void setFeeder5(String feeder5) {
		this.feeder5 = feeder5;
	}

	/**
	 * Id dosen pengampu slot ke-6 pada Feeder PDDikti.
	 *
	 * <p>Sepuluh kolom {@code feeder1}..{@code feeder10} adalah pendamping pola sepuluh slot
	 * {@code dosen1}..{@code dosen10}: satu kolom per slot, dipakai saat mengirim penugasan dosen
	 * ke Feeder.</p>
	 *
	 * @return id Feeder dosen ke-6, atau {@code null} bila belum disinkronkan.
	 * @see #getDosen6()
	 */
	public String getFeeder6() {
		return feeder6;
	}

	/**
	 * Menetapkan id Feeder untuk dosen pengampu slot ke-6.
	 *
	 * @param feeder6 id Feeder dosen ke-6.
	 */
	public void setFeeder6(String feeder6) {
		this.feeder6 = feeder6;
	}

	/**
	 * Id dosen pengampu slot ke-7 pada Feeder PDDikti.
	 *
	 * <p>Sepuluh kolom {@code feeder1}..{@code feeder10} adalah pendamping pola sepuluh slot
	 * {@code dosen1}..{@code dosen10}: satu kolom per slot, dipakai saat mengirim penugasan dosen
	 * ke Feeder.</p>
	 *
	 * @return id Feeder dosen ke-7, atau {@code null} bila belum disinkronkan.
	 * @see #getDosen7()
	 */
	public String getFeeder7() {
		return feeder7;
	}

	/**
	 * Menetapkan id Feeder untuk dosen pengampu slot ke-7.
	 *
	 * @param feeder7 id Feeder dosen ke-7.
	 */
	public void setFeeder7(String feeder7) {
		this.feeder7 = feeder7;
	}

	/**
	 * Id dosen pengampu slot ke-8 pada Feeder PDDikti.
	 *
	 * <p>Sepuluh kolom {@code feeder1}..{@code feeder10} adalah pendamping pola sepuluh slot
	 * {@code dosen1}..{@code dosen10}: satu kolom per slot, dipakai saat mengirim penugasan dosen
	 * ke Feeder.</p>
	 *
	 * @return id Feeder dosen ke-8, atau {@code null} bila belum disinkronkan.
	 * @see #getDosen8()
	 */
	public String getFeeder8() {
		return feeder8;
	}

	/**
	 * Menetapkan id Feeder untuk dosen pengampu slot ke-8.
	 *
	 * @param feeder8 id Feeder dosen ke-8.
	 */
	public void setFeeder8(String feeder8) {
		this.feeder8 = feeder8;
	}

	/**
	 * Id dosen pengampu slot ke-9 pada Feeder PDDikti.
	 *
	 * <p>Sepuluh kolom {@code feeder1}..{@code feeder10} adalah pendamping pola sepuluh slot
	 * {@code dosen1}..{@code dosen10}: satu kolom per slot, dipakai saat mengirim penugasan dosen
	 * ke Feeder.</p>
	 *
	 * @return id Feeder dosen ke-9, atau {@code null} bila belum disinkronkan.
	 * @see #getDosen9()
	 */
	public String getFeeder9() {
		return feeder9;
	}

	/**
	 * Menetapkan id Feeder untuk dosen pengampu slot ke-9.
	 *
	 * @param feeder9 id Feeder dosen ke-9.
	 */
	public void setFeeder9(String feeder9) {
		this.feeder9 = feeder9;
	}

	/**
	 * Id dosen pengampu slot ke-10 pada Feeder PDDikti.
	 *
	 * <p>Sepuluh kolom {@code feeder1}..{@code feeder10} adalah pendamping pola sepuluh slot
	 * {@code dosen1}..{@code dosen10}: satu kolom per slot, dipakai saat mengirim penugasan dosen
	 * ke Feeder.</p>
	 *
	 * @return id Feeder dosen ke-10, atau {@code null} bila belum disinkronkan.
	 * @see #getDosen10()
	 */
	public String getFeeder10() {
		return feeder10;
	}

	/**
	 * Menetapkan id Feeder untuk dosen pengampu slot ke-10.
	 *
	 * @param feeder10 id Feeder dosen ke-10.
	 */
	public void setFeeder10(String feeder10) {
		this.feeder10 = feeder10;
	}

	/**
	 * Jenis evaluasi yang berlaku bagi kelas ini (mis. evaluasi akademik biasa vs skema khusus).
	 *
	 * <p><b>Efek samping:</b> bila kosong, diisi otomatis dengan
	 * {@code ConstantValues.EvaluasiAkademik} dan ditulis balik ke field, sehingga pemanggil tidak
	 * perlu menangani {@code null}.</p>
	 *
	 * @return jenis evaluasi; praktis tidak pernah {@code null}.
	 * @see JenisEvaluasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_evaluasi", nullable = true)
	public JenisEvaluasi getJenisEvaluasi() {
		jenisEvaluasi = check(jenisEvaluasi);
		if (jenisEvaluasi == null) {
			jenisEvaluasi = ConstantValues.EvaluasiAkademik;
		}
		return jenisEvaluasi;
	}

	/**
	 * Menetapkan jenis evaluasi kelas.
	 *
	 * @param jenisEvaluasi jenis evaluasi; {@code null} agar kembali ke default.
	 */
	public void setJenisEvaluasi(JenisEvaluasi jenisEvaluasi) {
		this.jenisEvaluasi = jenisEvaluasi;
	}

	/**
	 * Catatan mentah hasil sinkronisasi Feeder (kolom {@code text}), dipakai untuk menyimpan
	 * respons/rekam jejak pengiriman yang tidak muat pada {@link #getFeeder()}.
	 *
	 * <p><b>Efek samping:</b> {@code null} dinormalkan menjadi string kosong dan ditulis balik.</p>
	 *
	 * @return catatan Feeder; tidak pernah {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getFeeders() {
		if (feeders == null) {
			feeders = "";
		}

		return feeders;
	}

	/**
	 * Menetapkan catatan mentah hasil sinkronisasi Feeder.
	 *
	 * @param feeders catatan Feeder.
	 */
	public void setFeeders(String feeders) {
		this.feeders = feeders;
	}

	/**
	 * Kode periode (semester) dalam format Feeder PDDikti: tahun awal ditambah satu digit jenis
	 * semester.
	 *
	 * <p>Contoh: tahun akademik {@code "2025/2026"} semester ganjil menghasilkan {@code "20251"},
	 * genap {@code "20252"}, dan semester pendek {@code "20253"}. Digit ditentukan lebih dulu oleh
	 * {@link #getStatusSemesterPendek()}, baru oleh {@link #getGanjilGenap()}.</p>
	 *
	 * <p>Kegagalan (mis. tahun akademik kosong sehingga pemecahan teks tidak menghasilkan bagian)
	 * hanya dicatat, dan nilai lama field dikembalikan apa adanya - bisa {@code null}.</p>
	 *
	 * <p>Dipakai oleh {@code FeederExporter}, {@code FeederJSONImport}, {@code ElearningApiUtil},
	 * beberapa laporan penilaian, dan {@link Detailperkuliahan}.</p>
	 *
	 * @return kode periode Feeder, atau {@code null} bila data periode belum lengkap.
	 */
	public String getIdSmt() {
		try {
			idSmt = this.getTahunAjaran().split("/")[0] + (this.getStatusSemesterPendek() != null
					&& this.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
							: (this.getGanjilGenap().equals(Perkuliahan.GENAP) ? "2" : "1"));
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:1475");
		}
		return idSmt;
	}

	/**
	 * Menetapkan kode periode Feeder secara manual (jarang dipakai; normalnya dihitung).
	 *
	 * @param idSmt kode periode Feeder.
	 */
	public void setIdSmt(String idSmt) {
		this.idSmt = idSmt;
	}

	/**
	 * Masa perkuliahan (rentang tanggal resmi kegiatan belajar mengajar) yang menaungi kelas ini.
	 *
	 * <p>Menjadi acuan utama {@link #getTanggalMulaiPerkuliahan()} saat kelas tidak boleh
	 * menentukan tanggal mulai sendiri.</p>
	 *
	 * @return masa perkuliahan, atau {@code null}.
	 * @see MasaPerkuliahan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "masa_perkuliahan", nullable = true)
	public MasaPerkuliahan getMasaPerkuliahan() {
		masaPerkuliahan = check(masaPerkuliahan);
		return masaPerkuliahan;
	}

	/**
	 * Menetapkan masa perkuliahan yang menaungi kelas ini.
	 *
	 * @param masaPerkuliahan masa perkuliahan, boleh {@code null}.
	 */
	public void setMasaPerkuliahan(MasaPerkuliahan masaPerkuliahan) {
		this.masaPerkuliahan = masaPerkuliahan;
	}

	/**
	 * Menyatakan apakah kelas ini boleh dijadwalkan meski waktunya bentrok dengan jadwal lain.
	 *
	 * <p>Dipakai untuk kelas yang memang tumpang tindih secara sah (mis. praktikum bergilir atau
	 * kelas gabungan). Nilai {@code null} dinormalkan menjadi {@code false} dan ditulis balik.</p>
	 *
	 * @return {@code true} bila pemeriksaan bentrok waktu dilewati.
	 */
	public Boolean getAbaikanWaktuBentrokDenganJadwalLain() {
		if (abaikanWaktuBentrokDenganJadwalLain == null) {
			abaikanWaktuBentrokDenganJadwalLain = false;
		}
		return abaikanWaktuBentrokDenganJadwalLain;
	}

	/**
	 * Menetapkan apakah pemeriksaan bentrok waktu boleh dilewati untuk kelas ini.
	 *
	 * @param abaikanWaktuBentrokDenganJadwalLain penanda pengabaian bentrok.
	 */
	public void setAbaikanWaktuBentrokDenganJadwalLain(Boolean abaikanWaktuBentrokDenganJadwalLain) {
		this.abaikanWaktuBentrokDenganJadwalLain = abaikanWaktuBentrokDenganJadwalLain;
	}

	/**
	 * Mencari baris {@link KurikulumPunyaMatakuliah} yang PERSIS cocok dengan kurikulum, semester,
	 * dan mata kuliah kelas ini, lalu menyimpannya ke field.
	 *
	 * <p>Pencocokannya ketat: ketiga kriteria harus terpenuhi sekaligus, dan bila salah satu dari
	 * kurikulum/semester/mata kuliah kosong, pencarian tidak dilakukan sama sekali. Untuk pencarian
	 * yang lebih longgar dan berjenjang, pakai {@link #ambilKurikulumPunyaMatakuliah()}.</p>
	 *
	 * <p><b>Efek samping:</b> membuka dan menutup {@link Session} Hibernate sendiri, serta menimpa
	 * field {@code kurikulumPunyaMatakuliah}. TIDAK menyimpan apa pun ke basis data.</p>
	 *
	 * <p>Dipanggil dari {@code PenjadwalanUtil}, {@code TemplatePerkuliahanDetailHelper},
	 * {@code AmbilDataPaketPerkuliahanHelper}, dan {@code FeederJSONImport}.</p>
	 *
	 * @return baris kurikulum yang cocok, atau nilai field sebelumnya (bisa {@code null}) bila
	 *         tidak ditemukan.
	 */
	public KurikulumPunyaMatakuliah populateKurikulumPunyaMatakuliah() {
		kurikulum = getKurikulum();
		matakuliah = getMatakuliah();
		if (kurikulum != null && semester != null && matakuliah != null) {
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
						.createCriteria(KurikulumPunyaMatakuliah.class).add(Restrictions.eq("semester", semester))
						.add(Restrictions.eq("kurikulum", kurikulum)).add(Restrictions.eq("matakuliah", matakuliah))
						.setMaxResults(1).uniqueResult();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (session != null && session.isOpen()) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}
		return kurikulumPunyaMatakuliah;
	}

	/**
	 * Baris kurikulum (pasangan kurikulum + mata kuliah) yang menjadi acuan RPS, capaian
	 * pembelajaran, dan jumlah rencana pertemuan kelas ini.
	 *
	 * <p><b>Efek samping yang tidak terduga dari sebuah getter:</b> selain memvalidasi proxy,
	 * method ini MENAMBAHKAN id seluruh dosen pengampu kelas ({@code populateDosenBuId()}) ke
	 * daftar dosen pada baris kurikulum tersebut. Tujuannya menjaga agar daftar dosen pengampu di
	 * tingkat kurikulum ikut mutakhir mengikuti penjadwalan, tetapi artinya membaca property ini
	 * dapat mengubah objek {@link KurikulumPunyaMatakuliah} yang dikembalikan. Kegagalan pada
	 * langkah ini hanya dicatat.</p>
	 *
	 * @return baris kurikulum acuan, atau {@code null}.
	 * @see #ambilKurikulumPunyaMatakuliah()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kurikulum_punya_matakuliah", nullable = true)
	public KurikulumPunyaMatakuliah getKurikulumPunyaMatakuliah() {
		kurikulumPunyaMatakuliah = check(kurikulumPunyaMatakuliah);

		if (kurikulumPunyaMatakuliah != null) {
			try {

				String p = kurikulumPunyaMatakuliah.getDosen();
				List<Long> dosens = populateDosenBuId();
				for (Long idDosen : dosens) {
					if (!kurikulumPunyaMatakuliah.getDosen().contains("," + idDosen + ",")) {
						p += p.isEmpty() ? idDosen + "" : "," + idDosen;
					}
				}
				kurikulumPunyaMatakuliah.setDosen(p);
				dosens.clear();
				dosens = null;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:1547");
				// TODO: handle exception
			}
		}

		return kurikulumPunyaMatakuliah;
	}

	/**
	 * Mencari baris kurikulum acuan secara BERJENJANG, dan menyimpan temuannya ke basis data.
	 *
	 * <p>Berbeda dengan {@link #populateKurikulumPunyaMatakuliah()} yang menuntut kecocokan persis,
	 * method ini dipakai ketika kelas belum tertaut ke baris kurikulum mana pun dan sistem tetap
	 * perlu RPS/capaian pembelajaran. Urutan usahanya:</p>
	 * <ol>
	 * <li>Nilai yang sudah tersimpan ({@link #getKurikulumPunyaMatakuliah()}) - bila ada, langsung
	 * dipakai.</li>
	 * <li>Kurikulum milik jurusan kelas ini DENGAN program yang sama, mata kuliah sama; diambil
	 * kurikulum tahun terbaru.</li>
	 * <li>Kurikulum milik jurusan yang sama TANPA program, mata kuliah sama.</li>
	 * <li>Kurikulum milik jurusan yang sama tanpa program, dicocokkan lewat KODE mata kuliah
	 * (bukan objeknya) - menampung kasus mata kuliah terduplikasi antar kurikulum. Blok ini
	 * ditulis dua kali secara identik pada kode aslinya, jadi usaha keempat efektif hanya
	 * sekali.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> begitu baris ditemukan, hasilnya DITULIS ke kolom
	 * {@code kurikulum_punya_matakuliah} lewat {@code Common.refreshUpdate(...)} pada session
	 * berjalan - jadi method ini mengubah basis data. Ia juga mewarisi efek samping penambahan id
	 * dosen dari {@link #getKurikulumPunyaMatakuliah()}.</p>
	 *
	 * <p>Dipanggil dari {@code AktifitasPerkuliahanHelper}, {@code PenjadwalanHelper},
	 * {@code PertemuanPunyaUjianHelper}, dan helper OBE.</p>
	 *
	 * @return baris kurikulum acuan, atau {@code null} bila semua usaha gagal.
	 */
	public KurikulumPunyaMatakuliah ambilKurikulumPunyaMatakuliah() {
		KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = getKurikulumPunyaMatakuliah();

		if (kurikulumPunyaMatakuliah == null) {
			Perkuliahan perkuliahan = this;
			kurikulumPunyaMatakuliah = perkuliahan.getKurikulumPunyaMatakuliah();

			if (kurikulumPunyaMatakuliah == null && perkuliahan != null && perkuliahan.getKurikulum() != null
					&& perkuliahan.getMatakuliah() != null) {
				Session session = HibernateUtil.currentSession();
				Jurusan jurusan = perkuliahan.getJurusan();
				String program = perkuliahan.getProgram();
				Matakuliah matakuliah = perkuliahan.getMatakuliah();

				kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
						.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("kurikulum", "kurikulum")
						.createAlias("kurikulum.program", "program").add(Restrictions.eq("kurikulum.jurusan", jurusan))
						.add(Restrictions.eq("program.nama", program)).add(Restrictions.eq("matakuliah", matakuliah))
						.addOrder(Order.desc("kurikulum.tahun")).addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();

				if (kurikulumPunyaMatakuliah == null) {
					kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
							.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("kurikulum", "kurikulum")
							.add(Restrictions.eq("kurikulum.jurusan", jurusan))
							.add(Restrictions.isNull("kurikulum.program"))
							.add(Restrictions.eq("matakuliah", matakuliah)).addOrder(Order.desc("kurikulum.tahun"))
							.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
				}

				if (kurikulumPunyaMatakuliah == null) {
					kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
							.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("kurikulum", "kurikulum")
							.createAlias("matakuliah", "matakuliah").add(Restrictions.eq("kurikulum.jurusan", jurusan))
							.add(Restrictions.isNull("kurikulum.program"))
							.add(Restrictions.ilike("matakuliah.kode", matakuliah.getKode(), MatchMode.EXACT))
							.addOrder(Order.desc("kurikulum.tahun")).addOrder(Order.desc("id")).setMaxResults(1)
							.uniqueResult();
				}

				if (kurikulumPunyaMatakuliah == null) {
					kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
							.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("kurikulum", "kurikulum")
							.createAlias("matakuliah", "matakuliah").add(Restrictions.eq("kurikulum.jurusan", jurusan))
							.add(Restrictions.isNull("kurikulum.program"))
							.add(Restrictions.ilike("matakuliah.kode", matakuliah.getKode(), MatchMode.EXACT))
							.addOrder(Order.desc("kurikulum.tahun")).addOrder(Order.desc("id")).setMaxResults(1)
							.uniqueResult();
				}

				if (kurikulumPunyaMatakuliah != null) {
					perkuliahan.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
					Common.refreshUpdate(session, perkuliahan);
				}
			}
		}

		return kurikulumPunyaMatakuliah;
	}

	/**
	 * Menetapkan baris kurikulum acuan kelas ini.
	 *
	 * @param kurikulumPunyaMatakuliah baris kurikulum, boleh {@code null}.
	 */
	public void setKurikulumPunyaMatakuliah(KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah) {
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliah;
	}

	/**
	 * Keterangan bebas mengenai kelas ini (catatan pengelola, syarat khusus, dsb).
	 *
	 * <p><b>Efek samping:</b> isi dilewatkan {@code filterTidakBoleh(...)} untuk membuang markup
	 * berbahaya, dan hasil bersihnya ditulis balik ke field.</p>
	 *
	 * @return keterangan yang sudah disaring dan di-trim; string kosong bila belum diisi.
	 */
	public String getKeterangan() {

		keterangan = filterTidakBoleh(keterangan);

		return keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Menetapkan keterangan bebas kelas.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Deskripsi pembelajaran kelas, dengan pewarisan berjenjang dari kurikulum lalu mata kuliah.
	 *
	 * <p>Bila kolom pada kelas ini masih kosong, isinya DIAMBIL dari
	 * {@link KurikulumPunyaMatakuliah} terlebih dahulu; bila itu pun kosong, dari
	 * {@link Matakuliah}. Rantai inilah yang membuat kelas baru langsung memiliki RPS tanpa
	 * pengetikan ulang, sekaligus membiarkan dosen menimpanya per kelas.</p>
	 *
	 * <p><b>Efek samping:</b> hasil pewarisan dan hasil {@code filterTidakBoleh(...)} DITULIS BALIK
	 * ke field, dan pembacaan kurikulum acuan membawa efek samping
	 * {@link #getKurikulumPunyaMatakuliah()}.</p>
	 *
	 * @return deskripsi pembelajaran; string kosong bila tidak tersedia di seluruh rantai.
	 */
	@Column(columnDefinition = "text")
	public String getDeskripsiPembelajaran() {
		kurikulumPunyaMatakuliah = getKurikulumPunyaMatakuliah();
		matakuliah = getMatakuliah();
		try {
			if ((deskripsiPembelajaran == null || deskripsiPembelajaran.trim().isEmpty())
					&& kurikulumPunyaMatakuliah != null
					&& !kurikulumPunyaMatakuliah.getDeskripsiPembelajaran().trim().isEmpty()) {
				deskripsiPembelajaran = kurikulumPunyaMatakuliah.getDeskripsiPembelajaran();
			}

			else if ((deskripsiPembelajaran == null || deskripsiPembelajaran.trim().isEmpty())
					&& !matakuliah.getDeskripsiPembelajaran().isEmpty()) {
				deskripsiPembelajaran = matakuliah.getDeskripsiPembelajaran();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:1644");
			// TODO: handle exception
		}

		deskripsiPembelajaran = filterTidakBoleh(deskripsiPembelajaran);

		return deskripsiPembelajaran == null ? "" : deskripsiPembelajaran.trim();
	}

	/**
	 * Menetapkan deskripsi pembelajaran khusus kelas ini (menimpa warisan kurikulum/mata kuliah).
	 *
	 * @param deskripsiPembelajaran teks deskripsi; kosongkan agar kembali mewarisi.
	 */
	public void setDeskripsiPembelajaran(String deskripsiPembelajaran) {
		this.deskripsiPembelajaran = deskripsiPembelajaran;
	}

	/**
	 * Capaian pembelajaran program studi yang dibebankan pada kelas ini.
	 *
	 * <p>Aturan pewarisan berjenjang dan efek sampingnya identik dengan
	 * {@link #getDeskripsiPembelajaran()}: kurikulum lebih dulu, lalu mata kuliah, dengan hasil
	 * ditulis balik ke field dan disaring {@code filterTidakBoleh(...)}.</p>
	 *
	 * @return capaian pembelajaran prodi; string kosong bila tidak tersedia.
	 */
	@Column(columnDefinition = "text")
	public String getCapaianPembelajaranProdi() {
		kurikulumPunyaMatakuliah = getKurikulumPunyaMatakuliah();
		matakuliah = getMatakuliah();
		try {
			if ((capaianPembelajaranProdi == null || capaianPembelajaranProdi.trim().isEmpty())
					&& kurikulumPunyaMatakuliah != null
					&& !kurikulumPunyaMatakuliah.getCapaianPembelajaranProdi().trim().isEmpty()) {
				capaianPembelajaranProdi = kurikulumPunyaMatakuliah.getCapaianPembelajaranProdi();
			}

			else if ((capaianPembelajaranProdi == null || capaianPembelajaranProdi.trim().isEmpty())
					&& !matakuliah.getCapaianPembelajaranProdi().isEmpty()) {
				capaianPembelajaranProdi = matakuliah.getCapaianPembelajaranProdi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:1672");
			// TODO: handle exception
		}

		capaianPembelajaranProdi = filterTidakBoleh(capaianPembelajaranProdi);

		return capaianPembelajaranProdi == null ? "" : capaianPembelajaranProdi.trim();
	}

	/**
	 * Menetapkan capaian pembelajaran prodi khusus kelas ini.
	 *
	 * @param capaianPembelajaranProdi teks capaian pembelajaran; kosongkan agar kembali mewarisi.
	 */
	public void setCapaianPembelajaranProdi(String capaianPembelajaranProdi) {
		this.capaianPembelajaranProdi = capaianPembelajaranProdi;
	}

	/**
	 * Penanda asrama tempat kelas diselenggarakan, untuk institusi yang memisahkan kelas per
	 * asrama.
	 *
	 * @return kode/nama asrama, atau {@code null}.
	 */
	public String getAsrama() {
		return asrama;
	}

	/**
	 * Menetapkan asrama tempat kelas diselenggarakan.
	 *
	 * @param asrama kode/nama asrama, boleh {@code null}.
	 */
	public void setAsrama(String asrama) {
		this.asrama = asrama;
	}

	/**
	 * Menyatakan apakah kelas ini boleh dipilih mahasiswa pada layar pengambilan KRS.
	 *
	 * <p>Default {@code true}: kelas yang tidak sengaja disembunyikan tetap terlihat. Setel
	 * {@code false} untuk kelas yang pesertanya ditentukan pengelola (paket, kelas khusus).</p>
	 *
	 * @return {@code true} bila kelas tampil di pengambilan KRS.
	 */
	public Boolean getTampilkanSaatPengambilanKrs() {
		return tampilkanSaatPengambilanKrs == null ? true : tampilkanSaatPengambilanKrs;
	}

	/**
	 * Menetapkan apakah kelas tampil pada layar pengambilan KRS.
	 *
	 * @param tampilkanSaatPengambilanKrs penanda tampil.
	 */
	public void setTampilkanSaatPengambilanKrs(Boolean tampilkanSaatPengambilanKrs) {
		this.tampilkanSaatPengambilanKrs = tampilkanSaatPengambilanKrs;
	}

	/**
	 * Tanggal pertemuan pertama kelas ini - titik awal seluruh penjadwalan pertemuan.
	 *
	 * <p>Bila kelas TIDAK diizinkan menentukan tanggal mulai sendiri
	 * ({@link #getBolehMenentukanTanggalMulaiPerkuliahan()} bernilai {@code false}), tanggal
	 * DIHITUNG ULANG setiap kali dibaca:</p>
	 * <ol>
	 * <li>Acuan pertama adalah awal {@link MasaPerkuliahan}; bila masa perkuliahan tidak tersedia,
	 * acuannya {@link #getAwalPerkuliahan()} (tanggal mulai belajar-mengajar dari rencana tahun
	 * akademik).</li>
	 * <li>Bila nama hari tanggal acuan sudah sama dengan {@link #getHari()}, tanggal itulah yang
	 * dipakai.</li>
	 * <li>Bila belum, tanggal digeser maju satu hari demi satu hari sampai nama harinya cocok,
	 * dengan pengaman maksimal 10 iterasi agar tidak berputar selamanya bila hari kelas berisi
	 * teks yang tidak dikenal.</li>
	 * </ol>
	 *
	 * <p>Penyeragaman ejaan {@code "Jumat"} menjadi {@code "Jum'at"} dilakukan di setiap
	 * pembandingan - tanpa itu kelas hari Jumat tidak akan pernah cocok (lihat
	 * {@link #getHari()}).</p>
	 *
	 * <p><b>Efek samping:</b> menulis balik ke field {@code tanggalMulaiPerkuliahan} dan
	 * {@code awalPerkuliahan}; jalur {@link #getAwalPerkuliahan()} juga melakukan query rencana
	 * tahun akademik. Karena itu getter ini relatif mahal dan tidak boleh dipanggil di dalam
	 * perulangan ketat.</p>
	 *
	 * @return tanggal pertemuan pertama, atau {@code null} bila acuan tanggal belum tersedia.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalMulaiPerkuliahan() {

		if (!getBolehMenentukanTanggalMulaiPerkuliahan()) {
			masaPerkuliahan = getMasaPerkuliahan();
			if ((tanggalMulaiPerkuliahan == null
					|| (masaPerkuliahan != null && masaPerkuliahan.getTanggalMulaiHarusSesuaiJadwal()))
					&& masaPerkuliahan != null && masaPerkuliahan.getMulai() != null && this.hari != null
					&& !this.hari.trim().isEmpty()) {

				String har = Common.dateFormat4Week.get().format(masaPerkuliahan.getMulai());
				if (har.equals("Jumat")) {
					har = "Jum'at";
				}

				if (har.equalsIgnoreCase(hari)) {
					tanggalMulaiPerkuliahan = masaPerkuliahan.getMulai();
				} else {

					Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
					calendar.setTime(masaPerkuliahan.getMulai());
					tanggalMulaiPerkuliahan = calendar.getTime();
//					System.out.println("calendar.get(Calendar.DAY_OF_WEEK) => " + calendar.get(Calendar.DAY_OF_WEEK)
//							+ ", " + Common.dateFormat4.get().format(tanggalMulaiPerkuliahan));
					int index = 0;
					while (true) {
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
						tanggalMulaiPerkuliahan = calendar.getTime();
//						System.out.println("calendar.get(Calendar.DAY_OF_WEEK) => " + calendar.get(Calendar.DAY_OF_WEEK)
//								+ ", " + Common.dateFormat4.get().format(tanggalMulaiPerkuliahan));
						har = Common.dateFormat4Week.get().format(tanggalMulaiPerkuliahan);
						if (har.equals("Jumat")) {
							har = "Jum'at";
						}
						if (har.equalsIgnoreCase(hari)) {
							break;
						}
						index++;
						if (index == 10) {
							break;
						}
					}
				}
			} else {
				awalPerkuliahan = getAwalPerkuliahan();
				if (awalPerkuliahan != null && hari != null) {

					String har = Common.dateFormat4Week.get().format(awalPerkuliahan);
					if (har.equals("Jumat")) {
						har = "Jum'at";
					}

					if (har.equalsIgnoreCase(hari)) {
						tanggalMulaiPerkuliahan = awalPerkuliahan;
					} else {

						Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
						calendar.setTime(awalPerkuliahan);
						tanggalMulaiPerkuliahan = calendar.getTime();
//						System.out.println("calendar.get(Calendar.DAY_OF_WEEK) => " + calendar.get(Calendar.DAY_OF_WEEK)
//								+ ", " + Common.dateFormat4.get().format(tanggalMulaiPerkuliahan));
						int index = 0;
						while (true) {
							calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
							tanggalMulaiPerkuliahan = calendar.getTime();
//							System.out.println(
//									"calendar.get(Calendar.DAY_OF_WEEK) => " + calendar.get(Calendar.DAY_OF_WEEK) + ", "
//											+ Common.dateFormat4.get().format(tanggalMulaiPerkuliahan));
							har = Common.dateFormat4Week.get().format(tanggalMulaiPerkuliahan);
							if (har.equals("Jumat")) {
								har = "Jum'at";
							}
							if (har.equalsIgnoreCase(hari)) {
								break;
							}
							index++;
							if (index == 10) {
								break;
							}
						}
					}

				}
			}
		}

		return tanggalMulaiPerkuliahan;
	}

	/**
	 * Menetapkan tanggal pertemuan pertama secara manual.
	 *
	 * <p>Hanya bertahan bila {@link #getBolehMenentukanTanggalMulaiPerkuliahan()} bernilai
	 * {@code true}; selain itu nilainya akan dihitung ulang saat dibaca.</p>
	 *
	 * @param tanggalMulaiPerkuliahan tanggal pertemuan pertama.
	 */
	public void setTanggalMulaiPerkuliahan(Date tanggalMulaiPerkuliahan) {
		this.tanggalMulaiPerkuliahan = tanggalMulaiPerkuliahan;
	}

	// public Boolean getJanganAmbilSilabusDariKurikulum() {
	// return janganAmbilSilabusDariKurikulum == null ? false :
	// janganAmbilSilabusDariKurikulum;
	// }
	//
	// public void setJanganAmbilSilabusDariKurikulum(Boolean
	// janganAmbilSilabusDariKurikulum) {
	// this.janganAmbilSilabusDariKurikulum = janganAmbilSilabusDariKurikulum;
	// }

	/**
	 * Menyatakan apakah dosen pengampu boleh menggeser tanggal pertemuan kelas ini.
	 *
	 * <p>Bila belum ditentukan per kelas, nilainya diambil dari konfigurasi global
	 * {@code secara_default_dosen_bisa_merubah_tanggal_perkuliahan}. Perhatikan bahwa pembacaan
	 * konfigurasi di sistem ini dapat MENULIS nilai default ke basis data bila kunci konfigurasi
	 * belum ada.</p>
	 *
	 * @return {@code true} bila dosen boleh mengubah tanggal pertemuan.
	 */
	public Boolean getDosenBisaMerubahTanggalPerkuliahan() {
		return dosenBisaMerubahTanggalPerkuliahan == null
				? Common.bolehKonfigurasi("secara_default_dosen_bisa_merubah_tanggal_perkuliahan")
				: dosenBisaMerubahTanggalPerkuliahan;
	}

	/**
	 * Menetapkan izin dosen mengubah tanggal pertemuan untuk kelas ini.
	 *
	 * @param dosenBisaMerubahTanggalPerkuliahan {@code null} agar mengikuti konfigurasi global.
	 */
	public void setDosenBisaMerubahTanggalPerkuliahan(Boolean dosenBisaMerubahTanggalPerkuliahan) {
		this.dosenBisaMerubahTanggalPerkuliahan = dosenBisaMerubahTanggalPerkuliahan;
	}

	/**
	 * Menyatakan apakah pembangkitan pertemuan harus MELEWATI hari libur nasional.
	 *
	 * <p>Default {@code true}: pertemuan yang jatuh pada tanggal merah digeser ke jadwal berikutnya
	 * alih-alih dijadwalkan pada hari libur.</p>
	 *
	 * @return {@code true} bila tanggal merah nasional dilewati.
	 */
	public Boolean getLewatiTanggalMerahNasional() {
		return lewatiTanggalMerahNasional == null ? true : lewatiTanggalMerahNasional;
	}

	/**
	 * Menetapkan apakah pembangkitan pertemuan melewati hari libur nasional.
	 *
	 * @param lewatiTanggalMerahNasional penanda lewati tanggal merah.
	 */
	public void setLewatiTanggalMerahNasional(Boolean lewatiTanggalMerahNasional) {
		this.lewatiTanggalMerahNasional = lewatiTanggalMerahNasional;
	}

	/**
	 * Menyatakan apakah dosen hanya boleh mengisi presensi pada rentang waktu jadwal kelas.
	 *
	 * <p>Salah satu dari lima penanda pengetatan presensi yang saling melengkapi:
	 * dua untuk dosen (jadwal dan alamat IP), dua untuk mahasiswa, dan satu kelonggaran untuk
	 * admin ({@link #getAdminBolehMenginputKehadiranDiluarJadwalDanIp()}). Default {@code false}
	 * agar kelas lama tidak mendadak terkunci.</p>
	 *
	 * @return {@code true} bila presensi dosen dibatasi jadwal.
	 */
	public Boolean getKehadiranDosenHarusDiinputSesuaiJadwal() {
		return kehadiranDosenHarusDiinputSesuaiJadwal == null ? false : kehadiranDosenHarusDiinputSesuaiJadwal;
	}

	/**
	 * Menetapkan pembatasan presensi dosen menurut jadwal.
	 *
	 * @param kehadiranDosenHarusDiinputSesuaiJadwal penanda pembatasan.
	 */
	public void setKehadiranDosenHarusDiinputSesuaiJadwal(Boolean kehadiranDosenHarusDiinputSesuaiJadwal) {
		this.kehadiranDosenHarusDiinputSesuaiJadwal = kehadiranDosenHarusDiinputSesuaiJadwal;
	}

	/**
	 * Menyatakan apakah dosen hanya boleh mengisi presensi dari alamat IP yang telah ditentukan
	 * (mis. jaringan kampus). Default {@code false}.
	 *
	 * @return {@code true} bila presensi dosen dibatasi alamat IP.
	 */
	public Boolean getKehadiranDosenHarusDiinputDiIpYangDitentukan() {
		return kehadiranDosenHarusDiinputDiIpYangDitentukan == null ? false
				: kehadiranDosenHarusDiinputDiIpYangDitentukan;
	}

	/**
	 * Menetapkan pembatasan presensi dosen menurut alamat IP.
	 *
	 * @param kehadiranDosenHarusDiinputDiIpYangDitentukan penanda pembatasan.
	 */
	public void setKehadiranDosenHarusDiinputDiIpYangDitentukan(Boolean kehadiranDosenHarusDiinputDiIpYangDitentukan) {
		this.kehadiranDosenHarusDiinputDiIpYangDitentukan = kehadiranDosenHarusDiinputDiIpYangDitentukan;
	}

	/**
	 * Menyatakan apakah mahasiswa hanya boleh mengisi presensi pada rentang waktu jadwal kelas.
	 * Default {@code false}.
	 *
	 * @return {@code true} bila presensi mahasiswa dibatasi jadwal.
	 * @see #getBolehAbsenSebelumWaktuMulaiDalamMenit()
	 */
	public Boolean getKehadiranMahasiswaHarusDiinputSesuaiJadwal() {
		return kehadiranMahasiswaHarusDiinputSesuaiJadwal == null ? false : kehadiranMahasiswaHarusDiinputSesuaiJadwal;
	}

	/**
	 * Menetapkan pembatasan presensi mahasiswa menurut jadwal.
	 *
	 * @param kehadiranMahasiswaHarusDiinputSesuaiJadwal penanda pembatasan.
	 */
	public void setKehadiranMahasiswaHarusDiinputSesuaiJadwal(Boolean kehadiranMahasiswaHarusDiinputSesuaiJadwal) {
		this.kehadiranMahasiswaHarusDiinputSesuaiJadwal = kehadiranMahasiswaHarusDiinputSesuaiJadwal;
	}

	/**
	 * Menyatakan apakah mahasiswa hanya boleh mengisi presensi dari alamat IP yang telah
	 * ditentukan. Default {@code false}.
	 *
	 * @return {@code true} bila presensi mahasiswa dibatasi alamat IP.
	 */
	public Boolean getKehadiranMahasiswaHarusDiinputDiIpYangDitentukan() {
		return kehadiranMahasiswaHarusDiinputDiIpYangDitentukan == null ? false
				: kehadiranMahasiswaHarusDiinputDiIpYangDitentukan;
	}

	/**
	 * Menetapkan pembatasan presensi mahasiswa menurut alamat IP.
	 *
	 * @param kehadiranMahasiswaHarusDiinputDiIpYangDitentukan penanda pembatasan.
	 */
	public void setKehadiranMahasiswaHarusDiinputDiIpYangDitentukan(
			Boolean kehadiranMahasiswaHarusDiinputDiIpYangDitentukan) {
		this.kehadiranMahasiswaHarusDiinputDiIpYangDitentukan = kehadiranMahasiswaHarusDiinputDiIpYangDitentukan;
	}

	/**
	 * Kelonggaran bagi admin: boleh mengisi presensi di luar jadwal maupun di luar alamat IP yang
	 * ditentukan.
	 *
	 * <p>Default {@code true} - tanpa ini, pengetatan presensi akan ikut memblokir petugas yang
	 * harus memperbaiki data presensi setelah kelas usai.</p>
	 *
	 * @return {@code true} bila admin dikecualikan dari pembatasan jadwal dan IP.
	 */
	public Boolean getAdminBolehMenginputKehadiranDiluarJadwalDanIp() {
		return adminBolehMenginputKehadiranDiluarJadwalDanIp == null ? true
				: adminBolehMenginputKehadiranDiluarJadwalDanIp;
	}

	/**
	 * Menetapkan kelonggaran admin terhadap pembatasan presensi.
	 *
	 * @param adminBolehMenginputKehadiranDiluarJadwalDanIp penanda kelonggaran.
	 */
	public void setAdminBolehMenginputKehadiranDiluarJadwalDanIp(
			Boolean adminBolehMenginputKehadiranDiluarJadwalDanIp) {
		this.adminBolehMenginputKehadiranDiluarJadwalDanIp = adminBolehMenginputKehadiranDiluarJadwalDanIp;
	}

	@SuppressWarnings("unchecked")
	public String populateInfoPersetujuanBiasa() throws Exception {
		if (id == null) {
			return "";
		}
		// if (!getUdah()) {
		// reInitDetailperkuliahan(HibernateUtil.currentSession());
		// setUdah(true);
		// }

		Integer[] s = ambilStatusKrs();

		Integer telahDisetujui = s[1];
		Integer belumDisetujui = s[0];
		Integer countDinilai = s[2];

		String info = "";
		if (telahDisetujui == null || belumDisetujui == null || telahDisetujui.equals(0) && belumDisetujui.equals(0)) {
			info = "belum ada data mahasiswa";
		} else if (telahDisetujui > 0 && belumDisetujui.equals(0)) {
			info = telahDisetujui + " mahasiswa telah disetujui";
		} else if (belumDisetujui > 0 && telahDisetujui.equals(0)) {
			info = belumDisetujui + " mahasiswa belum disetujui";
		} else {
			info = "terdapat " + belumDisetujui + " mahasiswa belum disetujui dan " + telahDisetujui
					+ " mahasiswa telah disetujui";
		}

		if (countDinilai > 0) {
			info += ", " + countDinilai + " mahasiswa telah dinilai";
		} else {
			info += ", belum ada mahasiswa yang dinilai";
		}

		Object[] jml = ambilJumlahPertemuanStatistik(false, false);
		int total = jml == null || jml[0] == null ? 0 : Integer.parseInt(jml[0].toString());
		if (total > 0) {
			int jumlah = jml == null || jml[1] == null ? 0 : Integer.parseInt(jml[1].toString());
			int absen = jml == null || jml[3] == null ? 0 : Integer.parseInt(jml[3].toString());
			int mhsSize = telahDisetujui + belumDisetujui;
			Map<String, Integer> statuses = (Map<String, Integer>) (jml == null || jml[4] == null ? null : jml[4]);
			String abs = statuses == null ? "" : statuses.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();

			int absenDosen = jml == null || jml[5] == null ? 0 : Integer.parseInt(jml[5].toString());
			Map<String, Integer> statusesDosen = (Map<String, Integer>) (jml == null || jml[6] == null ? null : jml[6]);
			String absDosen = statusesDosen == null ? ""
					: statusesDosen.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();

			Integer persen = total == jumlah ? 100 : ((jumlah * 100) / total);
			info += ", Tuntas " + jumlah + "/" + total + " (" + persen + "%)";
			int totalMhs = mhsSize * total;
			if (mhsSize > 0) {
				persen = totalMhs == absen ? 100 : ((absen * 100) / totalMhs);
				info += ", Kehadiran mhs " + absen + "/" + totalMhs + " (" + persen + "%), " + abs;
			}
			if (getJumlahDosen() > 0) {
				int totalDosen = getJumlahDosen() * total;
				persen = totalDosen == absenDosen ? 100 : ((absenDosen * 100) / totalDosen);
				info += ", Kehadiran dosen " + absenDosen + "/" + totalDosen + " (" + persen + "%), " + absDosen;
			}
		}
		return info;
	}

	@SuppressWarnings("unchecked")
	public String populateInfoPersetujuan() throws Exception {
		if (id == null) {
			return "";
		}
		String info = "";
		try {
			Integer[] s = ambilStatusKrs();

			Integer telahDisetujui = s[1];
			Integer belumDisetujui = s[0];
			Integer countDinilai = s[2];
			Integer countBelumDinilai = s[3];

			if (telahDisetujui == null || belumDisetujui == null
					|| telahDisetujui.equals(0) && belumDisetujui.equals(0)) {
				info = "<font style='font-size:10px;color:black;'>belum ada data mahasiswa</font>";
			} else if (telahDisetujui > 0 && belumDisetujui.equals(0)) {
				info = "<font style='font-size:10px;color:blue;'>" + telahDisetujui
						+ " mahasiswa telah disetujui</font>";
			} else if (belumDisetujui > 0 && telahDisetujui.equals(0)) {
				info = "<font style='font-size:10px;color:red;'>" + belumDisetujui
						+ " mahasiswa belum disetujui</font>";
			} else {
				info = "terdapat <font style='font-size:10px;color:red;'>" + belumDisetujui
						+ " mahasiswa belum disetujui</font> dan <font style='font-size:10px;color:blue;'>"
						+ telahDisetujui + " mahasiswa telah disetujui</font>";
			}

			if (countDinilai > 0 && countBelumDinilai > 0) {
				info += "<font style='font-size:10px;color:green;'>, " + countDinilai
						+ " mahasiswa telah dinilai, </font>" + "<font style='font-size:10px;color:pink;'>, "
						+ countBelumDinilai + " mahasiswa belum dinilai ("
						+ Common.numberFormat.get().format((countDinilai * 100.0) / (countDinilai + countBelumDinilai))
						+ "%)</font>";
			} else if (countDinilai > 0) {
				info += "<font style='font-size:10px;color:green;'>, " + countDinilai
						+ " mahasiswa telah dinilai (100%)</font>";
			} else if (countBelumDinilai > 0) {
				info += "<font style='font-size:10px;color:pink;'>, " + countBelumDinilai
						+ " mahasiswa belum dinilai (0%)</font>";
			} else {
				info += "<font style='font-size:10px;color:green;'>, belum ada mahasiswa yang dinilai</font>";
			}

			Object[] jml = ambilJumlahPertemuanStatistik(false, false);
			int total = jml == null || jml[0] == null ? 0 : Integer.parseInt(jml[0].toString());
			if (total > 0) {
				int jumlah = jml == null || jml[1] == null ? 0 : Integer.parseInt(jml[1].toString());
				int absen = jml == null || jml[3] == null ? 0 : Integer.parseInt(jml[3].toString());
				int mhsSize = telahDisetujui + belumDisetujui;
				Map<String, Integer> statuses = (Map<String, Integer>) (jml == null || jml[4] == null ? null : jml[4]);
				String abs = statuses == null ? ""
						: statuses.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();

				int absenDosen = jml == null || jml[5] == null ? 0 : Integer.parseInt(jml[5].toString());
				Map<String, Integer> statusesDosen = (Map<String, Integer>) (jml == null || jml[6] == null ? null
						: jml[6]);
				String absDosen = statusesDosen == null ? ""
						: statusesDosen.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();

				Integer persen = total == jumlah ? 100 : ((jumlah * 100) / total);
				info += "<font style='font-size:10px;color:#00a3b5;'>, Tuntas " + jumlah + "/" + total + " (" + persen
						+ "%)";
				int totalMhs = mhsSize * total;
				if (mhsSize > 0) {
					persen = totalMhs == absen ? 100 : ((absen * 100) / totalMhs);
					info += ", Kehadiran mhs " + absen + "/" + totalMhs + " (" + persen + "%), " + abs;
				}
				if (getJumlahDosen() > 0) {
					int totalDosen = getJumlahDosen() * total;
					persen = totalDosen == absenDosen ? 100 : ((absenDosen * 100) / totalDosen);
					info += ", Kehadiran dosen " + absenDosen + "/" + totalDosen + " (" + persen + "%), " + absDosen;
				}
				info += "</font>";
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:2008");
		}
		return info;
	}

	public Integer getJumlahMaksimalPertemuan() {
		try {
			// Bila flag "ikuti kurikulum" aktif, SELALU baca dari kurikulum (bukan nilai
			// lama di DB). Bila flag tidak aktif, cukup cek null (perilaku lama).
			if (jumlahMaksimalPertemuan == null
					|| Boolean.TRUE.equals(getJumlahRencanaPertemuanMengikutiKurikulum())) {
				kurikulumPunyaMatakuliah = getKurikulumPunyaMatakuliah();
				if (getJumlahRencanaPertemuanMengikutiKurikulum() && kurikulumPunyaMatakuliah != null) {
					jumlahMaksimalPertemuan = kurikulumPunyaMatakuliah.getJumlahPertemuanPerkuliahanDefault();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2024");
			// TODO: handle exception
		}
		return jumlahMaksimalPertemuan == null || jumlahMaksimalPertemuan.equals(0) ? 16 : jumlahMaksimalPertemuan;
	}

	public void setJumlahMaksimalPertemuan(Integer jumlahMaksimalPertemuan) {
		this.jumlahMaksimalPertemuan = jumlahMaksimalPertemuan;
	}

	public Boolean getMerupakanRemedial() {
		return merupakanRemedial == null ? false : merupakanRemedial;
	}

	public void setMerupakanRemedial(Boolean merupakanRemedial) {
		this.merupakanRemedial = merupakanRemedial;
	}

	public Boolean getJumlahRencanaPertemuanMengikutiKurikulum() {
		return jumlahRencanaPertemuanMengikutiKurikulum == null ? true : jumlahRencanaPertemuanMengikutiKurikulum;
	}

	public void setJumlahRencanaPertemuanMengikutiKurikulum(Boolean jumlahRencanaPertemuanMengikutiKurikulum) {
		this.jumlahRencanaPertemuanMengikutiKurikulum = jumlahRencanaPertemuanMengikutiKurikulum;
	}

	public Boolean getMerupakanPraPerkuliahan() {
		return merupakanPraPerkuliahan == null ? false : merupakanPraPerkuliahan;
	}

	public void setMerupakanPraPerkuliahan(Boolean merupakanPraPerkuliahan) {
		this.merupakanPraPerkuliahan = merupakanPraPerkuliahan;
	}

	public String getKeteranganJadwal() {
		return keteranganJadwal == null ? "" : keteranganJadwal;
	}

	public void setKeteranganJadwal(String keteranganJadwal) {
		this.keteranganJadwal = keteranganJadwal;
	}

	public Boolean getTerdapatKegiatanPraktek() {
		if (matakuliah != null && matakuliah.getSksPraktek() > 0) {
			terdapatKegiatanPraktek = true;
		}
		return terdapatKegiatanPraktek == null ? false : terdapatKegiatanPraktek;
	}

	public void setTerdapatKegiatanPraktek(Boolean terdapatKegiatanPraktek) {
		this.terdapatKegiatanPraktek = terdapatKegiatanPraktek;
	}

	public Boolean getMerupakanPerkuliahanUmum() {
		return merupakanPerkuliahanUmum == null ? false : merupakanPerkuliahanUmum;
	}

	public void setMerupakanPerkuliahanUmum(Boolean merupakanPerkuliahanUmum) {
		this.merupakanPerkuliahanUmum = merupakanPerkuliahanUmum;
	}

	public Boolean getAmbilMkDiluarSemesterKurikulum() {
		return ambilMkDiluarSemesterKurikulum == null ? false : ambilMkDiluarSemesterKurikulum;
	}

	public void setAmbilMkDiluarSemesterKurikulum(Boolean ambilMkDiluarSemesterKurikulum) {
		this.ambilMkDiluarSemesterKurikulum = ambilMkDiluarSemesterKurikulum;
	}

	public String getJenis() {
		return jenis == null ? "Mingguan" : jenis;
	}

	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	public Boolean getSembunyikanNilaiJikaBelumDiverifikasi() {
		if (sembunyikanNilaiJikaBelumDiverifikasi == null) {
			sembunyikanNilaiJikaBelumDiverifikasi = Common.bolehKonfigurasi("sembunyikanNilaiJikaBelumDiverifikasi", Konfigurasi.TIDAK_AKTIF);
		}
		return sembunyikanNilaiJikaBelumDiverifikasi == null ? false : sembunyikanNilaiJikaBelumDiverifikasi;
	}

	public void setSembunyikanNilaiJikaBelumDiverifikasi(Boolean sembunyikanNilaiJikaBelumDiverifikasi) {
		this.sembunyikanNilaiJikaBelumDiverifikasi = sembunyikanNilaiJikaBelumDiverifikasi;
	}

	public String ambilLokasiDetailPerkuliahan() {
		Perkuliahan p = this.getPerkuliahan_paralel() == null ? this : this.getPerkuliahan_paralel();
		File file = Common.getFileLocation(p, "detail_perkuliahan_" + p.getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2119");
		}
		return VOMahasiswa.dataJSON;
	}

	private Object ambilLockDetailPerkuliahan() {
		Perkuliahan p = this.getPerkuliahan_paralel() == null ? this : this.getPerkuliahan_paralel();
		Long perkuliahanId = p == null ? null : p.getId();
		int hash = perkuliahanId == null ? System.identityHashCode(p) : perkuliahanId.hashCode();
		return DETAIL_PERKULIAHAN_LOCKS[(hash & 0x7fffffff) % DETAIL_PERKULIAHAN_LOCKS.length];
	}

	private JSONObject ambilJsonDetailPerkuliahanAman() {
		String data = ambilLokasiDetailPerkuliahan();
		try {
			return new JSONObject(data == null || data.trim().length() == 0 ? VOMahasiswa.dataJSON : data);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"Perkuliahan.ambilJsonDetailPerkuliahanAman: JSON peserta rusak, dibangun ulang bertahap");
			return new JSONObject();
		}
	}

	public void tulisLokasiDetailPerkuliahan(String data) {
		Perkuliahan p = this.getPerkuliahan_paralel() == null ? this : this.getPerkuliahan_paralel();
		File file = Common.getFileLocation(p, "detail_perkuliahan_" + p.getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2129");
			// TODO Auto-generated catch block

		}
	}

	public void removeDetailperkuliahan(Serializable id) {
		if (id == null) {
			return;
		}
		synchronized (ambilLockDetailPerkuliahan()) {
			try {
				JSONObject c = ambilJsonDetailPerkuliahanAman();
				c.put(id.toString(), "");
				tulisLokasiDetailPerkuliahan(c.toString());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "Perkuliahan.removeDetailperkuliahan");
			}
		}
	}

	public void populateDetailperkuliahan(Detailperkuliahan detailperkuliahan) {
		if (detailperkuliahan == null || detailperkuliahan.getId() == null
				|| detailperkuliahan.getIkutiPerkuliahan() != null) {
			return;
		}
		synchronized (ambilLockDetailPerkuliahan()) {
			try {
				JSONObject c = ambilJsonDetailPerkuliahanAman();
				c.put(detailperkuliahan.getId().toString(), detailperkuliahan.write().getAbsolutePath());
				tulisLokasiDetailPerkuliahan(c.toString());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "Perkuliahan.populateDetailperkuliahan");
			}
		}
	}

	public String ambilLokasiMahasiswaJadiAsisten() {
		Perkuliahan p = this;
		File file = Common.getFileLocation(p, "MahasiswaJadiAsisten_" + p.getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2165");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiMahasiswaJadiAsisten(String data) {
		Perkuliahan p = this;
		File file = Common.getFileLocation(p, "MahasiswaJadiAsisten_" + p.getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2175");
			// TODO Auto-generated catch block

		}
	}

	public void removeMahasiswaJadiAsisten(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiMahasiswaJadiAsisten());
			c.put(id.toString(), "");
			tulisLokasiMahasiswaJadiAsisten(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2186");

		}
	}

	public void populateMahasiswaJadiAsisten(Long mahasiswaJadiAsistenid) {
		try {

			JSONObject c = new JSONObject(ambilLokasiMahasiswaJadiAsisten());
			c.put(mahasiswaJadiAsistenid.toString(), mahasiswaJadiAsistenid.toString());
			tulisLokasiMahasiswaJadiAsisten(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2197");
		}
	}

	public Long ambilDetailperkuliahan(Mahasiswa mahasiswa) {

		if (mahasiswa == null || mahasiswa.getId() == null || getId() == null) {
			return null;
		}

		for (Long detailperkuliahanid : mahasiswa.ambilDetailperkuliahan()) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null
						&& detailperkuliahan.getPerkuliahan().getId() != null
						&& detailperkuliahan.getPerkuliahan().getId().equals(getId())) {
					return detailperkuliahan.getId();
				}
			}
		}
		return null;
	}

	public int ambilJumlahDetailperkuliahan() {
		Collection<Long> detailperkuliahans = ambilDetailperkuliahan();
		int size = detailperkuliahans.size();
		detailperkuliahans = null;
		return size;
	}

	public Integer[] ambilStatusPenilaian() {
		Collection<Long> detailperkuliahans = ambilDetailperkuliahan();
		Integer countBelumDinilaiTemp = 0;
		Integer countSudahDinilaiTemp = 0;

		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getTotalNilai() > 0.1) {
					countSudahDinilaiTemp++;
				} else {
					countBelumDinilaiTemp++;
				}
			}
		}

		detailperkuliahans = null;
		return new Integer[] { countBelumDinilaiTemp, countSudahDinilaiTemp };
	}

	public Integer[] ambilStatusKrs() {
		Collection<Long> detailperkuliahans = ambilDetailperkuliahan();
		Integer countBelumDisetujui = 0;
		Integer countSudahDisetujui = 0;
		Integer countDinilai = 0;
		Integer countBelumDinilai = 0;

		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
					if (detailperkuliahan.getTotalNilai() > 0.1 || detailperkuliahan.getTotalNilaiSementara() > 0.1) {
						countDinilai++;
					} else {
						countBelumDinilai++;
					}
					countSudahDisetujui++;
				} else {
					countBelumDisetujui++;
				}
			}
		}

		detailperkuliahans = null;
		return new Integer[] { countBelumDisetujui, countSudahDisetujui, countDinilai, countBelumDinilai };
	}

	@SuppressWarnings("unchecked")
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		int jumlah = 0;
		try {
			JSONObject c = new JSONObject(ambilLokasiDetailPerkuliahan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						jumlah++;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2290");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2294");

		}

		return jumlah;
	}

	@SuppressWarnings("unchecked")
	public List<Mahasiswa> ambilMahasiswa() {

		if (getPerkuliahan_paralel() != null) {
			return getPerkuliahan_paralel().ambilMahasiswa();
		}

		if (!udah("detailperkulaiahan")) {
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				reInitDetailperkuliahan(session);
			} finally {
				if (session != null && session.isOpen()) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

		List<Mahasiswa> detailperkuliahansTemp = new ArrayList<Mahasiswa>();
		List<Long> idsBelumAda = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiDetailPerkuliahan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(Detailperkuliahan.class, key);
						if (generalValueObject != null) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) generalValueObject;
							if (detailperkuliahan.getMahasiswa() != null && (detailperkuliahan.getPerkuliahan() != null
									|| detailperkuliahan.getMatakuliahKonversi() != null)) {
								detailperkuliahansTemp.add(detailperkuliahan.getMahasiswa());
							}
						} else {
							idsBelumAda.add(Long.parseLong(key));
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2344");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2348");

		}

		if (!idsBelumAda.isEmpty()) {
			// System.out.println("idsBelumAda Detailperkuliahan -> " + idsBelumAda);
			Session session = null;
			List<Detailperkuliahan> detailperkuliahansData = null;
			try {
				session = HibernateUtil.openSession();
				detailperkuliahansData = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.in("id", idsBelumAda)).list();
				for (Detailperkuliahan detailperkuliahan : detailperkuliahansData) {
					masukkanData(Detailperkuliahan.class, detailperkuliahan);
					if (detailperkuliahan.getMahasiswa() != null && (detailperkuliahan.getPerkuliahan() != null
							|| detailperkuliahan.getMatakuliahKonversi() != null)) {
						detailperkuliahansTemp.add(detailperkuliahan.getMahasiswa());
					}
				}
			} finally {
				if (session != null && session.isOpen()) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
			detailperkuliahansData = null;
		}

		try {
			Collections.sort(detailperkuliahansTemp);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2379");
			// TODO: handle exception
		}
		return detailperkuliahansTemp;
	}

	/**
	 * Memeriksa peserta perkuliahan langsung ke tabel detailperkuliahan.
	 *
	 * Jalur ini sengaja tidak membaca flag/file cache peserta karena dipakai pada
	 * validasi transaksional seperti Absen Online. KRS yang baru disetujui harus
	 * langsung dikenali tanpa menunggu tombol sinkronisasi atau refresh cache.
	 */
	public boolean apakahMahasiswaPesertaDisetujuiLangsung(Mahasiswa mahasiswa) {
		if (mahasiswa == null || mahasiswa.getId() == null || getId() == null) {
			return false;
		}

		Perkuliahan perkuliahanPeserta = getPerkuliahan_paralel() == null ? this : getPerkuliahan_paralel();
		if (perkuliahanPeserta.getId() == null) {
			return false;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Number jumlah = (Number) session.createQuery("select count(dp.id) from Detailperkuliahan dp "
					+ "where dp.perkuliahan.id = :perkuliahanId and dp.mahasiswa.id = :mahasiswaId "
					+ "and dp.persetujuan = :persetujuan")
					.setLong("perkuliahanId", perkuliahanPeserta.getId().longValue())
					.setLong("mahasiswaId", mahasiswa.getId().longValue())
					.setInteger("persetujuan", Detailperkuliahan.DISETUJUI.intValue()).uniqueResult();
			return jumlah != null && jumlah.longValue() > 0L;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		} finally {
			HibernateUtil.closeOpenedSessionQuietly(session);
		}
	}

	@SuppressWarnings("unchecked")
	public Collection<Long> ambilMahasiswaId(boolean refresh) {

		if (getPerkuliahan_paralel() != null) {
			return getPerkuliahan_paralel().ambilMahasiswaId(refresh);
		}

		if (!udah("detailperkulaiahan") || refresh) {
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				reInitDetailperkuliahan(session);
			} finally {
				if (session != null && session.isOpen()) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

		Set<Long> detailperkuliahansTemp = new HashSet<Long>();
		List<Long> idsBelumAda = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiDetailPerkuliahan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(Detailperkuliahan.class, key);
						if (generalValueObject != null) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) generalValueObject;
							if (detailperkuliahan.getMahasiswa() != null && (detailperkuliahan.getPerkuliahan() != null
									|| detailperkuliahan.getMatakuliahKonversi() != null)) {
								detailperkuliahansTemp.add(detailperkuliahan.getMahasiswa().getId());
							}
						} else {
							idsBelumAda.add(Long.parseLong(key));
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2428");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2432");

		}

		if (!idsBelumAda.isEmpty()) {
			// System.out.println("idsBelumAda Detailperkuliahan -> " + idsBelumAda);
			/* FlushMode.MANUAL: query ini bisa terpanggil dari getter property
			 * Hibernate (mis. Pertemuan.getMahasiswas) saat flush; tanpa ini,
			 * list() memicu autoFlush -> flush memanggil getter lagi ->
			 * rekursi tak berujung (StackOverflow). */
			Session session = null;
			List<Detailperkuliahan> detailperkuliahans = null;
			try {
				session = HibernateUtil.openSession();
				detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.in("id", idsBelumAda)).setFlushMode(FlushMode.MANUAL).list();
			} finally {
				if (session != null && session.isOpen()) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
			if (detailperkuliahans != null) {
				for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
					masukkanData(Detailperkuliahan.class, detailperkuliahan);
					if (detailperkuliahan.getMahasiswa() != null && (detailperkuliahan.getPerkuliahan() != null
							|| detailperkuliahan.getMatakuliahKonversi() != null)) {
						detailperkuliahansTemp.add(detailperkuliahan.getMahasiswa().getId());
					}
				}
				detailperkuliahans = null;
			}
		}

		return detailperkuliahansTemp;
	}

	public Collection<Long> ambilDetailperkuliahanDisetujui() {
		Collection<Long> detailperkuliahansBaru = new ArrayList<Long>();
		Collection<Long> detailperkuliahans = ambilDetailperkuliahan();
		for (Long detailperkuliahanid : detailperkuliahans) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
					detailperkuliahansBaru.add(detailperkuliahan.getId());
				}
			}
		}
		detailperkuliahans = null;
		return detailperkuliahansBaru;
	}

	public Collection<Long> ambilDetailperkuliahan(boolean refresh, boolean simpanJmlMhs) {
		return ambilDetailperkuliahan(null, null, null, false, refresh, simpanJmlMhs);
	}

	public Collection<Long> ambilDetailperkuliahan() {
		return ambilDetailperkuliahan(null, null, null, false, false);
	}

	public Collection<Long> ambilDetailperkuliahan(String nim, String nama, String hanyaNama) {
		return ambilDetailperkuliahan(nim, nama, hanyaNama, false, false);
	}

	public Collection<Long> ambilDetailperkuliahan(String nim, String nama, String hanyaNama,
			boolean urutkanBerdasarkanNama, boolean refresh) {
		return ambilDetailperkuliahan(nim, nama, hanyaNama, urutkanBerdasarkanNama, refresh, false);
	}

	@SuppressWarnings("unchecked")
	public Collection<Long> ambilDetailperkuliahan(String nim, String nama, String hanyaNama,
			boolean urutkanBerdasarkanNama, boolean refresh, boolean simpanJmlMhs) {

		if (getPerkuliahan_paralel() != null) {
			try {
				return getPerkuliahan_paralel().ambilDetailperkuliahan(nim, nama, hanyaNama, urutkanBerdasarkanNama,
						refresh);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2511");
				// TODO: handle exception
			}
		}

		if (!udah("detailperkulaiahan") || refresh) {
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				reInitDetailperkuliahan(session);
			} finally {
				if (session != null && session.isOpen()) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

		List<Long> detailperkuliahansTemp = new ArrayList<Long>();
		List<Long> idsBelumAda = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiDetailPerkuliahan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(Detailperkuliahan.class, key);
						if (generalValueObject != null) {
							Detailperkuliahan detailperkuliahan = (Detailperkuliahan) generalValueObject;
							if (detailperkuliahan.getPerkuliahan() != null
									|| detailperkuliahan.getMatakuliahKonversi() != null) {
								detailperkuliahan.setPerkuliahan(this);
								detailperkuliahansTemp.add(detailperkuliahan.getId());
							}
						} else {

							idsBelumAda.add(Long.parseLong(key));

						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2555");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2559");

		}

		if (!idsBelumAda.isEmpty()) {
			// System.out.println("idsBelumAda Detailperkuliahan -> " + idsBelumAda);
			/* FlushMode.MANUAL: query ini bisa terpanggil dari getter property
			 * Hibernate (mis. Pertemuan.getMahasiswas) saat flush; tanpa ini,
			 * list() memicu autoFlush -> flush memanggil getter lagi ->
			 * rekursi tak berujung (StackOverflow). */
			Session session = null;
			List<Detailperkuliahan> detailperkuliahans = null;
			try {
				session = HibernateUtil.openSession();
				detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
						.add(Restrictions.in("id", idsBelumAda)).setFlushMode(FlushMode.MANUAL).list();
				for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
					masukkanData(Detailperkuliahan.class, detailperkuliahan);
					if (detailperkuliahan.getPerkuliahan() != null || detailperkuliahan.getMatakuliahKonversi() != null) {
						detailperkuliahan.setPerkuliahan(this);
						detailperkuliahansTemp.add(detailperkuliahan.getId());
					}
				}
			} finally {
				if (session != null && session.isOpen()) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
			detailperkuliahans = null;
		}

		TreeMap<Serializable, Long> maps = new TreeMap<Serializable, Long>();

		for (Long detailperkuliahanid : detailperkuliahansTemp) {
			try {
				if (detailperkuliahanid == null) {
					continue;
				}
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan != null && detailperkuliahan.getMahasiswa() != null
							&& detailperkuliahan.getMahasiswa().getId() != null
							&& detailperkuliahan.getMahasiswa().getNim() != null) {

						if (hanyaNama != null && !hanyaNama.trim().isEmpty()) {
							if ((detailperkuliahan.getMahasiswa().getNim() != null && detailperkuliahan.getMahasiswa()
									.getNim().toLowerCase().contains(hanyaNama.toLowerCase()))
									|| (detailperkuliahan.getMahasiswa().getNama() != null
											&& detailperkuliahan.getMahasiswa().getNama().toLowerCase()
													.contains(hanyaNama.toLowerCase()))) {

								if (urutkanBerdasarkanNama) {
									maps.put(
											detailperkuliahan.getMahasiswa().getNama()
													+ detailperkuliahan.getMahasiswa().getNim(),
											detailperkuliahan.getId());
								} else {
									maps.put(detailperkuliahan.getMahasiswa().getNim(), detailperkuliahan.getId());
								}
							}
						}

						else if ((nim == null || nim.trim().isEmpty()
								|| (detailperkuliahan.getMahasiswa().getNim() != null && detailperkuliahan
										.getMahasiswa().getNim().toLowerCase().contains(nim.toLowerCase())))

								&& (nama == null || nama.trim().isEmpty()
										|| (detailperkuliahan.getMahasiswa().getNama() != null
												&& detailperkuliahan.getMahasiswa().getNama().toLowerCase()
														.contains(nama.toLowerCase())))) {

							if (urutkanBerdasarkanNama) {
								maps.put(detailperkuliahan.getMahasiswa().getNama()
										+ detailperkuliahan.getMahasiswa().getNim(), detailperkuliahan.getId());
							} else {
								maps.put(detailperkuliahan.getMahasiswa().getNim(), detailperkuliahan.getId());
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:2640");
			}
		}

		detailperkuliahansTemp = null;

		int size = maps.size();
		if (simpanJmlMhs) {
			reInitJumlahMhs(size);
		} else if ((size == 0 && getJumlahMahasiswa() > 0)) {
			return ambilDetailperkuliahan(nim, nama, hanyaNama, urutkanBerdasarkanNama, true, true);
		}

		return maps.values();
	}

	public void reInitJumlahMhs(int size) {
		Session session = null;
		try {
			if (getId() == null) {
				return;
			}
			session = HibernateUtil.openSession();
			// Jangan refresh objek detached secara langsung. Cache lama masih dapat
			// memuat id perkuliahan yang sudah dihapus; refresh() atas id tersebut
			// melempar UnresolvableObjectException. Ambil ulang baris dari DB dan
			// hentikan normal bila baris memang sudah tidak ada.
			Perkuliahan perkuliahan = (Perkuliahan) session.get(Perkuliahan.class, getId());
			if (perkuliahan == null) {
				ais.common.EntityIdentityMap.evict(Perkuliahan.class, getId());
				return;
			}
			perkuliahan.setJumlahMahasiswa(size);
			session.getTransaction().begin();
			Common.refreshUpdate(session, perkuliahan);
			session.getTransaction().commit();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2667");
			// TODO: handle exception
		} finally {
			if (session != null && session.isOpen()) {
				session.clear();
				session.disconnect();
				session.close();
			}
		}
	}

	public void reInitDetailperkuliahan(Collection<Detailperkuliahan> detailperkuliahans) {
		tulisLokasiDetailPerkuliahan(new JSONObject().toString());
		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
			populateDetailperkuliahan(detailperkuliahan);
		}
	}

	public boolean merupakanAsistenAbsen(Mahasiswa mahasiswa) {
		if (mahasiswa == null || mahasiswa.getId() == null) {
			return false;
		}
		List<MahasiswaJadiAsisten> mahasiswaJadiAsistensTemp = ambilMahasiswaJadiAsisten();
		boolean ada = false;
		for (MahasiswaJadiAsisten mahasiswaJadiAsisten : mahasiswaJadiAsistensTemp) {
			Mahasiswa mhs = mahasiswaJadiAsisten == null || !mahasiswaJadiAsisten.getAktif() ? null
					: mahasiswaJadiAsisten.getMahasiswa();
			if (mhs != null && mhs.getId() != null && mahasiswa.getId().equals(mhs.getId())) {
				ada = mahasiswaJadiAsisten.getInputAbsen();
				break;
			}
		}
		mahasiswaJadiAsistensTemp = null;
		return ada;
	}

	public boolean merupakanAsistenNilai(Mahasiswa mahasiswa) {
		if (mahasiswa == null || mahasiswa.getId() == null) {
			return false;
		}
		List<MahasiswaJadiAsisten> mahasiswaJadiAsistensTemp = ambilMahasiswaJadiAsisten();
		boolean ada = false;
		for (MahasiswaJadiAsisten mahasiswaJadiAsisten : mahasiswaJadiAsistensTemp) {
			Mahasiswa mhs = mahasiswaJadiAsisten == null || !mahasiswaJadiAsisten.getAktif() ? null
					: mahasiswaJadiAsisten.getMahasiswa();
			if (mhs != null && mhs.getId() != null && mahasiswa.getId().equals(mhs.getId())) {
				ada = mahasiswaJadiAsisten.getInputNilai();
				break;
			}
		}
		mahasiswaJadiAsistensTemp = null;
		return ada;
	}

	public boolean merupakanAsisten(Mahasiswa mahasiswa) {
		if (mahasiswa == null || mahasiswa.getId() == null) {
			return false;
		}
		List<Mahasiswa> mahasiswas = ambilAsisten();
		boolean ada = false;
		for (Mahasiswa mhs : mahasiswas) {
			if (mhs != null && mhs.getId() != null && mahasiswa.getId().equals(mhs.getId())) {
				ada = true;
				break;
			}
		}
		mahasiswas = null;
		return ada;
	}

	public List<Mahasiswa> ambilAsisten() {
		List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
		List<MahasiswaJadiAsisten> mahasiswaJadiAsistensTemp = ambilMahasiswaJadiAsisten();
		for (MahasiswaJadiAsisten mahasiswaJadiAsisten : mahasiswaJadiAsistensTemp) {
			if (mahasiswaJadiAsisten.getAktif()) {
				mahasiswas.add(mahasiswaJadiAsisten.getMahasiswa());
			}
		}
		mahasiswaJadiAsistensTemp = null;
		return mahasiswas;
	}

	@SuppressWarnings("unchecked")
	public List<MahasiswaJadiAsisten> ambilMahasiswaJadiAsisten() {

		if (!udah("mahasiswaJadiAsisten")) {
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				reInitMahasiswaJadiAsisten(session);
			} finally {
				if (session != null && session.isOpen()) {
					session.clear();
					session.disconnect();
					session.close();
				}
			}
		}

		List<String> keysdata = new ArrayList<String>();
		try {
			JSONObject c = new JSONObject(ambilLokasiMahasiswaJadiAsisten());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						keysdata.add(key);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2777");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2781");

		}

		List<MahasiswaJadiAsisten> mahasiswaJadiAsistensTemp = ambilDataBanyak(MahasiswaJadiAsisten.class, keysdata);

		return mahasiswaJadiAsistensTemp;
	}

	@SuppressWarnings("unchecked")
	public void reInitMahasiswaJadiAsisten(Session session) {
		Collection<Long> mahasiswaJadiAsistens = session.createCriteria(MahasiswaJadiAsisten.class)
				.add(Restrictions.eq("perkuliahan", this)).setProjection(Projections.property("id")).list();
		reInitMahasiswaJadiAsisten(mahasiswaJadiAsistens);
	}

	public void reInitMahasiswaJadiAsisten(Collection<Long> mahasiswaJadiAsistens) {
		tulisLokasiMahasiswaJadiAsisten(new JSONObject().toString());
		for (Long mahasiswaJadiAsisten : mahasiswaJadiAsistens) {
			populateMahasiswaJadiAsisten(mahasiswaJadiAsisten);
		}
	}

	public void hapusPerkuliahanDosen() {
		List<Dosen> dosens = populateDosenBuNama();
		for (Dosen dosen : dosens) {
			dosen.removePerkuliahan(id);
		}
		dosens = null;
	}

	public void reInitPerkuliahanDosen() {
		List<Dosen> dosens = populateDosenBuNama();
		for (Dosen dosen : dosens) {
			dosen.populatePerkuliahan(this, true);
		}
		dosens = null;
	}

	@SuppressWarnings("unchecked")
	public void reInitDetailperkuliahan(Session session) {
		/* FlushMode.MANUAL: cegah autoFlush saat dipanggil dari getter
		 * property Hibernate (lihat catatan di ambilMahasiswaId). */
		List<Long> detailperkuliahansid = session.createCriteria(Detailperkuliahan.class)
				.setProjection(Projections.property("id")).add(Restrictions.isNull("ikutiPerkuliahan"))
				.add(Restrictions.eq("perkuliahan", this)).addOrder(Order.asc("id"))
				.setFlushMode(FlushMode.MANUAL).list();
		List<Long> idsBelumAda = new ArrayList<Long>();
		tulisLokasiDetailPerkuliahan(new JSONObject().toString());
		for (Long detailperkuliahanid : detailperkuliahansid) {

			if (detailperkuliahanid != null) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());

				if (detailperkuliahan != null) {
					masukkanData(Detailperkuliahan.class, detailperkuliahan);
					populateDetailperkuliahan(detailperkuliahan);
				} else {
					idsBelumAda.add(detailperkuliahanid);
				}

			}

		}
		detailperkuliahansid.clear();
		detailperkuliahansid = null;

		if (!idsBelumAda.isEmpty()) {
			// System.out.println("idsBelumAda Detailperkuliahan -> " + idsBelumAda);
			/* FlushMode.MANUAL: query ini bisa terpanggil dari getter property
			 * Hibernate (mis. Pertemuan.getMahasiswas) saat flush; tanpa ini,
			 * list() memicu autoFlush -> flush memanggil getter lagi ->
			 * rekursi tak berujung (StackOverflow). */
			List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.in("id", idsBelumAda)).setFlushMode(FlushMode.MANUAL).list();
			for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
				masukkanData(Detailperkuliahan.class, detailperkuliahan);
				populateDetailperkuliahan(detailperkuliahan);
			}
			detailperkuliahans = null;
		}
	}

	public void setStatus_penilaian(Integer status_penilaian) {
		this.status_penilaian = status_penilaian;
	}

	@Column(name = "status_penilaian", nullable = true)
	public Integer getStatus_penilaian() {
		return status_penilaian == null ? BELUM_ADA_MAHASISWA : status_penilaian;
	}

	public String ambilLokasiFormatNilai() {
		File file = Common.getFileLocation(this, "perkuliahan_punya_format_nilai_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2880");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiFormatNilai(String data) {
		File file = Common.getFileLocation(this, "perkuliahan_punya_format_nilai_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:2890");
		}
	}

	public void removeFormatNilai(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiFormatNilai());
			c.put(id.toString(), "");
			tulisLokasiFormatNilai(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2899");

		}
	}

	public void populateFormatNilai(FormatNilai formatNilai, boolean tulisUlang) {
		try {
			if (formatNilai == null) {
				return;
			}
			JSONObject c = new JSONObject(ambilLokasiFormatNilai());
			c.put(formatNilai.getId().toString(), formatNilai.write().getAbsolutePath());
			tulisLokasiFormatNilai(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2912");
		}
	}

	public List<FormatNilai> ambilFormatNilai(Session session) {
		return ambilFormatNilai(session, false);
	}

	public List<FormatNilai> ambilFormatNilai(Session session, boolean refresh) {
		return ambilFormatNilai(session, refresh, true);
	}

	public List<FormatNilai> formatNilaisa = null;

	@SuppressWarnings("unchecked")
	public List<FormatNilai> ambilFormatNilai(Session session, boolean refresh, boolean coba) {

		if (getPerkuliahan_paralel() != null) {
			formatNilaisa = getPerkuliahan_paralel().ambilFormatNilai(session, refresh, coba);
			return formatNilaisa;
		}

		if (session != null && (!udah("format_nilai_baru") || refresh)) {
			try {
				formatNilaisa = PembombotanNilai.setDefaultPembobotan(this, session, true);
				return formatNilaisa;
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:2939");
			}
		}

		Double persen = 0.0;

		// PERBAIKAN 1: Gunakan variabel LOKAL untuk memproses data agar terhindar dari
		// tabrakan thread.
		List<FormatNilai> localFormatNilai = new ArrayList<FormatNilai>();

		try {
			JSONObject c = new JSONObject(ambilLokasiFormatNilai());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						FormatNilai formatNilai = (FormatNilai) ambilData(FormatNilai.class, key);
						if (formatNilai != null) {
							if (formatNilai.getPersen() > 0.01 && formatNilai.getStatusPertemuan() != null
									&& formatNilai.getStatusPertemuan().getAktif()) {
								formatNilai.setPerkuliahan(this);
								localFormatNilai.add(formatNilai); // Gunakan variabel lokal
								persen += formatNilai.getPersen();
							}
						} else {
							File file = new File(s);
							if (file != null && file.exists()) {
								formatNilai = (FormatNilai) Common.convertToObject(
										new JSONObject(ais.common.BacaTulisUtil.baca(file)), FormatNilai.class);
								masukkanData(FormatNilai.class, formatNilai);
								if (formatNilai != null && formatNilai.getPersen() > 0.01
										&& formatNilai.getStatusPertemuan() != null
										&& formatNilai.getStatusPertemuan().getAktif()) {
									formatNilai.setPerkuliahan(this);
									localFormatNilai.add(formatNilai); // Gunakan variabel lokal
									persen += formatNilai.getPersen();
								}
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2982");
					// e.printStackTrace(); // Disarankan di-log agar jika ada JSON error tidak
					// silent
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:2987");
			// e.printStackTrace();
		}

		if (persen < 99.0 && coba) {
			return ambilFormatNilai(session, true, false);
		}

		// PERBAIKAN 2: Lakukan sort pada variabel LOKAL. Ini 100% aman dari thread
		// lain.
		Collections.sort(localFormatNilai);

		int i = 1;
		for (FormatNilai formatNilai : localFormatNilai) {
			if (formatNilai.getNomorUrut() == null) {
				formatNilai.setNomorUrut(i);
				Common.refreshUpdate(formatNilai);
			}
			i++;
		}

		// PERBAIKAN 3: Setelah list matang dan di-sort, baru assign ke variabel
		// instance.
		this.formatNilaisa = localFormatNilai;

		return localFormatNilai;
	}

	public String ambilLokasiParalel() {
		File file = Common.getFileLocation(this, "paralel_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:3021");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiParalel(String data) {
		File file = Common.getFileLocation(this, "paralel_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:3030");
			// TODO Auto-generated catch block

		}
	}

	public void removeParalel(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiParalel());
			c.put(id.toString(), "");
			tulisLokasiParalel(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:3041");

		}
	}

	public void populateParalel(Perkuliahan perkuliahan) {
		try {
			if (perkuliahan == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiParalel());
			c.put(perkuliahan.getId().toString(), perkuliahan.write().getAbsolutePath());
			tulisLokasiParalel(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:3055");
		}
	}

	@SuppressWarnings("unchecked")
	public void reInitParalel(Session session) {
		List<Perkuliahan> jadwalParalels;
		if (getPerkuliahan_paralel() != null) {
			jadwalParalels = session.createCriteria(Perkuliahan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.ne("id", getId()))
					.add(Restrictions.eq("perkuliahan_paralel", getPerkuliahan_paralel())).list();
			jadwalParalels.add(getPerkuliahan_paralel());
		} else {
			jadwalParalels = session.createCriteria(Perkuliahan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("perkuliahan_paralel", this)).list();
		}
		tulisLokasiParalel(new JSONObject().toString());
		for (Perkuliahan paralel : jadwalParalels) {
			populateParalel(paralel);
		}
		jadwalParalels = null;
	}

	public List<Perkuliahan> ambilParalelPerkuliahan() {
		List<Perkuliahan> perkuliahans = new ArrayList<Perkuliahan>();
		for (Long idperkuliahan : ambilParalel(null)) {
			Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(), idperkuliahan);
			if (perkuliahan != null) {
				perkuliahans.add(perkuliahan);
			}
		}
		return perkuliahans;
	}

	public List<Long> ambilParalel() {
		return ambilParalel(null);
	}

	@SuppressWarnings("unchecked")
	public List<Long> ambilParalel(Dosen dosen) {
		if (!udah("paralel")) {
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				reInitParalel(session);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:3103");
			} finally {
				if (session != null && session.isOpen()) {
					session.clear();
					try {
						session.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:3109");
						// ignore
					}
					try {
						session.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:3114");
						// ignore
					}
				}
			}
		}
		List<Long> paralelsTemp = new ArrayList<Long>();
		try {
			JSONObject c = new JSONObject(ambilLokasiParalel());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						Perkuliahan paralel = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
								Long.parseLong(key));
						if (paralel != null) {
							paralel.flagParalel = true;
							if (dosen != null && paralel.ada(dosen)) {
								paralelsTemp.add(paralel.getId());
							} else {
								paralelsTemp.add(paralel.getId());
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:3140");
//					e.printStackTrace();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Perkuliahan.java:3144");
//			e.printStackTrace();
		}
		return paralelsTemp;
	}

	private String course;
	private Boolean urutkanotomatis;

	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {

		course = filterTidakBoleh(course);

		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelasref", nullable = true)
	public Kelas getKelasref() {
		kelasref = check(kelasref);
		return kelasref;
	}

	public void setKelasref(Kelas kelasref) {
		this.kelasref = kelasref;
	}

	public Boolean getDosenBolehVerifikasiNilaiSendiri() {
		if (dosenBolehVerifikasiNilaiSendiri == null) {
			dosenBolehVerifikasiNilaiSendiri = Common
					.getKonfigurasi("dosenBolehVerifikasiNilaiSendiri", Konfigurasi.AKTIF).getNilai()
					.equals(Konfigurasi.TIDAK_AKTIF);
		}
		return dosenBolehVerifikasiNilaiSendiri == null ? false : dosenBolehVerifikasiNilaiSendiri;
	}

	public void setDosenBolehVerifikasiNilaiSendiri(Boolean dosenBolehVerifikasiNilaiSendiri) {
		this.dosenBolehVerifikasiNilaiSendiri = dosenBolehVerifikasiNilaiSendiri;
	}

	public Boolean getWaktuPerkuliahanOnlineBebas() {
		return waktuPerkuliahanOnlineBebas == null ? false : waktuPerkuliahanOnlineBebas;
	}

	public void setWaktuPerkuliahanOnlineBebas(Boolean waktuPerkuliahanOnlineBebas) {
		this.waktuPerkuliahanOnlineBebas = waktuPerkuliahanOnlineBebas;
	}

	@Column(name = "nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir")
	public Boolean getNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir() {
		if (nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir == null) {
			nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir = Common.bolehKonfigurasi("nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir");
		}
		return nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir;
	}

	public void setNilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir(
			Boolean nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir) {
		this.nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir = nilai_0_tidak_masuk_dalam_perhitungan_nilai_akhir;
	}

	public Boolean getJikaAdaNilai0TidakMenghitungNilaiAkhir() {
		if (jikaAdaNilai0TidakMenghitungNilaiAkhir == null) {
			jikaAdaNilai0TidakMenghitungNilaiAkhir = Common.bolehKonfigurasi("jika_ada_nilai_0_tidak_menghitung_nilai_akhir", Konfigurasi.TIDAK_AKTIF);
		}
		return jikaAdaNilai0TidakMenghitungNilaiAkhir;
	}

	public void setJikaAdaNilai0TidakMenghitungNilaiAkhir(Boolean jikaAdaNilai0TidakMenghitungNilaiAkhir) {
		this.jikaAdaNilai0TidakMenghitungNilaiAkhir = jikaAdaNilai0TidakMenghitungNilaiAkhir;
	}

	public Boolean getHanyaInputNilaiHuruf() {
		return hanyaInputNilaiHuruf == null ? false : hanyaInputNilaiHuruf;
	}

	public void setHanyaInputNilaiHuruf(Boolean hanyaInputNilaiHuruf) {
		this.hanyaInputNilaiHuruf = hanyaInputNilaiHuruf;
	}

	public Boolean getDosenBolehAbsenMenggunakanFoto() {
		return dosenBolehAbsenMenggunakanFoto == null ? true : dosenBolehAbsenMenggunakanFoto;
	}

	public void setDosenBolehAbsenMenggunakanFoto(Boolean dosenBolehAbsenMenggunakanFoto) {
		this.dosenBolehAbsenMenggunakanFoto = dosenBolehAbsenMenggunakanFoto;
	}

	public Boolean getMahasiswaBolehAbsenMenggunakanFoto() {
		return mahasiswaBolehAbsenMenggunakanFoto == null ? true : mahasiswaBolehAbsenMenggunakanFoto;
	}

	public void setMahasiswaBolehAbsenMenggunakanFoto(Boolean mahasiswaBolehAbsenMenggunakanFoto) {
		this.mahasiswaBolehAbsenMenggunakanFoto = mahasiswaBolehAbsenMenggunakanFoto;
	}

	public Integer getBolehAbsenSebelumWaktuMulaiDalamMenit() {
		return bolehAbsenSebelumWaktuMulaiDalamMenit == null ? 30 : bolehAbsenSebelumWaktuMulaiDalamMenit;
	}

	public void setBolehAbsenSebelumWaktuMulaiDalamMenit(Integer mahasiswaBolehAbsenSebelumWaktuMulaiDalamMenit) {
		this.bolehAbsenSebelumWaktuMulaiDalamMenit = mahasiswaBolehAbsenSebelumWaktuMulaiDalamMenit;
	}

	public Integer getBolehAbsenSetelahWaktuMulaiDalamMenit() {
		return bolehAbsenSetelahWaktuMulaiDalamMenit == null ? 30 : bolehAbsenSetelahWaktuMulaiDalamMenit;
	}

	public void setBolehAbsenSetelahWaktuMulaiDalamMenit(Integer bolehAbsenSetelahWaktuMulaiDalamMenit) {
		this.bolehAbsenSetelahWaktuMulaiDalamMenit = bolehAbsenSetelahWaktuMulaiDalamMenit;
	}

	public Boolean getBolehAbsenWaktuIkutiPerkuliahan() {
		return bolehAbsenWaktuIkutiPerkuliahan == null ? false : bolehAbsenWaktuIkutiPerkuliahan;
	}

	public void setBolehAbsenWaktuIkutiPerkuliahan(Boolean bolehAbsenWaktuIkutiPerkuliahan) {
		this.bolehAbsenWaktuIkutiPerkuliahan = bolehAbsenWaktuIkutiPerkuliahan;
	}

	public Double getPersenKehadiranDinilai0() {
		return persenKehadiranDinilai0 == null ? 0.0 : persenKehadiranDinilai0;
	}

	public void setPersenKehadiranDinilai0(Double persenKehadiranDinilai0) {
		this.persenKehadiranDinilai0 = persenKehadiranDinilai0;
	}

	public Boolean getSembunyikanFormatPenilaian() {
		return sembunyikanFormatPenilaian == null ? false : sembunyikanFormatPenilaian;
	}

	public void setSembunyikanFormatPenilaian(Boolean sembunyikanFormatPenilaian) {
		this.sembunyikanFormatPenilaian = sembunyikanFormatPenilaian;
	}

	@Transient
	public Date getAwalPerkuliahan() {
		try {
			awalPerkuliahan = null;
			jurusan = getJurusan();
			RencanaTahunAkademik s = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(jurusan.getFakultas(),
					jurusan, null, null, null, null, program, null, getTahunAjaran(),
					getGanjilGenap().equals(Perkuliahan.SP)
							? (getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)
							: getGanjilGenap());
			if (s != null) {
				awalPerkuliahan = s.getTanggalMulaiBelajarMengajar();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:3301");
		}
		return awalPerkuliahan;
	}

	public void setAwalPerkuliahan(Date awalPerkuliahan) {
		this.awalPerkuliahan = awalPerkuliahan;
	}

	public Boolean getBolehMenentukanTanggalMulaiPerkuliahan() {
		return bolehMenentukanTanggalMulaiPerkuliahan == null ? false : bolehMenentukanTanggalMulaiPerkuliahan;
	}

	public void setBolehMenentukanTanggalMulaiPerkuliahan(Boolean bolehMenentukanTanggalMulaiPerkuliahan) {
		this.bolehMenentukanTanggalMulaiPerkuliahan = bolehMenentukanTanggalMulaiPerkuliahan;
	}

	public String getMode() {
		return mode == null ? "M" : mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getLingkup() {
		return lingkup == null ? "1" : lingkup;
	}

	public void setLingkup(String lingkup) {
		this.lingkup = lingkup;
	}

	public Integer getBatasWaktuBolehAbsenKehadiran() {
		return batasWaktuBolehAbsenKehadiran == null ? 0 : batasWaktuBolehAbsenKehadiran;
	}

	public void setBatasWaktuBolehAbsenKehadiran(Integer batasWaktuBolehAbsenKehadiran) {
		this.batasWaktuBolehAbsenKehadiran = batasWaktuBolehAbsenKehadiran;
	}

	@Override
	public Boolean getUrutkanotomatis() {
		// Pertemuan diurutkan otomatis berdasarkan TANGGAL (default true). Dulu kurikulum OBE
		// DIPAKSA false (urut pertemuanKe) sehingga tanggal kolom presensi jadi tidak berurutan
		// (mis. ke-1 = 30/03 padahal ke-2 = 23/02) DAN bertentangan dengan logika renumber
		// pertemuanKe = posisi-tanggal di AktifitasPerkuliahanHelper. Pemaksaan itu DIHAPUS:
		// urutan kini mengikuti pilihan "Urutkan manual" (checkbox) — default tanggal — untuk
		// semua jenis kurikulum, lalu pertemuanKe diselaraskan otomatis mengikuti tanggal.
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

	public Boolean getSemuaPertemuanSesuaiRps() {
		return semuaPertemuanSesuaiRps == null ? false : semuaPertemuanSesuaiRps;
	}

	public void setSemuaPertemuanSesuaiRps(Boolean semuaPertemuanSesuaiRps) {
		this.semuaPertemuanSesuaiRps = semuaPertemuanSesuaiRps;
	}

	@Column(columnDefinition = "text")
	public String getCatatanSesuaiRps() {
		return catatanSesuaiRps == null ? "" : catatanSesuaiRps.trim();
	}

	public void setCatatanSesuaiRps(String catatanSesuaiRps) {
		this.catatanSesuaiRps = catatanSesuaiRps;
	}

	public Long getSemuaNilaiSesuaiRps() {
		return semuaNilaiSesuaiRps == null ? 0L : semuaNilaiSesuaiRps;
	}

	public void setSemuaNilaiSesuaiRps(Long semuaNilaiSesuaiRps) {
		this.semuaNilaiSesuaiRps = semuaNilaiSesuaiRps;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembombotan_nilai_backup", nullable = true)
	public PembombotanNilai getPembombotanNilaiBackup() {
		try {
			if (getDikunci() == null) {
				pembombotanNilaiBackup = getPembombotanNilai();
			} else {
				pembombotanNilaiBackup = check(pembombotanNilaiBackup);

				if (pembombotanNilaiBackup == null) {
					pembombotanNilaiBackup = check(pembombotanNilai);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Perkuliahan.java:3397");
		}
		return pembombotanNilaiBackup;
	}

	public void setPembombotanNilaiBackup(PembombotanNilai pembombotanNilaiBackup) {
		this.pembombotanNilaiBackup = pembombotanNilaiBackup;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	public Integer getJumlahMahasiswa() {
		return jumlahMahasiswa == null ? 0 : jumlahMahasiswa;
	}

	public void setJumlahMahasiswa(Integer jumlahMahasiswa) {
		this.jumlahMahasiswa = jumlahMahasiswa;
	}

	public Boolean getMahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen() {
		return mahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen == null ? false
				: mahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen;
	}

	public void setMahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen(
			Boolean mahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen) {
		this.mahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen = mahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen;
	}

	// CQI Loop 1: JSON array of per-CPMK improvement records for this semester offering
	// Format: [{"cpmk":"CPMK-1","masalah":"...","analisis":"...","rencana":"...","pj":"...","targetWaktu":"...","status":"Pending"}]
	private String cqiData;

	@javax.persistence.Column(columnDefinition = "text")
	public String getCqiData() {
		return cqiData == null ? "" : cqiData;
	}

	public void setCqiData(String cqiData) {
		this.cqiData = cqiData;
	}

}
