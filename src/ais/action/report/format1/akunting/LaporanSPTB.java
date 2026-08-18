package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Toolbar;

import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyWindow;

public class LaporanSPTB extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private GrupTransaksi grupTransaksi;

	private String jenis;

	private WorkspaceTreeModel workspaceTreeModel;

	private Workspace workspace;

	private Transaksi transaksi;

	public LaporanSPTB(Workspace workspace, GrupTransaksi grupTransaksi, Transaksi transaksi,
			WorkspaceTreeModel workspaceTreeModel, String jenis) {
		super();
		this.workspace = workspace;
		this.transaksi = transaksi;
		this.grupTransaksi = grupTransaksi;
		this.jenis = jenis;
		this.workspaceTreeModel = workspaceTreeModel;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan SPTB", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanSPTB(Workspace workspace, GrupTransaksi grupTransaksi, Transaksi transaksi,
			WorkspaceTreeModel workspaceTreeModel, String jenis, String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);
		this.workspace = workspace;
		this.transaksi = transaksi;
		this.grupTransaksi = grupTransaksi;
		this.jenis = jenis;
		this.workspaceTreeModel = workspaceTreeModel;
		init();
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
		}, "akunting/sptb", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();

		List<SatuanKerja> satuanKerjas = new ArrayList<SatuanKerja>();
		satuanKerjas.add(workspace.getSatuanKerja());
		workspaceTreeModel.getSatuanKerjaTreeModel().getParentSet(workspace.getSatuanKerja(), satuanKerjas);
		SatuanKerja satuanKerja = satuanKerjas.size() > 2 ? satuanKerjas.get(satuanKerjas.size() - 3) : null;
		SatuanKerja lembaga = satuanKerjas.size() > 0 ? satuanKerjas.get(satuanKerjas.size() - 1) : null;
		SatuanKerja unit = satuanKerjas.size() > 1 ? satuanKerjas.get(satuanKerjas.size() - 2) : null;

		Map<String, Object> map = new java.util.HashMap<String, Object>();

		// =================================
		// =================================
		// =================================

		Double pemakaian = workspaceTreeModel.getRealisasi(workspace, transaksi.getTanggalTransaksi());

		map.put("mak", transaksi.getAkun() == null ? ""
				: transaksi.getAkun().getKode() + " - " + transaksi.getAkun().getNama());
		map.put("pagu", workspace.getHargaTotal());
		map.put("spp_bulan_lalu", pemakaian);
		map.put("spp_ini", grupTransaksi.getTotalKredit());
		map.put("jml_spp_ini", grupTransaksi.getTotalKredit() + pemakaian);
		map.put("sisa_dana", workspace.getHargaTotal() - (grupTransaksi.getTotalKredit() + pemakaian));
		map.put("mak", transaksi.getAkun() == null ? ""
				: transaksi.getAkun().getKode() + " - " + transaksi.getAkun().getNama());

		map.put("satuan_kerja", satuanKerja == null ? "" : satuanKerja.getNama());
		map.put("keperluan", grupTransaksi.getKeperluan());
		map.put("keterangan", transaksi == null ? "" : (transaksi.getKeterangan()));
		map.put("tgl_ref", grupTransaksi.getTanggalTagihan() == null ? ""
				: Common.dateFormat2.get().format(grupTransaksi.getTanggalTagihan()));
		map.put("noref", grupTransaksi.getNomorTagihan());
		maps.add(map);

		// =================================
		// =================================
		// =================================

		Session session = HibernateUtil.currentSession();
		Pejabat pejabat = (Pejabat) session.createCriteria(Pejabat.class)
				.add(Restrictions.or(Restrictions.isNotNull("pegawai"),
						Restrictions.or(Restrictions.isNotNull("guru"), Restrictions.isNotNull("dosen"))))
				.add(Restrictions.or(Restrictions.or(Restrictions.eq("satuanKerja", satuanKerja),
						Restrictions.eq("satuanKerja", unit)), Restrictions.eq("satuanKerja", lembaga)))
				.createAlias("jenisJabatan", "jenisJabatan")
				.add(Restrictions.ilike("jenisJabatan.nama", "Kuasa Pengguna Anggaran", MatchMode.ANYWHERE))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).setMaxResults(1).uniqueResult();

		Map parameters = LaporanAkuntingHelper.getDefaultParameter(workspace, transaksi, workspaceTreeModel);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(grupTransaksi.getTanggalTransaksi());
		parameters.put("nomor",
				".../SPP-"
						+ (grupTransaksi.getJenisTransaksi() == null ? "" : grupTransaksi.getJenisTransaksi().getKode())
						+ "/BLSDM.6/" + (calendar.get(Calendar.YEAR)));
		parameters.put("jenis", jenis);
		parameters.put("pejabat",
				pejabat == null || pejabat.getPegawai() == null
						? (pejabat.getDosen() == null ? "" : pejabat.getDosen().getNama())
						: pejabat.getPegawai().getNama());
		parameters.put("nip_pejabat",
				pejabat == null || pejabat.getPegawai() == null
						? (pejabat.getDosen() == null ? "" : pejabat.getDosen().getCode())
						: pejabat.getPegawai().getCode());

		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/sptb",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan SPTB", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
