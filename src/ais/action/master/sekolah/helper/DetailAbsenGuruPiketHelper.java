package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Statusabsensi;
import ais.database.model.sekolah.AbsenGuruPiket;
import ais.database.model.sekolah.Guru;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailAbsenGuruPiketHelper implements DataLoader, DataCriteria {

	private MyGrid grid;

	private Textbox nama;

	private Paging paging;
	private AbsenGuruPiket absenGuruPiket;

	private List<Guru> guru = null;

	public DetailAbsenGuruPiketHelper() {

		paging = new Paging();
		Common.initPaging100(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
	}

	class DetailPARenderer extends ais.ui.util.MyRowRenderer {

		public DetailPARenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final Guru guru = (Guru) data;

			CommonMedia.tampilkanGambarKecil(guru).setParent(row);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(Guru.class, guru, guru.getNama())).setParent(row);
			a.appendChild(new Label(guru.getKode()));
			a.appendChild(new Label(guru.getNip()));

			String ket = absenGuruPiket.retreiveAbsensiKeterangan(guru.getId() + "");
			ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

			Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
					absenGuruPiket.retreiveAbsensiId(guru.getId() + ""));
			if (statusabsensi == null) {
				statusabsensi = Common.bolehKonfigurasi("absen_piket_otomatis_belum") ? ConstantValues.BELUM_ABSEN : ConstantValues.MASUK;
				absenGuruPiket.populate(guru.getId() + "", statusabsensi, ket, "", "", "AbsenGuruPiket");
				Common.refreshUpdate(absenGuruPiket);
			}
			final Textbox keterangan = new Textbox(ket);
			final Radiogroup kehadiran = new Radiogroup();

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (kehadiran.getSelectedItem() == null) {
						return;
					}

					if (absenGuruPiket.getId() != null) {
						HibernateUtil.currentSession().refresh(absenGuruPiket);
					}

					Statusabsensi statusabsensi = (Statusabsensi) kehadiran.getSelectedItem().getAttribute("value");

					absenGuruPiket.populate(guru.getId() + "", statusabsensi, keterangan.getValue(), "", "",
							"AbsenGuruPiket");

					Common.refreshUpdate(absenGuruPiket);
				}
			};

			Vbox vbox = new Vbox();
			vbox.setWidth("95%");
			vbox.setParent(row);

			if (ConstantValues.listAbsenMahasiswa.isEmpty()) {
				ConstantValues.initKehadiran(HibernateUtil.currentSession());
			}

			Common.insertRadioItemsMyConfig(kehadiran, "nama", ConstantValues.listAbsenMahasiswa);
			Common.selectRadioItem(kehadiran, statusabsensi);

			kehadiran.addEventListener("onClick", eventListener);
			kehadiran.setParent(vbox);

			keterangan.setCols(50);
			keterangan.setRows(2);
			keterangan.setParent(vbox);
			keterangan.addEventListener("onChange", eventListener);
		}

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Guru.class).add(Restrictions.isNotNull("sekolah"))
				.add(Restrictions.eq("sekolah", absenGuruPiket.getSekolah())).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("namaGuru", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("kode", nama.getValue().trim(), MatchMode.ANYWHERE)));

		if (order) {
			criteria.addOrder(Order.asc("namaGuru")).addOrder(Order.desc("id"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging100(initCriteria(false), paging);
		guru = ConstantValues.simpleList(initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_100).setFirstResult(
				Common.ROWS_COUNT_ON_PAGE_100 * (paging == null ? 0 : paging.getActivePage())), Guru.class);

		ListModel strset = new SimpleListModel(guru);
		grid.setRowRenderer(new DetailPARenderer());
		grid.setModelCheckMobile(strset);

	}

	private String[] contents = new String[] { "kode", "nik", "namaGuru", "sekolah.nama", "yayasan.nama" };

	public void displayDetailPA(final AbsenGuruPiket absenGuruPiket, final Component component, final MyWindow window) {
		this.absenGuruPiket = absenGuruPiket;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Guru : ")));
		toolbar.appendChild(nama = new Textbox());
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});

		MyToolbarbuttonConfig masuk = new MyToolbarbuttonConfig("Semua hadir", "/img/svg/check2.svg");
		masuk.setParent(toolbar);
		masuk.setTooltiptext("Tutup");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin semua mahaguru masuk kelas ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											if (absenGuruPiket.getId() != null) {
												HibernateUtil.currentSession().refresh(absenGuruPiket);
											}

											for (Guru guru : guru) {
												Statusabsensi statusabsensi = ConstantValues.MASUK;
												String ket = absenGuruPiket
														.retreiveAbsensiKeterangan(guru.getId() + "");
												ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
												absenGuruPiket.populate(guru.getId() + "", statusabsensi, ket, "", "",
														"AbsenGuruPiket");
											}

											Common.refreshUpdate(absenGuruPiket);

											loadData(null);
										}
									});

								}

							}
						});

			}
		});

		masuk = new MyToolbarbuttonConfig("Reset", "/img/reply.png");
		masuk.setParent(toolbar);
		masuk.setTooltiptext("Reset");
		masuk.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin me-reset absen di kelas ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											if (absenGuruPiket.getId() != null) {
												HibernateUtil.currentSession().refresh(absenGuruPiket);
											}

											for (Guru guru : guru) {
												Statusabsensi statusabsensi = ConstantValues.BELUM_ABSEN;
												String ket = absenGuruPiket
														.retreiveAbsensiKeterangan(guru.getId() + "");
												ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");
												absenGuruPiket.populate(guru.getId() + "", statusabsensi, ket, "", "",
														"AbsenGuruPiket");
											}

											Common.refreshUpdate(absenGuruPiket);

											loadData(null);

										}
									});

								}

							}
						});

			}
		});

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Status");
		columnHeadersAdding.add("Keterangan");
		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				Guru guru = (Guru) objects[0];
				XSSFRow row = (XSSFRow) objects[2];

				String ket = absenGuruPiket.retreiveAbsensiKeterangan(guru.getId() + "");
				ket = org.apache.commons.lang3.StringUtils.replace(ket, "_", ",");

				Statusabsensi statusabsensi = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
						absenGuruPiket.retreiveAbsensiId(guru.getId() + ""));
				if (statusabsensi == null) {
					statusabsensi = ConstantValues.BELUM_ABSEN;
				}

				row.createCell(contents.length).setCellValue(statusabsensi.getNama());
				row.createCell(contents.length + 1).setCellValue(ket);
			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(Guru.class, this, "Download Kehadiran",
				"/img/print.png", columnHeadersAdding, dataAdding, true, null, "Kehadiran", contents);
		toolbar.appendChild(cetakToolbarbutton);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status Kehadiran");

		loadData(null);

	}

}
