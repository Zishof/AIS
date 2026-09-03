package ais.database.model.payroll;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

/**
 * <h1>JatahCuti — jatah cuti tahunan pegawai per tahun masa kerja</h1>
 *
 * <p>Berbeda dari kesan "master data statis" yang tersirat dari komentar hbm2java, kelas ini
 * adalah tabel <b>penimpaan (override) jatah cuti per pegawai per tahun masa kerja</b> — BUKAN per
 * tahun kalender. Kolom {@link #tahun} diisi dengan angka urutan tahun masa kerja pegawai (mis.
 * {@code 1} untuk tahun pertama bekerja, {@code 5} untuk tahun kelima), dicocokkan terhadap
 * {@link ais.database.model.Pegawai#ambilMasaKerjaTahun()} — BUKAN {@code Calendar.YEAR} berjalan.
 * Ini memungkinkan kebijakan "jatah cuti bertambah seiring lama bekerja" (mis. 12 hari di tahun
 * 1–5, 15 hari di tahun 6 ke atas) diwakili sebagai beberapa baris {@code JatahCuti} per pegawai,
 * satu baris per ambang tahun masa kerja yang berbeda jatahnya.</p>
 *
 * <p><b>Konsumen nyata:</b> {@link ais.database.model.Pegawai#ambilJatahCuti()} memindai SELURUH
 * cache {@code ConstantValues} untuk kelas ini (lihat di bawah) mencari baris yang
 * {@code pegawai}-nya sama dengan pegawai yang diminta DAN {@code tahun}-nya sama dengan tahun masa
 * kerja pegawai itu saat ini; hasilnya lalu <b>ditulis balik</b> (bukan sekadar dibaca) ke field
 * {@code Pegawai.jatahCutiTahunan} oleh {@link ais.database.model.Pegawai#getJatahCutiTahunan()} —
 * pola getter destruktif yang sama seperti relasi lazy lain di modul ini. Nilai gabungan tersebut
 * lalu dipakai {@code CutiDanIzinAction} dan {@code DashboardKehadiranExpert} untuk menghitung
 * "sisa cuti" (jatah dikurangi hari cuti yang sudah dipakai dan bertanda
 * {@link ais.database.model.payroll.CutiDanIzin#getMemotongJatahCuti()} bernilai {@code true}).
 * Bila pegawai TIDAK punya baris {@code JatahCuti} yang cocok untuk tahun masa kerjanya, jatah jatuh
 * kembali (fallback) ke kolom {@code Pegawai.jatahCutiTahunan} sendiri, atau ke aturan default
 * {@link ais.action.master.payroll.helper.AturanCutiHelper#jatahTahunanMenurutMasaKerja} (opt-in,
 * default nonaktif), atau akhirnya ke angka baku {@code 12} — {@code JatahCuti} selalu diprioritaskan
 * paling dulu bila ada barisnya.</p>
 *
 * <p><b>TIDAK ADA logika reset/carry-over otomatis di kelas ini.</b> Kelas ini murni tabel lookup
 * "berapa jatah untuk tahun masa kerja ke-N"; ia TIDAK menyimpan sisa cuti, TIDAK mendekrement
 * apa pun saat cuti diambil, dan TIDAK memindahkan sisa jatah tahun sebelumnya ke tahun berikutnya.
 * Perhitungan "sisa cuti" (jatah dikurangi realisasi) dilakukan on-the-fly di {@code CutiDanIzinAction}
 * dengan menjumlah baris {@link ais.database.model.payroll.CutiDanIzin} yang disetujui dan memotong
 * jatah, BUKAN disimpan permanen sebagai state. Jadi tidak ada carry-over sisa cuti tahun lalu ke
 * tahun berikutnya kecuali dihitung ulang dari riwayat {@code CutiDanIzin} secara historis penuh.</p>
 *
 * <p><b>Master data ter-cache penuh:</b> kelas ini terdaftar di {@code InitData} sebagai salah satu
 * {@code ConstantValues} DAN masuk daftar {@code CLASS_JANGAN_DIBERSIHKAN} di {@code DataUtil}
 * (kelas yang sengaja DIKECUALIKAN dari rutin pembersihan/reset data massal) — konsisten dengan
 * sifatnya sebagai konfigurasi kepegawaian jangka panjang yang tidak boleh ikut terhapus saat
 * pembersihan data transaksional. Seluruh baris dimuat ke memori JVM dan dipakai ulang lewat
 * {@link ais.database.model.GeneralValueObject#check(Object)} (dipanggil dari
 * {@link #getPegawai()}), bukan query database per akses.</p>
 *
 * <p><b>CRUD:</b> layar pemeliharaan manual ada di {@code ais.action.master.payroll.JatahCutiAction}
 * — admin memilih pegawai, mengisi "Tahun ke-" (masa kerja, bukan tahun kalender) dan jumlah jatah
 * cuti; kombinasi {@code (pegawai, tahun)} divalidasi unik di layar itu ({@code checkNamaJatahCuti()})
 * sebelum simpan, sekaligus dijamin unik lagi di level database lewat kolom {@link #kodeUnik}.</p>
 *
 * <p>Seperti kelas {@code GeneralValueObject} turunan hbm2java lain di modul ini, field
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} adalah <b>field audit shadow</b> — bukan bug,
 * melainkan keharusan teknis karena {@link ais.database.model.GeneralValueObject} sendiri BUKAN
 * {@code @Entity} sehingga tidak bisa mendeklarasikan kolom audit generik yang diwariskan lewat
 * pemetaan JPA; tiap entity turunan mengulang field yang sama.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "jatah_cuti")
public class JatahCuti extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** ID baris (identity/auto-increment), primary key tabel {@code payroll.jatah_cuti}. */
	private Long id;

	/**
	 * Nama pengguna yang membuat/mengubah baris ini (field audit shadow, diisi oleh lapisan
	 * aplikasi — lihat catatan kelas). Tidak dipetakan ke kolom lewat anotasi JPA eksplisit di
	 * kelas ini; nilainya disetel manual oleh pemanggil (mis. {@code Common.refreshSaveOrUpdate}).
	 */
	private String oleh;

	/** ID pengguna yang membuat/mengubah baris ini (pasangan {@link #oleh}, lihat catatan kelas). */
	private String olehId;

	/**
	 * @return ID pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah
	 * diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna pengubah. <b>Menolak diam-diam</b> nilai {@code null} atau string kosong
	 * (setelah {@code trim()}) — pemanggilan dengan nilai kosong tidak melempar exception maupun
	 * mengubah field, sehingga nilai lama tetap dipertahankan alih-alih ditimpa kosong.
	 *
	 * @param olehId ID pengguna pengubah; diabaikan bila kosong/{@code null}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai kosong/
	 * {@code null} diabaikan diam-diam agar nilai lama tidak tertimpa kosong.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong/{@code null}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah
	 * diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus-hidup JPA {@code @PreUpdate} — dipanggil otomatis oleh Hibernate tepat sebelum
	 * baris ini di-UPDATE ke database. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang mengisi ulang
	 * {@link #tanggal_dirubah} (dan field audit sejenis bila ada) dengan waktu saat ini, sehingga
	 * jejak "kapan terakhir diubah" selalu akurat tanpa perlu pemanggil menyetelnya manual di setiap
	 * titik simpan.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi tanggal/jam terakhir baris ini diubah. Biasanya tidak perlu dipanggil manual karena
	 * {@link #onUpdate()} sudah mengisinya otomatis pada setiap UPDATE; nilai awal (saat objek baru
	 * dibuat) sudah terisi waktu saat itu lewat inisialisasi field.
	 *
	 * @param tanggal_dirubah tanggal/jam perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return tanggal/jam terakhir baris ini diubah; diinisialisasi ke waktu pembuatan objek dan
	 * diperbarui otomatis oleh {@link #onUpdate()} pada tiap UPDATE
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas untuk log/debug: {@code id} digabung {@link #tahun} (tahun masa
	 * kerja, bukan tahun kalender) dengan pemisah {@code "-"} (mis. {@code "7-1"} untuk baris jatah
	 * tahun masa kerja pertama)
	 */
	public String toString() {
		return id + "-" + tahun;
	}

	/** Pegawai pemilik baris jatah cuti ini. Wajib diisi ({@code nullable = false}). */
	private Pegawai pegawai;

	/** Jumlah hari jatah cuti tahunan untuk kombinasi {@link #pegawai} + {@link #tahun} ini. */
	private Integer jatahCutiTahunan;

	/**
	 * Urutan <b>tahun masa kerja</b> (BUKAN tahun kalender) tempat jatah ini berlaku — lihat catatan
	 * kelas. Dicocokkan terhadap {@link ais.database.model.Pegawai#ambilMasaKerjaTahun()}.
	 */
	private Integer tahun;

	/**
	 * Kode unik turunan otomatis {@code "<idPegawai>_<tahun>"} — lihat {@link #getKodeUnik()}.
	 * Menegakkan keunikan kombinasi (pegawai, tahun) di level database (kolom {@code unique = true}),
	 * sebagai jaring pengaman kedua setelah validasi aplikasi di layar {@code JatahCutiAction}.
	 */
	private String kodeUnik;

	/** Konstruktor kosong wajib JPA/Hibernate. Semua field diisi lewat setter setelah instansiasi. */
	public JatahCuti() {
	}

	/**
	 * @return ID baris (primary key), atau {@code null} untuk entity yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi ID baris. Kolom bertanda {@code insertable = false} pada anotasi {@link #getId()},
	 * jadi nilai yang disetel di sini tidak ikut dikirim pada INSERT — ID sebenarnya dihasilkan
	 * server (strategi {@code IDENTITY}/auto-increment) dan hanya relevan dipakai untuk kasus
	 * seperti {@code session.load(JatahCuti.class, id)}.
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return jumlah hari jatah cuti tahunan untuk baris ini; {@code 12} (BUKAN {@code null}) bila
	 * kolom kosong, sehingga aman langsung dipakai dalam perhitungan aritmatika "sisa cuti" oleh
	 * pemanggil tanpa null-check tambahan — lihat catatan kelas soal alur
	 * {@link ais.database.model.Pegawai#getJatahCutiTahunan()}
	 */
	public Integer getJatahCutiTahunan() {
		return jatahCutiTahunan == null ? 12 : jatahCutiTahunan;
	}

	/**
	 * Mengisi jumlah hari jatah cuti tahunan.
	 *
	 * @param jatahCutiTahunan jumlah hari; {@code null} diperlakukan sebagai {@code 12} oleh
	 * {@link #getJatahCutiTahunan()}
	 */
	public void setJatahCutiTahunan(Integer jatahCutiTahunan) {
		this.jatahCutiTahunan = jatahCutiTahunan;
	}

	/**
	 * @return urutan tahun masa kerja tempat jatah ini berlaku (BUKAN tahun kalender — lihat
	 * catatan kelas); {@code 0} (BUKAN {@code null}) bila kolom kosong
	 */
	@Column(nullable = false)
	public Integer getTahun() {
		return tahun == null ? 0 : tahun;
	}

	/**
	 * Mengisi urutan tahun masa kerja.
	 *
	 * @param tahun urutan tahun masa kerja (mis. {@code 1} untuk tahun pertama bekerja); {@code null}
	 * diperlakukan sebagai {@code 0} oleh {@link #getTahun()}
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * <b>Getter destruktif</b> — memuat ulang {@link #pegawai} lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} SETIAP dipanggil dan menulis
	 * balik hasilnya ke field, bukan sekadar membaca. Ini pola baku relasi {@code @ManyToOne} lazy
	 * di modul ini: memastikan objek yang dikembalikan konsisten dengan cache
	 * {@code ConstantValues}/{@code EntityIdentityMap} JVM (satu instance Java per ID entity),
	 * bukan sisa proxy Hibernate basi dari sesi lampau.
	 *
	 * @return pegawai pemilik jatah cuti ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Mengisi pegawai pemilik baris jatah cuti ini.
	 *
	 * @param pegawai pegawai pemilik
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * <b>Getter destruktif</b> — setiap dipanggil, method ini memuat ulang {@link #pegawai} (lewat
	 * {@link #getPegawai()}) dan, bila tidak {@code null}, MENGHITUNG ULANG serta MENULIS BALIK
	 * {@link #kodeUnik} menjadi {@code "<idPegawai>_<tahun>"} — bukan sekadar mengembalikan nilai
	 * yang tersimpan. Efeknya: kode unik ini turunan otomatis dari {@link #pegawai} + {@link #tahun}
	 * dan TIDAK PERNAH boleh diisi manual secara independen (lihat {@link #setKodeUnik(String)});
	 * nilai apa pun yang disetel manual akan tertimpa pada pemanggilan {@link #getPegawai()}
	 * berikutnya selama {@link #pegawai} tidak {@code null}. Kombinasi {@code unique = true} pada
	 * kolom ini menjadikannya jaring pengaman keunikan {@code (pegawai, tahun)} di level database.
	 *
	 * @return kode unik {@code "<idPegawai>_<tahun>"}, atau nilai {@link #kodeUnik} tersimpan
	 * apa adanya bila {@link #pegawai} kosong (mis. entity baru yang belum diisi pegawainya)
	 */
	@Column(nullable = false, unique = true)
	public String getKodeUnik() {
		pegawai = getPegawai();
		if (pegawai != null) {
			kodeUnik = pegawai.getId() + "_" + getTahun();
		}
		return kodeUnik;
	}

	/**
	 * Mengisi {@link #kodeUnik} secara langsung. Dalam praktiknya nilai ini akan ditimpa ulang oleh
	 * {@link #getKodeUnik()} pada pemanggilan berikutnya (lihat catatan getter tersebut) selama
	 * {@link #pegawai} sudah terisi — setter ini relevan terutama untuk deserialisasi/hydrasi awal
	 * sebelum {@link #pegawai} tersedia.
	 *
	 * @param kodeUnik kode unik
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

}
