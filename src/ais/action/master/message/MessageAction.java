package ais.action.master.message;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import ais.ui.util.MyCkEditor;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;

import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import org.zkoss.zul.Vbox;
import ais.ui.util.MyWindow;

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

public class MessageAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchjudul;
	private Textbox searchisi;
	private Textbox penerima;
	private Textbox judul;
	private MyCkEditor isi;

	private Message message;

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
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		if (timer != null) { timer.setRepeats(true); }
		timer.start();
	}

	class MessageRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Message message = (Message) arg1;

			// RevisiHelper.createNewRevisi(Message.class, message,
			// message.getJudul()).setParent(arg0);

			Tbmuser pengirim = message.getTbmuser();
			Mahasiswa mahasiswa = message.getMahasiswa();
			Dosen dosen = message.getDosen();
			Pegawai pegawai = message.getPegawai();

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
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/refresh-cw.svg");
			button.setTooltiptext("Ubah Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(message);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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

	public void onAdd(Event event) throws Exception {
		init(new Message());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void init(Message message) {
		this.message = message;
		addWindow.setTitle(message.getId() == null ? "Tambah Message" : "Ubah Message");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerima"));
		row.appendChild(penerima = new Textbox(message.getJudul()));
		penerima.setWidth("90%");
		penerima.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
		row.appendChild(judul = new Textbox(message.getJudul()));
		judul.setWidth("90%");
		judul.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(row);
		groupbox.appendChild(new MyCaptionStyled("Isi Pesan"));
		groupbox.appendChild(isi = new MyCkEditor());
		isi.setValue(message.getIsi());
		isi.setWidth("90%");
		isi.setHeight("250px");

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Kirim Pesan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (penerima.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Penerima harus diisi", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (judul.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Judul harus diisi", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		// if (message.getId() != null) {
		// message = messageDao.load(message.getId());
		//
		// }

		String[] penerimas = penerima.getValue().split(";");
		MessageDao messageDao = DaoFactory.getInstance().getMessageDao();
		Session session = messageDao.getCurrentSession();
		String gakKetemu = "";
		String terkirim = "";
		for (String penerima : penerimas) {

			Tbmuser myPenerima = (Tbmuser) session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.idEq(penerima.trim()))
					.uniqueResult();

			if (myPenerima != null) {
				Tbmuser tbmuser = Common.getCurrentUser();
				message = new Message();
				message.setJudul(judul.getValue());
				message.setIsi(isi.getValue());
				message.setTbmuser(tbmuser);
				message.setDosen(tbmuser.ambilDosen());
				message.setMahasiswa(tbmuser.getMahasiswa());
				message.setPegawai(tbmuser.ambilPegawai());

				message.setTbmuserf(myPenerima);
				message.setDosenf(myPenerima.getDosen());
				message.setMahasiswaf(myPenerima.getMahasiswa());
				message.setPegawaif(myPenerima.getPegawai());
				message.setAktif(true);

				if (message.getId() != null) {
					messageDao.update(message);
				} else {
					messageDao.save(message);
				}
				terkirim += "\"" + penerima + "\", ";
			} else {
				gakKetemu += "\"" + penerima + "\", ";
			}
		}

		if (!terkirim.equals("") && gakKetemu.equals("")) {
			MyMessageboxConfig.show("Pesan berhasil terkirim ke " + terkirim + "", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		} else if (!terkirim.equals("") && !gakKetemu.equals("")) {
			MyMessageboxConfig.show("Pesan belum terkitim ke " + terkirim + ". Namun tidak terkirim ke " + gakKetemu + "",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		} else {
			MyMessageboxConfig.show("Pesan tidak terkirim ke " + gakKetemu + " ", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Message.class);
		if (order)
			criteria.addOrder(Order.desc("id"));

		Criterion or = Restrictions.sqlRestriction("1!=1");
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.getMahasiswa() == null && tbmuser.getUserId() != null) {
			or = Restrictions.or(or, Restrictions.eq("tbmuserf", tbmuser));
		}

		if (tbmuser.getMahasiswa() != null) {
			or = Restrictions.or(or, Restrictions.eq("mahasiswaf", tbmuser.getMahasiswa()));
		}

		if (tbmuser.ambilDosen() != null) {
			or = Restrictions.or(or, Restrictions.eq("dosenf", tbmuser.ambilDosen()));
		}

		if (tbmuser.ambilPegawai() != null) {
			or = Restrictions.or(or, Restrictions.eq("pegawaif", tbmuser.ambilPegawai()));
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

}
