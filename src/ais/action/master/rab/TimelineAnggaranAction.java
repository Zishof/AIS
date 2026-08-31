package ais.action.master.rab;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkforge.timeline.Bandinfo;
import org.zkforge.timeline.Timeline;
import org.zkforge.timeline.data.OccurEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.West;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataSumberDanaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.action.report.format1.rab.RabReportHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk timeline anggaran. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Timeline timeline}, {@code Combobox
 * tahun}, {@code AmbilDataSatuanKerjaBanbox satuanKerja}, {@code AmbilDataSumberDanaBanbox sumberDana}, {@code
 * Combobox level}, {@code Center center}, {@code SatuanKerja mysatuanKerja}, {@code Bandinfo date};
 * inisialisasi/lifecycle ({@code init()}); operasi domain lain ({@code display()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see MyWindow
 */
public class TimelineAnggaranAction extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4764803952776097025L;

	private Timeline timeline;

	private Combobox tahun;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataSumberDanaBanbox sumberDana;
	private Combobox level;

	private Center center;

	private SatuanKerja mysatuanKerja;

	private Bandinfo date;

	private Bandinfo month;

	public TimelineAnggaranAction() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public TimelineAnggaranAction(SatuanKerja satuanKerja) {
		super();
		try {
			this.mysatuanKerja = satuanKerja;
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				sumberDana.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"),
						(Integer) (tahun.getSelectedItem() == null
								? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
								: tahun.getSelectedItem().getValue()));
				display();
			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("250px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun = new Combobox());
		int year = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = year + 5; i > (year - 20); i--) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
			if (i == year) {
				tahun.setSelectedItem(comboitem);
			}
		}
		tahun.setWidth("90%");
		tahun.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		satuanKerja.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sumber Dana"));
		row.appendChild(sumberDana = new AmbilDataSumberDanaBanbox());
		sumberDana.setWidth("90%");
		sumberDana.setEventListener(eventListener);

		sumberDana.setSatuanKerja(Common.getCurrentUser() == null ? null : Common.getCurrentUser().ambilSatuanKerja(),
				ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Level"));
		row.appendChild(level = new Combobox());
		int defaultLevel = 0;
		for (int i = 0; i < 20; i++) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			level.appendChild(comboitem);
			if (i == defaultLevel) {
				level.setSelectedItem(comboitem);
			}
		}
		level.setWidth("90%");
		level.addEventListener("onChange", eventListener);

		if (this.mysatuanKerja != null) {
			satuanKerja.setAttribute("satuanKerja", this.mysatuanKerja);
			satuanKerja.setValue(this.mysatuanKerja.toString());
			satuanKerja.setDisabled(true);
			sumberDana.setSatuanKerja(this.mysatuanKerja, Calendar.getInstance().get(Calendar.YEAR));
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		display();

	}

	@SuppressWarnings("unchecked")
	public void display() throws Exception {

		Common.clear(center);
		timeline = new Timeline();
		timeline.setHeight("100%");
		timeline.setWidth("100%");
		timeline.setParent(center);

		date = new Bandinfo();
		date.setWidth("40%");
		date.setIntervalUnit("day");
		date.setDate(ais.ui.util.WaktuUtil.getDate());
		timeline.appendChild(date);

		month = new Bandinfo();
		month.setWidth("40%");
		month.setIntervalUnit("month");
		month.setDate(ais.ui.util.WaktuUtil.getDate());
		timeline.appendChild(month);

		if (tahun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu tahun", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			// MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan",
			// MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (level.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu level", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (sumberDana.getAttribute("sumberDana") == null) {
			MyMessageboxConfig.show("Sumber Dana harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Integer tahun1 = (Integer) this.tahun.getSelectedItem().getValue();
		final SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		final SumberDana sumberDana = (SumberDana) this.sumberDana.getAttribute("sumberDana");

		RabReportHelper rabReportHelper = new RabReportHelper(tahun1, satuanKerja, sumberDana);

		SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (satuanKerja != null) {
			satuanKerjas.add(satuanKerja);
			satuanKerjaTreeModel.getChildsSet(satuanKerja, satuanKerjas);
		}
		Session session = HibernateUtil.currentSession();
		List<Workspace> workspaces = session.createCriteria(Workspace.class)
				.add(Restrictions.or(Restrictions.eq("carryOver", true),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
				.add(Restrictions.or(
						satuanKerja == null ? Restrictions.isNull("satuanKerja") : Restrictions.sqlRestriction("false"),
						Restrictions.in("satuanKerja", satuanKerjas)))
				.add(sumberDana == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("sumberDana", sumberDana))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).add(Restrictions.eq("tahunWorkspace", tahun1))
				.list();

		WorkspaceTreeModel workspaceTreeModel = new WorkspaceTreeModel(tahun1, rabReportHelper.getMaxrevisi(),
				satuanKerja, sumberDana);

		Integer level = (Integer) this.level.getSelectedItem().getValue();
		if (level > 0) {
			Iterator<Workspace> it = workspaces.iterator();
			List<Workspace> deletedWorkspaces = new ArrayList<Workspace>();
			while (it.hasNext()) {
				Workspace workspace = it.next();
				List<Long> longs = new ArrayList<Long>();
				workspaceTreeModel.getChildDeepSet(workspace.getId(), longs);
				if (level > longs.size()) {
					deletedWorkspaces.add(workspace);
				}
			}
			workspaces.removeAll(deletedWorkspaces);
		}

		Long parentId = WorkspaceTreeModel.checkForParent(tahun1, satuanKerja, rabReportHelper.getMaxrevisi());

		for (final Workspace workspace : workspaces) {

			List<String> names = new ArrayList<String>();
			workspaceTreeModel.getParentSetName(parentId, workspace.getId(), names);
			Collections.reverse(names);
			names.add(workspace.getNama());

			OccurEvent event = new OccurEvent();
			event.setDescription(workspace.getNama());
			event.setDuration(false);
			event.setText(names.toString());
			if (workspace.getMulai() != null && workspace.getSelesai() == null) {
				event.setStart(workspace.getMulai());
				date.addOccurEvent(event);
				month.addOccurEvent(event);
			} else if (workspace.getMulai() != null && workspace.getSelesai() != null) {
				event.setStart(workspace.getMulai());
				event.setEnd(workspace.getSelesai());
				event.setDuration(true);
				date.addOccurEvent(event);
				month.addOccurEvent(event);
			}

		}
	}

}
