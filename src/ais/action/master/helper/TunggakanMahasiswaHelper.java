package ais.action.master.helper;

import java.util.List;

import org.zkoss.zk.ui.Component;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.model.TunggakanMahasiswa;
import ais.ui.util.MyColumnConfig;

/**
 * Helper tampilan (UI builder) untuk menyajikan ringkasan tunggakan mahasiswa berupa grid
 * "Tahun Akademik / Semester / Jumlah" ke dalam sebuah {@link Component} ZK yang sudah disediakan
 * pemanggil. Tidak melakukan query database sendiri — daftar {@link TunggakanMahasiswa} yang
 * ditampilkan sudah harus disiapkan oleh pemanggil.
 *
 * <p>
 * Terdapat blok kode rinci per-item tunggakan (drill-down ke {@code TunggakanMahasiswaDetail} lewat
 * komponen {@code Detail}) yang dikomentari (nonaktif) — kemungkinan fitur yang pernah ada namun
 * sengaja dimatikan; dibiarkan apa adanya sesuai instruksi untuk tidak mengubah kode fungsional.
 * </p>
 */
public class TunggakanMahasiswaHelper {

	/**
	 * Membersihkan {@code rowInfoTunggakan}, lalu membangun ulang di dalamnya satu grid berisi
	 * baris per {@link TunggakanMahasiswa} (kolom Tahun Akademik, Semester, Jumlah — diformat
	 * dengan {@link Common#numberFormat}). Komponen induk dibuat tampak ({@code setVisible(true)})
	 * sebagai bagian dari pembangunan ulang.
	 *
	 * @param rowInfoTunggakan   komponen kontainer ZK yang akan diisi ulang dengan grid tunggakan
	 * @param tunggakanMahasiswas daftar tunggakan yang akan ditampilkan, satu baris grid per elemen
	 */
	public void display(Component rowInfoTunggakan,
			List<TunggakanMahasiswa> tunggakanMahasiswas) {
		Common.clear(rowInfoTunggakan);
		rowInfoTunggakan.setVisible(true);
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("95%");
		groupbox.setParent(rowInfoTunggakan);
		groupbox.appendChild(new MyCaptionStyled("Informasi Tunggakan Mahasiswa"));

		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(groupbox);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Tahun Akademik");
		column.setParent(columns);

		column = new MyColumnConfig("Semester");
		column.setParent(columns);

		column = new MyColumnConfig("Jumlah");
		column.setAlign("right");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);
		for (final TunggakanMahasiswa tunggakanMahasiswa : tunggakanMahasiswas) {
			Row row = new Row();row.setValign("top");
			row.setParent(rows);

			// final Detail detail = new Detail();
			// detail.setParent(row);
			// detail.addEventListener("onOpen", new EventListener() {
			//
			// @SuppressWarnings("unchecked")
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// Common.clear(detail);
			// if (detail.isOpen()) {
			//
			// Session session = HibernateUtil.currentSession();
			// List<TunggakanMahasiswaDetail> tunggakanMahasiswaDetails =
			// session
			// .createCriteria(TunggakanMahasiswaDetail.class)
			// .add(Restrictions.eq("tunggakanMahasiswa",
			// tunggakanMahasiswa)).list();
			//
			// Double totalBiaya = 0.0;
			// for (TunggakanMahasiswaDetail detailBiaya :
			// tunggakanMahasiswaDetails) {
			// totalBiaya += detailBiaya.getNilaiBiaya() == null ? 0.0
			// : detailBiaya.getNilaiBiaya();
			// }
			// tunggakanMahasiswa.setJumlahTunggakan(totalBiaya);
			// Common.refreshUpdate(session,(tunggakanMahasiswa));
			//
			// Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			// borderlayout.setHeight("450px");
			// borderlayout.setWidth("100%");
			// borderlayout.setParent(detail);
			//
			// Center center = new Center();
			// ais.ui.util.ZkCompat.setFlex(center, true);
			// center.setParent(borderlayout);
			//
			// MyGrid grid = new MyGrid();grid.setWidth("100%");
			// grid.setParent(center);
			// grid.setWidth("100%");
			// grid.setHeight("100%");
			//
			// Columns columns = new Columns();
			// columns.setParent(grid);
			//
			// MyColumnConfig column = new MyColumnConfig("Item Biaya");
			// column.setParent(columns);
			//
			// column = new MyColumnConfig("Jumlah");
			// column.setAlign("right");
			// column.setParent(columns);
			//
			// Rows rows = new Rows();
			// rows.setParent(grid);
			//
			// for (TunggakanMahasiswaDetail detailBiaya :
			// tunggakanMahasiswaDetails) {
			// Row row = new Row();row.setValign("top");
			// row.setParent(rows);
			//
			// row.appendChild(new ais.ui.util.MyLabelConfig(detailBiaya
			// .getItemBiaya() == null ? "" : detailBiaya
			// .getItemBiaya().getNama()));
			// row.appendChild(new ais.ui.util.MyLabelConfig(detailBiaya
			// .getNilaiBiaya() == null ? ""
			// : Common.numberFormat.get().format(detailBiaya
			// .getNilaiBiaya())));
			// }
			//
			// }
			//
			// }
			// });

			row.appendChild(new ais.ui.util.MyLabelConfig(tunggakanMahasiswa.getTahunAkademik()));
			row.appendChild(new ais.ui.util.MyLabelConfig(tunggakanMahasiswa.getSemester() + ""));
			row.appendChild(new ais.ui.util.MyLabelConfig(
					tunggakanMahasiswa.getJumlahTunggakan() == null ? ""
							: Common.numberFormat.get().format(tunggakanMahasiswa
									.getJumlahTunggakan())));
		}

	}

}
