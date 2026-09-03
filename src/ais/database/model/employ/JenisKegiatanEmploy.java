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
 * Model data untuk jenis kegiatan employ. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code String keterangan}, {@code String nama}, {@code String KGB}, {@code String pangkat},
 * {@code String pensiun}; pemetaan persistence: tabel {@code employ.jenis_kegiatan_employ}; pembacaan/pencarian
 * ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code
 * getKeterangan()}, {@code getNama()}); mutasi data ({@code setOlehId()}, {@code onUpdate()}, {@code setId()},
 * {@code setOleh()}, {@code setTanggal_dirubah()}, {@code setKeterangan()}); operasi domain lain ({@code
 * toString()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * <p><b>Dipakai oleh {@code KonfigurasiSK} untuk generate dokumen SK, bukan kegiatan
 * kesiswaan/kedosenan:</b> baris kelas ini adalah katalog jenis kegiatan administrasi
 * kepegawaian (mis. KGB, Kenaikan Pangkat, Pensiun, Mutasi Pindah — lihat konstanta {@link #KGB},
 * {@link #pangkat}, {@link #pensiun}, {@link #mutasiPindah}) yang direferensikan lewat relasi
 * {@code @ManyToOne} wajib ({@code nullable = false}) {@code jenisKegiatanEmploy} pada {@code
 * KonfigurasiSK}. {@code GenerateSkHelper} memakai pasangan {@code KonfigurasiSK.jenisField}/
 * {@code KonfigurasiSK.isi} yang dikelompokkan per baris kelas ini untuk menyusun potongan
 * kalimat template Surat Keputusan (SK) kepegawaian. Ini adalah konsep yang sama sekali berbeda
 * dari entity kegiatan kesiswaan/kedosenan pada modul akademik (mis. paket {@code sekolah}) —
 * kemiripan kata "kegiatan" pada namanya tidak menandakan relasi apa pun; kelas ini murni tentang
 * jenis peristiwa administratif kepegawaian yang menghasilkan SK.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "jenis_kegiatan_employ")



public class JenisKegiatanEmploy extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris jenis
	 * kegiatan employ ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang
	 * diwarisi pola generiknya dari {@link GeneralValueObject}.
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
	 * Konstanta label jenis kegiatan "Kenaikan Gaji Berkala" (KGB). Dipakai sebagai nilai
	 * pembanding/rujukan literal di kode pemanggil (mis. {@code GenerateSkHelper}), bukan sebagai
	 * primary key baris — kecocokan aktual tetap ditentukan oleh baris data di tabel {@code
	 * employ.jenis_kegiatan_employ}, konstanta ini hanya salinan teks yang harus dijaga tetap
	 * sinkron secara manual dengan data tersebut.
	 */
	public static final String KGB = "Kenaikan Gaji Berkala";

	/**
	 * Konstanta label jenis kegiatan "Kenaikan Pangkat". Lihat catatan {@link #KGB} soal sifat
	 * salinan teks yang harus tetap sinkron manual dengan data di database.
	 */
	public static final String pangkat = "Kenaikan Pangkat";

	/**
	 * Konstanta label jenis kegiatan "Pensiun". Lihat catatan {@link #KGB} soal sifat salinan teks
	 * yang harus tetap sinkron manual dengan data di database.
	 */
	public static final String pensiun = "Pensiun";

	/**
	 * Konstanta label jenis kegiatan "Mutasi Pindah". Lihat catatan {@link #KGB} soal sifat
	 * salinan teks yang harus tetap sinkron manual dengan data di database.
	 */
	public static final String mutasiPindah = "Mutasi Pindah";

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-{@code UPDATE}, mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memutakhirkan
	 * {@link #tanggal_dirubah}. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengembalikan primary key baris jenis kegiatan employ ini.
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
	 * kegiatan employ ini.
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
	 * Representasi teks jenis kegiatan employ ini: mengembalikan {@link #getKeterangan()} (bukan
	 * {@code nama}, berbeda dari kebanyakan katalog "Jenis*" lain di paket ini yang memakai nama
	 * pada {@code toString()}). Dipakai di combobox/label pemilihan jenis kegiatan pada UI.
	 *
	 * @return keterangan jenis kegiatan employ
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mengembalikan keterangan bebas untuk jenis kegiatan employ ini. Field ini yang dipakai
	 * sebagai representasi teks oleh {@link #toString()}.
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
	 * Mengembalikan nama jenis kegiatan employ apa adanya (tanpa trim, berbeda dari beberapa
	 * katalog "Jenis*" lain di paket ini yang men-trim nama saat dibaca).
	 *
	 * @return nama jenis kegiatan employ, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return nama;
	}

	/**
	 * Menetapkan nama jenis kegiatan employ.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

}
