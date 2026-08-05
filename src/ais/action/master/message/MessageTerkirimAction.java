package ais.action.master.message;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import ais.ui.util.MyToolbarbuttonConfig;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.dao.DaoFactory;
import ais.database.dao.MessageDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Message;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;

public class MessageTerkirimAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchjudul;
	private Textbox searchisi;

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
		if (session.getAttribute("usersTemp") == null) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		Timer timer = new Timer(60000);
		if (timer != null) { timer.setParent(page.getFirstRoot()); }
		if (timer != null) { timer.setRepeats(true); }
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		timer.start();
	}

	class MessageRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Message message = (Message) arg1;

			// RevisiHelper.createNewRevisi(Message.class, message,
			// message.getJudul()).setParent(arg0);

			Tbmuser pengirim = message.getTbmuserf();
			Mahasiswa mahasiswa = message.getMahasiswaf();
			Dosen dosen = message.getDosenf();
			Pegawai pegawai = message.getPegawaif();

			if (mahasiswa != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);

				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);

				Vbox vbox = new Vbox();
				vbox.setParent(hbox);

				Label label = new Label(mahasiswa.getNim() + " (Mahasiswa)");
				label.setStyle("padding-top:10px;font-size:14px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);
				label = new Label(mahasiswa.getNama());
				label.setStyle("padding-top:10px;font-size:10px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);
				label = new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama());
				label.setStyle("padding-top:10px;font-size:10px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);
				label = new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getNama());
				label.setStyle("padding-top:10px;font-size:10px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);

				label = new Label(Common.dateFormat5.get().format(message.getWaktu()));
				label.setStyle("padding-top:10px;font-size:9px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:blue");
				label.setParent(vbox);

			} else if (dosen != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);

				CommonMedia.tampilkanGambarKecil(dosen).setParent(hbox);

				Vbox vbox = new Vbox();
				vbox.setParent(hbox);

				Label label = new Label(dosen.getNama() + " (Dosen)");
				label.setStyle("padding-top:10px;font-size:14px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);
				label = new Label(dosen.getJurusan() == null ? "" : dosen.getJurusan().getNama());
				label.setStyle("padding-top:10px;font-size:10px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);
				label = new Label(dosen.getFakultas() == null ? "" : dosen.getFakultas().getNama());
				label.setStyle("padding-top:10px;font-size:10px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);

				label = new Label(Common.dateFormat5.get().format(message.getWaktu()));
				label.setStyle("padding-top:10px;font-size:9px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:blue");
				label.setParent(vbox);

			} else if (pegawai != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);

				CommonMedia.tampilkanGambarKecil(pegawai).setParent(hbox);

				Vbox vbox = new Vbox();
				vbox.setParent(hbox);

				Label label = new Label(pegawai.getNama() + " (Pegawai)");
				label.setStyle("padding-top:10px;font-size:14px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);
				label = new Label(pegawai.getJurusan() == null ? "" : pegawai.getJurusan().getNama());
				label.setStyle("padding-top:10px;font-size:10px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);
				label = new Label(pegawai.getFakultas() == null ? "" : pegawai.getFakultas().getNama());
				label.setStyle("padding-top:10px;font-size:10px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);

				label = new Label(Common.dateFormat5.get().format(message.getWaktu()));
				label.setStyle("padding-top:10px;font-size:9px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:blue");
				label.setParent(vbox);

			} else {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);

				CommonMedia.tampilkanGambarKecil(pengirim).setParent(hbox);

				Vbox vbox = new Vbox();
				vbox.setParent(hbox);

				Label label = new Label(pengirim.getUserId() + " (" + pengirim.hakAkses() + ")");
				label.setStyle("padding-top:10px;font-size:10px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);
				label = new Label(pengirim.ambilJurusan() == null ? "" : pengirim.ambilJurusan().getNama());
				label.setStyle("padding-top:10px;font-size:10px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);
				label = new Label(pengirim.ambilFakultas() == null ? "" : pengirim.ambilFakultas().getNama());
				label.setStyle("padding-top:10px;font-size:10px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:black");
				label.setParent(vbox);

				label = new Label(Common.dateFormat5.get().format(message.getWaktu()));
				label.setStyle("padding-top:10px;font-size:9px;font-weight:"
						+ (message.getAktif() ? "bold" : "normal") + ";color:blue");
				label.setParent(vbox);

			}

			Label label = new Label(message.getJudul());
			label.setStyle("padding-top:10px;font-size:12px;font-weight:"
					+ (message.getAktif() ? "bold" : "normal") + ";color:blue");
			label.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											MessageDao messageDao = DaoFactory.getInstance().getMessageDao();
											// messageDao.beginTransaction();
											messageDao.delete(messageDao.merge(message));
											// messageDao.commitTransaction();
											onSearchDefault(event);
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
			toolbar.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Message.class);
		if (order)
			criteria.addOrder(Order.desc("id"));

		Criterion or = Restrictions.sqlRestriction("1!=1");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.getMahasiswa() == null && tbmuser.getUserId() != null) {
			or = Restrictions.or(or, Restrictions.eq("tbmuser", tbmuser));
		}

		if (tbmuser.getMahasiswa() != null) {
			or = Restrictions.or(or, Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()));
		}

		if (tbmuser.ambilDosen() != null) {
			or = Restrictions.or(or, Restrictions.eq("dosen", tbmuser.ambilDosen()));
		}

		if (tbmuser.ambilPegawai() != null) {
			or = Restrictions.or(or, Restrictions.eq("pegawai", tbmuser.ambilPegawai()));
		}

		criteria.add(Restrictions.ilike("judul", searchjudul.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("isi", searchisi.getValue(), MatchMode.ANYWHERE)).add(or);

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Message> message = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(message);
		grid.setRowRenderer(new MessageRenderer());
		grid.setModelCheckMobile(strset);

	}

	// public Boolean checkJudulMessage() {
	//
	// Integer kotaCount = null;
	// Session session = HibernateUtil.currentSession();
	// kotaCount = ((Number) session
	// .createCriteria(Message.class)
	// .setProjection(Projections.rowCount())
	// .add(Restrictions.eq("judul", judul.getValue().trim()))
	// .add(this.message.getId() == null ? Restrictions
	// .sqlRestriction("1=1") : Restrictions.ne("id",
	// this.message.getId())).uniqueResult()).intValue();
	//
	// return !kotaCount.equals(0);
	// }

}
