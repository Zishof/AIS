package ais.database.model;

// Generated Dec 24, 2009 1:27:46 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Entity master data <b>status matakuliah</b> pada tabel {@code public.status_matakuliah}.
 * Mengklasifikasikan sifat wajib/pilihan sebuah {@code Matakuliah} dalam kurikulum (mis. "Wajib",
 * "Pilihan", "Wajib Peminatan", "Pilihan Peminatan", "Tugas akhir/Skripsi/Tesis/Disertasi") —
 * kategori yang lazim dipakai pada pelaporan Feeder/PDDIKTI.
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "status_matakuliah")
public class StatusMatakuliah extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 5783299708791587527L;
	/** Kunci utama tabel {@code public.status_matakuliah} ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String olehId;

	/**
	 * @return ID pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir. Masukan {@code null}/kosong/spasi diabaikan diam-diam
	 * (early return) sehingga nilai audit lama tidak bisa dihapus lewat setter ini.
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, masukan
	 * kosong/{@code null} diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}, dipanggil Hibernate sebelum {@code UPDATE} untuk mengisi ulang
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak untuk dipanggil
	 * langsung dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #getNama() nama} status matakuliah apa adanya. */
	public String toString() {
		return nama;
	}

	/**
	 * Kode singkat status matakuliah. <b>Tidak dipetakan sebagai kolom database</b> (tanpa
	 * {@code @Column}) — murni derivasi in-memory dari {@link #nama}, lihat {@link #getKode()}.
	 */
	private String kode;

	/**
	 * Menurunkan kode singkat dari {@link #getNama() nama} bila {@code kode} belum pernah diisi,
	 * lalu <b>menuliskannya balik ke field</b> {@code kode} sebagai memoisasi (pola getter-mutasi
	 * derivatif yang berulang di paket ini). Pemetaan nama &rarr; kode yang dikenal: "Wajib"
	 * &rarr; "A", "Pilihan" &rarr; "B", "Wajib Peminatan" &rarr; "C", "Pilihan Peminatan" &rarr;
	 * "D", "Tugas akhir/Skripsi/Tesis/Disertasi" &rarr; "S" (pencocokan tanpa membedakan
	 * besar-kecil huruf). Nama di luar daftar ini membuat {@code kode} tetap {@code null}.
	 *
	 * @return kode singkat hasil pemetaan di atas, nilai {@code kode} yang sudah tersimpan
	 *         sebelumnya, atau {@code null} bila nama kosong/tidak dikenali
	 */
	public String getKode() {
		if (kode == null || kode.isEmpty()) {
			if (getNama() != null) {
				if (getNama().equalsIgnoreCase("Wajib")) {
					kode = "A";
				} else if (getNama().equalsIgnoreCase("Pilihan")) {
					kode = "B";
				} else if (getNama().equalsIgnoreCase("Wajib Peminatan")) {
					kode = "C";
				} else if (getNama().equalsIgnoreCase("Pilihan Peminatan")) {
					kode = "D";
				} else if (getNama().equalsIgnoreCase("Tugas akhir/Skripsi/Tesis/Disertasi")) {
					kode = "S";
				}
			}
		}
		return kode;
	}

	/** @param kode kode singkat status matakuliah; menimpa hasil derivasi otomatis {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** Nama status matakuliah (mis. "Wajib", "Pilihan"); wajib diisi. */
	private String nama;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public StatusMatakuliah() {
	}

	/** @param nama nama status matakuliah yang langsung disetel lewat konstruktor ini. */
	public StatusMatakuliah(String nama) {
		this.nama = nama;
	}

	/**
	 * @return kunci utama baris ini, di-generate basis data ({@code IDENTITY}); {@code null}
	 *         sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama status matakuliah, di-trim; {@code null} bila belum pernah diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama status matakuliah; disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

}
