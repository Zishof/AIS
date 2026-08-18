package ais.action.master.pmb;

import java.io.File;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara;
import ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswaBankBjb;
import ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswaBankBtn;
import ais.action.master.helper.virtualaccount.DownloadNoRegistrasiCalonMahasiswaBankOnline;
import ais.action.master.helper.virtualaccount.DownloadNoUjianCalonMahasiswaBankNtt;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BarcodeCommon;
import ais.common.BniCommon;
import ais.common.BsiCommon;
import ais.common.CimbCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.DokuCommon;
import ais.common.FaspayCommon;
import ais.common.FinpayCommon;
import ais.common.IndonesianNumberToWords;
import ais.common.IpaymuCommon;
import ais.common.JatelindoCommon;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyWindow;

public class TampilanPaymentGateway {

	private static boolean bayarViaFinpay;
	private static boolean bayarViaIpaymu;
	private static boolean bayarViaFaspay;
	private static boolean bayarViaCimb;
	private static boolean bayarViaDoku;
	private static boolean bayarViaBni;
	private static boolean bayarViaBsi;
	private static boolean bayarViaMandiri;
	private static boolean aktifkan_pembayaran_via_bank_ntt;
	private static boolean aktifkan_pembayaran_via_bank_online;
	private static boolean aktifkan_pembayaran_via_bank_online_2;
	private static boolean aktifkan_pembayaran_via_bank_online_smartlink;
	private static boolean aktifkan_pembayaran_via_bank_qris;
	private static boolean aktifkan_pembayaran_via_bank_finpay;
	private static boolean aktifkan_pembayaran_via_bank_flip;
	private static boolean aktifkan_pembayaran_via_bank_btn;
	private static boolean aktifkan_pembayaran_via_bank_bjb;
	private static boolean aktifkan_pembayaran_via_bank_otto;
	private static boolean aktifkan_pembayaran_via_bank_bankaltimtara;
	private static boolean aktifkan_pembayaran_via_bank_briva;
	private static boolean aktifkan_pembayaran_via_bank_maja;

	public static void tampilPembayaranDaftarUlang(final BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		Common.displayWindow(
				"/common/daftarulang_mahasiswa_baru.zul?biodataCalonMahasiswa=" + biodataCalonMahasiswa.getId(), false,
				"2200px", null);
	}

	public static boolean adaPembayaranRegistrasi() {
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		bayarViaFinpay = Common.bolehKonfigurasi("aktifkan_pembayaran_via_finpay", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_finpay_" + perguruanTinggi.getId());
		bayarViaIpaymu = Common.bolehKonfigurasi("aktifkan_pembayaran_via_ipaymu", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_ipaymu_" + perguruanTinggi.getId());
		bayarViaFaspay = Common.bolehKonfigurasi("aktifkan_pembayaran_via_faspay", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_faspay_" + perguruanTinggi.getId());
		bayarViaCimb = Common.bolehKonfigurasi("aktifkan_pembayaran_via_cimb", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_cimb_" + perguruanTinggi.getId());
		bayarViaDoku = Common.bolehKonfigurasi("aktifkan_pembayaran_via_doku", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_doku_" + perguruanTinggi.getId());
		bayarViaBni = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bni", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bni_pt_" + perguruanTinggi.getId());
		bayarViaBsi = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bsi", Konfigurasi.TIDAK_AKTIF);
		bayarViaMandiri = Common.bolehKonfigurasi("aktifkan_pembayaran_via_jatelindo", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_jatelindo_pt_" + perguruanTinggi.getId());
		aktifkan_pembayaran_via_bank_ntt = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_ntt", Konfigurasi.TIDAK_AKTIF);

		aktifkan_pembayaran_via_bank_online = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_pt_" + perguruanTinggi.getId());

		aktifkan_pembayaran_via_bank_maja = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_maja", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_maja_pt_" + perguruanTinggi.getId());

		aktifkan_pembayaran_via_bank_online_2 = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_2", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_2_pt_" + perguruanTinggi.getId());

		aktifkan_pembayaran_via_bank_online_smartlink = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_smartlink_2", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_online_smartlink_2_pt_" + perguruanTinggi.getId());

		aktifkan_pembayaran_via_bank_qris = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_qris", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_qris_" + perguruanTinggi.getId());

		aktifkan_pembayaran_via_bank_finpay = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_finpay", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_finpay_" + perguruanTinggi.getId());

		aktifkan_pembayaran_via_bank_flip = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_flip", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_flip_" + perguruanTinggi.getId());

		aktifkan_pembayaran_via_bank_otto = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_otto", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_otto_" + perguruanTinggi.getId());

		aktifkan_pembayaran_via_bank_briva = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_briva", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_briva_" + perguruanTinggi.getId());

		aktifkan_pembayaran_via_bank_btn = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_btn", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_btn_pt_" + perguruanTinggi.getId());

		aktifkan_pembayaran_via_bank_bankaltimtara = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_bankaltimtara", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_bankaltimtara_" + perguruanTinggi.getId());

		aktifkan_pembayaran_via_bank_bjb = Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_bjb", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("aktifkan_pembayaran_via_bank_bjb_" + perguruanTinggi.getId());

		return bayarViaFinpay || bayarViaIpaymu || bayarViaDoku || bayarViaBni || bayarViaBsi || bayarViaMandiri
				|| bayarViaFaspay || bayarViaCimb || aktifkan_pembayaran_via_bank_online
				|| aktifkan_pembayaran_via_bank_maja || aktifkan_pembayaran_via_bank_online_2
				|| aktifkan_pembayaran_via_bank_online_smartlink || aktifkan_pembayaran_via_bank_qris
				|| aktifkan_pembayaran_via_bank_ntt || aktifkan_pembayaran_via_bank_btn
				|| aktifkan_pembayaran_via_bank_bjb || aktifkan_pembayaran_via_bank_finpay
				|| aktifkan_pembayaran_via_bank_flip || aktifkan_pembayaran_via_bank_otto
				|| aktifkan_pembayaran_via_bank_bankaltimtara || aktifkan_pembayaran_via_bank_briva;
	}

	public static void tampilPembayaranRegistrasi(final BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {

		if (adaPembayaranRegistrasi() && (biodataCalonMahasiswa.getPembayaranRegistrasi() == null
				|| (biodataCalonMahasiswa.getPembayaranRegistrasi() != null
						&& biodataCalonMahasiswa.getPembayaranRegistrasi().getPersentaseLunas() < 99.0))) {

			final JenisKegiatan jenisKegiatan = ConstantValues.PENDAFTARAN_CALON_MAHASISWA;
			Jurusan prodiLulus = biodataCalonMahasiswa.getProdiLulus();
			final List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
			if (prodiLulus == null || prodiLulus.getId() == null) {
				Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
						: biodataCalonMahasiswa.getProdi1();
				java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
						.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, true);
				detailBiayas.addAll(detailBiayas1);
			} else {
				java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtilHelper
						.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, prodiLulus, true);
				detailBiayas.addAll(detailBiayas1);
			}

			Double nilaiBiayaHarusDiBayars = 0.0;

			for (DetailBiaya detailBiaya : detailBiayas) {
				nilaiBiayaHarusDiBayars += (detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
						: detailBiaya.getNilaiBiayaBaru());
			}

			if (nilaiBiayaHarusDiBayars > 0.1) {

				final MyWindow window = new MyWindow("Proses Pembayaran Calon Mahasiswa", "none", true);
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				window.setHeight("40%");
				window.setWidth("800px");
				window.setClosable(true);

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(window);

				Center center = new Center();
				ais.ui.util.ZkCompat.setFlex(center, true);
				center.setParent(borderlayout);

				final MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig("Item Biaya");
				column.setParent(columns);

				column = new MyColumnConfig("Nilai");
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				for (DetailBiaya detailBiaya : detailBiayas) {
					try {
						CicilanPembayaran cicilanPembayaranSebelumnya = new CicilanPembayaran(detailBiaya);
						cicilanPembayaranSebelumnya.setItemBiaya(detailBiaya.getItemBiaya());
						cicilanPembayaranSebelumnya
								.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
										: detailBiaya.getNilaiBiayaBaru()));
						MyDoublebox jumlahCicilan = new MyDoublebox(cicilanPembayaranSebelumnya.getNilai());

						Row row = new Row();
						row.setValign("top");
						row.setValign("top");
						row.setAttribute("cicilanPembayaran", cicilanPembayaranSebelumnya);
						row.setValign("top");
						row.setAttribute("jumlahCicilan", jumlahCicilan);
						row.setParent(rows);
						row.appendChild(new ais.ui.util.MyLabelConfig(detailBiaya.getItemBiaya().getNama()));
						row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get()
								.format((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
										: detailBiaya.getNilaiBiayaBaru()))));
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/pmb/TampilanPaymentGateway.java:240");
					}
				}

				Foot foot = new Foot();
				foot.setParent(grid);

				Footer footer = new Footer("Total");
				footer.setParent(foot);
				footer.setStyle("font-weight:bold;font-size:15px;");

				footer = new Footer(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars));
				footer.setParent(foot);
				footer.setStyle("font-weight:bold;font-size:15px;");

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);
				south.setHeight("45px");

				Toolbar hbox = new Toolbar();
				hbox.setParent(south);
				hbox.setHeight("100%");
				hbox.setAlign("center");

				MyButtonConfig tutupButton = new MyButtonConfig("Tutup");
				tutupButton.setHeight("45px");
				tutupButton.setTooltiptext("Tutup jendela pembayaran ini");
				tutupButton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						window.detach();
					}
				});
				hbox.appendChild(tutupButton);

				if (bayarViaFinpay) {
					MyButtonConfig bayarViaFinpayButton = new MyButtonConfig("BAYAR UANG PENDAFTARAN VIA FINPAY",
							"/img/spi-finpay.png");
					bayarViaFinpayButton.setOrient("vertical");
					bayarViaFinpayButton.setHeight("45px");
					hbox.appendChild(bayarViaFinpayButton);

					bayarViaFinpayButton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							FinpayCommon.bayarCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									window.detach();
								}
							});
						}
					});
				}

				if (bayarViaIpaymu) {
					MyButtonConfig bayarViaIpaymuButton = new MyButtonConfig("BAYAR VIA IPaymu",
							"/img/logo_ipaymu.png");
					bayarViaIpaymuButton.setOrient("vertical");
					bayarViaIpaymuButton.setHeight("45px");
					hbox.appendChild(bayarViaIpaymuButton);

					bayarViaIpaymuButton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							IpaymuCommon.bayarCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									window.detach();
								}
							});
						}
					});
				}

				if (bayarViaFaspay) {
					MyButtonConfig bayarViaFaspayButton = FaspayCommon.createButton();
					bayarViaFaspayButton.setHeight("45px");
					hbox.appendChild(bayarViaFaspayButton);

					bayarViaFaspayButton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							FaspayCommon.bayarCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									window.detach();
								}
							});
						}
					});
				}

				if (bayarViaCimb) {
					MyButtonConfig bayarViaCimbButton = CimbCommon.createButton();
					bayarViaCimbButton.setHeight("45px");
					hbox.appendChild(bayarViaCimbButton);

					bayarViaCimbButton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							CimbCommon.bayarCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									window.detach();
								}
							});
						}
					});
				}

				if (bayarViaBni) {
					MyButtonConfig bayarViaBniButton = BniCommon.createButton();
					bayarViaBniButton.setHeight("45px");
					hbox.appendChild(bayarViaBniButton);

					bayarViaBniButton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							BniCommon.bayarCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, true);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									window.detach();
								}
							});
						}
					});
				}

				if (bayarViaBsi) {
					MyButtonConfig bayarViaBsiButton = BsiCommon.createButton();
					bayarViaBsiButton.setHeight("45px");
					hbox.appendChild(bayarViaBsiButton);

					bayarViaBsiButton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							BsiCommon.bayarCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, true);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									window.detach();
								}
							});
						}
					});
				}

				if (bayarViaMandiri) {
					MyButtonConfig bayarViaBniButton = JatelindoCommon.createButton();
					bayarViaBniButton.setHeight("45px");
					hbox.appendChild(bayarViaBniButton);

					bayarViaBniButton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							JatelindoCommon.bayarCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									window.detach();
								}
							});
						}
					});
				}

				if (aktifkan_pembayaran_via_bank_bankaltimtara) {
					final MyButtonConfig bayarBankNTT = new MyButtonConfig("BAYAR VIA BANK Bankaltimtara");
					bayarBankNTT.setWidth("200px");
					bayarBankNTT.setHeight("45px");
					hbox.appendChild(bayarBankNTT);

					bayarBankNTT.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (!detailBiayas.isEmpty()) {

										final MyWindow window = new MyWindow("Pilihlah Bayar Via", "none", false);
										window.setHeight("200px");
										window.setWidth("400px");

										Radiogroup radiogroup = new Radiogroup();
										radiogroup.setParent(window);

										Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
										borderlayout.setParent(radiogroup);
										Center center = new Center();
										center.setParent(borderlayout);
										ais.ui.util.ZkCompat.setFlex(center, true);

										MyGrid grid = new MyGrid();
										grid.setWidth("100%");
										grid.setParent(center);
										grid.setWidth("100%");
										grid.setHeight("100%");

										South southBatal = new South();
										ais.ui.util.ZkCompat.setFlex(southBatal, true);
										southBatal.setParent(borderlayout);
										southBatal.setHeight("40px");

										Toolbar toolbarBatal = new Toolbar();
										toolbarBatal.setParent(southBatal);
										toolbarBatal.setHeight("100%");
										toolbarBatal.setAlign("center");

										MyButtonConfig batalButton = new MyButtonConfig("Batal");
										batalButton.setHeight("32px");
										batalButton.setTooltiptext("Batalkan pilihan cara bayar");
										batalButton.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event event) throws Exception {
												window.detach();
											}
										});
										toolbarBatal.appendChild(batalButton);

										Rows rows = new Rows();
										rows.setParent(grid);
										for (final String kode : new String[] { "Virtual Account", "QRIS" }) {

											Row row = new Row();
											row.setValign("top");
											row.setParent(rows);
											MyRadioConfig radio = new MyRadioConfig(kode);
											radio.setParent(row);
											radio.addEventListener("onClick", new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {

															Serializable[] serializables = PembayaranUtil.getInstance()
																	.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
																			biodataCalonMahasiswa.getTanggalDaftar(),
																			jenisKegiatan,
																			biodataCalonMahasiswa.getJenjang(),
																			biodataCalonMahasiswa.getTahunAkademik(),
																			biodataCalonMahasiswa
																					.getGelombangPendaftaran()
																					.getJenisSemester()
																					.equalsIgnoreCase(
																							Perkuliahan.GANJIL),
																			biodataCalonMahasiswa.getJenisSeleksi(),
																			biodataCalonMahasiswa.getProgram(),
																			biodataCalonMahasiswa.getNoRegistrasi(),
																			biodataCalonMahasiswa
																					.getGelombangPendaftaran());
															JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];

															if (jadwalPembayaran == null) {
																MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
																		"Peringatan", MyMessageboxConfig.OK,
																		MyMessageboxConfig.INFORMATION);
																return;
															}

															Double biayaAdministrasi = 0.0;
															try {
																biayaAdministrasi = Double
																		.parseDouble(Common.getKonfigurasi(
																				"bankaltimtara_biaya_administrasi",
																				"0.0").getNilai());
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:509");

															}

															VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankBankaltimtara
																	.downloadData(biodataCalonMahasiswa,
																			jadwalPembayaran, detailBiayas,
																			biayaAdministrasi,
																			kode.equalsIgnoreCase("Virtual Account"));

															File myfilebarcode1 = new File(
																	Common.ambilREAL_PATH_REPORT() + "/crcode_"
																			+ virtualAccountBank.getId() + ".png");

															int h = 600;
															int w = 600;
															BarcodeCommon.generateCRCode(
																	virtualAccountBank.getBarcode(), myfilebarcode1, h,
																	w);

															String nama = biodataCalonMahasiswa.getNama();
															String myUrl = "/common/bankaltimtara/no_va.zul?pakaiva="
																	+ virtualAccountBank.getPakaiva() + "&va="
																	+ URLEncoder.encode(
																			virtualAccountBank.getKode(), "UTF-8")
																	+ "&nominal="
																	+ URLEncoder
																			.encode("Rp. " + Common.numberFormat.get()
																					.format(virtualAccountBank
																							.getTotal()),
																					"UTF-8")
																	+ "&biayaAdministrasi="
																	+ URLEncoder.encode("Rp. " + Common.numberFormat
																			.get().format(biayaAdministrasi), "UTF-8")
																	+ "&nama=" + URLEncoder.encode(nama, "UTF-8")
																	+ "&kadalurasa="
																	+ URLEncoder.encode(
																			Common.dateFormat.get()
																					.format(virtualAccountBank
																							.getKadaluarsaWaktu()),
																			"UTF-8")
																	+ "&biayaTotal="
																	+ URLEncoder.encode(
																			"Rp. " + Common.numberFormat.get().format(
																					virtualAccountBank.getTotal()
																							+ biayaAdministrasi),
																			"UTF-8")

																	+ (virtualAccountBank.getKadaluarsaBarcode() == null
																			? ""
																			: "&kadalurasa_barcode=" + URLEncoder
																					.encode(Common.dateFormat5.get()
																							.format(virtualAccountBank
																									.getKadaluarsaBarcode()),
																							"UTF-8"))

																	+ "&qr="
																	+ URLEncoder
																			.encode(Common.getRequestHostWithProtocol()
																					+ "/report/"
																					+ myfilebarcode1.getName(), "UTF-8")
																	+ (virtualAccountBank.getHtmlTemporaryData() == null
																			|| virtualAccountBank.getHtmlTemporaryData()
																					.isEmpty()
																							? ""
																							: "&html=" + URLEncoder
																									.encode(virtualAccountBank
																											.getHtmlTemporaryData(),
																											"UTF-8"))
																	+ "&terbilang="
																	+ URLEncoder.encode(IndonesianNumberToWords.convert(
																			(long) (virtualAccountBank.getTotal()
																					+ biayaAdministrasi)),
																			"UTF-8")
																	+ "&tampilBiayaAdministrasi="
																	+ (biayaAdministrasi > 0.1);

															Common.displayWindow(myUrl, true, "75%");
														}
													});
													window.detach();

												}
											});
										}

										ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
												.appendChild(window);
										window.onModal();
									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});
				}

				if (aktifkan_pembayaran_via_bank_ntt) {
					final MyButtonConfig bayarBankNTT = new MyButtonConfig("BAYAR VIA BANK NTT");
					bayarBankNTT.setWidth("200px");
					bayarBankNTT.setHeight("45px");
					hbox.appendChild(bayarBankNTT);

					bayarBankNTT.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										VirtualAccountBank virtualAccountBank = DownloadNoUjianCalonMahasiswaBankNtt
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas,
														grid);

										String code = virtualAccountBank.getKode();

										File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
												+ virtualAccountBank.getId() + ".png");

										BarcodeCommon.generateCRCode(code, myfilebarcode1);

										Double biayaAdministrasi = 0.0;

										String nama = biodataCalonMahasiswa.getNama();
										String myUrl = "/common/ntt/no_va.zul?va="
												+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8") + "&nominal="
												+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
														.format(virtualAccountBank.getTotal()), "UTF-8")
												+ "&biayaAdministrasi="
												+ URLEncoder.encode(
														"Rp. " + Common.numberFormat.get().format(biayaAdministrasi),
														"UTF-8")
												+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
												+ URLEncoder.encode(Common.dateFormat.get()
														.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
												+ "&biayaTotal="
												+ URLEncoder.encode(
														"Rp. " + Common.numberFormat.get().format(
																virtualAccountBank.getTotal() + biayaAdministrasi),
														"UTF-8")
												+ "&qr="
												+ URLEncoder.encode(Common.getRequestHostWithProtocol()
														+ "/report/" + myfilebarcode1.getName(), "UTF-8")
												+ "&terbilang="
												+ URLEncoder.encode(IndonesianNumberToWords.convert(
														(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
														"UTF-8")
												+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

										Common.displayWindow(myUrl, true, "75%");

									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});
				}

				if (bayarViaDoku) {
					MyButtonConfig bayarViaDokuButton = new MyButtonConfig("BAYAR VIA DOKU", "/img/msc-logo.png");
					bayarViaDokuButton.setOrient("vertical");
					bayarViaDokuButton.setHeight("45px");
					hbox.appendChild(bayarViaDokuButton);

					bayarViaDokuButton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							DokuCommon.bayarCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									window.detach();
								}
							});
						}
					});
				}

				if (aktifkan_pembayaran_via_bank_online) {

					final MyButtonConfig bayarBankOnline = new MyButtonConfig("BAYAR ONLINE");
					bayarBankOnline.setOrient("vertical");
					bayarBankOnline.setHeight("45px");
					hbox.appendChild(bayarBankOnline);

					bayarBankOnline.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings("rawtypes")
								@Override
								public void onEvent(Event arg0) throws Exception {

									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
										System.out.println("jadwalPembayaran => " + jadwalPembayaran);

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										Double biayaAdmin = 0.0;
										try {
											biayaAdmin = Double.parseDouble(Common
													.getKonfigurasi("online_biaya_administrasi", "0.0").getNilai());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:766");

										}

										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi("online_bank_host_ip", "").getNilai(),
												"Bank Host");
										Map param = new HashMap();

										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas,
														param, biayaAdmin, bankHost);

										if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif")) {
											return;
										}

										if (virtualAccountBank != null && !virtualAccountBank.getLink().isEmpty()) {
											Clients.evalJavaScript("popupCenter({url: '" + virtualAccountBank.getLink()
													+ "', title: 'Book', w: 1200, h: 600});");
											return;
										}

										if (virtualAccountBank != null && virtualAccountBank.getId() != null) {

											String code = virtualAccountBank.getKode();

											String kodebankLainOnline = Common
													.getKonfigurasi("prefix_kode_bank_lain_online_2", "").getNilai();
											if (!kodebankLainOnline.trim().isEmpty() && virtualAccountBank != null
													&& virtualAccountBank.getKanalPembayaran() != null
													&& virtualAccountBank.getKanalPembayaran().getBsiUsername() != null
													&& !virtualAccountBank.getKanalPembayaran().getBsiUsername()
															.isEmpty()) {
												code = (virtualAccountBank.getKanalPembayaran().getBsiUsername()
														+ code);

												kodebankLainOnline = kodebankLainOnline + "" + code;
											} else if (!kodebankLainOnline.trim().isEmpty()) {
												kodebankLainOnline = kodebankLainOnline + "" + code;
											}

											File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
													+ virtualAccountBank.getId() + ".png");

											BarcodeCommon.generateCRCode(code, myfilebarcode1);

											Double biayaAdministrasi = 0.0;
											try {
												biayaAdministrasi = Double.parseDouble(Common
														.getKonfigurasi("online_biaya_administrasi", "0.0").getNilai());
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:817");

											}

											String nama = biodataCalonMahasiswa.getNama();
											String myUrl = "/common/online/no_va.zul?va="
													+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8")
													+ "&nominal="
													+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
															.format(virtualAccountBank.getTotal()), "UTF-8")
													+ "&biayaAdministrasi="
													+ URLEncoder.encode("Rp. "
															+ Common.numberFormat.get().format(biayaAdministrasi),
															"UTF-8")
													+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
													+ URLEncoder.encode(Common.dateFormat.get()
															.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
													+ "&biayaTotal="
													+ URLEncoder.encode(
															"Rp. " + Common.numberFormat.get().format(
																	virtualAccountBank.getTotal() + biayaAdministrasi),
															"UTF-8")
													+ "&qr="
													+ URLEncoder.encode(Common.getRequestHostWithProtocol()
															+ "/report/" + myfilebarcode1.getName(), "UTF-8")
													+ "&terbilang="
													+ URLEncoder.encode(IndonesianNumberToWords.convert(
															(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
															"UTF-8")
													+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1)
													+ (kodebankLainOnline.trim().isEmpty() ? ""
															: "&kodeBankLain="
																	+ URLEncoder.encode(kodebankLainOnline, "UTF-8"));

											Common.displayWindow(myUrl, true, "75%");
										}
									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});

				}

				if (aktifkan_pembayaran_via_bank_online_2) {

					final MyButtonConfig bayarBankOnline = new MyButtonConfig("BAYAR ONLINE 2");
					bayarBankOnline.setOrient("vertical");
					bayarBankOnline.setHeight("45px");
					hbox.appendChild(bayarBankOnline);

					bayarBankOnline.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings("rawtypes")
								@Override
								public void onEvent(Event arg0) throws Exception {

									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
										System.out.println("jadwalPembayaran => " + jadwalPembayaran);

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										Double biayaAdministrasi = 0.0;
										try {
											biayaAdministrasi = Double.parseDouble(Common
													.getKonfigurasi("online_biaya_administrasi_2", "0.0").getNilai());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:910");

										}
										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi("online_2_bank_host_ip", "").getNilai(),
												"Bank Host");
										Map param = new HashMap();
										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas,
														param, biayaAdministrasi, bankHost);
										if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif")) {
											return;
										}

										if (virtualAccountBank != null && !virtualAccountBank.getLink().isEmpty()) {
											Clients.evalJavaScript("popupCenter({url: '" + virtualAccountBank.getLink()
													+ "', title: 'Book', w: 1200, h: 600});");
											return;
										}
										String code = virtualAccountBank.getKode();

										String kodebankLainOnline = Common
												.getKonfigurasi("prefix_kode_bank_lain_online_2", "").getNilai();
										if (!kodebankLainOnline.trim().isEmpty() && virtualAccountBank != null
												&& virtualAccountBank.getKanalPembayaran() != null
												&& virtualAccountBank.getKanalPembayaran().getBsiUsername() != null
												&& !virtualAccountBank.getKanalPembayaran().getBsiUsername()
														.isEmpty()) {
											code = (virtualAccountBank.getKanalPembayaran().getBsiUsername() + code);

											kodebankLainOnline = kodebankLainOnline + "" + code;
										} else if (!kodebankLainOnline.trim().isEmpty()) {
											kodebankLainOnline = kodebankLainOnline + "" + code;
										}

										File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
												+ virtualAccountBank.getId() + ".png");

										BarcodeCommon.generateCRCode(code, myfilebarcode1);

										String nama = biodataCalonMahasiswa.getNama();
										String myUrl = "/common/online/no_va.zul?va="
												+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8") + "&nominal="
												+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
														.format(virtualAccountBank.getTotal()), "UTF-8")
												+ "&biayaAdministrasi="
												+ URLEncoder.encode(
														"Rp. " + Common.numberFormat.get().format(biayaAdministrasi),
														"UTF-8")
												+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
												+ URLEncoder.encode(Common.dateFormat.get()
														.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
												+ "&biayaTotal="
												+ URLEncoder.encode(
														"Rp. " + Common.numberFormat.get().format(
																virtualAccountBank.getTotal() + biayaAdministrasi),
														"UTF-8")
												+ "&qr="
												+ URLEncoder.encode(Common.getRequestHostWithProtocol()
														+ "/report/" + myfilebarcode1.getName(), "UTF-8")
												+ "&terbilang="
												+ URLEncoder.encode(IndonesianNumberToWords.convert(
														(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
														"UTF-8")
												+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1)
												+ (kodebankLainOnline.trim().isEmpty() ? ""
														: "&kodeBankLain="
																+ URLEncoder.encode(kodebankLainOnline, "UTF-8"));

										Common.displayWindow(myUrl, true, "75%");
									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});

				}

				if (aktifkan_pembayaran_via_bank_online_smartlink) {

					final MyButtonConfig bayarBankOnline = new MyButtonConfig("BAYAR VIA ONLINE");
					bayarBankOnline.setOrient("vertical");
					bayarBankOnline.setHeight("45px");
					hbox.appendChild(bayarBankOnline);

					bayarBankOnline.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings({ "rawtypes", "unchecked" })
								@Override
								public void onEvent(Event arg0) throws Exception {

									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
										System.out.println("jadwalPembayaran => " + jadwalPembayaran);

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										Double biayaAdmin = 0.0;
										try {
											biayaAdmin = Double.parseDouble(
													Common.getKonfigurasi("online_smartlink_biaya_administrasi", "0.0")
															.getNilai());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:1038");

										}

										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi("online_bank_host_ip", "").getNilai(),
												"Bank Host");
										boolean smartlink = true;
										Map param = new HashMap();
										param.put("smartlink", smartlink);

										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas,
														param, biayaAdmin, bankHost);

										if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif")) {
											return;
										}

										if (virtualAccountBank != null && !virtualAccountBank.getLink().isEmpty()) {
											Clients.evalJavaScript("popupCenter({url: '" + virtualAccountBank.getLink()
													+ "', title: 'Book', w: 1200, h: 600});");
											return;
										}

										if (virtualAccountBank != null && virtualAccountBank.getId() != null) {

											String code = virtualAccountBank.getKode();

											String kodebankLainOnline = Common
													.getKonfigurasi("prefix_kode_bank_lain_online_2", "").getNilai();
											if (!kodebankLainOnline.trim().isEmpty() && virtualAccountBank != null
													&& virtualAccountBank.getKanalPembayaran() != null
													&& virtualAccountBank.getKanalPembayaran().getBsiUsername() != null
													&& !virtualAccountBank.getKanalPembayaran().getBsiUsername()
															.isEmpty()) {
												code = (virtualAccountBank.getKanalPembayaran().getBsiUsername()
														+ code);

												kodebankLainOnline = kodebankLainOnline + "" + code;
											} else if (!kodebankLainOnline.trim().isEmpty()) {
												kodebankLainOnline = kodebankLainOnline + "" + code;
											}

											File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
													+ virtualAccountBank.getId() + ".png");

											BarcodeCommon.generateCRCode(code, myfilebarcode1);

											Double biayaAdministrasi = 0.0;
											try {
												biayaAdministrasi = Double.parseDouble(Common
														.getKonfigurasi("online_biaya_administrasi", "0.0").getNilai());
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:1091");

											}

											String nama = biodataCalonMahasiswa.getNama();
											String myUrl = "/common/online/no_va.zul?va="
													+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8")
													+ "&nominal="
													+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
															.format(virtualAccountBank.getTotal()), "UTF-8")
													+ "&biayaAdministrasi="
													+ URLEncoder.encode("Rp. "
															+ Common.numberFormat.get().format(biayaAdministrasi),
															"UTF-8")
													+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
													+ URLEncoder.encode(Common.dateFormat.get()
															.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
													+ "&biayaTotal="
													+ URLEncoder.encode(
															"Rp. " + Common.numberFormat.get().format(
																	virtualAccountBank.getTotal() + biayaAdministrasi),
															"UTF-8")
													+ "&qr="
													+ URLEncoder.encode(Common.getRequestHostWithProtocol()
															+ "/report/" + myfilebarcode1.getName(), "UTF-8")
													+ "&terbilang="
													+ URLEncoder.encode(IndonesianNumberToWords.convert(
															(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
															"UTF-8")
													+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1)
													+ (kodebankLainOnline.trim().isEmpty() ? ""
															: "&kodeBankLain="
																	+ URLEncoder.encode(kodebankLainOnline, "UTF-8"));

											Common.displayWindow(myUrl, true, "75%");
										}
									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});

				}

				if (aktifkan_pembayaran_via_bank_maja) {

					final MyButtonConfig bayarBankOnline = new MyButtonConfig("BAYAR VIA BSI");
					bayarBankOnline.setOrient("vertical");
					bayarBankOnline.setHeight("45px");
					hbox.appendChild(bayarBankOnline);

					bayarBankOnline.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings({ "unchecked", "rawtypes" })
								@Override
								public void onEvent(Event arg0) throws Exception {

									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
										System.out.println("jadwalPembayaran => " + jadwalPembayaran);

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										Double biayaAdmin = 0.0;
										try {
											biayaAdmin = Double.parseDouble(
													Common.getKonfigurasi("maja_biaya_administrasi", "0.0").getNilai());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:1184");

										}

										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi("maja_bank_host_ip", "").getNilai(), "Bank Host");

										Map param = new HashMap();
										param.put("maja", true);

										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas,
														param, biayaAdmin, bankHost);
										if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif")) {
											return;
										}
										if (virtualAccountBank != null && virtualAccountBank.getId() != null) {

											String code = virtualAccountBank.getKode();

											File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
													+ virtualAccountBank.getId() + ".png");

											BarcodeCommon.generateCRCode(code, myfilebarcode1);

											Double biayaAdministrasi = 0.0;
											try {
												biayaAdministrasi = Double.parseDouble(Common
														.getKonfigurasi("maja_biaya_administrasi", "0.0").getNilai());
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:1213");

											}

											String nama = biodataCalonMahasiswa.getNama();
											String myUrl = "/common/online/no_va.zul?va="
													+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8")
													+ "&nominal="
													+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
															.format(virtualAccountBank.getTotal()), "UTF-8")
													+ "&biayaAdministrasi="
													+ URLEncoder.encode("Rp. "
															+ Common.numberFormat.get().format(biayaAdministrasi),
															"UTF-8")
													+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
													+ URLEncoder.encode(Common.dateFormat.get()
															.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
													+ "&biayaTotal="
													+ URLEncoder.encode(
															"Rp. " + Common.numberFormat.get().format(
																	virtualAccountBank.getTotal() + biayaAdministrasi),
															"UTF-8")
													+ "&qr="
													+ URLEncoder.encode(Common.getRequestHostWithProtocol()
															+ "/report/" + myfilebarcode1.getName(), "UTF-8")
													+ "&terbilang="
													+ URLEncoder.encode(IndonesianNumberToWords.convert(
															(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
															"UTF-8")
													+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

											Common.displayWindow(myUrl, true, "75%");
										}
									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});

				}

				if (aktifkan_pembayaran_via_bank_finpay) {
					final MyButtonConfig bayarBankOnline = new MyButtonConfig("BAYAR VIA FINPAY");
					bayarBankOnline.setOrient("vertical");
					bayarBankOnline.setHeight("45px");
					hbox.appendChild(bayarBankOnline);

					bayarBankOnline.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings({ "rawtypes", "unchecked" })
								@Override
								public void onEvent(Event arg0) throws Exception {

									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
										System.out.println("jadwalPembayaran => " + jadwalPembayaran);

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										Double biayaAdministrasi = 0.0;
										try {
											biayaAdministrasi = Double.parseDouble(Common
													.getKonfigurasi("finpay_biaya_administrasi", "0.0").getNilai());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:1302");

										}
										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi("finpay_bank_host_ip", "").getNilai(),
												"Bank Host");

										Map param = new HashMap();
										param.put("finpay", true);
										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas,
														param, biayaAdministrasi, bankHost);
										if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif")) {
											return;
										}
										ExecutionsCtrl.getCurrent().sendRedirect(virtualAccountBank.getLink(),
												"_blank");
									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});
				}

				if (aktifkan_pembayaran_via_bank_otto) {
					final MyButtonConfig bayarBankOnline = new MyButtonConfig("BAYAR VIA OTTO");
					bayarBankOnline.setOrient("vertical");
					bayarBankOnline.setHeight("45px");
					hbox.appendChild(bayarBankOnline);

					bayarBankOnline.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings({ "rawtypes", "unchecked" })
								@Override
								public void onEvent(Event arg0) throws Exception {

									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
										System.out.println("jadwalPembayaran => " + jadwalPembayaran);

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										Double biayaAdministrasi = 0.0;
										try {
											biayaAdministrasi = Double.parseDouble(
													Common.getKonfigurasi("otto_biaya_administrasi", "0.0").getNilai());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:1374");

										}
										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi("otto_bank_host_ip", "").getNilai(), "Bank Host");
										Map param = new HashMap();
										param.put("otto", true);
										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas,
														param, biayaAdministrasi, bankHost);
										if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif")) {
											return;
										}
										ExecutionsCtrl.getCurrent().sendRedirect(virtualAccountBank.getLink(),
												"_blank");
									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});
				}

				if (aktifkan_pembayaran_via_bank_flip) {
					final MyButtonConfig bayarBankOnline = new MyButtonConfig("BAYAR VIA FLIP");
					bayarBankOnline.setOrient("vertical");
					bayarBankOnline.setHeight("45px");
					hbox.appendChild(bayarBankOnline);

					bayarBankOnline.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings({ "rawtypes", "unchecked" })
								@Override
								public void onEvent(Event arg0) throws Exception {

									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
										System.out.println("jadwalPembayaran => " + jadwalPembayaran);

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										Double biayaAdministrasi = 0.0;
										try {
											biayaAdministrasi = Double.parseDouble(
													Common.getKonfigurasi("flip_biaya_administrasi", "0.0").getNilai());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:1444");

										}
										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi("flip_bank_host_ip", "").getNilai(), "Bank Host");
										Map param = new HashMap();
										param.put("flip", true);
										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas,
														param, biayaAdministrasi, bankHost);
										if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif")) {
											return;
										}

										Clients.evalJavaScript("popupCenter({url: '" + virtualAccountBank.getLink()
												+ "', title: 'Book', w: 1200, h: 600});");
									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});
				}

				if (aktifkan_pembayaran_via_bank_briva) {
					final MyButtonConfig bayarBankOnline = new MyButtonConfig("BAYAR VIA BRIVA");
					bayarBankOnline.setOrient("vertical");
					bayarBankOnline.setHeight("45px");
					hbox.appendChild(bayarBankOnline);

					bayarBankOnline.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings({ "unchecked", "rawtypes" })
								@Override
								public void onEvent(Event arg0) throws Exception {

									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
										System.out.println("jadwalPembayaran => " + jadwalPembayaran);

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										Double biayaAdministrasi = 0.0;
										try {
											biayaAdministrasi = Double.parseDouble(Common
													.getKonfigurasi("briva_biaya_administrasi", "0.0").getNilai());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:1515");

										}
										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi("briva_bank_host_ip", "").getNilai(),
												"Bank Host");
										Map param = new HashMap();
										param.put("briva", true);
										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas,
														param, biayaAdministrasi, bankHost);

										if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif")) {
											return;
										}

										File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
												+ virtualAccountBank.getId() + ".png");

										BarcodeCommon.generateCRCode(virtualAccountBank.getKode(), myfilebarcode1);

										String nama = biodataCalonMahasiswa.getNama();
										String myUrl = "/common/bri/no_va.zul?va="
												+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8") + "&nominal="
												+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
														.format(virtualAccountBank.getTotal()), "UTF-8")
												+ "&biayaAdministrasi="
												+ URLEncoder.encode(
														"Rp. " + Common.numberFormat.get().format(biayaAdministrasi),
														"UTF-8")
												+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
												+ URLEncoder.encode(Common.dateFormat.get()
														.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
												+ "&biayaTotal="
												+ URLEncoder.encode(
														"Rp. " + Common.numberFormat.get().format(
																virtualAccountBank.getTotal() + biayaAdministrasi),
														"UTF-8")
												+ "&qr="
												+ URLEncoder.encode(Common.getRequestHostWithProtocol()
														+ "/report/" + myfilebarcode1.getName(), "UTF-8")
												+ "&terbilang="
												+ URLEncoder.encode(IndonesianNumberToWords.convert(
														(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
														"UTF-8")
												+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);
										Common.displayWindow(myUrl, true, "75%");
									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});
				}

				if (aktifkan_pembayaran_via_bank_bjb) {

					final MyButtonConfig bayarBankBJB = new MyButtonConfig("BAYAR VIA BANK BJB");
					bayarBankBJB.setWidth("130px");
					bayarBankBJB.setHeight("55px");
					hbox.appendChild(bayarBankBJB);

					bayarBankBJB.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
										System.out.println("jadwalPembayaran => " + jadwalPembayaran);

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankBjb
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas);

										String code = virtualAccountBank.getKode();

										File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
												+ virtualAccountBank.getId() + ".png");

										BarcodeCommon.generateCRCode(code, myfilebarcode1);

										Double biayaAdministrasi = 0.0;

										String nama = biodataCalonMahasiswa.getNama();
										String myUrl = "/common/bjb/no_va.zul?va="
												+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8") + "&nominal="
												+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
														.format(virtualAccountBank.getTotal()), "UTF-8")
												+ "&biayaAdministrasi="
												+ URLEncoder.encode(
														"Rp. " + Common.numberFormat.get().format(biayaAdministrasi),
														"UTF-8")
												+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
												+ URLEncoder.encode(Common.dateFormat.get()
														.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
												+ "&biayaTotal="
												+ URLEncoder.encode(
														"Rp. " + Common.numberFormat.get().format(
																virtualAccountBank.getTotal() + biayaAdministrasi),
														"UTF-8")
												+ "&qr="
												+ URLEncoder.encode(Common.getRequestHostWithProtocol()
														+ "/report/" + myfilebarcode1.getName(), "UTF-8")
												+ "&terbilang="
												+ URLEncoder.encode(IndonesianNumberToWords.convert(
														(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
														"UTF-8")
												+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

										Common.displayWindow(myUrl, true, "75%");

									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});

				}

				if (aktifkan_pembayaran_via_bank_btn) {

					final MyButtonConfig bayarBankBTN = new MyButtonConfig("BAYAR VIA BANK BTN");
					bayarBankBTN.setOrient("vertical");
					bayarBankBTN.setHeight("45px");
					hbox.appendChild(bayarBankBTN);

					bayarBankBTN.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
										System.out.println("jadwalPembayaran => " + jadwalPembayaran);

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankBtn
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas);

										if (virtualAccountBank != null) {
											Double biayaAdministrasi = 0.0;

											String code = virtualAccountBank.getKode();

											File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
													+ virtualAccountBank.getId() + ".png");

											BarcodeCommon.generateCRCode(code, myfilebarcode1);

											String nama = biodataCalonMahasiswa.getNama();
											String myUrl = "/common/btn/no_va.zul?va="
													+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8")
													+ "&nominal="
													+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
															.format(virtualAccountBank.getTotal()), "UTF-8")
													+ "&biayaAdministrasi="
													+ URLEncoder.encode("Rp. "
															+ Common.numberFormat.get().format(biayaAdministrasi),
															"UTF-8")
													+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
													+ URLEncoder.encode(Common.dateFormat.get()
															.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
													+ "&biayaTotal="
													+ URLEncoder.encode(
															"Rp. " + Common.numberFormat.get().format(
																	virtualAccountBank.getTotal() + biayaAdministrasi),
															"UTF-8")
													+ "&qr="
													+ URLEncoder.encode(Common.getRequestHostWithProtocol()
															+ "/report/" + myfilebarcode1.getName(), "UTF-8")
													+ "&terbilang="
													+ URLEncoder.encode(IndonesianNumberToWords.convert(
															(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
															"UTF-8")
													+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

											Common.displayWindow(myUrl, true, "75%");
										} else {
											MyMessageboxConfig.show("Transaksi gagal dilakukan", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
										}

									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});

				}

				if (aktifkan_pembayaran_via_bank_qris) {

					final MyButtonConfig bayarBankOnline = new MyButtonConfig("BAYAR QRIS");
					bayarBankOnline.setOrient("vertical");
					bayarBankOnline.setHeight("45px");
					hbox.appendChild(bayarBankOnline);

					bayarBankOnline.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {

							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings({ "unchecked", "rawtypes" })
								@Override
								public void onEvent(Event arg0) throws Exception {

									if (!detailBiayas.isEmpty()) {

										Serializable[] serializables = PembayaranUtil.getInstance()
												.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
														biodataCalonMahasiswa.getTanggalDaftar(), jenisKegiatan,
														biodataCalonMahasiswa.getJenjang(),
														biodataCalonMahasiswa.getTahunAkademik(),
														biodataCalonMahasiswa.getGelombangPendaftaran()
																.getJenisSemester()
																.equalsIgnoreCase(Perkuliahan.GANJIL),
														biodataCalonMahasiswa.getJenisSeleksi(),
														biodataCalonMahasiswa.getProgram(),
														biodataCalonMahasiswa.getNoRegistrasi(),
														biodataCalonMahasiswa.getGelombangPendaftaran());
										JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
										System.out.println("jadwalPembayaran => " + jadwalPembayaran);

										if (jadwalPembayaran == null) {
											MyMessageboxConfig.show("Mohon maaf, jadwal pembayaran belum tersedia untuk calon mahasiswa ini. Langkah yang dapat dilakukan: (1) hubungi bagian keuangan untuk memastikan jadwal pembayaran sudah dikonfigurasi; (2) periksa apakah gelombang pendaftaran dan paket sudah benar; (3) coba muat ulang halaman ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
													MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											return;
										}

										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi("qris_bank_host_ip", "").getNilai(), "Bank Host");
										Double biayaAdministrasi = 0.0;
										try {
											biayaAdministrasi = Double.parseDouble(
													Common.getKonfigurasi("qris_biaya_administrasi", "0.0").getNilai());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/TampilanPaymentGateway.java:1810");

										}
										Map param = new HashMap();
										param.put("qris", true);
										VirtualAccountBank virtualAccountBank = DownloadNoRegistrasiCalonMahasiswaBankOnline
												.downloadData(biodataCalonMahasiswa, jadwalPembayaran, detailBiayas,
														param, biayaAdministrasi, bankHost);
										if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif")) {
											return;
										}
										if (virtualAccountBank != null && virtualAccountBank.getId() != null) {
											String code = virtualAccountBank.getKode();

											File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
													+ virtualAccountBank.getId() + ".png");

											BarcodeCommon.generateCRCode(code, myfilebarcode1);

											String nama = biodataCalonMahasiswa.getNama();
											String myUrl = "/common/qris/no_va.zul?va="
													+ URLEncoder.encode(virtualAccountBank.getKode(), "UTF-8")
													+ "&nominal="
													+ URLEncoder.encode("Rp. " + Common.numberFormat.get()
															.format(virtualAccountBank.getTotal()), "UTF-8")
													+ "&biayaAdministrasi="
													+ URLEncoder.encode("Rp. "
															+ Common.numberFormat.get().format(biayaAdministrasi),
															"UTF-8")
													+ "&nama=" + URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
													+ URLEncoder.encode(Common.dateFormat.get()
															.format(virtualAccountBank.getKadaluarsaWaktu()), "UTF-8")
													+ "&biayaTotal="
													+ URLEncoder.encode(
															"Rp. " + Common.numberFormat.get().format(
																	virtualAccountBank.getTotal() + biayaAdministrasi),
															"UTF-8")
													+ "&qr="
													+ URLEncoder.encode(Common.getRequestHostWithProtocol()
															+ "/report/" + myfilebarcode1.getName(), "UTF-8")
													+ "&terbilang="
													+ URLEncoder.encode(IndonesianNumberToWords.convert(
															(long) (virtualAccountBank.getTotal() + biayaAdministrasi)),
															"UTF-8")
													+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

											Common.displayWindow(myUrl, true, "75%");
										}
									} else {
										MyMessageboxConfig.show("Mohon maaf, tagihan belum dipilih. Langkah yang dapat dilakukan: (1) pilih atau centang tagihan yang akan dibayarkan dari daftar yang tersedia; (2) pastikan minimal satu tagihan telah dipilih; (3) ulangi proses pembayaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									}

								}
							}, "Proses pembayaran ..");
						}
					});

				}

				window.setVisible(true);
				window.onModal();
			}

		}
	}

	public static boolean adaPaymentGatewayYangAktif() {
		// TODO Auto-generated method stub
		return adaPembayaranRegistrasi();
	}

}
