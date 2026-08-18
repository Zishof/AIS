package ais.action.master.sop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.BroadcastHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.helper.ParameterTambahanDisposisiAlurSopListener;
import ais.action.master.sop.helper.RevisiDisposisiSopHelper;
import ais.action.master.sop.helper.SopUtil;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.Konfigurasi;
import ais.database.model.Menu;
import ais.database.model.ParameterTambahan;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.AktorSop;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.sop.DokumenAlurSop;
import ais.database.model.sop.KelompokParameterTambahanAlurSop;
import ais.database.model.sop.ParameterTambahanAlurSop;
import ais.database.model.sop.Sop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class DisposisiSopAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean ubah = true;

	private DisposisiSop disposisiSop;
	private MyToolbarbuttonConfig add;

	private MyDatebox waktu;
	protected Map<Long, LampiranLain> lainMahasiswa;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private Hbox hbFakultasLabel;
	private Hbox hbFakultas;

	private Hbox hbYayasanLabel;
	private Hbox hbYayasan;
	private Combobox sop;
	private Tbmuser tbmuser;
	private EventListener eventListener = null;
	protected ArrayList<Row> parameterRows = null;
	protected HashMap<String, LampiranLain> lampiranLains = null;
	protected ParameterTambahanDisposisiAlurSopListener parameterTambahanListener = null;
	protected AlurSop alurSop = null;
	protected Long ref = null;
	protected List<AlurSop> alurSops;
	protected Radiogroup radiogroup;
	protected Vbox vboxPilihan;
	protected boolean editPilihan;

	private FormSop formSop = null;
	protected LampiranLain lampiranCatatanDisposisi = null;
	private DisposisiAlurSop alurSopAwal;
	private ArrayList<AlurSop> selanjutnya;
	protected List<String> opsiAlurSops;
	protected Hbox hboxAktor;

	private static void closeNativeSessionSafely(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				session.clear();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (session.isOpen()) {
				session.disconnect();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
	}

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
		tbmuser = Common.getCurrentUser();
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

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (searchyayasan != null) {
			searchyayasan.getParent().setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}

		if (hbFakultasLabel != null) {
			hbFakultasLabel.setVisible(
					Common.bolehKonfigurasi("user_fakultas"));
		}

		if (hbFakultas != null) {
			hbFakultas.setVisible(
					Common.bolehKonfigurasi("user_fakultas"));
		}

		if (hbYayasanLabel != null) {
			hbYayasanLabel.setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}

		if (hbYayasan != null) {
			hbYayasan.setVisible(Common.bolehKonfigurasi("user_yayasan", Konfigurasi.TIDAK_AKTIF));
		}

		String[] contents = new String[] { "diajukanOleh.userNama", "mahasiswa.nama", "siswa.nama", "waktu", "aktif",
				"keterangan", "sop.kode", "sop.nama", "sop.versi", "sop.jenisDisposisiSop", "sop.tanggalTerbit",
				"sop.keterangan", "sop.jurusan", "sop.fakultas", "sop.yayasan", "sop.sekolah", "sop.satuanKerja" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(DisposisiSop.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Singkronkan Persetujuan", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				Common.createDefaultTimer(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload("Sinkronisasi Persetujuan Disposisi SOP");
						List<DisposisiSop> disposisiSops = initCriteria(false).addOrder(Order.asc("id"))
								.setMaxResults(5000).list();

						int baris = 0;
						for (DisposisiSop disposisiSop : disposisiSops) {

							String kunci = String.valueOf(disposisiSop);
							Session session = null;
							try {
								session = HibernateUtil.currentNativeSession();
								List<DisposisiAlurSop> disposisiAlurSops = session
										.createCriteria(DisposisiAlurSop.class).add(Restrictions.isNotNull("alurSop"))
										.add(Restrictions.eq("disposisiSop", disposisiSop)).addOrder(Order.asc("id"))
										.list();

								AlurSop alurSop = null;
								boolean adaForm = false;
								DisposisiAlurSop disposisiAlurSopTerakhir = null;
								DisposisiAlurSop disposisiAlurSopSetujui = null;
								for (DisposisiAlurSop disposisiAlurSop : disposisiAlurSops) {
									disposisiAlurSopTerakhir = disposisiAlurSop;

									if (disposisiAlurSop != null && disposisiAlurSop.getAlurSop() != null
											&& disposisiAlurSop.setujui()) {
										disposisiAlurSopSetujui = disposisiAlurSop;
									}

									if (disposisiAlurSopSetujui == null) {
										if (disposisiAlurSop != null && disposisiAlurSop.getAlurSop() != null
												&& disposisiAlurSop.getAlurSop().getJikaProsesDisetujuiMakaSelesai()) {
											disposisiAlurSopSetujui = disposisiAlurSop;
										}
									}

									try {
										if (!disposisiAlurSop.getAlurSop().getFormInputan().isEmpty()) {
											alurSop = disposisiAlurSop.getAlurSop();
											adaForm = true;
										}

									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}

								}

								session.getTransaction().begin();
								DisposisiAlurSopAction.checkAndSave(session, disposisiAlurSopTerakhir,
										disposisiAlurSopSetujui, disposisiSop);
								session.getTransaction().commit();

								if (adaForm && alurSop != null) {
									try {
										FormSop formSop = (FormSop) Class.forName(alurSop.getFormInputan())
												.newInstance();
										String key = formSop.ambilClass().getName();
										GeneralValueObject generalValueObject = null;
										JSONObject o = new JSONObject(disposisiSop.getProperti());
										JSONObject jsonObject = o.isNull(key) ? null : o.getJSONObject(key);
										generalValueObject = (GeneralValueObject) (jsonObject == null
												|| jsonObject.isNull("id") ? formSop.ambilClass().newInstance()
														: GeneralValueObject.ambilData(formSop.ambilClass(),
																(jsonObject.get("id") + ""), true));

										if (generalValueObject != null && generalValueObject.getId() != null
												&& session != null && session.isOpen()) {
											session.refresh(generalValueObject);
											session.getTransaction().begin();
											Common.refreshUpdate(session, generalValueObject);
											session.getTransaction().commit();
											System.out.println("update data " + key + " -> " + generalValueObject);
										}
									} catch (Exception e) {
										ais.common.Common.tampilErrorJikaAdmin(e);
									}
								}
								laporan.catatBerhasil(baris, kunci, "Sinkronisasi berhasil");
								} catch (Exception e) {
									try {
										if (session != null && session.getTransaction() != null
												&& session.getTransaction().isActive()) {
											session.getTransaction().rollback();
										}
									} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiSopAction.java:330");
									}
									ais.common.Common.tampilErrorJikaAdmin(e);
									laporan.catatGagalDetail(baris, kunci, e);
								} finally {
									closeNativeSessionSafely(session);
								}
							baris++;
						}

						laporan.selesaikan(new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								onSearchDefault(null);
							}
						});
					}
				});

			}

		});
		if (button != null) { button.setParent(add.getParent()); }

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiDisposisiSopHelper revisiHelper = new RevisiDisposisiSopHelper(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						});
					}
				});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		if (button != null) { button.setParent(add.getParent()); }
	        FilterLanjutHelper.setup(comp);
}

	class DisposisiSopRenderer extends ais.ui.util.MyRowRenderer {

		/** Field kode/nomor pengajuan yang dipindai di properti JSON (top-level &amp; nested). */
		private final String[] FIELD_KODE_PENGAJUAN = new String[] { "kode", "nomor", "nomorSurat", "noSurat",
				"no_surat", "kodeTransaksi" };

		/**
		 * Ambil kode pengajuan ROBUST dari beberapa sumber: properti DisposisiSop, lalu (fallback)
		 * properti langkah START (disposisiStart) yang selalu diisi kode saat pengajuan dibuat.
		 * Mengatasi kasus kode tak tampil karena struktur properti berbeda antar jenis SOP.
		 */
		private String ambilKodePengajuanRobust(DisposisiSop disposisiSop) {
			if (disposisiSop == null) {
				return null;
			}
			String kode = ambilKodeDariProperti(disposisiSop.getProperti());
			if ((kode == null || kode.trim().isEmpty()) && disposisiSop.getDisposisiStart() != null) {
				kode = ambilKodeDariProperti(disposisiSop.getDisposisiStart().getProperti());
			}
			return kode;
		}

		/**
		 * Pindai properti JSON: cari field kode/nomor di LEVEL ATAS, lalu di SETIAP objek transaksi
		 * bersarang. Kembalikan nilai pertama yang tidak kosong (versi lama hanya membaca key pertama
		 * sehingga kode tak tampil bila key pertama tak memuat kode).
		 */
		private String ambilKodeDariProperti(String propertiJson) {
			try {
				if (propertiJson == null || propertiJson.trim().isEmpty()) {
					return null;
				}
				JSONObject root = new JSONObject(propertiJson);
				for (int i = 0; i < FIELD_KODE_PENGAJUAN.length; i++) {
					String f = FIELD_KODE_PENGAJUAN[i];
					if (!root.isNull(f)) {
						String k = String.valueOf(root.get(f));
						if (k != null && !k.trim().isEmpty()) {
							return k.trim();
						}
					}
				}
				Iterator<String> keys = root.keys();
				while (keys.hasNext()) {
					Object val = root.opt(keys.next());
					if (val instanceof JSONObject) {
						JSONObject obj = (JSONObject) val;
						for (int i = 0; i < FIELD_KODE_PENGAJUAN.length; i++) {
							String f = FIELD_KODE_PENGAJUAN[i];
							if (!obj.isNull(f)) {
								String k = String.valueOf(obj.get(f));
								if (k != null && !k.trim().isEmpty()) {
									return k.trim();
								}
							}
						}
					}
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
			return null;
		}

		@SuppressWarnings({ "deprecation", "unchecked" })
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");

			MyDetail detail = new MyDetail();
			arg0.appendChild(detail);

			// TODO Auto-generated method stub
			final DisposisiSop disposisiSop = (DisposisiSop) arg1;

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			vbox.setWidth("100%");
			Tbmuser tbmuser = disposisiSop.getDiajukanOleh();
			if (tbmuser == null && disposisiSop.getMahasiswa() != null) {
				tbmuser = new Tbmuser(disposisiSop.getMahasiswa());
			} else if (tbmuser == null && disposisiSop.getSiswa() != null) {
				tbmuser = new Tbmuser(disposisiSop.getSiswa());
			}
			try {
				CommonMedia.tampilkanGambarKecil(tbmuser).setParent(vbox);
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			vbox.appendChild(new Label(tbmuser.getUserNama()));
			vbox.appendChild(new Label(disposisiSop.getDisposisiStart() == null ? ""
					: disposisiSop.getDisposisiStart().getAlurSop().getAktor()));

			Vbox a;
			(a = RevisiHelper.createNewRevisi(DisposisiSop.class, disposisiSop, disposisiSop.getSop().getNama()))
					.setParent(arg0);

			new Label(disposisiSop.getSop().getKode()).setParent(a);

			try {
				// Kode pengajuan dicari ROBUST (scan SEMUA key + nested + fallback langkah START) agar
				// tampil utk SEMUA jenis SOP — mis. Penerimaan Barang Investasi yg kode-nya TIDAK ada di
				// key pertama properti DisposisiSop (dulu hanya baca key pertama → kode tak tampil).
				String kode = ambilKodePengajuanRobust(disposisiSop);
				if (kode != null && !kode.trim().isEmpty()) {
					a.appendChild(new MyLabelBold(kode));
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Session session = HibernateUtil.currentSession();
			AlurSop alurSop = (AlurSop) session.createCriteria(AlurSop.class)
					.add(Restrictions.eq("sop", disposisiSop.getSop())).setMaxResults(1).addOrder(Order.asc("id"))
					.add(Restrictions.eq("start", true)).uniqueResult();

			if (alurSop != null) {

				Groupbox groupbox = new Groupbox();
				groupbox.setParent(detail);

				groupbox.appendChild(
						new Caption("Parameter dan Dokumen Pengajuan \"" + disposisiSop.getSop().getNama() + "\""));

				Vbox vbox11 = new Vbox();
				vbox11.setParent(groupbox);
				vbox11.setWidth("100%");

				if (!alurSop.getKelompokParameterTambahanAlurSops().isEmpty()) {

					Grid parameterGrid = new Grid();
					parameterGrid.setMold("paging");
					parameterGrid.setParent(vbox11);
					parameterGrid.setSclass("fgrid");

					Columns columnsparameter = new Columns();
					columnsparameter.setParent(parameterGrid);

					MyColumnConfig columnparameter = new MyColumnConfig("");
					columnparameter.setParent(columnsparameter);
					columnparameter.setWidth("30%");

					columnparameter = new MyColumnConfig("");
					columnparameter.setParent(columnsparameter);

					Rows rowsParameter = new Rows();
					rowsParameter.setParent(parameterGrid);

					for (KelompokParameterTambahanAlurSop kelompokParameterTambahanAlurSop : disposisiSop
							.getDisposisiStart().getAlurSop().getKelompokParameterTambahanAlurSops()) {
						MyFormRow rowParameterTambahan = new MyFormRow();
						rowParameterTambahan.setVisible(false);
						rowParameterTambahan.setParent(rowsParameter);
						ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
						rowParameterTambahan
								.appendChild(new MyLabelStyled(kelompokParameterTambahanAlurSop.getNama() + ""));

						List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
								session.createCriteria(ParameterTambahanAlurSop.class)
										.add(Restrictions.eq("kelompokParameterTambahanAlurSop",
												kelompokParameterTambahanAlurSop))
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanAlurSop",
												"kelompokParameterTambahanAlurSop")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanAlurSop.aktif", true))
										.setProjection(Projections.groupProperty("parameterTambahan.id")),
								ParameterTambahan.class, false);
						Collections.sort(parameterTambahans);

						rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
						if (!parameterTambahans.isEmpty()) {
							for (final ParameterTambahan parameterTambahan : parameterTambahans) {
								String jenis = kelompokParameterTambahanAlurSop.getId() + "->"
										+ parameterTambahan.getId();
								MyFormRow rowParameter = new MyFormRow();
								rowParameter.setValign("top");
								rowParameter.setParent(rowsParameter);
								rowParameter.appendChild(new Label(parameterTambahan.getLabelInputan()
										+ (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));

								String val = "";
								String[] spl = disposisiSop.getDisposisiStart().getParameterTambahanInds().split("\n");
								for (String d : spl) {
									String[] value = d.split("<=>");
									if (value[0].trim().equalsIgnoreCase(jenis)) {
										val = value.length > 1 ? value[1].trim() : "";
									}
								}

								String[] ss = val.split("->");
								if (ss.length > 1) {
									val = ss[1];
								}

								if (parameterTambahan.getHarusMenyertakanLampiran()) {
									Vbox vbox1 = new Vbox();
									rowParameter.appendChild(vbox1);
									vbox1.appendChild(new Label(val));

									Hbox hbox = new Hbox();
									hbox.setWidth("100%");

									LampiranLain.createDownloadUploadFileLain(hbox,
											disposisiSop.getDisposisiStart().getId() == null ? -Common.randLong()
													: disposisiSop.getDisposisiStart().getId(),
											jenis,
											parameterTambahan.getLabelInputan()
													+ (parameterTambahan.getLampiranWajibDiisi() ? " (*)" : " "),
											false, new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

												}
											}, null, false, false, false, Common.getApakahAdmin(), null);
									hbox.setParent(vbox1);

								} else {
									rowParameter.appendChild(new Label(val));
								}
							}
						}

					}
				}

				vbox11.appendChild(new MyLabelStyled("Dokumen"));

				Grid dokumenGrid = new Grid();
				dokumenGrid.setMold("paging");
				dokumenGrid.setParent(vbox11);
				dokumenGrid.setSclass("fgrid");

				Columns columnsdokumen = new Columns();

				columnsdokumen.setParent(dokumenGrid);

				MyColumnConfig columndokumen = new MyColumnConfig("Kode");
				columndokumen.setParent(columnsdokumen);
				columndokumen.setWidth("10%");

				columndokumen = new MyColumnConfig("Nama Dokumen");
				columndokumen.setParent(columnsdokumen);

				Rows rowsdokumen = new Rows();
				rowsdokumen.setParent(dokumenGrid);

				Set<DokumenAlurSop> dokumen = alurSop.getDokumenAlurSops();
				if (!dokumen.isEmpty()) {
					detail.setOpen(true);

					for (DokumenAlurSop dokumenAlurSop : dokumen) {
						if (dokumenAlurSop.getAktif()) {
							MyFormRow rowdokumen = new MyFormRow();
							rowdokumen.setValign("top");
							rowdokumen.setParent(rowsdokumen);

							rowdokumen.appendChild(new Label(dokumenAlurSop.getKode()));

							final LampiranLain lampiranLain = LampiranLain.ambil(disposisiSop.getId(),
									DokumenAlurSop.class.getName() + "_" + dokumenAlurSop.getId());

							if (lampiranLain != null) {

								A aa = new A(dokumenAlurSop.getNama() + (" -> " + lampiranLain.getNama()));
								aa.setParent(rowdokumen);
								aa.setWidth("95%");

								aa.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.display(lampiranLain);
									}
								});

							} else {
								rowdokumen.appendChild(new Label(dokumenAlurSop.getNama()));
							}

						}
					}

				}

			}

			Label l;
			(l = new Label(disposisiSop.getSop().getJenisSop().getNama())).setParent(arg0);
			l.setStyle("background-color:" + disposisiSop.getSop().getJenisSop().getWarna() + ";color:"
					+ disposisiSop.getSop().getJenisSop().getWarnatext() + ";");
			new Label(Common.dateFormat5.get().format(disposisiSop.getWaktu())).setParent(arg0);

			A aa;
			(aa = new A()).setParent(arg0);
			aa.setStyle("font-size:9px;");

			UIClassHelper.applyReadMore(aa, "SOP " + disposisiSop.getKeterangan() + " ("
					+ disposisiSop.getSop().getNama() + ")");

			aa.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					TampilanAlurSopAction.prosess(disposisiSop.getId(), null, null, true, arg0.getTarget());
				}
			});

			DisposisiAlurSop alurSopTerakhir = (DisposisiAlurSop) session.createCriteria(DisposisiAlurSop.class)
					.add(Restrictions.isNotNull("alurSop")).add(Restrictions.eq("disposisiSop", disposisiSop))
					.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
			new Label(alurSopTerakhir == null || alurSopTerakhir.getAlurSop() == null ? ""
					: alurSopTerakhir.getAlurSop().getKode() + " " + alurSopTerakhir.getAlurSop().getNama())
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(disposisiSop.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					disposisiSop.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(disposisiSop);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(disposisiSop);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus atau membatalkan pengajuan ini? Perlu diperhatikan bahwa tindakan ini bersifat permanen dan pengajuan yang telah dihapus tidak dapat dikembalikan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											disposisiSop.hapus();

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.showFormat(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) periksa data lain yang masih terkait dengan data ini; (2) hapus atau lepaskan keterkaitan tersebut terlebih dahulu; (3) hubungi admin apabila memerlukan bantuan.",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);

		}

	}

	public static void onAddExternal(EventListener eventListener, DisposisiSop disposisiSop, boolean ubah)
			throws Exception {
		DisposisiSopAction disposisiSopAction = new DisposisiSopAction();
		disposisiSopAction.eventListener = eventListener;
		disposisiSopAction.addWindow = new MyWindow();
		disposisiSopAction.ubah = ubah;
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(disposisiSopAction.addWindow);
		disposisiSopAction.addWindow.setHeight("95%");
		disposisiSopAction.addWindow.setWidth("850px");

		disposisiSopAction.init(disposisiSop);

		disposisiSopAction.addWindow.setVisible(true);
		disposisiSopAction.addWindow.setClosable(true);
		disposisiSopAction.addWindow.onModal();

	}

	public void onAdd(Event event) throws Exception {
		init(new DisposisiSop());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		disposisiSop = (DisposisiSop) obj;
		init(disposisiSop);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static Criterion createCriterionSop(Tbmuser tbmuser) {
		Criterion criterion = Restrictions.eq("aktif", true);
		if (AktorSop.bolehMelihatSemuaSop(tbmuser)) {
			return criterion;
		}
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			criterion = Restrictions.and(criterion, Restrictions.or(
					Restrictions.eq("diperuntukkan", GrupChecklistPenilaianUmum.UNTUK_UMUM),
					Restrictions.or(Restrictions.eq("diperuntukkan", GrupChecklistPenilaianUmum.UNTUK_MAHASISWA),
							Restrictions.or(
									Restrictions.eq("usernamePengguna", "," + tbmuser.getMahasiswa().getNim() + ","),
									Restrictions.ilike("jenisPengguna", "," + Tbmrole.MAHASISWA + ",")))));

			if (tbmuser != null && tbmuser.ambilFakultas() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("fakultas", tbmuser.ambilFakultas()), Restrictions.isNull("fakultas")));
			}
			if (tbmuser != null && tbmuser.ambilJurusan() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("jurusan", tbmuser.ambilJurusan()), Restrictions.isNull("jurusan")));
			}

		} else if (tbmuser != null && tbmuser.getSiswa() != null) {
			criterion = Restrictions
					.and(criterion,
							Restrictions
									.or(Restrictions.eq("diperuntukkan", GrupChecklistPenilaianUmum.UNTUK_UMUM),
											Restrictions
													.or(Restrictions.eq("diperuntukkan",
															GrupChecklistPenilaianUmum.UNTUK_SISWA),
															Restrictions.or(
																	Restrictions.eq("usernamePengguna",
																			"," + tbmuser.getSiswa()
																					.getNomorIndukNasional() + ","),
																	Restrictions.ilike("jenisPengguna",
																			"," + Tbmrole.SISWA + ",")))));

			if (tbmuser != null && tbmuser.ambilSekolah() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("sekolah", tbmuser.ambilSekolah()), Restrictions.isNull("sekolah")));
			}
			if (tbmuser != null && tbmuser.ambilYayasan() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("yayasan", tbmuser.ambilYayasan()), Restrictions.isNull("yayasan")));
			}

		} else if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			criterion = Restrictions.and(criterion,
					Restrictions.or(Restrictions.eq("diperuntukkan", GrupChecklistPenilaianUmum.UNTUK_UMUM),
							Restrictions.or(Restrictions.eq("diperuntukkan", GrupChecklistPenilaianUmum.UNTUK_DOSEN),
									Restrictions.or(
											Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
													MatchMode.ANYWHERE),
											Restrictions.ilike("jenisPengguna", "," + Tbmrole.DOSEN + ",")))));

			if (tbmuser != null && tbmuser.ambilFakultas() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("fakultas", tbmuser.ambilFakultas()), Restrictions.isNull("fakultas")));
			}
			if (tbmuser != null && tbmuser.ambilJurusan() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("jurusan", tbmuser.ambilJurusan()), Restrictions.isNull("jurusan")));
			}

		} else if (tbmuser != null && tbmuser.ambilGuru() != null) {
			criterion = Restrictions.and(criterion,
					Restrictions.or(Restrictions.eq("diperuntukkan", GrupChecklistPenilaianUmum.UNTUK_UMUM),
							Restrictions.or(Restrictions.eq("diperuntukkan", GrupChecklistPenilaianUmum.UNTUK_GURU),
									Restrictions.or(
											Restrictions.ilike("usernamePengguna", "," + tbmuser.getUserId() + ",",
													MatchMode.ANYWHERE),
											Restrictions.ilike("jenisPengguna", "," + Tbmrole.GURU + ",")))));

			if (tbmuser != null && tbmuser.ambilSekolah() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("sekolah", tbmuser.ambilSekolah()), Restrictions.isNull("sekolah")));
			}
			if (tbmuser != null && tbmuser.ambilYayasan() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("yayasan", tbmuser.ambilYayasan()), Restrictions.isNull("yayasan")));
			}

		} else if (tbmuser != null && tbmuser.hakAkses() != null) {

			criterion = Restrictions.and(criterion,
					Restrictions.and(Restrictions.eq("diperuntukkan", GrupChecklistPenilaianUmum.UNTUK_UMUM),
							Restrictions.or(
									Restrictions.or(Restrictions.ilike("usernamePengguna",
											"," + tbmuser.getUserId() + ",", MatchMode.ANYWHERE),
											Restrictions.eq("usernamePengguna", "")),
									Restrictions.or(
											Restrictions.ilike("jenisPengguna",
													"," + tbmuser.hakAkses().getRoleId() + ",", MatchMode.ANYWHERE),
											Restrictions.eq("jenisPengguna", "")))));

			if (tbmuser != null && tbmuser.ambilSekolah() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("sekolah", tbmuser.ambilSekolah()), Restrictions.isNull("sekolah")));
			}
			if (tbmuser != null && tbmuser.ambilYayasan() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("yayasan", tbmuser.ambilYayasan()), Restrictions.isNull("yayasan")));
			}
			if (tbmuser != null && tbmuser.ambilFakultas() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("fakultas", tbmuser.ambilFakultas()), Restrictions.isNull("fakultas")));
			}
			if (tbmuser != null && tbmuser.ambilJurusan() != null) {
				criterion = Restrictions.and(criterion, Restrictions
						.or(Restrictions.eq("jurusan", tbmuser.ambilJurusan()), Restrictions.isNull("jurusan")));
			}
			if (tbmuser != null && tbmuser.ambilSatuanKerja() != null
					&& !tbmuser.hakAkses().getMelihatDataSatkerLain()) {

				criterion = Restrictions.and(criterion,
						Restrictions.or(Restrictions.eq("satuanKerja", tbmuser.ambilSatuanKerja()),
								Restrictions.isNull("satuanKerja")));
			}
		}

		return criterion;
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void init(final DisposisiSop disposisiSop) throws Exception {
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		addWindow.setTitle("Pengajuan SOP (Workflow)");
		addWindow.setWidth("1100px");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("SOP (Workflow) *"));
		row.appendChild(sop = new Combobox());
		sop.setWidth("90%");

		tbmuser = Common.getCurrentUser();

		List<Long> sops = HibernateUtil.currentSession().createCriteria(AlurSop.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("start", true)).setProjection(Projections.groupProperty("sop.id"))
				.createAlias("aktorSop", "aktorSop").add(AktorSop.buatCriterion(tbmuser)).list();

		System.out.println("sops -> " + sops);

		Common.insertComboDanSemua(sop, new String[] { "nama" }, "keterangan", Sop.class, "== Pilih SOP (Workflow) ==",
				Restrictions.and(sops.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", sops),
						DisposisiSopAction.createCriterionSop(tbmuser)));
		Common.selectComboItem(true, sop, disposisiSop.getSop());
		sop.setReadonly(true);

		final MyFormRow rowFile = new MyFormRow();

		rowFile.setParent(rows);

		final MyToolbarbuttonConfig save1 = new MyToolbarbuttonConfig("Simpan / Ajukan", "/img/svg/save-2-fill.svg");

		final EventListener eventListener1 = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowFile);
				rowFile.appendChild(new ais.ui.util.MyLabelConfig("Lampiran/Informasi SOP"));
				rowFile.setVisible(false);
				Sop s = (Sop) (sop.getSelectedItem() == null ? null : sop.getSelectedItem().getValue());
				if (s != null) {

					FileFotoLain fileFotoLain = FileFotoLain.ambil(false, s.getId(), Sop.class.getName(),
							LampiranLain.class);
					if (fileFotoLain != null && fileFotoLain.getId() != null) {
						rowFile.setVisible(fileFotoLain != null);
						Vbox myvbox = new Vbox();
						myvbox.setParent(rowFile);

						Hbox hbox = new Hbox();
						hbox.setParent(myvbox);
						LampiranLain.createDownloadUploadFileLain(hbox, s.getId(), Sop.class.getName(),
								"Lampiran/Informasi SOP", false, null, null, false, false, false, Common.getApakahAdmin());
					}
				}
			}
		};
		sop.addEventListener("onChange", eventListener1);
		eventListener1.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Pengajuan *"));
		row.appendChild(waktu = new MyDatebox(disposisiSop.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setReadonly(true);
		waktu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Pengajuan"));
		row.appendChild(keterangan = new Textbox(disposisiSop.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		lampiranCatatanDisposisi = null;
		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("tampilkan_lampiran_catatan_disposisi"));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, null, "Lampiran Catatan Disposisi",
				"Lampiran Catatan Pengajuan", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiranCatatanDisposisi = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		if (row.isVisible()) {
			Common.initKeterangan(rows,
					"Jika file lampiran catatan pengajuan lebih dari satu file, zip dulu semua file tersebut");
		}
		lainMahasiswa = new HashMap<Long, LampiranLain>();

		MyFormRow rowLampiran = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
		rowLampiran.setParent(rows);

		final Grid gridLampiran = new Grid();
		gridLampiran.setSclass("fgrid");
		gridLampiran.setParent(rowLampiran);

		columns = new Columns();
		columns.setParent(gridLampiran);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		final Rows rowsLampiran = new Rows();
		rowsLampiran.setParent(gridLampiran);

		ref = DisposisiSopAction.this.disposisiSop.getId() == null ? -Common.randLong()
				: DisposisiSopAction.this.disposisiSop.getId();

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.clear(rowsLampiran);

				Sop s = (Sop) (sop.getSelectedItem() == null ? null : sop.getSelectedItem().getValue());
				if (s != null) {

					Session session = HibernateUtil.currentSession();

					alurSop = (AlurSop) ConstantValues.simpleObject(
							session.createCriteria(AlurSop.class).add(Restrictions.eq("sop", s)).setMaxResults(1)
									.addOrder(Order.asc("id")).add(Restrictions.eq("start", true)),
							AlurSop.class);

					if (alurSop == null || alurSop.getId() == null) {
						MyMessageboxConfig.show("Mohon maaf, Alur Awal (start) untuk SOP ini tidak ditemukan sehingga pengajuan belum dapat diproses. Langkah yang dapat dilakukan: (1) periksa kembali konfigurasi alur SOP yang dipilih; (2) pastikan terdapat alur bertanda Awal (start); (3) hubungi admin apabila memerlukan bantuan.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					DisposisiSopAction.reloadDataMenu(rowsLampiran, new JSONArray(alurSop.getHalamanMenu()),
							eventListener1);

					alurSops = alurSop.ambilAlurSetelahnya();
					opsiAlurSops = alurSop.ambilOpsiAlurSetelahnya();

					MyFormRow rowLampiran = new MyFormRow();
					ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
					rowLampiran.appendChild(new MyLabelStyled("Alur Disposisi Berikutnya"));
					rowLampiran.setParent(rowsLampiran);

					if (alurSops.isEmpty()) {
						rowLampiran = new MyFormRow();
						rowLampiran.setParent(rowsLampiran);
						ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
						rowLampiran.appendChild(
								new ais.ui.util.MyLabelAgakKecilBoldMerah("Tidak ada disposisi berikutnya"));
					}

					rowLampiran = new MyFormRow();
					rowLampiran.setVisible(!alurSops.isEmpty());
					rowLampiran.setParent(rowsLampiran);
					rowLampiran.appendChild(new ais.ui.util.MyLabelConfig("Proses SOP *"));

					editPilihan = DisposisiSopAction.this.disposisiSop.getId() == null;

					alurSopAwal = null;
					if (DisposisiSopAction.this.disposisiSop.getId() != null) {
						alurSopAwal = ((DisposisiAlurSop) session.createCriteria(DisposisiAlurSop.class)
								.add(Restrictions.isNotNull("alurSop"))
								.add(Restrictions.eq("disposisiSop", DisposisiSopAction.this.disposisiSop))
								.add(Restrictions.eq("alurSop", alurSop)).setMaxResults(1).uniqueResult());

						if (alurSopAwal != null && alurSopAwal.getSetelahnya() == null) {
							editPilihan = true;
						}
					}

					hboxAktor = new Hbox();

					if (editPilihan) {
						List<Long> ids = new ArrayList<Long>();

						if (alurSopAwal != null && alurSopAwal.getAlurSop() != null
								&& alurSopAwal.getAlurSop().getId() != null) {
							ids.add(alurSopAwal.getAlurSop().getId());
						}


						// Pre-select rute berikutnya yang SUDAH dipilih untuk langkah ini. KHUSUS
						// langkah pertama/pengaju: rute tersimpan di "sebelumnya" langkah berikutnya
						// (bukan di "setelahnya" langkah ini) -> perlu query terpisah, sama seperti
						// idsSelected di DisposisiAlurSopAction. Admin edit langkah yg sudah dirutekan -> kunci.
						boolean adaRuteBerikutnya = false;
						if (alurSopAwal != null) {
							try {
								List<?> nextAlurIds = session.createCriteria(DisposisiAlurSop.class)
									.add(Restrictions.isNotNull("alurSop"))
									.add(Restrictions.eq("sebelumnya", alurSopAwal))
									.setProjection(org.hibernate.criterion.Projections.groupProperty("alurSop.id")).list();
								if (nextAlurIds != null) {
									for (Object nid : nextAlurIds) {
										if (nid != null) {
											ids.add((Long) nid);
											adaRuteBerikutnya = true;
										}
									}
								}
							} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiSopAction.java:1163");
							}
						}
						final boolean kunciProsesAdmin = Common.getApakahAdmin()
								&& DisposisiSopAction.this.disposisiSop.getId() != null && adaRuteBerikutnya;

						if (alurSop.getAlurSetelahnyaBerupaPilihan()) {
							radiogroup = new Radiogroup();
							radiogroup.setOrient("vertical");
							rowLampiran.appendChild(radiogroup);
							int i = 0;
							for (AlurSop alurSop2 : alurSops) {

								String opsi = opsiAlurSops.size() > i ? opsiAlurSops.get(i) : "";
								if (opsi.trim().isEmpty()) {
									opsi = alurSop2.getOpsi();
								}

								if (!opsi.trim().isEmpty()) {
									opsi = opsi + " - ";
								}
								i++;

								boolean pilih = ids.contains(alurSop2.getId());

								Radio radio = new Radio(opsi + alurSop2.getAktor() + " - " + alurSop2.getNama());
								radio.setSelected(pilih);
								radio.setDisabled(kunciProsesAdmin);
								radio.setAttribute("alurSop", alurSop2);
								radiogroup.appendChild(radio);

								if (pilih) {
									radiogroup.setSelectedItem(radio);
								}

								final String usernames = alurSop2.getKhususUsername();
								final String jenisPengguna = alurSop2.getAktorSop() == null ? ""
										: alurSop2.getAktorSop().getJenisPengguna();
								radio.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										SopUtil.resetAktor(hboxAktor);
										hboxAktor.getParent().setVisible(false);
										if (((Radio) arg0.getTarget()).isChecked()) {
											AlurSop alur = (AlurSop) ((Radio) arg0.getTarget()).getAttribute("alurSop");
											SopUtil.tampilAktor(null, usernames, jenisPengguna, disposisiSop, alur,
													hboxAktor);
											hboxAktor.getParent().setVisible(true);

										}
									}
								});
							}

						} else {
							vboxPilihan = new Vbox();
							rowLampiran.appendChild(vboxPilihan);
							int i = 0;
							for (AlurSop alurSop2 : alurSops) {

								String opsi = opsiAlurSops.size() > i ? opsiAlurSops.get(i) : "";
								if (opsi.trim().isEmpty()) {
									opsi = alurSop2.getOpsi();
								}

								if (!opsi.trim().isEmpty()) {
									opsi = opsi + " - ";
								}
								i++;

								Checkbox checkbox = new Checkbox(
										opsi + alurSop2.getAktor() + " - " + alurSop2.getNama());
								checkbox.setChecked(ids.contains(alurSop2.getId()));
								checkbox.setDisabled(kunciProsesAdmin);
								checkbox.setAttribute("alurSop", alurSop2);
								vboxPilihan.appendChild(checkbox);

								checkbox.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										// resetAktor (bukan sekadar clear): attribute "usernamePengguna"
										// kini DIGABUNG lintas pemanggilan tampilAktor agar multi-centang
										// menyimpan SEMUA penerima — reset eksplisit mencegah sisa lama.
										SopUtil.resetAktor(hboxAktor);
										hboxAktor.getParent().setVisible(false);

										List<Component> components = vboxPilihan.getChildren();
										for (Component component : components) {
											if (((Checkbox) component).isChecked()) {
												AlurSop alur = (AlurSop) component.getAttribute("alurSop");

												SopUtil.tampilAktor(null, alur.getKhususUsername(),
														alur.getAktorSop() != null
																? alur.getAktorSop().getJenisPengguna()
																: "",
														disposisiSop, alur, hboxAktor);

												hboxAktor.getParent().setVisible(true);

											}
										}

									}
								});
							}
						}
					} else {
						Vbox vbox = new Vbox();
						rowLampiran.appendChild(vbox);
						for (AlurSop alurSop2 : alurSops) {

							vbox.appendChild(new Label(alurSop2.getAktor() + " - " + alurSop2.getNama()));
						}
					}

					rowLampiran = new MyFormRow();
					rowLampiran.setVisible(false);
					rowLampiran.setParent(rowsLampiran);
					rowLampiran.appendChild(new ais.ui.util.MyLabelConfig("Aktor SOP *"));
					rowLampiran.appendChild(hboxAktor);

					parameterRows = new ArrayList<Row>();
					lampiranLains = new HashMap<String, LampiranLain>();

					DisposisiAlurSop disposisiAlurSop = DisposisiSopAction.this.disposisiSop.getId() == null
							? new DisposisiAlurSop()
							: DisposisiSopAction.this.disposisiSop.getDisposisiStart();

					if (disposisiAlurSop == null) {
						disposisiAlurSop = DisposisiSopAction.this.disposisiSop.getId() == null ? new DisposisiAlurSop()
								: ((DisposisiAlurSop) session.createCriteria(DisposisiAlurSop.class)
										.add(Restrictions.isNotNull("alurSop"))
										.add(Restrictions.eq("disposisiSop", DisposisiSopAction.this.disposisiSop))
										.add(Restrictions.eq("alurSop", alurSop)).setMaxResults(1).uniqueResult());
					}

					if (disposisiAlurSop == null) {
						disposisiAlurSop = new DisposisiAlurSop();
					}

					disposisiAlurSop.setAlurSop(alurSop);

					parameterTambahanListener = new ParameterTambahanDisposisiAlurSopListener(disposisiAlurSop,
							parameterRows, lampiranLains, rowsLampiran, alurSop.getBekukanFormTampilan() && !Common.getApakahAdmin());
					parameterTambahanListener.onEvent(null);

					formSop = null;
					if (!alurSop.getFormInputan().isEmpty()) {
						try {
							formSop = (FormSop) Class.forName(alurSop.getFormInputan()).newInstance();

							rowLampiran = new MyFormRow();
							ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
							rowLampiran.appendChild(new MyLabelStyled(formSop.istilah()));
							rowLampiran.setParent(rowsLampiran);
							GeneralValueObject generalValueObject = null;
							String key = formSop.ambilClass().getName();
							JSONObject o = new JSONObject(DisposisiSopAction.this.disposisiSop.getProperti());
							JSONObject jsonObject = o.isNull(key) ? null : o.getJSONObject(key);

							if (disposisiSop != null && disposisiSop.getId() != null) {
								generalValueObject = (GeneralValueObject) (jsonObject == null || jsonObject.isNull("id")
										? formSop.ambilClass().newInstance()
										: GeneralValueObject.ambilData(formSop.ambilClass(),
												(jsonObject.get("id") + ""), true));
							} else {
								generalValueObject = (GeneralValueObject) formSop.ambilClass().newInstance();
							}

							if ((generalValueObject == null || generalValueObject.getId() == null)
									&& disposisiSop != null && disposisiSop.getId() != null) {
								try {
									generalValueObject = disposisiSop.ambil(session, formSop);
								} catch (Exception e) {
									ais.common.Common.tampilErrorJikaAdmin(e);
								}
							}

//							System.out.println("generalValueObject -> " + generalValueObject);

							if (generalValueObject == null) {
								generalValueObject = (GeneralValueObject) formSop.ambilClass().newInstance();
							}

							rowLampiran = new MyFormRow();
							ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
							rowLampiran.appendChild(formSop.form(generalValueObject,
									DisposisiSopAction.this.disposisiSop, save1, null));
							rowLampiran.setParent(rowsLampiran);

							if (alurSop.getBekukanFormTampilan() && !Common.getApakahAdmin()) {
								Common.freeze(rowLampiran, true);
							}

						} catch (Exception e) {
							ais.common.Common.tampilErrorJikaAdmin(e);
						}
					}

					if (formSop != null && (!alurSop.getBekukanFormTampilan() || Common.getApakahAdmin())) {
						save1.setLabel(
								Common.getBahasaConfig("Simpan / Ajukan sebagai") + " \"" + formSop.istilah() + "\"");
					}

					Set<DokumenAlurSop> dokumen = alurSop.getDokumenAlurSops();

					if (!dokumen.isEmpty()) {

						rowLampiran = new MyFormRow();
						ais.ui.util.ZkCompat.setSpans(rowLampiran, "2");
						rowLampiran.appendChild(new MyLabelStyled("Dokumen"));
						rowLampiran.setParent(rowsLampiran);

						for (final DokumenAlurSop dokumenAlurSop : dokumen) {
							if (dokumenAlurSop.getAktif()) {
								rowLampiran = new MyFormRow();
								rowLampiran.appendChild(new Label(dokumenAlurSop.getNama()));
								rowLampiran.setParent(rowsLampiran);

								Hbox hbox = new Hbox();
								LampiranLain.createDownloadUploadFileLain(hbox, ref,
										DokumenAlurSop.class.getName() + "_" + dokumenAlurSop.getId(),
										dokumenAlurSop.getNama(), false, new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												LampiranLain lain = (LampiranLain) arg0.getData();
												lainMahasiswa.put(dokumenAlurSop.getId(), lain);
											}
										});
								hbox.setParent(rowLampiran);
							}
						}

						Common.initKeterangan(rowsLampiran,
								"Jika file dokumen lebih dari satu file, zip dulu semua file tersebut");
					}

				}

			}
		};

		sop.addEventListener("onChange", eventListener);
		eventListener.onEvent(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		if (!ubah) {
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.setVisible(false);
				}
			});
			cancel.setParent(toolbar);

			Common.freezeGanti(grid, true);

		} else {
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					addWindow.setVisible(false);
				}
			});
			cancel.setParent(toolbar);

			save1.setTooltiptext("Simpan");
			save1.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (!check()) {
						return;
					}

					if (formSop != null && formSop.onSave(event)) {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								if (onSave(arg0)) {
									onSearchDefault(null);
									addWindow.setVisible(false);

									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											if (DisposisiSopAction.this.eventListener != null) {
												DisposisiSopAction.this.eventListener.onEvent(arg0);
											}
										}
									});
								}

							}
						});

					} else if (formSop == null) {
						if (onSave(event)) {
							onSearchDefault(null);
							addWindow.setVisible(false);

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									if (DisposisiSopAction.this.eventListener != null) {
										DisposisiSopAction.this.eventListener.onEvent(arg0);
									}
								}
							});
						}

					}

				}
			});
			save1.setParent(toolbar);
		}

	}

	@SuppressWarnings("unchecked")
	public boolean check() throws Exception {
		Sop s = (Sop) (sop.getSelectedItem() == null ? null : sop.getSelectedItem().getValue());
		if (s == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu memilih SOP (Workflow). Langkah yang dapat dilakukan: (1) buka pilihan SOP (Workflow); (2) pilih SOP yang sesuai; (3) lanjutkan menyimpan pengajuan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (waktu.getValue() == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Waktu Pengajuan SOP. Langkah yang dapat dilakukan: (1) klik kolom Waktu Pengajuan; (2) pilih tanggal dan waktu pengajuan; (3) lanjutkan menyimpan pengajuan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		selanjutnya = new ArrayList<AlurSop>();
		if (editPilihan && !alurSops.isEmpty()) {

			if (radiogroup != null) {
				if (radiogroup.getSelectedItem() != null
						&& radiogroup.getSelectedItem().getAttribute("alurSop") != null) {
					selanjutnya.add((AlurSop) radiogroup.getSelectedItem().getAttribute("alurSop"));
				}
			} else if (vboxPilihan != null) {
				List<Component> components = vboxPilihan.getChildren();
				for (Component component : components) {
					if (((Checkbox) component).isChecked()) {
						selanjutnya.add((AlurSop) component.getAttribute("alurSop"));
					}
				}
			}

		}

		System.out.println("selanjutnya -> " + selanjutnya);
		if (alurSop == null || !alurSop.getAlurSetelahnyaTidakWajib()) {
			if (!alurSops.isEmpty()) {
				if (radiogroup != null && !radiogroup.getChildren().isEmpty()) {
					boolean ada = false;
					List<Component> components = radiogroup.getChildren();
					for (Component component : components) {
						try {
							Radio radio = (Radio) component;
							if (radio.isChecked()) {
								ada = true;
								break;
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiSopAction.java:1544");
							// TODO: handle exception
						}
					}

					if (!ada) {
						MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu memilih Disposisi selanjutnya. Langkah yang dapat dilakukan: (1) periksa daftar pilihan disposisi yang tersedia; (2) pilih tujuan disposisi berikutnya; (3) lanjutkan menyimpan pengajuan.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return false;
					}
				}

				if (vboxPilihan != null && !vboxPilihan.getChildren().isEmpty()) {
					boolean ada = false;
					List<Component> components = vboxPilihan.getChildren();
					for (Component component : components) {
						try {
							Checkbox radio = (Checkbox) component;
							if (radio.isChecked()) {
								ada = true;
								break;
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiSopAction.java:1566");
							// TODO: handle exception
						}
					}

					if (!ada) {
						MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu memilih Disposisi selanjutnya. Langkah yang dapat dilakukan: (1) periksa daftar pilihan disposisi yang tersedia; (2) pilih tujuan disposisi berikutnya; (3) lanjutkan menyimpan pengajuan.", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return false;
					}
				}
			}
		}
		if (parameterTambahanListener != null && !parameterTambahanListener.validate()) {
			return false;
		}
		tbmuser = Common.getCurrentUser();
		if (alurSop != null && ref != null) {
			Set<DokumenAlurSop> dokumen = alurSop.getDokumenAlurSops();
			for (DokumenAlurSop dokumenAlurSop : dokumen) {

				if (dokumenAlurSop.getWajib()) {
					LampiranLain fileFotoLain = (LampiranLain) FileFotoLain.ambil(false, ref,
							DokumenAlurSop.class.getName() + "_" + dokumenAlurSop.getId(), LampiranLain.class);

					if (fileFotoLain == null) {
						MyMessageboxConfig.showFormat(
								"Mohon Bapak/Ibu terlebih dahulu mengunggah dokumen \"{V1}\" yang bersifat wajib. Langkah yang dapat dilakukan: (1) klik tombol unggah pada dokumen tersebut; (2) pilih berkas dokumen yang sesuai; (3) lanjutkan menyimpan pengajuan.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, dokumenAlurSop.getNama());
						return false;
					}
				}

			}
		}

		if (alurSop == null) {
			MyMessageboxConfig.show("Mohon maaf, Alur Awal (start) untuk SOP ini tidak ditemukan sehingga pengajuan belum dapat diproses. Langkah yang dapat dilakukan: (1) periksa kembali konfigurasi alur SOP yang dipilih; (2) pastikan terdapat alur bertanda Awal (start); (3) hubungi admin apabila memerlukan bantuan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (alurSop != null && alurSop.getCatatanWajibDiisi() && keterangan.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Catatan Pengajuan. Langkah yang dapat dilakukan: (1) klik kolom Catatan Pengajuan; (2) isikan catatan secara jelas; (3) lanjutkan menyimpan pengajuan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (Common.bolehKonfigurasi("tampilkan_lampiran_catatan_disposisi")) {
			if (alurSop != null && alurSop.getLampiranCatatanWajibDiisi()) {

				if (lampiranCatatanDisposisi == null) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengunggah Lampiran Catatan. Langkah yang dapat dilakukan: (1) klik tombol unggah Lampiran Catatan; (2) pilih berkas lampiran yang sesuai; (3) lanjutkan menyimpan pengajuan.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return false;
				}

			}
		}
		return true;
	}

	@SuppressWarnings({})
	public boolean onSave(Event event) throws Exception {

		if (!check()) {
			return false;
		}
		boolean baru = false;

		Session sessionSimpan = null;
		try {
			sessionSimpan = HibernateUtil.currentNativeSession();
			if (disposisiSop != null && disposisiSop.getId() != null) {
				disposisiSop = (DisposisiSop) sessionSimpan.load(DisposisiSop.class, disposisiSop.getId());
			} else {
				baru = true;
				disposisiSop.setDiajukanOleh(tbmuser);
				disposisiSop.setMahasiswa(tbmuser.getMahasiswa());
				disposisiSop.setSiswa(tbmuser.getSiswa());
			}
			Sop s = (Sop) (sop.getSelectedItem() == null ? null : sop.getSelectedItem().getValue());
			disposisiSop.setSop(s);
			disposisiSop.setWaktu(waktu.getValue());
			disposisiSop.setKeterangan(keterangan.getValue());

			sessionSimpan.getTransaction().begin();
			Common.refreshSaveOrUpdate(sessionSimpan, disposisiSop);
			sessionSimpan.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (sessionSimpan != null && sessionSimpan.getTransaction() != null
						&& sessionSimpan.getTransaction().isActive()) {
					sessionSimpan.getTransaction().rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiSopAction.java:1661");
			}
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			closeNativeSessionSafely(sessionSimpan);
		}

		Session sessionAlurAwal = null;
		try {
			sessionAlurAwal = HibernateUtil.currentNativeSession();
			alurSopAwal = ((DisposisiAlurSop) sessionAlurAwal.createCriteria(DisposisiAlurSop.class)
					.add(Restrictions.isNotNull("alurSop")).add(Restrictions.eq("disposisiSop", disposisiSop))
					.addOrder(Order.asc("id"))
					.add(Restrictions.eq("alurSop", alurSop)).setMaxResults(1).uniqueResult());
			// Tangkap identitas ASLI langkah awal (sebelum diubah) agar bisa dipertahankan saat admin menyunting.
			boolean alurAwalBaru = (alurSopAwal == null);
			ais.database.model.Tbmuser pengajuAsliAwal = alurAwalBaru ? null : alurSopAwal.getDiajukanOleh();
			String usernamePenggunaAsliAwal = alurAwalBaru ? null : alurSopAwal.getUsernamePengguna();
			if (alurSopAwal == null) {
				alurSopAwal = new DisposisiAlurSop(hboxAktor.getAttribute("usernamePengguna"));
				alurSopAwal.setAlurSop(alurSop);
				alurSopAwal.setDisposisiSop(disposisiSop);
			}
			// Bila ADMIN (Common.getApakahAdmin) menyunting langkah awal yang SUDAH ADA, JANGAN ganti aktor/
			// pengaju dengan akun admin — pertahankan identitas sesuai alur sebelumnya. Langkah baru tetap normal.
			boolean adminEditAwal = Common.getApakahAdmin() && !alurAwalBaru;
			if (!adminEditAwal) {
				alurSopAwal.setUsernamePengguna(hboxAktor.getAttribute("usernamePengguna") == null ? ""
						: hboxAktor.getAttribute("usernamePengguna").toString());
			}
			parameterTambahanListener.onSave(alurSopAwal);

			alurSopAwal.setKeterangan(keterangan.getValue());

			if (adminEditAwal) {
				// Kembalikan identitas asli (aktor & pengaju) agar tidak tersimpan sebagai akun admin.
				if (usernamePenggunaAsliAwal != null) alurSopAwal.setUsernamePengguna(usernamePenggunaAsliAwal);
				if (pengajuAsliAwal != null) alurSopAwal.setDiajukanOleh(pengajuAsliAwal);
			}

			sessionAlurAwal.getTransaction().begin();
			Common.refreshSaveOrUpdate(sessionAlurAwal, alurSopAwal);
			sessionAlurAwal.getTransaction().commit();
		} catch (Exception e) {
			try {
				if (sessionAlurAwal != null && sessionAlurAwal.getTransaction() != null
						&& sessionAlurAwal.getTransaction().isActive()) {
					sessionAlurAwal.getTransaction().rollback();
				}
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/sop/DisposisiSopAction.java:1710");
			}
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			closeNativeSessionSafely(sessionAlurAwal);
		}

		Common.createDefaultTimerNoBusy((new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				BroadcastHelper.kirimEmailDisposisi(alurSopAwal);
			}
		}));

		if (baru) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// PERBAIKAN PERFORMA: cetakDisposisi (PDF + email) sebelumnya dijalankan SINKRON
					// di thread event ZK sehingga klik "Tindak Lanjuti SOP" membekukan halaman hingga
					// belasan menit. Step disposisi sudah tersimpan sebelum titik ini, sehingga
					// notifikasi dijalankan di BACKGROUND THREAD agar UI langsung responsif (cetakDisposisi
					// membuka sesi Hibernate sendiri & host-URL memakai cache → aman di luar konteks ZK).
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								TampilanAlurSopAction.cetakDisposisi(disposisiSop, true);
							} catch (Throwable t) {
								t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/action/master/sop/DisposisiSopAction.java:1741");
							}
						}
					}).start();
				}
			}, "", false, 2500);
		}

		Common.createDefaultTimerNoBusy((new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Session session = HibernateUtil.currentSession();

				DisposisiSop disposisiSop = (DisposisiSop) session.createCriteria(DisposisiSop.class)
						.add(Restrictions.idEq(DisposisiSopAction.this.disposisiSop.getId())).uniqueResult();

				if (!alurSop.getBekukanFormTampilan()) {
					JSONObject jsonObjectData = new JSONObject(disposisiSop.getProperti());
					try {

						ais.database.model.sop.DataSop temp = formSop == null ? null : formSop.ambil();

						if (temp != null && temp.getId() != null) {

							ais.database.model.sop.DataSop generalValueObject = (DataSop) session
									.createCriteria(formSop.ambilClass()).add(Restrictions.idEq(temp.getId()))
									.uniqueResult();

							if (generalValueObject != null) {

								JSONObject jsonObject = new JSONObject();
								jsonObject.put("id", generalValueObject.getId());
								jsonObject.put("kode", generalValueObject.getKode());
								jsonObject.put("nama", generalValueObject.getNama());
								jsonObject.put("keterangan", keterangan.getValue());

								String key = formSop.ambilClass().getName();
								jsonObjectData.put(key, jsonObject);

								disposisiSop.setProperti(jsonObjectData.toString());
								// Jaga kolom kode DisposisiSop selalu = kode class di properti (selalu tampil).
								ais.action.master.sop.helper.SopKodeUtil.sinkronkanKode(disposisiSop);

								jsonObject = new JSONObject();
								jsonObject.put("id", generalValueObject.getId());
								jsonObject.put("kode", generalValueObject.getKode());
								jsonObject.put("nama", generalValueObject.getNama());
								jsonObject.put("keterangan", keterangan.getValue());
								alurSopAwal.setProperti(jsonObject.toString());
								// merge (bukan update): instance DisposisiAlurSop dengan id sama bisa
								// sudah ter-asosiasi di session → update() melempar NonUniqueObjectException.
								session.merge(alurSopAwal);
								session.flush();

								generalValueObject.setDisposisiSop(disposisiSop);

								session.update(generalValueObject);
								session.flush();
							}
						}
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}

					try {
						disposisiSop.setDisposisiStart(alurSopAwal);

						if (disposisiSop.getDisposisiEnd() == null || (disposisiSop.getDisposisiEnd() != null
								&& disposisiSop.getDisposisiEnd().getId() < alurSopAwal.getId())) {
							disposisiSop.setDisposisiEnd(alurSopAwal);
						}

						if (alurSopAwal != null && alurSopAwal.getAlurSop() != null
								&& (alurSopAwal.getAlurSop().getJikaProsesDisetujuiMakaSelesai()
										|| alurSopAwal.setujui())) {
							disposisiSop.setDisposisiSetuju(alurSopAwal);
						}

						Common.refreshUpdate(session, disposisiSop);
						session.flush();
					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}

				}

				for (AlurSop alurSop : selanjutnya) {

					AlurSop alurSopData = (AlurSop) session.createCriteria(AlurSop.class)
							.add(Restrictions.idEq(alurSop.getId())).uniqueResult();

					if (alurSopData != null) {

						DisposisiAlurSop a = (DisposisiAlurSop) session.createCriteria(DisposisiAlurSop.class)
								.add(Restrictions.isNotNull("alurSop")).add(Restrictions.eq("alurSop", alurSopData))
								.add(Restrictions.eq("disposisiSop", disposisiSop))
								.addOrder(Order.asc("id"))
								.add(Restrictions.eq("sebelumnya", alurSopAwal)).setMaxResults(1).uniqueResult();
						if (a == null) {
							a = new DisposisiAlurSop(hboxAktor.getAttribute("usernamePengguna"));
						}
						final DisposisiAlurSop disposisiAlurSopSetelah = a;
						disposisiAlurSopSetelah.setSebelumnya(alurSopAwal);
						disposisiAlurSopSetelah.setDisposisiSop(disposisiSop);
						disposisiAlurSopSetelah.setAlurSop(alurSopData);
						Common.refreshSaveOrUpdate(session, disposisiAlurSopSetelah);
						session.flush();

						Common.createDefaultTimerNoBusy((new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								BroadcastHelper.kirimEmailDisposisi(disposisiAlurSopSetelah);
							}
						}));
					}
				}

				if (lampiranCatatanDisposisi != null) {
					Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
					streamingSession.getTransaction().begin();
					streamingSession.refresh(lampiranCatatanDisposisi);
					lampiranCatatanDisposisi.setRef(alurSopAwal.getId());
					streamingSession.update(lampiranCatatanDisposisi);
					streamingSession.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}

				if (!lampiranLains.isEmpty()) {
					Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
					streamingSession.getTransaction().begin();
					for (LampiranLain lampiranLain : lampiranLains.values()) {
						streamingSession.refresh(lampiranLain);
						lampiranLain.setRef(alurSopAwal.getId());
						streamingSession.update(lampiranLain);
					}
					streamingSession.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}

				if (lainMahasiswa != null && !lainMahasiswa.isEmpty()) {
					try {
						session = StreamingHibernateUtil.getInstance().currentSession();

						for (Long id : lainMahasiswa.keySet()) {

							LampiranLain lain = lainMahasiswa.get(id);

							session.refresh(lain);
							lain.setRef(disposisiSop.getId());

							session.getTransaction().begin();
							session.update(lain);
							session.getTransaction().commit();
						}

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

			}
		}));

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DisposisiSop.class)

				.add(AktorSop.buatCriterionPengaju(tbmuser, ""))

				.createAlias("sop", "sop").add(Restrictions.eq("sop.aktif", true))
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("id"));

		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("properti", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.or(
								Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("sop.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))));

		criteria.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
				|| searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("sop.jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("sop.jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sop.fakultas"),
										CommonSearchFilterHelper.eqSelectedWithId("sop.fakultas", searchfakultas, false)))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sop.sekolah"),
										CommonSearchFilterHelper.eqSelectedWithId("sop.sekolah", searchsekolah, false)))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("sop.yayasan"),
										CommonSearchFilterHelper.eqSelectedWithId("sop.yayasan", searchyayasan, false)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<DisposisiSop> disposisiSop = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(disposisiSop);
		grid.setRowRenderer(new DisposisiSopRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static void reloadDataMenu(final Rows rows, final JSONArray array, final EventListener eventListener)
			throws Exception {
		if (array.length() > 0) {
			for (int i = 0; i < array.length(); i++) {
				JSONObject jsonObject = array.getJSONObject(i);

				Long menu = null;
				String nama = "";
				String param = "";
				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.getString("nama");
				}
				if (!jsonObject.isNull("param")) {
					param = jsonObject.getString("param");
				}
				if (!jsonObject.isNull("menu")) {
					menu = ais.common.CommonJSONUtil.ambilLong(jsonObject, "menu");
				}
				Menu myMenu = (Menu) (menu == null ? null : ConstantValues.ambil(Menu.class.getName(), menu));

				if (myMenu != null) {
					final String url = myMenu.getUrl() + param;
					final String n = nama.isEmpty() ? myMenu.getLabel() : nama;
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label());
					MyButtonConfig toolbarbutton = new MyButtonConfig(n, "/img/svg/list-task.svg");
					row.appendChild(toolbarbutton);
					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.displayWindow(url, false, "95%", "95%", eventListener, n);

						}
					});
				}
			}
		}
	}
}
