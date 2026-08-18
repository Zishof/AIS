package ais.action.master;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Rows;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHtml;

/**
 * Menampilkan 1 (satu) panel detail pertemuan (seperti tampilan Aktivitas
 * Perkuliahan e-Learning) dalam halaman/popup tersendiri. Dipanggil dari kartu
 * jadwal pada "Informasi Kehadiran Dosen/Guru Harian" melalui iframe modal.
 *
 * Tetap kompatibel Java 1.7 / ZKoss 5.5 (tanpa lambda / stream / try-with-resources).
 */
public class TampilanSatuPertemuanAction extends GenericAutowireComposer {

	private static final long serialVersionUID = 6021873239699170011L;

	private Div host;

	@Override
	public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		Tbmuser tbmuserTemp = Common.getCurrentFromSpringUser();
		if (tbmuserTemp == null) {
			tbmuserTemp = Common.getCurrentUser();
		}
		final Tbmuser tbmuser = tbmuserTemp;

		final String idStr = execution.getParameter("id");
		if (host == null || idStr == null || idStr.trim().isEmpty()) {
			return;
		}

		// Render setelah compose selesai agar komponen/desktop sudah siap.
		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tampil(idStr.trim(), tbmuser);
			}
		});
	}

	private void tampil(String idStr, Tbmuser tbmuser) {
		try {
			Common.clear(host);

			Long id = null;
			try {
				id = Long.valueOf(idStr);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/TampilanSatuPertemuanAction.java:76");
				// id tidak valid
			}

			Session session = HibernateUtil.currentSession();
			Pertemuan pertemuan = id == null ? null : (Pertemuan) session.get(Pertemuan.class, id);

			if (pertemuan == null) {
				new MyHtml("<div style='padding:18px;color:#b91c1c;font-weight:bold;'>"
						+ "Data pertemuan tidak ditemukan atau sudah tidak aktif.</div>").setParent(host);
				return;
			}

			Grid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setSclass("fgrid");
			grid.setParent(host);

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setWidth("100%");
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);

			// Bangun panel pertemuan lengkap (banner, foto dosen, bahan kajian,
			// catatan, tombol Catatan/Ujian/Diskusi/Materi/Tugas/.../Kehadiran).
			DashboardTimelinePertemuan.displayRow(row, Common.isMobile(), pertemuan, null, tbmuser);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			try {
				new MyHtml("<div style='padding:18px;color:#b91c1c;font-weight:bold;'>"
						+ "Gagal menampilkan detail pertemuan.</div>").setParent(host);
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/TampilanSatuPertemuanAction.java:117");
			}
		}
	}
}
