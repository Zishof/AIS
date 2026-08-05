package ais.action.master.sirs.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.akunting.helper.AmbilDataAkunKreditBanbox;
import ais.action.master.sirs.helper.AmbilDataJenisBiayaBanyak;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.listener.OnSave;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.sirs.Biaya;
import ais.database.model.sirs.GenerikItem;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JenisBiaya;
import ais.database.model.sirs.JenisItemMedis;
import ais.database.model.sirs.KelasItem;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.KelompokItem;
import ais.database.model.sirs.SatuanItem;
import ais.database.model.sirs.TarifKhusus;
import ais.database.model.sirs.TarifKhususPunyaItem;
import ais.ui.util.MyDoublebox;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

public class CommonItem {

	public static void onUploadBiaya(Event event, final TarifKhusus tarifKhusus) throws Exception {

		final Media media;

		if (event instanceof ForwardEvent) {
			ForwardEvent forwardEvent = (ForwardEvent) event;
			media = ((UploadEvent) forwardEvent.getOrigin()).getMedia();
			if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
				return;
		} else {
			media = ((UploadEvent) event).getMedia();
			if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
				return;
		}

		final Label label = new Label(ais.common.Common.getBahasaConfig("memproses data .."));

		try {
			InputStream inputStream = media.getStreamData();
			System.out.println("media = " + media);
			final File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
			System.out.println("file = " + file.getAbsolutePath());
			file.getParentFile().mkdirs();

			FileOutputStream fileOutputStream = new FileOutputStream(file);
			int c;
			while ((c = inputStream.read()) != -1) {
				fileOutputStream.write(c);
			}
			fileOutputStream.close();
			inputStream.close();

			if (media.getName().toLowerCase().endsWith("xls")) {

				Runnable runnable = new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {

						try {

							Cell cell = null;
							KelasPerawatan kelasPerawatanUmum = null;
							Map<Long, JenisBiaya> jenisBiayaMap = new HashMap<Long, JenisBiaya>();

							Workbook workbook = Workbook.getWorkbook(file);
							for (Sheet sheet : workbook.getSheets()) {

								String kelas = sheet.getName();
								// FIX bocor: SELALU di-reassign kedua cabang sebelum dipakai; bila di-open pasti orphan & bocor tiap sheet
								Session session = null;

								if (kelas.trim().equalsIgnoreCase("1000000-DATA")) {

									for (int i = 1; i <= sheet.getRows(); i++) {

										session = HibernateUtil.getSessionFactory().openSession();
										try {

											try {
												cell = sheet.getCell(0, i);
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:139");
												// session.disconnect();
												if (session.isOpen()) {session.disconnect();session.close();}
												break;
											}
											if (cell == null) {
												// session.disconnect();
												if (session.isOpen()) {session.disconnect();session.close();}
												break;
											}

											String id = sheet.getCell(0, i) == null
													|| sheet.getCell(0, i).getContents() == null ? null
															: sheet.getCell(0, i).getContents().trim();
											String kode = sheet.getCell(1, i) == null
													|| sheet.getCell(1, i).getContents() == null ? null
															: sheet.getCell(1, i).getContents().trim();

											String nama = sheet.getCell(2, i) == null
													|| sheet.getCell(2, i).getContents() == null ? null
															: sheet.getCell(2, i).getContents().trim();

											String bolehretur = sheet.getCell(3, i) == null
													|| sheet.getCell(3, i).getContents() == null ? null
															: sheet.getCell(3, i).getContents().trim();

											String sama = sheet.getCell(4, i) == null
													|| sheet.getCell(4, i).getContents() == null ? null
															: sheet.getCell(4, i).getContents().trim();

											String barcode = sheet.getCell(5, i) == null
													|| sheet.getCell(5, i).getContents() == null ? null
															: sheet.getCell(5, i).getContents().trim();

											String jenisItem = sheet.getCell(6, i) == null
													|| sheet.getCell(6, i).getContents() == null ? null
															: sheet.getCell(6, i).getContents().trim();

											String satuanItem = sheet.getCell(7, i) == null
													|| sheet.getCell(7, i).getContents() == null ? null
															: sheet.getCell(7, i).getContents().trim();
											String kelompokItem = sheet.getCell(8, i) == null
													|| sheet.getCell(8, i).getContents() == null ? null
															: sheet.getCell(8, i).getContents().trim();

											String kelasItem = sheet.getCell(9, i) == null
													|| sheet.getCell(9, i).getContents() == null ? null
															: sheet.getCell(9, i).getContents().trim();
											String generikItem = sheet.getCell(10, i) == null
													|| sheet.getCell(10, i).getContents() == null ? null
															: sheet.getCell(10, i).getContents().trim();

											String kandungan = sheet.getCell(11, i) == null
													|| sheet.getCell(11, i).getContents() == null ? null
															: sheet.getCell(11, i).getContents().trim();

											String keterangan = sheet.getCell(12, i) == null
													|| sheet.getCell(12, i).getContents() == null ? null
															: sheet.getCell(12, i).getContents().trim();

											ItemMedis item = null;

											if (id != null && !id.trim().isEmpty()) {
												item = (ItemMedis) session.createCriteria(ItemMedis.class)
														.setMaxResults(1)
														.add(Restrictions.idEq(Long.parseLong(id.trim())))
														.uniqueResult();
											}

											if (item == null && kode != null && !kode.trim().isEmpty()) {
												item = (ItemMedis) session.createCriteria(ItemMedis.class)
														.setMaxResults(1)
														.add(Restrictions.ilike("kode", kode.trim(), MatchMode.EXACT))
														.uniqueResult();
											}

											if (item == null && nama != null && !nama.trim().isEmpty()) {
												item = (ItemMedis) session.createCriteria(ItemMedis.class)
														.setMaxResults(1)
														.add(Restrictions.ilike("nama", nama.trim(), MatchMode.EXACT))
														.uniqueResult();
											}

											if (item == null) {
												item = new ItemMedis();
											}

											item.setKode(kode);
											item.setNama(nama);
											item.setBarcode(barcode);
											item.setKandungan(kandungan);
											item.setKeterangan(keterangan);

											JenisItemMedis myJenisItem = null;
											try {

												myJenisItem = (JenisItemMedis) session
														.createCriteria(JenisItemMedis.class)
														.add(Restrictions.idEq(
																Long.parseLong(jenisItem.trim().split("-")[0].trim())))
														.uniqueResult();
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:240");

											}

											if (myJenisItem == null) {
												try {

													myJenisItem = (JenisItemMedis) session
															.createCriteria(JenisItemMedis.class)
															.add(Restrictions.idEq(Long
																	.parseLong(jenisItem.trim().split("-")[1].trim())))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:252");

												}
											}

											if (myJenisItem == null) {
												try {
													myJenisItem = (JenisItemMedis) session
															.createCriteria(JenisItemMedis.class)
															.add(Restrictions.ilike("nama",
																	jenisItem.trim().split("-")[1].trim(),
																	MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:265");

												}

											}

											if (myJenisItem == null) {
												try {
													myJenisItem = (JenisItemMedis) session
															.createCriteria(JenisItemMedis.class).add(Restrictions
																	.ilike("nama", jenisItem.trim(), MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:277");

												}

											}

											item.setJenisItem(myJenisItem);

											SatuanItem mySatuanItem = null;
											try {

												mySatuanItem = (SatuanItem) session.createCriteria(SatuanItem.class)
														.add(Restrictions.idEq(
																Long.parseLong(satuanItem.trim().split("-")[0].trim())))
														.uniqueResult();
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:292");

											}

											if (mySatuanItem == null) {
												try {

													mySatuanItem = (SatuanItem) session.createCriteria(SatuanItem.class)
															.add(Restrictions.idEq(-Long
																	.parseLong(satuanItem.trim().split("-")[1].trim())))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:303");

												}
											}

											if (mySatuanItem == null) {
												try {
													mySatuanItem = (SatuanItem) session.createCriteria(SatuanItem.class)
															.add(Restrictions.ilike("nama",
																	satuanItem.trim().split("-")[1].trim(),
																	MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:315");

												}

											}

											if (mySatuanItem == null) {
												try {
													mySatuanItem = (SatuanItem) session
															.createCriteria(SatuanItem.class).add(Restrictions
																	.ilike("nama", satuanItem.trim(), MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:327");

												}

											}

											item.setSatuanItem(mySatuanItem);

											KelompokItem myKelompokItem = null;
											try {

												myKelompokItem = (KelompokItem) session
														.createCriteria(KelompokItem.class)
														.add(Restrictions.idEq(Long
																.parseLong(kelompokItem.trim().split("-")[0].trim())))
														.uniqueResult();
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:343");

											}

											if (myKelompokItem == null) {
												try {

													myKelompokItem = (KelompokItem) session
															.createCriteria(KelompokItem.class)
															.add(Restrictions.idEq(-Long.parseLong(
																	kelompokItem.trim().split("-")[1].trim())))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:355");

												}
											}

											if (myKelompokItem == null) {
												try {
													myKelompokItem = (KelompokItem) session
															.createCriteria(KelompokItem.class)
															.add(Restrictions.ilike("nama",
																	kelompokItem.trim().split("-")[1].trim(),
																	MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:368");

												}

											}

											if (myKelompokItem == null) {
												try {
													myKelompokItem = (KelompokItem) session
															.createCriteria(KelompokItem.class)
															.add(Restrictions.ilike("nama", kelompokItem.trim(),
																	MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:381");

												}

											}

											item.setKelompokItem(myKelompokItem);

											KelasItem myKelasItem = null;
											try {

												myKelasItem = (KelasItem) session.createCriteria(KelasItem.class)
														.add(Restrictions.idEq(
																Long.parseLong(kelasItem.trim().split("-")[0].trim())))
														.uniqueResult();
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:396");

											}

											if (myKelasItem == null) {
												try {

													myKelasItem = (KelasItem) session.createCriteria(KelasItem.class)
															.add(Restrictions.idEq(-Long
																	.parseLong(kelasItem.trim().split("-")[1].trim())))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:407");

												}
											}

											if (myKelasItem == null) {
												try {
													myKelasItem = (KelasItem) session.createCriteria(KelasItem.class)
															.add(Restrictions.ilike("nama",
																	kelasItem.trim().split("-")[1].trim(),
																	MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:419");

												}

											}

											if (myKelasItem == null) {
												try {
													myKelasItem = (KelasItem) session
															.createCriteria(KelasItem.class).add(Restrictions
																	.ilike("nama", kelasItem.trim(), MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:431");

												}

											}

											item.setKelasItem(myKelasItem);

											GenerikItem myGenerikItem = null;
											try {

												myGenerikItem = (GenerikItem) session.createCriteria(GenerikItem.class)
														.add(Restrictions.idEq(Long
																.parseLong(generikItem.trim().split("-")[0].trim())))
														.uniqueResult();
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:446");

											}

											if (myGenerikItem == null) {
												try {

													myGenerikItem = (GenerikItem) session
															.createCriteria(GenerikItem.class)
															.add(Restrictions.idEq(-Long.parseLong(
																	generikItem.trim().split("-")[1].trim())))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:458");

												}
											}

											if (myGenerikItem == null) {
												try {
													myGenerikItem = (GenerikItem) session
															.createCriteria(GenerikItem.class)
															.add(Restrictions.ilike("nama",
																	generikItem.trim().split("-")[1].trim(),
																	MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:471");

												}

											}

											if (myGenerikItem == null) {
												try {
													myGenerikItem = (GenerikItem) session
															.createCriteria(GenerikItem.class).add(Restrictions
																	.ilike("nama", generikItem.trim(), MatchMode.EXACT))
															.uniqueResult();
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:483");

												}

											}

											item.setGenerikItem(myGenerikItem);

											try {
												item.setBolehretur(Boolean.parseBoolean(bolehretur));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:494");
											}

											try {
												item.setSemuahargasama(Boolean.parseBoolean(sama.trim()));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:500");
											}

											label.setValue("data item -> "
													+ Common.numberFormat.get().format(i * 100.0 / sheet.getRows()) + "% - "
													+ item.toString());

											session.getTransaction().begin();
											session.saveOrUpdate(item);
											session.getTransaction().commit();

											if (tarifKhusus != null) {
												Number jumlah = (Number) session
														.createCriteria(TarifKhususPunyaItem.class)
														.add(Restrictions.eq("tarifKhusus", tarifKhusus))
														.add(Restrictions.eq("item", item))
														.setProjection(Projections.rowCount()).uniqueResult();

												if (jumlah.intValue() == 0) {
													TarifKhususPunyaItem tarifKhususPunyaItem = new TarifKhususPunyaItem();
													tarifKhususPunyaItem.setAktif(true);
													tarifKhususPunyaItem.setTarifKhusus(tarifKhusus);
													tarifKhususPunyaItem.setItem(item);
													session.getTransaction().begin();
													session.save(tarifKhususPunyaItem);
													session.getTransaction().commit();
												}
											}

										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:530");
										}

										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
									}

								} else {

									session = HibernateUtil.getSessionFactory().openSession();

									KelasPerawatan kelasPerawatan = null;
									try {
										kelasPerawatan = (KelasPerawatan) session
												.createCriteria(KelasPerawatan.class)
												.add(Restrictions.idEq(Long.parseLong(kelas.split("-")[0].trim())))
												.uniqueResult();
									} finally {
										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
									}

									if (kelasPerawatan != null) {

										if (kelasPerawatan.getNama().trim().equalsIgnoreCase("umum")) {
											kelasPerawatanUmum = kelasPerawatan;
										}

										for (int i = 1; i <= sheet.getRows(); i++) {

											session = HibernateUtil.getSessionFactory().openSession();
											try {

												String id = sheet.getCell(0, i) == null
														|| sheet.getCell(0, i).getContents() == null ? null
																: sheet.getCell(0, i).getContents().trim();
												String kode = sheet.getCell(1, i) == null
														|| sheet.getCell(1, i).getContents() == null ? null
																: sheet.getCell(1, i).getContents().trim();

												String nama = sheet.getCell(2, i) == null
														|| sheet.getCell(2, i).getContents() == null ? null
																: sheet.getCell(2, i).getContents().trim();

												String tarif = sheet.getCell(3, i) == null
														|| sheet.getCell(3, i).getContents() == null ? null
																: sheet.getCell(3, i).getContents().trim();

												String persen = sheet.getCell(4, i) == null
														|| sheet.getCell(4, i).getContents() == null ? null
																: sheet.getCell(4, i).getContents().trim();

												String berubah = sheet.getCell(5, i) == null
														|| sheet.getCell(5, i).getContents() == null ? null
																: sheet.getCell(5, i).getContents().trim();

												ItemMedis item = null;

												if (id != null && !id.trim().isEmpty()) {
													item = (ItemMedis) session.createCriteria(ItemMedis.class)
															.setMaxResults(1)
															.add(Restrictions.idEq(Long.parseLong(id.trim())))
															.uniqueResult();
												}

												if (item == null && kode != null && !kode.trim().isEmpty()) {
													item = (ItemMedis) session.createCriteria(ItemMedis.class)
															.setMaxResults(1).add(Restrictions.ilike("kode",
																	kode.trim(), MatchMode.EXACT))
															.uniqueResult();
												}

												if (item == null && nama != null && !nama.trim().isEmpty()) {
													item = (ItemMedis) session.createCriteria(ItemMedis.class)
															.setMaxResults(1).add(Restrictions.ilike("nama",
																	nama.trim(), MatchMode.EXACT))
															.uniqueResult();
												}

												if (item != null) {

													TarifKhususPunyaItem tarifKhususPunyaItem = null;

													if (tarifKhusus != null) {
														tarifKhususPunyaItem = (TarifKhususPunyaItem) session
																.createCriteria(TarifKhususPunyaItem.class)
																.add(Restrictions.eq("tarifKhusus", tarifKhusus))
																.add(Restrictions.eq("item", item)).setMaxResults(1)
																.uniqueResult();
													}

													HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(item,
															kelasPerawatan, tarifKhususPunyaItem, session);

													try {
														hargaJualItem.setPembagianBiayaDalamPersen(
																Boolean.parseBoolean(persen));
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:628");
													}

													try {
														hargaJualItem.setHargaBisaDirubahSaatTransaksi(
																Boolean.parseBoolean(berubah));
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:635");
													}

													try {
														hargaJualItem.setHargaJual(Double.parseDouble(tarif));
														session.getTransaction().begin();
														session.update(hargaJualItem);
														session.getTransaction().commit();
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:644");
													}

													label.setValue("data harga " + kelasPerawatan.getNama() + " -> "
															+ Common.numberFormat.get().format(i * 100.0 / sheet.getRows())
															+ "% - " + hargaJualItem.toString());

													for (int index = 6; index < sheet.getColumns(); index++) {

														try {
															String jenis = sheet.getCell(index, 0) == null
																	|| sheet.getCell(index, 0).getContents() == null
																			? null
																			: sheet.getCell(index, 0).getContents()
																					.trim();

															String nilai = sheet.getCell(index, i) == null
																	|| sheet.getCell(index, i).getContents() == null
																			? null
																			: sheet.getCell(index, i).getContents()
																					.trim();

															Double jumlah = 0.0;
															try {
																jumlah = Double.parseDouble(nilai.trim());
															} catch (Exception e) {
																e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:670");
															}

															JenisBiaya jenisBiaya = (JenisBiaya) session
																	.createCriteria(JenisBiaya.class)
																	.add(Restrictions.idEq(
																			Long.parseLong(jenis.split("-")[0].trim())))
																	.uniqueResult();

															if (jenisBiaya != null) {

																jenisBiayaMap.put(jenisBiaya.getId(), jenisBiaya);

																Biaya biaya = ((Biaya) session
																		.createCriteria(Biaya.class)
																		.add(Restrictions.eq("jenisBiaya", jenisBiaya))
																		.add(Restrictions.eq("hargaJualItem",
																				hargaJualItem))

																		.add(Restrictions
																				.isNull("detailTransaksiLayanan"))
																		.add(Restrictions.isNull("detailTransaksi"))

																		.setMaxResults(1).uniqueResult());

																if (biaya == null) {
																	biaya = new Biaya();
																}

																biaya.setHargaJualItem(hargaJualItem);
																biaya.setJenisBiaya(jenisBiaya);

																if (hargaJualItem.getPembagianBiayaDalamPersen()) {
																	biaya.setPersen(jumlah);
																} else {
																	biaya.setJumlah(jumlah);
																}

																session.getTransaction().begin();
																session.saveOrUpdate(biaya);
																session.getTransaction().commit();

															}

														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:715");
														}
													}
												}

											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:721");
												// e.printStackTrace();
											}
										}

										// session.disconnect();
										if (session.isOpen()) {session.disconnect();session.close();}
									}
								}
							}

							if (kelasPerawatanUmum != null) {
								Session session = HibernateUtil.getSessionFactory().openSession();
								try {

								List<ItemMedis> itemsSama = session.createCriteria(ItemMedis.class)
										.add(Restrictions.eq("semuahargasama", true)).addOrder(Order.asc("nama"))
										.list();

								List<KelasPerawatan> kelasPerawatans = session.createCriteria(KelasPerawatan.class)
										.add(Restrictions.ne("id", kelasPerawatanUmum.getId()))
										.addOrder(Order.asc("id")).list();

								int i = 1;
								int size = itemsSama.size();
								for (ItemMedis item : itemsSama) {
									label.setValue("Singkronisasi -> " + Common.numberFormat.get().format(i * 100.0 / size)
											+ "% - " + item.toString());
									i++;

									TarifKhususPunyaItem tarifKhususPunyaItem = null;

									if (tarifKhusus != null) {
										tarifKhususPunyaItem = (TarifKhususPunyaItem) session
												.createCriteria(TarifKhususPunyaItem.class)
												.add(Restrictions.eq("tarifKhusus", tarifKhusus))
												.add(Restrictions.eq("item", item)).setMaxResults(1).uniqueResult();
									}

									HargaJualItem hargaJualItemUmum = CommonTarifItem.getHargaJualItem(item,
											kelasPerawatanUmum, tarifKhususPunyaItem, session);

									for (JenisBiaya jenisBiaya : jenisBiayaMap.values()) {

										Biaya biayaUmum = ((Biaya) session.createCriteria(Biaya.class)
												.add(Restrictions.eq("jenisBiaya", jenisBiaya))
												.add(Restrictions.eq("hargaJualItem", hargaJualItemUmum))

												.add(Restrictions.isNull("detailTransaksiLayanan"))
												.add(Restrictions.isNull("detailTransaksi"))

												.setMaxResults(1).uniqueResult());

										if (biayaUmum != null) {

											for (KelasPerawatan kelasPerawatan : kelasPerawatans) {

												HargaJualItem hargaJualItem = CommonTarifItem.getHargaJualItem(item,
														kelasPerawatan, tarifKhususPunyaItem, session);

												Biaya biaya = ((Biaya) session.createCriteria(Biaya.class)
														.add(Restrictions.eq("jenisBiaya", jenisBiaya))
														.add(Restrictions.eq("hargaJualItem", hargaJualItem))

														.add(Restrictions.isNull("detailTransaksiLayanan"))
														.add(Restrictions.isNull("detailTransaksi"))

														.setMaxResults(1).uniqueResult());

												if (biaya == null) {
													biaya = new Biaya();
												}

												hargaJualItem.setHargaJual(hargaJualItemUmum.getHargaJual());
												hargaJualItem.setPembagianBiayaDalamPersen(
														hargaJualItemUmum.getPembagianBiayaDalamPersen());
												hargaJualItem.setHargaBisaDirubahSaatTransaksi(
														hargaJualItemUmum.getHargaBisaDirubahSaatTransaksi());

												biaya.setHargaJualItem(hargaJualItem);
												biaya.setJenisBiaya(jenisBiaya);

												biaya.setPersen(biayaUmum.getPersen());
												biaya.setJumlah(biayaUmum.getJumlah());
												biaya.setJumlahTotal(biayaUmum.getJumlahTotal());

												session.getTransaction().begin();
												session.update(hargaJualItem);
												session.saveOrUpdate(biaya);
												session.getTransaction().commit();

											}
										}
									}

								}

								} finally {
									// session.disconnect();
									if (session.isOpen()) {session.disconnect();session.close();}
								}
							}

						} catch (Exception ex) {
							System.out.println(ex);
							ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:826");

						}

						label.setValue("");
					}

				};

				new Thread(runnable).start();

				final Timer timer = new Timer(500);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						System.out.println("process = " + label.getValue());
						Clients.showBusy("Upload data .. " + label.getValue());
						if (label.getValue().isEmpty()) {
							Clients.clearBusy();
							MyMessageboxConfig.showFormat("Alhamdulillah, proses unggah data telah berhasil dilakukan. {V1}", "Error", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION, media);
							timer.detach();
						}

					}
				});
				timer.start();

			}

			else {
				MyMessageboxConfig.showFormat("Mohon maaf, berkas yang Bapak/Ibu unggah harus berformat Excel 2003 (xls). {V1}. Langkah yang dapat dilakukan: (1) simpan berkas dalam format .xls; (2) unggah kembali berkas tersebut.", "Error",
						MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
			}

		} catch (Exception e) {

			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:867");
			// e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static void onDownloadBiaya(Event event, final TarifKhusus tarifKhusus) throws Exception {

		Clients.showBusy("memproses data ..");

		final String filename = Sessions.getCurrent().getWebApp()
				.getRealPath("/tmp/biaya_item_" + (Common.dateFormat6.get().format(new Date())) + ".xls");

		new File(filename).createNewFile();

		final Label label = new Label(ais.common.Common.getBahasaConfig("memproses data .."));

		Runnable runnable = new Runnable() {

			@Override
			public void run() {

				Session session = null;
				try {

					session = HibernateUtil.getSessionFactory().openSession();
					List<KelasPerawatan> kelasPerawatans = session.createCriteria(KelasPerawatan.class)
							.addOrder(Order.asc("id")).list();
					@SuppressWarnings("resource")
					HSSFWorkbook workbook = new HSSFWorkbook();

					List<ItemMedis> items = null;

					if (tarifKhusus != null) {
						items = session.createCriteria(TarifKhususPunyaItem.class)
								.setProjection(Projections.groupProperty("item")).list();
					} else {
						items = session.createCriteria(ItemMedis.class).addOrder(Order.asc("nama")).list();
					}

					List<JenisBiaya> jenisBiayas = session.createCriteria(Biaya.class)
							.add(Restrictions.isNull("detailTransaksiLayanan"))
							.add(Restrictions.isNull("detailTransaksi"))
							.setProjection(Projections.groupProperty("jenisBiaya"))
							.createAlias("hargaJualItem", "hargaJualItem").createAlias("jenisBiaya", "jenisBiaya")
							.add(Restrictions.eq("jenisBiaya.aktif", true)).list();

					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}

					if (true) {

						HSSFSheet sheet = workbook.createSheet("1000000-DATA");

						HSSFRow rowhead = sheet.createRow((short) 0);
						rowhead.createCell(0).setCellValue("ID");
						rowhead.createCell(1).setCellValue("Kode");
						rowhead.createCell(2).setCellValue("Nama");
						rowhead.createCell(3).setCellValue("Boleh retur");
						rowhead.createCell(4).setCellValue("Harga Sama");
						rowhead.createCell(5).setCellValue("Barcode");
						rowhead.createCell(6).setCellValue("Jenis");

						rowhead.createCell(7).setCellValue("Satuan");
						rowhead.createCell(8).setCellValue("Kelompok");
						rowhead.createCell(9).setCellValue("Kelas");
						rowhead.createCell(10).setCellValue("Generik");
						rowhead.createCell(11).setCellValue("Kandungan");
						rowhead.createCell(12).setCellValue("Keterangan");

						int i = 1;
						int size = items.size();
						for (ItemMedis item : items) {

							session = HibernateUtil.getSessionFactory().openSession();

							label.setValue("data item -> " + Common.numberFormat.get().format(i * 100.0 / size) + "% - "
									+ item.toString());

							try {

								HSSFRow row = sheet.createRow(i);
								row.createCell(0).setCellValue(item.getId());
								row.createCell(1).setCellValue(item.getKode());
								row.createCell(2).setCellValue(item.getNama());
								row.createCell(3).setCellValue(item.getBolehretur());
								row.createCell(4).setCellValue(item.getSemuahargasama());

								row.createCell(5).setCellValue(item.getBarcode());

								row.createCell(6).setCellValue(item.getJenisItem() == null ? ""
										: (item.getJenisItem().getId() + "-" + item.getJenisItem().getNama().trim()));

								row.createCell(7).setCellValue(item.getSatuanItem() == null ? ""
										: (item.getSatuanItem().getId() + "-" + item.getSatuanItem().getNama().trim()));

								row.createCell(8)
										.setCellValue(item.getKelompokItem() == null ? ""
												: (item.getKelompokItem().getId() + "-"
														+ item.getKelompokItem().getNama().trim()));

								row.createCell(9).setCellValue(item.getKelasItem() == null ? ""
										: (item.getKelasItem().getId() + "-" + item.getKelasItem().getNama().trim()));

								row.createCell(10).setCellValue(

										item.getGenerikItem() == null ? ""
												: (item.getGenerikItem().getId() + "-"
														+ item.getGenerikItem().getNama().trim()));

								row.createCell(11).setCellValue(item.getKandungan());
								row.createCell(12).setCellValue(item.getKeterangan());

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:981");
							}

							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}

							i++;
						}
					}

					for (KelasPerawatan kelasPerawatan : kelasPerawatans) {

						HSSFSheet sheet = workbook.createSheet(kelasPerawatan.getId() + "-" + kelasPerawatan.getNama());

						HSSFRow rowhead = sheet.createRow((short) 0);
						rowhead.createCell(0).setCellValue("ID");
						rowhead.createCell(1).setCellValue("Kode");
						rowhead.createCell(2).setCellValue("Nama");
						rowhead.createCell(3).setCellValue("Tarif");
						rowhead.createCell(4).setCellValue("Biaya dalam persen");
						rowhead.createCell(5).setCellValue("Biaya bisa dirubah saat transaksi");

						int index = 6;
						for (JenisBiaya jenisBiaya : jenisBiayas) {
							rowhead.createCell(index).setCellValue(jenisBiaya.getId() + "-" + jenisBiaya.getNama());
							index++;
						}

						int i = 1;
						int size = items.size();
						for (ItemMedis item : items) {

							session = HibernateUtil.getSessionFactory().openSession();

							HargaJualItem hargaJualItem;
							TarifKhususPunyaItem tarifKhususPunyaItem = null;

							if (tarifKhusus != null) {
								tarifKhususPunyaItem = (TarifKhususPunyaItem) session
										.createCriteria(TarifKhususPunyaItem.class)
										.add(Restrictions.eq("tarifKhusus", tarifKhusus))
										.add(Restrictions.eq("item", item)).setMaxResults(1).uniqueResult();
							}

							if (tarifKhususPunyaItem == null) {
								hargaJualItem = CommonTarifItem.getHargaJualItem(item, kelasPerawatan, null, session);
							} else {
								hargaJualItem = CommonTarifItem.getHargaJualItem(item, kelasPerawatan,
										tarifKhususPunyaItem, session);
							}

							label.setValue("harga kelas " + kelasPerawatan.getNama() + " -> "
									+ Common.numberFormat.get().format(i * 100.0 / size) + "% - " + hargaJualItem.toString());

							try {

								HSSFRow row = sheet.createRow(i);
								row.createCell(0).setCellValue(item.getId());
								row.createCell(1).setCellValue(item.getKode());
								row.createCell(2).setCellValue(item.getNama());
								row.createCell(3).setCellValue(hargaJualItem.getHargaJual());

								row.createCell(4).setCellValue(hargaJualItem.getPembagianBiayaDalamPersen());
								row.createCell(5).setCellValue(hargaJualItem.getHargaBisaDirubahSaatTransaksi());

								index = 6;
								for (JenisBiaya jenisBiaya : jenisBiayas) {
									Biaya biaya = ((Biaya) session.createCriteria(Biaya.class)
											.add(Restrictions.eq("jenisBiaya", jenisBiaya))
											.add(Restrictions.eq("hargaJualItem", hargaJualItem))

											.add(Restrictions.isNull("detailTransaksiLayanan"))
											.add(Restrictions.isNull("detailTransaksi"))

											.setMaxResults(1).uniqueResult());

									if (hargaJualItem.getPembagianBiayaDalamPersen()) {
										row.createCell(index).setCellValue(biaya == null ? 0.0 : biaya.getPersen());
									} else {
										row.createCell(index).setCellValue(biaya == null ? 0.0 : biaya.getJumlah());
									}
									index++;
								}

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:1066");
							}

							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}

							i++;
						}
					}

					FileOutputStream fileOut = new FileOutputStream(filename);
					workbook.write(fileOut);
					fileOut.close();
					System.out.println("Your excel file has been generated! " + workbook.getSummaryInformation());

					label.setValue("");

				} catch (Exception ex) {
					System.out.println(ex);
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:1085");
					label.setValue("");
				} finally {
					// FIX bocor: session dedikasi (openSession) dulu ditutup di jalur normal saja;
					// finally menjamin penutupan walau exception (idempoten via isOpen()).
					if (session != null && session.isOpen()) {
						try { session.disconnect(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:1091");}
						try { session.close(); } catch (Exception eSf) { ais.common.ErrorAuditUtil.record(eSf, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:1092");}
					}
				}
			}
		};

		new Thread(runnable).start();

		final Timer timer = new Timer(500);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				System.out.println("process = " + label.getValue());
				Clients.showBusy("Download data .. " + label.getValue());
				if (label.getValue().isEmpty()) {
					Clients.clearBusy();
					Filedownload.save(new File(filename), "application/vnd.ms-excel");
					timer.detach();
				}

			}
		});
		timer.start();

	}

	public static class InitHarga {

		public Grid gridHargaJual = new Grid();
		public Checkbox semuahargasama = new Checkbox();

		@SuppressWarnings("unchecked")
		public boolean saveDetail(ItemMedis item, TarifKhususPunyaItem tarifKhususPunyaItem) {
			Session session = HibernateUtil.currentSession();
			List<Row> rows = gridHargaJual.getRows().getChildren();
			for (Row row : rows) {
				KelasPerawatan kelasPerawatan = (KelasPerawatan) row.getAttribute("kelasPerawatan");
				HargaJualItem hargaJualItem = (HargaJualItem) row.getAttribute("hargaJualItem");

				if (hargaJualItem != null) {
					Double nilaiBiaya = ((MyDoublebox) row.getChildren().get(row.getChildren().size() - 1)).getValue();
					nilaiBiaya = nilaiBiaya == null ? 0.0 : nilaiBiaya;

					Checkbox persen = (Checkbox) row.getChildren().get(1);
					Checkbox berubah = (Checkbox) row.getChildren().get(2);

					hargaJualItem.setHargaBisaDirubahSaatTransaksi(berubah.isChecked());
					hargaJualItem.setPembagianBiayaDalamPersen(persen.isChecked());

					if (tarifKhususPunyaItem == null) {
						hargaJualItem.setItem(item);
						hargaJualItem.setTarifKhususPunyaItem(null);
					} else {
						hargaJualItem.setTarifKhususPunyaItem(tarifKhususPunyaItem);
						hargaJualItem.setItem(null);
					}

					hargaJualItem.setKelasPerawatan(kelasPerawatan);
					hargaJualItem.setHargaJual(nilaiBiaya);

					session.saveOrUpdate(hargaJualItem);
					row.setAttribute("hargaJualItem", hargaJualItem);

					for (Object oo : row.getChildren()) {

						if (oo instanceof Hbox) {

							MyDoublebox o = (MyDoublebox) ((Hbox) oo).getChildren().get(0);
							MyDoublebox o1 = (MyDoublebox) ((Hbox) oo).getChildren().get(1);

							Biaya biaya = (Biaya) o.getAttribute("biaya");
							biaya.setHargaJualItem(hargaJualItem);
							biaya.setJumlah(o.getValue());
							biaya.setPersen(o1.getValue());

							session.saveOrUpdate(biaya);

						}

					}
				}
			}

			return true;
		}

		public EventListener ubahStatusRubahSemuaEventListener = new EventListener() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public void onEvent(Event arg0) throws Exception {

				List<Row> rows = gridHargaJual.getRows().getChildren();
				if (!rows.isEmpty()) {
					int i = 0;
					Row mainRow = rows.get(0);
					for (Row row : rows) {
						HargaJualItem hargaJualItem = (HargaJualItem) row.getAttribute("hargaJualItem");
						if (hargaJualItem != null) {
							if (i != 0) {
								if (semuahargasama.isChecked()) {

									int index = 0;
									for (Object oo : row.getChildren()) {

										if (oo instanceof Checkbox) {
											Checkbox o1 = ((Checkbox) mainRow.getChildren().get(index));
											((Checkbox) oo).setChecked(o1.isChecked());
											((Checkbox) oo).setDisabled(true);
										}

										if (oo instanceof MyDoublebox) {
											MyDoublebox o1 = ((MyDoublebox) mainRow.getChildren().get(index));
											((MyDoublebox) oo).setValue(o1.getValue());
											((MyDoublebox) oo).setDisabled(true);

											Biaya biaya = (Biaya) ((MyDoublebox) oo).getAttribute("biaya");
											biaya.setJumlahTotal(o1.getValue());
											((MyDoublebox) oo).setAttribute("biaya", biaya);
										}

										if (oo instanceof Hbox) {

											Object o = ((Hbox) oo).getChildren().get(0);
											MyDoublebox o1 = (MyDoublebox) ((Hbox) mainRow.getChildren().get(index))
													.getChildren().get(0);

											if (o instanceof MyDoublebox) {
												((MyDoublebox) o).setValue(o1.getValue());
												((MyDoublebox) o).setDisabled(true);

												Biaya biaya = (Biaya) ((MyDoublebox) o).getAttribute("biaya");
												biaya.setJumlah(o1.getValue());
												((MyDoublebox) o).setAttribute("biaya", biaya);
											}

											o = ((Hbox) oo).getChildren().get(1);
											o1 = (MyDoublebox) ((Hbox) mainRow.getChildren().get(index)).getChildren()
													.get(1);

											if (o instanceof MyDoublebox) {
												((MyDoublebox) o).setValue(o1.getValue());
												((MyDoublebox) o).setDisabled(true);

												Biaya biaya = (Biaya) ((MyDoublebox) o).getAttribute("biaya");
												biaya.setPersen(o1.getValue());
												((MyDoublebox) o).setAttribute("biaya", biaya);
											}
										}
										index++;
									}

								} else {
									for (Object oo : row.getChildren()) {
										if (oo instanceof Hbox) {
											Object o = ((Hbox) oo).getChildren().get(0);
											((MyDoublebox) o).setDisabled(false);
											o = ((Hbox) oo).getChildren().get(1);
											((MyDoublebox) o).setDisabled(false);
										}

										if (oo instanceof MyDoublebox) {
											((MyDoublebox) oo).setDisabled(false);
										}

										if (oo instanceof Checkbox) {
											((Checkbox) oo).setDisabled(false);
										}
									}
								}
							}
							i++;

							Checkbox pembagianBiayaDalamPersen = (Checkbox) row.getChildren().get(1);

							if (!pembagianBiayaDalamPersen.isChecked()) {
								try {
									Double totalNilai = 0.0;
									for (Object oo : row.getChildren()) {
										if (oo instanceof Hbox) {
											MyDoublebox o = (MyDoublebox) ((Hbox) oo).getChildren().get(0);
											totalNilai += o.getValue() == null ? 0.0 : o.getValue();
										}
									}

									System.out.println("totalNilai => " + totalNilai);

									hargaJualItem.setHargaJual(totalNilai);

									MyDoublebox total = (MyDoublebox) row.getChildren()
											.get(row.getChildren().size() - 1);

									total.setValue(hargaJualItem.getHargaJual());
									row.setAttribute("hargaJualItem", hargaJualItem);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirs/util/CommonItem.java:1290");
								}
							}
						}
					}
				}
			}
		};

		@SuppressWarnings("unchecked")
		public void initHargaJual(final ItemMedis item, final Tabpanel tabpanel, final OnSave onSave,
				final TarifKhususPunyaItem tarifKhususPunyaItem) throws Exception {

			final Boolean delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

			final Center c = new Center();

			Common.clear(tabpanel);

			Borderlayout bl = new Borderlayout();
			bl.setParent(tabpanel);

			North n = new North();
			n.setParent(bl);

			c.setParent(bl);
			ais.ui.util.ZkCompat.setFlex(c, true);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("25px");
			toolbar.setParent(n);
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Tambah akun lain-nya", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {

					if (!onSave.onSave(event)) {
						return;
					}

					List<JenisBiaya> jenisBiayas = CommonTarifItem.getJenisBiayas(item, tarifKhususPunyaItem);

					AmbilDataJenisBiayaBanyak ambilDataJenisBiayaBanyak = new AmbilDataJenisBiayaBanyak(jenisBiayas,
							JenisBiaya.TIPE_ITEM);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataJenisBiayaBanyak);
					ambilDataJenisBiayaBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<JenisBiaya> jenisBiayas = (List<JenisBiaya>) arg0.getData();

							Session session = HibernateUtil.currentSession();
							List<Row> rows = gridHargaJual.getRows().getChildren();
							if (!rows.isEmpty()) {
								Row mainRow = rows.get(0);
								HargaJualItem hargaJualItem = (HargaJualItem) mainRow.getAttribute("hargaJualItem");
								for (JenisBiaya jenisBiaya : jenisBiayas) {
									Integer count = ((Number) session.createCriteria(Biaya.class)
											.add(Restrictions.isNull("detailTransaksiLayanan"))
											.add(Restrictions.isNull("detailTransaksi"))
											.add(Restrictions.eq("hargaJualItem", hargaJualItem))
											.add(Restrictions.eq("jenisBiaya", jenisBiaya))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();
									if (count.equals(0)) {
										Biaya biaya = new Biaya();
										biaya.setHargaJualItem(hargaJualItem);
										biaya.setJenisBiaya(jenisBiaya);
										session.save(biaya);
									}
								}
							}

							initHargaJual(item, tabpanel, onSave, tarifKhususPunyaItem);

							ubahStatusRubahSemuaEventListener.onEvent(null);
						}
					});
					ambilDataJenisBiayaBanyak.setWidth("750px");
					ambilDataJenisBiayaBanyak.setHeight("97%");
					ambilDataJenisBiayaBanyak.setVisible(true);
					ambilDataJenisBiayaBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			toolbar.appendChild(semuahargasama = new Checkbox("Semua harga sama"));
			semuahargasama.setChecked(item.getSemuahargasama());

			semuahargasama.addEventListener("onCheck", ubahStatusRubahSemuaEventListener);

			Groupbox groupbox = new Groupbox();
			c.appendChild(groupbox);

			Session session = HibernateUtil.currentSession();

			TreeSet<JenisBiaya> jenisBiayas = new TreeSet<JenisBiaya>(
					CommonTarifItem.getJenisBiayas(item, tarifKhususPunyaItem));

			groupbox.appendChild(new Caption("Biaya Item"));

			gridHargaJual = new Grid();
			gridHargaJual.setParent(groupbox);
			gridHargaJual.setWidth("100%");
			gridHargaJual.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(gridHargaJual);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Kelas");
			column.setWidth("20%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("%");
			column.setWidth("3%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Berubah");
			column.setWidth("5%");

			for (final JenisBiaya jenisBiaya : jenisBiayas) {

				column = new Column();
				column.setParent(columns);

				button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
				button.setWidth("100%");
				button.setStyle("font-size:xx-small;text-align: left;");
				Label label = new Label(jenisBiaya.getNama());
				label.setMultiline(true);
				label.setTooltiptext(jenisBiaya.getNama() + "-"
						+ (jenisBiaya.getAkun() == null ? "" : jenisBiaya.getAkun().toString()));
				label.setWidth("90%");
				column.appendChild(new Hbox(new Component[] { button, label }));

				button.setTooltiptext(jenisBiaya.getNama());
				button.setVisible(delete && item.getId() != null && jenisBiayas.size() > 1);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin menghapus data ini? Perlu diketahui bahwa data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = HibernateUtil.currentSession();

												for (Biaya biaya : CommonTarifItem.getBiayaPerJenis(item,
														tarifKhususPunyaItem, jenisBiaya)) {
													session.delete(biaya);
												}

												initHargaJual(item, tabpanel, onSave, tarifKhususPunyaItem);

												ubahStatusRubahSemuaEventListener.onEvent(null);

											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sirs/util/CommonItem.java:1458");
												MyMessageboxConfig.show(Common.pesan(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berkaitan dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau pindahkan terlebih dahulu seluruh data yang berkaitan; (2) periksa kembali keterkaitan antar data; (3) hubungi administrator apabila kendala masih berlanjut.",
																e.getMessage()));
											}

										}

									}
								});

					}
				});

			}

			column = new Column();
			column.setParent(columns);
			column.setLabel("Total");
			column.setAlign("right");

			Rows rows = new Rows();
			rows.setParent(gridHargaJual);

			List<KelasPerawatan> kelasPerawatans = session.createCriteria(KelasPerawatan.class)
					.addOrder(Order.asc("id")).list();

			for (KelasPerawatan kelasPerawatan : kelasPerawatans) {

				HargaJualItem hargaJualItem;

				if (tarifKhususPunyaItem == null) {
					hargaJualItem = CommonTarifItem.getHargaJualItem(item, kelasPerawatan);
				} else {
					hargaJualItem = CommonTarifItem.getHargaJualItem(tarifKhususPunyaItem, kelasPerawatan);
				}

				final Checkbox pembagianBiayaDalamPersen = new Checkbox();
				final Checkbox hargaBisaDirubahSaatTransaksi = new Checkbox();

				pembagianBiayaDalamPersen.setChecked(hargaJualItem.getPembagianBiayaDalamPersen());
				hargaBisaDirubahSaatTransaksi.setChecked(hargaJualItem.getHargaBisaDirubahSaatTransaksi());

				final MyDoublebox total = new MyDoublebox(hargaJualItem.getHargaJual());
				total.setReadonly(!pembagianBiayaDalamPersen.isChecked());

				total.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						List<Row> rows = gridHargaJual.getRows().getChildren();
						if (!rows.isEmpty()) {
							for (Row row : rows) {
								for (Object oo : row.getChildren()) {

									if (oo instanceof MyDoublebox && oo == total) {
										Biaya biaya = (Biaya) ((MyDoublebox) oo).getAttribute("biaya");
										biaya.getHargaJualItem().setHargaJual(total.getValue());

										biaya.getHargaJualItem()
												.setPembagianBiayaDalamPersen(pembagianBiayaDalamPersen.isChecked());

										((MyDoublebox) oo).setAttribute("biaya", biaya);

										for (Object ooo : row.getChildren()) {

											if (ooo instanceof Hbox) {

												MyDoublebox harga = (MyDoublebox) ((Hbox) ooo).getChildren().get(0);
												MyDoublebox persen = (MyDoublebox) ((Hbox) ooo).getChildren().get(1);

												Biaya myBiaya = (Biaya) harga.getAttribute("biaya");
												myBiaya.setJumlahTotal(total.getValue());
												myBiaya.getHargaJualItem().setHargaJual(total.getValue());

												myBiaya.getHargaJualItem().setPembagianBiayaDalamPersen(
														pembagianBiayaDalamPersen.isChecked());

												harga.setAttribute("biaya", myBiaya);
												persen.setAttribute("biaya", myBiaya);

												harga.setValue(myBiaya.getJumlah());
												persen.setValue(myBiaya.getPersen());
											}

										}

									}
								}
							}
						}

						ubahStatusRubahSemuaEventListener.onEvent(arg0);
					}
				});

				final Row row = new Row();
				row.setAttribute("kelasPerawatan", kelasPerawatan);
				row.setAttribute("hargaJualItem", hargaJualItem);
				row.setStyle("border:0px;background: transparent;");
				row.setParent(rows);
				row.appendChild(new Label(kelasPerawatan.getNama()));

				row.appendChild(pembagianBiayaDalamPersen);

				final EventListener pembagianBiayaDalamPersenEventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						for (Object oo : row.getChildren()) {
							if (oo instanceof Hbox) {
								MyDoublebox o1 = (MyDoublebox) ((Hbox) oo).getChildren().get(0);
								MyDoublebox o2 = (MyDoublebox) ((Hbox) oo).getChildren().get(1);

								o1.setReadonly(pembagianBiayaDalamPersen.isChecked());
								o2.setReadonly(!pembagianBiayaDalamPersen.isChecked());
							}

							if (oo instanceof MyDoublebox) {
								MyDoublebox o1 = (MyDoublebox) (oo);
								o1.setReadonly(!pembagianBiayaDalamPersen.isChecked());
							}
						}

						total.setReadonly(!pembagianBiayaDalamPersen.isChecked());

						ubahStatusRubahSemuaEventListener.onEvent(arg0);

					}
				};

				pembagianBiayaDalamPersen.addEventListener("onCheck", pembagianBiayaDalamPersenEventListener);

				row.appendChild(hargaBisaDirubahSaatTransaksi);

				hargaBisaDirubahSaatTransaksi.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (hargaBisaDirubahSaatTransaksi.isChecked()) {
							pembagianBiayaDalamPersen.setChecked(true);
							pembagianBiayaDalamPersenEventListener.onEvent(arg0);
						} else {
							ubahStatusRubahSemuaEventListener.onEvent(arg0);
						}
					}
				});

				final Row rowAkun = new Row();
				rowAkun.setVisible(false);
				rowAkun.setStyle("border:0px;background: transparent;");
				rowAkun.setParent(rows);
				rowAkun.appendChild(new Label());

				for (JenisBiaya jenisBiaya : jenisBiayas) {
					Biaya biaya = ((Biaya) session.createCriteria(Biaya.class)
							.add(Restrictions.eq("jenisBiaya", jenisBiaya))
							.add(hargaJualItem.getId() == null ? Restrictions.sqlRestriction("false")
									: Restrictions.eq("hargaJualItem", hargaJualItem))

							.createAlias("hargaJualItem", "hargaJualItem")

							.add(Restrictions.isNull("detailTransaksiLayanan"))
							.add(Restrictions.isNull("detailTransaksi"))

							.setMaxResults(1).uniqueResult());
					if (biaya == null) {
						biaya = new Biaya();
						biaya.setHargaJualItem(hargaJualItem);
						biaya.setJenisBiaya(jenisBiaya);
					}

					final Biaya tempBiaya = biaya;

					final MyDoublebox boxBiaya = new MyDoublebox();
					final MyDoublebox boxBiayaPersen = new MyDoublebox();

					boxBiaya.setReadonly(pembagianBiayaDalamPersen.isChecked());
					boxBiayaPersen.setReadonly(!pembagianBiayaDalamPersen.isChecked());

					final AmbilDataAkunKreditBanbox akun = new AmbilDataAkunKreditBanbox();

					boxBiaya.setAttribute("biaya", tempBiaya);
					boxBiaya.setValue(biaya.getJumlah());

					boxBiayaPersen.setAttribute("biaya", tempBiaya);
					boxBiayaPersen.setValue(biaya.getPersen());

					total.setAttribute("biaya", tempBiaya);

					boxBiaya.setWidth("100%");
					boxBiayaPersen.setWidth("70%");
					Hbox hbox;
					row.appendChild(hbox = new Hbox(new Component[] { boxBiaya, boxBiayaPersen, new Label(" %") }));
					hbox.setWidth("100%");
					hbox.setPack("center");
					hbox.setAlign("center");

					boxBiaya.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							tempBiaya.getHargaJualItem()
									.setPembagianBiayaDalamPersen(pembagianBiayaDalamPersen.isChecked());

							tempBiaya.setJumlah(boxBiaya.getValue());
							tempBiaya.setAkun((Akun) akun.getAttribute("akun"));

							Double totalNilai = 0.0;
							for (Object oo : row.getChildren()) {
								if (oo instanceof Hbox) {
									MyDoublebox o = (MyDoublebox) ((Hbox) oo).getChildren().get(0);
									totalNilai += o.getValue() == null ? 0.0 : o.getValue();
								}
							}

							System.out.println("totalNilai => " + totalNilai);

							tempBiaya.getHargaJualItem().setHargaJual(totalNilai);
							total.setValue(totalNilai);

							tempBiaya.setJumlahTotal(total.getValue());
							boxBiayaPersen.setValue(tempBiaya.getPersen());
							boxBiaya.setAttribute("biaya", tempBiaya);
							total.setAttribute("biaya", tempBiaya);

							ubahStatusRubahSemuaEventListener.onEvent(arg0);
						}
					});

					boxBiayaPersen.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							tempBiaya.getHargaJualItem()
									.setPembagianBiayaDalamPersen(pembagianBiayaDalamPersen.isChecked());

							tempBiaya.setPersen(boxBiayaPersen.getValue());
							tempBiaya.setAkun((Akun) akun.getAttribute("akun"));

							tempBiaya.setJumlahTotal(total.getValue());
							boxBiaya.setValue(tempBiaya.getJumlah());
							boxBiayaPersen.setAttribute("biaya", tempBiaya);
							total.setAttribute("biaya", tempBiaya);

							ubahStatusRubahSemuaEventListener.onEvent(arg0);
						}
					});

					akun.setAttribute("biaya", tempBiaya);
					akun.setValue(tempBiaya.getAkun().toString());
					akun.setAttribute("akun", tempBiaya.getAkun());
					akun.setWidth("90%");
					akun.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tempBiaya.setJumlah(boxBiaya.getValue());
							tempBiaya.setAkun((Akun) akun.getAttribute("akun"));
							boxBiaya.setAttribute("biaya", tempBiaya);
						}
					});
					rowAkun.appendChild(akun);

					pembagianBiayaDalamPersenEventListener.onEvent(null);
				}
				row.appendChild(total);
				rowAkun.appendChild(new Label());
			}

			ubahStatusRubahSemuaEventListener.onEvent(null);

		}
	}
}
