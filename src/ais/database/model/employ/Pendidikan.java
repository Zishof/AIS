package ais.database.model.employ;

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

import ais.database.model.GeneralValueObject;

/**
 * Model data untuk tabel referensi <b>tingkat pendidikan</b> — daftar sederhana nilai pendidikan
 * (mis. SD, SMP, SMA, D3, S1, S2, S3) yang dipilih sebagai atribut anggota keluarga pegawai lewat
 * {@link Keluarga#getPendidikan()}. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap
 * dimiliki {@link GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar
 * spesifik untuk variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di
 * kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 *
 * <p>Kelas ini adalah entity referensi paling sederhana di klaster employ ini: hanya
 * {@code id}/{@code nama}/{@code keterangan} plus field audit generik, tanpa relasi
 * {@code @ManyToOne} apa pun. Berbeda dengan entity master gaji ({@link GajiPokok}/
 * {@link Insentif}/{@link Makan}/{@link Transport}), baris {@code Pendidikan} tidak dicari
 * berdasarkan kriteria kompleks — dipilih langsung lewat combobox berbasis {@link #getId()}/
 * {@link #toString()}.</p>
 *
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String
 * oleh}, {@code String olehId}, {@code String nama}, {@code String keterangan}, {@code Date
 * tanggal_dirubah}; pemetaan persistence: tabel {@code employ.pendidikan}; pembacaan/pencarian
 * ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code
 * getNama()}, {@code getKeterangan()}); mutasi data ({@code setOlehId()}, {@code onUpdate()},
 * {@code setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code setNama()}, {@code
 * setKeterangan()}); operasi domain lain ({@code toString()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value
 * object di memori. Tidak ada relasi lazy yang perlu diresolusi (tidak ada field
 * {@code @ManyToOne}). Persistence, transaksi, dan otorisasi tetap menjadi tanggung jawab
 * DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see Keluarga
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "employ", name = "pendidikan")
public class Pendidikan extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris
	 * tingkat pendidikan ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang
	 * diwarisi pola generiknya dari {@link GeneralValueObject}.
	 *
	 * @return id pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan secara
	 * diam-diam (tidak melempar exception, tidak mengubah state).
	 *
	 * @param olehId id pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan
	 * secara diam-diam, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna (kolom {@code oleh}) yang terakhir membuat/mengubah baris
	 * tingkat pendidikan ini.
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
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * Representasi teks tingkat pendidikan ini: mengembalikan {@link #getNama()} apa adanya.
	 * Dipakai di combobox/label pemilihan tingkat pendidikan pada UI (mis. form data keluarga
	 * pegawai).
	 *
	 * @return nama tingkat pendidikan
	 */
	public String toString() {
		return nama;
	}

	private String nama;
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang dipersyaratkan Hibernate/JPA untuk instansiasi entity lewat
	 * reflection. Tidak menginisialisasi field lain di luar default Java.
	 */
	public Pendidikan() {
	}

	/**
	 * Mengembalikan primary key baris tingkat pendidikan ini.
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
	 * Mengembalikan nama tingkat pendidikan (mis. "SMA", "S1"), di-trim (whitespace di awal/akhir
	 * dibuang) setiap kali dibaca.
	 *
	 * @return nama tingkat pendidikan hasil trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama tingkat pendidikan. Nilai disimpan apa adanya; trim baru terjadi saat dibaca
	 * lewat {@link #getNama()}.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk tingkat pendidikan ini.
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

}
