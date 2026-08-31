package ais.action.master.rab.helper;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;

import ais.action.master.rab.util.WorkspaceSelecter;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.rab.Acara;
import ais.database.model.rab.AcaraPunyaJenisParameter;
import ais.database.model.rab.JenisParameter;
import ais.database.model.rab.Workspace;
import ais.database.model.rab.WorkspacePunyaJenisParameter;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDiv;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyMessageboxConfig;

/**
 * Helper UI untuk merealisasikan nilai parameter tambahan bertipe bebas pada satu {@link Acara}
 * modul RAB, berdasarkan template parameter yang sudah ditentukan di level {@link Workspace}
 * ({@link WorkspacePunyaJenisParameter}). Berbeda dari {@link TugasPunyaJenisParameterHelper}
 * (yang mengizinkan menambah parameter bebas langsung pada tugas), kelas ini hanya menampilkan
 * satu baris per parameter yang SUDAH terdaftar pada workspace terkait — pengguna hanya mengisi
 * nilai realisasi ({@link AcaraPunyaJenisParameter}), bukan menambah/menghapus definisi parameter.
 * Workspace ditentukan lewat {@link WorkspaceSelecter} (item perencanaan terpilih) atau dari
 * {@code acara.getWorkspace()} bila sudah ditetapkan sebelumnya. Sama seperti helper serupa,
 * setiap baris menampilkan lima input nilai berbeda (satu per tipe {@link JenisParameter}) dan
 * hanya menampilkan yang sesuai tipe data parameter tersebut.
 */
public class AcaraPunyaJenisParameterHelper {

	private MyGrid gridParameter;
	private boolean edit = false;
	private WorkspaceSelecter selecter;

	/** Membuat helper untuk {@code gridParameter} dan menentukan status enable input dari hak akses pengguna saat ini. */
	public AcaraPunyaJenisParameterHelper(MyGrid gridParameter) {
		this.gridParameter = gridParameter;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
	}

	/**
	 * Membangun tata letak panel parameter untuk {@code acara}: grid kolom Parameter/Nilai/
	 * Keterangan yang langsung dimuat dengan seluruh parameter template dari workspace terkait
	 * ({@link #loadDataDetail}), diselesaikan lewat {@code selecter}.
	 *
	 * @param acara    acara RAB yang realisasi parameternya dikelola
	 * @param selecter penyedia item perencanaan (workspace) terpilih saat ini
	 * @return {@link Borderlayout} siap ditempelkan ke jendela detail acara
	 * @throws Exception diteruskan dari kegagalan resolusi workspace/query data
	 */
	public Borderlayout initDetail(final Acara acara, final WorkspaceSelecter selecter) throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		this.selecter = selecter;

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Common.clear(gridParameter);
		gridParameter.setParent(center);
		gridParameter.setWidth("100%");
		gridParameter.setHeight("100%");
		Columns columns = new Columns();
		columns.setParent(gridParameter);

		MyColumnConfig column = new MyColumnConfig("Parameter");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Nilai");
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig("Keterangan");
		column.setParent(columns);

		loadDataDetail(acara);

		return borderlayout;
	}

	/**
	 * Menentukan workspace yang berlaku (dari {@code selecter} atau {@code acara.getWorkspace()}),
	 * lalu untuk setiap {@link WorkspacePunyaJenisParameter} template milik workspace tersebut,
	 * mencari (atau membuat baru bila belum ada) baris realisasi {@link AcaraPunyaJenisParameter}
	 * milik {@code acara} dan merendernya sebagai baris grid. Menampilkan peringatan dan berhenti
	 * bila belum ada item perencanaan (workspace) yang dipilih.
	 */
	@SuppressWarnings("unchecked")
	private void loadDataDetail(final Acara acara) throws Exception {
		Workspace myWorkspace = (Workspace) (selecter.select() == null ? acara.getWorkspace()
				: selecter.select().getAttribute("workspace"));
		if (myWorkspace == null) {
			MyMessageboxConfig.show("Pilih salah satu item perencanaan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		acara.setWorkspace(myWorkspace);
		final Workspace workspace = acara.getWorkspace();
		Session session = HibernateUtil.currentSession();
		List<WorkspacePunyaJenisParameter> workspacePunyaJenisParameters = workspace == null
				|| workspace.getId() == null ? new ArrayList<WorkspacePunyaJenisParameter>()
						: session.createCriteria(WorkspacePunyaJenisParameter.class)
								.add(Restrictions.eq("workspace", workspace)).list();

		Rows rows = gridParameter.getRows() == null ? new Rows() : gridParameter.getRows();
		rows.setParent(gridParameter);
		// System.out.println("workspacePunyaJenisParameters = "
		// + workspacePunyaJenisParameters.size());
		for (WorkspacePunyaJenisParameter workspacePunyaJenisParameter : workspacePunyaJenisParameters) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);

			AcaraPunyaJenisParameter realisasiWorkspacePunyaJenisParameter = acara == null || acara.getId() == null
					? new AcaraPunyaJenisParameter()
					: (AcaraPunyaJenisParameter) session.createCriteria(AcaraPunyaJenisParameter.class)
							.add(Restrictions.eq("workspacePunyaJenisParameter", workspacePunyaJenisParameter))
							.add(Restrictions.eq("acara", acara)).setMaxResults(1).uniqueResult();

			if (realisasiWorkspacePunyaJenisParameter == null) {
				realisasiWorkspacePunyaJenisParameter = new AcaraPunyaJenisParameter();
			}

			realisasiWorkspacePunyaJenisParameter.setAcara(acara);
			realisasiWorkspacePunyaJenisParameter.setWorkspacePunyaJenisParameter(workspacePunyaJenisParameter);

			initRow(row, realisasiWorkspacePunyaJenisParameter);
		}
	}

	/**
	 * Mengisi satu baris grid dengan label nama parameter (dari template, tidak dapat diubah), lima
	 * input nilai realisasi (teks/integer/double/tanggal/waktu — hanya yang sesuai
	 * {@code typedata} parameter yang ditampilkan), dan field keterangan. Nilai awal input diambil
	 * dari realisasi tersimpan bila ada, atau fallback ke nilai default template workspace.
	 *
	 * @param row                                     baris ZK yang akan diisi
	 * @param realisasiWorkspacePunyaJenisParameter    entitas realisasi (baru atau tersimpan) yang direpresentasikan baris ini
	 */
	public void initRow(final Row row, final AcaraPunyaJenisParameter realisasiWorkspacePunyaJenisParameter) {
		final WorkspacePunyaJenisParameter workspacePunyaJenisParameter = realisasiWorkspacePunyaJenisParameter
				.getWorkspacePunyaJenisParameter();
		row.setValign("top");row.setAttribute("workspacePunyaJenisParameter", workspacePunyaJenisParameter);
		row.setValign("top");row.setAttribute("realisasiWorkspacePunyaJenisParameter", realisasiWorkspacePunyaJenisParameter);
		JenisParameter myJenisParameter = workspacePunyaJenisParameter.getJenisParameter();
		new Label(myJenisParameter == null ? "" : myJenisParameter.getNama()).setParent(row);

		final Textbox jenisParameterValue = new Textbox(
				realisasiWorkspacePunyaJenisParameter == null ? workspacePunyaJenisParameter.getJenisParameterValue()
						: realisasiWorkspacePunyaJenisParameter.getJenisParameterValue());

		final MyIntbox jenisParameterValueInteger = new MyIntbox(realisasiWorkspacePunyaJenisParameter == null
				? (workspacePunyaJenisParameter.getJenisParameterValueInteger() == null ? 0
						: workspacePunyaJenisParameter.getJenisParameterValueInteger())
				: (realisasiWorkspacePunyaJenisParameter.getJenisParameterValueInteger() == null ? 0
						: realisasiWorkspacePunyaJenisParameter.getJenisParameterValueInteger()));

		final MyDoublebox jenisParameterValueDouble = new MyDoublebox(realisasiWorkspacePunyaJenisParameter == null
				? (workspacePunyaJenisParameter.getJenisParameterValueDouble() == null ? 0.0
						: workspacePunyaJenisParameter.getJenisParameterValueDouble())
				: (realisasiWorkspacePunyaJenisParameter.getJenisParameterValueDouble() == null ? 0.0
						: realisasiWorkspacePunyaJenisParameter.getJenisParameterValueDouble()));

		final MyDatebox jenisParameterValueDate = new MyDatebox(realisasiWorkspacePunyaJenisParameter == null
				? workspacePunyaJenisParameter.getJenisParameterValueDate()
				: realisasiWorkspacePunyaJenisParameter.getJenisParameterValueDate());

		final Timebox jenisParameterValueTime = new ais.ui.util.MyTimebox(realisasiWorkspacePunyaJenisParameter == null
				? workspacePunyaJenisParameter.getJenisParameterValueTime()
				: realisasiWorkspacePunyaJenisParameter.getJenisParameterValueTime());

		jenisParameterValue.setVisible(false);
		jenisParameterValue.setDisabled(!edit);
		jenisParameterValue.setWidth("90%");
		jenisParameterValue.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				realisasiWorkspacePunyaJenisParameter.setJenisParameterValue(jenisParameterValue.getValue());
				row.setValign("top");row.setAttribute("realisasiWorkspacePunyaJenisParameter", realisasiWorkspacePunyaJenisParameter);
			}
		});

		jenisParameterValueInteger.setVisible(false);
		jenisParameterValueInteger.setDisabled(!edit);
		jenisParameterValueInteger.setWidth("90%");
		jenisParameterValueInteger.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				realisasiWorkspacePunyaJenisParameter
						.setJenisParameterValueInteger(jenisParameterValueInteger.getValue());
				row.setValign("top");row.setAttribute("realisasiWorkspacePunyaJenisParameter", realisasiWorkspacePunyaJenisParameter);
			}
		});

		jenisParameterValueDouble.setVisible(false);
		jenisParameterValueDouble.setDisabled(!edit);
		jenisParameterValueDouble.setWidth("90%");
		jenisParameterValueDouble.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				realisasiWorkspacePunyaJenisParameter
						.setJenisParameterValueDouble(jenisParameterValueDouble.getValue());
				row.setValign("top");row.setAttribute("realisasiWorkspacePunyaJenisParameter", realisasiWorkspacePunyaJenisParameter);
			}
		});

		jenisParameterValueDate.setVisible(false);
		jenisParameterValueDate.setDisabled(!edit);
		jenisParameterValueDate.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				realisasiWorkspacePunyaJenisParameter.setJenisParameterValueDate(jenisParameterValueDate.getValue());
				row.setValign("top");row.setAttribute("realisasiWorkspacePunyaJenisParameter", realisasiWorkspacePunyaJenisParameter);
			}
		});

		jenisParameterValueTime.setVisible(false);
		jenisParameterValueTime.setDisabled(!edit);
		jenisParameterValueTime.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				realisasiWorkspacePunyaJenisParameter.setJenisParameterValueTime(jenisParameterValueTime.getValue());
				row.setValign("top");row.setAttribute("realisasiWorkspacePunyaJenisParameter", realisasiWorkspacePunyaJenisParameter);
			}
		});

		// Vbox vbox = new Vbox(new Component[] { jenisParameterValue,
		// jenisParameterValueInteger, jenisParameterValueDouble,
		// jenisParameterValueDate, jenisParameterValueTime });
		MyDiv vbox = new MyDiv();
		vbox.setStyle("border:none;");
		vbox.appendChild(jenisParameterValue);
		vbox.appendChild(jenisParameterValueInteger);
		vbox.appendChild(jenisParameterValueDouble);
		vbox.appendChild(jenisParameterValueDate);
		vbox.appendChild(jenisParameterValueTime);

		vbox.setWidth("90%");
		vbox.setParent(row);

		final Textbox keterangan = new Textbox(workspacePunyaJenisParameter.getKeterangan());
		keterangan.setDisabled(!edit);
		keterangan.setWidth("90%");
		keterangan.setParent(row);
		keterangan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				realisasiWorkspacePunyaJenisParameter.setKeterangan(keterangan.getValue());
				row.setValign("top");row.setAttribute("realisasiWorkspacePunyaJenisParameter", realisasiWorkspacePunyaJenisParameter);
			}
		});

		if (myJenisParameter != null) {
			jenisParameterValue.setVisible(myJenisParameter.getTypedata().equals(String.class.getName()));
			jenisParameterValueInteger.setVisible(myJenisParameter.getTypedata().equals(Integer.class.getName()));
			jenisParameterValueDouble.setVisible(myJenisParameter.getTypedata().equals(Double.class.getName()));
			jenisParameterValueDate.setVisible(myJenisParameter.getTypedata().equals(Date.class.getName()));
			jenisParameterValueTime.setVisible(myJenisParameter.getTypedata().equals(Time.class.getName()));
		}

	}

}
