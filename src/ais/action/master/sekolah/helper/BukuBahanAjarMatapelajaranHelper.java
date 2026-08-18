package ais.action.master.sekolah.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.BukuBahanAjarAction;
import ais.action.master.helper.FileBukuAjarHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataBukuBahanAjarBanyak;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.MatapelajaranPunyaBukuBahanAjar;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class BukuBahanAjarMatapelajaranHelper implements DataLoader {

	private MyGrid grid;
	private Matapelajaran matapelajaran;
	private Component component;
	private String sqltambahan = "false";

	public BukuBahanAjarMatapelajaranHelper() {

	}

	class DetailMatapelajaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final MatapelajaranPunyaBukuBahanAjar matapelajaranPunyaBukuBahanAjar = (MatapelajaranPunyaBukuBahanAjar) data;

			final BukuBahanAjar bukuBahanAjar = matapelajaranPunyaBukuBahanAjar.getBukuBahanAjar();

			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						FileBukuAjarHelper fileBukuAjarHelper = new FileBukuAjarHelper(
								Common.getCurrentUser().getMahasiswa() == null);

						fileBukuAjarHelper.display(bukuBahanAjar, detail);
					}

				}
			});

			Vbox vbox = new Vbox();
			vbox.setParent(row);

			RevisiHelper.createNewRevisi(BukuBahanAjar.class, bukuBahanAjar, bukuBahanAjar.getNama()).setParent(vbox);

			Vbox myvbox = new Vbox();
			myvbox.setParent(vbox);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, bukuBahanAjar.getId(), LampiranLain.BUKU, LampiranLain.BUKU, true,
					null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(vbox);

			hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, bukuBahanAjar.getId(), LampiranLain.COVER_BUKU, "Cover", true,
					null, null, false, false, false, false);

			if (bukuBahanAjar.getPengarangAdalahDosen()) {
				BukuBahanAjarAction.tampilkanInfoDosen(bukuBahanAjar, false).setParent(row);
			} else {
				new Label((bukuBahanAjar.getPengarang1() != null && !bukuBahanAjar.getPengarang1().trim().equals("")
						? bukuBahanAjar.getPengarang1().trim() + ", " : "")
						+ (bukuBahanAjar.getPengarang2() != null && !bukuBahanAjar.getPengarang2().trim().equals("")
								? bukuBahanAjar.getPengarang2().trim() + ", " : "")
						+ (bukuBahanAjar.getPengarang3() != null && !bukuBahanAjar.getPengarang3().trim().equals("")
								? bukuBahanAjar.getPengarang3().trim() + ", " : "")).setParent(row);
			}

			new Label(bukuBahanAjar.getIsbn()).setParent(row);
			new Label(bukuBahanAjar.getPenerbit()).setParent(row);
			new Label(bukuBahanAjar.getLink()).setParent(row);
			new Label(bukuBahanAjar.getKeterangan()).setParent(row);
			new Label(bukuBahanAjar.getTahun() == null ? "" : bukuBahanAjar.getTahun() + "").setParent(row);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/eye-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Kutipan Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					BukuBahanAjarAction.tampilkanKutipan(bukuBahanAjar);
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(Common.getCurrentUser().getMahasiswa() == null);
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
											Common.refreshDelete(matapelajaranPunyaBukuBahanAjar); 

											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
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
		List<MatapelajaranPunyaBukuBahanAjar> matapelajaranPunyaBukuBahanAjar = session
				.createCriteria(MatapelajaranPunyaBukuBahanAjar.class).addOrder(Order.asc("id")).add(matapelajaran == null
						? Restrictions.sqlRestriction(sqltambahan) : Restrictions.eq("matapelajaran", matapelajaran))
				.list();

		if (component instanceof Tabpanel) {
			((Tabpanel) component).getLinkedTab()
					.setLabel("Buku Diktat / Ajar " + (matapelajaranPunyaBukuBahanAjar.size() == 0 ? ""
							: "(" + matapelajaranPunyaBukuBahanAjar.size() + ")"));
		}

		ListModel strset = new SimpleListModel(matapelajaranPunyaBukuBahanAjar);
		grid.setRowRenderer(new DetailMatapelajaranRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display(final String sqltambahan, final Component component) {
		this.sqltambahan = sqltambahan;
		display(matapelajaran, component, null);
	}

	public void display(final Matapelajaran matapelajaran, final Component component, final JadwalPelajaran jadwalPelajaran) {
		this.matapelajaran = matapelajaran;
		Common.clear(component);
		this.component = component;

		if (component instanceof Tabpanel) {

			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(component);
			groupbox.appendChild(new MyCaptionStyled("Daftar buku ajar"));
			Tbmuser tbmuser = Common.getCurrentUser();
			Toolbar toolbar = new Toolbar();
			toolbar.setVisible(
					tbmuser != null && (matapelajaran != null || jadwalPelajaran != null) && tbmuser.getMahasiswa() == null);
			// toolbar.setHeight("25px");
			toolbar.setParent(groupbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Buku Ajar", "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<BukuBahanAjar> bukuBahanAjars = HibernateUtil.currentSession()
							.createCriteria(MatapelajaranPunyaBukuBahanAjar.class)
							.add(Restrictions.eq("matapelajaran", matapelajaran))
							.setProjection(Projections.property("bukuBahanAjar")).list();

					AmbilDataBukuBahanAjarBanyak window = new AmbilDataBukuBahanAjarBanyak(bukuBahanAjars);

					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
					window.setWidth("870px");
					window.setHeight("90%");

					window.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<BukuBahanAjar> bukuBahanAjars = (List<BukuBahanAjar>) arg0.getData();

							if (bukuBahanAjars != null) {
								Session session = HibernateUtil.currentSession();
								for (BukuBahanAjar bukuBahanAjar : bukuBahanAjars) {

									MatapelajaranPunyaBukuBahanAjar matapelajaranPunyaBukuBahanAjar = new MatapelajaranPunyaBukuBahanAjar();
									matapelajaranPunyaBukuBahanAjar.setBukuBahanAjar(bukuBahanAjar);
									matapelajaranPunyaBukuBahanAjar.setMatapelajaran(matapelajaran);

									session.save(matapelajaranPunyaBukuBahanAjar);

									if (jadwalPelajaran != null) {
										CommonEmail.infoAdaBukuAjar(jadwalPelajaran, matapelajaranPunyaBukuBahanAjar);
									}
								}

								loadData(null);

							}

						}
					});

					window.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Tambah Buku Ajar", "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					BukuBahanAjarAction.onAddExternal(event, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							BukuBahanAjar bukuBahanAjar = (BukuBahanAjar) arg0.getData();

							if (bukuBahanAjar != null) {
								Session session = HibernateUtil.currentSession();
								MatapelajaranPunyaBukuBahanAjar matapelajaranPunyaBukuBahanAjar = new MatapelajaranPunyaBukuBahanAjar();
								matapelajaranPunyaBukuBahanAjar.setBukuBahanAjar(bukuBahanAjar);
								matapelajaranPunyaBukuBahanAjar.setMatapelajaran(matapelajaran);

								session.save(matapelajaranPunyaBukuBahanAjar);
								if (jadwalPelajaran != null) {
									CommonEmail.infoAdaBukuAjar(jadwalPelajaran, matapelajaranPunyaBukuBahanAjar);
								}

								loadData(null);
							}
						}
					}, new BukuBahanAjar());
				}

			});
			button.setParent(toolbar);

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);grid.getPagingChild().setMold("os");
			grid.setParent(groupbox);

		} else {
			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);grid.getPagingChild().setMold("os");
			grid.setParent(component);
		}

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengarang");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("ISBN");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penerbit");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Link");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

}
