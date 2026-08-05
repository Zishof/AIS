package ais.action.master.feeder.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.Kegiatan;
import ais.database.model.KelompokMatakuliah;
import ais.database.model.KelompokMatakuliahPunyaMatakuliah;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NilaiHuruf;
import ais.database.model.NilaiHurufExport;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.StatusMatakuliah;

public class FeederExporterGenerator {

	public static JSONObject dosen_pt(PenugasanDosenMengajar penugasanDosenMengajar, Session session) {

		Dosen dosen = penugasanDosenMengajar.getDosen();

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id_sdm", dosen.getFeeder());
		data.put("id_sp", dosen.getPerguruanTinggi().getFeeder());
		data.put("id_thn_ajaran", penugasanDosenMengajar.getTahun());
		data.put("id_sms", penugasanDosenMengajar.getJurusan().getFeeder());
		data.put("no_srt_tgs", penugasanDosenMengajar.getKode());

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
					penugasanDosenMengajar.getJurusan().getId().equals(dosen.getJurusan().getId()) ? "1" : "0");
		}

		data.put("a_aktif_bln_1", bulan == 1 ? "1" : "0");
		data.put("a_aktif_bln_2", bulan == 2 ? "1" : "0");
		data.put("a_aktif_bln_3", bulan == 3 ? "1" : "0");
		data.put("a_aktif_bln_4", bulan == 4 ? "1" : "0");
		data.put("a_aktif_bln_5", bulan == 5 ? "1" : "0");
		data.put("a_aktif_bln_6", bulan == 6 ? "1" : "0");
		data.put("a_aktif_bln_7", bulan == 7 ? "1" : "0");
		data.put("a_aktif_bln_8", bulan == 8 ? "1" : "0");
		data.put("a_aktif_bln_9", bulan == 9 ? "1" : "0");
		data.put("a_aktif_bln_10", bulan == 10 ? "1" : "0");
		data.put("a_aktif_bln_11", bulan == 11 ? "1" : "0");
		data.put("a_aktif_bln_12", bulan == 12 ? "1" : "0");

		data.put("id_jns_keluar", 1);
		data.put("tgl_ptk_keluar", Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()));

		if (dosen.getTanggallahir() != null)
			data.put("tgl_lahir", Common.databaseDateFormat.get().format(dosen.getTanggallahir()));

		data.put("nm_sdm", dosen.getNama());
		data.put("tmpt_lahir", dosen.getTempatlahir());

		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	public static JSONObject ajar_dosen(Dosen dosen, Perkuliahan perkuliahan, Integer jumlahPertemuan) {
		Matakuliah matakuliah = perkuliahan.getMatakuliah();
		Map<String, Object> data = new HashMap<String, Object>();
		if (dosen != null && dosen.getIdRegPtk() != null && !dosen.getIdRegPtk().trim().isEmpty()) {
			data.put("id_registrasi_dosen", dosen.getIdRegPtk().trim());
		}
		data.put("id_kelas_kuliah", perkuliahan.getFeeder());
		data.put("sks_substansi_total",
				matakuliah.getSks() > 0
						? (matakuliah.getSks().doubleValue() / perkuliahan.getJumlahDosen().doubleValue()) + ""
						: "0");
		data.put("sks_tm_subst",
				matakuliah.getSksDiskusi() > 0
						? (matakuliah.getSksDiskusi().doubleValue() / perkuliahan.getJumlahDosen().doubleValue()) + ""
						: "0");
		data.put("sks_prak_subst",
				matakuliah.getSksPraktek() > 0
						? (matakuliah.getSksPraktek().doubleValue() / perkuliahan.getJumlahDosen().doubleValue()) + ""
						: "0");
		data.put("sks_prak_lap_subst", matakuliah.getSksPraktekLapangan() > 0
				? (matakuliah.getSksPraktekLapangan().doubleValue() / perkuliahan.getJumlahDosen().doubleValue()) + ""
				: "0");
		data.put("sks_sim_subst",
				matakuliah.getSksSimulasi() > 0
						? (matakuliah.getSksSimulasi().doubleValue() / perkuliahan.getJumlahDosen().doubleValue()) + ""
						: "0");
		data.put("rencana_minggu_pertemuan", (jumlahPertemuan / perkuliahan.getJumlahDosen()) + "");
		data.put("realisasi_minggu_pertemuan", (jumlahPertemuan / perkuliahan.getJumlahDosen()) + "");
		data.put("id_jenis_evaluasi",
				perkuliahan.getJenisEvaluasi() == null ? 1 : perkuliahan.getJenisEvaluasi().getFeeder());

		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static JSONObject mahasiswa_pt(Mahasiswa mahasiswa, BiodataMahasiswa biodataMahasiswa, Session session) {

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id_sms", mahasiswa.getJurusan().getFeeder());
		data.put("id_pd", mahasiswa.getFeeder());

		data.put("id_sp", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFeeder());

		if (mahasiswa.getStatusAwalMahasiswa() != null) {
			data.put("id_jns_daftar", mahasiswa.getStatusAwalMahasiswa().getFeeder());
		}

		if (mahasiswa.getTanggalMasuk() != null) {
			data.put("tgl_masuk_sp", Common.databaseDateFormat.get().format(mahasiswa.getTanggalMasuk()));
		}

		data.put("id_pembiayaan", mahasiswa.getJenisPembiayaanMahasiswa().getFeeder());

		// biaya_masuk_kuliah

		try {

			BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) session
					.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();
			if (calonMahasiswa != null) {
				ArrayList detailBiayas = new ArrayList();
				java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(
						calonMahasiswa, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, mahasiswa.getJurusan(), 1,
						false);
				detailBiayas.addAll(detailBiayas1);

				int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, calonMahasiswa,
						ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, 1, detailBiayas, false, false);

				Collection biayaBulanan = null;
				if (countPengaturanBulanan > 0) {
					biayaBulanan = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(calonMahasiswa, session, 1,
							ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, detailBiayas, false, false);
				}
				Collection dataBTagihan = biayaBulanan != null ? biayaBulanan : detailBiayas;
				Double biaya = 0.0;
				for (Object o : dataBTagihan) {
					Kegiatan kegiatan = calonMahasiswa.ambilKegiatans(1,
							ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
					if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
						Double jumlah = pengaturanPembayaranBulanan.getNominal();
						biaya += jumlah;
					} else if (o instanceof DetailBiaya) {
						DetailBiaya detailBiaya = (DetailBiaya) o;

						Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
						biaya += jumlah;
					}
				}
				data.put("biaya_masuk_kuliah", biaya);
			} else {
				int semester = 1;
				Collection detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
						ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

				int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa,
						ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, semester, detailBiayas, false, false);

				if (countPengaturanBulanan > 0) {

					detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
							ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, countPengaturanBulanan > 0 ? "-1" : null, true,
							false);

				}

				if (!detailBiayas.isEmpty()) {
					Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
					Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
							: kegiatan.ambilDetailKegiatan(true);
					Double biaya = 0.0;
					for (Object o : detailBiayas) {
						if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
							Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa, semester,
									pengaturanPembayaranBulanan);
							biaya += jumlah;
						} else if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya = (DetailBiaya) o;

							Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
							biaya += jumlah;
						}
					}
					data.put("biaya_masuk_kuliah", biaya);
				} else {
					data.put("biaya_masuk_kuliah", 0.0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederExporterGenerator.java:234");
			data.put("biaya_masuk_kuliah", 0.0);
		}

		if (mahasiswa.getStatusKeluar() != null) {
			data.put("id_jns_keluar", mahasiswa.getStatusKeluar().getFeeder());
		}

		if (mahasiswa.getTanggalLulus() != null) {
			data.put("tgl_keluar", Common.databaseDateFormat.get().format(mahasiswa.getTanggalLulus()));
		}

		data.put("a_pernah_paud", biodataMahasiswa != null && biodataMahasiswa.getApakahPernahPaud() ? "1" : "0");
		data.put("a_pernah_tk", biodataMahasiswa != null && biodataMahasiswa.getApakahPernahTk() ? "1" : "0");

		if (mahasiswa.getTahunangkatan() != null) {
			data.put("mulai_smt", mahasiswa.getTahunangkatan()
					+ (mahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL) ? "1" : "2"));
		}

		data.put("judul_skripsi", Common.maxPanjang(mahasiswa.getJudulSkripsi(), 250));

		if (mahasiswa.getBlnAwalBimbingan() != null) {
			data.put("bln_awal_bimbingan", Common.databaseDateFormat.get().format(mahasiswa.getBlnAwalBimbingan()));
		}

		if (mahasiswa.getBlnAkhirBimbingan() != null) {
			data.put("bln_akhir_bimbingan", Common.databaseDateFormat.get().format(mahasiswa.getBlnAkhirBimbingan()));
		}

		data.put("sk_yudisium", Common.maxPanjang(mahasiswa.getNoAkta1(), 30));

		if (mahasiswa.getTanggalYudisium() != null) {
			data.put("tgl_sk_yudisium", Common.databaseDateFormat.get().format(mahasiswa.getTanggalYudisium()));
		}

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
		if (krsMahasiswa != null && krsMahasiswa.getIpk() != null) {
			data.put("ipk", krsMahasiswa.getIpk());
		}

		data.put("no_seri_ijazah", Common.maxPanjang(mahasiswa.getNoIjazah1(), 40));

		data.put("nm_pt_asal", Common.maxPanjang(mahasiswa.getPindahanDariKampus(), 50));

		data.put("nm_prodi_asal", Common.maxPanjang(mahasiswa.getNamaProdiPindah(), 50));

		if (mahasiswa.getMerupakanPindahan()) {
			data.put("sks_diakui", mahasiswa.getSksYangDiakui());
		} else if (mahasiswa.getMerupakanAlihProdi()) {
			data.put("sks_diakui", mahasiswa.getSksYangDiakuiPindahProdi());
		}

		if (mahasiswa.getIdRegPd() == null || mahasiswa.getIdRegPd().trim().isEmpty()) {
			data.put("nipd", Common.maxPanjang(mahasiswa.getNim(), 18));
		}

		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	public static JSONObject mahasiswa(Mahasiswa mahasiswa, BiodataMahasiswa biodataMahasiswa) {

		Map<String, Object> data = new HashMap<String, Object>();

		data.put("nm_pd", Common.maxPanjang(mahasiswa.getNama(), 50));
		data.put("tmpt_lahir", Common.maxPanjang(mahasiswa.getTempatlahir(), 20));

		if (mahasiswa.getTanggallahir() != null) {
			data.put("tgl_lahir", Common.databaseDateFormat.get().format(mahasiswa.getTanggallahir()));
		}

		data.put("jk", mahasiswa.getKelamin() == null ? "*"
				: mahasiswa.getKelamin().trim().equalsIgnoreCase("Laki-laki") ? "L" : "P");

		data.put("nik", Common.maxPanjang(biodataMahasiswa.getNoIdentitas(), 16));

		data.put("nisn", Common.maxPanjang(biodataMahasiswa.getNisn(), 16));
		data.put("npwp", Common.maxPanjang(biodataMahasiswa.getNpwp(), 16));

		if (biodataMahasiswa.getAgama() != null) {
			data.put("id_agama", biodataMahasiswa.getAgama().getFeeder());
		} else if (mahasiswa.getAgama() != null) {
			data.put("id_agama", mahasiswa.getAgama().getFeeder());
		} else {
			data.put("id_agama", 98);
		}

		data.put("id_sp", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFeeder());

		data.put("jln", Common.maxPanjang(mahasiswa.getAlamat(), 80));

		data.put("rt", Common.maxPanjangNumeric(biodataMahasiswa.getRt(), 2));

		data.put("rw", Common.maxPanjangNumeric(biodataMahasiswa.getRw(), 2));
		data.put("nm_dsn", Common.maxPanjang(biodataMahasiswa.getDusun(), 40));
		data.put("ds_kel", Common.maxPanjang(biodataMahasiswa.getKelurahan(), 40));

		if (biodataMahasiswa.getKecamatan() != null && biodataMahasiswa.getKecamatan().getFeeder() != null
				&& !biodataMahasiswa.getKecamatan().getFeeder().trim().isEmpty()) {
			data.put("id_wil", biodataMahasiswa.getKecamatan().getFeeder());
		} else {
			data.put("id_wil", "000000");
		}
		data.put("kode_pos", Common.maxPanjang(biodataMahasiswa.getKodepos(), 5));

		if (biodataMahasiswa.getJenisTinggalMahasiswa() != null
				&& biodataMahasiswa.getJenisTinggalMahasiswa().getFeeder() != null) {
			data.put("id_jns_tinggal", biodataMahasiswa.getJenisTinggalMahasiswa().getFeeder());
		}

		if (biodataMahasiswa.getAlatTransportasiMahasiswa() != null
				&& biodataMahasiswa.getAlatTransportasiMahasiswa().getFeeder() != null) {
			data.put("id_alat_transport", biodataMahasiswa.getAlatTransportasiMahasiswa().getFeeder());
		}

		data.put("telepon_rumah", Common.maxPanjang(biodataMahasiswa.getTeleponRumah(), 20));
		data.put("telepon_seluler", Common.maxPanjang(biodataMahasiswa.getHp(), 20));
		data.put("email", Common.maxPanjang(biodataMahasiswa.getEmail().split(",")[0], 50));
		HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
				.currentStatus(mahasiswa);

		if (historyStatusMahasiswa != null && historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed() != null
				&& (historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim().equalsIgnoreCase("A")
						|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim().equalsIgnoreCase("C")
						|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim().equalsIgnoreCase("D")
						|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim().equalsIgnoreCase("L")
						|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim().equalsIgnoreCase("P")
						|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim().equalsIgnoreCase("N")
						|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim().equalsIgnoreCase("G")
						|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim().equalsIgnoreCase("A")
						|| historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed().trim().equalsIgnoreCase("K"))

		) {
			data.put("stat_pd", historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed());
		} else {
			data.put("stat_pd", "A");
		}

		data.put("nik_ayah", Common.maxPanjang(biodataMahasiswa.getNikAyah(), 16));
		data.put("nm_ayah", Common.maxPanjang(biodataMahasiswa.getNamaAyah(), 50));
		if (biodataMahasiswa.getTanggalLahirAyah() != null) {
			data.put("tgl_lahir_ayah", Common.databaseDateFormat.get().format(biodataMahasiswa.getTanggalLahirAyah()));
		}

		if (biodataMahasiswa.getJenjangPendidikanAyah() != null
				&& biodataMahasiswa.getJenjangPendidikanAyah().getFeeder() != null) {
			data.put("id_jenjang_pendidikan_ayah", biodataMahasiswa.getJenjangPendidikanAyah().getFeeder());
		}

		if (biodataMahasiswa.getJenisPekerjaanAyah() != null
				&& biodataMahasiswa.getJenisPekerjaanAyah().getFeeder() != null) {
			data.put("id_pekerjaan_ayah", biodataMahasiswa.getJenisPekerjaanAyah().getFeeder());
		}

		if (biodataMahasiswa.getJenisPenghasilanAyah() != null
				&& biodataMahasiswa.getJenisPenghasilanAyah().getFeeder() != null) {
			data.put("id_penghasilan_ayah", biodataMahasiswa.getJenisPenghasilanAyah().getFeeder());
		}

		data.put("nik_ibu", Common.maxPanjang(biodataMahasiswa.getNikAyah(), 16));
		data.put("nm_ibu_kandung", Common.maxPanjang(biodataMahasiswa.getNamaIbu(), 50));
		if (biodataMahasiswa.getTanggalLahirIbu() != null) {
			data.put("tgl_lahir_ibu", Common.databaseDateFormat.get().format(biodataMahasiswa.getTanggalLahirIbu()));
		}

		if (biodataMahasiswa.getJenjangPendidikanIbu() != null
				&& biodataMahasiswa.getJenjangPendidikanIbu().getFeeder() != null) {
			data.put("id_jenjang_pendidikan_ibu", biodataMahasiswa.getJenjangPendidikanIbu().getFeeder());
		}

		if (biodataMahasiswa.getJenisPekerjaanIbu() != null
				&& biodataMahasiswa.getJenisPekerjaanIbu().getFeeder() != null) {
			data.put("id_pekerjaan_ibu", biodataMahasiswa.getJenisPekerjaanIbu().getFeeder());
		}

		if (biodataMahasiswa.getJenisPenghasilanIbu() != null
				&& biodataMahasiswa.getJenisPenghasilanIbu().getFeeder() != null) {
			data.put("id_penghasilan_ibu", biodataMahasiswa.getJenisPenghasilanIbu().getFeeder());
		}

		data.put("nm_wali", Common.maxPanjang(biodataMahasiswa.getNamaWali(), 30));
		if (biodataMahasiswa.getTanggalLahirWali() != null) {
			data.put("tgl_lahir_wali", Common.databaseDateFormat.get().format(biodataMahasiswa.getTanggalLahirWali()));
		}

		if (biodataMahasiswa.getJenjangPendidikanWali() != null
				&& biodataMahasiswa.getJenjangPendidikanWali().getFeeder() != null) {
			data.put("id_jenjang_pendidikan_wali", biodataMahasiswa.getJenjangPendidikanWali().getFeeder());
		}

		if (biodataMahasiswa.getJenisPekerjaanWali() != null
				&& biodataMahasiswa.getJenisPekerjaanWali().getFeeder() != null) {
			data.put("id_pekerjaan_wali", biodataMahasiswa.getJenisPekerjaanWali().getFeeder());
		}

		if (biodataMahasiswa.getJenisPenghasilanWali() != null
				&& biodataMahasiswa.getJenisPenghasilanWali().getFeeder() != null) {
			data.put("id_penghasilan_wali", biodataMahasiswa.getJenisPenghasilanWali().getFeeder());
		}

		data.put("kewarganegaraan",
				mahasiswa.getNegara() == null || mahasiswa.getNegara().getNama() == null
						|| mahasiswa.getNegara().getNama().trim().equalsIgnoreCase("Indonesia") ? "ID"
								: mahasiswa.getNegara().getKode());

		// if (mahasiswa.getFeeder() == null
		// || mahasiswa.getFeeder().trim().isEmpty()) {
		data.put("a_terima_kps", 0);
		data.put("id_kebutuhan_khusus_ayah", 0);
		data.put("id_kebutuhan_khusus_ibu", 0);
		data.put("id_kk", 0);

		// }
		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	public static JSONObject perkuliahan(Perkuliahan perkuliahan) {

		Matakuliah matakuliah = perkuliahan.getMatakuliah();
		String id_smt = perkuliahan.getTahunAjaran().split("/")[0] + (perkuliahan.getStatusSemesterPendek() != null
				&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK) ? "3"
						: (perkuliahan.getGanjilGenap().equalsIgnoreCase(Perkuliahan.GENAP) ? "2" : "1"));

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id_semester", id_smt);
		data.put("id_matkul", matakuliah.getFeeder());
		data.put("id_prodi", perkuliahan.getJurusan().getFeeder());

		if (Common.bolehKonfigurasi("kelas_digabung_dengan_semester_saat_export_feeder_tanpa_spasi", Konfigurasi.TIDAK_AKTIF)) {
			data.put("nama_kelas_kuliah",
					Common.maxPanjangAkhir(perkuliahan.getSemester() + "" + perkuliahan.getKelas(), 5));
		} else if (Common.bolehKonfigurasi("kelas_digabung_dengan_semester_saat_export_feeder", Konfigurasi.TIDAK_AKTIF)) {
			data.put("nama_kelas_kuliah",
					Common.maxPanjangAkhir(perkuliahan.getSemester() + " " + perkuliahan.getKelas(), 5));
		} else {
			data.put("nama_kelas_kuliah", Common.maxPanjangAkhir(perkuliahan.getKelas(), 5));
		}

		data.put("sks_mata_kuliah", matakuliah.getSks());
		data.put("sks_tatap_muka", matakuliah.getSksDiskusi());
		data.put("sks_praktek", matakuliah.getSksPraktek());
		data.put("sks_praktek_lapangan", matakuliah.getSksPraktekLapangan());
		data.put("sks_simulasi", matakuliah.getSksSimulasi());
		data.put("bahasan", Common.maxPanjang(matakuliah.getMetodeKuliah(), 20));

		data.put("kapasitas", perkuliahan.getKapasitasKelas() + "");
		data.put("a_selenggara_pditt", 1);
		data.put("apa_untuk_pditt", 0);
		data.put("lingkup", perkuliahan.getLingkup());
		data.put("mode", perkuliahan.getMode());

		if (perkuliahan.getPerkuliahanDimulai() != null) {
			data.put("tanggal_mulai_efektif", Common.databaseDateFormat.get().format(perkuliahan.getPerkuliahanDimulai()));
		}
		if (perkuliahan.getPerkuliahanSampai() != null) {
			data.put("tanggal_akhir_efektif", Common.databaseDateFormat.get().format(perkuliahan.getPerkuliahanSampai()));
		}
		if (perkuliahan.getPerkuliahanSampai() != null) {
			data.put("tanggal_tutup_daftar", Common.databaseDateFormat.get().format(perkuliahan.getPerkuliahanSampai()));
		}
		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	public static JSONObject formatNilai(FormatNilai formatNilai, Integer nomor_urut) {

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id_kelas_kuliah", formatNilai.getPerkuliahan().getFeeder());
		if (formatNilai.getJenisEvaluasi() != null && formatNilai.getJenisEvaluasi().getFeeder() != null) {
			data.put("id_jenis_evaluasi", formatNilai.getJenisEvaluasi().getFeeder().toString());
		}
		data.put("nama", formatNilai.getNama());
		data.put("nama_inggris", formatNilai.getNama());

		data.put("nomor_urut", nomor_urut);
		data.put("bobot_evaluasi", formatNilai.getPersen() + "");
		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	public static JSONObject perkuliahan_dosen(Perkuliahan perkuliahan, Dosen dosen, Session session) {

		Matakuliah matakuliah = perkuliahan.getMatakuliah();

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id_reg_ptk", dosen.getIdRegPtk());
		data.put("id_kls", perkuliahan.getFeeder());
		data.put("sks_subst_tot", matakuliah.getSks());
		data.put("sks_tm_subst", matakuliah.getSksDiskusi());
		data.put("sks_prak_subst", matakuliah.getSksPraktek());

		data.put("sks_prak_lap_subst", matakuliah.getSksPraktekLapangan());
		data.put("sks_sim_subst", matakuliah.getSksSimulasi());

		data.put("jml_tm_renc", perkuliahan.getJumlahMaksimalPertemuan());

		int jml_tm_real = ((Number) session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("perkuliahan", perkuliahan)).setProjection(Projections.rowCount()).uniqueResult())
				.intValue();

		data.put("jml_tm_real", jml_tm_real);

		data.put("id_jns_eval", 1);
		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	public static JSONObject kurikulumPunyaMatakuliah(KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah) {

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id_kurikulum", kurikulumPunyaMatakuliah.getKurikulum().getFeeder());
		data.put("id_matkul", kurikulumPunyaMatakuliah.getMatakuliah().getFeeder());

		data.put("sks_mata_kuliah", kurikulumPunyaMatakuliah.getMatakuliah().getSks() + "");
		data.put("sks_tatap_muka", kurikulumPunyaMatakuliah.getMatakuliah().getSksDiskusi() + "");
		data.put("sks_praktek", kurikulumPunyaMatakuliah.getMatakuliah().getSksPraktek() + "");
		data.put("sks_praktek_lapangan", kurikulumPunyaMatakuliah.getMatakuliah().getSksPraktekLapangan() + "");
		data.put("sks_simulasi", kurikulumPunyaMatakuliah.getMatakuliah().getSksSimulasi() + "");
		data.put("apakah_wajib",
				kurikulumPunyaMatakuliah.getMatakuliah().getStatus().equalsIgnoreCase("Wajib") ? "1" : "0");
		data.put("semester", kurikulumPunyaMatakuliah.getSemester() + "");

		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	public static JSONObject kurikulum(Kurikulum kurikulum) {

		Map<String, Object> data = new HashMap<String, Object>();

		data.put("id_semester",
				kurikulum.getTahun() + (kurikulum.getJenisSemester().equals(Perkuliahan.GANJIL) ? "1" : "2"));

		data.put("nama_kurikulum", kurikulum.getNama());
		data.put("id_prodi", kurikulum.getJurusan().getFeeder());
		data.put("jumlah_sks_wajib", kurikulum.getJumlahAturanSksWajib());
		data.put("jumlah_sks_pilihan", kurikulum.getJumlahAturanSksPilihan());
		data.put("jumlah_sks_lulus", kurikulum.getJumlahAturanSksLulus());

		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	public static JSONObject nilaiHuruf(NilaiHurufExport nilaiHurufExport) {

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 10);

		NilaiHuruf nilaiHuruf = nilaiHurufExport.getNilaiHuruf();
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("nilai_huruf", nilaiHuruf.getNilaiHuruf());
		data.put("bobot_nilai_min", nilaiHuruf.getMulai());
		data.put("id_sms", nilaiHurufExport.getJurusan().getFeeder());
		data.put("bobot_nilai_maks", nilaiHuruf.getSampai());
		data.put("nilai_indeks", nilaiHuruf.getNilaiDiIPK());
		data.put("tgl_mulai_efektif", Common.databaseDateFormat.get().format(nilaiHuruf.getTanggalMulaiBerlaku()));
		data.put("tgl_selesai_efektif", Common.databaseDateFormat.get().format(calendar.getTime()));

		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject matakuliah(Matakuliah matakuliah) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("nama_mata_kuliah", matakuliah.getNama());
		data.put("kode_mata_kuliah", matakuliah.getKode());
		data.put("id_prodi", matakuliah.getJurusan().getFeeder());
//		data.put("id_jenis_mata_kuliah", "A");
		data.put("sks_mata_kuliah", matakuliah.getSks() + "");
		data.put("sks_tatap_muka", matakuliah.getSksDiskusi() + "");
		data.put("sks_praktek", matakuliah.getSksPraktek() + "");
		data.put("sks_praktek_lapangan", matakuliah.getSksPraktekLapangan() + "");
		data.put("sks_simulasi", matakuliah.getSksSimulasi() + "");
		data.put("metode_kuliah", matakuliah.getMetodeKuliah());

		data.put("ada_sap", matakuliah.getAdaSap() ? "1" : "0");
		data.put("ada_silabus", matakuliah.getAdaSilabus() ? "1" : "0");
		data.put("ada_bahan_ajar", matakuliah.getAdaBahanAjar() ? "1" : "0");
		data.put("ada_acara_praktek", matakuliah.getAdaAcaraPraktek() ? "1" : "0");
		data.put("ada_diktat", matakuliah.getAdaDiktat() ? "1" : "0");

		if (matakuliah.getTanggalMulai() != null) {
			data.put("tanggal_mulai_efektif", Common.dateFormat1.get().format(matakuliah.getTanggalMulai()));
		}
		if (matakuliah.getTanggalSampai() != null) {
			data.put("tanggal_akhir_efektif", Common.dateFormat1.get().format(matakuliah.getTanggalSampai()));
		}

		String id_jenis_mata_kuliah = null;
		if (matakuliah.getStatus() != null) {
			Map<Long, StatusMatakuliah> map = ConstantValues.ambilBerdasarClass(StatusMatakuliah.class);
			for (StatusMatakuliah statusMatakuliah : map.values()) {
				if (statusMatakuliah.getNama() != null
						&& statusMatakuliah.getNama().equalsIgnoreCase(matakuliah.getStatus())) {
					id_jenis_mata_kuliah = statusMatakuliah.getKode();
				}
			}
		}

		data.put("id_jenis_mata_kuliah", id_jenis_mata_kuliah);

		Session session = HibernateUtil.currentNativeSession();
		KelompokMatakuliah mykelompokMatakuliah = matakuliah.getKelompokMatakuliah() != null
				? matakuliah.getKelompokMatakuliah()
				: (matakuliah.getId() == null ? null
						: ((KelompokMatakuliah) ConstantValues.simpleObject(
								session.createCriteria(KelompokMatakuliahPunyaMatakuliah.class)
										.addOrder(Order.desc("id")).add(Restrictions.eq("matakuliah", matakuliah))
										.createAlias("kelompokMatakuliah", "kelompokMatakuliah")
										.add(Restrictions.or(Restrictions.isNull("kelompokMatakuliah.aktif"),
												Restrictions.eq("kelompokMatakuliah.aktif", true)))
										.setProjection(Projections.property("kelompokMatakuliah.id")).setMaxResults(1),
								KelompokMatakuliah.class, false)));
		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}

		if (mykelompokMatakuliah != null && mykelompokMatakuliah.getFeeder() != null) {
			data.put("id_kelompok_mata_kuliah", mykelompokMatakuliah.getFeeder());
		}

		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	public static JSONObject krs(Detailperkuliahan detailperkuliahan) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id_kls", detailperkuliahan.getPerkuliahan().getFeeder());
		data.put("id_reg_pd", detailperkuliahan.getMahasiswa().getIdRegPd());

		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	public static JSONObject nilai(Detailperkuliahan detailperkuliahan) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id_kls", detailperkuliahan.getPerkuliahan().getFeeder());
		data.put("id_reg_pd", detailperkuliahan.getMahasiswa().getIdRegPd());

		if (detailperkuliahan.getTotalNilai() > 0.1) {

			if (detailperkuliahan.getPerkuliahan() != null
					&& detailperkuliahan.getPerkuliahan().getSembunyikanNilaiJikaBelumDiverifikasi()
					&& detailperkuliahan.getVerify().equals(Detailperkuliahan.NOT_VERIFIED)) {
				data.put("nilai_angka", detailperkuliahan.getTotalNilaiSementara());
				data.put("nilai_huruf", detailperkuliahan.getNilaiHurufSementara());
				data.put("nilai_indeks", detailperkuliahan.getTotalIPSementara());
			} else {
				data.put("nilai_angka", detailperkuliahan.getTotalNilai());
				data.put("nilai_huruf", detailperkuliahan.getNilaiHuruf());
				data.put("nilai_indeks", detailperkuliahan.getTotalIP());
			}
		}

		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	public static JSONObject nilai_transfer(Detailperkuliahan detailperkuliahan) {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id_matkul", detailperkuliahan.getMatakuliahKonversi().getFeeder());
		data.put("id_registrasi_mahasiswa", detailperkuliahan.getMahasiswa().getIdRegPd());
		data.put("sks_mata_kuliah_diakui", detailperkuliahan.getMatakuliahKonversi().getSks().toString());
		data.put("nilai_huruf_diakui", detailperkuliahan.getNilaiHuruf());
		data.put("nilai_angka_diakui", detailperkuliahan.getTotalIP().toString());

		data.put("sks_mata_kuliah_asal", detailperkuliahan.getSksAsal().toString());
		data.put("nilai_huruf_asal", detailperkuliahan.getNilaiHurufAsal());
		data.put("kode_mata_kuliah_asal", detailperkuliahan.getKodeMatakuliahAsal());
		data.put("nama_mata_kuliah_asal", detailperkuliahan.getNamaMatakuliahAsal());

		data.put("id_prodi", detailperkuliahan.getMahasiswa().getJurusan().getFeeder());

//		String id_smt = detailperkuliahan.getTahunAkademik().split("/")[0]
//				+ (detailperkuliahan.getSemester() == 0 ? "1" : (detailperkuliahan.getSemester() % 2 == 0 ? "2" : "1"));

		String id_smt = detailperkuliahan.getMahasiswa().getTahunangkatan() + ""
				+ (detailperkuliahan.getMahasiswa().getSemesterMulai().equals(Perkuliahan.GANJIL) ? "1" : "2");

		if (detailperkuliahan.getSemester() > 0) {
			id_smt = detailperkuliahan.getTahunAkademik().split("/")[0]
					+ (detailperkuliahan.getSemester() % 2 == 0 ? "2" : "1");
		}

		data.put("id_semester", id_smt);

		data.put("id_perguruan_tinggi",
				detailperkuliahan.getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi().getFeeder());

		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	/**
	 * Menghitung digit ganjil/genap (atau {@code "3"} untuk semester pendek) untuk {@code id_semester}
	 * Neo Feeder, <b>berdasarkan SEMESTER MASUK mahasiswa</b> — bukan sekadar {@code semester % 2}.
	 *
	 * <p>Mahasiswa yang <b>masuk pada semester GENAP</b> memiliki pemetaan terbalik: semester ganjil
	 * (1, 3, 5, ...) sebenarnya jatuh pada periode <b>GENAP</b> ({@code "2"}), dan semester genap
	 * (2, 4, ...) jatuh pada periode GANJIL ({@code "1"}). Tanpa penyesuaian ini, mahasiswa angkatan
	 * genap (mis. masuk 2025/2026 Genap tetapi di-set semester 1) akan salah dipetakan ke periode
	 * ganjil ({@code 20251}) sehingga Neo Feeder menolak dengan error <i>"di luar periode"</i> —
	 * padahal periode masuk mahasiswa di Feeder adalah {@code 20252}.</p>
	 *
	 * <p>Logika ini menyamakan perilaku dengan {@link #kuliah_mahasiswa} (ekspor AKM) yang memang
	 * sudah memperhitungkan {@code getSemesterMulai()}, sehingga ekspor Dosen PA / aktivitas KRS,
	 * skripsi, dan tugas akhir konsisten.</p>
	 *
	 * @param mahasiswa      mahasiswa terkait (dibaca {@code getSemesterMulai()}); {@code null} dianggap masuk ganjil
	 * @param semester       nomor semester mahasiswa (1..n); {@code null} dianggap 1
	 * @param semesterPendek penanda semester pendek; bila non-null hasilnya {@code "3"}
	 * @return {@code "1"} (ganjil) / {@code "2"} (genap) / {@code "3"} (pendek)
	 */
	public static String digitPeriodeFeeder(Mahasiswa mahasiswa, Integer semester, Integer semesterPendek) {
		if (semesterPendek != null) {
			return "3";
		}
		int smt = (semester == null) ? 1 : semester.intValue();
		boolean mulaiGenap = mahasiswa != null && mahasiswa.getSemesterMulai() != null
				&& mahasiswa.getSemesterMulai().equalsIgnoreCase(Perkuliahan.GENAP);
		if (mulaiGenap) {
			return (smt % 2 == 0) ? "1" : "2";
		}
		return (smt % 2 == 0) ? "2" : "1";
	}

	public static JSONObject kuliah_mahasiswa(Session session, Mahasiswa mahasiswa, Integer semester,
			Integer semesterPendek, KrsMahasiswa krsMahasiswa) {
		Integer semesterMulai = mahasiswa.getPindahKeKampusIniMasukSemester();
		int tahunAngkatanMhs = mahasiswa.getTahunangkatan();
		Integer tahunAkademikMulai = Common.getTahunAkademik(semester, tahunAngkatanMhs, semesterMulai,
				mahasiswa.getSemesterMulai());
		Double ipmhs = krsMahasiswa.getIps();
		Double ipkmhs = krsMahasiswa.getIpk();

		Integer sksmhss = krsMahasiswa.getSksBukanKonversi();
		Integer sksmhs = krsMahasiswa.getSksk();

		String id_smt = tahunAkademikMulai + (semesterPendek != null ? "3"
				: (mahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL) ? (semester % 2 == 0 ? "2" : "1")
						: (semester % 2 == 0 ? "1" : "2")));

		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id_smt", id_smt);
		data.put("id_reg_pd", mahasiswa.getIdRegPd());

		data.put("ips", ipmhs);
		data.put("sks_smt", sksmhss);
		data.put("ipk", ipkmhs);
		data.put("sks_total", sksmhs);

		try {
			Collection detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
					ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, false);

			int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa,
					ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, semester, detailBiayas, false, false);

			if (countPengaturanBulanan > 0) {

				detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
						ConstantValues.PENDAFTARAN_MAHASISWA_LAMA, countPengaturanBulanan > 0 ? "-1" : null, true,
						false);

			}

			Double biaya = 0.0;
			if (!detailBiayas.isEmpty()) {
				Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, ConstantValues.PENDAFTARAN_MAHASISWA_LAMA);
				Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
						: kegiatan.ambilDetailKegiatan(true);

				for (Object o : detailBiayas) {
					if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
						Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa, semester,
								pengaturanPembayaranBulanan);
						biaya += jumlah;
					} else if (o instanceof DetailBiaya) {
						DetailBiaya detailBiaya = (DetailBiaya) o;

						Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
						biaya += jumlah;
					}
				}
				data.put("biaya_smt", biaya);
			} else {
				data.put("biaya_smt", 0.0);
			}

			if (biaya < 0.01 && semester <= 1) {
				biaya = 0.0;
				BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) (mahasiswa
						.getBiodataCalonMahasiswa() == null ? null
								: ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(),
										mahasiswa.getBiodataCalonMahasiswa()));
				if (biodataCalonMahasiswa != null) {
					Kegiatan kegiatan = biodataCalonMahasiswa.ambilKegiatans(semester,
							ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
					Collection mydetailBiayas = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa,
							ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU,
							biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1()
									: biodataCalonMahasiswa.getProdiLulus(),
							semester, false);

					countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, biodataCalonMahasiswa,
							ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, semester, mydetailBiayas, false, true);
					if (countPengaturanBulanan > 0) {
						mydetailBiayas = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(
								biodataCalonMahasiswa, session, semester,
								ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, mydetailBiayas, false, true);
					}

					for (Object o : mydetailBiayas) {
						if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya = (DetailBiaya) o;
							biaya += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
							biaya += Kegiatan.ambilJumlahTagihan(kegiatan, null, null, semester,
									pengaturanPembayaranBulanan);
						}
					}
				}
			}
			data.put("biaya_smt", biaya);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/feeder/util/FeederExporterGenerator.java:868");
			data.put("biaya_smt", 0.0);
		}

		HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
				.getHistoryStatusMahasiswa(krsMahasiswa, false);

		if (historyStatusMahasiswa != null && historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed() != null) {
			data.put("id_stat_mhs", historyStatusMahasiswa.getStatusMahasiswa().getKodeEpsbed());
		} else {
			data.put("id_stat_mhs", "X");
		}

		JSONObject jsonObject = new JSONObject(data);
		return jsonObject;
	}
}
