package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.BukuBahanAjarAction;
import ais.action.master.helper.generic.AmbilDataBukuBahanAjarBanyak;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BukuBahanAjar;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahPunyaBukuBahanAjar;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class BukuBahanAjarHelper implements DataLoader {

	private Grid grid;
	private Matakuliah matakuliah;
	private Component component;
	private String sqltambahan = "false";
	private Textbox cari;

	public BukuBahanAjarHelper() {

	}

	class DetailMatakuliahRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final MatakuliahPunyaBukuBahanAjar matakuliahPunyaBukuBahanAjar = (MatakuliahPunyaBukuBahanAjar) data;

			final BukuBahanAjar bukuBahanAjar = matakuliahPunyaBukuBahanAjar.getBukuBahanAjar();

			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (detail != null) {
						Common.clear(detail);
					}
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
			LampiranLain.createDownloadUploadFileLain(hbox, bukuBahanAjar.getId(), LampiranLain.BUKU, LampiranLain.BUKU,
					true, null, null, false, false, false, false);

			myvbox = new Vbox();
			myvbox.setParent(vbox);

			hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, bukuBahanAjar.getId(), LampiranLain.COVER_BUKU, "Cover",
					true, null, null, false, false, false, false);

			if (bukuBahanAjar.getPengarangAdalahDosen()) {
				BukuBahanAjarAction.tampilkanInfoDosen(bukuBahanAjar, false).setParent(row);
			} else {
				new Label((bukuBahanAjar.getPengarang1() != null && !bukuBahanAjar.getPengarang1().trim().equals("")
						? bukuBahanAjar.getPengarang1().trim() + ", "
						: "")
						+ (bukuBahanAjar.getPengarang2() != null && !bukuBahanAjar.getPengarang2().trim().equals("")
								? bukuBahanAjar.getPengarang2().trim() + ", "
								: "")
						+ (bukuBahanAjar.getPengarang3() != null && !bukuBahanAjar.getPengarang3().trim().equals("")
								? bukuBahanAjar.getPengarang3().trim() + ", "
								: "")).setParent(row);
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
											Common.refreshDelete(matakuliahPunyaBukuBahanAjar);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
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
		List<MatakuliahPunyaBukuBahanAjar> matakuliahPunyaBukuBahanAjar = session
				.createCriteria(MatakuliahPunyaBukuBahanAjar.class)

				.createAlias("bukuBahanAjar", "bukuBahanAjar")
				.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("bukuBahanAjar.nama", cari.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("bukuBahanAjar.penerbit", cari.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("bukuBahanAjar.abstrak", cari.getValue().trim(),
												MatchMode.ANYWHERE))))

				.addOrder(Order.asc("id")).add(matakuliah == null ? Restrictions.sqlRestriction(sqltambahan)
						: Restrictions.eq("matakuliah", matakuliah))
				.list();

		if (component instanceof Tabpanel) {
			((Tabpanel) component).getLinkedTab()
					.setLabel("Buku Diktat / Ajar " + (matakuliahPunyaBukuBahanAjar.size() == 0 ? ""
							: "(" + matakuliahPunyaBukuBahanAjar.size() + ")"));
		}

		ListModel strset = new SimpleListModel(matakuliahPunyaBukuBahanAjar);
		grid.setRowRenderer(new DetailMatakuliahRenderer());
		grid.setModel(strset);

	}

	public void display(final String sqltambahan, final Component component) {
		this.sqltambahan = sqltambahan;
		display(matakuliah, component, null);
	}

	public void display(final Matakuliah matakuliah, final Component component, final Perkuliahan perkuliahan) {
		this.matakuliah = matakuliah;
		if (component != null) {
			Common.clear(component);
		}
		this.component = component;
		cari = new Textbox();
		Tbmuser tbmuser = Common.getCurrentUser();
		if (component instanceof Tabpanel) {

			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(component);
			groupbox.appendChild(new MyCaptionStyled("Daftar buku ajar"));

			Toolbar toolbar = new Toolbar();

			// toolbar.setHeight("25px");
			toolbar.setParent(groupbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Buku Ajar", "/img/new.gif");
			button.setVisible(
					tbmuser != null && (matakuliah != null || perkuliahan != null) && tbmuser.getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<BukuBahanAjar> bukuBahanAjars = HibernateUtil.currentSession()
							.createCriteria(MatakuliahPunyaBukuBahanAjar.class)
							.add(Restrictions.eq("matakuliah", matakuliah))
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

									MatakuliahPunyaBukuBahanAjar matakuliahPunyaBukuBahanAjar = new MatakuliahPunyaBukuBahanAjar();
									matakuliahPunyaBukuBahanAjar.setBukuBahanAjar(bukuBahanAjar);
									matakuliahPunyaBukuBahanAjar.setMatakuliah(matakuliah);

									session.save(matakuliahPunyaBukuBahanAjar);

									if (perkuliahan != null) {
										CommonEmail.infoAdaBukuAjar(perkuliahan, matakuliahPunyaBukuBahanAjar);
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
			button.setVisible(
					tbmuser != null && (matakuliah != null || perkuliahan != null) && tbmuser.getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					BukuBahanAjarAction.onAddExternal(event, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							BukuBahanAjar bukuBahanAjar = (BukuBahanAjar) arg0.getData();

							if (bukuBahanAjar != null) {
								Session session = HibernateUtil.currentSession();
								MatakuliahPunyaBukuBahanAjar matakuliahPunyaBukuBahanAjar = new MatakuliahPunyaBukuBahanAjar();
								matakuliahPunyaBukuBahanAjar.setBukuBahanAjar(bukuBahanAjar);
								matakuliahPunyaBukuBahanAjar.setMatakuliah(matakuliah);

								session.save(matakuliahPunyaBukuBahanAjar);
								if (perkuliahan != null) {
									CommonEmail.infoAdaBukuAjar(perkuliahan, matakuliahPunyaBukuBahanAjar);
								}

								loadData(null);
							}
						}
					}, new BukuBahanAjar());
				}

			});
			button.setParent(toolbar);

			toolbar.appendChild(new Space());
			toolbar.appendChild(new MyLabelConfig("Cari : "));
			toolbar.appendChild(cari);
			cari.setCols(15);
			cari.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});
			button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});
			button.setParent(toolbar);

			grid = new Grid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);grid.getPagingChild().setMold("os");
			grid.setParent(groupbox);

		} else {
			grid = new Grid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);grid.getPagingChild().setMold("os");
			grid.setParent(component);

			if (component instanceof Center) {

				North north = new North();
				north.setParent(component.getParent());
				ais.ui.util.ZkCompat.setFlex(north, true);
				north.setHeight("25px");

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(north);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Buku Ajar", "/img/new.gif");
				button.setVisible(tbmuser != null && (matakuliah != null || perkuliahan != null)
						&& tbmuser.getMahasiswa() == null);
				button.addEventListener("onClick", new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {

						List<BukuBahanAjar> bukuBahanAjars = HibernateUtil.currentSession()
								.createCriteria(MatakuliahPunyaBukuBahanAjar.class)
								.add(Restrictions.eq("matakuliah", matakuliah))
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

										MatakuliahPunyaBukuBahanAjar matakuliahPunyaBukuBahanAjar = new MatakuliahPunyaBukuBahanAjar();
										matakuliahPunyaBukuBahanAjar.setBukuBahanAjar(bukuBahanAjar);
										matakuliahPunyaBukuBahanAjar.setMatakuliah(matakuliah);

										session.save(matakuliahPunyaBukuBahanAjar);

										if (perkuliahan != null) {
											CommonEmail.infoAdaBukuAjar(perkuliahan, matakuliahPunyaBukuBahanAjar);
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
				button.setVisible(tbmuser != null && (matakuliah != null || perkuliahan != null)
						&& tbmuser.getMahasiswa() == null);
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						BukuBahanAjarAction.onAddExternal(event, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								BukuBahanAjar bukuBahanAjar = (BukuBahanAjar) arg0.getData();

								if (bukuBahanAjar != null) {
									Session session = HibernateUtil.currentSession();
									MatakuliahPunyaBukuBahanAjar matakuliahPunyaBukuBahanAjar = new MatakuliahPunyaBukuBahanAjar();
									matakuliahPunyaBukuBahanAjar.setBukuBahanAjar(bukuBahanAjar);
									matakuliahPunyaBukuBahanAjar.setMatakuliah(matakuliah);

									session.save(matakuliahPunyaBukuBahanAjar);
									if (perkuliahan != null) {
										CommonEmail.infoAdaBukuAjar(perkuliahan, matakuliahPunyaBukuBahanAjar);
									}

									loadData(null);
								}
							}
						}, new BukuBahanAjar());
					}

				});
				button.setParent(toolbar);

				toolbar.appendChild(new Space());
				toolbar.appendChild(new MyLabelConfig("Cari : "));
				toolbar.appendChild(cari);
				cari.setCols(15);
				cari.addEventListener("onOK", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				});
				button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						loadData(null);
					}
				});
				button.setParent(toolbar);
			}
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
