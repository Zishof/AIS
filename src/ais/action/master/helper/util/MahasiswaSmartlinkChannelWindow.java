package ais.action.master.helper.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Group;
import org.zkoss.zul.Label;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswaBankOnline;
import ais.action.master.helper.virtualaccount.DownloadNoUjianCalonMahasiswaBankOnline;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankOnline;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.JadwalPembayaran;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIframe;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MahasiswaSmartlinkChannelWindow extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -126126249643138234L;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		Mahasiswa mahasiswa = (Mahasiswa) (execution.getParameter("mahasiswa") == null
				|| execution.getParameter("mahasiswa").equals("-1") ? null
						: ConstantValues.ambil(Mahasiswa.class.getName(),
								Long.parseLong(execution.getParameter("mahasiswa"))));
		BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) (execution.getParameter("calonMahasiswa") == null
				|| execution.getParameter("calonMahasiswa").equals("-1") ? null
						: ConstantValues.ambil(BiodataCalonMahasiswa.class.getName(),
								Long.parseLong(execution.getParameter("calonMahasiswa"))));

		Map param = new HashMap();
		param.put("smartlink", true);
		Double biayaAdmin = 0.0;
		BankHost bankHost = (BankHost) (execution.getParameter("bankHost") == null
				|| execution.getParameter("bankHost").equals("-1") ? null
						: ConstantValues.ambil(BankHost.class.getName(),
								Long.parseLong(execution.getParameter("bankHost"))));
		JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) (execution.getParameter("jadwalPembayaran") == null
				|| execution.getParameter("jadwalPembayaran").equals("-1") ? null
						: ConstantValues.ambil(JadwalPembayaran.class.getName(),
								Long.parseLong(execution.getParameter("jadwalPembayaran"))));
		Collection detailBiayas = new ArrayList();

		String keterangan = execution.getParameter("ket");
		String pemb = execution.getParameter("pemb");
		String cicilan = execution.getParameter("cicilan");
		Double total = Double.parseDouble(execution.getParameter("total"));

		Double tabungan = execution.getParameter("tabungan") == null ? 0.0
				: Double.parseDouble(execution.getParameter("tabungan"));
		
		Double topup = execution.getParameter("topup") == null ? 0.0
				: Double.parseDouble(execution.getParameter("topup"));

		Integer smt = Integer.parseInt(execution.getParameter("smt"));

		JSONArray items = new JSONArray(execution.getParameter("items"));

		MahasiswaSmartlinkChannelWindow.init((MyWindow) page.getFirstRoot(), mahasiswa, calonMahasiswa, smt,
				jadwalPembayaran, detailBiayas, param, biayaAdmin, tabungan, topup, bankHost, keterangan, pemb, cicilan, total,
				items, false);
	}

	@SuppressWarnings({ "rawtypes", "deprecation", "unchecked" })
	public static void initBanyak(final MyWindow window, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa calonMahasiswa, final Set<KegiatanTemporary> selectedKegiatanTemporary,
			final Map param, final Double biayaAdmin, final BankHost bankHost, final String pemb, final String cicilan,
			final Double total, final boolean tampilBatal) {
		param.put("jangan_notif", true);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		final Center center = new Center();
		center.setTitle("Proses Pembayaran Online");
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (mahasiswa != null) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
			row.appendChild(new MyLabelBold(mahasiswa.getNim()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
			row.appendChild(new MyLabelBold(mahasiswa.getNama()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
			row.appendChild(new MyLabelBold(mahasiswa.getJurusan().getNama()));

		} else if (calonMahasiswa != null) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("No. Reg"));
			row.appendChild(new MyLabelBold(calonMahasiswa.getNoRegistrasi()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(new MyLabelBold(calonMahasiswa.getNama()));

			if (calonMahasiswa.getGelombangPendaftaran() != null) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang"));
				row.appendChild(new MyLabelBold(calonMahasiswa.getGelombangPendaftaran().getNama()));
			}
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tagihan"));
		row.appendChild(new Label(pemb));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Total Tagihan"));
//		aaaa.setStyle("font-size:16pxfont-weight: bolder;");
		MyLabelBold aaa;
		row.appendChild(aaa = new MyLabelBold(Common.numberFormat.get().format(total)));
		aaa.setStyle("font-size:16pxfont-weight: bolder;");

		Group group = new Group("Pilih Channel Pembayaran");
		group.setParent(rows);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		final MyLabelBold totalPembayaran = new MyLabelBold();
		final Radiogroup radiogroup = new Radiogroup();

		final Double mytotal = total;

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Radio selected = radiogroup.getSelectedItem();

				if (selected != null) {
					Double biaya_admin = (Double) selected.getAttribute("biaya_admin");

					totalPembayaran.setValue(Common.numberFormat.get().format(mytotal + biaya_admin));
				}

			}
		};

		radiogroup.setOrient("vertical");
		row.appendChild(radiogroup);

		String variableSmartlink = Common.getKonfigurasi("channel_biaya_e_smartlink",
				"VA_BNI:2500:BNI;VA_BRI:2500:BRI;VA_BCA:3500:BCA;VA_BNC:3500:BNC(Bank Neo Commerce);VA_CIMB:2500:CIMB Niaga;VA_MANDIRI:3500:Bank Mandiri;VA_PERMATA:2500:Bank Permata;VA_BSI:3000:BSI;VA_DANAMON:3000:Danamon;OTC_ALFAMART:3000:Alfamart;OTC_INDOMARET:3000:Indomart")
				.getNilai();

		/* Parsing toleran terpusat di SmartlinkChannelWindow.isiRadioKanal:
		 * entri konfigurasi yang tidak lengkap dilewati, tidak meledak
		 * ArrayIndexOutOfBoundsException. */
		SmartlinkChannelWindow.isiRadioKanal(radiogroup, variableSmartlink, eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Total Bayar"));
		row.appendChild(totalPembayaran);
		totalPembayaran.setStyle("font-size:16pxfont-weight: bolder;");

		boolean ditampilkanDefaultBayarWaktu = Common
				.getKonfigurasi("jangka_waktu_default_ditampilkan", Konfigurasi.AKTIF).getNilai().trim()
				.equalsIgnoreCase(Konfigurasi.AKTIF);
		String defaultBayarWaktu = Common.getKonfigurasi("jangka_waktu_default", "").getNilai().trim();

		group = new Group("Saya akan membayar tagihan ini dalam jangka waktu :");
		group.setParent(rows);

		row = new MyFormRow();
		row.setVisible(!ditampilkanDefaultBayarWaktu && !defaultBayarWaktu.isEmpty());
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new MyLabelBold(defaultBayarWaktu));

		row = new MyFormRow();
		row.setVisible(ditampilkanDefaultBayarWaktu);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		final Radiogroup waktuBayarDalam;
		row.appendChild(waktuBayarDalam = new Radiogroup());
		waktuBayarDalam.setOrient("vertical");
		Radio radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_15_MENIT);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_15_MENIT);
		radio.setValue(SmartlinkChannelWindow.WAKTU_15_MENIT);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_30_MENIT);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_30_MENIT);
		radio.setValue(SmartlinkChannelWindow.WAKTU_30_MENIT);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_1_JAM);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_1_JAM);
		radio.setValue(SmartlinkChannelWindow.WAKTU_1_JAM);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_3_JAM);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_3_JAM);
		radio.setValue(SmartlinkChannelWindow.WAKTU_3_JAM);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_6_JAM);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_6_JAM);
		radio.setValue(SmartlinkChannelWindow.WAKTU_6_JAM);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_24_JAM);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_24_JAM);
		radio.setValue(SmartlinkChannelWindow.WAKTU_24_JAM);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_3_HARI);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_3_HARI);
		radio.setValue(SmartlinkChannelWindow.WAKTU_3_HARI);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_1_MINGGU);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_1_MINGGU);
		radio.setValue(SmartlinkChannelWindow.WAKTU_1_MINGGU);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_1_BULAN);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_1_BULAN);
		radio.setValue(SmartlinkChannelWindow.WAKTU_1_BULAN);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);

		if (!defaultBayarWaktu.isEmpty()) {
			Common.selectRadioItem(waktuBayarDalam, defaultBayarWaktu);
		}

		if (tampilBatal) {

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(south);

			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setStyle("font-size:20pxfont-weight: bolder;");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses Bayar", "/img/save.gif");
			save.setStyle("font-size:20pxfont-weight: bolder;");
			save.setTooltiptext("Proses");
			save.setAttribute("janganDisabled", true);
			save.setParent(toolbar);
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Radio selected = radiogroup.getSelectedItem();

					if (selected != null) {
						Double biaya_admin = (Double) selected.getAttribute("biaya_admin");
						String channel = (String) selected.getAttribute("channel");
						param.put("esmartlinkBayarVia", channel);
						param.put("update", true);
						param.remove("smartlink_direct");
						param.put("pemb", pemb);
						param.put("cicilan", cicilan);
						param.put("total", total);

						List<String> warnings = new ArrayList<String>();
						param.put("warnings", warnings);

						String val = waktuBayarDalam.getSelectedItem() == null ? null
								: (String) waktuBayarDalam.getSelectedItem().getValue();
						if (val != null) {

							PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

							VirtualAccountBank virtualAccountBank = DownloadTagihanMahasiswaBankOnline.sendRequest(
									mahasiswa, calonMahasiswa, selectedKegiatanTemporary, biaya_admin, perguruanTinggi,
									bankHost, param, val);

							if (virtualAccountBank != null && !virtualAccountBank.getLink().isEmpty()) {
								window.detach();
								Common.displayWindowIframe(virtualAccountBank.getLink(), true, "600px", "95%",
										"Pembayaran Online");
							} else {
								MyMessageboxConfig.show(
										"Transaksi gagal dilakukan"
												+ (warnings.isEmpty() ? "" : "\n\n" + warnings.toString()),
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						} else {
							MyMessageboxConfig.show("Pilihlah batas waktu paling lambat Anda akan melakukan pembayaran",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					} else {
						MyMessageboxConfig.show("Pilih salah satu channel pembayaran", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}

				}
			});
		} else {
			MyFormRow rowPilih = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(rowPilih, "2");
			rowPilih.setParent(rows);

			MyButtonConfig save = new MyButtonConfig("Proses Bayar", "/img/save.gif");
			save.setStyle("font-size:20pxfont-weight: bolder;");
			save.setTooltiptext("Proses");
			save.setAttribute("janganDisabled", true);
			save.setParent(rowPilih);
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Radio selected = radiogroup.getSelectedItem();

					if (selected != null) {
						Double biaya_admin = (Double) selected.getAttribute("biaya_admin");
						String channel = (String) selected.getAttribute("channel");
						param.put("esmartlinkBayarVia", channel);
						param.remove("smartlink_direct");
						param.put("update", true);

						param.put("pemb", pemb);
						param.put("cicilan", cicilan);
						param.put("total", total);
						List<String> warnings = new ArrayList<String>();
						param.put("warnings", warnings);

						String val = waktuBayarDalam.getSelectedItem() == null ? null
								: (String) waktuBayarDalam.getSelectedItem().getValue();
						if (val != null) {

							PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

							VirtualAccountBank virtualAccountBank = DownloadTagihanMahasiswaBankOnline.sendRequest(
									mahasiswa, calonMahasiswa, selectedKegiatanTemporary, biaya_admin, perguruanTinggi,
									bankHost, param, val);

							if (virtualAccountBank != null && !virtualAccountBank.getLink().isEmpty()) {

								Common.clear(center);

								MyIframe myIframe = new MyIframe(virtualAccountBank.getLink());
								myIframe.setScrolling("yes");
								myIframe.setWidth("100%");
								myIframe.setHeight("100%");
								myIframe.setStyle("height:100%;width:100%");
								center.appendChild(myIframe);

							} else {
								MyMessageboxConfig.show(
										"Transaksi gagal dilakukan"
												+ (warnings.isEmpty() ? "" : "\n\n" + warnings.toString()),
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						} else {
							MyMessageboxConfig.show("Pilihlah batas waktu paling lambat Anda akan melakukan pembayaran",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					} else {
						MyMessageboxConfig.show("Pilih salah satu channel pembayaran", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}

				}
			});
		}

	}

	@SuppressWarnings({ "rawtypes", "deprecation", "unchecked" })
	public static void init(final MyWindow window, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa calonMahasiswa, final Integer smt, final JadwalPembayaran myjadwalPembayaran,
			final Collection detailBiayas, final Map param, final Double biayaAdmin, final Double tabungan, final Double topup,
			final BankHost bankHost, final String ket, final String pemb, final String cicilan, final Double total,
			final JSONArray items, final boolean tampilBatal) {
		param.put("jangan_notif", true);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		final Center center = new Center();
		center.setTitle("Proses Pembayaran Online");
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		if (mahasiswa != null) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
			row.appendChild(new MyLabelBold(mahasiswa.getNim()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
			row.appendChild(new MyLabelBold(mahasiswa.getNama()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
			row.appendChild(new MyLabelBold(mahasiswa.getJurusan().getNama()));

		} else if (calonMahasiswa != null) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("No. Reg"));
			row.appendChild(new MyLabelBold(calonMahasiswa.getNoRegistrasi()));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
			row.appendChild(new MyLabelBold(calonMahasiswa.getNama()));

			if (calonMahasiswa.getGelombangPendaftaran() != null) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang"));
				row.appendChild(new MyLabelBold(calonMahasiswa.getGelombangPendaftaran().getNama()));
			}
		}

		if (pemb != null && !pemb.trim().isEmpty()) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tagihan"));
			row.appendChild(new Label(pemb));
		}

		if (tabungan != null && tabungan > 0.1) {
			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Topup"));
			row.appendChild(new Label(Common.numberFormat.get().format(tabungan)));
		}

		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Total Tagihan"));
//		aaaa.setStyle("font-size:16pxfont-weight: bolder;");
		MyLabelBold aaa;
		row.appendChild(aaa = new MyLabelBold(Common.numberFormat.get().format(total)));
		aaa.setStyle("font-size:16pxfont-weight: bolder;");

		Group group = new Group("Pilih Channel Pembayaran");
		group.setParent(rows);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		final MyLabelBold totalPembayaran = new MyLabelBold();
		final Radiogroup radiogroup = new Radiogroup();

		final Double mytotal = total;

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Radio selected = radiogroup.getSelectedItem();

				if (selected != null) {
					Double biaya_admin = (Double) selected.getAttribute("biaya_admin");

					totalPembayaran.setValue(Common.numberFormat.get().format(mytotal + biaya_admin));
				}

			}
		};

		radiogroup.setOrient("vertical");
		row.appendChild(radiogroup);

		String variableSmartlink = Common.getKonfigurasi("channel_biaya_e_smartlink",
				"VA_BNI:2500:BNI;VA_BRI:2500:BRI;VA_BCA:3500:BCA;VA_BNC:3500:BNC(Bank Neo Commerce);VA_CIMB:2500:CIMB Niaga;VA_MANDIRI:3500:Bank Mandiri;VA_PERMATA:2500:Bank Permata;VA_BSI:3000:BSI;VA_DANAMON:3000:Danamon;OTC_ALFAMART:3000:Alfamart;OTC_INDOMARET:3000:Indomart")
				.getNilai();

		if (myjadwalPembayaran != null && myjadwalPembayaran.getJenisKegiatan() != null
				&& myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran() != null) {
			variableSmartlink = myjadwalPembayaran.getJenisKegiatan().getKanalPembayaran()
					.getVariableBiayaAdminEsmartlink();
		}

		/* Parsing toleran terpusat di SmartlinkChannelWindow.isiRadioKanal:
		 * entri konfigurasi yang tidak lengkap dilewati, tidak meledak
		 * ArrayIndexOutOfBoundsException. */
		SmartlinkChannelWindow.isiRadioKanal(radiogroup, variableSmartlink, eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Total Bayar"));
		row.appendChild(totalPembayaran);
		totalPembayaran.setStyle("font-size:16pxfont-weight: bolder;");

		boolean ditampilkanDefaultBayarWaktu = Common
				.getKonfigurasi("jangka_waktu_default_ditampilkan", Konfigurasi.AKTIF).getNilai().trim()
				.equalsIgnoreCase(Konfigurasi.AKTIF);
		String defaultBayarWaktu = Common.getKonfigurasi("jangka_waktu_default", "").getNilai().trim();

		group = new Group("Saya akan membayar tagihan ini dalam jangka waktu :");
		group.setParent(rows);

		row = new MyFormRow();
		row.setVisible(!ditampilkanDefaultBayarWaktu && !defaultBayarWaktu.isEmpty());
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new MyLabelBold(defaultBayarWaktu));

		row = new MyFormRow();
		row.setVisible(ditampilkanDefaultBayarWaktu);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		final Radiogroup waktuBayarDalam;
		row.appendChild(waktuBayarDalam = new Radiogroup());
		waktuBayarDalam.setOrient("vertical");
		Radio radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_15_MENIT);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_15_MENIT);
		radio.setValue(SmartlinkChannelWindow.WAKTU_15_MENIT);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_30_MENIT);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_30_MENIT);
		radio.setValue(SmartlinkChannelWindow.WAKTU_30_MENIT);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_1_JAM);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_1_JAM);
		radio.setValue(SmartlinkChannelWindow.WAKTU_1_JAM);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_3_JAM);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_3_JAM);
		radio.setValue(SmartlinkChannelWindow.WAKTU_3_JAM);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_6_JAM);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_6_JAM);
		radio.setValue(SmartlinkChannelWindow.WAKTU_6_JAM);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_24_JAM);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_24_JAM);
		radio.setValue(SmartlinkChannelWindow.WAKTU_24_JAM);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_3_HARI);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_3_HARI);
		radio.setValue(SmartlinkChannelWindow.WAKTU_3_HARI);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_1_MINGGU);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_1_MINGGU);
		radio.setValue(SmartlinkChannelWindow.WAKTU_1_MINGGU);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);
		radio = new MyRadioConfig(SmartlinkChannelWindow.WAKTU_1_BULAN);
		radio.setAttribute("value", SmartlinkChannelWindow.WAKTU_1_BULAN);
		radio.setValue(SmartlinkChannelWindow.WAKTU_1_BULAN);
		radio.setStyle("font-size:12px;");
		waktuBayarDalam.appendChild(radio);

		if (!defaultBayarWaktu.isEmpty()) {
			Common.selectRadioItem(waktuBayarDalam, defaultBayarWaktu);
		}

		if (tampilBatal) {

			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(south);

			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setStyle("font-size:20pxfont-weight: bolder;");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					window.detach();
				}
			});
			cancel.setParent(toolbar);

			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Proses Bayar", "/img/save.gif");
			save.setStyle("font-size:20pxfont-weight: bolder;");
			save.setTooltiptext("Proses");
			save.setAttribute("janganDisabled", true);
			save.setParent(toolbar);
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Radio selected = radiogroup.getSelectedItem();

					if (selected != null) {
						Double biaya_admin = (Double) selected.getAttribute("biaya_admin");
						String channel = (String) selected.getAttribute("channel");
						param.put("esmartlinkBayarVia", channel);
						param.put("update", true);
						param.remove("smartlink_direct");
						param.put("pemb", pemb);
						param.put("ket", ket);
						param.put("cicilan", cicilan);
						// Bersihkan admin lama dari items, masukkan admin channel yang dipilih saja
						JSONArray itemsChannel = new JSONArray();
						for (int ix = 0; ix < (items == null ? 0 : items.length()); ix++) {
							try {
								if (!"Biaya Admin".equals(items.getJSONObject(ix).optString("description"))) {
									itemsChannel.put(items.getJSONObject(ix));
								}
							} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/util/MahasiswaSmartlinkChannelWindow.java:721");
							}
						}
						if (biaya_admin != null && biaya_admin.intValue() > 0) {
							JSONObject adminItem = new JSONObject();
							adminItem.put("description", "Biaya Admin");
							adminItem.put("unitPrice", biaya_admin.intValue());
							adminItem.put("qty", 1);
							adminItem.put("amount", biaya_admin.intValue());
							itemsChannel.put(adminItem);
						}
						// Hitung ulang total dari item tagihan (non-admin) di itemsChannel.
						// downloadData() akan menambah biayaAdmin ke total ini → mytotal = totalHitung + biayaAdmin
						double totalHitung = 0.0;
						for (int ix = 0; ix < itemsChannel.length(); ix++) {
							try {
								JSONObject it = itemsChannel.getJSONObject(ix);
								if (!"Biaya Admin".equals(it.optString("description"))) {
									totalHitung += it.optDouble("amount", 0.0);
								}
							} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/util/MahasiswaSmartlinkChannelWindow.java:741");
							}
						}
						param.put("total", totalHitung);
						param.put("items", itemsChannel);

						List<String> warnings = new ArrayList<String>();
						param.put("warnings", warnings);

						String val = waktuBayarDalam.getSelectedItem() == null ? null
								: (String) waktuBayarDalam.getSelectedItem().getValue();
						if (val != null) {

							VirtualAccountBank virtualAccountBank = null;
							if (mahasiswa != null) {
								virtualAccountBank = DownloadTagihanMahasiswaBankOnline.downloadData(mahasiswa, smt,
										myjadwalPembayaran, detailBiayas, null, param, biaya_admin, tabungan, topup, bankHost,
										val);
							} else if (calonMahasiswa != null && smt.equals(0)) {
								virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline.downloadData(
										calonMahasiswa, myjadwalPembayaran, detailBiayas, param, biaya_admin, bankHost,
										val);
							} else if (calonMahasiswa != null) {
								virtualAccountBank = DownloadNoUjianCalonMahasiswaBankOnline.downloadData(
										calonMahasiswa, myjadwalPembayaran, detailBiayas, null, smt, param, biaya_admin,
										bankHost);
							}

							if (virtualAccountBank != null && !virtualAccountBank.getLink().isEmpty()) {
								window.detach();
								Common.displayWindowIframe(virtualAccountBank.getLink(), true, "600px", "95%",
										"Pembayaran Online");
							} else {
								MyMessageboxConfig.show(
										"Transaksi gagal dilakukan"
												+ (warnings.isEmpty() ? "" : "\n\n" + warnings.toString()),
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}
						} else {
							MyMessageboxConfig.show("Pilihlah batas waktu paling lambat Anda akan melakukan pembayaran",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					} else {
						MyMessageboxConfig.show("Pilih salah satu channel pembayaran", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}

				}
			});
		} else {
			MyFormRow rowPilih = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(rowPilih, "2");
			rowPilih.setParent(rows);

			MyButtonConfig save = new MyButtonConfig("Proses Bayar", "/img/save.gif");
			save.setStyle("font-size:20pxfont-weight: bolder;");
			save.setTooltiptext("Proses");
			save.setAttribute("janganDisabled", true);
			save.setParent(rowPilih);
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Radio selected = radiogroup.getSelectedItem();

					if (selected != null) {
						Double biaya_admin = (Double) selected.getAttribute("biaya_admin");
						String channel = (String) selected.getAttribute("channel");
						param.put("esmartlinkBayarVia", channel);
						param.remove("smartlink_direct");
						param.put("update", true);

						param.put("pemb", pemb);
						param.put("ket", ket);
						param.put("cicilan", cicilan);
						// Bersihkan admin lama dari items, masukkan admin channel yang dipilih saja
						JSONArray itemsChannel = new JSONArray();
						for (int ix = 0; ix < (items == null ? 0 : items.length()); ix++) {
							try {
								if (!"Biaya Admin".equals(items.getJSONObject(ix).optString("description"))) {
									itemsChannel.put(items.getJSONObject(ix));
								}
							} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/util/MahasiswaSmartlinkChannelWindow.java:823");
							}
						}
						if (biaya_admin != null && biaya_admin.intValue() > 0) {
							JSONObject adminItem = new JSONObject();
							adminItem.put("description", "Biaya Admin");
							adminItem.put("unitPrice", biaya_admin.intValue());
							adminItem.put("qty", 1);
							adminItem.put("amount", biaya_admin.intValue());
							itemsChannel.put(adminItem);
						}
						// Hitung ulang total dari item tagihan (non-admin) di itemsChannel.
						// downloadData() akan menambah biayaAdmin ke total ini → mytotal = totalHitung + biayaAdmin
						double totalHitung = 0.0;
						for (int ix = 0; ix < itemsChannel.length(); ix++) {
							try {
								JSONObject it = itemsChannel.getJSONObject(ix);
								if (!"Biaya Admin".equals(it.optString("description"))) {
									totalHitung += it.optDouble("amount", 0.0);
								}
							} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/util/MahasiswaSmartlinkChannelWindow.java:843");
							}
						}
						param.put("total", totalHitung);
						param.put("items", itemsChannel);

						List<String> warnings = new ArrayList<String>();
						param.put("warnings", warnings);

						VirtualAccountBank virtualAccountBank = null;
						if (mahasiswa != null) {
							virtualAccountBank = DownloadTagihanMahasiswaBankOnline.downloadData(mahasiswa, smt,
									myjadwalPembayaran, detailBiayas, null, param, biaya_admin, tabungan, topup, bankHost);
						} else if (calonMahasiswa != null && smt.equals(0)) {
							virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline.downloadData(
									calonMahasiswa, myjadwalPembayaran, detailBiayas, param, biaya_admin, bankHost);
						} else if (calonMahasiswa != null) {
							virtualAccountBank = DownloadNoUjianCalonMahasiswaBankOnline.downloadData(calonMahasiswa,
									myjadwalPembayaran, detailBiayas, null, smt, param, biaya_admin, bankHost);
						}

						if (virtualAccountBank != null && !virtualAccountBank.getLink().isEmpty()) {

							Common.clear(center);

							MyIframe myIframe = new MyIframe(virtualAccountBank.getLink());
							myIframe.setScrolling("yes");
							myIframe.setWidth("100%");
							myIframe.setHeight("100%");
							myIframe.setStyle("height:100%;width:100%");
							center.appendChild(myIframe);

						} else {
							MyMessageboxConfig.show(
									"Transaksi gagal dilakukan"
											+ (warnings.isEmpty() ? "" : "\n\n" + warnings.toString()),
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						}
					} else {
						MyMessageboxConfig.show("Pilih salah satu channel pembayaran", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}

				}
			});
		}

	}
}
