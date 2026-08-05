package ais.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.httpclient.methods.GetMethod;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Label;
import org.zkoss.zul.Timer;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Agama;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.JenisSekolahMahasiswaBaru;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jurusan;
import ais.database.model.JurusanSekolahMahasiswaBaru;
import ais.database.model.Konfigurasi;
import ais.database.model.Kota;
import ais.database.model.PekerjaanOrangTua;
import ais.database.model.Propinsi;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.ui.util.MyMessageboxConfig;

public class PmbArkatama {

	public static String token = "";
	public static String username = "";
	public static String password = "";

	private static void doLogin(String username, String password, String strURL) {
		PmbArkatama.username = username;
		PmbArkatama.password = password;
		try {

			String hasil = "";
			try {

				JSONObject postData = new JSONObject();
				postData.put("username", username);
				postData.put("password", password);

				String[] command = { "curl", "-k", "-H", "Accept: application/json", "-X", "POST", strURL, "--data",
						postData.toString() };

				ProcessBuilder process = new ProcessBuilder(command);
				Process p;
				p = process.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
					builder.append(System.getProperty("line.separator"));
				}
				hasil = builder.toString();

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:69");
			}

			System.out.println(hasil);

			JSONObject jSONObject = new JSONObject(hasil);
			// System.out.println("jSONObject = " + jSONObject);

			JSONObject data = jSONObject.getJSONObject("data");
			token = data.getString("token");
			System.out.println("token = " + token);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:81");
		}
	}

	public static void doPost(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> hasils) {
		if (token == null || token.trim().isEmpty()) {
			login();
		}
		try {
			String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
					+ "/api/Registrasi/register");

			JSONObject postData = new JSONObject();
			postData.put("id_jalur_masuk", biodataCalonMahasiswa.getJenisSeleksi().getKode());
			postData.put("nama_lengkap", biodataCalonMahasiswa.getNama());
			postData.put("nik", biodataCalonMahasiswa.getNoIdentitas());
			postData.put("nisn", biodataCalonMahasiswa.getNisn());

			postData.put("jenis_kelamin", biodataCalonMahasiswa.getJenisKelamin());
			postData.put("email", biodataCalonMahasiswa.getEmail().split(",")[0]);

			postData.put("kode_provinsi", biodataCalonMahasiswa.getKecamatanCalon() == null
					|| biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk() == null
					|| biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk().getWilayahInduk() == null ? ""
							: biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk().getWilayahInduk().getKode());
			postData.put("kode_kabupaten",
					biodataCalonMahasiswa.getKecamatanCalon() == null
							|| biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk() == null ? ""
									: biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk().getKode());
			postData.put("kode_kecamatan", biodataCalonMahasiswa.getKecamatanCalon() == null ? ""
					: biodataCalonMahasiswa.getKecamatanCalon().getKode());
			postData.put("no_pendaftaran", biodataCalonMahasiswa.getNoRegistrasi());

			postData.put("asal_instansi", biodataCalonMahasiswa.getNamaSekolahAsal() == null ? ""
					: biodataCalonMahasiswa.getNamaSekolahAsal().getNama());

			if (!biodataCalonMahasiswa.getJabatanDiInstansiAsal().isEmpty()) {
				postData.put("jabatan", biodataCalonMahasiswa.getJabatanDiInstansiAsal());
			}

			String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H", "Authorization: " + token, "-X",
					"POST", strURL, "--data", postData.toString() };

			ProcessBuilder process = new ProcessBuilder(command);
			Process p;
			p = process.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String hasil = builder.toString();

			System.out.println(hasil);

			JSONObject jSONObject = new JSONObject(hasil);
			// System.out.println("jSONObject = " + jSONObject);

			if (!jSONObject.isNull("status") && jSONObject.get("status").toString().trim().equals("200")) {
				biodataCalonMahasiswa
						.setPinPassword(jSONObject.getJSONObject("data").getString("id_registrasi").trim());
				hasils.add("Sukses pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
						+ biodataCalonMahasiswa.getNama() + " " + biodataCalonMahasiswa.getPinPassword());
			} else {
				hasils.add("Error pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
						+ biodataCalonMahasiswa.getNama() + ", error : " + jSONObject.getString("error"));
			}
		} catch (Exception e) {
			hasils.add("Error pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
					+ biodataCalonMahasiswa.getNama());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:153");
		}
	}

	public static void doPostLolos(BiodataCalonMahasiswa biodataCalonMahasiswa, List<String> hasils) {
		if (token == null || token.trim().isEmpty()) {
			login();
		}
		try {
			String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
					+ "/api/Registrasi/lolosBerkas");

			JSONObject postData = new JSONObject();
			postData.put("id_registrasi", biodataCalonMahasiswa.getPinPassword());
			postData.put("id_jalur_masuk", biodataCalonMahasiswa.getJenisSeleksi().getKode());
			postData.put("no_peserta_tes", biodataCalonMahasiswa.getNoRegistrasi());

			postData.put("nama_lengkap", biodataCalonMahasiswa.getNama());
			postData.put("nik", biodataCalonMahasiswa.getNoIdentitas());
			postData.put("nisn", biodataCalonMahasiswa.getNisn());

			postData.put("jenis_kelamin",
					biodataCalonMahasiswa.getJenisKelamin() == null ? "" : biodataCalonMahasiswa.getJenisKelamin());
			postData.put("no_hp", biodataCalonMahasiswa.getHp());

			postData.put("id_agama",
					biodataCalonMahasiswa.getAgama() == null ? "" : biodataCalonMahasiswa.getAgama().getKode());

			postData.put("status_perkawinan", biodataCalonMahasiswa.getStatusNikah().equals(0) ? "Belum Kawin"
					: biodataCalonMahasiswa.getStatusNikah().equals(1) ? "Kawin" : "Pernah Kawin");

			postData.put("email", biodataCalonMahasiswa.getEmail().split(",")[0]);

			postData.put("kode_provinsi",
					biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk().getWilayahInduk().getKode());
			postData.put("kode_kabupaten", biodataCalonMahasiswa.getKecamatanCalon().getWilayahInduk().getKode());
			postData.put("kode_kecamatan", biodataCalonMahasiswa.getKecamatanCalon().getKode());

			String url_photo;
			if (biodataCalonMahasiswa.getMahasiswa() != null) {
				url_photo = CommonMedia.getUrlFotoPengguna(new Tbmuser(biodataCalonMahasiswa.getMahasiswa()));
			} else {
				url_photo = CommonMedia.getUrlFotoPengguna(new Tbmuser(biodataCalonMahasiswa));
			}
			postData.put("url_photo", url_photo);
			postData.put("id_prodi_pilihan1",
					biodataCalonMahasiswa.getProdi1() == null ? "" : biodataCalonMahasiswa.getProdi1().getKode());
			postData.put("id_prodi_pilihan2",
					biodataCalonMahasiswa.getProdi2() == null ? "" : biodataCalonMahasiswa.getProdi2().getKode());
			postData.put("id_jenis_sekolah", biodataCalonMahasiswa.getJenisSekolah() == null ? ""
					: biodataCalonMahasiswa.getJenisSekolah().getKode());
			postData.put("asal_sekolah", biodataCalonMahasiswa.getNamaSekolahAsal() == null ? ""
					: biodataCalonMahasiswa.getNamaSekolahAsal().getNama());

			postData.put("id_jurusan", biodataCalonMahasiswa.getJurusanSekolah() == null ? ""
					: biodataCalonMahasiswa.getJurusanSekolah().getKode());

			postData.put("pekerjaan_orang_tua", biodataCalonMahasiswa.getPekerjaanAyah() == null ? ""
					: biodataCalonMahasiswa.getPekerjaanAyah().getKode());

			if (!biodataCalonMahasiswa.getInstansiAsal().isEmpty()) {
				postData.put("instansi_asal", biodataCalonMahasiswa.getInstansiAsal());
			}
			if (biodataCalonMahasiswa.getKotaInstansi() != null
					&& biodataCalonMahasiswa.getKotaInstansi().getWilayahInduk() != null) {
				postData.put("kode_provinsi_instansi",
						biodataCalonMahasiswa.getKotaInstansi().getWilayahInduk().getKode());
			}
			if (biodataCalonMahasiswa.getKotaInstansi() != null) {
				postData.put("kode_kabupaten_instansi", biodataCalonMahasiswa.getKotaInstansi().getKode());
			}
			if (!biodataCalonMahasiswa.getJabatanDiInstansiAsal().isEmpty()) {
				postData.put("jabatan", biodataCalonMahasiswa.getJabatanDiInstansiAsal());
			}

			System.out.println("postData = " + postData);

			String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H", "Authorization: " + token, "-X",
					"POST", strURL, "--data", postData.toString() };

			ProcessBuilder process = new ProcessBuilder(command);
			Process p;
			p = process.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String hasil = builder.toString();

			System.out.println(hasil);

			JSONObject jSONObject = new JSONObject(hasil);
			// System.out.println("jSONObject = " + jSONObject);

			if (jSONObject.getString("status").trim().equals("200")) {
				biodataCalonMahasiswa.setProgramNIM(jSONObject.getString("id_reg_lolos_berkas").trim());
				hasils.add("Sukses pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
						+ biodataCalonMahasiswa.getNama() + " " + biodataCalonMahasiswa.getPinPassword());
			} else {
				hasils.add("Hasil pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
						+ biodataCalonMahasiswa.getNama() + ", error : " + jSONObject.getString("error"));
			}
		} catch (Exception e) {
			hasils.add("Hasil pengiriman data " + biodataCalonMahasiswa.getNoRegistrasi() + " "
					+ biodataCalonMahasiswa.getNama());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:261");
		}
	}

	public static JSONObject prosesPost(String strURL, String data) {
		try {
			GetMethod post = new GetMethod(strURL);
			post.setRequestHeader("Authorization", token);
			post.setRequestHeader("Content-type", "application/json");

			String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H", "Authorization: " + token, "-X",
					"POST", strURL, "--data", data };

			ProcessBuilder process = new ProcessBuilder(command);
			Process p;
			p = process.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String hasil = builder.toString();

			System.out.println(hasil);

			JSONObject jSONObject = new JSONObject(hasil);

			return jSONObject;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:292");
		}
		return null;
	}

	private static JSONObject prosesGet(String strURL) {
		try {
			GetMethod post = new GetMethod(strURL);
			post.setRequestHeader("Authorization", token);
			post.setRequestHeader("Content-type", "application/json");

			String[] command = { "curl", "-k", "-H", "Accept: application/json", "-H", "Authorization: " + token, "-X",
					"GET", strURL };

			ProcessBuilder process = new ProcessBuilder(command);
			Process p;
			p = process.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String hasil = builder.toString();

			System.out.println(hasil);

			JSONObject jSONObject = new JSONObject(hasil);

			return jSONObject;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:324");
		}
		return null;
	}

	public static void login() {
		String username = Common.getKonfigurasi("pmb_arkatama_username", "445002").getNilai();
		String password = Common.getKonfigurasi("pmb_arkatama_password", "12345").getNilai();

		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Auth");

		doLogin(username, password, strURL);
	}

	public static void synPropinsi(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/Provinsi");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("kode_provinsi").trim();
					String nama = data.getString("nama_provinsi").trim();

					System.out.println("kode : " + kode + ", nama : " + nama);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);

					Propinsi propinsi = (Propinsi) session.createCriteria(Propinsi.class)
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (propinsi == null) {
						propinsi = (Propinsi) session.createCriteria(Propinsi.class)
								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (propinsi == null) {
						propinsi = (Propinsi) session.createCriteria(Propinsi.class)
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (propinsi == null) {
						propinsi = new Propinsi();
						propinsi.setNegara(ConstantValues.INDONESIA);
					}
					propinsi.setKode(kode);
					propinsi.setNama(nama);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, propinsi);
					session.getTransaction().commit();

					Wilayah wilayah = (Wilayah) session.createCriteria(Wilayah.class).add(Restrictions.eq("level", "1"))
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (wilayah == null) {
						wilayah = (Wilayah) session.createCriteria(Wilayah.class).add(Restrictions.eq("level", "1"))
								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (wilayah == null) {
						wilayah = (Wilayah) session.createCriteria(Wilayah.class).add(Restrictions.eq("level", "1"))
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (wilayah == null) {
						wilayah = new Wilayah();
						wilayah.setInduk("000000");
						wilayah.setLevel("1");
						wilayah.setFeeder(kode);
					}
					wilayah.setKeterangan(PmbArkatama.class.getSimpleName());
					wilayah.setKode(kode);
					wilayah.setNama(nama);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, wilayah);
					session.getTransaction().commit();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:402");
			}
			HibernateUtil.closeSession();
		}
	}

	public static void synKotakab(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/KabupatenKota");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("kode_kabupaten_kota").trim();
					String nama = data.getString("nama_kabupaten_kota").trim();
					String kode_provinsi = data.getString("kode_provinsi").trim();

					Propinsi propinsi = (Propinsi) session.createCriteria(Propinsi.class)
							.add(Restrictions.ilike("kode", kode_provinsi, MatchMode.EXACT)).setMaxResults(1)
							.uniqueResult();

					System.out.println("kode : " + kode + ", nama : " + nama + ", propinsi : " + propinsi);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama + ", propinsi : " + propinsi);

					if (propinsi != null) {

						Kota kota = (Kota) session.createCriteria(Kota.class).add(Restrictions.eq("propinsi", propinsi))
								.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
						if (kota == null) {
							kota = (Kota) session.createCriteria(Kota.class).add(Restrictions.eq("propinsi", propinsi))
									.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1)
									.uniqueResult();
						}

						if (kota == null) {
							kota = new Kota();
							kota.setPropinsi(propinsi);
						}
						kota.setKode(kode);
						kota.setNama(nama);
						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, kota);
						session.getTransaction().commit();

						Wilayah wilayahInduk = (Wilayah) session.createCriteria(Wilayah.class)
								.add(Restrictions.eq("level", "1"))
								.add(Restrictions.ilike("kode", kode_provinsi, MatchMode.EXACT)).setMaxResults(1)
								.uniqueResult();

						System.out.println("wilayahInduk : " + wilayahInduk);

						if (wilayahInduk != null) {
							Wilayah wilayah = (Wilayah) session.createCriteria(Wilayah.class)
									.add(Restrictions.eq("level", "2"))
									.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
									.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1)
									.uniqueResult();
							if (wilayah == null) {
								wilayah = (Wilayah) session.createCriteria(Wilayah.class)
										.add(Restrictions.eq("level", "2"))
										.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
										.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1)
										.uniqueResult();
							}
							if (wilayah == null) {
								wilayah = (Wilayah) session.createCriteria(Wilayah.class)
										.add(Restrictions.eq("level", "2"))
										.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
										.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1)
										.uniqueResult();
							}

							if (wilayah == null) {
								wilayah = (Wilayah) session.createCriteria(Wilayah.class)
										.add(Restrictions.eq("level", "2"))
										.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
										.add(Restrictions.ilike("nama",
												nama.toLowerCase().replaceAll("kabupaten", "kab."), MatchMode.END))
										.setMaxResults(1).uniqueResult();
							}

							if (wilayah == null) {
								wilayah = new Wilayah();
								wilayah.setInduk(wilayahInduk.getFeeder());
								wilayah.setFeeder(kode);
							}
							wilayah.setKeterangan(PmbArkatama.class.getSimpleName());
							wilayah.setLevel("2");
							wilayah.setWilayahInduk(wilayahInduk);
							wilayah.setKode(kode);
							wilayah.setNama(nama);
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, wilayah);
							session.getTransaction().commit();
						}
					}

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:505");
			}
			HibernateUtil.closeSession();
		}
	}

	public static void synKecamatan(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/Kecamatan");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("kode_kecamatan").trim();
					String nama = data.getString("nama_kecamatan").trim();
					String kode_kebupaten_kota = data.getString("kode_kebupaten_kota").trim();

					Wilayah wilayahInduk = (Wilayah) session.createCriteria(Wilayah.class)
							.add(Restrictions.eq("level", "2"))
							.add(Restrictions.ilike("kode", kode_kebupaten_kota, MatchMode.EXACT)).setMaxResults(1)
							.uniqueResult();

					System.out.println("kode : " + kode + ", nama : " + nama + ", wilayahInduk : " + wilayahInduk);

					label.setValue(
							"Proses data kode : " + kode + ", nama : " + nama + ", wilayahInduk : " + wilayahInduk);

					if (wilayahInduk != null) {
						Wilayah wilayah = (Wilayah) session.createCriteria(Wilayah.class)
								.add(Restrictions.eq("level", "3"))
								.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
								.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
						if (wilayah == null) {
							wilayah = (Wilayah) session.createCriteria(Wilayah.class).add(Restrictions.eq("level", "3"))
									.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
									.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1)
									.uniqueResult();
						}
						if (wilayah == null) {
							wilayah = (Wilayah) session.createCriteria(Wilayah.class).add(Restrictions.eq("level", "3"))
									.add(Restrictions.eq("induk", wilayahInduk.getFeeder()))
									.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1)
									.uniqueResult();
						}

						if (wilayah == null) {
							wilayah = new Wilayah();
							wilayah.setInduk(wilayahInduk.getFeeder());
							wilayah.setFeeder(kode);
						}
						wilayah.setKeterangan(PmbArkatama.class.getSimpleName());
						wilayah.setLevel("3");
						wilayah.setWilayahInduk(wilayahInduk);
						wilayah.setKode(kode);
						wilayah.setNama(nama);
						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, wilayah);
						session.getTransaction().commit();
					}

				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:570");
			}
			HibernateUtil.closeSession();
		}
	}

	public static void synJalurMasuk(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/JalurMasuk");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_jalur").trim();
					String nama = data.getString("nama_jalur").trim();
					String kode_jalur = data.getString("kode_jalur").trim();

					System.out.println("kode : " + kode + ", nama : " + nama + " kode_jalur : " + kode_jalur);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);
					JenisSeleksi jenisSeleksi = (JenisSeleksi) session.createCriteria(JenisSeleksi.class)
//							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
//					if (jenisSeleksi == null) {
//						jenisSeleksi = (JenisSeleksi) session.createCriteria(JenisSeleksi.class)
//								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
//					}
//					if (jenisSeleksi == null) {
//						jenisSeleksi = (JenisSeleksi) session.createCriteria(JenisSeleksi.class)
//								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
//								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
//					}

					if (jenisSeleksi == null) {
						jenisSeleksi = new JenisSeleksi();
						jenisSeleksi.setDeskripsi(nama);
					}
					jenisSeleksi.setKodeLain(kode_jalur);
					jenisSeleksi.setKode(kode);
					jenisSeleksi.setNama(nama);
					jenisSeleksi.setKeterangan(PmbArkatama.class.getSimpleName());
					jenisSeleksi.setAktif(true);
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, jenisSeleksi);
					session.getTransaction().commit();

				}

				session.createSQLQuery("update jenis_seleksi set aktif=false where keterangan !='"
						+ PmbArkatama.class.getSimpleName() + "';").executeUpdate();

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:626");
			}
			HibernateUtil.closeSession();
		}
	}

	public static void synAgama(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/agama");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_agama").trim();
					String nama = data.getString("nama_agama").trim();

					System.out.println("kode : " + kode + ", nama : " + nama);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);
					Agama agama = (Agama) session.createCriteria(Agama.class)
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (agama == null) {
						agama = (Agama) session.createCriteria(Agama.class)

								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (agama == null) {
						agama = (Agama) session.createCriteria(Agama.class)
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (agama == null) {
						agama = new Agama();
					}
					agama.setKode(kode);
					agama.setNama(nama);
					agama.setKeterangan(PmbArkatama.class.getSimpleName());
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, agama);
					session.getTransaction().commit();

				}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:673");
			}
			HibernateUtil.closeSession();
		}
	}

	public static void synProdi(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/prodi");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_prodi").trim();
					String nama = data.getString("nama_prodi").trim();
					String kode_prodi = data.getString("kode_prodi").trim();

					System.out.println("kode : " + kode + ", nama : " + nama);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);
					Jurusan jurusan = (Jurusan) session.createCriteria(Jurusan.class)
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (jurusan == null) {
						jurusan = (Jurusan) session.createCriteria(Jurusan.class)

								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (jurusan == null) {
						jurusan = (Jurusan) session.createCriteria(Jurusan.class)
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (jurusan == null) {
						jurusan = new Jurusan();
					}
					jurusan.setKodeLain(kode_prodi);
					jurusan.setKode(kode);
					jurusan.setNama(nama);
					jurusan.setKeterangan(PmbArkatama.class.getSimpleName());
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, jurusan);
					session.getTransaction().commit();

				}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:722");
			}
			HibernateUtil.closeSession();
		}
	}

	public static void synJenisSekolah(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/jenisSekolah");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_jenis_sekolah").trim();
					String nama = data.getString("nama_jenis_sekolah").trim();

					System.out.println("kode : " + kode + ", nama : " + nama);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);
					JenisSekolahMahasiswaBaru jenisSekolahMahasiswaBaru = (JenisSekolahMahasiswaBaru) session
							.createCriteria(JenisSekolahMahasiswaBaru.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (jenisSekolahMahasiswaBaru == null) {
						jenisSekolahMahasiswaBaru = (JenisSekolahMahasiswaBaru) session
								.createCriteria(JenisSekolahMahasiswaBaru.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (jenisSekolahMahasiswaBaru == null) {
						jenisSekolahMahasiswaBaru = (JenisSekolahMahasiswaBaru) session
								.createCriteria(JenisSekolahMahasiswaBaru.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (jenisSekolahMahasiswaBaru == null) {
						jenisSekolahMahasiswaBaru = new JenisSekolahMahasiswaBaru();
					}
					jenisSekolahMahasiswaBaru.setKode(kode);
					jenisSekolahMahasiswaBaru.setNama(nama);
					jenisSekolahMahasiswaBaru.setKeterangan(PmbArkatama.class.getSimpleName());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, jenisSekolahMahasiswaBaru);
					session.getTransaction().commit();

				}

				session.createSQLQuery("update jenis_sekolah_mahasiswa_baru set aktif=false where keterangan !='"
						+ PmbArkatama.class.getSimpleName() + "';").executeUpdate();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:774");
			}
			HibernateUtil.closeSession();
		}
	}

	public static void synJurusanSekolah(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/jurusanSekolah");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {
				Session session = HibernateUtil.currentNativeSession();
				@SuppressWarnings("unchecked")
				List<JenisSekolahMahasiswaBaru> jenisSekolahMahasiswaBarus = session
						.createCriteria(JenisSekolahMahasiswaBaru.class)
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).list();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_jurusan_sekolah").trim();
					String nama = data.getString("nama_jurusan_sekolah").trim();

					for (JenisSekolahMahasiswaBaru jenisSekolahMahasiswaBaru : jenisSekolahMahasiswaBarus) {

						System.out.println("kode : " + kode + ", nama : " + nama + ", jenisSekolahMahasiswaBaru "
								+ jenisSekolahMahasiswaBaru);

						label.setValue("Proses data kode : " + kode + ", nama : " + nama
								+ ", jenisSekolahMahasiswaBaru " + jenisSekolahMahasiswaBaru);

						JurusanSekolahMahasiswaBaru jurusanSekolahMahasiswaBaru = (JurusanSekolahMahasiswaBaru) session
								.createCriteria(JurusanSekolahMahasiswaBaru.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("jenisSekolahMahasiswaBaru", jenisSekolahMahasiswaBaru))
								.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
						if (jurusanSekolahMahasiswaBaru == null) {
							jurusanSekolahMahasiswaBaru = (JurusanSekolahMahasiswaBaru) session
									.createCriteria(JurusanSekolahMahasiswaBaru.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("jenisSekolahMahasiswaBaru", jenisSekolahMahasiswaBaru))
									.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1)
									.uniqueResult();
						}
						if (jurusanSekolahMahasiswaBaru == null) {
							jurusanSekolahMahasiswaBaru = (JurusanSekolahMahasiswaBaru) session
									.createCriteria(JurusanSekolahMahasiswaBaru.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("jenisSekolahMahasiswaBaru", jenisSekolahMahasiswaBaru))
									.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1)
									.uniqueResult();
						}

						if (jurusanSekolahMahasiswaBaru == null) {
							jurusanSekolahMahasiswaBaru = new JurusanSekolahMahasiswaBaru();
						}
						jurusanSekolahMahasiswaBaru.setJenisSekolahMahasiswaBaru(jenisSekolahMahasiswaBaru);
						jurusanSekolahMahasiswaBaru.setKode(kode);
						jurusanSekolahMahasiswaBaru.setNama(nama);
						jurusanSekolahMahasiswaBaru.setKeterangan(PmbArkatama.class.getSimpleName());
						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, jurusanSekolahMahasiswaBaru);
						session.getTransaction().commit();
					}
				}

				session.createSQLQuery("update jurusan_sekolah_mahasiswa_baru set aktif=false where keterangan !='"
						+ PmbArkatama.class.getSimpleName() + "';").executeUpdate();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:842");
			}

			HibernateUtil.closeSession();
		}
	}

	public static void synPekerjaanOrangTua(Label label) {
		String strURL = (Common.getKonfigurasi("pmb_arkatama_host_url", "https://pmb.pusdiktan.id").getNilai()
				+ "/api/Ref/pekerjaan");
		JSONObject jSONObject = prosesGet(strURL);
		if (jSONObject != null && !jSONObject.isNull("data")) {
			try {

				Session session = HibernateUtil.currentNativeSession();
				JSONArray array = jSONObject.getJSONArray("data");
				for (int i = 0; i < array.length(); i++) {
					JSONObject data = array.getJSONObject(i);
					String kode = data.getString("id_pekerjaan").trim();
					String nama = data.getString("nama_pekerjaan").trim();

					System.out.println("kode : " + kode + ", nama : " + nama);

					label.setValue("Proses data kode : " + kode + ", nama : " + nama);
					PekerjaanOrangTua pekerjaanOrangTua = (PekerjaanOrangTua) session
							.createCriteria(PekerjaanOrangTua.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("kode", kode, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					if (pekerjaanOrangTua == null) {
						pekerjaanOrangTua = (PekerjaanOrangTua) session.createCriteria(PekerjaanOrangTua.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ilike("nama", nama, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
					}
					if (pekerjaanOrangTua == null) {
						pekerjaanOrangTua = (PekerjaanOrangTua) session.createCriteria(PekerjaanOrangTua.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.ilike("nama", nama, MatchMode.END)).setMaxResults(1).uniqueResult();
					}

					if (pekerjaanOrangTua == null) {
						pekerjaanOrangTua = new PekerjaanOrangTua();
					}
					pekerjaanOrangTua.setKode(kode);
					pekerjaanOrangTua.setNama(nama);
					pekerjaanOrangTua.setKeterangan(PmbArkatama.class.getSimpleName());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, pekerjaanOrangTua);
					session.getTransaction().commit();

				}

				session.createSQLQuery("update pekerjaan_orang_tua set aktif=false where keterangan !='"
						+ PmbArkatama.class.getSimpleName() + "';").executeUpdate();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:896");
			}
			HibernateUtil.closeSession();
		}
	}

	public static void synRef() {

		if (!Common.bolehKonfigurasi("integrasi_pmb_arkatama", Konfigurasi.TIDAK_AKTIF)) {
			try {
				MyMessageboxConfig.show("Singkronisasi PMB Arkatama tidak diaktifkan", "Pemberitahuan",
						MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PmbArkatama.java:910");
			}
			return;
		}

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi PMB Arkatama"));

		new Thread(new Runnable() {

			@Override
			public void run() {
				login();

				if (token != null && !token.trim().isEmpty()) {
					synAgama(label);
					synProdi(label);
					synJalurMasuk(label);
					synJenisSekolah(label);
					synJurusanSekolah(label);
					synPekerjaanOrangTua(label);
					synPropinsi(label);
					synKotakab(label);
					synKecamatan(label);
					label.setValue("");
				} else {
					label.setValue("Error");
				}

			}
		}).start();

		final Timer timer = new Timer(500);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					Clients.clearBusy();
					MyMessageboxConfig.show("Singkronisasi PMB Arkatama telah selesai", "Pemberitahuan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					timer.detach();
				} else if (label.getValue().equalsIgnoreCase("Error")) {
					Clients.clearBusy();
					PesanFormalHelper.tampilkanGagal("sinkronisasi data referensi PMB Arkatama",
							"Sistem gagal memperoleh token autentikasi (login) ke server PMB Arkatama, "
									+ "kemungkinan disebabkan kredensial/konfigurasi integrasi PMB Arkatama yang "
									+ "belum benar atau server PMB Arkatama sedang tidak dapat dihubungi.",
							new String[] {
									"Periksa kembali username/password/URL integrasi PMB Arkatama pada menu Konfigurasi.",
									"Pastikan server aplikasi memiliki akses jaringan ke server PMB Arkatama.",
									"Ulangi proses sinkronisasi ini beberapa saat lagi." });
					timer.detach();
				}

			}
		});
		timer.start();
	}

}
