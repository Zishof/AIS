package ais.database.model.sekolah;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate untuk tabel {@code sekolah.checklist_baru_penilaian_guru_oleh_siswa},
 * merepresentasikan satu baris rekap penilaian seorang {@link Guru} oleh seorang {@link Siswa}
 * untuk satu {@link JadwalPelajaran} tertentu (modul jenjang sekolah). Kombinasi
 * (siswa, jadwalPelajaran, guru) bersifat unik per baris — dijaga lewat kolom turunan
 * {@link #getKodeUnik()} yang dihitung otomatis dari id ketiga relasi tersebut.
 * <p>
 * Berbeda dari pola satu-baris-per-item-penilaian, seluruh jawaban siswa atas banyak butir
 * {@link ChecklistPenilaianGuru} untuk kombinasi ini disimpan <b>terpadatkan dalam satu kolom
 * teks</b> {@link #getKeterangan()}, dengan format per butir {@code "DATA<idButir>;<nilai><>ket"}
 * dan antar-butir dipisah {@code "___"}. Method {@link #setValue(Integer, Siswa, Guru,
 * JadwalPelajaran, ChecklistPenilaianGuru, String)}, {@link #getValue(ChecklistPenilaianGuru)},
 * {@link #getKeteranganValue(ChecklistPenilaianGuru)}, {@link #check(ChecklistPenilaianGuru)},
 * dan {@link #ambilValue()} adalah satu-satunya jalur baca/tulis format terpadatkan ini —
 * dipakai alih-alih tabel anak terpisah agar seluruh hasil checklist satu siswa-guru-jadwal
 * dapat diperbarui dalam satu baris/transaksi.
 * <p>
 * Perubahan (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate
 * Envers), dan setiap update otomatis memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sekolah", name = "checklist_baru_penilaian_guru_oleh_siswa")
public class ChecklistBaruPenilaianGuruOlehSiswa extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private Siswa siswa;
	private JadwalPelajaran jadwalPelajaran;
	private Guru guru;
	private String keterangan = "";
	private String kodeUnik = "";
	private String masukan = "";

	public ChecklistBaruPenilaianGuruOlehSiswa() {
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
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Column(name = "keterangan", columnDefinition = "text", nullable = false)
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan.trim();
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_pelajaran", nullable = false)
	public JadwalPelajaran getJadwalPelajaran() {
		jadwalPelajaran = check(jadwalPelajaran);
		return jadwalPelajaran;
	}

	public void setJadwalPelajaran(JadwalPelajaran jadwalPelajaran) {
		this.jadwalPelajaran = jadwalPelajaran;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = false)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/** Kunci unik turunan {@code "<idSiswa>_<idJadwalPelajaran>_<idGuru>"}, dihitung ulang dari relasi setiap kali diakses; nilai lama dikembalikan bila salah satu relasi belum tersimpan (belum ber-id). */
	@Column(name = "kode_unik", unique = true)
	public String getKodeUnik() {
		Siswa s = getSiswa();
		JadwalPelajaran jp = getJadwalPelajaran();
		Guru g = getGuru();
		if (s == null || s.getId() == null || jp == null || jp.getId() == null || g == null || g.getId() == null) {
			return kodeUnik;
		}
		kodeUnik = s.getId() + "_" + jp.getId() + "_" + g.getId();
		return kodeUnik;
	}

	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	@Column(name = "masukan", columnDefinition = "text", nullable = true)
	public String getMasukan() {
		return masukan == null ? "" : masukan;
	}

	public void setMasukan(String masukan) {
		this.masukan = masukan;
	}

	public int count() {
		return StringUtils.countMatches(getKeterangan(), "DATA");
	}

	public boolean check(ChecklistPenilaianGuru checklistPenilaianGuru) {
		if (checklistPenilaianGuru == null || checklistPenilaianGuru.getId() == null) {
			return false;
		}
		String splBaru = "DATA" + checklistPenilaianGuru.getId() + ";";
		return StringUtils.indexOf(getKeterangan(), splBaru) > -1;
	}

	public List<Object[]> ambilValue() {
		List<Object[]> objects = new ArrayList<Object[]>();
		for (String s : StringUtils.split(getKeterangan(), "___")) {
			if (s == null || !s.contains("DATA")) {
				continue;
			}
			Object[] ss = new Object[3];
			try {
				String[] parts = s.split(";");
				ss[0] = Long.valueOf(parts[0].replaceAll("DATA", ""));
				String[] nilaiKet = parts[1].split("<>");
				ss[1] = Integer.valueOf(nilaiKet[0]);
				ss[2] = nilaiKet.length > 1 ? nilaiKet[1] : "";
				objects.add(ss);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/ChecklistBaruPenilaianGuruOlehSiswa.java:190");
			}
		}
		return objects;
	}

	public void setValue(Integer nilai, Siswa siswa, Guru guru, JadwalPelajaran jadwalPelajaran,
			ChecklistPenilaianGuru checklistPenilaianGuru, String ket) {
		if (checklistPenilaianGuru == null || checklistPenilaianGuru.getId() == null) {
			return;
		}
		if (nilai == null) {
			nilai = Integer.valueOf(0);
		}
		ket = ket == null ? "" : ket;
		ket = org.apache.commons.lang3.StringUtils.replace(ket, ";", ".");
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "<>", ".");
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "DATA", "dat");
		ket = org.apache.commons.lang3.StringUtils.replace(ket, "data", "dat");

		setJadwalPelajaran(jadwalPelajaran);
		setGuru(guru);
		setSiswa(siswa);

		String splBaru = "DATA" + checklistPenilaianGuru.getId() + ";";
		String newKeterangan = "";
		for (String s : StringUtils.split(getKeterangan(), "___")) {
			if (s != null && s.contains("DATA") && !s.startsWith(splBaru)) {
				newKeterangan += newKeterangan.isEmpty() ? s : "___" + s;
			}
		}
		String item = splBaru + nilai + "<>" + ket;
		newKeterangan += newKeterangan.isEmpty() ? item : "___" + item;
		keterangan = newKeterangan;
	}

	public Integer getValue(ChecklistPenilaianGuru checklistPenilaianGuru) {
		if (checklistPenilaianGuru == null || checklistPenilaianGuru.getId() == null) {
			return Integer.valueOf(0);
		}
		String splBaru = "DATA" + checklistPenilaianGuru.getId() + ";";
		int index = StringUtils.indexOf(getKeterangan(), splBaru);
		if (index > -1) {
			try {
				String nilai = StringUtils.substring(getKeterangan(), index);
				nilai = StringUtils.split(nilai, "___")[0];
				String[] sss = StringUtils.split(nilai, ";");
				nilai = sss[sss.length - 1].split("<>")[0];
				return Integer.valueOf(Integer.parseInt(nilai.trim()));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return Integer.valueOf(0);
	}

	public String getKeteranganValue(ChecklistPenilaianGuru checklistPenilaianGuru) {
		if (checklistPenilaianGuru == null || checklistPenilaianGuru.getId() == null) {
			return "";
		}
		String splBaru = "DATA" + checklistPenilaianGuru.getId() + ";";
		int index = StringUtils.indexOf(getKeterangan(), splBaru);
		if (index > -1) {
			try {
				String nilai = StringUtils.substring(getKeterangan(), index);
				nilai = StringUtils.split(nilai, "___")[0];
				String[] sss = StringUtils.split(nilai, ";");
				String[] nilaiKet = sss[sss.length - 1].split("<>");
				return nilaiKet.length > 1 ? nilaiKet[1].trim() : "";
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/ChecklistBaruPenilaianGuruOlehSiswa.java:259");
			}
		}
		return "";
	}

	public String toString() {
		return getKeterangan();
	}
}
