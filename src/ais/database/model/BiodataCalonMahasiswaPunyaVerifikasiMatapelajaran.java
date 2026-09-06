package ais.database.model;

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

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

/**
 * Entitas Hibernate: baris tautan antara {@link BiodataCalonMahasiswa} (calon mahasiswa pendaftar)
 * dan {@link MatapelajaranSekolah} (mata pelajaran asal sekolah) — dipetakan ke tabel
 * {@code public.biodata_calon_mahasiswa_punya_verifikasi_matapelajaran}. Menyimpan nilai rapor per
 * kelas/tingkat untuk satu mata pelajaran calon mahasiswa tsb, dipakai verifikasi berkas nilai saat
 * pendaftaran (mis. syarat KKM minimum via {@link #kkm}).
 *
 * <h2>Format {@link #nilaiKelas}</h2>
 * <p>
 * Alih-alih relasi/tabel terpisah per kelas, nilai per kelas disimpan sebagai SATU string di
 * {@link #nilaiKelas}: entri dipisah {@code ";"}, tiap entri berformat
 * {@code "<namaKelas>#<nilai>#<verified>"} (dipisah {@code "#"}). Lihat
 * {@link #masukkanNilai(String, Boolean, Double)} (menulis/mengganti entri per kelas),
 * {@link #ambilNilai(String)}, dan {@link #ambilVerifikasi(String)} (membaca balik) untuk detail
 * parsing-nya. Rapuh terhadap nama kelas yang mengandung karakter {@code ';'} atau {@code '#'}.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "biodata_calon_mahasiswa_punya_verifikasi_matapelajaran")
public class BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran extends GeneralValueObject {

	/** ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database). */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key baris, kolom {@code id} (identity, auto-generate). */
	private Long id;
	/** Nama/username aktor yang membuat/terakhir mengubah baris ini (field audit longgar, bukan FK). */
	private String oleh;
	/** ID aktor yang membuat/terakhir mengubah baris ini (pasangan {@link #oleh}, bukan FK). */
	private String olehId;
	/** Stempel waktu "terakhir diubah"; diinisialisasi ke waktu sekarang saat instance dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Calon mahasiswa pemilik data nilai ini. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Mata pelajaran asal sekolah yang nilainya dicatat. */
	private MatapelajaranSekolah matapelajaranSekolah;
	/** Nilai per kelas dalam format encoded {@code "kelas#nilai#verified;..."} — lihat "Format {@link #nilaiKelas}" pada Javadoc kelas. Jangan diakses langsung; pakai {@link #masukkanNilai(String, Boolean, Double)}/{@link #ambilNilai(String)}/{@link #ambilVerifikasi(String)}. */
	private String nilaiKelas;
	/** Kriteria Ketuntasan Minimal (KKM) untuk mata pelajaran ini; dinormalisasi ke {@code 0.0} (bukan {@code null}) sebelum simpan. */
	private Double kkm;
	/** Keterangan bebas untuk baris verifikasi nilai ini. */
	private String keterangan;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran() {
	}

	/**
	 * @return primary key baris ini; {@code null} sebelum baris di-{@code INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return ID aktor ({@link #olehId}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID aktor audit. Setter ini <b>fail-closed diam-diam</b>: nilai {@code null} atau
	 * string kosong/berspasi diabaikan sepenuhnya (nilai lama dipertahankan, lihat {@link
	 * #isBlank(String)}), tanpa exception maupun log. Nilai yang diterima di-{@code trim()}
	 * sebelum disimpan.
	 *
	 * @param olehId ID aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOlehId(String olehId) {
		if (isBlank(olehId)) {
			return;
		}
		this.olehId = olehId.trim();
	}

	/**
	 * @return nama aktor ({@link #oleh}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null}
	 * atau kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan; nilai yang diterima
	 * di-{@code trim()} sebelum disimpan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {
		if (isBlank(oleh)) {
			return;
		}
		this.oleh = oleh.trim();
	}

	/**
	 * Callback JPA {@code @PrePersist}: dipanggil otomatis oleh Hibernate tepat sebelum baris
	 * ini di-{@code INSERT} untuk pertama kali, mendelegasikan ke {@link #normalize()}.
	 */
	@javax.persistence.PrePersist
	protected void onPersist() {
		normalize();
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code
	 * UPDATE} dieksekusi. Selain memperbarui jejak audit "terakhir diubah" lewat {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah}, juga mendelegasikan ke {@link
	 * #normalize()}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
		normalize();
	}

	/**
	 * Dipanggil dari {@link #onPersist()}/{@link #onUpdate()}: men-trim {@link #nilaiKelas}/
	 * {@link #keterangan}, dan mengisi {@link #kkm} dengan {@code 0.0} bila {@code null}.
	 */
	private void normalize() {
		if (nilaiKelas != null) {
			nilaiKelas = nilaiKelas.trim();
		}
		if (keterangan != null) {
			keterangan = keterangan.trim();
		}
		if (kkm == null) {
			kkm = Double.valueOf(0.0);
		}
	}

	/**
	 * @param tanggal_dirubah stempel waktu "terakhir diubah" baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir baris ini diubah (kolom timestamp), diperbarui otomatis
	 *         oleh {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return calon mahasiswa pemilik data nilai ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	/**
	 * @param biodataCalonMahasiswa calon mahasiswa pemilik baru; {@code null} untuk melepas tautan.
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * @return mata pelajaran asal sekolah yang nilainya dicatat baris ini (proxy lazy
	 *         diresolusi via {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matapelajaran_sekolah", nullable = true)
	public MatapelajaranSekolah getMatapelajaranSekolah() {
		matapelajaranSekolah = check(matapelajaranSekolah);
		return matapelajaranSekolah;
	}

	/**
	 * @param matapelajaranSekolah mata pelajaran baru; {@code null} untuk melepas tautan.
	 */
	public void setMatapelajaranSekolah(MatapelajaranSekolah matapelajaranSekolah) {
		this.matapelajaranSekolah = matapelajaranSekolah;
	}

	/**
	 * @return keterangan bebas baris ini, di-{@code trim()}; string kosong ({@code ""}) bila
	 *         belum diisi — tidak pernah {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * @param keterangan keterangan baru; di-trim otomatis oleh {@link #normalize()} sebelum
	 *                   disimpan (bukan langsung di setter ini).
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return string encoded nilai per kelas apa adanya (lihat "Format {@link #nilaiKelas}" pada
	 *         Javadoc kelas), di-{@code trim()}; string kosong ({@code ""}) bila belum diisi.
	 *         Untuk membaca nilai/verifikasi satu kelas tertentu, pakai {@link
	 *         #ambilNilai(String)}/{@link #ambilVerifikasi(String)}, bukan mem-parse hasil
	 *         getter ini secara manual.
	 */
	@Column(columnDefinition = "text")
	public String getNilaiKelas() {
		return nilaiKelas == null ? "" : nilaiKelas.trim();
	}

	/**
	 * Menyetel string encoded mentah. Dipanggil internal oleh {@link #masukkanNilai(String,
	 * Boolean, Double)} setelah membangun ulang seluruh string; pemanggil luar sebaiknya
	 * memakai {@link #masukkanNilai(String, Boolean, Double)} agar format tetap konsisten,
	 * bukan menyusun string encoded secara manual.
	 *
	 * @param nilaiKelas string encoded baru (format lihat Javadoc kelas).
	 */
	public void setNilaiKelas(String nilaiKelas) {
		this.nilaiKelas = nilaiKelas;
	}

	/**
	 * Menulis atau mengganti entri nilai untuk {@code kelas} tertentu di {@link #nilaiKelas} —
	 * bila {@code kelas} sudah ada entrinya (dicocokkan case-insensitive), entri lama diganti;
	 * bila belum, entri baru ditambahkan di akhir. Lihat "Format {@link #nilaiKelas}" pada Javadoc
	 * kelas untuk struktur penyimpanannya.
	 *
	 * @param kelas   nama kelas/tingkat; tidak melakukan apa pun bila kosong/{@code null}
	 * @param verified status verifikasi nilai untuk kelas tsb
	 * @param nilai   nilai rapor untuk kelas tsb
	 */
	public void masukkanNilai(String kelas, Boolean verified, Double nilai) {
		if (isBlank(kelas)) {
			return;
		}
		String n = getNilaiKelas();
		StringBuilder baru = new StringBuilder();
		boolean ada = false;
		String nilaiText = nilai == null ? "" : String.valueOf(nilai);
		String verifiedText = String.valueOf(verified != null && verified.booleanValue());
		String dataBaru = kelas.trim() + "#" + nilaiText + "#" + verifiedText;

		String[] items = n.split(";");
		for (int i = 0; i < items.length; i++) {
			String s = items[i];
			if (isBlank(s)) {
				continue;
			}
			String[] c = StringUtils.split(s, "#");
			if (c != null && c.length > 0 && kelas.trim().equalsIgnoreCase(c[0].trim())) {
				appendNilai(baru, dataBaru);
				ada = true;
			} else {
				appendNilai(baru, s.trim());
			}
		}

		if (!ada) {
			appendNilai(baru, dataBaru);
		}
		setNilaiKelas(baru.toString());
	}

	/** @return nilai rapor untuk {@code kelas} tsb dari {@link #nilaiKelas} (lihat "Format {@link #nilaiKelas}" pada Javadoc kelas), atau {@code 0.0} bila {@code kelas} kosong/tidak ditemukan/gagal diparse. */
	public Double ambilNilai(String kelas) {
		if (isBlank(kelas)) {
			return Double.valueOf(0.0);
		}
		String[] items = getNilaiKelas().split(";");
		for (int i = 0; i < items.length; i++) {
			String[] c = StringUtils.split(items[i], "#");
			try {
				if (c != null && c.length > 1 && kelas.trim().equalsIgnoreCase(c[0].trim())) {
					return Double.valueOf(Double.parseDouble(c[1].trim()));
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran.java:192");
			}
		}
		return Double.valueOf(0.0);
	}

	/** @return status verifikasi untuk {@code kelas} tsb dari {@link #nilaiKelas}, atau {@code false} bila {@code kelas} kosong/tidak ditemukan. */
	public Boolean ambilVerifikasi(String kelas) {
		if (isBlank(kelas)) {
			return Boolean.FALSE;
		}
		String[] items = getNilaiKelas().split(";");
		for (int i = 0; i < items.length; i++) {
			String[] c = StringUtils.split(items[i], "#");
			if (c != null && c.length > 2 && kelas.trim().equalsIgnoreCase(c[0].trim())) {
				return Boolean.valueOf(Boolean.parseBoolean(c[2].trim()));
			}
		}
		return Boolean.FALSE;
	}

	/**
	 * @return Kriteria Ketuntasan Minimal (KKM) untuk mata pelajaran ini; {@code 0.0} bila
	 *         belum diisi (dinormalkan permanen ke {@code 0.0} saat simpan lewat {@link
	 *         #normalize()}, bukan hanya fallback sesaat pada getter ini).
	 */
	public Double getKkm() {
		return kkm == null ? Double.valueOf(0.0) : kkm;
	}

	/**
	 * @param kkm KKM baru.
	 */
	public void setKkm(Double kkm) {
		this.kkm = kkm;
	}

	/**
	 * Representasi ringkas untuk log/debug: nama mata pelajaran terkait.
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getMatapelajaranSekolah()}, yang menulis balik
	 * field terkait (resolusi proxy lazy via {@code check()}). Bila relasi gagal diresolusi
	 * (mis. entity sudah terputus dari sesi Hibernate), exception ditelan dan method
	 * mengembalikan string kosong.</p>
	 *
	 * @return nama mata pelajaran terkait; string kosong bila tidak ada relasi atau gagal dibaca
	 */
	public String toString() {
		try {
			return getMatapelajaranSekolah() == null ? "" : getMatapelajaranSekolah().getNama();
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * Menambahkan satu entri encoded ke {@link StringBuilder} yang sedang menyusun ulang
	 * {@link #nilaiKelas}, menyelipkan pemisah {@code ';'} bila builder sudah berisi entri
	 * sebelumnya. Entri kosong/{@code null} dilewati (tidak menambah apa pun, termasuk tidak
	 * menambah pemisah kosong).
	 *
	 * @param sb    builder yang sedang menyusun string encoded {@link #nilaiKelas}
	 * @param value entri encoded {@code "kelas#nilai#verified"} yang akan ditambahkan
	 */
	private static void appendNilai(StringBuilder sb, String value) {
		if (isBlank(value)) {
			return;
		}
		if (sb.length() > 0) {
			sb.append(';');
		}
		sb.append(value);
	}

	/**
	 * @param value string yang diperiksa
	 * @return {@code true} bila {@code value} {@code null} atau hanya berisi spasi/kosong
	 *         setelah di-{@code trim()}.
	 */
	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
