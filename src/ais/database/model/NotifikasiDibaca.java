package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import ais.ui.util.WaktuUtil;

/**
 * <h1>NotifikasiDibaca — status baca notifikasi PER-PENERIMA</h1>
 *
 * <p>
 * Kolom {@code buka} pada {@link Notifikasi} bersifat per-RECORD: bila sebuah
 * notifikasi ditujukan ke banyak penerima, begitu satu orang membukanya maka
 * seluruh penerima ikut berstatus "sudah dibaca". Tabel ini memperbaiki hal
 * tersebut: setiap baris mencatat bahwa SATU pengguna ({@link #getUserId()}) telah
 * membaca SATU notifikasi ({@link #getNotifikasiId()}). Dengan begitu badge dan
 * pembeda warna dihitung tepat per pengguna.
 * </p>
 *
 * <h2>Desain</h2>
 * <ul>
 * <li>Ringan dan <b>TIDAK</b> di-audit (bukan {@code @Audited}) karena bervolume
 * tinggi dan tidak memerlukan jejak revisi Envers.</li>
 * <li>{@code notifikasiId} disimpan sebagai {@link Long} biasa (bukan relasi
 * {@code @ManyToOne}) agar hemat dan bebas dari pemuatan lazy — pembacaan &amp;
 * penulisan dilakukan lewat SQL native di {@code NotifikasiCache}.</li>
 * <li>Keunikan pasangan {@code (notifikasi_id, user_id)} dijaga di tingkat aplikasi
 * (sisip hanya bila belum ada) dan sebaiknya diperkuat indeks unik di basis data
 * (lihat {@code ALTER_NOTIFIKASI_DIBACA.sql}).</li>
 * </ul>
 *
 * <p>
 * Tabel dibuat otomatis oleh {@code hbm2ddl.auto=update} saat startup (schema
 * {@code public}) setelah kelas ini didaftarkan pada {@code hibernate.cfg.xml}.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "public", name = "notifikasi_dibaca")
public class NotifikasiDibaca extends GeneralValueObject {

	private static final long serialVersionUID = 7451288813344556210L;

	private Long id;
	private Long notifikasiId;
	private String userId;
	private Date waktu = WaktuUtil.getDate();

	public NotifikasiDibaca() {
	}

	public NotifikasiDibaca(Long notifikasiId, String userId) {
		this.notifikasiId = notifikasiId;
		this.userId = userId;
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "notifikasi_id")
	public Long getNotifikasiId() {
		return notifikasiId;
	}

	public void setNotifikasiId(Long notifikasiId) {
		this.notifikasiId = notifikasiId;
	}

	@Column(name = "user_id", columnDefinition = "text")
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	@Override
	protected void onUpdate() {
		// Tidak ada aksi khusus; kolom waktu diisi sekali saat penyisipan.
	}
}
