package ais.database.model.employ;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;



/**
 * Model data untuk konfigurasi sk. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code String nama}, {@code Date tanggal_dirubah}, {@code String
 * MENIMBANG}, {@code String MENGINGAT}; pemetaan persistence: tabel {@code employ.konfigurasi_sk};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()},
 * {@code getKeterangan()}, {@code getNama()}); mutasi data ({@code setOlehId()}, {@code onUpdate()}, {@code
 * setId()}, {@code setOleh()}, {@code setTanggal_dirubah()}, {@code setKeterangan()}); operasi domain lain
 * ({@code toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "konfigurasi_sk")



public class KonfigurasiSK extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = -826395530259458150L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris
	 * konfigurasi SK ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang
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
	 * Mengembalikan primary key baris konfigurasi SK ini.
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
	 * Mengembalikan nama pengguna (kolom {@code oleh}) yang terakhir membuat/mengubah baris
	 * konfigurasi SK ini.
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
	 * Representasi teks baris konfigurasi SK ini: mengembalikan {@link #getKeterangan()} apa
	 * adanya (bisa {@code null} bila belum diisi).
	 *
	 * @return keterangan baris ini
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mengembalikan keterangan bebas untuk baris konfigurasi SK ini.
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
	 * Mengembalikan nama baris konfigurasi SK ini — label yang membedakan template/potongan
	 * kalimat SK yang satu dari yang lain dalam grid master data ({@code KonfigurasiSKAction}).
	 *
	 * @return nama baris, wajib diisi (kolom {@code nullable = false})
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return nama;
	}

	/**
	 * Menetapkan nama baris konfigurasi SK.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** Nilai {@link #getJenisField()} untuk baris yang berisi teks bagian "MENIMBANG" pada SK. */
	public static final String MENIMBANG = "MENIMBANG";
	/** Nilai {@link #getJenisField()} untuk baris yang berisi teks bagian "MENGINGAT" pada SK. */
	public static final String MENGINGAT = "MENGINGAT";
	/** Nilai {@link #getJenisField()} untuk baris yang berisi teks bagian "MEMPERHATIKAN" pada SK. */
	public static final String MEMPERHATIKAN = "MEMPERHATIKAN";

	private String jenisField;
	private JenisKegiatanEmploy jenisKegiatanEmploy;
	private String isi;

	/**
	 * Mengembalikan jenis field/bagian SK yang diwakili baris ini — salah satu dari
	 * {@link #MENIMBANG}, {@link #MENGINGAT}, {@link #MEMPERHATIKAN} (atau nilai lain sesuai
	 * konvensi pemanggil; kolom ini {@code String} bebas, bukan enum yang divalidasi database).
	 *
	 * @return jenis field, wajib diisi (kolom {@code nullable = false})
	 */
	@Column(name = "jenis_field", nullable = false)
	public String getJenisField() {
		return jenisField;
	}

	/**
	 * Menetapkan jenis field/bagian SK.
	 *
	 * @param jenisField salah satu konstanta {@link #MENIMBANG}/{@link #MENGINGAT}/
	 *                   {@link #MEMPERHATIKAN}, atau nilai lain sesuai konvensi pemanggil
	 */
	public void setJenisField(String jenisField) {
		this.jenisField = jenisField;
	}

	/**
	 * Mengembalikan jenis kegiatan employ yang menjadi konteks baris konfigurasi SK ini — dipakai
	 * {@code GenerateSkHelper} untuk memilih potongan teks SK yang relevan dengan jenis
	 * kegiatan/SK yang sedang dibuat. Dipetakan {@code fetch} default (eager, tanpa
	 * {@code FetchType.LAZY} eksplisit di anotasi) dengan {@code @Fetch(FetchMode.SELECT)}
	 * sehingga dimuat lewat query {@code SELECT} terpisah, bukan join.
	 *
	 * @return jenis kegiatan employ terkait, wajib diisi (kolom {@code nullable = false})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_kegiatan_employ", nullable = false)
	public JenisKegiatanEmploy getJenisKegiatanEmploy() {
		return jenisKegiatanEmploy;
	}

	/**
	 * Menetapkan jenis kegiatan employ terkait.
	 *
	 * @param jenisKegiatanEmploy jenis kegiatan employ baru
	 */
	public void setJenisKegiatanEmploy(JenisKegiatanEmploy jenisKegiatanEmploy) {
		this.jenisKegiatanEmploy = jenisKegiatanEmploy;
	}

	/**
	 * Mengembalikan isi teks (potongan kalimat SK) untuk baris ini — inilah konten aktual yang
	 * disisipkan {@code GenerateSkHelper} ke dokumen SK yang digenerate, pada bagian
	 * {@link #getJenisField()} dan {@link #getJenisKegiatanEmploy()} yang sesuai.
	 *
	 * @return isi teks, boleh {@code null}
	 */
	@Column(name = "isi")
	public String getIsi() {
		return isi;
	}

	/**
	 * Menetapkan isi teks.
	 *
	 * @param isi isi teks baru
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

}
