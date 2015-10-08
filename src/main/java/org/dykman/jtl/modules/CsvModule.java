package org.dykman.jtl.modules;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.dykman.jtl.ExecutionException;
import org.dykman.jtl.SourceInfo;
import org.dykman.jtl.future.AbstractInstructionFuture;
import org.dykman.jtl.future.AsyncExecutionContext;
import org.dykman.jtl.future.InstructionFuture;
import org.dykman.jtl.json.JSON;
import org.dykman.jtl.json.JSONArray;
import org.dykman.jtl.json.JSONBuilder;
import org.dykman.jtl.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class CsvModule extends AbstractModule {

	interface CSVProcessor {
		JSON process(Iterator<CSVRecord> alg, String[] headers);
	}

	JSONObject config;
	static Logger logger = LoggerFactory.getLogger(CsvModule.class);

	public CsvModule(JSONObject config) {
		this.config = config;
	}

	AsyncFunction<List<JSON>, JSON> createReader(AsyncExecutionContext<JSON> context, CSVProcessor proc) {
		return new AsyncFunction<List<JSON>, JSON>() {

			@Override
			public ListenableFuture<JSON> apply(List<JSON> input) throws Exception {
				Iterator<JSON> jit = input.iterator();
				Charset cs = Charset.forName("UTF-8");
				CSVFormat format = null;
				JSONBuilder builder = context.builder();
				String file = jit.next().stringValue();
				JSONObject conf = (JSONObject) (jit.hasNext() ? jit.next() : null);
				JSONObject fconf = config;
				if (conf != null) {
					fconf = fconf.overlay(conf);
				}
				JSON j = config.get("charset");
				if (j != null) {
					cs = Charset.forName(j.stringValue());
				}
				j = config.get("format");
				if (j != null) {
					format = CSVFormat.valueOf(j.stringValue());

					// CSVFormat.Predefined.Default;
				}
				j = config.get("seperator");
				if (j != null) {
					String s = j.stringValue();
					if (s != null && s.length() > 0) {
						if (format == null)
							format = CSVFormat.newFormat(s.charAt(0));
						else
							format = format.newFormat(s.charAt(0));
					}
				}
				j = config.get("header");
				boolean headers = j == null ? true : j.isTrue();
				format.withSkipHeaderRecord(!headers);
				File f = new File(context.currentDirectory(), file);

				logger.info("parsing file " + f.getAbsolutePath() + " with " + format.toString());
				CSVParser parser = CSVParser.parse(f, cs, format);

				JSONArray arr = builder.array(null);
				String[] hm = null;

				Iterator<CSVRecord> csvIt = parser.iterator();
				Map<String, Integer> headerMap = parser.getHeaderMap();
				if (headerMap != null) {
					for (Map.Entry<String, Integer> hh : headerMap.entrySet()) {
						hm[hh.getValue()] = hh.getKey();
					}
					if (!headers) {
						JSONArray a2 = builder.array(arr);
						for (String s : hm) {
							a2.add(builder.value(s));
						}
						arr.add(a2);
					}
				} else {
					if (headers) {
						CSVRecord hr = csvIt.next();
						hm = new String[hr.size()];
						int dx = 0;
						for (String s : hr) {
							hm[dx++] = s;
						}
					}

					// headers = false;
				}
				JSON res = proc.process(csvIt, headers ? hm : null);
				return Futures.immediateCheckedFuture(res);

			}
		};
	}

	abstract class CsvInstructionFuture extends AbstractInstructionFuture {
		CsvInstructionFuture(SourceInfo cs) {
			super(cs);
		}

		protected CSVFormat fromConfig(JSONObject cc) throws ExecutionException {
			CSVFormat format = null;
			Charset cs = null;
			JSON j = config.get("charset");
			if (j != null) {
				String c = stringValue(j);
				if (Charset.isSupported(c)) {
					cs = Charset.forName(c);
				} else {
					throw new ExecutionException("CharSet " + c + " is not supported", source);
				}
			}
			if (cs == null)
				cs = Charset.defaultCharset();
			j = config.get("format");
			if (j != null) {
				String c = stringValue(j);
				CSVFormat.valueOf(c);
				format = CSVFormat.valueOf(c);
				if (format == null) {
					throw new ExecutionException("unable to locate a CSVFormat from " + c, source);
				}
			}

			if (format == null) {
				format = CSVFormat.DEFAULT;
				j = config.get("sep");
			}
			if (j != null) {
				String c = stringValue(j);
				format = format.withDelimiter(c.charAt(0));
			}

			format = format.withIgnoreEmptyLines();
			if (!format.isQuoteCharacterSet())
				format = format.withQuote('"');

			return format;
		}
	}

	@Override
	public JSON define(SourceInfo meta, AsyncExecutionContext<JSON> context, boolean serverMode) {

		context.define("read", new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {

				InstructionFuture<JSON> fn = context.getdef("1");
				InstructionFuture<JSON> options = context.getdef("2");
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(fn.call(context, data));
				if (options != null) {
					ll.add(options.call(context, data));
				}
				return Futures.transform(Futures.allAsList(ll), createReader(context, new CSVProcessor() {

					@Override
					public JSON process(Iterator<CSVRecord> alg, String[] hm) {
						JSONBuilder builder = context.builder();
						JSONArray arr = builder.array(null);
						int ctr = 0;
						while (alg.hasNext()) {
							CSVRecord rec = alg.next();
							ctr++;
							if (hm != null) {
								int i = 0;
								JSONObject obj = builder.object(arr);
								if (hm != null)
									for (String s : hm) {
										obj.put(s, builder.value(rec.get(i++)));
									}
								arr.add(obj);
							} else {
								JSONArray a2 = builder.array(arr);
								rec.forEach(new Consumer<String>() {
									@Override
									public void accept(String t) {
										a2.add(builder.value(t));
									}
								});
								arr.add(a2);

							}
						}
						return arr;
					}
				}));
			}
		});
		context.define("pread", new AbstractInstructionFuture(meta) {

			@Override
			public ListenableFuture<JSON> _call(AsyncExecutionContext<JSON> context, ListenableFuture<JSON> data)
					throws ExecutionException {

				InstructionFuture<JSON> fn = context.getdef("1");
				InstructionFuture<JSON> options = context.getdef("2");
				List<ListenableFuture<JSON>> ll = new ArrayList<>();
				ll.add(fn.call(context, data));
				if (options != null) {
					ll.add(options.call(context, data));
				}
				return Futures.transform(Futures.allAsList(ll), createReader(context, new CSVProcessor() {

					@Override
					public JSON process(Iterator<CSVRecord> alg, String[] hm) {
						JSONBuilder builder = context.builder();
						JSONArray outerarr = builder.array(null);
						int rctr = 0;

						Map<String, JSON> cols = new LinkedHashMap<>();
						ArrayList<ArrayList<JSON>> arrs = new ArrayList<>();
						JSONObject obj = null;
						if (hm != null) {
							for (String h : hm) {
								cols.put(h, builder.array(null));
							}
							obj = builder.object(null, cols);
						} else {
							// TODO:: pivot the array, I guess
						}

						int len = 0;
						while (alg.hasNext()) {
							CSVRecord rec = alg.next();
							int fctr = 0;
							if (hm != null) {
								for (String s : rec) {
									String kk = hm[fctr];
									JSONArray cc = (JSONArray)cols.get(kk);
									cc.add(builder.value(s));
									++fctr;
								}

							} else {
								if (rctr == 0) {
									len = rec.size();
									for (int i = 0; i < len; ++i) {
										arrs.add(new ArrayList<>());
									}
								}
								for (String s : rec) {
									Collection<JSON> cc = arrs.get(fctr);
									cc.add(builder.value(s));
									++fctr;
								}
							}
						}
						if (hm != null) {
							return obj;
						} else {
							JSONArray ares = builder.array(null);
							for (ArrayList<JSON> ccs : arrs) {
								ares.add(builder.array(ares, ccs));
							}
							return ares;
						}
					}
				}));
			}
		});
		return context.builder().value(1);
	}

}
