package ais.action.servlet.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zul.Messagebox;

import ais.action.master.PengumumanAkademisAction;
import ais.action.master.SyaratUjianAction;
import ais.action.master.TampilanELearningAction;
import ais.action.master.helper.KrsUtilHelper;
import ais.action.master.helper.UtsDanUasCheckerHelper;
import ais.action.master.sekolah.helper.JadwalUtil;
import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.action.master.surat.SuratKeluarAction;
import ais.action.master.surat.SuratMasukAction;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPMB;
import ais.common.CommonPSB;
import ais.common.CommonPenilaian;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.VerifikasiPMBHtmlHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.FormulirKegiatan;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahPunyaBukuBahanAjar;
import ais.database.model.NamaTugasKelompok;
import ais.database.model.NilaiHuruf;
import ais.database.model.PembagianKuotaPerkuliahanBerdasarkantahunAngkatan;
import ais.database.model.Perkuliahan;
import ais.database.model.PerkuliahanPunyaItem;
import ais.database.model.Pertemuan;
import ais.database.model.Program;
import ais.database.model.Skripsi;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Statusabsensi;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.TugasFileContent;
import ais.database.model.inventory.Pedagang;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.AbsenPiket;
import ais.database.model.sekolah.AbsenPiketDetail;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.NilaiHurufSekolah;
import ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB;
import ais.database.model.sekolah.Siswa;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.WaktuUtil;

/**
 * Komponen batas HTTP/servlet untuk elearning api util. Tipe ini menerima input dari luar
 * aplikasi, meneruskannya ke layanan domain, lalu membentuk respons tanpa menduplikasi aturan
 * bisnis.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah pembacaan/pencarian ({@code daftarAmbilKrs()});
 * validasi/perhitungan ({@code alasanPedagangTidakBolehDihapus()}); mutasi data ({@code
 * update_nilai_mahasiswa()}, {@code update_absen()}, {@code simpanData()}, {@code simpanProperty()}, {@code
 * simpanDataBanyak()}, {@code simpanDataRinci()}); penghapusan/pembatalan ({@code hapusDataRinci()}, {@code
 * hapusDataRinci()}); operasi domain lain ({@code syaratKrs()}, {@code dataRinci()}, {@code ta()}, {@code
 * file()}, {@code current_smt()}, {@code daftar_nilai_mahasiswa()}). Bagian lain dari kontrak tetap mengikuti
 * kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class ElearningApiUtil {

	@SuppressWarnings({ "unchecked" })
	public static JSONObject syaratKrs(HttpServletRequest req, JSONObject request) { 
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				Integer semesterPendek = request.isNull("semesterPendek") ? null
						: Integer.parseInt(request.get("semesterPendek") + "");
				Integer semester = request.isNull("semester") ? null : Integer.parseInt(request.get("semester") + "");
				String tahunAjaran = request.isNull("tahunAjaran") ? null : request.get("tahunAjaran") + "";
				Boolean refresh = request.isNull("refresh") ? false : Boolean.parseBoolean(request.get("refresh") + "");
				Integer tahapan = request.isNull("tahapan") ? null : Integer.parseInt(request.get("tahapan") + "");

				Boolean remedial = request.isNull("remedial") ? false
						: Boolean.parseBoolean(request.get("remedial") + "");

				Mahasiswa mahasiswa = tbmuser.getMahasiswa();
				if (mahasiswa != null) {
					Session sessSyaratKrs = HibernateUtil.openSession();
					try {

					KrsMahasiswa krsMahasiswa = refresh
							? Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek, true)
							: Common.ambilKrsMahasiswaTanpaSinkronisasi(mahasiswa, semester, tahapan, semesterPendek);
					Dosen dosenPembimbingAkademik = krsMahasiswa.getDosenPa();

					Konfigurasi konfigurasi;
					Konfigurasi konfigurasiPerbaikan;
					if (Common.bolehKonfigurasi("input_krs_harus_berdasarkan_kalender_akademik")) {
						konfigurasi = Common.checkKonfigurasiDenganKalenderAkademik(sessSyaratKrs,
								remedial ? Konfigurasi.KRS_REMEDIAL
										: semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP,
								tahunAjaran, semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
								mahasiswa.getSemesterMulai(), mahasiswa.getJurusan().getFakultas(),
								mahasiswa.getJurusan(), mahasiswa.getProgram());

						konfigurasiPerbaikan = Common.checkKonfigurasiDenganKalenderAkademik(
								sessSyaratKrs,
								remedial ? Konfigurasi.PERBAIKAN_KRS_REMEDIAL
										: semesterPendek == null ? Konfigurasi.PERBAIKAN_KRS
												: Konfigurasi.PERBAIKAN_KRS_SP,
								tahunAjaran, semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
								mahasiswa.getSemesterMulai(), mahasiswa.getJurusan().getFakultas(),
								mahasiswa.getJurusan(), mahasiswa.getProgram());
					} else {
						konfigurasi = Common.getKonfigurasi(
								remedial ? Konfigurasi.KRS_REMEDIAL
										: semesterPendek == null ? Konfigurasi.KRS : Konfigurasi.KRS_SP,
								tahunAjaran, semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
						konfigurasiPerbaikan = Common.getKonfigurasi(
								remedial ? Konfigurasi.PERBAIKAN_KRS_REMEDIAL
										: semesterPendek == null ? Konfigurasi.PERBAIKAN_KRS
												: Konfigurasi.PERBAIKAN_KRS_SP,
								tahunAjaran, semester % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
					}

					List<String> warnings = new ArrayList<String>();
					if (mahasiswa != null) {
						List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
								sessSyaratKrs.createCriteria(SyaratUjian.class)
										.add(Restrictions.eq("krs", true)).add(Restrictions
												.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
								SyaratUjian.class);

						System.out.println("syaratUjians => " + syaratUjians);

						for (SyaratUjian syaratUjian : syaratUjians) {
							SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian, null, mahasiswa, semester,
									"Ambil KRS", warnings);
						}
					}
					if (!warnings.isEmpty()) {
						String w = "";
						for (String wa : warnings) {
							w += w.isEmpty() ? wa : "\n\n" + wa;
						}
						jsonObject.put("status", "89");
						jsonObject.put("description", w);
					} else {

						List<Long> detailperkuliahans = Common.getDetailperkuliahans(mahasiswa, semester, tahapan, null,
								semesterPendek, remedial, false, false, refresh);

						if (!(konfigurasi != null && konfigurasi.getNilai() != null
								&& konfigurasi.getNilai().equals(Konfigurasi.AKTIF))
								&& (konfigurasiPerbaikan != null && konfigurasiPerbaikan.getNilai() != null
										&& konfigurasiPerbaikan.getNilai().equals(Konfigurasi.AKTIF))
								&& detailperkuliahans.isEmpty()) {

							jsonObject.put("status", "89");
							jsonObject.put("description",
									"Anda belum pernah mengambil KRS, sehingga tidak bisa memperbaiki KRS. Harap segera menghubungi bagian Akademik atau Admin Fakultas atau Prodi untuk informasi lebih lanjut");

						} else {

							Konfigurasi konfigurasiDosenPembimbingAkademik = Common
									.getKonfigurasi("dosen_pa_harus_ada_sebelum_isi_krs", Konfigurasi.AKTIF);

							if (dosenPembimbingAkademik == null
									&& konfigurasiDosenPembimbingAkademik.getNilai().equals(Konfigurasi.AKTIF)) {

								jsonObject.put("status", "89");
								jsonObject.put("description",
										"Anda belum mempunyai dosen pembimbing akademik, sehingga tidak bisa mengambil KRS. Harap segera menghubungi bagian Akademik atau Admin Fakultas atau Prodi untuk mendaftarkan Dosen Pembimbing Akademik Anda");

							} else {
								String kelas = krsMahasiswa.getKelas();

								if (Common.bolehKonfigurasi("kelas_harus_ada_sebelum_isi_krs", Konfigurasi.TIDAK_AKTIF)
										&& (kelas == null || kelas.trim().isEmpty())) {

									jsonObject.put("status", "89");
									jsonObject.put("description",
											"Anda belum memiliki kelas, harap menghubungi bagian akademik");

								} else {

									boolean diizinkan = true;
									if (semesterPendek == null) {
										konfigurasi = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs",
												Konfigurasi.AKTIF);

										if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {
											if (!ConstantValues.aktifkanTahapanTerhubungKeKeuangan || tahapan == null
													|| tahapan.equals(0)) {
												if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa,
														false, semesterPendek != null)) {
													if (semester != null && semester.intValue() >= 1) {

														jsonObject.put("status", "89");
														jsonObject.put("description",
																"Anda belum membayar biaya perkuliahan di semester "
																		+ semester
																		+ (semesterPendek != null ? " semester pendek"
																				: "")
																		+ ". Ambillah KRS yang baru saja anda lakukan pembayaran. Harap hubungi bagian keuangan untuk informasi lebih lanjut");
														diizinkan = false;
													}
												}
											}

											if (!UtsDanUasCheckerHelper.checkPembayaranSebelumKRSSudahMemenuhi(
													mahasiswa, semester, tahapan)) {

												String n = "";
												for (JenisKegiatan jenisKegiatan : ais.common.CommonHelperClass.jenisKegiatansUntukKrs) {
													n += n.isEmpty() ? jenisKegiatan.getNamaKegiatan()
															: ", atau " + jenisKegiatan.getNamaKegiatan();
												}

												jsonObject.put("status", "89");
												jsonObject.put("description", "Mahasiswa dengan nim "
														+ mahasiswa.getNim() + " belum bisa " + " mengambil "
														+ " KRS, karena belum melakukan pembayaran di "
														+ ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan
																&& tahapan != null && tahapan > 0) ? " tahap " + tahapan
																		: " semester " + semester)
														+ "\n\nJenis pembayaran yang harus dibayar antara lain " + n);

												diizinkan = false;
											}
										}
									} else {
										konfigurasi = Common.getKonfigurasi("mahasiswa_harus_bayar_sebelum_isi_krs_sp",
												Konfigurasi.AKTIF);

										if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {
											if (!ConstantValues.aktifkanTahapanTerhubungKeKeuangan || tahapan == null
													|| tahapan.equals(0)) {
												if (!Common.checkStatusPembayaranMahasiswa(semester, tahapan, mahasiswa,
														false, semesterPendek != null)) {

													jsonObject.put("status", "89");
													jsonObject.put("description",

															"Anda belum membayar biaya perkuliahan semester pendek di semester "
																	+ semester
																	+ ". Ambillah KRS semester pendek yang baru saja anda lakukan pembayaran. Harap hubungi bagian keuangan untuk informasi lebih lanjut");
													diizinkan = false;
												}
											}
										}
									}

									if (diizinkan) {

										konfigurasi = Common.getKonfigurasi(
												"status_mahasiswa_harus_aktif_sebelum_isi_krs", Konfigurasi.AKTIF);

										if (konfigurasi.getNilai().equals(Konfigurasi.AKTIF)) {

											HistoryStatusMahasiswa historyStatusMahasiswa = Common
													.getHistoryStatusMahasiswa(krsMahasiswa);

											StatusMahasiswa statusMahasiswa = historyStatusMahasiswa
													.ambilStatusMahasiswa(semester);
											if (statusMahasiswa == null
													|| !statusMahasiswa.getId().equals(ConstantValues.AKTIF.getId())) {

												jsonObject.put("status", "89");
												jsonObject.put("description", "Status anda sedang "
														+ (statusMahasiswa == null ? "" : statusMahasiswa.getNama())
														+ ", anda tidak bisa mengambil KRS. Hubungi admin untuk informasi lebih lanjut");
												diizinkan = false;
											}
										}

										if (diizinkan) {

											if (semesterPendek == null) {
												if (!Common.checkStatusPembayaranMahasiswaSebelumnya(semester, tahapan,
														mahasiswa)) {
													Double harusLunas = 90.0;
													try {
														harusLunas = Double.parseDouble(Common.getKonfigurasi(
																"batas_terendah_persen_pembayaran_semester_yang_lalu_boleh_mengisi_krs",
																"90").getNilai().trim());
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:331");

													}

													jsonObject.put("status", "89");
													jsonObject.put("description", "Anda belum melunasi " + harusLunas
															+ "% biaya perkuliahan di "
															+ ((ConstantValues.aktifkanTahapanTerhubungKeKeuangan
																	&& tahapan != null && tahapan > 0)
																			? " tahap " + (tahapan - 1)
																			: " semester " + (semester - 1))
															+ ". Harap hubungi bagian keuangan untuk informasi lebih lanjut");
													diizinkan = false;

												}

											}

											Session session = HibernateUtil.openSession();
											try {

											List<String> alasans = session.createCriteria(BlokirMahasiswa.class)
													.add(Restrictions.isNotNull("keterangan"))
													.add(Restrictions.ne("keterangan", ""))
													.setProjection(Projections.property("keterangan"))
													.add(Restrictions.eq("mahasiswa", mahasiswa))
													.add(Restrictions.or(Restrictions.isNull("aktif"),
															Restrictions.eq("aktif", true)))
													.add(Restrictions.eq("krs", true)).list();
											// session.disconnect();
											ApiHelperSupport.closeOpenedSession(session);
											if (!alasans.isEmpty()) {

												String alas = "";
												for (String s : alasans) {
													alas += alas.isEmpty() ? s : "\n\n" + s;
												}

												try {
													MyMessageboxConfig.show(alas, "Informasi KRS", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);
												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:373");
												}

												jsonObject.put("status", "89");
												jsonObject.put("description", alas);
												diizinkan = false;
											}

											if (diizinkan) {
												jsonObject.put("status", "00");
												jsonObject.put("description", "Pengambilan KRS diizinkan");
											}
											} finally {
												HibernateUtil.closeSessionQuietly(session);
											}
										}
									}
								}
							}
						}
					}
					} finally {
						HibernateUtil.closeSessionQuietly(sessSyaratKrs);
					}
				} else {
					jsonObject.put("status", "89");
					jsonObject.put("description", "Harus login sebagai mahasiswa");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:408");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("rawtypes")
	public static JSONObject dataRinci(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Class clazz = Class.forName(request.getString("class"));
				Long id = Long.parseLong(String.valueOf(request.get("id")));
				int deep = request.isNull("deep") ? 6 : Integer.parseInt(String.valueOf(request.get("deep")));
				Session session = HibernateUtil.openSession();
				try {
				GeneralValueObject generalValueObject = ConstantValues
						.simpleObject(session.createCriteria(clazz).add(Restrictions.idEq(id)), clazz);
				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);

				if (generalValueObject != null) {

					JSONObject object = new JSONObject();
					Common.insertProperty(clazz, generalValueObject, object, "", deep);
					jsonObject.put("data", object);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Pengambilan data berhasil");
				} else {
					jsonObject.put("status", "00");
					jsonObject.put("description", "Pengambilan data berhasil, namun data tidak ditemukan");
				}
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:454");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({})
	public static JSONObject ta(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				JSONArray data = new JSONArray(Common.tahunAngkatans);
				jsonObject.put("data", data);
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:479");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({ "rawtypes" })
	public static JSONObject file(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Long ref = request.isNull("ref") ? -1L : Long.parseLong(request.getString("ref").trim());
				String jenis = request.isNull("jenis") ? "" : request.getString("jenis").trim();
				Class clazz = request.isNull("class") ? LampiranLain.class : Class.forName(request.getString("class"));

				FileFotoLain fileFotoLain = FileFotoLain.ambil(ref, jenis, clazz);
				if (fileFotoLain != null && fileFotoLain.getGdrive() != null && !fileFotoLain.getGdrive().isEmpty()) {
					jsonObject.put("url", fileFotoLain.exportGDriveUrl());
				} else {
					String link = FileFotoLain.ambilLinkLampiranLain(fileFotoLain, false, false, clazz);
					jsonObject.put("url", link);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:512");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({})
	public static JSONObject current_smt(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				try {
					String idSmt = Common.getCurrentTahunAkademik(tbmuser, WaktuUtil.getDate()).split("/")[0]
							+ (Common.isNowSemensterGanjil() ? "1" : "2");
					jsonObject.put("idSmt", idSmt);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Sukses");

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:536");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:546");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({})
	public static JSONObject daftar_nilai_mahasiswa(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				String nama = request.isNull("nama") ? "" : request.getString("nama");
				Boolean refresh = request.isNull("refresh") ? false : request.getBoolean("refresh");
				Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
						ais.common.CommonJSONUtil.ambilLong(request, "perkuliahan"), true);
				if (perkuliahan != null) {

					Boolean aktifPenilaian = Common.checkApakahDosenBolehMenilai(tbmuser.ambilDosen(), tbmuser,
							perkuliahan.getTahunAjaran(),
							perkuliahan.getStatusSemesterPendek() == null ? perkuliahan.getGanjilGenap()
									: Perkuliahan.SP);
					Konfigurasi konfigurasi = CommonPenilaian.getKonfigurasi(perkuliahan.getTahunAjaran(),
							perkuliahan.getGanjilGenap(), perkuliahan.getStatusSemesterPendek());

					List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan);
					Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahan(null, null, nama.trim(),
							true, refresh);

					JSONArray array = new JSONArray();
					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
						if (detailperkuliahan != null) {
							if (detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
								JSONObject object = new JSONObject();
								object.put("id", detailperkuliahan.getId());
								object.put("total_nilai", detailperkuliahan.getTotalNilai());
								object.put("huruf", detailperkuliahan.getNilaiHuruf());
								object.put("ip", detailperkuliahan.getTotalIP());
								object.put("nim", detailperkuliahan.getMahasiswa().getNim());
								object.put("nama", detailperkuliahan.getMahasiswa().getNama());
								object.put("prodi", detailperkuliahan.getMahasiswa().getJurusan().getNama());
								object.put("angkatan", detailperkuliahan.getMahasiswa().getTahunangkatan());

								String url = CommonMedia
										.getUrlFotoPengguna(new Tbmuser(detailperkuliahan.getMahasiswa()));
								object.put("foto", url);

								JSONArray arrayformatNilai = new JSONArray();
								for (FormatNilai formatNilai : formatNilais) {

									boolean kunci = perkuliahan.getDikunci() != null || formatNilai.getKunci() != null
											|| (formatNilai.getStatusPertemuan() != null
													&& formatNilai.getStatusPertemuan().getKunci())

											|| (!aktifPenilaian && (konfigurasi.getNilai() == null
													|| !konfigurasi.getNilai().equals(Konfigurasi.AKTIF)));
									JSONObject objectFormatNilai = new JSONObject();
									objectFormatNilai.put("kunci", kunci);
									objectFormatNilai.put("id", formatNilai.getId());
									objectFormatNilai.put("persen", formatNilai.getPersen());
									objectFormatNilai.put("status", formatNilai.getStatusPertemuan() == null ? ""
											: formatNilai.getStatusPertemuan().getNama());
									objectFormatNilai.put("nama", formatNilai.getNama());

									Double nilai = detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai);
									objectFormatNilai.put("nilai", nilai);
									arrayformatNilai.put(objectFormatNilai);
								}
								object.put("nilai", arrayformatNilai);
								array.put(object);
							}
						}
					}
					jsonObject.put("status", "00");
					jsonObject.put("description", "Pengambilan data berhasil");
					jsonObject.put("data", array);

				} else {
					jsonObject.put("status", "91");
					jsonObject.put("description", "Perkuliahan tidak ditemukan");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:640");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({})
	public static JSONObject update_nilai_mahasiswa(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Long detailperkuliahanid = request.isNull("detailperkuliahan") ? -1L
						: ais.common.CommonJSONUtil.ambilLong(request, "detailperkuliahan");
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				Long idFormat = request.isNull("format") ? null
						: ais.common.CommonJSONUtil.ambilLong(request, "format");
				Double nilai = request.isNull("nilai") ? null : request.getDouble("nilai");
				Boolean verify = request.isNull("verify") ? true : request.getBoolean("verify");
				if (detailperkuliahan != null && detailperkuliahan.getPerkuliahan() != null) {
					List<FormatNilai> formatNilais = Common.getFormatNilais(detailperkuliahan.getPerkuliahan());

					FormatNilai formatNilaia = null;
					for (FormatNilai f : formatNilais) {
						if (idFormat.equals(f.getId())) {
							formatNilaia = f;
							break;
						}
					}

					if (formatNilaia == null) {
						jsonObject.put("status", "97");
						jsonObject.put("description",
								"Format nilai tidak ditemukan, harap menghubungi bagian admin atau puskom");
					} else if (detailperkuliahan.apakahNilaiDikunci(formatNilaia)) {
						jsonObject.put("status", "97");
						jsonObject.put("description",
								"Nilai tidak diubah karena kolom penilaian telah dikunci di AIS");
					} else {

						Session session = HibernateUtil.openSession();
						try {
						session.refresh(detailperkuliahan);
						detailperkuliahan.populateDetailNilai(formatNilaia, null, nilai, verify, tbmuser);

						Double total = detailperkuliahan.hitungTotalNilai(true, formatNilais);

						Matakuliah matakuliah = detailperkuliahan == null ? null
								: detailperkuliahan.getPerkuliahan() != null
										? detailperkuliahan.getPerkuliahan().getMatakuliah()
										: detailperkuliahan.getMatakuliahKonversi();

						NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total,
								detailperkuliahan.getMahasiswa().getTahunangkatan(),
								detailperkuliahan.getMahasiswa().getJurusan(),
								detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
								detailperkuliahan.getTahunAkademik(),
								detailperkuliahan.getPerkuliahan() == null ? null
										: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
								matakuliah == null ? "" : matakuliah.getKode(),
								matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

						if (nilaiHuruf == null) {

							jsonObject.put("status", "97");
							jsonObject.put("description", "Nilai huruf untuk angka " + total
									+ " belum di konfigurasi secara sesuai, harap menghubungi bagian admin atau puskom");

						} else {

							detailperkuliahan.setTotalIP(nilaiHuruf.getNilaiDiIPK());
							detailperkuliahan.setTotalNilai(total);
							detailperkuliahan.setNilaiHuruf(nilaiHuruf.getNilaiHuruf());
							detailperkuliahan.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

							Double totalSementara = detailperkuliahan.hitungTotalNilaiSementara(true, formatNilais);
							nilaiHuruf = Common.getNilaiHuruf(totalSementara,
									detailperkuliahan.getMahasiswa().getTahunangkatan(),
									detailperkuliahan.getMahasiswa().getJurusan(),
									detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
									detailperkuliahan.getTahunAkademik(),
									detailperkuliahan.getPerkuliahan() == null ? null
											: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
									matakuliah == null ? "" : matakuliah.getKode(),
									matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

							detailperkuliahan.setTotalNilaiSementara(totalSementara);
							detailperkuliahan
									.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
							detailperkuliahan
									.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());

							session.getTransaction().begin();
							session.update(detailperkuliahan);
							session.getTransaction().commit();
						}

						// session.disconnect();
						ApiHelperSupport.closeOpenedSession(session);

						JSONObject object = new JSONObject();
						object.put("id", detailperkuliahan.getId());
						object.put("total_nilai", detailperkuliahan.getTotalNilai());
						object.put("huruf", detailperkuliahan.getNilaiHuruf());
						object.put("ip", detailperkuliahan.getTotalIP());
						object.put("nim", detailperkuliahan.getMahasiswa().getNim());
						object.put("nama", detailperkuliahan.getMahasiswa().getNama());
						object.put("prodi", detailperkuliahan.getMahasiswa().getJurusan().getNama());
						object.put("angkatan", detailperkuliahan.getMahasiswa().getTahunangkatan());

						String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(detailperkuliahan.getMahasiswa()));
						object.put("foto", url);

						JSONArray arrayformatNilai = new JSONArray();
						for (FormatNilai formatNilai : formatNilais) {
							JSONObject objectFormatNilai = new JSONObject();
							objectFormatNilai.put("id", formatNilai.getId());
							objectFormatNilai.put("persen", formatNilai.getPersen());
							objectFormatNilai.put("status", formatNilai.getStatusPertemuan() == null ? ""
									: formatNilai.getStatusPertemuan().getNama());
							objectFormatNilai.put("nama", formatNilai.getNama());

							nilai = detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai);
							objectFormatNilai.put("nilai", nilai);
							arrayformatNilai.put(objectFormatNilai);
						}
						object.put("nilai", arrayformatNilai);
						jsonObject.put("status", "00");
						jsonObject.put("description", "Ubah data berhasil");
						jsonObject.put("data", object);
						} finally {
							HibernateUtil.closeSessionQuietly(session);
						}
					}

				} else {
					jsonObject.put("status", "91");
					jsonObject.put("description", "Perkuliahan tidak ditemukan");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:787");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({ })
	public static JSONObject daftar_nilai_siswa_oleh_guru(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Boolean hanyaValid = tbmuser != null && (tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null)
						? true
						: null;
				Guru guru = tbmuser.ambilGuru();

				if (!request.isNull("guru")) {
					guru = (Guru) ConstantValues.ambil(Guru.class.getName(), Long.parseLong(request.get("guru") + ""));
				}

				if (guru != null) {

					String ta = request.isNull("ta") ? null : request.getString("ta");

					Integer smt = request.isNull("smt") ? null : request.getInt("smt");

					JSONArray dataKelases = new JSONArray();

					jsonObject.put("data", dataKelases);

					if (ta == null) {
						jsonObject.put("status", "91");
						jsonObject.put("description", "ta atau tahun ajaran kelas harus dipilih");
					} else {

						if (smt == null) {

							jsonObject.put("status", "91");
							jsonObject.put("description", "Semester harus dipilih");

						} else {

							Criterion criterion = Restrictions.eq("guru", guru);
							criterion = Restrictions.or(criterion, Restrictions.eq("guru2", guru));
							criterion = Restrictions.or(criterion, Restrictions.eq("guru3", guru));
							criterion = Restrictions.or(criterion, Restrictions.eq("guru4", guru));
							criterion = Restrictions.or(criterion, Restrictions.eq("guru5", guru));
							criterion = Restrictions.or(criterion, Restrictions.eq("guru6", guru));
							criterion = Restrictions.or(criterion, Restrictions.eq("guru7", guru));
							criterion = Restrictions.or(criterion, Restrictions.eq("guru8", guru));
							criterion = Restrictions.or(criterion, Restrictions.eq("guru9", guru));
							criterion = Restrictions.or(criterion, Restrictions.eq("guru10", guru));
							criterion = Restrictions.or(criterion, Restrictions.eq("guru11", guru));
							criterion = Restrictions.or(criterion, Restrictions.eq("guru12", guru));

							Session session = HibernateUtil.openSession();
							try {

							List<JadwalPelajaran> jadwalPelajarans = ConstantValues.simpleList(
									session.createCriteria(JadwalPelajaran.class)
											.add(Restrictions.eq("tahunAjaran", ta))
											.add(Restrictions.eq("semester", smt)).add(criterion),
									JadwalPelajaran.class);

							for (JadwalPelajaran jadwalPelajaran : jadwalPelajarans) {

								KelasSiswa kelasSiswa = jadwalPelajaran.getKelas();

								JSONObject dataKelas = new JSONObject();

								dataKelases.put(dataKelas);
								dataKelas.put("ta", kelasSiswa.getTahunAjaran());
								dataKelas.put("namakelas", kelasSiswa.getNama());
								dataKelas.put("walikelas", kelasSiswa.getGuruPembina() == null ? ""
										: kelasSiswa.getGuruPembina().getNama());
								dataKelas.put("ruangkelas",
										kelasSiswa.getRuang() == null ? "" : kelasSiswa.getRuang().getNama());
								dataKelas.put("tingkat", kelasSiswa.getTingkat());

								Common.insertProperty(JadwalPelajaran.class, jadwalPelajaran, dataKelas, "jadwal", 1);

								JSONArray data = new JSONArray();

								dataKelas.put("data", data);

								List<KelasSiswaPunyaSiswa> siswas = ConstantValues
										.simpleList(
												session.createCriteria(KelasSiswaPunyaSiswa.class)
														.add(Restrictions.eq("kelasSiswa", kelasSiswa)),
												KelasSiswaPunyaSiswa.class);

								List<JenisPenilaian> jenisPenilaians = ConstantValues.simpleList(
										session.createCriteria(KurikulumPunyaMatapelajaran.class)
												.add(Restrictions.eq("matapelajaran",
														jadwalPelajaran.getMatapelajaran()))

												.createAlias("matapelajaran", "matapelajaran")

												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)))

												.add(Restrictions.eq("kurikulumSekolah",
														kelasSiswa.getKurikulumSekolah()))
												.setProjection(
														Projections.groupProperty("matapelajaran.jenisPenilaian.id"))
												.addOrder(Order.asc("matapelajaran.jenisPenilaian")),
										JenisPenilaian.class, false);

								System.out.println("jenisPenilaians -> " + jenisPenilaians);

								for (JenisPenilaian jenisPenilaian : jenisPenilaians) {
									JSONObject jsonObjectJenisPenilaian = new JSONObject();
									jsonObjectJenisPenilaian.put("nama", jenisPenilaian.getNama());
									jsonObjectJenisPenilaian.put("jenis", jenisPenilaian.getJenis());

									List<KurikulumPunyaMatapelajaran> jenisItemPenilaianSiswa = ConstantValues
											.simpleList(
													session.createCriteria(KurikulumPunyaMatapelajaran.class)
															.add(Restrictions.eq("matapelajaran",
																	jadwalPelajaran.getMatapelajaran()))
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(Restrictions.eq("kurikulumSekolah",
																	kelasSiswa.getKurikulumSekolah()))
															.createAlias("matapelajaran", "matapelajaran")

															.createAlias("matapelajaran.kelompokMatapelajaran",
																	"kelompokMatapelajaran")

															.addOrder(Order.asc("kelompokMatapelajaran.nomorUrut"))
															.addOrder(Order.asc("matapelajaran.urutan"))
															.add(Restrictions.eq("matapelajaran.jenisPenilaian",
																	jenisPenilaian)),
													KurikulumPunyaMatapelajaran.class);

									JSONArray jsonArrayKurikulumPunyaMatapelajaran = new JSONArray();
									for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : jenisItemPenilaianSiswa) {

										JSONObject jsonObjectKurikulumMk = new JSONObject();
										jsonObjectKurikulumMk.put("kode",
												kurikulumPunyaMatapelajaran.getMatapelajaran().getKode());
										jsonObjectKurikulumMk.put("nama",
												kurikulumPunyaMatapelajaran.getMatapelajaran().getNama());
										jsonObjectKurikulumMk.put("guru", guru == null ? "" : guru.getNama());
										jsonArrayKurikulumPunyaMatapelajaran.put(jsonObjectKurikulumMk);

										jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
												.getJenisPenilaian();
										if (kurikulumPunyaMatapelajaran != null
												&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
												&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
														.getJenisPenilaian() != null) {
											jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
													.getJenisPenilaian();
										}

										List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(
												session.createCriteria(DetailJenisPenilaian.class)
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("jenisPenilaian", jenisPenilaian))
														.setProjection(Projections.groupProperty("grupPenilaian.id")),
												GrupPenilaian.class, false);

										JSONArray jsonArrayGrupPenilaians = new JSONArray();
										for (GrupPenilaian grupPenilaian : grupPenilaians) {

											if (grupPenilaian != null && kelasSiswa.getTingkat() > 0
													&& grupPenilaian.getKhususTingkat() != null && !grupPenilaian
															.getKhususTingkat().equals(kelasSiswa.getTingkat())) {
												continue;
											}

											JSONObject jsonObjectGrupPenilaian = new JSONObject();
											jsonArrayGrupPenilaians.put(jsonObjectGrupPenilaian);

											jsonObjectGrupPenilaian.put("nama", grupPenilaian.getNama());
											jsonObjectGrupPenilaian.put("keterangan", grupPenilaian.getKeterangan());

											List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues
													.simpleList(
															session.createCriteria(DetailGrupPenilaian.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"),
																			Restrictions.eq("aktif", true)))
																	.add(Restrictions.isNotNull(
																			"grupKategoriItemPenilaianSiswa"))
																	.setProjection(Projections.groupProperty(
																			"grupKategoriItemPenilaianSiswa.id"))
																	.add(Restrictions.eq("grupPenilaian",
																			grupPenilaian)),
															GrupKategoriItemPenilaianSiswa.class, false);

											if (grupKategoriItemPenilaianSiswas.isEmpty()) {
												continue;
											}

											Collections.sort(grupKategoriItemPenilaianSiswas);

											grupKategoriItemPenilaianSiswas.add(null);

											JSONArray jsonArrayGrupKategoriItemPenilaianSiswas = new JSONArray();
											for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {

												if (grupKategoriItemPenilaianSiswa != null
														&& kelasSiswa.getTingkat() > 0
														&& grupKategoriItemPenilaianSiswa.getKhususTingkat() != null
														&& !grupKategoriItemPenilaianSiswa.getKhususTingkat()
																.equals(kelasSiswa.getTingkat())) {
													continue;
												}

												JSONObject jsonObjectGrupKategoriItemPenilaianSiswa = new JSONObject();
												jsonArrayGrupKategoriItemPenilaianSiswas
														.put(jsonObjectGrupKategoriItemPenilaianSiswa);

												JSONArray jsonArrayNilai = new JSONArray();

												jsonObjectGrupKategoriItemPenilaianSiswa.put("nilai_rinci",
														jsonArrayNilai);

												if (grupKategoriItemPenilaianSiswa == null) {
													jsonObjectGrupKategoriItemPenilaianSiswa.put("nama", "Total");

													for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : siswas) {
														Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();
														Double total = 0.0;

														Double min = 0.0;

														Double max = 0.0;

														try {
															Date sekarang = WaktuUtil.getDate();
															String formula = grupPenilaian.getFormula();

															String target = GrupPenilaianUtil.ambilTarget(formula,
																	sekarang);

															total = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																	kurikulumPunyaMatapelajaran.getMatapelajaran(),
																	grupPenilaian, smt,
																	grupKategoriItemPenilaianSiswas);

															target = GrupPenilaianUtil.ambilTargetMin(formula,
																	sekarang);

															min = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																	kurikulumPunyaMatapelajaran.getMatapelajaran(),
																	grupPenilaian, smt,
																	grupKategoriItemPenilaianSiswas);

															target = GrupPenilaianUtil.ambilTargetMax(formula,
																	sekarang);

															max = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																	kurikulumPunyaMatapelajaran.getMatapelajaran(),
																	grupPenilaian, smt,
																	grupKategoriItemPenilaianSiswas);

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:1051");
														}

														JSONObject js = new JSONObject();
														try {
															js = new JSONObject(
																	smt == 1 ? kelasSiswaPunyaSiswa.getKeterangan1()
																			: kelasSiswaPunyaSiswa.getKeterangan2());
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:1059");
														}

														String keyKet = kurikulumPunyaMatapelajaran.getMatapelajaran()
																.getId() + "_" + grupPenilaian.getId();

														String ket = js.isNull(keyKet) ? "" : js.getString(keyKet);

														NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
																.getNilaiHurufSekolah(total, siswa.getTahunMasuk(),
																		siswa.getSekolah(), siswa.getYayasan(),
																		kelasSiswaPunyaSiswa.getKelasSiswa()
																				.getTahunAjaran(),
																		smt % 2 == 0 ? Perkuliahan.GENAP
																				: Perkuliahan.GANJIL,
																		grupPenilaian.getJenisNilaiHuruf());

														JSONObject jsonObjectNilai = new JSONObject();
														jsonObjectNilai.put("id_siswa", siswa.getId());
														jsonObjectNilai.put("nis_siswa", siswa.getNomorInduk());
														jsonObjectNilai.put("nama_siswa", siswa.getNama());
														jsonObjectNilai.put("foto_siswa",
																CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa)));

														jsonObjectNilai.put("nama", "Nilai Total");
														jsonObjectNilai.put("total", total);
														jsonObjectNilai.put("min", min);
														jsonObjectNilai.put("max", max);
														jsonObjectNilai.put("ket", ket);
														jsonObjectNilai.put("huruf", nilaiHurufSekolah == null ? ""
																: nilaiHurufSekolah.getNilaiHuruf());

														jsonArrayNilai.put(jsonObjectNilai);
													}

												} else {
													jsonObjectGrupKategoriItemPenilaianSiswa.put("nama",
															grupKategoriItemPenilaianSiswa.getNama());

													List<KategoriItemPenilaianSiswa> kategoriItemPenilaianSiswasId = ConstantValues
															.simpleList(session
																	.createCriteria(
																			DetailGrupKategoriItemPenilaianSiswa.class)

																	.add(Restrictions.eq(
																			"grupKategoriItemPenilaianSiswa",
																			grupKategoriItemPenilaianSiswa))
																	.add(Restrictions.or(Restrictions.isNull("aktif"),
																			Restrictions.eq("aktif", true)))

																	.setProjection(Projections.groupProperty(
																			"kategoriItemPenilaianSiswa.id")),
																	KategoriItemPenilaianSiswa.class, false);

													List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas = ConstantValues
															.simpleList(session
																	.createCriteria(JenisItemPenilaianSiswa.class)
																	.createAlias("kategoriItemPenilaianSiswa",
																			"kategoriItemPenilaianSiswa")
																	.addOrder(Order
																			.asc("kategoriItemPenilaianSiswa.kode"))
																	.addOrder(Order.asc("nomorUrut"))
																	.add(Restrictions.in("kategoriItemPenilaianSiswa",
																			kategoriItemPenilaianSiswasId))
																	.add(Restrictions.or(Restrictions.isNull("aktif"),
																			Restrictions.eq("aktif", true))),
																	JenisItemPenilaianSiswa.class);
													for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : siswas) {
														Double total = 0.0;
														Siswa siswa = kelasSiswaPunyaSiswa.getSiswa();
														try {
															Date sekarang = WaktuUtil.getDate();
															String formula = grupKategoriItemPenilaianSiswa
																	.getFormula();
															String target = GrupPenilaianUtil.ambilTarget(formula,
																	sekarang);
															total = kelasSiswaPunyaSiswa.retreiveTotalNilai(
																	jenisItemPenilaianSiswas, target,
																	kurikulumPunyaMatapelajaran.getMatapelajaran(),
																	grupPenilaian, grupKategoriItemPenilaianSiswa, smt,
																	hanyaValid);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:1140");
														}

														NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
																.getNilaiHurufSekolah(total, siswa.getTahunMasuk(),
																		siswa.getSekolah(), siswa.getYayasan(),
																		kelasSiswaPunyaSiswa.getKelasSiswa()
																				.getTahunAjaran(),
																		smt % 2 == 0 ? Perkuliahan.GENAP
																				: Perkuliahan.GANJIL,
																		grupPenilaian.getJenisNilaiHuruf());

														JSONObject jsonObjectNilai = new JSONObject();

														jsonObjectNilai.put("id_siswa", siswa.getId());
														jsonObjectNilai.put("nis_siswa", siswa.getNomorInduk());
														jsonObjectNilai.put("nama_siswa", siswa.getNama());
														jsonObjectNilai.put("foto_siswa",
																CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa)));

														jsonObjectNilai.put("nama",
																grupKategoriItemPenilaianSiswa.getNama());
														jsonObjectNilai.put("total", total);
														jsonObjectNilai.put("min", 0.0);
														jsonObjectNilai.put("max", total);
														jsonObjectNilai.put("ket", "");
														jsonObjectNilai.put("huruf", nilaiHurufSekolah == null ? ""
																: nilaiHurufSekolah.getNilaiHuruf());

														jsonArrayNilai.put(jsonObjectNilai);

													}

												}
											}
											jsonObjectGrupPenilaian.put("grupKategoriItemPenilaianSiswas",
													jsonArrayGrupKategoriItemPenilaianSiswas);
										}

										jsonObjectKurikulumMk.put("grupPenilaians", jsonArrayGrupPenilaians);

									}
									jsonObjectJenisPenilaian.put("kurikulumPunyaMatapelajaran",
											jsonArrayKurikulumPunyaMatapelajaran);
									data.put(jsonObjectJenisPenilaian);
								}
							}

							} finally {
								HibernateUtil.closeSessionQuietly(session);
							}
						}
					}
				} else {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Guru tidak ditemukan");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:1204");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({ })
	public static JSONObject daftar_nilai_siswa(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Boolean hanyaValid = tbmuser != null && (tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null)
						? true
						: null;
				Siswa siswa = tbmuser.getSiswa();
				Guru guru = tbmuser.ambilGuru();

				String ta = request.isNull("ta") ? null : request.getString("ta");

				Integer smt = request.isNull("smt") ? null : request.getInt("smt");

				JSONArray dataKelases = new JSONArray();

				jsonObject.put("data", dataKelases);

				if (ta == null) {
					jsonObject.put("status", "91");
					jsonObject.put("description", "ta atau tahun ajaran kelas harus dipilih");
				} else {

					if (smt == null) {

						jsonObject.put("status", "91");
						jsonObject.put("description", "Kurikulum belum di setting");

					} else {

						Session session = HibernateUtil.openSession();
						try {

						List<KelasSiswa> kelasSiswas = siswa != null
								&& siswa.getId() != null
										? ConstantValues
												.simpleList(
														session.createCriteria(KelasSiswaPunyaSiswa.class)
																.createAlias("kelasSiswa", "kelasSiswa")
																.add(Restrictions.eq("kelasSiswa.tahunAjaran", ta))
																.setProjection(
																		Projections.groupProperty("kelasSiswa.id"))
																.add(Restrictions.eq("siswa", siswa)),
														KelasSiswa.class, false)
										: ConstantValues.simpleList(
												session.createCriteria(KelasSiswa.class)
														.add(guru != null ? Restrictions.eq("guruPembina", guru)
																: Restrictions.sqlRestriction("true"))
														.add(Restrictions.eq("tahunAjaran", ta)),
												KelasSiswa.class, true);

						for (KelasSiswa kelasSiswa : kelasSiswas) {

							JSONObject dataKelas = new JSONObject();

							dataKelases.put(dataKelas);
							dataKelas.put("ta", kelasSiswa.getTahunAjaran());
							dataKelas.put("namakelas", kelasSiswa.getNama());
							dataKelas.put("walikelas",
									kelasSiswa.getGuruPembina() == null ? "" : kelasSiswa.getGuruPembina().getNama());
							dataKelas.put("ruangkelas",
									kelasSiswa.getRuang() == null ? "" : kelasSiswa.getRuang().getNama());
							dataKelas.put("tingkat", kelasSiswa.getTingkat());

							JSONArray data = new JSONArray();

							dataKelas.put("data", data);

							List<KelasSiswaPunyaSiswa> siswas = ConstantValues
									.simpleList(
											session.createCriteria(KelasSiswaPunyaSiswa.class)
													.add(siswa == null ? Restrictions.sqlRestriction("true")
															: Restrictions.eq("siswa", siswa))
													.add(Restrictions.eq("kelasSiswa", kelasSiswa)),
											KelasSiswaPunyaSiswa.class);

							if (smt == null) {
								jsonObject.put("status", "91");
								jsonObject.put("description", "smt atau semester harus dipilih");
							} else {

								Guru gur = tbmuser == null ? null : tbmuser.ambilGuru();
								List<Long> mt;
								if (gur != null) {
									mt = JadwalUtil.ambilJadwal(gur, kelasSiswa);
								} else {
									mt = new ArrayList<Long>();
								}

								List<Long> longs = kelasSiswa.ambilMk();

								List<JenisPenilaian> jenisPenilaians = ConstantValues
										.simpleList(
												session.createCriteria(KurikulumPunyaMatapelajaran.class)
														.createAlias("matapelajaran", "matapelajaran")
														.add(longs == null || longs.isEmpty()
																? Restrictions.sqlRestriction("true")
																: Restrictions.not(
																		Restrictions.in("matapelajaran.id", longs)))
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))

														.add(mt.isEmpty()
																? (gur == null ? Restrictions.sqlRestriction("true")
																		: Restrictions.sqlRestriction("false"))
																: Restrictions.in("matapelajaran.id", mt))

														.add(Restrictions.eq("kurikulumSekolah",
																kelasSiswa.getKurikulumSekolah()))
														.setProjection(Projections
																.groupProperty("matapelajaran.jenisPenilaian.id"))
														.addOrder(Order.asc("matapelajaran.jenisPenilaian")),
												JenisPenilaian.class, false);

								System.out.println("jenisPenilaians -> " + jenisPenilaians);

								for (JenisPenilaian jenisPenilaian : jenisPenilaians) {
									JSONObject jsonObjectJenisPenilaian = new JSONObject();
									jsonObjectJenisPenilaian.put("nama", jenisPenilaian.getNama());
									jsonObjectJenisPenilaian.put("jenis", jenisPenilaian.getJenis());

									List<KurikulumPunyaMatapelajaran> jenisItemPenilaianSiswa = ConstantValues
											.simpleList(
													session.createCriteria(KurikulumPunyaMatapelajaran.class)
															.add(Restrictions.or(Restrictions.isNull("aktif"),
																	Restrictions.eq("aktif", true)))
															.add(Restrictions.eq("kurikulumSekolah",
																	kelasSiswa.getKurikulumSekolah()))
															.createAlias("matapelajaran", "matapelajaran")
															.add(longs == null || longs.isEmpty()
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.not(
																			Restrictions.in("matapelajaran.id", longs)))

															.createAlias("matapelajaran.kelompokMatapelajaran",
																	"kelompokMatapelajaran")

															.addOrder(Order.asc("kelompokMatapelajaran.nomorUrut"))
															.addOrder(Order.asc("matapelajaran.urutan"))
															.add(mt.isEmpty() ? Restrictions.sqlRestriction("true")
																	: Restrictions.in("matapelajaran.id", mt))
															.add(Restrictions.eq("matapelajaran.jenisPenilaian",
																	jenisPenilaian)),
													KurikulumPunyaMatapelajaran.class);

									JSONArray jsonArrayKurikulumPunyaMatapelajaran = new JSONArray();
									for (KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran : jenisItemPenilaianSiswa) {

										guru = (Guru) ConstantValues.simpleObject(
												session.createCriteria(JadwalPelajaran.class)
														.setProjection(Projections.property("guru.id"))
														.add(Restrictions.eq("kelas", kelasSiswa))
														.add(Restrictions.eq("kurikulumPunyaMatapelajaran",
																kurikulumPunyaMatapelajaran))
														.setMaxResults(1),
												Guru.class, false);

										JSONObject jsonObjectKurikulumMk = new JSONObject();
										jsonObjectKurikulumMk.put("kode",
												kurikulumPunyaMatapelajaran.getMatapelajaran().getKode());
										jsonObjectKurikulumMk.put("nama",
												kurikulumPunyaMatapelajaran.getMatapelajaran().getNama());
										jsonObjectKurikulumMk.put("guru", guru == null ? "" : guru.getNama());
										jsonArrayKurikulumPunyaMatapelajaran.put(jsonObjectKurikulumMk);

										jenisPenilaian = kurikulumPunyaMatapelajaran.getMatapelajaran()
												.getJenisPenilaian();
										if (kurikulumPunyaMatapelajaran != null
												&& kurikulumPunyaMatapelajaran.getKurikulumSekolah() != null
												&& kurikulumPunyaMatapelajaran.getKurikulumSekolah()
														.getJenisPenilaian() != null) {
											jenisPenilaian = kurikulumPunyaMatapelajaran.getKurikulumSekolah()
													.getJenisPenilaian();
										}

										List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(
												session.createCriteria(DetailJenisPenilaian.class)
														.add(Restrictions.or(Restrictions.isNull("aktif"),
																Restrictions.eq("aktif", true)))
														.add(Restrictions.eq("jenisPenilaian", jenisPenilaian))
														.setProjection(Projections.groupProperty("grupPenilaian.id")),
												GrupPenilaian.class, false);

										JSONArray jsonArrayGrupPenilaians = new JSONArray();
										for (GrupPenilaian grupPenilaian : grupPenilaians) {

											if (grupPenilaian != null && kelasSiswa.getTingkat() > 0
													&& grupPenilaian.getKhususTingkat() != null && !grupPenilaian
															.getKhususTingkat().equals(kelasSiswa.getTingkat())) {
												continue;
											}

											JSONObject jsonObjectGrupPenilaian = new JSONObject();
											jsonArrayGrupPenilaians.put(jsonObjectGrupPenilaian);

											jsonObjectGrupPenilaian.put("nama", grupPenilaian.getNama());
											jsonObjectGrupPenilaian.put("keterangan", grupPenilaian.getKeterangan());

											List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues
													.simpleList(
															session.createCriteria(DetailGrupPenilaian.class)
																	.add(Restrictions.or(Restrictions.isNull("aktif"),
																			Restrictions.eq("aktif", true)))
																	.add(Restrictions.isNotNull(
																			"grupKategoriItemPenilaianSiswa"))
																	.setProjection(Projections.groupProperty(
																			"grupKategoriItemPenilaianSiswa.id"))
																	.add(Restrictions.eq("grupPenilaian",
																			grupPenilaian)),
															GrupKategoriItemPenilaianSiswa.class, false);

											if (grupKategoriItemPenilaianSiswas.isEmpty()) {
												continue;
											}

											Collections.sort(grupKategoriItemPenilaianSiswas);

											grupKategoriItemPenilaianSiswas.add(null);

											JSONArray jsonArrayGrupKategoriItemPenilaianSiswas = new JSONArray();
											for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {

												if (grupKategoriItemPenilaianSiswa != null
														&& kelasSiswa.getTingkat() > 0
														&& grupKategoriItemPenilaianSiswa.getKhususTingkat() != null
														&& !grupKategoriItemPenilaianSiswa.getKhususTingkat()
																.equals(kelasSiswa.getTingkat())) {
													continue;
												}

												JSONObject jsonObjectGrupKategoriItemPenilaianSiswa = new JSONObject();
												jsonArrayGrupKategoriItemPenilaianSiswas
														.put(jsonObjectGrupKategoriItemPenilaianSiswa);

												JSONArray jsonArrayNilai = new JSONArray();

												jsonObjectGrupKategoriItemPenilaianSiswa.put("nilai_rinci",
														jsonArrayNilai);

												if (grupKategoriItemPenilaianSiswa == null) {
													jsonObjectGrupKategoriItemPenilaianSiswa.put("nama", "Total");

													for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : siswas) {
														siswa = kelasSiswaPunyaSiswa.getSiswa();
														Double total = 0.0;

														Double min = 0.0;

														Double max = 0.0;

														try {
															Date sekarang = WaktuUtil.getDate();
															String formula = grupPenilaian.getFormula();

															String target = GrupPenilaianUtil.ambilTarget(formula,
																	sekarang);

															total = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																	kurikulumPunyaMatapelajaran.getMatapelajaran(),
																	grupPenilaian, smt,
																	grupKategoriItemPenilaianSiswas);

															target = GrupPenilaianUtil.ambilTargetMin(formula,
																	sekarang);

															min = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																	kurikulumPunyaMatapelajaran.getMatapelajaran(),
																	grupPenilaian, smt,
																	grupKategoriItemPenilaianSiswas);

															target = GrupPenilaianUtil.ambilTargetMax(formula,
																	sekarang);

															max = kelasSiswaPunyaSiswa.retreiveTotalNilaiTotal(target,
																	kurikulumPunyaMatapelajaran.getMatapelajaran(),
																	grupPenilaian, smt,
																	grupKategoriItemPenilaianSiswas);

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:1494");
														}

														JSONObject js = new JSONObject();
														try {
															js = new JSONObject(
																	smt == 1 ? kelasSiswaPunyaSiswa.getKeterangan1()
																			: kelasSiswaPunyaSiswa.getKeterangan2());
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:1502");
														}

														String keyKet = kurikulumPunyaMatapelajaran.getMatapelajaran()
																.getId() + "_" + grupPenilaian.getId();

														String ket = js.isNull(keyKet) ? "" : js.getString(keyKet);

														NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
																.getNilaiHurufSekolah(total, siswa.getTahunMasuk(),
																		siswa.getSekolah(), siswa.getYayasan(),
																		kelasSiswaPunyaSiswa.getKelasSiswa()
																				.getTahunAjaran(),
																		smt % 2 == 0 ? Perkuliahan.GENAP
																				: Perkuliahan.GANJIL,
																		grupPenilaian.getJenisNilaiHuruf());

														JSONObject jsonObjectNilai = new JSONObject();
														jsonObjectNilai.put("id_siswa", siswa.getId());
														jsonObjectNilai.put("nis_siswa", siswa.getNomorInduk());
														jsonObjectNilai.put("nama_siswa", siswa.getNama());
														jsonObjectNilai.put("foto_siswa",
																CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa)));

														jsonObjectNilai.put("nama", "Nilai Total");
														jsonObjectNilai.put("total", total);
														jsonObjectNilai.put("min", min);
														jsonObjectNilai.put("max", max);
														jsonObjectNilai.put("ket", ket);
														jsonObjectNilai.put("huruf", nilaiHurufSekolah == null ? ""
																: nilaiHurufSekolah.getNilaiHuruf());

														jsonArrayNilai.put(jsonObjectNilai);
													}

												} else {
													jsonObjectGrupKategoriItemPenilaianSiswa.put("nama",
															grupKategoriItemPenilaianSiswa.getNama());

													List<KategoriItemPenilaianSiswa> kategoriItemPenilaianSiswasId = ConstantValues
															.simpleList(session
																	.createCriteria(
																			DetailGrupKategoriItemPenilaianSiswa.class)

																	.add(Restrictions.eq(
																			"grupKategoriItemPenilaianSiswa",
																			grupKategoriItemPenilaianSiswa))
																	.add(Restrictions.or(Restrictions.isNull("aktif"),
																			Restrictions.eq("aktif", true)))

																	.setProjection(Projections.groupProperty(
																			"kategoriItemPenilaianSiswa.id")),
																	KategoriItemPenilaianSiswa.class, false);

													List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas = ConstantValues
															.simpleList(session
																	.createCriteria(JenisItemPenilaianSiswa.class)
																	.createAlias("kategoriItemPenilaianSiswa",
																			"kategoriItemPenilaianSiswa")
																	.addOrder(Order
																			.asc("kategoriItemPenilaianSiswa.kode"))
																	.addOrder(Order.asc("nomorUrut"))
																	.add(Restrictions.in("kategoriItemPenilaianSiswa",
																			kategoriItemPenilaianSiswasId))
																	.add(Restrictions.or(Restrictions.isNull("aktif"),
																			Restrictions.eq("aktif", true))),
																	JenisItemPenilaianSiswa.class);
													for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : siswas) {
														Double total = 0.0;
														siswa = kelasSiswaPunyaSiswa.getSiswa();
														try {
															Date sekarang = WaktuUtil.getDate();
															String formula = grupKategoriItemPenilaianSiswa
																	.getFormula();
															String target = GrupPenilaianUtil.ambilTarget(formula,
																	sekarang);
															total = kelasSiswaPunyaSiswa.retreiveTotalNilai(
																	jenisItemPenilaianSiswas, target,
																	kurikulumPunyaMatapelajaran.getMatapelajaran(),
																	grupPenilaian, grupKategoriItemPenilaianSiswa, smt,
																	hanyaValid);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:1583");
														}

														NilaiHurufSekolah nilaiHurufSekolah = NilaiHurufSekolah
																.getNilaiHurufSekolah(total, siswa.getTahunMasuk(),
																		siswa.getSekolah(), siswa.getYayasan(),
																		kelasSiswaPunyaSiswa.getKelasSiswa()
																				.getTahunAjaran(),
																		smt % 2 == 0 ? Perkuliahan.GENAP
																				: Perkuliahan.GANJIL,
																		grupPenilaian.getJenisNilaiHuruf());

														JSONObject jsonObjectNilai = new JSONObject();

														jsonObjectNilai.put("id_siswa", siswa.getId());
														jsonObjectNilai.put("nis_siswa", siswa.getNomorInduk());
														jsonObjectNilai.put("nama_siswa", siswa.getNama());
														jsonObjectNilai.put("foto_siswa",
																CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa)));

														jsonObjectNilai.put("nama",
																grupKategoriItemPenilaianSiswa.getNama());
														jsonObjectNilai.put("total", total);
														jsonObjectNilai.put("min", 0.0);
														jsonObjectNilai.put("max", total);
														jsonObjectNilai.put("ket", "");
														jsonObjectNilai.put("huruf", nilaiHurufSekolah == null ? ""
																: nilaiHurufSekolah.getNilaiHuruf());

														jsonArrayNilai.put(jsonObjectNilai);

													}

												}
											}
											jsonObjectGrupPenilaian.put("grupKategoriItemPenilaianSiswas",
													jsonArrayGrupKategoriItemPenilaianSiswas);
										}

										jsonObjectKurikulumMk.put("grupPenilaians", jsonArrayGrupPenilaians);

									}
									jsonObjectJenisPenilaian.put("kurikulumPunyaMatapelajaran",
											jsonArrayKurikulumPunyaMatapelajaran);
									data.put(jsonObjectJenisPenilaian);
								}
							}
						}
						} finally {
							HibernateUtil.closeSessionQuietly(session);
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:1644");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({})
	public static JSONObject update_absen(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Pertemuan pertemuan = (Pertemuan) ConstantValues.ambil(Pertemuan.class.getName(),
						ais.common.CommonJSONUtil.ambilLong(request, "pertemuan"), true);
				Long ref = ais.common.CommonJSONUtil.ambilLong(request, "ref");
				String status = request.getString("status");
				String keterangan = request.getString("keterangan");
				String waktuMulai = request.getString("waktuMulai");
				String waktuSelesai = request.getString("waktuSelesai");
				String jenis = request.getString("jenis");
				String lat = request.isNull("lat") ? null : request.getString("lat");
				String lng = request.isNull("lng") ? null : request.getString("lng");
				Statusabsensi statusabsensi = null;

				for (Object o : ConstantValues.ambilBerdasarClass(Statusabsensi.class).values()) {
					Statusabsensi ss = (Statusabsensi) o;
					if (status.equals(ss.getKode())) {
						statusabsensi = ss;
						break;
					}
				}

				if (statusabsensi == null) {
					statusabsensi = ConstantValues.BELUM_ABSEN;
				}

				if (pertemuan != null) {

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
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:1711");
						}
					}

					if (jarakBenar) {
						Session session = HibernateUtil.openSession();
						try {
						session.refresh(pertemuan);
						pertemuan.populate(ref, statusabsensi, keterangan, null, waktuMulai, waktuSelesai, jenis);

						session.getTransaction().begin();
						Common.refreshUpdate(session, pertemuan);
						session.getTransaction().commit();

						JSONObject object = new JSONObject();

						object.put("nama_status", statusabsensi.getNama());
						object.put("kode_status", statusabsensi.getKode());
						object.put("id_status", statusabsensi.getId());
						object.put("sampai", waktuSelesai);
						object.put("mulai", waktuMulai);
						object.put("keterangan", keterangan);

						// session.disconnect();
						ApiHelperSupport.closeOpenedSession(session);
						jsonObject.put("data", object);
						jsonObject.put("status", "00");
						jsonObject.put("description", "Update absen berhasil");
						} finally {
							HibernateUtil.closeSessionQuietly(session);
						}
					}

				} else {
					jsonObject.put("status", "91");
					jsonObject.put("description", "Pertemuan tidak ditemukan");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:1756");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({})
	public static JSONObject daftar_absen_dosen(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Pertemuan pertemuan = (Pertemuan) ConstantValues.ambil(Pertemuan.class.getName(),
						ais.common.CommonJSONUtil.ambilLong(request, "pertemuan"), true);
				if (pertemuan != null) {

					JSONArray array = new JSONArray();
					for (Dosen dosen : pertemuan.ambilDosen()) {

						JSONObject object = new JSONObject();
						object.put("id", dosen.getId());

						object.put("nidn", dosen.getNidn());
						object.put("nama", dosen.getNama());
						object.put("prodi", dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama());

						String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(dosen));
						object.put("foto", url);

						String ket = pertemuan.retreiveAbsensiKeterangan(dosen.getId());
						ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

						Statusabsensi statusabsensi = (Statusabsensi) ConstantValues
								.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(dosen.getId()));
						if (statusabsensi == null) {
							statusabsensi = ConstantValues.BELUM_ABSEN;
						}

						String mulai = pertemuan.retreiveAbsensiMulai(dosen.getId());
						String sampai = pertemuan.retreiveAbsensiSampai(dosen.getId());

						object.put("nama_status", statusabsensi.getNama());
						object.put("kode_status", statusabsensi.getKode());
						object.put("id_status", statusabsensi.getId());
						object.put("sampai", sampai);
						object.put("mulai", mulai);
						object.put("keterangan", ket);
						array.put(object);

					}
					jsonObject.put("status", "00");
					jsonObject.put("description", "Pengambilan data berhasil");
					jsonObject.put("data", array);

				} else {
					jsonObject.put("status", "91");
					jsonObject.put("description", "Pertemuan tidak ditemukan");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:1825");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({})
	public static JSONObject daftar_absen_mahasiswa(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Pertemuan pertemuan = (Pertemuan) ConstantValues.ambil(Pertemuan.class.getName(),
						ais.common.CommonJSONUtil.ambilLong(request, "pertemuan"), true);
				if (pertemuan != null) {

					JSONArray array = new JSONArray();
					for (Mahasiswa mahasiswa : pertemuan.ambilMahasiswa()) {

						JSONObject object = new JSONObject();
						object.put("id", mahasiswa.getId());

						object.put("nim", mahasiswa.getNim());
						object.put("nama", mahasiswa.getNama());
						object.put("prodi", mahasiswa.getJurusan().getNama());
						object.put("angkatan", mahasiswa.getTahunangkatan());

						String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa));
						object.put("foto", url);

						String ket = pertemuan.retreiveAbsensiKeterangan(mahasiswa.getId());
						ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

						Statusabsensi statusabsensi = (Statusabsensi) ConstantValues
								.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(mahasiswa.getId()));
						if (statusabsensi == null) {
							statusabsensi = ConstantValues.BELUM_ABSEN;
						}

						String mulai = pertemuan.retreiveAbsensiMulai(mahasiswa.getId());
						String sampai = pertemuan.retreiveAbsensiSampai(mahasiswa.getId());

						object.put("nama_status", statusabsensi.getNama());
						object.put("kode_status", statusabsensi.getKode());
						object.put("id_status", statusabsensi.getId());
						object.put("sampai", sampai);
						object.put("mulai", mulai);
						object.put("keterangan", ket);
						array.put(object);

					}
					jsonObject.put("status", "00");
					jsonObject.put("description", "Pengambilan data berhasil");
					jsonObject.put("data", array);

				} else {
					jsonObject.put("status", "91");
					jsonObject.put("description", "Pertemuan tidak ditemukan");
				}

			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:1895");
			}
		}
		return jsonObject;
	}

	 

	/**
	 * Membaca id BERTIPE ANGKA dari payload JSON secara toleran.
	 *
	 * <p><b>KE-FIX</b> ("java.lang.NumberFormatException: For input string: &quot;&quot;").
	 * Klien mengirim {@code "id": ""} (string KOSONG, bukan JSON null) untuk data BARU.
	 * {@code JSONObject.isNull} bernilai false untuk string kosong, sehingga
	 * {@code Long.parseLong("")} dieksekusi dan melempar NumberFormatException tiap kali
	 * data baru disimpan. Exception itu tertelan catch di bawah -- alurnya kebetulan tetap
	 * benar (id tetap null =&gt; buat data baru) tetapi Error Log terus dibanjiri.</p>
	 *
	 * <p>Nilai kosong/bukan angka kini langsung berarti "tanpa id" = data baru, persis
	 * seperti hasil akhir sebelumnya, tanpa exception.</p>
	 */
	private static Long angkaIdAtauNull(Object mentah) {
		if (mentah == null || JSONObject.NULL.equals(mentah)) {
			return null;
		}
		if (mentah instanceof Number) {
			return Long.valueOf(((Number) mentah).longValue());
		}
		String teks = String.valueOf(mentah).trim();
		if (teks.length() == 0 || "null".equalsIgnoreCase(teks) || "undefined".equalsIgnoreCase(teks)) {
			return null;
		}
		try {
			return Long.valueOf(teks);
		} catch (NumberFormatException bukanAngka) {
			// Bisa saja id desimal ("12.0") kiriman klien JavaScript; ambil bagian bulatnya.
			try {
				return Long.valueOf(new java.math.BigDecimal(teks).longValue());
			} catch (NumberFormatException tetapBukanAngka) {
				return null;
			}
		}
	}

	/**
	 * Membaca id BERTIPE TEKS (userId/roleId/nama) dari payload JSON.
	 *
	 * <p>String kosong dinormalkan menjadi null. Sebelumnya id "" diteruskan ke
	 * {@code Restrictions.idEq("")}: pada entitas ber-PK angka itu melempar galat tipe, dan
	 * pada entitas ber-PK teks hasilnya selalu nihil. Keduanya sama artinya dengan "data
	 * baru", jadi hasil akhirnya tidak berubah -- hanya tidak lagi lewat jalur galat.</p>
	 */
	private static String teksIdAtauNull(Object mentah) {
		if (mentah == null || JSONObject.NULL.equals(mentah)) {
			return null;
		}
		String teks = String.valueOf(mentah).trim();
		return teks.length() == 0 ? null : teks;
	}

	@SuppressWarnings("rawtypes")
	public static GeneralValueObject simpanData(Class clazz, Session session, JSONObject request, Tbmuser tbmuser,
			List<String> warnings, HttpServletRequest req) throws Exception {
		Serializable id = null;

		try {
			if (clazz.getName().equalsIgnoreCase(Tbmuser.class.getName())) {
				id = teksIdAtauNull(request.opt("userId"));
			} else if (clazz.getName().equalsIgnoreCase(Tbmrole.class.getName())) {
				id = teksIdAtauNull(request.opt("roleId"));
			} else if (clazz.getName().equalsIgnoreCase(Program.class.getName())) {
				id = teksIdAtauNull(request.opt("nama"));
			} else {
				id = angkaIdAtauNull(request.opt("id"));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:1918");
			// TODO: handle exception
		}
		String tidakBolehSama = request.isNull("tidakBolehSama") ? "" : (request.get("tidakBolehSama") + "").trim();

		JSONObject data = request.getJSONObject("data");
		try {
			if (id == null) {
				if (clazz.getName().equalsIgnoreCase(Tbmuser.class.getName())) {
					id = teksIdAtauNull(data.opt("userId"));
				} else if (clazz.getName().equalsIgnoreCase(Tbmrole.class.getName())) {
					id = teksIdAtauNull(data.opt("roleId"));
				} else if (clazz.getName().equalsIgnoreCase(Program.class.getName())) {
					id = teksIdAtauNull(data.opt("nama"));
				} else {
					id = angkaIdAtauNull(data.opt("id"));
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:1936");
			// TODO: handle exception
		}
		GeneralValueObject generalValueObject = ElearningApiUtil.simpanProperty(clazz, id, session, tbmuser, data,
				tidakBolehSama, warnings, req);

		return generalValueObject;
	}

	@SuppressWarnings({ "rawtypes", "deprecation", "unchecked" })
	public static GeneralValueObject simpanProperty(Class clazz, Serializable id, Session session, Tbmuser tbmuser,
			JSONObject data, String tidakBolehSama, List<String> warnings, HttpServletRequest req) throws Exception {

		System.out.println("simpanProperty -> clazz -> " + clazz.getName() + ", id -> " + id);

		// 1. Validasi Data Unik (Tidak Boleh Sama)
		if (!tidakBolehSama.isEmpty()) {
			for (String c : tidakBolehSama.split(";")) {
				if (!c.trim().isEmpty() && !data.isNull(c)) {
					try {
						String strid = (data.get(c) + "").trim();
						int count = ((Number) session.createCriteria(clazz).setProjection(Projections.rowCount())
								.add(Restrictions.eq(c, strid))
								.add(id == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", id))
								.uniqueResult()).intValue();
						if (count > 0) {
							warnings.add("Data \"" + c + "\" tidak diperbolehkan sama");
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:1964");
					}
				}
			}
		}

		if (!warnings.isEmpty()) {
			return null;
		}

		GeneralValueObject generalValueObject = null;
		try {
			// 2. Load Data Lama dari Database (Jika ID ada)
			if (id != null && !id.equals(-1L)) {
				generalValueObject = (GeneralValueObject) session.createCriteria(clazz).add(Restrictions.idEq(id))
						.uniqueResult();
			}

			// 3. Logic Relasi ke Tabel Utama (Jika object belum ada)
			try {
				if (generalValueObject == null && !data.isNull("relasi_ke_tabel_utama")) {

					JSONObject relasi_ke_tabel_utama = data.getJSONObject("relasi_ke_tabel_utama");
					String relasi = "";
					if (!relasi_ke_tabel_utama.isNull("id")
							&& Common.isNumber((relasi_ke_tabel_utama.get("id") + "").trim())) {

						relasi = relasi_ke_tabel_utama.get("relasi") + "";

						generalValueObject = (GeneralValueObject) session.createCriteria(clazz)
								.add(Restrictions.eq(relasi,
										Long.parseLong((relasi_ke_tabel_utama.get("id") + "").trim())))
								.setMaxResults(1).addOrder(Order.asc("id")).uniqueResult();
					}

					if (generalValueObject == null) {
						warnings.add("Data \"" + relasi + "\" tidak ditemukan untuk id \""
								+ relasi_ke_tabel_utama.get("id") + "\"");
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2005");
			}

			if (!warnings.isEmpty()) {
				return null;
			}

			ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);

			if (classMetadata == null) {
				classMetadata = StreamingHibernateUtil.getInstance().getClassMetadata(clazz);
			}

			String[] strings = classMetadata.getPropertyNames();

			// 4. Cek berdasarkan Kode jika object masih null
			if (generalValueObject == null) {
				boolean adaKode = false;
				for (String k : strings) {
					if (k.equals("kode")) {
						adaKode = true;
						break;
					}
				}

				if (adaKode && !data.isNull("kode") && !data.get("kode").toString().isEmpty()) {
					try {
						String strid = (data.get("kode") + "").trim();
						generalValueObject = ConstantValues
								.simpleObject(session.createCriteria(clazz).add(Restrictions.eq("kode", strid)), clazz);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2036");
					}
				}
			}

			boolean baru = false;
			if (generalValueObject == null) {
				baru = true;
				generalValueObject = (GeneralValueObject) clazz.newInstance();
			}

			// ========================================================================
			// 5. LOOPING PROPERTI & PENGECEKAN PERUBAHAN DATA (MODIFIKASI DISINI)
			// ========================================================================

			Iterator<String> properties = data.keys();
			while (properties.hasNext()) {

				try {
					String property = properties.next();
					if (property == null || "id".equalsIgnoreCase(property.trim())) {
						continue;
					}

					// Properti dari payload client kadang berupa field non-mapped (mis. "idUploadFoto"
					// dari alur upload foto terpisah/FileFotoLain, bukan kolom entity ini). Lewati
					// dengan aman tanpa menembak QueryException dari Hibernate metadata.

					// Konversi snake_case ke camelCase agar sesuai nama properti Hibernate
					// (mis. "berat_badan" -> "beratBadan"). Gunakan jsonKey untuk baca data JSON.
					String jsonKey = property;
					if (property.contains("_")) {
						StringBuilder sbProp = new StringBuilder();
						boolean nextUpper = false;
						for (int pi = 0; pi < property.length(); pi++) {
							char pc = property.charAt(pi);
							if (pc == '_') {
								nextUpper = true;
							} else {
								sbProp.append(nextUpper ? Character.toUpperCase(pc) : pc);
								nextUpper = false;
							}
						}
						property = sbProp.toString();
					}

					boolean adaProperty = false;
					for (String namaProperti : strings) {
						if (namaProperti.equals(property)) {
							adaProperty = true;
							break;
						}
					}
					if (!adaProperty) {
						System.out.println("lewati property tidak termapping -> " + property);
						continue;
					}

					Class claazz = classMetadata.getPropertyType(property).getReturnedClass();
					String strid = data.isNull(jsonKey) ? null : (data.get(jsonKey) + "").trim();

					System.out.println("awal property -> " + property);

					// Ambil Nilai Lama (Old Value) dari Object Hibernate
					Object oldValue = classMetadata.getPropertyValue(generalValueObject, property, EntityMode.POJO);

					Object value = null; // Ini akan menjadi New Value

					// --- Parsing Logic Mulai ---
					if (strid == null || strid.equalsIgnoreCase("null")) {
						value = null; // Explicitly null
					} else if (strid.startsWith("{")) {
						JSONObject jsonObject = new JSONObject(strid);
						Class clazzD = Class.forName(jsonObject.getString("class"));
						value = simpanData(clazzD, session, jsonObject, tbmuser, warnings, req);
					} else {
						if (strid != null && strid.equalsIgnoreCase("null")) {
							value = null; // Explicitly null
						} else {
							// Parsing Type Data Primitif/Wrapper
							if (claazz.getName().equals(Integer.class.getName())) {
								value = parseIntegerAman(property, strid, oldValue);
							} else if (claazz.getName().equals(Long.class.getName())) {
								value = parseLongAman(property, strid, oldValue);
							} else if (claazz.getName().equals(Boolean.class.getName())) {
								value = strid == null || strid.trim().isEmpty() ? null
										: Boolean.parseBoolean(strid.trim());
							} else if (claazz.getName().equals(Double.class.getName())) {
								value = parseDoubleAman(property, strid, oldValue);
							} else if (claazz.getName().equals(String.class.getName())) {
								value = strid;
							} else if (claazz.getName().equals(Date.class.getName()) && strid != null
									&& strid.contains("T") && strid.length() == 16) {
								value = Common.dateFormatInput.get().parse(strid);
							} else if (claazz.getName().equals(Date.class.getName()) && strid != null
									&& strid.split("-").length == 2 && strid.length() == 8) {
								value = Common.formatTahunTanggal.get().parse(strid);
							} else if (claazz.getName().equals(Date.class.getName()) && strid != null
									&& strid.length() >= 19) {
								value = strid == null || strid.trim().isEmpty() ? null
										: Common.databaseDateFormat1.get().parse(strid);
							} else if (claazz.getName().equals(Date.class.getName())) {
								value = strid == null || strid.trim().isEmpty() ? null
										: Common.databaseDateFormat.get().parse(strid);
							} else {
								// Parsing Object Relasi (Foreign Key)
								try {
									if (strid == null || strid.trim().isEmpty()) {
										value = null;
									} else if (Common.isNumber(strid)) {
										value = ConstantValues.simpleObject(session.createCriteria(claazz)
												.add(Restrictions.idEq(Long.parseLong(strid.trim()))), claazz);
									} else {
										value = ConstantValues.simpleObject(
												session.createCriteria(claazz).add(Restrictions.idEq(strid.trim())),
												claazz);
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:2116");
								}
							}
						}
					}
					// --- Parsing Logic Selesai (Kita sudah punya 'value' baru) ---

					// --- LOGIC UTAMA: Compare Old Value vs New Value ---
					boolean dataBerubah = false;

					if (oldValue == null && value == null) {
						dataBerubah = false; // Sama-sama null
					} else if (oldValue == null && value != null) {
						dataBerubah = true; // Dulu null, sekarang ada isi
					} else if (oldValue != null && value == null) {
						dataBerubah = true; // Dulu ada isi, sekarang dihapus (null)
					} else {
						// Keduanya tidak null, bandingkan isinya
						if (oldValue instanceof Tbmuser && value instanceof Tbmuser) {
							// Jika tipe data adalah Object Entity (Relasi), bandingkan ID-nya saja
							Serializable idOld = ((Tbmuser) oldValue).getUserId();
							Serializable idNew = ((Tbmuser) value).getUserId();

							// Cek null safety pada ID
							if (idOld == null && idNew == null) {
								dataBerubah = false;
							} else if (idOld == null || idNew == null) {
								dataBerubah = true;
							} else if (!idOld.equals(idNew)) {
								dataBerubah = true;
							}
						} else if (oldValue instanceof Tbmrole && value instanceof Tbmrole) {
							// Jika tipe data adalah Object Entity (Relasi), bandingkan ID-nya saja
							Serializable idOld = ((Tbmrole) oldValue).getRoleId();
							Serializable idNew = ((Tbmrole) value).getRoleId();

							// Cek null safety pada ID
							if (idOld == null && idNew == null) {
								dataBerubah = false;
							} else if (idOld == null || idNew == null) {
								dataBerubah = true;
							} else if (!idOld.equals(idNew)) {
								dataBerubah = true;
							}
						} else if (oldValue instanceof Program && value instanceof Program) {
							// Jika tipe data adalah Object Entity (Relasi), bandingkan ID-nya saja
							Serializable idOld = ((Program) oldValue).getNama();
							Serializable idNew = ((Program) value).getNama();

							// Cek null safety pada ID
							if (idOld == null && idNew == null) {
								dataBerubah = false;
							} else if (idOld == null || idNew == null) {
								dataBerubah = true;
							} else if (!idOld.equals(idNew)) {
								dataBerubah = true;
							}
						} else if (oldValue instanceof GeneralValueObject && value instanceof GeneralValueObject) {
							// Jika tipe data adalah Object Entity (Relasi), bandingkan ID-nya saja
							Serializable idOld = ((GeneralValueObject) oldValue).getId();
							Serializable idNew = ((GeneralValueObject) value).getId();

							// Cek null safety pada ID
							if (idOld == null && idNew == null) {
								dataBerubah = false;
							} else if (idOld == null || idNew == null) {
								dataBerubah = true;
							} else if (!idOld.equals(idNew)) {
								dataBerubah = true;
							}
						} else {
							// Jika tipe data Primitif/String/Date/dll
							if (!oldValue.equals(value)) {
								dataBerubah = true;
							}
						}
					}

					System.out.println("property -> " + property + ", data -> " + strid + ", val -> "
							+ labelObjectAman(value) + ", oldValue -> " + labelObjectAman(oldValue)
							+ ", dataBerubah -> " + dataBerubah);

					// --- EKSEKUSI UPDATE HANYA JIKA BERUBAH ---
					if (dataBerubah) {

						// Logic Khusus Pertemuan (sesuai code asli)
						if (clazz.getName().equalsIgnoreCase(Pertemuan.class.getName()) && value != null
								&& property.equalsIgnoreCase("tanggal")) {
							((Pertemuan) generalValueObject).setTanggalEdit((Date) value);
						}

						// UPDATE ke Object Memory
						classMetadata.setPropertyValue(generalValueObject, property, value, EntityMode.POJO);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:2209");
				}
			}
			// ========================================================================
			// AKHIR LOOPING
			// ========================================================================

			// 6. Logic ID Khusus (Tbmuser, Role, Program)
			if (id != null && (clazz.getName().equalsIgnoreCase(Tbmuser.class.getName())
					|| clazz.getName().equalsIgnoreCase(Tbmrole.class.getName())
					|| clazz.getName().equalsIgnoreCase(Program.class.getName()))) {
				classMetadata.setIdentifier(generalValueObject, id, (org.hibernate.engine.SessionImplementor) session);
			}

			// 7. Logic Auto Number Surat Keluar
			if (clazz.getName().equalsIgnoreCase(SuratKeluar.class.getName())) {
				SuratKeluar suratKeluar = (SuratKeluar) generalValueObject;
				if (suratKeluar.getIndex() == null) {
					String noAgenda = SuratKeluarAction.generateCode(true, suratKeluar.getKlasifikasiSuratKeluar(),
							suratKeluar.getTanggal());
					generalValueObject.setKode(noAgenda);

					String noAgendaData = SuratKeluarAction.generateCodeAgenda(true,
							suratKeluar.getKlasifikasiSuratKeluar(), suratKeluar.getTanggal());

					suratKeluar.setAgenda(noAgendaData);

					Long currentIndex = SuratKeluarAction.getindex(suratKeluar.getKlasifikasiSuratKeluar());
					suratKeluar.setIndex(++currentIndex);
				}
			}

			// 8. Logic Auto Number Surat Masuk
			if (clazz.getName().equalsIgnoreCase(SuratMasuk.class.getName())) {
				SuratMasuk suratMasuk = (SuratMasuk) generalValueObject;
				if (suratMasuk.getIndex() == null) {
					String noAgenda = SuratMasukAction.generateCode(true, suratMasuk.getKlasifikasiSuratMasuk(),
							suratMasuk.getTanggal());
					generalValueObject.setKode(noAgenda);

					Long currentIndex = SuratMasukAction.getindex(suratMasuk.getKlasifikasiSuratMasuk());
					suratMasuk.setIndex(++currentIndex);
				}
			}

			if (baru && (data.isNull("kode") || data.get("kode").toString().trim().isEmpty())
					&& clazz.getName().equalsIgnoreCase(AnggotaKoperasi.class.getName())) {
				AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) generalValueObject;
				String noAgenda = anggotaKoperasi.generateKodeMember(session, WaktuUtil.getDate());
				anggotaKoperasi.setKode(noAgenda);
			}

			// 9. Simpan History
			try {
				GeneralValueObject.ubahDataHistory(generalValueObject, CommonPrivilages.UPDATE);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:2264");
				// TODO: handle exception
			}

			Transaction transaction = null;

			try {
				// 1. WAJIB mulai transaksi
				transaction = session.beginTransaction();

				Tbmuser tbmuserPedagang = null;
				if (clazz.getName().equalsIgnoreCase(Pedagang.class.getName())) {
					Pedagang pedagang = (Pedagang) generalValueObject;
					if (pedagang.getUserid() != null) {
						tbmuserPedagang = (Tbmuser) session.createCriteria(Tbmuser.class)
								.add(Restrictions.idEq(pedagang.getUserid())).uniqueResult();
						if (tbmuserPedagang == null) {
							tbmuserPedagang = new Tbmuser();
							tbmuserPedagang.setUserId(pedagang.getUserid());
							tbmuserPedagang.setUserRole(ConstantValues.roleKantin);
							tbmuserPedagang.setUserPassword(pedagang.getPass());
							tbmuserPedagang.setUserNama(pedagang.getNama());
							tbmuserPedagang.setAktif(pedagang.getAktif());
							session.save(tbmuserPedagang);
						}
					}
				}

				// Jalur simpan generik ini tidak punya logika bisnis: ia menyimpan persis
				// field yang dikirim klien. Klien yang tidak mengirim bayar_tunai/bayar_non_tunai
				// menghasilkan nota "lunas" bernilai bayar NOL, yang kemudian terbaca sebagai
				// PIUTANG oleh laporan Saldo Piutang. Sudah terjadi: 80 nota Rp 1,9 juta dari
				// sinkronisasi luring. Diperbaiki di sini karena di sinilah satu-satunya titik
				// yang dilalui SEMUA klien jalur ini, termasuk klien yang sumbernya tidak ada
				// di repositori ini.
				if (clazz.getName().equalsIgnoreCase(
						ais.database.model.koperasi.PembelianAnggotaKoperasi.class.getName())) {
					try {
						if (ais.action.servlet.api.KantinHelper.normalkanNilaiBayar(
								(ais.database.model.koperasi.PembelianAnggotaKoperasi) generalValueObject)) {
							ais.common.ErrorAuditUtil.record(
									new IllegalStateException("nilai bayar kosong dilengkapi dari tanda metode"),
									"simpanDataRinci: PembelianAnggotaKoperasi tanpa nilai bayar");
						}
					} catch (Exception eNormalisasi) {
						// Gagal melengkapi TIDAK boleh menggagalkan simpan: nota yang tersimpan
						// dengan nilai bayar kosong masih bisa dikoreksi belakangan, sedangkan
						// transaksi yang gagal tersimpan hilang bersama uangnya.
						ais.common.ErrorAuditUtil.record(eNormalisasi,
								"simpanDataRinci: normalkanNilaiBayar gagal");
					}
				}

				Tbmuser tbmuserAnggota = null;
				if (clazz.getName().equalsIgnoreCase(AnggotaKoperasi.class.getName())) {
					AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) generalValueObject;
					if (anggotaKoperasi.getUserid() != null) {
						tbmuserAnggota = (Tbmuser) session.createCriteria(Tbmuser.class)
								.add(Restrictions.idEq(anggotaKoperasi.getUserid())).uniqueResult();
						if (tbmuserAnggota == null) {
							tbmuserAnggota = new Tbmuser();
							tbmuserAnggota.setUserId(anggotaKoperasi.getUserid());
							tbmuserAnggota.setUserRole(ConstantValues.roleAnggotaKoperasi);
							tbmuserAnggota.setUserPassword(anggotaKoperasi.getPass());
							tbmuserAnggota.setUserNama(anggotaKoperasi.getNama());
							tbmuserAnggota.setAktif(anggotaKoperasi.getAktif());
							session.save(tbmuserAnggota);
						}
					}
				}

				if (clazz.getName().equalsIgnoreCase(BiodataCalonMahasiswa.class.getName())) {
					BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) generalValueObject;
					if (biodataCalonMahasiswa.getNoRegistrasi() == null
							|| biodataCalonMahasiswa.getNoRegistrasi().trim().isEmpty()) {
						biodataCalonMahasiswa.setNoRegistrasi(CommonPMB.generateNoRegistrasi(biodataCalonMahasiswa));
					}

				}

				if (clazz.getName().equalsIgnoreCase(CalonSiswa.class.getName())) {
					CalonSiswa calonSiswa = (CalonSiswa) generalValueObject;
					if (calonSiswa.getId() == null || calonSiswa.getNoRegistrasi() == null
							|| calonSiswa.getNoRegistrasi().trim().isEmpty()) {
						calonSiswa.setNoRegistrasi(CommonPSB.generateNoRegistrasi(calonSiswa));
					}

				}

				if (generalValueObject.getId() != null) {
					// Karena kita sudah memfilter setPropertyValue di atas,
					// jika tidak ada perubahan data, method ini tidak akan menembak query update.
					session.update(generalValueObject);
				} else {
					session.save(generalValueObject);
				}
				session.flush();

				if (clazz.getName().equalsIgnoreCase(Pedagang.class.getName()) && tbmuserPedagang != null) {
					Pedagang pedagang = (Pedagang) generalValueObject;
					if (pedagang.getUserid() != null) {
						if (pedagang.getId() != null) {
							pedagang.setTbmuser(tbmuserPedagang);
							tbmuserPedagang.setPedagang(pedagang);
							session.update(tbmuserPedagang);
							session.update(pedagang);
						}
					}
				}

				if (clazz.getName().equalsIgnoreCase(AnggotaKoperasi.class.getName()) && tbmuserAnggota != null) {
					AnggotaKoperasi anggotaKoperasi = (AnggotaKoperasi) generalValueObject;
					if (anggotaKoperasi.getUserid() != null) {
						if (anggotaKoperasi.getId() != null) {
							anggotaKoperasi.setTbmuser(tbmuserAnggota);
							tbmuserAnggota.setAnggotaKoperasi(anggotaKoperasi);
							session.update(tbmuserAnggota);
							session.update(anggotaKoperasi);

						}
					}
				}

				// 3. Commit transaksi jika semuanya lancar
				transaction.commit();

			} catch (org.hibernate.exception.ConstraintViolationException cve) {
				if (clazz.getName().equalsIgnoreCase(BiodataCalonMahasiswa.class.getName())) {
					warnings.add(pesanKonflikPmbDariCve(cve));
				} else {
					String gabung = (cve.getMessage() != null ? cve.getMessage() : "")
							+ (cve.getCause() != null && cve.getCause().getMessage() != null
									? " | " + cve.getCause().getMessage() : "");
					warnings.add("[DATA-DUPLIKAT] Data tidak dapat disimpan karena konflik data unik. Detail: " + gabung);
				}
				if (transaction != null && transaction.isActive()) {
					try { transaction.rollback(); } catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:2375");}
				}
				cve.printStackTrace(); ais.common.ErrorAuditUtil.record(cve, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2377");
			} catch (org.hibernate.exception.DataException de) {
				if (clazz.getName().equalsIgnoreCase(BiodataCalonMahasiswa.class.getName())) {
					warnings.add("[PMB-PANJANG] Salah satu isian formulir melebihi batas panjang yang diizinkan oleh sistem. "
							+ "Mohon periksa kembali kolom seperti alamat, nama, keterangan, atau catatan tambahan dan persingkat isinya, "
							+ "lalu coba simpan kembali.");
				} else {
					warnings.add("[DATA-PANJANG] Isian melebihi batas panjang kolom. Detail: "
							+ (de.getMessage() != null ? de.getMessage() : "unknown"));
				}
				if (transaction != null && transaction.isActive()) {
					try { transaction.rollback(); } catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:2388");}
				}
				de.printStackTrace(); ais.common.ErrorAuditUtil.record(de, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2390");
			} catch (org.hibernate.StaleObjectStateException sose) {
				if (clazz.getName().equalsIgnoreCase(BiodataCalonMahasiswa.class.getName())) {
					warnings.add("[PMB-USANG] Data formulir Anda sudah diperbarui oleh sistem secara bersamaan. "
							+ "Mohon muat ulang halaman pendaftaran, kemudian isi kembali formulir dari awal dan coba simpan lagi.");
				} else {
					warnings.add("[DATA-USANG] Data berubah bersamaan. Muat ulang dan coba lagi.");
				}
				if (transaction != null && transaction.isActive()) {
					try { transaction.rollback(); } catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:2399");}
				}
				sose.printStackTrace(); ais.common.ErrorAuditUtil.record(sose, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2401");
			} catch (Exception e) {
				String detailPenyebab = e.getMessage() != null ? e.getMessage()
						: (e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage()
								: e.getClass().getSimpleName());
				if (clazz.getName().equalsIgnoreCase(BiodataCalonMahasiswa.class.getName())) {
					warnings.add("[PMB-SRV-001] Terjadi kesalahan sistem saat menyimpan data pendaftaran. "
							+ "Mohon catat waktu kejadian ini dan hubungi petugas administrasi penerimaan mahasiswa baru. "
							+ "Sampaikan kode kesalahan: PMB-SRV-001. Detail teknis: " + detailPenyebab);
				} else {
					warnings.add("[SRV-001] Kesalahan sistem: " + detailPenyebab);
				}
				if (transaction != null && transaction.isActive()) {
					try {
						transaction.rollback();
					} catch (Exception rollbackEx) {
						System.err.println("Gagal melakukan rollback: " + rollbackEx.getMessage());
					}
				}
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2420");
			} finally {

			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2426");
			String detailLuar = e.getMessage() != null ? e.getMessage()
					: (e.getCause() != null && e.getCause().getMessage() != null ? e.getCause().getMessage()
							: e.getClass().getSimpleName());
			warnings.add("[PMB-SRV-002] Kesalahan tidak terduga pada sistem. "
					+ "Mohon catat waktu kejadian dan hubungi petugas. Kode: PMB-SRV-002. Detail: " + detailLuar);
		}

		// Post-save: hanya jalankan jika save berhasil (warnings kosong) dan object tidak null
		if (warnings.isEmpty() && generalValueObject != null
				&& clazz.getName().equalsIgnoreCase(BiodataCalonMahasiswa.class.getName())) {
			BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) generalValueObject;
			biodataCalonMahasiswa.chekPembayaranDaftarUlang(session);
			biodataCalonMahasiswa.chekPembayaranRegistrasi(session);

			// Di dalam Backend (ElearningApiUtil.prosesSimpan) sesudah biodata
			// di-save/update

			String verifikasiRaw = data.has("verifikasiBerkasRaw") ? data.getString("verifikasiBerkasRaw") : null;

			if (verifikasiRaw != null && !verifikasiRaw.isEmpty()) {
				// Panggil helper yang baru kita buat
				VerifikasiPMBHtmlHelper.simpanVerifikasiHtml(biodataCalonMahasiswa, verifikasiRaw, session);
			}
		}

		if (clazz.getName().equalsIgnoreCase(CalonSiswa.class.getName())) {
			CalonSiswa calonSiswa = (CalonSiswa) generalValueObject;
			// Generate No. Ujian jika dikonfigurasi
			if (Common.bolehKonfigurasi("setelah_daftar_psb_langsung_generate_nomor_ujian")) {
				String noUjian = CommonPSB.generateNoUjian(calonSiswa, warnings);
				if (!warnings.isEmpty()) {
					return null;
				}
				RuangGelombangPendaftaranPsbPSB ruangGelombangPendaftaranPsbPSB = (RuangGelombangPendaftaranPsbPSB) session
						.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
						.add(Restrictions.eq("calonSiswa", calonSiswa)).setMaxResults(1).uniqueResult();

				if (ruangGelombangPendaftaranPsbPSB == null && calonSiswa.getNoUjian() != null
						&& !calonSiswa.getNoUjian().trim().isEmpty()) {
					ruangGelombangPendaftaranPsbPSB = CommonPSB.dapatkanRuangUjian(calonSiswa);
				}

				if (ruangGelombangPendaftaranPsbPSB == null) {
					warnings.add("Error : " + Common.getBahasaConfig(
							"Kuota / ruangan penerimaan calon siswa telah penuh, harap hubungi petugas."));

				}
				if (!warnings.isEmpty()) {
					return null;
				}
				calonSiswa.setNoUjian(noUjian);
			}
		}

		return generalValueObject;
	}

	private static String pesanKonflikPmbDariCve(org.hibernate.exception.ConstraintViolationException cve) {
		String gabung = "";
		if (cve.getMessage() != null) gabung += cve.getMessage().toLowerCase();
		if (cve.getCause() != null && cve.getCause().getMessage() != null)
			gabung += " " + cve.getCause().getMessage().toLowerCase();
		if (gabung.contains("no_registrasi") || gabung.contains("noregistrasi"))
			return "[PMB-DUP-REG] Nomor registrasi yang digenerate sistem sudah terpakai oleh data lain "
					+ "(kemungkinan ada dua pendaftaran bersamaan). "
					+ "Langkah yang disarankan: tutup tab ini, buka kembali halaman pendaftaran, lalu coba simpan lagi. "
					+ "Apabila kendala berulang lebih dari 2 kali, hubungi petugas administrasi PMB dan sampaikan kode: PMB-DUP-REG.";
		if (gabung.contains("nik") || gabung.contains("noidentitas") || gabung.contains("no_identitas"))
			return "[PMB-DUP-NIK] Nomor Induk Kependudukan (NIK) yang Anda masukkan sudah terdaftar dalam sistem. "
					+ "Kemungkinan Anda pernah mendaftar sebelumnya pada gelombang atau program lain. "
					+ "Langkah yang disarankan: (1) Periksa kotak masuk email Anda untuk konfirmasi pendaftaran sebelumnya, "
					+ "(2) Hubungi petugas administrasi PMB dan sampaikan NIK serta kode: PMB-DUP-NIK.";
		if (gabung.contains("email"))
			return "[PMB-DUP-EML] Alamat surel (email) yang Anda masukkan sudah terdaftar dalam sistem. "
					+ "Langkah yang disarankan: (1) Periksa kotak masuk email tersebut untuk data pendaftaran sebelumnya, "
					+ "(2) Gunakan alamat surel lain jika Anda belum pernah mendaftar, "
					+ "(3) Hubungi petugas dan sampaikan kode: PMB-DUP-EML.";
		if (gabung.contains("no_hp") || gabung.contains("nohp") || gabung.contains("telepon") || gabung.contains("phone"))
			return "[PMB-DUP-HP] Nomor telepon/HP yang Anda masukkan sudah terdaftar dalam sistem. "
					+ "Langkah yang disarankan: (1) Gunakan nomor HP lain jika Anda belum pernah mendaftar, "
					+ "(2) Hubungi petugas administrasi PMB dan sampaikan kode: PMB-DUP-HP.";
		if (gabung.contains("no_ujian") || gabung.contains("noujian"))
			return "[PMB-DUP-UJI] Nomor ujian yang digenerate sistem sudah terpakai. "
					+ "Langkah yang disarankan: muat ulang halaman dan coba simpan kembali. "
					+ "Jika kendala berulang, hubungi petugas dengan kode: PMB-DUP-UJI.";
		return "[PMB-DUP-UMM] Data tidak dapat disimpan karena terdapat isian yang sama dengan data yang sudah ada di sistem. "
				+ "Langkah yang disarankan: (1) Periksa kembali isian NIK, surel, dan nomor telepon, "
				+ "(2) Pastikan Anda belum mendaftar sebelumnya, "
				+ "(3) Hubungi petugas administrasi PMB dan sampaikan kode: PMB-DUP-UMM.";
	}

	private static Integer parseIntegerAman(String property, String strid, Object oldValue) {
		if (strid == null || strid.trim().isEmpty()) return null;
		try {
			return Integer.valueOf(strid.trim());
		} catch (NumberFormatException e) {
			System.out.println("[API-WARN] Nilai non-angka untuk field Integer '" + property + "': " + strid
					+ ". Nilai lama dipertahankan.");
			return oldValue instanceof Integer ? (Integer) oldValue : null;
		}
	}

	private static Long parseLongAman(String property, String strid, Object oldValue) {
		if (strid == null || strid.trim().isEmpty()) return null;
		try {
			return Long.valueOf(strid.trim());
		} catch (NumberFormatException e) {
			System.out.println("[API-WARN] Nilai non-angka untuk field Long '" + property + "': " + strid
					+ ". Nilai lama dipertahankan.");
			return oldValue instanceof Long ? (Long) oldValue : null;
		}
	}

	private static Double parseDoubleAman(String property, String strid, Object oldValue) {
		if (strid == null || strid.trim().isEmpty()) return null;
		try {
			return Double.valueOf(strid.trim());
		} catch (NumberFormatException e) {
			System.out.println("[API-WARN] Nilai non-angka untuk field Double '" + property + "': " + strid
					+ ". Nilai lama dipertahankan.");
			return oldValue instanceof Double ? (Double) oldValue : null;
		}
	}

	private static String labelObjectAman(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof GeneralValueObject) {
			Serializable id = ((GeneralValueObject) value).getId();
			return value.getClass().getName() + "#" + (id == null ? "baru" : id.toString());
		}
		if (value instanceof Tbmuser) {
			Serializable id = ((Tbmuser) value).getUserId();
			return value.getClass().getName() + "#" + (id == null ? "baru" : id.toString());
		}
		if (value instanceof Tbmrole) {
			Serializable id = ((Tbmrole) value).getRoleId();
			return value.getClass().getName() + "#" + (id == null ? "baru" : id.toString());
		}
		try {
			return value.toString();
		} catch (Exception e) {
			return value.getClass().getName();
		}
	}

	@SuppressWarnings({ })
	public static JSONObject daftarAmbilKrs(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				String tahunAkademik = request.getString("tahunAkademik");
				Integer semester = request.isNull("semester") ? null : request.getInt("semester");
				String semesterPendek = request.isNull("semesterPendek") ? null : request.getString("semesterPendek");
				String kelas = request.isNull("kelas") ? "" : request.getString("kelas");
				String namaMk = request.isNull("mk") ? "" : request.getString("mk");

				Long jurusan = request.isNull("jurusan") ? -1L : Long.parseLong(request.get("jurusan") + "");
				String program = request.isNull("program") ? "" : request.get("program") + "";

				Mahasiswa mahasiswa = tbmuser.getMahasiswa();

				List<String> warnings = LaporanApi.warningsKrs(mahasiswa,
						semester == null ? mahasiswa.currentSemester() : semester,
						semesterPendek != null ? Perkuliahan.SEMESTER_PENDEK : null, false, true);
				if (!warnings.isEmpty()) {

					String w = "";
					for (String wa : warnings) {
						w += w.isEmpty() ? wa : "\n\n" + wa;
					}

					jsonObject.put("status", "97");
					jsonObject.put("description", w);

				} else {

					List<Long> perkuliahanTelahDiambil = new ArrayList<Long>();
					for (Long oid : mahasiswa.ambilDetailperkuliahan()) {
						Detailperkuliahan o = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								oid.toString());
						if (o != null) {
							if (o.getPerkuliahan() != null && o.getSemester().equals(semester)) {
								perkuliahanTelahDiambil.add(o.getPerkuliahan().getMatakuliah().getId());
							}
						}
					}

					System.out.println("perkuliahanTelahDiambil -> " + perkuliahanTelahDiambil.size());

					Session session = HibernateUtil.openSession();
					try {
					Criteria criteria = session.createCriteria(Perkuliahan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
					criteria.add(kelas.trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("kelas", kelas.trim(), MatchMode.ANYWHERE))

							.add(semester == null ? Restrictions.sqlRestriction("1=1")
									: Restrictions.eq("semester", semester))

							.add(perkuliahanTelahDiambil.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.not(Restrictions.in("matakuliah.id", perkuliahanTelahDiambil)))

							.add(program == null || program.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("program", program))

							.add(jurusan == null || jurusan.equals(-1L) ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("jurusan.id", jurusan))

							.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
									: Restrictions.eq("statusSemesterPendek", semesterPendek))

							.add(Restrictions.eq("tahunAjaran", tahunAkademik))

							.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
									Restrictions.isNull("merupakan_paralel")))

							.add(namaMk.trim().isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(
											Restrictions.ilike("matakuliah.kode", namaMk.trim(), MatchMode.ANYWHERE),
											Restrictions.ilike("matakuliah.nama", namaMk.trim(), MatchMode.ANYWHERE)));

					criteria.add(Restrictions.sqlRestriction(
							"1=1 order by case hari when 'Senin' then 1 when 'Selasa' then 2 when 'Rabu' then 3 when 'Kamis' then 4 when 'Jumat' then 5  when 'Sabtu' then 6 when 'Minggu' then 7 else 5 end, waktu_mulai_d"));
					JSONArray object = new JSONArray();
					List<Perkuliahan> matakuliah = ConstantValues.simpleList(criteria, Perkuliahan.class);
					System.out.println("matakuliah -> " + matakuliah.size());
					List<Perkuliahan> perkuliahans = new ArrayList<Perkuliahan>();
					for (Perkuliahan perkuliahan : matakuliah) {
						if (perkuliahan != null && perkuliahan.getKurikulum() != null
								&& perkuliahan.getKurikulum().bolehAmbil(mahasiswa)) {
							perkuliahans.add(perkuliahan);
						}
					}
					matakuliah = null;
					System.out.println("perkuliahans -> " + perkuliahans.size());
					for (Perkuliahan perkuliahan : perkuliahans) {
						JSONObject d = new JSONObject();
						d.put("id", perkuliahan.getId());
						d.put("idSmt", perkuliahan.getIdSmt());
						int ii = 1;
						for (Dosen dsn : perkuliahan.populateDosenBuNama()) {
							String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(dsn));
							d.put("foto_dsn_" + ii, url);
							d.put("nama_dsn_" + ii, dsn.getNama());
							d.put("nidn_dsn_" + ii, dsn.getNidn());
							ii++;
						}

						d.put("kode_mk", perkuliahan.getMatakuliah().getKode());
						d.put("nama_mk", perkuliahan.getMatakuliah().getNama());
						d.put("sks_mk", perkuliahan.getMatakuliah().getSks());
						d.put("hari", perkuliahan.getHari());
						d.put("waktu", perkuliahan.getWaktuMulai() + " " + perkuliahan.getWaktuSelesai());

						Integer kapasitasKelas = perkuliahan.getKapasitasKelas();
						PembagianKuotaPerkuliahanBerdasarkantahunAngkatan pembagianKuotaPerkuliahanBerdasarkantahunAngkatan = KrsUtilHelper
								.ambilPembagianKuotaPerkuliahanBerdasarkantahunAngkatan(session, perkuliahan,
										mahasiswa.getTahunangkatan(), false);
						Number kuota = pembagianKuotaPerkuliahanBerdasarkantahunAngkatan == null ? null
								: pembagianKuotaPerkuliahanBerdasarkantahunAngkatan.getKuota();
						if (kuota != null) {
							kapasitasKelas = kuota.intValue();
						}

						d.put("kapasitasKelas", kapasitasKelas);
						Integer jumlahUdahMasuk = KrsUtilHelper.ambilJumlahDetailperkuliahan(session, perkuliahan,
								true);
						d.put("jumlahUdahMasuk", jumlahUdahMasuk);
						object.put(d);
					}
					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);

					jsonObject.put("data", object);

					jsonObject.put("status", "00");
					jsonObject.put("description", "Ambil data berhasil");
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2666");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("rawtypes")
	public static JSONObject simpanDataBanyak(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Class clazz = Class.forName(request.getString("class"));
				Session session = request.getString("class").startsWith("ais.database.model.file.")
						|| request.getString("class").startsWith("ais.database.model.streaming.")
								? StreamingHibernateUtil.getInstance().openSession()
								: HibernateUtil.openSession();
				try {

				JSONArray jsonArray = request.getJSONArray("data");
				JSONArray jsonArrayHasil = new JSONArray();

				List<String> warnings = new ArrayList<String>();

				for (int i = 0; i < jsonArray.length(); i++) {
					try {
						JSONObject ss = jsonArray.getJSONObject(i);

						GeneralValueObject generalValueObject = ElearningApiUtil.simpanData(clazz, session, ss, tbmuser,
								warnings, req);
						if (generalValueObject != null) {
							JSONObject object = new JSONObject();
							Common.insertProperty(clazz, generalValueObject, object, "", 1);
							jsonArrayHasil.put(object);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2705");
					}
				}

				try {
					// session.disconnect();
					if (session.isOpen()) {
						try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:2712");}
						session.disconnect();
						session.close();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:2716");
				}

				if (request.getString("class").startsWith("ais.database.model.file.")
						|| request.getString("class").startsWith("ais.database.model.streaming.")) {
					StreamingHibernateUtil.getInstance().closeSession();
				}
				jsonObject.put("data", jsonArrayHasil);

				if (!warnings.isEmpty()) {
					String w = "";
					for (String ww : warnings) {
						w += w.isEmpty() ? ww : "\n" + ww;
					}
					jsonObject.put("status", "90");
					jsonObject.put("description", w);
				} else {
					jsonObject.put("status", "00");
					jsonObject.put("description", "Simpan data berhasil");
				}

				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2747");
			}
		}
		return jsonObject;
	}

	public static JSONObject simpanDataRinci(HttpServletRequest req, JSONObject request) {
		return simpanDataRinci(req, request, false);
	}

	@SuppressWarnings("rawtypes")
	public static JSONObject prosesSimpan(HttpServletRequest req, JSONObject request, Tbmuser tbmuser)
			throws Exception {
		JSONObject jsonObject = new JSONObject();
		Class clazz = Class.forName(request.getString("class"));

		// Gerbang CRUD granular per grup pengguna (Tbmrole.ebisnisMenu.crud, lihat
		// ais.common.EbisnisMenuKatalog) -- HANYA utk 2 kelas master e-Kantin yang layar admin JSP-nya
		// (webapp/WEB-INF/baru/modul/kantin/cara_bayar/index.jsp, .../penyedia.jsp) menyimpan lewat
		// endpoint reflektif generik ini (bukan lewat KantinHelper/PosApi). Kelas lain yang lewat
		// endpoint yang sama (elearning, streaming file, dll) SAMA SEKALI tidak terpengaruh (kunciCrudSimpan
		// tetap null utk mereka). Default-allow (lihat bolehAksi) -- role lama yg blm pernah menyimpan
		// grid CRUD ini tetap boleh spt sebelumnya, hanya role yg eksplisit di-uncheck admin yg ditolak.
		String kelasNamaSimpan = request.getString("class");
		String kunciCrudSimpan = "ais.database.model.koperasi.CaraPembayaranKoperasi".equals(kelasNamaSimpan)
				? "pembayaran"
				: "ais.database.model.asset.PenyediaAsset".equals(kelasNamaSimpan) ? "penyedia" : null;
		if (kunciCrudSimpan != null) {
			ais.database.model.Tbmrole roleSimpan = tbmuser == null ? null : tbmuser.hakAkses();
			boolean adaIdSimpan = !request.isNull("id");
			String aksiSimpan = adaIdSimpan ? "update" : "create";
			if (roleSimpan != null && !ais.common.EbisnisMenuKatalog.bolehAksi(
					ais.common.EbisnisMenuKatalog.urai(roleSimpan.getEbisnisMenu()), kunciCrudSimpan, aksiSimpan)) {
				jsonObject.put("status", "91");
				jsonObject.put("description",
						"Anda tidak memiliki izin untuk " + (adaIdSimpan ? "mengubah" : "menambah") + " data ini.");
				return jsonObject;
			}
		}

		Session session = request.getString("class").startsWith("ais.database.model.file.")
				|| request.getString("class").startsWith("ais.database.model.streaming.")
						? StreamingHibernateUtil.getInstance().openSession()
						: HibernateUtil.openSession();
		List<String> warnings = new ArrayList<String>();

		GeneralValueObject generalValueObject;
		try {
			generalValueObject = ElearningApiUtil.simpanData(clazz, session, request, tbmuser, warnings, req);
		} finally {
			// Session lokal hasil openSession() wajib ditutup di finally, termasuk
			// ketika simpanData melempar exception (method ini "throws Exception").
			HibernateUtil.closeSessionQuietly(session);
		}

		if (!warnings.isEmpty()) {
			String w = "";
			for (String ww : warnings) {
				w += w.isEmpty() ? ww : "\n" + ww;
			}
			jsonObject.put("status", "90");
			jsonObject.put("description", w);
		} else if (generalValueObject != null) {
			JSONObject object = new JSONObject();
			Common.insertProperty(clazz, generalValueObject, object, "", 1);
			jsonObject.put("data", object);
			jsonObject.put("id", generalValueObject.getId());
			jsonObject.put("class", request.get("class"));
			jsonObject.put("status", "00");
			jsonObject.put("description", "Simpan data berhasil");
			GeneralValueObject.masukkanData(clazz, generalValueObject);
		} else {
			jsonObject.put("status", "01");
			jsonObject.put("description", "Simpan data gagal");
		}
		return jsonObject;
	}

	public static JSONObject simpanDataRinci(HttpServletRequest req, JSONObject request, boolean pakaiToken) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (!pakaiToken) {
				Tbmuser tbmuser = Common.getCurrentUser(req);
				jsonObject = prosesSimpan(req, request, tbmuser);
			} else {

				Tbmuser tbmuser = ApiUtil.currentUser(request, req);
				if (tbmuser == null || tbmuser.getUserId() == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Token tidak sesuai");
				} else {
					jsonObject = prosesSimpan(req, request, tbmuser);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2818");
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2824");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("rawtypes")
	private static String alasanPedagangTidakBolehDihapus(Session session, GeneralValueObject data) {
		if (!(data instanceof Pedagang)) {
			return null;
		}

		List pengguna = session.createCriteria(Tbmuser.class)
				.add(Restrictions.eq("pedagang", data)).list();
		if (pengguna == null || pengguna.isEmpty()) {
			return null;
		}

		Pedagang pedagang = (Pedagang) data;
		String nama = pedagang.getNama() == null ? "ID " + pedagang.getId() : pedagang.getNama();
		StringBuilder akun = new StringBuilder();
		int batas = Math.min(pengguna.size(), 3);
		for (int i = 0; i < batas; i++) {
			Tbmuser user = (Tbmuser) pengguna.get(i);
			if (akun.length() > 0) {
				akun.append(", ");
			}
			akun.append(user.getUserId() == null ? "akun tanpa ID" : user.getUserId());
		}
		if (pengguna.size() > batas) {
			akun.append(" dan ").append(pengguna.size() - batas).append(" akun lain");
		}

		return "Pedagang '" + nama + "' belum dapat dihapus karena masih menjadi identitas "
				+ pengguna.size() + " akun pengguna (" + akun + "). Tindakan: buka Konfigurasi > "
				+ "Akun Pengguna. Jika akun tidak lagi dipakai, ubah Status menjadi Nonaktif; data "
				+ "Pedagang tidak perlu dihapus. Jika penghapusan permanen memang wajib, minta admin "
				+ "sistem memindahkan atau melepaskan keterkaitan akun tersebut terlebih dahulu, lalu "
				+ "coba hapus kembali. Jangan menghapus atau mengubah relasi langsung di database.";
	}

	private static long jumlahPengumpulanTugasKelompok(Long tugasKelompokId) {
		Session streamingSession = null;
		try {
			streamingSession = StreamingHibernateUtil.getInstance().openSession();
			Object jumlah = streamingSession.createCriteria(TugasFileContent.class)
					.add(Restrictions.eq("pertemuan", tugasKelompokId))
					.add(Restrictions.or(Restrictions.isNull("classFrom"),
							Restrictions.ilike("classFrom", TugasKelompok.class.getName(), MatchMode.START)))
					.setProjection(Projections.rowCount()).uniqueResult();
			return jumlah instanceof Number ? ((Number) jumlah).longValue() : 0L;
		} finally {
			if (streamingSession != null && streamingSession.isOpen()) {
				streamingSession.close();
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static JSONObject prosesHapus(HttpServletRequest req, JSONObject request) throws Exception {
		JSONObject jsonObject = new JSONObject();
		Class clazz = Class.forName(request.getString("class"));

		// Gerbang CRUD granular (pola sama dgn prosesSimpan di atas) utk 2 kelas master e-Kantin yang
		// sama; kelas lain yang lewat endpoint reflektif generik ini tidak terpengaruh.
		String kelasNamaHapus = request.getString("class");
		String kunciCrudHapus = "ais.database.model.koperasi.CaraPembayaranKoperasi".equals(kelasNamaHapus)
				? "pembayaran"
				: "ais.database.model.asset.PenyediaAsset".equals(kelasNamaHapus) ? "penyedia" : null;
		if (kunciCrudHapus != null) {
			Tbmuser tbmuserHapus = Common.getCurrentUser(req);
			ais.database.model.Tbmrole roleHapus = tbmuserHapus == null ? null : tbmuserHapus.hakAkses();
			if (roleHapus != null && !ais.common.EbisnisMenuKatalog.bolehAksi(
					ais.common.EbisnisMenuKatalog.urai(roleHapus.getEbisnisMenu()), kunciCrudHapus, "delete")) {
				jsonObject.put("status", "91");
				jsonObject.put("description", "Anda tidak memiliki izin untuk menghapus data ini.");
				return jsonObject;
			}
		}

		Long id = angkaIdAtauNull(request.opt("id"));
		if (id == null) {
			/* KE-FIX: sebelumnya id kosong/bukan angka melempar NumberFormatException mentah
			 * ("For input string: &quot;&quot;") yang tampil ke pengguna sebagai layar galat.
			 * Tanpa id, tidak ada baris yang bisa dihapus -- balas dengan pesan yang jelas dan
			 * jangan membuka session/koneksi sama sekali. */
			jsonObject.put("status", "92");
			jsonObject.put("description", "Data yang akan dihapus tidak dikenali (id kosong).");
			return jsonObject;
		}
		Session session = request.getString("class").startsWith("ais.database.model.file.")
				|| request.getString("class").startsWith("ais.database.model.streaming.")
						? StreamingHibernateUtil.getInstance().openSession()
						: HibernateUtil.openSession();
		try {

		GeneralValueObject generalValueObject = ConstantValues
				.simpleObject(session.createCriteria(clazz).add(Restrictions.idEq(id)), clazz);
		if (generalValueObject != null) {
			if (generalValueObject instanceof TugasKelompok) {
				TugasKelompok tugasKelompok = (TugasKelompok) generalValueObject;
				Tbmuser pengguna = Common.getCurrentUser(req);
				Pertemuan pertemuanTugas = tugasKelompok.getPertemuan() == null ? null
						: (Pertemuan) session.get(Pertemuan.class, tugasKelompok.getPertemuan());
				if (pengguna == null || pengguna.getUserId() == null || pertemuanTugas == null
						|| !pertemuanTugas.bolehUbahAbsenSaja(pengguna)) {
					jsonObject.put("status", "91");
					jsonObject.put("description", "Anda tidak memiliki izin untuk menghapus tugas kelompok ini.");
					return jsonObject;
				}

				long jumlahKelompok = ((Number) session.createCriteria(NamaTugasKelompok.class)
						.add(Restrictions.eq("tugasKelompok", tugasKelompok))
						.setProjection(Projections.rowCount()).uniqueResult()).longValue();
				long jumlahPengumpulan = jumlahPengumpulanTugasKelompok(tugasKelompok.getId());
				if (jumlahKelompok > 0 || jumlahPengumpulan > 0) {
					jsonObject.put("status", "93");
					jsonObject.put("description", jumlahPengumpulan > 0
							? "Tugas kelompok tidak dapat dihapus karena sudah memiliki berkas pengumpulan."
							: "Tugas kelompok tidak dapat dihapus karena pembagian kelompok sudah dibuat.");
					return jsonObject;
				}
			}

			JSONObject object = new JSONObject();
			Common.insertProperty(clazz, generalValueObject, object, "", 1);
			jsonObject.put("data", object);

			// Pedagang dapat menjadi identitas login pada tbmuser. Tolak sebelum DELETE agar
			// koneksi tidak masuk status aborted dan pengguna mendapat langkah penyelesaian.
			String alasanTidakBolehDihapus = alasanPedagangTidakBolehDihapus(session, generalValueObject);
			if (alasanTidakBolehDihapus != null) {
				jsonObject.put("status", "91");
				jsonObject.put("description", alasanTidakBolehDihapus);
				return jsonObject;
			}

			Transaction transaction = null;
			boolean berhasilHapus = false;
			try {
				// 1. WAJIB mulai transaksi
				transaction = session.beginTransaction();
				if (generalValueObject.getId() != null) {
					// Hapus baris ANAK yang mereferensi entity ini agar tidak melanggar FK.
					// PembelianAnggotaKoperasi (header transaksi) memiliki baris Pembelian (item);
					// FK pembelian.pembelian_anggota_koperasi mencegah delete header selama item ada.
					if (generalValueObject instanceof ais.database.model.koperasi.PembelianAnggotaKoperasi) {
						session.createQuery("delete from ais.database.model.inventory.Pembelian p "
								+ "where p.pembelianAnggotaKoperasi = :header")
								.setParameter("header", generalValueObject).executeUpdate();
					}
					session.delete(generalValueObject);
				}

				// 3. Commit transaksi jika semuanya lancar
				transaction.commit();
				berhasilHapus = true;

			} catch (Exception e) {
				// 4. ROLLBACK jika terjadi error (mencegah lock menggantung di database)
				if (transaction != null && transaction.isActive()) {
					try {
						transaction.rollback();
						System.err.println("Transaction di-rollback karena terjadi error.");
					} catch (Exception rollbackEx) {
						System.err.println("Gagal melakukan rollback: " + rollbackEx.getMessage());
					}
				}

				// Sebaiknya gunakan Logger (misal: log.error) alih-alih printStackTrace di
				// production
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2883");

			} finally {
				// 5. WAJIB Tutup Session dengan bersih
				if (session != null && session.isOpen()) {
					try {
						// Tidak perlu session.disconnect(), cukup close() agar koneksi kembali ke pool
						session.close();
					} catch (Exception e) {
						System.err.println("Gagal menutup session: " + e.getMessage());
					}
				}
			}

			if (berhasilHapus) {
				jsonObject.put("status", "00");
				jsonObject.put("description", "Hapus data berhasil");
			} else {
				// JANGAN laporkan "berhasil" saat delete gagal (mis. FK: data masih dipakai
				// transaksi lain). Kembalikan status gagal yang jujur.
				jsonObject.put("status", "98");
				jsonObject.put("description",
						"Hapus data gagal: data masih terkait/direferensi transaksi lain");
			}
		} else {
			jsonObject.put("status", "00");
			jsonObject.put("description", "Hapus data gagal, data tidak ditemukan");
		}

		} finally {
			// Session lokal hasil openSession() selalu ditutup di finally
			HibernateUtil.closeSessionQuietly(session);
		}

		return jsonObject;
	}

	public static JSONObject hapusDataRinci(HttpServletRequest req, JSONObject request) {
		return hapusDataRinci(req, request, false);
	}

	public static JSONObject hapusDataRinci(HttpServletRequest req, JSONObject request, boolean pakaiToken) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (!pakaiToken) {
				jsonObject = prosesHapus(req, request);
			} else {
				Tbmuser tbmuser = ApiUtil.currentUser(request, req);
				if (tbmuser == null || tbmuser.getUserId() == null) {
					jsonObject.put("status", "97");
					jsonObject.put("description", "Token tidak sesuai");
				} else {

					jsonObject = prosesHapus(req, request);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:2946");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject aktiftasPerkuliahanInfo(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Long id = Long.parseLong(String.valueOf(request.get("id")));

				Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(), id);

				Session session = HibernateUtil.openSession();
				try {
				int referensi = ((Number) session.createCriteria(PerkuliahanPunyaItem.class)
						.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();
				jsonObject.put("referensi", referensi);
				int bukuAjar = ((Number) session.createCriteria(MatakuliahPunyaBukuBahanAjar.class)
						.add(Restrictions.eq("matakuliah", perkuliahan.getMatakuliah()))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();
				jsonObject.put("bukuAjar", bukuAjar);
				int tugasKelompok = ((Number) session.createCriteria(TugasKelompok.class)
						.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
						.uniqueResult()).intValue();
				jsonObject.put("tugasKelompok", tugasKelompok);

				Object[] jml = perkuliahan.ambilJumlahPertemuanStatistik(true, true);
				int mhsSize = perkuliahan.ambilJumlahDetailperkuliahanLangsung();
				jsonObject.put("mhsSize", mhsSize);
				Collection<Pertemuan> pertemuans = (Collection<Pertemuan>) (jml == null || jml[7] == null
						? new ArrayList<Pertemuan>()
						: jml[7]);

				int jumlahUjianTotal = jml == null || jml[8] == null ? 0 : Integer.parseInt(jml[8].toString());
				int jumlahDiskusiTotal = jml == null || jml[9] == null ? 0 : Integer.parseInt(jml[9].toString());
				int pertemuan_file_content = 0;
				int tugas_file_content = 0;
				int audio_pertemuan = 0;
				int video_pertemuan = 0;
				for (Pertemuan ids : pertemuans) {
					pertemuan_file_content += ids.ambilJumlahPertemuanFileContent();
					tugas_file_content += ids.ambilJumlahTugasFileContent();
					audio_pertemuan += ids.ambilJumlahAudioPertemuan();
					video_pertemuan += ids.ambilJumlahVideoPertemuan();
				}

				jsonObject.put("ujian", jumlahUjianTotal);
				jsonObject.put("dikusi", jumlahDiskusiTotal);
				jsonObject.put("materi", pertemuan_file_content);
				jsonObject.put("tugas", tugas_file_content);
				jsonObject.put("audio", audio_pertemuan);
				jsonObject.put("video", video_pertemuan);

				int total = jml == null || jml[0] == null ? 0 : Integer.parseInt(jml[0].toString());
				int jumlah = jml == null || jml[1] == null ? 0 : Integer.parseInt(jml[1].toString());
				int absen = jml == null || jml[3] == null ? 0 : Integer.parseInt(jml[3].toString());
				Map<String, Integer> statuses = (Map<String, Integer>) (jml == null || jml[4] == null ? null : jml[4]);
				String abs = statuses == null ? ""
						: statuses.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();

				int absenDosen = jml == null || jml[5] == null ? 0 : Integer.parseInt(jml[5].toString());
				Map<String, Integer> statusesDosen = (Map<String, Integer>) (jml == null || jml[6] == null ? null
						: jml[6]);
				String absDosen = statusesDosen == null ? ""
						: statusesDosen.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();

				Integer persen = total == 0 ? 0 : total == jumlah ? 100 : ((jumlah * 100) / total);

				jsonObject.put("total_abs", total);
				jsonObject.put("jumlah_abs", jumlah);
				jsonObject.put("absen_abs", absen);
				jsonObject.put("abs", abs);
				jsonObject.put("persen", persen);

				jsonObject.put("absenDosen", absenDosen);
				jsonObject.put("absDosen", absDosen);

				int totalDosen = total == 0 || perkuliahan == null ? 0 : (perkuliahan.getJumlahDosen() * total);
				persen = totalDosen == 0 ? 0 : (totalDosen == absenDosen ? 100 : ((absenDosen * 100) / totalDosen));
				jsonObject.put("totalDosen", totalDosen);
				jsonObject.put("persenDosen", persen);

				jsonObject.put("status", "00");
				jsonObject.put("description", "Pengambilan data berhasil");
				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:3048");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject agenda(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				boolean refresh = request.isNull("refresh") ? false
						: (request.get("refresh") + "").trim().equalsIgnoreCase("true");
				Integer jenis = request.isNull("jenis") ? TampilanELearningAction.PERKULIAHAN
						: Integer.parseInt(request.getString("jenis").trim());
				String tahunAkademik = request.isNull("tahunAkademik") ? null
						: request.getString("tahunAkademik").trim();
				String jenisSemester = request.isNull("jenisSemester") ? null
						: request.getString("jenisSemester").trim();

				String hr = request.isNull("hari") ? null : request.getString("hari").trim();
				String keyword = request.isNull("keyword") ? "" : request.getString("keyword").trim();
				String kelas = request.isNull("kelas") ? "" : request.getString("kelas").trim();

				boolean pra = request.isNull("pra") ? false : request.getString("pra").trim().equalsIgnoreCase("true");
				boolean ekstra = request.isNull("ekstra") ? false
						: request.getString("ekstra").trim().equalsIgnoreCase("true");
				boolean remedial = request.isNull("remedial") ? false
						: request.getString("remedial").trim().equalsIgnoreCase("true");
				boolean paralel = request.isNull("paralel") ? false
						: request.getString("paralel").trim().equalsIgnoreCase("true");

				boolean requestStatus = request.isNull("requestStatus") ? true
						: request.getString("requestStatus").trim().equalsIgnoreCase("true");
				boolean aktifStatus = request.isNull("aktifStatus") ? true
						: request.getString("aktifStatus").trim().equalsIgnoreCase("true");

				boolean seminarStatus = request.isNull("seminarStatus") ? true
						: request.getString("seminarStatus").trim().equalsIgnoreCase("true");

				boolean lulusStatus = request.isNull("lulusStatus") ? true
						: request.getString("lulusStatus").trim().equalsIgnoreCase("true");
				boolean gagalStatus = request.isNull("gagalStatus") ? true
						: request.getString("gagalStatus").trim().equalsIgnoreCase("true");
				boolean belumStatus = request.isNull("belumStatus") ? true
						: request.getString("belumStatus").trim().equalsIgnoreCase("true");

				boolean setujuStatus = request.isNull("setujuStatus") ? true
						: request.getString("setujuStatus").trim().equalsIgnoreCase("true");

				boolean mengulangStatus = request.isNull("mengulangStatus") ? true
						: request.getString("mengulangStatus").trim().equalsIgnoreCase("true");

				boolean sidangStatus = request.isNull("sidangStatus") ? true
						: request.getString("sidangStatus").trim().equalsIgnoreCase("true");

				Integer jumlahDataDalamSatuHalaman = request.isNull("jumlahDataDalamSatuHalaman") ? 10
						: Integer.parseInt(request.getString("jumlahDataDalamSatuHalaman").trim());
				Integer halaman = request.isNull("halaman") ? 0 : Integer.parseInt(request.getString("halaman").trim());

				Mahasiswa mahasiswa = tbmuser.getMahasiswa();
				Dosen dosen = tbmuser.ambilDosen();
				List<? extends VOPembelajaran> voPembelajarans;
				int size;

				if (mahasiswa != null) {
					if (refresh) {
						Session session = null;
						try {
							session = HibernateUtil.openSession();
							try {

							if (jenis.equals(TampilanELearningAction.PERKULIAHAN)) {
								mahasiswa.reInitDetailperkuliahan(session);
							} else if (jenis.equals(TampilanELearningAction.SKRIPSI)) {
								mahasiswa.reInitSkripsi(session);
							} else if (jenis.equals(TampilanELearningAction.BIMBINGAN)) {
								mahasiswa.reInitBimbingan(session);
							} else if (jenis.equals(TampilanELearningAction.KKN)) {
								mahasiswa.reInitKkn(session);
							} else if (jenis.equals(TampilanELearningAction.PKL)) {
								mahasiswa.reInitPkl(session);
							} else if (jenis.equals(TampilanELearningAction.KRS)) {
								mahasiswa.reInitKrs(session);
							} else if (jenis.equals(TampilanELearningAction.KEGIATAN)) {
								mahasiswa.reInitFormulirKegiatanPeserta(session);
							} else if (jenis.equals(TampilanELearningAction.KONSULTASI)) {
								mahasiswa.reInitKonsultasi(session);
							}
							// session.disconnect();
							if (session.isOpen()) {
								try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:3144");}
								session.disconnect();
								session.close();
							}
							} finally {
								HibernateUtil.closeSessionQuietly(session);
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:3152");
						} finally {
							// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
							// finally menjamin penutupan walau exception (idempoten via isOpen()).
							if (session != null && session.isOpen()) {
								try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:3157");}
								try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:3158");}
							}
						}
					}

					Object[] objects = mahasiswa.ambilPerkuliahanDanParalel(tahunAkademik, jenisSemester, hr, keyword,
							kelas, pra, ekstra ? Perkuliahan.EKSTRA : null, true, remedial, paralel, jenis,
							jumlahDataDalamSatuHalaman * halaman, jumlahDataDalamSatuHalaman);
					voPembelajarans = (List<VOPembelajaran>) objects[0];
					size = (Integer) objects[1];
				} else if (dosen != null) {
					if (refresh) {
						Session session = null;
						try {
							session = HibernateUtil.openSession();
							try {
							if (jenis.equals(TampilanELearningAction.PERKULIAHAN)) {
								dosen.reInitPerkuliahan(session);
							} else if (jenis.equals(TampilanELearningAction.SKRIPSI)) {
								dosen.reInitSkripsi(session);
							} else if (jenis.equals(TampilanELearningAction.BIMBINGAN)) {
								dosen.reInitBimbingan(session);
							} else if (jenis.equals(TampilanELearningAction.KKN)) {
								dosen.reInitKkn(session);
							} else if (jenis.equals(TampilanELearningAction.PKL)) {
								dosen.reInitPkl(session);
							} else if (jenis.equals(TampilanELearningAction.KRS)) {
								dosen.reInitKrs(session);
							} else if (jenis.equals(TampilanELearningAction.KEGIATAN)) {
								dosen.reInitFormulirKegiatanPeserta(session);
							} else if (jenis.equals(TampilanELearningAction.KONSULTASI)) {
								dosen.reInitKonsultasi(session);
							}
							// session.disconnect();
							if (session.isOpen()) {
								try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:3193");}
								session.disconnect();
								session.close();
							}
							} finally {
								HibernateUtil.closeSessionQuietly(session);
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:3201");
						} finally {
							// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
							// finally menjamin penutupan walau exception (idempoten via isOpen()).
							if (session != null && session.isOpen()) {
								try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:3206");}
								try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:3207");}
							}
						}
					}

					Session session = HibernateUtil.openSession();
					try {

					Object[] objects = dosen.ambilPerkuliahanDanParalel(session, tahunAkademik, jenisSemester, hr,
							keyword, kelas, pra, ekstra ? Perkuliahan.EKSTRA : null, true, remedial, paralel,

							requestStatus, aktifStatus, seminarStatus, mengulangStatus, lulusStatus, gagalStatus,

							belumStatus, setujuStatus, sidangStatus,

							jenis,

							jumlahDataDalamSatuHalaman * halaman, jumlahDataDalamSatuHalaman);
					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);
					voPembelajarans = (List<VOPembelajaran>) objects[0];
					size = (Integer) objects[1];
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} else {
					Session session = HibernateUtil.openSession();
					try {
					size = ((Number) TampilanELearningAction
							.initStaticCriteria(false, jenis, keyword, tbmuser.ambilFakultas(), tbmuser.ambilJurusan(),
									tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama(),
									tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), tahunAkademik, jenisSemester, hr,
									remedial, pra, ekstra, paralel, requestStatus, aktifStatus, seminarStatus,
									lulusStatus, mengulangStatus, gagalStatus, belumStatus, setujuStatus, sidangStatus,
									kelas, tbmuser, session)
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					if (jenis.equals(TampilanELearningAction.PERKULIAHAN)) {
						voPembelajarans = ConstantValues
								.simpleList(TampilanELearningAction
										.initStaticCriteria(true, jenis, keyword, tbmuser.ambilFakultas(),
												tbmuser.ambilJurusan(),
												tbmuser.ambilProgram() == null ? null
														: tbmuser.ambilProgram().getNama(),
												tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), tahunAkademik,
												jenisSemester, hr, remedial, pra, ekstra, paralel, requestStatus,
												aktifStatus, seminarStatus, lulusStatus, mengulangStatus, gagalStatus,
												belumStatus, setujuStatus, sidangStatus, kelas, tbmuser, session)
										.setMaxResults(jumlahDataDalamSatuHalaman)
										.setFirstResult(jumlahDataDalamSatuHalaman * halaman), Perkuliahan.class);
					} else if (jenis.equals(TampilanELearningAction.SKRIPSI)) {
						voPembelajarans = ConstantValues
								.simpleList(TampilanELearningAction
										.initStaticCriteria(true, jenis, keyword, tbmuser.ambilFakultas(),
												tbmuser.ambilJurusan(),
												tbmuser.ambilProgram() == null ? null
														: tbmuser.ambilProgram().getNama(),
												tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), tahunAkademik,
												jenisSemester, hr, remedial, pra, ekstra, paralel, requestStatus,
												aktifStatus, seminarStatus, lulusStatus, mengulangStatus, gagalStatus,
												belumStatus, setujuStatus, sidangStatus, kelas, tbmuser, session)
										.setMaxResults(jumlahDataDalamSatuHalaman)
										.setFirstResult(jumlahDataDalamSatuHalaman * halaman), Skripsi.class);
					} else if (jenis.equals(TampilanELearningAction.BIMBINGAN)) {
						voPembelajarans = ConstantValues.simpleList(
								TampilanELearningAction
										.initStaticCriteria(true, jenis, keyword, tbmuser.ambilFakultas(),
												tbmuser.ambilJurusan(),
												tbmuser.ambilProgram() == null ? null
														: tbmuser.ambilProgram().getNama(),
												tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), tahunAkademik,
												jenisSemester, hr, remedial, pra, ekstra, paralel, requestStatus,
												aktifStatus, seminarStatus, lulusStatus, mengulangStatus, gagalStatus,
												belumStatus, setujuStatus, sidangStatus, kelas, tbmuser, session)
										.setMaxResults(jumlahDataDalamSatuHalaman)
										.setFirstResult(jumlahDataDalamSatuHalaman * halaman),
								MahasiswaRequestTugasAkhir.class);
					} else if (jenis.equals(TampilanELearningAction.KKN)) {
						voPembelajarans = ConstantValues
								.simpleList(TampilanELearningAction
										.initStaticCriteria(true, jenis, keyword, tbmuser.ambilFakultas(),
												tbmuser.ambilJurusan(),
												tbmuser.ambilProgram() == null ? null
														: tbmuser.ambilProgram().getNama(),
												tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), tahunAkademik,
												jenisSemester, hr, remedial, pra, ekstra, paralel, requestStatus,
												aktifStatus, seminarStatus, lulusStatus, mengulangStatus, gagalStatus,
												belumStatus, setujuStatus, sidangStatus, kelas, tbmuser, session)
										.setMaxResults(jumlahDataDalamSatuHalaman)
										.setFirstResult(jumlahDataDalamSatuHalaman * halaman), KelompokKkn.class);
					} else if (jenis.equals(TampilanELearningAction.PKL)) {
						voPembelajarans = ConstantValues
								.simpleList(TampilanELearningAction
										.initStaticCriteria(true, jenis, keyword, tbmuser.ambilFakultas(),
												tbmuser.ambilJurusan(),
												tbmuser.ambilProgram() == null ? null
														: tbmuser.ambilProgram().getNama(),
												tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), tahunAkademik,
												jenisSemester, hr, remedial, pra, ekstra, paralel, requestStatus,
												aktifStatus, seminarStatus, lulusStatus, mengulangStatus, gagalStatus,
												belumStatus, setujuStatus, sidangStatus, kelas, tbmuser, session)
										.setMaxResults(jumlahDataDalamSatuHalaman)
										.setFirstResult(jumlahDataDalamSatuHalaman * halaman), KelompokPkl.class);
					} else if (jenis.equals(TampilanELearningAction.KEGIATAN)) {
						voPembelajarans = ConstantValues
								.simpleList(TampilanELearningAction
										.initStaticCriteria(true, jenis, keyword, tbmuser.ambilFakultas(),
												tbmuser.ambilJurusan(),
												tbmuser.ambilProgram() == null ? null
														: tbmuser.ambilProgram().getNama(),
												tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), tahunAkademik,
												jenisSemester, hr, remedial, pra, ekstra, paralel, requestStatus,
												aktifStatus, seminarStatus, lulusStatus, mengulangStatus, gagalStatus,
												belumStatus, setujuStatus, sidangStatus, kelas, tbmuser, session)
										.setMaxResults(jumlahDataDalamSatuHalaman)
										.setFirstResult(jumlahDataDalamSatuHalaman * halaman), FormulirKegiatan.class);
					} else if (jenis.equals(TampilanELearningAction.KRS)) {
						voPembelajarans = ConstantValues
								.simpleList(TampilanELearningAction
										.initStaticCriteria(true, jenis, keyword, tbmuser.ambilFakultas(),
												tbmuser.ambilJurusan(),
												tbmuser.ambilProgram() == null ? null
														: tbmuser.ambilProgram().getNama(),
												tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), tahunAkademik,
												jenisSemester, hr, remedial, pra, ekstra, paralel, requestStatus,
												aktifStatus, seminarStatus, lulusStatus, mengulangStatus, gagalStatus,
												belumStatus, setujuStatus, sidangStatus, kelas, tbmuser, session)
										.setMaxResults(jumlahDataDalamSatuHalaman)
										.setFirstResult(jumlahDataDalamSatuHalaman * halaman)
										.setProjection(Projections.property("id")), KrsMahasiswa.class, false);
					} else if (jenis.equals(TampilanELearningAction.PELAJARAN)) {

						voPembelajarans = TampilanELearningAction
								.initStaticCriteria(true, jenis, keyword, tbmuser.ambilFakultas(),
										tbmuser.ambilJurusan(),
										tbmuser.ambilProgram() == null ? null : tbmuser.ambilProgram().getNama(),
										tbmuser.ambilYayasan(), tbmuser.ambilSekolah(), tahunAkademik, jenisSemester,
										hr, remedial, pra, ekstra, paralel, requestStatus, aktifStatus, seminarStatus,
										lulusStatus, mengulangStatus, gagalStatus, belumStatus, setujuStatus,
										sidangStatus, kelas, tbmuser, session)
								.setMaxResults(jumlahDataDalamSatuHalaman)
								.setFirstResult(jumlahDataDalamSatuHalaman * halaman).list();

					} else {
						voPembelajarans = new ArrayList<VOPembelajaran>();
					}
					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				}

				jsonObject.put("status", "00");
				jsonObject.put("description", "Pengambilan data berhasil");
				jsonObject.put("size", size);

				JSONArray data = new JSONArray();
				for (VOPembelajaran voPembelajaran : voPembelajarans) {
					JSONObject d = new JSONObject();

					int ii = 1;
					for (Dosen dsn : voPembelajaran.populateDosenBuNama()) {
						String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(dsn));
						d.put("dsn_" + ii, url);
						ii++;
					}

					Object[] jml = voPembelajaran.ambilJumlahPertemuanStatistik(false, false);
					int mhsSize = voPembelajaran.ambilJumlahDetailperkuliahanLangsung();
					String subta = "";
					String infoData = "";
					if (voPembelajaran instanceof JadwalPelajaran) {
						JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) voPembelajaran;

						ii = 1;
						for (Guru guru : jadwalPelajaran.populateGuruBuNama()) {
							String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(guru));
							d.put("guru_" + ii, url);
							ii++;
						}

						subta = jadwalPelajaran.getTahunAjaran() + "/" + (jadwalPelajaran.getSemester() == null
								? "Semua"
								: jadwalPelajaran.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);

						infoData = jadwalPelajaran.infoSimple();
					} else {
						subta = voPembelajaran.ambilTahunAjaran() + "/"
								+ (!voPembelajaran.ambilMerupakanSP() ? voPembelajaran.ambilJenisSemester()
										: Common.getBahasaConfig("Semester Pendek"));

						infoData = voPembelajaran.infoSimple();

					}

					int total = jml == null || jml[0] == null ? 0 : Integer.parseInt(jml[0].toString());
					int jumlah = jml == null || jml[1] == null ? 0 : Integer.parseInt(jml[1].toString());
					int absen = jml == null || jml[3] == null ? 0 : Integer.parseInt(jml[3].toString());

					Collection<Pertemuan> pertemuans = (Collection<Pertemuan>) (jml == null || jml[7] == null
							? new ArrayList<Pertemuan>()
							: jml[7]);

					Map<String, Integer> statuses = (Map<String, Integer>) (jml == null || jml[4] == null ? null
							: jml[4]);
					String abs = statuses == null ? ""
							: statuses.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();

					int absenDosen = jml == null || jml[5] == null ? 0 : Integer.parseInt(jml[5].toString());
					Map<String, Integer> statusesDosen = (Map<String, Integer>) (jml == null || jml[6] == null ? null
							: jml[6]);
					String absDosen = statusesDosen == null ? ""
							: statusesDosen.toString().replaceAll("\\{", "").replaceAll("\\}", "").trim();

					Integer persen = total == 0 ? 0 : total == jumlah ? 100 : ((jumlah * 100) / total);

					d.put("class", voPembelajaran.getClass().getName());
					d.put("id", voPembelajaran.getId());

					d.put("mhsSize", mhsSize);
					d.put("subta", subta);
					d.put("infoData", infoData);
					d.put("total", total);
					d.put("jumlah", jumlah);
					d.put("absen", absen);
					d.put("abs", abs);

					d.put("absenDosen", absenDosen);
					d.put("absDosen", absDosen);
					d.put("persen", persen);

					int pertemuan_file_content = 0;
					int tugas_file_content = 0;
					int tugas_kelompok = 0;
					int audio_pertemuan = 0;
					int video_pertemuan = 0;
					for (Pertemuan ids : pertemuans) {
						TreeMap<Long, TugasPertemuan> tugases = ids.ambilTugasPertemuanTotal();
						TreeMap<Long, TugasKelompok> tugasesKelompok = ids.ambilTugasKelompokTotal();

						tugas_file_content += ids.getJudultugas().trim().isEmpty() ? 0 : 1;

						for (TugasPertemuan tugasPertemuan : tugases.values()) {
							tugas_file_content += tugasPertemuan.getJudultugas().trim().isEmpty() ? 0 : 1;
						}

						for (TugasKelompok tugasPertemuan : tugasesKelompok.values()) {
							tugas_kelompok += tugasPertemuan.getJudultugas().trim().isEmpty() ? 0 : 1;
						}

						tugases = null;
						tugasesKelompok = null;

						pertemuan_file_content += ids.ambilJumlahPertemuanFileContent();
						audio_pertemuan += ids.ambilJumlahAudioPertemuan();
						video_pertemuan += ids.ambilJumlahVideoPertemuan();

					}

					int pert = pertemuans.size();
					d.put("pert", pert);
					d.put("pertemuan_file_content", pertemuan_file_content);
					d.put("tugas_file_content", tugas_file_content);
					d.put("tugas_kelompok", tugas_kelompok);
					d.put("audio_pertemuan", audio_pertemuan);
					d.put("video_pertemuan", video_pertemuan);

					d.put("info", voPembelajaran.infoSimple());
					d.put("info_simple", voPembelajaran.infoSangatSimple());

					int jumlahUjianTotal = jml == null || jml[8] == null ? 0 : Integer.parseInt(jml[8].toString());
					int jumlahDiskusiTotal = jml == null || jml[9] == null ? 0 : Integer.parseInt(jml[9].toString());

					int totalDosen = total == 0 || voPembelajaran == null ? 0
							: (voPembelajaran.getJumlahDosen() * total);
					persen = totalDosen == 0 ? 0 : (totalDosen == absenDosen ? 100 : ((absenDosen * 100) / totalDosen));
					jsonObject.put("totalDosen", totalDosen);
					jsonObject.put("persenDosen", persen);

					jsonObject.put("jumlahUjianTotal", jumlahUjianTotal);
					jsonObject.put("jumlahDiskusiTotal", jumlahDiskusiTotal);

					Session session = HibernateUtil.openSession();
					try {
					if (voPembelajaran instanceof Perkuliahan) {
						Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
						int referensi = ((Number) session.createCriteria(PerkuliahanPunyaItem.class)
								.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount())
								.uniqueResult()).intValue();
						jsonObject.put("referensi", referensi);

						int bukuAjar = ((Number) session.createCriteria(MatakuliahPunyaBukuBahanAjar.class)
								.add(Restrictions.eq("matakuliah", perkuliahan.getMatakuliah()))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						jsonObject.put("bukuAjar", bukuAjar);
					} else {
						jsonObject.put("referensi", 0);
						jsonObject.put("bukuAjar", 0);
					}
					// session.disconnect();
					ApiHelperSupport.closeOpenedSession(session);

					if (voPembelajaran instanceof Perkuliahan) {
						Perkuliahan perkuliahan = (Perkuliahan) voPembelajaran;
						List<Perkuliahan> jadwalParalels = perkuliahan.ambilParalelPerkuliahan();
						int i = 1;
						for (Perkuliahan jadwal : jadwalParalels) {
							d.put("paralel_" + i, jadwal.infoSimple());
							i++;
						}
						jadwalParalels = null;
					}

					data.put(d);
					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				}
				jsonObject.put("data", data);
				voPembelajarans = null;
			}

		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:3536");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({ "unchecked" })
	public static JSONObject kehadiranDosen(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				String kodeProdi = request.isNull("kodeProdi") ? null : request.getString("kodeProdi");
				String sekarang = Common.dateFormat8.get().format(WaktuUtil.getDate());
				Session session = HibernateUtil.openSession();
				try {
				JSONArray jsonArray = new JSONArray();
				Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
				Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();

				Map<Long, Pertemuan> pertemuans = PengumumanAkademisAction.pertemuansHarian.get(sekarang);
				if (pertemuans == null) {
					List<Pertemuan> pertemuansData = session.createCriteria(Pertemuan.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.createAlias("perkuliahan", "perkuliahan").createAlias("perkuliahan.jurusan", "jurusan")
							.add(kodeProdi == null || kodeProdi.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.eq("jurusan.kodeEpsbed", kodeProdi),
											Restrictions.eq("jurusan.kode", kodeProdi)))
							.addOrder(Order.asc("waktuMulai")).add(Restrictions.eq("tanggal", WaktuUtil.getDate()))
							.add(Restrictions.isNotNull("perkuliahan")).list();
					pertemuans = new HashMap<Long, Pertemuan>();
					for (Pertemuan pertemuan : pertemuansData) {
						pertemuans.put(pertemuan.getId(), pertemuan);
					}
					pertemuansData = null;
				}

				List<Pertemuan> hariIni = new ArrayList<Pertemuan>();
				List<Long> perkuliahans = null;
				if (mahasiswa != null) {
					perkuliahans = mahasiswa.ambilPerkuliahanDanParalel();
				} else if (dosen != null) {
					perkuliahans = dosen.ambilPerkuliahan(session);
				}

				for (Pertemuan pertemuan : pertemuans.values()) {
					if (pertemuan.getPerkuliahan() != null && pertemuan.getTanggal() != null
							&& pertemuan.getPerkuliahan().getJumlahDosen() > 0
							&& sekarang.equals(Common.dateFormat8.get().format(pertemuan.getTanggal()))) {
						if (perkuliahans == null || (perkuliahans != null && pertemuan.getPerkuliahan() != null
								&& perkuliahans.contains(pertemuan.getPerkuliahan().getId()))) {
							hariIni.add(pertemuan);
						}
					}
				}

				for (Pertemuan pertemuan : hariIni) {

					List<Dosen> dosens = pertemuan.ambilDosen();
					for (Dosen idDosen : dosens) {
						try {
							JSONObject jsonObjectData = new JSONObject();
							String link = CommonMedia.getUrlFotoPengguna(new Tbmuser(idDosen));

							Statusabsensi statusabsensi = null;
							if (pertemuan.getId() != null) {

								statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
										pertemuan.retreiveAbsensiId(idDosen.getId()));

							}

							if (statusabsensi == null) {
								statusabsensi = ConstantValues.BELUM_ABSEN;
							}

							jsonObjectData.put("foto", link);
							jsonObjectData.put("nama", idDosen.getNama());
							jsonObjectData.put("hp", idDosen.getTelp());
							jsonObjectData.put("email", idDosen.getEmail());
							jsonObjectData.put("mk", pertemuan.getPerkuliahan().getMatakuliah().getNama());
							jsonObjectData.put("jadwal",
									pertemuan.getWaktuMulai() + " sd " + pertemuan.getWaktuSelesai());
							jsonObjectData.put("status", pertemuan.getStatusPertemuan().getNama());
							jsonObjectData.put("kehadiran", statusabsensi.getNama());
							jsonArray.put(jsonObjectData);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ElearningApiUtil.java:3625");
							// TODO: handle exception
						}
					}
				}

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);

				jsonObject.put("data", jsonArray);

				} finally {
					HibernateUtil.closeSessionQuietly(session);
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:3646");
			}
		}
		return jsonObject;
	}

	public static JSONObject simpanAbsenPiket(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {

				Siswa siswa = (Siswa) (request.isNull("siswa") ? null
						: ConstantValues.ambil(Siswa.class.getName(),
								ais.common.CommonJSONUtil.ambilLong(request, "siswa")));
				if (siswa != null) {
					Long absenPiketId = request.isNull("absenPiket") ? -1L
							: ais.common.CommonJSONUtil.ambilLong(request, "absenPiket");
					String ket = request.isNull("keterangan") ? "" : request.getString("keterangan");

					Session session = HibernateUtil.openSession();
					try {
					AbsenPiket absenPiket = (AbsenPiket) session.createCriteria(AbsenPiket.class)
							.add(Restrictions.idEq(absenPiketId)).uniqueResult();

					if (absenPiket != null) {

						AbsenPiketDetail absenPiketDetail = AbsenPiketDetail.ambil(null, siswa, absenPiket,
								absenPiket.getKelas().getAbsensi(), session);

						Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(
								Statusabsensi.class.getName(),
								absenPiketDetail.retreiveAbsensiId(siswa.getId() + "_" + absenPiketId));
						if (statusabsensi == null) {
							statusabsensi = ConstantValues.MASUK;
						}

						Long status = request.isNull("status") ? -1L
								: ais.common.CommonJSONUtil.ambilLong(request, "status");
						Statusabsensi statusabsensiStatus = (Statusabsensi) ConstantValues
								.ambil(Statusabsensi.class.getName(), status);
						if (statusabsensiStatus != null) {
							statusabsensi = statusabsensiStatus;
						}

						absenPiketDetail.populate(siswa.getId() + "_" + absenPiket.getId(), statusabsensi, ket, "", "",
								"AbsenPiket");

						KelasSiswa kelasSiswa = absenPiket.getKelas();
						session.refresh(kelasSiswa);
						kelasSiswa.populate(siswa.getId() + "_" + absenPiket.getId(), statusabsensi, ket, "", "",
								"AbsenPiket");

						session.getTransaction().begin();
						Common.refreshUpdate(session, kelasSiswa);
						Common.refreshUpdate(session, absenPiketDetail);
						session.getTransaction().commit();
						// session.disconnect();
						ApiHelperSupport.closeOpenedSession(session);

						jsonObject.put("status", "00");
						jsonObject.put("description", "Absen piket berhasil");
						jsonObject.put("absen", statusabsensi.getNama());
						jsonObject.put("absen_baru", statusabsensiStatus == null ? "" : statusabsensiStatus.getNama());
						jsonObject.put("data", absenPiketDetail.getAbsensi());
					} else {
						jsonObject.put("status", "90");
						jsonObject.put("description", "Data absen piket tidak ditemukan");
					}

					} finally {
						HibernateUtil.closeSessionQuietly(session);
					}
				} else {
					jsonObject.put("status", "90");
					jsonObject.put("description", "Data siswa tidak ditemukan");
				}
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/ElearningApiUtil.java:3733");
			}
		}
		return jsonObject;
	}

	public static JSONObject nilai_komponen_mahasiswa(HttpServletRequest req, JSONObject json) throws Exception {
		JSONObject result = new JSONObject();
		Session session = null;
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(json, req);
			if (tbmuser == null || tbmuser.getMahasiswa() == null) {
				result.put("status", "97");
				result.put("description", "Akses ditolak");
				return result;
			}
			Long idDp = json.isNull("id") ? null : ais.common.CommonJSONUtil.ambilLong(json, "id");
			if (idDp == null) {
				result.put("status", "91");
				result.put("description", "Parameter tidak lengkap");
				return result;
			}
			session = HibernateUtil.openSession();
			Detailperkuliahan dp = (Detailperkuliahan) session.get(Detailperkuliahan.class, idDp);
			if (dp == null) {
				result.put("status", "99");
				result.put("description", "Data tidak ditemukan");
				return result;
			}
			if (dp.getMahasiswa() == null || dp.getMahasiswa().getId() == null
					|| tbmuser.getMahasiswa().getId() == null
					|| !dp.getMahasiswa().getId().equals(tbmuser.getMahasiswa().getId())) {
				result.put("status", "97");
				result.put("description", "Akses ditolak");
				return result;
			}
			if (dp.getPerkuliahan() == null) {
				result.put("status", "99");
				result.put("description", "Data perkuliahan tidak ditemukan");
				return result;
			}
			boolean sembunyikan = Boolean.TRUE.equals(dp.getPerkuliahan().getSembunyikanNilaiJikaBelumDiverifikasi());
			List<FormatNilai> formatNilais = dp.getPerkuliahan().ambilFormatNilai(session);
			JSONArray komponens = new JSONArray();
			if (formatNilais == null) formatNilais = new java.util.ArrayList<FormatNilai>();
			for (FormatNilai fn : formatNilais) {
				if (fn == null) continue;
				JSONObject komp = new JSONObject();
				Boolean verified = dp.retreiveDetailVerifikasiNilai(fn);
				boolean isVerified = Boolean.TRUE.equals(verified);
				String namaKomp = fn.getNama();
				if (namaKomp == null) namaKomp = "";
				komp.put("nama", namaKomp);
				komp.put("persen", fn.getPersen() != null ? fn.getPersen() : 0);
				if (sembunyikan && !isVerified) {
					komp.put("nilai", JSONObject.NULL);
					komp.put("verified", false);
				} else {
					Double nilai = dp.retreiveDetailNilai(fn);
					komp.put("nilai", nilai != null ? nilai : 0.0);
					komp.put("verified", isVerified);
				}
				if (fn.getNomorUrut() != null) komp.put("urut", fn.getNomorUrut());
				komponens.put(komp);
			}
			result.put("status", "00");
			result.put("data", komponens);
		} catch (Exception e) {
			result.put("status", "90");
			result.put("description", Common.tampilErrorJikaAdmin(e));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return result;
	}
}
