package ais.action.master.resources;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zul.Label;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.spi.resource.Singleton;

import ais.action.master.helper.generic.LiveStreamingPlayerWindow;
import ais.action.master.resources.model.CommonID;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sisdes.Penduduk;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

@Path("/elearning")
@Singleton

/**
 * Titik akhir REST (Jersey/JAX-RS) untuk integrasi e-learning/mobile: kontrol live streaming
 * perkuliahan (mulai/berhenti, dipanggil server RTMP eksternal), absensi online berbasis
 * lokasi/token (dengan pengecekan jarak GPS terhadap koordinat dosen bila diaktifkan), login
 * (mencoba berurutan sebagai Mahasiswa/Siswa/Penduduk/Tbmuser berdasarkan NIM/NISN/kode/userId +
 * password terenkripsi DES) yang mengembalikan daftar menu mobile dinamis, dan pengambilan data
 * generik berbasis nama kelas Java.
 *
 * <p>
 * <b>Catatan keamanan — CELAH SERIUS (DITUTUP 2026-09-01):</b> {@link #getAmbilData(String, String,
 * String, String)} (endpoint {@code GET /elearning/ambil_data/{token}/{clazz}/{mulai}/{banyak}})
 * sebelumnya menerima nama kelas Java SEMBARANG dari path URL dan langsung memuatnya lewat
 * {@code Class.forName(clazz)} lalu menjalankan query Hibernate {@code Criteria} terhadapnya,
 * mengembalikan seluruh baris (dipaging {@code mulai}/{@code banyak}) sebagai JSON — TANPA
 * memvalidasi parameter {@code token} sama sekali (parameter itu diterima tapi tidak pernah
 * dibandingkan ke nilai apa pun). Siapa pun yang mengetahui URL dapat mengekspos isi tabel entitas
 * Hibernate APA PUN yang dikenal aplikasi (mis. {@link Tbmuser} berisi hash password, data
 * keuangan, data pribadi) tanpa login sama sekali. Endpoint ini kini DINONAKTIFKAN (selalu
 * melempar {@link NotFoundException}) sampai diganti dengan implementasi yang memvalidasi token
 * sesi DAN membatasi kelas entitas yang boleh diambil lewat allow-list eksplisit — lihat catatan
 * pada {@link #getAmbilData(String, String, String, String)}. Endpoint lain di kelas ini
 * ({@link #simpanLive}/{@link #stopLive}/{@link #simpanAbsen}) juga tidak memeriksa autentikasi,
 * meski dampaknya lebih terbatas (kontrol status streaming/absensi per id pertemuan) dan TIDAK
 * diubah pada perbaikan ini.
 * </p>
 */
/**
 * Tipe khusus untuk e learning resource. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getSystemTime()}, {@code
 * getMahasiswa()}, {@code getAmbilData()}, {@code getMasuk()}, {@code getMasukToken()}); mutasi data ({@code
 * simpanLive()}, {@code simpanAbsen()}); operasi domain lain ({@code stopLive()}, {@code doAbsen()}, {@code
 * convert()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
/**
 * Tipe khusus untuk e learning resource. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getSystemTime()}, {@code
 * getMahasiswa()}, {@code getAmbilData()}, {@code getMasuk()}, {@code getMasukToken()}); mutasi data ({@code
 * simpanLive()}, {@code simpanAbsen()}); operasi domain lain ({@code stopLive()}, {@code doAbsen()}, {@code
 * convert()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
/**
 * Tipe khusus untuk e learning resource. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getSystemTime()}, {@code
 * getMahasiswa()}, {@code getAmbilData()}, {@code getMasuk()}, {@code getMasukToken()}); mutasi data ({@code
 * simpanLive()}, {@code simpanAbsen()}); operasi domain lain ({@code stopLive()}, {@code doAbsen()}, {@code
 * convert()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
/**
 * Tipe khusus untuk e learning resource. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getSystemTime()}, {@code
 * getMahasiswa()}, {@code getAmbilData()}, {@code getMasuk()}, {@code getMasukToken()}); mutasi data ({@code
 * simpanLive()}, {@code simpanAbsen()}); operasi domain lain ({@code stopLive()}, {@code doAbsen()}, {@code
 * convert()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
/**
 * Tipe khusus untuk e learning resource. Kelas ini memberi nama dan batas tanggung jawab yang
 * eksplisit pada perilaku yang diwarisi atau kontrak yang diimplementasikannya.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code getSystemTime()}, {@code
 * getMahasiswa()}, {@code getAmbilData()}, {@code getMasuk()}, {@code getMasukToken()}); mutasi data ({@code
 * simpanLive()}, {@code simpanAbsen()}); operasi domain lain ({@code stopLive()}, {@code doAbsen()}, {@code
 * convert()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class ELearningResource {

	/** Mengembalikan waktu server saat ini (epoch millis) — dipakai untuk sinkronisasi jam klien mobile. */
	public long getSystemTime() {
		return System.currentTimeMillis();
	}

	@GET
	@Path("test/{nama}")
	@Produces({ MediaType.APPLICATION_JSON })
	/** Endpoint uji coba sederhana yang menggemakan (echo) parameter {@code nama} yang diberikan. */
	public CommonID getMahasiswa(@PathParam("nama") String nama) {
		CommonID commonID = new CommonID();
		commonID.setInfo1(nama);
		return commonID;
	}

	@GET
	@Path("simpan_live/{id}/{kodeStream}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Menandai satu {@link Pertemuan} sebagai sedang live streaming: mencatat kode stream ke
	 * {@link LiveStreamingPlayerWindow}, menyimpan info video, dan menandai kolom
	 * {@code publikasikanstreaming=true} lewat SQL native langsung. Dipanggil oleh server RTMP
	 * eksternal saat stream dimulai; tidak memeriksa autentikasi.
	 *
	 * @param id         id {@link Pertemuan} yang sedang live
	 * @param kodeStream kode unik stream dari server RTMP
	 * @return status sukses/gagal
	 */
	public CommonID simpanLive(@PathParam("id") String id, @PathParam("kodeStream") String kodeStream) {

		Session session = HibernateUtil.currentNativeSession();
		try {
			CommonID commonID = new CommonID();

			System.out.println("simpan_live = " + id + ", kodeStream = " + kodeStream);

			Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(id.trim()))).uniqueResult();

			if (pertemuan != null) {
				String host = Common.getKonfigurasi("rtmp_server", "live.ecampus.id").getNilai();
				LiveStreamingPlayerWindow.simpanVideo(kodeStream, host, pertemuan);

				LiveStreamingPlayerWindow.steams.put(kodeStream,
						new Object[] { new ArrayList<String>(), new ArrayList<Label>() });

				session.createSQLQuery("update pertemuan set publikasikanstreaming=true where id=" + pertemuan.getId())
						.executeUpdate();

				commonID.setInfo1("Sukses");
			} else {
				commonID.setInfo1("Gagal");
			}

			HibernateUtil.closeSession();
			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@GET
	@Path("stop_live/{id}/{kodeStream}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Menandai satu {@link Pertemuan} berhenti live streaming: menghapus kode stream (dan kode
	 * turunannya yang berawalan sama) dari {@link LiveStreamingPlayerWindow}, menandai kolom
	 * {@code publikasikanstreaming=false} lewat SQL native langsung. Dipanggil oleh server RTMP
	 * eksternal saat stream berakhir; tidak memeriksa autentikasi.
	 *
	 * @param id         id {@link Pertemuan} yang berhenti live
	 * @param kodeStream kode unik stream dari server RTMP
	 * @return status sukses/gagal
	 */
	public CommonID stopLive(@PathParam("id") String id, @PathParam("kodeStream") String kodeStream) {

		Session session = HibernateUtil.currentNativeSession();
		try {
			CommonID commonID = new CommonID();

			System.out.println("stop_live = " + id + ", kodeStream = " + kodeStream);

			Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(id.trim()))).uniqueResult();

			if (pertemuan != null) {
				LiveStreamingPlayerWindow.steams.remove(kodeStream);

				String s = kodeStream.split("_")[0];
				if (!s.trim().isEmpty()) {
					synchronized (LiveStreamingPlayerWindow.steams) {
						for (String k : LiveStreamingPlayerWindow.steams.keySet()) {
							if (k.startsWith(s)) {
								LiveStreamingPlayerWindow.steams.remove(k);
							}
						}
					}
				}

				session.createSQLQuery("update pertemuan set publikasikanstreaming=false where id=" + pertemuan.getId())
						.executeUpdate();
				commonID.setInfo1("Sukses");
			} else {
				commonID.setInfo1("Gagal");
			}

			HibernateUtil.closeSession();
			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	/**
	 * Implementasi inti pencatatan absensi online untuk satu {@link Pertemuan}: memvalidasi bahwa
	 * data absensi ({@code data}, berisi token/kode per peserta yang dipisah — lihat pemakaian lebih
	 * lanjut di badan method) diberikan, menghitung jendela waktu absensi dari jadwal pertemuan, dan
	 * (bila koordinat {@code lat}/{@code lng} diberikan dan validasi jarak GPS diaktifkan) memeriksa
	 * jarak terhadap lokasi acuan sebelum mencatat kehadiran dengan {@code keterangan} yang diberikan.
	 * Dipanggil oleh {@link #simpanAbsen} (endpoint publik) dan berpotensi jalur lain di aplikasi.
	 *
	 * @param id          id {@link Pertemuan} yang diabsen
	 * @param data        data absensi (kode/token peserta)
	 * @param lat         lintang lokasi absen, boleh {@code null}
	 * @param lng         bujur lokasi absen, boleh {@code null}
	 * @param keterangan  keterangan yang dicatat pada baris absensi
	 * @return status hasil absensi
	 */
	public static CommonID doAbsen(String id, String data, String lat, String lng, String keterangan) {

		Session session = HibernateUtil.currentNativeSession();
		try {
			CommonID commonID = new CommonID();

			System.out.println("simpan_absen = " + id + ", data = " + data);

			Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
					.add(Restrictions.idEq(Long.parseLong(id.trim()))).uniqueResult();

			if (pertemuan != null && data != null && !data.trim().isEmpty()) {

				Calendar calendarMulai = Calendar.getInstance();

				if (pertemuan.getTanggalRealisasi() != null) {
					calendarMulai.setTime(pertemuan.getTanggalRealisasi());
				} else if (pertemuan.getTanggal() != null) {
					calendarMulai.setTime(pertemuan.getTanggal());
				}

				try {
					if (pertemuan.getWaktuMulai() != null) {
						Integer jamMulai = Integer.parseInt(pertemuan.getWaktuMulai().split("\\.")[0]);
						Integer menitMulai = Integer.parseInt(pertemuan.getWaktuMulai().split("\\.")[1]);
						calendarMulai.set(Calendar.HOUR_OF_DAY, jamMulai);
						calendarMulai.set(Calendar.MINUTE,
								menitMulai - pertemuan.getBolehAbsenSebelumWaktuMulaiDalamMenit());
						calendarMulai.set(Calendar.SECOND, 1);

					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/resources/ELearningResource.java:179");
				}

				Calendar calendarSelesai = Calendar.getInstance();

				if (pertemuan.getTanggalRealisasi() != null) {
					calendarSelesai.setTime(pertemuan.getTanggalRealisasi());
				} else {
					calendarSelesai.setTime(pertemuan.getTanggal());
				}

				try {
					if (pertemuan.getWaktuMulai() != null) {
						Integer jamMulai = Integer.parseInt(pertemuan.getWaktuSelesai().split("\\.")[0]);
						Integer menitMulai = Integer.parseInt(pertemuan.getWaktuSelesai().split("\\.")[1]);
						calendarSelesai.set(Calendar.HOUR_OF_DAY, jamMulai);
						calendarSelesai.set(Calendar.MINUTE,
								menitMulai + pertemuan.getBolehAbsenSetelahWaktuMulaiDalamMenit());
						calendarSelesai.set(Calendar.SECOND, 1);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/resources/ELearningResource.java:200");
				}

				String status = "Sukses";
				String map = "";
				if (lat != null && lng != null) {
					map = "https://maps.google.com/maps?q=" + lat + "," + lng + "&hl=id&z=14";
				}

				if (pertemuan != null && pertemuan.getLokasi() != null && lat != null && lng != null) {

					try {
						double latitude1 = pertemuan.getLokasi().getLat();
						double longitude1 = pertemuan.getLokasi().getLng();
						double latitude2 = Double.parseDouble(lat);
						double longitude2 = Double.parseDouble(lng);

						Double jarakKm = Common.getDistanceBetweenPointsNew(latitude1, longitude1, latitude2,
								longitude2);

						if (jarakKm > pertemuan.getJarak()) {

							status = "Absensi gagal dilakukan pada " + Common.dateFormat5.get().format(WaktuUtil.getDate())
									+ ", karena jarak lokasi Anda berada " + Common.numberFormat.get().format(jarakKm)
									+ "km dari lokasi/koordinat " + pertemuan.getLokasi().getNama();

						}

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/resources/ELearningResource.java:229");
					}
				}

				if (status.equals("Sukses")) {

					int selisih = 0;
					int toleransiHari = 0;
					Date currentDate = WaktuUtil.getDate();
					boolean harusSesuai = Common.bolehKonfigurasi("absen_harus_sesuai_waktu");
					if (harusSesuai) {

						selisih = pertemuan.getTanggal() == null ? 0
								: Math.abs(Common.getBetweenTwoDates(currentDate, pertemuan.getTanggal())) - 1;

						toleransiHari = pertemuan.getPerkuliahan() == null ? 1000
								: pertemuan.getPerkuliahan().getBatasWaktuBolehAbsenKehadiran();

						if (Common.bolehKonfigurasi("jumlah_hari_batas_waktu_pakai_default", Konfigurasi.TIDAK_AKTIF)) {
							try {
								toleransiHari = Integer.parseInt(Common
										.getKonfigurasi("jumlah_hari_batas_waktu_dalam_hari", "0").getNilai().trim());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/ELearningResource.java:251");
								// TODO: handle exception
							}
						}
					}

					if (harusSesuai && pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal() && selisih > toleransiHari) {

						if (currentDate.before(calendarMulai.getTime())) {
							String d = (SmartDateTimeUtil.getDayString(calendarMulai.getTime(), null)
									+ Common.dateFormat5.get().format(calendarMulai.getTime()));
							status = "Absensi gagal pada " + Common.dateFormat5.get().format(WaktuUtil.getDate())
									+ ", karena absensi online belum dimulai " + d;
						} else if (currentDate.after(calendarSelesai.getTime())) {
							String d = (SmartDateTimeUtil.getDayString(calendarSelesai.getTime(), null)
									+ Common.dateFormat5.get().format(calendarSelesai.getTime()));
							status = "Absensi gagal pada " + Common.dateFormat5.get().format(WaktuUtil.getDate())
									+ ", karena absensi online telah terlewat " + d;
						}
					}
				}

				Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.eq("idfinger", data), Restrictions.eq("nim", data)))
						.setMaxResults(1), Mahasiswa.class);
				Siswa siswa = null;
				Dosen dosen = null;
				Guru guru = null;
				Tbmuser tbmuser = null;
				if (mahasiswa == null) {
					siswa = (Siswa) ConstantValues
							.simpleObject(session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
									.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
									.add(Restrictions.or(Restrictions.eq("idfinger", data),
											Restrictions.eq("nomorInduk", data)))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.setMaxResults(1), Siswa.class);
				}

				if (mahasiswa == null && siswa == null) {
					dosen = (Dosen) ConstantValues.simpleObject(session.createCriteria(Dosen.class)
							.add(Restrictions.or(Restrictions.eq("idfinger", data), Restrictions.eq("nidn", data)))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1), Dosen.class);
				}

				if (mahasiswa == null && siswa == null && dosen == null) {
					guru = (Guru) ConstantValues.simpleObject(session.createCriteria(Guru.class)
							.add(Restrictions.isNotNull("sekolah"))
							.add(Restrictions.or(Restrictions.eq("idfinger", data), Restrictions.eq("nuks", data)))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setMaxResults(1), Guru.class);
				}

				if (mahasiswa == null && siswa == null && dosen == null && guru == null) {
					tbmuser = (Tbmuser) ConstantValues.simpleObject(session.createCriteria(Tbmuser.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN).add(Restrictions
									.or(Restrictions.eq("pegawai.idfinger", data), Restrictions.eq("userId", data)))
							.setMaxResults(1), Tbmuser.class);
				}

				if (mahasiswa != null) {

					String ket = "";
					Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							pertemuan.retreiveAbsensiId(mahasiswa.getId()));
					if (statusabsensi == null) {
						statusabsensi = ConstantValues.BELUM_ABSEN;
					}

					if (status.equals("Sukses")) {
						statusabsensi = ConstantValues.MASUK;
						ket = (ket.isEmpty() ? keterangan : keterangan + ";") + ket;
					} else {
						ket = (ket.isEmpty() ? status : status + ";") + ket;
					}

					if (lat != null && lng != null) {
						ket += ", lokasi absen " + map + " ";
					}

					pertemuan.populate(mahasiswa.getId(), statusabsensi, ket, null,
							Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()), pertemuan.getWaktuSelesai(),
							"Mahasiswa");
					session.getTransaction().begin();
					session.update(pertemuan);
					session.getTransaction().commit();

					commonID = convert(mahasiswa, siswa, tbmuser);
					commonID.setInfo10(status);
				} else if (siswa != null) {

					String ket = "";
					Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							pertemuan.retreiveAbsensiId(siswa.getId()));
					if (statusabsensi == null) {
						statusabsensi = ConstantValues.BELUM_ABSEN;
					}

					if (status.equals("Sukses")) {
						statusabsensi = ConstantValues.MASUK;
						ket = (ket.isEmpty() ? keterangan : keterangan + ";") + ket;
					} else {
						ket = (ket.isEmpty() ? status : status + ";") + ket;
					}

					if (lat != null && lng != null) {
						ket += ", lokasi absen " + map + " ";
					}

					pertemuan.populate(siswa.getId(), statusabsensi, ket, null,
							Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()), pertemuan.getWaktuSelesai(),
							"Siswa");
					session.getTransaction().begin();
					session.update(pertemuan);
					session.getTransaction().commit();

					commonID = convert(mahasiswa, siswa, tbmuser);
					commonID.setInfo10(status);
				} else if (dosen != null || (tbmuser != null && tbmuser.getDosen() != null)) {

					if (dosen == null) {
						dosen = tbmuser.getDosen();
					}

					String ket = "";
					Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							pertemuan.retreiveAbsensiId(dosen.getId()));
					if (statusabsensi == null) {
						statusabsensi = ConstantValues.BELUM_ABSEN;
					}

					if (status.equals("Sukses")) {
						statusabsensi = ConstantValues.MASUK;
						ket = (ket.isEmpty() ? keterangan : keterangan + ";") + ket;
					} else {
						ket = (ket.isEmpty() ? status : status + ";") + ket;
					}

					if (lat != null && lng != null) {
						ket += ", lokasi absen " + map + " ";
					}

					pertemuan.populate(dosen.getId(), statusabsensi, ket, null,
							Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()), pertemuan.getWaktuSelesai(),
							"Dosen");
					session.getTransaction().begin();
					session.update(pertemuan);
					session.getTransaction().commit();

					commonID = convert(mahasiswa, siswa, tbmuser);
					commonID.setInfo10(status);
				} else if (guru != null || (tbmuser != null && tbmuser.getGuru() != null)) {

					if (guru == null) {
						guru = tbmuser.getGuru();
					}

					String ket = "";
					Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
							pertemuan.retreiveAbsensiId(guru.getId()));
					if (statusabsensi == null) {
						statusabsensi = ConstantValues.BELUM_ABSEN;
					}

					if (status.equals("Sukses")) {
						statusabsensi = ConstantValues.MASUK;
						ket = (ket.isEmpty() ? keterangan : keterangan + ";") + ket;
					} else {
						ket = (ket.isEmpty() ? status : status + ";") + ket;
					}

					if (lat != null && lng != null) {
						ket += ", lokasi absen " + map + " ";
					}

					pertemuan.populate(guru.getId(), statusabsensi, ket, null,
							Common.timeFormat2.get().format(ais.ui.util.WaktuUtil.getDate()), pertemuan.getWaktuSelesai(),
							"Guru");
					session.getTransaction().begin();
					session.update(pertemuan);
					session.getTransaction().commit();

					commonID = convert(mahasiswa, siswa, tbmuser);
					commonID.setInfo10(status);
				} else {
					commonID.setInfo1("Gagal");
					commonID.setInfo10("Gagal");
				}

			} else {
				commonID.setInfo1("Gagal");
				commonID.setInfo10("Gagal");
			}

			HibernateUtil.closeSession();
			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@GET
	@Path("simpan_absen/{id}/{data}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Endpoint absensi online tanpa koordinat GPS; mendelegasikan ke {@link #doAbsen} dengan
	 * keterangan waktu otomatis. Tidak memeriksa autentikasi.
	 *
	 * @param id   id {@link Pertemuan} yang diabsen
	 * @param data data absensi (kode/token peserta)
	 * @return status hasil absensi
	 */
	public CommonID simpanAbsen(@PathParam("id") String id, @PathParam("data") String data) {
		return doAbsen(id, data, null, null,
				"Absensi online sukses pada " + Common.dateFormat5.get().format(WaktuUtil.getDate()));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@GET
	@Path("ambil_data/{token}/{clazz}/{mulai}/{banyak}")
	@Produces({ MediaType.TEXT_PLAIN })
	/**
	 * DINONAKTIFKAN (2026-09-01) — sebelumnya mengambil data generik dari kelas entitas Hibernate
	 * MANA PUN yang namanya diberikan lewat URL ({@code Class.forName(clazz)}), dipaging
	 * {@code mulai}/{@code banyak}, dan diserialisasi ke JSON, TANPA memvalidasi parameter
	 * {@code token} sama sekali — siapa pun dapat mengekspos tabel entitas apa pun (mis.
	 * {@link Tbmuser}, berisi hash password seluruh pengguna) tanpa login. Method ini sekarang
	 * SELALU melempar {@link NotFoundException} tanpa memuat kelas atau menjalankan query apa
	 * pun, menutup total celah kebocoran data ini.
	 *
	 * <p>
	 * Untuk mengaktifkan kembali dengan aman, implementasi pengganti perlu: (1) memvalidasi
	 * {@code token} terhadap sesi aktif (bandingkan ke kolom {@code token} pada
	 * {@link Mahasiswa}/{@link Siswa}/{@link ais.database.model.sisdes.Penduduk}/{@link Tbmuser},
	 * diisi oleh {@link #getMasukToken(String, String, String)} saat login), DAN (2) membatasi
	 * {@code clazz} ke daftar allow-list kelas entitas yang memang dimaksudkan untuk diakses lewat
	 * endpoint mobile ini — daftar tersebut tidak dapat disimpulkan dari kode yang ada dan perlu
	 * ditentukan oleh pemilik aplikasi.
	 * </p>
	 *
	 * @param username nama parameter path adalah {@code token}; tidak lagi dipakai (endpoint nonaktif)
	 * @param clazz    tidak lagi dipakai (endpoint nonaktif)
	 * @param mulai    tidak lagi dipakai (endpoint nonaktif)
	 * @param banyak   tidak lagi dipakai (endpoint nonaktif)
	 * @return tidak pernah kembali secara normal — selalu melempar {@link NotFoundException}
	 * @throws Exception selalu {@link NotFoundException} ("Endpoint dinonaktifkan sementara...")
	 */
	public String getAmbilData(@PathParam("token") String username, @PathParam("clazz") String clazz,
			@PathParam("mulai") String mulai, @PathParam("banyak") String banyak) throws Exception {
		// DINONAKTIFKAN SEMENTARA (2026-09-01): endpoint ini sebelumnya memuat kelas Hibernate
		// APA SAJA yang diminta lewat parameter `clazz` (Class.forName + Criteria) dan
		// mengekspos seluruh baris tabelnya sebagai JSON, TANPA memvalidasi parameter `token`
		// sama sekali — kebocoran data tanpa autentikasi (mis. Tbmuser berisi hash password
		// seluruh pengguna). Ditutup total alih-alih ditambal parsial karena menutup celah ini
		// dengan benar butuh (a) validasi token terhadap sesi aktif dan (b) daftar allow-list
		// kelas entitas yang boleh diambil lewat endpoint ini — daftar itu tidak diketahui dari
		// pembacaan kode saja dan perlu ditentukan oleh pemilik aplikasi/API mobile sebelum
		// endpoint ini diaktifkan kembali dengan aman.
		throw new NotFoundException(
				"Endpoint dinonaktifkan sementara karena celah keamanan (kebocoran data tanpa autentikasi)");
	}

	@GET
	@Path("masuk/{username}/{password}/")
	@Produces({ MediaType.APPLICATION_JSON })
	/** Login tanpa token perangkat; mendelegasikan ke {@link #getMasukToken(String, String, String)} dengan token {@code null}. */
	public CommonID getMasuk(@PathParam("username") String username, @PathParam("password") String password)
			throws Exception {
		return getMasukToken(username, password, null);
	}

	@GET
	@Path("masuk_token/{username}/{password}/{token}")
	@Produces({ MediaType.APPLICATION_JSON })
	/**
	 * Login mobile universal: mencoba mencocokkan {@code username}/{@code password} (password
	 * dienkripsi DES sebelum dibandingkan) secara berurutan sebagai {@link Mahasiswa} (NIM), lalu
	 * {@link Siswa} (nomor induk), lalu {@link Penduduk} (kode), lalu {@link Tbmuser} (userId) — yang
	 * pertama cocok dipakai. Mengembalikan daftar menu mobile dinamis dalam {@link CommonID} sesuai
	 * jenis akun yang berhasil login (mis. menu KRS hanya muncul untuk mahasiswa). Parameter
	 * {@code token} diterima untuk keperluan token perangkat (push notification dsb.).
	 *
	 * @param username NIM/nomor induk/kode/userId pengguna
	 * @param password password akun (dikirim polos via path URL, dienkripsi DES sebelum dibandingkan)
	 * @param token    token perangkat untuk notifikasi, boleh {@code null}
	 * @return ringkasan hasil login berisi daftar menu mobile dalam field JSON
	 * @throws Exception diteruskan apa adanya dari kegagalan query/pembentukan JSON
	 */
	public CommonID getMasukToken(@PathParam("username") String username, @PathParam("password") String password,
			@PathParam("token") String token) throws Exception {

		Session session = HibernateUtil.currentNativeSession();
		String mypassword = Common.desEncrypter.get().encrypt(password);
		Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("nim", username)).add(Restrictions.eq("pass", mypassword)).setMaxResults(1),
				Mahasiswa.class);
		Siswa siswa = null;
		Tbmuser tbmuser = null;
		Penduduk penduduk = null;
		if (mahasiswa == null) {
			siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
					.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
					.add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorInduk", username))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("pass", mypassword)).setMaxResults(1), Siswa.class);
		}

		if (mahasiswa == null && siswa == null) {
			penduduk = (Penduduk) ConstantValues
					.simpleObject(session.createCriteria(Penduduk.class).add(Restrictions.isNotNull("nama"))
							.add(Restrictions.ne("nama", "")).add(Restrictions.eq("kode", username))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("pass", mypassword)).setMaxResults(1), Penduduk.class);
		}

		if (mahasiswa == null && siswa == null && penduduk == null) {
			tbmuser = (Tbmuser) ConstantValues.simpleObject(session.createCriteria(Tbmuser.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("userId", username)).add(Restrictions.eq("userPassword", mypassword))
					.setMaxResults(1), Tbmuser.class);
		}

		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("kode", "1");
		jsonObject.put("nama", "Pengumuman");
		jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/tampilan_pengumuman_akademis.zul?user="
				+ URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/announcement-icon.png");
		jsonArray.put(jsonObject);

		jsonObject = new JSONObject();
		jsonObject.put("kode", "2");
		jsonObject.put("nama", "Kalender");
		jsonObject.put("url",
				Common.getRequestHostWithProtocol() + "/common/mobile/kalender_akademik_mahasiswa.zul?user="
						+ URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/Calendar-icon_baru.png");
		jsonArray.put(jsonObject);

		jsonObject = new JSONObject();
		jsonObject.put("kode", "3");
		jsonObject.put("nama", "Jadwal");
		jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/mobile/penjadwalan_dosen.zul?user="
				+ URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/Calendar-icon_jadwal.png");
		jsonArray.put(jsonObject);

		if (mahasiswa != null) {
			jsonObject = new JSONObject();
			jsonObject.put("kode", "30");
			jsonObject.put("nama", "KRS");
			jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/mobile/krs.zul?user="
					+ URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
			jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/absensi_pmb.png");
			jsonArray.put(jsonObject);
		}

		jsonObject = new JSONObject();
		jsonObject.put("kode", "40");
		jsonObject.put("nama", "E-Learning");
		jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/mobile/e_learning.zul?user="
				+ URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/Tutorial-icon.png");
		jsonArray.put(jsonObject);

		jsonObject = new JSONObject();
		jsonObject.put("kode", "50");
		jsonObject.put("nama", "Nilai");
		jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/mobile/penilaian.zul?user="
				+ URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/Graduate-male-icon.png");
		jsonArray.put(jsonObject);

		jsonObject = new JSONObject();
		jsonObject.put("kode", "60");
		jsonObject.put("nama", "Kehadiran");
		jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/mobile/absensi.zul?user="
				+ URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/G12-Book-2-icon.png");
		jsonArray.put(jsonObject);

		//
		// jsonObject = new JSONObject();
		// jsonObject.put("kode", "100");
		// jsonObject.put("nama", "Pustaka");
		// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
		// "/img/books-icon.png");
		// jsonArray.put(jsonObject);
		//
		// jsonObject = new JSONObject();
		// jsonObject.put("kode", "101");
		// jsonObject.put("nama", "Buku");
		// jsonObject.put("parent", "100");
		// jsonObject.put("url", Common.getRequestHostWithProtocol() +
		// "/common/tampilan_pengumuman_akademis.zul?user="
		// + URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
		// "/img/book-icon_satu.png");
		// jsonArray.put(jsonObject);
		//
		// jsonObject = new JSONObject();
		// jsonObject.put("kode", "102");
		// jsonObject.put("nama", "Peminjaman");
		// jsonObject.put("parent", "100");
		// jsonObject.put("url", Common.getRequestHostWithProtocol() +
		// "/common/tampilan_pengumuman_akademis.zul?user="
		// + URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
		// "/img/out-icon.png");
		// jsonArray.put(jsonObject);
		//
		// jsonObject = new JSONObject();
		// jsonObject.put("kode", "103");
		// jsonObject.put("nama", "Pengembalian");
		// jsonObject.put("parent", "100");
		// jsonObject.put("url", Common.getRequestHostWithProtocol() +
		// "/common/tampilan_pengumuman_akademis.zul?user="
		// + URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
		// "/img/Business-Multiple-Input-icon.png");
		// jsonArray.put(jsonObject);

		// jsonObject = new JSONObject();
		// jsonObject.put("kode", "102");
		// jsonObject.put("nama", "Skripsi");
		// jsonObject.put("parent", "100");
		// jsonObject.put("url", Common.getRequestHostWithProtocol() +
		// "/common/tampilan_pengumuman_akademis.zul?user="
		// + URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
		// "/img/book-icon_skripsi.png");
		// jsonArray.put(jsonObject);
		//
		// jsonObject = new JSONObject();
		// jsonObject.put("kode", "103");
		// jsonObject.put("nama", "Thesis");
		// jsonObject.put("parent", "100");
		// jsonObject.put("url", Common.getRequestHostWithProtocol() +
		// "/common/tampilan_pengumuman_akademis.zul?user="
		// + URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
		// "/img/Client-icon.png");
		// jsonArray.put(jsonObject);
		//
		// jsonObject = new JSONObject();
		// jsonObject.put("kode", "104");
		// jsonObject.put("nama", "Jurnal");
		// jsonObject.put("parent", "100");
		// jsonObject.put("url", Common.getRequestHostWithProtocol() +
		// "/common/tampilan_pengumuman_akademis.zul?user="
		// + URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
		// "/img/Client-icon.png");
		// jsonArray.put(jsonObject);

		if (penduduk != null) {

			if (token != null && !token.trim().isEmpty()) {
				penduduk.setToken(token);
				session.getTransaction().begin();
				Common.refreshUpdate(session, penduduk);
				session.getTransaction().commit();
			} else if (penduduk.getToken() == null || penduduk.getToken().trim().isEmpty()) {
				token = Common.getGeneratedBarCode(30);
				penduduk.setToken(token);
				session.getTransaction().begin();
				Common.refreshUpdate(session, penduduk);
				session.getTransaction().commit();
			} else {
				token = penduduk.getToken();
			}

		} else if (siswa != null) {
			if (token != null && !token.trim().isEmpty()) {
				siswa.setToken(token);
				session.getTransaction().begin();
				Common.refreshUpdate(session, siswa);
				session.getTransaction().commit();
			} else if (siswa.getToken() == null || siswa.getToken().trim().isEmpty()) {
				token = Common.getGeneratedBarCode(30);
				siswa.setToken(token);
				session.getTransaction().begin();
				Common.refreshUpdate(session, siswa);
				session.getTransaction().commit();
			} else {
				token = siswa.getToken();
			}
		} else if (mahasiswa != null) {
			if (token != null && !token.trim().isEmpty()) {
				mahasiswa.setToken(token);
				session.getTransaction().begin();
				Common.refreshUpdate(session, mahasiswa);
				session.getTransaction().commit();
			} else if (mahasiswa.getToken() == null || mahasiswa.getToken().trim().isEmpty()) {
				token = Common.getGeneratedBarCode(30);
				mahasiswa.setToken(token);
				session.getTransaction().begin();
				Common.refreshUpdate(session, mahasiswa);
				session.getTransaction().commit();
			} else {
				token = mahasiswa.getToken();
			}
		} else if (tbmuser != null) {
			if (token != null && !token.trim().isEmpty()) {
				tbmuser.setToken(token);
				session.getTransaction().begin();
				Common.refreshUpdate(session, tbmuser);
				session.getTransaction().commit();
			} else if (tbmuser.getToken() == null || tbmuser.getToken().trim().isEmpty()) {
				token = Common.getGeneratedBarCode(30);
				tbmuser.setToken(token);
				session.getTransaction().begin();
				Common.refreshUpdate(session, tbmuser);
				session.getTransaction().commit();
			} else {
				token = tbmuser.getToken();
			}
		}

		HibernateUtil.closeSession();

		if (siswa != null) {

			// jsonObject = new JSONObject();
			// jsonObject.put("kode", "7");
			// jsonObject.put("nama", "Pembayaran Siswa");
			//// jsonObject.put("parent", "7");
			// jsonObject.put("url", Common.getRequestHostWithProtocol() +
			// "/common/mobile/pembayaran_online.zul?user="
			// + URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
			// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
			// "/img/online-icon.png");
			// jsonArray.put(jsonObject);

			jsonObject = new JSONObject();
			jsonObject.put("kode", "7");
			jsonObject.put("nama", "Pembayaran");
			jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/Sales-by-Payment-Method-rep-icon.png");
			jsonArray.put(jsonObject);

			// jsonObject = new JSONObject();
			// jsonObject.put("kode", "8");
			// jsonObject.put("nama", "Tagihan");
			// jsonObject.put("parent", "7");
			// jsonObject.put("url", Common.getRequestHostWithProtocol() +
			// "/common/mobile/tagihan.zul?siswa="
			// + siswa.getId() + "&is_mobile=true");
			// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
			// "/img/invoice-icon.png");
			// jsonArray.put(jsonObject);

			// jsonObject = new JSONObject();
			// jsonObject.put("kode", "9");
			// jsonObject.put("nama", "Pembayaran Siswa");
			// jsonObject.put("parent", "7");
			// jsonObject.put("url", Common.getRequestHostWithProtocol() +
			// "/common/mobile/pembayaran_siswa.zul?siswa="
			// + siswa.getId() + "&is_mobile=true");
			// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
			// "/img/sales-report-icon.png");
			// jsonArray.put(jsonObject);

			jsonObject = new JSONObject();
			jsonObject.put("kode", "10");
			jsonObject.put("nama", "Pembayaran Online");
			jsonObject.put("parent", "7");
			jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/mobile/pembayaran_online.zul?siswa="
					+ siswa.getId() + "&is_mobile=true");
			jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/online-icon.png");
			jsonArray.put(jsonObject);

			jsonObject = new JSONObject();
			jsonObject.put("kode", "11");
			jsonObject.put("nama", "Deposit Masuk");
			jsonObject.put("parent", "7");
			jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/mobile/deposit_siswa.zul?siswa="
					+ siswa.getId() + "&is_mobile=true");
			jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/safe-icon.png");
			jsonArray.put(jsonObject);

			// jsonObject = new JSONObject();
			// jsonObject.put("kode", "14");
			// jsonObject.put("nama", "Belanja Siswa");
			// jsonObject.put("parent", "7");
			// jsonObject.put("url", Common.getRequestHostWithProtocol() +
			// "/common/mobile/pembelian.zul?siswa="
			// + siswa.getId() + "&is_mobile=true");
			// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
			// "/img/sales-report-icon.png");
			// jsonArray.put(jsonObject);

			jsonObject = new JSONObject();
			jsonObject.put("kode", "50");
			jsonObject.put("nama", "Laporan");
			jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/Sales-by-Payment-Method-rep-icon.png");
			jsonArray.put(jsonObject);

			jsonObject = new JSONObject();
			jsonObject.put("kode", "51");
			jsonObject.put("nama", "Laporan Pembayaran Siswa");
			jsonObject.put("parent", "50");
			jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/mobile/laporan_pembayaran.zul?siswa="
					+ siswa.getId() + "&is_mobile=true");
			jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/sales-report-icon.png");
			jsonArray.put(jsonObject);

			jsonObject = new JSONObject();
			jsonObject.put("kode", "52");
			jsonObject.put("nama", "Laporan Belanja Siswa");
			jsonObject.put("parent", "50");
			jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/mobile/laporan_pembelian.zul?siswa="
					+ siswa.getId() + "&is_mobile=true");
			jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/sales-report-icon.png");
			jsonArray.put(jsonObject);

			jsonObject = new JSONObject();
			jsonObject.put("kode", "53");
			jsonObject.put("nama", "Laporan Saldo Siswa");
			jsonObject.put("parent", "50");
			jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/mobile/laporan_saldo.zul?siswa="
					+ siswa.getId() + "&is_mobile=true");
			jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/safe-icon.png");
			jsonArray.put(jsonObject);

			jsonObject = new JSONObject();
			jsonObject.put("kode", "54");
			jsonObject.put("nama", "Laporan Deposit Masuk");
			jsonObject.put("parent", "50");
			jsonObject.put("url", Common.getRequestHostWithProtocol()
					+ "/common/mobile/laporan_deposit_siswa.zul?siswa=" + siswa.getId() + "&is_mobile=true");
			jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/Sales-by-Payment-Method-rep-icon.png");
			jsonArray.put(jsonObject);

			jsonObject = new JSONObject();
			jsonObject.put("kode", "56");
			jsonObject.put("nama", "Laporan Tagihan Siswa");
			jsonObject.put("parent", "50");
			jsonObject.put("url", Common.getRequestHostWithProtocol() + "/common/mobile/laporan_tunggakan.zul?siswa="
					+ siswa.getId() + "&is_mobile=true");
			jsonObject.put("icon", Common.getRequestHostWithProtocol() + "/img/invoice-icon.png");
			jsonArray.put(jsonObject);

			// jsonObject = new JSONObject();
			// jsonObject.put("kode", "12");
			// jsonObject.put("nama", "Laporan Keuangan Siswa");
			// jsonObject.put("parent", "7");
			// jsonObject.put("url", Common.getRequestHostWithProtocol() +
			// "/common/mobile/pembayaran_online.zul?user="
			// + URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
			// jsonObject.put("icon", Common.getRequestHostWithProtocol() +
			// "/img/sales-report-icon.png");
			// jsonArray.put(jsonObject);
		}

		CommonID commonID = convert(mahasiswa, siswa, tbmuser);
		commonID.setInfo10(jsonArray.toString());
		commonID.setInfo12(Common.getRequestHostWithProtocol() + "/common/tampilan_pengumuman_akademis.zul?user="
				+ URLEncoder.encode(username, "UTF-8") + "&is_mobile=true");
		commonID.setInfo14(token);
		try {
			commonID.setInfo15(mahasiswa != null ? "mhs" : siswa != null ? "siswa" : tbmuser.hakAkses().getRoleId());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/resources/ELearningResource.java:884");
		}
		return commonID;
	}

	public static CommonID convert(Mahasiswa mahasiswa, Siswa siswa, Tbmuser tbmuser) {
		CommonID commonID = new CommonID();
		commonID.setInfo1("Gagal");
		if (mahasiswa != null) {

			commonID.setId(mahasiswa.getId());
			commonID.setInfo1(mahasiswa.getNama());
			commonID.setInfo2(mahasiswa.getNim());
			commonID.setInfo3(mahasiswa.getJurusan().getNama());
			commonID.setInfo4(mahasiswa.getJurusan().getFakultas().getNama());
			commonID.setInfo5(mahasiswa.getEmail());
			try {
				commonID.setInfo6(CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			try {
				int semester = mahasiswa.currentSemester();
				boolean bayar = Common.checkStatusPembayaranMahasiswa(semester, mahasiswa.currentTahapan(), mahasiswa,
						false, false);

				commonID.setInfo7(Common.getCurrentTahunAkademik());
				commonID.setInfo8(semester + "");
				commonID.setInfo9(bayar ? "Telah Membayar" : "Belum Membayar");

			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

		}

		else if (siswa != null) {

			commonID.setId(siswa.getId());
			commonID.setInfo1(siswa.getNama());
			commonID.setInfo2(siswa.getNim());
			commonID.setInfo3(siswa.getSekolah().getNama());
			commonID.setInfo4(siswa.getYayasan().getNama());
			commonID.setInfo5(siswa.getAlamatEmail());
			try {
				commonID.setInfo6(CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa)));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			try {

				commonID.setInfo7(siswa.getKelas() == null ? "Tidak ada kelas" : siswa.getKelas().getNama());
				commonID.setInfo8(siswa.getAsrama() == null ? "Tidak ada asrama" : siswa.getAsrama().getNama());
				commonID.setInfo9(siswa.getPanggilan());

			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

		} else if (tbmuser != null) {
			commonID.setInfo1(tbmuser.getUserId());
			commonID.setInfo2(tbmuser.getUserNama());
			commonID.setInfo3(tbmuser.hakAkses() == null ? "" : tbmuser.hakAkses().getNama());
			commonID.setInfo4(tbmuser.ambilSatuanKerja() == null ? "" : tbmuser.ambilSatuanKerja().getNama());
			commonID.setInfo5(tbmuser.getEmail());
			try {
				commonID.setInfo6(CommonMedia.getUrlFotoPengguna(tbmuser));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			try {

				commonID.setInfo7(tbmuser.ambilFakultas() == null
						? (tbmuser.ambilYayasan() == null ? "" : tbmuser.ambilYayasan().getNama())
						: tbmuser.ambilFakultas().getNama());
				commonID.setInfo7(tbmuser.ambilJurusan() == null
						? (tbmuser.ambilJurusan() == null ? "" : tbmuser.ambilJurusan().getNama())
						: tbmuser.ambilJurusan().getNama());

				commonID.setInfo9(tbmuser.getBahasa());

			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

		}

		else {
			throw new NotFoundException("Proses gagal dilakukan");
		}

		return commonID;
	}

}
