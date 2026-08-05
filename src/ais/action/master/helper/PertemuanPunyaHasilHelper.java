package ais.action.master.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.ChecklistPenilaianUmumOlehPesertaAction;
import ais.action.master.dashboard.admin.RekapHasilMahasiswa;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

public class PertemuanPunyaHasilHelper implements DataLoader {

	private Pertemuan pertemuan;

	protected LampiranLain lampiran;

	private ParameterTambahanPertemuanListener parameterTambahanPertemuanListener;

	public PertemuanPunyaHasilHelper() {

	}

	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();

	private Row groupboxRow;

	public void loadData(Object value) {
		if (pertemuan == null) {
			return;
		}

		Common.clear(groupboxRow);
		Common.clear(groupboxRowAngket);

		MyGrid gridEast = new MyGrid();
		gridEast.setWidth("100%");
		gridEast.setParent(groupboxRowAngket);
		gridEast.setWidth("100%");
		gridEast.setHeight("100%");

		org.zkoss.zul.Columns columns = new org.zkoss.zul.Columns();
		columns.setParent(gridEast);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		columns.appendChild(column);

		Rows rowsEast = new Rows();
		rowsEast.setParent(gridEast);

		List<Row> parameterRows = new ArrayList<Row>();
		parameterTambahanPertemuanListener = new ParameterTambahanPertemuanListener(pertemuan, parameterRows,
				lampiranLains, rowsEast);

		try {
			parameterTambahanPertemuanListener.onEvent(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PertemuanPunyaHasilHelper.java:80");
		}

		buttonDownload.setVisible(mahasiswa == null && siswa == null && !parameterRows.isEmpty());

		String diperuntukkan = null;
		if (pertemuan.getPerkuliahan() != null) {
			diperuntukkan = GrupChecklistPenilaianUmum.UNTUK_PERKULIAHAN;
		} else if (pertemuan.getSkripsi() != null) {
			diperuntukkan = GrupChecklistPenilaianUmum.UNTUK_SIDANG;
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			diperuntukkan = GrupChecklistPenilaianUmum.UNTUK_BIMBINGAN;
		} else if (pertemuan.getKelompokKkn() != null) {
			diperuntukkan = GrupChecklistPenilaianUmum.UNTUK_KKN;
		} else if (pertemuan.getKelompokPkl() != null) {
			diperuntukkan = GrupChecklistPenilaianUmum.UNTUK_PKL;
		} else if (pertemuan.getFormulirKegiatan() != null) {
			diperuntukkan = GrupChecklistPenilaianUmum.UNTUK_KEGIATAN;
		} else if (pertemuan.getKrsMahasiswa() != null) {
			diperuntukkan = GrupChecklistPenilaianUmum.UNTUK_AKADEMIK;
		} else if (pertemuan.getWisuda() != null) {
			diperuntukkan = GrupChecklistPenilaianUmum.UNTUK_WISUDA;
		}

		if (mahasiswa != null) {

			// getPerkuliahan() null utk pertemuan non-akademik (KKN/PKL/Bimbingan/Kegiatan/Wisuda) ->
			// hindari NPE saat baca tahun ajaran / ganjil-genap.
			String tahunAkademik = pertemuan.getPerkuliahan() == null ? null : pertemuan.getPerkuliahan().getTahunAjaran();
			String semester = pertemuan.getPerkuliahan() == null ? null : pertemuan.getPerkuliahan().getGanjilGenap();

			ChecklistPenilaianUmumOlehPesertaAction checklistPenilaianUmumOlehPesertaAction = new ChecklistPenilaianUmumOlehPesertaAction();
			checklistPenilaianUmumOlehPesertaAction.initData(mahasiswa, null, tbmuser, tahunAkademik, semester,
					diperuntukkan, null, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, false, pertemuan.getId(), refresh).setParent(groupboxRow);
			refresh = false;
		}
	}

	private boolean refresh = false;

	private Mahasiswa mahasiswa;

	private Siswa siswa;

	private Tbmuser tbmuser;

	private Row groupboxRowAngket;

	private MyToolbarbuttonConfig buttonDownload;

	public void display(final Pertemuan pertemuan, final Component component) {
		this.pertemuan = pertemuan;
		Common.clear(component);

		Groupbox groupbox = new Groupbox();
		groupbox.setParent(component);
		groupbox.appendChild(new Caption("Evaluasi Pertemuan"));

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(groupbox);

		tbmuser = Common.getCurrentUser();
		mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		siswa = tbmuser == null ? null : tbmuser.getSiswa();

//		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Parameter", "/img/Button-Refresh-icon.png");
//		button.setVisible(mahasiswa == null && siswa == null);
//		button.addEventListener("onClick", new EventListener() {
//
//			@Override
//			public void onEvent(Event arg0) throws Exception {
//				pertemuan.belum();
//				loadData(null);
//			}
//		});
//		button.setParent(toolbar);

		buttonDownload = new MyToolbarbuttonConfig("Download", "/img/print.png");
		buttonDownload.setVisible(mahasiswa == null && siswa == null);
		buttonDownload.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
				pertemuans.add(pertemuan);
				RekapHasilMahasiswa addWindow = new RekapHasilMahasiswa(pertemuans);
				addWindow.setClosable(true);
				addWindow.setTitle("Rekap Hasil");
				addWindow.setHeight("95%");
				addWindow.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
				addWindow.onModal();
			}
		});
		buttonDownload.setParent(toolbar);

		MyToolbarbuttonConfig buttonRefresh = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		buttonRefresh.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				refresh = true;
				pertemuan.belum();
				loadData(null);
			}
		});
		buttonRefresh.setParent(toolbar);

		groupboxRow = Common.tampilanScroll1(groupbox);

		groupboxRowAngket = new Row();
		groupboxRowAngket.setValign("top");
		groupboxRowAngket.setStyle("border:0px;background: transparent;");
		groupboxRowAngket.setParent(groupboxRow.getParent());

		loadData(null);

	}

}