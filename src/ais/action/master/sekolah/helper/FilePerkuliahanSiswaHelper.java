package ais.action.master.sekolah.helper;

import java.sql.Blob;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GrupPertemuan;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.Pertemuan;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class FilePerkuliahanSiswaHelper {

	private Siswa siswa;
	private CalonSiswa calonSiswa;

	private MyGrid uploadGrid;
	private Pertemuan pertemuan;
	private Tabpanel tabpanelFilePertemuan;
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;
	private KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail;
	private GrupPertemuan grupPertemuan;

	public FilePerkuliahanSiswaHelper(final Siswa siswa, final CalonSiswa calonSiswa) {
		this.siswa = siswa;
		this.calonSiswa = calonSiswa;

	}

	class FileUploadRenderer extends ais.ui.util.MyRowRenderer {

		public FileUploadRenderer() {

		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub

			final PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) arg1;
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(pertemuanFileContent.getNama()).setParent(vbox);
			vbox.setWidth("100%");
			CommonMedia.preview(pertemuanFileContent, vbox);

			new Label(Common.dateFormat.get().format(pertemuanFileContent.getUploadDate())).setParent(arg0);
			new Label(pertemuanFileContent.getFileMimeType()).setParent(arg0);

			if (siswa == null) {
				final Textbox ketarangan = new Textbox(pertemuanFileContent.getKeterangan());
				ketarangan.setWidth("90%");
				ketarangan.setRows(2);
				ketarangan.setParent(arg0);
				ketarangan.setMaxlength(255);

				ketarangan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = StreamingHibernateUtil.getInstance().currentSession();
						session.createSQLQuery("update pertemuan_file_content set keterangan= :keterangan where id="
								+ pertemuanFileContent.getId()).setParameter("keterangan", ketarangan.getValue().trim())
								.executeUpdate();
						StreamingHibernateUtil.getInstance().closeSession();
					}
				});
			} else {
				new Label(pertemuanFileContent.getKeterangan()).setParent(arg0);
			}

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("", pertemuanFileContent.iconDonwload());
			toolbarbutton.setOrient("vertical");
			toolbarbutton.setParent(hbox);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					try {

						Session session = StreamingHibernateUtil.getInstance().currentSession();

						PertemuanFileContent mypertemuanFileContent = (PertemuanFileContent) session
								.createCriteria(PertemuanFileContent.class)
								.add(Restrictions.idEq(pertemuanFileContent.getId())).uniqueResult();
						Filedownload.save(mypertemuanFileContent.ambilFile(), mypertemuanFileContent.getFileMimeType());

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

			});

			toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			toolbarbutton.setVisible(siswa == null);
			toolbarbutton.setOrient("vertical");
			toolbarbutton.setTooltiptext("Hapus Data");
			toolbarbutton.setParent(hbox);
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
											Session session = StreamingHibernateUtil.getInstance().currentSession();

											session.getTransaction().begin();
											Common.refreshDelete((pertemuanFileContent));
											session.getTransaction().commit();

											reloadPertemuanFileContent();
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											StreamingHibernateUtil.getInstance().rollbackTransaction();
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

	public void createFile(final Pertemuan pertemuan, final GrupPertemuan grupPertemuan,
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahTemp,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final Tabpanel tabpanelFilePertemuan) {

		if (kurikulumPunyaMatakuliahDetail != null) {
			kurikulumPunyaMatakuliahTemp = kurikulumPunyaMatakuliahDetail.getKurikulumPunyaMatakuliah();
		}

		this.tabpanelFilePertemuan = tabpanelFilePertemuan;
		this.pertemuan = pertemuan;
		this.grupPertemuan = grupPertemuan;
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliahTemp;
		this.kurikulumPunyaMatakuliahDetail = kurikulumPunyaMatakuliahDetail;

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(tabpanelFilePertemuan);

		North north = new North();
		north.setParent(borderlayout);

		final Toolbar vbox = new Toolbar();
		vbox.setVisible(siswa == null && calonSiswa == null
				&& (pertemuan != null || kurikulumPunyaMatakuliahDetail != null || grupPertemuan != null));
		vbox.setParent(north);
		vbox.setWidth("100%");
		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig("Upload File" + Common.ukuranLabelFileUpload(),
				"/img/new.gif");
		mybutton.setUpload(Common.ukuranFileUpload());
		vbox.appendChild(mybutton);

		mybutton.addEventListener(Events.ON_UPLOAD, new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				try {

					Media media = ((UploadEvent) event).getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
					Blob blob = Common.getBlobFromMedia(media, session);
					pertemuanFileContent.setFoto(blob);
					pertemuanFileContent.setNama(media.getName());
					pertemuanFileContent.setFileMimeType(media.getContentType());
					pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(
							kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
									: kurikulumPunyaMatakuliahDetail.getId());
					pertemuanFileContent
							.setGrupPertemuan(grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
					pertemuanFileContent.setKurikulumPunyaMatakuliah(
							kurikulumPunyaMatakuliah == null ? -Common.randLong() : kurikulumPunyaMatakuliah.getId());
					pertemuanFileContent.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
					pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
					session.getTransaction().begin();
					session.save(pertemuanFileContent);
					session.getTransaction().commit();
					reloadPertemuanFileContent();
					StreamingHibernateUtil.getInstance().closeSession();

					CommonEmail.infoAdaFilePerkuliahan(pertemuan, pertemuanFileContent);
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}
		});

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		uploadGrid = new MyGrid();
		uploadGrid.setParent(center);
		uploadGrid.setWidth("100%");
		uploadGrid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(uploadGrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("File");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis File");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		reloadPertemuanFileContent();
	}

	@SuppressWarnings("unchecked")
	private void reloadPertemuanFileContent() {
		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			List<PertemuanFileContent> pertemuanFileContent = session.createCriteria(PertemuanFileContent.class)
					.addOrder(Order.desc("id"))

					.add(grupPertemuan != null ? Restrictions.eq("grupPertemuan", grupPertemuan.getId())
							: Restrictions.sqlRestriction("true"))

					.add(kurikulumPunyaMatakuliahDetail != null
							? Restrictions.eq("kurikulumPunyaMatakuliahDetail", kurikulumPunyaMatakuliahDetail.getId())
							: pertemuan == null ? Restrictions.sqlRestriction("true")
									: Restrictions.eq("pertemuan", pertemuan.getId()))

					.add(kurikulumPunyaMatakuliah == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("kurikulumPunyaMatakuliah", kurikulumPunyaMatakuliah.getId()))

					.list();

			tabpanelFilePertemuan.getLinkedTab().setLabel("File (" + pertemuanFileContent.size() + " file)");

			ListModel strset = new SimpleListModel(pertemuanFileContent);
			uploadGrid.setRowRenderer(new FileUploadRenderer());
			uploadGrid.setModelCheckMobile(strset);
			uploadGrid.renderAll();
			uploadGrid.setOddRowSclass("non-odd");
			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}
	}
}
