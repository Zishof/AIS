package ais.database.model;

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

import ais.common.Common;

/**
 * Entity <b>satu pilihan jawaban</b> (tabel {@code public.bank_soal_detail}) milik satu
 * {@link #getBankSoal()} — pada soal pilihan ganda, satu {@link BankSoal} punya beberapa baris
 * {@code BankSoalDetail} (huruf A, B, C, ...) yang masing-masing menyimpan teks jawaban
 * ({@link #getJawaban()}), status benar/salah ({@link #getBetul()}), dan bobot skornya sendiri; pada
 * soal esai, satu {@link BankSoal} biasanya hanya punya satu baris berisi kunci jawaban esai
 * ({@link #getEssay()}).
 *
 * <h3>AKAR temuan task_bee6756e — dipakai BERSAMA seluruh sistem CBT akademik</h3>
 * <p>Tabel {@code bank_soal_detail} ini adalah tabel bersama (shared) yang dipakai LINTAS seluruh
 * modul ujian berbasis komputer (CBT) di aplikasi — ujian akademik reguler maupun modul kursus.
 * task_bee6756e mencatat bahwa endpoint {@code jawab_soal_kuis} pada modul kursus (lihat
 * {@link ais.database.model.kursus.PercobaanKuisKursus},
 * {@link ais.database.model.kursus.JawabanPercobaanKuisKursus}) TIDAK memverifikasi bahwa
 * {@code BankSoalDetail} yang dikirim client benar-benar milik {@link BankSoal} dari soal yang
 * SEDANG DIJAWAB pada percobaan kuis tersebut — karena {@code id} baris {@code BankSoalDetail}
 * bersifat global (bukan di-scope per percobaan/per soal), server yang hanya memvalidasi
 * keberadaan {@code id} (tanpa mencocokkan {@link #getBankSoal()}-nya ke soal yang sah untuk
 * percobaan itu) rentan menerima {@code id} milik soal LAIN — baik dari bank soal lain, ujian
 * lain, atau bahkan modul lain — sebagai jawaban yang "sah" untuk pertanyaan yang sedang berjalan.
 * Relasi {@link #getBankSoal()} pada class ini adalah sumber kebenaran yang seharusnya divalidasi
 * di titik tersebut sebelum submisi diterima.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "bank_soal_detail")

// @Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region="ujian")
public class BankSoalDetail extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah} setiap kali baris
	 * ini di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String essay;
	private Boolean betul = false;
	private String huruf = "A";
	private String jawaban;
	private BankSoal bankSoal;
	private Double skor = 1.0;
	private String kodeUnik;

	private Integer urutanBenar;
	private Integer urutanDiujikan;

	/**
	 * @param arg0 baris {@link BankSoalDetail} lain untuk dibandingkan.
	 * @return hasil perbandingan alfabetis {@link #getHuruf()} antara baris ini dan {@code arg0}
	 *         (dipakai untuk mengurutkan pilihan A, B, C, ... saat ditampilkan); {@code 0} bila
	 *         {@code arg0} bukan {@link BankSoalDetail} atau salah satu huruf {@code null}
	 *         (kegagalan cast diserap diam-diam).
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			BankSoalDetail bankSoalDetail = (BankSoalDetail) arg0;
			if (huruf != null && bankSoalDetail.huruf != null) {
				return huruf.compareTo(bankSoalDetail.huruf);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BankSoalDetail.java:92");

		}

		return 0;
	}

	/**
	 * @return representasi ringkas "{id}-{huruf}-{betul}-{jawaban}-{essay}", dipakai untuk keperluan
	 *         log/debug.
	 */
	public String toString() {
		return id + "-" + getHuruf() + "-" + getBetul() + "-" + getJawaban() + "-" + getEssay();
	}

	/**
	 * @param obj objek lain untuk dibandingkan.
	 * @return {@code true} bila {@code obj} adalah {@link BankSoalDetail} lain dengan
	 *         {@link #getHuruf()} yang sama persis (kesetaraan berbasis huruf pilihan, BUKAN id) —
	 *         perlu diwaspadai bila dipakai membandingkan baris dari {@link BankSoal} yang berbeda,
	 *         karena huruf "A" pada satu soal bisa dianggap "sama" dengan huruf "A" pada soal lain.
	 *         Jatuh balik ke {@code super.equals(obj)} bila huruf {@code null}.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BankSoalDetail && getHuruf() != null) {
			BankSoalDetail workspace = (BankSoalDetail) obj;
			return getHuruf().equals(workspace.getHuruf());
		} else {
			return super.equals(obj);
		}
	}

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public BankSoalDetail() {
	}

	/**
	 * @return id unik baris (surrogate key, auto-increment) — bersifat GLOBAL lintas seluruh
	 *         {@link BankSoal} di sistem (lihat catatan task_bee6756e pada Javadoc kelas).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kunci jawaban esai (untuk soal berjenis esai), di-trim saat dibaca; string kosong bila
	 *         belum diisi (bukan {@code null}).
	 */
	@Column(name = "essay", columnDefinition = "text")
	public String getEssay() {
		if (essay == null) {
			essay = "";
		}
		return this.essay.trim();
	}

	/**
	 * @param essay kunci jawaban esai.
	 */
	public void setEssay(String essay) {
		this.essay = essay;
	}

	/**
	 * @param betul status benar/salah pilihan ini sebagai kunci jawaban.
	 */
	public void setBetul(Boolean betul) {
		this.betul = betul;
	}

	/**
	 * @return {@code true} bila pilihan ini adalah kunci jawaban yang benar; default {@code false}
	 *         bila belum diisi.
	 */
	public Boolean getBetul() {
		if (betul == null) {
			betul = false;
		}
		return betul;
	}

	/**
	 * @param huruf huruf label pilihan ini (mis. "A", "B", "C").
	 */
	public void setHuruf(String huruf) {
		this.huruf = huruf;
	}

	/**
	 * @return huruf label pilihan ini; string kosong bila belum diisi (bukan {@code null}).
	 */
	public String getHuruf() {
		return huruf == null ? "" : huruf;
	}

	/**
	 * @param jawaban teks pilihan jawaban ini.
	 */
	public void setJawaban(String jawaban) {
		this.jawaban = jawaban;
	}

	/**
	 * @return teks pilihan jawaban ini, di-trim saat dibaca; string kosong bila belum diisi (bukan
	 *         {@code null}).
	 */
	@Column(name = "jawaban", columnDefinition = "text")
	public String getJawaban() {
		return jawaban == null ? "" : jawaban.trim();
	}

	/**
	 * @param bankSoal soal induk (bank soal) pemilik pilihan ini.
	 */
	public void setBankSoal(BankSoal bankSoal) {
		this.bankSoal = bankSoal;
	}

	/**
	 * @return soal induk (bank soal) pemilik pilihan ini — relasi yang menjadi sumber kebenaran untuk
	 *         memvalidasi kepemilikan baris ini terhadap soal yang sedang dijawab (lihat catatan
	 *         task_bee6756e pada Javadoc kelas).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_soal", nullable = false)
	public BankSoal getBankSoal() {
		bankSoal = check(bankSoal);
		return bankSoal;
	}

	/**
	 * @return kode unik baris ini untuk keperluan barcode/pelacakan; dibangkitkan sekali (lazy, hanya
	 *         bila {@link #bankSoal} sudah diisi dan kolom belum terisi) dari
	 *         "{idBankSoal}-{huruf}-{barcode-acak}" — getter-mutasi.
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		if (bankSoal != null && (kodeUnik == null || kodeUnik.trim().isEmpty())) {
			kodeUnik = bankSoal.getId() + "-" + getHuruf().trim() + "-" + Common.getGeneratedBarCode();
		}
		return kodeUnik;
	}

	/**
	 * @param kodeUnik kode unik baris ini.
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * @return bobot skor pilihan ini. Untuk soal pilihan ganda jenis {@link BankSoal#MULTIPLE_COICE}
	 *         atau {@link BankSoal#BENAR_SALAH}, skor SELALU dihitung ulang dari skor benar/salah
	 *         milik {@link #getBankSoal()} sesuai {@link #getBetul()} (getter-mutasi, menimpa nilai
	 *         {@link #skor} tersimpan); untuk jenis soal lain, nilai {@link #skor} tersimpan dipakai
	 *         apa adanya (default {@code 1.0} bila belum diisi).
	 */
	public Double getSkor() {
		bankSoal = check(bankSoal);
		if (bankSoal.getJenis().equals(BankSoal.PILIHAN_GANDA)
				&& (bankSoal.getJenisPilihanGanda().equals(BankSoal.MULTIPLE_COICE)
						|| bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH))) {
			skor = getBetul() ? bankSoal.getSkor() : bankSoal.getSkorSalah();
		}
		return skor == null ? 1.0 : skor;
	}

	/**
	 * @param skor bobot skor pilihan ini.
	 */
	public void setSkor(Double skor) {
		this.skor = skor;
	}

	/**
	 * @return urutan tampil pilihan ini pada kunci jawaban (urutan "benar"/master, sebelum diacak
	 *         untuk ujian); default {@code 1} bila belum diisi.
	 */
	public Integer getUrutanBenar() {
		return urutanBenar == null ? 1 : urutanBenar;
	}

	/**
	 * @param urutanBenar urutan tampil pilihan ini pada kunci jawaban master.
	 */
	public void setUrutanBenar(Integer urutanBenar) {
		this.urutanBenar = urutanBenar;
	}

	/**
	 * @return urutan tampil pilihan ini sebagaimana disajikan ke peserta saat ujian berlangsung
	 *         (bisa berbeda dari {@link #getUrutanBenar()} bila pilihan diacak per sesi); default
	 *         {@code 1} bila belum diisi.
	 */
	public Integer getUrutanDiujikan() {
		return urutanDiujikan == null ? 1 : urutanDiujikan;
	}

	/**
	 * @param urutanDiujikan urutan tampil pilihan ini saat diujikan ke peserta.
	 */
	public void setUrutanDiujikan(Integer urutanDiujikan) {
		this.urutanDiujikan = urutanDiujikan;
	}

}
