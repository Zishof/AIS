package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import org.json.JSONObject;

/**
 * Entitas Hibernate untuk tabel {@code public.checklist_penilaian_dosen}, merepresentasikan satu
 * butir/item checklist yang dipakai untuk menilai kinerja dosen (mis. oleh mahasiswa lewat
 * {@link ChecklistPenilaianDosenOlehMahasiswa}). Setiap butir tergabung dalam satu
 * {@link #getGrupChecklistPenilaianDosen()} (kelompok/kategori checklist, mis. "Kedisiplinan",
 * "Penguasaan Materi"), memiliki {@link #getBobot()} untuk perhitungan skor tertimbang, dan
 * opsional {@link #getPilihan()} berupa string JSON yang mendefinisikan daftar pilihan jawaban
 * kustom (bila kosong berlaku skala penilaian standar).
 * <p>
 * {@link #ambilkey()} menghasilkan kunci unik berbasis kombinasi id grup, lima huruf pertama isi
 * pertanyaan, dan id checklist — dipakai sebagai key baris pada rekap/laporan penilaian.
 * <p>
 * Perubahan tercatat historisnya lewat {@link Audited} (Hibernate Envers).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "checklist_penilaian_dosen")
public class ChecklistPenilaianDosen extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Formatter angka thread-safe (satu instance {@link DecimalFormat} per
	 * thread via {@link ThreadLocal}) yang mem-pad angka menjadi 5 digit
	 * dengan nol di depan (pola {@code "00000"}) — dipakai {@link #ambilkey()}
	 * agar potongan-potongan kunci sebanding secara leksikografis (urutan
	 * string sama dengan urutan numerik).
	 */
	private static final ThreadLocal<NumberFormat> NF = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			return new DecimalFormat("00000");
		}
	};
	/** Representasi string JSON object kosong ({@code "{}"}), dipakai sebagai nilai default {@link #getPilihan()} bila belum diisi. */
	private static final String JSON_KOSONG = new JSONObject().toString();

	/** Primary key (identity, auto-generated oleh database). */
	private Long id;
	/** Nama/label pihak yang menyusun atau terakhir mengubah butir ini (bebas teks, informasional). */
	private String oleh;
	/** Identifier (mis. username/ID pengguna) dari pihak pada {@link #oleh}, dipakai untuk penelusuran audit. */
	private String olehId;
	/** Stempel waktu terakhir diubah; diinisialisasi ke waktu saat objek dibuat dan diperbarui otomatis oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Teks pertanyaan/pernyataan butir checklist penilaian. */
	private String isi;
	/** Nomor urut tampilan/pengurutan butir ini dalam grupnya; dipakai {@link #ambilkey()}/{@link #ambilNomorUrutLaporanKey()} untuk mengurutkan rekap laporan (butir tanpa nomor urut ditempatkan paling akhir, lihat {@link #nomorUrutUrutkan()}). */
	private Integer nomorUrut;
	/** Keterangan tambahan opsional untuk butir checklist ini. */
	private String keterangan;
	/** Grup/kategori tempat butir checklist ini berada. */
	private GrupChecklistPenilaianDosen grupChecklistPenilaianDosen;
	/** Daftar pilihan jawaban kustom dalam format JSON; default JSON kosong bila belum diisi. */
	private String pilihan;
	/** Bobot butir ini dalam perhitungan skor tertimbang; default 1.0. */
	private Double bobot;
	/** Menandai apakah butir checklist ini aktif dipakai; default aktif ({@code true}) bila belum diset. */
	private Boolean aktif;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public ChecklistPenilaianDosen() {
	}

	/**
	 * Konstruktor pintasan untuk membuat referensi ringan (proxy) yang hanya
	 * membawa id, tanpa memuat field lain dari database — berguna dipakai
	 * sebagai target relasi ({@code ManyToOne}) tanpa query tambahan.
	 *
	 * @param id primary key butir checklist yang dirujuk
	 */
	public ChecklistPenilaianDosen(Long id) {
		this.id = id;
	}

	/**
	 * @return identifier pihak penyusun/pengubah ({@link #olehId}), atau {@code null} bila belum diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan identifier pihak penyusun/pengubah (di-trim). Nilai kosong
	 * atau hanya-spasi diabaikan (field lama dipertahankan).
	 *
	 * @param olehId identifier pihak penyusun/pengubah
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId.trim();
	}

	/**
	 * @return nama pihak penyusun/pengubah ({@link #oleh}), atau {@code null} bila belum diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyimpan nama pihak penyusun/pengubah (di-trim). Nilai kosong atau
	 * hanya-spasi diabaikan (field lama dipertahankan), sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pihak penyusun/pengubah
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh.trim();
	}

	/**
	 * Callback JPA sebelum UPDATE: memperbarui stempel waktu audit
	 * {@link #tanggal_dirubah} melalui {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir diubah ({@link #tanggal_dirubah}).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return teks isi butir checklist ({@link #isi}), atau string kosong bila {@code null} (tidak melalui trim).
	 */
	@Override
	public String toString() {
		return isi == null ? "" : isi;
	}

	/**
	 * Menghasilkan kunci unik/urut untuk baris rekap-laporan penilaian:
	 * gabungan id grup (5 digit, di-pad nol), nomor urut pengurutan (5 digit),
	 * lima huruf pertama isi pertanyaan, dan id checklist (5 digit), dipisah
	 * {@code "_"}. Karena setiap komponen numerik di-pad dengan
	 * {@link #NF}, kunci ini bisa diurutkan secara leksikografis dan tetap
	 * menghasilkan urutan yang benar secara numerik (grup lalu nomor urut
	 * lalu id). Nilai id grup/id checklist yang {@code null} diperlakukan
	 * sebagai {@code 0}.
	 *
	 * @return kunci komposit string untuk pengelompokan/pengurutan baris laporan.
	 */
	public String ambilkey() {
		String isiKey = getIsi() == null ? "" : getIsi().trim();
		String prefix = isiKey.length() > 5 ? isiKey.substring(0, 5) : isiKey;
		Long idChecklist = getId() == null ? Long.valueOf(0L) : getId();
		Long idGrup = getGrupChecklistPenilaianDosen() == null || getGrupChecklistPenilaianDosen().getId() == null
				? Long.valueOf(0L)
				: getGrupChecklistPenilaianDosen().getId();
		return NF.get().format(idGrup) + "_" + NF.get().format(Long.valueOf(nomorUrutUrutkan())) + "_" + prefix
				+ "_" + NF.get().format(idChecklist);
	}

	/**
	 * @return {@link #nomorUrut} sebagai {@code long}, atau {@code 99999} bila belum diisi (agar butir tanpa nomor urut jatuh ke urutan paling akhir).
	 */
	private long nomorUrutUrutkan() {
		return getNomorUrut() == null ? 99999L : getNomorUrut().longValue();
	}

	/**
	 * Menghasilkan kunci numerik tunggal yang menggabungkan
	 * {@link #nomorUrutUrutkan()} dan id checklist, dipakai sebagai kunci
	 * pengurutan/pengelompokan baris laporan dalam bentuk {@code long}
	 * (alternatif numerik dari {@link #ambilkey()} yang berbasis string).
	 * Nomor urut negatif dinormalisasi ke {@code 0}; id checklist diambil
	 * modulo 1 miliar agar tetap muat setelah dikombinasikan dengan nomor
	 * urut (nomor urut menempati "digit tinggi", id checklist menempati
	 * "digit rendah").
	 *
	 * @return kunci komposit numerik {@code (nomorUrut * 1_000_000_000) + (idChecklist % 1_000_000_000)}.
	 */
	public Long ambilNomorUrutLaporanKey() {
		long nomor = nomorUrutUrutkan();
		long idChecklist = getId() == null ? 0L : getId().longValue();
		if (nomor < 0L) {
			nomor = 0L;
		}
		return Long.valueOf((nomor * 1000000000L) + (idChecklist % 1000000000L));
	}

	/**
	 * @return primary key baris ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return teks isi butir checklist, bisa {@code null} (berbeda dari kebanyakan entitas checklist sejenis lain yang men-trim/default string kosong lewat {@code safeString}).
	 */
	@Column(name = "isi", nullable = false, columnDefinition = "text")
	public String getIsi() {
		return this.isi;
	}

	/**
	 * @param isi teks isi butir checklist
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/**
	 * @return nomor urut butir ini, bisa {@code null} bila belum diisi (lihat {@link #nomorUrutUrutkan()} untuk default pengurutan).
	 */
	@Column(name = "nomor_urut")
	public Integer getNomorUrut() {
		return nomorUrut;
	}

	/**
	 * @param nomorUrut nomor urut tampilan/pengurutan butir ini
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * @return keterangan tambahan, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan tambahan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return grup/kategori tempat butir checklist ini berada, divalidasi ulang via {@link #check(Object)}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_checklist_penilaian_dosen")
	public GrupChecklistPenilaianDosen getGrupChecklistPenilaianDosen() {
		grupChecklistPenilaianDosen = check(grupChecklistPenilaianDosen);
		return grupChecklistPenilaianDosen;
	}

	/**
	 * @param grupChecklistPenilaianDosen grup/kategori tempat butir checklist ini berada
	 */
	public void setGrupChecklistPenilaianDosen(GrupChecklistPenilaianDosen grupChecklistPenilaianDosen) {
		this.grupChecklistPenilaianDosen = grupChecklistPenilaianDosen;
	}

	/**
	 * @return {@code true} bila butir checklist ini aktif; default {@code true} bila belum diset (nilai default TIDAK di-cache ke field, berbeda dari getter serupa di entitas lain yang meng-cache default ke field-nya).
	 */
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/**
	 * @param aktif menandai apakah butir checklist ini aktif dipakai
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return bobot butir ini untuk perhitungan skor tertimbang; default {@code 1.0} bila belum diset (tidak di-cache ke field).
	 */
	public Double getBobot() {
		return bobot == null ? Double.valueOf(1.0D) : bobot;
	}

	/**
	 * @param bobot bobot butir ini dalam perhitungan skor tertimbang
	 */
	public void setBobot(Double bobot) {
		this.bobot = bobot;
	}

	/**
	 * @return daftar pilihan jawaban kustom dalam format JSON; default {@link #JSON_KOSONG} ({@code "{}"}) bila {@link #pilihan} kosong/belum diisi (tidak di-cache ke field).
	 */
	@Column(columnDefinition = "text")
	public String getPilihan() {
		return pilihan == null || pilihan.trim().isEmpty() ? JSON_KOSONG : pilihan;
	}

	/**
	 * @param pilihan daftar pilihan jawaban kustom dalam format JSON
	 */
	public void setPilihan(String pilihan) {
		this.pilihan = pilihan;
	}
}
