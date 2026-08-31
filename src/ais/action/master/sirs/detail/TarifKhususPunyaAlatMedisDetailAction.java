package ais.action.master.sirs.detail;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataAlatMedisBanyak;
import ais.action.master.sirs.util.CommonAlatMedis;
import ais.action.master.sirs.util.CommonAlatMedis.InitHarga;
import ais.action.master.sirs.util.CommonTarifAlatMedis;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.OnSave;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.Biaya;
import ais.database.model.sirs.BiayaAlatMedisPerKelas;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.TarifKhusus;
import ais.database.model.sirs.TarifKhususPunyaAlatMedis;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk tarif khusus punya alat medis detail. Tipe ini merupakan titik masuk
 * UI yang menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus
 * oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Tabpanel}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code TarifKhusus tarifKhusus}, {@code Paging
 * paging}, {@code Grid grid}, {@code InitHarga initHarga}, {@code String jenis}, {@code MyTextbox kode}, {@code
 * MyTextbox nama}, {@code TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis}; inisialisasi/lifecycle ({@code
 * initCriteria()}, {@code init()}); pembacaan/pencarian ({@code loadData()}); mutasi data ({@code onSave()});
 * operasi domain lain ({@code display()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Tabpanel
 */
public class TarifKhususPunyaAlatMedisDetailAction extends Tabpanel implements OnSave {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private TarifKhusus tarifKhusus;
	private Paging paging;
	private Grid grid;

	private InitHarga initHarga = new InitHarga();
	private String jenis;

	public TarifKhususPunyaAlatMedisDetailAction(TarifKhusus tarifKhusus, String jenis) {
		super();
		this.jenis = jenis;
		this.tarifKhusus = tarifKhusus;

	}

	class TarifKhususPunyaAlatMedisRenderer extends ais.ui.util.MyRowRenderer {

		public TarifKhususPunyaAlatMedisRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis = (TarifKhususPunyaAlatMedis) data;

			final AlatMedis alatMedis = tarifKhususPunyaAlatMedis.getAlatMedis();

			new Label(alatMedis.getKode()).setParent(arg0);
			new Label(alatMedis.getNama()).setParent(arg0);
			new Label(alatMedis.getJenisAlatMedis() == null ? "" : alatMedis.getJenisAlatMedis().getNama())
					.setParent(arg0);

			final MyTextbox keterangan = new MyTextbox(
					tarifKhususPunyaAlatMedis.getKeterangan() == null ? "" : tarifKhususPunyaAlatMedis.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setParent(arg0);

			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					tarifKhususPunyaAlatMedis.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (tarifKhususPunyaAlatMedis));
				}
			});

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Tarif", "/img/edit.gif");
			button.setTooltiptext("Tarif");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(tarifKhususPunyaAlatMedis);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();

											session.delete(session.merge(tarifKhususPunyaAlatMedis));

											loadData(null);

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/detail/TarifKhususPunyaAlatMedisDetailAction.java:151");
											MyMessageboxConfig.show(Common.pesan(
																"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
																	e.getMessage()));
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

		}
	}

	private MyTextbox kode;
	private MyTextbox nama;

	private TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis;

	private Criteria initCriteria(boolean order) {

		Criterion critKode = Restrictions.sqlRestriction("false");
		if (!kode.getValue().trim().equals("")) {
			critKode = Restrictions.or(critKode,
					Restrictions.ilike("alatMedis.kode", kode.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critKode = Restrictions.sqlRestriction("true");
		}

		Criterion critNama = Restrictions.sqlRestriction("false");
		if (!nama.getValue().trim().equals("")) {
			critNama = Restrictions.or(critNama,
					Restrictions.ilike("alatMedis.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		} else {
			critNama = Restrictions.sqlRestriction("true");
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TarifKhususPunyaAlatMedis.class)
				.createAlias("alatMedis", "alatMedis").add(Restrictions.eq("alatMedis.jenis", jenis)).add(critKode)
				.add(critNama)

				.add(Restrictions.eq("tarifKhusus", tarifKhusus));
		if (order)
			criteria.addOrder(Order.asc("alatMedis.nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TarifKhususPunyaAlatMedis> tarifKhususPunyaAlatMediss = tarifKhusus == null
				|| tarifKhusus.getId() == null
						? new ArrayList<TarifKhususPunyaAlatMedis>()
						: ConstantValues
								.simpleList(
										initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
												.setFirstResult(Common.ROWS_COUNT_ON_PAGE
														* (paging == null ? 0 : paging.getActivePage())),
										TarifKhususPunyaAlatMedis.class);

		ListModel strset = new SimpleListModel(tarifKhususPunyaAlatMediss);
		grid.setRowRenderer(new TarifKhususPunyaAlatMedisRenderer());
		grid.setModel(strset);
		grid.renderAll();
	}

	public void display() {

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(this);
		groupbox.appendChild(new Caption("Daftar alat medis dari jasa tenaga medis"));

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Ambil Alat Medis", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<AlatMedis> alatMediss = ConstantValues.simpleList(
						session.createCriteria(TarifKhususPunyaAlatMedis.class).add(Restrictions.isNotNull("alatMedis"))
								.setProjection(Projections.groupProperty("alatMedis.id"))
								.add(Restrictions.eq("tarifKhusus", tarifKhusus)),
						AlatMedis.class, false);

				AmbilDataAlatMedisBanyak ambilDataAlatMedisBanyak = new AmbilDataAlatMedisBanyak(alatMediss, jenis);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataAlatMedisBanyak);
				ambilDataAlatMedisBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<AlatMedis> alatMediss = (List<AlatMedis>) arg0.getData();
						Session session = HibernateUtil.currentSession();

						for (final AlatMedis alatMedis : alatMediss) {
							TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis = new TarifKhususPunyaAlatMedis();
							tarifKhususPunyaAlatMedis.setAlatMedis(alatMedis);
							tarifKhususPunyaAlatMedis.setKeterangan("");
							tarifKhususPunyaAlatMedis.setTarifKhusus(tarifKhusus);
							session.save(tarifKhususPunyaAlatMedis);

							List<KelasPerawatan> kelasPerawatans = session.createCriteria(KelasPerawatan.class)
									.addOrder(Order.asc("id")).list();

							for (KelasPerawatan kelasPerawatan : kelasPerawatans) {
								BiayaAlatMedisPerKelas biayaAlatMedisPerKelasDari = CommonTarifAlatMedis
										.getBiayaAlatMedisPerKelas(alatMedis, kelasPerawatan);

								BiayaAlatMedisPerKelas biayaAlatMedisPerKelasKe = (BiayaAlatMedisPerKelas) biayaAlatMedisPerKelasDari
										.clone();
								biayaAlatMedisPerKelasKe.setTarifKhususPunyaAlatMedis(tarifKhususPunyaAlatMedis);
								biayaAlatMedisPerKelasKe.setAlatMedis(null);
								session.save(biayaAlatMedisPerKelasKe);

								System.out.println("Saving biayaAlatMedisPerKelasKe " + biayaAlatMedisPerKelasKe);

								List<Biaya> biayasDari = session.createCriteria(Biaya.class)
										.add(Restrictions.isNull("detailTransaksiLayanan"))
										.add(Restrictions.isNull("detailTransaksi"))

										.add(Restrictions.eq("biayaAlatMedisPerKelas", biayaAlatMedisPerKelasDari))
										.list();

								System.out.println("Saving " + biayasDari);

								for (Biaya biaya : biayasDari) {
									Biaya newBiaya = (Biaya) biaya.clone();
									newBiaya.setId(null);
									newBiaya.setBiayaAlatMedisPerKelas(biayaAlatMedisPerKelasKe);
									session.save(newBiaya);
								}
							}
						}

						loadData(null);
					}
				});
				ambilDataAlatMedisBanyak.setWidth("95%");
				ambilDataAlatMedisBanyak.setHeight("97%");
				ambilDataAlatMedisBanyak.setVisible(true);
				ambilDataAlatMedisBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Download Biaya", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonAlatMedis.onDownloadBiaya(event, tarifKhusus, jenis);
			}

		});
		button.setParent(toolbar);

		button = new ais.ui.util.MyToolbarbuttonConfig("Upload Biaya", "/img/edit.gif");
		button.setUpload("true");
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonAlatMedis.onUploadBiaya(event, tarifKhusus, jenis);
			}

		});
		button.setParent(toolbar);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode:")));
		toolbar.appendChild(kode = new MyTextbox());
		kode.setWidth("80px");
		kode.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama:")));
		toolbar.appendChild(nama = new MyTextbox());
		nama.setWidth("80px");
		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		Toolbarbutton search;
		toolbar.appendChild(search = new ais.ui.util.MyToolbarbuttonConfig("", "/img/search.gif"));
		search.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});

		grid = new Grid();
		grid.setMold("paging");
		grid.setPageSize(25);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("15%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Jenis");
		column.setWidth("10%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
		paging.setParent(groupbox);

		loadData(null);
	}

	@SuppressWarnings("deprecation")
	private void init(final TarifKhususPunyaAlatMedis tarifKhususPunyaAlatMedis) throws Exception {
		this.tarifKhususPunyaAlatMedis = tarifKhususPunyaAlatMedis;
		final AlatMedis alatMedis = tarifKhususPunyaAlatMedis.getAlatMedis();
		final Window addWindow = new Window();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Tarif " + jenis);
		addWindow.setWidth("95%");
		addWindow.setHeight("90%");

		Borderlayout borderlayout = new Borderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		East east = new East();
		east.setWidth("150px");
		east.setTitle("Untuk Layanan");
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);

		Checkbox alatMedisLab;
		Checkbox alatMedisOperasi;
		Checkbox alatMedisRadiologi;
		Checkbox alatMedisVk;
		Checkbox alatMedisRenalUnit;
		Checkbox alatMedisGizi;
		east.appendChild(new Vbox(new Component[] { alatMedisLab = new Checkbox("Laboratorium"),
				alatMedisOperasi = new Checkbox("Operasi"), alatMedisRadiologi = new Checkbox("Radiologi"),
				alatMedisVk = new Checkbox("Vk"), alatMedisRenalUnit = new Checkbox("Renal Unit"),
				alatMedisGizi = new Checkbox("Gizi") }));

		alatMedisLab.setChecked(alatMedis.getAlatMedisLab());
		alatMedisOperasi.setChecked(alatMedis.getAlatMedisOperasi());
		alatMedisRadiologi.setChecked(alatMedis.getAlatMedisRadiologi());
		alatMedisVk.setChecked(alatMedis.getAlatMedisVk());
		alatMedisRenalUnit.setChecked(alatMedis.getAlatMedisRenalUnit());
		alatMedisGizi.setChecked(alatMedis.getAlatMedisGizi());

		alatMedisLab.setDisabled(true);
		alatMedisOperasi.setDisabled(true);
		alatMedisRadiologi.setDisabled(true);
		alatMedisVk.setDisabled(true);
		alatMedisRenalUnit.setDisabled(true);
		alatMedisGizi.setDisabled(true);

		Grid grid = new Grid();
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama ")));
		row.appendChild(new Label(alatMedis.getNama()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode ")));
		row.appendChild(new Label(alatMedis.getKode()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis ")));
		row.appendChild(
				new Label(alatMedis.getJenisAlatMedis() == null ? "" : alatMedis.getJenisAlatMedis().toString()));

		row = new Row();
		ais.ui.util.ZkCompat.setSpans(row, "1,1,1,3");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Aktif")));
		row.appendChild(new Label(alatMedis.getAktif() ? "Ya" : "Tidak"));

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(new Label(alatMedis.getKeterangan()));

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setStyle("border:0px;background: transparent;");
		tabbox.setHeight("240px");
		tabbox.setWidth("100%");
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setStyle("border:0px;background: transparent;");
		tabs.setParent(tabbox);

		final Tab tabPenjualan = new Tab("Harga");
		tabPenjualan.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setStyle("border:0px;background: transparent;");
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		initHarga.initHargaJual(alatMedis, tabpanel, this, tarifKhususPunyaAlatMedis);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	public boolean onSave(Event event) throws Exception {

		Session session = HibernateUtil.currentSession();

		tarifKhususPunyaAlatMedis.setSemuahargasama(initHarga.semuahargasama.isChecked());
		session.update(tarifKhususPunyaAlatMedis);

		initHarga.saveDetail(tarifKhususPunyaAlatMedis.getAlatMedis(), tarifKhususPunyaAlatMedis);

		return true;
	}

}
