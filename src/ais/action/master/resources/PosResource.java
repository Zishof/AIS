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

@Path("/pos")
@Singleton

public class PosResource {

	public long getSystemTime() {
		return System.currentTimeMillis();
	}

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

	@GET
	@Path("absen/{id_finger}/{waktu}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID getAbsen(@PathParam("id_finger") String id_finger, @PathParam("waktu") String waktu) {

		return PosResource.absen(id_finger, waktu, null);
	}

	@GET
	@Path("absen/{id_finger}/{waktu}/{state}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CommonID getAbsen(@PathParam("id_finger") String id_finger, @PathParam("waktu") String waktu,
			@PathParam("state") String state) {

		return PosResource.absen(id_finger, waktu, state);
	}

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
