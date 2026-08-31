package ais.action.report.format1.payroll;
import ais.common.PesanFormalHelper;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.payroll.CaraPembayaranGaji;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.EcampusUtil;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyJSONObject;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekap pembayaran gaji satuan kerja. Kelas ini mengubah
 * data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Spreadsheet excelku}, {@code MyTextbox
 * searchnama}, {@code Combobox bulan}, {@code Intbox tahun}, {@code Combobox caraBayar}, {@code
 * AmbilDataSatuanKerjaBanbox searchparent}, {@code SatuanKerjaTreeModel satuanKerjaTreeModel}, {@code Center
 * center}; inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onCetak()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapPembayaranGajiSatuanKerja extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Spreadsheet excelku;

	private MyTextbox searchnama = new MyTextbox();

	private Combobox bulan = new Combobox();
	private Intbox tahun = new Intbox(Calendar.getInstance().get(Calendar.YEAR));
	private Combobox caraBayar = new Combobox();
	private AmbilDataSatuanKerjaBanbox searchparent;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Center center;

	private MyToolbarbuttonConfig printAmbil;

	private Rows rowsData;
	private List<MyCheckboxConfig> checkboxConfigs = new ArrayList<MyCheckboxConfig>();

	public LaporanRekapPembayaranGajiSatuanKerja() throws Exception {
		super();
		init();
	}

	public LaporanRekapPembayaranGajiSatuanKerja(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

		searchparent = new AmbilDataSatuanKerjaBanbox();
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(arg0);
			}
		});

		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		for (int i = 0; i < 12; i++) {
			Comboitem comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Comboitem comboitem = new Comboitem("Semua");
		comboitem.setValue(null);
		bulan.appendChild(comboitem);
		bulan.setSelectedItem(comboitem);
		bulan.setReadonly(true);

		Common.selectComboItem(bulan, Calendar.getInstance().get(Calendar.MONTH) + 1);

		SatuanKerja satuanKerja = Common.getSatuanKerja();
		Common.insertComboDanSemua(caraBayar, new String[] { "nama", "satuanKerja" }, "akun", CaraPembayaranGaji.class,
				Restrictions.and(
						satuanKerja == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.eq("satuanKerja", satuanKerja)),
						Restrictions.and(Restrictions.isNotNull("akun"),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		LayoutRegion west = Common.isMobile() ? new North() : new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		if (Common.isMobile()) {
			west.setHeight("250px");
			west.setOpen(false);
		} else {
			west.setWidth("200px");
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		Vbox vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Pegawai"));
		vbox.appendChild(searchnama);
		searchnama.setCols(5);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Bulan"));
		vbox.appendChild(bulan);
		bulan.setCols(5);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		vbox.appendChild(tahun);
		tahun.setCols(5);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran"));
		vbox.appendChild(caraBayar);
		caraBayar.setCols(5);
		caraBayar.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		vbox.appendChild(searchparent);
		searchparent.setCols(5);
		searchparent.setReadonly(true);

		SatuanKerja satuanKerjaData = satuanKerja;
		Tbmuser tbmuser = Common.getCurrentUser();
		if (satuanKerjaData != null && tbmuser != null && tbmuser.hakAkses() != null
				&& !tbmuser.hakAkses().getMelihatDataSatkerLain()) {
			searchparent.setValue(satuanKerjaData.getNama());
			searchparent.setAttribute("satuanKerja", satuanKerjaData);
			searchparent.setAttribute("myValue", satuanKerjaData);
			searchparent.setDisabled(true);
		}


		row = new MyFormRow();
		row.setParent(rows);

		Vbox toolbar = new Vbox();
		toolbar.setParent(row);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetak(null);
			}
		});
		print.setParent(toolbar);

		printAmbil = new MyToolbarbuttonConfig("Ambil File", "/img/excel.png");
		printAmbil.setVisible(false);
		printAmbil.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					excelku.getBook().write(bout);
					bout.close();
					Filedownload.save(bout.toByteArray(),
							"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
							"tagihan_dan_realisasi.xlsx");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanRekapPembayaranGajiSatuanKerja.java:240");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Rekap Pembayaran Gaji Satuan Kerja", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});

				}
			}
		});
		printAmbil.setParent(toolbar);

		row = new MyFormRow();
		row.setParent(rows);

		rowsData = (Rows) Common.tampilanScroll1(row).getParent();

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

	}

	@SuppressWarnings("rawtypes")
	private List<List> datas = null;
	private TreeMap<String, List<ItemGaji>> myItems = new TreeMap<String, List<ItemGaji>>();
	private TreeMap<String, List<ItemGaji>> myItemsCopy = new TreeMap<String, List<ItemGaji>>();
	private Set<String> blmMasukGrup = new HashSet<String>();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetak(Event event) {

		try {

			final Label label = Common.displayLoadBar(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(center);
					LaporanRekapPembayaranGaji.tampilanPilihaItem(blmMasukGrup, rowsData, myItemsCopy, myItems,
							checkboxConfigs);
					excelku = new ais.ui.util.MySpreadsheet();
					center.appendChild(excelku);
					EcampusUtil.tampilkan(datas, excelku);
					// Tampilkan sebagai grid ringan; Excel tetap utuh saat tombol Download diklik.
					ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(excelku);
					printAmbil.setVisible(true);
				}
			});

			new Thread(new Runnable() {

				@Override
				public void run() {
					myItems.clear();

					String nama = searchnama.getValue().trim();
					Integer bln = (Integer) (bulan.getSelectedItem() == null ? null
							: bulan.getSelectedItem().getValue());
					Integer thn = tahun.getValue();

					CaraPembayaranGaji caraPembayaranGaji = (CaraPembayaranGaji) (caraBayar.getSelectedItem() == null
							? null
							: caraBayar.getSelectedItem().getValue());

					datas = new ArrayList<List>();

					SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
					Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
					if (parent != null) {
						satuanKerjas.clear(); satuanKerjas.add(parent);
						satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
					}

					Session session = HibernateUtil.currentSession();
					List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = session
							.createCriteria(PembayaranGajiPunyaPegawai.class)
							.createAlias("pembayaranGaji", "pembayaranGaji").createAlias("pegawai", "pegawai")

							.add(caraPembayaranGaji == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("pembayaranGaji.caraPembayaranGaji", caraPembayaranGaji))

							.createAlias("pembayaranGaji.caraPembayaranGaji", "caraPembayaranGaji")

							.add(satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
									: Restrictions.in("pegawai.satuanKerja", satuanKerjas))

							.add(bln == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("pembayaranGaji.bulan", bln))

							.add(thn == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("pembayaranGaji.tahun", thn))

							.createAlias("pegawai.satuanKerja", "satuanKerja", Criteria.LEFT_JOIN)

							.addOrder(Order.asc("satuanKerja.nama")).addOrder(Order.asc("pembayaranGaji.tahun"))
							.addOrder(Order.asc("pembayaranGaji.bulan")).addOrder(Order.asc("pegawai.nama"))

							.add(nama.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.or(Restrictions.ilike("pegawai.nama", nama, MatchMode.ANYWHERE),
											Restrictions.ilike("pegawai.code", nama, MatchMode.ANYWHERE)))

							.list();

					NumberFormat nf = new DecimalFormat("000");
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
									if (!blmMasukGrup.contains(kode)) {
										try {
											Double nilai = Double.parseDouble(
													jsonObject.get(itemGaji.getId().toString()).toString());
											if (nilai > 0.1) {
												List<ItemGaji> itemGajis = myItems.get(kode);
												if (itemGajis == null) {
													itemGajis = new ArrayList<ItemGaji>();
													myItems.put(kode, itemGajis);
												}
												itemGajis.add(itemGaji);
											}
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanRekapPembayaranGajiSatuanKerja.java:362");
											PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Pembayaran Gaji Satuan Kerja", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
												new String[] {
													"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
													"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
													"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
												});
										}
									}
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanRekapPembayaranGajiSatuanKerja.java:367");
							// TODO: handle exception
						}
					}

					String currentCaraPembayaranGaji = null;
					Map<Long, Map<String, Double>> datasSub = new HashMap<Long, Map<String, Double>>();

					for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {
						try {
							MyJSONObject jsonObject = new MyJSONObject(pembayaranGajiPunyaPegawai.getKomponenGaji());
							Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();

							label.setValue("Sedang memproses data " + pegawai.toString());

							Long idSatker = pegawai.getSatuanKerja() == null ? -1L : pegawai.getSatuanKerja().getId();

							Map<String, Double> data = datasSub.get(idSatker);
							if (data == null) {
								data = new HashMap();
								datasSub.put(idSatker, data);
							}

							for (String kode : myItems.keySet()) {
								List<ItemGaji> itemGajis = myItems.get(kode);
								Double nilaiTotal = 0.0;
								for (ItemGaji itemGaji : itemGajis) {

									if (!jsonObject.isNull(itemGaji.getId().toString())) {
										Double nilai = Double
												.parseDouble(jsonObject.get(itemGaji.getId().toString()).toString());

										nilaiTotal += nilai;
									}
								}

								Double totalSemua = data.get(kode);
								if (totalSemua == null) {
									totalSemua = 0.0;
								}

								totalSemua += nilaiTotal;
								data.put(kode, totalSemua);

							}

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Pembayaran Gaji Satuan Kerja", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
									new String[] {
										"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
										"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
										"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
									});
						}
					}

					ArrayList sub = new ArrayList();
					sub.add("**No.");
					sub.add("**Satuan Kerja");

					for (String kode : myItems.keySet()) {
						List<ItemGaji> itemGajis = myItems.get(kode);
						sub.add("**" + (!itemGajis.isEmpty() ? itemGajis.get(0).getNama() : "-"));
					}

					datas.add(sub);
					int nomor = 1;
					for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {

						String n = (pembayaranGajiPunyaPegawai.getPegawai().getSatuanKerja() == null ? "Tanpa Unit"
								: pembayaranGajiPunyaPegawai.getPegawai().getSatuanKerja().getNama());

						if (currentCaraPembayaranGaji == null || !currentCaraPembayaranGaji.equals(n)) {

							try {

								sub = new ArrayList();
								sub.add("**" + nomor);
								sub.add("**" + n);
								Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();
								Long idSatker = pegawai.getSatuanKerja() == null ? -1L
										: pegawai.getSatuanKerja().getId();
								Map<String, Double> data = datasSub.get(idSatker);

								for (String kode : myItems.keySet()) {
									sub.add(data == null || data.get(kode) == null ? 0.0
											: Common.numberFormat.get().format(data.get(kode)));
								}

								datas.add(sub);

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanRekapPembayaranGajiSatuanKerja.java:454");
							}

							currentCaraPembayaranGaji = n;
							nomor++;
						}

					}

					ais.action.report.helper.LoadingReportUtil.selesai(label);

				}
			}).start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Pembayaran Gaji Satuan Kerja", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
