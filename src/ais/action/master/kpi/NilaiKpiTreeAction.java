package ais.action.master.kpi;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treecol;
import org.zkoss.zul.Treecols;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.kpi.helper.AmbilDataPegawaiFormatKPIBanbox;
import ais.action.master.kpi.helper.DokumenBuktiKpi;
import ais.action.master.kpi.helper.ItemKpiTreeModel;
import ais.action.master.kpi.helper.KpiUtil;
import ais.action.report.kpi.LaporanNilaiKpi;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.Pegawai;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.kpi.ItemKpi;
import ais.database.model.kpi.MasaKpi;
import ais.database.model.kpi.NilaiKpi;
import ais.database.model.kpi.PenilaianKpi;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

public class NilaiKpiTreeAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Tree tree;

	private AmbilDataPegawaiFormatKPIBanbox pegawaiBanbox;
	private Combobox ta;

	private ItemKpiTreeModel itemKpiTreeModel;

	private TreeMap<Long, Treecell[]> treecellMap = new TreeMap<Long, Treecell[]>();

	private Date sekarang;
	private MyLabelConfig pegawaiBanboxLabel;
	private MyLabelBold totalTarget;
	private MyLabelBold totalRealisasi;
	private MyLabelBold totalPersen;
	private PenilaianKpi penilaianKpi;

	private boolean adminLainBoleh;

	private MyToolbarbuttonConfig upload;

	private boolean ubah = true;

	private MyToolbarbuttonConfig cetakToolbarbutton;

	private MyCheckboxConfig tampilkanFormula;

	private Tbmuser tbmuser;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (execution.getParameter("id") == null || execution.getParameter("id").trim().isEmpty()) {
			if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
				session.removeAttribute("usersTemp");
				Common.goLogoff();
				return;
			}
		}

		ubah = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		tbmuser = Common.getCurrentUser();
		if (execution.getParameter("kunci") != null && execution.getParameter("kunci").equalsIgnoreCase("true")
				&& tbmuser.getPegawai() != null) {
			pegawaiBanbox.setAttribute("pegawai", tbmuser.getPegawai());
			pegawaiBanbox.setAttribute("myValue", tbmuser.getPegawai());
			pegawaiBanbox.setValue(tbmuser.getPegawai().getNama());
			pegawaiBanbox.setDisabled(true);

			pegawaiBanboxLabel.setVisible(false);
			pegawaiBanbox.setVisible(false);
		}

		adminLainBoleh = tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.ambilDosen() == null && tbmuser.getSiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null && tbmuser.getCalonSiswa() == null;

		sekarang = WaktuUtil.getDate();

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pegawai pegawai = (Pegawai) pegawaiBanbox.getAttribute("pegawai");
				if (pegawai == null) {
					MyMessageboxConfig.show("Mohon maaf, data pegawai belum diisi. Langkah yang dapat dilakukan: (1) pilih pegawai terlebih dahulu; (2) ulangi proses.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return;
				}
				FormatKpiDetail formatKpiDetail = pegawai.ambilFormatKpiDetail(sekarang);
				if (formatKpiDetail == null) {
					MyMessageboxConfig.show("Mohon maaf, Format KPI untuk pegawai ini belum ditentukan. Langkah yang dapat dilakukan: (1) tentukan Format KPI pegawai terlebih dahulu melalui menu Format KPI; (2) ulangi proses.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				onReloadTree(arg0);
			}
		};

		String[] contents = new String[] { "id", "kode", "val", "valtampil", "penilaianKpi", "itemKpi", "realisasi",
				"persen", "keterangan" };
		cetakToolbarbutton = Common.cetakData(NilaiKpi.class, new DataCriteria() {

			@Override
			public Object initCriteria(boolean order) {

				Pegawai pegawai = (Pegawai) pegawaiBanbox.getAttribute("pegawai");
				if (pegawai == null) {
					return null;
				}

				FormatKpiDetail formatKpiDetail = pegawai.ambilFormatKpiDetail(sekarang);

				if (formatKpiDetail == null) {
					return null;
				}

				String t = (String) ta.getSelectedItem().getValue();

				Session session = HibernateUtil.currentSession();
				penilaianKpi = (PenilaianKpi) ConstantValues.simpleObject(
						session.createCriteria(PenilaianKpi.class).add(Restrictions.eq("ta", t))
								.addOrder(Order.asc("id")).add(Restrictions.eq("pegawai", pegawai)).setMaxResults(1),
						PenilaianKpi.class);
				if (penilaianKpi == null) {
					penilaianKpi = new PenilaianKpi();
					penilaianKpi.setPegawai(pegawai);
					penilaianKpi.setTa(t);
					session.save(penilaianKpi);
					session.flush();
				}

				return HibernateUtil.currentSession().createCriteria(NilaiKpi.class)
						.add(Restrictions.eq("penilaianKpi", penilaianKpi)).createAlias("itemKpi", "itemKpi")
						.addOrder(Order.asc("itemKpi.nomorUrut")).addOrder(Order.asc("itemKpi.id"));
			}
		}, contents);
		Common.appendKeToolbar(cetakToolbarbutton, totalTarget, comp);

		upload = Common.uploadData(new DataSearchDefault() {

			@Override
			public void onSearchDefault(Event event) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						onReloadTree(null);
					}
				});

			}
		}, NilaiKpi.class, contents);
		if (upload != null) { upload.setVisible(Common.getApakahAdmin()); }
		Common.appendKeToolbar(upload, totalTarget, comp);

		Common.generateTahunAjaranJuniJuli(ta);
		ta.addEventListener("onChange", eventListener);
		if (pegawaiBanbox != null) { pegawaiBanbox.setEventListener(eventListener); }
		if (ta != null) { ta.setReadonly(true); }
		if (pegawaiBanbox != null) { pegawaiBanbox.setReadonly(true); }
		if (pegawaiBanbox != null) { pegawaiBanbox.setCols(8); }

		if (execution.getParameter("id") != null) {

			penilaianKpi = (PenilaianKpi) ConstantValues.ambil(PenilaianKpi.class.getName(),
					Long.parseLong(execution.getParameter("id")));
			if (penilaianKpi != null) {
				pegawaiBanbox.setAttribute("pegawai", penilaianKpi.getPegawai());
				pegawaiBanbox.setAttribute("myValue", penilaianKpi.getPegawai());
				pegawaiBanbox.setValue(penilaianKpi.getPegawai().getNama());
				pegawaiBanbox.setDisabled(true);

				Common.selectComboItem(ta, penilaianKpi.getTa());
				ta.setDisabled(true);
//				upload.setVisible(false);

				ubah = false;
			}
		} else {
			tampilkanKunci();
		}

		tampilkanFormula = new MyCheckboxConfig("Formula");
		if (tampilkanFormula != null) { tampilkanFormula.setVisible(Common.getApakahAdmin()); }
		Common.appendKeToolbar(tampilkanFormula, totalTarget, comp);
		tampilkanFormula.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReloadTree(null);
			}
		});

		onReloadTree(null);

	}

	public void onCetak(Event event) throws Exception {
		Pegawai pegawai = (Pegawai) pegawaiBanbox.getAttribute("pegawai");
		if (pegawai == null) {
			MyMessageboxConfig.show("Mohon maaf, data pegawai belum diisi. Langkah yang dapat dilakukan: (1) pilih pegawai terlebih dahulu; (2) ulangi proses cetak.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		FormatKpiDetail formatKpiDetail = pegawai.ambilFormatKpiDetail(sekarang);
		if (formatKpiDetail == null) {
			MyMessageboxConfig.show("Mohon maaf, Format KPI untuk pegawai ini belum ditentukan. Langkah yang dapat dilakukan: (1) tentukan Format KPI pegawai terlebih dahulu melalui menu Format KPI; (2) ulangi proses cetak.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}

		String t = (String) ta.getSelectedItem().getValue();

		Session session = HibernateUtil.currentSession();
		penilaianKpi = (PenilaianKpi) ConstantValues.simpleObject(
				session.createCriteria(PenilaianKpi.class).add(Restrictions.eq("ta", t))
						.add(Restrictions.eq("pegawai", pegawai)).addOrder(Order.asc("id")).setMaxResults(1),
				PenilaianKpi.class);
		if (penilaianKpi == null) {
			penilaianKpi = new PenilaianKpi();
			penilaianKpi.setPegawai(pegawai);
			penilaianKpi.setTa(t);
			session.save(penilaianKpi);
			session.flush();
		}

		LaporanNilaiKpi laporanNilaiKpi = new LaporanNilaiKpi(penilaianKpi, formatKpiDetail);
		laporanNilaiKpi.setTitle("Cetak Nilai KPI");
		page.getFirstRoot().appendChild(laporanNilaiKpi);
		laporanNilaiKpi.setHeight("95%");
		laporanNilaiKpi.setWidth("90%");
		laporanNilaiKpi.setClosable(true);
		laporanNilaiKpi.onModal();
	}

	private Toolbarbutton bukaKunciDetail;
	private Toolbarbutton kunciDetail;

	private void tampilkanKunci() {

		kunciDetail.setTooltiptext("Klik untuk meng-kunci penilaian KPI ini");

		kunciDetail.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin mengunci penilaian KPI ini? Setelah dikunci, penilaian tidak dapat diubah sampai kunci dibuka kembali.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									Tbmuser tbmuser = Common.getCurrentUser();
									penilaianKpi.setKunci(tbmuser);
									Common.refreshUpdate(penilaianKpi);

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											onReloadTree(arg0);
										}
									});

								}

							}
						});
			}
		});

		bukaKunciDetail.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membuka kunci penilaian KPI ini? Setelah kunci dibuka, penilaian dapat diubah kembali.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									penilaianKpi.setKunci(null);
									Common.refreshUpdate(penilaianKpi);

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											onReloadTree(arg0);
										}
									});

								}

							}
						});
			}
		});

	}

	private void initTree() throws Exception {

		Treecols treecols = new Treecols();
		Treecol treecol = new Treecol("KPI");
		treecol.setParent(treecols);

		treecol = new Treecol("Target");
		treecol.setWidth("8%");
		treecol.setParent(treecols);

		treecol = new Treecol("Realisasi");
		treecol.setWidth("8%");
		treecol.setParent(treecols);

		treecol = new Treecol("Satuan");
		treecol.setWidth("8%");
		treecol.setParent(treecols);

		if (tampilkanFormula.isChecked()) {
			treecol = new Treecol("Formula Rumus");
			treecol.setWidth("15%");
			treecol.setParent(treecols);

			treecol = new Treecol("Formula Hitungan");
			treecol.setWidth("15%");
			treecol.setParent(treecols);
		}

		treecol = new Treecol("Poin Target");
		treecol.setWidth("8%");
		treecol.setParent(treecols);

		treecol = new Treecol("Poin Realisasi");
		treecol.setWidth("8%");
		treecol.setParent(treecols);

		treecol = new Treecol("%");
		treecol.setWidth("5%");
		treecol.setParent(treecols);

		if (!tampilkanFormula.isChecked()) {
			treecol = new Treecol("Keterangan");
			treecol.setWidth("18%");
			treecol.setParent(treecols);

			treecol = new Treecol("Bukti Realisasi");
			treecol.setWidth("15%");
			treecol.setParent(treecols);
		}

		if (pegawaiBanbox.isVisible()) {
			treecol = new Treecol("");
			treecol.setWidth(adminLainBoleh ? "25px" : "0px");
			treecol.setParent(treecols);
		}
		treecols.setParent(tree);
	}

	public void onReloadTreeRefresh(Event event) throws Exception {
		reload(true);
	}

	public void onReloadTree(Event event) throws Exception {
		reload(false);
	}

	private void reload(final boolean refresh) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
//				upload.setVisible(false);
				Common.clear(tree);

				Pegawai pegawai = (Pegawai) pegawaiBanbox.getAttribute("pegawai");
				if (pegawai == null) {
					return;
				}

				final FormatKpiDetail formatKpiDetail = pegawai.ambilFormatKpiDetail(sekarang);

				if (formatKpiDetail == null) {
					return;
				}

				String t = (String) ta.getSelectedItem().getValue();

				Session session = HibernateUtil.currentSession();
				penilaianKpi = (PenilaianKpi) ConstantValues.simpleObject(
						session.createCriteria(PenilaianKpi.class).add(Restrictions.eq("ta", t))
								.add(Restrictions.eq("pegawai", pegawai)).addOrder(Order.asc("id")).setMaxResults(1),
						PenilaianKpi.class);
				if (penilaianKpi == null) {
					penilaianKpi = new PenilaianKpi();
					penilaianKpi.setPegawai(pegawai);
					penilaianKpi.setTa(t);
					session.save(penilaianKpi);
					session.flush();
				}

				Sessions.getCurrent(true).setAttribute("nama_crit_" + NilaiKpi.class.getSimpleName(), "penilaianKpi");
				Sessions.getCurrent(true).setAttribute("nilai_crit_" + NilaiKpi.class.getSimpleName(), penilaianKpi);

				Tbmuser tbmuser = Common.getCurrentUser();
				bukaKunciDetail.setVisible(penilaianKpi.getKunci() != null);
				if (penilaianKpi.getKunci() != null) {
					bukaKunciDetail.setTooltiptext("Dikunci oleh " + penilaianKpi.getKunci().getUserId());
				}

				bukaKunciDetail.setVisible(penilaianKpi.getKunci() != null);
				bukaKunciDetail.setDisabled(tbmuser == null || penilaianKpi.getKunci() == null
						|| !penilaianKpi.getKunci().getUserId().equals(tbmuser.getUserId()));
				kunciDetail.setVisible(penilaianKpi.getKunci() == null);

//				upload.setVisible(penilaianKpi.getKunci() == null);

				bukaKunciDetail.setLabel(penilaianKpi.getKunci() == null ? "" : penilaianKpi.getKunci().getUserNama());
				kunciDetail.setLabel(penilaianKpi.getKunci() == null ? "Kunci" : penilaianKpi.getKunci().getUserNama());

				if (tbmuser != null && tbmuser.getPegawai() != null
						&& tbmuser.getPegawai().getId().equals(pegawai.getId())) {
					bukaKunciDetail.setVisible(false);
					kunciDetail.setVisible(false);
				}

				totalTarget.setValue(Common.numberFormat.get().format(formatKpiDetail.getNilai()));
				totalRealisasi.setValue(Common.numberFormat.get().format(penilaianKpi.getNilai()));

				Double persen = ((penilaianKpi.getNilai() * 100.0) / formatKpiDetail.getNilai());
				totalPersen.setValue(Common.numberFormat.get().format(persen) + "% (" + penilaianKpi.getKode() + ")");

				initTree();

				itemKpiTreeModel = new ItemKpiTreeModel(true, formatKpiDetail);
				tree.setModel(itemKpiTreeModel);
				tree.setItemRenderer(new ais.ui.util.MyTreeitemRenderer() {

					@Override
					public void render(final Treeitem treeitem, Object arg1) throws Exception {
						final ItemKpi itemKpi = (ItemKpi) arg1;

						try {
							Common.clear(treeitem);
							final Treerow treerow = new Treerow();
							treerow.setParent(treeitem);

							hasSomeChilds(treerow, itemKpi, formatKpiDetail, refresh, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									reloadTreeitem(treeitem, true, refresh, false);
								}
							});

						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
			}
		});

	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotal, final boolean refresh,
			final Boolean loadParent) {
		reloadTreeitem(treeitem, reloadTotal, loadParent, refresh, null);
	}

	private void reloadTreeitem(final Treeitem treeitem, final Boolean reloadTotal, final boolean refresh,
			final Boolean loadParent, final EventListener eventListener) {
		final Treeitem treeitemParent = loadParent ? treeitem.getParentItem() : treeitem;
		if (treeitemParent == null) {
			try {
				onReloadTree(null);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} else {
			treeitemParent.unload();
			final Timer timer = new Timer(300);
			timer.setParent(page.getFirstRoot());
			timer.addEventListener("onTimer", new EventListener() {

				@SuppressWarnings({})
				@Override
				public void onEvent(Event arg0) throws Exception {
					treeitemParent.setOpen(true);
					treeitem.setOpen(true);
					if (reloadTotal) {
						reloadTotal(refresh);
					}
					if (eventListener != null) {
						eventListener.onEvent(null);
					}
					timer.detach();
				}

			});

			timer.start();
		}
	}

	private void reloadTotal(boolean refresh) throws Exception {
		// TODO Auto-generated method stub
		for (Long nilaiKpiid : treecellMap.keySet()) {
			Treecell treecell = treecellMap.get(nilaiKpiid)[0];
			NilaiKpi nilaiKpi = (NilaiKpi) treecell.getAttribute("nilaiKpi");
			String target = KpiUtil.ambilTarget(nilaiKpi.getItemKpi().getFormula(), sekarang);
			Double hasil = itemKpiTreeModel.hitungNilaiKpi(nilaiKpi, target, penilaianKpi, refresh, null);
			treecell.setLabel(Common.numberFormat.get().format(hasil));

			if (nilaiKpi.getRealisasi().intValue() != hasil.intValue()) {
				Session session = HibernateUtil.currentSession();
				session.refresh(nilaiKpi);
				nilaiKpi.setRealisasi(hasil);
				Common.refreshUpdate(session, nilaiKpi);
				session.flush();
			}

		}

		for (Long nilaiKpiid : treecellMap.keySet()) {
			Treecell treecell = treecellMap.get(nilaiKpiid)[0];

			NilaiKpi nilaiKpi = (NilaiKpi) treecell.getAttribute("nilaiKpi");

			if (nilaiKpi.getItemKpi().getKpi().getNilaifinal() && penilaianKpi != null
					&& nilaiKpi.getRealisasi() != null
					&& penilaianKpi.getNilai().intValue() != nilaiKpi.getRealisasi().intValue()) {
				Session session = HibernateUtil.currentSession();
				session.refresh(penilaianKpi);
				penilaianKpi.setNilai(nilaiKpi.getRealisasi());
				Double persen = ((penilaianKpi.getNilai() * 100.0) / nilaiKpi.getItemKpi().getTarget());
				penilaianKpi.setPersen(persen);
				Common.refreshUpdate(session, penilaianKpi);
				session.flush();
			}

			Treecell treecellPersen = treecellMap.get(nilaiKpiid)[1];
			if (nilaiKpi.getItemKpi().getKpi().getNilaifinal()) {
				totalTarget.setValue(Common.numberFormat.get().format(nilaiKpi.getItemKpi().getTarget()));
				totalRealisasi.setValue(Common.numberFormat.get().format(penilaianKpi.getNilai()));
				totalPersen.setValue(
						Common.numberFormat.get().format(penilaianKpi.getPersen()) + "% (" + penilaianKpi.getKode() + ")");
				treecellPersen.setLabel(Common.numberFormat.get().format(penilaianKpi.getPersen()) + "%");
			} else {
				treecellPersen.setLabel(Common.numberFormat.get().format(nilaiKpi.getPersen()) + "%");
			}

		}
	}

	@SuppressWarnings({ "unchecked" })
	public void openChilds(final Treeitem treeitemParent, int max, int index) {
		if (max > index) {
			treeitemParent.setOpen(true);
			List<Treeitem> treeitems = treeitemParent.getChildren();
			for (Object object : treeitems) {

				if (object instanceof Treechildren) {
					Treechildren treechildren = (Treechildren) object;
					List<Treeitem> mytreeitems = treechildren.getChildren();
					for (Treeitem treeitem : mytreeitems) {
						openChilds(treeitem, max, (++index));
					}

				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	public void closeChilds(final Treeitem treeitemParent) {
		treeitemParent.setOpen(false);
		List<Treeitem> treeitems = treeitemParent.getChildren();
		for (Object object : treeitems) {

			if (object instanceof Treechildren) {
				Treechildren treechildren = (Treechildren) object;
				List<Treeitem> mytreeitems = treechildren.getChildren();
				for (Treeitem treeitem : mytreeitems) {
					closeChilds(treeitem);
				}

			}
		}
	}

	private Map<String, NilaiKpi> mapNilaiKpi = new HashMap<String, NilaiKpi>();

	private void hasSomeChilds(Treerow treerow, final ItemKpi itemKpi, FormatKpiDetail formatKpiDetail,
			final boolean refresh, final EventListener eventListener) throws Exception {

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
		final NilaiKpi nilaiKpi = nilaiKpiTemp;

		String bg = "color:" + itemKpi.getKpi().getColor() + ";";
		if (!itemKpi.getKpi().getTanpaWarnaBackgrond()) {
			bg = bg + "background-color:" + itemKpi.getKpi().getBackground() + ";";
		}

		String style = "font-size:x-small;text-align: left;" + bg;
		String style1 = "font-size:x-small;text-align: right;" + bg;

		if (itemKpi.getKpi().getFontBold() && itemKpi.getKpi().getFontBesar()) {
			style = "font-size:small;text-align: left; font-weight: bold;color:" + itemKpi.getKpi().getColor() + ";"
					+ bg;
			style1 = "font-size:small;text-align: right; font-weight: bold;color:" + itemKpi.getKpi().getColor() + ";"
					+ bg;
		} else if (itemKpi.getKpi().getFontBesar()) {
			style = "font-size:small;text-align: left;color:" + itemKpi.getKpi().getColor() + ";" + bg;
			style1 = "font-size:small;text-align: right;color:" + itemKpi.getKpi().getColor() + ";" + bg;
		} else if (itemKpi.getKpi().getFontBold()) {
			style = "font-size:x-small;text-align: left; font-weight: bold;color:" + itemKpi.getKpi().getColor() + ";"
					+ bg;
			style1 = "font-size:x-small;text-align: right; font-weight: bold;color:" + itemKpi.getKpi().getColor() + ";"
					+ bg;
		}

		Treecell treecell = new Treecell(
				itemKpi.getKode() + "-" + itemKpi.getNama() + (itemKpi.getKpi().getNilaifinal() ? "(Final)" : ""));
		treecell.setTooltiptext(itemKpi.toString());
		treecell.setStyle(style);
		treecell.setParent(treerow);

		ParameterTambahan parameterTambahan = itemKpi.getKpi().getSatuanKpi() == null ? null
				: itemKpi.getKpi().getSatuanKpi().getParameterTambahan();

		treecell = new Treecell(parameterTambahan == null ? "" : itemKpi.getValtampil());
		treecell.setTooltiptext(itemKpi.toString());
		treecell.setStyle(style1);
		treecell.setParent(treerow);

		Session session = HibernateUtil.currentSession();
		MasaKpi masaKpi = (MasaKpi) ConstantValues.simpleObject(
				session.createCriteria(MasaKpi.class)
						.add(Restrictions.sqlRestriction("date('" + Common.databaseDateFormat.get().format(sekarang)
								+ "') between this_.mulai and this_.sampai"))
						.add(Restrictions.eq("ta", penilaianKpi.getTa())).addOrder(Order.desc("id")).setMaxResults(1),
				MasaKpi.class);

		boolean editUpload = ubah && penilaianKpi.getKunci() == null
				&& (penilaianKpi.getPengajuanKpi() == null || penilaianKpi.getPengajuanKpi().getDisetujuiOleh() == null)
				&& masaKpi != null;

		if (penilaianKpi.getKunci() == null && tbmuser != null && tbmuser.getUserId() != null) {

//			System.out.println("formatKpiDetail.getFormatKpi().getUsernamePenggunaRealisasi() -> "
//					+ formatKpiDetail.getFormatKpi().getUsernamePenggunaRealisasi() + ", tbmuser.getUserId() -> "
//					+ tbmuser.getUserId());
			Tbmrole tbmrole = tbmuser.hakAkses();
			if (formatKpiDetail.getFormatKpi() != null && (formatKpiDetail.getFormatKpi().getUsernamePenggunaRealisasi()
					.toLowerCase().trim().contains("," + tbmuser.getUserId().trim().toLowerCase() + ",")
					|| (tbmrole != null && formatKpiDetail.getFormatKpi().getJenisPenggunaRealisasi().toLowerCase()
							.trim().contains("," + tbmrole.getRoleId().trim().toLowerCase() + ",")))) {
				editUpload = true;
			}

		}

		if (editUpload) {
			treecell = new Treecell();
			treecell.setStyle(style);
			treecell.setParent(treerow);
			if (parameterTambahan != null) {
				Component component = ParameterTambahan.ambilComponent(nilaiKpi.getVal(), parameterTambahan,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ParameterTambahan parameterTambahan = itemKpi.getKpi().getSatuanKpi() == null ? null
										: itemKpi.getKpi().getSatuanKpi().getParameterTambahan();
								String val = ParameterTambahan.ambilValComponent(arg0.getTarget(), parameterTambahan);
								try {
									val = val.split(":")[1];
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/NilaiKpiTreeAction.java:760");
									// TODO: handle exception
								}
								nilaiKpi.setVal(val);
								try {
									if (arg0.getTarget() instanceof Textbox) {
										nilaiKpi.setValtampil(((Textbox) arg0.getTarget()).getValue());
									} else if (arg0.getTarget() instanceof Doublebox) {
										nilaiKpi.setValtampil(
												Common.numberFormat.get().format(((Doublebox) arg0.getTarget()).getValue()));
									} else if (arg0.getTarget() instanceof Intbox) {
										nilaiKpi.setValtampil(
												Common.numberFormat.get().format(((Intbox) arg0.getTarget()).getValue()));
									} else if (arg0.getTarget() instanceof Longbox) {
										nilaiKpi.setValtampil(
												Common.numberFormat.get().format(((Longbox) arg0.getTarget()).getValue()));
									} else if (arg0.getTarget() instanceof Intbox) {
										nilaiKpi.setValtampil(
												Common.numberFormat.get().format(((Intbox) arg0.getTarget()).getValue()));
									} else if (arg0.getTarget() instanceof Datebox) {
										nilaiKpi.setValtampil(
												Common.dateFormat.get().format(((Datebox) arg0.getTarget()).getValue()));
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/kpi/NilaiKpiTreeAction.java:783");
									// TODO: handle exception
								}
								Common.refreshUpdate(nilaiKpi);
								eventListener.onEvent(arg0);
							}
						});

				treecell.appendChild(component);
			}
		} else {
			treecell = new Treecell(parameterTambahan != null ? nilaiKpi.getValtampil() : "");
			treecell.setStyle(style1);
			treecell.setParent(treerow);
		}

		treecell = new Treecell(itemKpi.getKpi() == null || itemKpi.getKpi().getSatuanKpi() == null ? ""
				: itemKpi.getKpi().getSatuanKpi().getKode());
		treecell.setStyle(style);
		treecell.setParent(treerow);

		List<String> penghitungan = new ArrayList<String>();
		String format = KpiUtil.ambilTarget(itemKpi.getFormula(), sekarang);
		Double hasil = itemKpiTreeModel.hitungNilaiKpi(nilaiKpi, format, penilaianKpi, refresh, penghitungan);
		if (tampilkanFormula.isChecked()) {
			treecell = new Treecell(format);
			treecell.setStyle(style1);
			treecell.setParent(treerow);

			treecell = new Treecell();
			treecell.setStyle(style1);
			treecell.setParent(treerow);

			Vbox vbox = new Vbox();
			for (String ss : penghitungan) {
				vbox.appendChild(new MyLabelAgakKecil(ss));
			}
			treecell.appendChild(vbox);
		}

		treecell = new Treecell(Common.numberFormat.get().format(itemKpi.getTarget()));
		treecell.setStyle(style1);
		treecell.setParent(treerow);

		Treecell treecellHasil = new Treecell(Common.numberFormat.get().format(hasil));
		treecellHasil.setAttribute("nilaiKpi", nilaiKpi);
		treecellHasil.setTooltiptext(Common.numberFormat.get().format(hasil));
		treecellHasil.setStyle(style1);
		treecellHasil.setParent(treerow);

		Double persen = ((hasil * 100.0) / itemKpi.getTarget());
		Treecell treecellPersen = new Treecell(Common.numberFormat.get().format(persen) + "%");
		treecellPersen.setStyle(style1);
		treecellPersen.setParent(treerow);

		if (!tampilkanFormula.isChecked()) {
			if (editUpload) {
				final Textbox keterangan = new Textbox(nilaiKpi.getKeterangan());
				treecell = new Treecell();
				treecell.setParent(treerow);
				treecell.appendChild(keterangan);
				keterangan.setWidth("90%");
				keterangan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						nilaiKpi.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(nilaiKpi);
					}
				});
			} else {
				treecell = new Treecell(nilaiKpi.getKeterangan());
				treecell.setParent(treerow);
			}
		}

		if (nilaiKpi.getRealisasi().intValue() != hasil.intValue()) {

			session.refresh(nilaiKpi);
			nilaiKpi.setRealisasi(hasil);
			nilaiKpi.setPersen(persen);
			Common.refreshUpdate(session, nilaiKpi);
			session.flush();
		}

		if (itemKpi.getKpi().getNilaifinal() && penilaianKpi != null && hasil != null
				&& penilaianKpi.getNilai().intValue() != hasil.intValue()) {

			session.refresh(penilaianKpi);
			penilaianKpi.setNilai(hasil);
			Common.refreshUpdate(session, penilaianKpi);
			session.flush();
		}

		persen = ((penilaianKpi.getNilai() * 100.0) / itemKpi.getTarget());

		if (itemKpi.getKpi().getNilaifinal() && penilaianKpi != null && persen != null
				&& penilaianKpi.getPersen().intValue() != persen.intValue()) {
			session.refresh(penilaianKpi);
			penilaianKpi.setPersen(persen);
			Common.refreshUpdate(session, penilaianKpi);
			session.flush();
		}

		if (itemKpi.getKpi().getNilaifinal()) {
			totalTarget.setValue(Common.numberFormat.get().format(itemKpi.getTarget()));
			totalRealisasi.setValue(Common.numberFormat.get().format(penilaianKpi.getNilai()));
			totalPersen.setValue(
					Common.numberFormat.get().format(penilaianKpi.getPersen()) + "% (" + penilaianKpi.getKode() + ")");

			if (penilaianKpi != null && penilaianKpi.getTarget().intValue() != itemKpi.getTarget().intValue()) {
				session.refresh(penilaianKpi);
				penilaianKpi.setTarget(itemKpi.getTarget());
				Common.refreshUpdate(session, penilaianKpi);
				session.flush();
			}
		}

		if (!tampilkanFormula.isChecked()) {
			treecell = new Treecell();
			treecell.setParent(treerow);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Dokumen", "/img/svg/list-task.svg");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					DokumenBuktiKpi revisiHelper = new DokumenBuktiKpi(nilaiKpi);
					revisiHelper.setHeight("95%");
					revisiHelper.setWidth("750px");
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
					revisiHelper.setVisible(true);
					revisiHelper.onModal();
				}

			});
			button.setParent(treecell);
		}

		if (pegawaiBanbox.isVisible()) {
			treecell = new Treecell();
			treecell.setParent(treerow);
			RevisiHelper.createNewRevisi(NilaiKpi.class, nilaiKpi, "H").setParent(treecell);
		}

		treecellMap.put(itemKpi.getId(), new Treecell[] { treecellHasil, treecellPersen });

	}

}
