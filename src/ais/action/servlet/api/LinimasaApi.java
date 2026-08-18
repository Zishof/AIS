package ais.action.servlet.api;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.zkoss.zul.Label;
import ais.common.PagingApi;

import ais.action.master.MahasiswaRequestTugasAkhirAction;
import ais.action.master.SkripsiAction;
import ais.action.master.SyaratUjianAction;
import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.helper.AbsensiHelper;
import ais.action.master.sekolah.helper.AbsensiSiswaHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.StatusPertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.file.TugasFileContent;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

public class LinimasaApi {
	// LRU BER-BATAS (bukan HashMap polos): key per token API mobile dan entri tidak
	// pernah dihapus — sebelumnya tumbuh monoton seumur JVM (optimasi RAM Fase 1).
	// Eviksi entri tertua hanya berarti posisi linimasa dihitung ulang saat akses
	// berikutnya. Juga aman dari race (synchronizedMap).
	private static Map<String, TreeMap<String, Long>> mapsLinimasa = java.util.Collections
			.synchronizedMap(new java.util.LinkedHashMap<String, TreeMap<String, Long>>(16, 0.75f, true) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(java.util.Map.Entry<String, TreeMap<String, Long>> eldest) {
					return size() > 1000;
				}
			});
	// LRU BER-BATAS (bukan HashMap polos): state paging per token API mobile, di-put di
	// 7 titik tanpa pernah dihapus — sebelumnya tumbuh monoton seumur JVM (optimasi RAM
	// Fase 1). Eviksi entri tertua hanya berarti paging dimulai ulang dari awal.
	private static Map<String, PagingApi> mapsPaging = java.util.Collections
			.synchronizedMap(new java.util.LinkedHashMap<String, PagingApi>(16, 0.75f, true) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(java.util.Map.Entry<String, PagingApi> eldest) {
					return size() > 1000;
				}
			});

	/**
	 * Baca attribute PagingApi sebagai int. Di jalur API, objek {@link PagingApi} hanya dipakai sebagai WADAH
	 * nilai paging (di-cache per token) — BUKAN komponen UI — sehingga nilainya disimpan/dibaca lewat
	 * attribute (HashMap biasa). Memakai setter ZK (setPageSize/setActivePage/setTotalSize) di sini akan
	 * memicu {@code Events.postEvent} yang butuh ZK Desktop/Execution; di servlet API Execution selalu
	 * null sehingga melempar NullPointerException.
	 */
	private static int ambilIntAttr(PagingApi paging, String nama, int bawaan) {
		if (paging == null) {
			return bawaan;
		}
		Object nilai = paging.getAttribute(nama);
		if (nilai instanceof Number) {
			return ((Number) nilai).intValue();
		}
		return bawaan;
	}


	/// Rekap kehadiran seorang mahasiswa lintas seluruh perkuliahan yang
	/// KRS-nya disetujui, untuk donat "Rekap Kehadiran" dasbor studi.
	///
	/// Absensi tersimpan sebagai string terserialisasi per pertemuan
	/// ("mhsId,statusId,...;..."), jadi agregasi dilakukan dengan satu
	/// query kolom `absensi` lalu diurai di memori — bukan N query per
	/// pertemuan. Status di luar Hadir/Izin/Sakit/Alpa dilaporkan apa
	/// adanya di ember `lainnya`; tidak ada angka yang dikarang.
	public static JSONObject rekapKehadiranMahasiswa(JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
				return jsonObject;
			}
			Mahasiswa mahasiswa = tbmuser.getMahasiswa();
			if (mahasiswa == null || mahasiswa.getId() == null) {
				jsonObject.put("status", "96");
				jsonObject.put("description", "Rekap kehadiran hanya untuk akun mahasiswa");
				return jsonObject;
			}

			long hadir = 0, izin = 0, sakit = 0, alpa = 0, belum = 0, lainnya = 0;
			int totalPertemuan = 0;
			Long mhsId = mahasiswa.getId();
			Long idHadir = ConstantValues.MASUK == null ? null : ConstantValues.MASUK.getId();
			Long idIzin = ConstantValues.IZIN == null ? null : ConstantValues.IZIN.getId();
			Long idSakit = ConstantValues.SAKIT == null ? null : ConstantValues.SAKIT.getId();
			Long idAlpa = ConstantValues.TIDAK_ADA_ALASAN == null ? null : ConstantValues.TIDAK_ADA_ALASAN.getId();
			Long idBelum = ConstantValues.BELUM_ABSEN == null ? null : ConstantValues.BELUM_ABSEN.getId();

			Session session = HibernateUtil.openSession();
			try {
				org.hibernate.criterion.DetachedCriteria perkuliahanMahasiswa =
						org.hibernate.criterion.DetachedCriteria.forClass(Detailperkuliahan.class)
								.add(Restrictions.eq("mahasiswa", mahasiswa))
								.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
								.setProjection(Projections.property("perkuliahan"));
				@SuppressWarnings("unchecked")
				List<Object> absensis = session.createCriteria(Pertemuan.class)
						.add(org.hibernate.criterion.Subqueries.propertyIn("perkuliahan", perkuliahanMahasiswa))
						.setProjection(Projections.property("absensi"))
						.list();

				for (Object raw : absensis) {
					totalPertemuan++;
					if (raw == null) {
						belum++;
						continue;
					}
					Long statusId = null;
					for (String entri : raw.toString().split(";")) {
						try {
							String[] kolom = entri.split(",");
							if (kolom.length < 2 || kolom[0].trim().isEmpty()) {
								continue;
							}
							if (mhsId.equals(Long.parseLong(kolom[0].trim()))) {
								statusId = Long.parseLong(kolom[1].trim());
								break;
							}
						} catch (Exception abaikan) {
							// Entri korup pada string absensi dilewati.
						}
					}
					if (statusId == null || statusId.longValue() < 0
							|| (idBelum != null && idBelum.equals(statusId))) {
						belum++;
					} else if (idHadir != null && idHadir.equals(statusId)) {
						hadir++;
					} else if (idIzin != null && idIzin.equals(statusId)) {
						izin++;
					} else if (idSakit != null && idSakit.equals(statusId)) {
						sakit++;
					} else if (idAlpa != null && idAlpa.equals(statusId)) {
						alpa++;
					} else {
						lainnya++;
					}
				}
			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}

			JSONObject data = new JSONObject();
			data.put("hadir", hadir);
			data.put("izin", izin);
			data.put("sakit", sakit);
			data.put("alpa", alpa);
			data.put("belum", belum);
			data.put("lainnya", lainnya);
			data.put("total_pertemuan", totalPertemuan);
			jsonObject.put("status", "00");
			jsonObject.put("data", data);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				jsonObject.put("status", "99");
				jsonObject.put("description", "Rekap kehadiran gagal dihitung");
			} catch (Exception abaikan) {
				// Tidak ada lagi yang bisa dilaporkan.
			}
		}
		return jsonObject;
	}

	public static JSONObject masukkanInfoKePertemuan(JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, request.getString("id"),
						true);
				pertemuan.masukkanData(request.getString("info"), tbmuser);
				jsonObject = displayPertemuan(pertemuan, tbmuser, servletRequest);
				jsonObject.put("status", "00");
				jsonObject.put("description", "Pengambilan data berhasil");
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:95");
			}
		}
		return jsonObject;
	}

	public static JSONObject refreshPertemuan(JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, request.getString("id"),
						true);
				jsonObject = displayPertemuan(pertemuan, tbmuser, servletRequest);
				jsonObject.put("status", "00");
				jsonObject.put("description", "Pengambilan data berhasil");
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:121");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	private static void diplayUjian(PertemuanPunyaUjian pertemuanPunyaUjian, JSONObject jsonObjectUjian,
			Pertemuan pertemuan, Tbmuser tbmuser) throws Exception {

		jsonObjectUjian.put("id", pertemuanPunyaUjian.getId());
		jsonObjectUjian.put("class", PertemuanPunyaUjian.class.getName());
		jsonObjectUjian.put("judul", pertemuanPunyaUjian.getNama());

		String waktu = "";

		waktu = (pertemuanPunyaUjian.getMulaiUjian() == null ? "(tidak ada waktu mulai)"
				: (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getMulaiUjian(), null)
						+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getMulaiUjian())))
				+ " sd "
				+ (pertemuanPunyaUjian.getSampaiUjian() == null ? "(tidak ada waktu selesai)"
						: (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getSampaiUjian(), null)
								+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getSampaiUjian())));

		jsonObjectUjian.put("waktu", waktu);
		jsonObjectUjian.put("mulai",
				(pertemuanPunyaUjian.getMulaiUjian() == null ? "(tidak ada waktu mulai)"
						: (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getMulaiUjian(), null)
								+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getMulaiUjian()))));
		jsonObjectUjian.put("sampai",
				(pertemuanPunyaUjian.getSampaiUjian() == null ? "(tidak ada waktu selesai)"
						: (SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getSampaiUjian(), null)
								+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getSampaiUjian()))));

		try {

			int jumlahPeserta = 0;

			Session session = HibernateUtil.openSession();
			try {

			List<HasilUjianMahasiswa> mahasiswasTemorary = session.createCriteria(HasilUjianMahasiswa.class)
					.add(Restrictions.isNotNull("keyhasil"))
					.add(Restrictions.eq("pertemuanPunyaUjian", pertemuanPunyaUjian)).list();

			jumlahPeserta = mahasiswasTemorary.size();

			jsonObjectUjian.put("jumlahPeserta", jumlahPeserta);
			JSONArray pesertaIkutUjian = new JSONArray();
			for (HasilUjianMahasiswa hasilUjianMahasiswa : mahasiswasTemorary) {

				Mahasiswa mahasiswa = hasilUjianMahasiswa.getMahasiswa();
				BiodataCalonMahasiswa biodataCalonMahasiswa = hasilUjianMahasiswa.getBiodataCalonMahasiswa();
				Siswa siswa = hasilUjianMahasiswa.getSiswa();
//				CalonSiswa calonSiswa = hasilUjianMahasiswa.getCalonSiswa();

				JSONObject jsonObject = new JSONObject();
				if (mahasiswa != null) {
					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa));
					jsonObject.put("nim", mahasiswa.getNim());
					jsonObject.put("nama", mahasiswa.getNama());
					jsonObject.put("foto", url);
					jsonObject.put("telp", mahasiswa.getTelp());
					jsonObject.put("email", mahasiswa.getEmail());
				} else if (siswa != null) {
					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa));

					jsonObject.put("nim", siswa.getNomorInduk());
					jsonObject.put("nama", siswa.getNama());
					jsonObject.put("foto", url);
					jsonObject.put("telp", siswa.getTeleponSiswa());
					jsonObject.put("email", siswa.getAlamatEmail());
				} else if (biodataCalonMahasiswa != null) {

					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(biodataCalonMahasiswa));

					jsonObject.put("nim", biodataCalonMahasiswa.getNoRegistrasi());
					jsonObject.put("nama", biodataCalonMahasiswa.getNama());
					jsonObject.put("foto", url);
					jsonObject.put("telp", biodataCalonMahasiswa.getHp());
					jsonObject.put("email", biodataCalonMahasiswa.getEmail());
				}
				jsonObject.put("jawabanBenar", hasilUjianMahasiswa.getJawabanBenar());
				jsonObject.put("keterangan", hasilUjianMahasiswa.getKeterangan());

				jsonObject.put("totalNilai", hasilUjianMahasiswa.getTotalNilai());
				jsonObject.put("lamaPengerjaan", hasilUjianMahasiswa.getLamaPengerjaan() == null ? ""
						: Common.dateFormat1.get().format(hasilUjianMahasiswa.getLamaPengerjaan()));
				jsonObject.put("sisaWaktuPengerjaan", hasilUjianMahasiswa.getSisaWaktuPengerjaan() == null ? ""
						: Common.dateFormat1.get().format(hasilUjianMahasiswa.getSisaWaktuPengerjaan()));
				jsonObject.put("selesaiPada", hasilUjianMahasiswa.getSelesaiPada() == null ? ""
						: Common.dateFormat3.get().format(hasilUjianMahasiswa.getSelesaiPada()));
				jsonObject.put("mulaiPada", hasilUjianMahasiswa.getMulaiPada() == null ? ""
						: Common.dateFormat3.get().format(hasilUjianMahasiswa.getMulaiPada()));
				jsonObject.put("telahIkutUjian", hasilUjianMahasiswa.getTelahIkutUjian());
				jsonObject.put("lengkapiJawaban", hasilUjianMahasiswa.getLengkapiJawaban());
				jsonObject.put("jumlahSoal", hasilUjianMahasiswa.getJumlahSoal());
				jsonObject.put("jawabanBenar", hasilUjianMahasiswa.getJawabanBenar());
				jsonObject.put("jawabanBenarMax", hasilUjianMahasiswa.getJawabanBenarMax());
				jsonObject.put("nilai", hasilUjianMahasiswa.getNilai());
				jsonObject.put("jumlahIkut", hasilUjianMahasiswa.getJumlahIkut());

				pesertaIkutUjian.put(jsonObject);
			}
			jsonObjectUjian.put("pesertaIkutUjian", pesertaIkutUjian);

			mahasiswasTemorary = null;
			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:232");
		}

		Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
		String isi = "";
		String isiBiasa = "";
		Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();

		List<String> warnings = new ArrayList<String>();
		if (pertemuanPunyaUjian != null && perkuliahan != null && tbmuser.getMahasiswa() != null
				&& pertemuanPunyaUjian.getUjian().getSyaratUjian() != null) {
			Detailperkuliahan detailperkuliahan = tbmuser.getMahasiswa().ambilDetailperkuliahan(perkuliahan);

			if (detailperkuliahan == null) {
				Session sessReInit1 = HibernateUtil.openSession();
				try {
					tbmuser.getMahasiswa().reInitDetailperkuliahan(sessReInit1);
				} finally {
					HibernateUtil.closeSessionQuietly(sessReInit1);
				}
				detailperkuliahan = tbmuser.getMahasiswa().ambilDetailperkuliahan(perkuliahan);
			}

			if (detailperkuliahan != null) {
				SyaratUjianAction.checkSyaratSyaratUjian(pertemuanPunyaUjian.getUjian().getSyaratUjian(),
						pertemuan.ambilVOPembelajaran(), tbmuser.getMahasiswa(), detailperkuliahan.getSemester(),
						pertemuanPunyaUjian.getNama(), warnings);
			}
		}

		if (!warnings.isEmpty()) {

			isi = ("<strong><font style='color:red;font-size: 15px;'>" + warnings.get(0)
					+ "</font></strong><br><img style='width:90%' src=\"" + Common.getRequestHostWithProtocol()
					+ "/img/oh.gif\" alt=\"WebP rules.\" />");

			isiBiasa = warnings.get(0);
		}

		else if (!(pertemuanPunyaUjian.getMulaiUjian() == null
				|| pertemuanPunyaUjian.getMulaiUjian().before(kemarin.getTime()))) {
			isi = ("<strong><font style='color:red;font-size: 15px;'>Untuk ujian \""
					+ (pertemuanPunyaUjian.getUjian().getNama() == null
							|| pertemuanPunyaUjian.getUjian().getNama().trim().isEmpty() ? pertemuan.getTopik()
									: pertemuanPunyaUjian.getUjian().getNama())
					+ "\", belum mulai..<br>Ujian akan ditampilkan setelah "
					+ SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getMulaiUjian(), null)
					+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getMulaiUjian()) + "</font></strong><br><img src=\""
					+ Common.getRequestHostWithProtocol()
					+ "/img/Apps-preferences-system-time-icon.png\" alt=\"WebP rules.\" />");

			isiBiasa = "Untuk ujian \""
					+ (pertemuanPunyaUjian.getUjian().getNama() == null
							|| pertemuanPunyaUjian.getUjian().getNama().trim().isEmpty() ? pertemuan.getTopik()
									: pertemuanPunyaUjian.getUjian().getNama())
					+ "\", belum mulai.. Ujian akan ditampilkan setelah "
					+ SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getMulaiUjian(), null)
					+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getMulaiUjian());

		} else if (!(pertemuanPunyaUjian.getSampaiUjian() == null
				|| pertemuanPunyaUjian.getSampaiUjian().after(kemarin.getTime()))) {
			isi = ("<strong><font style='color:red;font-size: 15px;'>Untuk ujian \""
					+ (pertemuanPunyaUjian.getUjian().getNama() == null
							|| pertemuanPunyaUjian.getUjian().getNama().trim().isEmpty() ? pertemuan.getTopik()
									: pertemuanPunyaUjian.getUjian().getNama())
					+ "\", telah selesai..<br>Ujian telah ditampilkan sebelum "
					+ SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getSampaiUjian(), null)
					+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getSampaiUjian())
					+ "</font></strong><br><img src=\"" + Common.getRequestHostWithProtocol()
					+ "/img/Apps-preferences-system-time-icon.png\" alt=\"WebP rules.\" />");
			isiBiasa = "Untuk ujian \""
					+ (pertemuanPunyaUjian.getUjian().getNama() == null
							|| pertemuanPunyaUjian.getUjian().getNama().trim().isEmpty() ? pertemuan.getTopik()
									: pertemuanPunyaUjian.getUjian().getNama())
					+ "\", telah selesai..<br>Ujian telah ditampilkan sebelum "
					+ SmartDateTimeUtil.getDayString(pertemuanPunyaUjian.getSampaiUjian(), null)
					+ Common.dateFormat5.get().format(pertemuanPunyaUjian.getSampaiUjian());
		} else {
			isi = (pertemuanPunyaUjian.getUjian().getNama() == null
					|| pertemuanPunyaUjian.getUjian().getNama().trim().isEmpty() ? pertemuan.getTopik()
							: pertemuanPunyaUjian.getUjian().getNama());
			isiBiasa = (pertemuanPunyaUjian.getUjian().getNama() == null
					|| pertemuanPunyaUjian.getUjian().getNama().trim().isEmpty() ? pertemuan.getTopik()
							: pertemuanPunyaUjian.getUjian().getNama());
		}

		jsonObjectUjian.put("isiUjianRinci", isi);

		jsonObjectUjian.put("isiUjian", isiBiasa);
	}

	private static void diplayTugas(Tugas tugas, JSONObject jsonObjectTugas, Pertemuan pertemuan, Tbmuser tbmuser)
			throws Exception {
		jsonObjectTugas.put("pert_id", pertemuan.getId());
		jsonObjectTugas.put("id", tugas.getId());
		jsonObjectTugas.put("class", tugas.getClass().getName());
		Number tugass = pertemuan.ambilJumlahTugasFileContent();
		jsonObjectTugas.put("uploadTugas", tugass);
		jsonObjectTugas.put("judulTugas", tugas.getJudultugas().trim());

		Mahasiswa currentMhs = tbmuser == null ? null : tbmuser.getMahasiswa();
		Siswa currentSiswa = tbmuser == null ? null : tbmuser.getSiswa();

		BiodataCalonMahasiswa currentBiodataCalonMahasiswa = tbmuser == null ? null
				: tbmuser.getBiodataCalonMahasiswa();
//		CalonSiswa currentCalonSiswa = tbmuser == null ? null : tbmuser.getCalonSiswa();

		try {
			tugas.currentTugasFileContent = null;
			tugas.currentUser = null;
			TreeMap<Long, TugasFileContent> treemapData = new TreeMap<Long, TugasFileContent>();
			TreeMap<Long, TugasFileContent> tugasFileContentsa = tugas.ambilTugasFileContentTotal(treemapData, "", null,
					500);
			List<TugasFileContent> pertemuanFileContent = new ArrayList<TugasFileContent>(tugasFileContentsa.values());
			Collections.sort(pertemuanFileContent);
			if (tugas.currentTugasFileContent == null && tbmuser != null && tbmuser.getSiswa() != null) {
				for (TugasFileContent tugasFileContent : pertemuanFileContent) {
					if (tugasFileContent.getSiswa() != null
							&& tbmuser.getSiswa().getId().equals(tugasFileContent.getSiswa())) {
						tugas.currentTugasFileContent = tugasFileContent;
						break;
					}
				}
			} else if (tugas.currentTugasFileContent == null && tbmuser != null && tbmuser.getMahasiswa() != null) {
				for (TugasFileContent tugasFileContent : pertemuanFileContent) {
					if (tugasFileContent.getMahasiswa() != null
							&& tbmuser.getMahasiswa().getId().equals(tugasFileContent.getMahasiswa())) {
						tugas.currentTugasFileContent = tugasFileContent;
						break;
					}
				}
			} else if (tugas.currentTugasFileContent == null && tbmuser != null
					&& tbmuser.getBiodataCalonMahasiswa() != null) {
				for (TugasFileContent tugasFileContent : pertemuanFileContent) {
					if (tugasFileContent.getBiodataCalonMahasiswa() != null && tbmuser.getBiodataCalonMahasiswa()
							.getId().equals(tugasFileContent.getBiodataCalonMahasiswa())) {
						tugas.currentTugasFileContent = tugasFileContent;
						break;
					}
				}
			}

			boolean peserta = tbmuser != null
					&& (tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null
							|| tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null);

			Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();
			boolean upload = false;
			upload = (!tugas.getJudultugas().isEmpty()
					&& ((tugas.getSelesai() == null || tugas.getSelesai().after(kemarin.getTime()))
							&& (tugas.getMulai() == null || tugas.getMulai().before(kemarin.getTime()))));
			if (!tugas.getJudultugas().isEmpty() && currentMhs != null
					&& tugas.getMhsBolehUploadUlang().contains("," + currentMhs.getId() + ",")) {
				upload = true;
			} else if (!tugas.getJudultugas().isEmpty() && currentBiodataCalonMahasiswa != null
					&& tugas.getMhsBolehUploadUlang().contains("," + currentBiodataCalonMahasiswa.getId() + ",")) {
				upload = true;
			} else if (!tugas.getJudultugas().isEmpty() && currentSiswa != null
					&& tugas.getMhsBolehUploadUlang().contains("," + currentSiswa.getId() + ",")) {
				upload = true;
			}

			JSONArray pesertaUpload = new JSONArray();
			for (TugasFileContent tgs : pertemuanFileContent) {
				Mahasiswa mahasiswa = (Mahasiswa) (tgs.getMahasiswa() == null ? null
						: ConstantValues.ambil(Mahasiswa.class.getName(), tgs.getMahasiswa()));
				Siswa siswa = (Siswa) (tgs.getSiswa() == null ? null
						: ConstantValues.ambil(Siswa.class.getName(), tgs.getSiswa()));
				BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) (tgs
						.getBiodataCalonMahasiswa() == null ? null
								: ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(),
										tgs.getBiodataCalonMahasiswa()));

				boolean readonly = currentMhs != null || currentSiswa != null || currentBiodataCalonMahasiswa != null
						? true
						: false;

				if (currentMhs != null && mahasiswa != null && currentMhs.getId().equals(mahasiswa.getId())) {
					readonly = false;
				}
				if (currentSiswa != null && siswa != null && currentSiswa.getId().equals(siswa.getId())) {
					readonly = false;
				}
				if (currentBiodataCalonMahasiswa != null && biodataCalonMahasiswa != null
						&& currentBiodataCalonMahasiswa.getId().equals(biodataCalonMahasiswa.getId())) {
					readonly = false;
				}

				String namaFile = tgs.getNama();
				JSONObject jsonObject = new JSONObject();
				if (mahasiswa != null) {
					tgs.ubahRealNameSesuaiDenganNIM(mahasiswa);
					namaFile = tgs.ambilRealNameSesuaiDenganNIM(mahasiswa);
					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa));
					jsonObject.put("id", tgs.getId());
					jsonObject.put("nim", mahasiswa.getNim());
					jsonObject.put("nama", mahasiswa.getNama());
					jsonObject.put("foto", url);
					jsonObject.put("telp", mahasiswa.getTelp());
					jsonObject.put("email", mahasiswa.getEmail());

				} else if (siswa != null) {
					tgs.ubahRealNameSesuaiDenganNIM(siswa);
					namaFile = tgs.ambilRealNameSesuaiDenganNIM(siswa);
					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa));
					jsonObject.put("id", tgs.getId());
					jsonObject.put("nim", siswa.getNomorInduk());
					jsonObject.put("nama", siswa.getNama());
					jsonObject.put("foto", url);
					jsonObject.put("telp", siswa.getTeleponSiswa());
					jsonObject.put("email", siswa.getAlamatEmail());

				} else if (biodataCalonMahasiswa != null) {
					tgs.ubahRealNameSesuaiDenganNIM(biodataCalonMahasiswa);
					namaFile = tgs.ambilRealNameSesuaiDenganNIM(biodataCalonMahasiswa);

					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(biodataCalonMahasiswa));
					jsonObject.put("id", tgs.getId());
					jsonObject.put("nim", biodataCalonMahasiswa.getNoRegistrasi());
					jsonObject.put("nama", biodataCalonMahasiswa.getNama());
					jsonObject.put("foto", url);
					jsonObject.put("telp", biodataCalonMahasiswa.getHp());
					jsonObject.put("email", biodataCalonMahasiswa.getEmail());

				}
				jsonObject.put("boleh_upload", !readonly && upload);
				jsonObject.put("ubah_nilai", !peserta);

				// §13.7: epoch mentah agar klien dapat menandai keterlambatan
				// tanpa mengurai string tanggal berformat lokal.
				if (tgs.getUploadDate() != null) {
					jsonObject.put("tgl_upload_epoch", tgs.getUploadDate().getTime());
				}
				jsonObject.put("tgl_upload",
						tgs.getUploadDate() == null ? ""
								: SmartDateTimeUtil.getDayString(tgs.getUploadDate(), null)
										+ Common.dateFormat.get().format(tgs.getUploadDate()));

				jsonObject.put("keterangan", tgs.getKeterangan());
				if (!readonly) {
					jsonObject.put("nilai", tgs.getNilai());
				}
				jsonObject.put("class", tgs.getClass().getName());
				jsonObject.put("namaFile", namaFile);
				if (!readonly) {
					jsonObject.put("url_tugas", tgs.createLinkUri());
				}
				pesertaUpload.put(jsonObject);
			}
			jsonObjectTugas.put("pesertaUpload", pesertaUpload);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:478");
		}

		String waktu = "";

		waktu = (tugas.getMulai() == null ? "(tidak ada waktu mulai)"
				: (SmartDateTimeUtil.getDayString(tugas.getMulai(), null)
						+ Common.dateFormat5.get().format(tugas.getMulai())))
				+ " sd "
				+ (tugas.getSelesai() == null ? "(tidak ada waktu selesai)"
						: (SmartDateTimeUtil.getDayString(tugas.getSelesai(), null)
								+ Common.dateFormat5.get().format(tugas.getSelesai())));

		jsonObjectTugas.put("waktu", waktu);
		jsonObjectTugas.put("mulai",
				(tugas.getMulai() == null ? "(tidak ada waktu mulai)"
						: (SmartDateTimeUtil.getDayString(tugas.getMulai(), null)
								+ Common.dateFormat5.get().format(tugas.getMulai()))));
		if (tugas.getSelesai() != null) {
			jsonObjectTugas.put("selesai_epoch", tugas.getSelesai().getTime());
		}
		jsonObjectTugas.put("sampai",
				(tugas.getSelesai() == null ? "(tidak ada waktu selesai)"
						: (SmartDateTimeUtil.getDayString(tugas.getSelesai(), null)
								+ Common.dateFormat5.get().format(tugas.getSelesai()))));

		LampiranLain fileFotoLain = LampiranLain.ambil(tugas.getId(), LampiranLain.TUGAS_MANDIRI_PERKULIAHAN);
		if (fileFotoLain != null) {
			jsonObjectTugas.put("lampiranTugas", fileFotoLain.createLinkUri());
		}

		Perkuliahan perkuliahan = pertemuan.getPerkuliahan();
		String isi = "";
		String isiBiasa = "";
		Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();
		boolean tampil = true;

		List<String> warnings = new ArrayList<String>();
		if (tugas != null && perkuliahan != null && tbmuser.getMahasiswa() != null
				&& tugas.getSyaratMengumpulkanTugas() != null) {
			Detailperkuliahan detailperkuliahan = tbmuser.getMahasiswa().ambilDetailperkuliahan(perkuliahan);

			if (detailperkuliahan == null) {
				Session sessReInit2 = HibernateUtil.openSession();
				try {
					tbmuser.getMahasiswa().reInitDetailperkuliahan(sessReInit2);
				} finally {
					HibernateUtil.closeSessionQuietly(sessReInit2);
				}
				detailperkuliahan = tbmuser.getMahasiswa().ambilDetailperkuliahan(perkuliahan);
			}

			if (detailperkuliahan != null) {
				SyaratUjianAction.checkSyaratSyaratUjian(tugas.getSyaratMengumpulkanTugas(),
						pertemuan.ambilVOPembelajaran(), tbmuser.getMahasiswa(), detailperkuliahan.getSemester(),
						tugas.getJudultugas(), warnings);
			}
		}

		if (!warnings.isEmpty()) {
			tampil = false;
			isi = ("<strong><font style='color:red;font-size: 15px;'>" + warnings.get(0)
					+ "</font></strong><br><img style='width:90%' src=\"" + Common.getRequestHostWithProtocol()
					+ "/img/oh.gif\" alt=\"WebP rules.\" />");

			isiBiasa = warnings.get(0);
		}

		else if (!(tugas.getMulai() == null || tugas.getMulai().before(kemarin.getTime()))) {
			tampil = false;
			isi = ("<strong><font style='color:red;font-size: 15px;'>Untuk tugas \""
					+ (tugas.getJudultugas() == null || tugas.getJudultugas().trim().isEmpty() ? pertemuan.getTopik()
							: tugas.getJudultugas())
					+ "\", belum mulai..<br>Tugas akan ditampilkan setelah "
					+ SmartDateTimeUtil.getDayString(tugas.getMulai(), null)
					+ Common.dateFormat5.get().format(tugas.getMulai()) + "</font></strong><br><img src=\""
					+ Common.getRequestHostWithProtocol()
					+ "/img/Apps-preferences-system-time-icon.png\" alt=\"WebP rules.\" />");

			isiBiasa = "Untuk tugas \""
					+ (tugas.getJudultugas() == null || tugas.getJudultugas().trim().isEmpty() ? pertemuan.getTopik()
							: tugas.getJudultugas())
					+ "\", belum mulai.. Tugas akan ditampilkan setelah "
					+ SmartDateTimeUtil.getDayString(tugas.getMulai(), null)
					+ Common.dateFormat5.get().format(tugas.getMulai());

		} else if (!(tugas.getSelesai() == null || tugas.getSelesai().after(kemarin.getTime()))) {
			tampil = false;
			isi = ("<strong><font style='color:red;font-size: 15px;'>Untuk tugas \""
					+ (tugas.getJudultugas() == null || tugas.getJudultugas().trim().isEmpty() ? pertemuan.getTopik()
							: tugas.getJudultugas())
					+ "\", telah selesai..<br>Tugas telah ditampilkan sebelum "
					+ SmartDateTimeUtil.getDayString(tugas.getSelesai(), null)
					+ Common.dateFormat5.get().format(tugas.getSelesai()) + "</font></strong><br><img src=\""
					+ Common.getRequestHostWithProtocol()
					+ "/img/Apps-preferences-system-time-icon.png\" alt=\"WebP rules.\" />");
			isiBiasa = "Untuk tugas \""
					+ (tugas.getJudultugas() == null || tugas.getJudultugas().trim().isEmpty() ? pertemuan.getTopik()
							: tugas.getJudultugas())
					+ "\", telah selesai.. Tugas telah ditampilkan sebelum "
					+ SmartDateTimeUtil.getDayString(tugas.getSelesai(), null)
					+ Common.dateFormat5.get().format(tugas.getSelesai());
		} else {
			isi = pertemuan.getIsitugas().trim();
			isiBiasa = pertemuan.getIsitugas().trim();
			isiBiasa = Jsoup.parse(isiBiasa).text();

		}

		jsonObjectTugas.put("isiTugasRinci", isi);
		jsonObjectTugas.put("boleh_upload", tampil);
		jsonObjectTugas.put("isiTugas", isiBiasa);
	}

	public static JSONObject displayPertemuan(Pertemuan pertemuan, Tbmuser tbmuser, HttpServletRequest servletRequest) {
		return displayPertemuan(pertemuan, tbmuser, servletRequest, true, true, true);
	}

	public static JSONObject displayPertemuan(Pertemuan pertemuan, Tbmuser tbmuser, HttpServletRequest servletRequest,
			boolean tampilUjian, boolean tampilTugas, boolean tampilMateri) {
		return displayPertemuan(pertemuan, tbmuser, servletRequest, tampilUjian, tampilTugas, tampilMateri, true);
	}

	@SuppressWarnings("rawtypes")
	public static JSONObject displayPertemuan(Pertemuan pertemuan, Tbmuser tbmuser, HttpServletRequest servletRequest,
			boolean tampilUjian, boolean tampilTugas, boolean tampilMateri, boolean tampilAbsen) {
		JSONObject da = new JSONObject();

		try {
			da.put("id", pertemuan.getId());
			da.put("class", Pertemuan.class.getName());
			da.put("ke", pertemuan.getPertemuanKe());
			da.put("info", pertemuan.info());
			da.put("warna", pertemuan.warna().split(",")[0]);
			da.put("topik", pertemuan.getTopik());

			VOPembelajaran voPembelajaran = pertemuan.ambilVOPembelajaran();
			if (voPembelajaran != null) {
				da.put("id_vo_pembelajaran", voPembelajaran.getId());
				da.put("class_vo_pembelajaran", voPembelajaran.getClass().getName());
			}

			JSONArray absensi = new JSONArray();
			if (tampilAbsen) {

				List hasilUjianMahasiswas;

				if (pertemuan.getJadwalPelajaran() != null) {
					hasilUjianMahasiswas = AbsensiSiswaHelper.populateSiswaDariPertemuan(pertemuan);
				} else if (pertemuan.getJadwalUjianPMB() != null) {
					hasilUjianMahasiswas = AbsensiHelper.populateMahasiswaDariPertemuan(pertemuan);
				} else {
					hasilUjianMahasiswas = pertemuan.ambilMahasiswa();
				}
				for (Object valueObject : hasilUjianMahasiswas) {
					JSONObject jsonObject = new JSONObject();
					if (valueObject instanceof Siswa) {

						Siswa siswa = (Siswa) valueObject;

						jsonObject.put("nim", siswa.getNim());
						jsonObject.put("nama", siswa.getNama());
						jsonObject.put("jurusan", siswa.getSekolah().getNama());
						jsonObject.put("angkatan", siswa.getTahunMasuk());

						Statusabsensi statusabsensi = (Statusabsensi) ConstantValues
								.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(siswa.getId()));
						if (statusabsensi == null) {
							statusabsensi = ConstantValues.BELUM_ABSEN;
						}

						jsonObject.put("kode_status", statusabsensi.getKode());
						jsonObject.put("nama_status", statusabsensi.getNama());

						String wkt = pertemuan.retreiveAbsensiMulai(siswa.getId()) + " s.d "
								+ pertemuan.retreiveAbsensiSampai(siswa.getId());

						jsonObject.put("wkt_status", wkt);

						String ket = pertemuan.retreiveAbsensiKeterangan(siswa.getId());
						ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
						jsonObject.put("ket_status", ket);

						List<String> urls = Common.getUrls(ket);
						for (String u : urls) {
							if (u.contains("maps")) {
								jsonObject.put("maps", u);
							} else if (ket.toLowerCase().contains("video") && u.contains("download")) {
								jsonObject.put("video", u);
							} else if (u.contains("download")) {
								jsonObject.put("download", u);
							}
						}

					}

					else if (valueObject instanceof Mahasiswa) {
						Mahasiswa mahasiswa = (Mahasiswa) valueObject;

						jsonObject.put("nim", mahasiswa.getNim());
						jsonObject.put("nama", mahasiswa.getNama());
						jsonObject.put("jurusan", mahasiswa.getJurusan() != null ? mahasiswa.getJurusan().getNama() : "");
						jsonObject.put("angkatan", mahasiswa.getTahunangkatan());

						Statusabsensi statusabsensi = (Statusabsensi) ConstantValues
								.ambil(Statusabsensi.class.getName(), pertemuan.retreiveAbsensiId(mahasiswa.getId()));
						if (statusabsensi == null) {
							statusabsensi = ConstantValues.BELUM_ABSEN;
						}

						jsonObject.put("kode_status", statusabsensi.getKode());
						jsonObject.put("nama_status", statusabsensi.getNama());

						String wkt = pertemuan.retreiveAbsensiMulai(mahasiswa.getId()) + " s.d "
								+ pertemuan.retreiveAbsensiSampai(mahasiswa.getId());

						jsonObject.put("wkt_status", wkt);

						String ket = pertemuan.retreiveAbsensiKeterangan(mahasiswa.getId());
						ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
						jsonObject.put("ket_status", ket);

						List<String> urls = Common.getUrls(ket);
						for (String u : urls) {
							if (u.contains("maps")) {
								jsonObject.put("maps", u);
							} else if (ket.toLowerCase().contains("video") && u.contains("download")) {
								jsonObject.put("video", u);
							} else if (u.contains("download")) {
								jsonObject.put("download", u);
							}
						}

					} else if (valueObject instanceof BiodataCalonMahasiswa) {
						BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) valueObject;

						jsonObject.put("nim", biodataCalonMahasiswa.getNoRegistrasi());
						jsonObject.put("nama", biodataCalonMahasiswa.getNama());
						jsonObject.put("jurusan",
								biodataCalonMahasiswa.getProdiLulus() != null
										? biodataCalonMahasiswa.getProdiLulus().getNama()
										: (biodataCalonMahasiswa.getProdi1() != null
												? biodataCalonMahasiswa.getProdi1().getNama()
												: ""));
						jsonObject.put("angkatan", biodataCalonMahasiswa.getTahun());

						Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(
								Statusabsensi.class.getName(),
								pertemuan.retreiveAbsensiId(biodataCalonMahasiswa.getId()));
						if (statusabsensi == null) {
							statusabsensi = ConstantValues.BELUM_ABSEN;
						}
						jsonObject.put("kode_status", statusabsensi.getKode());
						jsonObject.put("nama_status", statusabsensi.getNama());

						String wkt = pertemuan.retreiveAbsensiMulai(biodataCalonMahasiswa.getId()) + " s.d "
								+ pertemuan.retreiveAbsensiSampai(biodataCalonMahasiswa.getId());

						jsonObject.put("wkt_status", wkt);

						String ket = pertemuan.retreiveAbsensiKeterangan(biodataCalonMahasiswa.getId());
						ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
						jsonObject.put("ket_status", ket);

						List<String> urls = Common.getUrls(ket);
						for (String u : urls) {
							if (u.contains("maps")) {
								jsonObject.put("maps", u);
							} else if (ket.toLowerCase().contains("video") && u.contains("download")) {
								jsonObject.put("video", u);
							} else if (u.contains("download")) {
								jsonObject.put("download", u);
							}
						}
					}

					absensi.put(jsonObject);
				}
			}
			da.put("absensi_data", absensi);

			String waktu = pertemuan.getTanggal() == null ? "-"
					: (SmartDateTimeUtil.getDayString(pertemuan.getTanggal(), pertemuan.getWaktuMulai())
							+ Common.dateFormat6.get().format(pertemuan.getTanggal())) + " "
							+ (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null ? ""
									: pertemuan.getWaktuMulai() + "-" + pertemuan.getWaktuSelesai());

			da.put("waktu", waktu);
			Dosen dosenPengganti = null;
			if (pertemuan.getDosenPengganti() != null) {
				dosenPengganti = (Dosen) ConstantValues.ambil(Dosen.class.getName(), pertemuan.getDosenPengganti());
				if (dosenPengganti != null) {
					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(dosenPengganti));
					da.put("dosenPengganti.id", dosenPengganti.getId());
					da.put("dosenPengganti.nama", dosenPengganti.getNama());
					da.put("dosenPengganti.kode", dosenPengganti.getNidn());
					da.put("dosenPengganti.foto", url);
				}
			}
			Guru guruPengganti = null;
			if (pertemuan.getGuruPengganti() != null) {
				guruPengganti = (Guru) ConstantValues.ambil(Guru.class.getName(), pertemuan.getGuruPengganti());
				if (guruPengganti != null) {
					String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(guruPengganti));
					da.put("guruPengganti.id", guruPengganti.getId());
					da.put("guruPengganti.nama", guruPengganti.getNama());
					da.put("guruPengganti.kode", guruPengganti.getKode());
					da.put("guruPengganti.foto", url);
				}
			}

			String rinci = "";
			if (pertemuan.getPerkuliahan() != null) {
				rinci = pertemuan.getPerkuliahan().infoSimple(dosenPengganti);
			} else if (pertemuan.getJadwalPelajaran() != null) {

				rinci = pertemuan.getJadwalPelajaran().info(guruPengganti);
			} else if (pertemuan.getKelasLesSiswa() != null) {
				rinci = pertemuan.getKelasLesSiswa().infoSimple();
			} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
				rinci = MahasiswaRequestTugasAkhirAction
						.tampilkanInfoDosenSimple(pertemuan.getMahasiswaRequestTugasAkhir()).getValue();
			} else if (pertemuan.getSkripsi() != null) {
				rinci = SkripsiAction.tampilkanInfoDosenSimple(pertemuan.getSkripsi()).getValue();
			} else if (pertemuan.getKelompokKkn() != null) {

				String dsn = "";
				for (Dosen dosen : pertemuan.getKelompokKkn().populateDosenBuNama()) {
					dsn += dsn.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				}

				rinci = "Dosen Pembimbing : " + dsn;
			} else if (pertemuan.getKelompokPkl() != null) {

				String dsn = "";
				for (Dosen dosen : pertemuan.getKelompokPkl().populateDosenBuNama()) {
					dsn += dsn.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				}

				rinci = "Dosen Pembimbing : " + dsn;
			} else if (pertemuan.getKrsMahasiswa() != null && pertemuan.getKrsMahasiswa().getDosenPa() != null) {
				rinci = "Dosen PA : " + pertemuan.getKrsMahasiswa().getDosenPa().getNama();
			} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null
					&& pertemuan.getPertemuanPunyaGrupPertemuan().getGrupPertemuan() != null
					&& pertemuan.getPertemuanPunyaGrupPertemuan().getGrupPertemuan().getDosen() != null) {
				rinci = "Dosen konsultasi: "
						+ pertemuan.getPertemuanPunyaGrupPertemuan().getGrupPertemuan().getDosen().getNama();
			}
			da.put("rinci", rinci);
			// Identitas periode perkuliahan untuk filter global TA+Semester di
			// klien; hanya ada bila pertemuan milik perkuliahan kampus.
			if (pertemuan.getPerkuliahan() != null) {
				da.put("tahun_ajaran", pertemuan.getPerkuliahan().getTahunAjaran());
				da.put("ganjil_genap", pertemuan.getPerkuliahan().getGanjilGenap());
				if (pertemuan.getPerkuliahan().getStatusSemesterPendek() != null) {
					da.put("semester_pendek", pertemuan.getPerkuliahan().getStatusSemesterPendek());
				}
			}
			List<Dosen> map = pertemuan.ambilDosen();

			if (pertemuan.getDosenPengganti() != null) {
				dosenPengganti = (Dosen) ConstantValues.ambil(Dosen.class.getName(), pertemuan.getDosenPengganti());
				if (dosenPengganti != null) {
					map.add(dosenPengganti);
				}

			}

			JSONArray dosens = new JSONArray();
			String namaDosens = "";
			for (Dosen dsn : map) {
				JSONObject daDosen = new JSONObject();
				String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(dsn));
				daDosen.put("id", dsn.getId());
				daDosen.put("nama", dsn.getNama());
				daDosen.put("kode", dsn.getKode());
				daDosen.put("foto", url);
				dosens.put(daDosen);
				namaDosens += namaDosens.isEmpty() ? dsn.getNama() : ", " + dsn.getNama();
			}
			map = null;
			da.put("dosen", dosens);
			da.put("namaDosen", namaDosens);

			List<Guru> mapGuru = pertemuan.ambilGuru();

			if (pertemuan.getGuruPengganti() != null) {
				guruPengganti = (Guru) ConstantValues.ambil(Guru.class.getName(), pertemuan.getDosenPengganti());
				if (guruPengganti != null) {
					mapGuru.add(guruPengganti);
				}

			}

			JSONArray gurus = new JSONArray();
			String namaGurus = "";
			for (Guru dsn : mapGuru) {
				JSONObject daGuru = new JSONObject();
				String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(dsn));
				daGuru.put("id", dsn.getId());
				daGuru.put("nama", dsn.getNama());
				daGuru.put("kode", dsn.getKode());
				daGuru.put("foto", url);
				gurus.put(daGuru);
				namaGurus += namaGurus.isEmpty() ? dsn.getNama() : ", " + dsn.getNama();
			}
			map = null;
			da.put("guru", gurus);
			da.put("namaGuru", namaGurus);

//			List<Mahasiswa> mapMhs = pertemuan.ambilMahasiswa();
//			JSONArray mahasiswa = new JSONArray();
//			for (Mahasiswa mhs : mapMhs) {
//				JSONObject daDosen = new JSONObject();
//				String url = CommonMedia.getUrlFotoPengguna(new Tbmuser(mhs));
//				daDosen.put("id", mhs.getId());
//				daDosen.put("nama", mhs.getNama());
//				daDosen.put("kode", mhs.getNim());
//				daDosen.put("foto", url);
//				mahasiswa.put(daDosen);
//			}
//			mapMhs = null;
//			da.put("mahasiswa", mahasiswa);

			List<String> urls = Common.getUrls(pertemuan.getCatatan());
			String catat = pertemuan.getCatatan();
			catat = catat.replaceAll("\n", "<br>");
			for (String url : urls) {
				catat = org.apache.commons.lang3.StringUtils.replace(catat, url,
						"<a href='" + url + "' target='_blank'>" + url + "</a>");
			}
			da.put("catatan", catat);

			TreeMap<String, String> d = pertemuan.ambilData("online", null);
			String onl = "";
			if (!d.isEmpty()) {
				for (String user : d.keySet()) {
					try {
						String jam = d.get(user);
						String[] u = user.split("-");
						if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
								|| u[2].equalsIgnoreCase("CalonMahasiswa") || u[2].equalsIgnoreCase("CalonSiswa")
								|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
							onl += onl.isEmpty() ? u[0] + " (" + jam + ")" : "," + u[0] + " (" + jam + ")";
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:911");
						// e.printStackTrace();
					}
				}
			}
			da.put("online", onl);

			Set<String> moderator = new HashSet<String>();
			JSONObject jsonObject = new JSONObject(pertemuan.getCalendarEvent());
			if (!jsonObject.isNull("attendees")) {
				JSONArray attendees = jsonObject.getJSONArray("attendees");
				for (int i = 0; i < attendees.length(); i++) {
					try {
						JSONObject attendee = attendees.getJSONObject(i);
						if (!attendee.isNull("organizer") && attendee.getBoolean("organizer")) {
							moderator.add(attendee.getString("email").trim());
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:928");
						// e.printStackTrace();
					}
				}
			}
			if (!jsonObject.isNull("organizer")) {
				try {
					JSONObject organizer = jsonObject.getJSONObject("organizer");
					moderator.add(organizer.getString("email").trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:937");
					// e.printStackTrace();
				}
			}

			da.put("moderator", moderator.toString().replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\"", ""));

			Dosen dosenUtama = pertemuan.dosenUtama();
			if (dosenUtama != null && !dosenUtama.getOnlineMenggunakan().equals(Dosen.TIDAK_AKTIF)
					&& !dosenUtama.getOnlineLink().trim().isEmpty()) {
				Integer ol = dosenUtama.getOnlineMenggunakan();
				da.put("onlineMenggunakan", ol);
				String server = dosenUtama.getOnlineLink();
				da.put("onlineLink", server);
			} else {
				da.put("onlineMenggunakan", pertemuan.getOnlineMenggunakan());
				String server = pertemuan.generateJitsiLink(servletRequest);
				if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.GOOGLE_MEET)) {
					server = pertemuan.getMeetLink();
				} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.ZOOM)) {
					server = pertemuan.getZoomLink();
				} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.BBB)) {
					server = pertemuan.getBbbLink();
				} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.SKYPE)) {
					server = pertemuan.getSkypeLink();
				} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.SKYPE)) {
					server = pertemuan.getSkypeLink();
				} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.WA)) {
					server = pertemuan.getWaLink();
				} else if (pertemuan.getOnlineMenggunakan().equals(Pertemuan.LAIN)) {
					server = pertemuan.getLainLink();
				}

				da.put("onlineLink", server);
			}

			Number diskusi = pertemuan.ambilJumlahPertemuanPunyaDiskusi();

			da.put("diskusi", diskusi);

			// Tidak ada tabel pelacak baca per pengguna, jadi server tidak
			// mengklaim jumlah "belum dibaca". Yang dikirim adalah stempel
			// aktivitas diskusi terakhir (thread maupun balasan) supaya klien
			// bisa menandai "diskusi baru" relatif terhadap kunjungan lokalnya.
			if (diskusi != null && diskusi.intValue() > 0) {
				Session sesiDiskusi = HibernateUtil.openSession();
				try {
					Object diskusiTerakhir = sesiDiskusi
							.createCriteria(PertemuanPunyaDiskusi.class)
							.add(Restrictions.eq("pertemuan", pertemuan))
							.setProjection(Projections.max("tanggal_dirubah"))
							.uniqueResult();
					if (diskusiTerakhir instanceof Date) {
						da.put("diskusi_terakhir", ((Date) diskusiTerakhir).getTime());
					}
				} catch (Exception eDiskusi) {
					// Stempel opsional; kegagalan tidak boleh menggagalkan linimasa.
				} finally {
					HibernateUtil.closeSessionQuietly(sesiDiskusi);
				}
			}

			// §14.11: deadline terdekat, progres pengumpulan tugas, dan aktivitas
			// diskusi terakhir untuk kartu e-Learning. Semua nilai dihitung dari
			// data yang memang ada; klien tidak boleh mengarang angka.
			Date waktuSekarang = ais.ui.util.WaktuUtil.getCalendar().getTime();
			Date deadlineTerdekat = null;
			int tugasTotal14 = 0;
			int tugasDikumpul14 = 0;
			boolean pesertaProgres = tbmuser != null && (tbmuser.getMahasiswa() != null
					|| tbmuser.getSiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null);

			if (tampilUjian) {
				TreeMap<Long, PertemuanPunyaUjian> ujiandata = pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser);
				int ujian = ujiandata.size();
				da.put("ujian", ujian);
				JSONArray ujians = new JSONArray();
				if (ujian > 0) {

					int totalPeserta = 0;
					for (PertemuanPunyaUjian pertemuanPunyaUjian : ujiandata.values()) {

						JSONObject jsonObjectUjian = new JSONObject();
						diplayUjian(pertemuanPunyaUjian, jsonObjectUjian, pertemuan, tbmuser);
						if (pertemuanPunyaUjian.getMulaiUjian() != null
								&& pertemuanPunyaUjian.getMulaiUjian().after(waktuSekarang)
								&& (deadlineTerdekat == null
										|| pertemuanPunyaUjian.getMulaiUjian().before(deadlineTerdekat))) {
							deadlineTerdekat = pertemuanPunyaUjian.getMulaiUjian();
						}
						ujians.put(jsonObjectUjian);

						int tg = pertemuanPunyaUjian.ambilJumlahHasilUjianMahasiswaTelahIkut(false);
						totalPeserta += tg;
					}

					da.put("totalPesertaUjian", totalPeserta);
				} else {
					da.put("totalPesertaUjian", 0);
				}

				da.put("ujian", ujians);
			}

			if (tampilTugas) {
				JSONArray tugases = new JSONArray();
				if (!pertemuan.getJudultugas().trim().isEmpty()) {
					Tugas tugas = pertemuan;
					JSONObject jsonObjectTugas = new JSONObject();
					diplayTugas(tugas, jsonObjectTugas, pertemuan, tbmuser);
					if (tugas.currentTugasFileContent != null) {
						tugasDikumpul14++;
					}
					tugasTotal14++;
					if (tugas.getSelesai() != null && tugas.getSelesai().after(waktuSekarang)
							&& (deadlineTerdekat == null || tugas.getSelesai().before(deadlineTerdekat))) {
						deadlineTerdekat = tugas.getSelesai();
					}
					tugases.put(jsonObjectTugas);
				}

				for (TugasPertemuan tugasPertemuan : pertemuan.ambilTugasPertemuanTotal().values()) {
					if (!tugasPertemuan.getJudultugas().trim().isEmpty()) {
						Tugas tugas = tugasPertemuan;
						JSONObject jsonObjectTugas = new JSONObject();
						diplayTugas(tugas, jsonObjectTugas, pertemuan, tbmuser);
					if (tugas.currentTugasFileContent != null) {
						tugasDikumpul14++;
					}
					tugasTotal14++;
					if (tugas.getSelesai() != null && tugas.getSelesai().after(waktuSekarang)
							&& (deadlineTerdekat == null || tugas.getSelesai().before(deadlineTerdekat))) {
						deadlineTerdekat = tugas.getSelesai();
					}
						tugases.put(jsonObjectTugas);
					}
				}
				da.put("tugas", tugases);
			}

			if (tampilMateri) {
				Number file = pertemuan.ambilJumlahPertemuanFileContent();
				da.put("materi", file);
				Number audio = pertemuan.ambilJumlahAudioPertemuan();
				Number video = pertemuan.ambilJumlahVideoPertemuan();
				da.put("audio", audio);
				da.put("video", video);
			}
			if (deadlineTerdekat != null) {
				da.put("deadline_terdekat", deadlineTerdekat.getTime());
				da.put("deadline_terdekat_label",
						SmartDateTimeUtil.getDayString(deadlineTerdekat, null)
								+ Common.dateFormat5.get().format(deadlineTerdekat));
			}
			// Progres hanya untuk peserta; pengajar mempunyai sudut pandang
			// berbeda (jumlah pengumpul) yang belum dihitung di sini.
			if (pesertaProgres && tugasTotal14 > 0) {
				da.put("tugas_total", tugasTotal14);
				da.put("tugas_dikumpul", tugasDikumpul14);
				da.put("progres", (int) Math.round(tugasDikumpul14 * 100.0 / tugasTotal14));
			}

			TreeMap<String, String> akses = pertemuan.ambilData("akses", null);
			da.put("akses", akses.size());
			akses.clear();
			akses = null;

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:1037");
		}
		return da;
	}

	public static JSONObject daftar_ujian(JSONObject request, HttpServletRequest servletRequest) {
		Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
		return daftar_ujian(tbmuser, request, servletRequest);
	}

	@SuppressWarnings("unchecked")
	public static JSONObject daftar_ujian(Tbmuser tbmuser, JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				boolean refreh = request.isNull("refresh") ? false
						: (request.get("refresh") + "").trim().equalsIgnoreCase("true");

				boolean awal = request.isNull("awal") ? false
						: request.getString("awal").trim().equalsIgnoreCase("true");

				String token = request.isNull("token") ? "------" : request.getString("token");
				String mulaiD = request.isNull("mulai") ? null : request.getString("mulai").trim();
				String sampaiD = request.isNull("sampai") ? null : request.getString("sampai").trim();
				String cari = request.isNull("cari") ? "" : request.getString("cari").trim();
				String hanyaIdSaja = request.isNull("hanyaIdSaja") ? "" : request.getString("hanyaIdSaja").trim();

				String sort = request.isNull("sort") ? "id" : request.getString("sort").trim();
				String order = request.isNull("order") ? "asc" : request.getString("order").trim();

				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.set(Calendar.YEAR, calendarMulai.get(Calendar.YEAR) - 4);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 12);

				Date mulai = calendarMulai.getTime();
				Date sampai = calendarSampai.getTime();

				if (mulaiD != null) {
					try {
						mulai = Common.dateFormat1.get().parse(mulaiD);
						calendarMulai.setTime(mulai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1084");
						// TODO: handle exception
					}
				}
				if (sampaiD != null) {
					try {
						sampai = Common.dateFormat1.get().parse(sampaiD);
						calendarSampai.setTime(sampai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1092");
						// TODO: handle exception
					}
				}

				Sekolah sk = SekolahUtil.getSekolah(servletRequest);
				Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
				Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
				List<Long> pertemuans = new ArrayList<Long>();

				Session session = HibernateUtil.openSession();
				try {
				if (mahasiswa != null) {

					if (refreh) {

						mahasiswa.reInitPertemuan(session, new Label(), calendarMulai.getTime(),
								calendarSampai.getTime());
					}

					pertemuans.addAll(mahasiswa.ambilPertemuan(session).values());

				} else if (dosen != null) {

					if (refreh) {

						dosen.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
					}

					pertemuans.addAll(dosen.ambilPertemuan(session).values());
				} else if (!Common.getApakahAdminLain(tbmuser)) {

					Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
					Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

					if (guru != null) {
						sk = guru.getSekolah();
					}
					if (siswa != null) {
						sk = siswa.getSekolah();
					}

					String mul = null;
					String sam = null;

					Integer bln = 1;
					Integer ke = null;

					boolean jadwalPerkuliahan = !(sk != null && sk.getId() != null);

					boolean jadwalPelajaran = (sk != null && sk.getId() != null);

					boolean jadwalKkn = jadwalPerkuliahan;

					boolean jadwalPkl = jadwalPerkuliahan;

					boolean jadwalKegiatan = jadwalPerkuliahan;

					boolean jadwalRevisi = jadwalPerkuliahan;
					boolean jadwalKonsultasi = jadwalPerkuliahan;

					boolean jadwalBimbingan = jadwalPerkuliahan;

					boolean jadwalKonsultasiLain = jadwalPerkuliahan;

					boolean tdpDiskusi = false;

					boolean tdpUjian = false;

					boolean tdpMateri = false;
					boolean tdpTugas = false;
					boolean tdpCatatan = false;
					boolean tdpAudio = false;
					boolean tdpVideo = false;
					boolean tdpDosenPengganti = false;

					String cariMk = "";
					String cariDosen = "";
					String cariTopik = "";
					String cariCatatan = "";
					String cariMahasiswa = "";
					String cariKelas = "";
					String cariRuang = "";
					Integer ekstra = null;

					boolean remedial = false;
					boolean paralel = false;
					boolean pra = false;
					StatusPertemuan statusPertemuan = null;
					boolean ujian = false;

					String day = null;

					Criteria criteria = DashboardTimelinePertemuan.initStaticCriteria(true, calendarMulai.getTime(),
							calendarSampai.getTime(), tbmuser, cari, mul, sam, bln, statusPertemuan, ke, day, tdpVideo,
							tdpAudio, tdpMateri, jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
							jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian,
							tdpDiskusi, tdpTugas, tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel, pra,
							remedial, cariMk, ekstra != null && ekstra.equals(Perkuliahan.EKSTRA), cariKelas, cariRuang,
							cariDosen, cariMahasiswa, ujian, sk, session);

					pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());
				}

				PagingApi paging = mapsPaging.get(token);
				if (paging == null) {
					paging = new PagingApi();
					mapsPaging.put(token, paging);
				}

				Integer displayPerPage = request.isNull("displayPerPage") ? 10
						: Integer.parseInt(request.getString("displayPerPage").trim());
				Integer activePage = request.isNull("activePage") ? null
						: Integer.parseInt(request.getString("activePage").trim());

				try {
					paging.setPageSize(displayPerPage);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1209");
				}
				try {
					if (activePage != null && paging != null) {
						paging.setActivePage(activePage);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1215");
					// TODO: handle exception
				}

				Integer mulaiParam = request.isNull("mulaiParam") ? null
						: Integer.parseInt(request.getString("mulaiParam").trim());
				Integer sampaiParam = request.isNull("sampaiParam") ? null
						: Integer.parseInt(request.getString("sampaiParam").trim());

				if (paging != null) {
					paging.setAttribute("mulaiParam", mulaiParam);
					paging.setAttribute("sampaiParam", sampaiParam);
				}

				Criterion criterion = Common.getApakahAdminLain(tbmuser) ? Restrictions.sqlRestriction("true")
						: (pertemuans.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.in("pertemuan.id", pertemuans));
				int size = 0;
				try {
					size = ((Number) session.createCriteria(PertemuanPunyaUjian.class)

							.createAlias("ujian", "ujian")

							.add(cari.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.ilike("ujian.kode", cari, MatchMode.ANYWHERE),
											Restrictions.ilike("ujian.nama", cari, MatchMode.ANYWHERE)))

							.add(Restrictions.sqlRestriction(
									"date(this_.mulai_ujian) between date('" + Common.databaseDateFormat.get().format(mulai)
											+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
							.add(criterion).setProjection(Projections.rowCount()).uniqueResult()).intValue();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1246");
					// TODO: handle exception
				}
				paging.setTotalSize(size);

				if (awal) {

					int page = ((Number) session.createCriteria(PertemuanPunyaUjian.class)
							.add(Restrictions.isNotNull("pertemuan")).createAlias("ujian", "ujian")

							.add(cari.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.ilike("ujian.kode", cari, MatchMode.ANYWHERE),
											Restrictions.ilike("ujian.nama", cari, MatchMode.ANYWHERE)))
							.add(criterion).add(Restrictions.sqlRestriction("date(mulai_ujian)<CURRENT_DATE"))

							.add(Restrictions.sqlRestriction(
									"date(this_.mulai_ujian) between date('" + Common.databaseDateFormat.get().format(mulai)
											+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))

							.setProjection(Projections.rowCount()).uniqueResult()).intValue();

					try {
						System.out.println(
								"page -> " + page + ", size -> " + size + ", pertemuans -> " + pertemuans.size());
						paging.setActivePage((int) (page / Common.ROWS_COUNT_ON_PAGE));
					} catch (Exception e) {
						try {

							paging.setActivePage(((int) (page / Common.ROWS_COUNT_ON_PAGE)) - 1);
						} catch (Exception ae) { ais.common.ErrorAuditUtil.record(ae, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1275");
							// TODO: handle exception
						}
					}
				}

				if (hanyaIdSaja != null && hanyaIdSaja.equalsIgnoreCase("true")) {

					List<Object[]> pertemuanPunyaUjians = session.createCriteria(PertemuanPunyaUjian.class)
							.add(Restrictions.isNotNull("pertemuan"))
							.setProjection(Projections.projectionList().add(Projections.property("id"))
									.add(Projections.property("mulaiUjian")))

							.createAlias("ujian", "ujian")

							.add(cari.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.ilike("ujian.kode", cari, MatchMode.ANYWHERE),
											Restrictions.ilike("ujian.nama", cari, MatchMode.ANYWHERE)))

							.add(Restrictions.sqlRestriction(
									"date(this_.mulai_ujian) between date('" + Common.databaseDateFormat.get().format(mulai)
											+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
							.add(criterion)

							.addOrder(
									order.equalsIgnoreCase("asc") ? Order.asc("mulaiUjian") : Order.desc("mulaiUjian"))
							.addOrder(order.equalsIgnoreCase("asc") ? Order.asc(sort) : Order.desc(sort))

							.setMaxResults(displayPerPage)
							.setFirstResult(displayPerPage * (paging == null ? 0 : paging.getActivePage())).list();

					JSONArray ujians = new JSONArray();
					if (pertemuanPunyaUjians.size() > 0) {

						for (Object[] pertemuanPunyaUjian : pertemuanPunyaUjians) {

							Long idUjian = (Long) pertemuanPunyaUjian[0];
							Date mulaiUjian = (Date) pertemuanPunyaUjian[1];

							JSONObject jsonObjectData = new JSONObject();
							jsonObjectData.put("id", idUjian);
							jsonObjectData.put("tanggal", Common.dateFormat85.get().format(mulaiUjian));
							ujians.put(jsonObjectData);
						}

						jsonObject.put("totalPesertaUjian", 0);
					} else {
						jsonObject.put("totalPesertaUjian", 0);
					}

					jsonObject.put("data", ujians);

				} else {
					List<PertemuanPunyaUjian> pertemuanPunyaUjians = session.createCriteria(PertemuanPunyaUjian.class)
							.add(Restrictions.isNotNull("pertemuan")).createAlias("ujian", "ujian")

							.add(cari.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.ilike("ujian.kode", cari, MatchMode.ANYWHERE),
											Restrictions.ilike("ujian.nama", cari, MatchMode.ANYWHERE)))

							.add(Restrictions.sqlRestriction(
									"date(this_.mulai_ujian) between date('" + Common.databaseDateFormat.get().format(mulai)
											+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')"))
							.add(criterion)

							.addOrder(order.equalsIgnoreCase("asc") ? Order.asc("mulaiUjian") : Order.asc("mulaiUjian"))
							.addOrder(order.equalsIgnoreCase("asc") ? Order.asc(sort) : Order.desc(sort))
							.setMaxResults(displayPerPage)
							.setFirstResult(displayPerPage * (paging == null ? 0 : paging.getActivePage())).list();

					JSONArray ujians = new JSONArray();
					if (pertemuanPunyaUjians.size() > 0) {

						int totalPeserta = 0;
						for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {

							JSONObject jsonObjectUjian = new JSONObject();
							diplayUjian(pertemuanPunyaUjian, jsonObjectUjian, pertemuanPunyaUjian.getPertemuan(),
									tbmuser);
							jsonObjectUjian.put("info_pertemuan", displayPertemuan(pertemuanPunyaUjian.getPertemuan(),
									tbmuser, servletRequest, false, false, false));
							ujians.put(jsonObjectUjian);

							int tg = pertemuanPunyaUjian.ambilJumlahHasilUjianMahasiswaTelahIkut(false);
							jsonObjectUjian.put("jumlahPesertaUjian", tg);
							totalPeserta += tg;
						}

						jsonObject.put("totalPesertaUjian", totalPeserta);
					} else {
						jsonObject.put("totalPesertaUjian", 0);
					}

					jsonObject.put("data", ujians);
				}

				if (paging != null) {
					jsonObject.put("pageSize", paging.getPageSize());
					jsonObject.put("totalSize", paging.getTotalSize());
					jsonObject.put("activePage", paging.getActivePage());
				}

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:1390");
			}
		}
		return jsonObject;
	}

	public static JSONObject daftar_tugas(JSONObject request, HttpServletRequest servletRequest) {
		Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
		return daftar_tugas(tbmuser, request, servletRequest);
	}

	@SuppressWarnings("unchecked")
	public static JSONObject daftar_tugas(Tbmuser tbmuser, JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				boolean refreh = request.isNull("refresh") ? false
						: (request.get("refresh") + "").trim().equalsIgnoreCase("true");

				boolean awal = request.isNull("awal") ? false
						: request.getString("awal").trim().equalsIgnoreCase("true");

				String token = request.isNull("token") ? "------" : request.getString("token");
				String mulaiD = request.isNull("mulai") ? null : request.getString("mulai").trim();
				String sampaiD = request.isNull("sampai") ? null : request.getString("sampai").trim();
				String cari = request.isNull("cari") ? "" : request.getString("cari").trim();
				String hanyaIdSaja = request.isNull("hanyaIdSaja") ? "" : request.getString("hanyaIdSaja").trim();
//				String sort = request.isNull("sort") ? "id" : request.getString("sort").trim();
				String order = request.isNull("order") ? "asc" : request.getString("order").trim();
				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.set(Calendar.YEAR, calendarMulai.get(Calendar.YEAR) - 4);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 12);

				Date mulai = calendarMulai.getTime();
				Date sampai = calendarSampai.getTime();

				if (mulaiD != null) {
					try {
						mulai = Common.dateFormat1.get().parse(mulaiD);
						calendarMulai.setTime(mulai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1436");
						// TODO: handle exception
					}
				}
				if (sampaiD != null) {
					try {
						sampai = Common.dateFormat1.get().parse(sampaiD);
						calendarSampai.setTime(sampai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1444");
						// TODO: handle exception
					}
				}

				Sekolah sk = SekolahUtil.getSekolah(servletRequest);
				Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
				Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
				List<Long> pertemuans = new ArrayList<Long>();

				Session session = HibernateUtil.openSession();
				try {
				if (mahasiswa != null) {

					if (refreh) {

						mahasiswa.reInitPertemuan(session, new Label(), calendarMulai.getTime(),
								calendarSampai.getTime());
					}

					pertemuans.addAll(mahasiswa.ambilPertemuan(session).values());

				} else if (dosen != null) {

					if (refreh) {

						dosen.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
					}

					pertemuans.addAll(dosen.ambilPertemuan(session).values());
				} else if (!Common.getApakahAdminLain(tbmuser)) {

					Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
					Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

					if (guru != null) {
						sk = guru.getSekolah();
					}
					if (siswa != null) {
						sk = siswa.getSekolah();
					}

					String mul = null;
					String sam = null;

					Integer bln = 1;
					Integer ke = null;

					boolean jadwalPerkuliahan = !(sk != null && sk.getId() != null);

					boolean jadwalPelajaran = (sk != null && sk.getId() != null);

					boolean jadwalKkn = jadwalPerkuliahan;

					boolean jadwalPkl = jadwalPerkuliahan;

					boolean jadwalKegiatan = jadwalPerkuliahan;

					boolean jadwalRevisi = jadwalPerkuliahan;
					boolean jadwalKonsultasi = jadwalPerkuliahan;

					boolean jadwalBimbingan = jadwalPerkuliahan;

					boolean jadwalKonsultasiLain = jadwalPerkuliahan;

					boolean tdpDiskusi = false;

					boolean tdpUjian = false;

					boolean tdpMateri = false;
					boolean tdpTugas = false;
					boolean tdpCatatan = false;
					boolean tdpAudio = false;
					boolean tdpVideo = false;
					boolean tdpDosenPengganti = false;

					String cariMk = "";
					String cariDosen = "";
					String cariTopik = "";
					String cariCatatan = "";
					String cariMahasiswa = "";
					String cariKelas = "";
					String cariRuang = "";
					Integer ekstra = null;

					boolean remedial = false;
					boolean paralel = false;
					boolean pra = false;
					StatusPertemuan statusPertemuan = null;
					boolean ujian = false;

					String day = null;

					Criteria criteria = DashboardTimelinePertemuan.initStaticCriteria(true, calendarMulai.getTime(),
							calendarSampai.getTime(), tbmuser, cari, mul, sam, bln, statusPertemuan, ke, day, tdpVideo,
							tdpAudio, tdpMateri, jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
							jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian,
							tdpDiskusi, tdpTugas, tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel, pra,
							remedial, cariMk, ekstra != null && ekstra.equals(Perkuliahan.EKSTRA), cariKelas, cariRuang,
							cariDosen, cariMahasiswa, ujian, sk, session);

					pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());
				}

				PagingApi paging = mapsPaging.get(token);
				if (paging == null) {
					paging = new PagingApi();
					mapsPaging.put(token, paging);
				}

				Integer displayPerPage = request.isNull("displayPerPage") ? 10
						: Integer.parseInt(request.getString("displayPerPage").trim());
				Integer activePage = request.isNull("activePage") ? null
						: Integer.parseInt(request.getString("activePage").trim());
				try {
					paging.setPageSize(displayPerPage);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1560");
				}
				try {
					if (activePage != null && paging != null) {
						paging.setActivePage(activePage);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1566");
					// TODO: handle exception
				}

				Integer mulaiParam = request.isNull("mulaiParam") ? null
						: Integer.parseInt(request.getString("mulaiParam").trim());
				Integer sampaiParam = request.isNull("sampaiParam") ? null
						: Integer.parseInt(request.getString("sampaiParam").trim());

				if (paging != null) {
					paging.setAttribute("mulaiParam", mulaiParam);
					paging.setAttribute("sampaiParam", sampaiParam);
				}

				String inPer = "";
				for (Long id : pertemuans) {
					inPer += inPer.isEmpty() ? id.toString() : "," + id;
				}

				String where = "date(a.mulai) between date('" + Common.databaseDateFormat.get().format(mulai)
						+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')";

				if (Common.getApakahAdminLain(tbmuser)) {
					where = where + " and true ";
				} else if (inPer.isEmpty()) {
					where = where + " and false ";
				} else {
					where = where + " and a.pertemuan in (" + inPer + ") ";

				}

				if (!cari.isEmpty()) {
					where = where + " and a.judultugas ilike '%" + cari + "%' ";
				}

				String sql = "select count(*) as size from (\r\n"
						+ "	select id,'pertemuan',(case when mulai is null then tanggal else mulai end) as mulai,selesai,judultugas,id as pertemuan from pertemuan where judultugas is not null and judultugas!='' \r\n"
						+ "	union all\r\n"
						+ "	select aa.id,'tugas',(case when aa.mulai is null then bb.tanggal else aa.mulai end) as mulai,aa.selesai,aa.judultugas,aa.pertemuan from tugas_pertemuan aa inner join pertemuan bb on (aa.pertemuan=bb.id) where aa.judultugas is not null and aa.judultugas!='' \r\n"
						+ ") a where " + where + ";";

				int size = ((Number) session.createSQLQuery(sql).uniqueResult()).intValue();
				paging.setTotalSize(size);
				paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);

				if (awal) {

					String where1 = where + " and date(a.mulai) < CURRENT_DATE ";

					sql = "select count(*) as size from (\r\n"
							+ "	select id,'pertemuan',(case when mulai is null then tanggal else mulai end) as mulai,selesai,judultugas,id as pertemuan from pertemuan where judultugas is not null and judultugas!='' \r\n"
							+ "	union all\r\n"
							+ "	select aa.id,'tugas',(case when aa.mulai is null then bb.tanggal else aa.mulai end) as mulai,aa.selesai,aa.judultugas,aa.pertemuan from tugas_pertemuan aa inner join pertemuan bb on (aa.pertemuan=bb.id) where aa.judultugas is not null and aa.judultugas!='' \r\n"
							+ ") a where " + where1 + ";";

					int page = ((Number) session.createSQLQuery(sql).uniqueResult()).intValue();

					try {
						System.out.println(
								"page -> " + page + ", size -> " + size + ", pertemuans -> " + pertemuans.size());
						paging.setActivePage((int) (page / Common.ROWS_COUNT_ON_PAGE));
					} catch (Exception e) {
						try {

							paging.setActivePage(((int) (page / Common.ROWS_COUNT_ON_PAGE)) - 1);
						} catch (Exception ae) { ais.common.ErrorAuditUtil.record(ae, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1631");
							// TODO: handle exception
						}
					}
				}

				String orderBy = "a.mulai asc, a.id asc";
				if (!order.equalsIgnoreCase("asc")) {
					orderBy = "a.mulai desc, a.id desc";
				}

				sql = "select * from (\r\n"
						+ "	select id,'pertemuan',(case when mulai is null then tanggal else mulai end) as mulai,selesai,judultugas,id as pertemuan from pertemuan where judultugas is not null and judultugas!='' \r\n"
						+ "	union all\r\n"
						+ "	select aa.id,'tugas',(case when aa.mulai is null then bb.tanggal else aa.mulai end) as mulai,aa.selesai,aa.judultugas,aa.pertemuan from tugas_pertemuan aa inner join pertemuan bb on (aa.pertemuan=bb.id) where aa.judultugas is not null and aa.judultugas!='' \r\n"
						+ ") a where " + where + "  order by " + orderBy + " limit " + Common.ROWS_COUNT_ON_PAGE
						+ " offset " + (Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						+ ";";

				List<Object[]> tugasPertemuans = session.createSQLQuery(sql).list();

				JSONArray tugases = new JSONArray();
				for (Object[] objects : tugasPertemuans) {
					Long id = ((Number) objects[0]).longValue();
					String jenis = objects[1] + "";
					TugasPertemuan tugasPertemuan = jenis.equalsIgnoreCase("tugas")
							? (TugasPertemuan) session.createCriteria(TugasPertemuan.class).add(Restrictions.idEq(id))
									.uniqueResult()
							: null;

					Pertemuan pertemuan = jenis
							.equalsIgnoreCase("pertemuan")
									? (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, id.toString(), true)
									: (tugasPertemuan != null && tugasPertemuan.getPertemuan() != null
											? (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
													tugasPertemuan.getPertemuan().toString(), true)
											: null);
					if (pertemuan == null) {
						continue;
					}

					Tugas tugas = (Tugas) (tugasPertemuan != null ? tugasPertemuan : pertemuan);
					if (tugas.getJudultugas() != null && !tugas.getJudultugas().trim().isEmpty()) {

						if (hanyaIdSaja != null && hanyaIdSaja.equalsIgnoreCase("true")) {

							Long idUjian = tugas.getId();
							Date mulaiUjian = tugas.getMulai();

							if (mulaiUjian != null) {
								JSONObject jsonObjectData = new JSONObject();
								jsonObjectData.put("id", idUjian + "_" + tugas.getClass().getSimpleName());
								jsonObjectData.put("tanggal", Common.dateFormat85.get().format(mulaiUjian));
								tugases.put(jsonObjectData);
							}

						} else {
							JSONObject jsonObjectTugas = new JSONObject();
							jsonObjectTugas.put("info_pertemuan",
									displayPertemuan(pertemuan, tbmuser, servletRequest, false, false, false));
							if (tugasPertemuan != null) {
								diplayTugas(tugasPertemuan, jsonObjectTugas, pertemuan, tbmuser);
							} else {
								diplayTugas(pertemuan, jsonObjectTugas, pertemuan, tbmuser);
							}
							tugases.put(jsonObjectTugas);
						}
					}

				}

				jsonObject.put("data", tugases);

				if (paging != null) {
					jsonObject.put("pageSize", paging.getPageSize());
					jsonObject.put("totalSize", paging.getTotalSize());
					jsonObject.put("activePage", paging.getActivePage());
				}

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:1723");
			}
		}
		return jsonObject;
	}

	public static JSONObject daftar_tugas_kelompok(JSONObject request, HttpServletRequest servletRequest) {
		Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
		return daftar_tugas_kelompok(tbmuser, request, servletRequest);
	}

	@SuppressWarnings("unchecked")
	public static JSONObject daftar_tugas_kelompok(Tbmuser tbmuser, JSONObject request,
			HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				boolean refreh = request.isNull("refresh") ? false
						: (request.get("refresh") + "").trim().equalsIgnoreCase("true");

				boolean awal = request.isNull("awal") ? false
						: request.getString("awal").trim().equalsIgnoreCase("true");

				String token = request.isNull("token") ? "------" : request.getString("token");
				String mulaiD = request.isNull("mulai") ? null : request.getString("mulai").trim();
				String sampaiD = request.isNull("sampai") ? null : request.getString("sampai").trim();
				String cari = request.isNull("cari") ? "" : request.getString("cari").trim();
				String hanyaIdSaja = request.isNull("hanyaIdSaja") ? "" : request.getString("hanyaIdSaja").trim();
//				String sort = request.isNull("sort") ? "id" : request.getString("sort").trim();
				String order = request.isNull("order") ? "asc" : request.getString("order").trim();
				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.set(Calendar.YEAR, calendarMulai.get(Calendar.YEAR) - 4);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 12);

				Date mulai = calendarMulai.getTime();
				Date sampai = calendarSampai.getTime();

				if (mulaiD != null) {
					try {
						mulai = Common.dateFormat1.get().parse(mulaiD);
						calendarMulai.setTime(mulai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1770");
						// TODO: handle exception
					}
				}
				if (sampaiD != null) {
					try {
						sampai = Common.dateFormat1.get().parse(sampaiD);
						calendarSampai.setTime(sampai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1778");
						// TODO: handle exception
					}
				}

				Sekolah sk = SekolahUtil.getSekolah(servletRequest);
				Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
				Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
				List<Long> pertemuans = new ArrayList<Long>();

				Session session = HibernateUtil.openSession();
				try {
				if (mahasiswa != null) {

					if (refreh) {

						mahasiswa.reInitPertemuan(session, new Label(), calendarMulai.getTime(),
								calendarSampai.getTime());
					}

					pertemuans.addAll(mahasiswa.ambilPertemuan(session).values());

				} else if (dosen != null) {

					if (refreh) {

						dosen.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
					}

					pertemuans.addAll(dosen.ambilPertemuan(session).values());
				} else if (!Common.getApakahAdminLain(tbmuser)) {

					Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
					Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

					if (guru != null) {
						sk = guru.getSekolah();
					}
					if (siswa != null) {
						sk = siswa.getSekolah();
					}

					String mul = null;
					String sam = null;

					Integer bln = 1;
					Integer ke = null;

					boolean jadwalPerkuliahan = !(sk != null && sk.getId() != null);

					boolean jadwalPelajaran = (sk != null && sk.getId() != null);

					boolean jadwalKkn = jadwalPerkuliahan;

					boolean jadwalPkl = jadwalPerkuliahan;

					boolean jadwalKegiatan = jadwalPerkuliahan;

					boolean jadwalRevisi = jadwalPerkuliahan;
					boolean jadwalKonsultasi = jadwalPerkuliahan;

					boolean jadwalBimbingan = jadwalPerkuliahan;

					boolean jadwalKonsultasiLain = jadwalPerkuliahan;

					boolean tdpDiskusi = false;

					boolean tdpUjian = false;

					boolean tdpMateri = false;
					boolean tdpTugas = false;
					boolean tdpCatatan = false;
					boolean tdpAudio = false;
					boolean tdpVideo = false;
					boolean tdpDosenPengganti = false;

					String cariMk = "";
					String cariDosen = "";
					String cariTopik = "";
					String cariCatatan = "";
					String cariMahasiswa = "";
					String cariKelas = "";
					String cariRuang = "";
					Integer ekstra = null;

					boolean remedial = false;
					boolean paralel = false;
					boolean pra = false;
					StatusPertemuan statusPertemuan = null;
					boolean ujian = false;

					String day = null;

					Criteria criteria = DashboardTimelinePertemuan.initStaticCriteria(true, calendarMulai.getTime(),
							calendarSampai.getTime(), tbmuser, cari, mul, sam, bln, statusPertemuan, ke, day, tdpVideo,
							tdpAudio, tdpMateri, jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
							jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian,
							tdpDiskusi, tdpTugas, tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel, pra,
							remedial, cariMk, ekstra != null && ekstra.equals(Perkuliahan.EKSTRA), cariKelas, cariRuang,
							cariDosen, cariMahasiswa, ujian, sk, session);

					pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());
				}

				PagingApi paging = mapsPaging.get(token);
				if (paging == null) {
					paging = new PagingApi();
					mapsPaging.put(token, paging);
				}

				Integer displayPerPage = request.isNull("displayPerPage") ? 10
						: Integer.parseInt(request.getString("displayPerPage").trim());
				Integer activePage = request.isNull("activePage") ? null
						: Integer.parseInt(request.getString("activePage").trim());
				try {
					paging.setPageSize(displayPerPage);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1894");
				}
				try {
					if (activePage != null && paging != null) {
						paging.setActivePage(activePage);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1900");
					// TODO: handle exception
				}

				Integer mulaiParam = request.isNull("mulaiParam") ? null
						: Integer.parseInt(request.getString("mulaiParam").trim());
				Integer sampaiParam = request.isNull("sampaiParam") ? null
						: Integer.parseInt(request.getString("sampaiParam").trim());

				if (paging != null) {
					paging.setAttribute("mulaiParam", mulaiParam);
					paging.setAttribute("sampaiParam", sampaiParam);
				}

				String inPer = "";
				for (Long id : pertemuans) {
					inPer += inPer.isEmpty() ? id.toString() : "," + id;
				}

				String where = "date(a.mulai) between date('" + Common.databaseDateFormat.get().format(mulai)
						+ "') and date('" + Common.databaseDateFormat.get().format(sampai) + "')";

				if (Common.getApakahAdminLain(tbmuser)) {
					where = where + " and true ";
				} else if (inPer.isEmpty()) {
					where = where + " and false ";
				} else {
					where = where + " and a.pertemuan in (" + inPer + ") ";

				}

				if (!cari.isEmpty()) {
					where = where + " and a.judul ilike '%" + cari + "%' ";
				}

				String sql = "select count(*) as size from (\r\n"
						+ "	select id,'tugas_kelompok',mulai,selesai,judul,id as tugas_kelompok from tugas_kelompok where judul is not null and judul!='' \r\n"
						+ ") a where " + where + ";";

				int size = ((Number) session.createSQLQuery(sql).uniqueResult()).intValue();
				paging.setTotalSize(size);
				paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);

				if (awal) {

					String where1 = where + " and date(a.mulai) < CURRENT_DATE ";

					sql = "select count(*) as size from (\r\n"
							+ "	select aa.id,'tugas_kelompok',(case when aa.mulai is null then bb.tanggal else aa.mulai end) as mulai,aa.selesai,aa.judul,aa.pertemuan from tugas_kelompok aa inner join pertemuan bb on (aa.pertemuan=bb.id) where aa.judul is not null and aa.judul!='' \r\n"
							+ ") a where " + where1 + ";";

					int page = ((Number) session.createSQLQuery(sql).uniqueResult()).intValue();

					try {
						System.out.println(
								"page -> " + page + ", size -> " + size + ", pertemuans -> " + pertemuans.size());
						paging.setActivePage((int) (page / Common.ROWS_COUNT_ON_PAGE));
					} catch (Exception e) {
						try {

							paging.setActivePage(((int) (page / Common.ROWS_COUNT_ON_PAGE)) - 1);
						} catch (Exception ae) { ais.common.ErrorAuditUtil.record(ae, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:1961");
							// TODO: handle exception
						}
					}
				}

				String orderBy = "a.mulai asc, a.id asc";
				if (!order.equalsIgnoreCase("asc")) {
					orderBy = "a.mulai desc, a.id desc";
				}

				sql = "select * from (\r\n"
						+ "	select aa.id,'tugas_kelompok',(case when aa.mulai is null then bb.tanggal else aa.mulai end) as mulai,aa.selesai,aa.judul,aa.pertemuan from tugas_kelompok aa inner join pertemuan bb on (aa.pertemuan=bb.id) where aa.judul is not null and aa.judul!='' \r\n"
						+ ") a where " + where + "  order by " + orderBy + " limit " + Common.ROWS_COUNT_ON_PAGE
						+ " offset " + (Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						+ ";";

				System.out.println("sql -> " + sql);

				List<Object[]> tugasPertemuans = session.createSQLQuery(sql).list();

				JSONArray tugases = new JSONArray();
				for (Object[] objects : tugasPertemuans) {
					Long id = ((Number) objects[0]).longValue();
					TugasKelompok tugasKelompok = (TugasKelompok) session.createCriteria(TugasKelompok.class)
							.add(Restrictions.idEq(id)).uniqueResult();

					if (tugasKelompok.getJudultugas() != null && !tugasKelompok.getJudultugas().trim().isEmpty()) {

						if (hanyaIdSaja != null && hanyaIdSaja.equalsIgnoreCase("true")) {

							Long idUjian = tugasKelompok.getId();
							Date mulaiUjian = tugasKelompok.getMulai();

							if (mulaiUjian != null) {
								JSONObject jsonObjectData = new JSONObject();
								jsonObjectData.put("id", idUjian + "_" + tugasKelompok.getClass().getSimpleName());
								jsonObjectData.put("tanggal", Common.dateFormat85.get().format(mulaiUjian));
								tugases.put(jsonObjectData);
							}

						} else {
							Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
									tugasKelompok.getPertemuan().toString(), true);

							JSONObject jsonObjectTugas = new JSONObject();
							jsonObjectTugas.put("info_pertemuan",
									displayPertemuan(pertemuan, tbmuser, servletRequest, false, false, false));
							diplayTugas(tugasKelompok, jsonObjectTugas, pertemuan, tbmuser);
							tugases.put(jsonObjectTugas);
						}
					}

				}

				jsonObject.put("data", tugases);

				if (paging != null) {
					jsonObject.put("pageSize", paging.getPageSize());
					jsonObject.put("totalSize", paging.getTotalSize());
					jsonObject.put("activePage", paging.getActivePage());
				}

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:2037");
			}
		}
		return jsonObject;
	}

	public static JSONObject daftar_materi(JSONObject request, HttpServletRequest servletRequest) {
		Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
		return daftar_materi(tbmuser, request, servletRequest);
	}

	@SuppressWarnings("unchecked")
	public static JSONObject daftar_materi(Tbmuser tbmuser, JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				boolean refreh = request.isNull("refresh") ? false
						: (request.get("refresh") + "").trim().equalsIgnoreCase("true");

				boolean awal = request.isNull("awal") ? false
						: request.getString("awal").trim().equalsIgnoreCase("true");

				String token = request.isNull("token") ? "------" : request.getString("token");
				String mulaiD = request.isNull("mulai") ? null : request.getString("mulai").trim();
				String sampaiD = request.isNull("sampai") ? null : request.getString("sampai").trim();
				String cari = request.isNull("cari") ? "" : request.getString("cari").trim();
				String hanyaIdSaja = request.isNull("hanyaIdSaja") ? "" : request.getString("hanyaIdSaja").trim();
				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.set(Calendar.YEAR, calendarMulai.get(Calendar.YEAR) - 4);
				String sort = request.isNull("sort") ? "id" : request.getString("sort").trim();
				String order = request.isNull("order") ? "asc" : request.getString("order").trim();
				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 12);

				Date mulai = calendarMulai.getTime();
				Date sampai = calendarSampai.getTime();

				if (mulaiD != null) {
					try {
						mulai = Common.dateFormat1.get().parse(mulaiD);
						calendarMulai.setTime(mulai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2082");
						// TODO: handle exception
					}
				}
				if (sampaiD != null) {
					try {
						sampai = Common.dateFormat1.get().parse(sampaiD);
						calendarSampai.setTime(sampai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2090");
						// TODO: handle exception
					}
				}

				Sekolah sk = SekolahUtil.getSekolah(servletRequest);
				Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
				Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
				List<Long> pertemuans = new ArrayList<Long>();

				Session session = HibernateUtil.openSession();
				try {
				if (mahasiswa != null) {

					if (refreh) {

						mahasiswa.reInitPertemuan(session, new Label(), calendarMulai.getTime(),
								calendarSampai.getTime());
					}

					pertemuans.addAll(mahasiswa.ambilPertemuan(session).values());

				} else if (dosen != null) {

					if (refreh) {

						dosen.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
					}

					pertemuans.addAll(dosen.ambilPertemuan(session).values());
				} else if (!Common.getApakahAdminLain(tbmuser)) {

					Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
					Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

					if (guru != null) {
						sk = guru.getSekolah();
					}
					if (siswa != null) {
						sk = siswa.getSekolah();
					}

					String mul = null;
					String sam = null;

					Integer bln = 1;
					Integer ke = null;

					boolean jadwalPerkuliahan = !(sk != null && sk.getId() != null);

					boolean jadwalPelajaran = (sk != null && sk.getId() != null);

					boolean jadwalKkn = jadwalPerkuliahan;

					boolean jadwalPkl = jadwalPerkuliahan;

					boolean jadwalKegiatan = jadwalPerkuliahan;

					boolean jadwalRevisi = jadwalPerkuliahan;
					boolean jadwalKonsultasi = jadwalPerkuliahan;

					boolean jadwalBimbingan = jadwalPerkuliahan;

					boolean jadwalKonsultasiLain = jadwalPerkuliahan;

					boolean tdpDiskusi = false;

					boolean tdpUjian = false;

					boolean tdpMateri = false;
					boolean tdpTugas = false;
					boolean tdpCatatan = false;
					boolean tdpAudio = false;
					boolean tdpVideo = false;
					boolean tdpDosenPengganti = false;

					String cariMk = "";
					String cariDosen = "";
					String cariTopik = "";
					String cariCatatan = "";
					String cariMahasiswa = "";
					String cariKelas = "";
					String cariRuang = "";
					Integer ekstra = null;

					boolean remedial = false;
					boolean paralel = false;
					boolean pra = false;
					StatusPertemuan statusPertemuan = null;
					boolean ujian = false;

					String day = null;

					Criteria criteria = DashboardTimelinePertemuan.initStaticCriteria(true, calendarMulai.getTime(),
							calendarSampai.getTime(), tbmuser, cari, mul, sam, bln, statusPertemuan, ke, day, tdpVideo,
							tdpAudio, tdpMateri, jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
							jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian,
							tdpDiskusi, tdpTugas, tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel, pra,
							remedial, cariMk, ekstra != null && ekstra.equals(Perkuliahan.EKSTRA), cariKelas, cariRuang,
							cariDosen, cariMahasiswa, ujian, sk, session);

					pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());
				}

				PagingApi paging = mapsPaging.get(token);
				if (paging == null) {
					paging = new PagingApi();
					mapsPaging.put(token, paging);
				}

				Integer displayPerPage = request.isNull("displayPerPage") ? 10
						: Integer.parseInt(request.getString("displayPerPage").trim());
				Integer activePage = request.isNull("activePage") ? null
						: Integer.parseInt(request.getString("activePage").trim());
				// FIX: Paging ZK dipakai di konteks API (servlet murni, TANPA ZK Desktop/Execution).
				// setPageSize/setActivePage internal memanggil Events.postEvent -> Executions.getCurrent()
				// null -> NPE. Lebih parah: setActivePage dipanggil SEBELUM totalSize diketahui (pageCount
				// masih 1), sehingga permintaan halaman >=2 ditolak WrongValueException & DIABAIKAN ->
				// pengguna SELALU dapat halaman 1. Solusi: simpan nilai lewat attribute (HashMap biasa,
				// TANPA event ZK), lalu hitung paging dgn aritmatika int polos setelah totalSize diketahui.
				paging.setAttribute("pageSizeDipakai", Integer.valueOf(displayPerPage));
				if (activePage != null) {
					paging.setAttribute("activePageDipakai", activePage);
				}

				Integer mulaiParam = request.isNull("mulaiParam") ? null
						: Integer.parseInt(request.getString("mulaiParam").trim());
				Integer sampaiParam = request.isNull("sampaiParam") ? null
						: Integer.parseInt(request.getString("sampaiParam").trim());

				if (paging != null) {
					paging.setAttribute("mulaiParam", mulaiParam);
					paging.setAttribute("sampaiParam", sampaiParam);
				}

				Session sessionStream = StreamingHibernateUtil.getInstance().openSession();
				try {

				String inPer = "";
				for (Long id : pertemuans) {
					inPer += inPer.isEmpty() ? id.toString() : "," + id;
				}

				String where = " pertemuan is not null ";
				if (Common.getApakahAdminLain(tbmuser)) {
					where = where + " and true ";
				} else if (inPer.isEmpty()) {
					where = where + " and false ";
				} else {
					where = where + " and pertemuan in (" + inPer + ") ";

				}

				if (!cari.isEmpty()) {
					where = where + " and real_file ilike '%" + cari + "%' ";
				}

				String sql = "select count(*) as size from pertemuan_file_content a where " + where + ";";

				int size = ((Number) sessionStream.createSQLQuery(sql).uniqueResult()).intValue();
				// Simpan totalSize via attribute (setTotalSize/setVisible ZK bisa memicu Events.postEvent
				// -> NPE tanpa Desktop; setVisible juga tak relevan di API karena tak ada UI).
				paging.setAttribute("totalSizeDipakai", Integer.valueOf(size));
				// Aritmatika paging (replikasi clamp ZK): pageCount = ceil(total/pageSize), activePage
				// di-clamp ke [0, pageCount-1]. Dihitung SETELAH totalSize diketahui, sehingga halaman
				// yang diminta pengguna kini benar-benar dipakai (dulu selalu jatuh ke halaman 1).
				int ukuranHalaman = displayPerPage <= 0 ? 1 : displayPerPage;
				int jumlahHalaman = ((size - 1) / ukuranHalaman) + 1;
				if (jumlahHalaman <= 0) {
					jumlahHalaman = 1;
				}
				int activePageDipakai = ambilIntAttr(paging, "activePageDipakai", 0);
				if (activePageDipakai < 0) {
					activePageDipakai = 0;
				}
				if (activePageDipakai >= jumlahHalaman) {
					activePageDipakai = jumlahHalaman - 1;
				}

				if (awal) {

					Long pointPertemuan = (Long) session.createCriteria(Pertemuan.class)
							.setProjection(Projections.property("id"))
							.add(Restrictions.sqlRestriction("date(tanggal) < CURRENT_DATE"))
							.add(pertemuans.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("id", pertemuans))
							.addOrder(Order.desc("tanggal")).setMaxResults(1).uniqueResult();
					if (pointPertemuan == null) {
						pointPertemuan = -1L;
					}

					String where1 = where + " and pertemuan < " + pointPertemuan;

					sql = "select count(*) as size from pertemuan_file_content a where " + where1 + ";";

					int page = ((Number) sessionStream.createSQLQuery(sql).uniqueResult()).intValue();

					System.out.println(
							"page -> " + page + ", size -> " + size + ", pertemuans -> " + pertemuans.size());
					// Dulu: setActivePage(v) SUDAH menyet nilai lalu NPE di postEvent -> catch menyangka
					// gagal & memanggil setActivePage(v-1) => halaman MELESET SATU. Kini clamp langsung
					// dengan aritmatika int (tanpa event ZK), hasil sama tapi benar & tanpa exception.
					int halamanAwal = (int) (page / Common.ROWS_COUNT_ON_PAGE);
					if (halamanAwal < 0) {
						halamanAwal = 0;
					}
					if (halamanAwal >= jumlahHalaman) {
						halamanAwal = jumlahHalaman - 1;
					}
					activePageDipakai = halamanAwal;
				}

				// Simpan halaman efektif agar terbawa ke pemanggilan berikutnya (cache per token).
				paging.setAttribute("activePageDipakai", Integer.valueOf(activePageDipakai));

				List<PertemuanFileContent> tugasPertemuans = sessionStream.createCriteria(PertemuanFileContent.class)
						.add(Restrictions.sqlRestriction(where)).add(Restrictions.isNotNull("pertemuan"))
						.addOrder(order.equalsIgnoreCase("asc") ? Order.asc("uploadDate") : Order.desc("uploadDate"))
						.addOrder(order.equalsIgnoreCase("asc") ? Order.asc(sort) : Order.desc(sort))
						.setMaxResults(displayPerPage)
						.setFirstResult(displayPerPage * activePageDipakai).list();

				System.out.println("pertemuanFileContent size -> " + tugasPertemuans.size() + ", pertemuans -> "
						+ pertemuans.size());

				JSONArray data = new JSONArray();
				for (PertemuanFileContent pertemuanFileContent : tugasPertemuans) {
					Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
							pertemuanFileContent.getPertemuan().toString(), true);
					if (pertemuan == null) {
						continue;
					}
					if (hanyaIdSaja != null && hanyaIdSaja.equalsIgnoreCase("true")) {

						Long idUjian = pertemuanFileContent.getId();
						Date mulaiUjian = pertemuan.getTanggal();

						if (mulaiUjian != null) {
							JSONObject jsonObjectData = new JSONObject();
							jsonObjectData.put("id", idUjian);
							jsonObjectData.put("tanggal", Common.dateFormat85.get().format(mulaiUjian));
							data.put(jsonObjectData);
						} else {
							JSONObject jsonObjectData = new JSONObject();
							jsonObjectData.put("id", idUjian);
							jsonObjectData.put("tanggal",
									Common.dateFormat85.get().format(pertemuanFileContent.getTanggal_dirubah()));
							data.put(jsonObjectData);
						}

					} else {

						PertemuanFileContent.chekScrom(pertemuanFileContent);

						JSONObject dataObject = new JSONObject();
						dataObject.put("info_pertemuan",
								displayPertemuan(pertemuan, tbmuser, servletRequest, false, false, false));
						dataObject.put("keterangan", pertemuanFileContent.getKeterangan());
						dataObject.put("keterangan_tambahan", pertemuanFileContent.getKeterangan());
						dataObject.put("gdrive", pertemuanFileContent.getGdrive());
						try {

							if (pertemuanFileContent.getLokasiFisik() != null) {
								dataObject.put("link_download", pertemuanFileContent.getLink());
								dataObject.put("link_preview", pertemuanFileContent.getLink());
								dataObject.put("nama",
										!pertemuanFileContent.getKeterangan().isEmpty()
												? pertemuanFileContent.getKeterangan()
												: pertemuanFileContent.getNama());
								dataObject.put("jenis", "link");
							} else {

								String link = pertemuanFileContent.createLinkUri(false);
								dataObject.put("nama", pertemuanFileContent.getNama());
								dataObject.put("jenis", pertemuanFileContent.getFileMimeType());
								dataObject.put("link_download", link);
								dataObject.put("link_preview", "https://docs.google.com/gview?embedded=true&url="
										+ URLEncoder.encode(link, "UTF-8"));
							}

						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2351");
							// TODO: handle exception
						}

						data.put(dataObject);
					}
				}
				tugasPertemuans.clear();
				tugasPertemuans = null;
				jsonObject.put("data", data);

				if (paging != null) {
					// Baca dari attribute (bukan getter ZK) karena nilai kini disimpan tanpa event ZK.
					jsonObject.put("pageSize", ambilIntAttr(paging, "pageSizeDipakai", Common.ROWS_COUNT_ON_PAGE));
					jsonObject.put("totalSize", ambilIntAttr(paging, "totalSizeDipakai", 0));
					jsonObject.put("activePage", ambilIntAttr(paging, "activePageDipakai", 0));
				}

				try { sessionStream.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2368");}

				sessionStream.disconnect();
				sessionStream.close();
				StreamingHibernateUtil.getInstance().closeSession();
			} finally {
				HibernateUtil.closeSessionQuietly(sessionStream);
			}

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:2390");
			}
		}
		return jsonObject;
	}

	public static JSONObject daftar_video(JSONObject request, HttpServletRequest servletRequest) {
		Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
		return daftar_video(tbmuser, request, servletRequest);
	}

	@SuppressWarnings("unchecked")
	public static JSONObject daftar_video(Tbmuser tbmuser, JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				boolean refreh = request.isNull("refresh") ? false
						: (request.get("refresh") + "").trim().equalsIgnoreCase("true");

				boolean awal = request.isNull("awal") ? false
						: request.getString("awal").trim().equalsIgnoreCase("true");

				String token = request.isNull("token") ? "------" : request.getString("token");
				String mulaiD = request.isNull("mulai") ? null : request.getString("mulai").trim();
				String sampaiD = request.isNull("sampai") ? null : request.getString("sampai").trim();
				String cari = request.isNull("cari") ? "" : request.getString("cari").trim();
				String hanyaIdSaja = request.isNull("hanyaIdSaja") ? "" : request.getString("hanyaIdSaja").trim();
				String sort = request.isNull("sort") ? "id" : request.getString("sort").trim();
				String order = request.isNull("order") ? "asc" : request.getString("order").trim();
				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.set(Calendar.YEAR, calendarMulai.get(Calendar.YEAR) - 4);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 12);

				Date mulai = calendarMulai.getTime();
				Date sampai = calendarSampai.getTime();

				if (mulaiD != null) {
					try {
						mulai = Common.dateFormat1.get().parse(mulaiD);
						calendarMulai.setTime(mulai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2436");
						// TODO: handle exception
					}
				}
				if (sampaiD != null) {
					try {
						sampai = Common.dateFormat1.get().parse(sampaiD);
						calendarSampai.setTime(sampai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2444");
						// TODO: handle exception
					}
				}

				Sekolah sk = SekolahUtil.getSekolah(servletRequest);
				Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
				Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
				List<Long> pertemuans = new ArrayList<Long>();

				Session session = HibernateUtil.openSession();
				try {
				if (mahasiswa != null) {

					if (refreh) {

						mahasiswa.reInitPertemuan(session, new Label(), calendarMulai.getTime(),
								calendarSampai.getTime());
					}

					pertemuans.addAll(mahasiswa.ambilPertemuan(session).values());

				} else if (dosen != null) {

					if (refreh) {

						dosen.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
					}

					pertemuans.addAll(dosen.ambilPertemuan(session).values());
				} else if (!Common.getApakahAdminLain(tbmuser)) {

					Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
					Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

					if (guru != null) {
						sk = guru.getSekolah();
					}
					if (siswa != null) {
						sk = siswa.getSekolah();
					}

					String mul = null;
					String sam = null;

					Integer bln = 1;
					Integer ke = null;

					boolean jadwalPerkuliahan = !(sk != null && sk.getId() != null);

					boolean jadwalPelajaran = (sk != null && sk.getId() != null);

					boolean jadwalKkn = jadwalPerkuliahan;

					boolean jadwalPkl = jadwalPerkuliahan;

					boolean jadwalKegiatan = jadwalPerkuliahan;

					boolean jadwalRevisi = jadwalPerkuliahan;
					boolean jadwalKonsultasi = jadwalPerkuliahan;

					boolean jadwalBimbingan = jadwalPerkuliahan;

					boolean jadwalKonsultasiLain = jadwalPerkuliahan;

					boolean tdpDiskusi = false;

					boolean tdpUjian = false;

					boolean tdpMateri = false;
					boolean tdpTugas = false;
					boolean tdpCatatan = false;
					boolean tdpAudio = false;
					boolean tdpVideo = false;
					boolean tdpDosenPengganti = false;

					String cariMk = "";
					String cariDosen = "";
					String cariTopik = "";
					String cariCatatan = "";
					String cariMahasiswa = "";
					String cariKelas = "";
					String cariRuang = "";
					Integer ekstra = null;

					boolean remedial = false;
					boolean paralel = false;
					boolean pra = false;
					StatusPertemuan statusPertemuan = null;
					boolean ujian = false;

					String day = null;

					Criteria criteria = DashboardTimelinePertemuan.initStaticCriteria(true, calendarMulai.getTime(),
							calendarSampai.getTime(), tbmuser, cari, mul, sam, bln, statusPertemuan, ke, day, tdpVideo,
							tdpAudio, tdpMateri, jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
							jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian,
							tdpDiskusi, tdpTugas, tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel, pra,
							remedial, cariMk, ekstra != null && ekstra.equals(Perkuliahan.EKSTRA), cariKelas, cariRuang,
							cariDosen, cariMahasiswa, ujian, sk, session);

					pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());
				}

				PagingApi paging = mapsPaging.get(token);
				if (paging == null) {
					paging = new PagingApi();
					mapsPaging.put(token, paging);
				}

				Integer displayPerPage = request.isNull("displayPerPage") ? 10
						: Integer.parseInt(request.getString("displayPerPage").trim());
				Integer activePage = request.isNull("activePage") ? null
						: Integer.parseInt(request.getString("activePage").trim());
				try {
					paging.setPageSize(displayPerPage);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2560");
				}
				try {
					if (activePage != null && paging != null) {
						paging.setActivePage(activePage);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2566");
					// TODO: handle exception
				}

				Integer mulaiParam = request.isNull("mulaiParam") ? null
						: Integer.parseInt(request.getString("mulaiParam").trim());
				Integer sampaiParam = request.isNull("sampaiParam") ? null
						: Integer.parseInt(request.getString("sampaiParam").trim());

				if (paging != null) {
					paging.setAttribute("mulaiParam", mulaiParam);
					paging.setAttribute("sampaiParam", sampaiParam);
				}

				Session sessionStream = StreamingHibernateUtil.getInstance().openSession();
				try {

				String inPer = "";
				for (Long id : pertemuans) {
					inPer += inPer.isEmpty() ? id.toString() : "," + id;
				}

				String where = " pertemuan is not null ";
				if (Common.getApakahAdminLain(tbmuser)) {
					where = where + " and true ";
				} else if (inPer.isEmpty()) {
					where = where + " and false ";
				} else {
					where = where + " and pertemuan in (" + inPer + ") ";

				}

				if (!cari.isEmpty()) {
					where = where + " and real_file ilike '%" + cari + "%' ";
				}

				String sql = "select count(*) as size from video_pertemuan a where " + where + ";";

				int size = ((Number) sessionStream.createSQLQuery(sql).uniqueResult()).intValue();
				paging.setTotalSize(size);
				paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);

				if (awal) {

					Long pointPertemuan = (Long) session.createCriteria(Pertemuan.class)
							.setProjection(Projections.property("id"))
							.add(Restrictions.sqlRestriction("date(tanggal) < CURRENT_DATE"))
							.add(pertemuans.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("id", pertemuans))
							.addOrder(Order.desc("tanggal")).setMaxResults(1).uniqueResult();
					if (pointPertemuan == null) {
						pointPertemuan = -1L;
					}

					String where1 = where + " and pertemuan < " + pointPertemuan;

					sql = "select count(*) as size from video_pertemuan a where " + where1 + ";";

					int page = ((Number) sessionStream.createSQLQuery(sql).uniqueResult()).intValue();

					try {
						System.out.println(
								"page -> " + page + ", size -> " + size + ", pertemuans -> " + pertemuans.size());
						paging.setActivePage((int) (page / Common.ROWS_COUNT_ON_PAGE));
					} catch (Exception e) {
						try {

							paging.setActivePage(((int) (page / Common.ROWS_COUNT_ON_PAGE)) - 1);
						} catch (Exception ae) { ais.common.ErrorAuditUtil.record(ae, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2634");
							// TODO: handle exception
						}
					}
				}

				List<VideoPertemuan> tugasPertemuans = sessionStream.createCriteria(VideoPertemuan.class)
						.add(Restrictions.sqlRestriction(where)).add(Restrictions.isNotNull("pertemuan"))
						.addOrder(order.equalsIgnoreCase("asc") ? Order.asc("tanggal_dirubah")
								: Order.desc("tanggal_dirubah"))
						.addOrder(order.equalsIgnoreCase("asc") ? Order.asc(sort) : Order.desc(sort))
						.setMaxResults(displayPerPage)
						.setFirstResult(displayPerPage * (paging == null ? 0 : paging.getActivePage())).list();

				JSONArray data = new JSONArray();
				for (VideoPertemuan videoPertemuan : tugasPertemuans) {

					Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
							videoPertemuan.getPertemuan().toString(), true);
					if (pertemuan == null) {
						continue;
					}
					if (hanyaIdSaja != null && hanyaIdSaja.equalsIgnoreCase("true")) {

						Long idUjian = videoPertemuan.getId();
						Date mulaiUjian = pertemuan.getTanggal();

						if (mulaiUjian != null) {
							JSONObject jsonObjectData = new JSONObject();
							jsonObjectData.put("id", idUjian);
							jsonObjectData.put("tanggal", Common.dateFormat85.get().format(mulaiUjian));
							data.put(jsonObjectData);
						} else {
							JSONObject jsonObjectData = new JSONObject();
							jsonObjectData.put("id", idUjian);
							jsonObjectData.put("tanggal",
									Common.dateFormat85.get().format(videoPertemuan.getTanggal_dirubah()));
							data.put(jsonObjectData);
						}

					} else {
						JSONObject dataObject = new JSONObject();
						dataObject.put("info_pertemuan",
								displayPertemuan(pertemuan, tbmuser, servletRequest, false, false, false));
						dataObject.put("nama", videoPertemuan.getNama());
						dataObject.put("jenis", videoPertemuan.getType());
						dataObject.put("keterangan", videoPertemuan.getKeterangan());
						dataObject.put("keterangan_tambahan", videoPertemuan.getKeteranganTambahan());
						dataObject.put("gdrive", videoPertemuan.getGdrive());
						try {
							String link = videoPertemuan.createLinkUri(false);
							dataObject.put("link_download", link);
							dataObject.put("link_preview", "https://docs.google.com/gview?embedded=true&url="
									+ URLEncoder.encode(link, "UTF-8"));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2688");
							// TODO: handle exception
						}

						data.put(dataObject);
					}
				}

				jsonObject.put("data", data);

				if (paging != null) {
					jsonObject.put("pageSize", paging.getPageSize());
					jsonObject.put("totalSize", paging.getTotalSize());
					jsonObject.put("activePage", paging.getActivePage());
				}

				tugasPertemuans.clear();
				tugasPertemuans = null;

				try { sessionStream.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2707");}

				sessionStream.disconnect();
				sessionStream.close();
				StreamingHibernateUtil.getInstance().closeSession();
			} finally {
				HibernateUtil.closeSessionQuietly(sessionStream);
			}

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:2729");
			}
		}
		return jsonObject;
	}

	public static JSONObject daftar_audio(JSONObject request, HttpServletRequest servletRequest) {
		Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
		return daftar_audio(tbmuser, request, servletRequest);
	}

	@SuppressWarnings("unchecked")
	public static JSONObject daftar_audio(Tbmuser tbmuser, JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				boolean refreh = request.isNull("refresh") ? false
						: (request.get("refresh") + "").trim().equalsIgnoreCase("true");

				boolean awal = request.isNull("awal") ? false
						: request.getString("awal").trim().equalsIgnoreCase("true");

				String token = request.isNull("token") ? "------" : request.getString("token");
				String mulaiD = request.isNull("mulai") ? null : request.getString("mulai").trim();
				String sampaiD = request.isNull("sampai") ? null : request.getString("sampai").trim();
				String cari = request.isNull("cari") ? "" : request.getString("cari").trim();
				String hanyaIdSaja = request.isNull("hanyaIdSaja") ? "" : request.getString("hanyaIdSaja").trim();
				String sort = request.isNull("sort") ? "id" : request.getString("sort").trim();
				String order = request.isNull("order") ? "asc" : request.getString("order").trim();
				Calendar calendarMulai = Calendar.getInstance();
				calendarMulai.set(Calendar.YEAR, calendarMulai.get(Calendar.YEAR) - 4);

				Calendar calendarSampai = Calendar.getInstance();
				calendarSampai.set(Calendar.MONTH, calendarMulai.get(Calendar.MONTH) + 12);

				Date mulai = calendarMulai.getTime();
				Date sampai = calendarSampai.getTime();

				if (mulaiD != null) {
					try {
						mulai = Common.dateFormat1.get().parse(mulaiD);
						calendarMulai.setTime(mulai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2775");
						// TODO: handle exception
					}
				}
				if (sampaiD != null) {
					try {
						sampai = Common.dateFormat1.get().parse(sampaiD);
						calendarSampai.setTime(sampai);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2783");
						// TODO: handle exception
					}
				}

				Sekolah sk = SekolahUtil.getSekolah(servletRequest);
				Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
				Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
				List<Long> pertemuans = new ArrayList<Long>();

				Session session = HibernateUtil.openSession();
				try {
				if (mahasiswa != null) {

					if (refreh) {

						mahasiswa.reInitPertemuan(session, new Label(), calendarMulai.getTime(),
								calendarSampai.getTime());
					}

					pertemuans.addAll(mahasiswa.ambilPertemuan(session).values());

				} else if (dosen != null) {

					if (refreh) {

						dosen.reInitPertemuan(session, new Label(), calendarMulai.getTime(), calendarSampai.getTime());
					}

					pertemuans.addAll(dosen.ambilPertemuan(session).values());
				} else if (!Common.getApakahAdminLain(tbmuser)) {

					Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
					Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

					if (guru != null) {
						sk = guru.getSekolah();
					}
					if (siswa != null) {
						sk = siswa.getSekolah();
					}

					String mul = null;
					String sam = null;

					Integer bln = 1;
					Integer ke = null;

					boolean jadwalPerkuliahan = !(sk != null && sk.getId() != null);

					boolean jadwalPelajaran = (sk != null && sk.getId() != null);

					boolean jadwalKkn = jadwalPerkuliahan;

					boolean jadwalPkl = jadwalPerkuliahan;

					boolean jadwalKegiatan = jadwalPerkuliahan;

					boolean jadwalRevisi = jadwalPerkuliahan;
					boolean jadwalKonsultasi = jadwalPerkuliahan;

					boolean jadwalBimbingan = jadwalPerkuliahan;

					boolean jadwalKonsultasiLain = jadwalPerkuliahan;

					boolean tdpDiskusi = false;

					boolean tdpUjian = false;

					boolean tdpMateri = false;
					boolean tdpTugas = false;
					boolean tdpCatatan = false;
					boolean tdpAudio = false;
					boolean tdpVideo = false;
					boolean tdpDosenPengganti = false;

					String cariMk = "";
					String cariDosen = "";
					String cariTopik = "";
					String cariCatatan = "";
					String cariMahasiswa = "";
					String cariKelas = "";
					String cariRuang = "";
					Integer ekstra = null;

					boolean remedial = false;
					boolean paralel = false;
					boolean pra = false;
					StatusPertemuan statusPertemuan = null;
					boolean ujian = false;

					String day = null;

					Criteria criteria = DashboardTimelinePertemuan.initStaticCriteria(true, calendarMulai.getTime(),
							calendarSampai.getTime(), tbmuser, cari, mul, sam, bln, statusPertemuan, ke, day, tdpVideo,
							tdpAudio, tdpMateri, jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
							jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian,
							tdpDiskusi, tdpTugas, tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel, pra,
							remedial, cariMk, ekstra != null && ekstra.equals(Perkuliahan.EKSTRA), cariKelas, cariRuang,
							cariDosen, cariMahasiswa, ujian, sk, session);

					pertemuans.addAll(criteria.setProjection(Projections.property("id")).setMaxResults(32766).list());
				}

				PagingApi paging = mapsPaging.get(token);
				if (paging == null) {
					paging = new PagingApi();
					mapsPaging.put(token, paging);
				}

				Integer displayPerPage = request.isNull("displayPerPage") ? 10
						: Integer.parseInt(request.getString("displayPerPage").trim());
				Integer activePage = request.isNull("activePage") ? null
						: Integer.parseInt(request.getString("activePage").trim());
				try {
					paging.setPageSize(displayPerPage);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2899");
				}
				try {
					if (activePage != null && paging != null) {
						paging.setActivePage(activePage);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2905");
					// TODO: handle exception
				}

				Integer mulaiParam = request.isNull("mulaiParam") ? null
						: Integer.parseInt(request.getString("mulaiParam").trim());
				Integer sampaiParam = request.isNull("sampaiParam") ? null
						: Integer.parseInt(request.getString("sampaiParam").trim());

				if (paging != null) {
					paging.setAttribute("mulaiParam", mulaiParam);
					paging.setAttribute("sampaiParam", sampaiParam);
				}

				Session sessionStream = StreamingHibernateUtil.getInstance().openSession();
				try {

				String inPer = "";
				for (Long id : pertemuans) {
					inPer += inPer.isEmpty() ? id.toString() : "," + id;
				}

				String where = " pertemuan is not null ";
				if (Common.getApakahAdminLain(tbmuser)) {
					where = where + " and true ";
				} else if (inPer.isEmpty()) {
					where = where + " and false ";
				} else {
					where = where + " and pertemuan in (" + inPer + ") ";

				}

				if (!cari.isEmpty()) {
					where = where + " and real_file ilike '%" + cari + "%' ";
				}

				String sql = "select count(*) as size from audio_pertemuan a where " + where + ";";

				int size = ((Number) sessionStream.createSQLQuery(sql).uniqueResult()).intValue();
				paging.setTotalSize(size);
				paging.setVisible(size > Common.ROWS_COUNT_ON_PAGE);

				if (awal) {

					Long pointPertemuan = (Long) session.createCriteria(Pertemuan.class)
							.setProjection(Projections.property("id"))
							.add(Restrictions.sqlRestriction("date(tanggal) < CURRENT_DATE"))
							.add(pertemuans.isEmpty() ? Restrictions.sqlRestriction("false")
									: Restrictions.in("id", pertemuans))
							.addOrder(Order.desc("tanggal")).setMaxResults(1).uniqueResult();
					if (pointPertemuan == null) {
						pointPertemuan = -1L;
					}

					String where1 = where + " and pertemuan < " + pointPertemuan;

					sql = "select count(*) as size from audio_pertemuan a where " + where1 + ";";

					int page = ((Number) sessionStream.createSQLQuery(sql).uniqueResult()).intValue();

					try {
						System.out.println(
								"page -> " + page + ", size -> " + size + ", pertemuans -> " + pertemuans.size());
						paging.setActivePage((int) (page / Common.ROWS_COUNT_ON_PAGE));
					} catch (Exception e) {
						try {

							paging.setActivePage(((int) (page / Common.ROWS_COUNT_ON_PAGE)) - 1);
						} catch (Exception ae) { ais.common.ErrorAuditUtil.record(ae, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:2973");
							// TODO: handle exception
						}
					}
				}

				List<AudioPertemuan> tugasPertemuans = sessionStream.createCriteria(AudioPertemuan.class)
						.add(Restrictions.sqlRestriction(where)).add(Restrictions.isNotNull("pertemuan"))
						.addOrder(order.equalsIgnoreCase("asc") ? Order.asc("tanggal_dirubah")
								: Order.desc("tanggal_dirubah"))
						.addOrder(order.equalsIgnoreCase("asc") ? Order.asc(sort) : Order.desc(sort))
						.setMaxResults(displayPerPage)
						.setFirstResult(displayPerPage * (paging == null ? 0 : paging.getActivePage())).list();

				JSONArray data = new JSONArray();
				for (AudioPertemuan audioPertemuan : tugasPertemuans) {

					Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
							audioPertemuan.getPertemuan().toString(), true);
					if (pertemuan == null) {
						continue;
					}

					if (hanyaIdSaja != null && hanyaIdSaja.equalsIgnoreCase("true")) {

						Long idUjian = audioPertemuan.getId();
						Date mulaiUjian = pertemuan.getTanggal();

						if (mulaiUjian != null) {
							JSONObject jsonObjectData = new JSONObject();
							jsonObjectData.put("id", idUjian);
							jsonObjectData.put("tanggal", Common.dateFormat85.get().format(mulaiUjian));
							data.put(jsonObjectData);
						} else {
							JSONObject jsonObjectData = new JSONObject();
							jsonObjectData.put("id", idUjian);
							jsonObjectData.put("tanggal",
									Common.dateFormat85.get().format(audioPertemuan.getTanggal_dirubah()));
							data.put(jsonObjectData);
						}

					} else {

						JSONObject dataObject = new JSONObject();
						dataObject.put("info_pertemuan",
								displayPertemuan(pertemuan, tbmuser, servletRequest, false, false, false));
						dataObject.put("nama", audioPertemuan.getNama());
						dataObject.put("jenis", audioPertemuan.getType());
						dataObject.put("keterangan", audioPertemuan.getKeterangan());
						dataObject.put("keterangan_tambahan", audioPertemuan.getKeteranganTambahan());
						dataObject.put("gdrive", audioPertemuan.getGdrive());
						try {
							String link = audioPertemuan.createLinkUri(false);
							dataObject.put("link_download", link);
							dataObject.put("link_preview", "https://docs.google.com/gview?embedded=true&url="
									+ URLEncoder.encode(link, "UTF-8"));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:3029");
							// TODO: handle exception
						}

						data.put(dataObject);
					}
				}

				jsonObject.put("data", data);

				if (paging != null) {
					jsonObject.put("pageSize", paging.getPageSize());
					jsonObject.put("totalSize", paging.getTotalSize());
					jsonObject.put("activePage", paging.getActivePage());
				}

				tugasPertemuans.clear();
				tugasPertemuans = null;

				try { sessionStream.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:3048");}

				sessionStream.disconnect();
				sessionStream.close();
				StreamingHibernateUtil.getInstance().closeSession();
			} finally {
				HibernateUtil.closeSessionQuietly(sessionStream);
			}

				// session.disconnect();
				ApiHelperSupport.closeOpenedSession(session);
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
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:3070");
			}
		}
		return jsonObject;
	}

	public static JSONObject linimasa(JSONObject request, HttpServletRequest servletRequest) {
		Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
		return linimasa(tbmuser, request, servletRequest);
	}

	@SuppressWarnings("unchecked")
	public static JSONObject linimasa(Tbmuser tbmuser, JSONObject request, HttpServletRequest servletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
			} else {
				String token = request.isNull("token") ? "------" : request.getString("token");
				Sekolah sk = SekolahUtil.getSekolah(servletRequest);
				Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
				Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

				Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
				Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();

				if (guru != null) {
					sk = guru.getSekolah();
				}
				if (siswa != null) {
					sk = siswa.getSekolah();
				}

				String mul = request.isNull("mulai") ? null : request.getString("mulai").trim();
				String sam = request.isNull("sampai") ? null : request.getString("sampai").trim();

				Integer bln = request.isNull("sd") ? 1 : Integer.parseInt(request.getString("sd").trim());
				Integer ke = request.isNull("ke") ? null : Integer.parseInt(request.getString("ke").trim());

				boolean refresh = request.isNull("refresh") ? false
						: (request.get("refresh") + "").trim().equalsIgnoreCase("true");

				boolean jadwalPerkuliahan = request.isNull("jadwalPerkuliahan") ? true
						: request.getString("jadwalPerkuliahan").trim().equalsIgnoreCase("true");

				boolean jadwalPelajaran = request.isNull("jadwalPelajaran") ? true
						: request.getString("jadwalPelajaran").trim().equalsIgnoreCase("true");

				boolean jadwalKkn = request.isNull("jadwalKkn") ? true
						: request.getString("jadwalKkn").trim().equalsIgnoreCase("true");

				boolean jadwalPkl = request.isNull("jadwalPkl") ? true
						: request.getString("jadwalPkl").trim().equalsIgnoreCase("true");

				boolean jadwalKegiatan = request.isNull("jadwalKegiatan") ? true
						: request.getString("jadwalKegiatan").trim().equalsIgnoreCase("true");

				boolean wisuda = request.isNull("wisuda") ? true
						: request.getString("wisuda").trim().equalsIgnoreCase("true");

				boolean jadwalRevisi = request.isNull("jadwalRevisi") ? true
						: request.getString("jadwalRevisi").trim().equalsIgnoreCase("true");
				boolean jadwalKonsultasi = request.isNull("jadwalKonsultasi") ? true
						: request.getString("jadwalKonsultasi").trim().equalsIgnoreCase("true");

				boolean jadwalBimbingan = request.isNull("jadwalBimbingan") ? true
						: request.getString("jadwalBimbingan").trim().equalsIgnoreCase("true");

				boolean jadwalKonsultasiLain = request.isNull("jadwalKonsultasiLain") ? true
						: request.getString("jadwalKonsultasiLain").trim().equalsIgnoreCase("true");

				boolean tdpDiskusi = request.isNull("tdpDiskusi") ? false
						: request.getString("tdpDiskusi").trim().equalsIgnoreCase("true");

				boolean tdpUjian = request.isNull("tdpUjian") ? false
						: request.getString("tdpUjian").trim().equalsIgnoreCase("true");

				String cariNamaUjian = request.isNull("cariNamaUjian") ? "" : request.getString("cariNamaUjian").trim();

				boolean tdpMateri = request.isNull("tdpMateri") ? false
						: request.getString("tdpMateri").trim().equalsIgnoreCase("true");
				boolean tdpTugas = request.isNull("tdpTugas") ? false
						: request.getString("tdpTugas").trim().equalsIgnoreCase("true");
				boolean tdpCatatan = request.isNull("tdpCatatan") ? false
						: request.getString("tdpCatatan").trim().equalsIgnoreCase("true");
				boolean tdpAudio = request.isNull("tdpAudio") ? false
						: request.getString("tdpAudio").trim().equalsIgnoreCase("true");
				boolean tdpVideo = request.isNull("tdpVideo") ? false
						: request.getString("tdpVideo").trim().equalsIgnoreCase("true");
				boolean tdpDosenPengganti = request.isNull("tdpDosenPengganti") ? false
						: request.getString("tdpDosenPengganti").trim().equalsIgnoreCase("true");

				boolean remedial = request.isNull("remedial") ? false
						: request.getString("remedial").trim().equalsIgnoreCase("true");
				boolean paralel = request.isNull("paralel") ? false
						: request.getString("paralel").trim().equalsIgnoreCase("true");
				boolean tdpOnline = request.isNull("tdpOnline") ? false
						: request.getString("tdpOnline").trim().equalsIgnoreCase("true");
				boolean pra = request.isNull("pra") ? false : request.getString("pra").trim().equalsIgnoreCase("true");

				boolean hanyaIdSaja = request.isNull("hanyaIdSaja") ? false
						: request.getString("hanyaIdSaja").trim().equalsIgnoreCase("true");

				String cariMk = request.isNull("cariMk") ? "" : request.getString("cariMk").trim();
				String cariDosen = request.isNull("cariDosen") ? "" : request.getString("cariDosen").trim();
				String cariTopik = request.isNull("cariTopik") ? "" : request.getString("cariTopik").trim();
				String cariCatatan = request.isNull("cariCatatan") ? "" : request.getString("cariCatatan").trim();
				String hari = request.isNull("hari") ? null : request.getString("hari").trim();

				String cariMahasiswa = request.isNull("cariMahasiswa") ? "" : request.getString("cariMahasiswa").trim();
				String cariKelas = request.isNull("cariKelas") ? "" : request.getString("cariKelas").trim();
				String cariRuang = request.isNull("cariRuang") ? "" : request.getString("cariRuang").trim();

				String order = request.isNull("order") ? "asc" : request.getString("order").trim();

				Integer ekstra = request.isNull("ekstra") ? null : Integer.parseInt(request.getString("ekstra").trim());

				StatusPertemuan statusPertemuan = request.isNull("statusPertemuan") ? null
						: new StatusPertemuan(Long.parseLong(request.getString("statusPertemuan").trim()));
				PagingApi paging = mapsPaging.get(token);
				if (paging == null) {
					paging = new PagingApi();
					mapsPaging.put(token, paging);
				}

				Integer displayPerPage = request.isNull("displayPerPage") ? 10
						: Integer.parseInt(request.getString("displayPerPage").trim());
				Integer activePage = request.isNull("activePage") ? null
						: Integer.parseInt(request.getString("activePage").trim());
				// paging.setActivePage(...) milik ZK memanggil Events.postEvent(...)
				// yang mensyaratkan ada Execution ZK yang hidup - servlet REST API ini
				// tidak punya itu sama sekali, sehingga selalu berakhir
				// NullPointerException (lihat Mahasiswa.ambilPertemuan). Untuk cabang
				// mahasiswa, permintaan activePage cukup disimpan lewat attribute bag
				// komponen (aman, tidak memicu event ZK apa pun) - dibaca ulang &
				// di-clamp sendiri oleh Mahasiswa.ambilPertemuan tanpa menyentuh
				// setter ZK yang crash tsb sama sekali. Panggilan setter ZK asli
				// TETAP dipertahankan (dibungkus try/catch spt semula) hanya untuk
				// selain mahasiswa (mis. dosen.ambilPertemuan yang masih membaca
				// paging.getActivePage() langsung) supaya perilakunya tidak berubah.
				if (activePage != null && paging != null) {
					paging.setAttribute("activePageDipakai", activePage);
				}
				if (mahasiswa == null) {
					try {
						if (activePage != null && paging != null) {
							paging.setActivePage(activePage);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:3205");
						// TODO: handle exception
					}
				}

				Integer mulaiParam = request.isNull("mulaiParam") ? null
						: Integer.parseInt(request.getString("mulaiParam").trim());
				Integer sampaiParam = request.isNull("sampaiParam") ? null
						: Integer.parseInt(request.getString("sampaiParam").trim());

				if (paging != null) {
					paging.setAttribute("mulaiParam", mulaiParam);
					paging.setAttribute("sampaiParam", sampaiParam);
				}

				jsonObject.put("status", "00");
				jsonObject.put("description", "Pengambilan data berhasil");

				Calendar[] cc = DashboardTimelinePertemuan.plusMinus(bln);
				final Calendar calendarPlus = cc[0];
				final Calendar calendarMinus = cc[1];

				boolean jadikanAwal = false;
				List<Long> pertemuans = null;
				if (dosen != null) {

					if (!dosen.udah("dosen_buka_elearning_" + bln + "_"
							+ Common.dateFormatWeek.get().format(ais.ui.util.WaktuUtil.getDate())) || refresh) {
						Label label = new Label();
						Session session = HibernateUtil.openSession();
						try {
						try {
							dosen.reInitPertemuan(session, label, calendarPlus, calendarMinus);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:3238");
						}
						label.setValue("");
						// session.disconnect();
						ApiHelperSupport.closeOpenedSession(session);
						} finally {
							HibernateUtil.closeSessionQuietly(session);
						}
					}

					TreeMap<String, Long> pertemuansa = mapsLinimasa.get(token);
					if (pertemuansa == null || refresh) {
						jadikanAwal = true;
						Session sessAmbil3 = HibernateUtil.openSession();
						try {
							pertemuansa = dosen.ambilPertemuan(sessAmbil3);
						} finally {
							HibernateUtil.closeSessionQuietly(sessAmbil3);
						}
						mapsLinimasa.put(token, pertemuansa);
					}

					pertemuans = dosen.ambilPertemuan(pertemuansa, jadwalPerkuliahan, jadwalKkn, jadwalPkl,
							jadwalKegiatan, jadwalRevisi, jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain,
							tdpDiskusi, tdpUjian, cariNamaUjian, tdpMateri, tdpTugas, tdpCatatan, tdpAudio, tdpVideo,
							tdpDosenPengganti, ais.ui.util.WaktuUtil.getDate(), cariMk, cariDosen, mul, sam,

							cariTopik, cariCatatan, hari, cariMahasiswa, cariKelas, cariRuang, pra, ekstra, remedial,
							paralel, tdpOnline, statusPertemuan, ke, paging, refresh || jadikanAwal, displayPerPage,
							new MyToolbarbuttonConfig(), tbmuser, order);

					JSONArray array = new JSONArray();
					for (Long pertemuanid : pertemuans) {
						Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
								pertemuanid.toString(), true);
						if (pertemuan != null) {

							if (refresh) {
								pertemuan.refreshData();
							}

							if (hanyaIdSaja) {
								JSONObject jsonObjectData = new JSONObject();
								jsonObjectData.put("id", pertemuan.getId());
								jsonObjectData.put("tanggal", Common.dateFormat85.get().format(pertemuan.getTanggal()));
								array.put(jsonObjectData);
							} else {

								array.put(displayPertemuan(pertemuan, tbmuser, servletRequest));
							}
						}
					}
					jsonObject.put("data", array);
					pertemuans.clear();
					pertemuans = null;

				}

				else if (mahasiswa != null) {

					if (!mahasiswa.udah("mahasiswa_buka_elearning_" + bln + "_"
							+ Common.dateFormatWeek.get().format(ais.ui.util.WaktuUtil.getDate())) || refresh) {
						Label label = new Label();
						Session session = HibernateUtil.openSession();
						try {
						try {
							mahasiswa.reInitPertemuan(session, label, calendarPlus, calendarMinus);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:3306");
						}
						label.setValue("");
						// session.disconnect();
						ApiHelperSupport.closeOpenedSession(session);
						} finally {
							HibernateUtil.closeSessionQuietly(session);
						}
					}

					TreeMap<String, Long> pertemuansa = mapsLinimasa.get(token);
					if (pertemuansa == null || refresh) {
						jadikanAwal = true;
						Session sessAmbil4 = HibernateUtil.openSession();
						try {
							pertemuansa = mahasiswa.ambilPertemuan(sessAmbil4);
						} finally {
							HibernateUtil.closeSessionQuietly(sessAmbil4);
						}
						mapsLinimasa.put(token, pertemuansa);
					}

					pertemuans = mahasiswa.ambilPertemuan(pertemuansa, jadwalPerkuliahan, jadwalKkn, jadwalPkl,
							jadwalKegiatan, wisuda, jadwalRevisi, jadwalKonsultasi, jadwalBimbingan,
							jadwalKonsultasiLain, tdpDiskusi, tdpUjian, cariNamaUjian, tdpMateri, tdpTugas, tdpCatatan,
							tdpAudio, tdpVideo, tdpDosenPengganti, ais.ui.util.WaktuUtil.getDate(), cariMk, cariDosen,
							mul, sam,

							cariTopik, cariCatatan, hari, cariKelas, cariRuang, pra, ekstra, remedial, paralel,
							tdpOnline, statusPertemuan, ke, paging, refresh || jadikanAwal, displayPerPage,
							new MyToolbarbuttonConfig(), tbmuser, order);

					JSONArray array = new JSONArray();
					for (Long pertemuanid : pertemuans) {
						Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
								pertemuanid.toString(), true);
						if (pertemuan != null) {

							if (refresh) {
								pertemuan.refreshData();
							}

							if (hanyaIdSaja) {
								JSONObject jsonObjectData = new JSONObject();
								jsonObjectData.put("id", pertemuan.getId());
								jsonObjectData.put("tanggal", Common.dateFormat85.get().format(pertemuan.getTanggal()));
								array.put(jsonObjectData);
							} else {

								array.put(displayPertemuan(pertemuan, tbmuser, servletRequest));
							}
						}
					}
					jsonObject.put("data", array);
					pertemuans.clear();
					pertemuans = null;

				} else {
					TreeMap<String, Long> pertemuansa = mapsLinimasa.get(token);
					if (refresh || jadikanAwal || pertemuansa == null) {
						Date mulai = null;
						Date sampai = null;
						boolean ujian = false;

						String day = hari == null ? null
								: hari.equals(Common.haris[0]) ? "sunday"
										: hari.equals(Common.haris[1]) ? "monday"
												: hari.equals(Common.haris[2]) ? "tuesday"
														: hari.equals(Common.haris[3]) ? "wednesday"
																: hari.equals(Common.haris[4]) ? "thursday"
																		: hari.equals(Common.haris[5]) ? "friday"
																				: "saturday";
						Session session = HibernateUtil.openSession();
						try {
						Criteria criteria = DashboardTimelinePertemuan.initStaticCriteria(true, mulai, sampai, tbmuser,
								cariNamaUjian, mul, sam, bln, statusPertemuan, ke, day, tdpVideo, tdpAudio, tdpMateri,
								jadwalPerkuliahan, jadwalPelajaran, jadwalKkn, jadwalPkl, jadwalRevisi,
								jadwalKonsultasi, jadwalBimbingan, jadwalKonsultasiLain, jadwalKegiatan, tdpUjian,
								tdpDiskusi, tdpTugas, tdpCatatan, tdpDosenPengganti, cariTopik, cariCatatan, paralel,
								pra, remedial, cariMk, ekstra != null && ekstra.equals(Perkuliahan.EKSTRA), cariKelas,
								cariRuang, cariDosen, cariMahasiswa, ujian, sk, session);

						List<Long> pertemuansaLocal = criteria.setProjection(Projections.property("id")).list();

						pertemuansa = order.equalsIgnoreCase("asc") ? new TreeMap<String, Long>()
								: new TreeMap<String, Long>(Collections.reverseOrder());

						List<Long> idYgBelumkemabil = new ArrayList<Long>();
						for (Long idPertemuan : pertemuansaLocal) {
							Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
									idPertemuan.toString(), true);
							if (pertemuan == null) {
								idYgBelumkemabil.add(idPertemuan);
							} else {
								pertemuansa.put(
										Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
										pertemuan.getId());
							}

						}
						// session.disconnect();
						ApiHelperSupport.closeOpenedSession(session);

						if (!idYgBelumkemabil.isEmpty()) {
							HibernateUtil.closeSessionQuietly(session); // tutup session lama sebelum buka ulang (konversi openSession)
							session = HibernateUtil.openSession();
							List<Pertemuan> pertemuansaa = session.createCriteria(Pertemuan.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.in("id", idYgBelumkemabil)).list();
							for (Pertemuan pertemuan : pertemuansaa) {
								GeneralValueObject.masukkanData(Pertemuan.class, pertemuan);
								pertemuansa.put(
										Common.dateFormat8.get().format(pertemuan.getTanggal()) + "_" + pertemuan.getId(),
										pertemuan.getId());
							}
							// session.disconnect();
							ApiHelperSupport.closeOpenedSession(session);
						}

						mapsLinimasa.put(token, pertemuansa);

						int size = pertemuansaLocal.size();
						if (paging != null) {
							paging.setTotalSize(size);
						}
						int tengahTengah = 0;
						Date tanggalSekarang = WaktuUtil.getDate();
						String format = Common.dateFormat8.get().format(tanggalSekarang);
						for (String a : pertemuansa.keySet()) {
							try {
								String s = a.split("_")[0];
								if (format.equals(s)) {
									break;
								}
								Date tgl = Common.dateFormat8.get().parse(s);
								if (tgl.before(tanggalSekarang)) {
									tengahTengah = tengahTengah + 1;
								} else {
									tengahTengah = tengahTengah + 1;
									break;
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:3448");
							}
						}

						if (paging != null) {
							try {
								activePage = (int) (tengahTengah / displayPerPage);
								paging.setActivePage(activePage);
							} catch (Exception e) {
								try {
									activePage = (int) (tengahTengah / displayPerPage);
									paging.setActivePage(activePage - 1);
								} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/servlet/api/LinimasaApi.java:3460");
//									e.printStackTrace();
								}
							}
						}
						} finally {
							HibernateUtil.closeSessionQuietly(session);
						}
					}

					int mulai = paging == null ? 0 : displayPerPage * paging.getActivePage();
					int banyak = displayPerPage;

					JSONArray array = new JSONArray();

					int index = 0;
					for (Long pertemuanid : pertemuansa.values()) {
						if (index >= mulai && index < (mulai + banyak)) {
							Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class,
									pertemuanid.toString(), true);
							if (pertemuan != null) {
								if (refresh) {
									pertemuan.refreshData();
								}
								if (hanyaIdSaja) {
									JSONObject jsonObjectData = new JSONObject();
									jsonObjectData.put("id", pertemuan.getId());
									jsonObjectData.put("tanggal", Common.dateFormat85.get().format(pertemuan.getTanggal()));
									array.put(jsonObjectData);
								} else {

									array.put(displayPertemuan(pertemuan, tbmuser, servletRequest));
								}
							}
						}
						index++;
					}
					jsonObject.put("data", array);
				}

				if (paging != null) {
					// displayPerPage adalah sumber kebenaran page-size di semua cabang
					// (dosen/mahasiswa/lainnya) - dipakai langsung drpd paging.getPageSize()
					// karena utk cabang mahasiswa setter ZK-nya sengaja tidak disentuh lagi
					// (lihat Mahasiswa.ambilPertemuan) sehingga field internal Paging tsb
					// tidak lagi ter-update.
					jsonObject.put("pageSize", displayPerPage);
					jsonObject.put("totalSize", paging.getTotalSize());
					if (dosen == null && mahasiswa != null) {
						// Cabang mahasiswa menyimpan activePage yang benar2 dipakai lewat
						// attribute bag (aman thd Events.postEvent), bukan lewat field asli.
						Object activePageDipakaiFinal = paging.getAttribute("activePageDipakai");
						jsonObject.put("activePage",
								activePageDipakaiFinal instanceof Integer ? (Integer) activePageDipakaiFinal : 0);
					} else {
						jsonObject.put("activePage", paging.getActivePage());
					}
				}
			}

		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/LinimasaApi.java:3513");
			}
		}
		return jsonObject;
	}

}
