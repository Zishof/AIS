package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.generic.AmbilDataBiodataCalonMahasiswaBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.PesertaUjian;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class PesertaUjianAction extends MyDiv {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private Pertemuan pertemuan;
	private MyGrid grid;

	private boolean edit = false;
	private boolean add = false;
	private boolean delete = false;

	public PesertaUjianAction(Pertemuan pertemuan) {
		super();
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		this.pertemuan = pertemuan;
		display();
	}

	class PesertaUjianRenderer extends ais.ui.util.MyRowRenderer {

		public PesertaUjianRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final PesertaUjian pesertaUjian = (PesertaUjian) data;

			CommonMedia.tampilkanGambarKecil(pesertaUjian.getBiodataCalonMahasiswa()).setParent(row);

			new Label(pesertaUjian.getBiodataCalonMahasiswa() == null ? ""
					: pesertaUjian.getBiodataCalonMahasiswa().getNoRegistrasi()).setParent(row);

			RevisiHelper
					.createNewRevisi(PesertaUjian.class, pesertaUjian, pesertaUjian.getBiodataCalonMahasiswa() == null
							? "" : pesertaUjian.getBiodataCalonMahasiswa().getNama())
					.setParent(row);

			final MyTextbox keterangan = new MyTextbox(
					pesertaUjian.getKeterangan() == null ? "" : pesertaUjian.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setHeight("95%");
			keterangan.setDisabled(!edit);
			keterangan.setParent(row);
			keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					pesertaUjian.setKeterangan(keterangan.getValue());
					Common.refreshUpdate(session, (pesertaUjian));
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							int i = Integer.parseInt(event.getData().toString());
							if (i == MyMessageboxConfig.OK) {
								try {

									Common.refreshDelete(pesertaUjian);

									loadData(null);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException(
											"menghapus data peserta ujian",
											e,
											new String[] {
													"Periksa apakah data peserta ujian ini masih berelasi dengan data lain (misalnya data jawaban atau nilai ujian) sehingga tidak dapat dihapus.",
													"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
													"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
								}

							}

						}
					});

				}

			});
			button.setParent(toolbar);
			toolbar.setParent(row);

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<PesertaUjian> pesertaUjians = session.createCriteria(PesertaUjian.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("pertemuan", pertemuan)).list();

		ListModel strset = new SimpleListModel(pesertaUjians);
		grid.setRowRenderer(new PesertaUjianRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(new MyCaptionStyled("Daftar Peserta " + pertemuan.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Calon Mahasiswa", "/img/add_item.png");
		button.setDisabled(!add);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				Session session = HibernateUtil.currentSession();

				List<BiodataCalonMahasiswa> biodataCalonMahasiswas = session.createCriteria(PesertaUjian.class)
						.setProjection(Projections.groupProperty("biodataCalonMahasiswa"))
						.add(Restrictions.isNotNull("biodataCalonMahasiswa"))
						.add(Restrictions.eq("pertemuan", pertemuan)).list();

				AmbilDataBiodataCalonMahasiswaBanyak ambilDataBiodataCalonMahasiswaBanyak = new AmbilDataBiodataCalonMahasiswaBanyak(
						biodataCalonMahasiswas);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
						.appendChild(ambilDataBiodataCalonMahasiswaBanyak);
				ambilDataBiodataCalonMahasiswaBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<BiodataCalonMahasiswa> biodataCalonMahasiswas = (List<BiodataCalonMahasiswa>) arg0
								.getData();
						for (BiodataCalonMahasiswa biodataCalonMahasiswa : biodataCalonMahasiswas) {
							PesertaUjian pesertaUjian = new PesertaUjian();
							pesertaUjian.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
							pesertaUjian.setKeterangan("");
							pesertaUjian.setPertemuan(pertemuan);
							Common.refreshSaveOrUpdate(pesertaUjian);
						}

						loadData(null);
					}
				});
				ambilDataBiodataCalonMahasiswaBanyak.setWidth("950px");
				ambilDataBiodataCalonMahasiswaBanyak.setHeight("97%");
				ambilDataBiodataCalonMahasiswaBanyak.setVisible(true);
				ambilDataBiodataCalonMahasiswaBanyak.onModal();
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No Re.");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Peserta");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

}
