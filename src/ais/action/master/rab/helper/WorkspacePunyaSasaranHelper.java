package ais.action.master.rab.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Sasaran;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaSasaran;

/**
 * Helper UI ZK untuk mengelola relasi banyak-ke-banyak antara satu {@link Workspace} RAB dan
 * sasaran-sasarannya ({@link WorkspacePunyaSasaran}/{@link Sasaran}): menampilkan grid sasaran
 * terkait, menambah sasaran lewat dialog pemilihan banyak ({@code AmbilDataSasaranBanyak}, yang
 * mengecualikan sasaran yang sudah terhubung), dan menghapus relasi per baris. Aksi tambah/hapus
 * tampil sesuai privilese {@link CommonPrivilages#CREATE}/{@link CommonPrivilages#DELETE} user.
 */
public class WorkspacePunyaSasaranHelper {

	private MyGrid gridSasaran;
	private boolean add = false;
	// private boolean edit = false;
	private boolean delete = false;

	/** Menyiapkan helper untuk {@code gridSasaran} yang diberikan; hak tambah/hapus ditentukan dari privilese user yang sedang login. */
	public WorkspacePunyaSasaranHelper(MyGrid gridSasaran) {
		this.gridSasaran = gridSasaran;
		add = CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE);
		// edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
	}

	/** Membangun kerangka layar detail sasaran workspace: toolbar tombol Tambah Sasaran (membuka dialog pemilihan banyak) dan grid dengan kolom Kode/Isi Sasaran/Hapus, lalu langsung memuat datanya. */
	public Borderlayout initDetail(final Workspace workspace) {
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(north);

		MyToolbarbuttonConfig add = new MyToolbarbuttonConfig("Tambah Sasaran", "/img/new.gif");
		add.setVisible(WorkspacePunyaSasaranHelper.this.add);
		add.setParent(toolbar);
		add.setTooltiptext("Tambah");
		add.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Sasaran> sasarans = new ArrayList<Sasaran>();
				List<Row> myrows = gridSasaran.getRows().getChildren();
				for (Row row : myrows) {
					sasarans.add(((WorkspacePunyaSasaran) row
							.getAttribute("workspacePunyaSasaran"))
							.getSasaran());
				}
				AmbilDataSasaranBanyak ambilDataSasaranBanyak = new AmbilDataSasaranBanyak(
						sasarans);
				ambilDataSasaranBanyak.setHeight("95%");
				ambilDataSasaranBanyak.setWidth("90%");
				ambilDataSasaranBanyak.setParent(ExecutionsCtrl
						.getCurrentCtrl().getCurrentPage().getFirstRoot());
				ambilDataSasaranBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Sasaran> sasarans = (List<Sasaran>) arg0.getData();
						for (Sasaran sasaran : sasarans) {
							WorkspacePunyaSasaran workspacePunyaSasaran = new WorkspacePunyaSasaran();
							workspacePunyaSasaran.setWorkspace(workspace);
							workspacePunyaSasaran.setSasaran(sasaran);

							Rows rows = gridSasaran.getRows() == null ? new Rows()
									: gridSasaran.getRows();
							rows.setParent(gridSasaran);
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							initRow(row, workspacePunyaSasaran);
						}
					}
				});

				ambilDataSasaranBanyak.onModal();

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridSasaran);
		gridSasaran.setParent(center);
		gridSasaran.setWidth("100%");
		gridSasaran.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridSasaran);

		MyColumnConfig column = new MyColumnConfig("Kode");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Isi Sasaran");
		column.setParent(columns);

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("10%");

		loadDataDetail(workspace);

		return borderlayout;
	}

	/** Memuat seluruh {@link WorkspacePunyaSasaran} milik {@code workspace} dan merender satu baris grid per sasaran. */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Workspace workspace) {

		List<WorkspacePunyaSasaran> workspacePunyaSasarans = workspace == null
				|| workspace.getId() == null ? new ArrayList<WorkspacePunyaSasaran>()
				: HibernateUtil.currentSession()
						.createCriteria(WorkspacePunyaSasaran.class)
						.add(Restrictions.eq("workspace", workspace)).list();

		Rows rows = gridSasaran.getRows() == null ? new Rows() : gridSasaran
				.getRows();
		rows.setParent(gridSasaran);

		for (WorkspacePunyaSasaran workspacePunyaSasaran : workspacePunyaSasarans) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);
			initRow(row, workspacePunyaSasaran);
		}
	}

	/** Merender satu baris grid untuk {@code workspacePunyaSasaran} (kode dan nama sasaran, tombol hapus dengan konfirmasi) ke {@code row}. */
	public void initRow(final Row row,
			final WorkspacePunyaSasaran workspacePunyaSasaran) {
		row.setValign("top");row.setAttribute("workspacePunyaSasaran", workspacePunyaSasaran);

		new Label(workspacePunyaSasaran.getSasaran() == null ? ""
				: workspacePunyaSasaran.getSasaran().getKode()).setParent(row);

		new Label(workspacePunyaSasaran.getSasaran() == null ? ""
				: workspacePunyaSasaran.getSasaran().getNama()).setParent(row);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete);
		button.setParent(hbox);

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									if (workspacePunyaSasaran.getId() != null) {
										Session session = HibernateUtil
												.currentSession();
										session.delete(workspacePunyaSasaran);
									}
	row.setVisible(false);row.detach();
								}

							}
						});

			}
		});
	}

}
