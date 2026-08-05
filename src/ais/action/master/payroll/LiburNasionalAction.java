package ais.action.master.payroll;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.payroll.LiburNasional;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class LiburNasionalAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private Combobox searchtahun;

	private MyTextbox nama;
	private MyDatebox tanggal;
	private MyTextbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private LiburNasional liburNasional;
	private MyToolbarbuttonConfig add;
	private MyDatebox sampai;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		int th = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = th - 10; i <= th + 10; i++) {
			Comboitem comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			searchtahun.appendChild(comboitem);
		}
		Common.selectComboItem(searchtahun, th);

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

	}

	class LiburNasionalRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LiburNasional liburNasional = (LiburNasional) arg1;

			RevisiHelper.createNewRevisi(LiburNasional.class, liburNasional, liburNasional.getNama()).setParent(arg0);
			new Label(Common.dateFormat4.get().format(liburNasional.getTanggal())).setParent(arg0);
			new Label(Common.dateFormat4.get().format(liburNasional.getSampai())).setParent(arg0);

			// Satu sel berisi DUA pilihan: dihitung ketidakhadiran + penanda LIBUR PANJANG.
			// Digabung dalam Vbox agar jumlah kolom grid tidak berubah (header kolom ada di ZUL).
			org.zkoss.zul.Vbox selLibur = new org.zkoss.zul.Vbox();
			selLibur.setParent(arg0);

			final MyCheckboxConfig dihitungKetidakhadiran = new MyCheckboxConfig("Dihitung Ketidakhadiran");
			dihitungKetidakhadiran.setDisabled(!edit);
			dihitungKetidakhadiran.setChecked(liburNasional.getDihitungKetidakhadiran());
			dihitungKetidakhadiran.setParent(selLibur);
			dihitungKetidakhadiran.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					liburNasional.setDihitungKetidakhadiran(dihitungKetidakhadiran.isChecked());
					Common.refreshSaveOrUpdate(liburNasional);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LiburNasional.reInitLiburNasional();
						}
					});
				}
			});

			// ATURAN CUTI — tandai periode ini sebagai LIBUR PANJANG (mis. Idul Fitri, Natal-Tahun Baru).
			// Pelengkap deteksi otomatis (rentang >= ambang hari). Pemblokirannya baru berlaku bila
			// gerbang "cuti_gerbang_blokir_libur_panjang" diaktifkan di Konfigurasi.
			final MyCheckboxConfig liburPanjang = new MyCheckboxConfig("Libur Panjang");
			liburPanjang.setDisabled(!edit);
			liburPanjang.setChecked(liburNasional.getLiburPanjang());
			liburPanjang.setTooltiptext("Tandai bila periode ini tergolong libur panjang. Cuti pada rentang "
					+ "H-x sebelum s.d H+y sesudah libur panjang tidak akan disetujui (angka diatur di Konfigurasi).");
			liburPanjang.setParent(selLibur);
			liburPanjang.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					liburNasional.setLiburPanjang(liburPanjang.isChecked());
					Common.refreshSaveOrUpdate(liburNasional);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LiburNasional.reInitLiburNasional();
						}
					});
				}
			});

			new Label(liburNasional.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, liburNasional, new DataInitDefault() {

				@Override
				public void onSearchDefault(Event event) {
					LiburNasional.reInitLiburNasional();
					LiburNasionalAction.this.onSearchDefault(event);
				}

				@Override
				public void init(GeneralValueObject obj) throws Exception {
					LiburNasionalAction.this.init(obj);
				}
			}).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new LiburNasional());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		init((LiburNasional) obj);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(LiburNasional liburNasional) {
		this.liburNasional = liburNasional;
		addWindow.setTitle(liburNasional.getId() == null ? "Tambah Libur" : "Ubah Libur");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Libur")));
		row.appendChild(nama = new MyTextbox(liburNasional.getNama() == null ? "" : liburNasional.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Mulai Libur")));
		row.appendChild(tanggal = new MyDatebox(liburNasional.getTanggal()));
		tanggal.setWidth("90%");
		tanggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Sampai Libur")));
		row.appendChild(sampai = new MyDatebox(liburNasional.getSampai()));
		sampai.setWidth("90%");
		sampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(
				keterangan = new MyTextbox(liburNasional.getKeterangan() == null ? "" : liburNasional.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Nama Libur Nasional wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Libur Nasional pada kolom yang tersedia; (2) pastikan Nama tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Tanggal Libur Nasional wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan Tanggal Libur Nasional pada kolom yang tersedia; (2) pastikan Tanggal tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (liburNasional.getId() != null) {
			liburNasional = (LiburNasional) session.load(LiburNasional.class, liburNasional.getId());

		}

		liburNasional.setTanggal(tanggal.getValue());
		liburNasional.setSampai(sampai.getValue());
		liburNasional.setNama(nama.getValue());
		liburNasional.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, liburNasional);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				LiburNasional.reInitLiburNasional();
			}
		});

		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<LiburNasional> liburNasional = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(liburNasional);
		grid.setRowRenderer(new LiburNasionalRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(LiburNasional.class);
		if (order)
			criteria.addOrder(Order.asc("tanggal"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)))
				.add(searchtahun.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahun", searchtahun.getSelectedItem().getValue()));
		return criteria;
	}

}
