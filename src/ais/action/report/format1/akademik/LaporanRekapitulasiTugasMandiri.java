package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasPertemuan;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.TugasFileContent;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanRekapitulasiTugasMandiri extends MyWindow {

	private Center center;

	private Perkuliahan perkuliahan;

	private Toolbar toolbar;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Set<Tugas> selectedPertemuan;
	private Tbmuser tbmuser;

	public LaporanRekapitulasiTugasMandiri(Perkuliahan perkuliahan) {
		super();
		this.tbmuser = Common.getCurrentUser();
		this.perkuliahan = perkuliahan;
		selectedPertemuan = new HashSet<Tugas>();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Tugas Mandiri", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	@SuppressWarnings("unchecked")
	private void init() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();
		for (final Pertemuan pertemuan : pertemuans) {
			if (!pertemuan.getJudultugas().isEmpty()) {
				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				final Checkbox checkbox = new Checkbox(pertemuan.getJudultugas());
				checkbox.setChecked(true);
				row.appendChild(checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {

						if (checkbox.isChecked()) {
							selectedPertemuan.add(pertemuan);
						} else {
							selectedPertemuan.remove(pertemuan);
						}

						onCetak(event);
					}
				});
				selectedPertemuan.add(pertemuan);
			}

			for (final TugasPertemuan tugasPertemuan : pertemuan.ambilTugasPertemuanTotal().values()) {
				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				final Checkbox checkbox = new Checkbox(tugasPertemuan.getJudultugas());
				checkbox.setChecked(true);
				row.appendChild(checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {

						if (checkbox.isChecked()) {
							selectedPertemuan.add(tugasPertemuan);
						} else {
							selectedPertemuan.remove(tugasPertemuan);
						}

						onCetak(event);
					}
				});
				selectedPertemuan.add(tugasPertemuan);
			}
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "Daftar_Tugas_Mandiri", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);

			}
		}));

		onCetak(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		final Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("perkuliahan", perkuliahan == null || perkuliahan.getId() == null ? -1L : perkuliahan.getId());
		parameters.put("kelas",
				perkuliahan.getSemester() + " " + (perkuliahan.getKelas() == null ? "" : perkuliahan.getKelas()));

		parameters.put("tanggal_dibuat", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
		parameters.put("tampil_nilai", 1);
		parameters.put("fakultas", perkuliahan.getJurusan().getFakultas().getNama());
		parameters.put("jenis_semester",
				((Integer) perkuliahan.getSemester()) % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
		parameters.put("tahun_ajaran", perkuliahan.getTahunAjaran());
		parameters.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());
		parameters.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
		parameters.put("dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama());
		parameters.put("nip_dosen", perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getCode());
		parameters.put("jurusan", perkuliahan.getJurusan().getNama());

		String ttd = null;
		Dosen kaprodi = perkuliahan == null || perkuliahan.getJurusan() == null ? null
				: perkuliahan.getJurusan().getKaprodi();
		if (kaprodi != null) {
			LampiranLain lam = LampiranLain.ambil(kaprodi.getId(), LampiranLain.TTD_DOSEN);
			String nama = lam == null ? null : lam.getNama();

			if (nama != null) {
				if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
					ttd = lam.ambilFile().getAbsolutePath();

					parameters.put("ttd_kaprodi", ttd);
				}
			}
		}
		System.out.println("ttd_kaprodi => " + ttd);

		if (perkuliahan != null) {
			int d = 1;
			for (Dosen dosen : perkuliahan.populateDosenBuNama()) {
				LampiranLain lam = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						ttd = lam.ambilFile().getAbsolutePath();
						parameters.put("ttd_dosen_" + d, ttd);
						System.out.println("ttd_dosen_" + d + " => " + ttd);
					}
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
		}

		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
		Integer jumlah = 0;
		for (Tugas pertemuan : selectedPertemuan) {
			jumlah += pertemuan.getJudultugas().trim().isEmpty() ? 0 : 1;
		}
		parameters.put("jml", jumlah);
		for (Long detailperkuliahanid : perkuliahan.ambilDetailperkuliahan()) {
			Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
			if (detailperkuliahan != null) {
				if (detailperkuliahan.getMahasiswa() == null || (tbmuser != null && tbmuser.getMahasiswa() != null
						&& !tbmuser.getMahasiswa().getId().equals(detailperkuliahan.getMahasiswa().getId()))) {
					continue;
				}
				Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
				Integer count = 0;
				String pertemuanTugas = "";
				for (Tugas tugas : selectedPertemuan) {
					TugasFileContent tugasFileContent = tugas.ambilTugasFileContent(detailperkuliahan.getMahasiswa());
					if (tugasFileContent != null) {
						count++;
					}
					pertemuanTugas += tugasFileContent == null ? ""
							: (pertemuanTugas.equals("")
									? tugas.getJudultugas() + " (" + tugasFileContent.getNama() + ")"
									: ", " + tugas.getJudultugas() + " (" + tugasFileContent.getNama() + ")");
				}

				map.put("nim", detailperkuliahan.getMahasiswa().getNim());
				map.put("nama", detailperkuliahan.getMahasiswa().getNama());
				map.put("pertemuanTugas", pertemuanTugas);
				map.put("count", count);
				map.put("bobot", jumlah.equals(0) ? 0 : (count * 100 / jumlah));
				maps.add(map);
			}
		}
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onCetak(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Daftar_Tugas_Mandiri",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);

			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Tugas Mandiri", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
