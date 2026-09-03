package ais.database.model.employ;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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
import ais.database.model.rab.SatuanKerja;



/**
 * Model data untuk jam kerja pegawai. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date mulai}, {@code Date sampai}, {@code Integer hari}, {@code SatuanKerja
 * satuanKerja}, {@code Date tanggal_dirubah}; pemetaan persistence: tabel {@code employ.jam_kerja_pegawai};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getTanggal_dirubah()}, {@code getId()}, {@code getOleh()},
 * {@code getHari()}, {@code getMulai()}); mutasi data ({@code setOlehId()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah()}, {@code setId()}, {@code setOleh()}, {@code setHari()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
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
@Table(schema = "employ", name = "jam_kerja_pegawai")



public class JamKerjaPegawai extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris jam
	 * kerja ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang diwarisi pola
	 * generiknya dari {@link GeneralValueObject}.
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
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private Date mulai;
	private Date sampai;
	private Integer hari;
	private SatuanKerja satuanKerja;

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
	 * Mengembalikan primary key baris jam kerja pegawai ini.
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
	 * Mengembalikan nama pengguna (kolom {@code oleh}) yang terakhir membuat/mengubah baris jam
	 * kerja ini.
	 *
	 * @return nama pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Mengembalikan jumlah hari (per minggu, atau kode hari tergantung konvensi pemanggil) yang
	 * dicakup jadwal jam kerja ini.
	 *
	 * @return jumlah/kode hari, atau {@code null} bila belum diset
	 */
	public Integer getHari() {
		return hari;
	}

	/**
	 * Menetapkan jumlah/kode hari.
	 *
	 * @param hari nilai hari baru
	 */
	public void setHari(Integer hari) {
		this.hari = hari;
	}

	/**
	 * Mengembalikan jam mulai kerja. Bila belum pernah diset ({@code null}), <b>method ini
	 * menetapkan sekaligus mengembalikan</b> nilai default 07:30:00 — getter dengan efek samping:
	 * memanggilnya pada entity baru mengisi field {@link #mulai} secara permanen di memori, bukan
	 * sekadar mengembalikan nilai sementara. Kalender dasar diambil dari
	 * {@code WaktuUtil.getCalendar()} lalu jam/menit/detik ditimpa.
	 *
	 * @return jam mulai kerja, default 07:30:00 bila belum diset
	 */
	@Temporal(TemporalType.TIME)
	public Date getMulai() {
		if (mulai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.HOUR_OF_DAY, 7);
			calendar.set(Calendar.MINUTE, 30);
			calendar.set(Calendar.SECOND, 0);
			mulai = calendar.getTime();
		}
		return mulai;
	}

	/**
	 * Menetapkan jam mulai kerja secara eksplisit.
	 *
	 * @param mulai jam mulai baru
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan jam selesai kerja. Sama seperti {@link #getMulai()}, bila belum pernah diset
	 * method ini menetapkan sekaligus mengembalikan nilai default 16:00:00 — getter dengan efek
	 * samping yang mengisi field {@link #sampai} secara permanen saat pertama dipanggil.
	 *
	 * @return jam selesai kerja, default 16:00:00 bila belum diset
	 */
	@Temporal(TemporalType.TIME)
	public Date getSampai() {
		if (sampai == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.HOUR_OF_DAY, 16);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			sampai = calendar.getTime();
		}
		return sampai;
	}

	/**
	 * Menetapkan jam selesai kerja secara eksplisit.
	 *
	 * @param sampai jam selesai baru
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan satuan kerja (entity {@code ais.database.model.rab.SatuanKerja}, dari paket
	 * {@code rab} — <b>bukan</b> {@link SatuanKerjaEmploy} maupun {@link UnitKerja}, lihat catatan
	 * pembeda tiga entity "satuan kerja" di Javadoc {@link UnitKerja}) yang menjadi cakupan jadwal
	 * jam kerja ini.
	 *
	 * @return satuan kerja terkait, atau {@code null} bila berlaku umum/tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja cakupan jadwal jam kerja ini.
	 *
	 * @param satuanKerja satuan kerja baru
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
