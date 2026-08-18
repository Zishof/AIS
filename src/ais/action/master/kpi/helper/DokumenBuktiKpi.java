package ais.action.master.kpi.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.LampiranLain;
import ais.database.model.kpi.NilaiKpi;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DokumenBuktiKpi extends MyWindow {

	private static final long serialVersionUID = 6452461056684904810L;
	private NilaiKpi nilaiKpi;

	public DokumenBuktiKpi(NilaiKpi nilaiKpi) {
		super();
		this.nilaiKpi = nilaiKpi;
		reload();
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void reload() {
		Common.clear(this);

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				DokumenBuktiKpi.this.detach();
			}
		});
		cancel.setParent(toolbar);

		Center center = new Center();
		center.setBorder("none");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setTitle("Dokumen - "+nilaiKpi.getItemKpi().getNama());

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setStyle("border:0px;background: transparent;");
		grid.setParent(center);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyToolbarbuttonConfig tambahDokumen;
		row.appendChild(tambahDokumen = new MyToolbarbuttonConfig("Tambah Dokumen", "/img/svg/addthis.svg"));
		tambahDokumen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final MyWindow addWindow = new MyWindow("Tambah Dokumen", "none", false);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.setHeight("300px");
				addWindow.setWidth("450px");

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				borderlayout.setParent(addWindow);
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("40%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rowsTambah = new Rows();
				rowsTambah.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rowsTambah);
				row.appendChild(new ais.ui.util.MyLabelConfig("Nama Dokumen"));
				String[] lampiran_Pegawai = Common
						.getKonfigurasi("lampiran_kpi", "Bukti Kehadiran;Bukti Prestasi;DOkumen Lain").getNilai()
						.split(";");

				final Combobox dokumen = new Combobox();
				for (String s : lampiran_Pegawai) {
					Comboitem comboitem = new Comboitem(s);
					dokumen.appendChild(comboitem);
				}
				row.appendChild(dokumen);
				dokumen.setWidth("95%");

				Common.initKeterangan(rowsTambah, "Ketikkan nama dokumen jika tidak tercantum dalam pilihan");

				final MyFormRow rowDokumen = new MyFormRow();
				rowDokumen.setVisible(false);
				rowDokumen.setValign("top");
				rowDokumen.setParent(rowsTambah);
				rowDokumen.appendChild(new ais.ui.util.MyLabelConfig("File Dokumen"));

				dokumen.addEventListener("onOK", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						rowDokumen.setVisible(!dokumen.getValue().trim().isEmpty());
					}
				});
				dokumen.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						rowDokumen.setVisible(!dokumen.getValue().trim().isEmpty());
					}
				});

				Hbox myHbox = new Hbox();
				myHbox.setParent(rowDokumen);
				myHbox.setHeight("30px");

				Hbox hboxGambar = new Hbox();
				hboxGambar.setParent(myHbox);

				LampiranLain.createDownloadUploadFileLain(hboxGambar, nilaiKpi.getId(),
						"Dokumen_NilaiKpi_" + Common.getGeneratedBarCode(), "Dokumen", false, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								LampiranLain lainMahasiswaCover = (LampiranLain) arg0.getData();

								Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
								streamingSession.refresh(lainMahasiswaCover);
								lainMahasiswaCover.setRef(nilaiKpi.getId());
								lainMahasiswaCover.setJenis("Dokumen_NilaiKpi_" + dokumen.getValue().trim());

								streamingSession.getTransaction().begin();
								streamingSession.update(lainMahasiswaCover);
								streamingSession.getTransaction().commit();
								StreamingHibernateUtil.getInstance().closeSession();

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										addWindow.detach();
										reload();
									}
								});
							}
						});

				South south = new South();
				ais.ui.util.ZkCompat.setFlex(south, true);
				south.setParent(borderlayout);

				Toolbar toolbar = new Toolbar();
				// toolbar.setHeight("25px");
				toolbar.setParent(south);
				MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
				cancel.setTooltiptext("Tutup");
				cancel.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						addWindow.detach();
					}
				});
				cancel.setParent(toolbar);

				addWindow.setVisible(true);
				addWindow.onModal();
			}
		});

		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
		List<LampiranLain> lampiranLains = streamingSession.createCriteria(LampiranLain.class).addOrder(Order.asc("id"))
				.add(Restrictions.eq("ref", nilaiKpi.getId()))
				.add(Restrictions.ilike("jenis", "Dokumen_NilaiKpi_", MatchMode.START)).list();
		StreamingHibernateUtil.getInstance().closeSession();
		String doksId = "";
		for (final LampiranLain lampiranLain : lampiranLains) {

			doksId += doksId.isEmpty() ? lampiranLain.getId().toString() : "," + lampiranLain.getId();

			row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(lampiranLain.getJenis().replaceAll("Dokumen_NilaiKpi_", "")));

			Hbox hbox = new Hbox();
			row.appendChild(hbox);
			hbox.appendChild(tambahDokumen = new MyToolbarbuttonConfig("Lihat Dokumen", "/img/svg/eye.svg"));
			tambahDokumen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.display(lampiranLain);
				}
			});

			MyToolbarbuttonConfig hapusDokumen;
			hbox.appendChild(hapusDokumen = new MyToolbarbuttonConfig("Hapus Dokumen", "/img/svg/trash.svg"));
			hapusDokumen.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus dokumen ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											try {
												Session session = StreamingHibernateUtil.getInstance().currentSession();

												session.getTransaction().begin();
												session.delete(lampiranLain);
												session.getTransaction().commit();

												StreamingHibernateUtil.getInstance().closeSession();
											} catch (Exception e) {
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												Common.tampilErrorJikaAdmin(e);
											}

											reload();
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
		}

	}

}
