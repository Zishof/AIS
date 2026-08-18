package ais.action.master.helper;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyWindow;

public class UjianMahasiswaHelper implements DataLoader {

	private MyGrid grid;
	private Mahasiswa mahasiswa;
	private Integer semester;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMMM yyyy", Common.locale);

	private Integer semesterPendek;

	public UjianMahasiswaHelper(Integer semesterPendek) {
	}

	class DetailMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");

			final Perkuliahan perkuliahan = (Perkuliahan) ConstantValues.ambil(Perkuliahan.class.getName(),
					(Serializable) data);
			if (perkuliahan == null) {
				row.setVisible(false);
				return;
			}
			Matakuliah matakuliah = perkuliahan.getMatakuliah();
			Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah,
					mahasiswa == null ? null : mahasiswa.getNim(), false);
			matakuliah = matakuliahs[0];
			Matakuliah matakuliahAsli = matakuliahs[1];
			if (matakuliah == null) {
				row.setVisible(false);
				return;
			}
			final MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					tampilAbsensi(perkuliahan, detail);
				}
			});

			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getKode()
					: (matakuliah.getKode() + " (" + matakuliahAsli.getKode() + ")")).setParent(row);
			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getNama()
					: (matakuliah.getNama() + " (" + matakuliahAsli.getNama() + ")")).setParent(row);
			new Label((matakuliah.getId().equals(matakuliahAsli.getId()) ? (matakuliah.getSks() + "")
					: (matakuliah.getSks() + " (" + matakuliahAsli.getSks() + ")")) + " SKS").setParent(row);
			totalSks += matakuliah.getSks();
			ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(row, perkuliahan, false);

			ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(row, perkuliahan);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			List<Pertemuan> utsPertemuans = perkuliahan.ambilPertemuanList();
			for (Pertemuan pertemuan : utsPertemuans) {
				if (pertemuan.getStatusPertemuan() != null && pertemuan.getStatusPertemuan().getUjian()) {
					new Label(pertemuan.getStatusPertemuan().getNama() + ": "
							+ (pertemuan == null || pertemuan.getTanggal() == null ? ""
									: Common.dateFormat4.get().format(pertemuan.getTanggal())))
							.setParent(vbox);

				}
			}
			utsPertemuans = null;

		}

	}

	@SuppressWarnings({})
	private void tampilAbsensi(final Perkuliahan perkuliahan, final MyDetail detail) {

		Common.clear(detail);
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setWidth("95%");
		groupbox.setParent(detail);

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(groupbox);

		Columns columns = new Columns();
		columns.setParent(mygrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Materi");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Metode");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Pert.");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		// column.setWidth("35%");

		Rows rows = new Rows();
		rows.setParent(mygrid);
		for (Long pertemuanid : perkuliahan.ambilPertemuan().values()) {
			Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
			if (pertemuan != null && pertemuan.getStatusPertemuan() != null
					&& pertemuan.getStatusPertemuan().getUjian()) {
				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);

				MyDetail detailData = new MyDetail();
				detailData.setParent(row);
				detailData.setOpen(true);
				AktifitasPerkuliahanHelper.createKeterangan(pertemuan, new DataLoader() {

					@Override
					public void loadData(Object value) {
						tampilAbsensi(perkuliahan, detail);

					}
				}).setParent(detailData);

				new Label(pertemuan.getTanggal() == null ? "" : dateFormat.format(pertemuan.getTanggal()))
						.setParent(row);
				new Label(pertemuan.getTopik()).setParent(row);
				new Label(pertemuan.getMetodePembelajaran()).setParent(row);
				new Label(pertemuan.getStatusPertemuan() == null ? "" : pertemuan.getNama()).setParent(row);

				String kode = pertemuan.retreiveAbsensiKode(mahasiswa.getId());
				String nama = pertemuan.retreiveAbsensiNama(mahasiswa.getId());
				String keterangan = pertemuan.retreiveAbsensiKeterangan(mahasiswa.getId());

				new Label(kode + " (" + nama + ")").setParent(row);
				new Label(keterangan).setParent(row);
			}
		}
	}

	private int totalSks = 0;

	public void loadData(Object value) {

		try {
			totalSks = 0;

			List<Long> tempPerkuliahans = mahasiswa.ambilPerkuliahanDanParalel(semester, semesterPendek);

			ListModel strset = new SimpleListModel(tempPerkuliahans);
			grid.setRowRenderer(new DetailMahasiswaRenderer());
			grid.setModelCheckMobile(strset);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"memuat daftar perkuliahan/ujian mahasiswa",
					e, new String[] {
							"Muat ulang (refresh) halaman ini lalu coba kembali.",
							"Periksa apakah data mahasiswa dan semester yang dipilih sudah benar.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public void display(Mahasiswa mahasiswa, String tahunAjaran, Integer semester, Integer tahapan, Component component,
			Integer semesterPendek, MyWindow window, boolean keDatabase) {

		this.mahasiswa = mahasiswa;
		this.semester = semester;
		this.semesterPendek = semesterPendek;
		Common.clear(component);

		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setWidth("95%");
		groupbox.setParent(component);
		groupbox.appendChild(new Caption("Informasi Ujian Mahasiswa"));
		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek,
				keDatabase);

		Row rowUtama = Common.tampilanScroll1(groupbox);

		rowUtama.getGrid().setVisible(semester > 0);
		if (tahapan != null && tahapan.equals(-1)) {
			rowUtama.getGrid().setVisible(false);
		}
		rowUtama.getGrid().setHeight("100%");
		rowUtama.getGrid().setWidth("100%");

		rowUtama.appendChild(new MyLabelConfig("Dosen Pembimbing Akademik"));
		Dosen dosenPembimbingAkademik = krsMahasiswa.getDosenPa();
		Label dosenPembimbing = new Label(dosenPembimbingAkademik == null ? "Belum memiliki dosen pembimbing akademik"
				: dosenPembimbingAkademik.getNama());
		dosenPembimbing.setParent(rowUtama);

		Row rowUtama1;
		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		} else {
			rowUtama1 = rowUtama;
		}

		rowUtama1.appendChild(new MyLabelConfig("IPS"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getIps())));

		rowUtama1 = new MyFormRow();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());

		rowUtama1.appendChild(new MyLabelConfig("SKS Semester"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getSksYangDiambil())));

		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("IPK"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getIpk())));

		rowUtama1 = new MyFormRow();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());

		rowUtama1.appendChild(new MyLabelConfig("SKS Kumulatif"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getSksk())));

		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		String krs = mahasiswa.rubahKeteranganPengambilanKRS(semester, tahapan, semesterPendek, krsMahasiswa, false);
		rowUtama1.appendChild(new MyLabelConfig("Keterangan"));
		rowUtama1.appendChild(new Html(krs));

		rowUtama1 = new MyFormRow();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());

		rowUtama1.appendChild(new MyLabelConfig("Tahun Akademik"));
		rowUtama1.appendChild(new Label(krsMahasiswa.getTahunAkademik()));

		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("Semester"));
		rowUtama1.appendChild(new Label(krsMahasiswa.getSemester() + " / "
				+ (krsMahasiswa.getSemesterPendek() == null
						? (krsMahasiswa.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)
						: Common.getBahasaConfig("Semester Pendek"))));

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(20);
		grid.setParent(groupbox);
		grid.setSclass("dgrid");

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(Common.getBahasa("label_dosen"));
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Hari/Waktu/Ruang");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jadwal Ujian");
		column.setWidth("20%");

		loadData(null);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Foot foot = new Foot();
				foot.setParent(grid);

				Footer footer = new Footer();
				footer.setParent(foot);
				footer = new Footer();
				footer.setParent(foot);
				footer = new Footer("Total");
				footer.setParent(foot);
				footer = new Footer(Common.numberFormat.get().format(totalSks) + " SKS");
				footer.setParent(foot);
				footer = new Footer();
				footer.setParent(foot);
				footer = new Footer();
				footer.setParent(foot);

				footer = new Footer();
				footer.setParent(foot);
			}
		});
	}

}
