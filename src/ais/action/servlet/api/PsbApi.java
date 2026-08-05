package ais.action.servlet.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonPSB;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.WaktuUtil;

/**
 * <b>PsbApi</b> -- API publik portal PSB/PPDB untuk aplikasi mobile native.
 *
 * <p>Padanan native dari portal web {@code psb.zul} + {@link ais.action.maintenance.PSBAction}:
 * kop/identitas instansi, pengumuman PSB, daftar gelombang pendaftaran aktif,
 * pendaftaran calon siswa baru, dan cek status pendaftaran/kelulusan.
 * Semua action bersifat PUBLIK (tanpa login) sebagaimana portal webnya.</p>
 *
 * <p>Kompatibilitas: tanpa lambda/stream/diamond agar seragam dengan class API lain (Java 1.7).</p>
 */
public final class PsbApi {

	private static final int MAKS_PENGUMUMAN = 30;

	private PsbApi() {
	}

	private static SimpleDateFormat formatTanggal() {
		return new SimpleDateFormat("dd-MM-yyyy");
	}

	/**
	 * Action "psb_portal_info" -- identitas instansi + konfigurasi tampilan portal PSB.
	 * Sumber data sama dengan {@code PSBAction.headerBox()} dan {@code PSBAction.footer()}:
	 * Sekolah -> Yayasan -> PerguruanTinggi.
	 */
	public static JSONObject portalInfo(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		try {
			Sekolah sekolah = SekolahUtil.getSekolah(request);
			Yayasan yayasan = SekolahUtil.getYayasan(request);
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);

			String nama = null;
			String motto = null;
			String alamat = null;
			String telp = null;
			String wa = null;
			String email = null;
			String headerHtml = null;

			if (pt != null && pt.getId() != null) {
				nama = pt.getNama();
				motto = pt.getMotto();
				alamat = pt.getAlamat1();
				telp = pt.getTelepon();
				wa = pt.getWa();
				email = pt.getEmail();
			}
			if (yayasan != null && yayasan.getId() != null) {
				nama = yayasan.getNama();
				if (ApiHelperSupport.hasText(yayasan.getMotto())) {
					motto = yayasan.getMotto();
				}
				if (ApiHelperSupport.hasText(yayasan.getAlamat())) {
					alamat = yayasan.getAlamat();
				}
				if (ApiHelperSupport.hasText(yayasan.getTelp())) {
					telp = yayasan.getTelp();
				}
				if (ApiHelperSupport.hasText(yayasan.getWa())) {
					wa = yayasan.getWa();
				}
				if (ApiHelperSupport.hasText(yayasan.getEmail())) {
					email = yayasan.getEmail();
				}
				headerHtml = yayasan.getHeaderppdb();
			}
			if (sekolah != null && sekolah.getId() != null) {
				nama = sekolah.getNama();
				if (ApiHelperSupport.hasText(sekolah.getMotto())) {
					motto = sekolah.getMotto();
				}
				if (ApiHelperSupport.hasText(sekolah.getAlamat())) {
					alamat = sekolah.getAlamat();
				}
				if (ApiHelperSupport.hasText(sekolah.getTelp())) {
					telp = sekolah.getTelp();
				}
				if (ApiHelperSupport.hasText(sekolah.getWa())) {
					wa = sekolah.getWa();
				}
				if (ApiHelperSupport.hasText(sekolah.getEmail())) {
					email = sekolah.getEmail();
				}
				if (ApiHelperSupport.hasText(sekolah.getHeaderppdb())) {
					headerHtml = sekolah.getHeaderppdb();
				}
			}

			if (!ApiHelperSupport.hasText(nama)) {
				nama = Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai();
			}

			String logo = SekolahUtil.getSekolahMedia(request, "logo_sekolah_");
			if (!ApiHelperSupport.hasText(logo)) {
				logo = SekolahUtil.getYayasanMedia(request, "logo_yayasan_");
			}
			if (!ApiHelperSupport.hasText(logo)) {
				logo = PerguruanTinggiUtil.getPerguruanTinggiMedia("logo_perguruanTinggi_");
			}

			// Link file alur pendaftaran (bila sudah diupload) -- sama dengan onClickAlurPendaftaran().
			String alurUrl = null;
			try {
				LampiranLain alur = null;
				if (sekolah != null && sekolah.getId() != null) {
					alur = LampiranLain.ambil(sekolah.getId(), LampiranLain.ALUR_REGISTRASI_PSB);
				} else if (yayasan != null && yayasan.getId() != null) {
					alur = LampiranLain.ambil(yayasan.getId(), LampiranLain.ALUR_REGISTRASI_PSB);
				}
				if (alur == null) {
					alur = LampiranLain.ambil(LampiranLain.ID_ALUR_REGISTRASI_PSB, LampiranLain.ALUR_REGISTRASI_PSB);
				}
				if (alur != null && alur.getId() != null) {
					alurUrl = ApiHelperSupport.absoluteUrl(request, alur.createLinkUri());
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit PsbApi.portalInfo(alur)");
			}

			JSONObject data = new JSONObject();
			ApiHelperSupport.put(data, "nama", nama);
			ApiHelperSupport.put(data, "motto", motto);
			ApiHelperSupport.put(data, "alamat", alamat);
			ApiHelperSupport.put(data, "telp", telp);
			ApiHelperSupport.put(data, "wa", wa);
			ApiHelperSupport.put(data, "email", email);
			ApiHelperSupport.put(data, "label_portal",
					Common.getKonfigurasi("label_psb_kampus", "Seleksi Penerimaan Siswa Baru").getNilai());
			ApiHelperSupport.put(data, "header_html", headerHtml);
			ApiHelperSupport.put(data, "logo_url", ApiHelperSupport.absoluteUrl(request, logo));
			ApiHelperSupport.put(data, "alur_url", alurUrl);
			ApiHelperSupport.put(data, "tampil_alur", Common.bolehKonfigurasi("tampilkan_alur_psb"));
			ApiHelperSupport.put(data, "tampil_formulir", Common.bolehKonfigurasi("tampilkan_formulir_psb"));
			ApiHelperSupport.put(data, "tampil_info_pembayaran",
					Common.bolehKonfigurasi("tampilkan_informasiPembayaran_psb"));
			ApiHelperSupport.put(data, "tampil_info_kelulusan",
					Common.bolehKonfigurasi("tampilkan_informasiKelulusan_psb"));

			hasil.put("data", data);
			ApiHelperSupport.putSuccess(hasil, "Info portal PSB berhasil diambil");
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			ApiHelperSupport.putError(hasil, err);
		}
		return hasil;
	}

	/**
	 * Action "psb_pengumuman" -- daftar pengumuman PSB (diperuntukkan Untuk Calon Siswa).
	 * Kriteria mengikuti {@code TampilanPengumumanPSBAction.initCriteriaStatic} namun
	 * dijalankan pada sesi Hibernate sendiri agar aman untuk konteks servlet API.
	 */
	public static JSONObject pengumuman(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		Session session = null;
		try {
			Sekolah sekolah = SekolahUtil.getSekolah(request);
			Yayasan yayasan = SekolahUtil.getYayasan(request);

			session = HibernateUtil.getSessionFactory().openSession();

			Criteria criteria = session.createCriteria(PengumumanAkademis.class)
					.createAlias("kategoriPengumuman", "kategoriPengumuman", Criteria.LEFT_JOIN)
					.add(sekolah == null || sekolah.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("sekolah", sekolah))
					.add(yayasan == null || yayasan.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("yayasan", yayasan))
					.add(Restrictions.or(
							Restrictions.or(Restrictions.eq("tetapTampilkanPengumumanMeskipunSudahKelewat", true),
									Restrictions.isNull("tetapTampilkanPengumumanMeskipunSudahKelewat")),
							Restrictions.or(Restrictions.le("tanggal", WaktuUtil.getDate()),
									Restrictions.ge("sampai", WaktuUtil.getDate()))))
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_CALON_SISWA))
					.addOrder(Order.asc("kategoriPengumuman.nomorUrut")).addOrder(Order.desc("tanggal"))
					.addOrder(Order.desc("id")).setMaxResults(MAKS_PENGUMUMAN);

			String cari = ApiHelperSupport.optString(json, "cari");
			if (ApiHelperSupport.hasText(cari)) {
				criteria.add(Restrictions.ilike("judul", "%" + cari.trim() + "%"));
			}

			SimpleDateFormat df = formatTanggal();
			JSONArray array = new JSONArray();
			List<?> list = criteria.list();
			for (Object o : list) {
				PengumumanAkademis p = (PengumumanAkademis) o;
				if (p == null || p.getId() == null) {
					continue;
				}
				JSONObject item = new JSONObject();
				ApiHelperSupport.put(item, "id", p.getId());
				ApiHelperSupport.put(item, "judul", p.getJudul());
				ApiHelperSupport.put(item, "isi_html", p.getCatatan());
				ApiHelperSupport.put(item, "tanggal", p.getTanggal() == null ? null : df.format(p.getTanggal()));
				boolean utama = false;
				try {
					utama = p.getKategoriPengumuman() != null
							&& Boolean.TRUE.equals(p.getKategoriPengumuman().getMerupakanPengumumanUtama());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PsbApi.pengumuman(utama)");
				}
				ApiHelperSupport.put(item, "utama", utama);
				array.put(item);
			}

			hasil.put("data", array);
			if (array.length() == 0) {
				ApiHelperSupport.putEmpty(hasil, "Belum ada pengumuman");
			} else {
				ApiHelperSupport.putSuccess(hasil, "Pengumuman berhasil diambil");
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			ApiHelperSupport.putError(hasil, err);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/** Apakah gelombang sedang dibuka hari ini (aktif dan berada dalam rentang mulai-sampai). */
	private static boolean gelombangDibuka(GelombangPendaftaranPsb g, Date sekarang, String strSekarang,
			SimpleDateFormat df8) {
		if (g == null || !Boolean.TRUE.equals(g.getAktif()) || g.getMulai() == null || g.getSampai() == null) {
			return false;
		}
		String strMulai = df8.format(g.getMulai());
		String strSampai = df8.format(g.getSampai());
		return (g.getMulai().before(sekarang) || strMulai.equals(strSekarang))
				&& (g.getSampai().after(sekarang) || strSampai.equals(strSekarang));
	}

	/** Apakah gelombang berlaku untuk instansi pada request ini (scoping sekolah/yayasan). */
	private static boolean gelombangUntukInstansi(GelombangPendaftaranPsb g, Sekolah sekolah, Yayasan yayasan) {
		if (yayasan != null && yayasan.getId() != null
				&& (g.getYayasan() == null || !yayasan.getId().equals(g.getYayasan().getId()))) {
			return false;
		}
		if (sekolah != null && sekolah.getId() != null
				&& (g.getSekolah() == null || !sekolah.getId().equals(g.getSekolah().getId()))) {
			return false;
		}
		return true;
	}

	/**
	 * Action "psb_gelombang" -- daftar gelombang pendaftaran yang sedang dibuka.
	 * Filter identik dengan {@code TampilanPengumumanAkademisAction.tampilGelombang}:
	 * aktif, dalam rentang tanggal, sesuai sekolah/yayasan; gelombang khusus anak
	 * pegawai TIDAK ditampilkan karena pemanggil API ini anonim (belum login).
	 */
	public static JSONObject gelombang(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		try {
			Sekolah sekolah = SekolahUtil.getSekolah(request);
			Yayasan yayasan = SekolahUtil.getYayasan(request);

			Map<Long, GeneralValueObject> semua = ConstantValues.ambilBerdasarClass(GelombangPendaftaranPsb.class);
			List<GelombangPendaftaranPsb> terpilih = new ArrayList<GelombangPendaftaranPsb>();

			Date sekarang = WaktuUtil.getDate();
			SimpleDateFormat df8 = new SimpleDateFormat("yyyyMMdd");
			String strSekarang = df8.format(sekarang);

			if (semua != null) {
				for (GeneralValueObject gvo : semua.values()) {
					GelombangPendaftaranPsb g = (GelombangPendaftaranPsb) gvo;
					if (!gelombangDibuka(g, sekarang, strSekarang, df8)) {
						continue;
					}
					if (!gelombangUntukInstansi(g, sekolah, yayasan)) {
						continue;
					}
					if (Boolean.TRUE.equals(g.getHanyaUntukAnakPegawai())) {
						continue;
					}
					terpilih.add(g);
				}
			}
			Collections.sort(terpilih);

			SimpleDateFormat df = formatTanggal();
			JSONArray array = new JSONArray();
			for (GelombangPendaftaranPsb g : terpilih) {
				JSONObject item = new JSONObject();
				ApiHelperSupport.put(item, "id", g.getId());
				ApiHelperSupport.put(item, "nama", g.getNama());
				ApiHelperSupport.put(item, "tahun_ajaran", g.getTahunAjaran());
				ApiHelperSupport.put(item, "tahun_masuk", g.getTahunMasuk());
				ApiHelperSupport.put(item, "mulai", g.getMulai() == null ? null : df.format(g.getMulai()));
				ApiHelperSupport.put(item, "sampai", g.getSampai() == null ? null : df.format(g.getSampai()));
				ApiHelperSupport.put(item, "informasi", g.getInformasi());
				ApiHelperSupport.put(item, "keterangan", g.getKeterangan());
				ApiHelperSupport.put(item, "sekolah",
						g.getSekolah() == null || g.getSekolah().getId() == null ? null : g.getSekolah().getNama());
				array.put(item);
			}

			hasil.put("data", array);
			if (array.length() == 0) {
				ApiHelperSupport.putEmpty(hasil, "Belum ada gelombang pendaftaran yang dibuka");
			} else {
				ApiHelperSupport.putSuccess(hasil, "Gelombang pendaftaran berhasil diambil");
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			ApiHelperSupport.putError(hasil, err);
		}
		return hasil;
	}

	/**
	 * Action "psb_daftar" -- pendaftaran calon siswa baru dari aplikasi mobile.
	 *
	 * <p>Field wajib: {@code gelombang_id}, {@code nama}, {@code telepon}.
	 * Field opsional: jenis_kelamin (L/P), tempat_lahir, tanggal_lahir (dd-MM-yyyy),
	 * email, sekolah_asal, alamat, nama_ayah, nama_ibu, telepon_orang_tua.</p>
	 *
	 * <p>Alur mengikuti {@code CalonSiswaAction}: generate No Registrasi via
	 * {@link CommonPSB#generateNoRegistrasi}, lalu generate tagihan pendaftaran
	 * seperti {@link SiswaBaruApi#pendaftaran_siswa_baru} dan kembalikan link bayar.</p>
	 */
	public static JSONObject daftar(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		Session session = null;
		try {
			String nama = ApiHelperSupport.optString(json, "nama");
			String telepon = ApiHelperSupport.optString(json, "telepon");
			long gelombangId = json == null ? 0L : json.optLong("gelombang_id", 0L);

			if (!ApiHelperSupport.hasText(nama)) {
				return ApiHelperSupport.status("90", "Nama calon siswa wajib diisi");
			}
			if (!ApiHelperSupport.hasText(telepon)) {
				return ApiHelperSupport.status("90", "Nomor telepon/WA wajib diisi");
			}
			if (gelombangId <= 0L) {
				return ApiHelperSupport.status("90", "Gelombang pendaftaran wajib dipilih");
			}

			Date tanggalLahir = null;
			String strTanggalLahir = ApiHelperSupport.optString(json, "tanggal_lahir");
			if (ApiHelperSupport.hasText(strTanggalLahir)) {
				try {
					tanggalLahir = formatTanggal().parse(strTanggalLahir.trim());
				} catch (Exception e) {
					return ApiHelperSupport.status("90", "Format tanggal lahir tidak valid (gunakan dd-MM-yyyy)");
				}
			}

			session = HibernateUtil.getSessionFactory().openSession();

			GelombangPendaftaranPsb gelombang = (GelombangPendaftaranPsb) session.get(GelombangPendaftaranPsb.class,
					Long.valueOf(gelombangId));
			Date sekarang = WaktuUtil.getDate();
			SimpleDateFormat df8 = new SimpleDateFormat("yyyyMMdd");
			if (!gelombangDibuka(gelombang, sekarang, df8.format(sekarang), df8)) {
				return ApiHelperSupport.status("90",
						"Gelombang pendaftaran ini sudah ditutup. Silakan pilih gelombang lain.");
			}

			// Cegah pendaftaran ganda (mis. user menekan tombol dua kali):
			// nama + telepon sama pada gelombang yang sama dianggap pendaftaran yang sama.
			CalonSiswa sudahAda = (CalonSiswa) session.createCriteria(CalonSiswa.class)
					.add(Restrictions.eq("gelombangPendaftaranPsb", gelombang))
					.add(Restrictions.ilike("nama", nama.trim()))
					.add(Restrictions.eq("teleponSiswa", telepon.trim())).setMaxResults(1).uniqueResult();
			if (sudahAda != null && sudahAda.getId() != null) {
				JSONObject data = buatRingkasanCalonSiswa(request, sudahAda);
				hasil.put("data", data);
				ApiHelperSupport.putStatus(hasil, "00",
						"Anda sudah terdaftar pada gelombang ini. Berikut data pendaftaran Anda.");
				return hasil;
			}

			CalonSiswa calonSiswa = new CalonSiswa();
			calonSiswa.setNama(nama.trim());
			calonSiswa.setNamaSiswa(nama.trim());
			calonSiswa.setTeleponSiswa(telepon.trim());
			calonSiswa.setJenisKelamin(ApiHelperSupport.optString(json, "jenis_kelamin"));
			calonSiswa.setTempatLahir(ApiHelperSupport.optString(json, "tempat_lahir"));
			calonSiswa.setTanggalLahir(tanggalLahir);
			calonSiswa.setAlamatEmail(ApiHelperSupport.optString(json, "email"));
			calonSiswa.setSekolahAsal(ApiHelperSupport.optString(json, "sekolah_asal"));
			calonSiswa.setAlamatSiswa(ApiHelperSupport.optString(json, "alamat"));
			calonSiswa.setNamaAyah(ApiHelperSupport.optString(json, "nama_ayah"));
			calonSiswa.setNamaIbu(ApiHelperSupport.optString(json, "nama_ibu"));
			calonSiswa.setTeleponOrangTua(ApiHelperSupport.optString(json, "telepon_orang_tua"));
			calonSiswa.setGelombangPendaftaranPsb(gelombang);
			calonSiswa.setSekolah(gelombang.getSekolah());
			calonSiswa.setTahunMasuk(gelombang.getTahunMasuk());
			calonSiswa.setTanggalPendaftaran(sekarang);
			calonSiswa.setPadaTanggal(sekarang);
			calonSiswa.setNoRegistrasi(CommonPSB.generateNoRegistrasi(calonSiswa));

			session.beginTransaction();
			session.save(calonSiswa);
			session.getTransaction().commit();

			// Generate tagihan pendaftaran (pola sama dengan SiswaBaruApi.pendaftaran_siswa_baru).
			try {
				if (gelombang.getJenisBiayaSekolah() != null) {
					TagihanUtilCalonSiswa.doGenerateTagihanInsendentil(calonSiswa, gelombang.getJenisBiayaSekolah(),
							false);
				}
				if (gelombang.getJenisBiayaSekolahTerverifikasi() != null) {
					TagihanUtilCalonSiswa.doGenerateTagihanInsendentil(calonSiswa,
							gelombang.getJenisBiayaSekolahTerverifikasi(), false);
				}
				if (gelombang.getJenisBiayaSekolahLulus() != null) {
					TagihanUtilCalonSiswa.doGenerateTagihanInsendentil(calonSiswa,
							gelombang.getJenisBiayaSekolahLulus(), false);
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit PsbApi.daftar(tagihan)");
			}

			JSONObject data = buatRingkasanCalonSiswa(request, calonSiswa);
			hasil.put("data", data);
			ApiHelperSupport.putSuccess(hasil,
					"Pendaftaran berhasil. Simpan Nomor Registrasi Anda untuk cek status dan pembayaran.");
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			ApiHelperSupport.putError(hasil, err);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/**
	 * Action "psb_cek_status" -- cek status pendaftaran/kelulusan berdasarkan No Registrasi
	 * (padanan native halaman cari data peserta ujian & pembayaran di portal web).
	 */
	public static JSONObject cekStatus(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		Session session = null;
		try {
			String noReg = ApiHelperSupport.optString(json, "no_registrasi");
			if (!ApiHelperSupport.hasText(noReg)) {
				return ApiHelperSupport.status("90", "Nomor registrasi wajib diisi");
			}

			session = HibernateUtil.getSessionFactory().openSession();
			CalonSiswa calonSiswa = (CalonSiswa) session.createCriteria(CalonSiswa.class)
					.add(Restrictions.or(Restrictions.eq("noRegistrasi", noReg.trim()),
							Restrictions.eq("nomorInduk", noReg.trim())))
					.setMaxResults(1).uniqueResult();

			if (calonSiswa == null || calonSiswa.getId() == null) {
				return ApiHelperSupport.status("99",
						"Data pendaftaran tidak ditemukan. Periksa kembali Nomor Registrasi Anda.");
			}

			JSONObject data = buatRingkasanCalonSiswa(request, calonSiswa);
			hasil.put("data", data);
			ApiHelperSupport.putSuccess(hasil, "Data pendaftaran ditemukan");
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			ApiHelperSupport.putError(hasil, err);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/** Ringkasan CalonSiswa yang dikembalikan action daftar & cek status (bentuk konsisten, reusable). */
	private static JSONObject buatRingkasanCalonSiswa(HttpServletRequest request, CalonSiswa calonSiswa)
			throws Exception {
		JSONObject data = new JSONObject();
		SimpleDateFormat df = formatTanggal();
		ApiHelperSupport.put(data, "id", calonSiswa.getId());
		ApiHelperSupport.put(data, "no_registrasi", calonSiswa.getNoRegistrasi());
		ApiHelperSupport.put(data, "nama", calonSiswa.getNama());
		ApiHelperSupport.put(data, "tanggal_pendaftaran", calonSiswa.getTanggalPendaftaran() == null ? null
				: df.format(calonSiswa.getTanggalPendaftaran()));
		GelombangPendaftaranPsb g = calonSiswa.getGelombangPendaftaranPsb();
		ApiHelperSupport.put(data, "gelombang", g == null ? null : g.getNama());
		ApiHelperSupport.put(data, "tahun_ajaran", g == null ? null : g.getTahunAjaran());
		ApiHelperSupport.put(data, "sekolah", calonSiswa.getSekolah() == null || calonSiswa.getSekolah().getId() == null
				? null : calonSiswa.getSekolah().getNama());

		boolean diterima = Boolean.TRUE.equals(calonSiswa.getTelahDiterima());
		boolean terverifikasi = Boolean.TRUE.equals(calonSiswa.getTerverifikasi());
		ApiHelperSupport.put(data, "terverifikasi", terverifikasi);
		ApiHelperSupport.put(data, "diterima", diterima);
		ApiHelperSupport.put(data, "status_label",
				diterima ? "LULUS / DITERIMA" : terverifikasi ? "TERVERIFIKASI" : "TERDAFTAR");

		// Link pembayaran online -- pola sama dengan SiswaBaruApi.pendaftaran_siswa_baru.
		ApiHelperSupport.put(data, "link_bayar", Common.getRequestHostWithProtocol(request)
				+ "/pages/master/sekolah/pembayaran_online.zul?calon_siswa=" + calonSiswa.getId() + "&langsungBayar=true");
		return data;
	}
}
