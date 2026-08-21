package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.KrsDanSkripsiHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilaiProposalSkripsi;
import ais.database.model.FormatNilaiSkripsi;
import ais.database.model.JenisKegiatanMahasiswa;
import ais.database.model.JenisNilaiHurufMatakuliah;
import ais.database.model.Jurusan;
import ais.database.model.KomponenPenilaianProposalSkripsi;
import ais.database.model.Matakuliah;
import ais.database.model.ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.library.TipeItem;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class FormatNilaiProposalSkripsiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Combobox searchstatusAwalMahasiswa;
	private Checkbox searchaktif;

	private MyDoublebox pembimbing1;
	private MyDoublebox pembimbing2;
	private MyDoublebox pembimbing3;

	private FormatNilaiProposalSkripsi formatNilaiProposalSkripsi;
	private MyDoublebox penguji1;
	private MyDoublebox penguji2;
	private MyDoublebox penguji3;
	// private MyDoublebox penguji4;
	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private MyTextbox nama;
	private MyTextbox kodeMatakuliah;
	private MyIntbox minimalSks;
	private MyDoublebox bobot;
	private MyDoublebox minimalIpk;
	private MyDoublebox minimalAngkaKredit;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox program;
	private Combobox statusAwalMahasiswa;
	private Combobox alurSebelumnya;

	private List<KomponenPenilaianProposalSkripsi> selectedKomponenPenilaianProposalSkripsi;
	private MyTextbox dosen1;
	private MyTextbox dosen2;
	private MyTextbox dosen3;
	private MyTextbox dosen4;
	private MyTextbox dosen5;
	private MyTextbox dosen6;
	private MyCheckboxConfig tidakWajibMengambilMkTertentu;
	private MyCheckboxConfig adaProposal;
	private MyCheckboxConfig harusLunas;
	private MyCheckboxConfig sekaliBayar;
	private MyCheckboxConfig harusMengembalikanBukuPerpustakaan;
	private MyCheckboxConfig hanyaBisaDilakukanSekali;
	private MyDoublebox prosentaseLunas;
	private Textbox kodeItemBiaya;
	private MyCheckboxConfig adaPresentasi;
	private MyTextbox kodeMatakuliahDan;
	private Textbox tahunAngkatan;
	private MyCheckboxConfig terdapatSidangSetelahSelesai;
	private Combobox formatNilaiSkripsi;

	private MyTextbox uploadLampiran1;
	private MyTextbox uploadLampiran2;
	private MyTextbox uploadLampiran3;
	private MyTextbox uploadLampiran4;
	private MyTextbox uploadLampiran5;
	private MyTextbox uploadLampiran6;
	private MyTextbox uploadLampiran7;
	private MyTextbox uploadLampiran8;
	private MyTextbox uploadLampiran9;
	private MyTextbox uploadLampiran10;

	private MyTextbox uploadLampiran11;
	private MyTextbox uploadLampiran12;
	private MyTextbox uploadLampiran13;
	private MyTextbox uploadLampiran14;
	private MyTextbox uploadLampiran15;
	private MyTextbox uploadLampiran16;
	private MyTextbox uploadLampiran17;
	private MyTextbox uploadLampiran18;
	private MyTextbox uploadLampiran19;
	private MyTextbox uploadLampiran20;

	private MyCheckboxConfig uploadLampiran1Wajib;
	private MyCheckboxConfig uploadLampiran2Wajib;
	private MyCheckboxConfig uploadLampiran3Wajib;
	private MyCheckboxConfig uploadLampiran4Wajib;
	private MyCheckboxConfig uploadLampiran5Wajib;
	private MyCheckboxConfig uploadLampiran6Wajib;
	private MyCheckboxConfig uploadLampiran7Wajib;
	private MyCheckboxConfig uploadLampiran8Wajib;
	private MyCheckboxConfig uploadLampiran9Wajib;
	private MyCheckboxConfig uploadLampiran10Wajib;

	private MyCheckboxConfig uploadLampiran11Wajib;
	private MyCheckboxConfig uploadLampiran12Wajib;
	private MyCheckboxConfig uploadLampiran13Wajib;
	private MyCheckboxConfig uploadLampiran14Wajib;
	private MyCheckboxConfig uploadLampiran15Wajib;

	private MyCheckboxConfig uploadLampiran16Wajib;
	private MyCheckboxConfig uploadLampiran17Wajib;
	private MyCheckboxConfig uploadLampiran18Wajib;
	private MyCheckboxConfig uploadLampiran19Wajib;
	private MyCheckboxConfig uploadLampiran20Wajib;

	private MyCheckboxConfig tidakBolehDipilihMahasiswa;

	private Combobox tipeItem1;
	private Combobox tipeItem2;
	private Combobox tipeItem3;
	private Combobox tipeItem4;
	private Combobox tipeItem5;
	private Combobox tipeItem6;
	private Combobox tipeItem7;
	private Combobox tipeItem8;
	private Combobox tipeItem9;
	private Combobox tipeItem10;
	private Combobox tipeItem11;
	private Combobox tipeItem12;
	private Combobox tipeItem13;
	private Combobox tipeItem14;
	private Combobox tipeItem15;

	private Combobox tipeItem16;
	private Combobox tipeItem17;
	private Combobox tipeItem18;
	private Combobox tipeItem19;
	private Combobox tipeItem20;
	private Center center1;
	private MyCheckboxConfig dosen1Aktif;
	private MyCheckboxConfig dosen2Aktif;
	private MyCheckboxConfig dosen3Aktif;
	private MyCheckboxConfig dosen4Aktif;
	private MyCheckboxConfig dosen5Aktif;
	private MyCheckboxConfig dosen6Aktif;
	private MyTextbox kode1;
	private MyTextbox kode2;
	private MyTextbox kode3;
	private MyTextbox kode4;
	private MyTextbox kode5;
	private MyTextbox kode6;
	private Combobox jenis;
	private Combobox jenisNilaiHuruf;
	private MyCheckboxConfig mahasiswaBolehMengubahAgendaAtauJadwalBimbingan;

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

		fakultas = new Combobox();
		jurusan = new Combobox();
		program = new Combobox();
		statusAwalMahasiswa = new Combobox();
		Common.initPrograms(program);
		Common.insertComboDanSemua(statusAwalMahasiswa, new String[] { "nama" }, "keterangan",
				StatusAwalMahasiswa.class, "Semua Status Awal Mahasiswa",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, searchfakultas, searchjurusan);
		Common.initPrograms(searchprogram);
		if (searchprogram.getSelectedItem() != null && searchprogram.getSelectedItem().getValue() == null) {
			searchprogram.getSelectedItem().setLabel("Semua Program");
		}
		Common.insertComboDanSemua(searchstatusAwalMahasiswa, new String[] { "nama" }, "keterangan",
				StatusAwalMahasiswa.class, "Semua Status Awal Mahasiswa",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(searchstatusAwalMahasiswa, null);

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

	class FormatNilaiProposalSkripsiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FormatNilaiProposalSkripsi formatNilaiProposalSkripsi = (FormatNilaiProposalSkripsi) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			RevisiHelper.createNewRevisi(FormatNilaiProposalSkripsi.class, formatNilaiProposalSkripsi,
					formatNilaiProposalSkripsi.getNama()).setParent(vbox);
			
			if(formatNilaiProposalSkripsi.getJenisKegiatanMahasiswa() != null) {
				new Label(formatNilaiProposalSkripsi.getJenisKegiatanMahasiswa().getNama()).setParent(vbox);
			}

			for (String kode : formatNilaiProposalSkripsi.getKodeMatakuliah().split(",")) {
				if (!kode.trim().isEmpty()) {

					Object[] nama = (Object[]) HibernateUtil.currentSession().createCriteria(Matakuliah.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.projectionList().add(Projections.property("kode"))
									.add(Projections.property("nama")))
							.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
									Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
							.setMaxResults(1).uniqueResult();
					if (nama != null && nama.length > 1) {
						new MyLabelKecil(nama[0] + " - " + nama[1] + " (atau)").setParent(vbox);
					}
				}
			}

			String kodeDanEfektif = KrsDanSkripsiHelper.kodeMatakuliahDanEfektif(
					formatNilaiProposalSkripsi.getKodeMatakuliahDan(),
					formatNilaiProposalSkripsi.getKodeMatakuliah());
			for (String kode : kodeDanEfektif.split(",")) {
				if (!kode.trim().isEmpty()) {

					Object[] nama = (Object[]) HibernateUtil.currentSession().createCriteria(Matakuliah.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.setProjection(Projections.projectionList().add(Projections.property("kode"))
									.add(Projections.property("nama")))
							.add(Restrictions.or(Restrictions.ilike("nama", kode.trim(), MatchMode.EXACT),
									Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT)))
							.setMaxResults(1).uniqueResult();
					if (nama != null && nama.length > 1) {
						new MyLabelKecil(nama[0] + " - " + nama[1] + " (dan)").setParent(vbox);
					}
				}
			}

			new Label(Common.numberFormat.get().format(formatNilaiProposalSkripsi.getMinimalSks()) + " / "
					+ Common.numberFormat.get().format(formatNilaiProposalSkripsi.getMinimalIpk()) + " / "
					+ Common.numberFormat.get().format(formatNilaiProposalSkripsi.getMinimalAngkaKredit()) + " / "
					+ Common.numberFormat.get().format(formatNilaiProposalSkripsi.getBobot())).setParent(arg0);

			new Label(formatNilaiProposalSkripsi.getDosen1() + "/"
					+ (formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing1() == null ? ""
							: Common.numberFormat.get().format(formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing1())))
					.setParent(arg0);
			new Label(formatNilaiProposalSkripsi.getDosen2() + "/"
					+ (formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing2() == null ? ""
							: Common.numberFormat.get().format(formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing2())))
					.setParent(arg0);
			new Label(formatNilaiProposalSkripsi.getDosen3() + "/"
					+ (formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing3() == null ? ""
							: Common.numberFormat.get().format(formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing3())))
					.setParent(arg0);

			new Label(formatNilaiProposalSkripsi.getDosen4() + "/"
					+ (formatNilaiProposalSkripsi.getProsentasiNilaiPenguji1() == null ? ""
							: Common.numberFormat.get().format(formatNilaiProposalSkripsi.getProsentasiNilaiPenguji1())))
					.setParent(arg0);
			new Label(formatNilaiProposalSkripsi.getDosen5() + "/"
					+ (formatNilaiProposalSkripsi.getProsentasiNilaiPenguji2() == null ? ""
							: Common.numberFormat.get().format(formatNilaiProposalSkripsi.getProsentasiNilaiPenguji2())))
					.setParent(arg0);
			new Label(formatNilaiProposalSkripsi.getDosen6() + "/"
					+ (formatNilaiProposalSkripsi.getProsentasiNilaiPenguji3() == null ? ""
							: Common.numberFormat.get().format(formatNilaiProposalSkripsi.getProsentasiNilaiPenguji3())))
					.setParent(arg0);

			new Label(formatNilaiProposalSkripsi.getFakultas() == null ? "Semua"
					: formatNilaiProposalSkripsi.getFakultas().getNama()).setParent(arg0);
			new Label(formatNilaiProposalSkripsi.getJurusan() == null ? "Semua"
					: formatNilaiProposalSkripsi.getJurusan().getNama()).setParent(arg0);

			new Label(formatNilaiProposalSkripsi.getTahunAngkatan()).setParent(arg0);
			FormatNilaiProposalSkripsi sebelumnya = formatNilaiProposalSkripsi.ambilSebelumnya();
			new Label((sebelumnya == null ? "" : sebelumnya.getNama())
					+ (formatNilaiProposalSkripsi.getFormatNilaiSkripsi() == null ? ""
							: " / " + formatNilaiProposalSkripsi.getFormatNilaiSkripsi().getNama()))
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(formatNilaiProposalSkripsi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					formatNilaiProposalSkripsi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(formatNilaiProposalSkripsi);
				}
			});

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(formatNilaiProposalSkripsi);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

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

											// Hapus dulu template_format_bimbingan milik format ini (config default yang
											// auto-dibuat per format; FK format_nilai_proposal_skripsi NOT NULL). Bila tidak,
											// DELETE format melanggar FK -> flush gagal & MERUSAK sesi ZK, sehingga
											// onSearchDefault berikutnya gagal "createCriteria without active transaction".
											// currentSession() masih punya transaksi aktif di awal handler (belum ada
											// kegagalan) sehingga aman untuk executeUpdate; sesi TIDAK ditutup manual.
											HibernateUtil.currentSession()
													.createSQLQuery("delete from public.template_format_bimbingan "
															+ "where format_nilai_proposal_skripsi = :id")
													.setLong("id", formatNilaiProposalSkripsi.getId()).executeUpdate();

											Common.refreshDelete(formatNilaiProposalSkripsi);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}
	}

	public void onAdd(Event event) throws Exception {
		init(new FormatNilaiProposalSkripsi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(FormatNilaiProposalSkripsi formatNilaiProposalSkripsi) {
		this.formatNilaiProposalSkripsi = formatNilaiProposalSkripsi;
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		List<TipeItem> tipeItems = HibernateUtil.currentSession().createCriteria(TipeItem.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.asc("nama")).list();
		tipeItems.add(null);

		West west = new West();
		west.setTitle("Pendataan Format Nilai");
		west.setParent(borderlayout);
		west.setWidth("40%");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Format Pengajuan"));
		row.appendChild(nama = new MyTextbox(formatNilaiProposalSkripsi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alur Pengajuan Sebelumnya"));
		row.appendChild(alurSebelumnya = new Combobox());
		alurSebelumnya.setWidth("90%");

		Common.insertComboDanSemua(alurSebelumnya, new String[] { "nama", "fakultas", "jurusan" }, "kodeMatakuliah",
				FormatNilaiProposalSkripsi.class, "== Tanpa Alur Pengajuan Sebelumnya ==");
		Common.selectComboItem(alurSebelumnya, formatNilaiProposalSkripsi.getAlurSebelumnya() == null ? null
				: new FormatNilaiProposalSkripsi(formatNilaiProposalSkripsi.getAlurSebelumnya()));
		alurSebelumnya.setWidth("90%");
		alurSebelumnya.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(tidakBolehDipilihMahasiswa = new MyCheckboxConfig("Tidak Boleh Dipilih Mahasiswa"));
		tidakBolehDipilihMahasiswa.setChecked(formatNilaiProposalSkripsi.getTidakBolehDipilihMahasiswa());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(terdapatSidangSetelahSelesai = new MyCheckboxConfig(
				"Terdapat proses pengajuan sidang setelah selesai"));
		terdapatSidangSetelahSelesai.setChecked(formatNilaiProposalSkripsi.getTerdapatSidangSetelahSelesai());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Format Pengajuan Sidang"));
		row.appendChild(formatNilaiSkripsi = new Combobox());
		Common.insertComboDanSemua(formatNilaiSkripsi, new String[] { "nama", "fakultas", "jurusan" }, "kodeMatakuliah",
				FormatNilaiSkripsi.class, "== Tanpa Format Pengajuan Sidang ==");
		Common.selectComboItem(formatNilaiSkripsi, formatNilaiProposalSkripsi.getFormatNilaiSkripsi());
		formatNilaiSkripsi.setWidth("90%");
		formatNilaiSkripsi.setReadonly(true);
		formatNilaiSkripsi.setDisabled(!terdapatSidangSetelahSelesai.isChecked());

		terdapatSidangSetelahSelesai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				formatNilaiSkripsi.setDisabled(!terdapatSidangSetelahSelesai.isChecked());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama Matakuliah (Atau)"));
		row.appendChild(kodeMatakuliah = new MyTextbox(formatNilaiProposalSkripsi.getKodeMatakuliah()));
		kodeMatakuliah.setWidth("90%");
		kodeMatakuliah.setRows(2);
		Common.initKeterangan(rows,
				"Jika terdapat banyak kode atau nama matakuliah, pisah menggunakan tanda koma (,). Misal : BSC123,DCFR45,DESW56 maka artinya adalah BSC123 atau DCFR45 atau DESW56");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode/Nama Matakuliah (Dan)"));
		row.appendChild(kodeMatakuliahDan = new MyTextbox(formatNilaiProposalSkripsi.getKodeMatakuliahDan()));
		kodeMatakuliahDan.setWidth("90%");
		kodeMatakuliahDan.setRows(2);
		Common.initKeterangan(rows,
				"Jika terdapat banyak kode atau nama matakuliah, pisah menggunakan tanda koma (,). Misal : BSC123,DCFR45,DESW56 maka artinya adalah BSC123 dan DCFR45 dan DESW56");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunAngkatan = new Textbox(formatNilaiProposalSkripsi.getTahunAngkatan()));
		tahunAngkatan.setWidth("90%");

		Common.initKeterangan(rows,
				"(Kosongkan tahun angkatan jika format ini berlaku untuk semua tahun angkatan, jika terdapat banyak tahun angkatan, masukkan tahun angkatan yang dipisahkan koma, contoh 2017,2018,2019");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(tidakWajibMengambilMkTertentu = new MyCheckboxConfig(
				"Mahasiswa Tidak Wajib mengambil Matakuliah Tertentu"));
		tidakWajibMengambilMkTertentu.setChecked(formatNilaiProposalSkripsi.getTidakWajibMengambilMkTertentu());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bobot"));
		row.appendChild(bobot = new MyDoublebox(formatNilaiProposalSkripsi.getBobot()));
		bobot.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(adaProposal = new MyCheckboxConfig("Mahasiswa Harus Mengajukan Proposal"));
		adaProposal.setChecked(formatNilaiProposalSkripsi.getAdaProposal());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(adaPresentasi = new MyCheckboxConfig("Mahasiswa Harus Mengajukan File Presentasi"));
		adaPresentasi.setChecked(formatNilaiProposalSkripsi.getAdaPresentasi());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal SKS"));
		row.appendChild(minimalSks = new MyIntbox(formatNilaiProposalSkripsi.getMinimalSks()));
		minimalSks.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal IPK"));
		row.appendChild(minimalIpk = new MyDoublebox(formatNilaiProposalSkripsi.getMinimalIpk()));
		minimalIpk.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal Angka Kredit"));
		row.appendChild(minimalAngkaKredit = new MyDoublebox(formatNilaiProposalSkripsi.getMinimalAngkaKredit()));
		minimalAngkaKredit.setCols(2);

		jenisNilaiHuruf = new Combobox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Nilai Huruf"));
		Common.insertComboDanSemua(jenisNilaiHuruf, new String[] { "nama" }, "keterangan",
				JenisNilaiHurufMatakuliah.class, "Nilai Huruf Default", Restrictions.eq("aktif", true));
		Common.selectComboItem(true, jenisNilaiHuruf, formatNilaiProposalSkripsi.getJenisNilaiHuruf());
		row.appendChild(jenisNilaiHuruf);
		jenisNilaiHuruf.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas, formatNilaiProposalSkripsi.getFakultas() == null ? tbmuser.ambilFakultas()
				: formatNilaiProposalSkripsi.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan " + Common.getBahasaConfig("Fakultas")
				+ " jika format nilai ini berlaku untuk semua " + Common.getBahasaConfig("Fakultas") + ")");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, formatNilaiProposalSkripsi.getJurusan() == null ? tbmuser.ambilJurusan()
				: formatNilaiProposalSkripsi.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		Common.initKeterangan(rows, "(Kosongkan " + Common.getBahasaConfig("Jurusan")
				+ " jika format nilai ini berlaku untuk semua " + Common.getBahasaConfig("Jurusan") + ")");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program, formatNilaiProposalSkripsi.getProgram());
		row.appendChild(program);
		program.setWidth("90%");
		program.setReadonly(true);

		Common.initKeterangan(rows, "(Kosongkan Program jika format nilai ini berlaku untuk semua Program)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal Mahasiswa"));
		Common.selectComboItem(true, statusAwalMahasiswa, formatNilaiProposalSkripsi.getStatusAwalMahasiswa());
		row.appendChild(statusAwalMahasiswa);
		statusAwalMahasiswa.setWidth("90%");
		statusAwalMahasiswa.setReadonly(true);

		Common.initKeterangan(rows,
				"(Kosongkan Status Awal Mahasiswa jika format nilai ini berlaku untuk semua Status Awal Mahasiswa)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen1Aktif = new MyCheckboxConfig(),
				dosen1 = new MyTextbox(formatNilaiProposalSkripsi.getDosen1()),
				kode1 = new MyTextbox(formatNilaiProposalSkripsi.getKode1()) }));
		row.appendChild(pembimbing1 = new MyDoublebox(formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing1()));
		pembimbing1.setWidth("90%");
		dosen1Aktif.setChecked(formatNilaiProposalSkripsi.getDosen1Aktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen2Aktif = new MyCheckboxConfig(),
				dosen2 = new MyTextbox(formatNilaiProposalSkripsi.getDosen2()),
				kode2 = new MyTextbox(formatNilaiProposalSkripsi.getKode2()) }));
		row.appendChild(pembimbing2 = new MyDoublebox(formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing2()));
		pembimbing2.setWidth("90%");
		dosen2Aktif.setChecked(formatNilaiProposalSkripsi.getDosen2Aktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen3Aktif = new MyCheckboxConfig(),
				dosen3 = new MyTextbox(formatNilaiProposalSkripsi.getDosen3()),
				kode3 = new MyTextbox(formatNilaiProposalSkripsi.getKode3()) }));
		row.appendChild(pembimbing3 = new MyDoublebox(formatNilaiProposalSkripsi.getProsentasiNilaiPembimbing3()));
		pembimbing3.setWidth("90%");
		dosen3Aktif.setChecked(formatNilaiProposalSkripsi.getDosen3Aktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen4Aktif = new MyCheckboxConfig(),
				dosen4 = new MyTextbox(formatNilaiProposalSkripsi.getDosen4()),
				kode4 = new MyTextbox(formatNilaiProposalSkripsi.getKode4()) }));
		row.appendChild(penguji1 = new MyDoublebox(formatNilaiProposalSkripsi.getProsentasiNilaiPenguji1()));
		penguji1.setWidth("90%");
		dosen4Aktif.setChecked(formatNilaiProposalSkripsi.getDosen4Aktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen5Aktif = new MyCheckboxConfig(),
				dosen5 = new MyTextbox(formatNilaiProposalSkripsi.getDosen5()),
				kode5 = new MyTextbox(formatNilaiProposalSkripsi.getKode5()) }));
		row.appendChild(penguji2 = new MyDoublebox(formatNilaiProposalSkripsi.getProsentasiNilaiPenguji2()));
		penguji2.setWidth("90%");
		dosen5Aktif.setChecked(formatNilaiProposalSkripsi.getDosen5Aktif());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Hbox(new Component[] { dosen6Aktif = new MyCheckboxConfig(),
				dosen6 = new MyTextbox(formatNilaiProposalSkripsi.getDosen6()),
				kode6 = new MyTextbox(formatNilaiProposalSkripsi.getKode6()) }));
		row.appendChild(penguji3 = new MyDoublebox(formatNilaiProposalSkripsi.getProsentasiNilaiPenguji3()));
		penguji3.setWidth("90%");
		dosen6Aktif.setChecked(formatNilaiProposalSkripsi.getDosen6Aktif());

		kode1.setCols(3);
		kode2.setCols(3);
		kode3.setCols(3);
		kode4.setCols(3);
		kode5.setCols(3);
		kode6.setCols(3);

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dosen1.setDisabled(!dosen1Aktif.isChecked());
				pembimbing1.setDisabled(!dosen1Aktif.isChecked());
				dosen2.setDisabled(!dosen2Aktif.isChecked());
				pembimbing2.setDisabled(!dosen2Aktif.isChecked());
				dosen3.setDisabled(!dosen3Aktif.isChecked());
				pembimbing3.setDisabled(!dosen3Aktif.isChecked());
				dosen4.setDisabled(!dosen4Aktif.isChecked());
				penguji1.setDisabled(!dosen4Aktif.isChecked());
				dosen5.setDisabled(!dosen5Aktif.isChecked());
				penguji2.setDisabled(!dosen5Aktif.isChecked());
				dosen6.setDisabled(!dosen5Aktif.isChecked());
				penguji3.setDisabled(!dosen5Aktif.isChecked());

				kode1.setDisabled(!dosen1Aktif.isChecked());
				kode2.setDisabled(!dosen2Aktif.isChecked());

				kode3.setDisabled(!dosen3Aktif.isChecked());
				kode4.setDisabled(!dosen4Aktif.isChecked());
				kode5.setDisabled(!dosen5Aktif.isChecked());
				kode6.setDisabled(!dosen6Aktif.isChecked());
			}
		};

		dosen1Aktif.addEventListener("onClick", s);
		dosen2Aktif.addEventListener("onClick", s);
		dosen3Aktif.addEventListener("onClick", s);
		dosen4Aktif.addEventListener("onClick", s);
		dosen5Aktif.addEventListener("onClick", s);
		dosen6Aktif.addEventListener("onClick", s);

		try {
			s.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		jenis = new Combobox();
//		for (String key : FeederJSONImport.JENIS_KEGIATAN.keySet()) {
//			Comboitem comboitem = new Comboitem(key);
//			comboitem.setValue(FeederJSONImport.JENIS_KEGIATAN.get(key));
//			jenis.appendChild(comboitem);
//		}

//		Comboitem comboitem = new Comboitem("Tidak Ditentukan");
//		comboitem.setValue(null);
//		jenis.appendChild(comboitem);

		Common.insertComboDanSemua(jenis, new String[] { "nama" }, "keterengan", JenisKegiatanMahasiswa.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));

		Common.selectComboItem(jenis, formatNilaiProposalSkripsi.getJenisKegiatanMahasiswa());
		row.appendChild(jenis);
		jenis.setWidth("90%");
		jenis.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(harusLunas = new MyCheckboxConfig("Mahasiswa Harus Lunas Biaya Semester"));
		harusLunas.setChecked(formatNilaiProposalSkripsi.getHarusLunas());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prosentase Lunas Biaya Semester"));
		row.appendChild(prosentaseLunas = new MyDoublebox(formatNilaiProposalSkripsi.getProsentaseLunas()));
		prosentaseLunas.setCols(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Item Biaya"));
		row.appendChild(kodeItemBiaya = new Textbox(formatNilaiProposalSkripsi.getKodeItemBiaya()));
		kodeItemBiaya.setWidth("90%");
		kodeItemBiaya.setRows(2);

		Common.initKeterangan(rows,
				"Jika syarat harus membayar biaya tertentu, masukkan kode item biaya yang harus dibayar mahasiswa. Jika item biaya lebih dari satu, pisahkan dengan tanda koma (,), contoh : 502,505,506 dan seterusnya. Dan juga pastikan kode item biaya benar.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(sekaliBayar = new MyCheckboxConfig(
				"Kode item biaya tersebut sekali bayar saja, jadi kalau misalnya mahasiswa membayar di semester 7, tetap bisa mengajukan di semester 8 atau lebih tanpa membayar ulang."));
		sekaliBayar.setChecked(formatNilaiProposalSkripsi.getSekaliBayar());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(harusMengembalikanBukuPerpustakaan = new MyCheckboxConfig(
				"Mahasiswa Harus Mengembalikan Buku Perpustakaan"));
		harusMengembalikanBukuPerpustakaan
				.setChecked(formatNilaiProposalSkripsi.getHarusMengembalikanBukuPerpustakaan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(hanyaBisaDilakukanSekali = new MyCheckboxConfig("Hanya bisa dilakukan sekali oleh Mahasiswa"));
		hanyaBisaDilakukanSekali.setChecked(formatNilaiProposalSkripsi.getHanyaBisaDilakukanSekali());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		row.appendChild(mahasiswaBolehMengubahAgendaAtauJadwalBimbingan = new MyCheckboxConfig(
				"Mahasiswa boleh mengubah agenda atau jadwal bimbingan"));
		mahasiswaBolehMengubahAgendaAtauJadwalBimbingan
				.setChecked(formatNilaiProposalSkripsi.getMahasiswaBolehMengubahAgendaAtauJadwalBimbingan());

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelBold("Daftar Lampiran : "));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran1 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran1()));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran1Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran1.setWidth("90%");
		uploadLampiran1Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran1Wajib());
		hbox.appendChild(tipeItem1 = new Combobox());
		Common.insertComboItems(tipeItem1, "nama", "kode", tipeItems);
		tipeItem1.setCols(5);
		tipeItem1.setReadonly(true);
		Common.selectComboItem(tipeItem1, formatNilaiProposalSkripsi.getTipeItem1() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem1()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran2 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran2()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran2Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran2.setWidth("90%");
		uploadLampiran2Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran2Wajib());
		hbox.appendChild(tipeItem2 = new Combobox());
		Common.insertComboItems(tipeItem2, "nama", "kode", tipeItems);
		tipeItem2.setCols(5);
		tipeItem2.setReadonly(true);
		Common.selectComboItem(tipeItem2, formatNilaiProposalSkripsi.getTipeItem2() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem2()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran3 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran3()));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran3Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran3.setWidth("90%");
		uploadLampiran3Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran3Wajib());

		hbox.appendChild(tipeItem3 = new Combobox());
		Common.insertComboItems(tipeItem3, "nama", "kode", tipeItems);
		tipeItem3.setCols(5);
		tipeItem3.setReadonly(true);
		Common.selectComboItem(tipeItem3, formatNilaiProposalSkripsi.getTipeItem3() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem3()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran4 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran4()));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran4Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran4.setWidth("90%");
		uploadLampiran4Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran4Wajib());

		hbox.appendChild(tipeItem4 = new Combobox());
		Common.insertComboItems(tipeItem4, "nama", "kode", tipeItems);
		tipeItem4.setCols(5);
		tipeItem4.setReadonly(true);
		Common.selectComboItem(tipeItem4, formatNilaiProposalSkripsi.getTipeItem4() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem4()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran5 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran5()));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran5Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran5.setWidth("90%");
		uploadLampiran5Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran5Wajib());

		hbox.appendChild(tipeItem5 = new Combobox());
		Common.insertComboItems(tipeItem5, "nama", "kode", tipeItems);
		tipeItem5.setCols(5);
		tipeItem5.setReadonly(true);
		Common.selectComboItem(tipeItem5, formatNilaiProposalSkripsi.getTipeItem5() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem5()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran6 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran6()));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran6Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran6.setWidth("90%");
		uploadLampiran6Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran6Wajib());

		hbox.appendChild(tipeItem6 = new Combobox());
		Common.insertComboItems(tipeItem6, "nama", "kode", tipeItems);
		tipeItem6.setCols(5);
		tipeItem6.setReadonly(true);
		Common.selectComboItem(tipeItem6, formatNilaiProposalSkripsi.getTipeItem6() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem6()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran7 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran7()));

		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran7Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran7.setWidth("90%");
		uploadLampiran7Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran7Wajib());
		hbox.appendChild(tipeItem7 = new Combobox());
		Common.insertComboItems(tipeItem7, "nama", "kode", tipeItems);
		tipeItem7.setCols(5);
		tipeItem7.setReadonly(true);
		Common.selectComboItem(tipeItem7, formatNilaiProposalSkripsi.getTipeItem7() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem7()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran8 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran8()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran8Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran8.setWidth("90%");
		uploadLampiran8Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran8Wajib());

		hbox.appendChild(tipeItem8 = new Combobox());
		Common.insertComboItems(tipeItem8, "nama", "kode", tipeItems);
		tipeItem8.setCols(5);
		tipeItem8.setReadonly(true);
		Common.selectComboItem(tipeItem8, formatNilaiProposalSkripsi.getTipeItem8() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem8()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran9 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran9()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran9Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran9.setWidth("90%");
		uploadLampiran9Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran9Wajib());

		hbox.appendChild(tipeItem9 = new Combobox());
		Common.insertComboItems(tipeItem9, "nama", "kode", tipeItems);
		tipeItem9.setCols(5);
		tipeItem9.setReadonly(true);
		Common.selectComboItem(tipeItem9, formatNilaiProposalSkripsi.getTipeItem9() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem9()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran10 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran10()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran10Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran10.setWidth("90%");
		uploadLampiran10Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran10Wajib());

		hbox.appendChild(tipeItem10 = new Combobox());
		Common.insertComboItems(tipeItem10, "nama", "kode", tipeItems);
		tipeItem10.setCols(5);
		tipeItem10.setReadonly(true);
		Common.selectComboItem(tipeItem10, formatNilaiProposalSkripsi.getTipeItem10() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem10()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran11 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran11()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran11Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran11.setWidth("90%");
		uploadLampiran11Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran11Wajib());

		hbox.appendChild(tipeItem11 = new Combobox());
		Common.insertComboItems(tipeItem11, "nama", "kode", tipeItems);
		tipeItem11.setCols(5);
		tipeItem11.setReadonly(true);
		Common.selectComboItem(tipeItem11, formatNilaiProposalSkripsi.getTipeItem11() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem11()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran12 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran12()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran12Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran12.setWidth("90%");
		uploadLampiran12Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran12Wajib());

		hbox.appendChild(tipeItem12 = new Combobox());
		Common.insertComboItems(tipeItem12, "nama", "kode", tipeItems);
		tipeItem12.setCols(5);
		tipeItem12.setReadonly(true);
		Common.selectComboItem(tipeItem12, formatNilaiProposalSkripsi.getTipeItem12() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem12()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran13 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran13()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran13Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran13.setWidth("90%");
		uploadLampiran13Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran13Wajib());

		hbox.appendChild(tipeItem13 = new Combobox());
		Common.insertComboItems(tipeItem13, "nama", "kode", tipeItems);
		tipeItem13.setCols(5);
		tipeItem13.setReadonly(true);
		Common.selectComboItem(tipeItem13, formatNilaiProposalSkripsi.getTipeItem13() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem13()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran14 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran14()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran14Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran14.setWidth("90%");
		uploadLampiran14Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran14Wajib());

		hbox.appendChild(tipeItem14 = new Combobox());
		Common.insertComboItems(tipeItem14, "nama", "kode", tipeItems);
		tipeItem14.setCols(5);
		tipeItem14.setReadonly(true);
		Common.selectComboItem(tipeItem14, formatNilaiProposalSkripsi.getTipeItem14() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem14()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran15 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran15()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran15Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran15.setWidth("90%");
		uploadLampiran15Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran15Wajib());

		hbox.appendChild(tipeItem15 = new Combobox());
		Common.insertComboItems(tipeItem15, "nama", "kode", tipeItems);
		tipeItem15.setCols(5);
		tipeItem15.setReadonly(true);
		Common.selectComboItem(tipeItem15, formatNilaiProposalSkripsi.getTipeItem15() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem15()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran16 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran16()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran16Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran16.setWidth("90%");
		uploadLampiran16Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran16Wajib());

		hbox.appendChild(tipeItem16 = new Combobox());
		Common.insertComboItems(tipeItem16, "nama", "kode", tipeItems);
		tipeItem16.setCols(5);
		tipeItem16.setReadonly(true);
		Common.selectComboItem(tipeItem16, formatNilaiProposalSkripsi.getTipeItem16() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem16()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran17 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran17()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran17Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran17.setWidth("90%");
		uploadLampiran17Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran17Wajib());

		hbox.appendChild(tipeItem17 = new Combobox());
		Common.insertComboItems(tipeItem17, "nama", "kode", tipeItems);
		tipeItem17.setCols(5);
		tipeItem17.setReadonly(true);
		Common.selectComboItem(tipeItem17, formatNilaiProposalSkripsi.getTipeItem17() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem17()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran18 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran18()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran18Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran18.setWidth("90%");
		uploadLampiran18Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran18Wajib());

		hbox.appendChild(tipeItem18 = new Combobox());
		Common.insertComboItems(tipeItem18, "nama", "kode", tipeItems);
		tipeItem18.setCols(5);
		tipeItem18.setReadonly(true);
		Common.selectComboItem(tipeItem18, formatNilaiProposalSkripsi.getTipeItem18() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem18()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran19 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran19()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran19Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran19.setWidth("90%");
		uploadLampiran19Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran19Wajib());

		hbox.appendChild(tipeItem19 = new Combobox());
		Common.insertComboItems(tipeItem19, "nama", "kode", tipeItems);
		tipeItem19.setCols(5);
		tipeItem19.setReadonly(true);
		Common.selectComboItem(tipeItem19, formatNilaiProposalSkripsi.getTipeItem19() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem19()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(uploadLampiran20 = new MyTextbox(formatNilaiProposalSkripsi.getUploadLampiran20()));
		hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(uploadLampiran20Wajib = new MyCheckboxConfig("Wajib di-upload"));
		uploadLampiran20.setWidth("90%");
		uploadLampiran20Wajib.setChecked(formatNilaiProposalSkripsi.getUploadLampiran20Wajib());

		hbox.appendChild(tipeItem20 = new Combobox());
		Common.insertComboItems(tipeItem20, "nama", "kode", tipeItems);
		tipeItem20.setCols(5);
		tipeItem20.setReadonly(true);
		Common.selectComboItem(tipeItem20, formatNilaiProposalSkripsi.getTipeItem20() == null ? null
				: new TipeItem(formatNilaiProposalSkripsi.getTipeItem20()));

		center1 = new Center();
		center1.setTitle("Komponen Penilaian dan Template Bimbingan");
		center1.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center1, true);

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

		reloadKomponen();
		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadKomponen();
			}
		});
		fakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadKomponen();
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void reloadKomponen() {
		Common.clear(center1);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(center1);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		tabs.appendChild(new MyTabConfig("Komponen Penilaian", "/img/Folder-Scheduled-Tasks-icon.png"));

		MyTabConfig template;
		tabs.appendChild(template = new MyTabConfig("Template Bimbingan", "/img/User-Interface-Menu-icon.png"));
		template.setVisible(formatNilaiProposalSkripsi.getId() != null);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
		tabpanels.appendChild(tabpanelUtama);

		Borderlayout myborderlayout = new Borderlayout();
		myborderlayout.setParent(tabpanelUtama);

		Center centerKomponenPenilaian = new Center();
		centerKomponenPenilaian.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(centerKomponenPenilaian, true);

		MyGrid subGrid = new MyGrid();
		subGrid.setWidth("100%");
		subGrid.setParent(centerKomponenPenilaian);
		subGrid.setHeight("100%");

		Rows subRows = new Rows();
		subRows.setParent(subGrid);

		MyFormRow subRow = new MyFormRow();
		subRow.setStyle("border:0px;background: transparent;");
		subRow.setParent(subRows);
		subRow.setValign("top");

		Session session = HibernateUtil.currentSession();
		List<KomponenPenilaianProposalSkripsi> komponenPenilaianProposalSkripsis = session
				.createCriteria(KomponenPenilaianProposalSkripsi.class)

				.add(jurusan == null || jurusan.getSelectedItem() == null
						|| jurusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan"),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false)))

				.add(fakultas == null || fakultas.getSelectedItem() == null
						|| fakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false)))

				.createAlias("parent", "parent", Criteria.LEFT_JOIN).addOrder(Order.asc("parent.nomorUrut"))
				.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("nama"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).list();

		TreeMap<KomponenPenilaianProposalSkripsi, List<KomponenPenilaianProposalSkripsi>> dataKomponenPenilaian = new TreeMap<KomponenPenilaianProposalSkripsi, List<KomponenPenilaianProposalSkripsi>>();
		for (KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi : komponenPenilaianProposalSkripsis) {
			if (komponenPenilaianProposalSkripsi.getParent() != null) {
				if (!dataKomponenPenilaian.keySet().contains(komponenPenilaianProposalSkripsi.getParent())) {
					List<KomponenPenilaianProposalSkripsi> datas = new ArrayList<KomponenPenilaianProposalSkripsi>();
					datas.add(komponenPenilaianProposalSkripsi);
					dataKomponenPenilaian.put(komponenPenilaianProposalSkripsi.getParent(), datas);
				} else {
					dataKomponenPenilaian.get(komponenPenilaianProposalSkripsi.getParent())
							.add(komponenPenilaianProposalSkripsi);
				}
			}
		}

		for (KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi : komponenPenilaianProposalSkripsis) {
			if (komponenPenilaianProposalSkripsi.getParent() == null
					&& !dataKomponenPenilaian.containsKey(komponenPenilaianProposalSkripsi)) {
				List<KomponenPenilaianProposalSkripsi> datas = new ArrayList<KomponenPenilaianProposalSkripsi>();
				dataKomponenPenilaian.put(komponenPenilaianProposalSkripsi, datas);
			}
		}

		if (formatNilaiProposalSkripsi.getId() != null) {
			HibernateUtil.currentSession().refresh(this.formatNilaiProposalSkripsi);
		}

		if (formatNilaiProposalSkripsi.getId() != null) {

			selectedKomponenPenilaianProposalSkripsi = session
					.createCriteria(ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi.class)
					.setProjection(Projections.groupProperty("komponenPenilaianProposalSkripsi"))
					.createAlias("komponenPenilaianProposalSkripsi", "komponenPenilaianProposalSkripsi")
					.add(Restrictions.eq("formatNilaiProposalSkripsi", formatNilaiProposalSkripsi)).list();

		} else {
			selectedKomponenPenilaianProposalSkripsi = new ArrayList<KomponenPenilaianProposalSkripsi>();
		}

		Vbox vboxSkala = new Vbox();
		vboxSkala.setPack("top");
		vboxSkala.setParent(subRow);

		for (final KomponenPenilaianProposalSkripsi parent : dataKomponenPenilaian.keySet()) {

			final List<KomponenPenilaianProposalSkripsi> datas = dataKomponenPenilaian.get(parent);
			if (datas.isEmpty()) {
				final Checkbox checkbox = new Checkbox(parent.getNama());
				checkbox.setParent(vboxSkala);
				checkbox.setChecked(selectedKomponenPenilaianProposalSkripsi.contains(parent));
				checkbox.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (checkbox.isChecked()) {
							selectedKomponenPenilaianProposalSkripsi.add(parent);
						} else {
							selectedKomponenPenilaianProposalSkripsi.remove(parent);
						}
					}
				});
			} else {

				final Checkbox checkboxAll = new Checkbox(parent.getNama());
				checkboxAll.setParent(vboxSkala);
				checkboxAll.setChecked(selectedKomponenPenilaianProposalSkripsi.contains(parent));

				Hbox myHb = new Hbox();
				myHb.setParent(vboxSkala);

				myHb.appendChild(new Space());

				Vbox vboxSkalaSub = new Vbox();
				vboxSkalaSub.setPack("top");
				vboxSkalaSub.setParent(myHb);

				final List<Checkbox> checkboxs = new ArrayList<Checkbox>();
				for (final KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi : datas) {

					final Checkbox checkbox = new Checkbox(komponenPenilaianProposalSkripsi.getNama());
					checkboxs.add(checkbox);
					checkbox.setParent(vboxSkalaSub);
					checkbox.setChecked(
							selectedKomponenPenilaianProposalSkripsi.contains(komponenPenilaianProposalSkripsi));
					checkbox.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (checkbox.isChecked()) {
								selectedKomponenPenilaianProposalSkripsi.add(komponenPenilaianProposalSkripsi);
							} else {
								selectedKomponenPenilaianProposalSkripsi.remove(komponenPenilaianProposalSkripsi);
							}
						}
					});

				}

				checkboxAll.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (checkboxAll.isChecked()) {
							selectedKomponenPenilaianProposalSkripsi.add(parent);
						} else {
							selectedKomponenPenilaianProposalSkripsi.remove(parent);
						}

						for (Checkbox checkbox : checkboxs) {
							checkbox.setChecked(checkboxAll.isChecked());
						}
						for (final KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi : datas) {
							if (checkboxAll.isChecked()) {
								selectedKomponenPenilaianProposalSkripsi.add(komponenPenilaianProposalSkripsi);
							} else {
								selectedKomponenPenilaianProposalSkripsi.remove(komponenPenilaianProposalSkripsi);
							}
						}
					}
				});

			}
		}

		Tabpanel tabpaneltemplate = new ais.ui.util.MyTabpanel();
		tabpaneltemplate.setVisible(formatNilaiProposalSkripsi.getId() != null);
		tabpanels.appendChild(tabpaneltemplate);

		Borderlayout myborderlayouttemplate = new Borderlayout();
		myborderlayouttemplate.setParent(tabpaneltemplate);

		Center centerKomponenPenilaiantemplate = new Center();
		centerKomponenPenilaiantemplate.setParent(myborderlayouttemplate);
		ais.ui.util.ZkCompat.setFlex(centerKomponenPenilaiantemplate, true);

		centerKomponenPenilaiantemplate
				.appendChild(new MyInclude("/pages/master/template_format_bimbingan.zul?formatNilaiProposalSkripsi="
						+ formatNilaiProposalSkripsi.getId()));
	}

	public boolean onSave(Event event) throws Exception {
		if (pembimbing1.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Presentase Nilai Pembimbing 1",
					"Kolom Presentase Nilai Pembimbing 1 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Presentase Nilai Pembimbing 1.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			pembimbing1.focus();
			return false;
		}
		if (pembimbing2.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Presentase Nilai Pembimbing 2",
					"Kolom Presentase Nilai Pembimbing 2 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Presentase Nilai Pembimbing 2.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			pembimbing2.focus();
			return false;
		}
		if (pembimbing3.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Presentase Nilai Pembimbing 3",
					"Kolom Presentase Nilai Pembimbing 3 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Presentase Nilai Pembimbing 3.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			pembimbing3.focus();
			return false;
		}
		if (penguji1.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Presentase Nilai Penguji 1",
					"Kolom Presentase Nilai Penguji 1 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Presentase Nilai Penguji 1.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			penguji1.focus();
			return false;
		}

		if (penguji2.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Presentase Nilai Penguji 2",
					"Kolom Presentase Nilai Penguji 2 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Presentase Nilai Penguji 2.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			penguji2.focus();
			return false;
		}

		if (penguji3.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Presentase Nilai Penguji 3",
					"Kolom Presentase Nilai Penguji 3 belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Presentase Nilai Penguji 3.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			penguji3.focus();
			return false;
		}

		// if (penguji4.getValue() == null) {
		// MyMessageboxConfig.show("Presentase Nilai Penguji 4 harus diisi",
		// "Peringatan", MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// penguji4.focus();
		// return false;
		// }
		Double temp_total;
		temp_total = pembimbing1.getValue() + pembimbing2.getValue() + pembimbing3.getValue() + penguji1.getValue()
				+ penguji2.getValue() + penguji3.getValue();
		// + penguji4.getValue().intValue();
		System.out.println("total : " + temp_total);
		if (temp_total.intValue() < 0) {
			MyMessageboxConfig.show("Jumlah Format Nilai Proposal tidak boleh negatif", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (temp_total.intValue() > 100) {
			MyMessageboxConfig.show("Jumlah Format Nilai Proposal tidak boleh lebih besar dari 100", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (temp_total.intValue() != 100) {
			MyMessageboxConfig.show("Jumlah Format Nilai Proposal harus berjumlah 100", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (temp_total.intValue() < 100) {
			MyMessageboxConfig.show("Jumlah Format Nilai Proposal harus berjumlah 100", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (formatNilaiProposalSkripsi.getId() != null) {
			formatNilaiProposalSkripsi = (FormatNilaiProposalSkripsi) session.load(FormatNilaiProposalSkripsi.class,
					formatNilaiProposalSkripsi.getId());
		}
		formatNilaiProposalSkripsi.setTidakBolehDipilihMahasiswa(tidakBolehDipilihMahasiswa.isChecked());
		formatNilaiProposalSkripsi.setMinimalIpk(minimalIpk.getValue());
		formatNilaiProposalSkripsi.setMinimalSks(minimalSks.getValue());
		formatNilaiProposalSkripsi.setNama(nama.getValue());
		formatNilaiProposalSkripsi.setProsentasiNilaiPembimbing1(pembimbing1.getValue());
		formatNilaiProposalSkripsi.setProsentasiNilaiPembimbing2(pembimbing2.getValue());
		formatNilaiProposalSkripsi.setProsentasiNilaiPembimbing3(pembimbing3.getValue());
		formatNilaiProposalSkripsi.setProsentasiNilaiPenguji1(penguji1.getValue());
		formatNilaiProposalSkripsi.setProsentasiNilaiPenguji2(penguji2.getValue());
		formatNilaiProposalSkripsi.setProsentasiNilaiPenguji3(penguji3.getValue());
		// formatNilaiProposalSkripsi.setProsentasiNilaiPenguji4(penguji4.getValue());
		formatNilaiProposalSkripsi.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		formatNilaiProposalSkripsi.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		formatNilaiProposalSkripsi.setProgram(
				(String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
						: program.getSelectedItem().getValue()));
		formatNilaiProposalSkripsi.setStatusAwalMahasiswa(
				(StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null
						|| statusAwalMahasiswa.getSelectedItem().getValue() == null ? null
								: statusAwalMahasiswa.getSelectedItem().getValue()));

		formatNilaiProposalSkripsi.setDosen1(dosen1.getValue());
		formatNilaiProposalSkripsi.setDosen2(dosen2.getValue());
		formatNilaiProposalSkripsi.setDosen3(dosen3.getValue());
		formatNilaiProposalSkripsi.setDosen4(dosen4.getValue());
		formatNilaiProposalSkripsi.setDosen5(dosen5.getValue());
		formatNilaiProposalSkripsi.setDosen6(dosen6.getValue());
		String kodeMatakuliahAtau = kodeMatakuliah.getValue().trim();
		formatNilaiProposalSkripsi.setKodeMatakuliah(kodeMatakuliahAtau);
		formatNilaiProposalSkripsi.setKodeMatakuliahDan(KrsDanSkripsiHelper.kodeMatakuliahDanEfektif(
				kodeMatakuliahDan.getValue(), kodeMatakuliahAtau));

		formatNilaiProposalSkripsi.setAdaProposal(adaProposal.isChecked());
		formatNilaiProposalSkripsi.setAdaPresentasi(adaPresentasi.isChecked());
		formatNilaiProposalSkripsi.setMinimalAngkaKredit(minimalAngkaKredit.getValue());
		formatNilaiProposalSkripsi.setHarusLunas(harusLunas.isChecked());
		formatNilaiProposalSkripsi.setProsentaseLunas(prosentaseLunas.getValue());
		formatNilaiProposalSkripsi
				.setHarusMengembalikanBukuPerpustakaan(harusMengembalikanBukuPerpustakaan.isChecked());
		formatNilaiProposalSkripsi.setKodeItemBiaya(kodeItemBiaya.getValue());
		formatNilaiProposalSkripsi.setSekaliBayar(sekaliBayar.isChecked());
		formatNilaiProposalSkripsi.setHanyaBisaDilakukanSekali(hanyaBisaDilakukanSekali.isChecked());
		formatNilaiProposalSkripsi.setBobot(bobot.getValue());
		formatNilaiProposalSkripsi.setTidakWajibMengambilMkTertentu(tidakWajibMengambilMkTertentu.isChecked());
		formatNilaiProposalSkripsi.setTahunAngkatan(tahunAngkatan.getValue().trim());
		formatNilaiProposalSkripsi.setTerdapatSidangSetelahSelesai(terdapatSidangSetelahSelesai.isChecked());
		formatNilaiProposalSkripsi.setAlurSebelumnya(alurSebelumnya == null || alurSebelumnya.getSelectedItem() == null
				|| alurSebelumnya.getSelectedItem().getValue() == null ? null
						: ((FormatNilaiProposalSkripsi) alurSebelumnya.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi
				.setFormatNilaiSkripsi((FormatNilaiSkripsi) (formatNilaiSkripsi.getSelectedItem() == null ? null
						: formatNilaiSkripsi.getSelectedItem().getValue()));

		formatNilaiProposalSkripsi.setUploadLampiran1(uploadLampiran1.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran2(uploadLampiran2.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran3(uploadLampiran3.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran4(uploadLampiran4.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran5(uploadLampiran5.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran6(uploadLampiran6.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran7(uploadLampiran7.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran8(uploadLampiran8.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran9(uploadLampiran9.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran10(uploadLampiran10.getValue());

		formatNilaiProposalSkripsi.setUploadLampiran11(uploadLampiran11.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran12(uploadLampiran12.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran13(uploadLampiran13.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran14(uploadLampiran14.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran15(uploadLampiran15.getValue());

		formatNilaiProposalSkripsi.setUploadLampiran1Wajib(uploadLampiran1Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran2Wajib(uploadLampiran2Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran3Wajib(uploadLampiran3Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran4Wajib(uploadLampiran4Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran5Wajib(uploadLampiran5Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran6Wajib(uploadLampiran6Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran7Wajib(uploadLampiran7Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran8Wajib(uploadLampiran8Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran9Wajib(uploadLampiran9Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran10Wajib(uploadLampiran10Wajib.isChecked());

		formatNilaiProposalSkripsi.setUploadLampiran11Wajib(uploadLampiran11Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran12Wajib(uploadLampiran12Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran13Wajib(uploadLampiran13Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran14Wajib(uploadLampiran14Wajib.isChecked());
		formatNilaiProposalSkripsi.setUploadLampiran15Wajib(uploadLampiran15Wajib.isChecked());

		formatNilaiProposalSkripsi.setTipeItem1(
				tipeItem1.getSelectedItem() == null || tipeItem1.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem1.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem2(
				tipeItem2.getSelectedItem() == null || tipeItem2.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem2.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem3(
				tipeItem3.getSelectedItem() == null || tipeItem3.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem3.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem4(
				tipeItem4.getSelectedItem() == null || tipeItem4.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem4.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem5(
				tipeItem5.getSelectedItem() == null || tipeItem5.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem5.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem6(
				tipeItem6.getSelectedItem() == null || tipeItem6.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem6.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem7(
				tipeItem7.getSelectedItem() == null || tipeItem7.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem7.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem8(
				tipeItem8.getSelectedItem() == null || tipeItem8.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem8.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem9(
				tipeItem9.getSelectedItem() == null || tipeItem9.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem9.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem10(
				tipeItem10.getSelectedItem() == null || tipeItem10.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem10.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem11(
				tipeItem11.getSelectedItem() == null || tipeItem11.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem11.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem12(
				tipeItem12.getSelectedItem() == null || tipeItem12.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem12.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem13(
				tipeItem13.getSelectedItem() == null || tipeItem13.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem13.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem14(
				tipeItem14.getSelectedItem() == null || tipeItem14.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem14.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTipeItem15(
				tipeItem15.getSelectedItem() == null || tipeItem15.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem15.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setTahunAngkatan(tahunAngkatan.getValue());
		formatNilaiProposalSkripsi.setSekaliBayar(sekaliBayar.isChecked());

		formatNilaiProposalSkripsi.setUploadLampiran16(uploadLampiran16.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran16Wajib(uploadLampiran16Wajib.isChecked());
		formatNilaiProposalSkripsi.setTipeItem16(
				tipeItem16.getSelectedItem() == null || tipeItem16.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem16.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setUploadLampiran17(uploadLampiran17.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran17Wajib(uploadLampiran17Wajib.isChecked());
		formatNilaiProposalSkripsi.setTipeItem17(
				tipeItem17.getSelectedItem() == null || tipeItem17.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem17.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setUploadLampiran18(uploadLampiran18.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran18Wajib(uploadLampiran18Wajib.isChecked());
		formatNilaiProposalSkripsi.setTipeItem18(
				tipeItem18.getSelectedItem() == null || tipeItem18.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem18.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setUploadLampiran19(uploadLampiran19.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran19Wajib(uploadLampiran19Wajib.isChecked());
		formatNilaiProposalSkripsi.setTipeItem19(
				tipeItem19.getSelectedItem() == null || tipeItem19.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem19.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setUploadLampiran20(uploadLampiran20.getValue());
		formatNilaiProposalSkripsi.setUploadLampiran20Wajib(uploadLampiran20Wajib.isChecked());
		formatNilaiProposalSkripsi.setTipeItem20(
				tipeItem20.getSelectedItem() == null || tipeItem20.getSelectedItem().getValue() == null ? null
						: ((TipeItem) tipeItem20.getSelectedItem().getValue()).getId());

		formatNilaiProposalSkripsi.setDosen1Aktif(dosen1Aktif.isChecked());
		formatNilaiProposalSkripsi.setDosen2Aktif(dosen2Aktif.isChecked());
		formatNilaiProposalSkripsi.setDosen3Aktif(dosen3Aktif.isChecked());
		formatNilaiProposalSkripsi.setDosen4Aktif(dosen4Aktif.isChecked());
		formatNilaiProposalSkripsi.setDosen5Aktif(dosen5Aktif.isChecked());
		formatNilaiProposalSkripsi.setDosen6Aktif(dosen6Aktif.isChecked());

		formatNilaiProposalSkripsi.setKode1(kode1.getValue().trim());
		formatNilaiProposalSkripsi.setKode2(kode2.getValue().trim());
		formatNilaiProposalSkripsi.setKode3(kode3.getValue().trim());
		formatNilaiProposalSkripsi.setKode4(kode4.getValue().trim());
		formatNilaiProposalSkripsi.setKode5(kode5.getValue().trim());
		formatNilaiProposalSkripsi.setKode6(kode6.getValue().trim());
		formatNilaiProposalSkripsi.setJenisKegiatanMahasiswa(
				(JenisKegiatanMahasiswa) (jenis.getSelectedItem() == null ? null : jenis.getSelectedItem().getValue()));

		formatNilaiProposalSkripsi
				.setJenisNilaiHuruf((JenisNilaiHurufMatakuliah) (jenisNilaiHuruf.getSelectedItem() == null ? null
						: jenisNilaiHuruf.getSelectedItem().getValue()));

		formatNilaiProposalSkripsi.setMahasiswaBolehMengubahAgendaAtauJadwalBimbingan(
				mahasiswaBolehMengubahAgendaAtauJadwalBimbingan.isChecked());

		Common.refreshSaveOrUpdate(formatNilaiProposalSkripsi);

		session.createSQLQuery(
				"delete from proposal_skripsi_punya_komponen_penilaian where format_nilai_proposal_skripsi="
						+ formatNilaiProposalSkripsi.getId())
				.executeUpdate();
		for (KomponenPenilaianProposalSkripsi komponenPenilaianProposalSkripsi : selectedKomponenPenilaianProposalSkripsi) {
			ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi proposalSkripsiPunyaKomponenPenilaianProposalSkripsi = new ProposalSkripsiPunyaKomponenPenilaianProposalSkripsi();
			proposalSkripsiPunyaKomponenPenilaianProposalSkripsi
					.setKomponenPenilaianProposalSkripsi(komponenPenilaianProposalSkripsi);
			proposalSkripsiPunyaKomponenPenilaianProposalSkripsi.setNama(komponenPenilaianProposalSkripsi.getNama());
			proposalSkripsiPunyaKomponenPenilaianProposalSkripsi
					.setFormatNilaiProposalSkripsi(formatNilaiProposalSkripsi);
			session.save(proposalSkripsiPunyaKomponenPenilaianProposalSkripsi);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		// Native session: createCriteria pada currentSession() ZK BUTUH transaksi aktif. Setelah
		// operasi lain (mis. hapus) gagal & mengabort transaksi, currentSession() tak lagi punya tx
		// aktif -> "createCriteria is not valid without active transaction". Native self-healing.
		Session session = HibernateUtil.currentNativeSession();
		Criteria criteria = session.createCriteria(FormatNilaiProposalSkripsi.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.add(searchprogram == null || searchprogram.getSelectedItem() == null
						|| searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("program"),
								Restrictions.eq("program", (String) searchprogram.getSelectedItem().getValue())))

				.add(searchstatusAwalMahasiswa == null || searchstatusAwalMahasiswa.getSelectedItem() == null
						|| searchstatusAwalMahasiswa.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("statusAwalMahasiswa"),
								CommonSearchFilterHelper.eqSelectedWithId("statusAwalMahasiswa",
										searchstatusAwalMahasiswa, false)));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.asc("nama"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<FormatNilaiProposalSkripsi> formatNilaiProposalSkripsi = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(formatNilaiProposalSkripsi);
		grid.setRowRenderer(new FormatNilaiProposalSkripsiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkBobot() {

		Integer bobotCount = null;
		Session session = HibernateUtil.currentSession();
		bobotCount = ((Number) session.createCriteria(FormatNilaiProposalSkripsi.class)
				.setProjection(Projections.rowCount())
				.add(Restrictions.eq("prosentasiNilaiPembimbing1", pembimbing1.getValue()))
				.add(Restrictions.eq("prosentasiNilaiPembimbing2", pembimbing2.getValue()))
				.add(Restrictions.eq("prosentasiNilaiPembimbing3", pembimbing3.getValue()))
				.add(Restrictions.eq("prosentasiNilaiPenguji1", penguji1.getValue()))
				.add(Restrictions.eq("prosentasiNilaiPenguji2", penguji2.getValue()))
				.add(Restrictions.eq("prosentasiNilaiPenguji3", penguji3.getValue()))
				// .add(Restrictions.eq("prosentasiNilaiPenguji4",
				// penguji4.getValue()))
				.add(this.formatNilaiProposalSkripsi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.formatNilaiProposalSkripsi.getId()))
				.uniqueResult()).intValue();

		return !bobotCount.equals(0);
	}

}
