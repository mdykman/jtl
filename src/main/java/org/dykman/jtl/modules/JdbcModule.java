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

import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.Pair;
import org.dykman.jtl.SourceInfo;
//import org.codehaus.jettison.json.JSONArray;
import org.dykman.jtl.future.AbstractInstructionFuture;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.future.InstructionFutureFactory;
import org.dykman.jtl.json.Frame;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.dykman.jtl.json.JSONValue;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class JdbcModule implements Module {

	final JSONObject baseConfig;

	public JdbcModule(JSONObject config) {
		baseConfig = config;
	}

	interface Executor {
		JSON process(PreparedStatement stat, JSONBuilder builder)
				throws SQLException;
	}

	static class JdbcConnectionWrapper {
		JSONObject conf;

		JdbcConnectionWrapper(JSONObject conf) {
			this.conf = conf;
		}

		Connection connection = null;

		public Connection getConnection(SourceInfo src) throws ExecutionException {
			if (connection == null) {
				synchronized (JdbcModule.class) {
					if (connection == null) {
						String driver = stringValue(conf.get("driver"));
						String uri = stringValue(conf.get("uri"));
						String user = stringValue(conf.get("user"));
						String password = stringValue(conf.get("password"));
						if (driver != null) {
							try {
								Class<Driver> drc = (Class<Driver>) Class
										.forName(driver);
								Driver drv = drc.newInstance();
								Properties properties = new Properties();
								if (user != null) {
									properties.setProperty("user", user);
									properties
											.setProperty("password", password);
									// connection = drv.connect(uri,properties);
									connection = DriverManager.getConnection(
											uri, properties);
								} else {
									connection = DriverManager
											.getConnection(uri);
								}
							} catch (ClassNotFoundException
									| InstantiationException
									| IllegalAccessException e) {
								throw new ExecutionException(
										"JDBC: unable to load class " + driver,src);
							} catch (SQLException e) {
                        throw new ExecutionException(
										"JDBC: unable to connect to " + uri,src);
							}
						}
					}
				}
			}
			return connection;
		}

		public InstructionFuture<JSON> query(SourceInfo meta,Executor exec) {
			// Connection c = getConnection();
			return new AbstractInstructionFuture(meta) {
				@Override
				public ListenableFuture<JSON> _call(
						final AsyncExecutionContext<JSON> context,
						final ListenableFuture<JSON> data)
						throws ExecutionException {
					InstructionFuture<JSON> q = context.getdef("1");
					InstructionFuture<JSON> p = context.getdef("2");
					List<ListenableFuture<JSON>> ll = new ArrayList<>();
					ll.add(q.call(context, data));
					if (p != null) {
						ll.add(p.call(context, data));
					}
					return transform(allAsList(ll),
							new AsyncFunction<List<JSON>, JSON>() {
								@Override
								public ListenableFuture<JSON> apply(
										List<JSON> input) throws Exception {
									Iterator<JSON> jit = input.iterator();
									final JSON qq = jit.next();
									if(! qq.isValue()) return Futures.immediateFailedCheckedFuture(
									      new ExecutionException("query is not a string: " + qq.toString(),meta));
									final JSON pp = jit.hasNext() ? jit.next()
											: null;
                           if(pp !=null && ! (pp instanceof JSONArray)) return Futures.immediateFailedCheckedFuture(
                                 new ExecutionException("parameters are not an array: " + pp.toString(),meta));
									Callable<JSON> cc = new Callable<JSON>() {
										@Override
										public JSON call() throws Exception {
											Connection connection = getConnection(source);
											PreparedStatement prep = connection
													.prepareStatement(stringValue(qq));
											System.err.println("query:" + stringValue(qq) + pp.toString());
											if (pp != null) {
												switch (pp.getType()) {
                                    case FRAME:
												case ARRAY:
													JSONArray arr = (JSONArray) pp;
													int i = 1;
													for (JSON j : arr) {
														if (!j.isValue())
															throw new ExecutionException(
																	"query parameter "
																			+ i
																			+ " is not a scalar value: "
																			+ j.toString(),meta);
														
														prep.setObject(i++,
																((JSONValue) j)
																		.get());
													}
													break;
												case NULL:
												case STRING:
												case LONG:
												case BOOLEAN:
												case DOUBLE:
													prep.setObject(1,
															((JSONValue) pp)
																	.get());
													break;
												default:
													throw new ExecutionException(
															"query single parameter is not a scalar value:"
																	+ pp.toString(),meta);
												}

											}
											return exec.process(prep,
													context.builder());

										}
									};
									return context.executor().submit(cc);

								}
							});
				}
			};
		}
	}

	Executor queryExecutor = new Executor() {
      @Override
      public JSON process(PreparedStatement stat, JSONBuilder builder)
            throws SQLException {
         ResultSet rs = stat.executeQuery();
         Frame frame = builder.frame();
         ResultSetMetaData rsm = rs.getMetaData();
         int n = rsm.getColumnCount();
         while (rs.next()) {
            JSONObject obj = builder.object(frame);
            for (int i = 1; i <= n; ++i) {
               obj.put(rsm.getColumnName(i),
                     builder.value(rs.getObject(i)));
            }
            frame.add(obj);
         }
         return frame;
      }
   };
	@Override
	public void define(SourceInfo meta,AsyncExecutionContext<JSON> context) {
		JdbcConnectionWrapper wrapper = new JdbcConnectionWrapper(baseConfig);
		SourceInfo si = meta.clone();
		si.name = "query";
		context.define("query", wrapper.query(si,queryExecutor));

      si = meta.clone();
      si.name = "cquery";
	    context.define("cquery", wrapper.query(si,new Executor() {
	         @Override
	         public JSON process(PreparedStatement stat, JSONBuilder builder)
	               throws SQLException {
	            ResultSet rs = stat.executeQuery();
	            ResultSetMetaData rsm = rs.getMetaData();
	            int n = rsm.getColumnCount();
               JSONObject obj = builder.object(null);
               JSONArray[] aar = new JSONArray[n];
               for(int i  = 1;i <= n;++i) {
                  JSONArray arr = builder.array(obj);
                  aar[i-1] = arr;
                  obj.put(rsm.getColumnName(i),arr);
               }
	            while (rs.next()) {
	               for (int i = 1; i <= n; ++i) {
	                  aar[i-1].add(builder.value(rs.getObject(i)));
	               }
	            }
	            return obj;
	         }
	      }));

	   si = meta.clone();
	   si.name = "execute";

		context.define("execute", wrapper.query(si,new Executor() {
			@Override
			public JSON process(PreparedStatement stat, JSONBuilder builder)
					throws SQLException {
			   stat.execute();
				return builder.value(true);
			}

		}));

	    context.define("insert", wrapper.query(si,new Executor() {
	         @Override
	         public JSON process(PreparedStatement stat, JSONBuilder builder)
	               throws SQLException {
	            boolean b=stat.execute();
	               PreparedStatement lid = wrapper.connection.prepareStatement("select last_insert_id()");
	               JSONArray j = (JSONArray)queryExecutor.process(lid, builder);
						JSON retval = j.get(0);
System.err.println("returning from insert with " + stringValue(retval));
	               return builder.value(j.get(0));
//System.err.println("returning from insert with null");
//	            return builder.value();
	         }

	      }));

		
	}

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

}
