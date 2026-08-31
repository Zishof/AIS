package ais.action.master.feeder.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Node;

import ais.action.master.resources.FeederResource;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.Perkuliahan;

/**
 * Helper terfokus untuk feeder exporter. Tipe ini membungkus satu variasi kecil dari alur yang
 * lebih umum agar pemanggil memakai nama domain yang jelas dan tidak menggandakan implementasi.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah mutasi data ({@code simpanFeederIdDenganNativeSession()});
 * operasi domain lain ({@code dosen_pt()}, {@code ajar_dosen()}, {@code ajar_dosen()}, {@code
 * tutupSessionKhusus()}, {@code mahasiswa_pt()}, {@code kuliah_mahasiswa()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 */
public class FeederExporterHelper {

	public static void dosen_pt(Session session, FeederConnector feederConnector, String token,
			PenugasanDosenMengajar penugasanDosenMengajar) {
		Dosen dosen = penugasanDosenMengajar.getDosen();

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id_ptk", dosen.getFeeder());
		data.put("id_sp", dosen.getPerguruanTinggi().getFeeder());
		data.put("id_thn_ajaran", penugasanDosenMengajar.getTahun());
		data.put("id_sms", penugasanDosenMengajar.getJurusan().getFeeder());
		data.put("no_srt_tgs", penugasanDosenMengajar.getKode());
		data.put("tgl_srt_tgs", penugasanDosenMengajar.getKode());

		int bulan = -1;
		if (penugasanDosenMengajar.getTanggalSuratTugas() != null) {
			data.put("tgl_srt_tgs", Common.databaseDateFormat.get().format(penugasanDosenMengajar.getTanggalSuratTugas()));
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(penugasanDosenMengajar.getTanggalSuratTugas());
			bulan = calendar.get(Calendar.MONTH) + 1;
		}
		if (penugasanDosenMengajar.getTmtSuratTugas() != null) {
			data.put("tmt_srt_tgs", Common.databaseDateFormat.get().format(penugasanDosenMengajar.getTmtSuratTugas()));
		}

		if (dosen.getJurusan() != null) {
			data.put("a_sp_homebase",
					penugasanDosenMengajar.getJurusan().getId().equals(dosen.getJurusan().getId()) ? 1 : 0);
		}

		data.put("a_aktif_bln_1", bulan == 1 ? 1 : 0);
		data.put("a_aktif_bln_2", bulan == 2 ? 1 : 0);
		data.put("a_aktif_bln_3", bulan == 3 ? 1 : 0);
		data.put("a_aktif_bln_4", bulan == 4 ? 1 : 0);
		data.put("a_aktif_bln_5", bulan == 5 ? 1 : 0);
		data.put("a_aktif_bln_6", bulan == 6 ? 1 : 0);
		data.put("a_aktif_bln_7", bulan == 7 ? 1 : 0);
		data.put("a_aktif_bln_8", bulan == 8 ? 1 : 0);
		data.put("a_aktif_bln_9", bulan == 9 ? 1 : 0);
		data.put("a_aktif_bln_10", bulan == 10 ? 1 : 0);
		data.put("a_aktif_bln_11", bulan == 11 ? 1 : 0);
		data.put("a_aktif_bln_12", bulan == 12 ? 1 : 0);

		try {

			if (penugasanDosenMengajar.getFeeder() == null || penugasanDosenMengajar.getFeeder().trim().isEmpty()) {

				JSONObject jsonObject = new JSONObject(data);

				Node node = feederConnector.insertRecordOld(token, "dosen_pt", jsonObject.toString());
				String id_reg_ptk = FeederConverter.value(node, "id_reg_ptk");

				System.out.println("id_reg_ptk = " + id_reg_ptk);
				if (id_reg_ptk != null && !id_reg_ptk.isEmpty()) {
					penugasanDosenMengajar.setFeeder(id_reg_ptk);
					// FIX "Session is closed!": re-acquire native session (referensi lama bisa ditutup call Feeder).
					simpanFeederIdDenganNativeSession(dosen);
				}
			} else {

				JSONObject jsonObject = new JSONObject(data);

				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("id_reg_ptk", penugasanDosenMengajar.getFeeder().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);
				Map<String, Object> dataUpdate = new HashMap<String, Object>();
				dataUpdate.put("key", jsonObjectKey);
				dataUpdate.put("data", jsonObject);
				JSONObject dataUpdateObject = new JSONObject(dataUpdate);

				Node node = feederConnector.updateRecordOld(token, "dosen_pt", dataUpdateObject.toString());
				System.out.println("node = " + node);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void ajar_dosen(Session session, FeederConnector feederConnector, String token, Dosen dosen,
			Perkuliahan perkuliahan, Integer jumlahPertemuan, int index, List<String> errorLog) throws Exception {

		System.out.println("results dosen -> " + (dosen == null ? "" : dosen.getFeeder()) + " perkuliahan -> "
				+ (perkuliahan == null ? "" : perkuliahan.getFeeder()));

		if (errorLog != null && dosen != null && dosen.getFeeder() == null) {
			errorLog.add("Dosen " + dosen.getNama() + " belum tersingkron ke feeder");
			return;
		}

		if (dosen != null && dosen.getFeeder() != null && perkuliahan.getFeeder() != null) {

			try {
				JSONArray dataPenugasanDosen = feederConnector.getData("GetListPenugasanDosen", token,
						"id_dosen='" + dosen.getFeeder() + "' and id_tahun_ajaran='"
								+ StringUtils.split(perkuliahan.getTahunAjaran(), "/")[0] + "'",
						"id_tahun_ajaran", "5000", "0");

				System.out.println("results dataPenugasanDosen -> " + dataPenugasanDosen);

				for (int j = 0; j < dataPenugasanDosen.length(); j++) {
					try {
						JSONObject penugasanDosen = dataPenugasanDosen.getJSONObject(j);

						if (dosen.getIdRegPtk() == null || !dosen.getIdRegPtk()
								.equalsIgnoreCase(penugasanDosen.get("id_registrasi_dosen") + "")) {
							dosen.setIdRegPtk(penugasanDosen.get("id_registrasi_dosen") + "");
							// FIX "Session is closed!": re-acquire native session (referensi lama bisa ditutup call Feeder).
							simpanFeederIdDenganNativeSession(dosen);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederExporterHelper.java:138");
					}

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederExporterHelper.java:143");
			}

			final JSONObject jsonObject = FeederExporterGenerator.ajar_dosen(dosen, perkuliahan, jumlahPertemuan);

			String filter = "id_registrasi_dosen='" + dosen.getIdRegPtk() + "' AND id_kelas_kuliah='"
					+ perkuliahan.getFeeder() + "'";

			JSONArray dataDosenPengajarKelasKuliah = feederConnector.getData("GetDosenPengajarKelasKuliah", token,
					filter, "", "1", "");
			System.out.println("results dataDosenPengajarKelasKuliah -> " + dataDosenPengajarKelasKuliah);

			if (dataDosenPengajarKelasKuliah.length() > 0) {

				JSONObject a = dataDosenPengajarKelasKuliah.getJSONObject(0);
				String id_aktivitas_mengajar = a.getString("id_aktivitas_mengajar").trim();
				System.out.println("id_aktivitas_mengajar = " + id_aktivitas_mengajar);
				if (id_aktivitas_mengajar != null && !id_aktivitas_mengajar.isEmpty()) {

					JSONObject idMhs = new JSONObject();
					idMhs.put("id_aktivitas_mengajar", id_aktivitas_mengajar);

					feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateDosenPengajarKelasKuliah", jsonObject,
							errorLog, perkuliahan);
				}
			} else {

				feederConnector.insertOrUpdateRecordBaru(token, null, "InsertDosenPengajarKelasKuliah", jsonObject,
						errorLog, perkuliahan);

//				if (!errorLogTemp.isEmpty()) {
//					new Thread(new Runnable() {
//
//						@Override
//						public void run() {
//							try {
//								Thread.sleep(1000 * 10);
//
//								List<String> errorLogTemp = new ArrayList<String>();
//								feederConnector.insertOrUpdateRecordBaru(token, null, "InsertDosenPengajarKelasKuliah",
//										jsonObject, errorLogTemp, perkuliahan);
//
//							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporterHelper.java:185");
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//
//						}
//					}).start();
//				}
			}

		}
	}

	public static void ajar_dosen(Session session, FeederConnector feederConnector, String token,
			Perkuliahan perkuliahan, int index, List<String> errorLog) {

		Integer jumlahPertemuan = perkuliahan.ambilJumlahPertemuan();
		Map<String, Dosen> dosens = perkuliahan.populateDosen();

		for (Dosen dosen : dosens.values()) {
			try {
				ajar_dosen(session, feederConnector, token, dosen, perkuliahan, jumlahPertemuan, index, errorLog);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederExporterHelper.java:208");
			}
		}
	}

	/**
	 * Simpan entity Feeder (idRegPd mahasiswa / idRegPtk dosen) lewat session khusus milik method ini.
	 * Referensi session yang dibawa dari atas method mahasiswa_pt bisa sudah DITUTUP oleh panggilan
	 * Feeder bersarang (convertRiwayatMahasiswa / getData / insertOrUpdateRecordBaru memanggil
	 * HibernateUtil.closeSession()), sehingga session.getTransaction() melempar "Session is closed!".
	 * Entity detached digabungkan dengan merge. Session khusus selalu dibersihkan dan ditutup di
	 * finally sehingga tidak bergantung pada currentNativeSession milik thread lain.
	 */
	private static void simpanFeederIdDenganNativeSession(GeneralValueObject o) {
		Session s = null;
		Transaction transaction = null;
		try {
			s = HibernateUtil.getSessionFactory().openSession();
			transaction = s.beginTransaction();
			s.merge(o);
			transaction.commit();
		} catch (Exception e) {
			try {
				if (transaction != null && transaction.isActive()) {
					transaction.rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporterHelper.java:233");
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			tutupSessionKhusus(s);
		}
	}

	private static void tutupSessionKhusus(Session session) {
		if (session == null) {
			return;
		}
		try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) FeederExporterHelper.clear"); }
		try { if (session.isConnected()) session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) FeederExporterHelper.disconnect"); }
		try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) FeederExporterHelper.close"); }
	}

	public static void mahasiswa_pt(Session session, FeederConnector feederConnector, String token, Mahasiswa mahasiswa,
			List<String> errorLog) {
		try {
			JSONObject jsonObject = FeederResource.convertRiwayatMahasiswa(mahasiswa);
			String filter = "id_perguruan_tinggi='"
					+ mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFeeder() + "' and id_mahasiswa='"
					+ mahasiswa.getFeeder() + "' and upper(trim(nipd)) = upper(trim('" + mahasiswa.getNim() + "'))";

			JSONArray dataMhsPt = feederConnector.getData("GetListMahasiswa", token, filter, "", "1", "");
			System.out.println("results mahasiswa_pt -> " + dataMhsPt);

			if (dataMhsPt.length() == 0) {

				JSONObject a = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertRiwayatPendidikanMahasiswa",
						jsonObject, errorLog, mahasiswa);

				String id_registrasi_mahasiswa = a.isNull("data") ? null
						: a.getJSONObject("data").getString("id_registrasi_mahasiswa").trim();
				System.out.println("id_registrasi_mahasiswa = " + id_registrasi_mahasiswa);
				if (id_registrasi_mahasiswa != null && !id_registrasi_mahasiswa.isEmpty()) {
					mahasiswa.setIdRegPd(id_registrasi_mahasiswa);
					// FIX "Session is closed!": JANGAN pakai referensi session yang dibawa (bisa sudah ditutup
					// oleh panggilan Feeder bersarang). Re-acquire native session tepat sebelum menulis, dan
					// jangan tutup session di sini (pemilik lifecycle = FeederExporter.mahasiswa_pt).
					simpanFeederIdDenganNativeSession(mahasiswa);
				}

			} else {

				JSONObject mhs = dataMhsPt.getJSONObject(0);
				String id_registrasi_mahasiswa = mhs.getString("id_registrasi_mahasiswa");
				if (mahasiswa.getIdRegPd() == null
						|| !mahasiswa.getIdRegPd().equalsIgnoreCase(id_registrasi_mahasiswa)) {
					mahasiswa.setIdRegPd(id_registrasi_mahasiswa);
					// FIX "Session is closed!": JANGAN pakai referensi session yang dibawa (bisa sudah ditutup
					// oleh panggilan Feeder bersarang). Re-acquire native session tepat sebelum menulis, dan
					// jangan tutup session di sini (pemilik lifecycle = FeederExporter.mahasiswa_pt).
					simpanFeederIdDenganNativeSession(mahasiswa);
				}

				JSONObject idMhs = new JSONObject();
				idMhs.put("id_registrasi_mahasiswa", mahasiswa.getIdRegPd().trim());

				List<String> errorLog1 = new ArrayList<String>();
				feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateRiwayatPendidikanMahasiswa", jsonObject,
						errorLog1, mahasiswa);

			}

			if (mahasiswa.getStatusKeluar() != null) {
				jsonObject = FeederResource.convertRiwayatMahasiswaLulus(mahasiswa);

				filter = "id_registrasi_mahasiswa='" + mahasiswa.getIdRegPd() + "'";

				JSONArray dataMhsLulusDo = feederConnector.getData("GetListMahasiswaLulusDO", token, filter, "", "1",
						"");
				System.out.println("results dataMhsLulusDo -> " + dataMhsLulusDo);

				if (dataMhsLulusDo.length() > 0) {
					jsonObject.remove("id_registrasi_mahasiswa");
					JSONObject idMhs = new JSONObject();
					idMhs.put("id_registrasi_mahasiswa", mahasiswa.getIdRegPd().trim());
					feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateMahasiswaLulusDO", jsonObject,
							errorLog, mahasiswa);
				} else {
					feederConnector.insertOrUpdateRecordBaru(token, null, "InsertMahasiswaLulusDO", jsonObject,
							errorLog, mahasiswa);
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static void kuliah_mahasiswa(Session session, FeederConnector feederConnector, String token,
			Mahasiswa mahasiswa) {

		Number maxSemester = (Number) session.createCriteria(Detailperkuliahan.class)
				.setProjection(Projections.max("semester")).add(Restrictions.eq("mahasiswa", mahasiswa)).uniqueResult();
		Number minSemester = (Number) session.createCriteria(Detailperkuliahan.class)
				.setProjection(Projections.min("semester")).add(Restrictions.eq("mahasiswa", mahasiswa)).uniqueResult();

		if (maxSemester != null && minSemester != null) {

			for (int semester = minSemester.intValue(); semester <= maxSemester.intValue(); semester++) {
				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null, true);
				JSONObject jsonObject = FeederExporterGenerator.kuliah_mahasiswa(session, mahasiswa, semester, null,
						krsMahasiswa);
				try {
					FeederTranspoter.insertAkm(jsonObject, feederConnector, token, session);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}
	}

}
