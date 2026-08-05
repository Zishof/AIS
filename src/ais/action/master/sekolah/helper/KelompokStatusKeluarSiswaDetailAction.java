package ais.action.master.sekolah.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.generic.AmbilDataSiswaBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.KelompokStatusKeluarSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * <h3>Detail Kelompok Status Keluar Siswa (daftar anggota)</h3>
 *
 * <p>Panel expander untuk satu {@link KelompokStatusKeluarSiswa}. Menampilkan daftar siswa yang
 * tergabung dalam kelompok tersebut serta tombol untuk menambah siswa ke kelompok
 * ({@code Ambil Data Siswa}), mengubah tanggal lulus/keluar per siswa, mengeluarkan siswa dari
 * kelompok, dan menghapus seluruh anggota. Merupakan padanan
 * {@code KelompokStatusKeluarMahasiswaDetailAction}, namun disederhanakan sesuai atribut yang
 * relevan untuk Siswa (NIS, Nama, Kelas, Sekolah, Tanggal Lulus/Keluar).</p>
 */
public class KelompokStatusKeluarSiswaDetailAction extends MyDetail implements DataCriteria {

	private static final long serialVersionUID = 5086031585928643233L;

	private KelompokStatusKeluarSiswa kelompokStatusKeluarSiswa;
	private MyGrid grid;
	private Textbox pencarian;
	private List<Siswa> siswas = null;

	public KelompokStatusKeluarSiswaDetailAction(KelompokStatusKeluarSiswa kelompokStatusKeluarSiswa) {
		super();
		this.kelompokStatusKeluarSiswa = kelompokStatusKeluarSiswa;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(KelompokStatusKeluarSiswaDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class SiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			final Siswa siswa = (Siswa) data;

			CommonMedia.tampilkanGambarKecil(siswa).setParent(arg0);
			new Label(siswa.getNomorInduk()).setParent(arg0);
			new Label(siswa.getNama()).setParent(arg0);
			new Label(siswa.getKelas() == null ? "" : siswa.getKelas().getNama()).setParent(arg0);
			new Label(siswa.getSekolah() == null ? "" : siswa.getSekolah().getNama()).setParent(arg0);

			final MyDatebox tanggalLulus = new MyDatebox(siswa.getTanggalLulus());
			tanggalLulus.setWidth("95%");
			tanggalLulus.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					siswa.setTanggalLulus(tanggalLulus.getValue());
					Common.refreshUpdate(siswa);
				}
			});
			tanggalLulus.setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setVisible(siswa.getKelompokStatusKeluarSiswa() != null);
			button.setOrient("vertical");
			button.setTooltiptext("Keluarkan siswa dari kelompok ini");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mengeluarkan siswa ini dari kelompok ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											siswa.setKelompokStatusKeluarSiswa(null);
											Common.refreshSaveOrUpdate(siswa);
											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}
									}
								}
							});
				}
			});
			button.setParent(hbox);
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		siswas = ConstantValues.simpleList(initCriteria(true).setMaxResults(1500), Siswa.class);
		ListModel strset = new SimpleListModel(siswas);
		grid.setRowRenderer(new SiswaRenderer());
		grid.setModelCheckMobile(strset);
	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar siswa yang masuk kelompok " + kelompokStatusKeluarSiswa.getNama()));
		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Siswa", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataSiswaBanyak window = new AmbilDataSiswaBanyak(siswas);

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(final Event dataSiswa) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event arg0) throws Exception {
								List<Siswa> siswas = (List<Siswa>) dataSiswa.getData();

								if (siswas != null) {
									Session session = HibernateUtil.currentSession();
									for (Siswa siswa : siswas) {
										siswa.setKelompokStatusKeluarSiswa(kelompokStatusKeluarSiswa);
										Common.refreshUpdate(session, siswa);
									}

									loadData(null);
								}
							}
						});
					}
				});

				window.onModal();
			}
		});
		button.setParent(toolbar);

		pencarian = new Textbox();
		pencarian.setCols(8);
		pencarian.setParent(toolbar);
		pencarian.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		String[] contents = new String[] { "nomorInduk", "nama", "kelas.nama", "sekolah.nama", "tanggalLulus" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(Siswa.class, this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		button = new MyToolbarbuttonConfig("Hapus Semua", "/img/svg/trash.svg");
		button.setTooltiptext("Keluarkan semua siswa dari kelompok ini");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				MyMessageboxConfig.show("Apakah yakin ingin mengeluarkan semua siswa dari kelompok ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {
										List<Siswa> siswas = ConstantValues
												.simpleList(initCriteria(true).setMaxResults(5000), Siswa.class);
										for (Siswa siswa : siswas) {
											siswa.setKelompokStatusKeluarSiswa(null);
											Common.refreshSaveOrUpdate(siswa);
										}
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												loadData(null);
											}
										});
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(
												"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
														+ e.getMessage());
									}
								}
							}
						});
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIS");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kelas");
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sekolah");
		column.setWidth("18%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tgl.Lulus/Keluar");
		column.setWidth("14%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		Rows rows = new Rows();
		rows.setParent(grid);

		loadData(null);
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(Siswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(pencarian == null || pencarian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("nama", pencarian.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("nomorInduk", pencarian.getValue().trim(), MatchMode.ANYWHERE)))
				.addOrder(Order.desc("id"))
				.add(Restrictions.eq("kelompokStatusKeluarSiswa", kelompokStatusKeluarSiswa));
	}

}
