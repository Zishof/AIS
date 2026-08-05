package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import ais.common.AngketUtil;
import ais.common.ChecklistPenilaianGuruHelper;
import ais.common.ChecklistPenilaianHelper;
import ais.common.Common;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;

/**
 * Endpoint mobile untuk mengecek apakah user yang sedang login punya
 * kewajiban mengisi Angket/Kuesioner yang jadwalnya sedang aktif, sebelum
 * boleh melanjutkan pemakaian aplikasi -- meniru persis gerbang 3-cabang
 * {@code MainHelper.prosesAngketSaatLogin} di sisi web (dosen/guru dari
 * jadwal umum, grup kuesioner umum, umum), tapi murni membaca lewat method
 * yang sudah ada di {@link ChecklistPenilaianHelper}, {@link AngketUtil} dan
 * {@link ChecklistPenilaianGuruHelper}. Tidak mengubah endpoint
 * daftarAngket/daftarAngketUmum yang sudah dipakai fitur browsing/isi angket
 * di mobile.
 */
public class AngketKewajibanApi {

	public static JSONObject cekKewajiban(HttpServletRequest req, JSONObject request) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, req);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
				return jsonObject;
			}

			Object[] hasil = null;
			try {
				hasil = cekDosenGuruDariJadwalUmum(tbmuser);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			if (hasil == null) {
				try {
					hasil = cekGrup(tbmuser);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (hasil == null) {
				try {
					hasil = cekUmum(tbmuser);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (hasil == null) {
				jsonObject.put("wajibIsi", false);
			} else {
				jsonObject.put("wajibIsi", true);
				jsonObject.put("jenis", hasil[0]);
				jsonObject.put("tahunAkademik", hasil[1]);
				jsonObject.put("semester", hasil[2]);
				jsonObject.put("pesan", hasil[3]);
			}

			jsonObject.put("status", "00");
			jsonObject.put("description", "OK");
		} catch (Exception e) {
			String err = Common.tampilErrorJikaAdmin(e);
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", err);
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}
		return jsonObject;
	}

	/**
	 * Cabang 1: angket dosen (dinilai mahasiswa) / guru (dinilai siswa) yang
	 * dijadwalkan lewat Jadwal Angket Umum -- persis
	 * {@code MainHelper.prosesAngketDosenGuruDariJadwalUmum}.
	 */
	private static Object[] cekDosenGuruDariJadwalUmum(Tbmuser tbmuser) {
		List<Object[]> datas = gabungkanJadwalAngketUmum(tbmuser);
		if (datas == null || datas.isEmpty()) {
			return null;
		}

		for (Object[] obj : datas) {
			String tahunAkademik = getStringData(obj, 0);
			String semester = getStringData(obj, 1);
			if (tahunAkademik == null || semester == null) {
				continue;
			}

			Mahasiswa mahasiswa = tbmuser.getMahasiswa();
			if (mahasiswa != null && mahasiswa.getId() != null && Boolean.TRUE.equals(ChecklistPenilaianHelper
					.adaJadwalAngketDosenDariJadwalUmum(tahunAkademik, semester, tbmuser))) {
				int currentSmt = Common.getSemester(mahasiswa.getTahunangkatan(), semester,
						mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
				if (Boolean.TRUE.equals(AngketUtil.checkStatusChecklist(mahasiswa, currentSmt, null))) {
					String pesan = "Penilaian angket dosen oleh mahasiswa untuk tahun akademik " + tahunAkademik
							+ " dan semester " + semester
							+ " sebagian atau semuanya belum Anda lakukan. Sebelum Anda bisa melanjutkan akses aplikasi akademik ini, mohon isilah terlebih dulu Angket Dosen berikut.";
					return new Object[] { "dosen", tahunAkademik, semester, pesan };
				}
			}

			Siswa siswa = tbmuser.getSiswa();
			if (siswa != null && siswa.getId() != null && Boolean.TRUE.equals(
					ChecklistPenilaianHelper.adaJadwalAngketGuruDariJadwalUmum(tahunAkademik, semester, tbmuser))) {
				if (ChecklistPenilaianGuruHelper.checkStatusChecklistGuru(siswa, semester, tahunAkademik)) {
					String pesan = "Penilaian angket guru oleh siswa untuk tahun akademik " + tahunAkademik
							+ " dan semester " + semester
							+ " sebagian atau semuanya belum Anda lakukan. Sebelum Anda bisa melanjutkan akses aplikasi akademik ini, mohon isilah terlebih dulu Angket Guru berikut.";
					return new Object[] { "guru", tahunAkademik, semester, pesan };
				}
			}
		}
		return null;
	}

	/** Cabang 2: grup kuesioner umum -- persis {@code MainHelper.prosesAngketSaatLoginGrup}. */
	private static Object[] cekGrup(Tbmuser tbmuser) {
		List<Object[]> datas = ChecklistPenilaianHelper.getJadwalChecklistUmumGrup(tbmuser);
		if (datas == null || datas.isEmpty()) {
			return null;
		}
		for (Object[] obj : datas) {
			String tahunAkademik = getStringData(obj, 0);
			String semester = getStringData(obj, 1);
			if (tahunAkademik == null || semester == null) {
				continue;
			}
			if (ChecklistPenilaianHelper.checkStatusChecklistUmumGrup(tahunAkademik, semester, tbmuser)) {
				return new Object[] { "grup", tahunAkademik, semester, pesanUmum(tahunAkademik, semester) };
			}
		}
		return null;
	}

	/** Cabang 3: angket umum -- persis {@code MainHelper.prosesAngketSaatLoginUmum}. */
	private static Object[] cekUmum(Tbmuser tbmuser) {
		List<Object[]> datas = ChecklistPenilaianHelper.getJadwalChecklistUmum(tbmuser);
		if (datas == null || datas.isEmpty()) {
			return null;
		}
		for (Object[] obj : datas) {
			String tahunAkademik = getStringData(obj, 0);
			String semester = getStringData(obj, 1);
			if (tahunAkademik == null || semester == null) {
				continue;
			}
			if (ChecklistPenilaianHelper.checkStatusChecklistUmum(tahunAkademik, semester, tbmuser)) {
				return new Object[] { "umum", tahunAkademik, semester, pesanUmum(tahunAkademik, semester) };
			}
		}
		return null;
	}

	private static String pesanUmum(String tahunAkademik, String semester) {
		return "Penilaian angket umum sebagian atau semuanya untuk tahun akademik " + tahunAkademik + " dan semester "
				+ semester
				+ " belum Anda lakukan. Sebelum Anda bisa melanjutkan akses aplikasi akademik ini, mohon isilah terlebih dulu Angket umum berikut.";
	}

	/**
	 * Gabungkan jadwal grup + jadwal umum (union by tahunAkademik+semester),
	 * persis {@code MainHelper.gabungkanJadwalAngketUmum}.
	 */
	private static List<Object[]> gabungkanJadwalAngketUmum(Tbmuser tbmuser) {
		List<Object[]> rows = new ArrayList<Object[]>();
		Set<String> keySet = new HashSet<String>();
		tambahJadwalAngket(rows, keySet, ChecklistPenilaianHelper.getJadwalChecklistUmumGrup(tbmuser));
		tambahJadwalAngket(rows, keySet, ChecklistPenilaianHelper.getJadwalChecklistUmum(tbmuser));
		return rows;
	}

	private static void tambahJadwalAngket(List<Object[]> rows, Set<String> keySet, List<Object[]> source) {
		if (source == null || source.isEmpty()) {
			return;
		}
		for (Object[] obj : source) {
			String tahunAkademik = getStringData(obj, 0);
			String semester = getStringData(obj, 1);
			if (tahunAkademik == null || semester == null) {
				continue;
			}
			String key = tahunAkademik + "|" + semester;
			if (!keySet.contains(key)) {
				keySet.add(key);
				rows.add(obj);
			}
		}
	}

	private static String getStringData(Object[] data, int index) {
		if (data == null || data.length <= index || data[index] == null) {
			return null;
		}
		String value = data[index].toString();
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}
}
