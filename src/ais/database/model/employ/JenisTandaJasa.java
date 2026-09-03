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
 * Model data untuk jenis tanda jasa. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal: {@code id}, {@code oleh}, {@code olehId}, {@code
 * keterangan}, {@code nama}, {@code tanggal_dirubah}; operasi lokal: {@code getOlehId()}, {@code setOlehId()},
 * {@code onUpdate()}, {@code getId()}, {@code setId()}, {@code setOleh()}, {@code getOleh()}, {@code
 * setTanggal_dirubah}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 *
 * <p><b>Dipakai oleh {@code RiwayatTandaJasaPegawai}:</b> baris kelas ini adalah katalog jenis
 * tanda jasa/penghargaan (mis. Satya Lencana) yang direferensikan lewat relasi opsional {@code
 * jenisTandaJasa} pada {@code RiwayatTandaJasaPegawai} — kelas ini sendiri tidak menyimpan riwayat
 * per pegawai, hanya master datar tanpa hierarki, dikelola lewat {@code JenisTandaJasaAction} dan
 * DAO {@code JenisTandaJasaDao}/{@code JenisTandaJasaDaoImpl}.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "jenis_tanda_jasa")
public class JenisTandaJasa extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris jenis
	 * tanda jasa ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang diwarisi
	 * pola generiknya dari {@link GeneralValueObject}.
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
	private String nama;

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-{@code UPDATE}, mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memutakhirkan
	 * {@link #tanggal_dirubah}. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengembalikan primary key baris jenis tanda jasa ini.
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
	 * tanda jasa ini.
	 *
	 * @return nama pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

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
	 * Representasi teks jenis tanda jasa ini: mengembalikan {@link #getKeterangan()} (bukan
	 * {@code nama}, berbeda dari kebanyakan katalog "Jenis*" lain di paket ini yang memakai nama
	 * pada {@code toString()}). Dipakai di combobox/label pemilihan jenis tanda jasa pada UI.
	 *
	 * @return keterangan jenis tanda jasa
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mengembalikan keterangan bebas untuk jenis tanda jasa ini. Field ini yang dipakai sebagai
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

	/**
	 * Mengembalikan nama jenis tanda jasa apa adanya (tanpa trim, berbeda dari beberapa katalog
	 * "Jenis*" lain di paket ini yang men-trim nama saat dibaca).
	 *
	 * @return nama jenis tanda jasa, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return nama;
	}

	/**
	 * Menetapkan nama jenis tanda jasa.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

}
