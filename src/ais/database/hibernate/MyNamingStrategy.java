package ais.database.hibernate;

import org.hibernate.cfg.DefaultNamingStrategy;

public class MyNamingStrategy extends DefaultNamingStrategy {

	/**
	 * 
	 */
	private static final long serialVersionUID = 483792013046971333L;
	
	

	@Override
	public String tableName(String tableName) {
		if (tableName != null && tableName.trim().equalsIgnoreCase("cicilan_pembayaran")) {
			
		}
		return tableName;
	}

}
