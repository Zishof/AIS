package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
 * Entity Hibernate untuk tabel {@code public.cabang_prestasi_pegawai} &mdash; master
 * <b>cabang/bidang prestasi pegawai</b> (Kejuaraan Ilmiah, Seni, Olah Raga, Lain-Lain).
 * Padanan {@link CabangPrestasiDosen} untuk domain pegawai (non-dosen); nilai bawaan
 * empat kategori di atas di-seed oleh {@code ais.action.master.PrestasiPegawaiAction}.
 *
 * <p>Direferensikan sebagai FK opsional dari {@code PrestasiPegawai#getCabangPrestasiPegawai()},
 * dipasangkan dengan {@link KategoriPrestasiPegawai} (kategori/tingkat ajang, mis.
 * Internasional/Nasional) pada baris prestasi yang sama.
 *
 * <p>Dikelola lewat CRUD master data sederhana di
 * {@code ais.action.master.CabangPrestasiPegawaiAction}. Diturunkan dari
 * {@link GeneralValueObject}; {@code id}, {@code oleh}, {@code olehId}, dan
 * {@link #tanggal_dirubah} dideklarasikan ulang di sini karena kelas induk adalah POJO
 * abstrak biasa (bukan {@code @Entity}/{@code @MappedSuperclass}) &mdash; keharusan
 * teknis, bukan duplikasi keliru.
 *
 * @see KategoriPrestasiPegawai
 * @see CabangPrestasiDosen
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "cabang_prestasi_pegawai")
public class CabangPrestasiPegawai extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	/** Nama pelaku (audit shadow, lihat {@link GeneralValueObject}) yang membuat/mengubah baris ini. */
	private String oleh;
	/** Id pelaku (audit shadow) yang membuat/mengubah baris ini. */
	private String olehId;

	/** @return id pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pelaku. Nilai kosong/blank diabaikan (fail-safe agar audit shadow tidak
	 * tertimpa string kosong secara tidak sengaja) &mdash; bukan validasi keamanan.
	 *
	 * @param olehId id pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pelaku. Nilai kosong/blank diabaikan, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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

	/** @return representasi ringkas berupa {@code id-nama}, dipakai untuk debugging/log. */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public CabangPrestasiPegawai() {
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

	/**
	 * Mengembalikan kode pelaporan untuk cabang/bidang prestasi ini.
	 *
	 * <p><b>Bukan getter murni</b> &mdash; sama seperti {@link CabangPrestasiDosen#getKode()}.
	 * Method ini <i>menurunkan</i> kode dari {@link #getNama()} lewat perbandingan string
	 * harfiah ("Seni" &rarr; {@code "2"}, "Olah Raga" &rarr; {@code "3"}, "Kejuaraan Ilmiah"
	 * &rarr; {@code "1"}, "Lain-Lain" &rarr; {@code "9"}) dan <b>menulis balik</b> hasilnya ke
	 * field {@code kode} sebelum mengembalikannya. Untuk nama di luar keempat nilai baku itu,
	 * field {@code kode} yang tersimpan sebelumnya (atau {@code null}) dikembalikan apa
	 * adanya.
	 *
	 * @return kode pelaporan cabang prestasi, atau {@code null}/nilai lama bila nama tidak
	 *     cocok salah satu dari empat nilai baku
	 */
	public String getKode() {
		if (nama != null) {
			if (nama.equalsIgnoreCase("Seni")) {
				kode = "2";
			} else if (nama.equalsIgnoreCase("Olah Raga")) {
				kode = "3";
			} else if (nama.equalsIgnoreCase("Kejuaraan Ilmiah")) {
				kode = "1";
			} else if (nama.equalsIgnoreCase("Lain-Lain")) {
				kode = "9";
			}
		}
		return kode;
	}

	/** @param kode kode pelaporan; akan ditimpa oleh {@link #getKode()} bila nama cocok nilai baku. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama cabang/bidang prestasi (wajib diisi), sudah di-trim. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama cabang/bidang prestasi. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan tambahan untuk cabang prestasi ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan tambahan untuk cabang prestasi ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
