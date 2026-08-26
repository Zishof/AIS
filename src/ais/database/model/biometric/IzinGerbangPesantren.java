package ais.database.model.biometric;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;

/** Izin keluar/masuk pondok yang diverifikasi di gerbang. */
@Entity
@Audited
@Table(schema = "public", name = "izin_gerbang_pesantren", uniqueConstraints = @UniqueConstraint(
		name = "uk_izin_gerbang_mutation", columnNames = { "requester_user_id", "client_mutation_id" }))
public class IzinGerbangPesantren implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private String subjectUserId;
	private String requesterUserId;
	private String clientMutationId;
	private String alasan;
	private String tujuan;
	private String pendamping;
	private Date rencanaKeluar;
	private Date rencanaKembali;
	private String status = "DIAJUKAN";
	private String diprosesOleh;
	private String catatanPetugas;
	private Date diprosesPada;
	private Date keluarPada;
	private Date kembaliPada;
	private Long eventKeluarId;
	private Long eventKembaliId;
	private Boolean aktif = Boolean.TRUE;
	private Date dibuatPada = new Date();
	private Date diubahPada = new Date();

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long value) { id = value; }
	@Column(name = "subject_user_id", nullable = false, length = 255)
	public String getSubjectUserId() { return subjectUserId; }
	public void setSubjectUserId(String value) { subjectUserId = value; }
	@Column(name = "requester_user_id", nullable = false, length = 255)
	public String getRequesterUserId() { return requesterUserId; }
	public void setRequesterUserId(String value) { requesterUserId = value; }
	@Column(name = "client_mutation_id", nullable = false, length = 150)
	public String getClientMutationId() { return clientMutationId; }
	public void setClientMutationId(String value) { clientMutationId = value; }
	@Column(name = "alasan", nullable = false, length = 500)
	public String getAlasan() { return alasan; }
	public void setAlasan(String value) { alasan = value; }
	@Column(name = "tujuan", nullable = false, length = 500)
	public String getTujuan() { return tujuan; }
	public void setTujuan(String value) { tujuan = value; }
	@Column(name = "pendamping", length = 255)
	public String getPendamping() { return pendamping; }
	public void setPendamping(String value) { pendamping = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "rencana_keluar", nullable = false)
	public Date getRencanaKeluar() { return rencanaKeluar; }
	public void setRencanaKeluar(Date value) { rencanaKeluar = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "rencana_kembali", nullable = false)
	public Date getRencanaKembali() { return rencanaKembali; }
	public void setRencanaKembali(Date value) { rencanaKembali = value; }
	@Column(name = "status", nullable = false, length = 30)
	public String getStatus() { return status; }
	public void setStatus(String value) { status = value; }
	@Column(name = "diproses_oleh", length = 255)
	public String getDiprosesOleh() { return diprosesOleh; }
	public void setDiprosesOleh(String value) { diprosesOleh = value; }
	@Column(name = "catatan_petugas", length = 1000)
	public String getCatatanPetugas() { return catatanPetugas; }
	public void setCatatanPetugas(String value) { catatanPetugas = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "diproses_pada")
	public Date getDiprosesPada() { return diprosesPada; }
	public void setDiprosesPada(Date value) { diprosesPada = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "keluar_pada")
	public Date getKeluarPada() { return keluarPada; }
	public void setKeluarPada(Date value) { keluarPada = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "kembali_pada")
	public Date getKembaliPada() { return kembaliPada; }
	public void setKembaliPada(Date value) { kembaliPada = value; }
	@Column(name = "event_keluar_id")
	public Long getEventKeluarId() { return eventKeluarId; }
	public void setEventKeluarId(Long value) { eventKeluarId = value; }
	@Column(name = "event_kembali_id")
	public Long getEventKembaliId() { return eventKembaliId; }
	public void setEventKembaliId(Long value) { eventKembaliId = value; }
	@Column(name = "aktif", nullable = false)
	public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
	public void setAktif(Boolean value) { aktif = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "dibuat_pada", nullable = false)
	public Date getDibuatPada() { return dibuatPada; }
	public void setDibuatPada(Date value) { dibuatPada = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "diubah_pada", nullable = false)
	public Date getDiubahPada() { return diubahPada; }
	public void setDiubahPada(Date value) { diubahPada = value; }
}
