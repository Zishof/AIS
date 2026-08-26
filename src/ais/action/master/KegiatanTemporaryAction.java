package ais.action.master;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vlayout;

import ais.action.master.helper.RevisiCicilanPembayaranTemporaryHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.RevisiKegiatanTemporaryHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankOnline;
import ais.action.master.helper.virtualaccount.MahasiswaVirtualAccountHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BniCommon;
import ais.common.BniKeranjangPembayaran;
import ais.common.BsiCommon;
import ais.common.BsiKeranjangPembayaran;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.FaspayCommon;
import ais.common.FaspayKeranjangPembayaran;
import ais.common.JatelindoCommon;
import ais.common.JatelindoKeranjangPembayaran;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Fakultas;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.action.master.helper.FilterLanjutHelper;

public class KegiatanTemporaryAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;

	private Paging paging;

	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox jenissemester;
	private Combobox searchjurusan;
	private Decimalbox searchtahun;
	private Combobox searchjenjang;
	private Combobox searchJenisPembayaran;
	private Textbox searchnamamhs;
	private MyDatebox start;
	private MyDatebox end;

	private Mahasiswa mahasiswaMaster = null;
	private Mahasiswa mahasiswa;

	private MyToolbarbuttonConfig find;

	private Hbox spaceBayar;
	private Html ringkasanKeranjangHtml;

	// private Set<KegiatanTemporary> selectedKegiatanTemporary = new
	// HashSet<KegiatanTemporary>();

	private Double tagihan = 0.0;
	private PerguruanTinggi perguruanTinggi;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (Common.isNumber(execution.getParameter("mahasiswa"))) {
			mahasiswaMaster = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("mahasiswa").trim()))).uniqueResult();
		}

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenissemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenissemester.appendChild(comboitem);

		comboitem = new MyComboitemConfig("Semua");
		if (comboitem != null) { comboitem.setValue(null); }
		jenissemester.appendChild(comboitem);
		if (jenissemester != null) { jenissemester.setSelectedItem(comboitem); }
		if (jenissemester != null) { jenissemester.setReadonly(true); }

		Common.insertComboDanSemua(searchJenisPembayaran, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			mahasiswaMaster = tbmuser.getMahasiswa();
		}

		onSearchDefault(null);

		Common.initPaging25(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "mahasiswa", "calonMahasiswa",
				"jenisKegiatan", "tahunAkademik", "program", "tanggal", "semster", "validated", "validator",
				"jadwalPembayaran", "amount");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);

		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();


		initTombolPembayaranKeranjang();
		perbaruiRingkasanKeranjangPembayaran(null);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		if (button != null) { button.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null); }
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiKegiatanTemporaryHelper revisiHelper = new RevisiKegiatanTemporaryHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(find.getParent()); }

	        FilterLanjutHelper.setup(comp);
}

	public static class DetailKegiatanTemporaryRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) arg1;
			Mahasiswa mahasiswa = cicilanPembayaran.getKegiatanTemporary().getMahasiswa();
			Integer semester = cicilanPembayaran.getKegiatanTemporary().getSemster();
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
					.getPengaturanPembayaranBulanan();

			String desc = "";
			Double jumlah = 0.0;
			if (pengaturanPembayaranBulanan != null) {

				int tahapan = 0;
				if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan) {
					try {
						String bln = Common.BULAN[pengaturanPembayaranBulanan.getRealBulan() - 1];
						tahapan = Common.poulateTahapan(mahasiswa.getProgram(), mahasiswa.getJurusan(), semester,
								mahasiswa.getSemesterMulai()).get(bln);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

				jumlah = mahasiswa == null || semester == null ? pengaturanPembayaranBulanan.getNominal()
						: pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
				JadwalPembayaran jadwalPembayaran = cicilanPembayaran.getKegiatanTemporary().getJadwalPembayaran();
				JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
						&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
								? jadwalPembayaran
								: null;
				Double hasilDenda = pengaturanPembayaranBulanan.checkDenda(jumlah,
						cicilanPembayaran.getKegiatanTemporary().getTanggal(), jdw,
						jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan());

				desc = pengaturanPembayaranBulanan.getKeterangan();

				desc = (desc.isEmpty() ? (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama()) : desc)
						+ ",  " + pengaturanPembayaranBulanan.getNamaBulan() + " "

						+ ", nominal Rp. " + Common.numberFormat.get().format(jumlah)
						+ (hasilDenda.intValue() > jumlah.intValue() ? pengaturanPembayaranBulanan.getInfoDenda() : "")
						+ (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahapan > 0 ? ", tahap " + tahapan
								: "");
			} else {
				desc = cicilanPembayaran.getItemBiaya().getNama();
				jumlah = cicilanPembayaran.getNilai();
			}

			RevisiHelper.createNewRevisi(CicilanPembayaran.class, cicilanPembayaran, desc).setParent(arg0);

			new Label(Common.numberFormat.get().format(jumlah)).setParent(arg0);

		}
	}


	private String htmlAman(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private String formatRingkas(Double value) {
		try {
			return Common.numberFormat.get().format(value == null ? 0.0 : value.doubleValue());
		} catch (Exception e) {
			return "0";
		}
	}

	private void perbaruiRingkasanKeranjangPembayaran(List<KegiatanTemporary> data) {
		try {
			Component parent = spaceBayar == null ? null : spaceBayar.getParent();
			if (parent == null) {
				return;
			}
			if (ringkasanKeranjangHtml != null) {
				ringkasanKeranjangHtml.detach();
			}
			int jumlahData = data == null ? 0 : data.size();
			double total = 0.0;
			if (data != null) {
				for (KegiatanTemporary kt : data) {
					if (kt != null && kt.getAmount() != null) {
						total += kt.getAmount().doubleValue();
					}
				}
			}
			StringBuilder sb = new StringBuilder();
			sb.append("<div style='margin:10px 0 12px 0;padding:12px;border-radius:8px;background:#f8fafc;border:1px solid #dbe4ee;font-family:Arial,sans-serif;color:#0f172a;'>");
			sb.append("<div style='display:table;width:100%;table-layout:fixed;'>");
			sb.append("<div style='display:table-cell;vertical-align:top;padding-right:10px;'>");
			sb.append("<div style='font-size:15px;font-weight:bold;'>Keranjang pembayaran</div>");
			sb.append("<div style='font-size:11px;color:#64748b;margin-top:3px;'>VA keranjang dibuat dari item yang dipilih pada tabel.</div>");
			sb.append("</div>");
			sb.append("<div style='display:table-cell;width:145px;vertical-align:top;text-align:right;'>");
			sb.append("<div style='font-size:11px;color:#64748b;'>Total halaman</div>");
			sb.append("<div style='font-size:18px;font-weight:bold;color:#047857;'>Rp ").append(formatRingkas(total)).append("</div>");
			sb.append("</div>");
			sb.append("</div>");
			sb.append("<div style='margin-top:10px;display:table;width:100%;table-layout:fixed;border-spacing:8px 0;'>");
			sb.append("<div style='display:table-cell;background:white;border:1px solid #e2e8f0;border-radius:8px;padding:9px;'>");
			sb.append("<div style='font-size:11px;color:#64748b;'>Data tampil</div>");
			sb.append("<div style='font-size:17px;font-weight:bold;'>").append(jumlahData).append("</div>");
			sb.append("</div>");
			sb.append("<div style='display:table-cell;background:white;border:1px solid #e2e8f0;border-radius:8px;padding:9px;'>");
			sb.append("<div style='font-size:11px;color:#64748b;'>Status</div>");
			sb.append("<div style='font-size:13px;font-weight:bold;color:#0369a1;'>Siap diproses</div>");
			sb.append("</div>");
			sb.append("<div style='display:table-cell;background:white;border:1px solid #e2e8f0;border-radius:8px;padding:9px;'>");
			sb.append("<div style='font-size:11px;color:#64748b;'>Pembayaran</div>");
			sb.append("<div style='height:7px;background:#e2e8f0;border-radius:6px;margin-top:7px;'><div style='width:")
					.append(jumlahData > 0 ? "68" : "18")
					.append("%;height:7px;background:#0891b2;border-radius:6px;'></div></div>");
			sb.append("</div>");
			sb.append("</div>");
			sb.append("</div>");
			ringkasanKeranjangHtml = new Html(sb.toString());
			ringkasanKeranjangHtml.setParent(parent);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}


	private interface KeranjangGatewayExecutor {
		void execute(Double tagihan, Mahasiswa mahasiswa, Set<KegiatanTemporary> selected, Event event) throws Exception;
	}

	private Set<KegiatanTemporary> getSelectedKegiatanTemporary() {
		return MahasiswaVirtualAccountHelper.ambilKegiatanTemporaryTerpilih(grid);
	}

	private KeranjangSelection validateSelectedKegiatanTemporary() throws Exception {
		Set<KegiatanTemporary> selected = getSelectedKegiatanTemporary();
		String pesan = MahasiswaVirtualAccountHelper.validasiKeranjang(selected);
		if (pesan != null) {
			MyMessageboxConfig.show(pesan);
			return null;
		}

		KeranjangSelection selection = new KeranjangSelection();
		selection.selected = selected;
		selection.mahasiswa = MahasiswaVirtualAccountHelper.ambilMahasiswa(selected);
		selection.total = Double.valueOf(MahasiswaVirtualAccountHelper.hitungTotal(selected));
		if (selection.mahasiswa == null) {
			MyMessageboxConfig.show("Data mahasiswa pada keranjang pembayaran tidak ditemukan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return null;
		}
		mahasiswa = selection.mahasiswa;
		tagihan = selection.total;
		return selection;
	}

	private void initTombolPembayaranKeranjang() {
		if (spaceBayar == null) {
			return;
		}
		spaceBayar.setPack("center");
		spaceBayar.setAlign("center");
		setupKeranjangGateway("aktifkan_pembayaran_via_faspay", FaspayCommon.createButton(), "faspay_biaya_administrasi",
				new KeranjangGatewayExecutor() {
					@Override
					public void execute(Double tagihan, Mahasiswa mahasiswa, Set<KegiatanTemporary> selected, Event event)
							throws Exception {
						FaspayKeranjangPembayaran.onSaveFaspay(tagihan, mahasiswa, null, selected, event);
					}
				});

		setupKeranjangGateway("aktifkan_pembayaran_via_bni", BniCommon.createButton(), "bni_biaya_administrasi",
				new KeranjangGatewayExecutor() {
					@Override
					public void execute(Double tagihan, Mahasiswa mahasiswa, Set<KegiatanTemporary> selected, Event event)
							throws Exception {
						BniKeranjangPembayaran.onSaveBni(tagihan, mahasiswa, null, selected, event);
					}
				});

		setupKeranjangBankOnline();
		setupKeranjangSmartlink();

		setupKeranjangGateway("aktifkan_pembayaran_via_bsi", BsiCommon.createButton(), "bsi_biaya_administrasi",
				new KeranjangGatewayExecutor() {
					@Override
					public void execute(Double tagihan, Mahasiswa mahasiswa, Set<KegiatanTemporary> selected, Event event)
							throws Exception {
						BsiKeranjangPembayaran.onSaveBsi(tagihan, mahasiswa, null, selected, event);
					}
				});

		setupKeranjangGateway("aktifkan_pembayaran_via_jatelindo", JatelindoCommon.createButton(),
				"jatelindo_biaya_administrasi", new KeranjangGatewayExecutor() {
					@Override
					public void execute(Double tagihan, Mahasiswa mahasiswa, Set<KegiatanTemporary> selected, Event event)
							throws Exception {
						JatelindoKeranjangPembayaran.onSaveJatelindo(tagihan, mahasiswa, null, selected, event);
					}
				});
	}

	private void setupKeranjangGateway(String konfigurasiAktif, final MyButtonConfig button,
			final String konfigurasiBiayaAdmin, final KeranjangGatewayExecutor executor) {
		if (!Konfigurasi.AKTIF.equals(Common.getKonfigurasi(konfigurasiAktif, Konfigurasi.TIDAK_AKTIF).getNilai())) {
			return;
		}
		button.setWidth("200px");
		button.setHeight("55px");
		button.setStyle("font-weight:bold;border-radius:12px;box-shadow:0 8px 18px rgba(15,23,42,.12);");
		spaceBayar.appendChild(button);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(final Event event) throws Exception {
				final KeranjangSelection selection = validateSelectedKegiatanTemporary();
				if (selection == null) {
					return;
				}
				String label = button.getLabel() == null ? "pembayaran" : button.getLabel();
				Double biayaAdministrasi = MahasiswaVirtualAccountHelper.getKonfigurasiDouble(konfigurasiBiayaAdmin, "0.0");
				MyMessageboxConfig.show(MahasiswaVirtualAccountHelper.buatPesanKonfirmasi(label, selection.selected,
						biayaAdministrasi), "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {
							@Override
							public void onEvent(final Event eventConfirm) throws Exception {
								int i = Integer.parseInt(eventConfirm.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										executor.execute(selection.total, selection.mahasiswa, selection.selected, eventConfirm);
										Common.freeze(grid, true);
										button.setDisabled(true);
									}
								}, "Proses pembayaran ..");
							}
						});
			}
		});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setupKeranjangBankOnline() {
		setupKeranjangBankOnlineGateway("aktifkan_pembayaran_via_bank_online", "BAYAR ONLINE", null,
				"online_bank_host_ip", "online_biaya_administrasi", "prefix_kode_bank_lain_online");
	}

	private void setupKeranjangSmartlink() {
		setupKeranjangBankOnlineGateway("aktifkan_pembayaran_via_bank_online_smartlink",
				"BAYAR VIA SMARTLINK", "smartlink", "online_bank_host_ip",
				"online_smartlink_biaya_administrasi", "prefix_kode_bank_lain_online");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setupKeranjangBankOnlineGateway(String konfigurasiAktif, String labelTombol,
			final String gatewayFlag, final String konfigurasiBankHost, final String konfigurasiBiayaAdmin,
			final String konfigurasiPrefixBank) {
		boolean aktif = Konfigurasi.AKTIF.equals(
				Common.getKonfigurasi(konfigurasiAktif, Konfigurasi.TIDAK_AKTIF).getNilai());
		if (aktif && perguruanTinggi != null && perguruanTinggi.getId() != null) {
			aktif = Konfigurasi.AKTIF.equals(Common
					.getKonfigurasi(konfigurasiAktif + "_pt_" + perguruanTinggi.getId(),
							Konfigurasi.AKTIF)
					.getNilai());
		}
		if (!aktif) {
			return;
		}

		final MyButtonConfig bayarBankOnline = new MyButtonConfig(labelTombol);
		bayarBankOnline.setWidth("130px");
		bayarBankOnline.setHeight("55px");
		bayarBankOnline.setStyle("font-weight:bold;border-radius:12px;background:#0f766e;color:white;");
		spaceBayar.appendChild(bayarBankOnline);
		bayarBankOnline.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final KeranjangSelection selection = validateSelectedKegiatanTemporary();
				if (selection == null) {
					return;
				}
				final Double biayaAdministrasi = MahasiswaVirtualAccountHelper.getKonfigurasiDouble(
						konfigurasiBiayaAdmin, "0.0");
				MyMessageboxConfig.show(MahasiswaVirtualAccountHelper.buatPesanKonfirmasi(
						bayarBankOnline.getLabel(), selection.selected, biayaAdministrasi), "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event eventConfirm) throws Exception {
								int i = Integer.parseInt(eventConfirm.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										BankHost bankHost = PembayaranUtil.getInstance().getBankHost(
												Common.getKonfigurasi(konfigurasiBankHost, "").getNilai(),
												"Bank Host");
										Map param = new HashMap();
										if (gatewayFlag != null && gatewayFlag.length() > 0) {
											param.put(gatewayFlag, Boolean.TRUE);
										}
										String waktuSampai = null;
										VirtualAccountBank virtualAccountBank = DownloadTagihanMahasiswaBankOnline.sendRequest(
												selection.mahasiswa, null, selection.selected, biayaAdministrasi, perguruanTinggi,
												bankHost, param, waktuSampai);
										if (param.get("jangan_notif") != null && (Boolean) param.get("jangan_notif")) {
											return;
										}
										String prefixBankLain = Common.getKonfigurasi(konfigurasiPrefixBank, "")
												.getNilai();
										MahasiswaVirtualAccountHelper.tampilkanHasilVirtualAccount(virtualAccountBank,
												selection.mahasiswa, null, biayaAdministrasi,
												MahasiswaVirtualAccountHelper.DEFAULT_VA_WINDOW, prefixBankLain);
										Common.freeze(grid, true);
										bayarBankOnline.setDisabled(true);
									}
								}, "Proses pembayaran ..");
							}
						});
			}
		});
	}

	private static class KeranjangSelection {
		private Set<KegiatanTemporary> selected;
		private Mahasiswa mahasiswa;
		private Double total;
	}

	class KegiatanTemporaryRenderer extends ais.ui.util.MyRowRenderer {

		Tbmuser tbmuser = Common.getCurrentUser();

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KegiatanTemporary kegiatanTemporary = (KegiatanTemporary) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				private void reload() {

					Common.clear(detail);

					Vlayout vlayout = new Vlayout();
					vlayout.setParent(detail);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(vlayout);

					MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							RevisiCicilanPembayaranTemporaryHelper revisiHelper = new RevisiCicilanPembayaranTemporaryHelper(
									new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													reload();
												}
											});
										}
									}, kegiatanTemporary);
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
							revisiHelper.setVisible(true);
							revisiHelper.onModal();

						}

					});
					button.setParent(toolbar);

					Session session = HibernateUtil.currentSession();
					@SuppressWarnings("unchecked")
					List<CicilanPembayaran> cicilanPembayarans = session.createCriteria(CicilanPembayaran.class)
							.add(Restrictions.eq("kegiatanTemporary", kegiatanTemporary)).list();

					MyGrid grid = new MyGrid();
					grid.setParent(vlayout);
					ListModel strset = new SimpleListModel(cicilanPembayarans);
					grid.setRowRenderer(new DetailKegiatanTemporaryRenderer());
					grid.setModelCheckMobile(strset);
				}

				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						reload();

					}
				}
			});

			if (kegiatanTemporary.getKegiatan() == null) {
				final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Pilih");
				checkboxConfig.setParent(arg0);
				arg0.setAttribute("checkboxConfig", checkboxConfig);
				arg0.setAttribute("kegiatanTemporary", kegiatanTemporary);
				// checkboxConfig.addEventListener("onClick", new
				// EventListener() {
				//
				// @Override
				// public void onEvent(Event arg0) throws Exception {
				// if (checkboxConfig.isChecked()) {
				// selectedKegiatanTemporary.add(kegiatanTemporary);
				// } else {
				// selectedKegiatanTemporary.remove(kegiatanTemporary);
				// }
				// }
				// });
			} else {
				new Label().setParent(arg0);
			}

			if (kegiatanTemporary.getMahasiswa() != null) {

				new Label(kegiatanTemporary.getMahasiswa() == null ? "" : kegiatanTemporary.getMahasiswa().getNim())
						.setParent(arg0);

				RevisiHelper.createNewRevisi(KegiatanTemporary.class, kegiatanTemporary,
						kegiatanTemporary.getMahasiswa() == null ? "" : kegiatanTemporary.getMahasiswa().getNama())
						.setParent(arg0);

				new Label(kegiatanTemporary.getAmount() == null ? "0"
						: Common.numberFormat.get().format(kegiatanTemporary.getAmount())).setParent(arg0);

				new Label(kegiatanTemporary.getMahasiswa() == null
						|| kegiatanTemporary.getMahasiswa().getJurusan() == null ? ""
								: kegiatanTemporary.getMahasiswa().getJurusan().getNama())
						.setParent(arg0);
				new Label(kegiatanTemporary.getMahasiswa() == null
						|| kegiatanTemporary.getMahasiswa().getJurusan() == null
						|| kegiatanTemporary.getMahasiswa().getJurusan().getFakultas() == null ? ""
								: kegiatanTemporary.getMahasiswa().getJurusan().getFakultas().getNama())
						.setParent(arg0);
			} else if (kegiatanTemporary.getCalonMahasiswa() != null) {

				if (kegiatanTemporary.getJenisKegiatan() != null && kegiatanTemporary.getJenisKegiatan().getId()
						.equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
					new Label(kegiatanTemporary.getCalonMahasiswa() == null ? ""
							: (kegiatanTemporary.getCalonMahasiswa().getNoUjian() == null
									? kegiatanTemporary.getCalonMahasiswa().getNoRegistrasi()
									: kegiatanTemporary.getCalonMahasiswa().getNoUjian()))
							.setParent(arg0);
				} else {
					new Label(kegiatanTemporary.getCalonMahasiswa() == null ? ""
							: kegiatanTemporary.getCalonMahasiswa().getNoRegistrasi()).setParent(arg0);
				}

				RevisiHelper.createNewRevisi(KegiatanTemporary.class, kegiatanTemporary,
						kegiatanTemporary.getCalonMahasiswa() == null ? ""
								: kegiatanTemporary.getCalonMahasiswa().getNama())
						.setParent(arg0);
				new Label(kegiatanTemporary.getAmount() == null ? "0"
						: Common.numberFormat.get().format(kegiatanTemporary.getAmount())).setParent(arg0);

				new Label(kegiatanTemporary.getCalonMahasiswa() == null
						|| kegiatanTemporary.getCalonMahasiswa().getProdiLulus() == null
								? (kegiatanTemporary.getCalonMahasiswa().getProdi1() == null ? ""
										: kegiatanTemporary.getCalonMahasiswa().getProdi1().getNama())
								: kegiatanTemporary.getCalonMahasiswa().getProdiLulus().getNama())
						.setParent(arg0);
				new Label(kegiatanTemporary.getCalonMahasiswa() == null
						|| kegiatanTemporary.getCalonMahasiswa().getProdiLulus() == null
						|| kegiatanTemporary.getCalonMahasiswa().getProdiLulus().getFakultas() == null
								? (kegiatanTemporary.getCalonMahasiswa().getProdi1() == null ? ""
										: kegiatanTemporary.getCalonMahasiswa().getProdi1().getFakultas().getNama())
								: kegiatanTemporary.getCalonMahasiswa().getProdiLulus().getFakultas().getNama())
						.setParent(arg0);
			}
			new Label(kegiatanTemporary.getSemster() + "").setParent(arg0);

			new Label(Common.dateFormat3.get().format(kegiatanTemporary.getTanggal())).setParent(arg0);
			new Label(kegiatanTemporary.getJenisKegiatan() == null ? ""
					: kegiatanTemporary.getJenisKegiatan().getNamaKegiatan()).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Batalkan", "/img/svg/warning-outline.svg");
			button.setTooltiptext("Reversal");
			button.setOrient("vertical");

			button.setVisible(kegiatanTemporary.getKegiatan() == null);

			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin melakukan membatalkan pada keranjang pembayaran ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(kegiatanTemporary);

											onSearchDefault(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat Reversal .., error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		return initCriteria(order, Projections.groupProperty("kegiatanTemporary"));
	}

	public Criteria initCriteria(boolean order, Projection projection) {
		Session session = HibernateUtil.currentSession();
		Fakultas fakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
				|| searchfakultas.getSelectedItem().getValue() == null
				|| searchfakultas.getSelectedItem().getValue() == null ? null
						: searchfakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchjurusan.getSelectedItem() == null
				|| searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null ? null
						: searchjurusan.getSelectedItem().getValue());

		Criteria criteria = session.createCriteria(CicilanPembayaran.class).setProjection(projection);

		if (order)
			criteria.addOrder(Order.desc("kegiatanTemporary.id"));

		criteria.createCriteria("kegiatanTemporary")

				.add(searchJenisPembayaran.getSelectedItem() == null
						|| searchJenisPembayaran.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisKegiatan", searchJenisPembayaran.getSelectedItem().getValue()))

				.add(jenissemester.getSelectedItem() == null || jenissemester.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: jenissemester.getSelectedItem().getValue().toString().equalsIgnoreCase(Perkuliahan.GENAP)
								? Restrictions.in("semster", Common.genap)
								: Restrictions.in("semster", Common.ganjil))

				.add(mahasiswaMaster == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("mahasiswa", mahasiswaMaster))

				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa", "calonMahasiswa", Criteria.LEFT_JOIN)

				.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)
				.createAlias("calonMahasiswa.prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)

				.add(searchtahun.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("mahasiswa.tahunangkatan", searchtahun.getValue().intValue()),
								Restrictions.eq("calonMahasiswa.tahun", searchtahun.getValue().intValue())))

				.add(jurusan == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("mahasiswa.jurusan", jurusan),
								Restrictions.eq("calonMahasiswa.prodiLulus", jurusan)))

				.add(searchnamamhs.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nama", searchnamamhs.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.nama", searchnamamhs.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add((start == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (start.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("(this_.tanggal) >= ('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + " 00:00:00')")))

				.add((end == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (end.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("(this_.tanggal) <= ('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + " 23:59:59')")))

				.add(fakultas == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.eq("jurusan.fakultas", fakultas),
								Restrictions.eq("prodiLulus.fakultas", fakultas)))

				.add(Restrictions.or(
						Restrictions.or(Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("calonMahasiswa.nim", searchnama.getValue().trim(),
										MatchMode.ANYWHERE)),
								Restrictions.ilike("calonMahasiswa.noRegistrasi", searchnama.getValue().trim(),
										MatchMode.ANYWHERE)),
						Restrictions.ilike("calonMahasiswa.noUjian", searchnama.getValue().trim())));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		try {
			MahasiswaVirtualAccountHelper.tampilkanProgress("Keranjang Pembayaran",
					"Menyiapkan filter dan menghitung jumlah data keranjang pembayaran.", 15);
			Common.initPaging25(initCriteria(false, Projections.countDistinct("kegiatanTemporary")), paging, null, null);

			MahasiswaVirtualAccountHelper.tampilkanProgress("Keranjang Pembayaran",
					"Mengambil daftar tagihan keranjang sesuai halaman yang sedang dibuka.", 55);
			List<KegiatanTemporary> kegiatanTemporary = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_25)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE_25 * (paging == null ? 0 : paging.getActivePage())).list();
			perbaruiRingkasanKeranjangPembayaran(kegiatanTemporary);

			MahasiswaVirtualAccountHelper.tampilkanProgress("Keranjang Pembayaran",
					"Menyusun tabel, rincian tagihan, dan tombol pembayaran online.", 88);
			ListModel strset = new SimpleListModel(kegiatanTemporary);
			grid.setRowRenderer(new KegiatanTemporaryRenderer());
			grid.setModelCheckMobile(strset);

			MahasiswaVirtualAccountHelper.tampilkanProgress("Keranjang Pembayaran",
					"Data keranjang pembayaran selesai ditampilkan.", 100);
		} finally {
			MahasiswaVirtualAccountHelper.sembunyikanProgress();
		}
	}
}
