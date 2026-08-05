package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.KomponenPenilaianProposalSkripsi;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KomponenPenilaianProposalSkripsiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi;
	private MyToolbarbuttonConfig add;
	private MyIntbox nomorUrut;
	private Combobox parent;
	private MyDoublebox bobot;
	private Combobox fakultas;
	private Combobox jurusan;

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

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(KomponenPenilaianProposalSkripsi.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		if (count == 0) {

			KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi = new KomponenPenilaianProposalSkripsi(1);
			komponenPenilaianProposalSkripsi.setNama("Presentasi");
			komponenPenilaianProposalSkripsi
					.setKeterangan("Komunikatif, ketepatan waktu, kejelasan dan kerunutan dalam penyampaian materi.");
			session.save(komponenPenilaianProposalSkripsi);

			komponenPenilaianProposalSkripsi = new KomponenPenilaianProposalSkripsi(2);
			komponenPenilaianProposalSkripsi.setNama("Alat bantu presentasi");

			komponenPenilaianProposalSkripsi.setKeterangan("Kejelasan,dan tampilan dari power point.");
			session.save(komponenPenilaianProposalSkripsi);

			komponenPenilaianProposalSkripsi = new KomponenPenilaianProposalSkripsi(3);
			komponenPenilaianProposalSkripsi.setNama("Penampilan");
			komponenPenilaianProposalSkripsi.setKeterangan("Cara berpakaian, etika dan sopan santun.");
			session.save(komponenPenilaianProposalSkripsi);

			komponenPenilaianProposalSkripsi = new KomponenPenilaianProposalSkripsi(4);
			komponenPenilaianProposalSkripsi.setNama("Penguasaan Materi");
			komponenPenilaianProposalSkripsi.setKeterangan("Cara merespons pertanyaan dan kualitas jawaban.");
			session.save(komponenPenilaianProposalSkripsi);

			komponenPenilaianProposalSkripsi = new KomponenPenilaianProposalSkripsi(5);
			komponenPenilaianProposalSkripsi.setNama("Hasil penelitian");
			komponenPenilaianProposalSkripsi.setKeterangan("Hasil, pembahasan , kesimpulan, pustaka, lampiran.");
			session.save(komponenPenilaianProposalSkripsi);

		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "nama", "keterangan", "nomorUrut", "bobot", "parent", "fakultas",
				"jurusan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KomponenPenilaianProposalSkripsi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KomponenPenilaianProposalSkripsiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi = (KomponenPenilaianProposalSkripsi) arg1;

			new Label(komponenPenilaianProposalSkripsi.getNomorUrut().toString()).setParent(arg0);

			RevisiHelper.createNewRevisi(KomponenPenilaianProposalSkripsi.class, komponenPenilaianProposalSkripsi,
					komponenPenilaianProposalSkripsi.getNama()).setParent(arg0);

			new Label(komponenPenilaianProposalSkripsi.getFakultas() == null ? "Semua"
					: komponenPenilaianProposalSkripsi.getFakultas().getNama()).setParent(arg0);
			new Label(komponenPenilaianProposalSkripsi.getJurusan() == null ? "Semua"
					: komponenPenilaianProposalSkripsi.getJurusan().getNama()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(komponenPenilaianProposalSkripsi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianProposalSkripsi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianProposalSkripsi);
				}
			});
			new Label(komponenPenilaianProposalSkripsi.getParent() == null ? ""
					: komponenPenilaianProposalSkripsi.getParent().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(komponenPenilaianProposalSkripsi.getBobot())).setParent(arg0);
			new Label(komponenPenilaianProposalSkripsi.getKeterangan()).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			final MyCheckboxConfig dosen1 = new MyCheckboxConfig("Dsn1");
			dosen1.setChecked(komponenPenilaianProposalSkripsi.getDosen1());
			dosen1.setParent(hbox);
			dosen1.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianProposalSkripsi.setDosen1(dosen1.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianProposalSkripsi);
				}
			});

			final MyCheckboxConfig dosen2 = new MyCheckboxConfig("Dsn2");
			dosen2.setChecked(komponenPenilaianProposalSkripsi.getDosen2());
			dosen2.setParent(hbox);
			dosen2.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianProposalSkripsi.setDosen2(dosen2.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianProposalSkripsi);
				}
			});
			final MyCheckboxConfig dosen3 = new MyCheckboxConfig("Dsn3");
			dosen3.setChecked(komponenPenilaianProposalSkripsi.getDosen3());
			dosen3.setParent(hbox);
			dosen3.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianProposalSkripsi.setDosen3(dosen3.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianProposalSkripsi);
				}
			});
			final MyCheckboxConfig dosen4 = new MyCheckboxConfig("Dsn4");
			dosen4.setChecked(komponenPenilaianProposalSkripsi.getDosen4());
			dosen4.setParent(hbox);
			dosen4.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianProposalSkripsi.setDosen4(dosen4.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianProposalSkripsi);
				}
			});
			final MyCheckboxConfig dosen5 = new MyCheckboxConfig("Dsn5");
			dosen5.setChecked(komponenPenilaianProposalSkripsi.getDosen5());
			dosen5.setParent(hbox);
			dosen5.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianProposalSkripsi.setDosen5(dosen5.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianProposalSkripsi);
				}
			});
			final MyCheckboxConfig dosen6 = new MyCheckboxConfig("Dsn6");
			dosen6.setChecked(komponenPenilaianProposalSkripsi.getDosen6());
			dosen6.setParent(hbox);
			dosen6.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					komponenPenilaianProposalSkripsi.setDosen6(dosen6.isChecked());
					Common.refreshSaveOrUpdate(komponenPenilaianProposalSkripsi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, komponenPenilaianProposalSkripsi,
					KomponenPenilaianProposalSkripsiAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KomponenPenilaianProposalSkripsi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		komponenPenilaianProposalSkripsi = (KomponenPenilaianProposalSkripsi) obj;
		init(komponenPenilaianProposalSkripsi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi) {
		this.komponenPenilaianProposalSkripsi = komponenPenilaianProposalSkripsi;
		addWindow.setTitle(komponenPenilaianProposalSkripsi.getId() == null ? "Tambah Komponen Penilaian" : "Ubah Komponen Penilaian");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Komponen Penilaian"));
		row.appendChild(nama = new Textbox(komponenPenilaianProposalSkripsi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Urut"));
		row.appendChild(nomorUrut = new MyIntbox(komponenPenilaianProposalSkripsi.getNomorUrut()));
		nomorUrut.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Induk"));
		row.appendChild(parent = new Combobox());
		parent.setWidth("90%");
		Common.insertComboDanSemua(parent, "nama", KomponenPenilaianProposalSkripsi.class);
		Common.selectComboItem(parent, komponenPenilaianProposalSkripsi.getParent());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bobot"));
		row.appendChild(bobot = new MyDoublebox(komponenPenilaianProposalSkripsi.getBobot()));
		bobot.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		MyFormRow rowFakultas = new MyFormRow();
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new Label(ais.common.Common.getBahasaConfig("Fakultas")));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, komponenPenilaianProposalSkripsi.getFakultas());
		fakultas.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas",
						komponenPenilaianProposalSkripsi.getFakultas() == null ? tbmuser.ambilFakultas()
								: komponenPenilaianProposalSkripsi.getFakultas()));

		MyFormRow rowJurusan = new MyFormRow();
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new Label(ais.common.Common.getBahasaConfig("Jurusan")));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, komponenPenilaianProposalSkripsi.getJurusan());

		if (tbmuser != null && tbmuser.ambilJurusan() != null) {
			jurusan.setDisabled(true);
		}
		if (tbmuser != null && tbmuser.ambilFakultas() != null) {
			fakultas.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(komponenPenilaianProposalSkripsi.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
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
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Komponen Penilaian Proposal Skripsi",
					"Kolom Nama Komponen Penilaian Proposal Skripsi belum Bapak/Ibu isi, padahal kolom ini wajib "
							+ "diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi terlebih dahulu kolom Nama Komponen Penilaian Proposal Skripsi.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (komponenPenilaianProposalSkripsi.getId() != null) {
			komponenPenilaianProposalSkripsi = (KomponenPenilaianProposalSkripsi) session
					.load(KomponenPenilaianProposalSkripsi.class, komponenPenilaianProposalSkripsi.getId());

		}

		komponenPenilaianProposalSkripsi.setNama(nama.getValue());
		komponenPenilaianProposalSkripsi.setNomorUrut(nomorUrut.getValue());
		komponenPenilaianProposalSkripsi.setBobot(bobot.getValue());
		komponenPenilaianProposalSkripsi.setKeterangan(keterangan.getValue());
		komponenPenilaianProposalSkripsi.setKeterangan(keterangan.getValue());
		komponenPenilaianProposalSkripsi
				.setParent((KomponenPenilaianProposalSkripsi) (parent.getSelectedItem() == null ? null
						: parent.getSelectedItem().getValue()));

		komponenPenilaianProposalSkripsi.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null ? null : fakultas.getSelectedItem().getValue()));
		komponenPenilaianProposalSkripsi.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, komponenPenilaianProposalSkripsi);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KomponenPenilaianProposalSkripsi.class)
				
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan"),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))
				.createAlias("parent", "parent", Criteria.LEFT_JOIN);

		if (order)
			criteria.addOrder(Order.asc("parent.nomorUrut")).addOrder(Order.asc("nomorUrut"))
					.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KomponenPenilaianProposalSkripsi> komponenPenilaianProposalSkripsi = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(komponenPenilaianProposalSkripsi);
		grid.setRowRenderer(new KomponenPenilaianProposalSkripsiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
