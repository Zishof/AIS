package ais.action.servlet.api;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.CommonPayroll;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

public class AbsensiApiAction {
	private static final long BATAS_MUNDUR_WAKTU_ABSEN_MILLIS = 24L * 60L * 60L * 1000L;
	private static final long BATAS_MAJU_WAKTU_ABSEN_MILLIS = 5L * 60L * 1000L;
	private static final String[] KUNCI_WAKTU_ABSEN = { "waktu", "waktuAbsen", "tanggalAbsen", "timestamp",
			"capturedAt", "createdAt", "tanggal", "datetime", "date", "time" };
	private static final String[] FORMAT_WAKTU_ABSEN = { "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss",
			"yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss.SSS",
			"yyyy-MM-dd'T'HH:mm:ss", "dd-MM-yyyy HH:mm:ss", "dd/MM/yyyy HH:mm:ss" };

	@SuppressWarnings({})
	public static JSONObject absen(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				Siswa siswa = tbmuser.getSiswa();
				if (Common.bolehKonfigurasi("siswa_tidak_boleh_absen_online", Konfigurasi.TIDAK_AKTIF) && siswa != null) {

					String s = "Siswa tidak boleh absen";

					jsonObject.put("status", "91");
					jsonObject.put("description", s);
					return jsonObject;
				}

				Pertemuan pertemuan = null;
				if (!(request.isNull("pert") || !Common.isNumber(request.get("pert") + ""))) {
					Session sessPert = HibernateUtil.openSession();
					try {
						pertemuan = (Pertemuan) sessPert.createCriteria(Pertemuan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.idEq(Long.parseLong(request.get("pert") + ""))).uniqueResult();
					} finally {
						HibernateUtil.closeSessionQuietly(sessPert);
					}
				}

				Mahasiswa mahasiswa = tbmuser.getMahasiswa();

				Dosen dosen = tbmuser.ambilDosen();
				Guru guru = tbmuser.ambilGuru();
				BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser.getBiodataCalonMahasiswa();
				Pegawai pegawai = tbmuser.getPegawai();

				String lat = request.isNull("lat") ? null : request.get("lat") + "";
				String lng = request.isNull("lng") ? null : request.get("lng") + "";
				String pert = request.isNull("pert") ? null : request.get("pert") + "";
				String urlFoto = request.isNull("urlFoto") ? null : request.get("urlFoto") + "";
				String state = request.isNull("state") ? "" : request.get("state") + "";

				CutiDanIzin cutiDanIzin = null;
				try {
					if (pegawai != null && pegawai.getId() != null) {
						cutiDanIzin = CutiDanIzin.apakahSedangCuti(pegawai, WaktuUtil.getDate());
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/AbsensiApiAction.java:88");
				}

				if (cutiDanIzin != null && cutiDanIzin.getId() != null) {

					String s = "Status Anda sedang cuti dengan alasan \"" + cutiDanIzin.getKeterangan()
							+ "\" mulai tanggal " + Common.dateFormat6.get().format(cutiDanIzin.getMulai()) + " sd "
							+ Common.dateFormat6.get().format(cutiDanIzin.getSampai())
							+ " .. Absensi online gagal dilakukan";

					jsonObject.put("status", "91");
					jsonObject.put("description", s);

				} else {
					boolean jarakBenar = true;
					if (pertemuan != null && pertemuan.getLokasi() != null && lng != null && lat != null) {

						try {
							double latitude1 = pertemuan.getLokasi().getLat();
							double longitude1 = pertemuan.getLokasi().getLng();
							double latitude2 = Double.parseDouble(lat);
							double longitude2 = Double.parseDouble(lng);

							Double jarakKm = Common.getDistanceBetweenPointsNew(latitude1, longitude1, latitude2,
									longitude2);

							if (jarakKm > pertemuan.getJarak()) {

								String s = "Absensi gagal dilakukan pada "
										+ Common.dateFormat5.get().format(WaktuUtil.getDate())
										+ ", karena jarak lokasi Anda berada "
										+ Common.numberFormat.get().format(jarakKm) + "km dari lokasi/koordinat "
										+ pertemuan.getLokasi().getNama();

								jsonObject.put("status", "91");
								jsonObject.put("description", s);

								jarakBenar = false;
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/AbsensiApiAction.java:128");
						}
					}

					if (jarakBenar) {

						if (lat == null || lat.trim().isEmpty() || lat.trim().equalsIgnoreCase("0.0")
								|| lat.trim().equalsIgnoreCase("0") || lng == null || lng.trim().isEmpty()
								|| lng.trim().equalsIgnoreCase("0.0") || lng.trim().equalsIgnoreCase("0")) {

							jsonObject.put("status", "97");
							jsonObject.put("description",
									"Absensi online gagal dilakukan.. Lokasi Anda tidak ditemukan, pastikan pengaturan Lokasi Anda aktif dan sistem telah diizinkan (Allow) untuk mengakses lokasi.");

						} else {

							String map = "";
							if (lat != null && lng != null) {
								map = "https://maps.google.com/maps?q=" + lat + "," + lng + "&hl=id&z=14";
							}

							if (pertemuan != null) {
								JSONObject jsonObjectAbsen = new JSONObject();
								try {
									String sebelumnya = pertemuan.retreive("sejarah");
									if (!sebelumnya.isEmpty()) {
										jsonObjectAbsen = new JSONObject(sebelumnya);
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/AbsensiApiAction.java:156");
									// TODO: handle exception
								}

								Date tanggal = WaktuUtil.getDate();
								String keyTgl = "";
								if (mahasiswa != null && mahasiswa.getId() != null) {

									keyTgl = Common.dateFormat9.get().format(tanggal) + ":mhs" + mahasiswa.getId();

									jsonObjectAbsen.put(keyTgl + "_lokasi", map);
									jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
									jsonObject.put(keyTgl + "_foto", urlFoto);

									// Jangan memakai cache peserta untuk keputusan boleh/tidak boleh absen.
									// KRS yang baru disetujui harus langsung terbaca dari database.
									boolean ada = pertemuan.apakahMahasiswaPesertaDisetujuiLangsung(mahasiswa);
									if (!ada) {
										String s = "Mahasiswa dengan NIM \"" + mahasiswa.getNim() + "\" dan nama \""
												+ mahasiswa.getNama()
												+ "\" tidak terdaftar di absensi pertemuan ini. Absensi online gagal dilakukan.";

										jsonObject.put("status", "97");
										jsonObject.put("description", s);

										jsonObject.put(keyTgl + "_foto", urlFoto);
										jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);

										jsonObjectAbsen.put(keyTgl + "_info", s);
										pertemuan.put(jsonObjectAbsen.toString(), "sejarah");

									}
								} else if (dosen != null && dosen.getId() != null) {

									keyTgl = Common.dateFormat9.get().format(tanggal) + ":dsn" + dosen.getId();

									jsonObjectAbsen.put(keyTgl + "_lokasi", map);

									List<Dosen> dosens = pertemuan.ambilDosen();
									boolean ada = false;
									for (Dosen mhs : dosens) {
										if (mhs != null && mhs.getId() != null && mhs.getId().equals(dosen.getId())) {
											ada = true;
											break;
										}
									}
									dosens = null;
									if (!ada) {
										String s = "Dosen dengan nama \"" + dosen.getNama()
												+ "\" tidak terdaftar di absensi pertemuan ini. Absensi online gagal dilakukan.";

										jsonObject.put("status", "97");
										jsonObject.put("description", s);

										jsonObject.put(keyTgl + "_foto", urlFoto);
										jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);

										jsonObjectAbsen.put(keyTgl + "_info", s);
										pertemuan.put(jsonObjectAbsen.toString(), "sejarah");
									}
								}

								Calendar calendarMulai = Calendar.getInstance();

								if (pertemuan.getTanggalRealisasi() != null) {
									calendarMulai.setTime(pertemuan.getTanggalRealisasi());
								} else if (pertemuan.getTanggal() != null) {
									calendarMulai.setTime(pertemuan.getTanggal());
								}

								try {
									if (pertemuan.getWaktuMulai() != null) {
										Integer jamMulai = Integer.parseInt(pertemuan.getWaktuMulai().split("\\.")[0]);
										Integer menitMulai = Integer
												.parseInt(pertemuan.getWaktuMulai().split("\\.")[1]);
										calendarMulai.set(Calendar.HOUR_OF_DAY, jamMulai);
										calendarMulai.set(Calendar.MINUTE,
												menitMulai - pertemuan.getBolehAbsenSebelumWaktuMulaiDalamMenit());
										calendarMulai.set(Calendar.SECOND, 1);

									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/AbsensiApiAction.java:245");
								}

								Calendar calendarSelesai = Calendar.getInstance();

								if (pertemuan.getTanggalRealisasi() != null) {
									calendarSelesai.setTime(pertemuan.getTanggalRealisasi());
								} else {
									calendarSelesai.setTime(pertemuan.getTanggal());
								}

								try {
									if (pertemuan.getWaktuMulai() != null) {
										Integer jamMulai = Integer
												.parseInt(pertemuan.getWaktuSelesai().split("\\.")[0]);
										Integer menitMulai = Integer
												.parseInt(pertemuan.getWaktuSelesai().split("\\.")[1]);
										calendarSelesai.set(Calendar.HOUR_OF_DAY, jamMulai);
										calendarSelesai.set(Calendar.MINUTE,
												menitMulai + pertemuan.getBolehAbsenSetelahWaktuMulaiDalamMenit());
										calendarSelesai.set(Calendar.SECOND, 1);
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/AbsensiApiAction.java:268");
								}

								Statusabsensi statusabsensi = (Statusabsensi) (mahasiswa != null
										? ConstantValues.ambil(Statusabsensi.class.getName(),
												pertemuan.retreiveAbsensiId(mahasiswa.getId()))
										: dosen != null
												? ConstantValues.ambil(Statusabsensi.class.getName(),
														pertemuan.retreiveAbsensiId(dosen.getId()))
												: biodataCalonMahasiswa != null
														? ConstantValues.ambil(Statusabsensi.class.getName(), pertemuan
																.retreiveAbsensiId(biodataCalonMahasiswa.getId()))
														: null);

								if (statusabsensi == null) {
									statusabsensi = ConstantValues.BELUM_ABSEN;
								}

								if (ConstantValues.BELUM_ABSEN != null && ConstantValues.MASUK != null
										&& (statusabsensi.getId().equals(ConstantValues.BELUM_ABSEN.getId())
												|| statusabsensi.getId().equals(ConstantValues.MASUK.getId()))) {

									String keterangan = "";

									Date currentDate = WaktuUtil.getDate();

									boolean berhasil = true;
									if (mahasiswa != null && pertemuan.getPerkuliahan() != null && pertemuan
											.getPerkuliahan().getMahasiswaHanyaBolehAbsenSetelahAdaDosenYangAbsen()) {
										boolean dosenTelahAbsen = false;
										for (Long dosenId : pertemuan.getPerkuliahan().populateDosenBuId()) {
											Statusabsensi statusabsensiData = (Statusabsensi) ConstantValues.ambil(
													Statusabsensi.class.getName(),
													pertemuan.retreiveAbsensiId(dosenId));
											if (statusabsensiData != null
													&& statusabsensiData.getId().equals(ConstantValues.ABSEN.getId())) {
												dosenTelahAbsen = true;
												break;
											}
										}
										if (!dosenTelahAbsen) {
											String s = "Absensi gagal pada "
													+ Common.dateFormat5.get().format(WaktuUtil.getDate())
													+ ", karena belum ada dosen yang belum melakukan absensi";
											if (lat != null && lng != null) {
												s += ", lokasi absen " + map + " ";
											}
											if (urlFoto != null && !urlFoto.trim().isEmpty()) {
												s += ", " + "foto" + " absen " + urlFoto + " ";
											}

											keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

											jsonObject.put("status", "97");
											jsonObject.put("description", s);

											jsonObject.put(keyTgl + "_foto", urlFoto);
											jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);

											jsonObjectAbsen.put(keyTgl + "_info", s);
											pertemuan.put(jsonObjectAbsen.toString(), "sejarah");
											berhasil = false;
										}
									}

									if (pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal()
											&& currentDate.before(calendarMulai.getTime())) {
										String d = (SmartDateTimeUtil.getDayString(calendarMulai.getTime(), null)
												+ Common.dateFormat5.get().format(calendarMulai.getTime()));

										String s = "Absensi gagal pada "
												+ Common.dateFormat5.get().format(WaktuUtil.getDate())
												+ ", karena absensi online belum dimulai " + d;
										if (lat != null && lng != null) {
											s += ", lokasi absen " + map + " ";
										}
										if (urlFoto != null && !urlFoto.trim().isEmpty()) {
											s += ", " + "foto" + " absen " + urlFoto + " ";
										}

										keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

										jsonObject.put("status", "97");
										jsonObject.put("description", s);

										jsonObject.put(keyTgl + "_foto", urlFoto);
										jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);

										jsonObjectAbsen.put(keyTgl + "_info", s);
										pertemuan.put(jsonObjectAbsen.toString(), "sejarah");

									} else if (pertemuan.getPerkulaiahnOnlineHarusSesuaiJadwal()
											&& currentDate.after(calendarSelesai.getTime())) {
										String d = (SmartDateTimeUtil.getDayString(calendarSelesai.getTime(), null)
												+ Common.dateFormat5.get().format(calendarSelesai.getTime()));

										String s = "Absensi gagal pada "
												+ Common.dateFormat5.get().format(WaktuUtil.getDate())
												+ ", karena absensi online telah terlewat " + d;
										if (lat != null && lng != null) {
											s += ", lokasi absen " + map + " ";
										}
										if (urlFoto != null && !urlFoto.trim().isEmpty()) {
											s += ", " + "foto" + " absen " + urlFoto + " ";
										}

										keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;
										jsonObject.put(keyTgl + "_foto", urlFoto);
										jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
										jsonObjectAbsen.put(keyTgl + "_info", s);
										pertemuan.put(jsonObjectAbsen.toString(), "sejarah");

										jsonObject.put("status", "97");
										jsonObject.put("description", s);
									} else if (berhasil) {

										String s = "Absensi sukses pada "
												+ Common.dateFormat5.get().format(WaktuUtil.getDate());
										if (lat != null && lng != null) {
											s += ", lokasi absen " + map + " ";
										}
										if (urlFoto != null && !urlFoto.trim().isEmpty()) {
											s += ", " + "foto" + " absen " + urlFoto + " ";
										}

										keterangan = (keterangan.isEmpty() ? s : s + ";") + keterangan;

										statusabsensi = ConstantValues.MASUK;
										jsonObject.put(keyTgl + "_foto", urlFoto);
										jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
										jsonObject.put("status", "00");
										jsonObject.put("description", s);

										jsonObjectAbsen.put(keyTgl + "_info", s);
										pertemuan.put(jsonObjectAbsen.toString(), "sejarah");

									}

									pertemuan
											.populate(
													siswa != null ? siswa.getId()
															: guru != null ? guru.getId()
																	: mahasiswa != null ? mahasiswa.getId()
																			: dosen != null ? dosen.getId()
																					: biodataCalonMahasiswa != null
																							? biodataCalonMahasiswa
																									.getId()
																							: -1L,
													statusabsensi, keterangan, null,
													Common.timeFormat2.get().format(WaktuUtil.getDate()),
													pertemuan.getWaktuSelesai(),
													siswa != null ? "Siswa"
															: guru != null ? "Guru"
																	: mahasiswa != null ? "Mahasiswa"
																			: dosen != null ? "Dosen"
																					: biodataCalonMahasiswa != null
																							? "Calon Mahasiswa"
																							: "");

									Session session = HibernateUtil.openSession();
									try {
									session.getTransaction().begin();
									Common.refreshUpdate(session, pertemuan);
									session.getTransaction().commit();
									// session.disconnect();
									ApiHelperSupport.closeOpenedSession(session);

									} finally {
										HibernateUtil.closeSessionQuietly(session);
									}
								} else if (statusabsensi != null) {

									String s = "Status kehadiran Anda telah dinyatakan \"" + statusabsensi.getNama()
											+ "\", sehingga Anda tidak bisa melakukan absen ulang.";
									jsonObject.put(keyTgl + "_foto", urlFoto);
									jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
									jsonObjectAbsen.put(keyTgl + "_info", s);
									pertemuan.put(jsonObjectAbsen.toString(), "sejarah");

									jsonObject.put("status", "00");
									jsonObject.put("description", s);

								}
							}

							Date tanggal = WaktuUtil.getDate();
							if (pegawai != null && pertemuan == null) {
								tanggal = ambilWaktuAbsensi(request, tanggal);
							}

							if (pegawai != null && pert != null && pert.startsWith("P")) {
								String code = pert.split("-", 2)[1];
								System.out.println("Absensi pegawai code = " + code);
								String tgl = Common.desEncrypter.get().decrypt(code);
								System.out.println("Absensi pegawai tgl = " + tgl);
								Date tanggalQr = Common.dateFormat8.get().parse(tgl);
								System.out.println("Absensi pegawai tanggal = " + tanggalQr);
								if (!Common.dateFormat8.get().format(tanggal).equals(tgl)) {
									String s = "<strong><font style='color:red;font-size: 15px;'>Pegawai dengan nama \""
											+ pegawai.getNama()
											+ "\" tidak bisa melakukan absen karena tanggal QR-Code tidak sesuai, tanggal di QR-Code \""
											+ Common.dateFormat6.get().format(tanggalQr) + "\" sedangkan waktu absensi  \""
											+ Common.dateFormat6.get().format(tanggal)
											+ "\". Absensi online gagal dilakukan</font></strong><br><img style='width:50px' src=\""
											+ Common.getRequestHostWithProtocol()
											+ "/img/oh.gif\" alt=\"WebP rules.\" />";

									jsonObject.put("status", "97");
									jsonObject.put("description", s);
								}

							}

							String keyTgl = Common.dateFormat9.get().format(tanggal);
							Session session = HibernateUtil.openSession();
							try {
							try {

								StatuskehadiranKaryawanHarian statuskehadiranKaryawanHarian = CommonPayroll
										.getDefaultStatuskehadiranKaryawanHarian(tanggal, pegawai, mahasiswa, siswa,
												lat, lng, session, true);

								JSONObject jsonObjectAbsen = new JSONObject();
								try {
									String sebelumnya = statuskehadiranKaryawanHarian.retreive("sejarah");
									if (!sebelumnya.isEmpty()) {
										jsonObjectAbsen = new JSONObject(sebelumnya);
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/AbsensiApiAction.java:493");
									// TODO: handle exception
								}
								jsonObject.put(keyTgl + "_foto", urlFoto);
								jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
								jsonObjectAbsen.put(keyTgl + "_lokasi", map);

								boolean datang = true;

								System.out.println("state -> " + state);

								Date msk = statuskehadiranKaryawanHarian.getMasukjamState();
								Date plg = statuskehadiranKaryawanHarian.getPulangJamState();
								if ((state != null && state.trim().equalsIgnoreCase("0"))) {
									if (msk == null || (tanggal != null && msk != null
											&& Double.parseDouble(Common.timeFormat2.get().format(msk)) > Double
													.parseDouble(Common.timeFormat2.get().format((tanggal))))) {
										statuskehadiranKaryawanHarian.setMasukjam(tanggal);
										statuskehadiranKaryawanHarian.setMasukjamManual(tanggal);
										statuskehadiranKaryawanHarian.setMasukjamState(tanggal);
										statuskehadiranKaryawanHarian.setFotoAbsenDatang(urlFoto);
										statuskehadiranKaryawanHarian.setLokasiAbsenDatang(map);
									}
								} else if ((state != null && state.trim().equalsIgnoreCase("1"))) {
									if (plg == null || (tanggal != null && plg != null
											&& Double.parseDouble(Common.timeFormat2.get().format(plg)) < Double
													.parseDouble(Common.timeFormat2.get().format((tanggal))))) {
										statuskehadiranKaryawanHarian.setPulangJam(tanggal);
										statuskehadiranKaryawanHarian.setPulangJamManual(tanggal);
										statuskehadiranKaryawanHarian.setPulangJamState(tanggal);
										statuskehadiranKaryawanHarian.setLokasiAbsenPulang(map);
										statuskehadiranKaryawanHarian.setFotoAbsenPulang(urlFoto);
									}
									datang = false;
								} else if (statuskehadiranKaryawanHarian.ambilMasukjam() == null
										&& (cutiDanIzin == null || !cutiDanIzin.getSetujui())) {
									statuskehadiranKaryawanHarian.setMasukjam(tanggal);
									statuskehadiranKaryawanHarian.setFotoAbsenDatang(urlFoto);
									statuskehadiranKaryawanHarian.setLokasiAbsenDatang(map);
								} else {
									statuskehadiranKaryawanHarian.setLokasiAbsenPulang(map);

									datang = false;
									statuskehadiranKaryawanHarian.setFotoAbsenPulang(urlFoto);
									statuskehadiranKaryawanHarian.setPulangJam(tanggal);
									// Cegah SHADOW: majukan State/Manual yang sudah terisi lebih awal ke scan ini.
									statuskehadiranKaryawanHarian.majukanPulangStateManualJikaLebihAwal(tanggal);
								}

								Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
								calendar.setTime(tanggal);
								String hari = Common.haris[calendar.get(Calendar.DAY_OF_WEEK) - 1];
								DetailJenisShiftPegawai jenis = CommonPayroll.getDetailJenisShiftPegawai(pegawai,
										mahasiswa, siswa,
										statuskehadiranKaryawanHarian.ambilMasukjam() == null ? tanggal
												: statuskehadiranKaryawanHarian.ambilMasukjam(),
										statuskehadiranKaryawanHarian.getTanggal(), hari,
										statuskehadiranKaryawanHarian.getLiburNasional() != null);

								System.out.println("statuskehadiranKaryawanHarian -> " + statuskehadiranKaryawanHarian
										+ ", jenis -> " + jenis + " lng " + lng + " lat " + lat);

								if (jenis == null && pegawai != null) {
									String keterangan = statuskehadiranKaryawanHarian.getKeterangan();
									String s = "Hai " + pegawai.getNama()
											+ ". Absensi foto GAGAL dilakukan, karena pengaturan jadwal absensi tidak ditemukan, hubungi bagian kepegawaian atau HRD.";
									keterangan = (keterangan.isEmpty() ? s : s + ";\n") + keterangan;
									jsonObject.put(keyTgl + "_foto", urlFoto);
									jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
									jsonObjectAbsen.put(keyTgl + "_info", s);

									statuskehadiranKaryawanHarian.put(jsonObjectAbsen.toString(), "sejarah");

									jsonObject.put("status", "97");
									jsonObject.put("description", s);
								}

								else if (jenis != null)

								{

									System.out
											.println("1. statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() -> "
													+ statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai());

									if (statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() != null) {
										System.out.println("1. getAbaikanJarak -> " + statuskehadiranKaryawanHarian
												.getJenisShiftPunyaPegawai().getAbaikanJarak());

									}

									Lokasi lokasi = null;
									jarakBenar = true;
									if (jenis.getJenisShiftPegawai() != null && lng != null && lat != null
											&& !statuskehadiranKaryawanHarian.getAbaikanJarak()
											&& (statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() == null
													|| !statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai()
															.getAbaikanJarak())) {

										try {
											Entry<Double, Lokasi> entry = jenis.ambilJarakDanLokasiTerdekat(lat, lng);

											if (entry != null) {

												Double jarakKm = entry.getKey();
												lokasi = entry.getValue();
												System.out.println("jarakKm -> " + jarakKm);

												try {
													statuskehadiranKaryawanHarian.setJarak(jarakKm);
													statuskehadiranKaryawanHarian
															.setJarakMaks(jenis.getJenisShiftPegawai().getJarak());
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/AbsensiApiAction.java:603");
												}

												if (jarakKm > jenis.getJenisShiftPegawai().getJarak()) {

													String s = "Absensi gagal dilakukan pada "
															+ Common.dateFormat5.get().format(WaktuUtil.getDate())
															+ ", karena jarak lokasi Anda berada "
															+ Common.numberFormat.get().format(jarakKm)
															+ "km dari lokasi/koordinat " + lokasi.getNama();

													jsonObject.put("status", "91");
													jsonObject.put("description", s);
													jsonObject.put(keyTgl + "_foto", urlFoto);
													jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
													jsonObjectAbsen.put(keyTgl + "_info", s);

													statuskehadiranKaryawanHarian.put(jsonObjectAbsen.toString(),
															"sejarah");

													jarakBenar = false;
												}
											}
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/AbsensiApiAction.java:626");
										}
									}

									if (jarakBenar) {
										statuskehadiranKaryawanHarian.setDetailJenisShiftPegawai(jenis);
										if (datang) {

											Double menitDatangFoto = statuskehadiranKaryawanHarian
													.getJumlahMenitAbsenFotoSaatHadir();
											Double mulaiAbsenFoto = Math.abs(jenis.getMenitSebelumJamMulai());
											Double mulaiAbsenFotoSetelah = Math.abs(jenis.getMenitSetelahJamMulai());

											boolean belumMulaiToleransi = Math.abs(menitDatangFoto) > mulaiAbsenFoto;
											boolean setelahMulaiToleransi = Math
													.abs(menitDatangFoto) > mulaiAbsenFotoSetelah;
											System.out.println(jenis + " : menitDatangFoto -> " + menitDatangFoto
													+ ", mulaiAbsenFoto -> " + mulaiAbsenFoto
													+ ", mulaiAbsenFotoSetelah -> " + mulaiAbsenFotoSetelah
													+ ", belumMulaiToleransi -> " + belumMulaiToleransi
													+ ", setelahMulaiToleransi -> " + setelahMulaiToleransi);

											if (menitDatangFoto < 0 && belumMulaiToleransi) {
												String keterangan = statuskehadiranKaryawanHarian.getKeterangan();

												String s = "Hai " + pegawai.getNama()
														+ ". Absensi foto kedatangan GAGAL dilakukan, karena di pengaturan jadwal absensi, absen menggunakan foto atau video bisa dilakukan maksimal "
														+ ((int) Math.abs(mulaiAbsenFoto)) + " menit sebelum pukul "
														+ Common.timeFormat.get().format(jenis.getMulai())
														+ ", sedangkan saat ini " + ((int) Math.abs(menitDatangFoto))
														+ " menit sebelum pukul "
														+ Common.timeFormat.get().format(jenis.getMulai()) + ".";
												keterangan = (keterangan.isEmpty() ? s : s + ";\n") + keterangan;

												jsonObject.put("status", "97");
												jsonObject.put("description", s);
												jsonObject.put(keyTgl + "_foto", urlFoto);
												jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
												jsonObjectAbsen.put(keyTgl + "_info", s);

												statuskehadiranKaryawanHarian.put(jsonObjectAbsen.toString(),
														"sejarah");

											} else if (menitDatangFoto > 0 && setelahMulaiToleransi) {
												String keterangan = statuskehadiranKaryawanHarian.getKeterangan();

												String s = "Hai " + (pegawai == null ? "" : pegawai.getNama())
														+ ". Absensi foto kedatangan GAGAL dilakukan, karena di pengaturan jadwal absensi, absen menggunakan foto atau video bisa dilakukan maksimal "
														+ ((int) Math.abs(mulaiAbsenFotoSetelah))
														+ " menit setelah pukul "
														+ Common.timeFormat.get().format(jenis.getMulai())
														+ ", sedangkan saat ini " + ((int) Math.abs(menitDatangFoto))
														+ " menit setelah pukul "
														+ Common.timeFormat.get().format(jenis.getMulai()) + ".";
												keterangan = (keterangan.isEmpty() ? s : s + ";\n") + keterangan;

												jsonObject.put("status", "97");
												jsonObject.put("description", s);
												jsonObject.put(keyTgl + "_foto", urlFoto);
												jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
												jsonObjectAbsen.put(keyTgl + "_info", s);

												statuskehadiranKaryawanHarian.put(jsonObjectAbsen.toString(),
														"sejarah");

											} else {
												statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.MASUK);
											}

										} else {

											Double menitPulangFoto = statuskehadiranKaryawanHarian
													.getJumlahMenitAbsenFotoSaatPulang();
											Double mulaiAbsenFoto = Math.abs(jenis.getMenitSebelumJamSampai());
											Double mulaiAbsenFotoSetelah = Math.abs(jenis.getMenitSetelahJamSampai());

											boolean belumMulaiToleransi = Math.abs(menitPulangFoto) > mulaiAbsenFoto;
											boolean setelahMulaiToleransi = Math
													.abs(menitPulangFoto) > mulaiAbsenFotoSetelah;
											System.out.println(jenis + " : menitPulangFoto -> " + menitPulangFoto
													+ ", mulaiAbsenFoto -> " + mulaiAbsenFoto
													+ ", mulaiAbsenFotoSetelah -> " + mulaiAbsenFotoSetelah
													+ ", belumMulaiToleransi -> " + belumMulaiToleransi
													+ ", setelahMulaiToleransi -> " + setelahMulaiToleransi);

											if (menitPulangFoto < 0 && belumMulaiToleransi) {
												String keterangan = statuskehadiranKaryawanHarian.getKeterangan();

												String s = "Hai " + pegawai.getNama()
														+ ". Absensi foto kepulangan GAGAL dilakukan, karena di pengaturan jadwal absensi, absen menggunakan foto atau video bisa dilakukan maksimal "
														+ ((int) Math.abs(mulaiAbsenFoto)) + " menit sebelum pukul "
														+ Common.timeFormat.get().format(jenis.getSampai())
														+ ", sedangkan saat ini " + ((int) Math.abs(menitPulangFoto))
														+ " menit sebelum pukul "
														+ Common.timeFormat.get().format(jenis.getSampai()) + ".";
												keterangan = (keterangan.isEmpty() ? s : s + ";\n") + keterangan;

												jsonObject.put("status", "97");
												jsonObject.put("description", s);
												jsonObject.put(keyTgl + "_foto", urlFoto);
												jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
												jsonObjectAbsen.put(keyTgl + "_info", s);

												statuskehadiranKaryawanHarian.put(jsonObjectAbsen.toString(),
														"sejarah");

											} else if (menitPulangFoto > 0 && setelahMulaiToleransi) {
												String keterangan = statuskehadiranKaryawanHarian.getKeterangan();

												String s = "Hai " + pegawai.getNama()
														+ ". Absensi foto kepulangan GAGAL dilakukan, karena di pengaturan jadwal absensi, absen menggunakan foto atau video bisa dilakukan maksimal "
														+ ((int) Math.abs(mulaiAbsenFotoSetelah))
														+ " menit setelah pukul "
														+ Common.timeFormat.get().format(jenis.getSampai())
														+ ", sedangkan saat ini " + ((int) Math.abs(menitPulangFoto))
														+ " menit setelah pukul "
														+ Common.timeFormat.get().format(jenis.getSampai()) + ".";
												keterangan = (keterangan.isEmpty() ? s : s + ";\n") + keterangan;

												jsonObject.put("status", "97");
												jsonObject.put("description", s);
												jsonObject.put(keyTgl + "_foto", urlFoto);
												jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
												jsonObjectAbsen.put(keyTgl + "_info", s);

												statuskehadiranKaryawanHarian.put(jsonObjectAbsen.toString(),
														"sejarah");

											} else {
												statuskehadiranKaryawanHarian.setStatusabsensi(ConstantValues.MASUK);
											}

										}

										System.out.println(
												"2. statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() -> "
														+ statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai());

										if (statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() != null) {
											System.out.println("2. getAbaikanJarak -> " + statuskehadiranKaryawanHarian
													.getJenisShiftPunyaPegawai().getAbaikanJarak());

										}

										if (statuskehadiranKaryawanHarian.getJarak() != null
												&& statuskehadiranKaryawanHarian.getJarakMaks() != null
												&& statuskehadiranKaryawanHarian
														.getJarak() > statuskehadiranKaryawanHarian.getJarakMaks()
												&& !statuskehadiranKaryawanHarian.getAbaikanJarak()
												&& (statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai() == null
														|| !statuskehadiranKaryawanHarian.getJenisShiftPunyaPegawai()
																.getAbaikanJarak())) {
											String s = "Absensi gagal dilakukan pada "
													+ Common.dateFormat5.get().format(WaktuUtil.getDate())
													+ ", karena jarak lokasi Anda berada "
													+ Common.numberFormat.get()
															.format(statuskehadiranKaryawanHarian.getJarak())
													+ "km dari lokasi/koordinat "
													+ (lokasi == null ? "" : lokasi.getNama());

											jsonObject.put("status", "91");
											jsonObject.put("description", s);
											jsonObject.put(keyTgl + "_foto", urlFoto);
											jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
											jsonObjectAbsen.put(keyTgl + "_info", s);

											statuskehadiranKaryawanHarian.put(jsonObjectAbsen.toString(), "sejarah");
										} else {

											String s = "ABSEN " + (datang ? "DATANG " : "PULANG ")
													+ Common.dateFormat5.get().format(tanggal);
											if (lat != null && lng != null) {
												s += " " + map + " ";
											}
											if (urlFoto != null && !urlFoto.trim().isEmpty()) {
												s += ", " + "foto" + " absen " + urlFoto + " ";
											}

											statuskehadiranKaryawanHarian.setKeterangan(s);

											session.getTransaction().begin();
											Common.refreshSaveOrUpdate(session, statuskehadiranKaryawanHarian);
											session.getTransaction().commit();

											CommonPayroll.simpanDetail(session, statuskehadiranKaryawanHarian, true);

											jsonObject.put("status", "00");
											jsonObject.put("description", "Absensi "
													+ (datang ? "kedatangan " : "kepulangan") + " sukses dilakukan");
											jsonObject.put(keyTgl + "_foto", urlFoto);
											jsonObjectAbsen.put(keyTgl + "_foto", urlFoto);
											jsonObjectAbsen.put("Absensi " + (datang ? "kedatangan " : "kepulangan")
													+ " sukses dilakukan", keyTgl + "_info");

											statuskehadiranKaryawanHarian.put(jsonObjectAbsen.toString(), "sejarah");
										}
									}
								}

							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
							// session.disconnect();
							ApiHelperSupport.closeOpenedSession(session);
							} finally {
								HibernateUtil.closeSessionQuietly(session);
							}
						}
					}

				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/AbsensiApiAction.java:844");
			}
		}

		System.out.println("absen -> " + jsonObject);

		return jsonObject;
	}

	/**
	 * Mengambil waktu saat absensi dibuat di perangkat. Payload resend membawa
	 * kembali nilai ini sehingga jam absensi tidak berubah menjadi jam pengiriman
	 * ulang. Payload aplikasi lama yang belum mengirim waktu tetap memakai jam
	 * server.
	 */
	private static Date ambilWaktuAbsensi(JSONObject request, Date waktuServer) {
		if (request == null || waktuServer == null) {
			return waktuServer;
		}
		for (String key : KUNCI_WAKTU_ABSEN) {
			try {
				if (request.isNull(key)) {
					continue;
				}
				Date waktuPerangkat = parseWaktuAbsensi(request.get(key), waktuServer);
				if (waktuPerangkat == null) {
					continue;
				}
				long selisih = waktuServer.getTime() - waktuPerangkat.getTime();
				if (selisih >= -BATAS_MAJU_WAKTU_ABSEN_MILLIS
						&& selisih <= BATAS_MUNDUR_WAKTU_ABSEN_MILLIS) {
					return waktuPerangkat;
				}
			} catch (Exception e) {
				// Nilai dari versi aplikasi lama dapat berbeda format; gunakan jam server.
			}
		}
		return waktuServer;
	}

	private static Date parseWaktuAbsensi(Object nilai, Date waktuServer) {
		if (nilai == null || JSONObject.NULL.equals(nilai)) {
			return null;
		}
		String teks = String.valueOf(nilai).trim();
		if (teks.length() == 0) {
			return null;
		}
		try {
			long epoch = Long.parseLong(teks);
			if (Math.abs(epoch) < 100000000000L) {
				epoch *= 1000L;
			}
			return new Date(epoch);
		} catch (NumberFormatException e) {
			// Lanjutkan ke format tanggal tekstual.
		}

		String teksZona = teks;
		// DateTime.toIso8601String() dari Flutter dapat membawa 6 digit mikrodetik,
		// sedangkan SimpleDateFormat hanya memahami milidetik.
		teksZona = teksZona.replaceFirst("(\\.\\d{3})\\d+(?=Z$|[+-]\\d{2}:?\\d{2}$|$)", "$1");
		if (teksZona.endsWith("Z")) {
			teksZona = teksZona.substring(0, teksZona.length() - 1) + "+0000";
		} else if (teksZona.matches(".*[+-]\\d{2}:\\d{2}$")) {
			teksZona = teksZona.substring(0, teksZona.length() - 3)
					+ teksZona.substring(teksZona.length() - 2);
		}
		for (String pola : FORMAT_WAKTU_ABSEN) {
			SimpleDateFormat formatter = new SimpleDateFormat(pola);
			formatter.setLenient(false);
			ParsePosition posisi = new ParsePosition(0);
			Date hasil = formatter.parse(teksZona, posisi);
			if (hasil != null && posisi.getIndex() == teksZona.length()) {
				return hasil;
			}
		}

		String[] formatJam = { "HH:mm:ss", "HH:mm" };
		for (String pola : formatJam) {
			SimpleDateFormat formatter = new SimpleDateFormat(pola);
			formatter.setLenient(false);
			ParsePosition posisi = new ParsePosition(0);
			Date jam = formatter.parse(teks, posisi);
			if (jam != null && posisi.getIndex() == teks.length()) {
				Calendar sumber = Calendar.getInstance();
				sumber.setTime(jam);
				Calendar hasil = Calendar.getInstance();
				hasil.setTime(waktuServer);
				hasil.set(Calendar.HOUR_OF_DAY, sumber.get(Calendar.HOUR_OF_DAY));
				hasil.set(Calendar.MINUTE, sumber.get(Calendar.MINUTE));
				hasil.set(Calendar.SECOND, sumber.get(Calendar.SECOND));
				hasil.set(Calendar.MILLISECOND, 0);
				return hasil.getTime();
			}
		}
		return null;
	}

}
