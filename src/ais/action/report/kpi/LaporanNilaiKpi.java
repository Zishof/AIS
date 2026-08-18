package ais.action.report.kpi;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Toolbar;

import ais.action.master.kpi.helper.ItemKpiTreeModel;
import ais.action.master.kpi.helper.KpiUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.kpi.ItemKpi;
import ais.database.model.kpi.NilaiKpi;
import ais.database.model.kpi.PenilaianKpi;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanNilaiKpi extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private PenilaianKpi penilaianKpi;
	private ItemKpiTreeModel itemKpiTreeModel;

	public LaporanNilaiKpi(PenilaianKpi penilaianKpi, FormatKpiDetail formatKpiDetail) {
		super();
		try {
			this.penilaianKpi = penilaianKpi;
			itemKpiTreeModel = new ItemKpiTreeModel(true, formatKpiDetail);
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Nilai Kpi", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "kpi/Realisasi", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings("rawtypes")
	private Map generateParameter() throws Exception {
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		return generateParameter(null, maps);
	}

	private String getStrings(Integer deep) {
		String d = "";
		for (int i = 0; i < deep; i++) {
			d += "   ";
		}
		return d;
	}

	private Map<String, NilaiKpi> mapNilaiKpi = new HashMap<String, NilaiKpi>();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter(ItemKpi parent, List<Map<String, Object>> maps) throws Exception {
		Date sekarang = WaktuUtil.getDate();

		List<ItemKpi> workspaces = itemKpiTreeModel.getChildren(parent);

		for (ItemKpi itemKpi : workspaces) {
			ParameterTambahan parameterTambahan = itemKpi.getKpi().getSatuanKpi() == null ? null
					: itemKpi.getKpi().getSatuanKpi().getParameterTambahan();
			List<Long> longs = new ArrayList<Long>();

			itemKpiTreeModel.getParentCount(itemKpi, longs);

			Integer deep = longs.size();

			longs = null;

			String key = penilaianKpi.getId() + "_" + itemKpi.getId();

			NilaiKpi nilaiKpiTemp = mapNilaiKpi.get(key);
			if (nilaiKpiTemp == null) {
				Session session = HibernateUtil.currentSession();
				nilaiKpiTemp = (NilaiKpi) ConstantValues
						.simpleObject(
								session.createCriteria(NilaiKpi.class).add(Restrictions.eq("itemKpi", itemKpi))
										.add(Restrictions.eq("penilaianKpi", penilaianKpi)).setMaxResults(1),
								NilaiKpi.class);
				if (nilaiKpiTemp == null) {
					nilaiKpiTemp = new NilaiKpi();
					nilaiKpiTemp.setItemKpi(itemKpi);
					nilaiKpiTemp.setPenilaianKpi(penilaianKpi);
					session.save(nilaiKpiTemp);
					session.flush();
				}

				mapNilaiKpi.put(key, nilaiKpiTemp);
			}

			Map<String, Object> map = new java.util.HashMap<String, Object>();
			map.put("workspace_id", itemKpi.getId());
			map.put("unique_id", itemKpi.getId());
			map.put("kode", itemKpi.getKode() == null ? "" : itemKpi.getKode());
			map.put("nama", getStrings(deep) + (itemKpi.getNama() == null ? "" : itemKpi.getNama()));

			String format = KpiUtil.ambilTarget(itemKpi.getFormula(), sekarang);
			Double hasil = itemKpiTreeModel.hitungNilaiKpi(nilaiKpiTemp, format, penilaianKpi, false, null);

			map.put("target",
					parameterTambahan == null ? ""
							: (itemKpi.getValtampil() + " " + (itemKpi.getKpi().getSatuanKpi() == null ? ""
									: itemKpi.getKpi().getSatuanKpi().getKode())));

			map.put("realisasi",
					parameterTambahan == null ? ""
							: (nilaiKpiTemp.getValtampil() + " " + (itemKpi.getKpi().getSatuanKpi() == null ? ""
									: itemKpi.getKpi().getSatuanKpi().getKode())));

			map.put("hitungan_target", itemKpi.getTarget());
			map.put("hitungan", hasil);
			Double persen = ((hasil * 100.0) / itemKpi.getTarget());
			map.put("persen", persen);

			maps.add(map);

			if (!itemKpiTreeModel.isLeaf(itemKpi)) {
				generateParameter(itemKpi, maps);
			}
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("pegawai", penilaianKpi.getPegawai().getNama());
		parameters.put("ta", penilaianKpi.getTa());
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "kpi/Realisasi",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Nilai Kpi", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
