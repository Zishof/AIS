package ais.action.master.feeder.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;

import ais.action.master.resources.FeederResource;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataDosen;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CommonVO;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.FormulirKegiatan;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKemahasiswaan;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokKkn;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.NilaiHurufExport;
import ais.database.model.PenghargaanMahasiswa;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.Perkuliahan;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.Skripsi;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;

public class FeederExporter {
	private FeederConnector feederConnector;
	private String token;
	private Progressmeter progressmeter;
	private Progressmeter progressmeterChild;
	private Label labelProses;

	public FeederExporter(FeederConnector feederConnector, String token) {
		this.token = token;
		this.feederConnector = feederConnector;
	}

	public FeederExporter(FeederConnector feederConnector, String token, Progressmeter progressmeter,
			Progressmeter progressmeterChild, Label labelProses) {
		this.token = token;
		this.feederConnector = feederConnector;
		this.progressmeter = progressmeter;
		this.progressmeterChild = progressmeterChild;
		this.labelProses = labelProses;
	}

	public void kurikulumPunyaMatakuliah(KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah, List<String> errorLog) {

		if (kurikulumPunyaMatakuliah.getKurikulum() != null
				&& kurikulumPunyaMatakuliah.getKurikulum().getFeeder() == null) {
			kurikulum(kurikulumPunyaMatakuliah.getKurikulum(), errorLog);
		}
		if (kurikulumPunyaMatakuliah.getMatakuliah() != null
				&& kurikulumPunyaMatakuliah.getMatakuliah().getFeeder() == null) {
			matakuliah(kurikulumPunyaMatakuliah.getMatakuliah(), errorLog);
		}

		JSONObject jsonObject = FeederExporterGenerator.kurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah);
		Session session = HibernateUtil.currentNativeSession();
		try {

			String filter = "id_kurikulum='" + kurikulumPunyaMatakuliah.getKurikulum().getFeeder() + "' and id_matkul='"
					+ kurikulumPunyaMatakuliah.getMatakuliah().getFeeder() + "'";

			JSONArray dataMatkulKurikulum = feederConnector.getData("GetMatkulKurikulum", token, filter, "", "1", "");
			System.out.println("results dataMatkulKurikulum -> " + dataMatkulKurikulum);

			if (dataMatkulKurikulum.length() == 0) {

				feederConnector.insertOrUpdateRecordBaru(token, null, "InsertMatkulKurikulum", jsonObject, errorLog,
						kurikulumPunyaMatakuliah);

			} else {

				JSONObject idMhs = new JSONObject();
				idMhs.put("id_kurikulum", kurikulumPunyaMatakuliah.getKurikulum().getFeeder());
				idMhs.put("id_matkul", kurikulumPunyaMatakuliah.getMatakuliah().getFeeder());
				jsonObject.remove("id_kurikulum");
				jsonObject.remove("id_matkul");

				feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateMatkulKurikulum", jsonObject, errorLog,
						kurikulumPunyaMatakuliah);

			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// currentNativeSession dikelola ThreadLocal; satu jalur penutupan ini
			// melakukan clear/disconnect/close tanpa menutup dua kali object yang sama.
			HibernateUtil.closeSession();
		}
	}

	public void kurikulumPunyaMatakuliah() {
		kurikulumPunyaMatakuliah(null);
	}

	public void kurikulumPunyaMatakuliah(List<String> errorLog) {

		Session session = HibernateUtil.currentNativeSession();
		try {
		@SuppressWarnings("unchecked")
		List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = session
				.createCriteria(KurikulumPunyaMatakuliah.class).createAlias("kurikulum", "kurikulum")
				.createAlias("matakuliah", "matakuliah").add(Restrictions.isNotNull("kurikulum.feeder"))
				.add(Restrictions.ne("kurikulum.feeder", ""))

				.add(Restrictions.isNotNull("matakuliah.feeder")).add(Restrictions.ne("matakuliah.feeder", "")).list();
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		System.out.println("kurikulumPunyaMatakuliahs size => " + kurikulumPunyaMatakuliahs.size());

		int index = 0;
		for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / kurikulumPunyaMatakuliahs.size()));
				index++;
			}

			kurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah, errorLog);
		}

		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void kurikulum(Kurikulum kurikulum, List<String> errorLog) {

		JSONObject jsonObject = FeederExporterGenerator.kurikulum(kurikulum);
		Session session = HibernateUtil.currentNativeSession();
		try {

			if (kurikulum.getFeeder() == null || kurikulum.getFeeder().trim().isEmpty()) {

				JSONObject a = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertKurikulum", jsonObject,
						errorLog, kurikulum);

				String id_kurikulum = ambilNilaiData(a, "id_kurikulum");
				System.out.println("id_kurikulum = " + id_kurikulum);

				if (id_kurikulum != null && !id_kurikulum.isEmpty()) {
					kurikulum.setFeeder(id_kurikulum);
					session.getTransaction().begin();
					Common.refreshUpdate(session, kurikulum);
					session.getTransaction().commit();
				}
			} else {
				Map<String, Object> dataKey = new HashMap<String, Object>();
				dataKey.put("id_kurikulum", kurikulum.getFeeder().trim());
				JSONObject jsonObjectKey = new JSONObject(dataKey);

				feederConnector.insertOrUpdateRecordBaru(token, jsonObjectKey, "UpdateKurikulum", jsonObject, errorLog,
						kurikulum);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	public void kurikulum() {
		kurikulum(null);
	}

	public void kurikulum(List<String> errorLog) {

		@SuppressWarnings("unchecked")
		List<Kurikulum> kurikulums;
		Session session = HibernateUtil.currentNativeSession();
		try {
			kurikulums = session.createCriteria(Kurikulum.class).add(Restrictions.ne("nama", ""))
				.createAlias("jurusan", "jurusan").add(Restrictions.isNotNull("jurusan.feeder"))
				.add(Restrictions.ne("jurusan.feeder", "")).list();
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
		System.out.println("kurikulums size => " + kurikulums.size());

		int index = 0;
		for (Kurikulum kurikulum : kurikulums) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / kurikulums.size()));
				index++;
			}
			kurikulum(kurikulum, errorLog);
		}

	}

	@SuppressWarnings("unchecked")
	public void nilaiHuruf() {
		Session session = HibernateUtil.currentNativeSession();
		try {

		List<NilaiHuruf> nilaiHurufs1 = session.createCriteria(NilaiHuruf.class).add(Restrictions.ne("nilaiHuruf", ""))
				.add(Restrictions.isNull("jurusan")).list();

		List<Jurusan> jurusans = session.createCriteria(Jurusan.class).add(Restrictions.isNotNull("feeder"))
				.add(Restrictions.ne("feeder", "")).list();
		System.out.println("nilaiHurufs1 size => " + nilaiHurufs1.size() + ", jurusans = " + jurusans.size());
		int index = 0;
		for (NilaiHuruf nilaiHuruf : nilaiHurufs1) {
			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / nilaiHurufs1.size()));
				index++;
			}

			for (Jurusan jurusan : jurusans) {

				NilaiHurufExport nilaiHurufExport = (NilaiHurufExport) session.createCriteria(NilaiHurufExport.class)
						.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.eq("nilaiHuruf", nilaiHuruf))
						.setMaxResults(1).uniqueResult();
				if (nilaiHurufExport == null) {
					nilaiHurufExport = new NilaiHurufExport();
					nilaiHurufExport.setJurusan(jurusan);
					nilaiHurufExport.setNilaiHuruf(nilaiHuruf);
					session.getTransaction().begin();
					session.save(nilaiHurufExport);
					session.getTransaction().commit();
				}

				JSONObject jsonObject = FeederExporterGenerator.nilaiHuruf(nilaiHurufExport);

				try {

					if (nilaiHurufExport.getFeeder() == null || nilaiHurufExport.getFeeder().trim().isEmpty()) {

						Node node = feederConnector.insertRecordOld(token, "bobot_nilai", jsonObject.toString());
						String kode_bobot_nilai = FeederConverter.value(node, "kode_bobot_nilai");
						if (kode_bobot_nilai != null && !kode_bobot_nilai.isEmpty()) {
							System.out.println("kode_bobot_nilai = " + kode_bobot_nilai);
							nilaiHurufExport.setFeeder(kode_bobot_nilai);
							session.getTransaction().begin();
							Common.refreshUpdate(session, nilaiHurufExport);
							session.getTransaction().commit();
						}
					} else {
						Map<String, Object> dataKey = new HashMap<String, Object>();
						dataKey.put("kode_bobot_nilai", nilaiHurufExport.getFeeder().trim());
						JSONObject jsonObjectKey = new JSONObject(dataKey);
						Map<String, Object> dataUpdate = new HashMap<String, Object>();
						dataUpdate.put("key", jsonObjectKey);
						dataUpdate.put("data", jsonObject);
						JSONObject dataUpdateObject = new JSONObject(dataUpdate);

						Node node = feederConnector.updateRecordOld(token, "bobot_nilai", dataUpdateObject.toString());
						System.out.println("node = " + node);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}

		List<NilaiHuruf> nilaiHurufs = session.createCriteria(NilaiHuruf.class).add(Restrictions.ne("nilaiHuruf", ""))
				.createAlias("jurusan", "jurusan").add(Restrictions.isNotNull("jurusan.feeder"))
				.add(Restrictions.ne("jurusan.feeder", "")).list();

		System.out.println("nilaiHurufs size => " + nilaiHurufs.size());

		index = 0;
		for (NilaiHuruf nilaiHuruf : nilaiHurufs) {
			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / nilaiHurufs.size()));
				index++;
			}

			NilaiHurufExport nilaiHurufExport = (NilaiHurufExport) session.createCriteria(NilaiHurufExport.class)
					.add(Restrictions.eq("jurusan", nilaiHuruf.getJurusan()))
					.add(Restrictions.eq("nilaiHuruf", nilaiHuruf)).setMaxResults(1).uniqueResult();
			if (nilaiHurufExport == null) {
				nilaiHurufExport = new NilaiHurufExport();
				nilaiHurufExport.setJurusan(nilaiHuruf.getJurusan());
				nilaiHurufExport.setNilaiHuruf(nilaiHuruf);
				session.getTransaction().begin();
				session.save(nilaiHurufExport);
				session.getTransaction().commit();
			}

			JSONObject jsonObject = FeederExporterGenerator.nilaiHuruf(nilaiHurufExport);

			try {

				if (nilaiHurufExport.getFeeder() == null || nilaiHurufExport.getFeeder().trim().isEmpty()) {

					Node node = feederConnector.insertRecordOld(token, "bobot_nilai", jsonObject.toString());
					String kode_bobot_nilai = FeederConverter.value(node, "kode_bobot_nilai");
					if (kode_bobot_nilai != null && !kode_bobot_nilai.isEmpty()) {
						System.out.println("kode_bobot_nilai = " + kode_bobot_nilai);
						nilaiHurufExport.setFeeder(kode_bobot_nilai);
						session.getTransaction().begin();
						Common.refreshUpdate(session, nilaiHurufExport);
						session.getTransaction().commit();
					}
				} else {
					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("kode_bobot_nilai", nilaiHurufExport.getFeeder().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					Map<String, Object> dataUpdate = new HashMap<String, Object>();
					dataUpdate.put("key", jsonObjectKey);
					dataUpdate.put("data", jsonObject);
					JSONObject dataUpdateObject = new JSONObject(dataUpdate);

					Node node = feederConnector.updateRecordOld(token, "bobot_nilai", dataUpdateObject.toString());
					System.out.println("node = " + node);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Sinkronkan SATU {@link Matakuliah} ke Neo Feeder memakai pencarian BERTINGKAT 3 TAHAP agar
	 * tidak asal insert duplikat bila matakuliah itu sebenarnya SUDAH ada di Feeder (mis. terdaftar
	 * lewat prodi lain, atau kode sama tapi id_prodi lokal belum akurat):
	 * <ol>
	 *   <li><b>TAHAP 1</b> — cari berdasarkan <b>kode + prodi yang sama</b> ({@code id_prodi} dari
	 *       {@code matakuliah.getJurusan().getFeeder()}).</li>
	 *   <li><b>TAHAP 2</b> — bila tidak ketemu, cari lagi berdasarkan <b>kode saja</b> (tanpa filter
	 *       prodi) — menjangkau kasus matakuliah sudah terdaftar di Feeder pada prodi lain.</li>
	 *   <li><b>TAHAP 3</b> — bila tetap tidak ketemu di kedua tahap pencarian, baru
	 *       <b>insert matakuliah baru</b> ke Feeder ({@code InsertMataKuliah}).</li>
	 * </ol>
	 * Begitu ditemukan (tahap 1/2) atau berhasil diinsert (tahap 3), {@code id_matkul} ditautkan ke
	 * kolom {@code feeder} lokal lalu {@code UpdateMataKuliah} dikirim untuk menyamakan data.
	 * Setiap tahap dibungkus try/catch SENDIRI: kegagalan satu tahap dicatat DETAIL ke
	 * {@code errorLog} (terlihat pengguna + ikut unduhan) dan log server, TIDAK menghentikan
	 * tahap berikutnya.
	 */
	public void matakuliah(Matakuliah matakuliah, List<String> errorLog) {
		String kode = matakuliah.getKode();
		String konteks = "Matakuliah " + kode + " - " + matakuliah.getNama();
		String idProdi = (matakuliah.getJurusan() == null) ? null : matakuliah.getJurusan().getFeeder();

		String id_matkul = null;

		// TAHAP 1: cari berdasarkan KODE + PRODI YANG SAMA.
		if (idProdi != null && !idProdi.trim().isEmpty()) {
			try {
				String filter = "upper(trim(kode_mata_kuliah)) = upper(trim('" + kode + "')) and id_prodi='"
						+ idProdi + "'";
				JSONArray dataMataKuliah = feederConnector.getData("GetListMataKuliah", token, filter, "", "1", "");
				if (dataMataKuliah != null && dataMataKuliah.length() > 0) {
					id_matkul = dataMataKuliah.getJSONObject(0).getString("id_matkul");
					logLangkahFeederExporter(
							"[" + konteks + "] TAHAP 1 ditemukan via kode+prodi sama (id_matkul=" + id_matkul + ")");
				} else {
					logLangkahFeederExporter("[" + konteks + "] TAHAP 1 tidak ditemukan via kode+prodi sama.");
				}
			} catch (Exception e) {
				String pesan = "[" + konteks + "] TAHAP 1 GAGAL mencari MK (kode+prodi sama): " + e.getMessage();
				if (errorLog != null) {
					errorLog.add(pesan);
				}
				logLangkahFeederExporter(pesan);
				Common.tampilErrorJikaAdmin(e);
			}
		} else {
			logLangkahFeederExporter("[" + konteks + "] TAHAP 1 dilewati (prodi matakuliah belum tersingkron feeder).");
		}

		// TAHAP 2: bila tidak ketemu, cari berdasarkan KODE SAJA (prodi apa pun).
		if (id_matkul == null) {
			try {
				String filterKodeSaja = "upper(trim(kode_mata_kuliah)) = upper(trim('" + kode + "'))";
				JSONArray dataKodeSaja = feederConnector.getData("GetListMataKuliah", token, filterKodeSaja, "", "1",
						"");
				if (dataKodeSaja != null && dataKodeSaja.length() > 0) {
					id_matkul = dataKodeSaja.getJSONObject(0).getString("id_matkul");
					logLangkahFeederExporter("[" + konteks + "] TAHAP 2 ditemukan via kode saja, prodi lain "
							+ "(id_matkul=" + id_matkul + ")");
				} else {
					logLangkahFeederExporter("[" + konteks + "] TAHAP 2 tidak ditemukan via kode saja juga.");
				}
			} catch (Exception e) {
				String pesan = "[" + konteks + "] TAHAP 2 GAGAL mencari MK (kode saja): " + e.getMessage();
				if (errorLog != null) {
					errorLog.add(pesan);
				}
				logLangkahFeederExporter(pesan);
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (id_matkul != null && !id_matkul.trim().isEmpty()) {
			// Ditemukan (TAHAP 1 atau 2): tautkan id_matkul lokal (bila beda/belum ada) lalu Update.
			if (matakuliah.getFeeder() == null || !matakuliah.getFeeder().equalsIgnoreCase(id_matkul)) {
				Session session = HibernateUtil.currentNativeSession();
				try {
					matakuliah.setFeeder(id_matkul);
					session.getTransaction().begin();
					Common.refreshUpdate(session, matakuliah);
					session.getTransaction().commit();
				} catch (Exception e) {
					try {
						if (session.getTransaction() != null && session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
					} catch (Exception exRb) {
						ais.common.ErrorAuditUtil.record(exRb,
								"auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:matakuliah.rollback");
					}
					String pesan = "[" + konteks + "] Gagal menyimpan tautan id_matkul lokal: " + e.getMessage();
					if (errorLog != null) {
						errorLog.add(pesan);
					}
					logLangkahFeederExporter(pesan);
					Common.tampilErrorJikaAdmin(e);
				} finally {
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					HibernateUtil.closeSession();
				}
			}
			try {
				JSONObject idMhs = new JSONObject();
				idMhs.put("id_matkul", id_matkul);
				JSONObject jsonObjectq = FeederExporterGenerator.matakuliah(matakuliah);
				feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateMataKuliah", jsonObjectq, errorLog,
						matakuliah);
				logLangkahFeederExporter("[" + konteks + "] UpdateMataKuliah SELESAI.");
			} catch (Exception e) {
				String pesan = "[" + konteks + "] Gagal UpdateMataKuliah: " + e.getMessage();
				if (errorLog != null) {
					errorLog.add(pesan);
				}
				logLangkahFeederExporter(pesan);
				Common.tampilErrorJikaAdmin(e);
			}
		} else {
			// TAHAP 3: benar-benar tidak ada di Feeder pada kedua tahap pencarian -> INSERT baru.
			try {
				JSONObject jsonObject = FeederExporterGenerator.matakuliah(matakuliah);
				JSONObject a = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertMataKuliah", jsonObject,
						errorLog, matakuliah);
				String id_matkul_baru = ambilNilaiData(a, "id_matkul");
				if (id_matkul_baru != null && !id_matkul_baru.isEmpty()) {
					Session session = HibernateUtil.currentNativeSession();
					try {
						matakuliah.setFeeder(id_matkul_baru);
						session.getTransaction().begin();
						Common.refreshUpdate(session, matakuliah);
						session.getTransaction().commit();
						logLangkahFeederExporter(
								"[" + konteks + "] TAHAP 3 berhasil INSERT MK baru (id_matkul=" + id_matkul_baru + ").");
					} catch (Exception e) {
						try {
							if (session.getTransaction() != null && session.getTransaction().isActive()) {
								session.getTransaction().rollback();
							}
						} catch (Exception exRb) {
							ais.common.ErrorAuditUtil.record(exRb,
									"auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:matakuliah.insert.rollback");
						}
						String pesan = "[" + konteks + "] MK baru berhasil diinsert ke Feeder tapi GAGAL menyimpan "
								+ "tautan id_matkul lokal: " + e.getMessage();
						if (errorLog != null) {
							errorLog.add(pesan);
						}
						logLangkahFeederExporter(pesan);
						Common.tampilErrorJikaAdmin(e);
					} finally {
						if (session.isOpen()) {
							session.disconnect();
							session.close();
						}
						HibernateUtil.closeSession();
					}
				} else {
					String pesan = "[" + konteks + "] TAHAP 3 GAGAL insert MK baru ke Feeder (tidak ada id_matkul "
							+ "dikembalikan). Lihat pesan error InsertMataKuliah di atas untuk detail penyebab.";
					if (errorLog != null) {
						errorLog.add(pesan);
					}
					logLangkahFeederExporter(pesan);
				}
			} catch (Exception e) {
				String pesan = "[" + konteks + "] TAHAP 3 GAGAL insert MK baru ke Feeder: " + e.getMessage();
				if (errorLog != null) {
					errorLog.add(pesan);
				}
				logLangkahFeederExporter(pesan);
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	/**
	 * Mencatat satu langkah proses sinkronisasi {@link FeederExporter} ke log server
	 * ({@code System.out}, berstempel waktu) — dipakai untuk menelusuri urutan pencarian/insert
	 * matakuliah &amp; mendiagnosis kegagalan tanpa membanjiri popup progres. Tidak pernah
	 * melempar exception agar logging tidak pernah mengganggu proses utama.
	 */
	private static void logLangkahFeederExporter(String pesan) {
		try {
			System.out.println("[NeoFeeder-Exporter][" + new java.text.SimpleDateFormat("HH:mm:ss.SSS")
					.format(new java.util.Date()) + "] " + pesan);
		} catch (Throwable t) {
			// logging tidak boleh pernah mengganggu proses utama
		}
	}

	@SuppressWarnings("unchecked")
	public void aktivitasMahasiswaPkl(KelompokPkl kelompokPkl, List<String> errorLog) {

		String id_smt = kelompokPkl.getPkl().getTahunAkademik().split("/")[0]
				+ (kelompokPkl.getPkl().getSemester().equals(Perkuliahan.GENAP) ? "2" : "1");

		Session session = HibernateUtil.currentNativeSession();
		try {
			String idjenis = kelompokPkl.getPkl().getJenisAktfitasMahasiswa() == null ? "6"
					: kelompokPkl.getPkl().getJenisAktfitasMahasiswa().getFeeder().toString();
			List<Jurusan> jurusans = ConstantValues.simpleList(session.createCriteria(MahasiswaDapatKelompokPkl.class)
					.add(Restrictions.eq("kelompokPkl", kelompokPkl)).createAlias("mahasiswa", "mahasiswa")
					.setProjection(Projections.groupProperty("mahasiswa.jurusan.id")), Jurusan.class, false);

			Set<String> id_akt_mhss = new HashSet<String>();
			for (Jurusan jurusan : jurusans) {

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("jenis_anggota", "1");
				jsonObject.put("id_jenis_aktivitas", idjenis);
				jsonObject.put("id_prodi", jurusan.getFeeder());
				jsonObject.put("id_semester", id_smt);
				jsonObject.put("judul", kelompokPkl.getNama_kelompok());
				jsonObject.put("keterangan", kelompokPkl.getKeterangan());
				jsonObject.put("lokasi", kelompokPkl.getAlamat());
				jsonObject.put("sk_tugas", kelompokPkl.getNoSk());
				if (kelompokPkl.getTglSk() != null) {
					jsonObject.put("tanggal_sk_tugas", Common.databaseDateFormat.get().format(kelompokPkl.getTglSk()));
				}

				JSONObject feeder = kelompokPkl.getFeeder() == null || kelompokPkl.getFeeder().isEmpty()
						? new JSONObject()
						: new JSONObject(kelompokPkl.getFeeder());

				String idAkt = null;
				try {
					idAkt = feeder.getString(jurusan.getId().toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:491");
					// TODO: handle exception
				}
				List<String> errorLogAk = new ArrayList<String>();
				if (idAkt != null) {
					id_akt_mhss.add(idAkt);
					JSONObject idMhs = new JSONObject();
					idMhs.put("id_aktivitas", idAkt);
					feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateAktivitasMahasiswa", jsonObject,
							errorLogAk, kelompokPkl);
				}

				if (!errorLogAk.isEmpty() || idAkt == null) {
					JSONObject hasil = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAktivitasMahasiswa",
							jsonObject, errorLog, kelompokPkl);
					JSONObject data = ambilDataObject(hasil, errorLog, "InsertAktivitasMahasiswa");
					if (data != null) {
						if (!data.isNull("id_aktivitas")) {
							idAkt = data.getString("id_aktivitas");
							id_akt_mhss.add(idAkt);
							feeder.put(jurusan.getId().toString(), idAkt);
							session.getTransaction().begin();
							kelompokPkl.setFeeder(feeder.toString());
							Common.refreshUpdate(session, kelompokPkl);
							session.getTransaction().commit();
						}
					}
				}

				if (idAkt != null) {
					List<Mahasiswa> mahasiswas = ConstantValues.simpleList(session
							.createCriteria(MahasiswaDapatKelompokPkl.class)
							.add(Restrictions.eq("kelompokPkl", kelompokPkl)).createAlias("mahasiswa", "mahasiswa")
							.add(Restrictions.eq("mahasiswa.jurusan", jurusan))
							.setProjection(Projections.groupProperty("mahasiswa.id")), Mahasiswa.class, false);

					for (Mahasiswa mahasiswa : mahasiswas) {
						if (mahasiswa.getIdRegPd() == null) {
							errorLog.add("Mahasiswa " + mahasiswa + " belum terdaftar");
							continue;
						}

						String filter = "id_registrasi_mahasiswa='" + mahasiswa.getIdRegPd() + "' and id_aktivitas='"
								+ idAkt + "'";
						JSONArray dataAnggotaAktivitasMahasiswa = feederConnector
								.getData("GetListAnggotaAktivitasMahasiswa", token, filter, "", "1000", "");
						System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

						if (dataAnggotaAktivitasMahasiswa.length() == 0) {
							jsonObject = new JSONObject();
							jsonObject.put("id_aktivitas", idAkt);
							jsonObject.put("id_registrasi_mahasiswa", mahasiswa.getIdRegPd());
							jsonObject.put("jns_peran_mhs", "2");
							feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAnggotaAktivitasMahasiswa",
									jsonObject, errorLog, mahasiswa);
						}

					}
				}

				if (!id_akt_mhss.isEmpty()) {
					for (String id_akt_mhs : id_akt_mhss) {
						List<Dosen> dataDosen = kelompokPkl.populateDosenBuNama();
						int pembimbing = 1;
						for (Dosen dosen : dataDosen) {
							if (dosen.getFeeder() != null) {

								String filter = "id_dosen='" + dosen.getFeeder() + "' and id_aktivitas='" + id_akt_mhs
										+ "'";
								JSONArray getListBimbingMahasiswa = feederConnector.getData("GetListBimbingMahasiswa",
										token, filter, "", "1000", "");
								System.out.println("results getListBimbingMahasiswa -> " + getListBimbingMahasiswa);

								if (getListBimbingMahasiswa.length() == 0) {

									jsonObject = new JSONObject();
									jsonObject.put("id_aktivitas", id_akt_mhs);
									jsonObject.put("id_kategori_kegiatan", "110300");
									jsonObject.put("id_dosen", dosen.getFeeder());
									jsonObject.put("pembimbing_ke", "" + pembimbing);
									feederConnector.insertOrUpdateRecordBaru(token, null, "InsertBimbingMahasiswa",
											jsonObject, errorLog, dosen);
								}

								pembimbing++;
							} else {
								// Dosen tanpa kode Feeder JANGAN dilewati diam-diam — laporkan agar operator tahu.
								errorLog.add(NeoFeederErrorHelper.belumTerdaftar("Dosen pembimbing",
									dosen == null ? "-" : dosen.getNama(),
									"Kirim data Dosen ini ke Feeder dulu (menu Dosen > Kirim ke Feeder); untuk sementara dosen ini DILEWATI"));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	@SuppressWarnings("unchecked")
	public void aktivitasMahasiswaKkn(KelompokKkn kelompokKkn, List<String> errorLog) {

		String id_smt = kelompokKkn.getKkn().getTahunAkademik().split("/")[0]
				+ (kelompokKkn.getKkn().getSemester().equals(Perkuliahan.GENAP) ? "2" : "1");

		Session session = HibernateUtil.currentNativeSession();
		try {
//			String idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Kuliah kerja nyata") == null ? "5"
//					: FeederJSONImport.JENIS_KEGIATAN.get("Kuliah kerja nyata");

			String idjenis = kelompokKkn.getKkn().getJenisAktfitasMahasiswa() == null ? "5"
					: kelompokKkn.getKkn().getJenisAktfitasMahasiswa().getFeeder().toString();

			List<Jurusan> jurusans = ConstantValues.simpleList(session.createCriteria(MahasiswaDapatKelompokKkn.class)
					.add(Restrictions.eq("kelompokKkn", kelompokKkn)).createAlias("mahasiswa", "mahasiswa")
					.setProjection(Projections.groupProperty("mahasiswa.jurusan.id")), Jurusan.class, false);

			Set<String> id_akt_mhss = new HashSet<String>();
			for (Jurusan jurusan : jurusans) {

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("jenis_anggota", "1");
				jsonObject.put("id_jenis_aktivitas", idjenis);
				jsonObject.put("id_prodi", jurusan.getFeeder());
				jsonObject.put("id_semester", id_smt);
				jsonObject.put("judul", kelompokKkn.getNama_kelompok());
				jsonObject.put("keterangan", kelompokKkn.getKeterangan());
				jsonObject.put("lokasi", kelompokKkn.getAlamat());
				jsonObject.put("sk_tugas", kelompokKkn.getNoSk());
				if (kelompokKkn.getTglSk() != null) {
					jsonObject.put("tanggal_sk_tugas", Common.databaseDateFormat.get().format(kelompokKkn.getTglSk()));
				}

				JSONObject feeder = kelompokKkn.getFeeder() == null || kelompokKkn.getFeeder().isEmpty()
						? new JSONObject()
						: new JSONObject(kelompokKkn.getFeeder());

				String idAkt = null;
				try {
					idAkt = feeder.getString(jurusan.getId().toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:639");
					// TODO: handle exception
				}

				List<String> errorLogAk = new ArrayList<String>();
				if (idAkt != null) {
					id_akt_mhss.add(idAkt);
					JSONObject idMhs = new JSONObject();
					idMhs.put("id_aktivitas", idAkt);
					feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateAktivitasMahasiswa", jsonObject,
							errorLogAk, kelompokKkn);
				}

				if (!errorLogAk.isEmpty() || idAkt == null) {
					JSONObject hasil = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAktivitasMahasiswa",
							jsonObject, errorLog, kelompokKkn);
					JSONObject data = ambilDataObject(hasil, errorLog, "InsertAktivitasMahasiswa");
					if (data != null) {
						if (!data.isNull("id_aktivitas")) {
							idAkt = data.getString("id_aktivitas");
							id_akt_mhss.add(idAkt);
							feeder.put(jurusan.getId().toString(), idAkt);
							session.getTransaction().begin();
							kelompokKkn.setFeeder(feeder.toString());
							Common.refreshUpdate(session, kelompokKkn);
							session.getTransaction().commit();
						}
					}
				}

				if (idAkt != null) {
					List<Mahasiswa> mahasiswas = ConstantValues.simpleList(session
							.createCriteria(MahasiswaDapatKelompokKkn.class)
							.add(Restrictions.eq("kelompokKkn", kelompokKkn)).createAlias("mahasiswa", "mahasiswa")
							.add(Restrictions.eq("mahasiswa.jurusan", jurusan))
							.setProjection(Projections.groupProperty("mahasiswa.id")), Mahasiswa.class, false);

					for (Mahasiswa mahasiswa : mahasiswas) {
						if (mahasiswa.getIdRegPd() == null) {
							errorLog.add("Mahasiswa " + mahasiswa + " belum terdaftar");
							continue;
						}

						String filter = "id_registrasi_mahasiswa='" + mahasiswa.getIdRegPd() + "' and id_aktivitas='"
								+ idAkt + "'";
						JSONArray dataAnggotaAktivitasMahasiswa = feederConnector
								.getData("GetListAnggotaAktivitasMahasiswa", token, filter, "", "1000", "");
						System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

						if (dataAnggotaAktivitasMahasiswa.length() == 0) {
							jsonObject = new JSONObject();
							jsonObject.put("id_aktivitas", idAkt);
							jsonObject.put("id_registrasi_mahasiswa", mahasiswa.getIdRegPd());
							jsonObject.put("jns_peran_mhs", "2");
							feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAnggotaAktivitasMahasiswa",
									jsonObject, errorLog, mahasiswa);
						}

					}
				}

				if (!id_akt_mhss.isEmpty()) {
					for (String id_akt_mhs : id_akt_mhss) {
						List<Dosen> dataDosen = kelompokKkn.populateDosenBuNama();
						int pembimbing = 1;
						for (Dosen dosen : dataDosen) {
							if (dosen.getFeeder() != null) {

								String filter = "id_dosen='" + dosen.getFeeder() + "' and id_aktivitas='" + id_akt_mhs
										+ "'";
								JSONArray getListBimbingMahasiswa = feederConnector.getData("GetListBimbingMahasiswa",
										token, filter, "", "1000", "");
								System.out.println("results getListBimbingMahasiswa -> " + getListBimbingMahasiswa);

								if (getListBimbingMahasiswa.length() == 0) {

									jsonObject = new JSONObject();
									jsonObject.put("id_aktivitas", id_akt_mhs);
									jsonObject.put("id_kategori_kegiatan", "110300");
									jsonObject.put("id_dosen", dosen.getFeeder());
									jsonObject.put("pembimbing_ke", "" + pembimbing);
									feederConnector.insertOrUpdateRecordBaru(token, null, "InsertBimbingMahasiswa",
											jsonObject, errorLog, dosen);
								}

								pembimbing++;
							} else {
								// Dosen tanpa kode Feeder JANGAN dilewati diam-diam — laporkan agar operator tahu.
								errorLog.add(NeoFeederErrorHelper.belumTerdaftar("Dosen pembimbing",
									dosen == null ? "-" : dosen.getNama(),
									"Kirim data Dosen ini ke Feeder dulu (menu Dosen > Kirim ke Feeder); untuk sementara dosen ini DILEWATI"));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	@SuppressWarnings("unchecked")
	public void aktivitasMahasiswaForm(FormulirKegiatan formulirKegiatan, List<String> errorLog) {

		String id_smt = formulirKegiatan.getTahunAkademik().split("/")[0]
				+ (formulirKegiatan.getSemester().equals(Perkuliahan.GENAP) ? "2" : "1");

		Session session = HibernateUtil.currentNativeSession();
		try {
			String idjenis = formulirKegiatan.getJenisAktfitasMahasiswa() == null ? "10"
					: formulirKegiatan.getJenisAktfitasMahasiswa().getFeeder().toString();

			List<Jurusan> jurusans = ConstantValues.simpleList(session.createCriteria(FormulirKegiatanPeserta.class)
					.add(Restrictions.eq("formulirKegiatan", formulirKegiatan)).createAlias("mahasiswa", "mahasiswa")
					.setProjection(Projections.groupProperty("mahasiswa.jurusan.id")), Jurusan.class, false);

			Set<String> id_akt_mhss = new HashSet<String>();
			for (Jurusan jurusan : jurusans) {

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("jenis_anggota", "1");
				jsonObject.put("id_jenis_aktivitas", idjenis);
				jsonObject.put("id_prodi", jurusan.getFeeder());
				jsonObject.put("id_semester", id_smt);
				jsonObject.put("judul", formulirKegiatan.getNama());
				jsonObject.put("keterangan", formulirKegiatan.getKeterangan());
				jsonObject.put("lokasi", formulirKegiatan.getAlamat());
				jsonObject.put("sk_tugas", formulirKegiatan.getNoSk());
				if (formulirKegiatan.getTglSk() != null) {
					jsonObject.put("tanggal_sk_tugas", Common.databaseDateFormat.get().format(formulirKegiatan.getTglSk()));
				}

				JSONObject feeder = formulirKegiatan.getFeeder() == null || formulirKegiatan.getFeeder().isEmpty()
						? new JSONObject()
						: new JSONObject(formulirKegiatan.getFeeder());

				String idAkt = null;
				try {
					idAkt = feeder.getString(jurusan.getId().toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:785");
					// TODO: handle exception
				}

				List<String> errorLogAk = new ArrayList<String>();
				if (idAkt != null) {
					id_akt_mhss.add(idAkt);
					JSONObject idMhs = new JSONObject();
					idMhs.put("id_aktivitas", idAkt);
					feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateAktivitasMahasiswa", jsonObject,
							errorLogAk, formulirKegiatan);
				}

				if (!errorLogAk.isEmpty() || idAkt == null) {
					JSONObject hasil = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAktivitasMahasiswa",
							jsonObject, errorLog, formulirKegiatan);
					JSONObject data = ambilDataObject(hasil, errorLog, "InsertAktivitasMahasiswa");
					if (data != null) {
						if (!data.isNull("id_aktivitas")) {
							idAkt = data.getString("id_aktivitas");
							id_akt_mhss.add(idAkt);
							feeder.put(jurusan.getId().toString(), idAkt);
							session.getTransaction().begin();
							formulirKegiatan.setFeeder(feeder.toString());
							Common.refreshUpdate(session, formulirKegiatan);
							session.getTransaction().commit();
						}
					}
				}

				if (idAkt != null) {
					List<Mahasiswa> mahasiswas = ConstantValues.simpleList(session
							.createCriteria(FormulirKegiatanPeserta.class)
							.add(Restrictions.eq("formulirKegiatan", formulirKegiatan))
							.createAlias("mahasiswa", "mahasiswa").add(Restrictions.eq("mahasiswa.jurusan", jurusan))
							.setProjection(Projections.groupProperty("mahasiswa.id")), Mahasiswa.class, false);

					for (Mahasiswa mahasiswa : mahasiswas) {
						if (mahasiswa.getIdRegPd() == null) {
							errorLog.add("Mahasiswa " + mahasiswa + " belum terdaftar");
							continue;
						}

						String filter = "id_registrasi_mahasiswa='" + mahasiswa.getIdRegPd() + "' and id_aktivitas='"
								+ idAkt + "'";
						JSONArray dataAnggotaAktivitasMahasiswa = feederConnector
								.getData("GetListAnggotaAktivitasMahasiswa", token, filter, "", "1000", "");
						System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

						if (dataAnggotaAktivitasMahasiswa.length() == 0) {
							jsonObject = new JSONObject();
							jsonObject.put("id_aktivitas", idAkt);
							jsonObject.put("id_registrasi_mahasiswa", mahasiswa.getIdRegPd());
							jsonObject.put("jns_peran_mhs", "2");
							feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAnggotaAktivitasMahasiswa",
									jsonObject, errorLog, mahasiswa);
						}

					}
				}

				if (!id_akt_mhss.isEmpty()) {
					for (String id_akt_mhs : id_akt_mhss) {
						List<Dosen> dataDosen = ConstantValues.simpleList(session
								.createCriteria(FormulirKegiatanPeserta.class)
								.add(Restrictions.eq("formulirKegiatan", formulirKegiatan))
								.createAlias("dosen", "dosen").setProjection(Projections.groupProperty("dosen.id")),
								Dosen.class, false);

						int pembimbing = 1;
						for (Dosen dosen : dataDosen) {
							if (dosen.getFeeder() != null) {

								String filter = "id_dosen='" + dosen.getFeeder() + "' and id_aktivitas='" + id_akt_mhs
										+ "'";
								JSONArray getListBimbingMahasiswa = feederConnector.getData("GetListBimbingMahasiswa",
										token, filter, "", "1000", "");
								System.out.println("results getListBimbingMahasiswa -> " + getListBimbingMahasiswa);

								if (getListBimbingMahasiswa.length() == 0) {

									jsonObject = new JSONObject();
									jsonObject.put("id_aktivitas", id_akt_mhs);
									jsonObject.put("id_kategori_kegiatan", "110300");
									jsonObject.put("id_dosen", dosen.getFeeder());
									jsonObject.put("pembimbing_ke", "" + pembimbing);
									feederConnector.insertOrUpdateRecordBaru(token, null, "InsertBimbingMahasiswa",
											jsonObject, errorLog, dosen);
								}

								pembimbing++;
							} else {
								// Dosen tanpa kode Feeder JANGAN dilewati diam-diam — laporkan agar operator tahu.
								errorLog.add(NeoFeederErrorHelper.belumTerdaftar("Dosen pembimbing",
									dosen == null ? "-" : dosen.getNama(),
									"Kirim data Dosen ini ke Feeder dulu (menu Dosen > Kirim ke Feeder); untuk sementara dosen ini DILEWATI"));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	public void aktivitasMahasiswaPengahargaan(PenghargaanMahasiswa penghargaanMahasiswa, List<String> errorLog) {

		String id_smt = penghargaanMahasiswa.getTahunAkademik().split("/")[0]
				+ (penghargaanMahasiswa.getJenisSemester().equals(Perkuliahan.GENAP) ? "2" : "1");

		Session session = HibernateUtil.currentNativeSession();
		try {
			String idjenis = penghargaanMahasiswa.getJenisAktfitasMahasiswa() == null ? "10"
					: penghargaanMahasiswa.getJenisAktfitasMahasiswa().getFeeder().toString();

			Set<String> id_akt_mhss = new HashSet<String>();
			Jurusan jurusan = penghargaanMahasiswa.getMahasiswa().getJurusan();

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("jenis_anggota", "1");
			jsonObject.put("id_jenis_aktivitas", idjenis);
			jsonObject.put("id_prodi", jurusan.getFeeder());
			jsonObject.put("id_semester", id_smt);
			jsonObject.put("judul", penghargaanMahasiswa.getNama());
			jsonObject.put("keterangan", penghargaanMahasiswa.getKeterangan());
			jsonObject.put("lokasi", penghargaanMahasiswa.getAlamat());
			jsonObject.put("sk_tugas", penghargaanMahasiswa.getNoSk());
			if (penghargaanMahasiswa.getTglSk() != null) {
				jsonObject.put("tanggal_sk_tugas", Common.databaseDateFormat.get().format(penghargaanMahasiswa.getTglSk()));
			}

			JSONObject feeder = penghargaanMahasiswa.getFeeder() == null || penghargaanMahasiswa.getFeeder().isEmpty()
					? new JSONObject()
					: new JSONObject(penghargaanMahasiswa.getFeeder());

			String idAkt = null;
			try {
				idAkt = feeder.getString(jurusan.getId().toString());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:931");
				// TODO: handle exception
			}

			List<String> errorLogAk = new ArrayList<String>();
			if (idAkt != null) {
				id_akt_mhss.add(idAkt);
				JSONObject idMhs = new JSONObject();
				idMhs.put("id_aktivitas", idAkt);
				feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateAktivitasMahasiswa", jsonObject,
						errorLogAk, penghargaanMahasiswa);
			}

			if (!errorLogAk.isEmpty() || idAkt == null) {
				JSONObject hasil = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAktivitasMahasiswa",
						jsonObject, errorLog, penghargaanMahasiswa);
				JSONObject data = ambilDataObject(hasil, errorLog, "InsertAktivitasMahasiswa");
				if (data != null) {
					if (!data.isNull("id_aktivitas")) {
						idAkt = data.getString("id_aktivitas");
						id_akt_mhss.add(idAkt);
						feeder.put(jurusan.getId().toString(), idAkt);
						session.getTransaction().begin();
						penghargaanMahasiswa.setFeeder(feeder.toString());
						Common.refreshUpdate(session, penghargaanMahasiswa);
						session.getTransaction().commit();
					}
				}
			}

			if (idAkt != null) {
				Mahasiswa mahasiswa = penghargaanMahasiswa.getMahasiswa();
				if (mahasiswa.getIdRegPd() == null) {
					errorLog.add("Mahasiswa " + mahasiswa + " belum terdaftar");
					return;
				}

				String filter = "id_registrasi_mahasiswa='" + mahasiswa.getIdRegPd() + "' and id_aktivitas='" + idAkt
						+ "'";
				JSONArray dataAnggotaAktivitasMahasiswa = feederConnector.getData("GetListAnggotaAktivitasMahasiswa",
						token, filter, "", "1000", "");
				System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

				if (dataAnggotaAktivitasMahasiswa.length() == 0) {
					jsonObject = new JSONObject();
					jsonObject.put("id_aktivitas", idAkt);
					jsonObject.put("id_registrasi_mahasiswa", mahasiswa.getIdRegPd());
					jsonObject.put("jns_peran_mhs", "2");
					feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAnggotaAktivitasMahasiswa", jsonObject,
							errorLog, mahasiswa);
				}

			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	public void aktivitasMahasiswaPrestasi(PrestasiMahasiswa prestasiMahasiswa, List<String> errorLog) {

		String id_smt = prestasiMahasiswa.getTahunAkademik().split("/")[0]
				+ (prestasiMahasiswa.getJenisSemester().equals(Perkuliahan.GENAP) ? "2" : "1");
		String idAkt = null;
		Session session = HibernateUtil.currentNativeSession();
		try {
			String idjenis = prestasiMahasiswa.getJenisAktfitasMahasiswa() == null ? "10"
					: prestasiMahasiswa.getJenisAktfitasMahasiswa().getFeeder().toString();

			Set<String> id_akt_mhss = new HashSet<String>();
			Jurusan jurusan = prestasiMahasiswa.getMahasiswa().getJurusan();

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("jenis_anggota", "1");
			jsonObject.put("id_jenis_aktivitas", idjenis);
			jsonObject.put("id_prodi", jurusan.getFeeder());
			jsonObject.put("id_semester", id_smt);
			jsonObject.put("judul", prestasiMahasiswa.getNama());
			jsonObject.put("keterangan", prestasiMahasiswa.getKeterangan());
			jsonObject.put("lokasi", prestasiMahasiswa.getAlamat());
			jsonObject.put("sk_tugas", prestasiMahasiswa.getNoSk());
			if (prestasiMahasiswa.getTglSk() != null) {
				jsonObject.put("tanggal_sk_tugas", Common.databaseDateFormat.get().format(prestasiMahasiswa.getTglSk()));
			}

			JSONObject feeder = prestasiMahasiswa.getFeeder() == null || prestasiMahasiswa.getFeeder().isEmpty()
					? new JSONObject()
					: new JSONObject(prestasiMahasiswa.getFeeder());

			try {
				idAkt = feeder.getString(jurusan.getId().toString());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:1029");
				// TODO: handle exception
			}

			List<String> errorLogAk = new ArrayList<String>();
			if (idAkt != null) {
				id_akt_mhss.add(idAkt);
				JSONObject idMhs = new JSONObject();
				idMhs.put("id_aktivitas", idAkt);
				feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateAktivitasMahasiswa", jsonObject,
						errorLogAk, prestasiMahasiswa);
			}

			if (!errorLogAk.isEmpty() || idAkt == null) {
				JSONObject hasil = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAktivitasMahasiswa",
						jsonObject, errorLog, prestasiMahasiswa);
				JSONObject data = ambilDataObject(hasil, errorLog, "InsertAktivitasMahasiswa");
				if (data != null) {
					if (!data.isNull("id_aktivitas")) {
						idAkt = data.getString("id_aktivitas");
						id_akt_mhss.add(idAkt);
						feeder.put(jurusan.getId().toString(), idAkt);
						session.getTransaction().begin();
						prestasiMahasiswa.setFeeder(feeder.toString());
						Common.refreshUpdate(session, prestasiMahasiswa);
						session.getTransaction().commit();
					}
				}
			}

			if (idAkt != null) {
				Mahasiswa mahasiswa = prestasiMahasiswa.getMahasiswa();
				if (mahasiswa.getIdRegPd() == null) {
					errorLog.add("Mahasiswa " + mahasiswa + " belum terdaftar");
					return;
				}

				String filter = "id_registrasi_mahasiswa='" + mahasiswa.getIdRegPd() + "' and id_aktivitas='" + idAkt
						+ "'";
				JSONArray dataAnggotaAktivitasMahasiswa = feederConnector.getData("GetListAnggotaAktivitasMahasiswa",
						token, filter, "", "1000", "");
				System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

				if (dataAnggotaAktivitasMahasiswa.length() == 0) {
					jsonObject = new JSONObject();
					jsonObject.put("id_aktivitas", idAkt);
					jsonObject.put("id_registrasi_mahasiswa", mahasiswa.getIdRegPd());
					jsonObject.put("jns_peran_mhs", "2");
					feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAnggotaAktivitasMahasiswa", jsonObject,
							errorLog, mahasiswa);
				}

			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		try {

			Jurusan jurusan = prestasiMahasiswa.getMahasiswa().getJurusan();

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id_perguruan_tinggi", jurusan.getFakultas().getPerguruanTinggi().getFeeder());
			jsonObject.put("id_mahasiswa", prestasiMahasiswa.getMahasiswa().getIdRegPd());
			jsonObject.put("id_jenis_prestasi", prestasiMahasiswa.getCabangPrestasiMahasiswa().getFeeder());
			jsonObject.put("id_tingkat_prestasi", prestasiMahasiswa.getKategoriPrestasiMahasiswa().getFeeder());
			jsonObject.put("nama_prestasi", prestasiMahasiswa.getNama());
			jsonObject.put("tahun_prestasi", prestasiMahasiswa.getTahun() + "");
			jsonObject.put("penyelenggara", prestasiMahasiswa.getPenyelenggara());
			jsonObject.put("peringkat", prestasiMahasiswa.getPeringkat() + "");
			if (idAkt != null) {
				jsonObject.put("id_aktivitas", idAkt);
			}

			List<String> errorLogAk = new ArrayList<String>();
			if (prestasiMahasiswa.getFeederPrestasi() != null) {
				JSONObject idMhs = new JSONObject();
				idMhs.put("id_prestasi", prestasiMahasiswa.getFeederPrestasi());
				feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdatePrestasiMahasiswa", jsonObject,
						errorLogAk, prestasiMahasiswa);
			}

			if (!errorLogAk.isEmpty() || idAkt == null) {
				JSONObject hasil = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertPrestasiMahasiswa",
						jsonObject, errorLog, prestasiMahasiswa);
				JSONObject data = ambilDataObject(hasil, errorLog, "InsertPrestasiMahasiswa");
				if (data != null) {
					if (!data.isNull("id_prestasi")) {
						String id_prestasi = data.getString("id_prestasi");
						session.getTransaction().begin();
						prestasiMahasiswa.setFeederPrestasi(id_prestasi);
						Common.refreshUpdate(session, prestasiMahasiswa);
						session.getTransaction().commit();
					}
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
	}

	@SuppressWarnings("unchecked")
	public void aktivitasKegiatanMahasiswa(KegiatanKemahasiswaan kegiatanKemahasiswaan, List<String> errorLog) {

		String id_smt = kegiatanKemahasiswaan.getTahunAkademik().split("/")[0]
				+ (kegiatanKemahasiswaan.getJenisSemester().equals(Perkuliahan.GENAP) ? "2" : "1");

		Session session = HibernateUtil.currentNativeSession();
		try {
			String idjenis = kegiatanKemahasiswaan.getJenisAktfitasMahasiswa() == null ? "10"
					: kegiatanKemahasiswaan.getJenisAktfitasMahasiswa().getFeeder().toString();

			List<Jurusan> jurusans = ConstantValues
					.simpleList(session.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)
							.add(Restrictions.eq("kegiatanKemahasiswaan", kegiatanKemahasiswaan))
							.createAlias("mahasiswa", "mahasiswa")
							.setProjection(Projections.groupProperty("mahasiswa.jurusan.id")), Jurusan.class, false);

			Set<String> id_akt_mhss = new HashSet<String>();
			for (Jurusan jurusan : jurusans) {

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("jenis_anggota", "1");
				jsonObject.put("id_jenis_aktivitas", idjenis);
				jsonObject.put("id_prodi", jurusan.getFeeder());
				jsonObject.put("id_semester", id_smt);
				jsonObject.put("judul", kegiatanKemahasiswaan.getNama());
				jsonObject.put("keterangan", kegiatanKemahasiswaan.getKeterangan());
				jsonObject.put("lokasi", kegiatanKemahasiswaan.getTempat());
				jsonObject.put("sk_tugas", kegiatanKemahasiswaan.getNoSk());
				if (kegiatanKemahasiswaan.getTglSk() != null) {
					jsonObject.put("tanggal_sk_tugas",
							Common.databaseDateFormat.get().format(kegiatanKemahasiswaan.getTglSk()));
				}

				JSONObject feeder = kegiatanKemahasiswaan.getFeeder() == null
						|| kegiatanKemahasiswaan.getFeeder().isEmpty() ? new JSONObject()
								: new JSONObject(kegiatanKemahasiswaan.getFeeder());

				String idAkt = null;
				try {
					idAkt = feeder.getString(jurusan.getId().toString());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:1180");
					// TODO: handle exception
				}

				List<String> errorLogAk = new ArrayList<String>();
				if (idAkt != null) {
					id_akt_mhss.add(idAkt);
					JSONObject idMhs = new JSONObject();
					idMhs.put("id_aktivitas", idAkt);
					feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateAktivitasMahasiswa", jsonObject,
							errorLogAk, kegiatanKemahasiswaan);
				}

				if (!errorLogAk.isEmpty() || idAkt == null) {
					JSONObject hasil = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAktivitasMahasiswa",
							jsonObject, errorLog, kegiatanKemahasiswaan);
					JSONObject data = ambilDataObject(hasil, errorLog, "InsertAktivitasMahasiswa");
					if (data != null) {
						if (!data.isNull("id_aktivitas")) {
							idAkt = data.getString("id_aktivitas");
							id_akt_mhss.add(idAkt);
							feeder.put(jurusan.getId().toString(), idAkt);
							session.getTransaction().begin();
							kegiatanKemahasiswaan.setFeeder(feeder.toString());
							Common.refreshUpdate(session, kegiatanKemahasiswaan);
							session.getTransaction().commit();
						}
					}
				}

				if (idAkt != null) {

					List<Mahasiswa> mahasiswas = ConstantValues.simpleList(session
							.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)
							.add(Restrictions.eq("kegiatanKemahasiswaan", kegiatanKemahasiswaan))
							.createAlias("mahasiswa", "mahasiswa").add(Restrictions.eq("mahasiswa.jurusan", jurusan))
							.setProjection(Projections.groupProperty("mahasiswa.id")), Mahasiswa.class, false);

					for (Mahasiswa mahasiswa : mahasiswas) {
						if (mahasiswa.getIdRegPd() == null) {
							errorLog.add("Mahasiswa " + mahasiswa + " belum terdaftar");
							return;
						}

						String filter = "id_registrasi_mahasiswa='" + mahasiswa.getIdRegPd() + "' and id_aktivitas='"
								+ idAkt + "'";
						JSONArray dataAnggotaAktivitasMahasiswa = feederConnector
								.getData("GetListAnggotaAktivitasMahasiswa", token, filter, "", "1000", "");
						System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

						if (dataAnggotaAktivitasMahasiswa.length() == 0) {
							jsonObject = new JSONObject();
							jsonObject.put("id_aktivitas", idAkt);
							jsonObject.put("id_registrasi_mahasiswa", mahasiswa.getIdRegPd());
							jsonObject.put("jns_peran_mhs", "2");
							feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAnggotaAktivitasMahasiswa",
									jsonObject, errorLog, mahasiswa);
						}

					}
				}

			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	public void aktivitasMahasiswa(Skripsi skripsi, List<String> errorLog) {
		if (skripsi.getMahasiswa() == null || skripsi.getMahasiswa().getIdRegPd() == null
				|| skripsi.getMahasiswa().getIdRegPd().trim().isEmpty()) {
			errorLog.add(NeoFeederErrorHelper.belumTerdaftar("Mahasiswa",
					skripsi.getMahasiswa() == null ? "-"
							: (skripsi.getMahasiswa().getNim() + " - " + skripsi.getMahasiswa().getNama()),
					"Kirim data Mahasiswa ini ke Feeder dulu (menu Mahasiswa > Kirim ke Feeder), lalu ulangi kirim"));
			return;
		}
		// Prodi wajib punya kode Feeder (id_prodi) — bila kosong, Feeder pasti menolak.
		if (skripsi.getMahasiswa().getJurusan() == null || skripsi.getMahasiswa().getJurusan().getFeeder() == null
				|| skripsi.getMahasiswa().getJurusan().getFeeder().trim().isEmpty()) {
			errorLog.add(NeoFeederErrorHelper.belumTerdaftar("Program Studi",
					skripsi.getMahasiswa().getJurusan() == null ? "-" : skripsi.getMahasiswa().getJurusan().getNama(),
					"Sinkronkan data Program Studi ke Feeder dulu, lalu ulangi kirim"));
			return;
		}
		String id_smt = skripsi.getTahunAkademik().split("/")[0]
				+ FeederExporterGenerator.digitPeriodeFeeder(skripsi.getMahasiswa(), skripsi.getSemester(), null);
		String idjenis = skripsi.getFormatNilaiSkripsi() == null
				|| skripsi.getFormatNilaiSkripsi().getJenisKegiatanMahasiswa() == null ? null
						: skripsi.getFormatNilaiSkripsi().getJenisKegiatanMahasiswa().getKode();

		if (idjenis == null || idjenis.trim().isEmpty()) {
			idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Laporan akhir studi");
			try {
//				if (skripsi.getMahasiswa().getJurusan().getJenjang().getId().equals(ConstantValues.d3.getId())) {
//					idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Tugas akhir");
//				} else 

				if (skripsi.getMahasiswa().getJurusan().getJenjang().getId().equals(ConstantValues.s2.getId())) {
					idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Tesis");
				} else if (skripsi.getMahasiswa().getJurusan().getJenjang().getId().equals(ConstantValues.s3.getId())) {
					idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Disertasi");
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederExporter.java:1291");
			}
		}

		Session session = HibernateUtil.currentNativeSession();
		try {

			String filter = "id_registrasi_mahasiswa='" + skripsi.getMahasiswa().getIdRegPd() + "'";
			JSONArray dataAnggotaAktivitasMahasiswa = feederConnector.getData("GetListAnggotaAktivitasMahasiswa", token,
					filter, "", "1000", "");
			System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

			String id_akt_mhs = null;
			for (int i = 0; i < dataAnggotaAktivitasMahasiswa.length(); i++) {
				JSONObject jsonObject = dataAnggotaAktivitasMahasiswa.getJSONObject(i);

				filter = "id_aktivitas='" + jsonObject.getString("id_aktivitas") + "' and id_jenis_aktivitas='"
						+ idjenis + "' and id_semester = '" + id_smt + "'";

				JSONArray dataAktivitasMahasiswa = feederConnector.getData("GetListAktivitasMahasiswa", token, filter,
						"", "1", "");
				System.out.println("results dataAktivitasMahasiswa -> " + dataAktivitasMahasiswa);

				if (dataAktivitasMahasiswa.length() > 0) {
					id_akt_mhs = jsonObject.getString("id_aktivitas");
				}
			}

			if (id_akt_mhs != null) {
				session.getTransaction().begin();
				skripsi.setFeeder(id_akt_mhs);
				Common.refreshUpdate(session, skripsi);
				session.getTransaction().commit();
			}

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("jenis_anggota", "0");
			jsonObject.put("id_jenis_aktivitas", idjenis);
			jsonObject.put("id_prodi", skripsi.getMahasiswa().getJurusan().getFeeder());
			jsonObject.put("id_semester", id_smt);
			jsonObject.put("judul", skripsi.getJudul());
			jsonObject.put("keterangan", skripsi.getKeterangan());
			jsonObject.put("lokasi", skripsi.getLokasiUjian());
			jsonObject.put("sk_tugas", skripsi.getNomorSk());
			if (skripsi.getTglSk() != null) {
				jsonObject.put("tanggal_sk_tugas", Common.databaseDateFormat.get().format(skripsi.getTglSk()));
			}

			if (id_akt_mhs != null) {
				JSONObject idMhs = new JSONObject();
				idMhs.put("id_aktivitas", skripsi.getFeeder().trim());
				feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateAktivitasMahasiswa", jsonObject, errorLog,
						skripsi);
			} else {
				JSONObject hasil = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAktivitasMahasiswa",
						jsonObject, errorLog, skripsi);
				JSONObject data = ambilDataObject(hasil, errorLog, "InsertAktivitasMahasiswa");
				if (data != null) {
					if (!data.isNull("id_aktivitas")) {
						String id_aktivitas = data.getString("id_aktivitas");
						session.getTransaction().begin();
						skripsi.setFeeder(id_aktivitas);
						Common.refreshUpdate(session, skripsi);
						session.getTransaction().commit();
					}
				}

			}

			if (skripsi.getFeeder() != null) {

				filter = "id_registrasi_mahasiswa='" + skripsi.getMahasiswa().getIdRegPd() + "' and id_aktivitas='"
						+ skripsi.getFeeder() + "'";
				dataAnggotaAktivitasMahasiswa = feederConnector.getData("GetListAnggotaAktivitasMahasiswa", token,
						filter, "", "1000", "");
				System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

				if (dataAnggotaAktivitasMahasiswa.length() == 0) {
					jsonObject = new JSONObject();
					jsonObject.put("id_aktivitas", skripsi.getFeeder());
					jsonObject.put("id_registrasi_mahasiswa", skripsi.getMahasiswa().getIdRegPd());
					jsonObject.put("jns_peran_mhs", "3");
					feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAnggotaAktivitasMahasiswa", jsonObject,
							errorLog, skripsi);
				}

				List<CommonVO> dataDosen = skripsi.dataDosen(true);
				int pembimbing = 1;
				int penguji = 1;
				for (CommonVO commonVO : dataDosen) {
					Dosen dosen = (Dosen) commonVO.getValueObject();
					if (dosen.getFeeder() != null) {
						String key = commonVO.getName();
						String nama1 = commonVO.getName1();
						if (key.toLowerCase().trim().contains("penguji")) {

							filter = "id_dosen='" + dosen.getFeeder() + "' and id_aktivitas='" + skripsi.getFeeder()
									+ "'";
							JSONArray getListUjiMahasiswa = feederConnector.getData("GetListUjiMahasiswa", token,
									filter, "", "1000", "");
							System.out.println("results GetListUjiMahasiswa -> " + getListUjiMahasiswa);

							if (getListUjiMahasiswa.length() == 0) {

								jsonObject = new JSONObject();
								jsonObject.put("id_aktivitas", skripsi.getFeeder());

								if (nama1 != null && !nama1.isEmpty()) {
									jsonObject.put("id_kategori_kegiatan", nama1);
								} else if (key.toLowerCase().trim().contains("ketua penguji")) {
									jsonObject.put("id_kategori_kegiatan", "110501");
								} else if (key.toLowerCase().trim().contains("anggota penguji")) {
									jsonObject.put("id_kategori_kegiatan", "110502");
								} else {
									jsonObject.put("id_kategori_kegiatan", penguji == 1 ? "110501" : "110502");
								}
								jsonObject.put("id_dosen", dosen.getFeeder());
								jsonObject.put("penguji_ke", penguji + "");

								feederConnector.insertOrUpdateRecordBaru(token, null, "InsertUjiMahasiswa", jsonObject,
										errorLog, skripsi);
							}

							penguji++;

						} else {

							filter = "id_dosen='" + dosen.getFeeder() + "' and id_aktivitas='" + skripsi.getFeeder()
									+ "'";
							JSONArray getListBimbingMahasiswa = feederConnector.getData("GetListBimbingMahasiswa",
									token, filter, "", "1000", "");
							System.out.println("results getListBimbingMahasiswa -> " + getListBimbingMahasiswa);

							if (getListBimbingMahasiswa.length() == 0) {

								jsonObject = new JSONObject();
								jsonObject.put("id_aktivitas", skripsi.getFeeder());

								if (nama1 != null && !nama1.isEmpty()) {
									jsonObject.put("id_kategori_kegiatan", nama1);
								} else {
									jsonObject.put("id_kategori_kegiatan", pembimbing == 1 ? "110404" : "110408");
								}
								jsonObject.put("id_dosen", dosen.getFeeder());
								jsonObject.put("pembimbing_ke", pembimbing + "");
								feederConnector.insertOrUpdateRecordBaru(token, null, "InsertBimbingMahasiswa",
										jsonObject, errorLog, skripsi);
							}

							pembimbing++;
						}
					} else {
						// Dosen tanpa kode Feeder JANGAN dilewati diam-diam — laporkan agar operator tahu.
						errorLog.add(NeoFeederErrorHelper.belumTerdaftar(
								commonVO.getName() != null && commonVO.getName().toLowerCase().contains("penguji")
										? "Dosen penguji"
										: "Dosen pembimbing",
								dosen == null ? "-" : dosen.getNama(),
								"Kirim data Dosen ini ke Feeder dulu (menu Dosen > Kirim ke Feeder); untuk sementara dosen ini DILEWATI"));
					}
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	public void aktivitasMahasiswaBimbingan(MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			List<String> errorLog) {
		if (mahasiswaRequestTugasAkhir.getMahasiswa() == null
				|| mahasiswaRequestTugasAkhir.getMahasiswa().getIdRegPd() == null) {
			errorLog.add("Mahasiswa " + mahasiswaRequestTugasAkhir.getMahasiswa() + " belum terdaftar");
			return;
		}
		String id_smt = mahasiswaRequestTugasAkhir.getTahunAkademik().split("/")[0]
				+ FeederExporterGenerator.digitPeriodeFeeder(mahasiswaRequestTugasAkhir.getMahasiswa(),
						mahasiswaRequestTugasAkhir.getSemester(), null);
		String idjenis = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() == null
				|| mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getJenisKegiatanMahasiswa() == null ? null
						: mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getJenisKegiatanMahasiswa()
								.getKode();

		if (idjenis == null || idjenis.trim().isEmpty()) {
			idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Laporan akhir studi");
			try {
				if (mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getJenjang().getId()
						.equals(ConstantValues.d3.getId())) {
					idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Tugas akhir");
				} else if (mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getJenjang().getId()
						.equals(ConstantValues.s2.getId())) {
					idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Tesis");
				} else if (mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getJenjang().getId()
						.equals(ConstantValues.s3.getId())) {
					idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Disertasi");
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederExporter.java:1495");
			}
		}
		Session session = HibernateUtil.currentNativeSession();
		try {

			String filter = "id_registrasi_mahasiswa='" + mahasiswaRequestTugasAkhir.getMahasiswa().getIdRegPd() + "'";
			JSONArray dataAnggotaAktivitasMahasiswa = feederConnector.getData("GetListAnggotaAktivitasMahasiswa", token,
					filter, "", "1000", "");
			System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

			String id_akt_mhs = null;
			for (int i = 0; i < dataAnggotaAktivitasMahasiswa.length(); i++) {
				JSONObject jsonObject = dataAnggotaAktivitasMahasiswa.getJSONObject(i);

				filter = "id_aktivitas='" + jsonObject.getString("id_aktivitas") + "' and id_jenis_aktivitas='"
						+ idjenis + "' and id_semester = '" + id_smt + "'";

				JSONArray dataAktivitasMahasiswa = feederConnector.getData("GetListAktivitasMahasiswa", token, filter,
						"", "1", "");
				System.out.println("results dataAktivitasMahasiswa -> " + dataAktivitasMahasiswa);

				if (dataAktivitasMahasiswa.length() > 0) {
					id_akt_mhs = jsonObject.getString("id_aktivitas");
				}
			}

			if (id_akt_mhs != null) {
				session.getTransaction().begin();
				mahasiswaRequestTugasAkhir.setFeeder(id_akt_mhs);
				Common.refreshUpdate(session, mahasiswaRequestTugasAkhir);
				session.getTransaction().commit();
			}

			JSONObject jsonObject = new JSONObject();
			jsonObject.put("jenis_anggota", "0");
			jsonObject.put("id_jenis_aktivitas", idjenis);
			jsonObject.put("id_prodi", mahasiswaRequestTugasAkhir.getMahasiswa().getJurusan().getFeeder());
			jsonObject.put("id_semester", id_smt);
			jsonObject.put("judul", mahasiswaRequestTugasAkhir.getJudul());
			jsonObject.put("keterangan", mahasiswaRequestTugasAkhir.getKeterangan());
			jsonObject.put("lokasi", mahasiswaRequestTugasAkhir.getLokasiUjian());
			jsonObject.put("sk_tugas", mahasiswaRequestTugasAkhir.getNoSk());
			if (mahasiswaRequestTugasAkhir.getTglSk() != null) {
				jsonObject.put("tanggal_sk_tugas",
						Common.databaseDateFormat.get().format(mahasiswaRequestTugasAkhir.getTglSk()));
			}

			if (mahasiswaRequestTugasAkhir.getFeeder() != null) {
				JSONObject idMhs = new JSONObject();
				idMhs.put("id_aktivitas", mahasiswaRequestTugasAkhir.getFeeder().trim());
				feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateAktivitasMahasiswa", jsonObject, errorLog,
						mahasiswaRequestTugasAkhir);
			} else {
				JSONObject hasil = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAktivitasMahasiswa",
						jsonObject, errorLog, mahasiswaRequestTugasAkhir);
				JSONObject data = ambilDataObject(hasil, errorLog, "InsertAktivitasMahasiswa");
				if (data != null) {
					if (!data.isNull("id_aktivitas")) {
						String id_aktivitas = data.getString("id_aktivitas");
						session.getTransaction().begin();
						mahasiswaRequestTugasAkhir.setFeeder(id_aktivitas);
						Common.refreshUpdate(session, mahasiswaRequestTugasAkhir);
						session.getTransaction().commit();
					}
				}
			}

			if (mahasiswaRequestTugasAkhir.getFeeder() != null) {

				filter = "id_registrasi_mahasiswa='" + mahasiswaRequestTugasAkhir.getMahasiswa().getIdRegPd()
						+ "' and id_aktivitas='" + mahasiswaRequestTugasAkhir.getFeeder() + "'";
				dataAnggotaAktivitasMahasiswa = feederConnector.getData("GetListAnggotaAktivitasMahasiswa", token,
						filter, "", "1000", "");
				System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

				if (dataAnggotaAktivitasMahasiswa.length() == 0) {
					jsonObject = new JSONObject();
					jsonObject.put("id_aktivitas", mahasiswaRequestTugasAkhir.getFeeder());
					jsonObject.put("id_registrasi_mahasiswa", mahasiswaRequestTugasAkhir.getMahasiswa().getIdRegPd());
					jsonObject.put("jns_peran_mhs", "3");
					feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAnggotaAktivitasMahasiswa", jsonObject,
							errorLog, mahasiswaRequestTugasAkhir);
				}

				List<CommonVO> dataDosen = mahasiswaRequestTugasAkhir.dataDosen(true);
				int pembimbing = 1;
				int penguji = 1;
				for (CommonVO commonVO : dataDosen) {
					Dosen dosen = (Dosen) commonVO.getValueObject();
					if (dosen.getFeeder() != null) {
						String key = commonVO.getName();
						String nama1 = commonVO.getName1();
						if (key.toLowerCase().trim().startsWith("penguji")) {

							filter = "id_dosen='" + dosen.getFeeder() + "' and id_aktivitas='"
									+ mahasiswaRequestTugasAkhir.getFeeder() + "'";
							JSONArray getListUjiMahasiswa = feederConnector.getData("GetListUjiMahasiswa", token,
									filter, "", "1000", "");
							System.out.println("results GetListUjiMahasiswa -> " + getListUjiMahasiswa);

							if (getListUjiMahasiswa.length() == 0) {

								jsonObject = new JSONObject();
								jsonObject.put("id_aktivitas", mahasiswaRequestTugasAkhir.getFeeder());
								if (nama1 != null && !nama1.isEmpty()) {
									jsonObject.put("id_kategori_kegiatan", nama1);
								} else {
									jsonObject.put("id_kategori_kegiatan", "110500");
								}
								jsonObject.put("id_dosen", dosen.getFeeder());
								jsonObject.put("penguji_ke", penguji + "");

								feederConnector.insertOrUpdateRecordBaru(token, null, "InsertUjiMahasiswa", jsonObject,
										errorLog, mahasiswaRequestTugasAkhir);
							}

							penguji++;

						} else {

							filter = "id_dosen='" + dosen.getFeeder() + "' and id_aktivitas='"
									+ mahasiswaRequestTugasAkhir.getFeeder() + "'";
							JSONArray getListBimbingMahasiswa = feederConnector.getData("GetListBimbingMahasiswa",
									token, filter, "", "1000", "");
							System.out.println("results getListBimbingMahasiswa -> " + getListBimbingMahasiswa);

							if (getListBimbingMahasiswa.length() == 0) {

								jsonObject = new JSONObject();
								jsonObject.put("id_aktivitas", mahasiswaRequestTugasAkhir.getFeeder());
								if (nama1 != null && !nama1.isEmpty()) {
									jsonObject.put("id_kategori_kegiatan", nama1);
								} else {
									jsonObject.put("id_kategori_kegiatan", "110400");
								}
								jsonObject.put("id_dosen", dosen.getFeeder());
								jsonObject.put("pembimbing_ke", pembimbing + "");
								feederConnector.insertOrUpdateRecordBaru(token, null, "InsertBimbingMahasiswa",
										jsonObject, errorLog, mahasiswaRequestTugasAkhir);
							}

							pembimbing++;
						}
					} else {
						// Dosen tanpa kode Feeder JANGAN dilewati diam-diam — laporkan agar operator tahu.
						errorLog.add(NeoFeederErrorHelper.belumTerdaftar(commonVO.getName() != null && commonVO.getName().toLowerCase().contains("penguji")
								? "Dosen penguji" : "Dosen pembimbing",
							dosen == null ? "-" : dosen.getNama(),
							"Kirim data Dosen ini ke Feeder dulu (menu Dosen > Kirim ke Feeder); untuk sementara dosen ini DILEWATI"));
					}
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	public void aktivitasMahasiswaKrs(KrsMahasiswa krsMahasiswa, List<String> errorLog) {
		if (krsMahasiswa.getSemesterPendek() == null) {
			Dosen dosen = krsMahasiswa.getDosenPa();
			if (dosen == null || dosen.getFeeder() == null) {
				errorLog.add("Dosen " + dosen + " belum terdaftar");
				return;
			}
			if (krsMahasiswa.getMahasiswa() == null || krsMahasiswa.getMahasiswa().getIdRegPd() == null) {
				errorLog.add("Mahasiswa " + krsMahasiswa.getMahasiswa() + " belum terdaftar");
				return;
			}
			// Digit periode HARUS ikut semester masuk mahasiswa (mahasiswa masuk GENAP: semester
			// ganjil jatuh di periode genap). Tanpa ini, angkatan genap salah ke ganjil -> "di luar periode".
			String id_smt = krsMahasiswa.getTahunAkademik().split("/")[0]
					+ FeederExporterGenerator.digitPeriodeFeeder(krsMahasiswa.getMahasiswa(),
							krsMahasiswa.getSemester(), krsMahasiswa.getSemesterPendek());
			String idjenis = FeederJSONImport.JENIS_KEGIATAN.get("Bimbingan akademis");
			Session session = HibernateUtil.currentNativeSession();
			String judul = "Bimbingan / konsultasi akademik \"" + krsMahasiswa.getMahasiswa().getNim() + "\" \""
					+ krsMahasiswa.getMahasiswa().getNama() + "\" TA:" + krsMahasiswa.getTahunAkademik() + " SMT:"
					+ krsMahasiswa.getSemester();
			try {

				String filter = "id_registrasi_mahasiswa='" + krsMahasiswa.getMahasiswa().getIdRegPd() + "'";
				JSONArray dataAnggotaAktivitasMahasiswa = feederConnector.getData("GetListAnggotaAktivitasMahasiswa",
						token, filter, "", "1000", "");
				System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

				String id_akt_mhs = null;
				for (int i = 0; i < dataAnggotaAktivitasMahasiswa.length(); i++) {
					JSONObject jsonObject = dataAnggotaAktivitasMahasiswa.getJSONObject(i);

					filter = "id_aktivitas='" + jsonObject.getString("id_aktivitas") + "' and id_jenis_aktivitas='"
							+ idjenis + "' and id_semester = '" + id_smt + "'";

					JSONArray dataAktivitasMahasiswa = feederConnector.getData("GetListAktivitasMahasiswa", token,
							filter, "", "1", "");
					System.out.println("results dataAktivitasMahasiswa -> " + dataAktivitasMahasiswa);

					if (dataAktivitasMahasiswa.length() > 0) {
						id_akt_mhs = jsonObject.getString("id_aktivitas");
					}
				}

				if (id_akt_mhs != null) {
					session.getTransaction().begin();
					krsMahasiswa.setFeeder(id_akt_mhs);
					Common.refreshUpdate(session, krsMahasiswa);
					session.getTransaction().commit();
				}

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("jenis_anggota", "1");
				jsonObject.put("id_jenis_aktivitas", idjenis);
				jsonObject.put("id_prodi", krsMahasiswa.getMahasiswa().getJurusan().getFeeder());
				jsonObject.put("id_semester", id_smt);
				jsonObject.put("judul", judul);
				jsonObject.put("keterangan", krsMahasiswa.getKeterangan());
				jsonObject.put("lokasi", "");
				jsonObject.put("sk_tugas", krsMahasiswa.getNoSk());
				if (krsMahasiswa.getTglSk() != null) {
					jsonObject.put("tanggal_sk_tugas", Common.databaseDateFormat.get().format(krsMahasiswa.getTglSk()));
				}

				List<String> errorLogAk = new ArrayList<String>();
				if (krsMahasiswa.getFeeder() != null) {
					JSONObject idMhs = new JSONObject();
					idMhs.put("id_aktivitas", krsMahasiswa.getFeeder().trim());

					feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateAktivitasMahasiswa", jsonObject,
							errorLogAk, krsMahasiswa);
				}

				System.out.println("jsonObject -> " + jsonObject + " errorLogAk " + errorLogAk + " feeder "
						+ krsMahasiswa.getFeeder());

				if (!errorLogAk.isEmpty() || krsMahasiswa.getFeeder() == null) {
					JSONObject hasil = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAktivitasMahasiswa",
							jsonObject, errorLog, krsMahasiswa);
					JSONObject data = ambilDataObject(hasil, errorLog, "InsertAktivitasMahasiswa");
					if (data != null) {
						if (!data.isNull("id_aktivitas")) {
							String id_aktivitas = data.getString("id_aktivitas");
							session.getTransaction().begin();
							krsMahasiswa.setFeeder(id_aktivitas);
							Common.refreshUpdate(session, krsMahasiswa);
							session.getTransaction().commit();
						}
					}
				}

				if (krsMahasiswa.getFeeder() != null) {

					filter = "id_registrasi_mahasiswa='" + krsMahasiswa.getMahasiswa().getIdRegPd()
							+ "' and id_aktivitas='" + krsMahasiswa.getFeeder() + "'";
					dataAnggotaAktivitasMahasiswa = feederConnector.getData("GetListAnggotaAktivitasMahasiswa", token,
							filter, "", "1000", "");
					System.out.println("results dataAnggotaAktivitasMahasiswa -> " + dataAnggotaAktivitasMahasiswa);

					if (dataAnggotaAktivitasMahasiswa.length() == 0) {
						jsonObject = new JSONObject();
						jsonObject.put("id_aktivitas", krsMahasiswa.getFeeder());
						jsonObject.put("id_registrasi_mahasiswa", krsMahasiswa.getMahasiswa().getIdRegPd());
						jsonObject.put("jns_peran_mhs", "3");
						feederConnector.insertOrUpdateRecordBaru(token, null, "InsertAnggotaAktivitasMahasiswa",
								jsonObject, errorLog, krsMahasiswa);
					}

					filter = "id_dosen='" + dosen.getFeeder() + "' and id_aktivitas='" + krsMahasiswa.getFeeder() + "'";
					JSONArray getListBimbingMahasiswa = feederConnector.getData("GetListBimbingMahasiswa", token,
							filter, "", "1000", "");
					System.out.println("results getListBimbingMahasiswa -> " + getListBimbingMahasiswa);

					if (getListBimbingMahasiswa.length() == 0) {

						jsonObject = new JSONObject();
						jsonObject.put("id_aktivitas", krsMahasiswa.getFeeder());
						jsonObject.put("id_kategori_kegiatan", "110601");
						jsonObject.put("id_dosen", dosen.getFeeder());
						jsonObject.put("pembimbing_ke", "1");
						feederConnector.insertOrUpdateRecordBaru(token, null, "InsertBimbingMahasiswa", jsonObject,
								errorLog, krsMahasiswa);
					}

				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
	}

	public void matakuliah() {
		matakuliah(null);
	}

	public void matakuliah(List<String> errorLog) {
		@SuppressWarnings("unchecked")
		List<Matakuliah> matakuliahs;
		Session session = HibernateUtil.currentNativeSession();
		try {
			matakuliahs = session.createCriteria(Matakuliah.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ne("kode", "")).add(Restrictions.ne("nama", "")).createAlias("jurusan", "jurusan")
				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", "")).list();
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
		System.out.println("matakuliahs size => " + matakuliahs.size());

		int index = 0;
		for (Matakuliah matakuliah : matakuliahs) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / matakuliahs.size()));
				index++;
			}

			matakuliah(matakuliah, errorLog);
		}

	}

	public void kelas_kuliah(Perkuliahan perkuliahan, List<String> errorLog, int coba) {
		if (perkuliahan.getMatakuliah() == null) {
			if (errorLog != null) {
				errorLog.add("[" + perkuliahan.info() + "] Matakuliah kosong pada data perkuliahan ini — "
						+ "kelas kuliah & nilai mahasiswa TIDAK dikirim.");
			}
			return;
		}
		if (perkuliahan.getJurusan() == null || perkuliahan.getJurusan().getFeeder() == null
				|| perkuliahan.getJurusan().getFeeder().trim().isEmpty()) {
			if (errorLog != null) {
				errorLog.add("[" + perkuliahan.info() + "] Prodi " + perkuliahan.getJurusan()
						+ " belum tersingkron ke Feeder — kelas kuliah & nilai mahasiswa TIDAK dikirim.");
			}
			return;
		}

		// Matakuliah belum tertaut ke Feeder (id_matkul lokal masih kosong): SEBELUM menyerah, coba
		// selesaikan otomatis via 3 TAHAP (lihat javadoc {@link #matakuliah(Matakuliah, List)}):
		// (1) cari di Feeder berdasarkan KODE + PRODI yang sama, (2) bila tak ketemu, cari
		// berdasarkan KODE SAJA (prodi apa pun), (3) bila tetap tak ketemu, INSERT matakuliah baru
		// ke Feeder. Baru SETELAH itu lanjut mengirim kelas kuliah & nilai mahasiswa.
		if (perkuliahan.getMatakuliah().getFeeder() == null
				|| perkuliahan.getMatakuliah().getFeeder().trim().isEmpty()) {
			matakuliah(perkuliahan.getMatakuliah(), errorLog != null ? errorLog : new ArrayList<String>());
			if (perkuliahan.getMatakuliah().getFeeder() == null
					|| perkuliahan.getMatakuliah().getFeeder().trim().isEmpty()) {
				if (errorLog != null) {
					errorLog.add("[" + perkuliahan.info() + "] Matakuliah " + perkuliahan.getMatakuliah()
							+ " GAGAL disinkron ke Feeder walau sudah dicoba: (1) cari kode+prodi sama, "
							+ "(2) cari kode saja (prodi lain), (3) insert matakuliah baru. Kelas kuliah & "
							+ "nilai mahasiswa TIDAK dikirim untuk kelas ini. Lihat pesan error di atas "
							+ "untuk detail penyebab kegagalan tiap tahap.");
				}
				return;
			}
		}

		// AUTO-FIX error Feeder 631 "Data mata kuliah kurikulum ini tidak ada": InsertKelasKuliah
		// mensyaratkan relasi Kurikulum<->Matakuliah (MatkulKurikulum) SUDAH terdaftar di Feeder.
		// Bila relasinya ADA di eCampus (KurikulumPunyaMatakuliah) tapi belum/tidak lagi tersinkron
		// di Feeder, kirim dulu relasi itu (idempoten — kurikulumPunyaMatakuliah() sendiri sudah
		// cek-lalu-insert/update via GetMatkulKurikulum) SEBELUM mengirim kelas kuliah. Bila relasi
		// bahkan tidak ada di eCampus, ini murni data lokal belum lengkap — beri pesan jelas
		// (bukan pesan mentah "Data mata kuliah kurikulum ini tidak ada" dari Feeder yang membingungkan).
		if (perkuliahan.getKurikulum() != null && perkuliahan.getMatakuliah() != null) {
			try {
				Session sesCek = HibernateUtil.currentNativeSession();
				KurikulumPunyaMatakuliah kpm = null;
				try {
					kpm = (KurikulumPunyaMatakuliah) sesCek.createCriteria(KurikulumPunyaMatakuliah.class)
							.add(Restrictions.eq("kurikulum", perkuliahan.getKurikulum()))
							.add(Restrictions.eq("matakuliah", perkuliahan.getMatakuliah())).setMaxResults(1)
							.uniqueResult();
				} finally {
					if (sesCek.isOpen()) {
						sesCek.disconnect();
						sesCek.close();
					}
					HibernateUtil.closeSession();
				}
				if (kpm != null) {
					kurikulumPunyaMatakuliah(kpm, errorLog != null ? errorLog : new ArrayList<String>());
				} else if (errorLog != null) {
					errorLog.add("[Prasyarat Kelas Kuliah] Matakuliah \"" + perkuliahan.getMatakuliah()
							+ "\" belum terdaftar pada Kurikulum \"" + perkuliahan.getKurikulum()
							+ "\" di eCampus (tabel KurikulumPunyaMatakuliah). Tambahkan matakuliah ini ke "
							+ "kurikulum yang bersangkutan lebih dulu, baru kirim ulang kelas kuliah.");
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		JSONObject jsonObject = FeederExporterGenerator.perkuliahan(perkuliahan);
		Session session = HibernateUtil.currentNativeSession();
		try {

			String kls = perkuliahan.getKelas();
			if (Common.bolehKonfigurasi("kelas_digabung_dengan_semester_saat_export_feeder", Konfigurasi.TIDAK_AKTIF)) {
				kls = Common.maxPanjangAkhir(perkuliahan.getSemester() + " " + perkuliahan.getKelas(), 5);
			}

			if (Common.bolehKonfigurasi("kelas_digabung_dengan_semester_saat_export_feeder_tanpa_spasi", Konfigurasi.TIDAK_AKTIF)) {
				kls = Common.maxPanjangAkhir(perkuliahan.getSemester() + "" + perkuliahan.getKelas(), 5);
			}

			String filter = "id_prodi='" + perkuliahan.getJurusan().getFeeder() + "' and id_matkul='"
					+ perkuliahan.getMatakuliah().getFeeder() + "' AND upper(trim(nama_kelas_kuliah))=upper(trim('"
					+ Common.maxPanjangAkhir(kls, 5) + "')) AND id_semester='" + jsonObject.getString("id_semester")
					+ "'";
//			Matakuliah matakuliah = perkuliahan.getMatakuliah();

//			String filter = "id_prodi='" + perkuliahan.getJurusan().getFeeder()
//					+ "' and regexp_replace(trim(kode_mata_kuliah), '[^a-zA-Z]', '', 'g')=regexp_replace('"
//					+ matakuliah.getKode() + "', '[^a-zA-Z]', '', 'g')"
//					+ " AND upper(trim(nama_kelas_kuliah))=upper(trim('" + Common.maxPanjangAkhir(kls, 5)
//					+ "')) AND id_semester='" + perkuliahan.getIdSmt() + "'";

			JSONArray dataDetailKelasKuliah = feederConnector.getData("GetDetailKelasKuliah", token, filter, "", "1",
					"");
			System.out.println("results dataDetailKelasKuliah -> " + dataDetailKelasKuliah);

			if (dataDetailKelasKuliah.length() > 0) {
				JSONObject a = dataDetailKelasKuliah.getJSONObject(0);
				String id_kelas_kuliah = a.getString("id_kelas_kuliah").trim();
				System.out.println("id_kelas_kuliah = " + id_kelas_kuliah);
				if (id_kelas_kuliah != null && !id_kelas_kuliah.isEmpty()) {
					perkuliahan.setFeeder(id_kelas_kuliah);
					try {
						session.getTransaction().begin();
						Common.refreshUpdate(session, perkuliahan);
						session.getTransaction().commit();
					} catch (org.hibernate.exception.ConstraintViolationException cvEx) {
						try { session.getTransaction().rollback(); } catch (Exception exRb) { ais.common.ErrorAuditUtil.record(exRb, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:1885"); /* ignore */ }
						ais.common.Common.tampilErrorJikaAdmin(cvEx);
					}
				}
				JSONObject idMhs = new JSONObject();
				idMhs.put("id_kelas_kuliah", id_kelas_kuliah);

				feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateKelasKuliah", jsonObject, errorLog,
						perkuliahan);
			} else {
				JSONObject a = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertKelasKuliah", jsonObject,
						errorLog, perkuliahan);
				String id_kelas_kuliah = ambilNilaiData(a, "id_kelas_kuliah");
				System.out.println("id_kelas_kuliah = " + id_kelas_kuliah);
				if (id_kelas_kuliah != null && !id_kelas_kuliah.isEmpty()) {
					perkuliahan.setFeeder(id_kelas_kuliah);
					try {
						session.getTransaction().begin();
						Common.refreshUpdate(session, perkuliahan);
						session.getTransaction().commit();
					} catch (org.hibernate.exception.ConstraintViolationException cvEx) {
						try { session.getTransaction().rollback(); } catch (Exception exRb) { ais.common.ErrorAuditUtil.record(exRb, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:1907"); /* ignore */ }
						ais.common.Common.tampilErrorJikaAdmin(cvEx);
					}
				}

				if (coba == 0) {
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					HibernateUtil.closeSession();
					kelas_kuliah(perkuliahan, errorLog, 1);
					return;
				}
			}

		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive())
					session.getTransaction().rollback();
			} catch (Exception exRb) { ais.common.ErrorAuditUtil.record(exRb, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:1928"); /* ignore */ }
			Common.tampilErrorJikaAdmin(e);
		}
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();

		if (perkuliahan.getFeeder() == null && coba == 0) {
			kelas_kuliah(perkuliahan, errorLog, 1);
		} else {
			kirimkanDosen(perkuliahan, errorLog);
		}

		if (perkuliahan.getFeeder() != null && !perkuliahan.getFeeder().isEmpty()) {
			kirimkanKomponene(perkuliahan);
		}

	}

	public void kirimkanDosen(Perkuliahan perkuliahan, List<String> errorLog) {
		Integer jumlahPertemuan = perkuliahan.ambilJumlahPertemuan(true);
		int i = 1;
		for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
			try {

				Session session = HibernateUtil.currentNativeSession();
				try {
					FeederExporterHelper.ajar_dosen(session, feederConnector, token, dosen, perkuliahan,
							jumlahPertemuan, i, errorLog);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				HibernateUtil.closeSession();

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			i++;
		}
	}

	public void kirimkanKomponene(final Perkuliahan perkuliahan) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

				List<Detailperkuliahan> detailperkuliahans = new ArrayList<Detailperkuliahan>();
				for (Long detailperkuliahanid : perkuliahan.ambilDetailperkuliahan()) {
					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
							.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
					if (detailperkuliahan != null) {
						detailperkuliahans.add(detailperkuliahan);
					}
				}

				Session session = HibernateUtil.currentNativeSession();
				try {

					JSONArray dataGetListKomponenEvaluasiKelasAll = feederConnector.getData(
							"GetListKomponenEvaluasiKelas", token, "id_kelas_kuliah='" + perkuliahan.getFeeder() + "'",
							"", "100", "");
					for (int i = 0; i < dataGetListKomponenEvaluasiKelasAll.length(); i++) {
						JSONObject komponenEvaluasiKelas = dataGetListKomponenEvaluasiKelasAll.getJSONObject(i);
						System.out.println("komponenEvaluasiKelas -> " + komponenEvaluasiKelas);
						List<String> errorLogTemp = new ArrayList<String>();
						JSONObject key = new JSONObject();
						key.put("id_kelas_kuliah", perkuliahan.getFeeder());
						key.put("id_jenis_evaluasi", komponenEvaluasiKelas.get("id_jenis_evaluasi") + "");
						key.put("nama", komponenEvaluasiKelas.get("nama") + "");
						JSONObject deleteKomponenEvaluasiKelas = feederConnector.insertOrUpdateRecordBaru(token, key,
								"DeleteKomponenEvaluasiKelas", null, errorLogTemp, perkuliahan);

						System.out.println("hapus -> DeleteKomponenEvaluasiKelas " + deleteKomponenEvaluasiKelas
								+ ", errorLogTemp " + errorLogTemp);
					}

					List<FormatNilai> formatNilais = Common.getFormatNilais(perkuliahan, false);

					int urut = 1;
					for (FormatNilai formatNilai : formatNilais) {
						try {
							int nomorUrut = formatNilai.getNomorUrut() == null ? urut : formatNilai.getNomorUrut();
							urut++;
							JSONObject jsonObject = FeederExporterGenerator.formatNilai(formatNilai, nomorUrut);

							JSONArray dataGetListKomponenEvaluasiKelas = null;

							dataGetListKomponenEvaluasiKelas = feederConnector
									.getData(
											"GetListKomponenEvaluasiKelas", token, "id_kelas_kuliah='"
													+ perkuliahan.getFeeder() + "' and nomor_urut=" + nomorUrut + "",
											"", "1", "");

							System.out.println(
									"results dataGetListKomponenEvaluasiKelas -> " + dataGetListKomponenEvaluasiKelas);
							String id_komponen_evaluasi = null;
							if (dataGetListKomponenEvaluasiKelas != null
									&& dataGetListKomponenEvaluasiKelas.length() > 0) {
								JSONObject a = dataGetListKomponenEvaluasiKelas.getJSONObject(0);
								id_komponen_evaluasi = a.getString("id_komponen_evaluasi").trim();
								if (id_komponen_evaluasi != null && !id_komponen_evaluasi.isEmpty()) {
									formatNilai.setFeeder(id_komponen_evaluasi);
									// FIX "Session is closed!": session dipakai lintas-iterasi loop diselingi
									// banyak panggilan jaringan (bisa lama) — re-acquire bila sudah tertutup.
									if (session == null || !session.isOpen()) {
										session = HibernateUtil.currentNativeSession();
									}
									session.getTransaction().begin();
									Common.refreshUpdate(session, formatNilai);
									session.getTransaction().commit();
								}
								JSONObject idMhs = new JSONObject();
								idMhs.put("id_komponen_evaluasi", id_komponen_evaluasi);

								List<String> errorLogTemp = new ArrayList<String>();
								feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateKomponenEvaluasiKelas",
										jsonObject, errorLogTemp, formatNilai);
							} else {
								List<String> errorLogTemp = new ArrayList<String>();
								JSONObject a = feederConnector.insertOrUpdateRecordBaru(token, null,
										"InsertKomponenEvaluasiKelas", jsonObject, errorLogTemp, formatNilai);
								id_komponen_evaluasi = ambilNilaiData(a, "id_komponen_evaluasi");

								if (id_komponen_evaluasi != null && !id_komponen_evaluasi.isEmpty()) {
									formatNilai.setFeeder(id_komponen_evaluasi);
									// FIX "Session is closed!": re-acquire bila session sudah tertutup
									// (loop panjang diselingi banyak panggilan jaringan).
									if (session == null || !session.isOpen()) {
										session = HibernateUtil.currentNativeSession();
									}
									session.getTransaction().begin();
									Common.refreshUpdate(session, formatNilai);
									session.getTransaction().commit();
								} else if (!errorLogTemp.isEmpty()) {
									jsonObject.put("bobot_evaluasi", "0.0");
									feederConnector.insertOrUpdateRecordBaru(token, null, "InsertKomponenEvaluasiKelas",
											jsonObject, errorLogTemp, formatNilai);
									id_komponen_evaluasi = ambilNilaiData(a, "id_komponen_evaluasi");

									if (id_komponen_evaluasi != null && !id_komponen_evaluasi.isEmpty()) {
										formatNilai.setFeeder(id_komponen_evaluasi);
										if (session == null || !session.isOpen()) {
											session = HibernateUtil.currentNativeSession();
										}
										session.getTransaction().begin();
										Common.refreshUpdate(session, formatNilai);
										session.getTransaction().commit();
									}

								}

								System.out.println("errorLogTemp -> " + errorLogTemp);
							}

							System.out.println("id_komponen_evaluasi = " + id_komponen_evaluasi);
							if (id_komponen_evaluasi != null && !id_komponen_evaluasi.isEmpty()) {
								for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
									Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
									if (mahasiswa != null && mahasiswa.getIdRegPd() != null
											&& !mahasiswa.getIdRegPd().isEmpty()) {
										Double jumlah = detailperkuliahan.retreiveDetailNilaiBelumVerify(formatNilai);
										JSONObject data = new JSONObject();
										data.put("nilai_komponen_evaluasi", jumlah + "");

										JSONObject idMhs = new JSONObject();
										idMhs.put("id_komponen_evaluasi", id_komponen_evaluasi);
										idMhs.put("id_registrasi_mahasiswa", mahasiswa.getIdRegPd());
										ArrayList<String> errorLogTemp = new ArrayList<String>();
										feederConnector.insertOrUpdateRecordBaru(token, idMhs,
												"UpdateNilaiPerkuliahanKelasKomponenEvaluasi", data, errorLogTemp,
												formatNilai);
										System.out.println("UpdateNilaiPerkuliahanKelasKomponenEvaluasi\n"+data+"\n"+idMhs+"\nerrorLogTemp -> " + errorLogTemp);

									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederExporter.java:2104");
						}
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederExporter.java:2109");
				}
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				HibernateUtil.closeSession();

							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		}).start();
	}

	public void kelas_kuliah() {
		kelas_kuliah(null);
	}

	public void kelas_kuliah(List<String> errorLog) {
		@SuppressWarnings("unchecked")
		List<Perkuliahan> perkuliahans;
		Session session = HibernateUtil.currentNativeSession();
		try {
			perkuliahans = session.createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("jurusan", "jurusan").add(Restrictions.isNotNull("jurusan.feeder"))
				.add(Restrictions.ne("jurusan.feeder", "")).list();
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
		System.out.println("perkuliahans size => " + perkuliahans.size());

		int index = 0;
		for (Perkuliahan perkuliahan : perkuliahans) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / perkuliahans.size()));
				index++;
			}

			kelas_kuliah(perkuliahan, errorLog, 0);
		}

	}

	public void ajar_dosen() {
		ajar_dosen(null);
	}

	public void ajar_dosen(List<String> errorLog) {
		Session session = HibernateUtil.currentNativeSession();
		try {
		@SuppressWarnings("unchecked")
		List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("jurusan", "jurusan").add(Restrictions.isNotNull("feeder"))
				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", "")).list();

		System.out.println("perkuliahans size => " + perkuliahans.size());

		int index = 0;
		for (Perkuliahan perkuliahan : perkuliahans) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / perkuliahans.size()));
				index++;
			}

			Dosen[] dosens = new Dosen[] { perkuliahan.getDosen1(), perkuliahan.getDosen2(), perkuliahan.getDosen3(),
					perkuliahan.getDosen4(), perkuliahan.getDosen5(), perkuliahan.getDosen6(), perkuliahan.getDosen7(),
					perkuliahan.getDosen8(), perkuliahan.getDosen9(), perkuliahan.getDosen10() };

			int i = 1;
			for (Dosen dosen : dosens) {
				if (dosen != null) {
					FeederExporterHelper.ajar_dosen(session, feederConnector, token, perkuliahan, i, errorLog);
				}
				i++;
			}
		}

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void mahasiswa(Mahasiswa mahasiswa) {
		mahasiswa(mahasiswa, null);
	}

	@SuppressWarnings("unchecked")
	public void mahasiswa(Mahasiswa mahasiswa, List<String> errorLog) {

		BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

		try {

			String filter = "upper(trim(nama_mahasiswa)) = upper(trim('" + mahasiswa.getNama()
					+ "')) and tanggal_lahir='" + Common.dateFormat1.get().format(mahasiswa.getTanggallahir())
					+ "' and upper(trim(nama_ibu_kandung))=upper(trim('" + biodataMahasiswa.getNamaIbu() + "'))";

			JSONArray dataMhs = feederConnector.getData("GetBiodataMahasiswa", token, filter, "", "1", "");
			System.out.println("results mahasiswa -> " + dataMhs);
			if (dataMhs.length() == 0) {

				JSONObject jsonObjectq = FeederResource.convertMahasiswa(mahasiswa);
				JSONObject a = feederConnector.insertOrUpdateRecordBaru(token, null, "InsertBiodataMahasiswa",
						jsonObjectq, errorLog, mahasiswa);

				String id_mahasiswa = ambilNilaiData(a, "id_mahasiswa");
				System.out.println("id_mahasiswa = " + id_mahasiswa);
				if (id_mahasiswa != null && !id_mahasiswa.isEmpty()) {
					mahasiswa.setFeeder(id_mahasiswa);
					Session session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					Common.refreshUpdate(session, mahasiswa);
					session.getTransaction().commit();
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					HibernateUtil.closeSession();
				}

			} else {

				JSONObject mhs = dataMhs.getJSONObject(0);
				String id_mahasiswa = mhs.getString("id_mahasiswa");
				if (mahasiswa.getFeeder() == null || !mahasiswa.getFeeder().equalsIgnoreCase(id_mahasiswa)) {
					mahasiswa.setFeeder(id_mahasiswa);
					Session session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					Common.refreshUpdate(session, mahasiswa);
					session.getTransaction().commit();
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					HibernateUtil.closeSession();
				}

				JSONObject idMhs = new JSONObject();
				idMhs.put("id_mahasiswa", mahasiswa.getFeeder().trim());
				JSONObject jsonObjectq = FeederResource.convertMahasiswa(mahasiswa);
				Iterator<String> keys = jsonObjectq.keys();
				List<String> errorLogData = new ArrayList<String>();
				while (keys.hasNext()) {
					String d = keys.next();
					if (d.equalsIgnoreCase("nama_mahasiswa") || d.equalsIgnoreCase("tempat_lahir")
							|| d.equalsIgnoreCase("tanggal_lahir") || d.equalsIgnoreCase("nama_ibu_kandung")) {
						continue;
					}
					JSONObject data = new JSONObject();
					data.put(d, jsonObjectq.get(d));
					feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateBiodataMahasiswa", data, errorLogData,
							mahasiswa);
				}
				System.out.println("errorLogData -> " + errorLogData);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public void mahasiswa() {
		@SuppressWarnings("unchecked")
		List<Mahasiswa> mahasiswas;
		Session session = HibernateUtil.currentNativeSession();
		try {
			mahasiswas = session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ne("nim", "")).add(Restrictions.ne("nama", "")).createAlias("jurusan", "jurusan")
				.createAlias("jurusan.fakultas", "fakultas").createAlias("fakultas.perguruanTinggi", "perguruanTinggi")
				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", ""))
				.add(Restrictions.isNotNull("perguruanTinggi.feeder"))
				.add(Restrictions.ne("perguruanTinggi.feeder", "")).addOrder(Order.desc("nim")).list();
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
		System.out.println("mahasiswas size => " + mahasiswas.size());

		int index = 0;
		for (Mahasiswa mahasiswa : mahasiswas) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / mahasiswas.size()));
				index++;
			}

			mahasiswa(mahasiswa);
		}

	}

	public void mahasiswa_pt(Mahasiswa mahasiswa, List<String> errorLog) {
		Session session = HibernateUtil.currentNativeSession();
		try {
		FeederExporterHelper.mahasiswa_pt(session, feederConnector, token, mahasiswa, errorLog);
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void mahasiswa_pt() {
		mahasiswa_pt(null);
	}

	public void mahasiswa_pt(List<String> errorLog) {
		@SuppressWarnings("unchecked")
		List<Mahasiswa> mahasiswas;
		Session session = HibernateUtil.currentNativeSession();
		try {
			mahasiswas = session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("feeder")).add(Restrictions.ne("feeder", ""))
				.add(Restrictions.ne("nim", "")).add(Restrictions.ne("nama", "")).createAlias("jurusan", "jurusan")
				.createAlias("jurusan.fakultas", "fakultas").createAlias("fakultas.perguruanTinggi", "perguruanTinggi")
				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", ""))
				.add(Restrictions.isNotNull("perguruanTinggi.feeder"))
				.add(Restrictions.ne("perguruanTinggi.feeder", "")).addOrder(Order.desc("nim")).list();
		} finally {
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
		System.out.println("mahasiswas size => " + mahasiswas.size());

		int index = 0;
		for (Mahasiswa mahasiswa : mahasiswas) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / mahasiswas.size()));
				index++;
			}

			mahasiswa_pt(mahasiswa, errorLog);
		}

	}

	public void dosen() {
		Session session = HibernateUtil.currentNativeSession();
		try {
		@SuppressWarnings("unchecked")
		List<BiodataDosen> dosens = session.createCriteria(BiodataDosen.class).createCriteria("dosen")
				.add(Restrictions.ne("nama", "")).add(Restrictions.ne("feeder", ""))
				.add(Restrictions.isNotNull("feeder")).list();

		System.out.println("dosens size => " + dosens.size());

		for (BiodataDosen biodataDosen : dosens) {
			Dosen dosen = biodataDosen.getDosen();

			Map<String, Object> data = new HashMap<String, Object>();
			if (dosen.getIkatanKerjaDosen() != null) {
				data.put("id_ikatan_kerja", dosen.getIkatanKerjaDosen().getFeeder());
			}
			data.put("nm_ptk", Common.maxPanjang(dosen.getNama(), 50));
			data.put("nidn", Common.maxPanjang(dosen.getNidn(), 10));
			data.put("nip", Common.maxPanjang(dosen.getCode(), 18));

			data.put("jk",
					dosen.getKelamin() == null ? "*" : dosen.getKelamin().equalsIgnoreCase("Laki-laki") ? "L" : "P");
			data.put("tmpt_lahir", Common.maxPanjang(dosen.getTempatlahir(), 20));

			if (dosen.getTanggallahir() != null) {
				data.put("tgl_lahir", Common.databaseDateFormat.get().format(dosen.getTanggallahir()));
			}

			data.put("nik", Common.maxPanjang(dosen.getKtp(), 16));
			data.put("niy_nigk", Common.maxPanjang(dosen.getNiyNigk(), 30));
			data.put("nuptk", Common.maxPanjang(dosen.getKtp(), 16));

			if (dosen.getStatusKepegawaian() != null) {
				data.put("id_stat_pegawai", dosen.getStatusKepegawaian().getFeeder());
			}

			if (dosen.getJenisPendidikDanTenagaKependidikan() != null) {
				data.put("id_jns_ptk", dosen.getJenisPendidikDanTenagaKependidikan().getFeeder());
			}

			// data.put("id_bid_pengawas",
			// Common.maxPanjang(dosen.getid_bid_pengawas, 16));

			if (biodataDosen.getAgama() != null) {
				data.put("id_agama", biodataDosen.getAgama().getFeeder());
			} else {
				data.put("id_agama", 1);
			}

			data.put("jln", Common.maxPanjang(dosen.getAlamat(), 80));

			data.put("rt", Common.maxPanjangNumeric(biodataDosen.getRt(), 2));
			data.put("rw", Common.maxPanjangNumeric(biodataDosen.getRw(), 2));
			data.put("nm_dsn", Common.maxPanjang(biodataDosen.getDusun(), 40));
			data.put("ds_kel", Common.maxPanjang(biodataDosen.getKelurahan(), 40));

			if (biodataDosen.getKecamatan() != null && biodataDosen.getKecamatan().getFeeder() != null
					&& !biodataDosen.getKecamatan().getFeeder().trim().isEmpty()) {
				data.put("id_wil", biodataDosen.getKecamatan().getFeeder());
			} else {
				data.put("id_wil", "000000");
			}
			data.put("kode_pos", Common.maxPanjangNumeric(biodataDosen.getKodepos(), 5));

			data.put("no_tel_rmh", Common.maxPanjangNumeric(biodataDosen.getTeleponRumah(), 20));
			data.put("no_hp", Common.maxPanjangNumeric(biodataDosen.getHp(), 20));
			data.put("email", Common.maxPanjang(dosen.getEmail(), 50));

			if (dosen.getPerguruanTinggi() != null && dosen.getPerguruanTinggi().getFeeder() != null) {
				data.put("id_sp", dosen.getPerguruanTinggi().getFeeder());
			}

			if (dosen.getStatusPegawai() != null && dosen.getStatusPegawai().getFeeder() != null) {
				data.put("id_stat_aktif", dosen.getStatusPegawai().getFeeder());
			}

			data.put("sk_cpns", Common.maxPanjang(dosen.getSkCpns(), 40));

			if (dosen.getTglSkCpns() != null) {
				data.put("tgl_sk_cpns", Common.databaseDateFormat.get().format(dosen.getTglSkCpns()));
			}
			data.put("sk_angkat", Common.maxPanjang(dosen.getSkAngkat(), 40));

			if (dosen.getTmtSkAngkat() != null) {
				data.put("tmt_sk_angkat", Common.databaseDateFormat.get().format(dosen.getTmtSkAngkat()));
			}

			if (dosen.getLembagaPengangkat() != null && dosen.getLembagaPengangkat().getFeeder() != null) {
				data.put("id_lemb_angkat", dosen.getLembagaPengangkat().getFeeder());
			}

			if (dosen.getGolonganPegawai() != null && dosen.getGolonganPegawai().getFeeder() != null) {
				data.put("id_pangkat_gol", dosen.getGolonganPegawai().getFeeder());
			}

			// data.put("id_keahlian_lab", Common.maxPanjang(dosen.getEmail(),
			// 50));

			if (dosen.getSumberGaji() != null && dosen.getSumberGaji().getFeeder() != null) {
				data.put("id_sumber_gaji", dosen.getSumberGaji().getFeeder());
			}

			data.put("nm_ibu_kandung", Common.maxPanjang(biodataDosen.getNamaIbu(), 50));

			data.put("stat_kawin", biodataDosen.getStatusNikah().equals(0) ? 0 : 1);

			data.put("nm_suami_istri", Common.maxPanjang(biodataDosen.getNamaSuamiIstri(), 50));

			data.put("nip_suami_istri", Common.maxPanjang(biodataDosen.getNipSuamiIstri(), 18));

			if (biodataDosen.getPekerjaanSuamiIstri() != null) {
				data.put("id_pekerjaan_suami_istri",
						Common.maxPanjang(biodataDosen.getPekerjaanSuamiIstri().getNama(), 32));
			}

			if (dosen.getTmtPns() != null) {
				data.put("tmt_pns", Common.databaseDateFormat.get().format(dosen.getTmtPns()));
			}

			data.put("a_lisensi_kepsek", dosen.getaLisensiKepsek() ? 1 : 0);
			data.put("jml_sekolah_binaan", dosen.getJmlSekolahBinaan());
			data.put("akta_ijin_ajar", Common.maxPanjang(dosen.getAktaIjinAjar(), 1));
			data.put("nira", Common.maxPanjang(dosen.getNira(), 30));
			data.put("stat_data", dosen.getId() + "");
			// data.put("mampu_handle_kk", "");
			data.put("a_braille", dosen.getaBraille() ? 1 : 0);
			data.put("a_bhs_isyarat", dosen.getaBhsIsyarat() ? 1 : 0);
			data.put("npwp", Common.maxPanjang(dosen.getNpwp(), 30));
			data.put("kewarganegaraan", Common.maxPanjang(biodataDosen.getKewarganegaraanFeeder(), 2));

			try {

				if (dosen.getFeeder() == null || dosen.getFeeder().trim().isEmpty()) {

					JSONObject jsonObject = new JSONObject(data);

					Node node = feederConnector.insertRecordOld(token, "dosen", jsonObject.toString());
					String id_ptk = FeederConverter.value(node, "id_ptk");

					System.out.println("id_ptk = " + id_ptk);
					if (id_ptk != null && !id_ptk.isEmpty()) {
						dosen.setFeeder(id_ptk);
						session.getTransaction().begin();
						Common.refreshUpdate(session, dosen);
						session.getTransaction().commit();
					}
				} else {

					JSONObject jsonObject = new JSONObject(data);

					Map<String, Object> dataKey = new HashMap<String, Object>();
					dataKey.put("id_ptk", dosen.getFeeder().trim());
					JSONObject jsonObjectKey = new JSONObject(dataKey);
					Map<String, Object> dataUpdate = new HashMap<String, Object>();
					dataUpdate.put("key", jsonObjectKey);
					dataUpdate.put("data", jsonObject);
					JSONObject dataUpdateObject = new JSONObject(dataUpdate);

					Node node = feederConnector.updateRecordOld(token, "dosen", dataUpdateObject.toString());

					String id_ptk = FeederConverter.value(node, "id_ptk");
					String id_reg_ptk = FeederConverter.value(node, "id_reg_ptk");
					if (id_reg_ptk != null && !id_reg_ptk.isEmpty()) {
						dosen.setIdRegPtk(id_reg_ptk);
						session.getTransaction().begin();
						Common.refreshUpdate(session, dosen);
						session.getTransaction().commit();
					}

					System.out.println("id_ptk = " + id_ptk + " id_reg_ptk = " + id_reg_ptk);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void dosen_pt() {
		Session session = HibernateUtil.currentNativeSession();
		try {
		@SuppressWarnings("unchecked")
		List<PenugasanDosenMengajar> dosens = session.createCriteria(PenugasanDosenMengajar.class)
				.add(Restrictions.ne("kode", "")).add(Restrictions.isNotNull("kode"))

				.createAlias("jurusan", "jurusan").add(Restrictions.ne("jurusan.feeder", ""))
				.add(Restrictions.isNotNull("jurusan.feeder"))

				.createCriteria("dosen").add(Restrictions.ne("nama", "")).add(Restrictions.ne("feeder", ""))
				.add(Restrictions.isNotNull("feeder")).createAlias("perguruanTinggi", "perguruanTinggi")
				.add(Restrictions.isNotNull("perguruanTinggi.feeder"))
				.add(Restrictions.ne("perguruanTinggi.feeder", "")).list();

		System.out.println("dosens size => " + dosens.size());

		for (PenugasanDosenMengajar penugasanDosenMengajar : dosens) {
			FeederExporterHelper.dosen_pt(session, feederConnector, token, penugasanDosenMengajar);
		}

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void doEksport() {

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Ekspor data nilai huruf..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,10);
		}
		nilaiHuruf();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Ekspor data matakuliah..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,20);
		}
		matakuliah();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Ekspor data kurikulum..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,30);
		}
		kurikulum();
		kurikulumPunyaMatakuliah();
		kelas_kuliah();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Ekspor data mahasiswa..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,40);
		}
		mahasiswa();
		mahasiswa_pt();

		// feederExporter.dosen();
		// feederExporter.dosen_pt();

		ajar_dosen();
		nilai();
		kuliah_mahasiswa();

		if (labelProses != null) {
			NeoFeederProgressHelper.setLabelValueSafe(labelProses,"Ekspor selesai..");
			NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeter,100);
		}
	}

	public String value(Node result, String key) {
		if (result == null) {
			return null;
		}
		boolean hasNode = result.hasChildNodes();
		if (hasNode) {
			NodeList nodeList = result.getChildNodes();
			return value(nodeList, key);
		}
		return null;
	}

	public String value(NodeList nodeList, String key) {
		String hasil = null;
		for (int i = 0; i < nodeList.getLength(); i++) {
			try {
				Node node = nodeList.item(i);

				if (node.getTextContent() == null || node.getTextContent().trim().isEmpty()) {
					continue;
				}

				if (node.getNodeName().equalsIgnoreCase(key)) {
					hasil = node.getTextContent().trim();
					break;
				}

				if (node.hasChildNodes()) {
					NodeList list = node.getChildNodes();
					hasil = value(list, key);
				}

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederExporter.java:2668");

			}
		}

		return hasil;
	}

	public void nilaiTransfer(Detailperkuliahan detailperkuliahan, List<String> errorLog) {
		try {

			JSONObject jsonObject = FeederExporterGenerator.nilai_transfer(detailperkuliahan);
			String filter = "id_matkul='" + detailperkuliahan.getMatakuliahKonversi().getFeeder()
					+ "' AND id_registrasi_mahasiswa='" + detailperkuliahan.getMahasiswa().getIdRegPd() + "'";

			JSONArray dataNilaiTransferPendidikanMahasiswa = feederConnector
					.getData("GetNilaiTransferPendidikanMahasiswa", token, filter, "", "1", "");
			System.out
					.println("results dataNilaiTransferPendidikanMahasiswa -> " + dataNilaiTransferPendidikanMahasiswa);

			String id_transfer = null;
			if (dataNilaiTransferPendidikanMahasiswa.length() > 0) {

				JSONObject a = dataNilaiTransferPendidikanMahasiswa.getJSONObject(0);
				id_transfer = a.getString("id_transfer").trim();

				if (id_transfer != null && !id_transfer.isEmpty()) {

					JSONObject idMhs = new JSONObject();
					idMhs.put("id_transfer", id_transfer);
					idMhs.put("id_ekuivalensi", id_transfer);

					feederConnector.insertOrUpdateRecordBaru(token, idMhs, "UpdateNilaiTransferPendidikanMahasiswa",
							jsonObject, errorLog, detailperkuliahan);
				}

			} else {
				JSONObject a = feederConnector.insertOrUpdateRecordBaru(token, null,
						"InsertNilaiTransferPendidikanMahasiswa", jsonObject, errorLog, detailperkuliahan);

				JSONObject data = ambilDataObject(a, errorLog,
						"InsertNilaiTransferPendidikanMahasiswa");
				id_transfer = data == null ? null : trimKeNull(data.optString("id_transfer", null));
			}

			System.out.println("id_transfer = " + id_transfer);

			if (id_transfer != null
					&& (detailperkuliahan.getFeeder() == null || !detailperkuliahan.getFeeder().equals(id_transfer))) {
				Session session = null;
				Transaction transaction = null;
				detailperkuliahan.setFeeder(id_transfer);
				try {
					session = HibernateUtil.getSessionFactory().openSession();
					transaction = session.beginTransaction();
					session.merge(detailperkuliahan);
					transaction.commit();
				} catch (Exception e) {
					try { if (transaction != null && transaction.isActive()) transaction.rollback(); }
					catch (Exception rollbackError) { ais.common.ErrorAuditUtil.record(rollbackError, "auto-audit(empty-catch) FeederExporter.nilaiTransfer.rollback"); }
					throw e;
				} finally {
					tutupSessionKhusus(session);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederExporter.java:2723");
		}
	}

	/** Respons Neo Feeder tidak selalu mengembalikan data sebagai JSONObject. */
	private static JSONObject ambilDataObject(JSONObject response, List<String> errorLog, String operasi) {
		if (response == null || response.isNull("data")) {
			return null;
		}
		try {
			Object data = response.opt("data");
			if (data instanceof JSONObject) {
				return (JSONObject) data;
			}
			if (data instanceof JSONArray) {
				JSONArray array = (JSONArray) data;
				return array.length() > 0 && array.opt(0) instanceof JSONObject
						? (JSONObject) array.opt(0) : null;
			}
			if (data instanceof String) {
				String teks = ((String) data).trim();
				if (teks.startsWith("{")) {
					return new JSONObject(teks);
				}
				if (teks.startsWith("[")) {
					JSONArray array = new JSONArray(teks);
					return array.length() > 0 && array.opt(0) instanceof JSONObject
							? (JSONObject) array.opt(0) : null;
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit FeederExporter.ambilDataObject");
		}
		if (errorLog != null) {
			errorLog.add(operasi + " gagal: format field data dari Neo Feeder tidak dikenali. Respons: "
					+ response.toString());
		}
		return null;
	}

	/**
	 * Mengambil satu nilai dari objek {@code data} pada respons Neo Feeder.
	 *
	 * <p>Pemanggilnya diperkenalkan r78156 sebagai penyeragaman enam salinan
	 * ekspresi yang sama persis, tetapi helper-nya sendiri tidak ikut ter-commit --
	 * trunk karena itu tidak dapat dikompilasi sejak revisi tersebut. Badan method
	 * ini DIPULIHKAN apa adanya dari baris yang digantikan pada diff r78156:</p>
	 *
	 * <pre>a.isNull("data") ? null : a.getJSONObject("data").getString(kunci).trim()</pre>
	 *
	 * <p>Sengaja tidak "diperbaiki" menjadi lebih toleran (mis. {@code optString}):
	 * keenam pemanggil sebelumnya memang melempar bila kuncinya tidak ada, dan
	 * mengubah perilaku itu sambil memulihkan kompilasi akan menyelundupkan
	 * perubahan yang tidak diminta siapa pun ke dalam perbaikan build.</p>
	 */
	private static String ambilNilaiData(JSONObject a, String kunci)
			throws org.json.JSONException {
		if (a == null || a.isNull("data")) return null;
		Object data = a.opt("data");
		Object nilai = null;
		if (data instanceof JSONObject) {
			nilai = ((JSONObject) data).opt(kunci);
		} else if (data instanceof org.json.JSONArray) {
			org.json.JSONArray array = (org.json.JSONArray) data;
			if (array.length() > 0 && array.opt(0) instanceof JSONObject) {
				nilai = ((JSONObject) array.opt(0)).opt(kunci);
			}
		} else {
			// Beberapa versi Neo Feeder mengembalikan ID langsung sebagai scalar.
			nilai = data;
		}
		if (nilai == null || nilai == JSONObject.NULL) return null;
		String hasil = String.valueOf(nilai).trim();
		return hasil.length() == 0 || "null".equalsIgnoreCase(hasil) ? null : hasil;
	}

	private static String trimKeNull(String value) {
		if (value == null) return null;
		value = value.trim();
		return value.length() == 0 ? null : value;
	}

	private static void tutupSessionKhusus(Session session) {
		if (session == null) return;
		try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) FeederExporter.clear"); }
		try { if (session.isConnected()) session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) FeederExporter.disconnect"); }
		try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) FeederExporter.close"); }
	}

	public void nilai() {
		nilai(null);
	}

	public void nilai(List<String> errorLog) {
		Session session = HibernateUtil.currentNativeSession();
		try {
		@SuppressWarnings("unchecked")
		List<Detailperkuliahan> detailperkuliahans = session.createCriteria(Detailperkuliahan.class)
				.createAlias("mahasiswa", "mahasiswa").createAlias("perkuliahan", "perkuliahan")
				.add(Restrictions.isNotNull("mahasiswa.idRegPd")).add(Restrictions.ne("mahasiswa.idRegPd", ""))
				.add(Restrictions.isNotNull("perkuliahan.feeder")).add(Restrictions.ne("perkuliahan.feeder", ""))
				.add(Restrictions.gt("totalIP", 0.1)).list();

		System.out.println("detailperkuliahans size => " + detailperkuliahans.size());

		int index = 0;
		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / detailperkuliahans.size()));
				index++;
			}

			try {
				FeederTranspoter.insertNilai(feederConnector, token, detailperkuliahan, session, errorLog, true);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void nilai(Detailperkuliahan detailperkuliahan, List<String> errorLog) {
		if (detailperkuliahan == null) {
			if (errorLog != null) {
				errorLog.add("Nilai tidak dikirim ke Neo Feeder karena data Detailperkuliahan kosong.");
			}
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		try {
			Detailperkuliahan dp = detailperkuliahan;
			if (detailperkuliahan.getId() != null) {
				dp = (Detailperkuliahan) session.get(Detailperkuliahan.class, detailperkuliahan.getId());
			}
			if (dp == null) {
				if (errorLog != null) {
					errorLog.add("Nilai tidak dikirim ke Neo Feeder karena Detailperkuliahan tidak ditemukan.");
				}
				return;
			}

			String mahasiswaInfo = dp.getMahasiswa() == null ? "Mahasiswa kosong"
					: (dp.getMahasiswa().getNim() + " " + dp.getMahasiswa().getNama());
			if (dp.getMahasiswa() == null || dp.getMahasiswa().getIdRegPd() == null
					|| dp.getMahasiswa().getIdRegPd().trim().isEmpty()) {
				if (errorLog != null) {
					errorLog.add("[" + mahasiswaInfo
							+ "] Nilai tidak dikirim ke Neo Feeder karena mahasiswa belum punya ID registrasi Feeder.");
				}
				return;
			}
			if (dp.getPerkuliahan() == null) {
				if (errorLog != null) {
					errorLog.add("[" + mahasiswaInfo
							+ "] Nilai tidak dikirim ke Neo Feeder karena Detailperkuliahan belum terhubung ke Perkuliahan/Kelas.");
				}
				return;
			}
			if (dp.getPerkuliahan().getFeeder() == null || dp.getPerkuliahan().getFeeder().trim().isEmpty()) {
				if (errorLog != null) {
					errorLog.add("[" + mahasiswaInfo
							+ "] Nilai tidak dikirim ke Neo Feeder karena kelas/perkuliahan belum masuk Feeder.");
				}
				return;
			}
			if (dp.getTotalNilai() == null || dp.getTotalNilai() <= 0.1) {
				if (errorLog != null) {
					errorLog.add("[" + mahasiswaInfo
							+ "] Nilai tidak dikirim ke Neo Feeder karena total nilai masih kosong/0.");
				}
				return;
			}

			FeederTranspoter.insertNilai(feederConnector, token, dp, session, errorLog, true);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			if (errorLog != null) {
				errorLog.add("Gagal mengirim nilai ke Neo Feeder: " + e.getMessage());
			}
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void kuliah_mahasiswa() {

		Session session = HibernateUtil.currentNativeSession();
		try {
		@SuppressWarnings("unchecked")
		List<Mahasiswa> mahasiswas = session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("idRegPd")).add(Restrictions.ne("idRegPd", ""))
				.add(Restrictions.ne("nim", "")).add(Restrictions.ne("nama", "")).createAlias("jurusan", "jurusan")
				.createAlias("jurusan.fakultas", "fakultas").createAlias("fakultas.perguruanTinggi", "perguruanTinggi")
				.add(Restrictions.isNotNull("jurusan.feeder")).add(Restrictions.ne("jurusan.feeder", ""))
				.add(Restrictions.isNotNull("perguruanTinggi.feeder"))
				.add(Restrictions.ne("perguruanTinggi.feeder", "")).addOrder(Order.desc("nim")).list();

		System.out.println("mahasiswas size => " + mahasiswas.size());

		int index = 0;
		for (Mahasiswa mahasiswa : mahasiswas) {

			if (progressmeterChild != null) {
				NeoFeederProgressHelper.setProgressmeterValueSafe(progressmeterChild,(int) (index * 100.0 / mahasiswas.size()));
				index++;
			}

			FeederExporterHelper.kuliah_mahasiswa(session, feederConnector, token, mahasiswa);

		}

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();

		} finally {
			HibernateUtil.closeSession();
		}
	}

}
