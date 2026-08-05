package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyWindow;

public class LaporanKontrakPerkuliahan extends MyWindow {

	private Center center;

	private Perkuliahan perkuliahan;

	private Toolbar toolbar;

	private Tbmuser tbmuser;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	public LaporanKontrakPerkuliahan(Perkuliahan perkuliahan) {
		super();
		this.perkuliahan = perkuliahan;
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kontrak Perkuliahan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() {
		tbmuser = Common.getCurrentUser();
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "kontrak_perkuliahan", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);

			}
		}));

		onCetak(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("perkuliahan", perkuliahan == null || perkuliahan.getId() == null ? -1L : perkuliahan.getId());

		String ttd = null;
		Dosen kaprodi = perkuliahan == null || perkuliahan.getJurusan() == null ? null
				: perkuliahan.getJurusan().getKaprodi();
		if (kaprodi != null) {
			LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
			if (lam != null) {
				File file = lam.ambilFile();

				parameters.put("ttd_kaprodi", file == null ? "" : file.getAbsolutePath());
			}
		}
		System.out.println("ttd_kaprodi => " + ttd);
		String nama_dosen = "";
		String nidn_dosen = "";
		if (perkuliahan != null) {
			int d = 1;

			List<Dosen> dosens = perkuliahan.populateDosenBuNama();

			for (Dosen dosen : dosens) {
				nama_dosen += nama_dosen.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				nidn_dosen += nidn_dosen.isEmpty() ? dosen.getNidn() : ", " + dosen.getNidn();
				LampiranLain lam = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
				if (lam != null) {
					File file = lam.ambilFile();
					parameters.put("ttd_dosen_" + d, file == null ? "" : file.getAbsolutePath());
					parameters.put("ttd_dosen_id_" + dosen.getId(), file == null ? "" : file.getAbsolutePath());

					System.out.println("ttd_dosen_" + d + " => " + ttd);
				}
				d++;

			}

			if (kaprodi != null) {
				LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						ttd = lam.ambilFile().getAbsolutePath();

						parameters.put("ttd_dosen_" + d, ttd);
					}
				}
			}
			parameters.put("nama_pt", perkuliahan.getJurusan() == null || perkuliahan.getJurusan().getFakultas() == null
					|| perkuliahan.getJurusan().getFakultas().getPerguruanTinggi() == null
					|| perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama() == null ? ""
							: perkuliahan.getJurusan().getFakultas().getPerguruanTinggi().getNama().toUpperCase());
			parameters.put("kode_mk", perkuliahan.getMatakuliah().getKode());
			parameters.put("nama_mk", perkuliahan.getMatakuliah().getNama());
			parameters.put("jml_sks", perkuliahan.getMatakuliah().getSks());
			parameters.put("nama_dosen", nama_dosen);
			parameters.put("nidn_dosen", nidn_dosen);

			parameters.put("nama_kaprodi", perkuliahan.getJurusan().getKaprodi() == null ? ""
					: perkuliahan.getJurusan().getKaprodi().getNama());
			parameters.put("nidn_kaprodi", perkuliahan.getJurusan().getKaprodi() == null ? ""
					: perkuliahan.getJurusan().getKaprodi().getNidn());

			if (perkuliahan.getJurusan().getKaprodi() != null) {
				LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getKaprodi().getId(),
						LampiranLain.TTD_DOSEN);
				if (lam != null) {
					File file = lam.ambilFile();
					parameters.put("ttd_kaprodi", file == null ? "" : file.getAbsolutePath());
					parameters.put("ttd_kaprodi_id_" + perkuliahan.getJurusan().getKaprodi().getId(),
							file == null ? "" : file.getAbsolutePath());

					System.out.println("ttd_kaprodi_" + d + " => " + ttd);
				}
			}

			if (perkuliahan.getJurusan().getFakultas().getDekan() != null) {
				LampiranLain lam = LampiranLain.ambil(perkuliahan.getJurusan().getFakultas().getDekan().getId(),
						LampiranLain.TTD_DOSEN);
				if (lam != null) {
					File file = lam.ambilFile();
					parameters.put("ttd_dekan", file == null ? "" : file.getAbsolutePath());
					parameters.put("ttd_dekan_id_" + perkuliahan.getJurusan().getKaprodi().getId(),
							file == null ? "" : file.getAbsolutePath());

					System.out.println("ttd_dekan_" + d + " => " + ttd);
				}
			}
			parameters.put("tanggal_format", perkuliahan.getTanggalMulaiPerkuliahan() == null ? ""
					: Common.dateFormat6.get().format(perkuliahan.getTanggalMulaiPerkuliahan()));
			parameters.put("tanggal", perkuliahan.getTanggalMulaiPerkuliahan());
			parameters.put("waktu", (perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) + " sd "
					+ (perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()));

			parameters.put("materi", perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());
			parameters.put("catatan", perkuliahan.getKeterangan());

			parameters.put("tahun_ajaran", perkuliahan.getTahunAjaran());
			parameters.put("kelas", perkuliahan.getKelas());
			parameters.put("minimal_kehadiran", Common.numberFormat.get().format(perkuliahan.getPersenKehadiranDinilai0()));

			Common.insertProperty(Perkuliahan.class, perkuliahan, parameters, "");

			int indexDosen = 0;
			nama_dosen = "";
			nidn_dosen = "";
			for (Dosen dosen : dosens) {
				indexDosen++;
				nama_dosen += nama_dosen.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				nidn_dosen += nidn_dosen.isEmpty() ? dosen.getNidn() : ", " + dosen.getNidn();

				LampiranLain lam = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
				if (lam != null) {
					File file = lam.ambilFile();
					parameters.put("ttd_dosen_pertemuan_" + indexDosen, file == null ? "" : file.getAbsolutePath());
				}

			}

			parameters.put("nama_dosen", nama_dosen);
			parameters.put("nidn_dosen", nidn_dosen);

			List<FormatNilai> formats = Common.getFormatNilais(perkuliahan);
			List<Map> mapsFormat = new ArrayList<Map>();
			for (FormatNilai formatNilai : formats) {
				Map map = new HashMap();
				Common.insertProperty(FormatNilai.class, formatNilai, map, "", 1, "perkuliahan");
				mapsFormat.add(map);
			}
			parameters.put("mapsFormat", mapsFormat);

			List<Mahasiswa> mahasiswas = tbmuser != null && tbmuser.getMahasiswa() != null ? new ArrayList<Mahasiswa>()
					: perkuliahan.ambilMahasiswa();

			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				mahasiswas.add(tbmuser.getMahasiswa());
			}

			List<Map> maps = new ArrayList<Map>();
			for (Mahasiswa mahasiswa : mahasiswas) {

				Map map = new HashMap();

				LampiranLain lam = LampiranLain.ambil(mahasiswa.getId(), LampiranLain.TTD_MAHASISWA);
				if (lam != null) {
					File file = lam.ambilFile();
					map.put("ttd_mahasiswa_pertemuan", file == null ? "" : file.getAbsolutePath());
				}

				Common.insertProperty(Mahasiswa.class, mahasiswa, map, "");
				map.put("tanggal", perkuliahan.getTanggalMulaiPerkuliahan());
				map.put("waktu", (perkuliahan.getWaktuMulai() == null ? "" : perkuliahan.getWaktuMulai()) + " sd "
						+ (perkuliahan.getWaktuSelesai() == null ? "" : perkuliahan.getWaktuSelesai()));

				map.put("materi", perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama());
				map.put("catatan", perkuliahan.getKeterangan());

				map.put("jenis_semester",
						perkuliahan.getStatusSemesterPendek() != null
								&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
										? Perkuliahan.SP
										: perkuliahan.getGanjilGenap());
				map.put("tahun_ajaran", perkuliahan.getTahunAjaran());
				map.put("paraf", "");

				map.put("kode_mk", perkuliahan.getMatakuliah().getKode());
				map.put("nama_mk", perkuliahan.getMatakuliah().getNama());

				map.put("nama_dosen", nama_dosen);
				map.put("nidn_dosen", nidn_dosen);

				map.put("nama_kaprodi", perkuliahan.getJurusan().getKaprodi() == null ? ""
						: perkuliahan.getJurusan().getKaprodi().getNama());
				map.put("nidn_kaprodi", perkuliahan.getJurusan().getKaprodi() == null ? ""
						: perkuliahan.getJurusan().getKaprodi().getNidn());

				maps.add(map);
			}
			parameters.put("maps", maps);

			parameters.put("jenis_semester",
					perkuliahan.getStatusSemesterPendek() != null
							&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)
									? Perkuliahan.SP
									: perkuliahan.getGanjilGenap());
			parameters.put("tahun_ajaran", perkuliahan.getTahunAjaran());

			mahasiswas = null;

			List<Long> paralelId = new ArrayList<Long>();
			if (paralelId.isEmpty()) {
				paralelId.add(-1L);
			}
			paralelId.add(perkuliahan.getId());
			parameters.put("paralelId", paralelId.toArray());

			paralelId = new ArrayList<Long>(perkuliahan.ambilPertemuan().values());
			if (paralelId.isEmpty()) {
				paralelId.add(-1L);
			}
			parameters.put("pertemuans", paralelId.toArray());
		}

		return parameters;
	}

	@SuppressWarnings({})
	public void onCetak(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "kontrak_perkuliahan",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kontrak Perkuliahan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
