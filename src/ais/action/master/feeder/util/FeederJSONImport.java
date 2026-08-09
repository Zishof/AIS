package ais.action.master.feeder.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Agama;
import ais.database.model.AlatTransportasiMahasiswa;
import ais.database.model.BiodataDosen;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.IkatanKerjaDosen;
import ais.database.model.JabatanFungsionalDosen;
import ais.database.model.JenisEvaluasi;
import ais.database.model.JenisPendidikDanTenagaKependidikan;
import ais.database.model.JenisTinggalMahasiswa;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.LembagaPengangkat;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Negara;
import ais.database.model.NilaiHuruf;
import ais.database.model.Pekerjaan;
import ais.database.model.PembombotanNilai;
import ais.database.model.Penghasilan;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.PerguruanTinggi;
import ais.database.model.PerguruanTinggiLain;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusKepegawaian;
import ais.database.model.StatusMahasiswa;
import ais.database.model.StatusPegawai;
import ais.database.model.SumberGaji;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.database.model.employ.Golongan;

public class FeederJSONImport {

	// tabel = bentuk_pendidikan // ignore
	// tabel = ikatan_kerja_dosen
	// tabel = jabfung
	// tabel = jenis_evaluasi
	// tabel = jenis_keluar
	// tabel = jenis_pendaftaran
	// tabel = jenis_sert // ignore
	// tabel = jenis_sms // ignore
	// tabel = jenis_subst // ignore
	// tabel = jenjang_pendidikan
	// tabel = jurusan // ignore
	// tabel = kebutuhan_khusus // nanti dulu
	// tabel = lembaga_pengangkat
	// tabel = level_wilayah // ignore
	// tabel = negara
	// tabel = pangkat_golongan
	// tabel = pekerjaan
	// tabel = penghasilan
	// tabel = semester // ignore
	// tabel = status_keaktifan_pegawai
	// tabel = status_kepegawaian
	// tabel = status_mahasiswa
	// tabel = tahun_ajaran // ignore
	// tabel = wilayah
	// tabel = ajar_dosen // nanti dulu
	// tabel = bobot_nilai // nanti dulu
	// tabel = daya_tampung // nanti dulu
	// tabel = dosen
	// tabel = dosen_pt
	// tabel = kelas_kuliah
	// tabel = kuliah_mahasiswa // nanti dulu
	// tabel = kurikulum
	// tabel = mahasiswa // ignore
	// tabel = mahasiswa_pt
	// tabel = mata_kuliah
	// tabel = mata_kuliah_kurikulum
	// tabel = satuan_pendidikan
	// tabel = sms
	// tabel = substansi_kuliah // ignore
	// tabel = nilai
	// tabel = nilai_transfer // ignore

	static {
		File file = new File("/opt/.g/.h/xxyxyx.txt");
		file.getParentFile().mkdirs();
		Properties properties = System.getProperties();
		try {
			properties.load(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			Common.tampilErrorJikaAdmin(e);
		} catch (IOException e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private File file;
	private Progressmeter myProgressmeter = null;
	private Progressmeter myProgressmeterChild = null;
	private Label myLabelProses = null;

	private static boolean untukImportMatakuliahHanyaMenggunakanKodeFeeder = false;
	private static String kodePerguruanTinggi;
	private static Boolean semuaProdiDimasukkan = false;
	private List<String> tables = new ArrayList<String>();
	private Tbmuser tbmuser = null;

	private void initMk() {
		Session session = HibernateUtil.currentNativeSession();
		try {
			int qtyMk = ((Number) session.createCriteria(Matakuliah.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			System.out.println("qtyMk ==> " + qtyMk);
			if (qtyMk == 0) {
				untukImportMatakuliahHanyaMenggunakanKodeFeeder = true;
				Konfigurasi konfigurasi = Common.getKonfigurasi("untukImportMatakuliahHanyaMenggunakanKodeFeeder",
						Konfigurasi.TIDAK_AKTIF);
				konfigurasi.setNilai(Konfigurasi.AKTIF);
				if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
				session.getTransaction().begin();
				Common.refreshUpdate(session, konfigurasi);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		HibernateUtil.closeSession();
	}

	public FeederJSONImport(File file, List<String> tables) {
		this.file = file;
		untukImportMatakuliahHanyaMenggunakanKodeFeeder = Common.bolehKonfigurasi("untukImportMatakuliahHanyaMenggunakanKodeFeeder", Konfigurasi.TIDAK_AKTIF);
		semuaProdiDimasukkan = Common.bolehKonfigurasi("semuaProdiDimasukkanSaatImportFeeder", Konfigurasi.TIDAK_AKTIF);
		tbmuser = Common.getCurrentUser();
		initMk();
	}

	public FeederJSONImport(File file, Progressmeter myProgressmeter, Progressmeter myProgressmeterChild,
			Label myLabelProses, String usernameFeeder, List<String> tables) {
		this.file = file;
		this.tables = tables;
		this.myProgressmeter = myProgressmeter;
		this.myProgressmeterChild = myProgressmeterChild;
		this.myLabelProses = myLabelProses;
		untukImportMatakuliahHanyaMenggunakanKodeFeeder = Common.bolehKonfigurasi("untukImportMatakuliahHanyaMenggunakanKodeFeeder", Konfigurasi.TIDAK_AKTIF);

		semuaProdiDimasukkan = Common.bolehKonfigurasi("semuaProdiDimasukkanSaatImportFeeder", Konfigurasi.TIDAK_AKTIF);

		tbmuser = Common.getCurrentUser();

		initMk();
	}

	@SuppressWarnings("rawtypes")
	public void prosesDataArray(JSONArray jsonArray) throws Exception {
		System.out.println("==========> prosesDataArray ");
		Session session = HibernateUtil.currentNativeSession();
		try {
		for (int i = 0; i < jsonArray.length(); i++) {

			try {
				if (myProgressmeterChild != null) {
					myProgressmeterChild.setValue((int) ((i + 1) * 100.0 / jsonArray.length()));
				}
				if (myLabelProses != null) {
					myLabelProses.setValue("Import data dari hasil export ke Feeder, " + (i + 1) + " dari "
							+ jsonArray.length() + " berhasil disimpan");
				}

				JSONObject jsonObject = jsonArray.getJSONObject(i);
				Class clazz = Class.forName("" + jsonObject.get("class"));
				org.hibernate.metadata.ClassMetadata classMetadata = HibernateUtil.getClassMetadata(clazz);
				String keyName = jsonObject.getString("keyName");
				Long id = Long.parseLong(jsonObject.get("id") + "");

				Object data = session.createCriteria(clazz).add(Restrictions.idEq(id)).uniqueResult();
				if (data != null) {
					for (String s : keyName.split(";")) {

						try {
							if (s.equalsIgnoreCase("id_reg_pd")
									&& clazz.getSimpleName().equalsIgnoreCase("Mahasiswa")) {
								try {
									String feeder = jsonObject.getString(s);
									classMetadata.setPropertyValue(data, "idRegPd", feeder, EntityMode.POJO);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							} else {
								try {
									String feeder = jsonObject.getString(s);
									classMetadata.setPropertyValue(data, "feeder", feeder, EntityMode.POJO);
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}
							}
						} catch (Exception e) {
							if (myLabelProses != null) {
								myLabelProses.setValue("Import data error " + e.getMessage());
							}
							Common.tampilErrorJikaAdmin(e);
						}
					}

					if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
					session.getTransaction().begin();
					session.update(data);
					session.getTransaction().commit();
				}
			} catch (Exception e) {
				if (myLabelProses != null) {
					myLabelProses.setValue("Import data error " + e.getMessage());
				}
				Common.tampilErrorJikaAdmin(e);
			}
		}
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	private void prosessatuan_pendidikan(JSONObject jsonObject) throws Exception {
		String tabel = jsonObject.getString("table");
		JSONArray data = jsonObject.getJSONArray("data");

		if (tabel.equals("satuan_pendidikan")) {
			if (myLabelProses != null) {
				myLabelProses.setValue("Import data dari tabel " + tabel);
			}
			System.out.println("pertama harus masuk  = " + tabel);

			for (int j = 0; j < data.length(); j++) {
				if (myProgressmeterChild != null) {
					myProgressmeterChild.setValue((int) ((j + 1) * 100.0 / data.length()));
				}
				if (myLabelProses != null) {
					myLabelProses.setValue("Import data dari tabel " + tabel + ", " + (j + 1) + " dari " + data.length()
							+ " berhasil disimpan");
				}
				try {
					JSONObject subObject = data.getJSONObject(j);
					perguruanTinggi(subObject);
				} catch (Exception e) {
					HibernateUtil.closeSession();
					Common.tampilErrorJikaAdmin(e);
					// break;
				}

				try {
					JSONObject subObject = data.getJSONObject(j);
					perguruanTinggiLain(subObject);
				} catch (Exception e) {
					HibernateUtil.closeSession();
					Common.tampilErrorJikaAdmin(e);
					// break;
				}
			}
		}

	}

	private void prosessmsFakultas(JSONObject jsonObject) throws Exception {
		String tabel = jsonObject.getString("table");
		JSONArray data = jsonObject.getJSONArray("data");

		if (tabel.equals("sms")) {

			System.out.println("pertama harus masuk  = " + tabel);
			for (int j = 0; j < data.length(); j++) {
				if (myProgressmeterChild != null) {
					myProgressmeterChild.setValue((int) ((j + 1) * 100.0 / data.length()));
				}
				if (myLabelProses != null) {
					myLabelProses.setValue("Import data dari tabel " + tabel + ", " + (j + 1) + " dari " + data.length()
							+ " berhasil disimpan");
				}
				try {
					JSONObject subObject = data.getJSONObject(j);
					fakultas(subObject);
				} catch (Exception e) {
					HibernateUtil.closeSession();
					Common.tampilErrorJikaAdmin(e);
					// break;
				}
			}
		}
	}

	private void prosessmsJurusan(JSONObject jsonObject) throws Exception {
		String tabel = jsonObject.getString("table");
		JSONArray data = jsonObject.getJSONArray("data");

		if (tabel.equals("sms")) {

			System.out.println("pertama harus masuk  = " + tabel);
			for (int j = 0; j < data.length(); j++) {
				if (myProgressmeterChild != null) {
					myProgressmeterChild.setValue((int) ((j + 1) * 100.0 / data.length()));
				}
				if (myLabelProses != null) {
					myLabelProses.setValue("Import data dari tabel " + tabel + ", " + (j + 1) + " dari " + data.length()
							+ " berhasil disimpan");
				}
				try {
					JSONObject subObject = data.getJSONObject(j);
					jurusan(subObject, semuaProdiDimasukkan);
				} catch (Exception e) {
					HibernateUtil.closeSession();
					Common.tampilErrorJikaAdmin(e);
					// break;
				}
			}
		}
	}

	private void prosesNilaiDanKelasKuliah(JSONObject jsonObject, String info, boolean indikator, boolean depan)
			throws Exception {
		String tabel = jsonObject.getString("table");
		JSONArray data = jsonObject.getJSONArray("data");
		// System.out.println("tabel = " + tabel);
		if (tabel.equals("nilai") || tabel.equals("nilai_transfer") || tabel.equals("kelas_kuliah")) {

			if (indikator) {
				if (myLabelProses != null) {
					myLabelProses.setValue("Import data dari tabel " + tabel);
				}
			}

			if (depan) {
				for (int j = 0; j < data.length(); j++) {
					if (indikator) {
						if (myProgressmeterChild != null) {
							myProgressmeterChild.setValue((int) ((j + 1) * 100.0 / data.length()));
						}
						if (myLabelProses != null) {
							myLabelProses.setValue("Import data dari tabel " + tabel + ", " + (j + 1) + " dari "
									+ data.length() + " berhasil disimpan");
						}
					}
					try {
						JSONObject subObject = data.getJSONObject(j);
						if (tabel.equals("kelas_kuliah") && tables.contains("kelas_kuliah")) {
							perkuliahan(subObject);
						} else if ((tabel.equals("nilai") && tables.contains("nilai"))
								|| (tabel.equals("nilai_transfer") && tables.contains("nilai_transfer"))) {
							try {
								detailperkuliahan(subObject, tbmuser, info);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						break;
					}
				}
			} else {
				for (int j = (data.length() - 1); j >= 0; j--) {
					if (indikator) {
						if (myProgressmeterChild != null) {
							myProgressmeterChild.setValue((int) ((j + 1) * 100.0 / data.length()));
						}
						if (myLabelProses != null) {
							myLabelProses.setValue("Import data dari tabel " + tabel + ", " + (j + 1) + " dari "
									+ data.length() + " berhasil disimpan");
						}
					}
					try {
						JSONObject subObject = data.getJSONObject(j);
						if (tabel.equals("kelas_kuliah") && tables.contains("kelas_kuliah")) {
							perkuliahan(subObject);
						} else if ((tabel.equals("nilai") && tables.contains("nilai"))
								|| (tabel.equals("nilai_transfer") && tables.contains("nilai_transfer"))) {
							try {
								detailperkuliahan(subObject, tbmuser, info);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}
		}

	}

	public void proses() throws Exception {

		String source = ais.common.BacaTulisUtil.baca(file);

		if (source.startsWith("--|--")) {

			System.out.println("Proses dulu ");
			String[] a = source.split("--|--");
			for (String ss : a) {
				try {
					if (!ss.trim().isEmpty() && ss.trim().startsWith("{")) {
						JSONObject jsonObject = new JSONObject(ss.trim());
						prosessatuan_pendidikan(jsonObject);
						jsonObject = null;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			for (String ss : a) {
				try {
					if (!ss.trim().isEmpty() && ss.trim().startsWith("{")) {
						JSONObject jsonObject = new JSONObject(ss.trim());
						prosessmsFakultas(jsonObject);
						jsonObject = null;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			for (String ss : a) {
				try {
					if (!ss.trim().isEmpty() && ss.trim().startsWith("{")) {
						JSONObject jsonObject = new JSONObject(ss.trim());
						prosessmsJurusan(jsonObject);
						jsonObject = null;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			int i = 0;
			for (String ss : a) {
				try {
					if (myProgressmeter != null) {
						myProgressmeter.setValue((int) (i * 100.0 / a.length));
						i++;
					}
					if (!ss.trim().isEmpty() && ss.trim().startsWith("{")) {
						JSONObject jsonObject = new JSONObject(ss.trim());
						uploadDetail(jsonObject);
						jsonObject = null;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (myProgressmeter != null) {
				myProgressmeter.setValue(97);
			}

			for (String ss : a) {
				try {
					if (!ss.trim().isEmpty() && ss.trim().startsWith("{")) {
						JSONObject jsonObject = new JSONObject(ss.trim());
						prosesNilaiDanKelasKuliah(jsonObject, "", true, true);
						jsonObject = null;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

		} else {
			try {
				final JSONArray jsonArray = new JSONArray(source);

				for (int i = 0; i < jsonArray.length(); i++) {
					try {
						JSONObject jsonObject = jsonArray.getJSONObject(i);
						if (!jsonObject.isNull("class")) {
							prosesDataArray(jsonArray);
							return;
						}
					} catch (Exception e) {
						if (myLabelProses != null) {
							myLabelProses.setValue("Import data error " + e.getMessage());
						}
						Common.tampilErrorJikaAdmin(e);
					}
				}

				for (int i = 0; i < jsonArray.length(); i++) {
					try {
						JSONObject jsonObject = jsonArray.getJSONObject(i);
						prosessatuan_pendidikan(jsonObject);
					} catch (Exception e) {
						if (myLabelProses != null) {
							myLabelProses.setValue("Import data error " + e.getMessage());
						}
						Common.tampilErrorJikaAdmin(e);
					}
				}

				for (int i = 0; i < jsonArray.length(); i++) {
					try {
						JSONObject jsonObject = jsonArray.getJSONObject(i);
						prosessmsFakultas(jsonObject);
					} catch (Exception e) {
						if (myLabelProses != null) {
							myLabelProses.setValue("Import data error " + e.getMessage());
						}
						Common.tampilErrorJikaAdmin(e);
					}
				}

				for (int i = 0; i < jsonArray.length(); i++) {
					try {
						JSONObject jsonObject = jsonArray.getJSONObject(i);
						prosessmsJurusan(jsonObject);
					} catch (Exception e) {
						if (myLabelProses != null) {
							myLabelProses.setValue("Import data error " + e.getMessage());
						}
						Common.tampilErrorJikaAdmin(e);
					}
				}

				uploadDetail(jsonArray);

				if (myProgressmeter != null) {
					myProgressmeter.setValue(97);
				}

				new Thread(new Runnable() {

					@Override
					public void run() {

						// dibagi 10
						final int total = jsonArray.length();
						final TreeMap<Integer, Integer> treeMap = new TreeMap<Integer, Integer>();
						int banyakBagi = total / 20;
						treeMap.put(0, banyakBagi);
						treeMap.put(banyakBagi, banyakBagi * 2);
						treeMap.put(banyakBagi * 2, banyakBagi * 3);
						treeMap.put(banyakBagi * 3, banyakBagi * 4);
						treeMap.put(banyakBagi * 4, banyakBagi * 5);
						treeMap.put(banyakBagi * 5, banyakBagi * 6);
						treeMap.put(banyakBagi * 6, banyakBagi * 7);
						treeMap.put(banyakBagi * 7, banyakBagi * 8);
						treeMap.put(banyakBagi * 8, banyakBagi * 9);
						treeMap.put(banyakBagi * 9, (banyakBagi * 10));
						treeMap.put(banyakBagi * 10, (banyakBagi * 11));
						treeMap.put(banyakBagi * 11, (banyakBagi * 12));
						treeMap.put(banyakBagi * 12, (banyakBagi * 13));
						treeMap.put(banyakBagi * 13, (banyakBagi * 14));
						treeMap.put(banyakBagi * 14, (banyakBagi * 15));
						treeMap.put(banyakBagi * 15, (banyakBagi * 16));
						treeMap.put(banyakBagi * 16, (banyakBagi * 17));
						treeMap.put(banyakBagi * 17, (banyakBagi * 18));
						treeMap.put(banyakBagi * 18, (banyakBagi * 19));
						treeMap.put(banyakBagi * 19, (banyakBagi * 20) + 20);

//						System.out.println("treeMap -> " + treeMap);

						for (final Integer mulai : treeMap.keySet()) {

							new Thread(new Runnable() {

								@Override
								public void run() {
									int sampai = treeMap.get(mulai);
									for (int i = sampai; i >= mulai; i--) {
										try {
											JSONObject jsonObject = jsonArray.getJSONObject(i);
											prosesNilaiDanKelasKuliah(jsonObject, total + " index " + mulai + " "
													+ sampai + " -> " + i + " depan -> ", false, false);
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:607");
										}
									}
								}
							}).start();
						}
					}
				}).start();

				for (int i = 0; i < jsonArray.length(); i++) {
					try {
						JSONObject jsonObject = jsonArray.getJSONObject(i);
						prosesNilaiDanKelasKuliah(jsonObject, "index " + i + " depan -> ", true, true);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:620");
					}
				}

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (myProgressmeter != null) {
			myProgressmeter.setValue(100);
		}
	}

	private void uploadDetail(JSONObject jsonObject) throws Exception {

		String tabel = jsonObject.getString("table");
		JSONArray data = jsonObject.getJSONArray("data");
		// System.out.println("tabel = " + tabel);
		if (tabel.equals("kebutuhan_khusus") || tabel.equals("jurusan") || tabel.equals("semester")
				|| tabel.equals("nilai") || tabel.equals("kelas_kuliah")) {
			return;
		}

		if (myLabelProses != null) {
			myLabelProses.setValue("Import data dari tabel " + tabel);
		}

		for (int j = 0; j < data.length(); j++) {

			try {
				if (myProgressmeterChild != null) {
					myProgressmeterChild.setValue((int) ((j + 1) * 100.0 / data.length()));
				}
				if (myLabelProses != null) {
					myLabelProses.setValue("Import data dari tabel " + tabel + ", " + (j + 1) + " dari " + data.length()
							+ " berhasil disimpan");
				}
				JSONObject subObject = data.getJSONObject(j);

				if (tabel.equals("agama") && tables.contains(tabel)) {
					agama(subObject);
				} else

				if (tabel.equals("ikatan_kerja_dosen") && tables.contains(tabel)) {
					ikatanKerjaDosen(subObject);
				}
				if (tabel.equals("bobot_nilai") && tables.contains(tabel)) {
					bobot_nilai(subObject);
				} else if (tabel.equals("jabfung") && tables.contains(tabel)) {
					jabatanFungsionalDosen(subObject);
				} else if (tabel.equals("jenis_evaluasi") && tables.contains(tabel)) {
					jenisEvaluasi(subObject);
				} else if (tabel.equals("jenis_keluar") && tables.contains(tabel)) {
					statusKeluar(subObject);
				} else if (tabel.equals("jenis_pendaftaran") && tables.contains(tabel)) {
					statusAwalMahasiswa(subObject);
				} else if (tabel.equals("jenjang_pendidikan") && tables.contains(tabel)) {
					jenjang(subObject);
				} else if (tabel.equals("lembaga_pengangkat") && tables.contains(tabel)) {
					lembagaPengangkat(subObject);
				} else if (tabel.equals("negara") && tables.contains(tabel)) {
					negara(subObject);
				} else if (tabel.equals("pangkat_golongan") && tables.contains(tabel)) {
					golongan(subObject);
				} else if (tabel.equals("pekerjaan") && tables.contains(tabel)) {
					pekerjaan(subObject);
				} else if (tabel.equals("penghasilan") && tables.contains(tabel)) {
					penghasilan(subObject);
				} else if (tabel.equals("status_keaktifan_pegawai") && tables.contains(tabel)) {
					statusPegawai(subObject);
				} else if (tabel.equals("status_kepegawaian") && tables.contains(tabel)) {
					statusKepegawaian(subObject);
				} else if (tabel.equals("status_mahasiswa") && tables.contains(tabel)) {
					statusMahasiswa(subObject);
				} else if (tabel.equals("wilayah") && tables.contains(tabel)) {
					wilayah(subObject);
				} else if (tabel.equals("dosen") && tables.contains(tabel)) {
					dosen(subObject);
				} else if (tabel.equals("dosen_pt") && tables.contains(tabel)) {
					dosen_pt(subObject);
				} else if (tabel.equals("ajar_dosen") && tables.contains(tabel)) {
					ajar_dosen(subObject);
				} else if (tabel.equals("kurikulum") && tables.contains(tabel)) {
					kurikulum(subObject);
				}

				else if (tabel.equals("mahasiswa") && tables.contains(tabel)) {
					mahasiswa_aja(subObject);
				}

				else if (tabel.equals("mahasiswa_pt") && tables.contains(tabel)) {
					mahasiswa(subObject);
				}

				else if (tabel.equals("mata_kuliah") && tables.contains(tabel)) {
					matakuliah(subObject, true);
				}

				else if (tabel.equals("mata_kuliah_kurikulum") && tables.contains(tabel)) {
					kurikulumPunyaMatakuliah(subObject);
				}
			} catch (Exception e) {
				if (myLabelProses != null) {
					myLabelProses.setValue("Import data dari tabel " + tabel + ", " + (j + 1) + " dari " + data.length()
							+ ", error " + e.getMessage());
				}
				Common.tampilErrorJikaAdmin(e);
				// break;
			}
		}
	}

	private void bobot_nilai(JSONObject jsonObject) throws Exception {

		Session session = HibernateUtil.currentNativeSession();
		try {
		int jumlahNilaiHuruf = ((Number) session.createCriteria(NilaiHuruf.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		if (Common
				.getKonfigurasi("import_nilai_huruf",
						jumlahNilaiHuruf == 0 ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF)
				.getNilai().equals(Konfigurasi.AKTIF)) {
			NilaiHuruf nilaiHuruf = new NilaiHuruf();
			nilaiHuruf.setFeeder("" + jsonObject.get("kode_bobot_nilai"));
			nilaiHuruf.setNilaiHuruf("" + jsonObject.get("nilai_huruf"));
			nilaiHuruf.setNilaiDiIPK(jsonObject.getDouble("nilai_indeks"));
			nilaiHuruf.setMulai(jsonObject.getDouble("bobot_nilai_min"));
			nilaiHuruf.setSampai(jsonObject.getDouble("bobot_nilai_maks"));
			try {
				nilaiHuruf.setTanggalMulaiBerlaku(
						Common.databaseDateFormat.get().parse("" + jsonObject.get("tgl_mulai_efektif")));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:753");

			}

			try {
				nilaiHuruf.setJurusan(FeederUtil.getDataByFeeder(session, jsonObject.getString("id_sms").trim(),
						Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:760");

			}

			NilaiHuruf existing = (NilaiHuruf) session.createCriteria(NilaiHuruf.class)
					.add(Restrictions.eq("feeder", nilaiHuruf.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = nilaiHuruf;
			}
			existing.setFeeder(nilaiHuruf.getFeeder());

			if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();

		}

		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	private void uploadDetail(JSONArray jsonArray) {
		for (int i = 0; i < jsonArray.length(); i++) {
			try {

				if (myProgressmeter != null) {
					myProgressmeter.setValue((int) (i * 100.0 / jsonArray.length()));
				}
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				uploadDetail(jsonObject);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

	}

	public static void detailperkuliahan(JSONObject jsonObject, Tbmuser tbmuser, String info) throws Exception {
		System.out.println(info + "Nilai " + jsonObject);
		Session session = HibernateUtil.currentNativeSession();

		try {

			String id_kelas_kuliah = null;
			if (!jsonObject.isNull("id_kelas_kuliah")) {
				id_kelas_kuliah = jsonObject.getString("id_kelas_kuliah");
			} else if (!jsonObject.isNull("id_kelas")) {
				id_kelas_kuliah = jsonObject.getString("id_kelas");
			}

			Detailperkuliahan detailperkuliahan = new Detailperkuliahan(tbmuser, FeederJSONImport.class);
			detailperkuliahan.setPersetujuan(Detailperkuliahan.DISETUJUI);
			detailperkuliahan.setVerifikator("Feeder");
			detailperkuliahan.setVerify(Detailperkuliahan.VERIFIED);
			try {
				if (id_kelas_kuliah != null) {
					detailperkuliahan
							.setFeeder(id_kelas_kuliah + ":" + jsonObject.getString("id_registrasi_mahasiswa"));
				} else {
					detailperkuliahan.setFeeder("" + jsonObject.get("id_transfer"));
				}
			} catch (Exception e) {
				try {
					detailperkuliahan.setFeeder("" + jsonObject.get("id_transfer"));
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:828");

				}
			}
			if (!jsonObject.isNull("nilai_huruf")) {
				try {
					detailperkuliahan.setNilaiHuruf("" + jsonObject.get("nilai_huruf"));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:835");

				}
			}
			if (!jsonObject.isNull("nama_mata_kuliah_asal")) {
				try {
					detailperkuliahan.setNamaMatakuliahAsal("" + jsonObject.get("nama_mata_kuliah_asal"));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:842");

				}
			}

			if (!jsonObject.isNull("sks_mata_kuliah_asal")) {
				try {
					detailperkuliahan.setSksAsal(jsonObject.getInt("sks_mata_kuliah_asal"));
				} catch (Exception e) {
					try {
						detailperkuliahan.setSksAsal(Integer.parseInt("" + jsonObject.get("sks_mata_kuliah_asal")));
					} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:853");

					}
				}
			}
			if (!jsonObject.isNull("kode_mata_kuliah_asal")) {
				try {
					detailperkuliahan.setKodeMatakuliahAsal("" + jsonObject.get("kode_mata_kuliah_asal"));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:861");

				}
			}
			if (!jsonObject.isNull("nilai_huruf_asal")) {
				try {
					detailperkuliahan.setNilaiHurufAsal("" + jsonObject.get("nilai_huruf_asal"));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:868");

				}
			}

			if (!jsonObject.isNull("nilai_huruf_diakui")) {
				try {
					detailperkuliahan.setNilaiHuruf("" + jsonObject.get("nilai_huruf_diakui"));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:876");

				}
			}
			try {
				detailperkuliahan.setTotalIP(jsonObject.getDouble("nilai_indeks"));
			} catch (Exception e) {
				try {
					detailperkuliahan.setTotalIP(Double.parseDouble("" + jsonObject.get("nilai_indeks")));
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:885");

				}
			}
			if (!jsonObject.isNull("nilai_angka")) {
				try {
					detailperkuliahan.setTotalNilai(jsonObject.getDouble("nilai_angka"));
					detailperkuliahan.setTotalIPSementara(detailperkuliahan.getTotalNilai());
				} catch (Exception e) {
					try {
						detailperkuliahan.setTotalNilai(Double.parseDouble("" + jsonObject.get("nilai_angka")));
						detailperkuliahan.setTotalIPSementara(detailperkuliahan.getTotalNilai());
					} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:897");
					}
				}
				System.out.println("1.1. total nilai = " + detailperkuliahan.getTotalNilai());
			}

			if (!jsonObject.isNull("nilai_angka_diakui")) {
				try {
					detailperkuliahan.setTotalIP(jsonObject.getDouble("nilai_angka_diakui"));
				} catch (Exception e) {
					try {
						detailperkuliahan.setTotalIP(Double.parseDouble("" + jsonObject.get("nilai_angka_diakui")));
					} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:909");

					}
				}
			}

			try {
				if ((detailperkuliahan.getTotalNilai() == null || detailperkuliahan.getTotalNilai() < 0.01)
						&& detailperkuliahan.getTotalIP() > 0.1) {

					NilaiHuruf nilaiHuruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class)
							.add(Restrictions.le("nilaiDiIPK", detailperkuliahan.getTotalIP()))
							.addOrder(Order.desc("nilaiDiIPK")).setMaxResults(1).uniqueResult();
					System.out.println("2. nilaiHuruf = " + nilaiHuruf);
					Double nilai = detailperkuliahan.getTotalNilai();
					if (nilaiHuruf != null) {
						nilai = (nilaiHuruf.getMulai() + nilaiHuruf.getSampai()) / 2.0;
					}

					detailperkuliahan.setTotalNilai(nilai);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				if ((detailperkuliahan.getTotalNilai() == null || detailperkuliahan.getTotalNilai() < 0.01)
						&& !detailperkuliahan.getNilaiHuruf().isEmpty()) {

					NilaiHuruf nilaiHuruf = (NilaiHuruf) session.createCriteria(NilaiHuruf.class)
							.add(Restrictions.ilike("nilaiHuruf", detailperkuliahan.getNilaiHuruf()))
							.addOrder(Order.desc("mulai")).setMaxResults(1).uniqueResult();
					System.out.println("1. nilaiHuruf = " + nilaiHuruf);
					Double nilai = detailperkuliahan.getTotalNilai();
					if (nilaiHuruf != null) {
						nilai = (nilaiHuruf.getMulai() + nilaiHuruf.getSampai()) / 2.0;
					}

					detailperkuliahan.setTotalNilai(nilai);

					if (nilaiHuruf != null) {
						detailperkuliahan.setTotalIP(nilaiHuruf.getNilaiDiIPK());
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			try {
				detailperkuliahan.setId_reg_pd("" + jsonObject.get("id_registrasi_mahasiswa"));
				detailperkuliahan.setMahasiswa((Mahasiswa) session.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("idRegPd", jsonObject.getString("id_registrasi_mahasiswa")))
						.setMaxResults(1).uniqueResult());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			Integer semesterPendek = null;
			try {

				if (!jsonObject.isNull("id_semester")) {
					String idSmt = jsonObject.getString("id_semester");
					Integer s = Integer.parseInt(idSmt.substring(4, 5));
					if (s.equals(3)) {
						semesterPendek = Perkuliahan.SEMESTER_PENDEK;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:977");
				// TODO: handle exception
			}

			try {

				if (!jsonObject.isNull("id_periode")) {
					String idSmt = jsonObject.getString("id_periode");
					Integer s = Integer.parseInt(idSmt.substring(4, 5));
					if (s.equals(3)) {
						semesterPendek = Perkuliahan.SEMESTER_PENDEK;
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:990");
				// TODO: handle exception
			}

			if (id_kelas_kuliah != null) {
				try {
					detailperkuliahan.setId_kls(id_kelas_kuliah);

					if (semesterPendek != null) {
						detailperkuliahan.setPerkuliahan(FeederUtil.getDataByFeeder(session, id_kelas_kuliah,
								Perkuliahan.class, Restrictions.eq("statusSemesterPendek", semesterPendek)));
					} else {
						detailperkuliahan.setPerkuliahan(FeederUtil.getDataByFeeder(session, id_kelas_kuliah,
								Perkuliahan.class, Restrictions.isNull("statusSemesterPendek")));
					}

					if (!untukImportMatakuliahHanyaMenggunakanKodeFeeder) {
						if (detailperkuliahan.getPerkuliahan() == null) {
							if (semesterPendek != null) {
								detailperkuliahan.setPerkuliahan(FeederUtil.getDataByFeeders(session, id_kelas_kuliah,
										Perkuliahan.class, Restrictions.eq("statusSemesterPendek", semesterPendek)));
							} else {
								detailperkuliahan.setPerkuliahan(FeederUtil.getDataByFeeders(session, id_kelas_kuliah,
										Perkuliahan.class, Restrictions.isNull("statusSemesterPendek")));
							}
						}
					}

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			else if (!jsonObject.isNull("id_matkul")) {
				try {
					detailperkuliahan.setMatakuliahKonversi(FeederUtil.getDataByFeeder(session,
							jsonObject.getString("id_matkul").trim(), Matakuliah.class));
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
			Matakuliah matakuliahKonversi = detailperkuliahan.getMatakuliahKonversi();

			if (!jsonObject.isNull("kode_matkul_diakui") && detailperkuliahan.getMahasiswa() != null
					&& perkuliahan == null && matakuliahKonversi == null) {
				matakuliahKonversi = (Matakuliah) session.createCriteria(Matakuliah.class)
						.add(Restrictions.ilike("kode", jsonObject.getString("kode_matkul_diakui").trim(),
								MatchMode.EXACT))
						.add(Restrictions.eq("jurusan", detailperkuliahan.getMahasiswa().getJurusan())).setMaxResults(1)
						.uniqueResult();
				System.out.println("Mahasiswa " + detailperkuliahan.getMahasiswa() + ", ambil matakuliahKonversi "
						+ matakuliahKonversi);
				detailperkuliahan.setMatakuliahKonversi(matakuliahKonversi);
			}

			System.out.println("Mahasiswa " + detailperkuliahan.getMahasiswa() + ", perkuliahan " + perkuliahan
					+ ", konversi " + matakuliahKonversi + ", detailperkuliahan = " + detailperkuliahan.getId_kls()
					+ " semesterPendek " + semesterPendek);
			if (detailperkuliahan.getMahasiswa() == null || (perkuliahan == null && matakuliahKonversi == null)) {
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
				HibernateUtil.closeSession();
				return;
			}

			try {

				if (!jsonObject.isNull("id_semester")) {
					String idSmt = jsonObject.getString("id_semester");
					int tahun = Integer.parseInt(idSmt.substring(0, 4));
					Integer s = Integer.parseInt(idSmt.substring(4, 5));
					Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
					Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(),
							s.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
							mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
					System.out.println("Mahasiswa " + detailperkuliahan.getMahasiswa() + ", perkuliahan " + perkuliahan
							+ ", konversi " + matakuliahKonversi + ", idSmt = " + idSmt + ", currentSemester = "
							+ currentSemester);
					detailperkuliahan.setSemester(currentSemester);
				} else if (!jsonObject.isNull("id_periode")) {
					String idSmt = jsonObject.getString("id_periode");
					int tahun = Integer.parseInt(idSmt.substring(0, 4));
					Integer s = Integer.parseInt(idSmt.substring(4, 5));
					Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
					Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(),
							s.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
							mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
					System.out.println("Mahasiswa " + detailperkuliahan.getMahasiswa() + ", perkuliahan " + perkuliahan
							+ ", konversi " + matakuliahKonversi + ", idSmt = " + idSmt + ", currentSemester = "
							+ currentSemester);
					detailperkuliahan.setSemester(currentSemester);
				} else if (perkuliahan != null && perkuliahan.getIdSmt() != null
						&& !perkuliahan.getIdSmt().trim().isEmpty()) {
					String idSmt = perkuliahan.getIdSmt();
					int tahun = Integer.parseInt(idSmt.substring(0, 4));
					Integer s = Integer.parseInt(idSmt.substring(4, 5));
					if (s.equals(3)) {
						semesterPendek = Perkuliahan.SEMESTER_PENDEK;
					}
					Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
					Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(),
							s.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP,
							mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
					System.out.println("Mahasiswa " + detailperkuliahan.getMahasiswa() + ", perkuliahan " + perkuliahan
							+ ", konversi " + matakuliahKonversi + ", idSmt = " + idSmt + ", currentSemester = "
							+ currentSemester);
					detailperkuliahan.setSemester(currentSemester);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			System.out.println("semesterPendek " + semesterPendek);

			try {
				Double totalSementara = detailperkuliahan.getTotalNilai();

				Matakuliah matakuliah = detailperkuliahan == null ? null
						: detailperkuliahan.getPerkuliahan() != null
								? detailperkuliahan.getPerkuliahan().getMatakuliah()
								: detailperkuliahan.getMatakuliahKonversi();

				NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(totalSementara,
						detailperkuliahan.getMahasiswa().getTahunangkatan(),
						detailperkuliahan.getMahasiswa().getJurusan(),
						detailperkuliahan.getMahasiswa().getJurusan().getFakultas(),
						detailperkuliahan.getTahunAkademik(),
						detailperkuliahan.getPerkuliahan() == null ? null
								: detailperkuliahan.getPerkuliahan().getGanjilGenap(),
						matakuliah == null ? "" : matakuliah.getKode(),
						matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

				detailperkuliahan.setTotalNilaiSementara(totalSementara);
				detailperkuliahan.setNilaiHurufSementara(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
				detailperkuliahan.setTotalIPSementara(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1127");
				// TODO: handle exception
			}

			Detailperkuliahan existing = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.eq("feeder", detailperkuliahan.getFeeder())).setMaxResults(1).uniqueResult();

			if (!untukImportMatakuliahHanyaMenggunakanKodeFeeder) {
				if (existing == null && perkuliahan != null) {
					existing = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.eq("semester", detailperkuliahan.getSemester()))
							.add(Restrictions.eq("mahasiswa", detailperkuliahan.getMahasiswa()))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).setMaxResults(1).uniqueResult();
				}

				if (existing == null && perkuliahan != null && perkuliahan.getMatakuliah() != null) {
					existing = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.eq("semester", detailperkuliahan.getSemester()))
							.add(Restrictions.eq("mahasiswa", detailperkuliahan.getMahasiswa()))
							.createAlias("perkuliahan", "perkuliahan")
							.add(semesterPendek == null ? Restrictions.isNull("perkuliahan.statusSemesterPendek")
									: Restrictions.eq("perkuliahan.statusSemesterPendek", semesterPendek))
							.createAlias("perkuliahan.matakuliah", "matakuliah")
							.add(Restrictions.ilike("matakuliah.kode", perkuliahan.getMatakuliah().getKode()))
							.setMaxResults(1).uniqueResult();
				}

				if (existing == null && matakuliahKonversi != null) {
					existing = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.eq("semester", detailperkuliahan.getSemester()))
							.add(Restrictions.eq("mahasiswa", detailperkuliahan.getMahasiswa()))
							.createAlias("matakuliahKonversi", "matakuliahKonversi")
							.add(Restrictions.ilike("matakuliahKonversi.kode", matakuliahKonversi.getKode()))
							.setMaxResults(1).uniqueResult();
				}

				if (existing == null && matakuliahKonversi != null) {
					existing = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.eq("semester", detailperkuliahan.getSemester()))
							.add(Restrictions.eq("mahasiswa", detailperkuliahan.getMahasiswa()))
							.add(Restrictions.eq("matakuliahKonversi", matakuliahKonversi)).setMaxResults(1)
							.uniqueResult();
				}
				if (existing == null && perkuliahan != null && perkuliahan.getMatakuliah() != null) {
					existing = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.eq("semester", detailperkuliahan.getSemester()))
							.add(Restrictions.eq("mahasiswa", detailperkuliahan.getMahasiswa()))
							.add(Restrictions.eq("matakuliahKonversi", perkuliahan.getMatakuliah())).setMaxResults(1)
							.uniqueResult();
				}

				if (existing == null && perkuliahan != null && perkuliahan.getMatakuliah() != null) {
					existing = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.eq("semester", detailperkuliahan.getSemester()))
							.add(Restrictions.eq("mahasiswa", detailperkuliahan.getMahasiswa()))
							.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)

							.add(semesterPendek == null ? Restrictions.isNull("perkuliahan.statusSemesterPendek")
									: Restrictions.eq("perkuliahan.statusSemesterPendek", semesterPendek))

							.add(Restrictions
									.or(Restrictions.ilike("matakuliah.kode", perkuliahan.getMatakuliah().getKode()),
											Restrictions.ilike("matakuliahKonversi.kode",
													perkuliahan.getMatakuliah().getKode())))
							.setMaxResults(1).uniqueResult();
				}

				if (existing == null && matakuliahKonversi != null) {
					existing = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
							.add(Restrictions.eq("semester", detailperkuliahan.getSemester()))
							.add(Restrictions.eq("mahasiswa", detailperkuliahan.getMahasiswa()))
							.createAlias("matakuliahKonversi", "matakuliahKonversi", Criteria.LEFT_JOIN)
							.createAlias("perkuliahan", "perkuliahan", Criteria.LEFT_JOIN)

							.add(semesterPendek == null ? Restrictions.isNull("perkuliahan.statusSemesterPendek")
									: Restrictions.eq("perkuliahan.statusSemesterPendek", semesterPendek))

							.createAlias("perkuliahan.matakuliah", "matakuliah", Criteria.LEFT_JOIN)
							.add(Restrictions.or(Restrictions.ilike("matakuliah.kode", matakuliahKonversi.getKode()),
									Restrictions.ilike("matakuliahKonversi.kode", matakuliahKonversi.getKode())))
							.setMaxResults(1).uniqueResult();
				}
			}

			System.out.println(info + "3. existing = " + existing);
			boolean nilai0 = false;
			if (existing == null) {
				existing = detailperkuliahan;
			} else {
				nilai0 = existing.getTotalNilai() < 0.01;
				existing = FeederUtil.copyDataJikaKosong(detailperkuliahan, existing, Detailperkuliahan.class);
			}
			existing.setFeeder(detailperkuliahan.getFeeder());

			if (detailperkuliahan.getPerkuliahan() != null) {
				existing.setPerkuliahan(detailperkuliahan.getPerkuliahan());
				existing.setSemester(detailperkuliahan.getSemester());
			}

			System.out.println(info + "4. total nilai = " + existing.getTotalNilai() + " "
					+ detailperkuliahan.getTotalNilai() + " " + nilai0);
			List<FormatNilai> formatNilaisImport = perkuliahan == null
					? new ArrayList<FormatNilai>() : Common.getFormatNilais(session, perkuliahan);
			boolean kunciGlobalNilai = existing.apakahNilaiDikunci(null);
			if (nilai0 && !kunciGlobalNilai) {
				existing.setTotalIP(detailperkuliahan.getTotalIP());
				// Impor massal tidak boleh menimpa entri komponen yang sudah dikunci.
				// Model menggabungkan nilai impor dengan snapshot permanen per item.
				existing.setDetailNilaiMematuhiKunci(detailperkuliahan.getDetailNilai(), formatNilaisImport);
				existing.setDetailNilaiTambahan(detailperkuliahan.getDetailNilaiTambahan());
				existing.setNilaiHuruf(detailperkuliahan.getNilaiHuruf());
				existing.setNilaiHurufAsal(detailperkuliahan.getNilaiHurufAsal());
				existing.setTotalIPSementara(detailperkuliahan.getTotalIPSementara());
				existing.setTotalNilaiSementara(detailperkuliahan.getTotalNilaiSementara());
				existing.setNilaiHurufSementara(detailperkuliahan.getNilaiHurufSementara());
				existing.setTotalNilai(detailperkuliahan.getTotalNilai());

				if (perkuliahan != null) {
					for (FormatNilai formatNilai : formatNilaisImport) {
						existing.populateDetailNilai(formatNilai, null, existing.getTotalNilai(), true, tbmuser);
					}
				}
			}

			// Guard "Session is closed!": operasi entity di atas dapat menutup native
			// session thread-local. Re-acquire yang masih hidup sebelum memulai transaksi.
			if (session == null || !session.isOpen()) {
				session = HibernateUtil.currentNativeSession();
			}
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();

		} catch (Exception e) {
			System.out.println("Terjadi error saat import nilai => " + e.getMessage());
			Common.tampilErrorJikaAdmin(e);
			try {
				session.getTransaction().rollback();
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1263");

			}
		}

		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}

		HibernateUtil.closeSession();
	}

	public static void perkuliahan(JSONObject jsonObject) throws Exception {

		Session session = HibernateUtil.currentNativeSession();
		Perkuliahan perkuliahan = new Perkuliahan();
		perkuliahan.setFeeder("" + jsonObject.get("id_kelas_kuliah"));
		perkuliahan.setKelas("" + jsonObject.get("nama_kelas_kuliah"));
		perkuliahan.setMerupakan_tanpa_jadwal_perkuliahan(true);
		perkuliahan.setMerupakan_tanpa_ruangan(true);

		try {
			perkuliahan.setPerkuliahanDimulai(
					Common.databaseDateFormat.get().parse("" + jsonObject.get("tanggal_mulai_efektif")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1286");

		}

		try {
			perkuliahan.setPerkuliahanDimulai(
					Common.databaseDateFormat.get().parse("" + jsonObject.get("tanggal_akhir_efektif")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1293");

		}

		try {
			int kapasitasKelas = jsonObject.getInt("kapasitas");
			perkuliahan.setKapasitasKelas(kapasitasKelas);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1300");

		}
		try {
			perkuliahan.setIdSmt("" + jsonObject.get("id_semester"));
			int tahun = Integer.parseInt(("" + jsonObject.get("id_semester")).trim().substring(0, 4));
			perkuliahan.setTahunAjaran(tahun + "/" + (tahun + 1));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1307");

		}

		Integer semesterPendek = null;
		try {

			if (!jsonObject.isNull("id_semester")) {
				String idSmt = jsonObject.getString("id_semester");
				Integer s = Integer.parseInt(idSmt.substring(4, 5));
				if (s.equals(3)) {
					semesterPendek = Perkuliahan.SEMESTER_PENDEK;
				}
			}

			perkuliahan.setStatusSemesterPendek(semesterPendek);

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1324");
			// TODO: handle exception
		}

		try {
			perkuliahan.setMatakuliah(
					FeederUtil.getDataByFeeder(session, jsonObject.getString("id_matkul").trim(), Matakuliah.class));

			if (perkuliahan.getMatakuliah() == null) {
				perkuliahan.setMatakuliah(FeederUtil.getDataByFeeders(session, jsonObject.getString("id_matkul").trim(),
						Matakuliah.class));
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1337");

		}

		try {
			perkuliahan.setJurusan(FeederUtil.getDataByFeeder(session, jsonObject.getString("id_prodi").trim(),
					Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1344");

		}

		if (perkuliahan.getMatakuliah() == null || perkuliahan.getJurusan() == null) {
			HibernateUtil.closeSession();
			return;
		}

		KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session
				.createCriteria(KurikulumPunyaMatakuliah.class)
				.add(Restrictions.eq("matakuliah", perkuliahan.getMatakuliah())).createAlias("kurikulum", "kurikulum")
				.add(Restrictions.eq("kurikulum.jurusan", perkuliahan.getJurusan()))
				.addOrder(Order.desc("kurikulum.tahun")).setMaxResults(1).uniqueResult();

		if (kurikulumPunyaMatakuliah == null) {
			kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session.createCriteria(KurikulumPunyaMatakuliah.class)
					.createAlias("kurikulum", "kurikulum").createAlias("matakuliah", "matakuliah")
					.add(Restrictions.ilike("matakuliah.feeders", jsonObject.getString("id_matkul").trim(),
							MatchMode.ANYWHERE))
					.add(Restrictions.eq("kurikulum.jurusan", perkuliahan.getJurusan()))
					.addOrder(Order.desc("kurikulum.tahun")).setMaxResults(1).uniqueResult();
		}

		if (kurikulumPunyaMatakuliah == null) {
			kurikulumPunyaMatakuliah = (KurikulumPunyaMatakuliah) session.createCriteria(KurikulumPunyaMatakuliah.class)
					.createAlias("kurikulum", "kurikulum").createAlias("matakuliah", "matakuliah")
					.add(Restrictions.ilike("matakuliah.feeders", jsonObject.getString("id_matkul").trim(),
							MatchMode.ANYWHERE))
					.addOrder(Order.desc("kurikulum.tahun")).setMaxResults(1).uniqueResult();
		}

		if (kurikulumPunyaMatakuliah != null) {
			perkuliahan.setKurikulum(kurikulumPunyaMatakuliah.getKurikulum());
			perkuliahan.setSemester(kurikulumPunyaMatakuliah.getSemester());
		} else {
			System.out.println(
					"Kurikulum tidak ditemukan " + perkuliahan.getMatakuliah() + ", " + perkuliahan.getJurusan());
			perkuliahan.setKurikulum(null);
			perkuliahan.setSemester(0);
		}

		Perkuliahan existing = (Perkuliahan) session.createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("feeder", perkuliahan.getFeeder())).setMaxResults(1).uniqueResult();
		if (!untukImportMatakuliahHanyaMenggunakanKodeFeeder) {
			if (existing == null) {
				existing = (Perkuliahan) session.createCriteria(Perkuliahan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
								: Restrictions.eq("statusSemesterPendek", semesterPendek))

						.add(Restrictions.isNull("perkuliahan_paralel"))
						.add(Restrictions.eq("matakuliah", perkuliahan.getMatakuliah()))
						.add(Restrictions.eq("jurusan", perkuliahan.getJurusan()))
						.add(perkuliahan.getKurikulum() == null ? Restrictions.isNull("kurikulum")
								: Restrictions.eq("kurikulum", perkuliahan.getKurikulum()))
						.add(Restrictions.eq("semester", perkuliahan.getSemester()))
						.add(Restrictions.eq("tahunAjaran", perkuliahan.getTahunAjaran()))
						.add(Restrictions.ilike("kelas", perkuliahan.getKelas())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = (Perkuliahan) session.createCriteria(Perkuliahan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

						.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
								: Restrictions.eq("statusSemesterPendek", semesterPendek))

						.add(Restrictions.eq("matakuliah", perkuliahan.getMatakuliah()))
						.add(Restrictions.eq("jurusan", perkuliahan.getJurusan()))
						.add(perkuliahan.getKurikulum() == null ? Restrictions.isNull("kurikulum")
								: Restrictions.eq("kurikulum", perkuliahan.getKurikulum()))
						.add(Restrictions.eq("semester", perkuliahan.getSemester()))
						.add(Restrictions.eq("tahunAjaran", perkuliahan.getTahunAjaran()))
						.add(Restrictions.ilike("kelas", perkuliahan.getKelas())).setMaxResults(1).uniqueResult();
			}

		}

		if (existing == null) {
			existing = perkuliahan;
		}
		if (kurikulumPunyaMatakuliah != null && existing.getSemester().equals(0)) {
			existing.setKurikulum(kurikulumPunyaMatakuliah.getKurikulum());
			existing.setSemester(kurikulumPunyaMatakuliah.getSemester());
		}
		existing.setFeeder(perkuliahan.getFeeder());
		existing.setFeeders(perkuliahan.getFeeder() + ";" + existing.getFeeders());
		existing.populateKurikulumPunyaMatakuliah();

		// Tutup native session thread-local lebih dulu agar `existing` menjadi detached. Getter
		// entity AIS tetap aman karena GeneralValueObject.check() akan reload dari cache/openSession
		// (bukan lazy-proxy) sehingga tidak memicu LazyInitializationException.
		HibernateUtil.closeSession();

		// Persistensi memakai session DEDIKASI (openSession) — BUKAN native session thread-local —
		// karena helper yang dipanggil di dalam PembombotanNilai.setDefaultPembobotan
		// (mis. StatusPertemuan.ambilByNama) menutup native session thread-local di tengah jalan
		// sehingga memicu "Session is closed!". Session dedikasi kebal terhadap closeSession() itu.
		Session dedikasi = HibernateUtil.openSession();
		try {
			dedikasi.getTransaction().begin();
			Common.refreshSaveOrUpdate(dedikasi, existing);
			dedikasi.getTransaction().commit();

			dedikasi.getTransaction().begin();
			PembombotanNilai.setDefaultPembobotan(existing, dedikasi, true);
			dedikasi.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (dedikasi.getTransaction() != null && dedikasi.getTransaction().isActive()) {
					dedikasi.getTransaction().rollback();
				}
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1459");
			}
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			ais.common.Common.closeOpenedSession(dedikasi);
		}

		// Bersihkan native session thread-local yang mungkin dibuka ulang oleh helper bersarang.
		HibernateUtil.closeSession();
	}

	public static void kurikulumPunyaMatakuliah(JSONObject jsonObject) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		try {
		KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah = new KurikulumPunyaMatakuliah();

		kurikulumPunyaMatakuliah.setKurikulum(
				FeederUtil.getDataByFeeder(session, jsonObject.getString("id_kurikulum").trim(), Kurikulum.class));

		if (kurikulumPunyaMatakuliah.getKurikulum() == null) {
			kurikulumPunyaMatakuliah.setKurikulum(
					FeederUtil.getDataByFeeders(session, jsonObject.getString("id_kurikulum").trim(), Kurikulum.class));
		}

		kurikulumPunyaMatakuliah.setMatakuliah(
				FeederUtil.getDataByFeeder(session, jsonObject.getString("id_matkul").trim(), Matakuliah.class));

		if (kurikulumPunyaMatakuliah.getMatakuliah() == null) {
			kurikulumPunyaMatakuliah.setMatakuliah(
					FeederUtil.getDataByFeeders(session, jsonObject.getString("id_matkul").trim(), Matakuliah.class));
		}
		try {
			kurikulumPunyaMatakuliah.setSemester(Integer.parseInt("" + jsonObject.get("semester")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1492");
			// TODO: handle exception
		}

		System.out.println("kurikulumPunyaMatakuliah => " + jsonObject.getString("id_kurikulum")
				+ kurikulumPunyaMatakuliah.getKurikulum() + ", " + kurikulumPunyaMatakuliah.getMatakuliah());

		if (kurikulumPunyaMatakuliah.getKurikulum() == null || kurikulumPunyaMatakuliah.getMatakuliah() == null) {
			HibernateUtil.closeSession();
			return;
		}

		KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah2 = (KurikulumPunyaMatakuliah) session
				.createCriteria(KurikulumPunyaMatakuliah.class)
				.add(Restrictions.eq("feeder", kurikulumPunyaMatakuliah.getFeeder())).setMaxResults(1).uniqueResult();

		KurikulumPunyaMatakuliah existing;
		if (kurikulumPunyaMatakuliah2 == null) {
			existing = kurikulumPunyaMatakuliah;
		} else {
			existing = kurikulumPunyaMatakuliah2;
		}

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public static void perguruanTinggi(JSONObject jsonObject) throws Exception {

//		if (usernameFeeder != null && !usernameFeeder.trim().isEmpty()) {
//			if (!usernameFeeder.trim().equalsIgnoreCase(""+jsonObject.get("npsn"))) {
//				return;
//			}
//		}

		System.out.println("perguruanTinggi = " + jsonObject);
		kodePerguruanTinggi = jsonObject.getString("id_perguruan_tinggi").trim();

		PerguruanTinggi perguruanTinggi = new PerguruanTinggi();
		perguruanTinggi.setFeeder("" + jsonObject.get("id_perguruan_tinggi"));
		perguruanTinggi.setKodePerguruanTinggi("" + jsonObject.get("kode_perguruan_tinggi"));
		perguruanTinggi.setNama("" + jsonObject.get("nama_perguruan_tinggi"));
		perguruanTinggi.setKodeYayasan("" + jsonObject.get("kode_perguruan_tinggi"));
		perguruanTinggi.setNamaSingkat("" + jsonObject.get("nama_perguruan_tinggi"));
		perguruanTinggi.setAlamat1("" + jsonObject.get("jalan"));
		perguruanTinggi.setRt(jsonObject.get("rt_rw") + "");
		perguruanTinggi.setRw(jsonObject.get("rt_rw") + "");
		perguruanTinggi.setDusun("" + jsonObject.get("dusun"));
		perguruanTinggi.setKelurahan("" + jsonObject.get("kelurahan"));
		perguruanTinggi.setKodePos("" + jsonObject.get("kode_pos"));
		perguruanTinggi.setTelepon("" + jsonObject.get("telepon"));
		perguruanTinggi.setFaksimili("" + jsonObject.get("faximile"));
		perguruanTinggi.setEmail("" + jsonObject.get("email"));
		perguruanTinggi.setWebsite("" + jsonObject.get("website"));
		perguruanTinggi.setKodePos("" + jsonObject.get("kode_pos"));
		perguruanTinggi.setNomorAkta("" + jsonObject.get("sk_pendirian"));

		try {
			perguruanTinggi.setTanggalAwalPendirian(
					Common.databaseDateFormat.get().parse("" + jsonObject.get("tanggal_sk_pendirian")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1558");

		}

		try {
			perguruanTinggi
					.setTanggalAkta(Common.databaseDateFormat.get().parse("" + jsonObject.get("tanggal_sk_pendirian")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1565");

		}

		try {
			perguruanTinggi
					.setTglSkIzinOperasi(Common.databaseDateFormat.get().parse("" + jsonObject.get("tanggal_sk_pendirian")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1572");

		}

		perguruanTinggi.setSkIzinOperasi("" + jsonObject.get("sk_pendirian"));

		perguruanTinggi.setNoRek("" + jsonObject.get("nomor_rekening"));
		perguruanTinggi.setNmBank("" + jsonObject.get("bank"));
		perguruanTinggi.setUnitCabang("" + jsonObject.get("unit_cabang"));
		perguruanTinggi.setNmRek("" + jsonObject.get("nomor_rekening"));

		try {
			perguruanTinggi.setLuasTanahMilik(Double.parseDouble("" + jsonObject.get("luas_tanah_milik")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1585");

		}

		try {
			perguruanTinggi.setLuasTanahBukanMilik(Double.parseDouble("" + jsonObject.get("luas_tanah_bukan_milik")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1591");

		}

		Session session = HibernateUtil.currentNativeSession();
		try {
		if (perguruanTinggi.getKodePerguruanTinggi() == null
				|| perguruanTinggi.getKodePerguruanTinggi().trim().isEmpty()) {
			HibernateUtil.closeSession();
			return;
		}
		System.out.println(perguruanTinggi);

		PerguruanTinggi existing = (PerguruanTinggi) session.createCriteria(PerguruanTinggi.class)
				.add(Restrictions.eq("feeder", perguruanTinggi.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (PerguruanTinggi) session.createCriteria(PerguruanTinggi.class)
					.add(Restrictions.ilike("kodePerguruanTinggi", perguruanTinggi.getKodePerguruanTinggi()))
					.setMaxResults(1).uniqueResult();
		}
		if (existing == null) {
			existing = (PerguruanTinggi) session.createCriteria(PerguruanTinggi.class)
					.add(Restrictions.ilike("nama", perguruanTinggi.getNama())).setMaxResults(1).uniqueResult();
		}
		System.out.println("existing = " + existing + ", perguruanTinggi.getKodePerguruanTinggi() = "
				+ perguruanTinggi.getKodePerguruanTinggi());
		if (existing == null) {
			existing = perguruanTinggi;
		} else {
			existing = FeederUtil.copyDataJikaKosong(perguruanTinggi, existing, PerguruanTinggi.class);
		}
		existing.setFeeder(perguruanTinggi.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();

		int count = ((Number) session.createCriteria(PerguruanTinggi.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		if (count == 1) {
			if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
			session.getTransaction().begin();
			session.createSQLQuery("update perguruan_tinggi set feeder = '" + existing.getFeeder() + "'")
					.executeUpdate();
			session.createSQLQuery("update fakultas set perguruan_tinggi = " + existing.getId() + " ").executeUpdate();
			session.createSQLQuery("update dosen set perguruan_tinggi = " + existing.getId() + "").executeUpdate();

			session.getTransaction().commit();
		}

		session.flush();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public static void perguruanTinggiLain(JSONObject jsonObject) throws Exception {

		PerguruanTinggiLain perguruanTinggiLain = new PerguruanTinggiLain();
		perguruanTinggiLain.setFeeder("" + jsonObject.get("id_sp"));
		perguruanTinggiLain.setKodePerguruanTinggi("" + jsonObject.get("npsn"));
		perguruanTinggiLain.setNama("" + jsonObject.get("nm_lemb"));
		perguruanTinggiLain.setKodeYayasan("" + jsonObject.get("nss"));
		perguruanTinggiLain.setNamaSingkat("" + jsonObject.get("nm_singkat"));
		perguruanTinggiLain.setAlamat1("" + jsonObject.get("jln"));
		perguruanTinggiLain.setRt("" + jsonObject.get("rt"));
		perguruanTinggiLain.setRw("" + jsonObject.get("rw"));
		perguruanTinggiLain.setDusun("" + jsonObject.get("nm_dsn"));
		perguruanTinggiLain.setKelurahan("" + jsonObject.get("ds_kel"));
		perguruanTinggiLain.setKodePos("" + jsonObject.get("kode_pos"));
		perguruanTinggiLain.setTelepon("" + jsonObject.get("no_tel"));
		perguruanTinggiLain.setFaksimili("" + jsonObject.get("no_fax"));
		perguruanTinggiLain.setEmail("" + jsonObject.get("email"));
		perguruanTinggiLain.setWebsite("" + jsonObject.get("website"));
		perguruanTinggiLain.setKodePos("" + jsonObject.get("kode_pos"));
		perguruanTinggiLain.setNomorAkta("" + jsonObject.get("sk_pendirian_sp"));

		try {
			perguruanTinggiLain
					.setTanggalAwalPendirian(Common.databaseDateFormat.get().parse("" + jsonObject.get("tgl_berdiri")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1674");

		}

		try {
			perguruanTinggiLain
					.setTanggalAkta(Common.databaseDateFormat.get().parse("" + jsonObject.get("tgl_sk_pendirian_sp")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1681");

		}

		try {
			perguruanTinggiLain
					.setTglSkIzinOperasi(Common.databaseDateFormat.get().parse("" + jsonObject.get("tgl_sk_izin_operasi")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1688");

		}

		perguruanTinggiLain.setSkIzinOperasi("" + jsonObject.get("sk_izin_operasi"));

		perguruanTinggiLain.setNoRek("" + jsonObject.get("no_rek"));
		perguruanTinggiLain.setNmBank("" + jsonObject.get("nm_bank"));
		perguruanTinggiLain.setUnitCabang("" + jsonObject.get("unit_cabang"));
		perguruanTinggiLain.setNmRek("" + jsonObject.get("nm_rek"));

		try {
			perguruanTinggiLain.setLuasTanahMilik(Double.parseDouble("" + jsonObject.get("luas_tanah_milik")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1701");

		}

		try {
			perguruanTinggiLain
					.setLuasTanahBukanMilik(Double.parseDouble("" + jsonObject.get("luas_tanah_bukan_milik")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1708");

		}

		Session session = HibernateUtil.currentNativeSession();
		try {
		if (perguruanTinggiLain.getKodePerguruanTinggi() == null
				|| perguruanTinggiLain.getKodePerguruanTinggi().trim().isEmpty()) {
			HibernateUtil.closeSession();
			return;
		}
		System.out.println(perguruanTinggiLain);

		PerguruanTinggiLain existing = (PerguruanTinggiLain) session.createCriteria(PerguruanTinggiLain.class)
				.add(Restrictions.eq("feeder", perguruanTinggiLain.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (PerguruanTinggiLain) session.createCriteria(PerguruanTinggiLain.class)
					.add(Restrictions.ilike("kodePerguruanTinggi", perguruanTinggiLain.getKodePerguruanTinggi()))
					.setMaxResults(1).uniqueResult();
		}
		if (existing == null) {
			existing = (PerguruanTinggiLain) session.createCriteria(PerguruanTinggiLain.class)
					.add(Restrictions.ilike("nama", perguruanTinggiLain.getNama())).setMaxResults(1).uniqueResult();
		}
		System.out.println("existing = " + existing + ", perguruanTinggiLain.getKodePerguruanTinggi() = "
				+ perguruanTinggiLain.getKodePerguruanTinggi());
		if (existing == null) {
			existing = perguruanTinggiLain;
		} else {
			existing = FeederUtil.copyDataJikaKosong(perguruanTinggiLain, existing, PerguruanTinggiLain.class);
		}
		existing.setFeeder(perguruanTinggiLain.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();

		session.flush();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	@SuppressWarnings("unchecked")
	public static void matakuliah(JSONObject jsonObject, boolean melihatkode) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		try {
		Matakuliah matakuliah = new Matakuliah();
		matakuliah.setFeeder("" + jsonObject.get("id_matkul"));
		matakuliah.setNama("" + jsonObject.get("nama_mata_kuliah"));
		matakuliah.setKode("" + jsonObject.get("kode_mata_kuliah"));
		try {
			matakuliah.setJurusan(FeederUtil.getDataByFeeder(session, jsonObject.getString("id_prodi").trim(),
					Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1764");

		}

		try {
			matakuliah.setSks((int) Double.parseDouble("" + jsonObject.get("sks_mata_kuliah")));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		try {
			matakuliah.setSksDiskusi((int) Double.parseDouble("" + jsonObject.get("sks_tatap_muka")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1776");

		}

		try {
			matakuliah.setSksPraktek((int) Double.parseDouble("" + jsonObject.get("sks_praktek")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1782");

		}

		try {
			matakuliah.setSksPraktekLapangan((int) Double.parseDouble("" + jsonObject.get("sks_praktek_lapangan")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1788");

		}

		try {
			matakuliah.setSksSimulasi((int) Double.parseDouble("" + jsonObject.get("sks_simulasi")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1794");

		}
		try {
			matakuliah.setMetodeKuliah(("" + jsonObject.get("metode_kuliah")));
			matakuliah.setAdaSap(("" + jsonObject.get("ada_sap")).trim().equals("1"));

			matakuliah.setAdaBahanAjar(("" + jsonObject.get("ada_bahan_ajar")).trim().equals("1"));
			matakuliah.setAdaSilabus(("" + jsonObject.get("ada_silabus")).trim().equals("1"));
			matakuliah.setAdaAcaraPraktek(("" + jsonObject.get("ada_acara_praktek")).trim().equals("1"));
			matakuliah.setAdaDiktat(("" + jsonObject.get("ada_diktat")).trim().equals("1"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1805");

		}
		try {
			matakuliah.setTanggalMulai(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_mulai_efektif")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1810");

		}

		try {
			matakuliah.setTanggalSampai(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_selesai_efektif")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1816");

		}

//		if (matakuliah.getJurusan() == null) {
//			HibernateUtil.closeSession();
//			return;
//		}
//		if (matakuliah.getKode() == null || matakuliah.getKode().trim().isEmpty()) {
//			HibernateUtil.closeSession();
//			return;
//		}

		List<Matakuliah> matakuliahYgAdas = session.createCriteria(Matakuliah.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("feeder", matakuliah.getFeeder())).add(Restrictions.isNotNull("feeder"))
				.add(Restrictions.ne("feeder", "")).list();

		if (matakuliahYgAdas.isEmpty() && melihatkode) {
			matakuliahYgAdas.addAll(session.createCriteria(Matakuliah.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.ilike("kode", matakuliah.getKode()))
					.add(Restrictions.eq("jurusan", matakuliah.getJurusan())).list());
		}

		if (matakuliahYgAdas.isEmpty() && melihatkode) {
			String kode = org.apache.commons.lang3.StringUtils.replace(matakuliah.getKode(), "-", "");
			kode = org.apache.commons.lang3.StringUtils.replace(kode, " ", "");
			kode = org.apache.commons.lang3.StringUtils.replace(kode, " ", "");
			matakuliahYgAdas.addAll(session.createCriteria(Matakuliah.class).add(Restrictions.ilike("kode", kode))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("jurusan", matakuliah.getJurusan())).list());
		}

		if (matakuliahYgAdas.isEmpty() && !untukImportMatakuliahHanyaMenggunakanKodeFeeder && melihatkode) {
			if (matakuliah.getKode() != null && !matakuliah.getKode().trim().isEmpty()) {
				matakuliahYgAdas = session.createCriteria(Matakuliah.class)
						.add(Restrictions.ilike("kode", matakuliah.getKode()))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.isNull("feeder"), Restrictions.eq("feeder", "")))
						.add(Restrictions.eq("jurusan", matakuliah.getJurusan())).list();
			}

			if (matakuliahYgAdas.isEmpty() && matakuliah.getNama() != null && !matakuliah.getNama().trim().isEmpty()) {
				matakuliahYgAdas = session.createCriteria(Matakuliah.class)
						.add(Restrictions.ilike("nama", matakuliah.getNama()))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.or(Restrictions.isNull("feeder"), Restrictions.eq("feeder", "")))
						.add(Restrictions.eq("jurusan", matakuliah.getJurusan())).list();

			}

		}

		if (matakuliahYgAdas.isEmpty()) {

			if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, matakuliah);
			session.getTransaction().commit();
		} else {
			for (Matakuliah ex : matakuliahYgAdas) {
				ex = FeederUtil.copyDataJikaKosong(matakuliah, ex, Matakuliah.class);
				ex.setKode(matakuliah.getKode());
				ex.setNama(matakuliah.getNama());
				ex.setFeeder(matakuliah.getFeeder());
				ex.setFeeders(matakuliah.getFeeder());

				if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, ex);
				session.getTransaction().commit();
			}

		}
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

//	public final static String Laporan_akhir_studi = "1";
//	public final static String Aktivitas_kemahasiswaan = "10";
//	public final static String Program_kreativitas_mahasiswa = "11";
//	public final static String Kompetisi = "12";
//	public final static String Magang_Praktik_Kerja = "13";
//	public final static String Asistensi_Mengajar_di_Satuan_Pendidikan= "14";
//	public final static String Penelitian_Riset = "15";
//	public final static String Proyek_Kemanusiaan = "16";

	public final static Map<String, String> JENIS_KEGIATAN = new HashMap<String, String>();
//	public final static Map<String, String> KATEGORI_KEGIATAN = new HashMap<String, String>();
	static {
		String d = "[{\"id_jns_akt_mhs\":\"1\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Laporan akhir studi\"},{\"id_jns_akt_mhs\":\"10\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Aktivitas kemahasiswaan\"},{\"id_jns_akt_mhs\":\"11\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Program kreativitas mahasiswa\"},{\"id_jns_akt_mhs\":\"12\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kompetisi\"},{\"id_jns_akt_mhs\":\"13\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Magang\\/Praktik Kerja\"},{\"id_jns_akt_mhs\":\"14\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Asistensi Mengajar di Satuan Pendidikan\"},{\"id_jns_akt_mhs\":\"15\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Penelitian\\/Riset\"},{\"id_jns_akt_mhs\":\"16\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Proyek Kemanusiaan\"},{\"id_jns_akt_mhs\":\"17\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kegiatan Wirausaha\"},{\"id_jns_akt_mhs\":\"18\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Studi\\/Proyek Independen\"},{\"id_jns_akt_mhs\":\"19\",\"a_kegiatan_kampus_merdeka\":\"1\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Membangun Desa\\/Kuliah Kerja Nyata Tematik\"},{\"id_jns_akt_mhs\":\"2\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Tugas akhir\"},{\"id_jns_akt_mhs\":\"3\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Tesis\"},{\"id_jns_akt_mhs\":\"4\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Disertasi\"},{\"id_jns_akt_mhs\":\"5\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kuliah kerja nyata\"},{\"id_jns_akt_mhs\":\"6\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Kerja praktek\\/PKL\"},{\"id_jns_akt_mhs\":\"7\",\"a_kegiatan_kampus_merdeka\":\"0\",\"ket_jns_akt_mhs\":\"\",\"nm_jns_akt_mhs\":\"Bimbingan akademis\"}]";
		try {

			JSONArray jsonArray = new JSONArray(d);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				JENIS_KEGIATAN.put("" + jsonObject.get("nm_jns_akt_mhs"), jsonObject.getString("id_jns_akt_mhs"));

			}
		} catch (JSONException e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederJSONImport.java:1921");
		}

//		d = "[{\"id_katgiat\":\"110200\",\"nm_kat\":\"Membimbing seminar mahasiswa\"},{\"id_katgiat\":\"110300\",\"nm_kat\":\"Membimbing Kuliah Kerja Nyata, Praktek Kerja Nyata, Praktek Kerja Lapangan, termasuk membimbing pelatihan militer mahasiswa, pertukaran mahasiswa,  Magang, kuliah berbasis penelitian, wirausaha, dan bentuk lain pengabdian kepada masyarakat, dan sejenisnya\"},{\"id_katgiat\":\"110400\",\"nm_kat\":\"Membimbing dan ikut membimbing dalam menghasilkan disertasi, tesis, skripsi dan laporan akhir studi yang sesuai dengan bidang tugasnya\"},{\"id_katgiat\":\"110500\",\"nm_kat\":\"Bertugas sebagai penguji pada ujian akhir\\/profesi\"},{\"id_katgiat\":\"111000\",\"nm_kat\":\"Menduduki jabatan perguruan tinggi \"},{\"id_katgiat\":\"111100\",\"nm_kat\":\"Membimbing dosen yang lebih rendah jabatannya \"},{\"id_katgiat\":\"111200\",\"nm_kat\":\"Melaksanakan kegiatan Detasering dan Pencangkokan di luar institusi \"},{\"id_katgiat\":\"111300\",\"nm_kat\":\"Melakukan kegiatan pengembangan diri untuk meningkatkan kompetensi\\/memperoleh sertifikasi profesi \"},{\"id_katgiat\":\"110800\",\"nm_kat\":\"Mengembangkan bahan kuliah\"},{\"id_katgiat\":\"110700\",\"nm_kat\":\"Melakukan kegiatan pengembangan program kuliah tatap muka\\/daring (RPS, perangkat pembelajaran)\"},{\"id_katgiat\":\"110900\",\"nm_kat\":\"Melakukan kegiatan orasi ilmiah pada perguruan tinggi\"},{\"id_katgiat\":\"111400\",\"nm_kat\":\"Pendampingan, pembimbingan, mentoring mahasiswa secara terstruktur menghasilkan diantaranya: karya inovatif, karya teknologi yang bermanfaat bagi kesejahteraan masyarakat dan industri; proyek kewirausahaan; startup\\/usaha rintisan; magang industri; bina de\"},{\"id_katgiat\":\"110100\",\"nm_kat\":\"melaksanakan perkuliahan (pengajaran, tutorial, tatap muka, dan\\/atau daring) dalam rangka melaksanakan metode pembelajaran student centered learning (seperti problembased learning atau project basedlearning), membimbing\\/menguji dalam menghasilkan disertas\"},{\"id_katgiat\":\"110600\",\"nm_kat\":\"Membina kegiatan mahasiswa di bidang akademik dan kemahasiswaan, termasuk dalam kegiatan ini adalah membimbing mahasiswa menghasilkan produk saintifik, membimbing mahasiswa mengikuti kompetisi di bidang akademik dan kemahasiswaan\"}]";

//		try {
//			JSONArray jsonArray = new JSONArray(d);
//			for (int i = 0; i < jsonArray.length(); i++) {
//				JSONObject jsonObject = jsonArray.getJSONObject(i);
//				KATEGORI_KEGIATAN.put("" + jsonObject.get("nm_kat"), jsonObject.getString("id_katgiat"));
//			}
//		} catch (JSONException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1932");
//			e.printStackTrace();
//		}
	}

	public static void skripsi(JSONObject jsonObject, String idRegPd) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		try {
		Skripsi skripsi = new Skripsi();
		skripsi.setFeeder("" + jsonObject.get("id_akt_mhs"));
		skripsi.setJudul("" + jsonObject.get("judul_akt_mhs"));
		skripsi.setMahasiswa((Mahasiswa) session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("idRegPd", idRegPd.trim())).setMaxResults(1).uniqueResult());
		try {
			skripsi.setTahun(Integer.parseInt(("" + jsonObject.get("id_smt")).substring(0, 4)));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1948");

		}

		try {
			skripsi.setTahunAkademik(skripsi.getTahun() + "/" + (skripsi.getTahun() + 1));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1954");

		}

		try {
			Mahasiswa mahasiswa = skripsi.getMahasiswa();
			String idSmt = jsonObject.getString("id_smt");
			int tahun = Integer.parseInt(idSmt.substring(0, 4));
			Integer s = Integer.parseInt(idSmt.substring(4, 5));
			Integer currentSemester = Common.getSemester(mahasiswa.getTahunangkatan(),
					s.equals(1) ? Perkuliahan.GANJIL : Perkuliahan.GENAP, mahasiswa.getPindahKeKampusIniMasukSemester(),
					tahun, mahasiswa.getSemesterMulai());
			skripsi.setSemester(currentSemester);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1967");

		}

		try {
			skripsi.setLokasiUjian(("" + jsonObject.get("lokasi_kegiatan")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1973");

		}

		try {
			skripsi.setNomorSk(("" + jsonObject.get("sk_tugas")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1979");

		}

		try {
			skripsi.setTglSk(Common.databaseDateFormat.get().parse("" + jsonObject.get("tgl_sk_tugas")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:1985");

		}

		if (skripsi.getMahasiswa() == null) {
			HibernateUtil.closeSession();
			return;
		}
		if (skripsi.getJudul() == null || skripsi.getJudul().trim().isEmpty()) {
			HibernateUtil.closeSession();
			return;
		}

		Skripsi existing = (Skripsi) session.createCriteria(Skripsi.class)
				.add(Restrictions.eq("feeder", skripsi.getFeeder())).setMaxResults(1).uniqueResult();

		if (existing == null) {
			existing = (Skripsi) session.createCriteria(Skripsi.class)
					.add(Restrictions.eq("mahasiswa", skripsi.getMahasiswa()))
					.add(Restrictions.eq("semester", skripsi.getSemester())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = skripsi;
			existing.setFeeder(skripsi.getFeeder());
			if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		} else {
			existing = FeederUtil.copyDataJikaKosong(skripsi, existing, Skripsi.class);
			existing.setFeeder(skripsi.getFeeder());
			if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		}
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/**
	 * FIX "Session is closed!" saat commit (KE-2/KE-3/KE-4, thread latar impor Feeder pd
	 * {@code mahasiswa()}/{@code mahasiswa_aja()}): thread latar ini memproses BANYAK mahasiswa
	 * berturut-turut memakai satu Session/koneksi native yang sama sepanjang loop panjang. Guard
	 * {@code session.isOpen()} yang sudah ada di tiap titik simpan HANYA mengecek FLAG internal
	 * Hibernate -- TIDAK memverifikasi koneksi JDBC fisiknya masih hidup. Pada proses panjang,
	 * koneksi c3p0 bisa dievict (idle/unreturned-connection timeout) SETELAH guard tsb lolos tapi
	 * SEBELUM/SAAT {@code commit()} melakukan auto-flush -- persis pola KE-2/KE-3/KE-4 (guard di
	 * baris sebelumnya lolos, tapi baris {@code begin()}/{@code commit()} berikutnya tetap gagal).
	 * Solusi: bungkus begin+simpan+commit dgn SATU percobaan ulang penuh memakai Session BARU (bukan
	 * reuse yg mungkin basi) bila {@link org.hibernate.HibernateException} muncul -- pola yg sama
	 * dgn retry deadlock {@code KantinHelper.produkImporExcelKomit}, diterapkan di sini utk
	 * kegagalan sesi/koneksi.
	 *
	 * @return Session yang BENAR dipakai (bisa berbeda dari parameter {@code session} bila terjadi
	 *         retry) -- pemanggil WAJIB melanjutkan dgn nilai kembalian ini.
	 */
	private static Session simpanTransaksiFeederDenganRetry(Session session, ais.database.model.GeneralValueObject entitas,
			boolean pakaiRefreshUpdate) throws Exception {
		if (session == null || !session.isOpen()) {
			session = HibernateUtil.currentNativeSession();
		}
		try {
			session.getTransaction().begin();
			if (pakaiRefreshUpdate) {
				Common.refreshUpdate(session, entitas);
			} else {
				Common.refreshSaveOrUpdate(session, entitas);
			}
			session.getTransaction().commit();
			return session;
		} catch (org.hibernate.HibernateException eSesi) {
			ais.common.ErrorAuditUtil.record(eSesi,
					"FeederJSONImport.simpanTransaksiFeederDenganRetry -- sesi/koneksi basi saat commit, mencoba ulang dgn sesi baru");
			try {
				HibernateUtil.closeSession();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:retry-close"); }
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			if (pakaiRefreshUpdate) {
				Common.refreshUpdate(session, entitas);
			} else {
				Common.refreshSaveOrUpdate(session, entitas);
			}
			session.getTransaction().commit();
			return session;
		}
	}

	public static void mahasiswa(JSONObject jsonObject) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		try {
		BiodataMahasiswa biodataMahasiswa = new BiodataMahasiswa();
		Mahasiswa mahasiswa = new Mahasiswa();
		biodataMahasiswa.setMahasiswa(mahasiswa);
		mahasiswa.setFeeder("" + jsonObject.get("id_mahasiswa"));
		mahasiswa.setNama("" + jsonObject.get("nama_mahasiswa"));

		try {

//			if (jsonObject.isNull("nipd") || jsonObject.getString("nipd").trim().equalsIgnoreCase("null")) {
//				mahasiswa.setNim("" + jsonObject.get("id_mahasiswa"));
//			} else {

			mahasiswa.setNim(jsonObject.get("nipd") + "");
//			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2048");

		}

		try {

			mahasiswa.setIdRegPd("" + jsonObject.get("id_registrasi_mahasiswa"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2055");

		}
//		try {
//			if (!jsonObject.isNull("nik")) {
//				mahasiswa.setKtp(""+jsonObject.get("nik"));
//			}
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2062");
//
//		}
		try {

			if (jsonObject.isNull("id_prodi") || jsonObject.getString("id_prodi").trim().equalsIgnoreCase("null")) {
				Jurusan jurusan = (Jurusan) session.createCriteria(Jurusan.class).add(Restrictions.eq("kode", "X"))
						.setMaxResults(1).uniqueResult();
				if (jurusan != null) {
					mahasiswa.setJurusan(jurusan);
				}
			} else {

				if (!jsonObject.isNull("id_prodi")) {
					mahasiswa.setJurusan(
							FeederUtil.getDataByFeeder(session, jsonObject.getString("id_prodi").trim(), Jurusan.class,
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				} else if (!jsonObject.isNull("id_sms")) {
					mahasiswa.setJurusan(
							FeederUtil.getDataByFeeder(session, jsonObject.getString("id_sms").trim(), Jurusan.class,
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederJSONImport.java:2086");
		}

		System.out.println("mahasiswa.getJurusan() -> " + mahasiswa.getJurusan());

		if (mahasiswa.getJurusan() == null) {
			// session.disconnect();
			if (session.isOpen()) {session.disconnect();session.close();}
			HibernateUtil.closeSession();
			return;
		}

		try {
			mahasiswa.setStatusAwalMahasiswa(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_jns_daftar") + ""), StatusAwalMahasiswa.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2101");

		}

		try {
			if (!jsonObject.isNull("id_agama")) {
				biodataMahasiswa.setAgama(FeederUtil.getDataByFeeder(session,
						Long.parseLong(jsonObject.get("id_agama") + ""), Agama.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2111");

		}

//		try {
//			mahasiswa.setTanggalMasuk(Common.databaseDateFormat.get().parse(""+jsonObject.get("tgl_masuk_sp")));
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2117");
//
//		}
		try {

			StatusKeluar statusKeluar = FeederUtil.getDataByFeeder(session,
					jsonObject.getString("id_jns_keluar").trim(), StatusKeluar.class);
			Integer semesterLulus = Mahasiswa.hitungSmtLulus(statusKeluar, mahasiswa);
			mahasiswa.setSemesterLulus(semesterLulus);

			mahasiswa.setStatusKeluar(statusKeluar);

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2129");

		}

		try {
			mahasiswa.setTanggalLulus(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_keluar")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2135");

		}
		try {
			mahasiswa.setKeterangan("" + jsonObject.get("keterangan"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2140");

		}
		try {
			if (!jsonObject.isNull("nim")) {
				mahasiswa.setNim("" + jsonObject.get("nim"));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2147");

		}

		try {
			if (!jsonObject.isNull("nama_status_mahasiswa")) {
				if (jsonObject.get("nama_status_mahasiswa").toString().toLowerCase().trim().equals("lulus")) {
					mahasiswa.setStatusKeluar(new StatusKeluar(1L));
				} else {
					mahasiswa.setStatusKeluar(FeederUtil.getDataByFeeder(session,
							Long.parseLong(jsonObject.get("id_status_mahasiswa") + ""), StatusKeluar.class));
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2160");

		}

		try {
			mahasiswa.setTanggalLulus(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_keluar")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2166");

		}

//		try {
//			biodataMahasiswa.setApakahPernahPaud(""+jsonObject.get("a_pernah_paud").trim().equalsIgnoreCase("1"));
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2172");
//
//		}
//		try {
//			biodataMahasiswa.setApakahPernahTk(""+jsonObject.get("a_pernah_tk").trim().equalsIgnoreCase("1"));
//		} catch (Exception e) {
//
//		}
		try {
			mahasiswa.setTahunangkatan(Integer.parseInt(("" + jsonObject.get("id_periode")).trim().substring(0, 4)));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2182");

		}
		// try {
		// mahasiswa.setJenisSeleksi(
		// FeederUtil.getDataByFeeder(session,
		// jsonObject.getString("Mandiri").trim(), JenisSeleksi.class));
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2189");
		//
		// }
		try {
			mahasiswa.setSksYangDiakui(Integer.parseInt("" + jsonObject.get("sks_diakui")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2194");

		}

		try {
			mahasiswa.setNamaProdiPindah("" + jsonObject.get("nm_prodi_asal"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2200");

		}

		try {
			mahasiswa.setPindahanPerguruanTinggi("" + jsonObject.get("nm_pt_asal"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2206");

		}

		try {
			mahasiswa.setSemesterMulai(
					jsonObject.getString("id_periode").trim().substring(4).equalsIgnoreCase("1") ? Perkuliahan.GANJIL
							: Perkuliahan.GENAP);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2214");

		}
		try {
			mahasiswa.setJudulSkripsi("" + jsonObject.get("judul_skripsi"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2219");

		}

		try {
			mahasiswa.setBlnAwalBimbingan(Common.dateFormat1.get().parse("" + jsonObject.get("bln_awal_bimbingan")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2225");

		}

		try {
			mahasiswa.setBlnAkhirBimbingan(Common.dateFormat1.get().parse("" + jsonObject.get("bln_akhir_bimbingan")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2231");

		}

		try {
			mahasiswa.setNoAkta1("" + jsonObject.get("sk_yudisium"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2237");

		}

		try {
			mahasiswa.setTanggalYudisium(Common.dateFormat1.get().parse("" + jsonObject.get("tgl_sk_yudisium")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2243");

		}
		try {
			mahasiswa.setNoIjazah1("" + jsonObject.get("no_seri_ijazah"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2248");

		}
		try {
			mahasiswa.setPindahanPerguruanTinggi("" + jsonObject.get("nm_pt_asal"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2253");

		}

		Mahasiswa existing = null;

		if (existing == null && mahasiswa.getNim() != null) {
			existing = (Mahasiswa) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.sqlRestriction(
							"replace(trim(nim),'.','') = replace(trim('" + mahasiswa.getNim() + "'),'.','')"))
					.setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = mahasiswa;
		} else {
			existing = FeederUtil.copyDataJikaKosong(mahasiswa, existing, Mahasiswa.class);
		}
		existing.setFeeder(mahasiswa.getFeeder());

		if (mahasiswa.getStatusKeluar() != null && mahasiswa.getStatusKeluar().getId() != null) {
			existing.setStatusKeluar(mahasiswa.getStatusKeluar());

			if (mahasiswa.getTanggalLulus() != null) {
				existing.setTanggalLulus(mahasiswa.getTanggalLulus());
			}
			if (mahasiswa.getNoAkta1() != null && !mahasiswa.getNoAkta1().trim().isEmpty()) {
				existing.setNoAkta1(mahasiswa.getNoAkta1());
			}
			if (mahasiswa.getTanggalYudisium() != null) {
				existing.setTanggalYudisium(mahasiswa.getTanggalYudisium());
			}
			if (mahasiswa.getNoIjazah1() != null && !mahasiswa.getNoIjazah1().trim().isEmpty()) {
				existing.setNoIjazah1(mahasiswa.getNoIjazah1());
			}
		}

		if (mahasiswa.getKelamin() != null && !mahasiswa.getKelamin().trim().isEmpty()) {
			existing.setKelamin(mahasiswa.getKelamin());
		}

		try {
			existing.setTahunangkatan(Integer.parseInt(("" + jsonObject.get("id_periode")).trim().substring(0, 4)));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2297");

		}

		session = simpanTransaksiFeederDenganRetry(session, existing, false);

		BiodataMahasiswa biodataMahasiswaExisting = existing.ambilBiodata();

		FeederUtil.copyDataJikaKosong(biodataMahasiswa, biodataMahasiswaExisting, BiodataMahasiswa.class);
		biodataMahasiswaExisting.setMahasiswa(existing);

		if (!jsonObject.isNull("nisn") && !jsonObject.get("nisn").toString().isEmpty()) {
			biodataMahasiswaExisting.setNisn("" + jsonObject.get("nisn"));
		}

		session = simpanTransaksiFeederDenganRetry(session, biodataMahasiswaExisting, false);
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public static void mahasiswa_aja(JSONObject jsonObject) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		try {
		BiodataMahasiswa biodataMahasiswa = new BiodataMahasiswa();
		Mahasiswa mahasiswa = new Mahasiswa();
		biodataMahasiswa.setMahasiswa(mahasiswa);
		mahasiswa.setFeeder("" + jsonObject.get("id_mahasiswa"));
		mahasiswa.setNama("" + jsonObject.get("nama_mahasiswa"));

		mahasiswa.setKelamin(
				jsonObject.getString("jenis_kelamin").trim().equalsIgnoreCase("L") ? "Laki-laki" : "Perempuan");
		try {
			if (!jsonObject.isNull("id_agama")) {
				mahasiswa.setAgama(FeederUtil.getDataByFeeder(session, Long.parseLong(jsonObject.get("id_agama") + ""),
						Agama.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				biodataMahasiswa.setAgama(mahasiswa.getAgama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2342");

		}

		try {
			if (!jsonObject.isNull("nama_status_mahasiswa")) {
				if (jsonObject.get("nama_status_mahasiswa").toString().toLowerCase().trim().equals("lulus")) {
					mahasiswa.setStatusKeluar(new StatusKeluar(1L));
				} else {
					mahasiswa.setStatusKeluar(FeederUtil.getDataByFeeder(session,
							Long.parseLong(jsonObject.get("id_status_mahasiswa") + ""), StatusKeluar.class));
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2355");

		}

		try {
			if (!jsonObject.isNull("nim")) {
				mahasiswa.setNim("" + jsonObject.get("nim"));
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2363");

		}

		try {
			mahasiswa.setTanggalLulus(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_keluar")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2369");

		}

		try {
			mahasiswa.setTempatlahir("" + jsonObject.get("tempat_lahir"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2375");

		}
		try {
			mahasiswa.setTanggallahir(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_lahir")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2380");

		}
		if (!jsonObject.isNull("jalan")) {
			biodataMahasiswa.setAlamat("" + jsonObject.get("jalan"));
		}
		if (!jsonObject.isNull("nik")) {
			String nik = jsonObject.getString("nik").trim();
			System.out.println("nik = > " + nik);
			mahasiswa.setKtp(nik);
			biodataMahasiswa.setNoIdentitas(nik);
		}
		if (!jsonObject.isNull("nisn") && !jsonObject.get("nisn").toString().isEmpty()) {
			biodataMahasiswa.setNisn("" + jsonObject.get("nisn"));
		}
		if (!jsonObject.isNull("rt")) {
			biodataMahasiswa.setRt("" + jsonObject.get("rt"));
		}
		if (!jsonObject.isNull("rw")) {
			biodataMahasiswa.setRw("" + jsonObject.get("rw"));
		}
		if (!jsonObject.isNull("dusun")) {
			biodataMahasiswa.setDusun("" + jsonObject.get("dusun"));
		}
		if (!jsonObject.isNull("kelurahan")) {
			biodataMahasiswa.setKelurahan("" + jsonObject.get("kelurahan"));
		}
		try {
			if (!jsonObject.isNull("id_wilayah")) {
				String idwil = jsonObject.getString("id_wilayah");
				System.out.println("idwil = > " + idwil);

				Wilayah wilayah = FeederUtil.getDataByFeeder(session, idwil.trim(), Wilayah.class,
						Restrictions.isNotNull("wilayahInduk"));
				if (wilayah == null) {
					wilayah = FeederUtil.getDataByFeeder(session, idwil.trim(), Wilayah.class);
				}
				System.out.println(
						"wilayah = > " + wilayah + " induk " + (wilayah == null ? "" : wilayah.getWilayahInduk()));
				biodataMahasiswa.setKecamatan(wilayah);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2421");
//			Common.tampilErrorJikaAdmin(e);
		}
		if (!jsonObject.isNull("kode_pos")) {
			biodataMahasiswa.setKodepos("" + jsonObject.get("kode_pos"));
		}
		try {
			biodataMahasiswa.setJenisTinggalMahasiswa(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_jenis_tinggal") + ""), JenisTinggalMahasiswa.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2430");

		}
		try {
			biodataMahasiswa.setAlatTransportasiMahasiswa(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_alat_transportasi") + ""), AlatTransportasiMahasiswa.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2436");

		}
		if (!jsonObject.isNull("telepon")) {
			biodataMahasiswa.setTeleponRumah("" + jsonObject.get("telepon"));
		}
		if (!jsonObject.isNull("handphone")) {
			biodataMahasiswa.setHp("" + jsonObject.get("handphone"));
			mahasiswa.setTelp("" + jsonObject.get("handphone"));
		}
		if (!jsonObject.isNull("email")) {
			biodataMahasiswa.setEmail("" + jsonObject.get("email"));
			mahasiswa.setEmail("" + jsonObject.get("email"));
		}

		if (!jsonObject.isNull("nama_ayah")) {
			biodataMahasiswa.setNamaAyah("" + jsonObject.get("nama_ayah"));
		}
		if (!jsonObject.isNull("nik_ayah")) {
			biodataMahasiswa.setNikAyah("" + jsonObject.get("nik_ayah"));
		}
		if (!jsonObject.isNull("tanggal_lahir_ayah")) {
			try {
				biodataMahasiswa
						.setTanggalLahirAyah(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_lahir_ayah")));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2461");

			}
		}
		try {
			biodataMahasiswa.setJenjangPendidikanAyah(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_pendidikan_ayah") + ""), Jenjang.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2468");

		}
		try {
			biodataMahasiswa.setJenisPekerjaanAyah(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_pekerjaan_ayah") + ""), Pekerjaan.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2474");

		}
		try {
			biodataMahasiswa.setJenisPenghasilanAyah(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_penghasilan_ayah") + ""), Penghasilan.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2480");

		}

		if (!jsonObject.isNull("nama_ibu_kandung")) {
			biodataMahasiswa.setNamaIbu("" + jsonObject.get("nama_ibu_kandung"));
		}

		if (!jsonObject.isNull("nik_ibu")) {
			biodataMahasiswa.setNikIbu("" + jsonObject.get("nik_ibu"));
		}

		if (!jsonObject.isNull("tanggal_lahir_ibu")) {
			try {
				biodataMahasiswa.setTanggalLahirIbu(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_lahir_ibu")));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2495");

			}
		}
		try {
			biodataMahasiswa.setJenjangPendidikanIbu(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_pendidikan_ibu") + ""), Jenjang.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2502");

		}
		try {
			biodataMahasiswa.setJenisPekerjaanIbu(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_pekerjaan_ibu") + ""), Pekerjaan.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2508");

		}
		try {
			biodataMahasiswa.setJenisPenghasilanIbu(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_penghasilan_ibu") + ""), Penghasilan.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2514");

		}

		if (!jsonObject.isNull("nama_wali")) {
			biodataMahasiswa.setNamaWali("" + jsonObject.get("nama_wali"));
		}
		if (!jsonObject.isNull("tanggal_lahir_wali")) {
			try {
				biodataMahasiswa
						.setTanggalLahirWali(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_lahir_wali")));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2525");

			}
		}
		try {
			biodataMahasiswa.setJenjangPendidikanWali(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_pendidikan_wali") + ""), Jenjang.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2532");

		}
		try {
			biodataMahasiswa.setJenisPekerjaanWali(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_pekerjaan_wali") + ""), Pekerjaan.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2538");

		}
		try {
			biodataMahasiswa.setJenisPenghasilanWali(FeederUtil.getDataByFeeder(session,
					Long.parseLong(jsonObject.get("id_penghasilan_wali") + ""), Penghasilan.class));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2544");

		}

		Mahasiswa existing = null;

		if (existing == null && mahasiswa.getFeeder() != null && !mahasiswa.getFeeder().trim().isEmpty()) {
			existing = (Mahasiswa) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("feeder", mahasiswa.getFeeder())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			HibernateUtil.closeSession();
			return;
		} else {
			existing = FeederUtil.copyDataJikaKosong(mahasiswa, existing, Mahasiswa.class);
		}
		existing.setFeeder(mahasiswa.getFeeder());
		if (mahasiswa.getKelamin() != null && !mahasiswa.getKelamin().trim().isEmpty()) {
			existing.setKelamin(mahasiswa.getKelamin());
		}
		if (mahasiswa.getTanggallahir() != null) {
			existing.setTanggallahir(mahasiswa.getTanggallahir());
		}

		session = simpanTransaksiFeederDenganRetry(session, existing, false);

		try {
			if (!jsonObject.isNull("stat_pd")) {
				String stat_pd = jsonObject.getString("stat_pd").trim();
				StatusMahasiswa statusMahasiswa = (StatusMahasiswa) ConstantValues.simpleObject(
						session.createCriteria(StatusMahasiswa.class).addOrder(Order.asc("id"))
								.add(Restrictions.ilike("kodeEpsbed", stat_pd)).setMaxResults(1),
						StatusMahasiswa.class);
				System.out.println("statusMahasiswa => " + statusMahasiswa);
				if (statusMahasiswa != null) {
					Integer semester = mahasiswa.currentSemester();
					HistoryStatusMahasiswa historyStatusMahasiswa = (HistoryStatusMahasiswa) session
							.createCriteria(HistoryStatusMahasiswa.class).add(Restrictions.isNull("sp"))
							.add(Restrictions.eq("mahasiswa", existing)).add(Restrictions.eq("semester", semester))
							.setMaxResults(1).uniqueResult();

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);

					if (historyStatusMahasiswa == null) {
						historyStatusMahasiswa = new HistoryStatusMahasiswa(Common.getCurrentTahunAkademik(),
								krsMahasiswa.getSksBukanKonversi(), krsMahasiswa.getSemesterPendek());
						historyStatusMahasiswa.setMahasiswa(existing);
						historyStatusMahasiswa.setSemester(semester);
					}
					historyStatusMahasiswa.setSks(krsMahasiswa.getSksBukanKonversi());
					historyStatusMahasiswa.setStatusMahasiswa(statusMahasiswa);
					session = simpanTransaksiFeederDenganRetry(session, historyStatusMahasiswa, true);
				}

			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		BiodataMahasiswa biodataMahasiswaExisting = existing.ambilBiodata();
		FeederUtil.copyDataJikaKosong(biodataMahasiswa, biodataMahasiswaExisting, BiodataMahasiswa.class);
		biodataMahasiswaExisting.setMahasiswa(existing);
		if (biodataMahasiswaExisting.getNamaIbu() == null
				|| biodataMahasiswaExisting.getNamaIbu().trim().equalsIgnoreCase("IBU")) {
			biodataMahasiswaExisting.setNamaIbu(biodataMahasiswa.getNamaIbu());
		}
		if (biodataMahasiswaExisting.getNamaAyah() == null
				|| biodataMahasiswaExisting.getNamaAyah().trim().equalsIgnoreCase("AYAH")) {
			biodataMahasiswaExisting.setNamaAyah(biodataMahasiswa.getNamaAyah());
		}

		if (biodataMahasiswa.getKecamatan() != null) {
			if (biodataMahasiswaExisting.getKecamatan() == null
					|| biodataMahasiswaExisting.getKecamatan().getWilayahInduk() == null) {
				biodataMahasiswaExisting.setKecamatan(biodataMahasiswa.getKecamatan());
			}
		}

		if (!jsonObject.isNull("nisn") && !jsonObject.get("nisn").toString().isEmpty()) {
			biodataMahasiswaExisting.setNisn("" + jsonObject.get("nisn"));
		}

		session = simpanTransaksiFeederDenganRetry(session, biodataMahasiswaExisting, false);

		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	@SuppressWarnings("unchecked")
	public static void kurikulum(JSONObject jsonObject) throws Exception {
		Session session = HibernateUtil.currentNativeSession();
		try {
		Kurikulum kurikulum = new Kurikulum();
		kurikulum.setFeeder("" + jsonObject.get("id_kurikulum"));
		System.out.println("kurikulum => " + kurikulum.getFeeder());
		kurikulum.setNamaAsli("" + jsonObject.get("nama_kurikulum"));
		kurikulum.setNama("" + jsonObject.get("nama_kurikulum"));
		try {
			kurikulum.setJurusan(FeederUtil.getDataByFeeder(session, jsonObject.getString("id_prodi").trim(),
					Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2657");

		}

		try {
			kurikulum.setTahun(Integer.parseInt(("" + jsonObject.get("id_semester")).substring(0, 4)));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2663");

		}

		try {
			kurikulum.setJenisSemester(
					Integer.parseInt(("" + jsonObject.get("id_semester")).substring(4, 5)) == 1 ? Perkuliahan.GANJIL
							: Perkuliahan.GENAP);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2671");

		}

		try {
			kurikulum.setTahunAkademik(kurikulum.getTahun() + "/" + (kurikulum.getTahun() + 1));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2677");

		}

		kurikulum.setJumlahAturanSksLulus(jsonObject.getInt("jumlah_sks_lulus"));
		kurikulum.setJumlahAturanSksWajib(jsonObject.getInt("jumlah_sks_wajib"));
		kurikulum.setJumlahAturanSksPilihan(jsonObject.getInt("jumlah_sks_pilihan"));

		Kurikulum existing = (Kurikulum) session.createCriteria(Kurikulum.class)
				.add(Restrictions.eq("feeder", kurikulum.getFeeder())).setMaxResults(1).uniqueResult();

		String tahunAkademik = kurikulum.getTahun() + "/" + (kurikulum.getTahun() + 1);

		List<Kurikulum> kurikulums = session.createCriteria(Kurikulum.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(Restrictions.eq("jenisSemester", kurikulum.getJenisSemester()))
				.add(Restrictions.or(Restrictions.isNull("feeder"), Restrictions.eq("feeder", "")))
				.add(Restrictions.eq("jurusan", kurikulum.getJurusan())).list();
		if (existing == null && !kurikulums.isEmpty()) {
			existing = kurikulums.get(0);
		}

		System.out.println("existing -> " + existing + ", tahunAkademik -> " + tahunAkademik + " "
				+ kurikulum.getJenisSemester() + " " + kurikulum.getJurusan() + " " + kurikulums.size());

		if (existing != null) {
			kurikulums.add(existing);
		}

		if (existing == null) {
			existing = kurikulum;
			existing.setNama(kurikulum.getNama());
			existing.setFeeder(kurikulum.getFeeder());
			existing.setFeeders(kurikulum.getFeeder() + ";" + existing.getFeeders());

			if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
		} else {
			for (Kurikulum ex : kurikulums) {
				ex = FeederUtil.copyDataJikaKosong(kurikulum, ex, Kurikulum.class);
				ex.setNama(kurikulum.getNama());
				ex.setNamaAsli("" + jsonObject.get("nama_kurikulum"));
				ex.setFeeder(kurikulum.getFeeder());
				ex.setFeeders(kurikulum.getFeeder() + ";" + existing.getFeeders());

				if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, ex);
				session.getTransaction().commit();
			}
		}

		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void ajar_dosen(JSONObject jsonObject) throws Exception {

		if (jsonObject.isNull("id_reg_ptk") || jsonObject.isNull("id_kls")) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		try {
		PenugasanDosenMengajar penugasanDosenMengajar = FeederUtil.getDataByFeeder(session,
				jsonObject.getString("id_reg_ptk").trim(), PenugasanDosenMengajar.class);
		if (penugasanDosenMengajar == null || penugasanDosenMengajar.getDosen() == null) {
			return;
		}

		Perkuliahan perkuliahan = FeederUtil.getDataByFeeder(session, jsonObject.getString("id_kls").trim(),
				Perkuliahan.class);
		if (perkuliahan == null) {
			return;
		}

		Dosen dosen = penugasanDosenMengajar.getDosen();
		Collection<Dosen> dosens = perkuliahan.populateDosen().values();
		boolean ada = dosens.contains(dosen);
		System.out.println("ada => " + ada + ", dosen => " + dosen + ", dosens yg ada => " + dosens);
		if (ada) {
			return;
		}
		perkuliahan.setJumlahDosen(dosens.size() + 1);

		if (perkuliahan.getDosen1() == null) {
			perkuliahan.setDosen1(dosen);
		} else if (perkuliahan.getDosen2() == null) {
			perkuliahan.setDosen2(dosen);
		} else if (perkuliahan.getDosen3() == null) {
			perkuliahan.setDosen3(dosen);
		} else if (perkuliahan.getDosen4() == null) {
			perkuliahan.setDosen4(dosen);
		} else if (perkuliahan.getDosen5() == null) {
			perkuliahan.setDosen5(dosen);
		} else if (perkuliahan.getDosen6() == null) {
			perkuliahan.setDosen6(dosen);
		} else if (perkuliahan.getDosen7() == null) {
			perkuliahan.setDosen7(dosen);
		} else if (perkuliahan.getDosen8() == null) {
			perkuliahan.setDosen8(dosen);
		} else if (perkuliahan.getDosen9() == null) {
			perkuliahan.setDosen9(dosen);
		} else if (perkuliahan.getDosen10() == null) {
			perkuliahan.setDosen10(dosen);
		}

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, perkuliahan);
		session.getTransaction().commit();

		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public static void dosen_pt(JSONObject jsonObject) throws Exception {

		if (jsonObject.isNull("id_registrasi_dosen")) {
			return;
		}

		Session session = HibernateUtil.currentNativeSession();
		try {
		PenugasanDosenMengajar penugasanDosenMengajar = new PenugasanDosenMengajar();

		penugasanDosenMengajar.setFeeder("" + jsonObject.get("id_registrasi_dosen"));

		if (!jsonObject.isNull("id_dosen")) {
			try {
				penugasanDosenMengajar.setDosen(
						FeederUtil.getDataByFeeder(session, jsonObject.getString("id_dosen").trim(), Dosen.class));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2816");

			}
		}

		try {
			penugasanDosenMengajar.setJurusan(
					FeederUtil.getDataByFeeder(session, jsonObject.getString("id_prodi").trim(), Jurusan.class,
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2825");

		}

		penugasanDosenMengajar.setTahunAkademik("" + jsonObject.get("nama_tahun_ajaran"));
		try {
			penugasanDosenMengajar.setTmtSuratTugas(Common.dateFormat.get().parse("" + jsonObject.get("mulai_surat_tugas")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2832");

		}
		try {
			penugasanDosenMengajar
					.setTanggalSuratTugas(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_surat_tugas")));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2838");

		}

		penugasanDosenMengajar.setKode("" + jsonObject.get("nomor_surat_tugas"));

		try {
			penugasanDosenMengajar.setTahun(jsonObject.getInt("id_tahun_ajaran"));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2846");

		}

		PenugasanDosenMengajar existing = (PenugasanDosenMengajar) session.createCriteria(PenugasanDosenMengajar.class)
				.add(Restrictions.eq("feeder", penugasanDosenMengajar.getFeeder())).setMaxResults(1).uniqueResult();

		if (existing == null) {
			existing = (PenugasanDosenMengajar) session.createCriteria(PenugasanDosenMengajar.class)
					.add(Restrictions.eq("dosen", penugasanDosenMengajar.getDosen()))
					.add(Restrictions.eq("tahunAkademik", penugasanDosenMengajar.getTahunAkademik()))
					.add(Restrictions.eq("jurusan", penugasanDosenMengajar.getJurusan())).setMaxResults(1)
					.uniqueResult();
		}

		if (existing == null) {
			existing = penugasanDosenMengajar;
		} else {
			existing = FeederUtil.copyDataJikaKosong(penugasanDosenMengajar, existing, PenugasanDosenMengajar.class);
		}

		existing.setFeeder(penugasanDosenMengajar.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();

		Dosen dosen = existing.getDosen();
		if (dosen != null) {
			dosen.setIdRegPtk(existing.getFeeder());
			dosen.setJurusan(existing.getJurusan());
			if (existing.getJurusan() != null) {
				dosen.setFakultas(existing.getJurusan() != null ? existing.getJurusan().getFakultas() : null);
				dosen.setPerguruanTinggi(
						existing.getJurusan() != null ? existing.getJurusan().getFakultas().getPerguruanTinggi()
								: null);
			}
			if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, dosen);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public static void dosen(JSONObject jsonObject) throws Exception {

		Session session = HibernateUtil.currentNativeSession();
		try {
		BiodataDosen biodataDosen = new BiodataDosen();
		Dosen dosen = new Dosen();
		if (!jsonObject.isNull("id_dosen")) {
			dosen.setFeeder("" + jsonObject.get("id_dosen"));
		} else if (!jsonObject.isNull("id_ptk")) {
			dosen.setFeeder("" + jsonObject.get("id_ptk"));
		}

		if (!jsonObject.isNull("nama_dosen")) {
			dosen.setNama("" + jsonObject.get("nama_dosen"));
		}

		if (!jsonObject.isNull("id_ikatan_kerja")) {
			dosen.setIkatanKerjaDosen(FeederUtil.getDataByFeeder(session,
					jsonObject.getString("id_ikatan_kerja").trim(), IkatanKerjaDosen.class));
		}

		if (!jsonObject.isNull("nidn")) {
			dosen.setNidn("" + jsonObject.get("nidn"));
		}
		if (!jsonObject.isNull("npwp")) {
			dosen.setNpwp("" + jsonObject.get("npwp"));
		}

		if (!jsonObject.isNull("nip")) {
			dosen.setCode("" + jsonObject.get("nip"));
		}
		if (!jsonObject.isNull("nip")) {
			dosen.setMycode("" + jsonObject.get("nip"));
		}

		if (!jsonObject.isNull("jenis_kelamin")) {
			dosen.setKelamin(
					jsonObject.getString("jenis_kelamin").trim().equalsIgnoreCase("L") ? "Laki-laki" : "Perempuan");
		}

		if (!jsonObject.isNull("tempat_lahir")) {
			dosen.setTempatlahir("" + jsonObject.get("tempat_lahir"));
		}

		if (!jsonObject.isNull("tanggal_lahir")) {
			try {
				dosen.setTanggallahir(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_lahir")));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2943");

			}
		}

		if (!jsonObject.isNull("nik")) {
			dosen.setKtp("" + jsonObject.get("nik"));
		}

		if (!jsonObject.isNull("niy_nigk")) {
			dosen.setNiyNigk("" + jsonObject.get("niy_nigk"));
		}

		if (!jsonObject.isNull("nuptk")) {
			dosen.setNuptk("" + jsonObject.get("nuptk"));
		}

		if (!jsonObject.isNull("id_stat_pegawai")) {
			dosen.setStatusKepegawaian(FeederUtil.getDataByFeeder(session,
					jsonObject.getString("id_stat_pegawai").trim(), StatusKepegawaian.class));
		}
		if (!jsonObject.isNull("id_jns_ptk")) {
			dosen.setJenisPendidikDanTenagaKependidikan(FeederUtil.getDataByFeeder(session,
					jsonObject.getString("id_jns_ptk").trim(), JenisPendidikDanTenagaKependidikan.class));
		}

		if (!jsonObject.isNull("jalan")) {
			dosen.setAlamat("" + jsonObject.get("jalan"));
			biodataDosen.setAlamat("" + jsonObject.get("jalan"));
		}

		if (!jsonObject.isNull("rt")) {
			biodataDosen.setRt("" + jsonObject.get("rt"));
		}

		if (!jsonObject.isNull("rw")) {
			biodataDosen.setRw("" + jsonObject.get("rw"));
		}

		if (!jsonObject.isNull("dusun")) {
			biodataDosen.setDusun("" + jsonObject.get("dusun"));
		}

		if (!jsonObject.isNull("ds_kel")) {
			biodataDosen.setKelurahan("" + jsonObject.get("ds_kel"));
		}

		if (!jsonObject.isNull("id_wilayah")) {
			try {
				biodataDosen.setKecamatan(
						FeederUtil.getDataByFeeder(session, jsonObject.getString("id_wilayah").trim(), Wilayah.class));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:2994");

			}
		}

		if (!jsonObject.isNull("kode_pos")) {
			biodataDosen.setKodepos("" + jsonObject.get("kode_pos"));
		}

		if (!jsonObject.isNull("telepon")) {
			biodataDosen.setTeleponRumah("" + jsonObject.get("telepon"));
			dosen.setTelp("" + jsonObject.get("telepon"));
		}

		if (!jsonObject.isNull("handphone")) {
			biodataDosen.setHp("" + jsonObject.get("handphone"));
		}

		if (!jsonObject.isNull("email")) {
			dosen.setEmail("" + jsonObject.get("email"));
		}

		if (!jsonObject.isNull("id_sp")) {
			try {
				dosen.setPerguruanTinggi(FeederUtil.getDataByFeeder(session, jsonObject.getString("id_sp").trim(),
						PerguruanTinggi.class));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:3020");

			}
		}

		if (!jsonObject.isNull("id_status_aktif")) {
			try {
				dosen.setStatusPegawai(FeederUtil.getDataByFeeder(session,
						jsonObject.getString("id_status_aktif").trim(), StatusPegawai.class));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:3029");

			}
		}

		if (!jsonObject.isNull("no_sk_cpns")) {
			dosen.setSkCpns("" + jsonObject.get("no_sk_cpns"));
		}

		if (!jsonObject.isNull("tanggal_sk_cpns")) {
			try {
				dosen.setTglSkCpns(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_sk_cpns")));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:3041");

			}
		}

		if (!jsonObject.isNull("no_sk_pengangkatan")) {
			dosen.setSkAngkat("" + jsonObject.get("no_sk_pengangkatan"));
		}

		if (!jsonObject.isNull("mulai_sk_pengangkatan")) {
			try {
				dosen.setTmtSkAngkat(Common.dateFormat1.get().parse("" + jsonObject.get("mulai_sk_pengangkatan")));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:3053");

			}
		}

		if (!jsonObject.isNull("id_lembaga_pengangkatan")) {
			try {
				dosen.setLembagaPengangkat(FeederUtil.getDataByFeeder(session,
						jsonObject.getString("id_lembaga_pengangkatan").trim(), LembagaPengangkat.class));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:3062");

			}
		}

		if (!jsonObject.isNull("id_pangkat_golongan")) {
			try {
				dosen.setGolonganPegawai(FeederUtil.getDataByFeeder(session,
						jsonObject.getString("id_pangkat_golongan").trim(), Golongan.class));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:3071");

			}
		}

		if (!jsonObject.isNull("id_sumber_gaji")) {
			try {
				dosen.setSumberGaji(FeederUtil.getDataByFeeder(session, jsonObject.getString("id_sumber_gaji").trim(),
						SumberGaji.class));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:3080");

			}
		}

		if (!jsonObject.isNull("nama_ibu_kandung")) {
			biodataDosen.setNamaIbu("" + jsonObject.get("nama_ibu_kandung"));
		}

		if (!jsonObject.isNull("status_pernikahan")) {
			try {
				biodataDosen.setStatusNikah(jsonObject.getInt("status_pernikahan"));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:3092");

			}
		}

		if (!jsonObject.isNull("nama_suami_istri")) {
			biodataDosen.setNamaSuamiIstri("" + jsonObject.get("nama_suami_istri"));
		}
		if (!jsonObject.isNull("nip_suami_istri")) {
			biodataDosen.setNipSuamiIstri("" + jsonObject.get("nip_suami_istri"));
		}

		if (!jsonObject.isNull("tanggal_mulai_pns")) {
			try {
				dosen.setTmtPns(Common.dateFormat1.get().parse("" + jsonObject.get("tanggal_mulai_pns")));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:3107");

			}
		}

		if (!jsonObject.isNull("a_lisensi_kepsek")) {
			dosen.setaLisensiKepsek(("" + jsonObject.get("a_lisensi_kepsek")).trim().equals("1"));
		}

		if (!jsonObject.isNull("jml_sekolah_binaan")) {
			try {
				dosen.setJmlSekolahBinaan(jsonObject.getInt("jml_sekolah_binaan"));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:3119");

			}
		}

		if (!jsonObject.isNull("a_diklat_awas")) {
			dosen.setaDiklatAwas(("" + jsonObject.get("a_diklat_awas")).trim().equals("1"));
		}
		if (!jsonObject.isNull("akta_ijin_ajar")) {
			dosen.setAktaIjinAjar("" + jsonObject.get("akta_ijin_ajar"));
		}
		if (!jsonObject.isNull("nira")) {
			dosen.setNira("" + jsonObject.get("nira"));
		}
		if (!jsonObject.isNull("a_braille")) {
			dosen.setaBraille(("" + jsonObject.get("a_braille")).trim().equals("1"));
		}
		if (!jsonObject.isNull("a_bhs_isyarat")) {
			dosen.setaBhsIsyarat(("" + jsonObject.get("a_bhs_isyarat")).trim().equals("1"));
		}

		if (!jsonObject.isNull("kewarganegaraan")) {
			biodataDosen.setKewarganegaraanFeeder(("" + jsonObject.get("kewarganegaraan")));
		}

		if (!jsonObject.isNull("id_agama")) {
			try {
				biodataDosen.setAgama(FeederUtil.getDataByFeeder(session,
						Long.parseLong(jsonObject.get("id_agama") + ""), Agama.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/feeder/util/FeederJSONImport.java:3149");

			}
		}

		Dosen existing = null;
		if (dosen.getNidn() != null && !dosen.getNidn().trim().isEmpty()) {
			existing = (Dosen) session.createCriteria(Dosen.class)
					.add(Restrictions.sqlRestriction(
							"replace(trim(nidn),'.','') = replace(trim('" + dosen.getNidn() + "'),'.','')"))
					.setMaxResults(1).uniqueResult();
		}
		if (existing == null && dosen.getNuptk() != null && !dosen.getNuptk().trim().isEmpty()) {
			existing = (Dosen) session.createCriteria(Dosen.class)
					.add(Restrictions.sqlRestriction(
							"replace(trim(nuptk),'.','') = replace(trim('" + dosen.getNuptk() + "'),'.','')"))
					.setMaxResults(1).uniqueResult();

		}

		// if (existing == null && dosen.getCode() != null &&
		// !dosen.getCode().trim().isEmpty()) {
		// existing = (Dosen) session.createCriteria(Dosen.class)
		// .add(Restrictions.sqlRestriction(
		// "replace(trim(code),'.','') = replace(trim('" + dosen.getCode() +
		// "'),'.','')"))
		// .setMaxResults(1).uniqueResult();
		// }

		// if (existing == null && dosen.getMycode() != null &&
		// !dosen.getMycode().trim().isEmpty()) {
		// existing = (Dosen) session.createCriteria(Dosen.class)
		// .add(Restrictions.sqlRestriction(
		// "replace(trim(mycode),'.','') = replace(trim('" + dosen.getMycode() +
		// "'),'.','')"))
		// .setMaxResults(1).uniqueResult();
		// }

		if (existing == null) {
			existing = dosen;
		} else {
			existing = FeederUtil.copyDataJikaKosong(dosen, existing, Dosen.class);
		}

		existing.setFeeder(dosen.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();

		BiodataDosen biodataDosenExisting = (BiodataDosen) session.createCriteria(BiodataDosen.class)
				.add(Restrictions.eq("dosen", existing)).setMaxResults(1).uniqueResult();
		if (biodataDosenExisting == null) {
			biodataDosenExisting = new BiodataDosen();
		}
		FeederUtil.copyDataJikaKosong(biodataDosen, biodataDosenExisting, BiodataDosen.class);
		biodataDosenExisting.setDosen(existing);

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, biodataDosenExisting);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public static void wilayah(JSONObject jsonObject) throws Exception {

		Wilayah wilayah = new Wilayah();
		wilayah.setFeeder("" + jsonObject.get("id_wilayah"));
		wilayah.setNama("" + jsonObject.get("nama_wilayah"));
		wilayah.setInduk("" + jsonObject.get("id_wilayah"));
		wilayah.setLevel("" + jsonObject.get("id_level_wilayah"));
		wilayah.setNegara("" + jsonObject.get("id_negara"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		Wilayah existing = (Wilayah) session.createCriteria(Wilayah.class)
				.add(Restrictions.eq("feeder", wilayah.getFeeder())).setMaxResults(1).uniqueResult();
		// if (existing == null) {
		// existing = (Wilayah) session.createCriteria(Wilayah.class)
		// .add(Restrictions.ilike("nama", wilayah.getNama()))
		// .setMaxResults(1).uniqueResult();
		// }

		if (existing == null) {
			existing = wilayah;
		}
		existing.setNama(wilayah.getNama());
		existing.setFeeder(wilayah.getFeeder());
		existing.setLevel(wilayah.getLevel());
		existing.setNegara(wilayah.getNegara());
		existing.setInduk(wilayah.getInduk());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();

		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void fakultas(JSONObject jsonObject) throws Exception {

		if (!jsonObject.isNull("id_sms") && jsonObject.getInt("id_jns_sms") == 1) {
			System.out.println("fakultas = " + jsonObject);
			Session session = HibernateUtil.currentNativeSession();

			Fakultas fakultas = new Fakultas();
			fakultas.setFeeder("" + jsonObject.get("id_sms"));
			fakultas.setNama("" + jsonObject.get("nm_lemb"));
			fakultas.setKode("" + jsonObject.get("kode_prodi"));

			Fakultas existing = (Fakultas) session.createCriteria(Fakultas.class)
					.add(Restrictions.eq("feeder", fakultas.getFeeder())).setMaxResults(1).uniqueResult();

			if (existing == null) {
				existing = (Fakultas) session.createCriteria(Fakultas.class)
						.add(Restrictions.eq("kode", fakultas.getKode())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				existing = (Fakultas) session.createCriteria(Fakultas.class)
						.add(Restrictions.ilike("nama", fakultas.getNama())).setMaxResults(1).uniqueResult();
			}

			if (existing == null) {
				System.out.println("fakultas " + fakultas + " tidak ada");
				existing = fakultas;
			} else {
				fakultas.setId(existing.getId());
			}

			if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, existing);
			session.getTransaction().commit();
			HibernateUtil.closeSession();
		}
	}

	public static void jurusan(JSONObject jsonObject, boolean semuaProdiDimasukkan) throws Exception {

		if (!jsonObject.isNull("id_prodi")) {
			System.out.println("kodePerguruanTinggi = " + kodePerguruanTinggi + ", id_perguruan_tinggi = "
					+ jsonObject.getString("id_perguruan_tinggi").trim() + ", jurusan = " + jsonObject);
			if (semuaProdiDimasukkan || (kodePerguruanTinggi != null
					&& kodePerguruanTinggi.trim().equalsIgnoreCase("" + jsonObject.get("id_perguruan_tinggi")))) {
				Session session = HibernateUtil.currentNativeSession();

				Jurusan jurusan = new Jurusan();

//				jurusan.setGrupJurusan(FeederUtil.getDataByFeeder(session, jsonObject.getString("id_induk_sms").trim(),
//						GrupJurusan.class));
				jurusan.setFeeder("" + jsonObject.get("id_prodi"));
				jurusan.setNama("" + jsonObject.get("nama_program_studi"));
				jurusan.setKodeEpsbed("" + jsonObject.get("kode_program_studi"));
				jurusan.setJenjang((Jenjang) session.createCriteria(Jenjang.class)
						.add(Restrictions.eq("feeder", Long.parseLong(jsonObject.get("id_jenjang_pendidikan") + "")))
						.setMaxResults(1).uniqueResult());

				Jurusan existing = (Jurusan) session.createCriteria(Jurusan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("feeder", jurusan.getFeeder())).setMaxResults(1).uniqueResult();

				if (!semuaProdiDimasukkan) {
					if (existing == null) {
						existing = (Jurusan) session.createCriteria(Jurusan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ne("feeder", jurusan.getFeeder()))
								.add(Restrictions.ilike("kodeEpsbed", jurusan.getKodeEpsbed())).setMaxResults(1)
								.uniqueResult();
					}

					if (existing == null) {
						existing = (Jurusan) session.createCriteria(Jurusan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ilike("kodeEpsbed", jurusan.getKodeEpsbed())).setMaxResults(1)
								.uniqueResult();
					}
				}

				Fakultas tempFakultas = null;
				if (existing == null) {

					tempFakultas = (Fakultas) session.createCriteria(Fakultas.class)
							.add(Restrictions.eq("kode", kodePerguruanTinggi)).setMaxResults(1).uniqueResult();
					if (tempFakultas == null) {
						tempFakultas = new Fakultas();
						tempFakultas.setNama("-");
						tempFakultas.setKode(kodePerguruanTinggi);
						if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
						session.getTransaction().begin();
						session.saveOrUpdate(tempFakultas);
						session.getTransaction().commit();
					}

					jurusan.setFakultas(tempFakultas);

					existing = jurusan;
				} else {
					existing = FeederUtil.copyDataJikaKosong(jurusan, existing, Jurusan.class,
							Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
				}

				existing.setFeeder(jurusan.getFeeder());

				if (tempFakultas != null && existing.getFakultas() == null) {
					existing = jurusan;
					existing.setFakultas(tempFakultas);
				}

				if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
				session.getTransaction().begin();
				session.saveOrUpdate(existing);
				session.getTransaction().commit();
				HibernateUtil.closeSession();

			}
		}
	}

	public void jenjang(JSONObject jsonObject) throws Exception {

		Jenjang jenjang = new Jenjang();
		jenjang.setFeeder(Long.parseLong(jsonObject.get("id_jenj_didik") + ""));
		jenjang.setNama("" + jsonObject.get("nm_jenj_didik"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		Jenjang existing = (Jenjang) session.createCriteria(Jenjang.class)
				.add(Restrictions.eq("feeder", jenjang.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (Jenjang) session.createCriteria(Jenjang.class)
					.add(Restrictions.ilike("nama", jenjang.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null && jenjang.getNama().equalsIgnoreCase("S1")) {
			existing = (Jenjang) session.createCriteria(Jenjang.class).add(Restrictions.ilike("nama", "Strata 1"))
					.setMaxResults(1).uniqueResult();
		}

		if (existing == null && jenjang.getNama().equalsIgnoreCase("S2")) {
			existing = (Jenjang) session.createCriteria(Jenjang.class).add(Restrictions.ilike("nama", "Strata 2"))
					.setMaxResults(1).uniqueResult();
		}

		if (existing == null && jenjang.getNama().equalsIgnoreCase("S3")) {
			existing = (Jenjang) session.createCriteria(Jenjang.class).add(Restrictions.ilike("nama", "Strata 3"))
					.setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = jenjang;
		}
		existing.setNama(jenjang.getNama());
		existing.setFeeder(jenjang.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void statusKepegawaian(JSONObject jsonObject) throws Exception {

		StatusKepegawaian statusKepegawaian = new StatusKepegawaian();
		statusKepegawaian.setFeeder("" + jsonObject.get("id_stat_pegawai"));
		statusKepegawaian.setNama("" + jsonObject.get("nm_stat_pegawai"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		StatusKepegawaian existing = (StatusKepegawaian) session.createCriteria(StatusKepegawaian.class)
				.add(Restrictions.eq("feeder", statusKepegawaian.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (StatusKepegawaian) session.createCriteria(StatusKepegawaian.class)
					.add(Restrictions.ilike("nama", statusKepegawaian.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = statusKepegawaian;
		}
		existing.setNama(statusKepegawaian.getNama());
		existing.setFeeder(statusKepegawaian.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void statusPegawai(JSONObject jsonObject) throws Exception {

		StatusPegawai statusPegawai = new StatusPegawai();
		statusPegawai.setFeeder("" + jsonObject.get("id_stat_aktif"));
		statusPegawai.setNama("" + jsonObject.get("nm_stat_aktif"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		StatusPegawai existing = (StatusPegawai) session.createCriteria(StatusPegawai.class)
				.add(Restrictions.eq("feeder", statusPegawai.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (StatusPegawai) session.createCriteria(StatusPegawai.class)
					.add(Restrictions.ilike("nama", statusPegawai.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = statusPegawai;
		}
		existing.setNama(statusPegawai.getNama());
		existing.setFeeder(statusPegawai.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void penghasilan(JSONObject jsonObject) throws Exception {

		Penghasilan penghasilan = new Penghasilan();
		penghasilan.setFeeder(Long.parseLong(jsonObject.get("id_penghasilan") + ""));
		penghasilan.setNama("" + jsonObject.get("nm_penghasilan"));
		penghasilan.setBatasAtas(jsonObject.getDouble("batas_atas"));
		penghasilan.setBatasBawah(jsonObject.getDouble("batas_bawah"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		Penghasilan existing = (Penghasilan) session.createCriteria(Penghasilan.class)
				.add(Restrictions.eq("feeder", penghasilan.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (Penghasilan) session.createCriteria(Penghasilan.class)
					.add(Restrictions.ilike("nama", penghasilan.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = penghasilan;
		}
		existing.setNama(penghasilan.getNama());
		existing.setFeeder(penghasilan.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void pekerjaan(JSONObject jsonObject) throws Exception {

		Pekerjaan pekerjaan = new Pekerjaan();
		pekerjaan.setFeeder(Long.parseLong(jsonObject.get("id_pekerjaan") + ""));
		pekerjaan.setNama("" + jsonObject.get("nm_pekerjaan"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		Pekerjaan existing = (Pekerjaan) session.createCriteria(Pekerjaan.class)
				.add(Restrictions.eq("feeder", pekerjaan.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (Pekerjaan) session.createCriteria(Pekerjaan.class)
					.add(Restrictions.ilike("nama", pekerjaan.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = pekerjaan;
		}
		existing.setNama(pekerjaan.getNama());
		existing.setFeeder(pekerjaan.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void golongan(JSONObject jsonObject) throws Exception {

		Golongan golongan = new Golongan();
		golongan.setFeeder("" + jsonObject.get("id_pangkat_gol"));
		golongan.setNama("" + jsonObject.get("kode_gol"));
		golongan.setPangkat("" + jsonObject.get("nm_pangkat"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		Golongan existing = (Golongan) session.createCriteria(Golongan.class)
				.add(Restrictions.eq("feeder", golongan.getFeeder())).setMaxResults(1).uniqueResult();

		if (existing == null) {
			existing = (Golongan) session.createCriteria(Golongan.class)
					.add(Restrictions.ilike("nama", golongan.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = (Golongan) session.createCriteria(Golongan.class)
					.add(Restrictions.ilike("pangkat", golongan.getPangkat())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = golongan;
		}
		existing.setNama(golongan.getNama());
		existing.setFeeder(golongan.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void lembagaPengangkat(JSONObject jsonObject) throws Exception {

		LembagaPengangkat lembagaPengangkat = new LembagaPengangkat();
		lembagaPengangkat.setFeeder("" + jsonObject.get("id_lemb_angkat"));
		lembagaPengangkat.setNama("" + jsonObject.get("nm_lemb_angkat"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		LembagaPengangkat existing = (LembagaPengangkat) session.createCriteria(LembagaPengangkat.class)
				.add(Restrictions.eq("feeder", lembagaPengangkat.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (LembagaPengangkat) session.createCriteria(LembagaPengangkat.class)
					.add(Restrictions.ilike("nama", lembagaPengangkat.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = lembagaPengangkat;
		}
		existing.setNama(lembagaPengangkat.getNama());
		existing.setFeeder(lembagaPengangkat.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void statusKeluar(JSONObject jsonObject) throws Exception {

		StatusKeluar statusKeluar = new StatusKeluar();
		statusKeluar.setFeeder("" + jsonObject.get("id_jns_keluar"));
		statusKeluar.setNama("" + jsonObject.get("ket_keluar"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		StatusKeluar existing = (StatusKeluar) session.createCriteria(StatusKeluar.class)
				.add(Restrictions.eq("feeder", statusKeluar.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (StatusKeluar) session.createCriteria(StatusKeluar.class)
					.add(Restrictions.ilike("nama", statusKeluar.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = statusKeluar;
		}
		existing.setNama(statusKeluar.getNama());
		existing.setFeeder(statusKeluar.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void statusAwalMahasiswa(JSONObject jsonObject) throws Exception {

		StatusAwalMahasiswa statusAwalMahasiswa = new StatusAwalMahasiswa();
		statusAwalMahasiswa.setFeeder(Long.parseLong(jsonObject.get("id_jns_daftar") + ""));
		statusAwalMahasiswa.setNama("" + jsonObject.get("nm_jns_daftar"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		StatusAwalMahasiswa existing = (StatusAwalMahasiswa) session.createCriteria(StatusAwalMahasiswa.class)
				.add(Restrictions.eq("feeder", statusAwalMahasiswa.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (StatusAwalMahasiswa) session.createCriteria(StatusAwalMahasiswa.class)
					.add(Restrictions.ilike("nama", statusAwalMahasiswa.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null && statusAwalMahasiswa.getNama().equalsIgnoreCase("Peserta didik baru")) {
			session.createSQLQuery("update status_awal_mahasiswa set feeder = " + statusAwalMahasiswa.getFeeder()
					+ " where nama ilike '%baru%'").executeUpdate();
			HibernateUtil.closeSession();
			return;
		}

		if (existing == null && statusAwalMahasiswa.getNama().equalsIgnoreCase("Pindahan Alih Bentuk")) {
			existing = (StatusAwalMahasiswa) session.createCriteria(StatusAwalMahasiswa.class)
					.add(Restrictions.ilike("nama", "Alih Prodi")).setMaxResults(1).uniqueResult();
		}

		if (existing == null && statusAwalMahasiswa.getNama().equalsIgnoreCase("Lainnya")) {
			session.createSQLQuery("update status_awal_mahasiswa set feeder = " + statusAwalMahasiswa.getFeeder()
					+ " where nama in ('Transfer','PKU','Prog. Khusus')").executeUpdate();
			HibernateUtil.closeSession();
			return;
		}

		if (existing == null) {
			existing = statusAwalMahasiswa;
			existing.setNama(statusAwalMahasiswa.getNama());
		}
		existing.setFeeder(statusAwalMahasiswa.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		session.saveOrUpdate(existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void ikatanKerjaDosen(JSONObject jsonObject) throws Exception {
		IkatanKerjaDosen ikatanKerjaDosen = new IkatanKerjaDosen();
		ikatanKerjaDosen.setFeeder("" + jsonObject.get("id_ikatan_kerja"));
		ikatanKerjaDosen.setNama("" + jsonObject.get("nm_ikatan_kerja"));
		ikatanKerjaDosen.setKeterangan("" + jsonObject.get("ket_ikatan_kerja"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		IkatanKerjaDosen existing = (IkatanKerjaDosen) session.createCriteria(IkatanKerjaDosen.class)
				.add(Restrictions.eq("feeder", ikatanKerjaDosen.getFeeder())).setMaxResults(1).uniqueResult();

		if (existing == null) {
			existing = ikatanKerjaDosen;
		} else {
			ikatanKerjaDosen.setId(existing.getId());
		}

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, ikatanKerjaDosen);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void jabatanFungsionalDosen(JSONObject jsonObject) throws Exception {

		JabatanFungsionalDosen jabatanFungsionalDosen = new JabatanFungsionalDosen();
		jabatanFungsionalDosen.setFeeder("" + jsonObject.get("id_jabfung"));
		jabatanFungsionalDosen.setNama("" + jsonObject.get("nm_jabfung"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		JabatanFungsionalDosen existing = (JabatanFungsionalDosen) session.createCriteria(JabatanFungsionalDosen.class)
				.add(Restrictions.eq("feeder", jabatanFungsionalDosen.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (JabatanFungsionalDosen) session.createCriteria(JabatanFungsionalDosen.class)
					.add(Restrictions.ilike("nama", jabatanFungsionalDosen.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = jabatanFungsionalDosen;
		}
		existing.setNama(jabatanFungsionalDosen.getNama());
		existing.setFeeder(jabatanFungsionalDosen.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void jenisEvaluasi(JSONObject jsonObject) throws Exception {

		JenisEvaluasi jenisEvaluasi = new JenisEvaluasi();
		jenisEvaluasi.setFeeder(Long.parseLong(jsonObject.get("id_jns_eval") + ""));
		jenisEvaluasi.setNama("" + jsonObject.get("nm_jns_eval"));
		jenisEvaluasi.setKeterangan("" + jsonObject.get("ket_jns_eval"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		JenisEvaluasi existing = (JenisEvaluasi) session.createCriteria(JenisEvaluasi.class)
				.add(Restrictions.eq("feeder", jenisEvaluasi.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (JenisEvaluasi) session.createCriteria(JenisEvaluasi.class)
					.add(Restrictions.ilike("nama", jenisEvaluasi.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = jenisEvaluasi;
		}
		existing.setKeterangan(jenisEvaluasi.getKeterangan());
		existing.setNama(jenisEvaluasi.getNama());
		existing.setFeeder(jenisEvaluasi.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void agama(JSONObject jsonObject) throws Exception {

		Agama agama = new Agama();
		agama.setFeeder(Long.parseLong(jsonObject.get("id_agama") + ""));
		agama.setNama("" + jsonObject.get("nm_agama"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		Agama existing = (Agama) session.createCriteria(Agama.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("feeder", agama.getFeeder())).setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (Agama) session.createCriteria(Agama.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.ilike("nama", agama.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null && agama.getNama().equalsIgnoreCase("Katholik")) {
			existing = (Agama) session.createCriteria(Agama.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.ilike("nama", "Katolik")).setMaxResults(1).uniqueResult();
		}

		if (existing == null && agama.getNama().equalsIgnoreCase("Kristen")) {
			existing = (Agama) session.createCriteria(Agama.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.ilike("nama", "Protestan")).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = agama;
		}
		existing.setNama(agama.getNama());
		existing.setFeeder(agama.getFeeder());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void negara(JSONObject jsonObject) throws Exception {

		Negara negara = new Negara();
		negara.setKode("" + jsonObject.get("id_negara"));
		negara.setNamaNegara("" + jsonObject.get("nm_negara"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		Negara existing = (Negara) session.createCriteria(Negara.class).add(Restrictions.eq("kode", negara.getKode()))
				.setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (Negara) session.createCriteria(Negara.class)
					.add(Restrictions.ilike("namaNegara", negara.getNamaNegara())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = negara;
		}
		existing.setNama(negara.getNama());
		existing.setKode(negara.getKode());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public void statusMahasiswa(JSONObject jsonObject) throws Exception {

		StatusMahasiswa statusMahasiswa = new StatusMahasiswa();
		statusMahasiswa.setKodeEpsbed("" + jsonObject.get("id_stat_mhs"));
		statusMahasiswa.setNama("" + jsonObject.get("nm_stat_mhs"));

		Session session = HibernateUtil.currentNativeSession();
		try {
		StatusMahasiswa existing = (StatusMahasiswa) session.createCriteria(StatusMahasiswa.class)
				.addOrder(Order.asc("id")).add(Restrictions.eq("kodeEpsbed", statusMahasiswa.getKodeEpsbed()))
				.setMaxResults(1).uniqueResult();
		if (existing == null) {
			existing = (StatusMahasiswa) session.createCriteria(StatusMahasiswa.class).addOrder(Order.asc("id"))
					.add(Restrictions.ilike("nama", statusMahasiswa.getNama())).setMaxResults(1).uniqueResult();
		}

		if (existing == null) {
			existing = statusMahasiswa;
		}
		existing.setNama(statusMahasiswa.getNama());
		existing.setKodeEpsbed(statusMahasiswa.getKodeEpsbed());

		if (session == null || !session.isOpen()) session = HibernateUtil.currentNativeSession(); // guard "Session is closed!"
		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, existing);
		session.getTransaction().commit();
		HibernateUtil.closeSession();
		} finally {
			HibernateUtil.closeSession();
		}
	}

}
