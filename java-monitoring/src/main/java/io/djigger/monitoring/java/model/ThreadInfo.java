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
package io.djigger.monitoring.java.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class ThreadInfo implements Serializable {

	private Date timestamp;
	
	private long id;
	
	private String name;
	
	private Thread.State state;
	
	private Map<String, String> attributes;
	
	private StackTraceElement[] stackTrace;

	public ThreadInfo(StackTraceElement[] stackTrace) {
		super();

		this.stackTrace = stackTrace;
	}

	public StackTraceElement[] getStackTrace() {
		return stackTrace;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Thread.State getState() {
		return state;
	}

	public void setState(Thread.State state) {
		this.state = state;
	}

	public void setStackTrace(StackTraceElement[] stackTrace) {
		this.stackTrace = stackTrace;
	}
}
