package ais.action.master;

import java.sql.Blob;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PengumumanAkademis;
import ais.database.model.file.LampiranPengumumanAkademis;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AttachmentPengumumanAkademisAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8311826382193536728L;
	private PengumumanAkademis pengumumanAkademis;
	private MyGrid grids;

	private Boolean readonly = false;
	private MyWindow addWindowAttachment;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		init(new PengumumanAkademis());
	}

	public void init(final PengumumanAkademis pengumumanAkademis) {
		this.pengumumanAkademis = pengumumanAkademis;
		// Common.clear(component);
		addWindowAttachment.setTitle("Detail File Attachment");
		Common.clear(addWindowAttachment);
		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(addWindowAttachment);
		panel.setWidth("100%");
		panel.setHeight("100px");
		panel.setTitle("Daftar file terkait dengan Artikel ini");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setVisible(!readonly);
		toolbar.setParent(panel);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data" + Common.ukuranLabelFileUpload(),
				"/img/new.gif");
		button.setUpload(Common.ukuranFileUpload());
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				UploadEvent uploadEvent = (UploadEvent) event;
				Session session = Common.getManualSession();
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				Blob blob = Common.getBlobFromMedia(media, session);
				LampiranPengumumanAkademis lampiranPengumumanAkademis = new LampiranPengumumanAkademis();
				lampiranPengumumanAkademis.setFoto(blob);
				lampiranPengumumanAkademis.setMimeType(media.getContentType());
				lampiranPengumumanAkademis.setNama(media.getName());
				lampiranPengumumanAkademis.setPengumumanAkademis(pengumumanAkademis);
				lampiranPengumumanAkademis.setUploadDate(ais.ui.util.WaktuUtil.getDate());
				session.save(lampiranPengumumanAkademis);

				loadDataAttachment();

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

		grids = new MyGrid();
		grids.setMold("paging");
		grids.setPageSize(10);
		grids.setParent(center);

	}

	@SuppressWarnings("unchecked")
	public void loadDataAttachment() {
		Session session = HibernateUtil.currentSession();
		List<LampiranPengumumanAkademis> lampiranPengumumanAkademis = session
				.createCriteria(LampiranPengumumanAkademis.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("pengumumanAkademis", pengumumanAkademis)).setMaxResults(5).list();

		ListModel strset = new SimpleListModel(lampiranPengumumanAkademis);

		grids.setRowRenderer(new DetailLampiranPengumumanAkademisRenderer());
		grids.setModelCheckMobile(strset);
		grids.renderAll();
		grids.setOddRowSclass("non-odd");

	}

	class DetailLampiranPengumumanAkademisRenderer extends ais.ui.util.MyRowRenderer {

		public DetailLampiranPengumumanAkademisRenderer() {

		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LampiranPengumumanAkademis lampiranPengumumanAkademis = (LampiranPengumumanAkademis) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			vbox.setWidth("100%");
			CommonMedia.preview(lampiranPengumumanAkademis, vbox);

			new Label(Common.dateFormat.get().format(lampiranPengumumanAkademis.getUploadDate())).setParent(vbox);

			// Kolom aksi rapi: seluruh tombol dibungkus kebab popup (⋯) via UIHelper.buatBarisAksi.
			// Induk kebab adalah sel (vbox), bukan Row, karena sel ini juga memuat pratinjau + label.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(lampiranPengumumanAkademis.getNama(),
					lampiranPengumumanAkademis.iconDonwload());
			aksiButtons.add(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					LampiranPengumumanAkademis content = (LampiranPengumumanAkademis) HibernateUtil.currentSession()
							.createCriteria(LampiranPengumumanAkademis.class)
							.add(Restrictions.idEq(lampiranPengumumanAkademis.getId())).setMaxResults(1).uniqueResult();

					Filedownload.save(content.ambilFile(), content.getMimeType());

				}

			});

			toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			toolbarbutton.setTooltiptext("Hapus Data");
			toolbarbutton.setVisible(!readonly);
			aksiButtons.add(toolbarbutton);
			toolbarbutton.addEventListener("onClick", new EventListener() {
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
											
											Common.refreshDelete((lampiranPengumumanAkademis));

											loadDataAttachment();
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

			ais.ui.util.UIHelper.buatBarisAksi(vbox, 3, aksiButtons);
		}

	}

	public void setReadonly(Boolean readonly) {
		this.readonly = readonly;
	}

	public Boolean getReadonly() {
		return readonly;
	}

}
