package ais.action.servlet.api;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GelombangPendaftaran;
import ais.ui.util.WaktuUtil;

/**
 * <b>PmbCalonApi</b> -- API publik "Login Calon Mahasiswa" portal PMB untuk
 * aplikasi mobile native.
 *
 * <p>Padanan native dari {@code login_calon_mahasiswa.zul} + {@code LoginCalonAction}
 * beserta area calon mahasiswa setelah login di portal pmb.zul:</p>
 * <ul>
 *   <li>{@code pmb_login} -- autentikasi No. Pendaftaran/Ujian/Nama Lengkap +
 *       Tanggal Lahir, aturan pencarian sama persis dgn web (aktif, punya
 *       gelombang, EXACT match); menghormati batas
 *       {@code tanggalLoginCalonMahasiswaBerakhir} pada gelombang.</li>
 *   <li>{@code pmb_calon_profil} -- profil lengkap: identitas, PIN, No Ujian,
 *       gelombang + info, paket & prodi pilihan, status seleksi/pembayaran,
 *       dan tautan fungsi lanjutan (lengkapi berkas, ujian online, pembayaran).</li>
 *   <li>{@code pmb_update_biodata} -- lengkapi/perbarui biodata inti calon
 *       (padanan ringan form biodata; berkas & ujian online tetap lewat portal
 *       web via link yang dikembalikan profil).</li>
 * </ul>
 *
 * <p>Autentikasi stateless dgn kredensial yang sama dengan halaman login web
 * (identitas + tanggal lahir). Kompatibilitas: Java 1.7 (tanpa lambda/diamond).</p>
 */
public final class PmbCalonApi {

	private PmbCalonApi() {
	}

	private static SimpleDateFormat formatTanggal() {
		return new SimpleDateFormat("dd-MM-yyyy");
	}

	private static Date parseTanggalLahir(JSONObject json) {
		String str = ApiHelperSupport.optString(json, "tanggal_lahir");
		if (!ApiHelperSupport.hasText(str)) {
			return null;
		}
		try {
			return formatTanggal().parse(str.trim());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Cari calon mahasiswa dgn aturan sama persis {@code LoginCalonAction.onLogin}:
	 * aktif (null dianggap aktif), wajib punya gelombang, tanggal lahir cocok,
	 * identitas EXACT match ke nama / noRegistrasi / noUjian.
	 */
	private static BiodataCalonMahasiswa cariCalon(Session session, String identitas, Date tanggalLahir) {
		Criteria criteria = session.createCriteria(BiodataCalonMahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setMaxResults(1).add(Restrictions.isNotNull("gelombangPendaftaran"))
				.addOrder(Order.desc("id")).add(Restrictions.eq("tanggalLahir", tanggalLahir))
				.add(Restrictions.or(Restrictions.eq("nama", identitas).ignoreCase(),
						Restrictions.or(Restrictions.eq("noRegistrasi", identitas).ignoreCase(),
								Restrictions.eq("noUjian", identitas).ignoreCase())));
		return (BiodataCalonMahasiswa) criteria.uniqueResult();
	}

	/** Action "pmb_login". */
	public static JSONObject login(HttpServletRequest request, JSONObject json) {
		return proses(request, json, true);
	}

	/** Action "pmb_calon_profil". */
	public static JSONObject profil(HttpServletRequest request, JSONObject json) {
		return proses(request, json, false);
	}

	private static JSONObject proses(HttpServletRequest request, JSONObject json, boolean login) {
		JSONObject hasil = new JSONObject();
		Session session = null;
		try {
			String identitas = ApiHelperSupport.optString(json, "identitas");
			if (!ApiHelperSupport.hasText(identitas)) {
				return ApiHelperSupport.status("90", "Nomor Pendaftaran / Nama Lengkap wajib diisi");
			}
			Date tanggalLahir = parseTanggalLahir(json);
			if (tanggalLahir == null) {
				return ApiHelperSupport.status("90",
						"Tanggal lahir wajib diisi dengan format dd-MM-yyyy");
			}

			session = HibernateUtil.getSessionFactory().openSession();
			BiodataCalonMahasiswa biodata = cariCalon(session, identitas.trim(), tanggalLahir);

			if (biodata == null || biodata.getId() == null) {
				return ApiHelperSupport.status("99",
						"Mohon maaf, data Calon Mahasiswa dengan Nomor Pendaftaran / Nama Lengkap \""
								+ identitas.trim()
								+ "\" tidak ditemukan, atau Tanggal Lahir yang Anda masukkan belum sesuai. "
								+ "Periksa kembali penulisan dan tanggal lahir Anda, atau hubungi panitia penerimaan mahasiswa baru.");
			}

			// Batas waktu login gelombang -- logika sama dgn LoginCalonAction.onLogin():
			// lewat batas ditolak, KECUALI masih di hari yang sama dengan batasnya.
			GelombangPendaftaran gelombang = biodata.getGelombangPendaftaran();
			if (gelombang != null && gelombang.getTanggalLoginCalonMahasiswaBerakhir() != null
					&& gelombang.getTanggalLoginCalonMahasiswaBerakhir().before(WaktuUtil.getDate())) {
				SimpleDateFormat dfHari = new SimpleDateFormat("yyyyMMdd");
				if (!dfHari.format(gelombang.getTanggalLoginCalonMahasiswaBerakhir())
						.equals(dfHari.format(WaktuUtil.getDate()))) {
					return ApiHelperSupport.status("92",
							"Tanggal batas diperbolehkan login telah terlewat. "
									+ "Hubungi panitia penerimaan mahasiswa baru untuk informasi lebih lanjut.");
				}
			}

			JSONObject data = buatProfil(request, biodata);
			hasil.put("data", data);
			ApiHelperSupport.putSuccess(hasil,
					login ? "Login berhasil. Selamat datang, " + biodata.getNama() + "!"
							: "Profil calon mahasiswa berhasil diambil");
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			ApiHelperSupport.putError(hasil, err);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/** Profil lengkap calon mahasiswa -- ringkasan + biodata + info gelombang + link fungsi lanjutan. */
	private static JSONObject buatProfil(HttpServletRequest request, BiodataCalonMahasiswa biodata) throws Exception {
		JSONObject data = PmbApi.buatRingkasan(request, biodata);
		SimpleDateFormat df = formatTanggal();

		JSONObject bio = new JSONObject();
		ApiHelperSupport.put(bio, "jenis_kelamin", biodata.getJenisKelamin());
		ApiHelperSupport.put(bio, "tempat_lahir", biodata.getTempatLahir());
		ApiHelperSupport.put(bio, "tanggal_lahir",
				biodata.getTanggalLahir() == null ? null : df.format(biodata.getTanggalLahir()));
		ApiHelperSupport.put(bio, "asal_sma", biodata.getAsalSma());
		ApiHelperSupport.put(bio, "alamat", biodata.getAlamat());
		ApiHelperSupport.put(bio, "hp", biodata.getHp());
		ApiHelperSupport.put(bio, "email", biodata.getEmail());
		ApiHelperSupport.put(bio, "nama_ayah", biodata.getNamaAyah());
		ApiHelperSupport.put(bio, "nama_ibu", biodata.getNamaIbu());
		data.put("biodata", bio);

		ApiHelperSupport.put(data, "pin", biodata.getPin());
		ApiHelperSupport.put(data, "status_pembayaran", biodata.getStatusPembayaran());

		GelombangPendaftaran gelombang = biodata.getGelombangPendaftaran();
		if (gelombang != null) {
			JSONObject infoGelombang = new JSONObject();
			ApiHelperSupport.put(infoGelombang, "info", gelombang.getInfo());
			ApiHelperSupport.put(infoGelombang, "info_setelah_ujian_online",
					gelombang.getInfoSetelahUjianOnline());
			ApiHelperSupport.put(infoGelombang, "terdapat_ujian_online",
					Boolean.TRUE.equals(gelombang.getTerdapatUjianOnline()));
			data.put("info_gelombang", infoGelombang);
		}

		// Fungsi lanjutan lewat portal web -- link parameter sama dgn shortcut di
		// PMBAction.doAfterCompose (login/ujian/bayar).
		ApiHelperSupport.put(data, "link_portal", ApiHelperSupport.absoluteUrl(request, "/pmb?login=true"));
		ApiHelperSupport.put(data, "link_ujian", ApiHelperSupport.absoluteUrl(request, "/pmb?ujian=true"));
		ApiHelperSupport.put(data, "link_bayar", ApiHelperSupport.absoluteUrl(request, "/pmb?bayar=true"));
		return data;
	}

	/**
	 * Action "pmb_update_biodata" -- lengkapi/perbarui biodata inti calon
	 * mahasiswa. Kredensial sama dgn login; hanya field yang DIKIRIM yang
	 * diperbarui (partial update).
	 */
	public static JSONObject updateBiodata(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		Session session = null;
		try {
			String identitas = ApiHelperSupport.optString(json, "identitas");
			Date tanggalLahir = parseTanggalLahir(json);
			if (!ApiHelperSupport.hasText(identitas) || tanggalLahir == null) {
				return ApiHelperSupport.status("90",
						"Kredensial login (identitas + tanggal lahir dd-MM-yyyy) wajib disertakan");
			}

			session = HibernateUtil.getSessionFactory().openSession();
			BiodataCalonMahasiswa biodata = cariCalon(session, identitas.trim(), tanggalLahir);
			if (biodata == null || biodata.getId() == null) {
				return ApiHelperSupport.status("99",
						"Data calon mahasiswa tidak ditemukan. Silakan login ulang.");
			}

			if (json.has("jenis_kelamin")) {
				biodata.setJenisKelamin(ApiHelperSupport.optString(json, "jenis_kelamin"));
			}
			if (json.has("tempat_lahir")) {
				biodata.setTempatLahir(ApiHelperSupport.optString(json, "tempat_lahir"));
			}
			if (json.has("asal_sma")) {
				biodata.setAsalSma(ApiHelperSupport.optString(json, "asal_sma"));
			}
			if (json.has("alamat")) {
				biodata.setAlamat(ApiHelperSupport.optString(json, "alamat"));
			}
			if (json.has("hp")) {
				biodata.setHp(ApiHelperSupport.optString(json, "hp"));
			}
			if (json.has("email")) {
				biodata.setEmail(ApiHelperSupport.optString(json, "email"));
			}
			if (json.has("nama_ayah")) {
				biodata.setNamaAyah(ApiHelperSupport.optString(json, "nama_ayah"));
			}
			if (json.has("nama_ibu")) {
				biodata.setNamaIbu(ApiHelperSupport.optString(json, "nama_ibu"));
			}

			session.beginTransaction();
			session.update(biodata);
			session.getTransaction().commit();

			JSONObject data = buatProfil(request, biodata);
			hasil.put("data", data);
			ApiHelperSupport.putSuccess(hasil, "Biodata berhasil diperbarui");
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			ApiHelperSupport.putError(hasil, err);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}
}
