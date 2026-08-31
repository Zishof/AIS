package ais.action.servlet.api;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zul.Toolbar;

import ais.action.master.SertifikatAction;
import ais.action.master.SyaratUjianAction;
import ais.action.master.helper.NilaiValidator;
import ais.action.master.helper.UtsDanUasCheckerHelper;
import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanAbsensiPegawaiPerOrang;
import ais.action.report.format1.akademik.LaporanCatatanAdministrasi;
import ais.action.report.format1.akademik.LaporanKHS;
import ais.action.report.format1.akademik.LaporanKRS;
import ais.action.report.format1.akademik.LaporanRekamanNilai;
import ais.action.report.format1.akademik.LaporanTranskipAkademik;
import ais.action.report.format1.employ.LaporanCatatanPegawai;
import ais.action.report.format1.payroll.LaporanSlipGajiRealPegawaiPerOrang;
import ais.action.report.format1.sekolah.LaporanCatatanGuru;
import ais.action.report.format1.sekolah.LaporanCatatanKelasSiswa;
import ais.action.report.format1.sekolah.LaporanCatatanSiswa;
import ais.action.report.format1.sekolah.LaporanRaporSiswa;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisCatatanAdministrasi;
import ais.database.model.JenisCatatanPegawai;
import ais.database.model.JenisKegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.Perkuliahan;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmuser;
import ais.database.model.employ.GajiPokok;
import ais.database.model.file.LampiranLain;
import ais.database.model.kpi.PenilaianKpi;
import ais.database.model.payroll.FormatItemGaji;
import ais.database.model.payroll.JenisGajiPegawai;
import ais.database.model.payroll.KelompokParameterTambahanGajiPegawai;
import ais.database.model.payroll.ParameterTambahanGajiPegawai;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.JenisCatatanGuru;
import ais.database.model.sekolah.JenisCatatanKelasSiswa;
import ais.database.model.sekolah.JenisCatatanSiswa;
import ais.database.model.sekolah.JenisRaporSiswa;
import ais.database.model.sekolah.KelasLesSiswaPunyaSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.WaktuUtil;

/**
 * Penyusun/penyaji laporan untuk laporan api. Kelas ini mengubah data domain menjadi bentuk
 * laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke lapisan
 * report.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pelaporan/ekspor ({@code reportUrl()}); operasi domain lain
 * ({@code laporan_absen()}, {@code raport_siswa()}, {@code catatan_guru()}, {@code catatan_kelas_siswa()},
 * {@code catatan_pegawai()}, {@code catatan_administrasi()}). Bagian lain dari kontrak tetap mengikuti kelas
 * induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class LaporanApi {

	/**
	 * Membentuk URL laporan dari host request yang sedang aktif.
	 *
	 * <p>{@link Common#CURRENT_URL} merupakan state global proses dan dapat berisi
	 * host tenant/request lain pada instalasi multi-tenant. Mengembalikan URL dari
	 * nilai global tersebut membuat aplikasi desktop/mobile membuka halaman utama
	 * atau login tenant yang salah. URL laporan harus selalu mengikuti request API
	 * yang baru saja menghasilkan berkas.</p>
	 */
	private static String reportUrl(HttpServletRequest req, File file) throws Exception {
		if (!Common.pakaiDirReportTergabung()) {
			return ApiHelperSupport.absoluteUrl(req,
					"/report/" + URLEncoder.encode(file.getName(), "UTF-8"));
		}
		return ApiHelperSupport.absoluteUrl(req,
				"/pdf?p=" + URLEncoder.encode(
						Common.desEncrypter.get().encrypt(file.getName()), "UTF-8"));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static JSONObject laporan_absen(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else if (tbmuser.getPegawai() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Harus login sebagai pegawai / karyawan / dosen / guru");
			} else {
				Calendar calendar = WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

				Date mulai = request.isNull("mulai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("mulai") + "");
				Date sampai = request.isNull("sampai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("sampai") + "");

				boolean senin = request.isNull("senin") ? true : request.getBoolean("senin");
				boolean selasa = request.isNull("selasa") ? true : request.getBoolean("selasa");
				boolean rabu = request.isNull("rabu") ? true : request.getBoolean("rabu");
				boolean kamis = request.isNull("kamis") ? true : request.getBoolean("kamis");
				boolean jumat = request.isNull("jumat") ? true : request.getBoolean("jumat");
				boolean sabtu = request.isNull("sabtu") ? true : request.getBoolean("sabtu");
				boolean minggu = request.isNull("minggu") ? true : request.getBoolean("minggu");

				MyCheckboxConfig[] haris = new MyCheckboxConfig[] { new MyCheckboxConfig(), new MyCheckboxConfig(),
						new MyCheckboxConfig(), new MyCheckboxConfig(), new MyCheckboxConfig(), new MyCheckboxConfig(),
						new MyCheckboxConfig() };

				haris[0].setChecked(senin);
				haris[1].setChecked(selasa);
				haris[2].setChecked(rabu);
				haris[3].setChecked(kamis);
				haris[4].setChecked(jumat);
				haris[5].setChecked(sabtu);
				haris[6].setChecked(minggu);

				int hr = 1;
				for (MyCheckboxConfig hariCheckbox : haris) {
					hariCheckbox.setAttribute("hari", hr++);
				}

				List<Pegawai> pegawais = new ArrayList<Pegawai>();
				pegawais.add(tbmuser.getPegawai());
				List maps = LaporanAbsensiPegawaiPerOrang.data(pegawais, mulai, sampai, null, haris, null);

				Map map = ais.common.HashMapGenerator.getRand();

				int i = 0;
				for (MyCheckboxConfig checkbox : haris) {
					Integer hari = (Integer) checkbox.getAttribute("hari");
					map.put("hari" + i, checkbox.isChecked() ? hari : -1);
					i++;
				}
				if (maps != null) {
					map.put("maps", maps);
				}
				map.put("mulai", Common.dateFormat1.get().format(mulai));
				map.put("sampai", Common.dateFormat1.get().format(sampai));

				String namaFile = "Laporan_Absensi_Per_Pegawai";
				Toolbar toolbar = null;
				File file = Report.generateFileReport(Report.PDF, map, namaFile, ais.ui.util.WaktuUtil.getDate(),
						toolbar);

				if (file == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "File laporan tidak bisa di cetak");
				} else {
					String path = reportUrl(req, file);
					jsonObject.put("url", path);
					jsonObject.put("status", "00");
					jsonObject.put("description", "OK");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:171");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject raport_siswa(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else if (tbmuser.getSiswa() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Harus login sebagai siswa");
			} else {
				JenisRaporSiswa j = (JenisRaporSiswa) (request.isNull("jenisRaporSiswa") ? null
						: ConstantValues.ambil(JenisRaporSiswa.class.getName(),
								Long.parseLong(request.get("jenisRaporSiswa") + "")));

				String ta = request.isNull("ta") ? Common.getCurrentTahunAkademik() : request.get("ta") + "";
				Integer smtas = request.isNull("smt") ? (Common.isNowSemensterGanjil() ? 1 : 2)
						: Integer.parseInt(request.get("smt") + "");
				String smta = smtas % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
				Sekolah s = tbmuser.ambilSekolah();
				Date tgl = WaktuUtil.getDate();

				List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = new ArrayList<KelasSiswaPunyaSiswa>();
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					try {
					kelasSiswaPunyaSiswas = ConstantValues
							.simpleList(
									session.createCriteria(KelasSiswaPunyaSiswa.class)
											.createAlias("kelasSiswa", "kelasSiswa")
											.add(Restrictions.eq("siswa", tbmuser.getSiswa()))
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("kelasSiswa.tahunAjaran", ta))
											.add(Restrictions.eq("kelasSiswa.aktif", true)).addOrder(Order.asc("id")),
									KelasSiswaPunyaSiswa.class);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} finally {
					// closeSessionQuietly(session) di finally sebelumnya (line ~216) sudah menutup
					// tuntas (clear+rollback+disconnect+close) session ini. Blok berikut adalah
					// pembersihan cadangan (defensive) untuk jalur lama -- cek isOpen() dulu supaya
					// benar-benar no-op bila session sudah tertutup, bukan cuma mengandalkan catch
					// di bawah untuk meredam SessionException ("Session is closed!"/"already closed").
					if (session != null && session.isOpen()) {
						try {
							try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:221");}
							session.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:223");
						}
						try {
							session.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:227");
						}
					}
				}

				if (kelasSiswaPunyaSiswas.isEmpty()) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Siswa ini belum punya kelas");
				} else {
					TreeMap<Long, KelasSiswaPunyaSiswa> map = new TreeMap<Long, KelasSiswaPunyaSiswa>();
					for (KelasSiswaPunyaSiswa kelasPunyaSiswa : kelasSiswaPunyaSiswas) {
						map.put(kelasPunyaSiswa.getId(), kelasPunyaSiswa);
					}

					File file = null;
					if (j != null) {
						LampiranLain lainMahasiswa = LampiranLain.ambil(j.getId(),
								LampiranLain.FILE_JRXML_LAYOUT_JENIS_RAPOR);

						if (lainMahasiswa == null) {
							jsonObject.put("status", "97");
							jsonObject.put("description", "File laporan rapor siswa belum diupload");
							return jsonObject;
						} else {
							file = Report.generateCompileFileReport(Report.PDF,
									LaporanRaporSiswa.generateParameter(map, j, tbmuser, ta, smtas, smta, s, tgl),
									lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
						}
					} else {
						String namaFile = "sekolah/report";
						Toolbar toolbar = null;
						file = Report.generateFileReport(Report.PDF,
								LaporanRaporSiswa.generateParameter(map, j, tbmuser, ta, smtas, smta, s, tgl), namaFile,
								ais.ui.util.WaktuUtil.getDate(), toolbar);
					}

					if (file == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan tidak bisa di cetak");
					} else {
						String path = reportUrl(req, file);
						jsonObject.put("url", path);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:280");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject catatan_guru(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Calendar calendar = WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

				Date mulai = request.isNull("mulai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("mulai") + "");
				Date sampai = request.isNull("sampai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("sampai") + "");

				String ta = request.isNull("ta") ? Common.getCurrentTahunAkademik() : request.get("ta") + "";
				Integer smtas = request.isNull("smt") ? (Common.isNowSemensterGanjil() ? 1 : 2)
						: Integer.parseInt(request.get("smt") + "");
				JenisCatatanGuru j = (JenisCatatanGuru) (request.isNull("jenisCatatanGuru") ? null
						: ConstantValues.ambil(JenisCatatanGuru.class.getName(),
								Long.parseLong(request.get("jenisCatatanGuru") + "")));

				if (j == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Jenis catatan guru harus di pilih");
				} else {
					LampiranLain lainMahaguru = LampiranLain.ambil(j.getId(),
							LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_GURU);

					if (lainMahaguru == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan catatan guru belum diupload");
					} else {
						File file = Report.generateCompileFileReport(Report.PDF,
								LaporanCatatanGuru.generateParameter(tbmuser.ambilGuru(), mulai, sampai, ta, smtas,
										null, j),
								lainMahaguru.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
						if (file == null) {
							jsonObject.put("status", "97");
							jsonObject.put("description", "File laporan tidak bisa di cetak");
						} else {
							String path = reportUrl(req, file);
							jsonObject.put("url", path);
							jsonObject.put("status", "00");
							jsonObject.put("description", "OK");
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:343");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject catatan_kelas_siswa(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Calendar calendar = WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

				Date mulai = request.isNull("mulai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("mulai") + "");
				Date sampai = request.isNull("sampai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("sampai") + "");

				JenisCatatanKelasSiswa j = (JenisCatatanKelasSiswa) (request.isNull("jenisCatatanKelasSiswa") ? null
						: ConstantValues.ambil(JenisCatatanKelasSiswa.class.getName(),
								Long.parseLong(request.get("jenisCatatanKelasSiswa") + "")));

				KelasSiswa kelasSiswa = (KelasSiswa) (request.isNull("kelasSiswa") ? null
						: ConstantValues.ambil(KelasSiswa.class.getName(),
								Long.parseLong(request.get("kelasSiswa") + "")));

				if (kelasSiswa == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Kelas Siswa harus di pilih");
				} else if (j == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Jenis catatan kelas siswa harus di pilih");
				} else {
					LampiranLain lainMahakelasSiswa = LampiranLain.ambil(j.getId(),
							LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_KELAS_SISWA);

					if (lainMahakelasSiswa == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan catatan kelas siswa belum diupload");
					} else {
						File file = Report.generateCompileFileReport(Report.PDF,
								LaporanCatatanKelasSiswa.generateParameter(kelasSiswa, mulai, sampai, null, j, null,
										null),
								lainMahakelasSiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
						if (file == null) {
							jsonObject.put("status", "97");
							jsonObject.put("description", "File laporan tidak bisa di cetak");
						} else {
							String path = reportUrl(req, file);
							jsonObject.put("url", path);
							jsonObject.put("status", "00");
							jsonObject.put("description", "OK");
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:410");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject catatan_pegawai(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else if (tbmuser.getPegawai() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Harus login sebagai pegawai / karyawan / dosen / guru");
			} else {
				Calendar calendar = WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

				Date mulai = request.isNull("mulai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("mulai") + "");
				Date sampai = request.isNull("sampai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("sampai") + "");

				JenisCatatanPegawai j = (JenisCatatanPegawai) (request.isNull("jenisCatatanPegawai") ? null
						: ConstantValues.ambil(JenisCatatanPegawai.class.getName(),
								Long.parseLong(request.get("jenisCatatanPegawai") + "")));

				if (j == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Jenis catatan pegawai harus di pilih");
				} else {
					LampiranLain lainMahapegawai = LampiranLain.ambil(j.getId(),
							LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_PEGAWAI);

					if (lainMahapegawai == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan catatan pegawai belum diupload");
					} else {
						File file = Report.generateCompileFileReport(Report.PDF,
								LaporanCatatanPegawai.generateParameter(tbmuser.getPegawai(), mulai, sampai, null, j),
								lainMahapegawai.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

						if (file == null) {
							jsonObject.put("status", "97");
							jsonObject.put("description", "File laporan tidak bisa di cetak");
						} else {
							String path = reportUrl(req, file);
							jsonObject.put("url", path);
							jsonObject.put("status", "00");
							jsonObject.put("description", "OK");
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:473");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject catatan_administrasi(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Calendar calendar = WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

				Date mulai = request.isNull("mulai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("mulai") + "");
				Date sampai = request.isNull("sampai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("sampai") + "");

				JenisCatatanAdministrasi j = (JenisCatatanAdministrasi) (request.isNull("jenisCatatanAdministrasi")
						? null
						: ConstantValues.ambil(JenisCatatanAdministrasi.class.getName(),
								Long.parseLong(request.get("jenisCatatanAdministrasi") + "")));

				if (j == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Jenis catatan administrasi harus di pilih");
				} else {
					LampiranLain lainMahaadministrasi = LampiranLain.ambil(j.getId(),
							LampiranLain.FILE_JRXML_LAYOUT_JENIS_CATATAN_ADMINISTRASI);

					if (lainMahaadministrasi == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan catatan administrasi belum diupload");
					} else {
						File file = Report.generateCompileFileReport(Report.PDF,
								LaporanCatatanAdministrasi.generateParameter(mulai, sampai, null, j),
								lainMahaadministrasi.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

						if (file == null) {
							jsonObject.put("status", "97");
							jsonObject.put("description", "File laporan tidak bisa di cetak");
						} else {
							String path = reportUrl(req, file);
							jsonObject.put("url", path);
							jsonObject.put("status", "00");
							jsonObject.put("description", "OK");
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:534");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject catatan_siswa(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Calendar calendar = WaktuUtil.getCalendar();
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

				Date mulai = request.isNull("mulai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("mulai") + "");
				Date sampai = request.isNull("sampai") ? calendar.getTime()
						: Common.dateFormat1.get().parse(request.get("sampai") + "");

				JenisCatatanSiswa j = (JenisCatatanSiswa) (request.isNull("jenisCatatanSiswa") ? null
						: ConstantValues.ambil(JenisCatatanSiswa.class.getName(),
								Long.parseLong(request.get("jenisCatatanSiswa") + "")));

				if (j == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Jenis catatan siswa harus di pilih");
				} else {
					LampiranLain lainMahasiswa = LampiranLain.ambil(j.getId(),
							LampiranLain.FILE_JRXML_LAYOUT_JENIS_RAPOR);

					if (lainMahasiswa == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan catatan siswa belum diupload");
					} else {
						String ta = request.isNull("ta") ? Common.getCurrentTahunAkademik() : request.get("ta") + "";
						Integer smtas = request.isNull("smt") ? (Common.isNowSemensterGanjil() ? 1 : 2)
								: Integer.parseInt(request.get("smt") + "");

						File file = Report.generateCompileFileReport(Report.PDF,
								LaporanCatatanSiswa.generateParameter(tbmuser.getSiswa(), mulai, sampai, ta, smtas,
										null, j),
								lainMahasiswa.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());

						if (file == null) {
							jsonObject.put("status", "97");
							jsonObject.put("description", "File laporan tidak bisa di cetak");
						} else {
							String path = reportUrl(req, file);
							jsonObject.put("url", path);
							jsonObject.put("status", "00");
							jsonObject.put("description", "OK");
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:599");
			}
		}
		return jsonObject;
	}

	public static JSONObject khs(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else if (tbmuser.getMahasiswa() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Harus login sebagai mahasiswa");
			} else {
				boolean hitungUlang = request.isNull("hitungUlang") ? false : request.getBoolean("hitungUlang");
				boolean remedial = request.isNull("remedial") ? false : request.getBoolean("remedial");
				Integer smtas = request.isNull("smt") ? tbmuser.getMahasiswa().currentSemester()
						: Integer.parseInt(request.get("smt") + "");

				Integer semesterPendek = request.isNull("semesterPendek")
						|| request.get("semesterPendek").toString().equals("0")
						|| request.get("semesterPendek").toString().equalsIgnoreCase("false") ? null
								: Perkuliahan.SEMESTER_PENDEK;
				Date tgl = WaktuUtil.getDate();
				Mahasiswa mahasiswa = tbmuser.getMahasiswa();
				List<String> warnings = LaporanApi.warningsKhs(mahasiswa,
						smtas == null ? mahasiswa.currentSemester() : smtas,
						semesterPendek != null ? Perkuliahan.SEMESTER_PENDEK : null, false, true, true);

				if (!warnings.isEmpty()) {
					// OPTIMASI: Menggunakan StringBuilder daripada rentetan w += untuk manipulasi
					// string
					StringBuilder w = new StringBuilder();
					for (String wa : warnings) {
						if (w.length() > 0)
							w.append("\n\n");
						w.append(wa);
					}
					jsonObject.put("status", "97");
					jsonObject.put("description", w.toString());
				} else {
					@SuppressWarnings("rawtypes")
					Map map = LaporanKHS.generateParameter(tbmuser.getMahasiswa(), smtas, hitungUlang, semesterPendek,
							remedial, tgl, tgl);

					if (map == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "Data KHS tidak ditemukan untuk periode yang diminta");
					} else {
						String namaFile = "Kartu_Hasil_Studi";
						Toolbar toolbar = null;
						File file = Report.generateFileReport(Report.PDF, map, namaFile, ais.ui.util.WaktuUtil.getDate(),
								toolbar);
						if (file == null) {
							jsonObject.put("status", "97");
							jsonObject.put("description", "File laporan tidak bisa di cetak");
						} else {
							String path = reportUrl(req, file);
							jsonObject.put("url", path);
							jsonObject.put("status", "00");
							jsonObject.put("description", "OK");
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:668");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static List<String> warningsKhs(Mahasiswa mahasiswa, Integer smtas, Integer semesterPendek, boolean remedial,
			boolean chekkonfigurasi, boolean syarat) throws Exception {
		List<String> warnings = new ArrayList<String>();
		if (mahasiswa != null) {
			try {
				NilaiValidator.checkBolehLihatNilai(mahasiswa, smtas, false, warnings);

				boolean boleh_cetak_khs = Common.bolehKonfigurasi("mahasiswa_boleh_melihat_khs_sendiri");
				try {
					if (!boleh_cetak_khs) {
						warnings.add(
								"Mahasiswa tidak diperkenankan mencetak KHS sendiri, hubungi admin untuk informasi lebih lanjut");
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				Session session = null;
				try {
					session = HibernateUtil.openSession();
					try {

					List<String> alasans = session.createCriteria(BlokirMahasiswa.class)
							.add(Restrictions.isNotNull("keterangan")).add(Restrictions.ne("keterangan", ""))
							.setProjection(Projections.property("keterangan"))
							.add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("nilai", true)).list();

					if (!alasans.isEmpty()) {
						StringBuilder alas = new StringBuilder();
						for (String s : alasans) {
							if (alas.length() > 0)
								alas.append("\n\n");
							alas.append(s);
						}
						warnings.add(alas.toString());
					}

					if (syarat) {
						List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
								session.createCriteria(SyaratUjian.class).add(Restrictions.eq("nilai", true)).add(
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
								SyaratUjian.class);

						for (SyaratUjian syaratUjian : syaratUjians) {
							SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, smtas, "Cetak KHS",
									warnings);
						}
					}
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} finally {
					// closeSessionQuietly(session) di finally sebelumnya sudah menutup tuntas
					// (clear+rollback+disconnect+close) session ini. Blok berikut adalah
					// pembersihan cadangan (defensive) untuk jalur lama -- cek isOpen() dulu supaya
					// benar-benar no-op bila session sudah tertutup, bukan cuma mengandalkan catch
					// di bawah untuk meredam SessionException ("Session is closed!"/"already closed").
					if (session != null && session.isOpen()) {
						try {
							try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:731");}
							session.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:733");
						}
						try {
							session.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:737");
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/LaporanApi.java:742");
			}
		}
		return warnings;
	}

	@SuppressWarnings("unchecked")
	public static List<String> warningsUas(Mahasiswa mahasiswa, Integer smt) throws Exception {
		List<String> warnings = new ArrayList<String>();
		if (mahasiswa != null && smt != null) {
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				try {
				List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
						session.createCriteria(SyaratUjian.class).add(Restrictions.eq("uas", true))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						SyaratUjian.class);

				for (SyaratUjian syaratUjian : syaratUjians) {
					SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, smt, "Cetak Kartu UAS",
							warnings);
				}
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			} finally {
				if (session != null) {
					try {
						try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:771");}
						session.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:773");
					}
					try {
						session.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:777");
					}
				}
			}
		}
		return warnings;
	}

	@SuppressWarnings("unchecked")
	public static List<String> warningsUts(Mahasiswa mahasiswa, Integer smt) throws Exception {
		List<String> warnings = new ArrayList<String>();
		if (mahasiswa != null && smt != null) {
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				try {
				List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
						session.createCriteria(SyaratUjian.class).add(Restrictions.eq("uts", true))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						SyaratUjian.class);

				for (SyaratUjian syaratUjian : syaratUjians) {
					SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, smt, "Cetak Kartu UTS",
							warnings);
				}
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			} finally {
				if (session != null) {
					try {
						try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:808");}
						session.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:810");
					}
					try {
						session.close();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:814");
					}
				}
			}
		}
		return warnings;
	}

	@SuppressWarnings("unchecked")
	public static List<String> warningsKrs(Mahasiswa mahasiswa, Integer smtas, Integer semesterPendek, boolean remedial,
			boolean chekkonfigurasi) throws Exception {

		List<String> warnings = new ArrayList<String>();
		Integer tahapan = null;
		if (mahasiswa != null) {
			Session session = null;
			try {
				session = HibernateUtil.openSession();
				try {
				if (chekkonfigurasi) {
					Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
					Integer semesterMulai = 0;
					Integer tahunAkademikMulai = Common.getTahunAkademik(smtas, tahunAngkatanMhs, semesterMulai,
							mahasiswa.getSemesterMulai());
					String tahunAkademik = tahunAkademikMulai + "/" + (tahunAkademikMulai + 1);

					Konfigurasi konfigurasi;
					if (Common.bolehKonfigurasi("input_krs_harus_berdasarkan_kalender_akademik")) {
						konfigurasi = Common.checkKonfigurasiDenganKalenderAkademik(session,
								remedial ? Konfigurasi.KRS_REMEDIAL
										: semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP,
								tahunAkademik, smtas % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
								mahasiswa.getSemesterMulai(), mahasiswa.getJurusan().getFakultas(),
								mahasiswa.getJurusan(), mahasiswa.getProgram());

					} else {
						konfigurasi = Common.getKonfigurasi(
								remedial ? Konfigurasi.KRS_REMEDIAL
										: semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP,
								tahunAkademik, smtas % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
					}

					if (konfigurasi != null && konfigurasi.getNilai() != null
							&& !konfigurasi.getNilai().trim().equals(Konfigurasi.AKTIF)) {
						warnings.add("Tidak ada jadwal pengambilan KRS");
					}
				}

				if (semesterPendek == null) {
					Konfigurasi konfigurasi = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs",
							Konfigurasi.AKTIF);

					if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {
						if (!Common.checkStatusPembayaranMahasiswa(smtas, tahapan, mahasiswa, false,
								semesterPendek != null)) {
							if (smtas != null && smtas.intValue() >= 1) {
								warnings.add("Anda belum membayar biaya perkuliahan di semester " + smtas
										+ ". Ambillah KRS yang baru saja anda lakukan pembayaran. Harap hubungi bagian keuangan untuk informasi lebih lanjut");
							}
						}

						if (smtas != null
								&& !UtsDanUasCheckerHelper.checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, smtas, tahapan)) {
							if (ais.common.CommonHelperClass.jenisKegiatansUntukKrs == null) {
								Common.reloadJenisKegiatans();
							}
							StringBuilder n = new StringBuilder();
							for (JenisKegiatan jenisKegiatan : ais.common.CommonHelperClass.jenisKegiatansUntukKrs) {
								if (n.length() == 0)
									n.append(jenisKegiatan.getNamaKegiatan());
								else
									n.append(", atau ").append(jenisKegiatan.getNamaKegiatan());
							}

							try {
								warnings.add("Mahasiswa dengan nim " + mahasiswa.getNim()
										+ " belum bisa KRS, karena belum melakukan pembayaran di semester " + smtas
										+ "\n\nJenis pembayaran yang harus dibayar antara lain " + n.toString());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:892");
							}
						}
					}
				} else {
					Konfigurasi konfigurasi = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs_sp",
							Konfigurasi.AKTIF);

					if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {
						if (!Common.checkStatusPembayaranMahasiswa(smtas, tahapan, mahasiswa, false,
								semesterPendek != null)) {
							warnings.add("Anda belum membayar biaya perkuliahan semester pendek di semester " + smtas
									+ ". Ambillah KRS semester pendek yang baru saja anda lakukan pembayaran. Harap hubungi bagian keuangan untuk informasi lebih lanjut");
						}
					}
				}

				if (semesterPendek == null) {
					if (!Common.checkStatusPembayaranMahasiswaSebelumnya(smtas, tahapan, mahasiswa)) {
						Double harusLunas = 90.0;
						try {
							harusLunas = Double.parseDouble(Common.getKonfigurasi(
									"batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_mengisi_krs", "90")
									.getNilai().trim());
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:916");
						}
						warnings.add("\"" + mahasiswa.getNama() + "\" belum melunasi " + harusLunas
								+ "% biaya perkuliahan di  semester " + (smtas - 1)
								+ ". Harap mahasiswa tsb untuk segera menghubungi bagian keuangan");
					}
				}

				List<String> alasans = session.createCriteria(BlokirMahasiswa.class)
						.add(Restrictions.isNotNull("keterangan")).add(Restrictions.ne("keterangan", ""))
						.setProjection(Projections.property("keterangan")).add(Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("krs", true)).list();
				if (!alasans.isEmpty()) {
					StringBuilder alas = new StringBuilder();
					for (String s : alasans) {
						if (alas.length() > 0)
							alas.append("\n\n");
						alas.append(s);
					}
					warnings.add(alas.toString());
				}

				List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
						session.createCriteria(SyaratUjian.class).add(Restrictions.eq("krs", true))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						SyaratUjian.class);

				for (SyaratUjian syaratUjian : syaratUjians) {
					SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, smtas, "Cetak KRS",
							warnings);
				}
				} finally {
					// Penutupan TUNTAS (clear+rollback+disconnect+close) sudah dilakukan di sini,
					// dan closeSessionQuietly() SUDAH menjaga isOpen() sebelum bertindak. JANGAN
					// menambah clear()/disconnect()/close() manual lagi di finally luar (di bawah) —
					// itu akan memanggil ulang operasi pada session yang sudah tertutup dan memicu
					// SessionException berantai ("Session is closed!" lalu "Session was already
					// closed") yang menutupi exception asli penyebab kegagalan.
					HibernateUtil.closeSessionQuietly(session);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/LaporanApi.java:952");
			}
		}

		if (Common.bolehKonfigurasi("saat_cetak_krs_harus_telah_disetujui", Konfigurasi.TIDAK_AKTIF)) {

			List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan();
			int jml = 0;
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getPerkuliahan() != null
							&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI)) {
						if ((remedial && detailperkuliahan.getPerkuliahan().getMerupakanRemedial())
								|| (!remedial && !detailperkuliahan.getPerkuliahan().getMerupakanRemedial())) {

							if (detailperkuliahan.getSemester().equals(smtas)) {
								if ((semesterPendek == null
										&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() == null)
										|| (semesterPendek != null
												&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek() != null
												&& detailperkuliahan.getPerkuliahan().getStatusSemesterPendek()
														.equals(semesterPendek))) {
									jml++;
								}
							}
						}
					}
				}
			}

			if (jml > 0) {
				warnings.add(
						"Anda belum bisa mencetak KRS, karena terdapat " + jml + " perkuliahan yang belum disetujui");
			}
		}

		return warnings;
	}

	public static JSONObject krs(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else if (tbmuser.getMahasiswa() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Harus login sebagai mahasiswa");
			} else {
				boolean hitungUlang = request.isNull("hitungUlang") ? false : request.getBoolean("hitungUlang");
				boolean remedial = request.isNull("remedial") ? false : request.getBoolean("remedial");
				Integer smtas = request.isNull("smt") ? tbmuser.getMahasiswa().currentSemester()
						: Integer.parseInt(request.get("smt") + "");

				Integer semesterPendek = request.isNull("semesterPendek")
						|| request.get("semesterPendek").toString().equals("0")
						|| request.get("semesterPendek").toString().equalsIgnoreCase("false") ? null
								: Perkuliahan.SEMESTER_PENDEK;
				Date tgl = WaktuUtil.getDate();
				Mahasiswa mahasiswa = tbmuser.getMahasiswa();

				List<String> warnings = LaporanApi.warningsKrs(mahasiswa, smtas, semesterPendek, remedial, false);
				if (!warnings.isEmpty()) {
					StringBuilder w = new StringBuilder();
					for (String wa : warnings) {
						if (w.length() > 0)
							w.append("\n\n");
						w.append(wa);
					}
					jsonObject.put("status", "97");
					jsonObject.put("description", w.toString());
				} else {
					@SuppressWarnings("rawtypes")
					Map map = LaporanKRS.generateParameter(tbmuser.getMahasiswa(), smtas, hitungUlang, semesterPendek,
							remedial, tgl, tgl, false, false);

					String namaFile = "Cetak_KRS_Mahasiswa";
					Toolbar toolbar = null;
					File file = Report.generateFileReport(Report.PDF, map, namaFile, ais.ui.util.WaktuUtil.getDate(),
							toolbar);
					if (file == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan tidak bisa di cetak");
					} else {
						String path = reportUrl(req, file);
						jsonObject.put("url", path);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:1064");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject uts(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else if (tbmuser.getMahasiswa() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Harus login sebagai mahasiswa");
			} else {
				boolean hitungUlang = request.isNull("hitungUlang") ? false : request.getBoolean("hitungUlang");
				boolean remedial = request.isNull("remedial") ? false : request.getBoolean("remedial");
				Integer smtas = request.isNull("smt") ? tbmuser.getMahasiswa().currentSemester()
						: Integer.parseInt(request.get("smt") + "");

				Integer semesterPendek = request.isNull("semesterPendek")
						|| request.get("semesterPendek").toString().equals("0")
						|| request.get("semesterPendek").toString().equalsIgnoreCase("false") ? null
								: Perkuliahan.SEMESTER_PENDEK;
				Date tgl = WaktuUtil.getDate();

				List<String> warnings = LaporanApi.warningsUts(tbmuser.getMahasiswa(), smtas);
				if (!warnings.isEmpty()) {
					StringBuilder w = new StringBuilder();
					for (String wa : warnings) {
						if (w.length() > 0)
							w.append("\n\n");
						w.append(wa);
					}
					jsonObject.put("status", "97");
					jsonObject.put("description", w.toString());
				} else {
					@SuppressWarnings("rawtypes")
					Map map = LaporanKRS.generateParameter(tbmuser.getMahasiswa(), smtas, hitungUlang, semesterPendek,
							remedial, tgl, tgl, true, false);

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(tbmuser.getMahasiswa(), smtas, null,
							semesterPendek, hitungUlang);
					map.put("nomor_ujian", krsMahasiswa.getNoUts());

					String namaFile = "Cetak_KUTS_Mahasiswa";
					Toolbar toolbar = null;
					File file = Report.generateFileReport(Report.PDF, map, namaFile, ais.ui.util.WaktuUtil.getDate(),
							toolbar);
					if (file == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan tidak bisa di cetak");
					} else {
						String path = reportUrl(req, file);
						jsonObject.put("url", path);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:1133");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject uas(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else if (tbmuser.getMahasiswa() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Harus login sebagai mahasiswa");
			} else {
				boolean hitungUlang = request.isNull("hitungUlang") ? false : request.getBoolean("hitungUlang");
				boolean remedial = request.isNull("remedial") ? false : request.getBoolean("remedial");
				Integer smtas = request.isNull("smt") ? tbmuser.getMahasiswa().currentSemester()
						: Integer.parseInt(request.get("smt") + "");

				Integer semesterPendek = request.isNull("semesterPendek")
						|| request.get("semesterPendek").toString().equals("0")
						|| request.get("semesterPendek").toString().equalsIgnoreCase("false") ? null
								: Perkuliahan.SEMESTER_PENDEK;
				Date tgl = WaktuUtil.getDate();

				List<String> warnings = LaporanApi.warningsUts(tbmuser.getMahasiswa(), smtas);
				if (!warnings.isEmpty()) {
					StringBuilder w = new StringBuilder();
					for (String wa : warnings) {
						if (w.length() > 0)
							w.append("\n\n");
						w.append(wa);
					}
					jsonObject.put("status", "97");
					jsonObject.put("description", w.toString());
				} else {
					@SuppressWarnings("rawtypes")
					Map map = LaporanKRS.generateParameter(tbmuser.getMahasiswa(), smtas, hitungUlang, semesterPendek,
							remedial, tgl, tgl, false, true);

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(tbmuser.getMahasiswa(), smtas, null,
							semesterPendek, hitungUlang);
					map.put("nomor_ujian", krsMahasiswa.getNoUas());

					String namaFile = "Cetak_KUAS_Mahasiswa";
					Toolbar toolbar = null;
					File file = Report.generateFileReport(Report.PDF, map, namaFile, ais.ui.util.WaktuUtil.getDate(),
							toolbar);
					if (file == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan tidak bisa di cetak");
					} else {
						String path = reportUrl(req, file);
						jsonObject.put("url", path);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:1202");
			}
		}
		return jsonObject;
	}

	public static JSONObject transkrip(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else if (tbmuser.getMahasiswa() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Harus login sebagai mahasiswa");
			} else {
				boolean hitungUlang = request.isNull("hitungUlang") ? false : request.getBoolean("hitungUlang");
				Integer smtas = request.isNull("smt") ? tbmuser.getMahasiswa().currentSemester()
						: Integer.parseInt(request.get("smt") + "");

				Date tgl = WaktuUtil.getDate();
				Mahasiswa mahasiswa = tbmuser.getMahasiswa();

				List<String> warnings = LaporanApi.warningsKhs(mahasiswa,
						smtas == null ? mahasiswa.currentSemester() : smtas, null, false, true, false);
				if (!warnings.isEmpty()) {
					StringBuilder w = new StringBuilder();
					for (String wa : warnings) {
						if (w.length() > 0)
							w.append("\n\n");
						w.append(wa);
					}
					jsonObject.put("status", "97");
					jsonObject.put("description", w.toString());
				} else {
					@SuppressWarnings("rawtypes")
					Map map = LaporanTranskipAkademik.generateParameter(tbmuser.getMahasiswa(), smtas, hitungUlang,
							false, tgl, tgl);

					String namaFile = "Transkrip_Akademik";
					Toolbar toolbar = null;
					File file = Report.generateFileReport(Report.PDF, map, namaFile, ais.ui.util.WaktuUtil.getDate(),
							toolbar);
					if (file == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan tidak bisa di cetak");
					} else {
						String path = reportUrl(req, file);
						jsonObject.put("url", path);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:1263");
			}
		}
		return jsonObject;
	}

	public static JSONObject ipk(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else if (tbmuser.getMahasiswa() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Harus login sebagai mahasiswa");
			} else {
				boolean hitungUlang = request.isNull("hitungUlang") ? false : request.getBoolean("hitungUlang");
				Integer smtas = request.isNull("smt") ? tbmuser.getMahasiswa().currentSemester()
						: Integer.parseInt(request.get("smt") + "");

				Date tgl = WaktuUtil.getDate();
				Mahasiswa mahasiswa = tbmuser.getMahasiswa();

				List<String> warnings = LaporanApi.warningsKhs(mahasiswa,
						smtas == null ? mahasiswa.currentSemester() : smtas, null, false, true, false);
				if (!warnings.isEmpty()) {
					StringBuilder w = new StringBuilder();
					for (String wa : warnings) {
						if (w.length() > 0)
							w.append("\n\n");
						w.append(wa);
					}
					jsonObject.put("status", "97");
					jsonObject.put("description", w.toString());
				} else {
					@SuppressWarnings("rawtypes")
					Map map = LaporanRekamanNilai.generateParameter(tbmuser.getMahasiswa(), smtas, hitungUlang, tgl);

					String namaFile = "Rekaman_Nilai";
					Toolbar toolbar = null;
					File file = Report.generateFileReport(Report.PDF, map, namaFile, ais.ui.util.WaktuUtil.getDate(),
							toolbar);
					if (file == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan tidak bisa di cetak");
					} else {
						String path = reportUrl(req, file);
						jsonObject.put("url", path);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:1323");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject sertifikatKelasLes(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Siswa siswa = tbmuser.getSiswa();
				CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

				if (siswa == null) {
					if (calonSiswa != null && calonSiswa.getSiswa() != null) {
						siswa = calonSiswa.getSiswa();
					}
				}

				if (siswa != null) {
					Long kelasLesSiswaId = request.isNull("kelasLesSiswa") ? -1L
							: Long.parseLong(request.get("kelasLesSiswa") + "");

					Session session = null;
					KelasLesSiswaPunyaSiswa kelasLesSiswaPunyaSiswa = null;
					try {
						session = HibernateUtil.openSession();
						try {
						kelasLesSiswaPunyaSiswa = (KelasLesSiswaPunyaSiswa) ConstantValues.simpleObject(
								session.createCriteria(KelasLesSiswaPunyaSiswa.class)
										.add(Restrictions.eq("siswa", siswa))
										.add(Restrictions.eq("kelasLesSiswa.id", kelasLesSiswaId)).setMaxResults(1),
								KelasLesSiswaPunyaSiswa.class);
						} finally {
							HibernateUtil.closeSessionQuietly(session);
						}
					} finally {
						if (session != null) {
							try {
								try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1367");}
								session.disconnect();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1369");
							}
							try {
								session.close();
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1373");
							}
						}
					}

					if (kelasLesSiswaPunyaSiswa != null && kelasLesSiswaPunyaSiswa.getKelasLesSiswa() != null
							&& kelasLesSiswaPunyaSiswa.getKelasLesSiswa().getSertifikat() != null) {

						if (kelasLesSiswaPunyaSiswa.getAcc()) {
							@SuppressWarnings("rawtypes")
							Map parameters = SertifikatAction.mapSertifikat(kelasLesSiswaPunyaSiswa);

							LampiranLain lampiran = LampiranLain.ambil(
									kelasLesSiswaPunyaSiswa.getKelasLesSiswa().getSertifikat().getId(),
									LampiranLain.FILE_JRXML_LAYOUT_SERTIFIKAT);

							if (lampiran == null || lampiran.getId() == null) {
								jsonObject.put("status", "97");
								jsonObject.put("description", "File sertifikat belum di upload");
							} else {
								File file = Report.generateCompileFileReport(Report.PDF, parameters,
										lampiran.ambilFile().getAbsolutePath(), ais.ui.util.WaktuUtil.getDate());
								if (file == null) {
									jsonObject.put("status", "97");
									jsonObject.put("description", "File sertifikat tidak bisa di cetak");
								} else {
									String path = reportUrl(req, file);
									jsonObject.put("url", path);
									jsonObject.put("status", "00");
									jsonObject.put("description", "OK");
								}
							}
						} else {
							jsonObject.put("status", "90");
							jsonObject.put("description", "Anda dinyatakan belum lulus");
						}
					} else {
						jsonObject.put("status", "90");
						jsonObject.put("description", "Sertifikat tidak dimukan");
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Pengambilan data gagal, token harus sebagai siswa");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:1424");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static JSONObject struk(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else if (tbmuser.getMahasiswa() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Harus login sebagai mahasiswa");
			} else {
				Session session = null;
				PembayaranSiswa pembayaranSiswa = null;
				try {
					session = HibernateUtil.openSession();
					try {
					pembayaranSiswa = (PembayaranSiswa) session.createCriteria(PembayaranSiswa.class)
							.add(Restrictions.idEq(Long.parseLong(request.get("id") + ""))).uniqueResult();
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} finally {
					if (session != null) {
						try {
							try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1455");}
							session.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1457");
						}
						try {
							session.close();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1461");
						}
					}
				}

				if (pembayaranSiswa != null) {
					Map parameters = new HashMap();
					parameters.put("id_pembayaran", pembayaranSiswa.getId());
					parameters.put("id_sekolah", pembayaranSiswa.getSekolah().getId());
					parameters.put("id_bri", -1L);
					parameters.put("id_bni", -1L);
					parameters.put("id_bsi", -1L);
					parameters.put("id_va", -1L);

					String kode_transaksi = "0000000000000000000000" + pembayaranSiswa.getId();
					kode_transaksi = StringUtils.substring(kode_transaksi, kode_transaksi.length() - 8);
					parameters.put("kode_transaksi", kode_transaksi);

					parameters.put("waktu_cetak", Common.dateFormat1.get().format(pembayaranSiswa.getTanggal()));

					if (pembayaranSiswa.getCalonSiswa() != null) {
						Common.insertProperty(CalonSiswa.class, pembayaranSiswa.getCalonSiswa(), parameters, "");
					} else if (pembayaranSiswa.getSiswa() != null) {
						Common.insertProperty(Siswa.class, pembayaranSiswa.getSiswa(), parameters, "");
					}

					PembayaranSiswaUtil.dataPembayaran(pembayaranSiswa, null, null, null, null, parameters);

					String namaFile = "Rekaman_Nilai"; // Sesuai dengan kode aslinya
					Toolbar toolbar = null;
					File file = Report.generateFileReport(Report.PDF, parameters, namaFile,
							ais.ui.util.WaktuUtil.getDate(), toolbar);
					if (file == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan tidak bisa di cetak");
					} else {
						String path = reportUrl(req, file);
						jsonObject.put("url", path);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					}
				} else {
					jsonObject.put("status", "97");
					jsonObject.put("description", "File laporan tidak bisa di cetak");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:1513");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static JSONObject slip(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else if (tbmuser.getPegawai() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Harus login sebagai pegawai");
			} else {
				Integer tahun = Integer.parseInt((request.get("tahun") + "").trim());
				Integer bulan = Integer.parseInt((request.get("bulan") + "").trim());

				Pegawai pegawai = tbmuser.getPegawai();
				List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = new ArrayList<PembayaranGajiPunyaPegawai>();

				List<FormatItemGaji> formatGajis = pegawai.ambilFormatItemGajis();

				// FIX SessionException "Session was already closed": session ini SUDAH ditutup
				// sepenuhnya (clear+rollback+close, dgn cek isOpen()) oleh
				// HibernateUtil.closeSessionQuietly() di finally dalam. Blok finally LUAR yang
				// dulu ada di sini mencoba clear/disconnect/close ULANG session yang sama --
				// SELALU gagal krn session sudah tertutup, menghasilkan noise log setiap kali
				// endpoint ini dipanggil. Ditutup sekali saja, sesuai konvensi
				// openSession()/currentNativeSession() -> tutup di finally.
				Session session = null;
				try {
					session = HibernateUtil.openSession();
					pembayaranGajiPunyaPegawais = session.createCriteria(PembayaranGajiPunyaPegawai.class)
							.add(formatGajis.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("formatItemGaji", formatGajis))
							.add(Restrictions.eq("pegawai", pegawai)).createAlias("pembayaranGaji", "pembayaranGaji")
							.add(Restrictions.eq("pembayaranGaji.bulan", bulan + 1))
							.add(Restrictions.eq("pembayaranGaji.tahun", tahun)).list();
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}

				System.out.println("formatGajis -> " + formatGajis + ", pegawai -> " + pegawai
						+ ", pembayaranGajiPunyaPegawais -> " + pembayaranGajiPunyaPegawais.size());

				if (pembayaranGajiPunyaPegawais.isEmpty()) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Data gaji belum tersedia");
				} else {
					File filePdfBaru = new File(Common.ambilREAL_PATH_REPORT() + "/" + Common.getGeneratedBarCode() + ".pdf");
					PDFMergerUtility ut = new PDFMergerUtility();
					boolean hasFilesToMerge = false;

					for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {
						Map parameters = ais.common.HashMapGenerator.getRand();
						if (pembayaranGajiPunyaPegawai.getFormatItemGaji() != null) {
							Common.insertProperty(FormatItemGaji.class, pembayaranGajiPunyaPegawai.getFormatItemGaji(),
									parameters, "format", 1);
						}
						Common.insertProperty(Pegawai.class, pegawai, parameters, "pegawai", 1);
						Common.insertProperty(PembayaranGaji.class, pembayaranGajiPunyaPegawai.getPembayaranGaji(),
								parameters, "pembayaranGaji", 1);
						GajiPokok gajiPokok = pegawai.ambilGajiPokok(WaktuUtil.getDate());
						if (gajiPokok != null) {
							Common.insertProperty(GajiPokok.class, gajiPokok, parameters, "gp", 1);
						}

						for (Object o : ConstantValues.ambilBerdasarClass(PenilaianKpi.class).values()) {
							PenilaianKpi penilaianKpi = (PenilaianKpi) o;
							if (penilaianKpi.getPegawai().getId().equals(pegawai.getId())) {
								Common.insertProperty(PenilaianKpi.class, penilaianKpi, parameters,
										"kpi_" + penilaianKpi.getTa(), 1);
							}
						}

						parameters.put("tahun", tahun == null ? -1 : tahun);
						parameters.put("bulan", bulan == null ? -1 : bulan);
						try {
							parameters.put("nama_bulan", Common.BULAN[bulan == null ? -1 : bulan - 1]);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1603");
						}

						parameters.put("nama", pegawai.getNama());
						parameters.put("nip", pegawai.getKode());
						parameters.put("kode", pegawai.getKode());
						parameters.put("norek", pegawai.getNorek() + " - " + pegawai.getDitransferKe());
						parameters.put("tanggalMasuk", pegawai.getTanggalmasuk());
						parameters.put("status",
								pegawai.getStatusPegawai() == null ? "" : pegawai.getStatusPegawai().getNama());
						parameters.put("departemen",
								pegawai.getDepartemen() == null ? "" : pegawai.getDepartemen().getNama());

						parameters.put("maps", LaporanSlipGajiRealPegawaiPerOrang
								.generateDataDanImageAlbum(pembayaranGajiPunyaPegawai, parameters, bulan, tahun));

						try {
							JenisGajiPegawai j = pegawai.getJenisGajiPegawai();
							if (j != null) {
								Session sessionTmb = null;
								try {
									sessionTmb = HibernateUtil.openSession();
									try {
									sessionTmb.refresh(j);

									Set<KelompokParameterTambahanGajiPegawai> kelompokParameterTambahanGajiPegawais = new TreeSet<KelompokParameterTambahanGajiPegawai>();
									for (KelompokParameterTambahanGajiPegawai kelompokParameterTambahanGajiPegawai : j
											.getKelompokParameterTambahanGajiPegawais()) {
										kelompokParameterTambahanGajiPegawais.add(kelompokParameterTambahanGajiPegawai);
									}

									for (KelompokParameterTambahanGajiPegawai kelompokParameterTambahanGajiPegawai : kelompokParameterTambahanGajiPegawais) {

										List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
												sessionTmb.createCriteria(ParameterTambahanGajiPegawai.class)
														.add(Restrictions.eq("kelompokParameterTambahanGajiPegawai",
																kelompokParameterTambahanGajiPegawai))
														.createAlias("parameterTambahan", "parameterTambahan")
														.createAlias("kelompokParameterTambahanGajiPegawai",
																"kelompokParameterTambahanGajiPegawai")
														.add(Restrictions.eq("parameterTambahan.aktif", true))
														.add(Restrictions
																.eq("kelompokParameterTambahanGajiPegawai.aktif", true))
														.setProjection(
																Projections.groupProperty("parameterTambahan.id")),
												ParameterTambahan.class, false);

										if (!parameterTambahans.isEmpty()) {
											for (ParameterTambahan parameterTambahan : parameterTambahans) {
												String jenis = kelompokParameterTambahanGajiPegawai.getId() + "->"
														+ parameterTambahan.getId();

												String val = "";
												String ket = "";
												String[] spl = pegawai.getParameterTambahanInds().split("\n");
												for (String d : spl) {
													String[] value = d.split("<=>");
													if (value[0].trim().equalsIgnoreCase(jenis)) {
														val = value.length > 1 ? value[1].trim() : "";
														try {
															ket = value.length > 0 ? value[value.length - 1] : "";
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1664");
														}
													}
												}
												parameters.put(jenis, val);
												parameters.put(jenis + "_ket", ket);
											}
										}
									}
									} finally {
										HibernateUtil.closeSessionQuietly(sessionTmb);
									}
								} finally {
									if (sessionTmb != null) {
										try {
											try { sessionTmb.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1679");}
											sessionTmb.disconnect();
										} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1681");
										}
										try {
											sessionTmb.close();
										} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1685");
										}
									}
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1690");
						}

						String namaFile = "payroll/SlipGajiReal";
						Toolbar toolbar = null;
						File file = Report.generateFileReport(Report.PDF, parameters, namaFile,
								ais.ui.util.WaktuUtil.getDate(), toolbar);

						if (file != null) {
							ut.addSource(file);
							hasFilesToMerge = true;
						}
					}

					// OPTIMASI & BUG FIX: Pindahkan mergeDocuments ke bagian paling akhir setelah
					// semua file source ditambahkan
					if (hasFilesToMerge) {
						FileOutputStream fos = null;
						try {
							fos = new FileOutputStream(filePdfBaru);
							ut.setDestinationStream(fos);
							ut.mergeDocuments();
						} finally {
							if (fos != null) {
								try {
									fos.close();
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LaporanApi.java:1716");
								}
							}
						}
					}

					if (!hasFilesToMerge || filePdfBaru == null || !filePdfBaru.exists()) {
						jsonObject.put("status", "97");
						jsonObject.put("description", "File laporan tidak bisa di cetak");
					} else {
						String path = reportUrl(req, filePdfBaru);
						jsonObject.put("url", path);
						jsonObject.put("status", "00");
						jsonObject.put("description", "OK");
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LaporanApi.java:1739");
			}
		}
		return jsonObject;
	}
}
