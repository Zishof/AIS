package ais.database.model.crm;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Jenis pipeline CRM (konfigurasi lookup) — mis. "Admisi Mahasiswa Baru", "Kemitraan/Vendor",
 * "Donasi Alumni". Setiap jenis pipeline memiliki kumpulan tahap sendiri ({@link CrmStage}),
 * sehingga modul CRM ini generik lintas domain, bukan dikunci ke satu jenis prospek.
 *
 * <p>Mengikuti pola {@code ais.database.model.ticket.TicketKategori}. Tabel {@code public.crm_pipeline_type}
 * dibuat otomatis oleh {@code hbm2ddl=update} saat restart.</p>
 *
 * <p><b>Peran dalam verifikasi tujuan modul.</b> Entity inilah satu-satunya titik yang membedakan
 * konteks domain sebuah {@link CrmLead} (lihat javadoc di sana): nilainya murni baris konfigurasi
 * yang diisi admin lewat {@code ais.action.master.ticket.CrmKonfigurasiHelper}, BUKAN enum tetap
 * di kode. Tidak ada baris bawaan/seed yang dipaksakan oleh entity ini — bila admin belum pernah
 * membuat satu pun jenis pipeline, modul CRM tidak dapat dipakai sama sekali karena
 * {@code CrmLead#pipelineType} adalah kolom {@code nullable = false}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "crm_pipeline_type")
public class CrmPipelineType extends GeneralValueObject {

	private static final long serialVersionUID = 3120260815001L;

	/** Primary key baris {@code crm_pipeline_type}. */
	private Long id;
	/** Nama jenis pipeline, dipangkas spasi tepi saat dibaca; lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan/deskripsi bebas jenis pipeline. */
	private String keterangan;
	/** Nomor urut tampil pada pilihan combo. */
	private Integer nomorUrut;
	/** Flag aktif/nonaktif (soft delete); lihat {@link #getAktif()} untuk default-nya. */
	private Boolean aktif;
	/** Nama pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOleh(String)}. */
	private String oleh;
	/** Id pengguna pengubah terakhir; diisi jalur audit, lihat {@link #setOlehId(String)}. */
	private String olehId;
	/** Stempel waktu perubahan terakhir; diperbarui otomatis lewat {@link #onUpdate()} saat update. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
	public CrmPipelineType() {
	}

	/**
	 * Mengembalikan primary key baris {@code crm_pipeline_type}.
	 *
	 * @return primary key, atau {@code null} bila entity belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jenis pipeline, dipangkas spasi tepi ({@code trim()}).
	 *
	 * @return nama jenis pipeline yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return nama == null ? null : nama.trim();
	}

	/**
	 * Menyetel nama jenis pipeline. Nilai disimpan apa adanya (tanpa {@code trim()} saat setter
	 * dipanggil); pemangkasan spasi baru terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama jenis pipeline baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/deskripsi bebas jenis pipeline.
	 *
	 * @return keterangan jenis pipeline
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan/deskripsi bebas jenis pipeline.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nomor urut tampil pada pilihan combo. Sama seperti {@link CrmLostReason#getNomorUrut()},
	 * nilai {@code null} TIDAK dinormalkan menjadi {@code 0} — dikembalikan apa adanya.
	 *
	 * @return nomor urut, boleh {@code null} bila belum diisi
	 */
	@Column(name = "nomor_urut", nullable = true)
	public Integer getNomorUrut() {
		return nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil.
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan status aktif/nonaktif (soft delete). Nilai {@code null} dianggap {@code true}.
	 *
	 * @return {@code true} bila aktif (default), {@code false} bila dinonaktifkan
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif/nonaktif.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah entity ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi non-trivial: nilai {@code null}
	 * atau string kosong/spasi diabaikan diam-diam (lihat {@link GeneralValueObject#setOleh(String)}).
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah entity ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOleh(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; nilainya akan ditimpa otomatis
	 * oleh {@link #onUpdate()} pada jalur update Hibernate.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Hook siklus hidup Hibernate {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat
	 * sebelum statement {@code UPDATE} dieksekusi untuk baris ini, mendelegasikan pembaruan stempel
	 * waktu ke {@code AuditTimestampInterceptor.ubah(this)}.
	 */
	@PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Representasi teks ringkas untuk logging/debugging: {@code "<id>-<nama>"}.
	 *
	 * @return representasi teks entity ini
	 */
	public String toString() {
		return id + "-" + nama;
	}
}
