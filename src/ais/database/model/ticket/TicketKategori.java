package ais.database.model.ticket;

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
 * Kategori tiket (konfigurasi lookup) untuk modul Ticketing Management.
 *
 * <p>Kategori dipakai mengelompokkan tiket (mis. "Kendala Sistem", "Permintaan Modul",
 * "Progress Development", "Pertanyaan/Interaksi"). Nilai warna dipakai untuk penanda visual di UI.
 * Mengikuti pola entity standar (mirror {@code Agama}): kolom di public schema, di-audit Envers,
 * tabel dibuat otomatis oleh {@code hbm2ddl=update} saat restart.</p>
 *
 * <p><b>Relasi.</b> Direferensikan oleh {@link Ticket#getTicketKategori()} (FK
 * {@code ticket_kategori} pada tabel {@code ticket}); kategori tidak dapat dihapus selama masih
 * dipakai oleh tiket manapun (dicek di layer action, bukan lewat constraint di kelas ini).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "ticket_kategori")
public class TicketKategori extends GeneralValueObject {

	private static final long serialVersionUID = 3120250724001L;

	/** ID baris (primary key, auto-increment). */
	private Long id;
	/** Nama kategori (mis. "Kendala Sistem"); disyaratkan tidak kosong. */
	private String nama;
	/** Keterangan/deskripsi tambahan kategori; opsional. */
	private String keterangan;
	/** Kode warna (mis. hex/nama warna) dipakai sebagai penanda visual kategori di UI. */
	private String warna;
	/** Nomor urut tampil kategori pada daftar/dropdown. */
	private Integer nomorUrut;
	/** Penanda aktif/nonaktif kategori (soft-delete/hide); {@code null} dianggap aktif ({@code true}). */
	private Boolean aktif;
	/** Nama pengguna terakhir yang mengubah baris ini (field audit bayangan dari {@code GeneralValueObject}). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (field audit bayangan dari {@code GeneralValueObject}). */
	private String olehId;
	/** Waktu perubahan terakhir; diperbarui otomatis oleh {@link #onUpdate()} lewat {@code AuditTimestampInterceptor}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default (dibutuhkan Hibernate). */
	public TicketKategori() {
	}

	/**
	 * Mengambil ID baris (primary key).
	 *
	 * @return ID kategori, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Mengatur ID baris. Biasanya tidak dipanggil manual karena kolom {@code id} auto-increment
	 * dan {@code insertable = false}.
	 *
	 * @param id ID baris baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama kategori. Tidak menulis balik ke field {@link #nama} — hanya mengembalikan
	 * versi ter-{@code trim()} secara sementara, tanpa mempersist perubahan apa pun.
	 *
	 * @return nama kategori yang sudah di-trim, atau {@code null} bila field memang {@code null}.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return nama == null ? null : nama.trim();
	}

	/**
	 * Mengatur nama kategori.
	 *
	 * @param nama nama kategori baru (belum di-trim di setter ini).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/deskripsi tambahan kategori.
	 *
	 * @return keterangan kategori, boleh {@code null}.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengatur keterangan/deskripsi tambahan kategori.
	 *
	 * @param keterangan keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil kode warna penanda visual kategori.
	 *
	 * @return kode warna, boleh {@code null}.
	 */
	@Column(name = "warna", nullable = true, length = 32)
	public String getWarna() {
		return warna;
	}

	/**
	 * Mengatur kode warna penanda visual kategori.
	 *
	 * @param warna kode warna baru.
	 */
	public void setWarna(String warna) {
		this.warna = warna;
	}

	/**
	 * Mengambil nomor urut tampil kategori.
	 *
	 * @return nomor urut, boleh {@code null}.
	 */
	@Column(name = "nomor_urut", nullable = true)
	public Integer getNomorUrut() {
		return nomorUrut;
	}

	/**
	 * Mengatur nomor urut tampil kategori.
	 *
	 * @param nomorUrut nomor urut baru.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengambil status aktif/nonaktif kategori. Tidak menulis balik ke field {@link #aktif} —
	 * hanya mengembalikan {@code true} secara sementara bila field {@code null} (default aktif).
	 *
	 * @return {@code true} bila kategori dianggap aktif.
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengatur status aktif/nonaktif kategori.
	 *
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengambil nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, boleh {@code null} bila belum pernah diubah.
	 */
	@Column(name = "oleh")
	public String getOleh() {
		return oleh;
	}

	/**
	 * Mengatur nama pengguna terakhir yang mengubah baris ini. Nilai kosong/blank diabaikan
	 * (tidak menimpa nilai lama) agar riwayat "oleh" tidak hilang akibat pemanggilan dengan nilai
	 * kosong secara tidak sengaja.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil ID pengguna terakhir yang mengubah baris ini.
	 *
	 * @return ID pengguna pengubah, boleh {@code null} bila belum pernah diubah.
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengatur ID pengguna terakhir yang mengubah baris ini. Nilai kosong/blank diabaikan (tidak
	 * menimpa nilai lama), sama seperti {@link #setOleh(String)}.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengambil waktu perubahan terakhir baris ini.
	 *
	 * @return tanggal perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengatur waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah tanggal perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Callback JPA {@code @PreUpdate} — dipanggil otomatis oleh Hibernate sebelum setiap
	 * {@code UPDATE}, mendelegasikan pencatatan {@link #oleh}/{@link #olehId}/
	 * {@link #tanggal_dirubah} ke {@code AuditTimestampInterceptor.ubah(this)}.
	 */
	@PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Representasi string ringkas kategori, dipakai untuk debugging/log.
	 *
	 * @return string berformat {@code "<id>-<nama>"}.
	 */
	public String toString() {
		return id + "-" + nama;
	}
}
