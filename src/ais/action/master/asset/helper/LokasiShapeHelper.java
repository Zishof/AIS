package ais.action.master.asset.helper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.zkoss.gmaps.Gmaps;
import org.zkoss.gmaps.Gmarker;
import org.zkoss.gmaps.Gpolygon;
import org.zkoss.gmaps.Gpolyline;
import org.zkoss.gmaps.event.MapDropEvent;
import org.zkoss.gmaps.event.MapMouseEvent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.North;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Konfigurasi;
import ais.database.model.asset.Lokasi;
import ais.ui.util.MyButtonConfig;

public class LokasiShapeHelper {

	private Lokasi lokasi;
	protected boolean editable = false;
	protected boolean edit = false;
	private Gmaps theMap;
	private Gmarker theMarker = new Gmarker();
	private Gpolygon thePolygon = new Gpolygon();
	private LinkedList<Gmarker> linkBuffer;
	private EventListener eventListener;
	protected boolean[][] conTable;

	protected HashMap<Gmarker, HashSet<Gpolyline>> indexMap;

	public LokasiShapeHelper(Lokasi lokasi, EventListener eventListener) {
		indexMap = new HashMap<Gmarker, HashSet<Gpolyline>>();
		this.lokasi = lokasi;
		this.eventListener = eventListener;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		linkBuffer = new LinkedList<Gmarker>();
	}

	public Borderlayout display() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setHeight("45px");
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		final Center center = new Center();
		center.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("40px");
		toolbar.setVisible(edit);
		toolbar.setParent(north);

		final MyButtonConfig shape = new MyButtonConfig("Reset dan hapus kooordinat", "/img/control_service.png");
		shape.setHeight("40px");
		shape.setVisible(false);
		shape.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				lokasi.setCoordinate("");
				eventListener.onEvent(new Event("", theMap, lokasi));
				Common.clear(center);
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initMap(center);
						linkBuffer.clear();
						redrawPolygon();
					}
				});
			}
		});
		toolbar.appendChild(shape);

		final MyButtonConfig ubah = new MyButtonConfig("Ubah koordinat", "/img/svg/edit-box-line.svg");
		ubah.setVisible(false);
		ubah.setHeight("40px");
		ubah.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				if (!editable) {
					editable = true;
					ubah.setLabel("Kunci koordinat");
				} else {
					editable = false;
					ubah.setLabel("Ubah koordinat");
				}
				theMarker.setDraggingEnabled(editable);
			}
		});
		toolbar.appendChild(ubah);

		final MyButtonConfig arahkan = new MyButtonConfig("Arahkan koordinat ke default", "/img/svg/edit-box-line.svg");
		arahkan.setVisible(false);
		arahkan.setHeight("40px");
		arahkan.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				Double strLat = -6.30611698593644;
				Double strLng = 106.753340363502;
				try {
					strLat = Double.parseDouble(Common
							.getKonfigurasi("default_lat", Konfigurasi.AKTIF, "" + lokasi.getLat(), "", "").getInfo1());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}
				try {
					strLng = Double.parseDouble(Common
							.getKonfigurasi("default_lng", Konfigurasi.AKTIF, "" + lokasi.getLng(), "", "").getInfo1());
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e); 
				}
				lokasi.setLat(strLat);
				lokasi.setLng(strLng);
//				try {
//					List<String> strings = AssetHelper.getAdsress(theMap.getLat(), theMap.getLng());
//					lokasi.setDetailAlamat(strings + "");
//					if (strings.size() != 0) {
//						lokasi.setAlamat(strings.get(0));
//					}
//				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/LokasiShapeHelper.java:133");
//					Common.tampilErrorJikaAdmin(e); 
//				}
				eventListener.onEvent(new Event("", theMap, lokasi));

				Common.clear(center);
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initMap(center);
						linkBuffer.clear();
						redrawPolygon();
					}
				});
			}
		});
		toolbar.appendChild(arahkan);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				shape.setVisible(true);
				ubah.setVisible(true);
				arahkan.setVisible(true);
				initMap(center);
			}
		});

		return borderlayout;
	}

	public void initMap(Component center) throws Exception {
		Double strLat = -6.30611698593644;
		Double strLng = 106.753340363502;
		try {
			strLat = Double.parseDouble(
					Common.getKonfigurasi("default_lat", Konfigurasi.AKTIF, "" + lokasi.getLat(), "", "").getInfo1());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}
		try {
			strLng = Double.parseDouble(
					Common.getKonfigurasi("default_lng", Konfigurasi.AKTIF, "" + lokasi.getLng(), "", "").getInfo1());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
		}

		theMap = new Gmaps();
		theMap.setWidth("100%");
		theMap.setHeight("100%");
		theMap.setDroppable("true");
		theMap.setMapType("hybrid");
		theMap.setZoom(18);
		theMap.setShowScaleCtrl(true);
		theMap.setShowSmallCtrl(true);
		theMap.setParent(center);

		thePolygon.setParent(theMap);

		if (lokasi.getId() == null) {
			theMap.setLat(strLat);
			theMap.setLng(strLng);
			theMarker.setLat(strLat);
			theMarker.setLng(strLng);
		} else {
			theMarker.setLat(lokasi.getLat());
			theMarker.setLng(lokasi.getLng());
			theMap.setLat(lokasi.getLat());
			theMap.setLng(lokasi.getLng());

			String coordinate = lokasi.getCoordinate();

			String[] coList = coordinate.split(";");
			for (int i = 0; i < coList.length; i++) {
				try {
					String[] point = coList[i].split(",");
					Gmarker marker = new Gmarker();
					marker.setLat(Double.parseDouble(point[0]));
					marker.setLng(Double.parseDouble(point[1]));
					marker.setDraggingEnabled(true);
					marker.setParent(theMap);
					linkBuffer.add(marker);
					thePolygon.addPoint(marker.getLat(), marker.getLng(), 3);
				} catch (Exception e1) {
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/asset/helper/LokasiShapeHelper.java:219");
				}
			}
		}

		theMarker.setIconImage("http://www.google.com/mapfiles/ms/micons/red.png");
		theMarker.setDraggingEnabled(editable);

		theMarker.setParent(theMap);

		theMap.addEventListener("onMapDrop", new EventListener() {
			public void onEvent(Event event) throws Exception {

				String domain = theMap.getBaseDomain();
				System.out.println("============ onMapDrop =========== " + domain);
				if (editable) {

					MapDropEvent mde = (MapDropEvent) event;
					if (mde.getDragged() == theMarker) {
						theMarker.setLat(mde.getLat()); 
						theMarker.setLng(mde.getLng()); 
					}
					lokasi.setLat(theMarker.getLat());
					lokasi.setLng(theMarker.getLng());
					theMap.panTo(theMarker.getLat(), theMarker.getLng());

					try {
						Gmarker marker = (Gmarker) mde.getDragged();
						int index = linkBuffer.indexOf(marker);
						linkBuffer.remove(marker);
						marker.setLat(mde.getLat());
						marker.setLng(mde.getLng());
						linkBuffer.add(index, marker);

						StringBuffer coordinate = new StringBuffer();
						Iterator<Gmarker> i = linkBuffer.iterator();
						while (i.hasNext()) {
							Gmarker mymarker = i.next();
							coordinate.append(mymarker.getLat() + "," + mymarker.getLng() + ";");
						}
						lokasi.setCoordinate(coordinate.toString());
					} catch (Exception e) {

						StringBuffer coordinate = new StringBuffer();
						Iterator<Gmarker> i = linkBuffer.iterator();
						while (i.hasNext()) {
							Gmarker mymarker = i.next();
							coordinate.append(mymarker.getLat() + "," + mymarker.getLng() + ";");
						}
						lokasi.setCoordinate(coordinate.toString());

					}

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
//							try {
//								List<String> strings = AssetHelper.getAdsress(theMap.getLat(), theMap.getLng());
//								lokasi.setDetailAlamat(strings + "");
//								if (strings.size() != 0) {
//									lokasi.setAlamat(strings.get(0));
//								}
//							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/LokasiShapeHelper.java:282");
//								Common.tampilErrorJikaAdmin(e); 
//							}
							eventListener.onEvent(new Event("", theMap, lokasi));
							redrawPolygon();
						}
					});
				}
			}
		});

		theMap.addEventListener("onMapClick", new EventListener() {
			public void onEvent(Event e) throws Exception {
				if (editable) {
					MapMouseEvent mme = (MapMouseEvent) e;
					Gmarker marker = new Gmarker();
					marker.setLat(mme.getLat());
					marker.setLng(mme.getLng());
					marker.setDraggingEnabled(true);
					marker.setParent(theMap);
					linkBuffer.add(marker);
					thePolygon.addPoint(mme.getLat(), mme.getLng(), 3);

					StringBuffer coordinate = new StringBuffer();
					Iterator<Gmarker> i = linkBuffer.iterator();
					while (i.hasNext()) {
						Gmarker mymarker = i.next();
						coordinate.append(mymarker.getLat() + "," + mymarker.getLng() + ";");
					}
					lokasi.setCoordinate(coordinate.toString());
					eventListener.onEvent(new Event("", theMap, lokasi));

				}
			}
		});

		theMap.panTo(theMarker.getLat(), theMarker.getLng());
		eventListener.onEvent(new Event("", theMap, lokasi));
	}

	public void onMapClick(ForwardEvent e) {
		MapMouseEvent mme = (MapMouseEvent) e.getOrigin();
		if (editable) {
			thePolygon.addPoint(mme.getLat(), mme.getLng(), 3);
		}

	}

	/**
	 * Register a Gmarker with a GPolyline, we use HashSet to hold the data,
	 * saving memory and search time.
	 * 
	 * @param g1
	 * @param gplIndex
	 */
	public void bind(Gmarker g1, Gpolyline gplIndex) {
		HashSet<Gpolyline> gpSet = indexMap.get(g1);
		if (gpSet != null) {
			// add to Set and reinsert.
			gpSet.add(gplIndex);
			indexMap.put(g1, gpSet);
		} else {
			gpSet = new HashSet<Gpolyline>();
			gpSet.add(gplIndex);
			indexMap.put(g1, gpSet);
		}
	}

	public void redrawPolygon() {
		thePolygon.detach();
		this.thePolygon = new Gpolygon();
		thePolygon.setParent(theMap);
		Iterator<Gmarker> i = linkBuffer.iterator();
		while (i.hasNext()) {
			Gmarker marker = i.next();
			thePolygon.addPoint(marker.getLat(), marker.getLng(), 3);
		}
	}

}
