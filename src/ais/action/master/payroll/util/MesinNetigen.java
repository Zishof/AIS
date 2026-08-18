package ais.action.master.payroll.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.payroll.UploadLog;
import ais.database.model.sekolah.Siswa;

public class MesinNetigen {

	static String testData = "[2014/01/01-08:21:33]8888/1/128/1/1\n[2014/01/01-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/02-08:21:40]8888/1/128/1/1\n[2014/01/02-17:21:42]8888/1/128/1/2\n"
			+ "[2014/01/03-08:21:33]8888/1/128/1/1\n[2014/01/03-17:21:36]8888/1/128/1/2\n"
			+ "[2013/01/04-08:21:40]8888/1/128/1/1\n[2013/01/04-17:21:42]8888/1/128/1/2\n"
			+ "[2013/01/05-08:21:33]8888/1/128/1/1\n[2013/01/05-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/06-08:21:33]8888/1/128/1/1\n[2014/01/06-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/07-08:21:40]8888/1/128/1/1\n[2014/01/07-17:21:42]8888/1/128/1/2\n"
			+ "[2014/01/08-08:21:33]8888/1/128/1/1\n[2014/01/08-17:21:36]8888/1/128/1/2\n"
			+ "[2013/01/09-08:21:40]8888/1/128/1/1\n[2013/01/09-17:21:42]8888/1/128/1/2\n"
			+ "[2013/01/10-08:21:33]8888/1/128/1/1\n[2013/01/10-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/11-08:21:33]8888/1/128/1/1\n[2014/01/11-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/12-08:21:40]8888/1/128/1/1\n[2014/01/12-17:21:42]8888/1/128/1/2\n"
			+ "[2014/01/13-08:21:33]8888/1/128/1/1\n[2014/01/13-17:21:36]8888/1/128/1/2\n"
			+ "[2013/01/14-08:21:40]8888/1/128/1/1\n[2013/01/14-17:21:42]8888/1/128/1/2\n"
			+ "[2013/01/15-08:21:33]8888/1/128/1/1\n[2013/01/15-17:21:36]8888/1/128/1/2\n"
			+ "[2013/01/10-08:21:33]8888/1/128/1/1\n[2013/01/10-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/11-08:21:33]8888/1/128/1/1\n[2014/01/11-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/12-08:21:40]8888/1/128/1/1\n[2014/01/12-17:21:42]8888/1/128/1/2\n"
			+ "[2014/01/13-08:21:33]8888/1/128/1/1\n[2014/01/13-17:21:36]8888/1/128/1/2\n"
			+ "[2013/01/14-08:21:40]8888/1/128/1/1\n[2013/01/14-17:21:42]8888/1/128/1/2\n"
			+ "[2013/01/15-08:21:33]8888/1/128/1/1\n[2013/01/15-17:21:36]8888/1/128/1/2\n"
			+ "[2013/01/16-08:21:33]8888/1/128/1/1\n[2013/01/16-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/17-08:21:33]8888/1/128/1/1\n[2014/01/17-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/18-08:21:40]8888/1/128/1/1\n[2014/01/18-17:21:42]8888/1/128/1/2\n"
			+ "[2014/01/19-08:21:33]8888/1/128/1/1\n[2014/01/19-17:21:36]8888/1/128/1/2\n"
			+ "[2013/01/20-08:21:40]8888/1/128/1/1\n[2013/01/20-17:21:42]8888/1/128/1/2\n"
			+ "[2013/01/21-08:21:33]8888/1/128/1/1\n[2013/01/21-17:21:36]8888/1/128/1/2\n"
			+ "[2013/01/22-08:21:33]8888/1/128/1/1\n[2013/01/22-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/23-08:21:33]8888/1/128/1/1\n[2014/01/23-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/24-08:21:40]8888/1/128/1/1\n[2014/01/24-17:21:42]8888/1/128/1/2\n"
			+ "[2014/01/25-08:21:33]8888/1/128/1/1\n[2014/01/25-17:21:36]8888/1/128/1/2\n"
			+ "[2013/01/26-08:21:40]8888/1/128/1/1\n[2013/01/26-17:21:42]8888/1/128/1/2\n"
			+ "[2013/01/27-08:21:33]8888/1/128/1/1\n[2013/01/27-17:21:36]8888/1/128/1/2\n"
			+ "[2013/01/28-08:21:33]8888/1/128/1/1\n[2013/01/28-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/29-08:21:33]8888/1/128/1/1\n[2014/01/29-17:21:36]8888/1/128/1/2\n"
			+ "[2014/01/30-08:21:40]8888/1/128/1/1\n[2014/01/30-17:21:42]8888/1/128/1/2\n"
			+ "[2014/01/31-08:21:33]8888/1/128/1/1\n[2014/01/31-17:21:36]8888/1/128/1/2\n"
			+ "[2013/02/01-08:21:40]8888/1/128/1/1\n[2013/02/01-17:21:42]8888/1/128/1/2\n"
			+ "[2013/02/02-08:21:33]8888/1/128/1/1\n[2013/02/02-17:21:36]8888/1/128/1/2";

	@SuppressWarnings("rawtypes")
	public static void main(String[] argv) {
		List<Map> maps = execute(testData);
		System.out.println(maps);
	}

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

	@SuppressWarnings("rawtypes")
	public static List<Map> execute(String text) {
		List<Map> maps = new ArrayList<Map>();

		String[] textLines = StringUtils.split(text, "\n");
		for (String s : textLines) {
			if (!s.trim().isEmpty()) {
				try {
					Map<String, Object> values = new HashMap<String, Object>();
					String[] k1 = StringUtils.split(s.trim(), "]");
					Date dateTime = dateFormat.parse(org.apache.commons.lang3.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(k1[0], "[", ""), "]", ""));
					values.put("dateTime", dateTime);
					String[] data = StringUtils.split(k1[1].trim(), "/");
					values.put("kode", data[0].trim());
					values.put("no_mesin", data[1].trim());
					values.put("type_absen", data[2].trim());
					values.put("status_valid", data[3].trim());
					values.put("in_out", data[4].trim());
					values.put("origin", s);

					maps.add(values);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/util/MesinNetigen.java:96");

				}
			}
		}

		return maps;
	}

	@SuppressWarnings("rawtypes")
	public static void process(UploadLog uploadLog) {
		List<Map> maps = execute(uploadLog.getTextUpload());
		Session session = HibernateUtil.currentSession();
		for (Map data : maps) {
			String kode = (String) data.get("kode");
			String origin = (String) data.get("origin");
			if (kode != null) {
				Pegawai pegawai = (Pegawai) ConstantValues.simpleObject(
						session.createCriteria(Pegawai.class).createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
								.createAlias("guru", "guru", Criteria.LEFT_JOIN)

								.add(Restrictions.or(Restrictions.eq("idfinger", kode.trim()),
										Restrictions.or(Restrictions.eq("guru.idfinger", kode.trim()),
												Restrictions.eq("dosen.idfinger", kode.trim()))))

								.setMaxResults(1),
						Pegawai.class);
				Mahasiswa mahasiswa = null;
				Siswa siswa = null;
				if (pegawai == null) {
					mahasiswa = (Mahasiswa) ConstantValues
							.simpleObject(
									session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
											.add(Restrictions.or(Restrictions.eq("aktif", true),
													Restrictions.isNull("aktif")))
											.add(Restrictions.eq("nim", kode.trim())).setMaxResults(1),
									Mahasiswa.class);
				}
				if (mahasiswa == null) {
					siswa = (Siswa) ConstantValues
							.simpleObject(
									session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
											.add(Restrictions.or(Restrictions.eq("aktif", true),
													Restrictions.isNull("aktif")))
											.add(Restrictions.eq("nomorIndukNasional", kode.trim())).setMaxResults(1),
									Siswa.class);
				}

				if (siswa == null) {
					siswa = (Siswa) ConstantValues
							.simpleObject(
									session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
											.add(Restrictions.or(Restrictions.eq("aktif", true),
													Restrictions.isNull("aktif")))
											.add(Restrictions.eq("nomorInduk", kode.trim())).setMaxResults(1),
									Siswa.class);
				}

				if (pegawai == null || mahasiswa == null || siswa == null) {
					uploadLog.getLogDetail().add("GAGAL: \"" + origin + "\" Kode \"" + kode + "\"  tidak ditemukan");
				} else {

					Date tanggal = (Date) data.get("dateTime");
					if (tanggal == null) {
						uploadLog.getLogDetail().add("GAGAL: \"" + origin + "\"  dengan kode \"" + pegawai.toString()
								+ "\", kolom waktu tidak ditemukan");
					} else {

						String status_valid = (String) data.get("status_valid");
						if (status_valid == null || status_valid.trim().equals("0")) {
							uploadLog.getLogDetail()
									.add("GAGAL: \"" + origin + "\"  dengan kode \"" + pegawai.toString() + "\", waktu "
											+ Common.dateFormat3.get().format(tanggal) + " tidak valid atau gagal");
						} else {

							String in_out = (String) data.get("in_out");
							if (in_out == null) {
								uploadLog.getLogDetail()
										.add("GAGAL: \"" + origin + "\"  dengan kode \"" + pegawai.toString()
												+ "\", waktu " + Common.dateFormat3.get().format(tanggal)
												+ " tidak ada data in-out");
							} else {
								StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = CommonPayroll
										.getDefaultStatuskehadiranKaryawanHarian(tanggal, pegawai, mahasiswa, siswa);

								if (in_out.trim().equals("1")) {
									statuskehadiranKaryawanHarian.setMasukjam(tanggal);
									statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.MASUK);
									session.update(statuskehadiranKaryawanHarian);

									uploadLog.getLogDetail()
											.add("SUKSES: \"" + origin + "\"  dengan kode \"" + pegawai.toString()
													+ "\", waktu " + Common.dateFormat3.get().format(tanggal)
													+ " masuk ke kantor");

									Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
									calendar.setTime(tanggal);
									String hari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
									statuskehadiranKaryawanHarian.setDetailJenisShiftPegawai(
											CommonPayroll.getDetailJenisShiftPegawai(pegawai,
													statuskehadiranKaryawanHarian.ambilMasukjam(),
													statuskehadiranKaryawanHarian.getTanggal(), hari));

								} else if (in_out.trim().equals("2")) {
									statuskehadiranKaryawanHarian.setPulangJam(tanggal);
									statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.MASUK);
									session.update(statuskehadiranKaryawanHarian);

									uploadLog.getLogDetail()
											.add("SUKSES: \"" + origin + "\"  dengan kode \"" + pegawai.toString()
													+ "\", waktu " + Common.dateFormat3.get().format(tanggal)
													+ " keluar dari kantor");
								}
							}

						}
					}

				}
			} else {
				uploadLog.getLogDetail().add("GAGAL: Kode pegawai atau karyawan tidak ditemukan");
			}
		}
	}
}
