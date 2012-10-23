/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.database.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbConstant;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpInternalLogger;
import org.waarp.common.logging.WaarpInternalLoggerFactory;

/**
 * This Abstract class regroups common methods for all implementation classes.
 * 
 * @author Frederic Bregier
 * 
 */
public abstract class DbModelAbstract implements DbModel {
	/**
	 * Internal Logger
	 */
	private static final WaarpInternalLogger logger = WaarpInternalLoggerFactory
			.getLogger(DbModelAbstract.class);

	/**
	 * Recreate the disconnected session
	 * 
	 * @param dbSession
	 * @throws WaarpDatabaseNoConnectionException
	 */
	private void recreateSession(DbSession dbSession)
			throws WaarpDatabaseNoConnectionException {
		DbAdmin admin = dbSession.getAdmin();
		if (admin == null) {
			if (dbSession.autoCommit) {
				admin = DbConstant.admin;
			} else {
				admin = DbConstant.noCommitAdmin;
			}
		}
		DbSession newdbSession = new DbSession(admin, dbSession.isReadOnly);
		try {
			if (dbSession.conn != null) {
				dbSession.conn.close();
			}
		} catch (SQLException e1) {
		}
		dbSession.conn = newdbSession.conn;
		DbAdmin.addConnection(dbSession.internalId, dbSession);
		DbAdmin.removeConnection(newdbSession.internalId);
		logger.warn("Database Connection lost: database connection reopened");
	}

	/**
	 * Internal use for closing connection while validating it
	 * 
	 * @param dbSession
	 */
	protected void closeInternalConnection(DbSession dbSession) {
		try {
			if (dbSession.conn != null) {
				dbSession.conn.close();
			}
		} catch (SQLException e1) {
		}
		dbSession.isDisconnected = true;
		if (dbSession.admin != null)
			dbSession.admin.isConnected = false;
		DbAdmin.removeConnection(dbSession.internalId);
	}

	public void validConnection(DbSession dbSession)
			throws WaarpDatabaseNoConnectionException {
		// try to limit the number of check!
		synchronized (dbSession) {
			try {
				if (!dbSession.conn.isClosed()) {
					if (!dbSession.conn.isValid(DbConstant.VALIDTESTDURATION)) {
						// Give a try by closing the current connection
						throw new SQLException("Cannot connect to database");
					}
				}
				dbSession.isDisconnected = false;
				if (dbSession.admin != null)
					dbSession.admin.isConnected = true;
			} catch (SQLException e2) {
				dbSession.isDisconnected = true;
				if (dbSession.admin != null)
					dbSession.admin.isConnected = false;
				// Might be unsupported so switch to SELECT 1 way
				if (e2 instanceof org.postgresql.util.PSQLException) {
					validConnectionSelect(dbSession);
					return;
				}
				try {
					try {
						recreateSession(dbSession);
					} catch (WaarpDatabaseNoConnectionException e) {
						closeInternalConnection(dbSession);
						throw e;
					}
					try {
						if (!dbSession.conn.isValid(DbConstant.VALIDTESTDURATION)) {
							// Not ignored
							closeInternalConnection(dbSession);
							throw new WaarpDatabaseNoConnectionException(
									"Cannot connect to database", e2);
						}
					} catch (SQLException e) {
						closeInternalConnection(dbSession);
						throw new WaarpDatabaseNoConnectionException(
								"Cannot connect to database", e);
					}
					dbSession.isDisconnected = false;
					if (dbSession.admin != null)
						dbSession.admin.isConnected = true;
					dbSession.recreateLongTermPreparedStatements();
					return;
				} catch (WaarpDatabaseSqlException e1) {
					// ignore and will send a No Connection error
				}
				closeInternalConnection(dbSession);
				throw new WaarpDatabaseNoConnectionException(
						"Cannot connect to database", e2);
			}
		}
	}

	public void validConnectionSelect(DbSession dbSession)
			throws WaarpDatabaseNoConnectionException {
		// try to limit the number of check!
		synchronized (dbSession) {
			Statement stmt = null;
			try {
				stmt = dbSession.conn.createStatement();
				if (stmt.execute(validConnectionString())) {
					ResultSet set = stmt.getResultSet();
					if (!set.next()) {
						stmt.close();
						stmt = null;
						// Give a try by closing the current connection
						throw new SQLException("Cannot connect to database");
					}
				}
				dbSession.isDisconnected = false;
				if (dbSession.admin != null)
					dbSession.admin.isConnected = true;
			} catch (SQLException e2) {
				dbSession.isDisconnected = true;
				if (dbSession.admin != null)
					dbSession.admin.isConnected = false;
				try {
					try {
						recreateSession(dbSession);
					} catch (WaarpDatabaseNoConnectionException e) {
						closeInternalConnection(dbSession);
						throw e;
					}
					try {
						if (stmt != null) {
							stmt.close();
							stmt = null;
						}
					} catch (SQLException e) {
						// ignore
					}
					try {
						stmt = dbSession.conn.createStatement();
					} catch (SQLException e) {
						// Not ignored
						closeInternalConnection(dbSession);
						throw new WaarpDatabaseNoConnectionException(
								"Cannot connect to database", e);
					}
					try {
						if (stmt.execute(validConnectionString())) {
							ResultSet set = stmt.getResultSet();
							if (!set.next()) {
								if (stmt != null) {
									stmt.close();
									stmt = null;
								}
								closeInternalConnection(dbSession);
								throw new WaarpDatabaseNoConnectionException(
										"Cannot connect to database");
							}
						}
					} catch (SQLException e) {
						// not ignored
						try {
							if (stmt != null) {
								stmt.close();
								stmt = null;
							}
						} catch (SQLException e1) {
						}
						closeInternalConnection(dbSession);
						throw new WaarpDatabaseNoConnectionException(
								"Cannot connect to database", e);
					}
					dbSession.isDisconnected = false;
					if (dbSession.admin != null)
						dbSession.admin.isConnected = true;
					dbSession.recreateLongTermPreparedStatements();
					return;
				} catch (WaarpDatabaseSqlException e1) {
					// ignore and will send a No Connection error
				}
				closeInternalConnection(dbSession);
				throw new WaarpDatabaseNoConnectionException(
						"Cannot connect to database", e2);
			} finally {
				if (stmt != null) {
					try {
						stmt.close();
					} catch (SQLException e) {
					}
				}
			}
		}
	}

	/**
	 * 
	 * @return the associated String to validate the connection (as "select 1 frm dual")
	 */
	public abstract String validConnectionString();

	public Connection getDbConnection(String server, String user, String passwd)
			throws SQLException {
		// Default implementation
		return DriverManager.getConnection(server, user, passwd);
	}

	public void releaseResources() {
	}

	public int currentNumberOfPooledConnections() {
		return DbAdmin.getNbConnection();
	}

}
