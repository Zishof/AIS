package ais.action.report.format1.rab;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataSumberDanaBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan perencanaan tri wulan. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox tahun}, {@code
 * AmbilDataSatuanKerjaBanbox satuanKerja}, {@code AmbilDataSumberDanaBanbox sumberDana}, {@code
 * AmbilDataWorkspaceBanbox workspace}, {@code Combobox level}, {@code Center center}, {@code Toolbar toolbar};
 * inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onReport()}); operasi domain lain ({@code
 * generateParameter()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanPerencanaanTriWulan extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox tahun;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataSumberDanaBanbox sumberDana;
	private AmbilDataWorkspaceBanbox workspace;
	private Combobox level;

	private Center center;
	private Toolbar toolbar;

	public LaporanPerencanaanTriWulan() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Perencanaan Tri Wulan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPerencanaanTriWulan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				sumberDana.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"),
						(Integer) (tahun.getSelectedItem() == null
								? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
								: tahun.getSelectedItem().getValue()));
				workspace.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
				onReport(event);

			}
		};

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
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun = new Combobox());
		int year = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = year + 5; i > (year - 20); i--) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
			if (i == year) {
				tahun.setSelectedItem(comboitem);
			}
		}
		tahun.setWidth("90%");
		// tahun.// addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		// satuanKerja.// setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sumber Dana"));
		row.appendChild(sumberDana = new AmbilDataSumberDanaBanbox());
		sumberDana.setWidth("90%");
		// sumberDana.// setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Root"));
		row.appendChild(workspace = new AmbilDataWorkspaceBanbox(true));
		workspace.setWidth("90%");
		// workspace.// setEventListener(eventListener);
		sumberDana.setSatuanKerja(Common.getCurrentUser() == null ? null : Common.getCurrentUser().ambilSatuanKerja(),
				Calendar.getInstance().get(Calendar.YEAR));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Level"));
		row.appendChild(level = new Combobox());
		int defaultLevel = 0;
		for (int i = 0; i < 20; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			level.appendChild(comboitem);
			if (i == defaultLevel) {
				level.setSelectedItem(comboitem);
			}
		}
		level.setWidth("90%");
		// level.// addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan");
		button.setParent(row);
		button.addEventListener("onClick", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (tahun.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih salah satu tahun", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (satuanKerja.getAttribute("satuanKerja") == null) {
					MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return null;
				}
				// // if (sumberDana.getAttribute("sumberDana") == null)
				// {
				// MyMessageboxConfig.show("Sumber Dana harus diisi",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.EXCLAMATION);
				// return null;
				// }

				Map parameters = generateParameter();
				return parameters;
			}
		}, "rab/Rencana_Anggaran_Tri_Wulan", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (tahun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu tahun", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			// MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan",
			// MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return null;
		}
		if (level.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu level", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}
		// // if (sumberDana.getAttribute("sumberDana") == null) {
		// MyMessageboxConfig.show("Sumber Dana harus diisi", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
		// return null;
		// }

		Integer tahun1 = (Integer) this.tahun.getSelectedItem().getValue();
		final SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		final SumberDana sumberDana = (SumberDana) this.sumberDana.getAttribute("sumberDana");

		RabReportHelper rabReportHelper = new RabReportHelper(tahun1, satuanKerja, sumberDana);
		SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (satuanKerja != null) {
			satuanKerjas.add(satuanKerja);
			satuanKerjaTreeModel.getChildsSet(satuanKerja, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();
		List<Workspace> workspaces = session.createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.or(
						satuanKerja == null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"),
						Restrictions.in("satuanKerja", satuanKerjas)))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).add(Restrictions.eq("tahunWorkspace", tahun1))
				.list();

		WorkspaceTreeModel workspaceTreeModel = new WorkspaceTreeModel(tahun1, rabReportHelper.getMaxrevisi(),
				satuanKerja, sumberDana);

		Integer level = (Integer) this.level.getSelectedItem().getValue();
		if (level > 0) {
			Iterator<Workspace> it = workspaces.iterator();
			List<Workspace> deletedWorkspaces = new ArrayList<Workspace>();
			while (it.hasNext()) {
				Workspace workspace = it.next();
				List<Long> longs = new ArrayList<Long>();
				workspaceTreeModel.getChildDeepSet(workspace.getId(), longs);
				if (level > longs.size()) {
					deletedWorkspaces.add(workspace);
				}
			}
			workspaces.removeAll(deletedWorkspaces);
		}

		Long parentId = WorkspaceTreeModel.checkForParent(tahun1, satuanKerja, rabReportHelper.getMaxrevisi());

		final Workspace selectedWorkspace = (Workspace) workspace.getAttribute("workspace");
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		for (final Workspace workspace : workspaces) {
			if ((selectedWorkspace != null && selectedWorkspace.getId().equals(workspace.getId()))
					|| (selectedWorkspace == null
							&& (workspace.getParentId() == null || workspace.getParentId().equals(parentId)))) {

				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("workspace_id", workspace.getId());
				map.put("unique_id", workspace.getId());
				map.put("kode", workspace.getKode() == null ? "" : workspace.getKode());
				map.put("nama",
						workspace.getNama() == null ? ""
								: workspace.getNama() + (workspace.getUnitOrganisasi() == null ? ""
										: " - " + workspace.getUnitOrganisasi().getNama()));

				map.put("harga_total", workspace.getHargaTotal().equals(0.0) ? null : workspace.getHargaTotal());

				Set<Long> childsId = new HashSet<Long>();
				workspaceTreeModel.generateChildsByIds(workspace.getId(), childsId);
				List<Object[]> numbers = workspaceTreeModel.getHargaTotalPerencanaanTriWulan(childsId, tahun1);

				for (Object[] myNumbers : numbers) {
					map.put("total", myNumbers[0] == null ? 0L : ((Number) myNumbers[0]).doubleValue());
					map.put("harga_total_1", myNumbers[1] == null ? 0L : ((Number) myNumbers[1]).doubleValue());
					map.put("harga_total_2", myNumbers[2] == null ? 0L : ((Number) myNumbers[2]).doubleValue());
					map.put("harga_total_3", myNumbers[3] == null ? 0L : ((Number) myNumbers[3]).doubleValue());
					map.put("harga_total_4", myNumbers[4] == null ? 0L : ((Number) myNumbers[4]).doubleValue());
				}

				maps.add(map);
				rabReportHelper.generateRencanaTriWulanAnggaran(workspace.getParentId(), workspaceTreeModel,
						workspace.getId(), workspaces, tahun1, maps);
			}
		}

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("satuan_kerja", satuanKerja.getNama());
		parameters.put("tahun", tahun1);
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "rab/Rencana_Anggaran_Tri_Wulan",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Perencanaan Tri Wulan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
