package ais.action.report.format1.rab;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.rab.util.ChecklistLaporanDetailTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoBuktiChecklistLaporan;
import ais.database.model.rab.ChecklistLaporan;
import ais.database.model.rab.ChecklistLaporanDetail;
import ais.ui.util.MyWindow;

public class LaporanChecklist extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private ChecklistLaporan checklistLaporan;

	public LaporanChecklist(ChecklistLaporan checklistLaporan) {
		super();
		try {
			this.checklistLaporan = checklistLaporan;
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Checklist", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(
				new ParameterListener() {

					@SuppressWarnings({ "unchecked", "rawtypes" })
					@Override
					public Map<String, Serializable> generateParameters()
							throws Exception {

						Map parameters = generateParameter();
						return parameters;
					}
				}, "rab/Laporan_Checklist_Kegiatan", null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onReport(arg0);
					}
				}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		ChecklistLaporanDetailTreeModel checklistLaporanDetailTreeModel = new ChecklistLaporanDetailTreeModel(
				checklistLaporan);
		Session session = HibernateUtil.currentSession();
		List<ChecklistLaporanDetail> checklistLaporanDetails = session
				.createCriteria(ChecklistLaporanDetail.class)
				.add(Restrictions.eq("checklistLaporan", checklistLaporan))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"))
				.addOrder(Order.asc("id")).list();

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		for (final ChecklistLaporanDetail checklistLaporanDetail : checklistLaporanDetails) {
			if (checklistLaporanDetail.getParent() == null) {

				Integer count = 0;
				Session streamSession = StreamingHibernateUtil.getInstance()
						.currentSession();
				count = ((Number) streamSession
						.createCriteria(FotoBuktiChecklistLaporan.class)
						.add(Restrictions.eq("checklistLaporanDetail",
								checklistLaporanDetail.getId()))
						.setProjection(Projections.rowCount()).uniqueResult())
						.intValue();
				StreamingHibernateUtil.getInstance().closeSession();

				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("nama", checklistLaporanDetail.toString());
				map.put("ada", checklistLaporanDetail.getAda() != null
						&& checklistLaporanDetail.getAda() ? "Ya" : "Tidak");
				map.put("diperlukan",
						checklistLaporanDetail.getDiperlukan() != null
								&& checklistLaporanDetail.getDiperlukan() ? "Ya"
								: "Tidak");

				map.put("lampiran", count + " lampiran");
				maps.add(map);

				fillReport(checklistLaporanDetail, checklistLaporanDetails,
						checklistLaporanDetailTreeModel, maps);
			}
		}

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("maps", maps);
		parameters.put("keterangan", checklistLaporan.getNama() + "\n"
				+ checklistLaporan.getWorkspace().toString());
		return parameters;
	}

	private String getStrings(Integer deep) {
		String d = "";
		for (int i = 0; i < deep; i++) {
			d += "   ";
		}
		return d;
	}

	private void fillReport(ChecklistLaporanDetail parent,
			List<ChecklistLaporanDetail> checklistLaporanDetails,
			ChecklistLaporanDetailTreeModel checklistLaporanDetailTreeModel,
			List<Map<String, Object>> maps) {
		for (final ChecklistLaporanDetail checklistLaporanDetail : checklistLaporanDetails) {
			if (checklistLaporanDetail.getParent() != null
					&& checklistLaporanDetail.getParent().getId()
							.equals(parent.getId())) {
				Integer count = 0;
				Session streamSession = StreamingHibernateUtil.getInstance()
						.currentSession();
				count = ((Number) streamSession
						.createCriteria(FotoBuktiChecklistLaporan.class)
						.add(Restrictions.eq("checklistLaporanDetail",
								checklistLaporanDetail.getId()))
						.setProjection(Projections.rowCount()).uniqueResult())
						.intValue();
				StreamingHibernateUtil.getInstance().closeSession();

				List<Long> longs = new ArrayList<Long>();
				// if (checklistLaporanDetail.getDeep() == null) {
				checklistLaporanDetailTreeModel.getParentCount(
						checklistLaporanDetail, checklistLaporanDetail, longs);
				// }
				Integer deep = longs.size();

				Map<String, Object> map = new java.util.HashMap<String, Object>();
				map.put("nama",
						getStrings(deep) + checklistLaporanDetail.toString());
				map.put("ada", checklistLaporanDetail.getAda() != null
						&& checklistLaporanDetail.getAda() ? "Ya" : "Tidak");
				map.put("diperlukan",
						checklistLaporanDetail.getDiperlukan() != null
								&& checklistLaporanDetail.getDiperlukan() ? "Ya"
								: "Tidak");

				map.put("lampiran", count + " lampiran");
				maps.add(map);
				fillReport(checklistLaporanDetail, checklistLaporanDetails,
						checklistLaporanDetailTreeModel, maps);
			}
		}
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter(), "rab/Laporan_Checklist_Kegiatan",
					ais.ui.util.WaktuUtil.getDate(), null, 
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Checklist", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
