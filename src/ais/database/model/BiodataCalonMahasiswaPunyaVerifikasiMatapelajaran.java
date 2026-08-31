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

	private static final long serialVersionUID = 2463821577548439808L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Calon mahasiswa pemilik data nilai ini. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Mata pelajaran asal sekolah yang nilainya dicatat. */
	private MatapelajaranSekolah matapelajaranSekolah;
	/** Nilai per kelas dalam format encoded {@code "kelas#nilai#verified;..."} — lihat "Format {@link #nilaiKelas}" pada Javadoc kelas. Jangan diakses langsung; pakai {@link #masukkanNilai(String, Boolean, Double)}/{@link #ambilNilai(String)}/{@link #ambilVerifikasi(String)}. */
	private String nilaiKelas;
	/** Kriteria Ketuntasan Minimal (KKM) untuk mata pelajaran ini; dinormalisasi ke {@code 0.0} (bukan {@code null}) sebelum simpan. */
	private Double kkm;
	private String keterangan;

	public BiodataCalonMahasiswaPunyaVerifikasiMatapelajaran() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (isBlank(olehId)) {
			return;
		}
		this.olehId = olehId.trim();
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (isBlank(oleh)) {
			return;
		}
		this.oleh = oleh.trim();
	}

	@javax.persistence.PrePersist
	protected void onPersist() {
		normalize();
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
		normalize();
	}

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

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		biodataCalonMahasiswa = check(biodataCalonMahasiswa);
		return biodataCalonMahasiswa;
	}

	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matapelajaran_sekolah", nullable = true)
	public MatapelajaranSekolah getMatapelajaranSekolah() {
		matapelajaranSekolah = check(matapelajaranSekolah);
		return matapelajaranSekolah;
	}

	public void setMatapelajaranSekolah(MatapelajaranSekolah matapelajaranSekolah) {
		this.matapelajaranSekolah = matapelajaranSekolah;
	}

	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan.trim();
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(columnDefinition = "text")
	public String getNilaiKelas() {
		return nilaiKelas == null ? "" : nilaiKelas.trim();
	}

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

	public Double getKkm() {
		return kkm == null ? Double.valueOf(0.0) : kkm;
	}

	public void setKkm(Double kkm) {
		this.kkm = kkm;
	}

	public String toString() {
		try {
			return getMatapelajaranSekolah() == null ? "" : getMatapelajaranSekolah().getNama();
		} catch (Exception e) {
			return "";
		}
	}

	private static void appendNilai(StringBuilder sb, String value) {
		if (isBlank(value)) {
			return;
		}
		if (sb.length() > 0) {
			sb.append(';');
		}
		sb.append(value);
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
