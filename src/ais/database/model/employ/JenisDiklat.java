package ais.database.model.employ;

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

import ais.database.model.GeneralValueObject;



/**
 * Model data untuk jenis diklat. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code id}, {@code oleh}, {@code olehId}, {@code
 * keterangan}, {@code tanggal_dirubah}, {@code nama}; operasi lokal: {@code getOlehId()}, {@code setOlehId()},
 * {@code getId()}, {@code setId()}, {@code setOleh()}, {@code getOleh()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 *
 * <p><b>Dipakai oleh {@code Diklat}, dan berbeda dari {@link JenisPelatihan}:</b> baris kelas ini
 * adalah katalog jenis diklat (mis. Diklatpim, Prajabatan) yang direferensikan lewat relasi
 * {@code @ManyToOne} {@code jenisDiklat} pada entity {@code Diklat} — daftar datar tanpa hierarki
 * parent/level, berbeda dari {@link JenisPelatihan} yang berbentuk pohon (field {@code parent}/
 * {@code deep}) dan dipakai oleh {@code RiwayatPelatihanPegawai}, bukan oleh {@code Diklat}.
 * Kelas ini dikelola lewat {@code JenisDiklatAction} dan DAO {@code JenisDiklatDao}/{@code
 * JenisDiklatDaoImpl}.</p>
 *
 * <p><b>Jangan tertukar dengan entity paket {@code ais.database.model.sister}:</b> nama yang mirip
 * ({@code RefJenisDiklatSister}, dan field teks {@code jenisDiklat} pada {@code TridDiklatSister})
 * adalah entity terpisah untuk sinkronisasi data referensi SISTER (Pangkalan Data Perguruan
 * Tinggi) — tabel {@code public.sister_ref_jenis_diklat} dan kolom teks mentah hasil JSON, bukan
 * relasi JPA ke kelas ini dan tidak berbagi baris data dengan tabel {@code employ.jenis_diklat}.</p>
 *
 * @see GeneralValueObject
 * @see JenisPelatihan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "jenis_diklat")
public class JenisDiklat extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris jenis
	 * diklat ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang diwarisi pola
	 * generiknya dari {@link GeneralValueObject}.
	 *
	 * @return id pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {return olehId;}

	/**
	 * Menetapkan id pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan secara
	 * diam-diam (tidak melempar exception, tidak mengubah state).
	 *
	 * @param olehId id pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	private String keterangan;

	/**
	 * Mengembalikan primary key baris jenis diklat ini.
	 *
	 * @return id baris, atau {@code null} bila belum persisten
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Kolom dipetakan {@code insertable = false} (nilai dihasilkan
	 * database via {@code IDENTITY}), jadi setter ini praktis hanya dipakai saat memuat ulang
	 * entity dari hasil query.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan nama pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan
	 * secara diam-diam, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna (kolom {@code oleh}) yang terakhir membuat/mengubah baris jenis
	 * diklat ini.
	 *
	 * @return nama pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-{@code UPDATE}, mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memutakhirkan
	 * {@link #tanggal_dirubah}. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan tanggal terakhir baris ini dirubah. Biasanya diisi otomatis oleh
	 * {@link #onUpdate()}, bukan dipanggil manual.
	 *
	 * @param tanggal_dirubah tanggal perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan tanggal terakhir baris ini dirubah. Nilai awalnya (sebelum pernah di-update)
	 * diinisialisasi ke waktu saat object dibuat, lewat {@code WaktuUtil.getDate()}.
	 *
	 * @return tanggal perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks jenis diklat ini: mengembalikan {@link #getKeterangan()} (bukan {@code
	 * nama}, berbeda dari kebanyakan katalog "Jenis*" lain di paket ini yang memakai nama pada
	 * {@code toString()}). Dipakai di combobox/label pemilihan jenis diklat pada UI.
	 *
	 * @return keterangan jenis diklat
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mengembalikan keterangan bebas untuk jenis diklat ini. Field ini yang dipakai sebagai
	 * representasi teks oleh {@link #toString()}.
	 *
	 * @return teks keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	private String nama;

	/**
	 * Mengembalikan nama jenis diklat apa adanya (tanpa trim, berbeda dari beberapa katalog
	 * "Jenis*" lain di paket ini yang men-trim nama saat dibaca).
	 *
	 * @return nama jenis diklat, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return nama;
	}

	/**
	 * Menetapkan nama jenis diklat.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

}
