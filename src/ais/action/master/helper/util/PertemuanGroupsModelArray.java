package ais.action.master.helper.util;

import java.util.Comparator;

import org.zkoss.zul.GroupsModelArray;

import ais.database.model.Pertemuan;

public class PertemuanGroupsModelArray extends GroupsModelArray {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9178565020194034560L;

	@SuppressWarnings("rawtypes")
	public PertemuanGroupsModelArray(Pertemuan[] data, Comparator cmpr) {
		super(data, cmpr);

	}

	protected Object createGroupHead(Pertemuan[] groupdata, int index, int col) {
		return new Object[] { groupdata[0], index, col };
	}

	// Create GroupFoot Data
	protected Object createGroupFoot(Pertemuan[] groupdata, int index, int col) {
		return groupdata.length;
	}
}
