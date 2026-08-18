package ais.action.master.helper.util;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.library.util.BigFile;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaKategoriItem;
import ais.database.model.library.ItemPunyaPengarang;
import ais.database.model.library.KategoriItem;
import ais.database.model.library.Penerbit;
import ais.database.model.library.Pengarang;

public class ItemImporterProcessor extends TimerTask {

	private String localIp = "";

	public ItemImporterProcessor() {
		InetAddress thisIp;
		try {
			thisIp = InetAddress.getLocalHost();
			localIp = thisIp.getHostName();
			System.out.println("IP:" + localIp);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:36");
			// TODO Auto-generated catch block
//			Common.tampilErrorJikaAdmin(e); 
		}

	}

	@Override
	public void run() {
		try {
			doProcess();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
	}

	@SuppressWarnings("unused")
	private void doProcess() throws Exception {
		if (true) {
			return;
		}

		Konfigurasi auto_proses_tunggakan = Common.getKonfigurasi(
				"item_importer_processor", Konfigurasi.AKTIF,
				"/home/library/ol_dump_editions_2012-03-31.txt", "", "");

		File myfile = new File(auto_proses_tunggakan.getInfo1() + "");
		System.out
				.println("================================ ItemImporterProcessor ================================== "
						+ auto_proses_tunggakan.getNilai()
						+ ", lokasi = "
						+ myfile.getAbsolutePath()
						+ ", exist = "
						+ myfile.exists());

		if (auto_proses_tunggakan.getNilai().equals(Konfigurasi.AKTIF)
				&& myfile.exists()) {

			BigFile file = new BigFile(myfile.getAbsolutePath());

			for (String line : file) {

				try {
					String[] temp = line.split("	");
					String j = temp[temp.length - 1];
					JSONObject object = new JSONObject(j);

					String isbn_13 = "";
					try {
						isbn_13 = object.getString("isbn_13")
								.replaceAll("\\[", "").replaceAll("\\]", "")
								.replaceAll("\"", "").trim();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:88");
					}

					if (isbn_13 == null || isbn_13.trim().equals("")) {
						continue;
					}

					String revision = "";
					try {
						revision = (object.getString("revision").replaceAll(
								"\\[", "").replaceAll("\\]", "")).replaceAll(
								"\"", "");
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:100");
					}

					Session session = HibernateUtil.currentNativeSession();
					
					Integer count = ((Number) session
							.createCriteria(Item.class)
							.add(Restrictions.eq("isbn", isbn_13))
							.add(Restrictions.eq("revisi", revision))
							.setProjection(Projections.rowCount())
							.uniqueResult()).intValue();

					System.out.println("isbn_13 = " + isbn_13 + ", revision = "
							+ revision + ", count = " + count);

					if (count.equals(0)) {
						// Thread.sleep(100);
						Item item = (Item) (session.createCriteria(Item.class)
								.add(Restrictions.eq("isbn", isbn_13))
								.setMaxResults(1).uniqueResult());

						if (item == null) {
							item = new Item();
						}

						try {
							item.setNama(object.getString("title")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:129");
						}
						try {
							item.setTema(object.getString("subtitle")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:135");
						}
						try {
							item.setIsbn10(object.getString("isbn_10")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:141");
						}

						try {
							item.setLccn(object.getString("lccn")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:148");
						}

						try {
							item.setWaktuterbit(object
									.getString("publish_date")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:156");
						}

						try {
							item.setDewey_decimal_class(object
									.getString("dewey_decimal_class")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:164");
						}

						try {
							item.setLc_classifications(object
									.getString("lc_classifications")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:172");
						}

//						try {
//							item.setBy_statement(object
//									.getString("by_statement")
//									.replaceAll("\\[", "")
//									.replaceAll("\\]", "").replaceAll("\"", ""));
//						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:180");
//						}

						try {
							item.setKey((object.get("key")+"")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:187");
						}

						try {
							item.setPenaklikan(object.getString("pagination")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:194");
						}

						try {
							item.setTempatterbit(object
									.getJSONArray("publish_places")
									.getString(0).replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:202");
						}

						try {
							item.setCatatan(object.getString("notes")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:209");
						}

						try {
							item.setPublish_country(object
									.getString("publish_country")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:217");
						}

						try {
							item.setHalaman(object.getInt("number_of_pages"));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:222");
						}

						try {
							item.setEdisi(object.getString("edition_name")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:229");
						}

						try {
							item.setBahasa((object.getJSONArray("languages")
									.getJSONObject(0).get("key")+"")
									.replaceAll("/languages/", "")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", ""));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:238");
						}

						try {
							item.setTahun(Integer.parseInt(object
									.getString("publish_date")
									.replaceAll("\\[", "")
									.replaceAll("\\]", "").replaceAll("\"", "")
									.substring(0, 4)));
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:247");
						}

						item.setIsbn(isbn_13);

						if (item.getRevisi() == null
								|| !item.getRevisi().equals(revision)) {
							item.setRevisi(revision);
							System.out.println("Save or update item => " + item
									+ " - ID => " + item.getId() + ", isbn => "
									+ item.getIsbn());
							session.getTransaction().begin();
							session.saveOrUpdate(item);
							session.getTransaction().commit();

							List<String> strings;
							String p;
							try {
								JSONArray pengarangs = object
										.getJSONArray("authors");
								strings = new ArrayList<String>();
								for (int i = 0; i < pengarangs.length(); i++) {
									try {
										String key = (pengarangs
												.getJSONObject(i)
												.get("key")+"")
												.replaceAll("/authors/", "")
												.trim();

										Pengarang pengarang = (Pengarang) session
												.createCriteria(Pengarang.class)
												.add(Restrictions.eq("kode",
														key)).setMaxResults(1)
												.uniqueResult();

										String namaA = "";
										try {
											namaA = (pengarangs
													.getJSONObject(i)
													.getString("name"));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:287");
										}

										System.out.println("author key = "
												+ key + ", nama = " + namaA);

										if (pengarang == null
												&& !namaA.equals("")) {
											pengarang = new Pengarang();
											pengarang.setAktif(true);
											pengarang.setNama(namaA);
											pengarang.setKeterangan(namaA);
											pengarang.setKode(key);
											session.getTransaction().begin();
											session.save(pengarang);
											session.getTransaction().commit();
										}

										if (pengarang != null) {
											if (!pengarang.getNama().trim()
													.isEmpty()) {
												strings.add(pengarang.getNama());
											}
											ItemPunyaPengarang itemPunyaPengarang = (ItemPunyaPengarang) session
													.createCriteria(
															ItemPunyaPengarang.class)
													.add(Restrictions.eq(
															"item", item))
													.add(Restrictions.eq(
															"pengarang",
															pengarang))
													.setMaxResults(1)
													.uniqueResult();
											if (itemPunyaPengarang == null) {
												itemPunyaPengarang = new ItemPunyaPengarang();
												itemPunyaPengarang
														.setItem(item);
												itemPunyaPengarang
														.setPengarang(pengarang);
												session.getTransaction()
														.begin();
												session.save(itemPunyaPengarang);
												session.getTransaction()
														.commit();
											}
										}
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e); 
									}
								}

								p = strings.toString().replaceAll("\\[", "")
										.replaceAll("\\]", "");
								item.setPengarangs(p);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:341");
							}

							try {
								JSONArray subjects = object
										.getJSONArray("subjects");
								strings = new ArrayList<String>();
								for (int i = 0; i < subjects.length(); i++) {
									String subject = subjects.getString(i)
											.trim();
									if (subject == null
											|| subject.trim().equals("")) {
										continue;
									}
									KategoriItem kategoriItem = (KategoriItem) session
											.createCriteria(KategoriItem.class)
											.add(Restrictions.ilike("nama",
													subject)).setMaxResults(1)
											.uniqueResult();
									if (kategoriItem == null) {
										kategoriItem = new KategoriItem();
										kategoriItem.setDefaultItem(true);
										kategoriItem.setNama(subject);
										kategoriItem.setKeterangan(subject);

										session.getTransaction().begin();
										session.save(kategoriItem);
										session.getTransaction().commit();
									}

									if (kategoriItem != null) {
										strings.add(kategoriItem.getNama());
										ItemPunyaKategoriItem itemPunyaKategoriItem = (ItemPunyaKategoriItem) session
												.createCriteria(
														ItemPunyaKategoriItem.class)
												.add(Restrictions.eq("item",
														item))
												.add(Restrictions.eq(
														"kategoriItem",
														kategoriItem))
												.setMaxResults(1)
												.uniqueResult();
										if (itemPunyaKategoriItem == null) {
											itemPunyaKategoriItem = new ItemPunyaKategoriItem();
											itemPunyaKategoriItem.setItem(item);
											itemPunyaKategoriItem
													.setKategoriItem(kategoriItem);
											session.getTransaction().begin();
											session.save(itemPunyaKategoriItem);
											session.getTransaction().commit();
										}
									}
								}
								p = strings.toString().replaceAll("\\[", "")
										.replaceAll("\\]", "");
								item.setKategories(p);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:397");

							}

							try {
								JSONArray publishers = object
										.getJSONArray("publishers");
								for (int i = 0; i < publishers.length(); i++) {
									String publisher = publishers.getString(i)
											.trim();
									if (publisher == null
											|| publisher.trim().equals("")) {
										continue;
									}
									Penerbit penerbit = (Penerbit) session
											.createCriteria(Penerbit.class)
											.add(Restrictions.ilike("nama",
													publisher))
											.setMaxResults(1).uniqueResult();
									if (penerbit == null) {
										penerbit = new Penerbit();
										penerbit.setNama(publisher);
										penerbit.setKeterangan(publisher);

										session.getTransaction().begin();
										session.save(penerbit);
										session.getTransaction().commit();
									}

									item.setPenerbit(penerbit);
									break;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/ItemImporterProcessor.java:429");
							}

							String u = "http://covers.openlibrary.org/b/isbn/"
									+ item.getIsbn10() + "-M.jpg";
							System.out.println("u = " + u);
							item.setImageUrl(u);

							session.getTransaction().begin();
							session.saveOrUpdate(item);
							session.getTransaction().commit();
						}
					}

					
					HibernateUtil.closeSession();

				} catch (Exception e) {
					HibernateUtil.rollbackTransaction();
					Common.tampilErrorJikaAdmin(e); 
				}
			}

		}
	}

	public static void main(String[] argv) {
		String key = "/authors/OL1667738A".replaceAll("/authors/", "").trim();
		System.out.println(key);
	}

}
