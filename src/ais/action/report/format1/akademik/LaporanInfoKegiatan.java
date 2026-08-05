package ais.action.report.format1.akademik;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JadwalKegiatanKampus;
import ais.database.model.PerguruanTinggi;
import ais.ui.util.MyWindow;

public class LaporanInfoKegiatan extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5620991583788581962L;

	public LaporanInfoKegiatan() throws Exception {
		super();
		init();
	}

	public LaporanInfoKegiatan(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void init() throws Exception {
		//
		// EventListener eventListener = new EventListener() {
		//
		// @Override
		// public void onEvent(Event event) throws Exception {
		// onLaporanAbsensi(event);
		//
		// }
		// };

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("nama_universitas", perguruanTinggi == null ? "" : perguruanTinggi.getNama());
		List<JadwalKegiatanKampus> jadwalKegiatanKampus = HibernateUtil.currentSession()
				.createCriteria(JadwalKegiatanKampus.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("tanggalMulai")).list();

		List<Map<String, Serializable>> maps = new ArrayList<Map<String, Serializable>>();

		for (JadwalKegiatanKampus kegiatanKampus : jadwalKegiatanKampus) {
			Map<String, Serializable> map = new java.util.HashMap<String, Serializable>();
			map.put("hari_tanggal",
					(kegiatanKampus.getTanggalMulai() == null ? ""
							: Common.dateFormat41.get().format(kegiatanKampus.getTanggalMulai()))
							+ (kegiatanKampus.getTanggalSelesai() == null ? ""
									: " s.d " + Common.dateFormat41.get().format(kegiatanKampus.getTanggalSelesai())));

			map.put("nama_kegiatan", kegiatanKampus.getNama());
			map.put("waktu",
					(kegiatanKampus.getWaktuMulai() == null ? ""
							: Common.timeFormat.get().format(kegiatanKampus.getWaktuMulai()))
							+ (kegiatanKampus.getWaktuSelesai() == null ? ""
									: " s.d " + Common.timeFormat.get().format(kegiatanKampus.getWaktuSelesai())));

			map.put("pelaksana", kegiatanKampus.getPelaksana());
			map.put("peserta", kegiatanKampus.getPeserta());
			map.put("narasumber", kegiatanKampus.getNarasumber());
			map.put("tempat", kegiatanKampus.getTempat());
			map.put("keterangan", kegiatanKampus.getKeterangan());
			maps.add(map);
		}

		parameters.put("maps", maps);
		Report.generatePDFReport("pdf", parameters, "jadwal_kegiatan", ais.ui.util.WaktuUtil.getDate(), Common.locale,
				null, center);

	}

}
