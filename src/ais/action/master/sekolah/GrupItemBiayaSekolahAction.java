package ais.action.master.sekolah;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.GrupItemBiayaSekolah;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/** CRUD grup item biaya sekolah. Keanggotaan dikelola dari form Item Biaya. */
public class GrupItemBiayaSekolahAction extends GenericCrudAction<GrupItemBiayaSekolah> {

	private static final long serialVersionUID = 4800716294061911035L;
	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;
	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	@Override protected Class<GrupItemBiayaSekolah> getEntityClass() { return GrupItemBiayaSekolah.class; }
	@Override protected GrupItemBiayaSekolah createNewEntity() { return new GrupItemBiayaSekolah(); }
	@Override protected String getWindowTitle() { return "Grup Item Biaya Sekolah"; }
	@Override protected String[] getDownloadUploadContents() {
		return new String[] { "id", "kode", "nama", "sekolah", "keterangan", "aktif" };
	}

	@Override
	protected void onAfterInit(Component comp) throws Exception {
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);
		super.onAfterInit(comp);
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Criteria criteria = HibernateUtil.currentSession().createCriteria(GrupItemBiayaSekolah.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order) criteria.addOrder(Order.asc("kode")).addOrder(Order.asc("nama"));
		criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
				? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
		criteria.add(searchsekolah == null || searchsekolah.getSelectedItem() == null
				|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false));
		criteria.add(searchyayasan == null || searchyayasan.getSelectedItem() == null
				|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@Override protected MyRowRenderer createRenderer() { return new GrupRenderer(); }

	@Override
	protected void buildFormContent(MyWindow window, final GrupItemBiayaSekolah grup) throws Exception {
		org.zkoss.zul.Borderlayout layout = new MyBorderlayout();
		org.zkoss.zul.Center center = new org.zkoss.zul.Center();
		center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
		ZkCompat.setFlex(center, true);
		center.setParent(layout);

		Div card = new Div();
		card.setStyle(FormBuilder.STYLE_CARD_WRAP);
		card.setParent(center);
		Grid form = new Grid();
		form.setStyle("border:none;width:100%;");
		form.setParent(card);
		Rows rows = new Rows();
		rows.setParent(form);
		FormBuilder fb = new FormBuilder(rows);

		kode = new Textbox(grup.getKode()); kode.setWidth("100%");
		fb.addRow("Kode Grup *", kode, "Kode singkat yang tampil sebagai kepala kelompok tagihan");
		nama = new Textbox(grup.getNama()); nama.setWidth("100%");
		fb.addRow("Nama Grup *", nama, "Contoh: Biaya KBM Pondok");

		yayasan = new Combobox(); sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);
		yayasan.setWidth("100%"); yayasan.setReadonly(true);
		sekolah.setWidth("100%"); sekolah.setReadonly(true);
		Common.selectComboItem(yayasan, grup.getYayasan());
		Common.pilihSekolah(sekolah, grup.getSekolah());
		fb.addRow("Yayasan *", yayasan, "Lingkup yayasan pemilik grup");
		fb.addRow("Sekolah *", sekolah, "Item hanya dapat memakai grup pada sekolah yang sama");

		keterangan = new Textbox(grup.getKeterangan());
		keterangan.setWidth("100%"); keterangan.setRows(3);
		fb.addRow("Keterangan", keterangan, "Catatan tambahan bila diperlukan");

		org.zkoss.zul.South south = new org.zkoss.zul.South();
		ZkCompat.setFlex(south, true); south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA); south.setParent(layout);
		Toolbar toolbar = new Toolbar(); toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.addEventListener("onClick", new EventListener() {
			@Override public void onEvent(Event event) throws Exception { addWindow.setVisible(false); }
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.addEventListener("onClick", new EventListener() {
			@Override public void onEvent(Event event) throws Exception {
				if (onSave(event)) { onSearchDefault(null); addWindow.setVisible(false); }
			}
		});
		save.setParent(toolbar);
		layout.setParent(window);
	}

	@Override
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().isEmpty() || nama.getValue().trim().isEmpty()) {
			ais.ui.util.MyMessageboxConfig.show("Kode dan Nama Grup wajib diisi."); return false;
		}
		if (yayasan.getSelectedItem() == null || yayasan.getSelectedItem().getValue() == null
				|| sekolah.getSelectedItem() == null || sekolah.getSelectedItem().getValue() == null) {
			ais.ui.util.MyMessageboxConfig.show("Yayasan dan Sekolah wajib dipilih."); return false;
		}
		Sekolah sekolahData = (Sekolah) sekolah.getSelectedItem().getValue();
		Number duplikat = (Number) HibernateUtil.currentSession().createCriteria(GrupItemBiayaSekolah.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("sekolah", sekolahData))
				.add(Restrictions.ilike("kode", kode.getValue().trim(), MatchMode.EXACT))
				.add(currentEntity.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", currentEntity.getId())).uniqueResult();
		if (duplikat != null && duplikat.intValue() > 0) {
			ais.ui.util.MyMessageboxConfig.show("Kode grup sudah digunakan pada sekolah yang sama."); return false;
		}
		Session session = HibernateUtil.currentSession();
		GrupItemBiayaSekolah grup = currentEntity;
		if (grup.getId() != null) grup = (GrupItemBiayaSekolah) session.load(GrupItemBiayaSekolah.class, grup.getId());
		grup.setKode(kode.getValue()); grup.setNama(nama.getValue()); grup.setKeterangan(keterangan.getValue());
		grup.setYayasan((Yayasan) yayasan.getSelectedItem().getValue()); grup.setSekolah(sekolahData);
		Common.refreshSaveOrUpdate(session, grup); currentEntity = grup;
		return true;
	}

	class GrupRenderer extends MyRowRenderer {
		@Override public void render(final Row row, Object data) throws Exception {
			final GrupItemBiayaSekolah grup = (GrupItemBiayaSekolah) data;
			new Label(grup.getKode()).setParent(row);
			RevisiHelper.createNewRevisi(GrupItemBiayaSekolah.class, grup, grup.getNama()).setParent(row);
			new Label(grup.getSekolah() == null ? "" : grup.getSekolah().getNama()).setParent(row);
			Number jumlah = (Number) HibernateUtil.currentSession().createCriteria(ItemBiayaSekolah.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("grupItemBiayaSekolah", grup)).uniqueResult();
			new Label(jumlah == null ? "0" : String.valueOf(jumlah.longValue())).setParent(row);
			final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif"); aktif.setDisabled(!edit);
			aktif.setChecked(grup.getAktif()); aktif.setParent(row);
			aktif.addEventListener("onCheck", new EventListener() {
				@Override public void onEvent(Event event) throws Exception {
					grup.setAktif(aktif.isChecked()); Common.refreshSaveOrUpdate(grup);
				}
			});
			Common.copyEditDeleteButtons(edit, delete, grup, GrupItemBiayaSekolahAction.this).setParent(row);
		}
	}
}
