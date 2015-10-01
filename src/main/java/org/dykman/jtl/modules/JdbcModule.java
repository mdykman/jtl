package org.dykman.jtl.modules;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.transform;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.sql.DataSource;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AbstractInstructionFuture;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.ContextComplete;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.JList;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class JdbcModule implements Module {

	final JSONObject baseConfig;
	final String key;
	boolean debug = false;

	public JdbcModule(JSONObject config) {
		this.baseConfig = config;
		JSON j = config.get("debug");
		if (j != null)
			debug = j.isTrue();
		j = baseConfig.get("insert_id");
		final String insertIdExpr = j == null ? null : j.stringValue();
		this.key = "@jdbc-connection-" + Long.toHexString(System.identityHashCode(this));

		queryExecutor = new Executor() {
			@Override
			public JSON process(PreparedStatement stat, JSONBuilder builder) throws SQLException {
				ResultSet rs = stat.executeQuery();
				JList frame = builder.frame();
				ResultSetMetaData rsm = rs.getMetaData();
				int n = rsm.getColumnCount();
				while (rs.next()) {
					JSONObject obj = builder.object(frame);
					for (int i = 1; i <= n; ++i) {
						obj.put(rsm.getColumnLabel(i), builder.value(rs.getObject(i)));
					}
					frame.add(obj);
				}
				return frame;
			}
		};
		insertExecutor = new Executor() {

			@Override
			public JSON process(PreparedStatement stat, JSONBuilder builder) throws SQLException {
				stat.executeUpdate();
				if (insertIdExpr != null) {
					// stat.execute();
					final String idstat;

					if (insertIdExpr.contains("$TABLE")) {
						// TODO:: for table-name dependent last insert
						// expression.
						// this is seriously broken. probably easy to fix
						ResultSet rs = stat.getGeneratedKeys();
						if (rs.next()) {
							Object col = rs.getObject(1);
							idstat = insertIdExpr.replace("$TABLE", col.toString());
							return builder.value(col);
						} else {
							if (debug)
								System.err.println("failed to determine the table name for insert: " + insertIdExpr);
							throw new SQLException("failed to determine the table name for insert");
						}
					} else {
						idstat = insertIdExpr;
					}
					PreparedStatement lid = stat.getConnection().prepareStatement(idstat);
					ResultSet rs = lid.executeQuery();
					JSON r = builder.value();
					if (rs.next()) {
						r = builder.value(new Long(rs.getInt(1)));
					}
					lid.close();
					stat.close();

					return r;
				} else {
					ResultSet rs = stat.getGeneratedKeys();
					if (rs.next()) {
						String col = rs.getString(1);
						return builder.value(col);
					}
					if (debug)
						System.err.println("no insert_id statement provided and the system could not determine a key");
					return builder.value(true);
				}
			}
		};
	}

	interface Executor {
		JSON process(PreparedStatement stat, JSONBuilder builder) throws SQLException;
	}

	class JdbcConnectionWrapper {
		final JSONObject conf;
		final HikariConfig hkc;
///		final DataSource dataSource;
		final HikariDataSource hds;
		String databaseName;

		JdbcConnectionWrapper(JSONObject conf) {
			this.conf = conf;
			this.hkc = new HikariConfig();
//			try {
//				Class<Driver> drc = (Class<Driver>) Class.forName(conf.get("driver").stringValue());
				this.hkc.setDataSourceClassName(conf.get("driver").stringValue());
				this.hkc.setUsername(conf.get("user").stringValue());
				this.hkc.setPassword(conf.get("password").stringValue());
				this.hkc.setJdbcUrl(conf.get("uri").stringValue());
				hkc.setMaximumPoolSize(20);
				databaseName = conf.get("database").stringValue();
				this.hkc.setCatalog(databaseName);
				hds = new HikariDataSource(hkc);
//				this.dataSource = hkc.getDataSource();
//			} catch (ClassNotFoundException e) {
//				throw new ExceptionInInitializerError(e);
//			}
		}

		public Connection getConnection(SourceInfo src, AsyncExecutionContext<JSON> context) throws ExecutionException {
			final AsyncExecutionContext<JSON> rc = context.getRuntime();
			Connection connection = (Connection) rc.get(key);
			if (connection == null) {
				// naive connection manager ideal for CLI, not so much server
				synchronized (this) {
					connection = (Connection) rc.get(key);
					if (connection == null) {
						try {
							connection = hds.getConnection();
//							connection = this.dataSource.getConnection();
							rc.set(key, connection);
							final Connection theConnection = connection;
							rc.onCleanUp(new ContextComplete() {

								@Override
								public boolean complete() {
									try {
										theConnection.close();
										return true;
									} catch (SQLException e) {
										System.err.println("there was an error while closing a jdbc connection: "
												+ e.getLocalizedMessage());
										return false;
									}

								}
							});
							
						} catch (Throwable e) {
							e.printStackTrace();
							throw new ExecutionException(e,src);
						}
						/*
						String driver = conf.get("driver").stringValue();
						String uri = conf.get("uri").stringValue();
						String user = conf.get("user").stringValue();
						String password = conf.get("password").stringValue();
						if (driver != null) {
							try {
	//							Class<Driver> drc = (Class<Driver>) Class.forName(driver);
								// Driver drv = drc.newInstance();
								Properties properties = new Properties();
								if (user != null) {
									properties.setProperty("user", user);
									properties.setProperty("password", password);
									// connection = drv.connect(uri,properties);
									connection = DriverManager.getConnection(uri, properties);
								} else {
									connection = DriverManager.getConnection(uri);
								}
								
								final Connection theConnection = connection;
								rc.onCleanUp(new ContextComplete() {

									@Override
									public boolean complete() {
										try {
											theConnection.close();
											return true;
										} catch (SQLException e) {
											System.err.println("there was an error while closing a jdbc connection: "
													+ e.getLocalizedMessage());
											return false;
										}

									}
								});
								
//							} catch (ClassNotFoundException e) {
//								throw new ExecutionException("JDBC: unable to load class " + driver, src);
							} catch (SQLException e) {
								throw new ExecutionException("JDBC: unable to connect to " + uri, src);
							}
							
						}
						*/
					}
				}
			}
			rc.set(key, connection);
			return connection;
		}

		public InstructionFuture<JSON> query(SourceInfo meta, Executor exec) {
			// Connection c = getConnection();
			return new AbstractInstructionFuture(meta) {
				@Override
				public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
						final ListenableFuture<JSON> data) throws ExecutionException {
					InstructionFuture<JSON> q = context.getdef("1");
					InstructionFuture<JSON> p = context.getdef("2");
					List<ListenableFuture<JSON>> ll = new ArrayList<>();
					ll.add(q.call(context, data));
					if (p != null) {
						ll.add(p.call(context, data));
					}
					return transform(allAsList(ll), new AsyncFunction<List<JSON>, JSON>() {
						@Override
						public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
							Iterator<JSON> jit = input.iterator();
							final JSON qq = jit.next();
							if (!qq.isValue())
								return Futures.immediateFailedCheckedFuture(
										new ExecutionException("query is not a string: " + qq.toString(), meta));
							final JSON pp = jit.hasNext() ? jit.next() : null;

							Callable<JSON> cc = new Callable<JSON>() {
								@Override
								public JSON call() throws Exception {
									Connection connection = getConnection(source, context);
									synchronized(connection) {
										PreparedStatement prep = connection.prepareStatement(stringValue(qq));
										if (debug) {
											System.err.print("query:" + stringValue(qq));
											if (pp != null)
												System.err.println(" " + pp.toString());
											System.err.println();
										}
										if (pp != null) {
											switch (pp.getType()) {
											case LIST:
											case ARRAY:
												JSONArray arr = (JSONArray) pp;
												int i = 1;
												for (JSON j : arr) {
													if (!j.isValue())
														throw new ExecutionException(
																"parameter element" + (i - 1)
																		+ " is not a scalar value: " + j.toString(),
																source);

													prep.setObject(i++, ((JSONValue) j).get());
												}
												break;
											case NULL:
											case STRING:
											case LONG:
											case BOOLEAN:
											case DOUBLE:
												prep.setObject(1, ((JSONValue) pp).get());
												break;
											default:
												throw new ExecutionException(
														"single parameter is not a scalar value:" + pp.toString(),
														source);
											}

										}
										return exec.process(prep, context.builder());
									}
								}
							};
							return context.executor().submit(cc);

						}
					});
				}
			};
		}
	}

	final Executor queryExecutor;
	final Executor insertExecutor;

	@Override
	public void define(SourceInfo meta, AsyncExecutionContext<JSON> context) {
		JdbcConnectionWrapper wrapper = new JdbcConnectionWrapper(baseConfig);
		SourceInfo si = meta.clone();
		si.name = "query";
		si.code = "*internal*";
		context.define("query", wrapper.query(si, queryExecutor));

		si = meta.clone();
		si.name = "cquery";
		si.code = "*internal*";
		context.define("cquery", wrapper.query(si, new Executor() {
			@Override
			public JSON process(PreparedStatement stat, JSONBuilder builder) throws SQLException {
				ResultSet rs = stat.executeQuery();
				ResultSetMetaData rsm = rs.getMetaData();
				int n = rsm.getColumnCount();
				JSONObject obj = builder.object(null);
				JSONArray[] aar = new JSONArray[n];
				for (int i = 1; i <= n; ++i) {
					JSONArray arr = builder.array(obj);
					aar[i - 1] = arr;
					obj.put(rsm.getColumnLabel(i), arr);
				}
				while (rs.next()) {
					for (int i = 1; i <= n; ++i) {
						aar[i - 1].add(builder.value(rs.getObject(i)));
					}
				}
				return obj;
			}
		}));

		si = meta.clone();
		si = meta.clone();
		si.name = "execute";
		si.code = "*internal*";
		context.define("execute", wrapper.query(si, new Executor() {
			@Override
			public JSON process(PreparedStatement stat, JSONBuilder builder) throws SQLException {
				stat.execute();
				return builder.value(true);
			}

		}));

		context.define("insert", wrapper.query(si, insertExecutor));

	}
/*
	protected static String stringValue(JSON j) {
		if (j == null)
			return null;
		switch (j.getType()) {
		case STRING:
		case DOUBLE:
		case LONG:
			return ((JSONValue) j).stringValue();
		default:
			return null;
		}
	}
*/
}
