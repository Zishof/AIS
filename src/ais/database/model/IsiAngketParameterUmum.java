package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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

import org.hibernate.envers.Audited;
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.ChecklistBaruPenilaianGuruOlehSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "isi_angket_parameter_umum")
public class IsiAngketParameterUmum extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private Mahasiswa mahasiswa;
	private Dosen dosen;
	private Siswa siswa;
	private Guru guru;
	private Tbmuser tbmuser;
	private JadwalChecklistPenilaianUmum jadwalChecklistPenilaianUmum;
	private ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa;
	private ChecklistBaruPenilaianGuruOlehSiswa checklistBaruPenilaianGuruOlehSiswa;
	private String nama;
	private String parameterTambahan;
	private String parameterTambahanInds;

	public IsiAngketParameterUmum() {
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

	@javax.persistence.PrePersist
	protected void onPersist() {
		pastikanFieldWajibTerisi();
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		pastikanFieldWajibTerisi();
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	private void pastikanFieldWajibTerisi() {
		clearTbmuserJikaAdaPemilikKhusus();
		if (nama == null || nama.trim().isEmpty()) {
			nama = buildNamaDefault();
		}
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}
	}

	private String buildNamaDefault() {
		StringBuilder sb = new StringBuilder("Angket Parameter Umum");
		try {
			if (checklistBaruPenilaianDosenOlehMahasiswa != null
					&& checklistBaruPenilaianDosenOlehMahasiswa.getId() != null) {
				sb.append(" Dosen #").append(checklistBaruPenilaianDosenOlehMahasiswa.getId());
			} else if (checklistBaruPenilaianGuruOlehSiswa != null
					&& checklistBaruPenilaianGuruOlehSiswa.getId() != null) {
				sb.append(" Guru #").append(checklistBaruPenilaianGuruOlehSiswa.getId());
			} else if (jadwalChecklistPenilaianUmum != null && jadwalChecklistPenilaianUmum.getId() != null) {
				sb.append(" Jadwal #").append(jadwalChecklistPenilaianUmum.getId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/IsiAngketParameterUmum.java:139");
		}

		try {
			if (mahasiswa != null && mahasiswa.getId() != null) {
				sb.append(" - MHS #").append(mahasiswa.getId());
			} else if (siswa != null && siswa.getId() != null) {
				sb.append(" - SISWA #").append(siswa.getId());
			} else if (dosen != null && dosen.getId() != null) {
				sb.append(" - DOSEN #").append(dosen.getId());
			} else if (guru != null && guru.getId() != null) {
				sb.append(" - GURU #").append(guru.getId());
			} else if (tbmuser != null && tbmuser.getUserId() != null) {
				sb.append(" - USER " ).append(tbmuser.getUserId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/IsiAngketParameterUmum.java:154");
		}

		String value = sb.toString();
		return value.length() > 250 ? value.substring(0, 250) : value;
	}

	@Column(name = "nama", nullable = false)
	public String getNama() {
		if (nama == null || nama.trim().isEmpty()) {
			nama = buildNamaDefault();
		}
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
		clearTbmuserJikaAdaPemilikKhusus();
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
		clearTbmuserJikaAdaPemilikKhusus();
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
		clearTbmuserJikaAdaPemilikKhusus();
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = true)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	public void setGuru(Guru guru) {
		this.guru = guru;
		clearTbmuserJikaAdaPemilikKhusus();
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	public void setTbmuser(Tbmuser tbmuser) {
		/*
		 * Kolom tbmuser hanya dipakai untuk pengguna umum/admin yang benar-benar
		 * ada di tabel tbmuser. Untuk mahasiswa/siswa/dosen/guru, relasi khusus
		 * sudah disimpan pada kolom masing-masing. Jika object Tbmuser adalah
		 * wrapper/pseudo seperti new Tbmuser(mahasiswa) atau new Tbmuser(siswa),
		 * jangan disimpan ke kolom tbmuser karena bisa melanggar FK tbmuser.
		 */
		if (memilikiPemilikKhusus() || isTbmuserWrapperPeserta(tbmuser)) {
			this.tbmuser = null;
			return;
		}
		this.tbmuser = tbmuser;
	}

	private void clearTbmuserJikaAdaPemilikKhusus() {
		if (memilikiPemilikKhusus()) {
			this.tbmuser = null;
		}
	}

	private boolean memilikiPemilikKhusus() {
		return mahasiswa != null || siswa != null || dosen != null || guru != null;
	}

	private boolean isTbmuserWrapperPeserta(Tbmuser user) {
		if (user == null) {
			return false;
		}
		try {
			return user.getMahasiswa() != null || user.getSiswa() != null || user.ambilDosen() != null
					|| user.ambilGuru() != null;
		} catch (Exception e) {
			return false;
		}
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_checklist_penilaian_umum", nullable = false)
	public JadwalChecklistPenilaianUmum getJadwalChecklistPenilaianUmum() {
		jadwalChecklistPenilaianUmum = check(jadwalChecklistPenilaianUmum);
		return jadwalChecklistPenilaianUmum;
	}

	public void setJadwalChecklistPenilaianUmum(JadwalChecklistPenilaianUmum jadwalChecklistPenilaianUmum) {
		this.jadwalChecklistPenilaianUmum = jadwalChecklistPenilaianUmum;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "checklist_baru_penilaian_dosen_oleh_mahasiswa", nullable = true)
	public ChecklistBaruPenilaianDosenOlehMahasiswa getChecklistBaruPenilaianDosenOlehMahasiswa() {
		checklistBaruPenilaianDosenOlehMahasiswa = check(checklistBaruPenilaianDosenOlehMahasiswa);
		return checklistBaruPenilaianDosenOlehMahasiswa;
	}

	public void setChecklistBaruPenilaianDosenOlehMahasiswa(
			ChecklistBaruPenilaianDosenOlehMahasiswa checklistBaruPenilaianDosenOlehMahasiswa) {
		this.checklistBaruPenilaianDosenOlehMahasiswa = checklistBaruPenilaianDosenOlehMahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "checklist_baru_penilaian_guru_oleh_siswa", nullable = true)
	public ChecklistBaruPenilaianGuruOlehSiswa getChecklistBaruPenilaianGuruOlehSiswa() {
		checklistBaruPenilaianGuruOlehSiswa = check(checklistBaruPenilaianGuruOlehSiswa);
		return checklistBaruPenilaianGuruOlehSiswa;
	}

	public void setChecklistBaruPenilaianGuruOlehSiswa(
			ChecklistBaruPenilaianGuruOlehSiswa checklistBaruPenilaianGuruOlehSiswa) {
		this.checklistBaruPenilaianGuruOlehSiswa = checklistBaruPenilaianGuruOlehSiswa;
	}

	@Column(name = "parameter_tambahan", columnDefinition = "text")
	public String getParameterTambahan() {
		return parameterTambahan == null ? "" : parameterTambahan;
	}

	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	@Column(name = "parameter_tambahan_inds", columnDefinition = "text")
	public String getParameterTambahanInds() {
		return parameterTambahanInds == null ? "" : parameterTambahanInds;
	}

	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	public List<CommonVO> ambilDataParameterTambahan() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getParameterTambahan().split("\\n");
		for (int j = 0; j < splNama.length; j++) {
			String namaCol = splNama[j] == null ? "" : splNama[j];
			if (namaCol.trim().isEmpty()) {
				continue;
			}
			CommonVO commonVO = new CommonVO();
			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = Integer.valueOf(1);
			try {
				nomorUrut = value.length > 3 ? Integer.valueOf(Integer.parseInt(value[3].trim())) : Integer.valueOf(1);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/IsiAngketParameterUmum.java:334");
			}
			Long paramId = Long.valueOf(1L);
			try {
				paramId = value.length > 4 ? Long.valueOf(Long.parseLong(value[4].trim())) : Long.valueOf(1L);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/IsiAngketParameterUmum.java:339");
			}
			commonVO.setId(paramId.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
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
				if (parameterTambahan == null) {
					continue;
				}
				String jenis = buildJenis(row, parameterTambahan);
				if (jenis == null || jenis.trim().isEmpty()) {
					continue;
				}

				Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
						&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan") : null);
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

				String labelGrup = getLabelGrup(row);
				String s = labelGrup + "->" + parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url
						+ "<=>" + parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
						+ jenis + "<=>0<=>" + ket;
				parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

				String sIds = jenis + "<=>" + val + "<=>" + url + "<=>" + ket;
				parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	private String buildJenis(Row row, ParameterTambahan parameterTambahan) {
		Object grup = row.getAttribute("grupChecklistPenilaianDosen");
		if (grup instanceof GrupChecklistPenilaianDosen) {
			return "DOSEN:" + ((GrupChecklistPenilaianDosen) grup).getId() + "->" + parameterTambahan.getId();
		}
		grup = row.getAttribute("grupChecklistPenilaianGuru");
		if (grup instanceof ais.database.model.sekolah.GrupChecklistPenilaianGuru) {
			return "GURU:" + ((ais.database.model.sekolah.GrupChecklistPenilaianGuru) grup).getId() + "->" + parameterTambahan.getId();
		}
		grup = row.getAttribute("grupChecklistPenilaianUmum");
		if (grup instanceof GrupChecklistPenilaianUmum) {
			return ((GrupChecklistPenilaianUmum) grup).getId() + "->" + parameterTambahan.getId();
		}
		return "";
	}

	private String getLabelGrup(Row row) {
		Object grup = row.getAttribute("grupChecklistPenilaianDosen");
		if (grup instanceof GrupChecklistPenilaianDosen) {
			return "Dosen:" + ((GrupChecklistPenilaianDosen) grup).getIsi();
		}
		grup = row.getAttribute("grupChecklistPenilaianGuru");
		if (grup instanceof ais.database.model.sekolah.GrupChecklistPenilaianGuru) {
			return "Guru:" + ((ais.database.model.sekolah.GrupChecklistPenilaianGuru) grup).getIsi();
		}
		grup = row.getAttribute("grupChecklistPenilaianUmum");
		if (grup instanceof GrupChecklistPenilaianUmum) {
			return ((GrupChecklistPenilaianUmum) grup).getIsi();
		}
		return "Parameter";
	}

	public File write() {
		return write(JadwalChecklistPenilaianUmum.class.getName(), ChecklistBaruPenilaianDosenOlehMahasiswa.class.getName(),
				ChecklistBaruPenilaianGuruOlehSiswa.class.getName(), ParameterTambahan.class.getName());
	}

	public String toString() {
		String n = getNama();
		return n == null ? "" : n;
	}
}
