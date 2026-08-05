package ais.action.servlet.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.pmb.noreg.NoRegGenerator;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.Jurusan;
import ais.database.model.Paket;
import ais.database.model.PaketJurusanPmb;
import ais.database.model.PaketPunyaGelombangPendaftaran;
import ais.database.model.PengumumanAkademis;
import ais.database.model.PerguruanTinggi;
import ais.database.model.file.LampiranLain;
import ais.ui.util.WaktuUtil;

/**
 * <b>PmbApi</b> -- API publik portal PMB (Penerimaan Mahasiswa Baru) untuk
 * aplikasi mobile native.
 *
 * <p>Padanan native dari portal web {@code pmb.zul} + {@link ais.action.maintenance.PMBAction}:
 * kop/identitas perguruan tinggi, pengumuman PMB, program studi per fakultas,
 * gelombang pendaftaran yang dibuka (beserta paket & pilihan prodi),
 * pendaftaran calon mahasiswa baru, dan cek status berdasarkan No Registrasi.
 * Semua action bersifat PUBLIK (tanpa login) sebagaimana portal webnya.</p>
 *
 * <p>Pola & struktur response sengaja konsisten dengan {@link PsbApi}
 * (key: nama, motto, label_portal, header_html, dst.) agar model & widget
 * di sisi mobile bisa dipakai ulang. Kompatibilitas: tanpa lambda/stream/diamond
 * agar seragam dengan class API lain (Java 1.7).</p>
 */
public final class PmbApi {

	private static final int MAKS_PENGUMUMAN = 30;

	private PmbApi() {
	}

	private static SimpleDateFormat formatTanggal() {
		return new SimpleDateFormat("dd-MM-yyyy");
	}

	/** Tahun akademik PMB berjalan -- logika sama dengan {@code PMBAction.initProdi()}. */
	private static String tahunAkademikPmb() {
		return Common.getKonfigurasi("tahunAkademikPenerimaanMahasiswaBaru", Common.getCurrentTahunAkademik())
				.getNilai();
	}

	/**
	 * Action "pmb_portal_info" -- identitas perguruan tinggi + konfigurasi tampilan
	 * portal PMB. Sumber data sama dengan {@code PMBAction.headerBox()} dan footer PT.
	 */
	public static JSONObject portalInfo(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		try {
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);

			String nama = pt == null ? null : pt.getNama();
			if (!ApiHelperSupport.hasText(nama)) {
				nama = Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai();
			}

			// Link file alur pendaftaran -- sama dengan PMBAction.onClickAlurPendaftaran():
			// pakai lampiran ALUR_REGISTRASI_PMB bila ada, fallback gambar bawaan.
			String alurUrl = null;
			try {
				LampiranLain alur = LampiranLain.ambil(LampiranLain.ID_ALUR_REGISTRASI_PMB,
						LampiranLain.ALUR_REGISTRASI_PMB);
				if (alur != null && alur.getId() != null) {
					alurUrl = ApiHelperSupport.absoluteUrl(request, alur.createLinkUri());
				} else {
					alurUrl = ApiHelperSupport.absoluteUrl(request, "/img/Alur_SPMB_Mandiri.jpg");
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit PmbApi.portalInfo(alur)");
			}

			JSONObject data = new JSONObject();
			ApiHelperSupport.put(data, "nama", nama);
			ApiHelperSupport.put(data, "motto", pt == null ? null : pt.getMotto());
			ApiHelperSupport.put(data, "alamat", pt == null ? null : pt.getAlamat1());
			ApiHelperSupport.put(data, "telp", pt == null ? null : pt.getTelepon());
			ApiHelperSupport.put(data, "wa", pt == null ? null : pt.getWa());
			ApiHelperSupport.put(data, "email", pt == null ? null : pt.getEmail());
			ApiHelperSupport.put(data, "label_portal", Common
					.getKonfigurasi("label_pmb_kampus", "Seleksi Penerimaan Mahasiswa Baru").getNilai());
			ApiHelperSupport.put(data, "header_html", pt == null ? null : pt.getHeaderpmb());
			ApiHelperSupport.put(data, "logo_url", ApiHelperSupport.absoluteUrl(request,
					PerguruanTinggiUtil.getPerguruanTinggiMedia("logo_perguruanTinggi_")));
			ApiHelperSupport.put(data, "alur_url", alurUrl);
			ApiHelperSupport.put(data, "tampil_alur", Common.bolehKonfigurasi("tampilkan_alur_pmb"));
			ApiHelperSupport.put(data, "tampil_formulir", Common.bolehKonfigurasi("tampilkan_formulir_pmb"));
			ApiHelperSupport.put(data, "tampil_info_pembayaran",
					Common.bolehKonfigurasi("tampilkan_informasiPembayaran_pmb"));
			ApiHelperSupport.put(data, "tampil_info_kelulusan",
					Common.bolehKonfigurasi("tampilkan_informasiKelulusan_pmb"));

			hasil.put("data", data);
			ApiHelperSupport.putSuccess(hasil, "Info portal PMB berhasil diambil");
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			ApiHelperSupport.putError(hasil, err);
		}
		return hasil;
	}

	/**
	 * Action "pmb_pengumuman" -- daftar pengumuman PMB (Untuk Calon Mahasiswa /
	 * Untuk Peserta). Kriteria mengikuti {@code TampilanPengumumanPMBAction.initCriteriaStatic}
	 * namun dijalankan pada sesi Hibernate sendiri agar aman untuk konteks servlet API.
	 */
	public static JSONObject pengumuman(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		Session session = null;
		try {
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);

			session = HibernateUtil.getSessionFactory().openSession();

			Criteria criteria = session.createCriteria(PengumumanAkademis.class)
					.createAlias("kategoriPengumuman", "kategoriPengumuman", Criteria.LEFT_JOIN)
					.add(pt == null || pt.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("perguruanTinggi", pt),
									Restrictions.isNull("perguruanTinggi")))
					.createAlias("fakultas", "fakultas", Criteria.LEFT_JOIN)
					.add(pt == null || pt.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.or(Restrictions.eq("fakultas.perguruanTinggi", pt),
									Restrictions.isNull("fakultas.perguruanTinggi")))
					.add(Restrictions.or(
							Restrictions.or(Restrictions.eq("tetapTampilkanPengumumanMeskipunSudahKelewat", true),
									Restrictions.isNull("tetapTampilkanPengumumanMeskipunSudahKelewat")),
							Restrictions.or(Restrictions.le("tanggal", WaktuUtil.getDate()),
									Restrictions.ge("sampai", WaktuUtil.getDate()))))
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.or(
							Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_CALON_MAHASISWA),
							Restrictions.eq("diperuntukkan", PengumumanAkademis.UNTUK_PESERTA)))
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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) PmbApi.pengumuman(utama)");
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

	/**
	 * Action "pmb_prodi" -- daftar program studi (Jurusan) aktif per fakultas,
	 * mengikuti listing beranda {@code PMBAction.initProdi()}.
	 * Param opsional {@code paket_id}: hanya prodi yang tersedia untuk paket tsb
	 * (relasi {@link PaketJurusanPmb}).
	 */
	public static JSONObject prodi(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		Session session = null;
		try {
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
			long paketId = json == null ? 0L : json.optLong("paket_id", 0L);

			session = HibernateUtil.getSessionFactory().openSession();

			List<?> jurusans = session.createCriteria(Jurusan.class).createAlias("fakultas", "fakultas")
					.add(pt == null || pt.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("fakultas.perguruanTinggi", pt))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("fakultas.nama")).addOrder(Order.asc("nama")).list();

			// Bila paket dipilih: batasi ke jurusan yang punya relasi PaketJurusanPmb dgn paket tsb.
			Map<Long, Boolean> jurusanUntukPaket = null;
			if (paketId > 0L) {
				jurusanUntukPaket = new HashMap<Long, Boolean>();
				List<?> relasi = session.createCriteria(PaketJurusanPmb.class)
						.createAlias("paket", "paket").add(Restrictions.eq("paket.id", Long.valueOf(paketId)))
						.add(Restrictions.isNotNull("jurusan")).list();
				for (Object o : relasi) {
					PaketJurusanPmb pj = (PaketJurusanPmb) o;
					if (pj != null && pj.getJurusan() != null && pj.getJurusan().getId() != null) {
						jurusanUntukPaket.put(pj.getJurusan().getId(), Boolean.TRUE);
					}
				}
			}

			JSONArray array = new JSONArray();
			for (Object o : jurusans) {
				Jurusan jurusan = (Jurusan) o;
				if (jurusan == null || jurusan.getId() == null) {
					continue;
				}
				if (jurusanUntukPaket != null && !jurusanUntukPaket.containsKey(jurusan.getId())) {
					continue;
				}
				JSONObject item = new JSONObject();
				ApiHelperSupport.put(item, "id", jurusan.getId());
				ApiHelperSupport.put(item, "nama", jurusan.getNama());
				ApiHelperSupport.put(item, "jenjang",
						jurusan.getJenjang() == null ? null : jurusan.getJenjang().getNama());
				ApiHelperSupport.put(item, "fakultas",
						jurusan.getFakultas() == null ? null : jurusan.getFakultas().getNama());
				ApiHelperSupport.put(item, "deskripsi_html", jurusan.getDeskripsi());
				array.put(item);
			}

			hasil.put("data", array);
			if (array.length() == 0) {
				ApiHelperSupport.putEmpty(hasil, "Belum ada program studi");
			} else {
				ApiHelperSupport.putSuccess(hasil, "Program studi berhasil diambil");
			}
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			ApiHelperSupport.putError(hasil, err);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/**
	 * Action "pmb_gelombang" -- gelombang pendaftaran yang sedang dibuka beserta
	 * paket yang bisa dipilih. Filter mengikuti {@code PMBAction.initProdi()}:
	 * tahun akademik PMB berjalan (kecuali konfigurasi {@code default_ta_pmb_adalah_semua}),
	 * bisaDipilihPendaftarOnline, rentang mulai-sampai, PT, aktif.
	 */
	public static JSONObject gelombang(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		Session session = null;
		try {
			PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);

			session = HibernateUtil.getSessionFactory().openSession();

			Criteria criteria = session.createCriteria(GelombangPendaftaran.class)
					.add(Restrictions.or(Restrictions.eq("bisaDipilihPendaftarOnline", true),
							Restrictions.isNull("bisaDipilihPendaftarOnline")))
					.add(Restrictions.and(Restrictions.le("mulai", WaktuUtil.getDate()),
							Restrictions.ge("sampai", WaktuUtil.getDate())))
					.add(pt == null || pt.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("perguruanTinggi", pt))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("nama"));
			if (!Common.bolehKonfigurasi("default_ta_pmb_adalah_semua")) {
				criteria.add(Restrictions.eq("tahunAkademik", tahunAkademikPmb()));
			}
			List<?> gelombangs = criteria.list();

			// Paket per gelombang: relasi PaketPunyaGelombangPendaftaran + paket yang
			// bisa dipilih di semua gelombang.
			Map<Long, List<Paket>> paketPerGelombang = new LinkedHashMap<Long, List<Paket>>();
			List<?> relasi = session.createCriteria(PaketPunyaGelombangPendaftaran.class)
					.createAlias("paket", "paket").add(Restrictions.eq("paket.aktif", true))
					.add(Restrictions.isNotNull("gelombangPendaftaran")).list();
			for (Object o : relasi) {
				PaketPunyaGelombangPendaftaran punya = (PaketPunyaGelombangPendaftaran) o;
				if (punya == null || punya.getPaket() == null || punya.getGelombangPendaftaran() == null
						|| punya.getGelombangPendaftaran().getId() == null) {
					continue;
				}
				List<Paket> pakets = paketPerGelombang.get(punya.getGelombangPendaftaran().getId());
				if (pakets == null) {
					pakets = new ArrayList<Paket>();
					paketPerGelombang.put(punya.getGelombangPendaftaran().getId(), pakets);
				}
				pakets.add(punya.getPaket());
			}
			List<Paket> paketSemuaGelombang = new ArrayList<Paket>();
			List<?> paketBebas = session.createCriteria(Paket.class).add(Restrictions.eq("aktif", true))
					.add(Restrictions.eq("bisaDipilihSemuaGelombang", true)).addOrder(Order.asc("nama")).list();
			for (Object o : paketBebas) {
				paketSemuaGelombang.add((Paket) o);
			}

			SimpleDateFormat df = formatTanggal();
			JSONArray array = new JSONArray();
			for (Object o : gelombangs) {
				GelombangPendaftaran g = (GelombangPendaftaran) o;
				if (g == null || g.getId() == null) {
					continue;
				}
				JSONObject item = new JSONObject();
				ApiHelperSupport.put(item, "id", g.getId());
				ApiHelperSupport.put(item, "nama", g.getNama());
				ApiHelperSupport.put(item, "tahun_ajaran", g.getTahunAkademik());
				ApiHelperSupport.put(item, "jenis_semester", g.getJenisSemester());
				ApiHelperSupport.put(item, "mulai", g.getMulai() == null ? null : df.format(g.getMulai()));
				ApiHelperSupport.put(item, "sampai", g.getSampai() == null ? null : df.format(g.getSampai()));
				ApiHelperSupport.put(item, "informasi", g.getInfo());
				ApiHelperSupport.put(item, "keterangan", g.getKeterangan());

				// Gabungkan paket relasi + paket semua-gelombang (tanpa duplikat).
				Map<Long, Paket> gabung = new LinkedHashMap<Long, Paket>();
				List<Paket> punya = paketPerGelombang.get(g.getId());
				if (punya != null) {
					for (Paket paket : punya) {
						gabung.put(paket.getId(), paket);
					}
				}
				for (Paket paket : paketSemuaGelombang) {
					gabung.put(paket.getId(), paket);
				}
				JSONArray pakets = new JSONArray();
				for (Paket paket : gabung.values()) {
					JSONObject p = new JSONObject();
					ApiHelperSupport.put(p, "id", paket.getId());
					ApiHelperSupport.put(p, "nama", paket.getNama());
					ApiHelperSupport.put(p, "keterangan", paket.getKeterangan());
					ApiHelperSupport.put(p, "jumlah_prodi",
							paket.getJumlahProdiYgBolehDiambil() == null ? Integer.valueOf(1)
									: paket.getJumlahProdiYgBolehDiambil());
					pakets.put(p);
				}
				item.put("pakets", pakets);
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
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/** Generate No Registrasi memakai generator terkonfigurasi -- logika sama dgn
	 * {@code CommonPMB.generateNoRegistrasi} TANPA Messagebox ZK (tidak tersedia di servlet). */
	private static String generateNoReg(BiodataCalonMahasiswa biodata) throws Exception {
		NoRegGenerator generator = (NoRegGenerator) Class.forName(Common
				.getKonfigurasi("class_untuk_generate_no_reg", "ais.action.master.pmb.noreg.DefaultNoRegGenerator")
				.getNilai()).newInstance();
		try {
			return generator.generateNoReg(biodata);
		} finally {
			// DefaultNoRegGenerator memakai sesi thread-bound tanpa menutupnya.
			ApiHelperSupport.closeCurrentNativeSession();
		}
	}

	/**
	 * Action "pmb_daftar" -- pendaftaran calon mahasiswa baru dari aplikasi mobile.
	 *
	 * <p>Field wajib: {@code gelombang_id}, {@code prodi1_id}, {@code nama}, {@code hp}.
	 * Field opsional: paket_id, prodi2_id, jenis_kelamin (L/P), tempat_lahir,
	 * tanggal_lahir (dd-MM-yyyy), email, asal_sma, alamat, nama_ayah, nama_ibu.</p>
	 *
	 * <p>Alur mengikuti {@code BiodataCalonMahasiswaAction}: PIN acak, tanggal daftar
	 * (tanpa jam), No Registrasi via generator terkonfigurasi.</p>
	 */
	public static JSONObject daftar(HttpServletRequest request, JSONObject json) {
		JSONObject hasil = new JSONObject();
		Session session = null;
		try {
			String nama = ApiHelperSupport.optString(json, "nama");
			String hp = ApiHelperSupport.optString(json, "hp");
			long gelombangId = json == null ? 0L : json.optLong("gelombang_id", 0L);
			long prodi1Id = json == null ? 0L : json.optLong("prodi1_id", 0L);
			long prodi2Id = json == null ? 0L : json.optLong("prodi2_id", 0L);
			long paketId = json == null ? 0L : json.optLong("paket_id", 0L);

			if (!ApiHelperSupport.hasText(nama)) {
				return ApiHelperSupport.status("90", "Nama calon mahasiswa wajib diisi");
			}
			if (!ApiHelperSupport.hasText(hp)) {
				return ApiHelperSupport.status("90", "Nomor HP/WA wajib diisi");
			}
			if (gelombangId <= 0L) {
				return ApiHelperSupport.status("90", "Gelombang pendaftaran wajib dipilih");
			}
			if (prodi1Id <= 0L) {
				return ApiHelperSupport.status("90", "Program studi pilihan 1 wajib dipilih");
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

			GelombangPendaftaran gelombang = (GelombangPendaftaran) session.get(GelombangPendaftaran.class,
					Long.valueOf(gelombangId));
			Date sekarang = WaktuUtil.getDate();
			boolean dibuka = gelombang != null
					&& (gelombang.getAktif() == null || Boolean.TRUE.equals(gelombang.getAktif()))
					&& gelombang.getMulai() != null && gelombang.getSampai() != null
					&& !gelombang.getMulai().after(sekarang) && !gelombang.getSampai().before(sekarang);
			if (!dibuka) {
				return ApiHelperSupport.status("90",
						"Gelombang pendaftaran ini sudah ditutup. Silakan pilih gelombang lain.");
			}

			Jurusan prodi1 = (Jurusan) session.get(Jurusan.class, Long.valueOf(prodi1Id));
			if (prodi1 == null || prodi1.getId() == null) {
				return ApiHelperSupport.status("90", "Program studi pilihan 1 tidak ditemukan");
			}
			Jurusan prodi2 = prodi2Id <= 0L ? null : (Jurusan) session.get(Jurusan.class, Long.valueOf(prodi2Id));
			Paket paket = paketId <= 0L ? null : (Paket) session.get(Paket.class, Long.valueOf(paketId));

			// Cegah pendaftaran ganda (mis. user menekan tombol dua kali):
			// nama + hp sama pada gelombang yang sama dianggap pendaftaran yang sama.
			BiodataCalonMahasiswa sudahAda = (BiodataCalonMahasiswa) session
					.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.eq("gelombangPendaftaran", gelombang))
					.add(Restrictions.ilike("nama", nama.trim())).add(Restrictions.eq("hp", hp.trim()))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setMaxResults(1).uniqueResult();
			if (sudahAda != null && sudahAda.getId() != null) {
				JSONObject data = buatRingkasan(request, sudahAda);
				hasil.put("data", data);
				ApiHelperSupport.putStatus(hasil, "00",
						"Anda sudah terdaftar pada gelombang ini. Berikut data pendaftaran Anda.");
				return hasil;
			}

			BiodataCalonMahasiswa biodata = new BiodataCalonMahasiswa();
			biodata.setNama(nama.trim());
			biodata.setHp(hp.trim());
			biodata.setJenisKelamin(ApiHelperSupport.optString(json, "jenis_kelamin"));
			biodata.setTempatLahir(ApiHelperSupport.optString(json, "tempat_lahir"));
			biodata.setTanggalLahir(tanggalLahir);
			biodata.setEmail(ApiHelperSupport.optString(json, "email"));
			biodata.setAsalSma(ApiHelperSupport.optString(json, "asal_sma"));
			biodata.setAlamat(ApiHelperSupport.optString(json, "alamat"));
			biodata.setNamaAyah(ApiHelperSupport.optString(json, "nama_ayah"));
			biodata.setNamaIbu(ApiHelperSupport.optString(json, "nama_ibu"));
			biodata.setGelombangPendaftaran(gelombang);
			biodata.setPaket(paket);
			biodata.setProdi1(prodi1);
			biodata.setProdi2(prodi2);
			biodata.setTahun(gelombang.getTahun() != null ? gelombang.getTahun()
					: Integer.valueOf(WaktuUtil.getCalendar().get(Calendar.YEAR)));

			// PIN acak + tanggal daftar tanpa jam -- pola sama dengan BiodataCalonMahasiswaAction.
			biodata.setPin(Integer.valueOf(new Random().nextInt(99999)));
			Calendar cal = WaktuUtil.getCalendar();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			biodata.setTanggalDaftar(cal.getTime());
			biodata.setTanggalPendaftaran(sekarang);
			biodata.setNoRegistrasi(generateNoReg(biodata));

			session.beginTransaction();
			session.save(biodata);
			session.getTransaction().commit();

			JSONObject data = buatRingkasan(request, biodata);
			hasil.put("data", data);
			ApiHelperSupport.putSuccess(hasil,
					"Pendaftaran berhasil. Simpan Nomor Registrasi Anda untuk cek status seleksi.");
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			ApiHelperSupport.putError(hasil, err);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return hasil;
	}

	/**
	 * Action "pmb_cek_status" -- cek status pendaftaran/kelulusan berdasarkan No
	 * Registrasi atau No Ujian (padanan native halaman cari data peserta ujian PMB).
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
			BiodataCalonMahasiswa biodata = (BiodataCalonMahasiswa) session
					.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.eq("noRegistrasi", noReg.trim()),
							Restrictions.eq("noUjian", noReg.trim())))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

			if (biodata == null || biodata.getId() == null) {
				return ApiHelperSupport.status("99",
						"Data pendaftaran tidak ditemukan. Periksa kembali Nomor Registrasi Anda.");
			}

			JSONObject data = buatRingkasan(request, biodata);
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

	private static String namaProdi(Jurusan jurusan) {
		if (jurusan == null || jurusan.getId() == null) {
			return null;
		}
		return (jurusan.getJenjang() == null ? "" : jurusan.getJenjang().getNama() + " - ") + jurusan.getNama();
	}

	/** Ringkasan BiodataCalonMahasiswa untuk action daftar & cek status (bentuk
	 * konsisten dgn ringkasan {@link PsbApi} agar widget mobile bisa dipakai ulang). */
	private static JSONObject buatRingkasan(HttpServletRequest request, BiodataCalonMahasiswa biodata)
			throws Exception {
		JSONObject data = new JSONObject();
		SimpleDateFormat df = formatTanggal();
		ApiHelperSupport.put(data, "id", biodata.getId());
		ApiHelperSupport.put(data, "no_registrasi", biodata.getNoRegistrasi());
		ApiHelperSupport.put(data, "no_ujian", biodata.getNoUjian());
		ApiHelperSupport.put(data, "nama", biodata.getNama());
		ApiHelperSupport.put(data, "tanggal_pendaftaran",
				biodata.getTanggalDaftar() == null ? null : df.format(biodata.getTanggalDaftar()));
		GelombangPendaftaran g = biodata.getGelombangPendaftaran();
		ApiHelperSupport.put(data, "gelombang", g == null ? null : g.getNama());
		ApiHelperSupport.put(data, "tahun_ajaran", g == null ? null : g.getTahunAkademik());
		ApiHelperSupport.put(data, "paket", biodata.getPaket() == null ? null : biodata.getPaket().getNama());
		ApiHelperSupport.put(data, "prodi1", namaProdi(biodata.getProdi1()));
		ApiHelperSupport.put(data, "prodi2", namaProdi(biodata.getProdi2()));
		ApiHelperSupport.put(data, "prodi_lulus", namaProdi(biodata.getProdiLulus()));

		boolean diterima = biodata.getProdiLulus() != null && biodata.getProdiLulus().getId() != null;
		ApiHelperSupport.put(data, "diterima", diterima);
		ApiHelperSupport.put(data, "terverifikasi", false);
		ApiHelperSupport.put(data, "status_label", diterima ? "LULUS / DITERIMA" : "TERDAFTAR");
		return data;
	}
}
