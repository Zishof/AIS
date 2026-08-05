package ais.action.master.pmb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiParameter;
import ais.database.model.Paket;
import ais.database.model.PaketPunyaParameterVerifikasiCalonMahasiswa;
import ais.database.model.ParameterVerifikasiCalonMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class VerifikasiParameterPMBHelper {

	private static Paket resolvePaket(BiodataCalonMahasiswa biodataCalonMahasiswa, Combobox paket, Paket p) {
		try {
			if (p != null && p.getId() != null) {
				return p;
			}
			if (paket != null && paket.getSelectedItem() != null && paket.getSelectedItem().getValue() instanceof Paket) {
				return (Paket) paket.getSelectedItem().getValue();
			}
			return biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getPaket();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<PaketPunyaParameterVerifikasiCalonMahasiswa> ambilParameterPaket(Paket paket) {
		if (paket == null || paket.getId() == null) {
			return new ArrayList<PaketPunyaParameterVerifikasiCalonMahasiswa>();
		}
		try {
			Session session = HibernateUtil.currentSession();
			Paket paketData = (Paket) session.get(Paket.class, paket.getId());
			if (paketData == null) {
				paketData = paket;
			}
			return session.createCriteria(PaketPunyaParameterVerifikasiCalonMahasiswa.class)
					.add(Restrictions.eq("paket", paketData)).addOrder(Order.asc("nama")).list();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new ArrayList<PaketPunyaParameterVerifikasiCalonMahasiswa>();
		}
	}

	@SuppressWarnings("unchecked")
	private static void reloadData(final Rows subRowsParameterVerifikasi, final Paket gel,
			final BiodataCalonMahasiswa biodataCalonMahasiswa,
			final PaketPunyaParameterVerifikasiCalonMahasiswa paketPunyaParameterVerifikasiCalonMahasiswa) {
		Common.clear(subRowsParameterVerifikasi);
		if (gel == null || biodataCalonMahasiswa == null || paketPunyaParameterVerifikasiCalonMahasiswa == null) {
			return;
		}

		Session session = HibernateUtil.currentSession();
		Tbmuser tbmuser = Common.getCurrentUser();

		List<BiodataCalonMahasiswaPunyaVerifikasiParameter> biodataCalonMahasiswaPunyaVerifikasiParameters = session
				.createCriteria(BiodataCalonMahasiswaPunyaVerifikasiParameter.class)
				.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
				.add(Restrictions.eq("paketPunyaParameterVerifikasiCalonMahasiswa",
						paketPunyaParameterVerifikasiCalonMahasiswa))
				.addOrder(Order.asc("nama")).list();

		for (final BiodataCalonMahasiswaPunyaVerifikasiParameter biodataCalonMahasiswaPunyaVerifikasiParameter : biodataCalonMahasiswaPunyaVerifikasiParameters) {
			final Row subRow = new MyRowStyled();
			subRow.setAttribute("biodataCalonMahasiswaPunyaVerifikasiParameter",
					biodataCalonMahasiswaPunyaVerifikasiParameter);
			subRow.setStyle("border:1px;solid;background:transparent;");
			subRow.setParent(subRowsParameterVerifikasi);
			subRow.setValign("top");

			final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(!biodataCalonMahasiswaPunyaVerifikasiParameter.getVerified());
			final MyDetail myvbox = new MyDetail();
			myvbox.setParent(subRow);

			Vbox a = new Vbox();
			a.setParent(subRow);
			a.setWidth("99%");

			if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() == null) {
				final Checkbox checkbox = new Checkbox(biodataCalonMahasiswaPunyaVerifikasiParameter.getNama());
				checkbox.setStyle("font-size:14px;font-weight: bolder;");
				checkbox.setParent(a);
				checkbox.setChecked(biodataCalonMahasiswaPunyaVerifikasiParameter != null
						&& biodataCalonMahasiswaPunyaVerifikasiParameter.getVerified());
				subRow.setAttribute("checkbox", checkbox);

				String tingkatNama = biodataCalonMahasiswaPunyaVerifikasiParameter
						.getParameterVerifikasiCalonMahasiswa() == null ? ""
								: biodataCalonMahasiswaPunyaVerifikasiParameter
										.getParameterVerifikasiCalonMahasiswa().getNama();
				new Label(tingkatNama).setParent(subRow);

				final Textbox keterangan = new Textbox(biodataCalonMahasiswaPunyaVerifikasiParameter == null ? ""
						: biodataCalonMahasiswaPunyaVerifikasiParameter.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(2);
				keterangan.setParent(subRow);
				keterangan.setDisabled(checkbox.isChecked());
				subRow.setAttribute("keterangan", keterangan);

				EventListener listener = new EventListener() {
					@Override
					public void onEvent(Event arg0) throws Exception {
						biodataCalonMahasiswaPunyaVerifikasiParameter.setKeterangan(keterangan.getValue());
						biodataCalonMahasiswaPunyaVerifikasiParameter.setVerified(checkbox.isChecked());
						Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiParameter);
						button.setVisible(!checkbox.isChecked());
						keterangan.setDisabled(checkbox.isChecked());

						Common.clear(myvbox);
						Hbox hbox = new Hbox();
						hbox.setParent(myvbox);
						LampiranLain.createDownloadUploadFileLain(hbox,
								biodataCalonMahasiswaPunyaVerifikasiParameter.getId(),
								BiodataCalonMahasiswaPunyaVerifikasiParameter.class.getName(), "Bukti", false,
								null, null, false, false, false, !checkbox.isChecked());
						new ais.ui.util.MyHtml("<hr>").setParent(myvbox);
					}
				};

				checkbox.addEventListener("onClick", listener);
				keterangan.addEventListener("onChange", listener);
			} else {
				new Label(biodataCalonMahasiswaPunyaVerifikasiParameter.getNama()).setParent(a);
				String tingkatNama = biodataCalonMahasiswaPunyaVerifikasiParameter
						.getParameterVerifikasiCalonMahasiswa() == null ? ""
								: biodataCalonMahasiswaPunyaVerifikasiParameter
										.getParameterVerifikasiCalonMahasiswa().getNama();
				new Label(tingkatNama).setParent(subRow);
				new Label(biodataCalonMahasiswaPunyaVerifikasiParameter.getKeterangan()).setParent(subRow);
			}

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, biodataCalonMahasiswaPunyaVerifikasiParameter.getId(),
					BiodataCalonMahasiswaPunyaVerifikasiParameter.class.getName(), "Bukti", false, null, null, false,
					false, false, !biodataCalonMahasiswaPunyaVerifikasiParameter.getVerified());

			new ais.ui.util.MyHtml("<hr>").setParent(myvbox);
			myvbox.setOpen(true);

			button.setTooltiptext("Hapus Data");
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
											Common.refreshDelete(biodataCalonMahasiswaPunyaVerifikasiParameter);
											reloadData(subRowsParameterVerifikasi, gel, biodataCalonMahasiswa,
													paketPunyaParameterVerifikasiCalonMahasiswa);
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
			button.setParent(subRow);
		}
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public static List<Rows> tampilkanVerifikasi(final BiodataCalonMahasiswa biodataCalonMahasiswa, final Rows rows,
			final Combobox paket, final Paket p) throws Exception {
		return tampilkanVerifikasi(biodataCalonMahasiswa, rows, paket, p, null);
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public static List<Rows> tampilkanVerifikasi(final BiodataCalonMahasiswa biodataCalonMahasiswa, final Rows rows,
			final Combobox paket, final Paket p, final Combobox gelombangPendaftaran) throws Exception {

		final List<Rows> rowsVerifikasi = new ArrayList<Rows>();
		final List<Component> renderedComponents = new ArrayList<Component>();

		final EventListener reloadAll = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				for (Component component : new ArrayList<Component>(renderedComponents)) {
					try {
						component.detach();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/VerifikasiParameterPMBHelper.java:230");
					}
				}
				renderedComponents.clear();
				rowsVerifikasi.clear();

				final Paket gel = resolvePaket(biodataCalonMahasiswa, paket, p);
				if (gel == null || rows == null) {
					return;
				}

				List<PaketPunyaParameterVerifikasiCalonMahasiswa> punyaParameterVerifikasiCalonMahasiswas = ambilParameterPaket(gel);
				for (final PaketPunyaParameterVerifikasiCalonMahasiswa paketPunyaParameterVerifikasiCalonMahasiswa : punyaParameterVerifikasiCalonMahasiswas) {
					final Row rowBerkas = new MyRowStyled();
					rowBerkas.setVisible(false);
					rowBerkas.setParent(rows);
					renderedComponents.add(rowBerkas);
					rowBerkas.appendChild(
							new ais.ui.util.MyLabelBold(paketPunyaParameterVerifikasiCalonMahasiswa.getJudul()));

					final Row rowTambah = new MyRowStyled();
					ais.ui.util.ZkCompat.setSpans(rowTambah, "2");
					rowTambah.setParent(rows);
					renderedComponents.add(rowTambah);
					Button tambah = new Button("Tambah " + paketPunyaParameterVerifikasiCalonMahasiswa.getNama(),
							"/img/new.gif");
					rowTambah.appendChild(tambah);

					final Row rowVerifikasi = new MyRowStyled();
					ais.ui.util.ZkCompat.setSpans(rowVerifikasi, "2");
					rowVerifikasi.setParent(rows);
					renderedComponents.add(rowVerifikasi);
					final MyGrid subGrid = new MyGrid();
					rowVerifikasi.appendChild(subGrid);

					Columns subColumns = new Columns();
					subColumns.setParent(subGrid);

					Column c = new Column("");
					c.setWidth("0%");
					subColumns.appendChild(c);

					c = new Column(paketPunyaParameterVerifikasiCalonMahasiswa.getNama());
					subColumns.appendChild(c);
					c.setWidth("50%");

					c = new Column("Tingkat");
					subColumns.appendChild(c);
					c.setWidth("20%");

					c = new Column("Keterangan");
					subColumns.appendChild(c);

					c = new Column("");
					subColumns.appendChild(c);
					c.setWidth("5%");

					final Rows subRowsParameterVerifikasi = new Rows();
					subRowsParameterVerifikasi.setParent(subGrid);

					final EventListener eventListenerBerkas = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							reloadData(subRowsParameterVerifikasi, gel, biodataCalonMahasiswa,
									paketPunyaParameterVerifikasiCalonMahasiswa);
						}
					};
					eventListenerBerkas.onEvent(null);
					rowsVerifikasi.add(subRowsParameterVerifikasi);

					tambah.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							final MyWindow addWindow = new MyWindow();
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
							addWindow.setHeight("250px");
							addWindow.setWidth("600px");
							addWindow.setTitle("Tambah data " + paketPunyaParameterVerifikasiCalonMahasiswa.getNama());
							Common.clear(addWindow);
							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							Center center = new Center();
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);
							MyGrid grid = new MyGrid();
							grid.setWidth("100%");
							grid.setParent(center);
							grid.setHeight("100%");

							Rows rowsAdd = new Rows();
							rowsAdd.setParent(grid);

							Row row = new MyRowStyled();
							row.setParent(rowsAdd);
							row.appendChild(new ais.ui.util.MyLabelConfig(
									paketPunyaParameterVerifikasiCalonMahasiswa.getNama() + " *"));
							final Textbox nama = new Textbox();
							row.appendChild(nama);
							nama.setWidth("90%");
							nama.setRows(5);

							HibernateUtil.currentSession().refresh(paketPunyaParameterVerifikasiCalonMahasiswa);
							List<ParameterVerifikasiCalonMahasiswa> selectedParameterVerifikasiCalonMahasiswa = new ArrayList<ParameterVerifikasiCalonMahasiswa>(
									paketPunyaParameterVerifikasiCalonMahasiswa
											.getParameterVerifikasiCalonMahasiswas());
							Collections.sort(selectedParameterVerifikasiCalonMahasiswa);

							row = new MyRowStyled();
							row.setParent(rowsAdd);
							row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat *"));
							final Combobox tingkat = new Combobox();
							Common.insertComboItems(tingkat, "nama", selectedParameterVerifikasiCalonMahasiswa);
							tingkat.setWidth("90%");
							row.appendChild(tingkat);

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									addWindow.detach();
								}
							});
							cancel.setParent(toolbar);
							MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
							save.setTooltiptext("Simpan");
							save.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									if (nama.getValue().trim().isEmpty()) {
										MyMessageboxConfig.show(
												paketPunyaParameterVerifikasiCalonMahasiswa.getNama() + " harus diisi",
												"Peringatan", MyMessageboxConfig.OK,
												MyMessageboxConfig.INFORMATION);
										return;
									}
									if (tingkat.getSelectedItem() == null) {
										MyMessageboxConfig.show("Mohon maaf, Tingkat belum dipilih. Langkah yang dapat dilakukan: (1) pilih Tingkat dari daftar yang tersedia; (2) pastikan pilihan telah tersorot sebelum menyimpan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
												MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
										return;
									}

									BiodataCalonMahasiswaPunyaVerifikasiParameter biodataCalonMahasiswaPunyaVerifikasiParameter = new BiodataCalonMahasiswaPunyaVerifikasiParameter();
									biodataCalonMahasiswaPunyaVerifikasiParameter
											.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
									biodataCalonMahasiswaPunyaVerifikasiParameter.setNama(nama.getValue().trim());
									biodataCalonMahasiswaPunyaVerifikasiParameter
											.setPaketPunyaParameterVerifikasiCalonMahasiswa(
													paketPunyaParameterVerifikasiCalonMahasiswa);
									biodataCalonMahasiswaPunyaVerifikasiParameter
											.setParameterVerifikasiCalonMahasiswa(
													(ParameterVerifikasiCalonMahasiswa) tingkat.getSelectedItem().getValue());

									Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiParameter);
									eventListenerBerkas.onEvent(event);
									addWindow.detach();
								}
							});
							save.setParent(toolbar);
							borderlayout.setParent(addWindow);
							addWindow.onModal();
						}
					});
				}
			}
		};

		reloadAll.onEvent(null);
		if (paket != null) {
			paket.addEventListener("onChange", reloadAll);
		}
		if (gelombangPendaftaran != null) {
			gelombangPendaftaran.addEventListener("onChange", reloadAll);
		}
		return rowsVerifikasi;
	}

	@SuppressWarnings("unchecked")
	public static void simpanVerifikasi(BiodataCalonMahasiswa biodataCalonMahasiswa, List<Rows> rows) {
		if (rows == null) {
			return;
		}
		for (Rows subRowsParameterVerifikasi : rows) {
			if (subRowsParameterVerifikasi == null) {
				continue;
			}
			List<Row> rowsVerifikasi = subRowsParameterVerifikasi.getChildren();
			for (Row row : rowsVerifikasi) {
				try {
					BiodataCalonMahasiswaPunyaVerifikasiParameter biodataCalonMahasiswaPunyaVerifikasiParameter = (BiodataCalonMahasiswaPunyaVerifikasiParameter) row
							.getAttribute("biodataCalonMahasiswaPunyaVerifikasiParameter");
					if (biodataCalonMahasiswaPunyaVerifikasiParameter == null) {
						continue;
					}
					Checkbox verified = (Checkbox) row.getAttribute("checkbox");
					Textbox keterangan = (Textbox) row.getAttribute("keterangan");
					if (verified != null) {
						biodataCalonMahasiswaPunyaVerifikasiParameter.setVerified(verified.isChecked());
					}
					if (keterangan != null) {
						biodataCalonMahasiswaPunyaVerifikasiParameter.setKeterangan(keterangan.getValue());
					}
					Common.refreshSaveOrUpdate(biodataCalonMahasiswaPunyaVerifikasiParameter);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}
		}
	}
}
