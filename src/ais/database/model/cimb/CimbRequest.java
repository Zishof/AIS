package ais.database.model.cimb;

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




import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;



import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Mahasiswa;

/**
 * Entity JPA/Hibernate header transaksi permintaan pembayaran host-to-host (H2H) via payment
 * gateway <b>CIMB Niaga</b> (produk Virtual Account CIMB Niaga untuk pembayaran biaya mahasiswa,
 * calon mahasiswa, atau peserta kegiatan). Baris {@code cimb_request} dibuat sistem ketika
 * pengguna memulai pembayaran, ditautkan opsional ke {@link Mahasiswa} atau {@link
 * BiodataCalonMahasiswa}, {@link JenisKegiatan}, dan {@link JadwalPembayaran}, lalu direlasikan
 * satu-ke-satu (opsional, diisi belakangan) ke {@link CimbResponse} begitu balasan/callback dari
 * CIMB diterima. Rincian item biaya yang ditagihkan disimpan terpisah pada {@code
 * ais.database.model.cimb.CimbRequestDetail} (per pos tagihan) dan {@code
 * ais.database.model.cimb.CimbRequestDetailBiaya} (per komponen biaya administrasi/fee).
 *
 * <p><b>Pola arsitektur berulang:</b> kelas ini mengikuti pola generik 4-entity (Request/
 * RequestDetail/RequestDetailBiaya/Response) yang identik strukturnya di lebih dari 9 paket model
 * integrasi bank/payment-gateway berbeda di AIS (mis. {@code ais.database.model.bni.BniRequest}
 * untuk Bank BNI). Nama kolom dan makna field pada umumnya sama, namun kelas per gateway tetap
 * dapat memiliki field spesifik gateway tersebut (lihat javadoc tiap field di bawah).</p>
 *
 * <p><b>Catatan keamanan:</b> berbeda dengan beberapa gateway lain di keluarga ini (mis. BNI/
 * Faspay yang memiliki field {@code request}/{@code response} untuk menyimpan payload API mentah),
 * entity ini <i>tidak</i> menyimpan payload request/response mentah dari CIMB — hanya {@link
 * #getTrxId()}, {@link #getStatus()}, dan {@link #getKodeStatus()} yang berperan sebagai
 * identifier/status transaksi. Payload H2H yang sesungguhnya (nomor VA, jumlah tagihan) ditangani
 * oleh endpoint SOAP terpisah {@code CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoapImpl} (di
 * luar paket model ini), yang diketahui menulis representasi request/response ke stdout server
 * (System.out.println) tanpa masking — pola paparan data finansial serupa (walau kanal berbeda)
 * dengan temuan tercatat pada {@code LogHostToHostAction} untuk Bank Mandiri/OCBC NISP.</p>
 *
 * @see CimbResponse
 * @see ais.database.model.bni.BniRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "cimb_request")



public class CimbRequest extends GeneralValueObject {
	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build ({@link java.io.Serializable}).
	 */
	private static final long serialVersionUID = 2463821327548439808L;
	/** Primary key auto-increment (identity) baris permintaan CIMB ini. */
	private Long id;
	/** Nama/label pengguna (audit shadow) yang terakhir membuat/mengubah baris ini. */
	private String oleh;
	/** ID pengguna ({@code Tbmuser}) yang terakhir membuat/mengubah baris ini, disimpan sebagai
	 * audit shadow field yang independen dari relasi entity user. */
	private String olehId;

	/**
	 * Mengambil ID pengguna (audit shadow) yang terakhir membuat/mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna (audit shadow). Nilai {@code null} atau kosong diabaikan (tidak
	 * mereset field ke kosong) sehingga nilai audit sebelumnya tetap dipertahankan bila
	 * dipanggil dengan input tidak valid.
	 *
	 * @param olehId ID pengguna yang akan dicatat; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna (audit shadow). Nilai {@code null} atau kosong diabaikan (tidak
	 * mereset field ke kosong) sehingga nilai audit sebelumnya tetap dipertahankan bila
	 * dipanggil dengan input tidak valid.
	 *
	 * @param oleh nama pengguna yang akan dicatat; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna (audit shadow) yang terakhir membuat/mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: setiap kali baris ini di-UPDATE, memanggil {@code
	 * AuditTimestampInterceptor.ubah(this)} untuk memperbarui {@link #getTanggal_dirubah()}
	 * secara otomatis. Merupakan bagian dari mekanisme audit timestamp yang dipakai seragam di
	 * seluruh entity {@link GeneralValueObject}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi timestamp terakhir baris ini diubah. Biasanya diisi otomatis oleh {@link
	 * #onUpdate()}, bukan dipanggil manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp terakhir baris ini diubah.
	 *
	 * @return timestamp perubahan terakhir; diinisialisasi ke waktu saat objek dibuat sampai
	 *         diperbarui oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk debugging/log: {@code id-nama}.
	 *
	 * @return string ringkas berisi {@link #getId()} dan nama sesi pengguna.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Label sesi/nama pengguna yang membuat permintaan ini; dipetakan ke kolom {@code session_id}. */
	private String nama;
	/** Penanda apakah cicilan sebelumnya (belum lunas) harus dihapus saat permintaan baru dibuat. */
	private Boolean hapusCicilanSebelumnya;
	/** Penanda apakah status transaksi perlu dicek ulang ke CIMB sebelum diproses lebih lanjut. */
	private Boolean checkUlang;

	/** ID transaksi (trxId) yang dikirim ke/diterima dari CIMB sebagai identifier unik transaksi. */
	private String trxId;

	/** Mahasiswa pemilik transaksi ini, bila permintaan dibuat untuk mahasiswa aktif. */
	private Mahasiswa mahasiswa;
	/** Calon mahasiswa pemilik transaksi ini, bila permintaan dibuat untuk proses PMB (belum menjadi mahasiswa). */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Jenis kegiatan (mis. daftar ulang, her-registrasi) yang menjadi konteks tagihan ini. */
	private JenisKegiatan jenisKegiatan;
	/** Jadwal pembayaran yang menjadi acuan komponen biaya yang ditagihkan. */
	private JadwalPembayaran jadwalPembayaran;
	/** Semester akademik terkait transaksi ini. */
	private Integer semester;
	/** Tahun akademik terkait transaksi ini. */
	private String tahunAkademik;
	/** Keterangan bebas mengenai transaksi ini. */
	private String keterangan;
	/** Nilai potongan/pengurangan yang diterapkan pada tagihan. */
	private Double pengurangan;
	/** Total nominal yang diminta untuk dibayarkan melalui CIMB. */
	private Double amount;
	/** Total nilai biaya yang seharusnya dibayar sebelum potongan (nilai acuan, bukan nilai final tagihan). */
	private Double nilaiBiayaHarusDiBayars;
	/** Balasan/hasil dari CIMB untuk permintaan ini, diisi setelah callback diterima. */
	private CimbResponse cimbResponse;
	/** Status pemrosesan transaksi dalam bahasa Indonesia (mis. "Belum diproses"). */
	private String status;
	/** Kode status numerik/mentah dari hasil pemrosesan transaksi. */
	private String kodeStatus;

	/**
	 * Konstruktor default (dibutuhkan Hibernate).
	 */
	public CimbRequest() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID baris, atau {@code null} bila belum dipersistensi.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key baris ini. Umumnya tidak dipanggil manual karena kolom {@code id}
	 * bersifat {@code insertable = false} (nilai dihasilkan basis data).
	 *
	 * @param id nilai ID yang akan diisi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil label sesi/nama pengguna pembuat permintaan, dengan whitespace di-trim.
	 * Dipetakan ke kolom {@code session_id}.
	 *
	 * @return nama yang sudah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "session_id", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi label sesi/nama pengguna pembuat permintaan.
	 *
	 * @param nama nama/label sesi yang akan diisi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil ID transaksi (trxId) unik untuk permintaan ini. Bila field {@link #trxId} masih
	 * kosong namun {@link #id} dan {@link #jenisKegiatan} (beserta kodenya) sudah tersedia, getter
	 * ini <b>membangkitkan ulang</b> trxId secara lazy dari kombinasi kode jenis kegiatan (tanda
	 * baca "." dan "," dibuang) dengan 5 digit terakhir dari {@code id + 1}, lalu menyimpannya ke
	 * field {@link #trxId} sebagai efek samping — pola getter yang menghasilkan/menulis state,
	 * bukan getter murni.
	 *
	 * @return trxId yang sudah di-trim; string kosong bila trxId tidak pernah diisi dan prasyarat
	 *         pembangkitan ulang (id, jenisKegiatan, kode jenisKegiatan) tidak terpenuhi.
	 */
	@Column(unique = true)
	public String getTrxId() {
		if (id != null && jenisKegiatan != null && jenisKegiatan.getKode() != null) {
			String digitKetiga = "000000000000" + (id + 1);
			digitKetiga = digitKetiga.substring(digitKetiga.length() - 5);
			trxId = org.apache.commons.lang3.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(jenisKegiatan.getKode(), ".", ""), ",", "") + digitKetiga;
		}
		return trxId == null ? "" : trxId.trim();
	}

	/**
	 * Mengisi ID transaksi (trxId) secara eksplisit.
	 *
	 * @param trxId trxId yang akan diisi.
	 */
	public void setTrxId(String trxId) {
		this.trxId = trxId;
	}

	/**
	 * Mengambil relasi ke mahasiswa pemilik transaksi ini.
	 *
	 * @return {@link Mahasiswa} terkait, atau {@code null} bila permintaan ini untuk calon
	 *         mahasiswa (bukan mahasiswa aktif).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * Mengisi relasi ke mahasiswa pemilik transaksi ini.
	 *
	 * @param mahasiswa mahasiswa yang akan ditautkan.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengambil relasi ke calon mahasiswa pemilik transaksi ini.
	 *
	 * @return {@link BiodataCalonMahasiswa} terkait, atau {@code null} bila permintaan ini untuk
	 *         mahasiswa aktif (bukan calon mahasiswa).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		return biodataCalonMahasiswa;
	}

	/**
	 * Mengisi relasi ke calon mahasiswa pemilik transaksi ini.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa yang akan ditautkan.
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Mengambil jenis kegiatan yang menjadi konteks tagihan ini.
	 *
	 * @return {@link JenisKegiatan} terkait, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_kegiatan", nullable = true)
	public JenisKegiatan getJenisKegiatan() {
		return jenisKegiatan;
	}

	/**
	 * Mengisi jenis kegiatan yang menjadi konteks tagihan ini.
	 *
	 * @param jenisKegiatan jenis kegiatan yang akan ditautkan.
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * Mengambil jadwal pembayaran acuan komponen biaya yang ditagihkan.
	 *
	 * @return {@link JadwalPembayaran} terkait, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jadwal_pembayaran", nullable = true)
	public JadwalPembayaran getJadwalPembayaran() {
		return jadwalPembayaran;
	}

	/**
	 * Mengisi jadwal pembayaran acuan komponen biaya yang ditagihkan.
	 *
	 * @param jadwalPembayaran jadwal pembayaran yang akan ditautkan.
	 */
	public void setJadwalPembayaran(JadwalPembayaran jadwalPembayaran) {
		this.jadwalPembayaran = jadwalPembayaran;
	}

	/**
	 * Mengambil semester akademik terkait transaksi ini.
	 *
	 * @return nomor semester, atau {@code null} bila belum diisi.
	 */
	public Integer getSemester() {
		return semester;
	}

	/**
	 * Mengisi semester akademik terkait transaksi ini.
	 *
	 * @param semester nomor semester yang akan diisi.
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengambil tahun akademik terkait transaksi ini.
	 *
	 * @return tahun akademik, atau {@code null} bila belum diisi.
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Mengisi tahun akademik terkait transaksi ini.
	 *
	 * @param tahunAkademik tahun akademik yang akan diisi.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengambil nilai potongan/pengurangan tagihan.
	 *
	 * @return nilai pengurangan; {@code 0.0} bila belum pernah diisi (nilai {@code null}
	 *         di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	public Double getPengurangan() {
		if (pengurangan == null) {
			pengurangan = 0.0;
		}
		return pengurangan;
	}

	/**
	 * Mengisi nilai potongan/pengurangan tagihan.
	 *
	 * @param pengurangan nilai pengurangan yang akan diisi.
	 */
	public void setPengurangan(Double pengurangan) {
		this.pengurangan = pengurangan;
	}

	/**
	 * Mengambil total nilai biaya yang seharusnya dibayar (acuan sebelum potongan).
	 *
	 * @return nilai biaya harus dibayar; {@code 0.0} bila belum pernah diisi (nilai {@code null}
	 *         di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	public Double getNilaiBiayaHarusDiBayars() {
		if (nilaiBiayaHarusDiBayars == null) {
			nilaiBiayaHarusDiBayars = 0.0;
		}
		return nilaiBiayaHarusDiBayars;
	}

	/**
	 * Mengisi total nilai biaya yang seharusnya dibayar.
	 *
	 * @param nilaiBiayaHarusDiBayars nilai yang akan diisi.
	 */
	public void setNilaiBiayaHarusDiBayars(Double nilaiBiayaHarusDiBayars) {
		this.nilaiBiayaHarusDiBayars = nilaiBiayaHarusDiBayars;
	}

	/**
	 * Mengambil keterangan bebas mengenai transaksi ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan bebas mengenai transaksi ini.
	 *
	 * @param keterangan keterangan yang akan diisi.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil balasan/hasil dari CIMB untuk permintaan ini.
	 *
	 * @return {@link CimbResponse} terkait, atau {@code null} bila callback belum diterima.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "cimb_response", nullable = true)
	public CimbResponse getCimbResponse() {
		return cimbResponse;
	}

	/**
	 * Mengisi balasan/hasil dari CIMB untuk permintaan ini.
	 *
	 * @param cimbResponse respons CIMB yang akan ditautkan.
	 */
	public void setCimbResponse(CimbResponse cimbResponse) {
		this.cimbResponse = cimbResponse;
	}

	/**
	 * Mengambil total nominal yang diminta untuk dibayarkan melalui CIMB.
	 *
	 * @return nominal transaksi; {@code 0.0} bila belum pernah diisi (nilai {@code null}
	 *         di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	public Double getAmount() {
		if (amount == null) {
			amount = 0.0;
		}
		return amount;
	}

	/**
	 * Mengisi total nominal yang diminta untuk dibayarkan melalui CIMB.
	 *
	 * @param amount nominal yang akan diisi.
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}

	/**
	 * Mengambil status pemrosesan transaksi.
	 *
	 * @return status transaksi; {@code "Belum diproses"} bila belum pernah diisi (nilai {@code
	 *         null} di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	public String getStatus() {
		if (status == null) {
			status = "Belum diproses";
		}
		return status;
	}

	/**
	 * Mengisi status pemrosesan transaksi.
	 *
	 * @param status status yang akan diisi.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengambil kode status numerik/mentah dari hasil pemrosesan transaksi.
	 *
	 * @return kode status, atau {@code null} bila belum diisi.
	 */
	public String getKodeStatus() {
		return kodeStatus;
	}

	/**
	 * Mengisi kode status numerik/mentah dari hasil pemrosesan transaksi.
	 *
	 * @param kodeStatus kode status yang akan diisi.
	 */
	public void setKodeStatus(String kodeStatus) {
		this.kodeStatus = kodeStatus;
	}

	/**
	 * Mengambil penanda apakah cicilan sebelumnya yang belum lunas harus dihapus saat permintaan
	 * baru dibuat.
	 *
	 * @return {@code true} (default bila belum pernah diisi) atau {@code false}.
	 */
	public Boolean getHapusCicilanSebelumnya() {
		if (hapusCicilanSebelumnya == null) {
			hapusCicilanSebelumnya = true;
		}
		return hapusCicilanSebelumnya;
	}

	/**
	 * Mengisi penanda apakah cicilan sebelumnya yang belum lunas harus dihapus.
	 *
	 * @param hapusCicilanSebelumnya nilai penanda yang akan diisi.
	 */
	public void setHapusCicilanSebelumnya(Boolean hapusCicilanSebelumnya) {
		this.hapusCicilanSebelumnya = hapusCicilanSebelumnya;
	}

	/**
	 * Mengambil penanda apakah status transaksi perlu dicek ulang ke CIMB.
	 *
	 * @return {@code false} (default bila belum pernah diisi) atau {@code true}.
	 */
	public Boolean getCheckUlang() {
		if (checkUlang == null) {
			checkUlang = false;
		}
		return checkUlang;
	}

	/**
	 * Mengisi penanda apakah status transaksi perlu dicek ulang ke CIMB.
	 *
	 * @param checkUlang nilai penanda yang akan diisi.
	 */
	public void setCheckUlang(Boolean checkUlang) {
		this.checkUlang = checkUlang;
	}
}
