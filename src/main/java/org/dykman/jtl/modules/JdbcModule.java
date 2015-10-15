package org.dykman.jtl.modules;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.Futures.transform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AbstractFutureInstruction;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.ContextComplete;
import org.dykman.jtl.future.FutureInstruction;
import org.dykman.jtl.json.JList;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class JdbcModule extends AbstractModule {

	final JSONObject baseConfig;
	final String key;
	boolean debug = false;
	static Logger logger = LoggerFactory.getLogger(JdbcModule.class);

	final Executor queryExecutor;
	final Executor insertExecutor;

	public JdbcModule(JSONObject config) {
		this.baseConfig = config;
		JSON j = config.get("debug");
		if (j != null)
			debug = j.isTrue();
		j = baseConfig.get("insert_id");
		final String insertIdExpr = j == null ? null : j.stringValue();
		this.key = "@jdbc-" + bindingKey + "-" + Long.toHexString(System.identityHashCode(this));

		queryExecutor = new Executor() {
			@Override
			public JSON process(PreparedStatement stat, String query, JSONBuilder builder) throws SQLException {
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
			public JSON process(PreparedStatement stat, String query, JSONBuilder builder) throws SQLException {
				int res = stat.executeUpdate();
				final String idstat;
				if (insertIdExpr != null) {
					if (insertIdExpr.contains("$TABLE")) {
						String tn = "unknown";
						String pp[] = query.split("\\s+", 4);
						if (pp.length > 2)
							tn = pp[2];
						idstat = insertIdExpr.replace("$TABLE", tn);
					} else {
						idstat = insertIdExpr;
					}
					logger.info(idstat);
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
					int cc = 0;
					JSONArray arr = builder.array(null);
					JSON j = builder.value();
					while (rs.next()) {
						Object col = rs.getObject(1);
						j = builder.value(col);
						arr.add(j);
						++cc;
					}
					stat.close();
					if (cc == 1)
						return j;
					else
						return arr;
				}
			}
		};
	}

	interface Executor {
		JSON process(PreparedStatement stat, String query, JSONBuilder builder) throws SQLException;
	}

	class JdbcConnectionWrapper {
		final JSONObject conf;
		/// final DataSource dataSource;
		HikariDataSource hds;
		String databaseName;
		String username;
		String password;
		String host;
		String port;
		String jdbcUrl;
		String driverClass;
		String datasourceClass;
		String maxSize = null;

		boolean serverMode;

		JdbcConnectionWrapper(JSONObject conf, String key, boolean serverMode) throws ExecutionException {
			this.conf = conf;
			this.serverMode = serverMode;
			Properties props = new Properties();
			for (Pair<String, JSON> pp : conf) {
				switch (pp.f) {
				case "driverClass":
					driverClass = pp.s.stringValue();
					break;
				case "datasourceClass":
					datasourceClass = pp.s.stringValue();
					break;
				case "username":
					username = pp.s.stringValue();
					break;
				case "password":
					password = pp.s.stringValue();
					break;
				case "url":
					jdbcUrl = pp.s.stringValue();
					break;
				case "database":
					databaseName = pp.s.stringValue();
					break;
				case "host":
					host = pp.s.stringValue();
					break;
				case "port":
					port = pp.s.stringValue();
					break;
				case "maxSize":
					maxSize = pp.s.stringValue();
					break;
				default:
					logger.info("ignoring datasource setting " + pp.f + ": " + pp.s.stringValue());
				}
			}
			props.setProperty("poolName", bindingKey + " connection pool");
			hds = configHikari(props);
		}

		HikariDataSource configHikari(Properties props) throws ExecutionException {
			if (datasourceClass != null)
				props.put("dataSourceClassName", datasourceClass);
			else if (driverClass != null)
				props.put("driverClassName", driverClass);
			else
				throw new ExecutionException("neither driver nor datasource found in config",
						SourceInfo.internal("jdbc"));

			if (username != null)
				props.put("username", username);
			if (password != null)
				props.put("password", password);
			if (jdbcUrl != null)
				props.put("jdbcUrl", jdbcUrl);
			else {
				if (databaseName != null)
					props.put("databaseName", databaseName);
				if (host != null)
					props.put("serverName", host);
				if (port != null)
					props.put("portNumber", port);
			}
			if (serverMode) {
				if (maxSize != null) {
					props.put("maximumPoolSize", maxSize);
				} else {
					props.put("maximumPoolSize", "10");
				}
			} else {
				// cli, only ever needs 1 connection per datasource
				props.put("maximumPoolSize", "1");
			}

			HikariConfig hkc = new HikariConfig(props);
			return new HikariDataSource(hkc);
		}

		protected Connection getConnection(SourceInfo src, AsyncExecutionContext<JSON> context)
				throws ExecutionException {
			final AsyncExecutionContext<JSON> rc = context.getRuntime();
			Connection connection = (Connection) rc.get(key);
			if (connection == null) {
				// naive connection manager ideal for CLI, not so much server
				synchronized (this) {
					connection = (Connection) rc.get(key);
					if (connection == null) {
						try {
							connection = hds.getConnection();
							if (databaseName != null)
								connection.setCatalog(databaseName);
							// connection = this.dataSource.getConnection();
							rc.set(key, connection);
							final Connection theConnection = connection;
							rc.onCleanUp(new ContextComplete() {

								@Override
								public boolean complete() {
									try {
										if (databaseName != null)
											theConnection.setCatalog(databaseName);
										theConnection.close();
										return true;
									} catch (SQLException e) {
										logger.debug("there was an error while closing a jdbc connection: "
												+ e.getLocalizedMessage());
										return false;
									}

								}
							});

						} catch (Throwable e) {
							logger.error("there was an exception while acquiring the connection", e);
							e.printStackTrace();
							throw new ExecutionException(e, src);
						}
					}
				}
			}
			rc.set(key, connection);
			if (databaseName != null) {
				try {
					connection.setCatalog(databaseName);
				} catch (SQLException e) {
					logger.error("there was an error setting the catalog name", e);
				}
			}
			return connection;
		}

		public FutureInstruction<JSON> query(SourceInfo meta, Executor exec) {
			// Connection c = getConnection();
			return new AbstractFutureInstruction(meta) {
				@Override
				public ListenableFuture<JSON> _call(final AsyncExecutionContext<JSON> context,
						final ListenableFuture<JSON> data) throws ExecutionException {
					FutureInstruction<JSON> q = context.getdef("1");
					FutureInstruction<JSON> p = context.getdef("2");
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
									synchronized (connection) {
										PreparedStatement prep = connection.prepareStatement(stringValue(qq));
										if (logger.isInfoEnabled()) {
											StringBuilder sb = new StringBuilder("query: ");
											sb.append(qq);
											if (pp != null)
												sb.append(" :: ").append(pp.toString());
											logger.info(sb.toString());
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
										return exec.process(prep, qq.stringValue(), context.builder());
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

	@Override
	public JSON define(SourceInfo meta, AsyncExecutionContext<JSON> context, boolean serverMode)
			throws ExecutionException {
		JdbcConnectionWrapper wrapper = new JdbcConnectionWrapper(baseConfig, key, serverMode);
		SourceInfo si = meta.clone();
		si.name = "query";
		si.code = "*internal*";
		context.define("query", wrapper.query(si, queryExecutor));

		si = meta.clone();
		si.name = "cquery";
		si.code = "*internal*";
		context.define("cquery", wrapper.query(si, new Executor() {
			@Override
			public JSON process(PreparedStatement stat, String query, JSONBuilder builder) throws SQLException {
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
		si.name = "execute";
		si.code = "*internal*";
		context.define("execute", wrapper.query(si, new Executor() {
			@Override
			public JSON process(PreparedStatement stat, String query, JSONBuilder builder) throws SQLException {
				stat.execute();
				return builder.value(true);
			}
		}));
		context.define("insert", wrapper.query(si, insertExecutor));
		return context.builder().value(1);

	}
}
