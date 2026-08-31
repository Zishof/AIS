package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;
import ais.ui.util.MyWindow;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.file.TugasFileContent;

/**
 * Penyusun/penyaji laporan untuk laporan rekapitulasi nilai mandiri. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Perkuliahan
 * perkuliahan}, {@code Toolbar toolbar}, {@code Set selectedPertemuan}; inisialisasi/lifecycle ({@code init()});
 * pelaporan/ekspor ({@code onCetak()}); operasi domain lain ({@code generateParameter()}); konfigurasi
 * constructor: {@code selectedPertemuan}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapitulasiNilaiMandiri extends MyWindow {

	private Center center;

	private Perkuliahan perkuliahan;

	private Toolbar toolbar;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private Set<Pertemuan> selectedPertemuan;

	public LaporanRekapitulasiNilaiMandiri(Perkuliahan perkuliahan) {
		super();
		this.perkuliahan = perkuliahan;

		selectedPertemuan = new HashSet<Pertemuan>();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Nilai Mandiri", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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

		Session session = HibernateUtil.currentSession();
		List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("perkuliahan", perkuliahan)).list();
		for (final Pertemuan pertemuan : pertemuans) {
			if ((pertemuan.getIsitugas() == null || pertemuan.getIsitugas()
					.trim().equals(""))) {
				continue;
			}
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig(pertemuan.getTopik());
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

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(
				new ParameterListener() {

					@SuppressWarnings({ "rawtypes" })
					@Override
					public Map<String, Serializable> generateParameters()
							throws Exception {
						Map parameters = generateParameter();
						return parameters;
					}
				}, "Daftar_Nilai_Mandiri", null, new EventListener() {

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

		parameters.put("perkuliahan",
				perkuliahan == null || perkuliahan.getId() == null ? -1L : perkuliahan.getId());
		parameters.put(
				"kelas",
				perkuliahan.getSemester()
						+ " "
						+ (perkuliahan.getKelas() == null ? "" : perkuliahan
								.getKelas()));

		parameters.put("tanggal_dibuat", Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate()));
		parameters.put("tampil_nilai", 1);
		parameters.put("fakultas", perkuliahan.getJurusan().getFakultas()
				.getNama());
		parameters
				.put("jenis_semester",
						((Integer) perkuliahan.getSemester()) % 2 == 0 ? Perkuliahan.GENAP
								: Perkuliahan.GANJIL);
		parameters.put("tahun_ajaran", perkuliahan.getTahunAjaran());
		parameters
				.put("nama_matakuliah", perkuliahan.getMatakuliah().getNama());
		parameters
				.put("kode_matakuliah", perkuliahan.getMatakuliah().getKode());
		parameters.put("dosen", perkuliahan.getDosen1() == null ? ""
				: perkuliahan.getDosen1().getNama());
		parameters.put("nip_dosen", perkuliahan.getDosen1() == null ? ""
				: perkuliahan.getDosen1().getCode());
		parameters.put("jurusan", perkuliahan.getJurusan()
				.getNama());

		Session session = HibernateUtil.currentSession();
		List<Detailperkuliahan> detailperkuliahans = session
				.createCriteria(Detailperkuliahan.class)
				.createAlias("mahasiswa", "mahasiswa")
				.addOrder(Order.asc("mahasiswa.nim"))
				.add(Restrictions.eq("perkuliahan", perkuliahan)).list();

		Session streamingSession = StreamingHibernateUtil.getInstance()
				.currentSession();
		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();
		Integer jumlah = 0;
		for (Pertemuan pertemuan : selectedPertemuan) {
			jumlah += ((pertemuan.getIsitugas() == null || pertemuan
					.getIsitugas().trim().equals("")) ? 0 : 1);
		}
		parameters.put("jml", jumlah);
		for (Detailperkuliahan detailperkuliahan : detailperkuliahans) {
			if (detailperkuliahan.getMahasiswa() == null) {
				continue;
			}
			Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
			Integer count = 0;
			Double nilai = 0.0;
			String pertemuanTugas = "";
			for (Pertemuan pertemuan : selectedPertemuan) {

				ProjectionList projectionList = Projections.projectionList();
				projectionList.add(Projections.rowCount());
				projectionList.add(Projections.sum("nilai"));

				Number[] jml = ((Number[]) streamingSession
						.createCriteria(TugasFileContent.class)
						.add(Restrictions.eq("pertemuan", pertemuan.getId()))
						.add(Restrictions.eq("mahasiswa", detailperkuliahan
								.getMahasiswa().getId()))
						.setProjection(projectionList).uniqueResult());

				nilai += jml[1] == null ? 0.0 : jml[1].doubleValue();
				count += (jml[0].intValue() == 0 ? 0 : 1);
				pertemuanTugas += jml[0].intValue() == 0 ? "" : (pertemuanTugas
						.equals("") ? pertemuan.getTopik() : ", "
						+ pertemuan.getTopik());
			}

			nilai = nilai / count.doubleValue();

			map.put("nim", detailperkuliahan.getMahasiswa().getNim());
			map.put("nama", detailperkuliahan.getMahasiswa().getNama());
			map.put("pertemuanTugas", pertemuanTugas);
			map.put("count", count);
			map.put("bobot", nilai);
			maps.add(map);
		}
		parameters.put("maps", maps);

		String tahunAkademik = perkuliahan.getTahunAjaran();
		parameters.put("bar",
				"3-" + tahunAkademik + "-" + perkuliahan.getSemester() + "-"
						+ perkuliahan.getId());

		StreamingHibernateUtil.getInstance().closeSession();
		return parameters;
	}

	@SuppressWarnings({})
	public void onCetak(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter(), "Daftar_Nilai_Mandiri", ais.ui.util.WaktuUtil.getDate(),
					toolbar);

			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Nilai Mandiri", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
