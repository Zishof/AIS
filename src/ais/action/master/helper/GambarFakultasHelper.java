package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.sql.Blob;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.image.AImage;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.DaoFactory;
import ais.database.dao.GambarFakultasDao;
import ais.database.model.Fakultas;
import ais.database.model.file.GambarFakultas;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class GambarFakultasHelper implements DataLoader {

	private MyGrid grid;
	private Fakultas fakultas;

	class GambarFakultasRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");

			final GambarFakultas gambarFakultas = (GambarFakultas) data;

			if (gambarFakultas == null)
				return;
			AImage aImage = new AImage(gambarFakultas.ambilFile());

			Image image = new Image();
			image.setContent(aImage);
			image.setHeight("150px");
			image.setWidth("140px");
			image.setParent(row);

			// if (gambarFakultas.getGambarUtama() != null) {
			// aImage = new AImage("", gambarFakultas.getGambarUtama()
			// .// getBinaryStream());
			//
			// image = new Image();
			// image.setContent(aImage);
			// image.setHeight("150px");
			// image.setWidth("140px");
			// image.setParent(row);
			// }

			// else {
			new Label("").setParent(row);
			//
			// }

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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

											GambarFakultasDao gambarFakultasDao = DaoFactory.getInstance()
													.getGambarFakultasDao();
											// gambarFakultasDao.beginTransaction();
											gambarFakultasDao.delete((gambarFakultas));
											// gambarFakultasDao.commitTransaction();

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
		Session session = Common.getManualSession();

		List<GambarFakultas> gambarFakultas = session.createCriteria(GambarFakultas.class)
				.add(Restrictions.eq("fakultas", fakultas)).list();

		ListModel strset = new SimpleListModel(gambarFakultas);
		grid.setRowRenderer(new GambarFakultasRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void displayGambarFakultas(final Fakultas fakultas, final Component component, final MyWindow window) {
		this.fakultas = fakultas;
		Common.clear(component);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("300px");
		panel.setTitle("Gambar " + "Fakultas");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
				"Upload gambar fakultas" + Common.ukuranLabelFileUpload(), "/img/new.gif");
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				UploadEvent uploadEvent = (UploadEvent) event;
				Session session = Common.getManualSession();
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (!(media instanceof org.zkoss.image.AImage)) {
					MyMessageboxConfig.show("Maaf, yang anda upload harus berupa gambar");
					return;
				}
				GambarFakultasDao gambarFakultasDao = DaoFactory.getInstance().getGambarFakultasDao();
				gambarFakultasDao.getCurrentSession()
						.createSQLQuery(
								"update gambar_fakultas set gambar_header=null where fakultas=" + fakultas.getId())
						.executeUpdate();

				Blob blob = Common.getBlobFromMedia(media, session);

				GambarFakultas gambarFakultas = (GambarFakultas) session.createCriteria(GambarFakultas.class)
						.add(Restrictions.eq("fakultas", fakultas)).uniqueResult();
				if (gambarFakultas == null) {
					gambarFakultas = new GambarFakultas();
				}
				gambarFakultas.setGambarHeader(blob);

				gambarFakultas.setKeterangan(media.getName());
				gambarFakultas.setFakultas(fakultas);
				session.save(gambarFakultas);

				loadGambarFakultas();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Upload gambar utama", "/img/new.gif");
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				UploadEvent uploadEvent = (UploadEvent) event;

				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (!(media instanceof org.zkoss.image.AImage)) {
					MyMessageboxConfig.show("Maaf, yang anda upload harus berupa gambar");
					return;
				}
				Session session = Common.getManualSession();
				GambarFakultasDao gambarFakultasDao = DaoFactory.getInstance().getGambarFakultasDao();
				gambarFakultasDao.getCurrentSession()
						.createSQLQuery(
								"update gambar_fakultas set gambar_utama=null where fakultas=" + fakultas.getId())
						.executeUpdate();

				Blob blob = Common.getBlobFromMedia(media, session);

				GambarFakultas gambarFakultas = (GambarFakultas) session.createCriteria(GambarFakultas.class)
						.add(Restrictions.eq("fakultas", fakultas)).uniqueResult();
				if (gambarFakultas == null) {
					gambarFakultas = new GambarFakultas();
				}
				gambarFakultas.setFoto(blob);
				session.save(gambarFakultas);

				loadGambarFakultas();

			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Gambar Header");
		column.setWidth("45%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Gambar Utama");
		column.setWidth("45%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);
		// borderlayout.setParent(component);

	}

	@SuppressWarnings("unchecked")
	private void loadGambarFakultas() {
		Session session = Common.getManualSession();
		List<GambarFakultas> gambarFakultas = session.createCriteria(GambarFakultas.class)
				.add(Restrictions.eq("fakultas", fakultas)).setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(gambarFakultas);
		grid.setRowRenderer(new GambarFakultasRenderer());
		grid.setModelCheckMobile(strset);

	}

}
