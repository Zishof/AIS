package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
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
import ais.common.ConstantValues;
import ais.database.model.Dosen;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.file.LampiranLain;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.sekolah.Guru;
import ais.ui.util.MyWindow;

public class LaporanMonitorPerkuliahan extends MyWindow {

	private org.zkoss.zk.ui.Component center;

	private Perkuliahan perkuliahan;

	private Toolbar toolbar;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	public LaporanMonitorPerkuliahan(Perkuliahan perkuliahan) {
		super();
		this.perkuliahan = perkuliahan;
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Monitor Perkuliahan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() {

		org.zkoss.zul.Vbox vboxUtama = new org.zkoss.zul.Vbox();
		vboxUtama.setWidth("100%");
		vboxUtama.setParent(this);

		// West west = new West();
		// west.setTitle("Menu");
		// west.setCollapsible(true);
		// west.setParent(borderlayout);
		// ais.ui.util.ZkCompat.setFlex(west, true);
		// west.setWidth("350px");

		// MyGrid grid = new MyGrid();grid.setWidth("100%");
		// grid.setParent(west);
		// grid.setWidth("100%");
		// grid.setHeight("100%");
		//
		//
		// Columns columns = new Columns();
		// columns.setParent(grid);
		// MyColumnConfig column = new MyColumnConfig();
		// column.setWidth("20%");
		// column.setParent(columns);
		// column = new MyColumnConfig();
		// column.setParent(columns);

		center = new org.zkoss.zul.Div();

		(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "format1/lembar_monitoring_perkuliahan", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);

			}
		})).setParent(vboxUtama);
		center.setParent(vboxUtama);

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
			Common.insertProperty(Perkuliahan.class, perkuliahan, parameters, "");
			for (Dosen dosen : dosens) {
				Common.insertProperty(Dosen.class, dosen, parameters, "dosen_" + d);
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

			parameters.put("nama_dosen", nama_dosen);
			parameters.put("nidn_dosen", nidn_dosen);

			List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();

			for (Pertemuan pertemuan : pertemuans) {
				nama_dosen = "";
				nidn_dosen = "";
				int size = 0;
				for (Dosen dosen : dosens) {
					Statusabsensi statusabsensi = null;
					if (pertemuan.getId() != null) {

						statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
								pertemuan.retreiveAbsensiId(dosen.getId()));

					}

					if (statusabsensi == null) {
						statusabsensi = ConstantValues.BELUM_ABSEN;
					}

					if (ConstantValues.MASUK != null && statusabsensi.getId().equals(ConstantValues.MASUK.getId())) {
						size++;
						nama_dosen += nama_dosen.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
						nidn_dosen += nidn_dosen.isEmpty() ? dosen.getNidn() : ", " + dosen.getNidn();

						LampiranLain lam = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
						if (lam != null) {
							File file = lam.ambilFile();
							parameters.put("ttd_dosen_pertemuan_" + pertemuan.getId(),
									file == null ? "" : file.getAbsolutePath());
							parameters.put("ttd_dosen_pertemuan_" + pertemuan.getId() + "_" + size,
									file == null ? "" : file.getAbsolutePath());
						}
					}
				}

				parameters.put("nama_dosen_" + pertemuan.getId(), nama_dosen);
				parameters.put("nidn_dosen_" + pertemuan.getId(), nidn_dosen);
			}
			pertemuans = null;

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

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
					"format1/lembar_monitoring_perkuliahan", ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Monitor Perkuliahan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
