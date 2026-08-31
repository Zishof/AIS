package ais.database.model.sekolah;

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

/**
 * Entitas Hibernate untuk tabel {@code sekolah.grup_checklist_penilaian_guru}, merepresentasikan
 * satu grup/kategori dalam checklist penilaian kinerja guru pada modul sekolah (SD/SMP/SMA) —
 * padanan {@link ais.database.model.sekolah} dari {@link ais.database.model.GrupChecklistPenilaianDosen}
 * pada modul perguruan tinggi. Grup ini menaungi butir-butir pertanyaan checklist (mis. dikelola
 * lewat entitas checklist penilaian guru terkait) dan terhubung ke satu
 * {@link #getAngketPenilaianGuru()} (angket/kuesioner induk) via relasi {@code @ManyToOne} lazy,
 * opsional.
 * <p>
 * Perubahan tercatat historisnya lewat {@link Audited} (Hibernate Envers).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sekolah", name = "grup_checklist_penilaian_guru")
public class GrupChecklistPenilaianGuru extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Angket penilaian guru induk tempat grup ini berada. */
	private AngketPenilaianGuru angketPenilaianGuru;
	/** Nama/judul grup checklist, mis. "Kedisiplinan", "Penguasaan Materi". */
	private String isi;
	private String keterangan;
	private Boolean aktif;

	public GrupChecklistPenilaianGuru() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "angket_penilaian_guru", nullable = true)
	public AngketPenilaianGuru getAngketPenilaianGuru() {
		angketPenilaianGuru = check(angketPenilaianGuru);
		return angketPenilaianGuru;
	}

	public void setAngketPenilaianGuru(AngketPenilaianGuru angketPenilaianGuru) {
		this.angketPenilaianGuru = angketPenilaianGuru;
	}

	@Column(name = "isi", nullable = false, columnDefinition = "text")
	public String getIsi() {
		return isi;
	}

	public void setIsi(String isi) {
		this.isi = isi;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	public String toString() {
		return (id == null ? "" : id + "-") + (isi == null ? "" : isi);
	}
}
