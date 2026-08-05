package ais.action.master.sekolah.psb;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa;
import ais.database.model.sekolah.ParameterVerifikasiCalonSiswa;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7896939206824822505L;
	private GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa;
	private MyWindow addWindow;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchjudul;
	private Combobox gelombangPendaftaranPsb;
	private Combobox searchgelombangPendaftaranPsb;

	private boolean edit = true;
	private boolean delete = true;

	private GelombangPendaftaranPsb selectedGelombangPendaftaranPsb;
	private Textbox judul;
	private Textbox nama;
	private Set<ParameterVerifikasiCalonSiswa> selectedParameterVerifikasiCalonSiswa;

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

		Common.insertCombo(gelombangPendaftaranPsb = new Combobox(), "nama", "keterangan",
				GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(searchgelombangPendaftaranPsb, "nama", "keterangan", GelombangPendaftaranPsb.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (execution.getParameter("gelombangPendaftaranPsb") != null) {
			selectedGelombangPendaftaranPsb = (GelombangPendaftaranPsb) HibernateUtil.currentSession()
					.createCriteria(GelombangPendaftaranPsb.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("gelombangPendaftaranPsb"))))
					.uniqueResult();
			Common.selectComboItem(true, searchgelombangPendaftaranPsb, selectedGelombangPendaftaranPsb);
			searchgelombangPendaftaranPsb.setDisabled(true);
		}

		onSearchDefault(null);
	}

	class PilihanGelombangPendaftaranPsbRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa = (GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa) arg1;

			if (gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getGelombangPendaftaranPsb() == null
					&& selectedGelombangPendaftaranPsb != null) {
				gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa
						.setGelombangPendaftaranPsb(selectedGelombangPendaftaranPsb);
			}

			new Label(gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getGelombangPendaftaranPsb().getNama())
					.setParent(arg0);
			new Label(gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getJudul()).setParent(arg0);
			new Label(gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getNama()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (ParameterVerifikasiCalonSiswa parameterVerifikasiCalonSiswa : gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa
					.getParameterVerifikasiCalonSiswas()) {
				vbox.appendChild(new MyLabelAgakKecil(i + ". " + parameterVerifikasiCalonSiswa.getNama()));
				i++;
			}

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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
											Common.refreshDelete(
													gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa);
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
		init(new GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(
			GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa) {
		this.gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa = gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa;
		addWindow.setTitle(gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getId() == null ? "Tambah Parameter Verifikasi" : "Ubah Parameter Verifikasi");
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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul *"));
		row.appendChild(judul = new Textbox(gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getJudul()));
		judul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Parameter *"));
		row.appendChild(nama = new Textbox(gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran *"));
		Common.selectComboItem(true, gelombangPendaftaranPsb,
				gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getGelombangPendaftaranPsb() == null ? null
						: gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getGelombangPendaftaranPsb());
		row.appendChild(gelombangPendaftaranPsb);
		gelombangPendaftaranPsb.setWidth("90%");

		if (selectedGelombangPendaftaranPsb != null) {
			Common.selectComboItem(gelombangPendaftaranPsb, selectedGelombangPendaftaranPsb);
			gelombangPendaftaranPsb.setDisabled(true);
		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		MyGrid subGrid = new MyGrid();
		row.appendChild(subGrid);

		Columns subColumns = new Columns();
		subColumns.setParent(subGrid);
		subColumns.appendChild(new Column("Parameter Rinci"));

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		List<ParameterVerifikasiCalonSiswa> parameterVerifikasiCalonSiswas = HibernateUtil.currentSession()
				.createCriteria(ParameterVerifikasiCalonSiswa.class).addOrder(Order.asc("nomorUrut"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		if (gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.getId() != null) {
			HibernateUtil.currentSession().refresh(this.gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa);
		}
		selectedParameterVerifikasiCalonSiswa = this.gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa
				.getParameterVerifikasiCalonSiswas();

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);
		for (final ParameterVerifikasiCalonSiswa parameterVerifikasiCalonSiswa : parameterVerifikasiCalonSiswas) {
			final Checkbox checkbox = new Checkbox(parameterVerifikasiCalonSiswa.getNama());
			checkbox.setParent(vboxSkala);
			checkbox.setChecked(selectedParameterVerifikasiCalonSiswa.contains(parameterVerifikasiCalonSiswa));
			checkbox.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						selectedParameterVerifikasiCalonSiswa.add(parameterVerifikasiCalonSiswa);
					} else {
						selectedParameterVerifikasiCalonSiswa.remove(parameterVerifikasiCalonSiswa);
					}
				}
			});
		}

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
		if (judul.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Judul Parameter harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Parameter harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (gelombangPendaftaranPsb.getSelectedItem() == null) {
			MyMessageboxConfig.show("Gelombang Pendaftaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.setJudul(judul.getValue().trim());
		gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.setNama(nama.getValue().trim());
		gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa
				.setGelombangPendaftaranPsb(selectedGelombangPendaftaranPsb != null ? selectedGelombangPendaftaranPsb
						: (GelombangPendaftaranPsb) gelombangPendaftaranPsb.getSelectedItem().getValue());
		gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa
				.setParameterVerifikasiCalonSiswas(selectedParameterVerifikasiCalonSiswa);

		Common.refreshSaveOrUpdate(gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa);
		return true;
	}

	// matapelajaranSekolah

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				List<GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa> gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa = session
						.createCriteria(GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa.class)

						.add(searchgelombangPendaftaranPsb.getSelectedItem() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("gelombangPendaftaranPsb",
										searchgelombangPendaftaranPsb.getSelectedItem().getValue()))

						.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("judul", searchjudul.getValue().trim(), MatchMode.ANYWHERE))

						.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

						.setMaxResults(Common.MAX_RESULT).list();
				ListModel strset = new SimpleListModel(gelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa);
				grid.setRowRenderer(new PilihanGelombangPendaftaranPsbRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

}
