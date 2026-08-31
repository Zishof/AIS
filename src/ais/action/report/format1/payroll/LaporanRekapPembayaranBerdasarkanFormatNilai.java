package ais.action.report.format1.payroll;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
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
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Bank;
import ais.database.model.Jabatan;
import ais.database.model.KehadiranPegawaiBulanan;
import ais.database.model.Pegawai;
import ais.database.model.StatusKepegawaian;
import ais.database.model.Tbmuser;
import ais.database.model.employ.GajiPokok;
import ais.database.model.employ.JabatanFungsional;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.Keluarga;
import ais.database.model.employ.KenaikanPangkat;
import ais.database.model.file.LampiranLain;
import ais.database.model.kpi.PenilaianKpi;
import ais.database.model.payroll.CaraPembayaranGaji;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.JenisFormatGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyJSONObject;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Penyusun/penyaji laporan untuk laporan rekap pembayaran berdasarkan format nilai. Kelas ini
 * mengubah data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa
 * memindahkan aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyTextbox searchnama}, {@code Combobox
 * bulan}, {@code Intbox tahun}, {@code Combobox caraBayar}, {@code AmbilDataSatuanKerjaBanbox searchparent},
 * {@code SatuanKerjaTreeModel satuanKerjaTreeModel}, {@code Center center}, {@code Rows rowsData};
 * inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onCetak()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapPembayaranBerdasarkanFormatNilai extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private MyTextbox searchnama = new MyTextbox();

	private Combobox bulan = new Combobox();
	private Intbox tahun = new Intbox(Calendar.getInstance().get(Calendar.YEAR));
	private Combobox caraBayar = new Combobox();
	private AmbilDataSatuanKerjaBanbox searchparent;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	private Center center;

	private Rows rowsData;
	private List<MyCheckboxConfig> checkboxConfigs = new ArrayList<MyCheckboxConfig>();

	private Combobox formatGaji;

	private Combobox bank;

	private Combobox statusKepegawaian;

	public LaporanRekapPembayaranBerdasarkanFormatNilai() throws Exception {
		super();
		init();
	}

	public LaporanRekapPembayaranBerdasarkanFormatNilai(String title, String border, boolean closable)
			throws Exception {
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
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

		Tbmuser tbmuser = Common.getCurrentUser();
		if (satuanKerja != null && tbmuser != null && tbmuser.hakAkses() != null
				&& !tbmuser.hakAkses().getMelihatDataSatkerLain()) {
			searchparent.setValue(satuanKerja.getNama());
			searchparent.setAttribute("satuanKerja", satuanKerja);
			searchparent.setAttribute("myValue", satuanKerja);
			searchparent.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Bank"));
		bank = new Combobox();
		vbox.appendChild(bank);
		Common.insertComboDanSemua(bank, new String[] { "nama" }, "keterangan", Bank.class, "Semua Bank",
				Restrictions.eq("aktif", true));
		bank.setCols(5);
		bank.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Status Kepegawaian"));
		statusKepegawaian = new Combobox();
		vbox.appendChild(statusKepegawaian);
		Common.insertComboDanSemua(statusKepegawaian, new String[] { "nama" }, "keterangan", StatusKepegawaian.class,
				"Semua Status", Restrictions.ne("nama", ""),
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		statusKepegawaian.setWidth("90%");
		statusKepegawaian.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		vbox = new Vbox();
		vbox.setParent(row);
		vbox.setWidth("100%");
		vbox.appendChild(new ais.ui.util.MyLabelConfig("Format Laporan *"));
		vbox.appendChild(formatGaji = new Combobox());
		formatGaji.setCols(5);
		formatGaji.setReadonly(true);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
				Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
				if (parent != null) {
					satuanKerjas.clear();
					satuanKerjas.add(parent);
					satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
				}

				Common.insertCombo(formatGaji, "nama", "keterangan", JenisFormatGaji.class, Restrictions.and(
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas)),
						Restrictions.eq("aktif", true)));
				formatGaji.setReadonly(true);
				if (formatGaji.getChildren().size() == 1) {
					formatGaji.setSelectedIndex(0);
				}
			}
		};

		Common.createDefaultTimer(eventListener);

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

		try {
			String namaFile = "format";

			North north = new org.zkoss.zul.North();
			north.setParent(borderlayout);
			north.appendChild(CommonReport.exportReport(new ParameterListener() {

				@SuppressWarnings({ "unchecked" })
				@Override
				public Map<String, Serializable> generateParameters() throws Exception {
					return parameters;
				}
			}, namaFile, null, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onCetak(null);
				}
			}, false));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanRekapPembayaranBerdasarkanFormatNilai.java:332");
			// TODO: handle exception
		}

		row = new MyFormRow();
		row.setParent(rows);

		rowsData = (Rows) Common.tampilanScroll1(row).getParent();

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

	}

	private TreeMap<String, List<ItemGaji>> myItems = new TreeMap<String, List<ItemGaji>>();
	private TreeMap<String, List<ItemGaji>> myItemsCopy = new TreeMap<String, List<ItemGaji>>();
	private Set<String> blmMasukGrup = new HashSet<String>();
	@SuppressWarnings("rawtypes")
	private Collection pangkats = ConstantValues.ambilBerdasarClass(KenaikanPangkat.class).values();
	@SuppressWarnings("rawtypes")
	private Map parameters = new HashMap();

	@SuppressWarnings("unchecked")
	private Collection<Keluarga> keluargas = ConstantValues.ambilBerdasarClass(Keluarga.class).values();

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetak(Event event) {

		try {
			parameters.clear();
			final File file;
			final JenisFormatGaji j = (JenisFormatGaji) (formatGaji.getSelectedItem() == null ? null
					: formatGaji.getSelectedItem().getValue());
			final Bank b = (Bank) (bank.getSelectedItem() == null ? null : bank.getSelectedItem().getValue());
			if (j != null) {
				LampiranLain lainMahasiswa = LampiranLain.ambil(j.getId(),
						LampiranLain.FILE_JRXML_LAYOUT_JENIS_FORMAT_GAJI);

				if (lainMahasiswa == null) {
					MyMessageboxConfig.show("File laporan format gaji belum diupload", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				file = lainMahasiswa.ambilFile();
					// EKSPOR DINAMIS: jrxml terupload per jenis dipakai utk SEMUA format cetak (PDF/XLS/DOCX/PPTX).
					if (file != null && file.exists()) { parameters.put("nama_laporan", file.getAbsolutePath()); }
			} else {
				MyMessageboxConfig.show("File laporan format gaji belum diupload", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return;
			}
			final Label label = Common.displayLoadBar(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(center);
					if (!j.getQueryManual()) {
						LaporanRekapPembayaranGaji.tampilanPilihaItem(blmMasukGrup, rowsData, myItemsCopy, myItems,
								checkboxConfigs);
					}
					File f = Report.generateCompileFileReport(Report.PDF, parameters, file.getAbsolutePath(),
							ais.ui.util.WaktuUtil.getDate());
					CommonReport.tampilkanReportPDF(center, f);

				}
			});

			new Thread(new Runnable() {

				private String maukkandataSatker(Session session, List<Map> maps, Map<String, Map> mapsTotalPerJenis,
						Date sekarang, PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai,
						String currentCaraPembayaranGaji, Integer bln, Integer thn) {
					try {
						MyJSONObject jsonObject = new MyJSONObject(pembayaranGajiPunyaPegawai.getKomponenGaji());
						Pegawai pegawai = pembayaranGajiPunyaPegawai.getPegawai();
						PenilaianKpi penilaianKpiData = PenilaianKpi.hitungKpi(session, pegawai, sekarang);
						List<KenaikanPangkat> kenaikanPangkats = pegawai.ambilKenaikanPangkat(sekarang, pangkats);
						JabatanFungsional jabatanFungsional = pegawai.ambilJabatanFungsional(kenaikanPangkats);
						JabatanStruktural jabatanStruktural = pegawai.ambilJabatanStruktural(kenaikanPangkats);
						Jabatan jabatan = pegawai.ambilJabatan(kenaikanPangkats);

						GajiPokok gajiPokok = pegawai.ambilGajiPokok(sekarang);
						Bank bank = pegawai.ambilBank(pembayaranGajiPunyaPegawai.getFormatItemGaji());

						if (b == null || (b != null && bank != null && b.getId().equals(bank.getId()))) {

							if (j.getPerKeluarga()) {

								Double totalPremi = pegawai.getAsuransiPegawai1() == null ? 0.0
										: pegawai.getAsuransiPegawai1().getTarif();
								Double totalPremiKeluarga = 0.0;
								for (Keluarga keluarga : keluargas) {

									if (keluarga.getPegawai() != null && keluarga.getStatus()
											&& keluarga.getPegawai().getId().equals(pegawai.getId())) {
										totalPremi += keluarga.getPremiAsuransi1();
										totalPremiKeluarga += keluarga.getPremiAsuransi1();
									}
								}

								for (Keluarga keluarga : keluargas) {

									if (keluarga.getPegawai() != null && keluarga.getStatus()
											&& keluarga.getPegawai().getId().equals(pegawai.getId())) {

										Map map = new HashMap();

										if (jabatanFungsional != null) {
											Common.insertProperty(JabatanFungsional.class, jabatanFungsional, map,
													"fungsional");
										}
										if (jabatanFungsional != null) {
											Common.insertProperty(JabatanStruktural.class, jabatanStruktural, map,
													"struktural");
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

										Common.insertProperty(PembayaranGajiPunyaPegawai.class,
												pembayaranGajiPunyaPegawai, map, "pembayaran.gaji", 1, "pegawai");

										Common.insertProperty(Pegawai.class, pegawai, map, "");

										label.setValue("Sedang memproses data " + pegawai.toString());
										ItemGaji itemGajidata = null;
										for (String kode : myItems.keySet()) {
											List<ItemGaji> itemGajis = myItems.get(kode);
											Double nilaiTotal = 0.0;
											for (ItemGaji itemGaji : itemGajis) {

												if (!jsonObject.isNull(itemGaji.getId().toString())) {
													Double nilai = Double.parseDouble(
															jsonObject.get(itemGaji.getId().toString()).toString());
													map.put("sub_item_gaji_nilai_" + itemGaji.getKode(), nilai);
													map.put("sub_item_gaji_kode_" + itemGaji.getKode(),
															itemGaji.getKode());
													map.put("sub_item_gaji_nama_" + itemGaji.getKode(),
															itemGaji.getNama());
													map.put("sub_item_gaji_formula_" + itemGaji.getKode(),
															itemGaji.getDefaultFormula());
													nilaiTotal += nilai;

													itemGajidata = itemGaji;
												}
											}

											map.put("total_item_gaji_nilai_" + kode, nilaiTotal);
											if (itemGajidata != null) {
												map.put("total_item_gaji_kode_" + kode, itemGajidata.getKode());
												map.put("total_item_gaji_nama_" + kode, itemGajidata.getNama());
												map.put("total_item_gaji_formula_" + kode,
														itemGajidata.getDefaultFormula());
											}

										}

										if (bank != null) {
											Common.insertProperty(Bank.class, bank, map, "bank");
										}

										if (keluarga != null) {
											Common.insertProperty(Keluarga.class, keluarga, map, "keluarga", 2,
													"pegawai");
										}

										map.put("totalPremi", totalPremi);
										map.put("totalPremiKeluarga", totalPremiKeluarga);

										maps.add(map);

									}

								}

							}

							else if (j.getPerBank()) {

								String n = (bank == null ? "-1" : bank.getId().toString());

								if (currentCaraPembayaranGaji == null || !currentCaraPembayaranGaji.equals(n)) {

									if (mapsTotalPerJenis != null) {

										Map map = new HashMap();

//										System.out.println("mapsTotalPerJenis -> " + mapsTotalPerJenis);

										Bank bankA = null;

										for (String kodeD : mapsTotalPerJenis.keySet()) {
											Map mapdata = mapsTotalPerJenis.get(kodeD);
											if (mapdata != null) {
												bankA = (Bank) mapdata.get("bank");

												Double totalSemuaPerJenis = (Double) mapdata.get("total_nilai");
												String total_item_gaji_kode = (String) mapdata
														.get("total_item_gaji_kode");
												String total_item_gaji_nama = (String) mapdata
														.get("total_item_gaji_nama");
												String total_item_gaji_formula = (String) mapdata
														.get("total_item_gaji_formula");

												map.put("total_nilai_" + kodeD, totalSemuaPerJenis);
												map.put("total_item_gaji_kode_" + kodeD, total_item_gaji_kode);
												map.put("total_item_gaji_nama_" + kodeD, total_item_gaji_nama);
												map.put("total_item_gaji_formula_" + kodeD, total_item_gaji_formula);

											}
										}

										if (bankA != null) {
											Common.insertProperty(Bank.class, bankA, map, "bank");
											Common.insertProperty(Bank.class, bankA, map, "");
											maps.add(map);
										}

									}

									mapsTotalPerJenis.clear();
									currentCaraPembayaranGaji = n;
								}

								for (String kode : myItems.keySet()) {
									List<ItemGaji> itemGajis = myItems.get(kode);

									for (ItemGaji itemGaji : itemGajis) {

										if (!jsonObject.isNull(itemGaji.getId().toString())) {
											Double nilai = Double.parseDouble(
													jsonObject.get(itemGaji.getId().toString()).toString());

											String kodeD = itemGaji.getKode();

											Map map = mapsTotalPerJenis.get(kodeD);
											if (map == null) {
												map = new HashMap();
												mapsTotalPerJenis.put(kodeD, map);
											}

											map.put("bank", bank);

											Double totalSemuaPerJenis = (Double) map.get("total_nilai");
											if (totalSemuaPerJenis == null) {
												totalSemuaPerJenis = 0.0;
											}
											totalSemuaPerJenis += nilai;
											map.put("total_nilai", totalSemuaPerJenis);
											map.put("total_item_gaji_kode", itemGaji.getKode());
											map.put("total_item_gaji_nama", itemGaji.getNama());
											map.put("total_item_gaji_formula", itemGaji.getDefaultFormula());

											if (pembayaranGajiPunyaPegawai.getPegawai() != null
													&& pembayaranGajiPunyaPegawai.getPegawai()
															.getTipeMasaKerja() != null) {
												String kodeUnik = "_tipe_masa_kerja_" + pembayaranGajiPunyaPegawai
														.getPegawai().getTipeMasaKerja().getId();
												map = mapsTotalPerJenis.get(kodeD + kodeUnik);
												if (map == null) {
													map = new HashMap();
													mapsTotalPerJenis.put(kodeD + kodeUnik, map);
												}
												map.put("bank", bank);
												totalSemuaPerJenis = (Double) map.get("total_nilai");
												if (totalSemuaPerJenis == null) {
													totalSemuaPerJenis = 0.0;
												}
												totalSemuaPerJenis += nilai;
												map.put("total_nilai", totalSemuaPerJenis);
												map.put("total_item_gaji_kode", itemGaji.getKode());
												map.put("total_item_gaji_nama", itemGaji.getNama());
												map.put("total_item_gaji_formula", itemGaji.getDefaultFormula());
											}

										}
									}

								}

							}

							else if (j.getPerSatker() || j.getPerSatkerFakultas() || j.getPerSatkerJurusan()
									|| j.getPerSatkerSekolah()) {

								SatuanKerja satuanKerja = pegawai.getSatuanKerja();
								if (j.getPerSatkerJurusan() && pegawai.getTendikJurusan() != null
										&& pegawai.getTendikJurusan().getSatuanKerja() != null) {
									satuanKerja = pegawai.getTendikJurusan().getSatuanKerja();
								} else if (j.getPerSatkerFakultas() && pegawai.getTendikFakultas() != null
										&& pegawai.getTendikFakultas().getSatuanKerja() != null) {
									satuanKerja = pegawai.getTendikFakultas().getSatuanKerja();
								} else if (j.getPerSatkerSekolah() && pegawai.getTendikSekolah() != null
										&& pegawai.getTendikSekolah().getSatuanKerja() != null) {
									satuanKerja = pegawai.getTendikSekolah().getSatuanKerja();
								}

								String n = (satuanKerja == null ? "-1" : satuanKerja.getId().toString());

								if (currentCaraPembayaranGaji == null || !currentCaraPembayaranGaji.equals(n)) {

									if (mapsTotalPerJenis != null) {

										Map map = new HashMap();

										System.out.println("mapsTotalPerJenis -> " + mapsTotalPerJenis);

										SatuanKerja satuanKerjaA = null;

										for (String kodeD : mapsTotalPerJenis.keySet()) {
											Map mapdata = mapsTotalPerJenis.get(kodeD);
											if (mapdata != null) {
												satuanKerjaA = (SatuanKerja) mapdata.get("satuanKerja");

												Double totalSemuaPerJenis = (Double) mapdata.get("total_nilai");
												String total_item_gaji_kode = (String) mapdata
														.get("total_item_gaji_kode");
												String total_item_gaji_nama = (String) mapdata
														.get("total_item_gaji_nama");
												String total_item_gaji_formula = (String) mapdata
														.get("total_item_gaji_formula");

//											if (kodeD.equalsIgnoreCase("THP_EKSKUL")) {
												System.out.println("totalSemuaPerJenis -> " + totalSemuaPerJenis
														+ ", kodeD -> " + kodeD + ", pegawai -> " + pegawai);
//											}

												map.put("total_nilai_" + kodeD, totalSemuaPerJenis);
												map.put("total_item_gaji_kode_" + kodeD, total_item_gaji_kode);
												map.put("total_item_gaji_nama_" + kodeD, total_item_gaji_nama);
												map.put("total_item_gaji_formula_" + kodeD, total_item_gaji_formula);

											}
										}

										if (satuanKerjaA != null) {
											Common.insertProperty(SatuanKerja.class, satuanKerjaA, map, "satuanKerja");
											Common.insertProperty(SatuanKerja.class, satuanKerjaA, map, "");
											maps.add(map);
										}

										if (bank != null) {
											Common.insertProperty(Bank.class, bank, map, "bank");
										}

									}

									mapsTotalPerJenis.clear();
									currentCaraPembayaranGaji = n;
								}

								for (String kode : myItems.keySet()) {
									List<ItemGaji> itemGajis = myItems.get(kode);

									for (ItemGaji itemGaji : itemGajis) {

										if (!jsonObject.isNull(itemGaji.getId().toString())) {
											Double nilai = Double.parseDouble(
													jsonObject.get(itemGaji.getId().toString()).toString());

											String kodeD = itemGaji.getKode();

											Map map = mapsTotalPerJenis.get(kodeD);
											if (map == null) {
												map = new HashMap();
												mapsTotalPerJenis.put(kodeD, map);
											}

											map.put("satuanKerja", satuanKerja);

											Double totalSemuaPerJenis = (Double) map.get("total_nilai");
											if (totalSemuaPerJenis == null) {
												totalSemuaPerJenis = 0.0;
											}
											totalSemuaPerJenis += nilai;
											map.put("total_nilai", totalSemuaPerJenis);
											map.put("total_item_gaji_kode", itemGaji.getKode());
											map.put("total_item_gaji_nama", itemGaji.getNama());
											map.put("total_item_gaji_formula", itemGaji.getDefaultFormula());

											if (pembayaranGajiPunyaPegawai.getPegawai() != null
													&& pembayaranGajiPunyaPegawai.getPegawai()
															.getTipeMasaKerja() != null) {
												String kodeUnik = "_tipe_masa_kerja_" + pembayaranGajiPunyaPegawai
														.getPegawai().getTipeMasaKerja().getId();
												map = mapsTotalPerJenis.get(kodeD + kodeUnik);
												if (map == null) {
													map = new HashMap();
													mapsTotalPerJenis.put(kodeD + kodeUnik, map);
												}
												map.put("satuanKerja", satuanKerja);
												totalSemuaPerJenis = (Double) map.get("total_nilai");
												if (totalSemuaPerJenis == null) {
													totalSemuaPerJenis = 0.0;
												}
												totalSemuaPerJenis += nilai;
												map.put("total_nilai", totalSemuaPerJenis);
												map.put("total_item_gaji_kode", itemGaji.getKode());
												map.put("total_item_gaji_nama", itemGaji.getNama());
												map.put("total_item_gaji_formula", itemGaji.getDefaultFormula());
											}

										}
									}

								}

							} else {

								KehadiranPegawaiBulanan kehadiranPegawaiBulanan = (KehadiranPegawaiBulanan) session
										.createCriteria(KehadiranPegawaiBulanan.class)
										.add(Restrictions.eq("bulan", bln)).add(Restrictions.eq("tahun", thn))
										.add(Restrictions.eq("pegawai.id", pegawai.getId())).setMaxResults(1)
										.uniqueResult();

								Map map = new HashMap();

								if (jabatanFungsional != null) {
									Common.insertProperty(JabatanFungsional.class, jabatanFungsional, map,
											"fungsional");
								}
								if (jabatanFungsional != null) {
									Common.insertProperty(JabatanStruktural.class, jabatanStruktural, map,
											"struktural");
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
											Double nilai = Double.parseDouble(
													jsonObject.get(itemGaji.getId().toString()).toString());
											map.put("sub_item_gaji_nilai_" + itemGaji.getKode(), nilai);
											map.put("sub_item_gaji_kode_" + itemGaji.getKode(), itemGaji.getKode());
											map.put("sub_item_gaji_nama_" + itemGaji.getKode(), itemGaji.getNama());
											map.put("sub_item_gaji_formula_" + itemGaji.getKode(),
													itemGaji.getDefaultFormula());
											nilaiTotal += nilai;

											itemGajidata = itemGaji;
										}
									}

									map.put("total_item_gaji_nilai_" + kode, nilaiTotal);
									if (itemGajidata != null) {
										map.put("total_item_gaji_kode_" + kode, itemGajidata.getKode());
										map.put("total_item_gaji_nama_" + kode, itemGajidata.getNama());
										map.put("total_item_gaji_formula_" + kode, itemGajidata.getDefaultFormula());
									}

								}

								if (bank != null) {
									Common.insertProperty(Bank.class, bank, map, "bank");
								}

								maps.add(map);
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Pembayaran Berdasarkan Format Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
					}

					return currentCaraPembayaranGaji;
				}

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

					StatusKepegawaian status = (statusKepegawaian.getSelectedItem() == null ? null
							: (StatusKepegawaian) statusKepegawaian.getSelectedItem().getValue());

					parameters.put("bank_nama", b == null ? null : b.getNama());
					parameters.put("bank_id", b == null || b.getId() == null ? -1L : b.getId());
					parameters.put("nama", nama);
					parameters.put("status", status == null ? "" : status.getNama());
					parameters.put("status_id", status == null || status.getId() == null ? -1L : status.getId());
					parameters.put("bln", bln);
					parameters.put("nama_bln", bln == null ? "" : Common.BULAN[bln - 1]);
					parameters.put("thn", thn);
					parameters.put("caraPembayaranGaji", caraPembayaranGaji == null || caraPembayaranGaji.getId() == null ? -1L : caraPembayaranGaji.getId());

					SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
					Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
					if (parent != null) {
						satuanKerjas.clear();
						satuanKerjas.add(parent);
						satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
					}

					parameters.put("satuan_kerja_id", parent == null || parent.getId() == null ? -1L : parent.getId());
					parameters.put("satuan_kerja", parent == null ? "" : parent.getNama());

					if (!j.getQueryManual()) {

						Session session = HibernateUtil.currentSession();
						List<PembayaranGajiPunyaPegawai> pembayaranGajiPunyaPegawais = session
								.createCriteria(PembayaranGajiPunyaPegawai.class)
								.createAlias("pembayaranGaji", "pembayaranGaji").createAlias("pegawai", "pegawai")

								.add(status == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("pegawai.statusKepegawaian", status))

								.add(b == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.eq("pegawai.bank", b),
												Restrictions.or(Restrictions.eq("pegawai.bank5", b),
														Restrictions.or(Restrictions.eq("pegawai.bank4", b),
																Restrictions.or(Restrictions.eq("pegawai.bank3", b),
																		Restrictions.eq("pegawai.bank2", b)))))

								)

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
								MyJSONObject jsonObject = new MyJSONObject(
										pembayaranGajiPunyaPegawai.getKomponenGaji());
								Iterator<String> iterator = jsonObject.keys();
								while (iterator.hasNext()) {
									String key = iterator.next();
									ItemGaji itemGaji = (ItemGaji) ConstantValues.ambil(ItemGaji.class.getName(),
											Long.parseLong(key));
									if (itemGaji != null) {
										String kode = nf.format(itemGaji.getNomorUrut()) + "-" + itemGaji.getKode();
										if (!blmMasukGrup.contains(kode)) {

											try {

												List<ItemGaji> itemGajis = myItems.get(kode);
												if (itemGajis == null) {
													itemGajis = new ArrayList<ItemGaji>();
													myItems.put(kode, itemGajis);
												}
												itemGajis.add(itemGaji);

											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/payroll/LaporanRekapPembayaranBerdasarkanFormatNilai.java:934");
												PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Pembayaran Berdasarkan Format Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
													new String[] {
														"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
														"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
														"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
													});
											}
										}
									}
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/payroll/LaporanRekapPembayaranBerdasarkanFormatNilai.java:939");
								// TODO: handle exception
							}
						}

						Date sekarang = WaktuUtil.getDate();

						String currentCaraPembayaranGaji = null;
						List<Map> maps = new ArrayList<Map>();
						Map<String, Map> mapsTotalPerJenis = new HashMap<String, Map>();
						PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawaiTerakhir = null;
						for (PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai : pembayaranGajiPunyaPegawais) {
							pembayaranGajiPunyaPegawaiTerakhir = pembayaranGajiPunyaPegawai;
							currentCaraPembayaranGaji = maukkandataSatker(session, maps, mapsTotalPerJenis, sekarang,
									pembayaranGajiPunyaPegawai, currentCaraPembayaranGaji, bln, thn);

						}

						if (j.getPerBank()) {
							if (mapsTotalPerJenis != null) {

								Map map = new HashMap();

								System.out.println("mapsTotalPerJenis -> " + mapsTotalPerJenis);

								Bank bankA = null;

								for (String kodeD : mapsTotalPerJenis.keySet()) {
									Map mapdata = mapsTotalPerJenis.get(kodeD);
									if (mapdata != null) {
										bankA = (Bank) mapdata.get("bank");

										Double totalSemuaPerJenis = (Double) mapdata.get("total_nilai");
										String total_item_gaji_kode = (String) mapdata.get("total_item_gaji_kode");
										String total_item_gaji_nama = (String) mapdata.get("total_item_gaji_nama");
										String total_item_gaji_formula = (String) mapdata
												.get("total_item_gaji_formula");

										map.put("total_nilai_" + kodeD, totalSemuaPerJenis);
										map.put("total_item_gaji_kode_" + kodeD, total_item_gaji_kode);
										map.put("total_item_gaji_nama_" + kodeD, total_item_gaji_nama);
										map.put("total_item_gaji_formula_" + kodeD, total_item_gaji_formula);

									}
								}

								if (bankA != null) {
									Common.insertProperty(Bank.class, bankA, map, "bank");
									Common.insertProperty(Bank.class, bankA, map, "");
									maps.add(map);
								}

							}
						}

						else if ((j.getPerSatker() || j.getPerSatkerFakultas() || j.getPerSatkerJurusan()
								|| j.getPerSatkerSekolah()) && pembayaranGajiPunyaPegawaiTerakhir != null) {

							if (mapsTotalPerJenis != null) {

								Map map = new HashMap();
								SatuanKerja satuanKerjaA = null;
								for (String kodeD : mapsTotalPerJenis.keySet()) {
									Map mapdata = mapsTotalPerJenis.get(kodeD);
									if (mapdata != null) {
										satuanKerjaA = (SatuanKerja) mapdata.get("satuanKerja");

										Double totalSemuaPerJenis = (Double) mapdata.get("total_nilai");
										String total_item_gaji_kode = (String) mapdata.get("total_item_gaji_kode");
										String total_item_gaji_nama = (String) mapdata.get("total_item_gaji_nama");
										String total_item_gaji_formula = (String) mapdata
												.get("total_item_gaji_formula");

//										if (kodeD.equalsIgnoreCase("THP_EKSKUL")) {
										System.out.println("totalSemuaPerJenis -> " + totalSemuaPerJenis + ", kodeD -> "
												+ kodeD + ", satuanKerja -> " + satuanKerjaA);
//										}

										map.put("total_nilai_" + kodeD, totalSemuaPerJenis);
										map.put("total_item_gaji_kode_" + kodeD, total_item_gaji_kode);
										map.put("total_item_gaji_nama_" + kodeD, total_item_gaji_nama);
										map.put("total_item_gaji_formula_" + kodeD, total_item_gaji_formula);

									}
								}
								if (satuanKerjaA != null) {
									Common.insertProperty(SatuanKerja.class, satuanKerjaA, map, "satuanKerja");
									Common.insertProperty(SatuanKerja.class, satuanKerjaA, map, "");
									maps.add(map);
								}

							}

						}

						parameters.put("maps", maps);

					}

					ais.action.report.helper.LoadingReportUtil.selesai(label);

				}
			}).start();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Rekap Pembayaran Berdasarkan Format Nilai", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
