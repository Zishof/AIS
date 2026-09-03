package ais.action.servlet.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.sekolah.helper.DetailTagihanCalonSiswaHelper;
import ais.action.master.sekolah.helper.DetailTagihanSiswaHelper;
import ais.action.master.sekolah.helper.TagihanUtil;
import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.WaktuUtil;

/**
 * <b>PsbCalonApi</b> -- API publik "Login Calon Siswa" portal PSB/PPDB untuk
 * aplikasi mobile native.
 *
 * <p>Padanan native dari {@code login_calon_siswa.zul} + {@code LoginCalonSiswaAction}
 * beserta area calon siswa yang tampil setelah login di portal psb.zul
 * ({@code TampilanPengumumanAkademisAction.tampilGelombang} saat calon login):</p>
 * <ul>
 *   <li>{@code psb_login} -- autentikasi No. Pendaftaran/Ujian/Nama + Tanggal Lahir
 *       (+ PIN bila diberikan), aturan pencarian sama persis dgn web; menandai
 *       telahLogin/waktuLogin; menghormati konfigurasi
 *       {@code calon_siswa_harus_melakukan_pembayaran_sebelum_bisa_login_baru}.</li>
 *   <li>{@code psb_calon_profil} -- profil lengkap: identitas, status seleksi
 *       (5 kondisi), progress tracker (Daftar-Bayar-Verifikasi-Hasil), jadwal
 *       pertemuan, rincian tagihan/pembayaran, dan tautan fungsi lanjutan.</li>
 *   <li>{@code psb_update_biodata} -- lengkapi/perbarui biodata inti calon
 *       (padanan ringan "Lengkapi Biodata dan Berkas"; upload berkas & ujian
 *       online tetap lewat portal web via link yang dikembalikan profil).</li>
 * </ul>
 *
 * <p>Autentikasi stateless: setiap action memakai kredensial yang sama dengan
 * halaman login web (identitas + tanggal lahir), sehingga tidak perlu
 * infrastruktur token terpisah. Kompatibilitas: Java 1.7 (tanpa lambda/diamond).</p>
 */
public final class PsbCalonApi {

	private PsbCalonApi() {
	}

	private static SimpleDateFormat formatTanggal() {
		return new SimpleDateFormat("dd-MM-yyyy");
	}

	/** Parse kredensial tanggal lahir; null bila kosong/format salah. */
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
	 * Cari calon siswa dgn aturan sama persis {@code LoginCalonSiswaAction.onLogin}:
	 * wajib punya gelombang, tanggal lahir cocok, identitas dicocokkan EXACT ke
	 * nomorInduk / noUjian (ditambah noRegistrasi utk pendaftar mobile), lalu
	 * fallback ke nama lengkap. PIN ikut dicocokkan bila dikirim.
	 */
	private static CalonSiswa cariCalon(Session session, String identitas, Date tanggalLahir, String pin) {
		Criteria criteria = session.createCriteria(CalonSiswa.class)
				.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).setMaxResults(1)
				.addOrder(Order.desc("id")).add(Restrictions.eq("tanggalLahir", tanggalLahir))
				.add(Restrictions.or(Restrictions.eq("nomorInduk", identitas).ignoreCase(),
						Restrictions.or(Restrictions.eq("noUjian", identitas).ignoreCase(),
								Restrictions.eq("noRegistrasi", identitas).ignoreCase())));
		if (ApiHelperSupport.hasText(pin)) {
			criteria.add(Restrictions.eq("pinPassword", pin.trim()).ignoreCase());
		}
		CalonSiswa calonSiswa = (CalonSiswa) criteria.uniqueResult();

		if (calonSiswa == null) {
			Criteria criteriaNama = session.createCriteria(CalonSiswa.class)
					.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).setMaxResults(1)
					.addOrder(Order.desc("id")).add(Restrictions.eq("tanggalLahir", tanggalLahir))
					.add(Restrictions.eq("nama", identitas).ignoreCase());
			if (ApiHelperSupport.hasText(pin)) {
				criteriaNama.add(Restrictions.eq("pinPassword", pin.trim()).ignoreCase());
			}
			calonSiswa = (CalonSiswa) criteriaNama.uniqueResult();
		}
		return calonSiswa;
	}

	/** Action "psb_login" -- autentikasi + tandai telahLogin/waktuLogin (spt web). */
	public static JSONObject login(HttpServletRequest request, JSONObject json) {
		return proses(request, json, true);
	}

	/** Action "psb_calon_profil" -- profil calon (autentikasi ulang tanpa menandai login). */
	public static JSONObject profil(HttpServletRequest request, JSONObject json) {
		return proses(request, json, false);
	}

	private static JSONObject proses(HttpServletRequest request, JSONObject json, boolean tandaiLogin) {
		JSONObject hasil = new JSONObject();
		Session session = null;
		try {
			String identitas = ApiHelperSupport.optString(json, "identitas");
			if (!ApiHelperSupport.hasText(identitas)) {
				return ApiHelperSupport.status("90",
						"Nomor Pendaftaran / Ujian / Nama Lengkap wajib diisi");
			}
			Date tanggalLahir = parseTanggalLahir(json);
			if (tanggalLahir == null) {
				return ApiHelperSupport.status("90",
						"Tanggal lahir wajib diisi dengan format dd-MM-yyyy");
			}
			String pin = ApiHelperSupport.optString(json, "pin");

			session = HibernateUtil.getSessionFactory().openSession();
			CalonSiswa calonSiswa = cariCalon(session, identitas.trim(), tanggalLahir, pin);

			if (calonSiswa == null || calonSiswa.getId() == null) {
				return ApiHelperSupport.status("99", "Mohon maaf, data Calon Siswa dengan Nomor Pendaftaran / Nama \""
						+ identitas.trim()
						+ "\" tidak ditemukan, atau Tanggal Lahir yang Anda masukkan belum sesuai. "
						+ "Periksa kembali penulisan dan tanggal lahir Anda, atau hubungi panitia penerimaan siswa baru.");
			}

			// Konfigurasi: wajib bayar biaya pendaftaran dulu sebelum boleh login.
			if (calonSiswa.getGelombangPendaftaranPsb() != null && Common.bolehKonfigurasi(
					"calon_siswa_harus_melakukan_pembayaran_sebelum_bisa_login_baru", Konfigurasi.TIDAK_AKTIF)) {
				boolean bolehLogin;
				try {
					bolehLogin = GelombangPendaftaranPsb.chekSyaratBayar(calonSiswa);
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "auto-audit PsbCalonApi.proses(syaratBayar)");
					bolehLogin = false;
				}
				if (!bolehLogin) {
					JSONObject respon = ApiHelperSupport.status("92",
							"Mohon maaf, Anda belum menyelesaikan pembayaran biaya pendaftaran. "
									+ "Silakan selesaikan pembayaran terlebih dahulu, kemudian login kembali.");
					ApiHelperSupport.put(respon, "link_bayar",
							Common.getRequestHostWithProtocol(request)
									+ "/pages/master/sekolah/pembayaran_online.zul?calon_siswa=" + calonSiswa.getId()
									+ "&langsungBayar=true");
					return respon;
				}
			}

			if (tandaiLogin) {
				// Sama dengan web: tandai sudah pernah login + waktu login terakhir.
				calonSiswa.setTelahLogin(Boolean.TRUE);
				calonSiswa.setWaktuLogin(WaktuUtil.getDate());
				session.beginTransaction();
				session.update(calonSiswa);
				session.getTransaction().commit();
			}

			JSONObject data = buatProfil(request, session, calonSiswa);
			hasil.put("data", data);
			ApiHelperSupport.putSuccess(hasil,
					tandaiLogin ? "Login berhasil. Selamat datang, " + calonSiswa.getNama() + "!"
							: "Profil calon siswa berhasil diambil");
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			ApiHelperSupport.putError(hasil, err);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/** Status seleksi 5 kondisi -- urutan & label sama dgn area calon di portal web. */
	private static String[] statusSeleksi(CalonSiswa calonSiswa) {
		if (Boolean.TRUE.equals(calonSiswa.getMengundurkanDiri())) {
			return new String[] { "mengundurkan_diri", "Mengundurkan diri" };
		}
		if (Boolean.TRUE.equals(calonSiswa.getDitolak())) {
			return new String[] { "ditolak", "Tidak diterima (ditolak)" };
		}
		if (Boolean.TRUE.equals(calonSiswa.getTelahDiterima())) {
			return new String[] { "diterima", "Telah diterima" };
		}
		if (Boolean.TRUE.equals(calonSiswa.getTerverifikasi())) {
			return new String[] { "terverifikasi", "Telah diverifikasi" };
		}
		return new String[] { "terdaftar", "Belum dinyatakan lulus / diterima" };
	}

	/**
	 * Profil lengkap calon siswa -- data yang sama dgn kartu area calon di portal
	 * web setelah login: identitas, status, progress, jadwal pertemuan, tagihan.
	 */
	private static JSONObject buatProfil(HttpServletRequest request, Session session, CalonSiswa calonSiswa)
			throws Exception {
		JSONObject data = PsbApi.buatRingkasanCalonSiswa(request, calonSiswa);
		SimpleDateFormat df = formatTanggal();
		SimpleDateFormat dfWaktu = new SimpleDateFormat("dd-MM-yyyy HH:mm");

		JSONObject biodata = new JSONObject();
		ApiHelperSupport.put(biodata, "jenis_kelamin", calonSiswa.getJenisKelamin());
		ApiHelperSupport.put(biodata, "tempat_lahir", calonSiswa.getTempatLahir());
		ApiHelperSupport.put(biodata, "tanggal_lahir",
				calonSiswa.getTanggalLahir() == null ? null : df.format(calonSiswa.getTanggalLahir()));
		ApiHelperSupport.put(biodata, "sekolah_asal", calonSiswa.getSekolahAsal());
		ApiHelperSupport.put(biodata, "alamat", calonSiswa.getAlamatSiswa());
		ApiHelperSupport.put(biodata, "telepon", calonSiswa.getTeleponSiswa());
		ApiHelperSupport.put(biodata, "email", calonSiswa.getAlamatEmail());
		ApiHelperSupport.put(biodata, "nama_ayah", calonSiswa.getNamaAyah());
		ApiHelperSupport.put(biodata, "nama_ibu", calonSiswa.getNamaIbu());
		ApiHelperSupport.put(biodata, "telepon_orang_tua", calonSiswa.getTeleponOrangTua());
		ApiHelperSupport.put(biodata, "penjurusan",
				calonSiswa.getPenjurusanSekolah() == null ? null : calonSiswa.getPenjurusanSekolah().getNama());
		data.put("biodata", biodata);

		ApiHelperSupport.put(data, "no_ujian", calonSiswa.getNoUjian());

		String[] status = statusSeleksi(calonSiswa);
		JSONObject statusObj = new JSONObject();
		ApiHelperSupport.put(statusObj, "kode", status[0]);
		ApiHelperSupport.put(statusObj, "label", status[1]);
		ApiHelperSupport.put(statusObj, "keterangan", calonSiswa.getKeterangan());
		data.put("status_seleksi", statusObj);

		// Jadwal pertemuan siswa/orang tua (bila panitia menjadwalkan).
		try {
			if (calonSiswa.getJadwalPertemuanPSB() != null
					&& Boolean.TRUE.equals(calonSiswa.getJadwalPertemuanPSB().getAktif())) {
				JSONObject jadwal = new JSONObject();
				ApiHelperSupport.put(jadwal, "nama", calonSiswa.getJadwalPertemuanPSB().getNama());
				ApiHelperSupport.put(jadwal, "mulai",
						calonSiswa.getJadwalPertemuanPSB().getWaktuMulai() == null ? null
								: dfWaktu.format(calonSiswa.getJadwalPertemuanPSB().getWaktuMulai()));
				ApiHelperSupport.put(jadwal, "sampai",
						calonSiswa.getJadwalPertemuanPSB().getWaktuSampai() == null ? null
								: dfWaktu.format(calonSiswa.getJadwalPertemuanPSB().getWaktuSampai()));
				data.put("jadwal_pertemuan", jadwal);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PsbCalonApi.buatProfil(jadwal)");
		}

		// Rincian tagihan & pembayaran + progress tracker.
		boolean sudahBayar = false;
		JSONArray tagihans = new JSONArray();
		try {
			sudahBayar = isiTagihan(session, calonSiswa, tagihans);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PsbCalonApi.buatProfil(tagihan)");
		}
		data.put("tagihans", tagihans);

		boolean sudahVerif = Boolean.TRUE.equals(calonSiswa.getTerverifikasi());
		boolean sudahHasil = Boolean.TRUE.equals(calonSiswa.getTelahDiterima())
				|| Boolean.TRUE.equals(calonSiswa.getDitolak())
				|| Boolean.TRUE.equals(calonSiswa.getMengundurkanDiri());
		JSONObject progress = new JSONObject();
		ApiHelperSupport.put(progress, "daftar", true);
		ApiHelperSupport.put(progress, "bayar", sudahBayar);
		ApiHelperSupport.put(progress, "verifikasi", sudahVerif);
		ApiHelperSupport.put(progress, "hasil", sudahHasil);
		data.put("progress", progress);

		// Fungsi lanjutan yang tetap lewat portal web (lengkapi berkas & ujian
		// online) -- link login portal agar bisa dibuka dari mobile via WebView.
		ApiHelperSupport.put(data, "link_portal",
				ApiHelperSupport.absoluteUrl(request, "/ppdb?login=true"));
		return data;
	}

	/**
	 * Rincian tagihan calon (dibayar & belum) -- replika alur area calon portal web:
	 * (1) pembayaran yang sudah tercatat via {@link PembayaranSiswaDetail};
	 * (2) tagihan dari {@link PengaturanBiaya} aktif via TagihanUtil(CalonSiswa).
	 * Mengembalikan true bila sudah ada pembayaran tercatat (untuk progress "bayar").
	 */
	private static boolean isiTagihan(Session session, CalonSiswa calonSiswa, JSONArray keluaran) {
		Siswa siswa = calonSiswa.getSiswa();

		Criteria pembayaranCriteria = session.createCriteria(PembayaranSiswaDetail.class)
				.add(Restrictions.isNotNull("tagihan")).createAlias("itemBiayaSekolah", "itemBiayaSekolah")
				.addOrder(Order.asc("itemBiayaSekolah.nama")).createAlias("pembayaranSiswa", "pembayaranSiswa");
		if (siswa != null) {
			pembayaranCriteria.add(Restrictions.or(Restrictions.eq("pembayaranSiswa.siswa", siswa),
					Restrictions.eq("pembayaranSiswa.calonSiswa", calonSiswa)));
		} else {
			pembayaranCriteria.add(Restrictions.eq("pembayaranSiswa.calonSiswa", calonSiswa));
		}

		List<Long> dibayars = new ArrayList<Long>();
		List<?> pembayarans = pembayaranCriteria.list();
		for (Object o : pembayarans) {
			PembayaranSiswaDetail detail = (PembayaranSiswaDetail) o;
			Tagihan tagihan = detail.getTagihan();
			if (tagihan == null || tagihan.getId() == null) {
				continue;
			}
			dibayars.add(tagihan.getId());
			if (tagihan.getAktif() && !tagihan.ambilBukanTagihanData() && tagihan.getNominalBiaya() != null
					&& !tagihan.getNominalBiaya().getBukanTagihan()) {
				tambahItemTagihan(keluaran, tagihan, true);
			}
		}

		Integer bulan = Integer.valueOf(WaktuUtil.getCalendar().get(Calendar.MONTH) + 1);
		Integer tahun = Integer.valueOf(WaktuUtil.getCalendar().get(Calendar.YEAR));

		List<?> pengaturanBiayas = PengaturanBiaya
				.terapkanFilterPembayaran(session.createCriteria(PengaturanBiaya.class), siswa, calonSiswa)
				.addOrder(Order.desc("id")).addOrder(Order.desc("jenisBiayaSekolah.periode"))
				.addOrder(Order.asc("jenisBiayaSekolah.nama")).list();

		for (Object o : pengaturanBiayas) {
			PengaturanBiaya pengaturanBiaya = (PengaturanBiaya) o;
			JenisBiayaSekolah jenisBiaya = pengaturanBiaya.getJenisBiayaSekolah();
			if (!pengaturanBiaya.getAktif() || jenisBiaya == null) {
				continue;
			}
			boolean berlaku = (siswa != null && !jenisBiaya.getGunakanCalonSiswa()
					&& DetailTagihanSiswaHelper.apakahAda(pengaturanBiaya, siswa))
					|| (jenisBiaya.getGunakanCalonSiswa()
							&& DetailTagihanCalonSiswaHelper.apakahAda(pengaturanBiaya, calonSiswa));
			if (!berlaku) {
				continue;
			}

			List<Tagihan> tagihanList = jenisBiaya.getGunakanCalonSiswa()
					? TagihanUtilCalonSiswa.getTagihan(jenisBiaya, pengaturanBiaya, calonSiswa, bulan, tahun, false)
					: TagihanUtil.getTagihan(jenisBiaya, pengaturanBiaya, siswa, bulan, tahun, false);
			if (tagihanList == null) {
				continue;
			}
			for (Tagihan tagihan : tagihanList) {
				if (tagihan.getId() != null && dibayars.contains(tagihan.getId())) {
					continue; // sudah masuk daftar dibayar di atas
				}
				if (tagihan.getAktif() && !tagihan.ambilBukanTagihanData() && tagihan.getNominalBiaya() != null
						&& !tagihan.getNominalBiaya().getBukanTagihan()) {
					tambahItemTagihan(keluaran, tagihan, tagihan.getPembayaranSiswaDetail() != null);
				}
			}
		}
		return !dibayars.isEmpty();
	}

	private static void tambahItemTagihan(JSONArray keluaran, Tagihan tagihan, boolean dibayar) {
		try {
			JSONObject item = new JSONObject();
			ApiHelperSupport.put(item, "jenis_biaya",
					tagihan.getPengaturanBiaya() == null || tagihan.getPengaturanBiaya().getJenisBiayaSekolah() == null
							? null : tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getNama());
			ApiHelperSupport.put(item, "item",
					tagihan.getItemBiayaSekolah() == null ? null : tagihan.getItemBiayaSekolah().getNama());
			TagihanApiGrupUtil.putGrup(item, tagihan);
			ApiHelperSupport.put(item, "nominal", tagihan.getNominal());
			ApiHelperSupport.put(item, "dibayar", dibayar);
			keluaran.put(item);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit PsbCalonApi.tambahItemTagihan");
		}
	}

	/**
	 * Action "psb_update_biodata" -- lengkapi/perbarui biodata inti calon siswa.
	 * Kredensial sama dgn login; hanya field yang DIKIRIM yang diperbarui
	 * (partial update), sehingga aman dipanggil dari form apa pun.
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
			CalonSiswa calonSiswa = cariCalon(session, identitas.trim(), tanggalLahir,
					ApiHelperSupport.optString(json, "pin"));
			if (calonSiswa == null || calonSiswa.getId() == null) {
				return ApiHelperSupport.status("99",
						"Data calon siswa tidak ditemukan. Silakan login ulang.");
			}

			if (json.has("jenis_kelamin")) {
				calonSiswa.setJenisKelamin(ApiHelperSupport.optString(json, "jenis_kelamin"));
			}
			if (json.has("tempat_lahir")) {
				calonSiswa.setTempatLahir(ApiHelperSupport.optString(json, "tempat_lahir"));
			}
			if (json.has("sekolah_asal")) {
				calonSiswa.setSekolahAsal(ApiHelperSupport.optString(json, "sekolah_asal"));
			}
			if (json.has("alamat")) {
				calonSiswa.setAlamatSiswa(ApiHelperSupport.optString(json, "alamat"));
			}
			if (json.has("telepon")) {
				calonSiswa.setTeleponSiswa(ApiHelperSupport.optString(json, "telepon"));
			}
			if (json.has("email")) {
				calonSiswa.setAlamatEmail(ApiHelperSupport.optString(json, "email"));
			}
			if (json.has("nama_ayah")) {
				calonSiswa.setNamaAyah(ApiHelperSupport.optString(json, "nama_ayah"));
			}
			if (json.has("nama_ibu")) {
				calonSiswa.setNamaIbu(ApiHelperSupport.optString(json, "nama_ibu"));
			}
			if (json.has("telepon_orang_tua")) {
				calonSiswa.setTeleponOrangTua(ApiHelperSupport.optString(json, "telepon_orang_tua"));
			}

			session.beginTransaction();
			session.update(calonSiswa);
			session.getTransaction().commit();

			JSONObject data = buatProfil(request, session, calonSiswa);
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
