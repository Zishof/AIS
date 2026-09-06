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

import org.hibernate.envers.Audited;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.grup_checklist_penilaian_dosen}.
 * Merepresentasikan satu butir/kelompok item checklist ({@code isi}) di bawah
 * satu {@link AngketPenilaianDosen} (angket penilaian kinerja dosen) — dipakai
 * untuk mengelompokkan item-item pertanyaan/pernyataan pada angket penilaian
 * dosen (mis. oleh mahasiswa atau atasan) ke dalam grup checklist yang dapat
 * diaktifkan/dinonaktifkan ({@code aktif}).
 *
 * <p>Direferensikan sebagai FK dari {@code ChecklistPenilaianDosen#getGrupChecklistPenilaianDosen()}:
 * setiap butir checklist penilaian tergabung dalam satu grup di sini (mis. "Kedisiplinan",
 * "Penguasaan Materi"), dan id grup ikut membentuk kunci pengurutan
 * {@code ChecklistPenilaianDosen} (lihat javadoc method itu). Dikelola lewat CRUD master
 * data di {@code ais.action.master.GrupChecklistPenilaianDosenAction}.
 *
 * <p>Diturunkan dari {@link GeneralValueObject}; {@code id}, {@code oleh}, {@code olehId},
 * dan {@link #tanggal_dirubah} dideklarasikan ulang di sini karena kelas induk adalah POJO
 * abstrak biasa (bukan {@code @Entity}/{@code @MappedSuperclass}) &mdash; keharusan teknis,
 * bukan duplikasi keliru. Seperti master lain di paket ini, {@link #getAktif()} bersifat
 * satu arah: nilai {@code null} pada kolom dibaca sebagai {@code true} oleh getter, tetapi
 * setter tidak menormalkan {@code null} menjadi {@code true}.
 *
 * @see AngketPenilaianDosen
 * @see ais.database.model.sekolah.GrupChecklistPenilaianGuru
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "grup_checklist_penilaian_dosen")
public class GrupChecklistPenilaianDosen extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;

	private Long id;
	/** Nama pelaku (audit shadow, lihat {@link GeneralValueObject}) yang membuat/mengubah baris ini. */
	private String oleh;
	/** Id pelaku (audit shadow) yang membuat/mengubah baris ini. */
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Angket penilaian dosen tempat grup checklist ini berada. */
	private AngketPenilaianDosen angketPenilaianDosen;
	/** Isi/nama grup checklist (mis. "Kedisiplinan", "Penguasaan Materi"). */
	private String isi;
	/** Keterangan tambahan untuk grup ini, boleh {@code null}. */
	private String keterangan;
	/** Bendera aktif; {@code null} dibaca sebagai {@code true}. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public GrupChecklistPenilaianDosen() {
	}

	/** @return id pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pelaku, di-trim. Nilai kosong/blank diabaikan (fail-safe agar audit shadow
	 * tidak tertimpa string kosong secara tidak sengaja) &mdash; bukan validasi keamanan.
	 *
	 * @param olehId id pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId.trim();
	}

	/** @return nama pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Mengisi nama pelaku, di-trim. Nilai kosong/blank diabaikan, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh.trim();
	}

	/** Callback JPA sebelum update: menyegarkan {@link #tanggal_dirubah} lewat interceptor audit. */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Mengubah stempel waktu perubahan terakhir secara manual. Nilai default sudah
	 * di-set ke waktu saat ini pada deklarasi field dan di-refresh otomatis oleh
	 * {@link #onUpdate()} pada setiap update; setter ini jarang perlu dipanggil langsung.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas berupa {@code id-isi}, dipakai untuk debugging/log. */
	@Override
	public String toString() {
		return (id == null ? "" : id.toString()) + "-" + (isi == null ? "" : isi);
	}

	/** @return id baris (primary key, auto-generated identity di database). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; kolom tidak insertable sehingga nilai ini diabaikan saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return isi/nama grup checklist (wajib diisi), apa adanya (tidak di-trim). */
	@Column(name = "isi", nullable = false, columnDefinition = "text")
	public String getIsi() {
		return this.isi;
	}

	/** @param isi isi/nama grup checklist. */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/** @return keterangan tambahan untuk grup ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan tambahan untuk grup ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return {@code true} bila grup ini aktif dan boleh dipilih; {@code true} juga bila
	 *     kolom masih {@code null} (belum pernah diisi) &mdash; lihat catatan kelas soal
	 *     bendera satu arah.
	 */
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/** @param aktif status aktif baru; tidak dinormalisasi, boleh {@code null}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return angket penilaian dosen tempat grup checklist ini berada, boleh {@code null};
	 *     dilewatkan {@link GeneralValueObject#check(Object)} agar proxy lazy aman dipakai.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "angket_penilaian_dosen", nullable = true)
	public AngketPenilaianDosen getAngketPenilaianDosen() {
		angketPenilaianDosen = check(angketPenilaianDosen);
		return angketPenilaianDosen;
	}

	/** @param angketPenilaianDosen angket penilaian dosen tempat grup checklist ini berada. */
	public void setAngketPenilaianDosen(AngketPenilaianDosen angketPenilaianDosen) {
		this.angketPenilaianDosen = angketPenilaianDosen;
	}
}
