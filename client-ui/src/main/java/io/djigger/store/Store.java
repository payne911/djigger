/*******************************************************************************
 * (C) Copyright 2016 Jérôme Comte and Dorian Cransac
 *  
 *  This file is part of djigger
 *  
 *  djigger is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  djigger is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with djigger.  If not, see <http://www.gnu.org/licenses/>.
 *
 *******************************************************************************/
package io.djigger.store;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.djigger.model.Capture;
import io.djigger.monitoring.java.instrumentation.InstrumentationEvent;
import io.djigger.monitoring.java.model.ThreadInfo;
import io.djigger.ql.Filter;
import io.djigger.store.filter.StoreFilter;


public class Store implements Serializable {

	private static final long serialVersionUID = 8746530878726453216L;
	
	private List<Capture> captures;

	private final List<ThreadInfo> threadInfos;
	
	private transient List<ThreadInfo> threadInfosBuffer;
	
	private List<InstrumentationEvent> instrumentationSamples;

	private transient List<InstrumentationEvent> instrumentationSamplesBuffer;

	public Store() {
		super();
		captures = new ArrayList<Capture>();
		threadInfos = new ArrayList<>();
		threadInfosBuffer = new ArrayList<>();
		instrumentationSamples = new ArrayList<InstrumentationEvent>();
		instrumentationSamplesBuffer = new ArrayList<InstrumentationEvent>();
	}
	
	public synchronized void addThreadInfo(ThreadInfo threadInfo) {
		threadInfosBuffer.add(threadInfo);
	}

	public synchronized void addThreadInfos(List<ThreadInfo> threadInfos) {
		threadInfosBuffer.addAll(threadInfos);
	}
	
	private synchronized void processThreadInfosBuffer() {
		this.threadInfos.addAll(threadInfosBuffer);
		threadInfosBuffer.clear();
	}

	public synchronized void addInstrumentationEvent(InstrumentationEvent event) {
		event.setClassname(event.getClassname().intern());
		event.setMethodname(event.getMethodname().intern());
		instrumentationSamplesBuffer.add(event);
	}
	
	public synchronized void addInstrumentationSamples(List<InstrumentationEvent> events) {
		for(InstrumentationEvent event:events) {
			event.setClassname(event.getClassname().intern());
			event.setMethodname(event.getMethodname().intern());
		}
		instrumentationSamplesBuffer.addAll(events);
	}

	private synchronized void processInstrumentationSamplesBuffer() {
		instrumentationSamples.addAll(instrumentationSamplesBuffer);
		instrumentationSamplesBuffer.clear();
	}

	public synchronized void clear() {
		threadInfosBuffer.clear();
		instrumentationSamplesBuffer.clear();
		threadInfos.clear();
		instrumentationSamples = new ArrayList<InstrumentationEvent>();
	}

	public synchronized List<ThreadInfo> queryThreadDumps(StoreFilter filter) {
		return applyStorFilterThreadDumps(threadInfos, filter);
	}
	
	private static List<ThreadInfo> applyStorFilterThreadDumps(List<ThreadInfo> threads, StoreFilter filter) {
		List<ThreadInfo> results = new ArrayList<>();

		if(filter!=null) {
			for(ThreadInfo thread:threads) {
				if(filter.match(thread)) {
					results.add(thread);
				}
			}
		} else {
			results.addAll(threads);
		}
		
		return results;
	}
	
	public synchronized List<Capture> queryCaptures(long start, long end) {
		List<Capture> result = new ArrayList<Capture>();
		for(Capture capture:captures) {
			if(capture.getStart()<end && (capture.getEnd() == null || capture.getEnd()>start)) {
				result.add(capture);
			}
		}
		return result;
	}
	
	public synchronized int getInstrumentationSamplesCount() {
		return instrumentationSamples.size();
	}
	
	public synchronized List<InstrumentationEvent> queryInstrumentationSamples(StoreFilter filter) {
		List<InstrumentationEvent> result = new ArrayList<InstrumentationEvent>();
		for(InstrumentationEvent sample:instrumentationSamples) {
			if((filter==null || filter.match(sample))) {
				result.add(sample);
			}
		}
		return result;
	}
	
	public synchronized List<InstrumentationEvent> queryInstrumentationEvents(Filter<InstrumentationEvent> filter) {
		List<InstrumentationEvent> result = new ArrayList<InstrumentationEvent>();
		for(InstrumentationEvent sample:instrumentationSamples) {
			if((filter==null || filter.isValid(sample))) {
				result.add(sample);
			}
		}
		return result;
	}

	public void processBuffers() {
		processThreadInfosBuffer();
		processInstrumentationSamplesBuffer();
	}

	private Object readResolve() throws ObjectStreamException {
		threadInfosBuffer = new ArrayList<>(10000);
		instrumentationSamplesBuffer = new ArrayList<InstrumentationEvent>();
		return this;
	}

	public void addCaptures(List<Capture> captures) {
		this.captures.addAll(captures);
	}
	
	public void addOrUpdateCapture(Capture capture) {
		boolean update = false;
		for(Capture c:captures) {
			if(c.getStart() == capture.getStart()) {
				c.setEnd(capture.getEnd());
				update = true;
				break;
			}
		}
		if(!update) {
			captures.add(capture);
		}
	}
	
	public synchronized void clearBuffers() {
		threadInfosBuffer.clear();
		instrumentationSamplesBuffer.clear();
	}
}
