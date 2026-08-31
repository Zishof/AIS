package ais.action.master.resources;

import java.net.URLDecoder;
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
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.spi.resource.Singleton;

import ais.action.master.resources.model.CommonID;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPayroll;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.Produk;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * Endpoint REST (JAX-RS/Jersey) untuk perangkat POS (point of sale) kios sekolah dan mesin
 * absensi sidik jari (fingerprint): mencatat absensi masuk/pulang lewat {@link #absen}, mencatat
 * transaksi pembelian kios lewat {@link #update}, serta menyediakan pencarian produk dan data
 * siswa (termasuk saldo deposit) untuk ditampilkan di kios.
 *
 * <p>
 * <b>Catatan keamanan</b> — TIDAK SEPERTI keluarga {@code *Resource} lain di paket ini (yang
 * mewajibkan {@code username}/{@code password}), SELURUH endpoint di kelas ini tidak memiliki
 * pemeriksaan autentikasi/otorisasi apa pun: siapa saja yang dapat menjangkau path {@code /pos/*}
 * dapat mencatat absensi orang lain ({@link #absen}, memanipulasi jam masuk/pulang lewat
 * parameter {@code state}), mencatat transaksi pembelian ({@link #update}, termasuk mengurangi
 * saldo deposit siswa manapun via {@code kode_member}), serta membaca data pribadi siswa
 * (nama orang tua, sisa deposit, foto, nomor induk) tanpa verifikasi identitas pemanggil sama
 * sekali. Ini adalah keputusan desain lama (diasumsikan hanya dapat dijangkau dari jaringan
 * internal kios/mesin absensi tepercaya), dilaporkan di sini tanpa diperbaiki sesuai batasan
 * tugas dokumentasi ini.
 * </p>
 */
@Path("/pos")
@Singleton

public class PosResource {

	/** @return waktu sistem server saat ini (epoch milidetik), dipakai klien untuk sinkronisasi jam. */
	public long getSystemTime() {
		return System.currentTimeMillis();
	}

	/** Endpoint uji konektivitas: mengembalikan {@code nama} yang dikirim tanpa memproses data apa pun. */
	@GET
	@Path("test/{nama}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID getMahasiswa(@PathParam("nama") String nama) {
		CommonID commonID = new CommonID();
		commonID.setInfo1(nama);
		return commonID;
	}

	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private static String safeString(String value) {
		return value == null ? "" : value;
	}

	private static void rollbackQuietly(Transaction transaction) {
		try {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/PosResource.java:75");
		}
	}

	private static void closeCurrentNativeSession(Session session) {
		try {
			if (session != null) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/PosResource.java:84");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/PosResource.java:88");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/PosResource.java:92");
				}
			}
		} finally {
			try {
				HibernateUtil.closeSession();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/PosResource.java:98");
			}
		}
	}

	/** @return nama untuk ditampilkan pada respons absensi: nama pegawai, atau "NIM-nama" mahasiswa, atau "NISN-nama" siswa (prioritas dalam urutan itu). */
	private static String getNamaAbsen(Pegawai pegawai, Mahasiswa mahasiswa, Siswa siswa) {
		try {
			if (pegawai != null) {
				return safeString(pegawai.getNama());
			}
			if (mahasiswa != null) {
				return safeString(mahasiswa.getNim()) + "-" + safeString(mahasiswa.getNama());
			}
			if (siswa != null) {
				return safeString(siswa.getNomorIndukNasional()) + "-" + safeString(siswa.getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/PosResource.java:114");
		}
		return "";
	}

	/** Menyusun respons standar {@link #absen}: id finger, waktu, nama, status absensi (pesan {@code info} bila diisi, atau status/label pertemuan), serta jam masuk/pulang terformat. */
	private static CommonID buildAbsenResponse(String idFinger, String waktu, String info, Pegawai pegawai,
			Mahasiswa mahasiswa, Siswa siswa, StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian,
			Pertemuan pertemuanUtama) {
		CommonID commonID = new CommonID();
		commonID.setInfo1(safeTrim(idFinger));
		commonID.setInfo2(safeTrim(waktu));
		commonID.setInfo3(getNamaAbsen(pegawai, mahasiswa, siswa));

		Statusabsensi statusabsensi = statuskehadiranKaryawanHarian == null ? ConstantValues.BELUM_ABSEN
				: statuskehadiranKaryawanHarian.getStatusabsensi();
		if (statuskehadiranKaryawanHarian != null && ConstantValues.kehadiranHarusMulaiDanSampai) {
			if (statuskehadiranKaryawanHarian.getMasukjam() == null
					|| statuskehadiranKaryawanHarian.getPulangJam() == null) {
				statusabsensi = ConstantValues.BELUM_ABSEN;
			}
		}

		String statusText = info;
		if (statusText == null || statusText.trim().length() == 0) {
			statusText = statuskehadiranKaryawanHarian == null || statusabsensi == null
					? (pertemuanUtama == null || pertemuanUtama.getStatusPertemuan() == null ? ""
							: pertemuanUtama.getStatusPertemuan().getNama())
					: statusabsensi.getNama();
		}
		commonID.setInfo4(statusText);
		commonID.setInfo5(statuskehadiranKaryawanHarian == null
				|| statuskehadiranKaryawanHarian.ambilMasukjam() == null
						? (pertemuanUtama == null || pertemuanUtama.getStatusPertemuan() == null ? ""
								: pertemuanUtama.getWaktuMulai())
						: Common.timeFormat.get().format(statuskehadiranKaryawanHarian.ambilMasukjam()));
		commonID.setInfo6(statuskehadiranKaryawanHarian == null
				|| statuskehadiranKaryawanHarian.ambilPulangjam() == null
						? (pertemuanUtama == null || pertemuanUtama.getStatusPertemuan() == null ? ""
								: pertemuanUtama.getWaktuSelesai())
						: Common.timeFormat.get().format(statuskehadiranKaryawanHarian.ambilPulangjam()));
		return commonID;
	}

	/** @return {@code true} bila {@code tanggal} (dikurangi toleransi 2 jam) masih di masa depan relatif terhadap waktu server saat ini. */
	private static boolean isFutureAttendanceTime(Date tanggal) {
		try {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggal);
			calendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY) - 2);
			return calendar.getTime().after(WaktuUtil.getDate());
		} catch (Exception e) {
			return false;
		}
	}

	private static int minuteOfDay(Date date) {
		try {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(date);
			return (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE);
		} catch (Exception e) {
			return 0;
		}
	}

	private static String getHariName(Date tanggal) {
		try {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggal);
			int index = calendar.get(Calendar.DAY_OF_WEEK) - 1;
			if (Common.haris != null && index >= 0 && index < Common.haris.length) {
				return Common.haris[index];
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/PosResource.java:186");
		}
		return "";
	}

	/**
	 * Implementasi kanonik pencatatan absensi via mesin sidik jari, dipakai kedua overload REST
	 * {@code getAbsen}. Alur kerja: (1) validasi id/waktu tidak kosong dan waktu tidak di masa
	 * depan ({@link #isFutureAttendanceTime}); (2) cari identitas pemilik {@code id_finger} —
	 * berurutan: pegawai (lewat kolom idfinger pegawai/guru/dosen), lalu mahasiswa (idfinger atau
	 * NIM), lalu siswa (idfinger, NISN, atau nomor induk); (3) ambil/bentuk baris
	 * {@link StatuskehadiranKaryawanHarian} harian default untuk identitas dan tanggal tersebut;
	 * (4) tentukan jam masuk/pulang berdasarkan {@code state} ({@code "0"}=masuk hanya bila lebih
	 * awal dari yang tercatat, {@code "1"}=pulang hanya bila lebih akhir, lainnya=isi masuk dulu
	 * baru pulang) dan tentukan shift lewat {@code CommonPayroll#getDetailJenisShiftPegawai};
	 * (5) simpan baris kehadiran dalam transaksi, lalu simpan detail perhitungan lewat
	 * {@code CommonPayroll#simpanDetail} (kegagalan langkah ini tidak menggagalkan absensi).
	 * Setiap kegagalan validasi/pencarian mengembalikan respons dengan pesan galat spesifik,
	 * bukan melempar exception.
	 *
	 * @param id_finger id sidik jari/NIM/nomor induk yang dipindai
	 * @param waktu     waktu absensi (format {@code Common#dateFormat9})
	 * @param state     {@code "0"} untuk paksa catat sebagai masuk, {@code "1"} untuk pulang, atau {@code null}/lainnya untuk deteksi otomatis
	 * @return respons berisi status absensi (sukses dengan jam masuk/pulang, atau pesan gagal)
	 */
	public static CommonID absen(String id_finger, String waktu, String state) {
		Session session = null;
		Transaction transaction = null;
		Pegawai pegawai = null;
		StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = null;
		Siswa siswa = null;
		Mahasiswa mahasiswa = null;
		Pertemuan pertemuanUtama = null;
		String idFinger = safeTrim(id_finger);
		String waktuFinger = safeTrim(waktu);
		String stateFinger = safeTrim(state);

		try {
			if (idFinger.length() == 0) {
				return buildAbsenResponse(idFinger, waktuFinger, "Gagal, ID fingerprint kosong", pegawai, mahasiswa, siswa,
						statuskehadiranKaryawanHarian, pertemuanUtama);
			}
			if (waktuFinger.length() == 0) {
				return buildAbsenResponse(idFinger, waktuFinger, "Gagal, waktu absensi kosong", pegawai, mahasiswa, siswa,
						statuskehadiranKaryawanHarian, pertemuanUtama);
			}

			Date tanggal = Common.dateFormat9.get().parse(waktuFinger);
			if (isFutureAttendanceTime(tanggal)) {
				return buildAbsenResponse(idFinger, waktuFinger, "Gagal, waktu belum masuk", pegawai, mahasiswa, siswa,
						statuskehadiranKaryawanHarian, pertemuanUtama);
			}

			session = HibernateUtil.currentNativeSession();

			pegawai = (Pegawai) ConstantValues.simpleObject(session.createCriteria(Pegawai.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.createAlias("dosen", "dosen", Criteria.LEFT_JOIN).createAlias("guru", "guru", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.eq("idfinger", idFinger),
							Restrictions.or(Restrictions.eq("guru.idfinger", idFinger),
									Restrictions.eq("dosen.idfinger", idFinger))))
					.setMaxResults(1), Pegawai.class);

			if (pegawai == null) {
				mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("idfinger", idFinger)).setMaxResults(1), Mahasiswa.class);
			}

			if (mahasiswa == null) {
				mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("nim", idFinger)).setMaxResults(1), Mahasiswa.class);
			}

			if (mahasiswa == null) {
				siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
						.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
						.add(Restrictions.isNotNull("sekolah"))
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.add(Restrictions.eq("idfinger", idFinger)).setMaxResults(1), Siswa.class);
			}

			if (siswa == null) {
				siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
						.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
						.add(Restrictions.isNotNull("sekolah"))
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.add(Restrictions.eq("nomorIndukNasional", idFinger)).setMaxResults(1), Siswa.class);
			}

			if (siswa == null) {
				siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
						.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
						.add(Restrictions.isNotNull("sekolah"))
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.add(Restrictions.eq("nomorInduk", idFinger)).setMaxResults(1), Siswa.class);
			}

			if (pegawai == null && mahasiswa == null && siswa == null) {
				return buildAbsenResponse(idFinger, waktuFinger, "Gagal, ID fingerprint tidak ditemukan", pegawai,
						mahasiswa, siswa, statuskehadiranKaryawanHarian, pertemuanUtama);
			}

			statuskehadiranKaryawanHarian = CommonPayroll.getDefaultStatuskehadiranKaryawanHarian(tanggal, pegawai,
					mahasiswa, siswa, "", "", session, true);
			if (statuskehadiranKaryawanHarian == null) {
				return buildAbsenResponse(idFinger, waktuFinger, "Gagal, data absensi harian tidak dapat dibuat", pegawai,
						mahasiswa, siswa, statuskehadiranKaryawanHarian, pertemuanUtama);
			}

			statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.MASUK);
			Date msk = statuskehadiranKaryawanHarian.getMasukjamState();
			Date plg = statuskehadiranKaryawanHarian.getPulangJamState();

			if ("0".equalsIgnoreCase(stateFinger)) {
				if (msk == null || minuteOfDay(msk) > minuteOfDay(tanggal)) {
					statuskehadiranKaryawanHarian.setMasukjam(tanggal);
					statuskehadiranKaryawanHarian.setMasukjamManual(tanggal);
					statuskehadiranKaryawanHarian.setMasukjamState(tanggal);
				}
			} else if ("1".equalsIgnoreCase(stateFinger)) {
				if (plg == null || minuteOfDay(plg) < minuteOfDay(tanggal)) {
					statuskehadiranKaryawanHarian.setPulangJam(tanggal);
					statuskehadiranKaryawanHarian.setPulangJamManual(tanggal);
					statuskehadiranKaryawanHarian.setPulangJamState(tanggal);
				}
			} else if (statuskehadiranKaryawanHarian.getMasukjam() == null) {
				statuskehadiranKaryawanHarian.setMasukjam(tanggal);
			} else {
				statuskehadiranKaryawanHarian.setPulangJam(tanggal);
			}

			statuskehadiranKaryawanHarian.setDetailJenisShiftPegawai(CommonPayroll.getDetailJenisShiftPegawai(pegawai,
					mahasiswa, siswa, statuskehadiranKaryawanHarian.ambilMasukjam(),
					statuskehadiranKaryawanHarian.getTanggal(), getHariName(tanggal),
					statuskehadiranKaryawanHarian.getLiburNasional() != null));

			String catatanFingerprint = "Fingerprint " + Common.dateFormat5.get().format(tanggal) + ";" + stateFinger;
			String keterangan = safeString(statuskehadiranKaryawanHarian.getKeterangan());
			keterangan = (keterangan.trim().length() == 0 ? catatanFingerprint : catatanFingerprint + ";\n" + keterangan);
			statuskehadiranKaryawanHarian.setKeterangan(keterangan);

			transaction = session.beginTransaction();
			if (statuskehadiranKaryawanHarian.getId() == null) {
				session.save(statuskehadiranKaryawanHarian);
			} else {
				session.update(statuskehadiranKaryawanHarian);
			}
			transaction.commit();
			transaction = null;

			try {
				CommonPayroll.simpanDetail(session, statuskehadiranKaryawanHarian, true);
			} catch (Exception detailException) {
				Common.tampilErrorJikaAdmin(detailException);
			}

			return buildAbsenResponse(idFinger, waktuFinger, null, pegawai, mahasiswa, siswa,
					statuskehadiranKaryawanHarian, pertemuanUtama);
		} catch (Exception e) {
			rollbackQuietly(transaction);
			Common.tampilErrorJikaAdmin(e);
			return buildAbsenResponse(idFinger, waktuFinger, "Gagal, terjadi kesalahan internal: " + e.getMessage(),
					pegawai, mahasiswa, siswa, statuskehadiranKaryawanHarian, pertemuanUtama);
		} finally {
			closeCurrentNativeSession(session);
		}
	}

	/** Seperti {@link #absen(String, String, String)} dengan deteksi otomatis masuk/pulang ({@code state=null}). */
	@GET
	@Path("absen/{id_finger}/{waktu}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID getAbsen(@PathParam("id_finger") String id_finger, @PathParam("waktu") String waktu) {

		return PosResource.absen(id_finger, waktu, null);
	}

	/**
	 * Mencatat absensi masuk/pulang via mesin sidik jari. Tidak memvalidasi kredensial (lihat
	 * catatan keamanan di javadoc kelas).
	 *
	 * @param id_finger id sidik jari/NIM/nomor induk yang dipindai
	 * @param waktu     waktu absensi
	 * @param state     {@code "0"} untuk paksa masuk, {@code "1"} untuk paksa pulang, lainnya untuk otomatis
	 * @return respons status absensi, lihat {@link #absen(String, String, String)}
	 */
	@GET
	@Path("absen/{id_finger}/{waktu}/{state}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID getAbsen(@PathParam("id_finger") String id_finger, @PathParam("waktu") String waktu,
			@PathParam("state") String state) {

		return PosResource.absen(id_finger, waktu, state);
	}

	/**
	 * Mencatat (atau memperbarui, dicocokkan lewat kombinasi kode invoice + produk) satu baris
	 * transaksi pembelian {@link Pembelian} dari kios: mencari {@link Produk} berdasarkan id,
	 * mengaitkan ke {@link Siswa} bila {@code kode_member} cocok dengan nomor induk siswa, lalu
	 * menyimpan harga jual saat ini, kuantitas, kios asal, dan waktu transaksi. Tidak memvalidasi
	 * kredensial (lihat catatan keamanan di javadoc kelas).
	 *
	 * @param kode_invoice      kode invoice transaksi (URL-encoded, garis bawah dianggap kosong)
	 * @param produk_id         id produk yang dibeli
	 * @param jumlah            kuantitas yang dibeli
	 * @param diskon            nilai diskon (diterima tapi tidak dipakai pada implementasi saat ini)
	 * @param tanggal_dan_waktu waktu transaksi (format {@code Common#databaseDateFormat1})
	 * @param kode_member       nomor induk siswa pembeli, boleh kosong
	 * @param kode_kios         identitas kios asal transaksi
	 * @return respons dengan {@code info1="Sukses"}/{@code "Gagal"} sesuai hasil
	 * @throws NotFoundException bila terjadi kesalahan internal
	 */
	@GET
	@Path("kirim_transaksi/{kode_invoice}/{produk_id}/{jumlah}/{diskon}/{tanggal_dan_waktu}/{kode_member}/{kode_kios}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID update(@PathParam("kode_invoice") String kode_invoice, @PathParam("produk_id") String produk_id,
			@PathParam("jumlah") String jumlah, @PathParam("diskon") String diskon,
			@PathParam("tanggal_dan_waktu") String tanggal_dan_waktu, @PathParam("kode_member") String kode_member,
			@PathParam("kode_kios") String kode_kios) {

		Session session = HibernateUtil.currentNativeSession();
		try {
			CommonID commonID = new CommonID();
			kode_invoice = URLDecoder.decode(kode_invoice.replaceAll("_", ""), "UTF-8");
			produk_id = URLDecoder.decode(produk_id.replaceAll("_", ""), "UTF-8");
			jumlah = URLDecoder.decode(jumlah.replaceAll("_", ""), "UTF-8");
			diskon = URLDecoder.decode(diskon.replaceAll("_", ""), "UTF-8");
			tanggal_dan_waktu = URLDecoder.decode(tanggal_dan_waktu.replaceAll("_", ""), "UTF-8");
			kode_member = URLDecoder.decode(kode_member.replaceAll("_", ""), "UTF-8");
			kode_kios = URLDecoder.decode(kode_kios.replaceAll("_", ""), "UTF-8");

			System.out.println("kode_invoice = " + kode_invoice + ", produk_id = " + produk_id + ", jumlah => " + jumlah
					+ ", diskon => " + diskon + ", tanggal_dan_waktu => " + tanggal_dan_waktu + ", kode_member => "
					+ kode_member + ", kode_kios => " + kode_kios);

			Produk produk = (Produk) session.createCriteria(Produk.class)
					.add(Restrictions.idEq(Long.parseLong(produk_id.trim()))).uniqueResult();

			if (produk != null) {

				Long siswaId = (Long) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
						.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
						.add(Restrictions.eq("nomorInduk", kode_member)).setProjection(Projections.property("id"))
						.setMaxResults(1).uniqueResult();

				Pembelian pembelian = (Pembelian) session.createCriteria(Pembelian.class)
						.add(Restrictions.eq("kode", kode_invoice)).add(Restrictions.eq("produk", produk))
						.setMaxResults(1).uniqueResult();
				if (pembelian == null) {
					pembelian = new Pembelian();
				}
				pembelian.setProduk(produk);
				pembelian.setKode(kode_invoice);
				pembelian.setHargaJual(produk.getHargaJual());
				pembelian.setQty(Double.parseDouble(jumlah));
				pembelian.setKios(kode_kios);
				pembelian.setMember(kode_member);

				if (siswaId != null) {
					pembelian.setSiswa(new Siswa(siswaId));
				}

				try {
					pembelian.setWaktu(Common.databaseDateFormat1.get().parse(tanggal_dan_waktu));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/resources/PosResource.java:405");

				}

				session.getTransaction().begin();
				session.saveOrUpdate(pembelian);
				session.getTransaction().commit();

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
	 * Mencari produk aktif berdasarkan kode/nama (dipaginasi), dikembalikan sebagai JSON array
	 * teks pada {@code info1}. Tidak memvalidasi kredensial.
	 *
	 * @param nama   kata kunci pencarian kode/nama produk (URL-encoded), atau kosong untuk semua
	 * @param mulai  offset baris awal (paginasi)
	 * @param banyak jumlah baris maksimal (default 100 bila tidak valid)
	 * @return respons dengan {@code info1} berisi JSON array objek {id, nama, kode, harga}
	 * @throws NotFoundException bila terjadi kesalahan internal
	 */
	@GET
	@Path("produk/{nama}/{mulai}/{banyak}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID getProduk(@PathParam("nama") String nama, @PathParam("mulai") String mulai,
			@PathParam("banyak") String banyak) {
		Session session = HibernateUtil.currentNativeSession();
		try {

			System.out.println("banyak = " + banyak + ", mulai = " + mulai + ", nama => " + nama);

			banyak = URLDecoder.decode(banyak.replaceAll("_", ""), "UTF-8");
			mulai = URLDecoder.decode(mulai.replaceAll("_", ""), "UTF-8");
			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");

			int bnyk = 100;
			try {
				bnyk = Integer.parseInt(banyak.trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			int mul = 0;
			try {
				mul = Integer.parseInt(mulai.trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			@SuppressWarnings("unchecked")
			List<Produk> produks = session.createCriteria(Produk.class).addOrder(Order.asc("id"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("kode", nama, MatchMode.ANYWHERE),
									Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)))
					.setFirstResult(mul).setMaxResults(bnyk).list();

			System.out.println(
					"produks size => " + produks.size() + ", bnyk = " + bnyk + ", mul = " + mul + ", nama => " + nama);

			JSONArray array = new JSONArray();
			for (Produk produk : produks) {

				JSONObject json = new JSONObject();
				json.put("id", produk.getId());
				json.put("nama", produk.getNama());
				json.put("kode", produk.getKode());
				json.put("harga", produk.getHargaJual());

				array.put(json);
			}

			CommonID commonID = new CommonID();
			commonID.setInfo1(array.toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	/**
	 * @param nama kata kunci pencarian kode/nama produk (URL-encoded), atau kosong untuk semua
	 * @return respons dengan {@code info1} berisi jumlah produk aktif yang cocok
	 * @throws NotFoundException bila terjadi kesalahan internal
	 */
	@GET
	@Path("jumlah_produk/{nama}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID getProduk(@PathParam("nama") String nama) {
		Session session = HibernateUtil.currentNativeSession();
		try {

			System.out.println("nama => " + nama);

			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");

			Number jml = (Number) session.createCriteria(Produk.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("kode", nama, MatchMode.ANYWHERE),
									Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)))
					.setProjection(Projections.rowCount()).uniqueResult();

			System.out.println("produks size => " + jml + ", nama => " + nama);

			CommonID commonID = new CommonID();
			commonID.setInfo1((jml == null ? 0 : jml.intValue()) + "");

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	/**
	 * @param nama kata kunci pencarian nomor induk/nama siswa (URL-encoded), atau kosong untuk semua
	 * @return respons dengan {@code info1} berisi jumlah siswa aktif yang cocok
	 * @throws NotFoundException bila terjadi kesalahan internal
	 */
	@GET
	@Path("jumlah_siswa/{nama}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID getJumlahSiswa(@PathParam("nama") String nama) {
		Session session = HibernateUtil.currentNativeSession();
		try {

			System.out.println("nama => " + nama);

			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");

			Number jml = (Number) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
					.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("nomorInduk", nama, MatchMode.ANYWHERE),
									Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)))
					.setProjection(Projections.rowCount()).uniqueResult();

			System.out.println("siswa size => " + jml + ", nama => " + nama);

			CommonID commonID = new CommonID();
			commonID.setInfo1((jml == null ? 0 : jml.intValue()) + "");

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	/**
	 * Mencari siswa aktif berdasarkan nomor induk/nama (dipaginasi), dikembalikan sebagai JSON
	 * array teks pada {@code info1} — termasuk sisa deposit dan nama orang tua per siswa. Tidak
	 * memvalidasi kredensial (lihat catatan keamanan di javadoc kelas: data pribadi ini terbuka
	 * tanpa autentikasi).
	 *
	 * @param nama   kata kunci pencarian nomor induk/nama siswa (URL-encoded), atau kosong untuk semua
	 * @param mulai  offset baris awal (paginasi)
	 * @param banyak jumlah baris maksimal (default 100 bila tidak valid)
	 * @return respons dengan {@code info1} berisi JSON array objek {id, nama, foto, nomorInduk, sisaDeposit, sekolah, ayah, ibu, tahunMasuk}
	 * @throws NotFoundException bila terjadi kesalahan internal
	 */
	@GET
	@Path("siswa/{nama}/{mulai}/{banyak}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID getSiswa(@PathParam("nama") String nama, @PathParam("mulai") String mulai,
			@PathParam("banyak") String banyak) {
		Session session = HibernateUtil.currentNativeSession();
		try {

			System.out.println("banyak = " + banyak + ", mulai = " + mulai + ", nama => " + nama);

			banyak = URLDecoder.decode(banyak.replaceAll("_", ""), "UTF-8");
			mulai = URLDecoder.decode(mulai.replaceAll("_", ""), "UTF-8");
			nama = URLDecoder.decode(nama.replaceAll("_", ""), "UTF-8");

			int bnyk = 100;
			try {
				bnyk = Integer.parseInt(banyak.trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			int mul = 0;
			try {
				mul = Integer.parseInt(mulai.trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			@SuppressWarnings("unchecked")
			List<Siswa> siswas = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
					.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
					.addOrder(Order.asc("id"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(nama == null || nama.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.ilike("nomorInduk", nama, MatchMode.ANYWHERE),
									Restrictions.ilike("nama", nama, MatchMode.ANYWHERE)))
					.setFirstResult(mul).setMaxResults(bnyk).list();

			System.out.println(
					"siswas size => " + siswas.size() + ", bnyk = " + bnyk + ", mul = " + mul + ", nama => " + nama);

			JSONArray array = new JSONArray();
			for (Siswa siswa : siswas) {

				JSONObject json = new JSONObject();
				json.put("id", siswa.getId());
				json.put("nama", siswa.getNama());
				json.put("foto", CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa)));
				json.put("nomorInduk", siswa.getNomorInduk());
				json.put("sisaDeposit", siswa.hitungSisaDeposit(ais.ui.util.WaktuUtil.getDate()));
				json.put("sekolah", siswa.getSekolah().getNama());
				json.put("ayah", siswa.getNamaAyah());
				json.put("ibu", siswa.getNamaIbu());
				json.put("tahunMasuk", siswa.getTahunMasuk());
				array.put(json);
			}

			CommonID commonID = new CommonID();
			commonID.setInfo1(array.toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}

	@Path("ambil_siswa/{nomorInduk}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID getAmbilSiswa(@PathParam("nomorInduk") String nomorInduk) {
		Session session = HibernateUtil.currentNativeSession();
		try {

			System.out.println("nomorInduk => " + nomorInduk);

			nomorInduk = URLDecoder.decode(nomorInduk.replaceAll("_", ""), "UTF-8");

			Siswa siswa = (Siswa) session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
					.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.ilike("nomorInduk", nomorInduk, MatchMode.EXACT)).uniqueResult();

			System.out.println("siswa => " + siswa + ", nama => " + nomorInduk);

			if (siswa == null) {
				HibernateUtil.closeSession();
				throw new NotFoundException("Siswa tidak ditemukan");
			}

			CommonID commonID = new CommonID();
			commonID.setId(siswa.getId());
			commonID.setInfo1(siswa.getNomorInduk());
			commonID.setInfo2(siswa.getNama());
			commonID.setInfo3(siswa.getNamaAyah());
			commonID.setInfo4(siswa.getNamaIbu());
			commonID.setInfo5(siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama());
			commonID.setInfo6(siswa.hitungSisaDeposit(ais.ui.util.WaktuUtil.getDate()).intValue() + "");
			commonID.setInfo7(CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa)));
			commonID.setInfo8(siswa.getTahunMasuk().toString());

			HibernateUtil.closeSession();

			return commonID;
		} catch (Exception e) {
			HibernateUtil.closeSession();
			Common.tampilErrorJikaAdmin(e);
			throw new NotFoundException("Terjadi kesalahan internal");
		}
	}
}
