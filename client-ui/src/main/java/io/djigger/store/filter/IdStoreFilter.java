/*******************************************************************************
 * (C) Copyright  2016 Jérôme Comte and others.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *    - Jérôme Comte
 *******************************************************************************/

package io.djigger.store.filter;

import io.djigger.monitoring.java.instrumentation.InstrumentationSample;
import io.djigger.monitoring.java.model.ThreadInfo;

import java.util.Set;


public class IdStoreFilter implements StoreFilter {

	private final Set<Long> threadIds;

	private final Long threadDumpId1;

	private final Long threadDumpId2;

	public IdStoreFilter(Set<Long> threadIds, Long threadDumpId1,
			Long threadDumpId2) {
		super();
		this.threadIds = threadIds;
		this.threadDumpId1 = threadDumpId1;
		this.threadDumpId2 = threadDumpId2;
	}

	public Set<Long> getThreadIds() {
		return threadIds;
	}

	public Long getThreadDumpId1() {
		return threadDumpId1;
	}

	public Long getThreadDumpId2() {
		return threadDumpId2;
	}

	@Override
	public boolean match(InstrumentationSample sample) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean match(ThreadInfo dump) {
		return threadIds.contains(dump.getId());
	}
}
