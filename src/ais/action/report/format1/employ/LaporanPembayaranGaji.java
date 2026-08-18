package ais.action.report.format1.employ;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Label;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Jabatan;
import ais.database.model.KehadiranPegawaiBulanan;
import ais.database.model.Pegawai;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.kpi.PenilaianKpi;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.ui.util.MyJSONObject;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanPembayaranGaji extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private PembayaranGaji pembayaranGaji;

	public LaporanPembayaranGaji(PembayaranGaji pembayaranGaji) {
		super();
		this.pembayaranGaji = pembayaranGaji;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pembayaran Gaji", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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

			@SuppressWarnings({ "unchecked" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				return parameters;
			}
		}, "payroll/form_ver_unit", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings("rawtypes")
	private Map parameters = new HashMap();
	private @SuppressWarnings("rawtypes") Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class)
			.values();

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			final Label label = Common.displayLoadBar(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(center);
					File file = Report.generateFileReportWithProgress(Report.PDF, parameters, "payroll/form_ver_unit",
							ais.ui.util.WaktuUtil.getDate(), null, toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				}
			});

			new Thread(new Runnable() {

				@SuppressWarnings({ "unchecked", "rawtypes" })
				private String maukkandataSatker(Session session, List<Map> maps, Map<String, Map> mapsTotalPerJenis,
						Date sekarang, PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai,
						String currentCaraPembayaranGaji, Integer bln, Integer thn,
						TreeMap<String, List<ItemGaji>> myItems) {
					try {
						MyJSONObject jsonObject = new MyJSONObject(pembayaranGajiPunyaPegawai.getKomponenGaji());
						Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();
						PenilaianKpi penilaianKpiData = PenilaianKpi.hitungKpi(session, pegawai, sekarang);
						List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkat(sekarang, pangkats);
						JabatanFungsional jabatanFungsional = pegawai.ambilJabatanFungsional(kenaikanPangkats);
						JabatanStruktural jabatanStruktural = pegawai.ambilJabatanStruktural(kenaikanPangkats);
						Jabatan jabatan = pegawai.ambilJabatan(kenaikanPangkats);

						GajiPokok gajiPokok = pegawai.ambilGajiPokok(sekarang);

						KehadiranPegawaiBulanan kehadiranPegawaiBulanan = (KehadiranPegawaiBulanan) session
								.createCriteria(KehadiranPegawaiBulanan.class).add(Restrictions.eq("bulan", bln))
								.add(Restrictions.eq("tahun", thn)).add(Restrictions.eq("pegawai.id", pegawai.getId()))
								.setMaxResults(1).uniqueResult();

						Map map = new HashMap();

						if (jabatanFungsional != null) {
							Common.insertProperty(JabatanFungsional.class, jabatanFungsional, map, "fungsional");
						}
						if (jabatanFungsional != null) {
							Common.insertProperty(JabatanStruktural.class, jabatanStruktural, map, "struktural");
						}
						if (jabatanFungsional != null) {
							Common.insertProperty(Jabatan.class, jabatan, map, "jabatan");
						}
						if (gajiPokok != null) {
							Common.insertProperty(GajiPokok.class, gajiPokok, map, "gajiPokok");
						}
						if (penilaianKpiData != null) {
							Common.insertProperty(PenilaianKpi.class, penilaianKpiData, map, "kpi");
						}

						if (kehadiranPegawaiBulanan != null) {
							Common.insertProperty(KehadiranPegawaiBulanan.class, kehadiranPegawaiBulanan, map,
									"kehadiran", 1, "pegawai");
						}

						Common.insertProperty(PembayaranGajiPunyaPegawai.class, pembayaranGajiPunyaPegawai, map,
								"pembayaran.gaji", 1, "pegawai");

						Common.insertProperty(Pegawai.class, pegawai, map, "");

						label.setValue("Sedang memproses data " + pegawai.toString());
						ItemGaji itemGajidata = null;
						for (String kode : myItems.keySet()) {
							List<ItemGaji> itemGajis = myItems.get(kode);
							Double nilaiTotal = 0.0;
							for (ItemGaji itemGaji : itemGajis) {

								if (!jsonObject.isNull(itemGaji.getId().toString())) {
									Double nilai = Double
											.parseDouble(jsonObject.get(itemGaji.getId().toString()).toString());
									map.put("sub_item_gaji_nilai_" + itemGaji.getKode(), nilai);
									map.put("sub_item_gaji_kode_" + itemGaji.getKode(), itemGaji.getKode());
									map.put("sub_item_gaji_nama_" + itemGaji.getKode(), itemGaji.getNama());
									map.put("sub_item_gaji_formula_" + itemGaji.getKode(),
											itemGaji.getDefaultFormula());
									nilaiTotal = nilai;
									itemGajidata = itemGaji;
									break;
								}
							}

							map.put("total_item_gaji_nilai_" + kode, nilaiTotal);
							if (itemGajidata != null) {
								map.put("total_item_gaji_kode_" + kode, itemGajidata.getKode());
								map.put("total_item_gaji_nama_" + kode, itemGajidata.getNama());
								map.put("total_item_gaji_formula_" + kode, itemGajidata.getDefaultFormula());
							}

						}

						maps.add(map);

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pembayaran Gaji", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
					}

					return currentCaraPembayaranGaji;
				}

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void run() {

					parameters.put("bln", pembayaranGaji.getBulan());
					parameters.put("nama_bln", Common.BULAN[pembayaranGaji.getBulan() - 1]);
					parameters.put("thn", pembayaranGaji.getTahun());
					parameters.put("caraPembayaranGaji", pembayaranGaji.getCaraPembayaranGaji() == null ? -1L
							: pembayaranGaji.getCaraPembayaranGaji().getId());

					parameters.put("satuan_kerja_id",
							pembayaranGaji.getSatuanKerja() == null ? -1L : pembayaranGaji.getSatuanKerja().getId());
					parameters.put("satuan_kerja",
							pembayaranGaji.getSatuanKerja() == null ? "" : pembayaranGaji.getSatuanKerja().getNama());

					Session session = HibernateUtil.currentSession();
					List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = session
							.createCriteria(PembayaranGajiPunyaPegawai.class)
							.add(Restrictions.eq("pembayaranGaji", pembayaranGaji)).createAlias("pegawai", "pegawai")
							.addOrder(Order.asc("pegawai.nama")).list();

					NumberFormat nf = new DecimalFormat("000");
					TreeMap<String, List<ItemGaji>> myItems = new TreeMap<String, List<ItemGaji>>();
//					Set<String> blmMasukGrup = new HashSet<String>();
					for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

						try {

							MyJSONObject jsonObject = new MyJSONObject(pembayaranGajiPunyaPegawai.getKomponenGaji());
							Iterator<String> iterator = jsonObject.keys();
							while (iterator.hasNext()) {
								String key = iterator.next();
								ItemGaji itemGaji = (ItemGaji) ConstantValues.ambil(ItemGaji.class.getName(),
										Long.parseLong(key));
								if (itemGaji != null) {
									String kode = nf.format(itemGaji.getNomorUrut()) + "-" + itemGaji.getKode();
//									if (!blmMasukGrup.contains(kode)) {
//										blmMasukGrup.add(kode);
									try {

										List<ItemGaji> itemGajis = myItems.get(kode);
										if (itemGajis == null) {
											itemGajis = new ArrayList<ItemGaji>();
											myItems.put(kode, itemGajis);
										}
										itemGajis.add(itemGaji);

									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/employ/LaporanPembayaranGaji.java:260");
										PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pembayaran Gaji", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
											new String[] {
												"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
												"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
												"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
											});
									}
								}
							}
//							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/employ/LaporanPembayaranGaji.java:265");
							// TODO: handle exception
						}
					}

					Date sekarang = WaktuUtil.getDate();

					String currentCaraPembayaranGaji = null;
					List<Map> maps = new ArrayList<Map>();
					Map<String, Map> mapsTotalPerJenis = new HashMap<String, Map>();
					for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {
						maukkandataSatker(session, maps, mapsTotalPerJenis, sekarang, pembayaranGajiPunyaPegawai,
								currentCaraPembayaranGaji, pembayaranGaji.getBulan(), pembayaranGaji.getTahun(),
								myItems);
					}

					parameters.put("maps", maps);

					ais.action.report.helper.LoadingReportUtil.selesai(label);

				}
			}).start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pembayaran Gaji", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
